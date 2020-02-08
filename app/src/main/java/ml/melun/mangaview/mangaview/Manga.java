package ml.melun.mangaview.mangaview;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.Response;

public class Manga {
    public Manga(int i, String n, String d) {
        id = i;
        name = n;
        date = d;
    }
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public void addThumb(String src){
        thumb = src;
    }
    public String getDate() {
        return date;
    }

    public void setImgs(List<String> imgs){
        this.imgs = imgs;
    }

    public String getThumb() {
        if(thumb == null) return "";
        return thumb;
    }

    public void fetch(CustomHttpClient client){
        fetch(client, true);
    }
    public void fetch(CustomHttpClient client, Boolean doLogin) {
        imgs = new ArrayList<>();
        imgs1 = new ArrayList<>();
        eps = new ArrayList<>();
        comments = new ArrayList<>();
        bcomments = new ArrayList<>();
        cdn_domains = new ArrayList<>();
        int tries = 0;

        while(imgs.size()==0 && tries < 2) {
            Map<String,String> cookie = new HashMap<>();
            cookie.put("last_wr_id",String.valueOf(id));
            cookie.put("last_percent",String.valueOf(1));
            cookie.put("last_page",String.valueOf(0));

            Response response = client.mget("/bbs/board.php?bo_table=manga&wr_id="+id, doLogin, cookie);
            try {
                InputStream stream = response.body().byteStream();
                if(listener!=null) listener.setMessage("페이지 읽는중");
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                //StringBuffer buffer = new StringBuffer();
                String line = "";
                String raw = "";
                while ((line = reader.readLine()) != null) {
                    //save as raw html for jsoup
                    raw += line;
                    if(line.contains("var img_list =")) {
                        if(listener!=null) listener.setMessage("이미지 리스트 읽는중");
                        String imgStr = line;
                        if(imgStr!=null) {
                            String[] imgStrs = imgStr.split("\"");
                            //remove backslash
                            for (int i = 1; i < imgStrs.length; i += 2) {
                                String imgUrl = imgStrs[i].replace("\\","");
                                String cdn = cdn_domains.get((id + 4 * (i-1)) % cdn_domains.size());
                                imgUrl = imgUrl.replace("cdntigermask.xyz", cdn);
                                imgUrl = imgUrl.replace("cdnmadmax.xyz", cdn);
                                imgUrl = imgUrl.replace("filecdn.xyz", cdn);
                                if(imgUrl.contains("img.")) imgUrl += "?quick";
                                System.out.println("pppp  " + imgUrl);
                                imgs.add(imgUrl);
                            }
                        }
                    }else if(line.contains("var img_list1 =")) {
                        if(listener!=null) listener.setMessage("이미지 리스트 (보조) 읽는중");
                        String imgStr = line;
                        if(imgStr!=null) {
                            String[] imgStrs = imgStr.split("\"");
                            //remove backslash
                            for (int i = 1; i < imgStrs.length; i += 2) {
                                String imgUrl = imgStrs[i].replace("\\","");
                                String cdn = cdn_domains.get((id + 4 * (i-1)) % cdn_domains.size());
                                imgUrl = imgUrl.replace("cdntigermask.xyz", cdn);
                                imgUrl = imgUrl.replace("cdnmadmax.xyz", cdn);
                                imgUrl = imgUrl.replace("filecdn.xyz", cdn);
                                imgs1.add(imgUrl);
                            }
                        }
                    }else if(line.contains("var only_chapter =")){
                        if(listener!=null) listener.setMessage("화 목록 읽는중");
                        String epsStr = line;
                        String[] epsStrs = epsStr.split("\"");
                        //remove backslash
                        for (int i = 3; i < epsStrs.length; i += 4) {
                            eps.add(new Manga(Integer.parseInt(epsStrs[i]),epsStrs[i-2],""));
                        }
                    }else if(line.contains("var view_cnt =")){
                        String seedt = line.substring(0,line.length()-1);
                        seed = Integer.parseInt(seedt.split(" ")[3]);
                    }else if(line.contains("var manga404 = ")){
                        reported = Boolean.parseBoolean(line.split("=")[1].split(";")[0].split(" ")[1]);
                    }else if(line.contains("var link =")){
                        String idStr = line.substring(line.indexOf("manga_id=")+9, line.indexOf("&"));
                        System.out.println(idStr);
                        String titleName = URLDecoder.decode(line.substring(line.indexOf("manga_name=")+11,line.length()-2),"UTF-8");
                        System.out.println(titleName);
                        title = new Title(titleName,"","",new ArrayList<String>(), -1, Integer.parseInt(idStr));
                    }else if(line.contains("var cdn_domains = ")){
                        if(listener!=null) listener.setMessage("cdn 목록 읽는중");
                        String[] cdnStrs = line.split("\"");
                        for(int i=1; i < cdnStrs.length; i += 2){
                            if(cdnStrs[i] != null && cdnStrs[i].length()>0)
                                cdn_domains.add(cdnStrs[i]);
                        }

                    }

                    //if(imgs.size()>0 && eps.size()>0) break;
                }

                //jsoup parsing
                Document doc = Jsoup.parse(raw);

                //parse title
                if(title==null){
                    String href = doc.selectFirst("div.comic-navbar").select("a").get(3).attr("href");
                    String idStr = href.substring(href.indexOf("manga_id=")+11);
                    title = new Title("","","",new ArrayList<String>(), -1, Integer.parseInt(idStr));
                }

                //parse name
                this.name = doc.selectFirst("div.toon-title").ownText();

                if(listener!=null) listener.setMessage("댓글 읽는중");

                Elements cs = doc.select("section.comment-media").last().select("div.media");
                for(Element c:cs){
                    String icon, user, timestamp, content;
                    int indent, likes, level;
                    Elements i = c.select("img");
                    if(!i.isEmpty()) {
                        icon = i.get(0).attr("src");
                    }else icon = "";
                    user = c.selectFirst("span.member").ownText();
                    timestamp = c.selectFirst("span.media-info").selectFirst("span").text();
                    content = c.selectFirst("div.media-content").selectFirst("textarea").ownText();
                    String indentStr = c.attr("style");
                    if(indentStr.length()>0) {
                        String indentStrSplit = indentStr.split(":")[1].split("px")[0];
                        int indentRaw = Integer.parseInt(indentStrSplit);
                        indent = indentRaw / 64;
                    }else indent = 0;
                    likes = Integer.parseInt(c.selectFirst("a.cmt-good").selectFirst("span").text());
                    level = Integer.parseInt(c.selectFirst("span.lv-icon").text());
                    comments.add(new Comment(user, timestamp, icon, content,indent, likes, level));
                }

                cs = doc.select("section.comment-media.best-comment").last().select("div.media");
                for(Element c:cs){
                    String icon, user, timestamp, content;
                    int indent, likes, level;
                    Elements i = c.select("img");
                    if(!i.isEmpty()) {
                        icon = i.get(0).attr("src");
                    }else icon = "";
                    user = c.selectFirst("span.member").ownText();
                    timestamp = c.selectFirst("span.media-info").selectFirst("span").text();
                    content = c.selectFirst("div.commtent-content").ownText();
                    String indentStr = c.attr("style");
                    indent = 0;
                    likes = Integer.parseInt(c.selectFirst("a.cmt-good").selectFirst("span").text());
                    level = Integer.parseInt(c.selectFirst("span.lv-icon").text());
                    bcomments.add(new Comment(user, timestamp, icon, content,indent, likes, level));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            if(response!=null){
                response.close();
            }
            tries++;
        }
    }


    public List<Manga> getEps() {
        return eps;
    }

    public Title getTitle() {
        return title;
    }

    public List<String> getImgs(){
        return getImgs(false);
    }
    public List<String> getImgs(Boolean second){
        if(second){
            return imgs1;
        }
        return imgs;
    }
    public List<Comment> getComments(){ return comments; }

    public List<Comment> getBestComments() { return bcomments; }

    public int getSeed() {
        return seed;
    }

    public String toString(){
        JSONObject tmp = new JSONObject();
        try {
            tmp.put("id", id);
            tmp.put("name", name);
            tmp.put("date", date);
        }catch (Exception e){

        }
        return tmp.toString();
    }

    public void setTitle(Title title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object obj) {
        return this.id == ((Manga)obj).getId();
    }
    @Override
    public int hashCode() {
        return id;
    }

    public void setOfflineName(String offlineName) {
        this.offlineName = offlineName;
    }

    public String getOfflineName(){
        return this.offlineName;
    }

    public void setListener(Listener listener){
        this.listener = listener;
    }

    public Boolean getReported() {
        return reported;
    }

    private int id;
    String name;
    List<Manga> eps;
    List<String> imgs, imgs1, cdn_domains;
    List<Comment> comments, bcomments;
    String offlineName;
    String thumb;
    Title title;
    String date;
    int seed;
    Listener listener;
    Boolean reported = false;

    public interface Listener{
        void setMessage(String msg);
    }
}


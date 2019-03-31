package ml.melun.mangaview.mangaview;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;

import ml.melun.mangaview.Preference;

public class Manga {
    String base;
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
    public String getImg(int index){return imgs.get(index);}
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


    public void fetch(String base){
        fetch(base,new HashMap<String, String>());
    }
    public void fetch(String base, Map<String, String> cookie) {
        imgs = new ArrayList<>();
        eps = new ArrayList<>();
        comments = new ArrayList<>();
        bcomments = new ArrayList<>();
        int tries = 0;
        String cookies = "";
        for(String key : cookie.keySet()){
            cookies += key + '=' + cookie.get(key) + "; ";
        }
        //get images
        while(imgs.size()==0 && tries < 2) {
            URLConnection connection = null;
            //HttpsURLConnection sconnection = null;
            BufferedReader reader = null;
            InputStream stream = null;
            Boolean ssl = false;
            try {
                URL url = new URL(base + "/bbs/board.php?bo_table=manga&wr_id="+id);
                ssl = url.getProtocol().equals("https");
                if(listener!=null) listener.setMessage("프로토콜 확인중");
                if(!ssl) {
                    connection = url.openConnection();
                    ((HttpURLConnection)connection).setRequestMethod("GET");
                }else{
                    connection = url.openConnection();
                    ((HttpsURLConnection)connection).setRequestMethod("GET");
                }
                connection.setRequestProperty("Accept-Encoding", "*");
                connection.setRequestProperty("Accept", "*");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
                connection.setRequestProperty("Cookie", cookies);
                connection.connect();
                stream = connection.getInputStream();
                if(listener!=null) listener.setMessage("페이지 읽는중");
                reader = new BufferedReader(new InputStreamReader(stream));
                //StringBuffer buffer = new StringBuffer();
                String line = "";
                String raw = "";
                while ((line = reader.readLine()) != null) {
                    //save as raw html for jsoup
                    raw += line;
                    if(line.contains("var img_list")) {
                        if(listener!=null) listener.setMessage("이미지 리스트 읽는중");
                        String imgStr = line;
                        if(imgStr!=null) {
                            String[] imgStrs = imgStr.split("\"");
                            //remove backslash
                            for (int i = 1; i < imgStrs.length; i += 2) {
                                String imgUrl = imgStrs[i].replace("\\","")
                                        .replace(".mangashow2.me", ".mangashow3.me")
                                        + "?v=2";
                                imgs.add(imgUrl);
                            }
                        }
                    }else if(line.contains("var only_chapter")){
                        if(listener!=null) listener.setMessage("화 목록 읽는중");
                        String epsStr = line;
                        String[] epsStrs = epsStr.split("\"");
                        //remove backslash
                        for (int i = 3; i < epsStrs.length; i += 4) {
                            eps.add(new Manga(Integer.parseInt(epsStrs[i]),epsStrs[i-2],""));
                        }
                    }else if(line.contains("<h1>")){
                        name = line.substring(line.indexOf('>')+1,line.lastIndexOf('<'));
                    }else if(line.contains("var view_cnt")){
                        String seedt = line.substring(0,line.length()-1);
                        seed = Integer.parseInt(seedt.split(" ")[3]);
                    }

                    //if(imgs.size()>0 && eps.size()>0) break;
                }

                System.out.println(raw);

                //jsoup parsing
                Document doc = Jsoup.parse(raw);
                //parse title
                if(title==null){
                    String href = doc.selectFirst("div.comic-navbar").select("a").get(3).attr("href");
                    String name = href.substring(href.indexOf("manga_name=")+11);
                    title = new Title(java.net.URLDecoder.decode(name, "UTF-8"),"","",new ArrayList<String>(), -1);
                }

                if(listener!=null) listener.setMessage("댓글 읽는중");

                Elements cs = doc.select("section.comment-media").last().select("div.media");
                System.out.println(cs.size());
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
                System.out.println(cs.size());
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
            } finally {
                if(ssl) ((HttpsURLConnection)connection).disconnect();
                else ((HttpURLConnection)connection).disconnect();
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //get string inside double quotes : 1,3,5,7,9,...

            /*
            Document items = Jsoup.connect("https://mangashow.me/bbs/board.php?bo_table=msm_manga&wr_id=" + id)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .get();
            //System.out.println(items.html());
            //Deprecated since 18/12/25

            for (Element e : items.selectFirst("div.view-content.scroll-viewer").select("img")) {
                imgs.add(e.attr("src"));
            }
            */
            //now its contained inside javascript var
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

    private int id;
    String name;
    List<Manga> eps;
    List<String> imgs;
    List<Comment> comments, bcomments;
    String offlineName;
    String thumb;
    Title title;
    String date;
    int seed;
    Listener listener;

    public interface Listener{
        void setMessage(String msg);
    }
}


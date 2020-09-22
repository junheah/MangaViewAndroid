package ml.melun.mangaview.mangaview;


import com.eclipsesource.v8.V8;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.*;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.Response;

    /*
    mode:
    0 = online
    1 = offline - old
    2 = offline - old(moa) (title.data)
    3 = offline - latest(toki) (title.gson)
    4 = offline - new(moa) (title.gson)
     */

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
        fetch(client, true ,null);
    }
    public void fetch(CustomHttpClient client, Map<String,String> cookies){
        fetch(client, false, cookies);
    }
    public void fetch(CustomHttpClient client, boolean doLogin, Map<String,String> cookies) {
        mode = 0;
        imgs = new ArrayList<>();
        eps = new ArrayList<>();
        comments = new ArrayList<>();
        bcomments = new ArrayList<>();
        int tries = 0;

        while(imgs.size()==0 && tries < 2) {
            Response r = client.mget("/comic/" + String.valueOf(id), false, cookies);
            try {
                String body = r.body().string();
                r.close();
                if(body.contains("Connect Error: Connection timed out")){
                    //adblock : try again
                    r.close();
                    tries = 0;
                    continue;
                }

                Document d = Jsoup.parse(body);

                //name
                name = d.selectFirst("div.toon-title").ownText();

                //temp title
                Element navbar = d.selectFirst("div.toon-nav");
                int tid = Integer.parseInt(navbar.select("a")
                        .last()
                        .attr("href")
                        .split("comic/")[1]
                        .split("\\?")[0]);

                if(title == null) title = new Title(name, "", "", null, "", tid );

                //eps
                for(Element e :navbar.selectFirst("select").select("option")){
                    String idstr = e.attr("value");
                    if(idstr!=null && idstr.length()>0)
                        eps.add(new Manga(Integer.parseInt(idstr),e.ownText(),""));
                }

                //imgs
                String script = d.select("div.view-padding").get(1).selectFirst("script").data();
                StringBuilder encodedData = new StringBuilder();
                encodedData.append('%');
                for(String line : script.split("\n")){
                    if(line.contains("html_data+=")){
                        encodedData.append(line.substring(line.indexOf('\'')+1, line.lastIndexOf('\'')).replaceAll("[.]","%"));
                    }
                }
                if(encodedData.lastIndexOf("%") == encodedData.length()-1)
                    encodedData.deleteCharAt(encodedData.length()-1);
                String imgdiv = URLDecoder.decode(encodedData.toString(), "UTF-8");

                Document id = Jsoup.parse(imgdiv);
                for(Element e : id.select("img")){
                    String style = e.attr("style");
                    if(style == null || style.length()==0) {
                        for(Attribute a : e.attributes()){
                            if(a.getKey().contains("data")){
                                String img = a.getValue();
                                if (img != null && !img.isEmpty() && !img.contains("blank") && !img.contains("loading") && !img.startsWith("/"))
                                    imgs.add(img);
                            }
                        }
                    }
                }


                //comments
                Element commentdiv = d.selectFirst("div#viewcomment");

                String user;
                String icon;
                String content;
                String timestamp;
                int likes;
                int level;
                String lvlstr;
                int indent;
                String indentstr;
                try {
                    for (Element e : commentdiv.selectFirst("section#bo_vc").select("div.media")) {
                        if (e.id().contains("c_")) {
                            // is comment

                            //indent
                            indentstr = e.attr("style");
                            if (indentstr != null && indentstr.length() > 0)
                                indent = Integer.parseInt(indentstr.substring(indentstr.lastIndexOf(':') + 1, indentstr.lastIndexOf('p'))) / 64;
                            else
                                indent = 0;

                            //icon
                            Element icone = e.selectFirst(".media-object");
                            if (icone.is("img"))
                                icon = icone.attr("src");
                            else
                                icon = "";

                            Element header = e.selectFirst("div.media-heading");
                            Element userSpan = header.selectFirst("span");
                            user = userSpan.ownText();
                            if (userSpan.hasClass("guest"))
                                level = 0;
                            else {
                                lvlstr = userSpan.selectFirst("img").attr("src");
                                level = Integer.parseInt(lvlstr.substring(lvlstr.lastIndexOf('/') + 1, lvlstr.lastIndexOf('.')));
                            }
                            timestamp = header.selectFirst("span.media-info").ownText();

                            Element cbody = e.selectFirst("div.media-content");
                            content = cbody.ownText();
                            likes = Integer.parseInt(cbody.selectFirst("div.cmt-good-btn").selectFirst("span").ownText());
                            comments.add(new Comment(user, timestamp, icon, content, indent, likes, level));
                        }

                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            if(r!=null){
                r.close();
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
        if(mode == 0) {
            return imgs;
        }else{
            if(imgs == null) {
                imgs = new ArrayList<>();
                //is offline : read image list
                File[] offimgs = null;
                switch (mode) {
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        offimgs = offlinePath.listFiles();
                        break;
                }
                Arrays.sort(offimgs);
                for (File img : offimgs) {
                    imgs.add(img.getAbsolutePath());
                }
            }
             return imgs;
        }
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

    public void setOfflinePath(File offlinePath) {
        this.offlinePath = offlinePath;
    }

    public File getOfflinePath(){
        return this.offlinePath;
    }

    public void setListener(Listener listener){
        this.listener = listener;
    }

    public int getMode(){return mode;}

    public void setMode(int mode){
        this.mode = mode;
    }

    public String getUrl(){
        return "/manga/" + id;
    }

    public boolean useBookmark(){
        return id>0&&(mode==0||mode==3);
    }

    public boolean isOnline(){
        return id>0&&mode==0;
    }

    private int id;
    String name;
    List<Manga> eps;
    List<String> imgs;
    List<Comment> comments, bcomments;
    File offlinePath;
    String thumb;
    Title title;
    String date;
    int seed;
    int mode;
    Listener listener;

    public interface Listener{
        void setMessage(String msg);
    }
}


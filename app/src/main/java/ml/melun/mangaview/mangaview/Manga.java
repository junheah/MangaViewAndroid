package ml.melun.mangaview.mangaview;


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
            Response r = client.mget("/comic/" + String.valueOf(id));
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
                        .get(3)
                        .attr("href")
                        .split("comic/")[1]
                        .split("\\?")[0]);
                System.out.println(tid);

                if(title == null) title = new Title(name, "", "", null, "", tid );

                //eps
                for(Element e :navbar.selectFirst("select").select("option")){
                    eps.add(new Manga(Integer.parseInt(e.attr("value")),e.ownText(),""));
                }

                //imgs
                for(Element e : d.selectFirst("div.view-padding").select("img")) {
                    String img = e.attr("data-original");
                    if(img != null && !img.isEmpty() && !img.contains("blank") && !img.contains("loading") && !img.startsWith("/"))
                        imgs.add(img);
                }

                //comments
                //Element commentE = d.selectFirst("viewcomment");

                //todo: comments


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


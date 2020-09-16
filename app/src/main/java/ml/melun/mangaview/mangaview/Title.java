package ml.melun.mangaview.mangaview;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import ml.melun.mangaview.Preference;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.HashingSource;


public class Title extends MTitle {
    private List<Manga> eps = null;
    int bookmark = 0;
    Boolean bookmarked = false;
    String bookmarkLink = "";
    int rc = 0;

    public static final int BATTERY_EMPTY = 0;
    public static final int BATTERY_ONE_QUARTER = 1;
    public static final int BATTERY_HALF = 2;
    public static final int BATTERY_THREE_QUARTER = 3;
    public static final int BATTERY_FULL = 4;

    public Title(String n, String t, String a, List<String> tg, String r, int id) {
        super(n, id, t, a, tg, r);
    }

    public Title(String n, String t, String a, List<String> tg, String r, int id, int rc) {
        super(n, id, t, a, tg, r);
        this.id = id;
        this.rc = rc;
    }

    public Title(MTitle title){
        super(title.getName(), title.getId(), title.getThumb(), title.getAuthor(), title.getTags(), title.getRelease());
    }


    public List<Manga> getEps(){
        return eps;
    }

    public Boolean getBookmarked() {
        if(bookmarked==null) return false;
        return bookmarked;
    }

    public void fetchEps(CustomHttpClient client) {
        try {
            Response r = client.mget("/comic/" + id);
            String body = r.body().string();
            if(body.contains("Connect Error: Connection timed out")){
                //adblock : try again
                r.close();
                fetchEps(client);
                return;
            }
            Document d = Jsoup.parse(body);
            Element header = d.selectFirst("div.view-title");

            //extra info
            try{
                Element infoTable = d.selectFirst("table.table");
                //recommend
                rc = Integer.parseInt(infoTable.selectFirst("button.btn-red").selectFirst("b").ownText());
                //bookmark
                Element bookmark = infoTable.selectFirst("a#webtoon_bookmark");
                if(bookmark != null) {
                    //logged in
                    bookmarked = bookmark.hasClass("btn-orangered");
                    bookmarkLink = bookmark.attr("href");
                }else{
                    //not logged in
                    bookmarked = false;
                    bookmarkLink = "";
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            //thumb
            try {
                thumb = header.selectFirst("div.view-img").selectFirst("img").attr("src");
            }catch (Exception e){}

            Elements infos = header.select("div.view-content");
            //title
            try {
                name = infos.get(1).selectFirst("b").ownText();
            }catch (Exception e){}
            tags = new ArrayList<>();

            for(int i=1; i<infos.size(); i++){
                Element e = infos.get(i);
                try {
                    String type = e.selectFirst("strong").ownText();
                    if(type.equals("작가")){
                        author = e.selectFirst("a").ownText();
                    }else if(type.equals("분류")){
                        for(Element t: e.select("a"))
                            tags.add(t.ownText());
                    }else if(type.equals("발행구분")) {
                        release = e.selectFirst("a").ownText();
                    }

                }catch (Exception e2){continue;}
            }

            //eps
            String title, date;
            Manga tmp;
            int id;
            eps = new ArrayList<>();
            try{
                for(Element e : d.selectFirst("ul.list-body").select("li.list-item")) {
                    Element titlee = e.selectFirst("a.item-subject");
                    id = Integer.parseInt(titlee.attr("href").split("comic/")[1].split("\\?")[0]);
                    title = titlee.ownText();

                    Elements infoe = e.selectFirst("div.item-details").select("span");
                    date = infoe.get(0).ownText();
                    //has view-count, thumb-count and other extra info, implement later
                    tmp = new Manga(id, title, date);
                    tmp.setMode(0);
                    eps.add(tmp);
                }
            }catch (Exception e){}
            r.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean toggleBookmark(CustomHttpClient client, Preference p){
        RequestBody requestBody = new FormBody.Builder()
                .addEncoded("mode", bookmarked?"off":"on")
                .addEncoded("top","0")
                .addEncoded("js","on")
                .build();

        Map<String, String> headers = new HashMap<>();
        headers.put("Cookie", p.getLogin().getCookie(true));
        Response r = client.post(bookmarkLink, requestBody, headers);
        try {
            JSONObject obj = new JSONObject(r.body().string());
            if(obj.getString("error").isEmpty() && !obj.getString("success").isEmpty()){
                //success
                bookmarked = !bookmarked;
            }else{
                //failed
                r.close();
                return false;
            }
        }catch (Exception e){
            if(r!=null) r.close();
            e.printStackTrace();
            return false;
        }
        if(r!=null) r.close();
        return true;
    }


    public int getBookmark(){
        return bookmark;
    }
    public int getEpsCount(){ return eps.size();}

    public Boolean isNew() throws Exception{
        if(eps!=null){
            return eps.get(0).getName().split(" ")[0].contains("NEW");
        }else{
            throw new Exception("not loaded");
        }
    }

    public void setEps(List<Manga> list){
        eps = list;
    }

    public void removeEps(){
        if(eps!=null) eps.clear();
    }

    public void setBookmark(int b){bookmark = b;}


    @Override
    public Title clone(){
        return new Title(name, thumb, author, tags, release, id);
    }

    public int getRecommend_c() {
        return rc;
    }

    public void setRecommend_c(int recommend_c) {
        this.rc = recommend_c;
    }

    public MTitle minimize(){
        return new MTitle(name, id, thumb, author, tags, release);
    }

    public boolean hasCounter(){
        return !(rc==0&&(bookmarkLink==null||bookmarkLink.length()==0));
    }

    public static boolean isInteger(String s) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),10) < 0) return false;
        }
        return true;
    }

    public boolean useBookmark(){
        return !isInteger(release);
    }

}


package ml.melun.mangaview.mangaview;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import okhttp3.Response;


public class Title extends MTitle {
    private List<Manga> eps = null;
    int bookmark = 0;
    Boolean bookmarked = false;
    String bookmarkLink = "";
    int bc = 0;
    int rc = 0;
    int cc = 0;
    int bmc = 0;

    public static final int BATTERY_EMPTY = 0;
    public static final int BATTERY_ONE_QUARTER = 1;
    public static final int BATTERY_HALF = 2;
    public static final int BATTERY_THREE_QUARTER = 3;
    public static final int BATTERY_FULL = 4;

    public Title(String n, String t, String a, List<String> tg, String r, int id) {
        super(n, id, t, a, tg, r);
    }

    public Title(String n, String t, String a, List<String> tg, String r, int id, int rc, int bc, int cc, int bmc) {
        super(n, id, t, a, tg, r);
        this.id = id;
        this.rc = rc;
        this.bc = bc;
        this.cc = cc;
        this.bmc = bmc;
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
            Document d = Jsoup.parse(r.body().string());
            Element header = d.selectFirst("div.view-title");

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

                    eps.add(new Manga(id, title, date));
                }
            }catch (Exception e){}
            r.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void toggleBookmark(CustomHttpClient client){
        Response r = client.mget(bookmarkLink,true);
        if(r!=null) r.close();
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

    public int getBattery_c() {
        return bc;
    }

    public void setBattery_c(int battery_c) {
        this.bc = battery_c;
    }

    public int getRecommend_c() {
        return rc;
    }

    public void setRecommend_c(int recommend_c) {
        this.rc = recommend_c;
    }

    public int getComment_c() {
        return cc;
    }

    public void setComment_c(int comment_c) {
        this.cc = comment_c;
    }

    public int getBookmark_c() {
        return bmc;
    }

    public void setBookmark_c(int bookmark_c) {
        this.bmc = bookmark_c;
    }

    public String getBattery_s(){
        return (bc *25)+"%";
    }

    public boolean hasCounter(){
        return !(bc==0 && rc==0 && cc==0 && bmc==0);
    }

    public MTitle minimize(){
        return new MTitle(name, id, thumb, author, tags, release);
    }


}


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

    public static final int BATTERY_EMPTY = 0;
    public static final int BATTERY_ONE_QUARTER = 1;
    public static final int BATTERY_HALF = 2;
    public static final int BATTERY_THREE_QUARTER = 3;
    public static final int BATTERY_FULL = 4;

    public Title(String n, String t, String a, List<String> tg, int r, int id) {
        super(n, id, t, a, tg, r);
    }

    public Title(String n, String t, String a, List<String> tg, int r, int id, int rc, int bc, int cc, int bmc) {
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
        //fetch episodes
        try {
//            if(id<0){
//                this.id = getIdWithName(client, this.name);
//                if(this.id<0) return;
//            }

            eps = new ArrayList<>();
            //now uses id, not name
            //Response response = client.mget("/bbs/page.php?hid=manga_detail&manga_name="+ URLEncoder.encode(name,"UTF-8"));
            Response response = client.mget("/bbs/page.php?hid=manga_detail&manga_id="+id);
            Document items = Jsoup.parse(response.body().string());
            StringBuilder nameBuilder = new StringBuilder();
            for(Element e:items.select("div.slot")) {
                nameBuilder.setLength(0);
                for(Element child : e.selectFirst("div.title").getAllElements()){
                    if(!child.tag().toString().contains("span"))
                        nameBuilder.append(child.ownText());
                }
                eps.add(new Manga(Integer.parseInt(e.attr("data-wrid"))
                        ,nameBuilder.toString()
                        ,e.selectFirst("div.addedAt").ownText().split(" ")[0]));
            }
            thumb = items.selectFirst("div.manga-thumbnail").attr("style").split("\\(")[1].split("\\)")[0];

            name = items.selectFirst("div.manga-subject").selectFirst("div.title").text();
            try {
                author = items.selectFirst("a.author").ownText();
            }catch (Exception e){
                //noauthor
            }
            tags = new ArrayList<>();
            for(Element e:items.selectFirst("div.manga-tags").select("a.tag")){
                tags.add(e.ownText());
            }
            try{
                String releaseRaw =  items.selectFirst("div.manga-thumbnail").selectFirst("a.publish_type").attr("href");
                release = Integer.parseInt(releaseRaw.substring(releaseRaw.lastIndexOf('=') + 1));
            }catch (Exception e){

            }

            //fetch recommend buttons
            Element btns = items.selectFirst("div.btns");
            try{
                //rc
                rc = Integer.parseInt(btns.selectFirst("i.fa.fa-thumbs-up").ownText().replaceAll(",",""));
            }catch (Exception e){
                rc = -1;
                //e.printStackTrace();
            }

            try {
                //bc
                String battery = btns.selectFirst("div.recommend.red").getAllElements().last().className();
                if(battery.contains("battery-empty")){
                    this.bc = BATTERY_EMPTY;
                }else if(battery.contains("battery-quarter")){
                    this.bc = BATTERY_ONE_QUARTER;
                }else if(battery.contains("battery-half")) {
                    this.bc = BATTERY_HALF;
                }else if(battery.contains("battery-three-quarters")){
                    this.bc = BATTERY_THREE_QUARTER;
                }else if(battery.contains("battery-full")){
                    this.bc = BATTERY_FULL;
                }
            }catch (Exception e){
                this.bc = -1;
                //e.printStackTrace();
            }

            //cc
            try{
                cc = Integer.parseInt(btns.selectFirst("i.fa.fa-comment").ownText().replaceAll(",",""));
            }catch (Exception e){
                e.printStackTrace();
            }

            //bmc
            try{
                bmc = Integer.parseInt(btns.selectFirst("i.fa.fa-bookmark").ownText().replaceAll(",",""));
            }catch (Exception e){
                e.printStackTrace();
            }


            Element bookmark = items.selectFirst("div.favorit");
            bookmarked = bookmark.attr("class").contains("active");
            bookmarkLink = bookmark.attr("onclick").split("='")[1].split("'")[0];

            response.close();
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

    private List<Manga> eps = null;
    int bookmark =0;
    Boolean bookmarked = false;
    String bookmarkLink = "";
    int bc = 0;
    int rc = 0;
    int cc = 0;
    int bmc = 0;
}


package ml.melun.mangaview.mangaview;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import okhttp3.Response;

import static ml.melun.mangaview.Utils.getIdWithName;

public class Title {

    public Title(String n, String t, String a, List<String> tg, int r) {
        name = n;
        thumb = t;
        author = a;
        tags = tg;
        release = r;
        id = -1;
    }

    public Title(String n, String t, String a, List<String> tg, int r, int id) {
        name = n;
        thumb = t;
        author = a;
        tags = tg;
        release = r;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return ((Title)obj).getId() == this.id;
    }

    public String getName() {
        return name;
    }
    public String getThumb() {
        return thumb;
    }
    public List<Manga> getEps(){
        return eps;
    }
    public int getRelease() { return release; }

    public Boolean getBookmarked() {
        if(bookmarked==null) return false;
        return bookmarked;
    }

    public void fetchEps(CustomHttpClient client) {
        //fetch episodes
        try {
            System.out.println(id);
            if(id<0){
                this.id = getIdWithName(client, this.name);
                if(this.id<0) return;
            }

            eps = new ArrayList<>();
            //now uses id, not name
            //Response response = client.mget("/bbs/page.php?hid=manga_detail&manga_name="+ URLEncoder.encode(name,"UTF-8"));
            Response response = client.mget("/bbs/page.php?hid=manga_detail&manga_id="+id);
            Document items = Jsoup.parse(response.body().string());
            for(Element e:items.select("div.slot")) {
                eps.add(new Manga(Integer.parseInt(e.attr("data-wrid"))
                        ,e.selectFirst("div.title").ownText()
                        ,e.selectFirst("div.addedAt").ownText().split(" ")[0]));
            }
            thumb = items.selectFirst("div.manga-thumbnail").attr("style").split("\\(")[1].split("\\)")[0];
            System.out.println("pppp"+thumb);
            name = items.selectFirst("div.manga-subject").selectFirst("div.title").ownText();
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


    public String getAuthor(){
        if(author==null) return "";
        return author;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }
    public int getBookmark(){
        return bookmark;
    }
    public int getEpsCount(){ return eps.size();}
    public List<String> getTags(){
        if(tags==null) return new ArrayList<>();
        return tags;
    }
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

    public void setId(int id) {
        this.id = id;
    }

    private String name;
    private int id;
    private String thumb;
    private List<Manga> eps;
    int bookmark;
    String author;
    List<String> tags;
    int release;
    Boolean bookmarked;
    String bookmarkLink;
}


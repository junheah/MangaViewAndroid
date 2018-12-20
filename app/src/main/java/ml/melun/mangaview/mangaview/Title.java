package ml.melun.mangaview.mangaview;
import java.util.ArrayList;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Title {
    public Title(String n, String t) {
        name = n;
        thumb = t;
    }
    public String getName() {
        return name;
    }
    public String getThumb() {
        return thumb;
    }
    public ArrayList<Manga> getEps(){
        return eps;
    }
    public void fetchEps() {
        //fetch episodes
        try {
            eps = new ArrayList<>();
            Document items = Jsoup.connect("https://mangashow.me/bbs/page.php?hid=manga_detail&manga_name="+name)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .get();
            for(Element e:items.select("div.slot")) {
                eps.add(0,new Manga(Integer.parseInt(e.attr("data-wrid")),e.selectFirst("div.title").text()));
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    public Boolean clicked(){
        if(selected) return true;
        else return false;
    }
    public void click(){
        if(selected) selected=false;
        else selected = true;
    }
    private Boolean selected = false;
    private String name;
    private String thumb;
    private ArrayList<Manga> eps;
}


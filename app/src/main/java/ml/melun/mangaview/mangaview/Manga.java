package ml.melun.mangaview.mangaview;
import java.util.ArrayList;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Manga {
    public Manga(int i, String n) {
        id = i;
        name = n;
    }
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public void fetch() {
        imgs = new ArrayList<>();
        //get images
        try {
            Document items = Jsoup.connect("https://mangashow.me/bbs/board.php?bo_table=msm_manga&wr_id="+id).get();
            for(Element e:items.selectFirst("div.view-content.scroll-viewer").select("img")) {
                imgs.add(e.attr("src"));
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    public ArrayList<String> getImgs(){
        return imgs;
    }
    private int id;
    String name;
    private ArrayList<String> imgs;
}


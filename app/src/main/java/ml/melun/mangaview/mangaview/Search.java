package ml.melun.mangaview.mangaview;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class Search {
    public Search(String q) {
        query = q;
        fetch();
    }

    public void fetch() {
        result = new ArrayList<>();
        try {
            Document search = Jsoup.connect("https://mangashow.me/bbs/search.php?stx="+query).get();
            Elements items = search.select("div.post-row");
            for(Element item: items) {
                result.add(new Title(item.selectFirst("div.img-item").selectFirst("img").attr("alt"),
                        item.selectFirst("div.img-item").selectFirst("img").attr("src")));
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Title> getResult(){
        return result;
    }

    private String query;
    private ArrayList<Title> result;
}

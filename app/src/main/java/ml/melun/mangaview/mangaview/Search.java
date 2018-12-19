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
            //검색결과 페이지당 30개
            //stx=쿼리, page=0~
            int page = 0;
            while(true) {
                Document search = Jsoup.connect("https://mangashow.me/bbs/search.php?stx=" + query + "&page="+page).get();
                Elements items = search.select("div.post-row");
                if(items.size()<1) break;
                for (Element item : items) {
                    result.add(new Title(item.selectFirst("div.img-item").selectFirst("img").attr("alt"),
                            item.selectFirst("div.img-item").selectFirst("img").attr("src")));
                }
                if(items.size()==30) page++;
                else break;
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

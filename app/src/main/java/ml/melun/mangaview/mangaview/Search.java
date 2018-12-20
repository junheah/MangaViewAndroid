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
                    result.add(new Title(removeParenthesis(item.selectFirst("div.img-item").selectFirst("img").attr("alt")),
                            removeParenthesis(item.selectFirst("div.img-item").selectFirst("img").attr("src"))));
                }
                if(items.size()==30) page++;
                else break;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    public String removeParenthesis(String input){
        int i = input.indexOf('(');
        int j = input.indexOf(')');
        if(i>-1||j>-1){
            char[] tmp = input.toCharArray();
            if(i>-1) tmp[i] = ' ';
            if(j>-1) tmp[j] = ' ';
            input = String.valueOf(tmp);
        }
        return input;
    }

    public ArrayList<Title> getResult(){
        return result;
    }

    private String query;
    private ArrayList<Title> result;
}

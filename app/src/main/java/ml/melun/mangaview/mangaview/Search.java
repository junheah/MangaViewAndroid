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
                Document search = Jsoup.connect("https://mangashow.me/bbs/search.php?stx=" + query + "&page="+page)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .get();
                System.out.println(search.toString());
                Elements items = search.select("div.post-row");
                if(items.size()<1) break;
                for (Element item : items) {
                    String ntmp = (item.selectFirst("div.manga-subject").selectFirst("a").text());
                    String ttmp = (item.selectFirst("div.img-wrap-back").attr("style").split("\\(")[1].split("\\)")[0]);
                    result.add(new Title(ntmp, ttmp));
                }
                if(items.size()==30) page++;
                else break;
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    public String filter(String input){
        int i = input.indexOf('(');
        int j = input.indexOf(')');
        int m = input.indexOf('?');
        if(i>-1||j>-1||m>-1){
            char[] tmp = input.toCharArray();
            if(i>-1) tmp[i] = ' ';
            if(j>-1) tmp[j] = ' ';
            if(m>-1) tmp[m] = ' ';
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

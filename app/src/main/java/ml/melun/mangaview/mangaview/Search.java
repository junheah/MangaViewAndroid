package ml.melun.mangaview.mangaview;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Search {
    /* mode
    * 0 : 제목
    * 1 : 작가
    * 2 : 태그
    * 3 : 글자
    * 4 : 발행
    * 6 : null
    * 6 : 종합
     */
    public Search(String q, int mode) {
        query = q;
        this.mode = mode;
    }

    public Boolean isLast() {
        return last;
    }

    public void fetch() {
        result = new ArrayList<>();
        if(!last) {
            try {
                //검색결과 페이지당 30개
                //stx=쿼리, page=0~
                page++;
                String searchUrl = "";
                switch(mode){
                    case 0:
                        searchUrl = "https://mangashow.me/bbs/search.php?stx=";
                        break;
                    case 1:
                        searchUrl = "https://mangashow.me/bbs/page.php?hid=manga_list&page=0&sfl=4&stx=";
                        break;
                    case 2:
                        searchUrl = "https://mangashow.me/bbs/page.php?hid=manga_list&sfl=3&stx=";
                        break;
                    case 3:
                        searchUrl = "https://mangashow.me/bbs/page.php?hid=manga_list&sfl=1&stx=";
                        break;
                    case 4:
                        searchUrl = "https://mangashow.me/bbs/page.php?hid=manga_list&sfl=2&stx=";
                        break;
                    case 6:
                        searchUrl = "https://mangashow.me/bbs/page.php?hid=manga_list&";
                        break;
                }

                System.out.println("ppppppppppp\n"+searchUrl+query);
                Document search = Jsoup.connect(searchUrl + query + "&page=" + page)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
                        .get();
                Elements items = search.select("div.post-row");
                if (items.size() < 1) last = true;

                for (Element item : items) {
                    String ntmp = (item.selectFirst("div.manga-subject").selectFirst("a").text());
                    String ttmp = (item.selectFirst("div.img-wrap-back").attr("style").split("\\(")[1].split("\\)")[0]);
                    String atmp = "";
                    try {
                        atmp = item.selectFirst("div.author").selectFirst("div").text();
                    } catch (Exception e) {

                    }
                    List<String> tags = new ArrayList<>();
                    try {
                        tags = item.selectFirst("div.tags").select("a").eachText();
                    } catch (Exception e) {
                    }
                    result.add(new Title(ntmp, ttmp, atmp, tags));
                }
                if (items.size() < 30) last = true;

            } catch (Exception e) {
                page--;
                e.printStackTrace();
            }
        }
    }
//    public String filter(String input){
//        int i = input.indexOf('(');
//        int j = input.indexOf(')');
//        int m = input.indexOf('?');
//        if(i>-1||j>-1||m>-1){
//            char[] tmp = input.toCharArray();
//            if(i>-1) tmp[i] = ' ';
//            if(j>-1) tmp[j] = ' ';
//            if(m>-1) tmp[m] = ' ';
//            input = String.valueOf(tmp);
//        }
//        return input;
//    }

    public ArrayList<Title> getResult(){
        return result;
    }

    private String query;
    Boolean last = false;
    int mode;
    int page = -1;
    private ArrayList<Title> result;
}

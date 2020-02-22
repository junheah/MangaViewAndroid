package ml.melun.mangaview.mangaview;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

public class Search {
    /* mode
    * 0 : 제목
    * 1 : 작가
    * 2 : 태그
    * 3 : 글자
    * 4 : 발행
    * 5 : null
    * 6 : 종합
     */
    public Search(String q, int mode) {
        query = q;
        this.mode = mode;
        //if(mode==6) query = "";
    }

    public Boolean isLast() {
        return last;
    }
    // not used in android version since we use seperate TagSearch activity
    // which requires mode and single query value
    /*
    String q0= "", q1 = "", q2 = "", q3 = "";
    
    public void addQuery(int i, String q) {
    	switch(i) {
    	case 0:
    		if(q0.length()>0) q0 += ","+q;
    		else q0 = q;
    		break;
    	case 1:
    		if(q1.length()>0) q1 += ","+q;
    		else q1 = q;
    		break;
    	case 2:
    		if(q2.length()>0) q2 += ","+q;
    		else q2 = q;
    		break;
    	case 3:
    		if(q3.length()>0) q3 += ","+q;
    		else q3 = q;
    		break;
    	}
    	
    }
    */

    public int fetch(CustomHttpClient client) {
        result = new ArrayList<>();
        if(!last) {
            try {
                //검색결과 페이지당 30개
                //stx=쿼리, page=0~
                page++;
                String searchUrl = "";
                switch(mode){
                    case 0:
                        searchUrl = "/bbs/search.php?stx=";
                        break;
                    case 1:
                        searchUrl = "/bbs/page.php?hid=manga_list&sfl=4&stx=";
                        break;
                    case 2:
                        searchUrl = "/bbs/page.php?hid=manga_list&sfl=3&stx=";
                        break;
                    case 3:
                        searchUrl = "/bbs/page.php?hid=manga_list&sfl=1&stx=";
                        break;
                    case 4:
                        searchUrl = "/bbs/page.php?hid=manga_list&sfl=2&stx=";
                        break;
                    case 6:
                        searchUrl = "/bbs/page.php?hid=manga_list&";
                        break;
                }


                Response response = client.mget(searchUrl + query + "&page=" + page);
                Document search = Jsoup.parse(response.body().string());
                Elements items = search.select("div.post-row");
                if(response.code()>=400){
                    //has error
                    return 1;
                } else if (items.size() < 1)
                    last = true;

                for (Element item : items) {
                    Element manga_subject = item.selectFirst("div.manga-subject").selectFirst("a");
                    String ntmp = (manga_subject.ownText());
                    String idtmp = manga_subject.attr("href").split("manga_id=")[1];
                    String ttmp = (item.selectFirst("div.img-wrap-back").attr("style").split("\\(")[1].split("\\)")[0]);
                    String atmp = "";
                    try {
                        atmp = item.selectFirst("div.author").selectFirst("div").text();
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                    List<String> tags = new ArrayList<>();
                    try {
                        tags = item.selectFirst("div.tags").select("a").eachText();
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                    int release = -1;
                    try{
                        release = Integer.parseInt(item.selectFirst("div.publish-type").attr("onclick").split("\\(")[1].split("\\)")[0]);
                    }catch (Exception e){
                        //e.printStackTrace();
                    }
                    result.add(new Title(ntmp, ttmp, atmp, tags, release, Integer.parseInt(idtmp)));
                }
                if (items.size() < 30) last = true;
                response.close();

                if(result.size()==0)
                    page--;

            } catch (Exception e) {
                page--;
                e.printStackTrace();
                return 1;
            }
        }
        return 0;
    }


    public ArrayList<Title> getResult(){
        return result;
    }

    private String query;
    Boolean last = false;
    int mode;
    int page = -1;
    private ArrayList<Title> result;
}

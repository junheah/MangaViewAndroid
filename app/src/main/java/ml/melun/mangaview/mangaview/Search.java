package ml.melun.mangaview.mangaview;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

import static ml.melun.mangaview.mangaview.Title.BATTERY_EMPTY;
import static ml.melun.mangaview.mangaview.Title.BATTERY_FULL;
import static ml.melun.mangaview.mangaview.Title.BATTERY_HALF;
import static ml.melun.mangaview.mangaview.Title.BATTERY_ONE_QUARTER;
import static ml.melun.mangaview.mangaview.Title.BATTERY_THREE_QUARTER;

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
                        searchUrl = "/bbs/page.php?hid=manga_list&sfl=5&stx=";
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
                search.outputSettings().charset(Charset.forName("UTF-8"));
                Elements items = search.select("div.post-row");
                if(response.code()>=400){
                    //has error
                    return 1;
                } else if (items.size() < 1)
                    last = true;

                for (Element item : items) {
                    Element manga_subject = item.selectFirst("div.manga-subject").selectFirst("a");
                    String ntmp = manga_subject.text();
                    String idtmp = manga_subject.attr("href").split("manga_id=")[1];
                    String ttmp = (item.selectFirst("div.img-wrap-back").attr("style").split("\\(")[1].split("\\)")[0]);
                    String atmp = "";

                    Element ae = item.selectFirst("div.author");
                    if(ae!= null)
                        atmp = ae.selectFirst("div").text();

                    List<String> tags = new ArrayList<>();

                    Element te = item.selectFirst("div.tags");
                    if(te != null)
                        tags = te.select("a").eachText();

                    int release = -1;

                    Element re = item.selectFirst("div.publish-type");
                    if(re != null)
                        release = Integer.parseInt(re.attr("onclick").split("\\(")[1].split("\\)")[0]);

                    int recommend_c=-1, battery_c=-1, comment_c=-1, bookmark_c=-1;
                    //fetch recommend buttons
                    try{
                        //rc
                        recommend_c = Integer.parseInt(item.selectFirst("i.fa.fa-thumbs-up").ownText().replaceAll(",",""));
                    }catch (Exception e){
                        recommend_c = -1;
                        e.printStackTrace();
                    }

                    try {
                        //bc
                        String battery = item.selectFirst("i.fa.fa-smile-o").getAllElements().last().className();
                        if(battery.contains("battery-empty")){
                            battery_c = BATTERY_EMPTY;
                        }else if(battery.contains("battery-quarter")){
                            battery_c = BATTERY_ONE_QUARTER;
                        }else if(battery.contains("battery-half")) {
                            battery_c = BATTERY_HALF;
                        }else if(battery.contains("battery-three-quarters")){
                            battery_c = BATTERY_THREE_QUARTER;
                        }else if(battery.contains("battery-full")){
                            battery_c = BATTERY_FULL;
                        }
                    }catch (Exception e){
                        battery_c = -1;
                        e.printStackTrace();
                    }

                    //cc
                    try{
                        comment_c = Integer.parseInt(item.selectFirst("i.fa.fa-comment").ownText().replaceAll(",",""));
                    }catch (Exception e){

                    }

                    //bmc
                    try{
                        bookmark_c = Integer.parseInt(item.selectFirst("i.fa.fa-bookmark").ownText().replaceAll(",",""));
                    }catch (Exception e){

                    }

                    result.add(new Title(ntmp, ttmp, atmp, tags, release, Integer.parseInt(idtmp), recommend_c, battery_c, comment_c, bookmark_c));
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

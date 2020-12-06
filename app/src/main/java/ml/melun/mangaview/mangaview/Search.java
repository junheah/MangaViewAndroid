package ml.melun.mangaview.mangaview;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;

import okhttp3.Response;

import static ml.melun.mangaview.mangaview.MTitle.baseModeStr;

public class Search {
    /* mode
    * 0 : 제목
    * 1 : 작가
    * 2 : 태그
    * 3 : 글자
    * 4 : 발행
    * 5 : null
    * 6 : 종합
    * 7 : (웹툰)제목
    * 8 : (웹툰)작가
    * 9 : (웹툰)태그
    * 10 : (웹툰)글자
    * 11 : (웹툰)발행
    * 12 : (웹툰)null
    * 13 : (웹툰)종합
     */

    int baseMode;
    public Search(String q, int mode, int baseMode) {
        query = q;
        this.mode = mode;
        this.baseMode = baseMode;
        //if(mode==6) query = "";
    }


    public int getBaseMode() {
        return baseMode;
    }

    public Boolean isLast() {
        return last;
    }


    public int fetch(CustomHttpClient client) {
        result = new ArrayList<>();
        if(!last) {
            try {
                //검색결과 페이지당 210개
                //stx=쿼리, page=0~
                String searchUrl = "";
                switch(mode){
                    //todo add more modes
                    case 0:
                        searchUrl = "?bo_table="+baseModeStr(baseMode)+"&stx=";
                        break;
                    case 1:
                        searchUrl = "?bo_table="+baseModeStr(baseMode)+"&artist=";
                        break;
                    case 2:
                        searchUrl = "?bo_table="+baseModeStr(baseMode)+"&tag=";
                        break;
                    case 3:
                        searchUrl = "?bo_table="+baseModeStr(baseMode)+"&jaum=";
                        break;
                    case 4:
                        searchUrl = "?bo_table="+baseModeStr(baseMode)+"&publish=";
                        break;
                }


                Response response = client.mget('/'+baseModeStr(baseMode)+"/p" + page++ + searchUrl + URLEncoder.encode(query,"UTF-8"), true, null);
                String body = response.body().string();
                if(body.contains("Connect Error: Connection timed out")){
                    //adblock : try again
                    response.close();
                    page--;
                    return fetch(client);
                }
                Document d = Jsoup.parse(body);
                d.outputSettings().charset(Charset.forName("UTF-8"));

                Elements titles = d.select("div.list-item");

                if(response.code()>=400){
                    //has error
                    return 1;
                } else if (titles.size() < 1)
                    last = true;

                String title;
                String thumb;
                String author;
                String release;
                int id;

                for(Element e : titles) {
                    try {
                        Element infos = e.selectFirst("div.img-item");
                        Element infos2 = infos.selectFirst("div.in-lable");

                        id = Integer.parseInt(infos2.attr("rel"));
                        title = infos2.selectFirst("span").ownText();
                        thumb = infos.selectFirst("img").attr("src");

                        Element ae = e.selectFirst("div.list-artist");
                        if (ae != null) author = ae.selectFirst("a").ownText();
                        else author = "";

                        Element re = e.selectFirst("div.list-publish");
                        if (re != null) release = re.selectFirst("a").ownText();
                        else release = "";

                        result.add(new Title(title, thumb, author, null, release, id, baseMode));
                    }catch (Exception e2){
                        e2.printStackTrace();
                    }
                }
                response.close();
                if (result.size() < 70)
                    last = true;


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
    int page = 1;
    private ArrayList<Title> result;
}

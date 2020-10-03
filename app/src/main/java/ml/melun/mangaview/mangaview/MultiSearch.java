package ml.melun.mangaview.mangaview;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

import static ml.melun.mangaview.mangaview.MTitle.baseModeStr;
import static ml.melun.mangaview.mangaview.MTitle.base_comic;
import static ml.melun.mangaview.mangaview.MTitle.base_webtoon;

public class MultiSearch {

    int srows = 100;
    String baseUrl = "/bbs/search.php?";
    int page = 1;
    boolean last = false;
    String query;
    List<Title> result;

    public MultiSearch(String query){
        this.query = query;
    }

    public void fetch(CustomHttpClient client){
        if(!last) {
            result = new ArrayList<>();
            int tries = 0;
            String url = baseUrl + "stx=" + query + "&srows=" + srows + "&page=" + page++;
            try {
                Response r = client.mget(url);
                String body = r.body().string();
                if(body.contains("Connect Error: Connection timed out")){
                    //adblock : try again
                    r.close();
                    page--;
                    fetch(client);
                    return;
                }

                Document d = Jsoup.parse(body);

                String thumb,desc,title,author,urltmp;
                int id;
                int baseMode;

                Elements titles = d.select("div.media");
                for(Element e : titles){
                    thumb = e.selectFirst("img").attr("src");
                    desc = e.selectFirst("span.text-muted").ownText();
                    author = e.selectFirst("div.text-muted").ownText();
                    title = e.selectFirst("b").text();

                    urltmp = e.selectFirst("a").attr("href");

                    id = Integer.parseInt(urltmp.split("id=")[1]);

                    if(urltmp.contains("comic")) baseMode = base_comic;
                    else baseMode = base_webtoon;

                    result.add(new Title(title,thumb,author,null,"",id,baseMode));
                }

                if(result.size()<srows)
                    last = true;





            } catch (Exception e) {
                e.printStackTrace();
                page--;
                return;
            }
        }
    }
}

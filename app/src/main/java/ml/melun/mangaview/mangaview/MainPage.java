package ml.melun.mangaview.mangaview;

import android.content.Context;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.melun.mangaview.Preference;


public class MainPage {
    List<Manga> recent, ranking, favUpdate, onlineRecent;
    void fetch(String url,Map<String,String> cookie) {

        recent = new ArrayList<>();
        ranking = new ArrayList<>();
        favUpdate = new ArrayList<>();
        onlineRecent = new ArrayList<>();


        try{
            Document doc = Jsoup.connect(url)
                    .cookies(cookie)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
                    .get();
            Elements list = doc.selectFirst("div.msm-post-gallery").select("div.post-row");
            for(Element e:list){
                String[] tmp_idStr = e.selectFirst("a").attr("href").toString().split("=");
                int tmp_id = Integer.parseInt(tmp_idStr[tmp_idStr.length-1]);
                String tmp_thumb = e.selectFirst("img").attr("src").toString();
                String tmp_title = e.selectFirst("img").attr("alt").toString();
                Manga tmp = new Manga(tmp_id,tmp_title,"");
                tmp.addThumb(tmp_thumb);
                recent.add(tmp);
            }
            Elements rankingWidgets = doc.select("div.rank-manga-widget");

            // online data
            Elements fav= rankingWidgets.get(0).select("li");
            rankingWidgetLiParser(fav, favUpdate);

            Elements rec = rankingWidgets.get(1).select("li");
            rankingWidgetLiParser(rec, onlineRecent);

            // ranking
            Elements rank = rankingWidgets.get(2).select("li");
            rankingWidgetLiParser(rank, ranking);


        }catch (Exception e){

        }
    }

    void rankingWidgetLiParser(Elements input, List output){
        for(Element e: input){
            String[] tmp_link = e.selectFirst("a").attr("href").split("=");
            int tmp_id = Integer.parseInt(tmp_link[tmp_link.length-1]);
            String tmp_title = e.selectFirst("div.subject").ownText();
            output.add(new Manga(tmp_id, tmp_title,""));
        }
    }
    public MainPage(String url){
        fetch(url, new HashMap<>());
    }
    public MainPage(String url, Map<String, String> cookie){
        fetch(url, cookie);
    }

    public List<Manga> getRecent() {
        return recent;
    }

    public List<Manga> getFavUpdate() {
        return favUpdate;
    }

    public List<Manga> getOnlineRecent() {
        return onlineRecent;
    }

    public List<Manga> getRanking() { return ranking; }
}

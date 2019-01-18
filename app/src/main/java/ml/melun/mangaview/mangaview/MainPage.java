package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class MainPage {
    ArrayList<Manga> recent, ranking;
    public MainPage(){
        recent = new ArrayList<>();
        ranking = new ArrayList<>();
        try{
            Document doc = Jsoup.connect("https://mangashow.me")
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
            Elements rank = doc.select("div.rank-manga-widget").last().select("li");
            for(Element e: rank){
                String[] tmp_link = e.selectFirst("a").attr("href").split("=");
                int tmp_id = Integer.parseInt(tmp_link[tmp_link.length-1]);
                String tmp_title = e.selectFirst("div.subject").text();
                ranking.add(new Manga(tmp_id, tmp_title,""));
            }
        }catch (Exception e){

        }
    }

    public ArrayList<Manga> getRecent() {
        return recent;
    }

    public ArrayList<Manga> getRanking() { return ranking; }
}

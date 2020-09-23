package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import okhttp3.Response;

import static ml.melun.mangaview.Utils.getNumberFromString;


public class MainPage {
    List<Manga> recent, favUpdate, onlineRecent;
    List<Title> ranking;

    void fetch(CustomHttpClient client) {

        recent = new ArrayList<>();
        ranking = new ArrayList<>();
        favUpdate = new ArrayList<>();
        onlineRecent = new ArrayList<>();

        try{
            Response r = client.mget("",true,null,false);
            String body = r.body().string();
            if(body.contains("Connect Error: Connection timed out")){
                //adblock : try again
                r.close();
                fetch(client);
                return;
            }
            Document d = Jsoup.parse(body);
            r.close();

            //recent
            int id;
            String name;
            String thumb;
            Manga mtmp;
            Element infos;
            Title ttmp;

            for(Element e : d.selectFirst("div.miso-post-gallery").select("div.post-row")){
                id = getNumberFromString(e.selectFirst("a").attr("href").split(client.getBaseMode()+'/')[1]);
                infos = e.selectFirst("div.img-item");
                thumb = infos.selectFirst("img").attr("src");
                name = infos.selectFirst("b").ownText();

                mtmp = new Manga(id, name, "");
                mtmp.addThumb(thumb);
                recent.add(mtmp);
            }

            for(Element e : d.select("div.miso-post-gallery").last().select("div.post-row")){
                id = Integer.parseInt(e.selectFirst("a").attr("href").split(client.getBaseMode()+'/')[1]);
                infos = e.selectFirst("div.img-item");
                thumb = infos.selectFirst("img").attr("src");
                name = infos.selectFirst("div.in-subject").ownText();

                ttmp = new Title(name, thumb, "" , null, "", id);
                ranking.add(ttmp);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

/*
        try{
            Response response = client.mget("");
            Document doc = Jsoup.parse(response.body().string());

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

            //close response
            response.close();


        }catch (Exception e){
            e.printStackTrace();
        }
*/
    }

    void rankingWidgetLiParser(Elements input, List output){
        for(Element e: input){
            String[] tmp_link = e.selectFirst("a").attr("href").split("=");
            int tmp_id = Integer.parseInt(tmp_link[tmp_link.length-1]);
            String tmp_title = e.selectFirst("div.subject").ownText();
            output.add(new Manga(tmp_id, tmp_title,""));
        }
    }
    public MainPage(CustomHttpClient client) {
        fetch(client);
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

    public List<Title> getRanking() { return ranking; }
}

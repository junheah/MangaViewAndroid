package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

public class Bookmark {
    List<MTitle> result;
    int page = -1;
    boolean last = false;

    public Bookmark(){

    }

    public int fetch(CustomHttpClient client){
        result = new ArrayList<>();
        try {
            page++;
            Response r = client.mget("/bbs/page.php?hid=favorit_list&page=" + page, true);
            Document d = Jsoup.parse(r.body().string());
            Elements items = d.select("div.manga-container");
            for(Element item : items){
                List<String> tags = new ArrayList<>();
                String ntmp = item.selectFirst("div.title").ownText();
                String idtmp = item.selectFirst("a").attr("href").split("manga_id=")[1];
                int id = Integer.parseInt(idtmp);
                String ttmp = item.selectFirst("div.manga-thumbnail").attr("style").split("\\(")[1].split("\\)")[0];
                String atmp = "";
                int rtmp = -1;

                Element author = item.selectFirst("div.author");
                if(author != null) atmp = author.text();

                Element release = item.selectFirst("div.publish-type");
                if(release != null) rtmp = Integer.parseInt(release.attr("onclick").split("\\(")[1].split("\\)")[0]);

                Element telement = item.selectFirst("div.tags");
                if(telement != null){
                    tags = telement.select("a").eachText();
                }

                System.out.println("pppp" + ntmp);

                result.add(new MTitle(ntmp, id, ttmp, atmp, tags, rtmp));
                r.close();

                if (items.size() < 40)
                    last = true;

                if(result.size()==0)
                    page--;

            }
        }catch (Exception e){
            page--;
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    public boolean isLast() {
        return last;
    }

    public List<MTitle> getResult(){
        return this.result;
    }
}

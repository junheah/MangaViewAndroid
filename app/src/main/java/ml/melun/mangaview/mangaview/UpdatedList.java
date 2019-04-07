package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

import okhttp3.Response;

public class UpdatedList {
    public void UpdatedList(){
        //
    }
    public void fetch(CustomHttpClient client){
        result = new ArrayList<>();
        String url = "/bbs/board.php?bo_table=manga&page=";
        if(!last) {
            try {
                page++;
                Response response= client.get(url+page);
                Document document = Jsoup.parse(response.body().string());
                Elements items = document.select("div.post-row");
                if (items.size() < 50) last = true;
                for(Element item : items){
                    String ttmp = item.selectFirst("div.img-item").selectFirst("img").attr("src");
                    String ntmp = item.selectFirst("div.post-subject").selectFirst("a").ownText().replace('\n',' ');
                    String title = java.net.URLDecoder.decode(item.selectFirst("div.post-info").selectFirst("a.btn").attr("href").split("manga_name=")[1], "UTF-8");

                    String idRaw = item.selectFirst("div.post-image").selectFirst("a.ellipsis").attr("href");
                    int itmp = Integer.parseInt(idRaw.substring(idRaw.lastIndexOf("=")+1));

                    String dtmp = item.selectFirst("div.post-info").selectFirst("span").ownText();

                    Manga tmp = new Manga(itmp,ntmp,dtmp);
                    tmp.setTitle(new Title(title,"","",new ArrayList<String>(), -1));
                    tmp.addThumb(ttmp);
                    result.add(tmp);
                }
                response.close();
            } catch (Exception e) {
                e.printStackTrace();
                page--;
            }
        }
    }

    public ArrayList<Manga> getResult() {
        return result;
    }
    public boolean isLast(){return last;}

    Boolean last = false;
    ArrayList<Manga> result;
    int page = 0;
}

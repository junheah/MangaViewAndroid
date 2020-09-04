package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Response;

public class UpdatedList {
    Boolean last = false;
    ArrayList<Manga> result;
    int page = 1;

    public void fetch(CustomHttpClient client){
        //50 items per page
        result = new ArrayList<>();
        String url = "/thema/comic/widget/miso-post-comic-update/widget.rows.php?thema=comic&wname=miso-post-comic-update&wid=comic_update&page=";
        if(!last) {
            try {
                Response response= client.mget(url + page++);
                String body = response.body().string();
                if(body.contains("Connect Error: Connection timed out")){
                    //adblock : try again
                    response.close();
                    fetch(client);
                    return;
                }
                Document document = Jsoup.parse(body);
                Elements items = document.select("div.post-row");
                if (items == null || items.size() < 50) last = true;
                for(Element item : items){
                    try {
                        String img = item.selectFirst("img").attr("src");
                        String name = item.selectFirst("div.post-subject").selectFirst("a").ownText();
                        int id = Integer.parseInt(item
                                .selectFirst("div.pull-left")
                                .selectFirst("a")
                                .attr("href")
                                .split("wr_id=")[1]);

                        Elements rightInfo = item.selectFirst("div.pull-right").select("p");

                        int tid = Integer.parseInt(rightInfo
                                .get(0)
                                .selectFirst("a")
                                .attr("href")
                                .split("wr_id=")[1]);

                        String date = rightInfo.get(1).selectFirst("span").ownText();

                        List<String> tags = Arrays.asList(item.selectFirst("div.post-text").ownText().split(","));

                        Manga tmp = new Manga(id, name, date);
                        tmp.setTitle(new Title(name, img, "", new ArrayList<String>(), "", tid));
                        tmp.addThumb(img);
                        result.add(tmp);
                    }catch(Exception e){
                        e.printStackTrace();
                        continue;
                    }
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


}

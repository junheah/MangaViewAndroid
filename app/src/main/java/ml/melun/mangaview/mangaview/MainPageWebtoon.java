package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

import static ml.melun.mangaview.mangaview.MTitle.base_webtoon;

public class MainPageWebtoon {
    String baseUrl;

    public static final String ING_RECENT = "연재 - 최신";
    public static final String ING_MONDAY = "연재 - 월요";
    public static final String ING_ACTION = "연재 - 액션";
    public static final String ING_ROMANCE = "연재 - 로맨스";
    public static final String ING_FANTASY = "연재 - 판타지";
    public static final String ING_ADULT = "연재 - 성인";
    public static final String END_RECENT = "완결 - 최신";
    public static final String END_ACTION = "완결 - 액션";
    public static final String END_ROMANCE = "완결 - 로맨스";
    public static final String END_FANTASY = "완결 - 판타지";
    public static final String END_MARTIAL = "완결 - 무협";
    public static final String END_ADULT = "완결 - 성인";

    private static final String[][] SECTIONS = {
            {ING_RECENT, "/ing?type1=day&type2=recent&o=n"},
            {ING_MONDAY, "/ing?type1=day&type2=mon&o=n"},
            {ING_ACTION, "/ing?type1=genre&type2=액션&o=n"},
            {ING_ROMANCE, "/ing?type1=genre&type2=로맨스&o=n"},
            {ING_FANTASY, "/ing?type1=genre&type2=판타지&o=n"},
            {ING_ADULT, "/ing?type1=genre&type2=성인&o=n"},
            {END_RECENT, "/end?type1=day&type2=recent&o=n"},
            {END_ACTION, "/end?type1=genre&type2=액션&o=n"},
            {END_ROMANCE, "/end?type1=genre&type2=로맨스&o=n"},
            {END_FANTASY, "/end?type1=genre&type2=판타지&o=n"},
            {END_MARTIAL, "/end?type1=genre&type2=무협&o=n"},
            {END_ADULT, "/end?type1=genre&type2=성인&o=n"}
    };

    List<Ranking<?>> dataSet;

    public MainPageWebtoon(CustomHttpClient client){
        fetch(client);
    }

    public String getUrl(CustomHttpClient client){
        this.baseUrl = client.getUrl(base_webtoon);
        return this.baseUrl;
    }

    public void fetch(CustomHttpClient client){
        if(baseUrl == null || baseUrl.length()==0)
            if(getUrl(client)==null)
                return;
        try {
            dataSet = new ArrayList<>();
            for(String[] section : SECTIONS)
                parseWolfTitle(client, section[0], section[1], base_webtoon);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void parseWolfTitle(CustomHttpClient client, String title, String path, int baseMode){
        Ranking<Title> ranking = new Ranking<>(title);
        try{
            Response r = client.mget(path);
            String body = r.body().string();
            r.close();
            Document d = Jsoup.parse(body);
            for(Title webtoon : parseWolfTitles(d, baseMode, 20))
                ranking.add(webtoon);
        }catch (Exception e){
            e.printStackTrace();
        }
        dataSet.add(ranking);
    }

    public static ArrayList<Title> parseWolfTitles(Document d, int baseMode, int limit){
        ArrayList<Title> titles = new ArrayList<>();
        for(Element e : d.select("div.webtoon-list li, article.searchItem")){
            try{
                Element link = e.selectFirst("a[href*=toon=]");
                if(link == null) continue;
                String href = link.attr("href");
                int id = getQueryInt(href, "toon");
                if(id <= 0) continue;

                String name = firstOwnText(e.selectFirst("p.subject"));
                if(name.length() == 0)
                    name = cleanText(e.selectFirst("h6.searchDetailTitle"));
                if(name.length() == 0)
                    name = link.attr("title");
                if(name.length() == 0)
                    name = getQueryString(href, "title");

                String thumb = "";
                Element img = e.selectFirst("img[data-original]");
                if(img != null)
                    thumb = img.attr("data-original");
                if(thumb.length() == 0 && img != null)
                    thumb = img.attr("src");
                if(thumb.length() == 0) {
                    Element searchPng = e.selectFirst(".searchPng[style*=background-image]");
                    if(searchPng != null)
                        thumb = extractBackgroundImage(searchPng.attr("style"));
                }

                Elements infos = e.select("div.txt p");
                List<String> tags = new ArrayList<>();
                if(infos.size() > 1)
                    for(String tag : cleanTextWithoutChildren(infos.get(1)).split("/"))
                        if(tag.trim().length() > 0) tags.add(tag.trim());

                String release = "";
                if(infos.size() > 2)
                    release = cleanTextWithoutChildren(infos.get(2));

                titles.add(new Title(name, thumb, "", tags, release, id, baseMode));
                if(limit > 0 && titles.size() >= limit) break;
            }catch (Exception ignored){
            }
        }
        return titles;
    }

    static int getQueryInt(String href, String key){
        try{
            String value = getQueryString(href, key);
            if(value.length() == 0) return -1;
            return Integer.parseInt(value);
        }catch (Exception e){
            return -1;
        }
    }

    static String getQueryString(String href, String key){
        try{
            String target = key + "=";
            int start = href.indexOf(target);
            if(start < 0) return "";
            start += target.length();
            int end = href.indexOf('&', start);
            if(end < 0) end = href.length();
            return URLDecoder.decode(href.substring(start, end), "UTF-8");
        }catch (Exception e){
            return "";
        }
    }

    private static String firstOwnText(Element element){
        if(element == null) return "";
        for(TextNode node : element.textNodes()){
            String text = node.text().trim();
            if(text.length() > 0) return text;
        }
        return element.ownText().trim();
    }

    private static String cleanText(Element element){
        if(element == null) return "";
        return element.text().trim();
    }

    private static String cleanTextWithoutChildren(Element element){
        if(element == null) return "";
        Element copy = element.clone();
        copy.children().remove();
        return copy.text().trim();
    }

    private static String extractBackgroundImage(String style){
        int start = style.indexOf("url(");
        if(start < 0) return "";
        start += 4;
        int end = style.indexOf(')', start);
        if(end < 0) return "";
        return style.substring(start, end).replace("'", "").replace("\"", "").trim();
    }

    public List<Ranking<?>> getDataSet(){
        return this.dataSet;
    }

    public static List<Ranking<?>> getBlankDataSet(){
        List<Ranking<?>> dataset = new ArrayList<>();
        for(String[] section : SECTIONS)
            dataset.add(new Ranking<>(section[0]));
        return dataset;
    }
}

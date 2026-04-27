package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.Response;

import static ml.melun.mangaview.mangaview.MTitle.base_comic;
import static ml.melun.mangaview.mangaview.MTitle.base_webtoon;

public class MainPageWebtoon {
    String baseUrl;
    int baseMode;

    public static final String ING_RECENT = "연재|최신 업데이트";
    public static final String ING_ACTION = "연재|액션";
    public static final String ING_ROMANCE = "연재|로맨스";
    public static final String ING_FANTASY = "연재|판타지";
    public static final String ING_MARTIAL = "연재|무협";
    public static final String ING_DRAMA = "연재|드라마";
    public static final String ING_BL = "연재|BL";
    public static final String ING_ADULT = "연재|성인";
    public static final String END_RECENT = "완결|최신 업데이트";
    public static final String END_ACTION = "완결|액션";
    public static final String END_ROMANCE = "완결|로맨스";
    public static final String END_FANTASY = "완결|판타지";
    public static final String END_MARTIAL = "완결|무협";
    public static final String END_DRAMA = "완결|드라마";
    public static final String END_ADULT = "완결|성인";

    private static final int MAIN_SECTION_LIMIT = 10;

    private static final String[][] SECTIONS = {
            {ING_RECENT, "/ing?type1=day&type2=recent&o=n"},
            {ING_ACTION, genrePath("/ing", "액션")},
            {ING_ROMANCE, genrePath("/ing", "로맨스")},
            {ING_FANTASY, genrePath("/ing", "판타지")},
            {ING_MARTIAL, genrePath("/ing", "무협")},
            {END_RECENT, "/end?type1=day&type2=recent&o=n"},
            {END_ACTION, genrePath("/end", "액션")},
            {END_ROMANCE, genrePath("/end", "로맨스")},
            {END_FANTASY, genrePath("/end", "판타지")},
            {END_MARTIAL, genrePath("/end", "무협")}
    };

    private static final String[][] COMIC_SECTIONS = {
            {"만화책|최신 업데이트", "/cm?type1=complete&type2=recent&o=n"},
            {"만화책|주간", "/cm?type1=complete&type2=10&o=n"},
            {"만화책|완결", "/cm?type1=complete&type2=16&o=n"},
            {"만화책|단행본", "/cm?type1=complete&type2=15&o=n"},
            {"만화책|액션", comicGenrePath("액션")},
            {"만화책|판타지", comicGenrePath("판타지")},
            {"만화책|드라마", comicGenrePath("드라마")},
            {"만화책|로맨스", comicGenrePath("로맨스")},
            {"만화책|이세계", comicGenrePath("이세계")},
            {"만화책|학원", comicGenrePath("학원")}
    };

    List<Ranking<?>> dataSet;

    public MainPageWebtoon(CustomHttpClient client){
        this(client, base_webtoon);
    }

    public MainPageWebtoon(CustomHttpClient client, int baseMode){
        this.baseMode = baseMode;
        fetch(client);
    }

    public String getUrl(CustomHttpClient client){
        this.baseUrl = client.getUrl(baseMode);
        return this.baseUrl;
    }

    public void fetch(CustomHttpClient client){
        if(baseUrl == null || baseUrl.length()==0)
            if(getUrl(client)==null)
                return;
        try {
            dataSet = new ArrayList<>();
            String[][] sections = getSections();
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(4, sections.length));
            List<Future<Ranking<?>>> futures = new ArrayList<>();
            for(String[] section : sections) {
                Callable<Ranking<?>> task = () -> parseWolfTitle(client, section[0], section[1], baseMode);
                futures.add(executor.submit(task));
            }
            for(Future<Ranking<?>> future : futures)
                dataSet.add(future.get());
            executor.shutdown();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String[][] getSections(){
        if(baseMode == base_comic)
            return COMIC_SECTIONS;
        return SECTIONS;
    }

    public Ranking<Title> parseWolfTitle(CustomHttpClient client, String title, String path, int baseMode){
        Ranking<Title> ranking = new Ranking<>(title);
        try{
            Response r = client.mget(path);
            String body = r.body().string();
            r.close();
            Document d = Jsoup.parse(body);
            for(Title webtoon : parseWolfTitles(d, baseMode, MAIN_SECTION_LIMIT))
                ranking.add(webtoon);
        }catch (Exception e){
            e.printStackTrace();
        }
        return ranking;
    }

    private static String genrePath(String statusPath, String genre) {
        return statusPath + "?type1=genre&type2=" + percentEncode(genre, Charset.forName("EUC-KR")) + "&o=n";
    }

    private static String comicGenrePath(String genre) {
        return "/cm?type1=genre&type2=" + percentEncode(genre, Charset.forName("EUC-KR")) + "&o=n";
    }

    private static String percentEncode(String value, Charset charset) {
        byte[] bytes = value.getBytes(charset);
        StringBuilder encoded = new StringBuilder();
        for(byte b : bytes) {
            int c = b & 0xff;
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.' || c == '~')
                encoded.append((char)c);
            else
                encoded.append('%').append(String.format("%02X", c));
        }
        return encoded.toString();
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
        return getBlankDataSet(base_webtoon);
    }

    public static List<Ranking<?>> getBlankDataSet(int baseMode){
        List<Ranking<?>> dataset = new ArrayList<>();
        String[][] sections = baseMode == base_comic ? COMIC_SECTIONS : SECTIONS;
        for(String[] section : sections)
            dataset.add(new Ranking<>(section[0]));
        return dataset;
    }
}

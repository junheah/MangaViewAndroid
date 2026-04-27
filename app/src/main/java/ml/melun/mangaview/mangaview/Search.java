package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Response;

import static ml.melun.mangaview.mangaview.MTitle.base_comic;
import static ml.melun.mangaview.mangaview.MTitle.base_webtoon;
import static ml.melun.mangaview.mangaview.MTitle.baseModeStr;

public class Search {
    private static final long PAGE_CACHE_TTL_MS = 2 * 60 * 1000L;

    int baseMode;
    private final String query;
    Boolean last = false;
    int mode;
    int page = 1;
    private ArrayList<Title> result;

    public Search(String q, int mode, int baseMode) {
        query = q;
        this.mode = mode;
        this.baseMode = baseMode;
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
            if(baseMode == base_webtoon)
                return fetchWebtoon(client);
            if(baseMode == base_comic)
                return fetchComic(client);
            try {
                String searchUrl = "";
                switch(mode){
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
                    response.close();
                    page--;
                    return fetch(client);
                }
                Document d = Jsoup.parse(body);
                d.outputSettings().charset(StandardCharsets.UTF_8);

                Elements titles = d.select("div.list-item");

                if(response.code()>=400){
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
                if (result.size() < 35)
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

    private int fetchWebtoon(CustomHttpClient client) {
        try {
            ArrayList<Title> webtoonResults = new ArrayList<>();
            if(mode == 8) {
                appendWebtoonResults(client, webtoonResults, query, 0);
            } else if(mode == 2) {
                appendWebtoonResults(client, webtoonResults, "/ing?type1=genre&type2=" + percentEncode(query, Charset.forName("EUC-KR")) + "&o=n", 80);
                appendWebtoonResults(client, webtoonResults, "/end?type1=genre&type2=" + percentEncode(query, Charset.forName("EUC-KR")) + "&o=n", 80);
            } else if(mode == 4) {
                String status = webtoonStatus(query);
                if(status.length() > 0) {
                    appendWebtoonResults(client, webtoonResults, status + "?type1=day&type2=recent&o=n", 80);
                } else {
                    String day = webtoonDay(query);
                    if(day.length() > 0) {
                        appendWebtoonResults(client, webtoonResults, "/ing?type1=day&type2=" + day + "&o=n", 80);
                        appendWebtoonResults(client, webtoonResults, "/end?type1=day&type2=" + day + "&o=n", 80);
                    } else {
                        appendWebtoonResults(client, webtoonResults, "/search.html?q=" + percentEncode(query, Charset.forName("EUC-KR")), 80);
                    }
                }
            } else {
                appendWebtoonResults(client, webtoonResults, "/search.html?q=" + percentEncode(query, Charset.forName("EUC-KR")), 80);
            }

            Set<Integer> seen = new HashSet<>();
            for(Title title : webtoonResults)
                if(seen.add(title.getId()))
                    result.add(title);
            last = true;
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    private int fetchComic(CustomHttpClient client) {
        try {
            ArrayList<Title> comicResults = new ArrayList<>();
            if(mode == 8) {
                appendWebtoonResults(client, comicResults, query, 0);
            } else if(mode == 2) {
                appendWebtoonResults(client, comicResults, "/cm?type1=genre&type2=" + percentEncode(query, Charset.forName("EUC-KR")) + "&o=n", 120);
            } else if(mode == 4) {
                String type = comicType(query);
                if(type.length() > 0)
                    appendWebtoonResults(client, comicResults, "/cm?type1=complete&type2=" + type + "&o=n", 120);
                else
                    appendWebtoonResults(client, comicResults, "/search.html?q=" + percentEncode(query, Charset.forName("EUC-KR")), 120);
            } else {
                appendWebtoonResults(client, comicResults, "/search.html?q=" + percentEncode(query, Charset.forName("EUC-KR")), 120);
            }

            Set<Integer> seen = new HashSet<>();
            for(Title title : comicResults)
                if(seen.add(title.getId()))
                    result.add(title);
            last = true;
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    private void appendWebtoonResults(CustomHttpClient client, ArrayList<Title> target, String path, int limit) throws Exception {
        CustomHttpClient.PageResponse page = client.mgetCachedPage(path, PAGE_CACHE_TTL_MS);
        if(page.code >= 400)
            throw new Exception("Webtoon search failed: " + page.code);
        Document d = Jsoup.parse(page.body);
        ArrayList<Title> parsed = MainPageWebtoon.parseWolfTitles(d, baseMode, limit);
        if(parsed.size() == 0 && client.resolveWfwfDomainNow()) {
            page = client.mgetCachedPage(path, PAGE_CACHE_TTL_MS);
            if(page.code >= 400)
                throw new Exception("Webtoon search failed: " + page.code);
            parsed = MainPageWebtoon.parseWolfTitles(Jsoup.parse(page.body), baseMode, limit);
        }
        target.addAll(parsed);
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

    private static String webtoonStatus(String value) {
        if(value == null) return "";
        String q = value.trim().toLowerCase();
        if(q.equals("연재") || q.equals("연재중") || q.equals("ing") || q.equals("ongoing")) return "/ing";
        if(q.equals("완결") || q.equals("완결웹툰") || q.equals("end") || q.equals("completed") || q.equals("complete")) return "/end";
        return "";
    }

    private static String webtoonDay(String value) {
        if(value == null) return "";
        String q = value.trim().toLowerCase();
        if(q.equals("월") || q.equals("월요") || q.equals("월요일") || q.equals("mon") || q.equals("monday")) return "mon";
        if(q.equals("화") || q.equals("화요") || q.equals("화요일") || q.equals("tue") || q.equals("tuesday")) return "tue";
        if(q.equals("수") || q.equals("수요") || q.equals("수요일") || q.equals("wed") || q.equals("wednesday")) return "wed";
        if(q.equals("목") || q.equals("목요") || q.equals("목요일") || q.equals("thu") || q.equals("thursday")) return "thu";
        if(q.equals("금") || q.equals("금요") || q.equals("금요일") || q.equals("fri") || q.equals("friday")) return "fri";
        if(q.equals("토") || q.equals("토요") || q.equals("토요일") || q.equals("sat") || q.equals("saturday")) return "sat";
        if(q.equals("일") || q.equals("일요") || q.equals("일요일") || q.equals("sun") || q.equals("sunday")) return "sun";
        if(q.equals("최신") || q.equals("recent")) return "recent";
        return "";
    }

    private static String comicType(String value) {
        if(value == null) return "";
        String q = value.trim().toLowerCase();
        if(q.equals("recent") || q.equals("최신")) return "recent";
        if(q.equals("weekly") || q.equals("주간")) return "10";
        if(q.equals("biweekly") || q.equals("격주")) return "11";
        if(q.equals("monthly") || q.equals("월간")) return "12";
        if(q.equals("oneshot") || q.equals("단편")) return "14";
        if(q.equals("completed") || q.equals("complete") || q.equals("완결")) return "16";
        if(q.equals("book") || q.equals("단행본")) return "15";
        return "";
    }

    public ArrayList<Title> getResult(){
        return result;
    }
}

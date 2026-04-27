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

    private static final int MAIN_SECTION_LIMIT = 10;
    private static final long PAGE_CACHE_TTL_MS = 2 * 60 * 1000L;

    private static final String[] WEBTOON_STATUS = {"ing", "end"};
    private static final String[] WEBTOON_STATUS_LABELS = {"연재웹툰", "완결웹툰"};
    private static final String[] WEBTOON_DAY_LABELS = {"최신", "신작", "월", "화", "수", "목", "금", "토", "일", "열흘"};
    private static final String[] WEBTOON_DAY_VALUES = {"recent", "new", "1", "2", "3", "4", "5", "6", "7", "10"};
    private static final String[] WEBTOON_GENRES = {"성인", "드라마", "판타지", "액션", "로맨스", "일상", "개그", "미스터리", "순정", "스포츠", "BL", "스릴러", "무협", "학원", "공포", "스토리"};
    private static final String[] ALPHABET_LABELS = {"ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ", "A-Z", "0-9"};
    private static final String[] ALPHABET_VALUES = {"ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ", "a", "0"};

    private static final String[] COMIC_DAY_LABELS = {"최신", "주간", "격주", "월간", "단편", "완결", "단행본", "비정기", "미분류"};
    private static final String[] COMIC_DAY_VALUES = {"recent", "10", "11", "12", "14", "16", "15", "13", "20"};
    private static final String[] COMIC_GENRES = {"17", "드라마", "액션", "SF", "TS", "개그", "게임", "공포", "도박", "호러", "라노벨", "러브코미디", "로맨스", "먹방", "미스터리", "백합", "붕탁", "성인", "순정", "스릴러", "스포츠", "시대", "학원", "BL", "여장", "역사", "요리", "음악", "이세계", "일상", "전생", "추리"};

    public static final String[][] WEBTOON_FILTER_GROUPS = buildWebtoonFilterGroups();
    public static final String[][] COMIC_FILTER_GROUPS = buildComicFilterGroups();

    private static final String[][] SECTIONS = buildWebtoonSections();
    private static final String[][] COMIC_SECTIONS = buildComicSections();

    List<Ranking<?>> dataSet;

    public MainPageWebtoon(CustomHttpClient client){
        this(client, base_webtoon);
    }

    public MainPageWebtoon(CustomHttpClient client, int baseMode){
        this.baseMode = baseMode;
        fetch(client);
    }

    public MainPageWebtoon(int baseMode){
        this.baseMode = baseMode;
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
        return getSections(baseMode);
    }

    public static String[][] getSections(int baseMode){
        if(baseMode == base_comic)
            return COMIC_SECTIONS;
        return SECTIONS;
    }

    public Ranking<Title> parseWolfTitle(CustomHttpClient client, String title, String path, int baseMode){
        Ranking<Title> ranking = new Ranking<>(title);
        boolean retried = false;
        try{
            CustomHttpClient.PageResponse page = client.mgetCachedPage(path, PAGE_CACHE_TTL_MS);
            Document d = Jsoup.parse(page.body);
            for(Title webtoon : parseWolfTitles(d, baseMode, MAIN_SECTION_LIMIT))
                ranking.add(webtoon);
            if(ranking.size() == 0 && !retried && client.resolveWfwfDomainNow()) {
                retried = true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if(retried)
            return parseWolfTitle(client, title, path, baseMode);
        return ranking;
    }

    private static String[][] buildWebtoonSections() {
        ArrayList<String[]> sections = new ArrayList<>();
        for(int i = 0; i < WEBTOON_STATUS.length; i++) {
            String status = WEBTOON_STATUS[i];
            String statusLabel = WEBTOON_STATUS_LABELS[i];
            sections.add(section(statusLabel, "최신", webtoonDayPath(status, "recent", "n")));
            sections.add(section(statusLabel, "신작", webtoonDayPath(status, "new", "n")));
            sections.add(section(statusLabel, "인기순", webtoonOrderPath(status, "f")));
            sections.add(section(statusLabel, "드라마", webtoonGenrePath(status, "드라마", "n")));
            sections.add(section(statusLabel, "판타지", webtoonGenrePath(status, "판타지", "n")));
            sections.add(section(statusLabel, "액션", webtoonGenrePath(status, "액션", "n")));
            sections.add(section(statusLabel, "로맨스", webtoonGenrePath(status, "로맨스", "n")));
            sections.add(section(statusLabel, "무협", webtoonGenrePath(status, "무협", "n")));
        }
        return sections.toArray(new String[0][]);
    }

    private static String[][] buildComicSections() {
        ArrayList<String[]> sections = new ArrayList<>();
        for(int i = 0; i < COMIC_DAY_LABELS.length; i++)
            sections.add(section("연재일", COMIC_DAY_LABELS[i], comicDayPath(COMIC_DAY_VALUES[i], "n")));
        for(String genre : COMIC_GENRES)
            sections.add(section("장르별", genre, comicGenrePath(genre, "n")));
        for(int i = 0; i < ALPHABET_LABELS.length; i++)
            sections.add(section("작품별", ALPHABET_LABELS[i], comicAlphabetPath(ALPHABET_VALUES[i], "n")));
        return sections.toArray(new String[0][]);
    }

    private static String[][] buildWebtoonFilterGroups() {
        ArrayList<String[]> groups = new ArrayList<>();
        groups.add(new String[]{
                filter("정렬", "연재 인기순", webtoonOrderPath("ing", "f")),
                filter("정렬", "연재 최신순", webtoonOrderPath("ing", "n")),
                filter("정렬", "완결 인기순", webtoonOrderPath("end", "f")),
                filter("정렬", "완결 최신순", webtoonOrderPath("end", "n"))
        });
        groups.add(buildWebtoonStatusFilters("연재 요일별", "ing", "day"));
        groups.add(buildWebtoonStatusFilters("완결 요일별", "end", "day"));
        groups.add(buildWebtoonStatusFilters("연재 장르별", "ing", "genre"));
        groups.add(buildWebtoonStatusFilters("완결 장르별", "end", "genre"));
        groups.add(buildWebtoonStatusFilters("연재 작품별", "ing", "alphabet"));
        groups.add(buildWebtoonStatusFilters("완결 작품별", "end", "alphabet"));
        return groups.toArray(new String[0][]);
    }

    private static String[] buildWebtoonStatusFilters(String group, String status, String type) {
        ArrayList<String> filters = new ArrayList<>();
        if("day".equals(type)) {
            for(int i = 0; i < WEBTOON_DAY_LABELS.length; i++)
                filters.add(filter(group, WEBTOON_DAY_LABELS[i], webtoonDayPath(status, WEBTOON_DAY_VALUES[i], "n")));
        } else if("genre".equals(type)) {
            for(String genre : WEBTOON_GENRES)
                filters.add(filter(group, genre, webtoonGenrePath(status, genre, "n")));
        } else {
            for(int i = 0; i < ALPHABET_LABELS.length; i++)
                filters.add(filter(group, ALPHABET_LABELS[i], webtoonAlphabetPath(status, ALPHABET_VALUES[i], "n")));
        }
        return filters.toArray(new String[0]);
    }

    private static String[][] buildComicFilterGroups() {
        ArrayList<String[]> groups = new ArrayList<>();
        groups.add(new String[]{
                filter("정렬", "인기순", "/cm?type1=complete&type2=recent&o=f"),
                filter("정렬", "최신순", "/cm?type1=complete&type2=recent&o=n")
        });
        ArrayList<String> days = new ArrayList<>();
        for(int i = 0; i < COMIC_DAY_LABELS.length; i++)
            days.add(filter("연재일", COMIC_DAY_LABELS[i], comicDayPath(COMIC_DAY_VALUES[i], "n")));
        groups.add(days.toArray(new String[0]));
        ArrayList<String> genres = new ArrayList<>();
        for(String genre : COMIC_GENRES)
            genres.add(filter("장르별", genre, comicGenrePath(genre, "n")));
        groups.add(genres.toArray(new String[0]));
        ArrayList<String> alphabets = new ArrayList<>();
        for(int i = 0; i < ALPHABET_LABELS.length; i++)
            alphabets.add(filter("작품별", ALPHABET_LABELS[i], comicAlphabetPath(ALPHABET_VALUES[i], "n")));
        groups.add(alphabets.toArray(new String[0]));
        return groups.toArray(new String[0][]);
    }

    private static String[] section(String group, String label, String path) {
        return new String[]{filter(group, label, path), path};
    }

    private static String filter(String group, String label, String path) {
        return group + "|" + label + "|" + path;
    }

    private static String webtoonOrderPath(String status, String order) {
        if("end".equals(status))
            return "/end?type1=genre&type2=&o=" + order;
        return "/ing?type1=day&type2=recent&o=" + order;
    }

    private static String webtoonDayPath(String status, String value, String order) {
        return "/" + status + "?type1=day&type2=" + percentEncode(value, Charset.forName("EUC-KR")) + "&o=" + order;
    }

    private static String webtoonGenrePath(String status, String genre, String order) {
        if("성인".equals(genre))
            return "/" + status + "?type1=genre&o=" + order;
        return "/" + status + "?type1=genre&type2=" + percentEncode(genre, Charset.forName("EUC-KR")) + "&o=" + order;
    }

    private static String webtoonAlphabetPath(String status, String value, String order) {
        return "/" + status + "?type1=alphabet&type2=" + percentEncode(value, Charset.forName("EUC-KR")) + "&o=" + order;
    }

    private static String comicDayPath(String value, String order) {
        return "/cm?type1=complete&type2=" + value + "&o=" + order;
    }

    private static String comicGenrePath(String genre, String order) {
        return "/cm?type1=genre&type2=" + percentEncode(genre, Charset.forName("EUC-KR")) + "&o=" + order;
    }

    private static String comicAlphabetPath(String value, String order) {
        return "/cm?type1=alphabet&type2=" + percentEncode(value, Charset.forName("EUC-KR")) + "&o=" + order;
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
        String[][] sections = getSections(baseMode);
        for(String[] section : sections)
            dataset.add(new Ranking<>(section[0]));
        return dataset;
    }
}

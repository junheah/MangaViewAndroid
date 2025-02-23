package ml.melun.mangaview.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class UrlUtil {

    public static String getActiveUrl(String url) {
        try {
            // URL에서 HTML을 가져옵니다.
            Document document = Jsoup.connect(url).get();

            // 정확히 <a> 태그에서 data-testid="linkElement"와 aria-label="마나토끼 바로가기"를 만족하는 엘리먼트 선택
            Element linkElement = document.selectFirst("a[data-testid=\"linkElement\"][aria-label=\"마나토끼 바로가기\"]");

            // href 속성을 반환
            if (linkElement != null) {
                return linkElement.attr("href");
            } else {
                return "No matching <a> tag found.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred while fetching or parsing HTML.";
        }
    }
}
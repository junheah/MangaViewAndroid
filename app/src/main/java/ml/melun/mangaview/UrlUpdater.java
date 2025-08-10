package ml.melun.mangaview;

import static ml.melun.mangaview.MainApplication.p;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class UrlUpdater extends AsyncTask<Void, Void, Boolean> {
    String result;
    String fetchUrl;
    boolean silent = false;
    Context c;
    UrlUpdaterCallback callback;

    public UrlUpdater(Context c) {
        this.c = c;
        this.fetchUrl = p.getDefUrl();
    }

    public UrlUpdater(Context c, boolean silent, UrlUpdaterCallback callback, String defUrl) {
        this.c = c;
        this.silent = silent;
        this.callback = callback;
        this.fetchUrl = defUrl;
    }

    protected void onPreExecute() {
        if (!silent) {
            Toast.makeText(c, "자동 URL 설정중...", Toast.LENGTH_SHORT).show();
        }
    }

    protected Boolean doInBackground(Void... params) {
        return fetch();
    }

    protected Boolean fetch() {
        try {
            // 1. Jsoup으로 HTML 페이지를 가져옵니다.
            String url = "https://t.me/s/p48v267tsgubym7";
            Document document = Jsoup.connect(url).get();

            // 2. 텍스트 "manatoki"를 포함하는 <a> 태그를 선택
            Element linkElement = document.selectFirst("a:contains(manatoki)");

            // 3. href 속성을 반환
            if (linkElement != null) {
                String href = linkElement.attr("href");
                if (StringUtils.isEmpty(href)) {
                    return false;
                }

                // 4. 최종 URL 확인 및 결과 저장
                result = href;
                return true;
            } else {
                return false; // "manatoki" 링크를 찾지 못함
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false; // 예외 발생 처리
        }
    }

    protected void onPostExecute(Boolean r) {
        if (r && result != null) {
            p.setUrl(result);
            if (!silent) {
                Toast.makeText(c, "자동 URL 설정 완료!", Toast.LENGTH_SHORT).show();
            }
            if (callback != null) {
                callback.callback(true);
            }
        } else {
            if (!silent) {
                Toast.makeText(c, "자동 URL 설정 실패, 잠시후 다시 시도해 주세요", Toast.LENGTH_LONG).show();
            }
            if (callback != null) {
                callback.callback(false);
            }
        }
    }

    public interface UrlUpdaterCallback {
        void callback(boolean success);
    }
}

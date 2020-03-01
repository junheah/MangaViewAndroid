package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.MalformedURLException;
import java.net.URL;

import ml.melun.mangaview.R;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.mangaview.Login;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.Utils.showErrorPopup;

public class CaptchaActivity extends AppCompatActivity {
    WebView webView;
    public static final int RESULT_CAPTCHA = 15;
    String domain = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = this;
        setContentView(R.layout.activity_captcha);
        String purl = p.getUrl();

        try {
            URL u = new URL(purl);
            domain = u.getHost();
        }catch (MalformedURLException e){
            showErrorPopup(context, "URL 형식이 올바르지 않습니다.", e, true);
        }

        if(purl.contains("http://")){
            showErrorPopup(context, "ip 주소 혹은 잘못된 주소를 사용중입니다. 자동 URL 설정을 사용하거나, 주소를 다시 입력해 주세요", null, false);
        }

        webView = this.findViewById(R.id.captchaWebView);
        WebSettings settings = webView.getSettings();
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        settings.setJavaScriptEnabled(true);
        CookieManager cookiem = CookieManager.getInstance();
        cookiem.removeAllCookie();

        WebViewClient client = new WebViewClient() {
            int count = 2;

            @Override
            public void onLoadResource(WebView view, String url) {
                if(count == 0){
                    count--;
                    // read cookies and finish
                    try {
                        String cookieStr = cookiem.getCookie(url);
                        for (String s : cookieStr.split("; ")) {
                            String k = s.substring(0, s.indexOf("="));
                            String v = s.substring(s.indexOf("=") + 1);
                            httpClient.setCookie(k, v);
                        }
                        Intent resultIntent = new Intent();
                        setResult(RESULT_CAPTCHA, resultIntent);
                        finish();
                    }catch (Exception e){
                        Utils.showErrorPopup(context, "인증 도중 오류가 발생했습니다. 네트워크 연결 상태를 확인해주세요.", e, true);
                    }

                } else if(url.contains("favicon.ico")){
                    count--;
                    super.onLoadResource(view, url);
                } else if(url.toLowerCase().contains(domain.toLowerCase())) {
                    if(count == 1) count--;
                    super.onLoadResource(view, url);
                }
            }
        };

        webView.setWebViewClient(client);

        Login login = p.getLogin();
        if(login != null && login.getCookie() !=null && login.getCookie().length()>0){
            //session exists
            cookiem.setCookie(purl, login.getCookie(true));
        }

        webView.loadUrl(purl);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //destroy webview
        ((ConstraintLayout) findViewById(R.id.captchaContainer)).removeAllViews();
        webView.clearHistory();
        webView.clearCache(true);
        webView.destroy();
    }
}

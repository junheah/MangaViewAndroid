package ml.melun.mangaview.activity;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Login;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

public class CaptchaActivity extends AppCompatActivity {
    WebView webView;
    public static final int RESULT_CAPTCHA = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captcha);
        String purl = p.getUrl();

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
                    String cookieStr = cookiem.getCookie(url);
                    for(String s: cookieStr.split("; ")){
                        String k = s.substring(0,s.indexOf("="));
                        String v = s.substring(s.indexOf("=")+1);
                        httpClient.setCookie(k, v);
                    }
                    Intent resultIntent = new Intent();
                    setResult(RESULT_CAPTCHA, resultIntent);
                    finish();

                } else if(url.contains("favicon.ico")){
                    count--;
                    super.onLoadResource(view, url);
                } else if(url.contains(purl)) {
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

package ml.melun.mangaview.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Login;
import okhttp3.Response;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

public class CaptchaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captcha);

        Intent intent = getIntent();
        int id = intent.getIntExtra("id", 0);
        String url = p.getUrl() + "/bbs/board.php?bo_table=manga&wr_id=" + id;


        System.out.println("ppppppppppp" + p.getSession());

        WebView webView = this.findViewById(R.id.captchaWebView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        CookieManager cookiem = CookieManager.getInstance();
        cookiem.removeAllCookie();

        webView.setWebViewClient(new WebViewClient() {
            boolean catchNextRequest = false;

            public boolean shouldOverrideUrlLoading(WebView view, String url){
                // do your handling codes here, which url is the requested url
                // probably you need to open that url rather than redirect:
                String cookies = CookieManager.getInstance().getCookie(url);
                if(catchNextRequest){
                    String cookieStr = cookiem.getCookie(url);
                    for(String s: cookieStr.split(";")){
                        if(s.contains("PHPSESSID=")){
                            System.out.println(s);
                            submit(s.substring(s.indexOf("=")+1));
                            break;
                        }
                    }
                } else if(url.contains("sign_captcha.php")) {
                    catchNextRequest = true;
                    view.loadUrl(url);
                }else
                    view.loadUrl(url);
                return false; // then it is not handled by default action
            }

        });

        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");

        Login login = p.getLogin();

        // if logged-in, get session from login
        if(login != null && login.isValid()){
            cookiem.setCookie(p.getUrl(), login.getCookie(true));
        }else if(p.getSession().length()>0){
            // else, use session
            cookiem.setCookie(p.getUrl(), "PHPSESSID=" + p.getSession() + "; ");
        }
        webView.loadUrl(url, headers);
    }

    public void submit(String cookie){
        p.setSession(cookie);
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}

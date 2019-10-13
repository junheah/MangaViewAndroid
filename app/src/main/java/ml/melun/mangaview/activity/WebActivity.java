package ml.melun.mangaview.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import java.util.HashMap;
import java.util.Map;

import ml.melun.mangaview.R;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

public class WebActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        Button button = this.findViewById(R.id.webButton);
        WebView webView = this.findViewById(R.id.webView);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                // do your handling codes here, which url is the requested url
                // probably you need to open that url rather than redirect:
                view.loadUrl(url);
                return false; // then it is not handled by default action
            }
            @Override
            public void onPageFinished(WebView view, String url){
                String cookies = CookieManager.getInstance().getCookie(url);
                System.out.println("All the cookies in a string:" + cookies);
                if(cookies.contains("cf_clearance")){
                    //success
                    finish();
                }
            }
        });


        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        webView.loadUrl(p.getUrl(),headers);
    }
}

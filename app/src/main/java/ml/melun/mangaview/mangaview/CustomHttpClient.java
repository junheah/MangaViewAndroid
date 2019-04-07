package ml.melun.mangaview.mangaview;


import java.util.HashMap;
import java.util.Map;

import ml.melun.mangaview.Preference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CustomHttpClient {
    OkHttpClient client;
    CloudFlareDns cloudflareDns;
    Boolean isloaded = false;
    Preference p ;

    public CustomHttpClient(Preference p){
        this.p = p;
        this.cloudflareDns = new CloudFlareDns();
        this.client = new OkHttpClient.Builder().dns(cloudflareDns).followRedirects(false).followSslRedirects(false).build();
        //this.client = new OkHttpClient.Builder().build();
    }


    public Response getRaw(String url, Map<String, String> cookies){
        if(!isloaded){
            cloudflareDns.init();
            isloaded = true;
        }
        Response response = null;
        try {
            String cookie = "";
            for(String key : cookies.keySet()){
                cookie += key + '=' + cookies.get(key) + "; ";
            }
            Request request = new Request.Builder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
                    .addHeader("Cookie", cookie)
                    .url(url)
                    .get()
                    .build();
            response = client.newCall(request)
                    .execute();
        }catch (Exception e){
            e.printStackTrace();
        }
        return response;
    }

    public Response get(String url, Boolean doLogin){
        Map<String, String> cookies = new HashMap<>();
        Login login = p.getLogin();
        if(doLogin && login!=null){
            login.buildCookie(cookies);
        }
        return getRaw(p.getUrl()+url,cookies);
    }
    public Response get(String url){
        return get(url,true);
    }

    public Response get(String url,Boolean doLogin, Map<String, String> customCookie){
        Login login = p.getLogin();
        if(doLogin && login!=null){
            login.buildCookie(customCookie);
        }
        return getRaw(p.getUrl()+url, customCookie);
    }

    public Response post(String url, RequestBody body){
        if(!isloaded){
            cloudflareDns.init();
            isloaded = true;
        }
        Response response = null;
        try {
            String cookie = "";
            if(p.getLogin()!=null){
                cookie = p.getLogin().getCookie(true);
            }
            Request request = new Request.Builder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
                    .addHeader("Cookie", cookie)
                    .url(p.getUrl() + url)
                    .post(body)
                    .build();
            response = client.newCall(request)
                    .execute();
        }catch (Exception e){

        }
        return response;

    }
}

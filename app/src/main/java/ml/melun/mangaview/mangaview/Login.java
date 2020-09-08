package ml.melun.mangaview.mangaview;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.melun.mangaview.Preference;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http2.Header;

import static java.lang.System.currentTimeMillis;
import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

public class Login {
    private String user;
    private String pass;
    String cookie = "";
    long currentTime = 0;

    public Login(){
    }

    public void set(String id, String pass){
        this.user = id;
        this.pass = pass;
    }

    public byte[] prepare(CustomHttpClient client, Preference p){
        Response r;
        int tries = 3;
        while(tries > 0) {
            r = client.post(p.getUrl() + "/plugin/kcaptcha/kcaptcha_session.php", new FormBody.Builder().build(), new HashMap<>(),false);
            if(r.code() == 200) {
                List<String> setcookie = r.headers("Set-Cookie");
                for (String c : setcookie) {
                    if (c.contains("PHPSESSID=")) {
                        cookie = c.substring(c.indexOf("=") + 1, c.indexOf(";"));
                        client.setCookie("PHPSESSID",cookie);
                        System.out.println(cookie);
                    }
                }
                break;
            }else {
                r.close();
                tries--;
            }
        }
        currentTime = currentTimeMillis();
        r = client.mget("/plugin/kcaptcha/kcaptcha_image.php?t=" + currentTime, false);
        try {
            return r.body().bytes();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public Boolean submit(CustomHttpClient client, String answer){
        try{
            RequestBody requestBody = new FormBody.Builder()
                    .addEncoded("auto_login", "on")
                    .addEncoded("mb_id",user)
                    .addEncoded("mb_password",pass)
                    .addEncoded("captcha_key", answer)
                    .build();
            Map<String,String> headers = new HashMap<>();
            headers.put("Cookie", "PHPSESSID="+cookie+";");

            Response response = client.post(p.getUrl() + "/bbs/login_check.php", requestBody, headers);
            int responseCode = response.code();

            if(responseCode == 302) {
                //follow redirect
                client.get(response.header("Location"), headers);
                //set session?
                client.setCookie("PHPSESSID", cookie);
                response.close();
                return true;
            }
            else{
                response.close();
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    public void buildCookie(Map<String,String> map){
        //java always passes by reference
        map.put("PHPSESSID", cookie);
    }

    public boolean isValid(){
        return cookie !=null && cookie.length()>0;
    }

    public String getCookie(Boolean format){
        if(format) return "PHPSESSID=" +cookie +';';
        return cookie;
    }

    public String getCookie() {
        return cookie;
    }
}

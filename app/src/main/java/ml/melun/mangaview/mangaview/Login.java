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

        Response r = client.get(p.getUrl() + "/bbs/login.php", new HashMap<>());
        List<String> setcookie = r.headers("Set-Cookie");
        r.close();
        currentTime = currentTimeMillis();
        for (String c : setcookie) {
            if (c.contains("PHPSESSID=")) {
                cookie = c.substring(c.indexOf("=") + 1, c.indexOf(";"));
                System.out.println(cookie);
            }
        }
        client.setCookie("PHPSESSID", "pppp "+cookie);
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

            Response response = client.post(p.getUrl() + "/bbs/login_check.php", requestBody);
            int responseCode = response.code();
            List<String> cookies = response.headers("Set-Cookie");

            response.close();
            if(responseCode == 302) {
                for (String c : cookies) {
                    if (c.contains("PHPSESSID=")) {
                        cookie = c.substring(c.indexOf("=")+1,c.indexOf(";"));
                        // session : copy of login that is used more frequently
                        return true;
                    }
                }
                // session already exists?
                cookie = client.getCookie("PHPSESSID");
                return true;
            }
            else return false;
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

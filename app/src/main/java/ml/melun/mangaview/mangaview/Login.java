package ml.melun.mangaview.mangaview;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

import static ml.melun.mangaview.MainApplication.p;

public class Login {
    private String user;
    private String pass;
    String cookie = "";

    public Login(String user, String pass){
        this.user = user;
        this.pass = pass;
    }

    public Boolean submit(CustomHttpClient client){
        try{
            RequestBody requestBody = new FormBody.Builder()
                    .addEncoded("mb_id",user)
                    .addEncoded("mb_password",pass)
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

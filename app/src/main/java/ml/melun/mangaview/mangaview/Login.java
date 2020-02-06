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

    public Boolean oldsubmit(String baseUrl){
        try{
            URL url = new URL(baseUrl + "/bbs/login_check.php");
            String param = "mb_id="+user+"&mb_password="+pass;
            byte[] data = param.getBytes(Charset.forName("UTF-8"));
            List<String> cookies = null;
            int responseCode = 0;
            if(url.getProtocol().equals("http")) {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Accept", "*");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "UTF-8");
                connection.setRequestProperty("Content-Length", Integer.toString(data.length));
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
                new DataOutputStream(connection.getOutputStream()).write(data);
                responseCode = connection.getResponseCode();
                cookies = connection.getHeaderFields().get("Set-Cookie");
            }else if(url.getProtocol().equals("https")){
                HttpsURLConnection connections = (HttpsURLConnection) url.openConnection();
                connections.setInstanceFollowRedirects(false);
                connections.setRequestMethod("POST");
                connections.setRequestProperty("Accept", "*");
                connections.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
                connections.setRequestProperty( "Charset", "UTF-8");
                connections.setRequestProperty( "Content-Length", String.valueOf(data.length));
                connections.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
                new DataOutputStream(connections.getOutputStream()).write(data);
                responseCode = connections.getResponseCode();
                cookies = connections.getHeaderFields().get("Set-Cookie");
            }
            if(responseCode == 302) {
                for (String c : cookies) {
                    if (c.contains("PHPSESSID=")) {
                        cookie = c.substring(c.indexOf("=")+1,c.indexOf(";"));
                        return true;
                    }
                }
            }
            else return false;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public Boolean submit(CustomHttpClient client){
        try{
            RequestBody requestBody = new FormBody.Builder()
                    .addEncoded("mb_id",user)
                    .addEncoded("mb_password",pass)
                    .build();

            Response response = client.post("/bbs/login_check.php",requestBody);
            int responseCode = response.code();
            List<String> cookies = response.headers("Set-Cookie");

            response.close();
            if(responseCode == 302) {
                for (String c : cookies) {
                    if (c.contains("PHPSESSID=")) {
                        cookie = c.substring(c.indexOf("=")+1,c.indexOf(";"));
                        // session : copy of login that is used more frequently
                        p.setSession(cookie);
                        return true;
                    }
                }
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

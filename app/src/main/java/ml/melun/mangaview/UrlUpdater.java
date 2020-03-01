package ml.melun.mangaview;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import net.jhavar.main.DdosGuardBypass;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Response;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

public class UrlUpdater extends AsyncTask<Void, Void, Boolean> {
    String result;
    String fetchUrl = "http://mnmnmnmnm.xyz/";
    String ipFetchUrl = "http://52.74.159.59";
    String directIp = "http://185.141.63.93";
    Context c;
    public UrlUpdater(Context c){
        this.c = c;
    }
    protected void onPreExecute() {
        Toast.makeText(c, "자동 URL 설정중...", Toast.LENGTH_SHORT).show();
    }
    protected Boolean doInBackground(Void... params) {
        return directIpFetch() || ipFetch() || fetch();
    }

    protected boolean directIpFetch(){
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.0.2; en-us; Galaxy Nexus Build/ICL53F) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
            Response r = httpClient.get(directIp, headers);
            if (r.code() == 301) {
                result = r.header("Location");
                return true;
            } else
                return false;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    protected  boolean ipFetch(){
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.0.2; en-us; Galaxy Nexus Build/ICL53F) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
            Response r = httpClient.get(ipFetchUrl, headers);
            if (r.code() == 302) {
                result = r.header("Location");
                r.close();
                return true;
            } else{
                r.close();
                return false;
            }

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    protected boolean fetch(){
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.0.2; en-us; Galaxy Nexus Build/ICL53F) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
            Response r = httpClient.get(fetchUrl, headers);
            if(r.code() == 403) {
                DdosGuardBypass ddg = new DdosGuardBypass(fetchUrl);
                ddg.bypass();
                String raw = ddg.get(fetchUrl);
                result = raw.split("주소는 ")[1].split(" ")[0];
                r.close();
                return true;
            }else if(r.code() == 301){
                fetchUrl = r.header("Location");
                r.close();
                return fetch();
            }else if(r.code() == 302){
                result = r.header("Location");
                r.close();
                return true;
            }else if(r.code() == 200){
                result = r.body().string().split("주소는 ")[1].split(" ")[0];
                r.close();
                return true;
            }else {
                r.close();
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    protected void onPostExecute(Boolean r) {
        if(r && result !=null){
            p.setUrl(result);
            Toast.makeText(c, "자동 URL 설정 완료!", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(c, "자동 URL 설정 실패, 잠시후 다시 시도해 주세요", Toast.LENGTH_LONG).show();
        }
    }
}

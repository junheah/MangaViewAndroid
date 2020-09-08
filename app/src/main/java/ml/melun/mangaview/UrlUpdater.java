package ml.melun.mangaview;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Response;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

public class UrlUpdater extends AsyncTask<Void, Void, Boolean> {
    String result;
    String fetchUrl = "https://manatoki.net/";
    boolean silent = false;
    Context c;
    public UrlUpdater(Context c){
        this.c = c;
    }
    public UrlUpdater(Context c, boolean silent){
        this.c = c;
        this.silent = silent;
    }
    protected void onPreExecute() {
        if(!silent) Toast.makeText(c, "자동 URL 설정중...", Toast.LENGTH_SHORT).show();
    }
    protected Boolean doInBackground(Void... params) {
        return fetch();
    }

    protected Boolean fetch(){
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.0.2; en-us; Galaxy Nexus Build/ICL53F) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
            Response r = httpClient.get(fetchUrl, headers);
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

    protected void onPostExecute(Boolean r) {
        if(r && result !=null){
            p.setUrl(result);
            if(!silent)Toast.makeText(c, "자동 URL 설정 완료!", Toast.LENGTH_SHORT).show();
        }else{
            if(!silent)Toast.makeText(c, "자동 URL 설정 실패, 잠시후 다시 시도해 주세요", Toast.LENGTH_LONG).show();
        }
    }
}

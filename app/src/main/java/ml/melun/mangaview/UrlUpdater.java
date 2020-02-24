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

public class UrlUpdater extends AsyncTask<Void, Void, Integer> {
    String result;
    Context c;
    public UrlUpdater(Context c){
        this.c = c;
    }
    protected void onPreExecute() {
        Toast.makeText(c, "자동 URL 설정중...", Toast.LENGTH_SHORT).show();
    }
    protected Integer doInBackground(Void... params) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.0.2; en-us; Galaxy Nexus Build/ICL53F) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
            Response r = httpClient.get("http://mnmnmnmnm.xyz/", headers);
//            if(r.code() == 403) {
//                DdosGuardBypass ddg = new DdosGuardBypass("https://mnmnmnmnm.xyz/");
//                ddg.bypass();
//                String raw = ddg.get("https://mnmnmnmnm.xyz/");
//                result = raw.split("주소는 ")[1].split(" ")[0];
//                return 0;
//            }else
//                return 1;
            if(r.code() == 302){
                result = r.header("Location");
                return 0;
            }else
                return 1;


        }catch (Exception e){
            e.printStackTrace();
            return 1;
        }
    }
    protected void onPostExecute(Integer r) {
        if(r == 0 && result !=null){
            p.setUrl(result);
            Toast.makeText(c, "자동 URL 설정 완료!", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(c, "자동 URL 설정 실패, 잠시후 다시 시도해 주세요", Toast.LENGTH_LONG).show();
        }
    }
}

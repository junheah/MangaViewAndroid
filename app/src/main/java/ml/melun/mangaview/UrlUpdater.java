package ml.melun.mangaview;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Response;

import ml.melun.mangaview.mangaview.WfwfDomainResolver;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

public class UrlUpdater extends AsyncTask<Void, Void, Boolean> {
    String result;
    String fetchUrl;
    boolean silent = false;
    Context c;
    UrlUpdaterCallback callback;
    public UrlUpdater(Context c){
        this.c = c;
        this.fetchUrl = p.getDefUrl();
    }
    public UrlUpdater(Context c, boolean silent, UrlUpdaterCallback callback, String defUrl){
        this.c = c;
        this.silent = silent;
        this.callback = callback;
        this.fetchUrl = defUrl;
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
            headers.put("User-Agent", httpClient.agent);
            String root = WfwfDomainResolver.toRoot(fetchUrl);
            if(WfwfDomainResolver.isWfwfUrl(root)) {
                headers.put("Referer", root);
                result = WfwfDomainResolver.resolve(httpClient.client, root, headers);
                return result != null;
            }

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
            if(WfwfDomainResolver.isWfwfUrl(WfwfDomainResolver.toRoot(result))) {
                String root = WfwfDomainResolver.toRoot(result);
                p.setWebtoonUrl(root);
                p.setDefUrl(root + "/cm");
                p.setUrl(root + "/cm");
                httpClient.resetCookie();
                httpClient.clearPageCache();
            } else {
                p.setUrl(result);
            }
            if(!silent)Toast.makeText(c, "자동 URL 설정 완료!", Toast.LENGTH_SHORT).show();
            if(callback!=null) callback.callback(true);
        }else{
            if(!silent)Toast.makeText(c, "자동 URL 설정 실패, 잠시후 다시 시도해 주세요", Toast.LENGTH_LONG).show();
            if(callback!=null) callback.callback(false);
        }


    }


    public interface UrlUpdaterCallback{
        void callback(boolean success);
    }
}

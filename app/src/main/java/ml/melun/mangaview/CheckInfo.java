package ml.melun.mangaview;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import org.json.JSONObject;

import static ml.melun.mangaview.Utils.httpsGet;
import static ml.melun.mangaview.Utils.showPopup;

public class CheckInfo {
    Context context;
    updateCheck uc;
    noticeCheck nc;
    public CheckInfo(Context context){
        this.context = context;
        uc = new updateCheck();
        nc = new noticeCheck();
    }
    public void all(){
        if(update()) notice();
    }
    public Boolean update(){
        if(uc.getStatus()== AsyncTask.Status.RUNNING) {
            Toast.makeText(context, "이미 실행중입니다. 잠시후에 다시 시도해 주세요",Toast.LENGTH_SHORT).show();
            return false;
        }
        else{
            uc.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return true;
        }
    }
    public Boolean notice() {
        if (nc.getStatus() == AsyncTask.Status.RUNNING){
            Toast.makeText(context, "이미 실행중입니다. 잠시후에 다시 시도해 주세요",Toast.LENGTH_SHORT).show();
            return false;
        }
        else {
            nc.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return true;
        }
    }

    private class noticeCheck extends AsyncTask<Void, Void, Integer> {
        String title, content, date;
        int nid = 0;
        protected void onPreExecute() {
            super.onPreExecute();
        }
        protected Integer doInBackground(Void... params) {
            try {
                String rawdata = httpsGet("https://github.com/junheah/MangaViewAndroid/raw/master/notice.json");
                JSONObject data = new JSONObject(rawdata);
                title = data.getString("title");
                System.out.println("pppppp"+title);
                content = data.getString("content");
                date = data.getString("date");
                nid = data.getInt("id");
            }catch(Exception e){
                e.printStackTrace();
            }
            return 0;
        }
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if(nid>0) showNotice(nid,title,content,date);
        }
    }

    public class updateCheck extends AsyncTask<Void, Integer, Integer> {
        int version = 0;
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                version = pInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Toast.makeText(context, "업데이트 확인중..", Toast.LENGTH_SHORT).show();
        }

        protected Integer doInBackground(Void... params) {
            try {
                String rawdata = httpsGet("https://github.com/junheah/MangaViewAndroid/raw/master/version.json");
                JSONObject data = new JSONObject(rawdata);
                int lver = data.getInt("version");
                String link = data.getString("link");
                if(version<lver){
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    context.startActivity(browserIntent);
                    return 1;
                }
            }catch(Exception e){
                return -1;
            }return 0;
        }
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            switch(result){
                case -1:
                    Toast.makeText(context, "오류가 발생했습니다. 나중에 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                    break;
                case 0:
                    Toast.makeText(context, "최신버전 입니다.", Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    Toast.makeText(context, "새로운 버전을 찾았습니다. 다운로드 페이지로 이동합니다.", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    void showNotice(int nid, String title, String content, String date){
        //공지 표시
        try {
            SharedPreferences sharedPref = context.getSharedPreferences("mangaView", Context.MODE_PRIVATE);
            JSONObject notices = new JSONObject(sharedPref.getString("notices", "{}"));
            try {
                if (notices.getJSONObject(nid + "").getString("content").length() > 0) {
                    //notice already exists
                }
            }catch (Exception e) {
                JSONObject notice = new JSONObject();
                notice.put("title", title).put("content", content).put("date", date);
                notices.put(nid + "", notice);
                sharedPref.edit().putString("notices", notices.toString()).commit();
                //show notice
                showPopup(context,title,date+"\n\n"+content);
            }
        }catch (Exception e){e.printStackTrace();}
    }

}

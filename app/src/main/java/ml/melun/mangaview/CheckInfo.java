package ml.melun.mangaview;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

import ml.melun.mangaview.mangaview.CustomHttpClient;
import okhttp3.Response;

import static ml.melun.mangaview.Utils.showPopup;

public class CheckInfo {
    Context context;
    updateCheck uc;
    noticeCheck nc;
    SharedPreferences sharedPref;
    CustomHttpClient client;
    public CheckInfo(Context context, CustomHttpClient client){
        this.context = context;
        this.client = client;
        uc = new updateCheck();
        nc = new noticeCheck();
        sharedPref = context.getSharedPreferences("mangaView",Context.MODE_PRIVATE);
    }
    public void all(Boolean force){
        update(force);
        notice(force);
    }
    public Boolean update(Boolean force){
        Long lastUpdateTime = sharedPref.getLong("lastUpdateTime", 0);
        Long updateCycle = sharedPref.getLong("updateCycle",3600000); // def cycle = 1hr
        if(uc.getStatus()== AsyncTask.Status.RUNNING) {
            Toast.makeText(context, "이미 실행중입니다. 잠시후에 다시 시도해 주세요",Toast.LENGTH_SHORT).show();
            return false;
        }
        else{
            if ((System.currentTimeMillis()>lastUpdateTime + updateCycle) || force)
                uc.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return true;
        }
    }
    public Boolean notice(Boolean force) {
        Long lastUpdateTime = sharedPref.getLong("lastNoticeTime", 0);
        Long updateCycle = sharedPref.getLong("noticeCycle",3600000); // def cycle = 1hr
        if (nc.getStatus() == AsyncTask.Status.RUNNING){
            Toast.makeText(context, "이미 실행중입니다. 잠시후에 다시 시도해 주세요",Toast.LENGTH_SHORT).show();
            return false;
        }
        else {
            if ((System.currentTimeMillis()>lastUpdateTime + updateCycle) || force)
                nc.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return true;
        }
    }


    private class noticeCheck extends AsyncTask<Void, Void, Integer> {
        Notice notice;
        protected void onPreExecute() {
            super.onPreExecute();
            sharedPref.edit().putLong("lastNoticeTime", System.currentTimeMillis()).commit();
        }
        protected Integer doInBackground(Void... params) {
            //get all notices
            try {
                Response response = client.getRaw("https://raw.githubusercontent.com/junheah/MangaViewAndroid/master/etc/notice.json", new HashMap<>());
                String rawdata = response.body().string();
                response.close();
                notice = new Gson().fromJson(rawdata, new TypeToken<Notice>(){}.getType());
            }catch (Exception e){
                e.printStackTrace();
            }
            return 0;
        }
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            showNotice(notice);
        }
    }

    public class updateCheck extends AsyncTask<Void, Integer, Integer> {
        int version = 0;
        int newVersion = 0;
        JSONObject data;
        protected void onPreExecute() {
            sharedPref.edit().putLong("lastUpdateTime", System.currentTimeMillis()).commit();
            super.onPreExecute();
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                version = pInfo.versionCode;
                //version=0;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            Toast.makeText(context, "업데이트 확인중..", Toast.LENGTH_SHORT).show();
        }

        protected Integer doInBackground(Void... params) {
            try {
                Response response = client.getRaw("https://api.github.com/repos/junheah/MangaViewAndroid/releases/latest",new HashMap<>());
                String rawdata = response.body().string();
                response.close();
                data = new JSONObject(rawdata);
                newVersion = Integer.parseInt(data.getString("tag_name"));
                if(version<newVersion) {
                    return 1;
                }
            }catch(Exception e){
                e.printStackTrace();
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
                    showPrompt(data);
                   //create popup
                    break;
            }
        }
    }

    void showPrompt(JSONObject data){
        try{
            final String page = "http://mangaview.ml";
            final String message = "버전: " + data.getString("tag_name") +"\n체인지 로그:\n"+ data.getString("body");
            final String url = data.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
            AlertDialog.Builder builder;
            if(new Preference(context).getDarkTheme()) builder = new AlertDialog.Builder(context,R.style.darkDialog);
            else builder = new AlertDialog.Builder(context);
            builder.setTitle("업데이트")
                    .setMessage(message)
                    .setPositiveButton("다운로드", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int button) {
                            Intent downloader = new Intent(context.getApplicationContext(),Downloader.class);
                            downloader.setAction(Downloader.ACTION_UPDATE);
                            downloader.putExtra("url", url);
                            if (Build.VERSION.SDK_INT >= 26) {
                                context.startForegroundService(downloader);
                            }else{
                                context.startService(downloader);
                            }
                            Toast.makeText(context,"다운로드를 시작합니다.",Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("나중에", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int button) {
                            //do nothing
                        }
                    })
                    .setNeutralButton("사이트로 이동", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(page)));
                        }
                    })
                    .show();
        }catch (Exception e){
            e.printStackTrace();
            showPopup(context,"업데이터", "오류 발생");
        }
    }

    void showNotice(Notice notice){
        //공지 표시
        try {
            SharedPreferences sharedPref = context.getSharedPreferences("mangaView", Context.MODE_PRIVATE);
            List<Notice> notices = new Gson().fromJson(sharedPref.getString("notice", "[]"), new TypeToken<List<Notice>>(){}.getType());
            if(notice!=null&&!notices.contains(notice)){
                //add new notice
                notices.add(notice);
                //write notices
                sharedPref.edit().putString("notice", new Gson().toJson(notices)).commit();
                showPopup(context,notice.getTitle(),notice.getDate()+"\n\n"+notice.getContent());
            }

        }catch (Exception e){e.printStackTrace();}
    }

}

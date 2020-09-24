package ml.melun.mangaview;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.activity.MainActivity;
import ml.melun.mangaview.mangaview.DownloadTitle;
import ml.melun.mangaview.mangaview.MTitle;
import ml.melun.mangaview.mangaview.MainPage;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.mangaview.MTitle.base_comic;

public class Migrator extends Service {
    NotificationCompat.Builder notification;
    NotificationManager notificationManager;
    public static final int nid = 16848412;
    public static final String channeld = "MangaViewMG";
    Context serviceContext;
    PendingIntent pendingIntent;
    MigrationWorker mw;
    public static boolean running = false;
    String url = "";
    Intent resultIntent;

    public static final String MIGRATE_STOP = "ml.melun.mangaview.migrator.STOP";
    public static final String MIGRATE_START = "ml.melun.mangaview.migrator.START";
    public static final String MIGRATE_SUCCESS = "ml.melun.mangaview.migrator.SUCCESS";
    public static final String MIGRATE_FAIL = "ml.melun.mangaview.migrator.FAIL";
    public static final String MIGRATE_PROGRESS = "ml.melun.mangaview.migrator.PROGRESS";
    public static final String MIGRATE_RESULT = "ml.melun.mangaview.migrator.RESULT";

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            //notificationManager.deleteNotificationChannel("mangaView");
            NotificationChannel mchannel = new NotificationChannel(channeld, "MangaView", NotificationManager.IMPORTANCE_LOW);
            mchannel.setDescription("데이터 업데이트");
            mchannel.enableLights(true);
            mchannel.setLightColor(Color.MAGENTA);
            mchannel.enableVibration(false);
            mchannel.setSound(null, null);
            mchannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(mchannel);
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        resultIntent = new Intent(this, MainActivity.class);
        serviceContext = this;
        startNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        running = true;
        if(intent!=null){
            switch (intent.getAction()) {
                case MIGRATE_START:
                    startNotification();
                    if (mw == null) mw = new MigrationWorker();
                    mw.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                case MIGRATE_STOP:
                    //
                    break;
                case MIGRATE_PROGRESS:
                    if(mw != null){
                        mw.onProgressUpdate("...");
                    }
                    break;
            }
        }
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        running = false;
        endNotification();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startNotification() {
        notification = new NotificationCompat.Builder(this, channeld)
                .setContentIntent(pendingIntent)
                .setContentTitle("기록 업데이트중..")
                .setContentText("진행률을 확인하려면 터치")
                .setOngoing(true);
        if (Build.VERSION.SDK_INT >= 26)
            notification.setSmallIcon(R.drawable.ic_logo);
        else
            notification.setSmallIcon(R.drawable.notification_logo);
        startForeground(nid, notification.build());
    }


    private void endNotification(){
        PendingIntent resultPendingIntent = PendingIntent.getActivity(serviceContext, 0, resultIntent, 0);
        notification = new NotificationCompat.Builder(this, channeld)
                .setContentIntent(resultPendingIntent)
                .setContentTitle("기록 업데이트 완료")
                .setContentText("결과를 확인하려면 터치")
                .setOngoing(false);
        if (Build.VERSION.SDK_INT >= 26)
            notification.setSmallIcon(R.drawable.ic_logo);
        else
            notification.setSmallIcon(R.drawable.notification_logo);
        notificationManager.notify(nid, notification.build());
    }

    private void sendBroadcast(String action){
        sendBroadcast(action, null);
    }

    private void sendBroadcast(String action, String msg){
        Intent intent = new Intent();
        intent.setAction(action);
        if(msg!=null && msg.length()>0)
            intent.putExtra("msg", msg);
        sendBroadcast(intent);
    }

    private class MigrationWorker extends AsyncTask<Void, String, Integer> {

        int sum = 0;
        int current = 0;
        List<MTitle> newFavorites, newRecents;
        List<String> failed;
        Bundle bundle;

        @Override
        protected void onPreExecute() {
            startNotification();
        }


        @Override
        protected void onProgressUpdate(String... values) {
            String msg = current +" / " + sum+"\n앱을 종료하지 말아주세요.\n";
            if(values !=null && values.length>0) msg += values[0];
            sendBroadcast(MIGRATE_PROGRESS, msg);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            // check domain
            MainPage mp = new MainPage(httpClient);
            if(mp.getRecent().size()<1)
                return 1;

            List<MTitle> recents = p.getRecent();
            sum += recents.size();
            List<MTitle> favorites = p.getFavorite();
            sum += favorites.size();
            //recent data

            removeDups(favorites);
            removeDups(recents);

            newRecents = new ArrayList<>();
            newFavorites = new ArrayList<>();
            failed = new ArrayList<>();

            for(int i=0; i<recents.size(); i++){
                try {
                    current++;
                    MTitle newTitle = findTitle(recents.get(i));
                    publishProgress(newTitle.getName());
                    if(newTitle !=null)
                        newRecents.add(newTitle);
                    else
                        failed.add(recents.get(i).getName());
                }catch (Exception e){
                    e.printStackTrace();
                    failed.add(recents.get(i).getName());
                }
            }
            for(int i=0; i<favorites.size(); i++){
                try {
                    current++;
                    MTitle newTitle = findTitle(favorites.get(i));
                    publishProgress(newTitle.getName());
                    if(newTitle !=null)
                        newFavorites.add(newTitle);
                    else
                        failed.add(favorites.get(i).getName());
                }catch (Exception e){
                    e.printStackTrace();
                    failed.add(favorites.get(i).getName());
                }
            }

            p.setFavorites(newFavorites);
            p.setRecents(newRecents);

            //remove bookmarks
            p.resetViewerBookmark();
            p.resetBookmark();

            return 0;
        }

        void removeDups(List<MTitle> titles){
            for(int i=0; i<titles.size(); i++){
                MTitle target = titles.get(i);
                for(int j =0 ; j<titles.size(); j++){
                    if(j!=i && titles.get(j).getId() == target.getId()){
                        titles.remove(i);
                        i--;
                        break;
                    }
                }
            }
        }

        MTitle findTitle(String title){
            return findTitle(new MTitle(title,-1,"", "",new ArrayList<>(),"", base_comic));
        }

        MTitle findTitle(MTitle title){
            String name = title.getName();
            Search s = new Search(name,0, base_comic);
            while(!s.isLast()){
                s.fetch(httpClient);
                for(Title t : s.getResult()){
                    if(t.getName().equals(name)){
                        return t.minimize();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer resCode) {
            if(resCode == 0){
                StringBuilder builder = new StringBuilder();
                builder.append("기록 업데이트 완료.\n실패한 항목: ");
                builder.append(failed.size());
                builder.append("개\n");
                for(String t : failed){
                    builder.append("\n"+t);
                }
                resultIntent.setAction(MIGRATE_RESULT);
                resultIntent.putExtra("msg",builder.toString());
                endNotification();
                sendBroadcast(MIGRATE_SUCCESS, builder.toString());
            }
            else if(resCode == 1) {
                endNotification();
                sendBroadcast(MIGRATE_FAIL, "연결 오류 : 연결을 확인하고 다시 시도해 주세요.");
            }
            running = false;
            stopSelf();
        }

        @Override
        protected void onCancelled() {
            //todo?
        }
    }


}

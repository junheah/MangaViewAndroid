package ml.melun.mangaview;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.widget.Toast;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import ml.melun.mangaview.activity.MainActivity;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.Utils.filterFolder;

public class Downloader extends Service {
    File homeDir;
    String baseUrl;
    ArrayList<Title> titles;
    ArrayList<JSONArray> selected;
    float progress = 0;
    int maxProgress=1000;
    String notiTitle="";
    Boolean running = false;
    NotificationCompat.Builder notification;
    public static final String ACTION_START = "ml.melun.mangaview.action.START";
    public static final String ACTION_STOP = "ml.melun.mangaview.action.STOP";
    public static final String ACTION_QUEUE = "ml.melun.mangaview.action.QUEUE";
    public static final String ACTION_UPDATE = "ml.melun.mangaview.action.UPDATE";
    downloadTitle dt;
    Download d;
    NotificationManager notificationManager;
    int nid = 16848323;
    String channeld = "MangaViewDL";
    PendingIntent pendingIntent;
    PendingIntent stopIntent;
    Context serviceContext;


    @Override
    public void onCreate() {
        super.onCreate();
        if(titles==null) titles = new ArrayList<>();
        if(selected==null) selected = new ArrayList<>();
        homeDir = new File(getApplicationContext().getSharedPreferences("mangaView",Context.MODE_PRIVATE).getString("homeDir","/sdcard/MangaView/saved"));
        baseUrl = getApplicationContext().getSharedPreferences("mangaView",Context.MODE_PRIVATE).getString("url", "http://188.214.128.5");
        if(dt==null) dt = new downloadTitle();
        //android O bullshit
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            //notificationManager.deleteNotificationChannel("mangaView");
            NotificationChannel mchannel = new NotificationChannel(channeld, "MangaView", NotificationManager.IMPORTANCE_LOW);
            mchannel.setDescription("다운로드 상태");
            mchannel.enableLights(true);
            mchannel.setLightColor(Color.MAGENTA);
            mchannel.enableVibration(false);
            mchannel.setSound(null, null);
            mchannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(mchannel);
        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Intent previousIntent = new Intent(this, Downloader.class);
        previousIntent.setAction(ACTION_STOP);
        stopIntent = PendingIntent.getService(this, 0, previousIntent, 0);
        serviceContext = this;
        startNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch(intent.getAction()){
            case ACTION_START:
                break;
            case ACTION_QUEUE:
                startNotification();
                if(dt==null) dt = new downloadTitle();
                try {
                    Title target = new Gson().fromJson(intent.getStringExtra("title"), new TypeToken<Title>() {
                    }.getType());
                    JSONArray selection = new JSONArray(intent.getStringExtra("selected"));
                    queueTitle(target,selection);
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case ACTION_STOP:
                dt.cancel(true);
                break;
            case ACTION_UPDATE:
                if(dt==null) dt = new downloadTitle();
                String url = intent.getStringExtra("url");
                update(url);
                break;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        finishNotification();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void queueTitle(Title title, JSONArray selection){
        titles.add(title);
        selected.add(selection);
        updateNotification("");
        if(dt.getStatus() == AsyncTask.Status.PENDING || dt.getStatus() == AsyncTask.Status.FINISHED) {
            dt = new downloadTitle();
            dt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else{
            running = true;
        }
    }

    public void update(String url){
        //saves to android default download dir
        if(d==null) d = new Download();
        if(d.getStatus() == AsyncTask.Status.PENDING || d.getStatus() == AsyncTask.Status.FINISHED) {
            d = new Download();
            d.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
        }else{
            updateDownloading = true;
            Toast.makeText(serviceContext, "이미 다운로드 중 입니다.", Toast.LENGTH_SHORT).show();
        }
    }

    Boolean updateDownloading = false;

    private class Download extends AsyncTask<String,Void,Integer> {
        File downloaded;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            updateDownloading = true;
            Intent intent = new Intent(serviceContext, MainActivity.class);
            PendingIntent intentP = PendingIntent.getActivity(serviceContext, 0, intent, 0);
            NotificationCompat.Builder noti = new NotificationCompat.Builder(serviceContext, channeld)
                    .setContentIntent(intentP)
                    .setContentTitle("업데이트 다운로드중")
                    .setSmallIcon(R.drawable.ic_logo)
                    .setOngoing(true);
            notificationManager.notify(nid+3, noti.build());
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            downloaded.setReadable(true,false);
            Uri fileUri = Uri.fromFile(downloaded);
            if (Build.VERSION.SDK_INT >= 24) {
                fileUri = FileProvider.getUriForFile(serviceContext, serviceContext.getPackageName()+".provider", downloaded);
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            PendingIntent installP = PendingIntent.getActivity(serviceContext, 0, intent, 0);
            NotificationCompat.Builder  noti = new NotificationCompat.Builder(serviceContext, channeld)
                    .setContentIntent(installP)
                    .setContentTitle("업데이트 다운로드 완료")
                    .setContentText("지금 설치하려면 터치")
                    .setSmallIcon(R.drawable.ic_logo)
                    .setOngoing(false);
            notificationManager.notify(nid+3, noti.build());
            updateDownloading = false;
            if(!running) stopSelf();
        }

        @Override

        protected Integer doInBackground(String... urls) {
            String url = urls[0];
            downloaded = downloadFile(url, new File(serviceContext.getExternalFilesDir(null).getAbsolutePath(),"mangaview-update"));
            return null;
        }

        Uri getFileUri(Context context, File file) {
            return FileProvider.getUriForFile(context,
                    context.getApplicationContext().getPackageName() + "."
                    , file);
        }
    }

    private class downloadTitle extends AsyncTask<Void,Void,Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            running = true;
        }
        protected Integer doInBackground(Void... params) {
            try {
                while (titles.size() > 0) {
                    //check homeDir exist
                    if (!homeDir.exists()) {
                        this.cancel(true);
                        return 1;
                    }

                    //reset progress
                    progress = 0;

                    //get item from queue
                    Title title = titles.get(0);
                    JSONArray selectedEps = selected.get(0);

                    notiTitle = title.getName();
                    updateNotification("준비중");

                    if (title.getEps() == null) title.fetchEps(baseUrl);
                    List<Manga> mangas = title.getEps();

                    float stepSize = maxProgress / selectedEps.length();
                    for (int queueIndex = selectedEps.length()-1; queueIndex >= 0; queueIndex--) {
                        if (isCancelled()) return 0;

                        //create dir for title
                        File titleDir = new File(homeDir, filterFolder(title.getName()));
                        if (!titleDir.exists()) titleDir.mkdirs();

                        //if first manga, save title data
                        if (queueIndex == selectedEps.length()-1) {
                            try {
                                //save thumbnail
                                String thumb = downloadFile(title.getThumb(), new File(titleDir, "thumb")).getName();
                                title.setThumb(thumb);
                                //if first manga, save title data & id list to title.data as JSON
                                //title.removeEps();
//                                JSONObject json = new JSONObject();
//                                json.put("title", new JSONObject(new Gson().toJson(title)));
//                                JSONArray ids = new JSONArray();
//                                for (int i = mangas.size() - 1; i >= 0; i--) {
//                                    //save manga id to JSONArray
//                                    ids.put(mangas.get(i).getId());
//                                }
//                                json.put("ids", ids);

                                //if old title.data exist, remove file
                                File old = new File(titleDir,"title.data");
                                if(old.exists()) old.delete();

                                //save the whole title as gson
                                File summary = new File(titleDir, "title.gson");
                                summary.createNewFile();

                                FileOutputStream stream = new FileOutputStream(summary);
                                stream.write(new Gson().toJson(title).getBytes());
                                stream.flush();
                                stream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        //get index from JSONArray
                        int listIndex = 0;
                        try {
                            listIndex = selectedEps.getInt(queueIndex);
                        } catch (Exception e) {
                            this.cancel(true);
                            return 2;
                        }
                        Manga target = mangas.get(listIndex);

                        //fetch info of target
                        target.fetch(baseUrl);
                        Decoder d = new Decoder(target.getSeed(), target.getId());
                        List<String> urls = target.getImgs();

                        //set stepsize
                        float imgStepSize = stepSize / urls.size();

                        //create dir for manga
                        int realIndex = mangas.size()-mangas.indexOf(target);
                        File dir = new File(titleDir, filterFolder(new DecimalFormat("0000").format(realIndex)+"."+target.getName())+"."+target.getId());
                        if (!dir.exists()) dir.mkdirs();

                        //create download flag
                        File downloadFlag = new File(dir,"downloading");
                        downloadFlag.createNewFile();
                        for (int i = 0; i < urls.size(); i++) {
                            if (isCancelled()) return 0;
                            downloadImage(urls.get(i), new File(dir, new DecimalFormat("0000").format(i)), d);
                            progress += imgStepSize;
                            updateNotification((selectedEps.length() - queueIndex) + "/" + selectedEps.length());
                        }
                        downloadFlag.delete();
                    }
                    titles.remove(0);
                    selected.remove(0);
                }
            }catch (Exception e){
                //unexpected exception
                e.printStackTrace();
                this.cancel(true);
                return 3;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            endNotification();
            running = false;
            if(!updateDownloading) stopSelf();
        }

        @Override
        protected void onCancelled(Integer mode) {
            super.onCancelled();
            running = false;
            String why = "";
            switch(mode){
                case 0:
                    why = "유저 취소";
                    break;
                case 1:
                    why = "쓰기 실패";
                    break;
                case 2:
                    why = "만화 정보 파싱 실패";
                    break;
                case 3:
                    why = "예상치 못한 오류";
                    break;
            }
            notificationManager.cancel(nid);
            stopNotification(why);
            if(!updateDownloading) stopSelf();
        }
    }

    void downloadImage(String urlStr, File outputFile, Decoder d){
        try {
            URL url = new URL(urlStr);
            if(url.getProtocol().toLowerCase().equals("https")) {
                HttpsURLConnection init = (HttpsURLConnection) url.openConnection();
                int responseCode = init.getResponseCode();
                if (responseCode >= 300) {
                    url = new URL(init.getHeaderField("location"));
                }
            }else{
                HttpURLConnection init = (HttpURLConnection) url.openConnection();
                int responseCode = init.getResponseCode();
                if (responseCode >= 300) {
                    url = new URL(init.getHeaderField("location"));
                }
            }
            //String fileType = url.toString().substring(url.toString().lastIndexOf('.') + 1);
            URLConnection connection = url.openConnection();

            //load image as bitmap
            InputStream in = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            //decode image
            bitmap = d.decode(bitmap);
            //save image
            OutputStream outputStream = new FileOutputStream(outputFile.getAbsolutePath()+".jpg");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            in.close();
            outputStream.flush(); // Not really required
            outputStream.close(); // do not forget to close the stream
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    File downloadFile(String urlStr, File outputFile){
        //returns file name with extension
        String name = "";
        try {
            URL url = new URL(urlStr);
            if(url.getProtocol().toLowerCase().equals("https")) {
                HttpsURLConnection init = (HttpsURLConnection) url.openConnection();
                int responseCode = init.getResponseCode();
                if (responseCode >= 300) {
                    url = new URL(init.getHeaderField("location"));
                }
            }else{
                HttpURLConnection init = (HttpURLConnection) url.openConnection();
                int responseCode = init.getResponseCode();
                if (responseCode >= 300) {
                    url = new URL(init.getHeaderField("location"));
                }
            }
            String fileType = url.toString().substring(url.toString().lastIndexOf('.') + 1);
            URLConnection connection = url.openConnection();

            //load file
            InputStream in = connection.getInputStream();
            outputFile = new File(outputFile.getAbsolutePath()+'.'+fileType);
            name = outputFile.getName();
            OutputStream outputStream = new FileOutputStream(outputFile);
            //save file
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = in.read(buf)) > 0){
                outputStream.write(buf, 0, len);
            }
            in.close();
            outputStream.flush(); // Not really required
            outputStream.close(); // do not forget to close the stream
        } catch (Exception e) {
            //
            e.printStackTrace();
        }
        return outputFile;
    }
    public int getIndex(List<Manga> eps, int id){
        for(int i=0; i<eps.size(); i++){
            if(eps.get(i).getId()==id){
                return eps.size()-i;
            }
        }
        return 0;
    }
    private void startNotification() {
        notification = new NotificationCompat.Builder(this, channeld)
                .setContentIntent(pendingIntent)
                .setContentTitle("다운로드를 시작합니다")
                .setSmallIcon(R.drawable.ic_logo)
                .setOngoing(true);
        startForeground(nid, notification.build());
    }
    private void updateNotification(String text) {
        notification = new NotificationCompat.Builder(this, channeld)
                .setContentIntent(pendingIntent)
                .setContentTitle(notiTitle)
                .setSubText("대기열: "+ titles.size())
                .setContentText(text)
                .addAction(R.drawable.blank, "중지", stopIntent)
                .setProgress(maxProgress, (int)progress, !(progress > 0))
                .setSmallIcon(R.drawable.ic_logo)
                .setOngoing(true);
        notificationManager.notify(nid, notification.build());
    }

    private void endNotification(){
        notification = new NotificationCompat.Builder(this, channeld)
                .setContentIntent(pendingIntent)
                .setContentTitle("다운로드 완료")
                .setSmallIcon(R.drawable.ic_logo)
                .setOngoing(false);
        notificationManager.notify(nid, notification.build());
    }

    private void finishNotification(){
        notification = new NotificationCompat.Builder(this, channeld)
                .setContentIntent(pendingIntent)
                .setContentTitle("모든 다운로드가 완료되었습니다.")
                .setSmallIcon(R.drawable.ic_logo)
                .setOngoing(false);
        notificationManager.notify(nid+1, notification.build());
    }
    private void stopNotification(String why){
        notification = new NotificationCompat.Builder(this, channeld)
                .setContentIntent(pendingIntent)
                .setContentText(why)
                .setContentTitle("다운로드가 취소되었습니다.")
                .setSmallIcon(R.drawable.ic_logo)
                .setOngoing(false);
        notificationManager.notify(nid+2, notification.build());
    }
}

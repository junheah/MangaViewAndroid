package ml.melun.mangaview;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import ml.melun.mangaview.adapter.EpisodeAdapter;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

public class Downloader extends Service {
    String homeDir;
    String baseUrl;
    ArrayList<Title> titles;
    float progress = 0;
    String content="";
    int queue;
    Context context;
    Boolean running = false;
    NotificationCompat.Builder notification;
    public static final String ACTION_START = "ml.melu.mangaview.action.START";
    public static final String ACTION_STOP = "ml.melu.mangaview.action.STOP";
    public static final String ACTION_QUEUE = "ml.melu.mangaview.action.QUEUE";
    downloadTitle dt;
    NotificationManager notificationManager;
    int nid = 16848323;
    String channeld = "MangaViewDL";
    PendingIntent pendingIntent;
    PendingIntent stopIntent;


    @Override
    public void onCreate() {
        super.onCreate();
        if(titles==null) titles = new ArrayList<>();
        homeDir = getApplicationContext().getSharedPreferences("mangaView",Context.MODE_PRIVATE).getString("homeDir","/sdcard/MangaView/saved");
        baseUrl = getApplicationContext().getSharedPreferences("mangaView",Context.MODE_PRIVATE).getString("url", "http://188.214.128.5");
        if(dt==null) dt = new downloadTitle();
        //android O bullshit
        if (Build.VERSION.SDK_INT >= 26) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
        startNotification();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Intent previousIntent = new Intent(this, Downloader.class);
        previousIntent.setAction(ACTION_STOP);
        stopIntent = PendingIntent.getService(this, 0, previousIntent, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch(intent.getAction()){
            case ACTION_START:
                break;
            case ACTION_QUEUE:
                if(dt==null) dt = new downloadTitle();
                Title target = new Gson().fromJson(intent.getStringExtra("title"),new TypeToken<Title>(){}.getType());
                queueTitle(target);
                break;
            case ACTION_STOP:
                dt.cancel(true);
                break;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setQueue(int size){
        this.queue = size;
    }
    public void setName(String name){
        this.content = name;
    }
    public void setProgress(float progress){
        this.progress = progress;
    }


    public void queueTitle(Title title){
        if(dt.getStatus() == AsyncTask.Status.PENDING || dt.getStatus() == AsyncTask.Status.FINISHED) {
            dt = new downloadTitle();
            titles.add(title);
            dt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else{
            titles.add(title);
            running = true;
            setQueue(titles.size());
            updateNotification();
        }
    }
    private class downloadTitle extends AsyncTask<Void,Void,Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            running = true;
        }

        protected Integer doInBackground(Void... params) {
            while(titles.size()>0) {
                progress = 0;
                setQueue(titles.size());
                Title title = titles.get(0);
                setName(title.getName());
                updateNotification();
                if(title.getEps()==null) title.fetchEps(baseUrl);
                List<Manga> mangas = title.getEps();
                float stepSize = 1000/mangas.size();
                for(int h=0;h<mangas.size();h++) {
                    if(isCancelled()) return null;
                    Manga target = mangas.get(h);
                    target.fetch(baseUrl);
                    Decoder d = new Decoder(target.getSeed(), target.getId());
                    int index = getIndex(target.getEps(),target.getId());
                    List<String> urls = target.getImgs();
                    String targetDir = homeDir+'/' + filterString(title.getName())+'/'+(new DecimalFormat("0000").format(index))+". "+filterString(target.getName())+'/';
                    File dir = new File(targetDir);
                    if (!dir.exists()) dir.mkdirs();
                    //if first manga, save title data
                    if(h==0){
                        try {
                            //save thumbnail
                            String thumb = downloadFile(title.getThumb(), homeDir+'/' + filterString(title.getName()) +"/thumb");
                            title.setThumb(thumb);
                            //if first manga, save index:id list to file
                            List<Manga> realEps = target.getEps();
                            //save title class as GSON on index 0
                            title.removeEps();
                            JSONObject json = new JSONObject();
                            json.put("title", new JSONObject(new Gson().toJson(title)));
                            JSONArray ids = new JSONArray();
                            for(int i=realEps.size()-1; i>=0; i--){
                                //save manga id according to index
                                ids.put(realEps.get(i).getId());
                            }
                            json.put("ids", ids);

                            File summary = new File(homeDir+'/' + filterString(title.getName()) + "/title.data");
                            FileOutputStream stream = new FileOutputStream(summary);
                            stream.write(json.toString().getBytes());
                            stream.flush();
                            stream.close();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    float imgStepSize = stepSize/urls.size();
                    for (int i = 0; i < urls.size(); i++) {
                        if(isCancelled()) return null;
                        downloadImage(urls.get(i), targetDir + (new DecimalFormat("0000").format(i)), d);
                        progress+=imgStepSize;
                        setProgress(progress);
                        updateNotification();
                    }
                    //in case imgStepSize grounds to zero
                }
                titles.remove(0);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            endNotification();
            running = false;
            stopSelf();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            stopNotification();
            stopSelf();
        }
    }

    void downloadImage(String urlStr, String filePath, Decoder d){
        try {
            URL url = new URL(urlStr);
            if(url.getProtocol().toLowerCase().matches("https")) {
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

            //load image as bitmap
            InputStream in = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            //decode image
            bitmap = d.decode(bitmap);
            //save image
            File outputFile = new File(filePath);
            OutputStream outputStream = new FileOutputStream(outputFile+".jpg");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            in.close();
            outputStream.flush(); // Not really required
            outputStream.close(); // do not forget to close the stream
        } catch (Exception e) {
            //
            e.printStackTrace();
        }
    }

    String downloadFile(String urlStr, String filePath){
        //returns file name with extension
        String name = "";
        try {
            URL url = new URL(urlStr);
            if(url.getProtocol().toLowerCase().matches("https")) {
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
            File outputFile = new File(filePath+'.'+fileType);
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
        return name;
    }

    private String filterString(String input){
        int m=0;
        while(m>-1){
            m = input.indexOf('/');
            char[] tmp = input.toCharArray();
            if(m>-1) tmp[m] = ' ';
            input = String.valueOf(tmp);
        }
        return input;
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
    private void updateNotification() {
        notification = new NotificationCompat.Builder(this, channeld)
                .setContentIntent(pendingIntent)
                .setContentTitle(content)
                .setSubText("대기열: "+queue)
                .addAction(R.drawable.ic_logo, "중지", stopIntent)
                .setProgress(1000,(int)progress,false)
                .setSmallIcon(R.drawable.ic_logo)
                .setOngoing(true);
        notificationManager.notify(nid, notification.build());
    }

    private void endNotification(){
        notification = new NotificationCompat.Builder(this, channeld)
                .setContentIntent(pendingIntent)
                .setContentTitle("모든 다운로드가 완료되었습니다.")
                .setSmallIcon(R.drawable.ic_logo)
                .setOngoing(false);
        notificationManager.notify(nid+1, notification.build());
    }
    private void stopNotification(){
        notification = new NotificationCompat.Builder(this, channeld)
                .setContentIntent(pendingIntent)
                .setContentTitle("다운로드가 취소되었습니다.")
                .setSmallIcon(R.drawable.ic_logo)
                .setOngoing(false);
        notificationManager.notify(nid+2, notification.build());
    }
}

package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import ml.melun.mangaview.adapter.EpisodeAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

public class Downloader {
    static String homeDir = "/sdcard/MangaView/saved/";
    static downloadTitle dt;
    static ArrayList<Title> titles;

    public Downloader(){
        //static
        if(titles==null) titles = new ArrayList<>();
        if(dt==null) dt = new downloadTitle();
    }

    public void queueTitle(Title title){
        if(dt.getStatus() == AsyncTask.Status.PENDING || dt.getStatus() == AsyncTask.Status.FINISHED) {
            dt = new downloadTitle();
            titles.add(title);
            dt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            //System.out.println("pp async idle, starting async process");
        }else{
            titles.add(title);
            //System.out.println("pp async busy, queueing process");
        }
    }
    private class downloadTitle extends AsyncTask<Void,Void,Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Integer doInBackground(Void... params) {
            while(titles.size()>0) {
                //System.out.println("pp queued downloads : "+ titles.size());
                Title title = titles.get(0);
                //System.out.println("pp now downloading : "+ title.getName());
                if(title.getEps()==null) title.fetchEps();
                ArrayList<Manga> mangas = title.getEps();
                for(int h=0;h<mangas.size();h++) {
                    Manga target = mangas.get(h);
                    String targetDir = homeDir + filterString(title.getName())+'/'+filterString(target.getName())+'/';
                    File dir = new File(targetDir);
                    if (!dir.exists()) dir.mkdirs();
                    target.fetch();
                    ArrayList<String> urls = target.getImgs();
                    for (int i = 0; i < urls.size(); i++) {
                        try {
                            URL url = new URL(urls.get(i));
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
                            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                            File outputFile = new File(targetDir + (new DecimalFormat("0000").format(i)));
                            FileOutputStream out = new FileOutputStream(outputFile.toString() + '.'+fileType);
                            byte[] data = new byte[1024];
                            int length = connection.getContentLength();
                            int count;
                            while ((count = in.read(data, 0, 1024)) != -1) {
                                out.write(data, 0, count);
                            }

                        } catch (Exception e) {
                            //
                            e.printStackTrace();
                        }
                    }
                    //System.out.println("pp finished downloading : "+title.getName());
                }
                titles.remove(0);
            }
            return null;
        }
        private String filterString(String input){
            int i = input.indexOf('(');
            int j = input.indexOf(')');
            int m = input.indexOf('/');
            if(i>-1||j>-1||m>-1){
                char[] tmp = input.toCharArray();
                if(i>-1) tmp[i] = ' ';
                if(j>-1) tmp[j] = ' ';
                if(m>-1) tmp[m] = ' ';
                input = String.valueOf(tmp);
            }
            return input;
        }
        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            //System.out.println("pp asynctask done!");
        }

    }
}

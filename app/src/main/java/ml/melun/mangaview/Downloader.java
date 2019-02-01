package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Environment;


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
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

public class Downloader {
    static Preference p;
    static String homeDir;
    static downloadTitle dt;
    static ArrayList<Title> titles;
    static Listener listener;
    static int status =0;
    static float progress = 0;
    static Context context;

    public Downloader(Context context){
        //static
        if(titles==null) titles = new ArrayList<>();
        if(dt==null) dt = new downloadTitle();
        if(p==null) p = new Preference(context);
        if(this.context == null) this.context = context;
    }
    //pocess status no.
    // 0=idle 1=downloading

    public void queueTitle(Title title){
        homeDir = p.getHomeDir();
        if(dt.getStatus() == AsyncTask.Status.PENDING || dt.getStatus() == AsyncTask.Status.FINISHED) {
            dt = new downloadTitle();
            titles.add(title);
            dt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            //System.out.println("pp async idle, starting async process");
        }else{
            titles.add(title);
            status = 1;
            setStatus();
            sendQueue(titles.size());
            //System.out.println("pp async busy, queueing process");
        }
    }
    private class downloadTitle extends AsyncTask<Void,Void,Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            status = 1;
            setStatus();
        }

        protected Integer doInBackground(Void... params) {
            while(titles.size()>0) {
                progress = 0;
                sendQueue(titles.size());
                Title title = titles.get(0);
                sendName(title.getName());
                if(title.getEps()==null) title.fetchEps();
                ArrayList<Manga> mangas = title.getEps();
                float stepSize = 1000/mangas.size();
                for(int h=0;h<mangas.size();h++) {
                    Manga target = mangas.get(h);
                    target.fetch();
                    int seed = target.getSeed();
                    int index = getIndex(target.getEps(),target.getId());
                    ArrayList<String> urls = target.getImgs();

                    String targetDir = homeDir+'/' + filterString(title.getName())+'/'+(new DecimalFormat("0000").format(index))+". "+filterString(target.getName())+'/';
                    File dir = new File(targetDir);
                    if (!dir.exists()) dir.mkdirs();

                    if(h==0){
                        try {
                            //if first manga, save index:id list to file
                            ArrayList<Manga> realEps = target.getEps();
                            String data = title.getAuthor();
                            for(int i=realEps.size()-1; i>=0; i--){
                                data = data +','+realEps.get(i).getId();
                            }
                            File summary = new File(homeDir+'/' + filterString(title.getName()) + "/id.list");
                            FileOutputStream stream = new FileOutputStream(summary);
                            stream.write(data.getBytes());
                            stream.flush();
                            stream.close();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    float imgStepSize = stepSize/urls.size();
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

                            /*
                            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                            File outputFile = new File(targetDir + (new DecimalFormat("0000").format(i)));
                            FileOutputStream out = new FileOutputStream(outputFile.toString() + '.'+fileType);
                            byte[] data = new byte[1024];
                            int length = connection.getContentLength();
                            int count;
                            while ((count = in.read(data, 0, 1024)) != -1) {
                                out.write(data, 0, count);
                            }
                            */

                            //load image as bitmap
                            InputStream in = connection.getInputStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(in);
                            //decode image
                            if(seed!=0) {
                                bitmap = decode(bitmap, seed);
                            }
                            //save image
                            File outputFile = new File(targetDir + (new DecimalFormat("0000").format(i)) + ".jpg");
                            OutputStream outputStream = new FileOutputStream(outputFile);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
                            outputStream.flush(); // Not really required
                            outputStream.close(); // do not forget to close the stream


                            progress+=imgStepSize;
                            sendProgress((int)progress);

                        } catch (Exception e) {
                            //
                            e.printStackTrace();
                        }
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
            status = 0;
            setStatus();
        }

    }
    public void sendName(String text){ if(listener!=null) listener.changeNameStr(text); }
    public void setStatus(){ if(listener!=null) listener.processStatus(status); }
    public void sendQueue(int n){ if(listener!=null) listener.changeNo(n); }
    public void sendProgress(int p){ if(listener!=null) listener.setProgress(p);}

    public static int getStatus() {
        return status;
    }

    public void addListener(Listener l){
        listener = l;
    }

    public interface Listener{
        void changeNameStr(String name);
        void changeNo(int n);
        void processStatus(int s);
        void setProgress(int p);
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
    public Bitmap decode(Bitmap input, int __seed){
        if(__seed==0) return input;
        //decode image
        int[][] order = new int[25][2];
        for(int i=0; i<25; i++) {
            order[i][0] = i;
            order[i][1] = _random(__seed,i);
        }
        java.util.Arrays.sort(order, new java.util.Comparator<int[]>() {
            public int compare(int[] a, int[] b) {
                return Double.compare(a[1], b[1]);
            }
        });
        //create new bitmap
        Bitmap output = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        int row_w = input.getWidth()/5;
        int row_h = input.getHeight()/5;
        for(int i=0; i<25; i++){
            int[]o = order[i];
            int ox = i%5;
            int oy = i/5;
            int tx = o[0]%5;
            int ty = o[0]/5;
            Bitmap cropped = Bitmap.createBitmap(input,ox*row_w,oy*row_h,row_w,row_h);
            canvas.drawBitmap(cropped,tx*row_w,ty*row_h,null);
        }
        return output;
    }
    public int _random(int __seed, int index){
        double x = Math.sin(__seed+index) * 10000;
        return (int) Math.floor((x - Math.floor(x)) * 100000);
    }
    public int getIndex(ArrayList<Manga> eps, int id){
        for(int i=0; i<eps.size(); i++){
            if(eps.get(i).getId()==id){
                return eps.size()-i;
            }
        }
        return 0;
    }
}

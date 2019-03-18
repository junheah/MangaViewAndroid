package ml.melun.mangaview;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import ml.melun.mangaview.activity.EpisodeActivity;
import ml.melun.mangaview.activity.ViewerActivity;
import ml.melun.mangaview.activity.ViewerActivity2;
import ml.melun.mangaview.activity.ViewerActivity3;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

public class Utils {
    public static Boolean deleteRecursive(File fileOrDirectory) {
        if(!checkWriteable(fileOrDirectory)) return false;
        try {
            if (fileOrDirectory.isDirectory())
                for (File child : fileOrDirectory.listFiles())
                    if(!deleteRecursive(child)) return false;
            fileOrDirectory.delete();
        }catch (Exception e){
            return false;
        }
        return true;
    }

    public static boolean checkWriteable(File targetDir) {
        if(targetDir.isDirectory()) {
            File tmp = new File(targetDir, "mangaViewTestFile");
            try {
                if (tmp.createNewFile()) tmp.delete();
                else return false;
            } catch (Exception e) {
                return false;
            }
            return true;
        }else{
            File tmp = new File(targetDir.getParent(), "mangaViewTestFile");
            try {
                if (tmp.createNewFile()) tmp.delete();
                else return false;
            } catch (Exception e) {
                return false;
            }
            return true;
        }
    }

    public static String httpsGet(String urlin){
        HttpsURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlin);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Encoding", "*");
            connection.setRequestProperty("Accept", "*");
            connection.connect();
            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static Intent episodeIntent(Context context,Title title){
        Intent episodeView = new Intent(context, EpisodeActivity.class);
        episodeView.putExtra("title", new Gson().toJson(title));
        return episodeView;
    }

    public static Intent viewerIntent(Context context, Manga manga){
        Intent viewer = null;
        switch (new Preference(context).getViewerType()){
            case 0:
                viewer = new Intent(context, ViewerActivity.class);
                break;
            case 2:
                viewer = new Intent(context, ViewerActivity3.class);
                break;
            case 1:
                viewer = new Intent(context, ViewerActivity2.class);
                break;
        }
        viewer.putExtra("manga",new Gson().toJson(manga));
        return viewer;
    }
    public static void showPopup(Context context, String title, String content, DialogInterface.OnClickListener clickListener, DialogInterface.OnCancelListener cancelListener){
        AlertDialog.Builder builder;
        if (new Preference(context).getDarkTheme()) builder = new AlertDialog.Builder(context, R.style.darkDialog);
        else builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(content)
                .setPositiveButton("확인", clickListener)
                .setOnCancelListener(cancelListener)
                .show();
    }
    public static void showPopup(Context context, String title, String content){
        AlertDialog.Builder builder;
        if (new Preference(context).getDarkTheme()) builder = new AlertDialog.Builder(context, R.style.darkDialog);
        else builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(content)
                .setPositiveButton("확인", null)
                .show();
    }

    static char[] filter = {'/','?','*',':','|','<','>','\\'};
    static public String filterFolder(String input){
        for(int i=0; i<filter.length;i++) {
            int index = input.indexOf(filter[i]);
            while(index>=0) {
                char tmp[] = input.toCharArray();
                tmp[index] = ' ';
                input = String.valueOf(tmp);
                index = input.indexOf(filter[i]);
            }
        }
        return input;
    }

    static public String readFileToString(File data){
        StringBuilder raw = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(data));
            String line;
            while ((line = br.readLine()) != null) {
                raw.append(line);
            }
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return raw.toString();
    }

    public static Bitmap getSample(Bitmap input, int width){
        //scale down bitmap to avoid outofmem exception
        if(input.getWidth()<=width) return input;
        else{
            //ratio
            float ratio = (float)input.getHeight()/(float)input.getWidth();
            int height = Math.round(ratio*width);
            return Bitmap.createScaledBitmap(input, width, height,false);
        }
    }

    public static int getScreenSize(Display display){
        Point size = new Point();
        display.getSize(size);
        return (size.x>=size.y)? size.x : size.y;
    }
}

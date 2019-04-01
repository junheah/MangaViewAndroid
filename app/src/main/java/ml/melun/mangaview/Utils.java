package ml.melun.mangaview;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import javax.net.ssl.HttpsURLConnection;

import ml.melun.mangaview.activity.EpisodeActivity;
import ml.melun.mangaview.activity.ViewerActivity;
import ml.melun.mangaview.activity.ViewerActivity2;
import ml.melun.mangaview.activity.ViewerActivity3;
import ml.melun.mangaview.mangaview.Login;
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

    public static String httpsGet(String urlin, String cookie){
        BufferedReader reader = null;
        try {
            InputStream stream = null;
            URL url = new URL(urlin);
            if(url.getProtocol().equals("http")){
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept-Encoding", "*");
                connection.setRequestProperty("Accept", "*");
                connection.setRequestProperty("Cookie",cookie);
                connection.connect();
                stream = connection.getInputStream();
            }else if(url.getProtocol().equals("https")){
                HttpsURLConnection connections = (HttpsURLConnection) url.openConnection();
                connections.setInstanceFollowRedirects(false);
                connections.setRequestMethod("GET");
                connections.setRequestProperty("Accept-Encoding", "*");
                connections.setRequestProperty("Accept", "*");
                connections.setRequestProperty("Cookie",cookie);
                connections.connect();
                stream = connections.getInputStream();
            }
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

    public static String httpsGet(String urlin){
        return httpsGet(urlin, "");
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

    public static void showErrorPopup(Context context, Exception e){
        AlertDialog.Builder builder;
        String title = "뷰어 오류";
        String content = "만화 정보를 불러오는데 실패하였습니다. 연결 상태를 확인하고 다시 시도해 주세요.";
        if (new Preference(context).getDarkTheme()) builder = new AlertDialog.Builder(context, R.style.darkDialog);
        else builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(content)
                .setNeutralButton("자세히", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showStackTrace(context, e);
                    }
                })
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((Activity)context).finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        ((Activity)context).finish();
                    }
                })
                .show();
    }

    private static void showStackTrace(Context context, Exception e){
        StringBuilder sbuilder = new StringBuilder();
        for(StackTraceElement s : e.getStackTrace()){
            sbuilder.append(s+"\n");
        }
        final String error = sbuilder.toString();
        AlertDialog.Builder builder;
        String title = "STACK TRACE";
        if (new Preference(context).getDarkTheme()) builder = new AlertDialog.Builder(context, R.style.darkDialog);
        else builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(error)
                .setNeutralButton("복사", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("stack_trace", error);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context,"클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
                        ((Activity)context).finish();
                    }
                })
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((Activity)context).finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        ((Activity)context).finish();
                    }
                })
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
        int width = size.x>size.y ? size.x : size.y;
        //max pixels : 3000 ?
        return width>3000 ? 3000 : width ;
    }

    public static Boolean writeComment(Login login, int id, String content, String baseUrl){
        String cookie = "PHPSESSID="+login.getCookie()+';';
        try {
            String token = new JSONObject(httpsGet(baseUrl + "/bbs/ajax.comment_token.php?_="+System.currentTimeMillis(), cookie)).getString("token");
            URL url = new URL(baseUrl + "/bbs/write_comment_update.php");
            String param = "token="+token
                    +"&w=c&bo_table=manga&wr_id="+id
                    +"&comment_id=&pim=&sca=&sfl=&stx=&spt=&page=&is_good=0&wr_content="+URLEncoder.encode(content, "UTF-8");
            byte[] data = param.getBytes(Charset.forName("UTF-8"));
            int responseCode = 0;
            if(url.getProtocol().equals("http")){
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Cookie",cookie);
                connection.setRequestProperty("Accept", "*");
                connection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty( "Content-Length", Integer.toString(data.length));
                connection.setRequestProperty("Referer", baseUrl+"/bbs/board.php?bo_table=manga&wr_id="+id);
                connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
                new DataOutputStream(connection.getOutputStream()).write(data);
                responseCode = connection.getResponseCode();
            }else if(url.getProtocol().equals("https")){
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Cookie",cookie);
                connection.setRequestProperty("Accept", "*");
                connection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty( "Content-Length", Integer.toString(data.length));
                connection.setRequestProperty("Referer", baseUrl+"/bbs/board.php?bo_table=manga&wr_id="+id);
                connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
                new DataOutputStream(connection.getOutputStream()).write(data);
                responseCode = connection.getResponseCode();
            }
            if(responseCode == 302)
                return true;
        }catch (Exception e){

        }
        return false;
    }
}

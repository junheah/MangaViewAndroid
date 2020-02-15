package ml.melun.mangaview;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;

import androidx.appcompat.app.AlertDialog;
import android.view.Display;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import ml.melun.mangaview.activity.EpisodeActivity;
import ml.melun.mangaview.activity.ViewerActivity;
import ml.melun.mangaview.activity.ViewerActivity2;
import ml.melun.mangaview.activity.ViewerActivity3;
import ml.melun.mangaview.mangaview.CustomHttpClient;
import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

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

//    public static String httpsGet(String urlin, String cookie){
//        BufferedReader reader = null;
//        try {
//            InputStream stream = null;
//            URL url = new URL(urlin);
//            if(url.getProtocol().equals("http")){
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.setRequestMethod("GET");
//                connection.setRequestProperty("Accept-Encoding", "*");
//                connection.setRequestProperty("Accept", "*");
//                connection.setRequestProperty("Cookie",cookie);
//                connection.connect();
//                stream = connection.getInputStream();
//            }else if(url.getProtocol().equals("https")){
//                HttpsURLConnection connections = (HttpsURLConnection) url.openConnection();
//                connections.setInstanceFollowRedirects(false);
//                connections.setRequestMethod("GET");
//                connections.setRequestProperty("Accept-Encoding", "*");
//                connections.setRequestProperty("Accept", "*");
//                connections.setRequestProperty("Cookie",cookie);
//                connections.connect();
//                stream = connections.getInputStream();
//            }
//            reader = new BufferedReader(new InputStreamReader(stream));
//            StringBuffer buffer = new StringBuffer();
//            String line = "";
//            while ((line = reader.readLine()) != null) {
//                buffer.append(line);
//            }
//            return buffer.toString();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (reader != null) {
//                    reader.close();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return null;
//    }
//
//    public static String httpsGet(String urlin){
//        return httpsGet(urlin, "");
//    }
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

    public static void showErrorPopup(Context context){
        AlertDialog.Builder builder;
        String title = "뷰어 오류";
        String content = "만화 정보를 불러오는데 실패하였습니다. 연결 상태를 확인하고 다시 시도해 주세요.";
        if (new Preference(context).getDarkTheme()) builder = new AlertDialog.Builder(context, R.style.darkDialog);
        else builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(content)
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
        sbuilder.append(e.getMessage()+"\n");
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

    public static Boolean writeComment(CustomHttpClient client, Login login, int id, String content, String baseUrl){
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Cookie", login.getCookie(true));
//            headers.put("Content-Type","application/x-www-form-urlencoded");
//            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
//            headers.put("Accept-Encoding", "gzip, deflate, br");
//            headers.put("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");


            Response tokenResponse = client.mget("/bbs/ajax.comment_token.php?_="+ System.currentTimeMillis());
            String token = new JSONObject(tokenResponse.body().string()).getString("token");
            tokenResponse.close();
//
//            String param = "token="+token
//                    +"&w=c&bo_table=manga&wr_id="+id
//                    +"&comment_id=&pim=&sca=&sfl=&stx=&spt=&page=&is_good=0&wr_content="+URLEncoder.encode(content, "UTF-8");
            RequestBody requestBody = new FormBody.Builder()
                    .addEncoded("token",token)
                    .addEncoded("w","c")
                    .addEncoded("bo_table","manga")
                    .addEncoded("wr_id",String.valueOf(id))
                    .addEncoded("comment_id","")
                    .addEncoded("pim","")
                    .addEncoded("sca","")
                    .addEncoded("sfl","")
                    .addEncoded("stx","")
                    .addEncoded("spt","")
                    .addEncoded("page","")
                    .addEncoded("is_good","0")
                    .addEncoded("wr_content",content)
                    .build();



            Response commentResponse = client.post(baseUrl + "/bbs/write_comment_update.php", requestBody, headers);
            int responseCode = commentResponse.code();
            commentResponse.close();
            if(responseCode == 302)
                return true;
        }catch (Exception e){

        }
        return false;
    }


    static public void checkCaptcha(Preference p, Context context, int id, Runnable callback){
            String url = p.getUrl() + "/bbs/board.php?bo_table=manga&wr_id=" + id;

        WebView webView = new WebView(context);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        CookieManager cookiem = CookieManager.getInstance();
        cookiem.removeAllCookie();

        webView.setWebViewClient(new WebViewClient() {
            boolean catchNextRequest = false;

            public boolean shouldOverrideUrlLoading(WebView view, String url){
                String cookies = CookieManager.getInstance().getCookie(url);
                if(catchNextRequest){
                    String cookieStr = cookiem.getCookie(url);
                    for(String s: cookieStr.split(";")){
                        if(s.contains("PHPSESSID=")){
                            String cookie = s.substring(s.indexOf("=")+1);
                            p.setSession(cookie);
                            callback.run();
                            break;
                        }
                    }
                } else if(url.contains("sign_captcha.php")) {
                    catchNextRequest = true;
                    view.loadUrl(url);
                }else
                    view.loadUrl(url);
                return false; // then it is not handled by default action
            }
        });

        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");

        Login login = p.getLogin();

        // if logged-in, mget session from login
        if(login != null && login.isValid()){
            cookiem.setCookie(p.getUrl(), login.getCookie(true));
        }else if(p.getSession().length()>0){
            // else, use session
            cookiem.setCookie(p.getUrl(), "PHPSESSID=" + p.getSession() + "; ");
        }
        webView.loadUrl(url, headers);
    }

    public static void hideSpinnerDropDown(Spinner spinner) {
        try {
            Method method = Spinner.class.getDeclaredMethod("onDetachedFromWindow");
            method.setAccessible(true);
            method.invoke(spinner);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

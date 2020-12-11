package ml.melun.mangaview;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ml.melun.mangaview.activity.CaptchaActivity;
import ml.melun.mangaview.activity.EpisodeActivity;
import ml.melun.mangaview.activity.LoginActivity;
import ml.melun.mangaview.activity.MainActivity;
import ml.melun.mangaview.activity.ViewerActivity;
import ml.melun.mangaview.activity.ViewerActivity2;
import ml.melun.mangaview.activity.ViewerActivity3;
import ml.melun.mangaview.mangaview.CustomHttpClient;
import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.MTitle;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static ml.melun.mangaview.activity.CaptchaActivity.REQUEST_CAPTCHA;
import static ml.melun.mangaview.activity.SettingsActivity.urlSettingPopup;

public class Utils {

    private static int captchaCount = 1;

    public static final String ReservedChars = "|\\?*<\":>+[]/'";

    public static boolean deleteRecursive(File fileOrDirectory) {
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

    public static void showYesNoPopup(Context context, String title, String content,
                                      DialogInterface.OnClickListener posClickListener,
                                      DialogInterface.OnClickListener negClickListener,
                                      DialogInterface.OnCancelListener cancelListener){

        AlertDialog.Builder builder;
        if (new Preference(context).getDarkTheme()) builder = new AlertDialog.Builder(context, R.style.darkDialog);
        else builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(content)
                .setPositiveButton("예", posClickListener)
                .setNegativeButton("아니오", negClickListener)
                .setOnCancelListener(cancelListener)
                .show();
    }

    public static void showYesNoNeutralPopup(Context context, String title, String content, String neutral,
                                             DialogInterface.OnClickListener posClickListener,
                                             DialogInterface.OnClickListener negClickListener,
                                             DialogInterface.OnClickListener neuClickListener,
                                             DialogInterface.OnCancelListener cancelListener){

        AlertDialog.Builder builder;
        if (new Preference(context).getDarkTheme()) builder = new AlertDialog.Builder(context, R.style.darkDialog);
        else builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(content)
                .setPositiveButton("예", posClickListener)
                .setNegativeButton("아니오", negClickListener)
                .setNeutralButton(neutral, neuClickListener)
                .setOnCancelListener(cancelListener)
                .show();
    }

    public static void showErrorPopup(Context context, String message, Exception e, boolean force_close){
        AlertDialog.Builder builder;
        String title = "오류";
        if (new Preference(context).getDarkTheme()) builder = new AlertDialog.Builder(context, R.style.darkDialog);
        else builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(force_close) ((Activity)context).finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        if(force_close) ((Activity)context).finish();
                    }
                });
        if(e != null) {
            builder.setNeutralButton("자세히", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showStackTrace(context, e);
                }
            });
        }
        builder.show();
    }

    public static boolean checkConnection(Context context){
        if(context != null) {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) ((Activity) context).getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        }else return false;
    }



    public static void showCaptchaPopup(Context context, int code, Exception e, boolean force_close, Fragment fragment, Preference p){
        if(context != null) {
            if (!checkConnection(context)) {
                //no internet
                //showErrorPopup(context, "네트워크 연결이 없습니다.", e, force_close);
                Toast.makeText(context, "네트워크 연결이 없습니다.", Toast.LENGTH_LONG).show();
                if (force_close) ((Activity) context).finish();
            } else if (captchaCount == 0) {
                startCaptchaActivity(context, code, fragment);
            } else {
                AlertDialog.Builder builder;
                String title = "오류";
                String content = "정보를 불러오는데 실패하였습니다.";
                if (new Preference(context).getDarkTheme())
                    builder = new AlertDialog.Builder(context, R.style.darkDialog);
                else builder = new AlertDialog.Builder(context);
                builder.setTitle(title)
                        .setMessage(content)
                        .setNeutralButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (force_close) ((Activity) context).finish();
                            }
                        })
                        .setPositiveButton("CAPTCHA 인증", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startCaptchaActivity(context, code, fragment);
                            }
                        })
                        .setNegativeButton("URL 설정", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                urlSettingPopup(context, p);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                if (force_close) ((Activity) context).finish();
                            }
                        });
                if (e != null) {
                    builder.setNeutralButton("자세히", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showStackTrace(context, e);
                        }
                    });
                }
                try {
                    builder.show();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
            captchaCount++;
        }
    }

    static void startCaptchaActivity(Context context, int code, Fragment fragment){
        Intent captchaIntent = new Intent(context, CaptchaActivity.class);
        if(fragment == null)
            ((Activity)context).startActivityForResult(captchaIntent, code);
        else
            fragment.startActivityForResult(captchaIntent, code);
    }

    public static void showCaptchaPopup(Context context, int code, Exception e, boolean force_close, Preference p) {
        showCaptchaPopup(context,code,e,force_close,null, p);
    }

    public static void showCaptchaPopup(Context context, Exception e, Preference p) {
        // viewer call
        showCaptchaPopup(context, REQUEST_CAPTCHA, e, true, p);
    }

    public static void showCaptchaPopup(Context context, int code, Preference p){
        // menu call
        showCaptchaPopup(context, code, null, false, p);
    }

    public static void showCaptchaPopup(Context context, int code, Fragment fragment, Preference p){
        // menu call
        showCaptchaPopup(context, code, null, false, fragment, p);
    }

    public static void showCaptchaPopup(Context context, Preference p){
        // viewer call
        showCaptchaPopup(context, 0, null, true, p);
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

    public static int getScreenWidth(Display display){
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }
    public static boolean writeComment(CustomHttpClient client, Login login, int id, String content, String baseUrl){
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

    public static void hideSpinnerDropDown(Spinner spinner) {
        try {
            Method method = Spinner.class.getDeclaredMethod("onDetachedFromWindow");
            method.setAccessible(true);
            method.invoke(spinner);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean writePreferenceToFile(Context c, File f){
        try {
            FileOutputStream stream = new FileOutputStream(f);
            stream.write(readPref(c).getBytes());
            stream.flush();
            stream.close();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void jsonToPref(Context c, CustomJSONObject data){
        SharedPreferences.Editor editor = c.getSharedPreferences("mangaView", Context.MODE_PRIVATE).edit();
        editor.putString("recent",data.getJSONArray("recent", new JSONArray()).toString());
        editor.putString("favorite",data.getJSONArray("favorite", new JSONArray()).toString());
        editor.putString("homeDir",data.getString("homeDir", "/sdcard/MangaView/saved"));
        editor.putBoolean("darkTheme",data.getBoolean("darkTheme",false));
        editor.putInt("prevPageKey", data.getInt("prevPageKey", -1));
        editor.putInt("nextPageKey", data.getInt("nextPageKey", -1));
        editor.putString("bookmark",data.getJSONObject("bookmark", new JSONObject()).toString());
        editor.putString("bookmark2",data.getJSONObject("bookmark2", new JSONObject()).toString());
        editor.putInt("viewerType",data.getInt("viewerType", 0));
        editor.putBoolean("pageReverse",data.getBoolean("pageReverse", false));
        editor.putBoolean("dataSave",data.getBoolean("dataSave", false));
        editor.putBoolean("stretch",data.getBoolean("stretch", false));
        editor.putInt("startTab",data.getInt("startTab", 0));
        editor.putString("url",data.getString("url", ""));
        editor.putString("defUrl",data.getString("defUrl", "설정되지 않음"));
        editor.putString("notice",data.getJSONArray("notice", new JSONArray()).toString());
        editor.putLong("lastUpdateTime", data.getLong("lastUpdateTime", 0));
        editor.putLong("lastNoticeTime", data.getLong("lastNoticeTime", 0));
        editor.putBoolean("leftRight", data.getBoolean("leftRight", false));
        editor.putBoolean("autoUrl", data.getBoolean("autoUrl", true));
        editor.putString("login", data.getJSONObject("login", new JSONObject()).toString());
        editor.putFloat("pageControlButtonOffset", (float)data.getDouble("pageControlButtonOffset", -1));
        editor.commit();
    }

    public static boolean readPreferenceFromFile(Preference p, Context c, File f){
        try {
            CustomJSONObject data = new CustomJSONObject(readFileToString(f));
            jsonToPref(c, data);
            p.init(c);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String readPref(Context context){
        SharedPreferences sharedPref = ((Activity)context).getSharedPreferences("mangaView", Context.MODE_PRIVATE);
        JSONObject data = new JSONObject();
        try {
            data.put("recent",new JSONArray(sharedPref.getString("recent", "[]")));
            data.put("favorite",new JSONArray(sharedPref.getString("favorite", "[]")));
            data.put("homeDir",sharedPref.getString("homeDir","/sdcard/MangaView/saved"));
            data.put("darkTheme",sharedPref.getBoolean("darkTheme", false));
            data.put("bookmark",new JSONObject(sharedPref.getString("bookmark", "{}")));
            data.put("bookmark2",new JSONObject(sharedPref.getString("bookmark2", "{}")));
            data.put("viewerType", sharedPref.getInt("viewerType",0));
            data.put("pageReverse",sharedPref.getBoolean("pageReverse",false));
            data.put("dataSave",sharedPref.getBoolean("dataSave", false));
            data.put("stretch",sharedPref.getBoolean("stretch", false));
            data.put("leftRight", sharedPref.getBoolean("leftRight", false));
            data.put("startTab",sharedPref.getInt("startTab", 0));
            data.put("url",sharedPref.getString("url", ""));
            data.put("defUrl",sharedPref.getString("url", "설정되지 않음"));
            data.put("notice",new JSONArray(sharedPref.getString("notice", "[]")));
            data.put("lastNoticeTime",sharedPref.getLong("lastNoticeTime",0));
            data.put("lastUpdateTime",sharedPref.getLong("lastUpdateTime",0));
            data.put("autoUrl", sharedPref.getBoolean("autoUrl", true));
            data.put("prevPageKey", sharedPref.getInt("prevPageKey", -1));
            data.put("nextPageKey", sharedPref.getInt("nextPageKey", -1));
            data.put("pageControlButtonOffset", sharedPref.getFloat("pageControlButtonOffset", -1));
        }catch(Exception e){
            e.printStackTrace();
        }
        return (prefFilter(data.toString()));
    }

    public static String prefFilter(String input){
        // keep newline and filter everything else
        return input.replace("\\n", "/n")
                .replace("\\","")
                .replace("/n", "\\n");
    }

    public static float dpToPixel(float dp, Context context){
        return dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static float pixelToDp(float px, Context context){
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static void openViewer(Context context, Manga manga, int code){
        Intent viewer = viewerIntent(context,manga);
        viewer.putExtra("online",true);
        ((Activity)context).startActivityForResult(viewer, code);
    }

    public static void popup(Context context, View view, final int position, final Title title, final int m, PopupMenu.OnMenuItemClickListener listener, Preference p) {
        PopupMenu popup = new PopupMenu(context, view);
        //Inflating the Popup using xml file
        //todo: clean this part
        popup.getMenuInflater().inflate(R.menu.title_options, popup.getMenu());
        switch (m) {
            case 1:
                //최근
                popup.getMenu().findItem(R.id.del).setVisible(true);
            case 0:
                //검색
                popup.getMenu().findItem(R.id.favAdd).setVisible(true);
                popup.getMenu().findItem(R.id.favDel).setVisible(true);
                break;
            case 2:
                //좋아요
                popup.getMenu().findItem(R.id.favDel).setVisible(true);
                break;
            case 3:
                //저장됨
                popup.getMenu().findItem(R.id.favAdd).setVisible(true);
                popup.getMenu().findItem(R.id.favDel).setVisible(true);
                popup.getMenu().findItem(R.id.remove).setVisible(true);
                break;
        }
        //좋아요 추가/제거 중 하나만 남김
        if (m != 2) {
            if (p.findFavorite(title) > -1) popup.getMenu().removeItem(R.id.favAdd);
            else popup.getMenu().removeItem(R.id.favDel);
        }
        popup.setOnMenuItemClickListener(listener);
        popup.show();
    }

    public final static int REQUEST_LOGIN = 232;

    public static void requestLogin(Context context, Preference p){
        //toast
        Toast.makeText(context, "로그인 하세요",  Toast.LENGTH_SHORT).show();
        //reset login
        p.setLogin(null);
        //open login activity
        ((Activity) context).startActivityForResult(new Intent(context, LoginActivity.class), REQUEST_LOGIN);
    }


    public static int getNumberFromString(String input){
        if(input.isEmpty()) return -1;
        for(int i = 0; i < input.length(); i++) {
            if(Character.digit(input.charAt(i),10) < 0){
                if(i>0)
                    return Integer.parseInt(input.substring(0,i));
                else
                    return -1;
            }
        }
        return -1;
    }



}

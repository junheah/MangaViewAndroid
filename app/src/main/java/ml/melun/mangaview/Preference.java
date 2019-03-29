package ml.melun.mangaview;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.Title;

public class Preference {
    static SharedPreferences sharedPref;
    static Context context;
    //static ArrayList<Title> recent;
    static ArrayList<Title> recent;
    static ArrayList<Title> favorite;
    static SharedPreferences.Editor prefsEditor;
    static JSONObject pagebookmark;
    static JSONObject bookmark;
    static String homeDir;
    static Boolean volumeControl;
    static Boolean darkTheme;
    static int viewerType;
    static Boolean reverse;
    static Boolean dataSave;
    static int startTab;
    static String url;
    static Boolean stretch;
    static Boolean leftRight;
    static Login login;

    //Offline manga has id of -1
    public Preference(Context context){
        if(this.context==null) init(context);
    }
    public void init(Context mcontext){
        sharedPref = mcontext.getSharedPreferences("mangaView",Context.MODE_PRIVATE);
        context = mcontext;
        prefsEditor = sharedPref.edit();
        try {
            Gson gson = new Gson();
            recent = gson.fromJson(sharedPref.getString("recent", ""),new TypeToken<ArrayList<Title>>(){}.getType());
            if(recent==null) recent = new ArrayList<>();
            favorite = gson.fromJson(sharedPref.getString("favorite", ""),new TypeToken<ArrayList<Title>>(){}.getType());
            if(favorite==null) favorite = new ArrayList<>();
            homeDir = sharedPref.getString("homeDir","/sdcard/MangaView/saved");
            volumeControl = sharedPref.getBoolean("volumeControl",false);
            //pagebookmark = {id:page}
            pagebookmark = new JSONObject(sharedPref.getString("bookmark", "{}"));
            bookmark = new JSONObject(sharedPref.getString("bookmark2", "{}"));
            darkTheme = sharedPref.getBoolean("darkTheme", false);
            viewerType = sharedPref.getInt("viewerType",0);
            reverse = sharedPref.getBoolean("pageReverse",false);
            dataSave = sharedPref.getBoolean("dataSave", false);
            startTab = sharedPref.getInt("startTab", 0);
            url = sharedPref.getString("url", "http://188.214.128.5");
            stretch = sharedPref.getBoolean("stretch", false);
            leftRight = sharedPref.getBoolean("leftRight", false);
            login = gson.fromJson(sharedPref.getString("login","{}"),new TypeToken<Login>(){}.getType());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static Boolean getLeftRight() {
        return leftRight;
    }

    public static void setLeftRight(Boolean leftRight) {
        Preference.leftRight = leftRight;
        prefsEditor.putBoolean("leftRight", leftRight);
        prefsEditor.commit();
    }

    public static int getViewerType() {
        return viewerType;
    }

    public static void setViewerType(int viewerType) {
        Preference.viewerType = viewerType;
        prefsEditor.putInt("viewerType", viewerType);
        prefsEditor.commit();
    }

    public static Boolean getStretch() {
        return stretch;
    }

    public static void setStretch(Boolean stretch) {
        Preference.stretch = stretch;
        prefsEditor.putBoolean("stretch", stretch);
        prefsEditor.commit();
    }

    public static String getUrl() {
        return url;
    }

    public static void setUrl(String url) {
        Preference.url = url;
        prefsEditor.putString("url", url);
        prefsEditor.commit();
    }

    public static int getStartTab() {
        return startTab;
    }

    public static void setStartTab(int startTab) {
        Preference.startTab = startTab;
        prefsEditor.putInt("startTab", startTab);
        prefsEditor.commit();
    }

    public static Boolean getDataSave() {
        return dataSave;
    }

    public static void setDataSave(Boolean dataSave) {
        Preference.dataSave = dataSave;
        prefsEditor.putBoolean("dataSave", dataSave);
        prefsEditor.commit();
    }

    public static Boolean getReverse() {
        return reverse;
    }

    public static void setReverse(Boolean reverse) {
        Preference.reverse = reverse;
        prefsEditor.putBoolean("pageReverse", reverse);
        prefsEditor.commit();
    }


    public static Boolean getDarkTheme() {
        return darkTheme;
    }

    public static void setDarkTheme(Boolean darkTheme) {
        Preference.darkTheme = darkTheme;
        prefsEditor.putBoolean("darkTheme", darkTheme);
        prefsEditor.commit();
    }

    public static Boolean getVolumeControl() {
        return volumeControl;
    }

    public static void setVolumeControl(Boolean volumeControl) {
        Preference.volumeControl = volumeControl;
        prefsEditor.putBoolean("volumeControl", volumeControl);
        prefsEditor.commit();
    }

    public static String getHomeDir() {
        return homeDir;
    }

    public static void setHomeDir(String homeDir) {
        Preference.homeDir = homeDir;
        prefsEditor.putString("homeDir", homeDir);
        prefsEditor.commit();
    }
    public void removeRecent(int position){
        recent.remove(position);
        writeRecent();
    }

    public void addRecent(Title tmp){
        //FUCK YOU JAVA FOR NOT IMPLEMENTING POINTERS
        Title title = new Title(tmp.getName(),tmp.getThumb(),tmp.getAuthor(),tmp.getTags(),tmp.getRelease());
        int position = getIndexOf(title);
        if(position>-1) {
            recent.add(0,recent.get(position));
            recent.remove(position+1);
        } else recent.add(0,title);
        writeRecent();
    }
    public void updateRecentData(Title title){
        recent.get(0).setName(title.getName());
        recent.get(0).setThumb(title.getThumb());
        recent.get(0).setAuthor(title.getAuthor());
        recent.get(0).setTags(title.getTags());
        writeRecent();
        int index = findFavorite(title);
        if(index>-1){
            favorite.get(index).setName(title.getName());
            favorite.get(index).setThumb(title.getThumb());
            favorite.get(index).setAuthor(title.getAuthor());
            favorite.get(index).setTags(title.getTags());
            Gson gson = new Gson();
            prefsEditor.putString("favorite", gson.toJson(favorite));
            prefsEditor.commit();
        }
    }

    private int getIndexOf(Title title){
        String targetT = title.getName();
        for(int i=0; i<recent.size(); i++){
            if(match(targetT,recent.get(i).getName())) return i;
        }
        return -1;
    }

    public void setBookmark(String title, int id){
        try {
            bookmark.put(title, id);
        } catch (Exception e) {
            //
        }
        writeBookmark();
    }
    public int getBookmark(String title){
        //return recent.get(0).getBookmark();
        try {
            return  bookmark.getInt(title);
        } catch (Exception e) {
            //
        }
        return -1;
    }
    public void writeBookmark(){
        prefsEditor.putString("bookmark2", bookmark.toString());
        prefsEditor.commit();
    }

    public void resetBookmark(){
        try {
            bookmark = new JSONObject("{}");
        }catch (Exception e){}
        writeBookmark();
    }
    public void resetRecent(){
        recent = new ArrayList<>();
        writeRecent();
    }

    private void writeRecent(){
        Gson gson = new Gson();
        prefsEditor.putString("recent", gson.toJson(recent));
        prefsEditor.commit();
    }


    public void setViewerBookmark(int id,int index){
        if(id>-1) {
            if (index > 0) {
                try {
                    pagebookmark.put(id + "", index);
                } catch (Exception e) {
                    //
                }
                writeViewerBookmark();
            }
        }
    }
    public int getViewerBookmark(int id){
        if(id>-1) {
            try {
                return pagebookmark.getInt(id + "");
            } catch (Exception e) {
                //
            }
        }
        return 0;
    }
    public void removeViewerBookmark(int id){
        pagebookmark.remove(id+"");
        writeViewerBookmark();
    }
    public void resetViewerBookmark(){
        try {
            pagebookmark = new JSONObject("{}");
        }catch (Exception e){}
        writeViewerBookmark();
    }
    private void writeViewerBookmark(){
        prefsEditor.putString("bookmark", pagebookmark.toString());
        prefsEditor.commit();
    }

    public Boolean toggleFavorite(Title tmp, int position){
        int index = findFavorite(tmp);
        if(index==-1){
            Title title = new Title(tmp.getName(),tmp.getThumb(),tmp.getAuthor(),tmp.getTags(),tmp.getRelease());
            favorite.add(position,title);
            Gson gson = new Gson();
            prefsEditor.putString("favorite", gson.toJson(favorite));
            prefsEditor.commit();
            return true;
        }else{
            favorite.remove(index);
            Gson gson = new Gson();
            prefsEditor.putString("favorite", gson.toJson(favorite));
            prefsEditor.commit();
            return false;
        }
    }
    public int findFavorite(Title title){
        for(int i=0; i<favorite.size();i++){
            if(match(title.getName(),favorite.get(i).getName())) return i;
        }
        return -1;
    }

    public ArrayList<Title> getFavorite(){
        return favorite;
    }

    public ArrayList<Title> getRecent(){
        return recent;
    }

    public Boolean isViewed(int id){
        return pagebookmark.has(id+"");
    }

    public Boolean match(String s1, String s2){
        return filterString(s1).matches(filterString(s2));
    }
    private String filterString(String input){
        int i=0, j=0, m=0, k=0;
        while(i>-1||j>-1||m>-1||k>-1){
            i = input.indexOf('(');
            j = input.indexOf(')');
            m = input.indexOf('/');
            k = input.indexOf('?');
            char[] tmp = input.toCharArray();
            if(i>-1) tmp[i] = ' ';
            if(j>-1) tmp[j] = ' ';
            if(m>-1) tmp[m] = ' ';
            if(k>-1) tmp[k] = ' ';
            input = String.valueOf(tmp);
        }
        return input;
    }

    //for debug
    public void removeEpsFromData(){
        for(Title t:recent){t.removeEps();}
        for(Title t:favorite){t.removeEps();}
        writeRecent();
        Gson gson = new Gson();
        prefsEditor.putString("favorite", gson.toJson(favorite));
        prefsEditor.commit();
    }

    public void setLogin(Login login){
        Preference.login =login;
        prefsEditor.putString("login", new Gson().toJson(login));
        prefsEditor.commit();
    }
    public Login getLogin(){
        return login;
    }
}
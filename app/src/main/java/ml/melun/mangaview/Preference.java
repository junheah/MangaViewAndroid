package ml.melun.mangaview;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

public class Preference {
    static SharedPreferences sharedPref;
    static Context context;
    //static ArrayList<Title> recent;
    static ArrayList<Title> recent;
    static ArrayList<Title> favorite;
    static SharedPreferences.Editor prefsEditor;
    static JSONObject pagebookmark;
    static String homeDir;
    static Boolean volumeControl;
    static Boolean darkTheme;
    static Boolean scrollViewer;
    static Boolean reverse;
    static Boolean dataSave;
    static int startTab;

    //Offline manga has id of -1
    public Preference(){
        //
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
            darkTheme = sharedPref.getBoolean("darkTheme", false);
            scrollViewer = sharedPref.getBoolean("scrollViewer",true);
            reverse = sharedPref.getBoolean("pageReverse",false);
            dataSave = sharedPref.getBoolean("dataSave", false);
            startTab = sharedPref.getInt("startTab", 0);
        }catch(Exception e){
            e.printStackTrace();
        }
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

    public static Boolean getScrollViewer() {
        return scrollViewer;
    }

    public static void setScrollViewer(Boolean scrollViewer) {
        Preference.scrollViewer = scrollViewer;
        prefsEditor.putBoolean("scrollViewer", scrollViewer);
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

    public void addRecent(Title title){
        int position = getIndexOf(title);
        if(position>-1) {
            recent.add(0,recent.get(position));
            recent.remove(position+1);
        } else recent.add(0,title);
        writeRecent();
    }
    public void updateRecentData(String name, String thumb, String author,List<String> tags){
        recent.get(0).setName(name);
        recent.get(0).setThumb(thumb);
        recent.get(0).setAuthor(author);
        recent.get(0).setTags(tags);
        writeRecent();
    }
    private int getIndexOf(Title title){
        String targetT = title.getName();
        for(int i=0; i<recent.size(); i++){
            if(match(targetT,recent.get(i).getName())) return i;
        }
        return -1;
    }

    public void setBookmark(int id){
        //always set bookmark at index 0 : to view episodes, title has to be added to index 0
        recent.get(0).setBookmark(id);
        writeRecent();
    }
    public int getBookmark(){
        return recent.get(0).getBookmark();
    }

    public void resetBookmark(){
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

    public Boolean toggleFavorite(Title title, int position){
        int index = findFavorite(title);
        if(index==-1){
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
    public String filterString(String instr){
        int i = instr.indexOf('(');
        int j = instr.indexOf(')');
        int m = instr.indexOf('?');
        if(i>-1||j>-1||m>-1){
            char[] tmp = instr.toCharArray();
            if(i>-1) tmp[i] = ' ';
            if(j>-1) tmp[j] = ' ';
            if(m>-1) tmp[m] = ' ';
            instr = String.valueOf(tmp);
        }
        return instr;
    }

}

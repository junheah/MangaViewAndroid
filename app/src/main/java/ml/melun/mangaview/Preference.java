package ml.melun.mangaview;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;

import ml.melun.mangaview.mangaview.Title;

public class Preference {
    static SharedPreferences sharedPref;
    static Context context;
    static ArrayList<Title> recent;
    static ArrayList<Title> favorite;
    static SharedPreferences.Editor prefsEditor;
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
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void addRecent(Title title){
        int position = getIndexOf(title);
        if(position>-1) recent.remove(position);
        recent.add(0,title);
        writeRecent();
    }
    private int getIndexOf(Title title){
        String targetT = title.getName();
        for(int i=0; i<recent.size(); i++){
            if(targetT.matches(recent.get(i).getName())) return i;
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
    public void setViewerBookmark(int index){
        recent.get(0).setPageBookmark(index);
        writeRecent();
    }
    public int getViewerBookmark(){
        return recent.get(0).getPageBookmark();
    }

    private void writeRecent(){
        Gson gson = new Gson();
        prefsEditor.putString("recent", gson.toJson(recent));
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
            if(title.getName().matches(favorite.get(i).getName())) return i;
        }
        return -1;
    }

    public ArrayList<Title> getFavorite(){
        return favorite;
    }

    public ArrayList<Title> getRecent(){
        return recent;
    }

}

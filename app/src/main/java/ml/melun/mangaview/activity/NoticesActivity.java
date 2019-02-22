package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;

import static ml.melun.mangaview.Utils.httpsGet;
import static ml.melun.mangaview.Utils.showPopup;

public class NoticesActivity extends AppCompatActivity {
    Boolean dark;
    Context context;
    JSONObject savedNotices;
    ArrayList<JSONObject> data;
    SharedPreferences sharedPref;
    ListView listView;
    SwipyRefreshLayout swipe;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(dark = new Preference(this).getDarkTheme())setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notices);
        ActionBar actionBar = getSupportActionBar();
        data = new ArrayList<>();
        sharedPref = this.getSharedPreferences("mangaView", Context.MODE_PRIVATE);
        context = this;
        swipe = this.findViewById(R.id.noticeSwipe);
        listView = this.findViewById(R.id.noticeList);
        if(actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true);
        try {
            savedNotices = new JSONObject(sharedPref.getString("notices", "{}"));
        }catch (Exception e){
            e.printStackTrace();
        }
        swipe.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                getNotices gn = new getNotices();
                gn.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        swipe.setRefreshing(true);
        getNotices gn = new getNotices();
        gn.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    void showNotice(String title, String content, String date){
        showPopup(context,title,date+"\n\n"+content);
    }

    void addNotices(JSONArray notices){
        //write new notices to SharedPref
        try {
            for (int i = 0; i < notices.length(); i++) {
                JSONObject notice = notices.getJSONObject(i);
                int id = notice.getInt("id");
               try{
                   savedNotices.getJSONObject(String.valueOf(id));
                   //existing notice

                }catch (Exception e){
                   //new notice
                   notice.remove("id");
                   savedNotices.put(String.valueOf(id), notice.toString());
                }
                sharedPref.edit().putString("notices", savedNotices.toString()).commit();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void populate(){
        //read notices from sharedPref
        String[] list = new String[0];
        data = new ArrayList<>();
        try{
            JSONObject notices = new JSONObject(sharedPref.getString("notices", "{}"));
            Iterator<String> keys= notices.keys();
            while (keys.hasNext())
            {
                String keyValue = keys.next();
                data.add(0, new JSONObject(notices.getString(keyValue)));
            }
            list = new String[data.size()];
            for(int i=0;i<data.size();i++){
                list[i] = data.get(i).getString("title");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, list
        );
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                JSONObject target = data.get(position);
                try {
                    showNotice(target.getString("title"),target.getString("content"),target.getString("date"));
                }catch (Exception e){e.printStackTrace();}
            }
        });
    }

    private class getNotices extends AsyncTask<Void, Void, Integer> {
        JSONArray loaded;
        protected void onPreExecute() {
            super.onPreExecute();
            sharedPref.edit().putLong("lastNoticeTime", System.currentTimeMillis()).commit();
        }
        protected Integer doInBackground(Void... params) {
            //get all notices
            try {
                String rawdata = httpsGet("https://github.com/junheah/MangaViewAndroid/raw/master/notices.json");
                loaded = new JSONArray(rawdata);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }
        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            swipe.setRefreshing(false);
            try {
                if (loaded.length() > 0) addNotices(loaded);
            }catch (Exception e){
                //probably offline
            }
            populate();
        }
    }
}

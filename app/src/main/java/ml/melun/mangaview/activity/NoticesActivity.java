package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;

import static ml.melun.mangaview.Utils.showPopup;

public class NoticesActivity extends AppCompatActivity {
    Boolean dark;
    Context context;
    ArrayList<JSONObject> data;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(dark = new Preference(this).getDarkTheme())setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notices);
        ActionBar actionBar = getSupportActionBar();
        data = new ArrayList<>();
        SharedPreferences sharedPref = this.getSharedPreferences("mangaView", Context.MODE_PRIVATE);
        String[] list = null;
        context = this;
        try{
            JSONObject notices = new JSONObject(sharedPref.getString("notices", "{}"));
            Iterator<String> keys= notices.keys();
            while (keys.hasNext())
            {
                String keyValue = (String) keys.next();
                data.add(notices.getJSONObject(keyValue));
            }

            list = new String[data.size()];
            for(int i=0;i<data.size();i++){
                list[i] = data.get(i).getString("title");
            }
        }catch (Exception e){

        }


        ListView listview = this.findViewById(R.id.noticeList);
        if(actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, list
        );
        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                JSONObject target = data.get(position);
                try {
                    showNotice(target.getString("title"),target.getString("content"),target.getString("date"));
                }catch (Exception e){e.printStackTrace();}
            }
        });
    }
    void showNotice(String title, String content, String date){
        showPopup(context,title,date+"\n\n"+content);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.Utils.showPopup;

public class DebugActivity extends AppCompatActivity {
    TextView output;
    Context context;
    ScrollView scroll;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        Button pref =this.findViewById(R.id.debug_pref);
        output = this.findViewById(R.id.debug_out);
        context = this;
        this.findViewById(R.id.debug_webTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, WebActivity.class));
            }
        });
        scroll = this.findViewById(R.id.debug_scroll);
        pref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                output.setText(pref());
            }
        });
        Button migrate = this.findViewById(R.id.debug_migrate);
        migrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Preference p = new Preference(context);
                List<Title> titles = p.getRecent();
                StringBuilder b = new StringBuilder();
                for(Title t : titles){
                    if(t.getBookmark()>0) p.setBookmark(t.getName(),t.getBookmark());
                    b.append("제목: "+t.getName() +" | 북마크: "+t.getBookmark() +'\n');
                }
                b.append("북마크 이전이 완료되었습니다.");
                output.setText(b.toString());
            }
        });
        Button clear = this.findViewById(R.id.debug_clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                output.setText(" ");
            }
        });
        final EditText editor = this.findViewById(R.id.debug_edit);
        Button editPref = this.findViewById(R.id.debug_pref_edit);
        final Button save = this.findViewById(R.id.debug_save);
        final Button cancel = this.findViewById(R.id.debug_cancel);
        editPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.setVisibility(View.VISIBLE);
                save.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.VISIBLE);
                editor.setText(pref());
                save.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //save changes
                        writeToPref(editor.getText());
                        new Preference(context).init(context);
                        editor.setVisibility(View.GONE);
                        save.setVisibility(View.GONE);
                        cancel.setVisibility(View.GONE);
                        editor.setText("");
                    }
                });
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //discard
                        editor.setVisibility(View.GONE);
                        save.setVisibility(View.GONE);
                        cancel.setVisibility(View.GONE);
                        editor.setText("");
                    }
                });
            }
        });

        Button removeEps = this.findViewById(R.id.debug_removeEps);
        removeEps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Preference(context).removeEpsFromData();
                output.setText("작업 완료.");
            }
        });

        this.findViewById(R.id.debug_loginTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, LoginActivity.class));
            }
        });

    }
    String pref(){
        SharedPreferences sharedPref = this.getSharedPreferences("mangaView", Context.MODE_PRIVATE);
        JSONObject data = new JSONObject();
        try {
            data.put("recent",new JSONArray(sharedPref.getString("recent", "")));
            data.put("favorite",new JSONArray(sharedPref.getString("favorite", "")));
            data.put("homeDir",sharedPref.getString("homeDir","/sdcard/MangaView/saved"));
            data.put("darkTheme",sharedPref.getBoolean("darkTheme", false));
            data.put("volumeControl",sharedPref.getBoolean("volumeControl",false));
            data.put("bookmark(viewer)",new JSONObject(sharedPref.getString("bookmark", "{}")));
            data.put("bookmark(episode)",new JSONObject(sharedPref.getString("bookmark2", "{}")));
            data.put("viewerType", sharedPref.getInt("viewerType",0));
            data.put("pageReverse",sharedPref.getBoolean("pageReverse",false));
            data.put("dataSave",sharedPref.getBoolean("dataSave", false));
            data.put("stretch",sharedPref.getBoolean("stretch", false));
            data.put("leftRight", sharedPref.getBoolean("leftRight", false));
            data.put("startTab",sharedPref.getInt("startTab", 0));
            data.put("url",sharedPref.getString("url", "http://188.214.128.5"));
            data.put("notice",new JSONArray(sharedPref.getString("notice", "[]")));
            data.put("lastNoticeTime",sharedPref.getLong("lastNoticeTime",0));
            data.put("lastUpdateTime",sharedPref.getLong("lastUpdateTime",0));
            data.put("login",new JSONObject(sharedPref.getString("login","{}")));
        }catch(Exception e){
            e.printStackTrace();
        }
        return (filter(data.toString()));
    }

    void writeToPref(Editable edit){
        try {
            SharedPreferences sharedPref = this.getSharedPreferences("mangaView", Context.MODE_PRIVATE);
            JSONObject data = new JSONObject(edit.toString());
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("recent",filter(data.getJSONArray("recent").toString()));
            editor.putString("favorite",filter(data.getJSONArray("favorite").toString()));
            editor.putString("homeDir",filter(data.getString("homeDir")));
            editor.putBoolean("darkTheme",data.getBoolean("darkTheme"));
            editor.putBoolean("volumeControl",data.getBoolean("volumeControl"));
            editor.putString("bookmark",filter(data.getJSONObject("bookmark(viewer)").toString()));
            editor.putString("bookmark2",filter(data.getJSONObject("bookmark(episode)").toString()));
            editor.putInt("viewerType",data.getInt("viewerType"));
            editor.putBoolean("pageReverse",data.getBoolean("pageReverse"));
            editor.putBoolean("dataSave",data.getBoolean("dataSave"));
            editor.putBoolean("stretch",data.getBoolean("stretch"));
            editor.putInt("startTab",data.getInt("startTab"));
            editor.putString("url",data.getString("url").toString());
            editor.putString("notice",filter(data.getJSONArray("notice").toString()));
            editor.putLong("lastUpdateTime", data.getLong("lastUpdateTime"));
            editor.putLong("lastNoticeTime", data.getLong("lastNoticeTime"));
            editor.putBoolean("leftRight", data.getBoolean("leftRight"));
            editor.putString("login", filter(data.getJSONObject("login").toString()));
            editor.commit();
        }catch (Exception e){
            showPopup(context,"오류",e.getMessage());
            e.printStackTrace();
        }
    }

    String filter(String input){return input.replaceAll("(?<!\\\\)\\\\(?!\\\\)", "");}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.debug_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.debug_up) {
            scroll.fullScroll(View.FOCUS_UP);
        }else if (id == R.id.debug_down){
            scroll.fullScroll(View.FOCUS_DOWN);
        }
        return super.onOptionsItemSelected(item);
    }
}

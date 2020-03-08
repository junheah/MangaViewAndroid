package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import net.jhavar.main.DdosGuardBypass;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

import ml.melun.mangaview.CustomJSONObject;
import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.mangaview.Cloudflare;
import okhttp3.Response;

import static ml.melun.mangaview.Utils.readPref;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FILE_SAVE;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FILE_SELECT;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FOLDER_SELECT;

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
                startActivity(new Intent(context, CaptchaActivity.class));
            }
        });
        scroll = this.findViewById(R.id.debug_scroll);
        pref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                output.setText(readPref(context));
            }
        });
        Button migrate = this.findViewById(R.id.debug_migrate);
        migrate.setEnabled(false);
//        migrate.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Preference p = new Preference(context);
//                List<Title> titles = p.getRecent();
//                StringBuilder b = new StringBuilder();
//                for(Title t : titles){
//                    if(t.getBookmark()>0) p.setBookmark(t,t.getBookmark());
//                    b.append("제목: "+t.getName() +" | 북마크: "+t.getBookmark() +'\n');
//                }
//                b.append("북마크 이전이 완료되었습니다.");
//                printLine(b.toString());
//            }
//        });
        Button clear = this.findViewById(R.id.debug_clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                output.setText("");
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
                editor.setText(readPref(context));
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

        this.findViewById(R.id.debug_eula).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, FirstTimeActivity.class));
            }
        });

        Button removeEps = this.findViewById(R.id.debug_removeEps);
        removeEps.setEnabled(false);
//        removeEps.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new Preference(context).removeEpsFromData();
//                printLine("작업 완료.");
//            }
//        });

        this.findViewById(R.id.debug_loginTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, LoginActivity.class));
            }
        });

        this.findViewById(R.id.ddgbTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ddgBypassTest().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });


        Button cfBtn = this.findViewById(R.id.debug_cf);
        cfBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                printLine("cf-scrape start");
                //유사하렘 1화
                //String fetchUrl = p.getUrl() + "/bbs/board.php?bo_table=manga&wr_id=1639778";
                String fetchUrl = p.getUrl();

                new AsyncTask<Void,Void,List<HttpCookie>>(){
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        cfBtn.setEnabled(false);
                    }

                    @Override
                    protected List<HttpCookie> doInBackground(Void... voids) {
                        Cloudflare cf = new Cloudflare(fetchUrl);
                        return cf.getCookies();
                    }

                    @Override
                    protected void onPostExecute(List<HttpCookie> res) {
                        if(res == null){
                            printLine("cf-scrape fail");
                        }else{
                            printLine("cf-scrape success");
                            for(HttpCookie item : res){
                                printLine(item.getName() + " " + item.getValue());
                            }
                        }
                        cfBtn.setEnabled(true);
                    }
                }.execute();

            }
        });
        cfBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return false;
            }
        });

    }

    void writeToPref(Editable edit){
        try {
            SharedPreferences sharedPref = this.getSharedPreferences("mangaView", Context.MODE_PRIVATE);
            CustomJSONObject data = new CustomJSONObject(edit.toString());
            Utils.jsonToPref(this, data);
            // reload preference
            p.init(this);
        }catch (Exception e){
            showPopup(context,"오류",e.getMessage());
            e.printStackTrace();
        }
    }

//    String filter(String input){return input.replaceAll("(?<!\\\\)\\\\(?!\\\\)", "");}

    private void printLine(String text){
        output.append(System.currentTimeMillis() + " : " +text + "\n");
    }

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

    private class ddgBypassTest extends AsyncTask<Void, Void, Integer> {
        String result = "";
        protected void onPreExecute() {

        }
        protected Integer doInBackground(Void... params) {
            try {
                Response r = httpClient.get("https://mnmnmnmnm.xyz/", new HashMap<>());
                if(r.code() == 403) {
                    DdosGuardBypass ddg = new DdosGuardBypass("https://mnmnmnmnm.xyz/");
                    ddg.bypass();
                    result = ddg.get("https://mnmnmnmnm.xyz/");
                }else
                    result = "no ddos guard";
            }catch (Exception e){
                StringBuilder sbuilder = new StringBuilder();
                sbuilder.append(e.getMessage()+"\n");
                for(StackTraceElement s : e.getStackTrace()){
                    sbuilder.append(s+"\n");
                }
                final String error = sbuilder.toString();
                result = error;
            }
            return null;
        }
        protected void onPostExecute(Integer r) {
            printLine(result);
        }
    }

}

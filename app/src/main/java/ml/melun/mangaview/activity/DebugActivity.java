package ml.melun.mangaview.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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
import ml.melun.mangaview.interfaces.IntegerCallback;
import ml.melun.mangaview.mangaview.Cloudflare;
import ml.melun.mangaview.mangaview.Manga;
import okhttp3.Response;

import static ml.melun.mangaview.Utils.readPref;
import static ml.melun.mangaview.Utils.showIntegerInputPopup;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.Utils.viewerIntent;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FILE_SAVE;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FILE_SELECT;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FOLDER_SELECT;
import static ml.melun.mangaview.mangaview.MTitle.base_auto;
import static ml.melun.mangaview.mangaview.MTitle.base_comic;
import static ml.melun.mangaview.mangaview.MTitle.base_webtoon;

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

        this.findViewById(R.id.debug_file_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(p.getHomeDir());
                output.append("current home dir : "+p.getHomeDir()+"\n\n");
                try {
                    DocumentFile t = DocumentFile.fromTreeUri(context, uri);
                    output.append("path : " + t.getUri() + "\ncan read : " + t.canRead() + "\ncan write : " + t.canWrite()+"\n");
                }catch (Exception e){
                    if(e.getMessage() != null)
                        output.append(e.getMessage()+"\n");
                }
            }
        });

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

        this.findViewById(R.id.debug_layoutEditor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, LayoutEditActivity.class));
            }
        });

        this.findViewById(R.id.debug_baseMode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(p.getBaseMode() == base_comic) {
                    p.setBaseMode(base_webtoon);
                    Toast.makeText(context, "webtoon", Toast.LENGTH_SHORT).show();
                }else {
                    p.setBaseMode(base_comic);
                    Toast.makeText(context, "comic", Toast.LENGTH_SHORT).show();
                }
            }
        });

        this.findViewById(R.id.debug_idInput).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show popup
                showIntegerInputPopup(context, "input manga id", new IntegerCallback() {
                    @Override
                    public void callback(int i) {
                        Intent viewer = viewerIntent(context, new Manga(i,"","",base_auto));
                        viewer.putExtra("online",true);
                        ((Activity)context).startActivity(viewer);
                    }
                },false);
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

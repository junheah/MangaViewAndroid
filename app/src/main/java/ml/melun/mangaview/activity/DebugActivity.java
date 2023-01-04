package ml.melun.mangaview.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

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

import static ml.melun.mangaview.MainApplication.p;

import ml.melun.mangaview.CustomJSONObject;
import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.interfaces.IntegerCallback;
import ml.melun.mangaview.mangaview.Manga;

import static ml.melun.mangaview.Utils.readPref;
import static ml.melun.mangaview.Utils.showIntegerInputPopup;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.Utils.viewerIntent;
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

        this.findViewById(R.id.debug_file_test).setOnClickListener(view -> {
            Uri uri = Uri.parse(p.getHomeDir());
            output.append("current home dir : "+p.getHomeDir()+"\n\n");
            try {
                DocumentFile t = DocumentFile.fromTreeUri(context, uri);
                output.append("path : " + t.getUri() + "\ncan read : " + t.canRead() + "\ncan write : " + t.canWrite()+"\n");
            }catch (Exception e){
                if(e.getMessage() != null)
                    output.append(e.getMessage()+"\n");
            }
        });

        this.findViewById(R.id.debug_webTest).setOnClickListener(v -> startActivity(new Intent(context, CaptchaActivity.class)));
        scroll = this.findViewById(R.id.debug_scroll);
        pref.setOnClickListener(v -> output.setText(readPref(context)));

        Button clear = this.findViewById(R.id.debug_clear);
        clear.setOnClickListener(v -> output.setText(""));
        final EditText editor = this.findViewById(R.id.debug_edit);
        Button editPref = this.findViewById(R.id.debug_pref_edit);
        final Button save = this.findViewById(R.id.debug_save);
        final Button cancel = this.findViewById(R.id.debug_cancel);
        editPref.setOnClickListener(v -> {
            editor.setVisibility(View.VISIBLE);
            save.setVisibility(View.VISIBLE);
            cancel.setVisibility(View.VISIBLE);
            editor.setText(readPref(context));
            save.setOnClickListener(v12 -> {
                //save changes
                writeToPref(editor.getText());
                new Preference(context).init(context);
                editor.setVisibility(View.GONE);
                save.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
                editor.setText("");
            });
            cancel.setOnClickListener(v1 -> {
                //discard
                editor.setVisibility(View.GONE);
                save.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
                editor.setText("");
            });
        });

        this.findViewById(R.id.debug_eula).setOnClickListener(view -> startActivity(new Intent(context, FirstTimeActivity.class)));



        this.findViewById(R.id.debug_loginTest).setOnClickListener(view -> startActivity(new Intent(context, LoginActivity.class)));

        this.findViewById(R.id.debug_layoutEditor).setOnClickListener(view -> startActivity(new Intent(context, LayoutEditActivity.class)));

        this.findViewById(R.id.debug_baseMode).setOnClickListener(view -> {
            if(p.getBaseMode() == base_comic) {
                p.setBaseMode(base_webtoon);
                Toast.makeText(context, "webtoon", Toast.LENGTH_SHORT).show();
            }else {
                p.setBaseMode(base_comic);
                Toast.makeText(context, "comic", Toast.LENGTH_SHORT).show();
            }
        });

        this.findViewById(R.id.debug_idInput).setOnClickListener(v -> {
            //show popup
            showIntegerInputPopup(context, "input manga id", i -> {
                Intent viewer = viewerIntent(context, new Manga(i, "", "", base_auto));
                viewer.putExtra("online", true);
                ((Activity) context).startActivity(viewer);
            },false);
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


}

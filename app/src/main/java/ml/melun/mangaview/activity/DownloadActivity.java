package ml.melun.mangaview.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;

import java.util.List;

import ml.melun.mangaview.Downloader;
import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.SelectEpisodeAdapter;
import ml.melun.mangaview.mangaview.Title;

public class DownloadActivity extends AppCompatActivity {
    Title title;
    SelectEpisodeAdapter adapter;
    RecyclerView eplist;
    Preference p;
    Boolean dark;
    JSONArray selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        eplist = this.findViewById(R.id.dl_eplist);
        Intent intent = getIntent();
        try {
            title = new Gson().fromJson(intent.getStringExtra("title"),new TypeToken<Title>(){}.getType());
            eplist.setLayoutManager(new LinearLayoutManager(this));
            adapter = new SelectEpisodeAdapter(getApplicationContext(),title.getEps());
            adapter.setClickListener(new SelectEpisodeAdapter.ItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    adapter.select(position);
                }
            });
            eplist.setAdapter(adapter);
        }catch (Exception e){
            e.printStackTrace();
        }
        Button dl = findViewById(R.id.dl_btn);
        dl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(adapter.getSelected(false).length()>0) {
                    selected = adapter.getSelected(false);
                    downloadClick();
                }else{
                    Toast.makeText(getApplication(),"1개 이상의 화를 선택해 주세요", Toast.LENGTH_SHORT).show();
                }
            }
        });
        Button dlAll = findViewById(R.id.dl_all_btn);
        dlAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selected = adapter.getSelected(true);
                downloadClick();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void downloadClick(){
        //download manga
        //ask for confirmation
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        //check if download service is up and running
                        Intent downloader = new Intent(getApplicationContext(),Downloader.class);
                        downloader.setAction(Downloader.ACTION_QUEUE);
                        downloader.putExtra("title", new Gson().toJson(title));
                        downloader.putExtra("selected", selected.toString());

                        if (Build.VERSION.SDK_INT >= 26) {
                            startForegroundService(downloader);
                        }else{
                            startService(downloader);
                        }
                        //queue title to service
                        Toast.makeText(getApplication(),"다운로드를 시작합니다.", Toast.LENGTH_LONG).show();
                        finish();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };
        AlertDialog.Builder builder;
        if(dark) builder = new AlertDialog.Builder(this,R.style.darkDialog);
        else builder = new AlertDialog.Builder(this);
        builder.setMessage(title.getName()+ " 을(를) 다운로드 하시겠습니까?\n[ 총 "+selected.length()+"화 ]").setPositiveButton("네", dialogClickListener)
                .setNegativeButton("아니오", dialogClickListener).show();
    }
}

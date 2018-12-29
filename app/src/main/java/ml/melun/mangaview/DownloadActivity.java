package ml.melun.mangaview;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.adapter.SelectEpisodeAdapter;
import ml.melun.mangaview.mangaview.Title;

public class DownloadActivity extends AppCompatActivity {
    Title title;
    Downloader downloader;
    JSONArray episodes;
    SelectEpisodeAdapter adapter;
    RecyclerView eplist;
    Preference p;
    Boolean dark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference();
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        eplist = this.findViewById(R.id.dl_eplist);
        Intent intent = getIntent();
        downloader = new Downloader();
        try {
            title = new Title(intent.getStringExtra("name"), "","",new ArrayList<String>());
            episodes = new JSONArray(intent.getStringExtra("list"));
            eplist.setLayoutManager(new LinearLayoutManager(this));
            adapter = new SelectEpisodeAdapter(getApplicationContext(),episodes);
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
                if(adapter.getSelected().length()>0) {
                    title.setEps(adapter.getSelected());
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
                title.setEps(episodes);
                downloadClick();
            }
        });
    }


    private void downloadClick(){
        //download manga
        System.out.println("download clicked");
        //ask for confirmation
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        downloader.queueTitle(title);
                        Toast.makeText(getApplication(),"다운로드를 시작합니다. 진행률은 저장된 만화 탭에서 확인 가능합니다.", Toast.LENGTH_LONG).show();
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
        builder.setMessage(title.getName()+ " 을(를) 다운로드 하시겠습니까?\n[ 총 "+title.getEpsCount()+"화 ]").setPositiveButton("네", dialogClickListener)
                .setNegativeButton("아니오", dialogClickListener).show();
    }
}

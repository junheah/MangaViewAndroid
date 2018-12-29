package ml.melun.mangaview;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import ml.melun.mangaview.adapter.OfflineTitleApapter;
import ml.melun.mangaview.mangaview.Manga;

public class OfflineEpisodeActivity extends AppCompatActivity {
    String title;
    String homeDirStr;
    RecyclerView offEpsList;
    ArrayList<String> episodes;
    Context context;
    File[] episodeFiles;
    Preference p;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference();
        if(p.getDarkTheme()) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_episode);

        context = this;
        Intent i = getIntent();
        episodes = new ArrayList<>();
        title = i.getStringExtra("title");
        homeDirStr = i.getStringExtra("homeDir");
        offEpsList = findViewById(R.id.offEpsiodeList);
        System.out.println(homeDirStr+'/'+title+'/');
        episodeFiles = new File(homeDirStr+'/'+title).listFiles();
        Arrays.sort(episodeFiles);
        for(File f:episodeFiles){
            if(f.isDirectory()) {
                System.out.println(f.getAbsolutePath());
                episodes.add(f.getName());
            }
        }
        final OfflineTitleApapter adapter = new OfflineTitleApapter(context,episodes);
        offEpsList.setLayoutManager(new LinearLayoutManager(this));
        offEpsList.setAdapter(adapter);
        adapter.setClickListener(new OfflineTitleApapter.ItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                //create manga object, add local img list and pass to viewer
                String ep = adapter.getItem(position);
                File[] imgs = episodeFiles[position].listFiles();
                Arrays.sort(imgs);
                String[] imgPaths = new String[imgs.length];
                for(int i=0; i<imgs.length;i++){
                    imgPaths[i] = imgs[i].getAbsolutePath();
                }
                Intent viewer = new Intent(context, ViewerActivity.class);
                viewer.putExtra("name",ep);
                viewer.putExtra("id",-1);
                viewer.putExtra("localImgs",imgPaths);
                startActivity(viewer);
            }
        });
    }


}

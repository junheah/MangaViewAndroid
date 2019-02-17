package ml.melun.mangaview;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
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
    Intent viewer;
    ActionBar ab;
    int[] ids;
    Boolean idList =false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
        if(p.getDarkTheme()) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_episode);

        context = this;
        Intent i = getIntent();
        episodes = new ArrayList<>();
        title = i.getStringExtra("title");
        homeDirStr = i.getStringExtra("homeDir");
        offEpsList = findViewById(R.id.offEpsiodeList);
        ab = getSupportActionBar();
        File idsrc = new File(homeDirStr+'/'+title+"/id.list");
        if(idsrc.exists()){
            //id list exists
            idList = true;
            //read file to string
            StringBuilder idtmp = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(idsrc));
                String line;
                while ((line = br.readLine()) != null) {
                    idtmp.append(line);
                }
                br.close();
            }
            catch (Exception e) {
            }
            //save as string
            String idStr[] = idtmp.toString().split(",");
            // String to int
            ids = new int[idStr.length];
            for(int s=0; s<idStr.length; s++){
                if(s==0) ids[s] = 0;
                else ids[s] = Integer.parseInt(idStr[s]);
            }
        }
        episodeFiles = new File(homeDirStr+'/'+title).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        Arrays.sort(episodeFiles);
        for(File f:episodeFiles){
            episodes.add(f.getName());
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
                switch (p.getViewerType()){
                    case 0:
                    case 2:
                        viewer = new Intent(context, ViewerActivity.class);
                        break;
                    case 1:
                        viewer = new Intent(context, ViewerActivity2.class);
                        break;
                }
                viewer.putExtra("name",ep);
                viewer.putExtra("online", false);
                if(idList){
                    try {
                        viewer.putExtra("id", ids[Integer.parseInt(ep.split("\\.")[0])]);
                    }catch (Exception e) {viewer.putExtra("id", -1);}
                }else {
                    viewer.putExtra("id", -1);
                }
                viewer.putExtra("localImgs",imgPaths);
                startActivity(viewer);
            }
        });
        ab.setTitle(title);
        ab.setDisplayHomeAsUpEnabled(true);
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

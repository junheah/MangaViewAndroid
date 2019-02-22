package ml.melun.mangaview.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.EpisodeAdapter;
import ml.melun.mangaview.adapter.TagAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.Utils.filterFolder;


public class EpisodeActivity extends AppCompatActivity {
    //global variables
    ProgressDialog pd;
    Title title;
    EpisodeAdapter episodeAdapter;
    Context context = this;
    RecyclerView episodeList;
    Preference p;
    Boolean favoriteResult = false;
    Boolean recentResult = false;
    int position;
    int bookmarkId = -1;
    int bookmarkIndex = -1;
    FloatingActionButton upBtn;
    Boolean upBtnVisible = false;
    List<Manga> episodes;
    Boolean dark, online=true;
    Intent viewer;
    ActionBar actionBar;
    String homeDir;
    File[] offlineEpisodes;
    int mode = 0;
    /*
    mode:
    0 = online
    1 = offline - old
    2 = offline - old (title.data)
    3 = offline - new (title.gson)
     */

    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode== RESULT_OK && requestCode==0){
            int newid = data.getIntExtra("id", -1);
            if(newid>0 && newid!=bookmarkId){
                bookmarkId = newid;
                //find index of bookmark;
                for(int i=0; i< episodes.size(); i++){
                    if(episodes.get(i).getId()==bookmarkId){
                        bookmarkIndex = i+1;
                        episodeAdapter.setBookmark(bookmarkIndex);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode);
        Intent intent = getIntent();
        upBtn = (FloatingActionButton) findViewById(R.id.upBtn);
        title = new Gson().fromJson(intent.getStringExtra("title"),new TypeToken<Title>(){}.getType());
        online = intent.getBooleanExtra("online", true);
        bookmarkId = p.getBookmark(title.getName());
        position = intent.getIntExtra("position",0);
        favoriteResult = intent.getBooleanExtra("favorite",false);
        recentResult = intent.getBooleanExtra("recent",false);
        episodeList = this.findViewById(R.id.EpisodeList);
        episodeList.setLayoutManager(new LinearLayoutManager(this));
        homeDir = p.getHomeDir();
        ((SimpleItemAnimator) episodeList.getItemAnimator()).setSupportsChangeAnimations(false);
        if(recentResult){
            Intent resultIntent = new Intent();
            setResult(RESULT_OK,resultIntent);
        }
        actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.setTitle(title.getName());
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if(online) {
            mode = 0;
            getEpisodes g = new getEpisodes();
            g.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else{
            //offline title
            episodes = new ArrayList<>();
            offlineEpisodes = getOfflineEpisodes();
            //read ids and folder names
            File titleDir = new File(homeDir,filterFolder(title.getName()));
            File oldData = new File(titleDir,"title.data");
            File data = new File(titleDir,"title.gson");
            if(oldData.exists()){
                mode = 2;
                p.addRecent(title);
                //read file to string
                StringBuilder raw = new StringBuilder();
                try {
                    BufferedReader br = new BufferedReader(new FileReader(oldData));
                    String line;
                    while ((line = br.readLine()) != null) {
                        raw.append(line);
                    }
                    br.close();
                    JSONObject json = new JSONObject(raw.toString());
                    JSONArray ids = json.getJSONArray("ids");
                    for(int i=0; i<offlineEpisodes.length;i++){
                        Manga manga;
                        String episodeName = offlineEpisodes[i].getName();
                        try {
                            //real index starts from [ 0001 ]
                            int realIndex = Integer.parseInt(episodeName.split("\\.")[0])-1;
                            int id = ids.getInt(realIndex);
                            manga = new Manga(id, episodeName, String.valueOf(id));
                        }catch(Exception e){
                            manga = new Manga(-1,episodeName, "");
                        }
                        episodes.add(manga);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }else if(data.exists()){
                mode = 3;
                //new reader
                p.addRecent(title);
                episodes = title.getEps();
                for(int i=episodes.size()-1; i>=0; i--){
                    //mangas are saved as id
                    Manga m = episodes.get(i);
                    File dir = new File(titleDir,String.valueOf(m.getId()));
                    if(!dir.exists() || new File(dir,"downloading").exists()) episodes.remove(i);
                }
            } else {
                mode = 1;
                for (File f : offlineEpisodes) {
                    Manga manga;
                    manga = new Manga(-1, f.getName(), "");
                    //add local images to manga
                    episodes.add(manga);
                }
            }
            //set up adapter
            episodeAdapter = new EpisodeAdapter(context, episodes, title, online);
            afterLoad();
        }
    }

    public File[] getOfflineEpisodes(){
        File[] episodeFiles = new File(homeDir, filterFolder(title.getName())).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        //sort
        Arrays.sort(episodeFiles);
        //add as manga
        return episodeFiles;
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if(requestCode==0){
//            if(bookmarkId != p.getBookmark()){
//                bookmarkId = p.getBookmark();
//                episodeAdapter.setBookmark(bookmarkId);
//            }
//        }
//    }

    public void afterLoad(){
        //find bookmark
        if(bookmarkId>-1){
            for(int i=0; i< episodes.size(); i++){
                if(episodes.get(i).getId()==bookmarkId){
                    bookmarkIndex=i+1;
                    episodeAdapter.setBookmark(bookmarkIndex);
                    break;
                }
            }
        }
        episodeAdapter.setFavorite(p.findFavorite(title)>-1);
        episodeList.setAdapter(episodeAdapter);
        if(bookmarkIndex>8){
            episodeList.scrollToPosition(bookmarkIndex);
            upBtn.setAlpha(1.0f);
            upBtnVisible = true;
        }else{
            upBtn.setAlpha(0.0f);
            upBtnVisible = false;
        }
        episodeList.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstVisible = ((LinearLayoutManager) episodeList.getLayoutManager()).findFirstVisibleItemPosition();
                if(firstVisible>0 && !upBtnVisible){
                    upBtn.animate().translationX(0);
                    upBtn.animate().alpha(1.0f);
                    upBtnVisible = true;
                } else if(firstVisible==0 && upBtnVisible){
                    upBtn.animate().alpha(0.0f);
                    upBtn.animate().translationX(upBtn.getWidth());
                    upBtnVisible = false;
                }
            }
        });
        upBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(upBtnVisible) {
                    episodeList.scrollToPosition(0);
                    upBtn.animate().alpha(0.0f);
                    upBtn.animate().translationX(upBtn.getWidth());
                    upBtnVisible = false;
                }
            }
        });
        episodeAdapter.setClickListener(new EpisodeAdapter.ItemClickListener() {
            @Override
            public void onItemClick(int position, Manga selected) {
                //add local images to manga
                if(!online) {
                    List<String> localImgs = new ArrayList<>();
                    File[] imgs = new File[0];
                    switch(mode){
                        case 1:
                        case 2:
                            imgs = offlineEpisodes[position].listFiles();
                            break;
                        case 3:
                            File titleDir = new File(homeDir,title.getName());
                            imgs = new File(titleDir, String.valueOf(selected.getId())).listFiles();
                            break;
                    }
                    Arrays.sort(imgs);
                    for (File img : imgs) {
                        localImgs.add(img.getAbsolutePath());
                    }
                    selected.setImgs(localImgs);
                }
                openViewer(selected,0);
            }
            @Override
            public void onStarClick(){
                //star click handler
                episodeAdapter.setFavorite(p.toggleFavorite(title, position));
                if(favoriteResult){
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("favorite", p.findFavorite(title)>-1);
                    setResult(RESULT_OK, resultIntent);
                }
            }

            @Override
            public void onAuthorClick() {
                if(title.getAuthor().length()>0){
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("query",title.getAuthor());
                    i.putExtra("mode",1);
                    startActivity(i);
                }
            }

            @Override
            public void onDownloadClick(){
                //start download activity
                Intent download = new Intent(context, DownloadActivity.class);
                download.putExtra("title", new Gson().toJson(title));
                startActivity(download);
            }
        });
        episodeAdapter.setTagClickListener(new TagAdapter.tagOnclick() {
            @Override
            public void onClick(String tag) {
                Intent i = new Intent(context, TagSearchActivity.class);
                i.putExtra("query",tag);
                i.putExtra("mode",2);
                startActivity(i);
            }
        });
    }

    private class getEpisodes extends AsyncTask<Void,Void,Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            if(dark) pd = new ProgressDialog(EpisodeActivity.this, R.style.darkDialog);
            else pd = new ProgressDialog(EpisodeActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
        }

        protected Integer doInBackground(Void... params) {
            title.fetchEps(p.getUrl());
            episodes = title.getEps();
            episodeAdapter = new EpisodeAdapter(context, episodes, title, online);
            return null;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            afterLoad();
            p.addRecent(title);
            p.updateRecentData(title);
            if (pd.isShowing()) {
                pd.dismiss();
            }
        }
    }

    public void openViewer(Manga manga, int code){
        Intent viewer = null;
        switch (p.getViewerType()){
            case 0:
            case 2:
                viewer = new Intent(context, ViewerActivity.class);
                break;
            case 1:
                viewer = new Intent(context, ViewerActivity2.class);
                break;
        }
        viewer.putExtra("manga", new Gson().toJson(manga));
        viewer.putExtra("title", new Gson().toJson(title));
        viewer.putExtra("recent",true);
        viewer.putExtra("online",online);
        startActivityForResult(viewer, code);
    }
}

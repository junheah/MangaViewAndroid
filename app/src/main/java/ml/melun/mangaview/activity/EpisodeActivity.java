package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.EpisodeAdapter;
import ml.melun.mangaview.adapter.TagAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.CODE_SCOPED_STORAGE;
import static ml.melun.mangaview.Utils.getOfflineEpisodes;
import static ml.melun.mangaview.Utils.requestLogin;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.Utils.showTokiCaptchaPopup;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;
import static ml.melun.mangaview.mangaview.Title.LOAD_CAPTCHA;


public class EpisodeActivity extends AppCompatActivity {
    //global variables
    Title title;
    EpisodeAdapter episodeAdapter;
    Context context = this;
    RecyclerView episodeList;
    boolean favoriteResult = false;
    boolean recentResult = false;
    int position;
    int bookmarkId = -1;
    int bookmarkIndex = -1;
    List<Manga> episodes;
    boolean dark, online=true;
    Intent viewer;
    ActionBar actionBar;
    String homeDir;
    int mode = 0;
    FloatingActionButton resumefab;
    ProgressBar progress;
    boolean loaded = false;
    LinearLayoutCompat fab_container;


    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.episode_download:
                Intent download = new Intent(context, DownloadActivity.class);
                download.putExtra("title", new Gson().toJson(title));
                startActivity(download);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode== RESULT_OK){
            int newid = data.getIntExtra("id", -1);
            if(newid>0 && newid!=bookmarkId){
                bookmarkId = newid;
                //find index of bookmark;
                if(episodes != null)
                    for(int i=0; i< episodes.size(); i++){
                        if(episodes.get(i).getId()==bookmarkId){
                            bookmarkIndex = i+1;
                            episodeAdapter.setBookmark(bookmarkIndex);
                            break;
                        }
                    }
            }
            if(bookmarkId>-1)
                resumefab.show();
            else
                resumefab.hide();
        }else if(resultCode == RESULT_CAPTCHA){
            //captcha Checked
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDarkNoTitle);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode);
        Intent intent = getIntent();
        title = new Gson().fromJson(intent.getStringExtra("title"),new TypeToken<Title>(){}.getType());
        online = intent.getBooleanExtra("online", true);
        if(title.useBookmark())
            bookmarkId = p.getBookmark(title);
        position = intent.getIntExtra("position",0);
        favoriteResult = intent.getBooleanExtra("favorite",false);
        recentResult = intent.getBooleanExtra("recent",false);
        episodeList = this.findViewById(R.id.EpisodeList);
        progress = this.findViewById(R.id.progress);
        episodeList.setLayoutManager(new NpaLinearLayoutManager(this));
        homeDir = p.getHomeDir();
        resumefab = this.findViewById(R.id.resumefab);
        fab_container = findViewById(R.id.fab_container);

        ((SimpleItemAnimator) episodeList.getItemAnimator()).setSupportsChangeAnimations(false);
        if(recentResult){
            Intent resultIntent = new Intent();
            setResult(RESULT_OK,resultIntent);
        }


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.setTitle(title.getName());
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if(online) {
            mode = 0;
            fab_container.setVisibility(View.GONE);
            getEpisodes g = new getEpisodes();
            g.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else{
            //offline title
            //initialize eps list
            episodes = new ArrayList<>();

            //get child folder list of title dir
            if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE) {
                //scoped storage
                DocumentFile titleDir = DocumentFile.fromTreeUri(context, Uri.parse(title.getPath()));
                DocumentFile data = titleDir.findFile("title.gson");
                if(data!=null){
                    mode = 3;
                    if (!title.useBookmark()) {
                        // is migrated
                        mode = 4;
                    } else {
                        p.addRecent(title);
                    }

                    episodes = title.getEps();
                    for(DocumentFile f : getOfflineEpisodes(titleDir)){
                        String name = f.getName();
                        try {
                            int index = episodes.indexOf(new Manga(Integer.parseInt(name.substring(name.lastIndexOf('.') + 1)), "", "", title.getBaseMode()));
                            if (index > -1) {
                                episodes.get(index).setOfflinePath(f.getUri().toString());
                                episodes.get(index).setMode(mode);
                            }
                        } catch (Exception e) {
                            // folder name is not properly formatted
                        }
                    }
                    //for loop to remove non-existing episodes
                    if (episodes != null)
                        for (int i = episodes.size() - 1; i >= 0; i--) {
                            if (episodes.get(i).getOfflinePath() == null) episodes.remove(i);
                        }

                }else{
                    mode = 1;
                    for(DocumentFile f : getOfflineEpisodes(titleDir)){
                        Manga m = new Manga(-1, f.getName(), "", title.getBaseMode());
                        m.setMode(mode);
                        m.setOfflinePath(f.toString());
                    }
                }
            }else {

                //read ids and folder names
                File titleDir = new File(title.getPath());
                File data = new File(titleDir, "title.gson");
                if (data.exists()) {
                    mode = 3;

                    if (!title.useBookmark()) {
                        // is migrated
                        mode = 4;
                    } else {
                        p.addRecent(title);
                    }

                    episodes = title.getEps();
                    for (File folder : getOfflineEpisodes(title.getPath())) {
                        //get id from listContent
                        String name = folder.getName();
                        try {
                            int index = episodes.indexOf(new Manga(Integer.parseInt(name.substring(name.lastIndexOf('.') + 1)), "", "", title.getBaseMode()));
                            if (index > -1) {
                                episodes.get(index).setOfflinePath(folder.getAbsolutePath());
                                episodes.get(index).setMode(mode);
                            }
                        } catch (Exception e) {
                            // folder name is not properly formatted
                        }
                    }
                    //for loop to remove non-existing episodes
                    if (episodes != null)
                        for (int i = episodes.size() - 1; i >= 0; i--) {
                            if (episodes.get(i).getOfflinePath() == null) episodes.remove(i);
                        }

                } else {
                    mode = 1;
                    for (File f : getOfflineEpisodes(title.getPath())) {
                        Manga manga;
                        manga = new Manga(-1, f.getName(), "", title.getBaseMode());
                        manga.setMode(mode);
                        manga.setOfflinePath(f.getAbsolutePath());
                        //add local images to manga
                        episodes.add(manga);
                        // set eps to title object
                        title.setEps(episodes);
                    }
                }
            }
            //set up adapter
            episodeAdapter = new EpisodeAdapter(context, episodes, title, mode);
            afterLoad();
        }
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
        actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.setTitle(title.getName());
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if(bookmarkId>-1){
            if(episodes != null)
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
        if(bookmarkIndex>8) {
            episodeList.scrollToPosition(bookmarkIndex);
        }
        findViewById(R.id.upfab).setOnClickListener(v -> episodeList.scrollToPosition(0));
        findViewById(R.id.downfab).setOnClickListener(v -> {
            episodeList.scrollToPosition(episodes.size()); //헤더가 0이기 때문
        });
        if(bookmarkIndex>-1)
            resumefab.show();
        else
            resumefab.hide();
        resumefab.setOnClickListener(v -> openViewer(episodes.get(bookmarkIndex-1),0));

        episodeAdapter.setClickListener(new EpisodeAdapter.ItemClickListener() {

            @Override
            public void onBookmarkClick() {
                if(mode == 0 && p.getLogin() != null && p.getLogin().isValid()) {
                    new ToggleBookmark().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }else {
                    Toast.makeText(context, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onItemClick(int position, Manga selected) {
                //add local images to manga
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
            public void onFirstClick(){
                if(episodes != null && episodes.size()>0)
                    openViewer(episodes.get(episodes.size()-1),0);
            }
        });
        episodeAdapter.setTagClickListener(tag -> {
            Intent i = new Intent(context, TagSearchActivity.class);
            i.putExtra("query",tag);
            i.putExtra("mode",2);
            startActivity(i);
        });
    }

    private class ToggleBookmark extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return title.toggleBookmark(httpClient, p);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            episodeAdapter.toggleBookmark(success);
            if(!success){
                requestLogin(context, p);
            }
        }
    }

    private class getEpisodes extends AsyncTask<Void,Void,Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            progress.setVisibility(View.VISIBLE);
        }

        protected Integer doInBackground(Void... params) {
            int code = title.fetchEps(httpClient);
            episodes = title.getEps();
            episodeAdapter = new EpisodeAdapter(context, episodes, title, mode);
            return code;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            if(res == LOAD_CAPTCHA){
                //캡차 처리 팝업
                showTokiCaptchaPopup(context, p);
                return;
            }else if(episodes == null || episodes.size()==0){
                showCaptchaPopup(title.getUrl(), context, p);
                return;
            }else {
                afterLoad();
                p.addRecent(title);
                p.updateRecentData(title);
                progress.setVisibility(View.GONE);
                loaded = true;
                fab_container.setVisibility(View.VISIBLE);
                invalidateOptionsMenu();
            }
        }
    }

    public void openViewer(Manga manga, int code){
        manga.setMode(mode);
        Intent viewer = null;
        switch (p.getViewerType()){
            case 0:
                viewer = new Intent(context, ViewerActivity.class);
                break;
            case 2:
                viewer = new Intent(context, ViewerActivity3.class);
                break;
            case 1:
                viewer = new Intent(context, ViewerActivity2.class);
                break;
        }
        viewer.putExtra("manga", new Gson().toJson(manga));
        viewer.putExtra("title", new Gson().toJson(title));
        viewer.putExtra("recent",true);
        startActivityForResult(viewer, code);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        if(loaded)
            inflater.inflate(R.menu.episode_menu, menu);
        return true;
    }


}

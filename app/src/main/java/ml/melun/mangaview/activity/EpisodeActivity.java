package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

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

import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.EpisodeAdapter;
import ml.melun.mangaview.adapter.TagAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.filterFolder;
import static ml.melun.mangaview.Utils.requestLogin;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;


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
    FloatingActionButton upBtn;
    boolean upBtnVisible = false;
    List<Manga> episodes;
    boolean dark, online=true;
    Intent viewer;
    ActionBar actionBar;
    String homeDir;
    List<File> offlineEpisodes;
    int mode = 0;
    ProgressBar progress;


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
        if(resultCode== RESULT_OK){
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
        }else if(resultCode == RESULT_CAPTCHA){
            //captcha Checked
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode);
        Intent intent = getIntent();
        upBtn = (FloatingActionButton) findViewById(R.id.upBtn);
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
            //initialize eps list
            episodes = new ArrayList<>();

            //get child folder list of title dir
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
                    for(int i=0; i<offlineEpisodes.size();i++){
                        Manga manga;
                        String episodeName = offlineEpisodes.get(i).getName();
                        try {
                            //real index starts from [ 0001 ]
                            int realIndex = Integer.parseInt(episodeName.split("\\.")[0])-1;
                            int id = ids.getInt(realIndex);
                            manga = new Manga(id, episodeName, String.valueOf(id), title.getBaseMode());
                        }catch(Exception e){
                            manga = new Manga(-1,episodeName, "", title.getBaseMode());
                        }
                        manga.setMode(mode);
                        manga.setOfflinePath(offlineEpisodes.get(i));
                        episodes.add(manga);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }else if(data.exists()){
                mode = 3;

                if(!title.useBookmark()){
                    // is migrated
                   mode = 4;
                }else{
                    p.addRecent(title);
                }

                episodes =  title.getEps();
                offlineEpisodes = new ArrayList<>();
                for(File folder : getOfflineEpisodes()){
                    //get id from listContent
                    String name = folder.getName();
                    try {
                        int index = episodes.indexOf(new Manga(Integer.parseInt(name.substring(name.lastIndexOf('.') + 1)),"","", title.getBaseMode()));
                        if(index>-1){
                            episodes.get(index).setOfflinePath(folder);
                            episodes.get(index).setMode(mode);
                        }
                    }catch(Exception e){
                        // folder name is not properly formatted
                    }
                }
                //for loop to remove non-existing episodes
                for(int i = episodes.size()-1;i>=0 ;i--){
                    if(episodes.get(i).getOfflinePath()==null) episodes.remove(i);
                }

            } else {
                mode = 1;
                for (File f : offlineEpisodes) {
                    Manga manga;
                    manga = new Manga(-1, f.getName(), "", title.getBaseMode());
                    manga.setMode(mode);
                    manga.setOfflinePath(f);
                    //add local images to manga
                    episodes.add(manga);
                    // set eps to title object
                    title.setEps(episodes);
                }
            }
            //set up adapter
            episodeAdapter = new EpisodeAdapter(context, episodes, title, mode);
            afterLoad();
        }
    }

    public List<File> getOfflineEpisodes(){
        File[] episodeFiles = new File(homeDir, filterFolder(title.getName())).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        //sort
        Arrays.sort(episodeFiles);
        //add as manga
        return Arrays.asList(episodeFiles);
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
            title.fetchEps(httpClient);
            episodes = title.getEps();
            episodeAdapter = new EpisodeAdapter(context, episodes, title, mode);
            return null;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            if(episodes == null || episodes.size()==0){
                showCaptchaPopup(context, p);
                return;
            }
            afterLoad();
            p.addRecent(title);
            p.updateRecentData(title);
            progress.setVisibility(View.GONE);
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

}

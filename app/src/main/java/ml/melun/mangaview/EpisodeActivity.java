package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.EpisodeAdapter;
import ml.melun.mangaview.adapter.TagAdapter;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

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
    ArrayList<Manga> episodes;
    Boolean dark;
    Intent viewer;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode== RESULT_OK && requestCode==0){
            int newid = data.getIntExtra("id", -1);
            if(newid>-1 && newid!=bookmarkId){
                bookmarkId = newid;
                //find index of bookmark;
                for(int i=0; i< episodes.size(); i++){
                    if(episodes.get(i).getId()==bookmarkId){
                        bookmarkIndex=i;
                        break;
                    }
                }
                episodeAdapter.setBookmark(bookmarkIndex);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference();
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode);
        Intent intent = getIntent();
        upBtn = (FloatingActionButton) findViewById(R.id.upBtn);
        title = new Title(intent.getStringExtra("title")
                ,intent.getStringExtra("thumb")
                ,intent.getStringExtra("author")
                ,intent.getStringArrayListExtra("tags"));
        bookmarkId = p.getBookmark();
        position = intent.getIntExtra("position",0);
        favoriteResult = intent.getBooleanExtra("favorite",false);
        recentResult = intent.getBooleanExtra("recent",false);
        episodeList = this.findViewById(R.id.EpisodeList);
        episodeList.setLayoutManager(new LinearLayoutManager(this));
        ((SimpleItemAnimator) episodeList.getItemAnimator()).setSupportsChangeAnimations(false);
        if(recentResult){
            Intent resultIntent = new Intent();
            setResult(RESULT_OK,resultIntent);
        }
        getEpisodes g = new getEpisodes();
        g.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
            title.fetchEps();
            episodes = title.getEps();
            episodes.add(0,new Manga(0,""));
            //find bookmark
            if(bookmarkId!=-1){
                for(int i=0; i< episodes.size(); i++){
                    if(episodes.get(i).getId()==bookmarkId){
                        bookmarkIndex=i;
                        break;
                    }
                }
            }
            episodeAdapter = new EpisodeAdapter(context, episodes, title);
            return null;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            episodeAdapter.setFavorite(p.findFavorite(title)>-1);
            episodeAdapter.setBookmark(bookmarkIndex);
            episodeList.setAdapter(episodeAdapter);
            if(bookmarkIndex>8){
                episodeList.scrollToPosition(bookmarkIndex);
                upBtn.setAlpha(1.0f);
                upBtnVisible = true;
            }else{
                upBtn.setAlpha(0.0f);
                upBtnVisible = false;
            }
//            getActionBarView().setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    System.out.println("hellllllllloi");
//                }
//            });
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
                public void onItemClick(View v, int position) {
                    // start intent : Episode viewer
                    Manga selected = episodeAdapter.getItem(position);
                    System.out.println(selected.getId());

                    p.setBookmark(selected.getId());
                    if(p.getScrollViewer()) viewer = new Intent(context, ViewerActivity.class);
                    else viewer = new Intent(context, ViewerActivity2.class);
                    viewer.putExtra("id", selected.getId());
                    viewer.putExtra("name",selected.getName());
                    startActivityForResult(viewer,0);
                }
                @Override
                public void onStarClick(){
                    //star click handler
                    episodeAdapter.setFavorite(p.toggleFavorite(title,position));
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
                    JSONArray mangas = new JSONArray();
                    //index 0 contains header : blank Manga
                    for(int i=1; i<episodes.size();i++) {
                        mangas.put(episodes.get(i).toString());
                    }
                    download.putExtra("list",mangas.toString());
                    download.putExtra("name",title.getName());
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
            //if title data is updated, add data
            p.updateRecentData(title.getName(),title.getThumb(),title.getAuthor(),title.getTags());
            if (pd.isShowing()) {
                pd.dismiss();
            }
        }
    }
}

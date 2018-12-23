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

import java.util.ArrayList;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.EpisodeAdapter;
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
    Downloader downloader;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode);
        downloader =  new Downloader();
        Intent intent = getIntent();
        upBtn = (FloatingActionButton) findViewById(R.id.upBtn);
        title = new Title(intent.getStringExtra("title"),intent.getStringExtra("thumb"));
        p = new Preference();
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
    public View getActionBarView() {
        Window window = getWindow();
        View v = window.getDecorView();
        int resId = getResources().getIdentifier("action_bar_container", "id", "android");
        return v.findViewById(resId);
    }

    private class getEpisodes extends AsyncTask<Void,Void,Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(EpisodeActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
        }

        protected Integer doInBackground(Void... params) {
            title.fetchEps();
            ArrayList<Manga> episodes = title.getEps();
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
                        upBtn.animate().alpha(1.0f);
                        upBtnVisible = true;
                    } else if(firstVisible==0 && upBtnVisible){
                        upBtn.animate().alpha(0.0f);
                        upBtnVisible = false;
                    }
                }
            });
            upBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    episodeList.scrollToPosition(0);
                    upBtn.animate().alpha(0.0f);
                    upBtnVisible=false;
                }
            });
            episodeAdapter.setClickListener(new EpisodeAdapter.ItemClickListener() {
                @Override
                public void onItemClick(View v, int position) {
                    // start intent : Episode viewer
                    Manga selected = episodeAdapter.getItem(position);
                    System.out.println(selected.getId());
                    Intent viewer = new Intent(context, ViewerActivity.class);
                    p.setBookmark(selected.getId());
                    viewer.putExtra("id", selected.getId());
                    viewer.putExtra("name",selected.getName());
                    startActivity(viewer);
                }
                @Override
                public void onStarClick(View v){
                    //star click handler
                    episodeAdapter.setFavorite(p.toggleFavorite(title,position));
                    if(favoriteResult){
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("favorite", p.findFavorite(title)>-1);
                        setResult(RESULT_OK, resultIntent);
                    }
                }
                @Override
                public void onDownloadClick(View v){
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
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(title.getName()+ " 을(를) 다운로드 하시겠습니까?\n[총 "+title.getEpsCount()+"화]\n*테스트 중, 저장위치: /sdcard/MangaView/saveTest/").setPositiveButton("예!", dialogClickListener)
                            .setNegativeButton("그건좀..", dialogClickListener).show();
                }
            });

            if (pd.isShowing()) {
                pd.dismiss();
            }
        }
    }
}

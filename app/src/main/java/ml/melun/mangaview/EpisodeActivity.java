package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode);
        Intent intent = getIntent();
        title = new Title(intent.getStringExtra("title"),intent.getStringExtra("thumb"));
        p = new Preference();
        bookmarkId = p.getBookmark();
        position = intent.getIntExtra("position",0);
        favoriteResult = intent.getBooleanExtra("favorite",false);
        recentResult = intent.getBooleanExtra("recent",false);
        episodeList = this.findViewById(R.id.EpisodeList);
        episodeList.setLayoutManager(new LinearLayoutManager(this));
        if(recentResult){
            Intent resultIntent = new Intent();
            setResult(RESULT_OK,resultIntent);
        }

        getEpisodes g = new getEpisodes();
        g.execute();
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
            if(bookmarkIndex>8) episodeList.scrollToPosition(bookmarkIndex);

            //todo: 맨위로 스크롤 할수 있는 방법 제공 : ex) 액션바 터치했을때
//            getActionBarView().setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    System.out.println("hellllllllloi");
//                }
//            });
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
                    //
                }
            });

            if (pd.isShowing()) {
                pd.dismiss();
            }
        }
    }
}

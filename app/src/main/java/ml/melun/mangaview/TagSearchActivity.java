package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.w3c.dom.Text;

import java.util.ArrayList;

import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

public class TagSearchActivity extends AppCompatActivity {
    RecyclerView searchResult;
    int mode;
    String query;
    TitleAdapter adapter;
    Context context;
    ProgressDialog pd;
    Search search;
    TextView noresult;
    Preference p;
    SwipyRefreshLayout swipe;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference();
        if(p.getDarkTheme()) setTheme(R.style.AppThemeDark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_search);
        context = this;
        searchResult = this.findViewById(R.id.tagSearchResult);
        noresult = this.findViewById(R.id.tagSearchNoResult);
        LinearLayoutManager lm = new LinearLayoutManager(context);
        searchResult.setLayoutManager(lm);
        Intent i = getIntent();
        query = i.getStringExtra("query");
        mode = i.getIntExtra("mode",0);
        swipe = this.findViewById(R.id.tagSearchSwipe);

        ActionBar ab = getSupportActionBar();
        switch(mode){
            case 0:
                break;
            case 1:
                ab.setTitle("작가: "+query);
                break;
            case 2:
                ab.setTitle("태그: "+query);
                break;
            case 3:
            case 4:
                ab.setTitle("검색 결과");
                break;
        }
        adapter = new TitleAdapter(context);
        search  = new Search(query, mode);
        swipe.setRefreshing(true);
        searchManga sm = new searchManga();
        sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        swipe.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                if(!search.isLast()) {
                    searchManga sm = new searchManga();
                    sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else swipe.setRefreshing(false);
            }
        });
    }


    private class searchManga extends AsyncTask<String,String,String> {
        protected void onPreExecute(){
            super.onPreExecute();
        }
        protected String doInBackground(String... params){
            search.fetch();
            return null;
        }
        @Override
        protected void onPostExecute(String res){
            super.onPostExecute(res);
            if(adapter.getItemCount()==0) {
                adapter.addData(search.getResult());
                searchResult.setAdapter(adapter);
                adapter.setClickListener(new TitleAdapter.ItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        // start intent : Episode viewer
                        Title selected = adapter.getItem(position);
                        p.addRecent(selected);
                        System.out.println("onItemClick position: " + position);

                        Intent episodeView = new Intent(context, EpisodeActivity.class);
                        episodeView.putExtra("title", selected.getName());
                        episodeView.putExtra("thumb", selected.getThumb());
                        episodeView.putExtra("author", selected.getAuthor());
                        episodeView.putExtra("tags", new ArrayList<String>(selected.getTags()));
                        startActivity(episodeView);
                    }
                });
            }else{
                adapter.addData(search.getResult());
            }

            if(adapter.getItemCount()>0) {
                noresult.setVisibility(View.GONE);
            }else{
                noresult.setVisibility(View.VISIBLE);
            }
            swipe.setRefreshing(false);
        }
    }

}

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
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.w3c.dom.Text;

import java.util.ArrayList;

import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.adapter.UpdatedAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.mangaview.UpdatedList;

public class TagSearchActivity extends AppCompatActivity {
    RecyclerView searchResult;
    int mode;
    String query;
    TitleAdapter adapter;
    UpdatedAdapter uadapter;
    Context context;
    Search search;
    UpdatedList updated;
    TextView noresult;
    Preference p;
    SwipyRefreshLayout swipe;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
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
            case 5:
                ab.setTitle("최근 추가됨");
                break;
            case 6:
                ab.setTitle("검색결과");
                break;
        }
        ab.setDisplayHomeAsUpEnabled(true);

        if(mode == 5){
            uadapter = new UpdatedAdapter(context);
            swipe.setRefreshing(true);
            updated = new UpdatedList();
            getUpdated gu = new getUpdated();
            gu.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            swipe.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh(SwipyRefreshLayoutDirection direction) {
                    if (!updated.isLast()) {
                        getUpdated gu = new getUpdated();
                        gu.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else swipe.setRefreshing(false);
                }
            });
        }else {
            adapter = new TitleAdapter(context);
            swipe.setRefreshing(true);
            search = new Search(query,mode);
            searchManga sm = new searchManga();
            sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            swipe.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh(SwipyRefreshLayoutDirection direction) {
                    if (!search.isLast()) {
                        searchManga sm = new searchManga();
                        sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else swipe.setRefreshing(false);
                }
            });
        }
    }
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                        //System.out.println("onItemClick position: " + position);
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

    private class getUpdated extends AsyncTask<String,String,String> {
        protected void onPreExecute(){
            super.onPreExecute();
        }
        protected String doInBackground(String... params){
            updated.fetch();
            return null;
        }
        @Override
        protected void onPostExecute(String res){
            super.onPostExecute(res);
            if(uadapter.getItemCount()==0) {
                uadapter.addData(updated.getResult());
                searchResult.setAdapter(uadapter);
                uadapter.setOnClickListener(new UpdatedAdapter.onclickListener() {
                    @Override
                    public void onclick(Manga m) {
                        //open viewer
                        Intent viewer;
                        if(p.getScrollViewer())  viewer = new Intent(context, ViewerActivity.class);
                        else viewer = new Intent(context, ViewerActivity2.class);
                        viewer.putExtra("id", m.getId());
                        viewer.putExtra("name",m.getName());
                        startActivityForResult(viewer,0);
                    }
                });
            }else{
                uadapter.addData(updated.getResult());
            }

            if(uadapter.getItemCount()>0) {
                noresult.setVisibility(View.GONE);
            }else{
                noresult.setVisibility(View.VISIBLE);
            }
            swipe.setRefreshing(false);
        }
    }

}

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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
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
            search.fetch(p.getUrl());
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
                    public void onResumeClick(int position, int id) {
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
                        viewer.putExtra("manga",new Gson().toJson(new Manga(id,"","")));
                        viewer.putExtra("online",true);
                        startActivity(viewer);
                    }

                    @Override
                    public void onItemClick(int position) {
                        // start intent : Episode viewer
                        Title selected = adapter.getItem(position);
                        //System.out.println("onItemClick position: " + position);
                        Intent episodeView = new Intent(context, EpisodeActivity.class);
                        episodeView.putExtra("title", new Gson().toJson(selected));
                        episodeView.putExtra("online", true);
                        startActivity(episodeView);
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                        popup(view, position, adapter.getItem(position), 0);
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
            updated.fetch(p.getUrl());
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
                    public void onEpsClick(Title t) {
                        Intent eps = new Intent(context, EpisodeActivity.class);
                        eps.putExtra("title", new Gson().toJson(t));
                        eps.putExtra("online", true);
                        startActivity(eps);
                    }

                    @Override
                    public void onClick(Manga m) {
                        //open viewer
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
                        viewer.putExtra("manga", new Gson().toJson(m));
                        viewer.putExtra("online", true);
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
    void popup(View view, final int position, final Title title, final int m){
        PopupMenu popup = new PopupMenu(TagSearchActivity.this, view);
        //Inflating the Popup using xml file
        popup.getMenuInflater()
                .inflate(R.menu.title_options, popup.getMenu());
        popup.getMenu().removeItem(R.id.del);
        popup.getMenu().findItem(R.id.favAdd).setVisible(true);
        popup.getMenu().findItem(R.id.favDel).setVisible(true);
        if(p.findFavorite(title)>-1) popup.getMenu().removeItem(R.id.favAdd);
        else popup.getMenu().removeItem(R.id.favDel);


        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()){
                    case R.id.del:
                        break;
                    case R.id.favAdd:
                    case R.id.favDel:
                        //toggle favorite
                        p.toggleFavorite(title,0);
                        break;
                }
                return true;
            }
        });
        popup.show(); //showing popup menu
    }
}

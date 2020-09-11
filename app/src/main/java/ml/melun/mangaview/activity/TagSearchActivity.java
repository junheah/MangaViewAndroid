package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.adapter.UpdatedAdapter;
import ml.melun.mangaview.mangaview.Bookmark;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.mangaview.UpdatedList;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.Utils.viewerIntent;

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
    SwipyRefreshLayout swipe;
    Bookmark bookmark;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
            case 7:
                ab.setTitle("북마크");
                break;
        }

        ab.setDisplayHomeAsUpEnabled(true);
        swipe.setRefreshing(true);

        if(mode == 5) {
            uadapter = new UpdatedAdapter(context);
            updated = new UpdatedList();
            getUpdated gu = new getUpdated();
            gu.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            swipe.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh(SwipyRefreshLayoutDirection direction) {
                    if(p.getLogin() == null){
                        Toast.makeText(context, "다음 페이지를 보려면 로그인 해야 합니다.",  Toast.LENGTH_SHORT).show();
                        swipe.setRefreshing(false);
                    } else if (!updated.isLast()) {
                        getUpdated gu = new getUpdated();
                        gu.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else swipe.setRefreshing(false);
                }
            });

        }else if(mode == 7){
            adapter = new TitleAdapter(context);
            bookmark = new Bookmark();
            getBookmarks gb = new getBookmarks();
            gb.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            swipe.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh(SwipyRefreshLayoutDirection direction) {
                    if (!bookmark.isLast()) {
                        getBookmarks gb = new getBookmarks();
                        gb.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else swipe.setRefreshing(false);
                }
            });

        }else {
            adapter = new TitleAdapter(context);
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


    private class getBookmarks extends AsyncTask<Void, Void, Integer>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            if(integer != 0){
                showCaptchaPopup(context);
            }
            if(adapter.getItemCount()==0) {
                adapter.addData(bookmark.getResult());
                searchResult.setAdapter(adapter);
                adapter.setClickListener(new TitleAdapter.ItemClickListener() {
                    @Override
                    public void onResumeClick(int position, int id) {
                        Intent viewer = viewerIntent(context, new Manga(id,"",""));
                        viewer.putExtra("online",true);
                        startActivity(viewer);
                    }

                    @Override
                    public void onItemClick(int position) {
                        // start intent : Episode viewer
                        Title selected = adapter.getItem(position);
                        Intent episodeView = episodeIntent(context, selected);
                        episodeView.putExtra("online", true);
                        startActivity(episodeView);
                    }

                    @Override
                    public void onLongClick(View view, int position) {
                        popup(view, position, adapter.getItem(position), 0);
                    }
                });
            }else{
                adapter.addData(bookmark.getResult());
            }

            if(adapter.getItemCount()>0) {
                noresult.setVisibility(View.GONE);
            }else{
                noresult.setVisibility(View.VISIBLE);
            }
            swipe.setRefreshing(false);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            return bookmark.fetch(httpClient);
        }
    }


    private class searchManga extends AsyncTask<String,String,Integer> {
        protected void onPreExecute(){
            super.onPreExecute();
        }
        protected Integer doInBackground(String... params){
            return search.fetch(httpClient);
        }
        @Override
        protected void onPostExecute(Integer res){
            super.onPostExecute(res);
            if(res != 0){
                showCaptchaPopup(context);
            }
            if(adapter.getItemCount()==0) {
                adapter.addData(search.getResult());
                searchResult.setAdapter(adapter);
                adapter.setClickListener(new TitleAdapter.ItemClickListener() {
                    @Override
                    public void onResumeClick(int position, int id) {
                        Intent viewer = viewerIntent(context, new Manga(id,"",""));
                        viewer.putExtra("online",true);
                        startActivity(viewer);
                    }

                    @Override
                    public void onItemClick(int position) {
                        // start intent : Episode viewer
                        Title selected = adapter.getItem(position);
                        Intent episodeView = episodeIntent(context, selected);
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
            updated.fetch(httpClient);
            return null;
        }
        @Override
        protected void onPostExecute(String res){
            super.onPostExecute(res);
            if(updated.getResult().size() == 0 && uadapter.getItemCount() == 0){
                //error
                showCaptchaPopup(context);
            }
            if(uadapter.getItemCount()==0) {
                uadapter.addData(updated.getResult());
                searchResult.setAdapter(uadapter);
                uadapter.setOnClickListener(new UpdatedAdapter.onclickListener() {
                    @Override
                    public void onEpsClick(Title t) {
                        Intent eps = episodeIntent(context, t);
                        eps.putExtra("online", true);
                        startActivity(eps);
                    }

                    @Override
                    public void onClick(Manga m) {
                        //open viewer
                        Intent viewer = viewerIntent(context, m);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
        startActivity(getIntent());
    }
}

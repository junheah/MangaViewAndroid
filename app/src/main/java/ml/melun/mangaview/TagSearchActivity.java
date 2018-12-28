package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

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
    Boolean searching = false;
    Context context;
    ProgressDialog pd;
    Search search;
    TextView noresult;
    Preference p;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        p = new Preference();

        getSupportActionBar().setTitle("검색 결과");
        adapter = new TitleAdapter(context);
        search  = new Search(query, mode);
        searchManga sm = new searchManga();
        sm.execute();

        searchResult.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(!searchResult.canScrollVertically(1)&&!searching){
                    if(!search.isLast()){
                        searching = true;
                        System.out.println("ddddddddddddddddddsearch");
                        searchManga sm = new searchManga();
                        sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            }
        });

    }


    private class searchManga extends AsyncTask<String,String,String> {
        protected void onPreExecute(){
            super.onPreExecute();
            pd = new ProgressDialog(context);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
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
                    public void onItemClick(View v, int position) {
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

            if (pd.isShowing()){
                pd.dismiss();
            }
            searching = false;
        }
    }

}

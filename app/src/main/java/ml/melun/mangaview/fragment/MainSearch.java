package ml.melun.mangaview.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import ml.melun.mangaview.R;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.activity.AdvSearchActivity;
import ml.melun.mangaview.activity.MainActivity;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.openViewer;
import static ml.melun.mangaview.Utils.popup;

public class MainSearch extends Fragment {
    SwipyRefreshLayout swipe;
    FloatingActionButton advSearchBtn;
    TextView noresult;
    private EditText searchBox;
    RecyclerView searchResult;
    Spinner searchMode;
    TitleAdapter searchAdapter;
    Search search;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_search , container, false);

        //search content
        noresult = rootView.findViewById(R.id.noResult);
        searchBox = rootView.findViewById(R.id.searchBox);
        searchResult = rootView.findViewById(R.id.searchResult);
        searchResult.setLayoutManager(new LinearLayoutManager(getContext()));
        searchMode = rootView.findViewById(R.id.searchMode);
        advSearchBtn = rootView.findViewById(R.id.advSearchBtn);
        swipe = rootView.findViewById(R.id.searchSwipe);
        if(p.getDarkTheme()) searchMode.setPopupBackgroundResource(R.color.colorDarkWindowBackground);

        advSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent advSearch = new Intent(getContext(), AdvSearchActivity.class);
                startActivity(advSearch);
            }
        });

        searchBox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction()==KeyEvent.ACTION_DOWN && keyCode ==KeyEvent.KEYCODE_ENTER){
                    String query = searchBox.getText().toString();
                    if(query.length()>0) {
                        swipe.setRefreshing(true);
                        if(searchAdapter != null) searchAdapter.removeAll();
                        else searchAdapter = new TitleAdapter(getContext());
                        search = new Search(query,searchMode.getSelectedItemPosition());

                        SearchManga sm = new SearchManga();
                        sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                    return true;
                }
                return false;
            }
        });

        swipe.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                if(search==null) swipe.setRefreshing(false);
                else {
                    if (!search.isLast()) {
                        SearchManga sm = new SearchManga();
                        sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else swipe.setRefreshing(false);
                }
            }
        });
        return rootView;
    }


    private class SearchManga extends AsyncTask<String,String,Integer>{
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
                // error
                Utils.showCaptchaPopup(getContext(), 4);
            }

            if(searchAdapter.getItemCount()==0) {
                searchAdapter.addData(search.getResult());
                searchResult.setAdapter(searchAdapter);
                searchAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                    @Override
                    public void onLongClick(View view, int position) {
                        //none
                        Title title = searchAdapter.getItem(position);
                        popup(getContext(),view, position, title, 0, new PopupMenu.OnMenuItemClickListener(){
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch(item.getItemId()){
                                    case R.id.favAdd:
                                    case R.id.favDel:
                                        //toggle favorite
                                        p.toggleFavorite(title,0);
                                        break;
                                }
                                return false;
                            }
                        }, p);
                    }

                    @Override
                    public void onResumeClick(int position, int id) {
                        openViewer(getContext(),new Manga(id,"",""),-1);
                    }

                    @Override
                    public void onItemClick(int position) {
                        // start intent : Episode viewer
                        Intent episodeView = episodeIntent(getContext(), searchAdapter.getItem(position));
                        startActivity(episodeView);
                    }
                });
            }else{
                searchAdapter.addData(search.getResult());
            }

            if(searchAdapter.getItemCount()>0) {
                noresult.setVisibility(View.GONE);
            }else{
                noresult.setVisibility(View.VISIBLE);
            }

            swipe.setRefreshing(false);
        }
    }
}

package ml.melun.mangaview.fragment;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.openViewer;
import static ml.melun.mangaview.Utils.popup;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;

public class MainSearch extends Fragment {
    SwipyRefreshLayout swipe;
    FloatingActionButton advSearchBtn;
    TextView noresult;
    private EditText searchBox;
    RecyclerView searchResult;
    Spinner searchMode, baseMode;
    TitleAdapter searchAdapter;
    Search search;
    Fragment fragment;
    LinearLayoutCompat optionsPanel;
    String prequery = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_search , container, false);

        //search content
        noresult = rootView.findViewById(R.id.noResult);
        searchBox = rootView.findViewById(R.id.searchBox);
        searchResult = rootView.findViewById(R.id.searchResult);
        searchResult.setLayoutManager(new NpaLinearLayoutManager(getContext()));
        searchMode = rootView.findViewById(R.id.searchMode);
        baseMode = rootView.findViewById(R.id.searchBaseMode);
        advSearchBtn = rootView.findViewById(R.id.advSearchBtn);
        swipe = rootView.findViewById(R.id.searchSwipe);
        optionsPanel = rootView.findViewById(R.id.searchOptionPanel);
        fragment = this;
        if(p.getDarkTheme()){
            searchMode.setPopupBackgroundResource(R.color.colorDarkWindowBackground);
            baseMode.setPopupBackgroundResource(R.color.colorDarkWindowBackground);
        }

        searchBox.setOnFocusChangeListener((view, b) -> {
            if(b){
                optionsPanel.setVisibility(View.VISIBLE);
            }else{
                optionsPanel.setVisibility(View.GONE);
            }
        });

        advSearchBtn.setOnClickListener(v -> {
            Toast.makeText(getContext(), "고급검색 기능 사용 불가", Toast.LENGTH_LONG).show();
//                Intent advSearch = new Intent(getContext(), AdvSearchActivity.class);
//                startActivity(advSearch);
        });

        searchBox.setOnKeyListener((v, keyCode, event) -> {
            if(event.getAction()==KeyEvent.ACTION_DOWN && keyCode ==KeyEvent.KEYCODE_ENTER){
                searchSubmit();
                return true;
            }
            return false;
        });

        AdapterView.OnItemSelectedListener mlistener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                optionUpdate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                optionUpdate();
            }
        };
        baseMode.setOnItemSelectedListener(mlistener);
        searchMode.setOnItemSelectedListener(mlistener);

        baseMode.setSelection(p.getBaseMode()-1);



        swipe.setOnRefreshListener(direction -> {
            if(search==null) swipe.setRefreshing(false);
            else {
                if (!search.isLast()) {
                    SearchManga sm = new SearchManga();
                    sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else swipe.setRefreshing(false);
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(prequery != null){
            searchBox.setText(prequery);
            prequery = null;
        }
    }

    void optionUpdate(){
        //shows or hides options
        //p.setBaseMode(baseMode.getSelectedItemPosition()+1);
    }

    public void setSearch(String prequery){
        this.prequery = prequery;
    }

    void searchSubmit(){
        String query = searchBox.getText().toString();
        if(query.length()>0) {
            swipe.setRefreshing(true);
            if(searchAdapter != null) searchAdapter.removeAll();
            else searchAdapter = new TitleAdapter(getContext());
            search = new Search(query,searchMode.getSelectedItemPosition(), baseMode.getSelectedItemPosition()+1);
            new SearchManga().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_CAPTCHA && searchAdapter!=null && search != null)
            searchSubmit();
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
                Utils.showCaptchaPopup(getContext(), 4, fragment, p);
            }

            if(searchAdapter.getItemCount()==0) {
                searchAdapter.addData(search.getResult());
                searchResult.setAdapter(searchAdapter);
                searchAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                    @Override
                    public void onLongClick(View view, int position) {
                        //none
                        Title title = searchAdapter.getItem(position);
                        popup(getContext(),view, position, title, 0, item -> {
                            switch(item.getItemId()){
                                case R.id.favAdd:
                                case R.id.favDel:
                                    //toggle favorite
                                    p.toggleFavorite(title,0);
                                    break;
                            }
                            return false;
                        }, p);
                    }

                    @Override
                    public void onResumeClick(int position, int id) {
                        openViewer(getContext(),new Manga(id,"","", search.getBaseMode()),-1);
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

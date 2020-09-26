package ml.melun.mangaview.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.UrlUpdater;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.activity.TagSearchActivity;
import ml.melun.mangaview.adapter.MainAdapter;
import ml.melun.mangaview.adapter.MainWebtoonAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.openViewer;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;

public class MainMain extends Fragment{

    RecyclerView mainRecycler;
    MainAdapter mainadapter;
    MainWebtoonAdapter mainWebtoonAdapter;
    Fragment fragment;
    boolean wait = false;
    UrlUpdater.UrlUpdaterCallback callback;

    final static int COMIC_TAB = 0;
    final static int WEBTOON_TAB = 1;

    boolean fragmentActive = false;

    public void setWait(Boolean wait){
        this.wait = wait;
    }

    public MainMain(){
        callback = new UrlUpdater.UrlUpdaterCallback() {
            @Override
            public void callback(boolean success) {
                wait = false;
                if(mainadapter != null && fragmentActive) {
                    mainadapter.fetch();
                }
                if(mainWebtoonAdapter != null && fragmentActive) {
                    mainWebtoonAdapter.fetch();
                }
            }
        };
    }

    public UrlUpdater.UrlUpdaterCallback getCallback(){
        return callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_main , container, false);


        TabLayout tabLayout = rootView.findViewById(R.id.mainTab);
        tabLayout.addTab(tabLayout.newTab().setText("만화"));
        tabLayout.addTab(tabLayout.newTab().setText("웹툰"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if(tab.getPosition() == COMIC_TAB){
                    mainRecycler.setAdapter(mainadapter);
                }else if(tab.getPosition() == WEBTOON_TAB){
                    mainRecycler.setAdapter(mainWebtoonAdapter);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        fragment = this;
        //main content
        // 최근 추가된 만화
        mainRecycler = rootView.findViewById(R.id.main_recycler);
        mainRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mainadapter = new MainAdapter(getContext());
        mainadapter.setMainClickListener(new MainAdapter.onItemClick() {

            @Override
            public void clickedTitle(Title t) {
                startActivity(episodeIntent(getContext(), t));
            }

            @Override
            public void clickedManga(Manga m) {
                //mget title from manga m and start intent for manga m
                //getTitleFromManga intentStarter = new getTitleFromManga();
                //intentStarter.execute(m);
                openViewer(getContext(), m,-1);
            }

            @Override
            public void clickedTag(String t) {
                Intent i = new Intent(getContext(), TagSearchActivity.class);
                i.putExtra("query",t);
                i.putExtra("mode",2);
                startActivity(i);
            }

            @Override
            public void clickedName(String t) {
                Intent i = new Intent(getContext(), TagSearchActivity.class);
                i.putExtra("query",t);
                i.putExtra("mode",3);
                startActivity(i);
            }

            @Override
            public void clickedRelease(String t) {
                Intent i = new Intent(getContext(), TagSearchActivity.class);
                i.putExtra("query",t);
                i.putExtra("mode",4);
                startActivity(i);
            }

            @Override
            public void clickedMoreUpdated() {
                Intent i = new Intent(getContext(), TagSearchActivity.class);
                i.putExtra("mode",5);
                startActivity(i);
            }

            @Override
            public void captchaCallback() {
                Utils.showCaptchaPopup(getContext(), 3, fragment, p);
            }
        });

        mainWebtoonAdapter = new MainWebtoonAdapter(getContext());

        mainRecycler.setAdapter(mainadapter);

        if(!wait) {
            mainadapter.fetch();
            mainWebtoonAdapter.fetch();
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        fragmentActive = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        fragmentActive = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_CAPTCHA && mainadapter!=null)
            mainadapter.fetch();
    }
}

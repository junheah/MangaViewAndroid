package ml.melun.mangaview.fragment;

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

import ml.melun.mangaview.R;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.activity.TagSearchActivity;
import ml.melun.mangaview.adapter.MainAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.openViewer;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;

public class MainMain extends Fragment{

    RecyclerView mainRecycler;
    MainAdapter mainadapter;
    Fragment fragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_main , container, false);

        fragment = this;
        //main content
        // 최근 추가된 만화
        mainRecycler = rootView.findViewById(R.id.main_recycler);
        mainadapter = new MainAdapter(getContext());
        mainRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        mainRecycler.setAdapter(mainadapter);
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
                Utils.showCaptchaPopup(getContext(), 3, fragment);
            }
        });
        mainadapter.fetch();
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_CAPTCHA && mainadapter!=null)
            mainadapter.fetch();
    }
}

package ml.melun.mangaview.fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.MTitle;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static android.app.Activity.RESULT_OK;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.deleteRecursive;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.filterFolder;
import static ml.melun.mangaview.Utils.openViewer;
import static ml.melun.mangaview.Utils.popup;
import static ml.melun.mangaview.Utils.showPopup;

public class MainRecent extends MainActivityFragment {
    int selectedPosition = -1;
    RecyclerView recentResult;
    TitleAdapter recentAdapter;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_recent , container, false);
        recentResult = rootView.findViewById(R.id.recentList);
        recentAdapter = new TitleAdapter(getContext());
        recentAdapter.addData(p.getRecent());
        recentResult.setLayoutManager(new LinearLayoutManager(getContext()));
        recentResult.setAdapter(recentAdapter);
        recentAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
            @Override
            public void onLongClick(View view, int position) {
                //longclick
                Title title = recentAdapter.getItem(position);
                popup(getContext(), view, position, title, 1, new PopupMenu.OnMenuItemClickListener(){
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.del:
                                //delete (only in recent)
                                recentAdapter.remove(position);
                                p.removeRecent(position);
                                break;
                            case R.id.favAdd:
                            case R.id.favDel:
                                //toggle favorite
                                p.toggleFavorite(title, 0);
                                break;

                        }
                        return false;
                    }
                }, p);
            }

            @Override
            public void onResumeClick(int position, int id) {
                selectedPosition = position;
                openViewer(getContext(), new Manga(id,"",""),2);
            }

            @Override
            public void onItemClick(int position) {
                // start intent : Episode viewer
                selectedPosition = position;
                Intent episodeView = episodeIntent(getContext(), recentAdapter.getItem(position));
                episodeView.putExtra("recent",true);
                startActivityForResult(episodeView,2);
            }
        });

//        new AsyncTask<Void,Void,Void>(){
//            List<MTitle> titles;
//
//            @Override
//            protected void onPostExecute(Void aVoid) {
//                recentAdapter.addData(titles);
//            }
//
//            @Override
//            protected Void doInBackground(Void... voids) {
//                titles = p.getRecent();
//                return null;
//            }
//        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && recentAdapter != null && recentAdapter.getItemCount() > 0)
            recentAdapter.moveItemToTop(selectedPosition);
    }
}

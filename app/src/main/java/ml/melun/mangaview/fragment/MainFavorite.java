package ml.melun.mangaview.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static android.app.Activity.RESULT_OK;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.openViewer;
import static ml.melun.mangaview.Utils.popup;

public class MainFavorite extends Fragment {
    TitleAdapter favoriteAdapter;
    RecyclerView favoriteResult;
    int selectedPosition = -1;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_favorite , container, false);
        favoriteResult = rootView.findViewById(R.id.favoriteList);
        favoriteAdapter = new TitleAdapter(getContext());
        favoriteAdapter.addData(p.getFavorite());
        favoriteResult.setLayoutManager(new LinearLayoutManager(getContext()));
        favoriteResult.setAdapter(favoriteAdapter);
        favoriteAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
            @Override
            public void onResumeClick(int position, int id) {
                openViewer(getContext(), new Manga(id,"",""),-1);
            }

            @Override
            public void onLongClick(View view, int position) {
                Title title = favoriteAdapter.getItem(position);
                popup(getContext(), view, position, title, 2, new PopupMenu.OnMenuItemClickListener(){
                    public boolean onMenuItemClick(MenuItem item) {
                        switch(item.getItemId()){
                            case R.id.favAdd:
                            case R.id.favDel:
                                //toggle favorite
                                p.toggleFavorite(title,0);
                                favoriteAdapter.remove(position);
                                break;
                        }
                        return false;
                    }
                }, p);
            }

            @Override
            public void onItemClick(int position) {
                // start intent : Episode viewer
                //start intent for result : has to know if favorite has been removed or not
                Intent episodeView = episodeIntent(getContext(),favoriteAdapter.getItem(position));
                episodeView.putExtra("position", position);
                episodeView.putExtra("favorite",true);
                selectedPosition = position;
                startActivityForResult(episodeView,1);
            }
        });

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            boolean favorite_after = data.getBooleanExtra("favorite",true);
            if(!favorite_after && favoriteAdapter != null && favoriteAdapter.getItemCount()>0)
                favoriteAdapter.remove(selectedPosition);
        }
    }
}

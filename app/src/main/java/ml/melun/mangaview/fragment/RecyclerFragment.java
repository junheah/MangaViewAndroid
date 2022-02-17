package ml.melun.mangaview.fragment;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.MTitle;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static android.app.Activity.RESULT_OK;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.CODE_SCOPED_STORAGE;
import static ml.melun.mangaview.Utils.deleteRecursive;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.filterFolder;
import static ml.melun.mangaview.Utils.readFileToString;
import static ml.melun.mangaview.Utils.readUriToString;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.Utils.viewerIntent;

public class RecyclerFragment extends Fragment {
    int selectedPosition = -1;
    TitleAdapter titleAdapter;
    RecyclerView recyclerView;
    int mode = -1;
    boolean loaded = false;
    SearchView searchView;


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("mode", mode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_recycler , container, false);
        recyclerView = rootView.findViewById(R.id.recycler_list);
        titleAdapter = new TitleAdapter(getContext());
        recyclerView.setLayoutManager(new NpaLinearLayoutManager(getContext()));
        recyclerView.setAdapter(titleAdapter);
        titleAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
            @Override
            public void onResumeClick(int position, int id) {
                selectedPosition = position;
                if(mode == R.id.nav_recent) {
                    openViewer(new Manga(id, "", "" , titleAdapter.getItem(position).getBaseMode()), 2);
                } else if(mode == R.id.nav_favorite) {
                    openViewer(new Manga(id, "", "", titleAdapter.getItem(position).getBaseMode()), -1);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                Title title = titleAdapter.getItem(position);
                if(mode == R.id.nav_favorite) {
                    popup(view, position, title, 2);
                }else if(mode == R.id.nav_recent){
                    popup(view, position, title, 1);
                }else if(mode == R.id.nav_download){
                    popup(view, position, title,3);
                }
            }

            @Override
            public void onItemClick(int position) {
                selectedPosition = position;
                Intent episodeView = episodeIntent(getContext(), titleAdapter.getItem(position));
                if(mode == R.id.nav_favorite) {
                    episodeView.putExtra("position", position);
                    episodeView.putExtra("favorite",true);
                    startActivityForResult(episodeView,1);
                }else if(mode == R.id.nav_recent) {
                    episodeView.putExtra("recent",true);
                    startActivityForResult(episodeView,2);
                }else if(mode == R.id.nav_download) {
                    episodeView.putExtra("online", false);
                    startActivity(episodeView);
                }
            }
        });
        if(savedInstanceState != null){
            mode = savedInstanceState.getInt("mode");
        }
        if(mode > -1) {
            loaded = true;
            changeMode(mode);
        }
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            if(titleAdapter != null && titleAdapter.getItemCount() > 0 && selectedPosition > -1) {
                switch (requestCode) {
                    case 1:
                        //favorite result
                        Boolean favorite_after = data.getBooleanExtra("favorite", true);
                        if (!favorite_after && titleAdapter != null && titleAdapter.getItemCount() > 0)
                            titleAdapter.remove(selectedPosition);
                        break;
                    case 2:
                        //recent result
                        if (titleAdapter != null && titleAdapter.getItemCount() > 0)
                            titleAdapter.moveItemToTop(selectedPosition);
                        break;

                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mode = -1;
        loaded = false;
    }

    public void changeMode(int id){
        mode = id;
        if(!loaded)
            return;
        recyclerView.scrollToPosition(0);
        if(searchView != null){
            searchView.clearFocus();
            searchView.setQuery("", false);
        }
        if(id == R.id.nav_recent){
            titleAdapter.setResume(true);
            titleAdapter.setForceThumbnail(false);
            titleAdapter.setData(p.getRecent());
        }else if(id == R.id.nav_favorite){
            titleAdapter.setResume(true);
            titleAdapter.setForceThumbnail(false);
            titleAdapter.setData(p.getFavorite());
        }else if(id == R.id.nav_download){
            titleAdapter.setResume(false);
            titleAdapter.setForceThumbnail(true);
            titleAdapter.clearData();
            new OfflineReader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    public class OfflineReader extends AsyncTask<Void,Void,Integer>{
        List<Title> titles;
        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            titleAdapter.addData(titles);
        }
        @Override
        protected Integer doInBackground(Void... voids) {
            titles = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE) {
                //scoped storage
                Uri uri = Uri.parse(p.getHomeDir());
                DocumentFile home;
                try {
                    home = DocumentFile.fromTreeUri(getContext(), uri);
                }catch (IllegalArgumentException e){
                    //home not set
                    return null;
                }
                if(home != null && home.canRead()){
                    for(DocumentFile f : home.listFiles()){
                        if(f.isDirectory()) {
                            DocumentFile d = f.findFile("title.gson");
                            if (d != null) {
                                try {
                                    Title title = new Gson().fromJson(readUriToString(getContext(), d.getUri()), new TypeToken<Title>() {
                                    }.getType());
                                    title.setPath(f.getUri().toString());
                                    if (title.getThumb().length() > 0) {
                                        DocumentFile t = f.findFile(title.getThumb());
                                        if (t.exists()) title.setThumb(t.getUri().toString());
                                    }
                                    titles.add(title);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Title title = new Title(f.getName(), "", "", new ArrayList<String>(), "", 0, MTitle.base_auto);
                                    title.setPath(f.getUri().toString());
                                    titles.add(title);
                                }
                            } else {
                                Title title = new Title(f.getName(), "", "", new ArrayList<String>(), "", 0, MTitle.base_auto);
                                title.setPath(f.getUri().toString());
                                titles.add(title);
                            }
                        }
                    }
                }

            }else {
                File homeDir = new File(p.getHomeDir());
                if (homeDir.exists()) {
                    File[] files = homeDir.listFiles();
                    for (File f : files) {
                        if (f.isDirectory()) {
                            File data = new File(f, "title.gson");
                            if (data.exists()) {
                                try {
                                    Title title = new Gson().fromJson(readFileToString(data), new TypeToken<Title>() {
                                    }.getType());
                                    title.setPath(f.getAbsolutePath());
                                    if (title.getThumb().length() > 0)
                                        title.setThumb(f.getAbsolutePath() + '/' + title.getThumb());
                                    titles.add(title);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Title title = new Title(f.getName(), "", "", new ArrayList<String>(), "", 0, MTitle.base_auto);
                                    title.setPath(f.getAbsolutePath());
                                    titles.add(title);
                                }

                            } else {
                                Title title = new Title(f.getName(), "", "", new ArrayList<String>(), "", 0, MTitle.base_auto);
                                title.setPath(f.getAbsolutePath());
                                titles.add(title);
                            }
                        }
                    }
                    //add titles to adapter
                }
            }
            return null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search_menu, menu);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.filter_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint("검색");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                titleAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                titleAdapter.getFilter().filter(query);
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.filter_search) {
            return true;
        }
        return false;
    }

    void openViewer(Manga manga, int code){
        Intent viewer = viewerIntent(getContext(),manga);
        viewer.putExtra("online",true);
        startActivityForResult(viewer, code);
    }

    void popup(View view, final int position, final Title title, final int m){
        PopupMenu popup = new PopupMenu(getContext(), view);
        //Inflating the Popup using xml file
        //todo: clean this part
        popup.getMenuInflater()
                .inflate(R.menu.title_options, popup.getMenu());
        switch(m){
            case 1:
                //최근
                popup.getMenu().findItem(R.id.del).setVisible(true);
            case 0:
                //검색
                popup.getMenu().findItem(R.id.favAdd).setVisible(true);
                popup.getMenu().findItem(R.id.favDel).setVisible(true);
                break;
            case 2:
                //좋아요
                popup.getMenu().findItem(R.id.favDel).setVisible(true);
                break;
            case 3:
                //저장됨
                popup.getMenu().findItem(R.id.favAdd).setVisible(true);
                popup.getMenu().findItem(R.id.favDel).setVisible(true);
                popup.getMenu().findItem(R.id.remove).setVisible(true);
                break;
        }
        //좋아요 추가/제거 중 하나만 남김
        if(m!=2) {
            if (p.findFavorite(title) > -1) popup.getMenu().removeItem(R.id.favAdd);
            else popup.getMenu().removeItem(R.id.favDel);
        }

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch(item.getItemId()){
                    case R.id.del:
                        //delete (only in recent)
                        titleAdapter.remove(position);
                        p.removeRecent(position);
                        break;
                    case R.id.favAdd:
                    case R.id.favDel:
                        //toggle favorite
                        p.toggleFavorite(title,0);
                        if(m==2){
                            titleAdapter.remove(position);
                        }
                        break;
                    case R.id.remove:
                        //저장된 만화에서 삭제
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(which == DialogInterface.BUTTON_POSITIVE){
                                        //Yes button clicked
                                        if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE) {
                                            DocumentFile f = DocumentFile.fromTreeUri(getContext(), Uri.parse(p.getHomeDir()));
                                            DocumentFile target = f.findFile(title.getName());
                                            if(target != null && target.delete()){
                                                titleAdapter.remove(position);
                                                Toast.makeText(getContext(), "삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                            }else showPopup(getContext(), "알림", "삭제를 실패했습니다");
                                        }else {
                                            File folder = new File(p.getHomeDir(), filterFolder(title.getName()));
                                            if (deleteRecursive(folder)) {
                                                titleAdapter.remove(position);
                                                Toast.makeText(getContext(), "삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                            } else showPopup(getContext(), "알림", "삭제를 실패했습니다");
                                        }
                                }
                            }
                        };
                        AlertDialog.Builder builder;
                        if(p.getDarkTheme()) builder = new AlertDialog.Builder(getContext(),R.style.darkDialog);
                        else builder = new AlertDialog.Builder(getContext());
                        builder.setMessage("정말로 삭제 하시겠습니까?").setPositiveButton("네", dialogClickListener)
                                .setNegativeButton("아니오", dialogClickListener).show();
                        break;
                }
                return false;
            }
        });
        popup.show(); //showing popup menu
    }
}

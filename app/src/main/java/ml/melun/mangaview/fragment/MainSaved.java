package ml.melun.mangaview.fragment;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.deleteRecursive;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.filterFolder;
import static ml.melun.mangaview.Utils.popup;
import static ml.melun.mangaview.Utils.readFileToString;
import static ml.melun.mangaview.Utils.showPopup;

public class MainSaved extends MainActivityFragment{
    TitleAdapter offlineAdapter;
    RecyclerView savedList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_download , container, false);
        //downloaded list
        offlineAdapter = new TitleAdapter(getContext(), false);
        offlineAdapter.noResume();
        savedList = rootView.findViewById(R.id.savedList);
        savedList.setLayoutManager(new LinearLayoutManager(getContext()));
        savedList.setAdapter(offlineAdapter);
        offlineAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent i = episodeIntent(getContext(), offlineAdapter.getItem(position));
                i.putExtra("online", false);
                startActivity(i);
            }

            @Override
            public void onLongClick(View view, int position) {
                Title title = offlineAdapter.getItem(position);
                popup(getContext(), view, position, title,3, new PopupMenu.OnMenuItemClickListener(){
                    public boolean onMenuItemClick(MenuItem item) {
                        switch(item.getItemId()){
                            case R.id.favAdd:
                            case R.id.favDel:
                                //toggle favorite
                                p.toggleFavorite(title,0);
                                break;
                            case R.id.remove:
                                //저장된 만화에서 삭제
                                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which){
                                            case DialogInterface.BUTTON_POSITIVE:
                                                //Yes button clicked
                                                File folder = new File(p.getHomeDir(), filterFolder(title.getName()));
                                                if(deleteRecursive(folder)) {
                                                    offlineAdapter.remove(position);
                                                    Toast.makeText(getContext(),"삭제가 완료되었습니다.",Toast.LENGTH_SHORT).show();
                                                }
                                                else showPopup(getContext(), "알림","삭제를 실패했습니다");
                                                break;
                                            case DialogInterface.BUTTON_NEGATIVE:
                                                //No button clicked
                                                break;
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
                }, p);
            }

            @Override
            public void onResumeClick(int position, int id) {
                //not used
            }
        });


        return rootView;
    }

    @Override
    public void postDrawerJob() {
        new AsyncTask<Void, Void, Integer>() {
            List<Title> titles;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                offlineAdapter.addData(titles);
            }

            @Override
            protected Integer doInBackground(Void... voids) {
                titles = new ArrayList<>();
                List<Title> savedTitles = new ArrayList<>();
                File homeDir = new File(p.getHomeDir());
                if (homeDir.exists()) {
                    File[] files = homeDir.listFiles();
                    for (File f : files) {
                        if (f.isDirectory()) {
                            File oldData = new File(f, "title.data");
                            File data = new File(f, "title.gson");
                            if (oldData.exists()) {
                                try {
                                    JSONObject json = new JSONObject(readFileToString(oldData));
                                    Title title = new Gson().fromJson(json.getJSONObject("title").toString(), new TypeToken<Title>() {
                                    }.getType());
                                    if (title.getThumb().length() > 0)
                                        title.setThumb(new File(f.getAbsolutePath(), title.getThumb()).getAbsolutePath());
                                    titles.add(title);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    titles.add(new Title(f.getName(), "", "", new ArrayList<String>(), -1, 0));
                                }
                            } else if (data.exists()) {
                                try {
                                    Title title = new Gson().fromJson(readFileToString(data), new TypeToken<Title>() {
                                    }.getType());
                                    if (title.getThumb().length() > 0)
                                        title.setThumb(f.getAbsolutePath() + '/' + title.getThumb());
                                    titles.add(title);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    titles.add(new Title(f.getName(), "", "", new ArrayList<String>(), -1, 0));
                                }

                            } else
                                titles.add(new Title(f.getName(), "", "", new ArrayList<String>(), -1, 0));
                        }
                    }
                    //add titles to adapter
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search_menu, menu);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.filter_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                offlineAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                offlineAdapter.getFilter().filter(query);
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
}

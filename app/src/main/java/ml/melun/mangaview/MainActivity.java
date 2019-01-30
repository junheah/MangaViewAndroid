package ml.melun.mangaview;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import ml.melun.mangaview.adapter.OfflineTitleApapter;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.adapter.mainAdapter;
import ml.melun.mangaview.mangaview.MainPage;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Preference p;
    int startTab;
    //variables
    private ViewFlipper contentHolder;
    FloatingActionButton advSearchBtn;
    TextView noresult;
    private EditText searchBox;
    public Context context = this;
    ProgressDialog pd;
    Search search;
    TitleAdapter searchAdapter, recentAdapter, favoriteAdapter;
    RecyclerView searchResult, recentResult, favoriteResult, savedList, mainRecycler;
    private int version;
    int mode = 0;
    int selectedPosition=-1;
    Downloader downloader;
    TextView statNo, statName, stat;
    int dlstatus=0;
    int dlProgress;
    ProgressBar dlBar;
    ConstraintLayout dlStatContainer;
    MenuItem versionItem;
    String homeDirStr;
    SwipyRefreshLayout swipe;
    Boolean dark;
    Intent viewer;
    Spinner searchMode;
    NavigationView navigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
        p.init(this);
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDarkNoTitle);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // toolbar.getRootView().setBackgroundColor(getResources().getColor(R.color.colorDark));

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        if(dark) {
            int[][] states = new int[][]{
                    new int[]{-android.R.attr.state_enabled}, // disabled
                    new int[]{android.R.attr.state_enabled}, // enabled
                    new int[]{-android.R.attr.state_checked}, // unchecked
                    new int[]{android.R.attr.state_pressed}  // pressed
            };

            int[] colors = new int[]{
                    Color.parseColor("#565656"),
                    Color.parseColor("#a2a2a2"),
                    Color.WHITE,
                    Color.WHITE
            };
            ColorStateList colorStateList = new ColorStateList(states, colors);
            navigationView.setItemTextColor(colorStateList);
        }

        //custom var init starts here
        contentHolder = this.findViewById(R.id.contentHolder);
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //SharedPreferences preferences = getSharedPreferences("mangaView",MODE_PRIVATE);

        homeDirStr = p.getHomeDir();
        dlStatContainer = findViewById(R.id.statusContainter);
        downloader = new Downloader(this);

        versionItem = navigationView.getMenu().findItem(R.id.nav_version_display);
        versionItem.setTitle("v."+version);
        swipe = this.findViewById(R.id.searchSwipe);
        //check for permission
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(permissionCheck== PackageManager.PERMISSION_DENIED){
            // 권한 없음
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{READ_EXTERNAL_STORAGE,WRITE_EXTERNAL_STORAGE},
                        132322);
            }
        }else{
            //
        }

        stat = findViewById(R.id.statusText);
        statNo = findViewById(R.id.statusNo);
        statName = findViewById(R.id.statusTitle);
        dlBar = findViewById(R.id.statusProgress);
        savedList = findViewById(R.id.savedList);
        //show downloaded manga list
        downloader.addListener(new Downloader.Listener() {
            @Override
            public void changeNameStr(final String name) {
                statName.post(new Runnable() {
                    public void run() {
                        statName.setText(name);
                    }
                });
            }

            @Override
            public void changeNo(final int n) {
                statNo.post(new Runnable() {
                    public void run() {
                        statNo.setText(n+" in queue");
                    }
                });
            }

            @Override
            public void processStatus(int s) {
                if(dlstatus!=s){
                    dlstatus = s;
                    switch(s) {
                        case 0:
                            stat.post(new Runnable() {
                                public void run() {
                                    stat.setText("idle");
                                    Toast.makeText(getApplication(),"모든 다운로드가 완료되었습니다.", Toast.LENGTH_LONG).show();
                                    dlStatContainer.setVisibility(View.GONE);
                                }
                            });
                            //collapse download status container
                            break;
                        case 1:
                            stat.post(new Runnable() {
                                public void run() {
                                    dlStatContainer.setVisibility(View.VISIBLE);
                                    stat.setText("downloading");
                                }
                            });
                            //open download status container
                            break;
                    }
                }
            }

            @Override
            public void setProgress(int p) {
                if(dlProgress!=p){
                    dlProgress = p;
                    dlBar.post(new Runnable() {
                        @Override
                        public void run() {
                            dlBar.setProgress(dlProgress);
                        }
                    });
                }
            }
        });

        //set startTab and refresh views
        startTab = p.getStartTab();
        contentHolder.setDisplayedChild(startTab);
        refreshViews(getTabId(startTab));
        navigationView.getMenu().getItem(startTab).setChecked(true);

        //check update upon startup
        updateCheck u = new updateCheck();
        u.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public int getTabId(int i){
        switch(i){
            case 0:
                return(R.id.nav_main);
            case 1:
                return(R.id.nav_search);
            case 2:
                return(R.id.nav_recent);
            case 3:
                return(R.id.nav_favorite);
            case 4:
                return(R.id.nav_download);
        }
        return 0;
    }



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(contentHolder.getDisplayedChild()==startTab){
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                MainActivity.super.onBackPressed();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder;
                if(dark) builder = new AlertDialog.Builder(this,R.style.darkDialog);
                else builder = new AlertDialog.Builder(this);
                builder.setMessage("정말로 종료 하시겠습니까?").setPositiveButton("네", dialogClickListener)
                        .setNegativeButton("아니오", dialogClickListener).show();
            }else{
                contentHolder.setDisplayedChild(startTab);
                navigationView.getMenu().getItem(startTab).setChecked(true);
                refreshViews(getTabId(startTab));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingIntent = new Intent(context, SettingsActivity.class);
            startActivity(settingIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_main) {
            // Handle the main action
            contentHolder.setDisplayedChild(0);
            mode = 0;
        } else if (id == R.id.nav_search) {
            // Handle the search action
            contentHolder.setDisplayedChild(1);
            mode = 1;
        }else if(id==R.id.nav_recent) {
            // Handle the recent action
            contentHolder.setDisplayedChild(2);
            mode = 2;
        }else if(id==R.id.nav_favorite) {
            // Handle the favorite action
            contentHolder.setDisplayedChild(3);
            mode = 3;
        }else if(id==R.id.nav_download){
            contentHolder.setDisplayedChild(4);
            mode = 4;
        }else{
            //don't refresh views
            if(id==R.id.nav_update){
                //check update
                updateCheck u = new updateCheck();
                u.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }else if(id==R.id.nav_kakao){
                Toast.makeText(getApplicationContext(), "오픈톡방에 참가합니다.", Toast.LENGTH_LONG).show();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.kakao.com/o/gL4yY57"));
                startActivity(browserIntent);
            }else if(id==R.id.nav_settings){
                Intent settingIntent = new Intent(context, SettingsActivity.class);
                startActivity(settingIntent);
                return true;
            }
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }
        refreshViews(id);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void refreshViews(int id){
        //set views according to selected layout
        if(id==R.id.nav_main){
            //main content
            // 최근 추가된 만화
            mainRecycler = this.findViewById(R.id.main_recycler);
            mainAdapter mainadapter = new mainAdapter(context);
            mainRecycler.setLayoutManager(new LinearLayoutManager(context));
            mainRecycler.setAdapter(mainadapter);
            mainadapter.setMainClickListener(new mainAdapter.onItemClick() {
                @Override
                public void clickedManga(Manga m) {
                    //get title from manga m and start intent for manga m
                    //getTitleFromManga intentStarter = new getTitleFromManga();
                    //intentStarter.execute(m);

                    if(p.getScrollViewer()) viewer = new Intent(context, ViewerActivity.class);
                    else viewer = new Intent(context, ViewerActivity2.class);
                    viewer.putExtra("name",m.getName());
                    viewer.putExtra("id",m.getId());
                    startActivity(viewer);
                }

                @Override
                public void clickedTag(String t) {
                    System.out.println(t);
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("query",t);
                    i.putExtra("mode",2);
                    startActivity(i);
                }

                @Override
                public void clickedName(int t) {
                    System.out.println(t);
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("query",t+"");
                    i.putExtra("mode",3);
                    startActivity(i);
                }

                @Override
                public void clickedRelease(int t) {
                    System.out.println(t);
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("query",t+"");
                    i.putExtra("mode",4);
                    startActivity(i);
                }

                @Override
                public void clickedMoreUpdated() {
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("mode",5);
                    startActivity(i);
                }
            });


        }else if(id==R.id.nav_search){
            //search content
            noresult = this.findViewById(R.id.noResult);
            searchBox = this.findViewById(R.id.searchBox);
            searchResult = this.findViewById(R.id.searchResult);
            searchResult.setLayoutManager(new LinearLayoutManager(this));
            searchMode = this.findViewById(R.id.searchMode);
            advSearchBtn = this.findViewById(R.id.advSearchBtn);
            if(dark) searchMode.setPopupBackgroundResource(R.color.colorDarkWindowBackground);

            advSearchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent advSearch = new Intent(context, AdvSearchActivity.class);
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
                            else searchAdapter = new TitleAdapter(context);
                            search = new Search(query,searchMode.getSelectedItemPosition());
                            searchManga sm = new searchManga();
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
                            searchManga sm = new searchManga();
                            sm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        } else swipe.setRefreshing(false);
                    }
                }
            });
        }else if(id==R.id.nav_recent){
            recentResult = this.findViewById(R.id.recentList);
            recentAdapter = new TitleAdapter(context);
            recentAdapter.addData(p.getRecent());
            recentResult.setLayoutManager(new LinearLayoutManager(this));
            recentResult.setAdapter(recentAdapter);
            recentAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    // start intent : Episode viewer
                    Title selected = recentAdapter.getItem(position);
                    selectedPosition = position;
                    p.addRecent(selected);
                    Intent episodeView = new Intent(context, EpisodeActivity.class);
                    episodeView.putExtra("title", selected.getName());
                    episodeView.putExtra("thumb",selected.getThumb());
                    episodeView.putExtra("author",selected.getAuthor());
                    episodeView.putExtra("tags",new ArrayList<String>(selected.getTags()));
                    episodeView.putExtra("release",selected.getRelease());
                    episodeView.putExtra("recent",true);
                    startActivityForResult(episodeView,2);
                }
            });
        }else if(id==R.id.nav_favorite){
            favoriteResult = this.findViewById(R.id.favoriteList);
            favoriteAdapter = new TitleAdapter(context);
            favoriteAdapter.addData(p.getFavorite());
            favoriteResult.setLayoutManager(new LinearLayoutManager(this));
            favoriteResult.setAdapter(favoriteAdapter);
            favoriteAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    // start intent : Episode viewer
                    Title selected = favoriteAdapter.getItem(position);
                    p.addRecent(selected);
                    //start intent for result : has to know if favorite has been removed or not
                    Intent episodeView = new Intent(context, EpisodeActivity.class);
                    episodeView.putExtra("position", position);
                    episodeView.putExtra("title", selected.getName());
                    episodeView.putExtra("author",selected.getAuthor());
                    episodeView.putExtra("tags",new ArrayList<String>(selected.getTags()));
                    episodeView.putExtra("thumb",selected.getThumb());
                    episodeView.putExtra("favorite",true);
                    selectedPosition = position;
                    startActivityForResult(episodeView,1);
                }
            });
        }else if(id==R.id.nav_download){
            //downloaded list
            //그냥 코드 개더러워져도 액티비티 한개로 다할거임.. 귀찮고 이미 더러움...
            //원래 viewFlipper 도 비효율적이라 바꿔야지 했는데 이미 늦음
            System.out.println(getSavedTitles());
            final OfflineTitleApapter offAdapter = new OfflineTitleApapter(context,getSavedTitles());
            savedList.setLayoutManager(new LinearLayoutManager(this));
            savedList.setAdapter(offAdapter);
            offAdapter.setClickListener(new OfflineTitleApapter.ItemClickListener() {
                @Override
                public void onItemClick(View v, int position) {
                    Intent i = new Intent(context, OfflineEpisodeActivity.class);
                    i.putExtra("title",offAdapter.getItem(position));
                    i.putExtra("homeDir",homeDirStr);
                    startActivity(i);
                }
            });

        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case 1:
                    //favorite result
                    Boolean favorite_after = data.getBooleanExtra("favorite",true);
                    if(!favorite_after) favoriteAdapter.notifyItemRemoved(selectedPosition);
                    break;
                case 2:
                    //recent result
                    recentAdapter.moveItemToTop(selectedPosition);
                    for(int i= selectedPosition; i>0; i--){
                        recentAdapter.notifyItemMoved(i,i-1);
                    }
                    break;

            }
        }
    }

    private class searchManga extends AsyncTask<String,String,String>{
        protected void onPreExecute(){
            super.onPreExecute();
        }
        protected String doInBackground(String... params){
            search.fetch();
            return null;
        }
        @Override
        protected void onPostExecute(String res){
            super.onPostExecute(res);
            if(searchAdapter.getItemCount()==0) {
                searchAdapter.addData(search.getResult());
                searchResult.setAdapter(searchAdapter);
                searchAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        // start intent : Episode viewer
                        Title selected = searchAdapter.getItem(position);
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
    public ArrayList<String> getSavedTitles(){
        homeDirStr = p.getHomeDir();
        ArrayList<String> savedTitles = new ArrayList<>();
        File homeDir = new File(homeDirStr);
        if(homeDir.exists()){
            File[] files = homeDir.listFiles();
            for(File f:files){
                if(f.isDirectory()) savedTitles.add(f.getName());
            }
        }
        return savedTitles;
    }

    public String httpsGet(String urlin){
        HttpsURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlin);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Encoding", "*");
            connection.setRequestProperty("Accept", "*");
            connection.connect();
            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private class updateCheck extends AsyncTask<Void, Integer, Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            if(dark) pd = new ProgressDialog(MainActivity.this, R.style.darkDialog);
            else pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("업데이트 확인중");
            pd.setCancelable(false);
            pd.show();
        }

        protected Integer doInBackground(Void... params) {
            try {
                String rawdata = httpsGet("https://github.com/junheah/MangaViewAndroid/raw/master/version.json");
                JSONObject data = new JSONObject(rawdata);
                int lver = data.getInt("version");
                String link = data.getString("link");
                if(version<lver){
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(browserIntent);
                    return 1;
                }
            }catch(Exception e){
                return -1;
            }return 0;
        }
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            switch(result){
                case -1:
                    Toast.makeText(getApplicationContext(), "오류가 발생했습니다. 나중에 다시 시도해 주세요.", Toast.LENGTH_LONG).show();
                    break;
                case 0:
                    Toast.makeText(getApplicationContext(), "최신버전 입니다.", Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    Toast.makeText(getApplicationContext(), "새로운 버전을 찾았습니다. 다운로드 페이지로 이동합니다.", Toast.LENGTH_LONG).show();
                    break;
            }
            if (pd.isShowing()){
                pd.dismiss();
            }
        }
    }

    public void checkNew(){


    }



}

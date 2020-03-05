package ml.melun.mangaview.activity;

import android.Manifest;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ml.melun.mangaview.CheckInfo;
import ml.melun.mangaview.Downloader;
import ml.melun.mangaview.R;
import ml.melun.mangaview.UrlUpdater;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.adapter.MainAdapter;
import ml.melun.mangaview.mangaview.MTitle;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static ml.melun.mangaview.Downloader.BROADCAST_STOP;
import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.deleteRecursive;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.filterFolder;
import static ml.melun.mangaview.Utils.readFileToString;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.Utils.viewerIntent;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;
import static ml.melun.mangaview.activity.FirstTimeActivity.RESULT_EULA_AGREE;
import static ml.melun.mangaview.activity.SettingsActivity.RESULT_NEED_RESTART;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    int startTab;
    //variables
    private ViewFlipper contentHolder;
    FloatingActionButton advSearchBtn;
    TextView noresult;
    private EditText searchBox;
    private Context context = this;
    ProgressDialog pd;
    Search search;
    TitleAdapter searchAdapter, recentAdapter, favoriteAdapter, offlineAdapter;
    RecyclerView searchResult, recentResult, favoriteResult, savedList, mainRecycler;
    int selectedPosition=-1;
    MenuItem versionItem;
    String homeDirStr;
    SwipyRefreshLayout swipe;
    Boolean dark;
    Intent viewer;
    Spinner searchMode;
    NavigationView navigationView;
    NotificationManagerCompat notificationManagerc;
    NotificationManager notificationManager;
    Toolbar toolbar;
    MainAdapter mainadapter;
    private static final int FIRST_TIME_ACTIVITY = 9;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDarkNoTitle);
        else setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);


        if(!p.getSharedPref().getBoolean("eula",false)){
            startActivityForResult(new Intent(context, FirstTimeActivity.class), FIRST_TIME_ACTIVITY);
        }else {
            if(p.getAutoUrl())
                new UrlUpdater(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            activityInit();
        }

//        //captcha test
//        Intent captchaIntent = new Intent(context, CaptchaActivity.class);
//        startActivity(captchaIntent);

    }
    private void activityInit(){
        setContentView(R.layout.activity_main);
        //check prefs
        if(!p.check()){
            //popup to fix preferences
            System.out.println("needs fix");

            AlertDialog.Builder builder;
            if (p.getDarkTheme()) builder = new AlertDialog.Builder(context, R.style.darkDialog);
            else builder = new AlertDialog.Builder(context);
            builder.setTitle("기록 업데이트 필요")
                    .setCancelable(false)
                    .setMessage("저장된 데이터에서 더이상 지원되지 않는 이전 형식이 발견되었습니다. 정상적인 사용을 위해 업데이트가 필요합니다. 데이터를 업데이트 하시겠습니까?")
                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //update data
                            new DataUpdater().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    })
                    .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //reset pref
                            showPopup(context,"알림","예기치 못한 오류가 발생할 수 있습니다. 앱의 데이터를 초기화 하거나 데이터 업데이트를 진행 해 주세요.");
                        }
                    })
                    .show();
        }
        // url updater


        toolbar = (Toolbar) findViewById(R.id.toolbar);
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

        contentHolder = this.findViewById(R.id.contentHolder);
        //SharedPreferences preferences = getSharedPreferences("mangaView",MODE_PRIVATE);

        homeDirStr = p.getHomeDir();

        versionItem = navigationView.getMenu().findItem(R.id.nav_version_display);
        int version = 0;
        try{
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionCode;
        }catch (Exception e){

        }
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
        savedList = findViewById(R.id.savedList);
        //show downloaded manga list
        //set startTab and refresh views
        startTab = p.getStartTab();
        contentHolder.setDisplayedChild(startTab);
        refreshViews(getTabId(startTab));
        getSupportActionBar().setTitle(navigationView.getMenu().findItem(getTabId(startTab)).getTitle());
        navigationView.getMenu().getItem(startTab).setChecked(true);

        //check for update, notices
        new CheckInfo(context,httpClient).all(false);
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

                                //block interactivity
                                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                                if(Downloader.running){
                                    //downloader is running
                                    //show info prompt
                                    findViewById(R.id.waiting_panel).setVisibility(View.VISIBLE);

                                    //stop downloader service
                                    Intent downloader = new Intent(getApplicationContext(),Downloader.class);
                                    downloader.setAction(Downloader.ACTION_FORCE_STOP);
                                    if (Build.VERSION.SDK_INT >= 26) {
                                        startForegroundService(downloader);
                                    }else{
                                        startService(downloader);
                                    }

                                    //broadcast receiver
                                    BroadcastReceiver statusReceiver = new BroadcastReceiver() {
                                        @Override
                                        public void onReceive(Context context, Intent intent) {
                                            if(intent.getAction().matches(BROADCAST_STOP)){
                                                //service stopped
                                                finishAffinity();
                                                System.runFinalization();
                                                System.exit(0);
                                            }
                                        }
                                    };
                                    IntentFilter infil = new IntentFilter();
                                    infil.addAction(BROADCAST_STOP);
                                    registerReceiver(statusReceiver, infil);

                                }else{
                                    //kill application
                                    finishAffinity();
                                    System.runFinalization();
                                    System.exit(0);
                                }
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
                builder.setMessage(Downloader.running ? "다운로드가 진행중입니다. 정말로 종료 하시겠습니까?" : "정말로 종료 하시겠습니까?")
                        .setPositiveButton("네", dialogClickListener)
                        .setNegativeButton("아니오", dialogClickListener)
                        .show();
            }else{
                contentHolder.setDisplayedChild(startTab);
                navigationView.getMenu().getItem(startTab).setChecked(true);
                toolbar.setTitle(navigationView.getMenu().findItem(getTabId(startTab)).getTitle());
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
            startActivityForResult(settingIntent, 0);
            return true;
        }else if(id == R.id.action_debug){
            Intent debug = new Intent(context, DebugActivity.class);
            startActivity(debug);
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
        }else if (id == R.id.nav_search) {
            // Handle the search action
            contentHolder.setDisplayedChild(1);
        }else if(id==R.id.nav_recent) {
            // Handle the recent action
            contentHolder.setDisplayedChild(2);
        }else if(id==R.id.nav_favorite) {
            // Handle the favorite action
            contentHolder.setDisplayedChild(3);
        }else if(id==R.id.nav_download){
            contentHolder.setDisplayedChild(4);
        }else{
            //don't refresh views
            if(id==R.id.nav_update) {
                //check update
                new CheckInfo(context,httpClient).all(true);
            }else if(id==R.id.nav_notice){
                Intent noticesIntent = new Intent(context, NoticesActivity.class);
                startActivity(noticesIntent);
                return true;
            }else if(id==R.id.nav_kakao){

                View layout = getLayoutInflater().inflate(R.layout.content_kakao_popup, null);
                layout.findViewById(R.id.kakao_notice).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.kakao_notice))));
                    }
                });
                layout.findViewById(R.id.kakao_chat).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.kakao_chat))));
                    }
                });
                layout.findViewById(R.id.kakao_direct).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.kakao_direct))));
                    }
                });

                AlertDialog.Builder builder;
                if(dark) builder = new AlertDialog.Builder(context,R.style.darkDialog);
                else builder = new AlertDialog.Builder(context);
                builder.setTitle("오픈 카톡 참가")
                        .setView(layout)
                        .show();

//                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://open.kakao.com/o/gL4yY57"));
//                startActivity(browserIntent);
            }else if(id==R.id.nav_settings){
                Intent settingIntent = new Intent(context, SettingsActivity.class);
                startActivityForResult(settingIntent, 0);
                return true;
            }else if(id==R.id.nav_donate){
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://junheah.github.io/donate"));
                startActivity(browserIntent);
            }else if(id==R.id.nav_account){
                startActivity(new Intent(context, LoginActivity.class));
                return true;
            }
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }
        toolbar.setTitle(item.getTitle());
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
            mainadapter = new MainAdapter(context);
            mainRecycler.setLayoutManager(new LinearLayoutManager(context));
            mainRecycler.setAdapter(mainadapter);
            mainadapter.setMainClickListener(new MainAdapter.onItemClick() {
                @Override
                public void clickedManga(Manga m) {
                    //mget title from manga m and start intent for manga m
                    //getTitleFromManga intentStarter = new getTitleFromManga();
                    //intentStarter.execute(m);
                    openViewer(m,-1);
                }

                @Override
                public void clickedTag(String t) {
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("query",t);
                    i.putExtra("mode",2);
                    startActivity(i);
                }

                @Override
                public void clickedName(int t) {
                    Intent i = new Intent(context, TagSearchActivity.class);
                    i.putExtra("query",t+"");
                    i.putExtra("mode",3);
                    startActivity(i);
                }

                @Override
                public void clickedRelease(int t) {
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
                public void onLongClick(View view, int position) {
                    //longclick
                    popup(view, position, recentAdapter.getItem(position), 1);
                }

                @Override
                public void onResumeClick(int position, int id) {
                    selectedPosition = position;
                    openViewer(new Manga(id,"",""),2);
                }

                @Override
                public void onItemClick(int position) {
                    // start intent : Episode viewer
                    selectedPosition = position;
                    Intent episodeView = episodeIntent(context, recentAdapter.getItem(position));
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
                public void onResumeClick(int position, int id) {
                    openViewer(new Manga(id,"",""),-1);
                }

                @Override
                public void onLongClick(View view, int position) {
                    popup(view, position, favoriteAdapter.getItem(position), 2);
                }

                @Override
                public void onItemClick(int position) {
                    // start intent : Episode viewer
                    //start intent for result : has to know if favorite has been removed or not
                    Intent episodeView = episodeIntent(context,favoriteAdapter.getItem(position));
                    episodeView.putExtra("position", position);
                    episodeView.putExtra("favorite",true);
                    selectedPosition = position;
                    startActivityForResult(episodeView,1);
                }
            });
        }else if(id==R.id.nav_download){
            //downloaded list
            //그냥 코드 개더러워져도 액티비티 한개로 다할거임.. 귀찮고 이미 더러움...
            //원래 viewFlipper 도 비효율적이라 바꿔야지 했는데 이미 늦음
            //todo: viewflipper 갖다 버리고 fragment 사용하기
            offlineAdapter = new TitleAdapter(context, false);
            offlineAdapter.noResume();
            savedList.setLayoutManager(new LinearLayoutManager(this));
            savedList.setAdapter(offlineAdapter);
            offlineAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    Intent i = episodeIntent(context, offlineAdapter.getItem(position));
                    i.putExtra("online", false);
                    startActivity(i);
                }

                @Override
                public void onLongClick(View view, int position) {
                    popup(view, position, offlineAdapter.getItem(position),3);
                }

                @Override
                public void onResumeClick(int position, int id) {
                    //not used
                }
            });
            GetSavedTitles get = new GetSavedTitles();
            get.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == FIRST_TIME_ACTIVITY){
            if(resultCode == RESULT_EULA_AGREE) {
                activityInit();
            }else
                finish();
            return;
        }
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case 1:
                    //favorite result
                    Boolean favorite_after = data.getBooleanExtra("favorite",true);
                    if(!favorite_after && favoriteAdapter != null && favoriteAdapter.getItemCount()>0)
                        favoriteAdapter.remove(selectedPosition);
                    break;
                case 2:
                    //recent result
                    if(recentAdapter != null && recentAdapter.getItemCount()>0)
                        recentAdapter.moveItemToTop(selectedPosition);
                    break;

            }
        }else if(resultCode == RESULT_CAPTCHA){
            switch(requestCode){
                case 3:
                    //main page
                    if(mainadapter!=null)
                        mainadapter.fetch();
                    break;
                case 4:
                    //search
                    
                    break;
            }
        }else if(resultCode == RESULT_NEED_RESTART){
            System.out.println("ppppppppppppppppppppp");
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    private class searchManga extends AsyncTask<String,String,Integer>{
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
                Utils.showCaptchaPopup(context, 4);
            }

            if(searchAdapter.getItemCount()==0) {
                searchAdapter.addData(search.getResult());
                searchResult.setAdapter(searchAdapter);
                searchAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
                    @Override
                    public void onLongClick(View view, int position) {
                        //none
                        popup(view, position, searchAdapter.getItem(position), 0);
                    }

                    @Override
                    public void onResumeClick(int position, int id) {
                        openViewer(new Manga(id,"",""),-1);
                    }

                    @Override
                    public void onItemClick(int position) {
                        // start intent : Episode viewer
                        Intent episodeView = episodeIntent(context, searchAdapter.getItem(position));
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


    private class GetSavedTitles extends AsyncTask<Void, Void, Integer>{
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
            homeDirStr = p.getHomeDir();
            List<Title> savedTitles = new ArrayList<>();
            File homeDir = new File(homeDirStr);
            if(homeDir.exists()){
                File[] files = homeDir.listFiles();
                for(File f:files){
                    if(f.isDirectory()){
                        File oldData = new File(f,"title.data");
                        File data = new File(f,"title.gson");
                        if(oldData.exists()){
                            try {
                                JSONObject json = new JSONObject(readFileToString(oldData));
                                Title title = new Gson().fromJson(json.getJSONObject("title").toString(),new TypeToken<Title>(){}.getType());
                                if(title.getThumb().length()>0) title.setThumb(new File(f.getAbsolutePath(),title.getThumb()).getAbsolutePath());
                                titles.add(title);
                            }catch (Exception e){
                                e.printStackTrace();
                                titles.add(new Title(f.getName(),"","",new ArrayList<String>(),-1, 0));
                            }
                        }else if(data.exists()){
                            try {
                                Title title = new Gson().fromJson(readFileToString(data),new TypeToken<Title>(){}.getType());
                                if(title.getThumb().length()>0) title.setThumb(f.getAbsolutePath()+'/'+title.getThumb());
                                titles.add(title);
                            }catch (Exception e){
                                e.printStackTrace();
                                titles.add(new Title(f.getName(),"","",new ArrayList<String>(),-1, 0));
                            }

                        } else titles.add(new Title(f.getName(),"","",new ArrayList<String>(),-1, 0));
                    }
                }
                //add titles to adapter
            }
            return null;
        }
    }

    void checkNew(){
        //favorite adapter
        //todo: 만화 업데이트 확인
    }

    void popup(View view, final int position, final Title title, final int m){
        PopupMenu popup = new PopupMenu(MainActivity.this, view);
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
                        recentAdapter.remove(position);
                        p.removeRecent(position);
                        break;
                    case R.id.favAdd:
                    case R.id.favDel:
                        //toggle favorite
                        p.toggleFavorite(title,0);
                        if(m==2){
                            favoriteAdapter.remove(position);
                        }
                        break;
                    case R.id.remove:
                        //저장된 만화에서 삭제
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case DialogInterface.BUTTON_POSITIVE:
                                        //Yes button clicked
                                        File folder = new File(homeDirStr, filterFolder(title.getName()));
                                        if(deleteRecursive(folder)) {
                                            offlineAdapter.remove(position);
                                            Toast.makeText(context,"삭제가 완료되었습니다.",Toast.LENGTH_SHORT).show();
                                        }
                                        else showPopup(context, "알림","삭제를 실패했습니다");
                                        break;
                                    case DialogInterface.BUTTON_NEGATIVE:
                                        //No button clicked
                                        break;
                                }
                            }
                        };
                        AlertDialog.Builder builder;
                        if(dark) builder = new AlertDialog.Builder(context,R.style.darkDialog);
                        else builder = new AlertDialog.Builder(context);
                        builder.setMessage("정말로 삭제 하시겠습니까?").setPositiveButton("네", dialogClickListener)
                                .setNegativeButton("아니오", dialogClickListener).show();
                        break;
                }
                return false;
            }
        });
        popup.show(); //showing popup menu
    }

    public void openViewer(Manga manga, int code){
        Intent viewer = viewerIntent(context,manga);
        viewer.putExtra("online",true);
        startActivityForResult(viewer, code);
    }


    private class DataUpdater extends AsyncTask<Void,Void,Integer>{
        ProgressDialog pd;
        int sum = 0;
        int current = 0;
        @Override
        protected void onProgressUpdate(Void... values) {
            pd.setMessage(current +" / " + sum+"\n 앱을 종료하지 말아주세요.");
        }
        @Override
        protected void onPreExecute() {
            if(dark) pd = new ProgressDialog(context, R.style.darkDialog);
            else pd = new ProgressDialog(context);
            pd.setMessage("시작중");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected Integer doInBackground(Void... voids) {

            Search a = new Search("아이",0);
            a.fetch(httpClient);
            if(a.getResult().size()<=0){
                return 1;
            }


            List<MTitle> recents = p.getRecent();
            sum += recents.size();
            List<MTitle> favorites = p.getFavorite();
            sum += favorites.size();
            JSONObject bookmarks = p.getBookmarkObject();
            sum += bookmarks.length();
            //recent data

            //test only favorites
            titleList(favorites);
            removeDups(favorites);

            titleList(recents);
            removeDups(recents);


            Iterator<String> keys = bookmarks.keys();
            JSONObject newBookMark = new JSONObject();
            while(keys.hasNext()){
                try {
                    current++;
                    publishProgress();
                    String key = keys.next();
                    try {
                        Integer.parseInt(key);
                        newBookMark.put(key, bookmarks.get(key));
                    } catch (Exception e) {
                        //is not number
                        int id = findId(key);
                        if (id > 0) {
                            newBookMark.put(String.valueOf(id), bookmarks.get(key));
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            p.setFavorites(favorites);
            p.setRecents(recents);
            p.setBookmarks(newBookMark);

            return 0;
        }

        void removeDups(List<MTitle> titles){
            for(int i=0; i<titles.size(); i++){
                MTitle target = titles.get(i);
                for(int j =0 ; j<titles.size(); j++){
                    if(j!=i && titles.get(j).getId() == target.getId()){
                        titles.remove(i);
                        i--;
                        break;
                    }
                }
            }
        }

        void titleList(List<MTitle> titles){
            for(int i = 0; i<titles.size(); i++){
                current++;
                publishProgress();
                MTitle target = titles.get(i);
                if(target.getId() <= 0){
                    int newId = findId(target);
                    if(newId<0){
                        titles.remove(i);
                        i--;
                    }else{
                        target.setId(newId);
                    }
                }
            }
        }

        int findId(String title){
            return findId(new MTitle(title,-1,"", "",new ArrayList<>(),-1));
        }

        int findId(MTitle title){
            String name = title.getName();
            Search s = new Search(name,0);
            while(!s.isLast()){
                s.fetch(httpClient);
                for(Title t : s.getResult()){
                    if(t.getName().equals(name)){
                        return t.getId();
                    }
                }
            }
            return -1;
        }

        @Override
        protected void onPostExecute(Integer resCode) {
            if(pd.isShowing()){
                pd.dismiss();
            }

            if(resCode == 0) showPopup(context,"알림","데이터 업데이트 완료");
            else if(resCode == 1) showPopup(context,"연결 오류","연결을 확인하고 다시 시도해 주세요.");
        }

        @Override
        protected void onCancelled() {
            if(pd.isShowing()){
                pd.dismiss();
            }
        }
    }
}

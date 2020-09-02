package ml.melun.mangaview.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import android.view.View;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ml.melun.mangaview.CheckInfo;
import ml.melun.mangaview.Downloader;
import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.fragment.MainMain;
import ml.melun.mangaview.fragment.MainSearch;
import ml.melun.mangaview.fragment.RecyclerFragment;
import ml.melun.mangaview.mangaview.MTitle;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static ml.melun.mangaview.Downloader.BROADCAST_STOP;
import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.Utils.showYesNoNeutralPopup;
import static ml.melun.mangaview.Utils.showYesNoPopup;
import static ml.melun.mangaview.Utils.writePreferenceToFile;
import static ml.melun.mangaview.activity.FirstTimeActivity.RESULT_EULA_AGREE;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FILE_SAVE;
import static ml.melun.mangaview.activity.SettingsActivity.RESULT_NEED_RESTART;



//TODO: smooth transitioning between fragments

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static int PERMISSION_CODE = 132322;
    int startTab;
    int currentTab = -1;
    private Context context;
    MenuItem versionItem;
    String homeDirStr;
    Boolean dark;
    NavigationView navigationView;
    Toolbar toolbar;
    View progressView;
    private static final int FIRST_TIME_ACTIVITY = 9;

    Fragment[] fragments = new Fragment[3];

    FrameLayout content;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("currentTab", currentTab);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        fragments[0] = new MainMain();
        fragments[1] = new MainSearch();
        fragments[2] = new RecyclerFragment();
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDarkNoTitle);
        else setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        context = this;


        //check prefs
       if(!p.getSharedPref().getBoolean("eula",false)){
            startActivityForResult(new Intent(context, FirstTimeActivity.class), FIRST_TIME_ACTIVITY);
       }else if(!p.check()) {
           //popup to fix preferences
           System.out.println("preference needs update");
           showYesNoNeutralPopup(context, "기록 업데이트 필요",
                   "저장된 데이터에서 더이상 지원되지 않는 이전 형식이 발견되었습니다. 정상적인 사용을 위해 업데이트가 필요합니다. 데이터를 업데이트 하시겠습니까?" +
                           "\n(데이터 일부가 유실될 수 있습니다. 꼭 백업을 하고 진행해 주세요)",
                   "데이터 백업",
                   new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialogInterface, int i) {
                            //proceed
                           final EditText editText = new EditText(context);
                           editText.setHint(p.getDefUrl());

                           AlertDialog.Builder builder;
                           if (new Preference(context).getDarkTheme()) builder = new AlertDialog.Builder(context, R.style.darkDialog);
                           else builder = new AlertDialog.Builder(context);
                           builder.setTitle("기록 업데이트")
                                   .setView(editText)
                                   .setMessage("이 작업은 되돌릴수 없습니다. 계속 하려면 유효한 주소를 입력해 주세요")
                                   .setPositiveButton("계속", new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialogInterface, int i) {
                                           String url = editText.getText().toString();
                                           if(url == null || url.length()<1)
                                               url = p.getDefUrl();
                                           new Migrator(savedInstanceState, url).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                       }
                                   })
                                   .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialogInterface, int i) {
                                           finish();
                                       }
                                   })
                                   .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                       @Override
                                       public void onCancel(DialogInterface dialogInterface) {
                                           finish();
                                       }
                                   })
                                   .show();
                       }
                   }, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialogInterface, int i) {
                           showPopup(context, "알림", "앱의 데이터를 초기화 하거나 데이터 업데이트를 진행하지 않으면 사용이 불가합니다.", new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialogInterface, int i) {
                                   finish();
                               }
                           }, new DialogInterface.OnCancelListener() {
                               @Override
                               public void onCancel(DialogInterface dialogInterface) {
                                   finish();
                               }
                           });
                       }
                   }, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialogInterface, int i) {
                           //backup
                           Intent intent = new Intent(context, FolderSelectActivity.class);
                           intent.putExtra("mode", MODE_FILE_SAVE);
                           intent.putExtra("title", "백업");
                           startActivityForResult(intent, MODE_FILE_SAVE);
                       }
                   }, new DialogInterface.OnCancelListener() {
                       @Override
                       public void onCancel(DialogInterface dialogInterface) {
                           finish();
                       }
                   });
       }else {
            activityInit(savedInstanceState);
       }
    }

    private void activityInit(Bundle savedInstanceState){
        setContentView(R.layout.activity_main);



        progressView = this.findViewById(R.id.progress_panel);

        // url updater
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //nav_drawer color scheme
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

        homeDirStr = p.getHomeDir();

        // get app version
        versionItem = navigationView.getMenu().findItem(R.id.nav_version_display);
        int version = 0;
        try{
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionCode;
        }catch (Exception e){
            e.printStackTrace();
        }
        versionItem.setTitle("v."+version);

        //check for permission
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck== PackageManager.PERMISSION_DENIED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{READ_EXTERNAL_STORAGE,WRITE_EXTERNAL_STORAGE},
                        PERMISSION_CODE);
            }
        }

        content = findViewById(R.id.contentHolder);

        // set initial tab
        startTab = p.getStartTab();
        if(savedInstanceState != null)
            changeFragment(savedInstanceState.getInt("currentTab"));
        else
            changeFragment(startTab);

        getSupportActionBar().setTitle(navigationView.getMenu().findItem(getTabId(currentTab)).getTitle());
        navigationView.getMenu().getItem(currentTab).setChecked(true);

        // savedInstanceState


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

    public int getFragmentIndex(int i){
        switch(i){
            case R.id.nav_main:
                return 0;
            case R.id.nav_search:
                return 1;
            case R.id.nav_recent:
                return 2;
            case R.id.nav_favorite:
                return 3;
            case R.id.nav_download:
                return 4;
        }
        return -1;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(currentTab == startTab){

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
                changeFragment(startTab);
                navigationView.getMenu().getItem(startTab).setChecked(true);
                toolbar.setTitle(navigationView.getMenu().findItem(getTabId(startTab)).getTitle());
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

    boolean changeFragment(int index){
        boolean change = !(currentTab >= 2 && index >= 2);
        int fragmentI = index>2 ? 2 : index;
        if(index>-1 && index != currentTab){
            currentTab = index;
            if(index >= 2){
                ((RecyclerFragment)fragments[2]).changeMode(getTabId(index));
            }
            if(change) {
                getSupportFragmentManager().beginTransaction().replace(R.id.contentHolder, (Fragment) fragments[fragmentI]).commit();
            }

            return true;
        }else
            return false;   //fragment does not exist
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (!changeFragment(getFragmentIndex(id))) {
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
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == FIRST_TIME_ACTIVITY){
            if(resultCode == RESULT_EULA_AGREE) {
                activityInit(null);
            }else
                finish();
            return;
        }else if(requestCode == MODE_FILE_SAVE){
            String path = null;
            if(data!=null)
                path = data.getStringExtra("path");
            if(path != null){
                if(writePreferenceToFile(context, new File(path))) {
                    Toast.makeText(context, "백업 완료!", Toast.LENGTH_LONG).show();
                }
            }
            Toast.makeText(context, "백업 실패", Toast.LENGTH_LONG).show();
            finish();
            startActivity(getIntent());
        }
        if(resultCode == RESULT_NEED_RESTART){
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
    }

    public void hideProgressPanel(){
        progressView.setVisibility(View.GONE);
    }
    private class Migrator extends AsyncTask<Void, Void, Integer>{

        ProgressDialog pd;
        int sum = 0;
        int current = 0;
        List<MTitle> newFavorites, newRecents;
        List<String> failed;
        Bundle bundle;

        public Migrator(Bundle bundle, String url){
            this.bundle = bundle;
            p.setUrl(url);
        }

        @Override
        protected void onPreExecute() {
            if(p.getDarkTheme()) pd = new ProgressDialog(context, R.style.darkDialog);
            else pd = new ProgressDialog(context);
            pd.setMessage("시작중");
            pd.setCancelable(false);
            pd.show();
        }


        @Override
        protected void onProgressUpdate(Void... values) {
            pd.setMessage(current +" / " + sum+"\n 앱을 종료하지 말아주세요.");
        }

        @Override
        protected Integer doInBackground(Void... voids) {

            //test
            Search a = new Search("아이",0);
            a.fetch(httpClient);
            if(a.getResult().size()<1){
                return 1;
            }


            List<MTitle> recents = p.getRecent();
            sum += recents.size();
            List<MTitle> favorites = p.getFavorite();
            sum += favorites.size();
            //recent data

            //test only favorites
            removeDups(favorites);
            removeDups(recents);

            newRecents = new ArrayList<>();
            newFavorites = new ArrayList<>();
            failed = new ArrayList<>();

            for(int i=0; i<recents.size(); i++){
                try {
                    current++;
                    publishProgress();
                    MTitle newTitle = findTitle(recents.get(i));
                    if(newTitle !=null)
                        newRecents.add(newTitle);
                    else
                        failed.add(recents.get(i).getName());
                }catch (Exception e){
                    e.printStackTrace();
                    failed.add(recents.get(i).getName());
                }
            }
            for(int i=0; i<favorites.size(); i++){
                try {
                    current++;
                    publishProgress();
                    MTitle newTitle = findTitle(favorites.get(i));
                    if(newTitle !=null)
                        newFavorites.add(newTitle);
                    else
                        failed.add(favorites.get(i).getName());
                }catch (Exception e){
                    e.printStackTrace();
                    failed.add(favorites.get(i).getName());
                }
            }

            p.setFavorites(newFavorites);
            p.setRecents(newRecents);

            //remove bookmarks
            p.resetViewerBookmark();
            p.resetBookmark();

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

        MTitle findTitle(String title){
            return findTitle(new MTitle(title,-1,"", "",new ArrayList<>(),""));
        }

        MTitle findTitle(MTitle title){
            String name = title.getName();
            Search s = new Search(name,0);
            while(!s.isLast()){
                s.fetch(httpClient);
                for(Title t : s.getResult()){
                    if(t.getName().equals(name)){
                        return t.minimize();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer resCode) {
            if(pd.isShowing()){
                pd.dismiss();
            }

            if(resCode == 0){
                StringBuilder builder = new StringBuilder();
                for(String t : failed){
                    builder.append("\n"+t);
                }
                builder.deleteCharAt(0);

                final EditText editText = new EditText(context);
                editText.setText(builder.toString());

                AlertDialog.Builder abuilder;
                if (new Preference(context).getDarkTheme()) abuilder = new AlertDialog.Builder(context, R.style.darkDialog);
                else abuilder = new AlertDialog.Builder(context);
                abuilder.setTitle("알림")
                        .setView(editText)
                        .setMessage("기록 업데이트 완료.\n실패한 항목: " + failed.size() + "개")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                activityInit(bundle);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                activityInit(bundle);
                            }
                        })
                        .show();
            }
            else if(resCode == 1)
                showPopup(context, "연결 오류", "연결을 확인하고 다시 시도해 주세요.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                });
        }

        @Override
        protected void onCancelled() {
            if(pd.isShowing()){
                pd.dismiss();
            }
            finish();
        }
    }
}

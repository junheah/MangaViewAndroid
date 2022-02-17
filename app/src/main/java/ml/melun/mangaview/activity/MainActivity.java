package ml.melun.mangaview.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import ml.melun.mangaview.CheckInfo;
import ml.melun.mangaview.Downloader;
import ml.melun.mangaview.Migrator;
import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.fragment.MainMain;

import ml.melun.mangaview.fragment.MainSearch;
import ml.melun.mangaview.fragment.RecyclerFragment;
import ml.melun.mangaview.UrlUpdater;
import ml.melun.mangaview.interfaces.MainActivityCallback;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static ml.melun.mangaview.Downloader.BROADCAST_STOP;
import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Migrator.MIGRATE_FAIL;
import static ml.melun.mangaview.Migrator.MIGRATE_PROGRESS;
import static ml.melun.mangaview.Migrator.MIGRATE_RESULT;
import static ml.melun.mangaview.Migrator.MIGRATE_START;
import static ml.melun.mangaview.Migrator.MIGRATE_STOP;
import static ml.melun.mangaview.Migrator.MIGRATE_SUCCESS;
import static ml.melun.mangaview.Utils.CODE_SCOPED_STORAGE;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.Utils.showYesNoNeutralPopup;
import static ml.melun.mangaview.Utils.writePreferenceToFile;
import static ml.melun.mangaview.activity.FirstTimeActivity.RESULT_EULA_AGREE;
import static ml.melun.mangaview.activity.FolderSelectActivity.MODE_FILE_SAVE;
import static ml.melun.mangaview.activity.SettingsActivity.RESULT_NEED_RESTART;




public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MainActivityCallback {

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
    public void search(String query) {
        ((MainSearch)fragments[1]).setSearch(query);
        changeFragment(1);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        fragments[0] = MainMain.newInstance();
        fragments[1] = new MainSearch();
        fragments[2] = new RecyclerFragment();
        dark = p.getDarkTheme();
        if (dark) setTheme(R.style.AppThemeDarkNoTitle);
        else setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        context = this;
        Intent intent = getIntent();
        String action = intent.getAction();


        //check prefs
        if (p.getSharedPref().getLong("eula2", -1)<0) {
            startActivityForResult(new Intent(context, FirstTimeActivity.class), FIRST_TIME_ACTIVITY);
        } else if (Migrator.running) {
            ProgressDialog mpd;
            if (p.getDarkTheme()) mpd = new ProgressDialog(context, R.style.darkDialog);
            else mpd = new ProgressDialog(context);
            mpd.setMessage("기록 업데이트중..");
            mpd.setCancelable(false);
            mpd.show();

            BroadcastReceiver migratorStatusReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch (intent.getAction()) {
                        case MIGRATE_PROGRESS:
                            mpd.setMessage(intent.getStringExtra("msg"));
                            break;
                        case MIGRATE_START:
                            break;
                        case MIGRATE_STOP:
                            mpd.dismiss();
                            break;
                        case MIGRATE_FAIL:
                            mpd.dismiss();
                            migratorEndPopup(savedInstanceState, 1, intent.getStringExtra("msg"));
                            break;
                        case MIGRATE_SUCCESS:
                            mpd.dismiss();
                            migratorEndPopup(savedInstanceState, 0, intent.getStringExtra("msg"));
                            break;
                    }
                }
            };
            IntentFilter infil = new IntentFilter();
            infil.addAction(MIGRATE_PROGRESS);
            infil.addAction(MIGRATE_START);
            infil.addAction(MIGRATE_STOP);
            infil.addAction(MIGRATE_FAIL);
            infil.addAction(MIGRATE_SUCCESS);
            registerReceiver(migratorStatusReceiver, infil);

            Intent pintent = new Intent(getApplicationContext(), Migrator.class);
            pintent.setAction(MIGRATE_PROGRESS);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(pintent);
            } else {
                startService(pintent);
            }

        } else if (!p.check()) {
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
                            if (new Preference(context).getDarkTheme())
                                builder = new AlertDialog.Builder(context, R.style.darkDialog);
                            else builder = new AlertDialog.Builder(context);
                            builder.setTitle("기록 업데이트")
                                    .setView(editText)
                                    .setMessage("이 작업은 되돌릴수 없습니다. 계속 하려면 유효한 주소를 입력해 주세요.")
                                    .setPositiveButton("계속", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            String url = editText.getText().toString();
                                            if (url == null || url.length() < 1)
                                                url = p.getDefUrl();

                                            p.setUrl(url);

                                            Intent intent = new Intent(getApplicationContext(), Migrator.class);
                                            intent.setAction(MIGRATE_START);
                                            if (Build.VERSION.SDK_INT >= 26) {
                                                startForegroundService(intent);
                                            } else {
                                                startService(intent);
                                            }
                                            //queue title to service
                                            Toast.makeText(getApplication(), "작업을 시작합니다.", Toast.LENGTH_LONG).show();
                                            //restart activity
                                            finish();
                                            startActivity(getIntent());
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
        }else if(action != null && action.equals(MIGRATE_RESULT)){
            migratorEndPopup(savedInstanceState, 0, intent.getStringExtra("msg"));
        }else{
            activityInit(savedInstanceState);
       }
    }

    private void activityInit(Bundle savedInstanceState){
        p.check2();
        setContentView(R.layout.activity_main);
        progressView = this.findViewById(R.id.progress_panel);

        // url updater
        if(p.getAutoUrl()) {
            ((MainMain)fragments[0]).setWait(true);
            new UrlUpdater(context, false, ((MainMain)fragments[0]).getCallback(), p.getDefUrl()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

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
            if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE){
                //doesn't have to do anything
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{READ_EXTERNAL_STORAGE,WRITE_EXTERNAL_STORAGE},
                        PERMISSION_CODE);
            }
        }

        content = findViewById(R.id.contentHolder);

        // set initial tab
        startTab = p.getStartTab();
        if(savedInstanceState != null) {
            int t = savedInstanceState.getInt("currentTab", 0);
            changeFragment(t>-1 ? t : 0);
        }else
            changeFragment(startTab);



        // savedInstanceState


        //check for update, notices
        new CheckInfo(context,httpClient, true).all(false);

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

                                if(Downloader.isRunning()){
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
                builder.setMessage(Downloader.isRunning()  ? "다운로드가 진행중입니다. 정말로 종료 하시겠습니까?" : "정말로 종료 하시겠습니까?")
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
        boolean res = false;
        if(index>-1 && index != currentTab){
            currentTab = index;
            if(index >= 2){
                ((RecyclerFragment)fragments[2]).changeMode(getTabId(index));
            }
            if(change) {
                getSupportFragmentManager().beginTransaction().replace(R.id.contentHolder, (Fragment) fragments[fragmentI]).commit();
            }
            res = true;
        }
        getSupportActionBar().setTitle(navigationView.getMenu().findItem(getTabId(currentTab)).getTitle());
        navigationView.getMenu().getItem(currentTab).setChecked(true);
        return res;
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
                new CheckInfo(context,httpClient,false).all(true);
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
                }else Toast.makeText(context, "백업 실패", Toast.LENGTH_LONG).show();
            }else Toast.makeText(context, "백업 실패", Toast.LENGTH_LONG).show();

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


    private void migratorEndPopup(Bundle bundle, int resCode, String msg){
        if(resCode==0) {
            final ScrollView scrollView = new ScrollView(context);
            final LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            final TextView textView = new TextView(context);
            textView.setText(msg);
            final Button copyBtn = new Button(context);
            copyBtn.setText("결과 복사");
            copyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("result", msg);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
                }
            });
            final Button btn = new Button(context);
            btn.setText("닫기");
            linearLayout.addView(textView);
            linearLayout.addView(copyBtn);
            linearLayout.addView(btn);
            scrollView.addView(linearLayout);

            AlertDialog.Builder abuilder;
            if (new Preference(context).getDarkTheme())
                abuilder = new AlertDialog.Builder(context, R.style.darkDialog);
            else abuilder = new AlertDialog.Builder(context);
            AlertDialog dialog = abuilder.setTitle("결과")
                    .setView(scrollView)
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            activityInit(bundle);
                        }
                    })
                    .create();
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                    activityInit(bundle);
                }
            });
            dialog.show();
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
}

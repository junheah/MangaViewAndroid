package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import com.google.android.material.appbar.AppBarLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;

import androidx.core.view.DisplayCutoutCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.melun.mangaview.R;
import ml.melun.mangaview.ui.StripLayoutManager;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.adapter.CustomSpinnerAdapter;
import ml.melun.mangaview.adapter.StripAdapter;
import ml.melun.mangaview.ui.CustomSpinner;
import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.model.PageItem;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.getScreenSize;
import static ml.melun.mangaview.Utils.hideSpinnerDropDown;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;

public class ViewerActivity extends AppCompatActivity {

    Manga manga;
    Title title;
    RecyclerView strip;
    Context context = this;
    StripAdapter stripAdapter;
    androidx.appcompat.widget.Toolbar toolbar;
    boolean toolbarshow = true;
    TextView toolbarTitle;
    AppBarLayout appbar, appbarBottom;
    StripLayoutManager manager;
    ImageButton next, prev;
    Button cut, pageBtn;
    List<Manga> eps;

    boolean autoCut = false;
    List<String> imgs;
    boolean dark;
    Intent result;
    ImageButton commentBtn;
    int width=0;
    Intent intent;
    boolean captchaChecked = false;
    CustomSpinner spinner;
    CustomSpinnerAdapter spinnerAdapter;
    InfiniteScrollCallback infiniteScrollCallback;
    loadImages loader;


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("manga", new Gson().toJson(manga));
        outState.putString("title", new Gson().toJson(title));
        super.onSaveInstanceState(outState);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dark = p.getDarkTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        next = this.findViewById(R.id.toolbar_next);
        prev = this.findViewById(R.id.toolbar_previous);
        toolbar = this.findViewById(R.id.viewerToolbar);
        appbar = this.findViewById(R.id.viewerAppbar);
        toolbarTitle = this.findViewById(R.id.toolbar_title);
        appbarBottom = this.findViewById(R.id.viewerAppbarBottom);
        cut = this.findViewById(R.id.viewerBtn2);
        cut.setText("자동 분할");
        pageBtn = this.findViewById(R.id.viewerBtn1);
        pageBtn.setText("-/-");
        commentBtn = this.findViewById(R.id.commentButton);
        spinner = this.findViewById(R.id.toolbar_spinner);
        width = getScreenSize(getWindowManager().getDefaultDisplay());

        //initial padding setup
        appbar.setPadding(0, getStatusBarHeight(),0,0);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);


        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat windowInsetsCompat) {
                //This is where you get DisplayCutoutCompat
                int statusBarHeight = getStatusBarHeight();
                int ci;
                if(windowInsetsCompat.getDisplayCutout() == null) ci = 0;
                else ci = windowInsetsCompat.getDisplayCutout().getSafeInsetTop();

                //System.out.println(ci + " : " + statusBarHeight);
                appbar.setPadding(0,ci > statusBarHeight ? ci : statusBarHeight,0,0);
                view.setPadding(windowInsetsCompat.getStableInsetLeft(),0,windowInsetsCompat.getStableInsetRight(),windowInsetsCompat.getStableInsetBottom());
                return windowInsetsCompat;
            }
        });


        infiniteScrollCallback = new InfiniteScrollCallback() {
            @Override
            public Manga prevEp(InfiniteLoadCallback callback, Manga curm) {
                p.removeViewerBookmark(curm);
                int i = eps.indexOf(curm);
                if(i<eps.size()-1) {
                    if(loader != null) loader.cancel(true);
                    loader = new loadImages(eps.get(i + 1), new LoadMangaCallback() {
                        @Override
                        public void post(Manga m) {
                            if (m.getImgs().size() > 0)
                                stripAdapter.insertManga(m);
                            callback.prevLoaded(m);
                        }
                    },false);
                    loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    return eps.get(i + 1);
                }else{
                    callback.prevLoaded(null);
                    return null;
                }
            }

            @Override
            public Manga nextEp(InfiniteLoadCallback callback, Manga curm) {
                p.removeViewerBookmark(curm);
                int i = eps.indexOf(curm);
                if(i>0) {
                    if(loader != null) loader.cancel(true);
                    loader = new loadImages(eps.get(i - 1), new LoadMangaCallback() {
                        @Override
                        public void post(Manga m) {
                            if (m.getImgs().size() > 0)
                                stripAdapter.appendManga(m);
                            callback.nextLoaded(m);
                        }
                    },false);
                    loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    return eps.get(i - 1);
                }else{
                    callback.nextLoaded(null);
                    return null;
                }
            }

            @Override
            public void updateInfo(Manga m) {
                updateIntent(m);
                refreshToolbar(m);
            }
        };

        this.findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        //imageZoomHelper = new ImageZoomHelper(this);
        try {
            intent = getIntent();
            if(savedInstanceState == null) {
                title = new Gson().fromJson(intent.getStringExtra("title"), new TypeToken<Title>() {
                }.getType());
                manga = new Gson().fromJson(intent.getStringExtra("manga"), new TypeToken<Manga>() {
                }.getType());
            }else{
                title = new Gson().fromJson(savedInstanceState.getString("title"), new TypeToken<Title>() {
                }.getType());
                manga = new Gson().fromJson(savedInstanceState.getString("manga"), new TypeToken<Manga>() {
                }.getType());
            }

            toolbarTitle.setText(manga.getName());

            strip = this.findViewById(R.id.strip);
            manager = new StripLayoutManager(this);
            manager.setOrientation(LinearLayoutManager.VERTICAL);
            spinnerAdapter = new CustomSpinnerAdapter(context);
            spinnerAdapter.setListener(new CustomSpinnerAdapter.CustomSpinnerListener() {
                @Override
                public void onClick(Manga m, int i) {
                    lockUi(true);
                    spinner.setSelection(m);
                    hideSpinnerDropDown(spinner);
                    loadManga(m);

                }
            });
            spinner.setAdapter(spinnerAdapter);
            strip.setLayoutManager(manager);

            if(intent.getBooleanExtra("recent",false)){
                Intent resultIntent = new Intent();
                setResult(RESULT_OK,resultIntent);
            }


            if(!manga.isOnline()){
                commentBtn.setVisibility(View.GONE);
            }
            
            loadManga(manga);
            //strip.setItemAnimator(null);
            strip.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if(strip.getLayoutManager().getItemCount()>0 && newState == RecyclerView.SCROLL_STATE_DRAGGING && toolbarshow) {
                        toggleToolbar();
                    }
                }
            });

        }catch(Exception e){
            e.printStackTrace();
        }

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadManga(manga.nextEp());
            }
        });
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadManga(manga.prevEp());
            }
        });
        cut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAutoCut();
            }
        });

        pageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PageItem current = stripAdapter.getCurrentVisiblePage();
                AlertDialog.Builder alert;
                if(dark) alert = new AlertDialog.Builder(context,R.style.darkDialog);
                else alert = new AlertDialog.Builder(context);

                alert.setTitle("페이지 선택\n(1~"+current.manga.getImgs().size()+")");
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setRawInputType(Configuration.KEYBOARD_12KEY);
                alert.setView(input);
                alert.setPositiveButton("이동", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {
                        //이동 시
                        if(input.getText().length()>0) {
                            int page = Integer.parseInt(input.getText().toString());
                            if (page < 1) page = 1;
                            if (page > current.manga.getImgs().size()) page = current.manga.getImgs().size();
                            manager.scrollToPage(new PageItem(page-1,"",current.manga));
                            pageBtn.setText(page+"/"+current.manga.getImgs().size());
                        }
                    }
                });

                alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {
                        //취소 시
                    }
                });
                alert.show();
            }
        });

    }



    void refresh(){
        loadManga(manga);
    }

    void loadManga(Manga m, LoadMangaCallback callback){
        if(m!=null) {
            this.manga = m;
            loadImages l = new loadImages(m, callback,true);
            l.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    void loadManga(Manga m){
        if(stripAdapter!=null) stripAdapter.removeAll();
        if(m.isOnline()) {
            loadManga(m, new LoadMangaCallback() {
                @Override
                public void post(Manga m) {
                    manga = m;
                    setManga(m);
                }
            });
        }else{
            //offline
            eps = title.getEps();
//            for(int i=0; i<eps.size(); i++){
//                eps.get(i).setNextEp(i>0 ? eps.get(i-1) : null);
//                eps.get(i).setPrevEp(i<eps.size()-1 ? eps.get(i+1) : null);
//            }
            m = eps.get(eps.indexOf(m));
            setManga(m);
        }
    }


    public void setManga(Manga m){
        try {
            lockUi(false);
            if(m.getImgs() == null || m.getImgs().size()==0) {
                showCaptchaPopup(context, p);
                return;
            }
            stripAdapter = new StripAdapter(context, m, autoCut, width,title, infiniteScrollCallback);

            refreshAdapter();
            bookmarkRefresh(m);
            refreshToolbar(m);
            updateIntent(m);

        }catch (Exception e){
            Utils.showCaptchaPopup(context, e, p);
            e.printStackTrace();
        }
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if(keyCode == p.getPrevPageKey() || keyCode == p.getNextPageKey()) {
            int index = manager.findFirstVisibleItemPosition();
            if (keyCode == p.getNextPageKey()) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    manager.scrollToPosition(index+1);
                }
            } else if (keyCode == p.getPrevPageKey()) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    manager.scrollToPosition(index-1);
                }
            }
            if(toolbarshow) toggleToolbar();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(toolbarshow) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public void toggleToolbar(){
        //attrs = getWindow().getAttributes();
        if(toolbarshow){
            appbar.animate().translationY(-appbar.getHeight());
            appbarBottom.animate().translationY(+appbarBottom.getHeight());
            toolbarshow=false;
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        else {
            PageItem item = stripAdapter.getCurrentVisiblePage();
            if(item != null) {
                pageBtn.setText(item.index+1 + "/" + item.manga.getImgs().size());
                toolbarTitle.setText(item.manga.getName());
                commentBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent commentActivity = new Intent(context, CommentsActivity.class);
                        //create gson and put extra
                        Gson gson = new Gson();
                        commentActivity.putExtra("comments", gson.toJson(item.manga.getComments()));
                        commentActivity.putExtra("bestComments", gson.toJson(item.manga.getBestComments()));
                        commentActivity.putExtra("id", item.manga.getId());
                        startActivity(commentActivity);
                    }
                });
                appbar.animate().translationY(0);
                appbarBottom.animate().translationY(0);
                toolbarshow = true;
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }

        }
        //getWindow().setAttributes(attrs);
    }

    public void toggleAutoCut(){
        PageItem page = stripAdapter.getCurrentVisiblePage();
        if(autoCut){
            autoCut = false;
            cut.setBackgroundResource(R.drawable.button_bg);
            //viewerBookmark /= 2;
        } else{
            autoCut = true;
            cut.setBackgroundResource(R.drawable.button_bg_on);
            //viewerBookmark *= 2;
        }
        stripAdapter.removeAll();
        stripAdapter = new StripAdapter(context, page.manga, autoCut, width,title, infiniteScrollCallback);
        stripAdapter.preloadAll();
        strip.setAdapter(stripAdapter);
        stripAdapter.setClickListener(new StripAdapter.ItemClickListener() {
            public void onItemClick() {
                // show/hide toolbar
                toggleToolbar();
            }
        });
        manager.scrollToPage(page);
    }


//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        return imageZoomHelper.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev);
//    }


    @Override
    public void onBackPressed() {
        if(onBack!=null){
            onBack.run();
        }else{
            super.onBackPressed();
        }
    }

    Runnable onBack;

    void setOnBackPressed(Runnable onBackPressed){
        this.onBack = onBackPressed;
    }
    void resetOnBackPressed(){
        this.onBack = null;
    }



    private class loadImages extends AsyncTask<Void,String,Integer> {
        boolean lockui;
        LoadMangaCallback callback;
        Manga m;

        public loadImages(Manga m, LoadMangaCallback callback, boolean lockui){
            this.lockui = lockui;
            this.m = m;
            this.callback = callback;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            if(lockui) lockUi(true);
            setOnBackPressed(new Runnable() {
                @Override
                public void run(){
                    loadImages.super.cancel(true);
                    finish();
                }
            });
        }

        protected Integer doInBackground(Void... params) {
            if(m.isOnline()) {
                Login login = p.getLogin();
                Map<String, String> cookie = new HashMap<>();
                if (login != null) {
                    String php = p.getLogin().getCookie();
                    login.buildCookie(cookie);
                }
                m.fetch(httpClient);
                return 0;
            }else{
                return 0;
            }
        }

        @Override
        protected void onPostExecute(Integer res) {
            if(lockui) lockUi(false);
            if (title == null)
                title = m.getTitle();
            super.onPostExecute(res);
            resetOnBackPressed();
            callback.post(m);
        }
    }

    public void bookmarkRefresh(Manga m){
        if(m.useBookmark()) {
            PageItem page = new PageItem(p.getViewerBookmark(m), "", m);
            if (page.index > -1) {
                manager.scrollToPage(page);
            }
            if (m.useBookmark()) {
                // if manga is online or has title.gson
                if (title == null) title = m.getTitle();
                p.addRecent(title);
                if (m!=null && m.getId()>0) p.setBookmark(title, m.getId());
            }
        }else{
            manager.scrollToPage(new PageItem(0,"",m));
        }
    }

    public void updateIntent(Manga m){
        this.manga = m;
        result = new Intent();
        result.putExtra("id", m.getId());
        setResult(RESULT_OK, result);
    }

    public void refreshAdapter(){
        strip.setAdapter(stripAdapter);
        stripAdapter.setClickListener(new StripAdapter.ItemClickListener() {
            public void onItemClick() {
                // show/hide toolbar
                toggleToolbar();
            }
        });
    }

    public void refreshToolbar(Manga m){
        //spinner
        eps = m.getEps();
        if(eps == null || eps.size() == 0){
            //backup plan
            eps = title.getEps();
        }
        spinnerAdapter.setData(eps, m);
        spinner.setSelection(m);

        //top toolbar
        toolbarTitle.setText(m.getName());
        toolbarTitle.setSelected(true);

        if(m.nextEp() == null){
            next.setEnabled(false);
            next.setColorFilter(Color.BLACK);
        }
        else {
            next.setEnabled(true);
            next.setColorFilter(null);
        }
        if(m.prevEp() == null) {
            prev.setEnabled(false);
            prev.setColorFilter(Color.BLACK);
        }
        else {
            prev.setEnabled(true);
            prev.setColorFilter(null);
        }
        PageItem page = stripAdapter.getCurrentVisiblePage();
        if(page!=null)
            pageBtn.setText(page.index+1+"/"+page.manga.getImgs().size());
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        return super.onMenuOpened(featureId, menu);
    }

    void lockUi(boolean lock){
        commentBtn.setEnabled(!lock);
        next.setEnabled(!lock);
        prev.setEnabled(!lock);
        pageBtn.setEnabled(!lock);
        cut.setEnabled(!lock);
        strip.setEnabled(!lock);
        spinner.setEnabled(!lock);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_CAPTCHA) {
            refresh();
        }
    }

    public interface InfiniteScrollCallback{
        Manga nextEp(InfiniteLoadCallback callback, Manga curm);
        Manga prevEp(InfiniteLoadCallback callback, Manga curm);
        void updateInfo(Manga m);
    }
    public interface LoadMangaCallback {
        void post(Manga m);
    }
    public interface InfiniteLoadCallback{
        void prevLoaded(Manga m);
        void nextLoaded(Manga m);
    }

}

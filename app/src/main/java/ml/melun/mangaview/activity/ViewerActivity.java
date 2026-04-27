package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.AppBarLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.melun.mangaview.R;
import ml.melun.mangaview.interfaces.StringCallback;
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
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.Utils.showTokiCaptchaPopup;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;
import static ml.melun.mangaview.mangaview.Title.LOAD_CAPTCHA;
import static ml.melun.mangaview.mangaview.Title.LOAD_OK;

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
    prefetchImages nextPrefetcher;
    boolean previousEpisodeBoundaryLoading = false;
    boolean nextEpisodeBoundaryLoading = false;
    boolean previousEpisodeBoundaryJumpPending = false;
    boolean nextEpisodeBoundaryJumpPending = false;
    private static final int NEXT_EPISODE_PRELOAD_LIMIT = 12;
    private static final int PREVIOUS_EPISODE_PULL_THRESHOLD_DP = 36;
    float topPullStartY = 0;
    boolean topPullTriggered = false;
    boolean topPullEligible = false;
    boolean topPullInProgress = false;
    int previousPullDistancePx = 0;
    float bottomPullStartY = 0;
    boolean bottomPullTriggered = false;
    boolean bottomPullEligible = false;
    boolean bottomPullInProgress = false;
    int nextPullDistancePx = 0;
    int previousBoundaryRequestedFromId = -1;
    boolean previousEpisodePositioning = false;


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Utils.saveMangaState(outState, manga);
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


        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (view, windowInsetsCompat) -> {
            //This is where you get DisplayCutoutCompat
            int statusBarHeight = getStatusBarHeight();
            int ci;
            if(windowInsetsCompat.getDisplayCutout() == null) ci = 0;
            else ci = windowInsetsCompat.getDisplayCutout().getSafeInsetTop();

            appbar.setPadding(0, Math.max(ci, statusBarHeight),0,0);
            view.setPadding(windowInsetsCompat.getStableInsetLeft(),0,windowInsetsCompat.getStableInsetRight(),windowInsetsCompat.getStableInsetBottom());
            return windowInsetsCompat;
        });

        infiniteScrollCallback = new InfiniteScrollCallback() {
            @Override
            public Manga prevEp(InfiniteLoadCallback callback, Manga curm) {
                p.removeViewerBookmark(curm);
                int i = eps.indexOf(curm);
                if(i >= 0 && i<eps.size()-1) {
                    cancelActiveEpisodeLoader();
                    loader = new loadImages(eps.get(i + 1), m -> {
                        if (m.getImgs(context).size() > 0) {
                            previousEpisodePositioning = true;
                            insertMangaWhenIdle(m, () -> callback.prevLoaded(m));
                        } else {
                            previousEpisodePositioning = false;
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
                    Manga target = eps.get(i - 1);
                    if(hasLoadedImages(target)) {
                        preloadFirstPages(target);
                        appendMangaWhenIdle(target, () -> {
                            callback.nextLoaded(target);
                            prefetchNextEpisode(target);
                        });
                        return target;
                    }
                    cancelActiveEpisodeLoader();
                    loader = new loadImages(target, m -> {
                        if (m.getImgs(context).size() > 0) {
                            appendMangaWhenIdle(m, () -> {
                                callback.nextLoaded(m);
                                prefetchNextEpisode(m);
                            });
                        } else {
                            callback.nextLoaded(m);
                            prefetchNextEpisode(m);
                        }
                    },false);
                    loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    return target;
                }else{
                    callback.nextLoaded(null);
                    return null;
                }
            }

            @Override
            public void updateInfo(Manga m) {
                manga = m;
                updateIntent(m);
                refreshToolbar(m);
            }
        };

        this.findViewById(R.id.backButton).setOnClickListener(view -> finish());

        try {
            intent = getIntent();
            title = new Gson().fromJson(intent.getStringExtra("title"), new TypeToken<Title>() {
            }.getType());
            if(savedInstanceState == null) {
                manga = new Gson().fromJson(intent.getStringExtra("manga"), new TypeToken<Manga>() {
                }.getType());
            }else{
                manga = Utils.restoreMangaState(savedInstanceState, title);
                if(manga == null)
                    manga = new Gson().fromJson(intent.getStringExtra("manga"), new TypeToken<Manga>() {
                    }.getType());
            }

            toolbarTitle.setText(manga.getName());

            strip = this.findViewById(R.id.strip);
            manager = new StripLayoutManager(this);
            manager.setOrientation(LinearLayoutManager.VERTICAL);
            spinnerAdapter = new CustomSpinnerAdapter(context);
            spinnerAdapter.setListener((m, i) -> {
                lockUi(true);
                spinner.setSelection(m);
                hideSpinnerDropDown(spinner);
                loadManga(m);

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
            strip.setItemAnimator(null);
            strip.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if(strip.getLayoutManager().getItemCount()>0 && newState == RecyclerView.SCROLL_STATE_DRAGGING && toolbarshow) {
                        toggleToolbar();
                    }
                    if(newState == RecyclerView.SCROLL_STATE_IDLE)
                        loadEpisodeAtBoundaryIfNeeded();
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    loadEpisodeAtBoundaryIfNeeded();
                }
            });
            strip.setOnTouchListener((view, event) -> {
                handlePreviousEpisodePull(event);
                handleNextEpisodePull(event);
                return false;
            });

        }catch(Exception e){
            e.printStackTrace();
        }

        next.setOnClickListener(v -> loadManga(manga.nextEp()));
        prev.setOnClickListener(v -> loadManga(manga.prevEp()));
        cut.setOnClickListener(v -> toggleAutoCut());

        pageBtn.setOnClickListener(v -> {
            PageItem current = stripAdapter.getCurrentVisiblePage();
            AlertDialog.Builder alert;
            if(dark) alert = new AlertDialog.Builder(context,R.style.darkDialog);
            else alert = new AlertDialog.Builder(context);

            alert.setTitle("페이지 선택\n(1~"+current.manga.getImgs(context).size()+")");
            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setRawInputType(Configuration.KEYBOARD_12KEY);
            alert.setView(input);
            alert.setPositiveButton("이동", (dialog, button) -> {
                //이동 시
                if (input.getText().length() > 0) {
                    int page = Integer.parseInt(input.getText().toString());
                    if (page < 1) page = 1;
                    if (page > current.manga.getImgs(context).size())
                        page = current.manga.getImgs(context).size();
                    manager.scrollToPage(new PageItem(page - 1, "", current.manga));
                    pageBtn.setText(page + "/" + current.manga.getImgs(context).size());
                }
            });

            alert.setNegativeButton("취소", (dialog, button) -> {
                //취소 시
            });
            alert.show();
        });

    }

    void refresh(){
        loadManga(manga);
    }

    void loadManga(Manga m, LoadMangaCallback callback){
        if(m == null)
            return;
        if(title != null)
            m.setTitle(title);
        this.manga = m;
        if(loader != null)
            loader.cancel(true);
        loader = new loadImages(m, callback,true);
        loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    void loadManga(Manga m){
        if(m == null){
            showPopup(context, "오류", "만화를 불러 오던중 오류가 발생했습니다.", (dialog, which) -> ViewerActivity.this.finish(), dialog -> ViewerActivity.this.finish());
            return;
        }
        if(stripAdapter!=null) stripAdapter.removeAll();
        if(m.isOnline()) {
            if(hasLoadedImages(m)) {
                setManga(m);
            } else {
                loadManga(m, m1 -> {
                    manga = m1;
                    setManga(m1);
                });
            }
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
            if(m.getImgs(context) == null || m.getImgs(context).size()==0) {
                showCaptchaPopup(m.getUrl(), context, p);
                return;
            }
            stripAdapter = new StripAdapter(context, m, autoCut, width,title, infiniteScrollCallback);

            refreshAdapter();
            bookmarkRefresh(m);
            refreshToolbar(m);
            updateIntent(m);
            prefetchNextEpisode(m);

        }catch (Exception e){
            Utils.showCaptchaPopup(m.getUrl(), context, e, p);
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
                pageBtn.setText(item.index+1 + "/" + item.manga.getImgs(context).size());
                toolbarTitle.setText(item.manga.getName());
                commentBtn.setOnClickListener(v -> {
                    Intent commentActivity = new Intent(context, CommentsActivity.class);
                    //create gson and put extra
                    Gson gson = new Gson();
                    commentActivity.putExtra("comments", gson.toJson(item.manga.getComments()));
                    commentActivity.putExtra("bestComments", gson.toJson(item.manga.getBestComments()));
                    commentActivity.putExtra("id", item.manga.getId());
                    startActivity(commentActivity);
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
        stripAdapter.setClickListener(() -> {
            // show/hide toolbar
            toggleToolbar();
        });
        manager.scrollToPage(page);
    }


//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        return imageZoomHelper.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev);
//    }


    @Override
    public void onBackPressed() {
        if(onBack != null) {
            onBack.run();
            return;
        }
        super.onBackPressed();
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
            setOnBackPressed(() -> {
                loadImages.super.cancel(true);
                finish();
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
                return m.fetch(httpClient);
            }else{
                return LOAD_OK;
            }
        }

        @Override
        protected void onPostExecute(Integer res) {
            if(isCancelled() || isFinishing())
                return;
            if(loader == this)
                loader = null;
            if(res == LOAD_CAPTCHA){
                //캡차 처리 팝업
                if(lockui) lockUi(false);
                resetOnBackPressed();
                showTokiCaptchaPopup(context, p);
                return;
            }

            if(lockui) lockUi(false);
            if (title == null)
                title = m.getTitle();
            super.onPostExecute(res);
            resetOnBackPressed();
            callback.post(m);
        }

        @Override
        protected void onCancelled(Integer res) {
            super.onCancelled(res);
            if(loader == this) {
                loader = null;
                if(lockui) lockUi(false);
                resetOnBackPressed();
            }
        }
    }

    private class prefetchImages extends AsyncTask<Void, Void, Integer> {
        Manga target;

        prefetchImages(Manga target) {
            this.target = target;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            if(target == null || !target.isOnline() || hasLoadedImages(target))
                return LOAD_OK;
            return target.fetch(httpClient);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(nextPrefetcher == this)
                nextPrefetcher = null;
            if(isCancelled() || isFinishing() || result == LOAD_CAPTCHA || !hasLoadedImages(target))
                return;
            preloadFirstPages(target);
            if(stripAdapter != null && manager != null
                    && shouldAttachNextEpisodeAhead(manager.findLastVisibleItemPosition(), manager.getItemCount()))
                attachNextEpisode(false);
        }

        @Override
        protected void onCancelled(Integer result) {
            super.onCancelled(result);
            if(nextPrefetcher == this)
                nextPrefetcher = null;
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
        // show/hide toolbar
        stripAdapter.setClickListener(this::toggleToolbar);
    }

    private void loadEpisodeAtBoundaryIfNeeded() {
        if(strip == null || manager == null || stripAdapter == null || manager.getItemCount() == 0 || previousEpisodePositioning)
            return;
        int first = manager.findFirstVisibleItemPosition();
        int last = manager.findLastVisibleItemPosition();
        int total = manager.getItemCount();
        if(first > 0) {
            previousBoundaryRequestedFromId = -1;
        }
        if(shouldAttachNextEpisodeAhead(last, total))
            attachNextEpisode(false);
        if(last >= total - 2 || !strip.canScrollVertically(1))
            attachNextEpisode(true);
    }

    private boolean shouldAttachNextEpisodeAhead(int lastVisiblePosition, int totalItems) {
        if(totalItems <= 0 || lastVisiblePosition == RecyclerView.NO_POSITION)
            return false;
        int preloadWindow = Math.min(NEXT_EPISODE_PRELOAD_LIMIT, Math.max(2, totalItems / 2));
        return lastVisiblePosition >= totalItems - preloadWindow;
    }

    private void handlePreviousEpisodePull(MotionEvent event) {
        if(strip == null || manager == null || stripAdapter == null)
            return;
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                topPullStartY = event.getY();
                topPullTriggered = false;
                topPullEligible = isAtViewerTop();
                topPullInProgress = true;
                previousPullDistancePx = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if(topPullEligible || isAtViewerTop())
                    previousPullDistancePx = Math.max(previousPullDistancePx, Math.max(0, (int)(event.getY() - topPullStartY)));
                if(topPullTriggered || previousEpisodeBoundaryLoading)
                    return;
                if(!topPullEligible && isAtViewerTop()) {
                    topPullEligible = true;
                }
                if(!topPullEligible)
                    return;
                float threshold = PREVIOUS_EPISODE_PULL_THRESHOLD_DP * getResources().getDisplayMetrics().density;
                if(event.getY() - topPullStartY >= threshold) {
                    topPullTriggered = true;
                    attachPreviousEpisode(false);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float releaseThreshold = PREVIOUS_EPISODE_PULL_THRESHOLD_DP * getResources().getDisplayMetrics().density;
                boolean releasePulledFromTop = event.getY() - topPullStartY >= releaseThreshold && isAtViewerTop();
                boolean shouldLoadPrevious = (topPullTriggered || releasePulledFromTop)
                        && !previousEpisodeBoundaryLoading && !previousEpisodePositioning;
                if(topPullEligible || isAtViewerTop())
                    previousPullDistancePx = Math.max(previousPullDistancePx, Math.max(0, (int)(event.getY() - topPullStartY)));
                topPullInProgress = false;
                topPullTriggered = false;
                topPullEligible = false;
                if(shouldLoadPrevious)
                    attachPreviousEpisode(true);
                break;
        }
    }

    private void handleNextEpisodePull(MotionEvent event) {
        if(strip == null || manager == null || stripAdapter == null)
            return;
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                bottomPullStartY = event.getY();
                bottomPullTriggered = false;
                bottomPullEligible = isAtViewerBottom();
                bottomPullInProgress = true;
                nextPullDistancePx = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if(bottomPullEligible || isAtViewerBottom())
                    nextPullDistancePx = Math.max(nextPullDistancePx, Math.max(0, (int)(bottomPullStartY - event.getY())));
                if(bottomPullTriggered || nextEpisodeBoundaryLoading)
                    return;
                if(!bottomPullEligible && isAtViewerBottom()) {
                    bottomPullEligible = true;
                    bottomPullStartY = event.getY();
                }
                if(!bottomPullEligible)
                    return;
                float threshold = PREVIOUS_EPISODE_PULL_THRESHOLD_DP * getResources().getDisplayMetrics().density;
                if(bottomPullStartY - event.getY() >= threshold) {
                    bottomPullTriggered = true;
                    attachNextEpisode(false);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                float releaseThreshold = PREVIOUS_EPISODE_PULL_THRESHOLD_DP * getResources().getDisplayMetrics().density;
                boolean releasePulledFromBottom = bottomPullStartY - event.getY() >= releaseThreshold && isAtViewerBottom();
                boolean shouldLoadNext = (bottomPullTriggered || releasePulledFromBottom)
                        && !nextEpisodeBoundaryLoading && !previousEpisodePositioning;
                if(bottomPullEligible || isAtViewerBottom())
                    nextPullDistancePx = Math.max(nextPullDistancePx, Math.max(0, (int)(bottomPullStartY - event.getY())));
                bottomPullInProgress = false;
                bottomPullTriggered = false;
                bottomPullEligible = false;
                if(shouldLoadNext)
                    attachNextEpisode(true);
                break;
        }
    }

    private boolean isAtViewerTop() {
        if(strip == null)
            return false;
        if(!strip.canScrollVertically(-1))
            return true;
        if(stripAdapter == null || manager == null)
            return false;
        PageItem page = stripAdapter.getCurrentVisiblePage();
        if(page == null || page.manga == null || page.index != 0)
            return false;
        int firstPagePosition = stripAdapter.findFirstPagePosition(page.manga);
        if(firstPagePosition == RecyclerView.NO_POSITION)
            return false;
        View firstPage = manager.findViewByPosition(firstPagePosition);
        return firstPage != null && firstPage.getTop() >= 0;
    }

    private boolean isAtViewerBottom() {
        if(strip == null)
            return false;
        if(!strip.canScrollVertically(1))
            return true;
        if(stripAdapter == null || manager == null)
            return false;
        PageItem page = stripAdapter.getCurrentVisiblePage();
        if(page == null || page.manga == null)
            return false;
        List<String> images = page.manga.getImgs(context);
        if(images == null || page.index != images.size() - 1)
            return false;
        int lastPagePosition = stripAdapter.findLastPagePosition(page.manga);
        if(lastPagePosition == RecyclerView.NO_POSITION)
            return false;
        View lastPage = manager.findViewByPosition(lastPagePosition);
        return lastPage != null && lastPage.getBottom() <= strip.getHeight();
    }

    private void attachPreviousEpisode(boolean jumpToEpisode) {
        if(previousEpisodePositioning && !previousEpisodeBoundaryLoading)
            return;
        PageItem page = stripAdapter.getCurrentVisiblePage();
        if(page == null || page.manga == null || !page.manga.isOnline())
            return;
        Manga target = page.manga.prevEp();
        if(target == null)
            return;
        int loadedPosition = stripAdapter.findLastPagePosition(target);
        if(loadedPosition != RecyclerView.NO_POSITION) {
            previousBoundaryRequestedFromId = -1;
            return;
        }
        if(previousEpisodeBoundaryLoading)
        {
            if(jumpToEpisode)
                previousEpisodeBoundaryJumpPending = true;
            return;
        }
        if(previousBoundaryRequestedFromId == page.manga.getId())
            return;
        previousBoundaryRequestedFromId = page.manga.getId();
        previousEpisodeBoundaryJumpPending = jumpToEpisode;
        previousEpisodeBoundaryLoading = true;
        infiniteScrollCallback.prevEp(new InfiniteLoadCallback() {
            @Override
            public void prevLoaded(Manga m) {
                previousEpisodeBoundaryLoading = false;
                boolean shouldJump = jumpToEpisode || previousEpisodeBoundaryJumpPending;
                previousEpisodeBoundaryJumpPending = false;
                if(m == null || strip == null || stripAdapter == null || isFinishing()) {
                    previousEpisodePositioning = false;
                    return;
                }
                strip.post(() -> {
                    int position = stripAdapter.findLastPagePosition(m);
                    if(shouldJump && position != RecyclerView.NO_POSITION) {
                        previousBoundaryRequestedFromId = -1;
                    }
                    previousEpisodePositioning = false;
                });
            }

            @Override
            public void nextLoaded(Manga m) {
            }
        }, page.manga);
    }

    private void attachNextEpisode(boolean jumpToEpisode) {
        if(previousEpisodeBoundaryLoading || previousEpisodePositioning)
            return;
        PageItem page = stripAdapter.getCurrentVisiblePage();
        if(page == null || page.manga == null || !page.manga.isOnline())
            return;
        Manga target = page.manga.nextEp();
        if(target == null)
            return;
        int loadedPosition = stripAdapter.findFirstPagePosition(target);
        if(loadedPosition != RecyclerView.NO_POSITION) {
            return;
        }
        if(nextEpisodeBoundaryLoading)
        {
            if(jumpToEpisode)
                nextEpisodeBoundaryJumpPending = true;
            return;
        }
        nextEpisodeBoundaryJumpPending = jumpToEpisode;
        nextEpisodeBoundaryLoading = true;
        infiniteScrollCallback.nextEp(new InfiniteLoadCallback() {
            @Override
            public void prevLoaded(Manga m) {
            }

            @Override
            public void nextLoaded(Manga m) {
                nextEpisodeBoundaryLoading = false;
                nextEpisodeBoundaryJumpPending = false;
                if(m == null || strip == null || stripAdapter == null || isFinishing())
                    return;
            }
        }, page.manga);
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
            pageBtn.setText(page.index+1+"/"+page.manga.getImgs(context).size());
    }

    private void prefetchNextEpisode(Manga current) {
        if(current == null || !current.isOnline() || eps == null)
            return;
        int index = eps.indexOf(current);
        if(index <= 0)
            return;
        Manga target = eps.get(index - 1);
        if(hasLoadedImages(target))
            return;
        if(nextPrefetcher != null)
            nextPrefetcher.cancel(true);
        nextPrefetcher = new prefetchImages(target);
        nextPrefetcher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean hasLoadedImages(Manga target) {
        try {
            if(target == null)
                return false;
            List<String> loadedImages = target.getImgs(context);
            return loadedImages != null && loadedImages.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void preloadFirstPages(Manga target) {
        if(target == null || !target.isOnline())
            return;
        List<String> images = target.getImgs(context);
        int limit = Math.min(NEXT_EPISODE_PRELOAD_LIMIT, images.size());
        for(int i = 0; i < limit; i++)
            Glide.with(context)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .load(Utils.getGlideUrl(images.get(i), target.getBaseMode()))
                    .preload();
    }

    private void appendMangaWhenIdle(Manga target, Runnable afterAppend) {
        if(target == null || strip == null)
            return;
        strip.post(() -> {
            if(stripAdapter != null && !isFinishing()) {
                if(!stripAdapter.hasMangaLoaded(target)) {
                    stripAdapter.appendManga(target);
                    applyPendingBoundaryScroll(false);
                }
                if(afterAppend != null)
                    afterAppend.run();
            }
        });
    }

    private void insertMangaWhenIdle(Manga target, Runnable afterInsert) {
        if(target == null || strip == null)
            return;
        strip.post(() -> {
            if(stripAdapter != null && !isFinishing()) {
                if(!stripAdapter.hasMangaLoaded(target)) {
                    stripAdapter.insertManga(target);
                    applyPendingBoundaryScroll(true);
                }
                if(afterInsert != null)
                    afterInsert.run();
            }
        });
    }

    private void applyPendingBoundaryScroll(boolean previous) {
        if(strip == null)
            return;
        int distance = previous ? previousPullDistancePx : nextPullDistancePx;
        previousPullDistancePx = 0;
        nextPullDistancePx = 0;
        if(distance <= 0)
            return;
        strip.post(() -> {
            if(!isFinishing())
                strip.scrollBy(0, previous ? -distance : distance);
        });
    }

    private void cancelActiveEpisodeLoader() {
        if(loader != null)
            loader.cancel(true);
        previousEpisodeBoundaryLoading = false;
        nextEpisodeBoundaryLoading = false;
        previousEpisodeBoundaryJumpPending = false;
        nextEpisodeBoundaryJumpPending = false;
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
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CAPTCHA) {
            refresh();
        }
    }

    @Override
    protected void onDestroy() {
        if(loader != null)
            loader.cancel(true);
        if(nextPrefetcher != null)
            nextPrefetcher.cancel(true);
        super.onDestroy();
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

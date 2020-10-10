package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import com.google.android.material.appbar.AppBarLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.melun.mangaview.R;
import ml.melun.mangaview.StripLayoutManager;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.adapter.CustomSpinnerAdapter;
import ml.melun.mangaview.adapter.StripAdapter;
import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.getScreenSize;
import static ml.melun.mangaview.Utils.hideSpinnerDropDown;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;

public class ViewerActivity extends AppCompatActivity {
    String name;
    int id;
    Manga manga;
    RecyclerView strip;
    Context context = this;
    StripAdapter stripAdapter;
    androidx.appcompat.widget.Toolbar toolbar;
    boolean toolbarshow = true;
    TextView toolbarTitle;
    AppBarLayout appbar, appbarBottom;
    int viewerBookmark = 0;
    LinearLayoutManager manager;
    ImageButton next, prev;
    Button cut, pageBtn;
    List<Manga> eps;
    int index = -1;
    Title title;
    boolean autoCut = false;
    List<String> imgs;
    boolean dark;
    Intent result;
    ImageButton commentBtn;
    int seed = 0;
    int epsCount = 0;
    int width=0;
    Intent intent;
    boolean captchaChecked = false;
    Spinner spinner;
    CustomSpinnerAdapter spinnerAdapter;
    InfiniteScrollCallback infiniteScrollCallback;

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

        infiniteScrollCallback = new InfiniteScrollCallback() {
            @Override
            public Manga prevEp(Runnable callback, Manga curm) {
                int i = eps.indexOf(curm);
                if(i<eps.size()-1) {
                    new loadImages(eps.get(i+1), new LoadImagesCallback() {
                        @Override
                        public void post(Manga m) {
                            if(m.getImgs().size()>0)
                                stripAdapter.insertManga(m);
                            callback.run();
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    return eps.get(i+1);
                }else{
                    callback.run();
                    return null;
                }
            }

            @Override
            public Manga nextEp(Runnable callback, Manga curm) {
                int i = eps.indexOf(curm);
                if(i>0) {
                    new loadImages(eps.get(i-1), new LoadImagesCallback() {
                        @Override
                        public void post(Manga m) {
                            if(m.getImgs().size()>0)
                                stripAdapter.appendManga(m);
                            callback.run();
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    return eps.get(i-1);
                }else{
                    callback.run();
                    return null;
                }
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

            name = manga.getName();
            id = manga.getId();

            toolbarTitle.setText(name);
            viewerBookmark = p.getViewerBookmark(manga);

            strip = this.findViewById(R.id.strip);
            manager = new StripLayoutManager(this);
            manager.setOrientation(LinearLayoutManager.VERTICAL);
            strip.setLayoutManager(manager);
            if(intent.getBooleanExtra("recent",false)){
                Intent resultIntent = new Intent();
                setResult(RESULT_OK,resultIntent);
            }
            spinnerAdapter = new CustomSpinnerAdapter(context);
            spinnerAdapter.setListener(new CustomSpinnerAdapter.CustomSpinnerListener() {
                @Override
                public void onClick(int position) {
                    if(index!= position) {
                        lockUi(true);
                        index = position;
                        manga = eps.get(index);
                        id = manga.getId();
                        name = manga.getName();
                        spinner.setSelection(position, true);
                        hideSpinnerDropDown(spinner);
                        if(manga.isOnline())
                            refresh();
                        else
                            reloadManga(manga);
                    }else {
                        spinner.setSelection(position, true);
                        hideSpinnerDropDown(spinner);
                    }
                }
            });
            spinner.setAdapter(spinnerAdapter);

            if(!manga.isOnline()){
                // is offline
                //load local imgs

                commentBtn.setVisibility(View.GONE);


                reloadManga(manga);

            }else {
                refresh();
            }
            commentBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent commentActivity = new Intent(context, CommentsActivity.class);
                    //create gson and put extra
                    Gson gson = new Gson();
                    commentActivity.putExtra("comments", gson.toJson(manga.getComments()));
                    commentActivity.putExtra("bestComments", gson.toJson(manga.getBestComments()));
                    commentActivity.putExtra("id", manga.getId());
                    startActivity(commentActivity);
                }
            });
            strip.setItemAnimator(null);
            strip.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if(strip.getLayoutManager().getItemCount()>0) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            int firstVisible = ((StripLayoutManager) strip.getLayoutManager()).findFirstVisibleItemPosition();
                            viewerBookmark = firstVisible;
                        } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                            if (toolbarshow) {
                                toggleToolbar();
                            }
                        }
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                }
            });
        }catch(Exception e){
            e.printStackTrace();
        }

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(eps!=null && index>0) {
                    lockUi(true);
                    index--;
                    manga = eps.get(index);
                    id = manga.getId();
                    name = manga.getName();
                    if(manga.isOnline())
                        refresh();
                    else
                        reloadManga(manga);
                }
            }
        });
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(eps!=null && index<eps.size()-1) {
                    lockUi(true);
                    index++;
                    manga = eps.get(index);
                    id = manga.getId();
                    name = manga.getName();
                    if(manga.isOnline())
                        refresh();
                    else
                        reloadManga(manga);
                }
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
                AlertDialog.Builder alert;
                if(dark) alert = new AlertDialog.Builder(context,R.style.darkDialog);
                else alert = new AlertDialog.Builder(context);

                alert.setTitle("페이지 선택\n(1~"+imgs.size()+")");
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
                            if (page > imgs.size()) page = imgs.size();
                            viewerBookmark = page - 1;
                            if(autoCut) strip.scrollToPosition(viewerBookmark*2);
                            else strip.scrollToPosition(viewerBookmark);
                            pageBtn.setText(viewerBookmark+1+"/"+imgs.size());
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
        if(stripAdapter!=null) stripAdapter.removeAll();
        loadImages l = new loadImages(manga, new LoadImagesCallback() {
            @Override
            public void post(Manga m) {
                manga = m;
                reloadManga(m);
            }
        });
        l.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if(keyCode == p.getPrevPageKey() || keyCode == p.getNextPageKey()) {
            if (keyCode == p.getNextPageKey()) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (viewerBookmark < stripAdapter.getItemCount() - 1)
                        strip.scrollToPosition(++viewerBookmark);
                }
            } else if (keyCode == p.getPrevPageKey()) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (viewerBookmark > 0) strip.scrollToPosition(--viewerBookmark);
                }
            }
            if(manga.useBookmark()) {
                if (viewerBookmark > 0 && viewerBookmark < stripAdapter.getItemCount() - 1) {
                    p.setViewerBookmark(manga, viewerBookmark);
                } else p.removeViewerBookmark(manga);
            }
            if(toolbarshow) toggleToolbar();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public void toggleToolbar(){
        //attrs = getWindow().getAttributes();
        if(toolbarshow){
            appbar.animate().translationY(-appbar.getHeight());
            appbarBottom.animate().translationY(+appbarBottom.getHeight());
            toolbarshow=false;
        }
        else {
            pageBtn.setText("0 /"+imgs.size());
            appbar.animate().translationY(0);
            appbarBottom.animate().translationY(0);
            toolbarshow=true;
        }
        //getWindow().setAttributes(attrs);
    }

    public void toggleAutoCut(){
        if(autoCut){
            autoCut = false;
            cut.setBackgroundResource(R.drawable.button_bg);
            //viewerBookmark /= 2;
        } else{
            autoCut = true;
            cut.setBackgroundResource(R.drawable.button_bg_on);
            //viewerBookmark *= 2;
        }
        stripAdapter = new StripAdapter(context, manga, autoCut, seed, id, width, infiniteScrollCallback);
        stripAdapter.preloadAll();
        strip.setAdapter(stripAdapter);
        stripAdapter.setClickListener(new StripAdapter.ItemClickListener() {
            public void onItemClick() {
                // show/hide toolbar
                toggleToolbar();
            }
        });
        if(autoCut) strip.getLayoutManager().scrollToPosition(viewerBookmark*2);
        else strip.getLayoutManager().scrollToPosition(viewerBookmark);
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
        LoadImagesCallback callback;
        Manga m;
        public loadImages(Manga m, LoadImagesCallback callback){
            this.m = m;
            this.callback = callback;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            setOnBackPressed(new Runnable() {
                @Override
                public void run(){
                    loadImages.super.cancel(true);
                    finish();
                }
            });
        }

        protected Integer doInBackground(Void... params) {
            Login login = p.getLogin();
            Map<String, String> cookie = new HashMap<>();
            if(login !=null) {
                String php = p.getLogin().getCookie();
                login.buildCookie(cookie);
                cookie.put("last_wr_id",String.valueOf(id));
                cookie.put("last_percent",String.valueOf(1));
                cookie.put("last_page",String.valueOf(0));
            }
            m.fetch(httpClient);
            if(title == null)
                title = m.getTitle();
            return 0;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            resetOnBackPressed();
            callback.post(m);
        }
    }

    public void appendManga(){

    }

    public void reloadManga(Manga m){
        this.manga = m;
        this.imgs = m.getImgs();
        try {
            lockUi(false);
            if(m.getImgs() == null || m.getImgs().size()==0) {
                showCaptchaPopup(context, p);
                return;
            }
            stripAdapter = new StripAdapter(context, m, autoCut, m.getSeed(), id, width, infiniteScrollCallback);

            refreshAdapter();
            bookmarkRefresh();
            refreshToolbar();
            updateIntent();

        }catch (Exception e){
            StackTraceElement[] stack = e.getStackTrace();
            String message = "";
            for(StackTraceElement s : stack){
                message +=s.toString()+'\n';
            }
            Utils.showCaptchaPopup(context, e, p);
            e.printStackTrace();
        }
    }

    public void bookmarkRefresh(){
        if(manga.useBookmark()) {
            viewerBookmark = p.getViewerBookmark(manga);
            if (viewerBookmark != -1) {
                strip.scrollToPosition(viewerBookmark);
            }
            if (!autoCut) strip.scrollToPosition(viewerBookmark);
            else strip.scrollToPosition(viewerBookmark * 2);
            if (manga.useBookmark()) {
                // if manga is online or has title.gson
                if (title == null) title = manga.getTitle();
                p.addRecent(title);
                if (id > 0) p.setBookmark(title, id);
            }
        }else
            viewerBookmark = 0;
    }

    public void updateIntent(){
        result = new Intent();
        result.putExtra("id", id);
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

    public void refreshToolbar(){
        eps = manga.getEps();
        if(eps == null || eps.size() == 0){
            eps = title.getEps();
        }

        List<String> epsName = new ArrayList<>();
        for(int i=0; i<eps.size(); i++){
            if(id>0) {
                if (eps.get(i).getId() == id)
                    index = i;
            }else{
                if(eps.get(i).getName().equals(name))
                    index = i;
            }
            epsName.add(eps.get(i).getName());
        }
        //refresh spinner

        spinnerAdapter.setData(epsName, index);
        spinner.setSelection(index);



        toolbarTitle.setText(manga.getName());
        toolbarTitle.setSelected(true);

        if(index==0){
            next.setEnabled(false);
            next.setColorFilter(Color.BLACK);
        }
        else {
            next.setEnabled(true);
            next.setColorFilter(null);
        }
        if(index==eps.size()-1) {
            prev.setEnabled(false);
            prev.setColorFilter(Color.BLACK);
        }
        else {
            prev.setEnabled(true);
            prev.setColorFilter(null);
        }

        pageBtn.setText(viewerBookmark+1+"/"+imgs.size());
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
        Manga nextEp(Runnable callback, Manga curm);
        Manga prevEp(Runnable callback, Manga curm);
    }
    private interface LoadImagesCallback{
        void post(Manga m);
    }

}

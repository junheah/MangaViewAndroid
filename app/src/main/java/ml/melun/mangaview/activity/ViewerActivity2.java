package ml.melun.mangaview.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import androidx.annotation.Nullable;

import com.google.android.material.appbar.AppBarLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.melun.mangaview.R;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.adapter.CustomSpinnerAdapter;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.ui.CustomSpinner;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.getScreenSize;
import static ml.melun.mangaview.Utils.hideSpinnerDropDown;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;

public class ViewerActivity2 extends AppCompatActivity {
    Boolean dark, toolbarshow=true, reverse, touch=true, stretch, leftRight;
    Context context = this;
    String name;
    int id;
    Manga manga;
    ImageButton next, prev, commentBtn;
    androidx.appcompat.widget.Toolbar toolbar;
    Button pageBtn, nextPageBtn, prevPageBtn, touchToggleBtn;
    AppBarLayout appbar, appbarBottom;
    TextView toolbarTitle;
    int viewerBookmark = 0;
    List<String> imgs;
    List<Integer> types;
    ProgressDialog pd;
    List<Manga> eps;
    int index;
    Title title;
    ImageView frame;
    int type=-1;
    Bitmap imgCache, preloadImg;
    Intent result;
    AlertDialog.Builder alert;
    int width = 0;
    Intent intent;
    boolean captchaChecked = false;
    ImageButton toolbar_toggleBtn;
    CustomSpinner spinner;
    CustomSpinnerAdapter spinnerAdapter;
    Decoder d;
    boolean nextEpisodeVisible = false;
    View nextEpisode;

    @Override
    protected void onResume() {
        super.onResume();
        if(toolbarshow) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("manga", new Gson().toJson(manga));
        outState.putString("title", new Gson().toJson(title));
        super.onSaveInstanceState(outState);
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
    protected void onCreate(Bundle savedInstanceState) {
        dark = p.getDarkTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer2);

        next = this.findViewById(R.id.toolbar_next);
        prev = this.findViewById(R.id.toolbar_previous);
        toolbar = this.findViewById(R.id.viewerToolbar);
        appbar = this.findViewById(R.id.viewerAppbar);
        toolbarTitle = this.findViewById(R.id.toolbar_title);
        appbarBottom = this.findViewById(R.id.viewerAppbarBottom);
        reverse = p.getReverse();
        frame = this.findViewById(R.id.viewer_image);
        pageBtn = this.findViewById(R.id.viewerBtn1);
        toolbar_toggleBtn = this.findViewById(R.id.toolbar_toggleBtn);
        pageBtn.setText("-/-");
        leftRight = p.getLeftRight();
        spinner = this.findViewById(R.id.toolbar_spinner);
        nextEpisode = this.findViewById(R.id.viewerNextEpisode);

        nextEpisode.setVisibility(View.GONE);

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

                System.out.println(windowInsetsCompat.getStableInsetTop() +" && " + windowInsetsCompat.getSystemWindowInsetTop() + " _ " +windowInsetsCompat.getStableInsetBottom() +" && " + windowInsetsCompat.getSystemWindowInsetBottom());

                //System.out.println(ci + " : " + statusBarHeight);
                appbar.setPadding(0,ci > statusBarHeight ? ci : statusBarHeight,0,0);
                view.setPadding(windowInsetsCompat.getStableInsetLeft(),0,windowInsetsCompat.getStableInsetRight(),windowInsetsCompat.getStableInsetBottom());
                return windowInsetsCompat;
            }
        });

        this.findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        spinnerAdapter = new CustomSpinnerAdapter(context);
        spinnerAdapter.setListener(new CustomSpinnerAdapter.CustomSpinnerListener() {
            @Override
            public void onClick(Manga m, int i) {
                lockUi(true);
                spinner.setSelection(m);
                index = i;
                manga = m;
                hideSpinnerDropDown(spinner);
                loadManga(m);
            }
        });
        spinner.setAdapter(spinnerAdapter);

        if(leftRight){
            // button reverse
            nextPageBtn = this.findViewById(R.id.leftButton);
            prevPageBtn = this.findViewById(R.id.rightButton);
        }else{
            nextPageBtn = this.findViewById(R.id.rightButton);
            prevPageBtn = this.findViewById(R.id.leftButton);
        }

        refreshPageControlButton();

        touchToggleBtn = this.findViewById(R.id.viewerBtn2);
        touchToggleBtn.setText("입력 제한");
        commentBtn = this.findViewById(R.id.commentButton);
        stretch = p.getStretch();

        //refreshBtn = this.findViewById(R.id.refreshButton);
        if(stretch) frame.setScaleType(ImageView.ScaleType.FIT_XY);
        width = getScreenSize(getWindowManager().getDefaultDisplay());

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

//        refreshbtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                refreshImage();
//            }
//        });


        if(intent.getBooleanExtra("recent",false)){
            Intent resultIntent = new Intent();
            setResult(RESULT_OK,resultIntent);
        }
        if(!manga.isOnline()) {
            reloadManga();
            commentBtn.setVisibility(View.GONE);
        }else{
            //if online
            //fetch imgs
            loadImages l = new loadImages();
            l.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        nextPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(touch) nextPage();
            }
        });
        prevPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(touch) prevPage();
            }
        });

        toolbar_toggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleToolbar();
            }
        });

        touchToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(touch) {
                    touch = false;
                    touchToggleBtn.setBackgroundResource(R.drawable.button_bg_on);
                }
                else{
                    touch = true;
                    touchToggleBtn.setBackgroundResource(R.drawable.button_bg);
                }
            }
        });

        pageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                            refreshImage();
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

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(eps!=null && index>0) {
                    lockUi(true);
                    index--;
                    manga = eps.get(index);
                    id = manga.getId();
                    name = manga.getName();
                    loadManga(manga);
                }else
                    Toast.makeText(context, "마지막화 입니다", Toast.LENGTH_SHORT).show();

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
                    loadManga(manga);
                }
            }
        });

        View.OnLongClickListener tbToggle = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //touched = true;
                toggleToolbar();
                return true;
            }
        };
        nextPageBtn.setOnLongClickListener(tbToggle);
        prevPageBtn.setOnLongClickListener(tbToggle);

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

    }

    void refreshPageControlButton(){
        if(p.getPageControlButtonOffset()!= -1){
            Button left = this.findViewById(R.id.leftButton);
            ViewGroup.LayoutParams params = left.getLayoutParams();
            params.width = (int)(p.getPageControlButtonOffset() * Utils.getScreenWidth(getWindowManager().getDefaultDisplay()));
            left.setLayoutParams(params);
        }
    }


    void nextPage(){
        //refreshbtn.setVisibility(View.VISIBLE);
        if(viewerBookmark==imgs.size()-1 && (type==-1 || type==1)){
            //end of manga
            //refreshbtn.setVisibility(View.INVISIBLE);
            // 다음화 로드
            if(nextEpisodeVisible) {
                next.performClick();
            }
            toggleNextEpisode();

        }else if(type==0){
            //is two page, current pos: right
            //dont add page
            //only change type
            //refreshbtn.setVisibility(View.INVISIBLE);
            type = 1;
            int width = imgCache.getWidth();
            int height = imgCache.getHeight();
            if(reverse) frame.setImageBitmap(Bitmap.createBitmap(imgCache, width/2, 0, width / 2, height));
            else frame.setImageBitmap(Bitmap.createBitmap(imgCache, 0, 0, width / 2, height));

        }else{
            //is single page OR unidentified
            //add page
            //has to check if twopage
            viewerBookmark++;
            try {
                String image = imgs.get(viewerBookmark);

                //placeholder
                frame.setImageResource(R.drawable.placeholder);
                Glide.with(context)
                        .asBitmap()
                        .load(image)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                //
                            }

                            @Override
                            public void onResourceReady(Bitmap bitmap,
                                                        Transition<? super Bitmap> transition) {
                                //refreshbtn.setVisibility(View.INVISIBLE);
                                bitmap = d.decode(bitmap,width);
                                int width = bitmap.getWidth();
                                int height = bitmap.getHeight();
                                if(width>height){
                                    imgCache = bitmap;
                                    type=0;
                                    if(reverse) frame.setImageBitmap(Bitmap.createBitmap(imgCache,0,0,width/2,height));
                                    else frame.setImageBitmap(Bitmap.createBitmap(imgCache,width/2,0,width/2,height));
                                }else{
                                    type=-1;
                                    frame.setImageBitmap(bitmap);
                                }
                                preload();
                            }
                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                if(imgs.size()>0) {
                                    viewerBookmark--;
                                    nextPage();
                                }
                            }
                        });

            }catch (Exception e){
                e.printStackTrace();
                viewerBookmark--;
            }
        }
        if(manga.useBookmark()) {
            p.setViewerBookmark(manga, viewerBookmark);
            if (imgs.size() - 1 == viewerBookmark) p.removeViewerBookmark(manga);
        }
        updatePageIndex();
    }

    void prevPage(){
        //refreshbtn.setVisibility(View.VISIBLE);
        if(nextEpisodeVisible){
            toggleNextEpisode();
        }else if(viewerBookmark==0 && (type==-1 || type==0)){
            //start of manga
            //refreshbtn.setVisibility(View.INVISIBLE);
        } else if(type==1){
            //is two page, current pos: left
            //refreshbtn.setVisibility(View.INVISIBLE);
            type = 0;
            int width = imgCache.getWidth();
            int height = imgCache.getHeight();
            if(reverse) frame.setImageBitmap(Bitmap.createBitmap(imgCache, 0, 0, width / 2, height));
            else frame.setImageBitmap(Bitmap.createBitmap(imgCache, width/2, 0, width / 2, height));
        }else{
            //is single page OR unidentified
            //decrease page
            //has to check if twopage
            viewerBookmark--;
            try {
                String image = imgs.get(viewerBookmark);

                //placeholder
                frame.setImageResource(R.drawable.placeholder);
                Glide.with(context)
                        .asBitmap()
                        .load(image)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                                bitmap = d.decode(bitmap, width);
                                //refreshbtn.setVisibility(View.INVISIBLE);
                                int width = bitmap.getWidth();
                                int height = bitmap.getHeight();
                                if(width>height){
                                    imgCache = bitmap;
                                    type=1;
                                    if(reverse) frame.setImageBitmap(Bitmap.createBitmap(imgCache, width/2, 0, width / 2, height));
                                    else frame.setImageBitmap(Bitmap.createBitmap(imgCache,0,0,width/2,height));
                                }else{
                                    type=-1;
                                    frame.setImageBitmap(bitmap);
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                if(imgs.size()>0) {
                                    viewerBookmark++;
                                    prevPage();
                                }
                            }
                        });
            }catch (Exception e){
                e.printStackTrace();
                viewerBookmark++;
            }
        }
        if(manga.useBookmark()) {
            p.setViewerBookmark(manga, viewerBookmark);
            if (0 == viewerBookmark) p.removeViewerBookmark(manga);
        }
        updatePageIndex();

    }



    void refreshImage(){
        frame.setImageResource(R.drawable.placeholder);
        //refreshbtn.setVisibility(View.VISIBLE);
        try {
            String image = imgs.get(viewerBookmark);
            //placeholder
            //frame.setImageResource(R.drawable.placeholder);
            Glide.with(context)
                    .asBitmap()
                    .load(image)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }

                        @Override
                        public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                            //refreshbtn.setVisibility(View.INVISIBLE);
                            bitmap = d.decode(bitmap, width);
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            if (width > height) {
                                imgCache = bitmap;
                                type = 0;
                                if (reverse)
                                    frame.setImageBitmap(Bitmap.createBitmap(imgCache, 0, 0, width / 2, height));
                                else
                                    frame.setImageBitmap(Bitmap.createBitmap(imgCache, width / 2, 0, width / 2, height));
                            } else {
                                type = -1;
                                frame.setImageBitmap(bitmap);
                            }
                            preload();
                        }
                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            if(imgs.size()>0) {
                                refreshImage();
                            }
                        }
                    });
        }catch(Exception e) {
            Utils.showCaptchaPopup(context, e, p);
        }
    }

    void preload(){
        if(viewerBookmark<imgs.size()-1) {
            String image = imgs.get(viewerBookmark+1);
            Glide.with(context)
                    .asBitmap()
                    .load(image)
                    .addListener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            if (imgs.size() > 0) {
                                preload();
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .preload();
        }
    }
    void updatePageIndex(){
        pageBtn.setText(viewerBookmark+1+"/"+imgs.size());
        boolean lastPage = viewerBookmark == imgs.size()-1;
        boolean firstPage = viewerBookmark == 0;
        if(toolbarshow && !lastPage)
            toggleToolbar();
        else if(lastPage && !toolbarshow)
            toggleToolbar();
    }

    void toggleNextEpisode(){
        if(nextEpisodeVisible) {
            nextEpisodeVisible = false;
            nextEpisode.setVisibility(View.GONE);
        }else{
            nextEpisodeVisible = true;
            nextEpisode.setVisibility(View.VISIBLE);
        }
    }

    public void toggleToolbar(){
        //attrs = getWindow().getAttributes();
        if(toolbarshow){
            //hide toolbar
            appbar.animate().translationY(-appbar.getHeight());
            appbarBottom.animate().translationY(+appbarBottom.getHeight());
            toolbarshow=false;
            toolbar_toggleBtn.setVisibility(View.VISIBLE);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        else {
            //show toolbar
            appbar.animate().translationY(0);
            appbarBottom.animate().translationY(0);
            toolbarshow=true;
            toolbar_toggleBtn.setVisibility(View.GONE);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
        //getWindow().setAttributes(attrs);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == p.getNextPageKey() ) {
            if(event.getAction() == KeyEvent.ACTION_UP)
                nextPage();
            return true;
        } else if(keyCode == p.getPrevPageKey()) {
            if(event.getAction() == KeyEvent.ACTION_UP)
                prevPage();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private class loadImages extends AsyncTask<Void,String,Integer> {
        protected void onProgressUpdate(String... values) {
            pd.setMessage(values[0]);
        }
        protected void onPreExecute() {
            super.onPreExecute();
            if(dark) pd = new ProgressDialog(context, R.style.darkDialog);
            else pd = new ProgressDialog(context);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if(keyCode == KeyEvent.KEYCODE_BACK){
                        loadImages.super.cancel(true);
                        pd.dismiss();
                        finish();
                    }
                    return true;
                }
            });
            pd.show();
        }

        protected Integer doInBackground(Void... params) {
            manga.setListener(new Manga.Listener() {
                @Override
                public void setMessage(String msg) {
                    publishProgress(msg);
                }
            });
            Login login = p.getLogin();
            Map<String, String> cookie = new HashMap<>();
            if(login !=null) {
                String php = p.getLogin().getCookie();
                login.buildCookie(cookie);
                cookie.put("last_wr_id",String.valueOf(id));
                cookie.put("last_percent",String.valueOf(1));
                cookie.put("last_page",String.valueOf(0));
            }
            manga.fetch(httpClient);
            if(title == null)
                title = manga.getTitle();
            return 0;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            reloadManga();

            if (pd.isShowing()) {
                pd.dismiss();
            }
        }
    }

    public void reloadManga(){
        try{
            lockUi(false);
            imgs = manga.getImgs();
            if(imgs == null || imgs.size()==0) {
                showCaptchaPopup(context, p);
                return;
            }
            d = new Decoder(manga.getSeed(), manga.getId());
            bookmarkRefresh();
            refreshToolbar();
            updateIntent();
            refreshImage();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void bookmarkRefresh(){
        if(manga.useBookmark()) {
            viewerBookmark = p.getViewerBookmark(manga);
            // if manga is online or has title.gson
            p.addRecent(title);
            p.setBookmark(title, id);
        }else
            viewerBookmark = 0;
    }

    public void updateIntent(){
        result = new Intent();
        result.putExtra("id", id);
        setResult(RESULT_OK, result);
    }

    public void loadManga(Manga m){
        if(m!=null) {
            manga = m;
            id = manga.getId();
            if (m.isOnline())
                refresh();
            else
                reloadManga();
        }
    }

    public void refresh(){
        captchaChecked = false;
        loadImages l = new loadImages();
        l.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void refreshToolbar(){
        eps = manga.getEps();
        if(eps == null || eps.size() == 0){
            eps = title.getEps();
        }
        for(int i=0; i<eps.size(); i++){
            if(eps.get(i).equals(manga)){
                index = i;
                break;
            }
        }
        //refresh spinner
        spinnerAdapter.setData(eps, manga);
        spinner.setSelection(manga);

        toolbarTitle.setText(manga.getName());
        toolbarTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        toolbarTitle.setMarqueeRepeatLimit(-1);
        toolbarTitle.setSingleLine(true);
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_CAPTCHA) {
            refresh();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refreshPageControlButton();
    }

    void lockUi(Boolean lock){
        toolbar_toggleBtn.setEnabled(!lock);
        commentBtn.setEnabled(!lock);
        next.setEnabled(!lock);
        prev.setEnabled(!lock);
        pageBtn.setEnabled(!lock);
        touchToggleBtn.setEnabled(!lock);
        nextPageBtn.setEnabled(!lock);
        prevPageBtn.setEnabled(!lock);
        spinner.setEnabled(!lock);
    }


}
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
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

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
import ml.melun.mangaview.adapter.CustomSpinnerAdapter;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.getScreenSize;
import static ml.melun.mangaview.Utils.hideSpinnerDropDown;
import static ml.melun.mangaview.Utils.showErrorPopup;
import static ml.melun.mangaview.Utils.showPopup;

public class ViewerActivity2 extends AppCompatActivity {
    Boolean dark, volumeControl, toolbarshow=true, reverse, touch=true, stretch, leftRight;
    Context context = this;
    String name;
    int id;
    Manga manga;
    ImageButton next, prev, commentBtn;
    androidx.appcompat.widget.Toolbar toolbar;
    Button pageBtn, nextPageBtn, prevPageBtn, touchToggleBtn;
    AppBarLayout appbar, appbarBottom;
    TextView toolbarTitle;
    int viewerBookmark = -1;
    List<String> imgs, imgs1;
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
    Spinner spinner;
    CustomSpinnerAdapter spinnerAdapter;
    Decoder d;
    Boolean error = false, useSecond = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDarkNoTitle);
        else setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer2);

        next = this.findViewById(R.id.toolbar_next);
        prev = this.findViewById(R.id.toolbar_previous);
        toolbar = this.findViewById(R.id.viewerToolbar);
        appbar = this.findViewById(R.id.viewerAppbar);
        toolbarTitle = this.findViewById(R.id.toolbar_title);
        appbarBottom = this.findViewById(R.id.viewerAppbarBottom);
        volumeControl = p.getVolumeControl();
        reverse = p.getReverse();
        frame = this.findViewById(R.id.viewer_image);
        pageBtn = this.findViewById(R.id.viewerBtn1);
        toolbar_toggleBtn = this.findViewById(R.id.toolbar_toggleBtn);
        pageBtn.setText("-/-");
        leftRight = p.getLeftRight();
        spinner = this.findViewById(R.id.toolbar_spinner);

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
                    if(manga.getMode() == 0)
                        refresh();
                    else
                        reloadManga();
                }else {
                    spinner.setSelection(position, true);
                    hideSpinnerDropDown(spinner);
                }
            }
        });
        spinner.setAdapter(spinnerAdapter);

        if(leftRight){
            nextPageBtn = this.findViewById(R.id.nextPageBtn2);
            prevPageBtn = this.findViewById(R.id.prevPageBtn2);
        }else{
            nextPageBtn = this.findViewById(R.id.nextPageBtn);
            prevPageBtn = this.findViewById(R.id.prevPageBtn);
        }
        nextPageBtn.setVisibility(View.VISIBLE);
        prevPageBtn.setVisibility(View.VISIBLE);

        touchToggleBtn = this.findViewById(R.id.viewerBtn2);
        touchToggleBtn.setText("입력 제한");
        commentBtn = this.findViewById(R.id.commentButton);
        stretch = p.getStretch();

        //refreshBtn = this.findViewById(R.id.refreshButton);
        if(stretch) frame.setScaleType(ImageView.ScaleType.FIT_XY);
        width = getScreenSize(getWindowManager().getDefaultDisplay());

        intent = getIntent();

        manga = new Gson().fromJson(intent.getStringExtra("manga"),new TypeToken<Manga>(){}.getType());
        title = new Gson().fromJson(intent.getStringExtra("title"),new TypeToken<Title>(){}.getType());

        name = manga.getName();
        id = manga.getId();

        toolbarTitle.setText(name);
        viewerBookmark = p.getViewerBookmark(id);

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
        if(manga.getMode() != 0) {
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
                if(index>0) {
                    lockUi(true);
                    index--;
                    manga = eps.get(index);
                    id = manga.getId();
                    name = manga.getName();
                    if(manga.getMode() == 0)
                        refresh();
                    else
                        reloadManga();
                }

            }
        });
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(index<eps.size()-1) {
                    lockUi(true);
                    index++;
                    manga = eps.get(index);
                    id = manga.getId();
                    name = manga.getName();
                    if(manga.getMode() == 0)
                        refresh();
                    else
                        reloadManga();
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

    void nextPage(){
        //refreshbtn.setVisibility(View.VISIBLE);
        if(viewerBookmark==imgs.size()-1 && (type==-1 || type==1)){
            //end of manga
            //refreshbtn.setVisibility(View.INVISIBLE);
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
                String image = useSecond && imgs1!=null && imgs1.size()>0 ? imgs1.get(viewerBookmark) : imgs.get(viewerBookmark);
                if(error && !useSecond){
                    image = image.indexOf("img.") > -1 ? image.replace("img.","s3.") : image.replace("://", "://s3.");
                }

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
                                    if(!error && !useSecond) {
                                        error= true;
                                    }else if(!useSecond && error){
                                        useSecond = true;
                                        error= false;
                                    }else{
                                        error = false;
                                        useSecond = false;
                                    }
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
        p.setViewerBookmark(id,viewerBookmark);
        if(imgs.size()-1==viewerBookmark) p.removeViewerBookmark(id);
        updatePageIndex();
    }

    void prevPage(){
        //refreshbtn.setVisibility(View.VISIBLE);
        if(viewerBookmark==0 && (type==-1 || type==0)){
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
                String image = useSecond && imgs1!=null && imgs1.size()>0 ? imgs1.get(viewerBookmark) : imgs.get(viewerBookmark);
                if(error && !useSecond){
                    image = image.indexOf("img.") > -1 ? image.replace("img.","s3.") : image.replace("://", "://s3.");
                }

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
                                    if(!error && !useSecond) {
                                        error = true;
                                    }else if(!useSecond && error){
                                        error = false;
                                        useSecond = true;
                                    }else{
                                        error = false;
                                        useSecond = false;
                                    }
                                    viewerBookmark++;
                                    prevPage();
                                }
                            }
                        });
            }catch (Exception e){
                viewerBookmark++;
            }
        }
        p.setViewerBookmark(id,viewerBookmark);
        if(0==viewerBookmark) p.removeViewerBookmark(id);
        updatePageIndex();

    }



    void refreshImage(){
        frame.setImageResource(R.drawable.placeholder);
        //refreshbtn.setVisibility(View.VISIBLE);
        try {
            String image = useSecond && imgs1!=null && imgs1.size()>0 ? imgs1.get(viewerBookmark) : imgs.get(viewerBookmark);
            if(error && !useSecond){
                image = image.indexOf("img.") > -1 ? image.replace("img.","s3.") : image.replace("://", "://s3.");

            }
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
                                if(!error && !useSecond) {
                                    error = true;
                                }else if(!useSecond && error){
                                    useSecond = true;
                                    error = false;
                                }else{
                                    error = false;
                                    useSecond = false;
                                }
                                refreshImage();
                            }
                        }
                    });
        }catch(Exception e) {
            showErrorPopup(context, e);
        }
    }

    void preload(){
        if(viewerBookmark<imgs.size()-1) {
            String image = useSecond && imgs1!=null && imgs1.size()>0 ? imgs1.get(viewerBookmark+1) : imgs.get(viewerBookmark+1);
            if(error && !useSecond){
                image = image.indexOf("img.") > -1 ? image.replace("img.","s3.") : image.replace("://", "://s3.");
            }
            Glide.with(context)
                    .asBitmap()
                    .load(image)
                    .addListener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            if (imgs.size() > 0) {
                                if(!error && !useSecond){
                                    error = true;
                                }else if(!useSecond){
                                    error = false;
                                    useSecond = true;
                                }else{
                                    error = false;
                                    useSecond = false;
                                }
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

    public void toggleToolbar(){
        //attrs = getWindow().getAttributes();
        if(toolbarshow){
            //hide toolbar
            appbar.animate().translationY(-appbar.getHeight());
            appbarBottom.animate().translationY(+appbarBottom.getHeight());
            toolbarshow=false;
            toolbar_toggleBtn.setVisibility(View.VISIBLE);
        }
        else {
            //show toolbar
            appbar.animate().translationY(0);
            appbarBottom.animate().translationY(0);
            toolbarshow=true;
            toolbar_toggleBtn.setVisibility(View.GONE);
        }
        //getWindow().setAttributes(attrs);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(volumeControl && (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN ||keyCode==KeyEvent.KEYCODE_VOLUME_UP)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ) {
                nextPage();
            } else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                prevPage();
            }
            return true;
        }
        return super.onKeyDown(keyCode,event);
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
            return 0;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            if(res == 1){
                //error occured
                showErrorPopup(context);
                return;
            }
            reloadManga();

            if (pd.isShowing()) {
                pd.dismiss();
            }
            if(manga.getReported()){
                showPopup(context,"이미지 로드 실패", "문제가 접수된 게시물 입니다. 이미지가 제대로 보이지 않을 수 있습니다.");
            }
        }
    }

    public void reloadManga(){
        try{
            lockUi(false);
            imgs = manga.getImgs();
            imgs1 = manga.getImgs(true);
            if(imgs == null || imgs.size()==0) {
                showErrorPopup(context);
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
        if(id>0) {
            viewerBookmark = p.getViewerBookmark(id);
            if(manga.getMode() == 0 || manga.getMode() == 3) {
                // if manga is online or has title.gson
                if (title == null) title = manga.getTitle();
                p.addRecent(title);
                if (id > 0) p.setBookmark(title, id);
            }
        }
    }

    public void updateIntent(){
        result = new Intent();
        result.putExtra("id", id);
        setResult(RESULT_OK, result);
        //update intent : not sure if this works TODO: test this
        intent.putExtra("title", new Gson().toJson(title));
        intent.putExtra("manga", new Gson().toJson(manga));
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            //reload current ep
            lockUi(true);
            id = manga.getId();
            captchaChecked = false;
            loadImages l = new loadImages();
            l.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else
            finish();
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
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
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.Utils.getSample;
import static ml.melun.mangaview.Utils.getScreenSize;
import static ml.melun.mangaview.Utils.showPopup;

public class ViewerActivity2 extends AppCompatActivity {
    Preference p;
    Boolean dark, volumeControl, toolbarshow=true, reverse, touch=true, online, stretch, leftRight;
    Context context = this;
    String name;
    int id;
    Manga manga;
    ImageButton next, prev, commentBtn;
    android.support.v7.widget.Toolbar toolbar;
    Button pageBtn, nextPageBtn, prevPageBtn, touchToggleBtn;
    AppBarLayout appbar, appbarBottom;
    TextView toolbarTitle;
    int viewerBookmark = -1;
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
    Spinner spinner;
    int width = 0;
    Intent intent;

    Decoder d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
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
        pageBtn.setText("-/-");
        leftRight = p.getLeftRight();
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
        spinner = this.findViewById(R.id.toolbar_spinner);
        stretch = p.getStretch();

        //refreshBtn = this.findViewById(R.id.refreshButton);
        if(stretch) frame.setScaleType(ImageView.ScaleType.FIT_XY);
        width = getScreenSize(getWindowManager().getDefaultDisplay());

        intent = getIntent();

        manga = new Gson().fromJson(intent.getStringExtra("manga"),new TypeToken<Manga>(){}.getType());
        title = new Gson().fromJson(intent.getStringExtra("title"),new TypeToken<Title>(){}.getType());

        online = intent.getBooleanExtra("online",true);
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
        if(!online) {
            //load local imgs
            //appbarBottom.setVisibility(View.GONE);
            toolbarTitle.setText(manga.getName());
            toolbarTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            toolbarTitle.setMarqueeRepeatLimit(-1);
            toolbarTitle.setSingleLine(true);
            toolbarTitle.setSelected(true);
            next.setVisibility(View.GONE);
            prev.setVisibility(View.GONE);
            if(id>-1){
                //if manga has id = manga has title = update bookmark and add to recent
                p.addRecent(title);
                p.setBookmark(title.getName(),id);
            }
            imgs = manga.getImgs();
            types = new ArrayList<>();
            for(int i=0; i<imgs.size()*2;i++) types.add(-1);
            commentBtn.setVisibility(View.GONE);
            d = new Decoder(manga.getSeed(), manga.getId());
            refreshImage();
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
                    index--;
                    manga = eps.get(index);
                    id = manga.getId();
                    loadImages l = new loadImages();
                    l.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

            }
        });
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(index<eps.size()-1) {
                    index++;
                    manga = eps.get(index);
                    id = manga.getId();
                    loadImages l = new loadImages();
                    l.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                startActivity(commentActivity);
            }
        });

    }

    void nextPage(){
        //refreshbtn.setVisibility(View.VISIBLE);
        if(viewerBookmark==imgs.size()-1 && ( type==-1 || type==1)){
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
                final String image = imgs.get(viewerBookmark);

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
                                Bitmap sample = getSample(d.decode(bitmap),width);
                                int width = sample.getWidth();
                                int height = sample.getHeight();
                                if(width>height){
                                    imgCache = sample;
                                    type=0;
                                    if(reverse) frame.setImageBitmap(Bitmap.createBitmap(imgCache,0,0,width/2,height));
                                    else frame.setImageBitmap(Bitmap.createBitmap(imgCache,width/2,0,width/2,height));
                                }else{
                                    type=-1;
                                    frame.setImageBitmap(sample);
                                }
                                preload();
                            }
                        });
            }catch (Exception e){
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
                final String image = imgs.get(viewerBookmark);

                //placeholder
                frame.setImageResource(R.drawable.placeholder);
                Glide.with(context)
                        .asBitmap()
                        .load(image)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                                Bitmap sample = getSample(d.decode(bitmap), width);
                                //refreshbtn.setVisibility(View.INVISIBLE);
                                int width = sample.getWidth();
                                int height = sample.getHeight();
                                if(width>height){
                                    imgCache = sample;
                                    type=1;
                                    if(reverse) frame.setImageBitmap(Bitmap.createBitmap(imgCache, width/2, 0, width / 2, height));
                                    else frame.setImageBitmap(Bitmap.createBitmap(imgCache,0,0,width/2,height));
                                }else{
                                    type=-1;
                                    frame.setImageBitmap(sample);
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

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
            final String image = imgs.get(viewerBookmark);
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
                            Bitmap sample = getSample(d.decode(bitmap), width);
                            int width = sample.getWidth();
                            int height = sample.getHeight();
                            if (width > height) {
                                imgCache = sample;
                                type = 0;
                                if (reverse)
                                    frame.setImageBitmap(Bitmap.createBitmap(imgCache, 0, 0, width / 2, height));
                                else
                                    frame.setImageBitmap(Bitmap.createBitmap(imgCache, width / 2, 0, width / 2, height));
                            } else {
                                type = -1;
                                frame.setImageBitmap(sample);
                            }
                            preload();
                        }
                    });
            updatePageIndex();
        }catch(Exception e) {
            showPopup(context, "뷰어 오류", "이미지를 불러오는데 실패하였습니다. 연결 상태를 확인하고 다시 시도해 주세요.");
        }
    }

    void preload(){
        if(viewerBookmark<imgs.size()-1)
            Glide.with(context)
                    .asBitmap()
                    .load(imgs.get(viewerBookmark+1))
                    .preload();
    }
    void updatePageIndex(){
        pageBtn.setText(viewerBookmark+1+"/"+imgs.size());
        if(viewerBookmark==imgs.size()-1 && !toolbarshow) toggleToolbar();
    }

    public void toggleToolbar(){
        //attrs = getWindow().getAttributes();
        if(toolbarshow){
            appbar.animate().translationY(-appbar.getHeight());
            appbarBottom.animate().translationY(+appbarBottom.getHeight());
            toolbarshow=false;
        }
        else {
            appbar.animate().translationY(0);
            appbarBottom.animate().translationY(0);
            toolbarshow=true;
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

    private class loadImages extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onProgressUpdate(Integer... values) {
            String msg = "";
            switch(values[0]){
                case 0:
                    msg = "프로토콜 확인중";
                    break;
                case 1:
                    msg = "페이지 읽는중";
                    break;
                case 2:
                    msg = "이미지 리스트 읽는중";
                    break;
                case 3:
                    msg = "화 목록 읽는중";
                    break;
                case 4:
                    msg = "댓글 읽는중";
                    break;
            }
            pd.setMessage(msg);
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
            manga.setListener(new Manga.Listener() {
                @Override
                public void setMessage(int p) {
                    publishProgress(p);
                }
            });
        }

        protected Integer doInBackground(Void... params) {
            manga.fetch(p.getUrl());
            imgs = manga.getImgs();
            d = new Decoder(manga.getSeed(), manga.getId());
            return null;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            eps = manga.getEps();
            List<String> epsName = new ArrayList<>();
            for(int i=0; i<eps.size(); i++){
                if(eps.get(i).getId()==id){
                    index = i;
                }
                epsName.add(eps.get(i).getName());
            }
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
            result = new Intent();
            result.putExtra("id",id);
            setResult(RESULT_OK, result);

            //refresh spinner
            spinner.setAdapter(new ArrayAdapter(context, R.layout.spinner_item, epsName));
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long idt) {
                    ((TextView)parent.getChildAt(0)).setTextColor(Color.rgb(249, 249, 249));
                    if(index!= position) {
                        index = position;
                        manga = eps.get(index);
                        id = manga.getId();
                        loadImages l = new loadImages();
                        l.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            spinner.setSelection(index);
            try {
                if (title == null) {
                    title = manga.getTitle();
                    p.addRecent(title);
                }
                //update intent : not sure this works TODO: test this shit
                intent.putExtra("title", new Gson().toJson(title));
                intent.putExtra("manga", new Gson().toJson(manga));
                if (id > 0) p.setBookmark(title.getName(), id);
                viewerBookmark = p.getViewerBookmark(id);
                refreshImage();
            }catch(Exception e){
                showPopup(context, "뷰어 오류", "만화 정보를 불러오는데 실패하였습니다. 연결 상태를 확인하고 다시 시도해 주세요.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
            }
            if (pd.isShowing()) {
                pd.dismiss();
            }
        }
    }
}
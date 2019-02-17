package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
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
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

/*todo: glide를 안쓰고 프리로딩 서비스를 만들어서, 현재 페이지가 갑자기 바뀌었을 때도 dynamic 하게 처리할 수 있도록 하던가,
* 아니면 그냥 recycler 쓰던가 합시다
* 아니면 처음에 포문으로 이미지를 전부 한번씩 글라이드로 로딩 하면 될수도? (이미지 뷰에 안넣어도 캐싱을 한다는 가정 하에) = 안됨
*/
//todo: preload images
public class ViewerActivity2 extends AppCompatActivity {
    Preference p;
    Boolean dark, volumeControl, toolbarshow=true, reverse, touch=true, online, stretch;
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
    ArrayList<String> imgs;
    ArrayList<Integer> types;
    ProgressDialog pd;
    ArrayList<Manga> eps;
    int index;
    Title title;
    ImageView frame;
    int type=-1;
    Bitmap imgCache, preloadImg;
    Intent result;
    AlertDialog.Builder alert;
    Spinner spinner;

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
        toolbar = this.findViewById(R.id.viewerToolbar2);
        appbar = this.findViewById(R.id.viewerAppbar2);
        toolbarTitle = this.findViewById(R.id.toolbar_title);
        appbarBottom = this.findViewById(R.id.viewerAppbarBottom2);
        volumeControl = p.getVolumeControl();
        reverse = p.getReverse();
        frame = this.findViewById(R.id.viewer_image);
        pageBtn = this.findViewById(R.id.autoCutBtn);
        pageBtn.setText("");
        nextPageBtn = this.findViewById(R.id.nextPageBtn);
        prevPageBtn = this.findViewById(R.id.prevPageBtn);
        touchToggleBtn = this.findViewById(R.id.touchToggleBtn);
        commentBtn = this.findViewById(R.id.commentButton);
        spinner = this.findViewById(R.id.toolbar_spinner);
        stretch = p.getStretch();
        if(stretch) frame.setScaleType(ImageView.ScaleType.FIT_XY);

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        id = intent.getIntExtra("id",-1);
        online = intent.getBooleanExtra("online",true);
        String[] localImgs = intent.getStringArrayExtra("localImgs");
        toolbarTitle.setText(name);
        viewerBookmark = p.getViewerBookmark(id);
        manga = new Manga(id, name, "");

        if(intent.getBooleanExtra("recent",false)){
            Intent resultIntent = new Intent();
            setResult(RESULT_OK,resultIntent);
        }
        if(!online) {
            //load local imgs
            //appbarBottom.setVisibility(View.GONE);
            next.setVisibility(View.GONE);
            prev.setVisibility(View.GONE);
            imgs = new ArrayList<>(Arrays.asList(localImgs));
            types = new ArrayList<>();
            for(int i=0; i<imgs.size()*2;i++) types.add(-1);
            commentBtn.setVisibility(View.GONE);
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
        if(viewerBookmark==imgs.size()-1 && ( type==-1 || type==1)){
            //end of manga
        }else if(type==0){
            //is two page, current pos: right
            //dont add page
            //only change type
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
            final String image = imgs.get(viewerBookmark);
            //placeholder
            frame.setImageResource(R.drawable.placeholder);
            Glide.with(context)
                    .asBitmap()
                    .load(image)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap,
                                                    Transition<? super Bitmap> transition) {
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            if(width>height){
                                imgCache = d.decode(bitmap);
                                type=0;
                                if(reverse) frame.setImageBitmap(Bitmap.createBitmap(d.decode(bitmap),0,0,width/2,height));
                                else frame.setImageBitmap(Bitmap.createBitmap(d.decode(bitmap),width/2,0,width/2,height));
                            }else{
                                type=-1;
                                frame.setImageBitmap(d.decode(bitmap));
                            }
                        }
                    });
        }
        p.setViewerBookmark(id,viewerBookmark);
        if(imgs.size()-1==viewerBookmark) p.removeViewerBookmark(id);
        updatePageIndex();
    }
    void prevPage(){
        if(viewerBookmark==0 && (type==-1 || type==0)){
            //start of manga
        } else if(type==1){
            //is two page, current pos: left
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
            final String image = imgs.get(viewerBookmark);
            //placeholder
            frame.setImageResource(R.drawable.placeholder);
            Glide.with(context)
                    .asBitmap()
                    .load(image)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap,
                                                    Transition<? super Bitmap> transition) {
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            if(width>height){
                                imgCache = d.decode(bitmap);
                                type=1;
                                if(reverse) frame.setImageBitmap(Bitmap.createBitmap(d.decode(bitmap), width/2, 0, width / 2, height));
                                else frame.setImageBitmap(Bitmap.createBitmap(d.decode(bitmap),0,0,width/2,height));
                            }else{
                                type=-1;
                                frame.setImageBitmap(d.decode(bitmap));
                            }
                        }
                    });
        }
        p.setViewerBookmark(id,viewerBookmark);
        if(0==viewerBookmark) p.removeViewerBookmark(id);
        updatePageIndex();

    }

    void preload(){
        for(int i=viewerBookmark; i<imgs.size(); i++){
            String image = imgs.get(i);
            Glide.with(context)
                    .asBitmap()
                    .load(image)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap,
                                                    Transition<? super Bitmap> transition) {
                            //preload
                        }
                    });
        }
    }


    void refreshImage(){
        final String image = imgs.get(viewerBookmark);
        //placeholder
        //frame.setImageResource(R.drawable.placeholder);
        Glide.with(context)
                .asBitmap()
                .load(image)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap,
                                                Transition<? super Bitmap> transition) {
                        int width = bitmap.getWidth();
                        int height = bitmap.getHeight();
                        if(width>height){
                            imgCache = d.decode(bitmap);
                            type=0;
                            if(reverse) frame.setImageBitmap(Bitmap.createBitmap(imgCache, 0, 0, width / 2, height));
                            else frame.setImageBitmap(Bitmap.createBitmap(d.decode(bitmap),width/2,0,width/2,height));
                        }else{
                            type=-1;
                            frame.setImageBitmap(d.decode(bitmap));
                        }
                    }
                });
        updatePageIndex();
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

    private class loadImages extends AsyncTask<Void,Void,Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            if(dark) pd = new ProgressDialog(context, R.style.darkDialog);
            else pd = new ProgressDialog(context);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
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

            if(index==0) next.setEnabled(false);
            else next.setEnabled(true);
            if(index==eps.size()-1) prev.setEnabled(false);
            else prev.setEnabled(true);
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


            preload();
            if(title == null) title = manga.getTitle();
            p.addRecent(title);
            p.setBookmark(title.getName(),id);
            viewerBookmark = p.getViewerBookmark(id);
            refreshImage();
            if (pd.isShowing()) {
                pd.dismiss();
            }
        }
    }
}
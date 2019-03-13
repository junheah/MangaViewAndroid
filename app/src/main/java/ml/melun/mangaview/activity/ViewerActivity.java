package ml.melun.mangaview.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

//import com.viven.imagezoom.ImageZoomHelper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.StripAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.Utils.getScreenSize;
import static ml.melun.mangaview.Utils.showPopup;

public class ViewerActivity extends AppCompatActivity {
    String name;
    int id;
    Manga manga;
    RecyclerView strip;
    ProgressDialog pd;
    Context context = this;
    Preference p;
    StripAdapter stripAdapter;
    android.support.v7.widget.Toolbar toolbar;
    boolean toolbarshow = true;
    TextView toolbarTitle;
    AppBarLayout appbar, appbarBottom;
    int viewerBookmark;
    String[] localImgs;
    Boolean volumeControl;
    int pageForVolume;
    LinearLayoutManager manager;
    ImageButton next, prev;
    Button cut, pageBtn;
    List<Manga> eps;
    int index;
    Title title;
    Boolean autoCut = false;
    List<String> imgs;
    Boolean dark;
    Intent result;
    SwipyRefreshLayout swipe;
    ImageButton commentBtn;
    Spinner spinner;
    int seed = 0;
    int epsCount = 0;
    int viewerType=0;
    int width=0;
    Boolean online;
    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
        dark = p.getDarkTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);
        next = this.findViewById(R.id.toolbar_next);
        prev = this.findViewById(R.id.toolbar_previous);
        toolbar = this.findViewById(R.id.viewerToolbar);
        appbar = this.findViewById(R.id.viewerAppbar);
        toolbarTitle = this.findViewById(R.id.toolbar_title);
        appbarBottom = this.findViewById(R.id.viewerAppbarBottom);
        volumeControl = p.getVolumeControl();
        swipe = this.findViewById(R.id.viewerSwipe);
        cut = this.findViewById(R.id.viewerBtn2);
        cut.setText("자동 분할");
        pageBtn = this.findViewById(R.id.viewerBtn1);
        pageBtn.setText("-/-");
        spinner = this.findViewById(R.id.toolbar_spinner);
        commentBtn = this.findViewById(R.id.commentButton);
        viewerType = p.getViewerType();
        width = getScreenSize(getWindowManager().getDefaultDisplay());
        //imageZoomHelper = new ImageZoomHelper(this);

        try {
            intent = getIntent();
            title = new Gson().fromJson(intent.getStringExtra("title"),new TypeToken<Title>(){}.getType());
            manga = new Gson().fromJson(intent.getStringExtra("manga"),new TypeToken<Manga>(){}.getType());
            online = intent.getBooleanExtra("online", true);

            name = manga.getName();
            id = manga.getId();

            toolbarTitle.setText(name);
            viewerBookmark = p.getViewerBookmark(id);

            strip = this.findViewById(R.id.strip);
            manager = new LinearLayoutManager(this);
            if(viewerType==2){
                manager.setOrientation(LinearLayoutManager.HORIZONTAL);
                PagerSnapHelper snapHelper = new PagerSnapHelper();
                snapHelper.attachToRecyclerView(strip);
            }else manager.setOrientation(LinearLayoutManager.VERTICAL);
            strip.setLayoutManager(manager);
            if(intent.getBooleanExtra("recent",false)){
                Intent resultIntent = new Intent();
                setResult(RESULT_OK,resultIntent);
            }
            if(!online){
                //load local imgs
                if(id>-1){
                    //if manga has id = manga has title = update bookmark and add to recent
                    p.addRecent(title);
                    p.setBookmark(title.getName(),id);
                }
                spinner.setVisibility(View.GONE);
                prev.setVisibility(View.GONE);
                next.setVisibility(View.GONE);
                spinner.setVisibility(View.GONE);
                commentBtn.setVisibility(View.GONE);
                swipe.setEnabled(false);
                imgs = manga.getImgs();
                stripAdapter = new StripAdapter(context,imgs, autoCut, seed, id, width);
                strip.setAdapter(stripAdapter);
                stripAdapter.setClickListener(new StripAdapter.ItemClickListener() {
                    public void onItemClick() {
                        // show/hide toolbar
                        toggleToolbar();
                    }
                });
                if(id>-1){
                    bookmarkRefresh();
                }
                pageBtn.setText(viewerBookmark+1+"/"+imgs.size());
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
                    startActivity(commentActivity);
                }
            });
            strip.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if(newState==RecyclerView.SCROLL_STATE_IDLE){
                        int firstVisible = ((LinearLayoutManager) strip.getLayoutManager()).findFirstVisibleItemPosition();
                        int lastVisible = ((LinearLayoutManager) strip.getLayoutManager()).findLastVisibleItemPosition();
                        if(autoCut){
                            firstVisible /=2;
                            lastVisible /=2;
                        }
                        //bookmark handler
                        if (firstVisible == 0) p.removeViewerBookmark(id);
                        if (firstVisible != viewerBookmark) {
                            p.setViewerBookmark(id, firstVisible);
                            viewerBookmark = firstVisible;
                        }
                        if (lastVisible >= imgs.size() - 1) {
                            p.removeViewerBookmark(id);
                        }
                        if(viewerType==2) {
                            if ((!strip.canScrollHorizontally(1)) && !toolbarshow) {
                                toggleToolbar();
                            }
                        }else if(viewerType==0){
                            if ((!strip.canScrollVertically(1)) && !toolbarshow) {
                                toggleToolbar();
                            }
                        }
                    }else if(newState==RecyclerView.SCROLL_STATE_DRAGGING){
                        if(toolbarshow){
                            toggleToolbar();
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
                if(index>0) {
                    index--;
                    manga = eps.get(index);
                    id = manga.getId();
                    refresh();
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
                    refresh();
                }
            }
        });
        cut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAutoCut();
            }
        });
        swipe.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh(SwipyRefreshLayoutDirection direction) {
                refresh();
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
        loadImages l = new loadImages();
        l.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(volumeControl && (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN ||keyCode==KeyEvent.KEYCODE_VOLUME_UP)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ) {
                if(viewerBookmark<stripAdapter.getItemCount()-1) strip.scrollToPosition(++viewerBookmark);
            } else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if(viewerBookmark>0) strip.scrollToPosition(--viewerBookmark);
            }
            if(viewerBookmark>0&&viewerBookmark<stripAdapter.getItemCount()-1) {
                p.setViewerBookmark(id, viewerBookmark);
            }else p.removeViewerBookmark(id);
            if(toolbarshow) toggleToolbar();
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }

    public void toggleToolbar(){
        //attrs = getWindow().getAttributes();
        if(toolbarshow){
            appbar.animate().translationY(-appbar.getHeight());
            appbarBottom.animate().translationY(+appbarBottom.getHeight());
            toolbarshow=false;
        }
        else {
            pageBtn.setText(viewerBookmark+1+"/"+imgs.size());
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
        stripAdapter = new StripAdapter(context,imgs, autoCut, seed, id, width);
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

    public void bookmarkRefresh(){
        if(viewerBookmark!=-1){
            strip.scrollToPosition(viewerBookmark);
        }
        if(!autoCut) strip.getLayoutManager().scrollToPosition(p.getViewerBookmark(id));
        else strip.getLayoutManager().scrollToPosition(p.getViewerBookmark(id)*2);
    }



//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        return imageZoomHelper.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev);
//    }

    private class loadImages extends AsyncTask<Void,Void,Integer> {
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
                    pd.setMessage(msg);
                }
            });
            manga.fetch(p.getUrl());
            imgs = manga.getImgs();
            seed = manga.getSeed();
            stripAdapter = new StripAdapter(context,imgs, autoCut, seed, id, width);
            return null;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            strip.setAdapter(stripAdapter);

            stripAdapter.setClickListener(new StripAdapter.ItemClickListener() {
                public void onItemClick() {
                    // show/hide toolbar
                    toggleToolbar();
                }
            });
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

//            spinner.setAdapter(new ArrayAdapter<String>(context,
//                    android.R.layout.simple_spinner_item, epsName));
            //set button enabled
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
            swipe.setRefreshing(false);

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
                        refresh();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            spinner.setSelection(index);
            pageBtn.setText(viewerBookmark+1+"/"+imgs.size());
            bookmarkRefresh();
            try {
                if (title == null) title = manga.getTitle();
                p.addRecent(title);
                if (id > 0) p.setBookmark(title.getName(), id);
                result = new Intent();
                result.putExtra("id", id);
                setResult(RESULT_OK, result);
                //update intent : not sure this works TODO: test this shit
                intent.putExtra("title", new Gson().toJson(title));
                intent.putExtra("manga", new Gson().toJson(manga));
            }catch (Exception e){
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

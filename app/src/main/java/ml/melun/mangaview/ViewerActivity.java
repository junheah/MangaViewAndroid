package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

//import com.viven.imagezoom.ImageZoomHelper;

import com.google.gson.Gson;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ml.melun.mangaview.adapter.StripAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

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
    Button cut;
    ArrayList<Manga> eps;
    int index;
    Title title;
    Boolean autoCut = false;
    ArrayList<String> imgs;
    Boolean dark;
    Intent result;
    SwipyRefreshLayout swipe;
    ImageButton commentBtn;
    Spinner spinner;
    int seed;
    int epsCount = 0;
    Boolean online;
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
        cut = this.findViewById(R.id.autoCutBtn);
        spinner = this.findViewById(R.id.toolbar_spinner);
        commentBtn = this.findViewById(R.id.commentButton);
        //imageZoomHelper = new ImageZoomHelper(this);
        try {
            Intent intent = getIntent();
            name = intent.getStringExtra("name");
            seed = intent.getIntExtra("seed", 0);
            id = intent.getIntExtra("id",-1);
            System.out.println("ppppp"+id);
            localImgs = intent.getStringArrayExtra("localImgs");
            toolbarTitle.setText(name);
            viewerBookmark = p.getViewerBookmark(id);
            manga = new Manga(id, name, "");
            online = intent.getBooleanExtra("online", true);
            //getSupportActionBar().setTitle(title.getName());
            strip = this.findViewById(R.id.strip);
            manager = new LinearLayoutManager(this);
            manager.setOrientation(LinearLayoutManager.VERTICAL);
            strip.setLayoutManager(manager);
            if(!online){
                //load local imgs
                spinner.setVisibility(View.GONE);
                prev.setVisibility(View.GONE);
                next.setVisibility(View.GONE);
                spinner.setVisibility(View.GONE);
                commentBtn.setVisibility(View.GONE);
                imgs = new ArrayList<>(Arrays.asList(localImgs));
                stripAdapter = new StripAdapter(context,imgs, autoCut, seed, id);
                strip.setAdapter(stripAdapter);
                stripAdapter.setClickListener(new StripAdapter.ItemClickListener() {
                    public void onItemClick(View v, int position) {
                        // show/hide toolbar
                        toggleToolbar();
                    }
                });
                if(id>-1){
                    bookmarkRefresh();
                }
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
                        if (lastVisible == stripAdapter.getItemCount() - 1) {
                            p.removeViewerBookmark(id);
                        }
                        if (firstVisible != viewerBookmark) {
                            p.setViewerBookmark(id, firstVisible);
                            viewerBookmark = firstVisible;
                        }

                        if((!strip.canScrollVertically(1))&&!toolbarshow){
                            toggleToolbar();
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
                if(viewerBookmark<stripAdapter.getItemCount()-1)strip.scrollToPosition(++viewerBookmark);
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
            viewerBookmark = p.getViewerBookmark(id);
        } else{
            autoCut = true;
            cut.setBackgroundResource(R.drawable.button_bg_on);
            viewerBookmark = p.getViewerBookmark(id)*2;
        }
        stripAdapter = new StripAdapter(context,imgs, autoCut, seed, id);
        strip.setAdapter(stripAdapter);
        stripAdapter.setClickListener(new StripAdapter.ItemClickListener() {
            public void onItemClick(View v, int position) {
                // show/hide toolbar
                toggleToolbar();
            }
        });
        strip.getLayoutManager().scrollToPosition(viewerBookmark);
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
            pd.show();
        }

        protected Integer doInBackground(Void... params) {
            manga.fetch();
            imgs = manga.getImgs();
            seed = manga.getSeed();
            stripAdapter = new StripAdapter(context,imgs, autoCut, seed, id);
            return null;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            strip.setAdapter(stripAdapter);


            stripAdapter.setClickListener(new StripAdapter.ItemClickListener() {
                public void onItemClick(View v, int position) {
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
            if(index==0) next.setEnabled(false);
            else next.setEnabled(true);
            if(index==eps.size()-1) prev.setEnabled(false);
            else prev.setEnabled(true);
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

            bookmarkRefresh();

            if(title == null) title = manga.getTitle();
            p.addRecent(title);
            p.setBookmark(id);
            result = new Intent();
            result.putExtra("id",id);
            setResult(RESULT_OK, result);
            if (pd.isShowing()) {
                pd.dismiss();
            }

        }
    }
}

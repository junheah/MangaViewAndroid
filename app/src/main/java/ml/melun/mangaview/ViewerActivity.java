package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

//import com.viven.imagezoom.ImageZoomHelper;

import java.util.ArrayList;
import java.util.Arrays;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.EpisodeAdapter;
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
    AppBarLayout appbar;
    int viewerBookmark;
    String[] localImgs;
    Boolean volumeControl;
    int pageForVolume;
    LinearLayoutManager manager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);
        toolbar = this.findViewById(R.id.viewerToolbar);
        appbar = this.findViewById(R.id.viewerAppbar);
        toolbarTitle = this.findViewById(R.id.toolbar_title);
        p = new Preference();
        volumeControl = p.getVolumeControl();
        //imageZoomHelper = new ImageZoomHelper(this);
        try {
            Intent intent = getIntent();
            name = intent.getStringExtra("name");
            id = intent.getIntExtra("id",0);
            localImgs = intent.getStringArrayExtra("localImgs");
            toolbarTitle.setText(name);
            viewerBookmark = p.getViewerBookmark(id);
            manga = new Manga(id, name);
            //getSupportActionBar().setTitle(title.getName());
            strip = this.findViewById(R.id.strip);

            //ImageZoomHelper.setViewZoomable(findViewById(R.id.strip));
            manager = new LinearLayoutManager(this);
            manager.setOrientation(LinearLayoutManager.VERTICAL);
            strip.setLayoutManager(manager);
            if(localImgs!=null||id<0){
                //load local imgs
                ArrayList<String> imgs = new ArrayList<>(Arrays.asList(localImgs));
                stripAdapter = new StripAdapter(context,imgs);
                strip.setAdapter(stripAdapter);
                stripAdapter.setClickListener(new StripAdapter.ItemClickListener() {
                    public void onItemClick(View v, int position) {
                        // show/hide toolbar
                        toggleToolbar();
                    }
                });
            }else {
                loadImages l = new loadImages();
                l.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            strip.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if(newState==RecyclerView.SCROLL_STATE_IDLE){
                        int firstVisible = ((LinearLayoutManager) strip.getLayoutManager()).findFirstVisibleItemPosition();
                        int lastVisible = ((LinearLayoutManager) strip.getLayoutManager()).findLastVisibleItemPosition();
                        if(firstVisible==0) p.removeViewerBookmark(id);
                        if(lastVisible==stripAdapter.getItemCount()-1) {
                            p.removeViewerBookmark(id);
                        }
                        if(firstVisible!=viewerBookmark) {
                            p.setViewerBookmark(id,firstVisible);
                            viewerBookmark=firstVisible;
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
            toolbarshow=false;
        }
        else {
            appbar.animate().translationY(0);
            toolbarshow=true;
        }
        //getWindow().setAttributes(attrs);
    }

//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        return imageZoomHelper.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev);
//    }

    public void setPosition(int i){

    }

    private class loadImages extends AsyncTask<Void,Void,Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(context);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
        }

        protected Integer doInBackground(Void... params) {
            manga.fetch();
            ArrayList<String> imgs = manga.getImgs();
            stripAdapter = new StripAdapter(context,imgs);
            return null;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            strip.setAdapter(stripAdapter);
            if(viewerBookmark!=-1){
                strip.scrollToPosition(viewerBookmark);
                System.out.println("Viewer bookmark " + viewerBookmark);
            }

            stripAdapter.setClickListener(new StripAdapter.ItemClickListener() {
                public void onItemClick(View v, int position) {
                    // show/hide toolbar
                    toggleToolbar();
                }
            });
            if (pd.isShowing()) {
                pd.dismiss();
            }
        }
    }
}

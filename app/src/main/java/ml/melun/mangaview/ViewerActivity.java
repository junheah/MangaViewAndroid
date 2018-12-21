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
    StripAdapter stripAdapter;
    //ImageZoomHelper imageZoomHelper;
    android.support.v7.widget.Toolbar toolbar;
    boolean toolbarshow = true;
    TextView toolbarTitle;
    AppBarLayout appbar;
    //WindowManager.LayoutParams attrs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);
        toolbar = this.findViewById(R.id.viewerToolbar);
        appbar = this.findViewById(R.id.viewerAppbar);
        toolbarTitle = this.findViewById(R.id.toolbar_title);
        //imageZoomHelper = new ImageZoomHelper(this);
        try {
            Intent intent = getIntent();
            name = intent.getStringExtra("name");
            id = intent.getIntExtra("id",0);

            toolbarTitle.setText(name);
            manga = new Manga(id, name);
            //getSupportActionBar().setTitle(title.getName());
            strip = this.findViewById(R.id.strip);
            //ImageZoomHelper.setViewZoomable(findViewById(R.id.strip));
            LinearLayoutManager manager = new LinearLayoutManager(this);
            manager.setOrientation(LinearLayoutManager.VERTICAL);
            strip.setLayoutManager(manager);
            loadImages l = new loadImages();
            l.execute();
        }catch(Exception e){
            e.printStackTrace();
        }
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

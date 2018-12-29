package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.transformation.MangaCrop;

public class ViewerActivity2 extends AppCompatActivity {
    Preference p;
    Boolean dark;
    Context context = this;
    String name;
    int id;
    Manga manga;
    ImageButton next, prev;
    android.support.v7.widget.Toolbar toolbar;
    AppBarLayout appbar, appbarBottom;
    TextView toolbarTitle;
    Boolean volumeControl;
    int viewerBookmark = -1;
    ArrayList<String> imgs;
    ProgressDialog pd;
    ArrayList<Manga> eps;
    int index;
    Title title;
    ImageView frame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference();
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDarkNoTitle);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer2);

        next = this.findViewById(R.id.toolbar_next);
        prev = this.findViewById(R.id.toolbar_previous);
        toolbar = this.findViewById(R.id.viewerToolbar2);
        appbar = this.findViewById(R.id.viewerAppbar2);
        toolbarTitle = this.findViewById(R.id.toolbar_title);
        appbarBottom = this.findViewById(R.id.viewerAppbarBottom2);
        volumeControl = p.getVolumeControl();
        frame = this.findViewById(R.id.viewer_image);

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        id = intent.getIntExtra("id",0);
        String[] localImgs = intent.getStringArrayExtra("localImgs");
        toolbarTitle.setText(name);
        viewerBookmark = p.getViewerBookmark(id);
        manga = new Manga(id, name);

        if(localImgs!=null||id<0) {
            //load local imgs
            appbarBottom.setVisibility(View.GONE);
            imgs = new ArrayList<>(Arrays.asList(localImgs));
            refreshImage();

        }else{
            //if online
            //fetch imgs
            loadImages l = new loadImages();
            l.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        frame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewerBookmark++;
                refreshImage();
            }
        });
    }
    void refreshImage(){
        String image = imgs.get(viewerBookmark);
        Glide.with(context)
                .load(image)
                .apply(new RequestOptions().dontTransform())
                .into(frame);
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
            manga.fetch();
            imgs = manga.getImgs();
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

            if(index==0) next.setEnabled(false);
            else next.setEnabled(true);
            if(index==eps.size()-1) prev.setEnabled(false);
            else prev.setEnabled(true);

            if(title == null) title = manga.getTitle();
            p.addRecent(title);
            p.setBookmark(id);
            refreshImage();
            if (pd.isShowing()) {
                pd.dismiss();
            }

        }
    }
}

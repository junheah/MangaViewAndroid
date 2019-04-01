package ml.melun.mangaview.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.FragmentManager;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.StripAdapter;
import ml.melun.mangaview.adapter.ViewerPagerAdapter;
import ml.melun.mangaview.interfaces.PageInterface;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.Utils.getScreenSize;
import static ml.melun.mangaview.Utils.showErrorPopup;
import static ml.melun.mangaview.Utils.showPopup;

public class ViewerActivity3 extends AppCompatActivity {
    List<String> imgs;
    Manga manga;
    Preference p;
    Context context;
    ViewPager viewPager;
    Boolean dark;
    Boolean online;
    ImageButton next, prev;
    TextView toolbarTitle;
    AppBarLayout appbar, appbarBottom;
    Toolbar toolbar;
    Boolean volumeControl;
    Button cut, pageBtn;
    Spinner spinner;
    ImageButton commentBtn;
    int width;
    Intent intent;
    Title title;
    String name;
    int id;
    int viewerBookmark;
    int seed;
    ViewerPagerAdapter pageAdapter;
    int index;
    Intent result;
    List<Manga> eps;
    Boolean toolbarshow = true;
    ViewPager.OnPageChangeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        p = new Preference(this);
        dark = p.getDarkTheme();
        if(dark) setTheme(R.style.AppThemeDarkNoTitle);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer3);
        context = this;
        next = this.findViewById(R.id.toolbar_next);
        prev = this.findViewById(R.id.toolbar_previous);
        toolbar = this.findViewById(R.id.viewerToolbar);
        appbar = this.findViewById(R.id.viewerAppbar);
        toolbarTitle = this.findViewById(R.id.toolbar_title);
        appbarBottom = this.findViewById(R.id.viewerAppbarBottom);
        volumeControl = p.getVolumeControl();
        cut = this.findViewById(R.id.viewerBtn2);
        cut.setText("자동 분할");
        //TODO: autoCut
        cut.setVisibility(View.GONE);
        //
        pageBtn = this.findViewById(R.id.viewerBtn1);
        pageBtn.setText("-/-");
        spinner = this.findViewById(R.id.toolbar_spinner);
        commentBtn = this.findViewById(R.id.commentButton);
        width = getScreenSize(getWindowManager().getDefaultDisplay());
        viewPager = this.findViewById(R.id.viewerPager);

        //adapter
        pageAdapter = new ViewerPagerAdapter(getSupportFragmentManager(), width, context, new PageInterface() {
            @Override
            public void onPageClick() {
                toggleToolbar();
            }
        });
        listener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                viewerBookmark = position;
                pageBtn.setText(viewerBookmark+1+"/"+imgs.size());
                if(position == imgs.size()-1 || position == 0){
                    p.removeViewerBookmark(id);
                }
                else p.setViewerBookmark(id, viewerBookmark);
                if(position == imgs.size()-1 && !toolbarshow) toggleToolbar();
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };

        try {
            intent = getIntent();
            title = new Gson().fromJson(intent.getStringExtra("title"),new TypeToken<Title>(){}.getType());
            manga = new Gson().fromJson(intent.getStringExtra("manga"),new TypeToken<Manga>(){}.getType());
            online = intent.getBooleanExtra("online", true);

            name = manga.getName();
            id = manga.getId();

            toolbarTitle.setText(name);
            viewerBookmark = p.getViewerBookmark(id);

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
                toolbarTitle.setText(manga.getName());
                toolbarTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                toolbarTitle.setMarqueeRepeatLimit(-1);
                toolbarTitle.setSingleLine(true);
                toolbarTitle.setSelected(true);
                spinner.setVisibility(View.GONE);
                prev.setVisibility(View.GONE);
                next.setVisibility(View.GONE);
                spinner.setVisibility(View.GONE);
                commentBtn.setVisibility(View.GONE);
                imgs = manga.getImgs();
                pageAdapter.setManga(manga);
                viewPager.setAdapter(pageAdapter);
                viewPager.addOnPageChangeListener(listener);
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
                    commentActivity.putExtra("id", manga.getId());
                    startActivity(commentActivity);
                }
            });
        }catch(Exception e){
            e.printStackTrace();
        }

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
                            viewPager.setCurrentItem(viewerBookmark, false);
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
    }

    void refresh(){
        new LoadImages().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void bookmarkRefresh(){
        if(manga.getId()>-1) {
            viewerBookmark = p.getViewerBookmark(manga.getId());
            viewPager.setCurrentItem(viewerBookmark, false);
        }
        //if(!autoCut) strip.getLayoutManager().scrollToPosition(p.getViewerBookmark(id));
        //else strip.getLayoutManager().scrollToPosition(p.getViewerBookmark(id)*2);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if(volumeControl && (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN ||keyCode==KeyEvent.KEYCODE_VOLUME_UP)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ) {
                if(viewerBookmark<pageAdapter.getCount()-1) viewPager.setCurrentItem(++viewerBookmark);
            } else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if(viewerBookmark>0) viewPager.setCurrentItem(--viewerBookmark);
            }
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

    private class LoadImages extends AsyncTask<Void, String, Integer>{
        ProgressDialog pd;
        protected void onProgressUpdate(String... values) {
            pd.setMessage(values[0]);
        }
        @Override
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
                        LoadImages.super.cancel(true);
                        pd.dismiss();
                        finish();
                    }
                    return true;
                }
            });
            pd.show();
        }

        @Override
        protected Integer doInBackground(Void... voids) {
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
            manga.fetch(p.getUrl(), cookie);
            imgs = manga.getImgs();
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            viewPager.removeOnPageChangeListener(listener);
            //refresh views
            toolbarTitle.setText(manga.getName());
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

            try {
                if (title == null) title = manga.getTitle();
                p.addRecent(title);
                if (id > 0) p.setBookmark(title.getName(), id);
                result = new Intent();
                result.putExtra("id", id);
                setResult(RESULT_OK, result);
                //update intent : not sure if this works TODO: test this
                intent.putExtra("title", new Gson().toJson(title));
                intent.putExtra("manga", new Gson().toJson(manga));
            }catch (Exception e){
                showErrorPopup(context, e);
            }

            //adapter
            pageAdapter.setManga(manga);
            viewPager.setAdapter(pageAdapter);
            pageBtn.setText(viewerBookmark+1+"/"+imgs.size());
            bookmarkRefresh();
            viewPager.addOnPageChangeListener(listener);

            if(pd.isShowing()) pd.dismiss();
        }
    }
}

package ml.melun.mangaview.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import com.google.android.material.appbar.AppBarLayout;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
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

import ml.melun.mangaview.R;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.adapter.CustomSpinnerAdapter;
import ml.melun.mangaview.adapter.ViewerPagerAdapter;
import ml.melun.mangaview.interfaces.PageInterface;

import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.getScreenSize;
import static ml.melun.mangaview.Utils.hideSpinnerDropDown;
import static ml.melun.mangaview.Utils.isInteger;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.activity.CaptchaActivity.RESULT_CAPTCHA;

public class ViewerActivity3 extends AppCompatActivity {
    List<String> imgs;
    Manga manga;
    Context context;
    ViewPager viewPager;
    boolean dark;
    ImageButton next, prev;
    TextView toolbarTitle;
    AppBarLayout appbar, appbarBottom;
    Toolbar toolbar;
    Button cut, pageBtn;
    ImageButton commentBtn;
    int width;
    Intent intent;
    Title title;
    String name;
    boolean captchaChecked = false;
    int id;
    int viewerBookmark = 0;
    int seed;
    ViewerPagerAdapter pageAdapter;
    int index;
    Intent result;
    List<Manga> eps;
    boolean toolbarshow = true;
    ViewPager.OnPageChangeListener listener;
    Spinner spinner;
    CustomSpinnerAdapter spinnerAdapter;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("manga", new Gson().toJson(manga));
        outState.putString("title", new Gson().toJson(title));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        cut = this.findViewById(R.id.viewerBtn2);
        this.findViewById(R.id.backButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        cut.setText("자동 분할");
        //TODO: autoCut
        cut.setVisibility(View.GONE);
        //
        pageBtn = this.findViewById(R.id.viewerBtn1);
        pageBtn.setText("-/-");
        commentBtn = this.findViewById(R.id.commentButton);
        width = getScreenSize(getWindowManager().getDefaultDisplay());
        viewPager = this.findViewById(R.id.viewerPager);
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
                if(viewerBookmark != position) {
                    viewerBookmark = position;
                    pageBtn.setText(viewerBookmark + 1 + "/" + imgs.size());
                    if(!isInteger(title.getRelease())) {
                        if (position == imgs.size() - 1 || position == 0) {
                            p.removeViewerBookmark(id);
                        } else p.setViewerBookmark(id, viewerBookmark);
                    }

                    boolean lastPage = viewerBookmark == imgs.size() - 1;
                    boolean firstPage = viewerBookmark == 0;
                    if (toolbarshow && !lastPage)
                        toggleToolbar();
                    else if (lastPage && !toolbarshow)
                        toggleToolbar();
                }
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

            if(title == null)
                title = manga.getTitle();

            name = manga.getName();
            id = manga.getId();

            toolbarTitle.setText(name);
            if(!isInteger(title.getRelease())) viewerBookmark = p.getViewerBookmark(id);

            if(manga.getMode() == 0 || manga.getMode() == 3){
                result = new Intent();
                result.putExtra("id", id);
                setResult(RESULT_OK,result);
            }
            if(manga.getMode() != 0){
                //load local imgs
                commentBtn.setVisibility(View.GONE);
                reloadManga();
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
    }

    void refresh(){
        captchaChecked = false;
        new LoadImages().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if(keyCode == p.getNextPageKey()){
            if(event.getAction() == KeyEvent.ACTION_UP && viewerBookmark<pageAdapter.getCount()-1)
                viewPager.setCurrentItem(viewerBookmark+1);
            return true;
        }else if(keyCode == p.getPrevPageKey()){
            if(event.getAction() == KeyEvent.ACTION_UP && viewerBookmark>0)
                viewPager.setCurrentItem(viewerBookmark-1);
            return true;
        }
        return super.dispatchKeyEvent(event);
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
            manga.fetch(httpClient);
            if(title == null)
                title = manga.getTitle();
            return 0;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            reloadManga();
            if(pd.isShowing()) pd.dismiss();
        }
    }

    public void reloadManga(){
        try {
            lockUi(false);
            imgs = manga.getImgs();
            if(imgs == null || imgs.size()==0) {
                showCaptchaPopup(context);
                return;
            }
            refreshAdapter();
            bookmarkRefresh();
            refreshToolbar();
            updateIntent();
            viewPager.addOnPageChangeListener(listener);

        }catch (Exception e){
            StackTraceElement[] stack = e.getStackTrace();
            String message = "";
            for(StackTraceElement s : stack){
                message +=s.toString()+'\n';
            }
            Utils.showCaptchaPopup(context, e);
        }
    }

    public void bookmarkRefresh(){
        if(id>0 && !isInteger(title.getRelease())) {
            viewerBookmark = p.getViewerBookmark(id);
            if (viewerBookmark != -1) {
                viewerBookmark = p.getViewerBookmark(manga.getId());
                viewPager.setCurrentItem(viewerBookmark, false);
            }
            if (manga.getMode() == 0 || manga.getMode() == 3) {
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
    }

    public void refreshAdapter(){
        //adapter
        viewPager.removeOnPageChangeListener(listener);
        pageAdapter.setManga(manga);
        viewPager.setAdapter(pageAdapter);
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
            prev.setColorFilter(null);
        }
        pageBtn.setText(viewerBookmark+1+"/"+imgs.size());
    }

    void lockUi(boolean lock){
        commentBtn.setEnabled(!lock);
        next.setEnabled(!lock);
        prev.setEnabled(!lock);
        pageBtn.setEnabled(!lock);
        cut.setEnabled(!lock);
        spinner.setEnabled(!lock);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_CAPTCHA) {
            refresh();
        }
    }
}

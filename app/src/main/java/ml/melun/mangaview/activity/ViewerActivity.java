package ml.melun.mangaview.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.omadahealth.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.CustomSpinnerAdapter;
import ml.melun.mangaview.adapter.StripAdapter;
import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.getScreenSize;
import static ml.melun.mangaview.Utils.hideSpinnerDropDown;
import static ml.melun.mangaview.Utils.showErrorPopup;
import static ml.melun.mangaview.Utils.showPopup;

public class ViewerActivity extends AppCompatActivity {
    String name;
    int id;
    Manga manga;
    RecyclerView strip;
    ProgressDialog pd;
    Context context = this;
    StripAdapter stripAdapter;
    android.support.v7.widget.Toolbar toolbar;
    boolean toolbarshow = true;
    TextView toolbarTitle;
    AppBarLayout appbar, appbarBottom;
    int viewerBookmark;
    Boolean volumeControl;
    LinearLayoutManager manager;
    ImageButton next, prev;
    Button cut, pageBtn;
    List<Manga> eps;
    int index = -1;
    Title title;
    Boolean autoCut = false;
    List<String> imgs;
    Boolean dark;
    Intent result;
    SwipyRefreshLayout swipe;
    ImageButton commentBtn;
    int seed = 0;
    int epsCount = 0;
    int width=0;
    Intent intent;
    boolean captchaChecked = false;
    Spinner spinner;
    CustomSpinnerAdapter spinnerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        commentBtn = this.findViewById(R.id.commentButton);
        spinner = this.findViewById(R.id.toolbar_spinner);
        width = getScreenSize(getWindowManager().getDefaultDisplay());
        //imageZoomHelper = new ImageZoomHelper(this);
        try {
            intent = getIntent();
            title = new Gson().fromJson(intent.getStringExtra("title"),new TypeToken<Title>(){}.getType());
            manga = new Gson().fromJson(intent.getStringExtra("manga"),new TypeToken<Manga>(){}.getType());

            name = manga.getName();
            id = manga.getId();

            toolbarTitle.setText(name);
            viewerBookmark = p.getViewerBookmark(id);

            strip = this.findViewById(R.id.strip);
            manager = new LinearLayoutManager(this);
            manager.setOrientation(LinearLayoutManager.VERTICAL);
            strip.setLayoutManager(manager);
            if(intent.getBooleanExtra("recent",false)){
                Intent resultIntent = new Intent();
                setResult(RESULT_OK,resultIntent);
            }
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

            if(manga.getMode() != 0){
                // is offline
                //load local imgs

                commentBtn.setVisibility(View.GONE);
                swipe.setEnabled(false);


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

                        if ((!strip.canScrollVertically(1)) && !toolbarshow) {
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
        captchaChecked = false;
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
        stripAdapter = new StripAdapter(context,imgs,manga.getImgs(true), autoCut, seed, id, width);
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


//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        return imageZoomHelper.onDispatchTouchEvent(ev) || super.dispatchTouchEvent(ev);
//    }

    private class loadImages extends AsyncTask<Void,String,Integer> {
        @Override
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
        try {
            lockUi(false);
            imgs = manga.getImgs();
            if(imgs == null || imgs.size()==0) {
                showErrorPopup(context);
            }
            stripAdapter = new StripAdapter(context, imgs, manga.getImgs(true), autoCut, manga.getSeed(), id, width);

            refreshAdapter();
            bookmarkRefresh();
            refreshToolbar();
            updateIntent();

        }catch (Exception e){
            StackTraceElement[] stack = e.getStackTrace();
            String message = "";
            for(StackTraceElement s : stack){
                message +=s.toString()+'\n';
            }
            showErrorPopup(context, e);
            e.printStackTrace();
        }
    }

    public void bookmarkRefresh(){
        if(id>0) {
            viewerBookmark = p.getViewerBookmark(id);
            if (viewerBookmark != -1) {
                strip.scrollToPosition(viewerBookmark);
            }
            if (!autoCut) strip.scrollToPosition(viewerBookmark);
            else strip.scrollToPosition(viewerBookmark * 2);
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
        //update intent : not sure if this works TODO: test this
        intent.putExtra("title", new Gson().toJson(title));
        intent.putExtra("manga", new Gson().toJson(manga));
    }

    public void refreshAdapter(){
        strip.setAdapter(stripAdapter);
        stripAdapter.setClickListener(new StripAdapter.ItemClickListener() {
            public void onItemClick() {
                // show/hide toolbar
                toggleToolbar();
            }
        });
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
        swipe.setRefreshing(false);


        pageBtn.setText(viewerBookmark+1+"/"+imgs.size());
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        return super.onMenuOpened(featureId, menu);
    }

    void lockUi(Boolean lock){
        commentBtn.setEnabled(!lock);
        next.setEnabled(!lock);
        prev.setEnabled(!lock);
        pageBtn.setEnabled(!lock);
        cut.setEnabled(!lock);
        strip.setEnabled(!lock);
        spinner.setEnabled(!lock);
    }
}

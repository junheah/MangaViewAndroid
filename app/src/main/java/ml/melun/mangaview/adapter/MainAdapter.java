package ml.melun.mangaview.adapter;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.Utils;
import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.MainPage;
import ml.melun.mangaview.mangaview.MainPageWebtoon;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.showCaptchaPopup;
import static ml.melun.mangaview.mangaview.MTitle.base_comic;

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context mainContext;
    LayoutInflater mInflater, itemInflater;
    MainUpdatedAdapter uadapter;
    onItemClick mainClickListener;
    MainTagAdapter nadapter, tadapter, radapter;
    boolean dark, loaded = false;

    List<Manga> recent, favUpdate;
    List<Title> ranking;

    final static int ADDED = 0;
    final static int NAME = 1;
    final static int GENRE = 2;
    final static int RELEASE = 3;
    final static int UPDATE = 4;
    final static int RECENT = 5;
    final static int RANKING = 6;


    public MainAdapter(Context main) {
        super();
        mainContext = main;
        dark = p.getDarkTheme();
        this.mInflater = LayoutInflater.from(main);
        this.itemInflater = LayoutInflater.from(main);

        uadapter = new MainUpdatedAdapter(main);
        tadapter = new MainTagAdapter(main, Arrays.asList(mainContext.getResources().getStringArray(R.array.tag_genre)), 0);
        nadapter = new MainTagAdapter(main, Arrays.asList(mainContext.getResources().getStringArray(R.array.tag_name)), 1);
        radapter = new MainTagAdapter(main, Arrays.asList(mainContext.getResources().getStringArray(R.array.tag_release)), 2);


        ranking = new ArrayList<>();
        recent = new ArrayList<>();
        favUpdate = new ArrayList<>();

        setHasStableIds(true);

        uadapter.setLoad("URL 업데이트중...");
    }

    public void fetch(){
        //fetch main page data
        uadapter.setLoad();
        new fetchMain().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public int getItemViewType(int position) {
        int mRECENT = RECENT + favUpdate.size();
        int mRANKING = RANKING + favUpdate.size() + recent.size();
        if(position>=0 && position <=4) return position;
        else if(position == mRECENT) return RECENT;
        else if(position == mRANKING) return RANKING;
        else return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.main_tags, parent, false);
        switch (viewType){
            case ADDED:
                return new addedHolder(view);
            case GENRE:
                return new tagHolder(view);
            case NAME:
                return new nameHolder(view);
            case RELEASE:
                return new releaseHolder(view);
            case RANKING:
            case RECENT:
            case UPDATE:
                //return ranking title
                View rankingTitle = mInflater.inflate(R.layout.main_item_title,parent,false);
                return new rankingHolder(rankingTitle);
        }
        //ranking items
        View rankingItem = mInflater.inflate(R.layout.main_item_ranking,parent,false);
        return new rankingHolder(rankingItem);

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int RECENT = this.RECENT + favUpdate.size();
        int RANKING = this.RANKING + favUpdate.size() + recent.size();
        if(position == ADDED){
            addedHolder rh = (addedHolder) holder;
            rh.title.setText("최근 추가된 만화 +");

        }else if(position == GENRE){
            tagHolder th = (tagHolder) holder;
            th.title.setText("장르별");

        }else if(position == NAME){
            nameHolder nh = (nameHolder) holder;
            nh.title.setText("이름별");

        }else if(position == RELEASE){
            releaseHolder rlh = (releaseHolder) holder;
            rlh.title.setText("발행별");

        }else if(position == UPDATE) {
            rankingHolder rkh2 = (rankingHolder) holder;
            rkh2.title.setText("북마크 업데이트");
        }else if(position > UPDATE && position < RECENT){
            //update item
            rankingHolder h = (rankingHolder) holder;
            h.setManga(favUpdate.get(position-UPDATE-1), 0);
        }else if(position == RECENT){
            rankingHolder rkh3 = (rankingHolder) holder;
            rkh3.title.setText("최근에 본 만화");
        }else if(position > RECENT && position < RANKING){
            //recent item
            rankingHolder h = (rankingHolder) holder;
            h.setManga(recent.get(position-RECENT-1), 0);
        }else if(position == RANKING) {
            rankingHolder rkh = (rankingHolder) holder;
            rkh.title.setText("일본만화 베스트");
        }else if(position > RANKING){
            //ranking item
            rankingHolder h = (rankingHolder) holder;
            h.setTitleObject(ranking.get(position-RANKING-1), position-RANKING);
        }

    }

    @Override
    public int getItemCount() {
        return 7 + ranking.size() + favUpdate.size() + recent.size();
    }

    public void updateRankingWidgets(){
        int recentSize = recent.size();
        int updateSize = favUpdate.size();
        int rankingSize = ranking.size();
        int RECENT = this.RECENT + updateSize;
        int RANKING = this.RANKING + updateSize + recentSize;
        if(updateSize>0){
            notifyItemChanged(UPDATE+1);
            if(updateSize>1)
                notifyItemRangeInserted(UPDATE+2, updateSize-1);
        }else{
            favUpdate.add(new Manga(-1,"결과없음","", base_comic));
            notifyItemChanged(UPDATE+1);
        }

        if(recentSize>0){
            notifyItemChanged(RECENT+1);
            if(recentSize>1)
                notifyItemRangeInserted(RECENT+2, recentSize-1);
        }else{
            recent.add(new Manga(-1,"결과없음","", base_comic));
            notifyItemChanged(RECENT+1);
        }

        if(rankingSize>0){
            notifyItemChanged(RANKING+1);
            if(rankingSize>1)
                notifyItemRangeInserted(RANKING+2, rankingSize-1);
        }else{
            ranking.add(new Title("결과없음","", "", null, "", -1, base_comic));
            notifyItemChanged(RANKING+1);
        }

    }

    public void clearRecyclerPools(){

    }

    class addedHolder extends RecyclerView.ViewHolder{
        RecyclerView updatedList;
        TextView title;
        public addedHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.main_tag_title);
            updatedList = itemView.findViewById(R.id.main_tag);
            LinearLayoutManager lm = new LinearLayoutManager(mainContext);
            lm.setOrientation(RecyclerView.HORIZONTAL);
            updatedList.setLayoutManager(lm);
            updatedList.setAdapter(uadapter);
            uadapter.setClickListener(new MainUpdatedAdapter.onclick() {
                @Override
                public void onclick(Manga m) {
                    mainClickListener.clickedManga(m);
                }

                @Override
                public void refresh() {
                    fetch();
                }
            });
            title.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mainClickListener.clickedMoreUpdated();
                }
            });
        }
    }
    class rankingHolder extends RecyclerView.ViewHolder{
        TextView title;
        CardView card;
        TextView rank;
        Title titleobj;
        Manga manga;
        View rankLayout;

        public rankingHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.ranking_title);
            card = itemView.findViewById(R.id.ranking_card);
            rank = itemView.findViewById(R.id.ranking_rank);
            rankLayout = itemView.findViewById(R.id.ranking_rank_layout);
            if(card!=null){
                if(dark) {
                    card.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
                }
            }
        }

        public void setManga(Manga m, int r){
            manga = m;
            title.setText(m.getName());
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(manga!=null && manga.getId()>0)
                        mainClickListener.clickedManga(manga);
                }
            });

            if(r>0){
                rankLayout.setVisibility(View.VISIBLE);
                rank.setText(String.valueOf(r));
            }else{
                rankLayout.setVisibility(View.GONE);
            }
        }

        public void setTitleObject(Title t, int r){
            titleobj = t;
            title.setText(t.getName());
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(titleobj!=null && titleobj.getId()>0)
                        mainClickListener.clickedTitle(titleobj);
                }
            });

            if(r>0){
                rankLayout.setVisibility(View.VISIBLE);
                rank.setText(String.valueOf(r));
            }else{
                rankLayout.setVisibility(View.GONE);
            }
        }
    }


    class tagHolder extends RecyclerView.ViewHolder{
        RecyclerView tagList;
        TextView title;
        public tagHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.main_tag_title);
            tagList = itemView.findViewById(R.id.main_tag);
            LinearLayoutManager lm = new LinearLayoutManager(mainContext);
            lm.setOrientation(RecyclerView.HORIZONTAL);
            tagList.setLayoutManager(lm);
            tagList.setAdapter(tadapter);
            tadapter.setClickListener(new MainTagAdapter.tagOnclick() {
                @Override
                public void onClick(int position, String tag) {
                    mainClickListener.clickedTag(tag);
                }
            });
        }
    }
    class nameHolder extends RecyclerView.ViewHolder{
        RecyclerView nameList;
        TextView title;
        public nameHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.main_tag_title);
            nameList = itemView.findViewById(R.id.main_tag);
            LinearLayoutManager lm = new LinearLayoutManager(mainContext);
            lm.setOrientation(RecyclerView.HORIZONTAL);
            nameList.setLayoutManager(lm);
            nameList.setAdapter(nadapter);
            nadapter.setClickListener(new MainTagAdapter.tagOnclick() {
                @Override
                public void onClick(int pos, String tag) {
                    try {
                        mainClickListener.clickedName(tag);
                    }catch (Exception e){

                    }
                }
            });
        }
    }
    class releaseHolder extends RecyclerView.ViewHolder{
        RecyclerView releaseList;
        TextView title;
        public releaseHolder(View itemView) {
            super(itemView);
            LinearLayoutManager lm = new LinearLayoutManager(mainContext);
            lm.setOrientation(RecyclerView.HORIZONTAL);
            title = itemView.findViewById(R.id.main_tag_title);
            releaseList = itemView.findViewById(R.id.main_tag);
            releaseList.setLayoutManager(lm);
            releaseList.setAdapter(radapter);
            radapter.setClickListener(new MainTagAdapter.tagOnclick() {
                @Override
                public void onClick(int position, String tag) {
                    mainClickListener.clickedRelease(tag);
                }
            });
        }
    }

    public void setMainClickListener(onItemClick main) {
        this.mainClickListener = main;
    }

    public interface onItemClick{
        void clickedManga(Manga m);
        void clickedTag(String t);
        void clickedName(String t);
        void clickedRelease(String t);
        void clickedTitle(Title t);
        void clickedMoreUpdated();
        void captchaCallback();
    }

    private class fetchMain extends AsyncTask<Void, Integer, MainPage> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected MainPage doInBackground(Void... params) {
            Map<String,String> cookie = new HashMap<>();
            Login login = p.getLogin();
            if(login!=null && login.isValid()){
                p.getLogin().buildCookie(cookie);
            }
            MainPage u = new MainPage(httpClient);
            return u;
        }

        @Override
        protected void onPostExecute(MainPage main) {
            super.onPostExecute(main);
            //update adapters?
            if(main.getRecent().size() == 0){
                // captcha?
                mainClickListener.captchaCallback();
            }
            uadapter.setData(main.getRecent());
            ranking = main.getRanking();
            recent = main.getOnlineRecent();
            favUpdate = main.getFavUpdate();
            updateRankingWidgets();
        }
    }


}

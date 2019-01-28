package ml.melun.mangaview.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.MainPage;
import ml.melun.mangaview.mangaview.Manga;

public class mainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context mainContext;
    LayoutInflater mInflater, itemInflater;
    LinearLayoutManager update_lm, name_lm, tag_lm, release_lm;
    mainUpdatedAdapter uadapter;
    onItemClick mainClickListener;
    mainTagAdapter nadapter, tadapter, radapter;
    Boolean dark, loaded = false;

    ArrayList<Manga> ranking;

    public mainAdapter(Context main) {
        super();
        mainContext = main;
        dark = new Preference(mainContext).getDarkTheme();
        this.mInflater = LayoutInflater.from(main);
        this.itemInflater = LayoutInflater.from(main);

        update_lm = new LinearLayoutManager(main);
        tag_lm = new LinearLayoutManager(main);
        name_lm = new LinearLayoutManager(main);
        release_lm  = new LinearLayoutManager(main);

        update_lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        tag_lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        name_lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        release_lm.setOrientation(LinearLayoutManager.HORIZONTAL);

        uadapter = new mainUpdatedAdapter(main);
        tadapter = new mainTagAdapter(main, Arrays.asList(mainContext.getResources().getStringArray(R.array.tag_genre)), 0);
        nadapter = new mainTagAdapter(main, Arrays.asList(mainContext.getResources().getStringArray(R.array.tag_name)), 1);
        radapter = new mainTagAdapter(main, Arrays.asList(mainContext.getResources().getStringArray(R.array.tag_release)), 2);


        ranking = new ArrayList<>();
        ranking.add(new Manga(-1,"로드중..",""));


        //fetch main page data
        fetchMain fetch = new fetchMain();
        fetch.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.main_tags, parent, false);
        switch (viewType){
            case 0:
                return new addedHolder(view);
            case 2:
                return new tagHolder(view);
            case 1:
                return new nameHolder(view);
            case 3:
                return new releaseHolder(view);
            case 4:
                //return ranking title
                View rankingTitle = mInflater.inflate(R.layout.main_item_title,parent,false);
                return new rankingHolder(rankingTitle);
        }
        View rankingItem = mInflater.inflate(R.layout.main_item_ranking,parent,false);
        return new rankingHolder(rankingItem);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(position<5) {
            switch (position) {
                case 0:
                    addedHolder rh = (addedHolder) holder;
                    rh.title.setText("최근 추가된 만화 +");
                    break;
                case 2:
                    tagHolder th = (tagHolder) holder;
                    th.title.setText("장르별");
                    break;
                case 1:
                    nameHolder nh = (nameHolder) holder;
                    nh.title.setText("이름별");
                    break;
                case 3:
                    releaseHolder rlh = (releaseHolder) holder;
                    rlh.title.setText("발행별");
                    break;
                case 4:
                    rankingHolder rkh = (rankingHolder) holder;
                    rkh.title.setText("주간랭킹 TOP30");
                    break;
            }
        }else {
            rankingHolder rkh = (rankingHolder) holder;
            rkh.title.setText(ranking.get(position - 5).getName());
            rkh.rank.setText(position-4+"");
        }
    }

    @Override
    public int getItemCount() {
        return 5 + ranking.size();
    }

    public void updateRanking(){
        if(ranking.size()>0) {
            notifyItemChanged(5);
            notifyItemRangeInserted(6, 5 + ranking.size());
            loaded = true;
        }else{
            ranking.add(new Manga(-1,"결과없음",""));
            notifyItemChanged(6);
            loaded = false;
        }
    }

    class addedHolder extends RecyclerView.ViewHolder{
        RecyclerView updatedList;
        TextView title;
        public addedHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.main_tag_title);
            updatedList = itemView.findViewById(R.id.main_tag);
            updatedList.setLayoutManager(update_lm);
            updatedList.setAdapter(uadapter);
            uadapter.setClickListener(new mainUpdatedAdapter.onclick() {
                @Override
                public void onclick(Manga m) {
                    mainClickListener.clickedManga(m);
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
        public rankingHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.ranking_title);
            card = itemView.findViewById(R.id.ranking_card);
            rank = itemView.findViewById(R.id.ranking_rank);
            if(card!=null){
                if(dark){
                    card.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
                }
                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(loaded) {
                            mainClickListener.clickedManga(ranking.get(getAdapterPosition() - 5));
                        }
                    }
                });
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
            tagList.setLayoutManager(tag_lm);
            tagList.setAdapter(tadapter);
            tadapter.setClickListener(new mainTagAdapter.tagOnclick() {
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
            nameList.setLayoutManager(name_lm);
            nameList.setAdapter(nadapter);
            nadapter.setClickListener(new mainTagAdapter.tagOnclick() {
                @Override
                public void onClick(int pos, String tag) {
                    try {
                        mainClickListener.clickedName(pos);
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
            title = itemView.findViewById(R.id.main_tag_title);
            releaseList = itemView.findViewById(R.id.main_tag);
            releaseList.setLayoutManager(release_lm);
            releaseList.setAdapter(radapter);
            radapter.setClickListener(new mainTagAdapter.tagOnclick() {
                @Override
                public void onClick(int position, String tag) {
                    mainClickListener.clickedRelease(position);
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
        void clickedName(int t);
        void clickedRelease(int t);
        void clickedMoreUpdated();
    }

    private class fetchMain extends AsyncTask<Void, Integer, MainPage> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected MainPage doInBackground(Void... params) {
            MainPage u = new MainPage();
            return u;
        }

        @Override
        protected void onPostExecute(MainPage main) {
            super.onPostExecute(main);
            System.out.println("fetch update success");
            //update adapters?
            uadapter.setData(main.getRecent());
            ranking = main.getRanking();
            updateRanking();
        }
    }


}

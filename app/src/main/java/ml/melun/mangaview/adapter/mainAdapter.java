package ml.melun.mangaview.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;

public class mainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context mainContext;
    LayoutInflater mInflater;
    LinearLayoutManager update_lm, name_lm, tag_lm, release_lm;
    mainUpdatedAdapter uadapter;
    onItemClick mainClickListener;
    mainTagAdapter nadapter, tadapter, radapter;

    public mainAdapter(Context main) {
        super();
        mainContext = main;
        this.mInflater = LayoutInflater.from(main);

        update_lm = new LinearLayoutManager(main);
        tag_lm = new LinearLayoutManager(main);
        name_lm = new LinearLayoutManager(main);
        release_lm  = new LinearLayoutManager(main);

        update_lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        tag_lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        name_lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        release_lm.setOrientation(LinearLayoutManager.HORIZONTAL);

        uadapter = new mainUpdatedAdapter(main);
        List<String> tags = Arrays.asList("17","BL","GL","SF","TS","개그","게임","공포","도박","드라마","라노벨",
                "러브코미디","로맨스","먹방","미스테리","백합","붕탁","순정","스릴러","스포츠","시대","애니화","액션",
                "역사","요리","음악","이세계","일상","전생","추리","판타지","하렘","학원","호러");
        tadapter = new mainTagAdapter(main, tags, 0);
        List<String> names = Arrays.asList("ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ",
                "ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ");
        nadapter = new mainTagAdapter(main, names, 1);
        radapter = new mainTagAdapter(main, Arrays.asList("미분류","주간","격주","월간","격월/비정기","단편","단행본","완결"), 2);
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
                return new recyclerHolder(view);
            case 2:
                return new tagHolder(view);
            case 1:
                return new nameHolder(view);
            case 3:
                return new releaseHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (position){
            case 0:
                recyclerHolder rh = (recyclerHolder) holder;
                rh.title.setText("최근 추가된 만화");
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
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    class recyclerHolder extends RecyclerView.ViewHolder{
        RecyclerView updatedList;
        TextView title;
        public recyclerHolder(View itemView) {
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
    }


}

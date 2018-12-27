package ml.melun.mangaview.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Updated;

public class mainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context mainContext;
    LayoutInflater mInflater;
    LinearLayoutManager update_lm, name_lm, tag_lm;
    mainUpdatedAdapter uadapter;
    onItemClick mainClickListener;
    TagAdapter nadapter, tadapter;

    public mainAdapter(Context main) {
        super();
        mainContext = main;
        this.mInflater = LayoutInflater.from(main);

        update_lm = new LinearLayoutManager(main);
        tag_lm = new LinearLayoutManager(main);
        name_lm = new LinearLayoutManager(main);

        update_lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        tag_lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        name_lm.setOrientation(LinearLayoutManager.HORIZONTAL);

        uadapter = new mainUpdatedAdapter(main);
        List<String> tags = Arrays.asList("17","BL","GL","SF","TS","개그","게임","공포","도박","드라마","라노벨",
                "러브코미디","로맨스","먹방","미스테리","백합","붕탁","순정","스릴러","스포츠","시대","애니화","액션",
                "역사","요리","음악","이세계","일상","전생","추리","판타지","하렘","학원","호러");
        tadapter = new TagAdapter(main, tags);
        List<String> names = Arrays.asList("ㄱ","ㄴ","ㄷ","ㄹ","ㅁ","ㅂ","ㅅ",
                "ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ");
        nadapter = new TagAdapter(main, names);
    }


    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;
        switch(viewType) {
            case 0:
                //recently added manga list
                view = mInflater.inflate(R.layout.main_updated, parent, false);
                return new recyclerHolder(view);
            case 1:
                //search manga by tag
                view= mInflater.inflate(R.layout.main_tags, parent, false);
                return new tagHolder(view);
            case 2:
                //search manga by release type
                view= mInflater.inflate(R.layout.main_tags, parent, false);
                return new nameHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (position){
            case 0:
                recyclerHolder h = (recyclerHolder) holder;

                break;

        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    class recyclerHolder extends RecyclerView.ViewHolder{
        RecyclerView updatedList;
        public recyclerHolder(View itemView) {
            super(itemView);
            updatedList = itemView.findViewById(R.id.updatedList);
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
        public tagHolder(View itemView) {
            super(itemView);
            tagList = itemView.findViewById(R.id.main_tag);
            tagList.setLayoutManager(tag_lm);
            tagList.setAdapter(tadapter);
            tadapter.setClickListener(new TagAdapter.tagOnclick() {
                @Override
                public void onClick(String tag) {
                    mainClickListener.clickedTag(tag);
                }
            });
        }
    }
    class nameHolder extends RecyclerView.ViewHolder{
        RecyclerView nameList;
        public nameHolder(View itemView) {
            super(itemView);
            nameList = itemView.findViewById(R.id.main_tag);
            nameList.setLayoutManager(name_lm);
            nameList.setAdapter(nadapter);
            nadapter.setClickListener(new TagAdapter.tagOnclick() {
                @Override
                public void onClick(String tag) {
                    mainClickListener.clickedTag(tag);
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
    }


}

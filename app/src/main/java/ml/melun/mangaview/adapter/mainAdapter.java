package ml.melun.mangaview.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Updated;

public class mainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context mainContext;
    LayoutInflater mInflater;
    LinearLayoutManager update_lm;
    mainUpdatedAdapter adapter1;
    onItemClick mainClickListener;

    public mainAdapter(Context main) {
        super();
        mainContext = main;
        this.mInflater = LayoutInflater.from(main);
        update_lm = new LinearLayoutManager(main);
        update_lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        adapter1 = new mainUpdatedAdapter(main);
    }

    @Override
    public long getItemId(int position) {
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
                break;
            case 2:
                //search manga by release type
                break;
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
        return 1;
    }

    class recyclerHolder extends RecyclerView.ViewHolder{
        RecyclerView updatedList;
        public recyclerHolder(View itemView) {
            super(itemView);
            updatedList = itemView.findViewById(R.id.updatedList);
            updatedList.setLayoutManager(update_lm);
            updatedList.setAdapter(adapter1);
            adapter1.setClickListener(new mainUpdatedAdapter.onclick() {
                @Override
                public void onclick(Manga m) {
                    mainClickListener.clickedManga(m);
                }
            });
        }
    }

    public void setMainClickListener(onItemClick main) {
        this.mainClickListener = main;
    }

    public interface onItemClick{
        void clickedManga(Manga m);
        void clickedTag();
    }


}

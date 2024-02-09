package ml.melun.mangaview.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.MainPageWebtoon;
import ml.melun.mangaview.mangaview.Ranking;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

public class MainWebtoonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    LinearLayoutManager manager;
    Context context;
    boolean dark;
    LayoutInflater inflater;
    List<Ranking<?>> dataSet;
    MainAdapter.onItemClick listener;

    final static int HEADER = 23;

    final static int NN = 24;
    final static int AN = 25;
    final static int GN = 26;
    final static int CN = 27;
    final static int NB = 28;
    final static int AB = 29;
    final static int GB = 30;
    final static int CB = 31;

    int[] headers = {0,1,2,3,4,5,6,7};

    public MainWebtoonAdapter(Context context){
        this.context = context;
        this.dark = p.getDarkTheme();
        manager = new NpaLinearLayoutManager(context);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        inflater = LayoutInflater.from(context);
        dataSet = MainPageWebtoon.getBlankDataSet();
        setLoading();
        setHasStableIds(false);
        setLoading();
    }

    public void fetch(){
        new Fetcher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setLoading(){
        // show loading status to user
        dataSet = MainPageWebtoon.getBlankDataSet();
        notifyDataSetChanged();
    }

    public void setListener(MainAdapter.onItemClick listener){
        this.listener = listener;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == HEADER){
            return new HeaderHolder(inflater.inflate(R.layout.item_main_header, parent, false));
        }else{
            return new ItemHolder(inflater.inflate(R.layout.main_item_ranking, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if(dataSet.size()==0)
            return;
        if(type == HEADER){
            HeaderHolder h = (HeaderHolder) holder;
            for(int i=0; i<headers.length; i++){
                if(headers[i] == position) {
                    h.title.setText(dataSet.get(i).getName());
                    break;
                }
            }
        }else if(type<=CB){
            ItemHolder h = (ItemHolder) holder;
            int setIndex = type-24;
            int realPosition = position-(headers[setIndex])-1;
            Object d = dataSet.get(setIndex).get(realPosition);
            h.rank.setText(String.valueOf(realPosition+1));
            if(d instanceof String){
                //is search
                h.content.setText((String)d);
            }else if(d instanceof Title){
                //is title
                h.content.setText(((Title) d).getName());
            }
            h.card.setOnClickListener(view -> {
                //title
                listener.clickedTitle((Title)d);
            });
        }
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void refreshHeaders(){
        headers[0] = 0;
        for(int i=1; i<dataSet.size(); i++){
            headers[i]=headers[i-1]+dataSet.get(i-1).size()+1;
        }
    }

    @Override
    public int getItemViewType(int position) {
        //first header
        if(position == 0)
            return HEADER;
        refreshHeaders();
        //last items
        if(position>headers[headers.length-1]){
            return headers.length+24-1;
        }
        for(int i=1; i<headers.length; i++){
            if(position == headers[i])
                return HEADER;
            else if(position>headers[i-1] && position<headers[i])
                return i+23;
        }

        return -1;
    }

    public void updateWidgets(){
        refreshHeaders();
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        int sum = headers.length;
        for(Ranking<?> r : dataSet){
            sum+= r.size();
        }
        return sum;
    }

    class ItemHolder extends RecyclerView.ViewHolder{
        TextView rank;
        TextView content;
        CardView card;
        public ItemHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.ranking_card);
            rank = itemView.findViewById(R.id.ranking_rank);
            content = itemView.findViewById(R.id.header_title);
            if(dark)
                card.setBackgroundColor(ContextCompat.getColor(context, R.color.colorDarkBackground));
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder{
        TextView title;
        public HeaderHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.header_title);
        }
    }

    private class Fetcher extends AsyncTask<Void, Integer, MainPageWebtoon> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected MainPageWebtoon doInBackground(Void... params) {
            return new MainPageWebtoon(httpClient);
        }

        @Override
        protected void onPostExecute(MainPageWebtoon main) {
            super.onPostExecute(main);
            //update adapters?
            dataSet = main.getDataSet();
            if(dataSet == null)
                dataSet = main.getBlankDataSet();
            for (Ranking<?> r : dataSet) {
                if (r==null || r.size() == 0) {
                    // captcha?
                    listener.captchaCallback();
                    return;
                }
            }
            updateWidgets();
        }
    }




}

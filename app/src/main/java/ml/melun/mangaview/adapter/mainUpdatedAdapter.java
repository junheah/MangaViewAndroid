package ml.melun.mangaview.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Updated;

public class mainUpdatedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    ArrayList<Manga> mData;
    Context context;
    Fetch fetch;
    LayoutInflater mInflater;
    Boolean loaded = false;
    onclick monclick;

    public mainUpdatedAdapter(Context c) {
        super();
        context = c;
        mData = new ArrayList<>();
        mData.add(new Manga(0,"로드중..."));
        this.mInflater = LayoutInflater.from(c);
        Fetch fetch = new Fetch();
        fetch.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        //fetch data with async
        //data initialize
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_updated, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        viewHolder h = (viewHolder) holder;
        h.title.setText(mData.get(position).getName());
        Glide.with(context).load(mData.get(position).getThumb()).into(h.thumb);
    }


    @Override
    public int getItemCount() {
        return mData.size();
    }
    public interface onclick{
        void onclick(Manga m);
    }

    class viewHolder extends RecyclerView.ViewHolder{
        ImageView thumb;
        TextView title;
        CardView card;
        public viewHolder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.main_new_thumb);
            title = itemView.findViewById(R.id.main_new_name);
            card = itemView.findViewById(R.id.updatedCard);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(loaded){
                        monclick.onclick(mData.get(getAdapterPosition()));
                    }
                }
            });

        }
    }
    public void setClickListener(onclick o){
        this.monclick = o;
    }

    private class Fetch extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            Updated u = new Updated();
            mData = u.getResult();
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            System.out.println("ppppppppppppppppnewfetchworks!");
            //notifyDataSetChanged();
            notifyItemChanged(0);
            notifyItemRangeInserted(1,mData.size()-1);
            loaded = true;
        }
    }
}

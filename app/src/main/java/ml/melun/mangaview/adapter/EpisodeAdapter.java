package ml.melun.mangaview.adapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;


public class EpisodeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Manga> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context mainContext;
    TypedValue outValue;
    private int selected = -1;
    //header is in index 0
    Title header;

    // data is passed into the constructor
    public EpisodeAdapter(Context context, ArrayList<Manga> data, Title title) {
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.mData = data;
        this.header = title;
        outValue = new TypedValue();
        mainContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
    }

    @Override
    public int getItemViewType(int position) {
        if(position==0) return 0;
        else return 1;
    }

    // inflates the row layout from xml when needed
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        if(viewType==0) {
            view = mInflater.inflate(R.layout.item_header, parent, false);
            return new HeaderHolder(view);
        }else {
            view = mInflater.inflate(R.layout.item_episode, parent, false);
            return new ViewHolder(view);
        }
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(position==0){
            HeaderHolder h = (HeaderHolder) holder;

            String title = header.getName();
            String thumb = header.getThumb();
            h.h_title.setText(title);
            Glide.with(mainContext).load(thumb).into(h.h_thumb);

        }else {
            ViewHolder h = (ViewHolder) holder;

            String episode = mData.get(position).getName();
            h.episode.setText(episode);
            if (position == selected)
                h.itemView.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.selected));
            else h.itemView.setBackgroundResource(outValue.resourceId);
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView episode;
        ViewHolder(View itemView) {
            super(itemView);
            episode = itemView.findViewById(R.id.episode);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(selected!=-1){
                int pre = selected;
                notifyItemChanged(pre);
            }
            selected = getAdapterPosition();
            notifyItemChanged(selected);
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }
    public class HeaderHolder extends RecyclerView.ViewHolder{
        TextView h_title;
        ImageView h_thumb;
        HeaderHolder(View itemView) {
            super(itemView);
            h_title = itemView.findViewById(R.id.HeaderTitle);
            h_thumb = itemView.findViewById(R.id.HeaderThumb);
//            itemView.setOnClickListener(this);
        }
    }

    // convenience method for getting data at click position
    public Manga getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}

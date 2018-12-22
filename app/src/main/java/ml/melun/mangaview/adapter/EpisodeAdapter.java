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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;


public class EpisodeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Manga> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context mainContext;
    Boolean favorite = false;
    TypedValue outValue;
    private int bookmark = -1;
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
            if(favorite) h.h_star.setImageResource(R.drawable.star_on);
            else h.h_star.setImageResource(R.drawable.star_off);
            Glide.with(mainContext)
                    .load(thumb)
                    .apply(new RequestOptions().dontTransform())
                    .into(h.h_thumb);
        }else {
            ViewHolder h = (ViewHolder) holder;
            String episode = mData.get(position).getName();
            h.episode.setText(episode);
            if (position == bookmark) {
                h.itemView.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.selected));
            }
            else h.itemView.setBackgroundResource(R.drawable.item_bg);
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView episode;
        ViewHolder(View itemView) {
            super(itemView);
            episode = itemView.findViewById(R.id.episode);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(bookmark!=-1){
                        int pre = bookmark;
                        notifyItemChanged(pre);
                    }
                    bookmark = getAdapterPosition();
                    notifyItemChanged(bookmark);
                    mClickListener.onItemClick(v, getAdapterPosition());
                }
            });
        }
    }
    public class HeaderHolder extends RecyclerView.ViewHolder{
        TextView h_title;
        ImageView h_thumb;
        ImageView h_star;
        Button h_download;
        HeaderHolder(View itemView) {
            super(itemView);
            h_title = itemView.findViewById(R.id.HeaderTitle);
            h_thumb = itemView.findViewById(R.id.HeaderThumb);
            h_star = itemView.findViewById(R.id.FavoriteButton);
            h_download = itemView.findViewById(R.id.HeaderDownload);
            h_star.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onStarClick(v);
                }
            });
            h_download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onDownloadClick(v);
                }
            });
        }
    }

    // convenience method for getting data at click position
    public Manga getItem(int id) {
        return mData.get(id);
    }

    public void setFavorite(Boolean b){
        if(favorite!=b) {
            favorite = b;
            notifyItemChanged(0);
        }
    }

    public void setBookmark(int i){
        //THIS SHOULD BE SET TO INDEX, NOT ID!
        bookmark = i;
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
        void onStarClick(View view);
        void onDownloadClick(View view);
    }
}

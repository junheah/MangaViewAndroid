package ml.melun.mangaview.adapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;


public class EpisodeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Manga> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context mainContext;
    String[] releases = {"미분류","주간","격주","월간","격월/비정기","단편","단행본","완결"};
    Boolean favorite = false;
    TypedValue outValue;
    private int bookmark = -1;
    //header is in index 0
    Title header;
    TagAdapter ta;
    LinearLayoutManager lm;
    Boolean dark;
    Boolean save;
    Boolean online;

    // data is passed into the constructor
    public EpisodeAdapter(Context context, List<Manga> data, Title title, Boolean online) {
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.mData = data;
        this.header = title;
        this.online = online;
        outValue = new TypedValue();
        dark = new Preference(context).getDarkTheme();
        save = new Preference(context).getDataSave();
        mainContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        if(title.getTags()!=null) {
            ta = new TagAdapter(context, title.getTags());
            lm = new LinearLayoutManager(context);
            lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        }
        setHasStableIds(true);
        if(!online) save = false;
    }

    @Override
    public long getItemId(int position) {
        return position;
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
            int release = header.getRelease();
            h.h_title.setText(title);
            h.h_author.setText(header.getAuthor());
            if(release>-1) h.h_release.setText(releases[release]);
            else h.h_release.setText("");
            if(favorite) h.h_star.setImageResource(R.drawable.star_on);
            else h.h_star.setImageResource(R.drawable.star_off);
            if(!save) Glide.with(mainContext)
                    .load(thumb)
                    .apply(new RequestOptions().dontTransform())
                    .into(h.h_thumb);
            if(online){
                ((HeaderHolder) holder).h_download.setVisibility(View.VISIBLE);
            }else{
                ((HeaderHolder) holder).h_download.setVisibility(View.GONE);
            }
        }else {
            ViewHolder h = (ViewHolder) holder;
            int Dposition = position-1;
            h.episode.setText(mData.get(Dposition).getName());
            h.date.setText(mData.get(Dposition).getDate());
            if (position == bookmark) {
                if(dark) h.itemView.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.selectedDark));
                else h.itemView.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.selected));
            }
            else{
                if(dark) h.itemView.setBackgroundResource(R.drawable.item_bg_dark);
                else h.itemView.setBackgroundResource(R.drawable.item_bg);
            }
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size()+1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView episode,date;
        ViewHolder(View itemView) {
            super(itemView);
            episode = itemView.findViewById(R.id.episode);
            date = itemView.findViewById(R.id.date);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Manga m = mData.get(getAdapterPosition()-1);
                    if(m.getId()>-1) {
                        if (bookmark != -1) {
                            int pre = bookmark;
                            notifyItemChanged(pre);
                        }
                        bookmark = getAdapterPosition();
                        notifyItemChanged(bookmark);
                    }
                    mClickListener.onItemClick(getAdapterPosition()-1, m);
                }
            });
        }
    }
    public class HeaderHolder extends RecyclerView.ViewHolder{
        TextView h_title, h_author, h_release;
        ImageView h_thumb;
        ImageView h_star;
        Button h_download;
        RecyclerView h_tags;
        HeaderHolder(View itemView) {
            super(itemView);
            h_title = itemView.findViewById(R.id.HeaderTitle);
            h_thumb = itemView.findViewById(R.id.HeaderThumb);
            h_star = itemView.findViewById(R.id.FavoriteButton);
            h_download = itemView.findViewById(R.id.HeaderDownload);
            h_tags = itemView.findViewById(R.id.tagsContainer);
            h_author = itemView.findViewById(R.id.headerAuthor);
            h_release = itemView.findViewById(R.id.HeaderRelease);
            h_star.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onStarClick();
                }
            });
            h_download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onDownloadClick();
                }
            });
            h_author.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onAuthorClick();
                }
            });
            if(ta!=null) {
                h_tags.setLayoutManager(lm);
                h_tags.setAdapter(ta);
            }
            if(dark) h_tags.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.selectedDark));
        }
    }

    public void setFavorite(Boolean b){
        if(favorite!=b) {
            favorite = b;
            notifyItemChanged(0);
        }
    }

    public void setBookmark(int i){
        //THIS SHOULD BE SET TO INDEX, NOT ID! : because of notifyitemChanged
        //i is real index in recyclerview
        if(i!=bookmark){
            int tmp = bookmark;
            bookmark = i;
            if(tmp>0) notifyItemChanged(tmp);
            if(bookmark>0) notifyItemChanged(bookmark);
        }
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public void setTagClickListener(TagAdapter.tagOnclick t){
        ta.setClickListener(t);
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position, Manga m);
        void onStarClick();
        void onDownloadClick();
        void onAuthorClick();
    }
}

package ml.melun.mangaview.adapter;
import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.ImageView;

import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;


import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.mangaview.MTitle.releases;


public class EpisodeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Manga> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context mainContext;
    boolean favorite = false;
    boolean bookmarked = false;
    TypedValue outValue;
    private int bookmark = -1;
    //title is in index 0
    Title title;
    TagAdapter ta;
    LinearLayoutManager lm;
    boolean dark;
    boolean save;
    int mode = 0;
    boolean login;

    // data is passed into the constructor
    public EpisodeAdapter(Context context, List<Manga> data, Title title, int mode) {
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.mData = data;
        this.title = title;
        this.mode = mode;
        outValue = new TypedValue();
        dark = p.getDarkTheme();
        save = p.getDataSave();
        bookmarked = title.getBookmarked();
        mainContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        if(title.getTags()!=null) {
            ta = new TagAdapter(context, title.getTags());
            lm = new LinearLayoutManager(context);
            lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        }
        setHasStableIds(true);
        if(mode != 0) save = false;
        login = mode == 0 && p.getLogin() != null && p.getLogin().isValid();
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
            String titles = this.title.getName();
            String thumb = this.title.getThumb();
            int release = this.title.getRelease();
            h.h_title.setText(titles);
            h.h_author.setText(this.title.getAuthor());
            if(release>-1) h.h_release.setText(releases[release]);
            else h.h_release.setText("");
            if(favorite) h.h_star_icon.setImageResource(R.drawable.ic_favorite);
            else h.h_star_icon.setImageResource(R.drawable.ic_favorite_border);
            if(bookmarked) h.h_bookmark_icon.setImageResource(R.drawable.ic_bookmark);
            else h.h_bookmark_icon.setImageResource(R.drawable.ic_bookmark_border);

            if(!save) Glide.with(mainContext)
                    .load(thumb)
                    .apply(new RequestOptions().dontTransform())
                    .into(h.h_thumb);
            if(mode == 0 || mode == 3)
                h.h_star.setVisibility(View.VISIBLE);
            else
                h.h_star.setVisibility(View.GONE);

            if(mode == 0){
                h.h_download.setVisibility(View.VISIBLE);

                //set ext-info text
                h.h_recommend_c.setText(String.valueOf(title.getRecommend_c()));
                h.h_battery_c.setText(String.valueOf(title.getBattery_s()));
                h.h_comment_c.setText(String.valueOf(title.getComment_c()));
                h.h_bookmark_c.setText(String.valueOf(title.getBookmark_c()));

            }else{
                //offline manga
                h.h_download.setVisibility(View.GONE);
                h.h_bookmark.setVisibility(View.GONE);
                h.h_battery.setVisibility(View.GONE);
                h.h_recommend.setVisibility(View.GONE);
                h.h_comment.setVisibility(View.GONE);
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
        ImageView h_star_icon;
        ImageView h_bookmark_icon;

        Button h_download;
        RecyclerView h_tags;
        View h_bookmark, h_star, h_recommend, h_comment, h_battery;

        TextView h_bookmark_c, h_recommend_c, h_comment_c, h_battery_c;
        HeaderHolder(View itemView) {
            super(itemView);
            h_title = itemView.findViewById(R.id.HeaderTitle);
            h_thumb = itemView.findViewById(R.id.HeaderThumb);
            h_star_icon = itemView.findViewById(R.id.favoriteIcon);
            h_download = itemView.findViewById(R.id.HeaderDownload);
            h_tags = itemView.findViewById(R.id.tagsContainer);
            h_author = itemView.findViewById(R.id.headerAuthor);
            h_release = itemView.findViewById(R.id.HeaderRelease);
            h_bookmark_icon = itemView.findViewById(R.id.bookmarkIcon);

            h_star = itemView.findViewById(R.id.HeaderFavorite);
            h_bookmark = itemView.findViewById(R.id.HeaderBookmark);
            h_comment = itemView.findViewById(R.id.HeaderComment);
            h_recommend = itemView.findViewById(R.id.HeaderRecommend);
            h_battery = itemView.findViewById(R.id.HeaderBattery);

            h_bookmark_c = itemView.findViewById(R.id.bookmarkText);
            h_comment_c = itemView.findViewById(R.id.commentText);
            h_recommend_c = itemView.findViewById(R.id.recommendText);
            h_battery_c = itemView.findViewById(R.id.batteryText);


            h_bookmark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //set bookmark
                    mClickListener.onBookmarkClick();
                    if(login) {
                        bookmarked = !bookmarked;
                        notifyItemChanged(0);
                    }
                }
            });
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

    public void setFavorite(boolean b){
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
        void onBookmarkClick();
    }
}

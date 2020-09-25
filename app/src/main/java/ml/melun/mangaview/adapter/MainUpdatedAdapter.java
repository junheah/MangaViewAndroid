package ml.melun.mangaview.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.mangaview.MTitle.base_auto;

public class MainUpdatedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    List<Manga> mData;
    Context context;
    LayoutInflater mInflater;
    boolean loaded = false;
    onclick monclick;
    boolean dark, save;

    public MainUpdatedAdapter(Context c) {
        context = c;
        this.mInflater = LayoutInflater.from(c);
        dark = p.getDarkTheme();
        save = p.getDataSave();

        //fetch data with async
        //data initialize
        setHasStableIds(true);
        //setNull();
    }

    public void setNull(){
        if(mData != null){
            mData.clear();
            loaded = false;
        }
        else
            mData = new ArrayList<>();
        mData.add(new Manga(0,"","", base_auto));
        notifyDataSetChanged();
    }

    public void setLoad(){
        setLoad("로드중...");
    }

    public void setLoad(String msg){
        if(mData != null){
            mData.clear();
            loaded = false;
        }
        else
            mData = new ArrayList<>();
        Manga loading = new Manga(0,msg,"", base_auto);
        loading.addThumb("");
        mData.add(loading);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
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
        String thumb = mData.get(position).getThumb();
        if(thumb != null && thumb.length()==0)
            h.thumb.setImageResource(android.R.color.transparent);
        else if(thumb != null && thumb.equals("reload"))
            h.thumb.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_refresh));
        else if(save)
            h.thumb.setImageDrawable(context.getResources().getDrawable(R.mipmap.ic_launcher));
        else
            Glide.with(context).load(thumb).into(h.thumb);

        if(p.getBookmark(mData.get(position).getTitle())>0)
            h.seen.setVisibility(View.VISIBLE);
        else
            h.seen.setVisibility(View.GONE);
    }


    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }
    public interface onclick{
        void onclick(Manga m);
        void refresh();
    }

    class viewHolder extends RecyclerView.ViewHolder{
        ImageView thumb;
        TextView title;
        CardView card;
        ImageView seen;
        public viewHolder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.main_new_thumb);
            title = itemView.findViewById(R.id.main_new_name);
            seen = itemView.findViewById(R.id.seenIcon);
            title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            title.setMarqueeRepeatLimit(-1);
            title.setSingleLine(true);
            title.setSelected(true);
            card = itemView.findViewById(R.id.updatedCard);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(loaded){
                        monclick.onclick(mData.get(getAdapterPosition()));
                    }else
                        monclick.refresh();
                }
            });
            if(dark){
                card.setBackgroundColor(ContextCompat.getColor(context, R.color.colorDarkBackground));
            }

        }
    }
    public void setClickListener(onclick o){
        this.monclick = o;
    }

    public void setData(List<Manga> data){
        mData = data;
        if(mData.size()==0){
            Manga none = new Manga(0,"결과 없음","", base_auto);
            none.addThumb("reload");
            mData.add(none);
            notifyItemChanged(0);
            loaded = false;
        }else {
            notifyItemChanged(0);
            notifyItemRangeInserted(1, mData.size() - 1);
            loaded = true;
        }

    }
}

package ml.melun.mangaview.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.content.res.Resources;
import android.graphics.Color;
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
    OnClickCallback monclick;
    boolean dark, save;
    Resources res;

    public MainUpdatedAdapter(Context c) {
        context = c;
        this.mInflater = LayoutInflater.from(c);
        dark = p.getDarkTheme();
        save = p.getDataSave();
        this.res = context.getResources();

        //fetch data with async
        //data initialize
        setHasStableIds(true);
        //setNull();
    }

    public void setLoad(){
        setLoad("로드중...");
    }

    public void setLoad(String msg){
        if(mData != null){
            int size = mData.size();
            mData.clear();
            loaded = false;
            notifyItemRangeRemoved(0,size);
        }
        else
            mData = new ArrayList<>();
        Manga loading = new Manga(0,msg,"", base_auto);
        loading.addThumb("");
        mData.add(loading);
        notifyItemInserted(0);
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
        h.thumb.setColorFilter(null);
        if(thumb != null && thumb.length()==0)
            h.thumb.setImageResource(android.R.color.transparent);
        else if(thumb != null && thumb.equals("reload")) {
            h.thumb.setImageDrawable(ResourcesCompat.getDrawable(res, R.drawable.ic_refresh, null));
            h.thumb.setColorFilter(dark ? Color.WHITE : Color.DKGRAY);
        }else if(save)
            h.thumb.setImageDrawable(ResourcesCompat.getDrawable(res, R.mipmap.ic_launcher, null));
        else
            Glide.with(h.thumb).load(thumb).into(h.thumb);
    }


    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }
    public interface OnClickCallback {
        void onclick(Manga m);
        void refresh();
    }

    class viewHolder extends RecyclerView.ViewHolder{
        ImageView thumb;
        TextView title;
        CardView card;
        public viewHolder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.main_new_thumb);
            title = itemView.findViewById(R.id.main_new_name);
            title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            title.setMarqueeRepeatLimit(-1);
            title.setSingleLine(true);
            title.setSelected(true);
            card = itemView.findViewById(R.id.updatedCard);
            card.setOnClickListener(v -> {
                if(loaded){
                    monclick.onclick(mData.get(getAdapterPosition()));
                }else
                    monclick.refresh();
            });
            if(dark){
                card.setBackgroundColor(ContextCompat.getColor(context, R.color.colorDarkBackground));
            }

        }
    }
    public void setClickListener(OnClickCallback o){
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

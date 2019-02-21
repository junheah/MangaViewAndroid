package ml.melun.mangaview.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.lang.reflect.Array;
import java.util.ArrayList;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.MainPage;

public class mainUpdatedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    ArrayList<Manga> mData;
    Context context;
    LayoutInflater mInflater;
    Boolean loaded = false;
    onclick monclick;
    Boolean dark, save;

    public mainUpdatedAdapter(Context c) {
        super();
        context = c;
        mData = new ArrayList<>();
        mData.add(new Manga(0,"로드중...",""));
        this.mInflater = LayoutInflater.from(c);
        dark = new Preference(c).getDarkTheme();
        save = new Preference(c).getDataSave();

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
        if(!save) Glide.with(context).load(mData.get(position).getThumb()).into(h.thumb);
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
            title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            title.setMarqueeRepeatLimit(-1);
            title.setSingleLine(true);
            title.setSelected(true);
            card = itemView.findViewById(R.id.updatedCard);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(loaded ){
                        monclick.onclick(mData.get(getAdapterPosition()));
                    }
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

    public void setData(ArrayList<Manga> data){
        mData = data;
        if(mData.size()==0){
            mData.add(new Manga(0,"결과 없음",""));
            notifyItemChanged(0);
            loaded = false;
        }else {
            notifyItemChanged(0);
            notifyItemRangeInserted(1, mData.size() - 1);
            loaded = true;
        }

    }
}

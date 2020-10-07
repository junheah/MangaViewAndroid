package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.List;

import static ml.melun.mangaview.MainApplication.p;
import ml.melun.mangaview.R;
import ml.melun.mangaview.activity.ViewerActivity;
import ml.melun.mangaview.mangaview.Decoder;


public class StripAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<String> imgs;
    private LayoutInflater mInflater;
    private Context mainContext;
    private StripAdapter.ItemClickListener mClickListener;
    boolean autoCut = false;
    boolean reverse;
    int __seed;
    Decoder d;
    int width;
    ViewerActivity.InfiniteScrollCallback callback;

    // data is passed into the constructor
    public StripAdapter(Context context, List<String> data, Boolean cut, int seed, int id, int width, ViewerActivity.InfiniteScrollCallback callback) {
        this.callback = callback;
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.imgs = data;
        autoCut = cut;
        reverse = p.getReverse();
        __seed = seed;
        d = new Decoder(seed, id);
        this.width = width;
        setHasStableIds(true);
    }

    public void preloadAll(){
        for(String s : imgs) {
            Glide.with(mainContext)
                    .load(s)
                    .preload();
        }
    }

    final static int IMG = 0;
    final static int INFO = 1;

    @Override
    public int getItemViewType(int position) {
        if(position == 0 || position == getImgCount()+1){
            return INFO;
        }else{
            return IMG;
        }
    }

    int getImgPos(int position){
        return position-1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void removeAll(){
        imgs.clear();
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == IMG) {
            View view = mInflater.inflate(R.layout.item_strip, parent, false);
            return new ImgViewHolder(view);
        }else{
            //INFO
            View view = mInflater.inflate(R.layout.item_strip_info, parent, false);
            return new InfoViewHolder(view);
        }
    }


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int pos) {
        if(getItemViewType(pos) == IMG) {
            ((ImgViewHolder)holder).frame.setImageResource(R.drawable.placeholder);
            ((ImgViewHolder)holder).refresh.setVisibility(View.VISIBLE);
            glideBind((ImgViewHolder)holder, getImgPos(pos));
        }else{
            //INFO
            if(pos == 0){
                callback.prevEp();
            }else{
                callback.nextEp();
            }
        }
    }

    void glideBind(ImgViewHolder holder, int pos){
        if (autoCut) {
            final int type = pos % 2;
            String image = imgs.get(pos / 2);
            //set image to holder view
            Glide.with(mainContext)
                    .asBitmap()
                    .load(image)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                            bitmap = d.decode(bitmap, width);
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            if (width > height) {
                                if (type == 0) {
                                    if (reverse)
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, width / 2, height));
                                    else
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, width / 2, 0, width / 2, height));
                                } else {
                                    if (reverse)
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, width / 2, 0, width / 2, height));
                                    else
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, width / 2, height));
                                }
                            } else {
                                if (type == 0) {
                                    holder.frame.setImageBitmap(bitmap);
                                } else {
                                    holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap.getWidth(), 1, Bitmap.Config.ARGB_8888));
                                }
                            }
                            holder.refresh.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            holder.frame.setImageDrawable(placeholder);
                            holder.refresh.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            if (imgs.size() > 0) {
                                holder.frame.setImageResource(R.drawable.placeholder);
                                holder.refresh.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        } else {
            String image = imgs.get(pos);
            Glide.with(mainContext)
                    .asBitmap()
                    .load(image)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            resource = d.decode(resource, width);
                            holder.frame.setImageBitmap(resource);
                            holder.refresh.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            holder.frame.setImageDrawable(placeholder);
                            holder.refresh.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            if (imgs.size() > 0) {
                                holder.frame.setImageResource(R.drawable.placeholder);
                                holder.refresh.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        }
    }


    // total number of rows
    @Override
    public int getItemCount() {
        return getImgCount() + 2;
    }

    public int getImgCount(){
        if(autoCut) return imgs.size()*2;
        else return imgs.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ImgViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView frame;
        ImageButton refresh;
        ImgViewHolder(View itemView) {
            super(itemView);
            frame = itemView.findViewById(R.id.frame);
            refresh = itemView.findViewById(R.id.refreshButton);
            refresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //refresh image
                    notifyItemChanged(getAdapterPosition());
                }
            });
            itemView.setOnClickListener(this);
        }
        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick();
        }
    }

    public class InfoViewHolder extends RecyclerView.ViewHolder{
        TextView prevInfo, nextInfo;
        ProgressBar loading;
        InfoViewHolder(View itemView) {
            super(itemView);
            prevInfo = itemView.findViewById(R.id.prevEpInfo);
            nextInfo = itemView.findViewById(R.id.nextEpInfo);
            loading = itemView.findViewById(R.id.infoLoading);
        }

    }

    // allows clicks events to be caught
    public void setClickListener(StripAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick();
    }
}


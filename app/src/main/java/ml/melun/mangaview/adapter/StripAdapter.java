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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.List;

import static ml.melun.mangaview.MainApplication.p;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Decoder;


public class StripAdapter extends RecyclerView.Adapter<StripAdapter.ViewHolder> {

    private List<String> imgs, imgs1;
    private LayoutInflater mInflater;
    private Context mainContext;
    private StripAdapter.ItemClickListener mClickListener;
    Boolean autoCut = false;
    Boolean reverse;
    Boolean error = false;
    int __seed;
    Decoder d;
    int width;
    Boolean useSecond = false;



    // data is passed into the constructor
    public StripAdapter(Context context, List<String> data, List<String> data1, Boolean cut, int seed, int id, int width) {
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.imgs = data;
        this.imgs1 = data1;
        autoCut = cut;
        reverse = p.getReverse();
        __seed = seed;
        d = new Decoder(seed, id);
        this.width = width;
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_strip, parent, false);
        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(final ViewHolder holder, final int pos) {
        holder.frame.setImageResource(R.drawable.placeholder);
        holder.refresh.setVisibility(View.VISIBLE);
        glideBind(holder, pos);
    }

    void glideBind(ViewHolder holder, int pos){
        if(autoCut) {
            final int position = pos / 2;
            final int type = pos % 2;
            String image = useSecond && imgs1!=null && imgs1.size()>0 ? imgs1.get(position) : imgs.get(position);
            if(error && !useSecond){
                image = image.indexOf("img.") > -1 ? image.replace("img.","s3.") : image.replace("://", "://s3.");
            }
            //set image to holder view
            Glide.with(mainContext)
                    .asBitmap()
                    .load(image)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                            bitmap = d.decode(bitmap,width);
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            if(width>height){
                                if(type==0) {
                                    if (reverse)
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, width / 2, height));
                                    else
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, width / 2, 0, width / 2, height));
                                }else{
                                    if (reverse)
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, width / 2, 0, width / 2, height));
                                    else
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, width / 2, height));
                                }
                            }else{
                                if(type==0) {
                                    holder.frame.setImageBitmap(bitmap);
                                }else{
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
                            if(imgs.size()>0) {
                                holder.frame.setImageResource(R.drawable.placeholder);
                                holder.refresh.setVisibility(View.VISIBLE);
                                if(!error){
                                    error = true;
                                    glideBind(holder,pos);
                                }else{
                                    useSecond = true;
                                    glideBind(holder,pos);
                                }
                            }
                        }
                    });
        }
        else {
            String image = useSecond && imgs1!=null && imgs1.size()>0 ? imgs1.get(pos) : imgs.get(pos);
            if(error && !useSecond){
                image = image.indexOf("img.") > -1 ? image.replace("img.","s3.") : image.replace("://", "://s3.");
            }
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
                                if (!error) {
                                    error = true;
                                    glideBind(holder, pos);
                                } else if(!useSecond) {
                                    useSecond = true;
                                    glideBind(holder, pos);
                                }
                            }
                        }
                    });
        }
    }


    // total number of rows
    @Override
    public int getItemCount() {
        if(autoCut) return imgs.size()*2;
        else return imgs.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView frame;
        ImageButton refresh;
        ViewHolder(View itemView) {
            super(itemView);
            frame = itemView.findViewById(R.id.frame);
            refresh = itemView.findViewById(R.id.refreshButton);
            refresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //refresh image
                    error = false;
                    useSecond = false;
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

    // allows clicks events to be caught
    public void setClickListener(StripAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick();
    }
}


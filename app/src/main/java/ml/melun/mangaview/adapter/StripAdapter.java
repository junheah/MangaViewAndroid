package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Decoder;

public class StripAdapter extends RecyclerView.Adapter<StripAdapter.ViewHolder> {

    private ArrayList<String> imgs;
    private LayoutInflater mInflater;
    private Context mainContext;
    private StripAdapter.ItemClickListener mClickListener;
    Boolean autoCut = false;
    Boolean reverse;
    int __seed;
    Decoder d;


    // data is passed into the constructor
    public StripAdapter(Context context, ArrayList<String> data, Boolean cut, int seed, int id) {
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.imgs = data;
        autoCut = cut;
        reverse = new Preference(context).getReverse();
        __seed = seed;
        d = new Decoder(seed, id);
    }
    public void removeAll(){
        imgs.clear();
        notifyDataSetChanged();
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_strip, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int pos) {
        holder.frame.setImageResource(R.drawable.placeholder);
        holder.refresh.setVisibility(View.VISIBLE);
        if(autoCut) {
            final int position = pos / 2;
            final int type = pos % 2;
            String image = imgs.get(position);
            //set image to holder view
            Glide.with(mainContext)
                    .asBitmap()
                    .load(image)
                    .into(new SimpleTarget<android.graphics.Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap,
                                                    Transition<? super Bitmap> transition) {
                            Bitmap decoded = d.decode(bitmap);
                            int width = decoded.getWidth();
                            int height = decoded.getHeight();
                            if(width>height){
                                if(type==0) {
                                    if (reverse)
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(decoded, 0, 0, width / 2, height));
                                    else
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(decoded, width / 2, 0, width / 2, height));
                                }else{
                                    if (reverse)
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(decoded, width / 2, 0, width / 2, height));
                                    else
                                         holder.frame.setImageBitmap(Bitmap.createBitmap(decoded, 0, 0, width / 2, height));
                                }
                            }else{
                                if(type==0) {
                                    holder.frame.setImageBitmap(decoded);
                                }else{
                                    holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap.getWidth(), 1, Bitmap.Config.ARGB_8888));
                                }
                            }
                            holder.refresh.setVisibility(View.GONE);
                        }
                    });
        }
        else Glide.with(mainContext)
                .asBitmap()
                .load(imgs.get(pos))
                .apply(new RequestOptions().dontTransform().placeholder(R.drawable.placeholder))
                .into(new SimpleTarget<android.graphics.Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap,
                                                Transition<? super Bitmap> transition) {
                        Bitmap decoded = d.decode(bitmap);
                        holder.frame.setImageBitmap(decoded);
                        holder.refresh.setVisibility(View.GONE);
                    }
                });
    }


    // total number of rows
    @Override
    public int getItemCount() {
        if(autoCut) return imgs.size()*2;
        else return imgs.size();
    }


    public String getItem(int index){return imgs.get(index);}

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


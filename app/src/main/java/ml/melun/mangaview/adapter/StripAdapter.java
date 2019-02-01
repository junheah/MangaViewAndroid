package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class StripAdapter extends RecyclerView.Adapter<StripAdapter.ViewHolder> {

    private ArrayList<String> imgs;
    private LayoutInflater mInflater;
    private Context mainContext;
    private StripAdapter.ItemClickListener mClickListener;
    Boolean autoCut = false;
    Boolean reverse;
    int __seed;


    // data is passed into the constructor
    public StripAdapter(Context context, ArrayList<String> data, Boolean cut, int seed) {
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.imgs = data;
        autoCut = cut;
        reverse = new Preference(context).getReverse();
        __seed = seed;
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
                            Bitmap decoded = decode(bitmap);
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
                        Bitmap decoded = decode(bitmap);
                        holder.frame.setImageBitmap(decoded);
                    }
                });
    }

    public Bitmap decode(Bitmap input){
        if(__seed==0) return input;
        //decode image
        int[][] order = new int[25][2];
        for(int i=0; i<25; i++) {
            order[i][0] = i;
            order[i][1] = _random(i);
        }
        java.util.Arrays.sort(order, new java.util.Comparator<int[]>() {
            public int compare(int[] a, int[] b) {
                return Double.compare(a[1], b[1]);
            }
        });
        //create new bitmap
        Bitmap output = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        int row_w = input.getWidth()/5;
        int row_h = input.getHeight()/5;
        for(int i=0; i<25; i++){
            int[]o = order[i];
            int ox = i%5;
            int oy = i/5;
            int tx = o[0]%5;
            int ty = o[0]/5;
            Bitmap cropped = Bitmap.createBitmap(input,ox*row_w,oy*row_h,row_w,row_h);
            canvas.drawBitmap(cropped,tx*row_w,ty*row_h,null);
        }
        return output;
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
        ViewHolder(View itemView) {
            super(itemView);
            frame = itemView.findViewById(R.id.frame);
            itemView.setOnClickListener(this);
        }
        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // allows clicks events to be caught
    public void setClickListener(StripAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    public int _random(int index){
        double x = Math.sin(__seed+index) * 10000;
        return (int) Math.floor((x - Math.floor(x)) * 100000);
    }
}


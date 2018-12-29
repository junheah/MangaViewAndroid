package ml.melun.mangaview.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SizeReadyCallback;

import java.util.ArrayList;

import ml.melun.mangaview.R;
import ml.melun.mangaview.transformation.MangaCrop;
import ml.melun.mangaview.transformation.MangaCrop2;

public class StripAdapter extends RecyclerView.Adapter<StripAdapter.ViewHolder> {

    private ArrayList<String> imgs;
    private LayoutInflater mInflater;
    private Context mainContext;
    private StripAdapter.ItemClickListener mClickListener;
    Boolean autoCut = false;

    // data is passed into the constructor
    public StripAdapter(Context context, ArrayList<String> data, Boolean cut) {
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.imgs = data;
        autoCut = cut;
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
    public void onBindViewHolder(final ViewHolder holder, int pos) {
        if(autoCut) {
            int position = pos / 2;
            int type = pos % 2;
            String image = imgs.get(position);
            //set image to holder view
            Glide.with(mainContext)
                    .load(image)
                    .apply(new RequestOptions().bitmapTransform(new MangaCrop(holder.frame.getContext(), type)).placeholder(R.drawable.placeholder))
                    .into(holder.frame);
        }
        else Glide.with(mainContext)
                .load(imgs.get(pos))
                .apply(new RequestOptions().placeholder(R.drawable.placeholder))
                .into(holder.frame);
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

}


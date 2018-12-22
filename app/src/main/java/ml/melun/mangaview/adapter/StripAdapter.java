package ml.melun.mangaview.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

import ml.melun.mangaview.R;

public class StripAdapter extends RecyclerView.Adapter<StripAdapter.ViewHolder> {

    private ArrayList<String> imgs;
    private LayoutInflater mInflater;
    private Context mainContext;
    private StripAdapter.ItemClickListener mClickListener;

    // data is passed into the constructor
    public StripAdapter(Context context, ArrayList<String> data) {
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.imgs = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_strip, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String image = imgs.get(position);
        //set image to holder view
        Glide.with(mainContext)
                .load(image)
                .apply(new RequestOptions().dontTransform().placeholder(R.drawable.placeholder))
                .into(holder.frame);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return imgs.size();
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


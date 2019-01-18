package ml.melun.mangaview.adapter;
import android.content.Context;
import android.graphics.Color;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Title;

public class TitleAdapter extends RecyclerView.Adapter<TitleAdapter.ViewHolder> {

    private ArrayList<Title> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context mainContext;
    Boolean dark = false;
    Boolean save;



    // data is passed into the constructor
    public TitleAdapter(Context context) {
        dark = new Preference(context).getDarkTheme();
        save = new Preference(context).getDataSave();
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.mData = new ArrayList<>();
    }
    public void removeAll(){
        int originSize = mData.size();
        mData.clear();
        notifyItemRangeRemoved(0,originSize);
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_title, parent, false);
        return new ViewHolder(view);
    }
    public void addData(List<Title> t){
        int oSize = mData.size();
        mData.addAll(t);
        notifyItemRangeInserted(oSize,t.size());
    }
    public void moveItemToTop(int from){
        mData.add(0, mData.get(from));
        mData.remove(from+1);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String title = mData.get(position).getName();
        String thumb = mData.get(position).getThumb();
        String author = mData.get(position).getAuthor();
        String tags ="";
        for(String s:mData.get(position).getTags()){
            tags+=s+" ";
        }
        holder.name.setText(title);
        holder.author.setText(author);
        holder.tags.setText(tags);
        if(thumb.length()>1 && !save)Glide.with(mainContext).load(thumb).into(holder.thumb);
        else holder.thumb.setImageBitmap(null);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    class ViewHolder extends RecyclerView.ViewHolder{
        TextView name;
        ImageView thumb;
        TextView author;
        TextView tags;
        CardView card;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.Title);
            thumb = itemView.findViewById(R.id.Thumb);
            author =itemView.findViewById(R.id.TitleAuthor);
            tags = itemView.findViewById(R.id.TitleTag);
            card = itemView.findViewById(R.id.titleCard);
            if(dark){
                card.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
            }
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onItemClick(getAdapterPosition());
                }
            });


        }
    }

    // convenience method for getting data at click position
    public Title getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position);
    }
}
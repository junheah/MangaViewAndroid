package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Title;

public class SelectEpisodeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private JSONArray data;
    private LayoutInflater mInflater;
    private Context mainContext;
    Boolean favorite = false;
    TypedValue outValue;
    Boolean[] selected;
    ItemClickListener mClickListener;
    Boolean dark;

    // data is passed into the constructor
    public SelectEpisodeAdapter(Context context, JSONArray list) {
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.data = list;
        outValue = new TypedValue();
        selected = new Boolean[list.length()];
        Arrays.fill(selected,Boolean.FALSE);
        dark = new Preference(context).getDarkTheme();
        mainContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
    }

    // inflates the row layout from xml when needed
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_episode, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder h = (ViewHolder) holder;
        try {
            JSONObject episode = new JSONObject(data.getString(position));
            h.episode.setText(episode.getString("name"));
            h.date.setText(episode.getString("date"));
            if (selected[position]) {
                if(dark) h.itemView.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.selectedDark));
                else h.itemView.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.selected));
            } else {
                if(dark)h.itemView.setBackgroundResource(R.drawable.button_bg);
                else h.itemView.setBackgroundResource(R.drawable.item_bg);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return data.length();
    }

    public void select(int position){
        if(selected[position]) selected[position] = false;
        else selected[position] = true;
        notifyItemChanged(position);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView episode, date;
        ViewHolder(View itemView) {
            super(itemView);
            episode = itemView.findViewById(R.id.episode);
            date = itemView.findViewById(R.id.date);
            if(dark) episode.setTextColor(Color.WHITE);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onItemClick(v, getAdapterPosition());
                }
            });
        }
    }
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }
    public JSONArray getSelected(){
        JSONArray tmp= new JSONArray();
        for(int i=0; i<selected.length; i++){
            try {
                if (selected[i]) tmp.put(new JSONObject(data.getString(i)));
            }catch (Exception e){

            }
        }
        return tmp;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}

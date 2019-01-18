package ml.melun.mangaview.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Title;

public class OfflineTitleApapter extends RecyclerView.Adapter<OfflineTitleApapter.ViewHolder> {


    private ArrayList<String> titles;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context mainContext;
    Boolean dark;


    public OfflineTitleApapter(Context context, ArrayList<String> data) {
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.titles = data;
        dark = new Preference(context).getDarkTheme();
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_offline_title, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.name.setText(titles.get(position));
    }
    @Override
    public int getItemCount() {
        return titles.size();
    }

    public String getItem(int position){
        return titles.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView name;
        CardView card;
        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.off_title);
            card = itemView.findViewById(R.id.offlineTitleCard);
            if(dark){
                card.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
            }
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null){
                mClickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }
    public interface ItemClickListener{
        void onItemClick(View v, int position);
    }

}

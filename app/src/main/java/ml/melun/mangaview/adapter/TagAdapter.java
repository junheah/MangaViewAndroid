package ml.melun.mangaview.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;

public class TagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    Context mcontext;
    List<String> tags;
    LayoutInflater mInflater;
    private tagOnclick mClickListener;
    Boolean dark;

    public TagAdapter(Context m, List<String> t) {
        mcontext = m;
        tags = t;
        this.mInflater = LayoutInflater.from(m);
        dark = new Preference(m).getDarkTheme();
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_tag, parent, false);
        return new tagHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        tagHolder h = (tagHolder) holder;
        h.tag.setText(tags.get(position));
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public void setClickListener(tagOnclick t){
        this.mClickListener = t;
    }

    class tagHolder extends RecyclerView.ViewHolder{
        TextView tag;
        CardView card;
        public tagHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.tagCard);
            tag = itemView.findViewById(R.id.tag);
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onClick(tags.get(getAdapterPosition()));
                }
            });
        }
    }
    public interface tagOnclick{
        void onClick(String tag);
    }
}



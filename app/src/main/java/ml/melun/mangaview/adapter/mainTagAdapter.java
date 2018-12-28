package ml.melun.mangaview.adapter;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ml.melun.mangaview.R;

public class mainTagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    Context mcontext;
    List<String> tags;
    LayoutInflater mInflater;
    private tagOnclick mClickListener;
    int type;

    public mainTagAdapter(Context m, List<String> t , int type) {
        mcontext = m;
        tags = t;
        this.type = type;
        this.mInflater = LayoutInflater.from(m);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = null;
        switch (type){
            case 0:
                view = mInflater.inflate(R.layout.item_main_tag, parent, false);
                break;
            case 1:
                view = mInflater.inflate(R.layout.item_main_name, parent, false);
                break;
            case 2:
                view = mInflater.inflate(R.layout.item_main_tag, parent, false);
                break;
        }
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
            switch (type){
                case 0:
                case 2:
                    card = itemView.findViewById(R.id.mainTagCard);
                    tag = itemView.findViewById(R.id.main_tag_text);
                    break;
                case 1:
                    card = itemView.findViewById(R.id.mainNameCard);
                    tag = itemView.findViewById(R.id.main_name_text);
                    break;
            }

            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onClick(getAdapterPosition(), tags.get(getAdapterPosition()));
                }
            });
        }
    }
    public interface tagOnclick{
        void onClick(int position, String value);
    }
}



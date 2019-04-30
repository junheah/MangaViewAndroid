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


import java.util.Arrays;
import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;

import static ml.melun.mangaview.MainApplication.p;

public class mainTagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    Context mcontext;
    List<String> tags;
    Boolean[] selected;
    LayoutInflater mInflater;
    private tagOnclick mClickListener;
    int type;
    Boolean dark;
    Boolean singleSelect = false;
    int selection = -1;

    public mainTagAdapter(Context m, List<String> t , int type) {
        mcontext = m;
        tags = t;
        this.type = type;
        this.mInflater = LayoutInflater.from(m);
        dark = p.getDarkTheme();
        selected = new Boolean[t.size()];
        Arrays.fill(selected,Boolean.FALSE);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    public void setSingleSelect(Boolean b){
        singleSelect = b;
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
    public void toggleSelect(int position){
        if(singleSelect){
            if(position == selection) selection = -1;
            else{
                if(selection>-1){
                    int tmp = selection;
                    selection = position;
                    notifyItemChanged(tmp);
                }else{
                    selection = position;
                }
                notifyItemChanged(position);
            }
        }


        if(selected[position]) selected[position] = false;
        else selected[position] = true;
        notifyItemChanged(position);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        tagHolder h = (tagHolder) holder;
        h.tag.setText(tags.get(position));
        if(singleSelect){
            if(selection==position){
                if (dark)
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.selectedDark));
                else
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.selected));
            }else{
                if (dark)
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.colorDarkBackground));
                else
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.colorBackground));
            }
        }else {
            if (selected[position]) {
                if (dark)
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.selectedDark));
                else
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.selected));
            } else {
                if (dark)
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.colorDarkBackground));
                else
                    h.card.setCardBackgroundColor(ContextCompat.getColor(mcontext, R.color.colorBackground));
            }
        }
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
    public String getSelectedValues(){
        String res = "";
        for(int i=0; i<tags.size(); i++){
            if(selected[i]){
                if(res.length()>0) res += ','+tags.get(i);
                else res = tags.get(i);
            }
        }
        return res;
    }
    public String getSelectedIndex(){
        String res = "";
        if(singleSelect) {
            if(selection>-1) return selection + "";
            else return "";
        }

        for(int i=0; i<tags.size(); i++){
            if(selected[i]){
                if(res.length()>0) res += ","+i;
                else res = ""+i;
            }
        }
        return res;
    }
    public interface tagOnclick{
        void onClick(int position, String value);
    }
}



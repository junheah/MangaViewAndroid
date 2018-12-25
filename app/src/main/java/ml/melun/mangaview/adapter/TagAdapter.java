package ml.melun.mangaview.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import ml.melun.mangaview.R;

public class TagAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    Context mcontext;
    List<String> tags;
    LayoutInflater mInflater;
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_tag, parent, false);
        return new tagHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        tagHolder h =(tagHolder) holder;
        h.tag.setText(tags.get(position));
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public TagAdapter(Context m, List<String> t) {
        mcontext = m;
        tags = t;
        this.mInflater = LayoutInflater.from(m);
    }

    class tagHolder extends RecyclerView.ViewHolder{
        TextView tag;
        public tagHolder(View itemView) {
            super(itemView);
            tag = itemView.findViewById(R.id.tag);
        }
    }
    public interface tagOnclick{
        void onClick(View view);
    }
}



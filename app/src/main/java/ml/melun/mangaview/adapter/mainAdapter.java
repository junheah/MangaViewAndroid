package ml.melun.mangaview.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ml.melun.mangaview.R;

public class mainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context mainContext;
    LayoutInflater mInflater;

    public mainAdapter(Context main) {
        super();
        mainContext = main;
        this.mInflater = LayoutInflater.from(main);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_tag, parent, false);
        return new recyclerHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    class recyclerHolder extends RecyclerView.ViewHolder{
        public recyclerHolder(View itemView) {
            super(itemView);
        }
    }

    public interface onItemClick{
        void clicked();
    }

    private class fetchMainPage extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
        }
    }
}

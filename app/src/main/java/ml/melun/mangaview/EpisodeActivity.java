package ml.melun.mangaview;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.EpisodeAdapter;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Search;
import ml.melun.mangaview.mangaview.Title;

public class EpisodeActivity extends AppCompatActivity {
    //global variables
    TextView titleText;
    ProgressDialog pd;
    Title title;
    EpisodeAdapter episodeAdapter;
    Context context = this;
    RecyclerView episodeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode);
        Intent intent = getIntent();
        title = new Title(intent.getStringExtra("title"),null);

        titleText = this.findViewById(R.id.Title);
        titleText.setText(title.getName());
        episodeList = this.findViewById(R.id.EpisodeList);
        episodeList.setLayoutManager(new LinearLayoutManager(this));
        getEpisodes g = new getEpisodes();
        g.execute();
    }

    private class getEpisodes extends AsyncTask<Void,Void,Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(EpisodeActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
        }

        protected Integer doInBackground(Void... params) {
            title.fetchEps();
            ArrayList<Manga> episodes = title.getEps();
            episodeAdapter = new EpisodeAdapter(context, episodes);
            return null;
        }

        @Override
        protected void onPostExecute(Integer res) {
            super.onPostExecute(res);
            episodeList.setAdapter(episodeAdapter);
            episodeAdapter.setClickListener(new EpisodeAdapter.ItemClickListener() {
                @Override
                public void onItemClick(View v, int position) {
                    // start intent : Episode viewer
                    Manga selected = episodeAdapter.getItem(position);
                    System.out.println(selected.getId());
//                    Intent episodeView = new Intent(context, EpisodeActivity.class);
//                    episodeView.putExtra("title", selected.getName());
//                    startActivity(episodeView);
                }
//
//                @Override
//                public void onItemLongClick(int position, View v) {
//                    Log.d(TAG, "onItemLongClick pos = " + position);
//                }
            });
            if (pd.isShowing()) {
                pd.dismiss();
            }
        }
    }
}

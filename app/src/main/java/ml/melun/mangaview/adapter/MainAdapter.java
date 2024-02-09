package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Login;
import ml.melun.mangaview.mangaview.MainPage;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.mangaview.MTitle.base_comic;

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    Context mainContext;
    LayoutInflater mInflater, itemInflater;
    MainUpdatedAdapter uadapter;
    onItemClick mainClickListener;
    boolean dark, loaded = false;

    List<Object> data;

    final static int TITLE = 0;
    final static int MANGA = 1;
    final static int TAG = 2;
    final static int HEADER = 3;
    final static int UPDATED = 4;

    ButtonHeader addh;
    Header besth, updh, hish, weekh;

    public MainAdapter(Context main) {
        super();
        mainContext = main;
        dark = p.getDarkTheme();
        this.mInflater = LayoutInflater.from(main);
        this.itemInflater = LayoutInflater.from(main);

        data = new ArrayList<>();

        uadapter = new MainUpdatedAdapter(main);
        addh = new ButtonHeader("최근 추가된 만화", () -> mainClickListener.clickedMoreUpdated());

        data.add(addh);
        data.add(null);

        updh = new Header("북마크 업데이트");
        data.add(updh);

        hish = new Header("최근에 본 만화");
        data.add(hish);

        weekh = new Header("주간 베스트");
        data.add(weekh);

        besth = new Header("일본만화 베스트");
        data.add(besth);

        data.add(new Header("이름"));
        for(String s : mainContext.getResources().getStringArray(R.array.tag_name)){
            data.add(new NameTag(s));
        }
        data.add(new Header("장르"));
        for(String s : mainContext.getResources().getStringArray(R.array.tag_genre)){
            data.add(new GenreTag(s));
        }
        data.add(new Header("발행"));
        for(String s : mainContext.getResources().getStringArray(R.array.tag_release)){
            data.add(new ReleaseTag(s));
        }

        setHasStableIds(true);
        notifyDataSetChanged();
        uadapter.setLoad("URL 업데이트중...");
    }

    public void fetch(){
        //fetch main page data
        uadapter.setLoad();
        new MainFetcher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public long getItemId(int position) {
        if(data.get(position) == null) return -1;
        return data.get(position).hashCode();
    }


    @Override
    public int getItemViewType(int position) {
        Object o = data.get(position);
        if(o == null)
            return UPDATED;
        else if(o instanceof Title)
            return TITLE;
        else if(o instanceof Manga)
            return MANGA;
        else if(o instanceof Header)
            return HEADER;
        else if(o instanceof Tag)
            return TAG;
        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        switch (viewType){
            case TITLE:
                v = mInflater.inflate(R.layout.main_item_ranking,parent,false);
                return new TitleHolder(v);
            case MANGA:
                v = mInflater.inflate(R.layout.main_item_ranking,parent,false);
                return new MangaHolder(v);
            case HEADER:
                v = mInflater.inflate(R.layout.item_main_header,parent,false);
                return new HeaderHolder(v);
            case UPDATED:
                v = mInflater.inflate(R.layout.item_main_updated_list, parent, false);
                return new AddedHolder(v);
            case TAG:
                v = mInflater.inflate(R.layout.item_main_tag,parent,false);
                return new TagHolder(v);
        }
        return null;
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int t = getItemViewType(position);
        switch (t){
            case TITLE:
                ((TitleHolder)holder).setTitle((Title)data.get(position),0);
                break;
            case MANGA:
                ((MangaHolder) holder).setManga((Manga)data.get(position),0);
                break;
            case HEADER:
                ((HeaderHolder)holder).setHeader((Header)data.get(position));
                break;
            case UPDATED:
                break;
            case TAG:
                ((TagHolder) holder).tag.setText(data.get(position).toString());
                break;
        }

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class AddedHolder extends RecyclerView.ViewHolder{
        RecyclerView updatedList;
        public AddedHolder(View itemView) {
            super(itemView);
            updatedList = itemView.findViewById(R.id.main_tag);
            LinearLayoutManager lm = new NpaLinearLayoutManager(mainContext);
            lm.setOrientation(RecyclerView.HORIZONTAL);
            updatedList.setLayoutManager(lm);
            updatedList.setAdapter(uadapter);
            uadapter.setClickListener(new MainUpdatedAdapter.OnClickCallback() {
                @Override
                public void onclick(Manga m) {
                    mainClickListener.clickedManga(m);
                }

                @Override
                public void refresh() {
                    fetch();
                    mainClickListener.clickedRetry();
                }
            });
        }
    }
    class MangaHolder extends RecyclerView.ViewHolder{

        TextView text;
        CardView card;
        TextView rank;
        View rankLayout;

        public MangaHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.header_title);
            card = itemView.findViewById(R.id.ranking_card);
            rank = itemView.findViewById(R.id.ranking_rank);
            rankLayout = itemView.findViewById(R.id.ranking_rank_layout);
            if(card!=null){
                if(dark) {
                    card.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
                }
            }
        }

        public void setManga(Manga m, int r){
            text.setText(m.getName());
            card.setOnClickListener(v -> {
                if(m!=null && m.getId()>0)
                    mainClickListener.clickedManga(m);
            });

            if(m instanceof MainPage.RankingManga){
                rankLayout.setVisibility(View.VISIBLE);
                rank.setText(String.valueOf(((MainPage.RankingManga)m).getRanking()));
            }else{
                rankLayout.setVisibility(View.GONE);
            }
        }
    }
    class HeaderHolder extends RecyclerView.ViewHolder{
        TextView text;
        ImageView button;
        View container;

        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.header_title);
            button = itemView.findViewById(R.id.header_button);
            container = itemView.findViewById(R.id.header_container);
            if(dark)
                this.button.setColorFilter(Color.WHITE);
            else
                this.button.setColorFilter(Color.DKGRAY);
        }

        public void setHeader(Header h){
            this.text.setText(h.header);
            if(h instanceof ButtonHeader){
                this.container.setClickable(true);
                this.button.setVisibility(View.VISIBLE);
                this.container.setOnClickListener(view -> ((ButtonHeader)h).callback());
            }else{
                this.container.setClickable(false);
                this.button.setVisibility(View.GONE);
            }
        }
    }
    class TitleHolder extends RecyclerView.ViewHolder{
        TextView text;
        CardView card;
        TextView rank;
        View rankLayout;

        TitleHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.header_title);
            card = itemView.findViewById(R.id.ranking_card);
            rank = itemView.findViewById(R.id.ranking_rank);
            rankLayout = itemView.findViewById(R.id.ranking_rank_layout);
            if(card!=null){
                if(dark) {
                    card.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
                }
            }
        }
        public void setTitle(Title t, int r){
            text.setText(t.getName());
            card.setOnClickListener(v -> {
                if(t!=null && t.getId()>0)
                    mainClickListener.clickedTitle(t);
            });

            if(t instanceof MainPage.RankingTitle){
                rankLayout.setVisibility(View.VISIBLE);
                rank.setText(String.valueOf(((MainPage.RankingTitle)t).getRanking()));
            }else{
                rankLayout.setVisibility(View.GONE);
            }
        }
    }
    class TagHolder extends RecyclerView.ViewHolder{
        TextView tag;
        CardView card;
        public TagHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.mainTagCard);
            tag = itemView.findViewById(R.id.main_tag_text);

            if (dark)
                card.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
            else
                card.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorBackground));

            card.setOnClickListener(v -> {
                Tag t = (Tag) data.get(getAdapterPosition());
                if(t instanceof NameTag) mainClickListener.clickedName(t.tag);
                else if(t instanceof GenreTag) mainClickListener.clickedGenre(t.tag);
                else if(t instanceof ReleaseTag) mainClickListener.clickedRelease(t.tag);
            });
        }
    }

    static class Tag{
        public String tag;
        public Tag(String tag){this.tag=tag;}
        @NonNull
        @Override
        public String toString() {
            return tag;
        }
    }
    class NameTag extends Tag{
        public NameTag(String tag) {
            super(tag);
        }
    }
    class GenreTag extends Tag{
        public GenreTag(String tag) {
            super(tag);
        }
    }
    class ReleaseTag extends Tag{
        public ReleaseTag(String tag) {
            super(tag);
        }
    }
    static class Header{
        public String header;

        public Header(String header) {
            this.header = header;
        }
    }
    class ButtonHeader extends Header{
        public String header;
        Runnable callback;

        public ButtonHeader(String header, Runnable callback) {
            super(header);
            this.header = header;
            this.callback = callback;
        }

        public void callback() {
            callback.run();
        }
    }
    static class NoResultManga extends Manga{
        public NoResultManga() {
            super(-1, "결과 없음", "", base_comic);
        }
    }

    public void setMainClickListener(onItemClick main) {
        this.mainClickListener = main;
    }

    public interface onItemClick{
        void clickedManga(Manga m);
        void clickedGenre(String t);
        void clickedName(String t);
        void clickedRelease(String t);
        void clickedTitle(Title t);
        void clickedMoreUpdated();
        void captchaCallback();
        void clickedSearch(String query);
        void clickedRetry();
    }

    private class MainFetcher extends AsyncTask<Void, Integer, MainPage> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected MainPage doInBackground(Void... params) {
            Map<String,String> cookie = new HashMap<>();
            Login login = p.getLogin();
            if(login!=null && login.isValid()){
                p.getLogin().buildCookie(cookie);
            }
            return new MainPage(httpClient);
        }

        @Override
        protected void onPostExecute(MainPage u) {
            super.onPostExecute(u);
            //update adapters?
            if(u.getRecent().size() == 0){
                // captcha?
                mainClickListener.captchaCallback();
            }
            uadapter.setData(u.getRecent());

            for(int i=data.size()-1; i>=0; i--){
                if(data.get(i) instanceof NoResultManga) {
                    data.remove(i);
                    notifyItemRemoved(i);
                }
            }

            int i = data.indexOf(weekh);
            if(i>-1) {
                if (u.getWeeklyRanking().size() == 0 && !(data.get(i+1) instanceof NoResultManga)){
                    data.add(++i, new NoResultManga());
                    notifyItemInserted(i);
                }
                else {
                    for (MainPage.RankingManga m : u.getWeeklyRanking()) {
                        data.add(++i, m);
                        notifyItemInserted(i);
                    }
                }
            }

            i = data.indexOf(besth);
            if(i>-1) {
                if (u.getRanking().size() == 0){
                    data.add(++i, new NoResultManga());
                    notifyItemInserted(i);
                }
                else {
                    for (MainPage.RankingTitle t : u.getRanking()) {
                        data.add(++i, t);
                        notifyItemInserted(i);
                    }
                }
            }

            i = data.indexOf(hish);
            if(i>-1) {
                if (u.getOnlineRecent().size() == 0){
                    data.add(++i, new NoResultManga());
                    notifyItemInserted(i);
                }
                else {
                    for (Manga m : u.getOnlineRecent()) {
                        data.add(++i, m);
                        notifyItemInserted(i);
                    }
                }
            }

            i = data.indexOf(updh);
            if(i>-1){
                if(u.getFavUpdate().size() == 0){
                    data.add(++i, new NoResultManga());
                    notifyItemInserted(i);
                } else{
                    for(Manga m : u.getFavUpdate()){
                        data.add(++i, m);
                        notifyItemInserted(i);
                    }
                }
            }
        }
    }

}



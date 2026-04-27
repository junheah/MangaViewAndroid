package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.MainPageWebtoon;
import ml.melun.mangaview.mangaview.Ranking;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.ui.NpaLinearLayoutManager;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

public class MainWebtoonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int GROUP = 23;
    private static final int SECTION = 24;
    private static final int CATEGORY = 25;
    private static final String[] STATUS_FILTERS = {"연재", "완결"};
    private static final String[] GENRE_FILTERS = {"액션", "로맨스", "판타지", "무협", "드라마", "BL", "성인"};

    Context context;
    boolean dark;
    boolean save;
    LayoutInflater inflater;
    List<Ranking<?>> dataSet;
    List<Object> rows;
    MainAdapter.onItemClick listener;

    public MainWebtoonAdapter(Context context){
        this.context = context;
        this.dark = p.getDarkTheme();
        this.save = p.getDataSave();
        inflater = LayoutInflater.from(context);
        dataSet = MainPageWebtoon.getBlankDataSet();
        rows = buildRows(dataSet, true);
        setHasStableIds(false);
    }

    public void fetch(){
        new Fetcher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setLoading(){
        dataSet = MainPageWebtoon.getBlankDataSet();
        rows = buildRows(dataSet, true);
        notifyDataSetChanged();
    }

    public void setListener(MainAdapter.onItemClick listener){
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == CATEGORY)
            return new CategoryHolder(inflater.inflate(R.layout.item_webtoon_category_panel, parent, false));
        if(viewType == GROUP)
            return new GroupHolder(inflater.inflate(R.layout.item_webtoon_group_header, parent, false));
        return new SectionHolder(inflater.inflate(R.layout.item_webtoon_section, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = rows.get(position);
        if(holder instanceof CategoryHolder)
            ((CategoryHolder) holder).bind();
        else if(holder instanceof GroupHolder)
            ((GroupHolder) holder).bind((String) row);
        else
            ((SectionHolder) holder).bind((Ranking<?>) row);
    }

    @Override
    public int getItemViewType(int position) {
        if(rows.get(position) instanceof CategoryPanel)
            return CATEGORY;
        return rows.get(position) instanceof String ? GROUP : SECTION;
    }

    @Override
    public int getItemCount() {
        return rows == null ? 0 : rows.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private List<Object> buildRows(List<Ranking<?>> sections, boolean includeEmpty) {
        List<Object> result = new ArrayList<>();
        String lastGroup = "";
        result.add(new CategoryPanel());
        if(sections == null)
            return result;
        for(Ranking<?> section : sections) {
            if(section == null)
                continue;
            if(!includeEmpty && section.size() == 0)
                continue;
            SectionName name = parseSectionName(section.getName());
            if(!name.group.equals(lastGroup)) {
                result.add(name.group);
                lastGroup = name.group;
            }
            result.add(section);
        }
        return result;
    }

    private SectionName parseSectionName(String raw) {
        if(raw == null)
            return new SectionName("웹툰", "");
        int divider = raw.indexOf('|');
        if(divider < 0)
            return new SectionName("웹툰", raw);
        return new SectionName(raw.substring(0, divider), raw.substring(divider + 1));
    }

    static class SectionName {
        String group;
        String title;
        SectionName(String group, String title) {
            this.group = group;
            this.title = title;
        }
    }

    static class CategoryPanel {
    }

    class CategoryHolder extends RecyclerView.ViewHolder {
        ViewGroup statusFilters;
        ViewGroup genreFilters;

        CategoryHolder(View itemView) {
            super(itemView);
            statusFilters = itemView.findViewById(R.id.webtoon_status_filters);
            genreFilters = itemView.findViewById(R.id.webtoon_genre_filters);
        }

        void bind() {
            bindFilterRow(statusFilters, STATUS_FILTERS, label -> {
                if(listener != null)
                    listener.clickedRelease(label);
            });
            bindFilterRow(genreFilters, GENRE_FILTERS, label -> {
                if(listener != null)
                    listener.clickedGenre(label);
            });
        }

        void bindFilterRow(ViewGroup container, String[] labels, FilterClick click) {
            container.removeAllViews();
            for(String label : labels)
                container.addView(createFilterChip(label, click));
        }

        TextView createFilterChip(String label, FilterClick click) {
            TextView chip = new TextView(context);
            chip.setText(label);
            chip.setSingleLine(true);
            chip.setGravity(Gravity.CENTER);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setTextSize(13);
            chip.setPadding(dp(12), 0, dp(12), 0);
            chip.setBackground(filterBackground());
            chip.setOnClickListener(v -> click.onClick(label));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            params.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(params);
            return chip;
        }

        GradientDrawable filterBackground() {
            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(dp(6));
            background.setColor(ContextCompat.getColor(context, dark ? R.color.colorDarkBackground : R.color.colorBackground));
            background.setStroke(dp(1), ContextCompat.getColor(context, dark ? R.color.colorDarkWindowBackground : R.color.colorPrimaryDark));
            return background;
        }
    }

    interface FilterClick {
        void onClick(String label);
    }

    int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    class GroupHolder extends RecyclerView.ViewHolder {
        TextView title;

        GroupHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.webtoon_group_title);
        }

        void bind(String group) {
            title.setText(group);
        }
    }

    class SectionHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView count;
        RecyclerView list;

        SectionHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.webtoon_section_title);
            count = itemView.findViewById(R.id.webtoon_section_count);
            list = itemView.findViewById(R.id.webtoon_section_list);
            LinearLayoutManager manager = new NpaLinearLayoutManager(context);
            manager.setOrientation(RecyclerView.HORIZONTAL);
            list.setLayoutManager(manager);
            list.setNestedScrollingEnabled(false);
        }

        void bind(Ranking<?> section) {
            SectionName name = parseSectionName(section.getName());
            title.setText(name.title);
            count.setText(section.size() > 0 ? section.size() + "개" : "");
            list.setAdapter(new WebtoonCardAdapter(section));
        }
    }

    class WebtoonCardAdapter extends RecyclerView.Adapter<WebtoonCardAdapter.CardHolder> {
        List<?> items;

        WebtoonCardAdapter(List<?> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public CardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new CardHolder(inflater.inflate(R.layout.item_webtoon_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull CardHolder holder, int position) {
            Object item = items.get(position);
            if(!(item instanceof Title))
                return;

            Title title = (Title) item;
            holder.name.setText(title.getName());
            String meta = title.getRelease();
            if((meta == null || meta.length() == 0) && title.getTags().size() > 0)
                meta = TextUtils.join(" / ", title.getTags());
            holder.meta.setText(meta == null ? "" : meta);

            Glide.with(holder.thumb).clear(holder.thumb);
            String thumb = title.getThumb();
            if(save || thumb == null || thumb.length() == 0)
                holder.thumb.setImageResource(R.mipmap.ic_launcher);
            else
                Glide.with(holder.thumb).load(thumb).placeholder(R.mipmap.ic_launcher).into(holder.thumb);

            holder.card.setOnClickListener(v -> {
                if(listener != null)
                    listener.clickedTitle(title);
            });
        }

        @Override
        public int getItemCount() {
            return items == null ? 0 : items.size();
        }

        class CardHolder extends RecyclerView.ViewHolder {
            CardView card;
            ImageView thumb;
            TextView name;
            TextView meta;

            CardHolder(View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.webtoon_card);
                thumb = itemView.findViewById(R.id.webtoon_thumb);
                name = itemView.findViewById(R.id.webtoon_name);
                meta = itemView.findViewById(R.id.webtoon_meta);
                if(dark)
                    card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorDarkBackground));
            }
        }
    }

    private class Fetcher extends AsyncTask<Void, Integer, MainPageWebtoon> {
        @Override
        protected MainPageWebtoon doInBackground(Void... params) {
            return new MainPageWebtoon(httpClient);
        }

        @Override
        protected void onPostExecute(MainPageWebtoon main) {
            super.onPostExecute(main);
            dataSet = main.getDataSet();
            if(dataSet == null)
                dataSet = MainPageWebtoon.getBlankDataSet();

            boolean hasAnyResult = false;
            for(Ranking<?> r : dataSet)
                if(r != null && r.size() > 0) {
                    hasAnyResult = true;
                    break;
                }

            if(!hasAnyResult) {
                if(listener != null)
                    listener.captchaCallback();
                return;
            }

            rows = buildRows(dataSet, false);
            notifyDataSetChanged();
        }
    }
}

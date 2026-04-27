package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.getGlideUrl;

import ml.melun.mangaview.R;
import ml.melun.mangaview.activity.ViewerActivity;
import ml.melun.mangaview.interfaces.StringCallback;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.model.PageItem;


public class StripAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final LayoutInflater mInflater;
    private final Context mainContext;
    private StripAdapter.ItemClickListener mClickListener;
    boolean autoCut;
    boolean reverse;
    int __seed;
    Decoder d;
    int width;
    int count = 0;
    final static int MaxStackSize = 2;
    private static final int PRELOAD_AHEAD_COUNT = 3;
    ViewerActivity.InfiniteScrollCallback callback;
    Title title;

    List<Object> items;

    public List<Object> getItems(){
        return items;
    }


    public static class InfoItem{
        public InfoItem(Manga prev, Manga next) {
            if(next == null)
                this.next = prev.nextEp();
            else
                this.next = next;
            if(prev == null)
                this.prev = next.prevEp();
            else
                this.prev = prev;
        }

        @Override
        public int hashCode() {
            return (next==null?1:next.getId()) * (prev==null?1:prev.getId());
        }

        public Manga next;
        public Manga prev;
        boolean nextRequested;
        boolean prevRequested;
    }

    @Override
    public long getItemId(int position) {
        Object o = items.get(position);
        return o.hashCode();
    }

    public void appendManga(Manga m){
        if(items == null)
            items = new ArrayList<>();
        int prevsize = items.size();
        if(items.size() == 0)
            items.add(new InfoItem(m.prevEp(), m));
        List<String> imgs = m.getImgs(mainContext);
        for(int i=0; i<imgs.size(); i++){
            items.add(new PageItem(i,imgs.get(i),m));
            if(autoCut)
                items.add(new PageItem(i,imgs.get(i),m,PageItem.SECOND));
        }
        items.add(new InfoItem(m, m.nextEp()));
        notifyItemRangeInserted(prevsize, items.size()-prevsize);
        count++;
        if(count>MaxStackSize){
            popFirst();
        }
    }

    public void insertManga(Manga m){
        if(items == null || items.size() == 0) {
            appendManga(m);
            return;
        }
        int prevsize = items.size();
        List<String> imgs = m.getImgs(mainContext);
        for(int i=imgs.size()-1; i>=0; i--){
            if(autoCut)
                items.add(0, new PageItem(i,imgs.get(i),m,PageItem.SECOND));
            items.add(0,new PageItem(i,imgs.get(i),m));
        }
        items.add(0, new InfoItem(null, m));

        notifyItemRangeInserted(0, items.size()-prevsize);
        count++;

        if(count>MaxStackSize){
            popLast();
        }
    }

    public void popFirst(){
        int size = 0;
        for(int i=1; i<items.size(); i++){
            if(items.get(i) instanceof InfoItem){
                size = i;
                break;
            }
        }
        if (size > 0) {
            items.subList(0, size).clear();
        }
        count--;
        notifyItemRangeRemoved(0,size);
    }

    public void popLast(){
        int originalSize = items.size();
        int rsize = -1;
        for(int i=originalSize-2; i>=0; i--){
            if(items.get(i) instanceof InfoItem){
                rsize = i;
                break;
            }
        }
        if (rsize >= 0 && originalSize > rsize + 1) {
            int removeStart = rsize + 1;
            int removeCount = originalSize - removeStart;
            items.subList(removeStart, originalSize).clear();
            count--;
            notifyItemRangeRemoved(removeStart, removeCount);
        }
    }

    // data is passed into the constructor
    public StripAdapter(Context context, Manga manga, Boolean cut, int width, Title title, ViewerActivity.InfiniteScrollCallback callback) {
        autoCut = cut;
        this.callback = callback;
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        reverse = p.getReverse();
        __seed = manga.getSeed();
        d = new Decoder(manga.getSeed(), manga.getId());
        this.width = width;
        this.title = title;
        setHasStableIds(true);
        appendManga(manga);
    }



    public void preloadAll(){
        for(Object o : items) {
            if(o instanceof PageItem) {
                Object url = getImageModel((PageItem) o);
                Glide.with(mainContext)
                        .load(url)
                        .preload();
            }
        }
    }

    final static int IMG = 0;
    final static int INFO = 1;

    @Override
    public int getItemViewType(int position) {
        if(items.get(position) instanceof PageItem)
            return IMG;
        else if(items.get(position) instanceof InfoItem)
            return INFO;
        else
            return -1;
    }

    public void removeAll(){
        int size = items.size();
        items.clear();
        notifyItemRangeRemoved(0, size);
    }

    @Override
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == IMG) {
            View view = mInflater.inflate(R.layout.item_strip, parent, false);
            return new ImgViewHolder(view);
        }else{
            //INFO
            View view = mInflater.inflate(R.layout.item_strip_info, parent, false);
            return new InfoViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int pos) {
        int type = getItemViewType(pos);
        if(type == IMG) {
            ((ImgViewHolder)holder).frame.setImageResource(R.drawable.placeholder);
            ((ImgViewHolder)holder).refresh.setVisibility(View.VISIBLE);
            glideBind((ImgViewHolder)holder, pos);
        }else if(type == INFO){
            //INFO
            ((InfoViewHolder) holder).loading.setVisibility(View.INVISIBLE);
            InfoItem info = (InfoItem)items.get(pos);
            Manga prev = info.prev;
            Manga next = info.next;

            if(prev == null){
                prev = next.prevEp();
            }else if(next == null){
                next = prev.nextEp();
            }

            ((InfoViewHolder) holder).prevInfo.setText(prev == null ? "첫 화" : prev.getName());
            ((InfoViewHolder) holder).nextInfo.setText(next == null ? "마지막 화" : next.getName());

            ViewerActivity.InfiniteLoadCallback r = new ViewerActivity.InfiniteLoadCallback() {
                @Override
                public void prevLoaded(Manga m) {
                    if(!isInfoHolderStillBound(holder, info))
                        return;
                    ((InfoViewHolder) holder).loading.setVisibility(View.INVISIBLE);
                    ((InfoViewHolder) holder).prevInfo.setText(m==null?"오류":m.getName());
                }

                @Override
                public void nextLoaded(Manga m) {
                    if(!isInfoHolderStillBound(holder, info))
                        return;
                    ((InfoViewHolder) holder).loading.setVisibility(View.INVISIBLE);
                    ((InfoViewHolder) holder).nextInfo.setText(m==null?"오류":m.getName());
                }
            };

            Manga m;
            if(pos == 0 && !info.prevRequested){
                info.prevRequested = true;
                ((InfoViewHolder) holder).loading.setVisibility(View.VISIBLE);
                m = callback.prevEp(r, next);
                info.prev = m;
                ((InfoViewHolder) holder).prevInfo.setText(m==null? "첫 화":"이전 화");
            }else if(pos == items.size()-1 && !info.nextRequested){
                info.nextRequested = true;
                ((InfoViewHolder) holder).loading.setVisibility(View.VISIBLE);
                m = callback.nextEp(r, prev);
                info.next = m;
                ((InfoViewHolder) holder).nextInfo.setText(m==null? "마지막 화":"다음 화");
            }
        }
    }



    void glideBind(ImgViewHolder holder, int pos){
        PageItem item = ((PageItem)items.get(pos));
        Object url = getImageModel(item);
        if (autoCut) {
            //set image to holder view
            Glide.with(holder.frame)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .load(url)
                    .placeholder(R.drawable.placeholder)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, Transition<? super Bitmap> transition) {
                            if(!isHolderStillBound(holder, item))
                                return;
                            bitmap = d.decode(bitmap, width);
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            if (width > height) {
                                if (item.side == PageItem.FIRST) {
                                    if (reverse)
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, width / 2, height));
                                    else
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, width / 2, 0, width / 2, height));
                                } else {
                                    if (reverse)
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, width / 2, 0, width / 2, height));
                                    else
                                        holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, width / 2, height));
                                }
                            } else {
                                if (item.side == PageItem.FIRST) {
                                    holder.frame.setImageBitmap(bitmap);
                                } else {
                                    holder.frame.setImageBitmap(Bitmap.createBitmap(bitmap.getWidth(), 1, Bitmap.Config.ARGB_8888));
                                }
                            }
                            holder.refresh.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            holder.frame.setImageDrawable(placeholder);
                            holder.refresh.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            holder.frame.setImageResource(R.drawable.placeholder);
                            holder.refresh.setVisibility(View.VISIBLE);
                        }
                    });
        } else {
            Glide.with(holder.frame)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .load(url)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            if(!isHolderStillBound(holder, item))
                                return;
                            resource = d.decode(resource, width);
                            holder.frame.setImageBitmap(resource);
                            holder.refresh.setVisibility(View.GONE);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            holder.frame.setImageDrawable(placeholder);
                            holder.refresh.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            holder.frame.setImageResource(R.drawable.placeholder);
                            holder.refresh.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    private Object getImageModel(PageItem item) {
        return item.manga.isOnline() ? getGlideUrl(item.img, item.manga.getBaseMode()) : item.img;
    }

    private boolean isHolderStillBound(ImgViewHolder holder, PageItem item) {
        int position = holder.getAdapterPosition();
        return position != RecyclerView.NO_POSITION
                && position < items.size()
                && items.get(position) == item;
    }

    private boolean isInfoHolderStillBound(RecyclerView.ViewHolder holder, InfoItem item) {
        int position = holder.getAdapterPosition();
        return position != RecyclerView.NO_POSITION
                && position < items.size()
                && items.get(position) == item;
    }

    private void preloadAhead(int adapterPosition) {
        int preloaded = 0;
        for(int i = adapterPosition + 1; i < items.size() && preloaded < PRELOAD_AHEAD_COUNT; i++) {
            Object next = items.get(i);
            if(next instanceof PageItem) {
                Glide.with(mainContext)
                        .load(getImageModel((PageItem) next))
                        .preload();
                preloaded++;
            }
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return items.size();
    }

    public PageItem getCurrentVisiblePage(){
        return current;
    }

    PageItem current;

    boolean needUpdate = true;

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        //handle bookmark
        int layoutPos = holder.getLayoutPosition();
        if(layoutPos == RecyclerView.NO_POSITION || layoutPos >= items.size())
            return;
        int type = getItemViewType(layoutPos);
        if(type == IMG) {
            PageItem pi = (PageItem) items.get(layoutPos);
            current = pi;
            preloadAhead(layoutPos);
            if(pi.manga.useBookmark()){
                int index = pi.index;
                if (index == 0) {
                    p.removeViewerBookmark(pi.manga);
                } else {
                    p.setViewerBookmark(pi.manga, index);
                }
            }
            p.setBookmark(title, pi.manga.getId());
            if(needUpdate){
                needUpdate = false;
                callback.updateInfo(pi.manga);
            }
        } else if(type == INFO){
            needUpdate = true;
        }
    }
//
//    @Override
//    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
//        //remove unnecessary items
//        int type = holder.getItemViewType();
//        if(type == INFO){
//            PosData d = getImgPos(holder.getLayoutPosition());
//            // last info pos
//            if(d.setPos == data.size()) return;
//            else if(d.setPos == currentPos.setPos)
//                popFirst();
//            else if(d.setPos > currentPos.setPos)
//                popLast();
//        }
//    }


    // stores and recycles views as they are scrolled off screen
    public class ImgViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ImageView frame;
        ImageButton refresh;
        ImgViewHolder(View itemView) {
            super(itemView);
            frame = itemView.findViewById(R.id.frame);
            refresh = itemView.findViewById(R.id.refreshButton);
            refresh.setOnClickListener(v -> {
                //refresh image
                int position = getAdapterPosition();
                if(position != RecyclerView.NO_POSITION)
                    notifyItemChanged(position);
            });
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }
        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick();
        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }

    public class InfoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView prevInfo, nextInfo;
        ProgressBar loading;
        InfoViewHolder(View itemView) {
            super(itemView);
            prevInfo = itemView.findViewById(R.id.prevEpInfo);
            nextInfo = itemView.findViewById(R.id.nextEpInfo);
            loading = itemView.findViewById(R.id.infoLoading);
            itemView.setOnClickListener(this);
        }
        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick();
        }
    }

    // allows clicks events to be caught
    public void setClickListener(StripAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick();
    }

}


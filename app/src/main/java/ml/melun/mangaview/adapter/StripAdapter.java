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
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.ArrayList;
import java.util.List;

import static ml.melun.mangaview.MainApplication.p;
import ml.melun.mangaview.R;
import ml.melun.mangaview.activity.ViewerActivity;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;
import ml.melun.mangaview.model.PageItem;


public class StripAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private LayoutInflater mInflater;
    private Context mainContext;
    private StripAdapter.ItemClickListener mClickListener;
    boolean autoCut = false;
    boolean reverse;
    int __seed;
    Decoder d;
    int width;
    int count = 0;
    final static int MaxStackSize = 2;
    ViewerActivity.InfiniteScrollCallback callback;
    Title title;

    List<Object> items;

    public List<Object> getItems(){
        return items;
    }


    public class InfoItem{
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
    };

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
        List<String> imgs = m.getImgs();
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
        List<String> imgs = m.getImgs();
        for(int i=imgs.size()-1; i>=0; i--){
            if(autoCut)
                items.add(new PageItem(0,imgs.get(i),m,PageItem.SECOND));
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
        for(int i=size-1; i>=0; i--)
            items.remove(i);
        count--;
        notifyItemRangeRemoved(0,size);
    }

    public void popLast(){
        int rsize = 0;
        items.remove(items.size()-1);
        for(int i=items.size()-2; i>=0; i--){
            if(items.get(i) instanceof InfoItem){
                rsize = i;
                break;
            }
        }
        for(int i=items.size()-1; i>rsize; i--)
            items.remove(i);
        count--;
        notifyItemRangeRemoved(rsize+1,items.size()-rsize);
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
                Glide.with(mainContext)
                        .load(((PageItem)o).img)
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
        items.clear();
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int pos) {
        int type = getItemViewType(pos);
        if(type == IMG) {
            ((ImgViewHolder)holder).frame.setImageResource(R.drawable.placeholder);
            ((ImgViewHolder)holder).refresh.setVisibility(View.VISIBLE);
            glideBind((ImgViewHolder)holder, pos);
        }else if(type == INFO){
            //INFO
            ((InfoViewHolder) holder).loading.setVisibility(View.INVISIBLE);
            Manga prev = ((InfoItem)items.get(pos)).prev;
            Manga next = ((InfoItem)items.get(pos)).next;

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
                    ((InfoViewHolder) holder).loading.setVisibility(View.INVISIBLE);
                    ((InfoViewHolder) holder).prevInfo.setText(m==null?"오류":m.getName());
                }

                @Override
                public void nextLoaded(Manga m) {
                    ((InfoViewHolder) holder).loading.setVisibility(View.INVISIBLE);
                    ((InfoViewHolder) holder).nextInfo.setText(m==null?"오류":m.getName());
                }
            };

            Manga m;
            if(pos == 0){
                ((InfoViewHolder) holder).loading.setVisibility(View.VISIBLE);
                m = callback.prevEp(r, next);
                ((InfoItem)items.get(pos)).prev = m;
                ((InfoViewHolder) holder).prevInfo.setText(m==null? "첫 화":"이전 화");
            }else if(pos == items.size()-1){
                ((InfoViewHolder) holder).loading.setVisibility(View.VISIBLE);
                m = callback.nextEp(r, prev);
                ((InfoItem)items.get(pos)).next = m;
                ((InfoViewHolder) holder).nextInfo.setText(m==null? "마지막 화":"다음 화");
            }
        }
    }



    void glideBind(ImgViewHolder holder, int pos){
        PageItem item = ((PageItem)items.get(pos));
        if (autoCut) {
            //set image to holder view
            Glide.with(mainContext)
                    .asBitmap()
                    .load(item.img)
                    .placeholder(R.drawable.placeholder)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
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
            Glide.with(mainContext)
                    .asBitmap()
                    .load(item.img)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
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
        int type = getItemViewType(layoutPos);
        if(type == IMG) {
            PageItem pi = (PageItem) items.get(layoutPos);
            current = pi;
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
    public class ImgViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView frame;
        ImageButton refresh;
        ImgViewHolder(View itemView) {
            super(itemView);
            frame = itemView.findViewById(R.id.frame);
            refresh = itemView.findViewById(R.id.refreshButton);
            refresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //refresh image
                    notifyItemChanged(getAdapterPosition());
                }
            });
            itemView.setOnClickListener(this);
        }
        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick();
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

    public interface AdapterInterface{
        Object getItemAt(int pos);
    }

}


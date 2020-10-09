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
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.ArrayList;
import java.util.List;

import static ml.melun.mangaview.MainApplication.p;
import ml.melun.mangaview.R;
import ml.melun.mangaview.activity.ViewerActivity;
import ml.melun.mangaview.mangaview.Decoder;
import ml.melun.mangaview.mangaview.Manga;


public class StripAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Manga>data;
    private LayoutInflater mInflater;
    private Context mainContext;
    private StripAdapter.ItemClickListener mClickListener;
    boolean autoCut = false;
    boolean reverse;
    int __seed;
    Decoder d;
    int width;
    int current = 0;
    ViewerActivity.InfiniteScrollCallback callback;

    // data is passed into the constructor
    public StripAdapter(Context context, Manga manga, Boolean cut, int seed, int id, int width, ViewerActivity.InfiniteScrollCallback callback) {
        this.data = new ArrayList<>();
        this.data.add(manga);
        this.callback = callback;
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        autoCut = cut;
        reverse = p.getReverse();
        __seed = seed;
        d = new Decoder(seed, id);
        this.width = width;
        setHasStableIds(true);
    }



    public void preloadAll(){
        for(String s : data.get(current).getImgs()) {
            Glide.with(mainContext)
                    .load(s)
                    .preload();
        }
    }

    final static int IMG = 0;
    final static int INFO = 1;

    @Override
    public int getItemViewType(int position) {
        if(position == 0)
            return INFO;
        int curpos = 0;
        for(Manga m : data){
            curpos += m.getImgs().size()+1;
            if(position == curpos) return INFO;
            if(position < curpos) return IMG;
        }
        return INFO;
    }

    public class PosData{
        public int setPos;
        public int imgPos;
        public PosData(int setPos, int imgPos){
          this.setPos = setPos;
          this.imgPos = imgPos;
        }
    };

    PosData getImgPos(int position){
        if(position == 0)
            return new PosData(0,0);
        position-=1;
        int i=0;
        while(i<data.size()){
            Manga m = data.get(i);
            if(position >= m.getImgs().size())
                position -= m.getImgs().size()+1;
            else
                return new PosData(i, position);
            i++;
        }
        return new PosData(i,0);
    }

    public PosData getCurrentPos(){
        return currentPos;
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void removeAll(){
        data.clear();
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
        PosData p = getImgPos(pos);
        if(getItemViewType(pos) == IMG) {
            ((ImgViewHolder)holder).frame.setImageResource(R.drawable.placeholder);
            ((ImgViewHolder)holder).refresh.setVisibility(View.VISIBLE);
            glideBind((ImgViewHolder)holder, p);
        }else{
            //INFO
            ((InfoViewHolder) holder).loading.setVisibility(View.INVISIBLE);
            //prev info
            if(p.setPos>1) {
                ((InfoViewHolder) holder).prevInfo.setText(data.get(p.setPos-1).getName());
            }else{
                ((InfoViewHolder) holder).prevInfo.setText("첫 화 입니다.");
            }

            //next info
            if(p.setPos<data.size()-1) {
                ((InfoViewHolder) holder).nextInfo.setText(data.get(p.setPos+1).getName());
            }else{
                ((InfoViewHolder) holder).nextInfo.setText("마지막 화 입니다.");
            }

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ((InfoViewHolder) holder).loading.setVisibility(View.INVISIBLE);
                }
            };
            if(pos == 0){
                //prev
                ((InfoViewHolder) holder).loading.setVisibility(View.VISIBLE);
                callback.prevEp(r);
            }else if(pos == getItemCount()-1){
                //next
                ((InfoViewHolder) holder).loading.setVisibility(View.VISIBLE);
                callback.nextEp(r);
            }

        }
    }



    void glideBind(ImgViewHolder holder, PosData pos){
        if (autoCut) {
            final int type = pos.imgPos % 2;
            String image = data.get(pos.setPos).getImgs().get(pos.imgPos / 2);
            //set image to holder view
            Glide.with(mainContext)
                    .asBitmap()
                    .load(image)
                    .placeholder(R.drawable.placeholder)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                            bitmap = d.decode(bitmap, width);
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            if (width > height) {
                                if (type == 0) {
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
                                if (type == 0) {
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
            String image = data.get(pos.setPos).getImgs().get(pos.imgPos);
            Glide.with(mainContext)
                    .asBitmap()
                    .load(image)
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
        if(data.size() == 0) return 0;
        return getImgCount() + data.size() + 1;
    }

    public int getImgCount(){
        int imgsSize = 0;
        for(Manga m : data){
            imgsSize+=m.getImgs().size();
        }
        if(autoCut) return imgsSize*2;
        else return imgsSize;
    }

    PosData currentPos;


    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        //handle bookmark
//        int layoutPos = holder.getLayoutPosition();
//        System.out.println("ppppppppppp "+layoutPos);
//        PosData pos = getImgPos(layoutPos);
//        currentPos = pos;
//        int type = getItemViewType(layoutPos);
//        if(pos.setPos < data.size()) {
//            if (data.get(pos.setPos).useBookmark()) {
//                if (autoCut) pos.imgPos /= 2;
//                if (pos.imgPos == 0 || type == INFO) {
//                    p.removeViewerBookmark(data.get(pos.setPos));
//                } else {
//                    p.setViewerBookmark(data.get(pos.setPos), pos.imgPos);
//                }
//            }
//        }
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

    public void popFirst(){
        int size = data.get(0).getImgs().size() +1;
        data.remove(0);
        notifyItemRangeRemoved(0,size);
    }
    public void popLast(){
        int positionStart = getItemCount();
        int size = data.get(data.size()-1).getImgs().size() +1;
        data.remove(data.size()-1);
        notifyItemRangeRemoved(positionStart,size);
    }




    public void appendImgs(Manga m){
        int originalSize = getItemCount();
        this.data.add(m);
        notifyItemRangeInserted(originalSize,m.getImgs().size()+2);
        if(data.size()>2)
            popFirst();
    }

    public void insertImgs(Manga m){
        this.data.add(0,m);
        notifyItemRangeInserted(0,m.getImgs().size()+1);
        if(data.size()>2)
            popLast();
    }

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

    public class InfoViewHolder extends RecyclerView.ViewHolder{
        TextView prevInfo, nextInfo;
        ProgressBar loading;
        InfoViewHolder(View itemView) {
            super(itemView);
            prevInfo = itemView.findViewById(R.id.prevEpInfo);
            nextInfo = itemView.findViewById(R.id.nextEpInfo);
            loading = itemView.findViewById(R.id.infoLoading);
        }

    }

    // allows clicks events to be caught
    public void setClickListener(StripAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick();
    }
}


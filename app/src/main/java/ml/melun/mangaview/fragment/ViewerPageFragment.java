package ml.melun.mangaview.fragment;

import static ml.melun.mangaview.Utils.getGlideUrl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.fragment.app.Fragment;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import ml.melun.mangaview.R;
import ml.melun.mangaview.interfaces.PageInterface;
import ml.melun.mangaview.mangaview.Decoder;

public class ViewerPageFragment extends Fragment {
    String image;
    Decoder decoder;
    Context context;
    PageInterface i;
    int width;

    public ViewerPageFragment(){

    }
    public ViewerPageFragment(String image, Decoder decoder, int width, Context context, PageInterface i){
        this.image = image;
        this.decoder = decoder;
        this.width = width;
        this.context = context;
        this.i = i;
    }
    public static Fragment create(String image, Decoder decoder, int width, Context context, PageInterface i){
        return new ViewerPageFragment(image, decoder, width, context, i);
    }

    public void updatePageFragment(Context context){
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_viewer, container, false);
        ImageView frame = rootView.findViewById(R.id.page);
        ImageButton refresh = rootView.findViewById(R.id.refreshButton);
        //glide
        frame.setImageResource(R.drawable.placeholder);
        refresh.setVisibility(View.VISIBLE);

        if(context != null)
            loadImage(frame, refresh);

        refresh.setOnClickListener(v -> {
            if(context != null) {
                loadImage(frame, refresh);
            }
        });
        rootView.setOnClickListener(v -> i.onPageClick());

        return rootView;
    }

    void loadImage(ImageView frame, ImageButton refresh){
        Object target = image.startsWith("http") ? getGlideUrl(image) : image;
        Glide.with(frame)
                .asBitmap()
                .load(target)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        refresh.setVisibility(View.GONE);
                        bitmap = decoder.decode(bitmap,width);
                        frame.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        //
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        if(image.length()>0) {
                            frame.setImageResource(R.drawable.placeholder);
                            refresh.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    public void setOnClick(PageInterface i){
        this.i = i;
    }
}

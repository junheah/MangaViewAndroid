package ml.melun.mangaview.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import ml.melun.mangaview.R;
import ml.melun.mangaview.interfaces.PageInterface;
import ml.melun.mangaview.mangaview.Decoder;

public class ViewerPageFragment extends Fragment {
    String image;
    String image1;
    Decoder decoder;
    Context context;
    PageInterface i;
    int width;
    Boolean error = false;
    Boolean useSecond = false;

    public ViewerPageFragment(){

    }
    public ViewerPageFragment(String image, String image1, Decoder decoder, int width, Context context, PageInterface i){
        this.image = image;
        this.image1 = image1;
        this.decoder = decoder;
        this.width = width;
        this.context = context;
        this.i = i;
    }
    public static Fragment create(String image, String image1, Decoder decoder, int width, Context context, PageInterface i){
        return new ViewerPageFragment(image, image1, decoder, width, context, i);
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

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(context != null) {
                    error = false;
                    loadImage(frame, refresh);
                }
            }
        });
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                i.onPageClick();
            }
        });

        return rootView;
    }

    void loadImage(ImageView frame, ImageButton refresh){
        String target = useSecond && image1 != null && image1.length()>0 ? image1 : image;
        Glide.with(context)
                .asBitmap()
                .load(error && !useSecond ? target.indexOf("img.") > -1 ? target.replace("img.","s3.") : target.replace("://", "://s3.") : target)
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
                            if (!error && !useSecond) {
                                error = true;
                                loadImage(frame, refresh);
                            }else if(error && !useSecond){
                                error = false;
                                useSecond = true;
                                loadImage(frame, refresh);
                            }else{
                                error = false;
                                useSecond = false;
                                loadImage(frame, refresh);
                            }
                        }
                    }
                });
    }

    public void setOnClick(PageInterface i){
        this.i = i;
    }
}

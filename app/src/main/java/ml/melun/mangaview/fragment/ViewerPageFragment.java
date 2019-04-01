package ml.melun.mangaview.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import ml.melun.mangaview.R;
import ml.melun.mangaview.interfaces.PageInterface;
import ml.melun.mangaview.mangaview.Decoder;

import static ml.melun.mangaview.Utils.getSample;

public class ViewerPageFragment extends Fragment {
    String image;
    Decoder decoder;
    Context context;
    PageInterface i;
    int width;
    public ViewerPageFragment(){
    }
    public Fragment init(String image, Decoder decoder, int width, Context context, PageInterface i){
        this.image = image;
        this.decoder = decoder;
        this.width = width;
        this.context = context;
        this.i = i;
        return this;
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
        loadImage(frame, refresh);

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadImage(frame, refresh);
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
        Glide.with(context)
                .asBitmap()
                .load(image)
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
                        frame.setImageResource(R.drawable.placeholder);
                        refresh.setVisibility(View.VISIBLE);
                        if(image.contains("img.")){
                            image = image.replace("img.","s3.");
                            loadImage(frame,refresh);
                        }
                    }
                });
    }

    public void setOnClick(PageInterface i){
        this.i = i;
    }
}

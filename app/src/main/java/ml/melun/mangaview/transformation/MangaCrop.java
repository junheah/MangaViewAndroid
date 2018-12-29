package ml.melun.mangaview.transformation;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

public class MangaCrop extends BitmapTransformation {

    Context mcontext;
    int type;
    public MangaCrop(Context context, int t) {
        super();
        mcontext = context;
        type = t;
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        Bitmap transformed = crop(toTransform, type);
        return transformed;
    }

    private Bitmap crop(Bitmap toTransform, int t) {
        int width = toTransform.getWidth();
        int height = toTransform.getHeight();
        if(width>height && t==0){
            return Bitmap.createBitmap(toTransform,width/2,0,width/2,height);
        }
        else if(t==1){
            if(width>height) return Bitmap.createBitmap(toTransform,0,0,width/2,height);
            else return Bitmap.createBitmap(width,1,Bitmap.Config.ARGB_8888);
        }
        else return toTransform;
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        //super.updateDiskCacheKey(messageDigest);
    }
}

package ml.melun.mangaview.transformation;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

public class MangaCrop2 extends BitmapTransformation {

    Context mcontext;

    public MangaCrop2(Context context) {
        super();
        mcontext = context;

    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        Bitmap transformed = crop(toTransform);
        return transformed;
    }

    private Bitmap crop(Bitmap toTransform) {
        int width = toTransform.getWidth();
        int height = toTransform.getHeight();
        if(width>height){
            return Bitmap.createBitmap(toTransform,0,0,width/2,height);
        }
        return Bitmap.createBitmap(null);
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        //super.updateDiskCacheKey(messageDigest);
    }
}
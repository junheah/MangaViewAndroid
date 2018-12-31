package ml.melun.mangaview.transformation;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

public class MangaCropRight extends BitmapTransformation {

    Context mcontext;

    public MangaCropRight(Context context) {
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
        return Bitmap.createBitmap(toTransform,width/2,0,width/2,height);
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        //super.updateDiskCacheKey(messageDigest);
    }
}
package ml.melun.mangaview;

import android.content.Context;
import android.util.AttributeSet;

public class StripLayoutManager extends NpaLinearLayoutManager {

    public StripLayoutManager(Context context) {
        super(context);
    }

    public StripLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public StripLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public int findFirstVisibleItemPosition() {
        int real = super.findFirstVisibleItemPosition();
        int imgs = getItemCount();
        if(real==0){
            return 0;
        }else if(real>0 && real < imgs){
            return real-1;
        }else{
            return imgs-1;
        }
    }

    @Override
    public int findLastVisibleItemPosition() {
        int real = super.findLastVisibleItemPosition();
        int imgs = getItemCount();
        if(real==0){
            return 0;
        }else if(real>0 && real < imgs){
            return real-1;
        }else{
            return imgs-1;
        }
    }

}

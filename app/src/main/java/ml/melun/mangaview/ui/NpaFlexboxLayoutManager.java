package ml.melun.mangaview.ui;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.flexbox.FlexboxLayoutManager;

public class NpaFlexboxLayoutManager extends FlexboxLayoutManager {
    public NpaFlexboxLayoutManager(Context context) {
        super(context);
    }

    public NpaFlexboxLayoutManager(Context context, int flexDirection) {
        super(context, flexDirection);
    }

    public NpaFlexboxLayoutManager(Context context, int flexDirection, int flexWrap) {
        super(context, flexDirection, flexWrap);
    }

    public NpaFlexboxLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }
}

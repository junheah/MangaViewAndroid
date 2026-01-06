package ml.melun.mangaview.ui;

import android.content.Context;
import android.util.AttributeSet;

import ml.melun.mangaview.adapter.CustomSpinnerAdapter;
import ml.melun.mangaview.mangaview.Manga;

public class CustomSpinner extends androidx.appcompat.widget.AppCompatSpinner {

    public CustomSpinner(Context context) {
        super(context);
    }

    public CustomSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
    }

    public void setSelection(Manga m) {
        CustomSpinnerAdapter adapter = (CustomSpinnerAdapter) getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (m.equals((Manga) adapter.getItem(i))) {
                setSelection(i, true);
            }
        }
    }

}

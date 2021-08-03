// https://gist.github.com/InsanityOnABun/95c0757f2f527cc50e39
package ml.melun.mangaview.ui;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import java.lang.reflect.Field;

public class MarqueeToolbar extends Toolbar {

    TextView title;

    public MarqueeToolbar(Context context) {
        super(context);
    }

    public MarqueeToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MarqueeToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTitle(CharSequence title) {
        if (!reflected) {
            reflected = reflectTitle();
        }
        super.setTitle(title);
        selectTitle();
    }

    @Override
    public void setTitle(int resId) {
        if (!reflected) {
            reflected = reflectTitle();
        }
        super.setTitle(resId);
        selectTitle();
    }

    boolean reflected = false;
    private boolean reflectTitle() {
        try {
            Field field = Toolbar.class.getDeclaredField("mTitleTextView");
            field.setAccessible(true);
            title = (TextView) field.get(this);
            title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            title.setMarqueeRepeatLimit(-1);
            return true;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void selectTitle() {
        if (title != null)
            title.setSelected(true);
    }
}

package android.support.p002v7.widget;

import android.content.res.Resources.Theme;
import android.widget.SpinnerAdapter;

/* renamed from: android.support.v7.widget.ThemedSpinnerAdapter */
public interface ThemedSpinnerAdapter extends SpinnerAdapter {
    Theme getDropDownViewTheme();

    void setDropDownViewTheme(Theme theme);
}

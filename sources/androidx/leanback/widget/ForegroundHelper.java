package androidx.leanback.widget;

import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.view.View;

final class ForegroundHelper {
    static void setForeground(View view, Drawable drawable) {
        if (VERSION.SDK_INT >= 23) {
            view.setForeground(drawable);
        }
    }
}

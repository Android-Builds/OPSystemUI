package android.support.p002v7.widget;

import android.os.Build.VERSION;
import android.view.View;

/* renamed from: android.support.v7.widget.TooltipCompat */
public class TooltipCompat {
    public static void setTooltipText(View view, CharSequence charSequence) {
        if (VERSION.SDK_INT >= 26) {
            view.setTooltipText(charSequence);
        } else {
            TooltipCompatHandler.setTooltipText(view, charSequence);
        }
    }
}

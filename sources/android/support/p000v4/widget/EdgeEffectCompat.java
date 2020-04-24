package android.support.p000v4.widget;

import android.os.Build.VERSION;
import android.widget.EdgeEffect;

/* renamed from: android.support.v4.widget.EdgeEffectCompat */
public final class EdgeEffectCompat {
    public static void onPull(EdgeEffect edgeEffect, float f, float f2) {
        if (VERSION.SDK_INT >= 21) {
            edgeEffect.onPull(f, f2);
        } else {
            edgeEffect.onPull(f);
        }
    }
}

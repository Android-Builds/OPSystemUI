package androidx.slice.widget;

import android.content.Context;
import android.net.Uri;
import android.os.Build.VERSION;

class SliceMetrics {
    /* access modifiers changed from: protected */
    public void logHidden() {
        throw null;
    }

    /* access modifiers changed from: protected */
    public void logTouch(int i, Uri uri) {
        throw null;
    }

    /* access modifiers changed from: protected */
    public void logVisible() {
        throw null;
    }

    SliceMetrics() {
    }

    public static SliceMetrics getInstance(Context context, Uri uri) {
        if (VERSION.SDK_INT >= 28) {
            return new SliceMetricsWrapper(context, uri);
        }
        return null;
    }
}

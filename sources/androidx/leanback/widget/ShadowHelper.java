package androidx.leanback.widget;

import android.os.Build.VERSION;

final class ShadowHelper {
    static boolean supportsDynamicShadow() {
        return VERSION.SDK_INT >= 21;
    }
}

package androidx.leanback.widget;

import android.os.Build.VERSION;

final class StaticShadowHelper {
    static boolean supportsShadow() {
        return VERSION.SDK_INT >= 21;
    }
}

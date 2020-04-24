package android.support.p000v4.view;

import android.os.Build.VERSION;
import android.util.Log;
import android.view.ViewConfiguration;
import java.lang.reflect.Method;

/* renamed from: android.support.v4.view.ViewConfigurationCompat */
public final class ViewConfigurationCompat {
    private static Method sGetScaledScrollFactorMethod;

    static {
        if (VERSION.SDK_INT == 25) {
            try {
                sGetScaledScrollFactorMethod = ViewConfiguration.class.getDeclaredMethod("getScaledScrollFactor", new Class[0]);
            } catch (Exception unused) {
                Log.i("ViewConfigCompat", "Could not find method getScaledScrollFactor() on ViewConfiguration");
            }
        }
    }

    public static int getScaledHoverSlop(ViewConfiguration viewConfiguration) {
        if (VERSION.SDK_INT >= 28) {
            return viewConfiguration.getScaledHoverSlop();
        }
        return viewConfiguration.getScaledTouchSlop() / 2;
    }
}

package android.support.p000v4.util;

import android.os.Build.VERSION;
import java.util.Arrays;
import java.util.Objects;

/* renamed from: android.support.v4.util.ObjectsCompat */
public class ObjectsCompat {
    public static boolean equals(Object obj, Object obj2) {
        if (VERSION.SDK_INT >= 19) {
            return Objects.equals(obj, obj2);
        }
        return obj == obj2 || (obj != null && obj.equals(obj2));
    }

    public static int hash(Object... objArr) {
        if (VERSION.SDK_INT >= 19) {
            return Objects.hash(objArr);
        }
        return Arrays.hashCode(objArr);
    }
}

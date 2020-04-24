package android.support.p000v4.util;

/* renamed from: android.support.v4.util.Preconditions */
public class Preconditions {
    public static <T> T checkNotNull(T t) {
        if (t != null) {
            return t;
        }
        throw new NullPointerException();
    }

    public static <T> T checkNotNull(T t, Object obj) {
        if (t != null) {
            return t;
        }
        throw new NullPointerException(String.valueOf(obj));
    }

    public static int checkArgumentNonnegative(int i) {
        if (i >= 0) {
            return i;
        }
        throw new IllegalArgumentException();
    }
}

package kotlin.jvm.internal;

public class TypeIntrinsics {
    private static <T extends Throwable> T sanitizeStackTrace(T t) {
        Intrinsics.sanitizeStackTrace(t, TypeIntrinsics.class.getName());
        return t;
    }

    public static ClassCastException throwCce(ClassCastException classCastException) {
        sanitizeStackTrace(classCastException);
        throw classCastException;
    }

    public static Iterable asMutableIterable(Object obj) {
        return castToIterable(obj);
    }

    public static Iterable castToIterable(Object obj) {
        try {
            return (Iterable) obj;
        } catch (ClassCastException e) {
            throwCce(e);
            throw null;
        }
    }
}

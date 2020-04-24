package kotlin.collections;

import java.util.List;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: Collections.kt */
class CollectionsKt__CollectionsKt extends CollectionsKt__CollectionsJVMKt {
    public static final <T> int getLastIndex(List<? extends T> list) {
        Intrinsics.checkParameterIsNotNull(list, "receiver$0");
        return list.size() - 1;
    }
}

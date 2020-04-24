package com.android.systemui;

import java.lang.ref.WeakReference;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;

/* compiled from: DumpController.kt */
final class DumpController$removeListener$$inlined$synchronized$lambda$1 extends Lambda implements Function1<WeakReference<Dumpable>, Boolean> {
    final /* synthetic */ Dumpable $listener$inlined;
    final /* synthetic */ DumpController this$0;

    DumpController$removeListener$$inlined$synchronized$lambda$1(DumpController dumpController, Dumpable dumpable) {
        this.this$0 = dumpController;
        this.$listener$inlined = dumpable;
        super(1);
    }

    public /* bridge */ /* synthetic */ Object invoke(Object obj) {
        return Boolean.valueOf(invoke((WeakReference) obj));
    }

    public final boolean invoke(WeakReference<Dumpable> weakReference) {
        Intrinsics.checkParameterIsNotNull(weakReference, "it");
        return Intrinsics.areEqual((Dumpable) weakReference.get(), this.$listener$inlined) || weakReference.get() == null;
    }
}

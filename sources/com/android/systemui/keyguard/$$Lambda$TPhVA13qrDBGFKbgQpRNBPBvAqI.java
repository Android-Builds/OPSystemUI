package com.android.systemui.keyguard;

import com.android.systemui.keyguard.WakefulnessLifecycle.Observer;
import java.util.function.Consumer;

/* renamed from: com.android.systemui.keyguard.-$$Lambda$TPhVA13qrDBGFKbgQpRNBPBvAqI reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$TPhVA13qrDBGFKbgQpRNBPBvAqI implements Consumer {
    public static final /* synthetic */ $$Lambda$TPhVA13qrDBGFKbgQpRNBPBvAqI INSTANCE = new $$Lambda$TPhVA13qrDBGFKbgQpRNBPBvAqI();

    private /* synthetic */ $$Lambda$TPhVA13qrDBGFKbgQpRNBPBvAqI() {
    }

    public final void accept(Object obj) {
        ((Observer) obj).onStartedWakingUp();
    }
}

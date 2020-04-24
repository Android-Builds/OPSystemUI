package com.android.systemui.util.leak;

import com.android.systemui.p007qs.QSHost;
import com.android.systemui.util.leak.GarbageMonitor.MemoryTile;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class GarbageMonitor_MemoryTile_Factory implements Factory<MemoryTile> {
    private final Provider<QSHost> hostProvider;

    public GarbageMonitor_MemoryTile_Factory(Provider<QSHost> provider) {
        this.hostProvider = provider;
    }

    public MemoryTile get() {
        return provideInstance(this.hostProvider);
    }

    public static MemoryTile provideInstance(Provider<QSHost> provider) {
        return new MemoryTile((QSHost) provider.get());
    }

    public static GarbageMonitor_MemoryTile_Factory create(Provider<QSHost> provider) {
        return new GarbageMonitor_MemoryTile_Factory(provider);
    }
}

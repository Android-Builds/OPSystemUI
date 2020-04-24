package com.android.systemui.p007qs.tiles;

import com.android.systemui.p007qs.QSHost;
import dagger.internal.Factory;
import javax.inject.Provider;

/* renamed from: com.android.systemui.qs.tiles.GameModeTile_Factory */
public final class GameModeTile_Factory implements Factory<GameModeTile> {
    private final Provider<QSHost> hostProvider;

    public GameModeTile_Factory(Provider<QSHost> provider) {
        this.hostProvider = provider;
    }

    public GameModeTile get() {
        return provideInstance(this.hostProvider);
    }

    public static GameModeTile provideInstance(Provider<QSHost> provider) {
        return new GameModeTile((QSHost) provider.get());
    }

    public static GameModeTile_Factory create(Provider<QSHost> provider) {
        return new GameModeTile_Factory(provider);
    }
}

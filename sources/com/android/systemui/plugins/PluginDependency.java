package com.android.systemui.plugins;

import com.android.systemui.plugins.annotations.ProvidesInterface;

@ProvidesInterface(version = 1)
public class PluginDependency {
    public static final int VERSION = 1;
    static DependencyProvider sProvider;

    static abstract class DependencyProvider {
        /* access modifiers changed from: 0000 */
        public abstract <T> T get(Plugin plugin, Class<T> cls);

        DependencyProvider() {
        }
    }

    public static <T> T get(Plugin plugin, Class<T> cls) {
        return sProvider.get(plugin, cls);
    }
}

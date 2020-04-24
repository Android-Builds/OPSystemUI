package com.android.systemui.statusbar;

import android.content.Context;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import dagger.internal.DoubleCheck;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class NotificationMediaManager_Factory implements Factory<NotificationMediaManager> {
    private final Provider<Context> contextProvider;
    private final Provider<MediaArtworkProcessor> mediaArtworkProcessorProvider;
    private final Provider<NotificationEntryManager> notificationEntryManagerProvider;
    private final Provider<ShadeController> shadeControllerProvider;
    private final Provider<StatusBarWindowController> statusBarWindowControllerProvider;

    public NotificationMediaManager_Factory(Provider<Context> provider, Provider<ShadeController> provider2, Provider<StatusBarWindowController> provider3, Provider<NotificationEntryManager> provider4, Provider<MediaArtworkProcessor> provider5) {
        this.contextProvider = provider;
        this.shadeControllerProvider = provider2;
        this.statusBarWindowControllerProvider = provider3;
        this.notificationEntryManagerProvider = provider4;
        this.mediaArtworkProcessorProvider = provider5;
    }

    public NotificationMediaManager get() {
        return provideInstance(this.contextProvider, this.shadeControllerProvider, this.statusBarWindowControllerProvider, this.notificationEntryManagerProvider, this.mediaArtworkProcessorProvider);
    }

    public static NotificationMediaManager provideInstance(Provider<Context> provider, Provider<ShadeController> provider2, Provider<StatusBarWindowController> provider3, Provider<NotificationEntryManager> provider4, Provider<MediaArtworkProcessor> provider5) {
        NotificationMediaManager notificationMediaManager = new NotificationMediaManager((Context) provider.get(), DoubleCheck.lazy(provider2), DoubleCheck.lazy(provider3), (NotificationEntryManager) provider4.get(), (MediaArtworkProcessor) provider5.get());
        return notificationMediaManager;
    }

    public static NotificationMediaManager_Factory create(Provider<Context> provider, Provider<ShadeController> provider2, Provider<StatusBarWindowController> provider3, Provider<NotificationEntryManager> provider4, Provider<MediaArtworkProcessor> provider5) {
        NotificationMediaManager_Factory notificationMediaManager_Factory = new NotificationMediaManager_Factory(provider, provider2, provider3, provider4, provider5);
        return notificationMediaManager_Factory;
    }
}

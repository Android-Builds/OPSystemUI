package com.android.systemui.statusbar;

import android.content.Context;
import android.os.Handler;
import com.android.systemui.bubbles.BubbleData;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.notification.DynamicPrivacyController;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.ShadeController;
import dagger.internal.DoubleCheck;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class NotificationViewHierarchyManager_Factory implements Factory<NotificationViewHierarchyManager> {
    private final Provider<BubbleData> bubbleDataProvider;
    private final Provider<Context> contextProvider;
    private final Provider<NotificationGroupManager> groupManagerProvider;
    private final Provider<Handler> mainHandlerProvider;
    private final Provider<NotificationEntryManager> notificationEntryManagerProvider;
    private final Provider<NotificationLockscreenUserManager> notificationLockscreenUserManagerProvider;
    private final Provider<DynamicPrivacyController> privacyControllerProvider;
    private final Provider<ShadeController> shadeControllerProvider;
    private final Provider<StatusBarStateController> statusBarStateControllerProvider;
    private final Provider<VisualStabilityManager> visualStabilityManagerProvider;

    public NotificationViewHierarchyManager_Factory(Provider<Context> provider, Provider<Handler> provider2, Provider<NotificationLockscreenUserManager> provider3, Provider<NotificationGroupManager> provider4, Provider<VisualStabilityManager> provider5, Provider<StatusBarStateController> provider6, Provider<NotificationEntryManager> provider7, Provider<ShadeController> provider8, Provider<BubbleData> provider9, Provider<DynamicPrivacyController> provider10) {
        this.contextProvider = provider;
        this.mainHandlerProvider = provider2;
        this.notificationLockscreenUserManagerProvider = provider3;
        this.groupManagerProvider = provider4;
        this.visualStabilityManagerProvider = provider5;
        this.statusBarStateControllerProvider = provider6;
        this.notificationEntryManagerProvider = provider7;
        this.shadeControllerProvider = provider8;
        this.bubbleDataProvider = provider9;
        this.privacyControllerProvider = provider10;
    }

    public NotificationViewHierarchyManager get() {
        return provideInstance(this.contextProvider, this.mainHandlerProvider, this.notificationLockscreenUserManagerProvider, this.groupManagerProvider, this.visualStabilityManagerProvider, this.statusBarStateControllerProvider, this.notificationEntryManagerProvider, this.shadeControllerProvider, this.bubbleDataProvider, this.privacyControllerProvider);
    }

    public static NotificationViewHierarchyManager provideInstance(Provider<Context> provider, Provider<Handler> provider2, Provider<NotificationLockscreenUserManager> provider3, Provider<NotificationGroupManager> provider4, Provider<VisualStabilityManager> provider5, Provider<StatusBarStateController> provider6, Provider<NotificationEntryManager> provider7, Provider<ShadeController> provider8, Provider<BubbleData> provider9, Provider<DynamicPrivacyController> provider10) {
        NotificationViewHierarchyManager notificationViewHierarchyManager = new NotificationViewHierarchyManager((Context) provider.get(), (Handler) provider2.get(), (NotificationLockscreenUserManager) provider3.get(), (NotificationGroupManager) provider4.get(), (VisualStabilityManager) provider5.get(), (StatusBarStateController) provider6.get(), (NotificationEntryManager) provider7.get(), DoubleCheck.lazy(provider8), (BubbleData) provider9.get(), (DynamicPrivacyController) provider10.get());
        return notificationViewHierarchyManager;
    }

    public static NotificationViewHierarchyManager_Factory create(Provider<Context> provider, Provider<Handler> provider2, Provider<NotificationLockscreenUserManager> provider3, Provider<NotificationGroupManager> provider4, Provider<VisualStabilityManager> provider5, Provider<StatusBarStateController> provider6, Provider<NotificationEntryManager> provider7, Provider<ShadeController> provider8, Provider<BubbleData> provider9, Provider<DynamicPrivacyController> provider10) {
        NotificationViewHierarchyManager_Factory notificationViewHierarchyManager_Factory = new NotificationViewHierarchyManager_Factory(provider, provider2, provider3, provider4, provider5, provider6, provider7, provider8, provider9, provider10);
        return notificationViewHierarchyManager_Factory;
    }
}

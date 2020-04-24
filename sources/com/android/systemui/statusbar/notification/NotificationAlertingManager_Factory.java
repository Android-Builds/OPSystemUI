package com.android.systemui.statusbar.notification;

import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.NotificationListener;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.phone.ShadeController;
import dagger.internal.DoubleCheck;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class NotificationAlertingManager_Factory implements Factory<NotificationAlertingManager> {
    private final Provider<AmbientPulseManager> ambientPulseManagerProvider;
    private final Provider<NotificationEntryManager> notificationEntryManagerProvider;
    private final Provider<NotificationInterruptionStateProvider> notificationInterruptionStateProvider;
    private final Provider<NotificationListener> notificationListenerProvider;
    private final Provider<NotificationRemoteInputManager> remoteInputManagerProvider;
    private final Provider<ShadeController> shadeControllerProvider;
    private final Provider<VisualStabilityManager> visualStabilityManagerProvider;

    public NotificationAlertingManager_Factory(Provider<NotificationEntryManager> provider, Provider<AmbientPulseManager> provider2, Provider<NotificationRemoteInputManager> provider3, Provider<VisualStabilityManager> provider4, Provider<ShadeController> provider5, Provider<NotificationInterruptionStateProvider> provider6, Provider<NotificationListener> provider7) {
        this.notificationEntryManagerProvider = provider;
        this.ambientPulseManagerProvider = provider2;
        this.remoteInputManagerProvider = provider3;
        this.visualStabilityManagerProvider = provider4;
        this.shadeControllerProvider = provider5;
        this.notificationInterruptionStateProvider = provider6;
        this.notificationListenerProvider = provider7;
    }

    public NotificationAlertingManager get() {
        return provideInstance(this.notificationEntryManagerProvider, this.ambientPulseManagerProvider, this.remoteInputManagerProvider, this.visualStabilityManagerProvider, this.shadeControllerProvider, this.notificationInterruptionStateProvider, this.notificationListenerProvider);
    }

    public static NotificationAlertingManager provideInstance(Provider<NotificationEntryManager> provider, Provider<AmbientPulseManager> provider2, Provider<NotificationRemoteInputManager> provider3, Provider<VisualStabilityManager> provider4, Provider<ShadeController> provider5, Provider<NotificationInterruptionStateProvider> provider6, Provider<NotificationListener> provider7) {
        NotificationAlertingManager notificationAlertingManager = new NotificationAlertingManager((NotificationEntryManager) provider.get(), (AmbientPulseManager) provider2.get(), (NotificationRemoteInputManager) provider3.get(), (VisualStabilityManager) provider4.get(), DoubleCheck.lazy(provider5), (NotificationInterruptionStateProvider) provider6.get(), (NotificationListener) provider7.get());
        return notificationAlertingManager;
    }

    public static NotificationAlertingManager_Factory create(Provider<NotificationEntryManager> provider, Provider<AmbientPulseManager> provider2, Provider<NotificationRemoteInputManager> provider3, Provider<VisualStabilityManager> provider4, Provider<ShadeController> provider5, Provider<NotificationInterruptionStateProvider> provider6, Provider<NotificationListener> provider7) {
        NotificationAlertingManager_Factory notificationAlertingManager_Factory = new NotificationAlertingManager_Factory(provider, provider2, provider3, provider4, provider5, provider6, provider7);
        return notificationAlertingManager_Factory;
    }
}

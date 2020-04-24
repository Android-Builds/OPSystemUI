package com.android.systemui.bubbles;

import android.content.Context;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ZenModeController;
import dagger.internal.Factory;
import javax.inject.Provider;

public final class BubbleController_Factory implements Factory<BubbleController> {
    private final Provider<ConfigurationController> configurationControllerProvider;
    private final Provider<Context> contextProvider;
    private final Provider<BubbleData> dataProvider;
    private final Provider<NotificationInterruptionStateProvider> interruptionStateProvider;
    private final Provider<StatusBarWindowController> statusBarWindowControllerProvider;
    private final Provider<ZenModeController> zenModeControllerProvider;

    public BubbleController_Factory(Provider<Context> provider, Provider<StatusBarWindowController> provider2, Provider<BubbleData> provider3, Provider<ConfigurationController> provider4, Provider<NotificationInterruptionStateProvider> provider5, Provider<ZenModeController> provider6) {
        this.contextProvider = provider;
        this.statusBarWindowControllerProvider = provider2;
        this.dataProvider = provider3;
        this.configurationControllerProvider = provider4;
        this.interruptionStateProvider = provider5;
        this.zenModeControllerProvider = provider6;
    }

    public BubbleController get() {
        return provideInstance(this.contextProvider, this.statusBarWindowControllerProvider, this.dataProvider, this.configurationControllerProvider, this.interruptionStateProvider, this.zenModeControllerProvider);
    }

    public static BubbleController provideInstance(Provider<Context> provider, Provider<StatusBarWindowController> provider2, Provider<BubbleData> provider3, Provider<ConfigurationController> provider4, Provider<NotificationInterruptionStateProvider> provider5, Provider<ZenModeController> provider6) {
        BubbleController bubbleController = new BubbleController((Context) provider.get(), (StatusBarWindowController) provider2.get(), (BubbleData) provider3.get(), (ConfigurationController) provider4.get(), (NotificationInterruptionStateProvider) provider5.get(), (ZenModeController) provider6.get());
        return bubbleController;
    }

    public static BubbleController_Factory create(Provider<Context> provider, Provider<StatusBarWindowController> provider2, Provider<BubbleData> provider3, Provider<ConfigurationController> provider4, Provider<NotificationInterruptionStateProvider> provider5, Provider<ZenModeController> provider6) {
        BubbleController_Factory bubbleController_Factory = new BubbleController_Factory(provider, provider2, provider3, provider4, provider5, provider6);
        return bubbleController_Factory;
    }
}

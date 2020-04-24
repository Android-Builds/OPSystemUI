package com.android.systemui;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import com.android.internal.colorextraction.ColorExtractor.GradientColors;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardStatusView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.DaggerSystemUIFactory_SystemUIRootComponent.Builder;
import com.android.systemui.Dependency.DependencyInjector;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.dock.DockManager;
import com.android.systemui.fragments.FragmentService.FragmentCreator;
import com.android.systemui.keyguard.DismissCallbackRegistry;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.power.EnhancedEstimates;
import com.android.systemui.power.EnhancedEstimatesImpl;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.NotificationListener;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider;
import com.android.systemui.statusbar.notification.collection.NotificationData.KeyguardEnvironment;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.KeyguardBottomAreaView;
import com.android.systemui.statusbar.phone.KeyguardBouncer;
import com.android.systemui.statusbar.phone.KeyguardBouncer.BouncerExpansionCallback;
import com.android.systemui.statusbar.phone.KeyguardEnvironmentImpl;
import com.android.systemui.statusbar.phone.LockIcon;
import com.android.systemui.statusbar.phone.LockscreenWallpaper;
import com.android.systemui.statusbar.phone.NotificationIconAreaController;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.ScrimState;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBar.StatusBarInjector;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.util.InjectionInflationController.ViewCreator;
import com.android.systemui.util.leak.GarbageMonitor;
import com.android.systemui.volume.VolumeDialogComponent;
import java.util.function.Consumer;

public class SystemUIFactory {
    static SystemUIFactory mFactory;
    private SystemUIRootComponent mRootComponent;

    protected static class ContextHolder {
        private Context mContext;

        public ContextHolder(Context context) {
            this.mContext = context;
        }

        public Context provideContext() {
            return this.mContext;
        }
    }

    public interface SystemUIRootComponent {
        DependencyInjector createDependency();

        FragmentCreator createFragmentCreator();

        GarbageMonitor createGarbageMonitor();

        ViewCreator createViewCreator();

        StatusBarInjector getStatusBarInjector();
    }

    public boolean provideAllowNotificationLongPress() {
        return true;
    }

    public DockManager provideDockManager(Context context) {
        return null;
    }

    public String provideLeakReportEmail() {
        return null;
    }

    public static <T extends SystemUIFactory> T getInstance() {
        return mFactory;
    }

    public static void createFromConfig(Context context) {
        String string = context.getString(R$string.config_systemUIFactoryComponent);
        if (string == null || string.length() == 0) {
            throw new RuntimeException("No SystemUIFactory component configured");
        }
        try {
            mFactory = (SystemUIFactory) context.getClassLoader().loadClass(string).newInstance();
            mFactory.init(context);
        } catch (Throwable th) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error creating SystemUIFactory component: ");
            sb.append(string);
            Log.w("SystemUIFactory", sb.toString(), th);
            throw new RuntimeException(th);
        }
    }

    /* access modifiers changed from: protected */
    public void init(Context context) {
        Builder builder = DaggerSystemUIFactory_SystemUIRootComponent.builder();
        builder.systemUIFactory(this);
        builder.dependencyProvider(new DependencyProvider());
        builder.contextHolder(new ContextHolder(context));
        this.mRootComponent = builder.build();
    }

    public SystemUIRootComponent getRootComponent() {
        return this.mRootComponent;
    }

    public StatusBarKeyguardViewManager createStatusBarKeyguardViewManager(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        return new StatusBarKeyguardViewManager(context, viewMediatorCallback, lockPatternUtils);
    }

    public KeyguardBouncer createKeyguardBouncer(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils, ViewGroup viewGroup, DismissCallbackRegistry dismissCallbackRegistry, BouncerExpansionCallback bouncerExpansionCallback) {
        KeyguardBouncer keyguardBouncer = new KeyguardBouncer(context, viewMediatorCallback, lockPatternUtils, viewGroup, dismissCallbackRegistry, FalsingManagerFactory.getInstance(context), bouncerExpansionCallback, KeyguardUpdateMonitor.getInstance(context), new Handler(Looper.getMainLooper()));
        return keyguardBouncer;
    }

    public ScrimController createScrimController(ScrimView scrimView, ScrimView scrimView2, LockscreenWallpaper lockscreenWallpaper, TriConsumer<ScrimState, Float, GradientColors> triConsumer, Consumer<Integer> consumer, DozeParameters dozeParameters, AlarmManager alarmManager) {
        ScrimController scrimController = new ScrimController(scrimView, scrimView2, triConsumer, consumer, dozeParameters, alarmManager);
        return scrimController;
    }

    public NotificationIconAreaController createNotificationIconAreaController(Context context, StatusBar statusBar, StatusBarStateController statusBarStateController) {
        return new NotificationIconAreaController(context, statusBar, statusBarStateController, (NotificationMediaManager) Dependency.get(NotificationMediaManager.class));
    }

    public KeyguardIndicationController createKeyguardIndicationController(Context context, ViewGroup viewGroup, LockIcon lockIcon, KeyguardStatusView keyguardStatusView, KeyguardBottomAreaView keyguardBottomAreaView) {
        KeyguardIndicationController keyguardIndicationController = new KeyguardIndicationController(context, viewGroup, lockIcon, keyguardStatusView, keyguardBottomAreaView);
        return keyguardIndicationController;
    }

    public VolumeDialogComponent createVolumeDialogComponent(SystemUI systemUI, Context context) {
        return new VolumeDialogComponent(systemUI, context);
    }

    public KeyguardEnvironment provideKeyguardEnvironment(Context context) {
        return new KeyguardEnvironmentImpl();
    }

    public NotificationLockscreenUserManager provideNotificationLockscreenUserManager(Context context) {
        return new NotificationLockscreenUserManagerImpl(context);
    }

    public AssistManager provideAssistManager(DeviceProvisionedController deviceProvisionedController, Context context) {
        return new AssistManager(deviceProvisionedController, context);
    }

    public NotificationEntryManager provideNotificationEntryManager(Context context) {
        return new NotificationEntryManager(context);
    }

    public EnhancedEstimates provideEnhancedEstimates(Context context) {
        return new EnhancedEstimatesImpl();
    }

    public NotificationListener provideNotificationListener(Context context) {
        return new NotificationListener(context);
    }

    public NotificationInterruptionStateProvider provideNotificationInterruptionStateProvider(Context context) {
        return new NotificationInterruptionStateProvider(context);
    }

    public ShadeController provideShadeController(Context context) {
        return (ShadeController) SysUiServiceProvider.getComponent(context, StatusBar.class);
    }
}

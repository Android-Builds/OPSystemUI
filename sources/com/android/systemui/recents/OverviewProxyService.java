package com.android.systemui.recents;

import android.app.ActivityTaskManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.IFingerprintService;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Log;
import android.view.InputMonitor;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.policy.ScreenDecorationsUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.shared.recents.IOverviewProxy;
import com.android.systemui.shared.recents.IOverviewProxy.Stub;
import com.android.systemui.shared.recents.ISystemUiProxy;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.statusbar.phone.NavigationBarFragment;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowCallback;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.android.systemui.statusbar.policy.CallbackController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class OverviewProxyService implements CallbackController<OverviewProxyListener>, ModeChangedListener, Dumpable {
    private Region mActiveNavBarRegion;
    /* access modifiers changed from: private */
    public final ContentObserver mAssistContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        public void onChange(boolean z, Uri uri) {
            OverviewProxyService.this.sendAssistantAvailabilityInternal();
        }
    };
    /* access modifiers changed from: private */
    public String mAuthenticatingPackage = null;
    private boolean mBound;
    /* access modifiers changed from: private */
    public int mConnectionBackoffAttempts;
    /* access modifiers changed from: private */
    public final List<OverviewProxyListener> mConnectionCallbacks = new ArrayList();
    private final Runnable mConnectionRunnable = new Runnable() {
        public final void run() {
            OverviewProxyService.this.internalConnectToCurrentUser();
        }
    };
    /* access modifiers changed from: private */
    public ContentResolver mContentResolver;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public int mCurrentBoundedUserId = -1;
    /* access modifiers changed from: private */
    public final Runnable mDeferredConnectionCallback = new Runnable() {
        public final void run() {
            OverviewProxyService.this.lambda$new$0$OverviewProxyService();
        }
    };
    private final DeviceProvisionedListener mDeviceProvisionedCallback = new DeviceProvisionedListener() {
        public void onUserSetupChanged() {
            if (OverviewProxyService.this.mDeviceProvisionedController.isCurrentUserSetup()) {
                OverviewProxyService.this.internalConnectToCurrentUser();
            }
        }

        public void onUserSwitched() {
            OverviewProxyService.this.mConnectionBackoffAttempts = 0;
            OverviewProxyService.this.internalConnectToCurrentUser();
        }
    };
    /* access modifiers changed from: private */
    public final DeviceProvisionedController mDeviceProvisionedController;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private boolean mIsEnabled;
    private final BroadcastReceiver mLauncherStateChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String[] stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
            if (stringArrayExtra != null) {
                for (String equals : stringArrayExtra) {
                    if (equals.equals("net.oneplus.launcher.wallpaper.DummyWallpaper")) {
                        Log.d("OverviewProxyService", "Ignore dummy wallpaper package change");
                        return;
                    }
                }
            }
            OverviewProxyService.this.updateEnabledState();
            OverviewProxyService.this.startConnectionToCurrentUser();
        }
    };
    /* access modifiers changed from: private */
    public float mNavBarButtonAlpha;
    private final NavigationBarController mNavBarController;
    private int mNavBarMode = 0;
    /* access modifiers changed from: private */
    public IOverviewProxy mOverviewProxy;
    private final ServiceConnection mOverviewServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            String str = "OverviewProxyService";
            OverviewProxyService.this.mConnectionBackoffAttempts = 0;
            OverviewProxyService.this.mHandler.removeCallbacks(OverviewProxyService.this.mDeferredConnectionCallback);
            try {
                iBinder.linkToDeath(OverviewProxyService.this.mOverviewServiceDeathRcpt, 0);
                OverviewProxyService overviewProxyService = OverviewProxyService.this;
                overviewProxyService.mCurrentBoundedUserId = overviewProxyService.mDeviceProvisionedController.getCurrentUser();
                OverviewProxyService.this.mOverviewProxy = Stub.asInterface(iBinder);
                Bundle bundle = new Bundle();
                bundle.putBinder("extra_sysui_proxy", OverviewProxyService.this.mSysUiProxy.asBinder());
                bundle.putFloat("extra_window_corner_radius", OverviewProxyService.this.mWindowCornerRadius);
                bundle.putBoolean("extra_supports_window_corners", OverviewProxyService.this.mSupportsRoundedCornersOnWindows);
                try {
                    OverviewProxyService.this.mOverviewProxy.onInitialize(bundle);
                } catch (RemoteException e) {
                    OverviewProxyService.this.mCurrentBoundedUserId = -1;
                    Log.e(str, "Failed to call onInitialize()", e);
                }
                OverviewProxyService.this.dispatchNavButtonBounds();
                OverviewProxyService.this.updateSystemUiStateFlags();
                OverviewProxyService.this.notifyConnectionChanged();
                OverviewProxyService.this.sendAssistantAvailabilityInternal();
                OverviewProxyService.this.mContentResolver.registerContentObserver(Secure.getUriFor("assistant"), false, OverviewProxyService.this.mAssistContentObserver, -1);
            } catch (RemoteException e2) {
                Log.e(str, "Lost connection to launcher service", e2);
                OverviewProxyService.this.disconnectFromLauncherService();
                OverviewProxyService.this.retryConnectionWithBackoff();
            }
        }

        public void onNullBinding(ComponentName componentName) {
            StringBuilder sb = new StringBuilder();
            sb.append("Null binding of '");
            sb.append(componentName);
            sb.append("', try reconnecting");
            Log.w("OverviewProxyService", sb.toString());
            OverviewProxyService.this.mCurrentBoundedUserId = -1;
            OverviewProxyService.this.retryConnectionWithBackoff();
        }

        public void onBindingDied(ComponentName componentName) {
            StringBuilder sb = new StringBuilder();
            sb.append("Binding died of '");
            sb.append(componentName);
            sb.append("', try reconnecting");
            Log.w("OverviewProxyService", sb.toString());
            OverviewProxyService.this.mCurrentBoundedUserId = -1;
            OverviewProxyService.this.retryConnectionWithBackoff();
        }

        public void onServiceDisconnected(ComponentName componentName) {
            OverviewProxyService.this.mCurrentBoundedUserId = -1;
            OverviewProxyService.this.mContentResolver.unregisterContentObserver(OverviewProxyService.this.mAssistContentObserver);
        }
    };
    /* access modifiers changed from: private */
    public final DeathRecipient mOverviewServiceDeathRcpt = new DeathRecipient() {
        public final void binderDied() {
            OverviewProxyService.this.cleanupAfterDeath();
        }
    };
    private final Intent mQuickStepIntent;
    private final ComponentName mRecentsComponentName;
    private StatusBar mStatusBar;
    private MotionEvent mStatusBarGestureDownEvent;
    private final StatusBarWindowController mStatusBarWinController;
    private final StatusBarWindowCallback mStatusBarWindowCallback = new StatusBarWindowCallback() {
        public final void onStateChanged(boolean z, boolean z2, boolean z3) {
            OverviewProxyService.this.onStatusBarStateChanged(z, z2, z3);
        }
    };
    /* access modifiers changed from: private */
    public boolean mSupportsRoundedCornersOnWindows;
    /* access modifiers changed from: private */
    public ISystemUiProxy mSysUiProxy = new ISystemUiProxy.Stub() {
        public void startScreenPinning(int i) {
            if (verifyCaller("startScreenPinning")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable(i) {
                        private final /* synthetic */ int f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            C10661.this.lambda$startScreenPinning$0$OverviewProxyService$1(this.f$1);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$startScreenPinning$0$OverviewProxyService$1(int i) {
            StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(OverviewProxyService.this.mContext, StatusBar.class);
            if (statusBar != null) {
                statusBar.showScreenPinningRequest(i, false);
            }
        }

        public void stopScreenPinning() {
            if (verifyCaller("stopScreenPinning")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post($$Lambda$OverviewProxyService$1$WjFAUijOf0iWbjyxz5nDkhLzxA.INSTANCE);
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        static /* synthetic */ void lambda$stopScreenPinning$1() {
            try {
                ActivityTaskManager.getService().stopSystemLockTaskMode();
            } catch (RemoteException unused) {
                Log.e("OverviewProxyService", "Failed to stop screen pinning");
            }
        }

        public void onStatusBarMotionEvent(MotionEvent motionEvent) {
            if (verifyCaller("onStatusBarMotionEvent")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mTouchHandler.obtainMessage(1, motionEvent).sendToTarget();
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public void onSplitScreenInvoked() {
            if (verifyCaller("onSplitScreenInvoked")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Divider divider = (Divider) SysUiServiceProvider.getComponent(OverviewProxyService.this.mContext, Divider.class);
                    if (divider != null) {
                        divider.onDockedFirstAnimationFrame();
                    }
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public void onOverviewShown(boolean z) {
            if (verifyCaller("onOverviewShown")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable(z) {
                        private final /* synthetic */ boolean f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            C10661.this.lambda$onOverviewShown$2$OverviewProxyService$1(this.f$1);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$onOverviewShown$2$OverviewProxyService$1(boolean z) {
            for (int size = OverviewProxyService.this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
                ((OverviewProxyListener) OverviewProxyService.this.mConnectionCallbacks.get(size)).onOverviewShown(z);
            }
        }

        public Rect getNonMinimizedSplitScreenSecondaryBounds() {
            if (!verifyCaller("getNonMinimizedSplitScreenSecondaryBounds")) {
                return null;
            }
            long clearCallingIdentity = Binder.clearCallingIdentity();
            try {
                Divider divider = (Divider) SysUiServiceProvider.getComponent(OverviewProxyService.this.mContext, Divider.class);
                if (divider != null) {
                    return divider.getView().getNonMinimizedSplitScreenSecondaryBounds();
                }
                Binder.restoreCallingIdentity(clearCallingIdentity);
                return null;
            } finally {
                Binder.restoreCallingIdentity(clearCallingIdentity);
            }
        }

        public void setNavBarButtonAlpha(float f, boolean z) {
            if (verifyCaller("setNavBarButtonAlpha")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mNavBarButtonAlpha = f;
                    OverviewProxyService.this.mHandler.post(new Runnable(f, z) {
                        private final /* synthetic */ float f$1;
                        private final /* synthetic */ boolean f$2;

                        {
                            this.f$1 = r2;
                            this.f$2 = r3;
                        }

                        public final void run() {
                            C10661.this.lambda$setNavBarButtonAlpha$3$OverviewProxyService$1(this.f$1, this.f$2);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$setNavBarButtonAlpha$3$OverviewProxyService$1(float f, boolean z) {
            OverviewProxyService.this.notifyNavBarButtonAlphaChanged(f, z);
        }

        public void setBackButtonAlpha(float f, boolean z) {
            setNavBarButtonAlpha(f, z);
        }

        public void onAssistantProgress(float f) {
            if (verifyCaller("onAssistantProgress")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable(f) {
                        private final /* synthetic */ float f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            C10661.this.lambda$onAssistantProgress$4$OverviewProxyService$1(this.f$1);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$onAssistantProgress$4$OverviewProxyService$1(float f) {
            OverviewProxyService.this.notifyAssistantProgress(f);
        }

        public void onAssistantGestureCompletion(float f) {
            if (verifyCaller("onAssistantGestureCompletion")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable(f) {
                        private final /* synthetic */ float f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            C10661.this.lambda$onAssistantGestureCompletion$5$OverviewProxyService$1(this.f$1);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$onAssistantGestureCompletion$5$OverviewProxyService$1(float f) {
            OverviewProxyService.this.notifyAssistantGestureCompletion(f);
        }

        public void startAssistant(Bundle bundle) {
            if (verifyCaller("startAssistant")) {
                StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(OverviewProxyService.this.mContext, StatusBar.class);
                if (statusBar == null || !statusBar.checkGestureStartAssist(bundle)) {
                    long clearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        OverviewProxyService.this.mHandler.post(new Runnable(bundle) {
                            private final /* synthetic */ Bundle f$1;

                            {
                                this.f$1 = r2;
                            }

                            public final void run() {
                                C10661.this.lambda$startAssistant$6$OverviewProxyService$1(this.f$1);
                            }
                        });
                    } finally {
                        Binder.restoreCallingIdentity(clearCallingIdentity);
                    }
                }
            }
        }

        public /* synthetic */ void lambda$startAssistant$6$OverviewProxyService$1(Bundle bundle) {
            OverviewProxyService.this.notifyStartAssistant(bundle);
        }

        public Bundle monitorGestureInput(String str, int i) {
            if (!verifyCaller("monitorGestureInput")) {
                return null;
            }
            long clearCallingIdentity = Binder.clearCallingIdentity();
            try {
                InputMonitor monitorGestureInput = InputManager.getInstance().monitorGestureInput(str, i);
                Bundle bundle = new Bundle();
                bundle.putParcelable("extra_input_monitor", monitorGestureInput);
                return bundle;
            } finally {
                Binder.restoreCallingIdentity(clearCallingIdentity);
            }
        }

        public void notifyAccessibilityButtonClicked(int i) {
            if (verifyCaller("notifyAccessibilityButtonClicked")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    AccessibilityManager.getInstance(OverviewProxyService.this.mContext).notifyAccessibilityButtonClicked(i);
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public void notifyAccessibilityButtonLongClicked() {
            if (verifyCaller("notifyAccessibilityButtonLongClicked")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Intent intent = new Intent("com.android.internal.intent.action.CHOOSE_ACCESSIBILITY_BUTTON");
                    intent.addFlags(268468224);
                    OverviewProxyService.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public void notifyGestureStarted() {
            String str = "notifyGestureStarted";
            Log.d("OverviewProxyService", str);
            if (verifyCaller(str)) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                String str2 = "fingerprint";
                IFingerprintService asInterface = IFingerprintService.Stub.asInterface(ServiceManager.getService(str2));
                if (OpUtils.isCustomFingerprint()) {
                    System.putIntForUser(OverviewProxyService.this.mContext.getContentResolver(), "global_gesture_notify_applocker", 10, 0);
                }
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable((FingerprintManager) OverviewProxyService.this.mContext.getSystemService(str2), asInterface) {
                        private final /* synthetic */ FingerprintManager f$1;
                        private final /* synthetic */ IFingerprintService f$2;

                        {
                            this.f$1 = r2;
                            this.f$2 = r3;
                        }

                        public final void run() {
                            C10661.this.lambda$notifyGestureStarted$7$OverviewProxyService$1(this.f$1, this.f$2);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$notifyGestureStarted$7$OverviewProxyService$1(FingerprintManager fingerprintManager, IFingerprintService iFingerprintService) {
            String authenticatedPackage = fingerprintManager != null ? fingerprintManager.getAuthenticatedPackage() : null;
            StringBuilder sb = new StringBuilder();
            sb.append("authenticatingPkg = ");
            sb.append(authenticatedPackage);
            String str = "OverviewProxyService";
            Log.d(str, sb.toString());
            if (authenticatedPackage != null && authenticatedPackage.length() != 0 && iFingerprintService != null && !authenticatedPackage.equals("com.android.systemui")) {
                try {
                    OverviewProxyService.this.mAuthenticatingPackage = authenticatedPackage;
                    iFingerprintService.updateStatus(12);
                } catch (RemoteException e) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("updateStatus , ");
                    sb2.append(e);
                    Log.w(str, sb2.toString());
                }
            }
        }

        public void notifyGestureEnded(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("notifyGestureEnded action = ");
            sb.append(i);
            sb.append(", mAuthenticatingPackage = ");
            sb.append(OverviewProxyService.this.mAuthenticatingPackage);
            Log.d("OverviewProxyService", sb.toString());
            if (verifyCaller("notifyGestureEnded")) {
                long clearCallingIdentity = Binder.clearCallingIdentity();
                if (OpUtils.isCustomFingerprint()) {
                    System.putIntForUser(OverviewProxyService.this.mContext.getContentResolver(), "global_gesture_notify_applocker", i, 0);
                }
                try {
                    OverviewProxyService.this.mHandler.post(new Runnable(i) {
                        private final /* synthetic */ int f$1;

                        {
                            this.f$1 = r2;
                        }

                        public final void run() {
                            C10661.this.lambda$notifyGestureEnded$8$OverviewProxyService$1(this.f$1);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(clearCallingIdentity);
                }
            }
        }

        public /* synthetic */ void lambda$notifyGestureEnded$8$OverviewProxyService$1(int i) {
            try {
                if (OverviewProxyService.this.mAuthenticatingPackage != null && !"com.android.systemui".equals(OverviewProxyService.this.mAuthenticatingPackage) && (i == 3 || i == 50)) {
                    IFingerprintService asInterface = IFingerprintService.Stub.asInterface(ServiceManager.getService("fingerprint"));
                    if (asInterface != null) {
                        asInterface.updateStatus(11);
                    }
                }
                OverviewProxyService.this.mAuthenticatingPackage = null;
            } catch (RemoteException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("updateStatus , ");
                sb.append(e);
                Log.w("OverviewProxyService", sb.toString());
            }
        }

        private boolean verifyCaller(String str) {
            int identifier = Binder.getCallingUserHandle().getIdentifier();
            if (identifier == OverviewProxyService.this.mCurrentBoundedUserId) {
                return true;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Launcher called sysui with invalid user: ");
            sb.append(identifier);
            sb.append(", reason: ");
            sb.append(str);
            Log.w("OverviewProxyService", sb.toString());
            return false;
        }
    };
    private int mSysUiStateFlags;
    /* access modifiers changed from: private */
    public Handler mTouchHandler = new Handler(Looper.myLooper(), null, true) {
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                OverviewProxyService.this.dispatchLauncherTouch((MotionEvent) message.obj);
            } else if (i == 2) {
                OverviewProxyService.this.dispatchCancelTouch();
            }
        }
    };
    /* access modifiers changed from: private */
    public float mWindowCornerRadius;

    public interface OverviewProxyListener {
        void onAssistantGestureCompletion(float f) {
        }

        void onAssistantProgress(float f) {
        }

        void onConnectionChanged(boolean z) {
        }

        void onNavBarButtonAlphaChanged(float f, boolean z) {
        }

        void onOverviewShown(boolean z) {
        }

        void onSystemUiStateChanged(int i) {
        }

        void startAssistant(Bundle bundle) {
        }
    }

    public /* synthetic */ void lambda$new$0$OverviewProxyService() {
        Log.w("OverviewProxyService", "Binder supposed established connection but actual connection to service timed out, trying again");
        retryConnectionWithBackoff();
    }

    public OverviewProxyService(Context context, DeviceProvisionedController deviceProvisionedController, NavigationBarController navigationBarController, NavigationModeController navigationModeController, StatusBarWindowController statusBarWindowController) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mNavBarController = navigationBarController;
        this.mStatusBarWinController = statusBarWindowController;
        this.mDeviceProvisionedController = deviceProvisionedController;
        this.mConnectionBackoffAttempts = 0;
        this.mRecentsComponentName = ComponentName.unflattenFromString(context.getString(17039781));
        this.mQuickStepIntent = new Intent("android.intent.action.QUICKSTEP_SERVICE").setPackage(this.mRecentsComponentName.getPackageName());
        this.mWindowCornerRadius = ScreenDecorationsUtils.getWindowCornerRadius(this.mContext.getResources());
        this.mSupportsRoundedCornersOnWindows = ScreenDecorationsUtils.supportsRoundedCornersOnWindows(this.mContext.getResources());
        this.mNavBarButtonAlpha = 1.0f;
        this.mNavBarMode = navigationModeController.addListener(this);
        updateEnabledState();
        this.mDeviceProvisionedController.addCallback(this.mDeviceProvisionedCallback);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        intentFilter.addDataScheme("package");
        intentFilter.addDataSchemeSpecificPart(this.mRecentsComponentName.getPackageName(), 0);
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        this.mContext.registerReceiver(this.mLauncherStateChangedReceiver, intentFilter);
        statusBarWindowController.registerCallback(this.mStatusBarWindowCallback);
        this.mContentResolver = this.mContext.getContentResolver();
    }

    public void notifyBackAction(boolean z, int i, int i2, boolean z2, boolean z3) {
        try {
            if (this.mOverviewProxy != null) {
                this.mOverviewProxy.onBackAction(z, i, i2, z2, z3);
            }
        } catch (RemoteException e) {
            Log.e("OverviewProxyService", "Failed to notify back action", e);
        }
    }

    public void setSystemUiStateFlag(int i, boolean z, int i2) {
        if (i2 == 0) {
            int i3 = this.mSysUiStateFlags;
            int i4 = z ? i | i3 : (~i) & i3;
            if (this.mSysUiStateFlags != i4) {
                this.mSysUiStateFlags = i4;
                notifySystemUiStateChanged(this.mSysUiStateFlags);
                notifySystemUiStateFlags(this.mSysUiStateFlags);
            }
        }
    }

    public int getSystemUiStateFlags() {
        return this.mSysUiStateFlags;
    }

    public void updateSystemUiStateFlags() {
        NavigationBarFragment defaultNavigationBarFragment = this.mNavBarController.getDefaultNavigationBarFragment();
        NavigationBarView navigationBarView = this.mNavBarController.getNavigationBarView(this.mContext.getDisplayId());
        this.mSysUiStateFlags = 0;
        if (defaultNavigationBarFragment != null) {
            defaultNavigationBarFragment.updateSystemUiStateFlags(-1);
        }
        if (navigationBarView != null) {
            navigationBarView.updateSystemUiStateFlags();
        }
        StatusBarWindowController statusBarWindowController = this.mStatusBarWinController;
        if (statusBarWindowController != null) {
            statusBarWindowController.notifyStateChangedCallbacks();
        }
        notifySystemUiStateFlags(this.mSysUiStateFlags);
    }

    private void notifySystemUiStateFlags(int i) {
        String str = "OverviewProxyService";
        StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
        if (statusBar != null && statusBar.getStatusBarWindowState() == 2) {
            i |= 32768;
        }
        try {
            if (this.mOverviewProxy != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("SystemUi flags: ");
                sb.append(Integer.toHexString(i));
                Log.d(str, sb.toString());
                this.mOverviewProxy.onSystemUiStateChanged(i);
            }
        } catch (RemoteException e) {
            Log.e(str, "Failed to notify sysui state change", e);
        }
    }

    public void notifyKeyguardDone() {
        String str = "OverviewProxyService";
        try {
            if (this.mOverviewProxy != null) {
                this.mOverviewProxy.onKeyguardDone();
                Log.d(str, "notifyKeyguardDone");
            }
        } catch (RemoteException e) {
            Log.e(str, "Failed to notify keyguard done", e);
        }
    }

    /* access modifiers changed from: private */
    public void onStatusBarStateChanged(boolean z, boolean z2, boolean z3) {
        int displayId = this.mContext.getDisplayId();
        boolean z4 = true;
        setSystemUiStateFlag(64, z && !z2, displayId);
        if (!z || !z2) {
            z4 = false;
        }
        setSystemUiStateFlag(512, z4, displayId);
        setSystemUiStateFlag(8, z3, displayId);
    }

    public void onActiveNavBarRegionChanges(Region region) {
        this.mActiveNavBarRegion = region;
        dispatchNavButtonBounds();
    }

    /* access modifiers changed from: private */
    public void dispatchNavButtonBounds() {
        IOverviewProxy iOverviewProxy = this.mOverviewProxy;
        if (iOverviewProxy != null) {
            Region region = this.mActiveNavBarRegion;
            if (region != null) {
                try {
                    iOverviewProxy.onActiveNavBarRegionChanges(region);
                } catch (RemoteException e) {
                    Log.e("OverviewProxyService", "Failed to call onActiveNavBarRegionChanges()", e);
                }
            }
        }
    }

    public void cleanupAfterDeath() {
        if (this.mStatusBarGestureDownEvent != null) {
            this.mHandler.post(new Runnable() {
                public final void run() {
                    OverviewProxyService.this.lambda$cleanupAfterDeath$1$OverviewProxyService();
                }
            });
        }
        startConnectionToCurrentUser();
    }

    public /* synthetic */ void lambda$cleanupAfterDeath$1$OverviewProxyService() {
        StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
        if (statusBar != null) {
            try {
                this.mStatusBarGestureDownEvent.setAction(3);
                statusBar.dispatchNotificationsPanelTouchEvent(this.mStatusBarGestureDownEvent);
                this.mStatusBarGestureDownEvent.recycle();
                this.mStatusBarGestureDownEvent = null;
            } catch (NullPointerException e) {
                Log.e("OverviewProxyService", "StatusBarGestureDownEvent is null ", e);
            }
        }
    }

    public void startConnectionToCurrentUser() {
        if (this.mHandler.getLooper() != Looper.myLooper()) {
            this.mHandler.post(this.mConnectionRunnable);
        } else {
            internalConnectToCurrentUser();
        }
    }

    /* access modifiers changed from: private */
    public void dispatchLauncherTouch(MotionEvent motionEvent) {
        StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
        if (statusBar != null && motionEvent != null) {
            statusBar.dispatchNotificationsPanelTouchEvent(motionEvent);
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 0) {
                this.mStatusBarGestureDownEvent = MotionEvent.obtain(motionEvent);
            }
            boolean z = true;
            if (actionMasked == 1 || actionMasked == 3) {
                StringBuilder sb = new StringBuilder();
                sb.append("dispatchLauncherTouch, event:");
                if (this.mStatusBarGestureDownEvent == null) {
                    z = false;
                }
                sb.append(z);
                sb.append(", ");
                sb.append(actionMasked);
                Log.d("OverviewProxyService", sb.toString());
                MotionEvent motionEvent2 = this.mStatusBarGestureDownEvent;
                if (motionEvent2 != null) {
                    motionEvent2.recycle();
                    this.mStatusBarGestureDownEvent = null;
                }
            }
            motionEvent.recycle();
        }
    }

    /* access modifiers changed from: private */
    public void dispatchCancelTouch() {
        StatusBar statusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
        StringBuilder sb = new StringBuilder();
        sb.append("dispatchCancelTouch, event:");
        sb.append(this.mStatusBarGestureDownEvent != null);
        Log.d("OverviewProxyService", sb.toString());
        if (statusBar != null && this.mStatusBarGestureDownEvent != null) {
            System.out.println("MERONG dispatchNotificationPanelTouchEvent");
            this.mStatusBarGestureDownEvent.setAction(3);
            statusBar.dispatchNotificationsPanelTouchEvent(this.mStatusBarGestureDownEvent);
            this.mStatusBarGestureDownEvent.recycle();
            this.mStatusBarGestureDownEvent = null;
        }
    }

    /* access modifiers changed from: private */
    public void internalConnectToCurrentUser() {
        disconnectFromLauncherService();
        String str = "OverviewProxyService";
        if (!this.mDeviceProvisionedController.isCurrentUserSetup() || !isEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Cannot attempt connection, is setup ");
            sb.append(this.mDeviceProvisionedController.isCurrentUserSetup());
            sb.append(", is enabled ");
            sb.append(isEnabled());
            Log.v(str, sb.toString());
            return;
        }
        this.mHandler.removeCallbacks(this.mConnectionRunnable);
        try {
            this.mBound = this.mContext.bindServiceAsUser(new Intent("android.intent.action.QUICKSTEP_SERVICE").setPackage(this.mRecentsComponentName.getPackageName()), this.mOverviewServiceConnection, 33554433, UserHandle.of(this.mDeviceProvisionedController.getCurrentUser()));
        } catch (SecurityException e) {
            Log.e(str, "Unable to bind because of security error", e);
        }
        if (this.mBound) {
            this.mHandler.postDelayed(this.mDeferredConnectionCallback, 5000);
        } else {
            retryConnectionWithBackoff();
        }
    }

    /* access modifiers changed from: private */
    public void retryConnectionWithBackoff() {
        if (!this.mHandler.hasCallbacks(this.mConnectionRunnable)) {
            long min = (long) Math.min(Math.scalb(1000.0f, this.mConnectionBackoffAttempts), 600000.0f);
            this.mHandler.postDelayed(this.mConnectionRunnable, min);
            this.mConnectionBackoffAttempts++;
            StringBuilder sb = new StringBuilder();
            sb.append("Failed to connect on attempt ");
            sb.append(this.mConnectionBackoffAttempts);
            sb.append(" will try again in ");
            sb.append(min);
            sb.append("ms");
            Log.w("OverviewProxyService", sb.toString());
        }
    }

    public void addCallback(OverviewProxyListener overviewProxyListener) {
        this.mConnectionCallbacks.add(overviewProxyListener);
        overviewProxyListener.onConnectionChanged(this.mOverviewProxy != null);
        overviewProxyListener.onNavBarButtonAlphaChanged(this.mNavBarButtonAlpha, false);
        overviewProxyListener.onSystemUiStateChanged(this.mSysUiStateFlags);
    }

    public void removeCallback(OverviewProxyListener overviewProxyListener) {
        this.mConnectionCallbacks.remove(overviewProxyListener);
    }

    public boolean shouldShowSwipeUpUI() {
        return isEnabled() && !QuickStepContract.isLegacyMode(this.mNavBarMode);
    }

    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    public IOverviewProxy getProxy() {
        return this.mOverviewProxy;
    }

    public ISystemUiProxy getSysUIProxy() {
        return this.mSysUiProxy;
    }

    /* access modifiers changed from: private */
    public void disconnectFromLauncherService() {
        if (this.mBound) {
            this.mContext.unbindService(this.mOverviewServiceConnection);
            this.mBound = false;
        }
        IOverviewProxy iOverviewProxy = this.mOverviewProxy;
        if (iOverviewProxy != null) {
            iOverviewProxy.asBinder().unlinkToDeath(this.mOverviewServiceDeathRcpt, 0);
            this.mOverviewProxy = null;
            notifyNavBarButtonAlphaChanged(1.0f, false);
            notifyConnectionChanged();
        }
    }

    public void notifyNavBarButtonAlphaChanged(float f, boolean z) {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            ((OverviewProxyListener) this.mConnectionCallbacks.get(size)).onNavBarButtonAlphaChanged(f, z);
        }
    }

    /* access modifiers changed from: private */
    public void notifyConnectionChanged() {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            ((OverviewProxyListener) this.mConnectionCallbacks.get(size)).onConnectionChanged(this.mOverviewProxy != null);
        }
    }

    /* access modifiers changed from: private */
    public void notifyAssistantProgress(float f) {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            ((OverviewProxyListener) this.mConnectionCallbacks.get(size)).onAssistantProgress(f);
        }
    }

    /* access modifiers changed from: private */
    public void notifyAssistantGestureCompletion(float f) {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            ((OverviewProxyListener) this.mConnectionCallbacks.get(size)).onAssistantGestureCompletion(f);
        }
    }

    private void notifySystemUiStateChanged(int i) {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            ((OverviewProxyListener) this.mConnectionCallbacks.get(size)).onSystemUiStateChanged(i);
        }
    }

    /* access modifiers changed from: private */
    public void notifyStartAssistant(Bundle bundle) {
        for (int size = this.mConnectionCallbacks.size() - 1; size >= 0; size--) {
            ((OverviewProxyListener) this.mConnectionCallbacks.get(size)).startAssistant(bundle);
        }
    }

    /* access modifiers changed from: private */
    public void updateEnabledState() {
        this.mIsEnabled = this.mContext.getPackageManager().resolveServiceAsUser(this.mQuickStepIntent, 1048576, ActivityManagerWrapper.getInstance().getCurrentUserId()) != null;
        StringBuilder sb = new StringBuilder();
        sb.append("updateEnabledState to ");
        sb.append(this.mIsEnabled);
        Log.d("OverviewProxyService", sb.toString());
    }

    public void onNavigationModeChanged(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("onNavigationModeChanged: ");
        sb.append(this.mNavBarMode);
        sb.append(" to ");
        sb.append(i);
        Log.d("OverviewProxyService", sb.toString());
        this.mNavBarMode = i;
        WindowManagerWrapper.getInstance().setNavBarVirtualKeyHapticFeedbackEnabled(!shouldShowSwipeUpUI());
    }

    public int getNavBarMode() {
        return this.mNavBarMode;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("OverviewProxyService state:");
        printWriter.print("  recentsComponentName=");
        printWriter.println(this.mRecentsComponentName);
        printWriter.print("  isConnected=");
        printWriter.println(this.mOverviewProxy != null);
        printWriter.print("  isCurrentUserSetup=");
        printWriter.println(this.mDeviceProvisionedController.isCurrentUserSetup());
        printWriter.print("  connectionBackoffAttempts=");
        printWriter.println(this.mConnectionBackoffAttempts);
        printWriter.print("  quickStepIntent=");
        printWriter.println(this.mQuickStepIntent);
        printWriter.print("  quickStepIntentResolved=");
        printWriter.println(isEnabled());
        printWriter.print("  mSysUiStateFlags=");
        printWriter.println(this.mSysUiStateFlags);
        StringBuilder sb = new StringBuilder();
        sb.append("    ");
        sb.append(QuickStepContract.getSystemUiStateString(this.mSysUiStateFlags));
        printWriter.println(sb.toString());
        printWriter.print("    backGestureDisabled=");
        printWriter.println(QuickStepContract.isBackGestureDisabled(this.mSysUiStateFlags));
        printWriter.print("    assistantGestureDisabled=");
        printWriter.println(QuickStepContract.isAssistantGestureDisabled(this.mSysUiStateFlags));
        printWriter.print("    isEnabled=");
        printWriter.println(isEnabled());
        printWriter.print("    mNavBarMode=");
        printWriter.println(this.mNavBarMode);
        printWriter.print("    mAuthenticatingPackage=");
        printWriter.println(this.mAuthenticatingPackage);
    }

    public void updateSystemUIStateFlagsInternal() {
        if (this.mNavBarController.getNavigationBarView(this.mContext.getDisplayId()) == null) {
            int displayId = this.mContext.getDisplayId();
            if (this.mStatusBar == null) {
                this.mStatusBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
            }
            if (OpUtils.DEBUG_ONEPLUS) {
                Log.d("OverviewProxyService", "Update system ui flag state to launcher when navigation bar is hidden");
            }
            int disableFlag = this.mStatusBar.getDisableFlag();
            boolean z = true;
            setSystemUiStateFlag(128, (16777216 & disableFlag) != 0, displayId);
            if ((disableFlag & 2097152) == 0) {
                z = false;
            }
            setSystemUiStateFlag(256, z, displayId);
            if (this.mStatusBar.getPanel() != null) {
                setSystemUiStateFlag(4, this.mStatusBar.getPanel().isFullyExpanded(), displayId);
            }
        }
    }

    /* access modifiers changed from: private */
    public void sendAssistantAvailabilityInternal() {
        String str = "OverviewProxyService";
        boolean z = true;
        boolean z2 = ((AssistManager) Dependency.get(AssistManager.class)).getAssistInfoForUser(-2) != null;
        if (getProxy() != null) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("Send assistant availability data to launcher ");
                sb.append(z2);
                Log.d(str, sb.toString());
                IOverviewProxy proxy = getProxy();
                if (!z2 || !QuickStepContract.isGesturalMode(this.mNavBarMode)) {
                    z = false;
                }
                proxy.onAssistantAvailable(z);
            } catch (RemoteException unused) {
                Log.w(str, "Unable to send assistant availability data to launcher");
            }
        }
    }
}

package com.android.systemui.assist;

import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.metrics.LogMaker;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import com.android.internal.app.AssistUtils;
import com.android.internal.app.IVoiceInteractionSessionListener;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractionSessionShowCallback.Stub;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.applications.InterestingConfigChanges;
import com.android.systemui.ConfigurationChangedReceiver;
import com.android.systemui.Dependency;
import com.android.systemui.R$anim;
import com.android.systemui.R$dimen;
import com.android.systemui.R$layout;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.assist.p004ui.DefaultUiController;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.recents.OverviewProxyService.OverviewProxyListener;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;

public class AssistManager implements ConfigurationChangedReceiver {
    private final AssistDisclosure mAssistDisclosure;
    protected final AssistUtils mAssistUtils;
    protected final Context mContext;
    private final DeviceProvisionedController mDeviceProvisionedController;
    private final AssistHandleBehaviorController mHandleController;
    /* access modifiers changed from: private */
    public Runnable mHideRunnable = new Runnable() {
        public void run() {
            AssistManager.this.mView.removeCallbacks(this);
            AssistManager.this.mView.show(false, true);
        }
    };
    private final InterestingConfigChanges mInterestingConfigChanges;
    private boolean mIsPowerLongPressWithAssistant = false;
    private boolean mIsPowerLongPressWithGoogleAssistant = false;
    private final PhoneStateMonitor mPhoneStateMonitor;
    private final boolean mShouldEnableOrb;
    private IVoiceInteractionSessionShowCallback mShowCallback = new Stub() {
        public void onFailed() throws RemoteException {
            AssistManager.this.mView.post(AssistManager.this.mHideRunnable);
        }

        public void onShown() throws RemoteException {
            AssistManager.this.mView.post(AssistManager.this.mHideRunnable);
        }
    };
    private final UiController mUiController;
    /* access modifiers changed from: private */
    public AssistOrbContainer mView;
    private final WindowManager mWindowManager;

    public interface UiController {
        void onGestureCompletion(float f);

        void onInvocationProgress(int i, float f);
    }

    /* access modifiers changed from: protected */
    public boolean shouldShowOrb() {
        return true;
    }

    public AssistManager(DeviceProvisionedController deviceProvisionedController, Context context) {
        this.mContext = context;
        this.mDeviceProvisionedController = deviceProvisionedController;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mAssistUtils = new AssistUtils(context);
        this.mAssistDisclosure = new AssistDisclosure(context, new Handler());
        this.mPhoneStateMonitor = new PhoneStateMonitor(context);
        this.mHandleController = new AssistHandleBehaviorController(context, this.mAssistUtils, new Handler());
        registerVoiceInteractionSessionListener();
        this.mInterestingConfigChanges = new InterestingConfigChanges(-2147482748);
        onConfigurationChanged(context.getResources().getConfiguration());
        this.mShouldEnableOrb = false;
        this.mUiController = new DefaultUiController(this.mContext);
        ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).addCallback((OverviewProxyListener) new OverviewProxyListener() {
            public void onAssistantProgress(float f) {
                AssistManager.this.onInvocationProgress(1, f);
            }

            public void onAssistantGestureCompletion(float f) {
                AssistManager.this.onGestureCompletion(f);
            }
        });
    }

    /* access modifiers changed from: protected */
    public void registerVoiceInteractionSessionListener() {
        this.mAssistUtils.registerVoiceInteractionSessionListener(new IVoiceInteractionSessionListener.Stub() {
            public void onSetUiHints(Bundle bundle) {
            }

            public void onVoiceSessionHidden() throws RemoteException {
            }

            public void onVoiceSessionShown() throws RemoteException {
            }
        });
    }

    public void onConfigurationChanged(Configuration configuration) {
        boolean z;
        if (this.mInterestingConfigChanges.applyNewConfig(this.mContext.getResources())) {
            AssistOrbContainer assistOrbContainer = this.mView;
            if (assistOrbContainer != null) {
                z = assistOrbContainer.isShowing();
                this.mWindowManager.removeView(this.mView);
            } else {
                z = false;
            }
            this.mView = (AssistOrbContainer) LayoutInflater.from(this.mContext).inflate(R$layout.assist_orb, null);
            this.mView.setVisibility(8);
            this.mView.setSystemUiVisibility(1792);
            try {
                this.mWindowManager.addView(this.mView, getLayoutParams());
            } catch (RuntimeException e) {
                Log.e("AssistManager", e.getMessage());
            }
            if (z) {
                this.mView.show(true, false);
            }
        }
    }

    public void startAssist(Bundle bundle) {
        ComponentName assistInfo = getAssistInfo();
        if (assistInfo != null) {
            if (bundle != null) {
                this.mIsPowerLongPressWithAssistant = bundle.getBoolean("power_long_press_with_assistant_hint", false);
                this.mIsPowerLongPressWithGoogleAssistant = bundle.getBoolean("power_long_press_with_google_assistant_hint", false);
                if (this.mIsPowerLongPressWithGoogleAssistant) {
                    launchVoiceAssistFromKeyguard();
                    Log.v("AssistManager", "Launch google assistant from keyguard");
                    return;
                }
            } else {
                this.mIsPowerLongPressWithAssistant = false;
                this.mIsPowerLongPressWithGoogleAssistant = false;
            }
            boolean equals = assistInfo.equals(getVoiceInteractorComponentName());
            if (!equals || (!isVoiceSessionRunning() && shouldShowOrb())) {
                showOrb(assistInfo, equals);
                this.mView.postDelayed(this.mHideRunnable, equals ? 2500 : 1000);
            }
            if (bundle == null) {
                bundle = new Bundle();
            }
            int i = bundle.getInt("invocation_type", 0);
            if (i == 1) {
                this.mHandleController.onAssistantGesturePerformed();
            }
            int phoneState = this.mPhoneStateMonitor.getPhoneState();
            bundle.putInt("invocation_phone_state", phoneState);
            bundle.putLong("invocation_time_ms", SystemClock.uptimeMillis());
            MetricsLogger.action(new LogMaker(1716).setType(1).setSubtype(toLoggingSubType(i, phoneState)));
            if (bundle.getBoolean("com.heytap.speechassist", false)) {
                launchHeyTapVoiceAssist();
            } else {
                startAssistInternal(bundle, assistInfo, equals);
            }
        }
    }

    public void launchHeyTapVoiceAssist() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.heytap.speechassist", "com.heytap.speechassist.core.SpeechService"));
        intent.putExtra("caller_package", this.mContext.getPackageName());
        intent.putExtra("start_type", 1024);
        try {
            if (VERSION.SDK_INT >= 26) {
                this.mContext.getApplicationContext().startForegroundService(intent);
            } else {
                this.mContext.getApplicationContext().startService(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onInvocationProgress(int i, float f) {
        this.mUiController.onInvocationProgress(i, f);
    }

    public void onGestureCompletion(float f) {
        this.mUiController.onGestureCompletion(f);
    }

    public void hideAssist() {
        this.mAssistUtils.hideCurrentSession();
    }

    private LayoutParams getLayoutParams() {
        LayoutParams layoutParams = new LayoutParams(-1, this.mContext.getResources().getDimensionPixelSize(R$dimen.assist_orb_scrim_height), 2033, 280, -3);
        layoutParams.token = new Binder();
        layoutParams.gravity = 8388691;
        layoutParams.setTitle("AssistPreviewPanel");
        layoutParams.softInputMode = 49;
        return layoutParams;
    }

    private void showOrb(ComponentName componentName, boolean z) {
        maybeSwapSearchIcon(componentName, z);
        if (this.mShouldEnableOrb && !this.mIsPowerLongPressWithAssistant && !this.mIsPowerLongPressWithGoogleAssistant) {
            Log.v("AssistManager", "showOrb");
            this.mView.show(true, true);
        }
    }

    private void startAssistInternal(Bundle bundle, ComponentName componentName, boolean z) {
        if (z) {
            startVoiceInteractor(bundle);
        } else {
            startAssistActivity(bundle, componentName);
        }
    }

    private void startAssistActivity(Bundle bundle, ComponentName componentName) {
        if (this.mDeviceProvisionedController.isDeviceProvisioned()) {
            boolean z = false;
            ((CommandQueue) SysUiServiceProvider.getComponent(this.mContext, CommandQueue.class)).animateCollapsePanels(3, false);
            if (Secure.getIntForUser(this.mContext.getContentResolver(), "assist_structure_enabled", 1, -2) != 0) {
                z = true;
            }
            SearchManager searchManager = (SearchManager) this.mContext.getSystemService("search");
            if (searchManager != null) {
                final Intent assistIntent = searchManager.getAssistIntent(z);
                if (assistIntent != null) {
                    assistIntent.setComponent(componentName);
                    assistIntent.putExtras(bundle);
                    if (z) {
                        showDisclosure();
                    }
                    try {
                        final ActivityOptions makeCustomAnimation = ActivityOptions.makeCustomAnimation(this.mContext, R$anim.search_launch_enter, R$anim.search_launch_exit);
                        assistIntent.addFlags(268435456);
                        AsyncTask.execute(new Runnable() {
                            public void run() {
                                StringBuilder sb = new StringBuilder();
                                sb.append("start Assist Activity: ");
                                sb.append(assistIntent);
                                Log.d("AssistManager", sb.toString());
                                AssistManager.this.mContext.startActivityAsUser(assistIntent, makeCustomAnimation.toBundle(), new UserHandle(-2));
                            }
                        });
                    } catch (ActivityNotFoundException unused) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Activity not found for ");
                        sb.append(assistIntent.getAction());
                        Log.w("AssistManager", sb.toString());
                    }
                }
            }
        }
    }

    private void startVoiceInteractor(Bundle bundle) {
        Log.d("AssistManager", "start VoiceInteractor");
        this.mAssistUtils.showSessionForActiveService(bundle, 4, this.mShowCallback, null);
    }

    public void launchVoiceAssistFromKeyguard() {
        this.mAssistUtils.launchVoiceAssistFromKeyguard();
    }

    public boolean canVoiceAssistBeLaunchedFromKeyguard() {
        return this.mAssistUtils.activeServiceSupportsLaunchFromKeyguard();
    }

    public ComponentName getVoiceInteractorComponentName() {
        return this.mAssistUtils.getActiveServiceComponentName();
    }

    private boolean isVoiceSessionRunning() {
        return this.mAssistUtils.isSessionRunning();
    }

    private void maybeSwapSearchIcon(ComponentName componentName, boolean z) {
        replaceDrawable(this.mView.getOrb().getLogo(), componentName, "com.android.systemui.action_assist_icon", z);
    }

    public void replaceDrawable(ImageView imageView, ComponentName componentName, String str, boolean z) {
        Bundle bundle;
        if (componentName != null) {
            try {
                PackageManager packageManager = this.mContext.getPackageManager();
                if (z) {
                    bundle = packageManager.getServiceInfo(componentName, 128).metaData;
                } else {
                    bundle = packageManager.getActivityInfo(componentName, 128).metaData;
                }
                if (bundle != null) {
                    int i = bundle.getInt(str);
                    if (i != 0) {
                        imageView.setImageDrawable(packageManager.getResourcesForApplication(componentName.getPackageName()).getDrawable(i));
                        return;
                    }
                }
            } catch (NameNotFoundException unused) {
            } catch (NotFoundException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Failed to swap drawable from ");
                sb.append(componentName.flattenToShortString());
                Log.w("AssistManager", sb.toString(), e);
            }
        }
        imageView.setImageDrawable(null);
    }

    public ComponentName getAssistInfoForUser(int i) {
        return this.mAssistUtils.getAssistComponentForUser(i);
    }

    private ComponentName getAssistInfo() {
        return getAssistInfoForUser(KeyguardUpdateMonitor.getCurrentUser());
    }

    public void showDisclosure() {
        if (!this.mIsPowerLongPressWithAssistant && !this.mIsPowerLongPressWithGoogleAssistant) {
            Log.v("AssistManager", "showDisclosure");
            this.mAssistDisclosure.postShow();
        }
    }

    public void onLockscreenShown() {
        this.mAssistUtils.onLockscreenShown();
    }

    public int toLoggingSubType(int i) {
        return toLoggingSubType(i, this.mPhoneStateMonitor.getPhoneState());
    }

    private int toLoggingSubType(int i, int i2) {
        return ((this.mHandleController.areHandlesShowing() ^ true) | (i << 1)) | (i2 << 4) ? 1 : 0;
    }
}

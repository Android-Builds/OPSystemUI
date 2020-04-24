package com.oneplus.battery;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.internal.policy.IKeyguardStateCallback.Stub;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitor.BatteryStatus;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.R$attr;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.ScreenLifecycle.Observer;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.oneplus.battery.OpChargingAnimationController.ChargingStateChangeCallback;
import com.oneplus.plugin.OpLsState;
import com.oneplus.util.OpImageUtils;
import com.oneplus.util.OpUtils;
import java.util.ArrayList;
import java.util.function.Consumer;

public class OpChargingAnimationControllerImpl implements OpChargingAnimationController, BatteryStateChangeCallback, ConfigurationListener {
    private static boolean mPreventModeNoBackground;
    /* access modifiers changed from: private */
    public String TAG = "OpChargingAnimationControllerImpl";
    /* access modifiers changed from: private */
    public boolean isKeyguardShowing = false;
    private boolean mAnimationStarted;
    private BatteryController mBatteryController;
    /* access modifiers changed from: private */
    public boolean mBouncerShow = false;
    private final ArrayList<ChargingStateChangeCallback> mCallbacks = new ArrayList<>();
    private ConfigurationController mConfigurationController;
    private Context mContext;
    private int mCurrentAnimationState = 100;
    private boolean mFastCharging = false;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message message) {
            if (message.what == 1000) {
                OpChargingAnimationControllerImpl.this.mHandler.removeMessages(1000);
                if (OpChargingAnimationControllerImpl.this.mOpWarpChargingView != null) {
                    OpChargingAnimationControllerImpl.this.mOpWarpChargingView.releaseAsset();
                }
                Log.i(OpChargingAnimationControllerImpl.this.TAG, "releaseAsset");
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mKeyguardOn = false;
    private KeyguardStateCallback mKeyguardStateCallback;
    /* access modifiers changed from: private */
    public OpWarpChargingView mOpWarpChargingView;
    /* access modifiers changed from: private */
    public boolean mPluggedButNotUsb = false;
    /* access modifiers changed from: private */
    public boolean mPreventViewShow = false;
    private ScreenLifecycle mScreenLifecycle;
    private final Observer mScreenObserver = new Observer() {
        public void onScreenTurnedOff() {
            Log.i(OpChargingAnimationControllerImpl.this.TAG, "onScreenTurnedOff");
            if (OpChargingAnimationControllerImpl.this.mOpWarpChargingView != null) {
                OpChargingAnimationControllerImpl.this.mOpWarpChargingView.stopAnimation();
            }
        }
    };
    private int mSmallestWidth;
    private KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        public void onKeyguardVisibilityChanged(boolean z) {
            String access$200 = OpChargingAnimationControllerImpl.this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onKeyguardVisibilityChanged:");
            sb.append(z);
            Log.i(access$200, sb.toString());
            OpChargingAnimationControllerImpl.this.isKeyguardShowing = z;
            if (!OpChargingAnimationControllerImpl.this.isKeyguardShowing && OpChargingAnimationControllerImpl.this.mOpWarpChargingView != null) {
                OpChargingAnimationControllerImpl.this.mOpWarpChargingView.stopAnimation();
            }
        }

        public void onStartedGoingToSleep(int i) {
            Log.i(OpChargingAnimationControllerImpl.this.TAG, "onStartedGoingToSleep");
            if (OpChargingAnimationControllerImpl.this.mOpWarpChargingView != null) {
                OpChargingAnimationControllerImpl.this.mOpWarpChargingView.stopAnimation();
            }
        }

        public void onKeyguardBouncerChanged(boolean z) {
            OpChargingAnimationControllerImpl.this.mBouncerShow = z;
            String access$200 = OpChargingAnimationControllerImpl.this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onKeyguardBouncerChanged:");
            sb.append(OpChargingAnimationControllerImpl.this.mBouncerShow);
            Log.i(access$200, sb.toString());
            if (z && OpChargingAnimationControllerImpl.this.mOpWarpChargingView != null) {
                OpChargingAnimationControllerImpl.this.mOpWarpChargingView.stopAnimation();
            }
        }

        public void onPreventModeChanged(boolean z) {
            OpChargingAnimationControllerImpl.this.mPreventViewShow = z;
            if (z && OpChargingAnimationControllerImpl.this.mOpWarpChargingView != null) {
                OpChargingAnimationControllerImpl.this.mOpWarpChargingView.stopAnimation();
            }
        }

        public void onRefreshBatteryInfo(BatteryStatus batteryStatus) {
            OpChargingAnimationControllerImpl opChargingAnimationControllerImpl = OpChargingAnimationControllerImpl.this;
            int i = batteryStatus.plugged;
            boolean z = true;
            if (!(i == 1 || i == 4)) {
                z = false;
            }
            opChargingAnimationControllerImpl.mPluggedButNotUsb = z;
            String access$200 = OpChargingAnimationControllerImpl.this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onRefreshBatteryInfo / mPluggedButNotUsb:");
            sb.append(OpChargingAnimationControllerImpl.this.mPluggedButNotUsb);
            sb.append(" / status:");
            sb.append(batteryStatus);
            Log.i(access$200, sb.toString());
            if (OpChargingAnimationControllerImpl.this.mOpWarpChargingView != null && OpChargingAnimationControllerImpl.this.mKeyguardOn && OpChargingAnimationControllerImpl.this.mPluggedButNotUsb) {
                OpChargingAnimationControllerImpl.this.prepareAnimationResource();
            } else if (OpChargingAnimationControllerImpl.this.mOpWarpChargingView != null) {
                OpChargingAnimationControllerImpl.this.mOpWarpChargingView.stopAnimation();
            }
        }
    };
    private Bitmap mWallpaper = null;
    private boolean mWarpFastCharging = false;
    private FrameLayout mWrapChargingLayout;

    private class KeyguardStateCallback extends Stub {
        public void onFingerprintStateChange(boolean z, int i, int i2, int i3) {
        }

        public void onHasLockscreenWallpaperChanged(boolean z) {
        }

        public void onInputRestrictedStateChanged(boolean z) {
        }

        public void onPocketModeActiveChanged(boolean z) {
        }

        public void onSimSecureStateChanged(boolean z) {
        }

        public void onTrustedChanged(boolean z) {
        }

        private KeyguardStateCallback() {
        }

        public void onShowingStateChanged(boolean z) {
            String access$200 = OpChargingAnimationControllerImpl.this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onShowingStateChanged ");
            sb.append(z);
            Log.d(access$200, sb.toString());
            OpChargingAnimationControllerImpl.this.mKeyguardOn = z;
            if (OpChargingAnimationControllerImpl.this.mKeyguardOn && OpChargingAnimationControllerImpl.this.mPluggedButNotUsb) {
                OpChargingAnimationControllerImpl.this.prepareAnimationResource();
            } else if (OpChargingAnimationControllerImpl.this.mOpWarpChargingView != null && !OpChargingAnimationControllerImpl.this.mKeyguardOn) {
                OpChargingAnimationControllerImpl.this.mOpWarpChargingView.stopAnimation();
                OpChargingAnimationControllerImpl.this.releaseAnimationResource();
            }
        }

        public void onDisabledStateChanged(boolean z) {
            String access$200 = OpChargingAnimationControllerImpl.this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onDisabledStateChanged");
            sb.append(z);
            Log.d(access$200, sb.toString());
        }
    }

    public OpChargingAnimationControllerImpl(Context context) {
        this.mContext = context;
        this.mScreenLifecycle = (ScreenLifecycle) Dependency.get(ScreenLifecycle.class);
        Log.i(this.TAG, "OpChargingAnimationControllerImpl init");
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
        if (OpUtils.SUPPORT_WARP_CHARGING) {
            registerReceiver();
        }
        this.mConfigurationController = (ConfigurationController) Dependency.get(ConfigurationController.class);
        this.mConfigurationController.addCallback(this);
        this.mKeyguardStateCallback = new KeyguardStateCallback();
    }

    public void registerReceiver() {
        this.mBatteryController.addCallback(this);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateCallback);
        this.mScreenLifecycle.addObserver(this.mScreenObserver);
    }

    public void init(KeyguardViewMediator keyguardViewMediator) {
        Log.i(this.TAG, "init");
        genOpWarpChargingView();
        initOPWarpCharging();
        keyguardViewMediator.addStateMonitorCallback(this.mKeyguardStateCallback);
    }

    public void addCallback(ChargingStateChangeCallback chargingStateChangeCallback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.add(chargingStateChangeCallback);
        }
    }

    public void removeCallback(ChargingStateChangeCallback chargingStateChangeCallback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.remove(chargingStateChangeCallback);
        }
    }

    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        OpWarpChargingView opWarpChargingView = this.mOpWarpChargingView;
        if (opWarpChargingView != null) {
            opWarpChargingView.onBatteryLevelChanged(i, z, z2);
        }
    }

    public void onFastChargeChanged(int i) {
        if (Build.DEBUG_ONEPLUS) {
            String str = this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onFastChargeChanged:");
            sb.append(i);
            Log.i(str, sb.toString());
        }
        boolean isWarpCharging = this.mBatteryController.isWarpCharging(i);
        if (isWarpCharging != this.mWarpFastCharging) {
            this.mWarpFastCharging = isWarpCharging;
            boolean z = this.mScreenLifecycle.getScreenState() == 2;
            if (this.mWarpFastCharging && this.isKeyguardShowing && z && !this.mBouncerShow && !this.mPreventViewShow) {
                updateScrim();
                this.mOpWarpChargingView.startAnimation();
            }
        }
    }

    private void updateScrim() {
        StatusBar phoneStatusBar = OpLsState.getInstance().getPhoneStatusBar();
        if (this.mContext == null || phoneStatusBar == null) {
            Log.d(this.TAG, "can't updateScrim");
            return;
        }
        int scrimColor = this.mWallpaper != null ? phoneStatusBar.getScrimColor() : 0;
        String str = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("updateScrim, ");
        sb.append(scrimColor);
        Log.d(str, sb.toString());
        this.mOpWarpChargingView.updaetScrimColor(scrimColor);
        this.mOpWarpChargingView.updateColors(Utils.getColorAttrDefaultColor(this.mContext, R$attr.wallpaperTextColor));
    }

    public void animationStart(int i) {
        this.mAnimationStarted = true;
        Log.d(this.TAG, "animationStart");
        OpUtils.safeForeach(this.mCallbacks, new Consumer(i) {
            private final /* synthetic */ int f$0;

            {
                this.f$0 = r1;
            }

            public final void accept(Object obj) {
                ((ChargingStateChangeCallback) obj).onWarpCharingAnimationStart(this.f$0);
            }
        });
        if (this.mWallpaper == null) {
            mPreventModeNoBackground = true;
            OpLsState.getInstance().getPhoneStatusBar().setPanelViewAlpha(0.0f, true, -1);
        }
    }

    public void animationEnd(int i) {
        this.mAnimationStarted = false;
        Log.d(this.TAG, "animationEnd");
        OpUtils.safeForeach(this.mCallbacks, new Consumer(i) {
            private final /* synthetic */ int f$0;

            {
                this.f$0 = r1;
            }

            public final void accept(Object obj) {
                ((ChargingStateChangeCallback) obj).onWarpCharingAnimationEnd(this.f$0);
            }
        });
        if (mPreventModeNoBackground && !this.mPreventViewShow) {
            OpLsState.getInstance().getPhoneStatusBar().setPanelViewAlpha(1.0f, true, -1);
            mPreventModeNoBackground = false;
        }
    }

    /* access modifiers changed from: private */
    public void prepareAnimationResource() {
        OpWarpChargingView opWarpChargingView = this.mOpWarpChargingView;
        if (opWarpChargingView != null) {
            opWarpChargingView.prepareAsset();
        }
    }

    /* access modifiers changed from: private */
    public void releaseAnimationResource() {
        Message message = new Message();
        message.what = 1000;
        this.mHandler.removeMessages(1000);
        this.mHandler.sendMessageDelayed(message, 1000);
    }

    private OpWarpChargingView genOpWarpChargingView() {
        Log.i(this.TAG, "genOpWarpChargingView");
        if (this.mOpWarpChargingView != null) {
            if (Build.DEBUG_ONEPLUS) {
                String str = this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append(" mOpWarpChargingView != null / mOpWarpChargingView.getParent():");
                sb.append(this.mOpWarpChargingView.getParent());
                Log.i(str, sb.toString());
            }
            ((ViewGroup) this.mOpWarpChargingView.getParent()).removeView(this.mOpWarpChargingView);
        }
        this.mOpWarpChargingView = (OpWarpChargingView) LayoutInflater.from(this.mContext).inflate(R$layout.op_warp_charging_animation_view, null);
        return this.mOpWarpChargingView;
    }

    private void initOPWarpCharging() {
        this.mWrapChargingLayout = (FrameLayout) OpLsState.getInstance().getContainer().findViewById(R$id.wrap_charging_layout);
        this.mWrapChargingLayout.addView(this.mOpWarpChargingView);
        this.mOpWarpChargingView.setChargingAnimationController(this);
        StatusBar phoneStatusBar = OpLsState.getInstance().getPhoneStatusBar();
        if (phoneStatusBar != null) {
            onWallpaperChange(phoneStatusBar.getLockscreenWallpaper());
        }
    }

    public void onWallpaperChange(Bitmap bitmap) {
        String str = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("onWallpaperChange: bitmap:");
        sb.append(bitmap != null);
        Log.d(str, sb.toString());
        Bitmap bitmap2 = null;
        if (bitmap != null) {
            bitmap2 = OpImageUtils.computeCustomBackgroundBounds(this.mContext, bitmap);
        }
        this.mWallpaper = bitmap2;
        OpWarpChargingView opWarpChargingView = this.mOpWarpChargingView;
        if (opWarpChargingView != null) {
            opWarpChargingView.setBackgroundWallpaper(this.mWallpaper);
        }
    }

    public boolean isAnimationStarted() {
        return this.mAnimationStarted;
    }

    public void onConfigChanged(Configuration configuration) {
        String str = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("onConfigChanged / newConfig:");
        sb.append(configuration);
        Log.d(str, sb.toString());
        if (this.mSmallestWidth != configuration.smallestScreenWidthDp) {
            genOpWarpChargingView();
            initOPWarpCharging();
            this.mSmallestWidth = configuration.smallestScreenWidthDp;
        }
    }
}

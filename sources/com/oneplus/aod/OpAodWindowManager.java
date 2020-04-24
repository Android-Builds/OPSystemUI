package com.oneplus.aod;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.IPowerManager.Stub;
import android.os.ServiceManager;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.PathInterpolator;
import android.widget.RelativeLayout;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.R$id;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.OpUtils;

public class OpAodWindowManager {
    private View mAodContainer;
    private RelativeLayout mAodWindowView;
    /* access modifiers changed from: private */
    public Context mContext;
    private boolean mDozing;
    private Handler mHandler = new Handler();
    /* access modifiers changed from: private */
    public boolean mIsWakeAndUnlock;
    private boolean mIsWindowRemoved;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private OpLsState mLSState;
    private BroadcastReceiver mMdmReadyReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null && intent.getAction().equals("com.oneplus.intent.action.mdm_provider_ready")) {
                Log.i("AodWindowManager", "Receive MDM provider ready");
                OpAodWindowManager.this.reportMDMEvent();
            }
        }
    };
    private IPowerManager mPm;
    private final Runnable mRemoveWindow = new Runnable() {
        public void run() {
            OpAodWindowManager.this.removeAodWindow();
        }
    };
    /* access modifiers changed from: private */
    public SettingObserver mSettingsOberver = new SettingObserver();
    private StatusBarWindowController mStatusBarWindowController;
    /* access modifiers changed from: private */
    public BiometricUnlockController mUnlockController;
    private final KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        public void onUserSwitchComplete(int i) {
            OpAodUtils.updateDozeSettings(OpAodWindowManager.this.mContext, i);
        }

        public void onStartedWakingUp() {
            super.onStartedWakingUp();
            if (OpAodWindowManager.this.mUnlockController != null) {
                OpAodWindowManager opAodWindowManager = OpAodWindowManager.this;
                opAodWindowManager.mIsWakeAndUnlock = opAodWindowManager.mUnlockController.isWakeAndUnlock();
                if (!OpAodWindowManager.this.mIsWakeAndUnlock) {
                    OpAodWindowManager opAodWindowManager2 = OpAodWindowManager.this;
                    opAodWindowManager2.mWakingUpReason = opAodWindowManager2.mKeyguardUpdateMonitor.getWakingUpReason();
                    Log.d("AodWindowManager", "onStartedWakingUp");
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public String mWakingUpReason = null;
    private WindowManager mWm;

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }

        /* access modifiers changed from: 0000 */
        public void observe() {
            ContentResolver contentResolver = OpAodWindowManager.this.mContext.getContentResolver();
            contentResolver.registerContentObserver(Secure.getUriFor("notification_wake_enabled"), false, OpAodWindowManager.this.mSettingsOberver, -1);
            contentResolver.registerContentObserver(Secure.getUriFor("aod_display_mode"), false, OpAodWindowManager.this.mSettingsOberver, -1);
            contentResolver.registerContentObserver(System.getUriFor("prox_wake_enabled"), false, OpAodWindowManager.this.mSettingsOberver, -1);
            contentResolver.registerContentObserver(System.getUriFor("oem_acc_blackscreen_gestrue_enable"), false, OpAodWindowManager.this.mSettingsOberver, -1);
        }

        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            if (Secure.getUriFor("notification_wake_enabled").equals(uri)) {
                OpAodUtils.updateNotificationWakeState(OpAodWindowManager.this.mContext, currentUser);
            } else if (Secure.getUriFor("aod_display_mode").equals(uri)) {
                OpAodUtils.updateAlwaysOnState(OpAodWindowManager.this.mContext, currentUser);
            } else if (System.getUriFor("prox_wake_enabled").equals(uri)) {
                OpAodUtils.updateMotionAwakeState(OpAodWindowManager.this.mContext, currentUser);
            } else if (System.getUriFor("oem_acc_blackscreen_gestrue_enable").equals(uri)) {
                OpAodUtils.updateSingleTapAwakeState(OpAodWindowManager.this.mContext, currentUser);
            }
        }
    }

    public OpAodWindowManager(Context context, RelativeLayout relativeLayout) {
        this.mContext = context;
        this.mAodWindowView = relativeLayout;
        this.mWm = (WindowManager) this.mContext.getSystemService("window");
        this.mPm = Stub.asInterface(ServiceManager.getService("power"));
        this.mLSState = OpLsState.getInstance();
        this.mSettingsOberver.observe();
        this.mStatusBarWindowController = (StatusBarWindowController) Dependency.get(StatusBarWindowController.class);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mKeyguardUpdateMonitor.registerCallback(this.mUpdateCallback);
        OpAodUtils.init(context, KeyguardUpdateMonitor.getCurrentUser());
        this.mAodContainer = relativeLayout.findViewById(R$id.op_aod_container);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.oneplus.intent.action.mdm_provider_ready");
        this.mContext.registerReceiver(this.mMdmReadyReceiver, intentFilter);
    }

    /* access modifiers changed from: private */
    public void reportMDMEvent() {
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        ContentResolver contentResolver = this.mContext.getContentResolver();
        boolean z = false;
        boolean z2 = OpAodUtils.isMotionAwakeOn() || OpAodUtils.isSingleTapEnabled();
        boolean z3 = 1 == System.getIntForUser(contentResolver, "aod_smart_display_cur_state", 1, currentUser);
        String str = "switch";
        String str2 = "1";
        String str3 = "X9HFK50WT7";
        String str4 = "Smart_AOD";
        if (z2) {
            boolean z4 = 1 == System.getIntForUser(contentResolver, "aod_smart_display_music_info_enabled", 1, currentUser);
            if (1 == System.getIntForUser(contentResolver, "aod_smart_display_calendar_enabled", 1, currentUser)) {
                z = true;
            }
            String str5 = "0";
            OpMdmLogger.log(str4, str, z3 ? "2" : str5, str3);
            OpMdmLogger.log(str4, "Media", z4 ? str2 : str5, str3);
            if (z) {
                str5 = str2;
            }
            OpMdmLogger.log(str4, "Calendar", str5, str3);
            return;
        }
        OpMdmLogger.log(str4, str, str2, str3);
    }

    public void onWakingAndUnlocking() {
        this.mWakingUpReason = this.mKeyguardUpdateMonitor.getWakingUpReason();
        Log.d("AodWindowManager", "onWakingAndUnlocking");
    }

    public void startDozing() {
        String str = "AodWindowManager";
        Log.d(str, "startDozing");
        if (!this.mDozing) {
            if (this.mUnlockController == null) {
                this.mUnlockController = this.mLSState.getBiometricUnlockController();
            }
            this.mDozing = true;
            this.mIsWakeAndUnlock = false;
            this.mWakingUpReason = null;
            this.mAodWindowView.setAlpha(1.0f);
            this.mAodContainer.setTranslationY(0.0f);
            this.mAodContainer.setAlpha(1.0f);
            if (this.mAodWindowView.isAttachedToWindow()) {
                this.mHandler.removeCallbacks(this.mRemoveWindow);
                Log.d(str, "mAodView has already been added to window, do not add it again.");
            } else {
                this.mWm.addView(this.mAodWindowView, getAodViewLayoutParams());
            }
            this.mIsWindowRemoved = false;
            this.mAodWindowView.setSystemUiVisibility(this.mAodWindowView.getSystemUiVisibility() | 1792);
            this.mAodWindowView.setVisibility(0);
        }
    }

    public void stopDozing() {
        if (this.mDozing) {
            int i = 0;
            this.mDozing = false;
            boolean z = !this.mStatusBarWindowController.isShowingLiveWallpaper(true);
            StringBuilder sb = new StringBuilder();
            sb.append("handleStopDozing: mIsWakeAndUnlock = ");
            sb.append(this.mIsWakeAndUnlock);
            sb.append(", hasLockWallpaper = ");
            sb.append(z);
            String sb2 = sb.toString();
            String str = "AodWindowManager";
            Log.d(str, sb2);
            float f = 0.0f;
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("handleStopDozing mWakingUpReason ");
                sb3.append(this.mWakingUpReason);
                Log.d(str, sb3.toString());
            }
            if (OpUtils.isCustomFingerprint()) {
                String str2 = this.mWakingUpReason;
                if (str2 != null) {
                    String str3 = "com.android.systemui:FailedAttempts";
                    if (!str2.contains("FINGERPRINT") && this.mWakingUpReason.equals(str3)) {
                        i = 90;
                    }
                    if (this.mWakingUpReason.equals(str3)) {
                        f = 1.0f;
                    }
                }
            } else {
                String str4 = this.mWakingUpReason;
                if (str4 != null && str4.contains("FINGERPRINT_WALLPAPER")) {
                    i = 100;
                }
            }
            this.mAodWindowView.setAlpha(f);
            StringBuilder sb4 = new StringBuilder();
            sb4.append("aod remove window delay:");
            sb4.append(i);
            Log.d(str, sb4.toString());
            if (i > 0) {
                this.mHandler.postDelayed(this.mRemoveWindow, (long) i);
            } else {
                removeAodWindow();
            }
        }
    }

    /* access modifiers changed from: private */
    public void removeAodWindow() {
        if (!this.mIsWindowRemoved) {
            Log.d("AodWindowManager", "aod remove window");
            this.mWm.removeViewImmediate(this.mAodWindowView);
            this.mIsWindowRemoved = true;
        }
    }

    private LayoutParams getAodViewLayoutParams() {
        LayoutParams layoutParams = new LayoutParams();
        layoutParams.type = 2303;
        layoutParams.privateFlags = 16;
        layoutParams.layoutInDisplayCutoutMode = 1;
        if (VERSION.SDK_INT >= 27) {
            layoutParams.privateFlags |= 2097152;
        }
        layoutParams.flags = 1280;
        layoutParams.format = -2;
        layoutParams.width = -1;
        layoutParams.height = -1;
        layoutParams.gravity = 17;
        layoutParams.screenOrientation = 1;
        layoutParams.setTitle("OPAod");
        layoutParams.softInputMode = 3;
        return layoutParams;
    }

    public void unregisterCallback() {
        KeyguardUpdateMonitor keyguardUpdateMonitor = this.mKeyguardUpdateMonitor;
        if (keyguardUpdateMonitor != null) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mUpdateCallback;
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitor.removeCallback(keyguardUpdateMonitorCallback);
            }
        }
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsOberver);
    }

    public AnimatorSet getAodDisappearAnimation() {
        AnimatorSet animatorSet = new AnimatorSet();
        PathInterpolator pathInterpolator = new PathInterpolator(0.6f, 0.0f, 0.6f, 1.0f);
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.mAodContainer, View.TRANSLATION_Y, new float[]{0.0f, -100.0f});
        ofFloat.setDuration(300);
        ofFloat.setInterpolator(pathInterpolator);
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.mAodContainer, View.ALPHA, new float[]{1.0f, 0.0f});
        ofFloat2.setDuration(300);
        ofFloat2.setInterpolator(pathInterpolator);
        animatorSet.playTogether(new Animator[]{ofFloat, ofFloat2});
        animatorSet.addListener(new AnimatorListenerAdapter() {
            boolean mCancelled;

            public void onAnimationStart(Animator animator) {
                this.mCancelled = false;
                Log.i("AodWindowManager", "AodDisappearAnimation onAnimationStart:");
            }

            public void onAnimationCancel(Animator animator) {
                Log.i("AodWindowManager", "AodDisappearAnimation onAnimationCancel:");
                this.mCancelled = true;
            }

            public void onAnimationEnd(Animator animator) {
                Log.i("AodWindowManager", "AodDisappearAnimation onAnimationEnd:");
            }
        });
        return animatorSet;
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    public boolean isWakingAndUnlockByFP() {
        String str = this.mWakingUpReason;
        return str != null && str.contains("FINGERPRINT");
    }
}

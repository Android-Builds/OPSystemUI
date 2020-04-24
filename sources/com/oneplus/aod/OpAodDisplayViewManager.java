package com.oneplus.aod;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.biometrics.BiometricSourceType;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeHost.Callback;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.AmbientPulseManager.OnAmbientChangedListener;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.StatusBar;
import com.oneplus.aod.slice.OpSliceManager;
import com.oneplus.util.OpUtils;
import java.io.PrintWriter;

public class OpAodDisplayViewManager implements OnAmbientChangedListener {
    private final AmbientPulseManager mAmbientPulseManager = ((AmbientPulseManager) Dependency.get(AmbientPulseManager.class));
    /* access modifiers changed from: private */
    public OpAodMain mAodMainView;
    /* access modifiers changed from: private */
    public OpClockViewCtrl mClockViewCtrl;
    private ViewGroup mContainer;
    /* access modifiers changed from: private */
    public Context mContext;
    private DozeHost mDozeHost;
    private Handler mHandler = new Handler();
    private Callback mHostCallback = new Callback() {
        public void onFingerprintPoke() {
        }

        public void onPowerSaveChanged(boolean z) {
            Log.d("AodDisplayViewManager", "onPowerSaveChanged");
        }

        public void onThreeKeyChanged(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("onThreeKeyChanged: ");
            sb.append(i);
            Log.d("AodDisplayViewManager", sb.toString());
            OpAodDisplayViewManager.this.mThreeKeyView.onThreeKeyChanged(i);
        }

        public void onSingleTap() {
            Log.d("AodDisplayViewManager", "onSingleTap");
        }
    };
    /* access modifiers changed from: private */
    public OpFpAodIndicationText mIndication;
    public boolean mIsPlayFingerprintUnlockAnimation;
    private boolean mIsPress = false;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private int mLayoutDir;
    /* access modifiers changed from: private */
    public OpAodLightEffectContainer mLightEffectContainer;
    /* access modifiers changed from: private */
    public OpAodNotificationIconAreaController mNotificationIconCtrl;
    private PowerManager mPowerManager;
    private View mScrimView;
    private final SettingObserver mSettingObserver = new SettingObserver();
    private OpSingleNotificationView mSingleNotificationView;
    /* access modifiers changed from: private */
    public OpSliceManager mSliceManager;
    private int mStatus = 1;
    private StatusBar mStatusbar;
    /* access modifiers changed from: private */
    public OpAodThreeKeyStatusView mThreeKeyView;
    private final KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        public void onTimeChanged() {
            OpAodDisplayViewManager.this.mClockViewCtrl.onTimeChanged();
            OpAodDisplayViewManager.this.mSliceManager.onTimeChanged();
        }

        public void onUserSwitchComplete(int i) {
            OpAodDisplayViewManager.this.mClockViewCtrl.onUserSwitchComplete(i);
            OpAodDisplayViewManager.this.mAodMainView.setClockStyle(OpAodDisplayViewManager.this.mClockViewCtrl.getClockStyle());
            OpAodDisplayViewManager.this.mAodMainView.onUserSwitchComplete(i);
            OpAodDisplayViewManager.this.mNotificationIconCtrl.onUserSwitchComplete(i);
            OpAodDisplayViewManager.this.mSliceManager.onUserSwitchComplete(i);
        }

        public void onUserInfoChanged(int i) {
            super.onUserInfoChanged(i);
            OpAodDisplayViewManager.this.mClockViewCtrl.onUserInfoChanged(i);
        }

        public void onDreamingStateChanged(boolean z) {
            super.onDreamingStateChanged(z);
            OpAodDisplayViewManager.this.mClockViewCtrl.onDreamingStateChanged(z);
        }

        public void onScreenTurnedOn() {
            super.onScreenTurnedOn();
            OpAodDisplayViewManager.this.mClockViewCtrl.onScreenTurnedOn();
        }

        public void onScreenTurnedOff() {
            super.onScreenTurnedOff();
            if (OpAodUtils.isCustomFingerprint()) {
                OpAodDisplayViewManager.this.mIndication.resetState();
            }
        }

        public void onFinishedGoingToSleep(int i) {
            super.onFinishedGoingToSleep(i);
            if (OpAodUtils.isCustomFingerprint()) {
                OpAodDisplayViewManager.this.mIndication.updateFPIndicationText(false, null);
            }
        }

        public void onBiometricHelp(int i, String str, BiometricSourceType biometricSourceType) {
            super.onBiometricHelp(i, str, biometricSourceType);
            if (OpAodUtils.isCustomFingerprint() && biometricSourceType == BiometricSourceType.FINGERPRINT) {
                OpAodDisplayViewManager.this.mIndication.updateFPIndicationText(true, str);
            }
        }

        public void onBiometricError(int i, String str, BiometricSourceType biometricSourceType) {
            super.onBiometricError(i, str, biometricSourceType);
            if (OpAodUtils.isCustomFingerprint() && biometricSourceType == BiometricSourceType.FINGERPRINT) {
                OpAodDisplayViewManager.this.mIndication.resetState();
            }
        }

        public void onFingerprintAcquired(int i) {
            super.onFingerprintAcquired(i);
            if (OpAodUtils.isCustomFingerprint() && i == 6) {
                OpAodDisplayViewManager.this.mIndication.resetState();
            }
        }
    };

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }

        public void onChange(boolean z, Uri uri, int i) {
            super.onChange(z, uri, i);
            if (uri.equals(Secure.getUriFor("aod_clock_style"))) {
                OpAodUtils.checkAodStyle(OpAodDisplayViewManager.this.mContext, i);
                OpAodDisplayViewManager.this.mClockViewCtrl.updateClockDB();
                OpAodDisplayViewManager.this.mAodMainView.setClockStyle(OpAodDisplayViewManager.this.mClockViewCtrl.getClockStyle());
            } else if (uri.equals(Secure.getUriFor("aod_display_text"))) {
                OpAodDisplayViewManager.this.mClockViewCtrl.updateDisplayTextDB();
            } else {
                String str = "op_custom_horizon_light_animation_style";
                if (uri.equals(System.getUriFor(str))) {
                    OpAodDisplayViewManager.this.mLightEffectContainer.setLightIndex(System.getIntForUser(OpAodDisplayViewManager.this.mContext.getContentResolver(), str, 0, -2));
                }
            }
        }
    }

    private String getStateString(int i) {
        return i != 0 ? i != 1 ? i != 2 ? i != 3 ? "unknown" : "threekey" : "notification" : "main" : "none";
    }

    public OpAodDisplayViewManager(Context context, ViewGroup viewGroup, DozeHost dozeHost, StatusBar statusBar) {
        this.mContext = context;
        this.mDozeHost = dozeHost;
        this.mDozeHost.addCallback(this.mHostCallback);
        this.mStatusbar = statusBar;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mClockViewCtrl = new OpClockViewCtrl(context, viewGroup);
        initViews(viewGroup);
        this.mSliceManager = new OpSliceManager(this.mContext, viewGroup.findViewById(R$id.slice_info_container));
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mKeyguardUpdateMonitor.registerCallback(this.mUpdateCallback);
        this.mNotificationIconCtrl = new OpAodNotificationIconAreaController(context, this.mAodMainView);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("aod_clock_style"), false, this.mSettingObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("aod_display_text"), false, this.mSettingObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("op_custom_horizon_light_animation_style"), false, this.mSettingObserver, -1);
        this.mAmbientPulseManager.addListener(this);
    }

    private void initViews(ViewGroup viewGroup) {
        this.mContainer = (ViewGroup) viewGroup.findViewById(R$id.op_aod_container);
        this.mAodMainView = (OpAodMain) viewGroup.findViewById(R$id.op_aod_view);
        this.mAodMainView.setClockStyle(this.mClockViewCtrl.getClockStyle());
        this.mSingleNotificationView = (OpSingleNotificationView) viewGroup.findViewById(R$id.single_notification_view);
        this.mLayoutDir = viewGroup.getLayoutDirection();
        this.mThreeKeyView = (OpAodThreeKeyStatusView) viewGroup.findViewById(R$id.three_key_view);
        this.mScrimView = viewGroup.findViewById(R$id.aod_scrim);
        this.mLightEffectContainer = (OpAodLightEffectContainer) viewGroup.findViewById(R$id.notification_animation_view);
        this.mLightEffectContainer.setLightIndex(System.getIntForUser(this.mContext.getContentResolver(), "op_custom_horizon_light_animation_style", 0, 0));
        this.mIndication = (OpFpAodIndicationText) viewGroup.findViewById(R$id.op_aod_fp_indication_text);
        this.mIndication.init(this, this.mHandler);
        updateAodMainParam();
    }

    public void onDensityOrFontScaleChanged(ViewGroup viewGroup) {
        initViews(viewGroup);
        this.mClockViewCtrl.initViews(viewGroup);
        this.mSliceManager.initViews(viewGroup.findViewById(R$id.slice_info_container));
        this.mNotificationIconCtrl.initViews(this.mAodMainView);
        this.mAodMainView.onDensityOrFontScaleChanged();
        updateIndicationTextSize(0, this.mContext.getResources().getDimensionPixelSize(R$dimen.oneplus_contorl_text_size_body1));
    }

    private void updateAodMainParam() {
        int i;
        if (OpUtils.isSupportResolutionSwitch(this.mContext)) {
            LayoutParams layoutParams = (LayoutParams) this.mAodMainView.getLayoutParams();
            if (layoutParams != null) {
                Resources resources = this.mContext.getResources();
                if (OpUtils.is2KResolution()) {
                    i = R$dimen.op_biometric_icon_normal_location_y_2k;
                } else {
                    i = R$dimen.op_biometric_icon_normal_location_y_1080p;
                }
                layoutParams.height = resources.getDimensionPixelSize(i);
                if (OpUtils.isCutoutHide(this.mContext)) {
                    layoutParams.height -= OpUtils.getCutoutPathdataHeight(this.mContext);
                }
                this.mAodMainView.setLayoutParams(layoutParams);
            }
        }
    }

    public void onConfigChanged(Configuration configuration) {
        int layoutDirection = configuration.getLayoutDirection();
        if (this.mLayoutDir != layoutDirection) {
            this.mAodMainView.updateRTL(layoutDirection);
            this.mSingleNotificationView.updateRTL(layoutDirection);
            this.mLayoutDir = layoutDirection;
        }
    }

    public void updateForPulseReason(int i) {
        String str = "AodDisplayViewManager";
        if (!this.mStatusbar.isDozingCustom()) {
            Log.d(str, "setState: don't set view if not dozing");
        } else if (this.mPowerManager.isInteractive()) {
            Log.d(str, "setState: don't set view if waking up");
        } else {
            int i2 = 0;
            if (i == 3 || i == 12) {
                i2 = 1;
            } else if (i == 1) {
                i2 = 2;
            } else if (i == 10) {
                i2 = 3;
            } else if (i == 13) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("setState=");
            sb.append(getStateString(i2));
            sb.append(", from=");
            sb.append(getStateString(this.mStatus));
            Log.d(str, sb.toString());
            if (this.mStatus != i2) {
                this.mStatus = i2;
                if (this.mStatus == 1) {
                    this.mSliceManager.onInitiativePulse();
                }
                updateView();
            }
        }
    }

    public void resetStatus() {
        Log.d("AodDisplayViewManager", "resetStatus");
        this.mStatus = 0;
        this.mIsPress = false;
        updateView();
    }

    private void updateView() {
        StringBuilder sb = new StringBuilder();
        sb.append("updateView: state = ");
        sb.append(getStateString(this.mStatus));
        sb.append(" mIsPlayFingerprintUnlockAnimation:");
        sb.append(this.mIsPlayFingerprintUnlockAnimation);
        sb.append(" mIsPress:");
        sb.append(this.mIsPress);
        Log.d("AodDisplayViewManager", sb.toString());
        if (this.mStatus == 0) {
            this.mScrimView.setAlpha(1.0f);
            this.mAodMainView.setVisibility(0);
            this.mSingleNotificationView.setVisibility(4);
            this.mThreeKeyView.setVisibility(4);
            this.mLightEffectContainer.resetNotificationAnimView();
        } else if (this.mIsPlayFingerprintUnlockAnimation || this.mIsPress) {
            this.mContainer.setVisibility(4);
            this.mScrimView.setAlpha(0.0f);
            this.mAodMainView.setVisibility(4);
            this.mSingleNotificationView.setVisibility(4);
            this.mThreeKeyView.setVisibility(4);
        } else {
            this.mContainer.setVisibility(0);
            this.mScrimView.setAlpha(0.3f);
            int i = this.mStatus;
            if (i == 1) {
                if (!OpAodUtils.isAlwaysOnEnabled() || (OpAodUtils.isAlwaysOnEnabled() && OpAodUtils.isAlwaysOnEnabledWithTimer())) {
                    this.mAodMainView.setVisibility(0);
                } else {
                    this.mAodMainView.setVisibility(4);
                }
                this.mSingleNotificationView.setVisibility(4);
                this.mThreeKeyView.setVisibility(4);
            } else if (i == 2) {
                this.mAodMainView.setVisibility(4);
                this.mSingleNotificationView.setVisibility(0);
                this.mThreeKeyView.setVisibility(4);
            } else if (i == 3) {
                this.mAodMainView.setVisibility(4);
                this.mSingleNotificationView.setVisibility(4);
                this.mThreeKeyView.setVisibility(0);
            }
            if (OpAodUtils.isNotificationLightEnabled()) {
                if (this.mStatus == 2) {
                    this.mLightEffectContainer.showLight();
                } else {
                    this.mLightEffectContainer.resetNotificationAnimView();
                }
            }
        }
    }

    public void onFingerPressed(boolean z) {
        this.mIsPress = z;
        updateView();
    }

    public void onPlayFingerprintUnlockAnimation(boolean z) {
        Log.d("AodDisplayViewManager", "onPlayFingerprintUnlockAnimation");
        this.mIsPlayFingerprintUnlockAnimation = z;
        updateView();
    }

    public OpAodNotificationIconAreaController getAodNotificationIconCtrl() {
        return this.mNotificationIconCtrl;
    }

    public void handleUserUnlocked() {
        this.mSliceManager.onTimeChanged();
    }

    public void startDozing() {
        this.mClockViewCtrl.startDozing();
        this.mSliceManager.setListening(true);
    }

    public void stopDozing() {
        this.mSliceManager.setListening(false);
    }

    public void onAmbientStateChanged(NotificationEntry notificationEntry, boolean z) {
        this.mSingleNotificationView.onNotificationHeadsUp(notificationEntry);
    }

    public boolean playAodWakingUpAnimation() {
        return this.mStatus != 0;
    }

    public void dump(PrintWriter printWriter) {
        this.mSliceManager.dump(printWriter);
    }

    public void updateIndicationTextSize(final int i, final int i2) {
        this.mHandler.post(new Runnable() {
            public void run() {
                if (OpAodUtils.isCustomFingerprint()) {
                    OpAodDisplayViewManager.this.mIndication.setTextSize(i, (float) i2);
                }
            }
        });
    }
}

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.biometrics.BiometricSourceType;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.R$attr;
import com.android.systemui.R$dimen;
import com.android.systemui.R$string;
import com.android.systemui.dock.DockManager;
import com.android.systemui.dock.DockManager.DockEventListener;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardMonitor.Callback;
import com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener;
import com.oneplus.systemui.statusbar.phone.OpLockIcon;

public class LockIcon extends OpLockIcon implements OnUserInfoChangedListener, StateListener, ConfigurationListener, OnUnlockMethodChangedListener {
    private final AccessibilityController mAccessibilityController;
    private boolean mBouncerVisible;
    private final ConfigurationController mConfigurationController;
    private int mDensity;
    private final DockEventListener mDockEventListener = new DockEventListener() {
    };
    private final DockManager mDockManager;
    private float mDozeAmount;
    private boolean mDozing;
    private int mIconColor;
    private boolean mIsFaceUnlockState;
    /* access modifiers changed from: private */
    public final KeyguardMonitor mKeyguardMonitor;
    private final Callback mKeyguardMonitorCallback = new Callback() {
        public void onKeyguardShowingChanged() {
            LockIcon lockIcon = LockIcon.this;
            lockIcon.mKeyguardShowing = lockIcon.mKeyguardMonitor.isShowing();
            LockIcon.this.update(false);
        }
    };
    /* access modifiers changed from: private */
    public boolean mKeyguardShowing;
    /* access modifiers changed from: private */
    public final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private boolean mLastScreenOn;
    private int mLastState = 0;
    private final Handler mMainHandler;
    private boolean mPulsing;
    /* access modifiers changed from: private */
    public boolean mScreenOn;
    private boolean mShowingLaunchAffordance;
    /* access modifiers changed from: private */
    public boolean mSimLocked;
    private final StatusBarStateController mStatusBarStateController;
    private boolean mTransientBiometricsError;
    private final UnlockMethodCache mUnlockMethodCache;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onScreenTurnedOn() {
            LockIcon.this.mScreenOn = true;
            LockIcon.this.opSetScreenOn(true);
            LockIcon.this.update();
        }

        public void onScreenTurnedOff() {
            LockIcon.this.mScreenOn = false;
            LockIcon.this.opSetScreenOn(false);
            LockIcon.this.update();
        }

        public void onSimStateChanged(int i, int i2, State state) {
            boolean access$400 = LockIcon.this.mSimLocked;
            LockIcon lockIcon = LockIcon.this;
            lockIcon.mSimLocked = lockIcon.mKeyguardUpdateMonitor.isSimPinSecure();
            LockIcon lockIcon2 = LockIcon.this;
            lockIcon2.update(access$400 != lockIcon2.mSimLocked);
        }

        public void onKeyguardVisibilityChanged(boolean z) {
            LockIcon.this.update();
        }

        public void onBiometricRunningStateChanged(boolean z, BiometricSourceType biometricSourceType) {
            LockIcon.this.update();
        }

        public void onStrongAuthStateChanged(int i) {
            LockIcon.this.update();
        }

        public void onStartedWakingUp() {
            LockIcon.this.opSetDeviceInteractive(true);
        }

        public void onFinishedGoingToSleep(int i) {
            LockIcon.this.opSetDeviceInteractive(false);
        }

        public void onFacelockStateChanged(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("onFacelockStateChanged, type:");
            sb.append(i);
            Log.d("LockIcon", sb.toString());
            LockIcon.this.update(true);
        }
    };
    private boolean mWakeAndUnlockRunning;
    private boolean mWasPulsingOnThisFrame;

    public LockIcon(Context context, AttributeSet attributeSet, StatusBarStateController statusBarStateController, ConfigurationController configurationController, AccessibilityController accessibilityController, KeyguardMonitor keyguardMonitor, DockManager dockManager, Handler handler) {
        super(context, attributeSet);
        this.mContext = context;
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(context);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mAccessibilityController = accessibilityController;
        this.mConfigurationController = configurationController;
        this.mStatusBarStateController = statusBarStateController;
        this.mKeyguardMonitor = keyguardMonitor;
        this.mDockManager = dockManager;
        this.mMainHandler = handler;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mStatusBarStateController.addCallback(this);
        this.mConfigurationController.addCallback(this);
        this.mKeyguardMonitor.addCallback(this.mKeyguardMonitorCallback);
        this.mKeyguardUpdateMonitor.registerCallback(this.mUpdateMonitorCallback);
        this.mUnlockMethodCache.addListener(this);
        this.mSimLocked = this.mKeyguardUpdateMonitor.isSimPinSecure();
        DockManager dockManager = this.mDockManager;
        if (dockManager != null) {
            dockManager.addListener(this.mDockEventListener);
        }
        onThemeChanged();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mStatusBarStateController.removeCallback(this);
        this.mConfigurationController.removeCallback(this);
        this.mKeyguardUpdateMonitor.removeCallback(this.mUpdateMonitorCallback);
        this.mKeyguardMonitor.removeCallback(this.mKeyguardMonitorCallback);
        this.mUnlockMethodCache.removeListener(this);
        DockManager dockManager = this.mDockManager;
        if (dockManager != null) {
            dockManager.removeListener(this.mDockEventListener);
        }
    }

    public void onThemeChanged() {
        TypedArray obtainStyledAttributes = this.mContext.getTheme().obtainStyledAttributes(null, new int[]{R$attr.wallpaperTextColor}, 0, 0);
        this.mIconColor = obtainStyledAttributes.getColor(0, -1);
        obtainStyledAttributes.recycle();
        updateDarkTint();
    }

    public void onUserInfoChanged(String str, Drawable drawable, String str2) {
        update();
    }

    public void setTransientBiometricsError(boolean z) {
        this.mTransientBiometricsError = z;
        update();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        int i = configuration.densityDpi;
        if (i != this.mDensity) {
            this.mDensity = i;
            update();
        }
    }

    public void update() {
        update(false);
    }

    public void update(boolean z) {
        opUpdate(z);
    }

    /* JADX WARNING: Removed duplicated region for block: B:43:0x0084  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x008b  */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x0098  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x009c  */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x00c5  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00d0  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x00db  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void opUpdate(boolean r13) {
        /*
            r12 = this;
            boolean r0 = r12.isShown()
            r1 = 1
            r2 = 0
            if (r0 == 0) goto L_0x0016
            android.content.Context r0 = r12.mContext
            com.android.keyguard.KeyguardUpdateMonitor r0 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r0)
            boolean r0 = r0.isDeviceInteractive()
            if (r0 == 0) goto L_0x0016
            r0 = r1
            goto L_0x0017
        L_0x0016:
            r0 = r2
        L_0x0017:
            if (r0 == 0) goto L_0x001f
            com.oneplus.phone.OpTrustDrawable r0 = r12.mTrustDrawable
            r0.start()
            goto L_0x0024
        L_0x001f:
            com.oneplus.phone.OpTrustDrawable r0 = r12.mTrustDrawable
            r0.stop()
        L_0x0024:
            int r0 = r12.opGetState()
            r3 = 15
            if (r0 == r3) goto L_0x0032
            r3 = 3
            if (r0 != r3) goto L_0x0030
            goto L_0x0032
        L_0x0030:
            r10 = r2
            goto L_0x0033
        L_0x0032:
            r10 = r1
        L_0x0033:
            r11 = 11
            if (r0 != r11) goto L_0x0039
            r3 = r1
            goto L_0x003a
        L_0x0039:
            r3 = r2
        L_0x003a:
            r12.mIsFaceUnlockState = r3
            int r3 = r12.mLastState
            if (r0 != r3) goto L_0x0052
            boolean r3 = r12.mDeviceInteractive
            boolean r4 = r12.mLastDeviceInteractive
            if (r3 != r4) goto L_0x0052
            boolean r3 = r12.mScreenOn
            boolean r4 = r12.mLastScreenOn
            if (r3 != r4) goto L_0x0052
            if (r13 == 0) goto L_0x004f
            goto L_0x0052
        L_0x004f:
            r4 = r10
            goto L_0x0152
        L_0x0052:
            java.lang.Runnable r13 = r12.mPaddingRetryRunnable
            r12.removeCallbacks(r13)
            int r4 = r12.mLastState
            boolean r6 = r12.mLastDeviceInteractive
            boolean r7 = r12.mDeviceInteractive
            boolean r8 = r12.mLastScreenOn
            boolean r9 = r12.mScreenOn
            r3 = r12
            r5 = r0
            int r13 = r3.opGetAnimationResForTransition(r4, r5, r6, r7, r8, r9)
            r3 = -1
            if (r13 == r3) goto L_0x006c
            r3 = r1
            goto L_0x006d
        L_0x006c:
            r3 = r2
        L_0x006d:
            int r4 = com.android.systemui.R$drawable.op_lockscreen_fingerprint_draw_off_animation
            if (r13 != r4) goto L_0x0074
            r4 = r1
        L_0x0072:
            r10 = r4
            goto L_0x0082
        L_0x0074:
            int r4 = com.android.systemui.R$drawable.op_trusted_state_to_error_animation
            if (r13 != r4) goto L_0x007b
            r4 = r1
            r10 = r2
            goto L_0x0082
        L_0x007b:
            int r4 = com.android.systemui.R$drawable.op_error_to_trustedstate_animation
            if (r13 != r4) goto L_0x0081
            r4 = r2
            goto L_0x0072
        L_0x0081:
            r4 = r10
        L_0x0082:
            if (r3 == 0) goto L_0x008b
            android.content.Context r5 = r12.mContext
            android.graphics.drawable.Drawable r13 = r5.getDrawable(r13)
            goto L_0x0093
        L_0x008b:
            boolean r13 = r12.mScreenOn
            boolean r5 = r12.mDeviceInteractive
            android.graphics.drawable.Drawable r13 = r12.opGetIconForState(r0, r13, r5)
        L_0x0093:
            boolean r5 = r13 instanceof android.graphics.drawable.AnimatedVectorDrawable
            r6 = 0
            if (r5 == 0) goto L_0x009c
            r5 = r13
            android.graphics.drawable.AnimatedVectorDrawable r5 = (android.graphics.drawable.AnimatedVectorDrawable) r5
            goto L_0x009d
        L_0x009c:
            r5 = r6
        L_0x009d:
            android.content.res.Resources r7 = r12.getResources()
            int r8 = com.android.systemui.R$dimen.keyguard_affordance_icon_height
            int r7 = r7.getDimensionPixelSize(r8)
            android.content.res.Resources r8 = r12.getResources()
            int r9 = com.android.systemui.R$dimen.keyguard_affordance_icon_width
            int r8 = r8.getDimensionPixelSize(r9)
            int r9 = r13.getIntrinsicHeight()
            if (r9 != r7) goto L_0x00bd
            int r9 = r13.getIntrinsicWidth()
            if (r9 == r8) goto L_0x00c3
        L_0x00bd:
            com.oneplus.systemui.statusbar.phone.OpLockIcon$IntrinsicSizeDrawable r9 = new com.oneplus.systemui.statusbar.phone.OpLockIcon$IntrinsicSizeDrawable
            r9.<init>(r13, r8, r7)
            r13 = r9
        L_0x00c3:
            if (r10 == 0) goto L_0x00d0
            android.content.res.Resources r7 = r12.getResources()
            int r8 = com.android.systemui.R$dimen.fingerprint_icon_additional_padding
            int r7 = r7.getDimensionPixelSize(r8)
            goto L_0x00d1
        L_0x00d0:
            r7 = r2
        L_0x00d1:
            r12.setPaddingRelative(r2, r2, r2, r7)
            r12.setImageDrawable(r13, r2)
            boolean r13 = r12.mIsFaceUnlockState
            if (r13 == 0) goto L_0x00e8
            android.content.Context r13 = r12.getContext()
            int r7 = com.android.systemui.R$string.accessibility_scanning_face
            java.lang.String r13 = r13.getString(r7)
            r12.announceForAccessibility(r13)
        L_0x00e8:
            if (r5 == 0) goto L_0x00f2
            if (r3 == 0) goto L_0x00f2
            r5.forceAnimationOnUI()
            r5.start()
        L_0x00f2:
            int r13 = r12.mLastState
            if (r13 == r11) goto L_0x00fc
            if (r0 != r11) goto L_0x00fc
            r12.setBackground(r6)
            goto L_0x011d
        L_0x00fc:
            int r13 = r12.mLastState
            r3 = 13
            if (r13 == r3) goto L_0x0110
            if (r0 != r3) goto L_0x0110
            android.content.Context r13 = r12.mContext
            int r3 = com.android.systemui.R$drawable.op_facelock_lock_ripple_drawable
            android.graphics.drawable.Drawable r13 = r13.getDrawable(r3)
            r12.setBackground(r13)
            goto L_0x011d
        L_0x0110:
            if (r0 == r11) goto L_0x011d
            r13 = 12
            if (r0 == r13) goto L_0x011d
            if (r0 == r3) goto L_0x011d
            com.oneplus.phone.OpTrustDrawable r13 = r12.mTrustDrawable
            r12.setBackground(r13)
        L_0x011d:
            r12.opUpdateIconAnimation(r12, r0)
            boolean r13 = android.os.Build.DEBUG_ONEPLUS
            if (r13 == 0) goto L_0x0148
            int r13 = r12.mLastState
            if (r13 == r0) goto L_0x0148
            java.lang.StringBuilder r13 = new java.lang.StringBuilder
            r13.<init>()
            java.lang.String r3 = "from "
            r13.append(r3)
            int r3 = r12.mLastState
            r13.append(r3)
            java.lang.String r3 = " to "
            r13.append(r3)
            r13.append(r0)
            java.lang.String r13 = r13.toString()
            java.lang.String r3 = "LockIcon"
            android.util.Log.i(r3, r13)
        L_0x0148:
            r12.mLastState = r0
            boolean r13 = r12.mDeviceInteractive
            r12.mLastDeviceInteractive = r13
            boolean r13 = r12.mScreenOn
            r12.mLastScreenOn = r13
        L_0x0152:
            com.android.systemui.statusbar.phone.UnlockMethodCache r13 = r12.mUnlockMethodCache
            boolean r13 = r13.isTrustManaged()
            if (r13 == 0) goto L_0x015d
            if (r4 != 0) goto L_0x015d
            goto L_0x015e
        L_0x015d:
            r1 = r2
        L_0x015e:
            com.oneplus.phone.OpTrustDrawable r13 = r12.mTrustDrawable
            r13.setTrustManaged(r1)
            r12.updateClickability()
            r12.updateDarkTint()
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.LockIcon.opUpdate(boolean):void");
    }

    private void updateClickability() {
        opUpdateClickability();
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        boolean isFingerprintDetectionRunning = this.mKeyguardUpdateMonitor.isFingerprintDetectionRunning();
        boolean isUnlockingWithBiometricAllowed = this.mKeyguardUpdateMonitor.isUnlockingWithBiometricAllowed();
        if (isFingerprintDetectionRunning && isUnlockingWithBiometricAllowed) {
            accessibilityNodeInfo.addAction(new AccessibilityAction(16, getContext().getString(R$string.accessibility_unlock_without_fingerprint)));
            accessibilityNodeInfo.setHintText(getContext().getString(R$string.accessibility_waiting_for_fingerprint));
        } else if (this.mIsFaceUnlockState) {
            accessibilityNodeInfo.setClassName(LockIcon.class.getName());
            accessibilityNodeInfo.setContentDescription(getContext().getString(R$string.accessibility_scanning_face));
        }
    }

    public void onDozeAmountChanged(float f, float f2) {
        this.mDozeAmount = f2;
        updateDarkTint();
    }

    public void setPulsing(boolean z) {
        this.mPulsing = z;
        if (!this.mPulsing) {
            this.mWasPulsingOnThisFrame = true;
            this.mMainHandler.post(new Runnable() {
                public final void run() {
                    LockIcon.this.lambda$setPulsing$0$LockIcon();
                }
            });
        }
        update();
    }

    public /* synthetic */ void lambda$setPulsing$0$LockIcon() {
        this.mWasPulsingOnThisFrame = false;
    }

    public void onDozingChanged(boolean z) {
        this.mDozing = z;
        update();
    }

    private void updateDarkTint() {
        int blendARGB = ColorUtils.blendARGB(this.mIconColor, -1, this.mDozeAmount);
        StringBuilder sb = new StringBuilder();
        sb.append(" updateDarkTint state:");
        sb.append(opGetState());
        Log.i("LockIcon", sb.toString());
        if (opGetState() == 13) {
            blendARGB = Color.parseColor("#FF5236");
        }
        setImageTintList(ColorStateList.valueOf(blendARGB));
    }

    public void setBouncerVisible(boolean z) {
        if (this.mBouncerVisible != z) {
            this.mBouncerVisible = z;
            update();
        }
    }

    public void onDensityOrFontScaleChanged() {
        LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = getResources().getDimensionPixelSize(R$dimen.keyguard_lock_width);
            layoutParams.height = getResources().getDimensionPixelSize(R$dimen.keyguard_lock_height);
            setLayoutParams(layoutParams);
            update(true);
        }
    }

    public void onLocaleListChanged() {
        setContentDescription(getContext().getText(R$string.accessibility_unlock_button));
        update(true);
    }

    public void onUnlockMethodStateChanged() {
        update();
    }

    public void onBiometricAuthModeChanged(boolean z) {
        if (z) {
            this.mWakeAndUnlockRunning = true;
        }
        update();
    }

    public void onShowingLaunchAffordanceChanged(boolean z) {
        this.mShowingLaunchAffordance = z;
        update();
    }

    public void onScrimVisibilityChanged(int i) {
        if (this.mWakeAndUnlockRunning && i == 0) {
            this.mWakeAndUnlockRunning = false;
            update();
        }
    }
}

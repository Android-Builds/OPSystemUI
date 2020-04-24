package com.android.keyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.metrics.LogMaker;
import android.os.Build;
import android.provider.Settings.Secure;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.StatsLog;
import android.view.LayoutInflater;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.systemui.Dependency;
import com.android.systemui.R$anim;
import com.android.systemui.R$color;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.util.InjectionInflationController;
import com.oneplus.keyguard.OpEmergencyPanel;
import com.oneplus.keyguard.OpKeyguardSecurityContainer;
import com.oneplus.sarah.SarahClient;
import com.oneplus.util.OpUtils;

public class KeyguardSecurityContainer extends OpKeyguardSecurityContainer implements KeyguardSecurityView {
    private int mActivePointerId;
    private AlertDialog mAlertDialog;
    private boolean mAppearAnimationStarted;
    /* access modifiers changed from: private */
    public KeyguardSecurityCallback mCallback;
    /* access modifiers changed from: private */
    public SecurityMode mCurrentSecuritySelection;
    private KeyguardSecurityView mCurrentSecurityView;
    /* access modifiers changed from: private */
    public OpEmergencyPanel mEmergencyPanel;
    /* access modifiers changed from: private */
    public Animation mFacelockAnimationSet;
    private InjectionInflationController mInjectionInflationController;
    private float mLastTouchY;
    /* access modifiers changed from: private */
    public LockPatternUtils mLockPatternUtils;
    /* access modifiers changed from: private */
    public final MetricsLogger mMetricsLogger;
    private KeyguardSecurityCallback mNullCallback;
    /* access modifiers changed from: private */
    public SecurityCallback mSecurityCallback;
    /* access modifiers changed from: private */
    public View mSecurityIcon;
    private View mSecurityIconSwap;
    private KeyguardSecurityModel mSecurityModel;
    private KeyguardSecurityViewFlipper mSecurityViewFlipper;
    private final SpringAnimation mSpringAnimation;
    private float mStartTouchY;
    private boolean mSwipeUpToRetry;
    /* access modifiers changed from: private */
    public final KeyguardUpdateMonitor mUpdateMonitor;
    private final VelocityTracker mVelocityTracker;
    private final ViewConfiguration mViewConfiguration;

    /* renamed from: com.android.keyguard.KeyguardSecurityContainer$5 */
    static /* synthetic */ class C05935 {

        /* renamed from: $SwitchMap$com$android$keyguard$KeyguardSecurityModel$SecurityMode */
        static final /* synthetic */ int[] f51xdc0e830a = new int[SecurityMode.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(14:0|1|2|3|4|5|6|7|8|9|10|11|12|(3:13|14|16)) */
        /* JADX WARNING: Can't wrap try/catch for region: R(16:0|1|2|3|4|5|6|7|8|9|10|11|12|13|14|16) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:11:0x0040 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:13:0x004b */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        /* JADX WARNING: Missing exception handler attribute for start block: B:7:0x002a */
        /* JADX WARNING: Missing exception handler attribute for start block: B:9:0x0035 */
        static {
            /*
                com.android.keyguard.KeyguardSecurityModel$SecurityMode[] r0 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                f51xdc0e830a = r0
                int[] r0 = f51xdc0e830a     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.Pattern     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = f51xdc0e830a     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.PIN     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = f51xdc0e830a     // Catch:{ NoSuchFieldError -> 0x002a }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.Password     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                int[] r0 = f51xdc0e830a     // Catch:{ NoSuchFieldError -> 0x0035 }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.Invalid     // Catch:{ NoSuchFieldError -> 0x0035 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0035 }
                r2 = 4
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0035 }
            L_0x0035:
                int[] r0 = f51xdc0e830a     // Catch:{ NoSuchFieldError -> 0x0040 }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.None     // Catch:{ NoSuchFieldError -> 0x0040 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0040 }
                r2 = 5
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0040 }
            L_0x0040:
                int[] r0 = f51xdc0e830a     // Catch:{ NoSuchFieldError -> 0x004b }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.SimPin     // Catch:{ NoSuchFieldError -> 0x004b }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x004b }
                r2 = 6
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x004b }
            L_0x004b:
                int[] r0 = f51xdc0e830a     // Catch:{ NoSuchFieldError -> 0x0056 }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.SimPuk     // Catch:{ NoSuchFieldError -> 0x0056 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0056 }
                r2 = 7
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0056 }
            L_0x0056:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardSecurityContainer.C05935.<clinit>():void");
        }
    }

    public interface SecurityCallback {
        boolean dismiss(boolean z, int i);

        void finish(boolean z, int i);

        void onCancelClicked();

        void onSecurityModeChanged(SecurityMode securityMode, boolean z);

        void reportMDMEvent(String str, String str2, String str3);

        void reset();

        void tryToStartFaceLockFromBouncer();

        void userActivity();
    }

    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public KeyguardSecurityContainer(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public KeyguardSecurityContainer(Context context) {
        this(context, null, 0);
    }

    public KeyguardSecurityContainer(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mCurrentSecuritySelection = SecurityMode.Invalid;
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mLastTouchY = -1.0f;
        this.mActivePointerId = -1;
        this.mStartTouchY = -1.0f;
        this.mCallback = new KeyguardSecurityCallback() {
            public void userActivity() {
                if (KeyguardSecurityContainer.this.mSecurityCallback != null) {
                    KeyguardSecurityContainer.this.mSecurityCallback.userActivity();
                }
            }

            public void dismiss(boolean z, int i) {
                KeyguardSecurityContainer.this.mSecurityCallback.dismiss(z, i);
            }

            public void reportUnlockAttempt(int i, boolean z, int i2) {
                if (z) {
                    StatsLog.write(64, 2);
                    KeyguardSecurityContainer.this.mUpdateMonitor.clearFailedUnlockAttempts(true);
                    Secure.putIntForUser(KeyguardSecurityContainer.this.mContext.getContentResolver(), "confirm_lock_password_fragment.key_num_wrong_confirm_attempts", 0, i);
                    KeyguardSecurityContainer.this.mLockPatternUtils.reportSuccessfulPasswordAttempt(i);
                } else {
                    StatsLog.write(64, 1);
                    KeyguardSecurityContainer.this.reportFailedUnlockAttempt(i, i2);
                }
                KeyguardSecurityContainer.this.mMetricsLogger.write(new LogMaker(197).setType(z ? 10 : 11));
            }

            public void reset() {
                KeyguardSecurityContainer.this.mSecurityCallback.reset();
            }

            public void onCancelClicked() {
                KeyguardSecurityContainer.this.mSecurityCallback.onCancelClicked();
            }

            public void reportMDMEvent(String str, String str2, String str3) {
                if ("pass".equals(str2)) {
                    if (KeyguardSecurityContainer.this.mCurrentSecuritySelection == SecurityMode.Password) {
                        str2 = "password";
                    } else if (KeyguardSecurityContainer.this.mCurrentSecuritySelection == SecurityMode.PIN) {
                        str2 = "pin";
                    }
                }
                KeyguardSecurityContainer.this.mSecurityCallback.reportMDMEvent(str, str2, str3);
            }

            public void tryToStartFaceLockFromBouncer() {
                KeyguardSecurityContainer.this.mSecurityCallback.tryToStartFaceLockFromBouncer();
            }

            public void hideSecurityIcon() {
                KeyguardSecurityContainer.this.hideSecurityIcon();
            }

            public SecurityMode getCurrentSecurityMode() {
                return KeyguardSecurityContainer.this.getCurrentSecurityMode();
            }

            public OpEmergencyPanel getEmergencyPanel() {
                return KeyguardSecurityContainer.this.mEmergencyPanel;
            }
        };
        this.mNullCallback = new KeyguardSecurityCallback() {
            public void dismiss(boolean z, int i) {
            }

            public OpEmergencyPanel getEmergencyPanel() {
                return null;
            }

            public void hideSecurityIcon() {
            }

            public void reportMDMEvent(String str, String str2, String str3) {
            }

            public void reportUnlockAttempt(int i, boolean z, int i2) {
            }

            public void reset() {
            }

            public void tryToStartFaceLockFromBouncer() {
            }

            public void userActivity() {
            }

            public SecurityMode getCurrentSecurityMode() {
                return SecurityMode.None;
            }
        };
        this.mSecurityModel = new KeyguardSecurityModel(context);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mSpringAnimation = new SpringAnimation(this, DynamicAnimation.f18Y);
        this.mInjectionInflationController = new InjectionInflationController(SystemUIFactory.getInstance().getRootComponent());
        this.mViewConfiguration = ViewConfiguration.get(context);
        this.mFacelockAnimationSet = AnimationUtils.loadAnimation(this.mContext, R$anim.facelock_lock_blink);
        if (OpUtils.isSupportEmergencyPanel()) {
            LayoutInflater.from(context).inflate(R$layout.op_keyguard_emergency_panel, this);
        }
    }

    public void setSecurityCallback(SecurityCallback securityCallback) {
        this.mSecurityCallback = securityCallback;
    }

    public void onResume(int i) {
        SecurityMode securityMode = this.mCurrentSecuritySelection;
        if (securityMode != SecurityMode.None) {
            getSecurityView(securityMode).onResume(i);
        }
        updateBiometricRetry();
    }

    public void onPause() {
        AlertDialog alertDialog = this.mAlertDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.mAlertDialog = null;
        }
        SecurityMode securityMode = this.mCurrentSecuritySelection;
        if (securityMode != SecurityMode.None) {
            getSecurityView(securityMode).onPause();
        }
    }

    public void startAppearAnimation() {
        SecurityMode securityMode = this.mCurrentSecuritySelection;
        if (securityMode != SecurityMode.None) {
            getSecurityView(securityMode).startAppearAnimation();
        }
        this.mAppearAnimationStarted = true;
    }

    public boolean startDisappearAnimation(Runnable runnable) {
        this.mAppearAnimationStarted = false;
        SecurityMode securityMode = this.mCurrentSecuritySelection;
        if (securityMode != SecurityMode.None) {
            return getSecurityView(securityMode).startDisappearAnimation(runnable);
        }
        return false;
    }

    private void updateBiometricRetry() {
        SecurityMode securityMode = getSecurityMode();
        this.mSwipeUpToRetry = (!this.mUpdateMonitor.isUnlockWithFacePossible(KeyguardUpdateMonitor.getCurrentUser()) || securityMode == SecurityMode.SimPin || securityMode == SecurityMode.SimPuk || securityMode == SecurityMode.None) ? false : true;
    }

    public CharSequence getTitle() {
        return this.mSecurityViewFlipper.getTitle();
    }

    private KeyguardSecurityView getSecurityView(SecurityMode securityMode) {
        KeyguardSecurityView keyguardSecurityView;
        int securityViewIdForMode = getSecurityViewIdForMode(securityMode);
        int childCount = this.mSecurityViewFlipper.getChildCount();
        int i = 0;
        while (true) {
            if (i >= childCount) {
                keyguardSecurityView = null;
                break;
            } else if (this.mSecurityViewFlipper.getChildAt(i).getId() == securityViewIdForMode) {
                keyguardSecurityView = (KeyguardSecurityView) this.mSecurityViewFlipper.getChildAt(i);
                break;
            } else {
                i++;
            }
        }
        int layoutIdFor = getLayoutIdFor(securityMode);
        if (keyguardSecurityView == null && layoutIdFor != 0) {
            LayoutInflater from = LayoutInflater.from(this.mContext);
            StringBuilder sb = new StringBuilder();
            sb.append("inflating id = ");
            sb.append(layoutIdFor);
            Log.v("KeyguardSecurityView", sb.toString());
            View inflate = this.mInjectionInflationController.injectable(from).inflate(layoutIdFor, this.mSecurityViewFlipper, false);
            this.mSecurityViewFlipper.addView(inflate);
            updateSecurityView(inflate);
            keyguardSecurityView = (KeyguardSecurityView) inflate;
            keyguardSecurityView.reset();
        }
        updateSecurityIcon(keyguardSecurityView);
        return keyguardSecurityView;
    }

    private void updateSecurityView(View view) {
        if (view instanceof KeyguardSecurityView) {
            KeyguardSecurityView keyguardSecurityView = (KeyguardSecurityView) view;
            keyguardSecurityView.setKeyguardCallback(this.mCallback);
            keyguardSecurityView.setLockPatternUtils(this.mLockPatternUtils);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("View ");
        sb.append(view);
        sb.append(" is not a KeyguardSecurityView");
        Log.w("KeyguardSecurityView", sb.toString());
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mSecurityViewFlipper = (KeyguardSecurityViewFlipper) findViewById(R$id.view_flipper);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
        if (OpUtils.isSupportEmergencyPanel()) {
            this.mEmergencyPanel = (OpEmergencyPanel) findViewById(R$id.emergency_panel);
            this.mEmergencyPanel.setVisibility(8);
        }
    }

    public void setLockPatternUtils(LockPatternUtils lockPatternUtils) {
        this.mLockPatternUtils = lockPatternUtils;
        this.mSecurityModel.setLockPatternUtils(lockPatternUtils);
        this.mSecurityViewFlipper.setLockPatternUtils(this.mLockPatternUtils);
    }

    /* access modifiers changed from: protected */
    public boolean fitSystemWindows(Rect rect) {
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), rect.bottom);
        rect.bottom = 0;
        return false;
    }

    private void showDialog(String str, String str2) {
        AlertDialog alertDialog = this.mAlertDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        int themeColor = OpUtils.getThemeColor(this.mContext);
        this.mAlertDialog = new Builder(this.mContext, themeColor == 1 ? 16974374 : 16974394).setTitle(str).setMessage(str2).setCancelable(false).setNeutralButton(R$string.notification_appops_ok, null).create();
        if (!(this.mContext instanceof Activity)) {
            this.mAlertDialog.getWindow().setType(2009);
        }
        this.mAlertDialog.show();
        if (themeColor != 2) {
            Button button = this.mAlertDialog.getButton(-3);
            int themeAccentColor = OpUtils.getThemeAccentColor(this.mContext, R$color.qs_detail_button_white);
            if (button != null && themeAccentColor > 0) {
                button.setTextColor(themeAccentColor);
            }
        }
    }

    private void showTimeoutDialog(int i, int i2) {
        int i3;
        int i4 = i2 / 1000;
        int i5 = C05935.f51xdc0e830a[this.mSecurityModel.getSecurityMode(i).ordinal()];
        if (i5 == 1) {
            i3 = R$string.kg_too_many_failed_pattern_attempts_dialog_message;
        } else if (i5 == 2) {
            i3 = R$string.kg_too_many_failed_pin_attempts_dialog_message;
        } else if (i5 != 3) {
            i3 = 0;
        } else {
            i3 = R$string.kg_too_many_failed_password_attempts_dialog_message;
        }
        if (i3 != 0) {
            showDialog(null, this.mContext.getString(i3, new Object[]{Integer.valueOf(this.mLockPatternUtils.getCurrentFailedPasswordAttempts(i)), Integer.valueOf(i4)}));
        }
    }

    private void showAlmostAtWipeDialog(int i, int i2, int i3) {
        String str;
        if (i3 == 1) {
            str = this.mContext.getString(R$string.kg_failed_attempts_almost_at_wipe, new Object[]{Integer.valueOf(i), Integer.valueOf(i2)});
        } else if (i3 == 2) {
            str = this.mContext.getString(R$string.kg_failed_attempts_almost_at_erase_profile, new Object[]{Integer.valueOf(i), Integer.valueOf(i2)});
        } else if (i3 != 3) {
            str = null;
        } else {
            str = this.mContext.getString(R$string.kg_failed_attempts_almost_at_erase_user, new Object[]{Integer.valueOf(i), Integer.valueOf(i2)});
        }
        showDialog(null, str);
    }

    private void showWipeDialog(int i, int i2) {
        String str;
        if (i2 == 1) {
            str = this.mContext.getString(R$string.kg_failed_attempts_now_wiping, new Object[]{Integer.valueOf(i)});
        } else if (i2 == 2) {
            str = this.mContext.getString(R$string.kg_failed_attempts_now_erasing_profile, new Object[]{Integer.valueOf(i)});
        } else if (i2 != 3) {
            str = null;
        } else {
            str = this.mContext.getString(R$string.kg_failed_attempts_now_erasing_user, new Object[]{Integer.valueOf(i)});
        }
        showDialog(null, str);
    }

    /* access modifiers changed from: private */
    public void reportFailedUnlockAttempt(int i, int i2) {
        int i3 = 1;
        int currentFailedPasswordAttempts = this.mLockPatternUtils.getCurrentFailedPasswordAttempts(i) + 1;
        StringBuilder sb = new StringBuilder();
        sb.append("reportFailedPatternAttempt: #");
        sb.append(currentFailedPasswordAttempts);
        String str = "KeyguardSecurityView";
        Log.d(str, sb.toString());
        DevicePolicyManager devicePolicyManager = this.mLockPatternUtils.getDevicePolicyManager();
        int maximumFailedPasswordsForWipe = devicePolicyManager.getMaximumFailedPasswordsForWipe(null, i);
        int i4 = maximumFailedPasswordsForWipe > 0 ? maximumFailedPasswordsForWipe - currentFailedPasswordAttempts : Integer.MAX_VALUE;
        if (i4 < 5) {
            int profileWithMinimumFailedPasswordsForWipe = devicePolicyManager.getProfileWithMinimumFailedPasswordsForWipe(i);
            if (profileWithMinimumFailedPasswordsForWipe == i) {
                if (profileWithMinimumFailedPasswordsForWipe != 0) {
                    i3 = 3;
                }
            } else if (profileWithMinimumFailedPasswordsForWipe != -10000) {
                i3 = 2;
            }
            if (i4 > 0) {
                showAlmostAtWipeDialog(currentFailedPasswordAttempts, i4, i3);
            } else {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Too many unlock attempts; user ");
                sb2.append(profileWithMinimumFailedPasswordsForWipe);
                sb2.append(" will be wiped!");
                Slog.i(str, sb2.toString());
                showWipeDialog(currentFailedPasswordAttempts, i3);
            }
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).reportFailedStrongAuthUnlockAttempt(i);
        this.mLockPatternUtils.reportFailedPasswordAttempt(i);
        if (i2 > 0) {
            this.mLockPatternUtils.reportPasswordLockout(i2, i);
            showTimeoutDialog(i, i2);
        }
    }

    /* access modifiers changed from: 0000 */
    public void showPrimarySecurityScreen(boolean z) {
        SecurityMode securityMode = this.mSecurityModel.getSecurityMode(KeyguardUpdateMonitor.getCurrentUser());
        StringBuilder sb = new StringBuilder();
        sb.append("showPrimarySecurityScreen(turningOff=");
        sb.append(z);
        sb.append(")");
        Log.v("KeyguardSecurityView", sb.toString());
        showSecurityScreen(securityMode);
    }

    /* access modifiers changed from: 0000 */
    public boolean showNextSecurityScreenOrFinish(boolean z, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("showNextSecurityScreenOrFinish(");
        sb.append(z);
        sb.append("), ");
        sb.append(this.mCurrentSecuritySelection);
        sb.append(", ");
        sb.append(i);
        sb.append(", userHasTrust = ");
        sb.append(this.mUpdateMonitor.getUserHasTrust(i));
        String str = "KeyguardSecurityView";
        Log.d(str, sb.toString());
        int i2 = 4;
        String str2 = "swipe";
        boolean z2 = false;
        boolean z3 = true;
        if (this.mUpdateMonitor.getUserHasTrust(i)) {
            if (this.mContext != null) {
                SarahClient.getInstance(this.mContext).notifyUnlock(str2);
            }
            i2 = 3;
        } else if (this.mUpdateMonitor.getUserUnlockedWithBiometric(i)) {
            i2 = 2;
        } else {
            SecurityMode securityMode = SecurityMode.None;
            SecurityMode securityMode2 = this.mCurrentSecuritySelection;
            if (securityMode == securityMode2) {
                SecurityMode securityMode3 = this.mSecurityModel.getSecurityMode(i);
                if (SecurityMode.None == securityMode3) {
                    if (this.mContext != null) {
                        SarahClient.getInstance(this.mContext).notifyUnlock(str2);
                    }
                    i2 = 0;
                } else {
                    showSecurityScreen(securityMode3);
                }
            } else if (z) {
                int i3 = C05935.f51xdc0e830a[securityMode2.ordinal()];
                if (i3 == 1 || i3 == 2 || i3 == 3) {
                    i2 = 1;
                    z2 = true;
                } else if (i3 == 6 || i3 == 7) {
                    SecurityMode securityMode4 = this.mSecurityModel.getSecurityMode(i);
                    if (securityMode4 != SecurityMode.None || !this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser())) {
                        if (securityMode4 != SecurityMode.None || this.mUpdateMonitor.isSimPinSecure() || this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser())) {
                            showSecurityScreen(securityMode4);
                        } else {
                            Log.d(str, "finish it when no SimPin");
                        }
                    }
                } else {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Bad security screen ");
                    sb2.append(this.mCurrentSecuritySelection);
                    sb2.append(", fail safe");
                    Log.v(str, sb2.toString());
                    showPrimarySecurityScreen(false);
                }
            }
            i2 = -1;
            z3 = false;
        }
        if (i2 != -1) {
            this.mMetricsLogger.write(new LogMaker(197).setType(5).setSubtype(i2));
        }
        if (z3) {
            this.mSecurityCallback.finish(z2, i);
        }
        return z3;
    }

    private void showSecurityScreen(SecurityMode securityMode) {
        StringBuilder sb = new StringBuilder();
        sb.append("showSecurityScreen(");
        sb.append(securityMode);
        sb.append(")");
        Log.d("KeyguardSecurityView", sb.toString());
        SecurityMode securityMode2 = this.mCurrentSecuritySelection;
        if (securityMode != securityMode2) {
            KeyguardSecurityView securityView = getSecurityView(securityMode2);
            KeyguardSecurityView securityView2 = getSecurityView(securityMode);
            if (securityView != null) {
                securityView.onPause();
                securityView.setKeyguardCallback(this.mNullCallback);
            }
            if (securityMode != SecurityMode.None) {
                securityView2.onResume(2);
                securityView2.setKeyguardCallback(this.mCallback);
            }
            int childCount = this.mSecurityViewFlipper.getChildCount();
            int securityViewIdForMode = getSecurityViewIdForMode(securityMode);
            boolean z = false;
            int i = 0;
            while (true) {
                if (i >= childCount) {
                    break;
                } else if (this.mSecurityViewFlipper.getChildAt(i).getId() == securityViewIdForMode) {
                    this.mSecurityViewFlipper.setDisplayedChild(i);
                    break;
                } else {
                    i++;
                }
            }
            this.mCurrentSecuritySelection = securityMode;
            this.mCurrentSecurityView = securityView2;
            SecurityCallback securityCallback = this.mSecurityCallback;
            if (securityMode != SecurityMode.None && securityView2.needsInput()) {
                z = true;
            }
            securityCallback.onSecurityModeChanged(securityMode, z);
        }
    }

    private int getSecurityViewIdForMode(SecurityMode securityMode) {
        int i = C05935.f51xdc0e830a[securityMode.ordinal()];
        if (i == 1) {
            return R$id.keyguard_pattern_view;
        }
        if (i != 2) {
            if (i == 3) {
                return R$id.keyguard_password_view;
            }
            if (i == 6) {
                return R$id.keyguard_sim_pin_view;
            }
            if (i != 7) {
                return 0;
            }
            return R$id.keyguard_sim_puk_view;
        } else if (this.mUpdateMonitor.isAutoCheckPinEnabled()) {
            return R$id.keyguard_pin_view_auto;
        } else {
            return R$id.keyguard_pin_view;
        }
    }

    public int getLayoutIdFor(SecurityMode securityMode) {
        int i = C05935.f51xdc0e830a[securityMode.ordinal()];
        if (i == 1) {
            return R$layout.keyguard_pattern_view;
        }
        if (i != 2) {
            if (i == 3) {
                return R$layout.keyguard_password_view;
            }
            if (i == 6) {
                return R$layout.keyguard_sim_pin_view;
            }
            if (i != 7) {
                return 0;
            }
            return R$layout.keyguard_sim_puk_view;
        } else if (this.mUpdateMonitor.isAutoCheckPinEnabled()) {
            return R$layout.keyguard_pin_view_auto;
        } else {
            return R$layout.keyguard_pin_view;
        }
    }

    public SecurityMode getSecurityMode() {
        return this.mSecurityModel.getSecurityMode(KeyguardUpdateMonitor.getCurrentUser());
    }

    public SecurityMode getCurrentSecurityMode() {
        return this.mCurrentSecuritySelection;
    }

    public KeyguardSecurityView getCurrentSecurityView() {
        return this.mCurrentSecurityView;
    }

    public SecurityMode getCurrentSecuritySelection() {
        return this.mCurrentSecuritySelection;
    }

    public boolean needsInput() {
        return this.mSecurityViewFlipper.needsInput();
    }

    public void setKeyguardCallback(KeyguardSecurityCallback keyguardSecurityCallback) {
        this.mSecurityViewFlipper.setKeyguardCallback(keyguardSecurityCallback);
    }

    public void reset() {
        this.mSecurityViewFlipper.reset();
    }

    public void showPromptReason(int i) {
        if (this.mCurrentSecuritySelection != SecurityMode.None) {
            if (i != 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Strong auth required, reason: ");
                sb.append(i);
                Log.i("KeyguardSecurityView", sb.toString());
            }
            getSecurityView(this.mCurrentSecuritySelection).showPromptReason(i);
        }
    }

    public void showMessage(CharSequence charSequence, ColorStateList colorStateList) {
        SecurityMode securityMode = this.mCurrentSecuritySelection;
        if (securityMode != SecurityMode.None) {
            getSecurityView(securityMode).showMessage(charSequence, colorStateList);
        }
    }

    public boolean isCheckingPassword() {
        SecurityMode securityMode = this.mCurrentSecuritySelection;
        if (securityMode != SecurityMode.None) {
            return getSecurityView(securityMode).isCheckingPassword();
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void hideSecurityIcon() {
        Log.d("KeyguardSecurityView", "hideSecurityIcon");
        View view = this.mSecurityIcon;
        if (view != null) {
            ((ImageView) view.findViewById(R$id.security_image)).setClickable(false);
            this.mSecurityIcon.setVisibility(4);
            this.mSecurityIcon.clearAnimation();
        }
        View view2 = this.mSecurityIconSwap;
        if (view2 != null) {
            view2.setVisibility(0);
        }
        Animation animation = this.mFacelockAnimationSet;
        if (animation != null) {
            animation.setAnimationListener(null);
        }
    }

    private void updateSecurityIcon(KeyguardSecurityView keyguardSecurityView) {
        if (keyguardSecurityView != null) {
            ViewGroup viewGroup = (ViewGroup) keyguardSecurityView;
            this.mSecurityIcon = viewGroup.findViewById(R$id.fingerprint_icon);
            this.mSecurityIconSwap = viewGroup.findViewById(R$id.fingerprint_icon_swap);
        }
        updateSecurityIcon();
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x0084  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x0091  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0093  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x00b6  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateSecurityIcon() {
        /*
            r7 = this;
            android.view.View r0 = r7.mSecurityIcon
            if (r0 == 0) goto L_0x00c1
            int r1 = com.android.systemui.R$id.security_image
            android.view.View r0 = r0.findViewById(r1)
            android.widget.ImageView r0 = (android.widget.ImageView) r0
            com.android.keyguard.KeyguardUpdateMonitor r1 = r7.mUpdateMonitor
            boolean r1 = r1.isFacelockAvailable()
            r2 = 1
            r3 = 0
            if (r1 != 0) goto L_0x0021
            com.android.keyguard.KeyguardUpdateMonitor r1 = r7.mUpdateMonitor
            boolean r1 = r1.isFacelockRecognizing()
            if (r1 == 0) goto L_0x001f
            goto L_0x0021
        L_0x001f:
            r1 = r3
            goto L_0x0022
        L_0x0021:
            r1 = r2
        L_0x0022:
            com.android.keyguard.KeyguardUpdateMonitor r4 = r7.mUpdateMonitor
            boolean r4 = r4.isFingerprintDetectionRunning()
            com.android.keyguard.KeyguardUpdateMonitor r5 = r7.mUpdateMonitor
            boolean r5 = r5.isUnlockingWithBiometricAllowed()
            if (r5 == 0) goto L_0x003a
            com.android.keyguard.KeyguardUpdateMonitor r5 = r7.mUpdateMonitor
            boolean r5 = r5.isFingerprintLockout()
            if (r5 != 0) goto L_0x003a
            r5 = r2
            goto L_0x003b
        L_0x003a:
            r5 = r3
        L_0x003b:
            com.android.keyguard.KeyguardUpdateMonitor r6 = r7.mUpdateMonitor
            boolean r6 = r6.isKeyguardDone()
            if (r6 == 0) goto L_0x0045
        L_0x0043:
            r4 = r3
            goto L_0x0080
        L_0x0045:
            if (r1 == 0) goto L_0x004e
            int r4 = com.android.systemui.R$drawable.facelock_bouncer_icon
            r0.setImageResource(r4)
            r4 = r2
            goto L_0x0080
        L_0x004e:
            boolean r6 = com.oneplus.util.OpUtils.isCustomFingerprint()
            if (r6 != 0) goto L_0x0043
            if (r4 == 0) goto L_0x0043
            if (r5 == 0) goto L_0x0043
            android.content.Context r4 = r7.getContext()
            java.lang.String r5 = "fingerprint"
            java.lang.Object r4 = r4.getSystemService(r5)
            android.hardware.fingerprint.FingerprintManager r4 = (android.hardware.fingerprint.FingerprintManager) r4
            java.util.List r5 = r4.getEnrolledFingerprints()
            if (r5 == 0) goto L_0x006f
            int r5 = r5.size()
            goto L_0x0070
        L_0x006f:
            r5 = r3
        L_0x0070:
            boolean r4 = r4.isHardwareDetected()
            if (r4 == 0) goto L_0x007a
            if (r5 <= 0) goto L_0x007a
            r4 = r2
            goto L_0x007b
        L_0x007a:
            r4 = r3
        L_0x007b:
            int r5 = com.android.systemui.R$drawable.ic_fingerprint_lockscreen_blow
            r0.setImageResource(r5)
        L_0x0080:
            android.view.View r5 = r7.mSecurityIconSwap
            if (r5 == 0) goto L_0x008d
            if (r4 != 0) goto L_0x0088
            r6 = r3
            goto L_0x008a
        L_0x0088:
            r6 = 8
        L_0x008a:
            r5.setVisibility(r6)
        L_0x008d:
            android.view.View r5 = r7.mSecurityIcon
            if (r4 == 0) goto L_0x0093
            r6 = r3
            goto L_0x0094
        L_0x0093:
            r6 = 4
        L_0x0094:
            r5.setVisibility(r6)
            java.lang.String r5 = "KeyguardSecurityView"
            if (r4 == 0) goto L_0x00b2
            if (r1 == 0) goto L_0x00b2
            r0.setClickable(r2)
            com.android.keyguard.KeyguardSecurityContainer$3 r1 = new com.android.keyguard.KeyguardSecurityContainer$3
            r1.<init>()
            r0.setOnClickListener(r1)
            boolean r0 = android.os.Build.DEBUG_ONEPLUS
            if (r0 == 0) goto L_0x00be
            java.lang.String r0 = "show bouncer face icon"
            android.util.Log.d(r5, r0)
            goto L_0x00be
        L_0x00b2:
            boolean r1 = android.os.Build.DEBUG_ONEPLUS
            if (r1 == 0) goto L_0x00bb
            java.lang.String r1 = "hide bouncer face icon"
            android.util.Log.d(r5, r1)
        L_0x00bb:
            r0.setClickable(r3)
        L_0x00be:
            r7.updateIconAnimation()
        L_0x00c1:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardSecurityContainer.updateSecurityIcon():void");
    }

    private void updateIconAnimation() {
        if (this.mSecurityIcon != null && this.mFacelockAnimationSet != null) {
            String str = "KeyguardSecurityView";
            if (!this.mUpdateMonitor.isFacelockRecognizing() || !this.mAppearAnimationStarted) {
                if (Build.DEBUG_ONEPLUS) {
                    Log.d(str, "stop anim");
                }
                this.mSecurityIcon.clearAnimation();
                this.mFacelockAnimationSet.setAnimationListener(null);
                return;
            }
            this.mFacelockAnimationSet.setAnimationListener(new AnimationListener() {
                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    if (KeyguardSecurityContainer.this.mUpdateMonitor.isFacelockRecognizing() && KeyguardSecurityContainer.this.mFacelockAnimationSet != null && KeyguardSecurityContainer.this.mSecurityIcon != null) {
                        if (Build.DEBUG_ONEPLUS) {
                            Log.d("KeyguardSecurityView", "start again");
                        }
                        KeyguardSecurityContainer.this.mFacelockAnimationSet.setAnimationListener(this);
                        KeyguardSecurityContainer.this.mSecurityIcon.startAnimation(KeyguardSecurityContainer.this.mFacelockAnimationSet);
                    }
                }
            });
            if (Build.DEBUG_ONEPLUS) {
                Log.d(str, "start anim");
            }
            this.mSecurityIcon.startAnimation(this.mFacelockAnimationSet);
        }
    }
}

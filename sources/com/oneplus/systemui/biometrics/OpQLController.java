package com.oneplus.systemui.biometrics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R$layout;
import com.oneplus.util.OpUtils;
import com.oneplus.util.VibratorSceneUtils;

public class OpQLController {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    static final boolean IS_SUPPORT_QL = OpUtils.isSupportQuickLaunch();
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new Builder().setContentType(4).setUsage(13).build();
    /* access modifiers changed from: private */
    public Context mContext;
    private OpBiometricDialogImpl mDialogImpl;
    private OpFingerprintDialogView mFodDialogView;
    private OpFodFingerTouchValidator mFodFingerTouchValidator;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mIsEnableQL;
    private QLStateListener mListener;
    private QLContentObserver mObserver;
    /* access modifiers changed from: private */
    public String mQLConfig;
    private OpQLRootView mQLRootView;
    private boolean mQLShowing;
    private QLReceiver mReceiver;
    private Runnable mShowQLView = new Runnable() {
        public final void run() {
            OpQLController.this.lambda$new$1$OpQLController();
        }
    };
    private WindowManager mWindowManager;

    private final class QLContentObserver extends ContentObserver {
        private final Uri mQLAppsUri = Secure.getUriFor("op_quick_launch_apps");
        private final Uri mQLEnableUri = Secure.getUriFor("op_quickpay_enable");

        public QLContentObserver() {
            super(OpQLController.this.mHandler);
            OpQLController.this.mContext.getContentResolver().registerContentObserver(this.mQLEnableUri, true, this, -1);
            OpQLController.this.mContext.getContentResolver().registerContentObserver(this.mQLAppsUri, true, this, -1);
            onChange();
        }

        public void onChange(boolean z, Uri uri) {
            if (this.mQLEnableUri.equals(uri)) {
                updateQuickpayEnable();
            } else if (this.mQLAppsUri.equals(uri)) {
                updateQuickLaunchApps();
            }
        }

        public void onChange() {
            updateQuickpayEnable();
            updateQuickLaunchApps();
        }

        private void updateQuickpayEnable() {
            OpQLController opQLController = OpQLController.this;
            boolean z = false;
            if (Secure.getInt(opQLController.mContext.getContentResolver(), "op_quickpay_enable", 0) == 1) {
                z = true;
            }
            opQLController.mIsEnableQL = z;
            if (OpQLController.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("op_quickpay_enable ");
                sb.append(OpQLController.this.mIsEnableQL);
                Log.d("OpQLController", sb.toString());
            }
        }

        private void updateQuickLaunchApps() {
            OpQLController opQLController = OpQLController.this;
            opQLController.mQLConfig = Secure.getString(opQLController.mContext.getContentResolver(), "op_quick_launch_apps");
            if (SystemProperties.getInt("debug.ql.wx.mini.program", 0) != 0) {
                OpQLController opQLController2 = OpQLController.this;
                StringBuilder sb = new StringBuilder();
                sb.append(OpQLController.this.mQLConfig);
                sb.append("OpenWxMiniProgram:com.tencent.mm;0,");
                opQLController2.mQLConfig = sb.toString();
            }
            if (OpQLController.DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("op_quick_launch_apps change ");
                sb2.append(OpQLController.this.mQLConfig);
                Log.d("OpQLController", sb2.toString());
            }
        }
    }

    private final class QLReceiver extends BroadcastReceiver {
        public QLReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PHONE_STATE");
            intentFilter.addAction("com.android.deskclock.ALARM_ALERT");
            OpQLController.this.mContext.registerReceiver(this, intentFilter, null, OpQLController.this.mHandler);
        }

        public void onReceive(Context context, Intent intent) {
            if (OpQLController.this.isQLEnabled()) {
                String action = intent.getAction();
                boolean z = false;
                if ("android.intent.action.PHONE_STATE".equals(action)) {
                    z = TelephonyManager.EXTRA_STATE_RINGING.equals(intent.getStringExtra("state"));
                } else if ("com.android.deskclock.ALARM_ALERT".equals(action)) {
                    z = true;
                }
                if (z) {
                    OpQLController.this.shouldHideQLView();
                }
            }
        }
    }

    public interface QLStateListener {
        void onQLVisibilityChanged(boolean z);
    }

    public OpQLController(Context context, Handler handler, OpBiometricDialogImpl opBiometricDialogImpl, OpFingerprintDialogView opFingerprintDialogView, OpFodFingerTouchValidator opFodFingerTouchValidator, QLStateListener qLStateListener) {
        this.mContext = context;
        this.mHandler = handler;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mDialogImpl = opBiometricDialogImpl;
        this.mFodDialogView = opFingerprintDialogView;
        this.mFodFingerTouchValidator = opFodFingerTouchValidator;
        this.mListener = qLStateListener;
        if (IS_SUPPORT_QL) {
            this.mObserver = new QLContentObserver();
            this.mReceiver = new QLReceiver();
        }
    }

    public boolean isQLEnabled() {
        return IS_SUPPORT_QL && this.mIsEnableQL && KeyguardUpdateMonitor.getCurrentUser() == 0;
    }

    public boolean shouldShowQL() {
        if (!isQLEnabled()) {
            return false;
        }
        String str = "OpQLController";
        if (this.mQLShowing) {
            if (DEBUG) {
                Log.d(str, "QL view is already showing");
            }
            return false;
        }
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("shouldShowQL: isFingerDown: ");
            sb.append(this.mFodFingerTouchValidator.isFingerDownOnView());
            Log.d(str, sb.toString());
        }
        if (!this.mFodFingerTouchValidator.isFingerDownOnView()) {
            return false;
        }
        this.mHandler.removeCallbacks(this.mShowQLView);
        this.mHandler.postDelayed(this.mShowQLView, 500);
        if (DEBUG) {
            Log.d(str, "shouldShowQL: waiting to show...");
        }
        return true;
    }

    public boolean isQLShowing() {
        return this.mQLShowing;
    }

    public void interruptShowingQLView() {
        if (this.mHandler.hasCallbacks(this.mShowQLView)) {
            StringBuilder sb = new StringBuilder();
            sb.append("interrupt showing ql view ");
            sb.append(Debug.getCallers(2));
            Log.d("OpQLController", sb.toString());
            this.mHandler.removeCallbacks(this.mShowQLView);
        } else if (!this.mDialogImpl.inFingerprintDialogUiThread()) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    OpQLController.this.shouldHideQLView();
                }
            });
        } else {
            shouldHideQLView();
        }
    }

    public void shouldHideQLView() {
        if (this.mQLShowing && this.mQLRootView != null) {
            if (DEBUG) {
                Log.d("OpQLController", "hideQLView");
            }
            this.mHandler.removeCallbacks(this.mShowQLView);
            $$Lambda$OpQLController$2TCG8Dg9bfuOlZpWl6dbwhpYtg4 r0 = new Runnable() {
                public final void run() {
                    OpQLController.this.lambda$shouldHideQLView$0$OpQLController();
                }
            };
            if (!this.mQLRootView.isAttachedToWindow()) {
                this.mHandler.postDelayed(r0, 500);
            } else {
                r0.run();
            }
            OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
            if (opFingerprintDialogView != null) {
                opFingerprintDialogView.updateIconVisibility(false);
            }
        }
    }

    public /* synthetic */ void lambda$shouldHideQLView$0$OpQLController() {
        OpQLRootView opQLRootView = this.mQLRootView;
        if (opQLRootView != null) {
            this.mWindowManager.removeViewImmediate(opQLRootView);
            this.mQLRootView.onQLExit();
            this.mQLRootView = null;
        }
        this.mQLShowing = false;
        notifyQLViewVisibilityChanged(false);
    }

    public void handleQLTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        String str = "OpQLController";
        if (DEBUG && (action == 0 || action == 1)) {
            StringBuilder sb = new StringBuilder();
            sb.append("mQLShowing ");
            sb.append(this.mQLShowing);
            sb.append(" mQLRootView ");
            sb.append(this.mQLRootView);
            sb.append(" attach ");
            OpQLRootView opQLRootView = this.mQLRootView;
            sb.append(opQLRootView != null ? Boolean.valueOf(opQLRootView.isAttachedToWindow()) : null);
            Log.d(str, sb.toString());
        }
        if (isQLEnabled()) {
            if (action == 1 || action == 3) {
                if (DEBUG) {
                    Log.d(str, "removeCallbacks mShowQLView");
                }
                this.mHandler.removeCallbacks(this.mShowQLView);
            }
            if (this.mQLShowing) {
                OpQLRootView opQLRootView2 = this.mQLRootView;
                if (opQLRootView2 != null) {
                    if (opQLRootView2.isAttachedToWindow()) {
                        this.mQLRootView.onTouch(motionEvent);
                    }
                    if (action == 1 || action == 3) {
                        shouldHideQLView();
                    }
                }
            }
        }
    }

    public /* synthetic */ void lambda$new$1$OpQLController() {
        boolean isCurrentGuest = OpUtils.isCurrentGuest(this.mContext);
        String str = "OpQLController";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("mShowQLView enable ");
            sb.append(isQLEnabled());
            sb.append(" isGuest ");
            sb.append(isCurrentGuest);
            Log.d(str, sb.toString());
        }
        if (isQLEnabled() && !isCurrentGuest) {
            if (DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("mShowQLView mQLShowing ");
                sb2.append(this.mQLShowing);
                sb2.append(" mFingerOnView ");
                sb2.append(this.mFodFingerTouchValidator.isFingerDownOnView());
                Log.d(str, sb2.toString());
            }
            if (!this.mQLShowing && this.mQLRootView == null && this.mFodFingerTouchValidator.isFingerDownOnView()) {
                vibrate(1023);
                OpFingerprintDialogView opFingerprintDialogView = this.mFodDialogView;
                if (opFingerprintDialogView != null) {
                    opFingerprintDialogView.updateIconVisibility(true);
                }
                this.mQLShowing = true;
                this.mQLRootView = (OpQLRootView) LayoutInflater.from(this.mContext).inflate(R$layout.ql_root_view, null);
                String str2 = this.mQLConfig;
                if (str2 != null) {
                    this.mQLRootView.setQLConfig(str2);
                }
                WindowManager windowManager = this.mWindowManager;
                OpQLRootView opQLRootView = this.mQLRootView;
                windowManager.addView(opQLRootView, opQLRootView.getLayoutParams());
                this.mQLRootView.setOnTouchListener(new OnTouchListener() {
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        OpQLController.this.shouldHideQLView();
                        return true;
                    }
                });
                this.mDialogImpl.expandTransparentLayout();
                notifyQLViewVisibilityChanged(true);
            }
        }
    }

    private void vibrate(int i) {
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        if (OpUtils.isSupportLinearVibration()) {
            VibratorSceneUtils.doVibrateWithSceneIfNeeded(this.mContext, vibrator, i);
        } else if (OpUtils.isSupportZVibrationMotor()) {
            vibrator.vibrate(VibrationEffect.get(0), VIBRATION_ATTRIBUTES);
        } else {
            vibrator.vibrate(VibrationEffect.get(5), VIBRATION_ATTRIBUTES);
        }
    }

    private void notifyQLViewVisibilityChanged(boolean z) {
        QLStateListener qLStateListener = this.mListener;
        if (qLStateListener != null) {
            qLStateListener.onQLVisibilityChanged(z);
        }
    }
}

package com.oneplus.plugin;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SystemSensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.provider.Settings.System;
import android.util.Log;
import android.util.OpFeatures;
import android.view.MotionEvent;
import android.widget.ImageView;
import com.android.internal.view.IInputMethodManager.Stub;
import com.android.systemui.Dependency;
import com.android.systemui.R$id;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.oneplus.scene.OpSceneModeObserver;
import com.oneplus.util.OpImageUtils;
import com.oneplus.util.OpUtils;

public class OpPreventModeCtrl extends OpBaseCtrl {
    private static final boolean IS_SUPPORT_POCKET_SWITCH = OpFeatures.isSupport(new int[]{145});
    /* access modifiers changed from: private */
    public static boolean mPreventModeActive;
    private static boolean mPreventModeNoBackground;
    private static boolean mProximitySensorEnabled;
    private final boolean DEBUG = true;
    private final String TAG = "OpPreventModeCtrl";
    private ValueAnimator mAlphaAnimator;
    ImageView mBackground;
    /* access modifiers changed from: private */
    public Bitmap mBitmap;
    private boolean mBouncer = false;
    private boolean mDozing = false;
    private Handler mHandler;
    private int mKeyLockMode;
    private boolean mKeyguardIsShowing = false;
    private boolean mKeyguardIsVisible = false;
    /* access modifiers changed from: private */
    public Object mObject = new Object();
    private OpSceneModeObserver mOpSceneModeObserver;
    OpPreventModeView mPMView;
    SensorEventListener mProximityListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            synchronized (OpPreventModeCtrl.this.mObject) {
                boolean z = false;
                if (sensorEvent.values.length == 0) {
                    Log.d("OpPreventModeCtrl", "Event has no values!");
                    finishWithResult(0);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Event: value=");
                    sb.append(sensorEvent.values[0]);
                    sb.append(" max=");
                    sb.append(OpPreventModeCtrl.this.mProximitySensor.getMaximumRange());
                    Log.d("OpPreventModeCtrl", sb.toString());
                    int i = 1;
                    if (sensorEvent.values[0] == 0.0f) {
                        z = true;
                    }
                    if (!z) {
                        i = 2;
                    }
                    finishWithResult(i);
                }
            }
        }

        private void finishWithResult(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("finishWithResult: result = ");
            sb.append(i);
            Log.d("OpPreventModeCtrl", sb.toString());
            if (i == 1) {
                OpPreventModeCtrl.this.startRootAnimation();
            } else if (i == 2 && OpPreventModeCtrl.mPreventModeActive) {
                OpPreventModeCtrl.this.mPMView.setVisibility(8);
                OpPreventModeCtrl.this.stopPreventMode();
            } else if (i == 0) {
                OpPreventModeCtrl.this.stopPreventMode();
            }
        }
    };
    /* access modifiers changed from: private */
    public Sensor mProximitySensor;
    private SensorManager mSensorManager;
    StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;

    private class ProximityHandler extends Handler {
        private ProximityHandler() {
        }

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                synchronized (OpPreventModeCtrl.this.mObject) {
                    OpPreventModeCtrl.this.enableProximitySensorInternal();
                }
            } else if (i == 2 || i == 3) {
                synchronized (OpPreventModeCtrl.this.mObject) {
                    OpPreventModeCtrl.this.disableProximitySensorInternal();
                    OpPreventModeCtrl.this.stopPreventMode();
                }
            }
        }
    }

    public void onStartCtrl() {
        String str = "OpPreventModeCtrl";
        Log.d(str, "onStartCtrl");
        this.mStatusBarKeyguardViewManager = OpLsState.getInstance().getStatusBarKeyguardViewManager();
        this.mPMView = (OpPreventModeView) OpLsState.getInstance().getContainer().findViewById(R$id.prevent_mode_view);
        this.mBackground = (ImageView) OpLsState.getInstance().getContainer().findViewById(R$id.pevent_mode_background);
        if (this.mPMView == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("mPMView == null, ");
            sb.append(OpLsState.getInstance().getContainer());
            Log.d(str, sb.toString());
        }
        this.mPMView.init();
        this.mHandler = new ProximityHandler();
        this.mSensorManager = new SystemSensorManager(this.mContext, this.mHandler.getLooper());
        this.mProximitySensor = this.mSensorManager.getDefaultSensor(8, true);
        this.mOpSceneModeObserver = (OpSceneModeObserver) Dependency.get(OpSceneModeObserver.class);
    }

    public void onScreenTurnedOn() {
        if (this.mHandler != null && isPreventModeEnabled() && !this.mDozing) {
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
            this.mHandler.sendEmptyMessage(1);
        }
    }

    public void onStartedWakingUp() {
        if (this.mHandler != null && isPreventModeEnabled()) {
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
            this.mHandler.sendEmptyMessage(1);
        }
    }

    public void onFinishedGoingToSleep(int i) {
        disableProximitySensor();
    }

    public void onKeyguardBouncerChanged(boolean z) {
        this.mBouncer = z;
    }

    public void onKeyguardVisibilityChanged(boolean z) {
        this.mKeyguardIsVisible = z;
    }

    public void setKeyguardShowing(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("setKeyguardShowing, ");
        sb.append(z);
        Log.d("OpPreventModeCtrl", sb.toString());
        this.mKeyguardIsShowing = z;
        if (!z) {
            disableProximitySensor();
        }
    }

    public void disPatchTouchEvent(MotionEvent motionEvent) {
        this.mPMView.dispatchTouchEvent(motionEvent);
    }

    private boolean isPreventModeEnabled() {
        boolean z = false;
        if (!IS_SUPPORT_POCKET_SWITCH) {
            return false;
        }
        if (OpUtils.isPreventModeEnalbed(this.mContext) && this.mKeyguardIsShowing) {
            z = true;
        }
        return z;
    }

    /* access modifiers changed from: private */
    public void enableProximitySensorInternal() {
        if (!mProximitySensorEnabled) {
            Log.d("OpPreventModeCtrl", "enableProximitySensor");
            Bitmap bitmap = this.mBitmap;
            if (bitmap != null) {
                this.mBackground.setImageBitmap(bitmap);
            }
            long clearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mSensorManager.registerListener(this.mProximityListener, this.mProximitySensor, 3);
                mProximitySensorEnabled = true;
            } finally {
                Binder.restoreCallingIdentity(clearCallingIdentity);
            }
        }
    }

    public void disableProximitySensor() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.removeMessages(1);
            this.mHandler.removeMessages(2);
            this.mHandler.sendEmptyMessage(2);
        }
    }

    public void stopPreventMode() {
        if (mPreventModeActive) {
            String str = "OpPreventModeCtrl";
            Log.d(str, "stopPreventMode");
            long clearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mPMView.setVisibility(8);
                this.mBackground.setImageDrawable(null);
                if (mPreventModeNoBackground && OpLsState.getInstance().getPhoneStatusBar() != null) {
                    OpLsState.getInstance().getPhoneStatusBar().setPanelViewAlpha(1.0f, true, -1);
                    Log.d(str, "panel alpha to 1");
                }
                mPreventModeNoBackground = false;
                mPreventModeActive = false;
                if (OpLsState.getInstance().getPhoneStatusBar() != null) {
                    OpLsState.getInstance().getPhoneStatusBar().notifyPreventModeChange(false);
                }
            } finally {
                Binder.restoreCallingIdentity(clearCallingIdentity);
            }
        }
    }

    /* access modifiers changed from: private */
    public void disableProximitySensorInternal() {
        if (mProximitySensorEnabled) {
            StringBuilder sb = new StringBuilder();
            sb.append("disableProximitySensor, ");
            sb.append(this.mKeyLockMode);
            Log.d("OpPreventModeCtrl", sb.toString());
            long clearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mSensorManager.unregisterListener(this.mProximityListener);
                mProximitySensorEnabled = false;
            } finally {
                Binder.restoreCallingIdentity(clearCallingIdentity);
            }
        }
    }

    /* access modifiers changed from: private */
    public void startRootAnimation() {
        if (!mPreventModeActive && this.mKeyguardIsShowing && !bypassPreventMode()) {
            StatusBar phoneStatusBar = OpLsState.getInstance().getPhoneStatusBar();
            this.mKeyLockMode = System.getInt(this.mContext.getContentResolver(), "oem_acc_key_lock_mode", -1);
            hideSoftInput();
            StringBuilder sb = new StringBuilder();
            sb.append("startRootAnimation, ");
            sb.append(this.mKeyLockMode);
            sb.append(", ");
            sb.append(this.mBackground.getDrawable());
            String str = "OpPreventModeCtrl";
            Log.d(str, sb.toString());
            if (this.mBackground.getDrawable() == null && phoneStatusBar != null) {
                phoneStatusBar.setPanelViewAlpha(0.0f, true, -1);
                mPreventModeNoBackground = true;
                Log.d(str, "panel alpha to 0");
            }
            mPreventModeActive = true;
            if (phoneStatusBar != null) {
                if (phoneStatusBar.getFacelockController() != null) {
                    phoneStatusBar.getFacelockController().stopFacelockLightMode();
                }
                phoneStatusBar.notifyPreventModeChange(true);
            }
            this.mAlphaAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
            this.mAlphaAnimator.setDuration(0);
            this.mAlphaAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    OpPreventModeCtrl.this.mPMView.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
                }
            });
            this.mAlphaAnimator.addListener(new AnimatorListener() {
                public void onAnimationCancel(Animator animator) {
                }

                public void onAnimationEnd(Animator animator) {
                }

                public void onAnimationRepeat(Animator animator) {
                }

                public void onAnimationStart(Animator animator) {
                    if (OpPreventModeCtrl.this.mBitmap != null) {
                        OpPreventModeCtrl opPreventModeCtrl = OpPreventModeCtrl.this;
                        opPreventModeCtrl.mBackground.setImageBitmap(opPreventModeCtrl.mBitmap);
                    }
                    OpPreventModeCtrl.this.mPMView.setVisibility(0);
                }
            });
            this.mAlphaAnimator.start();
        }
    }

    public void onWallpaperChange(Bitmap bitmap) {
        StringBuilder sb = new StringBuilder();
        sb.append("onWallpaperChange: bitmap:");
        sb.append(bitmap != null);
        Log.d("OpPreventModeCtrl", sb.toString());
        if (bitmap != null) {
            this.mBitmap = OpImageUtils.computeCustomBackgroundBounds(this.mContext, bitmap);
            return;
        }
        this.mBitmap = null;
        this.mBackground.setImageDrawable(null);
    }

    public boolean isPreventModeActive() {
        return mPreventModeActive;
    }

    public boolean isPreventModeNoBackground() {
        return mPreventModeNoBackground;
    }

    private void hideSoftInput() {
        try {
            Stub.asInterface(ServiceManager.getService("input_method")).hideSoftInputForLongshot(0, null);
        } catch (Exception e) {
            Log.w("OpPreventModeCtrl", "hide ime failed, ", e);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x001e, code lost:
        if (r2.isInBrickMode() != false) goto L_0x0020;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean bypassPreventMode() {
        /*
            r2 = this;
            boolean r0 = r2.mKeyguardIsVisible
            r1 = 0
            if (r0 == 0) goto L_0x0006
            return r1
        L_0x0006:
            com.oneplus.plugin.OpLsState r0 = com.oneplus.plugin.OpLsState.getInstance()
            com.android.systemui.statusbar.phone.StatusBar r0 = r0.getPhoneStatusBar()
            if (r0 == 0) goto L_0x0016
            boolean r0 = r0.isCameraForeground()
            if (r0 != 0) goto L_0x0020
        L_0x0016:
            com.oneplus.scene.OpSceneModeObserver r2 = r2.mOpSceneModeObserver
            if (r2 == 0) goto L_0x0021
            boolean r2 = r2.isInBrickMode()
            if (r2 == 0) goto L_0x0021
        L_0x0020:
            r1 = 1
        L_0x0021:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.plugin.OpPreventModeCtrl.bypassPreventMode():boolean");
    }

    public void onPanelExpandedChange(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("onPanelExpandedChange expand");
        sb.append(z);
        sb.append(" mPreventModeActive:");
        sb.append(mPreventModeActive);
        Log.d("OpPreventModeCtrl", sb.toString());
        if (mPreventModeActive) {
            OpPreventModeView opPreventModeView = this.mPMView;
            if (opPreventModeView != null) {
                opPreventModeView.setAlpha(z ? 1.0f : 0.0f);
            }
        }
    }
}

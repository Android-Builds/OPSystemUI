package com.oneplus.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ViewGroup;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.oneplus.battery.OpChargingAnimationController;
import com.oneplus.plugin.OpBaseCtrl.ControlCallback;
import com.oneplus.systemui.biometrics.OpFingerprintAnimationCtrl;

public class OpLsState implements ControlCallback {
    private static OpLsState sInstance;
    private BiometricUnlockController mBiometricUnlockController;
    private CommandQueue mCommandQueue;
    private ViewGroup mContainer;
    private Context mContext;
    public final OpBaseCtrl[] mControls = {this.mPreventModeCtrl};
    private OpFingerprintAnimationCtrl mFingerprintAnimationCtrl;
    private boolean mInit = false;
    /* access modifiers changed from: private */
    public boolean mIsFinishedScreenTuredOn = false;
    private KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onStartedWakingUp() {
            OpBaseCtrl[] opBaseCtrlArr;
            OpLsState.this.mIsFinishedScreenTuredOn = true;
            for (OpBaseCtrl opBaseCtrl : OpLsState.this.mControls) {
                if (opBaseCtrl != null && opBaseCtrl.isEnable()) {
                    opBaseCtrl.onStartedWakingUp();
                }
            }
        }

        public void onStartedGoingToSleep(int i) {
            OpBaseCtrl[] opBaseCtrlArr;
            OpLsState.this.mIsFinishedScreenTuredOn = false;
            for (OpBaseCtrl opBaseCtrl : OpLsState.this.mControls) {
                if (opBaseCtrl != null && opBaseCtrl.isEnable()) {
                    opBaseCtrl.onStartedGoingToSleep(i);
                }
            }
        }

        public void onFinishedGoingToSleep(int i) {
            OpBaseCtrl[] opBaseCtrlArr;
            for (OpBaseCtrl opBaseCtrl : OpLsState.this.mControls) {
                if (opBaseCtrl != null && opBaseCtrl.isEnable()) {
                    opBaseCtrl.onFinishedGoingToSleep(i);
                }
            }
        }

        public void onScreenTurnedOff() {
            OpBaseCtrl[] opBaseCtrlArr;
            OpLsState.this.mIsFinishedScreenTuredOn = false;
            for (OpBaseCtrl opBaseCtrl : OpLsState.this.mControls) {
                if (opBaseCtrl != null && opBaseCtrl.isEnable()) {
                    opBaseCtrl.onScreenTurnedOff();
                }
            }
        }

        public void onKeyguardBouncerChanged(boolean z) {
            OpBaseCtrl[] opBaseCtrlArr;
            for (OpBaseCtrl opBaseCtrl : OpLsState.this.mControls) {
                if (opBaseCtrl != null && opBaseCtrl.isEnable()) {
                    opBaseCtrl.onKeyguardBouncerChanged(z);
                }
            }
        }

        public void onKeyguardVisibilityChanged(boolean z) {
            OpBaseCtrl[] opBaseCtrlArr;
            for (OpBaseCtrl opBaseCtrl : OpLsState.this.mControls) {
                if (opBaseCtrl != null && opBaseCtrl.isEnable()) {
                    opBaseCtrl.onKeyguardVisibilityChanged(z);
                }
            }
        }
    };
    private Looper mNonUiLooper;
    private StatusBar mPhonstatusBar;
    private final OpPreventModeCtrl mPreventModeCtrl = new OpPreventModeCtrl();
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private Handler mUIHandler = new MyUIHandler();
    private KeyguardUpdateMonitor mUpdateMonitor;

    private class MyUIHandler extends Handler {
        private MyUIHandler() {
        }

        public void handleMessage(Message message) {
            if (message.what == 1) {
                synchronized (OpLsState.this) {
                }
            }
        }
    }

    public OpPreventModeCtrl getPreventModeCtrl() {
        return this.mPreventModeCtrl;
    }

    public static synchronized OpLsState getInstance() {
        OpLsState opLsState;
        synchronized (OpLsState.class) {
            if (sInstance == null) {
                sInstance = new OpLsState();
            }
            opLsState = sInstance;
        }
        return opLsState;
    }

    OpLsState() {
    }

    public void init(Context context, ViewGroup viewGroup, StatusBar statusBar, CommandQueue commandQueue) {
        OpBaseCtrl[] opBaseCtrlArr;
        synchronized (this) {
            if (!this.mInit) {
                Log.d("OpLsState", "init");
                this.mContainer = viewGroup;
                this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
                this.mPhonstatusBar = statusBar;
                this.mCommandQueue = commandQueue;
                this.mUpdateMonitor.hasBootCompleted();
                this.mInit = true;
                this.mContext = context;
                getNonUILooper();
                for (OpBaseCtrl opBaseCtrl : this.mControls) {
                    if (opBaseCtrl != null) {
                        opBaseCtrl.setCallback(this);
                        opBaseCtrl.init(context);
                        opBaseCtrl.startCtrl();
                    }
                }
                this.mUpdateMonitor.registerCallback(this.mKeyguardUpdateMonitorCallback);
            }
        }
    }

    public void onFingerprintStartedGoingToSleep() {
        this.mIsFinishedScreenTuredOn = false;
    }

    public void onWallpaperChange(Bitmap bitmap) {
        OpBaseCtrl[] opBaseCtrlArr;
        for (OpBaseCtrl opBaseCtrl : this.mControls) {
            if (opBaseCtrl != null && opBaseCtrl.isEnable()) {
                opBaseCtrl.onWallpaperChange(bitmap);
            }
        }
        this.mPhonstatusBar.onWallpaperChange(bitmap);
        ((OpChargingAnimationController) Dependency.get(OpChargingAnimationController.class)).onWallpaperChange(bitmap);
    }

    public void onScreenTurnedOn() {
        OpBaseCtrl[] opBaseCtrlArr;
        this.mIsFinishedScreenTuredOn = true;
        for (OpBaseCtrl opBaseCtrl : this.mControls) {
            if (opBaseCtrl != null && opBaseCtrl.isEnable()) {
                opBaseCtrl.onScreenTurnedOn();
            }
        }
    }

    public Looper getNonUILooper() {
        Looper looper;
        synchronized (this) {
            if (this.mNonUiLooper == null) {
                HandlerThread handlerThread = new HandlerThread("OpLsState thread");
                handlerThread.start();
                this.mNonUiLooper = handlerThread.getLooper();
            }
            looper = this.mNonUiLooper;
        }
        return looper;
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    public StatusBarKeyguardViewManager getStatusBarKeyguardViewManager() {
        return this.mStatusBarKeyguardViewManager;
    }

    public StatusBar getPhoneStatusBar() {
        return this.mPhonstatusBar;
    }

    public ViewGroup getContainer() {
        return this.mContainer;
    }

    public boolean isFinishedScreenTuredOn() {
        return this.mIsFinishedScreenTuredOn;
    }

    public void setBiometricUnlockController(BiometricUnlockController biometricUnlockController) {
        this.mBiometricUnlockController = biometricUnlockController;
    }

    public BiometricUnlockController getBiometricUnlockController() {
        return this.mBiometricUnlockController;
    }

    public void setFpAnimationCtrl(OpFingerprintAnimationCtrl opFingerprintAnimationCtrl) {
        this.mFingerprintAnimationCtrl = opFingerprintAnimationCtrl;
    }

    public OpFingerprintAnimationCtrl getFpAnimationCtrl() {
        return this.mFingerprintAnimationCtrl;
    }
}

package com.oneplus.faceunlock;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SystemSensorManager;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricSourceType;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.util.Log;
import android.util.OpFeatures;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.policy.IKeyguardStateCallback.Stub;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.oneplus.anim.OpCameraAnimateController;
import com.oneplus.faceunlock.internal.IOPFaceSettingService;
import com.oneplus.faceunlock.internal.IOPFacelockCallback;
import com.oneplus.faceunlock.internal.IOPFacelockService;
import com.oneplus.keyguard.OpKeyguardUpdateMonitor;
import com.oneplus.p009os.OpMotorManager;
import com.oneplus.systemui.keyguard.OpKeyguardViewMediator;
import com.oneplus.systemui.statusbar.phone.OpLockIcon;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.OpUtils;
import com.oneplus.util.VibratorSceneUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;

public class OpFacelockController extends KeyguardUpdateMonitorCallback {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    private static final int FAILED_ATTEMPTS_TO_TIMEOUT = (OpUtils.isWeakFaceUnlockEnabled() ? 3 : 5);
    private static int mFaceUnlockNoticeDelay = 3000;
    private static int mMaxMotorUpDuration = 18000;
    private static int mMaxMotorUpTimes = 6;
    private final boolean isLod = OpFeatures.isSupport(new int[]{192});
    /* access modifiers changed from: private */
    public boolean mBinding = false;
    /* access modifiers changed from: private */
    public boolean mBindingSetting = false;
    /* access modifiers changed from: private */
    public boolean mBouncer = false;
    /* access modifiers changed from: private */
    public boolean mBoundToService = false;
    private final BroadcastReceiver mBroadcastReceiver;
    private boolean mCameraLaunching = false;
    private ServiceConnection mConnection;
    /* access modifiers changed from: private */
    public Context mContext;
    private boolean mEnterBouncerAfterScreenOn;
    BiometricUnlockController mFPC;
    /* access modifiers changed from: private */
    public boolean mFaceLockActive = false;
    private HandlerThread mFacelockThread;
    /* access modifiers changed from: private */
    public int mFailAttempts;
    private long mFpFailTimeStamp;
    private FingerprintManager mFpm;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public KeyguardIndicationController mIndicator;
    /* access modifiers changed from: private */
    public boolean mIsGoingToSleep = false;
    /* access modifiers changed from: private */
    public boolean mIsKeyguardShowing = false;
    /* access modifiers changed from: private */
    public boolean mIsScreenOffUnlock = false;
    private boolean mIsScreenTurnedOn = false;
    private boolean mIsScreenTurningOn = false;
    /* access modifiers changed from: private */
    public boolean mIsSleep = false;
    private KeyguardStateCallback mKeyguardStateCallback;
    /* access modifiers changed from: private */
    public KeyguardViewMediator mKeyguardViewMediator;
    private ViewMediatorCallback mKeyguardViewMediatorCallback;
    private final Sensor mLightSensor;
    private final SensorEventListener mLightSensorListener;
    private int mLightingModeBrightness;
    private double mLightingModeBrightnessAdjustment;
    /* access modifiers changed from: private */
    public boolean mLightingModeEnabled;
    /* access modifiers changed from: private */
    public int mLightingModeSensorThreshold;
    private boolean mLockout = false;
    private OpMotorManager mMotorManager;
    private LinkedList<Long> mMotorQueue;
    /* access modifiers changed from: private */
    public boolean mNeedToPendingStopFacelock = false;
    private final IOPFacelockCallback mOPFacelockCallback;
    /* access modifiers changed from: private */
    public final Runnable mOffAuthenticateRunnable;
    private boolean mPendingFacelockWhenBouncer;
    /* access modifiers changed from: private */
    public String mPendingLaunchCameraSource = null;
    private boolean mPendingStopFacelock = false;
    /* access modifiers changed from: private */
    public StatusBar mPhoneStatusBar;
    IPowerManager mPowerManager;
    /* access modifiers changed from: private */
    public final Runnable mResetScreenOnRunnable;
    private SensorManager mSensorManager;
    private int mSensorRate;
    /* access modifiers changed from: private */
    public IOPFacelockService mService;
    private ServiceConnection mSettingConnection;
    /* access modifiers changed from: private */
    public IOPFaceSettingService mSettingService;
    /* access modifiers changed from: private */
    public boolean mSimSecure;
    long mSleepTime;
    private boolean mStartFacelockWhenScreenOn = false;
    /* access modifiers changed from: private */
    public StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    /* access modifiers changed from: private */
    public StatusBarWindowController mStatusBarWindowController;
    /* access modifiers changed from: private */
    public Handler mUIHandler;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mUpdateMonitor;
    /* access modifiers changed from: private */
    public Vibrator mVibrator;
    private IWindowManager mWM;

    private class FacelockHandler extends Handler {
        FacelockHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            String str = "OpFacelockController";
            if (OpFacelockController.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("handleMessage: what:");
                sb.append(message.what);
                sb.append(", bound:");
                sb.append(OpFacelockController.this.mBoundToService);
                sb.append(", active:");
                sb.append(OpFacelockController.this.mFaceLockActive);
                Log.d(str, sb.toString());
            }
            switch (message.what) {
                case 1:
                    if (OpFacelockController.this.mBoundToService) {
                        OpFacelockController.this.handleStartFacelock();
                        break;
                    } else {
                        return;
                    }
                case 2:
                    if (OpFacelockController.this.mBoundToService) {
                        OpFacelockController.this.updateRecognizedState(0, -1);
                        OpFacelockController.this.handleStopFacelock();
                        break;
                    } else {
                        return;
                    }
                case 3:
                    if (OpFacelockController.this.mFaceLockActive && OpFacelockController.this.mBoundToService) {
                        OpFacelockController.this.unlockKeyguard();
                        break;
                    } else {
                        return;
                    }
                    break;
                case 4:
                    if (OpFacelockController.this.mFaceLockActive) {
                        OpFacelockController.this.handleRecognizeFail();
                        break;
                    } else {
                        return;
                    }
                case 5:
                    if (OpFacelockController.this.mFaceLockActive) {
                        OpFacelockController.this.playFacelockIndicationTextAnim();
                        OpFacelockController.this.updateRecognizedState(6, -65536);
                        OpFacelockController.this.handleStopFacelock();
                        break;
                    } else {
                        return;
                    }
                case 6:
                    OpFacelockController.this.handleResetLockout();
                    break;
                case 7:
                    if (OpFacelockController.this.mFaceLockActive && OpFacelockController.this.mBoundToService) {
                        OpFacelockController.this.handleSkipBouncer();
                        break;
                    } else {
                        return;
                    }
                case 8:
                    OpFacelockController.this.handleResetFacelockPending();
                    break;
                case 10:
                    if (OpFacelockController.this.mFaceLockActive) {
                        OpFacelockController.this.playFacelockIndicationTextAnim();
                        OpFacelockController.this.updateRecognizedState(8, -65536);
                        OpFacelockController.this.handleStopFacelock();
                        break;
                    } else {
                        return;
                    }
                case 11:
                    if (OpFacelockController.this.mFaceLockActive) {
                        OpFacelockController.this.playFacelockIndicationTextAnim();
                        OpFacelockController.this.updateRecognizedState(9, -65536);
                        OpFacelockController.this.handleStopFacelock();
                        break;
                    } else {
                        return;
                    }
                case 12:
                    OpFacelockController.this.updateIsFaceAdded();
                    break;
                case 13:
                    if (OpFacelockController.this.mFaceLockActive) {
                        OpFacelockController.this.enterBouncer();
                        OpFacelockController.this.playFacelockIndicationTextAnim();
                        OpFacelockController.this.updateRecognizedState(11, -65536);
                        OpFacelockController.this.handleStopFacelock();
                        break;
                    } else {
                        return;
                    }
                case 14:
                    OpFacelockController.this.handleFaceUnlockNotice();
                    break;
                default:
                    Log.e(str, "Unhandled message");
                    break;
            }
            if (OpFacelockController.DEBUG) {
                Log.d(str, "handleMessage: done");
            }
        }
    }

    private class KeyguardStateCallback extends Stub {
        public void onDisabledStateChanged(boolean z) {
        }

        public void onFingerprintStateChange(boolean z, int i, int i2, int i3) {
        }

        public void onHasLockscreenWallpaperChanged(boolean z) {
        }

        public void onInputRestrictedStateChanged(boolean z) {
        }

        public void onPocketModeActiveChanged(boolean z) {
        }

        public void onShowingStateChanged(boolean z) {
        }

        public void onTrustedChanged(boolean z) {
        }

        private KeyguardStateCallback() {
        }

        public void onSimSecureStateChanged(boolean z) {
            if (OpFacelockController.this.mSimSecure != z) {
                if (!z && OpFacelockController.this.mBoundToService && OpFacelockController.this.mIsKeyguardShowing && OpFacelockController.this.mUpdateMonitor.isAutoFacelockEnabled()) {
                    Log.d("OpFacelockController", "onSimSecure to start");
                    OpFacelockController.this.tryToStartFaceLock(true);
                }
                OpFacelockController.this.mSimSecure = z;
            }
        }
    }

    public OpFacelockController(Context context, KeyguardViewMediator keyguardViewMediator, StatusBar statusBar, StatusBarKeyguardViewManager statusBarKeyguardViewManager, StatusBarWindowController statusBarWindowController, BiometricUnlockController biometricUnlockController) {
        this.mLightingModeSensorThreshold = this.isLod ? 5 : 0;
        this.mLightingModeBrightnessAdjustment = 0.294d;
        this.mLightingModeBrightness = 300;
        this.mLightingModeEnabled = false;
        this.mPendingFacelockWhenBouncer = false;
        this.mSleepTime = 0;
        this.mMotorQueue = new LinkedList<>();
        this.mEnterBouncerAfterScreenOn = false;
        this.mSimSecure = false;
        this.mFpFailTimeStamp = 0;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("com.oneplus.faceunlock.action.FACE_SETTING_CHANGED".equals(intent.getAction())) {
                    OpFacelockController.this.mHandler.removeMessages(12);
                    OpFacelockController.this.mHandler.sendEmptyMessage(12);
                    if (OpFacelockController.DEBUG) {
                        Log.d("OpFacelockController", "intent to update face added");
                    }
                }
            }
        };
        this.mResetScreenOnRunnable = new Runnable() {
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("reset screen on, offUnlock:");
                sb.append(OpFacelockController.this.mIsScreenOffUnlock);
                Log.d("OpFacelockController", sb.toString());
                if (OpFacelockController.this.mIsScreenOffUnlock) {
                    OpFacelockController.this.updateKeyguardAlpha(1, true, true);
                }
            }
        };
        this.mOffAuthenticateRunnable = new Runnable() {
            public void run() {
                OpFacelockController.this.mKeyguardViewMediator.notifyScreenOffAuthenticate(false, OpKeyguardViewMediator.AUTHENTICATE_FACEUNLOCK);
            }
        };
        this.mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d("OpFacelockController", "Connected to Facelock service");
                OpFacelockController.this.mService = IOPFacelockService.Stub.asInterface(iBinder);
                OpFacelockController.this.mBinding = false;
                OpFacelockController.this.mBoundToService = true;
                OpFacelockController.this.tryToStartFaceLock(false);
            }

            public void onServiceDisconnected(ComponentName componentName) {
                Log.e("OpFacelockController", "disconnect from Facelock service");
                OpFacelockController.this.mService = null;
                OpFacelockController.this.mBinding = false;
                OpFacelockController.this.mBoundToService = false;
            }
        };
        this.mSettingConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                OpFacelockController.this.mSettingService = IOPFaceSettingService.Stub.asInterface(iBinder);
                StringBuilder sb = new StringBuilder();
                sb.append("Connected to FaceSetting service, ");
                sb.append(OpFacelockController.this.mSettingService);
                Log.d("OpFacelockController", sb.toString());
                OpFacelockController.this.updateIsFaceAdded();
                OpFacelockController.this.mBindingSetting = false;
            }

            public void onServiceDisconnected(ComponentName componentName) {
                Log.e("OpFacelockController", "disconnect from FaceSetting service");
                OpFacelockController.this.mSettingService = null;
                OpFacelockController.this.mUpdateMonitor.setIsFaceAdded(false);
                OpFacelockController.this.mBindingSetting = false;
            }
        };
        this.mOPFacelockCallback = new IOPFacelockCallback.Stub() {
            public void onBeginRecognize(int i) {
                if (OpFacelockController.this.mFaceLockActive && OpFacelockController.DEBUG) {
                    Log.d("OpFacelockController", "onBeginRecognize");
                }
            }

            public void onCompared(int i, int i2, int i3, int i4, int i5) {
                if (i3 == 2 && OpFacelockController.this.mIsScreenOffUnlock) {
                    if (OpFacelockController.DEBUG) {
                        Log.d("OpFacelockController", "onCompared 2 to remove timeout");
                    }
                    OpFacelockController.this.mUIHandler.removeCallbacks(OpFacelockController.this.mResetScreenOnRunnable);
                    OpFacelockController.this.updateKeyguardAlpha(1, true, true);
                }
            }

            public void onEndRecognize(int i, int i2, int i3) {
                if (OpFacelockController.this.mFaceLockActive) {
                    if (OpFacelockController.this.mIsScreenOffUnlock && i3 != 0) {
                        OpFacelockController.this.mHandler.removeCallbacks(OpFacelockController.this.mOffAuthenticateRunnable);
                        OpFacelockController.this.mKeyguardViewMediator.notifyScreenOffAuthenticate(false, OpKeyguardViewMediator.AUTHENTICATE_FACEUNLOCK);
                    }
                    OpFacelockController.this.mHandler.removeMessages(8);
                    OpFacelockController.this.mNeedToPendingStopFacelock = false;
                    boolean isUnlockingWithBiometricAllowed = OpFacelockController.this.mUpdateMonitor.isUnlockingWithBiometricAllowed();
                    StringBuilder sb = new StringBuilder();
                    sb.append("onEndRecognize, result:");
                    sb.append(i3);
                    sb.append(", keyguardShow:");
                    sb.append(OpFacelockController.this.mIsKeyguardShowing);
                    sb.append(", bouncer:");
                    sb.append(OpFacelockController.this.mBouncer);
                    sb.append(", allowed:");
                    sb.append(isUnlockingWithBiometricAllowed);
                    sb.append(", isSleep:");
                    sb.append(OpFacelockController.this.mIsSleep);
                    sb.append(", simpin:");
                    sb.append(OpFacelockController.this.mUpdateMonitor.isSimPinSecure());
                    sb.append(", pending:");
                    sb.append(OpFacelockController.this.mPendingLaunchCameraSource != null);
                    sb.append(", auto:");
                    sb.append(OpFacelockController.this.mUpdateMonitor.isAutoFacelockEnabled());
                    String str = "OpFacelockController";
                    Log.d(str, sb.toString());
                    OpFacelockController.this.mKeyguardViewMediator.userActivity();
                    String str2 = "face_bright";
                    String str3 = "face";
                    String str4 = "1";
                    if (i3 != 0) {
                        String str5 = "lock_unlock_failed";
                        if (i3 == 2) {
                            OpMdmLogger.log(str5, "face_timeout", str4);
                            Log.d(str, "onEndRecognize: no face");
                            OpFacelockController.this.mHandler.sendEmptyMessage(5);
                        } else if (i3 == 3) {
                            Log.d(str, "onEndRecognize: camera error");
                            if (OpFacelockController.this.mIsScreenOffUnlock) {
                                OpFacelockController.this.mUIHandler.removeCallbacks(OpFacelockController.this.mResetScreenOnRunnable);
                                OpFacelockController.this.updateKeyguardAlpha(1, false, true);
                            }
                            OpFacelockController.this.mHandler.sendEmptyMessage(10);
                        } else if (i3 == 4) {
                            Log.d(str, "onEndRecognize: no permission");
                            if (OpFacelockController.this.mIsScreenOffUnlock) {
                                OpFacelockController.this.mUIHandler.removeCallbacks(OpFacelockController.this.mResetScreenOnRunnable);
                                OpFacelockController.this.updateKeyguardAlpha(1, false, true);
                            }
                            OpFacelockController.this.mHandler.sendEmptyMessage(11);
                        } else {
                            if (OpFacelockController.this.mLightingModeEnabled) {
                                OpMdmLogger.log(str5, str2, str4);
                            } else {
                                OpMdmLogger.log(str5, str3, str4);
                            }
                            VibratorSceneUtils.doVibrateWithSceneMultipleTimes(OpFacelockController.this.mContext, OpFacelockController.this.mVibrator, 1019, 0, 50, 3);
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("onEndRecognize: fail ");
                            sb2.append(OpFacelockController.this.mFailAttempts + 1);
                            sb2.append(" times");
                            Log.d(str, sb2.toString());
                            OpFacelockController.this.mHandler.sendEmptyMessage(4);
                        }
                    } else if (!OpFacelockController.this.mUpdateMonitor.allowShowingLock() || !isUnlockingWithBiometricAllowed || OpFacelockController.this.mIsSleep || OpFacelockController.this.mUpdateMonitor.isSimPinSecure()) {
                        Log.d(str, "not handle recognize");
                        OpFacelockController.this.mHandler.removeMessages(2);
                        OpFacelockController.this.mHandler.sendEmptyMessage(2);
                        if (OpFacelockController.this.mIsScreenOffUnlock) {
                            OpFacelockController.this.mHandler.removeCallbacks(OpFacelockController.this.mOffAuthenticateRunnable);
                            OpFacelockController.this.mKeyguardViewMediator.notifyScreenOffAuthenticate(false, OpKeyguardViewMediator.AUTHENTICATE_FACEUNLOCK);
                        }
                    } else {
                        try {
                            if (!(System.currentTimeMillis() - OpFacelockController.this.getFpFailTimeStamp() <= 2000)) {
                                str4 = "0";
                            }
                            String str6 = "lock_unlock_success";
                            if (OpFacelockController.this.mLightingModeEnabled) {
                                OpMdmLogger.log(str6, str2, str4);
                            } else {
                                OpMdmLogger.log(str6, str3, str4);
                            }
                            OpFacelockController.this.mUpdateMonitor;
                            if (OpKeyguardUpdateMonitor.isMotorCameraSupported()) {
                                OpFacelockController.this.mUpdateMonitor.reportFaceUnlock();
                            }
                        } catch (Exception e) {
                            StringBuilder sb3 = new StringBuilder();
                            sb3.append("Exception e = ");
                            sb3.append(e.toString());
                            Log.w(str, sb3.toString());
                        }
                        if (OpUtils.isWeakFaceUnlockEnabled()) {
                            OpFacelockController.this.mKeyguardViewMediator.reportBiometricUnlocked(0, 0);
                        }
                        if (!OpFacelockController.this.mUpdateMonitor.isAutoFacelockEnabled() && !OpFacelockController.this.mPhoneStatusBar.isBouncerShowing()) {
                            OpFacelockController.this.mUpdateMonitor;
                            if (!OpKeyguardUpdateMonitor.isMotorCameraSupported()) {
                                if (OpFacelockController.this.mPendingLaunchCameraSource == null || OpFacelockController.this.mUpdateMonitor.isAutoFacelockEnabled()) {
                                    Log.d(str, "onEndRecognize, result ok to skip bouncer");
                                    OpFacelockController.this.mHandler.sendEmptyMessage(7);
                                    if (OpFacelockController.this.mIsScreenOffUnlock) {
                                        OpFacelockController.this.mUIHandler.removeCallbacks(OpFacelockController.this.mResetScreenOnRunnable);
                                        OpFacelockController.this.updateKeyguardAlpha(1, false, false);
                                        OpFacelockController.this.mHandler.removeCallbacks(OpFacelockController.this.mOffAuthenticateRunnable);
                                        OpFacelockController.this.mKeyguardViewMediator.notifyScreenOffAuthenticate(false, OpKeyguardViewMediator.AUTHENTICATE_FACEUNLOCK);
                                    }
                                } else {
                                    Log.d(str, "onEndRecognize, result ok to unlock and camera");
                                    OpFacelockController.this.mUIHandler.removeCallbacks(OpFacelockController.this.mResetScreenOnRunnable);
                                    OpFacelockController.this.mHandler.sendEmptyMessage(3);
                                }
                            }
                        }
                        Log.d(str, "onEndRecognize, result ok to unlock");
                        OpFacelockController.this.mUIHandler.removeCallbacks(OpFacelockController.this.mResetScreenOnRunnable);
                        OpFacelockController.this.mHandler.sendEmptyMessage(3);
                    }
                }
            }
        };
        this.mLightSensorListener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int i) {
            }

            public void onSensorChanged(SensorEvent sensorEvent) {
                float f = sensorEvent.values[0];
                if (OpFacelockController.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("light sensor: lux:");
                    sb.append(f);
                    sb.append(", already lighting:");
                    sb.append(OpFacelockController.this.mLightingModeEnabled);
                    sb.append(", threshold:");
                    sb.append(OpFacelockController.this.mLightingModeSensorThreshold);
                    Log.d("OpFacelockController", sb.toString());
                }
                if (f <= ((float) OpFacelockController.this.mLightingModeSensorThreshold) && !OpFacelockController.this.mLightingModeEnabled) {
                    OpFacelockController.this.updateFacelockLightMode(true);
                }
            }
        };
        Log.d("OpFacelockController", "new facelock");
        this.mContext = context;
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mUpdateMonitor.registerCallback(this);
        this.mKeyguardViewMediator = keyguardViewMediator;
        this.mPhoneStatusBar = statusBar;
        this.mKeyguardViewMediatorCallback = keyguardViewMediator.getViewMediatorCallback();
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
        this.mStatusBarWindowController = statusBarWindowController;
        this.mFacelockThread = new HandlerThread("FacelockThread");
        this.mFacelockThread.start();
        this.mHandler = new FacelockHandler(this.mFacelockThread.getLooper());
        this.mUIHandler = new Handler();
        this.mWM = WindowManagerGlobal.getWindowManagerService();
        this.mSensorManager = new SystemSensorManager(this.mContext, this.mHandler.getLooper());
        this.mLightSensor = this.mSensorManager.getDefaultSensor(5);
        this.mSensorRate = this.mContext.getResources().getInteger(17694739);
        String str = "power";
        this.mPowerManager = IPowerManager.Stub.asInterface(ServiceManager.getService(str));
        this.mLightingModeBrightness = ((PowerManager) this.mContext.getSystemService(str)).getMaximumScreenBrightnessSetting();
        this.mFPC = biometricUnlockController;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.oneplus.faceunlock.action.FACE_SETTING_CHANGED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        this.mKeyguardStateCallback = new KeyguardStateCallback();
        keyguardViewMediator.addStateMonitorCallback(this.mKeyguardStateCallback);
        this.mFpm = (FingerprintManager) this.mContext.getSystemService("fingerprint");
        new OpCameraAnimateController(this.mContext).init();
    }

    private int downMotorBySystemApp() {
        String str = "OpFacelockController";
        int i = -999;
        try {
            i = this.mMotorManager.downMotorBySystemApp("systemui_faceunlock");
            StringBuilder sb = new StringBuilder();
            sb.append("downMotor ");
            sb.append(i);
            Log.d(str, sb.toString());
            return i;
        } catch (Exception e) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("downMotor fail, ");
            sb2.append(e.getMessage());
            Log.e(str, sb2.toString());
            return i;
        }
    }

    private int upMotorBySystemApp() {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        int motorStateBySystemApp = this.mMotorManager.getMotorStateBySystemApp();
        String str = "OpFacelockController";
        if (motorStateBySystemApp <= 0) {
            try {
                if (checkReachUpTimes(elapsedRealtime)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("queue size: ");
                    sb.append(this.mMotorQueue.size());
                    sb.append("::::");
                    sb.append(Arrays.toString(this.mMotorQueue.toArray()));
                    Log.d(str, sb.toString());
                    return -999;
                }
            } catch (Exception e) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("upMotor fail, ");
                sb2.append(e.getMessage());
                Log.e(str, sb2.toString());
                return -999;
            }
        }
        int upMotorBySystemApp = this.mMotorManager.upMotorBySystemApp("systemui_faceunlock");
        StringBuilder sb3 = new StringBuilder();
        sb3.append("upMotor ");
        sb3.append(upMotorBySystemApp);
        sb3.append(", time:");
        sb3.append(elapsedRealtime);
        sb3.append(", state:");
        sb3.append(motorStateBySystemApp);
        Log.d(str, sb3.toString());
        return upMotorBySystemApp;
    }

    private boolean checkReachUpTimes(long j) {
        if (this.mMotorQueue.size() == mMaxMotorUpTimes) {
            this.mMotorQueue.removeFirst();
            this.mMotorQueue.addLast(Long.valueOf(j));
            if (((Long) this.mMotorQueue.getLast()).longValue() - ((Long) this.mMotorQueue.getFirst()).longValue() <= ((long) mMaxMotorUpDuration)) {
                return true;
            }
        } else if (this.mMotorQueue.size() == mMaxMotorUpTimes - 1) {
            this.mMotorQueue.addLast(Long.valueOf(j));
            if (((Long) this.mMotorQueue.getLast()).longValue() - ((Long) this.mMotorQueue.getFirst()).longValue() <= ((long) mMaxMotorUpDuration)) {
                return true;
            }
        } else if (this.mMotorQueue.size() < 0 || this.mMotorQueue.size() > mMaxMotorUpTimes - 2) {
            Log.w("OpFacelockController", "error queue state");
        } else {
            this.mMotorQueue.addLast(Long.valueOf(j));
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void handleFaceUnlockNotice() {
        this.mHandler.removeMessages(14);
        if (OpKeyguardUpdateMonitor.isMotorCameraSupported() && this.mIsKeyguardShowing && !this.mBouncer) {
            KeyguardIndicationController keyguardIndicationController = this.mIndicator;
            if (keyguardIndicationController != null) {
                boolean isShowingText = keyguardIndicationController.isShowingText();
                StringBuilder sb = new StringBuilder();
                sb.append("handleNotice, ");
                sb.append(isShowingText);
                Log.d("OpFacelockController", sb.toString());
                if (!isShowingText) {
                    this.mUIHandler.post(new Runnable() {
                        public void run() {
                            if (OpFacelockController.this.mPhoneStatusBar != null) {
                                OpFacelockController.this.mPhoneStatusBar.onEmptySpaceClick();
                            }
                        }
                    });
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateRecognizedState(int i, int i2) {
        if (!this.mLockout) {
            this.mUpdateMonitor.notifyFacelockStateChanged(i);
            updateNotifyMessage(i, i2);
            if (this.mUpdateMonitor.isFacelockDisabled()) {
                this.mLockout = true;
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleResetLockout() {
        this.mLockout = false;
        if (this.mBoundToService && canLaunchFacelock()) {
            updateRecognizedState(5, -1);
        }
    }

    /* access modifiers changed from: private */
    public void handleSkipBouncer() {
        if (DEBUG) {
            Log.d("OpFacelockController", "handleSkipBouncer");
        }
        this.mFailAttempts = 0;
        this.mMotorQueue.clear();
        updateRecognizedState(2, -1);
        handleStopFacelock();
    }

    /* access modifiers changed from: private */
    public void handleRecognizeFail() {
        boolean z = true;
        this.mFailAttempts++;
        int i = this.mFailAttempts % FAILED_ATTEMPTS_TO_TIMEOUT != 0 ? 7 : 1;
        if (this.mFailAttempts >= 3) {
            if (this.mPhoneStatusBar != null) {
                if (DEBUG) {
                    Log.d("OpFacelockController", "enter Bouncer");
                }
                enterBouncer();
            }
            z = false;
        }
        if (z) {
            playFacelockIndicationTextAnim();
        }
        updateRecognizedState(i, -65536);
        handleStopFacelock();
    }

    /* access modifiers changed from: private */
    public void playFacelockIndicationTextAnim() {
        StatusBar statusBar = this.mPhoneStatusBar;
        if (statusBar != null && !statusBar.isBouncerShowing()) {
            this.mUIHandler.post(new Runnable() {
                public void run() {
                    OpFacelockController.this.mPhoneStatusBar.startFacelockFailAnimation();
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void enterBouncer() {
        Log.d("OpFacelockController", "handle enter Bouncer");
        this.mEnterBouncerAfterScreenOn = false;
        this.mUIHandler.post(new Runnable() {
            public void run() {
                OpFacelockController.this.mStatusBarKeyguardViewManager.showBouncer(false);
                if (!OpFacelockController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    OpFacelockController.this.mPhoneStatusBar.animateCollapsePanels(0, true, false, 1.3f);
                }
            }
        });
    }

    private boolean isWakingUpReasonSupported(String str) {
        if ("com.android.systemui:CAMERA_GESTURE_CIRCLE".equals(str)) {
            return true;
        }
        if ((OpKeyguardUpdateMonitor.isMotorCameraSupported() || OpUtils.isSupportZVibrationMotor()) && !"android.policy:POWER".equals(str) && !"android.policy:DOUBLE_TAP".equals(str)) {
            return true;
        }
        return true;
    }

    public void onPreStartedWakingUp() {
        String str;
        String str2 = "OpFacelockController";
        try {
            str = this.mPowerManager.getWakingUpReason();
        } catch (RemoteException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("getWakingUpReason,");
            sb.append(e.getMessage());
            Log.e(str2, sb.toString());
            str = "android.policy:POWER";
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("onPreStartedWakingUp, bound:");
        sb2.append(this.mBoundToService);
        sb2.append(", pending:");
        sb2.append(this.mPendingFacelockWhenBouncer);
        sb2.append(", bouncerRec:");
        sb2.append(this.mUpdateMonitor.isBouncerRecognizeEnabled());
        sb2.append(", fp:");
        sb2.append(this.mFPC.isWakeAndUnlock());
        sb2.append(", reason:");
        sb2.append(str);
        Log.d(str2, sb2.toString());
        this.mIsSleep = false;
        if (this.mBoundToService && canLaunchFacelock() && (!OpKeyguardUpdateMonitor.isMotorCameraSupported() || (!this.mUpdateMonitor.isBouncerRecognizeEnabled() && !this.mFPC.isWakeAndUnlock()))) {
            if (!isWakingUpReasonSupported(str)) {
                updateRecognizedState(12, -1);
                return;
            }
            if (this.mPendingFacelockWhenBouncer) {
                updateRecognizedState(3, -1);
            }
            this.mHandler.removeMessages(2);
            this.mHandler.removeMessages(1);
            this.mHandler.sendEmptyMessage(1);
        }
    }

    public void onStartedWakingUp() {
        String str;
        String str2 = "OpFacelockController";
        try {
            str = this.mPowerManager.getWakingUpReason();
        } catch (RemoteException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("getWakingUpReason,");
            sb.append(e.getMessage());
            Log.e(str2, sb.toString());
            str = "android.policy:POWER";
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("onStartedWakingUp, bound:");
        sb2.append(this.mBoundToService);
        sb2.append(", lockout:");
        sb2.append(this.mLockout);
        sb2.append(", bouncerRec:");
        sb2.append(this.mUpdateMonitor.isBouncerRecognizeEnabled());
        sb2.append(", fp:");
        sb2.append(this.mFPC.isWakeAndUnlock());
        sb2.append(", reason:");
        sb2.append(str);
        sb2.append(", notice:");
        sb2.append(this.mUpdateMonitor.getFacelockNoticeEnabled());
        Log.d(str2, sb2.toString());
        this.mIsSleep = false;
        if (this.mBoundToService && canLaunchFacelock()) {
            if (OpKeyguardUpdateMonitor.isMotorCameraSupported() && (this.mUpdateMonitor.isBouncerRecognizeEnabled() || this.mFPC.isWakeAndUnlock())) {
                if (this.mUpdateMonitor.getFacelockNoticeEnabled()) {
                    this.mHandler.removeMessages(14);
                    this.mHandler.sendEmptyMessageDelayed(14, (long) mFaceUnlockNoticeDelay);
                }
            } else if (isWakingUpReasonSupported(str)) {
                this.mHandler.removeMessages(2);
                this.mHandler.removeMessages(1);
                this.mHandler.sendEmptyMessage(1);
            }
        }
    }

    public void onFacelockLightingChanged(boolean z) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onLightChanged ");
            sb.append(z);
            Log.d("OpFacelockController", sb.toString());
        }
    }

    public void onScreenTurningOn() {
        if (DEBUG) {
            Log.d("OpFacelockController", "onScreenTurningOn");
        }
        this.mIsScreenTurningOn = true;
    }

    public void onScreenTurnedOn() {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onScreenTurnedOn, ");
            sb.append(this.mStartFacelockWhenScreenOn);
            String str = ", ";
            sb.append(str);
            sb.append(this.mIsSleep);
            sb.append(str);
            sb.append(this.mEnterBouncerAfterScreenOn);
            Log.d("OpFacelockController", sb.toString());
        }
        this.mIsScreenTurnedOn = true;
        if (this.mEnterBouncerAfterScreenOn) {
            enterBouncer();
        }
        if (this.mStartFacelockWhenScreenOn) {
            this.mStartFacelockWhenScreenOn = false;
            if (canLaunchFacelock()) {
                this.mIsSleep = false;
                if (this.mBoundToService) {
                    this.mHandler.removeMessages(2);
                    this.mHandler.removeMessages(1);
                    this.mHandler.sendEmptyMessage(1);
                }
            }
        }
    }

    public void onScreenTurnedOff() {
        if (DEBUG) {
            Log.d("OpFacelockController", "onScreenTurnedOff");
        }
        this.mIsScreenTurnedOn = false;
        this.mIsScreenTurningOn = false;
        this.mEnterBouncerAfterScreenOn = false;
    }

    public void onPreStartedGoingToSleep() {
        if (DEBUG) {
            Log.d("OpFacelockController", "onPreStartedGoingToSleep");
        }
        this.mIsSleep = true;
    }

    public void onStartedGoingToSleep(int i) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onStartedGoingToSleep, ");
            sb.append(i);
            sb.append(", bound:");
            sb.append(this.mBoundToService);
            Log.d("OpFacelockController", sb.toString());
        }
        this.mIsGoingToSleep = true;
        this.mStartFacelockWhenScreenOn = false;
        this.mCameraLaunching = false;
        this.mIsSleep = true;
        this.mHandler.removeMessages(14);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(2);
        this.mPendingFacelockWhenBouncer = false;
        this.mSleepTime = SystemClock.uptimeMillis();
    }

    public void onFinishedGoingToSleep(int i) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onFinishedGoingToSleep, ");
            sb.append(i);
            Log.d("OpFacelockController", sb.toString());
        }
        this.mIsGoingToSleep = false;
        this.mLightingModeBrightness = SystemProperties.getInt("persist.sys.facelock.bright", this.mLightingModeBrightness);
        int i2 = SystemProperties.getInt("persist.sys.facelock.lsensor", 0);
        if (i2 > 0) {
            this.mLightingModeSensorThreshold = i2;
        }
        mMaxMotorUpTimes = SystemProperties.getInt("persist.sys.facelock.uptimes", 6);
        mMaxMotorUpDuration = SystemProperties.getInt("persist.sys.facelock.updura", 18000);
    }

    public void onDreamingStateChanged(boolean z) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onDreamingStateChanged, ");
            sb.append(z);
            Log.d("OpFacelockController", sb.toString());
        }
    }

    public void onUserSwitchComplete(int i) {
        if (i != 0) {
            stopFacelock();
            return;
        }
        Log.d("OpFacelockController", "user switch to owner");
        tryToStartFaceLock(false);
    }

    public void onDeviceProvisioned() {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onDeviceProvisioned, bound:");
            sb.append(this.mBoundToService);
            Log.d("OpFacelockController", sb.toString());
        }
        if (!this.mBoundToService) {
            bindFacelock();
        }
        this.mHandler.removeMessages(12);
        this.mHandler.sendEmptyMessage(12);
    }

    public void onStrongAuthStateChanged(int i) {
        if (OpUtils.isCustomFingerprint() && !canLaunchFacelock()) {
            if (this.mUpdateMonitor.isFacelockAvailable() || this.mUpdateMonitor.isFacelockRecognizing()) {
                Log.d("OpFacelockController", "onStrongAuthStateChanged to stop");
                stopFacelock();
            }
        }
    }

    public void onKeyguardVisibilityChanged(boolean z) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onKeyguardVisibilityChanged, show:");
            sb.append(z);
            sb.append(", bound:");
            sb.append(this.mBoundToService);
            Log.d("OpFacelockController", sb.toString());
        }
        if (this.mIsKeyguardShowing != z) {
            if (!this.mBoundToService) {
                bindFacelock();
            }
            this.mHandler.removeMessages(12);
            this.mHandler.sendEmptyMessage(12);
            if (!z) {
                this.mHandler.removeMessages(14);
                this.mStartFacelockWhenScreenOn = false;
                this.mCameraLaunching = false;
                this.mNeedToPendingStopFacelock = false;
                this.mHandler.removeMessages(1);
                this.mHandler.sendEmptyMessage(2);
            } else if (!this.mIsKeyguardShowing && this.mBoundToService && canLaunchFacelock()) {
                if (!OpKeyguardUpdateMonitor.isMotorCameraSupported() && !OpUtils.isSupportZVibrationMotor()) {
                    this.mHandler.removeMessages(2);
                    this.mHandler.removeMessages(1);
                    this.mHandler.sendEmptyMessage(1);
                } else if (this.mUpdateMonitor.isAutoFacelockEnabled() || OpUtils.isSupportZVibrationMotor()) {
                    updateRecognizedState(12, -1);
                }
            }
            this.mIsKeyguardShowing = z;
            if (!z) {
                this.mPendingFacelockWhenBouncer = false;
            }
        }
    }

    public boolean tryToStartFaceLockInBouncer() {
        boolean userCanSkipBouncer = this.mUpdateMonitor.getUserCanSkipBouncer(KeyguardUpdateMonitor.getCurrentUser());
        StringBuilder sb = new StringBuilder();
        sb.append("startInBouncer, bound:");
        sb.append(this.mBoundToService);
        sb.append(", ");
        sb.append(canLaunchFacelock());
        sb.append(", skip:");
        sb.append(userCanSkipBouncer);
        Log.d("OpFacelockController", sb.toString());
        if (!canLaunchFacelock() || userCanSkipBouncer) {
            return false;
        }
        if (this.mBoundToService) {
            this.mHandler.removeMessages(2);
            this.mHandler.removeMessages(1);
            this.mHandler.sendEmptyMessage(1);
        }
        return true;
    }

    public void onKeyguardBouncerChanged(boolean z) {
        String str = "OpFacelockController";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onKeyguardBouncerChanged , bouncer:");
            sb.append(z);
            sb.append(", show:");
            sb.append(this.mIsKeyguardShowing);
            sb.append(", skip:");
            sb.append(this.mUpdateMonitor.canSkipBouncerByFacelock());
            sb.append(", unlocking:");
            sb.append(this.mUpdateMonitor.isFacelockUnlocking());
            sb.append(", bouncerRec:");
            sb.append(this.mUpdateMonitor.isBouncerRecognizeEnabled());
            sb.append(", type:");
            sb.append(this.mUpdateMonitor.getFacelockRunningType());
            Log.d(str, sb.toString());
        }
        this.mBouncer = z;
        if (OpKeyguardUpdateMonitor.isMotorCameraSupported() && !this.mUpdateMonitor.isFacelockUnlocking()) {
            if (this.mIsKeyguardShowing && z) {
                if (this.mUpdateMonitor.isBouncerRecognizeEnabled()) {
                    tryToStartFaceLockInBouncer();
                } else if (this.mUpdateMonitor.getFacelockRunningType() == 0) {
                    tryToStartFaceLockInBouncer();
                }
            }
        } else if (this.mIsKeyguardShowing || !z) {
            if (OpUtils.isSupportZVibrationMotor() && this.mUpdateMonitor.isFacelockWaitingTap()) {
                tryToStartFaceLock(false);
            } else if (!this.mUpdateMonitor.isAutoFacelockEnabled() && this.mFaceLockActive) {
                updateRecognizedState(3, -1);
            }
            if (this.mIsKeyguardShowing && z) {
                if (this.mUpdateMonitor.canSkipBouncerByFacelock()) {
                    this.mFPC.startWakeAndUnlockForFace(6);
                } else if (this.mUpdateMonitor.isFacelockUnlocking()) {
                    Log.d(str, "just keyguardDone");
                    this.mKeyguardViewMediator.keyguardDone();
                }
            }
        } else {
            tryToStartFaceLock(false);
        }
    }

    public void onClearFailedFacelockAttempts() {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onClearFailedFacelockAttempts, failed:");
            sb.append(this.mFailAttempts);
            sb.append(", lockout:");
            sb.append(this.mLockout);
            Log.d("OpFacelockController", sb.toString());
        }
        this.mFailAttempts = 0;
        this.mLockout = false;
        this.mMotorQueue.clear();
    }

    public void onSystemReady() {
        if (DEBUG) {
            Log.d("OpFacelockController", "onSystemReady");
        }
        this.mMotorManager = new OpMotorManager(this.mContext);
        bindFacelock();
        bindFacelockSetting();
    }

    public void onPasswordLockout() {
        if (DEBUG) {
            Log.d("OpFacelockController", "onPasswordLockout");
        }
        stopFacelock();
    }

    public boolean tryToStartFaceLock(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("tryToStartFaceLock, bound:");
        sb.append(this.mBoundToService);
        sb.append(", motor:");
        sb.append(z);
        Log.d("OpFacelockController", sb.toString());
        if ((OpKeyguardUpdateMonitor.isMotorCameraSupported() && !z) || !canLaunchFacelock()) {
            return false;
        }
        if (this.mBoundToService) {
            this.mHandler.removeMessages(2);
            this.mHandler.removeMessages(1);
            this.mHandler.sendEmptyMessage(1);
        }
        return true;
    }

    public boolean canLaunchFacelock() {
        String str = "OpFacelockController";
        if (this.mCameraLaunching) {
            Log.d(str, "not start when camera launching");
            return false;
        } else if (!this.mUpdateMonitor.isFacelockAllowed()) {
            if (DEBUG) {
                Log.d(str, "not allow to facelock");
            }
            return false;
        } else if (!isFacelockTimeout()) {
            return true;
        } else {
            Log.d(str, "timeout, not allow to facelock");
            return false;
        }
    }

    public boolean isFacelockRunning() {
        return this.mFaceLockActive;
    }

    /* access modifiers changed from: private */
    public void updateIsFaceAdded() {
        int i;
        String str = "OpFacelockController";
        IOPFaceSettingService iOPFaceSettingService = this.mSettingService;
        if (iOPFaceSettingService == null) {
            this.mUpdateMonitor.setIsFaceAdded(false);
            bindFacelockSetting();
            return;
        }
        boolean z = true;
        try {
            i = iOPFaceSettingService.checkState(0);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateIsFaceAdded fail: ");
            sb.append(e.getMessage());
            Log.d(str, sb.toString());
            i = 1;
        }
        boolean isFaceAdded = this.mUpdateMonitor.isFaceAdded();
        if (i != 0) {
            z = false;
        }
        if (DEBUG) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("isFaceAdded:");
            sb2.append(z);
            sb2.append(", pre:");
            sb2.append(isFaceAdded);
            Log.d(str, sb2.toString());
        }
        if (!this.mUpdateMonitor.isFaceAdded() && z && !this.mUpdateMonitor.isUnlockingWithBiometricAllowed() && this.mStatusBarKeyguardViewManager.getBouncer() != null) {
            this.mUpdateMonitor.setIsFaceAdded(z);
            this.mStatusBarKeyguardViewManager.getBouncer().updateBouncerPromptReason();
            Log.d(str, "face is added and not allowed, update Prompt reason");
        }
        this.mUpdateMonitor.setIsFaceAdded(z);
        if (z != isFaceAdded) {
            if (z) {
                tryToStartFaceLock(false);
            } else {
                stopFacelock();
            }
        }
    }

    public boolean notifyCameraLaunching(boolean z, String str) {
        if (this.mIsKeyguardShowing) {
            this.mCameraLaunching = z;
        }
        boolean z2 = false;
        StringBuilder sb = new StringBuilder();
        sb.append("notifyCameraLaunching, source:");
        sb.append(str);
        sb.append(", facelockActive:");
        sb.append(this.mFaceLockActive);
        sb.append(", keyguard:");
        sb.append(this.mIsKeyguardShowing);
        Log.d("OpFacelockController", sb.toString());
        if (this.mFaceLockActive) {
            if (str != null) {
                this.mPendingLaunchCameraSource = str;
                z2 = true;
            }
            stopFacelock();
        }
        return z2;
    }

    /* access modifiers changed from: private */
    public void handleStartFacelock() {
        this.mHandler.removeMessages(14);
        boolean isCameraErrorState = this.mUpdateMonitor.isCameraErrorState();
        StringBuilder sb = new StringBuilder();
        sb.append("handle startFacelock, active:");
        sb.append(this.mFaceLockActive);
        sb.append(", pendingStop:");
        sb.append(this.mPendingStopFacelock);
        sb.append(", live wp:");
        sb.append(this.mStatusBarWindowController.isShowingLiveWallpaper(false));
        sb.append(", cameraError:");
        sb.append(isCameraErrorState);
        sb.append(", showing:");
        sb.append(this.mIsKeyguardShowing);
        sb.append(", pending:");
        sb.append(this.mPendingFacelockWhenBouncer);
        sb.append(", on:");
        sb.append(this.mIsScreenTurnedOn);
        Log.d("OpFacelockController", sb.toString());
        if (this.mService == null) {
            Log.d("OpFacelockController", "not start Facelock");
        } else if (isCameraErrorState) {
            Log.d("OpFacelockController", "not start when camera error");
        } else if (this.mPendingFacelockWhenBouncer) {
            Log.d("OpFacelockController", "pending in bouncer");
        } else if (this.mFaceLockActive) {
            this.mPendingStopFacelock = false;
            updateRecognizedState(3, -1);
        } else if (!this.mIsScreenTurnedOn && this.mKeyguardViewMediator.isScreenOffAuthenticating()) {
            this.mStartFacelockWhenScreenOn = true;
            Log.d("OpFacelockController", "pending start to screen on");
        } else if (!OpKeyguardUpdateMonitor.isMotorCameraSupported() || upMotorBySystemApp() != -999) {
            this.mStartFacelockWhenScreenOn = false;
            updateRecognizedState(3, -1);
            this.mFaceLockActive = true;
            this.mNeedToPendingStopFacelock = true;
            if (!OpKeyguardUpdateMonitor.isMotorCameraSupported() && !this.mIsScreenTurningOn && !this.mIsScreenTurnedOn && !this.mKeyguardViewMediator.isScreenOffAuthenticating() && this.mIsKeyguardShowing && this.mUpdateMonitor.isAutoFacelockEnabled()) {
                this.mIsScreenOffUnlock = true;
                updateKeyguardAlpha(0, true, false);
                this.mUIHandler.removeCallbacks(this.mResetScreenOnRunnable);
                this.mUIHandler.postDelayed(this.mResetScreenOnRunnable, 600);
            }
            synchronized (this) {
                try {
                    this.mService.registerCallback(this.mOPFacelockCallback);
                    this.mService.prepare();
                    this.mService.startFaceUnlock(0);
                } catch (RemoteException e) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("startFacelock fail, ");
                    sb2.append(e.getMessage());
                    Log.e("OpFacelockController", sb2.toString());
                    this.mNeedToPendingStopFacelock = false;
                    this.mHandler.sendEmptyMessage(4);
                    return;
                } catch (NullPointerException e2) {
                    String str = "OpFacelockController";
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("startFacelock mService null, ");
                    sb3.append(e2.getMessage());
                    Log.e(str, sb3.toString());
                    this.mNeedToPendingStopFacelock = false;
                    this.mHandler.sendEmptyMessage(4);
                    return;
                }
            }
            this.mHandler.removeMessages(8);
            this.mHandler.sendEmptyMessageDelayed(8, 500);
            registerLightSensor(true);
        } else {
            if (!this.mIsScreenTurnedOn) {
                this.mEnterBouncerAfterScreenOn = true;
            } else {
                enterBouncer();
            }
            updateRecognizedState(10, -65536);
            Log.d("OpFacelockController", "not start motor for up limited");
            this.mMotorQueue.clear();
        }
    }

    /* access modifiers changed from: private */
    public void handleResetFacelockPending() {
        this.mNeedToPendingStopFacelock = false;
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("handleResetFacelockPending, ");
            sb.append(this.mPendingStopFacelock);
            Log.d("OpFacelockController", sb.toString());
        }
        if (this.mPendingStopFacelock) {
            handleStopFacelock();
        }
    }

    private void stopFacelock() {
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(2);
    }

    /* access modifiers changed from: private */
    public void handleStopFacelock() {
        if (!this.mFaceLockActive) {
            StringBuilder sb = new StringBuilder();
            sb.append("not stop facelock, active:");
            sb.append(this.mFaceLockActive);
            Log.d("OpFacelockController", sb.toString());
        } else if (this.mNeedToPendingStopFacelock) {
            this.mPendingStopFacelock = true;
            if (DEBUG) {
                Log.d("OpFacelockController", "pending stop facelock");
            }
        } else {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("handle stopFacelock, pending camera:");
            sb2.append(this.mPendingLaunchCameraSource);
            Log.d("OpFacelockController", sb2.toString());
            this.mHandler.removeMessages(8);
            this.mPendingStopFacelock = false;
            this.mFaceLockActive = false;
            stopFacelockLightMode();
            if (OpKeyguardUpdateMonitor.isMotorCameraSupported()) {
                downMotorBySystemApp();
            }
            synchronized (this) {
                try {
                    this.mService.unregisterCallback(this.mOPFacelockCallback);
                    this.mService.stopFaceUnlock(0);
                    this.mService.release();
                } catch (RemoteException e) {
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("stopFacelock fail, ");
                    sb3.append(e.getMessage());
                    Log.e("OpFacelockController", sb3.toString());
                } catch (NullPointerException e2) {
                    String str = "OpFacelockController";
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append("stopFacelock mService null, ");
                    sb4.append(e2.getMessage());
                    Log.e(str, sb4.toString());
                }
            }
            final String str2 = this.mPendingLaunchCameraSource;
            if (str2 != null) {
                this.mUIHandler.post(new Runnable() {
                    public void run() {
                        OpFacelockController.this.launchCamera(str2);
                    }
                });
                this.mPendingLaunchCameraSource = null;
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateKeyguardAlpha(final int i, boolean z, boolean z2) {
        StringBuilder sb = new StringBuilder();
        sb.append("update alpha:");
        sb.append(i);
        String str = ", ";
        sb.append(str);
        sb.append(this.mIsScreenOffUnlock);
        sb.append(", live wp:");
        sb.append(this.mStatusBarWindowController.isShowingLiveWallpaper(false));
        sb.append(str);
        sb.append(z2);
        Log.d("OpFacelockController", sb.toString());
        if (i == 0 && z) {
            this.mHandler.removeCallbacks(this.mOffAuthenticateRunnable);
            this.mKeyguardViewMediator.notifyScreenOffAuthenticate(true, OpKeyguardViewMediator.AUTHENTICATE_FACEUNLOCK, 1);
        }
        this.mUIHandler.post(new Runnable() {
            public void run() {
                if (!OpFacelockController.this.mStatusBarWindowController.isShowingLiveWallpaper(false)) {
                    OpFacelockController.this.mKeyguardViewMediator.changePanelAlpha(i, OpKeyguardViewMediator.AUTHENTICATE_FACEUNLOCK);
                    OpFacelockController.this.mStatusBarKeyguardViewManager.getViewRootImpl().setReportNextDraw();
                }
            }
        });
        if (i == 1) {
            this.mIsScreenOffUnlock = false;
            this.mUpdateMonitor.isFingerprintEnrolled(KeyguardUpdateMonitor.getCurrentUser());
            if (z) {
                int i2 = SystemClock.uptimeMillis() - this.mSleepTime > 5000 ? 10 : 100;
                if (OpUtils.isCustomFingerprint()) {
                    this.mHandler.removeCallbacks(this.mOffAuthenticateRunnable);
                    this.mHandler.postDelayed(this.mOffAuthenticateRunnable, (long) i2);
                    return;
                }
                this.mHandler.removeCallbacks(this.mOffAuthenticateRunnable);
                if (z2) {
                    this.mHandler.postDelayed(this.mOffAuthenticateRunnable, (long) i2);
                } else {
                    this.mKeyguardViewMediator.notifyScreenOffAuthenticate(false, OpKeyguardViewMediator.AUTHENTICATE_FACEUNLOCK);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void unlockKeyguard() {
        final boolean isShowingLiveWallpaper = this.mStatusBarWindowController.isShowingLiveWallpaper(false);
        final boolean isBouncerShowing = this.mPhoneStatusBar.isBouncerShowing();
        boolean isDeviceInteractive = this.mUpdateMonitor.isDeviceInteractive();
        StringBuilder sb = new StringBuilder();
        sb.append("unlockKeyguard, bouncer:");
        sb.append(isBouncerShowing);
        sb.append(", live wp:");
        sb.append(isShowingLiveWallpaper);
        sb.append(", interactive = ");
        sb.append(isDeviceInteractive);
        sb.append(", offUnlock:");
        sb.append(this.mIsScreenOffUnlock);
        Log.d("OpFacelockController", sb.toString());
        this.mFailAttempts = 0;
        this.mMotorQueue.clear();
        this.mUpdateMonitor.hideFODDim();
        this.mUpdateMonitor.onFacelockUnlocking(true);
        this.mUpdateMonitor.notifyFacelockStateChanged(4);
        this.mUIHandler.post(new Runnable() {
            public void run() {
                int i = 5;
                if (OpFacelockController.this.mIsScreenOffUnlock && !isShowingLiveWallpaper) {
                    i = 1;
                } else if (isBouncerShowing) {
                    i = 6;
                } else {
                    OpFacelockController.this.mUpdateMonitor;
                    if (!OpKeyguardUpdateMonitor.isMotorCameraSupported() && !isShowingLiveWallpaper && OpFacelockController.this.mUpdateMonitor.isDeviceInteractive()) {
                        OpFacelockController.this.mKeyguardViewMediator.onWakeAndUnlocking();
                        i = 0;
                    }
                }
                OpFacelockController.this.resetFPTimeout();
                OpFacelockController.this.mFPC.startWakeAndUnlockForFace(i);
            }
        });
        this.mHandler.removeCallbacks(this.mOffAuthenticateRunnable);
        this.mKeyguardViewMediator.notifyScreenOffAuthenticate(false, OpKeyguardViewMediator.AUTHENTICATE_FACEUNLOCK, 2);
        this.mUpdateMonitor.notifyFacelockStateChanged(0);
        stopFacelock();
    }

    public boolean isFacelockTimeout() {
        return this.mLockout || !this.mUpdateMonitor.isUnlockingWithBiometricAllowed();
    }

    private void bindFacelock() {
        String str = "OpFacelockController";
        if (!this.mBinding) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.oneplus.faceunlock", "com.oneplus.faceunlock.FaceUnlockService"));
            try {
                if (this.mContext.bindServiceAsUser(intent, this.mConnection, 1, UserHandle.OWNER)) {
                    Log.d(str, "Binding ok");
                    this.mBinding = true;
                } else {
                    Log.d(str, "Binding fail");
                }
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                sb.append("bindFacelock fail, ");
                sb.append(e.getMessage());
                Log.e(str, sb.toString());
            }
        }
    }

    private void bindFacelockSetting() {
        String str = "OpFacelockController";
        if (this.mBindingSetting) {
            Log.d(str, "return Binding");
            return;
        }
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.oneplus.faceunlock", "com.oneplus.faceunlock.FaceSettingService"));
        try {
            if (this.mContext.bindServiceAsUser(intent, this.mSettingConnection, 1, UserHandle.OWNER)) {
                Log.d(str, "Binding setting ok");
                this.mBindingSetting = true;
            } else {
                Log.d(str, "Binding setting fail");
            }
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("bind setting fail, ");
            sb.append(e.getMessage());
            Log.e(str, sb.toString());
        }
    }

    private void updateNotifyMessage(final int i, final int i2) {
        final int facelockNotifyMsgId = this.mUpdateMonitor.getFacelockNotifyMsgId(i);
        this.mUIHandler.post(new Runnable() {
            public void run() {
                OpLockIcon lockIcon = OpFacelockController.this.mIndicator != null ? OpFacelockController.this.mIndicator.getLockIcon() : null;
                if (lockIcon != null) {
                    if (OpFacelockController.this.mIsGoingToSleep) {
                        int i = i;
                        if (i == 0) {
                            lockIcon.setFacelockRunning(i, false);
                        }
                    }
                    lockIcon.setFacelockRunning(i, true);
                }
                if (OpFacelockController.this.mIndicator != null) {
                    int i2 = i;
                    if (i2 == 3) {
                        OpFacelockController.this.mIndicator.showTransientIndication(" ", ColorStateList.valueOf(i2));
                    } else if (i2 == 2) {
                        OpFacelockController.this.mIndicator.showTransientIndication((CharSequence) null);
                    } else {
                        if (facelockNotifyMsgId > 0) {
                            if (!OpUtils.isCustomFingerprint()) {
                                OpFacelockController.this.mIndicator.showTransientIndication(OpFacelockController.this.mContext.getString(facelockNotifyMsgId), ColorStateList.valueOf(i2));
                            } else if (OpFacelockController.this.mUpdateMonitor.isFacelockAvailable()) {
                                OpFacelockController.this.mIndicator.showTransientIndication((CharSequence) null);
                            } else {
                                OpFacelockController.this.mIndicator.showTransientIndication(OpFacelockController.this.mContext.getString(facelockNotifyMsgId), ColorStateList.valueOf(-1));
                            }
                        }
                    }
                }
            }
        });
    }

    private void registerLightSensor(boolean z) {
        if (this.mUpdateMonitor.isFacelockLightingEnabled()) {
            if (z) {
                this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, this.mSensorRate * 1000, this.mHandler);
            } else {
                this.mSensorManager.unregisterListener(this.mLightSensorListener);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateFacelockLightMode(boolean z) {
        if (z) {
            try {
                this.mPowerManager.overrideScreenBrightnessRangeMinimum(this.mLightingModeBrightness);
            } catch (RemoteException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("updateFacelockLightMode, overrideScreenBrightness:");
                sb.append(e.getMessage());
                Log.e("OpFacelockController", sb.toString());
            }
        } else {
            this.mPowerManager.overrideScreenBrightnessRangeMinimum(0);
        }
        this.mLightingModeEnabled = z;
        if (z && this.mIsKeyguardShowing) {
            registerLightSensor(false);
        }
    }

    public void stopFacelockLightMode() {
        registerLightSensor(false);
        updateFacelockLightMode(false);
    }

    public void setKeyguardIndicationController(KeyguardIndicationController keyguardIndicationController) {
        this.mIndicator = keyguardIndicationController;
    }

    /* access modifiers changed from: private */
    public void launchCamera(String str) {
        StatusBar statusBar = this.mPhoneStatusBar;
        if (statusBar != null) {
            statusBar.getKeyguardBottomAreaView().launchCamera(str);
        }
    }

    public boolean isScreenOffUnlock() {
        return this.mIsScreenOffUnlock;
    }

    public void onMotorStateChanged(int i) {
        if (i == 5) {
            this.mHandler.removeMessages(13);
            this.mHandler.sendEmptyMessage(13);
        } else if (i == -10) {
            this.mHandler.removeMessages(13);
            this.mHandler.sendEmptyMessage(13);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("  OpFacelockController: \n");
        StringBuilder sb = new StringBuilder();
        sb.append("  mFailAttempts: ");
        sb.append(this.mFailAttempts);
        printWriter.print(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("  mLockout: ");
        sb2.append(this.mLockout);
        printWriter.print(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append("  mBinding: ");
        sb3.append(this.mBinding);
        printWriter.print(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append("  mCameraLaunching: ");
        sb4.append(this.mCameraLaunching);
        printWriter.print(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append("  mBoundToService: ");
        sb5.append(this.mBoundToService);
        printWriter.print(sb5.toString());
        StringBuilder sb6 = new StringBuilder();
        sb6.append("  mFaceLockActive: ");
        sb6.append(this.mFaceLockActive);
        printWriter.print(sb6.toString());
        StringBuilder sb7 = new StringBuilder();
        sb7.append("  mService: ");
        sb7.append(this.mService);
        printWriter.print(sb7.toString());
        StringBuilder sb8 = new StringBuilder();
        sb8.append("  isFacelockEnabled: ");
        sb8.append(this.mUpdateMonitor.isFacelockEnabled());
        printWriter.print(sb8.toString());
        StringBuilder sb9 = new StringBuilder();
        sb9.append("  isAutoFacelockEnabled: ");
        sb9.append(this.mUpdateMonitor.isAutoFacelockEnabled());
        printWriter.print(sb9.toString());
        StringBuilder sb10 = new StringBuilder();
        sb10.append("  isFacelockLightingEnabled: ");
        sb10.append(this.mUpdateMonitor.isFacelockLightingEnabled());
        printWriter.print(sb10.toString());
        StringBuilder sb11 = new StringBuilder();
        sb11.append("  FacelockRunningType: ");
        sb11.append(this.mUpdateMonitor.getFacelockRunningType());
        printWriter.print(sb11.toString());
        StringBuilder sb12 = new StringBuilder();
        sb12.append("  isFacelockTimeout: ");
        sb12.append(isFacelockTimeout());
        printWriter.print(sb12.toString());
        StringBuilder sb13 = new StringBuilder();
        sb13.append("  isFacelockAllowed: ");
        sb13.append(this.mUpdateMonitor.isFacelockAllowed());
        printWriter.print(sb13.toString());
        StringBuilder sb14 = new StringBuilder();
        sb14.append("  mIsKeyguardShowing: ");
        sb14.append(this.mIsKeyguardShowing);
        printWriter.print(sb14.toString());
        StringBuilder sb15 = new StringBuilder();
        sb15.append("  mBouncer: ");
        sb15.append(this.mBouncer);
        printWriter.print(sb15.toString());
        StringBuilder sb16 = new StringBuilder();
        sb16.append("  mIsScreenTurnedOn: ");
        sb16.append(this.mIsScreenTurnedOn);
        printWriter.print(sb16.toString());
        StringBuilder sb17 = new StringBuilder();
        sb17.append("  mNeedToPendingStopFacelock: ");
        sb17.append(this.mNeedToPendingStopFacelock);
        printWriter.print(sb17.toString());
        StringBuilder sb18 = new StringBuilder();
        sb18.append("  mPendingStopFacelock: ");
        sb18.append(this.mPendingStopFacelock);
        printWriter.print(sb18.toString());
        StringBuilder sb19 = new StringBuilder();
        sb19.append("  mPendingLaunchCameraSource: ");
        sb19.append(this.mPendingLaunchCameraSource);
        printWriter.print(sb19.toString());
        StringBuilder sb20 = new StringBuilder();
        sb20.append("  mIsScreenOffUnlock: ");
        sb20.append(this.mIsScreenOffUnlock);
        printWriter.print(sb20.toString());
        StringBuilder sb21 = new StringBuilder();
        sb21.append("  mStartFacelockWhenScreenOn: ");
        sb21.append(this.mStartFacelockWhenScreenOn);
        printWriter.print(sb21.toString());
        StringBuilder sb22 = new StringBuilder();
        sb22.append("  mIsSleep: ");
        sb22.append(this.mIsSleep);
        printWriter.print(sb22.toString());
        StringBuilder sb23 = new StringBuilder();
        sb23.append("  mLightingModeEnabled: ");
        sb23.append(this.mLightingModeEnabled);
        printWriter.print(sb23.toString());
        StringBuilder sb24 = new StringBuilder();
        sb24.append("  mLightingModeSensorThreshold: ");
        sb24.append(this.mLightingModeSensorThreshold);
        printWriter.print(sb24.toString());
        StringBuilder sb25 = new StringBuilder();
        sb25.append("  mLightingModeBrightness: ");
        sb25.append(this.mLightingModeBrightness);
        printWriter.print(sb25.toString());
        StringBuilder sb26 = new StringBuilder();
        sb26.append("  FAILED_ATTEMPTS_TO_TIMEOUT: ");
        sb26.append(FAILED_ATTEMPTS_TO_TIMEOUT);
        printWriter.print(sb26.toString());
        StringBuilder sb27 = new StringBuilder();
        sb27.append("  mMotorQueue: ");
        sb27.append(Arrays.toString(this.mMotorQueue.toArray()));
        printWriter.print(sb27.toString());
    }

    /* access modifiers changed from: private */
    public void resetFPTimeout() {
        if (this.mFpm != null) {
            BiometricManager biometricManager = (BiometricManager) this.mContext.getSystemService(BiometricManager.class);
            if (biometricManager != null) {
                biometricManager.resetLockout(null);
            }
        }
    }

    public void onBiometricAuthFailed(BiometricSourceType biometricSourceType) {
        super.onBiometricAuthFailed(biometricSourceType);
        this.mFpFailTimeStamp = System.currentTimeMillis();
    }

    public long getFpFailTimeStamp() {
        return this.mFpFailTimeStamp;
    }
}

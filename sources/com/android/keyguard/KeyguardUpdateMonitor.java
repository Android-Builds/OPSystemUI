package com.android.keyguard;

import android.app.ActivityManager;
import android.app.ActivityManager.StackInfo;
import android.app.ActivityTaskManager;
import android.app.Instrumentation;
import android.app.UserSwitchObserver;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.app.trust.TrustManager.TrustListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.database.ContentObserver;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricSourceType;
import android.hardware.biometrics.IBiometricEnabledOnKeyguardCallback;
import android.hardware.biometrics.IBiometricEnabledOnKeyguardCallback.Stub;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceManager.AuthenticationCallback;
import android.hardware.face.FaceManager.AuthenticationResult;
import android.hardware.face.FaceManager.LockoutResetCallback;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.service.dreams.IDreamManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.settingslib.WirelessUtils;
import com.android.systemui.Dependency;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.TaskStackChangeListener;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.google.android.collect.Lists;
import com.oneplus.keyguard.OpKeyguardUpdateMonitor;
import com.oneplus.plugin.OpLsState;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.function.Consumer;

public class KeyguardUpdateMonitor extends OpKeyguardUpdateMonitor implements TrustListener {
    public static final boolean CORE_APPS_ONLY;
    /* access modifiers changed from: private */
    public static final boolean DEBUG = true;
    private static final ComponentName FALLBACK_HOME_COMPONENT = new ComponentName("com.android.settings", "com.android.settings.FallbackHome");
    private static int sCurrentUser;
    private static boolean sDisableHandlerCheckForTesting;
    private static KeyguardUpdateMonitor sInstance;
    private boolean mAssistantVisible;
    private boolean mAuthInterruptActive;
    private BatteryStatus mBatteryStatus;
    private IBiometricEnabledOnKeyguardCallback mBiometricEnabledCallback = new Stub() {
        public void onChanged(BiometricSourceType biometricSourceType, boolean z) throws RemoteException {
            if (biometricSourceType == BiometricSourceType.FACE) {
                KeyguardUpdateMonitor.this.mFaceSettingEnabledForUser = z;
                KeyguardUpdateMonitor.this.updateFaceListeningState();
            }
        }
    };
    private BiometricManager mBiometricManager;
    private boolean mBootCompleted;
    private boolean mBouncer;
    @VisibleForTesting
    protected final BroadcastReceiver mBroadcastAllReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.app.action.NEXT_ALARM_CLOCK_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(301);
            } else if ("android.intent.action.USER_INFO_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(317, intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId()), 0));
            } else if ("com.android.facelock.FACE_UNLOCK_STARTED".equals(action)) {
                Trace.beginSection("KeyguardUpdateMonitor.mBroadcastAllReceiver#onReceive ACTION_FACE_UNLOCK_STARTED");
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(327, 1, getSendingUserId()));
                Trace.endSection();
            } else if ("com.android.facelock.FACE_UNLOCK_STOPPED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(327, 0, getSendingUserId()));
            } else if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(309);
            } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(334);
            } else if ("android.intent.action.TIME_SET".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(301);
            }
        }
    };
    @VisibleForTesting
    protected final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String str = "KeyguardUpdateMonitor";
            if (KeyguardUpdateMonitor.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("received broadcast ");
                sb.append(action);
                Log.d(str, sb.toString());
            }
            if ("android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(339, intent.getStringExtra("time-zone")));
            } else {
                int i = -1;
                if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                    int intExtra = intent.getIntExtra("status", 1);
                    int intExtra2 = intent.getIntExtra("plugged", 0);
                    int intExtra3 = intent.getIntExtra("level", 0);
                    int intExtra4 = intent.getIntExtra("health", 1);
                    int intExtra5 = intent.getIntExtra("max_charging_current", -1);
                    int intExtra6 = intent.getIntExtra("max_charging_voltage", -1);
                    if (intExtra6 <= 0) {
                        intExtra6 = 5000000;
                    }
                    if (intExtra5 > 0) {
                        i = (intExtra5 / 1000) * (intExtra6 / 1000);
                    }
                    int i2 = i;
                    int intExtra7 = intent.getIntExtra("fastcharge_status", 0);
                    boolean z = intent.getBooleanExtra("pd_charge", false) && intExtra2 == 1;
                    int i3 = OpUtils.SUPPORT_WARP_CHARGING ? intExtra7 : intExtra7 > 0 ? 1 : 0;
                    if (OpUtils.DEBUG_ONEPLUS) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("pdCharge ");
                        sb2.append(z);
                        sb2.append(", chargingstatus ");
                        sb2.append(intExtra7);
                        sb2.append(", fastcharge:");
                        sb2.append(i3);
                        Log.d(str, sb2.toString());
                    }
                    OpHandler access$2800 = KeyguardUpdateMonitor.this.mHandler;
                    BatteryStatus batteryStatus = new BatteryStatus(intExtra, intExtra3, intExtra2, intExtra4, i2, i3, z);
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(access$2800.obtainMessage(302, batteryStatus));
                } else {
                    String str2 = "action ";
                    if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                        SimData fromIntent = SimData.fromIntent(intent);
                        if (intent.getBooleanExtra("rebroadcastOnUnlock", false)) {
                            if (fromIntent.simState == State.ABSENT) {
                                KeyguardUpdateMonitor.this.mHandler.obtainMessage(338, Boolean.valueOf(KeyguardUpdateMonitor.DEBUG)).sendToTarget();
                            }
                            return;
                        }
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append(str2);
                        sb3.append(action);
                        sb3.append(" state: ");
                        sb3.append(intent.getStringExtra("ss"));
                        sb3.append(" slotId: ");
                        sb3.append(fromIntent.slotId);
                        sb3.append(" subid: ");
                        sb3.append(fromIntent.subId);
                        Log.v(str, sb3.toString());
                        KeyguardUpdateMonitor.this.mHandler.obtainMessage(304, fromIntent.subId, fromIntent.slotId, fromIntent.simState).sendToTarget();
                    } else if ("android.media.RINGER_MODE_CHANGED".equals(action)) {
                        KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(305, intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1), 0));
                    } else if ("android.intent.action.PHONE_STATE".equals(action)) {
                        KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(306, intent.getStringExtra("state")));
                    } else if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                        KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(329);
                    } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                        KeyguardUpdateMonitor.this.dispatchBootCompleted();
                    } else if ("android.intent.action.SERVICE_STATE".equals(action)) {
                        ServiceState newFromBundle = ServiceState.newFromBundle(intent.getExtras());
                        int intExtra8 = intent.getIntExtra("subscription", -1);
                        StringBuilder sb4 = new StringBuilder();
                        sb4.append(str2);
                        sb4.append(action);
                        sb4.append(" serviceState=");
                        sb4.append(newFromBundle);
                        sb4.append(" subId=");
                        sb4.append(intExtra8);
                        Log.v(str, sb4.toString());
                        KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(330, intExtra8, 0, newFromBundle));
                    } else if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                        KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(337);
                    }
                }
            }
        }
    };
    private final ArrayList<WeakReference<KeyguardUpdateMonitorCallback>> mCallbacks = Lists.newArrayList();
    private final Context mContext;
    private boolean mDeviceInteractive;
    private final DevicePolicyManager mDevicePolicyManager;
    /* access modifiers changed from: private */
    public boolean mDeviceProvisioned;
    private ContentObserver mDeviceProvisionedObserver;
    private DisplayClientState mDisplayClientState = new DisplayClientState();
    private final IDreamManager mDreamManager;
    @VisibleForTesting
    AuthenticationCallback mFaceAuthenticationCallback = new AuthenticationCallback() {
        public void onAuthenticationFailed() {
            KeyguardUpdateMonitor.this.handleFaceAuthFailed();
        }

        public void onAuthenticationSucceeded(AuthenticationResult authenticationResult) {
            Trace.beginSection("KeyguardUpdateMonitor#onAuthenticationSucceeded");
            KeyguardUpdateMonitor.this.handleFaceAuthenticated(authenticationResult.getUserId());
            Trace.endSection();
        }

        public void onAuthenticationHelp(int i, CharSequence charSequence) {
            KeyguardUpdateMonitor.this.handleFaceHelp(i, charSequence.toString());
        }

        public void onAuthenticationError(int i, CharSequence charSequence) {
            KeyguardUpdateMonitor.this.handleFaceError(i, charSequence != null ? charSequence.toString() : "");
        }

        public void onAuthenticationAcquired(int i) {
            KeyguardUpdateMonitor.this.handleFaceAcquired(i);
        }
    };
    private CancellationSignal mFaceCancelSignal;
    private final LockoutResetCallback mFaceLockoutResetCallback = new LockoutResetCallback() {
        public void onLockoutReset() {
            KeyguardUpdateMonitor.this.handleFaceLockoutReset();
        }
    };
    private FaceManager mFaceManager;
    private int mFaceRunningState = 0;
    /* access modifiers changed from: private */
    public boolean mFaceSettingEnabledForUser;
    private FingerprintManager.AuthenticationCallback mFingerprintAuthenticationCallback = new FingerprintManager.AuthenticationCallback() {
        public void onAuthenticationFailed() {
            KeyguardUpdateMonitor.this.mHandler.removeMessages(501);
            if (KeyguardUpdateMonitor.this.mDuringAcquired) {
                KeyguardUpdateMonitor.this.mDuringAcquired = false;
                KeyguardUpdateMonitor.this.updateFingerprintListeningState();
            }
            KeyguardUpdateMonitor.this.handleFingerprintAuthFailed();
        }

        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult authenticationResult) {
            Trace.beginSection("KeyguardUpdateMonitor#onAuthenticationSucceeded");
            Log.d("KeyguardUpdateMonitor", "received onAuthenticationSucceeded");
            KeyguardUpdateMonitor.this.mHandler.removeMessages(501);
            if (KeyguardUpdateMonitor.this.mDuringAcquired) {
                KeyguardUpdateMonitor.this.mDuringAcquired = false;
            }
            KeyguardUpdateMonitor.this.handleFingerprintAuthenticated(authenticationResult.getUserId());
            Trace.endSection();
        }

        public void onAuthenticationHelp(int i, CharSequence charSequence) {
            KeyguardUpdateMonitor.this.mHandler.removeMessages(501);
            if (KeyguardUpdateMonitor.this.mDuringAcquired) {
                KeyguardUpdateMonitor.this.mDuringAcquired = false;
                KeyguardUpdateMonitor.this.updateFingerprintListeningState();
            }
            KeyguardUpdateMonitor.this.handleFingerprintHelp(i, charSequence.toString());
        }

        public void onAuthenticationError(int i, CharSequence charSequence) {
            if (i == 101) {
                if (KeyguardUpdateMonitor.this.mFingerprintRunningState == 1 && !KeyguardUpdateMonitor.this.mLockoutState) {
                    Log.d("KeyguardUpdateMonitor", "state stopped when interrupted");
                    KeyguardUpdateMonitor.this.setFingerprintRunningState(0);
                }
                return;
            }
            KeyguardUpdateMonitor.this.mHandler.removeMessages(501);
            if (KeyguardUpdateMonitor.this.mDuringAcquired) {
                KeyguardUpdateMonitor.this.mDuringAcquired = false;
                KeyguardUpdateMonitor.this.updateFingerprintListeningState();
            }
            if (i == 9 && KeyguardUpdateMonitor.this.getFingerprintFailedUnlockAttempts() == 4) {
                KeyguardUpdateMonitor.this.handleFingerprintAuthFailed();
            }
            KeyguardUpdateMonitor.this.handleFingerprintError(i, charSequence == null ? "" : charSequence.toString());
        }

        public void onAuthenticationAcquired(int i) {
            KeyguardUpdateMonitor.this.handleFingerprintAcquired(i);
        }
    };
    private CancellationSignal mFingerprintCancelSignal;
    private final FingerprintManager.LockoutResetCallback mFingerprintLockoutResetCallback = new FingerprintManager.LockoutResetCallback() {
        public void onLockoutReset() {
            KeyguardUpdateMonitor.this.handleFingerprintLockoutReset();
        }
    };
    /* access modifiers changed from: private */
    public int mFingerprintRunningState = 0;
    private FingerprintManager mFpm;
    private boolean mGoingToSleep;
    /* access modifiers changed from: private */
    public OpHandler mHandler = new OpHandler(Looper.getMainLooper()) {
        public void handleMessage(Message message) {
            int i = message.what;
            boolean z = KeyguardUpdateMonitor.DEBUG;
            switch (i) {
                case 301:
                    KeyguardUpdateMonitor.this.handleTimeUpdate();
                    break;
                case 302:
                    KeyguardUpdateMonitor.this.handleBatteryUpdate((BatteryStatus) message.obj);
                    break;
                case 304:
                    KeyguardUpdateMonitor.this.opHandlePendingSubInfoChange(message.arg2);
                    KeyguardUpdateMonitor.this.handleSimStateChange(message.arg1, message.arg2, (State) message.obj);
                    break;
                case 305:
                    KeyguardUpdateMonitor.this.handleRingerModeChange(message.arg1);
                    break;
                case 306:
                    KeyguardUpdateMonitor.this.handlePhoneStateChanged((String) message.obj);
                    break;
                case 308:
                    KeyguardUpdateMonitor.this.handleDeviceProvisioned();
                    break;
                case 309:
                    KeyguardUpdateMonitor.this.handleDevicePolicyManagerStateChanged();
                    break;
                case 310:
                    KeyguardUpdateMonitor.this.handleUserSwitching(message.arg1, (IRemoteCallback) message.obj);
                    break;
                case 312:
                    KeyguardUpdateMonitor.this.handleKeyguardReset();
                    break;
                case 313:
                    KeyguardUpdateMonitor.this.handleBootCompleted();
                    break;
                case 314:
                    KeyguardUpdateMonitor.this.handleUserSwitchComplete(message.arg1);
                    break;
                case 317:
                    KeyguardUpdateMonitor.this.handleUserInfoChanged(message.arg1);
                    break;
                case 318:
                    KeyguardUpdateMonitor.this.handleReportEmergencyCallAction();
                    break;
                case 319:
                    Trace.beginSection("KeyguardUpdateMonitor#handler MSG_STARTED_WAKING_UP");
                    KeyguardUpdateMonitor.this.handleStartedWakingUp();
                    Trace.endSection();
                    break;
                case 320:
                    KeyguardUpdateMonitor.this.handleFinishedGoingToSleep(message.arg1);
                    break;
                case 321:
                    KeyguardUpdateMonitor.this.handleStartedGoingToSleep(message.arg1);
                    break;
                case 322:
                    KeyguardUpdateMonitor.this.handleKeyguardBouncerChanged(message.arg1);
                    break;
                case 327:
                    Trace.beginSection("KeyguardUpdateMonitor#handler MSG_FACE_UNLOCK_STATE_CHANGED");
                    KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.this;
                    if (message.arg1 == 0) {
                        z = false;
                    }
                    keyguardUpdateMonitor.handleFaceUnlockStateChanged(z, message.arg2);
                    Trace.endSection();
                    break;
                case 328:
                    if (!KeyguardUpdateMonitor.this.mSimUnlockSlot0 && !KeyguardUpdateMonitor.this.mSimUnlockSlot1) {
                        KeyguardUpdateMonitor.this.handleSimSubscriptionInfoChanged();
                        break;
                    } else {
                        KeyguardUpdateMonitor.this.mPendingSubInfoChange = KeyguardUpdateMonitor.DEBUG;
                        Log.d("KeyguardUpdateMonitor", "delay handle subinfo change");
                        return;
                    }
                    break;
                case 329:
                    KeyguardUpdateMonitor.this.handleAirplaneModeChanged();
                    break;
                case 330:
                    KeyguardUpdateMonitor.this.handleServiceStateChange(message.arg1, (ServiceState) message.obj);
                    break;
                case 331:
                    KeyguardUpdateMonitor.this.handleScreenTurnedOn();
                    break;
                case 332:
                    Trace.beginSection("KeyguardUpdateMonitor#handler MSG_SCREEN_TURNED_ON");
                    KeyguardUpdateMonitor.this.handleScreenTurnedOff();
                    Trace.endSection();
                    break;
                case 333:
                    KeyguardUpdateMonitor.this.handleDreamingStateChanged(message.arg1);
                    break;
                case 334:
                    KeyguardUpdateMonitor.this.handleUserUnlocked();
                    break;
                case 335:
                    KeyguardUpdateMonitor.this.setAssistantVisible(((Boolean) message.obj).booleanValue());
                    break;
                case 336:
                    KeyguardUpdateMonitor.this.updateBiometricListeningState();
                    break;
                case 337:
                    KeyguardUpdateMonitor.this.updateLogoutEnabled();
                    break;
                case 338:
                    KeyguardUpdateMonitor.this.updateTelephonyCapable(((Boolean) message.obj).booleanValue());
                    break;
                case 339:
                    KeyguardUpdateMonitor.this.handleTimeZoneUpdate((String) message.obj);
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }
    };
    /* access modifiers changed from: private */
    public int mHardwareFaceUnavailableRetryCount = 0;
    /* access modifiers changed from: private */
    public int mHardwareFingerprintUnavailableRetryCount = 0;
    private boolean mHasLockscreenWallpaper;
    private boolean mIsDreaming;
    private final boolean mIsPrimaryUser;
    private boolean mKeyguardGoingAway;
    private boolean mKeyguardIsVisible;
    private boolean mKeyguardOccluded;
    private boolean mLockIconPressed;
    private LockPatternUtils mLockPatternUtils;
    private boolean mLogoutEnabled;
    private boolean mNeedsSlowUnlockTransition;
    private int mPhoneState;
    private Runnable mRetryFaceAuthentication = new Runnable() {
        public void run() {
            StringBuilder sb = new StringBuilder();
            sb.append("Retrying face after HW unavailable, attempt ");
            sb.append(KeyguardUpdateMonitor.this.mHardwareFaceUnavailableRetryCount);
            Log.w("KeyguardUpdateMonitor", sb.toString());
            KeyguardUpdateMonitor.this.updateFaceListeningState();
        }
    };
    private Runnable mRetryFingerprintAuthentication = new Runnable() {
        public void run() {
            StringBuilder sb = new StringBuilder();
            sb.append("Retrying fingerprint after HW unavailable, attempt ");
            sb.append(KeyguardUpdateMonitor.this.mHardwareFingerprintUnavailableRetryCount);
            Log.w("KeyguardUpdateMonitor", sb.toString());
            KeyguardUpdateMonitor.this.updateFingerprintListeningState();
        }
    };
    private int mRingMode;
    private boolean mScreenOn;
    HashMap<Integer, ServiceState> mServiceStates = new HashMap<>();
    HashMap<Integer, SimData> mSimDatas = new HashMap<>();
    @VisibleForTesting
    protected StrongAuthTracker mStrongAuthTracker;
    private List<SubscriptionInfo> mSubscriptionInfo;
    private OnSubscriptionsChangedListener mSubscriptionListener = new OnSubscriptionsChangedListener() {
        public void onSubscriptionsChanged() {
            KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(328);
        }
    };
    private SubscriptionManager mSubscriptionManager;
    private boolean mSwitchingUser;
    private final TaskStackChangeListener mTaskStackListener = new TaskStackChangeListener() {
        public void onTaskStackChangedBackground() {
            try {
                StackInfo stackInfo = ActivityTaskManager.getService().getStackInfo(0, 4);
                if (stackInfo != null) {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(335, Boolean.valueOf(stackInfo.visible)));
                }
            } catch (RemoteException e) {
                Log.e("KeyguardUpdateMonitor", "unable to check task stack", e);
            }
        }
    };
    @VisibleForTesting
    protected boolean mTelephonyCapable;
    private TrustManager mTrustManager;
    private Runnable mUpdateBiometricListeningState = new Runnable() {
        public final void run() {
            KeyguardUpdateMonitor.this.updateBiometricListeningState();
        }
    };
    private SparseBooleanArray mUserFaceAuthenticated = new SparseBooleanArray();
    private SparseBooleanArray mUserFaceUnlockRunning = new SparseBooleanArray();
    private SparseBooleanArray mUserFingerprintAuthenticated = new SparseBooleanArray();
    private SparseBooleanArray mUserHasTrust = new SparseBooleanArray();
    private UserManager mUserManager;
    private SparseBooleanArray mUserTrustIsManaged = new SparseBooleanArray();

    /* renamed from: com.android.keyguard.KeyguardUpdateMonitor$15 */
    static /* synthetic */ class C061515 {

        /* renamed from: $SwitchMap$com$android$keyguard$KeyguardSecurityModel$SecurityMode */
        static final /* synthetic */ int[] f52xdc0e830a = new int[SecurityMode.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(8:0|1|2|3|4|5|6|8) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        static {
            /*
                com.android.keyguard.KeyguardSecurityModel$SecurityMode[] r0 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                f52xdc0e830a = r0
                int[] r0 = f52xdc0e830a     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.Pattern     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = f52xdc0e830a     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.PIN     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = f52xdc0e830a     // Catch:{ NoSuchFieldError -> 0x002a }
                com.android.keyguard.KeyguardSecurityModel$SecurityMode r1 = com.android.keyguard.KeyguardSecurityModel.SecurityMode.Password     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardUpdateMonitor.C061515.<clinit>():void");
        }
    }

    public static class BatteryStatus {
        public final int fastCharge;
        public final int health;
        public final int level;
        public final int maxChargingWattage;
        public final boolean pdcharge;
        public final int plugged;
        public final int status;

        public BatteryStatus(int i, int i2, int i3, int i4, int i5, int i6, boolean z) {
            this.status = i;
            this.level = i2;
            this.plugged = i3;
            this.health = i4;
            this.maxChargingWattage = i5;
            this.fastCharge = i6;
            this.pdcharge = z;
        }

        public boolean isPluggedIn() {
            int i = this.plugged;
            if (i == 1 || i == 2 || i == 4) {
                return KeyguardUpdateMonitor.DEBUG;
            }
            return false;
        }

        public boolean isPluggedInWired() {
            int i = this.plugged;
            if (i == 1 || i == 2) {
                return KeyguardUpdateMonitor.DEBUG;
            }
            return false;
        }

        public boolean isPdCharging() {
            return this.pdcharge;
        }

        public boolean isCharged() {
            if (this.status == 5 || this.level >= 100) {
                return KeyguardUpdateMonitor.DEBUG;
            }
            return false;
        }

        public final int getChargingSpeed(int i, int i2) {
            if (((BatteryController) Dependency.get(BatteryController.class)).isFastCharging(this.fastCharge)) {
                return 2;
            }
            if (((BatteryController) Dependency.get(BatteryController.class)).isWarpCharging(this.fastCharge)) {
                return 3;
            }
            return this.maxChargingWattage <= 0 ? -1 : 1;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("BatteryStatus{status=");
            sb.append(this.status);
            sb.append(",level=");
            sb.append(this.level);
            sb.append(",plugged=");
            sb.append(this.plugged);
            sb.append(",health=");
            sb.append(this.health);
            sb.append(",maxChargingWattage=");
            sb.append(this.maxChargingWattage);
            sb.append("}, fastCharge:");
            sb.append(this.fastCharge);
            sb.append(", pdcharge:");
            sb.append(this.pdcharge);
            return sb.toString();
        }
    }

    static class DisplayClientState {
        DisplayClientState() {
        }
    }

    private static class SimData {
        public State simState;
        public int slotId;
        public int subId;

        SimData(State state, int i, int i2) {
            this.simState = state;
            this.slotId = i;
            this.subId = i2;
        }

        static SimData fromIntent(Intent intent) {
            State state;
            if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                String stringExtra = intent.getStringExtra("ss");
                int intExtra = intent.getIntExtra("phone", 0);
                int intExtra2 = intent.getIntExtra("subscription", -1);
                String str = "reason";
                if ("ABSENT".equals(stringExtra)) {
                    if ("PERM_DISABLED".equals(intent.getStringExtra(str))) {
                        state = State.PERM_DISABLED;
                    } else {
                        state = State.ABSENT;
                    }
                } else if ("READY".equals(stringExtra)) {
                    state = State.READY;
                } else if ("LOCKED".equals(stringExtra)) {
                    String stringExtra2 = intent.getStringExtra(str);
                    if ("PIN".equals(stringExtra2)) {
                        state = State.PIN_REQUIRED;
                    } else if ("PUK".equals(stringExtra2)) {
                        state = State.PUK_REQUIRED;
                    } else {
                        state = State.UNKNOWN;
                    }
                } else if ("NETWORK".equals(stringExtra)) {
                    state = State.NETWORK_LOCKED;
                } else if ("CARD_IO_ERROR".equals(stringExtra)) {
                    state = State.CARD_IO_ERROR;
                } else if ("LOADED".equals(stringExtra) || "IMSI".equals(stringExtra)) {
                    state = State.READY;
                } else {
                    state = State.UNKNOWN;
                }
                return new SimData(state, intExtra, intExtra2);
            }
            throw new IllegalArgumentException("only handles intent ACTION_SIM_STATE_CHANGED");
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("SimData{state=");
            sb.append(this.simState);
            sb.append(",slotId=");
            sb.append(this.slotId);
            sb.append(",subId=");
            sb.append(this.subId);
            sb.append("}");
            return sb.toString();
        }
    }

    public static class StrongAuthTracker extends com.android.internal.widget.LockPatternUtils.StrongAuthTracker {
        private boolean mIsFaceTimeout = false;
        private final Consumer<Integer> mStrongAuthRequiredChangedCallback;

        public StrongAuthTracker(Context context, Consumer<Integer> consumer) {
            super(context);
            this.mStrongAuthRequiredChangedCallback = consumer;
        }

        public boolean isUnlockingWithBiometricAllowed() {
            return isBiometricAllowedForUser(KeyguardUpdateMonitor.getCurrentUser());
        }

        public boolean hasUserAuthenticatedSinceBoot() {
            if ((getStrongAuthForUser(KeyguardUpdateMonitor.getCurrentUser()) & 1) == 0) {
                return KeyguardUpdateMonitor.DEBUG;
            }
            return false;
        }

        public void onStrongAuthRequiredChanged(int i) {
            this.mStrongAuthRequiredChangedCallback.accept(Integer.valueOf(i));
        }

        public void onWeakFaceTimeoutChanged(boolean z, int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("[WeakFace] onWeakFaceTimeoutChanged ");
            sb.append(z);
            sb.append(", ");
            sb.append(i);
            Log.d("KeyguardUpdateMonitor", sb.toString());
            this.mIsFaceTimeout = z;
        }

        public boolean isWeakFaceTimeout() {
            return this.mIsFaceTimeout;
        }
    }

    static {
        try {
            CORE_APPS_ONLY = IPackageManager.Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static synchronized void setCurrentUser(int i) {
        synchronized (KeyguardUpdateMonitor.class) {
            sCurrentUser = i;
        }
    }

    public static synchronized int getCurrentUser() {
        int i;
        synchronized (KeyguardUpdateMonitor.class) {
            i = sCurrentUser;
        }
        return i;
    }

    public void onTrustChanged(boolean z, int i, int i2) {
        checkIsHandlerThread();
        this.mUserHasTrust.put(i, z);
        for (int i3 = 0; i3 < this.mCallbacks.size(); i3++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i3)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onTrustChanged(i);
                if (z && i2 != 0) {
                    keyguardUpdateMonitorCallback.onTrustGrantedWithFlags(i2, i);
                }
            }
        }
    }

    public void onTrustError(CharSequence charSequence) {
        dispatchErrorMessage(charSequence);
    }

    /* access modifiers changed from: private */
    public void handleSimSubscriptionInfoChanged() {
        String str = "KeyguardUpdateMonitor";
        Log.v(str, "onSubscriptionInfoChanged()");
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList != null) {
            for (SubscriptionInfo subscriptionInfo : activeSubscriptionInfoList) {
                StringBuilder sb = new StringBuilder();
                sb.append("SubInfo:");
                sb.append(subscriptionInfo);
                Log.v(str, sb.toString());
            }
        } else {
            Log.v(str, "onSubscriptionInfoChanged: list is null");
        }
        List subscriptionInfo2 = getSubscriptionInfo(DEBUG);
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < subscriptionInfo2.size(); i++) {
            SubscriptionInfo subscriptionInfo3 = (SubscriptionInfo) subscriptionInfo2.get(i);
            if (refreshSimState(subscriptionInfo3.getSubscriptionId(), subscriptionInfo3.getSimSlotIndex())) {
                arrayList.add(subscriptionInfo3);
            }
        }
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            SimData simData = (SimData) this.mSimDatas.get(Integer.valueOf(((SubscriptionInfo) arrayList.get(i2)).getSubscriptionId()));
            for (int i3 = 0; i3 < this.mCallbacks.size(); i3++) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i3)).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onSimStateChanged(simData.subId, simData.slotId, simData.simState);
                }
            }
        }
        for (int i4 = 0; i4 < this.mCallbacks.size(); i4++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback2 = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i4)).get();
            if (keyguardUpdateMonitorCallback2 != null) {
                keyguardUpdateMonitorCallback2.onRefreshCarrierInfo();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleAirplaneModeChanged() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onRefreshCarrierInfo();
            }
        }
    }

    public List<SubscriptionInfo> getSubscriptionInfo(boolean z) {
        List<SubscriptionInfo> list = this.mSubscriptionInfo;
        if (list == null || z) {
            list = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        }
        if (list == null) {
            this.mSubscriptionInfo = new ArrayList();
        } else {
            this.mSubscriptionInfo = list;
        }
        return this.mSubscriptionInfo;
    }

    public void onTrustManagedChanged(boolean z, int i) {
        checkIsHandlerThread();
        this.mUserTrustIsManaged.put(i, z);
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onTrustManagedChanged(i);
            }
        }
    }

    public void setKeyguardGoingAway(boolean z) {
        this.mKeyguardGoingAway = z;
        updateFingerprintListeningState();
    }

    public void setKeyguardOccluded(boolean z) {
        this.mKeyguardOccluded = z;
        updateBiometricListeningState();
    }

    public boolean isDreaming() {
        return this.mIsDreaming;
    }

    public void awakenFromDream() {
        if (this.mIsDreaming) {
            IDreamManager iDreamManager = this.mDreamManager;
            if (iDreamManager != null) {
                try {
                    iDreamManager.awaken();
                } catch (RemoteException unused) {
                    Log.e("KeyguardUpdateMonitor", "Unable to awaken from dream");
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void onFingerprintAuthenticated(int i) {
        Trace.beginSection("KeyGuardUpdateMonitor#onFingerPrintAuthenticated");
        this.mUserFingerprintAuthenticated.put(i, DEBUG);
        if (getUserCanSkipBouncer(i)) {
            this.mTrustManager.unlockedByBiometricForUser(i, BiometricSourceType.FINGERPRINT);
        }
        this.mFingerprintCancelSignal = null;
        opOnFingerprintAuthenticated(i);
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onBiometricAuthenticated(i, BiometricSourceType.FINGERPRINT);
            }
        }
        OpHandler opHandler = this.mHandler;
        opHandler.sendMessageDelayed(opHandler.obtainMessage(336), 500);
        this.mAssistantVisible = false;
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleFingerprintAuthFailed() {
        if (getFingerprintFailedUnlockAttempts() < 5) {
            this.mFingerprintFailedAttempts.put(sCurrentUser, getFingerprintFailedUnlockAttempts() + 1);
            if (OpKeyguardUpdateMonitor.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("fp Auth Failed, failed attempts = ");
                sb.append(getFingerprintFailedUnlockAttempts());
                Log.d("KeyguardUpdateMonitor", sb.toString());
            }
        }
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onBiometricAuthFailed(BiometricSourceType.FINGERPRINT);
            }
        }
        handleFingerprintHelp(-1, this.mContext.getString(R$string.kg_fingerprint_not_recognized));
    }

    /* access modifiers changed from: private */
    public void handleFingerprintAcquired(int i) {
        opHandleFingerprintAcquired(i);
    }

    /* access modifiers changed from: private */
    public void handleFingerprintAuthenticated(int i) {
        String str = "KeyguardUpdateMonitor";
        Trace.beginSection("KeyGuardUpdateMonitor#handlerFingerPrintAuthenticated");
        try {
            int i2 = ActivityManager.getService().getCurrentUser().id;
            if (i2 != i) {
                StringBuilder sb = new StringBuilder();
                sb.append("Fingerprint authenticated for wrong user: ");
                sb.append(i);
                Log.d(str, sb.toString());
                handleFingerprintAuthFailed();
            } else if (isFingerprintDisabled(i2)) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Fingerprint disabled by DPM for userId: ");
                sb2.append(i2);
                Log.d(str, sb2.toString());
                handleFingerprintAuthFailed();
            } else {
                onFingerprintAuthenticated(i2);
                setFingerprintRunningState(0);
                Trace.endSection();
            }
        } catch (RemoteException e) {
            Log.e(str, "Failed to get current user id: ", e);
        } finally {
            setFingerprintRunningState(0);
        }
    }

    /* access modifiers changed from: private */
    public void handleFingerprintHelp(int i, String str) {
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onBiometricHelp(i, str, BiometricSourceType.FINGERPRINT);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFingerprintError(int i, String str) {
        opHandleFingerprintError1(i);
        if (i == 5 && this.mFingerprintRunningState == 3) {
            setFingerprintRunningState(0);
            updateFingerprintListeningState();
        } else {
            setFingerprintRunningState(0);
        }
        if (i == 1) {
            int i2 = this.mHardwareFingerprintUnavailableRetryCount;
            if (i2 < 3) {
                this.mHardwareFingerprintUnavailableRetryCount = i2 + 1;
                this.mHandler.removeCallbacks(this.mRetryFingerprintAuthentication);
                this.mHandler.postDelayed(this.mRetryFingerprintAuthentication, 3000);
            }
        }
        if (i == 9) {
            this.mLockPatternUtils.requireStrongAuth(8, getCurrentUser());
        }
        for (int i3 = 0; i3 < this.mCallbacks.size(); i3++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i3)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onBiometricError(i, str, BiometricSourceType.FINGERPRINT);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFingerprintLockoutReset() {
        OpHandler opHandler = this.mHandler;
        opHandler.sendMessageDelayed(opHandler.obtainMessage(336), !this.mLockoutState ? 100 : 500);
        opHandleFingerprintLockoutReset();
    }

    /* access modifiers changed from: private */
    public void setFingerprintRunningState(int i) {
        boolean z = false;
        boolean z2 = this.mFingerprintRunningState == 1;
        if (i == 1) {
            z = true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("fingerprintRunningState: ");
        sb.append(z2);
        sb.append(" to ");
        sb.append(z);
        sb.append(", ");
        sb.append(i);
        Log.d("KeyguardUpdateMonitor", sb.toString());
        this.mFingerprintRunningState = i;
        if (z2 != z) {
            notifyFingerprintRunningStateChanged();
        }
    }

    private void notifyFingerprintRunningStateChanged() {
        checkIsHandlerThread();
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onBiometricRunningStateChanged(isFingerprintDetectionRunning(), BiometricSourceType.FINGERPRINT);
            }
        }
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void onFaceAuthenticated(int i) {
        Trace.beginSection("KeyGuardUpdateMonitor#onFaceAuthenticated");
        this.mUserFaceAuthenticated.put(i, DEBUG);
        if (getUserCanSkipBouncer(i)) {
            this.mTrustManager.unlockedByBiometricForUser(i, BiometricSourceType.FACE);
        }
        this.mFaceCancelSignal = null;
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onBiometricAuthenticated(i, BiometricSourceType.FACE);
            }
        }
        OpHandler opHandler = this.mHandler;
        opHandler.sendMessageDelayed(opHandler.obtainMessage(336), 500);
        this.mAssistantVisible = false;
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleFaceAuthFailed() {
        setFaceRunningState(0);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onBiometricAuthFailed(BiometricSourceType.FACE);
            }
        }
        handleFaceHelp(-1, this.mContext.getString(R$string.kg_face_not_recognized));
    }

    /* access modifiers changed from: private */
    public void handleFaceAcquired(int i) {
        if (i == 0) {
            for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onBiometricAcquired(BiometricSourceType.FACE);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFaceAuthenticated(int i) {
        String str = "KeyguardUpdateMonitor";
        Trace.beginSection("KeyGuardUpdateMonitor#handlerFaceAuthenticated");
        try {
            int i2 = ActivityManager.getService().getCurrentUser().id;
            if (i2 != i) {
                StringBuilder sb = new StringBuilder();
                sb.append("Face authenticated for wrong user: ");
                sb.append(i);
                Log.d(str, sb.toString());
            } else if (isFaceDisabled(i2)) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Face authentication disabled by DPM for userId: ");
                sb2.append(i2);
                Log.d(str, sb2.toString());
            } else {
                onFaceAuthenticated(i2);
                setFaceRunningState(0);
                Trace.endSection();
            }
        } catch (RemoteException e) {
            Log.e(str, "Failed to get current user id: ", e);
        } finally {
            setFaceRunningState(0);
        }
    }

    /* access modifiers changed from: private */
    public void handleFaceHelp(int i, String str) {
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onBiometricHelp(i, str, BiometricSourceType.FACE);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFaceError(int i, String str) {
        if (i == 5 && this.mFaceRunningState == 3) {
            setFaceRunningState(0);
            updateFaceListeningState();
        } else {
            setFaceRunningState(0);
        }
        if (i == 1) {
            int i2 = this.mHardwareFaceUnavailableRetryCount;
            if (i2 < 3) {
                this.mHardwareFaceUnavailableRetryCount = i2 + 1;
                this.mHandler.removeCallbacks(this.mRetryFaceAuthentication);
                this.mHandler.postDelayed(this.mRetryFaceAuthentication, 3000);
            }
        }
        if (i == 9) {
            this.mLockPatternUtils.requireStrongAuth(8, getCurrentUser());
        }
        for (int i3 = 0; i3 < this.mCallbacks.size(); i3++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i3)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onBiometricError(i, str, BiometricSourceType.FACE);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFaceLockoutReset() {
        updateFaceListeningState();
    }

    private void setFaceRunningState(int i) {
        boolean z = false;
        boolean z2 = this.mFaceRunningState == 1;
        if (i == 1) {
            z = true;
        }
        this.mFaceRunningState = i;
        StringBuilder sb = new StringBuilder();
        sb.append("faceRunningState: ");
        sb.append(this.mFaceRunningState);
        Log.d("KeyguardUpdateMonitor", sb.toString());
        if (z2 != z) {
            notifyFaceRunningStateChanged();
        }
    }

    private void notifyFaceRunningStateChanged() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onBiometricRunningStateChanged(isFaceDetectionRunning(), BiometricSourceType.FACE);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleFaceUnlockStateChanged(boolean z, int i) {
        checkIsHandlerThread();
        this.mUserFaceUnlockRunning.put(i, z);
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onFaceUnlockStateChanged(z, i);
            }
        }
    }

    public boolean isFingerprintDetectionRunning() {
        if (this.mFingerprintRunningState == 1) {
            return DEBUG;
        }
        return false;
    }

    public boolean isFaceDetectionRunning() {
        if (this.mFaceRunningState == 1) {
            return DEBUG;
        }
        return false;
    }

    private boolean isTrustDisabled(int i) {
        return isSimPinSecure();
    }

    private boolean isFingerprintDisabled(int i) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if ((devicePolicyManager == null || (devicePolicyManager.getKeyguardDisabledFeatures(null, i) & 32) == 0) && !isSimPinSecure()) {
            return false;
        }
        return DEBUG;
    }

    private boolean isFaceDisabled(int i) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if ((devicePolicyManager == null || (devicePolicyManager.getKeyguardDisabledFeatures(null, i) & 128) == 0) && !isSimPinSecure()) {
            return false;
        }
        return DEBUG;
    }

    public boolean getUserCanSkipBouncer(int i) {
        if (getUserHasTrust(i) || getUserUnlockedWithBiometric(i)) {
            return DEBUG;
        }
        return false;
    }

    public boolean getUserHasTrust(int i) {
        if ((isTrustDisabled(i) || !this.mUserHasTrust.get(i)) && !canSkipBouncerByFacelock()) {
            return false;
        }
        return DEBUG;
    }

    public boolean getUserUnlockedWithBiometric(int i) {
        if ((!(this.mUserFingerprintAuthenticated.get(i) || this.mUserFaceAuthenticated.get(i)) || !isUnlockingWithBiometricAllowed()) && !canSkipBouncerByFacelock() && !isFacelockUnlocking()) {
            return false;
        }
        return DEBUG;
    }

    public boolean getUserTrustIsManaged(int i) {
        if ((!this.mUserTrustIsManaged.get(i) || isTrustDisabled(i)) && !canSkipBouncerByFacelock()) {
            return false;
        }
        return DEBUG;
    }

    public boolean isUnlockingWithBiometricAllowed() {
        return this.mStrongAuthTracker.isUnlockingWithBiometricAllowed();
    }

    public boolean isWeakFaceTimeout() {
        boolean z = false;
        if (!OpUtils.isWeakFaceUnlockEnabled() || !isUnlockWithFacelockPossible()) {
            return false;
        }
        if (this.mStrongAuthTracker.isWeakFaceTimeout() || isFacelockDisabled()) {
            z = DEBUG;
        }
        return z;
    }

    public int getBiometricTimeoutStringWhenLock() {
        int i = 0;
        if (!OpUtils.isWeakFaceUnlockEnabled()) {
            return 0;
        }
        if (isWeakFaceTimeout()) {
            boolean isUnlockingWithBiometricAllowed = isUnlockingWithBiometricAllowed();
            boolean isUnlockWithFingerprintPossible = isUnlockWithFingerprintPossible(getCurrentUser());
            if (isUnlockingWithBiometricAllowed && isUnlockWithFingerprintPossible) {
                i = R$string.op_kg_prompt_reason_face_timeout_swipe;
            } else if (!isUnlockWithFingerprintPossible) {
                i = R$string.op_kg_prompt_reason_timeout_swipe;
            }
        }
        return i;
    }

    public int getBiometricTimeoutStringWhenPrompt(BiometricSourceType biometricSourceType, SecurityMode securityMode) {
        int i = 0;
        if (!OpUtils.isWeakFaceUnlockEnabled()) {
            return 0;
        }
        if (isWeakFaceTimeout()) {
            if (!isUnlockingWithBiometricAllowed() || !isUnlockWithFingerprintPossible(getCurrentUser())) {
                int i2 = C061515.f52xdc0e830a[securityMode.ordinal()];
                if (i2 == 1) {
                    i = R$string.op_kg_prompt_reason_face_timeout_pattern;
                } else if (i2 == 2) {
                    i = R$string.op_kg_prompt_reason_face_timeout_pin;
                } else if (i2 == 3) {
                    i = R$string.op_kg_prompt_reason_face_timeout_password;
                }
            } else {
                int i3 = C061515.f52xdc0e830a[securityMode.ordinal()];
                if (i3 == 1) {
                    i = R$string.op_kg_prompt_reason_face_timeout_bouncer_pattern;
                } else if (i3 == 2) {
                    i = R$string.op_kg_prompt_reason_face_timeout_bouncer_pin;
                } else if (i3 == 3) {
                    i = R$string.op_kg_prompt_reason_face_timeout_bouncer_password;
                }
            }
        }
        return i;
    }

    public boolean isUserInLockdown(int i) {
        if (this.mStrongAuthTracker.getStrongAuthForUser(i) == 32) {
            return DEBUG;
        }
        return false;
    }

    public boolean needsSlowUnlockTransition() {
        return this.mNeedsSlowUnlockTransition;
    }

    public StrongAuthTracker getStrongAuthTracker() {
        return this.mStrongAuthTracker;
    }

    /* access modifiers changed from: private */
    public void notifyStrongAuthStateChanged(int i) {
        checkIsHandlerThread();
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onStrongAuthStateChanged(i);
            }
        }
    }

    public boolean isScreenOn() {
        return this.mScreenOn;
    }

    private void dispatchErrorMessage(CharSequence charSequence) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onTrustAgentErrorMessage(charSequence);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void setAssistantVisible(boolean z) {
        this.mAssistantVisible = z;
        updateBiometricListeningState();
    }

    public static KeyguardUpdateMonitor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KeyguardUpdateMonitor(context);
        }
        return sInstance;
    }

    /* access modifiers changed from: protected */
    public void handleStartedWakingUp() {
        Trace.beginSection("KeyguardUpdateMonitor#handleStartedWakingUp");
        updateBiometricListeningState();
        int size = this.mCallbacks.size();
        for (int i = 0; i < size; i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onStartedWakingUp();
            }
        }
        super.handleStartedWakingUp();
        Trace.endSection();
    }

    /* access modifiers changed from: protected */
    public void handleStartedGoingToSleep(int i) {
        clearBiometricRecognized();
        int size = this.mCallbacks.size();
        for (int i2 = 0; i2 < size; i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onStartedGoingToSleep(i);
            }
        }
        this.mGoingToSleep = DEBUG;
        opHandleStartedGoingToSleep();
        this.mCameraLaunched = false;
    }

    /* access modifiers changed from: protected */
    public void handleFinishedGoingToSleep(int i) {
        this.mGoingToSleep = false;
        int size = this.mCallbacks.size();
        for (int i2 = 0; i2 < size; i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onFinishedGoingToSleep(i);
            }
        }
        updateBiometricListeningState();
    }

    /* access modifiers changed from: private */
    public void handleScreenTurnedOn() {
        int size = this.mCallbacks.size();
        int i = 0;
        while (i < size) {
            try {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onScreenTurnedOn();
                }
                i++;
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                sb.append("handleScreenTurnedOn error : ");
                sb.append(e);
                Log.e("KeyguardUpdateMonitor", sb.toString());
            }
        }
        opHandleScreenTurnedOn();
    }

    /* access modifiers changed from: private */
    public void handleScreenTurnedOff() {
        this.mLockIconPressed = false;
        this.mHardwareFingerprintUnavailableRetryCount = 0;
        this.mHardwareFaceUnavailableRetryCount = 0;
        int size = this.mCallbacks.size();
        for (int i = 0; i < size; i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onScreenTurnedOff();
            }
        }
        opHandleScreenTurnedOff();
    }

    /* access modifiers changed from: private */
    public void handleDreamingStateChanged(int i) {
        int size = this.mCallbacks.size();
        boolean z = DEBUG;
        if (i != 1) {
            z = false;
        }
        this.mIsDreaming = z;
        for (int i2 = 0; i2 < size; i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onDreamingStateChanged(this.mIsDreaming);
            }
        }
        updateBiometricListeningState();
    }

    /* access modifiers changed from: private */
    public void handleUserInfoChanged(int i) {
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onUserInfoChanged(i);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleUserUnlocked() {
        this.mNeedsSlowUnlockTransition = resolveNeedsSlowUnlockTransition();
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onUserUnlocked();
            }
        }
    }

    @VisibleForTesting
    protected KeyguardUpdateMonitor(Context context) {
        super(context);
        init();
        this.mContext = context;
        this.mSubscriptionManager = SubscriptionManager.from(context);
        this.mDeviceProvisioned = isDeviceProvisionedInSettingsDb();
        this.mStrongAuthTracker = new StrongAuthTracker(context, new Consumer() {
            public final void accept(Object obj) {
                KeyguardUpdateMonitor.this.notifyStrongAuthStateChanged(((Integer) obj).intValue());
            }
        });
        if (!this.mDeviceProvisioned) {
            watchForDeviceProvisioning();
        }
        if (OpKeyguardUpdateMonitor.IS_SUPPORT_FACE_UNLOCK) {
            watchForFacelockSettings();
        }
        BatteryStatus batteryStatus = new BatteryStatus(1, 100, 0, 0, 0, 0, false);
        this.mBatteryStatus = batteryStatus;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.SERVICE_STATE");
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction("android.media.RINGER_MODE_CHANGED");
        String str = "android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED";
        intentFilter.addAction(str);
        context.registerReceiver(this.mBroadcastReceiver, intentFilter, null, this.mHandler);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.setPriority(1000);
        intentFilter2.addAction("android.intent.action.BOOT_COMPLETED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter2, null, this.mHandler);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.intent.action.TIME_SET");
        intentFilter3.addAction("android.intent.action.USER_INFO_CHANGED");
        intentFilter3.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        intentFilter3.addAction("com.android.facelock.FACE_UNLOCK_STARTED");
        intentFilter3.addAction("com.android.facelock.FACE_UNLOCK_STOPPED");
        intentFilter3.addAction(str);
        intentFilter3.addAction("android.intent.action.USER_UNLOCKED");
        context.registerReceiverAsUser(this.mBroadcastAllReceiver, UserHandle.ALL, intentFilter3, null, this.mHandler);
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionListener);
        try {
            ActivityManager.getService().registerUserSwitchObserver(new UserSwitchObserver() {
                public void onUserSwitching(int i, IRemoteCallback iRemoteCallback) {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(310, i, 0, iRemoteCallback));
                }

                public void onUserSwitchComplete(int i) throws RemoteException {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(314, i, 0));
                }
            }, "KeyguardUpdateMonitor");
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
        this.mTrustManager = (TrustManager) context.getSystemService("trust");
        this.mTrustManager.registerTrustListener(this);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mLockPatternUtils.registerStrongAuthTracker(this.mStrongAuthTracker);
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"));
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint")) {
            this.mFpm = (FingerprintManager) context.getSystemService("fingerprint");
        }
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.biometrics.face")) {
            this.mFaceManager = (FaceManager) context.getSystemService("face");
        }
        if (!(this.mFpm == null && this.mFaceManager == null)) {
            this.mBiometricManager = (BiometricManager) context.getSystemService(BiometricManager.class);
            this.mBiometricManager.registerEnabledOnKeyguardCallback(this.mBiometricEnabledCallback);
        }
        updateBiometricListeningState();
        FingerprintManager fingerprintManager = this.mFpm;
        if (fingerprintManager != null) {
            fingerprintManager.addLockoutResetCallback(this.mFingerprintLockoutResetCallback);
        }
        FaceManager faceManager = this.mFaceManager;
        if (faceManager != null) {
            faceManager.addLockoutResetCallback(this.mFaceLockoutResetCallback);
        }
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskStackListener);
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mIsPrimaryUser = this.mUserManager.isPrimaryUser();
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        this.mLogoutEnabled = this.mDevicePolicyManager.isLogoutEnabled();
        updateAirplaneModeState();
    }

    private void updateAirplaneModeState() {
        if (WirelessUtils.isAirplaneModeOn(this.mContext) && !this.mHandler.hasMessages(329)) {
            this.mHandler.sendEmptyMessage(329);
        }
    }

    public void updateBiometricListeningState() {
        updateFingerprintListeningState();
        updateFaceListeningState();
    }

    /* access modifiers changed from: private */
    public void updateFingerprintListeningState() {
        if (!this.mHandler.hasMessages(336)) {
            String str = "KeyguardUpdateMonitor";
            if (this.mDuringAcquired) {
                Log.d(str, "not update fp listen state during acquired");
                return;
            }
            this.mHandler.removeCallbacks(this.mRetryFingerprintAuthentication);
            boolean shouldListenForFingerprint = shouldListenForFingerprint();
            int i = this.mFingerprintRunningState;
            boolean z = DEBUG;
            if (!(i == 1 || i == 3)) {
                z = false;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("updateFP: ");
            sb.append(shouldListenForFingerprint);
            String str2 = ", ";
            sb.append(str2);
            sb.append(z);
            sb.append(str2);
            sb.append(this.mFingerprintRunningState);
            Log.d(str, sb.toString());
            if (z && !shouldListenForFingerprint) {
                stopListeningForFingerprint();
            } else if (!z && shouldListenForFingerprint) {
                startListeningForFingerprint();
            }
        }
    }

    public void onAuthInterruptDetected(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("onAuthInterruptDetected(");
        sb.append(z);
        sb.append(")");
        Log.d("KeyguardUpdateMonitor", sb.toString());
        if (this.mAuthInterruptActive != z) {
            this.mAuthInterruptActive = z;
            updateFaceListeningState();
        }
    }

    public void requestFaceAuth() {
        Log.d("KeyguardUpdateMonitor", "requestFaceAuth()");
        updateFaceListeningState();
    }

    /* access modifiers changed from: private */
    public void updateFaceListeningState() {
        if (!this.mHandler.hasMessages(336)) {
            this.mHandler.removeCallbacks(this.mRetryFaceAuthentication);
            boolean shouldListenForFace = shouldListenForFace();
            if (this.mFaceRunningState == 1 && !shouldListenForFace) {
                stopListeningForFace();
            } else if (this.mFaceRunningState != 1 && shouldListenForFace) {
                startListeningForFace();
            }
        }
    }

    private boolean shouldListenForFaceAssistant() {
        if (!this.mAssistantVisible || !this.mKeyguardOccluded || this.mUserFaceAuthenticated.get(getCurrentUser(), false) || this.mUserHasTrust.get(getCurrentUser(), false)) {
            return false;
        }
        return DEBUG;
    }

    private boolean shouldListenForFingerprint() {
        boolean z = (this.mKeyguardIsVisible || !this.mDeviceInteractive || ((this.mBouncer && !this.mKeyguardGoingAway) || this.mGoingToSleep || (this.mKeyguardOccluded && this.mIsDreaming))) && !this.mSwitchingUser && !isFingerprintDisabled(getCurrentUser()) && (!this.mKeyguardGoingAway || !this.mDeviceInteractive) && this.mIsPrimaryUser;
        StringBuilder sb = new StringBuilder();
        sb.append("shouldListen: ");
        sb.append(z);
        sb.append(", vis:");
        sb.append(this.mKeyguardIsVisible);
        sb.append(", inter:");
        sb.append(this.mDeviceInteractive);
        sb.append(", bouncer:");
        sb.append(this.mBouncer);
        sb.append(", going:");
        sb.append(this.mKeyguardGoingAway);
        sb.append(", sleep:");
        sb.append(this.mGoingToSleep);
        sb.append(", occlude:");
        sb.append(this.mKeyguardOccluded);
        sb.append(", dream:");
        sb.append(this.mIsDreaming);
        sb.append(", switch:");
        sb.append(this.mSwitchingUser);
        sb.append(", disabled:");
        sb.append(isFingerprintDisabled(getCurrentUser()));
        sb.append(", primary:");
        sb.append(this.mIsPrimaryUser);
        Log.d("KeyguardUpdateMonitor", sb.toString());
        if (!opShouldListenForFingerprint() || !z) {
            return false;
        }
        return DEBUG;
    }

    private boolean shouldListenForFace() {
        boolean z = this.mKeyguardIsVisible && this.mDeviceInteractive && !this.mGoingToSleep;
        int currentUser = getCurrentUser();
        if ((this.mBouncer || this.mAuthInterruptActive || z || shouldListenForFaceAssistant()) && !this.mSwitchingUser && !getUserCanSkipBouncer(currentUser) && !isFaceDisabled(currentUser) && !this.mKeyguardGoingAway && this.mFaceSettingEnabledForUser && !this.mLockIconPressed && this.mUserManager.isUserUnlocked(currentUser) && this.mIsPrimaryUser) {
            return DEBUG;
        }
        return false;
    }

    public void onLockIconPressed() {
        this.mLockIconPressed = DEBUG;
        this.mUserFaceAuthenticated.put(getCurrentUser(), false);
        notifyFacelockStateChanged(0);
        updateFaceListeningState();
    }

    private void startListeningForFingerprint() {
        StringBuilder sb = new StringBuilder();
        sb.append("startListeningForFingerprint(), ");
        sb.append(this.mFingerprintRunningState);
        Log.v("KeyguardUpdateMonitor", sb.toString());
        int i = this.mFingerprintRunningState;
        if (i == 2) {
            setFingerprintRunningState(3);
        } else if (i != 3) {
            int currentUser = getCurrentUser();
            if (isUnlockWithFingerprintPossible(currentUser)) {
                CancellationSignal cancellationSignal = this.mFingerprintCancelSignal;
                if (cancellationSignal != null) {
                    cancellationSignal.cancel();
                }
                this.mFingerprintCancelSignal = new CancellationSignal();
                this.mFpm.authenticate(null, this.mFingerprintCancelSignal, 0, this.mFingerprintAuthenticationCallback, null, currentUser);
                setFingerprintRunningState(1);
            }
        }
    }

    private void startListeningForFace() {
        StringBuilder sb = new StringBuilder();
        sb.append("startListeningForFace, ");
        sb.append(this.mFaceRunningState);
        Log.v("KeyguardUpdateMonitor", sb.toString());
        if (this.mFaceRunningState == 2) {
            setFaceRunningState(3);
            return;
        }
        int currentUser = getCurrentUser();
        if (isUnlockWithFacePossible(currentUser)) {
            CancellationSignal cancellationSignal = this.mFaceCancelSignal;
            if (cancellationSignal != null) {
                cancellationSignal.cancel();
            }
            this.mFaceCancelSignal = new CancellationSignal();
            this.mFaceManager.authenticate(null, this.mFaceCancelSignal, 0, this.mFaceAuthenticationCallback, null, currentUser);
            setFaceRunningState(1);
        }
    }

    public boolean isUnlockingWithBiometricsPossible(int i) {
        if (isUnlockWithFacePossible(i) || isUnlockWithFingerprintPossible(i)) {
            return DEBUG;
        }
        return false;
    }

    public boolean isUnlockWithFingerprintPossible(int i) {
        FingerprintManager fingerprintManager = this.mFpm;
        if (fingerprintManager == null || !fingerprintManager.isHardwareDetected() || isFingerprintDisabled(i) || this.mFpm.getEnrolledFingerprints(i).size() <= 0) {
            return false;
        }
        return DEBUG;
    }

    public boolean isUnlockWithFacePossible(int i) {
        FaceManager faceManager = this.mFaceManager;
        if (faceManager == null || !faceManager.isHardwareDetected() || isFaceDisabled(i) || !this.mFaceManager.hasEnrolledTemplates(i)) {
            return false;
        }
        return DEBUG;
    }

    private void stopListeningForFingerprint() {
        StringBuilder sb = new StringBuilder();
        sb.append("stopListeningForFingerprint, ");
        sb.append(this.mFingerprintRunningState);
        Log.v("KeyguardUpdateMonitor", sb.toString());
        if (this.mFingerprintRunningState == 1) {
            CancellationSignal cancellationSignal = this.mFingerprintCancelSignal;
            if (cancellationSignal != null) {
                cancellationSignal.cancel();
                this.mFingerprintCancelSignal = null;
            }
            setFingerprintRunningState(2);
        }
        if (this.mFingerprintRunningState == 3) {
            setFingerprintRunningState(2);
        }
    }

    private void stopListeningForFace() {
        StringBuilder sb = new StringBuilder();
        sb.append("stopListeningForFace, ");
        sb.append(this.mFaceRunningState);
        Log.v("KeyguardUpdateMonitor", sb.toString());
        if (this.mFaceRunningState == 1) {
            CancellationSignal cancellationSignal = this.mFaceCancelSignal;
            if (cancellationSignal != null) {
                cancellationSignal.cancel();
                this.mFaceCancelSignal = null;
            }
            setFaceRunningState(2);
        }
        if (this.mFaceRunningState == 3) {
            setFaceRunningState(2);
        }
    }

    /* access modifiers changed from: private */
    public boolean isDeviceProvisionedInSettingsDb() {
        if (Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0) {
            return DEBUG;
        }
        return false;
    }

    private void watchForDeviceProvisioning() {
        this.mDeviceProvisionedObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean z) {
                super.onChange(z);
                KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.this;
                keyguardUpdateMonitor.mDeviceProvisioned = keyguardUpdateMonitor.isDeviceProvisionedInSettingsDb();
                if (KeyguardUpdateMonitor.this.mDeviceProvisioned) {
                    KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(308);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("DEVICE_PROVISIONED state = ");
                sb.append(KeyguardUpdateMonitor.this.mDeviceProvisioned);
                Log.d("KeyguardUpdateMonitor", sb.toString());
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("device_provisioned"), false, this.mDeviceProvisionedObserver);
        boolean isDeviceProvisionedInSettingsDb = isDeviceProvisionedInSettingsDb();
        if (isDeviceProvisionedInSettingsDb != this.mDeviceProvisioned) {
            this.mDeviceProvisioned = isDeviceProvisionedInSettingsDb;
            if (this.mDeviceProvisioned) {
                this.mHandler.sendEmptyMessage(308);
            }
        }
    }

    public void setHasLockscreenWallpaper(boolean z) {
        checkIsHandlerThread();
        if (z != this.mHasLockscreenWallpaper) {
            this.mHasLockscreenWallpaper = z;
            for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(size)).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onHasLockscreenWallpaperChanged(z);
                }
            }
        }
    }

    public boolean hasLockscreenWallpaper() {
        return this.mHasLockscreenWallpaper;
    }

    /* access modifiers changed from: private */
    public void handleDevicePolicyManagerStateChanged() {
        updateFingerprintListeningState();
        for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(size)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onDevicePolicyManagerStateChanged();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleUserSwitching(int i, IRemoteCallback iRemoteCallback) {
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onUserSwitching(i);
            }
        }
        try {
            iRemoteCallback.sendResult(null);
        } catch (RemoteException unused) {
        }
    }

    /* access modifiers changed from: private */
    public void handleUserSwitchComplete(int i) {
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onUserSwitchComplete(i);
            }
        }
    }

    public void dispatchBootCompleted() {
        this.mHandler.sendEmptyMessage(313);
    }

    /* access modifiers changed from: private */
    public void handleBootCompleted() {
        if (!this.mBootCompleted) {
            this.mBootCompleted = DEBUG;
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onBootCompleted();
                }
            }
        }
    }

    public boolean hasBootCompleted() {
        return this.mBootCompleted;
    }

    /* access modifiers changed from: private */
    public void handleDeviceProvisioned() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onDeviceProvisioned();
            }
        }
        if (this.mDeviceProvisionedObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mDeviceProvisionedObserver);
            this.mDeviceProvisionedObserver = null;
        }
    }

    /* access modifiers changed from: private */
    public void handlePhoneStateChanged(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("handlePhoneStateChanged(");
        sb.append(str);
        sb.append(")");
        Log.d("KeyguardUpdateMonitor", sb.toString());
        if (TelephonyManager.EXTRA_STATE_IDLE.equals(str)) {
            checkIfHangup(this.mPhoneState, str);
            this.mPhoneState = 0;
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(str)) {
            this.mPhoneState = 2;
        } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(str)) {
            this.mPhoneState = 1;
        }
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onPhoneStateChanged(this.mPhoneState);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleRingerModeChange(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("handleRingerModeChange(");
        sb.append(i);
        sb.append(")");
        Log.d("KeyguardUpdateMonitor", sb.toString());
        this.mRingMode = i;
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onRingerModeChanged(i);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleTimeUpdate() {
        Log.d("KeyguardUpdateMonitor", "handleTimeUpdate");
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onTimeChanged();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleTimeZoneUpdate(String str) {
        Log.d("KeyguardUpdateMonitor", "handleTimeZoneUpdate");
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onTimeZoneChanged(TimeZone.getTimeZone(str));
                keyguardUpdateMonitorCallback.onTimeChanged();
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleBatteryUpdate(BatteryStatus batteryStatus) {
        if (OpKeyguardUpdateMonitor.DEBUG) {
            Log.d("KeyguardUpdateMonitor", "handleBatteryUpdate");
        }
        boolean isBatteryUpdateInteresting = isBatteryUpdateInteresting(this.mBatteryStatus, batteryStatus);
        this.mBatteryStatus = batteryStatus;
        if (isBatteryUpdateInteresting) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onRefreshBatteryInfo(batteryStatus);
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void updateTelephonyCapable(boolean z) {
        if (z != this.mTelephonyCapable) {
            this.mTelephonyCapable = z;
            Iterator it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) it.next()).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onTelephonyCapable(this.mTelephonyCapable);
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x007d  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x00ac  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00f2  */
    @com.android.internal.annotations.VisibleForTesting
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleSimStateChange(int r9, int r10, com.android.internal.telephony.IccCardConstants.State r11) {
        /*
            r8 = this;
            r8.checkIsHandlerThread()
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "handleSimStateChange(subId="
            r0.append(r1)
            r0.append(r9)
            java.lang.String r1 = ", slotId="
            r0.append(r1)
            r0.append(r10)
            java.lang.String r1 = ", state="
            r0.append(r1)
            r0.append(r11)
            java.lang.String r1 = ")"
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "KeyguardUpdateMonitor"
            android.util.Log.d(r1, r0)
            boolean r0 = android.telephony.SubscriptionManager.isValidSubscriptionId(r9)
            r2 = 0
            r3 = 1
            if (r0 != 0) goto L_0x006c
            java.lang.String r0 = "invalid subId in handleSimStateChange()"
            android.util.Log.w(r1, r0)
            com.android.internal.telephony.IccCardConstants$State r0 = com.android.internal.telephony.IccCardConstants.State.ABSENT
            if (r11 != r0) goto L_0x0063
            r8.updateTelephonyCapable(r3)
            java.util.HashMap<java.lang.Integer, com.android.keyguard.KeyguardUpdateMonitor$SimData> r0 = r8.mSimDatas
            java.util.Collection r0 = r0.values()
            java.util.Iterator r0 = r0.iterator()
        L_0x004c:
            boolean r4 = r0.hasNext()
            if (r4 == 0) goto L_0x0061
            java.lang.Object r4 = r0.next()
            com.android.keyguard.KeyguardUpdateMonitor$SimData r4 = (com.android.keyguard.KeyguardUpdateMonitor.SimData) r4
            int r5 = r4.slotId
            if (r5 != r10) goto L_0x004c
            com.android.internal.telephony.IccCardConstants$State r5 = com.android.internal.telephony.IccCardConstants.State.ABSENT
            r4.simState = r5
            goto L_0x004c
        L_0x0061:
            r0 = r3
            goto L_0x006d
        L_0x0063:
            com.android.internal.telephony.IccCardConstants$State r0 = com.android.internal.telephony.IccCardConstants.State.CARD_IO_ERROR
            if (r11 != r0) goto L_0x006b
            r8.updateTelephonyCapable(r3)
            goto L_0x006c
        L_0x006b:
            return
        L_0x006c:
            r0 = r2
        L_0x006d:
            java.util.HashMap<java.lang.Integer, com.android.keyguard.KeyguardUpdateMonitor$SimData> r4 = r8.mSimDatas
            java.lang.Integer r5 = java.lang.Integer.valueOf(r9)
            java.lang.Object r4 = r4.get(r5)
            com.android.keyguard.KeyguardUpdateMonitor$SimData r4 = (com.android.keyguard.KeyguardUpdateMonitor.SimData) r4
            java.lang.String r5 = ", "
            if (r4 != 0) goto L_0x00ac
            com.android.keyguard.KeyguardUpdateMonitor$SimData r4 = new com.android.keyguard.KeyguardUpdateMonitor$SimData
            r4.<init>(r11, r10, r9)
            java.util.HashMap<java.lang.Integer, com.android.keyguard.KeyguardUpdateMonitor$SimData> r6 = r8.mSimDatas
            java.lang.Integer r7 = java.lang.Integer.valueOf(r9)
            r6.put(r7, r4)
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r6 = "init SimData: "
            r4.append(r6)
            r4.append(r9)
            r4.append(r5)
            r4.append(r10)
            r4.append(r5)
            r4.append(r11)
            java.lang.String r4 = r4.toString()
            android.util.Log.d(r1, r4)
            goto L_0x00e2
        L_0x00ac:
            com.android.internal.telephony.IccCardConstants$State r6 = r4.simState
            if (r6 != r11) goto L_0x00ba
            int r6 = r4.subId
            if (r6 != r9) goto L_0x00ba
            int r6 = r4.slotId
            if (r6 == r10) goto L_0x00b9
            goto L_0x00ba
        L_0x00b9:
            r3 = r2
        L_0x00ba:
            r4.simState = r11
            r4.subId = r9
            r4.slotId = r10
            if (r3 == 0) goto L_0x00e2
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r6 = "change SimData: "
            r4.append(r6)
            r4.append(r9)
            r4.append(r5)
            r4.append(r10)
            r4.append(r5)
            r4.append(r11)
            java.lang.String r4 = r4.toString()
            android.util.Log.d(r1, r4)
        L_0x00e2:
            if (r3 != 0) goto L_0x00e6
            if (r0 == 0) goto L_0x0108
        L_0x00e6:
            com.android.internal.telephony.IccCardConstants$State r0 = com.android.internal.telephony.IccCardConstants.State.UNKNOWN
            if (r11 == r0) goto L_0x0108
        L_0x00ea:
            java.util.ArrayList<java.lang.ref.WeakReference<com.android.keyguard.KeyguardUpdateMonitorCallback>> r0 = r8.mCallbacks
            int r0 = r0.size()
            if (r2 >= r0) goto L_0x0108
            java.util.ArrayList<java.lang.ref.WeakReference<com.android.keyguard.KeyguardUpdateMonitorCallback>> r0 = r8.mCallbacks
            java.lang.Object r0 = r0.get(r2)
            java.lang.ref.WeakReference r0 = (java.lang.ref.WeakReference) r0
            java.lang.Object r0 = r0.get()
            com.android.keyguard.KeyguardUpdateMonitorCallback r0 = (com.android.keyguard.KeyguardUpdateMonitorCallback) r0
            if (r0 == 0) goto L_0x0105
            r0.onSimStateChanged(r9, r10, r11)
        L_0x0105:
            int r2 = r2 + 1
            goto L_0x00ea
        L_0x0108:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardUpdateMonitor.handleSimStateChange(int, int, com.android.internal.telephony.IccCardConstants$State):void");
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void handleServiceStateChange(int i, ServiceState serviceState) {
        StringBuilder sb = new StringBuilder();
        sb.append("handleServiceStateChange(subId=");
        sb.append(i);
        sb.append(", serviceState=");
        sb.append(serviceState);
        String str = "KeyguardUpdateMonitor";
        Log.d(str, sb.toString());
        if (!SubscriptionManager.isValidSubscriptionId(i)) {
            Log.w(str, "invalid subId in handleServiceStateChange()");
            return;
        }
        updateTelephonyCapable(DEBUG);
        this.mServiceStates.put(Integer.valueOf(i), serviceState);
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onRefreshCarrierInfo();
                keyguardUpdateMonitorCallback.onServiceStateChanged(i, serviceState);
            }
        }
    }

    public boolean isKeyguardVisible() {
        return this.mKeyguardIsVisible;
    }

    public void onKeyguardVisibilityChanged(boolean z) {
        checkIsHandlerThread();
        StringBuilder sb = new StringBuilder();
        sb.append("onKeyguardVisibilityChanged(");
        sb.append(z);
        sb.append(")");
        Log.d("KeyguardUpdateMonitor", sb.toString());
        this.mKeyguardIsVisible = z;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onKeyguardVisibilityChangedRaw(z);
            }
        }
        opOnKeyguardVisibilityChanged(z);
        if (OpUtils.DEBUG_ONEPLUS) {
            StatusBar phoneStatusBar = OpLsState.getInstance().getPhoneStatusBar();
            if (phoneStatusBar != null) {
                StatusBarWindowController statusBarWindowController = phoneStatusBar.getStatusBarWindowController();
                if (statusBarWindowController != null) {
                    statusBarWindowController.debugBarHeight();
                }
            }
        }
        updateBiometricListeningState();
    }

    /* access modifiers changed from: private */
    public void handleKeyguardReset() {
        Log.d("KeyguardUpdateMonitor", "handleKeyguardReset");
        updateBiometricListeningState();
        this.mNeedsSlowUnlockTransition = resolveNeedsSlowUnlockTransition();
    }

    private boolean resolveNeedsSlowUnlockTransition() {
        if (this.mUserManager.isUserUnlocked(getCurrentUser())) {
            return false;
        }
        return FALLBACK_HOME_COMPONENT.equals(this.mContext.getPackageManager().resolveActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME"), 0).getComponentInfo().getComponentName());
    }

    /* access modifiers changed from: private */
    public void handleKeyguardBouncerChanged(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("handleKeyguardBouncerChanged(");
        sb.append(i);
        sb.append(")");
        Log.d("KeyguardUpdateMonitor", sb.toString());
        boolean z = DEBUG;
        if (i != 1) {
            z = false;
        }
        this.mBouncer = z;
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i2)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onKeyguardBouncerChanged(z);
            }
        }
        if (z) {
            updateLaunchingLeftAffordance(false);
        }
        updateBiometricListeningState();
    }

    /* access modifiers changed from: private */
    public void handleReportEmergencyCallAction() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onEmergencyCallAction();
            }
        }
    }

    private boolean isBatteryUpdateInteresting(BatteryStatus batteryStatus, BatteryStatus batteryStatus2) {
        boolean isPluggedIn = batteryStatus2.isPluggedIn();
        boolean isPluggedIn2 = batteryStatus.isPluggedIn();
        boolean z = isPluggedIn2 && isPluggedIn && batteryStatus.status != batteryStatus2.status;
        if (isPluggedIn2 != isPluggedIn || z || batteryStatus.level != batteryStatus2.level) {
            return DEBUG;
        }
        if (isPluggedIn && batteryStatus2.maxChargingWattage != batteryStatus.maxChargingWattage) {
            return DEBUG;
        }
        if (!isPluggedIn || batteryStatus2.fastCharge == batteryStatus.fastCharge) {
            return false;
        }
        return DEBUG;
    }

    public void removeCallback(KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback) {
        checkIsHandlerThread();
        StringBuilder sb = new StringBuilder();
        sb.append("*** unregister callback for ");
        sb.append(keyguardUpdateMonitorCallback);
        Log.v("KeyguardUpdateMonitor", sb.toString());
        for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
            if (((WeakReference) this.mCallbacks.get(size)).get() == keyguardUpdateMonitorCallback) {
                this.mCallbacks.remove(size);
            }
        }
    }

    public void registerCallback(KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback) {
        checkIsHandlerThread();
        StringBuilder sb = new StringBuilder();
        sb.append("*** register callback for ");
        sb.append(keyguardUpdateMonitorCallback);
        String str = "KeyguardUpdateMonitor";
        Log.v(str, sb.toString());
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            if (((WeakReference) this.mCallbacks.get(i)).get() == keyguardUpdateMonitorCallback) {
                Log.e(str, "Object tried to add another callback", new Exception("Called by"));
                return;
            }
        }
        this.mCallbacks.add(new WeakReference(keyguardUpdateMonitorCallback));
        removeCallback(null);
        sendUpdates(keyguardUpdateMonitorCallback);
    }

    public boolean isSwitchingUser() {
        return this.mSwitchingUser;
    }

    public void setSwitchingUser(boolean z) {
        this.mSwitchingUser = z;
        this.mHandler.post(this.mUpdateBiometricListeningState);
    }

    private void sendUpdates(KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback) {
        keyguardUpdateMonitorCallback.onRefreshBatteryInfo(this.mBatteryStatus);
        keyguardUpdateMonitorCallback.onTimeChanged();
        keyguardUpdateMonitorCallback.onRingerModeChanged(this.mRingMode);
        keyguardUpdateMonitorCallback.onPhoneStateChanged(this.mPhoneState);
        keyguardUpdateMonitorCallback.onRefreshCarrierInfo();
        keyguardUpdateMonitorCallback.onClockVisibilityChanged();
        keyguardUpdateMonitorCallback.onKeyguardVisibilityChangedRaw(this.mKeyguardIsVisible);
        keyguardUpdateMonitorCallback.onTelephonyCapable(this.mTelephonyCapable);
        for (Entry value : this.mSimDatas.entrySet()) {
            SimData simData = (SimData) value.getValue();
            if (SubscriptionManager.isValidSubscriptionId(simData.subId) || simData.simState != State.ABSENT) {
                keyguardUpdateMonitorCallback.onSimStateChanged(simData.subId, simData.slotId, simData.simState);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("skip this invalid callback for sudId: ");
                sb.append(simData.subId);
                Log.d("KeyguardUpdateMonitor", sb.toString());
            }
        }
    }

    public void sendKeyguardReset() {
        this.mHandler.obtainMessage(312).sendToTarget();
    }

    public void sendKeyguardBouncerChanged(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("sendKeyguardBouncerChanged(");
        sb.append(z);
        sb.append(")");
        Log.d("KeyguardUpdateMonitor", sb.toString());
        Message obtainMessage = this.mHandler.obtainMessage(322);
        obtainMessage.arg1 = z ? 1 : 0;
        obtainMessage.sendToTarget();
    }

    public void reportSimUnlocked(int i) {
        opReportSimUnlocked(i);
    }

    public void reportEmergencyCallAction(boolean z) {
        if (!z) {
            this.mHandler.obtainMessage(318).sendToTarget();
            return;
        }
        checkIsHandlerThread();
        handleReportEmergencyCallAction();
    }

    public boolean isDeviceProvisioned() {
        return this.mDeviceProvisioned;
    }

    public ServiceState getServiceState(int i) {
        return (ServiceState) this.mServiceStates.get(Integer.valueOf(i));
    }

    public void clearBiometricRecognized() {
        this.mUserFingerprintAuthenticated.clear();
        this.mUserFaceAuthenticated.clear();
        this.mTrustManager.clearAllBiometricRecognized(BiometricSourceType.FINGERPRINT);
        this.mTrustManager.clearAllBiometricRecognized(BiometricSourceType.FACE);
    }

    public boolean isSimPinVoiceSecure() {
        return isSimPinSecure();
    }

    public boolean isSimPinSecure() {
        for (SubscriptionInfo subscriptionId : getSubscriptionInfo(false)) {
            if (isSimPinSecure(getSimState(subscriptionId.getSubscriptionId()))) {
                return DEBUG;
            }
        }
        return false;
    }

    public State getSimState(int i) {
        if (this.mSimDatas.containsKey(Integer.valueOf(i))) {
            return ((SimData) this.mSimDatas.get(Integer.valueOf(i))).simState;
        }
        return State.UNKNOWN;
    }

    public int getSimSlotId(int i) {
        if (this.mSimDatas.containsKey(Integer.valueOf(i))) {
            return ((SimData) this.mSimDatas.get(Integer.valueOf(i))).slotId;
        }
        Log.w("KeyguardUpdateMonitor", "invalid subid not in keyguard");
        return -1;
    }

    public boolean isOOS() {
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        boolean z = true;
        int i = 0;
        while (true) {
            String str = "KeyguardUpdateMonitor";
            if (i < phoneCount) {
                int[] subId = SubscriptionManager.getSubId(i);
                if (subId != null && subId.length >= 1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("slot id:");
                    sb.append(i);
                    sb.append(" subId:");
                    sb.append(subId[0]);
                    Log.d(str, sb.toString());
                    ServiceState serviceState = (ServiceState) this.mServiceStates.get(Integer.valueOf(subId[0]));
                    if (serviceState != null) {
                        if (serviceState.isEmergencyOnly()) {
                            z = false;
                        }
                        if (!(serviceState.getVoiceRegState() == 1 || serviceState.getVoiceRegState() == 3)) {
                            z = false;
                        }
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("is emergency: ");
                        sb2.append(serviceState.isEmergencyOnly());
                        Log.d(str, sb2.toString());
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("voice state: ");
                        sb3.append(serviceState.getVoiceRegState());
                        Log.d(str, sb3.toString());
                    } else {
                        Log.d(str, "state is NULL");
                    }
                }
                i++;
            } else {
                StringBuilder sb4 = new StringBuilder();
                sb4.append("is Emergency supported: ");
                sb4.append(z);
                Log.d(str, sb4.toString());
                return z;
            }
        }
    }

    private boolean refreshSimState(int i, int i2) {
        State state;
        String str = "KeyguardUpdateMonitor";
        int simState = TelephonyManager.from(this.mContext).getSimState(i2);
        try {
            state = State.intToState(simState);
        } catch (IllegalArgumentException unused) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unknown sim state: ");
            sb.append(simState);
            Log.w(str, sb.toString());
            state = State.UNKNOWN;
        }
        SimData simData = (SimData) this.mSimDatas.get(Integer.valueOf(i));
        boolean z = DEBUG;
        if (simData == null) {
            this.mSimDatas.put(Integer.valueOf(i), new SimData(state, i2, i));
            StringBuilder sb2 = new StringBuilder();
            sb2.append("init refreshSimState: ");
            sb2.append(i);
            String str2 = ", ";
            sb2.append(str2);
            sb2.append(i2);
            sb2.append(str2);
            sb2.append(state);
            Log.d(str, sb2.toString());
        } else {
            if (simData.simState == state) {
                z = false;
            }
            if (z) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("refresh SimData: ");
                sb3.append(i);
                sb3.append(", old: ");
                sb3.append(simData.slotId);
                sb3.append(", slotId:");
                sb3.append(i2);
                sb3.append(", old:");
                sb3.append(simData.simState);
                sb3.append(", state:");
                sb3.append(state);
                Log.d(str, sb3.toString());
            }
            simData.simState = state;
            if (z) {
                simData.slotId = i2;
            }
        }
        return z;
    }

    public static boolean isSimPinSecure(State state) {
        if (state == State.PIN_REQUIRED || state == State.PUK_REQUIRED || state == State.PERM_DISABLED) {
            return DEBUG;
        }
        return false;
    }

    public void dispatchStartedWakingUp() {
        synchronized (this) {
            this.mDeviceInteractive = DEBUG;
        }
        this.mHandler.sendEmptyMessage(319);
    }

    public void dispatchStartedGoingToSleep(int i) {
        OpHandler opHandler = this.mHandler;
        opHandler.sendMessage(opHandler.obtainMessage(321, i, 0));
    }

    public void dispatchFinishedGoingToSleep(int i) {
        synchronized (this) {
            this.mDeviceInteractive = false;
        }
        OpHandler opHandler = this.mHandler;
        opHandler.sendMessage(opHandler.obtainMessage(320, i, 0));
    }

    public void dispatchScreenTurnedOn() {
        synchronized (this) {
            this.mScreenOn = DEBUG;
        }
        this.mHandler.sendEmptyMessage(331);
    }

    public void dispatchScreenTurnedOff() {
        synchronized (this) {
            this.mScreenOn = false;
        }
        this.mHandler.sendEmptyMessage(332);
    }

    public void dispatchDreamingStarted() {
        OpHandler opHandler = this.mHandler;
        opHandler.sendMessage(opHandler.obtainMessage(333, 1, 0));
    }

    public void dispatchDreamingStopped() {
        OpHandler opHandler = this.mHandler;
        opHandler.sendMessage(opHandler.obtainMessage(333, 0, 0));
    }

    public boolean isDeviceInteractive() {
        return this.mDeviceInteractive;
    }

    public boolean isGoingToSleep() {
        return this.mGoingToSleep;
    }

    public int getNextSubIdForState(State state) {
        List subscriptionInfo = getSubscriptionInfo(false);
        int i = -1;
        int i2 = Integer.MAX_VALUE;
        for (int i3 = 0; i3 < subscriptionInfo.size(); i3++) {
            int subscriptionId = ((SubscriptionInfo) subscriptionInfo.get(i3)).getSubscriptionId();
            int slotIndex = SubscriptionManager.getSlotIndex(subscriptionId);
            if (state == getSimState(subscriptionId) && i2 > slotIndex) {
                i = subscriptionId;
                i2 = slotIndex;
            }
        }
        return i;
    }

    public SubscriptionInfo getSubscriptionInfoForSubId(int i) {
        List subscriptionInfo = getSubscriptionInfo(false);
        for (int i2 = 0; i2 < subscriptionInfo.size(); i2++) {
            SubscriptionInfo subscriptionInfo2 = (SubscriptionInfo) subscriptionInfo.get(i2);
            if (i == subscriptionInfo2.getSubscriptionId()) {
                return subscriptionInfo2;
            }
        }
        return null;
    }

    public boolean isLogoutEnabled() {
        return this.mLogoutEnabled;
    }

    /* access modifiers changed from: private */
    public void updateLogoutEnabled() {
        checkIsHandlerThread();
        boolean isLogoutEnabled = this.mDevicePolicyManager.isLogoutEnabled();
        if (this.mLogoutEnabled != isLogoutEnabled) {
            this.mLogoutEnabled = isLogoutEnabled;
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = (KeyguardUpdateMonitorCallback) ((WeakReference) this.mCallbacks.get(i)).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onLogoutEnabledChanged();
                }
            }
        }
    }

    private void checkIsHandlerThread() {
        if (!sDisableHandlerCheckForTesting && !this.mHandler.getLooper().isCurrentThread()) {
            StringBuilder sb = new StringBuilder();
            sb.append("must call on mHandler's thread ");
            sb.append(this.mHandler.getLooper().getThread());
            sb.append(", not ");
            sb.append(Thread.currentThread());
            Log.wtf("KeyguardUpdateMonitor", sb.toString());
        }
    }

    @VisibleForTesting
    public static void disableHandlerCheckForTesting(Instrumentation instrumentation) {
        Preconditions.checkNotNull(instrumentation, "Must only call this method in tests!");
        sDisableHandlerCheckForTesting = DEBUG;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String str;
        printWriter.println("KeyguardUpdateMonitor state:");
        printWriter.println("  SIM States:");
        Iterator it = this.mSimDatas.values().iterator();
        while (true) {
            str = "    ";
            if (!it.hasNext()) {
                break;
            }
            SimData simData = (SimData) it.next();
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(simData.toString());
            printWriter.println(sb.toString());
        }
        printWriter.println("  Subs:");
        if (this.mSubscriptionInfo != null) {
            for (int i = 0; i < this.mSubscriptionInfo.size(); i++) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(str);
                sb2.append(this.mSubscriptionInfo.get(i));
                printWriter.println(sb2.toString());
            }
        }
        printWriter.println("  Service states:");
        for (Integer intValue : this.mServiceStates.keySet()) {
            int intValue2 = intValue.intValue();
            StringBuilder sb3 = new StringBuilder();
            sb3.append(str);
            sb3.append(intValue2);
            sb3.append("=");
            sb3.append(this.mServiceStates.get(Integer.valueOf(intValue2)));
            printWriter.println(sb3.toString());
        }
        FingerprintManager fingerprintManager = this.mFpm;
        String str2 = "    trustManaged=";
        String str3 = "    strongAuthFlags=";
        String str4 = "    possible=";
        String str5 = "    disabled(DPM)=";
        String str6 = "    authSinceBoot=";
        String str7 = "    auth'd=";
        String str8 = "    allowed=";
        String str9 = ")";
        if (fingerprintManager != null && fingerprintManager.isHardwareDetected()) {
            int currentUser = ActivityManager.getCurrentUser();
            int strongAuthForUser = this.mStrongAuthTracker.getStrongAuthForUser(currentUser);
            StringBuilder sb4 = new StringBuilder();
            sb4.append("  Fingerprint state (user=");
            sb4.append(currentUser);
            sb4.append(str9);
            printWriter.println(sb4.toString());
            StringBuilder sb5 = new StringBuilder();
            sb5.append(str8);
            sb5.append(isUnlockingWithBiometricAllowed());
            printWriter.println(sb5.toString());
            StringBuilder sb6 = new StringBuilder();
            sb6.append(str7);
            sb6.append(this.mUserFingerprintAuthenticated.get(currentUser));
            printWriter.println(sb6.toString());
            StringBuilder sb7 = new StringBuilder();
            sb7.append(str6);
            sb7.append(getStrongAuthTracker().hasUserAuthenticatedSinceBoot());
            printWriter.println(sb7.toString());
            StringBuilder sb8 = new StringBuilder();
            sb8.append(str5);
            sb8.append(isFingerprintDisabled(currentUser));
            printWriter.println(sb8.toString());
            StringBuilder sb9 = new StringBuilder();
            sb9.append(str4);
            sb9.append(isUnlockWithFingerprintPossible(currentUser));
            printWriter.println(sb9.toString());
            StringBuilder sb10 = new StringBuilder();
            sb10.append("    listening: actual=");
            sb10.append(this.mFingerprintRunningState);
            sb10.append(" expected=");
            sb10.append(shouldListenForFingerprint() ? 1 : 0);
            printWriter.println(sb10.toString());
            StringBuilder sb11 = new StringBuilder();
            sb11.append(str3);
            sb11.append(Integer.toHexString(strongAuthForUser));
            printWriter.println(sb11.toString());
            StringBuilder sb12 = new StringBuilder();
            sb12.append(str2);
            sb12.append(getUserTrustIsManaged(currentUser));
            printWriter.println(sb12.toString());
        }
        FaceManager faceManager = this.mFaceManager;
        if (faceManager != null && faceManager.isHardwareDetected()) {
            int currentUser2 = ActivityManager.getCurrentUser();
            int strongAuthForUser2 = this.mStrongAuthTracker.getStrongAuthForUser(currentUser2);
            StringBuilder sb13 = new StringBuilder();
            sb13.append("  Face authentication state (user=");
            sb13.append(currentUser2);
            sb13.append(str9);
            printWriter.println(sb13.toString());
            StringBuilder sb14 = new StringBuilder();
            sb14.append(str8);
            sb14.append(isUnlockingWithBiometricAllowed());
            printWriter.println(sb14.toString());
            StringBuilder sb15 = new StringBuilder();
            sb15.append(str7);
            sb15.append(this.mUserFaceAuthenticated.get(currentUser2));
            printWriter.println(sb15.toString());
            StringBuilder sb16 = new StringBuilder();
            sb16.append(str6);
            sb16.append(getStrongAuthTracker().hasUserAuthenticatedSinceBoot());
            printWriter.println(sb16.toString());
            StringBuilder sb17 = new StringBuilder();
            sb17.append(str5);
            sb17.append(isFaceDisabled(currentUser2));
            printWriter.println(sb17.toString());
            StringBuilder sb18 = new StringBuilder();
            sb18.append(str4);
            sb18.append(isUnlockWithFacePossible(currentUser2));
            printWriter.println(sb18.toString());
            StringBuilder sb19 = new StringBuilder();
            sb19.append(str3);
            sb19.append(Integer.toHexString(strongAuthForUser2));
            printWriter.println(sb19.toString());
            StringBuilder sb20 = new StringBuilder();
            sb20.append(str2);
            sb20.append(getUserTrustIsManaged(currentUser2));
            printWriter.println(sb20.toString());
            StringBuilder sb21 = new StringBuilder();
            sb21.append("    enabledByUser=");
            sb21.append(this.mFaceSettingEnabledForUser);
            printWriter.println(sb21.toString());
        }
        if (this.mStrongAuthTracker != null) {
            StringBuilder sb22 = new StringBuilder();
            sb22.append("    Tracker.isWeakFaceTimeout=");
            sb22.append(this.mStrongAuthTracker.isWeakFaceTimeout());
            printWriter.println(sb22.toString());
        }
        opDump(fileDescriptor, printWriter, strArr);
    }
}

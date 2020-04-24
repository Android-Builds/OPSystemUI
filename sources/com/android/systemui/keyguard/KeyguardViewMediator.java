package com.android.systemui.keyguard;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.hardware.biometrics.BiometricSourceType;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.Builder;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.airbnb.lottie.C0526R;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.util.LatencyTracker;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardDisplayManager;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitor.StrongAuthTracker;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R$bool;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.util.InjectionInflationController;
import com.oneplus.plugin.OpLsState;
import com.oneplus.sarah.SarahClient;
import com.oneplus.systemui.keyguard.OpKeyguardViewMediator;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.worklife.OPWLBHelper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class KeyguardViewMediator extends OpKeyguardViewMediator {
    private static final Intent USER_PRESENT_INTENT = new Intent("android.intent.action.USER_PRESENT").addFlags(606076928);
    private AlarmManager mAlarmManager;
    private boolean mAodShowing;
    private AudioManager mAudioManager;
    private boolean mBootCompleted;
    private boolean mBootSendUserPresent;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction())) {
                OPWLBHelper.getInstance(context).sendShutDownBroadcast();
                synchronized (KeyguardViewMediator.this) {
                    KeyguardViewMediator.this.mShuttingDown = true;
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public CharSequence mCustomMessage;
    private final BroadcastReceiver mDelayedLockBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD".equals(intent.getAction())) {
                int intExtra = intent.getIntExtra("seq", 0);
                StringBuilder sb = new StringBuilder();
                sb.append("received DELAYED_KEYGUARD_ACTION with seq = ");
                sb.append(intExtra);
                sb.append(", mDelayedShowingSequence = ");
                sb.append(KeyguardViewMediator.this.mDelayedShowingSequence);
                Log.d("KeyguardViewMediator", sb.toString());
                synchronized (KeyguardViewMediator.this) {
                    if (KeyguardViewMediator.this.mDelayedShowingSequence == intExtra) {
                        KeyguardViewMediator.this.doKeyguardLocked(null);
                    }
                }
                return;
            }
            if ("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK".equals(intent.getAction())) {
                int intExtra2 = intent.getIntExtra("seq", 0);
                int intExtra3 = intent.getIntExtra("android.intent.extra.USER_ID", 0);
                if (intExtra3 != 0) {
                    synchronized (KeyguardViewMediator.this) {
                        if (KeyguardViewMediator.this.mDelayedProfileShowingSequence == intExtra2) {
                            KeyguardViewMediator.this.lockProfile(intExtra3);
                        }
                    }
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public int mDelayedProfileShowingSequence;
    /* access modifiers changed from: private */
    public int mDelayedShowingSequence;
    /* access modifiers changed from: private */
    public boolean mDeviceInteractive;
    private final DismissCallbackRegistry mDismissCallbackRegistry = new DismissCallbackRegistry();
    private IKeyguardDrawnCallback mDrawnCallback;
    private IKeyguardExitCallback mExitSecureCallback;
    private boolean mExternallyEnabled = true;
    private boolean mGoingToSleep;
    /* access modifiers changed from: private */
    public OpHandler mHandler = new OpHandler(Looper.myLooper(), null, true) {
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 101) {
                switch (i) {
                    case 1:
                        KeyguardViewMediator.this.handleShow((Bundle) message.obj);
                        return;
                    case 2:
                        KeyguardViewMediator.this.handleHide();
                        return;
                    case 3:
                        KeyguardViewMediator.this.handleReset();
                        return;
                    case 4:
                        Trace.beginSection("KeyguardViewMediator#handleMessage VERIFY_UNLOCK");
                        KeyguardViewMediator.this.handleVerifyUnlock();
                        Trace.endSection();
                        return;
                    case 5:
                        KeyguardViewMediator.this.handleNotifyFinishedGoingToSleep();
                        return;
                    case 6:
                        Trace.beginSection("KeyguardViewMediator#handleMessage NOTIFY_SCREEN_TURNING_ON");
                        KeyguardViewMediator.this.handleNotifyScreenTurningOn((IKeyguardDrawnCallback) message.obj);
                        Trace.endSection();
                        return;
                    case 7:
                        Trace.beginSection("KeyguardViewMediator#handleMessage KEYGUARD_DONE");
                        KeyguardViewMediator.this.handleKeyguardDone();
                        Trace.endSection();
                        return;
                    case 8:
                        Trace.beginSection("KeyguardViewMediator#handleMessage KEYGUARD_DONE_DRAWING");
                        KeyguardViewMediator.this.handleKeyguardDoneDrawing();
                        Trace.endSection();
                        return;
                    case 9:
                        Trace.beginSection("KeyguardViewMediator#handleMessage SET_OCCLUDED");
                        KeyguardViewMediator keyguardViewMediator = KeyguardViewMediator.this;
                        boolean z = true;
                        boolean z2 = message.arg1 != 0;
                        if (message.arg2 == 0) {
                            z = false;
                        }
                        keyguardViewMediator.handleSetOccluded(z2, z);
                        Trace.endSection();
                        return;
                    case 10:
                        synchronized (KeyguardViewMediator.this) {
                            KeyguardViewMediator.this.doKeyguardLocked((Bundle) message.obj);
                        }
                        return;
                    case 11:
                        DismissMessage dismissMessage = (DismissMessage) message.obj;
                        KeyguardViewMediator.this.handleDismiss(dismissMessage.getCallback(), dismissMessage.getMessage());
                        return;
                    case 12:
                        break;
                    case 13:
                        Trace.beginSection("KeyguardViewMediator#handleMessage KEYGUARD_DONE_PENDING_TIMEOUT");
                        Log.w("KeyguardViewMediator", "Timeout while waiting for activity drawn!");
                        Trace.endSection();
                        return;
                    case 14:
                        Trace.beginSection("KeyguardViewMediator#handleMessage NOTIFY_STARTED_WAKING_UP");
                        KeyguardViewMediator.this.handleNotifyStartedWakingUp();
                        Trace.endSection();
                        return;
                    case 15:
                        Trace.beginSection("KeyguardViewMediator#handleMessage NOTIFY_SCREEN_TURNED_ON");
                        KeyguardViewMediator.this.handleNotifyScreenTurnedOn();
                        Trace.endSection();
                        return;
                    case 16:
                        KeyguardViewMediator.this.handleNotifyScreenTurnedOff();
                        return;
                    case 17:
                        KeyguardViewMediator.this.handleNotifyStartedGoingToSleep();
                        return;
                    case 18:
                        KeyguardViewMediator.this.handleSystemReady();
                        return;
                    default:
                        return;
                }
            } else {
                Log.d("KeyguardViewMediator", "START_KEYGUARD_EXIT_ANIM_TIMEOUT");
            }
            Trace.beginSection("KeyguardViewMediator#handleMessage START_KEYGUARD_EXIT_ANIM");
            StartKeyguardExitAnimParams startKeyguardExitAnimParams = (StartKeyguardExitAnimParams) message.obj;
            KeyguardViewMediator.this.handleStartKeyguardExitAnimation(startKeyguardExitAnimParams.startTime, startKeyguardExitAnimParams.fadeoutDuration);
            FalsingManagerFactory.getInstance(KeyguardViewMediator.this.mContext).onSucccessfulUnlock();
            Trace.endSection();
        }
    };
    /* access modifiers changed from: private */
    public Animation mHideAnimation;
    /* access modifiers changed from: private */
    public final Runnable mHideAnimationFinishedRunnable = new Runnable() {
        public final void run() {
            KeyguardViewMediator.this.lambda$new$4$KeyguardViewMediator();
        }
    };
    /* access modifiers changed from: private */
    public boolean mHideAnimationRun = false;
    /* access modifiers changed from: private */
    public boolean mHideAnimationRunning = false;
    private boolean mHiding;
    private boolean mInputRestricted;
    /* access modifiers changed from: private */
    public KeyguardDisplayManager mKeyguardDisplayManager;
    /* access modifiers changed from: private */
    public boolean mKeyguardDonePending = false;
    private final Runnable mKeyguardGoingAwayRunnable = new Runnable() {
        public void run() {
            Trace.beginSection("KeyguardViewMediator.mKeyGuardGoingAwayRunnable");
            Log.d("KeyguardViewMediator", "keyguardGoingAway");
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.keyguardGoingAway();
            int i = (KeyguardViewMediator.this.mStatusBarKeyguardViewManager.shouldDisableWindowAnimationsForUnlock() || KeyguardViewMediator.this.mStatusBar.getFacelockController().isScreenOffUnlock()) ? 2 : 0;
            if (KeyguardViewMediator.this.mStatusBarKeyguardViewManager.isGoingToNotificationShade() || (KeyguardViewMediator.this.mWakeAndUnlocking && KeyguardViewMediator.this.mPulsing)) {
                i |= 1;
            }
            if (KeyguardViewMediator.this.mStatusBarKeyguardViewManager.isUnlockWithWallpaper()) {
                i |= 4;
            }
            KeyguardViewMediator.this.mUpdateMonitor.setKeyguardGoingAway(true);
            KeyguardViewMediator.this.mUiOffloadThread.submit(new Runnable(i) {
                private final /* synthetic */ int f$0;

                {
                    this.f$0 = r1;
                }

                public final void run() {
                    C09036.lambda$run$0(this.f$0);
                }
            });
            Trace.endSection();
        }

        static /* synthetic */ void lambda$run$0(int i) {
            try {
                ActivityTaskManager.getService().keyguardGoingAway(i);
            } catch (RemoteException e) {
                Log.e("KeyguardViewMediator", "Error while calling WindowManager", e);
            }
        }
    };
    /* access modifiers changed from: private */
    public final ArrayList<IKeyguardStateCallback> mKeyguardStateCallbacks = new ArrayList<>();
    /* access modifiers changed from: private */
    public final SparseArray<State> mLastSimStates = new SparseArray<>();
    private boolean mLockLater;
    /* access modifiers changed from: private */
    public LockPatternUtils mLockPatternUtils;
    private int mLockSoundId;
    private int mLockSoundStreamId;
    private float mLockSoundVolume;
    private SoundPool mLockSounds;
    private boolean mNeedToReshowWhenReenabled = false;
    private boolean mOccluded = false;
    private PowerManager mPM;
    private boolean mPendingLock;
    private boolean mPendingReset;
    private String mPhoneState = TelephonyManager.EXTRA_STATE_IDLE;
    /* access modifiers changed from: private */
    public boolean mPowerKeyCameraLaunching = false;
    /* access modifiers changed from: private */
    public boolean mPulsing;
    private WakeLock mShowKeyguardWakeLock;
    /* access modifiers changed from: private */
    public boolean mShowing;
    /* access modifiers changed from: private */
    public boolean mShuttingDown;
    /* access modifiers changed from: private */
    public StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private StatusBarManager mStatusBarManager;
    private boolean mSystemReady;
    /* access modifiers changed from: private */
    public TrustManager mTrustManager;
    private int mTrustedSoundId;
    /* access modifiers changed from: private */
    public final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));
    private int mUiSoundsStreamType;
    private int mUnlockSoundId;
    KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        public void onUserInfoChanged(int i) {
        }

        public void onUserSwitching(int i) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.resetKeyguardDonePendingLocked();
                if (KeyguardViewMediator.this.mLockPatternUtils.isLockScreenDisabled(i)) {
                    KeyguardViewMediator.this.dismiss(null, null);
                } else {
                    KeyguardViewMediator.this.resetStateLocked();
                }
                KeyguardViewMediator.this.adjustStatusBarLocked();
            }
        }

        public void onUserSwitchComplete(int i) {
            if (i != 0) {
                UserInfo userInfo = UserManager.get(KeyguardViewMediator.this.mContext).getUserInfo(i);
                if (userInfo != null && !KeyguardViewMediator.this.mLockPatternUtils.isSecure(i)) {
                    if (userInfo.isGuest() || userInfo.isDemo()) {
                        KeyguardViewMediator.this.dismiss(null, null);
                    }
                }
            }
        }

        public void onClockVisibilityChanged() {
            KeyguardViewMediator.this.adjustStatusBarLocked();
        }

        public void onDeviceProvisioned() {
            KeyguardViewMediator.this.sendUserPresentBroadcast();
            synchronized (KeyguardViewMediator.this) {
                if (KeyguardViewMediator.this.mustNotUnlockCurrentUser()) {
                    KeyguardViewMediator.this.doKeyguardLocked(null);
                }
            }
        }

        public void onSimStateChanged(int i, int i2, State state) {
            StringBuilder sb = new StringBuilder();
            sb.append("onSimStateChanged(subId=");
            sb.append(i);
            sb.append(", slotId=");
            sb.append(i2);
            sb.append(",state=");
            sb.append(state);
            sb.append(")");
            Log.d("KeyguardViewMediator", sb.toString());
            int size = KeyguardViewMediator.this.mKeyguardStateCallbacks.size();
            boolean isSimPinSecure = KeyguardViewMediator.this.mUpdateMonitor.isSimPinSecure();
            boolean z = true;
            for (int i3 = size - 1; i3 >= 0; i3--) {
                try {
                    ((IKeyguardStateCallback) KeyguardViewMediator.this.mKeyguardStateCallbacks.get(i3)).onSimSecureStateChanged(isSimPinSecure);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onSimSecureStateChanged", e);
                    if (e instanceof DeadObjectException) {
                        KeyguardViewMediator.this.mKeyguardStateCallbacks.remove(i3);
                    }
                }
            }
            synchronized (KeyguardViewMediator.this) {
                State state2 = (State) KeyguardViewMediator.this.mLastSimStates.get(i2);
                if (state2 != State.PIN_REQUIRED) {
                    if (state2 != State.PUK_REQUIRED) {
                        z = false;
                    }
                }
                if (state != State.UNKNOWN) {
                    KeyguardViewMediator.this.mLastSimStates.append(i2, state);
                }
            }
            switch (C09047.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
                case 1:
                case 2:
                    synchronized (KeyguardViewMediator.this) {
                        if (KeyguardViewMediator.this.shouldWaitForProvisioning()) {
                            if (!KeyguardViewMediator.this.mShowing) {
                                Log.d("KeyguardViewMediator", "ICC_ABSENT isn't showing, we need to show the keyguard since the device isn't provisioned yet.");
                                KeyguardViewMediator.this.doKeyguardLocked(null);
                            } else {
                                KeyguardViewMediator.this.resetStateLocked();
                            }
                        }
                        if (state == State.ABSENT && z) {
                            Log.d("KeyguardViewMediator", "SIM moved to ABSENT when the previous state was locked. Reset the state.");
                            KeyguardViewMediator.this.resetStateLocked();
                        }
                    }
                    return;
                case 3:
                case 4:
                    synchronized (KeyguardViewMediator.this) {
                        if (!KeyguardViewMediator.this.mShowing) {
                            Log.d("KeyguardViewMediator", "INTENT_VALUE_ICC_LOCKED and keygaurd isn't showing; need to show keyguard so user can enter sim pin");
                            KeyguardViewMediator.this.doKeyguardLocked(null);
                        } else {
                            KeyguardViewMediator.this.resetStateLocked();
                        }
                    }
                    return;
                case 5:
                    synchronized (KeyguardViewMediator.this) {
                        if (!KeyguardViewMediator.this.mShowing) {
                            Log.d("KeyguardViewMediator", "PERM_DISABLED and keygaurd isn't showing.");
                            KeyguardViewMediator.this.doKeyguardLocked(null);
                        } else {
                            Log.d("KeyguardViewMediator", "PERM_DISABLED, resetStateLocked toshow permanently disabled message in lockscreen.");
                            KeyguardViewMediator.this.resetStateLocked();
                        }
                    }
                    return;
                case 6:
                    synchronized (KeyguardViewMediator.this) {
                        String str = "KeyguardViewMediator";
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("READY, reset state? ");
                        sb2.append(KeyguardViewMediator.this.mShowing);
                        Log.d(str, sb2.toString());
                        if (KeyguardViewMediator.this.mShowing && z) {
                            Log.d("KeyguardViewMediator", "SIM moved to READY when the previous state was locked. Reset the state.");
                            KeyguardViewMediator.this.resetStateLocked();
                        }
                    }
                    return;
                default:
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("Unspecific state: ");
                    sb3.append(state);
                    Log.v("KeyguardViewMediator", sb3.toString());
                    return;
            }
        }

        public void onBiometricAuthFailed(BiometricSourceType biometricSourceType) {
            int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            if (KeyguardViewMediator.this.mLockPatternUtils.isSecure(currentUser)) {
                KeyguardViewMediator.this.mLockPatternUtils.getDevicePolicyManager().reportFailedBiometricAttempt(currentUser);
            }
        }

        public void onBiometricAuthenticated(int i, BiometricSourceType biometricSourceType) {
            if (KeyguardViewMediator.this.mLockPatternUtils.isSecure(i)) {
                KeyguardViewMediator.this.mLockPatternUtils.getDevicePolicyManager().reportSuccessfulBiometricAttempt(i);
            }
        }

        public void onTrustChanged(int i) {
            if (i == KeyguardUpdateMonitor.getCurrentUser()) {
                synchronized (KeyguardViewMediator.this) {
                    KeyguardViewMediator.this.notifyTrustedChangedLocked(KeyguardViewMediator.this.mUpdateMonitor.getUserHasTrust(i));
                }
            }
        }

        public void onHasLockscreenWallpaperChanged(boolean z) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.notifyHasLockscreenWallpaperChanged(z);
            }
        }
    };
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mUpdateMonitor;
    ViewMediatorCallback mViewMediatorCallback = new ViewMediatorCallback() {
        public void userActivity() {
            KeyguardViewMediator.this.userActivity();
        }

        public void keyguardDone(boolean z, int i) {
            if (i == ActivityManager.getCurrentUser()) {
                KeyguardViewMediator.this.tryKeyguardDone();
            }
        }

        public void keyguardDoneDrawing() {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#keyguardDoneDrawing");
            KeyguardViewMediator.this.mHandler.sendEmptyMessage(8);
            Trace.endSection();
        }

        public void setNeedsInput(boolean z) {
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.setNeedsInput(z);
        }

        public void keyguardDonePending(boolean z, int i) {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#keyguardDonePending");
            if (i != ActivityManager.getCurrentUser()) {
                Trace.endSection();
                return;
            }
            if (KeyguardViewMediator.this.mUpdateMonitor.isFacelockUnlocking()) {
                KeyguardViewMediator.this.changePanelAlpha(1, OpKeyguardViewMediator.AUTHENTICATE_FACEUNLOCK);
            }
            KeyguardViewMediator.this.mKeyguardDonePending = true;
            KeyguardViewMediator.this.mHideAnimationRun = true;
            KeyguardViewMediator.this.mHideAnimationRunning = true;
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.startPreHideAnimation(KeyguardViewMediator.this.mHideAnimationFinishedRunnable);
            KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(13, 3000);
            Trace.endSection();
        }

        public void keyguardGone() {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#keyguardGone");
            KeyguardViewMediator.this.mKeyguardDisplayManager.hide();
            Trace.endSection();
        }

        public void readyForKeyguardDone() {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#readyForKeyguardDone");
            if (KeyguardViewMediator.this.mKeyguardDonePending) {
                KeyguardViewMediator.this.mKeyguardDonePending = false;
                KeyguardViewMediator.this.tryKeyguardDone();
            }
            Trace.endSection();
        }

        public void resetKeyguard() {
            KeyguardViewMediator.this.resetStateLocked();
        }

        public void onCancelClicked() {
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.onCancelClicked();
        }

        public void onBouncerVisiblityChanged(boolean z) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.adjustStatusBarLocked(z);
            }
        }

        public void playTrustedSound() {
            KeyguardViewMediator.this.playTrustedSound();
        }

        public boolean isScreenOn() {
            return KeyguardViewMediator.this.mDeviceInteractive;
        }

        public int getBouncerPromptReason() {
            int currentUser = ActivityManager.getCurrentUser();
            boolean isTrustUsuallyManaged = KeyguardViewMediator.this.mTrustManager.isTrustUsuallyManaged(currentUser);
            boolean z = isTrustUsuallyManaged || (KeyguardViewMediator.this.mUpdateMonitor.isUnlockingWithBiometricsPossible(currentUser) || KeyguardViewMediator.this.mUpdateMonitor.isUnlockWithFacelockPossible());
            StrongAuthTracker strongAuthTracker = KeyguardViewMediator.this.mUpdateMonitor.getStrongAuthTracker();
            int strongAuthForUser = strongAuthTracker.getStrongAuthForUser(currentUser);
            if (z && !strongAuthTracker.hasUserAuthenticatedSinceBoot()) {
                return 1;
            }
            if (z && (strongAuthForUser & 16) != 0) {
                return 2;
            }
            if (z && (strongAuthForUser & 2) != 0) {
                return 3;
            }
            if (isTrustUsuallyManaged && (strongAuthForUser & 4) != 0) {
                return 4;
            }
            if (!z || (strongAuthForUser & 8) == 0) {
                return 0;
            }
            return 5;
        }

        public CharSequence consumeCustomMessage() {
            CharSequence access$2500 = KeyguardViewMediator.this.mCustomMessage;
            KeyguardViewMediator.this.mCustomMessage = null;
            return access$2500;
        }

        public void tryToStartFaceLockFromBouncer() {
            if (KeyguardViewMediator.this.mStatusBar != null && KeyguardViewMediator.this.mStatusBar.getFacelockController() != null) {
                KeyguardViewMediator.this.mStatusBar.getFacelockController().tryToStartFaceLock(true);
            }
        }

        public void reportMDMEvent(String str, String str2, String str3) {
            OpMdmLogger.log(str, str2, str3);
        }

        public void startPowerKeyLaunchCamera() {
            Log.d("KeyguardViewMediator", "startPowerKeyLaunchCamera");
            KeyguardViewMediator.this.mPowerKeyCameraLaunching = true;
            OpHandler access$1400 = KeyguardViewMediator.this.mHandler;
            StartKeyguardExitAnimParams startKeyguardExitAnimParams = new StartKeyguardExitAnimParams(SystemClock.uptimeMillis() + KeyguardViewMediator.this.mHideAnimation.getStartOffset(), KeyguardViewMediator.this.mHideAnimation.getDuration());
            KeyguardViewMediator.this.mHandler.sendMessageDelayed(access$1400.obtainMessage(C0526R.styleable.AppCompatTheme_switchStyle, startKeyguardExitAnimParams), 1000);
        }
    };
    private boolean mWaitingUntilKeyguardVisible = false;
    /* access modifiers changed from: private */
    public boolean mWakeAndUnlocking;
    private WorkLockActivityController mWorkLockController;

    /* renamed from: com.android.systemui.keyguard.KeyguardViewMediator$7 */
    static /* synthetic */ class C09047 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[State.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(12:0|1|2|3|4|5|6|7|8|9|10|(3:11|12|14)) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:11:0x0040 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        /* JADX WARNING: Missing exception handler attribute for start block: B:7:0x002a */
        /* JADX WARNING: Missing exception handler attribute for start block: B:9:0x0035 */
        static {
            /*
                com.android.internal.telephony.IccCardConstants$State[] r0 = com.android.internal.telephony.IccCardConstants.State.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State = r0
                int[] r0 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.android.internal.telephony.IccCardConstants$State r1 = com.android.internal.telephony.IccCardConstants.State.NOT_READY     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.internal.telephony.IccCardConstants$State r1 = com.android.internal.telephony.IccCardConstants.State.ABSENT     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x002a }
                com.android.internal.telephony.IccCardConstants$State r1 = com.android.internal.telephony.IccCardConstants.State.PIN_REQUIRED     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                int[] r0 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x0035 }
                com.android.internal.telephony.IccCardConstants$State r1 = com.android.internal.telephony.IccCardConstants.State.PUK_REQUIRED     // Catch:{ NoSuchFieldError -> 0x0035 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0035 }
                r2 = 4
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0035 }
            L_0x0035:
                int[] r0 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x0040 }
                com.android.internal.telephony.IccCardConstants$State r1 = com.android.internal.telephony.IccCardConstants.State.PERM_DISABLED     // Catch:{ NoSuchFieldError -> 0x0040 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0040 }
                r2 = 5
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0040 }
            L_0x0040:
                int[] r0 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x004b }
                com.android.internal.telephony.IccCardConstants$State r1 = com.android.internal.telephony.IccCardConstants.State.READY     // Catch:{ NoSuchFieldError -> 0x004b }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x004b }
                r2 = 6
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x004b }
            L_0x004b:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.keyguard.KeyguardViewMediator.C09047.<clinit>():void");
        }
    }

    private static class DismissMessage {
        private final IKeyguardDismissCallback mCallback;
        private final CharSequence mMessage;

        DismissMessage(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) {
            this.mCallback = iKeyguardDismissCallback;
            this.mMessage = charSequence;
        }

        public IKeyguardDismissCallback getCallback() {
            return this.mCallback;
        }

        public CharSequence getMessage() {
            return this.mMessage;
        }
    }

    private static class StartKeyguardExitAnimParams {
        long fadeoutDuration;
        long startTime;

        private StartKeyguardExitAnimParams(long j, long j2) {
            this.startTime = j;
            this.fadeoutDuration = j2;
        }
    }

    public void onShortPowerPressedGoHome() {
    }

    public void userActivity() {
        this.mPM.userActivity(SystemClock.uptimeMillis(), false);
    }

    /* access modifiers changed from: 0000 */
    public boolean mustNotUnlockCurrentUser() {
        return UserManager.isSplitSystemUser() && KeyguardUpdateMonitor.getCurrentUser() == 0;
    }

    private void setupLocked() {
        this.mPM = (PowerManager) this.mContext.getSystemService("power");
        this.mTrustManager = (TrustManager) this.mContext.getSystemService("trust");
        this.mShowKeyguardWakeLock = this.mPM.newWakeLock(1, "show keyguard");
        boolean z = false;
        this.mShowKeyguardWakeLock.setReferenceCounted(false);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD");
        intentFilter2.addAction("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK");
        this.mContext.registerReceiver(this.mDelayedLockBroadcastReceiver, intentFilter2, "com.android.systemui.permission.SELF", null);
        this.mKeyguardDisplayManager = new KeyguardDisplayManager(this.mContext, new InjectionInflationController(SystemUIFactory.getInstance().getRootComponent()));
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        KeyguardUpdateMonitor.setCurrentUser(ActivityManager.getCurrentUser());
        if (this.mContext.getResources().getBoolean(R$bool.config_enableKeyguardService)) {
            if (!shouldWaitForProvisioning() && !this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser())) {
                z = true;
            }
            setShowingLocked(z, this.mAodShowing, true);
        } else {
            setShowingLocked(false, this.mAodShowing, true);
        }
        this.mStatusBarKeyguardViewManager = SystemUIFactory.getInstance().createStatusBarKeyguardViewManager(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mDeviceInteractive = this.mPM.isInteractive();
        this.mLockSounds = new Builder().setMaxStreams(1).setAudioAttributes(new AudioAttributes.Builder().setUsage(13).setContentType(4).build()).build();
        String string = Global.getString(contentResolver, "lock_sound");
        if (string != null) {
            this.mLockSoundId = this.mLockSounds.load(string, 1);
        }
        String str = "KeyguardViewMediator";
        if (string == null || this.mLockSoundId == 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("failed to load lock sound from ");
            sb.append(string);
            Log.w(str, sb.toString());
        }
        String string2 = Global.getString(contentResolver, "unlock_sound");
        if (string2 != null) {
            this.mUnlockSoundId = this.mLockSounds.load(string2, 1);
        }
        if (string2 == null || this.mUnlockSoundId == 0) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("failed to load unlock sound from ");
            sb2.append(string2);
            Log.w(str, sb2.toString());
        }
        String string3 = Global.getString(contentResolver, "trusted_sound");
        if (string3 != null) {
            this.mTrustedSoundId = this.mLockSounds.load(string3, 1);
        }
        if (string3 == null || this.mTrustedSoundId == 0) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("failed to load trusted sound from ");
            sb3.append(string3);
            Log.w(str, sb3.toString());
        }
        this.mLockSoundVolume = (float) Math.pow(10.0d, (double) (((float) this.mContext.getResources().getInteger(17694827)) / 20.0f));
        this.mHideAnimation = AnimationUtils.loadAnimation(this.mContext, 17432818);
        this.mWorkLockController = new WorkLockActivityController(this.mContext);
    }

    public void start() {
        super.start();
        synchronized (this) {
            setupLocked();
        }
        putComponent(KeyguardViewMediator.class, this);
    }

    public void onSystemReady() {
        this.mHandler.obtainMessage(18).sendToTarget();
    }

    /* access modifiers changed from: private */
    public void handleSystemReady() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "onSystemReady");
            this.mSystemReady = true;
            doKeyguardLocked(null);
            this.mUpdateMonitor.registerCallback(this.mUpdateCallback);
            this.mUpdateMonitor.dispatchSystemReady();
            SarahClient.getInstance(this.mContext);
        }
        maybeSendUserPresentBroadcast();
        this.mUpdateMonitor.setUserUnlocked(UserManager.get(this.mContext).isUserUnlocked(KeyguardUpdateMonitor.getCurrentUser()));
    }

    /* JADX WARNING: Removed duplicated region for block: B:13:0x0083 A[Catch:{ RemoteException -> 0x009a }] */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0089 A[Catch:{ RemoteException -> 0x009a }] */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00e3 A[Catch:{ RemoteException -> 0x009a }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onStartedGoingToSleep(int r10) {
        /*
            r9 = this;
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "onStartedGoingToSleep("
            r0.append(r1)
            r0.append(r10)
            java.lang.String r1 = ")"
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "KeyguardViewMediator"
            android.util.Log.d(r1, r0)
            monitor-enter(r9)
            r0 = 0
            r9.mDeviceInteractive = r0     // Catch:{ all -> 0x00f4 }
            r1 = 1
            r9.mGoingToSleep = r1     // Catch:{ all -> 0x00f4 }
            int r2 = com.android.keyguard.KeyguardUpdateMonitor.getCurrentUser()     // Catch:{ all -> 0x00f4 }
            com.android.internal.widget.LockPatternUtils r3 = r9.mLockPatternUtils     // Catch:{ all -> 0x00f4 }
            boolean r3 = r3.getPowerButtonInstantlyLocks(r2)     // Catch:{ all -> 0x00f4 }
            if (r3 != 0) goto L_0x0039
            com.android.internal.widget.LockPatternUtils r3 = r9.mLockPatternUtils     // Catch:{ all -> 0x00f4 }
            boolean r3 = r3.isSecure(r2)     // Catch:{ all -> 0x00f4 }
            if (r3 != 0) goto L_0x0037
            goto L_0x0039
        L_0x0037:
            r3 = r0
            goto L_0x003a
        L_0x0039:
            r3 = r1
        L_0x003a:
            int r4 = com.android.keyguard.KeyguardUpdateMonitor.getCurrentUser()     // Catch:{ all -> 0x00f4 }
            long r4 = r9.getLockTimeout(r4)     // Catch:{ all -> 0x00f4 }
            r9.mLockLater = r0     // Catch:{ all -> 0x00f4 }
            com.android.keyguard.KeyguardUpdateMonitor r6 = r9.mUpdateMonitor     // Catch:{ all -> 0x00f4 }
            r6.setGoingToSleepReason(r10)     // Catch:{ all -> 0x00f4 }
            java.lang.String r6 = "KeyguardViewMediator"
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch:{ all -> 0x00f4 }
            r7.<init>()     // Catch:{ all -> 0x00f4 }
            java.lang.String r8 = "onStartedGoingToSleep: mShowing:"
            r7.append(r8)     // Catch:{ all -> 0x00f4 }
            boolean r8 = r9.mShowing     // Catch:{ all -> 0x00f4 }
            r7.append(r8)     // Catch:{ all -> 0x00f4 }
            java.lang.String r8 = ", isKeyguardDone:"
            r7.append(r8)     // Catch:{ all -> 0x00f4 }
            com.android.keyguard.KeyguardUpdateMonitor r8 = r9.mUpdateMonitor     // Catch:{ all -> 0x00f4 }
            boolean r8 = r8.isKeyguardDone()     // Catch:{ all -> 0x00f4 }
            r7.append(r8)     // Catch:{ all -> 0x00f4 }
            java.lang.String r8 = ", lockImmediately:"
            r7.append(r8)     // Catch:{ all -> 0x00f4 }
            r7.append(r3)     // Catch:{ all -> 0x00f4 }
            java.lang.String r8 = ", timeout:"
            r7.append(r8)     // Catch:{ all -> 0x00f4 }
            r7.append(r4)     // Catch:{ all -> 0x00f4 }
            java.lang.String r7 = r7.toString()     // Catch:{ all -> 0x00f4 }
            android.util.Log.d(r6, r7)     // Catch:{ all -> 0x00f4 }
            r6 = 10
            if (r10 != r6) goto L_0x0089
            com.android.keyguard.KeyguardUpdateMonitor r0 = r9.mUpdateMonitor     // Catch:{ all -> 0x00f4 }
            r0.notifyFakeLocking(r1)     // Catch:{ all -> 0x00f4 }
            goto L_0x00df
        L_0x0089:
            com.android.internal.policy.IKeyguardExitCallback r6 = r9.mExitSecureCallback     // Catch:{ all -> 0x00f4 }
            if (r6 == 0) goto L_0x00ad
            java.lang.String r2 = "KeyguardViewMediator"
            java.lang.String r3 = "pending exit secure callback cancelled"
            android.util.Log.d(r2, r3)     // Catch:{ all -> 0x00f4 }
            com.android.internal.policy.IKeyguardExitCallback r2 = r9.mExitSecureCallback     // Catch:{ RemoteException -> 0x009a }
            r2.onKeyguardExitResult(r0)     // Catch:{ RemoteException -> 0x009a }
            goto L_0x00a2
        L_0x009a:
            r0 = move-exception
            java.lang.String r2 = "KeyguardViewMediator"
            java.lang.String r3 = "Failed to call onKeyguardExitResult(false)"
            android.util.Slog.w(r2, r3, r0)     // Catch:{ all -> 0x00f4 }
        L_0x00a2:
            r0 = 0
            r9.mExitSecureCallback = r0     // Catch:{ all -> 0x00f4 }
            boolean r0 = r9.mExternallyEnabled     // Catch:{ all -> 0x00f4 }
            if (r0 != 0) goto L_0x00df
            r9.hideLocked()     // Catch:{ all -> 0x00f4 }
            goto L_0x00df
        L_0x00ad:
            boolean r0 = r9.mShowing     // Catch:{ all -> 0x00f4 }
            if (r0 == 0) goto L_0x00bc
            com.android.keyguard.KeyguardUpdateMonitor r0 = r9.mUpdateMonitor     // Catch:{ all -> 0x00f4 }
            boolean r0 = r0.isKeyguardDone()     // Catch:{ all -> 0x00f4 }
            if (r0 != 0) goto L_0x00bc
            r9.mPendingReset = r1     // Catch:{ all -> 0x00f4 }
            goto L_0x00df
        L_0x00bc:
            r0 = 3
            if (r10 != r0) goto L_0x00c5
            r6 = 0
            int r0 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1))
            if (r0 > 0) goto L_0x00ca
        L_0x00c5:
            r0 = 2
            if (r10 != r0) goto L_0x00d5
            if (r3 != 0) goto L_0x00d5
        L_0x00ca:
            r9.doKeyguardLaterLocked(r4)     // Catch:{ all -> 0x00f4 }
            r9.mLockLater = r1     // Catch:{ all -> 0x00f4 }
            com.android.keyguard.KeyguardUpdateMonitor r0 = r9.mUpdateMonitor     // Catch:{ all -> 0x00f4 }
            r0.notifyFakeLocking(r1)     // Catch:{ all -> 0x00f4 }
            goto L_0x00df
        L_0x00d5:
            com.android.internal.widget.LockPatternUtils r0 = r9.mLockPatternUtils     // Catch:{ all -> 0x00f4 }
            boolean r0 = r0.isLockScreenDisabled(r2)     // Catch:{ all -> 0x00f4 }
            if (r0 != 0) goto L_0x00df
            r9.mPendingLock = r1     // Catch:{ all -> 0x00f4 }
        L_0x00df:
            boolean r0 = r9.mPendingLock     // Catch:{ all -> 0x00f4 }
            if (r0 == 0) goto L_0x00e6
            r9.playSounds(r1)     // Catch:{ all -> 0x00f4 }
        L_0x00e6:
            monitor-exit(r9)     // Catch:{ all -> 0x00f4 }
            android.content.Context r0 = r9.mContext
            com.android.keyguard.KeyguardUpdateMonitor r0 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r0)
            r0.dispatchStartedGoingToSleep(r10)
            r9.notifyStartedGoingToSleep()
            return
        L_0x00f4:
            r10 = move-exception
            monitor-exit(r9)     // Catch:{ all -> 0x00f4 }
            throw r10
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.keyguard.KeyguardViewMediator.onStartedGoingToSleep(int):void");
    }

    public void onFinishedGoingToSleep(int i, boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("onFinishedGoingToSleep(");
        sb.append(i);
        sb.append(")");
        Log.d("KeyguardViewMediator", sb.toString());
        synchronized (this) {
            this.mDeviceInteractive = false;
            this.mGoingToSleep = false;
            this.mWakeAndUnlocking = false;
            resetKeyguardDonePendingLocked();
            this.mHideAnimationRun = false;
            notifyFinishedGoingToSleep();
            if (z) {
                Log.i("KeyguardViewMediator", "Camera gesture was triggered, preventing Keyguard locking.");
                ((PowerManager) this.mContext.getSystemService(PowerManager.class)).wakeUp(SystemClock.uptimeMillis(), 5, "com.android.systemui:CAMERA_GESTURE_PREVENT_LOCK");
                this.mPendingLock = false;
                this.mPendingReset = false;
            }
            if (this.mPendingReset) {
                resetStateLocked();
                this.mPendingReset = false;
            }
            if (this.mPendingLock) {
                doKeyguardLocked(null);
                this.mPendingLock = false;
            }
            if (!this.mLockLater && !z) {
                doKeyguardForChildProfilesLocked();
            }
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchFinishedGoingToSleep(i);
    }

    private long getLockTimeout(int i) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        long j = (long) Secure.getInt(contentResolver, "lock_screen_lock_after_timeout", 5000);
        long maximumTimeToLock = this.mLockPatternUtils.getDevicePolicyManager().getMaximumTimeToLock(null, i);
        return maximumTimeToLock <= 0 ? j : Math.max(Math.min(maximumTimeToLock - Math.max((long) System.getInt(contentResolver, "screen_off_timeout", 30000), 0), j), 0);
    }

    private void doKeyguardLaterLocked() {
        long lockTimeout = getLockTimeout(KeyguardUpdateMonitor.getCurrentUser());
        if (lockTimeout == 0) {
            doKeyguardLocked(null);
        } else {
            doKeyguardLaterLocked(lockTimeout);
        }
    }

    private void doKeyguardLaterLocked(long j) {
        long elapsedRealtime = SystemClock.elapsedRealtime() + j;
        Intent intent = new Intent("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD");
        intent.putExtra("seq", this.mDelayedShowingSequence);
        intent.addFlags(268435456);
        this.mAlarmManager.setExactAndAllowWhileIdle(2, elapsedRealtime, PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456));
        StringBuilder sb = new StringBuilder();
        sb.append("setting alarm to turn off keyguard, seq = ");
        sb.append(this.mDelayedShowingSequence);
        Log.d("KeyguardViewMediator", sb.toString());
        doKeyguardLaterForChildProfilesLocked();
    }

    private void doKeyguardLaterForChildProfilesLocked() {
        int[] enabledProfileIds;
        for (int i : UserManager.get(this.mContext).getEnabledProfileIds(UserHandle.myUserId())) {
            if (this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i)) {
                long lockTimeout = getLockTimeout(i);
                if (lockTimeout == 0) {
                    doKeyguardForChildProfilesLocked();
                } else {
                    long elapsedRealtime = SystemClock.elapsedRealtime() + lockTimeout;
                    Intent intent = new Intent("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK");
                    intent.putExtra("seq", this.mDelayedProfileShowingSequence);
                    intent.putExtra("android.intent.extra.USER_ID", i);
                    intent.addFlags(268435456);
                    this.mAlarmManager.setExactAndAllowWhileIdle(2, elapsedRealtime, PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456));
                }
            }
        }
    }

    private void doKeyguardForChildProfilesLocked() {
        int[] enabledProfileIds;
        for (int i : UserManager.get(this.mContext).getEnabledProfileIds(UserHandle.myUserId())) {
            if (this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i)) {
                lockProfile(i);
            }
        }
    }

    private void cancelDoKeyguardLaterLocked() {
        this.mDelayedShowingSequence++;
    }

    private void cancelDoKeyguardForChildProfilesLocked() {
        this.mDelayedProfileShowingSequence++;
    }

    public void onStartedWakingUp() {
        Trace.beginSection("KeyguardViewMediator#onStartedWakingUp");
        if (SystemProperties.getInt("sys.debug.systemui.mes", 0) != 0) {
            this.mHandler.getLooper().setMessageLogging(new LogPrinter(3, "SystemUI"));
        } else {
            this.mHandler.getLooper().setMessageLogging(null);
        }
        this.mUpdateMonitor.setGoingToSleepReason(-1);
        this.mUpdateMonitor.notifyFakeLocking(false);
        synchronized (this) {
            this.mDeviceInteractive = true;
            cancelDoKeyguardLaterLocked();
            cancelDoKeyguardForChildProfilesLocked();
            StringBuilder sb = new StringBuilder();
            sb.append("onStartedWakingUp, seq = ");
            sb.append(this.mDelayedShowingSequence);
            Log.d("KeyguardViewMediator", sb.toString());
            notifyStartedWakingUp();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchStartedWakingUp();
        StatusBar statusBar = this.mStatusBar;
        if (!(statusBar == null || statusBar.getFacelockController() == null)) {
            this.mStatusBar.getFacelockController().onPreStartedWakingUp();
        }
        maybeSendUserPresentBroadcast();
        Trace.endSection();
    }

    public void onScreenTurningOn(IKeyguardDrawnCallback iKeyguardDrawnCallback) {
        Trace.beginSection("KeyguardViewMediator#onScreenTurningOn");
        notifyScreenOn(iKeyguardDrawnCallback);
        Trace.endSection();
    }

    public void onScreenTurnedOn() {
        Trace.beginSection("KeyguardViewMediator#onScreenTurnedOn");
        notifyScreenTurnedOn();
        this.mUpdateMonitor.dispatchScreenTurnedOn();
        Trace.endSection();
    }

    public void onScreenTurnedOff() {
        notifyScreenTurnedOff();
        this.mUpdateMonitor.dispatchScreenTurnedOff();
    }

    private void maybeSendUserPresentBroadcast() {
        if (this.mSystemReady && this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser())) {
            sendUserPresentBroadcast();
        } else if (this.mSystemReady && shouldWaitForProvisioning()) {
            getLockPatternUtils().userPresent(KeyguardUpdateMonitor.getCurrentUser());
        }
    }

    public void onDreamingStarted() {
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchDreamingStarted();
        synchronized (this) {
            if (this.mDeviceInteractive && this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser())) {
                doKeyguardLaterLocked();
            }
        }
    }

    public void onDreamingStopped() {
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchDreamingStopped();
        synchronized (this) {
            if (this.mDeviceInteractive) {
                cancelDoKeyguardLaterLocked();
            }
        }
    }

    /* JADX WARNING: Can't wrap try/catch for region: R(7:29|30|31|32|42|39|27) */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x00ad, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x0095, code lost:
        continue;
     */
    /* JADX WARNING: Missing exception handler attribute for start block: B:31:0x009d */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setKeyguardEnabled(boolean r4) {
        /*
            r3 = this;
            monitor-enter(r3)
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x00ae }
            r1.<init>()     // Catch:{ all -> 0x00ae }
            java.lang.String r2 = "setKeyguardEnabled("
            r1.append(r2)     // Catch:{ all -> 0x00ae }
            r1.append(r4)     // Catch:{ all -> 0x00ae }
            java.lang.String r2 = ")"
            r1.append(r2)     // Catch:{ all -> 0x00ae }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x00ae }
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x00ae }
            r3.mExternallyEnabled = r4     // Catch:{ all -> 0x00ae }
            r0 = 1
            if (r4 != 0) goto L_0x0042
            boolean r1 = r3.mShowing     // Catch:{ all -> 0x00ae }
            if (r1 == 0) goto L_0x0042
            com.android.internal.policy.IKeyguardExitCallback r4 = r3.mExitSecureCallback     // Catch:{ all -> 0x00ae }
            if (r4 == 0) goto L_0x0032
            java.lang.String r4 = "KeyguardViewMediator"
            java.lang.String r0 = "in process of verifyUnlock request, ignoring"
            android.util.Log.d(r4, r0)     // Catch:{ all -> 0x00ae }
            monitor-exit(r3)     // Catch:{ all -> 0x00ae }
            return
        L_0x0032:
            java.lang.String r4 = "KeyguardViewMediator"
            java.lang.String r1 = "remembering to reshow, hiding keyguard, disabling status bar expansion"
            android.util.Log.d(r4, r1)     // Catch:{ all -> 0x00ae }
            r3.mNeedToReshowWhenReenabled = r0     // Catch:{ all -> 0x00ae }
            r3.updateInputRestrictedLocked()     // Catch:{ all -> 0x00ae }
            r3.hideLocked()     // Catch:{ all -> 0x00ae }
            goto L_0x00ac
        L_0x0042:
            if (r4 == 0) goto L_0x00ac
            boolean r4 = r3.mNeedToReshowWhenReenabled     // Catch:{ all -> 0x00ae }
            if (r4 == 0) goto L_0x00ac
            java.lang.String r4 = "KeyguardViewMediator"
            java.lang.String r1 = "previously hidden, reshowing, reenabling status bar expansion"
            android.util.Log.d(r4, r1)     // Catch:{ all -> 0x00ae }
            r4 = 0
            r3.mNeedToReshowWhenReenabled = r4     // Catch:{ all -> 0x00ae }
            r3.updateInputRestrictedLocked()     // Catch:{ all -> 0x00ae }
            com.android.internal.policy.IKeyguardExitCallback r1 = r3.mExitSecureCallback     // Catch:{ all -> 0x00ae }
            r2 = 0
            if (r1 == 0) goto L_0x0075
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.String r1 = "onKeyguardExitResult(false), resetting"
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x00ae }
            com.android.internal.policy.IKeyguardExitCallback r0 = r3.mExitSecureCallback     // Catch:{ RemoteException -> 0x0067 }
            r0.onKeyguardExitResult(r4)     // Catch:{ RemoteException -> 0x0067 }
            goto L_0x006f
        L_0x0067:
            r4 = move-exception
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.String r1 = "Failed to call onKeyguardExitResult(false)"
            android.util.Slog.w(r0, r1, r4)     // Catch:{ all -> 0x00ae }
        L_0x006f:
            r3.mExitSecureCallback = r2     // Catch:{ all -> 0x00ae }
            r3.resetStateLocked()     // Catch:{ all -> 0x00ae }
            goto L_0x00ac
        L_0x0075:
            com.android.keyguard.KeyguardUpdateMonitor r1 = r3.mUpdateMonitor     // Catch:{ all -> 0x00ae }
            r1.notifyKeyguardDone(r4)     // Catch:{ all -> 0x00ae }
            com.android.keyguard.KeyguardUpdateMonitor r1 = r3.mUpdateMonitor     // Catch:{ all -> 0x00ae }
            r1.notifyFakeLocking(r4)     // Catch:{ all -> 0x00ae }
            r3.showLocked(r2)     // Catch:{ all -> 0x00ae }
            r3.mWaitingUntilKeyguardVisible = r0     // Catch:{ all -> 0x00ae }
            com.oneplus.systemui.keyguard.OpKeyguardViewMediator$OpHandler r4 = r3.mHandler     // Catch:{ all -> 0x00ae }
            r0 = 8
            r1 = 2000(0x7d0, double:9.88E-321)
            r4.sendEmptyMessageDelayed(r0, r1)     // Catch:{ all -> 0x00ae }
            java.lang.String r4 = "KeyguardViewMediator"
            java.lang.String r0 = "waiting until mWaitingUntilKeyguardVisible is false"
            android.util.Log.d(r4, r0)     // Catch:{ all -> 0x00ae }
        L_0x0095:
            boolean r4 = r3.mWaitingUntilKeyguardVisible     // Catch:{ all -> 0x00ae }
            if (r4 == 0) goto L_0x00a5
            r3.wait()     // Catch:{ InterruptedException -> 0x009d }
            goto L_0x0095
        L_0x009d:
            java.lang.Thread r4 = java.lang.Thread.currentThread()     // Catch:{ all -> 0x00ae }
            r4.interrupt()     // Catch:{ all -> 0x00ae }
            goto L_0x0095
        L_0x00a5:
            java.lang.String r4 = "KeyguardViewMediator"
            java.lang.String r0 = "done waiting for mWaitingUntilKeyguardVisible"
            android.util.Log.d(r4, r0)     // Catch:{ all -> 0x00ae }
        L_0x00ac:
            monitor-exit(r3)     // Catch:{ all -> 0x00ae }
            return
        L_0x00ae:
            r4 = move-exception
            monitor-exit(r3)     // Catch:{ all -> 0x00ae }
            throw r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.keyguard.KeyguardViewMediator.setKeyguardEnabled(boolean):void");
    }

    public void verifyUnlock(IKeyguardExitCallback iKeyguardExitCallback) {
        Trace.beginSection("KeyguardViewMediator#verifyUnlock");
        synchronized (this) {
            Log.d("KeyguardViewMediator", "verifyUnlock");
            if (shouldWaitForProvisioning()) {
                Log.d("KeyguardViewMediator", "ignoring because device isn't provisioned");
                try {
                    iKeyguardExitCallback.onKeyguardExitResult(false);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e);
                }
            } else if (this.mExternallyEnabled) {
                Log.w("KeyguardViewMediator", "verifyUnlock called when not externally disabled");
                try {
                    iKeyguardExitCallback.onKeyguardExitResult(false);
                } catch (RemoteException e2) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e2);
                }
            } else if (this.mExitSecureCallback != null) {
                try {
                    iKeyguardExitCallback.onKeyguardExitResult(false);
                } catch (RemoteException e3) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e3);
                }
            } else if (!isSecure()) {
                this.mExternallyEnabled = true;
                this.mNeedToReshowWhenReenabled = false;
                updateInputRestricted();
                try {
                    iKeyguardExitCallback.onKeyguardExitResult(true);
                } catch (RemoteException e4) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e4);
                }
            } else {
                try {
                    iKeyguardExitCallback.onKeyguardExitResult(false);
                } catch (RemoteException e5) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e5);
                }
            }
        }
        Trace.endSection();
    }

    public boolean isShowingAndNotOccluded() {
        return this.mShowing && !this.mOccluded;
    }

    public void setOccluded(boolean z, boolean z2) {
        Trace.beginSection("KeyguardViewMediator#setOccluded");
        StringBuilder sb = new StringBuilder();
        sb.append("setOccluded ");
        sb.append(z);
        Log.d("KeyguardViewMediator", sb.toString());
        this.mHandler.removeMessages(9);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9, z ? 1 : 0, z2 ? 1 : 0));
        Trace.endSection();
    }

    public boolean isHiding() {
        return this.mHiding;
    }

    /* access modifiers changed from: private */
    public void handleSetOccluded(boolean z, boolean z2) {
        boolean z3;
        Trace.beginSection("KeyguardViewMediator#handleSetOccluded");
        synchronized (this) {
            if (this.mHiding && z) {
                startKeyguardExitAnimation(0, 0);
            }
            if (this.mOccluded != z) {
                this.mOccluded = z;
                this.mUpdateMonitor.setKeyguardOccluded(z);
                StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
                if (!KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure()) {
                    if (z2 && this.mDeviceInteractive) {
                        z3 = true;
                        statusBarKeyguardViewManager.setOccluded(z, z3);
                        adjustStatusBarLocked();
                    }
                }
                z3 = false;
                statusBarKeyguardViewManager.setOccluded(z, z3);
                adjustStatusBarLocked();
            }
        }
        Trace.endSection();
    }

    public void doKeyguardTimeout(Bundle bundle) {
        this.mHandler.removeMessages(10);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(10, bundle));
    }

    public boolean isInputRestricted() {
        return this.mShowing || this.mNeedToReshowWhenReenabled;
    }

    private void updateInputRestricted() {
        synchronized (this) {
            updateInputRestrictedLocked();
        }
    }

    private void updateInputRestrictedLocked() {
        boolean isInputRestricted = isInputRestricted();
        if (this.mInputRestricted != isInputRestricted) {
            this.mInputRestricted = isInputRestricted;
            for (int size = this.mKeyguardStateCallbacks.size() - 1; size >= 0; size--) {
                IKeyguardStateCallback iKeyguardStateCallback = (IKeyguardStateCallback) this.mKeyguardStateCallbacks.get(size);
                try {
                    iKeyguardStateCallback.onInputRestrictedStateChanged(isInputRestricted);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onDeviceProvisioned", e);
                    if (e instanceof DeadObjectException) {
                        this.mKeyguardStateCallbacks.remove(iKeyguardStateCallback);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void doKeyguardLocked(Bundle bundle) {
        String str = "KeyguardViewMediator";
        if (KeyguardUpdateMonitor.CORE_APPS_ONLY) {
            Log.d(str, "doKeyguard: not showing because booting to cryptkeeper");
            return;
        }
        boolean z = true;
        if (!this.mExternallyEnabled) {
            Log.d(str, "doKeyguard: not showing because externally disabled");
            this.mNeedToReshowWhenReenabled = true;
            this.mUpdateMonitor.notifyFakeLocking(true);
        } else if (this.mStatusBarKeyguardViewManager.isShowing()) {
            Log.d(str, "doKeyguard: not showing because it is already showing");
            resetStateLocked();
            this.mUpdateMonitor.notifyKeyguardDone(false);
        } else {
            if (!mustNotUnlockCurrentUser() || !this.mUpdateMonitor.isDeviceProvisioned()) {
                boolean z2 = this.mUpdateMonitor.isSimPinSecure() || ((SubscriptionManager.isValidSubscriptionId(this.mUpdateMonitor.getNextSubIdForState(State.ABSENT)) || SubscriptionManager.isValidSubscriptionId(this.mUpdateMonitor.getNextSubIdForState(State.PERM_DISABLED))) && (SystemProperties.getBoolean("keyguard.no_require_sim", false) ^ true));
                if (z2 || !shouldWaitForProvisioning()) {
                    if (bundle == null || !bundle.getBoolean("force_show", false)) {
                        z = false;
                    }
                    if (this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser()) && !z2 && !z) {
                        Log.d(str, "doKeyguard: not showing because lockscreen is off");
                        return;
                    } else if (this.mLockPatternUtils.checkVoldPassword(KeyguardUpdateMonitor.getCurrentUser())) {
                        Log.d(str, "Not showing lock screen since just decrypted");
                        setShowingLocked(false, this.mAodShowing);
                        hideLocked();
                        return;
                    }
                } else {
                    Log.d(str, "doKeyguard: not showing because device isn't provisioned and the sim is not locked or missing");
                    return;
                }
            }
            try {
                ActivityTaskManager.getService().setKeyguardDone(false);
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Exception e = ");
                sb.append(e.toString());
                Log.w(str, sb.toString());
            }
            this.mUpdateMonitor.notifyKeyguardDone(false);
            Log.d(str, "doKeyguard: showing the lock screen");
            this.mUpdateMonitor.notifyFakeLocking(false);
            showLocked(bundle);
        }
    }

    /* access modifiers changed from: private */
    public void lockProfile(int i) {
        this.mTrustManager.setDeviceLockedForUser(i, true);
    }

    /* access modifiers changed from: private */
    public boolean shouldWaitForProvisioning() {
        return !this.mUpdateMonitor.isDeviceProvisioned() && !isSecure();
    }

    /* access modifiers changed from: private */
    public void handleDismiss(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) {
        if (this.mShowing) {
            if (iKeyguardDismissCallback != null) {
                this.mDismissCallbackRegistry.addCallback(iKeyguardDismissCallback);
            }
            this.mCustomMessage = charSequence;
            this.mStatusBarKeyguardViewManager.dismissAndCollapse();
        } else if (iKeyguardDismissCallback != null) {
            new DismissCallbackWrapper(iKeyguardDismissCallback).notifyDismissError();
        }
    }

    public void dismiss(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) {
        this.mHandler.obtainMessage(11, new DismissMessage(iKeyguardDismissCallback, charSequence)).sendToTarget();
    }

    /* access modifiers changed from: private */
    public void resetStateLocked() {
        Log.e("KeyguardViewMediator", "resetStateLocked");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3));
    }

    private void notifyStartedGoingToSleep() {
        Log.d("KeyguardViewMediator", "notifyStartedGoingToSleep");
        this.mHandler.sendEmptyMessage(17);
    }

    private void notifyFinishedGoingToSleep() {
        Log.d("KeyguardViewMediator", "notifyFinishedGoingToSleep");
        this.mHandler.sendEmptyMessage(5);
    }

    private void notifyStartedWakingUp() {
        Log.d("KeyguardViewMediator", "notifyStartedWakingUp");
        this.mHandler.sendEmptyMessage(14);
    }

    private void notifyScreenOn(IKeyguardDrawnCallback iKeyguardDrawnCallback) {
        Log.d("KeyguardViewMediator", "notifyScreenOn");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(6, iKeyguardDrawnCallback));
    }

    private void notifyScreenTurnedOn() {
        Log.d("KeyguardViewMediator", "notifyScreenTurnedOn");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(15));
    }

    private void notifyScreenTurnedOff() {
        Log.d("KeyguardViewMediator", "notifyScreenTurnedOff");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(16));
    }

    private void showLocked(Bundle bundle) {
        Trace.beginSection("KeyguardViewMediator#showLocked aqcuiring mShowKeyguardWakeLock");
        Log.d("KeyguardViewMediator", "showLocked");
        this.mIgnoreHandleShow = false;
        this.mShowKeyguardWakeLock.acquire();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, bundle));
        Trace.endSection();
    }

    private void hideLocked() {
        Trace.beginSection("KeyguardViewMediator#hideLocked");
        Log.d("KeyguardViewMediator", "hideLocked");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
        Trace.endSection();
    }

    public boolean isSecure() {
        return isSecure(KeyguardUpdateMonitor.getCurrentUser());
    }

    public boolean isSecure(int i) {
        return this.mLockPatternUtils.isSecure(i) || KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure();
    }

    public void setSwitchingUser(boolean z) {
        KeyguardUpdateMonitor.getInstance(this.mContext).setSwitchingUser(z);
    }

    public void setCurrentUser(int i) {
        KeyguardUpdateMonitor.setCurrentUser(i);
        synchronized (this) {
            notifyTrustedChangedLocked(this.mUpdateMonitor.getUserHasTrust(i));
        }
    }

    public void keyguardDone() {
        Trace.beginSection("KeyguardViewMediator#keyguardDone");
        Log.d("KeyguardViewMediator", "keyguardDone()");
        userActivity();
        EventLog.writeEvent(70000, 2);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(7));
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void tryKeyguardDone() {
        if (!this.mKeyguardDonePending && this.mHideAnimationRun && !this.mHideAnimationRunning) {
            handleKeyguardDone();
        } else if (!this.mHideAnimationRun) {
            this.mHideAnimationRun = true;
            this.mHideAnimationRunning = true;
            this.mStatusBarKeyguardViewManager.startPreHideAnimation(this.mHideAnimationFinishedRunnable);
        }
    }

    /* access modifiers changed from: private */
    public void handleKeyguardDone() {
        Trace.beginSection("KeyguardViewMediator#handleKeyguardDone");
        this.mUiOffloadThread.submit(new Runnable(KeyguardUpdateMonitor.getCurrentUser()) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                KeyguardViewMediator.this.lambda$handleKeyguardDone$0$KeyguardViewMediator(this.f$1);
            }
        });
        Log.d("KeyguardViewMediator", "handleKeyguardDone");
        synchronized (this) {
            resetKeyguardDonePendingLocked();
        }
        int i = 0;
        this.mUpdateMonitor.clearFailedUnlockAttempts(false);
        this.mUpdateMonitor.clearBiometricRecognized();
        if (this.mGoingToSleep) {
            Log.i("KeyguardViewMediator", "Device is going to sleep, aborting keyguardDone");
            BiometricUnlockController biometricUnlockController = OpLsState.getInstance().getBiometricUnlockController();
            if (biometricUnlockController != null) {
                i = biometricUnlockController.getMode();
            }
            if (!(biometricUnlockController == null || i == 1 || i == 6 || i == 5)) {
                biometricUnlockController.opResetMode();
            }
            return;
        }
        IKeyguardExitCallback iKeyguardExitCallback = this.mExitSecureCallback;
        if (iKeyguardExitCallback != null) {
            try {
                iKeyguardExitCallback.onKeyguardExitResult(true);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult()", e);
            }
            this.mExitSecureCallback = null;
            this.mExternallyEnabled = true;
            this.mNeedToReshowWhenReenabled = false;
            updateInputRestricted();
        }
        try {
            ActivityTaskManager.getService().setKeyguardDone(true);
        } catch (Exception e2) {
            StringBuilder sb = new StringBuilder();
            sb.append("Exception e = ");
            sb.append(e2.toString());
            Log.w("KeyguardViewMediator", sb.toString());
        }
        this.mUpdateMonitor.notifyKeyguardDone(true);
        this.mUpdateMonitor.setUserUnlocked(true);
        handleHide();
        Trace.endSection();
    }

    public /* synthetic */ void lambda$handleKeyguardDone$0$KeyguardViewMediator(int i) {
        if (this.mLockPatternUtils.isSecure(i)) {
            this.mLockPatternUtils.userPresent(i);
            this.mLockPatternUtils.getDevicePolicyManager().reportKeyguardDismissed(i);
        }
    }

    /* access modifiers changed from: private */
    public void sendUserPresentBroadcast() {
        synchronized (this) {
            if (this.mBootCompleted) {
                int currentUser = KeyguardUpdateMonitor.getCurrentUser();
                UserManager userManager = (UserManager) this.mContext.getSystemService("user");
                this.mUiOffloadThread.submit(new Runnable(userManager, new UserHandle(currentUser), currentUser) {
                    private final /* synthetic */ UserManager f$1;
                    private final /* synthetic */ UserHandle f$2;
                    private final /* synthetic */ int f$3;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                    }

                    public final void run() {
                        KeyguardViewMediator.this.lambda$sendUserPresentBroadcast$1$KeyguardViewMediator(this.f$1, this.f$2, this.f$3);
                    }
                });
            } else {
                this.mBootSendUserPresent = true;
            }
        }
    }

    public /* synthetic */ void lambda$sendUserPresentBroadcast$1$KeyguardViewMediator(UserManager userManager, UserHandle userHandle, int i) {
        for (int of : userManager.getProfileIdsWithDisabled(userHandle.getIdentifier())) {
            this.mContext.sendBroadcastAsUser(USER_PRESENT_INTENT, UserHandle.of(of));
        }
        getLockPatternUtils().userPresent(i);
    }

    /* access modifiers changed from: private */
    public void handleKeyguardDoneDrawing() {
        Trace.beginSection("KeyguardViewMediator#handleKeyguardDoneDrawing");
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleKeyguardDoneDrawing");
            if (this.mWaitingUntilKeyguardVisible) {
                Log.d("KeyguardViewMediator", "handleKeyguardDoneDrawing: notifying mWaitingUntilKeyguardVisible");
                this.mWaitingUntilKeyguardVisible = false;
                notifyAll();
                this.mHandler.removeMessages(8);
            }
        }
        Trace.endSection();
    }

    private void playSounds(boolean z) {
        playSound(z ? this.mLockSoundId : this.mUnlockSoundId);
    }

    private void playSound(int i) {
        if (i != 0 && System.getIntForUser(this.mContext.getContentResolver(), "lockscreen_sounds_enabled", 1, -2) == 1) {
            this.mLockSounds.stop(this.mLockSoundStreamId);
            if (this.mAudioManager == null) {
                this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
                AudioManager audioManager = this.mAudioManager;
                if (audioManager != null) {
                    this.mUiSoundsStreamType = audioManager.getUiSoundsStreamType();
                } else {
                    return;
                }
            }
            this.mUiOffloadThread.submit(new Runnable(i) {
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    KeyguardViewMediator.this.lambda$playSound$2$KeyguardViewMediator(this.f$1);
                }
            });
        }
    }

    public /* synthetic */ void lambda$playSound$2$KeyguardViewMediator(int i) {
        if (!this.mAudioManager.isStreamMute(this.mUiSoundsStreamType)) {
            SoundPool soundPool = this.mLockSounds;
            float f = this.mLockSoundVolume;
            int play = soundPool.play(i, f, f, 1, 0, 1.0f);
            StringBuilder sb = new StringBuilder();
            sb.append("play lock soundId: ");
            sb.append(i);
            sb.append(", volume:");
            sb.append(this.mLockSoundVolume);
            sb.append(", ");
            sb.append(this.mLockSounds);
            sb.append(", type:");
            sb.append(this.mUiSoundsStreamType);
            sb.append(", ");
            sb.append(play);
            Log.d("KeyguardViewMediator", sb.toString());
            synchronized (this) {
                this.mLockSoundStreamId = play;
            }
        }
    }

    /* access modifiers changed from: private */
    public void playTrustedSound() {
        playSound(this.mTrustedSoundId);
    }

    private void updateActivityLockScreenState(boolean z, boolean z2) {
        this.mUiOffloadThread.submit(new Runnable(z, z2) {
            private final /* synthetic */ boolean f$0;
            private final /* synthetic */ boolean f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            public final void run() {
                KeyguardViewMediator.lambda$updateActivityLockScreenState$3(this.f$0, this.f$1);
            }
        });
    }

    static /* synthetic */ void lambda$updateActivityLockScreenState$3(boolean z, boolean z2) {
        String str = "KeyguardViewMediator";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("setLockScreenShown, ");
            sb.append(z);
            sb.append(", ");
            sb.append(z2);
            Log.d(str, sb.toString());
            ActivityTaskManager.getService().setLockScreenShown(z, z2);
        } catch (RemoteException unused) {
        }
    }

    /* access modifiers changed from: private */
    public void handleShow(Bundle bundle) {
        Trace.beginSection("KeyguardViewMediator#handleShow");
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        if (this.mLockPatternUtils.isSecure(currentUser)) {
            this.mLockPatternUtils.getDevicePolicyManager().reportKeyguardSecured(currentUser);
        }
        synchronized (this) {
            if (!this.mSystemReady) {
                Log.d("KeyguardViewMediator", "ignoring handleShow because system is not ready.");
                try {
                    ActivityTaskManager.getService().setKeyguardDone(true);
                } catch (Exception e) {
                    String str = "KeyguardViewMediator";
                    StringBuilder sb = new StringBuilder();
                    sb.append("Exception e = ");
                    sb.append(e.toString());
                    Log.w(str, sb.toString());
                }
            } else {
                Log.d("KeyguardViewMediator", "handleShow");
                setShowingLocked(true, this.mAodShowing);
                this.mStatusBarKeyguardViewManager.show(bundle);
                ((NotificationMediaManager) Dependency.get(NotificationMediaManager.class)).updateMediaMetaData(false, true);
                this.mHiding = false;
                this.mWakeAndUnlocking = false;
                this.mUpdateMonitor.onFacelockUnlocking(false);
                resetKeyguardDonePendingLocked();
                this.mHideAnimationRun = false;
                adjustStatusBarLocked();
                userActivity();
                this.mUpdateMonitor.resetFingerprintAlreadyAuthenticated();
                this.mUpdateMonitor.setKeyguardGoingAway(false);
                this.mShowKeyguardWakeLock.release();
                this.mKeyguardDisplayManager.show();
                Trace.endSection();
            }
        }
    }

    public /* synthetic */ void lambda$new$4$KeyguardViewMediator() {
        this.mHideAnimationRunning = false;
        tryKeyguardDone();
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0058, code lost:
        android.os.Trace.endSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x005b, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleHide() {
        /*
            r5 = this;
            java.lang.String r0 = "KeyguardViewMediator#handleHide"
            android.os.Trace.beginSection(r0)
            boolean r0 = r5.mAodShowing
            if (r0 == 0) goto L_0x001d
            android.content.Context r0 = r5.mContext
            java.lang.Class<android.os.PowerManager> r1 = android.os.PowerManager.class
            java.lang.Object r0 = r0.getSystemService(r1)
            android.os.PowerManager r0 = (android.os.PowerManager) r0
            long r1 = android.os.SystemClock.uptimeMillis()
            r3 = 4
            java.lang.String r4 = "com.android.systemui:BOUNCER_DOZING"
            r0.wakeUp(r1, r3, r4)
        L_0x001d:
            monitor-enter(r5)
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.String r1 = "handleHide"
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x005c }
            boolean r0 = r5.mustNotUnlockCurrentUser()     // Catch:{ all -> 0x005c }
            if (r0 == 0) goto L_0x0034
            java.lang.String r0 = "KeyguardViewMediator"
            java.lang.String r1 = "Split system user, quit unlocking."
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x005c }
            monitor-exit(r5)     // Catch:{ all -> 0x005c }
            return
        L_0x0034:
            r0 = 1
            r5.mHiding = r0     // Catch:{ all -> 0x005c }
            boolean r0 = r5.mShowing     // Catch:{ all -> 0x005c }
            if (r0 == 0) goto L_0x0043
            boolean r0 = r5.mOccluded     // Catch:{ all -> 0x005c }
            if (r0 != 0) goto L_0x0043
            r5.opHandleHide()     // Catch:{ all -> 0x005c }
            goto L_0x0057
        L_0x0043:
            long r0 = android.os.SystemClock.uptimeMillis()     // Catch:{ all -> 0x005c }
            android.view.animation.Animation r2 = r5.mHideAnimation     // Catch:{ all -> 0x005c }
            long r2 = r2.getStartOffset()     // Catch:{ all -> 0x005c }
            long r0 = r0 + r2
            android.view.animation.Animation r2 = r5.mHideAnimation     // Catch:{ all -> 0x005c }
            long r2 = r2.getDuration()     // Catch:{ all -> 0x005c }
            r5.handleStartKeyguardExitAnimation(r0, r2)     // Catch:{ all -> 0x005c }
        L_0x0057:
            monitor-exit(r5)     // Catch:{ all -> 0x005c }
            android.os.Trace.endSection()
            return
        L_0x005c:
            r0 = move-exception
            monitor-exit(r5)     // Catch:{ all -> 0x005c }
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.keyguard.KeyguardViewMediator.handleHide():void");
    }

    public void reportBiometricUnlocked(int i, int i2) {
        getLockPatternUtils().reportBiometricUnlocked(0, i2);
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x008e, code lost:
        if (com.android.keyguard.KeyguardUpdateMonitor.getInstance(r4.mContext).isSimPinSecure() == false) goto L_0x009a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0090, code lost:
        android.util.Log.d("KeyguardViewMediator", "doKeyguard again when sim pin is still locked");
        doKeyguardLocked(null);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x009a, code lost:
        android.os.Trace.endSection();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x009d, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleStartKeyguardExitAnimation(long r5, long r7) {
        /*
            r4 = this;
            java.lang.String r0 = "KeyguardViewMediator#handleStartKeyguardExitAnimation"
            android.os.Trace.beginSection(r0)
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "handleStartKeyguardExitAnimation startTime="
            r0.append(r1)
            r0.append(r5)
            java.lang.String r1 = " fadeoutDuration="
            r0.append(r1)
            r0.append(r7)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "KeyguardViewMediator"
            android.util.Log.d(r1, r0)
            r0 = 70000(0x11170, float:9.8091E-41)
            r1 = 2
            android.util.EventLog.writeEvent(r0, r1)
            monitor-enter(r4)
            boolean r0 = r4.mHiding     // Catch:{ all -> 0x009e }
            if (r0 != 0) goto L_0x0039
            boolean r5 = r4.mShowing     // Catch:{ all -> 0x009e }
            boolean r6 = r4.mAodShowing     // Catch:{ all -> 0x009e }
            r7 = 1
            r4.setShowingLocked(r5, r6, r7)     // Catch:{ all -> 0x009e }
            monitor-exit(r4)     // Catch:{ all -> 0x009e }
            return
        L_0x0039:
            r0 = 0
            r4.mHiding = r0     // Catch:{ all -> 0x009e }
            boolean r1 = r4.mWakeAndUnlocking     // Catch:{ all -> 0x009e }
            r2 = 0
            if (r1 == 0) goto L_0x0055
            com.android.internal.policy.IKeyguardDrawnCallback r1 = r4.mDrawnCallback     // Catch:{ all -> 0x009e }
            if (r1 == 0) goto L_0x0055
            com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager r1 = r4.mStatusBarKeyguardViewManager     // Catch:{ all -> 0x009e }
            android.view.ViewRootImpl r1 = r1.getViewRootImpl()     // Catch:{ all -> 0x009e }
            r1.setReportNextDraw()     // Catch:{ all -> 0x009e }
            com.android.internal.policy.IKeyguardDrawnCallback r1 = r4.mDrawnCallback     // Catch:{ all -> 0x009e }
            r4.notifyDrawn(r1)     // Catch:{ all -> 0x009e }
            r4.mDrawnCallback = r2     // Catch:{ all -> 0x009e }
        L_0x0055:
            java.lang.String r1 = android.telephony.TelephonyManager.EXTRA_STATE_IDLE     // Catch:{ all -> 0x009e }
            java.lang.String r3 = r4.mPhoneState     // Catch:{ all -> 0x009e }
            boolean r1 = r1.equals(r3)     // Catch:{ all -> 0x009e }
            if (r1 == 0) goto L_0x0062
            r4.playSounds(r0)     // Catch:{ all -> 0x009e }
        L_0x0062:
            r4.mWakeAndUnlocking = r0     // Catch:{ all -> 0x009e }
            boolean r1 = r4.mAodShowing     // Catch:{ all -> 0x009e }
            r4.setShowingLocked(r0, r1)     // Catch:{ all -> 0x009e }
            com.android.systemui.keyguard.DismissCallbackRegistry r1 = r4.mDismissCallbackRegistry     // Catch:{ all -> 0x009e }
            r1.notifyDismissSucceeded()     // Catch:{ all -> 0x009e }
            com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager r1 = r4.mStatusBarKeyguardViewManager     // Catch:{ all -> 0x009e }
            r1.hide(r5, r7)     // Catch:{ all -> 0x009e }
            com.android.keyguard.KeyguardUpdateMonitor r5 = r4.mUpdateMonitor     // Catch:{ all -> 0x009e }
            r5.onFacelockUnlocking(r0)     // Catch:{ all -> 0x009e }
            r4.resetKeyguardDonePendingLocked()     // Catch:{ all -> 0x009e }
            r4.mHideAnimationRun = r0     // Catch:{ all -> 0x009e }
            r4.adjustStatusBarLocked()     // Catch:{ all -> 0x009e }
            r4.sendUserPresentBroadcast()     // Catch:{ all -> 0x009e }
            monitor-exit(r4)     // Catch:{ all -> 0x009e }
            android.content.Context r5 = r4.mContext
            com.android.keyguard.KeyguardUpdateMonitor r5 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r5)
            boolean r5 = r5.isSimPinSecure()
            if (r5 == 0) goto L_0x009a
            java.lang.String r5 = "KeyguardViewMediator"
            java.lang.String r6 = "doKeyguard again when sim pin is still locked"
            android.util.Log.d(r5, r6)
            r4.doKeyguardLocked(r2)
        L_0x009a:
            android.os.Trace.endSection()
            return
        L_0x009e:
            r5 = move-exception
            monitor-exit(r4)     // Catch:{ all -> 0x009e }
            throw r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.keyguard.KeyguardViewMediator.handleStartKeyguardExitAnimation(long, long):void");
    }

    /* access modifiers changed from: private */
    public void adjustStatusBarLocked() {
        adjustStatusBarLocked(false);
    }

    /* access modifiers changed from: private */
    public void adjustStatusBarLocked(boolean z) {
        if (this.mStatusBarManager == null) {
            this.mStatusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
        }
        String str = "KeyguardViewMediator";
        if (this.mStatusBarManager == null) {
            Log.w(str, "Could not get status bar manager");
            return;
        }
        int i = 0;
        if (z || isShowingAndNotOccluded()) {
            i = 18874368;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("adjustStatusBarLocked: mShowing=");
        sb.append(this.mShowing);
        sb.append(" mOccluded=");
        sb.append(this.mOccluded);
        sb.append(" isSecure=");
        sb.append(isSecure());
        sb.append(" force=");
        sb.append(z);
        sb.append(" --> flags=0x");
        sb.append(Integer.toHexString(i));
        Log.d(str, sb.toString());
        this.mStatusBarManager.disable(i);
    }

    /* access modifiers changed from: private */
    public void handleReset() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleReset");
            this.mStatusBarKeyguardViewManager.reset(true);
        }
    }

    /* access modifiers changed from: private */
    public void handleVerifyUnlock() {
        Trace.beginSection("KeyguardViewMediator#handleVerifyUnlock");
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleVerifyUnlock");
            setShowingLocked(true, this.mAodShowing);
            this.mStatusBarKeyguardViewManager.dismissAndCollapse();
        }
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleNotifyStartedGoingToSleep() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyStartedGoingToSleep");
            this.mStatusBarKeyguardViewManager.onStartedGoingToSleep();
        }
    }

    /* access modifiers changed from: private */
    public void handleNotifyFinishedGoingToSleep() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyFinishedGoingToSleep");
            this.mStatusBarKeyguardViewManager.onFinishedGoingToSleep();
        }
    }

    /* access modifiers changed from: private */
    public void handleNotifyStartedWakingUp() {
        Trace.beginSection("KeyguardViewMediator#handleMotifyStartedWakingUp");
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyWakingUp");
            this.mStatusBarKeyguardViewManager.onStartedWakingUp();
        }
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleNotifyScreenTurningOn(IKeyguardDrawnCallback iKeyguardDrawnCallback) {
        Trace.beginSection("KeyguardViewMediator#handleNotifyScreenTurningOn");
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyScreenTurningOn");
            this.mStatusBarKeyguardViewManager.onScreenTurningOn();
            this.mUpdateMonitor.notifyScreenTurningOn();
            if (iKeyguardDrawnCallback != null) {
                if (this.mWakeAndUnlocking) {
                    this.mDrawnCallback = iKeyguardDrawnCallback;
                } else {
                    notifyDrawn(iKeyguardDrawnCallback);
                }
            }
        }
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleNotifyScreenTurnedOn() {
        Trace.beginSection("KeyguardViewMediator#handleNotifyScreenTurnedOn");
        if (LatencyTracker.isEnabled(this.mContext)) {
            LatencyTracker.getInstance(this.mContext).onActionEnd(5);
        }
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyScreenTurnedOn");
            OpLsState.getInstance().onScreenTurnedOn();
            this.mStatusBarKeyguardViewManager.onScreenTurnedOn();
        }
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void handleNotifyScreenTurnedOff() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyScreenTurnedOff");
            this.mDrawnCallback = null;
            this.mUpdateMonitor.onFacelockUnlocking(false);
        }
    }

    private void notifyDrawn(IKeyguardDrawnCallback iKeyguardDrawnCallback) {
        Trace.beginSection("KeyguardViewMediator#notifyDrawn");
        try {
            iKeyguardDrawnCallback.onDrawn();
        } catch (RemoteException e) {
            Slog.w("KeyguardViewMediator", "Exception calling onDrawn():", e);
        }
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void resetKeyguardDonePendingLocked() {
        this.mKeyguardDonePending = false;
        this.mHandler.removeMessages(13);
    }

    public void onBootCompleted() {
        this.mUpdateMonitor.dispatchBootCompleted();
        synchronized (this) {
            this.mBootCompleted = true;
            if (this.mBootSendUserPresent) {
                sendUserPresentBroadcast();
            }
        }
    }

    public void onWakeAndUnlocking() {
        Trace.beginSection("KeyguardViewMediator#onWakeAndUnlocking");
        this.mWakeAndUnlocking = true;
        keyguardDone();
        Trace.endSection();
    }

    public StatusBarKeyguardViewManager registerStatusBar(StatusBar statusBar, ViewGroup viewGroup, NotificationPanelView notificationPanelView, BiometricUnlockController biometricUnlockController, ViewGroup viewGroup2) {
        this.mStatusBar = statusBar;
        this.mStatusBarKeyguardViewManager.registerStatusBar(statusBar, viewGroup, notificationPanelView, biometricUnlockController, this.mDismissCallbackRegistry, viewGroup2);
        return this.mStatusBarKeyguardViewManager;
    }

    public void startKeyguardExitAnimation(long j, long j2) {
        Trace.beginSection("KeyguardViewMediator#startKeyguardExitAnimation");
        if (this.mPowerKeyCameraLaunching) {
            this.mPowerKeyCameraLaunching = false;
            this.mHandler.removeMessages(C0526R.styleable.AppCompatTheme_switchStyle);
            Log.d("KeyguardViewMediator", "handleStartKeyguardExitAnimation: callback receive from wm, remove time out message");
        }
        OpHandler opHandler = this.mHandler;
        StartKeyguardExitAnimParams startKeyguardExitAnimParams = new StartKeyguardExitAnimParams(j, j2);
        this.mHandler.sendMessage(opHandler.obtainMessage(12, startKeyguardExitAnimParams));
        Trace.endSection();
    }

    public ViewMediatorCallback getViewMediatorCallback() {
        return this.mViewMediatorCallback;
    }

    public LockPatternUtils getLockPatternUtils() {
        return this.mLockPatternUtils;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("  mSystemReady: ");
        printWriter.println(this.mSystemReady);
        printWriter.print("  mBootCompleted: ");
        printWriter.println(this.mBootCompleted);
        printWriter.print("  mBootSendUserPresent: ");
        printWriter.println(this.mBootSendUserPresent);
        printWriter.print("  mExternallyEnabled: ");
        printWriter.println(this.mExternallyEnabled);
        printWriter.print("  mShuttingDown: ");
        printWriter.println(this.mShuttingDown);
        printWriter.print("  mNeedToReshowWhenReenabled: ");
        printWriter.println(this.mNeedToReshowWhenReenabled);
        printWriter.print("  mShowing: ");
        printWriter.println(this.mShowing);
        printWriter.print("  mInputRestricted: ");
        printWriter.println(this.mInputRestricted);
        printWriter.print("  mOccluded: ");
        printWriter.println(this.mOccluded);
        printWriter.print("  mDelayedShowingSequence: ");
        printWriter.println(this.mDelayedShowingSequence);
        printWriter.print("  mExitSecureCallback: ");
        printWriter.println(this.mExitSecureCallback);
        printWriter.print("  mDeviceInteractive: ");
        printWriter.println(this.mDeviceInteractive);
        printWriter.print("  mGoingToSleep: ");
        printWriter.println(this.mGoingToSleep);
        printWriter.print("  mHiding: ");
        printWriter.println(this.mHiding);
        printWriter.print("  mWaitingUntilKeyguardVisible: ");
        printWriter.println(this.mWaitingUntilKeyguardVisible);
        printWriter.print("  mKeyguardDonePending: ");
        printWriter.println(this.mKeyguardDonePending);
        printWriter.print("  mHideAnimationRun: ");
        printWriter.println(this.mHideAnimationRun);
        printWriter.print("  mPendingReset: ");
        printWriter.println(this.mPendingReset);
        printWriter.print("  mPendingLock: ");
        printWriter.println(this.mPendingLock);
        printWriter.print("  mWakeAndUnlocking: ");
        printWriter.println(this.mWakeAndUnlocking);
        printWriter.print("  mDrawnCallback: ");
        printWriter.println(this.mDrawnCallback);
    }

    public void setAodShowing(boolean z) {
        setShowingLocked(this.mShowing, z);
    }

    public void setPulsing(boolean z) {
        this.mPulsing = z;
    }

    private void setShowingLocked(boolean z, boolean z2) {
        setShowingLocked(z, z2, false);
    }

    private void setShowingLocked(boolean z, boolean z2, boolean z3) {
        boolean z4 = (z == this.mShowing && z2 == this.mAodShowing && !z3) ? false : true;
        if (z4) {
            this.mShowing = z;
            this.mAodShowing = z2;
            StringBuilder sb = new StringBuilder();
            sb.append("updateActivityLockScreenState:");
            sb.append(z);
            sb.append(", ");
            sb.append(z2);
            Log.d("KeyguardViewMediator", sb.toString());
            updateActivityLockScreenState(z, z2);
            if (OpLsState.getInstance().getPreventModeCtrl() != null) {
                OpLsState.getInstance().getPreventModeCtrl().setKeyguardShowing(this.mShowing);
            }
            if (z4) {
                notifyDefaultDisplayCallbacks(z);
            }
        }
    }

    private void notifyDefaultDisplayCallbacks(boolean z) {
        for (int size = this.mKeyguardStateCallbacks.size() - 1; size >= 0; size--) {
            IKeyguardStateCallback iKeyguardStateCallback = (IKeyguardStateCallback) this.mKeyguardStateCallbacks.get(size);
            try {
                iKeyguardStateCallback.onShowingStateChanged(z);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onShowingStateChanged", e);
                if (e instanceof DeadObjectException) {
                    this.mKeyguardStateCallbacks.remove(iKeyguardStateCallback);
                }
            }
        }
        updateInputRestrictedLocked();
        this.mUiOffloadThread.submit(new Runnable() {
            public final void run() {
                KeyguardViewMediator.this.lambda$notifyDefaultDisplayCallbacks$5$KeyguardViewMediator();
            }
        });
    }

    public /* synthetic */ void lambda$notifyDefaultDisplayCallbacks$5$KeyguardViewMediator() {
        this.mTrustManager.reportKeyguardShowingChanged();
    }

    /* access modifiers changed from: private */
    public void notifyTrustedChangedLocked(boolean z) {
        for (int size = this.mKeyguardStateCallbacks.size() - 1; size >= 0; size--) {
            try {
                ((IKeyguardStateCallback) this.mKeyguardStateCallbacks.get(size)).onTrustedChanged(z);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call notifyTrustedChangedLocked", e);
                if (e instanceof DeadObjectException) {
                    this.mKeyguardStateCallbacks.remove(size);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyHasLockscreenWallpaperChanged(boolean z) {
        for (int size = this.mKeyguardStateCallbacks.size() - 1; size >= 0; size--) {
            try {
                ((IKeyguardStateCallback) this.mKeyguardStateCallbacks.get(size)).onHasLockscreenWallpaperChanged(z);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onHasLockscreenWallpaperChanged", e);
                if (e instanceof DeadObjectException) {
                    this.mKeyguardStateCallbacks.remove(size);
                }
            }
        }
    }

    public void addStateMonitorCallback(IKeyguardStateCallback iKeyguardStateCallback) {
        synchronized (this) {
            this.mKeyguardStateCallbacks.add(iKeyguardStateCallback);
            try {
                iKeyguardStateCallback.onSimSecureStateChanged(this.mUpdateMonitor.isSimPinSecure());
                iKeyguardStateCallback.onShowingStateChanged(this.mShowing);
                iKeyguardStateCallback.onInputRestrictedStateChanged(this.mInputRestricted);
                iKeyguardStateCallback.onTrustedChanged(this.mUpdateMonitor.getUserHasTrust(KeyguardUpdateMonitor.getCurrentUser()));
                iKeyguardStateCallback.onHasLockscreenWallpaperChanged(this.mUpdateMonitor.hasLockscreenWallpaper());
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call to IKeyguardStateCallback", e);
            }
        }
    }
}

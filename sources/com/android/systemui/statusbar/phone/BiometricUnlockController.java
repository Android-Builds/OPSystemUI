package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.hardware.biometrics.BiometricSourceType;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.Settings.Secure;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.LatencyTracker;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.R$bool;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.ScreenLifecycle.Observer;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.oneplus.systemui.statusbar.phone.OpBiometricUnlockController;
import java.io.PrintWriter;

public class BiometricUnlockController extends OpBiometricUnlockController {
    /* access modifiers changed from: private */
    public final Context mContext;
    private DozeScrimController mDozeScrimController;
    private final Tunable mFaceDismissedKeyguardTunable;
    @VisibleForTesting
    protected boolean mFaceDismissesKeyguard;
    /* access modifiers changed from: private */
    public final boolean mFaceDismissesKeyguardByDefault;
    private boolean mFadedAwayAfterWakeAndUnlock;
    private final Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mHasScreenTurnedOnSinceAuthenticating;
    private KeyguardViewMediator mKeyguardViewMediator;
    private final NotificationMediaManager mMediaManager;
    private final MetricsLogger mMetricsLogger;
    private int mMode;
    private BiometricSourceType mPendingAuthenticatedBioSourceType;
    private int mPendingAuthenticatedUserId;
    /* access modifiers changed from: private */
    public boolean mPendingShowBouncer;
    private final PowerManager mPowerManager;
    private final Runnable mReleaseBiometricWakeLockRunnable;
    private final Observer mScreenObserver;
    private ScrimController mScrimController;
    private StatusBar mStatusBar;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    /* access modifiers changed from: private */
    public final StatusBarWindowController mStatusBarWindowController;
    private final UnlockMethodCache mUnlockMethodCache;
    private final KeyguardUpdateMonitor mUpdateMonitor;
    private WakeLock mWakeLock;
    private final int mWakeUpDelay;
    private final WakefulnessLifecycle.Observer mWakefulnessObserver;

    /* renamed from: com.android.systemui.statusbar.phone.BiometricUnlockController$6 */
    static /* synthetic */ class C13926 {
        static final /* synthetic */ int[] $SwitchMap$android$hardware$biometrics$BiometricSourceType = new int[BiometricSourceType.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(8:0|1|2|3|4|5|6|8) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        static {
            /*
                android.hardware.biometrics.BiometricSourceType[] r0 = android.hardware.biometrics.BiometricSourceType.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                $SwitchMap$android$hardware$biometrics$BiometricSourceType = r0
                int[] r0 = $SwitchMap$android$hardware$biometrics$BiometricSourceType     // Catch:{ NoSuchFieldError -> 0x0014 }
                android.hardware.biometrics.BiometricSourceType r1 = android.hardware.biometrics.BiometricSourceType.FINGERPRINT     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = $SwitchMap$android$hardware$biometrics$BiometricSourceType     // Catch:{ NoSuchFieldError -> 0x001f }
                android.hardware.biometrics.BiometricSourceType r1 = android.hardware.biometrics.BiometricSourceType.FACE     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = $SwitchMap$android$hardware$biometrics$BiometricSourceType     // Catch:{ NoSuchFieldError -> 0x002a }
                android.hardware.biometrics.BiometricSourceType r1 = android.hardware.biometrics.BiometricSourceType.IRIS     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.BiometricUnlockController.C13926.<clinit>():void");
        }
    }

    public BiometricUnlockController(Context context, DozeScrimController dozeScrimController, KeyguardViewMediator keyguardViewMediator, ScrimController scrimController, StatusBar statusBar, UnlockMethodCache unlockMethodCache, Handler handler, KeyguardUpdateMonitor keyguardUpdateMonitor, TunerService tunerService) {
        this(context, dozeScrimController, keyguardViewMediator, scrimController, statusBar, unlockMethodCache, handler, keyguardUpdateMonitor, tunerService, context.getResources().getInteger(17694925), context.getResources().getBoolean(R$bool.config_faceAuthDismissesKeyguard));
    }

    @VisibleForTesting
    protected BiometricUnlockController(Context context, DozeScrimController dozeScrimController, KeyguardViewMediator keyguardViewMediator, ScrimController scrimController, StatusBar statusBar, UnlockMethodCache unlockMethodCache, Handler handler, KeyguardUpdateMonitor keyguardUpdateMonitor, TunerService tunerService, int i, boolean z) {
        super(context, keyguardViewMediator, statusBar);
        this.mPendingAuthenticatedUserId = -1;
        this.mPendingAuthenticatedBioSourceType = null;
        this.mFaceDismissedKeyguardTunable = new Tunable() {
            public void onTuningChanged(String str, String str2) {
                boolean access$000 = BiometricUnlockController.this.mFaceDismissesKeyguardByDefault;
                BiometricUnlockController biometricUnlockController = BiometricUnlockController.this;
                biometricUnlockController.mFaceDismissesKeyguard = Secure.getIntForUser(biometricUnlockController.mContext.getContentResolver(), "face_unlock_dismisses_keyguard", access$000 ? 1 : 0, KeyguardUpdateMonitor.getCurrentUser()) != 0;
            }
        };
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mReleaseBiometricWakeLockRunnable = new Runnable() {
            public void run() {
                Log.i("BiometricUnlockController", "biometric wakelock: TIMEOUT!!");
                BiometricUnlockController.this.releaseBiometricWakeLock();
            }
        };
        this.mWakefulnessObserver = new WakefulnessLifecycle.Observer() {
            public void onFinishedWakingUp() {
            }

            public void onStartedWakingUp() {
                if (BiometricUnlockController.this.mPendingShowBouncer) {
                    BiometricUnlockController.this.showBouncer();
                }
            }
        };
        this.mScreenObserver = new Observer() {
            public void onScreenTurnedOn() {
                BiometricUnlockController.this.mHasScreenTurnedOnSinceAuthenticating = true;
            }
        };
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mUpdateMonitor = keyguardUpdateMonitor;
        this.mUpdateMonitor.registerCallback(this);
        this.mMediaManager = (NotificationMediaManager) Dependency.get(NotificationMediaManager.class);
        ((WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class)).addObserver(this.mWakefulnessObserver);
        ((ScreenLifecycle) Dependency.get(ScreenLifecycle.class)).addObserver(this.mScreenObserver);
        this.mStatusBarWindowController = (StatusBarWindowController) Dependency.get(StatusBarWindowController.class);
        this.mDozeScrimController = dozeScrimController;
        this.mKeyguardViewMediator = keyguardViewMediator;
        this.mScrimController = scrimController;
        this.mStatusBar = statusBar;
        this.mUnlockMethodCache = unlockMethodCache;
        this.mHandler = handler;
        this.mWakeUpDelay = i;
        this.mFaceDismissesKeyguardByDefault = z;
        tunerService.addTunable(this.mFaceDismissedKeyguardTunable, "face_unlock_dismisses_keyguard");
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    /* access modifiers changed from: private */
    public void releaseBiometricWakeLock() {
        if (this.mWakeLock != null) {
            this.mHandler.removeCallbacks(this.mReleaseBiometricWakeLockRunnable);
            Log.i("BiometricUnlockController", "releasing biometric wakelock");
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
    }

    public void acquireWakeLock() {
        this.mWakeLock = this.mPowerManager.newWakeLock(1, "wake-and-unlock wakelock");
        this.mWakeLock.acquire();
        this.mHandler.postDelayed(this.mReleaseBiometricWakeLockRunnable, 15000);
        Log.i("BiometricUnlockController", "fingerprint acquired, grabbing biometric wakelock");
    }

    public void onBiometricAcquired(BiometricSourceType biometricSourceType) {
        Trace.beginSection("BiometricUnlockController#onBiometricAcquired");
        releaseBiometricWakeLock();
        if (!this.mUpdateMonitor.isDeviceInteractive()) {
            if (LatencyTracker.isEnabled(this.mContext)) {
                int i = 2;
                if (biometricSourceType == BiometricSourceType.FACE) {
                    i = 6;
                }
                LatencyTracker.getInstance(this.mContext).onActionStart(i);
            }
            this.mWakeLock = this.mPowerManager.newWakeLock(1, "wake-and-unlock wakelock");
            Trace.beginSection("acquiring wake-and-unlock");
            this.mWakeLock.acquire();
            Trace.endSection();
            Log.i("BiometricUnlockController", "biometric acquired, grabbing biometric wakelock");
            this.mHandler.postDelayed(this.mReleaseBiometricWakeLockRunnable, 15000);
        }
        Trace.endSection();
    }

    /* renamed from: onBiometricAuthenticated */
    public void lambda$onFinishedGoingToSleep$1$BiometricUnlockController(int i, BiometricSourceType biometricSourceType) {
        Trace.beginSection("BiometricUnlockController#onBiometricAuthenticated");
        setFingerprintState(false, this.mStatusBar.isBouncerShowing() ? 2 : 3);
        if (this.mUpdateMonitor.isGoingToSleep()) {
            this.mPendingAuthenticatedUserId = i;
            this.mPendingAuthenticatedBioSourceType = biometricSourceType;
            Trace.endSection();
            return;
        }
        this.mMetricsLogger.write(new LogMaker(1697).setType(10).setSubtype(toSubtype(biometricSourceType)));
        startWakeAndUnlock(calculateMode(biometricSourceType));
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x009f, code lost:
        if (r6 != 7) goto L_0x012e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void startWakeAndUnlock(int r10) {
        /*
            r9 = this;
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "startWakeAndUnlock("
            r0.append(r1)
            r0.append(r10)
            java.lang.String r1 = ")"
            r0.append(r1)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "BiometricUnlockController"
            android.util.Log.v(r1, r0)
            com.android.keyguard.KeyguardUpdateMonitor r0 = r9.mUpdateMonitor
            boolean r0 = r0.isDeviceInteractive()
            r9.mMode = r10
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "onFingerprintAuthenticated: mMode = "
            r2.append(r3)
            int r3 = r9.mMode
            r2.append(r3)
            java.lang.String r2 = r2.toString()
            android.util.Log.d(r1, r2)
            int r2 = r9.mMode
            java.lang.String r3 = "finger"
            java.lang.String r4 = "lock_unlock_success"
            r5 = 5
            if (r2 == r5) goto L_0x004c
            r6 = 4
            if (r2 != r6) goto L_0x0046
            goto L_0x004c
        L_0x0046:
            java.lang.String r2 = "0"
            com.oneplus.systemui.util.OpMdmLogger.log(r4, r3, r2)
            goto L_0x0051
        L_0x004c:
            java.lang.String r2 = "1"
            com.oneplus.systemui.util.OpMdmLogger.log(r4, r3, r2)
        L_0x0051:
            r2 = 0
            r9.mHasScreenTurnedOnSinceAuthenticating = r2
            com.android.systemui.statusbar.phone.StatusBarWindowController r3 = r9.mStatusBarWindowController
            r4 = 1
            boolean r3 = r3.isShowingLiveWallpaper(r4)
            r3 = r3 ^ r4
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "has wallpaper="
            r6.append(r7)
            r6.append(r3)
            java.lang.String r3 = r6.toString()
            android.util.Log.d(r1, r3)
            android.content.Context r3 = r9.mContext
            com.android.systemui.statusbar.phone.DozeParameters r3 = com.android.systemui.statusbar.phone.DozeParameters.getInstance(r3)
            boolean r3 = r3.getAlwaysOn()
            if (r10 != r4) goto L_0x0084
            if (r3 == 0) goto L_0x0084
            int r10 = r9.mWakeUpDelay
            if (r10 <= 0) goto L_0x0084
            r10 = r4
            goto L_0x0085
        L_0x0084:
            r10 = r2
        L_0x0085:
            com.android.systemui.statusbar.phone.-$$Lambda$BiometricUnlockController$eARUOiIHQidy4dPvrf3UVu6gsv0 r3 = new com.android.systemui.statusbar.phone.-$$Lambda$BiometricUnlockController$eARUOiIHQidy4dPvrf3UVu6gsv0
            r3.<init>(r0, r10)
            if (r10 != 0) goto L_0x008f
            r3.run()
        L_0x008f:
            int r6 = r9.mMode
            r7 = 2
            if (r6 == r4) goto L_0x00e0
            if (r6 == r7) goto L_0x00e0
            r8 = 3
            if (r6 == r8) goto L_0x00cf
            if (r6 == r5) goto L_0x00b2
            r0 = 6
            if (r6 == r0) goto L_0x00a3
            r0 = 7
            if (r6 == r0) goto L_0x00e0
            goto L_0x012e
        L_0x00a3:
            java.lang.String r10 = "MODE_DISMISS"
            android.os.Trace.beginSection(r10)
            com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager r10 = r9.mStatusBarKeyguardViewManager
            r10.notifyKeyguardAuthenticated(r2)
            android.os.Trace.endSection()
            goto L_0x012e
        L_0x00b2:
            if (r0 == 0) goto L_0x00cf
            com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager r10 = r9.mStatusBarKeyguardViewManager
            boolean r10 = r10.isOccluded()
            if (r10 == 0) goto L_0x00cf
            java.lang.String r10 = "MODE_UNLOCK"
            android.os.Trace.beginSection(r10)
            java.lang.String r10 = "MODE_UNLOCK, and keyguard is occluded. Direct unlock without animation."
            android.util.Log.i(r1, r10)
            com.android.systemui.keyguard.KeyguardViewMediator r10 = r9.mKeyguardViewMediator
            r10.keyguardDone()
            android.os.Trace.endSection()
            goto L_0x012e
        L_0x00cf:
            java.lang.String r10 = "MODE_UNLOCK or MODE_SHOW_BOUNCER"
            android.os.Trace.beginSection(r10)
            if (r0 != 0) goto L_0x00d9
            r9.mPendingShowBouncer = r4
            goto L_0x00dc
        L_0x00d9:
            r9.showBouncer()
        L_0x00dc:
            android.os.Trace.endSection()
            goto L_0x012e
        L_0x00e0:
            int r0 = r9.mMode
            if (r0 != r7) goto L_0x00ea
            java.lang.String r0 = "MODE_WAKE_AND_UNLOCK_PULSING"
            android.os.Trace.beginSection(r0)
            goto L_0x00fc
        L_0x00ea:
            if (r0 != r4) goto L_0x00f2
            java.lang.String r0 = "MODE_WAKE_AND_UNLOCK"
            android.os.Trace.beginSection(r0)
            goto L_0x00fc
        L_0x00f2:
            java.lang.String r0 = "MODE_WAKE_AND_UNLOCK_FROM_DREAM"
            android.os.Trace.beginSection(r0)
            com.android.keyguard.KeyguardUpdateMonitor r0 = r9.mUpdateMonitor
            r0.awakenFromDream()
        L_0x00fc:
            com.android.systemui.statusbar.NotificationMediaManager r0 = r9.mMediaManager
            r0.updateMediaMetaData(r2, r4)
            com.android.systemui.statusbar.phone.StatusBarWindowController r0 = r9.mStatusBarWindowController
            r0.setStatusBarFocusable(r2)
            if (r10 == 0) goto L_0x0111
            android.os.Handler r10 = r9.mHandler
            int r0 = r9.mWakeUpDelay
            long r0 = (long) r0
            r10.postDelayed(r3, r0)
            goto L_0x011a
        L_0x0111:
            com.android.systemui.keyguard.KeyguardViewMediator r10 = r9.mKeyguardViewMediator
            boolean r0 = r9.shouldApplySpeedUpPolicy()
            r10.onWakeAndUnlocking(r0)
        L_0x011a:
            com.android.systemui.statusbar.phone.StatusBar r10 = r9.mStatusBar
            com.android.systemui.statusbar.phone.NavigationBarView r10 = r10.getNavigationBarView()
            if (r10 == 0) goto L_0x012b
            com.android.systemui.statusbar.phone.StatusBar r10 = r9.mStatusBar
            com.android.systemui.statusbar.phone.NavigationBarView r10 = r10.getNavigationBarView()
            r10.setWakeAndUnlocking(r4)
        L_0x012b:
            android.os.Trace.endSection()
        L_0x012e:
            com.android.systemui.statusbar.phone.StatusBar r9 = r9.mStatusBar
            r9.notifyBiometricAuthModeChanged()
            android.os.Trace.endSection()
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.BiometricUnlockController.startWakeAndUnlock(int):void");
    }

    public /* synthetic */ void lambda$startWakeAndUnlock$0$BiometricUnlockController(boolean z, boolean z2) {
        if (!z) {
            Log.i("BiometricUnlockController", "bio wakelock: Authenticated, waking up...");
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 4, "android.policy:BIOMETRIC");
        }
        if (z2) {
            this.mKeyguardViewMediator.onWakeAndUnlocking();
        }
        Trace.beginSection("release wake-and-unlock");
        releaseBiometricWakeLock();
        this.mIsScreenOffUnlock = false;
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void showBouncer() {
        super.opShowBouncer();
    }

    public void onStartedGoingToSleep(int i) {
        resetMode();
        this.mFadedAwayAfterWakeAndUnlock = false;
        this.mPendingAuthenticatedUserId = -1;
        this.mPendingAuthenticatedBioSourceType = null;
    }

    public void onFinishedGoingToSleep(int i) {
        Trace.beginSection("BiometricUnlockController#onFinishedGoingToSleep");
        BiometricSourceType biometricSourceType = this.mPendingAuthenticatedBioSourceType;
        int i2 = this.mPendingAuthenticatedUserId;
        if (!(i2 == -1 || biometricSourceType == null)) {
            this.mHandler.post(new Runnable(i2, biometricSourceType) {
                private final /* synthetic */ int f$1;
                private final /* synthetic */ BiometricSourceType f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    BiometricUnlockController.this.lambda$onFinishedGoingToSleep$1$BiometricUnlockController(this.f$1, this.f$2);
                }
            });
        }
        this.mPendingAuthenticatedUserId = -1;
        this.mPendingAuthenticatedBioSourceType = null;
        Trace.endSection();
    }

    public boolean hasPendingAuthentication() {
        return this.mPendingAuthenticatedUserId != -1 && this.mUpdateMonitor.isUnlockingWithBiometricAllowed() && this.mPendingAuthenticatedUserId == KeyguardUpdateMonitor.getCurrentUser();
    }

    public int getMode() {
        return this.mMode;
    }

    private int calculateMode(BiometricSourceType biometricSourceType) {
        boolean isUnlockingWithBiometricAllowed = this.mUpdateMonitor.isUnlockingWithBiometricAllowed();
        boolean isDreaming = this.mUpdateMonitor.isDreaming();
        int i = 0;
        boolean z = biometricSourceType == BiometricSourceType.FACE && !this.mFaceDismissesKeyguard;
        int i2 = 4;
        if (!this.mUpdateMonitor.isDeviceInteractive()) {
            if (!this.mStatusBarKeyguardViewManager.isShowing()) {
                return 4;
            }
            if (this.mDozeScrimController.isPulsing() && isUnlockingWithBiometricAllowed) {
                if (!z) {
                    i = 2;
                }
                return i;
            } else if (isUnlockingWithBiometricAllowed || !this.mUnlockMethodCache.isMethodSecure()) {
                return 1;
            } else {
                return 3;
            }
        } else if (isUnlockingWithBiometricAllowed && isDreaming && !z) {
            return 7;
        } else {
            if (this.mStatusBarKeyguardViewManager.isShowing()) {
                if ((this.mStatusBarKeyguardViewManager.isBouncerShowing() || this.mStatusBarKeyguardViewManager.isBouncerPartiallyVisible()) && isUnlockingWithBiometricAllowed) {
                    return 6;
                }
                if (isUnlockingWithBiometricAllowed) {
                    if (!z) {
                        i2 = 5;
                    }
                    return i2;
                } else if (!this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    return 3;
                }
            }
            return 0;
        }
    }

    public void onBiometricAuthFailed(BiometricSourceType biometricSourceType) {
        this.mMetricsLogger.write(new LogMaker(1697).setType(11).setSubtype(toSubtype(biometricSourceType)));
        if (biometricSourceType == BiometricSourceType.FINGERPRINT) {
            onFingerprintAuthFailed();
        }
        cleanup();
    }

    public void onBiometricError(int i, String str, BiometricSourceType biometricSourceType) {
        this.mMetricsLogger.write(new LogMaker(1697).setType(15).setSubtype(toSubtype(biometricSourceType)).addTaggedData(1741, Integer.valueOf(i)));
        StringBuilder sb = new StringBuilder();
        sb.append("onFingerprintError: ");
        sb.append(str);
        Log.d("BiometricUnlockController", sb.toString());
        onFingerprintUnlockCancel(2);
        cleanup();
    }

    private void cleanup() {
        releaseBiometricWakeLock();
        this.mIsScreenOffUnlock = false;
    }

    public void startKeyguardFadingAway() {
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                BiometricUnlockController.this.mStatusBarWindowController.setForceDozeBrightness(false);
            }
        }, 96);
    }

    public void finishKeyguardFadingAway() {
        if (isWakeAndUnlock()) {
            this.mFadedAwayAfterWakeAndUnlock = true;
        }
        resetMode();
    }

    private void resetMode() {
        Log.d("BiometricUnlockController", "resetMode");
        this.mMode = 0;
        this.mStatusBarWindowController.setForceDozeBrightness(false);
        if (this.mStatusBar.getNavigationBarView() != null) {
            this.mStatusBar.getNavigationBarView().setWakeAndUnlocking(false);
        }
        this.mStatusBar.notifyBiometricAuthModeChanged();
        setFaceLockMode(0);
        changePanelVisibilityByAlpha(1, true);
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println(" BiometricUnlockController:");
        printWriter.print("   mMode=");
        printWriter.println(this.mMode);
        printWriter.print("   mWakeLock=");
        printWriter.println(this.mWakeLock);
    }

    public boolean isWakeAndUnlock() {
        int i = this.mMode;
        return i == 1 || i == 2 || i == 7;
    }

    public boolean isBiometricUnlock() {
        return opIsBiometricUnlock();
    }

    private int toSubtype(BiometricSourceType biometricSourceType) {
        int i = C13926.$SwitchMap$android$hardware$biometrics$BiometricSourceType[biometricSourceType.ordinal()];
        if (i == 1) {
            return 0;
        }
        if (i != 2) {
            return i != 3 ? 3 : 2;
        }
        return 1;
    }
}

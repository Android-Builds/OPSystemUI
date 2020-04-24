package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.IWallpaperManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.StatusBarManager;
import android.app.UiModeManager;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager.Stub;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.service.dreams.IDreamManager;
import android.service.notification.StatusBarNotification;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.RemoteAnimationAdapter;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewStub;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.DateTimeView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.colorextraction.ColorExtractor.GradientColors;
import com.android.internal.colorextraction.ColorExtractor.OnColorsChangedListener;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.RegisterStatusBarResult;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardStatusView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.ViewMediatorCallback;
import com.android.settingslib.Utils;
import com.android.systemui.ActivityIntentHelper;
import com.android.systemui.ActivityStarterDelegate;
import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.EventLogTags;
import com.android.systemui.ForegroundServiceController;
import com.android.systemui.InitController;
import com.android.systemui.Interpolators;
import com.android.systemui.Prefs;
import com.android.systemui.R$anim;
import com.android.systemui.R$array;
import com.android.systemui.R$bool;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.R$style;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.appops.AppOpsController;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.bubbles.BubbleController.BubbleExpandListener;
import com.android.systemui.charging.WirelessChargingAnimation;
import com.android.systemui.classifier.FalsingLog;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeHost.PulseCallback;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.doze.DozeReceiver;
import com.android.systemui.fragments.ExtensionFragmentListener;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.fragments.FragmentHostManager.FragmentListener;
import com.android.systemui.keyguard.KeyguardSliceProvider;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.ScreenLifecycle.Observer;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.p007qs.QSFragment;
import com.android.systemui.p007qs.QSPanel;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.ActivityStarter.OnDismissAction;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.plugins.p006qs.C0959QS;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper.SnoozeOption;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.ScreenPinningRequest;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.stackdivider.WindowManagerProxy;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.AmbientPulseManager.OnAmbientChangedListener;
import com.android.systemui.statusbar.BackDropView;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyboardShortcuts;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.statusbar.NotificationListener;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.NotificationViewHierarchyManager;
import com.android.systemui.statusbar.PulseExpansionHandler;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.NotificationActivityStarter;
import com.android.systemui.statusbar.notification.NotificationAlertingManager;
import com.android.systemui.statusbar.notification.NotificationClicker;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationInterruptionStateProvider;
import com.android.systemui.statusbar.notification.NotificationListController;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.NotificationRowBinderImpl;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationGutsManager;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.ExtensionController.Extension;
import com.android.systemui.statusbar.policy.ExtensionController.ExtensionBuilder;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.policy.RemoteInputQuickSettingsDisabler;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.policy.ZenModeController.Callback;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.InjectionInflationController;
import com.android.systemui.util.NotificationChannels;
import com.android.systemui.volume.VolumeComponent;
import com.oneplus.aod.OpAodDisplayViewManager;
import com.oneplus.aod.OpAodWindowManager;
import com.oneplus.battery.OpChargingAnimationController;
import com.oneplus.faceunlock.OpFacelockController;
import com.oneplus.notification.OpNotificationController;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.keyguard.OpKeyguardViewMediator;
import com.oneplus.systemui.statusbar.phone.OpStatusBar;
import com.oneplus.systemui.statusbar.phone.OpStatusBar.OpDozeCallbacks;
import com.oneplus.systemui.util.OpMdmLogger;
import com.oneplus.util.OpNavBarUtils;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StatusBar extends OpStatusBar implements DemoMode, ActivityStarter, OnUnlockMethodChangedListener, OnHeadsUpChangedListener, Callbacks, Callback, OnColorsChangedListener, ConfigurationListener, StateListener, ShadeController, ActivityLaunchAnimator.Callback, OnAmbientChangedListener, AppOpsController.Callback {
    protected static final int[] APP_OPS = {26, 24, 27, 0, 1};
    public static final boolean DEBUG_CAMERA_LIFT = Build.DEBUG_ONEPLUS;
    public static final boolean ENABLE_CHILD_NOTIFICATIONS = SystemProperties.getBoolean("debug.child_notifs", true);
    public static final boolean ONLY_CORE_APPS;
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new Builder().setContentType(4).setUsage(13).build();
    private final int[] mAbsPos = new int[2];
    protected AccessibilityManager mAccessibilityManager;
    private ActivityIntentHelper mActivityIntentHelper;
    private ActivityLaunchAnimator mActivityLaunchAnimator;
    /* access modifiers changed from: private */
    public View mAmbientIndicationContainer;
    protected AmbientPulseManager mAmbientPulseManager = ((AmbientPulseManager) Dependency.get(AmbientPulseManager.class));
    private final Runnable mAnimateCollapsePanels = new Runnable() {
        public final void run() {
            StatusBar.this.animateCollapsePanels();
        }
    };
    protected AssistManager mAssistManager;
    @VisibleForTesting
    protected AutoHideController mAutoHideController;
    private final BroadcastReceiver mBannerActionBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String str = "com.android.systemui.statusbar.banner_action_setup";
            if ("com.android.systemui.statusbar.banner_action_cancel".equals(action) || str.equals(action)) {
                ((NotificationManager) StatusBar.this.mContext.getSystemService("notification")).cancel(5);
                Secure.putInt(StatusBar.this.mContext.getContentResolver(), "show_note_about_notification_hiding", 0);
                if (str.equals(action)) {
                    StatusBar.this.animateCollapsePanels(2, true);
                    StatusBar.this.mContext.startActivity(new Intent("android.settings.ACTION_APP_NOTIFICATION_REDACTION").addFlags(268435456));
                }
            }
        }
    };
    protected IStatusBarService mBarService;
    /* access modifiers changed from: private */
    public BatteryController mBatteryController;
    protected BiometricUnlockController mBiometricUnlockController;
    protected boolean mBouncerShowing;
    private boolean mBouncerWasShowingWhenHidden;
    private BrightnessMirrorController mBrightnessMirrorController;
    private boolean mBrightnessMirrorVisible;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int i = 0;
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                KeyboardShortcuts.dismiss();
                if (StatusBar.this.mRemoteInputManager.getController() != null) {
                    StatusBar.this.mRemoteInputManager.getController().closeRemoteInputs();
                }
                if (StatusBar.this.mBubbleController.isStackExpanded()) {
                    StatusBar.this.mBubbleController.collapseStack();
                }
                if (StatusBar.this.mLockscreenUserManager.isCurrentProfile(getSendingUserId())) {
                    String stringExtra = intent.getStringExtra("reason");
                    if (stringExtra != null && stringExtra.equals("recentapps")) {
                        i = 2;
                    }
                    StatusBar.this.animateCollapsePanels(i);
                }
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                StatusBarWindowController statusBarWindowController = StatusBar.this.mStatusBarWindowController;
                if (statusBarWindowController != null) {
                    statusBarWindowController.setNotTouchable(false);
                }
                if (StatusBar.this.mBubbleController.isStackExpanded()) {
                    StatusBar.this.mBubbleController.collapseStack();
                }
                StatusBar.this.finishBarAnimations();
                StatusBar.this.resetUserExpandedStates();
                StatusBar.this.hideNavigationBarGuide();
            } else if ("android.app.action.SHOW_DEVICE_MONITORING_DIALOG".equals(action)) {
                StatusBar.this.mQSPanel.showDeviceMonitoringDialog();
            }
        }
    };
    protected BubbleController mBubbleController;
    private final BubbleExpandListener mBubbleExpandListener = new BubbleExpandListener() {
        public final void onBubbleExpandChanged(boolean z, String str) {
            StatusBar.this.lambda$new$1$StatusBar(z, str);
        }
    };
    private long[] mCameraLaunchGestureVibePattern;
    private CarModeScreenshotReceiver mCarModeReceiver;
    /* access modifiers changed from: private */
    public final Runnable mCheckBarModes = new Runnable() {
        public final void run() {
            StatusBar.this.checkBarModes();
        }
    };
    private SysuiColorExtractor mColorExtractor;
    protected CommandQueue mCommandQueue;
    private final Point mCurrentDisplaySize = new Point();
    private boolean mDemoCarModeHighHintDemoMode = false;
    private Notification.Builder mDemoCarModeHighlightHint = null;
    private Notification.Builder mDemoHighlightHint = null;
    private boolean mDemoMode;
    private boolean mDemoModeAllowed;
    private final BroadcastReceiver mDemoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.android.systemui.demo".equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String lowerCase = extras.getString("command", "").trim().toLowerCase();
                    if (lowerCase.length() > 0) {
                        try {
                            StatusBar.this.dispatchDemoCommand(lowerCase, extras);
                        } catch (Throwable th) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Error running demo command, intent=");
                            sb.append(intent);
                            Log.w("StatusBar", sb.toString(), th);
                        }
                    }
                }
            } else {
                "fake_artwork".equals(action);
            }
        }
    };
    protected boolean mDeviceInteractive;
    protected DevicePolicyManager mDevicePolicyManager;
    /* access modifiers changed from: private */
    public DeviceProvisionedController mDeviceProvisionedController = ((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class));
    private int mDisabled1 = 0;
    private int mDisabled2 = 0;
    protected Display mDisplay;
    private int mDisplayId;
    private final DisplayMetrics mDisplayMetrics = ((DisplayMetrics) Dependency.get(DisplayMetrics.class));
    protected DozeScrimController mDozeScrimController;
    @VisibleForTesting
    DozeServiceHost mDozeServiceHost = new DozeServiceHost();
    protected boolean mDozing;
    private boolean mDozingRequested;
    private NotificationEntry mDraggedDownEntry;
    private IDreamManager mDreamManager;
    protected NotificationEntryManager mEntryManager;
    private boolean mExpandedVisible;
    protected FalsingManager mFalsingManager;
    protected ForegroundServiceController mForegroundServiceController;
    private final GestureRecorder mGestureRec = null;
    private FrameLayout mGestureView = null;
    protected WakeLock mGestureWakeLock;
    private final OnClickListener mGoToLockedShadeListener = new OnClickListener() {
        public final void onClick(View view) {
            StatusBar.this.lambda$new$0$StatusBar(view);
        }
    };
    protected NotificationGroupAlertTransferHelper mGroupAlertTransferHelper;
    protected NotificationGroupManager mGroupManager;
    private NotificationGutsManager mGutsManager;
    protected final C1543H mHandler = createHandler();
    private HeadsUpAppearanceController mHeadsUpAppearanceController;
    protected HeadsUpManagerPhone mHeadsUpManager;
    private boolean mHideIconsForBouncer;
    private boolean mHideNavBar = false;
    private boolean mHighHintDemoMode = false;
    protected StatusBarIconController mIconController;
    private PhoneStatusBarPolicy mIconPolicy;
    InjectionInflationController mInjectionInflater;
    private int mInteractingWindows;
    private boolean mIsInMultiWindow = false;
    protected boolean mIsKeyguard;
    private boolean mIsOccluded;
    KeyguardIndicationController mKeyguardIndicationController;
    protected KeyguardManager mKeyguardManager;
    /* access modifiers changed from: private */
    public KeyguardMonitor mKeyguardMonitor = ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class));
    @VisibleForTesting
    KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    protected KeyguardViewMediator mKeyguardViewMediator;
    private ViewMediatorCallback mKeyguardViewMediatorCallback;
    /* access modifiers changed from: private */
    public int mLastCameraLaunchSource;
    private final Rect mLastDockedStackBounds = new Rect();
    private final Rect mLastFullscreenStackBounds = new Rect();
    private int mLastLoggedStateFingerprint;
    /* access modifiers changed from: private */
    public boolean mLaunchCameraOnFinishedGoingToSleep;
    /* access modifiers changed from: private */
    public boolean mLaunchCameraWhenFinishedWaking;
    private Runnable mLaunchTransitionEndRunnable;
    private LightBarController mLightBarController;
    private LockPatternUtils mLockPatternUtils;
    protected NotificationLockscreenUserManager mLockscreenUserManager;
    protected LockscreenWallpaper mLockscreenWallpaper;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private NotificationMediaManager mMediaManager;
    private final MetricsLogger mMetricsLogger = ((MetricsLogger) Dependency.get(MetricsLogger.class));
    private int mNaturalBarHeight = -1;
    protected NavigationBarController mNavigationBarController;
    protected NavigationBarGuide mNavigationBarGuide;
    private NetworkController mNetworkController;
    private boolean mNoAnimationOnNextBarModeChange;
    private NotificationActivityStarter mNotificationActivityStarter;
    private final NotificationAlertingManager mNotificationAlertingManager = ((NotificationAlertingManager) Dependency.get(NotificationAlertingManager.class));
    protected NotificationIconAreaController mNotificationIconAreaController;
    private NotificationInterruptionStateProvider mNotificationInterruptionStateProvider;
    private NotificationListController mNotificationListController;
    protected NotificationListener mNotificationListener;
    protected NotificationLogger mNotificationLogger;
    protected NotificationPanelView mNotificationPanel;
    protected NotificationShelf mNotificationShelf;
    protected boolean mPanelExpanded;
    private View mPendingRemoteInputView;
    /* access modifiers changed from: private */
    public int mPhoneState;
    private final ArrayList<Runnable> mPostCollapseRunnables = new ArrayList<>();
    protected PowerManager mPowerManager;
    protected NotificationPresenter mPresenter;
    PulseExpansionHandler mPulseExpansionHandler;
    /* access modifiers changed from: private */
    public boolean mPulsing;
    /* access modifiers changed from: private */
    public QSPanel mQSPanel;
    private final Object mQueueLock = new Object();
    protected Recents mRecents;
    protected NotificationRemoteInputManager mRemoteInputManager;
    private RemoteInputQuickSettingsDisabler mRemoteInputQuickSettingsDisabler = ((RemoteInputQuickSettingsDisabler) Dependency.get(RemoteInputQuickSettingsDisabler.class));
    private View mReportRejectedTouch;
    private ScreenLifecycle mScreenLifecycle;
    final Observer mScreenObserver = new Observer() {
        public void onScreenTurningOn() {
            StatusBar.this.mFalsingManager.onScreenTurningOn();
            StatusBar.this.mNotificationPanel.onScreenTurningOn();
        }

        public void onScreenTurnedOn() {
            StatusBar.this.mScrimController.onScreenTurnedOn();
        }

        public void onScreenTurnedOff() {
            StatusBar.this.updateDozing();
            StatusBar.this.mFalsingManager.onScreenOff();
            StatusBar.this.mScrimController.onScreenTurnedOff();
            StatusBar.this.updateIsKeyguard();
            if (StatusBar.this.mAodDisplayViewManager != null) {
                DozeScrimController dozeScrimController = StatusBar.this.mDozeScrimController;
                if (dozeScrimController != null && !dozeScrimController.isPulsing()) {
                    StatusBar.this.mAodDisplayViewManager.resetStatus();
                }
            }
            StatusBarWindowController statusBarWindowController = StatusBar.this.mStatusBarWindowController;
            if (statusBarWindowController != null) {
                statusBarWindowController.setNotTouchable(false);
            }
        }
    };
    private ScreenPinningRequest mScreenPinningRequest;
    protected ScrimController mScrimController;
    private ShadeController mShadeController;
    private StatusBarSignalPolicy mSignalPolicy;
    protected NotificationStackScrollLayout mStackScroller;
    final Runnable mStartTracing = new Runnable() {
        public void run() {
            StatusBar.this.vibrate();
            SystemClock.sleep(250);
            Log.d("StatusBar", "startTracing");
            Debug.startMethodTracing("/data/statusbar-traces/trace");
            StatusBar statusBar = StatusBar.this;
            statusBar.mHandler.postDelayed(statusBar.mStopTracing, 10000);
        }
    };
    protected int mState;
    protected StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private int mStatusBarMode;
    private final SysuiStatusBarStateController mStatusBarStateController = ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class));
    private LogMaker mStatusBarStateLog;
    protected PhoneStatusBarView mStatusBarView;
    protected StatusBarWindowView mStatusBarWindow;
    protected StatusBarWindowController mStatusBarWindowController;
    private boolean mStatusBarWindowHidden;
    private int mStatusBarWindowState = 0;
    final Runnable mStopTracing = new Runnable() {
        public final void run() {
            StatusBar.this.lambda$new$24$StatusBar();
        }
    };
    private int mSystemUiVisibility = 0;
    /* access modifiers changed from: private */
    public final int[] mTmpInt2 = new int[2];
    private boolean mTopHidesStatusBar;
    private UiModeManager mUiModeManager;
    private final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));
    protected UnlockMethodCache mUnlockMethodCache;
    private final ScrimController.Callback mUnlockScrimCallback = new ScrimController.Callback() {
        public void onFinished() {
            StatusBar statusBar = StatusBar.this;
            if (statusBar.mStatusBarKeyguardViewManager == null) {
                Log.w("StatusBar", "Tried to notify keyguard visibility when mStatusBarKeyguardViewManager was null");
                return;
            }
            if (statusBar.mKeyguardMonitor.isKeyguardFadingAway()) {
                StatusBar.this.mStatusBarKeyguardViewManager.onKeyguardFadedAway();
            }
        }

        public void onCancelled() {
            onFinished();
        }
    };
    private final KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        public void onDreamingStateChanged(boolean z) {
            if (z) {
                StatusBar.this.maybeEscalateHeadsUp();
            }
        }

        public void onStrongAuthStateChanged(int i) {
            super.onStrongAuthStateChanged(i);
            StatusBar.this.mEntryManager.updateNotifications();
        }

        public void onUserSwitchComplete(int i) {
            ContentObserver contentObserver = StatusBar.this.mDisableQsObserver;
            if (contentObserver != null) {
                contentObserver.onChange(false);
            }
        }

        public void onPhoneStateChanged(int i) {
            super.onPhoneStateChanged(i);
            StatusBar.this.mPhoneState = i;
        }
    };
    @VisibleForTesting
    protected boolean mUserSetup = false;
    private final DeviceProvisionedListener mUserSetupObserver = new DeviceProvisionedListener() {
        public void onUserSetupChanged() {
            boolean isUserSetup = StatusBar.this.mDeviceProvisionedController.isUserSetup(StatusBar.this.mDeviceProvisionedController.getCurrentUser());
            StringBuilder sb = new StringBuilder();
            sb.append("mUserSetupObserver - DeviceProvisionedListener called for user ");
            sb.append(StatusBar.this.mDeviceProvisionedController.getCurrentUser());
            Log.d("StatusBar", sb.toString());
            StatusBar statusBar = StatusBar.this;
            if (isUserSetup != statusBar.mUserSetup) {
                statusBar.mUserSetup = isUserSetup;
                if (!statusBar.mUserSetup && statusBar.mStatusBarView != null) {
                    statusBar.animateCollapseQuickSettings();
                }
                StatusBar statusBar2 = StatusBar.this;
                NotificationPanelView notificationPanelView = statusBar2.mNotificationPanel;
                if (notificationPanelView != null) {
                    notificationPanelView.setUserSetupComplete(statusBar2.mUserSetup);
                }
                StatusBar.this.updateQsExpansionEnabled();
            }
        }
    };
    protected UserSwitcherController mUserSwitcherController;
    private boolean mVibrateOnOpening;
    private Vibrator mVibrator;
    private VibratorHelper mVibratorHelper;
    protected NotificationViewHierarchyManager mViewHierarchyManager;
    protected boolean mVisible;
    private boolean mVisibleToUser;
    protected VisualStabilityManager mVisualStabilityManager;
    private VolumeComponent mVolumeComponent;
    /* access modifiers changed from: private */
    public boolean mWakeUpComingFromTouch;
    NotificationWakeUpCoordinator mWakeUpCoordinator;
    /* access modifiers changed from: private */
    public PointF mWakeUpTouchLocation;
    @VisibleForTesting
    WakefulnessLifecycle mWakefulnessLifecycle;
    @VisibleForTesting
    final WakefulnessLifecycle.Observer mWakefulnessObserver = new WakefulnessLifecycle.Observer() {
        public void onFinishedGoingToSleep() {
            StatusBar.this.mCameraWakeAndUnlocking = false;
            StatusBar.this.mNotificationPanel.onAffordanceLaunchEnded();
            StatusBar.this.releaseGestureWakeLock();
            StatusBar.this.mLaunchCameraWhenFinishedWaking = false;
            StatusBar statusBar = StatusBar.this;
            statusBar.mDeviceInteractive = false;
            statusBar.mWakeUpComingFromTouch = false;
            StatusBar.this.mWakeUpTouchLocation = null;
            StatusBar.this.mVisualStabilityManager.setScreenOn(false);
            StatusBar.this.updateVisibleToUser();
            StatusBar.this.updateNotificationPanelTouchState();
            StatusBar.this.mStatusBarWindow.cancelCurrentTouch();
            if (StatusBar.this.mLaunchCameraOnFinishedGoingToSleep) {
                StatusBar.this.mLaunchCameraOnFinishedGoingToSleep = false;
                StatusBar.this.mHandler.post(new Runnable() {
                    public final void run() {
                        C152912.this.lambda$onFinishedGoingToSleep$0$StatusBar$12();
                    }
                });
            }
            StatusBar.this.updateIsKeyguard();
        }

        public /* synthetic */ void lambda$onFinishedGoingToSleep$0$StatusBar$12() {
            StatusBar statusBar = StatusBar.this;
            statusBar.onCameraLaunchGestureDetected(statusBar.mLastCameraLaunchSource);
        }

        public void onStartedGoingToSleep() {
            StatusBar.this.updateNotificationPanelTouchState();
            StatusBar.this.notifyHeadsUpGoingToSleep();
            StatusBar.this.dismissVolumeDialog();
            StatusBar.this.opOnStartedGoingToSleep();
        }

        public void onStartedWakingUp() {
            StatusBar statusBar = StatusBar.this;
            statusBar.mDeviceInteractive = true;
            statusBar.mWakeUpCoordinator.setWakingUp(true);
            StatusBar.this.mAmbientPulseManager.releaseAllImmediately();
            StatusBar.this.mVisualStabilityManager.setScreenOn(true);
            StatusBar.this.updateVisibleToUser();
            if (StatusBar.this.mCameraWakeAndUnlocking) {
                StatusBarWindowController statusBarWindowController = StatusBar.this.mStatusBarWindowController;
                if (statusBarWindowController != null && statusBarWindowController.isKeyguardFadingAway()) {
                    Log.d("StatusBar", "not update when camera unlocking");
                    return;
                }
            }
            StatusBar.this.updateIsKeyguard();
            StatusBar.this.mDozeServiceHost.stopDozing();
            StatusBar.this.updateNotificationPanelTouchState();
            StatusBar.this.mPulseExpansionHandler.onStartedWakingUp();
        }

        public void onFinishedWakingUp() {
            StatusBar.this.opOnFinishedWakingUp();
            StatusBar.this.mWakeUpCoordinator.setWakingUp(false);
            if (StatusBar.this.mLaunchCameraWhenFinishedWaking) {
                StatusBar statusBar = StatusBar.this;
                statusBar.mNotificationPanel.launchCamera(false, statusBar.mLastCameraLaunchSource);
                StatusBar.this.mLaunchCameraWhenFinishedWaking = false;
            }
            StatusBar.this.updateScrimController();
        }
    };
    private final BroadcastReceiver mWallpaperChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            WallpaperManager wallpaperManager = (WallpaperManager) context.getSystemService(WallpaperManager.class);
            if (wallpaperManager == null) {
                Log.w("StatusBar", "WallpaperManager not available");
                return;
            }
            WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo(-2);
            boolean z = true;
            boolean z2 = !DozeParameters.getInstance(StatusBar.this.mContext).getDisplayNeedsBlanking();
            if (!StatusBar.this.mContext.getResources().getBoolean(17891420) || ((wallpaperInfo != null || !z2) && (wallpaperInfo == null || !wallpaperInfo.supportsAmbientMode()))) {
                z = false;
            }
            StatusBar.this.mStatusBarWindowController.setWallpaperSupportsAmbientMode(z);
            StatusBar.this.mScrimController.setWallpaperSupportsAmbientMode(z);
        }
    };
    private boolean mWereIconsJustHidden;
    protected WindowManager mWindowManager;
    protected IWindowManager mWindowManagerService;
    private ZenModeController mZenController;

    public class CarModeScreenshotReceiver extends BroadcastReceiver {
        public CarModeScreenshotReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Log.i("StatusBar", " cancel carMode");
            StatusBar.this.showDemooCarModeHighLight(false, false);
        }
    }

    @VisibleForTesting
    final class DozeServiceHost implements DozeHost, OpDozeCallbacks {
        private boolean mAnimateScreenOff;
        private boolean mAnimateWakeup;
        private final ArrayList<DozeHost.Callback> mCallbacks = new ArrayList<>();
        /* access modifiers changed from: private */
        public boolean mIgnoreTouchWhilePulsing;

        DozeServiceHost() {
        }

        public ArrayList<DozeHost.Callback> getCallbacks() {
            return this.mCallbacks;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("PSB.DozeServiceHost[mCallbacks=");
            sb.append(this.mCallbacks.size());
            sb.append("]");
            return sb.toString();
        }

        public void firePowerSaveChanged(boolean z) {
            Iterator it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                ((DozeHost.Callback) it.next()).onPowerSaveChanged(z);
            }
        }

        public void fireNotificationPulse() {
            Iterator it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                ((DozeHost.Callback) it.next()).onNotificationAlerted();
            }
        }

        public void addCallback(DozeHost.Callback callback) {
            this.mCallbacks.add(callback);
        }

        public void removeCallback(DozeHost.Callback callback) {
            this.mCallbacks.remove(callback);
        }

        public void startDozing() {
            StatusBar.this.mStartDozingRequested = true;
            if (StatusBar.this.mOpSceneModeObserver.isInBrickMode()) {
                Slog.i("StatusBar", "Do not start dozing in brick mode");
            } else {
                StatusBar.this.checkToStartDozing();
            }
        }

        public void pulseWhileDozing(final PulseCallback pulseCallback, int i) {
            if (i == 5) {
                StatusBar.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 4, "com.android.systemui:LONG_PRESS");
                StatusBar.this.startAssist(new Bundle());
                return;
            }
            if (i == 8) {
                StatusBar.this.mScrimController.setWakeLockScreenSensorActive(true);
            }
            if (i == 6) {
                StatusBarWindowView statusBarWindowView = StatusBar.this.mStatusBarWindow;
                if (statusBarWindowView != null) {
                    statusBarWindowView.suppressWakeUpGesture(true);
                }
            }
            final boolean z = i == 1;
            StatusBar.this.mPulsing = true;
            StatusBar.this.mDozeScrimController.pulse(new PulseCallback() {
                public void onPulseStarted() {
                    pulseCallback.onPulseStarted();
                    StatusBar.this.updateNotificationPanelTouchState();
                    setPulsing(true);
                }

                public void onPulseFinished() {
                    StatusBar.this.mPulsing = false;
                    pulseCallback.onPulseFinished();
                    StatusBar.this.updateNotificationPanelTouchState();
                    StatusBar.this.mScrimController.setWakeLockScreenSensorActive(false);
                    StatusBarWindowView statusBarWindowView = StatusBar.this.mStatusBarWindow;
                    if (statusBarWindowView != null) {
                        statusBarWindowView.suppressWakeUpGesture(false);
                    }
                    setPulsing(false);
                }

                private void setPulsing(boolean z) {
                    StatusBar.this.mStatusBarKeyguardViewManager.setPulsing(z);
                    StatusBar.this.mKeyguardViewMediator.setPulsing(z);
                    StatusBar.this.mNotificationPanel.setPulsing(z);
                    StatusBar.this.mVisualStabilityManager.setPulsing(z);
                    StatusBar.this.mStatusBarWindow.setPulsing(z);
                    DozeServiceHost.this.mIgnoreTouchWhilePulsing = false;
                    KeyguardUpdateMonitor keyguardUpdateMonitor = StatusBar.this.mKeyguardUpdateMonitor;
                    if (keyguardUpdateMonitor != null && z) {
                        keyguardUpdateMonitor.onAuthInterruptDetected(z);
                    }
                    StatusBar.this.updateScrimController();
                    StatusBar.this.mPulseExpansionHandler.setPulsing(z);
                    StatusBar.this.mWakeUpCoordinator.setPulsing(z);
                }
            }, i);
            StatusBar.this.updateScrimController();
        }

        public void stopDozing() {
            StatusBar.this.mStartDozingRequested = false;
            StatusBar.this.checkToStopDozing();
        }

        public void onIgnoreTouchWhilePulsing(boolean z) {
            if (z != this.mIgnoreTouchWhilePulsing) {
                DozeLog.tracePulseTouchDisabledByProx(StatusBar.this.mContext, z);
            }
            this.mIgnoreTouchWhilePulsing = z;
            if (StatusBar.this.isDozing() && z) {
                StatusBar.this.mStatusBarWindow.cancelCurrentTouch();
            }
        }

        public void dozeTimeTick() {
            StatusBar.this.mNotificationPanel.dozeTimeTick();
            if (StatusBar.this.mAmbientIndicationContainer instanceof DozeReceiver) {
                ((DozeReceiver) StatusBar.this.mAmbientIndicationContainer).dozeTimeTick();
            }
        }

        public boolean isPowerSaveActive() {
            return StatusBar.this.mBatteryController.isAodPowerSave();
        }

        public boolean isPulsingBlocked() {
            if (StatusBar.this.mBiometricUnlockController.getMode() == 1) {
                return true;
            }
            if (KeyguardUpdateMonitor.getInstance(StatusBar.this.mContext).getGoingToSleepReason() != 10 || StatusBar.this.mPhoneState == 0) {
                return false;
            }
            return true;
        }

        public boolean isProvisioned() {
            return StatusBar.this.mDeviceProvisionedController.isDeviceProvisioned() && StatusBar.this.mDeviceProvisionedController.isCurrentUserSetup();
        }

        public boolean isBlockingDoze() {
            if (!StatusBar.this.mBiometricUnlockController.hasPendingAuthentication()) {
                return false;
            }
            Log.i("StatusBar", "Blocking AOD because fingerprint has authenticated");
            return true;
        }

        public void extendPulse(int i) {
            if (i == 8) {
                StatusBar.this.mScrimController.setWakeLockScreenSensorActive(true);
            }
            if (StatusBar.this.mDozeScrimController.isPulsing() && StatusBar.this.mAmbientPulseManager.hasNotifications()) {
                StatusBar.this.mAmbientPulseManager.extendPulse();
            }
            StatusBar.this.mDozeScrimController.extendPulse(i);
        }

        public void stopPulsing() {
            if (StatusBar.this.mDozeScrimController.isPulsing()) {
                StatusBar.this.mDozeScrimController.pulseOutNow();
            }
        }

        public void setAnimateWakeup(boolean z) {
            if (StatusBar.this.mWakefulnessLifecycle.getWakefulness() != 2 && StatusBar.this.mWakefulnessLifecycle.getWakefulness() != 1) {
                this.mAnimateWakeup = z;
            }
        }

        public void setAnimateScreenOff(boolean z) {
            this.mAnimateScreenOff = z;
        }

        public void onSlpiTap(float f, float f2) {
            if (f > 0.0f && f2 > 0.0f && StatusBar.this.mAmbientIndicationContainer != null && StatusBar.this.mAmbientIndicationContainer.getVisibility() == 0) {
                StatusBar.this.mAmbientIndicationContainer.getLocationOnScreen(StatusBar.this.mTmpInt2);
                float f3 = f - ((float) StatusBar.this.mTmpInt2[0]);
                float f4 = f2 - ((float) StatusBar.this.mTmpInt2[1]);
                if (0.0f <= f3 && f3 <= ((float) StatusBar.this.mAmbientIndicationContainer.getWidth()) && 0.0f <= f4 && f4 <= ((float) StatusBar.this.mAmbientIndicationContainer.getHeight())) {
                    dispatchTap(StatusBar.this.mAmbientIndicationContainer, f3, f4);
                }
            }
        }

        public void setDozeScreenBrightness(int i) {
            StatusBar.this.mStatusBarWindowController.setDozeScreenBrightness(i);
        }

        public void setAodDimmingScrim(float f) {
            StatusBar.this.mScrimController.setAodFrontScrimAlpha(f);
        }

        private void dispatchTap(View view, float f, float f2) {
            View view2 = view;
            float f3 = f;
            float f4 = f2;
            long elapsedRealtime = SystemClock.elapsedRealtime();
            dispatchTouchEvent(view2, f3, f4, elapsedRealtime, 0);
            dispatchTouchEvent(view2, f3, f4, elapsedRealtime, 1);
        }

        private void dispatchTouchEvent(View view, float f, float f2, long j, int i) {
            MotionEvent obtain = MotionEvent.obtain(j, j, i, f, f2, 0);
            view.dispatchTouchEvent(obtain);
            obtain.recycle();
        }

        /* access modifiers changed from: private */
        public boolean shouldAnimateWakeup() {
            return this.mAnimateWakeup;
        }

        public boolean shouldAnimateScreenOff() {
            return this.mAnimateScreenOff;
        }
    }

    /* renamed from: com.android.systemui.statusbar.phone.StatusBar$H */
    protected class C1543H extends Handler {
        protected C1543H() {
        }

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1026) {
                StatusBar.this.toggleKeyboardShortcuts(message.arg1);
            } else if (i != 1027) {
                switch (i) {
                    case 1000:
                        StatusBar.this.animateExpandNotificationsPanel();
                        return;
                    case 1001:
                        StatusBar.this.animateCollapsePanels();
                        return;
                    case 1002:
                        StatusBar.this.animateExpandSettingsPanel((String) message.obj);
                        return;
                    case 1003:
                        StatusBar.this.onLaunchTransitionTimeout();
                        return;
                    default:
                        return;
                }
            } else {
                StatusBar.this.dismissKeyboardShortcuts();
            }
        }
    }

    public interface StatusBarInjector {
        void createStatusBar(StatusBar statusBar);
    }

    private int barMode(int i) {
        if ((67108864 & i) != 0) {
            return 1;
        }
        if ((1073741824 & i) != 0) {
            return 2;
        }
        if ((i & 9) == 9) {
            return 6;
        }
        if ((i & 8) != 0) {
            return 4;
        }
        return (i & 1) != 0 ? 3 : 0;
    }

    /* JADX WARNING: Incorrect type for immutable var: ssa=boolean, code=int, for r1v0, types: [boolean, int] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=boolean, code=int, for r2v0, types: [boolean, int] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=boolean, code=int, for r3v0, types: [boolean, int] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=boolean, code=int, for r4v0, types: [boolean, int] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=boolean, code=int, for r5v0, types: [boolean, int] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int getLoggingFingerprint(int r0, int r1, int r2, int r3, int r4, int r5) {
        /*
            r0 = r0 & 255(0xff, float:3.57E-43)
            int r1 = r1 << 8
            r0 = r0 | r1
            int r1 = r2 << 9
            r0 = r0 | r1
            int r1 = r3 << 10
            r0 = r0 | r1
            int r1 = r4 << 11
            r0 = r0 | r1
            int r1 = r5 << 12
            r0 = r0 | r1
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.StatusBar.getLoggingFingerprint(int, boolean, boolean, boolean, boolean, boolean):int");
    }

    public void onHeadsUpUnPinned(NotificationEntry notificationEntry) {
    }

    static {
        boolean z;
        try {
            z = Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
        } catch (RemoteException unused) {
            z = false;
        }
        ONLY_CORE_APPS = z;
    }

    public /* synthetic */ void lambda$new$0$StatusBar(View view) {
        if (this.mState == 1) {
            wakeUpIfDozing(SystemClock.uptimeMillis(), view, "SHADE_CLICK");
            goToLockedShade(null);
        }
        onNotificationShelfClicked();
    }

    public /* synthetic */ void lambda$new$1$StatusBar(boolean z, String str) {
        this.mEntryManager.updateNotifications();
        updateScrimController();
    }

    public void onActiveStateChanged(int i, int i2, String str, boolean z) {
        Handler handler = (Handler) Dependency.get(Dependency.MAIN_HANDLER);
        $$Lambda$StatusBar$1N2jdpaP82HJRT31BJo2G2gJK5c r1 = new Runnable(i, i2, str, z) {
            private final /* synthetic */ int f$1;
            private final /* synthetic */ int f$2;
            private final /* synthetic */ String f$3;
            private final /* synthetic */ boolean f$4;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
                this.f$4 = r5;
            }

            public final void run() {
                StatusBar.this.lambda$onActiveStateChanged$2$StatusBar(this.f$1, this.f$2, this.f$3, this.f$4);
            }
        };
        handler.post(r1);
    }

    public /* synthetic */ void lambda$onActiveStateChanged$2$StatusBar(int i, int i2, String str, boolean z) {
        this.mForegroundServiceController.onAppOpChanged(i, i2, str, z);
        this.mNotificationListController.updateNotificationsForAppOp(i, i2, str, z);
    }

    public void start() {
        RegisterStatusBarResult registerStatusBarResult;
        super.start();
        this.mGroupManager = (NotificationGroupManager) Dependency.get(NotificationGroupManager.class);
        this.mGroupAlertTransferHelper = (NotificationGroupAlertTransferHelper) Dependency.get(NotificationGroupAlertTransferHelper.class);
        this.mVisualStabilityManager = (VisualStabilityManager) Dependency.get(VisualStabilityManager.class);
        this.mNotificationLogger = (NotificationLogger) Dependency.get(NotificationLogger.class);
        this.mRemoteInputManager = (NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class);
        this.mNotificationListener = (NotificationListener) Dependency.get(NotificationListener.class);
        this.mNotificationListener.registerAsSystemService();
        this.mNetworkController = (NetworkController) Dependency.get(NetworkController.class);
        this.mUserSwitcherController = (UserSwitcherController) Dependency.get(UserSwitcherController.class);
        this.mScreenLifecycle = (ScreenLifecycle) Dependency.get(ScreenLifecycle.class);
        this.mScreenLifecycle.addObserver(this.mScreenObserver);
        this.mWakefulnessLifecycle = (WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class);
        this.mWakefulnessLifecycle.addObserver(this.mWakefulnessObserver);
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
        this.mAssistManager = (AssistManager) Dependency.get(AssistManager.class);
        this.mUiModeManager = (UiModeManager) this.mContext.getSystemService(UiModeManager.class);
        this.mLockscreenUserManager = (NotificationLockscreenUserManager) Dependency.get(NotificationLockscreenUserManager.class);
        this.mGutsManager = (NotificationGutsManager) Dependency.get(NotificationGutsManager.class);
        this.mMediaManager = (NotificationMediaManager) Dependency.get(NotificationMediaManager.class);
        this.mEntryManager = (NotificationEntryManager) Dependency.get(NotificationEntryManager.class);
        this.mNotificationInterruptionStateProvider = (NotificationInterruptionStateProvider) Dependency.get(NotificationInterruptionStateProvider.class);
        this.mViewHierarchyManager = (NotificationViewHierarchyManager) Dependency.get(NotificationViewHierarchyManager.class);
        this.mForegroundServiceController = (ForegroundServiceController) Dependency.get(ForegroundServiceController.class);
        this.mZenController = (ZenModeController) Dependency.get(ZenModeController.class);
        this.mKeyguardViewMediator = (KeyguardViewMediator) getComponent(KeyguardViewMediator.class);
        this.mColorExtractor = (SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class);
        this.mDeviceProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
        this.mNavigationBarController = (NavigationBarController) Dependency.get(NavigationBarController.class);
        this.mBubbleController = (BubbleController) Dependency.get(BubbleController.class);
        this.mBubbleController.setExpandListener(this.mBubbleExpandListener);
        this.mActivityIntentHelper = new ActivityIntentHelper(this.mContext);
        KeyguardSliceProvider attachedInstance = KeyguardSliceProvider.getAttachedInstance();
        if (attachedInstance != null) {
            attachedInstance.initDependencies(this.mMediaManager, this.mStatusBarStateController);
        } else {
            Log.w("StatusBar", "Cannot init KeyguardSliceProvider dependencies");
        }
        this.mColorExtractor.addOnColorsChangedListener(this);
        this.mStatusBarStateController.addCallback(this, 0);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.checkService("dreams"));
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDisplayId = this.mDisplay.getDisplayId();
        updateDisplaySize();
        this.mVibrateOnOpening = this.mContext.getResources().getBoolean(R$bool.config_vibrateOnIconAnimation);
        this.mVibratorHelper = (VibratorHelper) Dependency.get(VibratorHelper.class);
        DateTimeView.setReceiverHandler((Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
        putComponent(StatusBar.class, this);
        this.mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        this.mRecents = (Recents) getComponent(Recents.class);
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mCommandQueue = (CommandQueue) getComponent(CommandQueue.class);
        this.mCommandQueue.addCallback((Callbacks) this);
        try {
            registerStatusBarResult = this.mBarService.registerStatusBar(this.mCommandQueue);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            registerStatusBarResult = null;
        }
        createAndAddWindows(registerStatusBarResult);
        this.mContext.registerReceiverAsUser(this.mWallpaperChangedReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.WALLPAPER_CHANGED"), null, null);
        this.mWallpaperChangedReceiver.onReceive(this.mContext, null);
        setUpPresenter();
        setSystemUiVisibility(this.mDisplayId, registerStatusBarResult.mSystemUiVisibility, registerStatusBarResult.mFullscreenStackSysUiVisibility, registerStatusBarResult.mDockedStackSysUiVisibility, -1, registerStatusBarResult.mFullscreenStackBounds, registerStatusBarResult.mDockedStackBounds, registerStatusBarResult.mNavbarColorManagedByIme);
        setImeWindowStatus(this.mDisplayId, registerStatusBarResult.mImeToken, registerStatusBarResult.mImeWindowVis, registerStatusBarResult.mImeBackDisposition, registerStatusBarResult.mShowImeSwitcher);
        int size = registerStatusBarResult.mIcons.size();
        for (int i = 0; i < size; i++) {
            this.mCommandQueue.setIcon((String) registerStatusBarResult.mIcons.keyAt(i), (StatusBarIcon) registerStatusBarResult.mIcons.valueAt(i));
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.systemui.statusbar.banner_action_cancel");
        intentFilter.addAction("com.android.systemui.statusbar.banner_action_setup");
        this.mContext.registerReceiver(this.mBannerActionBroadcastReceiver, intentFilter, "com.android.systemui.permission.SELF", null);
        try {
            IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper")).setInAmbientMode(false, 0);
        } catch (RemoteException unused) {
        }
        this.mIconPolicy = new PhoneStatusBarPolicy(this.mContext, this.mIconController);
        this.mSignalPolicy = new StatusBarSignalPolicy(this.mContext, this.mIconController);
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(this.mContext);
        this.mUnlockMethodCache.addListener(this);
        startKeyguard();
        this.mKeyguardUpdateMonitor.registerCallback(this.mUpdateCallback);
        putComponent(DozeHost.class, this.mDozeServiceHost);
        this.mScreenPinningRequest = new ScreenPinningRequest(this.mContext);
        this.mFalsingManager = FalsingManagerFactory.getInstance(this.mContext);
        ((ActivityStarterDelegate) Dependency.get(ActivityStarterDelegate.class)).setActivityStarterImpl(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        ((InitController) Dependency.get(InitController.class)).addPostInitTask(new Runnable() {
            public final void run() {
                StatusBar.this.updateAreThereNotifications();
            }
        });
        ((InitController) Dependency.get(InitController.class)).addPostInitTask(new Runnable(registerStatusBarResult.mDisabledFlags1, registerStatusBarResult.mDisabledFlags2) {
            private final /* synthetic */ int f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                StatusBar.this.lambda$start$3$StatusBar(this.f$1, this.f$2);
            }
        });
        this.mNavigationBarGuide = new NavigationBarGuide(this.mContext, this);
        this.mOpNotificationController = (OpNotificationController) Dependency.get(OpNotificationController.class);
        OpNotificationController opNotificationController = this.mOpNotificationController;
        if (opNotificationController != null) {
            opNotificationController.setEntryManager(this.mEntryManager);
        }
        ((HighlightHintController) Dependency.get(HighlightHintController.class)).addCallback(this);
        startInner();
    }

    /* access modifiers changed from: protected */
    public void makeStatusBarView(RegisterStatusBarResult registerStatusBarResult) {
        Context context = this.mContext;
        updateDisplaySize();
        updateResources();
        updateTheme();
        inflateStatusBarWindow(context);
        inflateOPAodView(context);
        this.mStatusBarWindow.setService(this);
        this.mStatusBarWindow.setOnTouchListener(getStatusBarWindowTouchListener());
        this.mNotificationPanel = (NotificationPanelView) this.mStatusBarWindow.findViewById(R$id.notification_panel);
        this.mStackScroller = (NotificationStackScrollLayout) this.mStatusBarWindow.findViewById(R$id.notification_stack_scroller);
        this.mZenController.addCallback(this);
        this.mNotificationLogger.setUpWithContainer(this.mStackScroller);
        this.mNotificationIconAreaController = SystemUIFactory.getInstance().createNotificationIconAreaController(context, this, this.mStatusBarStateController);
        inflateShelf();
        this.mNotificationIconAreaController.setupShelf(this.mNotificationShelf);
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver((DarkReceiver) this.mNotificationIconAreaController);
        ((PluginDependencyProvider) Dependency.get(PluginDependencyProvider.class)).allowPluginDependency(DarkIconDispatcher.class);
        ((PluginDependencyProvider) Dependency.get(PluginDependencyProvider.class)).allowPluginDependency(StatusBarStateController.class);
        FragmentHostManager fragmentHostManager = FragmentHostManager.get(this.mStatusBarWindow);
        String str = "CollapsedStatusBarFragment";
        fragmentHostManager.addTagListener(str, new FragmentListener() {
            public final void onFragmentViewCreated(String str, Fragment fragment) {
                StatusBar.this.lambda$makeStatusBarView$4$StatusBar(str, fragment);
            }
        });
        fragmentHostManager.getFragmentManager().beginTransaction().replace(R$id.status_bar_container, new CollapsedStatusBarFragment(), str).commit();
        this.mIconController = (StatusBarIconController) Dependency.get(StatusBarIconController.class);
        HeadsUpManagerPhone headsUpManagerPhone = new HeadsUpManagerPhone(context, this.mStatusBarWindow, this.mGroupManager, this, this.mVisualStabilityManager);
        this.mHeadsUpManager = headsUpManagerPhone;
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this.mHeadsUpManager);
        this.mHeadsUpManager.addListener(this);
        this.mHeadsUpManager.addListener(this.mNotificationPanel);
        this.mHeadsUpManager.addListener(this.mGroupManager);
        this.mHeadsUpManager.addListener(this.mGroupAlertTransferHelper);
        this.mHeadsUpManager.addListener(this.mVisualStabilityManager);
        this.mAmbientPulseManager.addListener(this);
        this.mAmbientPulseManager.addListener(this.mGroupManager);
        this.mAmbientPulseManager.addListener(this.mGroupAlertTransferHelper);
        this.mNotificationPanel.setHeadsUpManager(this.mHeadsUpManager);
        this.mGroupManager.setHeadsUpManager(this.mHeadsUpManager);
        this.mGroupAlertTransferHelper.setHeadsUpManager(this.mHeadsUpManager);
        this.mNotificationLogger.setHeadsUpManager(this.mHeadsUpManager);
        putComponent(HeadsUpManager.class, this.mHeadsUpManager);
        createNavigationBar(registerStatusBarResult);
        this.mLockscreenWallpaper = new LockscreenWallpaper(this.mContext, this, this.mHandler);
        this.mKeyguardIndicationController = SystemUIFactory.getInstance().createKeyguardIndicationController(this.mContext, (ViewGroup) this.mStatusBarWindow.findViewById(R$id.keyguard_indication_area), (LockIcon) this.mStatusBarWindow.findViewById(R$id.lock_icon), (KeyguardStatusView) getPanel().findViewById(R$id.keyguard_status_view), getKeyguardBottomAreaView());
        this.mNotificationPanel.setKeyguardIndicationController(this.mKeyguardIndicationController);
        OpFacelockController opFacelockController = this.mOpFacelockController;
        if (opFacelockController != null) {
            opFacelockController.setKeyguardIndicationController(this.mKeyguardIndicationController);
        }
        this.mAmbientIndicationContainer = this.mStatusBarWindow.findViewById(R$id.ambient_indication_container);
        this.mBatteryController.addCallback(new BatteryStateChangeCallback() {
            public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
            }

            public void onPowerSaveChanged(boolean z) {
                StatusBar statusBar = StatusBar.this;
                statusBar.mHandler.post(statusBar.mCheckBarModes);
                DozeServiceHost dozeServiceHost = StatusBar.this.mDozeServiceHost;
                if (dozeServiceHost != null) {
                    dozeServiceHost.firePowerSaveChanged(z);
                }
            }
        });
        this.mAutoHideController = (AutoHideController) Dependency.get(AutoHideController.class);
        this.mAutoHideController.setStatusBar(this);
        this.mLightBarController = (LightBarController) Dependency.get(LightBarController.class);
        this.mScrimController = SystemUIFactory.getInstance().createScrimController((ScrimView) this.mStatusBarWindow.findViewById(R$id.scrim_behind), (ScrimView) this.mStatusBarWindow.findViewById(R$id.scrim_in_front), this.mLockscreenWallpaper, new TriConsumer() {
            public final void accept(Object obj, Object obj2, Object obj3) {
                StatusBar.this.lambda$makeStatusBarView$5$StatusBar((ScrimState) obj, (Float) obj2, (GradientColors) obj3);
            }
        }, new Consumer() {
            public final void accept(Object obj) {
                StatusBar.this.lambda$makeStatusBarView$6$StatusBar((Integer) obj);
            }
        }, DozeParameters.getInstance(this.mContext), (AlarmManager) this.mContext.getSystemService(AlarmManager.class));
        this.mNotificationPanel.initDependencies(this, this.mGroupManager, this.mNotificationShelf, this.mHeadsUpManager, this.mNotificationIconAreaController, this.mScrimController);
        this.mDozeScrimController = new DozeScrimController(context, DozeParameters.getInstance(context));
        this.mAodWindowManager = new OpAodWindowManager(this.mContext, this.mOPAodWindow);
        this.mAodDisplayViewManager = new OpAodDisplayViewManager(this.mContext, this.mOPAodWindow, this.mDozeServiceHost, this);
        this.mNotificationIconAreaController.setAodIconController(this.mAodDisplayViewManager.getAodNotificationIconCtrl());
        BackDropView backDropView = (BackDropView) this.mStatusBarWindow.findViewById(R$id.backdrop);
        this.mMediaManager.setup(backDropView, (ImageView) backDropView.findViewById(R$id.backdrop_front), (ImageView) backDropView.findViewById(R$id.backdrop_back), this.mScrimController, this.mLockscreenWallpaper);
        this.mVolumeComponent = (VolumeComponent) getComponent(VolumeComponent.class);
        this.mNotificationPanel.setUserSetupComplete(this.mUserSetup);
        if (UserManager.get(this.mContext).isUserSwitcherEnabled()) {
            createUserSwitcher();
        }
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        StatusBarWindowView statusBarWindowView = this.mStatusBarWindow;
        Objects.requireNonNull(statusBarWindowView);
        notificationPanelView.setLaunchAffordanceListener(new Consumer() {
            public final void accept(Object obj) {
                StatusBarWindowView.this.onShowingLaunchAffordanceChanged(((Boolean) obj).booleanValue());
            }
        });
        View findViewById = this.mStatusBarWindow.findViewById(R$id.qs_frame);
        if (findViewById != null) {
            FragmentHostManager fragmentHostManager2 = FragmentHostManager.get(findViewById);
            int i = R$id.qs_frame;
            ExtensionBuilder newExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(C0959QS.class);
            newExtension.withPlugin(C0959QS.class);
            newExtension.withDefault(new Supplier() {
                public final Object get() {
                    return StatusBar.this.createDefaultQSFragment();
                }
            });
            Extension build = newExtension.build();
            String str2 = C0959QS.TAG;
            ExtensionFragmentListener.attachExtensonToFragment(findViewById, str2, i, build);
            this.mBrightnessMirrorController = new BrightnessMirrorController(this.mStatusBarWindow, new Consumer() {
                public final void accept(Object obj) {
                    StatusBar.this.lambda$makeStatusBarView$7$StatusBar((Boolean) obj);
                }
            });
            fragmentHostManager2.addTagListener(str2, new FragmentListener() {
                public final void onFragmentViewCreated(String str, Fragment fragment) {
                    StatusBar.this.lambda$makeStatusBarView$8$StatusBar(str, fragment);
                }
            });
        }
        this.mReportRejectedTouch = this.mStatusBarWindow.findViewById(R$id.report_rejected_touch);
        if (this.mReportRejectedTouch != null) {
            updateReportRejectedTouchVisibility();
            this.mReportRejectedTouch.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    StatusBar.this.lambda$makeStatusBarView$9$StatusBar(view);
                }
            });
        }
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        String str3 = "android.intent.action.SCREEN_OFF";
        if (!powerManager.isScreenOn()) {
            this.mBroadcastReceiver.onReceive(this.mContext, new Intent(str3));
        }
        this.mGestureWakeLock = powerManager.newWakeLock(10, "GestureWakeLock");
        this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        int[] intArray = this.mContext.getResources().getIntArray(R$array.config_cameraLaunchGestureVibePattern);
        this.mCameraLaunchGestureVibePattern = new long[intArray.length];
        for (int i2 = 0; i2 < intArray.length; i2++) {
            this.mCameraLaunchGestureVibePattern[i2] = (long) intArray[i2];
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        intentFilter.addAction(str3);
        intentFilter.addAction("android.app.action.SHOW_DEVICE_MONITORING_DIALOG");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.android.systemui.demo");
        context.registerReceiverAsUser(this.mDemoReceiver, UserHandle.ALL, intentFilter2, "android.permission.DUMP", null);
        this.mDeviceProvisionedController.addCallback(this.mUserSetupObserver);
        this.mUserSetupObserver.onUserSetupChanged();
        ThreadedRenderer.overrideProperty("disableProfileBars", "true");
        ThreadedRenderer.overrideProperty("ambientRatio", String.valueOf(1.5f));
        super.makeStatusBarView(context);
        OpLsState.getInstance().init(this.mContext, this.mStatusBarWindow, this, this.mCommandQueue);
    }

    public /* synthetic */ void lambda$makeStatusBarView$4$StatusBar(String str, Fragment fragment) {
        ((CollapsedStatusBarFragment) fragment).initNotificationIconArea(this.mNotificationIconAreaController);
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        this.mStatusBarView = (PhoneStatusBarView) fragment.getView();
        this.mStatusBarView.setBar(this);
        this.mStatusBarView.setPanel(this.mNotificationPanel);
        this.mStatusBarView.setScrimController(this.mScrimController);
        if (this.mHeadsUpManager.hasPinnedHeadsUp()) {
            this.mNotificationPanel.notifyBarPanelExpansionChanged();
        }
        this.mStatusBarView.setBouncerShowing(this.mBouncerShowing);
        if (phoneStatusBarView != null) {
            this.mStatusBarView.panelExpansionChanged(phoneStatusBarView.getExpansionFraction(), phoneStatusBarView.isExpanded());
        }
        HeadsUpAppearanceController headsUpAppearanceController = this.mHeadsUpAppearanceController;
        if (headsUpAppearanceController != null) {
            headsUpAppearanceController.destroy();
        }
        this.mHeadsUpAppearanceController = new HeadsUpAppearanceController(this.mNotificationIconAreaController, this.mHeadsUpManager, this.mStatusBarWindow);
        this.mHeadsUpAppearanceController.readFrom(headsUpAppearanceController);
        this.mStatusBarWindow.setStatusBarView(this.mStatusBarView);
        updateAreThereNotifications();
        checkBarModes();
    }

    public /* synthetic */ void lambda$makeStatusBarView$5$StatusBar(ScrimState scrimState, Float f, GradientColors gradientColors) {
        this.mLightBarController.setScrimState(scrimState, f.floatValue(), gradientColors);
    }

    public /* synthetic */ void lambda$makeStatusBarView$6$StatusBar(Integer num) {
        StatusBarWindowController statusBarWindowController = this.mStatusBarWindowController;
        if (statusBarWindowController != null) {
            statusBarWindowController.setScrimsVisibility(num.intValue());
        }
        StatusBarWindowView statusBarWindowView = this.mStatusBarWindow;
        if (statusBarWindowView != null) {
            statusBarWindowView.onScrimVisibilityChanged(num.intValue());
        }
    }

    public /* synthetic */ void lambda$makeStatusBarView$7$StatusBar(Boolean bool) {
        this.mBrightnessMirrorVisible = bool.booleanValue();
        updateScrimController();
    }

    public /* synthetic */ void lambda$makeStatusBarView$8$StatusBar(String str, Fragment fragment) {
        C0959QS qs = (C0959QS) fragment;
        if (qs instanceof QSFragment) {
            this.mQSPanel = ((QSFragment) qs).getQsPanel();
            this.mQSPanel.setBrightnessMirror(this.mBrightnessMirrorController);
        }
    }

    public /* synthetic */ void lambda$makeStatusBarView$9$StatusBar(View view) {
        Uri reportRejectedTouch = this.mFalsingManager.reportRejectedTouch();
        if (reportRejectedTouch != null) {
            StringWriter stringWriter = new StringWriter();
            stringWriter.write("Build info: ");
            stringWriter.write(SystemProperties.get("ro.build.description"));
            stringWriter.write("\nSerial number: ");
            stringWriter.write(SystemProperties.get("ro.serialno"));
            stringWriter.write("\n");
            PrintWriter printWriter = new PrintWriter(stringWriter);
            FalsingLog.dump(printWriter);
            printWriter.flush();
            startActivityDismissingKeyguard(Intent.createChooser(new Intent("android.intent.action.SEND").setType("*/*").putExtra("android.intent.extra.SUBJECT", "Rejected touch report").putExtra("android.intent.extra.STREAM", reportRejectedTouch).putExtra("android.intent.extra.TEXT", stringWriter.toString()), "Share rejected touch report").addFlags(268435456), true, true);
        }
    }

    /* access modifiers changed from: protected */
    public C0959QS createDefaultQSFragment() {
        return (C0959QS) FragmentHostManager.get(this.mStatusBarWindow).create(QSFragment.class);
    }

    private void setUpPresenter() {
        this.mActivityLaunchAnimator = new ActivityLaunchAnimator(this.mStatusBarWindow, this, this.mNotificationPanel, this.mStackScroller);
        NotificationRowBinderImpl notificationRowBinderImpl = new NotificationRowBinderImpl(this.mContext, SystemUIFactory.getInstance().provideAllowNotificationLongPress());
        StatusBarNotificationPresenter statusBarNotificationPresenter = new StatusBarNotificationPresenter(this.mContext, this.mNotificationPanel, this.mHeadsUpManager, this.mStatusBarWindow, this.mStackScroller, this.mDozeScrimController, this.mScrimController, this.mActivityLaunchAnimator, this.mStatusBarKeyguardViewManager, this.mNotificationAlertingManager, notificationRowBinderImpl);
        this.mPresenter = statusBarNotificationPresenter;
        this.mNotificationListController = new NotificationListController(this.mEntryManager, this.mStackScroller, this.mForegroundServiceController, this.mDeviceProvisionedController);
        this.mNotificationShelf.setOnActivatedListener(this.mPresenter);
        this.mRemoteInputManager.getController().addCallback(this.mStatusBarWindowController);
        StatusBarRemoteInputCallback statusBarRemoteInputCallback = (StatusBarRemoteInputCallback) Dependency.get(NotificationRemoteInputManager.Callback.class);
        this.mShadeController = (ShadeController) Dependency.get(ShadeController.class);
        ActivityStarter activityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        Context context = this.mContext;
        Context context2 = context;
        CommandQueue commandQueue = this.mCommandQueue;
        AssistManager assistManager = this.mAssistManager;
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        NotificationPresenter notificationPresenter = this.mPresenter;
        NotificationEntryManager notificationEntryManager = this.mEntryManager;
        HeadsUpManagerPhone headsUpManagerPhone = this.mHeadsUpManager;
        ActivityLaunchAnimator activityLaunchAnimator = this.mActivityLaunchAnimator;
        IStatusBarService iStatusBarService = this.mBarService;
        SysuiStatusBarStateController sysuiStatusBarStateController = this.mStatusBarStateController;
        Context context3 = context;
        KeyguardManager keyguardManager = this.mKeyguardManager;
        NotificationRowBinderImpl notificationRowBinderImpl2 = notificationRowBinderImpl;
        Context context4 = context3;
        StatusBarNotificationActivityStarter statusBarNotificationActivityStarter = r3;
        IDreamManager iDreamManager = this.mDreamManager;
        NotificationRemoteInputManager notificationRemoteInputManager = this.mRemoteInputManager;
        NotificationGroupManager notificationGroupManager = this.mGroupManager;
        NotificationLockscreenUserManager notificationLockscreenUserManager = this.mLockscreenUserManager;
        ShadeController shadeController = this.mShadeController;
        KeyguardMonitor keyguardMonitor = this.mKeyguardMonitor;
        NotificationInterruptionStateProvider notificationInterruptionStateProvider = this.mNotificationInterruptionStateProvider;
        MetricsLogger metricsLogger = this.mMetricsLogger;
        LockPatternUtils lockPatternUtils = r2;
        LockPatternUtils lockPatternUtils2 = new LockPatternUtils(context4);
        StatusBarNotificationActivityStarter statusBarNotificationActivityStarter2 = new StatusBarNotificationActivityStarter(context2, commandQueue, assistManager, notificationPanelView, notificationPresenter, notificationEntryManager, headsUpManagerPhone, activityStarter, activityLaunchAnimator, iStatusBarService, sysuiStatusBarStateController, keyguardManager, iDreamManager, notificationRemoteInputManager, statusBarRemoteInputCallback, notificationGroupManager, notificationLockscreenUserManager, shadeController, keyguardMonitor, notificationInterruptionStateProvider, metricsLogger, lockPatternUtils, (Handler) Dependency.get(Dependency.MAIN_HANDLER), (Handler) Dependency.get(Dependency.BG_HANDLER), this.mActivityIntentHelper, this.mBubbleController);
        this.mNotificationActivityStarter = statusBarNotificationActivityStarter;
        this.mGutsManager.setNotificationActivityStarter(this.mNotificationActivityStarter);
        NotificationRowBinderImpl notificationRowBinderImpl3 = notificationRowBinderImpl2;
        this.mEntryManager.setRowBinder(notificationRowBinderImpl3);
        notificationRowBinderImpl3.setNotificationClicker(new NotificationClicker(this, (BubbleController) Dependency.get(BubbleController.class), this.mNotificationActivityStarter));
        this.mGroupAlertTransferHelper.bind(this.mEntryManager, this.mGroupManager);
        this.mNotificationListController.bind();
    }

    /* access modifiers changed from: protected */
    /* renamed from: setUpDisableFlags */
    public void lambda$start$3$StatusBar(int i, int i2) {
        this.mCommandQueue.disable(this.mDisplayId, i, i2, false);
    }

    public void addAfterKeyguardGoneRunnable(Runnable runnable) {
        this.mStatusBarKeyguardViewManager.addAfterKeyguardGoneRunnable(runnable);
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    public void wakeUpIfDozing(long j, View view, String str) {
        if (this.mDozing) {
            PowerManager powerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
            StringBuilder sb = new StringBuilder();
            sb.append("com.android.systemui:");
            sb.append(str);
            powerManager.wakeUp(j, 4, sb.toString());
            this.mWakeUpComingFromTouch = true;
            view.getLocationInWindow(this.mTmpInt2);
            this.mWakeUpTouchLocation = new PointF((float) (this.mTmpInt2[0] + (view.getWidth() / 2)), (float) (this.mTmpInt2[1] + (view.getHeight() / 2)));
            this.mFalsingManager.onScreenOnFromTouch();
        }
    }

    public boolean isDozingCustom() {
        return this.mCustomDozing;
    }

    /* access modifiers changed from: protected */
    public void createNavigationBar(RegisterStatusBarResult registerStatusBarResult) {
        this.mNavigationBarController.createNavigationBars(true, registerStatusBarResult);
    }

    /* access modifiers changed from: protected */
    public OnTouchListener getStatusBarWindowTouchListener() {
        return new OnTouchListener() {
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return StatusBar.this.lambda$getStatusBarWindowTouchListener$10$StatusBar(view, motionEvent);
            }
        };
    }

    public /* synthetic */ boolean lambda$getStatusBarWindowTouchListener$10$StatusBar(View view, MotionEvent motionEvent) {
        this.mAutoHideController.checkUserAutoHide(motionEvent);
        this.mRemoteInputManager.checkRemoteInputOutside(motionEvent);
        if (motionEvent.getAction() == 0 && this.mExpandedVisible && !OpLsState.getInstance().getPreventModeCtrl().isPreventModeActive()) {
            animateCollapsePanels();
        }
        return this.mStatusBarWindow.onTouchEvent(motionEvent);
    }

    private void inflateShelf() {
        this.mNotificationShelf = (NotificationShelf) LayoutInflater.from(this.mContext).inflate(R$layout.status_bar_notification_shelf, this.mStackScroller, false);
        this.mNotificationShelf.setOnClickListener(this.mGoToLockedShadeListener);
    }

    public void onDensityOrFontScaleChanged() {
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.onDensityOrFontScaleChanged();
        }
        ((UserInfoControllerImpl) Dependency.get(UserInfoController.class)).onDensityOrFontScaleChanged();
        ((UserSwitcherController) Dependency.get(UserSwitcherController.class)).onDensityOrFontScaleChanged();
        KeyguardUserSwitcher keyguardUserSwitcher = this.mKeyguardUserSwitcher;
        if (keyguardUserSwitcher != null) {
            keyguardUserSwitcher.onDensityOrFontScaleChanged();
        }
        this.mNotificationIconAreaController.onDensityOrFontScaleChanged(this.mContext);
        this.mHeadsUpManager.onDensityOrFontScaleChanged();
        opOnDensityOrFontScaleChanged();
    }

    public void onThemeChanged() {
        StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
        if (statusBarKeyguardViewManager != null) {
            statusBarKeyguardViewManager.onThemeChanged();
        }
        View view = this.mAmbientIndicationContainer;
        if (view instanceof AutoReinflateContainer) {
            ((AutoReinflateContainer) view).inflateLayout();
        }
        OpFacelockController opFacelockController = this.mOpFacelockController;
        if (opFacelockController != null) {
            opFacelockController.setKeyguardIndicationController(this.mKeyguardIndicationController);
        }
    }

    public void onOverlayChanged() {
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.onOverlayChanged();
        }
        this.mNotificationPanel.onThemeChanged();
        onThemeChanged();
    }

    public void onUiModeChanged() {
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.onUiModeChanged();
        }
        opOnUiModeChanged();
    }

    /* access modifiers changed from: protected */
    public void createUserSwitcher() {
        this.mKeyguardUserSwitcher = new KeyguardUserSwitcher(this.mContext, (ViewStub) this.mStatusBarWindow.findViewById(R$id.keyguard_user_switcher), (KeyguardStatusBarView) this.mStatusBarWindow.findViewById(R$id.keyguard_header), this.mNotificationPanel);
    }

    /* access modifiers changed from: protected */
    public void inflateStatusBarWindow(Context context) {
        this.mStatusBarWindow = (StatusBarWindowView) this.mInjectionInflater.injectable(LayoutInflater.from(context)).inflate(R$layout.super_status_bar, null);
    }

    /* access modifiers changed from: protected */
    public void startKeyguard() {
        Trace.beginSection("StatusBar#startKeyguard");
        KeyguardViewMediator keyguardViewMediator = (KeyguardViewMediator) getComponent(KeyguardViewMediator.class);
        Context context = this.mContext;
        BiometricUnlockController biometricUnlockController = new BiometricUnlockController(context, this.mDozeScrimController, keyguardViewMediator, this.mScrimController, this, UnlockMethodCache.getInstance(context), new Handler(), this.mKeyguardUpdateMonitor, (TunerService) Dependency.get(TunerService.class));
        this.mBiometricUnlockController = biometricUnlockController;
        putComponent(BiometricUnlockController.class, this.mBiometricUnlockController);
        this.mStatusBarKeyguardViewManager = keyguardViewMediator.registerStatusBar(this, getBouncerContainer(), this.mNotificationPanel, this.mBiometricUnlockController, (ViewGroup) this.mStatusBarWindow.findViewById(R$id.lock_icon_container));
        this.mKeyguardIndicationController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mBiometricUnlockController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        this.mRemoteInputManager.getController().addCallback(this.mStatusBarKeyguardViewManager);
        OpLsState.getInstance().setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        ((OpChargingAnimationController) Dependency.get(OpChargingAnimationController.class)).init(keyguardViewMediator);
        this.mKeyguardViewMediatorCallback = keyguardViewMediator.getViewMediatorCallback();
        this.mLightBarController.setBiometricUnlockController(this.mBiometricUnlockController);
        this.mMediaManager.setBiometricUnlockController(this.mBiometricUnlockController);
        ((KeyguardDismissUtil) Dependency.get(KeyguardDismissUtil.class)).setDismissHandler(new KeyguardDismissHandler() {
            public final void executeWhenUnlocked(OnDismissAction onDismissAction) {
                StatusBar.this.executeWhenUnlocked(onDismissAction);
            }
        });
        OpLsState.getInstance().setBiometricUnlockController(this.mBiometricUnlockController);
        OpLsState.getInstance().setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        OpFacelockController opFacelockController = new OpFacelockController(this.mContext, this.mKeyguardViewMediator, this, this.mStatusBarKeyguardViewManager, this.mStatusBarWindowController, this.mBiometricUnlockController);
        this.mOpFacelockController = opFacelockController;
        OpFacelockController opFacelockController2 = this.mOpFacelockController;
        if (opFacelockController2 != null) {
            opFacelockController2.setKeyguardIndicationController(this.mKeyguardIndicationController);
        }
        Trace.endSection();
    }

    /* access modifiers changed from: protected */
    public View getStatusBarView() {
        return this.mStatusBarView;
    }

    public StatusBarWindowView getStatusBarWindow() {
        return this.mStatusBarWindow;
    }

    /* access modifiers changed from: protected */
    public ViewGroup getBouncerContainer() {
        return this.mStatusBarWindow;
    }

    public int getStatusBarHeight() {
        if (this.mNaturalBarHeight < 0) {
            this.mNaturalBarHeight = this.mContext.getResources().getDimensionPixelSize(17105422);
        }
        return this.mNaturalBarHeight;
    }

    /* access modifiers changed from: protected */
    public boolean toggleSplitScreenMode(int i, int i2) {
        int i3 = 0;
        if (this.mRecents == null) {
            return false;
        }
        if (WindowManagerProxy.getInstance().getDockSide() == -1) {
            int navBarPosition = WindowManagerWrapper.getInstance().getNavBarPosition(this.mDisplayId);
            if (navBarPosition == -1) {
                return false;
            }
            if (navBarPosition == 1) {
                i3 = 1;
            }
            return this.mRecents.splitPrimaryTask(i3, null, i);
        }
        Divider divider = (Divider) getComponent(Divider.class);
        if (divider != null) {
            if (divider.isMinimized() && !divider.isHomeStackResizable()) {
                return false;
            }
            divider.onUndockingTask();
            if (i2 != -1) {
                this.mMetricsLogger.action(i2);
            }
        }
        return true;
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0026, code lost:
        if (ONLY_CORE_APPS == false) goto L_0x002a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:7:0x0015, code lost:
        if (r0.isSimpleUserSwitcher() != false) goto L_0x0029;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateQsExpansionEnabled() {
        /*
            r3 = this;
            com.android.systemui.statusbar.policy.DeviceProvisionedController r0 = r3.mDeviceProvisionedController
            boolean r0 = r0.isDeviceProvisioned()
            r1 = 1
            if (r0 == 0) goto L_0x0029
            boolean r0 = r3.mUserSetup
            if (r0 != 0) goto L_0x0017
            com.android.systemui.statusbar.policy.UserSwitcherController r0 = r3.mUserSwitcherController
            if (r0 == 0) goto L_0x0017
            boolean r0 = r0.isSimpleUserSwitcher()
            if (r0 != 0) goto L_0x0029
        L_0x0017:
            int r0 = r3.mDisabled2
            r2 = r0 & 4
            if (r2 != 0) goto L_0x0029
            r0 = r0 & r1
            if (r0 != 0) goto L_0x0029
            boolean r0 = r3.mDozing
            if (r0 != 0) goto L_0x0029
            boolean r0 = ONLY_CORE_APPS
            if (r0 != 0) goto L_0x0029
            goto L_0x002a
        L_0x0029:
            r1 = 0
        L_0x002a:
            com.android.systemui.statusbar.phone.NotificationPanelView r3 = r3.mNotificationPanel
            r3.setQsExpansionEnabled(r1)
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r0 = "updateQsExpansionEnabled - QS Expand enabled: "
            r3.append(r0)
            r3.append(r1)
            java.lang.String r3 = r3.toString()
            java.lang.String r0 = "StatusBar"
            android.util.Log.d(r0, r3)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.StatusBar.updateQsExpansionEnabled():void");
    }

    public void addQsTile(ComponentName componentName) {
        QSPanel qSPanel = this.mQSPanel;
        if (qSPanel != null && qSPanel.getHost() != null) {
            this.mQSPanel.getHost().addTile(componentName);
        }
    }

    public void remQsTile(ComponentName componentName) {
        QSPanel qSPanel = this.mQSPanel;
        if (qSPanel != null && qSPanel.getHost() != null) {
            this.mQSPanel.getHost().removeTile(componentName);
        }
    }

    public void clickTile(ComponentName componentName) {
        this.mQSPanel.clickTile(componentName);
    }

    public boolean areNotificationsHidden() {
        return this.mZenController.areNotificationsHiddenInShade();
    }

    public void requestNotificationUpdate() {
        this.mEntryManager.updateNotifications();
    }

    public void requestFaceAuth() {
        if (!this.mUnlockMethodCache.canSkipBouncer()) {
            this.mKeyguardUpdateMonitor.requestFaceAuth();
        }
    }

    public void updateAreThereNotifications() {
        AnimatorListener animatorListener;
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            final View findViewById = phoneStatusBarView.findViewById(R$id.notification_lights_out);
            boolean z = true;
            boolean z2 = hasActiveNotifications() && !areLightsOn();
            if (findViewById.getAlpha() != 1.0f) {
                z = false;
            }
            if (z2 != z) {
                float f = 0.0f;
                if (z2) {
                    findViewById.setAlpha(0.0f);
                    findViewById.setVisibility(0);
                }
                ViewPropertyAnimator animate = findViewById.animate();
                if (z2) {
                    f = 1.0f;
                }
                ViewPropertyAnimator interpolator = animate.alpha(f).setDuration(z2 ? 750 : 250).setInterpolator(new AccelerateInterpolator(2.0f));
                if (z2) {
                    animatorListener = null;
                } else {
                    animatorListener = new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animator) {
                            findViewById.setVisibility(8);
                        }
                    };
                }
                interpolator.setListener(animatorListener).start();
            }
        }
        this.mMediaManager.findAndUpdateMediaNotifications();
    }

    private void updateReportRejectedTouchVisibility() {
        View view = this.mReportRejectedTouch;
        if (view != null) {
            view.setVisibility((this.mState != 1 || this.mDozing || !this.mFalsingManager.isReportingEnabled()) ? 4 : 0);
        }
    }

    public void disable(int i, int i2, int i3, boolean z) {
        int i4 = i2;
        if (i == this.mDisplayId) {
            int adjustDisableFlags = this.mRemoteInputQuickSettingsDisabler.adjustDisableFlags(i3);
            int i5 = this.mStatusBarWindowState;
            int i6 = this.mDisabled1 ^ i4;
            this.mDisabled1 = i4;
            int i7 = this.mDisabled2 ^ adjustDisableFlags;
            this.mDisabled2 = adjustDisableFlags;
            StringBuilder sb = new StringBuilder();
            sb.append("disable<");
            int i8 = i4 & 65536;
            sb.append(i8 != 0 ? 'E' : 'e');
            int i9 = 65536 & i6;
            char c = ' ';
            sb.append(i9 != 0 ? '!' : ' ');
            char c2 = 'I';
            sb.append((i4 & 131072) != 0 ? 'I' : 'i');
            sb.append((131072 & i6) != 0 ? '!' : ' ');
            int i10 = i4 & 262144;
            sb.append(i10 != 0 ? 'A' : 'a');
            int i11 = 262144 & i6;
            sb.append(i11 != 0 ? '!' : ' ');
            sb.append((i4 & 1048576) != 0 ? 'S' : 's');
            sb.append((1048576 & i6) != 0 ? '!' : ' ');
            sb.append((i4 & 4194304) != 0 ? 'B' : 'b');
            sb.append((4194304 & i6) != 0 ? '!' : ' ');
            sb.append((i4 & 2097152) != 0 ? 'H' : 'h');
            sb.append((2097152 & i6) != 0 ? '!' : ' ');
            int i12 = 16777216 & i4;
            sb.append(i12 != 0 ? 'R' : 'r');
            int i13 = 16777216 & i6;
            sb.append(i13 != 0 ? '!' : ' ');
            sb.append((8388608 & i4) != 0 ? 'C' : 'c');
            sb.append((8388608 & i6) != 0 ? '!' : ' ');
            sb.append((33554432 & i4) != 0 ? 'S' : 's');
            sb.append((i6 & 33554432) != 0 ? '!' : ' ');
            sb.append("> disable2<");
            sb.append((adjustDisableFlags & 1) != 0 ? 'Q' : 'q');
            int i14 = i7 & 1;
            sb.append(i14 != 0 ? '!' : ' ');
            if ((adjustDisableFlags & 2) == 0) {
                c2 = 'i';
            }
            sb.append(c2);
            sb.append((i7 & 2) != 0 ? '!' : ' ');
            sb.append((adjustDisableFlags & 4) != 0 ? 'N' : 'n');
            int i15 = i7 & 4;
            if (i15 != 0) {
                c = '!';
            }
            sb.append(c);
            writeDisableFlagdbg(adjustDisableFlags, i7, sb);
            sb.append('>');
            Log.d("StatusBar", sb.toString());
            if (!(i9 == 0 || i8 == 0)) {
                animateCollapsePanels();
            }
            if (!(i13 == 0 || i12 == 0)) {
                this.mHandler.removeMessages(1020);
                this.mHandler.sendEmptyMessage(1020);
            }
            if (i11 != 0) {
                this.mNotificationInterruptionStateProvider.setDisableNotificationAlerts(i10 != 0);
            }
            if (i14 != 0) {
                updateQsExpansionEnabled();
            }
            if (i15 != 0) {
                updateQsExpansionEnabled();
                if ((i4 & 4) != 0) {
                    animateCollapsePanels();
                }
            }
            disable(adjustDisableFlags, i7);
            ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).updateSystemUIStateFlagsInternal();
        }
    }

    /* access modifiers changed from: protected */
    public C1543H createHandler() {
        return new C1543H();
    }

    public void startActivity(Intent intent, boolean z, boolean z2, int i) {
        startActivityDismissingKeyguard(intent, z, z2, i);
    }

    public void startActivity(Intent intent, boolean z) {
        startActivityDismissingKeyguard(intent, false, z);
    }

    public void startActivity(Intent intent, boolean z, boolean z2) {
        startActivityDismissingKeyguard(intent, z, z2);
    }

    public void startActivity(Intent intent, boolean z, ActivityStarter.Callback callback) {
        startActivityDismissingKeyguard(intent, false, z, false, callback, 0);
    }

    public void setQsExpanded(boolean z) {
        int i = 0;
        if (z) {
            this.mStackScroller.hideDismissAnimate(true);
        } else if (this.mStackScroller.hasActiveClearableNotifications(0)) {
            this.mStackScroller.showDismissAnimate(true);
        }
        this.mStatusBarWindowController.setQsExpanded(z);
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        if (z) {
            i = 4;
        }
        notificationPanelView.setStatusAccessibilityImportance(i);
    }

    public boolean isWakeUpComingFromTouch() {
        return this.mWakeUpComingFromTouch;
    }

    public boolean isFalsingThresholdNeeded() {
        return this.mStatusBarStateController.getState() == 1;
    }

    public void onKeyguardViewManagerStatesUpdated() {
        logStateToEventlog();
    }

    public void onUnlockMethodStateChanged() {
        updateKeyguardState();
        logStateToEventlog();
    }

    public void onHeadsUpPinnedModeChanged(boolean z) {
        if (z) {
            this.mStatusBarWindowController.setHeadsUpShowing(true);
            this.mStatusBarWindowController.setForceStatusBarVisible(true);
            if (this.mNotificationPanel.isFullyCollapsed()) {
                this.mNotificationPanel.requestLayout();
                if (!isKeyguardShowing() || (this.mIsOccluded && !this.mBouncerShowing)) {
                    this.mStatusBarWindowController.setForceWindowCollapsed(true);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onHeadsUpPinnedModeChanged: inPinnedMode= ");
                    sb.append(z);
                    sb.append("keyguard is showing. mIsOccluded= ");
                    sb.append(this.mIsOccluded);
                    sb.append(", mBouncerShowing= ");
                    sb.append(this.mBouncerShowing);
                    Log.d("StatusBar", sb.toString());
                }
                this.mNotificationPanel.post(new Runnable() {
                    public final void run() {
                        StatusBar.this.lambda$onHeadsUpPinnedModeChanged$11$StatusBar();
                    }
                });
            }
        } else {
            if (this.mOpSceneModeObserver.isInBrickMode()) {
                this.mStatusBarWindowController.setForceStatusBarVisible(false);
            }
            if (!this.mNotificationPanel.isFullyCollapsed() || this.mNotificationPanel.isTracking()) {
                this.mStatusBarWindowController.setHeadsUpShowing(false);
            } else {
                this.mHeadsUpManager.setHeadsUpGoingAway(true);
                this.mNotificationPanel.runAfterAnimationFinished(new Runnable() {
                    public final void run() {
                        StatusBar.this.lambda$onHeadsUpPinnedModeChanged$12$StatusBar();
                    }
                });
            }
        }
        ((HighlightHintController) Dependency.get(HighlightHintController.class)).onHeadUpPinnedModeChange(z);
    }

    public /* synthetic */ void lambda$onHeadsUpPinnedModeChanged$11$StatusBar() {
        this.mStatusBarWindowController.setForceWindowCollapsed(false);
    }

    public /* synthetic */ void lambda$onHeadsUpPinnedModeChanged$12$StatusBar() {
        if (!this.mHeadsUpManager.hasPinnedHeadsUp()) {
            this.mStatusBarWindowController.setHeadsUpShowing(false);
            this.mHeadsUpManager.setHeadsUpGoingAway(false);
        }
        this.mRemoteInputManager.onPanelCollapsed();
    }

    public void onHeadsUpPinned(NotificationEntry notificationEntry) {
        dismissVolumeDialog();
    }

    public void onHeadsUpStateChanged(NotificationEntry notificationEntry, boolean z) {
        this.mEntryManager.updateNotifications();
    }

    public void onAmbientStateChanged(NotificationEntry notificationEntry, boolean z) {
        this.mEntryManager.updateNotifications();
        if (z) {
            this.mDozeServiceHost.fireNotificationPulse();
        } else {
            if (!this.mAmbientPulseManager.hasNotifications()) {
            }
        }
    }

    public boolean isKeyguardCurrentlySecure() {
        return !this.mUnlockMethodCache.canSkipBouncer();
    }

    public void setPanelExpanded(boolean z) {
        this.mPanelExpanded = z;
        updateHideIconsForBouncer(false);
        this.mStatusBarWindowController.setPanelExpanded(z);
        this.mVisualStabilityManager.setPanelExpanded(z);
        if (z && this.mStatusBarStateController.getState() != 1) {
            clearNotificationEffects();
        }
        if (!z) {
            this.mRemoteInputManager.onPanelCollapsed();
        }
        if (z) {
            this.mOpNotificationController.hideSimpleHeadsUps();
        }
    }

    public NotificationStackScrollLayout getNotificationScrollLayout() {
        return this.mStackScroller;
    }

    public boolean isPulsing() {
        return this.mPulsing;
    }

    public boolean hideStatusBarIconsWhenExpanded() {
        return this.mNotificationPanel.hideStatusBarIconsWhenExpanded();
    }

    public void onColorsChanged(ColorExtractor colorExtractor, int i) {
        updateTheme();
    }

    public View getAmbientIndicationContainer() {
        return this.mAmbientIndicationContainer;
    }

    public boolean isOccluded() {
        return this.mIsOccluded;
    }

    public void setOccluded(boolean z) {
        this.mIsOccluded = z;
        this.mScrimController.setKeyguardOccluded(z);
        updateHideIconsForBouncer(false);
    }

    public boolean hideStatusBarIconsForBouncer() {
        return this.mHideIconsForBouncer || this.mWereIconsJustHidden;
    }

    private void updateHideIconsForBouncer(boolean z) {
        boolean z2 = false;
        boolean z3 = this.mTopHidesStatusBar && this.mIsOccluded && (this.mStatusBarWindowHidden || this.mBouncerShowing);
        boolean z4 = !this.mPanelExpanded && !this.mIsOccluded && this.mBouncerShowing;
        if (z3 || z4) {
            z2 = true;
        }
        if (this.mHideIconsForBouncer != z2) {
            this.mHideIconsForBouncer = z2;
            if (z2 || !this.mBouncerWasShowingWhenHidden) {
                this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, z);
            } else {
                this.mWereIconsJustHidden = true;
                this.mHandler.postDelayed(new Runnable() {
                    public final void run() {
                        StatusBar.this.lambda$updateHideIconsForBouncer$13$StatusBar();
                    }
                }, 500);
            }
        }
        if (z2) {
            this.mBouncerWasShowingWhenHidden = this.mBouncerShowing;
        }
    }

    public /* synthetic */ void lambda$updateHideIconsForBouncer$13$StatusBar() {
        this.mWereIconsJustHidden = false;
        this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, true);
    }

    public boolean isHeadsUpShouldBeVisible() {
        return this.mHeadsUpAppearanceController.shouldBeVisible();
    }

    public void onLaunchAnimationCancelled() {
        if (!this.mPresenter.isCollapsing()) {
            onClosingFinished();
        }
    }

    public void onExpandAnimationFinished(boolean z) {
        if (!this.mPresenter.isCollapsing()) {
            onClosingFinished();
        }
        if (z) {
            instantCollapseNotificationPanel();
        }
    }

    public void onExpandAnimationTimedOut() {
        if (this.mPresenter.isPresenterFullyCollapsed() && !this.mPresenter.isCollapsing()) {
            ActivityLaunchAnimator activityLaunchAnimator = this.mActivityLaunchAnimator;
            if (activityLaunchAnimator != null && !activityLaunchAnimator.isLaunchForActivity()) {
                onClosingFinished();
                return;
            }
        }
        collapsePanel(true);
    }

    public boolean areLaunchAnimationsEnabled() {
        return this.mState == 0;
    }

    public boolean isDeviceInVrMode() {
        return this.mPresenter.isDeviceInVrMode();
    }

    public NotificationPresenter getPresenter() {
        return this.mPresenter;
    }

    public void maybeEscalateHeadsUp() {
        this.mHeadsUpManager.getAllEntries().forEach(new Consumer() {
            public final void accept(Object obj) {
                StatusBar.this.lambda$maybeEscalateHeadsUp$14$StatusBar((NotificationEntry) obj);
            }
        });
        this.mHeadsUpManager.releaseAllImmediately();
    }

    public /* synthetic */ void lambda$maybeEscalateHeadsUp$14$StatusBar(NotificationEntry notificationEntry) {
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        Notification notification = statusBarNotification.getNotification();
        if (notification.fullScreenIntent != null) {
            boolean z = this.mPhoneState == 0;
            boolean isInCall = this.mTelecomManager.isInCall();
            boolean equals = "com.android.dialer".equals(statusBarNotification.getPackageName());
            StringBuilder sb = new StringBuilder();
            sb.append("maybeEscalateHeadsUp, isDialer: ");
            sb.append(equals);
            sb.append(", isCallStateIdle: ");
            sb.append(z);
            sb.append(", isInCall: ");
            sb.append(isInCall);
            String str = "StatusBar";
            Log.d(str, sb.toString());
            if (!equals || (!z && isInCall)) {
                try {
                    EventLog.writeEvent(36003, statusBarNotification.getKey());
                    notification.fullScreenIntent.send();
                    notificationEntry.notifyFullScreenIntentLaunched();
                } catch (CanceledException unused) {
                }
            } else {
                Log.d(str, "Bypass fullScreenIntent of dialer since call state is idle or not in a call");
            }
        }
    }

    public void handleSystemKey(int i) {
        if (this.mCommandQueue.panelsEnabled() && this.mKeyguardMonitor.isDeviceInteractive() && ((!this.mKeyguardMonitor.isShowing() || this.mKeyguardMonitor.isOccluded()) && this.mUserSetup)) {
            if (280 == i) {
                this.mMetricsLogger.action(493);
                this.mNotificationPanel.collapse(false, 1.0f);
            } else if (281 == i) {
                this.mMetricsLogger.action(494);
                if (this.mNotificationPanel.isFullyCollapsed()) {
                    if (this.mVibrateOnOpening) {
                        this.mVibratorHelper.vibrate(2);
                    }
                    this.mNotificationPanel.expand(true);
                    this.mMetricsLogger.count("panel_open", 1);
                } else if (!this.mNotificationPanel.isInSettings() && !this.mNotificationPanel.isExpanding()) {
                    this.mNotificationPanel.flingSettings(0.0f, 0);
                    this.mMetricsLogger.count("panel_open_qs", 1);
                }
            }
        }
    }

    public void showPinningEnterExitToast(boolean z) {
        if (getNavigationBarView() != null) {
            getNavigationBarView().showPinningEnterExitToast(z);
        }
    }

    public void showPinningEscapeToast() {
        if (getNavigationBarView() != null) {
            getNavigationBarView().showPinningEscapeToast();
        }
    }

    /* access modifiers changed from: 0000 */
    public void makeExpandedVisible(boolean z) {
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("Make expanded visible: mExpandedVisible=");
            sb.append(this.mExpandedVisible);
            sb.append(" force:");
            sb.append(z);
            sb.append(" panelsEnabled:");
            sb.append(!this.mCommandQueue.panelsEnabled());
            Log.d("StatusBar", sb.toString());
        }
        if (z || (!this.mExpandedVisible && this.mCommandQueue.panelsEnabled())) {
            this.mExpandedVisible = true;
            this.mStatusBarWindowController.setPanelVisible(true);
            visibilityChanged(true);
            this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, !z);
            setInteracting(1, true);
            ((HighlightHintController) Dependency.get(HighlightHintController.class)).onExpandedVisibleChange(true);
        }
    }

    public void animateCollapsePanels() {
        animateCollapsePanels(0);
    }

    public void postAnimateCollapsePanels() {
        this.mHandler.post(this.mAnimateCollapsePanels);
    }

    public void postAnimateForceCollapsePanels() {
        this.mHandler.post(new Runnable() {
            public final void run() {
                StatusBar.this.lambda$postAnimateForceCollapsePanels$15$StatusBar();
            }
        });
    }

    public /* synthetic */ void lambda$postAnimateForceCollapsePanels$15$StatusBar() {
        animateCollapsePanels(0, true);
    }

    public void postAnimateOpenPanels() {
        this.mHandler.sendEmptyMessage(1002);
    }

    public void togglePanel() {
        if (this.mPanelExpanded) {
            animateCollapsePanels();
        } else {
            animateExpandNotificationsPanel();
        }
    }

    public void animateCollapsePanels(int i) {
        animateCollapsePanels(i, false, false, 1.0f);
    }

    public void animateCollapsePanels(int i, boolean z) {
        animateCollapsePanels(i, z, false, 1.0f);
    }

    public void animateCollapsePanels(int i, boolean z, boolean z2) {
        animateCollapsePanels(i, z, z2, 1.0f);
    }

    public void animateCollapsePanels(int i, boolean z, boolean z2, float f) {
        if (z || this.mState == 0) {
            String str = "StatusBar";
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("animateCollapse(): mExpandedVisible=");
                sb.append(this.mExpandedVisible);
                sb.append(" flags=");
                sb.append(Integer.toHexString(i));
                Log.d(str, sb.toString());
            }
            if ((i & 2) == 0 && !this.mHandler.hasMessages(1020)) {
                this.mHandler.removeMessages(1020);
                this.mHandler.sendEmptyMessage(1020);
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("mStatusBarWindow: ");
            sb2.append(this.mStatusBarWindow);
            sb2.append(" canPanelBeCollapsed(): ");
            sb2.append(this.mNotificationPanel.canPanelBeCollapsed());
            Log.v(str, sb2.toString());
            if (this.mStatusBarWindow == null || !this.mNotificationPanel.canPanelBeCollapsed()) {
                this.mBubbleController.collapseStack();
            } else {
                this.mStatusBarWindowController.setStatusBarFocusable(false);
                this.mStatusBarWindow.cancelExpandHelper();
                this.mStatusBarView.collapsePanel(true, z2, f);
            }
            return;
        }
        runPostCollapseRunnables();
    }

    /* access modifiers changed from: private */
    public void runPostCollapseRunnables() {
        ArrayList arrayList = new ArrayList(this.mPostCollapseRunnables);
        this.mPostCollapseRunnables.clear();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            ((Runnable) arrayList.get(i)).run();
        }
        this.mStatusBarKeyguardViewManager.readyForKeyguardDone();
    }

    public void dispatchNotificationsPanelTouchEvent(MotionEvent motionEvent) {
        if (this.mCommandQueue.panelsEnabled()) {
            this.mNotificationPanel.dispatchTouchEvent(motionEvent);
            int action = motionEvent.getAction();
            if (action == 0) {
                this.mStatusBarWindowController.setNotTouchable(true);
            } else if (action == 1 || action == 3) {
                this.mStatusBarWindowController.setNotTouchable(false);
            }
        }
    }

    public void animateExpandNotificationsPanel() {
        if (this.mCommandQueue.panelsEnabled()) {
            this.mNotificationPanel.expandWithoutQs();
        }
    }

    public void animateExpandSettingsPanel(String str) {
        if (this.mCommandQueue.panelsEnabled() && this.mUserSetup) {
            if (str != null) {
                this.mQSPanel.openDetails(str);
            }
            this.mNotificationPanel.expandWithQs();
        }
    }

    public void animateCollapseQuickSettings() {
        if (this.mState == 0) {
            this.mStatusBarView.collapsePanel(true, false, 1.0f);
        }
    }

    /* access modifiers changed from: 0000 */
    public void makeExpandedInvisible() {
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("Make expanded invisible: mExpandedVisible:");
            sb.append(this.mExpandedVisible);
            sb.append(" mStatusBarWindow:");
            sb.append(this.mStatusBarWindow);
            Log.d("StatusBar", sb.toString());
        }
        if (this.mExpandedVisible && this.mStatusBarWindow != null) {
            this.mStatusBarView.collapsePanel(false, false, 1.0f);
            this.mNotificationPanel.closeQs();
            this.mExpandedVisible = false;
            visibilityChanged(false);
            this.mStatusBarWindowController.setPanelVisible(false);
            this.mStatusBarWindowController.setForceStatusBarVisible(false);
            this.mGutsManager.closeAndSaveGuts(true, true, true, -1, -1, true);
            runPostCollapseRunnables();
            setInteracting(1, false);
            if (!this.mNotificationActivityStarter.isCollapsingToShowActivityOverLockscreen()) {
                showBouncerIfKeyguard();
            }
            this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, this.mNotificationPanel.hideStatusBarIconsWhenExpanded());
            if (!this.mStatusBarKeyguardViewManager.isShowing()) {
                WindowManagerGlobal.getInstance().trimMemory(20);
            }
            ((HighlightHintController) Dependency.get(HighlightHintController.class)).onExpandedVisibleChange(false);
        }
    }

    public boolean interceptTouchEvent(MotionEvent motionEvent) {
        if (this.mStatusBarWindowState == 0) {
            if (!(motionEvent.getAction() == 1 || motionEvent.getAction() == 3) || this.mExpandedVisible) {
                setInteracting(1, true);
            } else {
                setInteracting(1, false);
            }
        }
        return false;
    }

    public GestureRecorder getGestureRecorder() {
        return this.mGestureRec;
    }

    public BiometricUnlockController getBiometricUnlockController() {
        return this.mBiometricUnlockController;
    }

    public int getStatusBarWindowState() {
        return this.mStatusBarWindowState;
    }

    public void setWindowState(int i, int i2, int i3) {
        if (i == this.mDisplayId) {
            boolean z = true;
            boolean z2 = i3 == 0;
            if (!(this.mStatusBarWindow == null || i2 != 1 || this.mStatusBarWindowState == i3)) {
                OpMdmLogger.setStatusBarState(z2);
                this.mStatusBarWindowState = i3;
                StringBuilder sb = new StringBuilder();
                sb.append("Status bar ");
                sb.append(StatusBarManager.windowStateToString(i3));
                Log.d("StatusBar", sb.toString());
                if (!z2 && this.mState == 0) {
                    this.mStatusBarView.collapsePanel(false, false, 1.0f);
                }
                ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).updateSystemUiStateFlags();
                if (this.mStatusBarView != null) {
                    if (i3 != 2) {
                        z = false;
                    }
                    this.mStatusBarWindowHidden = z;
                    updateHideIconsForBouncer(false);
                }
            }
        }
    }

    public void setSystemUiVisibility(int i, int i2, int i3, int i4, int i5, Rect rect, Rect rect2, boolean z) {
        boolean z2;
        int i6 = i5;
        if (i == this.mDisplayId) {
            int i7 = this.mSystemUiVisibility;
            int i8 = ((~i6) & i7) | (i2 & i6);
            int i9 = i8 ^ i7;
            String str = "StatusBar";
            boolean z3 = false;
            if (i9 != 0 && OpStatusBar.DEBUG) {
                Log.d(str, String.format("setSystemUiVisibility displayId=%d vis=%s mask=%s oldVal=%s newVal=%s diff=%s", new Object[]{Integer.valueOf(i), Integer.toHexString(i2), Integer.toHexString(i5), Integer.toHexString(i7), Integer.toHexString(i8), Integer.toHexString(i9)}));
            }
            if (i9 != 0) {
                this.mSystemUiVisibility = i8;
                if ((i9 & 1) != 0) {
                    updateAreThereNotifications();
                }
                if ((268435456 & i2) != 0) {
                    this.mNoAnimationOnNextBarModeChange = true;
                }
                int computeStatusBarMode = computeStatusBarMode(i7, i8);
                boolean z4 = computeStatusBarMode != -1;
                if (z4 && computeStatusBarMode != this.mStatusBarMode) {
                    this.mStatusBarMode = computeStatusBarMode;
                    checkBarModes();
                    this.mAutoHideController.touchAutoHide();
                }
                z2 = z4;
            } else {
                z2 = false;
            }
            this.mIsInMultiWindow = !rect.isEmpty() || !rect2.isEmpty();
            this.mLightBarController.onSystemUiVisibilityChanged(i3, i4, i5, rect, rect2, z2, this.mStatusBarMode, z);
            StatusBarWindowView statusBarWindowView = this.mStatusBarWindow;
            if (!(statusBarWindowView == null || this.mLastIsAppFullscreen == statusBarWindowView.isAppFullscreen())) {
                boolean isNavShowing = isNavShowing();
                if (OpUtils.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("setSystemUiVisibility, mStatusBarWindow.requestApplyInsets, mLastIsAppFullscreen: ");
                    sb.append(this.mLastIsAppFullscreen);
                    sb.append(", isAppFullscreen:");
                    sb.append(this.mStatusBarWindow.isAppFullscreen());
                    sb.append(", isNavShowing: ");
                    sb.append(isNavShowing);
                    Log.d(str, sb.toString());
                }
                this.mLastIsAppFullscreen = this.mStatusBarWindow.isAppFullscreen();
                this.mStatusBarWindow.requestApplyInsets();
                StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
                if (statusBarKeyguardViewManager != null) {
                    if (!isNavShowing || this.mStatusBarWindow.isAppFullscreen()) {
                        z3 = true;
                    }
                    statusBarKeyguardViewManager.onHideNavBar(z3);
                }
            }
        }
    }

    public void showWirelessChargingAnimation(int i) {
        if (this.mDozing || this.mKeyguardManager.isKeyguardLocked()) {
            WirelessChargingAnimation.makeWirelessChargingAnimation(this.mContext, null, i, new WirelessChargingAnimation.Callback() {
                public void onAnimationStarting() {
                    CrossFadeHelper.fadeOut((View) StatusBar.this.mNotificationPanel, 1.0f);
                }

                public void onAnimationEnded() {
                    CrossFadeHelper.fadeIn(StatusBar.this.mNotificationPanel);
                }
            }, this.mDozing).show();
        } else {
            WirelessChargingAnimation.makeWirelessChargingAnimation(this.mContext, null, i, null, false).show();
        }
    }

    public void onRecentsAnimationStateChanged(boolean z) {
        setInteracting(2, z);
    }

    /* access modifiers changed from: protected */
    public int computeStatusBarMode(int i, int i2) {
        return computeBarMode(i, i2);
    }

    /* access modifiers changed from: protected */
    public BarTransitions getStatusBarTransitions() {
        return this.mStatusBarView.getBarTransitions();
    }

    /* access modifiers changed from: protected */
    public int computeBarMode(int i, int i2) {
        int barMode = barMode(i);
        int barMode2 = barMode(i2);
        if (barMode == barMode2) {
            return -1;
        }
        return barMode2;
    }

    /* access modifiers changed from: 0000 */
    public void checkBarModes() {
        if (!this.mDemoMode || this.mHighHintDemoMode) {
            if (this.mStatusBarView != null) {
                checkBarMode(this.mStatusBarMode, this.mStatusBarWindowState, getStatusBarTransitions());
            }
            this.mNavigationBarController.checkNavBarModes(this.mDisplayId);
            this.mNoAnimationOnNextBarModeChange = false;
        }
    }

    /* access modifiers changed from: 0000 */
    public void setQsScrimEnabled(boolean z) {
        this.mNotificationPanel.setQsScrimEnabled(z);
    }

    /* access modifiers changed from: 0000 */
    public void checkBarMode(int i, int i2, BarTransitions barTransitions) {
        boolean z = !this.mNoAnimationOnNextBarModeChange && this.mDeviceInteractive && i2 != 2;
        HighlightHintController highlightHintController = (HighlightHintController) Dependency.get(HighlightHintController.class);
        if (highlightHintController.showOvalLayout() || !highlightHintController.isHighLightHintShow() || barTransitions != this.mStatusBarView.getBarTransitions()) {
            barTransitions.transitionTo(i, z);
        } else {
            barTransitions.transitionTo(7, this.mHighlightColor, false);
        }
    }

    /* access modifiers changed from: private */
    public void finishBarAnimations() {
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            phoneStatusBarView.getBarTransitions().finishAnimations();
        }
        this.mNavigationBarController.finishBarAnimations(this.mDisplayId);
    }

    public void setInteracting(int i, boolean z) {
        int i2;
        boolean z2 = true;
        if (((this.mInteractingWindows & i) != 0) == z) {
            z2 = false;
        }
        if (z) {
            i2 = this.mInteractingWindows | i;
        } else {
            i2 = this.mInteractingWindows & (~i);
        }
        this.mInteractingWindows = i2;
        if (this.mInteractingWindows != 0) {
            this.mAutoHideController.suspendAutoHide();
        } else {
            this.mAutoHideController.resumeSuspendedAutoHide();
        }
        if (z2 && z && i == 2) {
            this.mNavigationBarController.touchAutoDim(this.mDisplayId);
            dismissVolumeDialog();
        }
        checkBarModes();
    }

    /* access modifiers changed from: private */
    public void dismissVolumeDialog() {
        VolumeComponent volumeComponent = this.mVolumeComponent;
        if (volumeComponent != null) {
            volumeComponent.dismissNow();
        }
    }

    public boolean inFullscreenMode() {
        return (this.mSystemUiVisibility & 6) != 0;
    }

    public boolean inImmersiveMode() {
        return (this.mSystemUiVisibility & 6144) != 0;
    }

    private boolean areLightsOn() {
        return (this.mSystemUiVisibility & 1) == 0;
    }

    public static String viewInfo(View view) {
        StringBuilder sb = new StringBuilder();
        sb.append("[(");
        sb.append(view.getLeft());
        String str = ",";
        sb.append(str);
        sb.append(view.getTop());
        sb.append(")(");
        sb.append(view.getRight());
        sb.append(str);
        sb.append(view.getBottom());
        sb.append(") ");
        sb.append(view.getWidth());
        sb.append("x");
        sb.append(view.getHeight());
        sb.append("]");
        return sb.toString();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String str;
        synchronized (this.mQueueLock) {
            printWriter.println("Current Status Bar state:");
            StringBuilder sb = new StringBuilder();
            sb.append("  mExpandedVisible=");
            sb.append(this.mExpandedVisible);
            printWriter.println(sb.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append("  mDisplayMetrics=");
            sb2.append(this.mDisplayMetrics);
            printWriter.println(sb2.toString());
            StringBuilder sb3 = new StringBuilder();
            sb3.append("  mStackScroller: ");
            sb3.append(viewInfo(this.mStackScroller));
            printWriter.println(sb3.toString());
            StringBuilder sb4 = new StringBuilder();
            sb4.append("  mStackScroller: ");
            sb4.append(viewInfo(this.mStackScroller));
            sb4.append(" scroll ");
            sb4.append(this.mStackScroller.getScrollX());
            sb4.append(",");
            sb4.append(this.mStackScroller.getScrollY());
            printWriter.println(sb4.toString());
        }
        printWriter.print("  mInteractingWindows=");
        printWriter.println(this.mInteractingWindows);
        printWriter.print("  mStatusBarWindowState=");
        printWriter.println(StatusBarManager.windowStateToString(this.mStatusBarWindowState));
        printWriter.print("  mSystemUiVisibility=");
        printWriter.println(Integer.toHexString(this.mSystemUiVisibility));
        StatusBarWindowView statusBarWindowView = this.mStatusBarWindow;
        if (!(statusBarWindowView == null || statusBarWindowView.findViewById(R$id.backdrop) == null)) {
            StringBuilder sb5 = new StringBuilder();
            sb5.append("  mBackdrop: visibility=");
            sb5.append(this.mStatusBarWindow.findViewById(R$id.backdrop).getVisibility());
            sb5.append(" , alpha=");
            sb5.append(this.mStatusBarWindow.findViewById(R$id.backdrop).getAlpha());
            printWriter.println(sb5.toString());
        }
        printWriter.print("  mStatusBarMode=");
        printWriter.println(BarTransitions.modeToString(this.mStatusBarMode));
        printWriter.print("  mDozing=");
        printWriter.println(this.mDozing);
        printWriter.print("  mZenMode=");
        printWriter.println(Global.zenModeToString(Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0)));
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            dumpBarTransitions(printWriter, "mStatusBarView", phoneStatusBarView.getBarTransitions());
        }
        printWriter.println("  StatusBarWindowView: ");
        StatusBarWindowView statusBarWindowView2 = this.mStatusBarWindow;
        if (statusBarWindowView2 != null) {
            statusBarWindowView2.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  mMediaManager: ");
        NotificationMediaManager notificationMediaManager = this.mMediaManager;
        if (notificationMediaManager != null) {
            notificationMediaManager.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  Panels: ");
        if (this.mNotificationPanel != null) {
            StringBuilder sb6 = new StringBuilder();
            sb6.append("    mNotificationPanel=");
            sb6.append(this.mNotificationPanel);
            sb6.append(" params=");
            sb6.append(this.mNotificationPanel.getLayoutParams().debug(""));
            printWriter.println(sb6.toString());
            printWriter.print("      ");
            StringBuilder sb7 = new StringBuilder();
            sb7.append(" PanelView alpha=");
            sb7.append(this.mNotificationPanel.getAlpha());
            printWriter.println(sb7.toString());
            StringBuilder sb8 = new StringBuilder();
            sb8.append(" PanelView visibility=");
            sb8.append(this.mNotificationPanel.getVisibility());
            printWriter.println(sb8.toString());
            this.mNotificationPanel.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  mStackScroller: ");
        if (this.mStackScroller instanceof Dumpable) {
            printWriter.print("      ");
            this.mStackScroller.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  Theme:");
        if (this.mUiModeManager == null) {
            str = "null";
        } else {
            StringBuilder sb9 = new StringBuilder();
            sb9.append(this.mUiModeManager.getNightMode());
            sb9.append("");
            str = sb9.toString();
        }
        StringBuilder sb10 = new StringBuilder();
        sb10.append("    dark theme: ");
        sb10.append(str);
        sb10.append(" (auto: ");
        sb10.append(0);
        sb10.append(", yes: ");
        sb10.append(2);
        sb10.append(", no: ");
        boolean z = true;
        sb10.append(1);
        sb10.append(")");
        printWriter.println(sb10.toString());
        if (this.mContext.getThemeResId() != R$style.Theme_SystemUI_Light) {
            z = false;
        }
        StringBuilder sb11 = new StringBuilder();
        sb11.append("    light wallpaper theme: ");
        sb11.append(z);
        printWriter.println(sb11.toString());
        DozeLog.dump(printWriter);
        BiometricUnlockController biometricUnlockController = this.mBiometricUnlockController;
        if (biometricUnlockController != null) {
            biometricUnlockController.dump(printWriter);
        }
        KeyguardIndicationController keyguardIndicationController = this.mKeyguardIndicationController;
        if (keyguardIndicationController != null) {
            keyguardIndicationController.dump(fileDescriptor, printWriter, strArr);
        }
        ScrimController scrimController = this.mScrimController;
        if (scrimController != null) {
            scrimController.dump(fileDescriptor, printWriter, strArr);
        }
        StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
        if (statusBarKeyguardViewManager != null) {
            statusBarKeyguardViewManager.dump(printWriter);
        }
        synchronized (this.mEntryManager.getNotificationData()) {
            this.mEntryManager.getNotificationData().dump(printWriter, "  ");
        }
        HeadsUpManagerPhone headsUpManagerPhone = this.mHeadsUpManager;
        if (headsUpManagerPhone != null) {
            headsUpManagerPhone.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println("  mHeadsUpManager: null");
        }
        NotificationGroupManager notificationGroupManager = this.mGroupManager;
        if (notificationGroupManager != null) {
            notificationGroupManager.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println("  mGroupManager: null");
        }
        LightBarController lightBarController = this.mLightBarController;
        if (lightBarController != null) {
            lightBarController.dump(fileDescriptor, printWriter, strArr);
        }
        KeyguardUpdateMonitor keyguardUpdateMonitor = this.mKeyguardUpdateMonitor;
        if (keyguardUpdateMonitor != null) {
            keyguardUpdateMonitor.dump(fileDescriptor, printWriter, strArr);
        }
        FalsingManagerFactory.getInstance(this.mContext).dump(printWriter);
        FalsingLog.dump(printWriter);
        printWriter.println("SharedPreferences:");
        for (Entry entry : Prefs.getAll(this.mContext).entrySet()) {
            printWriter.print("  ");
            printWriter.print((String) entry.getKey());
            printWriter.print("=");
            printWriter.println(entry.getValue());
        }
        super.dump(fileDescriptor, printWriter, strArr);
    }

    static void dumpBarTransitions(PrintWriter printWriter, String str, BarTransitions barTransitions) {
        printWriter.print("  ");
        printWriter.print(str);
        printWriter.print(".BarTransitions.mMode=");
        printWriter.println(BarTransitions.modeToString(barTransitions.getMode()));
    }

    public void createAndAddWindows(RegisterStatusBarResult registerStatusBarResult) {
        makeStatusBarView(registerStatusBarResult);
        this.mStatusBarWindowController = (StatusBarWindowController) Dependency.get(StatusBarWindowController.class);
        this.mStatusBarWindowController.add(this.mStatusBarWindow, getStatusBarHeight());
        OpLsState.getInstance().onWallpaperChange(this.mLockscreenWallpaper.getBitmap());
    }

    /* access modifiers changed from: 0000 */
    public void updateDisplaySize() {
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        this.mDisplay.getSize(this.mCurrentDisplaySize);
    }

    /* access modifiers changed from: 0000 */
    public float getDisplayDensity() {
        return this.mDisplayMetrics.density;
    }

    /* access modifiers changed from: 0000 */
    public float getDisplayWidth() {
        return (float) this.mDisplayMetrics.widthPixels;
    }

    /* access modifiers changed from: 0000 */
    public float getDisplayHeight() {
        return (float) this.mDisplayMetrics.heightPixels;
    }

    /* access modifiers changed from: 0000 */
    public int getRotation() {
        return this.mDisplay.getRotation();
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean z, boolean z2, int i) {
        startActivityDismissingKeyguard(intent, z, z2, false, null, i);
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean z, boolean z2) {
        startActivityDismissingKeyguard(intent, z, z2, 0);
    }

    public void startActivityDismissingKeyguard(Intent intent, boolean z, boolean z2, boolean z3, ActivityStarter.Callback callback, int i) {
        if (!z || this.mDeviceProvisionedController.isDeviceProvisioned()) {
            boolean wouldLaunchResolverActivity = this.mActivityIntentHelper.wouldLaunchResolverActivity(intent, this.mLockscreenUserManager.getCurrentUserId());
            $$Lambda$StatusBar$cYI_U_ShQVlsmm6P5qEeF15rkKQ r0 = new Runnable(intent, i, z3, callback) {
                private final /* synthetic */ Intent f$1;
                private final /* synthetic */ int f$2;
                private final /* synthetic */ boolean f$3;
                private final /* synthetic */ ActivityStarter.Callback f$4;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                }

                public final void run() {
                    StatusBar.this.lambda$startActivityDismissingKeyguard$17$StatusBar(this.f$1, this.f$2, this.f$3, this.f$4);
                }
            };
            executeRunnableDismissingKeyguard(r0, new Runnable() {
                public final void run() {
                    StatusBar.lambda$startActivityDismissingKeyguard$18(ActivityStarter.Callback.this);
                }
            }, z2, wouldLaunchResolverActivity, true);
        }
    }

    public /* synthetic */ void lambda$startActivityDismissingKeyguard$17$StatusBar(Intent intent, int i, boolean z, ActivityStarter.Callback callback) {
        int i2;
        Intent intent2 = intent;
        ActivityStarter.Callback callback2 = callback;
        this.mAssistManager.hideAssist();
        intent2.setFlags(335544320);
        intent.addFlags(i);
        ActivityOptions makeCustomAnimation = ActivityOptions.makeCustomAnimation(this.mContext, R$anim.op_qs_tile_long_press_enter, R$anim.op_qs_tile_long_press_exit);
        makeCustomAnimation.setDisallowEnterPictureInPictureWhileLaunching(z);
        if (intent2 == KeyguardBottomAreaView.INSECURE_CAMERA_INTENT) {
            makeCustomAnimation.setRotationAnimationHint(3);
        }
        if (intent.getAction() == "android.settings.panel.action.VOLUME") {
            makeCustomAnimation.setDisallowEnterPictureInPictureWhileLaunching(true);
        }
        try {
            i2 = ActivityTaskManager.getService().startActivityAsUser(null, this.mContext.getBasePackageName(), intent, intent2.resolveTypeIfNeeded(this.mContext.getContentResolver()), null, null, 0, 268435456, null, makeCustomAnimation.toBundle(), UserHandle.CURRENT.getIdentifier());
        } catch (RemoteException e) {
            Log.w("StatusBar", "Unable to start activity", e);
            i2 = -96;
        }
        if (callback2 != null) {
            callback2.onActivityStarted(i2);
        }
    }

    static /* synthetic */ void lambda$startActivityDismissingKeyguard$18(ActivityStarter.Callback callback) {
        if (callback != null) {
            callback.onActivityStarted(-96);
        }
    }

    public void readyForKeyguardDone() {
        this.mStatusBarKeyguardViewManager.readyForKeyguardDone();
    }

    public void executeRunnableDismissingKeyguard(Runnable runnable, Runnable runnable2, boolean z, boolean z2, boolean z3) {
        dismissKeyguardThenExecute(new OnDismissAction(runnable, z, z3) {
            private final /* synthetic */ Runnable f$1;
            private final /* synthetic */ boolean f$2;
            private final /* synthetic */ boolean f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final boolean onDismiss() {
                return StatusBar.this.lambda$executeRunnableDismissingKeyguard$19$StatusBar(this.f$1, this.f$2, this.f$3);
            }
        }, runnable2, z2);
    }

    public /* synthetic */ boolean lambda$executeRunnableDismissingKeyguard$19$StatusBar(Runnable runnable, boolean z, boolean z2) {
        if (runnable != null) {
            if (!this.mStatusBarKeyguardViewManager.isShowing() || !this.mStatusBarKeyguardViewManager.isOccluded()) {
                AsyncTask.execute(runnable);
            } else {
                this.mStatusBarKeyguardViewManager.addAfterKeyguardGoneRunnable(runnable);
            }
        }
        if (z) {
            if (!this.mExpandedVisible || this.mBouncerShowing) {
                this.mHandler.post(new Runnable() {
                    public final void run() {
                        StatusBar.this.runPostCollapseRunnables();
                    }
                });
            } else {
                animateCollapsePanels(2, true, true);
            }
        } else if (isInLaunchTransition() && this.mNotificationPanel.isLaunchTransitionFinished()) {
            C1543H h = this.mHandler;
            StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
            Objects.requireNonNull(statusBarKeyguardViewManager);
            h.post(new Runnable() {
                public final void run() {
                    StatusBarKeyguardViewManager.this.readyForKeyguardDone();
                }
            });
        }
        return z2;
    }

    public void resetUserExpandedStates() {
        ArrayList activeNotifications = this.mEntryManager.getNotificationData().getActiveNotifications();
        int size = activeNotifications.size();
        for (int i = 0; i < size; i++) {
            ((NotificationEntry) activeNotifications.get(i)).resetUserExpansion();
        }
    }

    /* access modifiers changed from: private */
    public void executeWhenUnlocked(OnDismissAction onDismissAction) {
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(true);
        }
        dismissKeyguardThenExecute(onDismissAction, null, false);
    }

    /* access modifiers changed from: protected */
    public void dismissKeyguardThenExecute(OnDismissAction onDismissAction, boolean z) {
        dismissKeyguardThenExecute(onDismissAction, null, z);
    }

    public void dismissKeyguardThenExecute(OnDismissAction onDismissAction, Runnable runnable, boolean z) {
        if (this.mWakefulnessLifecycle.getWakefulness() == 0 && this.mUnlockMethodCache.canSkipBouncer() && !this.mStatusBarStateController.leaveOpenOnKeyguardHide() && isPulsing()) {
            this.mBiometricUnlockController.startWakeAndUnlock(2);
        }
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            this.mStatusBarKeyguardViewManager.dismissWithAction(onDismissAction, runnable, z);
        } else {
            onDismissAction.onDismiss();
        }
    }

    public void onConfigChanged(Configuration configuration) {
        updateResources();
        updateDisplaySize();
        this.mViewHierarchyManager.updateRowStates();
        this.mScreenPinningRequest.onConfigurationChanged();
        opOnConfigChanged(configuration);
    }

    public void setLockscreenUser(int i) {
        this.mLockscreenWallpaper.setCurrentUser(i);
        this.mScrimController.setCurrentUser(i);
        this.mWallpaperChangedReceiver.onReceive(this.mContext, null);
    }

    /* access modifiers changed from: 0000 */
    public void updateResources() {
        QSPanel qSPanel = this.mQSPanel;
        if (qSPanel != null) {
            if (this.mExpandedVisible) {
                qSPanel.updateResources();
            } else {
                this.mHandler.postDelayed(new Runnable() {
                    public final void run() {
                        StatusBar.this.lambda$updateResources$20$StatusBar();
                    }
                }, 500);
            }
        }
        loadDimens();
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            phoneStatusBarView.updateResources();
        }
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        if (notificationPanelView != null) {
            if (this.mExpandedVisible) {
                notificationPanelView.updateResources();
            } else {
                this.mHandler.postDelayed(new Runnable() {
                    public final void run() {
                        StatusBar.this.lambda$updateResources$21$StatusBar();
                    }
                }, 500);
            }
        }
        BrightnessMirrorController brightnessMirrorController = this.mBrightnessMirrorController;
        if (brightnessMirrorController != null) {
            brightnessMirrorController.updateResources();
        }
    }

    public /* synthetic */ void lambda$updateResources$20$StatusBar() {
        this.mQSPanel.updateResources();
    }

    public /* synthetic */ void lambda$updateResources$21$StatusBar() {
        this.mNotificationPanel.updateResources();
    }

    /* access modifiers changed from: protected */
    public void loadDimens() {
        Resources resources = this.mContext.getResources();
        int i = this.mNaturalBarHeight;
        this.mNaturalBarHeight = resources.getDimensionPixelSize(17105422);
        StatusBarWindowController statusBarWindowController = this.mStatusBarWindowController;
        if (statusBarWindowController != null) {
            int i2 = this.mNaturalBarHeight;
            if (i2 != i) {
                statusBarWindowController.setBarHeight(i2);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void handleVisibleToUserChanged(boolean z) {
        if (z) {
            handleVisibleToUserChangedImpl(z);
            this.mNotificationLogger.startNotificationLogging();
            return;
        }
        this.mNotificationLogger.stopNotificationLogging();
        handleVisibleToUserChangedImpl(z);
    }

    /* access modifiers changed from: 0000 */
    public void handlePeekToExpandTransistion() {
        try {
            this.mBarService.onPanelRevealed(false, this.mEntryManager.getNotificationData().getActiveNotifications().size());
        } catch (RemoteException unused) {
        }
    }

    private void handleVisibleToUserChangedImpl(boolean z) {
        boolean z2;
        if (z) {
            boolean hasPinnedHeadsUp = this.mHeadsUpManager.hasPinnedHeadsUp();
            int i = 1;
            if (!this.mPresenter.isPresenterFullyCollapsed()) {
                int i2 = this.mState;
                if ((i2 == 0 || i2 == 2) && isNotificationLightBlinking()) {
                    z2 = true;
                    int size = this.mEntryManager.getNotificationData().getActiveNotifications().size();
                    if (!hasPinnedHeadsUp || !this.mPresenter.isPresenterFullyCollapsed()) {
                        i = size;
                    }
                    this.mUiOffloadThread.submit(new Runnable(z2, i) {
                        private final /* synthetic */ boolean f$1;
                        private final /* synthetic */ int f$2;

                        {
                            this.f$1 = r2;
                            this.f$2 = r3;
                        }

                        public final void run() {
                            StatusBar.this.lambda$handleVisibleToUserChangedImpl$22$StatusBar(this.f$1, this.f$2);
                        }
                    });
                    return;
                }
            }
            z2 = false;
            int size2 = this.mEntryManager.getNotificationData().getActiveNotifications().size();
            i = size2;
            this.mUiOffloadThread.submit(new Runnable(z2, i) {
                private final /* synthetic */ boolean f$1;
                private final /* synthetic */ int f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    StatusBar.this.lambda$handleVisibleToUserChangedImpl$22$StatusBar(this.f$1, this.f$2);
                }
            });
            return;
        }
        this.mUiOffloadThread.submit(new Runnable() {
            public final void run() {
                StatusBar.this.lambda$handleVisibleToUserChangedImpl$23$StatusBar();
            }
        });
    }

    public /* synthetic */ void lambda$handleVisibleToUserChangedImpl$22$StatusBar(boolean z, int i) {
        try {
            this.mBarService.onPanelRevealed(z, i);
        } catch (RemoteException unused) {
        }
    }

    public /* synthetic */ void lambda$handleVisibleToUserChangedImpl$23$StatusBar() {
        try {
            this.mBarService.onPanelHidden();
        } catch (RemoteException unused) {
        }
    }

    private void logStateToEventlog() {
        boolean isShowing = this.mStatusBarKeyguardViewManager.isShowing();
        boolean isOccluded = this.mStatusBarKeyguardViewManager.isOccluded();
        boolean isBouncerShowing = this.mStatusBarKeyguardViewManager.isBouncerShowing();
        boolean isMethodSecure = this.mUnlockMethodCache.isMethodSecure();
        boolean canSkipBouncer = this.mUnlockMethodCache.canSkipBouncer();
        int loggingFingerprint = getLoggingFingerprint(this.mState, isShowing, isOccluded, isBouncerShowing, isMethodSecure, canSkipBouncer);
        if (loggingFingerprint != this.mLastLoggedStateFingerprint) {
            if (this.mStatusBarStateLog == null) {
                this.mStatusBarStateLog = new LogMaker(0);
            }
            this.mMetricsLogger.write(this.mStatusBarStateLog.setCategory(isBouncerShowing ? 197 : 196).setType(isShowing ? 1 : 2).setSubtype(isMethodSecure ? 1 : 0));
            EventLogTags.writeSysuiStatusBarState(this.mState, isShowing ? 1 : 0, isOccluded ? 1 : 0, isBouncerShowing ? 1 : 0, isMethodSecure ? 1 : 0, canSkipBouncer ? 1 : 0);
            this.mLastLoggedStateFingerprint = loggingFingerprint;
        }
    }

    /* access modifiers changed from: 0000 */
    public void vibrate() {
        ((Vibrator) this.mContext.getSystemService("vibrator")).vibrate(250, VIBRATION_ATTRIBUTES);
    }

    public /* synthetic */ void lambda$new$24$StatusBar() {
        Debug.stopMethodTracing();
        Log.d("StatusBar", "stopTracing");
        vibrate();
    }

    public void postQSRunnableDismissingKeyguard(Runnable runnable) {
        this.mHandler.post(new Runnable(runnable) {
            private final /* synthetic */ Runnable f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$postQSRunnableDismissingKeyguard$26$StatusBar(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$postQSRunnableDismissingKeyguard$26$StatusBar(Runnable runnable) {
        this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(true);
        executeRunnableDismissingKeyguard(new Runnable(runnable) {
            private final /* synthetic */ Runnable f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$postQSRunnableDismissingKeyguard$25$StatusBar(this.f$1);
            }
        }, null, false, false, false);
    }

    public /* synthetic */ void lambda$postQSRunnableDismissingKeyguard$25$StatusBar(Runnable runnable) {
        this.mHandler.post(runnable);
    }

    public void postStartActivityDismissingKeyguard(PendingIntent pendingIntent) {
        this.mHandler.post(new Runnable(pendingIntent) {
            private final /* synthetic */ PendingIntent f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$postStartActivityDismissingKeyguard$27$StatusBar(this.f$1);
            }
        });
    }

    public void postStartActivityDismissingKeyguard(Intent intent, int i) {
        this.mHandler.postDelayed(new Runnable(intent) {
            private final /* synthetic */ Intent f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$postStartActivityDismissingKeyguard$28$StatusBar(this.f$1);
            }
        }, (long) i);
    }

    public /* synthetic */ void lambda$postStartActivityDismissingKeyguard$28$StatusBar(Intent intent) {
        handleStartActivityDismissingKeyguard(intent, true);
    }

    private void handleStartActivityDismissingKeyguard(Intent intent, boolean z) {
        startActivityDismissingKeyguard(intent, z, true);
    }

    public void dispatchDemoCommand(String str, Bundle bundle) {
        View view;
        int i = 0;
        if (!this.mDemoModeAllowed) {
            this.mDemoModeAllowed = Global.getInt(this.mContext.getContentResolver(), "sysui_demo_allowed", 0) != 0;
        }
        if (this.mDemoModeAllowed) {
            String str2 = "enter";
            String str3 = "exit";
            if (str.equals(str2)) {
                this.mDemoMode = true;
            } else if (str.equals(str3)) {
                this.mDemoMode = false;
                checkBarModes();
            } else if (!this.mDemoMode) {
                dispatchDemoCommand(str2, new Bundle());
            }
            boolean z = str.equals(str2) || str.equals(str3);
            if (z || str.equals("volume")) {
                VolumeComponent volumeComponent = this.mVolumeComponent;
                if (volumeComponent != null) {
                    volumeComponent.dispatchDemoCommand(str, bundle);
                }
            }
            if (z || str.equals("clock")) {
                dispatchDemoCommandToView(str, bundle, R$id.clock);
            }
            if (z || str.equals("battery")) {
                this.mBatteryController.dispatchDemoCommand(str, bundle);
            }
            if (z || str.equals("status")) {
                ((StatusBarIconControllerImpl) this.mIconController).dispatchDemoCommand(str, bundle);
            }
            if (this.mNetworkController != null && (z || str.equals("network"))) {
                this.mNetworkController.dispatchDemoCommand(str, bundle);
            }
            if (z || str.equals("notifications")) {
                PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
                if (phoneStatusBarView == null) {
                    view = null;
                } else {
                    view = phoneStatusBarView.findViewById(R$id.notification_icon_area);
                }
                if (view != null) {
                    view.setVisibility((!this.mDemoMode || !"false".equals(bundle.getString("visible"))) ? 0 : 4);
                }
            }
            String str4 = "mode";
            if (str.equals("bars")) {
                String string = bundle.getString(str4);
                if (!"opaque".equals(string)) {
                    i = "translucent".equals(string) ? 2 : "semi-transparent".equals(string) ? 1 : "transparent".equals(string) ? 4 : "warning".equals(string) ? 5 : -1;
                }
                if (i != -1) {
                    PhoneStatusBarView phoneStatusBarView2 = this.mStatusBarView;
                    if (phoneStatusBarView2 != null) {
                        phoneStatusBarView2.getBarTransitions().transitionTo(i, true);
                    }
                    this.mNavigationBarController.transitionTo(this.mDisplayId, i, true);
                }
            }
            if (z || str.equals("operator")) {
                dispatchDemoCommandToView(str, bundle, R$id.operator_name);
            }
            String str5 = "start";
            String str6 = "chronometer";
            String str7 = "show";
            String str8 = "type";
            String str9 = "enable";
            if (str.equals("highlight")) {
                this.mHighHintDemoMode = bundle.getString(str4).equals(str9);
                showDemoHighLight(bundle.getString(str8).equals(str7), bundle.getString(str6).equals(str5));
            }
            if (str.equals("carmode_highlight")) {
                this.mDemoCarModeHighHintDemoMode = bundle.getString(str4).equals(str9);
                showDemooCarModeHighLight(bundle.getString(str8).equals(str7), bundle.getString(str6).equals(str5));
            }
        }
    }

    private void dispatchDemoCommandToView(String str, Bundle bundle, int i) {
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            View findViewById = phoneStatusBarView.findViewById(i);
            if (findViewById instanceof DemoMode) {
                ((DemoMode) findViewById).dispatchDemoCommand(str, bundle);
            }
        }
    }

    private void showDemoHighLight(boolean z, boolean z2) {
        StringBuilder sb = new StringBuilder();
        sb.append(" showDemoHighLight show:");
        sb.append(z);
        String str = "StatusBar";
        Log.i(str, sb.toString());
        Context context = this.mContext;
        if (context != null) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
            if (z) {
                if (this.mDemoHighlightHint == null) {
                    this.mDemoHighlightHint = getDemoNotificationBuilder();
                }
                this.mDemoHighlightHint.setShowOnStatusBar(true);
                this.mDemoHighlightHint.setUsesChronometerOnStatusBar(true);
                this.mDemoHighlightHint.setChronometerBase(0);
                if (z2) {
                    this.mDemoHighlightHint.setChronometerState(0);
                } else {
                    this.mDemoHighlightHint.setChronometerState(1);
                }
                notificationManager.notify(50, this.mDemoHighlightHint.build());
                Log.i(str, " send demo HighlightHint");
                return;
            }
            this.mDemoHighlightHint = null;
            notificationManager.cancel(50);
            Log.i(str, " cancel demo HighlightHint");
        }
    }

    private Notification.Builder getDemoNotificationBuilder() {
        Notification.Builder builder = new Notification.Builder(this.mContext, NotificationChannels.GENERAL);
        builder.setOngoing(true);
        builder.setPriority(1);
        constructDemoHighLightNotification(builder, R$drawable.ic_add);
        return builder;
    }

    private void constructDemoHighLightNotification(Notification.Builder builder, int i) {
        builder.setSmallIcon(i);
        builder.setIconOnStatusBar(i);
        builder.setPriority(1);
        builder.setPriorityOnStatusBar(50);
        builder.setTextOnStatusBar(R$string.notification_tap_again);
        builder.setBackgroundColorOnStatusBar(-16470538);
        builder.setUsesChronometer(true);
        builder.setUsesChronometerOnStatusBar(true);
        builder.setShowOnStatusBar(true);
    }

    /* access modifiers changed from: private */
    public void showDemooCarModeHighLight(boolean z, boolean z2) {
        StringBuilder sb = new StringBuilder();
        sb.append(" showDemooCarModeHighLight show:");
        sb.append(z);
        String str = "StatusBar";
        Log.i(str, sb.toString());
        Context context = this.mContext;
        if (context != null) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
            if (z) {
                if (this.mDemoCarModeHighlightHint == null) {
                    this.mDemoCarModeHighlightHint = getCarModeDemoNotificationBuilder();
                }
                this.mDemoCarModeHighlightHint.setShowOnStatusBar(true);
                this.mDemoCarModeHighlightHint.setUsesChronometerOnStatusBar(true);
                this.mDemoCarModeHighlightHint.setChronometerBase(0);
                String str2 = "com.oneplus.carmode.test";
                this.mDemoCarModeHighlightHint.setIntentOnStatusBar(new Intent(str2));
                if (z2) {
                    this.mDemoCarModeHighlightHint.setChronometerState(0);
                } else {
                    this.mDemoCarModeHighlightHint.setChronometerState(1);
                }
                if (this.mCarModeReceiver == null) {
                    this.mCarModeReceiver = new CarModeScreenshotReceiver();
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(str2);
                    this.mContext.registerReceiver(this.mCarModeReceiver, intentFilter);
                }
                Notification build = this.mDemoCarModeHighlightHint.build();
                notificationManager.notify(200, build);
                StringBuilder sb2 = new StringBuilder();
                sb2.append(" send demo carModeHighlightHint intent:");
                sb2.append(build.getIntentOnStatusBar());
                Log.i(str, sb2.toString());
                return;
            }
            this.mDemoCarModeHighlightHint = null;
            CarModeScreenshotReceiver carModeScreenshotReceiver = this.mCarModeReceiver;
            if (carModeScreenshotReceiver != null) {
                this.mContext.unregisterReceiver(carModeScreenshotReceiver);
                this.mCarModeReceiver = null;
            }
            notificationManager.cancel(200);
            Log.i(str, " cancel demo carModeHighlightHint");
        }
    }

    private Notification.Builder getCarModeDemoNotificationBuilder() {
        Notification.Builder builder = new Notification.Builder(this.mContext, NotificationChannels.GENERAL);
        builder.setOngoing(true);
        builder.setPriority(1);
        constructCarModeDemoHighLightNotification(builder, R$drawable.ic_add);
        return builder;
    }

    private void constructCarModeDemoHighLightNotification(Notification.Builder builder, int i) {
        builder.setSmallIcon(i);
        builder.setIconOnStatusBar(i);
        builder.setPriority(1);
        builder.setPriorityOnStatusBar(200);
        builder.setTextOnStatusBar(R$string.notification_tap_again);
        builder.setBackgroundColorOnStatusBar(-3823);
        builder.setShowOnStatusBar(true);
    }

    public void showKeyguard() {
        hideNavigationBarGuide();
        this.mStatusBarStateController.setKeyguardRequested(true);
        this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(false);
        this.mPendingRemoteInputView = null;
        updateIsKeyguard();
        this.mAssistManager.onLockscreenShown();
    }

    public boolean hideKeyguard() {
        this.mStatusBarStateController.setKeyguardRequested(false);
        return updateIsKeyguard();
    }

    public boolean isFullScreenUserSwitcherState() {
        return this.mState == 3;
    }

    /* access modifiers changed from: private */
    public boolean updateIsKeyguard() {
        boolean z = true;
        boolean z2 = this.mBiometricUnlockController.getMode() == 1;
        boolean z3 = this.mDozingRequested && (!this.mDeviceInteractive || (isGoingToSleep() && (isScreenFullyOff() || this.mIsKeyguard)));
        if ((!this.mStatusBarStateController.isKeyguardRequested() && !z3) || z2) {
            z = false;
        }
        if (z3) {
            updatePanelExpansionForKeyguard();
        }
        if (!z) {
            return hideKeyguardImpl();
        }
        if (!isGoingToSleep() || this.mScreenLifecycle.getScreenState() != 3) {
            showKeyguardImpl();
        }
        return false;
    }

    public void showKeyguardImpl() {
        if (OpStatusBar.DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("showKeyguardImpl, mLaunchTransitionFadingAway:");
            sb.append(this.mKeyguardMonitor.isLaunchTransitionFadingAway());
            Log.d("StatusBar", sb.toString());
        }
        this.mIsKeyguard = true;
        if (this.mKeyguardMonitor.isLaunchTransitionFadingAway()) {
            this.mNotificationPanel.animate().cancel();
            onLaunchTransitionFadingEnded();
        }
        this.mHandler.removeMessages(1003);
        UserSwitcherController userSwitcherController = this.mUserSwitcherController;
        if (userSwitcherController != null && userSwitcherController.useFullscreenUserSwitcher()) {
            this.mStatusBarStateController.setState(3);
        } else if (!this.mPulseExpansionHandler.isWakingToShadeLocked()) {
            this.mStatusBarStateController.setState(1);
        }
        if (this.mState == 1) {
            this.mNotificationPanel.resetViews(false);
        }
        updatePanelExpansionForKeyguard();
        NotificationEntry notificationEntry = this.mDraggedDownEntry;
        if (notificationEntry != null) {
            notificationEntry.setUserLocked(false);
            this.mDraggedDownEntry.notifyHeightChanged(false);
            this.mDraggedDownEntry = null;
        }
    }

    private void updatePanelExpansionForKeyguard() {
        opUpdatePanelExpansionForKeyguard();
    }

    /* access modifiers changed from: private */
    public void onLaunchTransitionFadingEnded() {
        this.mNotificationPanel.setAlpha(1.0f);
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        runLaunchTransitionEndRunnable();
        this.mKeyguardMonitor.setLaunchTransitionFadingAway(false);
        this.mPresenter.updateMediaMetaData(true, true);
    }

    public void addPostCollapseAction(Runnable runnable) {
        this.mPostCollapseRunnables.add(runnable);
    }

    public boolean isInLaunchTransition() {
        return this.mNotificationPanel.isLaunchTransitionRunning() || this.mNotificationPanel.isLaunchTransitionFinished();
    }

    public void fadeKeyguardAfterLaunchTransition(Runnable runnable, Runnable runnable2) {
        this.mHandler.removeMessages(1003);
        this.mLaunchTransitionEndRunnable = runnable2;
        $$Lambda$StatusBar$Y3fMrUHySZxiJoTF8C7vKsQWUE r4 = new Runnable(runnable) {
            private final /* synthetic */ Runnable f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                StatusBar.this.lambda$fadeKeyguardAfterLaunchTransition$29$StatusBar(this.f$1);
            }
        };
        if (this.mNotificationPanel.isLaunchTransitionRunning()) {
            this.mNotificationPanel.setLaunchTransitionEndRunnable(r4);
        } else {
            r4.run();
        }
    }

    public /* synthetic */ void lambda$fadeKeyguardAfterLaunchTransition$29$StatusBar(Runnable runnable) {
        this.mKeyguardMonitor.setLaunchTransitionFadingAway(true);
        if (runnable != null) {
            runnable.run();
        }
        updateScrimController();
        this.mPresenter.updateMediaMetaData(false, true);
        this.mNotificationPanel.setAlpha(1.0f);
        this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(100).setDuration(300).withLayer().withEndAction(new Runnable() {
            public final void run() {
                StatusBar.this.onLaunchTransitionFadingEnded();
            }
        });
        this.mCommandQueue.appTransitionStarting(this.mDisplayId, SystemClock.uptimeMillis(), 120, true);
    }

    public void fadeKeyguardWhilePulsing() {
        this.mNotificationPanel.animate().alpha(0.0f).setStartDelay(0).setDuration(96).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
            public final void run() {
                StatusBar.this.lambda$fadeKeyguardWhilePulsing$30$StatusBar();
            }
        }).start();
    }

    public /* synthetic */ void lambda$fadeKeyguardWhilePulsing$30$StatusBar() {
        hideKeyguard();
        this.mStatusBarKeyguardViewManager.onKeyguardFadedAway();
    }

    public void animateKeyguardUnoccluding() {
        this.mNotificationPanel.setExpandedFraction(1.0f);
        animateExpandNotificationsPanel();
    }

    public void startLaunchTransitionTimeout() {
        this.mHandler.sendEmptyMessageDelayed(1003, 5000);
    }

    /* access modifiers changed from: private */
    public void onLaunchTransitionTimeout() {
        Log.w("StatusBar", "Launch transition: Timeout!");
        this.mNotificationPanel.onAffordanceLaunchEnded();
        releaseGestureWakeLock();
        this.mNotificationPanel.resetViews(false);
        this.mStatusBarKeyguardViewManager.dismissWithAction(null, null, false);
    }

    private void runLaunchTransitionEndRunnable() {
        Runnable runnable = this.mLaunchTransitionEndRunnable;
        if (runnable != null) {
            this.mLaunchTransitionEndRunnable = null;
            runnable.run();
        }
    }

    public boolean hideKeyguardImpl() {
        this.mIsKeyguard = false;
        Trace.beginSection("StatusBar#hideKeyguard");
        boolean leaveOpenOnKeyguardHide = this.mStatusBarStateController.leaveOpenOnKeyguardHide();
        if (!this.mStatusBarStateController.setState(0)) {
            this.mLockscreenUserManager.updatePublicMode();
        }
        if (this.mStatusBarStateController.leaveOpenOnKeyguardHide()) {
            if (!this.mStatusBarStateController.isKeyguardRequested()) {
                this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(false);
            }
            long calculateGoingToFullShadeDelay = this.mKeyguardMonitor.calculateGoingToFullShadeDelay();
            this.mNotificationPanel.animateToFullShade(calculateGoingToFullShadeDelay);
            NotificationEntry notificationEntry = this.mDraggedDownEntry;
            if (notificationEntry != null) {
                notificationEntry.setUserLocked(false);
                this.mDraggedDownEntry = null;
            }
            this.mNavigationBarController.disableAnimationsDuringHide(this.mDisplayId, calculateGoingToFullShadeDelay);
        } else if (!this.mNotificationPanel.isCollapsing()) {
            instantCollapseNotificationPanel();
        }
        QSPanel qSPanel = this.mQSPanel;
        if (qSPanel != null) {
            qSPanel.refreshAllTiles();
        }
        this.mHandler.removeMessages(1003);
        releaseGestureWakeLock();
        this.mNotificationPanel.onAffordanceLaunchEnded();
        this.mNotificationPanel.animate().cancel();
        this.mNotificationPanel.setAlpha(1.0f);
        updateScrimController();
        Trace.endSection();
        return leaveOpenOnKeyguardHide;
    }

    /* access modifiers changed from: private */
    public void releaseGestureWakeLock() {
        if (this.mGestureWakeLock.isHeld()) {
            this.mGestureWakeLock.release();
        }
    }

    public void keyguardGoingAway() {
        this.mKeyguardMonitor.notifyKeyguardGoingAway(true);
        this.mCommandQueue.appTransitionPending(this.mDisplayId, true);
    }

    public void setKeyguardFadingAway(long j, long j2, long j3) {
        long j4 = j3;
        this.mCommandQueue.appTransitionStarting(this.mDisplayId, (j + j4) - 120, 120, true);
        this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, j4 > 0);
        this.mCommandQueue.appTransitionStarting(this.mDisplayId, j - 120, 120, true);
        this.mKeyguardMonitor.notifyKeyguardFadingAway(j2, j4);
    }

    public void finishKeyguardFadingAway() {
        this.mKeyguardMonitor.notifyKeyguardDoneFading();
        this.mScrimController.setExpansionAffectsAlpha(true);
    }

    /* access modifiers changed from: protected */
    public void updateTheme() {
        int i = this.mColorExtractor.getNeutralColors().supportsDarkText() ? R$style.Theme_SystemUI_Light : R$style.Theme_SystemUI;
        if (this.mContext.getThemeResId() != i) {
            this.mContext.setTheme(i);
            ((ConfigurationController) Dependency.get(ConfigurationController.class)).notifyThemeChanged();
        }
    }

    private void updateDozingState() {
        Trace.traceCounter(4096, "dozing", this.mDozing ? 1 : 0);
        Trace.beginSection("StatusBar#updateDozingState");
        boolean isGoingToSleepVisibleNotOccluded = this.mStatusBarKeyguardViewManager.isGoingToSleepVisibleNotOccluded();
        boolean z = false;
        boolean z2 = this.mBiometricUnlockController.getMode() == 1;
        if ((!this.mDozing && this.mDozeServiceHost.shouldAnimateWakeup() && !z2) || (this.mDozing && this.mDozeServiceHost.shouldAnimateScreenOff() && isGoingToSleepVisibleNotOccluded)) {
            z = true;
        }
        this.mNotificationPanel.setDozing(this.mDozing, z, this.mWakeUpTouchLocation);
        updateQsExpansionEnabled();
        Trace.endSection();
    }

    public void userActivity() {
        if (this.mState == 1) {
            this.mKeyguardViewMediatorCallback.userActivity();
        }
    }

    public boolean interceptMediaKey(KeyEvent keyEvent) {
        if (this.mState != 1 || !this.mStatusBarKeyguardViewManager.interceptMediaKey(keyEvent)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean shouldUnlockOnMenuPressed() {
        return this.mDeviceInteractive && this.mState != 0 && this.mStatusBarKeyguardViewManager.shouldDismissOnMenuPressed();
    }

    public boolean onMenuPressed() {
        if (!shouldUnlockOnMenuPressed()) {
            return false;
        }
        animateCollapsePanels(2, true);
        return true;
    }

    public void endAffordanceLaunch() {
        releaseGestureWakeLock();
        this.mNotificationPanel.onAffordanceLaunchEnded();
    }

    public boolean onBackPressed() {
        boolean z = this.mScrimController.getState() == ScrimState.BOUNCER_SCRIMMED;
        if (this.mStatusBarKeyguardViewManager.onBackPressed(z)) {
            if (!z) {
                this.mNotificationPanel.expandWithoutQs();
            } else {
                this.mHandler.removeMessages(1003);
                this.mNotificationPanel.resetViews(false);
            }
            return true;
        } else if (this.mNotificationPanel.isQsExpanded()) {
            if (this.mNotificationPanel.isQsDetailShowing()) {
                this.mNotificationPanel.closeQsDetail();
            } else {
                this.mNotificationPanel.animateCloseQs(false);
            }
            return true;
        } else {
            int i = this.mState;
            if (i == 1 || i == 2) {
                KeyguardUserSwitcher keyguardUserSwitcher = this.mKeyguardUserSwitcher;
                return keyguardUserSwitcher != null && keyguardUserSwitcher.hideIfNotSimple(true);
            }
            if (this.mNotificationPanel.canPanelBeCollapsed()) {
                animateCollapsePanels();
            } else {
                this.mBubbleController.performBackPressIfNeeded();
            }
            return true;
        }
    }

    public boolean onSpacePressed() {
        if (!this.mDeviceInteractive || this.mState == 0) {
            return false;
        }
        animateCollapsePanels(2, true);
        return true;
    }

    private void showBouncerIfKeyguard() {
        int i = this.mState;
        if ((i == 1 || i == 2) && !this.mKeyguardViewMediator.isHiding()) {
            showBouncer(true);
        }
    }

    public void showBouncer(boolean z) {
        this.mStatusBarKeyguardViewManager.showBouncer(z);
    }

    public void instantExpandNotificationsPanel() {
        makeExpandedVisible(true);
        this.mNotificationPanel.expand(false);
        this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, false);
    }

    public boolean closeShadeIfOpen() {
        if (!this.mNotificationPanel.isFullyCollapsed()) {
            this.mCommandQueue.animateCollapsePanels(2, true);
            visibilityChanged(false);
            this.mAssistManager.hideAssist();
        }
        return false;
    }

    public void postOnShadeExpanded(final Runnable runnable) {
        this.mNotificationPanel.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                if (StatusBar.this.getStatusBarWindow().getHeight() != StatusBar.this.getStatusBarHeight()) {
                    StatusBar.this.mNotificationPanel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    StatusBar.this.mNotificationPanel.post(runnable);
                }
            }
        });
    }

    public void instantCollapseNotificationPanel() {
        this.mNotificationPanel.instantCollapse();
        runPostCollapseRunnables();
    }

    public void onStatePreChange(int i, int i2) {
        if (this.mVisible && (i2 == 2 || ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class)).goingToFullShade())) {
            clearNotificationEffects();
        }
        if (i2 == 1) {
            this.mRemoteInputManager.onPanelCollapsed();
            maybeEscalateHeadsUp();
        }
    }

    public void onStateChanged(int i) {
        this.mState = i;
        updateReportRejectedTouchVisibility();
        updateDozing();
        updateTheme();
        this.mNavigationBarController.touchAutoDim(this.mDisplayId);
        StringBuilder sb = new StringBuilder();
        sb.append("updateKeyguardState: ");
        sb.append(this.mStatusBarStateController.goingToFullShade());
        sb.append(", ");
        sb.append(this.mStatusBarStateController.fromShadeLocked());
        sb.append(", show:");
        sb.append(this.mStatusBarKeyguardViewManager.isShowing());
        sb.append(", occlude");
        sb.append(this.mStatusBarKeyguardViewManager.isOccluded());
        sb.append(", state:");
        sb.append(this.mState);
        Log.d("StatusBar", sb.toString());
        Trace.beginSection("StatusBar#updateKeyguardState");
        boolean z = true;
        if (this.mState == 1) {
            this.mKeyguardIndicationController.setVisible(true);
            KeyguardUserSwitcher keyguardUserSwitcher = this.mKeyguardUserSwitcher;
            if (keyguardUserSwitcher != null) {
                keyguardUserSwitcher.setKeyguard(true, this.mStatusBarStateController.fromShadeLocked());
            }
            PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
            if (phoneStatusBarView != null) {
                phoneStatusBarView.removePendingHideExpandedRunnables();
            }
            View view = this.mAmbientIndicationContainer;
            if (view != null) {
                view.setVisibility(0);
            }
        } else {
            this.mKeyguardIndicationController.setVisible(false);
            KeyguardUserSwitcher keyguardUserSwitcher2 = this.mKeyguardUserSwitcher;
            if (keyguardUserSwitcher2 != null) {
                keyguardUserSwitcher2.setKeyguard(false, this.mStatusBarStateController.goingToFullShade() || this.mState == 2 || this.mStatusBarStateController.fromShadeLocked());
            }
            View view2 = this.mAmbientIndicationContainer;
            if (view2 != null) {
                view2.setVisibility(4);
            }
        }
        ((HighlightHintController) Dependency.get(HighlightHintController.class)).onBarStatechange(this.mState);
        if (this.mState == 1) {
            this.mStackScroller.hideDismissAnimate(true);
        }
        updateDozingState();
        checkBarModes();
        updateScrimController();
        NotificationPresenter notificationPresenter = this.mPresenter;
        if (this.mState == 1) {
            z = false;
        }
        notificationPresenter.updateMediaMetaData(false, z);
        updateKeyguardState();
        Trace.endSection();
    }

    public void onDozingChanged(boolean z) {
        Trace.beginSection("StatusBar#updateDozing");
        this.mDozing = z;
        this.mNotificationPanel.resetViews(this.mDozingRequested && DozeParameters.getInstance(this.mContext).shouldControlScreenOff());
        updateQsExpansionEnabled();
        this.mKeyguardViewMediator.setAodShowing(this.mDozing);
        this.mEntryManager.updateNotifications();
        updateDozingState();
        updateScrimController();
        updateReportRejectedTouchVisibility();
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    public void updateDozing() {
        opUpdateDozing();
    }

    private void updateKeyguardState() {
        this.mKeyguardMonitor.notifyKeyguardState(this.mStatusBarKeyguardViewManager.isShowing(), this.mUnlockMethodCache.isMethodSecure(), this.mStatusBarKeyguardViewManager.isOccluded());
    }

    public void onActivationReset() {
        this.mKeyguardIndicationController.hideTransientIndication();
    }

    public void onTrackingStarted() {
        runPostCollapseRunnables();
    }

    public void onClosingFinished() {
        runPostCollapseRunnables();
        StringBuilder sb = new StringBuilder();
        sb.append("onClosingFinished: ");
        sb.append(this.mPresenter.isPresenterFullyCollapsed());
        Log.v("StatusBar", sb.toString());
        if (!this.mPresenter.isPresenterFullyCollapsed()) {
            this.mStatusBarWindowController.setStatusBarFocusable(true);
        }
    }

    public void onUnlockHintStarted() {
        this.mFalsingManager.onUnlockHintStarted();
        if (OpUtils.isWeakFaceUnlockEnabled()) {
            KeyguardUpdateMonitor keyguardUpdateMonitor = this.mKeyguardUpdateMonitor;
            if (keyguardUpdateMonitor != null) {
                int biometricTimeoutStringWhenLock = keyguardUpdateMonitor.getBiometricTimeoutStringWhenLock();
                if (biometricTimeoutStringWhenLock != 0) {
                    this.mKeyguardIndicationController.showTransientIndication(biometricTimeoutStringWhenLock);
                    Log.d("StatusBar", "WeakFace transient");
                    return;
                }
            }
        }
        this.mKeyguardIndicationController.showTransientIndication(R$string.keyguard_unlock);
    }

    public void onHintFinished() {
        this.mKeyguardIndicationController.hideTransientIndicationDelayed(1200);
    }

    public void onCameraHintStarted() {
        this.mFalsingManager.onCameraHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(R$string.camera_hint);
    }

    public void onVoiceAssistHintStarted() {
        this.mFalsingManager.onLeftAffordanceHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(R$string.voice_hint);
    }

    public void onPhoneHintStarted() {
        this.mFalsingManager.onLeftAffordanceHintStarted();
        this.mKeyguardIndicationController.showTransientIndication(R$string.phone_hint);
    }

    public void onTrackingStopped(boolean z) {
        int i = this.mState;
        if ((i == 1 || i == 2) && !z && !this.mUnlockMethodCache.canSkipBouncer()) {
            showBouncer(false);
        }
    }

    public NavigationBarView getNavigationBarView() {
        return this.mNavigationBarController.getNavigationBarView(this.mDisplayId);
    }

    public KeyguardBottomAreaView getKeyguardBottomAreaView() {
        return this.mNotificationPanel.getKeyguardBottomAreaView();
    }

    public void goToLockedShade(View view) {
        NotificationEntry notificationEntry;
        if ((this.mDisabled2 & 4) == 0) {
            int currentUserId = this.mLockscreenUserManager.getCurrentUserId();
            if (view instanceof ExpandableNotificationRow) {
                notificationEntry = ((ExpandableNotificationRow) view).getEntry();
                notificationEntry.setUserExpanded(true, true);
                notificationEntry.setGroupExpansionChanging(true);
                StatusBarNotification statusBarNotification = notificationEntry.notification;
                if (statusBarNotification != null) {
                    currentUserId = statusBarNotification.getUserId();
                }
            } else {
                notificationEntry = null;
            }
            NotificationLockscreenUserManager notificationLockscreenUserManager = this.mLockscreenUserManager;
            boolean z = !notificationLockscreenUserManager.userAllowsPrivateNotificationsInPublic(notificationLockscreenUserManager.getCurrentUserId()) || !this.mLockscreenUserManager.shouldShowLockscreenNotifications() || this.mFalsingManager.shouldEnforceBouncer();
            if (!this.mLockscreenUserManager.isLockscreenPublicMode(currentUserId) || !z) {
                this.mNotificationPanel.animateToFullShade(0);
                this.mStatusBarStateController.setState(2);
            } else {
                this.mStatusBarStateController.setLeaveOpenOnKeyguardHide(true);
                showBouncerIfKeyguard();
                this.mDraggedDownEntry = notificationEntry;
                this.mPendingRemoteInputView = null;
            }
        }
    }

    public void goToKeyguard() {
        if (this.mState == 2) {
            this.mStatusBarStateController.setState(1);
        }
    }

    public void setBouncerShowing(boolean z) {
        this.mBouncerShowing = z;
        PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
        if (phoneStatusBarView != null) {
            phoneStatusBarView.setBouncerShowing(z);
        }
        updateHideIconsForBouncer(true);
        this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, true);
        updateScrimController();
        if (!this.mBouncerShowing) {
            updatePanelExpansionForKeyguard();
        }
    }

    /* access modifiers changed from: private */
    public void updateNotificationPanelTouchState() {
        boolean z = true;
        boolean z2 = isGoingToSleep() && !DozeParameters.getInstance(this.mContext).shouldControlScreenOff();
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        if ((this.mDeviceInteractive || this.mPulsing) && !z2) {
            z = false;
        }
        notificationPanelView.setTouchAndAnimationDisabled(z);
    }

    public int getWakefulnessState() {
        return this.mWakefulnessLifecycle.getWakefulness();
    }

    private void vibrateForCameraGesture() {
        if (!opVibrateForCameraGesture(this.mContext, this.mVibrator)) {
            this.mVibrator.vibrate(this.mCameraLaunchGestureVibePattern, -1);
        }
    }

    public boolean isScreenFullyOff() {
        return this.mScreenLifecycle.getScreenState() == 0;
    }

    public void showScreenPinningRequest(int i) {
        if (!this.mKeyguardMonitor.isShowing()) {
            showScreenPinningRequest(i, true);
        }
    }

    public void showScreenPinningRequest(int i, boolean z) {
        this.mScreenPinningRequest.showPrompt(i, z);
    }

    public boolean hasActiveNotifications() {
        return !this.mEntryManager.getNotificationData().getActiveNotifications().isEmpty();
    }

    public void appTransitionCancelled(int i) {
        if (i == this.mDisplayId) {
            ((Divider) getComponent(Divider.class)).onAppTransitionFinished();
        }
    }

    public void appTransitionFinished(int i) {
        if (i == this.mDisplayId) {
            ((Divider) getComponent(Divider.class)).onAppTransitionFinished();
        }
    }

    public void onCameraLaunchGestureDetected(int i) {
        onOnePlusCameraLaunchGestureDetected(i);
    }

    /* access modifiers changed from: 0000 */
    public boolean isCameraAllowedByAdmin() {
        boolean z = false;
        if (this.mDevicePolicyManager.getCameraDisabled(null, this.mLockscreenUserManager.getCurrentUserId())) {
            return false;
        }
        if (this.mStatusBarKeyguardViewManager != null && (!isKeyguardShowing() || !isKeyguardSecure())) {
            return true;
        }
        if ((this.mDevicePolicyManager.getKeyguardDisabledFeatures(null, this.mLockscreenUserManager.getCurrentUserId()) & 2) == 0) {
            z = true;
        }
        return z;
    }

    private boolean isGoingToSleep() {
        return this.mWakefulnessLifecycle.getWakefulness() == 3;
    }

    private boolean isWakingUpOrAwake() {
        if (this.mWakefulnessLifecycle.getWakefulness() == 2 || this.mWakefulnessLifecycle.getWakefulness() == 1) {
            return true;
        }
        return false;
    }

    public void notifyBiometricAuthModeChanged() {
        updateDozing();
        updateScrimController();
        this.mStatusBarWindow.onBiometricAuthModeChanged(this.mBiometricUnlockController.isWakeAndUnlock());
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void updateScrimController() {
        opUpdateScrimController();
    }

    public boolean isKeyguardShowing() {
        StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
        if (statusBarKeyguardViewManager != null) {
            return statusBarKeyguardViewManager.isShowing();
        }
        Slog.i("StatusBar", "isKeyguardShowing() called before startKeyguard(), returning true");
        return true;
    }

    public boolean shouldIgnoreTouch() {
        return isDozing() && this.mDozeServiceHost.mIgnoreTouchWhilePulsing;
    }

    public boolean isDeviceInteractive() {
        return this.mDeviceInteractive;
    }

    public void collapsePanel(boolean z) {
        if (z) {
            if (!collapsePanel()) {
                runPostCollapseRunnables();
            }
        } else if (!this.mPresenter.isPresenterFullyCollapsed()) {
            instantCollapseNotificationPanel();
            visibilityChanged(false);
        } else {
            runPostCollapseRunnables();
        }
    }

    public boolean collapsePanel() {
        if (this.mNotificationPanel.isFullyCollapsed()) {
            return false;
        }
        animateCollapsePanels(2, true, true);
        visibilityChanged(false);
        return true;
    }

    public void setNotificationSnoozed(StatusBarNotification statusBarNotification, SnoozeOption snoozeOption) {
        if (snoozeOption.getSnoozeCriterion() != null) {
            this.mNotificationListener.snoozeNotification(statusBarNotification.getKey(), snoozeOption.getSnoozeCriterion().getId());
        } else {
            this.mNotificationListener.snoozeNotification(statusBarNotification.getKey(), (long) (snoozeOption.getMinutesToSnoozeFor() * 60 * 1000));
        }
    }

    public void toggleSplitScreen() {
        toggleSplitScreenMode(-1, -1);
    }

    /* access modifiers changed from: 0000 */
    public void awakenDreams() {
        ((UiOffloadThread) Dependency.get(UiOffloadThread.class)).submit(new Runnable() {
            public final void run() {
                StatusBar.this.lambda$awakenDreams$31$StatusBar();
            }
        });
    }

    public /* synthetic */ void lambda$awakenDreams$31$StatusBar() {
        try {
            this.mDreamManager.awaken();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void preloadRecentApps() {
        this.mHandler.removeMessages(1022);
        this.mHandler.sendEmptyMessage(1022);
    }

    public void cancelPreloadRecentApps() {
        this.mHandler.removeMessages(1023);
        this.mHandler.sendEmptyMessage(1023);
    }

    public void dismissKeyboardShortcutsMenu() {
        this.mHandler.removeMessages(1027);
        this.mHandler.sendEmptyMessage(1027);
    }

    public void toggleKeyboardShortcutsMenu(int i) {
        this.mHandler.removeMessages(1026);
        this.mHandler.obtainMessage(1026, i, 0).sendToTarget();
    }

    public void setTopAppHidesStatusBar(boolean z) {
        this.mTopHidesStatusBar = z;
        if (!z && this.mWereIconsJustHidden) {
            this.mWereIconsJustHidden = false;
            this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, true);
        }
        updateHideIconsForBouncer(true);
    }

    /* access modifiers changed from: protected */
    public void toggleKeyboardShortcuts(int i) {
        KeyboardShortcuts.toggle(this.mContext, i);
    }

    /* access modifiers changed from: protected */
    public void dismissKeyboardShortcuts() {
        KeyboardShortcuts.dismiss();
    }

    public void onPanelLaidOut() {
        updateKeyguardMaxNotifications();
    }

    public void updateKeyguardMaxNotifications() {
        if (this.mState == 1 && this.mPresenter.getMaxNotificationsWhileLocked(false) != this.mPresenter.getMaxNotificationsWhileLocked(true)) {
            this.mViewHierarchyManager.updateRowStates();
        }
    }

    public void executeActionDismissingKeyguard(Runnable runnable, boolean z) {
        if (this.mDeviceProvisionedController.isDeviceProvisioned()) {
            dismissKeyguardThenExecute(new OnDismissAction(runnable) {
                private final /* synthetic */ Runnable f$1;

                {
                    this.f$1 = r2;
                }

                public final boolean onDismiss() {
                    return StatusBar.this.lambda$executeActionDismissingKeyguard$33$StatusBar(this.f$1);
                }
            }, z);
        }
    }

    public /* synthetic */ boolean lambda$executeActionDismissingKeyguard$33$StatusBar(Runnable runnable) {
        new Thread(new Runnable(runnable) {
            private final /* synthetic */ Runnable f$0;

            {
                this.f$0 = r1;
            }

            public final void run() {
                StatusBar.lambda$executeActionDismissingKeyguard$32(this.f$0);
            }
        }).start();
        return collapsePanel();
    }

    static /* synthetic */ void lambda$executeActionDismissingKeyguard$32(Runnable runnable) {
        try {
            ActivityManager.getService().resumeAppSwitches();
        } catch (RemoteException unused) {
        }
        runnable.run();
    }

    /* renamed from: startPendingIntentDismissingKeyguard */
    public void lambda$postStartActivityDismissingKeyguard$27$StatusBar(PendingIntent pendingIntent) {
        startPendingIntentDismissingKeyguard(pendingIntent, null);
    }

    public void startPendingIntentDismissingKeyguard(PendingIntent pendingIntent, Runnable runnable) {
        startPendingIntentDismissingKeyguard(pendingIntent, runnable, null);
    }

    public void startPendingIntentDismissingKeyguard(PendingIntent pendingIntent, Runnable runnable, View view) {
        executeActionDismissingKeyguard(new Runnable(pendingIntent, view, runnable) {
            private final /* synthetic */ PendingIntent f$1;
            private final /* synthetic */ View f$2;
            private final /* synthetic */ Runnable f$3;

            {
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r4;
            }

            public final void run() {
                StatusBar.this.lambda$startPendingIntentDismissingKeyguard$34$StatusBar(this.f$1, this.f$2, this.f$3);
            }
        }, pendingIntent.isActivity() && this.mActivityIntentHelper.wouldLaunchResolverActivity(pendingIntent.getIntent(), this.mLockscreenUserManager.getCurrentUserId()));
    }

    public /* synthetic */ void lambda$startPendingIntentDismissingKeyguard$34$StatusBar(PendingIntent pendingIntent, View view, Runnable runnable) {
        String str = "StatusBar";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("startPendingIntent: ");
            sb.append(pendingIntent);
            Log.d(str, sb.toString());
            pendingIntent.send(null, 0, null, null, null, null, getActivityOptions(this.mActivityLaunchAnimator.getLaunchAnimation(view, this.mShadeController.isOccluded())));
        } catch (CanceledException e) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Sending intent failed: ");
            sb2.append(e);
            Log.w(str, sb2.toString());
        }
        if (pendingIntent.isActivity()) {
            this.mAssistManager.hideAssist();
        }
        if (runnable != null) {
            postOnUiThread(runnable);
        }
    }

    private void postOnUiThread(Runnable runnable) {
        this.mMainThreadHandler.post(runnable);
    }

    public static Bundle getActivityOptions(RemoteAnimationAdapter remoteAnimationAdapter) {
        return OpStatusBar.getActivityOptionsInternal(remoteAnimationAdapter).toBundle();
    }

    /* access modifiers changed from: protected */
    public void visibilityChanged(boolean z) {
        if (this.mVisible != z) {
            this.mVisible = z;
            if (!z) {
                this.mGutsManager.closeAndSaveGuts(true, true, true, -1, -1, true);
            }
        }
        updateVisibleToUser();
    }

    /* access modifiers changed from: protected */
    public void updateVisibleToUser() {
        boolean z = this.mVisibleToUser;
        this.mVisibleToUser = this.mVisible && this.mDeviceInteractive;
        boolean z2 = this.mVisibleToUser;
        if (z != z2) {
            handleVisibleToUserChanged(z2);
        }
    }

    public void clearNotificationEffects() {
        if (!isNotificationLightBlinking()) {
            try {
                this.mBarService.clearNotificationEffects();
            } catch (RemoteException unused) {
            }
        }
    }

    /* access modifiers changed from: protected */
    public void notifyHeadsUpGoingToSleep() {
        maybeEscalateHeadsUp();
    }

    public boolean isBouncerShowing() {
        return this.mBouncerShowing;
    }

    public boolean isBouncerShowingScrimmed() {
        return isBouncerShowing() && this.mStatusBarKeyguardViewManager.bouncerNeedsScrimming();
    }

    public static PackageManager getPackageManagerForUser(Context context, int i) {
        if (i >= 0) {
            try {
                context = context.createPackageContextAsUser(context.getPackageName(), 4, new UserHandle(i));
            } catch (NameNotFoundException unused) {
            }
        }
        return context.getPackageManager();
    }

    public boolean isKeyguardSecure() {
        StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
        if (statusBarKeyguardViewManager != null) {
            return statusBarKeyguardViewManager.isSecure();
        }
        Slog.w("StatusBar", "isKeyguardSecure() called before startKeyguard(), returning false", new Throwable());
        return false;
    }

    public void showAssistDisclosure() {
        AssistManager assistManager = this.mAssistManager;
        if (assistManager != null) {
            assistManager.showDisclosure();
        }
    }

    public NotificationPanelView getPanel() {
        return this.mNotificationPanel;
    }

    public void startAssist(Bundle bundle) {
        AssistManager assistManager = this.mAssistManager;
        if (assistManager != null) {
            assistManager.startAssist(bundle);
        }
    }

    public NotificationGutsManager getGutsManager() {
        return this.mGutsManager;
    }

    public NotificationViewHierarchyManager getViewHierarchyManager() {
        return this.mViewHierarchyManager;
    }

    public int getStatusBarMode() {
        return this.mStatusBarMode;
    }

    public void notifyNavBarColorChanged(int i, String str) {
        if (OpNavBarUtils.isSupportCustomNavBar()) {
            this.mNavigationBarController.notifyNavBarColorChanged(i, str);
            PhoneStatusBarView phoneStatusBarView = this.mStatusBarView;
            if (phoneStatusBarView != null) {
                phoneStatusBarView.updateTopPackage(str);
            }
        }
    }

    private void initControlPanelWindow() {
        FrameLayout frameLayout = this.mGestureView;
        if (frameLayout != null) {
            this.mWindowManager.removeViewImmediate(frameLayout);
            this.mGestureView = null;
        }
        if (this.mContext.getDisplay().getRotation() == 0 && this.mHideNavBar) {
            this.mGestureView = new FrameLayout(this.mContext);
            this.mGestureView.setVisibility(0);
            this.mGestureView.setOnTouchListener(new OnTouchListener() {
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return true;
                }
            });
            int height = this.mContext.getDisplay().getHeight();
            int i = height / 100;
            LayoutParams layoutParams = new LayoutParams(-1, i, 0, height - i, 2014, 16777224, -3);
            this.mWindowManager.addView(this.mGestureView, layoutParams);
        }
    }

    public void onHideNavBar(boolean z) {
        this.mHideNavBar = z;
        StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
        if (statusBarKeyguardViewManager != null) {
            statusBarKeyguardViewManager.onHideNavBar(z);
        }
        initControlPanelWindow();
    }

    public boolean isHideNavBar() {
        return this.mHideNavBar;
    }

    public Bitmap getLockscreenWallpaper() {
        LockscreenWallpaper lockscreenWallpaper = this.mLockscreenWallpaper;
        if (lockscreenWallpaper == null) {
            return null;
        }
        return lockscreenWallpaper.getBitmap();
    }

    public int getScrimColor() {
        return this.mScrimController.getBackgroundColor();
    }

    public void setPanelViewAlpha(float f, boolean z, int i) {
        String str = "StatusBar";
        if (!z && OpLsState.getInstance().getPreventModeCtrl().isPreventModeNoBackground()) {
            Log.d(str, "not set alpha when prevent");
        } else if (this.mKeyguardMonitor.isLaunchTransitionFadingAway()) {
            Log.d(str, "Launch transition fadingAway, skip set panel alpha");
        } else {
            if (f <= 0.0f && z && this.mBouncerShowing) {
                this.mStatusBarKeyguardViewManager.reset(true);
            }
            if (this.mNotificationPanel != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("setPanelViewAlpha to ");
                sb.append(f);
                sb.append(", overlayLayout:");
                sb.append(z);
                sb.append(", currentType:");
                sb.append(i);
                Log.d(str, sb.toString());
                if (!OpUtils.isCustomFingerprint()) {
                    this.mNotificationPanel.setAlpha(f);
                } else if (f < 1.0f) {
                    if (i == OpKeyguardViewMediator.AUTHENTICATE_FACEUNLOCK || z) {
                        this.mNotificationPanel.setAlpha(f);
                    } else {
                        this.mNotificationPanel.setUnlockAlpha(f);
                    }
                } else if (!((OpChargingAnimationController) Dependency.get(OpChargingAnimationController.class)).isAnimationStarted() || i != OpKeyguardViewMediator.AUTHENTICATE_FINGERPRINT) {
                    this.mNotificationPanel.setAlpha(f);
                    this.mNotificationPanel.setUnlockAlpha(f);
                } else {
                    Log.d(str, "not set alpha when warp");
                }
            }
        }
    }

    public void setWallpaperAlpha(float f) {
        StatusBarWindowView statusBarWindowView = this.mStatusBarWindow;
        if (statusBarWindowView != null) {
            BackDropView backDropView = (BackDropView) statusBarWindowView.findViewById(R$id.backdrop);
            if (this.mScrimController != null) {
                if (f == 0.0f) {
                    boolean isFacelockUnlocking = KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockUnlocking();
                    if (!OpUtils.isHomeApp() || !this.mPowerManager.isInteractive() || isFacelockUnlocking) {
                        this.mScrimController.forceHideScrims(true, false);
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("setWallpaperAlpha isShowingWallpaper:");
                    sb.append(isShowingWallpaper());
                    sb.append(" mState:");
                    sb.append(this.mState);
                    Log.i("StatusBar", sb.toString());
                    if (isShowingWallpaper() || this.mScrimController.getState() != ScrimState.UNLOCKED) {
                        this.mScrimController.forceHideScrims(false, false);
                    } else {
                        this.mScrimController.forceHideScrims(false, true);
                    }
                }
            }
            if (f == 0.0f) {
                backDropView.animate().cancel();
                f = 0.002f;
            }
            backDropView.setAlpha(f);
        }
    }

    public void showNavigationBarGuide() {
        NavigationBarGuide navigationBarGuide = this.mNavigationBarGuide;
        if (navigationBarGuide != null) {
            navigationBarGuide.show();
        }
    }

    public void hideNavigationBarGuide() {
        NavigationBarGuide navigationBarGuide = this.mNavigationBarGuide;
        if (navigationBarGuide != null) {
            navigationBarGuide.hide();
        }
    }

    public void notifyPreventModeChange(boolean z) {
        KeyguardUpdateMonitor.getInstance(this.mContext).notifyPreventModeChange(z);
        if (this.mStatusBarKeyguardViewManager != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("notifyPreventModeChange, prevent:");
            sb.append(z);
            sb.append(", occluded:");
            sb.append(this.mStatusBarKeyguardViewManager.isOccluded());
            Log.d("StatusBar", sb.toString());
            if (this.mStatusBarKeyguardViewManager.isOccluded()) {
                if (z) {
                    instantExpandNotificationsPanel();
                } else {
                    instantCollapseNotificationPanel();
                }
            }
        }
        KeyguardViewMediator keyguardViewMediator = this.mKeyguardViewMediator;
        if (keyguardViewMediator != null) {
            keyguardViewMediator.notifyPreventModeChange(z);
        }
        KeyguardIndicationController keyguardIndicationController = this.mKeyguardIndicationController;
        if (keyguardIndicationController != null) {
            keyguardIndicationController.notifyPreventModeChange(z);
            StatusBarWindowController statusBarWindowController = this.mStatusBarWindowController;
            if (statusBarWindowController != null) {
                statusBarWindowController.setPreventModeActive(z);
            }
        }
    }

    public boolean isCameraForeground() {
        NotificationPanelView notificationPanelView = this.mNotificationPanel;
        if (notificationPanelView != null) {
            return notificationPanelView.isCameraForeground();
        }
        return false;
    }

    public void onFpPressedTimeOut() {
        this.mHandler.post(new Runnable() {
            public void run() {
                StatusBar statusBar = StatusBar.this;
                statusBar.mKeyguardIndicationController.showTransientIndication(statusBar.mContext.getResources().getString(17040025), Utils.getColorError(StatusBar.this.mContext));
                StatusBar.this.mKeyguardIndicationController.hideTransientIndicationDelayed(1000);
            }
        });
    }

    public boolean isShowingWallpaper() {
        StatusBarWindowController statusBarWindowController = this.mStatusBarWindowController;
        if (statusBarWindowController != null) {
            return statusBarWindowController.isShowingWallpaper();
        }
        return false;
    }

    public OpAodDisplayViewManager getAodDisplayViewManager() {
        return this.mAodDisplayViewManager;
    }

    public void passSystemUIEvent(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("passSystemUIEvent: ");
        sb.append(i);
        sb.append(", ");
        sb.append(this.mState);
        Log.d("StatusBar", sb.toString());
        KeyguardViewMediator keyguardViewMediator = this.mKeyguardViewMediator;
        if (keyguardViewMediator != null && this.mState == 1) {
            keyguardViewMediator.dismiss(null, null);
        }
    }

    public boolean isInMultiWindow() {
        return this.mIsInMultiWindow;
    }

    public void onOnePlusCameraLaunchGestureDetected(int i) {
        boolean z;
        this.mLastCameraLaunchSource = i;
        boolean isSimPinSecure = KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure();
        boolean isLockScreenDisabled = this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser());
        StringBuilder sb = new StringBuilder();
        sb.append("onCameraLaunch , ");
        sb.append(i);
        sb.append(", isSecure:");
        sb.append(this.mKeyguardMonitor.isSecure());
        sb.append(", interactive:");
        sb.append(this.mDeviceInteractive);
        sb.append(", isWake:");
        sb.append(isWakingUpOrAwake());
        sb.append(", expand:");
        sb.append(this.mExpandedVisible);
        sb.append(", occlude:");
        sb.append(this.mStatusBarKeyguardViewManager.isOccluded());
        sb.append(", simpin:");
        sb.append(isSimPinSecure);
        sb.append(", isLockDisabled:");
        sb.append(isLockScreenDisabled);
        String str = "StatusBar";
        Log.d(str, sb.toString());
        if (isSimPinSecure) {
            Log.d(str, "not launch camera for simpin");
            return;
        }
        boolean z2 = i == 11;
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        boolean z3 = (268435456 & i) != 0;
        if (isGoingToSleep()) {
            if (DEBUG_CAMERA_LIFT) {
                Slog.d(str, "Finish going to sleep before launching camera");
            }
            this.mLaunchCameraOnFinishedGoingToSleep = true;
            return;
        }
        String str2 = "com.android.systemui:CAMERA_GESTURE_CIRCLE";
        if (!this.mNotificationPanel.canCameraGestureBeLaunched(this.mStatusBarKeyguardViewManager.isShowing() && (this.mExpandedVisible || (this.mKeyguardMonitor.isSecure() && !this.mStatusBarKeyguardViewManager.isOccluded())))) {
            if (!this.mDeviceInteractive && z3) {
                powerManager.wakeUp(SystemClock.uptimeMillis(), str2);
            }
            if (DEBUG_CAMERA_LIFT) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Can't launch camera right now, mExpandedVisible: ");
                sb2.append(this.mExpandedVisible);
                Slog.d(str, sb2.toString());
            }
            return;
        }
        if (!this.mDeviceInteractive) {
            if (z3) {
                powerManager.wakeUp(SystemClock.uptimeMillis(), str2);
                StatusBarKeyguardViewManager statusBarKeyguardViewManager = this.mStatusBarKeyguardViewManager;
                if (statusBarKeyguardViewManager != null && statusBarKeyguardViewManager.isShowing()) {
                    this.mCameraWakeAndUnlocking = true;
                }
            } else {
                powerManager.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:CAMERA_GESTURE");
            }
        }
        if (!z3 && !z2) {
            vibrateForCameraGesture();
        }
        if (!this.mStatusBarKeyguardViewManager.isShowing()) {
            Intent intent = KeyguardBottomAreaView.INSECURE_CAMERA_INTENT;
            intent.putExtra("com.android.systemui.camera_launch_source_gesture", i);
            if (!isLockScreenDisabled || this.mState != 1 || !z3 || isWakingUpOrAwake()) {
                z = true;
            } else {
                Slog.d(str, "dismissShade to false");
                z = false;
            }
            Intent doubleTapPowerOpAppIntent = getDoubleTapPowerOpAppIntent(i);
            startActivityDismissingKeyguard(doubleTapPowerOpAppIntent != null ? doubleTapPowerOpAppIntent : intent, false, z, true, null, 0);
        } else {
            if (!this.mDeviceInteractive) {
                this.mGestureWakeLock.acquire(6000);
            }
            if (isWakingUpOrAwake() || z3) {
                if (DEBUG_CAMERA_LIFT) {
                    Slog.d(str, "Launching camera");
                }
                if (this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    this.mStatusBarKeyguardViewManager.reset(true);
                }
                this.mNotificationPanel.launchCamera(this.mDeviceInteractive, i);
                if (!z3 && !this.mKeyguardMonitor.isSecure()) {
                    this.mKeyguardViewMediatorCallback.startPowerKeyLaunchCamera();
                }
                updateScrimController();
            } else {
                if (DEBUG_CAMERA_LIFT) {
                    Slog.d(str, "Deferring until screen turns on");
                }
                this.mLaunchCameraWhenFinishedWaking = true;
            }
        }
    }

    public void createKeyguardIndication(LockIcon lockIcon) {
        KeyguardIndicationController keyguardIndicationController = this.mKeyguardIndicationController;
        if (keyguardIndicationController != null) {
            keyguardIndicationController.unregisterCallback();
        }
        this.mKeyguardIndicationController = SystemUIFactory.getInstance().createKeyguardIndicationController(this.mContext, (ViewGroup) this.mStatusBarWindow.findViewById(R$id.keyguard_indication_area), lockIcon, (KeyguardStatusView) getPanel().findViewById(R$id.keyguard_status_view), getKeyguardBottomAreaView());
        this.mKeyguardIndicationController.setStatusBarKeyguardViewManager(this.mStatusBarKeyguardViewManager);
        OpFacelockController opFacelockController = this.mOpFacelockController;
        if (opFacelockController != null) {
            opFacelockController.setKeyguardIndicationController(this.mKeyguardIndicationController);
        }
        this.mNotificationPanel.setKeyguardIndicationController(this.mKeyguardIndicationController);
    }

    public void notifyImeWindowVisibleStatus(int i, IBinder iBinder, int i2, int i3, boolean z) {
        int navBarMode = ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).getNavBarMode();
        if ((getNavigationBarHiddenMode() == 1 || !EdgeBackGestureHandler.sSideGestureEnabled) && QuickStepContract.isGesturalMode(navBarMode)) {
            notifyImeWindowVisible(i, iBinder, i2, i3, z);
        }
    }

    public void toggleWxBus() {
        String str = "StatusBar";
        Log.i(str, "toggleWxBus");
        boolean z = true;
        String str2 = "wxaf96fb8d548f8cb7";
        try {
            Class cls = Class.forName("com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram$Req");
            Object newInstance = cls.newInstance();
            OpReflectionUtils.setValue(cls, newInstance, "userName", "gh_3cf62f4f1d52");
            OpReflectionUtils.setValue(cls, newInstance, "path", "pages/qrcode/index?noback=1&attach=02_app_yijia&channel=app_yijia");
            OpReflectionUtils.setValue(cls, newInstance, "miniprogramType", Integer.valueOf(0));
            OpReflectionUtils.methodInvokeWithArgs(OpReflectionUtils.methodInvokeWithArgs(null, OpReflectionUtils.getMethodWithParams(Class.forName("com.tencent.mm.opensdk.openapi.WXAPIFactory"), "createWXAPI", Context.class, String.class), this.mContext, str2), OpReflectionUtils.getMethodWithParams(Class.forName("com.tencent.mm.opensdk.openapi.IWXAPI"), "sendReq", newInstance.getClass().getSuperclass()), newInstance);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e2) {
            e2.printStackTrace();
        } catch (InstantiationException e3) {
            e3.printStackTrace();
        } catch (IllegalAccessException e4) {
            e4.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("toggleWxBus, isSuccessGetJarAndCall:");
        sb.append(z);
        Log.i(str, sb.toString());
        z = false;
        StringBuilder sb2 = new StringBuilder();
        sb2.append("toggleWxBus, isSuccessGetJarAndCall:");
        sb2.append(z);
        Log.i(str, sb2.toString());
    }
}

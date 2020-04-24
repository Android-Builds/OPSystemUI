package com.oneplus.systemui.statusbar.phone;

import android.animation.AnimatorSet;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.service.notification.StatusBarNotification;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.Display;
import android.view.RemoteAnimationAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.RelativeLayout;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.SystemUI;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationViewHierarchyManager;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.DozeScrimController;
import com.android.systemui.statusbar.phone.EdgeBackGestureHandler;
import com.android.systemui.statusbar.phone.HeadsUpAppearanceController;
import com.android.systemui.statusbar.phone.HeadsUpManagerPhone;
import com.android.systemui.statusbar.phone.HighlightHintController;
import com.android.systemui.statusbar.phone.HighlightHintController.OnHighlightHintStateChangeListener;
import com.android.systemui.statusbar.phone.NavigationBarFragment;
import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.ScrimState;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.util.NotificationChannels;
import com.oneplus.aod.OpAodDisplayViewManager;
import com.oneplus.aod.OpAodUtils;
import com.oneplus.aod.OpAodWindowManager;
import com.oneplus.faceunlock.OpFacelockController;
import com.oneplus.networkspeed.NetworkSpeedController;
import com.oneplus.notification.OpNotificationController;
import com.oneplus.opzenmode.OpZenModeController;
import com.oneplus.opzenmode.OpZenModeController.Callback;
import com.oneplus.scene.OpSceneModeObserver;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;
import com.oneplus.util.SystemSetting;
import com.oneplus.util.ThemeColorUtils;
import com.oneplus.util.VibratorSceneUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class OpStatusBar extends SystemUI implements OnHighlightHintStateChangeListener, Callback, KeyguardMonitor.Callback {
    public static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    private static BroadcastReceiver sPkgReceiver = null;
    /* access modifiers changed from: private */
    public boolean isCTSStart = false;
    private int mAccentColor;
    /* access modifiers changed from: protected */
    public OpAodDisplayViewManager mAodDisplayViewManager;
    protected OpAodWindowManager mAodWindowManager;
    protected int mBackDisposition;
    /* access modifiers changed from: protected */
    public boolean mCameraWakeAndUnlocking = false;
    private final Runnable mCheckNavigationBarTask = new Runnable() {
        public final void run() {
            OpStatusBar.this.lambda$new$0$OpStatusBar();
        }
    };
    protected boolean mCustomDozing;
    /* access modifiers changed from: private */
    public int mDisableQs = 0;
    public ContentObserver mDisableQsObserver;
    /* access modifiers changed from: private */
    public int mDoubaleTapPowerApp = 0;
    private ContentObserver mDoubleTapPowerObserver;
    private ContentObserver mFullScreenGestureObserver;
    private boolean mGoogleDarkTheme;
    protected int mHighlightColor;
    protected int mImeDisplayId;
    private LayoutParams mImeNavLp;
    private boolean mImeShow = false;
    protected boolean mImeStateChange = false;
    protected int mImeVisibleState;
    private KeyguardMonitor mKeyguardMonitor;
    private int mLastBarHeight = -1;
    protected boolean mLastIsAppFullscreen = false;
    private long mLastUpdateNavBarTime = 0;
    private LayoutParams mNavLp;
    private boolean mNavShowing = true;
    /* access modifiers changed from: private */
    public int mNavType = 0;
    /* access modifiers changed from: private */
    public boolean mNeedShowOTAWizard = false;
    NetworkSpeedController mNetworkSpeedController;
    protected RelativeLayout mOPAodWindow;
    private boolean mOpDozingRequested;
    private OpEdgeBackGestureHandler mOpEdgeBackGestureHandler;
    protected OpFacelockController mOpFacelockController;
    private OpGestureButtonViewController mOpGestureButtonViewController;
    protected OpNotificationController mOpNotificationController;
    /* access modifiers changed from: protected */
    public OpSceneModeObserver mOpSceneModeObserver;
    protected OpWakingUpScrimController mOpWakingUpScrimController;
    protected int mOrientation;
    private ContentObserver mOtaWizardObserver;
    int mOutOfBoundThenRelayoutTimes = 0;
    /* access modifiers changed from: private */
    public boolean mPreHideAod = false;
    private boolean mQsDisabled = false;
    private int mRotation;
    protected boolean mShowImeSwitcher;
    private boolean mSpecialTheme;
    /* access modifiers changed from: protected */
    public boolean mStartDozingRequested;
    protected TelecomManager mTelecomManager;
    private int mThemeColor;
    private SystemSetting mThemeSetting;
    protected IBinder mToken;
    private AnimatorSet mWakingUpAnimation;
    protected boolean mWakingUpAnimationStart = false;
    private WindowManager mWindowManager;

    public interface OpDozeCallbacks {
        ArrayList<DozeHost.Callback> getCallbacks();

        void fireThreeKeyChanged(int i) {
            Iterator it = getCallbacks().iterator();
            while (it.hasNext()) {
                ((DozeHost.Callback) it.next()).onThreeKeyChanged(i);
            }
        }

        void fireSingleTap() {
            Iterator it = getCallbacks().iterator();
            while (it.hasNext()) {
                ((DozeHost.Callback) it.next()).onSingleTap();
            }
        }

        void fireFingerprintPoke() {
            Iterator it = getCallbacks().iterator();
            while (it.hasNext()) {
                ((DozeHost.Callback) it.next()).onFingerprintPoke();
            }
        }
    }

    public boolean isCameraNotchIgnoreSetting() {
        return false;
    }

    public void start() {
        ThemeColorUtils.init(this.mContext);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mOtaWizardObserver = new ContentObserver(getHandler()) {
            public void onChange(boolean z) {
                boolean z2 = false;
                if (Global.getInt(OpStatusBar.this.mContext.getContentResolver(), "oneplus_need_show_ota_wizard", 0) == 1) {
                    z2 = true;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("needShowOTAWizard: ");
                sb.append(OpStatusBar.this.mNeedShowOTAWizard);
                sb.append(" to ");
                sb.append(z2);
                Log.d("OpStatusBar", sb.toString());
                if (OpStatusBar.this.mNeedShowOTAWizard != z2) {
                    OpStatusBar.this.mNeedShowOTAWizard = z2;
                    if (OpStatusBar.this.getNotificationPanelView() != null) {
                        OpStatusBar.this.getNotificationPanelView().setShowOTAWizard(z2);
                    }
                }
            }
        };
        this.mOtaWizardObserver.onChange(true);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("oneplus_need_show_ota_wizard"), true, this.mOtaWizardObserver, -1);
        this.mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        this.mKeyguardMonitor.addCallback(this);
        this.mFullScreenGestureObserver = new ContentObserver(getHandler()) {
            public void onChange(boolean z) {
                int i = 0;
                int intForUser = System.getIntForUser(OpStatusBar.this.mContext.getContentResolver(), "oneplus_fullscreen_gesture_type", 0, -2);
                StringBuilder sb = new StringBuilder();
                sb.append("gesture type ");
                sb.append(OpStatusBar.this.mNavType);
                sb.append(" to ");
                sb.append(intForUser);
                String str = "OpStatusBar";
                Log.d(str, sb.toString());
                OpStatusBar.this.mNavType = intForUser;
                boolean z2 = System.getIntForUser(OpStatusBar.this.mContext.getContentResolver(), "op_gesture_button_side_enabled", 1, -2) != 0;
                if (EdgeBackGestureHandler.sSideGestureEnabled != z2) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("gesture side to ");
                    sb2.append(z2);
                    Log.d(str, sb2.toString());
                    EdgeBackGestureHandler.sSideGestureEnabled = z2;
                }
                int navBarMode = ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).getNavBarMode();
                if ((OpStatusBar.this.mNavType == 1 || !EdgeBackGestureHandler.sSideGestureEnabled) && QuickStepContract.isGesturalMode(navBarMode)) {
                    i = 1;
                }
                System.putInt(OpStatusBar.this.mContext.getContentResolver(), "buttons_show_on_screen_navkeys", i ^ 1);
                OpStatusBar.this.checkNavigationBarState();
            }
        };
        Log.d("OpStatusBar", "FullScreenGestureObserver internal onChange");
        this.mFullScreenGestureObserver.onChange(true);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("oneplus_fullscreen_gesture_type"), true, this.mFullScreenGestureObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("op_gesture_button_side_enabled"), true, this.mFullScreenGestureObserver, -1);
        this.mDoubleTapPowerObserver = new ContentObserver(getHandler()) {
            public void onChange(boolean z) {
                int intForUser = Secure.getIntForUser(OpStatusBar.this.mContext.getContentResolver(), "op_app_double_tap_power_gesture", 0, -2);
                StringBuilder sb = new StringBuilder();
                sb.append("DoubleTapPower: app=");
                sb.append(intForUser);
                Log.i("OpStatusBar", sb.toString());
                OpStatusBar.this.mDoubaleTapPowerApp = intForUser;
            }
        };
        this.mDoubleTapPowerObserver.onChange(true);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("op_app_double_tap_power_gesture"), true, this.mDoubleTapPowerObserver, -1);
        this.mDisableQsObserver = new ContentObserver(getHandler()) {
            public void onChange(boolean z) {
                ContentResolver contentResolver = OpStatusBar.this.mContext.getContentResolver();
                OpStatusBar.this.getKeyguardUpdateMonitor();
                int intForUser = Secure.getIntForUser(contentResolver, "oneplus_disable_qs_when_locked", 0, KeyguardUpdateMonitor.getCurrentUser());
                StringBuilder sb = new StringBuilder();
                sb.append("disable qs:");
                sb.append(intForUser);
                sb.append(", ");
                OpStatusBar.this.getKeyguardUpdateMonitor();
                sb.append(KeyguardUpdateMonitor.getCurrentUser());
                Log.d("OpStatusBar", sb.toString());
                OpStatusBar.this.mDisableQs = intForUser;
                OpStatusBar.this.updateQsEnabled();
            }
        };
        this.mDisableQsObserver.onChange(true);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("oneplus_disable_qs_when_locked"), true, this.mDisableQsObserver, -1);
        ((OpZenModeController) Dependency.get(OpZenModeController.class)).addCallback(this);
        this.mThemeColor = OpUtils.getThemeColor(this.mContext);
        this.mAccentColor = OpUtils.getThemeAccentColor(this.mContext, R$color.qs_tile_icon);
        ThemeColorUtils.init(this.mContext);
        this.mSpecialTheme = OpUtils.isSpecialTheme(this.mContext);
        this.mGoogleDarkTheme = OpUtils.isGoogleDarkTheme(this.mContext);
        this.mOpNotificationController = (OpNotificationController) Dependency.get(OpNotificationController.class);
        OpNotificationController opNotificationController = this.mOpNotificationController;
        if (opNotificationController != null) {
            opNotificationController.setEntryManager(getEntryManager());
        }
        this.mOpSceneModeObserver = (OpSceneModeObserver) Dependency.get(OpSceneModeObserver.class);
        OpUtils.init(this.mContext);
        SystemSetting systemSetting = this.mThemeSetting;
        if (systemSetting != null) {
            systemSetting.setListening(false);
        }
        C21055 r3 = new SystemSetting(this.mContext, null, "oem_black_mode", true) {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                OpStatusBar.this.getHandler().post(new Runnable() {
                    public void run() {
                        if (OpStatusBar.DEBUG) {
                            Log.d("OpStatusBar", "theme setting changed.");
                        }
                        OpStatusBar.this.checkIfThemeChanged();
                    }
                });
            }
        };
        this.mThemeSetting = r3;
        this.mThemeSetting.setListening(true);
        this.mOpWakingUpScrimController = new OpWakingUpScrimController(this.mContext);
        initDetectCTSReceiver();
        this.mTelecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        String str = "NavigationBar";
        if (this.mNavLp == null) {
            LayoutParams layoutParams = new LayoutParams(-1, -1, 2019, 545521768, -3);
            this.mNavLp = layoutParams;
            this.mNavLp.token = new Binder();
            LayoutParams layoutParams2 = this.mNavLp;
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(this.mContext.getDisplayId());
            layoutParams2.setTitle(sb.toString());
            this.mNavLp.accessibilityTitle = this.mContext.getString(R$string.nav_bar);
            LayoutParams layoutParams3 = this.mNavLp;
            layoutParams3.windowAnimations = 0;
            layoutParams3.privateFlags |= 16777216;
        }
        if (this.mImeNavLp == null) {
            LayoutParams layoutParams4 = new LayoutParams(-1, this.mContext.getResources().getDimensionPixelSize(17105285), 2017, 545522024, -3);
            this.mImeNavLp = layoutParams4;
            this.mImeNavLp.token = new Binder();
            LayoutParams layoutParams5 = this.mImeNavLp;
            StringBuilder sb2 = new StringBuilder();
            sb2.append(str);
            sb2.append(this.mContext.getDisplayId());
            layoutParams5.setTitle(sb2.toString());
            this.mImeNavLp.accessibilityTitle = this.mContext.getString(R$string.nav_bar);
            LayoutParams layoutParams6 = this.mImeNavLp;
            layoutParams6.windowAnimations = 0;
            layoutParams6.gravity = 80;
            layoutParams6.privateFlags = 16777216 | layoutParams6.privateFlags;
        }
        this.mOpGestureButtonViewController = new OpGestureButtonViewController(this.mContext);
    }

    private void initDetectCTSReceiver() {
        if (sPkgReceiver == null) {
            sPkgReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String str = "OpStatusBar";
                    try {
                        String action = intent.getAction();
                        context.getContentResolver();
                        Uri data = intent.getData();
                        if (data != null) {
                            String schemeSpecificPart = data.getSchemeSpecificPart();
                            if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                                if ("com.android.compatibility.common.deviceinfo".equals(schemeSpecificPart)) {
                                    OpStatusBar.this.isCTSStart = true;
                                    OpUtils.setCTSAdded(true);
                                    Log.i(str, "isSpecial case start");
                                }
                            } else if ("com.android.tradefed.utils.wifi".equals(schemeSpecificPart)) {
                                OpStatusBar.this.isCTSStart = false;
                                OpUtils.setCTSAdded(false);
                                Log.i(str, "isSpecial case end");
                            }
                        }
                    } catch (Exception e) {
                        Log.w(str, "sPkgReceiver error.", e);
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addDataScheme("package");
            this.mContext.registerReceiver(sPkgReceiver, intentFilter);
        }
    }

    /* access modifiers changed from: protected */
    public void startInner() {
        int currentUserId = getLockscreenUserManager().getCurrentUserId();
        OpAodUtils.checkAodSettingsState(this.mContext, currentUserId);
        OpAodUtils.checkAodStyle(this.mContext, currentUserId);
    }

    public Intent getDoubleTapPowerOpAppIntent(int i) {
        Intent intent = null;
        if (i == 1) {
            int i2 = this.mDoubaleTapPowerApp;
            if (i2 != 0) {
                if (i2 == 1) {
                    Intent intent2 = new Intent("coloros.wallet.intent.action.OPEN");
                    intent2.setFlags(268468224);
                    intent = intent2;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("DoubleTapPower: getDoubleTapPowerOpAppIntent() ");
                sb.append(intent);
                Log.i("OpStatusBar", sb.toString());
            }
        }
        return intent;
    }

    /* access modifiers changed from: protected */
    public void makeStatusBarView(Context context) {
        this.mNetworkSpeedController = (NetworkSpeedController) Dependency.get(NetworkSpeedController.class);
        getNetworkController().setNetworkSpeedController(this.mNetworkSpeedController);
        this.mOrientation = this.mContext.getResources().getConfiguration().orientation;
        setUpHighlightHintInfo();
    }

    public int getNavigationBarHiddenMode() {
        return this.mNavType;
    }

    public void checkNavigationBarState() {
        long uptimeMillis = SystemClock.uptimeMillis();
        long j = uptimeMillis - this.mLastUpdateNavBarTime;
        StringBuilder sb = new StringBuilder();
        sb.append("updateNavigationBar: now=");
        sb.append(uptimeMillis);
        sb.append(", last=");
        sb.append(this.mLastUpdateNavBarTime);
        String str = "OpStatusBar";
        Log.d(str, sb.toString());
        if (getHandler().hasCallbacks(this.mCheckNavigationBarTask)) {
            Log.i(str, "checkNavigationBarState: already scheduled, skip.");
        } else if (j < 1000) {
            getHandler().postAtTime(this.mCheckNavigationBarTask, this.mLastUpdateNavBarTime + 1000);
        } else {
            getHandler().post(this.mCheckNavigationBarTask);
        }
    }

    public /* synthetic */ void lambda$new$0$OpStatusBar() {
        int navBarMode = ((OverviewProxyService) Dependency.get(OverviewProxyService.class)).getNavBarMode();
        View navView = ((NavigationBarController) Dependency.get(NavigationBarController.class)).getNavView();
        boolean z = navView != null && navView.isAttachedToWindow();
        StringBuilder sb = new StringBuilder();
        sb.append("checkNavBarState mode: ");
        sb.append(navBarMode);
        sb.append(", type: ");
        sb.append(this.mNavType);
        sb.append(", showing: ");
        sb.append(this.mNavShowing);
        sb.append(", attached:");
        sb.append(z);
        sb.append(", view:");
        sb.append(navView != null);
        sb.append(", ImeShow: ");
        sb.append(this.mImeShow);
        String str = "OpStatusBar";
        Log.d(str, sb.toString());
        boolean z2 = this.mNavShowing;
        if ((this.mNavType != 1 && EdgeBackGestureHandler.sSideGestureEnabled) || !QuickStepContract.isGesturalMode(navBarMode)) {
            if (navView != null && !navView.isAttachedToWindow() && !this.mNavShowing) {
                Log.d(str, "NavBar add it");
                ViewGroup.LayoutParams layoutParams = this.mNavLp;
                if (layoutParams == null) {
                    layoutParams = navView.getLayoutParams();
                }
                this.mWindowManager.addView(navView, layoutParams);
                this.mNavShowing = true;
            }
            disableGestureHandler();
        } else if (!this.mImeShow) {
            if (z && this.mNavShowing) {
                Log.d(str, "NavBar remove it");
                this.mWindowManager.removeViewImmediate(navView);
                this.mNavShowing = false;
            }
            enableGestureHandler();
            updateImeWindowStatus();
        } else if (this.mOrientation == 1) {
            if (navView != null && !navView.isAttachedToWindow() && !this.mNavShowing) {
                Log.d(str, "NavBar add it for IME show");
                this.mWindowManager.addView(navView, this.mImeNavLp);
                this.mNavShowing = true;
            }
            disableGestureHandler();
        } else {
            if (z && this.mNavShowing) {
                Log.d(str, "NavBar remove it for IME show in landscape mode");
                this.mWindowManager.removeViewImmediate(navView);
                this.mNavShowing = false;
            }
            enableGestureHandler();
            updateImeWindowStatus();
        }
        if (!(z2 == this.mNavShowing || getStatusBarKeyguardViewManager() == null)) {
            getStatusBarKeyguardViewManager().onHideNavBar(!this.mNavShowing);
        }
        this.mLastUpdateNavBarTime = SystemClock.uptimeMillis();
    }

    public boolean checkGestureStartAssist(Bundle bundle) {
        if (this.mNavShowing) {
            return false;
        }
        getHandler().post(new Runnable(bundle) {
            private final /* synthetic */ Bundle f$0;

            {
                this.f$0 = r1;
            }

            public final void run() {
                ((AssistManager) Dependency.get(AssistManager.class)).startAssist(this.f$0);
            }
        });
        Log.d("OpStatusBar", "startAssist");
        return true;
    }

    public void updateQsEnabled() {
        String str = "OpStatusBar";
        if (this.mDisableQs != 1 || getStatusBarKeyguardViewManager() == null || !getStatusBarKeyguardViewManager().isShowing() || !isKeyguardSecure()) {
            if (this.mQsDisabled) {
                Log.d(str, "enable QS");
            }
            this.mQsDisabled = false;
            return;
        }
        if (!this.mQsDisabled) {
            Log.d(str, "disable QS");
        }
        this.mQsDisabled = true;
    }

    private boolean isKeyguardSecure() {
        return ((Boolean) OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "isKeyguardSecure", new Object[0])).booleanValue();
    }

    public boolean isQsDisabled() {
        return this.mQsDisabled;
    }

    public void onKeyguardShowingChanged() {
        updateQsEnabled();
    }

    /* access modifiers changed from: protected */
    public void inflateOPAodView(Context context) {
        this.mOPAodWindow = (RelativeLayout) View.inflate(context, R$layout.op_aod_view, null);
    }

    /* access modifiers changed from: protected */
    public void opOnDensityOrFontScaleChanged() {
        inflateOPAodView(this.mContext);
        this.mAodWindowManager.unregisterCallback();
        this.mAodWindowManager = new OpAodWindowManager(this.mContext, this.mOPAodWindow);
        this.mAodDisplayViewManager.onDensityOrFontScaleChanged(this.mOPAodWindow);
    }

    public void onWakingAndUnlocking() {
        OpAodWindowManager opAodWindowManager = this.mAodWindowManager;
        if (opAodWindowManager != null) {
            opAodWindowManager.onWakingAndUnlocking();
            Log.d("OpStatusBar", "onWakingAndUnlocking");
            if (this.mAodWindowManager.isWakingAndUnlockByFP()) {
                checkToStopDozing();
            }
        }
    }

    public void launchHighlightHintAp() {
        Log.d("OpStatusBar", "launchHighlightHintAp");
        StatusBarNotification highlightHintNotification = getEntryManager().getNotificationData().getHighlightHintNotification();
        Intent intentOnStatusBar = highlightHintNotification != null ? highlightHintNotification.getNotification().getIntentOnStatusBar() : null;
        if (intentOnStatusBar != null) {
            this.mContext.startActivityAsUser(intentOnStatusBar, new UserHandle(UserHandle.CURRENT.getIdentifier()));
        }
    }

    public void onHighlightHintStateChange() {
        this.mHighlightColor = ((HighlightHintController) Dependency.get(HighlightHintController.class)).getHighlighColor();
        checkBarModes();
        setUpHighlightHintInfo();
    }

    /* access modifiers changed from: protected */
    public void setUpHighlightHintInfo() {
        if (((HighlightHintController) Dependency.get(HighlightHintController.class)).showOvalLayout() && this.mOrientation == 2 && getPanel() != null) {
            getPanel().setUpHighlightHintInfo();
        }
    }

    public void onHighlightHintInfoChange() {
        this.mHighlightColor = ((HighlightHintController) Dependency.get(HighlightHintController.class)).getHighlighColor();
        checkBarModes();
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0026  */
    /* JADX WARNING: Removed duplicated region for block: B:13:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0021  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void opUpdateDozing() {
        /*
            r3 = this;
            boolean r0 = r3.mOpDozingRequested
            r1 = 0
            if (r0 != 0) goto L_0x0016
            com.android.systemui.statusbar.phone.BiometricUnlockController r0 = r3.getBiometricUnlockController()
            int r0 = r0.getMode()
            r3.getBiometricUnlockController()
            r2 = 2
            if (r0 != r2) goto L_0x0014
            goto L_0x0016
        L_0x0014:
            r0 = r1
            goto L_0x0017
        L_0x0016:
            r0 = 1
        L_0x0017:
            com.android.systemui.statusbar.phone.BiometricUnlockController r2 = r3.getBiometricUnlockController()
            boolean r2 = r2.isWakeAndUnlock()
            if (r2 == 0) goto L_0x0022
            r0 = r1
        L_0x0022:
            boolean r1 = r3.mCustomDozing
            if (r1 == r0) goto L_0x0038
            r3.mCustomDozing = r0
            com.android.systemui.statusbar.phone.DozeScrimController r0 = r3.getDozeScrimController()
            boolean r1 = r3.mCustomDozing
            r0.setDozing(r1)
            com.android.systemui.statusbar.notification.NotificationEntryManager r3 = r3.getEntryManager()
            r3.updateNotifications()
        L_0x0038:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.systemui.statusbar.phone.OpStatusBar.opUpdateDozing():void");
    }

    /* access modifiers changed from: protected */
    public void checkToStartDozing() {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("checkToStartDozing mStartDozingRequested=");
            sb.append(this.mStartDozingRequested);
            sb.append(" mOpDozingRequested=");
            sb.append(this.mOpDozingRequested);
            Log.d("OpStatusBar", sb.toString());
        }
        if (this.mStartDozingRequested && !this.mOpDozingRequested) {
            this.mOpDozingRequested = true;
            this.mAodWindowManager.startDozing();
            this.mAodDisplayViewManager.startDozing();
            DozeLog.traceDozing(this.mContext, isDozing());
            opUpdateDozing();
            updateIsKeyguard();
        }
    }

    public OpAodWindowManager getAodWindowManager() {
        return this.mAodWindowManager;
    }

    /* access modifiers changed from: protected */
    public void checkToStopDozing() {
        String str = "OpStatusBar";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("checkToStopDozing mOpDozingRequested=");
            sb.append(this.mOpDozingRequested);
            sb.append(":");
            sb.append(this.isCTSStart);
            Log.d(str, sb.toString());
        }
        if (this.isCTSStart) {
            if (this.mOpDozingRequested) {
                this.mAodWindowManager.stopDozing();
                this.mAodDisplayViewManager.stopDozing();
                this.mAodDisplayViewManager.resetStatus();
            }
            this.mOpWakingUpScrimController.removeFromWindow(true);
        } else if (this.mOpDozingRequested) {
            getKeyguardUpdateMonitor().isFingerprintAlreadyAuthenticated();
            AnimatorSet aodDisappearAnimation = this.mAodWindowManager.getAodDisappearAnimation();
            if (this.mWakingUpAnimationStart && aodDisappearAnimation != null && !aodDisappearAnimation.isStarted()) {
                Log.d(str, "WakingUpAnimation already started. remove aod window directly");
                this.mAodWindowManager.stopDozing();
                this.mAodDisplayViewManager.stopDozing();
                this.mAodDisplayViewManager.resetStatus();
            }
            startWakingUpAnimation();
        }
        if (this.mOpDozingRequested) {
            this.mOpDozingRequested = false;
            DozeLog.traceDozing(this.mContext, isDozing());
            updateDozing();
        }
    }

    public void onFingerprintAuthenticated() {
        String str = "OpStatusBar";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onFingerprintAuthenticated mOpDozingRequested=");
            sb.append(this.mOpDozingRequested);
            sb.append(" playAodWakingUpAnimation=");
            sb.append(this.mAodDisplayViewManager.playAodWakingUpAnimation());
            Log.d(str, sb.toString());
        }
        if (this.mOpDozingRequested && this.mAodDisplayViewManager.playAodWakingUpAnimation()) {
            this.mPreHideAod = true;
            if (DEBUG) {
                Log.d(str, "onFingerprintAuthenticated");
            }
            this.mAodDisplayViewManager.onPlayFingerprintUnlockAnimation(true);
            startWakingUpAnimation();
            this.mAodWindowManager.stopDozing();
            this.mAodDisplayViewManager.stopDozing();
            this.mAodDisplayViewManager.resetStatus();
        }
    }

    public void opOnStartedGoingToSleep() {
        this.mPreHideAod = false;
        String str = "OpStatusBar";
        Log.d(str, "opOnStartedGoingToSleep");
        AnimatorSet animatorSet = this.mWakingUpAnimation;
        if (animatorSet != null) {
            animatorSet.cancel();
        }
        this.mWakingUpAnimationStart = false;
        this.mAodDisplayViewManager.onPlayFingerprintUnlockAnimation(false);
        if (!this.isCTSStart) {
            this.mOpWakingUpScrimController.prepare();
        } else {
            Log.w(str, "don't request show wakingUpScrim for PIP window test");
        }
    }

    public void opOnFinishedWakingUp() {
        Log.d("OpStatusBar", "opOnFinishedWakingUp");
        startWakingUpAnimation();
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x004f  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x00ad  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x00b9  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x0105  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void startWakingUpAnimation() {
        /*
            r9 = this;
            boolean r0 = r9.mWakingUpAnimationStart
            java.lang.String r1 = "OpStatusBar"
            if (r0 == 0) goto L_0x000c
            java.lang.String r9 = "don't startWakingUpAnimation since animation started"
            android.util.Log.d(r1, r9)
            return
        L_0x000c:
            r0 = 1
            r9.mWakingUpAnimationStart = r0
            com.oneplus.aod.OpAodDisplayViewManager r2 = r9.mAodDisplayViewManager
            boolean r2 = r2.playAodWakingUpAnimation()
            com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager r3 = r9.getStatusBarKeyguardViewManager()
            boolean r3 = r3.isShowing()
            boolean r4 = r9.getBouncerShowing()
            r5 = 0
            if (r4 != 0) goto L_0x0042
            boolean r4 = r9.getIsOccluded()
            if (r4 != 0) goto L_0x0042
            if (r3 == 0) goto L_0x0042
            com.oneplus.faceunlock.OpFacelockController r4 = r9.mOpFacelockController
            if (r4 == 0) goto L_0x0042
            boolean r4 = r4.isScreenOffUnlock()
            if (r4 != 0) goto L_0x0042
            com.android.keyguard.KeyguardUpdateMonitor r4 = r9.getKeyguardUpdateMonitor()
            boolean r4 = r4.isCameraLaunched()
            if (r4 != 0) goto L_0x0042
            r4 = r0
            goto L_0x0043
        L_0x0042:
            r4 = r5
        L_0x0043:
            com.android.keyguard.KeyguardUpdateMonitor r6 = r9.getKeyguardUpdateMonitor()
            boolean r6 = r6.isFingerprintAlreadyAuthenticated()
            boolean r7 = DEBUG
            if (r7 == 0) goto L_0x00a9
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "startWakingUpAnimation isFingerprintUnlock:"
            r7.append(r8)
            r7.append(r6)
            java.lang.String r8 = " bouncerShow:"
            r7.append(r8)
            boolean r8 = r9.getBouncerShowing()
            r7.append(r8)
            java.lang.String r8 = " isShowing:"
            r7.append(r8)
            r7.append(r3)
            java.lang.String r8 = " getIsOccluded:"
            r7.append(r8)
            boolean r8 = r9.getIsOccluded()
            r7.append(r8)
            java.lang.String r8 = " playAodWakingUpAnimation:"
            r7.append(r8)
            r7.append(r2)
            java.lang.String r8 = " isScreenOffUnlock:"
            r7.append(r8)
            com.oneplus.faceunlock.OpFacelockController r8 = r9.mOpFacelockController
            boolean r8 = r8.isScreenOffUnlock()
            r7.append(r8)
            java.lang.String r8 = " isCameraLaunched:"
            r7.append(r8)
            com.android.keyguard.KeyguardUpdateMonitor r8 = r9.getKeyguardUpdateMonitor()
            boolean r8 = r8.isCameraLaunched()
            r7.append(r8)
            java.lang.String r7 = r7.toString()
            android.util.Log.i(r1, r7)
        L_0x00a9:
            android.animation.AnimatorSet r7 = r9.mWakingUpAnimation
            if (r7 == 0) goto L_0x00b0
            r7.cancel()
        L_0x00b0:
            android.animation.AnimatorSet r7 = new android.animation.AnimatorSet
            r7.<init>()
            r9.mWakingUpAnimation = r7
            if (r2 == 0) goto L_0x0105
            com.oneplus.aod.OpAodWindowManager r2 = r9.mAodWindowManager
            android.animation.AnimatorSet r2 = r2.getAodDisappearAnimation()
            com.android.systemui.statusbar.phone.NotificationPanelView r3 = r9.getNotificationPanelView()
            android.animation.AnimatorSet r3 = r3.getNotificationAppearAnimation()
            if (r6 != 0) goto L_0x00e2
            if (r4 == 0) goto L_0x00e2
            com.oneplus.systemui.statusbar.phone.OpWakingUpScrimController r4 = r9.mOpWakingUpScrimController
            r4.startAnimation(r0)
            android.animation.AnimatorSet r4 = r9.mWakingUpAnimation
            r6 = 2
            android.animation.Animator[] r6 = new android.animation.Animator[r6]
            r6[r5] = r3
            r6[r0] = r2
            r4.playTogether(r6)
            java.lang.String r0 = "mWakingUpAnimation case 1"
            android.util.Log.i(r1, r0)
            goto L_0x00f5
        L_0x00e2:
            android.animation.AnimatorSet r3 = r9.mWakingUpAnimation
            r3.play(r2)
            com.oneplus.systemui.statusbar.phone.OpWakingUpScrimController r2 = r9.mOpWakingUpScrimController
            if (r6 != 0) goto L_0x00ec
            goto L_0x00ed
        L_0x00ec:
            r0 = r5
        L_0x00ed:
            r2.startAnimation(r0)
            java.lang.String r0 = "mWakingUpAnimation case 2"
            android.util.Log.i(r1, r0)
        L_0x00f5:
            android.animation.AnimatorSet r0 = r9.mWakingUpAnimation
            com.oneplus.systemui.statusbar.phone.OpStatusBar$7 r1 = new com.oneplus.systemui.statusbar.phone.OpStatusBar$7
            r1.<init>()
            r0.addListener(r1)
            android.animation.AnimatorSet r9 = r9.mWakingUpAnimation
            r9.start()
            goto L_0x014f
        L_0x0105:
            com.oneplus.aod.OpAodWindowManager r2 = r9.mAodWindowManager
            r2.stopDozing()
            com.oneplus.aod.OpAodDisplayViewManager r2 = r9.mAodDisplayViewManager
            r2.stopDozing()
            com.oneplus.aod.OpAodDisplayViewManager r2 = r9.mAodDisplayViewManager
            r2.resetStatus()
            java.lang.String r2 = "stopDozing"
            android.util.Log.i(r1, r2)
            com.android.systemui.statusbar.phone.NotificationPanelView r2 = r9.getNotificationPanelView()
            android.animation.AnimatorSet r2 = r2.getNotificationAppearAnimation()
            if (r3 == 0) goto L_0x0145
            com.android.keyguard.KeyguardUpdateMonitor r3 = r9.getKeyguardUpdateMonitor()
            boolean r3 = r3.isCameraLaunched()
            if (r3 != 0) goto L_0x0145
            if (r4 == 0) goto L_0x013a
            android.animation.AnimatorSet r0 = r9.mWakingUpAnimation
            r0.play(r2)
            android.animation.AnimatorSet r0 = r9.mWakingUpAnimation
            r0.start()
        L_0x013a:
            com.oneplus.systemui.statusbar.phone.OpWakingUpScrimController r9 = r9.mOpWakingUpScrimController
            r9.startAnimation(r5)
            java.lang.String r9 = "mWakingUpAnimation case 3"
            android.util.Log.i(r1, r9)
            goto L_0x014f
        L_0x0145:
            com.oneplus.systemui.statusbar.phone.OpWakingUpScrimController r9 = r9.mOpWakingUpScrimController
            r9.removeFromWindow(r0)
            java.lang.String r9 = "mWakingUpAnimation case 4"
            android.util.Log.i(r1, r9)
        L_0x014f:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.systemui.statusbar.phone.OpStatusBar.startWakingUpAnimation():void");
    }

    /* access modifiers changed from: protected */
    public void writeDisableFlagdbg(int i, int i2, StringBuilder sb) {
        sb.append((i & 1073741824) != 0 ? 'V' : 'v');
        sb.append((1073741824 & i2) != 0 ? '!' : ' ');
    }

    /* access modifiers changed from: protected */
    public void disable(int i, int i2) {
        if ((i2 & 1073741824) != 0 && getNavigationBarView() != null) {
            getNavigationBarView().getRootView().setVisibility((i & 1073741824) != 0 ? 8 : 0);
        }
    }

    public void onBrickModeChanged(boolean z) {
        if (getHeadsUpAppearanceController() != null) {
            getHeadsUpAppearanceController().updateTopEntry();
        }
        getKeyguardUpdateMonitor().onBrickModeChanged(z);
        if (z) {
            checkToStopDozing();
        } else {
            checkToStartDozing();
        }
    }

    public void flingToStartFacelock() {
        OpFacelockController opFacelockController = this.mOpFacelockController;
        if (opFacelockController != null) {
            opFacelockController.tryToStartFaceLockInBouncer();
        }
    }

    public void onEmptySpaceClick() {
        boolean z = true;
        boolean z2 = getNotificationPanelView() != null;
        if (getNotificationPanelView().isTracking()) {
            z = false;
        }
        if (z2 && z) {
            getNotificationPanelView().onEmptySpaceClicked(0.0f, 0.0f);
        }
    }

    /* access modifiers changed from: protected */
    public void onNotificationShelfClicked() {
        NotificationPanelView notificationPanelView = getNotificationPanelView();
        if (!notificationPanelView.isExpanding() && notificationPanelView.isQsExpanded() && notificationPanelView.isFullyExpanded()) {
            notificationPanelView.flingSettings(0.0f, 1);
        }
    }

    public void onPackagePreferencesCleared() {
        NotificationChannels.createAll(this.mContext);
    }

    public boolean notifyCameraLaunching(String str) {
        OpFacelockController opFacelockController = this.mOpFacelockController;
        if (opFacelockController != null) {
            return opFacelockController.notifyCameraLaunching(true, str);
        }
        return false;
    }

    public OpFacelockController getFacelockController() {
        return this.mOpFacelockController;
    }

    public void startFacelockFailAnimation() {
        getNotificationPanelView();
    }

    public void notifyBarHeightChange(int i) {
        NotificationPanelView notificationPanelView = getNotificationPanelView();
        if (!(getKeyguardViewMediator() == null || this.mLastBarHeight == i || notificationPanelView == null)) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("BarHeight change to ");
                sb.append(i);
                sb.append(", alpha:");
                sb.append(notificationPanelView.getAlpha());
                Log.d("OpStatusBar", sb.toString());
            }
            if (DEBUG && getStatusBarWindowController() != null) {
                getStatusBarWindowController().debugBarHeight();
            }
            if (notificationPanelView.getAlpha() < 1.0f && i != -1) {
                getKeyguardViewMediator().notifyBarHeightChange(i);
            } else if (!(i == -1 || getScrimController() == null)) {
                getScrimController().resetForceHide();
            }
        }
        this.mLastBarHeight = i;
    }

    public void onThreeKeyStatus(final int i) {
        if (OpAodUtils.isMotionAwakeOn() || OpAodUtils.isSingleTapEnabled()) {
            getHandler().post(new Runnable() {
                public void run() {
                    if (OpStatusBar.this.getDozeServiceHost() != null) {
                        OpStatusBar.this.getDozeServiceHost().fireThreeKeyChanged(i);
                    }
                }
            });
        }
    }

    public void onSingleTap() {
        getHandler().post(new Runnable() {
            public void run() {
                if (OpStatusBar.this.getDozeServiceHost() != null) {
                    OpStatusBar.this.getDozeServiceHost().fireSingleTap();
                }
            }
        });
    }

    public void onFingerprintPoke() {
        getHandler().post(new Runnable() {
            public void run() {
                if (OpStatusBar.this.getDozeServiceHost() != null) {
                    OpStatusBar.this.getDozeServiceHost().fireFingerprintPoke();
                }
            }
        });
    }

    public boolean shouldHideSensitive(NotificationEntry notificationEntry) {
        return ((NotificationViewHierarchyManager) Dependency.get(NotificationViewHierarchyManager.class)).shouldHideSensitive(notificationEntry, KeyguardUpdateMonitor.getCurrentUser());
    }

    /* access modifiers changed from: protected */
    public void opOnUiModeChanged() {
        if (DEBUG) {
            Log.d("OpStatusBar", "opOnUiModeChanged");
        }
        checkIfThemeChanged();
    }

    /* access modifiers changed from: protected */
    public void opOnConfigChanged(Configuration configuration) {
        String str = "OpStatusBar";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("opOnConfigChanged newConfig ");
            sb.append(configuration);
            Log.d(str, sb.toString());
        }
        int i = this.mOrientation;
        int i2 = configuration.orientation;
        if (i != i2) {
            this.mOrientation = i2;
        }
        setUpHighlightHintInfo();
        checkIfThemeChanged();
        this.mAodDisplayViewManager.onConfigChanged(configuration);
        if (this.mOpEdgeBackGestureHandler != null) {
            if (DEBUG) {
                Log.d(str, "OpEdgeBackGestureHandler onConfigurationChanged ");
            }
            this.mOpEdgeBackGestureHandler.onConfigurationChanged(((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getRotation());
        }
        if (this.mImeNavLp != null) {
            this.mImeNavLp.height = this.mContext.getResources().getDimensionPixelSize(17105285);
        }
    }

    /* access modifiers changed from: private */
    public void checkIfThemeChanged() {
        int themeColor = OpUtils.getThemeColor(this.mContext);
        int themeAccentColor = OpUtils.getThemeAccentColor(this.mContext, R$color.qs_tile_icon);
        boolean isSpecialTheme = OpUtils.isSpecialTheme(this.mContext);
        boolean isGoogleDarkTheme = OpUtils.isGoogleDarkTheme(this.mContext);
        Log.d("OpStatusBar", String.format("mThemeColor=0x%x -> 0x%x, mAccentColor=0x%x -> 0x%x, mSpecialTheme=%b -> %b, mGoogleDarkTheme=%b -> %b", new Object[]{Integer.valueOf(this.mThemeColor), Integer.valueOf(themeColor), Integer.valueOf(this.mAccentColor), Integer.valueOf(themeAccentColor), Boolean.valueOf(this.mSpecialTheme), Boolean.valueOf(isSpecialTheme), Boolean.valueOf(this.mGoogleDarkTheme), Boolean.valueOf(isGoogleDarkTheme)}));
        if (this.mThemeColor != themeColor || this.mAccentColor != themeAccentColor || this.mSpecialTheme != isSpecialTheme || this.mGoogleDarkTheme != isGoogleDarkTheme) {
            this.mThemeColor = themeColor;
            this.mAccentColor = themeAccentColor;
            this.mSpecialTheme = isSpecialTheme;
            ThemeColorUtils.init(this.mContext);
            if (this.mGoogleDarkTheme == isGoogleDarkTheme) {
                FragmentHostManager.get(getStatusBarWindow()).reloadFragments();
            }
            this.mGoogleDarkTheme = isGoogleDarkTheme;
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mOpFacelockController != null) {
            printWriter.println();
            this.mOpFacelockController.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
        }
        if (this.mAodDisplayViewManager != null) {
            printWriter.println();
            this.mAodDisplayViewManager.dump(printWriter);
            printWriter.println();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(" mDisableQs:");
        sb.append(this.mDisableQs);
        printWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append(" mNavType:");
        sb2.append(this.mNavType);
        printWriter.println(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append(" notificationBGColor:");
        sb3.append(this.mContext.getColor(R$color.notification_material_background_color));
        printWriter.println(sb3.toString());
    }

    /* access modifiers changed from: protected */
    public boolean opVibrateForCameraGesture(Context context, Vibrator vibrator) {
        if (!OpUtils.isSupportLinearVibration()) {
            return false;
        }
        if (VibratorSceneUtils.isVibratorSceneSupported(context, 1024)) {
            VibratorSceneUtils.doVibrateWithSceneIfNeeded(context, vibrator, 1024);
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void opUpdatePanelExpansionForKeyguard() {
        StringBuilder sb = new StringBuilder();
        sb.append(" getFaceLockMode:");
        sb.append(getBiometricUnlockController().getFaceLockMode());
        sb.append(" isFingerprintAlreadyAuthenticated");
        sb.append(KeyguardUpdateMonitor.getInstance(this.mContext).isFingerprintAlreadyAuthenticated());
        Log.i("OpStatusBar", sb.toString());
        if (getState() == 1 && getBiometricUnlockController().getMode() != 1 && !getBouncerShowing() && getBiometricUnlockController().getFaceLockMode() != 6 && !KeyguardUpdateMonitor.getInstance(this.mContext).isFingerprintAlreadyAuthenticated()) {
            instantExpandNotificationsPanel();
        } else if (getState() == 3) {
            instantCollapseNotificationPanel();
        }
    }

    public void removeHeadsUps() {
        HeadsUpManagerPhone headsUpManager = getHeadsUpManager();
        if (headsUpManager != null) {
            headsUpManager.releaseAllImmediately();
        }
        OpNotificationController opNotificationController = this.mOpNotificationController;
        if (opNotificationController != null) {
            opNotificationController.hideSimpleHeadsUps();
        }
    }

    public ActivityLaunchAnimator getActivityLaunchAnimator() {
        return (ActivityLaunchAnimator) OpReflectionUtils.getValue(StatusBar.class, this, "mActivityLaunchAnimator");
    }

    public static ActivityOptions getActivityOptionsInternal(RemoteAnimationAdapter remoteAnimationAdapter) {
        ActivityOptions activityOptions;
        if (remoteAnimationAdapter != null) {
            activityOptions = ActivityOptions.makeRemoteAnimation(remoteAnimationAdapter);
        } else {
            activityOptions = ActivityOptions.makeBasic();
        }
        activityOptions.setLaunchWindowingMode(4);
        return activityOptions;
    }

    /* access modifiers changed from: protected */
    public void opUpdateScrimController() {
        Trace.beginSection("StatusBar#updateScrimController");
        boolean isWakeAndUnlock = getBiometricUnlockController().isWakeAndUnlock();
        ScrimController scrimController = getScrimController();
        scrimController.setExpansionAffectsAlpha(!getBiometricUnlockController().isBiometricUnlock());
        boolean isLaunchingAffordanceWithPreview = getNotificationPanelView().isLaunchingAffordanceWithPreview();
        scrimController.setLaunchingAffordanceWithPreview(isLaunchingAffordanceWithPreview);
        if (getBouncerShowing() || ((getBiometricUnlockController().getFaceLockMode() == 6 || getBiometricUnlockController().getMode() == 6) && isKeyguardShowing())) {
            scrimController.transitionTo(getStatusBarKeyguardViewManager().bouncerNeedsScrimming() ? ScrimState.BOUNCER_SCRIMMED : ScrimState.BOUNCER);
        } else if ((isInLaunchTransition() && !getIsKeyguard()) || isLaunchCameraWhenFinishedWaking() || isLaunchingAffordanceWithPreview) {
            scrimController.transitionTo(ScrimState.UNLOCKED, getUnlockScrimCallback());
        } else if (isBrightnessMirrorVisible()) {
            scrimController.transitionTo(ScrimState.BRIGHTNESS_MIRROR);
        } else if (isPulsing()) {
            scrimController.transitionTo(ScrimState.PULSING, getDozeScrimController().getScrimCallback());
        } else if (isDozing() && !isWakeAndUnlock) {
            scrimController.transitionTo(ScrimState.AOD);
        } else if (getIsKeyguard() && !isWakeAndUnlock) {
            scrimController.transitionTo(ScrimState.KEYGUARD);
        } else if (getBubbleController().isStackExpanded()) {
            scrimController.transitionTo(ScrimState.BUBBLE_EXPANDED);
        } else {
            scrimController.transitionTo(ScrimState.UNLOCKED, getUnlockScrimCallback());
        }
        Trace.endSection();
    }

    public void onWallpaperChange(Bitmap bitmap) {
        if (getStatusBarWindowController() != null) {
            getStatusBarWindowController().setLockscreenWallpaper(bitmap != null);
        } else {
            Log.v("OpStatusBar", "getStatusBarWindowController is null");
        }
    }

    /* access modifiers changed from: private */
    public NotificationPanelView getNotificationPanelView() {
        return (NotificationPanelView) OpReflectionUtils.getValue(StatusBar.class, this, "mNotificationPanel");
    }

    public View getOpStatusBarView() {
        return (View) OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "getStatusBarView", new Object[0]);
    }

    private NotificationEntryManager getEntryManager() {
        return (NotificationEntryManager) OpReflectionUtils.getValue(StatusBar.class, this, "mEntryManager");
    }

    private HeadsUpManagerPhone getHeadsUpManager() {
        return (HeadsUpManagerPhone) OpReflectionUtils.getValue(StatusBar.class, this, "mHeadsUpManager");
    }

    private NotificationPanelView getPanel() {
        return (NotificationPanelView) OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "getPanel", new Object[0]);
    }

    private void checkBarModes() {
        OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "checkBarModes", new Object[0]);
    }

    /* access modifiers changed from: private */
    public Handler getHandler() {
        return (Handler) OpReflectionUtils.getValue(StatusBar.class, this, "mHandler");
    }

    private BiometricUnlockController getBiometricUnlockController() {
        return (BiometricUnlockController) OpReflectionUtils.getValue(StatusBar.class, this, "mBiometricUnlockController");
    }

    private KeyguardViewMediator getKeyguardViewMediator() {
        return (KeyguardViewMediator) OpReflectionUtils.getValue(StatusBar.class, this, "mKeyguardViewMediator");
    }

    public StatusBarWindowController getStatusBarWindowController() {
        return (StatusBarWindowController) OpReflectionUtils.getValue(StatusBar.class, this, "mStatusBarWindowController");
    }

    private ScrimController getScrimController() {
        return (ScrimController) OpReflectionUtils.getValue(StatusBar.class, this, "mScrimController");
    }

    private boolean isDozing() {
        return ((Boolean) OpReflectionUtils.getValue(StatusBar.class, this, "mDozing")).booleanValue();
    }

    /* access modifiers changed from: protected */
    public boolean isNotificationLightBlinking() {
        OpNotificationController opNotificationController = this.mOpNotificationController;
        return opNotificationController != null && opNotificationController.keepNotificationLightBlinking(getEntryManager().getNotificationData().getActiveNotifications());
    }

    public boolean needRefreshExpanded() {
        if (getNotificationPanelView() == null || (((OverviewProxyService) Dependency.get(OverviewProxyService.class)).getSystemUiStateFlags() & 4) == 0 || getNotificationPanelView().isFullyExpanded()) {
            return false;
        }
        return true;
    }

    public int getSystemIconAreaMaxWidth(int i) {
        int i2;
        PhoneStatusBarView phoneStatusBarView = (PhoneStatusBarView) getOpStatusBarView();
        int i3 = 0;
        if (phoneStatusBarView == null) {
            return 0;
        }
        ViewGroup viewGroup = (ViewGroup) phoneStatusBarView.findViewById(R$id.status_bar_contents);
        if (viewGroup != null) {
            View findViewById = viewGroup.findViewById(R$id.clock);
            View findViewById2 = viewGroup.findViewById(R$id.system_icon_area);
            if (!(findViewById == null || findViewById2 == null)) {
                View findViewById3 = findViewById2.findViewById(R$id.battery);
                int paddingStart = viewGroup != null ? viewGroup.getPaddingStart() : 0;
                int paddingEnd = viewGroup != null ? viewGroup.getPaddingEnd() : 0;
                boolean isHighLightHintShow = ((HighlightHintController) Dependency.get(HighlightHintController.class)).isHighLightHintShow();
                int layoutDirection = phoneStatusBarView.getLayoutDirection();
                int maxDotsForNotificationIconContainer = OpUtils.getMaxDotsForNotificationIconContainer(this.mContext);
                Context context = this.mContext;
                int dimensionPixelSize = (context == null || maxDotsForNotificationIconContainer <= 0) ? 0 : context.getResources().getDimensionPixelSize(R$dimen.status_bar_icon_size) * maxDotsForNotificationIconContainer;
                int width = viewGroup.getWidth();
                int measuredWidth = paddingStart + paddingEnd + findViewById3.getMeasuredWidth() + findViewById.getMeasuredWidth();
                if (isHighLightHintShow) {
                    i3 = phoneStatusBarView.getHighlightHintWidth();
                }
                i2 = width - ((measuredWidth + i3) + dimensionPixelSize);
                return i2;
            }
        }
        i2 = -1;
        return i2;
    }

    private boolean updateIsKeyguard() {
        return ((Boolean) OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "updateIsKeyguard", new Object[0])).booleanValue();
    }

    private void updateDozing() {
        OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "updateDozing", new Object[0]);
    }

    private DozeScrimController getDozeScrimController() {
        return (DozeScrimController) OpReflectionUtils.getValue(StatusBar.class, this, "mDozeScrimController");
    }

    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor getKeyguardUpdateMonitor() {
        return (KeyguardUpdateMonitor) OpReflectionUtils.getValue(StatusBar.class, this, "mKeyguardUpdateMonitor");
    }

    private HeadsUpAppearanceController getHeadsUpAppearanceController() {
        return (HeadsUpAppearanceController) OpReflectionUtils.getValue(StatusBar.class, this, "mHeadsUpAppearanceController");
    }

    private NavigationBarView getNavigationBarView() {
        return (NavigationBarView) OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "getNavigationBarView", new Object[0]);
    }

    private NetworkController getNetworkController() {
        return (NetworkController) OpReflectionUtils.getValue(StatusBar.class, this, "mNetworkController");
    }

    /* access modifiers changed from: private */
    public OpDozeCallbacks getDozeServiceHost() {
        return (OpDozeCallbacks) OpReflectionUtils.getValue(StatusBar.class, this, "mDozeServiceHost");
    }

    private StatusBarWindowView getStatusBarWindow() {
        return (StatusBarWindowView) OpReflectionUtils.getValue(StatusBar.class, this, "mStatusBarWindow");
    }

    private boolean getIsOccluded() {
        return ((Boolean) OpReflectionUtils.getValue(StatusBar.class, this, "mIsOccluded")).booleanValue();
    }

    private boolean getBouncerShowing() {
        return ((Boolean) OpReflectionUtils.getValue(StatusBar.class, this, "mBouncerShowing")).booleanValue();
    }

    private StatusBarKeyguardViewManager getStatusBarKeyguardViewManager() {
        return (StatusBarKeyguardViewManager) OpReflectionUtils.getValue(StatusBar.class, this, "mStatusBarKeyguardViewManager");
    }

    private void instantExpandNotificationsPanel() {
        OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "instantExpandNotificationsPanel", new Object[0]);
    }

    private void instantCollapseNotificationPanel() {
        OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "instantCollapseNotificationPanel", new Object[0]);
    }

    private int getState() {
        return ((Integer) OpReflectionUtils.getValue(StatusBar.class, this, "mState")).intValue();
    }

    private boolean isKeyguardShowing() {
        return ((Boolean) OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "isKeyguardShowing", new Object[0])).booleanValue();
    }

    private boolean isInLaunchTransition() {
        return ((Boolean) OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "isInLaunchTransition", new Object[0])).booleanValue();
    }

    private boolean isLaunchCameraWhenFinishedWaking() {
        return ((Boolean) OpReflectionUtils.getValue(StatusBar.class, this, "mLaunchCameraWhenFinishedWaking")).booleanValue();
    }

    private ScrimController.Callback getUnlockScrimCallback() {
        return (ScrimController.Callback) OpReflectionUtils.getValue(StatusBar.class, this, "mUnlockScrimCallback");
    }

    private boolean isBrightnessMirrorVisible() {
        return ((Boolean) OpReflectionUtils.getValue(StatusBar.class, this, "mBrightnessMirrorVisible")).booleanValue();
    }

    private boolean isPulsing() {
        return ((Boolean) OpReflectionUtils.methodInvokeVoid(StatusBar.class, this, "isPulsing", new Object[0])).booleanValue();
    }

    private boolean getIsKeyguard() {
        return ((Boolean) OpReflectionUtils.getValue(StatusBar.class, this, "mIsKeyguard")).booleanValue();
    }

    private BubbleController getBubbleController() {
        return (BubbleController) OpReflectionUtils.getValue(StatusBar.class, this, "mBubbleController");
    }

    public int getDisableFlag() {
        return ((Integer) OpReflectionUtils.getValue(StatusBar.class, this, "mDisabled1")).intValue();
    }

    public int getSystemUiVisibility() {
        return ((Integer) OpReflectionUtils.getValue(StatusBar.class, this, "mSystemUiVisibility")).intValue();
    }

    private NotificationLockscreenUserManager getLockscreenUserManager() {
        return (NotificationLockscreenUserManager) OpReflectionUtils.getValue(StatusBar.class, this, "mLockscreenUserManager");
    }

    /* access modifiers changed from: protected */
    public void notifyImeWindowVisible(int i, IBinder iBinder, int i2, int i3, boolean z) {
        this.mImeDisplayId = i;
        this.mToken = iBinder;
        this.mImeVisibleState = i2;
        this.mBackDisposition = i3;
        this.mShowImeSwitcher = z;
        this.mImeShow = (i2 & 2) != 0;
        this.mImeStateChange = true;
        if (!getHandler().hasCallbacks(this.mCheckNavigationBarTask)) {
            Log.d("OpStatusBar", "Check navigation bar state when ime state change");
            checkNavigationBarState();
        }
    }

    public void updateImeWindowStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("updateImeWindowStatus ImeStateChange ");
        sb.append(this.mImeStateChange);
        sb.append(" , imeShow ");
        sb.append(this.mImeShow);
        Log.d("OpStatusBar", sb.toString());
        if (this.mImeStateChange) {
            NavigationBarFragment defaultNavigationBarFragment = ((NavigationBarController) Dependency.get(NavigationBarController.class)).getDefaultNavigationBarFragment();
            if (defaultNavigationBarFragment != null) {
                defaultNavigationBarFragment.setImeWindowStatus(this.mImeDisplayId, this.mToken, this.mImeVisibleState, this.mBackDisposition, this.mShowImeSwitcher);
            }
            this.mImeStateChange = false;
        }
    }

    public boolean isImeStateChange() {
        return this.mImeStateChange;
    }

    private void enableGestureHandler() {
        OverviewProxyService overviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        int navBarMode = overviewProxyService.getNavBarMode();
        Display defaultDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        String str = "OpStatusBar";
        if (this.mOpEdgeBackGestureHandler == null) {
            if (DEBUG) {
                Log.d(str, "OpStatusBar new OpEdgeBackGestureHandler when NavBar hidden");
            }
            this.mOpEdgeBackGestureHandler = new OpEdgeBackGestureHandler(this.mContext, overviewProxyService);
            Context currentUserContext = ((NavigationModeController) Dependency.get(NavigationModeController.class)).getCurrentUserContext();
            if (currentUserContext != null) {
                this.mOpEdgeBackGestureHandler.onNavigationModeChanged(navBarMode, currentUserContext);
            }
            this.mOpEdgeBackGestureHandler.onNavigationBarHidden();
            this.mOpEdgeBackGestureHandler.onConfigurationChanged(defaultDisplay.getRotation());
            this.mRotation = defaultDisplay.getRotation();
            return;
        }
        if (DEBUG) {
            Log.d(str, "OpEdgeBackGestureHandler update window");
        }
        if (this.mRotation != defaultDisplay.getRotation()) {
            this.mOpEdgeBackGestureHandler.onConfigurationChanged(defaultDisplay.getRotation());
            this.mRotation = defaultDisplay.getRotation();
        }
    }

    private void disableGestureHandler() {
        if (this.mOpEdgeBackGestureHandler != null) {
            if (DEBUG) {
                Log.d("OpStatusBar", "OpStatusBar release OpEdgeBackGestureHandler when NavBar show");
            }
            this.mOpEdgeBackGestureHandler.onNavigationBarShow();
            this.mOpEdgeBackGestureHandler = null;
        }
    }

    public OpGestureButtonViewController getGestureButtonController() {
        return this.mOpGestureButtonViewController;
    }

    /* access modifiers changed from: protected */
    public boolean isNavShowing() {
        return this.mNavShowing;
    }
}

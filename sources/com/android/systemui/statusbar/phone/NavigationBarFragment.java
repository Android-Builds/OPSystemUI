package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.AccessibilityServicesStateChangeListener;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.LatencyTracker;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.ScreenDecorations;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.fragments.FragmentHostManager.FragmentListener;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.recents.OverviewProxyService.OverviewProxyListener;
import com.android.systemui.recents.Recents;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.NavigationBarController;
import com.android.systemui.statusbar.phone.ContextualButton.ContextButtonListener;
import com.android.systemui.statusbar.phone.NavigationBarView.OnVerticalChangedListener;
import com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener;
import com.android.systemui.statusbar.policy.AccessibilityManagerWrapper;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.android.systemui.util.LifecycleFragment;
import com.oneplus.scene.OpSceneModeObserver;
import com.oneplus.util.OpNavBarUtils;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.function.Consumer;

public class NavigationBarFragment extends LifecycleFragment implements Callbacks, ModeChangedListener {
    /* access modifiers changed from: private */
    public static Context sContext;
    private final AccessibilityServicesStateChangeListener mAccessibilityListener;
    /* access modifiers changed from: private */
    public AccessibilityManager mAccessibilityManager;
    private final AccessibilityManagerWrapper mAccessibilityManagerWrapper;
    private final ContentObserver mAssistContentObserver;
    protected final AssistManager mAssistManager;
    /* access modifiers changed from: private */
    public boolean mAssistantAvailable;
    private final Runnable mAutoDim;
    private AutoHideController mAutoHideController;
    private int mBackupNavBarMode;
    private final BroadcastReceiver mBroadcastReceiver;
    final Runnable mCheckNavigationBarState;
    private CommandQueue mCommandQueue;
    /* access modifiers changed from: private */
    public ContentResolver mContentResolver;
    private final DeviceProvisionedController mDeviceProvisionedController;
    private int mDisabledFlags1;
    private int mDisabledFlags2;
    public int mDisplayId;
    private Divider mDivider;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mHideNavBar;
    public boolean mHomeBlockedThisTouch;
    /* access modifiers changed from: private */
    public boolean mImeShow;
    private boolean mIsInBrickMode;
    private boolean mIsOnDefaultDisplay;
    private long mLastLockToAppLongPress;
    private int mLayoutDirection;
    private LightBarController mLightBarController;
    private Locale mLocale;
    private MagnificationContentObserver mMagnificationObserver;
    private final MetricsLogger mMetricsLogger;
    private OnClickListener mNavActionListener;
    /* access modifiers changed from: private */
    public int mNavBarMode;
    private NavBarSettingObserver mNavBarSettingObserver;
    private int mNavigationBarColor;
    private int mNavigationBarMode;
    protected NavigationBarView mNavigationBarView = null;
    private int mNavigationBarWindowState;
    private int mNavigationIconHints;
    private int mOrientation;
    private final OverviewProxyListener mOverviewProxyListener;
    /* access modifiers changed from: private */
    public OverviewProxyService mOverviewProxyService;
    private Recents mRecents;
    private final ContextButtonListener mRotationButtonListener;
    private final Consumer<Integer> mRotationWatcher;
    private ScreenDecorations mScreenDecorations;
    /* access modifiers changed from: private */
    public StatusBar mStatusBar;
    private final StatusBarStateController mStatusBarStateController;
    private int mSystemUiVisibility;
    private WindowManager mWindowManager;

    private class MagnificationContentObserver extends ContentObserver {
        public MagnificationContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean z) {
            NavigationBarFragment navigationBarFragment = NavigationBarFragment.this;
            navigationBarFragment.updateAccessibilityServicesState(navigationBarFragment.mAccessibilityManager);
        }
    }

    private class NavBarSettingObserver extends ContentObserver {
        public NavBarSettingObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean z) {
            if (Build.DEBUG_ONEPLUS) {
                Log.d("NavigationBar", "mNavBarSettingObserver onChange");
            }
            if (NavigationBarFragment.this.mContentResolver != null) {
                boolean z2 = true;
                boolean z3 = !(NavigationBarFragment.this.mOverviewProxyService != null ? NavigationBarFragment.this.mOverviewProxyService.shouldShowSwipeUpUI() : false) && QuickStepContract.isLegacyMode(NavigationBarFragment.this.mNavBarMode) && System.getInt(NavigationBarFragment.this.mContentResolver, "op_gesture_button_enabled", 0) == 1;
                NavigationBarView navigationBarView = NavigationBarFragment.this.mNavigationBarView;
                if (navigationBarView != null) {
                    navigationBarView.updateNavButtonState(z3);
                }
                String str = "systemui_navigation_bar_hided";
                if (!z3) {
                    System.putInt(NavigationBarFragment.this.mContentResolver, str, 0);
                    NavigationBarFragment.this.mHideNavBar = false;
                } else {
                    NavigationBarFragment navigationBarFragment = NavigationBarFragment.this;
                    if (System.getInt(navigationBarFragment.mContentResolver, str, 0) != 1) {
                        z2 = false;
                    }
                    navigationBarFragment.mHideNavBar = z2;
                }
                NavigationBarFragment.this.onHideNavBar(false);
            }
        }
    }

    private int barMode(int i) {
        if ((134217728 & i) != 0) {
            return 1;
        }
        if ((Integer.MIN_VALUE & i) != 0) {
            return 2;
        }
        if ((i & 134217729) == 134217729) {
            return 6;
        }
        if ((32768 & i) != 0) {
            return 4;
        }
        return (i & 1) != 0 ? 3 : 0;
    }

    public /* synthetic */ void lambda$new$0$NavigationBarFragment(ContextualButton contextualButton, boolean z) {
        if (z) {
            this.mAutoHideController.touchAutoHide();
        }
    }

    public /* synthetic */ void lambda$new$1$NavigationBarFragment() {
        getBarTransitions().setAutoDim(true);
    }

    public NavigationBarFragment(AccessibilityManagerWrapper accessibilityManagerWrapper, DeviceProvisionedController deviceProvisionedController, MetricsLogger metricsLogger, AssistManager assistManager, OverviewProxyService overviewProxyService, NavigationModeController navigationModeController, StatusBarStateController statusBarStateController) {
        boolean z = false;
        this.mNavigationBarWindowState = 0;
        this.mNavigationIconHints = 0;
        this.mSystemUiVisibility = 0;
        this.mNavBarMode = 0;
        this.mHandler = (Handler) Dependency.get(Dependency.MAIN_HANDLER);
        this.mHideNavBar = false;
        this.mNavigationBarColor = 0;
        this.mIsInBrickMode = false;
        this.mBackupNavBarMode = -1;
        this.mImeShow = false;
        this.mOverviewProxyListener = new OverviewProxyListener() {
            public void onConnectionChanged(boolean z) {
                NavigationBarFragment.this.mNavigationBarView.updateStates();
                NavigationBarFragment.this.updateScreenPinningGestures();
                if (z) {
                    NavigationBarFragment navigationBarFragment = NavigationBarFragment.this;
                    navigationBarFragment.sendAssistantAvailability(navigationBarFragment.mAssistantAvailable);
                }
            }

            public void startAssistant(Bundle bundle) {
                NavigationBarFragment.this.mAssistManager.startAssist(bundle);
            }

            public void onNavBarButtonAlphaChanged(float f, boolean z) {
                ButtonDispatcher buttonDispatcher;
                if (QuickStepContract.isSwipeUpMode(NavigationBarFragment.this.mNavBarMode)) {
                    buttonDispatcher = NavigationBarFragment.this.mNavigationBarView.getBackButton();
                } else if (QuickStepContract.isGesturalMode(NavigationBarFragment.this.mNavBarMode)) {
                    if (KeyguardUpdateMonitor.getInstance(NavigationBarFragment.sContext).isKeyguardVisible() || (QuickStepContract.isGesturalMode(NavigationBarFragment.this.mNavBarMode) && NavigationBarFragment.this.mImeShow && (NavigationBarFragment.this.mStatusBar.getNavigationBarHiddenMode() == 1 || !EdgeBackGestureHandler.sSideGestureEnabled))) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("onNavBarButtonAlphaChanged replace alpha from ");
                        sb.append(f);
                        sb.append(" to 0.f.");
                        Log.d("NavigationBar", sb.toString());
                        f = 0.0f;
                    }
                    buttonDispatcher = NavigationBarFragment.this.mNavigationBarView.getHomeHandle();
                } else {
                    buttonDispatcher = null;
                }
                if (buttonDispatcher != null) {
                    buttonDispatcher.setVisibility(f > 0.0f ? 0 : 4);
                    buttonDispatcher.setAlpha(f, z);
                }
            }
        };
        this.mRotationButtonListener = new ContextButtonListener() {
            public final void onVisibilityChanged(ContextualButton contextualButton, boolean z) {
                NavigationBarFragment.this.lambda$new$0$NavigationBarFragment(contextualButton, z);
            }
        };
        this.mAutoDim = new Runnable() {
            public final void run() {
                NavigationBarFragment.this.lambda$new$1$NavigationBarFragment();
            }
        };
        this.mAssistContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            public void onChange(boolean z, Uri uri) {
                boolean z2 = NavigationBarFragment.this.mAssistManager.getAssistInfoForUser(-2) != null;
                if (NavigationBarFragment.this.mAssistantAvailable != z2) {
                    NavigationBarFragment.this.sendAssistantAvailability(z2);
                    NavigationBarFragment.this.mAssistantAvailable = z2;
                }
            }
        };
        this.mNavActionListener = new OnClickListener() {
            public void onClick(View view) {
                String str = "systemui_navigation_bar_hided";
                if (NavigationBarFragment.this.mHideNavBar) {
                    System.putInt(NavigationBarFragment.this.mContentResolver, str, 0);
                } else {
                    System.putInt(NavigationBarFragment.this.mContentResolver, str, 1);
                    NavigationBarFragment.this.mStatusBar.showNavigationBarGuide();
                }
                NavigationBarFragment navigationBarFragment = NavigationBarFragment.this;
                navigationBarFragment.mNavigationBarView.updateNavButtonIcon(navigationBarFragment.mHideNavBar);
                NavigationBarFragment navigationBarFragment2 = NavigationBarFragment.this;
                navigationBarFragment2.mHideNavBar = true ^ navigationBarFragment2.mHideNavBar;
                NavigationBarFragment.this.onHideNavBar(false);
            }
        };
        this.mCheckNavigationBarState = new Runnable() {
            public void run() {
                if (NavigationBarFragment.this.mStatusBar != null) {
                    NavigationBarFragment.this.mStatusBar.checkNavigationBarState();
                }
            }
        };
        this.mAccessibilityListener = new AccessibilityServicesStateChangeListener() {
            public final void onAccessibilityServicesStateChanged(AccessibilityManager accessibilityManager) {
                NavigationBarFragment.this.updateAccessibilityServicesState(accessibilityManager);
            }
        };
        this.mRotationWatcher = new Consumer() {
            public final void accept(Object obj) {
                NavigationBarFragment.this.lambda$new$4$NavigationBarFragment((Integer) obj);
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String str = "android.intent.action.SCREEN_ON";
                if ("android.intent.action.SCREEN_OFF".equals(action) || str.equals(action)) {
                    NavigationBarFragment.this.notifyNavigationBarScreenOn();
                    if (!str.equals(action)) {
                        NavigationBarFragment.this.mNavigationBarView.getTintController().stop();
                    } else if (NavBarTintController.isEnabled(NavigationBarFragment.this.getContext(), NavigationBarFragment.this.mNavBarMode)) {
                        NavigationBarFragment.this.mNavigationBarView.getTintController().start();
                    }
                }
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    NavigationBarFragment navigationBarFragment = NavigationBarFragment.this;
                    navigationBarFragment.updateAccessibilityServicesState(navigationBarFragment.mAccessibilityManager);
                }
            }
        };
        this.mAccessibilityManagerWrapper = accessibilityManagerWrapper;
        this.mDeviceProvisionedController = deviceProvisionedController;
        this.mStatusBarStateController = statusBarStateController;
        this.mMetricsLogger = metricsLogger;
        this.mAssistManager = assistManager;
        if (this.mAssistManager.getAssistInfoForUser(-2) != null) {
            z = true;
        }
        this.mAssistantAvailable = z;
        this.mOverviewProxyService = overviewProxyService;
        this.mNavBarMode = navigationModeController.addListener(this);
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mCommandQueue = (CommandQueue) SysUiServiceProvider.getComponent(getContext(), CommandQueue.class);
        this.mCommandQueue.observe(getLifecycle(), this);
        this.mStatusBar = (StatusBar) SysUiServiceProvider.getComponent(getContext(), StatusBar.class);
        this.mRecents = (Recents) SysUiServiceProvider.getComponent(getContext(), Recents.class);
        this.mDivider = (Divider) SysUiServiceProvider.getComponent(getContext(), Divider.class);
        this.mWindowManager = (WindowManager) getContext().getSystemService(WindowManager.class);
        this.mAccessibilityManager = (AccessibilityManager) getContext().getSystemService(AccessibilityManager.class);
        this.mContentResolver = getContext().getContentResolver();
        this.mMagnificationObserver = new MagnificationContentObserver(getContext().getMainThreadHandler());
        this.mContentResolver.registerContentObserver(Secure.getUriFor("accessibility_display_magnification_navbar_enabled"), false, this.mMagnificationObserver, -1);
        this.mContentResolver.registerContentObserver(Secure.getUriFor("assistant"), false, this.mAssistContentObserver, -1);
        this.mNavBarSettingObserver = new NavBarSettingObserver(getContext().getMainThreadHandler());
        this.mNavBarSettingObserver.onChange(true);
        this.mContentResolver.registerContentObserver(System.getUriFor("op_gesture_button_enabled"), false, this.mNavBarSettingObserver, -1);
        String str = "NavigationBar";
        if (bundle != null) {
            this.mDisabledFlags1 = bundle.getInt("disabled_state", 0);
            this.mDisabledFlags2 = bundle.getInt("disabled2_state", 0);
            this.mSystemUiVisibility = bundle.getInt("system_ui_visibility", 0);
            if (Build.DEBUG_ONEPLUS && this.mStatusBar != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("NavBar vis get from savedInstance ");
                sb.append(this.mSystemUiVisibility);
                sb.append(", Status bar vis ");
                sb.append(this.mStatusBar.getSystemUiVisibility());
                Log.d(str, sb.toString());
            }
        } else {
            StatusBar statusBar = this.mStatusBar;
            if (!(statusBar == null || statusBar.getSystemUiVisibility() == 0 || (this.mSystemUiVisibility != 0 && bundle != null))) {
                this.mSystemUiVisibility = this.mStatusBar.getSystemUiVisibility();
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Get mSystemUiVisibility from status bar ");
                sb2.append(this.mSystemUiVisibility);
                Log.d(str, sb2.toString());
            }
        }
        this.mAccessibilityManagerWrapper.addCallback(this.mAccessibilityListener);
        this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, false);
        this.mIsInBrickMode = ((OpSceneModeObserver) Dependency.get(OpSceneModeObserver.class)).isInBrickMode();
        onHideNavBar(false);
        this.mOrientation = getContext().getResources().getConfiguration().orientation;
    }

    public void onDestroy() {
        super.onDestroy();
        this.mAccessibilityManagerWrapper.removeCallback(this.mAccessibilityListener);
        this.mContentResolver.unregisterContentObserver(this.mMagnificationObserver);
        this.mContentResolver.unregisterContentObserver(this.mAssistContentObserver);
        this.mContentResolver.unregisterContentObserver(this.mNavBarSettingObserver);
        this.mOverviewProxyService.removeCallback(this.mOverviewProxyListener);
        if (getBarTransitions() != null) {
            getBarTransitions().destroy();
        }
    }

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        if (this.mNavigationBarView == null) {
            return layoutInflater.inflate(R$layout.navigation_bar, viewGroup, false);
        }
        Log.d("NavigationBar", " Return the navigation bar view if it's already created");
        return this.mNavigationBarView;
    }

    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.mNavigationBarView = (NavigationBarView) view;
        Display display = view.getDisplay();
        if (display != null) {
            this.mDisplayId = display.getDisplayId();
            this.mIsOnDefaultDisplay = this.mDisplayId == 0;
        }
        this.mNavigationBarView.setComponents(this.mStatusBar.getPanel(), this.mAssistManager);
        this.mNavigationBarView.setDisabledFlags(this.mDisabledFlags1);
        this.mNavigationBarView.setOnVerticalChangedListener(new OnVerticalChangedListener() {
            public final void onVerticalChanged(boolean z) {
                NavigationBarFragment.this.onVerticalChanged(z);
            }
        });
        this.mNavigationBarView.setOnTouchListener(new OnTouchListener() {
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return NavigationBarFragment.this.onNavigationTouch(view, motionEvent);
            }
        });
        if (bundle != null) {
            this.mNavigationBarView.getLightTransitionsController().restoreState(bundle);
        }
        this.mNavigationBarView.setNavigationIconHints(this.mNavigationIconHints);
        checkHideNavBarState();
        this.mNavigationBarView.setWindowVisible(isNavBarWindowVisible());
        prepareNavigationBarView();
        checkNavBarModes();
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, null);
        notifyNavigationBarScreenOn();
        this.mOverviewProxyService.addCallback(this.mOverviewProxyListener);
        updateSystemUiStateFlags(-1);
        if (this.mIsOnDefaultDisplay) {
            this.mNavigationBarView.getRotateSuggestionButton().setListener(this.mRotationButtonListener);
            RotationButtonController rotationButtonController = this.mNavigationBarView.getRotationButtonController();
            rotationButtonController.addRotationCallback(this.mRotationWatcher);
            if (display != null && rotationButtonController.isRotationLocked()) {
                rotationButtonController.setRotationLockedAtAngle(display.getRotation());
            }
        } else {
            this.mDisabledFlags2 |= 16;
        }
        setDisabled2Flags(this.mDisabledFlags2);
        this.mScreenDecorations = (ScreenDecorations) SysUiServiceProvider.getComponent(getContext(), ScreenDecorations.class);
        getBarTransitions().addDarkIntensityListener(this.mScreenDecorations);
    }

    public void onDestroyView() {
        super.onDestroyView();
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView != null) {
            navigationBarView.getBarTransitions().removeDarkIntensityListener(this.mScreenDecorations);
            this.mNavigationBarView.getBarTransitions().destroy();
            this.mNavigationBarView.getLightTransitionsController().destroy(getContext());
        }
        getContext().unregisterReceiver(this.mBroadcastReceiver);
    }

    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("disabled_state", this.mDisabledFlags1);
        bundle.putInt("disabled2_state", this.mDisabledFlags2);
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("onSaveInstanceState save vis ");
            sb.append(this.mSystemUiVisibility);
            Log.d("NavigationBar", sb.toString());
        }
        bundle.putInt("system_ui_visibility", this.mSystemUiVisibility);
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView != null) {
            navigationBarView.getLightTransitionsController().saveState(bundle);
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (getContext() != null) {
            Locale locale = getContext().getResources().getConfiguration().locale;
            int layoutDirectionFromLocale = TextUtils.getLayoutDirectionFromLocale(locale);
            if (!locale.equals(this.mLocale) || layoutDirectionFromLocale != this.mLayoutDirection) {
                this.mLocale = locale;
                this.mLayoutDirection = layoutDirectionFromLocale;
                refreshLayout(layoutDirectionFromLocale);
            }
            repositionNavigationBar();
            this.mOrientation = configuration.orientation;
            updateNavigationBarTouchableState();
        }
    }

    public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mNavigationBarView != null) {
            printWriter.print("  mNavigationBarWindowState=");
            printWriter.println(StatusBarManager.windowStateToString(this.mNavigationBarWindowState));
            printWriter.print("  mNavigationBarMode=");
            printWriter.println(BarTransitions.modeToString(this.mNavigationBarMode));
            StatusBar.dumpBarTransitions(printWriter, "mNavigationBarView", this.mNavigationBarView.getBarTransitions());
        }
        printWriter.print("  mNavigationBarView=");
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView == null) {
            printWriter.println("null");
        } else {
            navigationBarView.dump(fileDescriptor, printWriter, strArr);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:15:0x003b, code lost:
        if (r6 != 3) goto L_0x0045;
     */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0047  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0049  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x004f A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0050  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setImeWindowStatus(int r3, android.os.IBinder r4, int r5, int r6, boolean r7) {
        /*
            r2 = this;
            int r4 = r2.mDisplayId
            if (r3 == r4) goto L_0x0005
            return
        L_0x0005:
            r3 = 2
            r4 = r5 & 2
            r5 = 1
            if (r4 == 0) goto L_0x000d
            r4 = r5
            goto L_0x000e
        L_0x000d:
            r4 = 0
        L_0x000e:
            r2.mImeShow = r4
            boolean r0 = android.os.Build.DEBUG_ONEPLUS
            if (r0 == 0) goto L_0x0032
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "imeShown "
            r0.append(r1)
            r0.append(r4)
            java.lang.String r1 = ", showImeSwitcher "
            r0.append(r1)
            r0.append(r7)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "NavigationBar"
            android.util.Log.d(r1, r0)
        L_0x0032:
            int r0 = r2.mNavigationIconHints
            if (r6 == 0) goto L_0x0041
            if (r6 == r5) goto L_0x0041
            if (r6 == r3) goto L_0x0041
            r4 = 3
            if (r6 == r4) goto L_0x003e
            goto L_0x0045
        L_0x003e:
            r0 = r0 & -2
            goto L_0x0045
        L_0x0041:
            if (r4 == 0) goto L_0x003e
            r0 = r0 | 1
        L_0x0045:
            if (r7 == 0) goto L_0x0049
            r3 = r3 | r0
            goto L_0x004b
        L_0x0049:
            r3 = r0 & -3
        L_0x004b:
            int r4 = r2.mNavigationIconHints
            if (r3 != r4) goto L_0x0050
            return
        L_0x0050:
            r2.mNavigationIconHints = r3
            com.android.systemui.statusbar.phone.NavigationBarView r4 = r2.mNavigationBarView
            if (r4 == 0) goto L_0x0059
            r4.setNavigationIconHints(r3)
        L_0x0059:
            r2.checkBarModes()
            r2.updateNavigationBarTouchableState()
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NavigationBarFragment.setImeWindowStatus(int, android.os.IBinder, int, int, boolean):void");
    }

    public void setWindowState(int i, int i2, int i3) {
        if (i == this.mDisplayId && this.mNavigationBarView != null && i2 == 2 && this.mNavigationBarWindowState != i3) {
            this.mNavigationBarWindowState = i3;
            StringBuilder sb = new StringBuilder();
            sb.append("Navigation bar ");
            sb.append(StatusBarManager.windowStateToString(i3));
            Log.d("NavigationBar", sb.toString());
            updateSystemUiStateFlags(-1);
            this.mNavigationBarView.setWindowVisible(isNavBarWindowVisible());
            this.mBackupNavBarMode = this.mNavBarMode;
        }
    }

    public void onRotationProposal(int i, boolean z) {
        int rotation = this.mNavigationBarView.getDisplay().getRotation();
        boolean hasDisable2RotateSuggestionFlag = RotationButtonController.hasDisable2RotateSuggestionFlag(this.mDisabledFlags2);
        RotationButtonController rotationButtonController = this.mNavigationBarView.getRotationButtonController();
        rotationButtonController.getRotationButton();
        if (!hasDisable2RotateSuggestionFlag) {
            rotationButtonController.onRotationProposal(i, rotation, z);
        }
    }

    public void restoreSystemUiVisibilityState() {
        int computeBarMode = computeBarMode(0, this.mSystemUiVisibility);
        if (computeBarMode != -1) {
            this.mNavigationBarMode = computeBarMode;
        }
        checkNavBarModes();
        this.mAutoHideController.touchAutoHide();
        this.mLightBarController.onNavigationVisibilityChanged(this.mSystemUiVisibility, 0, true, this.mNavigationBarMode, false);
    }

    public void setSystemUiVisibility(int i, int i2, int i3, int i4, int i5, Rect rect, Rect rect2, boolean z) {
        boolean z2;
        int i6;
        if (i == this.mDisplayId) {
            int i7 = this.mSystemUiVisibility;
            int i8 = ((~i5) & i7) | (i2 & i5);
            if ((i8 ^ i7) != 0) {
                this.mSystemUiVisibility = i8;
                if (getView() == null) {
                    i6 = -1;
                } else {
                    i6 = computeBarMode(i7, i8);
                }
                boolean z3 = i6 != -1;
                if (z3) {
                    int i9 = this.mNavigationBarMode;
                    if (i9 != i6) {
                        if (i9 == 4 || i9 == 6) {
                            this.mNavigationBarView.hideRecentsOnboarding();
                        }
                        this.mNavigationBarMode = i6;
                        checkNavBarModes();
                    }
                    this.mAutoHideController.touchAutoHide();
                }
                if (OpNavBarUtils.isSupportHideNavBar()) {
                    NavigationBarView navigationBarView = this.mNavigationBarView;
                    if (navigationBarView != null) {
                        if ((i2 & 4098) != 0) {
                            navigationBarView.onImmersiveSticky(true);
                        } else {
                            navigationBarView.onImmersiveSticky(false);
                        }
                    }
                }
                z2 = z3;
            } else {
                z2 = false;
            }
            this.mLightBarController.onNavigationVisibilityChanged(i2, i5, z2, this.mNavigationBarMode, z);
        }
    }

    private int computeBarMode(int i, int i2) {
        int barMode = barMode(i);
        int barMode2 = barMode(i2);
        if (barMode == barMode2) {
            return -1;
        }
        return barMode2;
    }

    public void disable(int i, int i2, int i3, boolean z) {
        if (i == this.mDisplayId) {
            int i4 = 56623104 & i2;
            if (i4 != this.mDisabledFlags1) {
                this.mDisabledFlags1 = i4;
                NavigationBarView navigationBarView = this.mNavigationBarView;
                if (navigationBarView != null) {
                    navigationBarView.setDisabledFlags(i2);
                }
                updateScreenPinningGestures();
            }
            if (this.mIsOnDefaultDisplay) {
                int i5 = i3 & 16;
                if (i5 != this.mDisabledFlags2) {
                    this.mDisabledFlags2 = i5;
                    setDisabled2Flags(i5);
                }
            }
        }
    }

    private void setDisabled2Flags(int i) {
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView != null) {
            navigationBarView.getRotationButtonController().onDisable2FlagChanged(i);
        }
    }

    public void refreshLayout(int i) {
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView != null) {
            navigationBarView.setLayoutDirection(i);
        }
    }

    private boolean shouldDisableNavbarGestures() {
        return !this.mDeviceProvisionedController.isDeviceProvisioned() || (this.mDisabledFlags1 & 33554432) != 0;
    }

    private void repositionNavigationBar() {
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView != null && navigationBarView.isAttachedToWindow()) {
            prepareNavigationBarView();
            this.mWindowManager.updateViewLayout((View) this.mNavigationBarView.getParent(), ((View) this.mNavigationBarView.getParent()).getLayoutParams());
        }
    }

    /* access modifiers changed from: private */
    public void updateScreenPinningGestures() {
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView != null) {
            boolean isRecentsButtonVisible = navigationBarView.isRecentsButtonVisible();
            ButtonDispatcher backButton = this.mNavigationBarView.getBackButton();
            if (isRecentsButtonVisible) {
                backButton.setOnLongClickListener(new OnLongClickListener() {
                    public final boolean onLongClick(View view) {
                        return NavigationBarFragment.this.onLongPressBackRecents(view);
                    }
                });
            } else {
                backButton.setOnLongClickListener(new OnLongClickListener() {
                    public final boolean onLongClick(View view) {
                        return NavigationBarFragment.this.onLongPressBackHome(view);
                    }
                });
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifyNavigationBarScreenOn() {
        this.mNavigationBarView.updateNavButtonIcons();
    }

    private void prepareNavigationBarView() {
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView != null) {
            navigationBarView.reorient();
            if (OpNavBarUtils.isSupportHideNavBar()) {
                this.mNavigationBarView.getNavButton().setOnClickListener(this.mNavActionListener);
            }
            ButtonDispatcher recentsButton = this.mNavigationBarView.getRecentsButton();
            recentsButton.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    NavigationBarFragment.this.onRecentsClick(view);
                }
            });
            recentsButton.setOnTouchListener(new OnTouchListener() {
                public final boolean onTouch(View view, MotionEvent motionEvent) {
                    return NavigationBarFragment.this.onRecentsTouch(view, motionEvent);
                }
            });
            recentsButton.setLongClickable(true);
            recentsButton.setOnLongClickListener(new OnLongClickListener() {
                public final boolean onLongClick(View view) {
                    return NavigationBarFragment.this.onLongPressBackRecents(view);
                }
            });
            this.mNavigationBarView.getBackButton().setLongClickable(true);
            ButtonDispatcher homeButton = this.mNavigationBarView.getHomeButton();
            homeButton.setOnTouchListener(new OnTouchListener() {
                public final boolean onTouch(View view, MotionEvent motionEvent) {
                    return NavigationBarFragment.this.onHomeTouch(view, motionEvent);
                }
            });
            homeButton.setOnLongClickListener(new OnLongClickListener() {
                public final boolean onLongClick(View view) {
                    return NavigationBarFragment.this.onHomeLongClick(view);
                }
            });
            ButtonDispatcher accessibilityButton = this.mNavigationBarView.getAccessibilityButton();
            accessibilityButton.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    NavigationBarFragment.this.onAccessibilityClick(view);
                }
            });
            accessibilityButton.setOnLongClickListener(new OnLongClickListener() {
                public final boolean onLongClick(View view) {
                    return NavigationBarFragment.this.onAccessibilityLongClick(view);
                }
            });
            updateAccessibilityServicesState(this.mAccessibilityManager);
            updateScreenPinningGestures();
        }
    }

    /* access modifiers changed from: private */
    public boolean onHomeTouch(View view, MotionEvent motionEvent) {
        if (this.mHomeBlockedThisTouch && motionEvent.getActionMasked() != 0) {
            return true;
        }
        int action = motionEvent.getAction();
        if (action == 0) {
            this.mHomeBlockedThisTouch = false;
            TelecomManager telecomManager = (TelecomManager) getContext().getSystemService(TelecomManager.class);
            if (telecomManager != null && telecomManager.isRinging() && this.mStatusBar.isKeyguardShowing()) {
                Log.i("NavigationBar", "Ignoring HOME; there's a ringing incoming call. No heads up");
                this.mHomeBlockedThisTouch = true;
                return true;
            }
        } else if (action == 1 || action == 3) {
            this.mStatusBar.awakenDreams();
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void onVerticalChanged(boolean z) {
        this.mStatusBar.setQsScrimEnabled(!z);
    }

    /* access modifiers changed from: private */
    public boolean onNavigationTouch(View view, MotionEvent motionEvent) {
        this.mAutoHideController.checkUserAutoHide(motionEvent);
        return false;
    }

    /* access modifiers changed from: 0000 */
    public boolean onHomeLongClick(View view) {
        if (!this.mNavigationBarView.isRecentsButtonVisible() && ActivityManagerWrapper.getInstance().isScreenPinningActive()) {
            return onLongPressBackHome(view);
        }
        if (shouldDisableNavbarGestures()) {
            return false;
        }
        this.mMetricsLogger.action(239);
        Bundle bundle = new Bundle();
        bundle.putInt("invocation_type", 5);
        this.mAssistManager.startAssist(bundle);
        this.mStatusBar.awakenDreams();
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView != null) {
            navigationBarView.abortCurrentGesture();
        }
        return true;
    }

    /* access modifiers changed from: private */
    public boolean onRecentsTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction() & 255;
        if (action == 0) {
            this.mCommandQueue.preloadRecentApps();
        } else if (action == 3) {
            this.mCommandQueue.cancelPreloadRecentApps();
        } else if (action == 1 && !view.isPressed()) {
            this.mCommandQueue.cancelPreloadRecentApps();
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void onRecentsClick(View view) {
        if (LatencyTracker.isEnabled(getContext())) {
            LatencyTracker.getInstance(getContext()).onActionStart(1);
        }
        this.mStatusBar.awakenDreams();
        this.mCommandQueue.toggleRecentApps();
    }

    /* access modifiers changed from: private */
    public boolean onLongPressBackHome(View view) {
        return onLongPressNavigationButtons(view, R$id.back, R$id.home);
    }

    /* access modifiers changed from: private */
    public boolean onLongPressBackRecents(View view) {
        return onLongPressNavigationButtons(view, R$id.back, R$id.recent_apps);
    }

    private boolean onLongPressNavigationButtons(View view, int i, int i2) {
        boolean z;
        boolean z2;
        ButtonDispatcher buttonDispatcher;
        try {
            IActivityTaskManager service = ActivityTaskManager.getService();
            boolean isTouchExplorationEnabled = this.mAccessibilityManager.isTouchExplorationEnabled();
            boolean isInLockTaskMode = service.isInLockTaskMode();
            if (isInLockTaskMode && !isTouchExplorationEnabled) {
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis - this.mLastLockToAppLongPress < 200) {
                    service.stopSystemLockTaskMode();
                    this.mNavigationBarView.updateNavButtonIcons();
                    return true;
                }
                if (view.getId() == i) {
                    if (i2 == R$id.recent_apps) {
                        buttonDispatcher = this.mNavigationBarView.getRecentsButton();
                    } else {
                        buttonDispatcher = this.mNavigationBarView.getHomeButton();
                    }
                    if (!buttonDispatcher.getCurrentView().isPressed()) {
                        z = true;
                        this.mLastLockToAppLongPress = currentTimeMillis;
                    }
                }
                z = false;
                this.mLastLockToAppLongPress = currentTimeMillis;
            } else if (view.getId() == i) {
                z = true;
            } else if (isTouchExplorationEnabled && isInLockTaskMode) {
                service.stopSystemLockTaskMode();
                this.mNavigationBarView.updateNavButtonIcons();
                return true;
            } else if (view.getId() == i2) {
                if (i2 == R$id.recent_apps) {
                    z2 = onLongPressRecents();
                } else {
                    z2 = onHomeLongClick(this.mNavigationBarView.getHomeButton().getCurrentView());
                }
                return z2;
            } else {
                z = false;
            }
            if (z) {
                KeyButtonView keyButtonView = (KeyButtonView) view;
                keyButtonView.sendEvent(0, 128);
                keyButtonView.sendAccessibilityEvent(2);
                return true;
            }
        } catch (RemoteException e) {
            Log.d("NavigationBar", "Unable to reach activity manager", e);
        }
        return false;
    }

    private boolean onLongPressRecents() {
        if (this.mRecents == null || !ActivityTaskManager.supportsMultiWindow(getContext()) || !this.mDivider.getView().getSnapAlgorithm().isSplitScreenFeasible() || ActivityManager.isLowRamDeviceStatic() || this.mOverviewProxyService.getProxy() != null) {
            return false;
        }
        return this.mStatusBar.toggleSplitScreenMode(271, 286);
    }

    /* access modifiers changed from: private */
    public void onAccessibilityClick(View view) {
        Display display = view.getDisplay();
        this.mAccessibilityManager.notifyAccessibilityButtonClicked(display != null ? display.getDisplayId() : 0);
    }

    /* access modifiers changed from: private */
    public boolean onAccessibilityLongClick(View view) {
        Intent intent = new Intent("com.android.internal.intent.action.CHOOSE_ACCESSIBILITY_BUTTON");
        intent.addFlags(268468224);
        view.getContext().startActivityAsUser(intent, UserHandle.CURRENT);
        return true;
    }

    /* access modifiers changed from: private */
    public void updateAccessibilityServicesState(AccessibilityManager accessibilityManager) {
        boolean z = true;
        int a11yButtonState = getA11yButtonState(new boolean[1]);
        boolean z2 = (a11yButtonState & 16) != 0;
        if ((a11yButtonState & 32) == 0) {
            z = false;
        }
        this.mNavigationBarView.setAccessibilityButtonState(z2, z);
        updateSystemUiStateFlags(a11yButtonState);
    }

    public void updateSystemUiStateFlags(int i) {
        if (i < 0) {
            i = getA11yButtonState(null);
        }
        boolean z = false;
        boolean z2 = (i & 16) != 0;
        if ((i & 32) != 0) {
            z = true;
        }
        this.mOverviewProxyService.setSystemUiStateFlag(16, z2, this.mDisplayId);
        this.mOverviewProxyService.setSystemUiStateFlag(32, z, this.mDisplayId);
        this.mOverviewProxyService.setSystemUiStateFlag(2, !isNavBarWindowVisible(), this.mDisplayId);
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x0022  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x003c  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0041  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0045  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x003a A[EDGE_INSN: B:27:0x003a->B:18:0x003a ?: BREAK  
    EDGE_INSN: B:27:0x003a->B:18:0x003a ?: BREAK  , SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getA11yButtonState(boolean[] r9) {
        /*
            r8 = this;
            r0 = 0
            r1 = 1
            android.content.ContentResolver r2 = r8.mContentResolver     // Catch:{ SettingNotFoundException -> 0x000f }
            java.lang.String r3 = "accessibility_display_magnification_navbar_enabled"
            r4 = -2
            int r2 = android.provider.Settings.Secure.getIntForUser(r2, r3, r4)     // Catch:{ SettingNotFoundException -> 0x000f }
            if (r2 != r1) goto L_0x000f
            r2 = r1
            goto L_0x0010
        L_0x000f:
            r2 = r0
        L_0x0010:
            android.view.accessibility.AccessibilityManager r8 = r8.mAccessibilityManager
            r3 = -1
            java.util.List r8 = r8.getEnabledAccessibilityServiceList(r3)
            int r3 = r8.size()
            int r3 = r3 - r1
            r4 = r2
            r2 = r0
        L_0x001e:
            r5 = 16
            if (r3 < 0) goto L_0x003a
            java.lang.Object r6 = r8.get(r3)
            android.accessibilityservice.AccessibilityServiceInfo r6 = (android.accessibilityservice.AccessibilityServiceInfo) r6
            int r7 = r6.flags
            r7 = r7 & 256(0x100, float:3.59E-43)
            if (r7 == 0) goto L_0x0030
            int r4 = r4 + 1
        L_0x0030:
            int r6 = r6.feedbackType
            if (r6 == 0) goto L_0x0037
            if (r6 == r5) goto L_0x0037
            r2 = r1
        L_0x0037:
            int r3 = r3 + -1
            goto L_0x001e
        L_0x003a:
            if (r9 == 0) goto L_0x003e
            r9[r0] = r2
        L_0x003e:
            if (r4 < r1) goto L_0x0041
            goto L_0x0042
        L_0x0041:
            r5 = r0
        L_0x0042:
            r8 = 2
            if (r4 < r8) goto L_0x0047
            r0 = 32
        L_0x0047:
            r8 = r5 | r0
            return r8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NavigationBarFragment.getA11yButtonState(boolean[]):int");
    }

    /* access modifiers changed from: private */
    public void sendAssistantAvailability(boolean z) {
        if (this.mOverviewProxyService.getProxy() != null) {
            try {
                this.mOverviewProxyService.getProxy().onAssistantAvailable(z && QuickStepContract.isGesturalMode(this.mNavBarMode));
            } catch (RemoteException unused) {
                Log.w("NavigationBar", "Unable to send assistant availability data to launcher");
            }
        }
    }

    public void touchAutoDim() {
        getBarTransitions().setAutoDim(false);
        this.mHandler.removeCallbacks(this.mAutoDim);
        int state = this.mStatusBarStateController.getState();
        if (state != 1 && state != 2) {
            this.mHandler.postDelayed(this.mAutoDim, 2250);
        }
    }

    public void setLightBarController(LightBarController lightBarController) {
        this.mLightBarController = lightBarController;
        this.mLightBarController.setNavigationBar(this.mNavigationBarView.getLightTransitionsController());
    }

    public void setAutoHideController(AutoHideController autoHideController) {
        this.mAutoHideController = autoHideController;
        this.mAutoHideController.setNavigationBar(this);
    }

    public boolean isSemiTransparent() {
        return this.mNavigationBarMode == 1;
    }

    private void checkBarModes() {
        if (this.mIsOnDefaultDisplay) {
            this.mStatusBar.checkBarModes();
        } else {
            checkNavBarModes();
        }
    }

    public boolean isNavBarWindowVisible() {
        return this.mNavigationBarWindowState == 0;
    }

    public void checkNavBarModes() {
        this.mNavigationBarView.getBarTransitions().transitionTo(this.mNavigationBarMode, this.mStatusBar.isDeviceInteractive() && this.mNavigationBarWindowState != 2);
        if (OpNavBarUtils.isSupportCustomNavBar() && !QuickStepContract.isGesturalMode(this.mNavBarMode)) {
            NavigationBarView navigationBarView = this.mNavigationBarView;
            if (navigationBarView != null) {
                int i = this.mNavigationBarMode;
                if (i == 1 || i == 2) {
                    this.mNavigationBarView.notifyNavBarColorChange(0);
                } else {
                    navigationBarView.notifyNavBarColorChange(this.mNavigationBarColor);
                }
            }
        }
    }

    public void onNavigationModeChanged(int i) {
        String str = "NavigationBar";
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("onNavigationModeChanged mode from ");
            sb.append(this.mNavBarMode);
            sb.append(" to ");
            sb.append(i);
            Log.d(str, sb.toString());
        }
        this.mNavBarMode = i;
        updateScreenPinningGestures();
        if (ActivityManagerWrapper.getInstance().getCurrentUserId() != 0) {
            this.mHandler.post(new Runnable() {
                public final void run() {
                    NavigationBarFragment.this.lambda$onNavigationModeChanged$2$NavigationBarFragment();
                }
            });
        }
        boolean z = (this.mStatusBar.getNavigationBarHiddenMode() == 1 || !EdgeBackGestureHandler.sSideGestureEnabled) && QuickStepContract.isGesturalMode(this.mNavBarMode);
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("onNavigationModeChanged navBarHidden ");
            sb2.append(z);
            Log.d(str, sb2.toString());
        }
        System.putInt(sContext.getContentResolver(), "buttons_show_on_screen_navkeys", z ^ true ? 1 : 0);
        this.mHandler.removeCallbacks(this.mCheckNavigationBarState);
        this.mHandler.postDelayed(this.mCheckNavigationBarState, 1000);
        updateNavigationBarTouchableState();
    }

    public /* synthetic */ void lambda$onNavigationModeChanged$2$NavigationBarFragment() {
        FragmentHostManager.get(this.mNavigationBarView).reloadFragments();
    }

    public void disableAnimationsDuringHide(long j) {
        this.mNavigationBarView.setLayoutTransitionsEnabled(false);
        this.mNavigationBarView.postDelayed(new Runnable() {
            public final void run() {
                NavigationBarFragment.this.lambda$disableAnimationsDuringHide$3$NavigationBarFragment();
            }
        }, j + 448);
    }

    public /* synthetic */ void lambda$disableAnimationsDuringHide$3$NavigationBarFragment() {
        this.mNavigationBarView.setLayoutTransitionsEnabled(true);
    }

    public void transitionTo(int i, boolean z) {
        getBarTransitions().transitionTo(i, z);
    }

    public NavigationBarTransitions getBarTransitions() {
        return this.mNavigationBarView.getBarTransitions();
    }

    public void finishBarAnimations() {
        this.mNavigationBarView.getBarTransitions().finishAnimations();
    }

    public /* synthetic */ void lambda$new$4$NavigationBarFragment(Integer num) {
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView != null && navigationBarView.needsReorient(num.intValue())) {
            repositionNavigationBar();
        }
    }

    public static View create(Context context, final FragmentListener fragmentListener) {
        sContext = context;
        LayoutParams layoutParams = new LayoutParams(-1, -1, 2019, 545521768, -3);
        layoutParams.token = new Binder();
        StringBuilder sb = new StringBuilder();
        sb.append("NavigationBar");
        sb.append(context.getDisplayId());
        layoutParams.setTitle(sb.toString());
        layoutParams.accessibilityTitle = context.getString(R$string.nav_bar);
        layoutParams.windowAnimations = 0;
        layoutParams.privateFlags |= 16777216;
        View inflate = LayoutInflater.from(context).inflate(R$layout.navigation_bar_window, null);
        if (inflate == null) {
            return null;
        }
        inflate.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View view) {
                FragmentHostManager fragmentHostManager = FragmentHostManager.get(view);
                String str = "NavigationBar";
                fragmentHostManager.getFragmentManager().beginTransaction().replace(R$id.navigation_bar_frame, NavigationBarFragment.this, str).commit();
                fragmentHostManager.addTagListener(str, fragmentListener);
            }

            public void onViewDetachedFromWindow(View view) {
                FragmentHostManager.removeAndDestroy(view);
            }
        });
        ((WindowManager) context.getSystemService(WindowManager.class)).addView(inflate, layoutParams);
        return inflate;
    }

    /* access modifiers changed from: 0000 */
    public int getNavigationIconHints() {
        return this.mNavigationIconHints;
    }

    public void notifyNavBarColorChanged(int i, String str) {
        if (OpNavBarUtils.isSupportCustomNavBar()) {
            this.mNavigationBarColor = i;
            OpUtils.updateTopPackage(sContext, str);
            if (this.mNavigationBarView != null && !QuickStepContract.isGesturalMode(this.mNavBarMode)) {
                this.mNavigationBarView.notifyNavBarColorChange(i);
            }
        }
    }

    /* access modifiers changed from: private */
    public void onHideNavBar(boolean z) {
        boolean z2 = this.mHideNavBar && !z;
        this.mStatusBar.onHideNavBar(z2);
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView != null) {
            navigationBarView.setHideNavBarOn(z2);
        }
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("onHideNavBar mHideNavBar=");
            sb.append(this.mHideNavBar);
            sb.append(" forceShow=");
            sb.append(z);
            Log.d("NavigationBar", sb.toString());
        }
    }

    public void onBrickModeChanged(boolean z) {
        this.mIsInBrickMode = z;
        NavigationBarView navigationBarView = this.mNavigationBarView;
        if (navigationBarView != null) {
            navigationBarView.onBrickModeChanged(this.mIsInBrickMode);
        }
        onHideNavBar(z);
    }

    private void checkHideNavBarState() {
        int i = this.mBackupNavBarMode;
        int i2 = this.mNavBarMode;
        if (i != i2 && QuickStepContract.isGesturalMode(i2) && !isNavBarWindowVisible()) {
            Log.d("NavigationBar", "checkHideNavBarState: It's gesture mode. Reset state to showing.");
            this.mNavigationBarWindowState = 0;
        }
    }

    private void updateNavigationBarTouchableState() {
        View navView = ((NavigationBarController) Dependency.get(NavigationBarController.class)).getNavView();
        String str = "NavigationBar";
        if (navView != null && navView.isAttachedToWindow() && this.mImeShow && this.mOrientation == 2 && QuickStepContract.isGesturalMode(this.mNavBarMode)) {
            Log.d(str, " Set navigation bar not touchable when ime show on landscape");
            LayoutParams layoutParams = (LayoutParams) navView.getLayoutParams();
            layoutParams.flags |= 16;
            this.mWindowManager.updateViewLayout(navView, layoutParams);
        } else if (navView != null && navView.isAttachedToWindow()) {
            Log.d(str, " Set navigation bar touchable when waking");
            LayoutParams layoutParams2 = (LayoutParams) navView.getLayoutParams();
            layoutParams2.flags &= -17;
            layoutParams2.flags |= 64;
            this.mWindowManager.updateViewLayout(navView, layoutParams2);
        }
    }
}

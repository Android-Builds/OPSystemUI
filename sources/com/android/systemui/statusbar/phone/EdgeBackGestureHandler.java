package com.android.systemui.statusbar.phone;

import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.OpFeatures;
import android.util.Slog;
import android.view.Display;
import android.view.IPinnedStackController;
import android.view.IPinnedStackListener;
import android.view.ISystemGestureExclusionListener;
import android.view.ISystemGestureExclusionListener.Stub;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.InputMonitor;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewRootImplInjector;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import com.android.systemui.Dependency;
import com.android.systemui.DockedStackExistsListener;
import com.android.systemui.R$dimen;
import com.android.systemui.R$string;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.shared.system.TaskStackChangeListener;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.statusbar.phone.RegionSamplingHelper.SamplingCallback;
import com.oneplus.phone.GesturePointContainer;
import com.oneplus.phone.OpSideGestureConfiguration;
import com.oneplus.phone.OpSideGestureNavView;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.OpSystemUIInjector;
import com.oneplus.systemui.statusbar.phone.OpGestureButtonViewController;
import com.oneplus.util.OpUtils;
import java.io.PrintWriter;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class EdgeBackGestureHandler implements DisplayListener {
    private static final String[] DEFAULT_HOME_CHANGE_ACTIONS = {"android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED", "android.intent.action.BOOT_COMPLETED", "android.intent.action.PACKAGE_ADDED", "android.intent.action.PACKAGE_CHANGED", "android.intent.action.PACKAGE_REMOVED"};
    private static final int GESTURE_KEY_DISTANCE_THRESHOLD = SystemProperties.getInt("persist.gesture_button.dis", 60);
    private static final int MAX_LONG_PRESS_TIMEOUT = SystemProperties.getInt("persist.gesture_button.long_click_timeout", 200);
    private static final int ONE_HANDED_EDGE_HORIZONTAL_SCALE = SystemProperties.getInt("persist.gesture_button.one.handed.hor.scale", Resources.getSystem().getInteger(84475918));
    private static final float ONE_HANDED_EDGE_VERTICAL_SCALE = (((float) SystemProperties.getInt("persist.gesture_button.one.handed.ver.scale", Resources.getSystem().getInteger(84475921))) * 0.001f);
    private static final float ONE_HANDED_GESTURE_EDGE_SHIFT_SCALE = (((float) SystemProperties.getInt("persist.gesture_button.one.handed.horizontal.shift.rate", Resources.getSystem().getInteger(84475919))) * 0.001f);
    private static final int ONE_HANDED_MODE_SLIDE_TIME_THRESHOLD = SystemProperties.getInt("persist.gesture_button.one.handed.slide.time.threshold", Resources.getSystem().getInteger(84475920));
    private static final float PORTRAIT_LEAVE_ONE_HANDED_SCALE = (((float) SystemProperties.getInt("persist.gesture_button.one.handed.leave.scale", 45)) * 0.01f);
    private static final float SIDE_GESTURE_EDGE_BACK_SCALE = (((float) SystemProperties.getInt("persist.gesture_button.side.back.scale", Resources.getSystem().getInteger(84475922))) * 0.001f);
    private static final float SIDE_GESTURE_EDGE_HORIZONTAL_SCALE = (((float) SystemProperties.getInt("persist.gesture_button.side.hor.scale", 300)) * 0.01f);
    private static final float SIDE_GESTURE_EDGE_MOVE_SCALE = (((float) SystemProperties.getInt("persist.gesture_button.side.move.scale", 45)) * 0.001f);
    private static final float SIDE_GESTURE_EDGE_SCALE = (((float) SystemProperties.getInt("persist.gesture_button.side.edge.scale", Resources.getSystem().getInteger(84475923))) * 0.001f);
    private static int mCameraNotchHeight = 80;
    public static boolean sSideGestureEnabled = false;
    private final ActivityManagerWrapper mActivityManagerWrapper;
    private boolean mAllowGesture = false;
    private boolean mAllowLeaveOneHandedGesture;
    private boolean mAllowOneHandedGesture;
    private StatusBar mBar;
    /* access modifiers changed from: private */
    public final Context mContext;
    private final BroadcastReceiver mDefaultHomeBroadcastReceiver;
    private final IntentFilter mDefaultHomeIntentFilter;
    private Display mDisplay;
    /* access modifiers changed from: private */
    public final int mDisplayId;
    private final Point mDisplaySize = new Point();
    private final Consumer<Boolean> mDockedListener;
    private boolean mDockedStackExists;
    private final PointF mDownPoint = new PointF();
    /* access modifiers changed from: private */
    public OpSideGestureNavView mEdgePanel;
    private LayoutParams mEdgePanelLp;
    private int mEdgeSwipeStartThreshold = ((int) (SIDE_GESTURE_EDGE_SCALE * 1080.0f));
    private int mEdgeWidth;
    /* access modifiers changed from: private */
    public final Region mExcludeRegion = new Region();
    private final int mFingerOffset;
    private ISystemGestureExclusionListener mGestureExclusionListener = new Stub() {
        public void onSystemGestureExclusionChanged(int i, Region region) {
            if (i == EdgeBackGestureHandler.this.mDisplayId) {
                EdgeBackGestureHandler.this.mMainExecutor.execute(new Runnable(region) {
                    private final /* synthetic */ Region f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        C14022.this.mo17281x7f99a267(this.f$1);
                    }
                });
            }
        }

        /* renamed from: lambda$onSystemGestureExclusionChanged$0$EdgeBackGestureHandler$2 */
        public /* synthetic */ void mo17281x7f99a267(Region region) {
            EdgeBackGestureHandler.this.mExcludeRegion.set(region);
        }
    };
    private final IPinnedStackListener.Stub mImeChangedListener = new IPinnedStackListener.Stub() {
        public void onActionsChanged(ParceledListSlice parceledListSlice) {
        }

        public void onListenerRegistered(IPinnedStackController iPinnedStackController) {
        }

        public void onMinimizedStateChanged(boolean z) {
        }

        public void onMovementBoundsChanged(Rect rect, Rect rect2, Rect rect3, boolean z, boolean z2, int i) {
        }

        public void onShelfVisibilityChanged(boolean z, int i) {
        }

        public void onImeVisibilityChanged(boolean z, int i) {
            EdgeBackGestureHandler edgeBackGestureHandler = EdgeBackGestureHandler.this;
            if (!z) {
                i = 0;
            }
            edgeBackGestureHandler.mImeHeight = i;
        }
    };
    /* access modifiers changed from: private */
    public int mImeHeight = 0;
    private InputEventReceiver mInputEventReceiver;
    private InputMonitor mInputMonitor;
    private boolean mIsAttached;
    private boolean mIsEnabled;
    private boolean mIsGesturalModeEnabled;
    protected boolean mIsHidden;
    private boolean mIsIgnoreCameraNotch;
    private boolean mIsOnLeftEdge;
    /* access modifiers changed from: private */
    public boolean mIsOneHandedPerformed;
    /* access modifiers changed from: private */
    public int mIsOneHandedSettingEnable;
    private int mLeftInset;
    private final int mLongPressTimeout;
    /* access modifiers changed from: private */
    public final Executor mMainExecutor;
    private final Handler mMainThreadHandler;
    private final int mMinArrowPosition;
    private final int mNavBarHeight;
    private int mOneHandHorizontalShift;
    private int mOneHandedGestureKeyDistanceXThreshold;
    private int mOneHandedGestureKeyDistanceYThreshold;
    private ContentObserver mOneHandedGestureObserver;
    private int mOneHandedSlideTimeThreshold;
    private OpGestureButtonViewController mOpGestureButtonViewController;
    private OpSideGestureConfiguration mOpSideGestureConfiguration;
    private final OverviewProxyService mOverviewProxyService;
    private RegionSamplingHelper mRegionSamplingHelper;
    private int mRightInset;
    private int mRotation;
    private int mRunningTaskId;
    /* access modifiers changed from: private */
    public final Rect mSamplingRect = new Rect();
    private int mScreenHeight;
    private int mScreenWidth;
    private int mSideGestureKeyAnimThreshold;
    private int mSideGestureKeyDistanceThreshold;
    private int mSwipeStartThreshold;
    private final TaskStackChangeListener mTaskStackChangeListener;
    private boolean mThresholdCrossed = false;
    private final float mTouchSlop;
    private final WindowManager mWm;

    class SysUiInputEventReceiver extends InputEventReceiver {
        SysUiInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        public void onInputEvent(InputEvent inputEvent) {
            EdgeBackGestureHandler.this.onInputEvent(inputEvent);
            finishInputEvent(inputEvent, true);
        }
    }

    public void onDisplayAdded(int i) {
    }

    public void onDisplayRemoved(int i) {
    }

    public /* synthetic */ void lambda$new$0$EdgeBackGestureHandler(Boolean bool) {
        this.mDockedStackExists = bool.booleanValue();
    }

    public EdgeBackGestureHandler(Context context, OverviewProxyService overviewProxyService) {
        float f = SIDE_GESTURE_EDGE_MOVE_SCALE;
        this.mSideGestureKeyAnimThreshold = (int) (f * 1080.0f);
        this.mSideGestureKeyDistanceThreshold = (int) (SIDE_GESTURE_EDGE_BACK_SCALE * 1080.0f);
        this.mScreenHeight = -1;
        this.mScreenWidth = -1;
        this.mIsHidden = false;
        this.mSwipeStartThreshold = 50;
        this.mIsIgnoreCameraNotch = false;
        this.mIsOneHandedSettingEnable = 0;
        this.mOneHandedSlideTimeThreshold = ONE_HANDED_MODE_SLIDE_TIME_THRESHOLD;
        this.mOneHandedGestureKeyDistanceYThreshold = (int) (f * 1080.0f);
        this.mOneHandedGestureKeyDistanceXThreshold = this.mSideGestureKeyDistanceThreshold / ONE_HANDED_EDGE_HORIZONTAL_SCALE;
        this.mOneHandHorizontalShift = (int) (ONE_HANDED_GESTURE_EDGE_SHIFT_SCALE * 1080.0f);
        this.mIsOneHandedPerformed = false;
        this.mAllowOneHandedGesture = false;
        this.mAllowLeaveOneHandedGesture = false;
        this.mMainThreadHandler = new Handler(Looper.getMainLooper());
        this.mDockedListener = new Consumer() {
            public final void accept(Object obj) {
                EdgeBackGestureHandler.this.lambda$new$0$EdgeBackGestureHandler((Boolean) obj);
            }
        };
        this.mTaskStackChangeListener = new TaskStackChangeListener() {
            public void onTaskStackChanged() {
                super.onTaskStackChanged();
                EdgeBackGestureHandler.this.updateTopPackage();
            }
        };
        this.mDefaultHomeBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                EdgeBackGestureHandler.this.updateTopPackage();
            }
        };
        Resources resources = context.getResources();
        this.mContext = context;
        this.mDisplayId = context.getDisplayId();
        this.mMainExecutor = context.getMainExecutor();
        this.mWm = (WindowManager) context.getSystemService(WindowManager.class);
        this.mOverviewProxyService = overviewProxyService;
        this.mTouchSlop = ((float) ViewConfiguration.get(context).getScaledTouchSlop()) * 0.75f;
        this.mLongPressTimeout = Math.min(MAX_LONG_PRESS_TIMEOUT, ViewConfiguration.getLongPressTimeout());
        this.mNavBarHeight = resources.getDimensionPixelSize(R$dimen.navigation_bar_frame_height);
        this.mMinArrowPosition = resources.getDimensionPixelSize(R$dimen.navigation_edge_arrow_min_y);
        this.mFingerOffset = resources.getDimensionPixelSize(R$dimen.navigation_edge_finger_offset);
        updateCurrentUserResources(resources);
        this.mBar = (StatusBar) SysUiServiceProvider.getComponent(this.mContext, StatusBar.class);
        this.mOpGestureButtonViewController = this.mBar.getGestureButtonController();
        int i = this.mScreenWidth;
        this.mSideGestureKeyAnimThreshold = (int) (((float) i) * SIDE_GESTURE_EDGE_MOVE_SCALE);
        this.mSideGestureKeyDistanceThreshold = (int) (((float) i) * SIDE_GESTURE_EDGE_BACK_SCALE);
        this.mDisplay = this.mWm.getDefaultDisplay();
        this.mOpSideGestureConfiguration = new OpSideGestureConfiguration(this.mDisplay);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mDisplay.getRealMetrics(displayMetrics);
        this.mScreenHeight = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
        this.mScreenWidth = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        this.mOneHandedGestureKeyDistanceXThreshold = this.mSideGestureKeyDistanceThreshold / ONE_HANDED_EDGE_HORIZONTAL_SCALE;
        int i2 = this.mScreenWidth;
        this.mOneHandedGestureKeyDistanceYThreshold = (int) (((float) i2) * ONE_HANDED_EDGE_VERTICAL_SCALE);
        this.mOneHandHorizontalShift = (int) (((float) i2) * ONE_HANDED_GESTURE_EDGE_SHIFT_SCALE);
        mCameraNotchHeight = this.mContext.getResources().getDimensionPixelSize(17105422);
        if (ViewRootImplInjector.IS_SUPPORT_CAMERA_NOTCH) {
            this.mIsIgnoreCameraNotch = true;
        }
        this.mActivityManagerWrapper = ActivityManagerWrapper.getInstance();
        this.mDefaultHomeIntentFilter = new IntentFilter();
        for (String addAction : DEFAULT_HOME_CHANGE_ACTIONS) {
            this.mDefaultHomeIntentFilter.addAction(addAction);
        }
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("is Support one hand feature ");
            sb.append(OpFeatures.isSupport(new int[]{254}));
            Log.d("EdgeBackGestureHandler", sb.toString());
        }
        if (OpFeatures.isSupport(new int[]{254})) {
            this.mOneHandedGestureObserver = new ContentObserver(this.mMainThreadHandler) {
                public void onChange(boolean z) {
                    int intForUser = System.getIntForUser(EdgeBackGestureHandler.this.mContext.getContentResolver(), "op_one_hand_mode_setting", 0, -2);
                    EdgeBackGestureHandler edgeBackGestureHandler = EdgeBackGestureHandler.this;
                    edgeBackGestureHandler.mIsOneHandedPerformed = "1".equals(Global.getStringForUser(edgeBackGestureHandler.mContext.getContentResolver(), "one_hand_mode_status", -1));
                    if (OpUtils.DEBUG_ONEPLUS) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Trigger change by self. one handed mode type from ");
                        sb.append(EdgeBackGestureHandler.this.mIsOneHandedSettingEnable);
                        sb.append(" to ");
                        sb.append(intForUser);
                        Log.d("EdgeBackGestureHandler", sb.toString());
                    }
                    EdgeBackGestureHandler.this.mIsOneHandedSettingEnable = intForUser;
                    EdgeBackGestureHandler.this.updateIsOneHandedEnabled();
                }

                public void onChange(boolean z, Uri uri) {
                    String str = "op_one_hand_mode_setting";
                    String str2 = " to ";
                    String str3 = "EdgeBackGestureHandler";
                    if (uri.equals(System.getUriFor(str))) {
                        int intForUser = System.getIntForUser(EdgeBackGestureHandler.this.mContext.getContentResolver(), str, 0, -2);
                        if (OpUtils.DEBUG_ONEPLUS) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Trigger change by observer uri. one handed mode type from ");
                            sb.append(EdgeBackGestureHandler.this.mIsOneHandedSettingEnable);
                            sb.append(str2);
                            sb.append(intForUser);
                            Log.d(str3, sb.toString());
                        }
                        EdgeBackGestureHandler.this.mIsOneHandedSettingEnable = intForUser;
                        EdgeBackGestureHandler.this.updateIsOneHandedEnabled();
                    }
                    String str4 = "one_hand_mode_status";
                    if (uri.equals(Global.getUriFor(str4))) {
                        boolean equals = "1".equals(Global.getStringForUser(EdgeBackGestureHandler.this.mContext.getContentResolver(), str4, -1));
                        if (OpUtils.DEBUG_ONEPLUS) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("Trigger chang by observer uri. one handed mode status from ");
                            sb2.append(EdgeBackGestureHandler.this.mIsOneHandedPerformed);
                            sb2.append(str2);
                            sb2.append(equals);
                            Log.d(str3, sb2.toString());
                        }
                        EdgeBackGestureHandler.this.mIsOneHandedPerformed = equals;
                        if (!equals) {
                            EdgeBackGestureHandler.this.notifyLeaveOneHandedMode();
                        }
                    }
                }
            };
        }
    }

    public void updateCurrentUserResources(Resources resources) {
        this.mEdgeWidth = resources.getDimensionPixelSize(17105049);
    }

    public void onNavBarAttached() {
        Log.d("EdgeBackGestureHandler", "onNavBarAttached");
        this.mIsAttached = true;
        updateIsEnabled();
        if (this.mOneHandedGestureObserver != null) {
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor("op_one_hand_mode_setting"), true, this.mOneHandedGestureObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("one_hand_mode_status"), true, this.mOneHandedGestureObserver, -1);
            this.mOneHandedGestureObserver.onChange(true);
        }
        DockedStackExistsListener.register(this.mDockedListener);
        this.mActivityManagerWrapper.registerTaskStackListener(this.mTaskStackChangeListener);
        this.mContext.registerReceiver(this.mDefaultHomeBroadcastReceiver, this.mDefaultHomeIntentFilter);
    }

    public void onNavBarDetached() {
        Log.d("EdgeBackGestureHandler", "onNavBarDetached");
        this.mIsAttached = false;
        updateIsEnabled();
        if (this.mOneHandedGestureObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mOneHandedGestureObserver);
        }
        this.mActivityManagerWrapper.unregisterTaskStackListener(this.mTaskStackChangeListener);
        this.mIsOneHandedSettingEnable = 0;
        this.mContext.unregisterReceiver(this.mDefaultHomeBroadcastReceiver);
    }

    public void onNavigationModeChanged(int i, Context context) {
        this.mIsGesturalModeEnabled = QuickStepContract.isGesturalMode(i);
        updateIsEnabled();
        updateCurrentUserResources(context.getResources());
    }

    private void disposeInputChannel() {
        InputEventReceiver inputEventReceiver = this.mInputEventReceiver;
        if (inputEventReceiver != null) {
            inputEventReceiver.dispose();
            this.mInputEventReceiver = null;
        }
        InputMonitor inputMonitor = this.mInputMonitor;
        if (inputMonitor != null) {
            inputMonitor.dispose();
            this.mInputMonitor = null;
        }
    }

    private void updateIsEnabled() {
        StringBuilder sb = new StringBuilder();
        sb.append("updateIsEnabled: ");
        sb.append(this.mIsGesturalModeEnabled);
        sb.append(", Attach: ");
        sb.append(this.mIsAttached);
        sb.append(", enable: ");
        sb.append(this.mIsEnabled);
        sb.append(", isHidden: ");
        sb.append(this.mIsHidden);
        String str = "EdgeBackGestureHandler";
        Log.d(str, sb.toString());
        boolean z = true;
        boolean z2 = (this.mIsAttached || this.mIsHidden) && this.mIsGesturalModeEnabled;
        if (z2 != this.mIsEnabled) {
            this.mIsEnabled = z2;
            disposeInputChannel();
            OpSideGestureNavView opSideGestureNavView = this.mEdgePanel;
            if (opSideGestureNavView != null) {
                this.mWm.removeView(opSideGestureNavView);
                this.mEdgePanel = null;
                this.mRegionSamplingHelper.stop();
                this.mRegionSamplingHelper = null;
            }
            if (!this.mIsEnabled) {
                WindowManagerWrapper.getInstance().removePinnedStackListener(this.mImeChangedListener);
                ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).unregisterDisplayListener(this);
                try {
                    if (((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(this.mDisplayId) != null) {
                        WindowManagerGlobal.getWindowManagerService().unregisterSystemGestureExclusionListener(this.mGestureExclusionListener, this.mDisplayId);
                    } else {
                        Log.d(str, "It is not unregister system gesture exclusion listener, because display is null or display already removed.");
                    }
                } catch (RemoteException e) {
                    Log.e(str, "Failed to unregister window manager callbacks", e);
                }
                OpGestureButtonViewController opGestureButtonViewController = this.mOpGestureButtonViewController;
                if (!this.mIsEnabled && !isOneHandedSettingEnable()) {
                    z = false;
                }
                opGestureButtonViewController.updateRegion(z);
            } else {
                updateDisplaySize();
                ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).registerDisplayListener(this, this.mContext.getMainThreadHandler());
                try {
                    if (((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(this.mDisplayId) != null) {
                        WindowManagerWrapper.getInstance().addPinnedStackListener(this.mImeChangedListener);
                        WindowManagerGlobal.getWindowManagerService().registerSystemGestureExclusionListener(this.mGestureExclusionListener, this.mDisplayId);
                    } else {
                        Log.d(str, "It is not register system gesture exclusion listener, because display is null or display already removed.");
                    }
                } catch (RemoteException e2) {
                    Log.e(str, "Failed to register window manager callbacks", e2);
                }
                this.mInputMonitor = InputManager.getInstance().monitorGestureInput("edge-swipe", this.mDisplayId);
                this.mInputEventReceiver = new SysUiInputEventReceiver(this.mInputMonitor.getInputChannel(), Looper.getMainLooper());
                this.mEdgePanel = new OpSideGestureNavView(this.mContext, 0, 0);
                LayoutParams layoutParams = new LayoutParams(OpSideGestureConfiguration.getWindowWidth(), OpSideGestureConfiguration.getWindowHeight(0), 2024, 8388904, -3);
                this.mEdgePanelLp = layoutParams;
                LayoutParams layoutParams2 = this.mEdgePanelLp;
                StringBuilder sb2 = new StringBuilder();
                sb2.append(str);
                sb2.append(this.mDisplayId);
                layoutParams2.setTitle(sb2.toString());
                this.mEdgePanelLp.accessibilityTitle = this.mContext.getString(R$string.nav_bar_edge_panel);
                LayoutParams layoutParams3 = this.mEdgePanelLp;
                layoutParams3.windowAnimations = 0;
                this.mEdgePanel.setLayoutParams(layoutParams3);
                this.mWm.addView(this.mEdgePanel, this.mEdgePanelLp);
                this.mRegionSamplingHelper = new RegionSamplingHelper(this.mEdgePanel, new SamplingCallback() {
                    public void onRegionDarknessChanged(boolean z) {
                        EdgeBackGestureHandler.this.mEdgePanel.setIsDark(!z, true);
                    }

                    public Rect getSampledRegion(View view) {
                        return EdgeBackGestureHandler.this.mSamplingRect;
                    }
                });
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateIsOneHandedEnabled() {
        String str = "EdgeBackGestureHandler";
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("isOneHandedSettingEnable() ");
            sb.append(isOneHandedSettingEnable());
            sb.append(", Gesture Mode Enabled ");
            sb.append(this.mIsEnabled);
            Log.d(str, sb.toString());
        }
        if (!isOneHandedSettingEnable() && this.mIsOneHandedPerformed && OpSystemUIInjector.requestExitOneHandMode()) {
            Log.d(str, "One handed disable. Cancel One Handed immediate.");
            this.mIsOneHandedPerformed = false;
            notifyLeaveOneHandedMode();
        }
        if (this.mIsEnabled) {
            Log.d(str, "Do not enable again because it's already enabled.");
            return;
        }
        disposeInputChannel();
        if (!isOneHandedSettingEnable()) {
            Log.d(str, "unregister relate listener by one handed disable");
            ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).unregisterDisplayListener(this);
            try {
                if (((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(this.mDisplayId) != null) {
                    WindowManagerGlobal.getWindowManagerService().unregisterSystemGestureExclusionListener(this.mGestureExclusionListener, this.mDisplayId);
                } else {
                    Log.d(str, "It is not unregister system gesture exclusion listener, because display is null or display already removed.");
                }
            } catch (RemoteException e) {
                Log.e(str, "Failed to unregister window manager callbacks", e);
            }
        } else {
            Log.d(str, "register relate listener by one handed enable");
            updateDisplaySize();
            ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).registerDisplayListener(this, this.mContext.getMainThreadHandler());
            try {
                if (((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(this.mDisplayId) != null) {
                    WindowManagerWrapper.getInstance().addPinnedStackListener(this.mImeChangedListener);
                    WindowManagerGlobal.getWindowManagerService().registerSystemGestureExclusionListener(this.mGestureExclusionListener, this.mDisplayId);
                } else {
                    Log.d(str, "It is not register system gesture exclusion listener, because display is null or display already removed.");
                }
            } catch (RemoteException e2) {
                Log.e(str, "Failed to register window manager callbacks", e2);
            }
            this.mInputMonitor = InputManager.getInstance().monitorGestureInput("edge-swipe", this.mDisplayId);
            this.mInputEventReceiver = new SysUiInputEventReceiver(this.mInputMonitor.getInputChannel(), Looper.getMainLooper());
        }
    }

    private boolean isPortrait() {
        int i = this.mRotation;
        return i == 0 || i == 2;
    }

    /* access modifiers changed from: private */
    public void onInputEvent(InputEvent inputEvent) {
        if (inputEvent instanceof MotionEvent) {
            onMotionEvent((MotionEvent) inputEvent);
        }
    }

    private boolean isYInTouchRegion(int i) {
        return ((float) i) > ((float) getScreenHeight(this.mRotation)) * OpSideGestureConfiguration.PORTRAIT_NON_DETECT_SCALE;
    }

    private boolean isYInLeaveOneHandedTouchRegion(int i) {
        return ((float) i) < ((float) getScreenHeight(this.mRotation)) * PORTRAIT_LEAVE_ONE_HANDED_SCALE;
    }

    private boolean isWithinTouchRegion(int i, int i2) {
        if (!this.mIsGesturalModeEnabled) {
            return false;
        }
        if (!sSideGestureEnabled) {
            if (isPortrait()) {
                if (i2 < this.mScreenHeight - this.mSwipeStartThreshold) {
                    return false;
                }
                int i3 = this.mScreenWidth;
                if (i < i3 / 3 || i > (i3 * 2) / 3) {
                    return true;
                }
            } else if ((this.mRotation == 1 && i < this.mScreenHeight - this.mSwipeStartThreshold) || (this.mRotation == 3 && i > this.mSwipeStartThreshold)) {
                return false;
            } else {
                int i4 = this.mScreenWidth;
                if (i2 < i4 / 3 || i2 > (i4 * 2) / 3) {
                    return true;
                }
            }
            return false;
        }
        OpSideGestureNavView opSideGestureNavView = this.mEdgePanel;
        if ((opSideGestureNavView != null && !opSideGestureNavView.isExitAnimFinished()) || i2 > this.mDisplaySize.y - this.mNavBarHeight || !isYInTouchRegion(i2)) {
            return false;
        }
        int adjuestEdgeThreshold = adjuestEdgeThreshold(i, i2, this.mRotation);
        if (i > adjuestEdgeThreshold && i < this.mDisplaySize.x - adjuestEdgeThreshold) {
            return false;
        }
        boolean contains = this.mExcludeRegion.contains(i, i2);
        if (contains) {
            this.mOverviewProxyService.notifyBackAction(false, -1, -1, false, !this.mIsOnLeftEdge);
        }
        return !contains;
    }

    private boolean isWithinOneHandedTouchRegion(int i, int i2) {
        String str = "EdgeBackGestureHandler";
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("isOneHandedSettingEnable() ");
            sb.append(isOneHandedSettingEnable());
            sb.append(", isPortrait() ");
            sb.append(isPortrait());
            sb.append(", mDockedStackExists ");
            sb.append(this.mDockedStackExists);
            Log.d(str, sb.toString());
        }
        boolean z = false;
        if (isOneHandedSettingEnable() && isPortrait() && !this.mDockedStackExists && i2 <= this.mDisplaySize.y - this.mNavBarHeight && isYInTouchRegion(i2)) {
            int adjuestEdgeThreshold = adjuestEdgeThreshold(i, i2, this.mRotation);
            if (i < 0 || i >= this.mOneHandHorizontalShift) {
                int i3 = this.mOneHandHorizontalShift;
                if (i <= adjuestEdgeThreshold + i3 || i >= (this.mDisplaySize.x - adjuestEdgeThreshold) - i3) {
                    int i4 = this.mDisplaySize.x;
                    if (i > i4 || i <= i4 - this.mOneHandHorizontalShift) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("isWithinOneHandedTouchRegion isHomeApp ");
                        sb2.append(OpUtils.isHomeApp());
                        sb2.append(" ,mIsOneHandedPerformed ");
                        sb2.append(this.mIsOneHandedPerformed);
                        sb2.append(" inExcludeRegion ");
                        sb2.append(this.mExcludeRegion.contains(i, i2));
                        Log.d(str, sb2.toString());
                        if (this.mIsOneHandedPerformed || OpUtils.isHomeApp() || !this.mExcludeRegion.contains(i, i2)) {
                            z = true;
                        }
                    }
                }
            }
            return false;
        }
        return z;
    }

    private void cancelGesture(MotionEvent motionEvent) {
        if (this.mAllowGesture) {
            this.mAllowGesture = false;
            MotionEvent obtain = MotionEvent.obtain(motionEvent);
            obtain.setAction(3);
            this.mEdgePanel.handleTouch(obtain);
            obtain.recycle();
        }
    }

    private void onMotionEvent(MotionEvent motionEvent) {
        MotionEvent motionEvent2 = motionEvent;
        float rawX = motionEvent.getRawX();
        float realRawY = motionEvent.getRealRawY();
        int actionMasked = motionEvent.getActionMasked();
        String str = "EdgeBackGestureHandler";
        boolean z = false;
        if (actionMasked == 0) {
            int systemUiStateFlags = this.mOverviewProxyService.getSystemUiStateFlags();
            this.mIsOnLeftEdge = motionEvent.getX() <= ((float) adjuestEdgeThreshold((int) motionEvent.getX(), (int) motionEvent.getY(), this.mRotation));
            boolean isQsDetailShowing = OpLsState.getInstance().getPhoneStatusBar().mNotificationPanel.isQsDetailShowing();
            this.mAllowGesture = (!QuickStepContract.isBackGestureDisabled(systemUiStateFlags) || isQsDetailShowing) && isWithinTouchRegion((int) motionEvent.getX(), (int) motionEvent.getY());
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("mAllowGesture: ");
                sb.append(!QuickStepContract.isBackGestureDisabled(systemUiStateFlags));
                sb.append(", ");
                sb.append(isQsDetailShowing);
                Log.d(str, sb.toString());
            }
            this.mAllowOneHandedGesture = isWithinOneHandedTouchRegion((int) motionEvent.getX(), (int) motionEvent.getY());
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("mAllowOneHandedGesture ");
                sb2.append(this.mAllowOneHandedGesture);
                Log.d(str, sb2.toString());
            }
            if (this.mAllowGesture || this.mAllowOneHandedGesture) {
                if (Build.DEBUG_ONEPLUS) {
                    Slog.d(str, "AllowGesture down");
                }
                if (this.mEdgePanel != null) {
                    this.mEdgePanelLp.gravity = this.mIsOnLeftEdge ? 83 : 85;
                    this.mEdgePanel.setIsLeftPanel(this.mIsOnLeftEdge);
                    this.mEdgePanel.handleTouch(motionEvent2);
                    this.mWm.updateViewLayout(this.mEdgePanel, this.mEdgePanelLp);
                    this.mRegionSamplingHelper.start(this.mSamplingRect);
                }
                this.mDownPoint.set(motionEvent.getX(), motionEvent.getY());
                this.mThresholdCrossed = false;
            }
            this.mAllowLeaveOneHandedGesture = isOneHandedSettingEnable() && isYInLeaveOneHandedTouchRegion((int) motionEvent.getY());
        } else if (this.mAllowGesture || this.mAllowOneHandedGesture) {
            if (!this.mThresholdCrossed) {
                if (actionMasked == 5) {
                    cancelGesture(motionEvent);
                    return;
                } else if (actionMasked == 2) {
                    if (motionEvent.getEventTime() - motionEvent.getDownTime() > ((long) this.mLongPressTimeout)) {
                        cancelGesture(motionEvent);
                        return;
                    }
                    float abs = Math.abs(motionEvent.getX() - this.mDownPoint.x);
                    float abs2 = Math.abs(motionEvent.getY() - this.mDownPoint.y);
                    if (this.mAllowOneHandedGesture) {
                        if (abs < ((float) this.mOneHandedGestureKeyDistanceXThreshold) && abs2 > ((float) this.mOneHandedGestureKeyDistanceYThreshold) && motionEvent.getEventTime() - motionEvent.getDownTime() < ((long) this.mOneHandedSlideTimeThreshold) && isPortrait()) {
                            if (motionEvent.getY() > this.mDownPoint.y && !this.mIsOneHandedPerformed && OpSystemUIInjector.requestStartOneHandMode()) {
                                this.mIsOneHandedPerformed = true;
                                Log.d(str, "One Handed Started.");
                                if (this.mOverviewProxyService.getSysUIProxy() != null) {
                                    try {
                                        if (OpUtils.DEBUG_ONEPLUS) {
                                            Log.d(str, "notifyGestureStarted");
                                        }
                                        this.mOverviewProxyService.getSysUIProxy().notifyGestureStarted();
                                    } catch (RemoteException e) {
                                        StringBuilder sb3 = new StringBuilder();
                                        sb3.append(" notifyGestureStarted , ");
                                        sb3.append(e);
                                        Log.w(str, sb3.toString());
                                    }
                                }
                                cancelGesture(motionEvent);
                                return;
                            } else if (motionEvent.getY() < this.mDownPoint.y && this.mIsOneHandedPerformed && OpSystemUIInjector.requestExitOneHandMode()) {
                                this.mIsOneHandedPerformed = false;
                                Log.d(str, "Has left One Handed.");
                                notifyLeaveOneHandedMode();
                                cancelGesture(motionEvent);
                                return;
                            }
                        }
                    }
                    if (!sSideGestureEnabled && this.mAllowGesture) {
                        if (!isPortrait() ? abs > ((float) GESTURE_KEY_DISTANCE_THRESHOLD) : abs2 > ((float) GESTURE_KEY_DISTANCE_THRESHOLD)) {
                            Log.i(str, "onMotionEvent sendBackKey");
                            sendEvent(0, 4);
                            sendEvent(1, 4);
                            this.mOpGestureButtonViewController.onBackAction(this.mDownPoint.x);
                            cancelGesture(motionEvent);
                        }
                        return;
                    } else if (this.mAllowGesture) {
                        int i = this.mSideGestureKeyAnimThreshold;
                        if (abs > ((float) i)) {
                            this.mThresholdCrossed = true;
                            this.mInputMonitor.pilferPointers();
                            this.mEdgePanel.onDownEvent();
                        } else if (abs2 > ((float) i) * SIDE_GESTURE_EDGE_HORIZONTAL_SCALE && (!isOneHandedSettingEnable() || (isOneHandedSettingEnable() && motionEvent.getEventTime() - motionEvent.getDownTime() > ((long) this.mOneHandedSlideTimeThreshold)))) {
                            StringBuilder sb4 = new StringBuilder();
                            sb4.append("Swipe too skew ");
                            sb4.append(abs2);
                            Log.d(str, sb4.toString());
                            if (OpUtils.DEBUG_ONEPLUS) {
                                StringBuilder sb5 = new StringBuilder();
                                sb5.append("One-Handed mode enable ");
                                sb5.append(isOneHandedSettingEnable());
                                sb5.append(", move over 100ms ");
                                if (motionEvent.getEventTime() - motionEvent.getDownTime() > ((long) this.mOneHandedSlideTimeThreshold)) {
                                    z = true;
                                }
                                sb5.append(z);
                                Log.d(str, sb5.toString());
                            }
                            cancelGesture(motionEvent);
                            return;
                        }
                    }
                }
            }
            if (this.mAllowGesture) {
                this.mEdgePanel.handleTouch(motionEvent2);
                boolean isYInTouchRegion = isYInTouchRegion((int) motionEvent.getY());
                int rotation = this.mDisplay.getRotation();
                float abs3 = Math.abs(motionEvent.getX() - this.mDownPoint.x);
                int i2 = abs3 > ((float) this.mSideGestureKeyDistanceThreshold) ? 2 : 1;
                int i3 = (actionMasked == 2 && !isYInTouchRegion && i2 == 2) ? 4 : i2;
                OpSideGestureNavView opSideGestureNavView = this.mEdgePanel;
                GesturePointContainer gesturePointContainer = new GesturePointContainer(new PointF(rawX, realRawY), i3, this.mIsOnLeftEdge ^ true ? 1 : 0, rotation, this.mScreenHeight, this.mScreenWidth);
                opSideGestureNavView.onUpdateGestureView(gesturePointContainer);
                boolean z2 = actionMasked == 1;
                boolean z3 = z2 && abs3 > ((float) this.mSideGestureKeyDistanceThreshold) && isYInTouchRegion;
                if (z2) {
                    OpSideGestureNavView opSideGestureNavView2 = this.mEdgePanel;
                    GesturePointContainer gesturePointContainer2 = new GesturePointContainer(new PointF(rawX, realRawY), 2, this.mIsOnLeftEdge ^ true ? 1 : 0, rotation, this.mScreenHeight, this.mScreenWidth);
                    opSideGestureNavView2.onGestureFinished(gesturePointContainer2);
                } else if (actionMasked == 3 || actionMasked == 5) {
                    OpSideGestureNavView opSideGestureNavView3 = this.mEdgePanel;
                    GesturePointContainer gesturePointContainer3 = new GesturePointContainer(new PointF(rawX, realRawY), 2, this.mIsOnLeftEdge ^ true ? 1 : 0, rotation, this.mScreenHeight, this.mScreenWidth);
                    opSideGestureNavView3.onGestureFinished(gesturePointContainer3);
                    this.mEdgePanel.onUpEvent();
                    cancelGesture(motionEvent);
                }
                if (z2 && z3) {
                    boolean shouldTriggerBack = this.mEdgePanel.shouldTriggerBack();
                    if (shouldTriggerBack) {
                        sendEvent(0, 4);
                        sendEvent(1, 4);
                    }
                    OverviewProxyService overviewProxyService = this.mOverviewProxyService;
                    PointF pointF = this.mDownPoint;
                    overviewProxyService.notifyBackAction(shouldTriggerBack, (int) pointF.x, (int) pointF.y, false, !this.mIsOnLeftEdge);
                    this.mAllowLeaveOneHandedGesture = false;
                }
                if (z2 || actionMasked == 3) {
                    this.mRegionSamplingHelper.stop();
                } else {
                    updateSamplingRect();
                    this.mRegionSamplingHelper.updateSamplingRect();
                }
            }
        }
        if (this.mAllowLeaveOneHandedGesture && actionMasked == 1 && isYInLeaveOneHandedTouchRegion((int) motionEvent.getY()) && this.mIsOneHandedPerformed && isPortrait() && OpSystemUIInjector.requestExitOneHandMode()) {
            Log.d(str, "Touch leave region to leave One Handed.");
            this.mIsOneHandedPerformed = false;
            notifyLeaveOneHandedMode();
        }
    }

    private void updateSamplingRect() {
        LayoutParams layoutParams = this.mEdgePanelLp;
        int i = layoutParams.y;
        int i2 = this.mIsOnLeftEdge ? this.mLeftInset : (this.mDisplaySize.x - this.mRightInset) - layoutParams.width;
        this.mSamplingRect.set(i2, i, this.mEdgePanelLp.width + i2, this.mEdgePanelLp.height + i);
        this.mEdgePanel.adjustRectToBoundingBox(this.mSamplingRect);
    }

    public void onDisplayChanged(int i) {
        if (i == this.mDisplayId) {
            updateDisplaySize();
            this.mRotation = this.mDisplay.getRotation();
        }
    }

    private void updateDisplaySize() {
        String str = "EdgeBackGestureHandler";
        if (((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(this.mDisplayId) == null) {
            Log.d(str, "It's not update display size, because display is null or display already removed.");
            return;
        }
        ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(this.mDisplayId).getRealSize(this.mDisplaySize);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mDisplay = this.mWm.getDefaultDisplay();
        this.mDisplay.getRealMetrics(displayMetrics);
        this.mScreenHeight = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
        this.mScreenWidth = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        int i = this.mScreenWidth;
        this.mEdgeSwipeStartThreshold = (int) (((float) i) * SIDE_GESTURE_EDGE_SCALE);
        this.mSideGestureKeyAnimThreshold = (int) (((float) i) * SIDE_GESTURE_EDGE_MOVE_SCALE);
        this.mSideGestureKeyDistanceThreshold = (int) (((float) i) * SIDE_GESTURE_EDGE_BACK_SCALE);
        this.mOpSideGestureConfiguration = new OpSideGestureConfiguration(this.mDisplay);
        this.mOneHandedGestureKeyDistanceXThreshold = this.mSideGestureKeyDistanceThreshold / ONE_HANDED_EDGE_HORIZONTAL_SCALE;
        this.mOneHandedGestureKeyDistanceYThreshold = (int) (((float) this.mScreenWidth) * ONE_HANDED_EDGE_VERTICAL_SCALE);
        this.mOpGestureButtonViewController.updateDisplaySize();
        this.mOpGestureButtonViewController.updateRegion(this.mIsEnabled || isOneHandedSettingEnable());
        this.mOneHandHorizontalShift = (int) (((float) this.mScreenWidth) * ONE_HANDED_GESTURE_EDGE_SHIFT_SCALE);
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("SIDE_GESTURE_EDGE_SCALE ");
            sb.append(SIDE_GESTURE_EDGE_SCALE);
            sb.append("\n ONE_HANDED_GESTURE_EDGE_SHIFT_SCALE ");
            sb.append(ONE_HANDED_GESTURE_EDGE_SHIFT_SCALE);
            sb.append("\n ONE_HANDED_MODE_SLIDE_TIME_THRESHOLD ");
            sb.append(ONE_HANDED_MODE_SLIDE_TIME_THRESHOLD);
            sb.append("\n ONE_HANDED_EDGE_VERTICAL_SCALE ");
            sb.append(ONE_HANDED_EDGE_VERTICAL_SCALE);
            sb.append("\n SIDE_GESTURE_EDGE_BACK_SCALE ");
            sb.append(SIDE_GESTURE_EDGE_BACK_SCALE);
            sb.append("\n ONE_HANDED_EDGE_HORIZONTAL_SCALE ");
            sb.append(ONE_HANDED_EDGE_HORIZONTAL_SCALE);
            Log.d(str, sb.toString());
        }
    }

    private void sendEvent(int i, int i2) {
        long uptimeMillis = SystemClock.uptimeMillis();
        KeyEvent keyEvent = new KeyEvent(uptimeMillis, uptimeMillis, i, i2, 0, 0, -1, 0, 72, 257);
        int expandedDisplayId = ((BubbleController) Dependency.get(BubbleController.class)).getExpandedDisplayId(this.mContext);
        if (i2 == 4 && expandedDisplayId != -1) {
            keyEvent.setDisplayId(expandedDisplayId);
        }
        InputManager.getInstance().injectInputEvent(keyEvent, 0);
    }

    public void setInsets(int i, int i2) {
        this.mLeftInset = i;
        this.mRightInset = i2;
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("EdgeBackGestureHandler:");
        StringBuilder sb = new StringBuilder();
        sb.append("  mIsEnabled=");
        sb.append(this.mIsEnabled);
        printWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("  mAllowGesture=");
        sb2.append(this.mAllowGesture);
        printWriter.println(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append("  mExcludeRegion=");
        sb3.append(this.mExcludeRegion);
        printWriter.println(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append("  mImeHeight=");
        sb4.append(this.mImeHeight);
        printWriter.println(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append("  mIsAttached=");
        sb5.append(this.mIsAttached);
        printWriter.println(sb5.toString());
        StringBuilder sb6 = new StringBuilder();
        sb6.append("  mEdgeWidth=");
        sb6.append(this.mEdgeWidth);
        printWriter.println(sb6.toString());
    }

    public void onConfigurationChanged(int i) {
        if (this.mIsEnabled) {
            this.mRotation = i;
            updateDisplaySize();
            LayoutParams layoutParams = (LayoutParams) this.mEdgePanel.getLayoutParams();
            layoutParams.height = OpSideGestureConfiguration.getWindowHeight(i);
            layoutParams.width = OpSideGestureConfiguration.getWindowWidth();
            this.mWm.updateViewLayout(this.mEdgePanel, layoutParams);
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("Edge onConfigurationChanged rotation:");
                sb.append(i);
                sb.append(", height:");
                sb.append(layoutParams.height);
                sb.append(", width:");
                sb.append(layoutParams.width);
                Log.d("EdgeBackGestureHandler", sb.toString());
            }
            this.mEdgePanel.onConfigChanged(i);
        }
        updateIsOneHandedEnabled();
    }

    private int getScreenHeight(int i) {
        if (i == 0 || i == 2) {
            return this.mScreenHeight;
        }
        if (i == 1 || i == 3) {
            return this.mScreenWidth;
        }
        return this.mScreenHeight;
    }

    /* access modifiers changed from: 0000 */
    public int adjuestEdgeThreshold(int i, int i2, int i3) {
        int i4 = this.mEdgeSwipeStartThreshold;
        if (!ViewRootImplInjector.IS_SUPPORT_CAMERA_NOTCH) {
            return i4;
        }
        boolean z = i < this.mScreenHeight / 2;
        new Region();
        if ((i3 != 1 || !z) && (i3 != 3 || z)) {
            return (int) (((float) this.mScreenWidth) * SIDE_GESTURE_EDGE_SCALE);
        }
        return this.mIsIgnoreCameraNotch ? mCameraNotchHeight + this.mEdgeSwipeStartThreshold : i4;
    }

    private boolean isOneHandedSettingEnable() {
        return this.mIsOneHandedSettingEnable == 1;
    }

    /* access modifiers changed from: private */
    public void updateTopPackage() {
        RunningTaskInfo runningTask = this.mActivityManagerWrapper.getRunningTask();
        if (runningTask != null) {
            if (this.mRunningTaskId != runningTask.taskId && runningTask.topActivity != null && runningTask.topActivity.getPackageName() != null) {
                this.mRunningTaskId = runningTask.taskId;
                OpUtils.updateTopPackage(this.mContext, runningTask.topActivity.getPackageName());
            } else {
                return;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("updateTopPackage isHomeApp ");
        sb.append(OpUtils.isHomeApp());
        Log.d("EdgeBackGestureHandler", sb.toString());
    }

    /* access modifiers changed from: private */
    public void notifyLeaveOneHandedMode() {
        String str = "EdgeBackGestureHandler";
        if (Build.DEBUG_ONEPLUS) {
            Log.d(str, "notifyLeaveOneHandedMode");
        }
        if (this.mOverviewProxyService.getSysUIProxy() != null) {
            try {
                this.mOverviewProxyService.getSysUIProxy().notifyGestureEnded(50);
            } catch (RemoteException e) {
                StringBuilder sb = new StringBuilder();
                sb.append(" notifyGestureEnded , ");
                sb.append(e);
                Log.w(str, sb.toString());
            }
        }
    }
}

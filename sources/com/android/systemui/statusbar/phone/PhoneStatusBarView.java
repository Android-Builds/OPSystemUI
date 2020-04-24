package com.android.systemui.statusbar.phone;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Debug;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.OpFeatures;
import android.util.Pair;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.android.systemui.Dependency;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.ScreenDecorations.DisplayCutoutView;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.statusbar.CommandQueue;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.statusbar.phone.OpPhoneStatusBarView;
import com.oneplus.util.OpUtils;
import java.util.Objects;

public class PhoneStatusBarView extends OpPhoneStatusBarView {
    public static int MODE_COMPAT = 100;
    public static int MODE_FULL = 102;
    private static boolean mIsFullScreenMode;
    private AppOpsManager mAppOps;
    StatusBar mBar;
    private final PhoneStatusBarTransitions mBarTransitions = new PhoneStatusBarTransitions(this);
    private DarkReceiver mBattery;
    private View mCenterIconSpace;
    private final CommandQueue mCommandQueue;
    private int mCutoutSideNudge = 0;
    private View mCutoutSpace;
    private DisplayCutout mDisplayCutout;
    private Runnable mHideExpandedRunnable = new Runnable() {
        public void run() {
            PhoneStatusBarView phoneStatusBarView = PhoneStatusBarView.this;
            if (phoneStatusBarView.mPanelFraction == 0.0f) {
                phoneStatusBarView.mBar.makeExpandedInvisible();
            }
        }
    };
    boolean mIsFullyOpenedPanel = false;
    private int mLastOrientation;
    private float mMinFraction;
    private OverviewProxyService mOverviewProxyService = ((OverviewProxyService) Dependency.get(OverviewProxyService.class));
    private ScrimController mScrimController;
    private String mTopActivityPackage = "";

    public PhoneStatusBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCommandQueue = (CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class);
    }

    public BarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public void setBar(StatusBar statusBar) {
        this.mBar = statusBar;
    }

    public void setScrimController(ScrimController scrimController) {
        this.mScrimController = scrimController;
    }

    public void onFinishInflate() {
        this.mBarTransitions.init();
        this.mBattery = (DarkReceiver) findViewById(R$id.battery);
        this.mCutoutSpace = findViewById(R$id.cutout_space_view);
        this.mCenterIconSpace = findViewById(R$id.centered_icon_area);
        super.onFinishInflate();
        updateResources();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this.mBattery);
        if (updateOrientationAndCutout(getResources().getConfiguration().orientation)) {
            updateLayoutForCutout();
        }
        ((HighlightHintController) Dependency.get(HighlightHintController.class)).addCallback(this);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this.mBattery);
        this.mDisplayCutout = null;
        ((HighlightHintController) Dependency.get(HighlightHintController.class)).removeCallback(this);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (updateOrientationAndCutout(configuration.orientation)) {
            updateLayoutForCutout();
            requestLayout();
        }
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        if (updateOrientationAndCutout(this.mLastOrientation)) {
            updateLayoutForCutout();
            requestLayout();
        }
        return super.onApplyWindowInsets(windowInsets);
    }

    private boolean updateOrientationAndCutout(int i) {
        boolean z;
        if (i == Integer.MIN_VALUE || this.mLastOrientation == i) {
            z = false;
        } else {
            this.mLastOrientation = i;
            z = true;
        }
        if (Objects.equals(getRootWindowInsets().getDisplayCutout(), this.mDisplayCutout)) {
            return z;
        }
        this.mDisplayCutout = getRootWindowInsets().getDisplayCutout();
        return true;
    }

    private boolean checkFullScreenMode(String str) {
        boolean isFullAndNotchEnabled = isFullAndNotchEnabled(str);
        if (isFullAndNotchEnabled == mIsFullScreenMode) {
            return false;
        }
        mIsFullScreenMode = isFullAndNotchEnabled;
        return true;
    }

    private boolean isFullAndNotchEnabled(String str) {
        boolean z = true;
        if (OpFeatures.isSupport(new int[]{67}) && !TextUtils.isEmpty(str)) {
            StatusBar statusBar = this.mBar;
            boolean isCameraNotchIgnoreSetting = statusBar != null ? statusBar.isCameraNotchIgnoreSetting() : false;
            try {
                ApplicationInfo applicationInfo = getContext().getPackageManager().getApplicationInfo(str, 1);
                if (applicationInfo == null) {
                    return false;
                }
                int i = applicationInfo.uid;
                if (isCameraNotchIgnoreSetting || getCompatMode(this.mAppOps, str, i) != 1) {
                    z = false;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("isFullAndNotchEnabled() isFullMode=");
                sb.append(z);
                sb.append(" pkg=");
                sb.append(str);
                sb.append(" isCameraNotchIgnoreSetting=");
                sb.append(isCameraNotchIgnoreSetting);
                sb.append(", uid=");
                sb.append(i);
                Log.i("PhoneStatusBarView", sb.toString());
                return z;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private int getCompatMode(AppOpsManager appOpsManager, String str, int i) {
        StatusBar statusBar = this.mBar;
        int i2 = 0;
        boolean isCameraNotchIgnoreSetting = statusBar != null ? statusBar.isCameraNotchIgnoreSetting() : false;
        String str2 = "PhoneStatusBarView";
        if (appOpsManager != null) {
            try {
                int checkOpNoThrow = appOpsManager.checkOpNoThrow(1006, i, str);
                if (checkOpNoThrow != 2) {
                    if (checkOpNoThrow != MODE_FULL) {
                        if (checkOpNoThrow == 0 || checkOpNoThrow == MODE_COMPAT) {
                            i2 = 2;
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("getCompatMode: mode:");
                        sb.append(i2);
                        sb.append(" ops:");
                        sb.append(checkOpNoThrow);
                        Log.d(str2, sb.toString());
                    }
                }
                if (!isCameraNotchIgnoreSetting) {
                    i2 = 1;
                }
                StringBuilder sb2 = new StringBuilder();
                sb2.append("getCompatMode: mode:");
                sb2.append(i2);
                sb2.append(" ops:");
                sb2.append(checkOpNoThrow);
                Log.d(str2, sb2.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(str2, "AppOps is null");
        }
        return i2;
    }

    public void updateTopPackage(String str) {
        if (TextUtils.isEmpty(this.mTopActivityPackage) || !TextUtils.equals(this.mTopActivityPackage, str)) {
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("updateTopPackage pkg:");
                sb.append(str);
                Log.i("PhoneStatusBarView", sb.toString());
            }
            this.mTopActivityPackage = str;
            if (checkFullScreenMode(str)) {
                updateLayoutForCutout();
                requestLayout();
            }
        }
    }

    public boolean panelEnabled() {
        return this.mCommandQueue.panelsEnabled();
    }

    public boolean onRequestSendAccessibilityEventInternal(View view, AccessibilityEvent accessibilityEvent) {
        if (!super.onRequestSendAccessibilityEventInternal(view, accessibilityEvent)) {
            return false;
        }
        AccessibilityEvent obtain = AccessibilityEvent.obtain();
        onInitializeAccessibilityEvent(obtain);
        dispatchPopulateAccessibilityEvent(obtain);
        accessibilityEvent.appendRecord(obtain);
        return true;
    }

    public void onPanelPeeked() {
        super.onPanelPeeked();
        this.mBar.makeExpandedVisible(false);
    }

    public void onPanelCollapsed() {
        super.onPanelCollapsed();
        post(this.mHideExpandedRunnable);
        this.mIsFullyOpenedPanel = false;
        this.mBar.getNotificationScrollLayout().hideDismissAnimate(false);
    }

    public void removePendingHideExpandedRunnables() {
        StringBuilder sb = new StringBuilder();
        sb.append("removePendingHideExpandedRunnables: ");
        sb.append(Debug.getCallers(5));
        Log.i("PhoneStatusBarView", sb.toString());
        removeCallbacks(this.mHideExpandedRunnable);
    }

    public void onPanelFullyOpened() {
        super.onPanelFullyOpened();
        if (!this.mIsFullyOpenedPanel) {
            this.mPanel.sendAccessibilityEvent(32);
        }
        this.mIsFullyOpenedPanel = true;
        if (this.mBar.getNotificationScrollLayout().hasActiveClearableNotifications(0)) {
            this.mBar.getNotificationScrollLayout().showDismissAnimate(true);
        }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        return this.mBar.interceptTouchEvent(motionEvent) || super.onTouchEvent(motionEvent);
    }

    public void onTrackingStarted() {
        super.onTrackingStarted();
        this.mBar.onTrackingStarted();
        this.mScrimController.onTrackingStarted();
        removePendingHideExpandedRunnables();
    }

    public void onClosingFinished() {
        super.onClosingFinished();
        this.mBar.onClosingFinished();
    }

    public void onTrackingStopped(boolean z) {
        super.onTrackingStopped(z);
        this.mBar.onTrackingStopped(z);
    }

    public void onExpandingFinished() {
        super.onExpandingFinished();
        this.mScrimController.onExpandingFinished();
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return this.mBar.interceptTouchEvent(motionEvent) || super.onInterceptTouchEvent(motionEvent);
    }

    public void panelScrimMinFractionChanged(float f) {
        if (this.mMinFraction != f) {
            this.mMinFraction = f;
            updateScrimFraction();
        }
    }

    public void panelExpansionChanged(float f, boolean z) {
        super.panelExpansionChanged(f, z);
        updateScrimFraction();
        int i = (f > 0.0f ? 1 : (f == 0.0f ? 0 : -1));
        if ((i == 0 || f == 1.0f) && this.mBar.getNavigationBarView() != null) {
            this.mBar.getNavigationBarView().onPanelExpandedChange();
        }
        if (i == 0 || f == 1.0f) {
            OpLsState.getInstance().getPreventModeCtrl().onPanelExpandedChange(z);
        }
        if (this.mBar.getNavigationBarView() != null && this.mBar.needRefreshExpanded()) {
            this.mBar.getNavigationBarView().updateSystemUiStateFlags();
            Log.d("PhoneStatusBarView", "update SystemUi flag");
        }
        if ((i == 0 || f == 1.0f) && this.mBar.getNavigationBarView() == null) {
            this.mOverviewProxyService.updateSystemUIStateFlagsInternal();
        }
    }

    private void updateScrimFraction() {
        float f = this.mPanelFraction;
        float f2 = this.mMinFraction;
        if (f2 < 1.0f) {
            f = Math.max((f - f2) / (1.0f - f2), 0.0f);
        }
        this.mScrimController.setPanelExpansion(f);
    }

    public void updateResources() {
        this.mCutoutSideNudge = getResources().getDimensionPixelSize(R$dimen.display_cutout_margin_consumption);
        LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelSize(R$dimen.status_bar_height);
        setLayoutParams(layoutParams);
    }

    private void updateLayoutForCutout() {
        Pair cornerCutoutMargins = cornerCutoutMargins(this.mContext, this.mDisplayCutout, getDisplay());
        updateCutoutLocation(cornerCutoutMargins);
        updateSafeInsets(cornerCutoutMargins);
    }

    private void updateCutoutLocation(Pair<Integer, Integer> pair) {
        if (this.mCutoutSpace != null) {
            DisplayCutout displayCutout = this.mDisplayCutout;
            if (displayCutout == null || displayCutout.isEmpty() || this.mLastOrientation != 1 || pair != null) {
                this.mCenterIconSpace.setVisibility(0);
                this.mCutoutSpace.setVisibility(8);
                return;
            }
            this.mCenterIconSpace.setVisibility(8);
            this.mCutoutSpace.setVisibility(0);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mCutoutSpace.getLayoutParams();
            Rect rect = new Rect();
            DisplayCutoutView.boundsFromDirection(this.mContext, this.mDisplayCutout, 48, rect);
            int i = rect.left;
            int i2 = this.mCutoutSideNudge;
            rect.left = i + i2;
            rect.right -= i2;
            int dimensionPixelSize = getResources().getDimensionPixelSize(R$dimen.op_cust_statusbar_cutout_show_region_left);
            int dimensionPixelSize2 = getResources().getDimensionPixelSize(R$dimen.op_cust_statusbar_cutout_show_region_right);
            if (!(dimensionPixelSize == 0 && dimensionPixelSize2 == 0) && !OpUtils.isCutoutEmulationEnabled()) {
                rect.left = dimensionPixelSize;
                rect.right = dimensionPixelSize2;
                if (OpUtils.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("left:");
                    sb.append(rect.left);
                    sb.append(", right:");
                    sb.append(rect.right);
                    sb.append(", width():");
                    sb.append(rect.width());
                    sb.append(", height():");
                    sb.append(rect.height());
                    Log.i("PhoneStatusBarView", sb.toString());
                }
            }
            layoutParams.width = rect.width();
            layoutParams.height = rect.height();
        }
    }

    private void updateSafeInsets(Pair<Integer, Integer> pair) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        DisplayCutout displayCutout = this.mDisplayCutout;
        if (displayCutout == null || displayCutout.isEmpty() || this.mLastOrientation != 1 || pair == null) {
            layoutParams.leftMargin = 0;
            layoutParams.rightMargin = 0;
            return;
        }
        layoutParams.leftMargin = Math.max(layoutParams.leftMargin, ((Integer) pair.first).intValue());
        layoutParams.rightMargin = Math.max(layoutParams.rightMargin, ((Integer) pair.second).intValue());
        WindowInsets rootWindowInsets = getRootWindowInsets();
        int systemWindowInsetLeft = rootWindowInsets.getSystemWindowInsetLeft();
        int systemWindowInsetRight = rootWindowInsets.getSystemWindowInsetRight();
        if (layoutParams.leftMargin <= systemWindowInsetLeft) {
            layoutParams.leftMargin = 0;
        }
        if (layoutParams.rightMargin <= systemWindowInsetRight) {
            layoutParams.rightMargin = 0;
        }
    }

    public static Pair<Integer, Integer> cornerCutoutMargins(Context context, DisplayCutout displayCutout, Display display) {
        if (displayCutout == null) {
            return null;
        }
        Point point = new Point();
        display.getRealSize(point);
        Rect rect = new Rect();
        DisplayCutoutView.boundsFromDirection(context, displayCutout, 48, rect);
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("cornerCutoutMargins left:");
            sb.append(rect.left);
            sb.append(", right:");
            sb.append(rect.right);
            sb.append(", width:");
            sb.append(rect.width());
            sb.append(", height:");
            sb.append(rect.height());
            Log.i("PhoneStatusBarView", sb.toString());
        }
        if (rect.left <= 0) {
            return new Pair<>(Integer.valueOf(rect.right), Integer.valueOf(0));
        }
        if (rect.right >= point.x) {
            return new Pair<>(Integer.valueOf(0), Integer.valueOf(point.x - rect.left));
        }
        return null;
    }
}

package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import com.android.internal.annotations.VisibleForTesting;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.RemoteInputController.Callback;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.google.android.collect.Lists;
import com.oneplus.plugin.OpLsState;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class StatusBarWindowController implements Callback, Dumpable, ConfigurationListener {
    private final IActivityManager mActivityManager;
    private int mBarHeight;
    private final ArrayList<WeakReference<StatusBarWindowCallback>> mCallbacks;
    private final SysuiColorExtractor mColorExtractor;
    private final Context mContext;
    private final State mCurrentState;
    private final DozeParameters mDozeParameters;
    private boolean mHasTopUi;
    private boolean mHasTopUiChanged;
    private final boolean mKeyguardScreenRotation;
    private OtherwisedCollapsedListener mListener;
    private LayoutParams mLp;
    private final LayoutParams mLpChanged;
    private float mScreenBrightnessDoze;
    private final StateListener mStateListener;
    private ViewGroup mStatusBarView;
    private final WindowManager mWindowManager;

    public interface OtherwisedCollapsedListener {
        void setWouldOtherwiseCollapse(boolean z);
    }

    private static class State {
        boolean backdropShowing;
        boolean bouncerShowing;
        boolean bubbleExpanded;
        boolean bubblesShowing;
        boolean dozing;
        boolean forceCollapsed;
        boolean forceDozeBrightness;
        boolean forcePluginOpen;
        boolean forceStatusBarVisible;
        boolean forceUserActivity;
        boolean hasWallpaper;
        boolean headsUpShowing;
        boolean keyguardFadingAway;
        boolean keyguardNeedsInput;
        boolean keyguardOccluded;
        boolean keyguardShowing;
        boolean notTouchable;
        boolean panelExpanded;
        boolean panelVisible;
        boolean preventModeActive;
        boolean qsExpanded;
        boolean remoteInputActive;
        int scrimsVisibility;
        boolean statusBarFocusable;
        int statusBarState;
        boolean wallpaperSupportsAmbientMode;

        private State() {
            this.preventModeActive = false;
        }

        /* access modifiers changed from: private */
        public boolean isKeyguardShowingAndNotOccluded() {
            return this.keyguardShowing && !this.keyguardOccluded;
        }

        public String toString() {
            Field[] declaredFields;
            StringBuilder sb = new StringBuilder();
            sb.append("Window State {");
            String str = "\n";
            sb.append(str);
            for (Field field : State.class.getDeclaredFields()) {
                sb.append("  ");
                try {
                    sb.append(field.getName());
                    sb.append(": ");
                    sb.append(field.get(this));
                } catch (IllegalAccessException unused) {
                }
                sb.append(str);
            }
            sb.append("}");
            return sb.toString();
        }
    }

    private boolean shouldEnableKeyguardScreenRotation() {
        return false;
    }

    public StatusBarWindowController(Context context) {
        this(context, (WindowManager) context.getSystemService(WindowManager.class), ActivityManager.getService(), DozeParameters.getInstance(context));
    }

    @VisibleForTesting
    public StatusBarWindowController(Context context, WindowManager windowManager, IActivityManager iActivityManager, DozeParameters dozeParameters) {
        this.mCurrentState = new State();
        this.mCallbacks = Lists.newArrayList();
        this.mColorExtractor = (SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class);
        this.mStateListener = new StateListener() {
            public void onStateChanged(int i) {
                StatusBarWindowController.this.setStatusBarState(i);
            }

            public void onDozingChanged(boolean z) {
                StatusBarWindowController.this.setDozing(z);
            }
        };
        this.mContext = context;
        this.mWindowManager = windowManager;
        this.mActivityManager = iActivityManager;
        this.mKeyguardScreenRotation = shouldEnableKeyguardScreenRotation();
        this.mDozeParameters = dozeParameters;
        this.mScreenBrightnessDoze = this.mDozeParameters.getScreenBrightnessDoze();
        this.mLpChanged = new LayoutParams();
        ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this.mStateListener, 1);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    public void registerCallback(StatusBarWindowCallback statusBarWindowCallback) {
        int i = 0;
        while (i < this.mCallbacks.size()) {
            if (((WeakReference) this.mCallbacks.get(i)).get() != statusBarWindowCallback) {
                i++;
            } else {
                return;
            }
        }
        this.mCallbacks.add(new WeakReference(statusBarWindowCallback));
    }

    public void add(ViewGroup viewGroup, int i) {
        LayoutParams layoutParams = new LayoutParams(-1, i, 2000, -2138832824, -3);
        this.mLp = layoutParams;
        this.mLp.token = new Binder();
        LayoutParams layoutParams2 = this.mLp;
        layoutParams2.gravity = 48;
        layoutParams2.softInputMode = 16;
        layoutParams2.setTitle("StatusBar");
        this.mLp.packageName = this.mContext.getPackageName();
        LayoutParams layoutParams3 = this.mLp;
        layoutParams3.layoutInDisplayCutoutMode = 1;
        this.mStatusBarView = viewGroup;
        this.mBarHeight = i;
        this.mWindowManager.addView(this.mStatusBarView, layoutParams3);
        this.mLpChanged.copyFrom(this.mLp);
        onThemeChanged();
    }

    public ViewGroup getStatusBarView() {
        return this.mStatusBarView;
    }

    public void setDozeScreenBrightness(int i) {
        this.mScreenBrightnessDoze = ((float) i) / 255.0f;
    }

    private void setKeyguardDark(boolean z) {
        int systemUiVisibility = this.mStatusBarView.getSystemUiVisibility();
        this.mStatusBarView.setSystemUiVisibility(z ? systemUiVisibility | 16 | 8192 : systemUiVisibility & -17 & -8193);
    }

    private void applyKeyguardFlags(State state) {
        if (state.keyguardShowing) {
            this.mLpChanged.privateFlags |= 1024;
        } else {
            this.mLpChanged.privateFlags &= -1025;
        }
        if (!state.keyguardShowing || !state.bouncerShowing || state.hasWallpaper) {
            this.mLpChanged.privateFlags &= -1073741825;
        } else {
            this.mLpChanged.privateFlags |= 1073741824;
        }
        boolean z = true;
        boolean z2 = state.scrimsVisibility == 2;
        if (!state.keyguardShowing && (!state.dozing || !this.mDozeParameters.getAlwaysOn())) {
            z = false;
        }
        if (!z || z2 || (state.backdropShowing && !OpUtils.isHomeApp())) {
            this.mLpChanged.flags &= -1048577;
        } else {
            this.mLpChanged.flags |= 1048576;
        }
        if (state.dozing) {
            this.mLpChanged.privateFlags |= 524288;
            return;
        }
        this.mLpChanged.privateFlags &= -524289;
    }

    private void adjustScreenOrientation(State state) {
        if (state.bouncerShowing || state.preventModeActive) {
            if (OpUtils.DEBUG_ONEPLUS) {
                Log.d("StatusBarWindowController", "force adjust screenOrientation to portrait");
            }
            this.mLpChanged.screenOrientation = 1;
            return;
        }
        if (!state.isKeyguardShowingAndNotOccluded() && !state.dozing) {
            this.mLpChanged.screenOrientation = -1;
        } else if (this.mKeyguardScreenRotation) {
            this.mLpChanged.screenOrientation = 2;
        } else {
            this.mLpChanged.screenOrientation = 5;
        }
    }

    private void applyFocusableFlag(State state) {
        boolean z = state.statusBarFocusable && state.panelExpanded;
        if ((state.bouncerShowing && (state.keyguardOccluded || state.keyguardNeedsInput)) || ((NotificationRemoteInputManager.ENABLE_REMOTE_INPUT && state.remoteInputActive) || state.bubbleExpanded)) {
            LayoutParams layoutParams = this.mLpChanged;
            layoutParams.flags &= -9;
            layoutParams.flags &= -131073;
        } else if (state.isKeyguardShowingAndNotOccluded() || z) {
            LayoutParams layoutParams2 = this.mLpChanged;
            layoutParams2.flags &= -9;
            layoutParams2.flags |= 131072;
        } else {
            BiometricUnlockController biometricUnlockController = OpLsState.getInstance().getBiometricUnlockController();
            if (biometricUnlockController == null || !biometricUnlockController.isPlayingScrimAnimation()) {
                LayoutParams layoutParams3 = this.mLpChanged;
                layoutParams3.flags |= 8;
                layoutParams3.flags &= -131073;
            } else {
                LayoutParams layoutParams4 = this.mLpChanged;
                layoutParams4.flags |= 8;
                layoutParams4.flags &= -131073;
                layoutParams4.softInputMode = 16;
                return;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("applyFocusableFlag: ");
        sb.append(state.statusBarFocusable);
        String str = ", ";
        sb.append(str);
        sb.append(state.panelExpanded);
        sb.append(str);
        sb.append(state.isKeyguardShowingAndNotOccluded());
        sb.append(str);
        sb.append(Integer.toHexString(this.mLpChanged.flags));
        Log.d("StatusBarWindowController", sb.toString());
        this.mLpChanged.softInputMode = 16;
    }

    private void applyForceShowNavigationFlag(State state) {
        if (state.panelExpanded || state.bouncerShowing || (NotificationRemoteInputManager.ENABLE_REMOTE_INPUT && state.remoteInputActive)) {
            this.mLpChanged.privateFlags |= 8388608;
            return;
        }
        this.mLpChanged.privateFlags &= -8388609;
    }

    private void applyHeight(State state) {
        boolean isExpanded = isExpanded(state);
        if (state.forcePluginOpen) {
            OtherwisedCollapsedListener otherwisedCollapsedListener = this.mListener;
            if (otherwisedCollapsedListener != null) {
                otherwisedCollapsedListener.setWouldOtherwiseCollapse(isExpanded);
            }
            isExpanded = true;
        }
        if (isExpanded) {
            this.mLpChanged.height = -1;
        } else {
            this.mLpChanged.height = this.mBarHeight;
        }
        if (OpLsState.getInstance().getPhoneStatusBar() != null) {
            OpLsState.getInstance().getPhoneStatusBar().notifyBarHeightChange(this.mLpChanged.height);
        }
    }

    private boolean isExpanded(State state) {
        return !state.forceCollapsed && (state.isKeyguardShowingAndNotOccluded() || state.panelVisible || state.keyguardFadingAway || state.bouncerShowing || state.headsUpShowing || state.bubblesShowing || state.scrimsVisibility != 0);
    }

    private void applyFitsSystemWindows(State state) {
        boolean z = !state.isKeyguardShowingAndNotOccluded();
        ViewGroup viewGroup = this.mStatusBarView;
        if (viewGroup != null && viewGroup.getFitsSystemWindows() != z) {
            this.mStatusBarView.setFitsSystemWindows(z);
            this.mStatusBarView.requestApplyInsets();
        }
    }

    private void applyUserActivityTimeout(State state) {
        if (!state.isKeyguardShowingAndNotOccluded() || state.statusBarState != 1 || state.qsExpanded) {
            this.mLpChanged.userActivityTimeout = -1;
        } else {
            this.mLpChanged.userActivityTimeout = 10000;
        }
    }

    private void applyInputFeatures(State state) {
        if (!state.isKeyguardShowingAndNotOccluded() || state.statusBarState != 1 || state.qsExpanded || state.forceUserActivity) {
            this.mLpChanged.inputFeatures &= -5;
            return;
        }
        this.mLpChanged.inputFeatures |= 4;
    }

    private void applyStatusBarColorSpaceAgnosticFlag(State state) {
        if (!isExpanded(state)) {
            this.mLpChanged.privateFlags |= 16777216;
            return;
        }
        this.mLpChanged.privateFlags &= -16777217;
    }

    private void apply(State state) {
        applyKeyguardFlags(state);
        applyForceStatusBarVisibleFlag(state);
        applyFocusableFlag(state);
        applyForceShowNavigationFlag(state);
        adjustScreenOrientation(state);
        applyHeight(state);
        applyUserActivityTimeout(state);
        applyInputFeatures(state);
        applyFitsSystemWindows(state);
        applyModalFlag(state);
        applyBrightness(state);
        applyHasTopUi(state);
        applySleepToken(state);
        applyNotTouchable(state);
        applyStatusBarColorSpaceAgnosticFlag(state);
        LayoutParams layoutParams = this.mLp;
        if (!(layoutParams == null || layoutParams.copyFrom(this.mLpChanged) == 0)) {
            this.mWindowManager.updateViewLayout(this.mStatusBarView, this.mLp);
        }
        boolean z = this.mHasTopUi;
        boolean z2 = this.mHasTopUiChanged;
        if (z != z2) {
            try {
                this.mActivityManager.setHasTopUi(z2);
            } catch (RemoteException e) {
                Log.e("StatusBarWindowController", "Failed to call setHasTopUi", e);
            }
            this.mHasTopUi = this.mHasTopUiChanged;
        }
        notifyStateChangedCallbacks();
    }

    public void notifyStateChangedCallbacks() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            StatusBarWindowCallback statusBarWindowCallback = (StatusBarWindowCallback) ((WeakReference) this.mCallbacks.get(i)).get();
            if (statusBarWindowCallback != null) {
                State state = this.mCurrentState;
                statusBarWindowCallback.onStateChanged(state.keyguardShowing, state.keyguardOccluded, state.bouncerShowing);
            }
        }
    }

    private void applyForceStatusBarVisibleFlag(State state) {
        boolean isExpanded = isExpanded(state);
        boolean z = true;
        if (state.forcePluginOpen) {
            this.mListener.setWouldOtherwiseCollapse(isExpanded);
            isExpanded = true;
        }
        if (!isExpanded || !KeyguardUpdateMonitor.getInstance(this.mContext).isCameraLaunched()) {
            z = false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("applyExpandedFlag expanded:");
        sb.append(isExpanded);
        sb.append(" isCameraLaunched:");
        sb.append(KeyguardUpdateMonitor.getInstance(this.mContext).isCameraLaunched());
        Log.i("StatusBarWindowController", sb.toString());
        if (state.forceStatusBarVisible || z) {
            this.mLpChanged.privateFlags |= 4096;
            return;
        }
        this.mLpChanged.privateFlags &= -4097;
    }

    private void applyModalFlag(State state) {
        if (state.headsUpShowing) {
            this.mLpChanged.flags |= 32;
            return;
        }
        this.mLpChanged.flags &= -33;
    }

    private void applyBrightness(State state) {
        if (state.forceDozeBrightness) {
            this.mLpChanged.screenBrightness = this.mScreenBrightnessDoze;
            return;
        }
        this.mLpChanged.screenBrightness = -1.0f;
    }

    private void applyHasTopUi(State state) {
        this.mHasTopUiChanged = isExpanded(state);
    }

    private void applyNotTouchable(State state) {
        if (state.notTouchable) {
            this.mLpChanged.flags |= 16;
        } else {
            this.mLpChanged.flags &= -17;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("setNotTouchable: ");
        sb.append(state.notTouchable);
        Log.d("StatusBarWindowController", sb.toString());
    }

    public void setKeyguardShowing(boolean z) {
        State state = this.mCurrentState;
        state.keyguardShowing = z;
        apply(state);
    }

    public void setKeyguardOccluded(boolean z) {
        State state = this.mCurrentState;
        state.keyguardOccluded = z;
        apply(state);
    }

    public void setKeyguardNeedsInput(boolean z) {
        State state = this.mCurrentState;
        state.keyguardNeedsInput = z;
        apply(state);
    }

    public void setPanelVisible(boolean z) {
        State state = this.mCurrentState;
        state.panelVisible = z;
        state.statusBarFocusable = z;
        apply(state);
    }

    public void setStatusBarFocusable(boolean z) {
        State state = this.mCurrentState;
        state.statusBarFocusable = z;
        apply(state);
    }

    public void setBouncerShowing(boolean z) {
        State state = this.mCurrentState;
        state.bouncerShowing = z;
        apply(state);
    }

    public void setBackdropShowing(boolean z) {
        State state = this.mCurrentState;
        state.backdropShowing = z;
        apply(state);
    }

    public void setKeyguardFadingAway(boolean z) {
        State state = this.mCurrentState;
        state.keyguardFadingAway = z;
        apply(state);
    }

    public void setQsExpanded(boolean z) {
        State state = this.mCurrentState;
        state.qsExpanded = z;
        apply(state);
    }

    public void setScrimsVisibility(int i) {
        State state = this.mCurrentState;
        state.scrimsVisibility = i;
        apply(state);
    }

    public void setHeadsUpShowing(boolean z) {
        State state = this.mCurrentState;
        state.headsUpShowing = z;
        apply(state);
    }

    public void setWallpaperSupportsAmbientMode(boolean z) {
        State state = this.mCurrentState;
        state.wallpaperSupportsAmbientMode = z;
        apply(state);
    }

    /* access modifiers changed from: private */
    public void setStatusBarState(int i) {
        State state = this.mCurrentState;
        state.statusBarState = i;
        apply(state);
    }

    public void setForceStatusBarVisible(boolean z) {
        State state = this.mCurrentState;
        state.forceStatusBarVisible = z;
        apply(state);
    }

    public void setForceWindowCollapsed(boolean z) {
        State state = this.mCurrentState;
        state.forceCollapsed = z;
        apply(state);
    }

    public void setPanelExpanded(boolean z) {
        State state = this.mCurrentState;
        state.panelExpanded = z;
        apply(state);
    }

    public void onRemoteInputActive(boolean z) {
        State state = this.mCurrentState;
        state.remoteInputActive = z;
        apply(state);
    }

    public void setForceDozeBrightness(boolean z) {
        State state = this.mCurrentState;
        state.forceDozeBrightness = z;
        apply(state);
    }

    public void setDozing(boolean z) {
        State state = this.mCurrentState;
        state.dozing = z;
        apply(state);
    }

    public void setBarHeight(int i) {
        this.mBarHeight = i;
        apply(this.mCurrentState);
    }

    public void setForcePluginOpen(boolean z) {
        State state = this.mCurrentState;
        state.forcePluginOpen = z;
        apply(state);
    }

    public void setNotTouchable(boolean z) {
        State state = this.mCurrentState;
        state.notTouchable = z;
        apply(state);
    }

    public void setBubblesShowing(boolean z) {
        State state = this.mCurrentState;
        state.bubblesShowing = z;
        apply(state);
    }

    public boolean getBubblesShowing() {
        return this.mCurrentState.bubblesShowing;
    }

    public void setBubbleExpanded(boolean z) {
        State state = this.mCurrentState;
        state.bubbleExpanded = z;
        apply(state);
    }

    public boolean getPanelExpanded() {
        return this.mCurrentState.panelExpanded;
    }

    public void setStateListener(OtherwisedCollapsedListener otherwisedCollapsedListener) {
        this.mListener = otherwisedCollapsedListener;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("StatusBarWindowController state:");
        printWriter.println(this.mCurrentState);
    }

    public boolean isShowingWallpaper() {
        return !this.mCurrentState.backdropShowing;
    }

    public void onThemeChanged() {
        if (this.mStatusBarView != null) {
            setKeyguardDark(this.mColorExtractor.getNeutralColors().supportsDarkText());
        }
    }

    public void setLockscreenWallpaper(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("LockscreenWP: ");
        sb.append(z);
        Log.d("StatusBarWindowManager", sb.toString());
        State state = this.mCurrentState;
        state.hasWallpaper = z;
        apply(state);
    }

    public boolean isShowingLiveWallpaper(boolean z) {
        return (!OpUtils.isCustomFingerprint() || !z) && !this.mCurrentState.hasWallpaper;
    }

    public void setPreventModeActive(boolean z) {
        State state = this.mCurrentState;
        state.preventModeActive = z;
        apply(state);
    }

    public boolean isKeyguardFadingAway() {
        return this.mCurrentState.keyguardFadingAway;
    }

    private void applySleepToken(State state) {
        if (state.dozing) {
            this.mLpChanged.privateFlags |= 2097152;
            return;
        }
        this.mLpChanged.privateFlags &= -2097153;
    }

    public void debugBarHeight() {
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("debugBarHeight forceCollapsed ");
            sb.append(this.mCurrentState.forceCollapsed);
            sb.append(" isKeyguardShowingAndNotOccluded ");
            sb.append(this.mCurrentState.isKeyguardShowingAndNotOccluded());
            sb.append(" panelVisible ");
            sb.append(this.mCurrentState.panelVisible);
            sb.append(" keyguardFadingAway ");
            sb.append(this.mCurrentState.keyguardFadingAway);
            sb.append(" bouncerShowing ");
            sb.append(this.mCurrentState.bouncerShowing);
            sb.append(" headsUpShowing ");
            sb.append(this.mCurrentState.headsUpShowing);
            sb.append(" scrimsVisibility ");
            sb.append(this.mCurrentState.scrimsVisibility);
            sb.append(" forcePluginOpen ");
            sb.append(this.mCurrentState.forcePluginOpen);
            Log.d("StatusBarWindowController", sb.toString());
        }
    }
}

package com.android.systemui.statusbar.phone;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.InternalInsetsInfo;
import android.view.ViewTreeObserver.OnComputeInternalInsetsListener;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.graphics.ColorUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.DockedStackExistsListener;
import com.android.systemui.Interpolators;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$string;
import com.android.systemui.R$style;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsOnboarding;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.shared.system.WindowManagerWrapper;
import com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.oneplus.scene.OpSceneModeObserver;
import com.oneplus.systemui.statusbar.phone.OpEdgeNavGestureHandler;
import com.oneplus.util.OpNavBarUtils;
import com.oneplus.util.OpUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Consumer;

public class NavigationBarView extends FrameLayout implements ModeChangedListener {
    static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    private final int COLOR_BACKGROUND_DARK;
    private final int COLOR_BACKGROUND_LIGHT;
    private final int COLOR_BACKGROUND_TRANSPARENT;
    private final int COLOR_KEY_CTS_60;
    private final int COLOR_KEY_DARK;
    private final int COLOR_KEY_LIGHT;
    private final int COLOR_KEY_TRANSPARENT;
    private final Region mActiveRegion;
    private Rect mBackButtonBounds;
    private KeyButtonDrawable mBackIcon;
    private int mBackgroundColor;
    private final NavigationBarTransitions mBarTransitions;
    private final SparseArray<ButtonDispatcher> mButtonDispatchers;
    private Configuration mConfiguration;
    private final ContextualButtonGroup mContextualButtonGroup;
    private int mCurrentRotation;
    View mCurrentView;
    private final DeadZone mDeadZone;
    private boolean mDeadZoneConsuming;
    int mDisabledFlags;
    private KeyButtonDrawable mDockedIcon;
    private final Consumer<Boolean> mDockedListener;
    private boolean mDockedStackExists;
    private final EdgeBackGestureHandler mEdgeBackGestureHandler;
    private FloatingRotationButton mFloatingRotationButton;
    private HeadsUpManager mHeadsUpManager;
    private boolean mHideNavBar;
    private Rect mHomeButtonBounds;
    private KeyButtonDrawable mHomeDefaultIcon;
    private View mHorizontal;
    private boolean mImeShow;
    private final OnClickListener mImeSwitcherClickListener;
    private boolean mImeVisible;
    private boolean mInCarMode;
    private boolean mIsCTSOn;
    public boolean mIsCustomNavBar;
    private boolean mIsHideNavBarOn;
    private boolean mIsImmersiveSticky;
    private boolean mIsInBrickMode;
    private boolean mIsLightBar;
    private boolean mIsPanelViewFullExpanded;
    private boolean mIsVertical;
    private boolean mKeyguardShow;
    private int mLastButtonColor;
    private int mLastRippleColor;
    private boolean mLayoutTransitionsEnabled;
    boolean mLongClickableAccessibilityButton;
    private int mNavBarMode;
    int mNavigationIconHints;
    private NavigationBarInflaterView mNavigationInflaterView;
    private final OnComputeInternalInsetsListener mOnComputeInternalInsetsListener;
    private OnVerticalChangedListener mOnVerticalChangedListener;
    private final OpEdgeNavGestureHandler mOpEdgeNavGestureHandler;
    private final OverviewProxyService mOverviewProxyService;
    private NotificationPanelView mPanelView;
    private final AccessibilityDelegate mQuickStepAccessibilityDelegate;
    private KeyButtonDrawable mRecentIcon;
    private Rect mRecentsButtonBounds;
    private RecentsOnboarding mRecentsOnboarding;
    private Rect mRotationButtonBounds;
    private RotationButtonController mRotationButtonController;
    private ScreenPinningNotify mScreenPinningNotify;
    private boolean mShowNavKey;
    private NavBarTintController mTintController;
    private Configuration mTmpLastConfiguration;
    private int[] mTmpPosition;
    private final NavTransitionListener mTransitionListener;
    private boolean mUseCarModeUi;
    private View mVertical;
    private boolean mWakeAndUnlocking;

    private class NavTransitionListener implements TransitionListener {
        private boolean mBackTransitioning;
        private long mDuration;
        private boolean mHomeAppearing;
        private TimeInterpolator mInterpolator;
        private long mStartDelay;

        private NavTransitionListener() {
        }

        public void startTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
            if (view.getId() == R$id.back) {
                this.mBackTransitioning = true;
            } else if (view.getId() == R$id.home && i == 2) {
                this.mHomeAppearing = true;
                this.mStartDelay = layoutTransition.getStartDelay(i);
                this.mDuration = layoutTransition.getDuration(i);
                this.mInterpolator = layoutTransition.getInterpolator(i);
            }
        }

        public void endTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
            if (view.getId() == R$id.back) {
                this.mBackTransitioning = false;
            } else if (view.getId() == R$id.home && i == 2) {
                this.mHomeAppearing = false;
            }
        }

        public void onBackAltCleared() {
            ButtonDispatcher backButton = NavigationBarView.this.getBackButton();
            if (!this.mBackTransitioning && backButton.getVisibility() == 0 && this.mHomeAppearing && NavigationBarView.this.getHomeButton().getAlpha() == 0.0f) {
                NavigationBarView.this.getBackButton().setAlpha(0.0f);
                ObjectAnimator ofFloat = ObjectAnimator.ofFloat(backButton, "alpha", new float[]{0.0f, 1.0f});
                ofFloat.setStartDelay(this.mStartDelay);
                ofFloat.setDuration(this.mDuration);
                ofFloat.setInterpolator(this.mInterpolator);
                ofFloat.start();
            }
        }
    }

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean z);
    }

    private static String visibilityToString(int i) {
        return i != 4 ? i != 8 ? "VISIBLE" : "GONE" : "INVISIBLE";
    }

    public /* synthetic */ void lambda$new$0$NavigationBarView(InternalInsetsInfo internalInsetsInfo) {
        if (!QuickStepContract.isGesturalMode(this.mNavBarMode) || this.mImeVisible) {
            internalInsetsInfo.setTouchableInsets(0);
            return;
        }
        internalInsetsInfo.setTouchableInsets(3);
        ButtonDispatcher imeSwitchButton = getImeSwitchButton();
        if (imeSwitchButton.getVisibility() == 0) {
            int[] iArr = new int[2];
            View currentView = imeSwitchButton.getCurrentView();
            currentView.getLocationInWindow(iArr);
            internalInsetsInfo.touchableRegion.set(iArr[0], iArr[1], iArr[0] + currentView.getWidth(), iArr[1] + currentView.getHeight());
            return;
        }
        internalInsetsInfo.touchableRegion.setEmpty();
    }

    public NavigationBarView(Context context, AttributeSet attributeSet) {
        int i;
        super(context, attributeSet);
        this.mCurrentView = null;
        this.mCurrentRotation = -1;
        this.mDisabledFlags = 0;
        this.mNavigationIconHints = 0;
        this.mNavBarMode = 0;
        this.mHomeButtonBounds = new Rect();
        this.mBackButtonBounds = new Rect();
        this.mRecentsButtonBounds = new Rect();
        this.mRotationButtonBounds = new Rect();
        this.mActiveRegion = new Region();
        this.mTmpPosition = new int[2];
        this.mDeadZoneConsuming = false;
        this.mTransitionListener = new NavTransitionListener();
        this.mLayoutTransitionsEnabled = true;
        this.mUseCarModeUi = false;
        this.mInCarMode = false;
        this.mButtonDispatchers = new SparseArray<>();
        this.COLOR_KEY_CTS_60 = -1728053248;
        this.mIsCTSOn = false;
        this.mLastButtonColor = 0;
        this.mLastRippleColor = 0;
        this.mIsHideNavBarOn = false;
        this.mBackgroundColor = 0;
        this.mShowNavKey = false;
        this.mKeyguardShow = false;
        this.mImeShow = false;
        this.mIsImmersiveSticky = false;
        this.mIsLightBar = false;
        this.mIsCustomNavBar = OpNavBarUtils.isSupportCustomNavBar();
        this.mIsInBrickMode = false;
        this.mHideNavBar = false;
        this.mIsPanelViewFullExpanded = false;
        this.mImeSwitcherClickListener = new OnClickListener() {
            public void onClick(View view) {
                ((InputMethodManager) NavigationBarView.this.mContext.getSystemService(InputMethodManager.class)).showInputMethodPickerFromSystem(true, NavigationBarView.this.getContext().getDisplayId());
            }
        };
        this.mQuickStepAccessibilityDelegate = new AccessibilityDelegate() {
            private AccessibilityAction mToggleOverviewAction;

            public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
                if (this.mToggleOverviewAction == null) {
                    this.mToggleOverviewAction = new AccessibilityAction(R$id.action_toggle_overview, NavigationBarView.this.getContext().getString(R$string.quick_step_accessibility_toggle_overview));
                }
                accessibilityNodeInfo.addAction(this.mToggleOverviewAction);
            }

            public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
                if (i != R$id.action_toggle_overview) {
                    return super.performAccessibilityAction(view, i, bundle);
                }
                ((Recents) SysUiServiceProvider.getComponent(NavigationBarView.this.getContext(), Recents.class)).toggleRecentApps();
                return true;
            }
        };
        this.mOnComputeInternalInsetsListener = new OnComputeInternalInsetsListener() {
            public final void onComputeInternalInsets(InternalInsetsInfo internalInsetsInfo) {
                NavigationBarView.this.lambda$new$0$NavigationBarView(internalInsetsInfo);
            }
        };
        this.mDockedListener = new Consumer() {
            public final void accept(Object obj) {
                NavigationBarView.this.lambda$new$2$NavigationBarView((Boolean) obj);
            }
        };
        this.mIsVertical = false;
        this.mLongClickableAccessibilityButton = false;
        this.mNavBarMode = ((NavigationModeController) Dependency.get(NavigationModeController.class)).addListener(this);
        boolean isGesturalMode = QuickStepContract.isGesturalMode(this.mNavBarMode);
        this.mContextualButtonGroup = new ContextualButtonGroup(R$id.menu_container);
        ContextualButton contextualButton = new ContextualButton(R$id.ime_switcher, R$drawable.ic_ime_switcher_default);
        int i2 = R$id.rotate_suggestion;
        if (this.mIsCustomNavBar) {
            i = R$drawable.ic_sysbar_rotate_button2;
        } else {
            i = R$drawable.ic_sysbar_rotate_button;
        }
        RotationContextButton rotationContextButton = new RotationContextButton(i2, i);
        ContextualButton contextualButton2 = new ContextualButton(R$id.accessibility_button, R$drawable.ic_sysbar_accessibility_button);
        this.mContextualButtonGroup.addButton(contextualButton);
        if (!isGesturalMode) {
            this.mContextualButtonGroup.addButton(rotationContextButton);
        }
        this.mContextualButtonGroup.addButton(contextualButton2);
        this.mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        this.mRecentsOnboarding = new RecentsOnboarding(context, this.mOverviewProxyService);
        this.mFloatingRotationButton = new FloatingRotationButton(context);
        this.mRotationButtonController = new RotationButtonController(context, R$style.RotateButtonCCWStart90, isGesturalMode ? this.mFloatingRotationButton : rotationContextButton);
        ContextualButton contextualButton3 = new ContextualButton(R$id.back, 0);
        this.mConfiguration = new Configuration();
        this.mTmpLastConfiguration = new Configuration();
        this.mConfiguration.updateFrom(context.getResources().getConfiguration());
        this.mScreenPinningNotify = new ScreenPinningNotify(this.mContext);
        this.mBarTransitions = new NavigationBarTransitions(this);
        this.mButtonDispatchers.put(R$id.back, contextualButton3);
        SparseArray<ButtonDispatcher> sparseArray = this.mButtonDispatchers;
        int i3 = R$id.home;
        sparseArray.put(i3, new ButtonDispatcher(i3));
        SparseArray<ButtonDispatcher> sparseArray2 = this.mButtonDispatchers;
        int i4 = R$id.home_handle;
        sparseArray2.put(i4, new ButtonDispatcher(i4));
        SparseArray<ButtonDispatcher> sparseArray3 = this.mButtonDispatchers;
        int i5 = R$id.recent_apps;
        sparseArray3.put(i5, new ButtonDispatcher(i5));
        this.mButtonDispatchers.put(R$id.ime_switcher, contextualButton);
        this.mButtonDispatchers.put(R$id.accessibility_button, contextualButton2);
        this.mButtonDispatchers.put(R$id.rotate_suggestion, rotationContextButton);
        this.mButtonDispatchers.put(R$id.menu_container, this.mContextualButtonGroup);
        this.mDeadZone = new DeadZone(this);
        this.mEdgeBackGestureHandler = new EdgeBackGestureHandler(context, this.mOverviewProxyService);
        this.mOpEdgeNavGestureHandler = new OpEdgeNavGestureHandler(context, this.mOverviewProxyService);
        this.mTintController = new NavBarTintController(this, getLightTransitionsController());
        if (OpNavBarUtils.isSupportHideNavBar()) {
            SparseArray<ButtonDispatcher> sparseArray4 = this.mButtonDispatchers;
            int i6 = R$id.nav;
            sparseArray4.put(i6, new ButtonDispatcher(i6));
        }
        this.COLOR_BACKGROUND_LIGHT = context.getColor(R$color.op_nav_bar_background_light);
        this.COLOR_BACKGROUND_DARK = context.getColor(R$color.op_nav_bar_background_dark);
        this.COLOR_BACKGROUND_TRANSPARENT = context.getColor(R$color.op_nav_bar_background_transparent);
        this.COLOR_KEY_LIGHT = context.getColor(R$color.op_nav_bar_key_light);
        this.COLOR_KEY_DARK = context.getColor(R$color.op_nav_bar_key_dark);
        this.COLOR_KEY_TRANSPARENT = context.getColor(R$color.op_nav_bar_key_transparent);
        this.mIsInBrickMode = ((OpSceneModeObserver) Dependency.get(OpSceneModeObserver.class)).isInBrickMode();
    }

    public NavBarTintController getTintController() {
        return this.mTintController;
    }

    public NavigationBarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public LightBarTransitionsController getLightTransitionsController() {
        return this.mBarTransitions.getLightTransitionsController();
    }

    public void setComponents(NotificationPanelView notificationPanelView, AssistManager assistManager) {
        this.mPanelView = notificationPanelView;
        updateSystemUiStateFlags();
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        this.mTintController.onDraw();
    }

    public void setOnVerticalChangedListener(OnVerticalChangedListener onVerticalChangedListener) {
        this.mOnVerticalChangedListener = onVerticalChangedListener;
        notifyVerticalChangedListener(this.mIsVertical);
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return shouldDeadZoneConsumeTouchEvents(motionEvent) || super.onInterceptTouchEvent(motionEvent);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        shouldDeadZoneConsumeTouchEvents(motionEvent);
        return super.onTouchEvent(motionEvent);
    }

    /* access modifiers changed from: 0000 */
    public void onBarTransition(int i) {
        if (i == 0) {
            this.mTintController.stop();
            getLightTransitionsController().setIconsDark(false, true);
            return;
        }
        this.mTintController.start();
    }

    private boolean shouldDeadZoneConsumeTouchEvents(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            this.mDeadZoneConsuming = false;
        }
        if (!this.mDeadZone.onTouchEvent(motionEvent) && !this.mDeadZoneConsuming) {
            return false;
        }
        if (actionMasked == 0) {
            setSlippery(true);
            this.mDeadZoneConsuming = true;
        } else if (actionMasked == 1 || actionMasked == 3) {
            updateSlippery();
            this.mDeadZoneConsuming = false;
        }
        return true;
    }

    public void abortCurrentGesture() {
        getHomeButton().abortCurrentGesture();
    }

    public View getCurrentView() {
        return this.mCurrentView;
    }

    public RotationButtonController getRotationButtonController() {
        return this.mRotationButtonController;
    }

    public ButtonDispatcher getNavButton() {
        return (ButtonDispatcher) this.mButtonDispatchers.get(R$id.nav);
    }

    public ButtonDispatcher getRecentsButton() {
        return (ButtonDispatcher) this.mButtonDispatchers.get(R$id.recent_apps);
    }

    public ButtonDispatcher getBackButton() {
        return (ButtonDispatcher) this.mButtonDispatchers.get(R$id.back);
    }

    public ButtonDispatcher getHomeButton() {
        return (ButtonDispatcher) this.mButtonDispatchers.get(R$id.home);
    }

    public ButtonDispatcher getImeSwitchButton() {
        return (ButtonDispatcher) this.mButtonDispatchers.get(R$id.ime_switcher);
    }

    public ButtonDispatcher getAccessibilityButton() {
        return (ButtonDispatcher) this.mButtonDispatchers.get(R$id.accessibility_button);
    }

    public RotationContextButton getRotateSuggestionButton() {
        return (RotationContextButton) this.mButtonDispatchers.get(R$id.rotate_suggestion);
    }

    public ButtonDispatcher getHomeHandle() {
        return (ButtonDispatcher) this.mButtonDispatchers.get(R$id.home_handle);
    }

    public SparseArray<ButtonDispatcher> getButtonDispatchers() {
        return this.mButtonDispatchers;
    }

    public boolean isRecentsButtonVisible() {
        return getRecentsButton().getVisibility() == 0;
    }

    public boolean isOverviewEnabled() {
        return (this.mDisabledFlags & 16777216) == 0;
    }

    public boolean isQuickStepSwipeUpEnabled() {
        return this.mOverviewProxyService.shouldShowSwipeUpUI() && isOverviewEnabled();
    }

    private void reloadNavIcons() {
        updateIcons(Configuration.EMPTY);
    }

    public void setHideNavBarOn(boolean z) {
        this.mIsHideNavBarOn = z;
        if (this.mIsHideNavBarOn) {
            updateButtonColor(this.COLOR_KEY_TRANSPARENT, -1);
        } else {
            notifyNavBarColorChange(this.mBackgroundColor);
        }
    }

    public void updateNavButtonState(boolean z) {
        this.mShowNavKey = z;
        if (getNavButton() != null) {
            getNavButton().setVisibility(showNavKey() ? 0 : 4);
        }
    }

    private boolean isLightColor(int i) {
        return !isLegible(-1, i);
    }

    private boolean isDarkColor(int i) {
        return !isLegible(-16777216, i);
    }

    private boolean isLegible(int i, int i2) {
        return ColorUtils.calculateContrast(i, ColorUtils.setAlphaComponent(i2, 255)) >= 2.0d;
    }

    public void notifyNavBarColorChange(int i) {
        notifyNavBarColorChange(i, false);
    }

    private void notifyNavBarColorChange(int i, boolean z) {
        int mode = this.mBarTransitions.getMode();
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("notifyNavBarColorChange barMode: ");
            sb.append(mode);
            sb.append(", bgcolor: 0x");
            sb.append(Integer.toHexString(i));
            sb.append(", mImeShow: ");
            sb.append(this.mImeShow);
            sb.append(", mKeyguardShow: ");
            sb.append(this.mKeyguardShow);
            sb.append(", expanded: ");
            sb.append(z);
            sb.append(", hasPinnedHeadsUp(): ");
            sb.append(hasPinnedHeadsUp());
            sb.append(", mDockedStackExists: ");
            sb.append(this.mDockedStackExists);
            sb.append(", mIsLightBar: ");
            sb.append(this.mIsLightBar);
            sb.append(", isScreenCompat: ");
            sb.append(OpUtils.isScreenCompat());
            sb.append(", isScreenSaverOn: ");
            sb.append(isScreenSaverOn());
            Log.d("StatusBar/NavBarView", sb.toString());
        }
        if (this.mIsCTSOn != OpUtils.isCTS()) {
            this.mIsCTSOn = OpUtils.isCTS();
            updateMainIcons(this.mIsCTSOn);
        }
        if (OpUtils.isCTS()) {
            updateButtonColor(-1728053248, -16777216);
            return;
        }
        if (getNavButton() != null) {
            getNavButton().setVisibility(showNavKey() ? 0 : 4);
        }
        if (this.mImeShow) {
            if (mode == 2) {
                updateButtonColor(this.COLOR_KEY_TRANSPARENT, -1);
            } else {
                updateButtonColor(this.COLOR_KEY_LIGHT, -16777216);
            }
        } else if ((this.mKeyguardShow || (z && !hasPinnedHeadsUp() && !this.mDockedStackExists)) && !this.mIsLightBar && !isScreenSaverOn() && mode != 0) {
            updateButtonColor(this.COLOR_KEY_TRANSPARENT, -1);
        } else {
            this.mBackgroundColor = i;
            if (this.mDockedStackExists) {
                updateButtonColor(this.COLOR_KEY_LIGHT, -16777216);
            } else if (mode == 1 || mode == 2) {
                updateButtonColor(this.COLOR_KEY_TRANSPARENT, -1);
            } else if (mode == 0) {
                if (OpUtils.isScreenCompat()) {
                    updateButtonColor(this.COLOR_KEY_DARK, -1);
                } else {
                    updateButtonColor(this.COLOR_KEY_LIGHT, -16777216);
                }
            } else if (mode == 3 && isScreenSaverOn()) {
                updateButtonColor(this.COLOR_KEY_LIGHT, -16777216);
            } else if (!this.mIsHideNavBarOn || OpUtils.isHomeApp()) {
                if (OpUtils.isScreenCompat()) {
                    updateButtonColor(this.COLOR_KEY_DARK, -1);
                } else if (this.mIsLightBar) {
                    updateButtonColor(this.COLOR_KEY_LIGHT, -16777216);
                } else {
                    int i2 = this.mBackgroundColor;
                    if (i2 == 0) {
                        updateButtonColor(this.COLOR_KEY_TRANSPARENT, -1);
                    } else if (i2 == this.COLOR_BACKGROUND_LIGHT) {
                        updateButtonColor(this.COLOR_KEY_LIGHT, -16777216);
                    } else if (i2 == this.COLOR_BACKGROUND_DARK || i2 == -16777216) {
                        updateButtonColor(this.COLOR_KEY_DARK, -1);
                    } else {
                        applyAppCustomColor();
                    }
                }
            } else {
                updateButtonColor(this.COLOR_KEY_TRANSPARENT, -1);
            }
        }
    }

    private void applyAppCustomColor() {
        if (isLightColor(this.mBackgroundColor)) {
            updateButtonColor(this.COLOR_KEY_LIGHT, -16777216);
        } else if (isDarkColor(this.mBackgroundColor)) {
            updateButtonColor(this.COLOR_KEY_DARK, -1);
        } else {
            updateButtonColor(this.COLOR_KEY_TRANSPARENT, -1);
        }
    }

    private void updateButtonColor(int i, int i2) {
        updateButtonColor(i, i2, false);
    }

    private void updateButtonColor(int i, int i2, boolean z) {
        if (i != this.mLastButtonColor || i2 != this.mLastRippleColor || z) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("updateButtonColor buttonColor=0x");
                sb.append(Integer.toHexString(i));
                sb.append(", rippleColor=0x");
                sb.append(Integer.toHexString(i2));
                sb.append(", force=");
                sb.append(z);
                sb.append(", caller: ");
                sb.append(Debug.getCallers(8));
                Log.d("StatusBar/NavBarView", sb.toString());
            }
            this.mLastButtonColor = i;
            this.mLastRippleColor = i2;
            for (int i3 = 0; i3 < this.mButtonDispatchers.size(); i3++) {
                ArrayList views = ((ButtonDispatcher) this.mButtonDispatchers.valueAt(i3)).getViews();
                int size = views.size();
                for (int i4 = 0; i4 < size; i4++) {
                    if (views.get(i4) instanceof KeyButtonView) {
                        KeyButtonView keyButtonView = (KeyButtonView) views.get(i4);
                        keyButtonView.updateThemeColor(i);
                        keyButtonView.setRippleColor(i2);
                    }
                }
            }
            postInvalidate();
        }
    }

    private boolean showNavKey() {
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("showNavKey mShowNavKey: ");
            sb.append(this.mShowNavKey);
            sb.append(" isHomeApp: ");
            sb.append(OpUtils.isHomeApp());
            sb.append(" isScreenCompat: ");
            sb.append(OpUtils.isScreenCompat());
            sb.append(" mImeShow: ");
            sb.append(this.mImeShow);
            sb.append(" mKeyguardShow: ");
            sb.append(this.mKeyguardShow);
            sb.append(" isMultiWindow: ");
            sb.append(this.mDockedStackExists);
            sb.append(" isScreenSaveron: ");
            sb.append(isScreenSaverOn());
            sb.append(" isImmersiveSticky: ");
            sb.append(this.mIsImmersiveSticky);
            sb.append(" isSystemUI: ");
            sb.append(OpUtils.isSystemUI());
            sb.append(" isInBrickMode: ");
            sb.append(this.mIsInBrickMode);
            Log.d("StatusBar/NavBarView", sb.toString());
        }
        return this.mShowNavKey && !OpUtils.isHomeApp() && !OpUtils.isScreenCompat() && !this.mImeShow && !this.mKeyguardShow && !isScreenSaverOn() && !this.mIsImmersiveSticky && !this.mDockedStackExists && !OpUtils.isSystemUI() && !this.mIsInBrickMode;
    }

    public void onShowKeyguard(boolean z) {
        if (this.mKeyguardShow != z) {
            this.mKeyguardShow = z;
            if (OpNavBarUtils.isSupportCustomNavBar() && !QuickStepContract.isGesturalMode(this.mNavBarMode)) {
                int i = 0;
                boolean z2 = this.mKeyguardShow && isScreenSaverOn();
                if (!z) {
                    i = this.mBackgroundColor;
                }
                if (!z2) {
                    notifyNavBarColorChange(i);
                }
            }
        }
    }

    public void onImmersiveSticky(boolean z) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onImmersiveSticky ");
            sb.append(z);
            Log.d("StatusBar/NavBarView", sb.toString());
        }
        this.mIsImmersiveSticky = z;
        if (getNavButton() != null) {
            getNavButton().setVisibility(showNavKey() ? 0 : 4);
        }
    }

    public void setLightBar(boolean z) {
        if (this.mIsLightBar != z) {
            StringBuilder sb = new StringBuilder();
            sb.append("setLightBar to ");
            sb.append(z);
            Log.d("StatusBar/NavBarView", sb.toString());
        }
        this.mIsLightBar = z;
        if (this.mIsCustomNavBar) {
            notifyNavBarColorChange(this.mBackgroundColor);
        }
    }

    private boolean hasPinnedHeadsUp() {
        HeadsUpManager headsUpManager = this.mHeadsUpManager;
        return headsUpManager != null && headsUpManager.hasPinnedHeadsUp();
    }

    public void refreshButtonColor() {
        updateButtonColor(this.mLastButtonColor, this.mLastRippleColor, true);
    }

    public boolean isScreenSaverOn() {
        return KeyguardUpdateMonitor.getInstance(this.mContext).isDreaming() && KeyguardUpdateMonitor.getInstance(this.mContext).isScreenOn();
    }

    private void updateIcons(Configuration configuration) {
        KeyButtonDrawable keyButtonDrawable;
        KeyButtonDrawable keyButtonDrawable2;
        boolean z = true;
        boolean z2 = configuration.orientation != this.mConfiguration.orientation;
        boolean z3 = configuration.densityDpi != this.mConfiguration.densityDpi;
        if (configuration.getLayoutDirection() == this.mConfiguration.getLayoutDirection()) {
            z = false;
        }
        if (z2 || z3) {
            if (this.mIsCustomNavBar) {
                keyButtonDrawable2 = getDrawable(R$drawable.ic_sysbar_docked2);
            } else {
                keyButtonDrawable2 = getDrawable(R$drawable.ic_sysbar_docked);
            }
            this.mDockedIcon = keyButtonDrawable2;
            this.mHomeDefaultIcon = getHomeDrawable();
        }
        if (z3 || z) {
            if (this.mIsCustomNavBar) {
                keyButtonDrawable = getDrawable(R$drawable.ic_sysbar_recent2);
            } else {
                keyButtonDrawable = getDrawable(R$drawable.ic_sysbar_recent);
            }
            this.mRecentIcon = keyButtonDrawable;
            this.mContextualButtonGroup.updateIcons();
        }
        if (z2 || z3 || z) {
            this.mBackIcon = getBackDrawable();
        }
    }

    private void updateMainIcons(boolean z) {
        KeyButtonDrawable keyButtonDrawable;
        this.mHomeDefaultIcon = getHomeDrawable();
        this.mBackIcon = getBackDrawable();
        if (z) {
            this.mRecentIcon = getDrawable(R$drawable.ic_sysbar_recent);
        } else {
            if (this.mIsCustomNavBar) {
                keyButtonDrawable = getDrawable(R$drawable.ic_sysbar_recent2);
            } else {
                keyButtonDrawable = getDrawable(R$drawable.ic_sysbar_recent);
            }
            this.mRecentIcon = keyButtonDrawable;
        }
        this.mContextualButtonGroup.updateIcons();
        getHomeButton().setImageDrawable(this.mHomeDefaultIcon);
        getBackButton().setImageDrawable(this.mBackIcon);
        getRecentsButton().setImageDrawable(this.mRecentIcon);
    }

    public KeyButtonDrawable getBackDrawable() {
        KeyButtonDrawable drawable = getDrawable(getBackDrawableRes());
        orientBackButton(drawable);
        return drawable;
    }

    public int getBackDrawableRes() {
        if (!OpNavBarUtils.isSupportCustomNavBar() || OpUtils.isCTS()) {
            return chooseNavigationIconDrawableRes(R$drawable.ic_sysbar_back, R$drawable.ic_sysbar_back_quick_step);
        }
        return chooseNavigationIconDrawableRes(R$drawable.ic_sysbar_back2, R$drawable.ic_sysbar_back_quick_step2);
    }

    public KeyButtonDrawable getHomeDrawable() {
        KeyButtonDrawable keyButtonDrawable;
        KeyButtonDrawable keyButtonDrawable2;
        boolean z = true;
        if (!OpNavBarUtils.isSupportCustomNavBar() || OpUtils.isCTS()) {
            if (!this.mOverviewProxyService.shouldShowSwipeUpUI() || QuickStepContract.isLegacyMode(this.mNavBarMode)) {
                z = false;
            }
            if (z) {
                keyButtonDrawable = getDrawable(R$drawable.ic_sysbar_home_quick_step);
            } else {
                keyButtonDrawable = getDrawable(R$drawable.ic_sysbar_home);
            }
            orientHomeButton(keyButtonDrawable);
            return keyButtonDrawable;
        }
        if (!this.mOverviewProxyService.shouldShowSwipeUpUI() || QuickStepContract.isLegacyMode(this.mNavBarMode)) {
            z = false;
        }
        if (z) {
            keyButtonDrawable2 = getDrawable(R$drawable.ic_sysbar_home_quick_step2);
        } else {
            keyButtonDrawable2 = getDrawable(R$drawable.ic_sysbar_home2);
        }
        orientHomeButton(keyButtonDrawable2);
        return keyButtonDrawable2;
    }

    private void orientBackButton(KeyButtonDrawable keyButtonDrawable) {
        boolean z = (this.mNavigationIconHints & 1) != 0;
        boolean z2 = this.mConfiguration.getLayoutDirection() == 1;
        float f = 0.0f;
        float f2 = z ? (float) -90 : 0.0f;
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("orientBackBtn: useAltBack=");
            sb.append(z);
            sb.append(", isRtl=");
            sb.append(z2);
            sb.append(", degrees=");
            sb.append(keyButtonDrawable.getRotation());
            sb.append("->");
            sb.append(f2);
            Log.d("StatusBar/NavBarView", sb.toString());
        }
        if (keyButtonDrawable.getRotation() != f2) {
            if (QuickStepContract.isGesturalMode(this.mNavBarMode)) {
                keyButtonDrawable.setRotation(f2);
                return;
            }
            if (!this.mOverviewProxyService.shouldShowSwipeUpUI() && !this.mIsVertical && z) {
                f = -getResources().getDimension(R$dimen.navbar_back_button_ime_offset);
            }
            ObjectAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(keyButtonDrawable, new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat(KeyButtonDrawable.KEY_DRAWABLE_ROTATE, new float[]{f2}), PropertyValuesHolder.ofFloat(KeyButtonDrawable.KEY_DRAWABLE_TRANSLATE_Y, new float[]{f})});
            ofPropertyValuesHolder.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            ofPropertyValuesHolder.setDuration(0);
            ofPropertyValuesHolder.start();
        }
    }

    private void orientHomeButton(KeyButtonDrawable keyButtonDrawable) {
        keyButtonDrawable.setRotation(this.mIsVertical ? 90.0f : 0.0f);
    }

    private int chooseNavigationIconDrawableRes(int i, int i2) {
        return this.mOverviewProxyService.shouldShowSwipeUpUI() && !QuickStepContract.isLegacyMode(this.mNavBarMode) ? i2 : i;
    }

    private KeyButtonDrawable getDrawable(int i) {
        return KeyButtonDrawable.create(this.mContext, i, true);
    }

    public void setWindowVisible(boolean z) {
        this.mTintController.setWindowVisible(z);
        this.mRotationButtonController.onNavigationBarWindowVisibilityChange(z);
    }

    public void setLayoutDirection(int i) {
        reloadNavIcons();
        this.mNavigationInflaterView.onLikelyDefaultLayoutChange();
    }

    public void setNavigationIconHints(int i) {
        if (i != this.mNavigationIconHints) {
            boolean z = false;
            boolean z2 = (i & 1) != 0;
            if ((this.mNavigationIconHints & 1) != 0) {
                z = true;
            }
            if (z2 != z) {
                onImeVisibilityChanged(z2);
            }
            this.mNavigationIconHints = i;
            updateNavButtonIcons();
            this.mImeShow = z2;
            if (OpNavBarUtils.isSupportCustomNavBar() && !QuickStepContract.isGesturalMode(this.mNavBarMode)) {
                if (z2) {
                    notifyNavBarColorChange(this.COLOR_KEY_LIGHT);
                } else {
                    notifyNavBarColorChange(this.mBackgroundColor);
                }
            }
        }
    }

    private void onImeVisibilityChanged(boolean z) {
        if (!z) {
            this.mTransitionListener.onBackAltCleared();
        }
        this.mImeVisible = z;
        this.mRotationButtonController.getRotationButton().setCanShowRotationButton(!this.mImeVisible);
        changeHomeHandleAlpha(false);
    }

    public void setDisabledFlags(int i) {
        if (this.mDisabledFlags != i) {
            boolean isOverviewEnabled = isOverviewEnabled();
            this.mDisabledFlags = i;
            if (!isOverviewEnabled && isOverviewEnabled()) {
                reloadNavIcons();
            }
            updateNavButtonIcons();
            updateSlippery();
            setUpSwipeUpOnboarding(isQuickStepSwipeUpEnabled());
            updateSystemUiStateFlags();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:43:0x00a5  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x00b2  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x00c6  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x00e4  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x00e6  */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x00f0  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f2  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x00fc  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateNavButtonIcons() {
        /*
            r8 = this;
            int r0 = r8.mNavigationIconHints
            r1 = 1
            r0 = r0 & r1
            r2 = 0
            if (r0 == 0) goto L_0x0009
            r0 = r1
            goto L_0x000a
        L_0x0009:
            r0 = r2
        L_0x000a:
            com.android.systemui.statusbar.policy.KeyButtonDrawable r3 = r8.mBackIcon
            r8.orientBackButton(r3)
            com.android.systemui.statusbar.policy.KeyButtonDrawable r4 = r8.mHomeDefaultIcon
            boolean r5 = r8.mUseCarModeUi
            if (r5 != 0) goto L_0x0018
            r8.orientHomeButton(r4)
        L_0x0018:
            com.android.systemui.statusbar.phone.ButtonDispatcher r5 = r8.getHomeButton()
            r5.setImageDrawable(r4)
            com.android.systemui.statusbar.phone.ButtonDispatcher r4 = r8.getBackButton()
            r4.setImageDrawable(r3)
            r8.updateRecentsIcon()
            boolean r3 = r8.mHideNavBar
            r8.updateNavButtonIcon(r3)
            int r3 = r8.mNavBarMode
            boolean r3 = com.android.systemui.shared.system.QuickStepContract.isGesturalMode(r3)
            r4 = 3
            if (r3 == 0) goto L_0x0041
            if (r0 == 0) goto L_0x0041
            int r3 = r8.mCurrentRotation
            if (r3 == r1) goto L_0x003f
            if (r3 != r4) goto L_0x0041
        L_0x003f:
            r3 = r1
            goto L_0x0042
        L_0x0041:
            r3 = r2
        L_0x0042:
            com.android.systemui.statusbar.phone.ContextualButtonGroup r5 = r8.mContextualButtonGroup
            int r6 = com.android.systemui.R$id.ime_switcher
            int r7 = r8.mNavigationIconHints
            r7 = r7 & 2
            if (r7 == 0) goto L_0x0050
            if (r3 != 0) goto L_0x0050
            r3 = r1
            goto L_0x0051
        L_0x0050:
            r3 = r2
        L_0x0051:
            r5.setButtonVisibility(r6, r3)
            com.android.systemui.statusbar.phone.NavigationBarTransitions r3 = r8.mBarTransitions
            r3.reapplyDarkIntensity()
            int r3 = r8.mNavBarMode
            boolean r3 = com.android.systemui.shared.system.QuickStepContract.isGesturalMode(r3)
            if (r3 != 0) goto L_0x006b
            int r3 = r8.mDisabledFlags
            r5 = 2097152(0x200000, float:2.938736E-39)
            r3 = r3 & r5
            if (r3 == 0) goto L_0x0069
            goto L_0x006b
        L_0x0069:
            r3 = r2
            goto L_0x006c
        L_0x006b:
            r3 = r1
        L_0x006c:
            boolean r5 = r8.isRecentsButtonDisabled()
            if (r0 != 0) goto L_0x0083
            int r6 = r8.mNavBarMode
            boolean r6 = com.android.systemui.shared.system.QuickStepContract.isGesturalMode(r6)
            if (r6 != 0) goto L_0x0081
            int r6 = r8.mDisabledFlags
            r7 = 4194304(0x400000, float:5.877472E-39)
            r6 = r6 & r7
            if (r6 == 0) goto L_0x0083
        L_0x0081:
            r6 = r1
            goto L_0x0084
        L_0x0083:
            r6 = r2
        L_0x0084:
            int r7 = r8.mNavBarMode
            boolean r7 = com.android.systemui.shared.system.QuickStepContract.isGesturalMode(r7)
            if (r7 == 0) goto L_0x0095
            if (r0 == 0) goto L_0x0095
            int r0 = r8.mCurrentRotation
            if (r0 == r1) goto L_0x0094
            if (r0 != r4) goto L_0x0095
        L_0x0094:
            r6 = r1
        L_0x0095:
            com.android.systemui.shared.system.ActivityManagerWrapper r0 = com.android.systemui.shared.system.ActivityManagerWrapper.getInstance()
            boolean r0 = r0.isScreenPinningActive()
            com.android.systemui.recents.OverviewProxyService r4 = r8.mOverviewProxyService
            boolean r4 = r4.isEnabled()
            if (r4 == 0) goto L_0x00b2
            int r4 = r8.mNavBarMode
            boolean r4 = com.android.systemui.shared.system.QuickStepContract.isLegacyMode(r4)
            r1 = r1 ^ r4
            r1 = r1 | r5
            if (r0 == 0) goto L_0x00b8
            r3 = r2
            r6 = r3
            goto L_0x00b8
        L_0x00b2:
            if (r0 == 0) goto L_0x00b7
            r1 = r2
            r6 = r1
            goto L_0x00b8
        L_0x00b7:
            r1 = r5
        L_0x00b8:
            android.view.View r0 = r8.getCurrentView()
            int r4 = com.android.systemui.R$id.nav_buttons
            android.view.View r0 = r0.findViewById(r4)
            android.view.ViewGroup r0 = (android.view.ViewGroup) r0
            if (r0 == 0) goto L_0x00dd
            android.animation.LayoutTransition r0 = r0.getLayoutTransition()
            if (r0 == 0) goto L_0x00dd
            java.util.List r4 = r0.getTransitionListeners()
            com.android.systemui.statusbar.phone.NavigationBarView$NavTransitionListener r5 = r8.mTransitionListener
            boolean r4 = r4.contains(r5)
            if (r4 != 0) goto L_0x00dd
            com.android.systemui.statusbar.phone.NavigationBarView$NavTransitionListener r4 = r8.mTransitionListener
            r0.addTransitionListener(r4)
        L_0x00dd:
            com.android.systemui.statusbar.phone.ButtonDispatcher r0 = r8.getBackButton()
            r4 = 4
            if (r6 == 0) goto L_0x00e6
            r5 = r4
            goto L_0x00e7
        L_0x00e6:
            r5 = r2
        L_0x00e7:
            r0.setVisibility(r5)
            com.android.systemui.statusbar.phone.ButtonDispatcher r0 = r8.getHomeButton()
            if (r3 == 0) goto L_0x00f2
            r3 = r4
            goto L_0x00f3
        L_0x00f2:
            r3 = r2
        L_0x00f3:
            r0.setVisibility(r3)
            com.android.systemui.statusbar.phone.ButtonDispatcher r8 = r8.getRecentsButton()
            if (r1 == 0) goto L_0x00fd
            r2 = r4
        L_0x00fd:
            r8.setVisibility(r2)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NavigationBarView.updateNavButtonIcons():void");
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public boolean isRecentsButtonDisabled() {
        return this.mUseCarModeUi || !isOverviewEnabled() || getContext().getDisplayId() != 0;
    }

    private Display getContextDisplay() {
        return getContext().getDisplay();
    }

    public void setLayoutTransitionsEnabled(boolean z) {
        this.mLayoutTransitionsEnabled = z;
        updateLayoutTransitionsEnabled();
    }

    public void setWakeAndUnlocking(boolean z) {
        setUseFadingAnimations(z);
        this.mWakeAndUnlocking = z;
        updateLayoutTransitionsEnabled();
    }

    private void updateLayoutTransitionsEnabled() {
        boolean z = !this.mWakeAndUnlocking && this.mLayoutTransitionsEnabled;
        LayoutTransition layoutTransition = ((ViewGroup) getCurrentView().findViewById(R$id.nav_buttons)).getLayoutTransition();
        if (layoutTransition == null) {
            return;
        }
        if (z) {
            layoutTransition.enableTransitionType(2);
            layoutTransition.enableTransitionType(3);
            layoutTransition.enableTransitionType(0);
            layoutTransition.enableTransitionType(1);
            return;
        }
        layoutTransition.disableTransitionType(2);
        layoutTransition.disableTransitionType(3);
        layoutTransition.disableTransitionType(0);
        layoutTransition.disableTransitionType(1);
    }

    private void setUseFadingAnimations(boolean z) {
        LayoutParams layoutParams = (LayoutParams) ((ViewGroup) getParent()).getLayoutParams();
        if (layoutParams != null) {
            boolean z2 = layoutParams.windowAnimations != 0;
            if (!z2 && z) {
                layoutParams.windowAnimations = R$style.Animation_NavigationBarFadeIn;
            } else if (z2 && !z) {
                layoutParams.windowAnimations = 0;
            }
            if (!isAttachedToWindow()) {
                Log.w("StatusBar/NavBarView", "view isn't attached");
                return;
            }
            ((WindowManager) getContext().getSystemService("window")).updateViewLayout((View) getParent(), layoutParams);
        }
    }

    public void onPanelExpandedChange() {
        updateSlippery();
        updateSystemUiStateFlags();
    }

    public void updateSystemUiStateFlags() {
        int displayId = this.mContext.getDisplayId();
        this.mOverviewProxyService.setSystemUiStateFlag(1, ActivityManagerWrapper.getInstance().isScreenPinningActive(), displayId);
        boolean z = false;
        this.mOverviewProxyService.setSystemUiStateFlag(128, (this.mDisabledFlags & 16777216) != 0, displayId);
        OverviewProxyService overviewProxyService = this.mOverviewProxyService;
        if ((this.mDisabledFlags & 2097152) != 0) {
            z = true;
        }
        overviewProxyService.setSystemUiStateFlag(256, z, displayId);
        NotificationPanelView notificationPanelView = this.mPanelView;
        if (notificationPanelView != null) {
            this.mOverviewProxyService.setSystemUiStateFlag(4, notificationPanelView.isFullyExpanded(), displayId);
            if (this.mIsPanelViewFullExpanded != this.mPanelView.isFullyExpanded()) {
                this.mIsPanelViewFullExpanded = this.mPanelView.isFullyExpanded();
                changeHomeHandleAlpha(true);
            }
        }
    }

    public void updateStates() {
        boolean z = this.mOverviewProxyService.shouldShowSwipeUpUI() && !QuickStepContract.isLegacyMode(this.mNavBarMode);
        if (z) {
            updateNavButtonState(false);
        }
        NavigationBarInflaterView navigationBarInflaterView = this.mNavigationInflaterView;
        if (navigationBarInflaterView != null) {
            navigationBarInflaterView.onLikelyDefaultLayoutChange();
        }
        updateSlippery();
        reloadNavIcons();
        updateNavButtonIcons();
        setUpSwipeUpOnboarding(isQuickStepSwipeUpEnabled());
        StringBuilder sb = new StringBuilder();
        sb.append("setHaptic ");
        sb.append(!z);
        sb.append(", ");
        sb.append(this);
        Log.d("StatusBar/NavBarView", sb.toString());
        WindowManagerWrapper.getInstance().setNavBarVirtualKeyHapticFeedbackEnabled(!z);
        getHomeButton().setAccessibilityDelegate(z ? this.mQuickStepAccessibilityDelegate : null);
    }

    public void updateSlippery() {
        boolean z;
        if (isQuickStepSwipeUpEnabled()) {
            NotificationPanelView notificationPanelView = this.mPanelView;
            if (notificationPanelView == null || !notificationPanelView.isFullyExpanded() || this.mPanelView.isCollapsing()) {
                z = false;
                setSlippery(z);
            }
        }
        z = true;
        setSlippery(z);
    }

    private void setSlippery(boolean z) {
        setWindowFlag(536870912, z);
    }

    private void setWindowFlag(int i, boolean z) {
        ViewGroup viewGroup = (ViewGroup) getParent();
        if (viewGroup != null) {
            LayoutParams layoutParams = (LayoutParams) viewGroup.getLayoutParams();
            if (layoutParams != null) {
                if (z != ((layoutParams.flags & i) != 0)) {
                    if (z) {
                        layoutParams.flags = i | layoutParams.flags;
                    } else {
                        layoutParams.flags = (~i) & layoutParams.flags;
                    }
                    if (!isAttachedToWindow()) {
                        Log.w("StatusBar/NavBarView", "isn't attached");
                        return;
                    }
                    ((WindowManager) getContext().getSystemService("window")).updateViewLayout(viewGroup, layoutParams);
                }
            }
        }
    }

    public void onNavigationModeChanged(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("onNavigationModeChanged: ");
        sb.append(this.mNavBarMode);
        sb.append(" to ");
        sb.append(i);
        Log.d("StatusBar/NavBarView", sb.toString());
        Context currentUserContext = ((NavigationModeController) Dependency.get(NavigationModeController.class)).getCurrentUserContext();
        this.mNavBarMode = i;
        this.mBarTransitions.onNavigationModeChanged(this.mNavBarMode);
        this.mEdgeBackGestureHandler.onNavigationModeChanged(this.mNavBarMode, currentUserContext);
        this.mOpEdgeNavGestureHandler.onNavigationModeChanged(this.mNavBarMode);
        this.mRecentsOnboarding.onNavigationModeChanged(this.mNavBarMode);
        getRotateSuggestionButton().onNavigationModeChanged(this.mNavBarMode);
        this.mTintController.onNavigationModeChanged(this.mNavBarMode);
        if (QuickStepContract.isGesturalMode(this.mNavBarMode)) {
            this.mTintController.start();
        } else {
            this.mTintController.stop();
        }
        if (OpNavBarUtils.isSupportCustomNavBar() && !QuickStepContract.isGesturalMode(this.mNavBarMode)) {
            setLightBar(getLightTransitionsController().getCurrentDarkIntensity() == 1.0f);
        }
    }

    public void setAccessibilityButtonState(boolean z, boolean z2) {
        this.mLongClickableAccessibilityButton = z2;
        getAccessibilityButton().setLongClickable(z2);
        this.mContextualButtonGroup.setButtonVisibility(R$id.accessibility_button, z);
    }

    /* access modifiers changed from: 0000 */
    public void hideRecentsOnboarding() {
        this.mRecentsOnboarding.hide(true);
    }

    public void onFinishInflate() {
        this.mNavigationInflaterView = (NavigationBarInflaterView) findViewById(R$id.navigation_inflater);
        this.mNavigationInflaterView.setButtonDispatchers(this.mButtonDispatchers);
        getImeSwitchButton().setOnClickListener(this.mImeSwitcherClickListener);
        DockedStackExistsListener.register(this.mDockedListener);
        updateOrientationViews();
        reloadNavIcons();
        boolean z = true;
        int i = 0;
        if (this.mOverviewProxyService.shouldShowSwipeUpUI() || !QuickStepContract.isLegacyMode(this.mNavBarMode) || System.getInt(this.mContext.getContentResolver(), "op_gesture_button_enabled", 0) != 1) {
            z = false;
        }
        this.mShowNavKey = z;
        if (getNavButton() != null) {
            ButtonDispatcher navButton = getNavButton();
            if (!showNavKey()) {
                i = 4;
            }
            navButton.setVisibility(i);
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        this.mDeadZone.onDraw(canvas);
        super.onDraw(canvas);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mActiveRegion.setEmpty();
        updateButtonLocation(getBackButton(), this.mBackButtonBounds, true);
        updateButtonLocation(getHomeButton(), this.mHomeButtonBounds, false);
        updateButtonLocation(getRecentsButton(), this.mRecentsButtonBounds, false);
        updateButtonLocation(getRotateSuggestionButton(), this.mRotationButtonBounds, true);
        this.mOverviewProxyService.onActiveNavBarRegionChanges(this.mActiveRegion);
        this.mRecentsOnboarding.setNavBarHeight(getMeasuredHeight());
    }

    private void updateButtonLocation(ButtonDispatcher buttonDispatcher, Rect rect, boolean z) {
        View currentView = buttonDispatcher.getCurrentView();
        if (currentView == null) {
            rect.setEmpty();
            return;
        }
        float translationX = currentView.getTranslationX();
        float translationY = currentView.getTranslationY();
        currentView.setTranslationX(0.0f);
        currentView.setTranslationY(0.0f);
        if (z) {
            currentView.getLocationOnScreen(this.mTmpPosition);
            int[] iArr = this.mTmpPosition;
            rect.set(iArr[0], iArr[1], iArr[0] + currentView.getMeasuredWidth(), this.mTmpPosition[1] + currentView.getMeasuredHeight());
            this.mActiveRegion.op(rect, Op.UNION);
        }
        currentView.getLocationInWindow(this.mTmpPosition);
        int[] iArr2 = this.mTmpPosition;
        rect.set(iArr2[0], iArr2[1], iArr2[0] + currentView.getMeasuredWidth(), this.mTmpPosition[1] + currentView.getMeasuredHeight());
        currentView.setTranslationX(translationX);
        currentView.setTranslationY(translationY);
    }

    private void updateOrientationViews() {
        this.mHorizontal = findViewById(R$id.horizontal);
        this.mVertical = findViewById(R$id.vertical);
        updateCurrentView();
    }

    /* access modifiers changed from: 0000 */
    public boolean needsReorient(int i) {
        return this.mCurrentRotation != i;
    }

    private void updateCurrentView() {
        resetViews();
        this.mCurrentView = this.mIsVertical ? this.mVertical : this.mHorizontal;
        boolean z = false;
        this.mCurrentView.setVisibility(0);
        this.mNavigationInflaterView.setVertical(this.mIsVertical);
        this.mNavigationInflaterView.updateCurrentView();
        this.mCurrentRotation = getContextDisplay().getRotation();
        NavigationBarInflaterView navigationBarInflaterView = this.mNavigationInflaterView;
        if (this.mCurrentRotation == 1) {
            z = true;
        }
        navigationBarInflaterView.setAlternativeOrder(z);
        this.mNavigationInflaterView.updateButtonDispatchersCurrentView();
        updateLayoutTransitionsEnabled();
    }

    private void resetViews() {
        this.mHorizontal.setVisibility(8);
        this.mVertical.setVisibility(8);
    }

    private void updateRecentsIcon() {
        this.mDockedIcon.setRotation((!this.mDockedStackExists || !this.mIsVertical) ? 0.0f : 90.0f);
        getRecentsButton().setImageDrawable(this.mDockedStackExists ? this.mDockedIcon : this.mRecentIcon);
        this.mBarTransitions.reapplyDarkIntensity();
    }

    public void updateNavButtonIcon(boolean z) {
        KeyButtonDrawable keyButtonDrawable;
        this.mHideNavBar = z;
        if (getNavButton() != null) {
            ButtonDispatcher navButton = getNavButton();
            if (z) {
                keyButtonDrawable = getDrawable(R$drawable.ic_sysbar_pin_on);
            } else {
                keyButtonDrawable = getDrawable(R$drawable.ic_sysbar_pin_off);
            }
            navButton.setImageDrawable(keyButtonDrawable);
        }
    }

    public void showPinningEnterExitToast(boolean z) {
        if (z) {
            this.mScreenPinningNotify.showPinningStartToast();
        } else {
            this.mScreenPinningNotify.showPinningExitToast();
        }
    }

    public void showPinningEscapeToast() {
        this.mScreenPinningNotify.showEscapeToast(isRecentsButtonVisible());
    }

    public void reorient() {
        updateCurrentView();
        ((NavigationBarFrame) getRootView()).setDeadZone(this.mDeadZone);
        this.mDeadZone.onConfigurationChanged(this.mCurrentRotation);
        this.mBarTransitions.init();
        boolean z = false;
        if (getNavButton() != null) {
            getNavButton().setVisibility(showNavKey() ? 0 : 4);
        }
        String str = "StatusBar/NavBarView";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("reorient(): rot=");
            sb.append(this.mCurrentRotation);
            Log.d(str, sb.toString());
        }
        if (!isLayoutDirectionResolved()) {
            resolveLayoutDirection();
        }
        updateNavButtonIcons();
        getHomeButton().setVertical(this.mIsVertical);
        ButtonDispatcher homeHandle = getHomeHandle();
        int i = this.mCurrentRotation;
        if (i == 0 || i == 2) {
            z = true;
        }
        homeHandle.setVertical(z);
        if (this.mEdgeBackGestureHandler != null) {
            if (DEBUG) {
                Log.d(str, "NavBarView onConfigurationChanged");
            }
            this.mEdgeBackGestureHandler.onConfigurationChanged(this.mCurrentRotation);
        }
        OpEdgeNavGestureHandler opEdgeNavGestureHandler = this.mOpEdgeNavGestureHandler;
        if (opEdgeNavGestureHandler != null) {
            opEdgeNavGestureHandler.onConfigurationChanged(this.mCurrentRotation);
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        int size = MeasureSpec.getSize(i);
        int size2 = MeasureSpec.getSize(i2);
        String str = "StatusBar/NavBarView";
        if (DEBUG) {
            Log.d(str, String.format("onMeasure: (%dx%d) old: (%dx%d)", new Object[]{Integer.valueOf(size), Integer.valueOf(size2), Integer.valueOf(getMeasuredWidth()), Integer.valueOf(getMeasuredHeight())}));
        }
        boolean z = size > 0 && size2 > size && !QuickStepContract.isGesturalMode(this.mNavBarMode);
        if (z != this.mIsVertical) {
            this.mIsVertical = z;
            if (DEBUG) {
                Object[] objArr = new Object[3];
                objArr[0] = Integer.valueOf(size2);
                objArr[1] = Integer.valueOf(size);
                objArr[2] = this.mIsVertical ? "y" : "n";
                Log.d(str, String.format("onMeasure: h=%d, w=%d, vert=%s", objArr));
            }
            reorient();
            notifyVerticalChangedListener(z);
        }
        if (QuickStepContract.isGesturalMode(this.mNavBarMode)) {
            if (this.mIsVertical) {
                i3 = getResources().getDimensionPixelSize(17105290);
            } else {
                i3 = getResources().getDimensionPixelSize(17105288);
            }
            this.mBarTransitions.setBackgroundFrame(new Rect(0, getResources().getDimensionPixelSize(17105285) - i3, size, size2));
        } else {
            this.mBarTransitions.setBackgroundFrame(null);
        }
        super.onMeasure(i, i2);
    }

    private void notifyVerticalChangedListener(boolean z) {
        OnVerticalChangedListener onVerticalChangedListener = this.mOnVerticalChangedListener;
        if (onVerticalChangedListener != null) {
            onVerticalChangedListener.onVerticalChanged(z);
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mTmpLastConfiguration.updateFrom(this.mConfiguration);
        this.mConfiguration.updateFrom(configuration);
        boolean updateCarMode = updateCarMode();
        updateIcons(this.mTmpLastConfiguration);
        updateRecentsIcon();
        this.mRecentsOnboarding.onConfigurationChanged(this.mConfiguration);
        if (!updateCarMode) {
            Configuration configuration2 = this.mTmpLastConfiguration;
            if (configuration2.densityDpi == this.mConfiguration.densityDpi && configuration2.getLayoutDirection() == this.mConfiguration.getLayoutDirection()) {
                return;
            }
        }
        updateNavButtonIcons();
    }

    private boolean updateCarMode() {
        Configuration configuration = this.mConfiguration;
        if (configuration != null) {
            boolean z = (configuration.uiMode & 15) == 3;
            if (z != this.mInCarMode) {
                this.mInCarMode = z;
                this.mUseCarModeUi = false;
            }
        }
        return false;
    }

    private String getResourceName(int i) {
        if (i == 0) {
            return "(null)";
        }
        try {
            return getContext().getResources().getResourceName(i);
        } catch (NotFoundException unused) {
            return "(unknown)";
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestApplyInsets();
        reorient();
        this.mNavBarMode = ((NavigationModeController) Dependency.get(NavigationModeController.class)).addListener(this);
        onNavigationModeChanged(this.mNavBarMode);
        setUpSwipeUpOnboarding(isQuickStepSwipeUpEnabled());
        RotationButtonController rotationButtonController = this.mRotationButtonController;
        if (rotationButtonController != null) {
            rotationButtonController.registerListeners();
        }
        this.mEdgeBackGestureHandler.onNavBarAttached();
        this.mOpEdgeNavGestureHandler.setHomeHandle(getHomeHandle().getCurrentView());
        this.mOpEdgeNavGestureHandler.onNavBarAttached();
        changeHomeHandleAlpha(false);
        getViewTreeObserver().addOnComputeInternalInsetsListener(this.mOnComputeInternalInsetsListener);
        getLightTransitionsController().bypassTransition(true);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((NavigationModeController) Dependency.get(NavigationModeController.class)).removeListener(this);
        setUpSwipeUpOnboarding(false);
        for (int i = 0; i < this.mButtonDispatchers.size(); i++) {
            ((ButtonDispatcher) this.mButtonDispatchers.valueAt(i)).onDestroy();
        }
        RotationButtonController rotationButtonController = this.mRotationButtonController;
        if (rotationButtonController != null) {
            rotationButtonController.unregisterListeners();
        }
        this.mEdgeBackGestureHandler.onNavBarDetached();
        this.mOpEdgeNavGestureHandler.onNavBarDetached();
        getViewTreeObserver().removeOnComputeInternalInsetsListener(this.mOnComputeInternalInsetsListener);
    }

    private void setUpSwipeUpOnboarding(boolean z) {
        if (z) {
            this.mRecentsOnboarding.onConnectedToLauncher();
        } else {
            this.mRecentsOnboarding.onDisconnectedFromLauncher();
        }
    }

    /* JADX WARNING: type inference failed for: r7v6, types: [java.lang.String] */
    /* JADX WARNING: type inference failed for: r7v19, types: [java.lang.Integer] */
    /* JADX WARNING: type inference failed for: r7v20 */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Unknown variable types count: 1 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void dump(java.io.FileDescriptor r7, java.io.PrintWriter r8, java.lang.String[] r9) {
        /*
            r6 = this;
            java.lang.String r7 = "NavigationBarView {"
            r8.println(r7)
            android.graphics.Rect r7 = new android.graphics.Rect
            r7.<init>()
            android.graphics.Point r9 = new android.graphics.Point
            r9.<init>()
            android.view.Display r0 = r6.getContextDisplay()
            r0.getRealSize(r9)
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "      this: "
            r0.append(r1)
            java.lang.String r1 = com.android.systemui.statusbar.phone.StatusBar.viewInfo(r6)
            r0.append(r1)
            java.lang.String r1 = " "
            r0.append(r1)
            int r2 = r6.getVisibility()
            java.lang.String r2 = visibilityToString(r2)
            r0.append(r2)
            java.lang.String r0 = r0.toString()
            r2 = 0
            java.lang.Object[] r3 = new java.lang.Object[r2]
            java.lang.String r0 = java.lang.String.format(r0, r3)
            r8.println(r0)
            r6.getWindowVisibleDisplayFrame(r7)
            int r0 = r7.right
            int r3 = r9.x
            r4 = 1
            if (r0 > r3) goto L_0x0058
            int r0 = r7.bottom
            int r9 = r9.y
            if (r0 <= r9) goto L_0x0056
            goto L_0x0058
        L_0x0056:
            r9 = r2
            goto L_0x0059
        L_0x0058:
            r9 = r4
        L_0x0059:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r3 = "      window: "
            r0.append(r3)
            java.lang.String r7 = r7.toShortString()
            r0.append(r7)
            r0.append(r1)
            int r7 = r6.getWindowVisibility()
            java.lang.String r7 = visibilityToString(r7)
            r0.append(r7)
            java.lang.String r7 = ""
            if (r9 == 0) goto L_0x007f
            java.lang.String r9 = " OFFSCREEN!"
            goto L_0x0080
        L_0x007f:
            r9 = r7
        L_0x0080:
            r0.append(r9)
            java.lang.String r9 = r0.toString()
            r8.println(r9)
            r9 = 5
            java.lang.Object[] r9 = new java.lang.Object[r9]
            android.view.View r0 = r6.getCurrentView()
            int r0 = r0.getId()
            java.lang.String r0 = r6.getResourceName(r0)
            r9[r2] = r0
            android.view.View r0 = r6.getCurrentView()
            int r0 = r0.getWidth()
            java.lang.Integer r0 = java.lang.Integer.valueOf(r0)
            r9[r4] = r0
            android.view.View r0 = r6.getCurrentView()
            int r0 = r0.getHeight()
            java.lang.Integer r0 = java.lang.Integer.valueOf(r0)
            r1 = 2
            r9[r1] = r0
            android.view.View r0 = r6.getCurrentView()
            int r0 = r0.getVisibility()
            java.lang.String r0 = visibilityToString(r0)
            r3 = 3
            r9[r3] = r0
            r0 = 4
            android.view.View r5 = r6.getCurrentView()
            float r5 = r5.getAlpha()
            java.lang.Float r5 = java.lang.Float.valueOf(r5)
            r9[r0] = r5
            java.lang.String r0 = "      mCurrentView: id=%s (%dx%d) %s %f"
            java.lang.String r9 = java.lang.String.format(r0, r9)
            r8.println(r9)
            java.lang.Object[] r9 = new java.lang.Object[r3]
            int r0 = r6.mDisabledFlags
            java.lang.Integer r0 = java.lang.Integer.valueOf(r0)
            r9[r2] = r0
            boolean r0 = r6.mIsVertical
            if (r0 == 0) goto L_0x00f1
            java.lang.String r0 = "true"
            goto L_0x00f3
        L_0x00f1:
            java.lang.String r0 = "false"
        L_0x00f3:
            r9[r4] = r0
            com.android.systemui.statusbar.phone.LightBarTransitionsController r0 = r6.getLightTransitionsController()
            float r0 = r0.getCurrentDarkIntensity()
            java.lang.Float r0 = java.lang.Float.valueOf(r0)
            r9[r1] = r0
            java.lang.String r0 = "      disabled=0x%08x vertical=%s darkIntensity=%.2f"
            java.lang.String r9 = java.lang.String.format(r0, r9)
            r8.println(r9)
            com.android.systemui.statusbar.phone.ButtonDispatcher r9 = r6.getBackButton()
            java.lang.String r0 = "back"
            dumpButton(r8, r0, r9)
            com.android.systemui.statusbar.phone.ButtonDispatcher r9 = r6.getHomeButton()
            java.lang.String r0 = "home"
            dumpButton(r8, r0, r9)
            com.android.systemui.statusbar.phone.ButtonDispatcher r9 = r6.getRecentsButton()
            java.lang.String r0 = "rcnt"
            dumpButton(r8, r0, r9)
            com.android.systemui.statusbar.phone.RotationContextButton r9 = r6.getRotateSuggestionButton()
            java.lang.String r0 = "rota"
            dumpButton(r8, r0, r9)
            com.android.systemui.statusbar.phone.ButtonDispatcher r9 = r6.getAccessibilityButton()
            java.lang.String r0 = "a11y"
            dumpButton(r8, r0, r9)
            java.lang.String r9 = "    }"
            r8.println(r9)
            com.android.systemui.statusbar.phone.ContextualButtonGroup r9 = r6.mContextualButtonGroup
            r9.dump(r8)
            com.android.systemui.recents.RecentsOnboarding r9 = r6.mRecentsOnboarding
            r9.dump(r8)
            com.android.systemui.statusbar.phone.NavBarTintController r9 = r6.mTintController
            r9.dump(r8)
            com.android.systemui.statusbar.phone.EdgeBackGestureHandler r9 = r6.mEdgeBackGestureHandler
            r9.dump(r8)
            boolean r9 = DEBUG
            if (r9 == 0) goto L_0x01f0
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r0 = "barMode: "
            r9.append(r0)
            com.android.systemui.statusbar.phone.NavigationBarTransitions r0 = r6.mBarTransitions
            if (r0 == 0) goto L_0x016c
            int r7 = r0.getMode()
            java.lang.Integer r7 = java.lang.Integer.valueOf(r7)
        L_0x016c:
            r9.append(r7)
            java.lang.String r7 = r9.toString()
            r8.println(r7)
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r9 = "mIsLightBar: "
            r7.append(r9)
            boolean r9 = r6.mIsLightBar
            r7.append(r9)
            java.lang.String r7 = r7.toString()
            r8.println(r7)
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r9 = "mIsHideNavBarOn: "
            r7.append(r9)
            boolean r9 = r6.mIsHideNavBarOn
            r7.append(r9)
            java.lang.String r7 = r7.toString()
            r8.println(r7)
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r9 = "mBackgroundColor: "
            r7.append(r9)
            int r9 = r6.mBackgroundColor
            java.lang.String r9 = java.lang.Integer.toHexString(r9)
            r7.append(r9)
            java.lang.String r7 = r7.toString()
            r8.println(r7)
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r9 = "mLastButtonColor: "
            r7.append(r9)
            int r9 = r6.mLastButtonColor
            java.lang.String r9 = java.lang.Integer.toHexString(r9)
            r7.append(r9)
            java.lang.String r7 = r7.toString()
            r8.println(r7)
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r9 = "mLastRippleColor: "
            r7.append(r9)
            int r6 = r6.mLastRippleColor
            java.lang.String r6 = java.lang.Integer.toHexString(r6)
            r7.append(r6)
            java.lang.String r6 = r7.toString()
            r8.println(r6)
        L_0x01f0:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NavigationBarView.dump(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):void");
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        int systemWindowInsetLeft = windowInsets.getSystemWindowInsetLeft();
        int systemWindowInsetRight = windowInsets.getSystemWindowInsetRight();
        setPadding(systemWindowInsetLeft, windowInsets.getSystemWindowInsetTop(), systemWindowInsetRight, windowInsets.getSystemWindowInsetBottom());
        this.mEdgeBackGestureHandler.setInsets(systemWindowInsetLeft, systemWindowInsetRight);
        return super.onApplyWindowInsets(windowInsets);
    }

    private static void dumpButton(PrintWriter printWriter, String str, ButtonDispatcher buttonDispatcher) {
        StringBuilder sb = new StringBuilder();
        sb.append("      ");
        sb.append(str);
        sb.append(": ");
        printWriter.print(sb.toString());
        if (buttonDispatcher == null) {
            printWriter.print("null");
        } else {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(visibilityToString(buttonDispatcher.getVisibility()));
            sb2.append(" alpha=");
            sb2.append(buttonDispatcher.getAlpha());
            printWriter.print(sb2.toString());
        }
        printWriter.println();
    }

    public /* synthetic */ void lambda$new$2$NavigationBarView(Boolean bool) {
        post(new Runnable(bool) {
            private final /* synthetic */ Boolean f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                NavigationBarView.this.lambda$new$1$NavigationBarView(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$new$1$NavigationBarView(Boolean bool) {
        this.mDockedStackExists = bool.booleanValue();
        updateRecentsIcon();
    }

    public void onBrickModeChanged(boolean z) {
        this.mIsInBrickMode = z;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:30:0x006d, code lost:
        if (r6.isFullyExpanded() != false) goto L_0x0071;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void changeHomeHandleAlpha(boolean r7) {
        /*
            r6 = this;
            android.content.Context r0 = r6.getContext()
            java.lang.Class<com.android.systemui.statusbar.phone.StatusBar> r1 = com.android.systemui.statusbar.phone.StatusBar.class
            java.lang.Object r0 = com.android.systemui.SysUiServiceProvider.getComponent(r0, r1)
            com.android.systemui.statusbar.phone.StatusBar r0 = (com.android.systemui.statusbar.phone.StatusBar) r0
            int r1 = r6.mNavBarMode
            boolean r1 = com.android.systemui.shared.system.QuickStepContract.isGesturalMode(r1)
            r2 = 1
            if (r1 == 0) goto L_0x0032
            int r1 = r0.getNavigationBarHiddenMode()
            if (r1 == r2) goto L_0x0032
            android.content.Context r1 = r6.mContext
            com.android.keyguard.KeyguardUpdateMonitor r1 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r1)
            boolean r1 = r1.isKeyguardVisible()
            if (r1 != 0) goto L_0x0032
            boolean r1 = com.oneplus.util.OpUtils.isHomeApp()
            if (r1 != 0) goto L_0x0032
            com.android.systemui.statusbar.phone.ButtonDispatcher r1 = r6.getHomeHandle()
            goto L_0x0033
        L_0x0032:
            r1 = 0
        L_0x0033:
            int r3 = r6.mNavBarMode
            boolean r3 = com.android.systemui.shared.system.QuickStepContract.isGesturalMode(r3)
            r4 = 0
            if (r3 == 0) goto L_0x005b
            boolean r3 = r6.mImeVisible
            if (r3 == 0) goto L_0x005b
            int r0 = r0.getNavigationBarHiddenMode()
            if (r0 == r2) goto L_0x004e
            com.android.systemui.statusbar.phone.EdgeBackGestureHandler r0 = r6.mEdgeBackGestureHandler
            if (r0 == 0) goto L_0x005b
            boolean r0 = com.android.systemui.statusbar.phone.EdgeBackGestureHandler.sSideGestureEnabled
            if (r0 != 0) goto L_0x005b
        L_0x004e:
            com.android.systemui.statusbar.phone.ButtonDispatcher r1 = r6.getHomeHandle()
            java.lang.String r0 = "StatusBar/NavBarView"
            java.lang.String r3 = "hideHomeHandle when no bar and ime"
            android.util.Log.d(r0, r3)
            r0 = r2
            goto L_0x005c
        L_0x005b:
            r0 = r4
        L_0x005c:
            if (r1 == 0) goto L_0x0085
            int r3 = r6.mCurrentRotation
            if (r3 == r2) goto L_0x0065
            r5 = 3
            if (r3 != r5) goto L_0x0070
        L_0x0065:
            com.android.systemui.statusbar.phone.NotificationPanelView r6 = r6.mPanelView
            if (r6 == 0) goto L_0x0070
            boolean r6 = r6.isFullyExpanded()
            if (r6 == 0) goto L_0x0070
            goto L_0x0071
        L_0x0070:
            r2 = r4
        L_0x0071:
            if (r2 != 0) goto L_0x0075
            if (r0 == 0) goto L_0x0076
        L_0x0075:
            r4 = 4
        L_0x0076:
            r1.setVisibility(r4)
            if (r2 != 0) goto L_0x0081
            if (r0 == 0) goto L_0x007e
            goto L_0x0081
        L_0x007e:
            r6 = 1065353216(0x3f800000, float:1.0)
            goto L_0x0082
        L_0x0081:
            r6 = 0
        L_0x0082:
            r1.setAlpha(r6, r7)
        L_0x0085:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NavigationBarView.changeHomeHandleAlpha(boolean):void");
    }

    public boolean isImeShow() {
        return this.mImeShow;
    }
}

package com.android.systemui.statusbar.phone;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R$bool;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.phone.StatusBarIconController.DarkIconManager;
import com.android.systemui.statusbar.policy.EncryptionHelper;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.oneplus.systemui.statusbar.phone.OpCollapsedStatusBarFragment;

public class CollapsedStatusBarFragment extends OpCollapsedStatusBarFragment implements Callbacks, StateListener {
    private View mCenteredIconArea;
    private View mClockView;
    /* access modifiers changed from: private */
    public CommandQueue mCommandQueue;
    private DarkIconManager mDarkIconManager;
    private int mDisabled1;
    private KeyguardMonitor mKeyguardMonitor;
    private NetworkController mNetworkController;
    private View mNotificationIconAreaInner;
    private View mOperatorNameFrame;
    private SignalCallback mSignalCallback = new SignalCallback() {
        public void setIsAirplaneMode(IconState iconState) {
            CollapsedStatusBarFragment.this.mCommandQueue.recomputeDisableFlags(CollapsedStatusBarFragment.this.getContext().getDisplayId(), true);
        }
    };
    private PhoneStatusBarView mStatusBar;
    private StatusBar mStatusBarComponent;
    private StatusBarStateController mStatusBarStateController;
    private LinearLayout mSystemIconArea;

    public void onStateChanged(int i) {
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        this.mNetworkController = (NetworkController) Dependency.get(NetworkController.class);
        this.mStatusBarStateController = (StatusBarStateController) Dependency.get(StatusBarStateController.class);
        this.mStatusBarComponent = (StatusBar) SysUiServiceProvider.getComponent(getContext(), StatusBar.class);
        this.mCommandQueue = (CommandQueue) SysUiServiceProvider.getComponent(getContext(), CommandQueue.class);
        onCreateInternal();
    }

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R$layout.status_bar, viewGroup, false);
    }

    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.mStatusBar = (PhoneStatusBarView) view;
        if (bundle != null) {
            String str = "panel_state";
            if (bundle.containsKey(str)) {
                this.mStatusBar.restoreHierarchyState(bundle.getSparseParcelableArray(str));
            }
        }
        this.mDarkIconManager = new DarkIconManager((LinearLayout) view.findViewById(R$id.statusIcons));
        this.mDarkIconManager.setShouldLog(true);
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).addIconGroup(this.mDarkIconManager);
        this.mSystemIconArea = (LinearLayout) this.mStatusBar.findViewById(R$id.system_icon_area);
        adjustSystemIconAreaLayoutParams(this.mSystemIconArea);
        this.mClockView = this.mStatusBar.findViewById(R$id.clock);
        showSystemIconArea(false);
        showClock(false);
        initEmergencyCryptkeeperText();
        initOperatorName();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).setTraceView(view.findViewById(R$id.traceview));
    }

    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        SparseArray sparseArray = new SparseArray();
        this.mStatusBar.saveHierarchyState(sparseArray);
        bundle.putSparseParcelableArray("panel_state", sparseArray);
    }

    public void onResume() {
        super.onResume();
        this.mCommandQueue.addCallback((Callbacks) this);
        this.mStatusBarStateController.addCallback(this);
    }

    public void onPause() {
        super.onPause();
        this.mCommandQueue.removeCallback((Callbacks) this);
        this.mStatusBarStateController.removeCallback(this);
    }

    public void onDestroyView() {
        super.onDestroyView();
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).removeIconGroup(this.mDarkIconManager);
        if (this.mNetworkController.hasEmergencyCryptKeeperText()) {
            this.mNetworkController.removeCallback(this.mSignalCallback);
        }
    }

    public void initNotificationIconArea(NotificationIconAreaController notificationIconAreaController) {
        ViewGroup viewGroup = (ViewGroup) this.mStatusBar.findViewById(R$id.notification_icon_area);
        this.mNotificationIconAreaInner = notificationIconAreaController.getNotificationInnerAreaView();
        if (this.mNotificationIconAreaInner.getParent() != null) {
            ((ViewGroup) this.mNotificationIconAreaInner.getParent()).removeView(this.mNotificationIconAreaInner);
        }
        viewGroup.addView(this.mNotificationIconAreaInner);
        ViewGroup viewGroup2 = (ViewGroup) this.mStatusBar.findViewById(R$id.centered_icon_area);
        this.mCenteredIconArea = notificationIconAreaController.getCenteredNotificationAreaView();
        if (this.mCenteredIconArea.getParent() != null) {
            ((ViewGroup) this.mCenteredIconArea.getParent()).removeView(this.mCenteredIconArea);
        }
        viewGroup2.addView(this.mCenteredIconArea);
        showNotificationIconArea(false);
    }

    public void disable(int i, int i2, int i3, boolean z) {
        if (i == getContext().getDisplayId()) {
            this.mState1 = i2;
            int adjustDisableFlags = adjustDisableFlags(i2);
            int i4 = this.mDisabled1 ^ adjustDisableFlags;
            this.mDisabled1 = adjustDisableFlags;
            if ((i4 & 1048576) != 0) {
                if ((1048576 & adjustDisableFlags) != 0) {
                    hideSystemIconArea(z);
                    hideOperatorName(z);
                } else {
                    showSystemIconArea(z);
                    showOperatorName(z);
                }
            }
            if ((i4 & 131072) != 0) {
                if ((131072 & adjustDisableFlags) != 0) {
                    hideNotificationIconArea(z);
                } else {
                    showNotificationIconArea(z);
                }
            }
            if (!((i4 & 8388608) == 0 && this.mClockView.getVisibility() == clockHiddenMode())) {
                if ((adjustDisableFlags & 8388608) != 0) {
                    hideClock(z);
                } else {
                    showClock(z);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public int adjustDisableFlags(int i) {
        if (!this.mKeyguardMonitor.isLaunchTransitionFadingAway() && !this.mKeyguardMonitor.isKeyguardFadingAway() && shouldHideNotificationIcons()) {
            i = i | 131072 | 1048576 | 8388608;
        }
        if (this.mStatusBarComponent.isHeadsUpShouldBeVisible()) {
            i |= 8388608;
        }
        NetworkController networkController = this.mNetworkController;
        if (networkController != null && EncryptionHelper.IS_DATA_ENCRYPTED) {
            if (networkController.hasEmergencyCryptKeeperText()) {
                i |= 131072;
            }
            if (!this.mNetworkController.isRadioOn()) {
                i |= 1048576;
            }
        }
        return (!this.mStatusBarStateController.isDozing() || !this.mStatusBarComponent.getPanel().hasCustomClock()) ? i : i | 9437184;
    }

    private boolean shouldHideNotificationIcons() {
        if (shouldHideNotificationIconsInternal()) {
            return true;
        }
        if ((this.mStatusBar.isClosed() || !this.mStatusBarComponent.hideStatusBarIconsWhenExpanded()) && !this.mStatusBarComponent.hideStatusBarIconsForBouncer()) {
            return false;
        }
        return true;
    }

    public void hideSystemIconArea(boolean z) {
        animateHide(this.mSystemIconArea, z);
    }

    public void showSystemIconArea(boolean z) {
        animateShow(this.mSystemIconArea, z);
    }

    public void hideClock(boolean z) {
        animateHiddenState(this.mClockView, clockHiddenMode(), z);
    }

    public void showClock(boolean z) {
        animateShow(this.mClockView, z);
    }

    private int clockHiddenMode() {
        return (this.mStatusBar.isClosed() || this.mKeyguardMonitor.isShowing() || this.mStatusBarStateController.isDozing()) ? 8 : 4;
    }

    public void hideNotificationIconArea(boolean z) {
        animateHide(this.mNotificationIconAreaInner, z);
        animateHide(this.mCenteredIconArea, z);
    }

    public void showNotificationIconArea(boolean z) {
        animateShow(this.mNotificationIconAreaInner, z);
        animateShow(this.mCenteredIconArea, z);
    }

    public void hideOperatorName(boolean z) {
        View view = this.mOperatorNameFrame;
        if (view != null) {
            animateHide(view, z);
        }
    }

    public void showOperatorName(boolean z) {
        View view = this.mOperatorNameFrame;
        if (view != null) {
            animateShow(view, z);
        }
    }

    private void animateHiddenState(View view, int i, boolean z) {
        view.animate().cancel();
        if (!z) {
            view.setAlpha(0.0f);
            view.setVisibility(i);
            return;
        }
        view.animate().alpha(0.0f).setDuration(160).setStartDelay(0).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable(view, i) {
            private final /* synthetic */ View f$0;
            private final /* synthetic */ int f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            public final void run() {
                this.f$0.setVisibility(this.f$1);
            }
        });
    }

    private void animateHide(View view, boolean z) {
        animateHiddenState(view, 4, z);
    }

    private void animateShow(View view, boolean z) {
        view.animate().cancel();
        view.setVisibility(0);
        if (!z) {
            view.setAlpha(1.0f);
            return;
        }
        view.animate().alpha(1.0f).setDuration(320).setInterpolator(Interpolators.ALPHA_IN).setStartDelay(50).withEndAction(null);
        if (this.mKeyguardMonitor.isKeyguardFadingAway()) {
            view.animate().setDuration(this.mKeyguardMonitor.getKeyguardFadingAwayDuration()).setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).setStartDelay(this.mKeyguardMonitor.getKeyguardFadingAwayDelay()).start();
        }
    }

    private void initEmergencyCryptkeeperText() {
        View findViewById = this.mStatusBar.findViewById(R$id.emergency_cryptkeeper_text);
        if (this.mNetworkController.hasEmergencyCryptKeeperText()) {
            if (findViewById != null) {
                ((ViewStub) findViewById).inflate();
            }
            this.mNetworkController.addCallback(this.mSignalCallback);
        } else if (findViewById != null) {
            ((ViewGroup) findViewById.getParent()).removeView(findViewById);
        }
    }

    private void initOperatorName() {
        if (getResources().getBoolean(R$bool.config_showOperatorNameInStatusBar)) {
            this.mOperatorNameFrame = ((ViewStub) this.mStatusBar.findViewById(R$id.operator_name)).inflate();
        }
    }

    public void onDozingChanged(boolean z) {
        int displayId = getContext().getDisplayId();
        int i = this.mDisabled1;
        disable(displayId, i, i, false);
    }
}

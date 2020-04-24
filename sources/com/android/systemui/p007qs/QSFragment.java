package com.android.systemui.p007qs;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.FrameLayout.LayoutParams;
import com.android.systemui.Interpolators;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$style;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.p007qs.customize.QSCustomizer;
import com.android.systemui.plugins.p006qs.C0959QS;
import com.android.systemui.plugins.p006qs.C0959QS.HeightListener;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer;
import com.android.systemui.statusbar.policy.RemoteInputQuickSettingsDisabler;
import com.android.systemui.util.InjectionInflationController;
import com.oneplus.systemui.p011qs.OpQSFragment;

/* renamed from: com.android.systemui.qs.QSFragment */
public class QSFragment extends OpQSFragment implements C0959QS, Callbacks {
    /* access modifiers changed from: private */
    public final AnimatorListener mAnimateHeaderSlidingInListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            QSFragment.this.mHeaderAnimating = false;
            QSFragment.this.updateQsState();
        }
    };
    private QSContainerImpl mContainer;
    /* access modifiers changed from: private */
    public long mDelay;
    private QSFooter mFooter;
    protected QuickStatusBarHeader mHeader;
    /* access modifiers changed from: private */
    public boolean mHeaderAnimating;
    private final QSTileHost mHost;
    private final InjectionInflationController mInjectionInflater;
    private boolean mKeyguardShowing;
    private float mLastQSExpansion = -1.0f;
    private int mLayoutDirection;
    private boolean mListening;
    private HeightListener mPanelView;
    private QSAnimator mQSAnimator;
    private QSCustomizer mQSCustomizer;
    private QSDetail mQSDetail;
    protected QSPanel mQSPanel;
    private final Rect mQsBounds = new Rect();
    private boolean mQsDisabled;
    private boolean mQsExpanded;
    private final RemoteInputQuickSettingsDisabler mRemoteInputQuickSettingsDisabler;
    private boolean mStackScrollerOverscrolling;
    private final OnPreDrawListener mStartHeaderSlidingIn = new OnPreDrawListener() {
        public boolean onPreDraw() {
            QSFragment.this.getView().getViewTreeObserver().removeOnPreDrawListener(this);
            QSFragment.this.getView().animate().translationY(0.0f).setStartDelay(QSFragment.this.mDelay).setDuration(448).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setListener(QSFragment.this.mAnimateHeaderSlidingInListener).start();
            QSFragment.this.getView().setY((float) (-QSFragment.this.mHeader.getHeight()));
            return true;
        }
    };

    public void setHasNotifications(boolean z) {
    }

    public void setHeaderClickable(boolean z) {
    }

    public QSFragment(RemoteInputQuickSettingsDisabler remoteInputQuickSettingsDisabler, InjectionInflationController injectionInflationController, Context context, QSTileHost qSTileHost) {
        this.mRemoteInputQuickSettingsDisabler = remoteInputQuickSettingsDisabler;
        this.mInjectionInflater = injectionInflationController;
        ((CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class)).observe(getLifecycle(), this);
        this.mHost = qSTileHost;
    }

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return this.mInjectionInflater.injectable(layoutInflater.cloneInContext(new ContextThemeWrapper(getContext(), R$style.qs_theme))).inflate(R$layout.qs_panel, viewGroup, false);
    }

    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.mQSPanel = (QSPanel) view.findViewById(R$id.quick_settings_panel);
        this.mQSDetail = (QSDetail) view.findViewById(R$id.qs_detail);
        this.mHeader = (QuickStatusBarHeader) view.findViewById(R$id.header);
        this.mFooter = (QSFooter) view.findViewById(R$id.qs_footer);
        this.mContainer = (QSContainerImpl) view.findViewById(R$id.quick_settings_container);
        this.mQSDetail.setQsPanel(this.mQSPanel, this.mHeader, (View) this.mFooter);
        this.mQSAnimator = new QSAnimator(this, (QuickQSPanel) this.mHeader.findViewById(R$id.quick_qs_panel), this.mQSPanel);
        this.mQSCustomizer = (QSCustomizer) view.findViewById(R$id.qs_customize);
        this.mQSCustomizer.setQs(this);
        if (bundle != null) {
            setExpanded(bundle.getBoolean("expanded"));
            setListening(bundle.getBoolean("listening"));
            setEditLocation(view);
            this.mQSCustomizer.restoreInstanceState(bundle);
            if (this.mQsExpanded) {
                this.mQSPanel.getTileLayout().restoreInstanceState(bundle);
            }
        }
        setHost(this.mHost);
    }

    public void onDestroy() {
        super.onDestroy();
        this.mQSPanel.setBrightnessListening(false);
        if (this.mListening) {
            setListening(false);
        }
    }

    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("expanded", this.mQsExpanded);
        bundle.putBoolean("listening", this.mListening);
        this.mQSCustomizer.saveInstanceState(bundle);
        if (this.mQsExpanded) {
            this.mQSPanel.getTileLayout().saveInstanceState(bundle);
        }
    }

    /* access modifiers changed from: 0000 */
    public boolean isListening() {
        return this.mListening;
    }

    /* access modifiers changed from: 0000 */
    public boolean isExpanded() {
        return this.mQsExpanded;
    }

    public View getHeader() {
        return this.mHeader;
    }

    public void setPanelView(HeightListener heightListener) {
        this.mPanelView = heightListener;
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        setEditLocation(getView());
        if (configuration.getLayoutDirection() != this.mLayoutDirection) {
            this.mLayoutDirection = configuration.getLayoutDirection();
            QSAnimator qSAnimator = this.mQSAnimator;
            if (qSAnimator != null) {
                qSAnimator.onRtlChanged();
            }
        }
    }

    private void setEditLocation(View view) {
        View findViewById = view.findViewById(16908291);
        int[] locationOnScreen = findViewById.getLocationOnScreen();
        this.mQSCustomizer.setEditLocation(locationOnScreen[0] + (findViewById.getWidth() / 2), locationOnScreen[1] + (findViewById.getHeight() / 2));
    }

    public void setContainer(ViewGroup viewGroup) {
        if (viewGroup instanceof NotificationsQuickSettingsContainer) {
            this.mQSCustomizer.setContainer((NotificationsQuickSettingsContainer) viewGroup);
        }
    }

    public boolean isCustomizing() {
        return this.mQSCustomizer.isCustomizing();
    }

    public void setHost(QSTileHost qSTileHost) {
        this.mQSPanel.setHost(qSTileHost, this.mQSCustomizer);
        this.mHeader.setQSPanel(this.mQSPanel);
        this.mFooter.setQSPanel(this.mQSPanel);
        this.mQSDetail.setHost(qSTileHost);
        QSAnimator qSAnimator = this.mQSAnimator;
        if (qSAnimator != null) {
            qSAnimator.setHost(qSTileHost);
        }
    }

    public void disable(int i, int i2, int i3, boolean z) {
        if (i == getContext().getDisplayId()) {
            int adjustDisableFlags = this.mRemoteInputQuickSettingsDisabler.adjustDisableFlags(i3);
            boolean z2 = (adjustDisableFlags & 1) != 0;
            if (z2 != this.mQsDisabled) {
                this.mQsDisabled = z2;
                this.mContainer.disable(i2, adjustDisableFlags, z);
                this.mHeader.disable(i2, adjustDisableFlags, z);
                this.mFooter.disable(i2, adjustDisableFlags, z);
                updateQsState();
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateQsState() {
        boolean z = true;
        int i = 0;
        boolean z2 = this.mQsExpanded || this.mStackScrollerOverscrolling || this.mHeaderAnimating;
        this.mQSPanel.setExpanded(this.mQsExpanded);
        this.mQSDetail.setExpanded(this.mQsExpanded);
        this.mHeader.setVisibility((this.mQsExpanded || !this.mKeyguardShowing || this.mHeaderAnimating) ? 0 : 4);
        this.mHeader.setExpanded((this.mKeyguardShowing && !this.mHeaderAnimating) || (this.mQsExpanded && !this.mStackScrollerOverscrolling));
        this.mFooter.setVisibility((this.mQsDisabled || (!this.mQsExpanded && this.mKeyguardShowing && !this.mHeaderAnimating)) ? 4 : 0);
        QSFooter qSFooter = this.mFooter;
        if ((!this.mKeyguardShowing || this.mHeaderAnimating) && (!this.mQsExpanded || this.mStackScrollerOverscrolling)) {
            z = false;
        }
        qSFooter.setExpanded(z);
        QSPanel qSPanel = this.mQSPanel;
        if (this.mQsDisabled || !z2) {
            i = 4;
        }
        qSPanel.setVisibility(i);
    }

    public QSPanel getQsPanel() {
        return this.mQSPanel;
    }

    public boolean isShowingDetail() {
        return this.mQSPanel.isShowingCustomize() || this.mQSDetail.isShowingDetail();
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return isCustomizing();
    }

    public void setExpanded(boolean z) {
        this.mQsExpanded = z;
        this.mQSPanel.setListening(this.mListening, this.mQsExpanded);
        updateQsState();
    }

    public void setKeyguardShowing(boolean z) {
        this.mKeyguardShowing = z;
        this.mLastQSExpansion = -1.0f;
        QSAnimator qSAnimator = this.mQSAnimator;
        if (qSAnimator != null) {
            qSAnimator.setOnKeyguard(z);
        }
        this.mFooter.setKeyguardShowing(z);
        updateQsState();
    }

    public void setOverscrolling(boolean z) {
        this.mStackScrollerOverscrolling = z;
        updateQsState();
    }

    public void setListening(boolean z) {
        this.mListening = z;
        this.mHeader.setListening(z);
        this.mFooter.setListening(z);
        this.mQSPanel.setListening(this.mListening, this.mQsExpanded);
    }

    public void setHeaderListening(boolean z) {
        this.mHeader.setListening(z);
        this.mFooter.setListening(z);
    }

    public void setQsExpansion(float f, float f2) {
        this.mContainer.setExpansion(f);
        this.mQSPanel.updateWLBExpansion(f);
        this.mQSPanel.updateWLBHeaderExpansion(f2);
        float f3 = f - 1.0f;
        if (!this.mHeaderAnimating) {
            View view = getView();
            if (this.mKeyguardShowing) {
                f2 = ((float) this.mHeader.getHeight()) * f3;
            }
            view.setTranslationY(f2);
        }
        if (f != this.mLastQSExpansion) {
            this.mLastQSExpansion = f;
            boolean z = true;
            boolean z2 = f == 1.0f;
            float bottom = f3 * ((float) ((this.mQSPanel.getBottom() - this.mHeader.getBottom()) + this.mHeader.getPaddingBottom() + this.mFooter.getHeight()));
            this.mHeader.setExpansion(this.mKeyguardShowing, f, bottom);
            this.mFooter.setExpansion(this.mKeyguardShowing ? 1.0f : f);
            this.mQSPanel.getQsTileRevealController().setExpansion(f);
            this.mQSPanel.getTileLayout().setExpansion(f);
            this.mQSPanel.setTranslationY(bottom);
            this.mQSDetail.setFullyExpanded(z2);
            QSPanel qSPanel = this.mQSPanel;
            if (0.0f >= f || f >= 1.0f) {
                z = false;
            }
            qSPanel.setIsExpanding(z);
            if (z2) {
                this.mQSPanel.setClipBounds(null);
            } else {
                this.mQsBounds.top = (int) (-this.mQSPanel.getTranslationY());
                this.mQsBounds.right = this.mQSPanel.getWidth();
                this.mQsBounds.bottom = this.mQSPanel.getHeight();
                this.mQSPanel.setClipBounds(this.mQsBounds);
            }
            QSAnimator qSAnimator = this.mQSAnimator;
            if (qSAnimator != null) {
                qSAnimator.setPosition(f);
            }
        }
    }

    public void animateHeaderSlidingIn(long j) {
        if (!this.mQsExpanded) {
            this.mHeaderAnimating = true;
            this.mDelay = j;
            getView().getViewTreeObserver().addOnPreDrawListener(this.mStartHeaderSlidingIn);
        }
    }

    public void animateHeaderSlidingOut() {
        this.mHeaderAnimating = true;
        getView().animate().y((float) (-this.mHeader.getHeight())).setStartDelay(0).setDuration(360).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                if (QSFragment.this.getView() != null) {
                    QSFragment.this.getView().animate().setListener(null);
                }
                QSFragment.this.mHeaderAnimating = false;
                QSFragment.this.updateQsState();
            }
        }).start();
    }

    public void setExpandClickListener(OnClickListener onClickListener) {
        this.mFooter.setExpandClickListener(onClickListener);
    }

    public void closeDetail() {
        this.mQSPanel.closeDetail();
    }

    public void notifyCustomizeChanged() {
        this.mContainer.updateExpansion();
        int i = 0;
        this.mQSPanel.setVisibility(!this.mQSCustomizer.isCustomizing() ? 0 : 4);
        QSFooter qSFooter = this.mFooter;
        if (this.mQSCustomizer.isCustomizing()) {
            i = 4;
        }
        qSFooter.setVisibility(i);
        this.mPanelView.onQsHeightChanged();
    }

    public int getDesiredHeight() {
        if (this.mQSCustomizer.isCustomizing()) {
            return getView().getHeight();
        }
        if (!this.mQSDetail.isClosingDetail()) {
            return getView().getMeasuredHeight();
        }
        LayoutParams layoutParams = (LayoutParams) this.mQSPanel.getLayoutParams();
        return layoutParams.topMargin + layoutParams.bottomMargin + this.mQSPanel.getMeasuredHeight() + getView().getPaddingBottom();
    }

    public void setHeightOverride(int i) {
        this.mContainer.setHeightOverride(i);
    }

    public int getQsMinExpansionHeight() {
        return this.mHeader.getHeight();
    }

    public void hideImmediately() {
        getView().animate().cancel();
        getView().setY((float) (-this.mHeader.getHeight()));
    }
}

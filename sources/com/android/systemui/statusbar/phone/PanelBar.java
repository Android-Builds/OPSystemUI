package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public abstract class PanelBar extends FrameLayout {
    public static final String TAG = "PanelBar";
    private boolean mBouncerShowing;
    private boolean mExpanded;
    protected PanelView mPanel;
    protected float mPanelFraction;
    private int mState = 0;
    private boolean mTracking;

    public void onClosingFinished() {
    }

    public void onExpandingFinished() {
    }

    public void onPanelCollapsed() {
    }

    public void onPanelFullyOpened() {
    }

    public void onPanelPeeked() {
    }

    public boolean panelEnabled() {
        return true;
    }

    public abstract void panelScrimMinFractionChanged(float f);

    /* renamed from: go */
    public void mo18000go(int i) {
        this.mState = i;
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("panel_bar_super_parcelable", super.onSaveInstanceState());
        bundle.putInt("state", this.mState);
        return bundle;
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable == null || !(parcelable instanceof Bundle)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        Bundle bundle = (Bundle) parcelable;
        super.onRestoreInstanceState(bundle.getParcelable("panel_bar_super_parcelable"));
        String str = "state";
        if (bundle.containsKey(str)) {
            mo18000go(bundle.getInt(str, 0));
        }
    }

    public PanelBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setPanel(PanelView panelView) {
        this.mPanel = panelView;
        panelView.setBar(this);
    }

    public void setBouncerShowing(boolean z) {
        this.mBouncerShowing = z;
        int i = z ? 4 : 0;
        setImportantForAccessibility(i);
        updateVisibility();
        PanelView panelView = this.mPanel;
        if (panelView != null) {
            panelView.setImportantForAccessibility(i);
        }
    }

    public float getExpansionFraction() {
        return this.mPanelFraction;
    }

    public boolean isExpanded() {
        return this.mExpanded;
    }

    private void updateVisibility() {
        this.mPanel.setVisibility((this.mExpanded || this.mBouncerShowing) ? 0 : 4);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z = false;
        if (!panelEnabled()) {
            if (motionEvent.getAction() == 0) {
                Log.v(TAG, String.format("onTouch: all panels disabled, ignoring touch at (%d,%d)", new Object[]{Integer.valueOf((int) motionEvent.getX()), Integer.valueOf((int) motionEvent.getY())}));
            }
            return false;
        }
        if (motionEvent.getAction() == 0) {
            PanelView panelView = this.mPanel;
            if (panelView == null) {
                Log.v(TAG, String.format("onTouch: no panel for touch at (%d,%d)", new Object[]{Integer.valueOf((int) motionEvent.getX()), Integer.valueOf((int) motionEvent.getY())}));
                return true;
            } else if (!panelView.isEnabled()) {
                Log.v(TAG, String.format("onTouch: panel (%s) is disabled, ignoring touch at (%d,%d)", new Object[]{panelView, Integer.valueOf((int) motionEvent.getX()), Integer.valueOf((int) motionEvent.getY())}));
                return true;
            }
        }
        PanelView panelView2 = this.mPanel;
        if (panelView2 == null || panelView2.onTouchEvent(motionEvent)) {
            z = true;
        }
        return z;
    }

    public void panelExpansionChanged(float f, boolean z) {
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        PanelView panelView = this.mPanel;
        this.mExpanded = z;
        this.mPanelFraction = f;
        updateVisibility();
        boolean z6 = true;
        if (z) {
            if (this.mState == 0) {
                mo18000go(1);
                onPanelPeeked();
                z5 = true;
            } else {
                z5 = false;
            }
            z4 = panelView.getExpandedFraction() >= 1.0f;
            z3 = z5;
            z2 = false;
        } else {
            z4 = false;
            z3 = false;
            z2 = true;
        }
        if (z4 && !this.mTracking) {
            mo18000go(2);
            onPanelFullyOpened();
        } else if (!z2 || this.mTracking || this.mState == 0) {
            z6 = z3;
        } else {
            mo18000go(0);
            onPanelCollapsed();
        }
        if (Build.DEBUG_ONEPLUS && z6) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append(" panelExpansionChanged:");
            sb.append(this.mState);
            sb.append(" fullyOpened:");
            sb.append(z4);
            sb.append(" fullyClosed:");
            sb.append(z2);
            Log.d(str, sb.toString());
        }
    }

    public void collapsePanel(boolean z, boolean z2, float f) {
        boolean z3;
        PanelView panelView = this.mPanel;
        if (!z || panelView.isFullyCollapsed()) {
            panelView.resetViews(false);
            panelView.setExpandedFraction(0.0f);
            panelView.cancelPeek();
            z3 = false;
        } else {
            panelView.collapse(z2, f);
            z3 = true;
        }
        if (!z3 && this.mState != 0) {
            mo18000go(0);
            onPanelCollapsed();
        }
    }

    public boolean isClosed() {
        return this.mState == 0;
    }

    public void onTrackingStarted() {
        this.mTracking = true;
    }

    public void onTrackingStopped(boolean z) {
        this.mTracking = false;
    }
}

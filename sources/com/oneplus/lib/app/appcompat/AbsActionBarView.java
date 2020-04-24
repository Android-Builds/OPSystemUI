package com.oneplus.lib.app.appcompat;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.menu.ActionMenuPresenter;
import com.oneplus.lib.menu.ActionMenuView;
import com.oneplus.support.core.view.MotionEventCompat;
import com.oneplus.support.core.view.ViewPropertyAnimatorCompat;
import com.oneplus.support.core.view.ViewPropertyAnimatorListener;

abstract class AbsActionBarView extends ViewGroup {
    protected ActionMenuPresenter mActionMenuPresenter;
    protected int mContentHeight;
    private boolean mEatingHover;
    private boolean mEatingTouch;
    protected ActionMenuView mMenuView;
    protected final Context mPopupContext;
    protected final VisibilityAnimListener mVisAnimListener;
    protected ViewPropertyAnimatorCompat mVisibilityAnim;

    protected class VisibilityAnimListener implements ViewPropertyAnimatorListener {
        private boolean mCanceled = false;
        int mFinalVisibility;

        protected VisibilityAnimListener() {
        }

        public void onAnimationStart(View view) {
            AbsActionBarView.super.setVisibility(0);
            this.mCanceled = false;
        }

        public void onAnimationEnd(View view) {
            if (!this.mCanceled) {
                AbsActionBarView absActionBarView = AbsActionBarView.this;
                absActionBarView.mVisibilityAnim = null;
                AbsActionBarView.super.setVisibility(this.mFinalVisibility);
            }
        }

        public void onAnimationCancel(View view) {
            this.mCanceled = true;
        }
    }

    protected static int next(int i, int i2, boolean z) {
        return z ? i - i2 : i + i2;
    }

    public abstract void setContentHeight(int i);

    AbsActionBarView(Context context) {
        this(context, null);
    }

    AbsActionBarView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    AbsActionBarView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mVisAnimListener = new VisibilityAnimListener();
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(16843917, typedValue, true)) {
            int i2 = typedValue.resourceId;
            if (i2 != 0) {
                this.mPopupContext = new ContextThemeWrapper(context, i2);
                return;
            }
        }
        this.mPopupContext = context;
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(null, R$styleable.ActionBar, 16843470, 0);
        setContentHeight(obtainStyledAttributes.getLayoutDimension(R$styleable.ActionBar_android_height, 0));
        obtainStyledAttributes.recycle();
        ActionMenuPresenter actionMenuPresenter = this.mActionMenuPresenter;
        if (actionMenuPresenter != null) {
            actionMenuPresenter.onConfigurationChanged(configuration);
        }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        int actionMasked = MotionEventCompat.getActionMasked(motionEvent);
        if (actionMasked == 0) {
            this.mEatingTouch = false;
        }
        if (!this.mEatingTouch) {
            boolean onTouchEvent = super.onTouchEvent(motionEvent);
            if (actionMasked == 0 && !onTouchEvent) {
                this.mEatingTouch = true;
            }
        }
        if (actionMasked == 1 || actionMasked == 3) {
            this.mEatingTouch = false;
        }
        return true;
    }

    public boolean onHoverEvent(MotionEvent motionEvent) {
        int actionMasked = MotionEventCompat.getActionMasked(motionEvent);
        if (actionMasked == 9) {
            this.mEatingHover = false;
        }
        if (!this.mEatingHover) {
            boolean onHoverEvent = super.onHoverEvent(motionEvent);
            if (actionMasked == 9 && !onHoverEvent) {
                this.mEatingHover = true;
            }
        }
        if (actionMasked == 10 || actionMasked == 3) {
            this.mEatingHover = false;
        }
        return true;
    }

    public void setVisibility(int i) {
        if (i != getVisibility()) {
            ViewPropertyAnimatorCompat viewPropertyAnimatorCompat = this.mVisibilityAnim;
            if (viewPropertyAnimatorCompat != null) {
                viewPropertyAnimatorCompat.cancel();
            }
            super.setVisibility(i);
        }
    }

    /* access modifiers changed from: protected */
    public int measureChildView(View view, int i, int i2, int i3) {
        view.measure(MeasureSpec.makeMeasureSpec(i, Integer.MIN_VALUE), i2);
        return Math.max(0, (i - view.getMeasuredWidth()) - i3);
    }

    /* access modifiers changed from: protected */
    public int positionChild(View view, int i, int i2, int i3, boolean z) {
        int measuredWidth = view.getMeasuredWidth();
        int measuredHeight = view.getMeasuredHeight();
        int i4 = i2 + ((i3 - measuredHeight) / 2);
        if (z) {
            view.layout(i - measuredWidth, i4, i, measuredHeight + i4);
        } else {
            view.layout(i, i4, i + measuredWidth, measuredHeight + i4);
        }
        return z ? -measuredWidth : measuredWidth;
    }
}

package android.support.p002v7.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.support.p000v4.view.ViewPropertyAnimatorCompat;
import android.support.p000v4.view.ViewPropertyAnimatorListener;
import android.support.p002v7.appcompat.R$attr;
import android.support.p002v7.appcompat.R$styleable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;

/* renamed from: android.support.v7.widget.AbsActionBarView */
abstract class AbsActionBarView extends ViewGroup {
    protected ActionMenuPresenter mActionMenuPresenter;
    protected int mContentHeight;
    private boolean mEatingHover;
    private boolean mEatingTouch;
    protected ActionMenuView mMenuView;
    protected final Context mPopupContext;
    protected final VisibilityAnimListener mVisAnimListener;
    protected ViewPropertyAnimatorCompat mVisibilityAnim;

    /* renamed from: android.support.v7.widget.AbsActionBarView$VisibilityAnimListener */
    protected class VisibilityAnimListener implements ViewPropertyAnimatorListener {
        private boolean mCanceled = false;

        protected VisibilityAnimListener() {
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
        if (context.getTheme().resolveAttribute(R$attr.actionBarPopupTheme, typedValue, true)) {
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
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(null, R$styleable.ActionBar, R$attr.actionBarStyle, 0);
        setContentHeight(obtainStyledAttributes.getLayoutDimension(R$styleable.ActionBar_height, 0));
        obtainStyledAttributes.recycle();
        ActionMenuPresenter actionMenuPresenter = this.mActionMenuPresenter;
        if (actionMenuPresenter != null) {
            actionMenuPresenter.onConfigurationChanged(configuration);
            throw null;
        }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
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
        int actionMasked = motionEvent.getActionMasked();
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
            if (viewPropertyAnimatorCompat == null) {
                super.setVisibility(i);
            } else {
                viewPropertyAnimatorCompat.cancel();
                throw null;
            }
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

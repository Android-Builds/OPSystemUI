package com.oneplus.lib.design.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.oneplus.lib.design.widget.CoordinatorLayout.Behavior;

class ViewOffsetBehavior<V extends View> extends Behavior<V> {
    private int mTempLeftRightOffset = 0;
    private int mTempTopBottomOffset = 0;
    private ViewOffsetHelper mViewOffsetHelper;

    public ViewOffsetBehavior() {
    }

    public ViewOffsetBehavior(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public boolean onLayoutChild(CoordinatorLayout coordinatorLayout, V v, int i) {
        layoutChild(coordinatorLayout, v, i);
        if (this.mViewOffsetHelper == null) {
            this.mViewOffsetHelper = new ViewOffsetHelper(v);
        }
        this.mViewOffsetHelper.onViewLayout();
        int i2 = this.mTempTopBottomOffset;
        if (i2 != 0) {
            this.mViewOffsetHelper.setTopAndBottomOffset(i2);
            this.mTempTopBottomOffset = 0;
        }
        int i3 = this.mTempLeftRightOffset;
        if (i3 != 0) {
            this.mViewOffsetHelper.setLeftAndRightOffset(i3);
            this.mTempLeftRightOffset = 0;
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void layoutChild(CoordinatorLayout coordinatorLayout, V v, int i) {
        coordinatorLayout.onLayoutChild(v, i);
    }

    public boolean setTopAndBottomOffset(int i) {
        ViewOffsetHelper viewOffsetHelper = this.mViewOffsetHelper;
        if (viewOffsetHelper != null) {
            return viewOffsetHelper.setTopAndBottomOffset(i);
        }
        this.mTempTopBottomOffset = i;
        return false;
    }

    public int getTopAndBottomOffset() {
        ViewOffsetHelper viewOffsetHelper = this.mViewOffsetHelper;
        if (viewOffsetHelper != null) {
            return viewOffsetHelper.getTopAndBottomOffset();
        }
        return 0;
    }
}

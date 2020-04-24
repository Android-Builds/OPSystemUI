package com.oneplus.lib.design.widget;

import android.view.View;

class ViewOffsetHelper {
    private int mLayoutLeft;
    private int mLayoutTop;
    private int mOffsetLeft;
    private int mOffsetTop;
    private final View mView;

    public ViewOffsetHelper(View view) {
        this.mView = view;
    }

    public void onViewLayout() {
        this.mLayoutTop = this.mView.getTop();
        this.mLayoutLeft = this.mView.getLeft();
        updateOffsets();
    }

    private void updateOffsets() {
        View view = this.mView;
        view.offsetTopAndBottom(this.mOffsetTop - (view.getTop() - this.mLayoutTop));
        View view2 = this.mView;
        view2.offsetLeftAndRight(this.mOffsetLeft - (view2.getLeft() - this.mLayoutLeft));
    }

    public boolean setTopAndBottomOffset(int i) {
        if (this.mOffsetTop == i) {
            return false;
        }
        this.mOffsetTop = i;
        updateOffsets();
        return true;
    }

    public boolean setLeftAndRightOffset(int i) {
        if (this.mOffsetLeft == i) {
            return false;
        }
        this.mOffsetLeft = i;
        updateOffsets();
        return true;
    }

    public int getTopAndBottomOffset() {
        return this.mOffsetTop;
    }

    public int getLayoutTop() {
        return this.mLayoutTop;
    }
}

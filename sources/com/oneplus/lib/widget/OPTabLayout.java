package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;
import java.util.ArrayList;

public class OPTabLayout extends HorizontalScrollView {
    private Interpolator fast_out_slow_in_interpolator;
    private int mContentInsetStart;
    /* access modifiers changed from: private */
    public int mMode;
    private final int mRequestedTabMaxWidth;
    private final int mTabBackgroundResId;
    /* access modifiers changed from: private */
    public int mTabGravity;
    private int mTabHorizontalSpacing;
    private int mTabMaxWidth;
    private int mTabMinWidth;
    private int mTabPaddingBottom;
    private int mTabPaddingEnd;
    private int mTabPaddingStart;
    private int mTabPaddingTop;
    private final SlidingTabStrip mTabStrip;
    private int mTabTextAppearance;
    private ColorStateList mTabTextColors;
    private final ArrayList<Object> mTabs;

    private class SlidingTabStrip extends LinearLayout {
        private int mIndicatorLeft = -1;
        private int mIndicatorRight = -1;
        private int mSelectedIndicatorHeight;
        private final Paint mSelectedIndicatorPaint;
        private int mSelectedPosition = -1;
        private float mSelectionOffset;

        SlidingTabStrip(Context context) {
            super(context);
            setWillNotDraw(false);
            this.mSelectedIndicatorPaint = new Paint();
        }

        /* access modifiers changed from: 0000 */
        public void setSelectedIndicatorColor(int i) {
            if (this.mSelectedIndicatorPaint.getColor() != i) {
                this.mSelectedIndicatorPaint.setColor(i);
                postInvalidateOnAnimation();
            }
        }

        /* access modifiers changed from: 0000 */
        public void setSelectedIndicatorHeight(int i) {
            if (this.mSelectedIndicatorHeight != i) {
                this.mSelectedIndicatorHeight = i;
                postInvalidateOnAnimation();
            }
        }

        /* access modifiers changed from: protected */
        public void onMeasure(int i, int i2) {
            super.onMeasure(i, i2);
            if (MeasureSpec.getMode(i) == 1073741824 && OPTabLayout.this.mMode == 1 && OPTabLayout.this.mTabGravity == 1) {
                int childCount = getChildCount();
                int makeMeasureSpec = MeasureSpec.makeMeasureSpec(0, 0);
                int i3 = 0;
                for (int i4 = 0; i4 < childCount; i4++) {
                    View childAt = getChildAt(i4);
                    childAt.measure(makeMeasureSpec, i2);
                    i3 = Math.max(i3, childAt.getMeasuredWidth());
                }
                if (i3 > 0) {
                    if (i3 * childCount <= getMeasuredWidth() - (OPTabLayout.this.dpToPx(16) * 2)) {
                        for (int i5 = 0; i5 < childCount; i5++) {
                            LayoutParams layoutParams = (LayoutParams) getChildAt(i5).getLayoutParams();
                            layoutParams.width = i3;
                            layoutParams.weight = 0.0f;
                        }
                    } else {
                        OPTabLayout.this.mTabGravity = 0;
                        OPTabLayout.this.updateTabViewsLayoutParams();
                    }
                    super.onMeasure(i, i2);
                }
            }
        }

        /* access modifiers changed from: protected */
        public void onLayout(boolean z, int i, int i2, int i3, int i4) {
            super.onLayout(z, i, i2, i3, i4);
            updateIndicatorPosition();
        }

        private void updateIndicatorPosition() {
            int i;
            View childAt = getChildAt(this.mSelectedPosition);
            int i2 = -1;
            if (childAt == null || childAt.getWidth() <= 0) {
                i = -1;
            } else {
                int left = childAt.getLeft();
                i = childAt.getRight();
                boolean z = false;
                if (!isRtl() ? this.mSelectedPosition < getChildCount() - 1 : this.mSelectedPosition > 0) {
                    z = true;
                }
                if (this.mSelectionOffset > 0.0f && z) {
                    int i3 = this.mSelectedPosition;
                    if (!isRtl()) {
                        i2 = 1;
                    }
                    View childAt2 = getChildAt(i3 + i2);
                    float left2 = this.mSelectionOffset * ((float) childAt2.getLeft());
                    float f = this.mSelectionOffset;
                    left = (int) (left2 + ((1.0f - f) * ((float) left)));
                    i = (int) ((f * ((float) childAt2.getRight())) + ((1.0f - this.mSelectionOffset) * ((float) i)));
                }
                i2 = left;
            }
            setIndicatorPosition(i2, i);
        }

        private boolean isRtl() {
            return getLayoutDirection() == 1;
        }

        private void setIndicatorPosition(int i, int i2) {
            if (i != this.mIndicatorLeft || i2 != this.mIndicatorRight) {
                this.mIndicatorLeft = i;
                this.mIndicatorRight = i2;
                postInvalidateOnAnimation();
            }
        }

        public void draw(Canvas canvas) {
            super.draw(canvas);
            int i = this.mIndicatorLeft;
            if (i >= 0 && this.mIndicatorRight > i) {
                canvas.drawRect((float) i, (float) (getHeight() - this.mSelectedIndicatorHeight), (float) this.mIndicatorRight, (float) getHeight(), this.mSelectedIndicatorPaint);
            }
        }
    }

    public OPTabLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.OPTabLayoutStyle);
    }

    public OPTabLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mTabs = new ArrayList<>();
        this.mTabMaxWidth = Integer.MAX_VALUE;
        this.fast_out_slow_in_interpolator = AnimationUtils.loadInterpolator(context, 17563661);
        setHorizontalScrollBarEnabled(false);
        setFillViewport(true);
        this.mTabStrip = new SlidingTabStrip(context);
        addView(this.mTabStrip, -2, -1);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPTabLayout, i, R$style.Oneplus_Widget_Design_OPTabLayout);
        this.mTabStrip.setSelectedIndicatorHeight(obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPTabLayout_op_tabIndicatorHeight, 0));
        this.mTabStrip.setSelectedIndicatorColor(obtainStyledAttributes.getColor(R$styleable.OPTabLayout_op_tabIndicatorColor, 0));
        this.mTabTextAppearance = obtainStyledAttributes.getResourceId(R$styleable.OPTabLayout_op_tabTextAppearance, R$style.Oneplus_TextAppearance_Design_Tab);
        int dimensionPixelSize = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPTabLayout_op_tabPadding, 0);
        this.mTabPaddingBottom = dimensionPixelSize;
        this.mTabPaddingEnd = dimensionPixelSize;
        this.mTabPaddingTop = dimensionPixelSize;
        this.mTabPaddingStart = dimensionPixelSize;
        this.mTabPaddingStart = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPTabLayout_op_tabPaddingStart, this.mTabPaddingStart);
        this.mTabPaddingTop = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPTabLayout_op_tabPaddingTop, this.mTabPaddingTop);
        this.mTabPaddingEnd = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPTabLayout_op_tabPaddingEnd, this.mTabPaddingEnd);
        this.mTabPaddingBottom = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPTabLayout_op_tabPaddingBottom, this.mTabPaddingBottom);
        this.mTabTextColors = loadTextColorFromTextAppearance(this.mTabTextAppearance);
        if (obtainStyledAttributes.hasValue(R$styleable.OPTabLayout_op_tabTextColor)) {
            this.mTabTextColors = obtainStyledAttributes.getColorStateList(R$styleable.OPTabLayout_op_tabTextColor);
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPTabLayout_op_tabSelectedTextColor)) {
            this.mTabTextColors = createColorStateList(this.mTabTextColors.getDefaultColor(), obtainStyledAttributes.getColor(R$styleable.OPTabLayout_op_tabSelectedTextColor, 0));
        }
        this.mTabMinWidth = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPTabLayout_op_tabMinWidth, 0);
        this.mRequestedTabMaxWidth = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPTabLayout_op_tabMaxWidth, 0);
        this.mTabBackgroundResId = obtainStyledAttributes.getResourceId(R$styleable.OPTabLayout_op_tabBackground, 0);
        this.mContentInsetStart = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPTabLayout_op_tabContentStart, 0);
        this.mTabHorizontalSpacing = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPTabLayout_op_horizontalSpacing, 0);
        this.mMode = obtainStyledAttributes.getInt(R$styleable.OPTabLayout_op_tabMode, 1);
        this.mTabGravity = obtainStyledAttributes.getInt(R$styleable.OPTabLayout_op_tabGravity, 0);
        obtainStyledAttributes.recycle();
        applyModeAndGravity();
    }

    public int getTabCount() {
        return this.mTabs.size();
    }

    private void updateTabViewLayoutParams(LayoutParams layoutParams) {
        if (this.mMode == 1 && this.mTabGravity == 0) {
            layoutParams.width = 0;
            layoutParams.weight = 1.0f;
            return;
        }
        layoutParams.width = -2;
        layoutParams.weight = 0.0f;
    }

    /* access modifiers changed from: private */
    public int dpToPx(int i) {
        return Math.round(getResources().getDisplayMetrics().density * ((float) i));
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int dimensionPixelSize = getContext().getResources().getDimensionPixelSize(R$dimen.tab_layout_default_height_material) + getPaddingTop() + getPaddingBottom();
        int mode = MeasureSpec.getMode(i2);
        if (mode == Integer.MIN_VALUE) {
            i2 = MeasureSpec.makeMeasureSpec(Math.min(dimensionPixelSize, MeasureSpec.getSize(i2)), 1073741824);
        } else if (mode == 0) {
            i2 = MeasureSpec.makeMeasureSpec(dimensionPixelSize, 1073741824);
        }
        super.onMeasure(i, i2);
        if (this.mMode == 1 && getChildCount() == 1) {
            View childAt = getChildAt(0);
            int measuredWidth = getMeasuredWidth();
            if (childAt.getMeasuredWidth() > measuredWidth) {
                childAt.measure(MeasureSpec.makeMeasureSpec(measuredWidth, 1073741824), HorizontalScrollView.getChildMeasureSpec(i2, getPaddingTop() + getPaddingBottom(), childAt.getLayoutParams().height));
            }
        }
        int i3 = this.mRequestedTabMaxWidth;
        int measuredWidth2 = getMeasuredWidth() - getDefaultMaxWidth();
        if (i3 == 0 || i3 > measuredWidth2) {
            i3 = measuredWidth2;
        }
        if (this.mTabMaxWidth != i3) {
            this.mTabMaxWidth = i3;
            super.onMeasure(i, i2);
        }
    }

    private int getDefaultMaxWidth() {
        if (getTabCount() == 0) {
            return this.mRequestedTabMaxWidth;
        }
        return getMeasuredWidth() / getTabCount();
    }

    private void applyModeAndGravity() {
        this.mTabStrip.setPaddingRelative(this.mMode == 0 ? this.mContentInsetStart : 0, 0, 0, 0);
        int i = this.mMode;
        if (i == 0) {
            this.mTabStrip.setGravity(8388611);
        } else if (i == 1) {
            this.mTabStrip.setGravity(1);
        }
        updateTabViewsLayoutParams();
    }

    /* access modifiers changed from: private */
    public void updateTabViewsLayoutParams() {
        for (int i = 0; i < this.mTabStrip.getChildCount(); i++) {
            View childAt = this.mTabStrip.getChildAt(i);
            updateTabViewLayoutParams((LayoutParams) childAt.getLayoutParams());
            childAt.requestLayout();
        }
    }

    private static ColorStateList createColorStateList(int i, int i2) {
        return new ColorStateList(new int[][]{HorizontalScrollView.SELECTED_STATE_SET, HorizontalScrollView.EMPTY_STATE_SET}, new int[]{i2, i});
    }

    private ColorStateList loadTextColorFromTextAppearance(int i) {
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(i, R$styleable.TextAppearance);
        try {
            return obtainStyledAttributes.getColorStateList(R$styleable.TextAppearance_android_textColor);
        } finally {
            obtainStyledAttributes.recycle();
        }
    }
}

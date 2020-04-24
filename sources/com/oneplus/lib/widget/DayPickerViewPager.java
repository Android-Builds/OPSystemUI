package com.oneplus.lib.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import com.oneplus.support.viewpager.widget.ViewPager;
import com.oneplus.support.viewpager.widget.ViewPager.LayoutParams;
import java.util.ArrayList;

class DayPickerViewPager extends ViewPager {
    private final ArrayList<View> mMatchParentChildren;

    public DayPickerViewPager(Context context) {
        this(context, null);
    }

    public DayPickerViewPager(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public DayPickerViewPager(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public DayPickerViewPager(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mMatchParentChildren = new ArrayList<>(1);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        int i4;
        populate();
        int childCount = getChildCount();
        boolean z = (MeasureSpec.getMode(i) == 1073741824 && MeasureSpec.getMode(i2) == 1073741824) ? false : true;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        for (int i8 = 0; i8 < childCount; i8++) {
            View childAt = getChildAt(i8);
            if (childAt.getVisibility() != 8) {
                measureChild(childAt, i, i2);
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                i5 = Math.max(i5, childAt.getMeasuredWidth());
                i6 = Math.max(i6, childAt.getMeasuredHeight());
                i7 = ViewGroup.combineMeasuredStates(i7, childAt.getMeasuredState());
                if (z && (layoutParams.width == -1 || layoutParams.height == -1)) {
                    this.mMatchParentChildren.add(childAt);
                }
            }
        }
        int paddingLeft = i5 + getPaddingLeft() + getPaddingRight();
        int max = Math.max(i6 + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight());
        int max2 = Math.max(paddingLeft, getSuggestedMinimumWidth());
        if (VERSION.SDK_INT >= 23) {
            Drawable foreground = getForeground();
            if (foreground != null) {
                max = Math.max(max, foreground.getMinimumHeight());
                max2 = Math.max(max2, foreground.getMinimumWidth());
            }
        }
        setMeasuredDimension(ViewGroup.resolveSizeAndState(max2, i, i7), ViewGroup.resolveSizeAndState(max, i2, i7 << 16));
        int size = this.mMatchParentChildren.size();
        if (size > 1) {
            for (int i9 = 0; i9 < size; i9++) {
                View view = (View) this.mMatchParentChildren.get(i9);
                LayoutParams layoutParams2 = (LayoutParams) view.getLayoutParams();
                if (layoutParams2.width == -1) {
                    i3 = MeasureSpec.makeMeasureSpec((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight(), 1073741824);
                } else {
                    i3 = ViewGroup.getChildMeasureSpec(i, getPaddingLeft() + getPaddingRight(), layoutParams2.width);
                }
                if (layoutParams2.height == -1) {
                    i4 = MeasureSpec.makeMeasureSpec((getMeasuredHeight() - getPaddingTop()) - getPaddingBottom(), 1073741824);
                } else {
                    i4 = ViewGroup.getChildMeasureSpec(i2, getPaddingTop() + getPaddingBottom(), layoutParams2.height);
                }
                view.measure(i3, i4);
            }
        }
        this.mMatchParentChildren.clear();
    }
}

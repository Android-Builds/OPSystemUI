package com.oneplus.lib.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ViewAnimator;
import java.util.ArrayList;

public class DialogViewAnimator extends ViewAnimator {
    private final ArrayList<View> mMatchParentChildren = new ArrayList<>(1);

    public DialogViewAnimator(Context context) {
        super(context);
    }

    public DialogViewAnimator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        int i4;
        int i5;
        int i6;
        int i7 = i;
        int i8 = i2;
        boolean z = (MeasureSpec.getMode(i) == 1073741824 && MeasureSpec.getMode(i2) == 1073741824) ? false : true;
        int childCount = getChildCount();
        int i9 = 0;
        int i10 = 0;
        int i11 = 0;
        for (int i12 = 0; i12 < childCount; i12++) {
            View childAt = getChildAt(i12);
            if (getMeasureAllChildren() || childAt.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                boolean z2 = layoutParams.width == -1;
                boolean z3 = layoutParams.height == -1;
                if (z && (z2 || z3)) {
                    this.mMatchParentChildren.add(childAt);
                }
                LayoutParams layoutParams2 = layoutParams;
                View view = childAt;
                int i13 = i9;
                int i14 = i10;
                measureChildWithMargins(childAt, i, 0, i2, 0);
                if (!z || z2) {
                    i5 = 0;
                } else {
                    i11 = Math.max(i11, view.getMeasuredWidth() + layoutParams2.leftMargin + layoutParams2.rightMargin);
                    i5 = (view.getMeasuredWidthAndState() & -16777216) | 0;
                }
                if (!z || z3) {
                    i6 = i14;
                } else {
                    i5 |= (view.getMeasuredHeightAndState() >> 16) & -256;
                    i6 = Math.max(i14, view.getMeasuredHeight() + layoutParams2.topMargin + layoutParams2.bottomMargin);
                }
                i9 = ViewAnimator.combineMeasuredStates(i13, i5);
                i10 = i6;
            }
        }
        int i15 = i9;
        int paddingLeft = i11 + getPaddingLeft() + getPaddingRight();
        int max = Math.max(i10 + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight());
        int max2 = Math.max(paddingLeft, getSuggestedMinimumWidth());
        Drawable foreground = getForeground();
        if (foreground != null) {
            max = Math.max(max, foreground.getMinimumHeight());
            max2 = Math.max(max2, foreground.getMinimumWidth());
        }
        setMeasuredDimension(ViewAnimator.resolveSizeAndState(max2, i7, i15), ViewAnimator.resolveSizeAndState(max, i8, i15 << 16));
        int size = this.mMatchParentChildren.size();
        for (int i16 = 0; i16 < size; i16++) {
            View view2 = (View) this.mMatchParentChildren.get(i16);
            MarginLayoutParams marginLayoutParams = (MarginLayoutParams) view2.getLayoutParams();
            if (marginLayoutParams.width == -1) {
                i3 = MeasureSpec.makeMeasureSpec((((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight()) - marginLayoutParams.leftMargin) - marginLayoutParams.rightMargin, 1073741824);
            } else {
                i3 = ViewAnimator.getChildMeasureSpec(i7, getPaddingLeft() + getPaddingRight() + marginLayoutParams.leftMargin + marginLayoutParams.rightMargin, marginLayoutParams.width);
            }
            if (marginLayoutParams.height == -1) {
                i4 = MeasureSpec.makeMeasureSpec((((getMeasuredHeight() - getPaddingTop()) - getPaddingBottom()) - marginLayoutParams.topMargin) - marginLayoutParams.bottomMargin, 1073741824);
            } else {
                i4 = ViewAnimator.getChildMeasureSpec(i8, getPaddingTop() + getPaddingBottom() + marginLayoutParams.topMargin + marginLayoutParams.bottomMargin, marginLayoutParams.height);
            }
            view2.measure(i3, i4);
        }
        this.mMatchParentChildren.clear();
    }
}

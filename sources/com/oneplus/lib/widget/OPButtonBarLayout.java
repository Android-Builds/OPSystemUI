package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$styleable;

public class OPButtonBarLayout extends LinearLayout {
    private boolean mAllowStacking;
    private int mLastWidthSize = -1;

    public OPButtonBarLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPButtonBarLayout);
        this.mAllowStacking = obtainStyledAttributes.getBoolean(R$styleable.OPButtonBarLayout_op_allowStacking, true);
        obtainStyledAttributes.recycle();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int i3;
        int size = MeasureSpec.getSize(i);
        boolean z = false;
        if (this.mAllowStacking) {
            if (size > this.mLastWidthSize && isStacked()) {
                setStacked(false);
            }
            this.mLastWidthSize = size;
        }
        if (isStacked() || MeasureSpec.getMode(i) != 1073741824) {
            i3 = i;
        } else {
            i3 = MeasureSpec.makeMeasureSpec(size, Integer.MIN_VALUE);
            z = true;
        }
        super.onMeasure(i3, i2);
        if (this.mAllowStacking && !isStacked() && (getMeasuredWidthAndState() & -16777216) == 16777216) {
            setStacked(true);
            z = true;
        }
        if (z) {
            super.onMeasure(i, i2);
        }
    }

    private void setStacked(boolean z) {
        setOrientation(z ? 1 : 0);
        setGravity(z ? 5 : 80);
        View findViewById = findViewById(R$id.spacer);
        int i = 4;
        if (findViewById != null) {
            findViewById.setVisibility(4);
        }
        View findViewById2 = findViewById(R$id.spacer2);
        if (findViewById2 != null) {
            if (z) {
                i = 8;
            }
            findViewById2.setVisibility(i);
        }
    }

    private boolean isStacked() {
        return getOrientation() == 1;
    }
}

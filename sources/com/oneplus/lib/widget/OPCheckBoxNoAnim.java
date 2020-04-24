package com.oneplus.lib.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class OPCheckBoxNoAnim extends ImageView {
    private boolean mChecked;
    private int mCheckedResId;
    private int mIntrinsicWidth;
    private int mUnCheckedResId;

    private void init() {
    }

    public void setImageResource(int i) {
    }

    public OPCheckBoxNoAnim(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public OPCheckBoxNoAnim(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIntrinsicWidth = 0;
        this.mCheckedResId = 0;
        this.mUnCheckedResId = 0;
        this.mChecked = false;
        init();
    }
}

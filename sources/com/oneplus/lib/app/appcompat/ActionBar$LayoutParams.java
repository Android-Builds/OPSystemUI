package com.oneplus.lib.app.appcompat;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup.MarginLayoutParams;
import com.oneplus.commonctrl.R$styleable;

public class ActionBar$LayoutParams extends MarginLayoutParams {
    public int gravity = 0;

    public ActionBar$LayoutParams(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPActionBarLayout);
        this.gravity = obtainStyledAttributes.getInt(R$styleable.OPActionBarLayout_android_layout_gravity, 0);
        obtainStyledAttributes.recycle();
    }
}

package com.oneplus.lib.design.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.support.core.view.ViewCompat;

class BaseTransientBottomBar$SnackbarBaseLayout extends FrameLayout {
    private BaseTransientBottomBar$OnAttachStateChangeListener mOnAttachStateChangeListener;
    private BaseTransientBottomBar$OnLayoutChangeListener mOnLayoutChangeListener;

    BaseTransientBottomBar$SnackbarBaseLayout(Context context) {
        this(context, null);
    }

    BaseTransientBottomBar$SnackbarBaseLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.SnackbarLayout);
        if (obtainStyledAttributes.hasValue(R$styleable.SnackbarLayout_op_elevation)) {
            ViewCompat.setElevation(this, (float) obtainStyledAttributes.getDimensionPixelSize(R$styleable.SnackbarLayout_op_elevation, 0));
        }
        obtainStyledAttributes.recycle();
        setClickable(true);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        BaseTransientBottomBar$OnLayoutChangeListener baseTransientBottomBar$OnLayoutChangeListener = this.mOnLayoutChangeListener;
        if (baseTransientBottomBar$OnLayoutChangeListener != null) {
            baseTransientBottomBar$OnLayoutChangeListener.onLayoutChange(this, i, i2, i3, i4);
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        BaseTransientBottomBar$OnAttachStateChangeListener baseTransientBottomBar$OnAttachStateChangeListener = this.mOnAttachStateChangeListener;
        if (baseTransientBottomBar$OnAttachStateChangeListener != null) {
            baseTransientBottomBar$OnAttachStateChangeListener.onViewAttachedToWindow(this);
        }
        ViewCompat.requestApplyInsets(this);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        BaseTransientBottomBar$OnAttachStateChangeListener baseTransientBottomBar$OnAttachStateChangeListener = this.mOnAttachStateChangeListener;
        if (baseTransientBottomBar$OnAttachStateChangeListener != null) {
            baseTransientBottomBar$OnAttachStateChangeListener.onViewDetachedFromWindow(this);
        }
    }
}

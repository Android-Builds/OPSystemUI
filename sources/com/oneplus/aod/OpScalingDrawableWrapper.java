package com.oneplus.aod;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;

class OpScalingDrawableWrapper extends DrawableWrapper {
    private float mScaleFactor;

    public OpScalingDrawableWrapper(Drawable drawable, float f) {
        super(drawable);
        this.mScaleFactor = f;
    }

    public int getIntrinsicWidth() {
        return (int) (((float) super.getIntrinsicWidth()) * this.mScaleFactor);
    }

    public int getIntrinsicHeight() {
        return (int) (((float) super.getIntrinsicHeight()) * this.mScaleFactor);
    }
}

package com.oneplus.lib.app.appcompat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class TintTypedArray {
    private final Context mContext;
    private final TypedArray mWrapped;

    public static TintTypedArray obtainStyledAttributes(Context context, AttributeSet attributeSet, int[] iArr) {
        return new TintTypedArray(context, context.obtainStyledAttributes(attributeSet, iArr));
    }

    public static TintTypedArray obtainStyledAttributes(Context context, AttributeSet attributeSet, int[] iArr, int i, int i2) {
        return new TintTypedArray(context, context.obtainStyledAttributes(attributeSet, iArr, i, i2));
    }

    private TintTypedArray(Context context, TypedArray typedArray) {
        this.mContext = context;
        this.mWrapped = typedArray;
    }

    public Drawable getDrawable(int i) {
        if (this.mWrapped.hasValue(i)) {
            int resourceId = this.mWrapped.getResourceId(i, 0);
            if (resourceId != 0) {
                return AppCompatResources.getDrawable(this.mContext, resourceId);
            }
        }
        return this.mWrapped.getDrawable(i);
    }

    public CharSequence getText(int i) {
        return this.mWrapped.getText(i);
    }

    public boolean getBoolean(int i, boolean z) {
        return this.mWrapped.getBoolean(i, z);
    }

    public int getInt(int i, int i2) {
        return this.mWrapped.getInt(i, i2);
    }

    public int getColor(int i, int i2) {
        return this.mWrapped.getColor(i, i2);
    }

    public ColorStateList getColorStateList(int i) {
        if (this.mWrapped.hasValue(i)) {
            int resourceId = this.mWrapped.getResourceId(i, 0);
            if (resourceId != 0) {
                ColorStateList colorStateList = AppCompatResources.getColorStateList(this.mContext, resourceId);
                if (colorStateList != null) {
                    return colorStateList;
                }
            }
        }
        return this.mWrapped.getColorStateList(i);
    }

    public int getInteger(int i, int i2) {
        return this.mWrapped.getInteger(i, i2);
    }

    public int getDimensionPixelOffset(int i, int i2) {
        return this.mWrapped.getDimensionPixelOffset(i, i2);
    }

    public int getDimensionPixelSize(int i, int i2) {
        return this.mWrapped.getDimensionPixelSize(i, i2);
    }

    public int getLayoutDimension(int i, int i2) {
        return this.mWrapped.getLayoutDimension(i, i2);
    }

    public int getResourceId(int i, int i2) {
        return this.mWrapped.getResourceId(i, i2);
    }

    public boolean hasValue(int i) {
        return this.mWrapped.hasValue(i);
    }

    public void recycle() {
        this.mWrapped.recycle();
    }
}

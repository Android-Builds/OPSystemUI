package com.oneplus.lib.widget.cardview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.widget.FrameLayout;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;

public class CardView extends FrameLayout implements CardViewDelegate {
    private static final CardViewImpl IMPL = new CardViewApi21();
    private boolean mCompatPadding;
    private final Rect mContentPadding = new Rect();
    private boolean mPreventCornerOverlap;
    private final Rect mShadowBounds = new Rect();

    public void setPadding(int i, int i2, int i3, int i4) {
    }

    public void setPaddingRelative(int i, int i2, int i3, int i4) {
    }

    static {
        IMPL.initStatic();
    }

    public CardView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize(context, attributeSet, 0);
    }

    public boolean getUseCompatPadding() {
        return this.mCompatPadding;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        if (!(IMPL instanceof CardViewApi21)) {
            int mode = MeasureSpec.getMode(i);
            if (mode == Integer.MIN_VALUE || mode == 1073741824) {
                i = MeasureSpec.makeMeasureSpec(Math.max((int) Math.ceil((double) IMPL.getMinWidth(this)), MeasureSpec.getSize(i)), mode);
            }
            int mode2 = MeasureSpec.getMode(i2);
            if (mode2 == Integer.MIN_VALUE || mode2 == 1073741824) {
                i2 = MeasureSpec.makeMeasureSpec(Math.max((int) Math.ceil((double) IMPL.getMinHeight(this)), MeasureSpec.getSize(i2)), mode2);
            }
            super.onMeasure(i, i2);
            return;
        }
        super.onMeasure(i, i2);
    }

    private void initialize(Context context, AttributeSet attributeSet, int i) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.CardView, i, R$style.CardView_Light);
        int color = obtainStyledAttributes.getColor(R$styleable.CardView_cardBackgroundColor, 0);
        float dimension = obtainStyledAttributes.getDimension(R$styleable.CardView_cardCornerRadius, 0.0f);
        float dimension2 = obtainStyledAttributes.getDimension(R$styleable.CardView_cardElevation, 0.0f);
        float dimension3 = obtainStyledAttributes.getDimension(R$styleable.CardView_cardMaxElevation, 0.0f);
        this.mCompatPadding = obtainStyledAttributes.getBoolean(R$styleable.CardView_cardUseCompatPadding, false);
        this.mPreventCornerOverlap = obtainStyledAttributes.getBoolean(R$styleable.CardView_cardPreventCornerOverlap, true);
        int dimensionPixelSize = obtainStyledAttributes.getDimensionPixelSize(R$styleable.CardView_contentPadding, 0);
        this.mContentPadding.left = obtainStyledAttributes.getDimensionPixelSize(R$styleable.CardView_contentPaddingLeft, dimensionPixelSize);
        this.mContentPadding.top = obtainStyledAttributes.getDimensionPixelSize(R$styleable.CardView_contentPaddingTop, dimensionPixelSize);
        this.mContentPadding.right = obtainStyledAttributes.getDimensionPixelSize(R$styleable.CardView_contentPaddingRight, dimensionPixelSize);
        this.mContentPadding.bottom = obtainStyledAttributes.getDimensionPixelSize(R$styleable.CardView_contentPaddingBottom, dimensionPixelSize);
        float f = dimension2 > dimension3 ? dimension2 : dimension3;
        obtainStyledAttributes.recycle();
        IMPL.initialize(this, context, color, dimension, dimension2, f);
    }

    public void setCardBackgroundColor(int i) {
        IMPL.setBackgroundColor(this, i);
    }

    public void setShadowPadding(int i, int i2, int i3, int i4) {
        this.mShadowBounds.set(i, i2, i3, i4);
        Rect rect = this.mContentPadding;
        super.setPadding(i + rect.left, i2 + rect.top, i3 + rect.right, i4 + rect.bottom);
    }

    public boolean getPreventCornerOverlap() {
        return this.mPreventCornerOverlap;
    }
}

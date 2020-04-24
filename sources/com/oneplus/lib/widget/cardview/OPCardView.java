package com.oneplus.lib.widget.cardview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;

public class OPCardView extends CardView {
    private int mBackgroundColor;
    private int mBackgroundColorMask;
    Paint mCardBackgroundMaskPaint;
    private boolean mIsCardSelected;

    public OPCardView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize(context, attributeSet, 0);
    }

    private void initialize(Context context, AttributeSet attributeSet, int i) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.CardView, i, R$style.Oneplus_CardView_Light);
        this.mBackgroundColor = obtainStyledAttributes.getColor(R$styleable.CardView_cardBackgroundColor, 0);
        this.mBackgroundColorMask = obtainStyledAttributes.getColor(R$styleable.CardView_cardBackgroundColorMask, 0);
        obtainStyledAttributes.recycle();
        setCardBackgroundColor(this.mBackgroundColor);
        this.mCardBackgroundMaskPaint = new Paint();
        this.mCardBackgroundMaskPaint.setColor(this.mBackgroundColorMask);
    }

    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mCardBackgroundMaskPaint != null && this.mIsCardSelected) {
            canvas.drawRect(0.0f, 0.0f, (float) getWidth(), (float) getHeight(), this.mCardBackgroundMaskPaint);
        }
    }
}

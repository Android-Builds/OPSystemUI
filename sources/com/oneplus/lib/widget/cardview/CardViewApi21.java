package com.oneplus.lib.widget.cardview;

import android.content.Context;
import android.view.View;

class CardViewApi21 implements CardViewImpl {
    public void initStatic() {
    }

    CardViewApi21() {
    }

    public void initialize(CardViewDelegate cardViewDelegate, Context context, int i, float f, float f2, float f3) {
        cardViewDelegate.setBackgroundDrawable(new RoundRectDrawable(i, f));
        View view = (View) cardViewDelegate;
        view.setClipToOutline(true);
        view.setElevation(f2);
        setMaxElevation(cardViewDelegate, f3);
    }

    public void setMaxElevation(CardViewDelegate cardViewDelegate, float f) {
        ((RoundRectDrawable) cardViewDelegate.getBackground()).setPadding(f, cardViewDelegate.getUseCompatPadding(), cardViewDelegate.getPreventCornerOverlap());
        updatePadding(cardViewDelegate);
    }

    public float getMaxElevation(CardViewDelegate cardViewDelegate) {
        return ((RoundRectDrawable) cardViewDelegate.getBackground()).getPadding();
    }

    public float getMinWidth(CardViewDelegate cardViewDelegate) {
        return getRadius(cardViewDelegate) * 2.0f;
    }

    public float getMinHeight(CardViewDelegate cardViewDelegate) {
        return getRadius(cardViewDelegate) * 2.0f;
    }

    public float getRadius(CardViewDelegate cardViewDelegate) {
        return ((RoundRectDrawable) cardViewDelegate.getBackground()).getRadius();
    }

    public void updatePadding(CardViewDelegate cardViewDelegate) {
        if (!cardViewDelegate.getUseCompatPadding()) {
            cardViewDelegate.setShadowPadding(0, 0, 0, 0);
            return;
        }
        float maxElevation = getMaxElevation(cardViewDelegate);
        float radius = getRadius(cardViewDelegate);
        int ceil = (int) Math.ceil((double) RoundRectDrawableWithShadow.calculateHorizontalPadding(maxElevation, radius, cardViewDelegate.getPreventCornerOverlap()));
        int ceil2 = (int) Math.ceil((double) RoundRectDrawableWithShadow.calculateVerticalPadding(maxElevation, radius, cardViewDelegate.getPreventCornerOverlap()));
        cardViewDelegate.setShadowPadding(ceil, ceil2, ceil, ceil2);
    }

    public void setBackgroundColor(CardViewDelegate cardViewDelegate, int i) {
        ((RoundRectDrawable) cardViewDelegate.getBackground()).setColor(i);
    }
}

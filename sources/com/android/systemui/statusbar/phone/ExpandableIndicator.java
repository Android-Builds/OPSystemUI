package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;

public class ExpandableIndicator extends ImageView {
    private boolean mExpanded;
    private boolean mIsDefaultDirection = true;

    public ExpandableIndicator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        updateIndicatorDrawable();
        setContentDescription(getContentDescription(this.mExpanded));
    }

    public void setExpanded(boolean z) {
        if (z != this.mExpanded) {
            this.mExpanded = z;
            AnimatedVectorDrawable animatedVectorDrawable = (AnimatedVectorDrawable) getContext().getDrawable(getDrawableResourceId(!this.mExpanded)).getConstantState().newDrawable();
            setImageDrawable(animatedVectorDrawable);
            animatedVectorDrawable.forceAnimationOnUI();
            animatedVectorDrawable.start();
            setContentDescription(getContentDescription(z));
        }
    }

    private int getDrawableResourceId(boolean z) {
        int i;
        int i2;
        if (this.mIsDefaultDirection) {
            if (z) {
                i2 = R$drawable.ic_volume_collapse_animation;
            } else {
                i2 = R$drawable.ic_volume_expand_animation;
            }
            return i2;
        }
        if (z) {
            i = R$drawable.ic_volume_expand_animation;
        } else {
            i = R$drawable.ic_volume_collapse_animation;
        }
        return i;
    }

    private String getContentDescription(boolean z) {
        if (z) {
            return this.mContext.getString(R$string.accessibility_quick_settings_collapse);
        }
        return this.mContext.getString(R$string.accessibility_quick_settings_expand);
    }

    private void updateIndicatorDrawable() {
        setImageResource(getDrawableResourceId(this.mExpanded));
    }
}

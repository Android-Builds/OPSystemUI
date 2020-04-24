package com.oneplus.lib.app.appcompat;

import android.graphics.Outline;
import android.graphics.drawable.Drawable;

class ActionBarBackgroundDrawableV21 extends ActionBarBackgroundDrawable {
    public ActionBarBackgroundDrawableV21(ActionBarContainer actionBarContainer) {
        super(actionBarContainer);
    }

    public void getOutline(Outline outline) {
        ActionBarContainer actionBarContainer = this.mContainer;
        if (actionBarContainer.mIsSplit) {
            Drawable drawable = actionBarContainer.mSplitBackground;
            if (drawable != null) {
                drawable.getOutline(outline);
                return;
            }
            return;
        }
        Drawable drawable2 = actionBarContainer.mBackground;
        if (drawable2 != null) {
            drawable2.getOutline(outline);
        }
    }
}

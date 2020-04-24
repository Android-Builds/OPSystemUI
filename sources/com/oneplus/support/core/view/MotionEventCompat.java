package com.oneplus.support.core.view;

import android.view.MotionEvent;

public final class MotionEventCompat {
    @Deprecated
    public static int getActionMasked(MotionEvent motionEvent) {
        return motionEvent.getActionMasked();
    }
}

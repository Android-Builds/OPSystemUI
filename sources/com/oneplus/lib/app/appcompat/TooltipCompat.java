package com.oneplus.lib.app.appcompat;

import android.view.View;

public class TooltipCompat {
    public static void setTooltipText(View view, CharSequence charSequence) {
        TooltipCompatHandler.setTooltipText(view, charSequence);
    }
}

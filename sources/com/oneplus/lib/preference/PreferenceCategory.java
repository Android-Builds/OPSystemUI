package com.oneplus.lib.preference;

import android.content.Context;
import android.util.AttributeSet;
import com.oneplus.commonctrl.R$attr;

public class PreferenceCategory extends PreferenceGroup {
    public boolean isEnabled() {
        return false;
    }

    public PreferenceCategory(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public PreferenceCategory(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public PreferenceCategory(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_preferenceCategoryStyle);
    }

    public boolean shouldDisableDependents() {
        return !super.isEnabled();
    }
}

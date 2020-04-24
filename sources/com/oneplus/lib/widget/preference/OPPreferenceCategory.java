package com.oneplus.lib.widget.preference;

import android.content.Context;
import android.util.AttributeSet;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$style;
import com.oneplus.lib.preference.PreferenceCategory;
import com.oneplus.lib.widget.util.C1963utils;

public class OPPreferenceCategory extends PreferenceCategory {
    public OPPreferenceCategory(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_preferenceCategoryStyle);
    }

    public OPPreferenceCategory(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.Oneplus_DeviceDefault_Preference_Material_Category);
    }

    public OPPreferenceCategory(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
    }
}

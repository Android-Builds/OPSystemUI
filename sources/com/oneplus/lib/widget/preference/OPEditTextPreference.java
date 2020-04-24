package com.oneplus.lib.widget.preference;

import android.content.Context;
import android.util.AttributeSet;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$style;
import com.oneplus.lib.preference.EditTextPreference;
import com.oneplus.lib.widget.util.C1963utils;

public class OPEditTextPreference extends EditTextPreference {
    public OPEditTextPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_editTextPreferenceStyle);
    }

    public OPEditTextPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.f107xb637d887);
    }

    public OPEditTextPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
    }
}

package com.oneplus.lib.widget.preference;

import android.content.Context;
import android.util.AttributeSet;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$style;
import com.oneplus.lib.preference.DialogPreference;
import com.oneplus.lib.widget.util.C1963utils;

public class OPDialogPreference extends DialogPreference {
    public OPDialogPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_dialogPreferenceStyle);
    }

    public OPDialogPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.OnePlus_DeviceDefault_Preference_Material_DialogPreference);
    }

    public OPDialogPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
    }
}

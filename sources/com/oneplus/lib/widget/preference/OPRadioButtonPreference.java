package com.oneplus.lib.widget.preference;

import android.content.Context;
import android.util.AttributeSet;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$style;
import com.oneplus.lib.preference.CheckBoxPreference;
import com.oneplus.lib.preference.Preference;
import com.oneplus.lib.widget.util.C1963utils;
import java.lang.reflect.Field;

public class OPRadioButtonPreference extends CheckBoxPreference {
    public OPRadioButtonPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_radioButtonPreferenceStyle);
    }

    public OPRadioButtonPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.OnePlus_DeviceDefault_Preference_Material_RadioButtonPreference);
    }

    public OPRadioButtonPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
        setCanRecycleLayout(true);
    }

    private void setCanRecycleLayout(boolean z) {
        try {
            Field declaredField = Preference.class.getDeclaredField("mCanRecycleLayout");
            declaredField.setAccessible(true);
            declaredField.setBoolean(this, z);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        }
    }
}

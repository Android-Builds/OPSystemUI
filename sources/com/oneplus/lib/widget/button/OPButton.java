package com.oneplus.lib.widget.button;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RemoteViews.RemoteView;
import android.widget.TextView;
import com.oneplus.commonctrl.R$style;
import com.oneplus.lib.widget.util.C1963utils;

@RemoteView
public class OPButton extends TextView {
    public OPButton(Context context) {
        this(context, null);
    }

    public OPButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842824);
    }

    public OPButton(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.OnePlus_DeviceDefault_Widget_Material_Button);
    }

    public OPButton(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
    }

    public CharSequence getAccessibilityClassName() {
        return OPButton.class.getName();
    }
}

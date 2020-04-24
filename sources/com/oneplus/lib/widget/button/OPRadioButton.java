package com.oneplus.lib.widget.button;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.oneplus.commonctrl.R$style;
import com.oneplus.lib.widget.util.C1963utils;

public class OPRadioButton extends OPCompoundButton {
    public OPRadioButton(Context context) {
        this(context, null);
    }

    public OPRadioButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842878);
    }

    public OPRadioButton(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.Oneplus_DeviceDefault_Widget_Material_CompoundButton_RadioButton);
    }

    public OPRadioButton(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
    }

    public void toggle() {
        if (!isChecked()) {
            super.toggle();
        }
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setClassName(OPRadioButton.class.getName());
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName(OPRadioButton.class.getName());
    }
}

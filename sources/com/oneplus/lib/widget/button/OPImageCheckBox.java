package com.oneplus.lib.widget.button;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$style;
import com.oneplus.lib.widget.util.C1963utils;

public class OPImageCheckBox extends OPCompoundButton {
    public OPImageCheckBox(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.OPImageCheckboxStyle);
    }

    public OPImageCheckBox(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.f110x174c0b3a);
    }

    public OPImageCheckBox(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setClassName(OPImageCheckBox.class.getName());
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName(OPImageCheckBox.class.getName());
    }
}

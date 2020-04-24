package com.oneplus.lib.widget.button;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.oneplus.commonctrl.R$style;
import com.oneplus.lib.widget.util.C1963utils;

public class OPCheckBox extends OPCompoundButton {
    public OPCheckBox(Context context) {
        this(context, null);
    }

    public OPCheckBox(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842860);
    }

    public OPCheckBox(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.f109xdcb6174f);
    }

    public OPCheckBox(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setClassName(OPCheckBox.class.getName());
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName(OPCheckBox.class.getName());
    }

    public void setChecked(boolean z) {
        setChecked(Boolean.valueOf(z));
    }

    public void setChecked(Boolean bool) {
        setTriStateChecked(bool);
    }
}

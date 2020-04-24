package com.oneplus.custom.utils;

import com.oneplus.custom.utils.OpCustomizeSettings.CUSTOM_TYPE;

public class OpCustomizeSettingsG1 extends OpCustomizeSettings {
    /* access modifiers changed from: protected */
    public CUSTOM_TYPE getCustomization() {
        CUSTOM_TYPE custom_type = CUSTOM_TYPE.NONE;
        int custFlagVal = ParamReader.getCustFlagVal();
        if (custFlagVal == 1) {
            return CUSTOM_TYPE.JCC;
        }
        if (custFlagVal != 2) {
            return custom_type;
        }
        return CUSTOM_TYPE.SW;
    }
}

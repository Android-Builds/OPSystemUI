package com.oneplus.custom.utils;

import com.oneplus.custom.utils.OpCustomizeSettings.CUSTOM_TYPE;

public class OpCustomizeSettingsG2 extends OpCustomizeSettings {
    /* access modifiers changed from: protected */
    public CUSTOM_TYPE getCustomization() {
        CUSTOM_TYPE custom_type = CUSTOM_TYPE.NONE;
        int custFlagVal = ParamReader.getCustFlagVal();
        if (custFlagVal == 3) {
            return CUSTOM_TYPE.AVG;
        }
        if (custFlagVal == 6) {
            return CUSTOM_TYPE.MCL;
        }
        if (custFlagVal != 7) {
            return custom_type;
        }
        return CUSTOM_TYPE.OPR_RETAIL;
    }
}

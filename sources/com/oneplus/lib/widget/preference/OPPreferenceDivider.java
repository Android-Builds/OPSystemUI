package com.oneplus.lib.widget.preference;

import android.content.Context;
import android.util.AttributeSet;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.lib.preference.PreferenceCategory;

public class OPPreferenceDivider extends PreferenceCategory {
    private Context mContext;

    public OPPreferenceDivider(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initViews(context);
    }

    private void initViews(Context context) {
        this.mContext = context;
        setLayoutResource(R$layout.op_ctrl_preference_divider);
    }
}

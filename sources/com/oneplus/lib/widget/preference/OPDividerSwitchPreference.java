package com.oneplus.lib.widget.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$style;
import com.oneplus.lib.preference.SwitchPreference;
import com.oneplus.lib.widget.util.C1963utils;

public class OPDividerSwitchPreference extends SwitchPreference {
    private final Context mContext;

    /* access modifiers changed from: protected */
    public void onClick() {
    }

    public OPDividerSwitchPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_dividerSwitchPreferenceStyle);
    }

    public OPDividerSwitchPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, R$style.f108x2eb4ce41);
    }

    public OPDividerSwitchPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, C1963utils.resolveDefStyleAttr(context, i), i2);
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void onBindView(View view) {
        super.onBindView(view);
        View findViewById = view.findViewById(R$id.left_layout);
        View findViewById2 = view.findViewById(R$id.right_layout);
        if (!isSummaryEmpty()) {
            if (findViewById != null) {
                View findViewById3 = view.findViewById(R$id.top_space);
                LayoutParams layoutParams = (LayoutParams) findViewById.getLayoutParams();
                if (layoutParams != null) {
                    if (findViewById3 != null) {
                        findViewById3.setVisibility(0);
                    }
                    layoutParams.height = -2;
                    findViewById.setPadding(0, 0, 0, this.mContext.getResources().getDimensionPixelOffset(R$dimen.op_preference_divider_switch_bottom_padding));
                    findViewById.setLayoutParams(layoutParams);
                }
            }
        } else if (findViewById2 != null) {
            LayoutParams layoutParams2 = (LayoutParams) findViewById2.getLayoutParams();
            if (layoutParams2 != null) {
                layoutParams2.gravity = 16;
                layoutParams2.topMargin = 0;
                findViewById2.setLayoutParams(layoutParams2);
            }
        }
    }
}

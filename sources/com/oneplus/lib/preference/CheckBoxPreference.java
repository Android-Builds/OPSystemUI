package com.oneplus.lib.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$styleable;

public class CheckBoxPreference extends TwoStatePreference {
    public CheckBoxPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public CheckBoxPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.CheckBoxPreference, i, i2);
        setSummaryOn(obtainStyledAttributes.getString(R$styleable.CheckBoxPreference_android_summaryOn));
        setSummaryOff(obtainStyledAttributes.getString(R$styleable.CheckBoxPreference_android_summaryOff));
        setDisableDependentsState(obtainStyledAttributes.getBoolean(R$styleable.CheckBoxPreference_android_disableDependentsState, false));
        obtainStyledAttributes.recycle();
    }

    public CheckBoxPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_checkBoxPreferenceStyle);
    }

    /* access modifiers changed from: protected */
    public void onBindView(View view) {
        super.onBindView(view);
        View findViewById = view.findViewById(16908289);
        if (findViewById != null && (findViewById instanceof Checkable)) {
            ((Checkable) findViewById).setChecked(this.mChecked);
        }
        syncSummaryView(view);
    }
}

package com.oneplus.lib.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$styleable;

public class SwitchPreference extends TwoStatePreference {
    private final Listener mListener;
    private CharSequence mSwitchOff;
    private CharSequence mSwitchOn;

    private class Listener implements OnCheckedChangeListener {
        private Listener() {
        }

        public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
            if (!SwitchPreference.this.callChangeListener(Boolean.valueOf(z))) {
                compoundButton.setChecked(!z);
            } else {
                SwitchPreference.this.setChecked(z);
            }
        }
    }

    public SwitchPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mListener = new Listener();
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.SwitchPreference, i, i2);
        setSummaryOn(obtainStyledAttributes.getString(R$styleable.SwitchPreference_android_summaryOn));
        setSummaryOff(obtainStyledAttributes.getString(R$styleable.SwitchPreference_android_summaryOff));
        setSwitchTextOn(obtainStyledAttributes.getString(R$styleable.SwitchPreference_android_switchTextOn));
        setSwitchTextOff(obtainStyledAttributes.getString(R$styleable.SwitchPreference_android_switchTextOff));
        setDisableDependentsState(obtainStyledAttributes.getBoolean(R$styleable.SwitchPreference_android_disableDependentsState, false));
        obtainStyledAttributes.recycle();
    }

    public SwitchPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public SwitchPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_switchPreferenceStyle);
    }

    /* access modifiers changed from: protected */
    public void onBindView(View view) {
        super.onBindView(view);
        View findViewById = view.findViewById(R$id.switchWidget);
        if (findViewById != null && (findViewById instanceof Checkable)) {
            boolean z = findViewById instanceof Switch;
            if (z) {
                ((Switch) findViewById).setOnCheckedChangeListener(null);
            }
            ((Checkable) findViewById).setChecked(this.mChecked);
            if (z) {
                Switch switchR = (Switch) findViewById;
                switchR.setTextOn(this.mSwitchOn);
                switchR.setTextOff(this.mSwitchOff);
                switchR.setOnCheckedChangeListener(this.mListener);
            }
        }
        syncSummaryView(view);
    }

    public void setSwitchTextOn(CharSequence charSequence) {
        this.mSwitchOn = charSequence;
        notifyChanged();
    }

    public void setSwitchTextOff(CharSequence charSequence) {
        this.mSwitchOff = charSequence;
        notifyChanged();
    }
}

package com.oneplus.keyguard;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import com.android.keyguard.KeyguardEsimArea;
import com.android.keyguard.KeyguardPinBasedInputView;
import com.android.keyguard.KeyguardSimPinView;
import com.android.keyguard.R$plurals;
import com.android.keyguard.R$string;
import com.oneplus.util.OpReflectionUtils;

public abstract class OpKeyguardSimPinView extends KeyguardPinBasedInputView {
    public OpKeyguardSimPinView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public OpBounerMessageAreaInfo getPinPasswordErrorMessageObject(int i, boolean z) {
        int i2;
        int i3;
        OpBounerMessageAreaInfo opBounerMessageAreaInfo = new OpBounerMessageAreaInfo();
        opBounerMessageAreaInfo.setType(1);
        if (i == 0) {
            opBounerMessageAreaInfo.setDisplayMessage(getContext().getString(R$string.kg_password_wrong_pin_code_pukked));
        } else if (i <= 0) {
            opBounerMessageAreaInfo.setDisplayMessage(getContext().getString(z ? R$string.kg_sim_pin_instructions : R$string.kg_password_pin_failed));
        } else if (TelephonyManager.getDefault().getSimCount() > 1) {
            if (z) {
                i3 = R$plurals.kg_password_default_pin_message_multi_sim;
            } else {
                i3 = R$plurals.kg_password_wrong_pin_code_multi_sim;
            }
            opBounerMessageAreaInfo.setDisplayMessage(getContext().getResources().getQuantityString(i3, i, new Object[]{Integer.valueOf(getSlotId() + 1), Integer.valueOf(i)}));
            if (i3 == R$plurals.kg_password_default_pin_message_multi_sim) {
                opBounerMessageAreaInfo.setType(0);
            }
        } else {
            if (z) {
                i2 = R$plurals.kg_password_default_pin_message;
            } else {
                i2 = R$plurals.kg_password_wrong_pin_code;
            }
            opBounerMessageAreaInfo.setDisplayMessage(getContext().getResources().getQuantityString(i2, i, new Object[]{Integer.valueOf(i)}));
            if (i2 == R$plurals.kg_password_default_pin_message) {
                opBounerMessageAreaInfo.setType(0);
            }
        }
        if (KeyguardEsimArea.isEsimLocked(this.mContext, getSubId())) {
            opBounerMessageAreaInfo.setDisplayMessage(getResources().getString(R$string.kg_sim_lock_esim_instructions, new Object[]{opBounerMessageAreaInfo}));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("getPinPasswordErrorMessageObject: attemptsRemaining=");
        sb.append(i);
        sb.append(" displayMessage=");
        sb.append(opBounerMessageAreaInfo.toString());
        Log.d("OpKeyguardSimPinView", sb.toString());
        return opBounerMessageAreaInfo;
    }

    private int getSlotId() {
        return ((Integer) OpReflectionUtils.getValue(KeyguardSimPinView.class, this, "mSlotId")).intValue();
    }

    private int getSubId() {
        return ((Integer) OpReflectionUtils.getValue(KeyguardSimPinView.class, this, "mSubId")).intValue();
    }
}

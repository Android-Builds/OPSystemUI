package com.android.keyguard;

import android.content.res.ColorStateList;

public interface SecurityMessageDisplay {
    void setMessage(int i);

    void setMessage(int i, int i2) {
    }

    void setMessage(CharSequence charSequence);

    void setMessage(CharSequence charSequence, int i) {
    }

    void setNextMessageColor(ColorStateList colorStateList);
}

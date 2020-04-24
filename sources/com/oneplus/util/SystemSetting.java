package com.oneplus.util;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings.System;
import com.android.systemui.statusbar.policy.Listenable;

public abstract class SystemSetting extends ContentObserver implements Listenable {
    private final Context mContext;
    private boolean mCurrentUserOnly;
    private boolean mListening;
    private final String mSettingName;

    /* access modifiers changed from: protected */
    public abstract void handleValueChanged(int i, boolean z);

    public SystemSetting(Context context, Handler handler, String str, boolean z) {
        super(handler);
        this.mContext = context;
        this.mSettingName = str;
        this.mCurrentUserOnly = z;
    }

    public int getValue() {
        return getValue(0);
    }

    public int getValue(int i) {
        if (this.mCurrentUserOnly) {
            return System.getIntForUser(this.mContext.getContentResolver(), this.mSettingName, i, -2);
        }
        return System.getInt(this.mContext.getContentResolver(), this.mSettingName, i);
    }

    public void setValue(int i) {
        if (this.mCurrentUserOnly) {
            System.putIntForUser(this.mContext.getContentResolver(), this.mSettingName, i, -2);
        } else {
            System.putInt(this.mContext.getContentResolver(), this.mSettingName, i);
        }
    }

    public void setListening(boolean z) {
        this.mListening = z;
        if (!z) {
            this.mContext.getContentResolver().unregisterContentObserver(this);
        } else if (this.mCurrentUserOnly) {
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor(this.mSettingName), false, this, -2);
        } else {
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor(this.mSettingName), false, this);
        }
    }

    public void onChange(boolean z) {
        handleValueChanged(getValue(), z);
    }

    public void onUserSwitched() {
        if (this.mListening) {
            setListening(false);
            setListening(true);
        }
    }
}

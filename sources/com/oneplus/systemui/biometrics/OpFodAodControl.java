package com.oneplus.systemui.biometrics;

import android.util.Log;

public class OpFodAodControl extends OpDisplayControl {
    private int mAodMode = 0;

    public /* bridge */ /* synthetic */ boolean disable(String str) {
        return super.disable(str);
    }

    public /* bridge */ /* synthetic */ boolean enable(String str) {
        return super.enable(str);
    }

    public OpFodAodControl(OpFodDisplayController opFodDisplayController) {
        super(opFodDisplayController);
    }

    public boolean canEnable() {
        return this.mAodMode != 2;
    }

    public boolean canDisable() {
        return this.mAodMode != 0;
    }

    public int getAodMode() {
        return this.mAodMode;
    }

    public void enableInner(String str) {
        String str2 = this.mTAG;
        StringBuilder sb = new StringBuilder();
        sb.append("set aod mode on, reason: ");
        sb.append(str);
        Log.d(str2, sb.toString());
        this.mAodMode = 2;
        getNotifier().notifyAodMode(2);
    }

    public void disableInner(String str) {
        String str2 = this.mTAG;
        StringBuilder sb = new StringBuilder();
        sb.append("set aod mode off, reason: ");
        sb.append(str);
        Log.d(str2, sb.toString());
        this.mAodMode = 0;
        getNotifier().notifyAodMode(0);
    }
}

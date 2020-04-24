package com.oneplus.systemui.biometrics;

import android.util.Log;

public class OpFodHighlightControl extends OpDisplayControl {
    private boolean mIsHighlight;

    public /* bridge */ /* synthetic */ boolean disable(String str) {
        return super.disable(str);
    }

    public /* bridge */ /* synthetic */ boolean enable(String str) {
        return super.enable(str);
    }

    public OpFodHighlightControl(OpFodDisplayController opFodDisplayController) {
        super(opFodDisplayController);
    }

    public boolean canEnable() {
        if (this.mIsHighlight) {
            Log.d(this.mTAG, "canEnable: press state not correct");
        }
        return !this.mIsHighlight;
    }

    public boolean canDisable() {
        if (!this.mIsHighlight) {
            Log.d(this.mTAG, "canDisable: press state not correct");
        }
        return this.mIsHighlight;
    }

    public void enableInner(String str) {
        this.mIsHighlight = true;
        if (!getPowerManager().isInteractive()) {
            Log.d(this.mTAG, "device is not interactive, let fp sensor to handle it.");
        } else {
            getNotifier().notifyPressMode(1);
        }
    }

    public void disableInner(String str) {
        this.mIsHighlight = false;
        getNotifier().notifyPressMode(0);
    }

    public boolean isHighlight() {
        return this.mIsHighlight;
    }
}

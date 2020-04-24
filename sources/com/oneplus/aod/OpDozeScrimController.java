package com.oneplus.aod;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class OpDozeScrimController {
    private WakeLock mWakeLock;

    /* access modifiers changed from: protected */
    public void initWakeLock(Context context) {
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "OpDozeScrimController");
        this.mWakeLock.setReferenceCounted(true);
    }

    /* access modifiers changed from: protected */
    public void acquireWakeLock() {
        if (!this.mWakeLock.isHeld()) {
            Log.d("OpDozeScrimController", "hold pulse wake lock");
            this.mWakeLock.acquire();
        }
    }

    /* access modifiers changed from: protected */
    public void releaseWaleLock() {
        if (this.mWakeLock.isHeld()) {
            Log.d("OpDozeScrimController", "release pulse wake lock");
            this.mWakeLock.release();
        }
    }
}

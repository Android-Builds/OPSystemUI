package com.oneplus.systemui.statusbar.phone;

import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.view.WindowManagerGlobal;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.statusbar.phone.KeyguardBouncer;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.oneplus.util.OpReflectionUtils;
import com.oneplus.util.OpUtils;

public class OpStatusBarKeyguardViewManager {
    private Handler mHandler = new Handler();
    private final Runnable mTrimMemoryRunnable = new Runnable() {
        public void run() {
            String str = "trimMemory#onKeyguardFadedAway";
            Log.d("OpStatusBarKeyguardViewManager", str);
            Trace.traceBegin(8, str);
            WindowManagerGlobal.getInstance().trimMemory(20);
            Trace.traceEnd(8);
        }
    };

    public void showBouncerMessage(String str, ColorStateList colorStateList, int i) {
        if (getBouncer() != null) {
            getBouncer().showMessage(str, colorStateList, i);
        }
    }

    private KeyguardBouncer getBouncer() {
        return (KeyguardBouncer) OpReflectionUtils.getValue(StatusBarKeyguardViewManager.class, this, "mBouncer");
    }

    /* access modifiers changed from: protected */
    public void opOnStartedGoingToSleep() {
        if (this.mHandler.hasCallbacks(this.mTrimMemoryRunnable)) {
            this.mHandler.removeCallbacks(this.mTrimMemoryRunnable);
            this.mTrimMemoryRunnable.run();
        }
    }

    /* access modifiers changed from: protected */
    public void opTrimMemory(boolean z) {
        int i = SystemProperties.getInt("debug.trimMemory.delay", 1000);
        this.mHandler.removeCallbacks(this.mTrimMemoryRunnable);
        boolean z2 = ((ScreenLifecycle) Dependency.get(ScreenLifecycle.class)).getScreenState() == 2;
        StringBuilder sb = new StringBuilder();
        sb.append("onKeyguardFadedAway:");
        sb.append(i);
        String str = ", ";
        sb.append(str);
        sb.append(z2);
        sb.append(str);
        sb.append(z);
        sb.append(str);
        sb.append(OpUtils.isHomeApp());
        Log.d("OpStatusBarKeyguardViewManager", sb.toString());
        if (!z || !OpUtils.isHomeApp() || !z2) {
            WindowManagerGlobal.getInstance().trimMemory(20);
        } else {
            this.mHandler.postDelayed(this.mTrimMemoryRunnable, (long) i);
        }
    }

    private boolean isDozing() {
        return ((Boolean) OpReflectionUtils.getValue(StatusBarKeyguardViewManager.class, this, "mDozing")).booleanValue();
    }

    /* access modifiers changed from: protected */
    public void opOnThemeChanged() {
        boolean needsFullscreenBouncer = getBouncer().needsFullscreenBouncer();
        boolean isDozing = isDozing();
        StringBuilder sb = new StringBuilder();
        sb.append("opOnThemeChanged: needsFullscreenBouncer= ");
        sb.append(needsFullscreenBouncer);
        sb.append(", isDozing= ");
        sb.append(isDozing);
        Log.d("OpStatusBarKeyguardViewManager", sb.toString());
        if (needsFullscreenBouncer && !isDozing) {
            getBouncer().show(true);
            OpReflectionUtils.methodInvokeVoid(StatusBarKeyguardViewManager.class, this, "updateStates", new Object[0]);
        }
    }
}

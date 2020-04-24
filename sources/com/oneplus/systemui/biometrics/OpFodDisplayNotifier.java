package com.oneplus.systemui.biometrics;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.utils.ThreadUtils;
import com.oneplus.core.oimc.OIMCServiceManager;
import com.oneplus.keyguard.OpKeyguardUpdateMonitor;
import com.oneplus.util.OpUtils;
import vendor.oneplus.hardware.display.V1_0.IOneplusDisplay;

public class OpFodDisplayNotifier {
    private Context mContext;
    private IOneplusDisplay mDaemon = null;
    private final OIMCServiceManager mOIMCServiceManager;
    private PowerManager mPowerManager;
    private OpKeyguardUpdateMonitor mUpdateMonitor;

    public OpFodDisplayNotifier(Context context) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mOIMCServiceManager = new OIMCServiceManager();
        try {
            this.mDaemon = IOneplusDisplay.getService();
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("IOneplusDisplay getService Exception e = ");
            sb.append(e.toString());
            Log.d("OpFodDisplayNotifier", sb.toString());
        }
    }

    public void notifyAodMode(int i) {
        if (OpUtils.isCustomFingerprint()) {
            ThreadUtils.postOnBackgroundThread(new Runnable(i) {
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    OpFodDisplayNotifier.this.lambda$notifyAodMode$0$OpFodDisplayNotifier(this.f$1);
                }
            });
        }
    }

    public /* synthetic */ void lambda$notifyAodMode$0$OpFodDisplayNotifier(int i) {
        String str = "OpFodDisplayNotifier";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("set OP_DISPLAY_AOD_MODE: ");
            sb.append(i);
            Log.i(str, sb.toString());
            this.mDaemon.setMode(8, i);
        } catch (Exception e) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("postOnBackgroundThread Exception e = ");
            sb2.append(e.toString());
            Log.d(str, sb2.toString());
        }
    }

    public void notifyDisplayDimMode(int i, int i2) {
        ThreadUtils.postOnBackgroundThread(new Runnable(i, i2) {
            private final /* synthetic */ int f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                OpFodDisplayNotifier.this.lambda$notifyDisplayDimMode$1$OpFodDisplayNotifier(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$notifyDisplayDimMode$1$OpFodDisplayNotifier(int i, int i2) {
        String str = "OpFodDisplayNotifier";
        try {
            boolean isKeyguardDone = this.mUpdateMonitor.isKeyguardDone();
            StringBuilder sb = new StringBuilder();
            sb.append("set dim mode to ");
            sb.append(i);
            sb.append(", isKeyguardDone = ");
            sb.append(isKeyguardDone);
            Log.d(str, sb.toString());
            this.mDaemon.setMode(10, i);
            String str2 = "FingerPrintMode";
            if (i != 0) {
                this.mOIMCServiceManager.notifyModeChange(str2, 1, 0);
            } else if (i2 != 2 || (i2 == 2 && isKeyguardDone)) {
                this.mOIMCServiceManager.notifyModeChange(str2, 2, 0);
            }
        } catch (Exception e) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("notifyDisplayDimMode Exception e = ");
            sb2.append(e.toString());
            Log.d(str, sb2.toString());
        }
    }

    public void notifyPressMode(int i) {
        ThreadUtils.postOnBackgroundThread(new Runnable(i) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                OpFodDisplayNotifier.this.lambda$notifyPressMode$2$OpFodDisplayNotifier(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$notifyPressMode$2$OpFodDisplayNotifier(int i) {
        String str = "OpFodDisplayNotifier";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("notifyPressMode: ");
            sb.append(i);
            Log.i(str, sb.toString());
            this.mDaemon.setMode(9, i);
        } catch (Exception e) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("postOnBackgroundThread Exception e = ");
            sb2.append(e.toString());
            Log.d(str, sb2.toString());
        }
    }
}

package com.android.systemui.screenshot;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Log;

public class TakeScreenshotService extends Service {
    /* access modifiers changed from: private */
    public static GlobalScreenshot mScreenshot;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            final Messenger messenger = message.replyTo;
            C10951 r1 = new Runnable() {
                public void run() {
                    try {
                        messenger.send(Message.obtain(null, 1));
                    } catch (RemoteException unused) {
                    }
                }
            };
            String str = "TakeScreenshotService";
            if (!((UserManager) TakeScreenshotService.this.getSystemService(UserManager.class)).isUserUnlocked()) {
                Log.w(str, "Skipping screenshot because storage is locked!");
                post(r1);
                return;
            }
            if (TakeScreenshotService.mScreenshot == null) {
                TakeScreenshotService.mScreenshot = new GlobalScreenshot(TakeScreenshotService.this);
            }
            int i = message.what;
            boolean z = false;
            if (i == 1) {
                GlobalScreenshot access$000 = TakeScreenshotService.mScreenshot;
                boolean z2 = message.arg1 > 0;
                if (message.arg2 > 0) {
                    z = true;
                }
                access$000.takeScreenshot(r1, z2, z);
            } else if (i != 2) {
                StringBuilder sb = new StringBuilder();
                sb.append("Invalid screenshot option: ");
                sb.append(message.what);
                Log.d(str, sb.toString());
            } else {
                GlobalScreenshot access$0002 = TakeScreenshotService.mScreenshot;
                boolean z3 = message.arg1 > 0;
                if (message.arg2 > 0) {
                    z = true;
                }
                access$0002.takeScreenshotPartial(r1, z3, z);
            }
        }
    };

    public IBinder onBind(Intent intent) {
        return new Messenger(this.mHandler).getBinder();
    }

    public boolean onUnbind(Intent intent) {
        GlobalScreenshot globalScreenshot = mScreenshot;
        if (globalScreenshot != null) {
            globalScreenshot.stopScreenshot();
        }
        return true;
    }
}

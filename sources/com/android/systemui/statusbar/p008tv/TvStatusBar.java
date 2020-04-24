package com.android.systemui.statusbar.p008tv;

import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.IStatusBarService.Stub;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;

/* renamed from: com.android.systemui.statusbar.tv.TvStatusBar */
public class TvStatusBar extends SystemUI implements Callbacks {
    private IStatusBarService mBarService;

    public void start() {
        putComponent(TvStatusBar.class, this);
        CommandQueue commandQueue = (CommandQueue) getComponent(CommandQueue.class);
        commandQueue.addCallback((Callbacks) this);
        this.mBarService = Stub.asInterface(ServiceManager.getService("statusbar"));
        try {
            this.mBarService.registerStatusBar(commandQueue);
        } catch (RemoteException unused) {
        }
    }
}

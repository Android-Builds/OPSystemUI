package com.android.systemui.doze;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.dreams.DreamService;
import com.android.systemui.Dependency;
import com.android.systemui.doze.DozeMachine.Service;
import com.android.systemui.doze.DozeMachine.State;
import com.android.systemui.plugins.DozeServicePlugin;
import com.android.systemui.plugins.DozeServicePlugin.RequestDoze;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.shared.plugins.PluginManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.oneplus.plugin.OpLsState;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DozeService extends DreamService implements Service, RequestDoze, PluginListener<DozeServicePlugin> {
    static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    private DozeMachine mDozeMachine;
    private DozeServicePlugin mDozePlugin;
    private PluginManager mPluginManager;

    public DozeService() {
        setDebug(DEBUG);
    }

    public void onCreate() {
        super.onCreate();
        setWindowless(true);
        if (DozeFactory.getHost(this) == null) {
            finish();
            return;
        }
        this.mPluginManager = (PluginManager) Dependency.get(PluginManager.class);
        this.mPluginManager.addPluginListener((PluginListener<T>) this, DozeServicePlugin.class, false);
        this.mDozeMachine = new DozeFactory().assembleMachine(this);
    }

    public void onDestroy() {
        PluginManager pluginManager = this.mPluginManager;
        if (pluginManager != null) {
            pluginManager.removePluginListener(this);
        }
        super.onDestroy();
        this.mDozeMachine = null;
    }

    public void onPluginConnected(DozeServicePlugin dozeServicePlugin, Context context) {
        this.mDozePlugin = dozeServicePlugin;
        this.mDozePlugin.setDozeRequester(this);
    }

    public void onPluginDisconnected(DozeServicePlugin dozeServicePlugin) {
        DozeServicePlugin dozeServicePlugin2 = this.mDozePlugin;
        if (dozeServicePlugin2 != null) {
            dozeServicePlugin2.onDreamingStopped();
            this.mDozePlugin = null;
        }
    }

    public void onDreamingStarted() {
        super.onDreamingStarted();
        DozeMachine dozeMachine = this.mDozeMachine;
        if (dozeMachine == null) {
            finish();
            return;
        }
        dozeMachine.requestState(State.INITIALIZED);
        startDozing();
        DozeServicePlugin dozeServicePlugin = this.mDozePlugin;
        if (dozeServicePlugin != null) {
            dozeServicePlugin.onDreamingStarted();
        }
    }

    public void onDreamingStopped() {
        super.onDreamingStopped();
        this.mDozeMachine.requestState(State.FINISH);
        DozeServicePlugin dozeServicePlugin = this.mDozePlugin;
        if (dozeServicePlugin != null) {
            dozeServicePlugin.onDreamingStopped();
        }
    }

    /* access modifiers changed from: protected */
    public void dumpOnHandler(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dumpOnHandler(fileDescriptor, printWriter, strArr);
        DozeMachine dozeMachine = this.mDozeMachine;
        if (dozeMachine != null) {
            dozeMachine.dump(printWriter);
        }
    }

    public void requestWakeUp() {
        ((PowerManager) getSystemService(PowerManager.class)).wakeUp(SystemClock.uptimeMillis(), 4, "com.android.systemui:NODOZE");
    }

    public void onRequestShowDoze() {
        DozeMachine dozeMachine = this.mDozeMachine;
        if (dozeMachine != null) {
            dozeMachine.requestState(State.DOZE_AOD);
        }
    }

    public void onRequestHideDoze() {
        DozeMachine dozeMachine = this.mDozeMachine;
        if (dozeMachine != null) {
            dozeMachine.requestState(State.DOZE);
        }
    }

    public void onSingleTap() {
        StatusBar phoneStatusBar = OpLsState.getInstance().getPhoneStatusBar();
        if (phoneStatusBar != null) {
            phoneStatusBar.onSingleTap();
        }
    }
}

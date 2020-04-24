package com.oneplus.opzenmode;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.android.systemui.p007qs.GlobalSetting;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.volume.Util;
import com.google.android.collect.Lists;
import com.oneplus.opzenmode.OpZenModeController.Callback;
import com.oneplus.util.SystemSetting;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class OpZenModeControllerImpl implements OpZenModeController {
    private static boolean DEBUG = Build.DEBUG_ONEPLUS;
    private final ArrayList<WeakReference<Callback>> mCallbacks = Lists.newArrayList();
    private Context mContext;
    private int mCurrentUserId;
    private boolean mDndEnable = false;
    private SystemSetting mDndSettingObserver;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 201) {
                OpZenModeControllerImpl.this.handleThreeKeyUpdate(message.arg1);
            } else if (i == 202) {
                OpZenModeControllerImpl.this.handleDndUpdate(((Boolean) message.obj).booleanValue());
            }
        }
    };
    private int mThreeKeySatus = -1;
    private GlobalSetting mThreeKeySettingObserver;
    private final CurrentUserTracker mUserTracker;
    private GlobalSetting mZenModeSettingObserver;

    public OpZenModeControllerImpl(Context context) {
        String str = "OpZenModeControllerImpl";
        Log.i(str, str);
        this.mContext = context;
        registerListener();
        this.mCurrentUserId = -2;
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            public void onUserSwitched(int i) {
                OpZenModeControllerImpl.this.onUserSwitched(i);
            }
        };
        this.mUserTracker.startTracking();
    }

    /* access modifiers changed from: private */
    public void onUserSwitched(int i) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onUserSwitched ");
            sb.append(i);
            Log.i("OpZenModeControllerImpl", sb.toString());
        }
        if (this.mCurrentUserId != i) {
            this.mCurrentUserId = i;
            registerListener();
        }
    }

    private void registerListener() {
        this.mZenModeSettingObserver = new GlobalSetting(this.mContext, new Handler(), "zen_mode") {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i) {
                OpZenModeControllerImpl.this.onDndUpdate(i != 0);
                StringBuilder sb = new StringBuilder();
                sb.append(" zenMode Chnage:");
                sb.append(i);
                Log.i("OpZenModeControllerImpl", sb.toString());
            }
        };
        this.mZenModeSettingObserver.setListening(true);
        onDndUpdate(this.mZenModeSettingObserver.getValue() != 0);
        this.mThreeKeySettingObserver = new GlobalSetting(this.mContext, this.mHandler, "three_Key_mode") {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i) {
                OpZenModeControllerImpl.this.onThreeKeyUpdate(i);
            }
        };
        this.mThreeKeySettingObserver.setListening(true);
        int threeKeyStatus = Util.getThreeKeyStatus(this.mContext);
        if (threeKeyStatus != this.mThreeKeySatus) {
            this.mThreeKeySatus = threeKeyStatus;
            onThreeKeyUpdate(this.mThreeKeySatus);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("registerListener mThreeKeySatus:");
        sb.append(this.mThreeKeySatus);
        sb.append(" current user:");
        sb.append(-2);
        Log.i("OpZenModeControllerImpl", sb.toString());
    }

    public void setDndEnable(boolean z) {
        this.mDndSettingObserver.setValue(z ? 1 : 0);
    }

    public boolean getDndEnable() {
        return this.mDndEnable;
    }

    /* access modifiers changed from: private */
    public void onThreeKeyUpdate(int i) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(201, i, 0));
    }

    /* access modifiers changed from: private */
    public void onDndUpdate(boolean z) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(202, Boolean.valueOf(z)));
    }

    /* access modifiers changed from: private */
    public void handleThreeKeyUpdate(int i) {
        if (i != this.mThreeKeySatus) {
            this.mThreeKeySatus = i;
            StringBuilder sb = new StringBuilder();
            sb.append(" handleThreeKeyUpdate :");
            sb.append(i);
            Log.i("OpZenModeControllerImpl", sb.toString());
            for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
                Callback callback = (Callback) ((WeakReference) this.mCallbacks.get(i2)).get();
                if (callback != null) {
                    callback.onThreeKeyStatus(this.mThreeKeySatus);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleDndUpdate(boolean z) {
        if (this.mDndEnable != z) {
            StringBuilder sb = new StringBuilder();
            sb.append(" handleDndUpdate enable:");
            sb.append(z);
            Log.i("OpZenModeControllerImpl", sb.toString());
            this.mDndEnable = z;
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                Callback callback = (Callback) ((WeakReference) this.mCallbacks.get(i)).get();
                if (callback != null) {
                    callback.onDndChanged(this.mDndEnable);
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0047, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addCallback(com.oneplus.opzenmode.OpZenModeController.Callback r5) {
        /*
            r4 = this;
            java.util.ArrayList<java.lang.ref.WeakReference<com.oneplus.opzenmode.OpZenModeController$Callback>> r0 = r4.mCallbacks
            monitor-enter(r0)
            boolean r1 = DEBUG     // Catch:{ all -> 0x005e }
            if (r1 == 0) goto L_0x001d
            java.lang.String r1 = "OpZenModeControllerImpl"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x005e }
            r2.<init>()     // Catch:{ all -> 0x005e }
            java.lang.String r3 = "*** register callback for "
            r2.append(r3)     // Catch:{ all -> 0x005e }
            r2.append(r5)     // Catch:{ all -> 0x005e }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x005e }
            android.util.Log.v(r1, r2)     // Catch:{ all -> 0x005e }
        L_0x001d:
            r1 = 0
        L_0x001e:
            java.util.ArrayList<java.lang.ref.WeakReference<com.oneplus.opzenmode.OpZenModeController$Callback>> r2 = r4.mCallbacks     // Catch:{ all -> 0x005e }
            int r2 = r2.size()     // Catch:{ all -> 0x005e }
            if (r1 >= r2) goto L_0x004b
            java.util.ArrayList<java.lang.ref.WeakReference<com.oneplus.opzenmode.OpZenModeController$Callback>> r2 = r4.mCallbacks     // Catch:{ all -> 0x005e }
            java.lang.Object r2 = r2.get(r1)     // Catch:{ all -> 0x005e }
            java.lang.ref.WeakReference r2 = (java.lang.ref.WeakReference) r2     // Catch:{ all -> 0x005e }
            java.lang.Object r2 = r2.get()     // Catch:{ all -> 0x005e }
            if (r2 != r5) goto L_0x0048
            boolean r4 = DEBUG     // Catch:{ all -> 0x005e }
            if (r4 == 0) goto L_0x0046
            java.lang.String r4 = "OpZenModeControllerImpl"
            java.lang.String r5 = "Object tried to add another callback"
            java.lang.Exception r1 = new java.lang.Exception     // Catch:{ all -> 0x005e }
            java.lang.String r2 = "Called by"
            r1.<init>(r2)     // Catch:{ all -> 0x005e }
            android.util.Log.e(r4, r5, r1)     // Catch:{ all -> 0x005e }
        L_0x0046:
            monitor-exit(r0)     // Catch:{ all -> 0x005e }
            return
        L_0x0048:
            int r1 = r1 + 1
            goto L_0x001e
        L_0x004b:
            java.util.ArrayList<java.lang.ref.WeakReference<com.oneplus.opzenmode.OpZenModeController$Callback>> r1 = r4.mCallbacks     // Catch:{ all -> 0x005e }
            java.lang.ref.WeakReference r2 = new java.lang.ref.WeakReference     // Catch:{ all -> 0x005e }
            r2.<init>(r5)     // Catch:{ all -> 0x005e }
            r1.add(r2)     // Catch:{ all -> 0x005e }
            r1 = 0
            r4.removeCallback(r1)     // Catch:{ all -> 0x005e }
            monitor-exit(r0)     // Catch:{ all -> 0x005e }
            r4.sendUpdates(r5)
            return
        L_0x005e:
            r4 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x005e }
            throw r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.opzenmode.OpZenModeControllerImpl.addCallback(com.oneplus.opzenmode.OpZenModeController$Callback):void");
    }

    public void removeCallback(Callback callback) {
        synchronized (this.mCallbacks) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("*** unregister callback for ");
                sb.append(callback);
                Log.v("OpZenModeControllerImpl", sb.toString());
            }
            for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
                if (((WeakReference) this.mCallbacks.get(size)).get() == callback) {
                    this.mCallbacks.remove(size);
                }
            }
        }
    }

    private void sendUpdates(Callback callback) {
        callback.onDndChanged(this.mDndEnable);
        callback.onThreeKeyStatus(this.mThreeKeySatus);
    }

    public int getThreeKeySatus() {
        return this.mThreeKeySatus;
    }
}

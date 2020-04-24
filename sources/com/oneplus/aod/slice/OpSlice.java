package com.oneplus.aod.slice;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.systemui.Dependency;
import com.oneplus.aod.slice.OpSliceManager.Callback;
import java.io.PrintWriter;

public abstract class OpSlice {
    protected static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    protected Callback mCallback;
    protected C1750H mHandler = new C1750H((Looper) Dependency.get(Dependency.BG_LOOPER));
    protected int mIcon = 0;
    private boolean mIsActive = false;
    private boolean mIsEnabled = false;
    protected String mPrimary;
    protected String mRemark;
    protected String mSecondary;
    protected String mTag;

    /* renamed from: com.oneplus.aod.slice.OpSlice$H */
    protected final class C1750H extends Handler {
        protected C1750H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            int i = message.what;
            boolean z = false;
            if (i == 1) {
                OpSlice opSlice = OpSlice.this;
                if (message.arg1 == 1) {
                    z = true;
                }
                opSlice.handleSetListening(z);
            } else if (i == 2) {
                OpSlice opSlice2 = OpSlice.this;
                if (message.arg1 == 1) {
                    z = true;
                }
                opSlice2.handleSetEnabled(z);
            } else if (i == 3) {
                OpSlice.this.handleTimeChanged();
            }
        }
    }

    public OpSlice(Callback callback) {
        StringBuilder sb = new StringBuilder();
        sb.append("OpSlice.");
        sb.append(getClass().getSimpleName());
        this.mTag = sb.toString();
        this.mCallback = callback;
    }

    /* access modifiers changed from: protected */
    public void setListening(boolean z) {
        this.mHandler.obtainMessage(1, z ? 1 : 0, 0).sendToTarget();
    }

    /* access modifiers changed from: protected */
    public void setEnabled(boolean z) {
        if (this.mIsEnabled != z) {
            this.mHandler.obtainMessage(2, z ? 1 : 0, 0).sendToTarget();
            this.mIsEnabled = z;
        }
    }

    public int getIcon() {
        return this.mIcon;
    }

    public String getPrimaryString() {
        return this.mPrimary;
    }

    public String getRemark() {
        return this.mRemark;
    }

    public String getSecondaryString() {
        return this.mSecondary;
    }

    public void onTimeChanged() {
        this.mHandler.obtainMessage(3).sendToTarget();
    }

    /* access modifiers changed from: protected */
    public void updateUI() {
        Callback callback = this.mCallback;
        if (callback != null) {
            callback.updateUI();
        }
    }

    /* access modifiers changed from: protected */
    public void setActive(boolean z) {
        this.mIsActive = z;
    }

    /* access modifiers changed from: protected */
    public boolean isActive() {
        return this.mIsActive;
    }

    /* access modifiers changed from: protected */
    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    /* access modifiers changed from: protected */
    public void handleSetListening(boolean z) {
        if (DEBUG) {
            String str = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("handleSetListening: ");
            sb.append(z);
            Log.i(str, sb.toString());
        }
    }

    /* access modifiers changed from: protected */
    public void handleSetEnabled(boolean z) {
        if (DEBUG) {
            String str = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("handleSetEnabled: ");
            sb.append(z);
            Log.d(str, sb.toString());
        }
    }

    /* access modifiers changed from: protected */
    public void handleTimeChanged() {
        if (DEBUG) {
            Log.d(this.mTag, "handleTimeChanged");
        }
    }

    public void dump(PrintWriter printWriter) {
        printWriter.print(this.mTag);
        printWriter.println(":");
        printWriter.print("  mIsEnabled=");
        printWriter.print(this.mIsEnabled);
        printWriter.print(" mIsActive=");
        printWriter.println(this.mIsActive);
    }
}

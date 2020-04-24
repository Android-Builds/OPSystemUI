package com.oneplus.keyguard.clock;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

public class OpClockCtrl {
    private static OpClockCtrl mInstance;
    private BGHandler mBGHandler;
    private OnTimeUpdatedListener mListener;
    private final Object mLock = new Object();
    private Looper mNonUiLooper;
    private boolean mScreenON;

    private class BGHandler extends Handler {
        public BGHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            if (message == null) {
                Log.w("ClockCtrl", "BGHandler: msg null");
                return;
            }
            if (message.what == 131072) {
                OpClockCtrl opClockCtrl = OpClockCtrl.this;
                boolean z = true;
                if (message.arg1 != 1) {
                    z = false;
                }
                opClockCtrl.handleNotifySchedule(z);
            }
        }
    }

    public interface OnTimeUpdatedListener {
        void onTimeChanged();
    }

    public static synchronized OpClockCtrl getInstance() {
        OpClockCtrl opClockCtrl;
        synchronized (OpClockCtrl.class) {
            if (mInstance == null) {
                mInstance = new OpClockCtrl();
            }
            opClockCtrl = mInstance;
        }
        return opClockCtrl;
    }

    public void onStartCtrl(OnTimeUpdatedListener onTimeUpdatedListener, Context context) {
        this.mListener = onTimeUpdatedListener;
        if (this.mBGHandler == null) {
            this.mBGHandler = new BGHandler(getNonUILooper());
            if (context != null) {
                this.mScreenON = ((PowerManager) context.getSystemService("power")).isScreenOn();
            }
        }
        startUpdate("startCtrl");
    }

    public void onScreenTurnedOn() {
        this.mScreenON = true;
        startUpdate("ScreenON");
    }

    public void onScreenTurnedOff() {
        this.mScreenON = false;
        stopUpdate("ScreenOFF");
    }

    /* access modifiers changed from: private */
    public void handleNotifySchedule(boolean z) {
        long currentTimeMillis = System.currentTimeMillis();
        long j = (((currentTimeMillis / 60000) + 1) * 60000) - currentTimeMillis;
        StringBuilder sb = new StringBuilder();
        sb.append(" schedule next: ");
        sb.append(j);
        Log.i("ClockCtrl", sb.toString());
        if (this.mBGHandler != null) {
            Message obtain = Message.obtain();
            obtain.what = 131072;
            obtain.arg1 = 1;
            this.mBGHandler.sendMessageDelayed(obtain, j);
        }
        if (z) {
            dispatchTimeChanged();
        }
    }

    private void startUpdate(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("startUpdate: ");
        sb.append(str);
        sb.append(", ");
        sb.append(this.mScreenON);
        Log.i("ClockCtrl", sb.toString());
        if (this.mScreenON) {
            BGHandler bGHandler = this.mBGHandler;
            if (bGHandler != null) {
                bGHandler.removeMessages(131072);
                this.mBGHandler.sendEmptyMessage(131072);
            }
            dispatchTimeChanged();
        }
    }

    private void stopUpdate(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("stopUpdate: ");
        sb.append(str);
        Log.i("ClockCtrl", sb.toString());
        BGHandler bGHandler = this.mBGHandler;
        if (bGHandler != null) {
            bGHandler.removeMessages(131072);
        }
    }

    private void dispatchTimeChanged() {
        OnTimeUpdatedListener onTimeUpdatedListener = this.mListener;
        if (onTimeUpdatedListener != null) {
            onTimeUpdatedListener.onTimeChanged();
        }
    }

    public Looper getNonUILooper() {
        Looper looper;
        synchronized (this.mLock) {
            if (this.mNonUiLooper == null) {
                HandlerThread handlerThread = new HandlerThread("ClockCtrl thread");
                handlerThread.start();
                this.mNonUiLooper = handlerThread.getLooper();
            }
            looper = this.mNonUiLooper;
        }
        return looper;
    }
}

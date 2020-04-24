package com.android.systemui.keyguard;

import android.os.Trace;
import com.android.systemui.Dumpable;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class ScreenLifecycle extends Lifecycle<Observer> implements Dumpable {
    private int mScreenState = 0;

    public interface Observer {
        void onScreenTurnedOff() {
        }

        void onScreenTurnedOn() {
        }

        void onScreenTurningOff() {
        }

        void onScreenTurningOn() {
        }
    }

    public int getScreenState() {
        return this.mScreenState;
    }

    public void dispatchScreenTurningOn() {
        setScreenState(1);
        dispatch($$Lambda$w9PiqN50NESCg48fJRhE_dJBSdc.INSTANCE);
    }

    public void dispatchScreenTurnedOn() {
        setScreenState(2);
        dispatch($$Lambda$n4aPxVrHdTzFo5NE6H_ILivOadQ.INSTANCE);
    }

    public void dispatchScreenTurningOff() {
        setScreenState(3);
        dispatch($$Lambda$DmSZzOb4vxXoGU7unAMsJYIcFwE.INSTANCE);
    }

    public void dispatchScreenTurnedOff() {
        setScreenState(0);
        dispatch($$Lambda$K8LiTMkPknhhclqjA2eboLxaGEU.INSTANCE);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("ScreenLifecycle:");
        StringBuilder sb = new StringBuilder();
        sb.append("  mScreenState=");
        sb.append(this.mScreenState);
        printWriter.println(sb.toString());
    }

    private void setScreenState(int i) {
        this.mScreenState = i;
        Trace.traceCounter(4096, "screenState", i);
    }
}

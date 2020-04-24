package com.android.systemui.pip.phone;

public abstract class PipTouchGesture {
    /* access modifiers changed from: 0000 */
    public abstract void onDown(PipTouchState pipTouchState);

    /* access modifiers changed from: 0000 */
    public abstract boolean onMove(PipTouchState pipTouchState);

    /* access modifiers changed from: 0000 */
    public abstract boolean onUp(PipTouchState pipTouchState);
}

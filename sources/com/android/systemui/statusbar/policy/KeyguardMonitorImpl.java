package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.statusbar.policy.KeyguardMonitor.Callback;
import com.oneplus.util.OpUtils;
import java.util.ArrayList;
import java.util.Iterator;

public class KeyguardMonitorImpl extends KeyguardUpdateMonitorCallback implements KeyguardMonitor {
    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    private final Context mContext;
    private boolean mKeyguardFadingAway;
    private long mKeyguardFadingAwayDelay;
    private long mKeyguardFadingAwayDuration;
    private boolean mKeyguardGoingAway;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private boolean mLaunchTransitionFadingAway;
    private boolean mListening;
    private boolean mOccluded;
    private boolean mSecure;
    private boolean mShowing;

    public KeyguardMonitorImpl(Context context) {
        this.mContext = context;
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
    }

    public void addCallback(Callback callback) {
        Preconditions.checkNotNull(callback, "Callback must not be null. b/128895449");
        this.mCallbacks.add(callback);
        if (this.mCallbacks.size() != 0 && !this.mListening) {
            this.mListening = true;
            this.mKeyguardUpdateMonitor.registerCallback(this);
        }
    }

    public void removeCallback(Callback callback) {
        Preconditions.checkNotNull(callback, "Callback must not be null. b/128895449");
        if (this.mCallbacks.remove(callback) && this.mCallbacks.size() == 0 && this.mListening) {
            this.mListening = false;
            this.mKeyguardUpdateMonitor.removeCallback(this);
        }
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public boolean isSecure() {
        return this.mSecure;
    }

    public boolean isOccluded() {
        return this.mOccluded;
    }

    public void notifyKeyguardState(boolean z, boolean z2, boolean z3) {
        if (this.mShowing != z || this.mSecure != z2 || this.mOccluded != z3) {
            this.mShowing = z;
            this.mSecure = z2;
            this.mOccluded = z3;
            notifyKeyguardChanged();
        }
    }

    public void onTrustChanged(int i) {
        notifyKeyguardChanged();
    }

    public boolean isDeviceInteractive() {
        return this.mKeyguardUpdateMonitor.isDeviceInteractive();
    }

    private void notifyKeyguardChanged() {
        Iterator it = new ArrayList(this.mCallbacks).iterator();
        while (it.hasNext()) {
            Callback callback = (Callback) it.next();
            if (callback != null) {
                callback.onKeyguardShowingChanged();
            }
        }
    }

    public void notifyKeyguardFadingAway(long j, long j2) {
        this.mKeyguardFadingAway = true;
        this.mKeyguardFadingAwayDelay = j;
        this.mKeyguardFadingAwayDuration = j2;
    }

    public void notifyKeyguardDoneFading() {
        this.mKeyguardFadingAway = false;
        this.mKeyguardGoingAway = false;
    }

    public boolean isKeyguardFadingAway() {
        return this.mKeyguardFadingAway;
    }

    public boolean isKeyguardGoingAway() {
        return this.mKeyguardGoingAway;
    }

    public long getKeyguardFadingAwayDelay() {
        return this.mKeyguardFadingAwayDelay;
    }

    public long getKeyguardFadingAwayDuration() {
        return this.mKeyguardFadingAwayDuration;
    }

    public long calculateGoingToFullShadeDelay() {
        return this.mKeyguardFadingAwayDelay + this.mKeyguardFadingAwayDuration;
    }

    public void notifyKeyguardGoingAway(boolean z) {
        this.mKeyguardGoingAway = z;
    }

    public void setLaunchTransitionFadingAway(boolean z) {
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append(" fadeKeyguardAfterLaunchTransition:");
            sb.append(z);
            Log.i("KeyguardMonitor", sb.toString());
        }
        this.mLaunchTransitionFadingAway = z;
    }

    public boolean isLaunchTransitionFadingAway() {
        return this.mLaunchTransitionFadingAway;
    }
}

package com.android.systemui.doze;

import android.hardware.display.AmbientDisplayConfiguration;
import android.os.Build;
import android.os.Trace;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.Assert;
import com.android.systemui.util.wakelock.WakeLock;
import java.io.PrintWriter;
import java.util.ArrayList;

public class DozeMachine {
    static final boolean DEBUG = DozeService.DEBUG;
    private static final boolean ONEPLUS_DEBUG = Build.DEBUG_ONEPLUS;
    private final AmbientDisplayConfiguration mConfig;
    private final Service mDozeService;
    private Part[] mParts;
    private int mPulseReason;
    private final ArrayList<State> mQueuedRequests = new ArrayList<>();
    private State mState = State.UNINITIALIZED;
    private final WakeLock mWakeLock;
    private boolean mWakeLockHeldForCurrentState = false;

    /* renamed from: com.android.systemui.doze.DozeMachine$1 */
    static /* synthetic */ class C08431 {
        static final /* synthetic */ int[] $SwitchMap$com$android$systemui$doze$DozeMachine$State = new int[State.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(22:0|1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|20|(3:21|22|24)) */
        /* JADX WARNING: Can't wrap try/catch for region: R(24:0|1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|19|20|21|22|24) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:11:0x0040 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:13:0x004b */
        /* JADX WARNING: Missing exception handler attribute for start block: B:15:0x0056 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:17:0x0062 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:19:0x006e */
        /* JADX WARNING: Missing exception handler attribute for start block: B:21:0x007a */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        /* JADX WARNING: Missing exception handler attribute for start block: B:7:0x002a */
        /* JADX WARNING: Missing exception handler attribute for start block: B:9:0x0035 */
        static {
            /*
                com.android.systemui.doze.DozeMachine$State[] r0 = com.android.systemui.doze.DozeMachine.State.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                $SwitchMap$com$android$systemui$doze$DozeMachine$State = r0
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_AOD     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x002a }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_AOD_PAUSED     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x0035 }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_AOD_PAUSING     // Catch:{ NoSuchFieldError -> 0x0035 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0035 }
                r2 = 4
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0035 }
            L_0x0035:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x0040 }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_REQUEST_PULSE     // Catch:{ NoSuchFieldError -> 0x0040 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0040 }
                r2 = 5
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0040 }
            L_0x0040:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x004b }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_PULSING     // Catch:{ NoSuchFieldError -> 0x004b }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x004b }
                r2 = 6
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x004b }
            L_0x004b:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x0056 }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_PULSING_BRIGHT     // Catch:{ NoSuchFieldError -> 0x0056 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0056 }
                r2 = 7
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0056 }
            L_0x0056:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x0062 }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.UNINITIALIZED     // Catch:{ NoSuchFieldError -> 0x0062 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0062 }
                r2 = 8
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0062 }
            L_0x0062:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x006e }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.INITIALIZED     // Catch:{ NoSuchFieldError -> 0x006e }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x006e }
                r2 = 9
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x006e }
            L_0x006e:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x007a }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.FINISH     // Catch:{ NoSuchFieldError -> 0x007a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x007a }
                r2 = 10
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x007a }
            L_0x007a:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x0086 }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_PULSE_DONE     // Catch:{ NoSuchFieldError -> 0x0086 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0086 }
                r2 = 11
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0086 }
            L_0x0086:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.doze.DozeMachine.C08431.<clinit>():void");
        }
    }

    public interface Part {
        void dump(PrintWriter printWriter) {
        }

        void transitionTo(State state, State state2);
    }

    public interface Service {

        public static class Delegate implements Service {
            private final Service mDelegate;

            public Delegate(Service service) {
                this.mDelegate = service;
            }

            public void finish() {
                this.mDelegate.finish();
            }

            public void setDozeScreenState(int i) {
                this.mDelegate.setDozeScreenState(i);
            }

            public void requestWakeUp() {
                this.mDelegate.requestWakeUp();
            }

            public void setDozeScreenBrightness(int i) {
                this.mDelegate.setDozeScreenBrightness(i);
            }
        }

        void finish();

        void requestWakeUp();

        void setDozeScreenBrightness(int i);

        void setDozeScreenState(int i);
    }

    public enum State {
        UNINITIALIZED,
        INITIALIZED,
        DOZE,
        DOZE_AOD,
        DOZE_REQUEST_PULSE,
        DOZE_PULSING,
        DOZE_PULSING_BRIGHT,
        DOZE_PULSE_DONE,
        FINISH,
        DOZE_AOD_PAUSED,
        DOZE_AOD_PAUSING;

        /* access modifiers changed from: 0000 */
        public boolean canPulse() {
            int i = C08431.$SwitchMap$com$android$systemui$doze$DozeMachine$State[ordinal()];
            return i == 1 || i == 2 || i == 3 || i == 4;
        }

        /* access modifiers changed from: 0000 */
        public boolean staysAwake() {
            int i = C08431.$SwitchMap$com$android$systemui$doze$DozeMachine$State[ordinal()];
            return i == 5 || i == 6 || i == 7;
        }

        /* access modifiers changed from: 0000 */
        public int screenState(DozeParameters dozeParameters) {
            int i = 1;
            switch (C08431.$SwitchMap$com$android$systemui$doze$DozeMachine$State[ordinal()]) {
                case 1:
                case 3:
                    return 1;
                case 2:
                case 4:
                    return 4;
                case 5:
                case 8:
                case 9:
                    if (dozeParameters.shouldControlScreenOff()) {
                        i = 2;
                    }
                    return i;
                case 6:
                case 7:
                    return 3;
                default:
                    return 0;
            }
        }
    }

    public DozeMachine(Service service, AmbientDisplayConfiguration ambientDisplayConfiguration, WakeLock wakeLock) {
        this.mDozeService = service;
        this.mConfig = ambientDisplayConfiguration;
        this.mWakeLock = wakeLock;
    }

    public void setParts(Part[] partArr) {
        Preconditions.checkState(this.mParts == null);
        this.mParts = partArr;
    }

    public void requestState(State state) {
        Preconditions.checkArgument(state != State.DOZE_REQUEST_PULSE);
        requestState(state, -1);
    }

    public void requestPulse(int i) {
        Preconditions.checkState(!isExecutingTransition());
        requestState(State.DOZE_REQUEST_PULSE, i);
    }

    private void requestState(State state, int i) {
        Assert.isMainThread();
        String str = "DozeMachine";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("request: current=");
            sb.append(this.mState);
            sb.append(" req=");
            sb.append(state);
            Log.i(str, sb.toString(), new Throwable("here"));
        }
        boolean z = !isExecutingTransition();
        this.mQueuedRequests.add(state);
        if (z) {
            String str2 = "DozeMachine#requestState";
            this.mWakeLock.acquire(str2);
            for (int i2 = 0; i2 < this.mQueuedRequests.size(); i2++) {
                transitionTo((State) this.mQueuedRequests.get(i2), i);
                StringBuilder sb2 = new StringBuilder();
                sb2.append("transitionTo: finish= ");
                sb2.append(i2);
                sb2.append(", size= ");
                sb2.append(this.mQueuedRequests.size());
                Log.d(str, sb2.toString());
            }
            this.mQueuedRequests.clear();
            this.mWakeLock.release(str2);
            return;
        }
        StringBuilder sb3 = new StringBuilder();
        sb3.append("queue event: ");
        sb3.append(state);
        sb3.append(", inside queue: ");
        sb3.append(this.mQueuedRequests);
        Log.d(str, sb3.toString());
    }

    public State getState() {
        Assert.isMainThread();
        if (!isExecutingTransition()) {
            return this.mState;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Cannot get state because there were pending transitions: ");
        sb.append(this.mQueuedRequests.toString());
        throw new IllegalStateException(sb.toString());
    }

    public int getPulseReason() {
        Assert.isMainThread();
        State state = this.mState;
        boolean z = state == State.DOZE_REQUEST_PULSE || state == State.DOZE_PULSING || state == State.DOZE_PULSING_BRIGHT || state == State.DOZE_PULSE_DONE;
        StringBuilder sb = new StringBuilder();
        sb.append("must be in pulsing state, but is ");
        sb.append(this.mState);
        Preconditions.checkState(z, sb.toString());
        return this.mPulseReason;
    }

    public void wakeUp() {
        this.mDozeService.requestWakeUp();
    }

    public boolean isExecutingTransition() {
        return !this.mQueuedRequests.isEmpty();
    }

    private void transitionTo(State state, int i) {
        State transitionPolicy = transitionPolicy(state);
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("transition: old=");
            sb.append(this.mState);
            sb.append(" req=");
            sb.append(state);
            sb.append(" new=");
            sb.append(transitionPolicy);
            Log.i("DozeMachine", sb.toString());
        }
        if (transitionPolicy != this.mState) {
            validateTransition(transitionPolicy);
            State state2 = this.mState;
            this.mState = transitionPolicy;
            DozeLog.traceState(transitionPolicy);
            Trace.traceCounter(4096, "doze_machine_state", transitionPolicy.ordinal());
            updatePulseReason(transitionPolicy, state2, i);
            performTransitionOnComponents(state2, transitionPolicy);
            updateWakeLockState(transitionPolicy);
            resolveIntermediateState(transitionPolicy);
        }
    }

    private void updatePulseReason(State state, State state2, int i) {
        if (state == State.DOZE_REQUEST_PULSE) {
            this.mPulseReason = i;
        } else if (state2 == State.DOZE_PULSE_DONE) {
            this.mPulseReason = -1;
        }
    }

    private void performTransitionOnComponents(State state, State state2) {
        Part[] partArr;
        for (Part part : this.mParts) {
            String str = "DozeMachine";
            if (ONEPLUS_DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("performTransitionOnComponents start. Part:");
                sb.append(part);
                Log.d(str, sb.toString());
            }
            part.transitionTo(state, state2);
            if (ONEPLUS_DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("performTransitionOnComponents end. Part:");
                sb2.append(part);
                Log.d(str, sb2.toString());
            }
        }
        if (C08431.$SwitchMap$com$android$systemui$doze$DozeMachine$State[state2.ordinal()] == 10) {
            this.mDozeService.finish();
        }
    }

    private void validateTransition(State state) {
        try {
            int i = C08431.$SwitchMap$com$android$systemui$doze$DozeMachine$State[this.mState.ordinal()];
            boolean z = true;
            if (i == 8) {
                Preconditions.checkState(state == State.INITIALIZED);
            } else if (i == 10) {
                Preconditions.checkState(state == State.FINISH);
            }
            int i2 = C08431.$SwitchMap$com$android$systemui$doze$DozeMachine$State[state.ordinal()];
            if (i2 == 6) {
                if (this.mState != State.DOZE_REQUEST_PULSE) {
                    z = false;
                }
                Preconditions.checkState(z);
            } else if (i2 == 11) {
                if (!(this.mState == State.DOZE_REQUEST_PULSE || this.mState == State.DOZE_PULSING)) {
                    if (this.mState != State.DOZE_PULSING_BRIGHT) {
                        z = false;
                    }
                }
                Preconditions.checkState(z);
            } else if (i2 == 8) {
                throw new IllegalArgumentException("can't transition to UNINITIALIZED");
            } else if (i2 == 9) {
                if (this.mState != State.UNINITIALIZED) {
                    z = false;
                }
                Preconditions.checkState(z);
            }
        } catch (RuntimeException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Illegal Transition: ");
            sb.append(this.mState);
            sb.append(" -> ");
            sb.append(state);
            throw new IllegalStateException(sb.toString(), e);
        }
    }

    private State transitionPolicy(State state) {
        State state2 = this.mState;
        State state3 = State.FINISH;
        if (state2 == state3) {
            return state3;
        }
        String str = "DozeMachine";
        if ((state2 == State.DOZE_AOD_PAUSED || state2 == State.DOZE_AOD_PAUSING || state2 == State.DOZE_AOD || state2 == State.DOZE) && state == State.DOZE_PULSE_DONE) {
            StringBuilder sb = new StringBuilder();
            sb.append("Dropping pulse done because current state is already done: ");
            sb.append(this.mState);
            Log.i(str, sb.toString());
            return this.mState;
        } else if (state != State.DOZE_REQUEST_PULSE || this.mState.canPulse()) {
            return state;
        } else {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Dropping pulse request because current state can't pulse: ");
            sb2.append(this.mState);
            Log.i(str, sb2.toString());
            return this.mState;
        }
    }

    private void updateWakeLockState(State state) {
        boolean staysAwake = state.staysAwake();
        String str = "DozeMachine#heldForState";
        if (this.mWakeLockHeldForCurrentState && !staysAwake) {
            this.mWakeLock.release(str);
            this.mWakeLockHeldForCurrentState = false;
        } else if (!this.mWakeLockHeldForCurrentState && staysAwake) {
            this.mWakeLock.acquire(str);
            this.mWakeLockHeldForCurrentState = true;
        }
    }

    private void resolveIntermediateState(State state) {
        int i = C08431.$SwitchMap$com$android$systemui$doze$DozeMachine$State[state.ordinal()];
        if (i == 9 || i == 11) {
            transitionTo(this.mConfig.alwaysOnEnabled(-2) ? State.DOZE_AOD : State.DOZE, -1);
        }
    }

    public void dump(PrintWriter printWriter) {
        printWriter.print(" state=");
        printWriter.println(this.mState);
        printWriter.print(" wakeLockHeldForCurrentState=");
        printWriter.println(this.mWakeLockHeldForCurrentState);
        printWriter.print(" wakeLock=");
        printWriter.println(this.mWakeLock);
        printWriter.println("Parts:");
        for (Part dump : this.mParts) {
            dump.dump(printWriter);
        }
    }
}

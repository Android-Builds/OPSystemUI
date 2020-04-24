package com.android.systemui.doze;

import android.app.AlarmManager;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.AmbientDisplayConfiguration;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.Preconditions;
import com.android.systemui.Dependency;
import com.android.systemui.dock.DockManager;
import com.android.systemui.doze.DozeHost.Callback;
import com.android.systemui.doze.DozeMachine.Part;
import com.android.systemui.doze.DozeMachine.State;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.Assert;
import com.android.systemui.util.wakelock.WakeLock;
import com.oneplus.aod.OpAodDisplayViewManager;
import com.oneplus.aod.OpAodUtils;
import com.oneplus.plugin.OpLsState;
import com.oneplus.sarah.SarahClient;
import com.oneplus.util.OpUtils;
import java.io.PrintWriter;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class DozeTriggers implements Part {
    private static final boolean DEBUG = DozeService.DEBUG;
    private static boolean sWakeDisplaySensorState = true;
    private final boolean mAllowPulseTriggers;
    private OpAodDisplayViewManager mAodDisplayViewManager;
    private final TriggerReceiver mBroadcastReceiver = new TriggerReceiver();
    private final AmbientDisplayConfiguration mConfig;
    /* access modifiers changed from: private */
    public final Context mContext;
    private final DockEventListener mDockEventListener = new DockEventListener();
    private final DockManager mDockManager;
    private final DozeHost mDozeHost;
    private final DozeParameters mDozeParameters;
    /* access modifiers changed from: private */
    public final DozeSensors mDozeSensors;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private Callback mHostCallback = new Callback() {
        public void onNotificationAlerted() {
            DozeTriggers.this.onNotification();
        }

        public void onPowerSaveChanged(boolean z) {
            if (z && !OpUtils.isCustomFingerprint()) {
                DozeTriggers.this.mMachine.requestState(State.FINISH);
            }
        }

        public void onThreeKeyChanged(int i) {
            if (DozeMachine.DEBUG) {
                Log.d("DozeTriggers", "requestThreeKeyStatusPulse");
            }
            DozeTriggers.this.requestPulse(10, false);
            DozeLog.tracePulseStart(10);
        }

        public void onSingleTap() {
            if (DozeMachine.DEBUG) {
                Log.d("DozeTriggers", "requestSingleTapPulse");
            }
            DozeTriggers.this.requestPulse(12, false);
            DozeLog.tracePulseStart(12);
        }

        public void onFingerprintPoke() {
            if (DozeMachine.DEBUG) {
                Log.d("DozeTriggers", "requestFingerprintPoke");
            }
            DozeTriggers.this.requestPulse(13, false);
            DozeLog.tracePulseStart(13);
        }
    };
    /* access modifiers changed from: private */
    public final DozeMachine mMachine;
    private final MetricsLogger mMetricsLogger = ((MetricsLogger) Dependency.get(MetricsLogger.class));
    private long mNotificationPulseTime;
    private boolean mPulsePending;
    /* access modifiers changed from: private */
    public final SensorManager mSensorManager;
    private final UiModeManager mUiModeManager;
    /* access modifiers changed from: private */
    public final WakeLock mWakeLock;

    /* renamed from: com.android.systemui.doze.DozeTriggers$3 */
    static /* synthetic */ class C08503 {
        static final /* synthetic */ int[] $SwitchMap$com$android$systemui$doze$DozeMachine$State = new int[State.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(20:0|1|2|3|4|5|6|7|8|9|10|11|12|13|14|15|16|17|18|20) */
        /* JADX WARNING: Code restructure failed: missing block: B:21:?, code lost:
            return;
         */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:11:0x0040 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:13:0x004b */
        /* JADX WARNING: Missing exception handler attribute for start block: B:15:0x0056 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:17:0x0062 */
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
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.INITIALIZED     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x002a }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_AOD     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x0035 }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_AOD_PAUSED     // Catch:{ NoSuchFieldError -> 0x0035 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0035 }
                r2 = 4
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0035 }
            L_0x0035:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x0040 }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_AOD_PAUSING     // Catch:{ NoSuchFieldError -> 0x0040 }
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
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.DOZE_PULSE_DONE     // Catch:{ NoSuchFieldError -> 0x0062 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0062 }
                r2 = 8
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0062 }
            L_0x0062:
                int[] r0 = $SwitchMap$com$android$systemui$doze$DozeMachine$State     // Catch:{ NoSuchFieldError -> 0x006e }
                com.android.systemui.doze.DozeMachine$State r1 = com.android.systemui.doze.DozeMachine.State.FINISH     // Catch:{ NoSuchFieldError -> 0x006e }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x006e }
                r2 = 9
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x006e }
            L_0x006e:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.doze.DozeTriggers.C08503.<clinit>():void");
        }
    }

    private class DockEventListener implements com.android.systemui.dock.DockManager.DockEventListener {
        private DockEventListener() {
        }
    }

    private abstract class ProximityCheck implements SensorEventListener, Runnable {
        private boolean mFinished;
        private float mMaxRange;
        private boolean mRegistered;

        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        /* access modifiers changed from: protected */
        public abstract void onProximityResult(int i);

        private ProximityCheck() {
        }

        public void check() {
            Preconditions.checkState(!this.mFinished && !this.mRegistered);
            Sensor defaultSensor = DozeTriggers.this.mSensorManager.getDefaultSensor(33171025);
            String str = "DozeTriggers";
            if (defaultSensor == null) {
                if (DozeMachine.DEBUG) {
                    Log.d(str, "ProxCheck: No sensor found");
                }
                finishWithResult(0);
                return;
            }
            DozeTriggers.this.mDozeSensors.setDisableSensorsInterferingWithProximity(true);
            this.mMaxRange = defaultSensor.getMaximumRange();
            DozeTriggers.this.mSensorManager.registerListener(this, defaultSensor, 3, 0, DozeTriggers.this.mHandler);
            DozeTriggers.this.mHandler.postDelayed(this, 500);
            DozeTriggers.this.mWakeLock.acquire(str);
            this.mRegistered = true;
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            String str = "DozeTriggers";
            boolean z = false;
            if (sensorEvent.values.length == 0) {
                if (DozeMachine.DEBUG) {
                    Log.d(str, "ProxCheck: Event has no values!");
                }
                finishWithResult(0);
                return;
            }
            if (DozeMachine.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("ProxCheck: Event: value=");
                sb.append(sensorEvent.values[0]);
                sb.append(" max=");
                sb.append(this.mMaxRange);
                Log.d(str, sb.toString());
            }
            int i = 1;
            if (sensorEvent.values[0] == 1.0f) {
                z = true;
            }
            if (!z) {
                i = 2;
            }
            finishWithResult(i);
        }

        public void run() {
            if (DozeMachine.DEBUG) {
                Log.d("DozeTriggers", "ProxCheck: No event received before timeout");
            }
            finishWithResult(0);
        }

        private void finishWithResult(int i) {
            if (!this.mFinished) {
                boolean z = this.mRegistered;
                if (z) {
                    DozeTriggers.this.mHandler.removeCallbacks(this);
                    DozeTriggers.this.mSensorManager.unregisterListener(this);
                    DozeTriggers.this.mDozeSensors.setDisableSensorsInterferingWithProximity(false);
                    this.mRegistered = false;
                }
                onProximityResult(i);
                if (z) {
                    DozeTriggers.this.mWakeLock.release("DozeTriggers");
                }
                this.mFinished = true;
            }
        }
    }

    private class TriggerReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        private TriggerReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.doze.pulse".equals(intent.getAction())) {
                if (DozeMachine.DEBUG) {
                    Log.d("DozeTriggers", "Received pulse intent");
                }
                DozeTriggers.this.requestPulse(0, false);
            }
            if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(intent.getAction())) {
                DozeTriggers.this.mMachine.requestState(State.FINISH);
            }
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                DozeTriggers.this.mDozeSensors.onUserSwitched();
            }
        }

        public void register(Context context) {
            if (!this.mRegistered) {
                IntentFilter intentFilter = new IntentFilter("com.android.systemui.doze.pulse");
                intentFilter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
                intentFilter.addAction("android.intent.action.USER_SWITCHED");
                context.registerReceiver(this, intentFilter);
                this.mRegistered = true;
            }
        }

        public void unregister(Context context) {
            if (this.mRegistered) {
                context.unregisterReceiver(this);
                this.mRegistered = false;
            }
        }
    }

    private boolean canPulse() {
        return true;
    }

    public DozeTriggers(Context context, DozeMachine dozeMachine, DozeHost dozeHost, AlarmManager alarmManager, AmbientDisplayConfiguration ambientDisplayConfiguration, DozeParameters dozeParameters, SensorManager sensorManager, Handler handler, WakeLock wakeLock, boolean z, DockManager dockManager) {
        this.mContext = context;
        this.mMachine = dozeMachine;
        this.mDozeHost = dozeHost;
        AmbientDisplayConfiguration ambientDisplayConfiguration2 = ambientDisplayConfiguration;
        this.mConfig = ambientDisplayConfiguration2;
        DozeParameters dozeParameters2 = dozeParameters;
        this.mDozeParameters = dozeParameters2;
        this.mSensorManager = sensorManager;
        this.mHandler = handler;
        WakeLock wakeLock2 = wakeLock;
        this.mWakeLock = wakeLock2;
        this.mAllowPulseTriggers = z;
        DozeSensors dozeSensors = new DozeSensors(context, alarmManager, this.mSensorManager, dozeParameters2, ambientDisplayConfiguration2, wakeLock2, new DozeSensors.Callback() {
            public final void onSensorPulse(int i, boolean z, float f, float f2, float[] fArr) {
                DozeTriggers.this.onSensor(i, z, f, f2, fArr);
            }
        }, new Consumer() {
            public final void accept(Object obj) {
                DozeTriggers.this.onProximityFar(((Boolean) obj).booleanValue());
            }
        }, dozeParameters.getPolicy());
        this.mDozeSensors = dozeSensors;
        this.mUiModeManager = (UiModeManager) this.mContext.getSystemService(UiModeManager.class);
        this.mDockManager = dockManager;
        this.mAodDisplayViewManager = OpLsState.getInstance().getPhoneStatusBar().getAodDisplayViewManager();
    }

    /* access modifiers changed from: private */
    public void onNotification() {
        if (DozeMachine.DEBUG) {
            Log.d("DozeTriggers", "requestNotificationPulse");
        }
        this.mNotificationPulseTime = SystemClock.elapsedRealtime();
        if (OpAodUtils.isNotificationWakeUpEnabled()) {
            requestPulse(1, false);
            DozeLog.traceNotificationPulse(this.mContext);
        }
    }

    /* JADX WARNING: type inference failed for: r1v2 */
    /* JADX WARNING: type inference failed for: r1v3, types: [int] */
    /* JADX WARNING: type inference failed for: r1v4 */
    /* JADX WARNING: type inference failed for: r1v5, types: [boolean] */
    /* JADX WARNING: type inference failed for: r1v6 */
    /* JADX WARNING: type inference failed for: r1v7 */
    /* JADX WARNING: Multi-variable type inference failed. Error: jadx.core.utils.exceptions.JadxRuntimeException: No candidate types for var: r1v6
      assigns: [?[int, float, boolean, short, byte, char, OBJECT, ARRAY], ?[boolean, int, float, short, byte, char], ?[int, float, short, byte, char]]
      uses: [boolean, int]
      mth insns count: 45
    	at jadx.core.dex.visitors.typeinference.TypeSearch.fillTypeCandidates(TypeSearch.java:237)
    	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
    	at jadx.core.dex.visitors.typeinference.TypeSearch.run(TypeSearch.java:53)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runMultiVariableSearch(TypeInferenceVisitor.java:99)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:92)
    	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
    	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
    	at java.base/java.util.ArrayList.forEach(ArrayList.java:1540)
    	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
    	at jadx.core.ProcessClass.process(ProcessClass.java:30)
    	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:311)
    	at jadx.api.JavaClass.decompile(JavaClass.java:62)
    	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:217)
     */
    /* JADX WARNING: Unknown variable types count: 2 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void proximityCheckThenCall(java.util.function.IntConsumer r9, boolean r10, int r11) {
        /*
            r8 = this;
            com.android.systemui.doze.DozeSensors r0 = r8.mDozeSensors
            java.lang.Boolean r0 = r0.isProximityCurrentlyFar()
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "proximityCheckThenCall: alreadyPerformedProxCheck="
            r1.append(r2)
            r1.append(r10)
            java.lang.String r2 = ", cachedProxFar="
            r1.append(r2)
            r1.append(r0)
            java.lang.String r2 = ", reason="
            r1.append(r2)
            r1.append(r11)
            java.lang.String r1 = r1.toString()
            java.lang.String r2 = "DozeTriggers"
            android.util.Log.d(r2, r1)
            r1 = 1
            int[] r2 = new int[r1]
            r3 = 0
            r4 = 80
            r2[r3] = r4
            boolean r2 = android.util.OpFeatures.isSupport(r2)
            if (r2 == 0) goto L_0x004f
            com.android.systemui.doze.DozeSensors r10 = r8.mDozeSensors
            int r10 = r10.getCustomProximityResult()
            android.content.Context r8 = r8.mContext
            if (r10 != r1) goto L_0x0045
            goto L_0x0046
        L_0x0045:
            r1 = r3
        L_0x0046:
            r2 = 0
            com.android.systemui.doze.DozeLog.traceProximityResult(r8, r1, r2, r11)
            r9.accept(r10)
            goto L_0x0073
        L_0x004f:
            if (r10 == 0) goto L_0x0056
            r8 = 3
            r9.accept(r8)
            goto L_0x0073
        L_0x0056:
            if (r0 == 0) goto L_0x0063
            boolean r8 = r0.booleanValue()
            if (r8 == 0) goto L_0x005f
            r1 = 2
        L_0x005f:
            r9.accept(r1)
            goto L_0x0073
        L_0x0063:
            long r4 = android.os.SystemClock.uptimeMillis()
            com.android.systemui.doze.DozeTriggers$1 r10 = new com.android.systemui.doze.DozeTriggers$1
            r2 = r10
            r3 = r8
            r6 = r11
            r7 = r9
            r2.<init>(r4, r6, r7)
            r10.check()
        L_0x0073:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.doze.DozeTriggers.proximityCheckThenCall(java.util.function.IntConsumer, boolean, int):void");
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void onSensor(int i, boolean z, float f, float f2, float[] fArr) {
        boolean z2;
        int i2 = i;
        float[] fArr2 = fArr;
        boolean z3 = true;
        boolean z4 = i2 == 4;
        boolean z5 = i2 == 9;
        boolean z6 = i2 == 3;
        boolean z7 = i2 == 5;
        boolean z8 = i2 == 7;
        boolean z9 = i2 == 8;
        boolean z10 = (fArr2 == null || fArr2.length <= 0 || fArr2[0] == 0.0f) ? false : true;
        if (i2 == -1) {
            this.mDozeHost.stopPulsing();
            return;
        }
        if (z8) {
            onWakeScreen(z10, this.mMachine.isExecutingTransition() ? null : this.mMachine.getState());
        } else if (z7) {
            requestPulse(i, z);
        } else if (!z9) {
            $$Lambda$DozeTriggers$tDF3nUoeKjZGk_4_gWQ9lz7YWJk r0 = new IntConsumer(z4, z5, f, f2, i, z6, z) {
                private final /* synthetic */ boolean f$1;
                private final /* synthetic */ boolean f$2;
                private final /* synthetic */ float f$3;
                private final /* synthetic */ float f$4;
                private final /* synthetic */ int f$5;
                private final /* synthetic */ boolean f$6;
                private final /* synthetic */ boolean f$7;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                    this.f$5 = r6;
                    this.f$6 = r7;
                    this.f$7 = r8;
                }

                public final void accept(int i) {
                    DozeTriggers.this.lambda$onSensor$0$DozeTriggers(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, i);
                }
            };
            if (!z) {
                DockManager dockManager = this.mDockManager;
                if (dockManager == null || !dockManager.isDocked()) {
                    z2 = false;
                    proximityCheckThenCall(r0, z2, i2);
                }
            }
            z2 = true;
            proximityCheckThenCall(r0, z2, i2);
        } else if (z10) {
            requestPulse(i, z);
        }
        if (z6) {
            if (SystemClock.elapsedRealtime() - this.mNotificationPulseTime >= ((long) this.mDozeParameters.getPickupVibrationThreshold())) {
                z3 = false;
            }
            DozeLog.tracePickupWakeUp(this.mContext, z3);
        }
    }

    public /* synthetic */ void lambda$onSensor$0$DozeTriggers(boolean z, boolean z2, float f, float f2, int i, boolean z3, boolean z4, int i2) {
        if (i2 == 1) {
            Log.d("DozeTriggers", "In pocket, drop event");
            return;
        }
        if (z || z2) {
            if (!(f == -1.0f || f2 == -1.0f)) {
                this.mDozeHost.onSlpiTap(f, f2);
            }
            gentleWakeUp(i);
        } else if (z3) {
            requestPulse(i, z4);
        } else {
            this.mDozeHost.extendPulse(i);
        }
    }

    private void gentleWakeUp(int i) {
        this.mMetricsLogger.write(new LogMaker(223).setType(6).setSubtype(i));
        if (this.mDozeParameters.getDisplayNeedsBlanking()) {
            this.mDozeHost.setAodDimmingScrim(255.0f);
        }
        this.mMachine.wakeUp();
    }

    /* access modifiers changed from: private */
    public void onProximityFar(boolean z) {
        String str = "DozeTriggers";
        if (this.mMachine.isExecutingTransition()) {
            Log.w(str, "onProximityFar called during transition. Ignoring sensor response.");
            return;
        }
        boolean z2 = !z;
        State state = this.mMachine.getState();
        boolean z3 = false;
        boolean z4 = state == State.DOZE_AOD_PAUSED;
        boolean z5 = state == State.DOZE_AOD_PAUSING;
        if (state == State.DOZE_AOD) {
            z3 = true;
        }
        if (state == State.DOZE_PULSING || state == State.DOZE_PULSING_BRIGHT) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Prox changed, ignore touch = ");
                sb.append(z2);
                Log.i(str, sb.toString());
            }
            this.mDozeHost.onIgnoreTouchWhilePulsing(z2);
        }
        if (z && (z4 || z5)) {
            if (DEBUG) {
                Log.i(str, "Prox FAR, unpausing AOD");
            }
            this.mMachine.requestState(State.DOZE_AOD);
        } else if (z2 && z3) {
            if (DEBUG) {
                Log.i(str, "Prox NEAR, pausing AOD");
            }
            this.mMachine.requestState(State.DOZE_AOD_PAUSING);
        }
    }

    private void onWakeScreen(boolean z, State state) {
        DozeLog.traceWakeDisplay(z);
        sWakeDisplaySensorState = z;
        boolean z2 = false;
        if (z) {
            proximityCheckThenCall(new IntConsumer(state) {
                private final /* synthetic */ State f$1;

                {
                    this.f$1 = r2;
                }

                public final void accept(int i) {
                    DozeTriggers.this.lambda$onWakeScreen$1$DozeTriggers(this.f$1, i);
                }
            }, false, 7);
            return;
        }
        boolean z3 = state == State.DOZE_AOD_PAUSED;
        if (state == State.DOZE_AOD_PAUSING) {
            z2 = true;
        }
        if (!z2 && !z3) {
            this.mMachine.requestState(State.DOZE);
            this.mMetricsLogger.write(new LogMaker(223).setType(2).setSubtype(7));
        }
    }

    public /* synthetic */ void lambda$onWakeScreen$1$DozeTriggers(State state, int i) {
        if (i != 1 && state == State.DOZE) {
            this.mMachine.requestState(State.DOZE_AOD);
            this.mMetricsLogger.write(new LogMaker(223).setType(1).setSubtype(7));
        }
    }

    public void transitionTo(State state, State state2) {
        switch (C08503.$SwitchMap$com$android$systemui$doze$DozeMachine$State[state2.ordinal()]) {
            case 1:
                this.mBroadcastReceiver.register(this.mContext);
                this.mDozeHost.addCallback(this.mHostCallback);
                DockManager dockManager = this.mDockManager;
                if (dockManager != null) {
                    dockManager.addListener(this.mDockEventListener);
                }
                this.mDozeSensors.requestTemporaryDisable();
                checkTriggersAtInit();
                return;
            case 2:
            case 3:
                this.mDozeSensors.setCustomProxListening(true);
                this.mDozeSensors.resetMotionValue();
                this.mDozeSensors.setProxListening(state2 != State.DOZE);
                this.mDozeSensors.setListening(true);
                if (state2 == State.DOZE_AOD && !sWakeDisplaySensorState) {
                    onWakeScreen(false, state2);
                    return;
                }
                return;
            case 4:
            case 5:
                this.mDozeSensors.setProxListening(true);
                this.mDozeSensors.setListening(false);
                return;
            case 6:
            case 7:
                Context context = this.mContext;
                if (context != null) {
                    SarahClient.getInstance(context).notifyAodOn();
                }
                this.mDozeSensors.setCustomProxListening(false);
                this.mDozeSensors.setTouchscreenSensorsListening(false);
                return;
            case 8:
                this.mDozeSensors.requestTemporaryDisable();
                return;
            case 9:
                this.mBroadcastReceiver.unregister(this.mContext);
                this.mDozeHost.removeCallback(this.mHostCallback);
                this.mDozeSensors.setCustomProxListening(false);
                DockManager dockManager2 = this.mDockManager;
                if (dockManager2 != null) {
                    dockManager2.removeListener(this.mDockEventListener);
                }
                this.mDozeSensors.setListening(false);
                this.mDozeSensors.setProxListening(false);
                return;
            default:
                return;
        }
    }

    private void checkTriggersAtInit() {
        if (this.mUiModeManager.getCurrentModeType() == 3 || ((this.mDozeHost.isPowerSaveActive() && !OpUtils.isCustomFingerprint()) || this.mDozeHost.isBlockingDoze() || !this.mDozeHost.isProvisioned())) {
            this.mMachine.requestState(State.FINISH);
        }
    }

    /* access modifiers changed from: private */
    public void requestPulse(int i, boolean z) {
        Assert.isMainThread();
        if (this.mMachine.isExecutingTransition()) {
            Log.w("DozeTriggers", "requestPulse called during transition. ignore pulse");
            return;
        }
        this.mDozeHost.extendPulse(i);
        if (this.mMachine.getState() == State.DOZE_PULSING && i == 8) {
            this.mMachine.requestState(State.DOZE_PULSING_BRIGHT);
        } else if (this.mPulsePending || !this.mAllowPulseTriggers || !canPulse()) {
            if (this.mAllowPulseTriggers) {
                DozeLog.tracePulseDropped(this.mContext, this.mPulsePending, this.mMachine.getState(), this.mDozeHost.isPulsingBlocked());
            }
        } else {
            boolean z2 = true;
            this.mPulsePending = true;
            $$Lambda$DozeTriggers$EDCYzTgUQ8bpFfKolETll4jmVsA r1 = new IntConsumer(i) {
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void accept(int i) {
                    DozeTriggers.this.lambda$requestPulse$2$DozeTriggers(this.f$1, i);
                }
            };
            if (this.mDozeParameters.getProxCheckBeforePulse() && !z) {
                z2 = false;
            }
            proximityCheckThenCall(r1, z2, i);
            this.mMetricsLogger.write(new LogMaker(223).setType(6).setSubtype(i));
        }
    }

    public /* synthetic */ void lambda$requestPulse$2$DozeTriggers(int i, int i2) {
        if (i2 == 1) {
            this.mPulsePending = false;
        } else {
            continuePulseRequest(i);
        }
    }

    private void continuePulseRequest(int i) {
        this.mPulsePending = false;
        StringBuilder sb = new StringBuilder();
        sb.append("continuePulseRequest: state = ");
        sb.append(this.mMachine.getState());
        sb.append(", reason = ");
        sb.append(i);
        Log.d("DozeTriggers", sb.toString());
        if (this.mMachine.getState() != State.DOZE_PULSING || i != 3) {
            if (this.mDozeHost.isPulsingBlocked() || !canPulse()) {
                DozeLog.tracePulseDropped(this.mContext, this.mPulsePending, this.mMachine.getState(), this.mDozeHost.isPulsingBlocked());
                return;
            }
            this.mAodDisplayViewManager.updateForPulseReason(i);
            this.mMachine.requestPulse(i);
        }
    }

    public void dump(PrintWriter printWriter) {
        printWriter.print(" notificationPulseTime=");
        printWriter.println(Formatter.formatShortElapsedTime(this.mContext, this.mNotificationPulseTime));
        printWriter.print(" pulsePending=");
        printWriter.println(this.mPulsePending);
        printWriter.println("DozeSensors:");
        this.mDozeSensors.dump(printWriter);
    }
}

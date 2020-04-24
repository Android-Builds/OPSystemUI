package com.android.systemui.doze;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.hardware.display.AmbientDisplayConfiguration;
import android.net.Uri;
import android.os.Debug;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import android.util.OpFeatures;
import com.android.systemui.R$string;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.plugins.SensorManagerPlugin;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.AlarmTimeout;
import com.android.systemui.util.AsyncSensorManager;
import com.android.systemui.util.wakelock.WakeLock;
import com.oneplus.aod.OpAodUtils;
import com.oneplus.sarah.SarahClient;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class DozeSensors {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = DozeService.DEBUG;
    /* access modifiers changed from: private */
    public final AlarmManager mAlarmManager;
    /* access modifiers changed from: private */
    public final Callback mCallback;
    /* access modifiers changed from: private */
    public final AmbientDisplayConfiguration mConfig;
    /* access modifiers changed from: private */
    public final Context mContext;
    private CustomProximityCheck mCustomProximityCheck;
    /* access modifiers changed from: private */
    public long mDebounceFrom;
    /* access modifiers changed from: private */
    public final DozeParameters mDozeParameters;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler();
    /* access modifiers changed from: private */
    public MotionCheck mMotionCheck;
    private PickupCheck mPickUpCheck;
    private final TriggerSensor mPickupSensor;
    /* access modifiers changed from: private */
    public final Consumer<Boolean> mProxCallback;
    private final ProxSensor mProxSensor;
    /* access modifiers changed from: private */
    public int mProximityResult = 0;
    /* access modifiers changed from: private */
    public final ContentResolver mResolver;
    /* access modifiers changed from: private */
    public final SensorManager mSensorManager;
    protected TriggerSensor[] mSensors;
    private boolean mSettingRegistered;
    /* access modifiers changed from: private */
    public final ContentObserver mSettingsObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean z, Uri uri, int i) {
            if (i == ActivityManager.getCurrentUser()) {
                for (TriggerSensor updateListener : DozeSensors.this.mSensors) {
                    updateListener.updateListener();
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public final WakeLock mWakeLock;

    public interface Callback {
        void onSensorPulse(int i, boolean z, float f, float f2, float[] fArr);
    }

    private abstract class CustomProximityCheck implements SensorEventListener, Runnable {
        private boolean mFinished;
        private float mMaxRange;
        private boolean mRegistered;
        private final String mTag;

        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        public abstract void onProximityResult(int i);

        private CustomProximityCheck() {
            this.mTag = "DozeSensor.CustomProximityCheck";
        }

        public void check() {
            if (!this.mFinished && !this.mRegistered) {
                Sensor defaultSensor = DozeSensors.this.mSensorManager.getDefaultSensor(33171025);
                String str = "DozeSensor.CustomProximityCheck";
                if (defaultSensor == null) {
                    if (DozeSensors.DEBUG) {
                        Log.d(str, "No sensor found");
                    }
                    finishWithResult(0);
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("register pocket ");
                sb.append(hashCode());
                Log.d(str, sb.toString());
                this.mMaxRange = defaultSensor.getMaximumRange();
                DozeSensors.this.mSensorManager.registerListener(this, defaultSensor, 3, 0, DozeSensors.this.mHandler);
                DozeSensors.this.mHandler.postDelayed(this, 500);
                this.mRegistered = true;
            }
        }

        public void setListening(boolean z) {
            if (z) {
                check();
            } else {
                release();
            }
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            String str = "DozeSensor.CustomProximityCheck";
            boolean z = false;
            if (sensorEvent.values.length == 0) {
                if (DozeSensors.DEBUG) {
                    Log.d(str, "Event has no values!");
                }
                finishWithResult(0);
                return;
            }
            if (DozeSensors.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Event: value=");
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
            if (DozeSensors.DEBUG) {
                Log.d("DozeSensor.CustomProximityCheck", "No event received before timeout");
            }
            finishWithResult(0);
        }

        private void finishWithResult(int i) {
            if (this.mRegistered) {
                DozeSensors.this.mHandler.removeCallbacks(this);
            }
            onProximityResult(i);
        }

        private void release() {
            if (this.mRegistered && DozeSensors.this.mSensorManager != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Unregister pocket Sensor ");
                sb.append(hashCode());
                Log.d("DozeSensor.CustomProximityCheck", sb.toString());
                DozeSensors.this.mSensorManager.unregisterListener(this);
                this.mRegistered = false;
            }
        }
    }

    private class MotionCheck implements SensorEventListener, Runnable {
        private boolean mConfigured;
        private int mCurrentState;
        private boolean mFinished = false;
        private float mMaxRange;
        private boolean mProximityChecking;
        private int mPulseReason;
        private boolean mRegistered;
        private int mSensorType = 30;
        private final String mTag = "DozeSensor.MotionCheck";

        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        public void run() {
        }

        public MotionCheck(boolean z, int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("choose sensor: ");
            sb.append("TYPE_MOTION");
            Log.d("DozeSensors", sb.toString());
            this.mConfigured = z;
            this.mPulseReason = i;
        }

        public void check() {
            if (!this.mFinished && !this.mRegistered && this.mConfigured) {
                Sensor defaultSensor = DozeSensors.this.mSensorManager.getDefaultSensor(this.mSensorType);
                String str = "DozeSensor.MotionCheck";
                if (defaultSensor == null) {
                    if (DozeSensors.DEBUG) {
                        Log.d(str, "No sensor found");
                    }
                    finishWithResult(0);
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("sensor registered ");
                sb.append(hashCode());
                Log.d(str, sb.toString());
                this.mMaxRange = defaultSensor.getMaximumRange();
                DozeSensors.this.mSensorManager.registerListener(this, defaultSensor, 3, 0, DozeSensors.this.mHandler);
                DozeSensors.this.mHandler.postDelayed(this, 500);
                this.mRegistered = true;
            }
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            StringBuilder sb = new StringBuilder();
            sb.append("onSensorChanged: proximity checking = ");
            sb.append(this.mProximityChecking);
            String str = "DozeSensor.MotionCheck";
            Log.d(str, sb.toString());
            if (!this.mProximityChecking) {
                float[] fArr = sensorEvent.values;
                if (fArr.length == 0) {
                    if (DozeSensors.DEBUG) {
                        Log.d(str, "Event has no values!");
                    }
                    finishWithResult(0);
                } else if (fArr[0] == 1.0f) {
                    finishWithResult(1);
                } else if (((double) fArr[0]) == 2.0d) {
                    finishWithResult(2);
                } else if (fArr[0] == 0.0f) {
                    finishWithResult(3);
                    DozeSensors.this.mHandler.removeCallbacks(this);
                } else if (fArr[0] == -1.0f) {
                    finishWithResult(4);
                }
                StringBuilder sb2 = new StringBuilder();
                sb2.append("onSensorChanged: value = ");
                sb2.append(sensorEvent.values[0]);
                Log.d(str, sb2.toString());
                if (!(sensorEvent.values[0] == 0.0f || DozeSensors.this.mContext == null)) {
                    SarahClient.getInstance(DozeSensors.this.mContext).notifyAodOnReason(sensorEvent.values[1]);
                }
            }
        }

        private void finishWithResult(int i) {
            if (this.mRegistered) {
                if (this.mCurrentState != 0 && i == 1) {
                    DozeSensors.this.mCallback.onSensorPulse(this.mPulseReason, false, -1.0f, -1.0f, null);
                } else if (this.mCurrentState == 0 || i != 2) {
                    int i2 = this.mCurrentState;
                    if ((i2 == 2 || i2 == 1) && i == 4) {
                        DozeSensors.this.mCallback.onSensorPulse(-1, true, -1.0f, -1.0f, null);
                        i = 3;
                    }
                } else {
                    DozeSensors.this.mCallback.onSensorPulse(this.mPulseReason, true, -1.0f, -1.0f, null);
                }
                this.mCurrentState = i;
            }
        }

        public void setListening(boolean z) {
            if (z) {
                check();
            } else {
                release();
            }
        }

        public int getCurrentState() {
            return this.mCurrentState;
        }

        public void resetCurrentState() {
            this.mCurrentState = 3;
        }

        private void release() {
            if (this.mRegistered && DozeSensors.this.mSensorManager != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Unregister Motion Sensor ");
                sb.append(hashCode());
                Log.d("DozeSensor.MotionCheck", sb.toString());
                DozeSensors.this.mSensorManager.unregisterListener(this);
                this.mRegistered = false;
            }
        }
    }

    private class PickupCheck implements SensorEventListener, Runnable {
        private int mCurrentState;
        private boolean mFinished = false;
        private float mMaxRange;
        private boolean mProximityChecking;
        private boolean mRegistered;
        private int mSensorType = 22;
        private final String mTag = "DozeSensors.PickupCheck";

        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        public void run() {
        }

        public PickupCheck() {
            StringBuilder sb = new StringBuilder();
            sb.append("choose sensor: ");
            sb.append("TYPE_PICK_UP");
            Log.d("DozeSensors", sb.toString());
        }

        public void check() {
            if (!this.mFinished && !this.mRegistered) {
                Sensor defaultSensor = DozeSensors.this.mSensorManager.getDefaultSensor(this.mSensorType);
                String str = "DozeSensors.PickupCheck";
                if (defaultSensor == null) {
                    if (DozeSensors.DEBUG) {
                        Log.d(str, "No sensor found");
                    }
                    finishWithResult(0);
                    return;
                }
                Log.d(str, "sensor registered");
                this.mMaxRange = defaultSensor.getMaximumRange();
                DozeSensors.this.mSensorManager.registerListener(this, defaultSensor, 3, 0, DozeSensors.this.mHandler);
                DozeSensors.this.mHandler.postDelayed(this, 500);
                this.mRegistered = true;
            }
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            StringBuilder sb = new StringBuilder();
            sb.append("onSensorChanged = ");
            sb.append(sensorEvent);
            Log.i("DozeSensors", sb.toString());
            if (!this.mProximityChecking) {
                String str = "DozeSensors.PickupCheck";
                boolean z = false;
                if (sensorEvent.values.length == 0) {
                    if (DozeSensors.DEBUG) {
                        Log.d(str, "Event has no values!");
                    }
                    finishWithResult(0);
                } else {
                    if (DozeSensors.DEBUG) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("Event: value=");
                        sb2.append(sensorEvent.values[0]);
                        sb2.append(" max=");
                        sb2.append(this.mMaxRange);
                        Log.d(str, sb2.toString());
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
            }
        }

        private void finishWithResult(int i) {
            if (this.mRegistered) {
                if (!OpAodUtils.isAlwaysOnEnabled()) {
                    if (this.mCurrentState != 0 && i == 1) {
                        DozeSensors.this.mCallback.onSensorPulse(3, false, -1.0f, -1.0f, null);
                    } else if (i == 2) {
                        DozeSensors.this.mCallback.onSensorPulse(-1, false, -1.0f, -1.0f, null);
                    }
                }
                this.mCurrentState = i;
            }
        }

        public void setListening(boolean z) {
            if (z) {
                check();
            } else {
                release();
            }
        }

        private void release() {
            if (this.mRegistered && DozeSensors.this.mSensorManager != null) {
                Log.d("DozeSensors.PickupCheck", "Unregister P Sensor");
                DozeSensors.this.mSensorManager.unregisterListener(this);
                this.mRegistered = false;
            }
        }
    }

    class PluginSensor extends TriggerSensor implements SensorManagerPlugin.SensorEventListener {
        private long mDebounce;
        final SensorManagerPlugin.Sensor mPluginSensor;
        final /* synthetic */ DozeSensors this$0;

        PluginSensor(DozeSensors dozeSensors, SensorManagerPlugin.Sensor sensor, String str, boolean z, int i, boolean z2, boolean z3) {
            this(dozeSensors, sensor, str, z, i, z2, z3, 0);
        }

        PluginSensor(DozeSensors dozeSensors, SensorManagerPlugin.Sensor sensor, String str, boolean z, int i, boolean z2, boolean z3, long j) {
            DozeSensors dozeSensors2 = dozeSensors;
            this.this$0 = dozeSensors2;
            super(dozeSensors2, null, str, z, i, z2, z3);
            this.mPluginSensor = sensor;
            this.mDebounce = j;
        }

        public void updateListener() {
            if (this.mConfigured) {
                AsyncSensorManager asyncSensorManager = (AsyncSensorManager) this.this$0.mSensorManager;
                String str = "DozeSensors";
                if (this.mRequested && !this.mDisabled && ((enabledBySetting() || this.mIgnoresSetting) && !this.mRegistered)) {
                    asyncSensorManager.registerPluginListener(this.mPluginSensor, this);
                    this.mRegistered = true;
                    if (DozeSensors.DEBUG) {
                        Log.d(str, "registerPluginListener");
                    }
                } else if (this.mRegistered) {
                    asyncSensorManager.unregisterPluginListener(this.mPluginSensor, this);
                    this.mRegistered = false;
                    if (DozeSensors.DEBUG) {
                        Log.d(str, "unregisterPluginListener");
                    }
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("{mRegistered=");
            sb.append(this.mRegistered);
            sb.append(", mRequested=");
            sb.append(this.mRequested);
            sb.append(", mDisabled=");
            sb.append(this.mDisabled);
            sb.append(", mConfigured=");
            sb.append(this.mConfigured);
            sb.append(", mIgnoresSetting=");
            sb.append(this.mIgnoresSetting);
            sb.append(", mSensor=");
            sb.append(this.mPluginSensor);
            sb.append("}");
            return sb.toString();
        }

        private String triggerEventToString(SensorManagerPlugin.SensorEvent sensorEvent) {
            if (sensorEvent == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder("PluginTriggerEvent[");
            sb.append(sensorEvent.getSensor());
            sb.append(',');
            sb.append(sensorEvent.getVendorType());
            if (sensorEvent.getValues() != null) {
                for (float append : sensorEvent.getValues()) {
                    sb.append(',');
                    sb.append(append);
                }
            }
            sb.append(']');
            return sb.toString();
        }

        public void onSensorChanged(SensorManagerPlugin.SensorEvent sensorEvent) {
            DozeLog.traceSensor(this.this$0.mContext, this.mPulseReason);
            this.this$0.mHandler.post(this.this$0.mWakeLock.wrap(new Runnable(sensorEvent) {
                private final /* synthetic */ SensorManagerPlugin.SensorEvent f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    PluginSensor.this.lambda$onSensorChanged$0$DozeSensors$PluginSensor(this.f$1);
                }
            }));
        }

        public /* synthetic */ void lambda$onSensorChanged$0$DozeSensors$PluginSensor(SensorManagerPlugin.SensorEvent sensorEvent) {
            String str = "DozeSensors";
            if (SystemClock.uptimeMillis() < this.this$0.mDebounceFrom + this.mDebounce) {
                if (DozeSensors.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onSensorEvent dropped: ");
                    sb.append(triggerEventToString(sensorEvent));
                    Log.d(str, sb.toString());
                }
                return;
            }
            if (DozeSensors.DEBUG) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("onSensorEvent: ");
                sb2.append(triggerEventToString(sensorEvent));
                Log.d(str, sb2.toString());
            }
            this.this$0.mCallback.onSensorPulse(this.mPulseReason, true, -1.0f, -1.0f, sensorEvent.getValues());
        }
    }

    private class ProxSensor implements SensorEventListener {
        final AlarmTimeout mCooldownTimer;
        Boolean mCurrentlyFar;
        long mLastNear;
        final AlwaysOnDisplayPolicy mPolicy;
        boolean mRegistered;
        boolean mRequested;
        final Sensor mSensor;

        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        public ProxSensor(AlwaysOnDisplayPolicy alwaysOnDisplayPolicy) {
            this.mPolicy = alwaysOnDisplayPolicy;
            this.mCooldownTimer = new AlarmTimeout(DozeSensors.this.mAlarmManager, new OnAlarmListener() {
                public final void onAlarm() {
                    ProxSensor.this.updateRegistered();
                }
            }, "prox_cooldown", DozeSensors.this.mHandler);
            Sensor findSensorWithType = DozeSensors.findSensorWithType(DozeSensors.this.mSensorManager, DozeSensors.this.mContext.getString(R$string.doze_brightness_sensor_type));
            if (findSensorWithType == null) {
                findSensorWithType = DozeSensors.this.mSensorManager.getDefaultSensor(8);
            }
            this.mSensor = findSensorWithType;
        }

        /* access modifiers changed from: 0000 */
        public void setRequested(boolean z) {
            if (this.mRequested == z) {
                DozeSensors.this.mHandler.post(new Runnable() {
                    public final void run() {
                        ProxSensor.this.lambda$setRequested$0$DozeSensors$ProxSensor();
                    }
                });
                return;
            }
            this.mRequested = z;
            updateRegistered();
        }

        public /* synthetic */ void lambda$setRequested$0$DozeSensors$ProxSensor() {
            if (this.mCurrentlyFar != null) {
                DozeSensors.this.mProxCallback.accept(this.mCurrentlyFar);
            }
        }

        /* access modifiers changed from: private */
        public void updateRegistered() {
            setRegistered(this.mRequested && !this.mCooldownTimer.isScheduled());
        }

        private void setRegistered(boolean z) {
            if (this.mRegistered != z) {
                if (DozeSensors.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("setRegistered: ");
                    sb.append(z);
                    sb.append(", ");
                    sb.append(Debug.getCallers(5));
                    Log.d("DozeSensors", sb.toString());
                }
                if (z) {
                    this.mRegistered = DozeSensors.this.mSensorManager.registerListener(this, DozeSensors.this.mSensorManager.getDefaultSensor(33171025), 3, DozeSensors.this.mHandler);
                } else {
                    DozeSensors.this.mSensorManager.unregisterListener(this);
                    this.mRegistered = false;
                    this.mCurrentlyFar = null;
                }
            }
        }

        public void onSensorChanged(SensorEvent sensorEvent) {
            if (DozeSensors.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("onSensorChanged ");
                sb.append(sensorEvent);
                Log.d("DozeSensors", sb.toString());
            }
            boolean z = false;
            if (sensorEvent.values[0] != 1.0f) {
                z = true;
            }
            this.mCurrentlyFar = Boolean.valueOf(z);
            DozeSensors.this.mProxCallback.accept(this.mCurrentlyFar);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            Boolean bool = this.mCurrentlyFar;
            if (bool != null) {
                if (!bool.booleanValue()) {
                    this.mLastNear = elapsedRealtime;
                } else if (this.mCurrentlyFar.booleanValue()) {
                    long j = elapsedRealtime - this.mLastNear;
                    AlwaysOnDisplayPolicy alwaysOnDisplayPolicy = this.mPolicy;
                    if (j < alwaysOnDisplayPolicy.proxCooldownTriggerMs) {
                        this.mCooldownTimer.schedule(alwaysOnDisplayPolicy.proxCooldownPeriodMs, 1);
                        updateRegistered();
                    }
                }
            }
        }

        public String toString() {
            return String.format("{registered=%s, requested=%s, coolingDown=%s, currentlyFar=%s, sensor=%s}", new Object[]{Boolean.valueOf(this.mRegistered), Boolean.valueOf(this.mRequested), Boolean.valueOf(this.mCooldownTimer.isScheduled()), this.mCurrentlyFar, this.mSensor});
        }
    }

    class TriggerSensor extends TriggerEventListener {
        final boolean mConfigured;
        protected boolean mDisabled;
        protected boolean mIgnoresSetting;
        final int mPulseReason;
        protected boolean mRegistered;
        final boolean mReportsTouchCoordinates;
        protected boolean mRequested;
        final boolean mRequiresTouchscreen;
        final Sensor mSensor;
        final String mSetting;
        final boolean mSettingDefault;

        public TriggerSensor(DozeSensors dozeSensors, Sensor sensor, String str, boolean z, int i, boolean z2, boolean z3) {
            this(dozeSensors, sensor, str, true, z, i, z2, z3);
        }

        public TriggerSensor(DozeSensors dozeSensors, Sensor sensor, String str, boolean z, boolean z2, int i, boolean z3, boolean z4) {
            this(sensor, str, z, z2, i, z3, z4, false);
        }

        private TriggerSensor(Sensor sensor, String str, boolean z, boolean z2, int i, boolean z3, boolean z4, boolean z5) {
            this.mSensor = sensor;
            this.mSetting = str;
            this.mSettingDefault = z;
            this.mConfigured = z2;
            this.mPulseReason = i;
            this.mReportsTouchCoordinates = z3;
            this.mRequiresTouchscreen = z4;
            this.mIgnoresSetting = z5;
        }

        public void setListening(boolean z) {
            if (this.mRequested != z) {
                this.mRequested = z;
                updateListener();
            }
        }

        public void setDisabled(boolean z) {
            if (this.mDisabled != z) {
                this.mDisabled = z;
                updateListener();
            }
        }

        public void updateListener() {
            if (this.mConfigured && this.mSensor != null) {
                String str = "DozeSensors";
                if (this.mRequested && !this.mDisabled && ((enabledBySetting() || this.mIgnoresSetting) && !this.mRegistered)) {
                    this.mRegistered = DozeSensors.this.mSensorManager.requestTriggerSensor(this, this.mSensor);
                    if (DozeSensors.DEBUG) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("requestTriggerSensor ");
                        sb.append(this.mRegistered);
                        Log.d(str, sb.toString());
                    }
                } else if (this.mRegistered) {
                    boolean cancelTriggerSensor = DozeSensors.this.mSensorManager.cancelTriggerSensor(this, this.mSensor);
                    if (DozeSensors.DEBUG) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("cancelTriggerSensor ");
                        sb2.append(cancelTriggerSensor);
                        Log.d(str, sb2.toString());
                    }
                    this.mRegistered = false;
                }
            }
        }

        /* access modifiers changed from: protected */
        public boolean enabledBySetting() {
            boolean z = false;
            if (!DozeSensors.this.mConfig.enabled(-2)) {
                return false;
            }
            if (TextUtils.isEmpty(this.mSetting)) {
                return true;
            }
            if (Secure.getIntForUser(DozeSensors.this.mResolver, this.mSetting, this.mSettingDefault ? 1 : 0, -2) != 0) {
                z = true;
            }
            return z;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("{mRegistered=");
            sb.append(this.mRegistered);
            sb.append(", mRequested=");
            sb.append(this.mRequested);
            sb.append(", mDisabled=");
            sb.append(this.mDisabled);
            sb.append(", mConfigured=");
            sb.append(this.mConfigured);
            sb.append(", mIgnoresSetting=");
            sb.append(this.mIgnoresSetting);
            sb.append(", mSensor=");
            sb.append(this.mSensor);
            sb.append("}");
            return sb.toString();
        }

        public void onTrigger(TriggerEvent triggerEvent) {
            DozeLog.traceSensor(DozeSensors.this.mContext, this.mPulseReason);
            DozeSensors.this.mHandler.post(DozeSensors.this.mWakeLock.wrap(new Runnable(triggerEvent) {
                private final /* synthetic */ TriggerEvent f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    TriggerSensor.this.lambda$onTrigger$0$DozeSensors$TriggerSensor(this.f$1);
                }
            }));
        }

        /* JADX WARNING: Removed duplicated region for block: B:17:0x0073  */
        /* JADX WARNING: Removed duplicated region for block: B:19:? A[RETURN, SYNTHETIC] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public /* synthetic */ void lambda$onTrigger$0$DozeSensors$TriggerSensor(android.hardware.TriggerEvent r9) {
            /*
                r8 = this;
                boolean r0 = com.android.systemui.doze.DozeSensors.DEBUG
                if (r0 == 0) goto L_0x0020
                java.lang.StringBuilder r0 = new java.lang.StringBuilder
                r0.<init>()
                java.lang.String r1 = "onTrigger: "
                r0.append(r1)
                java.lang.String r1 = r8.triggerEventToString(r9)
                r0.append(r1)
                java.lang.String r0 = r0.toString()
                java.lang.String r1 = "DozeSensors"
                android.util.Log.d(r1, r0)
            L_0x0020:
                android.hardware.Sensor r0 = r8.mSensor
                r1 = 0
                if (r0 == 0) goto L_0x0049
                int r0 = r0.getType()
                r2 = 25
                if (r0 != r2) goto L_0x0049
                float[] r0 = r9.values
                r0 = r0[r1]
                int r0 = (int) r0
                com.android.systemui.doze.DozeSensors r2 = com.android.systemui.doze.DozeSensors.this
                android.content.Context r2 = r2.mContext
                r3 = 411(0x19b, float:5.76E-43)
                com.android.internal.logging.MetricsLogger.action(r2, r3, r0)
                com.android.systemui.doze.DozeSensors r2 = com.android.systemui.doze.DozeSensors.this
                com.android.systemui.statusbar.phone.DozeParameters r2 = r2.mDozeParameters
                boolean r0 = r2.getPickupSubtypePerformsProxCheck(r0)
                r4 = r0
                goto L_0x004a
            L_0x0049:
                r4 = r1
            L_0x004a:
                r8.mRegistered = r1
                boolean r0 = r8.mReportsTouchCoordinates
                r2 = -1082130432(0xffffffffbf800000, float:-1.0)
                if (r0 == 0) goto L_0x0060
                float[] r0 = r9.values
                int r3 = r0.length
                r5 = 2
                if (r3 < r5) goto L_0x0060
                r1 = r0[r1]
                r2 = 1
                r0 = r0[r2]
                r6 = r0
                r5 = r1
                goto L_0x0062
            L_0x0060:
                r5 = r2
                r6 = r5
            L_0x0062:
                com.android.systemui.doze.DozeSensors r0 = com.android.systemui.doze.DozeSensors.this
                com.android.systemui.doze.DozeSensors$Callback r2 = r0.mCallback
                int r3 = r8.mPulseReason
                float[] r7 = r9.values
                r2.onSensorPulse(r3, r4, r5, r6, r7)
                boolean r9 = r8.mRegistered
                if (r9 != 0) goto L_0x0076
                r8.updateListener()
            L_0x0076:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.doze.DozeSensors.TriggerSensor.lambda$onTrigger$0$DozeSensors$TriggerSensor(android.hardware.TriggerEvent):void");
        }

        public void registerSettingsObserver(ContentObserver contentObserver) {
            if (this.mConfigured && !TextUtils.isEmpty(this.mSetting)) {
                DozeSensors.this.mResolver.registerContentObserver(Secure.getUriFor(this.mSetting), false, DozeSensors.this.mSettingsObserver, -1);
            }
        }

        /* access modifiers changed from: protected */
        public String triggerEventToString(TriggerEvent triggerEvent) {
            if (triggerEvent == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder("SensorEvent[");
            sb.append(triggerEvent.timestamp);
            sb.append(',');
            sb.append(triggerEvent.sensor.getName());
            if (triggerEvent.values != null) {
                for (float append : triggerEvent.values) {
                    sb.append(',');
                    sb.append(append);
                }
            }
            sb.append(']');
            return sb.toString();
        }
    }

    public DozeSensors(Context context, AlarmManager alarmManager, SensorManager sensorManager, DozeParameters dozeParameters, AmbientDisplayConfiguration ambientDisplayConfiguration, WakeLock wakeLock, Callback callback, Consumer<Boolean> consumer, AlwaysOnDisplayPolicy alwaysOnDisplayPolicy) {
        this.mContext = context;
        this.mAlarmManager = alarmManager;
        this.mSensorManager = sensorManager;
        this.mDozeParameters = dozeParameters;
        this.mConfig = ambientDisplayConfiguration;
        this.mWakeLock = wakeLock;
        this.mProxCallback = consumer;
        this.mResolver = this.mContext.getContentResolver();
        boolean alwaysOnEnabled = this.mConfig.alwaysOnEnabled(-2);
        TriggerSensor[] triggerSensorArr = new TriggerSensor[7];
        TriggerSensor triggerSensor = new TriggerSensor(this, this.mSensorManager.getDefaultSensor(17), null, dozeParameters.getPulseOnSigMotion(), 2, false, false);
        triggerSensorArr[0] = triggerSensor;
        TriggerSensor triggerSensor2 = new TriggerSensor(this, this.mSensorManager.getDefaultSensor(25), "doze_pulse_on_pick_up", ambientDisplayConfiguration.dozePickupSensorAvailable(), 3, false, false);
        this.mPickupSensor = triggerSensor2;
        triggerSensorArr[1] = triggerSensor2;
        TriggerSensor triggerSensor3 = new TriggerSensor(this, findSensorWithType(ambientDisplayConfiguration.doubleTapSensorType()), "doze_pulse_on_double_tap", true, 4, dozeParameters.doubleTapReportsTouchCoordinates(), true);
        triggerSensorArr[2] = triggerSensor3;
        TriggerSensor triggerSensor4 = new TriggerSensor(this, findSensorWithType(ambientDisplayConfiguration.tapSensorType()), "doze_tap_gesture", true, 9, false, true);
        triggerSensorArr[3] = triggerSensor4;
        TriggerSensor triggerSensor5 = new TriggerSensor(this, findSensorWithType(ambientDisplayConfiguration.longPressSensorType()), "doze_pulse_on_long_press", false, true, 5, true, true);
        triggerSensorArr[4] = triggerSensor5;
        PluginSensor pluginSensor = new PluginSensor(this, new SensorManagerPlugin.Sensor(2), "doze_wake_screen_gesture", this.mConfig.wakeScreenGestureAvailable() && alwaysOnEnabled, 7, false, false);
        triggerSensorArr[5] = pluginSensor;
        PluginSensor pluginSensor2 = new PluginSensor(this, new SensorManagerPlugin.Sensor(1), "doze_wake_screen_gesture", this.mConfig.wakeScreenGestureAvailable() && alwaysOnEnabled, 8, false, false, this.mConfig.getWakeLockScreenDebounce());
        triggerSensorArr[6] = pluginSensor2;
        this.mSensors = triggerSensorArr;
        boolean isPulsingBlocked = ((DozeHost) ((SystemUIApplication) this.mContext.getApplicationContext()).getComponent(DozeHost.class)).isPulsingBlocked();
        if (!OpFeatures.isSupport(new int[]{80}) || isPulsingBlocked) {
            this.mPickUpCheck = new PickupCheck();
        } else {
            this.mMotionCheck = new MotionCheck(true, 3);
            this.mCustomProximityCheck = new CustomProximityCheck() {
                public void onProximityResult(int i) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onProximityResult: ");
                    sb.append(i);
                    String str = "DozeSensors";
                    Log.d(str, sb.toString());
                    int access$100 = DozeSensors.this.mProximityResult;
                    DozeSensors.this.mProximityResult = i;
                    if (access$100 == 1 && i == 2 && DozeSensors.this.mMotionCheck.getCurrentState() == 1) {
                        DozeSensors.this.mMotionCheck.resetCurrentState();
                        Log.d(str, "pulse from pocket");
                        DozeSensors.this.mCallback.onSensorPulse(3, false, -1.0f, -1.0f, null);
                    }
                }
            };
        }
        this.mProxSensor = new ProxSensor(alwaysOnDisplayPolicy);
        this.mCallback = callback;
    }

    public int getCustomProximityResult() {
        return this.mProximityResult;
    }

    public void requestTemporaryDisable() {
        this.mDebounceFrom = SystemClock.uptimeMillis();
    }

    private Sensor findSensorWithType(String str) {
        return findSensorWithType(this.mSensorManager, str);
    }

    static Sensor findSensorWithType(SensorManager sensorManager, String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        for (Sensor sensor : sensorManager.getSensorList(-1)) {
            if (str.equals(sensor.getStringType())) {
                return sensor;
            }
        }
        return null;
    }

    public void setListening(boolean z) {
        if (OpAodUtils.isMotionAwakeOn()) {
            MotionCheck motionCheck = this.mMotionCheck;
            if (motionCheck != null) {
                motionCheck.setListening(z);
            }
            PickupCheck pickupCheck = this.mPickUpCheck;
            if (pickupCheck != null) {
                pickupCheck.setListening(z);
            }
        }
        for (TriggerSensor listening : this.mSensors) {
            listening.setListening(z);
        }
        registerSettingsObserverIfNeeded(z);
    }

    public void setTouchscreenSensorsListening(boolean z) {
        TriggerSensor[] triggerSensorArr;
        for (TriggerSensor triggerSensor : this.mSensors) {
            if (triggerSensor.mRequiresTouchscreen) {
                triggerSensor.setListening(z);
            }
        }
    }

    public void onUserSwitched() {
        for (TriggerSensor updateListener : this.mSensors) {
            updateListener.updateListener();
        }
    }

    public void setCustomProxListening(boolean z) {
        if (shouldListenProximity()) {
            CustomProximityCheck customProximityCheck = this.mCustomProximityCheck;
            if (customProximityCheck != null) {
                customProximityCheck.setListening(z);
            }
        }
    }

    private boolean shouldListenProximity() {
        if (OpFeatures.isSupport(new int[]{114})) {
            if (OpFeatures.isSupport(new int[]{80})) {
                return false;
            }
        }
        return true;
    }

    public void setProxListening(boolean z) {
        if (shouldListenProximity()) {
            this.mProxSensor.setRequested(z);
        }
    }

    public void setDisableSensorsInterferingWithProximity(boolean z) {
        this.mPickupSensor.setDisabled(z);
    }

    public void dump(PrintWriter printWriter) {
        TriggerSensor[] triggerSensorArr;
        for (TriggerSensor triggerSensor : this.mSensors) {
            printWriter.print("  Sensor: ");
            printWriter.println(triggerSensor.toString());
        }
        printWriter.print("  ProxSensor: ");
        printWriter.println(this.mProxSensor.toString());
    }

    public Boolean isProximityCurrentlyFar() {
        return this.mProxSensor.mCurrentlyFar;
    }

    public void resetMotionValue() {
        MotionCheck motionCheck = this.mMotionCheck;
        if (motionCheck != null) {
            motionCheck.resetCurrentState();
        }
    }

    private void registerSettingsObserverIfNeeded(boolean z) {
        if (!z) {
            this.mResolver.unregisterContentObserver(this.mSettingsObserver);
        } else if (!this.mSettingRegistered) {
            for (TriggerSensor registerSettingsObserver : this.mSensors) {
                registerSettingsObserver.registerSettingsObserver(this.mSettingsObserver);
            }
        }
        this.mSettingRegistered = z;
    }
}

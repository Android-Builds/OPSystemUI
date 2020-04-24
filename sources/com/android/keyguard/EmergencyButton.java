package com.android.keyguard;

import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.telephony.ServiceState;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.widget.Button;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.util.EmergencyAffordanceManager;
import com.android.internal.widget.LockPatternUtils;
import com.oneplus.util.OpUtils;

public class EmergencyButton extends Button {
    private static final Intent INTENT_EMERGENCY_DIAL = new Intent().setAction("com.android.phone.EmergencyDialer.DIAL").setPackage("com.android.phone").setFlags(343932928).putExtra("com.android.phone.EmergencyDialer.extra.ENTRY_TYPE", 1);
    /* access modifiers changed from: private */
    public boolean mDebounce;
    private int mDownX;
    private int mDownY;
    /* access modifiers changed from: private */
    public final EmergencyAffordanceManager mEmergencyAffordanceManager;
    /* access modifiers changed from: private */
    public EmergencyButtonCallback mEmergencyButtonCallback;
    private final boolean mEnableEmergencyCallWhileSimLocked;
    private Handler mHandler;
    KeyguardUpdateMonitorCallback mInfoCallback;
    private final boolean mIsVoiceCapable;
    private LockPatternUtils mLockPatternUtils;
    /* access modifiers changed from: private */
    public boolean mLongPressWasDragged;
    private PowerManager mPowerManager;

    public interface EmergencyButtonCallback {
        boolean isShowEmergencyPanel();

        void onEmergencyButtonClickedWhenInCall();

        void onLaunchEmergencyPanel();
    }

    public EmergencyButton(Context context) {
        this(context, null);
    }

    public EmergencyButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDebounce = false;
        this.mHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message message) {
                if (message.what == 100) {
                    EmergencyButton.this.mDebounce = false;
                }
            }
        };
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onSimStateChanged(int i, int i2, State state) {
                EmergencyButton.this.updateEmergencyCallButton();
            }

            public void onPhoneStateChanged(int i) {
                EmergencyButton.this.updateEmergencyCallButton();
            }

            public void onServiceStateChanged(int i, ServiceState serviceState) {
                EmergencyButton.this.updateEmergencyCallButton();
            }
        };
        this.mIsVoiceCapable = context.getResources().getBoolean(17891576);
        this.mEnableEmergencyCallWhileSimLocked = this.mContext.getResources().getBoolean(17891455);
        this.mEmergencyAffordanceManager = new EmergencyAffordanceManager(context);
        if (OpUtils.isCustomFingerprint()) {
            setPaintFlags(getPaintFlags() | 8);
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mInfoCallback);
        this.mHandler.removeMessages(100);
        this.mDebounce = false;
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mInfoCallback);
        this.mHandler.removeMessages(100);
        this.mDebounce = false;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (EmergencyButton.this.mEmergencyButtonCallback == null) {
                    EmergencyButton.this.takeEmergencyCallAction();
                    return;
                }
                if (Build.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(" isShowEmergencyPanel:");
                    sb.append(EmergencyButton.this.mEmergencyButtonCallback.isShowEmergencyPanel());
                    Log.i("EmergencyButton", sb.toString());
                }
                if (!EmergencyButton.this.mEmergencyButtonCallback.isShowEmergencyPanel() || EmergencyButton.this.isInCall()) {
                    if (!KeyguardUpdateMonitor.getInstance(EmergencyButton.this.mContext).isKeyguardVisible() && EmergencyButton.this.mEmergencyButtonCallback != null) {
                        EmergencyButton.this.mEmergencyButtonCallback.onEmergencyButtonClickedWhenInCall();
                    }
                    EmergencyButton.this.takeEmergencyCallAction();
                } else {
                    EmergencyButton.this.mEmergencyButtonCallback.onLaunchEmergencyPanel();
                }
            }
        });
        setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View view) {
                if (EmergencyButton.this.mLongPressWasDragged || !EmergencyButton.this.mEmergencyAffordanceManager.needsEmergencyAffordance()) {
                    return false;
                }
                EmergencyButton.this.mEmergencyAffordanceManager.performEmergencyCall();
                return true;
            }
        });
        updateEmergencyCallButton();
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        if (motionEvent.getActionMasked() == 0) {
            this.mDownX = x;
            this.mDownY = y;
            this.mLongPressWasDragged = false;
        } else {
            int abs = Math.abs(x - this.mDownX);
            int abs2 = Math.abs(y - this.mDownY);
            int scaledTouchSlop = ViewConfiguration.get(this.mContext).getScaledTouchSlop();
            if (Math.abs(abs2) > scaledTouchSlop || Math.abs(abs) > scaledTouchSlop) {
                this.mLongPressWasDragged = true;
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    public boolean performLongClick() {
        return super.performLongClick();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateEmergencyCallButton();
    }

    public void takeEmergencyCallAction() {
        if (!this.mDebounce) {
            this.mDebounce = true;
            this.mHandler.removeMessages(100);
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(100), 500);
            MetricsLogger.action(this.mContext, 200);
            this.mPowerManager.userActivity(SystemClock.uptimeMillis(), true);
            try {
                ActivityTaskManager.getService().stopSystemLockTaskMode();
            } catch (RemoteException unused) {
                Slog.w("EmergencyButton", "Failed to stop app pinning");
            }
            if (isInCall()) {
                resumeCall();
                EmergencyButtonCallback emergencyButtonCallback = this.mEmergencyButtonCallback;
                if (emergencyButtonCallback != null) {
                    emergencyButtonCallback.onEmergencyButtonClickedWhenInCall();
                }
            } else {
                KeyguardUpdateMonitor.getInstance(this.mContext).reportEmergencyCallAction(true);
                getContext().startActivityAsUser(INTENT_EMERGENCY_DIAL, ActivityOptions.makeCustomAnimation(getContext(), 0, 0).toBundle(), new UserHandle(KeyguardUpdateMonitor.getCurrentUser()));
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0054, code lost:
        if (r3.isOOS() == false) goto L_0x005a;
     */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x005c  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0070  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateEmergencyCallButton() {
        /*
            r5 = this;
            boolean r0 = r5.mIsVoiceCapable
            r1 = 1
            r2 = 0
            if (r0 == 0) goto L_0x0059
            boolean r0 = r5.isInCall()
            if (r0 == 0) goto L_0x000d
            goto L_0x005a
        L_0x000d:
            android.content.Context r0 = r5.mContext
            com.android.keyguard.KeyguardUpdateMonitor r0 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r0)
            boolean r0 = r0.isSimPinVoiceSecure()
            if (r0 == 0) goto L_0x001c
            boolean r0 = r5.mEnableEmergencyCallWhileSimLocked
            goto L_0x003a
        L_0x001c:
            com.android.internal.widget.LockPatternUtils r0 = r5.mLockPatternUtils
            int r3 = com.android.keyguard.KeyguardUpdateMonitor.getCurrentUser()
            boolean r0 = r0.isSecure(r3)
            if (r0 != 0) goto L_0x0039
            android.content.Context r0 = r5.mContext
            android.content.res.Resources r0 = r0.getResources()
            int r3 = com.android.keyguard.R$bool.config_showEmergencyButton
            boolean r0 = r0.getBoolean(r3)
            if (r0 == 0) goto L_0x0037
            goto L_0x0039
        L_0x0037:
            r0 = r2
            goto L_0x003a
        L_0x0039:
            r0 = r1
        L_0x003a:
            android.content.Context r3 = r5.mContext
            android.content.res.Resources r3 = r3.getResources()
            int r4 = com.android.keyguard.R$bool.kg_hide_emgcy_btn_when_oos
            boolean r3 = r3.getBoolean(r4)
            if (r3 == 0) goto L_0x0057
            android.content.Context r3 = r5.mContext
            com.android.keyguard.KeyguardUpdateMonitor r3 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r3)
            if (r0 == 0) goto L_0x0059
            boolean r0 = r3.isOOS()
            if (r0 != 0) goto L_0x0059
            goto L_0x005a
        L_0x0057:
            r1 = r0
            goto L_0x005a
        L_0x0059:
            r1 = r2
        L_0x005a:
            if (r1 == 0) goto L_0x0070
            r5.setVisibility(r2)
            boolean r0 = r5.isInCall()
            if (r0 == 0) goto L_0x0069
            r0 = 17040289(0x10403a1, float:2.4247175E-38)
            goto L_0x006c
        L_0x0069:
            r0 = 17040262(0x1040386, float:2.42471E-38)
        L_0x006c:
            r5.setText(r0)
            goto L_0x0075
        L_0x0070:
            r0 = 8
            r5.setVisibility(r0)
        L_0x0075:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.EmergencyButton.updateEmergencyCallButton():void");
    }

    public void setCallback(EmergencyButtonCallback emergencyButtonCallback) {
        this.mEmergencyButtonCallback = emergencyButtonCallback;
    }

    private void resumeCall() {
        if (getTelecommManager() == null) {
            Log.d("EmergencyButton", " resumeCall: getTelecommManager is null");
        } else {
            getTelecommManager().showInCallScreen(false);
        }
    }

    /* access modifiers changed from: private */
    public boolean isInCall() {
        if (getTelecommManager() != null) {
            return getTelecommManager().isInCall();
        }
        Log.d("EmergencyButton", " isInCall: getTelecommManager is null");
        return false;
    }

    private TelecomManager getTelecommManager() {
        return (TelecomManager) this.mContext.getSystemService("telecom");
    }
}

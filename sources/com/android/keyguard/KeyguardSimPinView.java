package com.android.keyguard;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.telephony.ITelephony.Stub;
import com.android.internal.telephony.IccCardConstants.State;
import com.oneplus.keyguard.OpBounerMessageAreaInfo;
import com.oneplus.keyguard.OpKeyguardSimPinView;

public class KeyguardSimPinView extends OpKeyguardSimPinView {
    /* access modifiers changed from: private */
    public CheckSimPin mCheckSimPinThread;
    /* access modifiers changed from: private */
    public int mRemainingAttempts;
    private AlertDialog mRemainingAttemptsDialog;
    /* access modifiers changed from: private */
    public int mRetryCount;
    /* access modifiers changed from: private */
    public boolean mShowDefaultMessage;
    private ImageView mSimImageView;
    /* access modifiers changed from: private */
    public ProgressDialog mSimUnlockProgressDialog;
    /* access modifiers changed from: private */
    public int mSlotId;
    /* access modifiers changed from: private */
    public int mSubId;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    /* renamed from: com.android.keyguard.KeyguardSimPinView$4 */
    static /* synthetic */ class C05994 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[State.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(8:0|1|2|3|4|5|6|8) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        static {
            /*
                com.android.internal.telephony.IccCardConstants$State[] r0 = com.android.internal.telephony.IccCardConstants.State.values()
                int r0 = r0.length
                int[] r0 = new int[r0]
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State = r0
                int[] r0 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x0014 }
                com.android.internal.telephony.IccCardConstants$State r1 = com.android.internal.telephony.IccCardConstants.State.ABSENT     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.internal.telephony.IccCardConstants$State r1 = com.android.internal.telephony.IccCardConstants.State.PUK_REQUIRED     // Catch:{ NoSuchFieldError -> 0x001f }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x001f }
                r2 = 2
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x001f }
            L_0x001f:
                int[] r0 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x002a }
                com.android.internal.telephony.IccCardConstants$State r1 = com.android.internal.telephony.IccCardConstants.State.READY     // Catch:{ NoSuchFieldError -> 0x002a }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x002a }
                r2 = 3
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x002a }
            L_0x002a:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardSimPinView.C05994.<clinit>():void");
        }
    }

    private abstract class CheckSimPin extends Thread {
        private final String mPin;
        private int mSubId;

        /* access modifiers changed from: 0000 */
        public abstract void onSimCheckResponse(int i, int i2);

        protected CheckSimPin(String str, int i) {
            this.mPin = str;
            this.mSubId = i;
        }

        public void run() {
            String str = "KeyguardSimPinView";
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("call supplyPinReportResultForSubscriber(subid=");
                sb.append(this.mSubId);
                sb.append(")");
                Log.v(str, sb.toString());
                final int[] supplyPinReportResultForSubscriber = Stub.asInterface(ServiceManager.checkService("phone")).supplyPinReportResultForSubscriber(this.mSubId, this.mPin);
                StringBuilder sb2 = new StringBuilder();
                sb2.append("supplyPinReportResult returned: ");
                sb2.append(supplyPinReportResultForSubscriber[0]);
                sb2.append(" ");
                sb2.append(supplyPinReportResultForSubscriber[1]);
                Log.v(str, sb2.toString());
                KeyguardSimPinView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPin checkSimPin = CheckSimPin.this;
                        int[] iArr = supplyPinReportResultForSubscriber;
                        checkSimPin.onSimCheckResponse(iArr[0], iArr[1]);
                    }
                });
            } catch (RemoteException e) {
                Log.e(str, "RemoteException for supplyPinReportResult:", e);
                KeyguardSimPinView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPin.this.onSimCheckResponse(2, -1);
                    }
                });
            }
        }
    }

    /* access modifiers changed from: protected */
    public int getPromptReasonStringRes(int i) {
        return 0;
    }

    public boolean isShowEmergencyPanel() {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean shouldLockout(long j) {
        return false;
    }

    public void startAppearAnimation() {
    }

    public boolean startDisappearAnimation(Runnable runnable) {
        return false;
    }

    public KeyguardSimPinView(Context context) {
        this(context, null);
    }

    public KeyguardSimPinView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mSimUnlockProgressDialog = null;
        this.mShowDefaultMessage = true;
        this.mRemainingAttempts = -1;
        this.mSubId = -1;
        this.mRetryCount = 0;
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            public void onSimStateChanged(int i, int i2, State state) {
                StringBuilder sb = new StringBuilder();
                sb.append("onSimStateChanged(subId=");
                sb.append(i);
                sb.append(",slotId=");
                sb.append(i2);
                sb.append(",state=");
                sb.append(state);
                sb.append(") , mSub=");
                sb.append(KeyguardSimPinView.this.mSubId);
                Log.v("KeyguardSimPinView", sb.toString());
                boolean isSimPinSecure = KeyguardUpdateMonitor.isSimPinSecure(KeyguardUpdateMonitor.getInstance(KeyguardSimPinView.this.getContext()).getSimState(KeyguardSimPinView.this.mSubId));
                if (i != KeyguardSimPinView.this.mSubId) {
                    if (i2 == KeyguardSimPinView.this.mSlotId || !isSimPinSecure) {
                        KeyguardSimPinView.this.mShowDefaultMessage = true;
                    } else {
                        return;
                    }
                }
                int i3 = C05994.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()];
                if (i3 == 1) {
                    KeyguardSimPinView.this.mRemainingAttempts = -1;
                    KeyguardUpdateMonitor.getInstance(KeyguardSimPinView.this.getContext()).reportSimUnlocked(i);
                    KeyguardSecurityCallback keyguardSecurityCallback = KeyguardSimPinView.this.mCallback;
                    if (keyguardSecurityCallback != null) {
                        keyguardSecurityCallback.userActivity();
                        KeyguardSimPinView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                    }
                    KeyguardSimPinView.this.mShowDefaultMessage = true;
                    KeyguardSimPinView.this.mRetryCount = 0;
                } else if (i3 == 2) {
                    KeyguardSimPinView.this.mShowDefaultMessage = true;
                    KeyguardSimPinView.this.resetState();
                } else if (i3 != 3) {
                    KeyguardSimPinView.this.resetState();
                } else {
                    KeyguardSimPinView.this.mRemainingAttempts = -1;
                    KeyguardSimPinView.this.resetState();
                }
            }
        };
    }

    public void resetState() {
        super.resetState();
        StringBuilder sb = new StringBuilder();
        sb.append("Resetting state, default=");
        sb.append(this.mShowDefaultMessage);
        Log.v("KeyguardSimPinView", sb.toString());
        handleSubInfoChangeIfNeeded();
        int i = 0;
        if (this.mShowDefaultMessage) {
            this.mRetryCount = 0;
            showDefaultMessage(false);
        }
        boolean isEsimLocked = KeyguardEsimArea.isEsimLocked(this.mContext, this.mSubId);
        KeyguardEsimArea keyguardEsimArea = (KeyguardEsimArea) findViewById(R$id.keyguard_esim_area);
        if (!isEsimLocked) {
            i = 8;
        }
        keyguardEsimArea.setVisibility(i);
    }

    private void setLockedSimMessage() {
        String str;
        this.mSlotId = SubscriptionManager.getSlotIndex(this.mSubId);
        String str2 = "KeyguardSimPinView";
        if (this.mSlotId == -1) {
            Log.w(str2, "get invalid slot index");
            this.mSlotId = KeyguardUpdateMonitor.getInstance(this.mContext).getSimSlotId(this.mSubId);
        }
        boolean isEsimLocked = KeyguardEsimArea.isEsimLocked(this.mContext, this.mSubId);
        int simCount = TelephonyManager.getDefault().getSimCount();
        Resources resources = getResources();
        int i = 1;
        TypedArray obtainStyledAttributes = this.mContext.obtainStyledAttributes(new int[]{R$attr.wallpaperTextColor});
        int color = obtainStyledAttributes.getColor(0, -1);
        obtainStyledAttributes.recycle();
        TextView textView = (TextView) findViewById(R$id.slot_id_name);
        if (simCount < 2) {
            str = resources.getString(R$string.kg_sim_pin_instructions);
            textView.setVisibility(4);
        } else {
            SubscriptionInfo subscriptionInfoForSubId = KeyguardUpdateMonitor.getInstance(this.mContext).getSubscriptionInfoForSubId(this.mSubId);
            String string = resources.getString(R$string.kg_sim_pin_instructions_multi, new Object[]{subscriptionInfoForSubId != null ? subscriptionInfoForSubId.getDisplayName() : ""});
            if (subscriptionInfoForSubId != null) {
                color = subscriptionInfoForSubId.getIconTint();
            }
            textView.setText(this.mContext.getString(R$string.kg_slot_name, new Object[]{Integer.valueOf(this.mSlotId + 1)}));
            textView.setVisibility(0);
            str = string;
        }
        if (isEsimLocked) {
            str = resources.getString(R$string.kg_sim_lock_esim_instructions, new Object[]{str});
        } else {
            i = 0;
        }
        SecurityMessageDisplay securityMessageDisplay = this.mSecurityMessageDisplay;
        if (securityMessageDisplay != null) {
            securityMessageDisplay.setMessage((CharSequence) str, i);
        }
        this.mSimImageView.setImageTintList(ColorStateList.valueOf(color));
        StringBuilder sb = new StringBuilder();
        sb.append("mSubId=");
        sb.append(this.mSubId);
        sb.append(" , slot=");
        sb.append(this.mSlotId);
        sb.append(", esim:");
        sb.append(isEsimLocked);
        Log.d(str2, sb.toString());
    }

    /* access modifiers changed from: private */
    public void showDefaultMessage(boolean z) {
        KeyguardUpdateMonitor instance = KeyguardUpdateMonitor.getInstance(this.mContext);
        String str = "KeyguardSimPinView";
        if (!instance.isSimPinSecure()) {
            Log.d(str, "return when no simpin");
            return;
        }
        int nextSubIdForState = instance.getNextSubIdForState(State.PIN_REQUIRED);
        if (this.mSubId != nextSubIdForState && SubscriptionManager.isValidSubscriptionId(nextSubIdForState)) {
            this.mSubId = nextSubIdForState;
            StringBuilder sb = new StringBuilder();
            sb.append("change subId to ");
            sb.append(nextSubIdForState);
            Log.d(str, sb.toString());
            z = true;
        }
        TextView textView = (TextView) findViewById(R$id.slot_id_name);
        if (SubscriptionManager.isValidSubscriptionId(this.mSubId)) {
            int i = this.mRemainingAttempts;
            if (i < 0 || z) {
                setLockedSimMessage();
                new CheckSimPin("", this.mSubId) {
                    /* access modifiers changed from: 0000 */
                    public void onSimCheckResponse(int i, int i2) {
                        String str = "KeyguardSimPinView";
                        if (i == 2 && i2 == 0 && KeyguardSimPinView.this.mRetryCount <= 10) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("PIN_GENERAL_FAILURE, retry again, ");
                            sb.append(KeyguardSimPinView.this.mRetryCount);
                            Log.w(str, sb.toString());
                            KeyguardSimPinView.this.mRetryCount = KeyguardSimPinView.this.mRetryCount + 1;
                            KeyguardSimPinView.this.postDelayed(new Runnable() {
                                public void run() {
                                    KeyguardSimPinView.this.showDefaultMessage(true);
                                }
                            }, 100);
                        }
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("onSimCheckResponse  dummy One result");
                        sb2.append(i);
                        sb2.append(" attemptsRemaining=");
                        sb2.append(i2);
                        Log.d(str, sb2.toString());
                        if (i2 >= 0) {
                            KeyguardSimPinView.this.mRemainingAttempts = i2;
                            OpBounerMessageAreaInfo access$600 = KeyguardSimPinView.this.getPinPasswordErrorMessageObject(i2, true);
                            SecurityMessageDisplay securityMessageDisplay = KeyguardSimPinView.this.mSecurityMessageDisplay;
                            if (securityMessageDisplay != null && access$600 != null) {
                                securityMessageDisplay.setMessage((CharSequence) access$600.getDisplayMessage(), access$600.getType());
                            }
                        }
                    }
                }.start();
                return;
            }
            OpBounerMessageAreaInfo pinPasswordErrorMessageObject = getPinPasswordErrorMessageObject(i, true);
            SecurityMessageDisplay securityMessageDisplay = this.mSecurityMessageDisplay;
            if (!(securityMessageDisplay == null || pinPasswordErrorMessageObject == null)) {
                securityMessageDisplay.setMessage((CharSequence) pinPasswordErrorMessageObject.getDisplayMessage(), pinPasswordErrorMessageObject.getType());
            }
        }
    }

    private void handleSubInfoChangeIfNeeded() {
        int nextSubIdForState = KeyguardUpdateMonitor.getInstance(this.mContext).getNextSubIdForState(State.PIN_REQUIRED);
        if (nextSubIdForState != this.mSubId && SubscriptionManager.isValidSubscriptionId(nextSubIdForState)) {
            this.mSubId = nextSubIdForState;
            this.mShowDefaultMessage = true;
            this.mRemainingAttempts = -1;
            StringBuilder sb = new StringBuilder();
            sb.append("subinfo change subId to ");
            sb.append(nextSubIdForState);
            Log.d("KeyguardSimPinView", sb.toString());
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        resetState();
    }

    private String getPinPasswordErrorMessage(int i, boolean z) {
        String str;
        int i2;
        int i3;
        if (i == 0) {
            str = getContext().getString(R$string.kg_password_wrong_pin_code_pukked);
        } else if (i <= 0) {
            str = getContext().getString(z ? R$string.kg_sim_pin_instructions : R$string.kg_password_pin_failed);
        } else if (TelephonyManager.getDefault().getSimCount() > 1) {
            if (z) {
                i3 = R$plurals.kg_password_default_pin_message_multi_sim;
            } else {
                i3 = R$plurals.kg_password_wrong_pin_code_multi_sim;
            }
            str = getContext().getResources().getQuantityString(i3, i, new Object[]{Integer.valueOf(this.mSlotId + 1), Integer.valueOf(i)});
        } else {
            if (z) {
                i2 = R$plurals.kg_password_default_pin_message;
            } else {
                i2 = R$plurals.kg_password_wrong_pin_code;
            }
            str = getContext().getResources().getQuantityString(i2, i, new Object[]{Integer.valueOf(i)});
        }
        if (KeyguardEsimArea.isEsimLocked(this.mContext, this.mSubId)) {
            str = getResources().getString(R$string.kg_sim_lock_esim_instructions, new Object[]{str});
        }
        StringBuilder sb = new StringBuilder();
        sb.append("getPinPasswordErrorMessage: attemptsRemaining=");
        sb.append(i);
        sb.append(" displayMessage=");
        sb.append(str);
        Log.d("KeyguardSimPinView", sb.toString());
        return str;
    }

    /* access modifiers changed from: protected */
    public int getPasswordTextViewId() {
        return R$id.simPinEntry;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        View view = this.mEcaView;
        if (view instanceof EmergencyCarrierArea) {
            ((EmergencyCarrierArea) view).setCarrierTextVisible(true);
        }
        this.mSimImageView = (ImageView) findViewById(R$id.keyguard_sim);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mRetryCount = 0;
        if (this.mShowDefaultMessage) {
            showDefaultMessage(false);
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateMonitorCallback);
    }

    public void onResume(int i) {
        super.onResume(i);
        resetState();
    }

    public void onPause() {
        ProgressDialog progressDialog = this.mSimUnlockProgressDialog;
        if (progressDialog != null) {
            progressDialog.dismiss();
            this.mSimUnlockProgressDialog = null;
        }
    }

    private Dialog getSimUnlockProgressDialog() {
        if (this.mSimUnlockProgressDialog == null) {
            this.mSimUnlockProgressDialog = new ProgressDialog(this.mContext);
            this.mSimUnlockProgressDialog.setMessage(this.mContext.getString(R$string.kg_sim_unlock_progress_dialog_message));
            this.mSimUnlockProgressDialog.setIndeterminate(true);
            this.mSimUnlockProgressDialog.setCancelable(false);
            this.mSimUnlockProgressDialog.getWindow().setType(2009);
        }
        return this.mSimUnlockProgressDialog;
    }

    /* access modifiers changed from: private */
    public Dialog getSimRemainingAttemptsDialog(int i) {
        String pinPasswordErrorMessage = getPinPasswordErrorMessage(i, false);
        AlertDialog alertDialog = this.mRemainingAttemptsDialog;
        if (alertDialog == null) {
            Builder builder = new Builder(this.mContext);
            builder.setMessage(pinPasswordErrorMessage);
            builder.setCancelable(false);
            builder.setNeutralButton(R$string.f53ok, null);
            this.mRemainingAttemptsDialog = builder.create();
            this.mRemainingAttemptsDialog.getWindow().setType(2009);
        } else {
            alertDialog.setMessage(pinPasswordErrorMessage);
        }
        return this.mRemainingAttemptsDialog;
    }

    /* access modifiers changed from: protected */
    public void verifyPasswordAndUnlock() {
        if (this.mPasswordEntry.getText().length() < 4) {
            SecurityMessageDisplay securityMessageDisplay = this.mSecurityMessageDisplay;
            if (securityMessageDisplay != null) {
                securityMessageDisplay.setMessage(R$string.kg_invalid_sim_pin_hint, 1);
            }
            resetPasswordText(true, true);
            this.mCallback.userActivity();
            return;
        }
        getSimUnlockProgressDialog().show();
        if (this.mCheckSimPinThread == null) {
            Log.d("KeyguardSimPinView", "begin verifyPasswordAndUnlock");
            this.mCheckSimPinThread = new CheckSimPin(this.mPasswordEntry.getText(), this.mSubId) {
                /* access modifiers changed from: 0000 */
                public void onSimCheckResponse(final int i, final int i2) {
                    KeyguardSimPinView.this.post(new Runnable() {
                        public void run() {
                            KeyguardSimPinView.this.mRemainingAttempts = i2;
                            if (KeyguardSimPinView.this.mSimUnlockProgressDialog != null) {
                                KeyguardSimPinView.this.mSimUnlockProgressDialog.hide();
                            }
                            KeyguardSimPinView.this.resetPasswordText(true, i != 0);
                            if (i == 0) {
                                KeyguardSimPinView.this.mRemainingAttempts = -1;
                                KeyguardSimPinView.this.mRetryCount = 0;
                                KeyguardSimPinView.this.mShowDefaultMessage = true;
                                KeyguardUpdateMonitor.getInstance(KeyguardSimPinView.this.getContext()).reportSimUnlocked(KeyguardSimPinView.this.mSubId);
                                KeyguardSecurityCallback keyguardSecurityCallback = KeyguardSimPinView.this.mCallback;
                                if (keyguardSecurityCallback != null) {
                                    keyguardSecurityCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                                }
                            } else {
                                KeyguardSimPinView.this.mShowDefaultMessage = false;
                                if (i == 1) {
                                    OpBounerMessageAreaInfo access$800 = KeyguardSimPinView.this.getPinPasswordErrorMessageObject(i2, false);
                                    SecurityMessageDisplay securityMessageDisplay = KeyguardSimPinView.this.mSecurityMessageDisplay;
                                    if (!(securityMessageDisplay == null || access$800 == null)) {
                                        securityMessageDisplay.setMessage((CharSequence) access$800.getDisplayMessage(), access$800.getType());
                                    }
                                    int i = i2;
                                    if (i <= 2) {
                                        KeyguardSimPinView.this.getSimRemainingAttemptsDialog(i).show();
                                    }
                                } else {
                                    KeyguardSimPinView keyguardSimPinView = KeyguardSimPinView.this;
                                    SecurityMessageDisplay securityMessageDisplay2 = keyguardSimPinView.mSecurityMessageDisplay;
                                    if (securityMessageDisplay2 != null) {
                                        securityMessageDisplay2.setMessage((CharSequence) keyguardSimPinView.getContext().getString(R$string.kg_password_pin_failed), 1);
                                    }
                                }
                                StringBuilder sb = new StringBuilder();
                                sb.append("verifyPasswordAndUnlock  CheckSimPin.onSimCheckResponse: ");
                                sb.append(i);
                                sb.append(" attemptsRemaining=");
                                sb.append(i2);
                                Log.d("KeyguardSimPinView", sb.toString());
                            }
                            KeyguardSecurityCallback keyguardSecurityCallback2 = KeyguardSimPinView.this.mCallback;
                            if (keyguardSecurityCallback2 != null) {
                                keyguardSecurityCallback2.userActivity();
                            }
                            KeyguardSimPinView.this.mCheckSimPinThread = null;
                        }
                    });
                }
            };
            this.mCheckSimPinThread.start();
        }
    }

    public CharSequence getTitle() {
        return getContext().getString(17040182);
    }
}

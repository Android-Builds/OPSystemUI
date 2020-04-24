package com.android.keyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.internal.telephony.ITelephony.Stub;
import com.android.internal.telephony.IccCardConstants.State;

public class KeyguardSimPukView extends KeyguardPinBasedInputView {
    /* access modifiers changed from: private */
    public CheckSimPuk mCheckSimPukThread;
    /* access modifiers changed from: private */
    public String mPinText;
    /* access modifiers changed from: private */
    public String mPukText;
    /* access modifiers changed from: private */
    public int mRemainingAttempts;
    private AlertDialog mRemainingAttemptsDialog;
    /* access modifiers changed from: private */
    public boolean mShowDefaultMessage;
    /* access modifiers changed from: private */
    public ImageView mSimImageView;
    /* access modifiers changed from: private */
    public ProgressDialog mSimUnlockProgressDialog;
    /* access modifiers changed from: private */
    public int mSlotId;
    /* access modifiers changed from: private */
    public StateMachine mStateMachine;
    /* access modifiers changed from: private */
    public int mSubId;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    /* renamed from: com.android.keyguard.KeyguardSimPukView$4 */
    static /* synthetic */ class C06054 {
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
                com.android.internal.telephony.IccCardConstants$State r1 = com.android.internal.telephony.IccCardConstants.State.NOT_READY     // Catch:{ NoSuchFieldError -> 0x0014 }
                int r1 = r1.ordinal()     // Catch:{ NoSuchFieldError -> 0x0014 }
                r2 = 1
                r0[r1] = r2     // Catch:{ NoSuchFieldError -> 0x0014 }
            L_0x0014:
                int[] r0 = $SwitchMap$com$android$internal$telephony$IccCardConstants$State     // Catch:{ NoSuchFieldError -> 0x001f }
                com.android.internal.telephony.IccCardConstants$State r1 = com.android.internal.telephony.IccCardConstants.State.ABSENT     // Catch:{ NoSuchFieldError -> 0x001f }
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
            throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardSimPukView.C06054.<clinit>():void");
        }
    }

    private abstract class CheckSimPuk extends Thread {
        private final String mPin;
        private final String mPuk;
        private final int mSubId;

        /* access modifiers changed from: 0000 */
        public abstract void onSimLockChangedResponse(int i, int i2);

        protected CheckSimPuk(String str, String str2, int i) {
            this.mPuk = str;
            this.mPin = str2;
            this.mSubId = i;
        }

        public void run() {
            String str = "KeyguardSimPukView";
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("call supplyPukReportResult() , mSubId = ");
                sb.append(this.mSubId);
                Log.v(str, sb.toString());
                final int[] supplyPukReportResultForSubscriber = Stub.asInterface(ServiceManager.checkService("phone")).supplyPukReportResultForSubscriber(this.mSubId, this.mPuk, this.mPin);
                StringBuilder sb2 = new StringBuilder();
                sb2.append("supplyPukReportResult returned: ");
                sb2.append(supplyPukReportResultForSubscriber[0]);
                sb2.append(" ");
                sb2.append(supplyPukReportResultForSubscriber[1]);
                Log.v(str, sb2.toString());
                KeyguardSimPukView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPuk checkSimPuk = CheckSimPuk.this;
                        int[] iArr = supplyPukReportResultForSubscriber;
                        checkSimPuk.onSimLockChangedResponse(iArr[0], iArr[1]);
                    }
                });
            } catch (RemoteException e) {
                Log.e(str, "RemoteException for supplyPukReportResult:", e);
                KeyguardSimPukView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPuk.this.onSimLockChangedResponse(2, -1);
                    }
                });
            }
        }
    }

    private class StateMachine {
        final int CONFIRM_PIN;
        final int DONE;
        final int ENTER_PIN;
        final int ENTER_PUK;
        private int state;

        private StateMachine() {
            this.ENTER_PUK = 0;
            this.ENTER_PIN = 1;
            this.CONFIRM_PIN = 2;
            this.DONE = 3;
            this.state = 0;
        }

        /* JADX WARNING: Removed duplicated region for block: B:22:0x004f  */
        /* JADX WARNING: Removed duplicated region for block: B:26:? A[RETURN, SYNTHETIC] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void next() {
            /*
                r5 = this;
                int r0 = r5.state
                r1 = 0
                r2 = 1
                if (r0 != 0) goto L_0x001a
                com.android.keyguard.KeyguardSimPukView r0 = com.android.keyguard.KeyguardSimPukView.this
                boolean r0 = r0.checkPuk()
                if (r0 == 0) goto L_0x0016
                r5.state = r2
                int r0 = com.android.keyguard.R$string.kg_puk_enter_pin_hint
            L_0x0012:
                r4 = r1
                r1 = r0
                r0 = r4
                goto L_0x0048
            L_0x0016:
                int r1 = com.android.keyguard.R$string.kg_invalid_sim_puk_hint
            L_0x0018:
                r0 = r2
                goto L_0x0048
            L_0x001a:
                r3 = 2
                if (r0 != r2) goto L_0x002d
                com.android.keyguard.KeyguardSimPukView r0 = com.android.keyguard.KeyguardSimPukView.this
                boolean r0 = r0.checkPin()
                if (r0 == 0) goto L_0x002a
                r5.state = r3
                int r0 = com.android.keyguard.R$string.kg_enter_confirm_pin_hint
                goto L_0x0012
            L_0x002a:
                int r1 = com.android.keyguard.R$string.kg_invalid_sim_pin_hint
                goto L_0x0018
            L_0x002d:
                if (r0 != r3) goto L_0x0047
                com.android.keyguard.KeyguardSimPukView r0 = com.android.keyguard.KeyguardSimPukView.this
                boolean r0 = r0.confirmPin()
                if (r0 == 0) goto L_0x0042
                r0 = 3
                r5.state = r0
                int r0 = com.android.keyguard.R$string.keyguard_sim_unlock_progress_dialog_message
                com.android.keyguard.KeyguardSimPukView r3 = com.android.keyguard.KeyguardSimPukView.this
                r3.updateSim()
                goto L_0x0012
            L_0x0042:
                r5.state = r2
                int r1 = com.android.keyguard.R$string.kg_invalid_confirm_pin_hint
                goto L_0x0018
            L_0x0047:
                r0 = r1
            L_0x0048:
                com.android.keyguard.KeyguardSimPukView r3 = com.android.keyguard.KeyguardSimPukView.this
                r3.resetPasswordText(r2, r2)
                if (r1 == 0) goto L_0x0058
                com.android.keyguard.KeyguardSimPukView r5 = com.android.keyguard.KeyguardSimPukView.this
                com.android.keyguard.SecurityMessageDisplay r5 = r5.mSecurityMessageDisplay
                if (r5 == 0) goto L_0x0058
                r5.setMessage(r1, r0)
            L_0x0058:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardSimPukView.StateMachine.next():void");
        }

        /* access modifiers changed from: 0000 */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x00d7  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void reset() {
            /*
                r9 = this;
                com.android.keyguard.KeyguardSimPukView r0 = com.android.keyguard.KeyguardSimPukView.this
                java.lang.String r1 = ""
                r0.mPinText = r1
                com.android.keyguard.KeyguardSimPukView r0 = com.android.keyguard.KeyguardSimPukView.this
                r0.mPukText = r1
                r0 = 0
                r9.state = r0
                com.android.keyguard.KeyguardSimPukView r1 = com.android.keyguard.KeyguardSimPukView.this
                android.content.Context r1 = r1.mContext
                com.android.keyguard.KeyguardUpdateMonitor r1 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r1)
                com.android.keyguard.KeyguardSimPukView r2 = com.android.keyguard.KeyguardSimPukView.this
                int r3 = com.android.keyguard.R$id.slot_id_name
                android.view.View r2 = r2.findViewById(r3)
                android.widget.TextView r2 = (android.widget.TextView) r2
                com.android.keyguard.KeyguardSimPukView r3 = com.android.keyguard.KeyguardSimPukView.this
                com.android.internal.telephony.IccCardConstants$State r4 = com.android.internal.telephony.IccCardConstants.State.PUK_REQUIRED
                int r4 = r1.getNextSubIdForState(r4)
                r3.mSubId = r4
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "reset, mSubId="
                r3.append(r4)
                com.android.keyguard.KeyguardSimPukView r4 = com.android.keyguard.KeyguardSimPukView.this
                int r4 = r4.mSubId
                r3.append(r4)
                java.lang.String r4 = ", slotId="
                r3.append(r4)
                com.android.keyguard.KeyguardSimPukView r4 = com.android.keyguard.KeyguardSimPukView.this
                int r4 = r4.mSubId
                int r4 = android.telephony.SubscriptionManager.getPhoneId(r4)
                r3.append(r4)
                java.lang.String r3 = r3.toString()
                java.lang.String r4 = "KeyguardSimPukView"
                android.util.Log.d(r4, r3)
                com.android.keyguard.KeyguardSimPukView r3 = com.android.keyguard.KeyguardSimPukView.this
                int r3 = r3.mSubId
                boolean r3 = android.telephony.SubscriptionManager.isValidSubscriptionId(r3)
                if (r3 == 0) goto L_0x00fb
                com.android.keyguard.KeyguardSimPukView r3 = com.android.keyguard.KeyguardSimPukView.this
                android.content.Context r3 = r3.mContext
                int r4 = com.android.keyguard.R$string.kg_slot_name
                r5 = 1
                java.lang.Object[] r6 = new java.lang.Object[r5]
                com.android.keyguard.KeyguardSimPukView r7 = com.android.keyguard.KeyguardSimPukView.this
                int r7 = r7.mSubId
                int r7 = android.telephony.SubscriptionManager.getPhoneId(r7)
                int r7 = r7 + r5
                java.lang.Integer r7 = java.lang.Integer.valueOf(r7)
                r6[r0] = r7
                java.lang.String r3 = r3.getString(r4, r6)
                com.android.keyguard.KeyguardSimPukView r4 = com.android.keyguard.KeyguardSimPukView.this
                int r6 = r4.mSubId
                int r6 = android.telephony.SubscriptionManager.getPhoneId(r6)
                r4.mSlotId = r6
                android.telephony.TelephonyManager r4 = android.telephony.TelephonyManager.getDefault()
                int r4 = r4.getSimCount()
                com.android.keyguard.KeyguardSimPukView r6 = com.android.keyguard.KeyguardSimPukView.this
                android.content.res.Resources r6 = r6.getResources()
                r7 = 2
                r8 = -1
                if (r4 >= r7) goto L_0x00b0
                int r1 = com.android.keyguard.R$string.kg_puk_enter_puk_hint
                java.lang.String r1 = r6.getString(r1)
                r4 = r1
            L_0x00ae:
                r1 = r8
                goto L_0x00cf
            L_0x00b0:
                com.android.keyguard.KeyguardSimPukView r4 = com.android.keyguard.KeyguardSimPukView.this
                int r4 = r4.mSubId
                android.telephony.SubscriptionInfo r1 = r1.getSubscriptionInfoForSubId(r4)
                if (r1 == 0) goto L_0x00bf
                r1.getDisplayName()
            L_0x00bf:
                int r4 = com.android.keyguard.R$string.kg_puk_enter_puk_hint_multi
                java.lang.Object[] r7 = new java.lang.Object[r5]
                r7[r0] = r3
                java.lang.String r4 = r6.getString(r4, r7)
                if (r1 == 0) goto L_0x00ae
                int r1 = r1.getIconTint()
            L_0x00cf:
                com.android.keyguard.KeyguardSimPukView r6 = com.android.keyguard.KeyguardSimPukView.this
                boolean r6 = r6.mShowDefaultMessage
                if (r6 == 0) goto L_0x00e0
                com.android.keyguard.KeyguardSimPukView r6 = com.android.keyguard.KeyguardSimPukView.this
                com.android.keyguard.SecurityMessageDisplay r6 = r6.mSecurityMessageDisplay
                if (r6 == 0) goto L_0x00e0
                r6.setMessage(r4, r5)
            L_0x00e0:
                com.android.keyguard.KeyguardSimPukView r4 = com.android.keyguard.KeyguardSimPukView.this
                r4.mShowDefaultMessage = r5
                com.android.keyguard.KeyguardSimPukView r4 = com.android.keyguard.KeyguardSimPukView.this
                android.widget.ImageView r4 = r4.mSimImageView
                android.content.res.ColorStateList r1 = android.content.res.ColorStateList.valueOf(r1)
                r4.setImageTintList(r1)
                r2.setText(r3)
                r2.setTextColor(r8)
                r2.setVisibility(r0)
            L_0x00fb:
                com.android.keyguard.KeyguardSimPukView r1 = com.android.keyguard.KeyguardSimPukView.this
                android.content.Context r1 = r1.mContext
                com.android.keyguard.KeyguardSimPukView r2 = com.android.keyguard.KeyguardSimPukView.this
                int r2 = r2.mSubId
                boolean r1 = com.android.keyguard.KeyguardEsimArea.isEsimLocked(r1, r2)
                com.android.keyguard.KeyguardSimPukView r2 = com.android.keyguard.KeyguardSimPukView.this
                int r3 = com.android.keyguard.R$id.keyguard_esim_area
                android.view.View r2 = r2.findViewById(r3)
                com.android.keyguard.KeyguardEsimArea r2 = (com.android.keyguard.KeyguardEsimArea) r2
                if (r1 == 0) goto L_0x0118
                goto L_0x011a
            L_0x0118:
                r0 = 8
            L_0x011a:
                r2.setVisibility(r0)
                com.android.keyguard.KeyguardSimPukView r9 = com.android.keyguard.KeyguardSimPukView.this
                com.android.keyguard.PasswordTextView r9 = r9.mPasswordEntry
                r9.requestFocus()
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.KeyguardSimPukView.StateMachine.reset():void");
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

    public KeyguardSimPukView(Context context) {
        this(context, null);
    }

    public KeyguardSimPukView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mSimUnlockProgressDialog = null;
        this.mShowDefaultMessage = true;
        this.mRemainingAttempts = -1;
        this.mStateMachine = new StateMachine();
        this.mSubId = -1;
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            public void onSimStateChanged(int i, int i2, State state) {
                StringBuilder sb = new StringBuilder();
                sb.append("onSimStateChanged(subId=");
                sb.append(i);
                sb.append(",mSubId=");
                sb.append(KeyguardSimPukView.this.mSubId);
                sb.append(",slotId=");
                sb.append(i2);
                sb.append(",mSlotId=");
                sb.append(KeyguardSimPukView.this.mSlotId);
                sb.append(",state=");
                sb.append(state);
                sb.append(")");
                Log.v("KeyguardSimPukView", sb.toString());
                if (i != KeyguardSimPukView.this.mSubId && KeyguardSimPukView.this.mSubId > 0) {
                    if (i2 == KeyguardSimPukView.this.mSlotId) {
                        KeyguardSimPukView.this.mShowDefaultMessage = true;
                    } else {
                        return;
                    }
                }
                int i3 = C06054.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()];
                if (i3 == 1 || i3 == 2 || i3 == 3) {
                    KeyguardUpdateMonitor.getInstance(KeyguardSimPukView.this.getContext()).reportSimUnlocked(i);
                    KeyguardSimPukView.this.mRemainingAttempts = -1;
                    KeyguardSimPukView.this.mShowDefaultMessage = true;
                    KeyguardSecurityCallback keyguardSecurityCallback = KeyguardSimPukView.this.mCallback;
                    if (keyguardSecurityCallback != null) {
                        keyguardSecurityCallback.userActivity();
                        KeyguardSimPukView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                    }
                    KeyguardSimPukView.this.resetState();
                } else {
                    KeyguardSimPukView.this.resetState();
                }
            }
        };
    }

    /* access modifiers changed from: private */
    public String getPukPasswordErrorMessage(int i, boolean z) {
        String str;
        int i2;
        int i3;
        if (i == 0) {
            str = getContext().getString(R$string.kg_password_wrong_puk_code_dead);
        } else if (i > 0) {
            if (z) {
                i3 = R$plurals.kg_password_default_puk_message;
            } else {
                i3 = R$plurals.kg_password_wrong_puk_code;
            }
            str = getContext().getResources().getQuantityString(i3, i, new Object[]{Integer.valueOf(i)});
        } else {
            if (z) {
                i2 = R$string.kg_puk_enter_puk_hint;
            } else {
                i2 = R$string.kg_password_puk_failed;
            }
            str = getContext().getString(i2);
        }
        if (KeyguardEsimArea.isEsimLocked(this.mContext, this.mSubId)) {
            str = getResources().getString(R$string.kg_sim_lock_esim_instructions, new Object[]{str});
        }
        StringBuilder sb = new StringBuilder();
        sb.append("getPukPasswordErrorMessage: attemptsRemaining=");
        sb.append(i);
        sb.append(" displayMessage=");
        sb.append(str);
        Log.d("KeyguardSimPukView", sb.toString());
        return str;
    }

    public void resetState() {
        super.resetState();
        this.mStateMachine.reset();
    }

    /* access modifiers changed from: protected */
    public int getPasswordTextViewId() {
        return R$id.pukEntry;
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
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
        resetState();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateMonitorCallback);
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
            if (!(this.mContext instanceof Activity)) {
                this.mSimUnlockProgressDialog.getWindow().setType(2009);
            }
        }
        return this.mSimUnlockProgressDialog;
    }

    /* access modifiers changed from: private */
    public Dialog getPukRemainingAttemptsDialog(int i) {
        String pukPasswordErrorMessage = getPukPasswordErrorMessage(i, false);
        AlertDialog alertDialog = this.mRemainingAttemptsDialog;
        if (alertDialog == null) {
            Builder builder = new Builder(this.mContext);
            builder.setMessage(pukPasswordErrorMessage);
            builder.setCancelable(false);
            builder.setNeutralButton(R$string.f53ok, null);
            this.mRemainingAttemptsDialog = builder.create();
            this.mRemainingAttemptsDialog.getWindow().setType(2009);
        } else {
            alertDialog.setMessage(pukPasswordErrorMessage);
        }
        return this.mRemainingAttemptsDialog;
    }

    /* access modifiers changed from: private */
    public boolean checkPuk() {
        if (this.mPasswordEntry.getText().length() != 8) {
            return false;
        }
        this.mPukText = this.mPasswordEntry.getText();
        return true;
    }

    /* access modifiers changed from: private */
    public boolean checkPin() {
        int length = this.mPasswordEntry.getText().length();
        if (length < 4 || length > 8) {
            return false;
        }
        this.mPinText = this.mPasswordEntry.getText();
        return true;
    }

    public boolean confirmPin() {
        return this.mPinText.equals(this.mPasswordEntry.getText());
    }

    /* access modifiers changed from: private */
    public void updateSim() {
        getSimUnlockProgressDialog().show();
        if (this.mCheckSimPukThread == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("begin verifyPasswordAndUnlock , mSubId = ");
            sb.append(this.mSubId);
            sb.append(", slot=");
            sb.append(this.mSlotId);
            Log.d("KeyguardSimPukView", sb.toString());
            this.mCheckSimPukThread = new CheckSimPuk(this.mPukText, this.mPinText, this.mSubId) {
                /* access modifiers changed from: 0000 */
                public void onSimLockChangedResponse(final int i, final int i2) {
                    KeyguardSimPukView.this.post(new Runnable() {
                        public void run() {
                            if (KeyguardSimPukView.this.mSimUnlockProgressDialog != null) {
                                KeyguardSimPukView.this.mSimUnlockProgressDialog.hide();
                            }
                            KeyguardSimPukView.this.resetPasswordText(true, i != 0);
                            if (i == 0) {
                                KeyguardUpdateMonitor.getInstance(KeyguardSimPukView.this.getContext()).reportSimUnlocked(KeyguardSimPukView.this.mSubId);
                                KeyguardSimPukView.this.mRemainingAttempts = -1;
                                KeyguardSimPukView.this.mShowDefaultMessage = true;
                                KeyguardSecurityCallback keyguardSecurityCallback = KeyguardSimPukView.this.mCallback;
                                if (keyguardSecurityCallback != null) {
                                    keyguardSecurityCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                                }
                            } else {
                                KeyguardSimPukView.this.mShowDefaultMessage = false;
                                if (i == 1) {
                                    KeyguardSimPukView keyguardSimPukView = KeyguardSimPukView.this;
                                    SecurityMessageDisplay securityMessageDisplay = keyguardSimPukView.mSecurityMessageDisplay;
                                    if (securityMessageDisplay != null) {
                                        securityMessageDisplay.setMessage((CharSequence) keyguardSimPukView.getPukPasswordErrorMessage(i2, false), 1);
                                    }
                                    int i = i2;
                                    if (i <= 2) {
                                        KeyguardSimPukView.this.getPukRemainingAttemptsDialog(i).show();
                                    }
                                } else {
                                    KeyguardSimPukView keyguardSimPukView2 = KeyguardSimPukView.this;
                                    SecurityMessageDisplay securityMessageDisplay2 = keyguardSimPukView2.mSecurityMessageDisplay;
                                    if (securityMessageDisplay2 != null) {
                                        securityMessageDisplay2.setMessage((CharSequence) keyguardSimPukView2.getContext().getString(R$string.kg_password_puk_failed), 1);
                                    }
                                }
                                StringBuilder sb = new StringBuilder();
                                sb.append("verifyPasswordAndUnlock  UpdateSim.onSimCheckResponse:  attemptsRemaining=");
                                sb.append(i2);
                                Log.d("KeyguardSimPukView", sb.toString());
                                KeyguardSimPukView.this.mStateMachine.reset();
                            }
                            KeyguardSimPukView.this.mCheckSimPukThread = null;
                        }
                    });
                }
            };
            this.mCheckSimPukThread.start();
        }
    }

    /* access modifiers changed from: protected */
    public void verifyPasswordAndUnlock() {
        this.mStateMachine.next();
    }

    public CharSequence getTitle() {
        return getContext().getString(17040183);
    }
}

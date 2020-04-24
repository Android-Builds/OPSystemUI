package com.oneplus.keyguard;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import com.android.keyguard.KeyguardAbsKeyInputView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R$id;
import com.android.systemui.R$string;
import com.oneplus.keyguard.OpPasswordTextViewForPin.UserActivityListener;
import com.oneplus.keyguard.OpPasswordTextViewForPin.onTextChangedListerner;

public abstract class OpKeyguardPinBasedInputViewForPin extends KeyguardAbsKeyInputView implements OnKeyListener {
    private static String TAG = "OpKeyguardPinBasedInputViewForPin";
    private View mButton0;
    private View mButton1;
    private View mButton2;
    private View mButton3;
    private View mButton4;
    private View mButton5;
    private View mButton6;
    private View mButton7;
    private View mButton8;
    private View mButton9;
    private TextView mDeleteButton;
    private View mOkButton;
    protected OpPasswordTextViewForPin mPasswordEntry;

    public OpKeyguardPinBasedInputViewForPin(Context context) {
        this(context, null);
    }

    public OpKeyguardPinBasedInputViewForPin(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void reset() {
        this.mPasswordEntry.requestFocus();
        super.reset();
    }

    /* access modifiers changed from: protected */
    public boolean onRequestFocusInDescendants(int i, Rect rect) {
        return this.mPasswordEntry.requestFocus(i, rect);
    }

    /* access modifiers changed from: protected */
    public void resetState() {
        this.mPasswordEntry.setEnabled(true);
    }

    /* access modifiers changed from: protected */
    public void setPasswordEntryEnabled(boolean z) {
        this.mPasswordEntry.setEnabled(z);
    }

    /* access modifiers changed from: protected */
    public void setPasswordEntryInputEnabled(boolean z) {
        this.mPasswordEntry.setEnabled(z);
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (KeyEvent.isConfirmKey(i)) {
            performClick(this.mOkButton);
            return true;
        } else if (i == 67) {
            performClick(this.mDeleteButton);
            return true;
        } else if (i >= 7 && i <= 16) {
            performNumberClick(i - 7);
            return true;
        } else if (i < 144 || i > 153) {
            return super.onKeyDown(i, keyEvent);
        } else {
            performNumberClick(i - 144);
            return true;
        }
    }

    /* access modifiers changed from: protected */
    public int getPromptReasonStringRes(int i) {
        if (i == 0) {
            return 0;
        }
        if (i == 1) {
            return R$string.kg_prompt_reason_restart_pin;
        }
        if (i == 2) {
            return R$string.kg_prompt_reason_timeout_pin;
        }
        if (i == 3) {
            return R$string.kg_prompt_reason_device_admin;
        }
        if (i != 4) {
            return R$string.kg_prompt_reason_timeout_pin;
        }
        return R$string.kg_prompt_reason_user_request;
    }

    private void performClick(View view) {
        if (view != null) {
            view.performClick();
        }
    }

    private void performNumberClick(int i) {
        switch (i) {
            case 0:
                performClick(this.mButton0);
                return;
            case 1:
                performClick(this.mButton1);
                return;
            case 2:
                performClick(this.mButton2);
                return;
            case 3:
                performClick(this.mButton3);
                return;
            case 4:
                performClick(this.mButton4);
                return;
            case 5:
                performClick(this.mButton5);
                return;
            case 6:
                performClick(this.mButton6);
                return;
            case 7:
                performClick(this.mButton7);
                return;
            case 8:
                performClick(this.mButton8);
                return;
            case 9:
                performClick(this.mButton9);
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: protected */
    public void resetPasswordText(boolean z, boolean z2) {
        this.mPasswordEntry.reset(z, z2);
    }

    /* access modifiers changed from: protected */
    public byte[] getPasswordText() {
        return charSequenceToByteArray(this.mPasswordEntry.getText());
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mPasswordEntry = (OpPasswordTextViewForPin) findViewById(getPasswordTextViewId());
        this.mPasswordEntry.setOnKeyListener(this);
        this.mPasswordEntry.setSelected(true);
        this.mPasswordEntry.setUserActivityListener(new UserActivityListener() {
            public void onUserActivity() {
                OpKeyguardPinBasedInputViewForPin.this.onUserInput();
            }

            public void onCheckPasswordAndUnlock() {
                if (OpKeyguardPinBasedInputViewForPin.this.mPasswordEntry.isEnabled()) {
                    OpKeyguardPinBasedInputViewForPin.this.verifyPasswordAndUnlock();
                }
            }
        });
        if (!KeyguardUpdateMonitor.getInstance(getContext()).isAutoCheckPinEnabled()) {
            this.mOkButton = findViewById(R$id.key_enter);
            View view = this.mOkButton;
            if (view != null) {
                view.setOnClickListener(new OnClickListener() {
                    public void onClick(View view) {
                        OpKeyguardPinBasedInputViewForPin.this.doHapticKeyClick();
                        if (OpKeyguardPinBasedInputViewForPin.this.mPasswordEntry.isEnabled()) {
                            OpKeyguardPinBasedInputViewForPin.this.verifyPasswordAndUnlock();
                        }
                    }
                });
                OnHoverListener createLiftToActivateListener = createLiftToActivateListener();
                if (createLiftToActivateListener != null) {
                    this.mOkButton.setOnHoverListener(createLiftToActivateListener);
                }
            }
        }
        this.mDeleteButton = (TextView) findViewById(R$id.deleteOrCancel);
        this.mDeleteButton.setText(getContext().getResources().getString(17039867));
        this.mDeleteButton.setVisibility(0);
        this.mDeleteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (OpKeyguardPinBasedInputViewForPin.this.mPasswordEntry.isEnabled()) {
                    OpKeyguardPinBasedInputViewForPin.this.mPasswordEntry.deleteLastChar();
                }
                OpKeyguardPinBasedInputViewForPin.this.doHapticKeyClick();
            }
        });
        this.mDeleteButton.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View view) {
                if (OpKeyguardPinBasedInputViewForPin.this.mPasswordEntry.isEnabled()) {
                    OpKeyguardPinBasedInputViewForPin.this.resetPasswordText(true, true);
                }
                OpKeyguardPinBasedInputViewForPin.this.doHapticKeyClick();
                return true;
            }
        });
        this.mPasswordEntry.setTextChangeListener(new onTextChangedListerner() {
            public void onTextChanged(String str) {
            }
        });
        this.mButton0 = findViewById(R$id.key0);
        this.mButton1 = findViewById(R$id.key1);
        this.mButton2 = findViewById(R$id.key2);
        this.mButton3 = findViewById(R$id.key3);
        this.mButton4 = findViewById(R$id.key4);
        this.mButton5 = findViewById(R$id.key5);
        this.mButton6 = findViewById(R$id.key6);
        this.mButton7 = findViewById(R$id.key7);
        this.mButton8 = findViewById(R$id.key8);
        this.mButton9 = findViewById(R$id.key9);
        this.mPasswordEntry.requestFocus();
        super.onFinishInflate();
    }

    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (keyEvent.getAction() == 0) {
            return onKeyDown(i, keyEvent);
        }
        return false;
    }

    public CharSequence getTitle() {
        return getContext().getString(17040181);
    }

    private static byte[] charSequenceToByteArray(CharSequence charSequence) {
        if (charSequence == null) {
            return null;
        }
        byte[] bArr = new byte[charSequence.length()];
        for (int i = 0; i < charSequence.length(); i++) {
            bArr[i] = (byte) charSequence.charAt(i);
        }
        return bArr;
    }

    private OnHoverListener createLiftToActivateListener() {
        Class[] clsArr = {Context.class};
        Object createInstanceWithArgs = createInstanceWithArgs(getContext(), "com.android.keyguard.LiftToActivateListener", clsArr, getContext());
        if (createInstanceWithArgs instanceof OnHoverListener) {
            return (OnHoverListener) createInstanceWithArgs;
        }
        return null;
    }

    private Object createInstanceWithArgs(Context context, String str, Class[] clsArr, Object... objArr) {
        if (str == null || str.length() == 0) {
            Log.e(TAG, "invalide class name ");
        }
        try {
            return Class.forName(str).getConstructor(clsArr).newInstance(objArr);
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("createInstanceWithArgs fail:");
            sb.append(str);
            Log.e(str2, sb.toString());
            e.printStackTrace();
            return null;
        }
    }
}

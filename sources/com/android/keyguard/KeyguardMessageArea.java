package com.android.keyguard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.settingslib.Utils;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.oneplus.keyguard.OpKeyguardMessageArea;
import com.oneplus.util.OpUtils;
import java.lang.ref.WeakReference;

public class KeyguardMessageArea extends OpKeyguardMessageArea implements SecurityMessageDisplay, ConfigurationListener {
    private static final Object ANNOUNCE_TOKEN = new Object();
    /* access modifiers changed from: private */
    public boolean mBouncerVisible;
    private final ConfigurationController mConfigurationController;
    private ColorStateList mDefaultColorState;
    private final Handler mHandler;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    private CharSequence mMessage;
    private ColorStateList mNextMessageColorState;

    private static class AnnounceRunnable implements Runnable {
        private final WeakReference<View> mHost;
        private final CharSequence mTextToAnnounce;

        AnnounceRunnable(View view, CharSequence charSequence) {
            this.mHost = new WeakReference<>(view);
            this.mTextToAnnounce = charSequence;
        }

        public void run() {
            View view = (View) this.mHost.get();
            if (view != null) {
                view.announceForAccessibility(this.mTextToAnnounce);
            }
        }
    }

    public KeyguardMessageArea(Context context) {
        super(context, null);
        this.mNextMessageColorState = ColorStateList.valueOf(-1);
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onFinishedGoingToSleep(int i) {
                KeyguardMessageArea.this.setSelected(false);
            }

            public void onStartedWakingUp() {
                KeyguardMessageArea.this.setSelected(true);
            }

            public void onKeyguardBouncerChanged(boolean z) {
                KeyguardMessageArea.this.mBouncerVisible = z;
                KeyguardMessageArea.this.update();
            }

            public void onKeyguardVisibilityChanged(boolean z) {
                if (!z) {
                    KeyguardMessageArea.this.setSelected(false);
                }
            }
        };
        throw new IllegalStateException("This constructor should never be invoked");
    }

    public KeyguardMessageArea(Context context, AttributeSet attributeSet, ConfigurationController configurationController) {
        this(context, attributeSet, KeyguardUpdateMonitor.getInstance(context), configurationController);
    }

    public KeyguardMessageArea(Context context, AttributeSet attributeSet, KeyguardUpdateMonitor keyguardUpdateMonitor, ConfigurationController configurationController) {
        super(context, attributeSet, keyguardUpdateMonitor, configurationController);
        this.mNextMessageColorState = ColorStateList.valueOf(-1);
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onFinishedGoingToSleep(int i) {
                KeyguardMessageArea.this.setSelected(false);
            }

            public void onStartedWakingUp() {
                KeyguardMessageArea.this.setSelected(true);
            }

            public void onKeyguardBouncerChanged(boolean z) {
                KeyguardMessageArea.this.mBouncerVisible = z;
                KeyguardMessageArea.this.update();
            }

            public void onKeyguardVisibilityChanged(boolean z) {
                if (!z) {
                    KeyguardMessageArea.this.setSelected(false);
                }
            }
        };
        setLayerType(2, null);
        keyguardUpdateMonitor.registerCallback(this.mInfoCallback);
        this.mHandler = new Handler(Looper.myLooper());
        this.mConfigurationController = configurationController;
        onThemeChanged();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mConfigurationController.addCallback(this);
        onThemeChanged();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mConfigurationController.removeCallback(this);
    }

    public void setNextMessageColor(ColorStateList colorStateList) {
        this.mNextMessageColorState = colorStateList;
    }

    public void onThemeChanged() {
        TypedArray obtainStyledAttributes = this.mContext.obtainStyledAttributes(new int[]{R$attr.wallpaperTextColor});
        ColorStateList valueOf = ColorStateList.valueOf(obtainStyledAttributes.getColor(0, -65536));
        obtainStyledAttributes.recycle();
        this.mDefaultColorState = valueOf;
        update();
    }

    public void onDensityOrFontScaleChanged() {
        TypedArray obtainStyledAttributes = this.mContext.obtainStyledAttributes(R$style.OpKeyguard_TextView, new int[]{16842901});
        setTextSize(0, (float) obtainStyledAttributes.getDimensionPixelSize(0, 0));
        obtainStyledAttributes.recycle();
    }

    public void setMessage(CharSequence charSequence) {
        if (!TextUtils.isEmpty(charSequence)) {
            securityMessageChanged(charSequence);
        } else {
            clearMessage();
        }
    }

    public void setMessage(int i) {
        setMessage(i != 0 ? getContext().getResources().getText(i) : null);
    }

    public static KeyguardMessageArea findSecurityMessageDisplay(View view) {
        KeyguardMessageArea keyguardMessageArea = (KeyguardMessageArea) view.findViewById(R$id.keyguard_message_area);
        if (keyguardMessageArea == null) {
            keyguardMessageArea = (KeyguardMessageArea) view.getRootView().findViewById(R$id.keyguard_message_area);
        }
        if (keyguardMessageArea != null) {
            return keyguardMessageArea;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Can't find keyguard_message_area in ");
        sb.append(view.getClass());
        throw new RuntimeException(sb.toString());
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        setSelected(KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive());
    }

    private void securityMessageChanged(CharSequence charSequence) {
        this.mMessage = charSequence;
        update();
        this.mHandler.removeCallbacksAndMessages(ANNOUNCE_TOKEN);
        this.mHandler.postAtTime(new AnnounceRunnable(this, getText()), ANNOUNCE_TOKEN, SystemClock.uptimeMillis() + 250);
    }

    private void clearMessage() {
        this.mMessage = null;
        this.mMessageType = 0;
        update();
    }

    /* access modifiers changed from: private */
    public void update() {
        CharSequence charSequence = this.mMessage;
        setTextWithAnim(charSequence, this.mBouncerVisible);
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("update, status:");
            sb.append(charSequence);
            sb.append(", bouncer:");
            sb.append(this.mBouncerVisible);
            Log.i("KeyguardMessageArea", sb.toString());
        }
        if (!(this.mMessageType != 1 || charSequence == null || charSequence.length() == 0)) {
            if (!" ".equals(charSequence.toString())) {
                this.mMessageType = 0;
                animateErrorText(this);
            }
        }
        ColorStateList colorStateList = this.mDefaultColorState;
        if (OpUtils.isCustomFingerprint()) {
            colorStateList = Utils.getColorAttr(this.mContext, R$attr.wallpaperTextColor);
        } else if (this.mNextMessageColorState.getDefaultColor() != -1) {
            colorStateList = this.mNextMessageColorState;
            this.mNextMessageColorState = ColorStateList.valueOf(-1);
        }
        setTextColor(colorStateList);
    }
}

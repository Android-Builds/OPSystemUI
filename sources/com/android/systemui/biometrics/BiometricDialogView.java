package com.android.systemui.biometrics;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.IFingerprintService;
import android.hardware.fingerprint.IFingerprintService.Stub;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.Interpolators;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.R$style;
import com.android.systemui.util.leak.RotationUtils;
import com.oneplus.util.OpUtils;
import com.oneplus.util.ThemeColorUtils;

public abstract class BiometricDialogView extends LinearLayout {
    /* access modifiers changed from: private */
    public boolean mAnimatingAway;
    /* access modifiers changed from: private */
    public final float mAnimationTranslationOffset;
    private String mAuthenticatePkg;
    protected final ImageView mBiometricIcon;
    protected Bundle mBundle;
    protected final DialogViewCallback mCallback;
    protected final TextView mDescriptionText;
    private final DevicePolicyManager mDevicePolicyManager;
    protected final LinearLayout mDialog;
    private final float mDialogWidth;
    private final int mErrorColor;
    protected TextView mErrorText;
    protected Handler mHandler;
    protected final ViewGroup mLayout;
    /* access modifiers changed from: private */
    public final Interpolator mLinearOutSlowIn;
    protected Button mNegativeButton;
    private boolean mPause;
    protected Button mPositiveButton;
    protected boolean mRequireConfirmation;
    private Bundle mRestoredState;
    private final Runnable mShowAnimationRunnable;
    private boolean mSkipIntro;
    private int mState;
    protected final TextView mSubtitleText;
    protected int mTextColor;
    protected final TextView mTitleText;
    protected final Button mTryAgainButton;
    private int mUserId;
    private final UserManager mUserManager;
    private boolean mWasForceRemoved;
    /* access modifiers changed from: private */
    public final WindowManager mWindowManager;
    private final IBinder mWindowToken;

    /* access modifiers changed from: protected */
    public abstract int getAuthenticatedAccessibilityResourceId();

    /* access modifiers changed from: protected */
    public abstract int getDelayAfterAuthenticatedDurationMs();

    /* access modifiers changed from: protected */
    public abstract int getHintStringResourceId();

    /* access modifiers changed from: protected */
    public abstract int getIconDescriptionResourceId();

    /* access modifiers changed from: protected */
    public abstract void handleResetMessage();

    /* access modifiers changed from: protected */
    public DialogViewCallback needWrap(DialogViewCallback dialogViewCallback) {
        return dialogViewCallback;
    }

    /* access modifiers changed from: protected */
    public void onBiometricPromptReady() {
    }

    /* access modifiers changed from: protected */
    public abstract boolean shouldGrayAreaDismissDialog();

    public void showTryAgainButton(boolean z) {
    }

    /* access modifiers changed from: protected */
    public abstract void updateIcon(int i, int i2);

    public BiometricDialogView(Context context, DialogViewCallback dialogViewCallback, boolean z) {
        int i;
        if (ThemeColorUtils.getCurrentTheme() == 1) {
            i = R$style.Oneplus_Theme_BiometricDialog_Dark;
        } else {
            i = R$style.Oneplus_Theme_BiometricDialog_Light;
        }
        super(context, null, i);
        this.mWindowToken = new Binder();
        this.mState = 0;
        this.mShowAnimationRunnable = new Runnable() {
            public void run() {
                BiometricDialogView.this.mLayout.animate().alpha(1.0f).setDuration(250).setInterpolator(BiometricDialogView.this.mLinearOutSlowIn).withLayer().start();
                BiometricDialogView.this.mDialog.animate().translationY(0.0f).setDuration(250).setInterpolator(BiometricDialogView.this.mLinearOutSlowIn).withLayer().withEndAction(new Runnable() {
                    public final void run() {
                        C07981.this.lambda$run$0$BiometricDialogView$1();
                    }
                }).start();
            }

            public /* synthetic */ void lambda$run$0$BiometricDialogView$1() {
                BiometricDialogView.this.onDialogAnimatedIn();
            }
        };
        this.mHandler = new Handler() {
            public void handleMessage(Message message) {
                if (message.what != 1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unhandled message: ");
                    sb.append(message.what);
                    Log.e("BiometricDialogView", sb.toString());
                    return;
                }
                BiometricDialogView.this.handleResetMessage();
            }
        };
        this.mCallback = needWrap(dialogViewCallback);
        this.mLinearOutSlowIn = Interpolators.LINEAR_OUT_SLOW_IN;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService(WindowManager.class);
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class);
        this.mAnimationTranslationOffset = getResources().getDimension(R$dimen.biometric_dialog_animation_translation_offset);
        this.mErrorColor = getResources().getColor(R$color.biometric_dialog_error);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
        DisplayMetrics displayMetrics2 = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getRealMetrics(displayMetrics2);
        this.mDialogWidth = (float) Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        this.mLayout = (ViewGroup) LayoutInflater.from(getContext()).inflate(R$layout.op_biometric_dialog, this, false);
        addView(this.mLayout);
        this.mLayout.setOnKeyListener(new OnKeyListener() {
            boolean downPressed = false;

            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (i != 4) {
                    return false;
                }
                if (keyEvent.getAction() == 0 && !this.downPressed) {
                    this.downPressed = true;
                } else if (keyEvent.getAction() == 0) {
                    this.downPressed = false;
                } else if (keyEvent.getAction() == 1 && this.downPressed) {
                    this.downPressed = false;
                    BiometricDialogView.this.mCallback.onUserCanceled();
                }
                return true;
            }
        });
        View findViewById = this.mLayout.findViewById(R$id.space);
        View findViewById2 = this.mLayout.findViewById(R$id.left_space);
        View findViewById3 = this.mLayout.findViewById(R$id.right_space);
        this.mDialog = (LinearLayout) this.mLayout.findViewById(R$id.dialog);
        this.mTitleText = (TextView) this.mLayout.findViewById(R$id.title);
        this.mSubtitleText = (TextView) this.mLayout.findViewById(R$id.subtitle);
        this.mDescriptionText = (TextView) this.mLayout.findViewById(R$id.description);
        this.mBiometricIcon = (ImageView) this.mLayout.findViewById(R$id.biometric_icon);
        shouldAdjustForOpUIDesign(this.mLayout, displayMetrics, displayMetrics2, z);
        this.mTryAgainButton = (Button) this.mLayout.findViewById(R$id.button_try_again);
        int color = ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT);
        this.mPositiveButton.setTextColor(color);
        this.mNegativeButton.setTextColor(color);
        this.mTryAgainButton.setTextColor(color);
        this.mBiometricIcon.setContentDescription(getResources().getString(getIconDescriptionResourceId()));
        setDismissesDialog(findViewById);
        setDismissesDialog(findViewById2);
        setDismissesDialog(findViewById3);
        this.mNegativeButton.setOnClickListener(new OnClickListener() {
            public final void onClick(View view) {
                BiometricDialogView.this.lambda$new$0$BiometricDialogView(view);
            }
        });
        this.mPositiveButton.setOnClickListener(new OnClickListener() {
            public final void onClick(View view) {
                BiometricDialogView.this.lambda$new$2$BiometricDialogView(view);
            }
        });
        this.mTryAgainButton.setOnClickListener(new OnClickListener() {
            public final void onClick(View view) {
                BiometricDialogView.this.lambda$new$3$BiometricDialogView(view);
            }
        });
        this.mLayout.setFocusableInTouchMode(true);
        this.mLayout.requestFocus();
    }

    public /* synthetic */ void lambda$new$0$BiometricDialogView(View view) {
        int i = this.mState;
        if (i == 3 || i == 4) {
            this.mCallback.onUserCanceled();
        } else {
            this.mCallback.onNegativePressed();
        }
    }

    public /* synthetic */ void lambda$new$2$BiometricDialogView(View view) {
        updateState(4);
        this.mHandler.postDelayed(new Runnable() {
            public final void run() {
                BiometricDialogView.this.lambda$new$1$BiometricDialogView();
            }
        }, (long) getDelayAfterAuthenticatedDurationMs());
    }

    public /* synthetic */ void lambda$new$1$BiometricDialogView() {
        this.mCallback.onPositivePressed();
    }

    public /* synthetic */ void lambda$new$3$BiometricDialogView(View view) {
        handleResetMessage();
        updateState(1);
        showTryAgainButton(false);
        this.mCallback.onTryAgainPressed();
    }

    public void onSaveState(Bundle bundle) {
        bundle.putInt("key_try_again_visibility", this.mTryAgainButton.getVisibility());
        bundle.putInt("key_confirm_visibility", this.mPositiveButton.getVisibility());
        bundle.putInt("key_state", this.mState);
        bundle.putInt("key_error_text_visibility", this.mErrorText.getVisibility());
        bundle.putCharSequence("key_error_text_string", this.mErrorText.getText());
        bundle.putBoolean("key_error_text_is_temporary", this.mHandler.hasMessages(1));
        bundle.putInt("key_error_text_color", this.mErrorText.getCurrentTextColor());
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ImageView imageView = (ImageView) this.mLayout.findViewById(R$id.background);
        if (this.mUserManager.isManagedProfile(this.mUserId)) {
            Drawable drawable = getResources().getDrawable(R$drawable.work_challenge_background, this.mContext.getTheme());
            drawable.setColorFilter(this.mDevicePolicyManager.getOrganizationColorForUser(this.mUserId), Mode.DARKEN);
            imageView.setImageDrawable(drawable);
        } else {
            imageView.setImageDrawable(null);
            imageView.setBackgroundColor(R$color.biometric_dialog_dim_color);
        }
        this.mNegativeButton.setVisibility(0);
        if (RotationUtils.getRotation(this.mContext) != 0) {
            this.mDialog.getLayoutParams().width = (int) this.mDialogWidth;
        }
        if (this.mRestoredState == null) {
            updateState(1);
            this.mErrorText.setText(getHintStringResourceId());
            this.mErrorText.setContentDescription(this.mContext.getString(getHintStringResourceId()));
            this.mErrorText.setVisibility(0);
        } else {
            updateState(this.mState);
        }
        this.mAuthenticatePkg = this.mBundle.getString("key_fingerprint_package_name");
        CharSequence charSequence = this.mBundle.getCharSequence("title");
        this.mTitleText.setVisibility(0);
        this.mTitleText.setText(charSequence);
        CharSequence charSequence2 = this.mBundle.getCharSequence("subtitle");
        if (TextUtils.isEmpty(charSequence2)) {
            this.mSubtitleText.setVisibility(8);
        } else {
            this.mSubtitleText.setVisibility(0);
            this.mSubtitleText.setText(charSequence2);
        }
        CharSequence charSequence3 = this.mBundle.getCharSequence("description");
        if (TextUtils.isEmpty(charSequence3)) {
            this.mDescriptionText.setVisibility(8);
        } else {
            this.mDescriptionText.setVisibility(0);
            this.mDescriptionText.setText(charSequence3);
        }
        this.mNegativeButton.setText(this.mBundle.getCharSequence("negative_text"));
        if (requiresConfirmation() && this.mRestoredState == null) {
            this.mPositiveButton.setVisibility(0);
            this.mPositiveButton.setEnabled(false);
        }
        if (this.mWasForceRemoved || this.mSkipIntro) {
            this.mLayout.animate().cancel();
            this.mDialog.animate().cancel();
            this.mDialog.setAlpha(1.0f);
            this.mDialog.setTranslationY(0.0f);
            this.mLayout.setAlpha(1.0f);
            onBiometricPromptReady();
        } else {
            this.mDialog.setTranslationY(this.mAnimationTranslationOffset);
            this.mLayout.setAlpha(0.0f);
            postOnAnimation(this.mShowAnimationRunnable);
        }
        this.mWasForceRemoved = false;
        this.mSkipIntro = false;
    }

    private void setDismissesDialog(View view) {
        view.setClickable(true);
        view.setOnClickListener(new OnClickListener() {
            public final void onClick(View view) {
                BiometricDialogView.this.lambda$setDismissesDialog$4$BiometricDialogView(view);
            }
        });
    }

    public /* synthetic */ void lambda$setDismissesDialog$4$BiometricDialogView(View view) {
        if (this.mState != 4 && shouldGrayAreaDismissDialog()) {
            this.mCallback.onUserCanceled();
        }
    }

    public void startDismiss() {
        this.mAnimatingAway = true;
        final C08014 r0 = new Runnable() {
            public void run() {
                BiometricDialogView.this.mWindowManager.removeView(BiometricDialogView.this);
                BiometricDialogView.this.mAnimatingAway = false;
                BiometricDialogView.this.handleResetMessage();
                BiometricDialogView.this.showTryAgainButton(false);
                BiometricDialogView.this.updateState(0);
            }
        };
        postOnAnimation(new Runnable() {
            public void run() {
                BiometricDialogView.this.mLayout.animate().alpha(0.0f).setDuration(350).setInterpolator(BiometricDialogView.this.mLinearOutSlowIn).withLayer().start();
                BiometricDialogView.this.mDialog.animate().translationY(BiometricDialogView.this.mAnimationTranslationOffset).setDuration(350).setInterpolator(BiometricDialogView.this.mLinearOutSlowIn).withLayer().withEndAction(r0).start();
            }
        });
    }

    public void forceRemove() {
        this.mLayout.animate().cancel();
        this.mDialog.animate().cancel();
        this.mWindowManager.removeView(this);
        this.mAnimatingAway = false;
        this.mWasForceRemoved = true;
    }

    public void setSkipIntro(boolean z) {
        this.mSkipIntro = z;
    }

    public void setBundle(Bundle bundle) {
        this.mBundle = bundle;
    }

    public void setRequireConfirmation(boolean z) {
        this.mRequireConfirmation = z;
    }

    public boolean requiresConfirmation() {
        return this.mRequireConfirmation;
    }

    public void setUserId(int i) {
        this.mUserId = i;
    }

    /* access modifiers changed from: protected */
    public void showTemporaryMessage(String str) {
        this.mHandler.removeMessages(1);
        this.mErrorText.setText(str);
        this.mErrorText.setTextColor(this.mErrorColor);
        this.mErrorText.setContentDescription(str);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(1), 2000);
    }

    public void onHelpReceived(String str) {
        updateState(2);
        showTemporaryMessage(str);
    }

    public void onAuthenticationFailed(String str) {
        updateState(2);
        showTemporaryMessage(str);
    }

    public void onErrorReceived(String str) {
        updateState(2);
        showTemporaryMessage(str);
        showTryAgainButton(false);
        this.mCallback.onErrorShown();
    }

    public void updateState(int i) {
        if (i == 3) {
            this.mHandler.removeMessages(1);
            this.mErrorText.setVisibility(4);
            this.mPositiveButton.setVisibility(0);
            this.mPositiveButton.setEnabled(true);
        } else if (i == 4) {
            this.mPositiveButton.setVisibility(8);
            this.mNegativeButton.setVisibility(8);
            this.mErrorText.setVisibility(4);
        }
        if (i == 3 || i == 4) {
            this.mNegativeButton.setText(R$string.cancel);
        }
        updateIcon(this.mState, i);
        this.mState = i;
    }

    public void onDialogAnimatedIn() {
        onBiometricPromptReady();
    }

    public void restoreState(Bundle bundle) {
        this.mRestoredState = bundle;
        this.mTryAgainButton.setVisibility(bundle.getInt("key_try_again_visibility"));
        this.mPositiveButton.setVisibility(bundle.getInt("key_confirm_visibility"));
        this.mState = bundle.getInt("key_state");
        String str = "key_error_text_string";
        this.mErrorText.setText(bundle.getCharSequence(str));
        this.mErrorText.setContentDescription(bundle.getCharSequence(str));
        this.mErrorText.setVisibility(bundle.getInt("key_error_text_visibility"));
        this.mErrorText.setTextColor(bundle.getInt("key_error_text_color"));
        if (bundle.getBoolean("key_error_text_is_temporary")) {
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(1), 2000);
        }
    }

    /* access modifiers changed from: protected */
    public int getState() {
        return this.mState;
    }

    public LayoutParams getLayoutParams() {
        LayoutParams layoutParams = new LayoutParams(-1, -1, 2014, 16777472, -3);
        layoutParams.privateFlags |= 16;
        layoutParams.layoutInDisplayCutoutMode = 1;
        layoutParams.setTitle("BiometricDialogView");
        layoutParams.token = this.mWindowToken;
        return layoutParams;
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0070  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x008b  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x00b6  */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x001f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void shouldAdjustForOpUIDesign(android.view.ViewGroup r7, android.util.DisplayMetrics r8, android.util.DisplayMetrics r9, boolean r10) {
        /*
            r6 = this;
            boolean r0 = com.oneplus.util.OpUtils.isCustomFingerprint()
            r1 = 0
            if (r0 == 0) goto L_0x0018
            int r0 = com.android.systemui.R$id.error1
            boolean r2 = r6 instanceof com.android.systemui.biometrics.FingerprintDialogView
            if (r2 == 0) goto L_0x001a
            float r2 = r6.getFodIconSize()
            int r2 = (int) r2
            int r3 = r6.getFodIconBottomSpace(r8)
            r4 = 1
            goto L_0x001d
        L_0x0018:
            int r0 = com.android.systemui.R$id.error2
        L_0x001a:
            r2 = r1
            r3 = r2
            r4 = r3
        L_0x001d:
            if (r4 != 0) goto L_0x0035
            android.content.res.Resources r2 = r6.getResources()
            int r3 = com.android.systemui.R$dimen.biometric_dialog_biometric_icon_size
            float r2 = r2.getDimension(r3)
            int r2 = (int) r2
            android.content.res.Resources r3 = r6.getResources()
            int r5 = com.android.systemui.R$dimen.op_biometric_dialog_bottom_height_no_fod
            float r3 = r3.getDimension(r5)
            int r3 = (int) r3
        L_0x0035:
            android.view.View r0 = r7.findViewById(r0)
            android.widget.TextView r0 = (android.widget.TextView) r0
            r6.mErrorText = r0
            android.widget.TextView r0 = r6.mErrorText
            r0.setVisibility(r1)
            android.widget.TextView r0 = r6.mErrorText
            android.content.res.ColorStateList r0 = r0.getTextColors()
            int r0 = r0.getDefaultColor()
            r6.mTextColor = r0
            android.widget.ImageView r0 = r6.mBiometricIcon
            android.view.ViewGroup$LayoutParams r0 = r0.getLayoutParams()
            r0.width = r2
            android.widget.ImageView r0 = r6.mBiometricIcon
            android.view.ViewGroup$LayoutParams r0 = r0.getLayoutParams()
            r0.height = r2
            int r0 = com.android.systemui.R$id.bottom_space
            android.view.View r7 = r7.findViewById(r0)
            int r9 = r9.heightPixels
            int r8 = r8.heightPixels
            int r9 = r9 - r8
            int r3 = r3 + r9
            int r8 = android.os.Build.VERSION.SDK_INT
            r9 = 29
            if (r8 < r9) goto L_0x0083
            android.view.WindowManager r8 = r6.mWindowManager
            android.view.Display r8 = r8.getDefaultDisplay()
            android.view.DisplayCutout r8 = r8.getCutout()
            if (r8 == 0) goto L_0x0083
            if (r4 != 0) goto L_0x0083
            int r8 = r8.getSafeInsetTop()
            int r3 = r3 - r8
        L_0x0083:
            android.view.ViewGroup$LayoutParams r7 = r7.getLayoutParams()
            r7.height = r3
            if (r10 == 0) goto L_0x00b6
            android.view.ViewGroup r7 = r6.mLayout
            int r8 = com.android.systemui.R$id.button1
            android.view.View r7 = r7.findViewById(r8)
            android.widget.Button r7 = (android.widget.Button) r7
            r6.mNegativeButton = r7
            android.view.ViewGroup r7 = r6.mLayout
            int r8 = com.android.systemui.R$id.button2
            android.view.View r7 = r7.findViewById(r8)
            android.widget.Button r7 = (android.widget.Button) r7
            r6.mPositiveButton = r7
            android.widget.Button r7 = r6.mPositiveButton
            r8 = 8
            r7.setVisibility(r8)
            android.widget.Button r7 = r6.mNegativeButton
            float r6 = r6.mDialogWidth
            r8 = 1073741824(0x40000000, float:2.0)
            float r6 = r6 / r8
            int r6 = (int) r6
            r7.setMaxWidth(r6)
            goto L_0x00ce
        L_0x00b6:
            android.view.ViewGroup r7 = r6.mLayout
            int r8 = com.android.systemui.R$id.button2
            android.view.View r7 = r7.findViewById(r8)
            android.widget.Button r7 = (android.widget.Button) r7
            r6.mNegativeButton = r7
            android.view.ViewGroup r7 = r6.mLayout
            int r8 = com.android.systemui.R$id.button1
            android.view.View r7 = r7.findViewById(r8)
            android.widget.Button r7 = (android.widget.Button) r7
            r6.mPositiveButton = r7
        L_0x00ce:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.biometrics.BiometricDialogView.shouldAdjustForOpUIDesign(android.view.ViewGroup, android.util.DisplayMetrics, android.util.DisplayMetrics, boolean):void");
    }

    private float getFodIconSize() {
        int i;
        if (OpUtils.is2KResolution()) {
            i = R$dimen.op_biometric_icon_normal_width_2k;
        } else {
            i = R$dimen.op_biometric_icon_normal_width_1080p;
        }
        return (float) getResources().getDimensionPixelSize(i);
    }

    private float getFodAnimationSize() {
        int i;
        if (OpUtils.is2KResolution()) {
            i = R$dimen.fp_animation_height_2k;
        } else {
            i = R$dimen.fp_animation_height_1080p;
        }
        return (float) getResources().getDimensionPixelSize(i);
    }

    private int getFodIconBottomSpace(DisplayMetrics displayMetrics) {
        int i;
        int i2 = displayMetrics.heightPixels;
        float fodAnimationSize = getFodAnimationSize();
        float fodIconSize = getFodIconSize();
        boolean isSupportCustomFingerprintType2 = OpUtils.isSupportCustomFingerprintType2();
        boolean is2KResolution = OpUtils.is2KResolution();
        boolean isSupportCutout = OpUtils.isSupportCutout();
        boolean isCutoutHide = OpUtils.isCutoutHide(getContext());
        boolean isSupportResolutionSwitch = OpUtils.isSupportResolutionSwitch(getContext());
        StringBuilder sb = new StringBuilder();
        sb.append("adjusting bottom space. isFpType2= ");
        sb.append(isSupportCustomFingerprintType2);
        sb.append(", is2kDisplay= ");
        sb.append(is2KResolution);
        sb.append(", isSupportCutout= ");
        sb.append(isSupportCutout);
        sb.append(", isCutoutHide= ");
        sb.append(isCutoutHide);
        sb.append(", isSupportResolutionSwitch= ");
        sb.append(isSupportResolutionSwitch);
        Log.d("BiometricDialogView", sb.toString());
        if (isSupportResolutionSwitch) {
            if (isSupportCustomFingerprintType2) {
                i = R$dimen.op_biometric_animation_view_ss_y;
            } else if (is2KResolution) {
                i = R$dimen.op_biometric_animation_view_y_2k;
            } else {
                i = R$dimen.op_biometric_animation_view_y_1080p;
            }
        } else if (isSupportCustomFingerprintType2) {
            i = R$dimen.op_biometric_animation_view_ss_y;
        } else {
            i = R$dimen.op_biometric_animation_view_y;
        }
        float dimensionPixelSize = (float) getResources().getDimensionPixelSize(i);
        if (isSupportCutout && isCutoutHide) {
            i2 += OpUtils.getCutoutPathdataHeight(getContext());
        }
        return (int) (((((float) i2) - dimensionPixelSize) - fodIconSize) - ((fodAnimationSize - fodIconSize) / 2.0f));
    }

    private void updateFingerprintStatus() {
        IFingerprintService asInterface = Stub.asInterface(ServiceManager.getService("fingerprint"));
        String str = "BiometricDialogView";
        if (asInterface != null) {
            try {
                String authenticatedPackage = asInterface.getAuthenticatedPackage();
                if (TextUtils.isEmpty(this.mAuthenticatePkg) || !TextUtils.equals(this.mAuthenticatePkg, authenticatedPackage)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("updateFingerprintStatus: current authentication pkg is not ");
                    sb.append(this.mAuthenticatePkg);
                    Log.d(str, sb.toString());
                    return;
                }
                asInterface.updateStatus(this.mPause ? 12 : 11);
            } catch (RemoteException e) {
                Log.d(str, "updateStatus occur remote exception", e);
            }
        } else {
            Log.e(str, "updateFingerprintStatus null pointer");
        }
    }

    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (OpUtils.isCustomFingerprint()) {
            StringBuilder sb = new StringBuilder();
            sb.append("onWindowFocusChanged: hasFocus= ");
            sb.append(z);
            sb.append(", mAnimatingAway= ");
            sb.append(this.mAnimatingAway);
            String str = "BiometricDialogView";
            Log.d(str, sb.toString());
            if (this.mAnimatingAway) {
                return;
            }
            if (z && this.mPause) {
                Log.d(str, "onWindowFocusChanged: need to resume");
                this.mPause = false;
                updateFingerprintStatus();
            } else if (!z && !this.mPause) {
                Log.d(str, "onWindowFocusChanged: need to suspend");
                this.mPause = true;
                updateFingerprintStatus();
            }
        }
    }
}

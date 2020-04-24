package com.oneplus.systemui.biometrics;

import android.content.Context;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import com.android.systemui.R$dimen;
import com.oneplus.util.OpUtils;

public class OpFodWindowManager {
    private static final boolean DEBUG_ONEPLUS = Build.DEBUG_ONEPLUS;
    private Context mContext;
    private boolean mCustHideCutout;
    private OpBiometricDialogImpl mDialogImpl;
    private OpFingerprintDialogView mDialogView;
    private LayoutParams mFpLayoutParams;
    private ViewGroup mHighlightView;
    private LayoutParams mHighlightViewParams;
    private boolean mIs2KDisplay;
    private boolean mTransparentIconExpand;
    private LayoutParams mTransparentIconParams;
    private int mTransparentIconSize;
    private View mTransparentIconView;
    private WindowManager mWindowManager;
    private final IBinder mWindowToken = new Binder();

    public OpFodWindowManager(Context context, OpBiometricDialogImpl opBiometricDialogImpl, OpFingerprintDialogView opFingerprintDialogView) {
        boolean z = false;
        this.mIs2KDisplay = false;
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mDialogImpl = opBiometricDialogImpl;
        this.mDialogView = opFingerprintDialogView;
        this.mFpLayoutParams = getFpLayoutParams();
        this.mHighlightViewParams = getHighlightViewLayoutParams();
        this.mTransparentIconParams = getIconLayoutParams();
        this.mDialogView.setFodWindowManager(this);
        this.mCustHideCutout = OpUtils.isCutoutHide(this.mContext);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
        StringBuilder sb = new StringBuilder();
        sb.append("metrics.width = ");
        sb.append(displayMetrics.widthPixels);
        Log.d("OpFodWindowManager", sb.toString());
        if (displayMetrics.widthPixels == 1440) {
            z = true;
        }
        this.mIs2KDisplay = z;
    }

    public void addFpViewToWindow() {
        OpFingerprintDialogView opFingerprintDialogView = this.mDialogView;
        if (opFingerprintDialogView == null || opFingerprintDialogView.isAttachedToWindow()) {
            Log.d("OpFodWindowManager", "can't add fp view window.");
        } else {
            this.mWindowManager.addView(this.mDialogView, this.mFpLayoutParams);
        }
    }

    public void addHighlightViewToWindow(ViewGroup viewGroup) {
        if (viewGroup != null) {
            this.mHighlightView = viewGroup;
            this.mWindowManager.addView(viewGroup, this.mHighlightViewParams);
        }
    }

    public void addTransparentIconViewToWindow(View view) {
        if (view != null) {
            this.mTransparentIconView = view;
            this.mWindowManager.addView(view, this.mTransparentIconParams);
        }
    }

    public void removeTransparentIconView() {
        if (this.mTransparentIconView.isAttachedToWindow()) {
            Log.d("OpFodWindowManager", "removeTransparentIconView");
            this.mWindowManager.removeViewImmediate(this.mTransparentIconView);
        }
    }

    public LayoutParams getFpLayoutParams() {
        return getCustomLayoutParams("OPFingerprintView");
    }

    private LayoutParams getHighlightViewLayoutParams() {
        return getCustomLayoutParams("OPFingerprintVDpressed");
    }

    private LayoutParams getCustomLayoutParams(String str) {
        boolean isKeyguardAuthenticating = OpFodHelper.getInstance().isKeyguardAuthenticating();
        String currentOwner = OpFodHelper.getInstance().getCurrentOwner();
        String str2 = "OPFingerprintView";
        String str3 = "com.oneplus.applocker";
        int i = 1;
        if (str.equals(str2)) {
            LayoutParams layoutParams = this.mFpLayoutParams;
            if (layoutParams != null) {
                if (isKeyguardAuthenticating || str3.equals(currentOwner)) {
                    i = -1;
                }
                layoutParams.screenOrientation = i;
                return this.mFpLayoutParams;
            }
        }
        String str4 = "OPFingerprintVDpressed";
        if (str.equals(str4)) {
            LayoutParams layoutParams2 = this.mHighlightViewParams;
            if (layoutParams2 != null) {
                if (isKeyguardAuthenticating || str3.equals(currentOwner)) {
                    i = -1;
                }
                layoutParams2.screenOrientation = i;
                return this.mHighlightViewParams;
            }
        }
        LayoutParams layoutParams3 = new LayoutParams();
        if (str.equals(str2)) {
            layoutParams3.type = 2305;
        } else if (str.equals(str4)) {
            layoutParams3.type = 2306;
            layoutParams3.privateFlags |= 1048576;
        }
        layoutParams3.privateFlags |= 16;
        layoutParams3.layoutInDisplayCutoutMode = 1;
        boolean isSupportCustomFingerprintType2 = OpUtils.isSupportCustomFingerprintType2();
        layoutParams3.flags = 16778520;
        layoutParams3.format = -2;
        layoutParams3.width = -1;
        layoutParams3.height = -1;
        layoutParams3.gravity = 17;
        if (OpUtils.isSupportResolutionSwitch(this.mContext)) {
            boolean is2KResolution = OpUtils.is2KResolution();
            if (str.equals(str2)) {
                layoutParams3.width = -1;
                layoutParams3.height = this.mContext.getResources().getDimensionPixelSize(is2KResolution ? R$dimen.fp_animation_height_2k : R$dimen.fp_animation_height_1080p);
                Resources resources = this.mContext.getResources();
                int i2 = isSupportCustomFingerprintType2 ? R$dimen.op_biometric_animation_view_ss_y : is2KResolution ? R$dimen.op_biometric_animation_view_y_2k : R$dimen.op_biometric_animation_view_y_1080p;
                layoutParams3.y = resources.getDimensionPixelSize(i2);
                layoutParams3.gravity = 48;
            } else if (str.equals(str4)) {
                Resources resources2 = this.mContext.getResources();
                int i3 = isSupportCustomFingerprintType2 ? R$dimen.op_biometric_icon_flash_ss_width : is2KResolution ? R$dimen.op_biometric_icon_flash_width_2k : R$dimen.op_biometric_icon_flash_width_1080p;
                int dimensionPixelSize = resources2.getDimensionPixelSize(i3);
                layoutParams3.height = dimensionPixelSize;
                layoutParams3.width = dimensionPixelSize;
                Resources resources3 = this.mContext.getResources();
                int i4 = isSupportCustomFingerprintType2 ? R$dimen.op_biometric_icon_flash_ss_location_y : is2KResolution ? R$dimen.op_biometric_icon_flash_location_y_2k : R$dimen.op_biometric_icon_flash_location_y_1080p;
                layoutParams3.y = resources3.getDimensionPixelSize(i4);
                layoutParams3.gravity = 48;
            }
        } else if (str.equals(str2)) {
            layoutParams3.width = -1;
            layoutParams3.height = this.mContext.getResources().getDimensionPixelSize(R$dimen.fp_animation_height);
            layoutParams3.y = this.mContext.getResources().getDimensionPixelSize(isSupportCustomFingerprintType2 ? R$dimen.op_biometric_animation_view_ss_y : R$dimen.op_biometric_animation_view_y);
            layoutParams3.gravity = 48;
        } else if (str.equals(str4)) {
            int dimensionPixelSize2 = this.mContext.getResources().getDimensionPixelSize(isSupportCustomFingerprintType2 ? R$dimen.op_biometric_icon_flash_ss_width : R$dimen.op_biometric_icon_flash_width);
            layoutParams3.height = dimensionPixelSize2;
            layoutParams3.width = dimensionPixelSize2;
            layoutParams3.y = this.mContext.getResources().getDimensionPixelSize(isSupportCustomFingerprintType2 ? R$dimen.op_biometric_icon_flash_ss_location_y : R$dimen.op_biometric_icon_flash_location_y);
            layoutParams3.gravity = 48;
        }
        if (OpUtils.isCutoutHide(this.mContext)) {
            layoutParams3.y -= OpUtils.getCutoutPathdataHeight(this.mContext);
        }
        if (isKeyguardAuthenticating || str3.equals(currentOwner)) {
            i = -1;
        }
        layoutParams3.screenOrientation = i;
        layoutParams3.windowAnimations = 84934692;
        layoutParams3.setTitle(str);
        layoutParams3.token = this.mWindowToken;
        StringBuilder sb = new StringBuilder();
        sb.append("getCustomLayoutParams owner:");
        sb.append(currentOwner);
        sb.append(" title:");
        sb.append(str);
        Log.i("OpFodWindowManager", sb.toString());
        this.mDialogView.setSystemUiVisibility(this.mDialogView.getSystemUiVisibility() | 1026);
        return layoutParams3;
    }

    private LayoutParams getIconLayoutParams() {
        if (OpUtils.isSupportResolutionSwitch(this.mContext)) {
            this.mTransparentIconSize = this.mContext.getResources().getDimensionPixelSize(OpUtils.is2KResolution() ? R$dimen.op_biometric_transparent_icon_size_2k : R$dimen.op_biometric_transparent_icon_size_1080p);
        } else {
            this.mTransparentIconSize = this.mContext.getResources().getDimensionPixelSize(R$dimen.op_biometric_transparent_icon_size);
        }
        int i = this.mTransparentIconSize;
        LayoutParams layoutParams = new LayoutParams(i, i, 2305, 16777480, -3);
        layoutParams.privateFlags |= 16;
        layoutParams.setTitle("FingerprintTransparentIcon");
        layoutParams.gravity = 49;
        layoutParams.windowAnimations = 0;
        layoutParams.layoutInDisplayCutoutMode = 1;
        boolean isSupportCustomFingerprintType2 = OpUtils.isSupportCustomFingerprintType2();
        layoutParams.y = this.mContext.getResources().getDimensionPixelSize(isSupportCustomFingerprintType2 ? R$dimen.op_biometric_transparent_icon_ss_location_y : R$dimen.op_biometric_transparent_icon_location_y);
        if (OpUtils.isSupportResolutionSwitch(this.mContext)) {
            Resources resources = this.mContext.getResources();
            int i2 = isSupportCustomFingerprintType2 ? R$dimen.op_biometric_transparent_icon_ss_location_y : OpUtils.is2KResolution() ? R$dimen.op_biometric_transparent_icon_location_y_2k : R$dimen.op_biometric_transparent_icon_location_y_1080p;
            layoutParams.y = resources.getDimensionPixelSize(i2);
        } else {
            layoutParams.y = this.mContext.getResources().getDimensionPixelSize(isSupportCustomFingerprintType2 ? R$dimen.op_biometric_transparent_icon_ss_location_y : R$dimen.op_biometric_transparent_icon_location_y);
        }
        if (OpUtils.isCutoutHide(this.mContext)) {
            layoutParams.y -= OpUtils.getCutoutPathdataHeight(this.mContext);
        }
        layoutParams.screenOrientation = -1;
        return layoutParams;
    }

    public void handleConfigurationChange() {
        this.mFpLayoutParams = null;
        this.mFpLayoutParams = getCustomLayoutParams("OPFingerprintView");
        StringBuilder sb = new StringBuilder();
        sb.append("mViewLayoutParams height ");
        sb.append(this.mFpLayoutParams.height);
        Log.d("OpFodWindowManager", sb.toString());
        OpFingerprintDialogView opFingerprintDialogView = this.mDialogView;
        if (opFingerprintDialogView != null && opFingerprintDialogView.isAttachedToWindow()) {
            this.mWindowManager.updateViewLayout(this.mDialogView, this.mFpLayoutParams);
        }
        this.mHighlightViewParams = null;
        this.mHighlightViewParams = getCustomLayoutParams("OPFingerprintVDpressed");
        ViewGroup viewGroup = this.mHighlightView;
        if (viewGroup != null && viewGroup.isAttachedToWindow()) {
            this.mWindowManager.updateViewLayout(this.mHighlightView, this.mHighlightViewParams);
        }
        this.mTransparentIconParams = null;
        this.mTransparentIconParams = getIconLayoutParams();
        View view = this.mTransparentIconView;
        if (view != null && view.isAttachedToWindow() && !this.mTransparentIconExpand) {
            this.mWindowManager.updateViewLayout(this.mTransparentIconView, this.mTransparentIconParams);
        }
    }

    public void handleUpdateTransparentIconLayoutParams(boolean z) {
        View view = this.mTransparentIconView;
        if (view != null && view.isAttachedToWindow()) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateTransparentIconLayoutParams: ");
            sb.append(z);
            Log.d("OpFodWindowManager", sb.toString());
            LayoutParams iconLayoutParams = getIconLayoutParams();
            this.mTransparentIconExpand = z;
            if (z) {
                iconLayoutParams.width = -1;
                iconLayoutParams.height = -1;
                iconLayoutParams.y = 0;
                iconLayoutParams.screenOrientation = 1;
            }
            this.mWindowManager.updateViewLayout(this.mTransparentIconView, iconLayoutParams);
        }
    }

    public void onOverlayChanged() {
        StringBuilder sb = new StringBuilder();
        sb.append("onOverlayChanged be trigger in OpBiometricDialogImpl, mCustHideCutout:");
        sb.append(this.mCustHideCutout);
        sb.append(", OpUtils.isCutoutHide(mContext):");
        sb.append(OpUtils.isCutoutHide(this.mContext));
        String str = "OpFodWindowManager";
        Log.i(str, sb.toString());
        if (this.mCustHideCutout != OpUtils.isCutoutHide(this.mContext)) {
            this.mCustHideCutout = OpUtils.isCutoutHide(this.mContext);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            this.mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
            boolean z = true;
            boolean z2 = this.mContext.getResources().getConfiguration().orientation == 2;
            if (z2) {
                if (displayMetrics.heightPixels != 1440) {
                    z = false;
                }
                this.mIs2KDisplay = z;
            } else {
                if (displayMetrics.widthPixels != 1440) {
                    z = false;
                }
                this.mIs2KDisplay = z;
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("onOverlayChanged, metrics.width = ");
            sb2.append(displayMetrics.widthPixels);
            sb2.append(", metrics.height = ");
            sb2.append(displayMetrics.heightPixels);
            sb2.append(", isLandscape = ");
            sb2.append(z2);
            sb2.append(", is2KDisplay = ");
            sb2.append(this.mIs2KDisplay);
            Log.d(str, sb2.toString());
            OpFingerprintDialogView opFingerprintDialogView = this.mDialogView;
            if (opFingerprintDialogView != null) {
                opFingerprintDialogView.updateLayoutDimension(this.mIs2KDisplay, (float) displayMetrics.widthPixels);
            }
        }
    }

    public void onDensityOrFontScaleChanged() {
        String str = "OpFodWindowManager";
        Log.d(str, "onDensityOrFontScaleChanged");
        if (this.mDialogView == null) {
            Log.d(str, "onDensityOrFontScaleChanged mDialogView doesn't init yet");
            return;
        }
        if (OpUtils.isSupportResolutionSwitch(this.mContext) && this.mDialogView != null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            this.mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
            boolean z = true;
            boolean z2 = this.mContext.getResources().getConfiguration().orientation == 2;
            if (z2) {
                if (displayMetrics.heightPixels != 1440) {
                    z = false;
                }
                this.mIs2KDisplay = z;
            } else {
                if (displayMetrics.widthPixels != 1440) {
                    z = false;
                }
                this.mIs2KDisplay = z;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("metrics.width = ");
            sb.append(displayMetrics.widthPixels);
            sb.append(", metrics.height = ");
            sb.append(displayMetrics.heightPixels);
            sb.append(", isLandscape = ");
            sb.append(z2);
            sb.append(", is2KDisplay = ");
            sb.append(this.mIs2KDisplay);
            Log.d(str, sb.toString());
            this.mDialogView.updateLayoutDimension(this.mIs2KDisplay, (float) displayMetrics.widthPixels);
        }
    }

    public boolean isTransparentViewExpanded() {
        return this.mTransparentIconExpand;
    }
}

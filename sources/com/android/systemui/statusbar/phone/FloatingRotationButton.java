package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import com.android.settingslib.Utils;
import com.android.systemui.R$attr;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$layout;
import com.android.systemui.statusbar.policy.KeyButtonDrawable;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.oneplus.util.OpNavBarUtils;

public class FloatingRotationButton implements RotationButton {
    private boolean mCanShow = true;
    private final Context mContext;
    private final int mDiameter;
    private boolean mIsShowing;
    private KeyButtonDrawable mKeyButtonDrawable;
    private final KeyButtonView mKeyButtonView;
    private final int mMargin;
    private RotationButtonController mRotationButtonController;
    private final WindowManager mWindowManager;

    FloatingRotationButton(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mKeyButtonView = (KeyButtonView) LayoutInflater.from(this.mContext).inflate(R$layout.rotate_suggestion, null);
        this.mKeyButtonView.setVisibility(0);
        Resources resources = this.mContext.getResources();
        this.mDiameter = resources.getDimensionPixelSize(R$dimen.floating_rotation_button_diameter);
        this.mMargin = Math.max(resources.getDimensionPixelSize(R$dimen.floating_rotation_button_min_margin), resources.getDimensionPixelSize(R$dimen.rounded_corner_content_padding));
    }

    public void setRotationButtonController(RotationButtonController rotationButtonController) {
        this.mRotationButtonController = rotationButtonController;
    }

    public View getCurrentView() {
        return this.mKeyButtonView;
    }

    public boolean show() {
        if (!this.mCanShow || this.mIsShowing) {
            return false;
        }
        this.mIsShowing = true;
        int i = this.mDiameter;
        int i2 = this.mMargin;
        LayoutParams layoutParams = new LayoutParams(i, i, i2, i2, 2024, 8, -3);
        layoutParams.privateFlags |= 16;
        layoutParams.setTitle("FloatingRotationButton");
        int rotation = this.mWindowManager.getDefaultDisplay().getRotation();
        if (rotation == 0) {
            layoutParams.gravity = 83;
        } else if (rotation == 1) {
            layoutParams.gravity = 85;
        } else if (rotation == 2) {
            layoutParams.gravity = 53;
        } else if (rotation == 3) {
            layoutParams.gravity = 51;
        }
        updateIcon();
        this.mWindowManager.addView(this.mKeyButtonView, layoutParams);
        KeyButtonDrawable keyButtonDrawable = this.mKeyButtonDrawable;
        if (keyButtonDrawable != null && keyButtonDrawable.canAnimate()) {
            this.mKeyButtonDrawable.resetAnimation();
            this.mKeyButtonDrawable.startAnimation();
        }
        return true;
    }

    public boolean hide() {
        if (!this.mIsShowing) {
            return false;
        }
        this.mWindowManager.removeViewImmediate(this.mKeyButtonView);
        this.mIsShowing = false;
        return true;
    }

    public boolean isVisible() {
        return this.mIsShowing;
    }

    public void updateIcon() {
        if (this.mIsShowing) {
            this.mKeyButtonDrawable = getImageDrawable();
            this.mKeyButtonView.setImageDrawable(this.mKeyButtonDrawable);
            this.mKeyButtonDrawable.setCallback(this.mKeyButtonView);
            KeyButtonDrawable keyButtonDrawable = this.mKeyButtonDrawable;
            if (keyButtonDrawable != null && keyButtonDrawable.canAnimate()) {
                this.mKeyButtonDrawable.resetAnimation();
                this.mKeyButtonDrawable.startAnimation();
            }
        }
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.mKeyButtonView.setOnClickListener(onClickListener);
    }

    public void setOnHoverListener(OnHoverListener onHoverListener) {
        this.mKeyButtonView.setOnHoverListener(onHoverListener);
    }

    public KeyButtonDrawable getImageDrawable() {
        int i;
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(this.mContext.getApplicationContext(), this.mRotationButtonController.getStyleRes());
        int themeAttr = Utils.getThemeAttr(contextThemeWrapper, R$attr.darkIconTheme);
        ContextThemeWrapper contextThemeWrapper2 = new ContextThemeWrapper(contextThemeWrapper, Utils.getThemeAttr(contextThemeWrapper, R$attr.lightIconTheme));
        int colorAttrDefaultColor = Utils.getColorAttrDefaultColor(new ContextThemeWrapper(contextThemeWrapper, themeAttr), R$attr.singleToneColor);
        Color valueOf = Color.valueOf((float) Color.red(colorAttrDefaultColor), (float) Color.green(colorAttrDefaultColor), (float) Color.blue(colorAttrDefaultColor), 0.92f);
        int colorAttrDefaultColor2 = Utils.getColorAttrDefaultColor(contextThemeWrapper2, R$attr.singleToneColor);
        if (OpNavBarUtils.isSupportCustomNavBar()) {
            i = R$drawable.ic_sysbar_rotate_button2;
        } else {
            i = R$drawable.ic_sysbar_rotate_button;
        }
        return KeyButtonDrawable.create(contextThemeWrapper2, colorAttrDefaultColor2, colorAttrDefaultColor, i, false, valueOf);
    }

    public void setDarkIntensity(float f) {
        this.mKeyButtonView.setDarkIntensity(f);
    }

    public void setCanShowRotationButton(boolean z) {
        this.mCanShow = z;
        if (!this.mCanShow) {
            hide();
        }
    }
}

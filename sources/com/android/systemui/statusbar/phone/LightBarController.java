package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import com.android.internal.colorextraction.ColorExtractor.GradientColors;
import com.android.systemui.Dumpable;
import com.android.systemui.R$color;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryController.BatteryStateChangeCallback;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class LightBarController implements BatteryStateChangeCallback, Dumpable {
    private final BatteryController mBatteryController;
    private BiometricUnlockController mBiometricUnlockController;
    private final Color mDarkModeColor;
    private boolean mDirectReplying;
    private boolean mDockedLight;
    private int mDockedStackVisibility;
    private boolean mForceDarkForScrim;
    private boolean mFullscreenLight;
    private int mFullscreenStackVisibility;
    private boolean mHasLightNavigationBar;
    private final Rect mLastDockedBounds = new Rect();
    private final Rect mLastFullscreenBounds = new Rect();
    private int mLastNavigationBarMode;
    private int mLastStatusBarMode;
    private boolean mNavbarColorManagedByIme;
    private LightBarTransitionsController mNavigationBarController;
    private boolean mNavigationLight;
    private boolean mQsCustomizing;
    private final SysuiDarkIconDispatcher mStatusBarIconController;
    private int mSystemUiVisibility;

    private boolean isLight(int i, int i2, int i3) {
        return (i2 == 4 || i2 == 6) && ((i & i3) != 0);
    }

    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
    }

    public LightBarController(Context context, DarkIconDispatcher darkIconDispatcher, BatteryController batteryController) {
        this.mDarkModeColor = Color.valueOf(context.getColor(R$color.dark_mode_icon_color_single_tone));
        this.mStatusBarIconController = (SysuiDarkIconDispatcher) darkIconDispatcher;
        this.mBatteryController = batteryController;
        this.mBatteryController.addCallback(this);
    }

    public void setNavigationBar(LightBarTransitionsController lightBarTransitionsController) {
        this.mNavigationBarController = lightBarTransitionsController;
        updateNavigation();
    }

    public void setBiometricUnlockController(BiometricUnlockController biometricUnlockController) {
        this.mBiometricUnlockController = biometricUnlockController;
    }

    public void onSystemUiVisibilityChanged(int i, int i2, int i3, Rect rect, Rect rect2, boolean z, int i4, boolean z2) {
        int i5 = this.mFullscreenStackVisibility;
        int i6 = ~i3;
        int i7 = (i & i3) | (i5 & i6);
        int i8 = i5 ^ i7;
        int i9 = this.mDockedStackVisibility;
        int i10 = (i2 & i3) | (i6 & i9);
        int i11 = i10 ^ i9;
        if ((i8 & 8192) != 0 || (i11 & 8192) != 0 || z || !this.mLastFullscreenBounds.equals(rect) || !this.mLastDockedBounds.equals(rect2)) {
            this.mFullscreenLight = isLight(i7, i4, 8192);
            this.mDockedLight = isLight(i10, i4, 8192);
            updateStatus(rect, rect2);
        }
        this.mFullscreenStackVisibility = i7;
        this.mDockedStackVisibility = i10;
        this.mLastStatusBarMode = i4;
        this.mNavbarColorManagedByIme = z2;
        this.mLastFullscreenBounds.set(rect);
        this.mLastDockedBounds.set(rect2);
    }

    public void onNavigationVisibilityChanged(int i, int i2, boolean z, int i3, boolean z2) {
        int i4 = this.mSystemUiVisibility;
        int i5 = (i2 & i) | ((~i2) & i4);
        if (((i4 ^ i5) & 16) != 0 || z) {
            boolean z3 = this.mNavigationLight;
            this.mHasLightNavigationBar = isLight(i, i3, 16);
            this.mNavigationLight = this.mHasLightNavigationBar && ((this.mDirectReplying && this.mNavbarColorManagedByIme) || !this.mForceDarkForScrim) && !this.mQsCustomizing;
            StringBuilder sb = new StringBuilder();
            sb.append("onNavigationVisibilityChanged mNavigationLight = ");
            sb.append(this.mNavigationLight);
            Log.d("LightBarController", sb.toString());
            if (this.mNavigationLight != z3) {
                updateNavigation();
            }
        }
        this.mSystemUiVisibility = i5;
        this.mLastNavigationBarMode = i3;
        this.mNavbarColorManagedByIme = z2;
    }

    private void reevaluate() {
        onSystemUiVisibilityChanged(this.mFullscreenStackVisibility, this.mDockedStackVisibility, 0, this.mLastFullscreenBounds, this.mLastDockedBounds, true, this.mLastStatusBarMode, this.mNavbarColorManagedByIme);
        onNavigationVisibilityChanged(this.mSystemUiVisibility, 0, true, this.mLastNavigationBarMode, this.mNavbarColorManagedByIme);
    }

    public void setQsCustomizing(boolean z) {
        if (this.mQsCustomizing != z) {
            this.mQsCustomizing = z;
            reevaluate();
        }
    }

    public void setDirectReplying(boolean z) {
        if (this.mDirectReplying != z) {
            this.mDirectReplying = z;
            reevaluate();
        }
    }

    public void setScrimState(ScrimState scrimState, float f, GradientColors gradientColors) {
        boolean z = this.mForceDarkForScrim;
        this.mForceDarkForScrim = scrimState != ScrimState.BOUNCER && scrimState != ScrimState.BOUNCER_SCRIMMED && f >= 0.1f && !gradientColors.supportsDarkText();
        if (this.mHasLightNavigationBar && this.mForceDarkForScrim != z) {
            reevaluate();
        }
    }

    private boolean animateChange() {
        BiometricUnlockController biometricUnlockController = this.mBiometricUnlockController;
        boolean z = false;
        if (biometricUnlockController == null) {
            return false;
        }
        int mode = biometricUnlockController.getMode();
        if (!(mode == 2 || mode == 1)) {
            z = true;
        }
        return z;
    }

    private void updateStatus(Rect rect, Rect rect2) {
        boolean z = !rect2.isEmpty();
        if ((this.mFullscreenLight && this.mDockedLight) || (this.mFullscreenLight && !z)) {
            this.mStatusBarIconController.setIconsDarkArea(null);
            this.mStatusBarIconController.getTransitionsController().setIconsDark(true, animateChange());
        } else if ((this.mFullscreenLight || this.mDockedLight) && (this.mFullscreenLight || z)) {
            if (!this.mFullscreenLight) {
                rect = rect2;
            }
            if (rect.isEmpty()) {
                this.mStatusBarIconController.setIconsDarkArea(null);
            } else {
                this.mStatusBarIconController.setIconsDarkArea(rect);
            }
            this.mStatusBarIconController.getTransitionsController().setIconsDark(true, animateChange());
        } else {
            this.mStatusBarIconController.getTransitionsController().setIconsDark(false, animateChange());
        }
    }

    private void updateNavigation() {
        LightBarTransitionsController lightBarTransitionsController = this.mNavigationBarController;
        if (lightBarTransitionsController != null) {
            lightBarTransitionsController.setIconsDark(this.mNavigationLight, animateChange());
        }
    }

    public void onPowerSaveChanged(boolean z) {
        reevaluate();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("LightBarController: ");
        printWriter.print(" mSystemUiVisibility=0x");
        printWriter.print(Integer.toHexString(this.mSystemUiVisibility));
        printWriter.print(" mFullscreenStackVisibility=0x");
        printWriter.print(Integer.toHexString(this.mFullscreenStackVisibility));
        printWriter.print(" mDockedStackVisibility=0x");
        printWriter.println(Integer.toHexString(this.mDockedStackVisibility));
        printWriter.print(" mFullscreenLight=");
        printWriter.print(this.mFullscreenLight);
        printWriter.print(" mDockedLight=");
        printWriter.println(this.mDockedLight);
        printWriter.print(" mLastFullscreenBounds=");
        printWriter.print(this.mLastFullscreenBounds);
        printWriter.print(" mLastDockedBounds=");
        printWriter.println(this.mLastDockedBounds);
        printWriter.print(" mNavigationLight=");
        printWriter.print(this.mNavigationLight);
        printWriter.print(" mHasLightNavigationBar=");
        printWriter.println(this.mHasLightNavigationBar);
        printWriter.print(" mLastStatusBarMode=");
        printWriter.print(this.mLastStatusBarMode);
        printWriter.print(" mLastNavigationBarMode=");
        printWriter.println(this.mLastNavigationBarMode);
        printWriter.print(" mForceDarkForScrim=");
        printWriter.print(this.mForceDarkForScrim);
        printWriter.print(" mQsCustomizing=");
        printWriter.print(this.mQsCustomizing);
        printWriter.print(" mDirectReplying=");
        printWriter.println(this.mDirectReplying);
        printWriter.print(" mNavbarColorManagedByIme=");
        printWriter.println(this.mNavbarColorManagedByIme);
        printWriter.println();
        LightBarTransitionsController transitionsController = this.mStatusBarIconController.getTransitionsController();
        if (transitionsController != null) {
            printWriter.println(" StatusBarTransitionsController:");
            transitionsController.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
        }
        if (this.mNavigationBarController != null) {
            printWriter.println(" NavigationBarTransitionsController:");
            this.mNavigationBarController.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
        }
    }
}

package com.android.systemui.statusbar.phone;

import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.util.MathUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.R$dimen;
import com.android.systemui.doze.util.BurnInHelperKt;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.oneplus.systemui.statusbar.phone.OpKeyguardClockPositionAlgorithm;

public class KeyguardClockPositionAlgorithm extends OpKeyguardClockPositionAlgorithm {
    private static float CLOCK_HEIGHT_WEIGHT = 0.7f;
    private static boolean ONEPLUS_PORTING = true;
    private int mBurnInPreventionOffsetX;
    private int mBurnInPreventionOffsetY;
    private int mClockNotificationsMargin;
    private int mClockPreferredY;
    private int mContainerTopPadding;
    private float mDarkAmount;
    private float mEmptyDragAmount;
    private boolean mHasCustomClock;
    private boolean mHasVisibleNotifs;
    private int mHeight;
    private int mKeyguardStatusHeight;
    private int mMaxShadeBottom;
    private int mMinTopMargin;
    private int mNotificationStackHeight;
    private float mPanelExpansion;

    public static class Result {
        public float clockAlpha;
        public int clockX;
        public int clockY;
        public int stackScrollerPadding;
    }

    public void loadDimens(Resources resources) {
        this.mClockNotificationsMargin = resources.getDimensionPixelSize(R$dimen.keyguard_clock_notifications_margin);
        this.mContainerTopPadding = Math.max(resources.getDimensionPixelSize(R$dimen.keyguard_clock_top_margin), resources.getDimensionPixelSize(R$dimen.keyguard_lock_height) + resources.getDimensionPixelSize(R$dimen.keyguard_lock_padding) + resources.getDimensionPixelSize(R$dimen.keyguard_clock_lock_margin));
        this.mBurnInPreventionOffsetX = resources.getDimensionPixelSize(R$dimen.burn_in_prevention_offset_x);
        this.mBurnInPreventionOffsetY = resources.getDimensionPixelSize(R$dimen.burn_in_prevention_offset_y);
        if (ONEPLUS_PORTING) {
            opLoadDimens(resources);
        }
    }

    public void setup(int i, int i2, int i3, float f, int i4, int i5, int i6, boolean z, boolean z2, float f2, float f3) {
        this.mMinTopMargin = this.mContainerTopPadding + i;
        this.mMaxShadeBottom = i2;
        this.mNotificationStackHeight = i3;
        this.mPanelExpansion = f;
        this.mHeight = i4;
        this.mKeyguardStatusHeight = i5;
        this.mClockPreferredY = i6;
        this.mHasCustomClock = z;
        this.mHasVisibleNotifs = z2;
        this.mDarkAmount = f2;
        this.mEmptyDragAmount = f3;
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("setup: minTopMargin= ");
            sb.append(i);
            sb.append(", maxShadeBottom= ");
            sb.append(i2);
            sb.append(", notificationStackHeight= ");
            sb.append(i3);
            sb.append(", panelExpansion= ");
            sb.append(f);
            sb.append(", parentHeight= ");
            sb.append(i4);
            sb.append(", keyguardStatusHeight= ");
            sb.append(i5);
            sb.append(", clockPreferredY= ");
            sb.append(i6);
            sb.append(", hasCustomClock= ");
            sb.append(z);
            sb.append(", hasVisibleNotifs= ");
            sb.append(z2);
            sb.append(", dark= ");
            sb.append(f2);
            sb.append(", emptyDragAmount= ");
            sb.append(f3);
            Log.d("KeyguardClockPositionAlgorithm", sb.toString());
        }
    }

    public void run(Result result) {
        int clockY = getClockY();
        result.clockY = clockY;
        result.clockAlpha = getClockAlpha(clockY);
        result.stackScrollerPadding = clockY + this.mKeyguardStatusHeight;
        result.clockX = (int) NotificationUtils.interpolate(0.0f, burnInPreventionOffsetX(), this.mDarkAmount);
    }

    public float getMinStackScrollerPadding() {
        return (float) (this.mMinTopMargin + this.mKeyguardStatusHeight + this.mClockNotificationsMargin);
    }

    private int getMaxClockY() {
        return opGetMaxClockY();
    }

    private int getPreferredClockY() {
        return this.mClockPreferredY;
    }

    private int getExpandedPreferredClockY() {
        if (!this.mHasCustomClock || this.mHasVisibleNotifs) {
            return getExpandedClockPosition();
        }
        return getPreferredClockY();
    }

    public int getExpandedClockPosition() {
        int i = this.mMaxShadeBottom;
        int i2 = this.mMinTopMargin;
        float f = ((((float) (((i - i2) / 2) + i2)) - (((float) this.mKeyguardStatusHeight) * CLOCK_HEIGHT_WEIGHT)) - ((float) this.mClockNotificationsMargin)) - ((float) (this.mNotificationStackHeight / 2));
        if (f < ((float) i2)) {
            f = (float) i2;
        }
        float maxClockY = (float) getMaxClockY();
        if (f <= maxClockY) {
            maxClockY = f;
        }
        return (int) maxClockY;
    }

    private int getClockY() {
        float max = MathUtils.max(0.0f, ((float) (this.mHasCustomClock ? getPreferredClockY() : getMaxClockY())) + burnInPreventionOffsetY());
        float expandedPreferredClockY = (float) getExpandedPreferredClockY();
        float f = (float) (-this.mKeyguardStatusHeight);
        float interpolation = Interpolators.FAST_OUT_LINEAR_IN.getInterpolation(this.mPanelExpansion);
        return (int) (MathUtils.lerp(MathUtils.lerp(f, expandedPreferredClockY, interpolation), MathUtils.lerp(f, max, interpolation), this.mDarkAmount) + this.mEmptyDragAmount);
    }

    private float getClockAlpha(int i) {
        float max = Math.max(0.0f, ((float) i) / Math.max(1.0f, (float) getExpandedPreferredClockY()));
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("getClockAlpha: y= ");
            sb.append(i);
            sb.append(", expandedPreferredClockY= ");
            sb.append(getExpandedPreferredClockY());
            Log.d("KeyguardClockPositionAlgorithm", sb.toString());
        }
        return MathUtils.lerp(Interpolators.ACCELERATE.getInterpolation(max), 1.0f, this.mDarkAmount);
    }

    private float burnInPreventionOffsetY() {
        return (float) (BurnInHelperKt.getBurnInOffset(this.mBurnInPreventionOffsetY * 2, false) - this.mBurnInPreventionOffsetY);
    }

    private float burnInPreventionOffsetX() {
        return (float) (BurnInHelperKt.getBurnInOffset(this.mBurnInPreventionOffsetX * 2, true) - this.mBurnInPreventionOffsetX);
    }
}

package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.settingslib.Utils;
import com.android.systemui.R$attr;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy.WifiIconState;
import com.oneplus.plugin.OpLsState;
import com.oneplus.util.OpUtils;

public class StatusBarWifiView extends FrameLayout implements DarkReceiver, StatusIconDisplayable {
    private View mAirplaneSpacer;
    private ContextThemeWrapper mDarkContext;
    private float mDarkIntensity = 0.0f;
    private boolean mDirty = true;
    private StatusBarIconView mDotView;
    private View mInoutContainer;
    private ContextThemeWrapper mLightContext;
    private Rect mRect;
    private View mSignalSpacer;
    private String mSlot;
    private WifiIconState mState;
    private int mTint;
    private int mVisibleState = -1;
    private ImageView mWifiActivity;
    private int mWifiActivityId = 0;
    private LinearLayout mWifiGroup;
    private ImageView mWifiIcon;

    public static StatusBarWifiView fromContext(Context context, String str) {
        StatusBarWifiView statusBarWifiView = (StatusBarWifiView) LayoutInflater.from(context).inflate(R$layout.status_bar_wifi_group, null);
        statusBarWifiView.setSlot(str);
        statusBarWifiView.init();
        statusBarWifiView.setVisibleState(0);
        return statusBarWifiView;
    }

    public StatusBarWifiView(Context context) {
        super(context);
    }

    public StatusBarWifiView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public StatusBarWifiView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public StatusBarWifiView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public void setSlot(String str) {
        this.mSlot = str;
    }

    public void setStaticDrawableColor(int i) {
        ColorStateList valueOf = ColorStateList.valueOf(i);
        this.mWifiIcon.setImageTintList(valueOf);
        this.mWifiActivity.setImageTintList(valueOf);
        this.mDotView.setDecorColor(i);
    }

    public void setDecorColor(int i) {
        this.mDotView.setDecorColor(i);
    }

    public String getSlot() {
        return this.mSlot;
    }

    public boolean isIconVisible() {
        WifiIconState wifiIconState = this.mState;
        return wifiIconState != null && wifiIconState.visible;
    }

    public void setVisibleState(int i, boolean z) {
        if (i != this.mVisibleState) {
            this.mVisibleState = i;
            if (i == 0) {
                this.mWifiGroup.setVisibility(0);
                this.mDotView.setVisibility(8);
            } else if (i != 1) {
                this.mWifiGroup.setVisibility(8);
                this.mDotView.setVisibility(8);
            } else {
                this.mWifiGroup.setVisibility(8);
                this.mDotView.setVisibility(0);
            }
        }
    }

    public int getVisibleState() {
        return this.mVisibleState;
    }

    public void getDrawingRect(Rect rect) {
        super.getDrawingRect(rect);
        float translationX = getTranslationX();
        float translationY = getTranslationY();
        rect.left = (int) (((float) rect.left) + translationX);
        rect.right = (int) (((float) rect.right) + translationX);
        rect.top = (int) (((float) rect.top) + translationY);
        rect.bottom = (int) (((float) rect.bottom) + translationY);
    }

    private void init() {
        int themeAttr = Utils.getThemeAttr(this.mContext, R$attr.lightIconTheme);
        int themeAttr2 = Utils.getThemeAttr(this.mContext, R$attr.darkIconTheme);
        this.mLightContext = new ContextThemeWrapper(this.mContext, themeAttr);
        this.mDarkContext = new ContextThemeWrapper(this.mContext, themeAttr2);
        this.mWifiGroup = (LinearLayout) findViewById(R$id.wifi_group);
        this.mWifiIcon = (ImageView) findViewById(R$id.wifi_signal);
        this.mWifiActivity = (ImageView) findViewById(R$id.wifi_inout);
        this.mSignalSpacer = findViewById(R$id.wifi_signal_spacer);
        this.mAirplaneSpacer = findViewById(R$id.wifi_airplane_spacer);
        this.mInoutContainer = findViewById(R$id.inout_container);
        initDotView();
    }

    private void initDotView() {
        this.mDotView = new StatusBarIconView(this.mContext, this.mSlot, null);
        this.mDotView.setVisibleState(1);
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R$dimen.status_bar_icon_size);
        LayoutParams layoutParams = new LayoutParams(dimensionPixelSize, dimensionPixelSize);
        layoutParams.gravity = 8388627;
        addView(this.mDotView, layoutParams);
    }

    public void applyWifiState(WifiIconState wifiIconState) {
        boolean z = true;
        if (wifiIconState == null) {
            if (getVisibility() == 8) {
                z = false;
            }
            setVisibility(8);
            this.mState = null;
        } else {
            WifiIconState wifiIconState2 = this.mState;
            if (wifiIconState2 == null) {
                this.mState = wifiIconState.copy();
                initViewState();
            } else {
                z = !wifiIconState2.equals(wifiIconState) ? updateState(wifiIconState.copy()) : false;
            }
        }
        if (z) {
            requestLayout();
        }
    }

    private boolean updateState(WifiIconState wifiIconState) {
        setContentDescription(wifiIconState.contentDescription);
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("wify icon res name:");
            sb.append(OpUtils.getResourceName(this.mContext, wifiIconState.resId));
            sb.append(", mState.resId:");
            sb.append(this.mState.resId);
            sb.append(", state.resId:");
            sb.append(wifiIconState.resId);
            Log.i("StatusBarWifiView", sb.toString());
        }
        int i = this.mState.resId;
        int i2 = wifiIconState.resId;
        if (i != i2 && i2 >= 0) {
            this.mWifiIcon.setImageDrawable(this.mContext.getDrawable(wifiIconState.resId));
        }
        this.mWifiActivityId = getWifiActivityId(wifiIconState.activityIn, wifiIconState.activityOut);
        int i3 = this.mWifiActivityId;
        if (i3 != 0) {
            this.mWifiActivity.setImageResource(i3);
        }
        int i4 = 8;
        this.mInoutContainer.setVisibility((wifiIconState.activityIn || wifiIconState.activityOut) ? 0 : 8);
        this.mAirplaneSpacer.setVisibility(wifiIconState.airplaneSpacerVisible ? 0 : 8);
        this.mSignalSpacer.setVisibility(wifiIconState.signalSpacerVisible ? 0 : 8);
        boolean z = (wifiIconState.activityIn == this.mState.activityIn && wifiIconState.activityOut == this.mState.activityOut) ? false : true;
        if (this.mState.visible != wifiIconState.visible) {
            z |= true;
            if (wifiIconState.visible) {
                i4 = 0;
            }
            setVisibility(i4);
        }
        this.mState = wifiIconState;
        return z;
    }

    private void initViewState() {
        setContentDescription(this.mState.contentDescription);
        if (this.mState.resId >= 0) {
            this.mWifiIcon.setImageDrawable(this.mContext.getDrawable(this.mState.resId));
        }
        this.mWifiActivityId = getWifiActivityId(this.mState.activityIn, this.mState.activityOut);
        int i = this.mWifiActivityId;
        if (i != 0) {
            this.mWifiActivity.setImageResource(i);
        }
        int i2 = 8;
        this.mInoutContainer.setVisibility((this.mState.activityIn || this.mState.activityOut) ? 0 : 8);
        this.mAirplaneSpacer.setVisibility(this.mState.airplaneSpacerVisible ? 0 : 8);
        this.mSignalSpacer.setVisibility(this.mState.signalSpacerVisible ? 0 : 8);
        if (this.mState.visible) {
            i2 = 0;
        }
        setVisibility(i2);
    }

    public void onDarkChanged(Rect rect, float f, int i) {
        this.mRect = rect;
        this.mDarkIntensity = f;
        this.mTint = i;
        applyColors();
    }

    private void applyColors() {
        Rect rect = this.mRect;
        if (rect != null) {
            float f = this.mDarkIntensity;
            int i = this.mTint;
            this.mDarkIntensity = f;
            this.mWifiIcon.setImageTintList(ColorStateList.valueOf(DarkIconDispatcher.getTint(rect, this, i)));
            this.mWifiActivity.setImageTintList(ColorStateList.valueOf(DarkIconDispatcher.getTint(rect, this, i)));
            this.mDotView.setDecorColor(i);
            this.mDotView.setIconColor(i, false);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StatusBarWifiView(slot=");
        sb.append(this.mSlot);
        sb.append(" state=");
        sb.append(this.mState);
        sb.append(")");
        return sb.toString();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (OpLsState.getInstance().getPhoneStatusBar().isInMultiWindow() && this.mDirty && getWidth() > 0) {
            applyColors();
            this.mDirty = false;
        }
    }

    public void setVisibility(int i) {
        super.setVisibility(i);
        if (OpLsState.getInstance().getPhoneStatusBar().isInMultiWindow() && i == 0) {
            if (getWidth() > 0) {
                applyColors();
            }
            this.mDirty = true;
        }
    }

    private int getWifiActivityId(boolean z, boolean z2) {
        int i = z ? 1 : 0;
        if (z2) {
            i += 2;
        }
        if (i == 1) {
            return R$drawable.stat_sys_signal_in;
        }
        if (i == 2) {
            return R$drawable.stat_sys_signal_out;
        }
        if (i != 3) {
            return R$drawable.stat_sys_signal_none;
        }
        return R$drawable.stat_sys_signal_inout;
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        this.mDirty = true;
    }
}

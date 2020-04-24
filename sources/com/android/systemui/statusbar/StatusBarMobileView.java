package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.DualToneHandler;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy.MobileIconState;
import com.oneplus.plugin.OpLsState;
import com.oneplus.signal.OpSignalIcons;
import com.oneplus.util.OpUtils;

public class StatusBarMobileView extends FrameLayout implements DarkReceiver, StatusIconDisplayable {
    static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    private static final int NOSIM_EDGE_RESID;
    private static final int NOSIM_RESID;
    private float mDarkIntensity;
    private boolean mDirty = true;
    private StatusBarIconView mDotView;
    private DualToneHandler mDualToneHandler;
    private ImageView mIn;
    private View mInoutContainer;
    private ImageView mMobile;
    private SignalDrawable mMobileDrawable;
    private LinearLayout mMobileGroup;
    private ImageView mMobileInOut;
    private ImageView mMobileRoaming;
    private View mMobileRoamingSpace;
    private ViewGroup mMobileSingleGroup;
    private ViewGroup mMobileStackedGroup;
    private ImageView mMobileType;
    private int mMobileTypeOverlap = 0;
    private int mMobileTypeOverlapPlus = 0;
    private ImageView mOut;
    private Rect mRect;
    private String mSlot;
    private ViewGroup mStackedDataGroup;
    private ImageView mStackedDataStrengthView;
    private ImageView mStackedDataTypeView;
    private ViewGroup mStackedVoiceGroup;
    private ImageView mStackedVoiceStrengthView;
    private ImageView mStackedVoiceTypeView;
    private MobileIconState mState;
    private int mTint;
    private int mVisibleState = -1;
    private ImageView mVolte;

    static {
        int i = R$drawable.stat_sys_no_sims_edge;
        NOSIM_RESID = i;
        NOSIM_EDGE_RESID = i;
    }

    public static StatusBarMobileView fromContext(Context context, String str) {
        StatusBarMobileView statusBarMobileView = (StatusBarMobileView) LayoutInflater.from(context).inflate(R$layout.status_bar_mobile_signal_group, null);
        statusBarMobileView.setSlot(str);
        statusBarMobileView.init();
        statusBarMobileView.setVisibleState(0);
        return statusBarMobileView;
    }

    public StatusBarMobileView(Context context) {
        super(context);
    }

    public StatusBarMobileView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public StatusBarMobileView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public StatusBarMobileView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
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
        this.mDualToneHandler = new DualToneHandler(getContext());
        this.mMobileGroup = (LinearLayout) findViewById(R$id.mobile_group);
        this.mMobile = (ImageView) findViewById(R$id.mobile_signal);
        this.mMobileType = (ImageView) findViewById(R$id.mobile_type);
        this.mMobileRoaming = (ImageView) findViewById(R$id.mobile_roaming);
        this.mMobileRoamingSpace = findViewById(R$id.mobile_roaming_space);
        this.mIn = (ImageView) findViewById(R$id.mobile_in);
        this.mOut = (ImageView) findViewById(R$id.mobile_out);
        this.mInoutContainer = findViewById(R$id.inout_container);
        this.mVolte = (ImageView) findViewById(R$id.mobile_volte);
        this.mMobileDrawable = new SignalDrawable(getContext());
        this.mMobile.setImageDrawable(this.mMobileDrawable);
        initDotView();
        this.mStackedDataGroup = (ViewGroup) findViewById(R$id.mobile_signal_data);
        this.mStackedVoiceGroup = (ViewGroup) findViewById(R$id.mobile_signal_voice);
        this.mStackedDataStrengthView = (ImageView) this.mStackedDataGroup.findViewById(R$id.mobile_signal);
        this.mStackedDataTypeView = (ImageView) this.mStackedDataGroup.findViewById(R$id.mobile_type);
        this.mStackedVoiceStrengthView = (ImageView) this.mStackedVoiceGroup.findViewById(R$id.mobile_signal);
        this.mStackedVoiceTypeView = (ImageView) this.mStackedVoiceGroup.findViewById(R$id.mobile_type);
        this.mMobileSingleGroup = (ViewGroup) findViewById(R$id.mobile_signal_single);
        this.mMobileStackedGroup = (ViewGroup) findViewById(R$id.mobile_signal_stacked);
        this.mMobileInOut = (ImageView) findViewById(R$id.mobile_inout);
        this.mMobileTypeOverlap = this.mContext.getResources().getDimensionPixelSize(R$dimen.statusbar_mobile_type_overlap);
        this.mMobileTypeOverlapPlus = this.mContext.getResources().getDimensionPixelSize(R$dimen.statusbar_mobile_type_overlap_plus);
    }

    private void initDotView() {
        this.mDotView = new StatusBarIconView(this.mContext, this.mSlot, null);
        this.mDotView.setVisibleState(1);
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R$dimen.status_bar_icon_size);
        LayoutParams layoutParams = new LayoutParams(dimensionPixelSize, dimensionPixelSize);
        layoutParams.gravity = 8388627;
        addView(this.mDotView, layoutParams);
    }

    public void applyMobileState(MobileIconState mobileIconState) {
        String str;
        String str2 = "StatusBarMobileView";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("applyMobileState / state:");
            String str3 = "null";
            if (mobileIconState == null) {
                str = str3;
            } else {
                str = mobileIconState.toString();
            }
            sb.append(str);
            sb.append(" / mState:");
            MobileIconState mobileIconState2 = this.mState;
            if (mobileIconState2 != null) {
                str3 = mobileIconState2.toString();
            }
            sb.append(str3);
            Log.d(str2, sb.toString());
        }
        boolean z = true;
        if (mobileIconState == null) {
            if (getVisibility() == 8) {
                z = false;
            }
            setVisibility(8);
            this.mState = null;
        } else {
            MobileIconState mobileIconState3 = this.mState;
            if (mobileIconState3 == null) {
                this.mState = mobileIconState.copy();
                initViewState();
            } else {
                z = !mobileIconState3.equals(mobileIconState) ? updateState(mobileIconState.copy()) : false;
            }
        }
        if (z) {
            requestLayout();
        }
        String str4 = " height=";
        if (needFixVisibleState()) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("fix VisibleState width=");
            sb2.append(getWidth());
            sb2.append(str4);
            sb2.append(getHeight());
            Log.d(str2, sb2.toString());
            this.mVisibleState = 0;
            setVisibility(0);
            requestLayout();
        } else if (needFixInVisibleState()) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("fix InVisibleState width=");
            sb3.append(getWidth());
            sb3.append(str4);
            sb3.append(getHeight());
            Log.d(str2, sb3.toString());
            this.mVisibleState = -1;
            setVisibility(4);
            requestLayout();
        }
    }

    private void initViewState() {
        setContentDescription(this.mState.contentDescription);
        int i = 8;
        if (!this.mState.visible) {
            this.mMobileGroup.setVisibility(8);
        } else {
            this.mMobileGroup.setVisibility(0);
        }
        this.mMobileDrawable.setLevel(this.mState.strengthId);
        MobileIconState mobileIconState = this.mState;
        if (mobileIconState.typeId > 0) {
            this.mMobileType.setContentDescription(mobileIconState.typeContentDescription);
            this.mMobileType.setImageResource(this.mState.typeId);
            this.mMobileType.setVisibility(0);
        } else {
            this.mMobileType.setVisibility(8);
        }
        int i2 = this.mState.volteId;
        if (i2 > 0) {
            this.mVolte.setImageResource(i2);
            this.mVolte.setVisibility(0);
        } else {
            this.mVolte.setVisibility(8);
        }
        if (showStatck(this.mState)) {
            this.mStackedDataStrengthView.setImageResource(this.mState.stackedDataStrengthId);
            this.mStackedDataTypeView.setImageResource(this.mState.stackedDataTypeId);
            this.mStackedVoiceStrengthView.setImageResource(this.mState.stackedVoiceStrengthId);
            this.mStackedVoiceTypeView.setImageResource(this.mState.stackedVoiceTypeId);
            this.mMobileSingleGroup.setVisibility(8);
            this.mMobileStackedGroup.setVisibility(0);
        } else {
            MobileIconState mobileIconState2 = this.mState;
            if (mobileIconState2.showNoSim) {
                this.mMobile.setImageResource(mobileIconState2.phoneId == 0 ? NOSIM_RESID : NOSIM_EDGE_RESID);
            } else {
                int i3 = mobileIconState2.strengthId;
                if (i3 != 0) {
                    this.mMobile.setImageResource(i3);
                }
            }
            this.mMobileSingleGroup.setVisibility(0);
            this.mMobileStackedGroup.setVisibility(8);
        }
        this.mMobileInOut.setImageResource(getInOutIndicator(this.mState.activityIn, this.mState.activityOut));
        ImageView imageView = this.mMobileInOut;
        if (this.mState.dataConnected) {
            i = 0;
        }
        imageView.setVisibility(i);
        updateMobileIconPadding();
        updateInOutIndicatorPadding();
    }

    private boolean updateState(MobileIconState mobileIconState) {
        boolean z;
        setContentDescription(mobileIconState.contentDescription);
        boolean z2 = true;
        int i = 8;
        if (this.mState.visible != mobileIconState.visible) {
            this.mMobileGroup.setVisibility(mobileIconState.visible ? 0 : 8);
            z = true;
        } else {
            z = false;
        }
        int i2 = this.mState.strengthId;
        int i3 = mobileIconState.strengthId;
        if (i2 != i3) {
            this.mMobileDrawable.setLevel(i3);
        }
        int i4 = this.mState.typeId;
        int i5 = mobileIconState.typeId;
        if (i4 != i5) {
            z |= i5 == 0 || i4 == 0;
            if (mobileIconState.typeId != 0) {
                this.mMobileType.setContentDescription(mobileIconState.typeContentDescription);
                this.mMobileType.setImageResource(mobileIconState.typeId);
                this.mMobileType.setVisibility(0);
            } else {
                this.mMobileType.setVisibility(8);
            }
        }
        int i6 = this.mState.volteId;
        int i7 = mobileIconState.volteId;
        if (i6 != i7) {
            if (i7 != 0) {
                this.mVolte.setImageResource(i7);
                this.mVolte.setVisibility(0);
            } else {
                this.mVolte.setVisibility(8);
            }
        }
        if (showStatck(mobileIconState)) {
            this.mStackedDataStrengthView.setImageResource(mobileIconState.stackedDataStrengthId);
            this.mStackedDataTypeView.setImageResource(mobileIconState.stackedDataTypeId);
            this.mStackedVoiceStrengthView.setImageResource(mobileIconState.stackedVoiceStrengthId);
            this.mStackedVoiceTypeView.setImageResource(mobileIconState.stackedVoiceTypeId);
            this.mMobileSingleGroup.setVisibility(8);
            this.mMobileStackedGroup.setVisibility(0);
        } else {
            if (mobileIconState.showNoSim) {
                this.mMobile.setImageResource(this.mState.phoneId == 0 ? NOSIM_RESID : NOSIM_EDGE_RESID);
            } else {
                int i8 = mobileIconState.strengthId;
                if (i8 != 0) {
                    this.mMobile.setImageResource(i8);
                }
            }
            this.mMobileSingleGroup.setVisibility(0);
            this.mMobileStackedGroup.setVisibility(8);
        }
        this.mMobileInOut.setImageResource(getInOutIndicator(mobileIconState.activityIn, mobileIconState.activityOut));
        ImageView imageView = this.mMobileInOut;
        if (mobileIconState.dataConnected) {
            i = 0;
        }
        imageView.setVisibility(i);
        if (mobileIconState.roaming == this.mState.roaming && mobileIconState.activityIn == this.mState.activityIn && mobileIconState.activityOut == this.mState.activityOut) {
            z2 = false;
        }
        boolean z3 = z | z2;
        this.mState = mobileIconState;
        updateMobileIconPadding();
        updateInOutIndicatorPadding();
        return z3;
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
            this.mMobileDrawable.setDarkIntensity(f);
            ColorStateList valueOf = ColorStateList.valueOf(DarkIconDispatcher.getTint(rect, this, i));
            this.mMobileType.setImageTintList(valueOf);
            this.mMobileInOut.setImageTintList(valueOf);
            this.mStackedDataStrengthView.setImageTintList(valueOf);
            this.mStackedDataTypeView.setImageTintList(valueOf);
            this.mStackedVoiceStrengthView.setImageTintList(valueOf);
            this.mStackedVoiceTypeView.setImageTintList(valueOf);
            this.mMobile.setImageTintList(valueOf);
            this.mDotView.setDecorColor(i);
            this.mDotView.setIconColor(i, false);
        }
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

    public String getSlot() {
        return this.mSlot;
    }

    public void setSlot(String str) {
        this.mSlot = str;
    }

    public void setStaticDrawableColor(int i) {
        ColorStateList valueOf = ColorStateList.valueOf(i);
        this.mMobileDrawable.setTintList(ColorStateList.valueOf(this.mDualToneHandler.getSingleColor(i == -1 ? 0.0f : 1.0f)));
        this.mMobileType.setImageTintList(valueOf);
        this.mMobileInOut.setImageTintList(valueOf);
        this.mStackedDataStrengthView.setImageTintList(valueOf);
        this.mStackedDataTypeView.setImageTintList(valueOf);
        this.mStackedVoiceStrengthView.setImageTintList(valueOf);
        this.mStackedVoiceTypeView.setImageTintList(valueOf);
        this.mMobile.setImageTintList(valueOf);
    }

    public void setDecorColor(int i) {
        this.mDotView.setDecorColor(i);
    }

    public boolean isIconVisible() {
        return this.mState.visible;
    }

    public void setVisibleState(int i, boolean z) {
        if (i != this.mVisibleState) {
            this.mVisibleState = i;
            if (i == 0) {
                this.mMobileGroup.setVisibility(0);
                this.mDotView.setVisibility(8);
            } else if (i == 1) {
                this.mMobileGroup.setVisibility(4);
                this.mDotView.setVisibility(0);
            } else if (this.mState.visible) {
                this.mMobileGroup.setVisibility(4);
                this.mDotView.setVisibility(4);
            } else {
                this.mMobileGroup.setVisibility(8);
                this.mDotView.setVisibility(8);
            }
        }
    }

    public int getVisibleState() {
        return this.mVisibleState;
    }

    @VisibleForTesting
    public MobileIconState getState() {
        return this.mState;
    }

    private boolean needFixVisibleState() {
        return this.mState.visible && getVisibility() != 0;
    }

    private boolean needFixInVisibleState() {
        return !this.mState.visible && getVisibility() == 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StatusBarMobileView(slot=");
        sb.append(this.mSlot);
        sb.append(" state=");
        sb.append(this.mState);
        sb.append(")");
        return sb.toString();
    }

    private void updateMobileIconPadding() {
        ImageView imageView = this.mMobile;
        if (imageView != null) {
            MarginLayoutParams marginLayoutParams = (MarginLayoutParams) imageView.getLayoutParams();
            int i = 0;
            if (this.mMobileType.getDrawable() != null && this.mMobileType.getVisibility() == 0) {
                int intrinsicWidth = this.mMobileType.getDrawable().getIntrinsicWidth();
                int i2 = this.mState.typeId;
                if (i2 == OpSignalIcons.FOUR_G_LTE || i2 == OpSignalIcons.FOUR_G_PLUS_LTE || OpUtils.isUSS() || (OpUtils.isUST() && this.mState.typeId == R$drawable.stat_sys_data_fully_connected_5g)) {
                    i = intrinsicWidth;
                }
            }
            if (marginLayoutParams.getMarginStart() != i) {
                marginLayoutParams.setMarginStart(i);
                this.mMobile.setLayoutParams(marginLayoutParams);
            }
        }
    }

    private void updateInOutIndicatorPadding() {
        ImageView imageView = this.mMobileInOut;
        if (imageView != null) {
            MarginLayoutParams marginLayoutParams = (MarginLayoutParams) imageView.getLayoutParams();
            int i = marginLayoutParams.topMargin;
            int dimensionPixelSize = (!OpUtils.isUST() || this.mState.typeId != R$drawable.stat_sys_data_fully_connected_5g) ? 0 : this.mContext.getResources().getDimensionPixelSize(R$dimen.op_5g_inout_indicator_margin_top);
            if (marginLayoutParams.topMargin != dimensionPixelSize) {
                marginLayoutParams.topMargin = dimensionPixelSize;
                this.mMobileInOut.setLayoutParams(marginLayoutParams);
            }
        }
    }

    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        if (this.mState != null) {
            ImageView imageView = this.mMobile;
            if (imageView != null && this.mStackedDataStrengthView != null && this.mStackedVoiceStrengthView != null) {
                imageView.setImageDrawable(null);
                this.mStackedDataStrengthView.setImageDrawable(null);
                this.mStackedVoiceStrengthView.setImageDrawable(null);
                updateState(this.mState);
            }
        }
    }

    private int getInOutIndicator(boolean z, boolean z2) {
        int i = z ? 1 : 0;
        if (z2) {
            i += 2;
        }
        if (i == 1) {
            return R$drawable.stat_sys_signal_stacked_in;
        }
        if (i == 2) {
            return R$drawable.stat_sys_signal_stacked_out;
        }
        if (i != 3) {
            return R$drawable.stat_sys_signal_stacked_none;
        }
        return R$drawable.stat_sys_signal_stacked_inout;
    }

    private boolean showStatck(MobileIconState mobileIconState) {
        return (mobileIconState.stackedDataStrengthId == 0 || mobileIconState.stackedVoiceStrengthId == 0 || mobileIconState.stackedDataTypeId == 0 || mobileIconState.stackedVoiceTypeId == 0 || mobileIconState.showNoSim) ? false : true;
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        this.mDirty = true;
    }
}

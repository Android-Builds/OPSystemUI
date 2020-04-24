package com.android.systemui;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.SysuiLifecycle;
import com.android.systemui.util.Utils.DisableStateTracker;
import com.oneplus.battery.OpBatteryDashChargeView;
import com.oneplus.battery.OpBatteryMeterDrawable;
import com.oneplus.plugin.OpLsState;
import com.oneplus.systemui.OpBatteryMeterView;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.NumberFormat;

public class BatteryMeterView extends OpBatteryMeterView {
    private BatteryController mBatteryController;
    private final ImageView mBatteryIconView;
    private boolean mBatteryPercentShow;
    private TextView mBatteryPercentView;
    private boolean mCharging;
    private float mDarkIntensity;
    private boolean mDirty;
    private DualToneHandler mDualToneHandler;
    private boolean mForceShowPercent;
    private boolean mIgnoreTunerUpdates;
    private boolean mIsOptimizatedCharge;
    private boolean mIsSubscribedForTunerUpdates;
    private int mLevel;
    private int mNonAdaptedBackgroundColor;
    private int mNonAdaptedForegroundColor;
    private int mNonAdaptedSingleToneColor;
    private final int mPercentageStyleId;
    private Rect mRect;
    /* access modifiers changed from: private */
    public SettingObserver mSettingObserver;
    private boolean mShowPercentAvailable;
    private int mShowPercentMode;
    private final String mSlotBattery;
    private int mTextColor;
    private int mTint;
    /* access modifiers changed from: private */
    public int mUser;
    private final CurrentUserTracker mUserTracker;

    private final class SettingObserver extends ContentObserver {
        public SettingObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            BatteryMeterView.this.updateShowPercent();
            if (TextUtils.equals(uri.getLastPathSegment(), "battery_estimates_last_update_time")) {
                BatteryMeterView.this.updatePercentText();
            }
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mShowPercentMode = 0;
        this.mDirty = true;
        this.mDarkIntensity = 0.0f;
        this.mBatteryPercentShow = false;
        this.mIsOptimizatedCharge = false;
        setOrientation(0);
        setGravity(8388627);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.BatteryMeterView, i, 0);
        int color = obtainStyledAttributes.getColor(R$styleable.BatteryMeterView_frameColor, context.getColor(R$color.meter_background_color));
        this.mPercentageStyleId = 0;
        this.mDrawable = new OpBatteryMeterDrawable(context, color);
        obtainStyledAttributes.recycle();
        this.mSettingObserver = new SettingObserver(new Handler(context.getMainLooper()));
        this.mShowPercentAvailable = context.getResources().getBoolean(17891372);
        addOnAttachStateChangeListener(new DisableStateTracker(0, 2));
        setupLayoutTransition();
        this.mSlotBattery = context.getString(17041081);
        this.mBatteryIconView = new ImageView(context);
        this.mBatteryIconView.setImageDrawable(this.mDrawable);
        this.mBatteryIconView.setLayerType(1, null);
        MarginLayoutParams marginLayoutParams = new MarginLayoutParams(getResources().getDimensionPixelSize(R$dimen.status_bar_battery_icon_width), getResources().getDimensionPixelSize(R$dimen.status_bar_battery_icon_height));
        int dimensionPixelSize = getResources().getDimensionPixelSize(R$dimen.battery_margin_bottom);
        if (getResources().getConfiguration().densityDpi <= 400) {
            dimensionPixelSize += getResources().getDimensionPixelSize(R$dimen.oneplus_battery_margin_bottom);
        }
        marginLayoutParams.setMargins(0, 0, getResources().getDimensionPixelSize(R$dimen.op_status_bar_battery_icon_margin_right), dimensionPixelSize);
        addView(this.mBatteryIconView, marginLayoutParams);
        this.mBatteryDashChargeView = new OpBatteryDashChargeView(context, null);
        MarginLayoutParams marginLayoutParams2 = new MarginLayoutParams(getResources().getDimensionPixelSize(R$dimen.op_status_bar_battery_dash_icon_width), getResources().getDimensionPixelSize(R$dimen.op_status_bar_battery_dash_icon_height));
        marginLayoutParams2.setMargins(0, 0, 0, 0);
        addView(this.mBatteryDashChargeView, marginLayoutParams2);
        this.mFontScale = this.mContext.getResources().getConfiguration().fontScale;
        updateShowPercent();
        this.mDualToneHandler = new DualToneHandler(context);
        onDarkChanged(new Rect(), 0.0f, -1);
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            public void onUserSwitched(int i) {
                BatteryMeterView.this.mUser = i;
                BatteryMeterView.this.getContext().getContentResolver().unregisterContentObserver(BatteryMeterView.this.mSettingObserver);
                BatteryMeterView.this.getContext().getContentResolver().registerContentObserver(System.getUriFor("status_bar_show_battery_percent"), false, BatteryMeterView.this.mSettingObserver, i);
                BatteryMeterView.this.updateShowPercent();
            }
        };
        setClipChildren(false);
        setClipToPadding(false);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).observe(SysuiLifecycle.viewAttachLifecycle(this), this);
    }

    private void setupLayoutTransition() {
        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setDuration(200);
        String str = "alpha";
        layoutTransition.setAnimator(2, ObjectAnimator.ofFloat(null, str, new float[]{0.0f, 1.0f}));
        layoutTransition.setInterpolator(2, Interpolators.ALPHA_IN);
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(null, str, new float[]{1.0f, 0.0f});
        layoutTransition.setInterpolator(3, Interpolators.ALPHA_OUT);
        layoutTransition.setAnimator(3, ofFloat);
        setLayoutTransition(layoutTransition);
    }

    public void setForceShowPercent(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("setForceShowPercent / mForceShowPercent:");
        sb.append(this.mForceShowPercent);
        sb.append(" / show:");
        sb.append(z);
        Log.i("BatteryMeterView", sb.toString());
        this.mForceShowPercent = z;
        setPercentShowMode(z ? 1 : 0);
    }

    public void setPercentShowMode(int i) {
        this.mShowPercentMode = i;
        updateShowPercent();
    }

    public void setIgnoreTunerUpdates(boolean z) {
        this.mIgnoreTunerUpdates = z;
        updateTunerSubscription();
    }

    private void updateTunerSubscription() {
        if (this.mIgnoreTunerUpdates) {
            unsubscribeFromTunerUpdates();
        } else {
            subscribeForTunerUpdates();
        }
    }

    private void subscribeForTunerUpdates() {
        if (!this.mIsSubscribedForTunerUpdates && !this.mIgnoreTunerUpdates) {
            ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "icon_blacklist");
            this.mIsSubscribedForTunerUpdates = true;
        }
    }

    private void unsubscribeFromTunerUpdates() {
        if (this.mIsSubscribedForTunerUpdates) {
            ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
            this.mIsSubscribedForTunerUpdates = false;
        }
    }

    public void setColorsFromContext(Context context) {
        if (context != null) {
            this.mDualToneHandler.setColorsFromContext(context);
        }
    }

    public void onTuningChanged(String str, String str2) {
        if ("icon_blacklist".equals(str)) {
            StatusBarIconController.getIconBlacklist(str2);
        }
        updateBatteryMeterVisibility();
        updateShowPercent();
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        StringBuilder sb = new StringBuilder();
        sb.append("onAttachedToWindow / ParentView:");
        sb.append(getParent());
        Log.i("BatteryMeterView", sb.toString());
        this.mBatteryController = (BatteryController) Dependency.get(BatteryController.class);
        this.mBatteryController.addCallback(this);
        this.mUser = ActivityManager.getCurrentUser();
        String str = "status_bar_show_battery_percent";
        boolean z = false;
        getContext().getContentResolver().registerContentObserver(System.getUriFor(str), false, this.mSettingObserver, this.mUser);
        getContext().getContentResolver().registerContentObserver(Global.getUriFor("battery_estimates_last_update_time"), false, this.mSettingObserver);
        if (System.getIntForUser(getContext().getContentResolver(), str, 0, 0) != 0) {
            z = true;
        }
        this.mBatteryPercentShow = z;
        updateShowPercent();
        subscribeForTunerUpdates();
        this.mUserTracker.startTracking();
        scaleBatteryMeterViews();
        updateBatteryMeterVisibility();
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        StringBuilder sb = new StringBuilder();
        sb.append("/ onDetachedFromWindow / ParentView:");
        sb.append(getParent());
        Log.i("BatteryMeterView", sb.toString());
        this.mUserTracker.stopTracking();
        this.mBatteryController.removeCallback(this);
        getContext().getContentResolver().unregisterContentObserver(this.mSettingObserver);
        unsubscribeFromTunerUpdates();
    }

    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
        if (Build.DEBUG_ONEPLUS && this.mLevel != i) {
            StringBuilder sb = new StringBuilder();
            sb.append("onBatteryLevelChanged / level:");
            sb.append(i);
            sb.append(" / pluggedIn:");
            sb.append(z);
            sb.append(" / charging:");
            sb.append(z2);
            sb.append(" / ParentView:");
            sb.append(getParent());
            Log.i("BatteryMeterView", sb.toString());
        }
        this.mDrawable.setCharging(z);
        this.mDrawable.setBatteryLevel(i);
        this.mCharging = z;
        this.mLevel = i;
        updatePercentText();
        updateDashChargeView();
    }

    public void onBatteryStyleChanged(int i) {
        this.mDrawable.onBatteryStyleChanged(i);
        this.mBatteryStyle = i;
        updateBatteryMeterVisibility();
        scaleBatteryMeterViews();
        updateViews();
        postInvalidate();
    }

    public void onBatteryPercentShowChange(boolean z) {
        if (this.mBatteryPercentShow != z) {
            this.mBatteryPercentShow = z;
            updateShowPercent();
        }
    }

    public void onFastChargeChanged(int i) {
        boolean z = i > 0;
        if (this.mFastCharge != z) {
            this.mFastCharge = z;
            updateDashChargeView();
            updateBatteryMeterVisibility();
        }
    }

    public void onPowerSaveChanged(boolean z) {
        super.onPowerSaveChanged(z);
    }

    public void onOptimizatedStatusChange(boolean z) {
        if (this.mIsOptimizatedCharge != z) {
            this.mIsOptimizatedCharge = z;
            StringBuilder sb = new StringBuilder();
            sb.append("onOptimizatedStatusChange isOptimizatedCharge");
            sb.append(z);
            sb.append(" / ParentView:");
            sb.append(getParent());
            Log.i("BatteryMeterView", sb.toString());
        }
        updateDashChargeView();
        updateBatteryMeterVisibility();
        this.mDrawable.onOptimizatedStatusChange(z);
        scaleBatteryMeterViews();
        updateViews();
        postInvalidate();
    }

    private TextView loadPercentView() {
        return (TextView) LayoutInflater.from(getContext()).inflate(R$layout.battery_percentage_view, null);
    }

    public void updatePercentView() {
        TextView textView = this.mBatteryPercentView;
        if (textView != null) {
            removeView(textView);
            this.mBatteryPercentView = null;
        }
        updateShowPercent();
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0044, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void updatePercentText() {
        /*
            r5 = this;
            monitor-enter(r5)
            com.android.systemui.statusbar.policy.BatteryController r0 = r5.mBatteryController     // Catch:{ all -> 0x0045 }
            if (r0 != 0) goto L_0x0007
            monitor-exit(r5)
            return
        L_0x0007:
            android.widget.TextView r0 = r5.mBatteryPercentView     // Catch:{ all -> 0x0045 }
            if (r0 == 0) goto L_0x0023
            int r0 = r5.mShowPercentMode     // Catch:{ all -> 0x0045 }
            r1 = 3
            if (r0 != r1) goto L_0x001f
            boolean r0 = r5.mCharging     // Catch:{ all -> 0x0045 }
            if (r0 != 0) goto L_0x001f
            com.android.systemui.statusbar.policy.BatteryController r0 = r5.mBatteryController     // Catch:{ all -> 0x0045 }
            com.android.systemui.-$$Lambda$BatteryMeterView$yZDQalqWJG2q_49RDLUqR8bhWwM r1 = new com.android.systemui.-$$Lambda$BatteryMeterView$yZDQalqWJG2q_49RDLUqR8bhWwM     // Catch:{ all -> 0x0045 }
            r1.<init>()     // Catch:{ all -> 0x0045 }
            r0.getEstimatedTimeRemainingString(r1)     // Catch:{ all -> 0x0045 }
            goto L_0x0043
        L_0x001f:
            r5.setPercentTextAtCurrentLevel()     // Catch:{ all -> 0x0045 }
            goto L_0x0043
        L_0x0023:
            android.content.Context r0 = r5.getContext()     // Catch:{ all -> 0x0045 }
            boolean r1 = r5.mCharging     // Catch:{ all -> 0x0045 }
            if (r1 == 0) goto L_0x002e
            int r1 = com.android.systemui.R$string.accessibility_battery_level_charging     // Catch:{ all -> 0x0045 }
            goto L_0x0030
        L_0x002e:
            int r1 = com.android.systemui.R$string.accessibility_battery_level     // Catch:{ all -> 0x0045 }
        L_0x0030:
            r2 = 1
            java.lang.Object[] r2 = new java.lang.Object[r2]     // Catch:{ all -> 0x0045 }
            r3 = 0
            int r4 = r5.mLevel     // Catch:{ all -> 0x0045 }
            java.lang.Integer r4 = java.lang.Integer.valueOf(r4)     // Catch:{ all -> 0x0045 }
            r2[r3] = r4     // Catch:{ all -> 0x0045 }
            java.lang.String r0 = r0.getString(r1, r2)     // Catch:{ all -> 0x0045 }
            r5.setContentDescription(r0)     // Catch:{ all -> 0x0045 }
        L_0x0043:
            monitor-exit(r5)
            return
        L_0x0045:
            r0 = move-exception
            monitor-exit(r5)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.BatteryMeterView.updatePercentText():void");
    }

    public /* synthetic */ void lambda$updatePercentText$0$BatteryMeterView(String str) {
        if (str != null) {
            this.mBatteryPercentView.setText(str);
            setContentDescription(getContext().getString(R$string.accessibility_battery_level_with_estimate, new Object[]{Integer.valueOf(this.mLevel), str}));
            return;
        }
        setPercentTextAtCurrentLevel();
    }

    private void setPercentTextAtCurrentLevel() {
        int i;
        this.mBatteryPercentView.setText(NumberFormat.getPercentInstance().format((double) (((float) this.mLevel) / 100.0f)));
        Context context = getContext();
        if (this.mCharging) {
            i = R$string.accessibility_battery_level_charging;
        } else {
            i = R$string.accessibility_battery_level;
        }
        setContentDescription(context.getString(i, new Object[]{Integer.valueOf(this.mLevel)}));
    }

    /* access modifiers changed from: private */
    public void updateShowPercent() {
        boolean z = this.mBatteryPercentView != null;
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateShowPercent showing:");
            sb.append(z);
            sb.append(", mBatteryPercentShow:");
            sb.append(this.mBatteryPercentShow);
            sb.append(", mForceShowPercent:");
            sb.append(this.mForceShowPercent);
            Log.i("BatteryMeterView", sb.toString());
        }
        if (this.mBatteryPercentShow || this.mForceShowPercent) {
            if (!z) {
                this.mBatteryPercentView = loadPercentView();
                int i = this.mPercentageStyleId;
                if (i != 0) {
                    this.mBatteryPercentView.setTextAppearance(i);
                }
                int i2 = this.mTextColor;
                if (i2 != 0) {
                    this.mBatteryPercentView.setTextColor(i2);
                }
                updatePercentText();
                addView(this.mBatteryPercentView, 0, new LayoutParams(-2, -1));
                scaleBatteryMeterViews();
            }
        } else if (z) {
            removeView(this.mBatteryPercentView);
            this.mBatteryPercentView = null;
        }
    }

    public void setTextColor(int i) {
        this.mTextColor = i;
        TextView textView = this.mBatteryPercentView;
        if (textView != null) {
            textView.setTextColor(i);
        }
    }

    public void updateDashChargeView() {
        if (showFastCharge()) {
            this.mBatteryDashChargeView.setLevel(this.mLevel);
            this.mBatteryDashChargeView.setVisibility(0);
            return;
        }
        this.mBatteryDashChargeView.setVisibility(8);
    }

    private void updateBatteryMeterVisibility() {
        if (this.mBatteryStyle == 2 || showFastCharge()) {
            this.mBatteryIconView.setVisibility(8);
        } else {
            this.mBatteryIconView.setVisibility(0);
        }
    }

    private boolean showFastCharge() {
        return this.mFastCharge && !this.mIsOptimizatedCharge;
    }

    public void onDensityOrFontScaleChanged() {
        float f = this.mContext.getResources().getConfiguration().fontScale;
        if (this.mFontScale != f) {
            this.mFontScale = f;
        }
        scaleBatteryMeterViews();
    }

    private void scaleBatteryMeterViews() {
        int i;
        Resources resources = getContext().getResources();
        TypedValue typedValue = new TypedValue();
        resources.getValue(R$dimen.status_bar_icon_scale_factor, typedValue, true);
        float f = typedValue.getFloat();
        int dimensionPixelSize = resources.getDimensionPixelSize(R$dimen.status_bar_battery_icon_height);
        if (this.mBatteryStyle == 1) {
            i = dimensionPixelSize;
        } else {
            i = resources.getDimensionPixelSize(R$dimen.status_bar_battery_icon_width);
        }
        int dimensionPixelSize2 = resources.getDimensionPixelSize(R$dimen.battery_margin_bottom);
        if (resources.getConfiguration().densityDpi <= 420) {
            dimensionPixelSize2 += resources.getDimensionPixelSize(R$dimen.oneplus_battery_margin_bottom);
        }
        int dimensionPixelSize3 = getResources().getDimensionPixelSize(R$dimen.op_status_bar_battery_icon_margin_right);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) (((float) i) * f), (int) (((float) dimensionPixelSize) * f));
        layoutParams.setMargins(0, 0, dimensionPixelSize3, dimensionPixelSize2);
        this.mBatteryIconView.setLayoutParams(layoutParams);
        float dimensionPixelSize4 = ((float) this.mContext.getResources().getDimensionPixelSize(R$dimen.battery_level_text_size)) * this.mFontScale;
        TextView textView = this.mBatteryPercentView;
        if (textView != null) {
            textView.setTextSize(0, dimensionPixelSize4);
        }
        this.mBatteryDashChargeView.updateViews();
        this.mBatteryDashChargeView.setLayoutParams((LinearLayout.LayoutParams) this.mBatteryDashChargeView.getLayoutParams());
    }

    public void onDarkChanged(Rect rect, float f, int i) {
        this.mRect = rect;
        this.mDarkIntensity = f;
        this.mTint = i;
        updateAllBatteryColors();
    }

    public void updateColors(int i, int i2, int i3) {
        updateColors(i, i2);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String str;
        CharSequence charSequence = null;
        if (this.mDrawable == null) {
            str = null;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(this.mDrawable.getPowerSave());
            sb.append("");
            str = sb.toString();
        }
        TextView textView = this.mBatteryPercentView;
        if (textView != null) {
            charSequence = textView.getText();
        }
        printWriter.println("  BatteryMeterView:");
        StringBuilder sb2 = new StringBuilder();
        sb2.append("    mDrawable.getPowerSave: ");
        sb2.append(str);
        printWriter.println(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append("    mBatteryPercentView.getText(): ");
        sb3.append(charSequence);
        printWriter.println(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append("    mTextColor: #");
        sb4.append(Integer.toHexString(this.mTextColor));
        printWriter.println(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append("    mLevel: ");
        sb5.append(this.mLevel);
        printWriter.println(sb5.toString());
        StringBuilder sb6 = new StringBuilder();
        sb6.append("    mForceShowPercent: ");
        sb6.append(this.mForceShowPercent);
        printWriter.println(sb6.toString());
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (OpLsState.getInstance().getPhoneStatusBar().isInMultiWindow() && this.mDirty && getWidth() > 0) {
            updateAllBatteryColors();
            this.mDirty = false;
        }
    }

    private void updateAllBatteryColors() {
        Rect rect = this.mRect;
        if (rect != null) {
            float f = DarkIconDispatcher.isInArea(rect, this) ? this.mDarkIntensity : 0.0f;
            this.mNonAdaptedSingleToneColor = this.mDualToneHandler.getSingleColor(f);
            this.mNonAdaptedForegroundColor = this.mDualToneHandler.getFillColor(f);
            this.mNonAdaptedBackgroundColor = this.mDualToneHandler.getBackgroundColor(f);
            int tint = DarkIconDispatcher.getTint(this.mRect, this, this.mTint);
            setTextColor(tint);
            this.mBatteryDashChargeView.setIconTint(tint);
            updateColors(tint, this.mNonAdaptedBackgroundColor, this.mNonAdaptedSingleToneColor);
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        this.mDirty = true;
    }
}

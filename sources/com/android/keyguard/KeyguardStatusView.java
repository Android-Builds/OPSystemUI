package com.android.keyguard;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.TimeZone;

public class KeyguardStatusView extends GridLayout implements ConfigurationListener {
    private int mBottomMargin;
    private int mBottomMarginWithHeader;
    private boolean mCharging;
    private TextView mChargingInfo;
    private KeyguardClockSwitch mClockView;
    private float mDarkAmount;
    private Handler mHandler;
    private final IActivityManager mIActivityManager;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    private ViewGroup mInfoView;
    private KeyguardSliceView mKeyguardSlice;
    private ViewGroup mLockIconContainer;
    private final LockPatternUtils mLockPatternUtils;
    private TextView mLogoutView;
    private TextView mOwnerInfo;
    private Runnable mPendingMarqueeStart;
    private boolean mPulsing;
    private boolean mShowingHeader;
    private LinearLayout mStatusViewContainer;
    private int mTextColor;

    private static final class Patterns {
        static String cacheKey;
        static String clockView12;
        static String clockView24;

        static void update(Context context) {
            Locale locale = Locale.getDefault();
            Resources resources = context.getResources();
            String string = resources.getString(R$string.clock_12hr_format);
            String string2 = resources.getString(R$string.clock_24hr_format);
            StringBuilder sb = new StringBuilder();
            sb.append(locale.toString());
            sb.append(string);
            sb.append(string2);
            String sb2 = sb.toString();
            if (!sb2.equals(cacheKey)) {
                clockView12 = DateFormat.getBestDateTimePattern(locale, string);
                String str = "a";
                if (!string.contains(str)) {
                    clockView12 = clockView12.replaceAll(str, "").trim();
                }
                clockView24 = DateFormat.getBestDateTimePattern(locale, string2);
                clockView24 = clockView24.replace(':', 60929);
                clockView12 = clockView12.replace(':', 60929);
                cacheKey = sb2;
            }
        }
    }

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mDarkAmount = 0.0f;
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onTimeChanged() {
                KeyguardStatusView.this.refreshTime();
            }

            public void onTimeZoneChanged(TimeZone timeZone) {
                KeyguardStatusView.this.updateTimeZone(timeZone);
            }

            public void onKeyguardVisibilityChanged(boolean z) {
                if (z) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("refresh statusview showing:");
                    sb.append(z);
                    Slog.v("KeyguardStatusView", sb.toString());
                    KeyguardStatusView.this.refreshTime();
                    KeyguardStatusView.this.updateOwnerInfo();
                    KeyguardStatusView.this.updateLogoutView();
                }
            }

            public void onStartedWakingUp() {
                KeyguardStatusView.this.setEnableMarquee(true);
            }

            public void onFinishedGoingToSleep(int i) {
                KeyguardStatusView.this.setEnableMarquee(false);
            }

            public void onUserSwitchComplete(int i) {
                KeyguardStatusView.this.refreshFormat();
                KeyguardStatusView.this.updateOwnerInfo();
                KeyguardStatusView.this.updateLogoutView();
            }

            public void onLogoutEnabledChanged() {
                KeyguardStatusView.this.updateLogoutView();
            }
        };
        this.mIActivityManager = ActivityManager.getService();
        this.mLockPatternUtils = new LockPatternUtils(getContext());
        this.mHandler = new Handler(Looper.myLooper());
        onDensityOrFontScaleChanged();
    }

    public boolean hasCustomClock() {
        return this.mClockView.hasCustomClock();
    }

    /* access modifiers changed from: private */
    public void setEnableMarquee(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("Schedule setEnableMarquee: ");
        sb.append(z ? "Enable" : "Disable");
        Log.v("KeyguardStatusView", sb.toString());
        if (!z) {
            Runnable runnable = this.mPendingMarqueeStart;
            if (runnable != null) {
                this.mHandler.removeCallbacks(runnable);
                this.mPendingMarqueeStart = null;
            }
            setEnableMarqueeImpl(false);
        } else if (this.mPendingMarqueeStart == null) {
            this.mPendingMarqueeStart = new Runnable() {
                public final void run() {
                    KeyguardStatusView.this.lambda$setEnableMarquee$0$KeyguardStatusView();
                }
            };
            this.mHandler.postDelayed(this.mPendingMarqueeStart, 2000);
        }
    }

    public /* synthetic */ void lambda$setEnableMarquee$0$KeyguardStatusView() {
        setEnableMarqueeImpl(true);
        this.mPendingMarqueeStart = null;
    }

    private void setEnableMarqueeImpl(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append(z ? "Enable" : "Disable");
        sb.append(" transport text marquee");
        Log.v("KeyguardStatusView", sb.toString());
        TextView textView = this.mOwnerInfo;
        if (textView != null) {
            textView.setSelected(z);
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mStatusViewContainer = (LinearLayout) findViewById(R$id.status_view_container);
        this.mLogoutView = (TextView) findViewById(R$id.logout);
        TextView textView = this.mLogoutView;
        if (textView != null) {
            textView.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    KeyguardStatusView.this.onLogoutClicked(view);
                }
            });
        }
        this.mClockView = (KeyguardClockSwitch) findViewById(R$id.keyguard_clock_container);
        this.mClockView.setShowCurrentUserTime(true);
        if (KeyguardClockAccessibilityDelegate.isNeeded(this.mContext)) {
            this.mClockView.setAccessibilityDelegate(new KeyguardClockAccessibilityDelegate(this.mContext));
        }
        this.mInfoView = (ViewGroup) findViewById(R$id.charging_and_owner_info_view);
        if (this.mInfoView != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("onFinishInflate, mInfoView.getVisibility():");
            sb.append(this.mInfoView.getVisibility());
            sb.append(", mInfoViewID:");
            sb.append(this.mInfoView.getId());
            sb.append("resId:");
            sb.append(R$id.charging_and_owner_info_view);
            sb.append(", mInfoView:");
            sb.append(this.mInfoView);
            sb.append(", mClockView:");
            sb.append(this.mClockView);
            Log.d("KeyguardStatusView", sb.toString());
        }
        this.mOwnerInfo = (TextView) findViewById(R$id.owner_info);
        this.mKeyguardSlice = (KeyguardSliceView) findViewById(R$id.keyguard_status_area);
        this.mTextColor = this.mClockView.getCurrentTextColor();
        this.mKeyguardSlice.setContentChangeListener(new Runnable() {
            public final void run() {
                KeyguardStatusView.this.onSliceContentChanged();
            }
        });
        onSliceContentChanged();
        setEnableMarquee(KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive());
        refreshFormat();
        updateOwnerInfo();
        updateLogoutView();
        updateDark();
        this.mChargingInfo = (TextView) findViewById(R$id.charging_info);
        this.mLockIconContainer = (ViewGroup) findViewById(R$id.lock_icon_container);
    }

    /* access modifiers changed from: private */
    public void onSliceContentChanged() {
        boolean hasHeader = this.mKeyguardSlice.hasHeader();
        this.mClockView.setKeyguardShowingHeader(hasHeader);
        if (this.mShowingHeader != hasHeader) {
            this.mShowingHeader = hasHeader;
            LinearLayout linearLayout = this.mStatusViewContainer;
            if (linearLayout != null) {
                MarginLayoutParams marginLayoutParams = (MarginLayoutParams) linearLayout.getLayoutParams();
                marginLayoutParams.setMargins(marginLayoutParams.leftMargin, marginLayoutParams.topMargin, marginLayoutParams.rightMargin, hasHeader ? this.mBottomMarginWithHeader : this.mBottomMargin);
                this.mStatusViewContainer.setLayoutParams(marginLayoutParams);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        layoutOwnerInfo();
    }

    public void onDensityOrFontScaleChanged() {
        KeyguardClockSwitch keyguardClockSwitch = this.mClockView;
        if (keyguardClockSwitch != null) {
            keyguardClockSwitch.setTextSize(0, (float) getResources().getDimensionPixelSize(OpUtils.isMCLVersion() ? R$dimen.oneplus_widget_big_font_size_mcl : R$dimen.oneplus_widget_big_font_size));
        }
        TextView textView = this.mOwnerInfo;
        if (textView != null) {
            textView.setTextSize(0, (float) getResources().getDimensionPixelSize(R$dimen.oneplus_widget_label_font_size));
        }
        TextView textView2 = this.mChargingInfo;
        if (textView2 != null) {
            textView2.setTextSize(0, (float) getResources().getDimensionPixelSize(R$dimen.oneplus_widget_label_font_size));
            if (OpUtils.isMCLVersionFont()) {
                Typeface mclTypeface = OpUtils.getMclTypeface(3);
                if (mclTypeface != null) {
                    this.mChargingInfo.setTypeface(mclTypeface);
                }
            }
        }
        loadBottomMargin();
    }

    public void dozeTimeTick() {
        refreshTime();
        this.mKeyguardSlice.refresh();
    }

    /* access modifiers changed from: private */
    public void refreshTime() {
        this.mClockView.refresh();
    }

    /* access modifiers changed from: private */
    public void updateTimeZone(TimeZone timeZone) {
        this.mClockView.onTimeZoneChanged(timeZone);
    }

    /* access modifiers changed from: private */
    public void refreshFormat() {
        Patterns.update(this.mContext);
        this.mClockView.setFormat12Hour(Patterns.clockView12);
        this.mClockView.setFormat24Hour(Patterns.clockView24);
    }

    public int getLogoutButtonHeight() {
        TextView textView = this.mLogoutView;
        int i = 0;
        if (textView == null) {
            return 0;
        }
        if (textView.getVisibility() == 0) {
            i = this.mLogoutView.getHeight();
        }
        return i;
    }

    public float getClockTextSize() {
        return this.mClockView.getTextSize();
    }

    public int getClockPreferredY(int i) {
        return this.mClockView.getPreferredY(i);
    }

    /* access modifiers changed from: private */
    public void updateLogoutView() {
        TextView textView = this.mLogoutView;
        if (textView != null) {
            textView.setVisibility(shouldShowLogout() ? 0 : 8);
            this.mLogoutView.setText(this.mContext.getResources().getString(17040069));
        }
    }

    /* access modifiers changed from: private */
    public void updateOwnerInfo() {
        if (this.mOwnerInfo != null) {
            String deviceOwnerInfo = this.mLockPatternUtils.getDeviceOwnerInfo();
            if (deviceOwnerInfo == null && this.mLockPatternUtils.isOwnerInfoEnabled(KeyguardUpdateMonitor.getCurrentUser())) {
                deviceOwnerInfo = this.mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
            }
            this.mOwnerInfo.setText(deviceOwnerInfo);
            updateDark();
            updateInfoViewVisibility();
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mInfoCallback);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mInfoCallback);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    public void onLocaleListChanged() {
        refreshFormat();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        Object obj;
        printWriter.println("KeyguardStatusView:");
        StringBuilder sb = new StringBuilder();
        sb.append("  mOwnerInfo: ");
        TextView textView = this.mOwnerInfo;
        boolean z = true;
        if (textView == null) {
            obj = "null";
        } else {
            obj = Boolean.valueOf(textView.getVisibility() == 0);
        }
        sb.append(obj);
        printWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("  mPulsing: ");
        sb2.append(this.mPulsing);
        printWriter.println(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append("  mDarkAmount: ");
        sb3.append(this.mDarkAmount);
        printWriter.println(sb3.toString());
        StringBuilder sb4 = new StringBuilder();
        sb4.append("  mTextColor: ");
        sb4.append(Integer.toHexString(this.mTextColor));
        printWriter.println(sb4.toString());
        StringBuilder sb5 = new StringBuilder();
        sb5.append("  mInfoView.getVisibility : ");
        ViewGroup viewGroup = this.mInfoView;
        sb5.append(viewGroup == null ? null : Integer.valueOf(viewGroup.getVisibility()));
        printWriter.println(sb5.toString());
        if (this.mLogoutView != null) {
            StringBuilder sb6 = new StringBuilder();
            sb6.append("  logout visible: ");
            if (this.mLogoutView.getVisibility() != 0) {
                z = false;
            }
            sb6.append(z);
            printWriter.println(sb6.toString());
        }
        KeyguardClockSwitch keyguardClockSwitch = this.mClockView;
        if (keyguardClockSwitch != null) {
            keyguardClockSwitch.dump(fileDescriptor, printWriter, strArr);
        }
        KeyguardSliceView keyguardSliceView = this.mKeyguardSlice;
        if (keyguardSliceView != null) {
            keyguardSliceView.dump(fileDescriptor, printWriter, strArr);
        }
    }

    private void loadBottomMargin() {
        this.mBottomMargin = getResources().getDimensionPixelSize(R$dimen.widget_vertical_padding);
        this.mBottomMarginWithHeader = getResources().getDimensionPixelSize(R$dimen.widget_vertical_padding_with_header);
    }

    public void setDarkAmount(float f) {
        if (this.mDarkAmount != f) {
            this.mDarkAmount = f;
            this.mClockView.setDarkAmount(f);
            updateDark();
        }
    }

    private void updateDark() {
        float f = 1.0f;
        int i = 0;
        boolean z = this.mDarkAmount == 1.0f;
        TextView textView = this.mLogoutView;
        if (textView != null) {
            if (z) {
                f = 0.0f;
            }
            textView.setAlpha(f);
        }
        TextView textView2 = this.mOwnerInfo;
        if (textView2 != null) {
            boolean z2 = !TextUtils.isEmpty(textView2.getText());
            TextView textView3 = this.mOwnerInfo;
            if (!z2) {
                i = 8;
            }
            textView3.setVisibility(i);
            layoutOwnerInfo();
        }
        int blendARGB = ColorUtils.blendARGB(this.mTextColor, -1, this.mDarkAmount);
        this.mKeyguardSlice.setDarkAmount(this.mDarkAmount);
        this.mClockView.setTextColor(blendARGB);
    }

    private void layoutOwnerInfo() {
        TextView textView = this.mOwnerInfo;
        if (textView != null && textView.getVisibility() != 8) {
            this.mOwnerInfo.setAlpha(1.0f - this.mDarkAmount);
            setBottom(getMeasuredHeight() - ((int) (((float) ((this.mOwnerInfo.getBottom() + this.mOwnerInfo.getPaddingBottom()) - (this.mOwnerInfo.getTop() - this.mOwnerInfo.getPaddingTop()))) * this.mDarkAmount)));
        }
    }

    public void setPulsing(boolean z) {
        if (this.mPulsing != z) {
            this.mPulsing = z;
        }
    }

    private boolean shouldShowLogout() {
        return KeyguardUpdateMonitor.getInstance(this.mContext).isLogoutEnabled() && KeyguardUpdateMonitor.getCurrentUser() != 0;
    }

    /* access modifiers changed from: private */
    public void onLogoutClicked(View view) {
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        try {
            this.mIActivityManager.switchUser(0);
            this.mIActivityManager.stopUser(currentUser, true, null);
        } catch (RemoteException e) {
            Log.e("KeyguardStatusView", "Failed to logout user", e);
        }
    }

    public void setCharging(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("setCharging: ");
        sb.append(z);
        Log.d("KeyguardStatusView", sb.toString());
        this.mCharging = z;
        updateInfoViewVisibility();
    }

    public void updateInfoViewVisibility() {
        String str = "KeyguardStatusView";
        if (this.mInfoView == null) {
            Log.d(str, "updateInfoViewVisibility, mInfoView == null");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("updateInfoViewVisibility, mInfoView.getVisibility():");
            sb.append(this.mInfoView.getVisibility());
            sb.append(", mInfoViewID:");
            sb.append(this.mInfoView.getId());
            sb.append("resId:");
            sb.append(R$id.charging_and_owner_info_view);
            sb.append(", mInfoView:");
            sb.append(this.mInfoView);
            sb.append(", mOwnerInfo:");
            sb.append(this.mOwnerInfo);
            sb.append(", mClockView:");
            sb.append(this.mClockView);
            Log.d(str, sb.toString());
        }
        this.mInfoView = (ViewGroup) findViewById(R$id.charging_and_owner_info_view);
        TextView textView = this.mOwnerInfo;
        if (textView != null) {
            if (this.mCharging) {
                textView.setVisibility(8);
            } else {
                this.mOwnerInfo.setVisibility((!(TextUtils.isEmpty(textView.getText()) ^ true) || this.mDarkAmount == 1.0f || this.mCharging) ? 8 : 0);
            }
        }
        TextView textView2 = this.mOwnerInfo;
        if ((textView2 == null || textView2.getVisibility() != 0) && !this.mCharging) {
            this.mInfoView.setVisibility(8);
        } else {
            this.mInfoView.setVisibility(0);
        }
    }

    public boolean hasOwnerInfo() {
        if (!(!TextUtils.isEmpty(this.mOwnerInfo.getText())) || this.mDarkAmount == 1.0f) {
            return false;
        }
        return true;
    }

    public ViewGroup getLockIconContainer() {
        return this.mLockIconContainer;
    }
}

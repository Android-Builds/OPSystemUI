package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import com.android.settingslib.Utils;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R$attr;
import com.android.systemui.R$dimen;
import com.android.systemui.R$styleable;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.CommandQueue.Callbacks;
import com.android.systemui.statusbar.phone.HighlightHintController;
import com.android.systemui.statusbar.phone.HighlightHintController.OnHighlightHintStateChangeListener;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import libcore.icu.LocaleData;

public class Clock extends TextView implements DemoMode, Tunable, Callbacks, DarkReceiver, ConfigurationListener, OnHighlightHintStateChangeListener {
    /* access modifiers changed from: private */
    public static final String TAG = "Clock";
    private final int mAmPmStyle;
    private boolean mAttached;
    /* access modifiers changed from: private */
    public Calendar mCalendar;
    /* access modifiers changed from: private */
    public SimpleDateFormat mClockFormat;
    /* access modifiers changed from: private */
    public String mClockFormatString;
    private boolean mClockVisibleByPolicy;
    private boolean mClockVisibleByUser;
    private SimpleDateFormat mContentDescriptionFormat;
    /* access modifiers changed from: private */
    public int mCurrentUserId;
    private final CurrentUserTracker mCurrentUserTracker;
    private boolean mDemoMode;
    private final BroadcastReceiver mIntentReceiver;
    /* access modifiers changed from: private */
    public Locale mLocale;
    private int mNonAdaptedColor;
    private final BroadcastReceiver mScreenReceiver;
    /* access modifiers changed from: private */
    public final Runnable mSecondTick;
    /* access modifiers changed from: private */
    public Handler mSecondsHandler;
    private final boolean mShowDark;
    private boolean mShowSeconds;
    /* access modifiers changed from: private */
    public int mTimeTickCount;
    private boolean mUseWallpaperTextColor;

    public Clock(Context context) {
        this(context, null);
    }

    public Clock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    /* JADX INFO: finally extract failed */
    public Clock(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mClockVisibleByPolicy = true;
        this.mClockVisibleByUser = true;
        this.mTimeTickCount = 0;
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Clock.this.getHandler() == null) {
                    String access$100 = Clock.TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("getHandler is null, action = ");
                    sb.append(action);
                    sb.append(", tag:");
                    sb.append(Clock.this.getTag());
                    Log.d(access$100, sb.toString());
                }
                if (action.equals("android.intent.action.TIMEZONE_CHANGED")) {
                    String stringExtra = intent.getStringExtra("time-zone");
                    if (Clock.this.getHandler() != null) {
                        Clock.this.getHandler().post(new Runnable(stringExtra) {
                            private final /* synthetic */ String f$1;

                            {
                                this.f$1 = r2;
                            }

                            public final void run() {
                                C15802.this.lambda$onReceive$0$Clock$2(this.f$1);
                            }
                        });
                    } else {
                        return;
                    }
                } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                    Locale locale = Clock.this.getResources().getConfiguration().locale;
                    if (Clock.this.getHandler() != null) {
                        Clock.this.getHandler().post(new Runnable(locale) {
                            private final /* synthetic */ Locale f$1;

                            {
                                this.f$1 = r2;
                            }

                            public final void run() {
                                C15802.this.lambda$onReceive$1$Clock$2(this.f$1);
                            }
                        });
                    } else {
                        return;
                    }
                }
                if (Clock.this.getHandler() != null) {
                    if (action.equals("android.intent.action.TIME_TICK") && !Clock.this.showSeconds() && Clock.this.mSecondsHandler != null && Clock.this.mSecondsHandler.hasCallbacks(Clock.this.mSecondTick)) {
                        if (Clock.this.mTimeTickCount > 3) {
                            Log.d(Clock.TAG, "time_tick, remove call back");
                            Clock.this.mSecondsHandler.removeCallbacks(Clock.this.mSecondTick);
                        } else {
                            Clock.this.mTimeTickCount = Clock.this.mTimeTickCount + 1;
                            String access$1002 = Clock.TAG;
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("time_tick, counter:");
                            sb2.append(Clock.this.mTimeTickCount);
                            Log.d(access$1002, sb2.toString());
                        }
                    }
                    Clock.this.getHandler().post(new Runnable() {
                        public final void run() {
                            C15802.this.lambda$onReceive$2$Clock$2();
                        }
                    });
                }
            }

            public /* synthetic */ void lambda$onReceive$0$Clock$2(String str) {
                Clock.this.mCalendar = Calendar.getInstance(TimeZone.getTimeZone(str));
                if (Clock.this.mClockFormat != null) {
                    Clock.this.mClockFormat.setTimeZone(Clock.this.mCalendar.getTimeZone());
                }
            }

            public /* synthetic */ void lambda$onReceive$1$Clock$2(Locale locale) {
                if (!locale.equals(Clock.this.mLocale)) {
                    Clock.this.mLocale = locale;
                    Clock.this.mClockFormatString = "";
                }
            }

            public /* synthetic */ void lambda$onReceive$2$Clock$2() {
                Clock.this.updateClock();
            }
        };
        this.mScreenReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    if (Clock.this.mSecondsHandler != null) {
                        Clock.this.mSecondsHandler.removeCallbacks(Clock.this.mSecondTick);
                    }
                } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                    Clock.this.mTimeTickCount = 0;
                    Clock.this.runSecondTick();
                }
            }
        };
        this.mSecondTick = new Runnable() {
            public void run() {
                if (Clock.this.mCalendar != null) {
                    Clock.this.updateClock();
                }
                long uptimeMillis = SystemClock.uptimeMillis();
                Clock.this.mSecondsHandler.postAtTime(this, Clock.this.showSeconds() ? ((uptimeMillis / 1000) * 1000) + 1000 : ((uptimeMillis / 60000) + 1) * 60000);
            }
        };
        TypedArray obtainStyledAttributes = context.getTheme().obtainStyledAttributes(attributeSet, R$styleable.Clock, 0, 0);
        try {
            this.mAmPmStyle = obtainStyledAttributes.getInt(R$styleable.Clock_amPmStyle, 2);
            this.mShowDark = obtainStyledAttributes.getBoolean(R$styleable.Clock_showDark, true);
            this.mNonAdaptedColor = getCurrentTextColor();
            obtainStyledAttributes.recycle();
            this.mCurrentUserTracker = new CurrentUserTracker(context) {
                public void onUserSwitched(int i) {
                    Clock.this.mCurrentUserId = i;
                }
            };
        } catch (Throwable th) {
            obtainStyledAttributes.recycle();
            throw th;
        }
    }

    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("clock_super_parcelable", super.onSaveInstanceState());
        bundle.putInt("current_user_id", this.mCurrentUserId);
        bundle.putBoolean("visible_by_policy", this.mClockVisibleByPolicy);
        bundle.putBoolean("visible_by_user", this.mClockVisibleByUser);
        bundle.putBoolean("show_seconds", this.mShowSeconds);
        bundle.putInt("visibility", getVisibility());
        return bundle;
    }

    public void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable == null || !(parcelable instanceof Bundle)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        Bundle bundle = (Bundle) parcelable;
        super.onRestoreInstanceState(bundle.getParcelable("clock_super_parcelable"));
        String str = "current_user_id";
        if (bundle.containsKey(str)) {
            this.mCurrentUserId = bundle.getInt(str);
        }
        this.mClockVisibleByPolicy = bundle.getBoolean("visible_by_policy", true);
        this.mClockVisibleByUser = bundle.getBoolean("visible_by_user", true);
        this.mShowSeconds = bundle.getBoolean("show_seconds", false);
        String str2 = "visibility";
        if (bundle.containsKey(str2)) {
            super.setVisibility(bundle.getInt(str2));
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mAttached) {
            this.mAttached = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.TIME_TICK");
            intentFilter.addAction("android.intent.action.TIME_SET");
            intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            intentFilter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            intentFilter.addAction("android.intent.action.USER_SWITCHED");
            getContext().registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, intentFilter, null, (Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
            IntentFilter intentFilter2 = new IntentFilter();
            intentFilter2.addAction("android.intent.action.SCREEN_OFF");
            intentFilter2.addAction("android.intent.action.SCREEN_ON");
            getContext().registerReceiver(this.mScreenReceiver, intentFilter2);
            runSecondTick();
            ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "clock_seconds", "icon_blacklist");
            ((CommandQueue) SysUiServiceProvider.getComponent(getContext(), CommandQueue.class)).addCallback((Callbacks) this);
            if (this.mShowDark) {
                ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver((DarkReceiver) this);
            }
            this.mCurrentUserTracker.startTracking();
            this.mCurrentUserId = this.mCurrentUserTracker.getCurrentUserId();
        }
        this.mCalendar = Calendar.getInstance(TimeZone.getDefault());
        updateClock();
        updateClockVisibility();
        updateShowSeconds();
        ((HighlightHintController) Dependency.get(HighlightHintController.class)).addCallback(this);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAttached) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("unregisterReceiver, tag:");
            sb.append(getTag());
            Log.d(str, sb.toString());
            getContext().unregisterReceiver(this.mIntentReceiver);
            this.mAttached = false;
            ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
            ((CommandQueue) SysUiServiceProvider.getComponent(getContext(), CommandQueue.class)).removeCallback((Callbacks) this);
            if (this.mShowDark) {
                ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver((DarkReceiver) this);
            }
            this.mCurrentUserTracker.stopTracking();
        }
        ((HighlightHintController) Dependency.get(HighlightHintController.class)).removeCallback(this);
        releaseReceiver();
    }

    public void setVisibility(int i) {
        if (i == 0 && !shouldBeVisible()) {
            i = 8;
        }
        super.setVisibility(i);
    }

    public void setClockVisibleByUser(boolean z) {
        this.mClockVisibleByUser = z;
        updateClockVisibility();
    }

    public void setClockVisibilityByPolicy(boolean z) {
        this.mClockVisibleByPolicy = z;
        updateClockVisibility();
    }

    private boolean shouldBeVisible() {
        return this.mClockVisibleByPolicy && this.mClockVisibleByUser;
    }

    private void updateClockVisibility() {
        super.setVisibility(shouldBeVisible() ? 0 : 8);
    }

    /* access modifiers changed from: 0000 */
    public final void updateClock() {
        if (this.mDemoMode) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("updateClock is in demo mode, tag:");
            sb.append(getTag());
            Log.d(str, sb.toString());
            return;
        }
        Calendar calendar = this.mCalendar;
        if (calendar != null) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            setText(getSmallTime());
            setContentDescription(this.mContentDescriptionFormat.format(this.mCalendar.getTime()));
        }
    }

    public void onTuningChanged(String str, String str2) {
        if ("clock_seconds".equals(str)) {
            this.mShowSeconds = TunerService.parseIntegerSwitch(str2, false);
            updateShowSeconds();
            return;
        }
        setClockVisibleByUser(!StatusBarIconController.getIconBlacklist(str2).contains("clock"));
        updateClockVisibility();
    }

    public void disable(int i, int i2, int i3, boolean z) {
        if (i == getDisplay().getDisplayId()) {
            boolean z2 = (8388608 & i2) == 0;
            if (z2 != this.mClockVisibleByPolicy) {
                setClockVisibilityByPolicy(z2);
            }
        }
    }

    public void onDarkChanged(Rect rect, float f, int i) {
        this.mNonAdaptedColor = DarkIconDispatcher.getTint(rect, this, i);
        if (!this.mUseWallpaperTextColor) {
            setTextColor(this.mNonAdaptedColor);
        }
    }

    public void onDensityOrFontScaleChanged() {
        FontSizeUtils.updateFontSize(this, R$dimen.status_bar_clock_size);
        setPaddingRelative(this.mContext.getResources().getDimensionPixelSize(R$dimen.status_bar_clock_starting_padding), 0, this.mContext.getResources().getDimensionPixelSize(R$dimen.status_bar_clock_end_padding), 0);
    }

    public void useWallpaperTextColor(boolean z) {
        if (z != this.mUseWallpaperTextColor) {
            this.mUseWallpaperTextColor = z;
            if (this.mUseWallpaperTextColor) {
                setTextColor(Utils.getColorAttr(this.mContext, R$attr.wallpaperTextColor));
            } else {
                setTextColor(this.mNonAdaptedColor);
            }
        }
    }

    private void updateShowSeconds() {
        if (showSeconds()) {
            runSecondTick();
        } else {
            updateClock();
        }
    }

    private void releaseReceiver() {
        if (this.mSecondsHandler != null) {
            try {
                this.mContext.unregisterReceiver(this.mScreenReceiver);
            } catch (IllegalArgumentException unused) {
            }
            this.mSecondsHandler.removeCallbacks(this.mSecondTick);
            this.mSecondsHandler = null;
            updateClock();
        }
    }

    /* access modifiers changed from: private */
    public boolean showSeconds() {
        return this.mShowSeconds && !((HighlightHintController) Dependency.get(HighlightHintController.class)).isHighLightHintShow();
    }

    private final CharSequence getSmallTime() {
        SimpleDateFormat simpleDateFormat;
        Context context = getContext();
        boolean is24HourFormat = DateFormat.is24HourFormat(context, this.mCurrentUserId);
        LocaleData localeData = LocaleData.get(context.getResources().getConfiguration().locale);
        String str = showSeconds() ? is24HourFormat ? localeData.timeFormat_Hms : localeData.timeFormat_hms : is24HourFormat ? localeData.timeFormat_Hm : localeData.timeFormat_hm;
        if (!str.equals(this.mClockFormatString)) {
            this.mContentDescriptionFormat = new SimpleDateFormat(str);
            if (this.mAmPmStyle != 0) {
                int i = 0;
                boolean z = false;
                while (true) {
                    if (i >= str.length()) {
                        i = -1;
                        break;
                    }
                    char charAt = str.charAt(i);
                    if (charAt == '\'') {
                        z = !z;
                    }
                    if (!z && charAt == 'a') {
                        break;
                    }
                    i++;
                }
                if (i >= 0) {
                    int i2 = i;
                    while (i2 > 0 && Character.isWhitespace(str.charAt(i2 - 1))) {
                        i2--;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(str.substring(0, i2));
                    sb.append(61184);
                    sb.append(str.substring(i2, i));
                    sb.append("a");
                    sb.append(61185);
                    sb.append(str.substring(i + 1));
                    str = sb.toString();
                }
            }
            simpleDateFormat = new SimpleDateFormat(str);
            this.mClockFormat = simpleDateFormat;
            this.mClockFormatString = str;
        } else {
            simpleDateFormat = this.mClockFormat;
        }
        String format = simpleDateFormat.format(this.mCalendar.getTime());
        if (this.mAmPmStyle != 0) {
            int indexOf = format.indexOf(61184);
            int indexOf2 = format.indexOf(61185);
            if (indexOf >= 0 && indexOf2 > indexOf) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(format);
                int i3 = this.mAmPmStyle;
                if (i3 == 2) {
                    spannableStringBuilder.delete(indexOf, indexOf2 + 1);
                } else {
                    if (i3 == 1) {
                        spannableStringBuilder.setSpan(new RelativeSizeSpan(0.7f), indexOf, indexOf2, 34);
                    }
                    spannableStringBuilder.delete(indexOf2, indexOf2 + 1);
                    spannableStringBuilder.delete(indexOf, indexOf + 1);
                }
                return spannableStringBuilder;
            }
        }
        return format;
    }

    public void dispatchDemoCommand(String str, Bundle bundle) {
        if (!this.mDemoMode && str.equals("enter")) {
            this.mDemoMode = true;
        } else if (this.mDemoMode && str.equals("exit")) {
            this.mDemoMode = false;
            updateClock();
        } else if (this.mDemoMode && str.equals("clock")) {
            String string = bundle.getString("millis");
            String string2 = bundle.getString("hhmm");
            if (string != null) {
                this.mCalendar.setTimeInMillis(Long.parseLong(string));
            } else if (string2 != null && string2.length() == 4) {
                int parseInt = Integer.parseInt(string2.substring(0, 2));
                int parseInt2 = Integer.parseInt(string2.substring(2));
                if (DateFormat.is24HourFormat(getContext(), this.mCurrentUserId)) {
                    this.mCalendar.set(11, parseInt);
                } else {
                    this.mCalendar.set(10, parseInt);
                }
                this.mCalendar.set(12, parseInt2);
            }
            setText(getSmallTime());
            setContentDescription(this.mContentDescriptionFormat.format(this.mCalendar.getTime()));
        }
    }

    /* access modifiers changed from: private */
    public void runSecondTick() {
        if (this.mSecondsHandler == null) {
            this.mSecondsHandler = new Handler();
        }
        if (getDisplay() != null && getDisplay().getState() == 2) {
            if (this.mSecondsHandler.hasCallbacks(this.mSecondTick)) {
                this.mSecondsHandler.removeCallbacks(this.mSecondTick);
            }
            this.mSecondsHandler.postAtTime(this.mSecondTick, ((SystemClock.uptimeMillis() / 1000) * 1000) + 1000);
        }
    }

    public void onHighlightHintStateChange() {
        Log.i(TAG, "onHighlightHintStateChange");
        updateShowSeconds();
    }
}

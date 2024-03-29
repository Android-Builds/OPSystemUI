package com.android.settingslib.notification;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.PhoneWindow;
import com.android.settingslib.R$id;
import com.android.settingslib.R$layout;
import com.android.settingslib.R$string;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;

public class EnableZenModeDialog {
    @VisibleForTesting
    protected static final int COUNTDOWN_ALARM_CONDITION_INDEX = 2;
    @VisibleForTesting
    protected static final int COUNTDOWN_CONDITION_INDEX = 1;
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("EnableZenModeDialog", 3);
    private static final int DEFAULT_BUCKET_INDEX;
    @VisibleForTesting
    protected static final int FOREVER_CONDITION_INDEX = 0;
    private static final int MAX_BUCKET_MINUTES;
    private static final int[] MINUTE_BUCKETS = ZenModeConfig.MINUTE_BUCKETS;
    private static final int MIN_BUCKET_MINUTES;
    private int MAX_MANUAL_DND_OPTIONS = 3;
    private AlarmManager mAlarmManager;
    private boolean mAttached;
    private int mBucketIndex = -1;
    @VisibleForTesting
    protected Context mContext;
    @VisibleForTesting
    protected Uri mForeverId;
    @VisibleForTesting
    protected LayoutInflater mLayoutInflater;
    @VisibleForTesting
    protected NotificationManager mNotificationManager;
    private int mUserId;
    @VisibleForTesting
    protected TextView mZenAlarmWarning;
    /* access modifiers changed from: private */
    public RadioGroup mZenRadioGroup;
    @VisibleForTesting
    protected LinearLayout mZenRadioGroupContent;

    @VisibleForTesting
    protected static class ConditionTag {
        public Condition condition;
        public TextView line1;
        public TextView line2;
        public View lines;

        /* renamed from: rb */
        public RadioButton f57rb;

        protected ConditionTag() {
        }
    }

    static {
        int[] iArr = MINUTE_BUCKETS;
        MIN_BUCKET_MINUTES = iArr[0];
        MAX_BUCKET_MINUTES = iArr[iArr.length - 1];
        DEFAULT_BUCKET_INDEX = Arrays.binarySearch(iArr, 60);
    }

    public EnableZenModeDialog(Context context) {
        this.mContext = context;
    }

    public Dialog createDialog() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mForeverId = Condition.newId(this.mContext).appendPath("forever").build();
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUserId = this.mContext.getUserId();
        this.mAttached = false;
        Builder positiveButton = new Builder(this.mContext).setTitle(R$string.zen_mode_settings_turn_on_dialog_title).setNegativeButton(R$string.cancel, null).setPositiveButton(R$string.zen_mode_enable_dialog_turn_on, new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                ConditionTag conditionTagAt = EnableZenModeDialog.this.getConditionTagAt(EnableZenModeDialog.this.mZenRadioGroup.getCheckedRadioButtonId());
                String str = "EnableZenModeDialog";
                if (EnableZenModeDialog.this.isForever(conditionTagAt.condition)) {
                    MetricsLogger.action(EnableZenModeDialog.this.mContext, 1259);
                } else if (EnableZenModeDialog.this.isAlarm(conditionTagAt.condition)) {
                    MetricsLogger.action(EnableZenModeDialog.this.mContext, 1261);
                } else if (EnableZenModeDialog.this.isCountdown(conditionTagAt.condition)) {
                    MetricsLogger.action(EnableZenModeDialog.this.mContext, 1260);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invalid manual condition: ");
                    sb.append(conditionTagAt.condition);
                    Slog.d(str, sb.toString());
                }
                EnableZenModeDialog enableZenModeDialog = EnableZenModeDialog.this;
                enableZenModeDialog.mNotificationManager.setZenMode(1, enableZenModeDialog.getRealConditionId(conditionTagAt.condition), str);
            }
        });
        View contentView = getContentView();
        bindConditions(forever());
        positiveButton.setView(contentView);
        return positiveButton.create();
    }

    private void hideAllConditions() {
        int childCount = this.mZenRadioGroupContent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mZenRadioGroupContent.getChildAt(i).setVisibility(8);
        }
        this.mZenAlarmWarning.setVisibility(8);
    }

    /* access modifiers changed from: protected */
    public View getContentView() {
        if (this.mLayoutInflater == null) {
            this.mLayoutInflater = new PhoneWindow(this.mContext).getLayoutInflater();
        }
        View inflate = this.mLayoutInflater.inflate(R$layout.zen_mode_turn_on_dialog_container, null);
        ScrollView scrollView = (ScrollView) inflate.findViewById(R$id.container);
        this.mZenRadioGroup = (RadioGroup) scrollView.findViewById(R$id.zen_radio_buttons);
        this.mZenRadioGroupContent = (LinearLayout) scrollView.findViewById(R$id.zen_radio_buttons_content);
        this.mZenAlarmWarning = (TextView) scrollView.findViewById(R$id.zen_alarm_warning);
        for (int i = 0; i < this.MAX_MANUAL_DND_OPTIONS; i++) {
            View inflate2 = this.mLayoutInflater.inflate(R$layout.zen_mode_radio_button, this.mZenRadioGroup, false);
            this.mZenRadioGroup.addView(inflate2);
            inflate2.setId(i);
            View inflate3 = this.mLayoutInflater.inflate(R$layout.zen_mode_condition, this.mZenRadioGroupContent, false);
            inflate3.setId(this.MAX_MANUAL_DND_OPTIONS + i);
            this.mZenRadioGroupContent.addView(inflate3);
        }
        hideAllConditions();
        return inflate;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void bind(Condition condition, View view, int i) {
        ConditionTag conditionTag;
        if (condition != null) {
            boolean z = true;
            boolean z2 = condition.state == 1;
            if (view.getTag() != null) {
                conditionTag = (ConditionTag) view.getTag();
            } else {
                conditionTag = new ConditionTag();
            }
            final ConditionTag conditionTag2 = conditionTag;
            view.setTag(conditionTag2);
            if (conditionTag2.f57rb != null) {
                z = false;
            }
            if (conditionTag2.f57rb == null) {
                conditionTag2.f57rb = (RadioButton) this.mZenRadioGroup.getChildAt(i);
            }
            conditionTag2.condition = condition;
            final Uri conditionId = getConditionId(conditionTag2.condition);
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("bind i=");
                sb.append(this.mZenRadioGroupContent.indexOfChild(view));
                sb.append(" first=");
                sb.append(z);
                sb.append(" condition=");
                sb.append(conditionId);
                Log.d("EnableZenModeDialog", sb.toString());
            }
            conditionTag2.f57rb.setEnabled(z2);
            conditionTag2.f57rb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                    if (z) {
                        conditionTag2.f57rb.setChecked(true);
                        if (EnableZenModeDialog.DEBUG) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("onCheckedChanged ");
                            sb.append(conditionId);
                            Log.d("EnableZenModeDialog", sb.toString());
                        }
                        MetricsLogger.action(EnableZenModeDialog.this.mContext, 164);
                        EnableZenModeDialog.this.updateAlarmWarningText(conditionTag2.condition);
                    }
                }
            });
            updateUi(conditionTag2, view, condition, z2, i, conditionId);
            view.setVisibility(0);
            return;
        }
        throw new IllegalArgumentException("condition must not be null");
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public ConditionTag getConditionTagAt(int i) {
        return (ConditionTag) this.mZenRadioGroupContent.getChildAt(i).getTag();
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void bindConditions(Condition condition) {
        bind(forever(), this.mZenRadioGroupContent.getChildAt(0), 0);
        if (condition == null) {
            bindGenericCountdown();
            bindNextAlarm(getTimeUntilNextAlarmCondition());
        } else if (isForever(condition)) {
            getConditionTagAt(0).f57rb.setChecked(true);
            bindGenericCountdown();
            bindNextAlarm(getTimeUntilNextAlarmCondition());
        } else if (isAlarm(condition)) {
            bindGenericCountdown();
            bindNextAlarm(condition);
            getConditionTagAt(2).f57rb.setChecked(true);
        } else if (isCountdown(condition)) {
            bindNextAlarm(getTimeUntilNextAlarmCondition());
            bind(condition, this.mZenRadioGroupContent.getChildAt(1), 1);
            getConditionTagAt(1).f57rb.setChecked(true);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid manual condition: ");
            sb.append(condition);
            Slog.d("EnableZenModeDialog", sb.toString());
        }
    }

    public static Uri getConditionId(Condition condition) {
        if (condition != null) {
            return condition.id;
        }
        return null;
    }

    public Condition forever() {
        Condition condition = new Condition(Condition.newId(this.mContext).appendPath("forever").build(), foreverSummary(this.mContext), "", "", 0, 1, 0);
        return condition;
    }

    public long getNextAlarm() {
        AlarmClockInfo nextAlarmClock = this.mAlarmManager.getNextAlarmClock(this.mUserId);
        if (nextAlarmClock != null) {
            return nextAlarmClock.getTriggerTime();
        }
        return 0;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public boolean isAlarm(Condition condition) {
        return condition != null && ZenModeConfig.isValidCountdownToAlarmConditionId(condition.id);
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public boolean isCountdown(Condition condition) {
        return condition != null && ZenModeConfig.isValidCountdownConditionId(condition.id);
    }

    /* access modifiers changed from: private */
    public boolean isForever(Condition condition) {
        return condition != null && this.mForeverId.equals(condition.id);
    }

    /* access modifiers changed from: private */
    public Uri getRealConditionId(Condition condition) {
        if (isForever(condition)) {
            return null;
        }
        return getConditionId(condition);
    }

    private String foreverSummary(Context context) {
        return context.getString(17041352);
    }

    private static void setToMidnight(Calendar calendar) {
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public Condition getTimeUntilNextAlarmCondition() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        setToMidnight(gregorianCalendar);
        gregorianCalendar.add(5, 6);
        long nextAlarm = getNextAlarm();
        if (nextAlarm > 0) {
            GregorianCalendar gregorianCalendar2 = new GregorianCalendar();
            gregorianCalendar2.setTimeInMillis(nextAlarm);
            setToMidnight(gregorianCalendar2);
            if (gregorianCalendar.compareTo(gregorianCalendar2) >= 0) {
                return ZenModeConfig.toNextAlarmCondition(this.mContext, nextAlarm, ActivityManager.getCurrentUser());
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void bindGenericCountdown() {
        this.mBucketIndex = DEFAULT_BUCKET_INDEX;
        Condition timeCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
        if (!this.mAttached || getConditionTagAt(1).condition == null) {
            bind(timeCondition, this.mZenRadioGroupContent.getChildAt(1), 1);
        }
    }

    private void updateUi(final ConditionTag conditionTag, final View view, Condition condition, boolean z, final int i, Uri uri) {
        String str;
        boolean z2 = true;
        if (conditionTag.lines == null) {
            conditionTag.lines = view.findViewById(16908290);
            conditionTag.lines.setAccessibilityLiveRegion(1);
        }
        if (conditionTag.line1 == null) {
            conditionTag.line1 = (TextView) view.findViewById(16908308);
        }
        if (conditionTag.line2 == null) {
            conditionTag.line2 = (TextView) view.findViewById(16908309);
        }
        if (!TextUtils.isEmpty(condition.line1)) {
            str = condition.line1;
        } else {
            str = condition.summary;
        }
        String str2 = condition.line2;
        conditionTag.line1.setText(str);
        boolean z3 = false;
        if (TextUtils.isEmpty(str2)) {
            conditionTag.line2.setVisibility(8);
        } else {
            conditionTag.line2.setVisibility(0);
            conditionTag.line2.setText(str2);
        }
        conditionTag.lines.setEnabled(z);
        conditionTag.lines.setAlpha(z ? 1.0f : 0.4f);
        conditionTag.lines.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                conditionTag.f57rb.setChecked(true);
            }
        });
        ImageView imageView = (ImageView) view.findViewById(16908313);
        imageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EnableZenModeDialog.this.onClickTimeButton(view, conditionTag, false, i);
            }
        });
        ImageView imageView2 = (ImageView) view.findViewById(16908314);
        imageView2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                EnableZenModeDialog.this.onClickTimeButton(view, conditionTag, true, i);
            }
        });
        long tryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(uri);
        if (i != 1 || tryParseCountdownConditionId <= 0) {
            imageView.setVisibility(8);
            imageView2.setVisibility(8);
            return;
        }
        imageView.setVisibility(0);
        imageView2.setVisibility(0);
        int i2 = this.mBucketIndex;
        if (i2 > -1) {
            imageView.setEnabled(i2 > 0);
            if (this.mBucketIndex >= MINUTE_BUCKETS.length - 1) {
                z2 = false;
            }
            imageView2.setEnabled(z2);
        } else {
            if (tryParseCountdownConditionId - System.currentTimeMillis() > ((long) (MIN_BUCKET_MINUTES * 60000))) {
                z3 = true;
            }
            imageView.setEnabled(z3);
            imageView2.setEnabled(!Objects.equals(condition.summary, ZenModeConfig.toTimeCondition(this.mContext, MAX_BUCKET_MINUTES, ActivityManager.getCurrentUser()).summary));
        }
        float f = 0.5f;
        imageView.setAlpha(imageView.isEnabled() ? 1.0f : 0.5f);
        if (imageView2.isEnabled()) {
            f = 1.0f;
        }
        imageView2.setAlpha(f);
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void bindNextAlarm(Condition condition) {
        View childAt = this.mZenRadioGroupContent.getChildAt(2);
        ConditionTag conditionTag = (ConditionTag) childAt.getTag();
        if (condition != null && (!this.mAttached || conditionTag == null || conditionTag.condition == null)) {
            bind(condition, childAt, 2);
        }
        ConditionTag conditionTag2 = (ConditionTag) childAt.getTag();
        boolean z = (conditionTag2 == null || conditionTag2.condition == null) ? false : true;
        int i = 8;
        this.mZenRadioGroup.getChildAt(2).setVisibility(z ? 0 : 8);
        if (z) {
            i = 0;
        }
        childAt.setVisibility(i);
    }

    /* access modifiers changed from: private */
    public void onClickTimeButton(View view, ConditionTag conditionTag, boolean z, int i) {
        Condition condition;
        int i2;
        int i3;
        long j;
        ConditionTag conditionTag2 = conditionTag;
        boolean z2 = z;
        MetricsLogger.action(this.mContext, 163, z2);
        int length = MINUTE_BUCKETS.length;
        int i4 = this.mBucketIndex;
        int i5 = 0;
        int i6 = -1;
        if (i4 == -1) {
            long tryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(getConditionId(conditionTag2.condition));
            long currentTimeMillis = System.currentTimeMillis();
            while (true) {
                if (i5 >= length) {
                    condition = null;
                    break;
                }
                i2 = z2 ? i5 : (length - 1) - i5;
                i3 = MINUTE_BUCKETS[i2];
                j = currentTimeMillis + ((long) (60000 * i3));
                if ((!z2 || j <= tryParseCountdownConditionId) && (z2 || j >= tryParseCountdownConditionId)) {
                    i5++;
                }
            }
            this.mBucketIndex = i2;
            condition = ZenModeConfig.toTimeCondition(this.mContext, j, i3, ActivityManager.getCurrentUser(), false);
            if (condition == null) {
                this.mBucketIndex = DEFAULT_BUCKET_INDEX;
                condition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
            }
        } else {
            int i7 = length - 1;
            if (z2) {
                i6 = 1;
            }
            this.mBucketIndex = Math.max(0, Math.min(i7, i4 + i6));
            condition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
        }
        bind(condition, view, i);
        updateAlarmWarningText(conditionTag2.condition);
        conditionTag2.f57rb.setChecked(true);
    }

    /* access modifiers changed from: private */
    public void updateAlarmWarningText(Condition condition) {
        String computeAlarmWarningText = computeAlarmWarningText(condition);
        this.mZenAlarmWarning.setText(computeAlarmWarningText);
        this.mZenAlarmWarning.setVisibility(computeAlarmWarningText == null ? 8 : 0);
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public String computeAlarmWarningText(Condition condition) {
        int i;
        if ((this.mNotificationManager.getNotificationPolicy().priorityCategories & 32) != 0) {
            return null;
        }
        long currentTimeMillis = System.currentTimeMillis();
        long nextAlarm = getNextAlarm();
        if (nextAlarm < currentTimeMillis) {
            return null;
        }
        if (condition == null || isForever(condition)) {
            i = R$string.zen_alarm_warning_indef;
        } else {
            long tryParseCountdownConditionId = ZenModeConfig.tryParseCountdownConditionId(condition.id);
            i = (tryParseCountdownConditionId <= currentTimeMillis || nextAlarm >= tryParseCountdownConditionId) ? 0 : R$string.zen_alarm_warning;
        }
        if (i == 0) {
            return null;
        }
        return this.mContext.getResources().getString(i, new Object[]{getTime(nextAlarm, currentTimeMillis)});
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public String getTime(long j, long j2) {
        boolean z = j - j2 < 86400000;
        boolean is24HourFormat = DateFormat.is24HourFormat(this.mContext, ActivityManager.getCurrentUser());
        String str = z ? is24HourFormat ? "Hm" : "hma" : is24HourFormat ? "EEEHm" : "EEEhma";
        return this.mContext.getResources().getString(z ? R$string.alarm_template : R$string.alarm_template_far, new Object[]{DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), str), j)});
    }
}

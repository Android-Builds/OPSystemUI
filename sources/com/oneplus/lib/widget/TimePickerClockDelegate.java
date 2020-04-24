package com.oneplus.lib.widget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.TtsSpan.VerbatimBuilder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.oneplus.commonctrl.R$drawable;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$integer;
import com.oneplus.commonctrl.R$layout;
import com.oneplus.commonctrl.R$string;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.util.AnimatorUtils;
import com.oneplus.lib.util.SystemUtils;
import com.oneplus.lib.widget.NumericTextView.OnValueChangedListener;
import com.oneplus.lib.widget.TimePicker.OnTimeChangedListener;
import java.util.Calendar;

class TimePickerClockDelegate extends AbstractTimePickerDelegate {
    private static final int[] ATTRS_DISABLED_ALPHA = {16842803};
    private static final int[] ATTRS_TEXT_COLOR = {16842904};
    /* access modifiers changed from: private */
    public boolean mAllowAutoAdvance;
    private final RadioButton mAmLabel;
    private final View mAmPmLayout;
    private final OnClickListener mClickListener = new OnClickListener() {
        public void onClick(View view) {
            int id = view.getId();
            if (id == R$id.am_label) {
                TimePickerClockDelegate.this.setAmOrPm(0);
            } else if (id == R$id.pm_label) {
                TimePickerClockDelegate.this.setAmOrPm(1);
            } else if (id == R$id.hours) {
                TimePickerClockDelegate.this.setCurrentItemShowing(0, true, true);
            } else if (id == R$id.minutes) {
                TimePickerClockDelegate.this.setCurrentItemShowing(1, true, true);
            } else {
                return;
            }
            TimePickerClockDelegate.this.tryVibrate();
        }
    };
    /* access modifiers changed from: private */
    public final Runnable mCommitHour = new Runnable() {
        public void run() {
            TimePickerClockDelegate timePickerClockDelegate = TimePickerClockDelegate.this;
            timePickerClockDelegate.setHour(timePickerClockDelegate.mHourView.getValue());
        }
    };
    /* access modifiers changed from: private */
    public final Runnable mCommitMinute = new Runnable() {
        public void run() {
            TimePickerClockDelegate timePickerClockDelegate = TimePickerClockDelegate.this;
            timePickerClockDelegate.setMinute(timePickerClockDelegate.mMinuteView.getValue());
        }
    };
    private int mCurrentHour;
    private int mCurrentMinute;
    private final OnValueChangedListener mDigitEnteredListener = new OnValueChangedListener() {
        public void onValueChanged(NumericTextView numericTextView, int i, boolean z, boolean z2) {
            Runnable runnable;
            View view = null;
            if (numericTextView == TimePickerClockDelegate.this.mHourView) {
                runnable = TimePickerClockDelegate.this.mCommitHour;
                if (numericTextView.isFocused()) {
                    view = TimePickerClockDelegate.this.mMinuteView;
                }
            } else if (numericTextView == TimePickerClockDelegate.this.mMinuteView) {
                runnable = TimePickerClockDelegate.this.mCommitMinute;
            } else {
                return;
            }
            numericTextView.removeCallbacks(runnable);
            if (!z) {
                return;
            }
            if (z2) {
                runnable.run();
                if (view != null) {
                    view.requestFocus();
                    return;
                }
                return;
            }
            numericTextView.postDelayed(runnable, 2000);
        }
    };
    private int mDuration;
    private final OnFocusChangeListener mFocusListener = new OnFocusChangeListener() {
        public void onFocusChange(View view, boolean z) {
            if (z) {
                int id = view.getId();
                if (id == R$id.am_label) {
                    TimePickerClockDelegate.this.setAmOrPm(0);
                } else if (id == R$id.pm_label) {
                    TimePickerClockDelegate.this.setAmOrPm(1);
                } else if (id == R$id.hours) {
                    TimePickerClockDelegate.this.setCurrentItemShowing(0, true, true);
                } else if (id == R$id.minutes) {
                    TimePickerClockDelegate.this.setCurrentItemShowing(1, true, true);
                } else {
                    return;
                }
                TimePickerClockDelegate.this.tryVibrate();
            }
        }
    };
    private int mHeaderOffset;
    private int mHeaderPositionY;
    private boolean mHourFormatShowLeadingZero;
    private boolean mHourFormatStartsAtZero;
    /* access modifiers changed from: private */
    public final NumericTextView mHourView;
    private final View mImageSeparatorView;
    private int mInputBlockPositionY;
    private boolean mIs24Hour;
    private boolean mIsAmPmAtStart;
    private boolean mIsEnabled = true;
    /* access modifiers changed from: private */
    public boolean mIsToggleTimeMode;
    private boolean mLastAnnouncedIsHour;
    private CharSequence mLastAnnouncedText;
    /* access modifiers changed from: private */
    public final NumericTextView mMinuteView;
    private final OnValueSelectedListener mOnValueSelectedListener = new OnValueSelectedListener() {
        public void onValueSelected(int i, int i2, boolean z) {
            boolean z2 = false;
            if (i == 0) {
                boolean z3 = TimePickerClockDelegate.this.getHour() != i2;
                boolean z4 = TimePickerClockDelegate.this.mAllowAutoAdvance && z;
                TimePickerClockDelegate.this.setHourInternal(i2, 1, !z4);
                if (z4) {
                    TimePickerClockDelegate.this.setCurrentItemShowing(1, true, false);
                    int access$1100 = TimePickerClockDelegate.this.getLocalizedHour(i2);
                    TimePicker timePicker = TimePickerClockDelegate.this.mDelegator;
                    StringBuilder sb = new StringBuilder();
                    sb.append(access$1100);
                    sb.append(". ");
                    sb.append(TimePickerClockDelegate.this.mSelectMinutes);
                    timePicker.announceForAccessibility(sb.toString());
                }
                z2 = z3;
            } else if (i == 1) {
                if (TimePickerClockDelegate.this.getMinute() != i2) {
                    z2 = true;
                }
                TimePickerClockDelegate.this.setMinuteInternal(i2, 1);
            }
            TimePickerClockDelegate timePickerClockDelegate = TimePickerClockDelegate.this;
            OnTimeChangedListener onTimeChangedListener = timePickerClockDelegate.mOnTimeChangedListener;
            if (onTimeChangedListener != null && z2) {
                onTimeChangedListener.onTimeChanged(timePickerClockDelegate.mDelegator, timePickerClockDelegate.getHour(), TimePickerClockDelegate.this.getMinute());
            }
        }
    };
    private final OnValueTypedListener mOnValueTypedListener = new OnValueTypedListener() {
        public void onValueChanged(int i, int i2) {
            if (i == 0) {
                TimePickerClockDelegate.this.setHourInternal(i2, 2, false);
            } else if (i == 1) {
                TimePickerClockDelegate.this.setMinuteInternal(i2, 2);
            } else if (i == 2) {
                TimePickerClockDelegate.this.setAmOrPm(i2);
            }
        }
    };
    private final RadioButton mPmLabel;
    private boolean mRadialPickerModeEnabled = true;
    /* access modifiers changed from: private */
    public final LinearLayout mRadialTimePickerHeader;
    private final ImageButton mRadialTimePickerModeButton;
    /* access modifiers changed from: private */
    public final RadialTimePickerView mRadialTimePickerView;
    private float mRadialTimeViewAlpha;
    private float mRadialTimeViewScale;
    private final String mSelectHours;
    /* access modifiers changed from: private */
    public final String mSelectMinutes;
    private final TextView mSeparatorView;
    private final Calendar mTempCalendar;
    /* access modifiers changed from: private */
    public final View mTextInputPickerHeader;
    /* access modifiers changed from: private */
    public final TextInputTimePickerView mTextInputPickerView;

    private static class ClickActionDelegate extends AccessibilityDelegate {
        private final AccessibilityAction mClickAction;

        public ClickActionDelegate(Context context, int i) {
            this.mClickAction = new AccessibilityAction(16, context.getString(i));
        }

        public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
            accessibilityNodeInfo.addAction(this.mClickAction);
        }
    }

    private static class NearestTouchDelegate implements OnTouchListener {
        private View mInitialTouchTarget;

        private NearestTouchDelegate() {
        }

        public boolean onTouch(View view, MotionEvent motionEvent) {
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 0) {
                if (view instanceof ViewGroup) {
                    this.mInitialTouchTarget = findNearestChild((ViewGroup) view, (int) motionEvent.getX(), (int) motionEvent.getY());
                } else {
                    this.mInitialTouchTarget = null;
                }
            }
            View view2 = this.mInitialTouchTarget;
            if (view2 == null) {
                return false;
            }
            float scrollX = (float) (view.getScrollX() - view2.getLeft());
            float scrollY = (float) (view.getScrollY() - view2.getTop());
            motionEvent.offsetLocation(scrollX, scrollY);
            boolean dispatchTouchEvent = view2.dispatchTouchEvent(motionEvent);
            motionEvent.offsetLocation(-scrollX, -scrollY);
            if (actionMasked == 1 || actionMasked == 3) {
                this.mInitialTouchTarget = null;
            }
            return dispatchTouchEvent;
        }

        private View findNearestChild(ViewGroup viewGroup, int i, int i2) {
            int childCount = viewGroup.getChildCount();
            View view = null;
            int i3 = Integer.MAX_VALUE;
            for (int i4 = 0; i4 < childCount; i4++) {
                View childAt = viewGroup.getChildAt(i4);
                int left = i - (childAt.getLeft() + (childAt.getWidth() / 2));
                int top = i2 - (childAt.getTop() + (childAt.getHeight() / 2));
                int i5 = (left * left) + (top * top);
                if (i3 > i5) {
                    view = childAt;
                    i3 = i5;
                }
            }
            return view;
        }
    }

    public int getBaseline() {
        return -1;
    }

    public TimePickerClockDelegate(TimePicker timePicker, Context context, AttributeSet attributeSet, int i, int i2) {
        super(timePicker, context);
        TypedArray obtainStyledAttributes = this.mContext.obtainStyledAttributes(attributeSet, R$styleable.TimePicker, i, i2);
        LayoutInflater layoutInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        Resources resources = this.mContext.getResources();
        this.mSelectHours = resources.getString(R$string.select_hours);
        this.mSelectMinutes = resources.getString(R$string.select_minutes);
        View inflate = layoutInflater.inflate(obtainStyledAttributes.getResourceId(R$styleable.TimePicker_internalLayout, R$layout.op_time_picker_material), timePicker);
        inflate.setSaveFromParentEnabled(false);
        this.mRadialTimePickerHeader = (LinearLayout) inflate.findViewById(R$id.time_header);
        this.mRadialTimePickerHeader.setOnTouchListener(new NearestTouchDelegate());
        this.mHourView = (NumericTextView) inflate.findViewById(R$id.hours);
        this.mHourView.setOnClickListener(this.mClickListener);
        this.mHourView.setOnFocusChangeListener(this.mFocusListener);
        this.mHourView.setOnDigitEnteredListener(this.mDigitEnteredListener);
        this.mHourView.setAccessibilityDelegate(new ClickActionDelegate(context, R$string.select_hours));
        this.mSeparatorView = (TextView) inflate.findViewById(R$id.separator);
        this.mImageSeparatorView = inflate.findViewById(R$id.separator_shape);
        this.mMinuteView = (NumericTextView) inflate.findViewById(R$id.minutes);
        this.mMinuteView.setOnClickListener(this.mClickListener);
        this.mMinuteView.setOnFocusChangeListener(this.mFocusListener);
        this.mMinuteView.setOnDigitEnteredListener(this.mDigitEnteredListener);
        this.mMinuteView.setAccessibilityDelegate(new ClickActionDelegate(context, R$string.select_minutes));
        this.mMinuteView.setRange(0, 59);
        inflate.findViewById(R$id.separator).setActivated(true);
        ((TextView) inflate.findViewById(R$id.separator)).getPaint().setFakeBoldText(true);
        this.mAmPmLayout = inflate.findViewById(R$id.ampm_layout);
        this.mAmPmLayout.setOnTouchListener(new NearestTouchDelegate());
        String[] amPmStrings = TimePicker.getAmPmStrings(context);
        this.mAmLabel = (RadioButton) this.mAmPmLayout.findViewById(R$id.am_label);
        this.mAmLabel.setText(obtainVerbatim(amPmStrings[0]));
        this.mAmLabel.setOnClickListener(this.mClickListener);
        ensureMinimumTextWidth(this.mAmLabel);
        this.mPmLabel = (RadioButton) this.mAmPmLayout.findViewById(R$id.pm_label);
        this.mPmLabel.setText(obtainVerbatim(amPmStrings[1]));
        this.mPmLabel.setOnClickListener(this.mClickListener);
        ensureMinimumTextWidth(this.mPmLabel);
        int resourceId = obtainStyledAttributes.getResourceId(R$styleable.TimePicker_android_headerTimeTextAppearance, 0);
        if (resourceId != 0) {
            TypedArray obtainStyledAttributes2 = this.mContext.obtainStyledAttributes(null, ATTRS_TEXT_COLOR, 0, resourceId);
            obtainStyledAttributes2.getColorStateList(0);
            obtainStyledAttributes2.recycle();
        }
        ColorStateList colorStateList = obtainStyledAttributes.getColorStateList(R$styleable.TimePicker_headerTextColor);
        this.mTextInputPickerHeader = inflate.findViewById(R$id.input_header);
        if (colorStateList != null) {
            this.mHourView.setTextColor(colorStateList);
            this.mSeparatorView.setTextColor(colorStateList);
            this.mMinuteView.setTextColor(colorStateList);
            this.mAmLabel.setTextColor(colorStateList);
            this.mPmLabel.setTextColor(colorStateList);
        }
        obtainStyledAttributes.recycle();
        this.mDuration = context.getResources().getInteger(R$integer.oneplus_contorl_time_part6);
        this.mRadialTimeViewScale = 0.0f;
        this.mRadialTimeViewAlpha = 0.0f;
        this.mRadialTimePickerView = (RadialTimePickerView) inflate.findViewById(R$id.radial_picker);
        this.mRadialTimePickerView.applyAttributes(attributeSet, i, i2);
        this.mRadialTimePickerView.setOnValueSelectedListener(this.mOnValueSelectedListener);
        this.mTextInputPickerView = (TextInputTimePickerView) inflate.findViewById(R$id.input_mode);
        this.mTextInputPickerView.setListener(this.mOnValueTypedListener);
        this.mRadialTimePickerModeButton = (ImageButton) inflate.findViewById(R$id.toggle_mode);
        this.mRadialTimePickerModeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                TimePickerClockDelegate.this.toggleRadialPickerMode();
            }
        });
        this.mAllowAutoAdvance = true;
        updateHourFormat();
        this.mTempCalendar = Calendar.getInstance(this.mLocale);
        initialize(this.mTempCalendar.get(11), this.mTempCalendar.get(12), this.mIs24Hour, 0);
    }

    /* access modifiers changed from: private */
    public void toggleRadialPickerMode() {
        if (!this.mIsToggleTimeMode) {
            if (this.mHeaderPositionY == 0) {
                int[] iArr = new int[2];
                this.mRadialTimePickerHeader.getLocationOnScreen(iArr);
                int y = (int) this.mRadialTimePickerHeader.findViewById(R$id.hours).getY();
                this.mHeaderPositionY = iArr[1];
                View inputBlock = this.mTextInputPickerView.getInputBlock();
                this.mHeaderOffset = Math.abs(((int) this.mTextInputPickerView.getInputBlock().findViewById(R$id.input_hour).getY()) - y);
                if (this.mHeaderOffset == 0) {
                    this.mHeaderOffset = Math.round(this.mContext.getResources().getDisplayMetrics().density * 4.0f);
                }
                inputBlock.getLocationOnScreen(iArr);
                this.mInputBlockPositionY = iArr[1];
            }
            if (this.mRadialPickerModeEnabled) {
                animationInInputTimeField();
                this.mRadialTimePickerModeButton.setImageResource(R$drawable.op_btn_clock_material);
                this.mRadialPickerModeEnabled = false;
            } else {
                animationOutInputTimeField();
                this.mRadialTimePickerModeButton.setImageResource(R$drawable.op_btn_keyboard_key_material);
                this.mRadialPickerModeEnabled = true;
            }
        }
    }

    private void animationInInputTimeField() {
        this.mTextInputPickerHeader.setVisibility(4);
        this.mTextInputPickerView.setVisibility(4);
        this.mTextInputPickerHeader.setVisibility(0);
        this.mTextInputPickerHeader.animate().alpha(1.0f).setDuration((long) this.mDuration).start();
        this.mRadialTimePickerView.animate().scaleX(this.mRadialTimeViewScale).scaleY(this.mRadialTimeViewScale).alpha(this.mRadialTimeViewAlpha).setDuration((long) this.mDuration).setInterpolator(AnimatorUtils.FastOutSlowInInterpolator).start();
        this.mRadialTimePickerHeader.setTranslationY(0.0f);
        this.mRadialTimePickerHeader.animate().setInterpolator(AnimatorUtils.FastOutSlowInInterpolator).translationY((float) ((this.mInputBlockPositionY - this.mHeaderPositionY) + this.mRadialTimePickerHeader.getPaddingTop() + this.mHeaderOffset)).setDuration((long) this.mDuration).setListener(new AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
                TimePickerClockDelegate.this.mTextInputPickerView.showInputBlock(true);
                TimePickerClockDelegate.this.mTextInputPickerView.showLabels(true);
                TimePickerClockDelegate.this.mIsToggleTimeMode = true;
            }

            public void onAnimationEnd(Animator animator) {
                TimePickerClockDelegate.this.mRadialTimePickerView.setVisibility(4);
                TimePickerClockDelegate.this.mRadialTimePickerHeader.setVisibility(4);
                TimePickerClockDelegate.this.mTextInputPickerView.setVisibility(0);
                TimePickerClockDelegate.this.mIsToggleTimeMode = false;
            }
        }).start();
    }

    private void animationOutInputTimeField() {
        this.mTextInputPickerView.getInputBlock();
        this.mRadialTimePickerView.setVisibility(0);
        this.mRadialTimePickerHeader.setVisibility(0);
        this.mRadialTimePickerView.setAlpha(0.0f);
        this.mTextInputPickerHeader.animate().alpha(0.0f).setDuration((long) this.mDuration).start();
        this.mTextInputPickerView.showLabels(false);
        this.mRadialTimePickerView.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setInterpolator(AnimatorUtils.FastOutSlowInInterpolator).setDuration((long) this.mDuration).start();
        this.mRadialTimePickerHeader.animate().translationY(0.0f).setInterpolator(AnimatorUtils.FastOutSlowInInterpolator).setDuration((long) this.mDuration).setListener(new AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
                TimePickerClockDelegate.this.mTextInputPickerView.showInputBlock(false);
                TimePickerClockDelegate.this.mIsToggleTimeMode = true;
            }

            public void onAnimationEnd(Animator animator) {
                TimePickerClockDelegate.this.mRadialTimePickerView.setVisibility(0);
                TimePickerClockDelegate.this.mRadialTimePickerHeader.setVisibility(0);
                TimePickerClockDelegate.this.mTextInputPickerHeader.setVisibility(4);
                TimePickerClockDelegate.this.mTextInputPickerView.setVisibility(4);
                TimePickerClockDelegate.this.updateTextInputPicker();
                TimePickerClockDelegate.this.mIsToggleTimeMode = false;
            }
        }).start();
    }

    private static void ensureMinimumTextWidth(TextView textView) {
        textView.measure(0, 0);
        int measuredWidth = textView.getMeasuredWidth();
        textView.setMinWidth(measuredWidth);
        textView.setMinimumWidth(measuredWidth);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0031, code lost:
        if (r3 >= r1) goto L_0x003b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0037, code lost:
        if (r7 != r0.charAt(r3)) goto L_0x003b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0039, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x003b, code lost:
        r0 = false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateHourFormat() {
        /*
            r9 = this;
            java.util.Locale r0 = r9.mLocale
            boolean r1 = r9.mIs24Hour
            if (r1 == 0) goto L_0x0009
            java.lang.String r1 = "Hm"
            goto L_0x000b
        L_0x0009:
            java.lang.String r1 = "hm"
        L_0x000b:
            java.lang.String r0 = android.text.format.DateFormat.getBestDateTimePattern(r0, r1)
            int r1 = r0.length()
            r2 = 0
            r3 = r2
        L_0x0015:
            r4 = 72
            r5 = 75
            r6 = 1
            if (r3 >= r1) goto L_0x003d
            char r7 = r0.charAt(r3)
            if (r7 == r4) goto L_0x0030
            r8 = 104(0x68, float:1.46E-43)
            if (r7 == r8) goto L_0x0030
            if (r7 == r5) goto L_0x0030
            r8 = 107(0x6b, float:1.5E-43)
            if (r7 != r8) goto L_0x002d
            goto L_0x0030
        L_0x002d:
            int r3 = r3 + 1
            goto L_0x0015
        L_0x0030:
            int r3 = r3 + r6
            if (r3 >= r1) goto L_0x003b
            char r0 = r0.charAt(r3)
            if (r7 != r0) goto L_0x003b
            r0 = r6
            goto L_0x003f
        L_0x003b:
            r0 = r2
            goto L_0x003f
        L_0x003d:
            r0 = r2
            r7 = r0
        L_0x003f:
            r9.mHourFormatShowLeadingZero = r0
            if (r7 == r5) goto L_0x0045
            if (r7 != r4) goto L_0x0046
        L_0x0045:
            r2 = r6
        L_0x0046:
            r9.mHourFormatStartsAtZero = r2
            boolean r0 = r9.mHourFormatStartsAtZero
            r0 = r0 ^ r6
            boolean r1 = r9.mIs24Hour
            if (r1 == 0) goto L_0x0052
            r1 = 23
            goto L_0x0054
        L_0x0052:
            r1 = 11
        L_0x0054:
            int r1 = r1 + r0
            com.oneplus.lib.widget.NumericTextView r2 = r9.mHourView
            r2.setRange(r0, r1)
            com.oneplus.lib.widget.NumericTextView r0 = r9.mHourView
            boolean r1 = r9.mHourFormatShowLeadingZero
            r0.setShowLeadingZeroes(r1)
            int r0 = android.os.Build.VERSION.SDK_INT     // Catch:{ Exception -> 0x006f }
            r1 = 24
            if (r0 < r1) goto L_0x0073
            com.oneplus.lib.widget.TextInputTimePickerView r0 = r9.mTextInputPickerView     // Catch:{ Exception -> 0x006f }
            java.util.Locale r9 = r9.mLocale     // Catch:{ Exception -> 0x006f }
            com.oneplus.lib.widget.TimePickerCompat24.setHourFormat(r0, r9)     // Catch:{ Exception -> 0x006f }
            goto L_0x0073
        L_0x006f:
            r9 = move-exception
            r9.printStackTrace()
        L_0x0073:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.TimePickerClockDelegate.updateHourFormat():void");
    }

    static final CharSequence obtainVerbatim(String str) {
        return new SpannableStringBuilder().append(str, new VerbatimBuilder(str).build(), 0);
    }

    private void initialize(int i, int i2, boolean z, int i3) {
        this.mCurrentHour = i;
        this.mCurrentMinute = i2;
        this.mIs24Hour = z;
        updateUI(i3);
    }

    private void updateUI(int i) {
        updateHeaderAmPm();
        updateHeaderHour(this.mCurrentHour, false);
        updateHeaderSeparator();
        updateHeaderMinute(this.mCurrentMinute, false);
        updateRadialPicker(i);
        updateTextInputPicker();
        this.mDelegator.invalidate();
    }

    /* access modifiers changed from: private */
    public void updateTextInputPicker() {
        this.mTextInputPickerView.updateTextInputValues(getLocalizedHour(this.mCurrentHour), this.mCurrentMinute, this.mCurrentHour < 12 ? 0 : 1, this.mIs24Hour, this.mHourFormatStartsAtZero);
    }

    private void updateRadialPicker(int i) {
        this.mRadialTimePickerView.initialize(this.mCurrentHour, this.mCurrentMinute, this.mIs24Hour);
        setCurrentItemShowing(i, false, true);
    }

    private void updateHeaderAmPm() {
        if (this.mIs24Hour) {
            this.mAmPmLayout.setVisibility(8);
            return;
        }
        boolean startsWith = DateFormat.getBestDateTimePattern(this.mLocale, "hm").startsWith("a");
        setAmPmAtStart(startsWith);
        setInputAmPmAtStart(startsWith);
        updateAmPmLabelStates(this.mCurrentHour < 12 ? 0 : 1);
    }

    private void setInputAmPmAtStart(boolean z) {
        this.mTextInputPickerView.setAmPmAtStart(z);
    }

    private void setAmPmAtStart(boolean z) {
        if (this.mIsAmPmAtStart != z) {
            this.mIsAmPmAtStart = z;
            if (z) {
                this.mRadialTimePickerHeader.removeView(this.mAmPmLayout);
                this.mRadialTimePickerHeader.addView(this.mAmPmLayout, 0);
                return;
            }
            this.mRadialTimePickerHeader.removeView(this.mAmPmLayout);
            this.mRadialTimePickerHeader.addView(this.mAmPmLayout);
        }
    }

    public void setHour(int i) {
        setHourInternal(i, 0, true);
    }

    /* access modifiers changed from: private */
    public void setHourInternal(int i, int i2, boolean z) {
        if (this.mCurrentHour != i) {
            this.mCurrentHour = i;
            updateHeaderHour(i, z);
            updateHeaderAmPm();
            int i3 = 1;
            if (i2 != 1) {
                this.mRadialTimePickerView.setCurrentHour(i);
                RadialTimePickerView radialTimePickerView = this.mRadialTimePickerView;
                if (i < 12) {
                    i3 = 0;
                }
                radialTimePickerView.setAmOrPm(i3);
            }
            if (i2 != 2) {
                updateTextInputPicker();
            }
            this.mDelegator.invalidate();
            onTimeChanged();
        }
    }

    public int getHour() {
        int currentHour = this.mRadialTimePickerView.getCurrentHour();
        if (this.mIs24Hour) {
            return currentHour;
        }
        if (this.mRadialTimePickerView.getAmOrPm() == 1) {
            return (currentHour % 12) + 12;
        }
        return currentHour % 12;
    }

    public void setMinute(int i) {
        setMinuteInternal(i, 0);
    }

    /* access modifiers changed from: private */
    public void setMinuteInternal(int i, int i2) {
        if (this.mCurrentMinute != i) {
            this.mCurrentMinute = i;
            updateHeaderMinute(i, true);
            if (i2 != 1) {
                this.mRadialTimePickerView.setCurrentMinute(i);
            }
            if (i2 != 2) {
                updateTextInputPicker();
            }
            this.mDelegator.invalidate();
            onTimeChanged();
        }
    }

    public int getMinute() {
        return this.mRadialTimePickerView.getCurrentMinute();
    }

    public boolean is24Hour() {
        return this.mIs24Hour;
    }

    public void setEnabled(boolean z) {
        this.mHourView.setEnabled(z);
        this.mMinuteView.setEnabled(z);
        this.mAmLabel.setEnabled(z);
        this.mPmLabel.setEnabled(z);
        this.mRadialTimePickerView.setEnabled(z);
        this.mIsEnabled = z;
    }

    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    public Parcelable onSaveInstanceState(Parcelable parcelable) {
        SavedState savedState = new SavedState(parcelable, getHour(), getMinute(), is24Hour(), getCurrentItemShowing());
        return savedState;
    }

    public void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable instanceof SavedState) {
            SavedState savedState = (SavedState) parcelable;
            initialize(savedState.getHour(), savedState.getMinute(), savedState.is24HourMode(), savedState.getCurrentItemShowing());
            this.mRadialTimePickerView.invalidate();
        }
    }

    private int getCurrentItemShowing() {
        return this.mRadialTimePickerView.getCurrentItemShowing();
    }

    private void onTimeChanged() {
        this.mDelegator.sendAccessibilityEvent(4);
        OnTimeChangedListener onTimeChangedListener = this.mOnTimeChangedListener;
        if (onTimeChangedListener != null) {
            onTimeChangedListener.onTimeChanged(this.mDelegator, getHour(), getMinute());
        }
        OnTimeChangedListener onTimeChangedListener2 = this.mAutoFillChangeListener;
        if (onTimeChangedListener2 != null) {
            onTimeChangedListener2.onTimeChanged(this.mDelegator, getHour(), getMinute());
        }
    }

    /* access modifiers changed from: private */
    public void tryVibrate() {
        this.mDelegator.performHapticFeedback(4);
    }

    private void updateAmPmLabelStates(int i) {
        boolean z = false;
        boolean z2 = i == 0;
        this.mAmLabel.setActivated(z2);
        this.mAmLabel.setChecked(z2);
        this.mAmLabel.getPaint().setFakeBoldText(z2);
        if (i == 1) {
            z = true;
        }
        this.mPmLabel.setActivated(z);
        this.mPmLabel.setChecked(z);
        this.mPmLabel.getPaint().setFakeBoldText(z);
    }

    /* access modifiers changed from: private */
    public int getLocalizedHour(int i) {
        if (!this.mIs24Hour) {
            i %= 12;
        }
        if (this.mHourFormatStartsAtZero || i != 0) {
            return i;
        }
        return this.mIs24Hour ? 24 : 12;
    }

    private void updateHeaderHour(int i, boolean z) {
        this.mHourView.setValue(getLocalizedHour(i));
        if (z) {
            tryAnnounceForAccessibility(this.mHourView.getText(), true);
        }
    }

    private void updateHeaderMinute(int i, boolean z) {
        this.mMinuteView.setValue(i);
        if (z) {
            tryAnnounceForAccessibility(this.mMinuteView.getText(), false);
        }
    }

    private void updateHeaderSeparator() {
        String str;
        String bestDateTimePattern = DateFormat.getBestDateTimePattern(this.mLocale, this.mIs24Hour ? "Hm" : "hm");
        int lastIndexOfAny = lastIndexOfAny(bestDateTimePattern, new char[]{'H', 'h', 'K', 'k'});
        if (lastIndexOfAny == -1) {
            str = ":";
        } else {
            str = Character.toString(bestDateTimePattern.charAt(lastIndexOfAny + 1));
        }
        this.mTextInputPickerView.updateSeparator(str);
    }

    private static int lastIndexOfAny(String str, char[] cArr) {
        if (r0 > 0) {
            for (int length = str.length() - 1; length >= 0; length--) {
                char charAt = str.charAt(length);
                for (char c : cArr) {
                    if (charAt == c) {
                        return length;
                    }
                }
            }
        }
        return -1;
    }

    private void tryAnnounceForAccessibility(CharSequence charSequence, boolean z) {
        if (this.mLastAnnouncedIsHour != z || !charSequence.equals(this.mLastAnnouncedText)) {
            this.mDelegator.announceForAccessibility(charSequence);
            this.mLastAnnouncedText = charSequence;
            this.mLastAnnouncedIsHour = z;
        }
    }

    /* access modifiers changed from: private */
    public void setCurrentItemShowing(int i, boolean z, boolean z2) {
        this.mRadialTimePickerView.setCurrentItemShowing(i, z);
        if (i == 0) {
            if (z2) {
                this.mDelegator.announceForAccessibility(this.mSelectHours);
            }
        } else if (z2) {
            this.mDelegator.announceForAccessibility(this.mSelectMinutes);
        }
        boolean z3 = false;
        this.mHourView.setActivated(i == 0);
        NumericTextView numericTextView = this.mMinuteView;
        if (i == 1) {
            z3 = true;
        }
        numericTextView.setActivated(z3);
        if (i == 0) {
            resetInputTimeTextAppearance(R$style.OPTextAppearance_Material_TimePicker_TimeLabel, this.mHourView);
            resetInputTimeTextAppearance(R$style.OPTextAppearance_Material_TimePicker_TimeLabelUnActivated, this.mMinuteView);
            return;
        }
        resetInputTimeTextAppearance(R$style.OPTextAppearance_Material_TimePicker_TimeLabel, this.mMinuteView);
        resetInputTimeTextAppearance(R$style.OPTextAppearance_Material_TimePicker_TimeLabelUnActivated, this.mHourView);
    }

    private void resetInputTimeTextAppearance(int i, TextView textView) {
        if (SystemUtils.isAtLeastM()) {
            textView.setTextAppearance(i);
        } else {
            textView.setTextAppearance(this.mContext, i);
        }
    }

    /* access modifiers changed from: private */
    public void setAmOrPm(int i) {
        updateAmPmLabelStates(i);
        if (this.mRadialTimePickerView.setAmOrPm(i)) {
            this.mCurrentHour = getHour();
            updateTextInputPicker();
            OnTimeChangedListener onTimeChangedListener = this.mOnTimeChangedListener;
            if (onTimeChangedListener != null) {
                onTimeChangedListener.onTimeChanged(this.mDelegator, getHour(), getMinute());
            }
        }
    }
}

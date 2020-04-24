package com.oneplus.lib.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$color;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.util.MathUtils;
import com.oneplus.lib.util.OPFeaturesUtils;
import com.oneplus.lib.util.VibratorSceneUtils;
import com.oneplus.lib.widget.util.ViewUtils;
import java.lang.reflect.Array;
import java.util.Calendar;
import java.util.Locale;

public class RadialTimePickerView extends View {
    private static final float[] COS_30 = new float[12];
    private static final int[] HOURS_NUMBERS = {12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private static final int[] HOURS_NUMBERS_24 = {0, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] MINUTES_NUMBERS = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55};
    private static final int[] SECOND_MINUTES_NUMBERS = new int[4];
    private static final float[] SIN_30 = new float[12];
    private static final int[] SNAP_PREFER_30S_MAP = new int[361];
    private final FloatProperty<RadialTimePickerView> HOURS_TO_MINUTES;
    /* access modifiers changed from: private */
    public int mAmOrPm;
    private int mCenterDotRadius;
    boolean mChangedDuringTouch;
    /* access modifiers changed from: private */
    public int mCircleRadius;
    private float mDisabledAlpha;
    private int mHalfwayDist;
    private final String[] mHours12Texts;
    /* access modifiers changed from: private */
    public float mHoursToMinutes;
    private ObjectAnimator mHoursToMinutesAnimator;
    private final String[] mInnerHours24Texts;
    private String[] mInnerTextHours;
    private final float[] mInnerTextX;
    private final float[] mInnerTextY;
    private boolean mInputEnabled;
    /* access modifiers changed from: private */
    public boolean mIs24HourMode;
    private boolean mIsOnInnerCircle;
    private OnValueSelectedListener mListener;
    private int mMaxDistForOuterNumber;
    private int mMinDistForInnerNumber;
    private String[] mMinutesText;
    private final String[] mMinutesTexts;
    private final String[] mOuterHours24Texts;
    private String[] mOuterTextHours;
    private final float[][] mOuterTextX;
    private final float[][] mOuterTextY;
    private final Paint[] mPaint;
    private final Paint mPaintBackground;
    private final Paint mPaintCenter;
    private final Paint[] mPaintSelector;
    private final int[] mSelectionDegrees;
    private int mSelectorColor;
    private int mSelectorDotColor;
    private int mSelectorDotRadius;
    private final Path mSelectorPath;
    /* access modifiers changed from: private */
    public int mSelectorRadius;
    private int mSelectorStroke;
    /* access modifiers changed from: private */
    public boolean mShowHours;
    private final ColorStateList[] mTextColor;
    /* access modifiers changed from: private */
    public final int[] mTextInset;
    private final int[] mTextSize;
    private final RadialPickerTouchHelper mTouchHelper;
    private final Typeface mTypeface;
    private long[] mVibratePattern;
    private Vibrator mVibrator;
    /* access modifiers changed from: private */
    public int mXCenter;
    /* access modifiers changed from: private */
    public int mYCenter;
    RectF oval;

    interface OnValueSelectedListener {
        void onValueSelected(int i, int i2, boolean z);
    }

    private class RadialPickerTouchHelper extends ExploreByTouchHelper {
        private final int MASK_TYPE = 15;
        private final int MASK_VALUE = 255;
        private final int MINUTE_INCREMENT = 5;
        private final int SHIFT_TYPE = 0;
        private final int SHIFT_VALUE = 8;
        private final int TYPE_HOUR = 1;
        private final int TYPE_MINUTE = 2;
        private final Rect mTempRect = new Rect();

        private int getTypeFromId(int i) {
            return (i >>> 0) & 15;
        }

        private int getValueFromId(int i) {
            return (i >>> 8) & 255;
        }

        private int hour12To24(int i, int i2) {
            if (i != 12) {
                return i2 == 1 ? i + 12 : i;
            }
            if (i2 == 0) {
                return 0;
            }
            return i;
        }

        private int hour24To12(int i) {
            if (i == 0) {
                return 12;
            }
            if (i > 12) {
                i -= 12;
            }
            return i;
        }

        private int makeId(int i, int i2) {
            return (i << 0) | (i2 << 8);
        }

        public RadialPickerTouchHelper() {
            super(RadialTimePickerView.this);
        }

        public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
            accessibilityNodeInfo.addAction(AccessibilityAction.ACTION_SCROLL_FORWARD);
            accessibilityNodeInfo.addAction(AccessibilityAction.ACTION_SCROLL_BACKWARD);
        }

        public boolean performAccessibilityAction(View view, int i, Bundle bundle) {
            if (super.performAccessibilityAction(view, i, bundle)) {
                return true;
            }
            if (i == 4096) {
                adjustPicker(1);
                return true;
            } else if (i != 8192) {
                return false;
            } else {
                adjustPicker(-1);
                return true;
            }
        }

        private void adjustPicker(int i) {
            int i2;
            int i3;
            int i4 = 1;
            int i5 = 0;
            if (RadialTimePickerView.this.mShowHours) {
                i3 = RadialTimePickerView.this.getCurrentHour();
                if (RadialTimePickerView.this.mIs24HourMode) {
                    i2 = 23;
                } else {
                    i3 = hour24To12(i3);
                    i2 = 12;
                    i5 = 1;
                }
            } else {
                i4 = 5;
                i3 = RadialTimePickerView.this.getCurrentMinute() / 5;
                i2 = 55;
            }
            int constrain = MathUtils.constrain((i3 + i) * i4, i5, i2);
            if (RadialTimePickerView.this.mShowHours) {
                RadialTimePickerView.this.setCurrentHour(constrain);
            } else {
                RadialTimePickerView.this.setCurrentMinute(constrain);
            }
        }

        /* access modifiers changed from: protected */
        public int getVirtualViewAt(float f, float f2) {
            int access$300 = RadialTimePickerView.this.getDegreesFromXY(f, f2, true);
            if (access$300 == -1) {
                return Integer.MIN_VALUE;
            }
            int access$400 = RadialTimePickerView.snapOnly30s(access$300, 0) % 360;
            if (RadialTimePickerView.this.mShowHours) {
                int access$600 = RadialTimePickerView.this.getHourForDegrees(access$400, RadialTimePickerView.this.getInnerCircleFromXY(f, f2));
                if (!RadialTimePickerView.this.mIs24HourMode) {
                    access$600 = hour24To12(access$600);
                }
                return makeId(1, access$600);
            }
            int currentMinute = RadialTimePickerView.this.getCurrentMinute();
            int access$700 = RadialTimePickerView.this.getMinuteForDegrees(access$300);
            int access$7002 = RadialTimePickerView.this.getMinuteForDegrees(access$400);
            if (getCircularDiff(currentMinute, access$700, 60) >= getCircularDiff(access$7002, access$700, 60)) {
                currentMinute = access$7002;
            }
            return makeId(2, currentMinute);
        }

        private int getCircularDiff(int i, int i2, int i3) {
            int abs = Math.abs(i - i2);
            return abs > i3 / 2 ? i3 - abs : abs;
        }

        /* access modifiers changed from: protected */
        public void getVisibleVirtualViews(IntArray intArray) {
            if (RadialTimePickerView.this.mShowHours) {
                int i = RadialTimePickerView.this.mIs24HourMode ? 23 : 12;
                for (int i2 = !RadialTimePickerView.this.mIs24HourMode; i2 <= i; i2++) {
                    intArray.add(makeId(1, i2));
                }
                return;
            }
            int currentMinute = RadialTimePickerView.this.getCurrentMinute();
            for (int i3 = 0; i3 < 60; i3 += 5) {
                intArray.add(makeId(2, i3));
                if (currentMinute > i3 && currentMinute < i3 + 5) {
                    intArray.add(makeId(2, currentMinute));
                }
            }
        }

        /* access modifiers changed from: protected */
        public void onPopulateEventForVirtualView(int i, AccessibilityEvent accessibilityEvent) {
            accessibilityEvent.setClassName(RadialPickerTouchHelper.class.getName());
            accessibilityEvent.setContentDescription(getVirtualViewDescription(getTypeFromId(i), getValueFromId(i)));
        }

        /* access modifiers changed from: protected */
        public void onPopulateNodeForVirtualView(int i, AccessibilityNodeInfo accessibilityNodeInfo) {
            accessibilityNodeInfo.setClassName(RadialPickerTouchHelper.class.getName());
            accessibilityNodeInfo.addAction(AccessibilityAction.ACTION_CLICK);
            int typeFromId = getTypeFromId(i);
            int valueFromId = getValueFromId(i);
            accessibilityNodeInfo.setContentDescription(getVirtualViewDescription(typeFromId, valueFromId));
            getBoundsForVirtualView(i, this.mTempRect);
            accessibilityNodeInfo.setBoundsInParent(this.mTempRect);
            accessibilityNodeInfo.setSelected(isVirtualViewSelected(typeFromId, valueFromId));
            int virtualViewIdAfter = getVirtualViewIdAfter(typeFromId, valueFromId);
            if (virtualViewIdAfter != Integer.MIN_VALUE && VERSION.SDK_INT >= 22) {
                accessibilityNodeInfo.setTraversalBefore(RadialTimePickerView.this, virtualViewIdAfter);
            }
        }

        private int getVirtualViewIdAfter(int i, int i2) {
            if (i == 1) {
                int i3 = i2 + 1;
                if (i3 <= (RadialTimePickerView.this.mIs24HourMode ? 23 : 12)) {
                    return makeId(i, i3);
                }
            } else if (i == 2) {
                int currentMinute = RadialTimePickerView.this.getCurrentMinute();
                int i4 = (i2 - (i2 % 5)) + 5;
                if (i2 < currentMinute && i4 > currentMinute) {
                    return makeId(i, currentMinute);
                }
                if (i4 < 60) {
                    return makeId(i, i4);
                }
            }
            return Integer.MIN_VALUE;
        }

        /* access modifiers changed from: protected */
        public boolean onPerformActionForVirtualView(int i, int i2, Bundle bundle) {
            if (i2 == 16) {
                int typeFromId = getTypeFromId(i);
                int valueFromId = getValueFromId(i);
                if (typeFromId == 1) {
                    if (!RadialTimePickerView.this.mIs24HourMode) {
                        valueFromId = hour12To24(valueFromId, RadialTimePickerView.this.mAmOrPm);
                    }
                    RadialTimePickerView.this.setCurrentHour(valueFromId);
                    return true;
                } else if (typeFromId == 2) {
                    RadialTimePickerView.this.setCurrentMinute(valueFromId);
                    return true;
                }
            }
            return false;
        }

        private void getBoundsForVirtualView(int i, Rect rect) {
            float f;
            float f2;
            int i2;
            float f3;
            int typeFromId = getTypeFromId(i);
            int valueFromId = getValueFromId(i);
            float f4 = 0.0f;
            if (typeFromId == 1) {
                if (RadialTimePickerView.this.getInnerCircleForHour(valueFromId)) {
                    f3 = (float) (RadialTimePickerView.this.mCircleRadius - RadialTimePickerView.this.mTextInset[2]);
                    i2 = RadialTimePickerView.this.mSelectorRadius;
                } else {
                    f3 = (float) (RadialTimePickerView.this.mCircleRadius - RadialTimePickerView.this.mTextInset[0]);
                    i2 = RadialTimePickerView.this.mSelectorRadius;
                }
                f4 = f3;
                f = (float) RadialTimePickerView.this.getDegreesForHour(valueFromId);
                f2 = (float) i2;
            } else if (typeFromId == 2) {
                f4 = (float) (RadialTimePickerView.this.mCircleRadius - RadialTimePickerView.this.mTextInset[1]);
                f = (float) RadialTimePickerView.this.getDegreesForMinute(valueFromId);
                f2 = (float) RadialTimePickerView.this.mSelectorRadius;
            } else {
                f = 0.0f;
                f2 = 0.0f;
            }
            double radians = Math.toRadians((double) f);
            float access$1500 = ((float) RadialTimePickerView.this.mXCenter) + (((float) Math.sin(radians)) * f4);
            float access$1600 = ((float) RadialTimePickerView.this.mYCenter) - (f4 * ((float) Math.cos(radians)));
            rect.set((int) (access$1500 - f2), (int) (access$1600 - f2), (int) (access$1500 + f2), (int) (access$1600 + f2));
        }

        private CharSequence getVirtualViewDescription(int i, int i2) {
            if (i == 1 || i == 2) {
                return Integer.toString(i2);
            }
            return null;
        }

        private boolean isVirtualViewSelected(int i, int i2) {
            if (i == 1) {
                if (RadialTimePickerView.this.getCurrentHour() != i2) {
                    return false;
                }
            } else if (!(i == 2 && RadialTimePickerView.this.getCurrentMinute() == i2)) {
                return false;
            }
            return true;
        }
    }

    /* access modifiers changed from: private */
    public int getDegreesForMinute(int i) {
        return i * 6;
    }

    static {
        preparePrefer30sMap();
        double d = 1.5707963267948966d;
        for (int i = 0; i < 12; i++) {
            COS_30[i] = (float) Math.cos(d);
            SIN_30[i] = (float) Math.sin(d);
            d += 0.5235987755982988d;
        }
    }

    private static void preparePrefer30sMap() {
        int i = 1;
        int i2 = 8;
        int i3 = 0;
        for (int i4 = 0; i4 < 361; i4++) {
            SNAP_PREFER_30S_MAP[i4] = i3;
            if (i == i2) {
                i3 += 6;
                int i5 = i3 == 360 ? 7 : i3 % 30 == 0 ? 14 : 4;
                i2 = i5;
                i = 1;
            } else {
                i++;
            }
        }
    }

    private static int snapPrefer30s(int i) {
        int[] iArr = SNAP_PREFER_30S_MAP;
        if (iArr == null) {
            return -1;
        }
        return iArr[i];
    }

    /* access modifiers changed from: private */
    public static int snapOnly30s(int i, int i2) {
        int i3 = (i / 30) * 30;
        int i4 = i3 + 30;
        if (i2 != 1) {
            if (i2 == -1) {
                return i == i3 ? i3 - 30 : i3;
            }
            if (i - i3 < i4 - i) {
                return i3;
            }
        }
        return i4;
    }

    public RadialTimePickerView(Context context) {
        this(context, null);
    }

    public RadialTimePickerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.timePickerStyle);
    }

    public RadialTimePickerView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public RadialTimePickerView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet);
        this.HOURS_TO_MINUTES = new FloatProperty<RadialTimePickerView>("hoursToMinutes") {
            public Float get(RadialTimePickerView radialTimePickerView) {
                return Float.valueOf(radialTimePickerView.mHoursToMinutes);
            }

            public void setValue(RadialTimePickerView radialTimePickerView, float f) {
                radialTimePickerView.mHoursToMinutes = f;
                radialTimePickerView.invalidate();
            }
        };
        this.mHours12Texts = new String[12];
        this.mOuterHours24Texts = new String[12];
        this.mInnerHours24Texts = new String[12];
        this.mMinutesTexts = new String[12];
        this.mPaint = new Paint[2];
        this.mPaintCenter = new Paint();
        this.mPaintSelector = new Paint[3];
        this.mPaintBackground = new Paint();
        this.mTextColor = new ColorStateList[3];
        this.mTextSize = new int[3];
        this.mTextInset = new int[3];
        this.mOuterTextX = (float[][]) Array.newInstance(float.class, new int[]{2, 12});
        this.mOuterTextY = (float[][]) Array.newInstance(float.class, new int[]{2, 12});
        this.mInnerTextX = new float[12];
        this.mInnerTextY = new float[12];
        this.mSelectionDegrees = new int[2];
        this.mSelectorPath = new Path();
        this.mInputEnabled = true;
        this.oval = new RectF();
        this.mChangedDuringTouch = false;
        applyAttributes(attributeSet, i, i2);
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(16842803, typedValue, true);
        this.mDisabledAlpha = typedValue.getFloat();
        this.mTypeface = Typeface.create("sans-serif", 0);
        this.mPaint[0] = new Paint();
        this.mPaint[0].setAntiAlias(true);
        this.mPaint[0].setTextAlign(Align.CENTER);
        this.mPaint[1] = new Paint();
        this.mPaint[1].setAntiAlias(true);
        this.mPaint[1].setTextAlign(Align.CENTER);
        this.mPaintCenter.setAntiAlias(true);
        this.mPaintSelector[0] = new Paint();
        this.mPaintSelector[0].setAntiAlias(true);
        this.mPaintSelector[1] = new Paint();
        this.mPaintSelector[1].setAntiAlias(true);
        this.mPaintSelector[2] = new Paint();
        this.mPaintSelector[2].setAntiAlias(true);
        this.mPaintSelector[2].setStrokeWidth(2.0f);
        this.mPaintBackground.setAntiAlias(true);
        Resources resources = getResources();
        this.mSelectorRadius = resources.getDimensionPixelSize(R$dimen.timepicker_selector_radius);
        this.mSelectorStroke = resources.getDimensionPixelSize(R$dimen.timepicker_selector_stroke);
        this.mSelectorDotRadius = resources.getDimensionPixelSize(R$dimen.timepicker_selector_dot_radius);
        this.mCenterDotRadius = resources.getDimensionPixelSize(R$dimen.timepicker_center_dot_radius);
        this.mTextSize[0] = resources.getDimensionPixelSize(R$dimen.timepicker_text_size_normal);
        this.mTextSize[1] = resources.getDimensionPixelSize(R$dimen.timepicker_text_size_normal);
        this.mTextSize[2] = resources.getDimensionPixelSize(R$dimen.timepicker_text_size_inner);
        this.mTextInset[0] = resources.getDimensionPixelSize(R$dimen.timepicker_text_inset_normal);
        this.mTextInset[1] = resources.getDimensionPixelSize(R$dimen.timepicker_text_inset_normal);
        this.mTextInset[2] = resources.getDimensionPixelSize(R$dimen.timepicker_text_inset_inner);
        this.mShowHours = true;
        this.mHoursToMinutes = 0.0f;
        this.mIs24HourMode = false;
        this.mAmOrPm = 0;
        this.mTouchHelper = new RadialPickerTouchHelper();
        setAccessibilityDelegate(this.mTouchHelper);
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
        initHoursAndMinutesText();
        initData();
        Calendar instance = Calendar.getInstance(Locale.getDefault());
        int i3 = instance.get(11);
        int i4 = instance.get(12);
        setCurrentHourInternal(i3, false, false);
        setCurrentMinuteInternal(i4, false);
        if (OPFeaturesUtils.isSupportXVibrate()) {
            this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        }
        setHapticFeedbackEnabled(true);
    }

    /* access modifiers changed from: 0000 */
    public void applyAttributes(AttributeSet attributeSet, int i, int i2) {
        Context context = getContext();
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R$styleable.TimePicker, i, i2);
        ColorStateList colorStateList = obtainStyledAttributes.getColorStateList(R$styleable.TimePicker_numbersTextColor);
        ColorStateList colorStateList2 = obtainStyledAttributes.getColorStateList(R$styleable.TimePicker_numbersInnerTextColor);
        ColorStateList[] colorStateListArr = this.mTextColor;
        int i3 = -65281;
        if (colorStateList == null) {
            colorStateList = ColorStateList.valueOf(-65281);
        }
        colorStateListArr[0] = colorStateList;
        ColorStateList[] colorStateListArr2 = this.mTextColor;
        if (colorStateList2 == null) {
            colorStateList2 = ColorStateList.valueOf(-65281);
        }
        colorStateListArr2[2] = colorStateList2;
        ColorStateList[] colorStateListArr3 = this.mTextColor;
        colorStateListArr3[1] = colorStateListArr3[0];
        ColorStateList colorStateList3 = obtainStyledAttributes.getColorStateList(R$styleable.TimePicker_android_numbersSelectorColor);
        if (colorStateList3 != null) {
            i3 = colorStateList3.getColorForState(ViewUtils.getViewState(40), 0);
        }
        this.mPaintCenter.setColor(i3);
        int[] viewState = ViewUtils.getViewState(40);
        this.mSelectorColor = i3;
        this.mSelectorDotColor = this.mTextColor[0].getColorForState(viewState, 0);
        this.mPaintBackground.setColor(obtainStyledAttributes.getColor(R$styleable.TimePicker_android_numbersBackgroundColor, context.getResources().getColor(R$color.timepicker_default_numbers_background_color_material)));
        obtainStyledAttributes.recycle();
    }

    public void initialize(int i, int i2, boolean z) {
        if (this.mIs24HourMode != z) {
            this.mIs24HourMode = z;
            initData();
        }
        setCurrentHourInternal(i, false, false);
        setCurrentMinuteInternal(i2, false);
    }

    public void setCurrentItemShowing(int i, boolean z) {
        if (i == 0) {
            showHours(z);
        } else if (i != 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("ClockView does not support showing item ");
            sb.append(i);
            Log.e("RadialTimePickerView", sb.toString());
        } else {
            showMinutes(z);
        }
    }

    public int getCurrentItemShowing() {
        return this.mShowHours ^ true ? 1 : 0;
    }

    public void setOnValueSelectedListener(OnValueSelectedListener onValueSelectedListener) {
        this.mListener = onValueSelectedListener;
    }

    public void setCurrentHour(int i) {
        setCurrentHourInternal(i, true, false);
    }

    private void setCurrentHourInternal(int i, boolean z, boolean z2) {
        this.mSelectionDegrees[0] = (i % 12) * 30;
        int i2 = (i == 0 || i % 24 < 12) ? 0 : 1;
        boolean innerCircleForHour = getInnerCircleForHour(i);
        if (!(this.mAmOrPm == i2 && this.mIsOnInnerCircle == innerCircleForHour)) {
            this.mAmOrPm = i2;
            this.mIsOnInnerCircle = innerCircleForHour;
            initData();
            this.mTouchHelper.invalidateRoot();
        }
        invalidate();
        if (z) {
            OnValueSelectedListener onValueSelectedListener = this.mListener;
            if (onValueSelectedListener != null) {
                onValueSelectedListener.onValueSelected(0, i, z2);
            }
        }
    }

    public int getCurrentHour() {
        return getHourForDegrees(this.mSelectionDegrees[0], this.mIsOnInnerCircle);
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:5:0x0010, code lost:
        if (r3 != 0) goto L_0x0018;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:7:0x0016, code lost:
        if (r2.mAmOrPm == 1) goto L_0x0018;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getHourForDegrees(int r3, boolean r4) {
        /*
            r2 = this;
            int r3 = r3 / 30
            r0 = 12
            int r3 = r3 % r0
            boolean r1 = r2.mIs24HourMode
            if (r1 == 0) goto L_0x0013
            if (r4 != 0) goto L_0x000e
            if (r3 != 0) goto L_0x000e
            goto L_0x001c
        L_0x000e:
            if (r4 == 0) goto L_0x001b
            if (r3 == 0) goto L_0x001b
            goto L_0x0018
        L_0x0013:
            int r2 = r2.mAmOrPm
            r4 = 1
            if (r2 != r4) goto L_0x001b
        L_0x0018:
            int r0 = r3 + 12
            goto L_0x001c
        L_0x001b:
            r0 = r3
        L_0x001c:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.RadialTimePickerView.getHourForDegrees(int, boolean):int");
    }

    /* access modifiers changed from: private */
    public int getDegreesForHour(int i) {
        if (this.mIs24HourMode) {
            if (i >= 12) {
                i -= 12;
            }
        } else if (i == 12) {
            i = 0;
        }
        return i * 30;
    }

    /* access modifiers changed from: private */
    public boolean getInnerCircleForHour(int i) {
        return this.mIs24HourMode && (i == 0 || i > 12);
    }

    public void setCurrentMinute(int i) {
        setCurrentMinuteInternal(i, true);
    }

    private void setCurrentMinuteInternal(int i, boolean z) {
        this.mSelectionDegrees[1] = (i % 60) * 6;
        invalidate();
        if (z) {
            OnValueSelectedListener onValueSelectedListener = this.mListener;
            if (onValueSelectedListener != null) {
                onValueSelectedListener.onValueSelected(1, i, false);
            }
        }
    }

    public int getCurrentMinute() {
        return getMinuteForDegrees(this.mSelectionDegrees[1]);
    }

    /* access modifiers changed from: private */
    public int getMinuteForDegrees(int i) {
        return i / 6;
    }

    public boolean setAmOrPm(int i) {
        if (this.mAmOrPm == i || this.mIs24HourMode) {
            return false;
        }
        this.mAmOrPm = i;
        invalidate();
        this.mTouchHelper.invalidateRoot();
        return true;
    }

    public int getAmOrPm() {
        return this.mAmOrPm;
    }

    public void showHours(boolean z) {
        showPicker(true, z);
    }

    public void showMinutes(boolean z) {
        showPicker(false, z);
    }

    private void initHoursAndMinutesText() {
        for (int i = 0; i < 12; i++) {
            String str = "%d";
            this.mHours12Texts[i] = String.format(str, new Object[]{Integer.valueOf(HOURS_NUMBERS[i])});
            String str2 = "%02d";
            this.mInnerHours24Texts[i] = String.format(str2, new Object[]{Integer.valueOf(HOURS_NUMBERS_24[i])});
            this.mOuterHours24Texts[i] = String.format(str, new Object[]{Integer.valueOf(HOURS_NUMBERS[i])});
            this.mMinutesTexts[i] = String.format(str2, new Object[]{Integer.valueOf(MINUTES_NUMBERS[i])});
        }
    }

    private void initData() {
        if (this.mIs24HourMode) {
            this.mOuterTextHours = this.mOuterHours24Texts;
            this.mInnerTextHours = this.mInnerHours24Texts;
        } else {
            String[] strArr = this.mHours12Texts;
            this.mOuterTextHours = strArr;
            this.mInnerTextHours = strArr;
        }
        this.mMinutesText = this.mMinutesTexts;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (z) {
            this.mXCenter = getWidth() / 2;
            this.mYCenter = getHeight() / 2;
            this.mCircleRadius = Math.min(this.mXCenter - Math.max(getPaddingLeft(), getPaddingRight()), this.mYCenter - Math.max(getPaddingTop(), getPaddingBottom()));
            int i5 = this.mCircleRadius;
            int[] iArr = this.mTextInset;
            int i6 = i5 - iArr[2];
            int i7 = this.mSelectorRadius;
            this.mMinDistForInnerNumber = i6 - i7;
            this.mMaxDistForOuterNumber = (i5 - iArr[0]) + i7;
            this.mHalfwayDist = i5 - ((iArr[0] + iArr[2]) / 2);
            calculatePositionsHours();
            calculatePositionsMinutes();
            this.mTouchHelper.invalidateRoot();
        }
    }

    public void onDraw(Canvas canvas) {
        float f = this.mInputEnabled ? 1.0f : this.mDisabledAlpha;
        drawCircleBackground(canvas);
        Path path = this.mSelectorPath;
        drawSelector(canvas, path);
        drawHours(canvas, path, f);
        drawMinutes(canvas, path, f);
        drawCenter(canvas, f);
    }

    private void showPicker(boolean z, boolean z2) {
        if (this.mShowHours != z) {
            this.mShowHours = z;
            if (z2) {
                animatePicker(z, 500);
            } else {
                ObjectAnimator objectAnimator = this.mHoursToMinutesAnimator;
                if (objectAnimator != null && objectAnimator.isStarted()) {
                    this.mHoursToMinutesAnimator.cancel();
                    this.mHoursToMinutesAnimator = null;
                }
                this.mHoursToMinutes = z ? 0.0f : 1.0f;
            }
            initData();
            invalidate();
            this.mTouchHelper.invalidateRoot();
        }
    }

    private void animatePicker(boolean z, long j) {
        float f = z ? 0.0f : 1.0f;
        float f2 = this.mHoursToMinutes;
        if (f2 == f) {
            ObjectAnimator objectAnimator = this.mHoursToMinutesAnimator;
            if (objectAnimator != null && objectAnimator.isStarted()) {
                this.mHoursToMinutesAnimator.cancel();
                this.mHoursToMinutesAnimator = null;
            }
            return;
        }
        this.mHoursToMinutesAnimator = ObjectAnimator.ofFloat(this, "HoursToMinutes", new float[]{f2, f});
        this.mHoursToMinutesAnimator.setAutoCancel(true);
        this.mHoursToMinutesAnimator.setDuration(j);
        this.mHoursToMinutesAnimator.start();
    }

    private void drawCircleBackground(Canvas canvas) {
        canvas.drawCircle((float) this.mXCenter, (float) this.mYCenter, (float) this.mCircleRadius, this.mPaintBackground);
    }

    private void drawHours(Canvas canvas, Path path, float f) {
        int i = (int) (((1.0f - this.mHoursToMinutes) * 255.0f * f) + 0.5f);
        if (i > 0) {
            canvas.save();
            canvas.clipPath(path, Op.DIFFERENCE);
            drawHoursClipped(canvas, i, false);
            canvas.restore();
            canvas.save();
            canvas.clipPath(path, Op.INTERSECT);
            drawHoursClipped(canvas, i, true);
            canvas.restore();
        }
    }

    private void drawHoursClipped(Canvas canvas, int i, boolean z) {
        drawTextElements(canvas, (float) this.mTextSize[0], this.mTypeface, this.mTextColor[0], this.mOuterTextHours, this.mOuterTextX[0], this.mOuterTextY[0], this.mPaint[0], i, z && !this.mIsOnInnerCircle, this.mSelectionDegrees[0], z);
        if (this.mIs24HourMode) {
            String[] strArr = this.mInnerTextHours;
            if (strArr != null) {
                drawTextElements(canvas, (float) this.mTextSize[2], this.mTypeface, this.mTextColor[2], strArr, this.mInnerTextX, this.mInnerTextY, this.mPaint[0], i, z && this.mIsOnInnerCircle, this.mSelectionDegrees[0], z);
            }
        }
    }

    private void drawMinutes(Canvas canvas, Path path, float f) {
        int i = (int) ((this.mHoursToMinutes * 255.0f * f) + 0.5f);
        if (i > 0) {
            canvas.save();
            canvas.clipPath(path, Op.DIFFERENCE);
            drawMinutesClipped(canvas, i, false);
            canvas.restore();
            canvas.save();
            canvas.clipPath(path, Op.INTERSECT);
            drawMinutesClipped(canvas, i, true);
            canvas.restore();
        }
    }

    private void drawMinutesClipped(Canvas canvas, int i, boolean z) {
        drawTextElements(canvas, (float) this.mTextSize[1], this.mTypeface, this.mTextColor[1], this.mMinutesText, this.mOuterTextX[1], this.mOuterTextY[1], this.mPaint[1], i, z, this.mSelectionDegrees[1], z);
    }

    private void drawCenter(Canvas canvas, float f) {
        this.mPaintCenter.setAlpha((int) ((f * 255.0f) + 0.5f));
        canvas.drawCircle((float) this.mXCenter, (float) this.mYCenter, (float) this.mCenterDotRadius, this.mPaintCenter);
    }

    private int getMultipliedAlpha(int i, int i2) {
        return (int) ((((double) Color.alpha(i)) * (((double) i2) / 255.0d)) + 0.5d);
    }

    private void drawSelector(Canvas canvas, Path path) {
        Canvas canvas2 = canvas;
        Path path2 = path;
        int i = this.mIsOnInnerCircle ? 2 : 0;
        int i2 = this.mTextInset[i];
        int[] iArr = this.mSelectionDegrees;
        int i3 = i % 2;
        int i4 = iArr[i3];
        int i5 = iArr[i3] % 30;
        float f = 1.0f;
        float f2 = i5 != 0 ? 1.0f : 0.0f;
        int i6 = this.mTextInset[1];
        int[] iArr2 = this.mSelectionDegrees;
        int i7 = iArr2[1];
        if (iArr2[1] % 30 == 0) {
            f = 0.0f;
        }
        int i8 = this.mSelectorRadius;
        float lerp = ((float) this.mCircleRadius) - MathUtils.lerp((float) i2, (float) i6, this.mHoursToMinutes);
        double radians = Math.toRadians((double) MathUtils.lerpDeg((float) i4, (float) i7, this.mHoursToMinutes));
        float sin = ((float) this.mXCenter) + (((float) Math.sin(radians)) * lerp);
        float cos = ((float) this.mYCenter) - (((float) Math.cos(radians)) * lerp);
        Paint paint = this.mPaintSelector[0];
        paint.setColor(this.mSelectorColor);
        float f3 = (float) i8;
        Canvas canvas3 = canvas;
        canvas3.drawCircle(sin, cos, f3, paint);
        if (path2 != null) {
            path.reset();
            path2.addCircle(sin, cos, f3, Direction.CCW);
        }
        float lerp2 = MathUtils.lerp(f2, f, this.mHoursToMinutes);
        if (lerp2 > 0.0f) {
            Paint paint2 = this.mPaintSelector[1];
            paint2.setColor(this.mSelectorDotColor);
            canvas3.drawCircle(sin, cos, ((float) this.mSelectorDotRadius) * lerp2, paint2);
        }
        double sin2 = Math.sin(radians);
        double cos2 = Math.cos(radians);
        float f4 = lerp - f3;
        int i9 = this.mXCenter;
        int i10 = this.mCenterDotRadius;
        double d = (double) f4;
        float f5 = (float) (i9 + ((int) (((double) i10) * sin2)) + ((int) (sin2 * d)));
        float f6 = (float) ((this.mYCenter - ((int) (((double) i10) * cos2))) - ((int) (d * cos2)));
        Paint paint3 = this.mPaintSelector[2];
        paint3.setColor(this.mSelectorColor);
        paint3.setStrokeWidth((float) this.mSelectorStroke);
        canvas.drawLine((float) this.mXCenter, (float) this.mYCenter, f5, f6, paint3);
        if (!this.mShowHours) {
            paint3.setColor(-7829368);
            RectF rectF = this.oval;
            float[][] fArr = this.mOuterTextX;
            rectF.set(fArr[1][9], fArr[1][0], fArr[1][3], fArr[1][6]);
            canvas.drawArc(this.oval, (float) (getDegreesForMinute(getCurrentMinute()) - 45), 90.0f, true, paint3);
        }
    }

    private void calculatePositionsHours() {
        calculatePositions(this.mPaint[0], (float) (this.mCircleRadius - this.mTextInset[0]), (float) this.mXCenter, (float) this.mYCenter, (float) this.mTextSize[0], this.mOuterTextX[0], this.mOuterTextY[0]);
        if (this.mIs24HourMode) {
            calculatePositions(this.mPaint[0], (float) (this.mCircleRadius - this.mTextInset[2]), (float) this.mXCenter, (float) this.mYCenter, (float) this.mTextSize[2], this.mInnerTextX, this.mInnerTextY);
        }
    }

    private void calculatePositionsMinutes() {
        calculatePositions(this.mPaint[1], (float) (this.mCircleRadius - this.mTextInset[1]), (float) this.mXCenter, (float) this.mYCenter, (float) this.mTextSize[1], this.mOuterTextX[1], this.mOuterTextY[1]);
    }

    private static void calculatePositions(Paint paint, float f, float f2, float f3, float f4, float[] fArr, float[] fArr2) {
        paint.setTextSize(f4);
        float descent = f3 - ((paint.descent() + paint.ascent()) / 2.0f);
        for (int i = 0; i < 12; i++) {
            fArr[i] = f2 - (COS_30[i] * f);
            fArr2[i] = descent - (SIN_30[i] * f);
        }
    }

    private void drawTextElements(Canvas canvas, float f, Typeface typeface, ColorStateList colorStateList, String[] strArr, float[] fArr, float[] fArr2, Paint paint, int i, boolean z, int i2, boolean z2) {
        Paint paint2 = paint;
        float f2 = f;
        paint2.setTextSize(f);
        paint2.setTypeface(typeface);
        float f3 = ((float) i2) / 30.0f;
        int i3 = (int) f3;
        int ceil = ((int) Math.ceil((double) f3)) % 12;
        int i4 = 0;
        while (i4 < 12) {
            boolean z3 = i3 == i4 || ceil == i4;
            if (!z2 || z3) {
                int colorForState = colorStateList.getColorForState(ViewUtils.getViewState(((!z || !z3) ? 0 : 32) | 8), 0);
                paint2.setColor(colorForState);
                paint2.setAlpha(getMultipliedAlpha(colorForState, i));
                Canvas canvas2 = canvas;
                canvas.drawText(strArr[i4], fArr[i4], fArr2[i4], paint2);
            } else {
                Canvas canvas3 = canvas;
                ColorStateList colorStateList2 = colorStateList;
                int i5 = i;
            }
            i4++;
        }
    }

    /* access modifiers changed from: private */
    public int getDegreesFromXY(float f, float f2, boolean z) {
        int i;
        int i2;
        if (!this.mIs24HourMode || !this.mShowHours) {
            int i3 = this.mCircleRadius - this.mTextInset[!this.mShowHours];
            int i4 = this.mSelectorRadius;
            int i5 = i3 - i4;
            i = i3 + i4;
            i2 = i5;
        } else {
            i2 = this.mMinDistForInnerNumber;
            i = this.mMaxDistForOuterNumber;
        }
        double d = (double) (f - ((float) this.mXCenter));
        double d2 = (double) (f2 - ((float) this.mYCenter));
        double sqrt = Math.sqrt((d * d) + (d2 * d2));
        if (sqrt < ((double) i2) || (z && sqrt > ((double) i))) {
            return -1;
        }
        int degrees = (int) (Math.toDegrees(Math.atan2(d2, d) + 1.5707963267948966d) + 0.5d);
        if (degrees < 0) {
            degrees += 360;
        }
        return degrees;
    }

    /* access modifiers changed from: private */
    public boolean getInnerCircleFromXY(float f, float f2) {
        if (!this.mIs24HourMode || !this.mShowHours) {
            return false;
        }
        double d = (double) (f - ((float) this.mXCenter));
        double d2 = (double) (f2 - ((float) this.mYCenter));
        if (Math.sqrt((d * d) + (d2 * d2)) <= ((double) this.mHalfwayDist)) {
            return true;
        }
        return false;
    }

    private boolean isVisible() {
        return getVisibility() == 0;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z;
        if (!isVisible()) {
            return super.onTouchEvent(motionEvent);
        }
        if (!this.mInputEnabled) {
            return true;
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 2 || actionMasked == 1 || actionMasked == 0) {
            boolean z2 = false;
            if (actionMasked == 0) {
                this.mChangedDuringTouch = false;
            } else if (actionMasked == 1) {
                if (!this.mChangedDuringTouch) {
                    z = true;
                    z2 = true;
                } else {
                    z = true;
                }
                this.mChangedDuringTouch = handleTouchInput(motionEvent.getX(), motionEvent.getY(), z2, z) | this.mChangedDuringTouch;
            }
            z = false;
            this.mChangedDuringTouch = handleTouchInput(motionEvent.getX(), motionEvent.getY(), z2, z) | this.mChangedDuringTouch;
        }
        return true;
    }

    private boolean handleTouchInput(float f, float f2, boolean z, boolean z2) {
        boolean z3;
        int i;
        int i2;
        boolean innerCircleFromXY = getInnerCircleFromXY(f, f2);
        int degreesFromXY = getDegreesFromXY(f, f2, false);
        if (degreesFromXY == -1) {
            return false;
        }
        animatePicker(this.mShowHours, 60);
        if (this.mShowHours) {
            int snapOnly30s = snapOnly30s(degreesFromXY, 0) % 360;
            z3 = (this.mIsOnInnerCircle == innerCircleFromXY && this.mSelectionDegrees[0] == snapOnly30s) ? false : true;
            this.mIsOnInnerCircle = innerCircleFromXY;
            this.mSelectionDegrees[0] = snapOnly30s;
            i2 = getCurrentHour();
            i = 0;
        } else {
            int snapPrefer30s = snapPrefer30s(degreesFromXY) % 360;
            z3 = this.mSelectionDegrees[1] != snapPrefer30s;
            this.mSelectionDegrees[1] = snapPrefer30s;
            i2 = getCurrentMinute();
            i = 1;
        }
        if (!z3 && !z && !z2) {
            return false;
        }
        OnValueSelectedListener onValueSelectedListener = this.mListener;
        if (onValueSelectedListener != null) {
            onValueSelectedListener.onValueSelected(i, i2, z2);
        }
        if (z3 || z) {
            if (!OPFeaturesUtils.isSupportXVibrate()) {
                performHapticFeedback(4);
            } else if (i != 1) {
                performHapticFeedback(4);
            } else if (VibratorSceneUtils.systemVibrateEnabled(getContext())) {
                this.mVibratePattern = VibratorSceneUtils.getVibratorScenePattern(getContext(), this.mVibrator, 1030);
                VibratorSceneUtils.vibrateIfNeeded(this.mVibratePattern, this.mVibrator);
            }
            invalidate();
        }
        return true;
    }

    public boolean dispatchHoverEvent(MotionEvent motionEvent) {
        if (this.mTouchHelper.dispatchHoverEvent(motionEvent)) {
            return true;
        }
        return super.dispatchHoverEvent(motionEvent);
    }

    public PointerIcon onResolvePointerIcon(MotionEvent motionEvent, int i) {
        if (!isEnabled()) {
            return null;
        }
        if (getDegreesFromXY(motionEvent.getX(), motionEvent.getY(), false) == -1 || VERSION.SDK_INT < 24) {
            return super.onResolvePointerIcon(motionEvent, i);
        }
        return PointerIcon.getSystemIcon(getContext(), 1002);
    }
}

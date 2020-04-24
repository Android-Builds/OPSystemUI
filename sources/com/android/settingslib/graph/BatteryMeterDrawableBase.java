package com.android.settingslib.graph;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Path.FillType;
import android.graphics.Path.Op;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import com.android.settingslib.R$array;
import com.android.settingslib.R$color;
import com.android.settingslib.R$dimen;
import com.android.settingslib.R$fraction;
import com.android.settingslib.R$string;
import com.android.settingslib.Utils;

public class BatteryMeterDrawableBase extends Drawable {
    public static final String TAG = "BatteryMeterDrawableBase";
    protected final Paint mBatteryPaint;
    private final RectF mBoltFrame = new RectF();
    protected final Paint mBoltPaint;
    private final Path mBoltPath = new Path();
    private final float[] mBoltPoints;
    private final RectF mButtonFrame = new RectF();
    protected float mButtonHeightFraction;
    private int mChargeColor;
    private boolean mCharging;
    private final int[] mColors;
    protected final Context mContext;
    private final int mCriticalLevel;
    private final RectF mFrame = new RectF();
    protected final Paint mFramePaint;
    private int mHeight;
    private int mIconTint = -1;
    private final int mIntrinsicHeight;
    private final int mIntrinsicWidth;
    private boolean mIsOptimizatedCharge = false;
    private int mLevel = -1;
    private float mOldDarkIntensity = -1.0f;
    private final Path mOutlinePath = new Path();
    private final Rect mPadding = new Rect();
    private final RectF mPlusFrame = new RectF();
    protected final Paint mPlusPaint;
    private final Path mPlusPath = new Path();
    private final float[] mPlusPoints;
    protected boolean mPowerSaveAsColorError = true;
    private boolean mPowerSaveEnabled;
    protected final Paint mPowersavePaint;
    private final Path mShapePath = new Path();
    private boolean mShowPercent;
    private float mSubpixelSmoothingLeft;
    private float mSubpixelSmoothingRight;
    private float mTextHeight;
    protected final Paint mTextPaint;
    private final Path mTextPath = new Path();
    private String mWarningString;
    private float mWarningTextHeight;
    protected final Paint mWarningTextPaint;
    private int mWidth;

    /* access modifiers changed from: protected */
    public float getAspectRatio() {
        return 0.58f;
    }

    public int getOpacity() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public float getRadiusRatio() {
        return 0.05882353f;
    }

    public void setAlpha(int i) {
    }

    public BatteryMeterDrawableBase(Context context, int i) {
        this.mContext = context;
        Resources resources = context.getResources();
        TypedArray obtainTypedArray = resources.obtainTypedArray(R$array.batterymeter_color_levels);
        TypedArray obtainTypedArray2 = resources.obtainTypedArray(R$array.batterymeter_color_values);
        int length = obtainTypedArray.length();
        this.mColors = new int[(length * 2)];
        for (int i2 = 0; i2 < length; i2++) {
            int i3 = i2 * 2;
            this.mColors[i3] = obtainTypedArray.getInt(i2, 0);
            if (obtainTypedArray2.getType(i2) == 2) {
                this.mColors[i3 + 1] = Utils.getColorAttrDefaultColor(context, obtainTypedArray2.getThemeAttributeId(i2, 0));
            } else {
                this.mColors[i3 + 1] = obtainTypedArray2.getColor(i2, 0);
            }
        }
        obtainTypedArray.recycle();
        obtainTypedArray2.recycle();
        this.mWarningString = context.getString(R$string.battery_meter_very_low_overlay_symbol);
        this.mCriticalLevel = this.mContext.getResources().getInteger(17694765);
        this.mButtonHeightFraction = context.getResources().getFraction(R$fraction.battery_button_height_fraction, 1, 1);
        this.mSubpixelSmoothingLeft = context.getResources().getFraction(R$fraction.battery_subpixel_smoothing_left, 1, 1);
        this.mSubpixelSmoothingRight = context.getResources().getFraction(R$fraction.battery_subpixel_smoothing_right, 1, 1);
        this.mFramePaint = new Paint(1);
        this.mFramePaint.setColor(i);
        this.mFramePaint.setDither(true);
        this.mFramePaint.setStrokeWidth(0.0f);
        this.mFramePaint.setStyle(Style.FILL_AND_STROKE);
        this.mBatteryPaint = new Paint(1);
        this.mBatteryPaint.setDither(true);
        this.mBatteryPaint.setStrokeWidth(0.0f);
        this.mBatteryPaint.setStyle(Style.FILL_AND_STROKE);
        this.mTextPaint = new Paint(1);
        this.mTextPaint.setTypeface(Typeface.create("sans-serif-condensed", 1));
        this.mTextPaint.setTextAlign(Align.CENTER);
        this.mWarningTextPaint = new Paint(1);
        this.mWarningTextPaint.setTypeface(Typeface.create("sans-serif", 1));
        this.mWarningTextPaint.setTextAlign(Align.CENTER);
        int[] iArr = this.mColors;
        if (iArr.length > 1) {
            this.mWarningTextPaint.setColor(iArr[1]);
        }
        this.mChargeColor = Utils.getColorStateListDefaultColor(this.mContext, R$color.meter_consumed_color);
        this.mBoltPaint = new Paint(1);
        this.mBoltPaint.setColor(Utils.getColorStateListDefaultColor(this.mContext, R$color.batterymeter_bolt_color));
        this.mBoltPoints = loadPoints(resources, R$array.batterymeter_bolt_points);
        this.mPlusPaint = new Paint(1);
        this.mPlusPaint.setColor(Utils.getColorStateListDefaultColor(this.mContext, R$color.batterymeter_plus_color));
        this.mPlusPoints = loadPoints(resources, R$array.batterymeter_plus_points);
        this.mPowersavePaint = new Paint(1);
        this.mPowersavePaint.setColor(this.mPlusPaint.getColor());
        this.mPowersavePaint.setStyle(Style.STROKE);
        this.mPowersavePaint.setStrokeWidth((float) context.getResources().getDimensionPixelSize(R$dimen.battery_powersave_outline_thickness));
        this.mIntrinsicWidth = context.getResources().getDimensionPixelSize(R$dimen.battery_width);
        this.mIntrinsicHeight = context.getResources().getDimensionPixelSize(R$dimen.battery_height);
    }

    public int getIntrinsicHeight() {
        return this.mIntrinsicHeight;
    }

    public int getIntrinsicWidth() {
        return this.mIntrinsicWidth;
    }

    public void setCharging(boolean z) {
        this.mCharging = z;
        postInvalidate();
    }

    public boolean getCharging() {
        return this.mCharging;
    }

    public void setBatteryLevel(int i) {
        this.mLevel = i;
        postInvalidate();
    }

    public int getBatteryLevel() {
        return this.mLevel;
    }

    public boolean getPowerSave() {
        return this.mPowerSaveEnabled;
    }

    /* access modifiers changed from: protected */
    public void postInvalidate() {
        unscheduleSelf(new Runnable() {
            public final void run() {
                BatteryMeterDrawableBase.this.invalidateSelf();
            }
        });
        scheduleSelf(new Runnable() {
            public final void run() {
                BatteryMeterDrawableBase.this.invalidateSelf();
            }
        }, 0);
    }

    private static float[] loadPoints(Resources resources, int i) {
        int[] intArray = resources.getIntArray(i);
        int i2 = 0;
        int i3 = 0;
        for (int i4 = 0; i4 < intArray.length; i4 += 2) {
            i2 = Math.max(i2, intArray[i4]);
            i3 = Math.max(i3, intArray[i4 + 1]);
        }
        float[] fArr = new float[intArray.length];
        for (int i5 = 0; i5 < intArray.length; i5 += 2) {
            fArr[i5] = ((float) intArray[i5]) / ((float) i2);
            int i6 = i5 + 1;
            fArr[i6] = ((float) intArray[i6]) / ((float) i3);
        }
        return fArr;
    }

    public void setBounds(int i, int i2, int i3, int i4) {
        super.setBounds(i, i2, i3, i4);
        updateSize();
    }

    private void updateSize() {
        Rect bounds = getBounds();
        int i = bounds.bottom;
        Rect rect = this.mPadding;
        this.mHeight = (i - rect.bottom) - (bounds.top + rect.top);
        this.mWidth = (bounds.right - rect.right) - (bounds.left + rect.left);
        this.mWarningTextPaint.setTextSize(((float) this.mHeight) * 0.75f);
        this.mWarningTextHeight = -this.mWarningTextPaint.getFontMetrics().ascent;
    }

    public boolean getPadding(Rect rect) {
        Rect rect2 = this.mPadding;
        if (rect2.left == 0 && rect2.top == 0 && rect2.right == 0 && rect2.bottom == 0) {
            return super.getPadding(rect);
        }
        rect.set(this.mPadding);
        return true;
    }

    public void setPadding(int i, int i2, int i3, int i4) {
        Rect rect = this.mPadding;
        rect.left = i;
        rect.top = i2;
        rect.right = i3;
        rect.bottom = i4;
        updateSize();
    }

    /* access modifiers changed from: protected */
    public int getColorForLevel(int i) {
        int i2 = 0;
        int i3 = 0;
        while (true) {
            int[] iArr = this.mColors;
            if (i2 >= iArr.length) {
                return i3;
            }
            int i4 = iArr[i2];
            int i5 = iArr[i2 + 1];
            if (i <= i4) {
                return i2 == iArr.length + -2 ? this.mIconTint : i5;
            }
            i2 += 2;
            i3 = i5;
        }
    }

    public void setColors(int i, int i2) {
        this.mIconTint = i;
        this.mFramePaint.setColor(i2);
        this.mBoltPaint.setColor(i);
        this.mChargeColor = i;
        invalidateSelf();
    }

    /* access modifiers changed from: protected */
    public int batteryColorForLevel(int i) {
        if (this.mCharging || (this.mPowerSaveEnabled && this.mPowerSaveAsColorError)) {
            return this.mChargeColor;
        }
        return getColorForLevel(i);
    }

    public void draw(Canvas canvas) {
        float f;
        float f2;
        float f3;
        float[] fArr;
        Canvas canvas2 = canvas;
        int i = this.mLevel;
        Rect bounds = getBounds();
        if (i != -1) {
            float f4 = ((float) i) / 100.0f;
            int i2 = this.mHeight;
            int aspectRatio = (int) (getAspectRatio() * ((float) this.mHeight));
            int i3 = (this.mWidth - aspectRatio) / 2;
            float f5 = (float) i2;
            int round = Math.round(this.mButtonHeightFraction * f5);
            Rect rect = this.mPadding;
            int i4 = rect.left + bounds.left;
            int i5 = (bounds.bottom - rect.bottom) - i2;
            int i6 = i2 / 17;
            float f6 = (float) i4;
            this.mFrame.set(f6, (float) (i5 + i6), (float) (i4 + aspectRatio), (float) ((i2 + i5) - i6));
            this.mFrame.offset((float) i3, 0.0f);
            RectF rectF = this.mButtonFrame;
            float f7 = ((float) aspectRatio) * 0.28f;
            float round2 = this.mFrame.left + ((float) Math.round(f7));
            RectF rectF2 = this.mFrame;
            float f8 = (float) round;
            rectF.set(round2, rectF2.top, rectF2.right - ((float) Math.round(f7)), this.mFrame.top + f8);
            this.mFrame.top += f8;
            this.mBatteryPaint.setColor(batteryColorForLevel(i));
            if (i >= 96) {
                f4 = 1.0f;
            } else if (i <= this.mCriticalLevel) {
                f4 = 0.0f;
            }
            if (f4 == 1.0f) {
                f = this.mButtonFrame.top;
            } else {
                RectF rectF3 = this.mFrame;
                f = rectF3.top + (rectF3.height() * (1.0f - f4));
            }
            this.mShapePath.reset();
            this.mOutlinePath.reset();
            float radiusRatio = getRadiusRatio() * (this.mFrame.height() + f8);
            this.mShapePath.setFillType(FillType.WINDING);
            this.mShapePath.addRoundRect(this.mFrame, radiusRatio, radiusRatio, Direction.CW);
            this.mShapePath.addRect(this.mButtonFrame, Direction.CW);
            this.mOutlinePath.addRoundRect(this.mFrame, radiusRatio, radiusRatio, Direction.CW);
            Path path = new Path();
            path.addRect(this.mButtonFrame, Direction.CW);
            this.mOutlinePath.op(path, Op.XOR);
            boolean z = false;
            if (this.mPowerSaveEnabled) {
                float width = (this.mFrame.width() * 2.0f) / 3.0f;
                RectF rectF4 = this.mFrame;
                float width2 = rectF4.left + ((rectF4.width() - width) / 2.0f);
                RectF rectF5 = this.mFrame;
                float height = rectF5.top + ((rectF5.height() - width) / 2.0f);
                RectF rectF6 = this.mFrame;
                float width3 = rectF6.right - ((rectF6.width() - width) / 2.0f);
                RectF rectF7 = this.mFrame;
                float height2 = rectF7.bottom - ((rectF7.height() - width) / 2.0f);
                RectF rectF8 = this.mPlusFrame;
                if (!(rectF8.left == width2 && rectF8.top == height && rectF8.right == width3 && rectF8.bottom == height2)) {
                    this.mPlusFrame.set(width2, height, width3, height2);
                    this.mPlusPath.reset();
                    Path path2 = this.mPlusPath;
                    RectF rectF9 = this.mPlusFrame;
                    float width4 = rectF9.left + (this.mPlusPoints[0] * rectF9.width());
                    RectF rectF10 = this.mPlusFrame;
                    path2.moveTo(width4, rectF10.top + (this.mPlusPoints[1] * rectF10.height()));
                    int i7 = 2;
                    while (true) {
                        fArr = this.mPlusPoints;
                        if (i7 >= fArr.length) {
                            break;
                        }
                        Path path3 = this.mPlusPath;
                        RectF rectF11 = this.mPlusFrame;
                        float width5 = rectF11.left + (fArr[i7] * rectF11.width());
                        RectF rectF12 = this.mPlusFrame;
                        path3.lineTo(width5, rectF12.top + (this.mPlusPoints[i7 + 1] * rectF12.height()));
                        i7 += 2;
                    }
                    Path path4 = this.mPlusPath;
                    RectF rectF13 = this.mPlusFrame;
                    float width6 = rectF13.left + (fArr[0] * rectF13.width());
                    RectF rectF14 = this.mPlusFrame;
                    path4.lineTo(width6, rectF14.top + (this.mPlusPoints[1] * rectF14.height()));
                }
                this.mShapePath.op(this.mPlusPath, Op.DIFFERENCE);
                if (this.mPowerSaveAsColorError) {
                    canvas2.drawPath(this.mPlusPath, this.mPlusPaint);
                }
            }
            String str = null;
            if (this.mCharging || this.mPowerSaveEnabled || i <= this.mCriticalLevel || !this.mShowPercent) {
                f3 = 0.0f;
                f2 = 0.0f;
            } else {
                this.mTextPaint.setColor(getColorForLevel(i));
                this.mTextPaint.setTextSize(f5 * (this.mLevel == 100 ? 0.38f : 0.5f));
                this.mTextHeight = -this.mTextPaint.getFontMetrics().ascent;
                str = String.valueOf(i);
                f3 = (((float) this.mWidth) * 0.5f) + f6;
                f2 = ((((float) this.mHeight) + this.mTextHeight) * 0.47f) + ((float) i5);
                if (f > f2) {
                    z = true;
                }
                if (!z) {
                    this.mTextPath.reset();
                    this.mTextPaint.getTextPath(str, 0, str.length(), f3, f2, this.mTextPath);
                    this.mShapePath.op(this.mTextPath, Op.DIFFERENCE);
                }
            }
            canvas2.drawPath(this.mShapePath, this.mFramePaint);
            this.mFrame.top = f;
            canvas.save();
            canvas2.clipRect(this.mFrame);
            canvas2.drawPath(this.mShapePath, this.mBatteryPaint);
            canvas.restore();
            if (!this.mCharging && !this.mPowerSaveEnabled) {
                if (i <= this.mCriticalLevel) {
                    canvas2.drawText(this.mWarningString, (((float) this.mWidth) * 0.5f) + f6, ((((float) this.mHeight) + this.mWarningTextHeight) * 0.48f) + ((float) i5), this.mWarningTextPaint);
                } else if (z) {
                    canvas2.drawText(str, f3, f2, this.mTextPaint);
                }
            }
            if (!this.mCharging && this.mPowerSaveEnabled && this.mPowerSaveAsColorError) {
                canvas2.drawPath(this.mOutlinePath, this.mPowersavePaint);
            }
        }
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.mFramePaint.setColorFilter(colorFilter);
        this.mBatteryPaint.setColorFilter(colorFilter);
        this.mWarningTextPaint.setColorFilter(colorFilter);
        this.mBoltPaint.setColorFilter(colorFilter);
        this.mPlusPaint.setColorFilter(colorFilter);
    }

    public int getChargeColor() {
        return this.mChargeColor;
    }

    public void onOptimizatedStatusChange(boolean z) {
        this.mIsOptimizatedCharge = z;
        invalidateSelf();
    }
}

package com.oneplus.battery;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.android.settingslib.graph.BatteryMeterDrawableBase;
import com.android.systemui.R$color;
import com.android.systemui.R$drawable;
import com.oneplus.util.OpImageUtils;

public class OpBatteryMeterDrawable extends BatteryMeterDrawableBase {
    private Bitmap mBatterySaveOutline;
    private int mBatteryStyle = 0;
    private Bitmap mChargingOutlineBitmap;
    private Paint mCircleBackPaint = new Paint(1);
    private Paint mCircleChargingPaint;
    private Paint mCircleFrontPaint;
    private Paint mCirclePowerSavePaint;
    private final RectF mCircleRect = new RectF();
    private int mCircleSize;
    private int mHeight;
    private boolean mIsOptimizatedCharge = false;
    private int mLastHeight = 0;
    private int mLastWidth = 0;
    private int mMaskColor;
    private Drawable mMaskDrawable;
    private Bitmap mMaskOutlineBitmap;
    private Bitmap mOptimizatedChargeOutline;
    private Paint mOptimizatedChargePaint;
    private int mOutlineColor;
    private int mPowerSaveColor;
    private boolean mPowerSaveEnabled;
    private int mWidth;

    public OpBatteryMeterDrawable(Context context, int i) {
        super(context, i);
        this.mCircleBackPaint.setColor(i);
        this.mCircleBackPaint.setStrokeCap(Cap.BUTT);
        this.mCircleBackPaint.setDither(true);
        this.mCircleBackPaint.setStrokeWidth(0.0f);
        this.mCircleBackPaint.setStyle(Style.STROKE);
        this.mCircleFrontPaint = new Paint(1);
        this.mCircleFrontPaint.setStrokeCap(Cap.BUTT);
        this.mCircleFrontPaint.setDither(true);
        this.mCircleFrontPaint.setStrokeWidth(0.0f);
        this.mCircleFrontPaint.setStyle(Style.STROKE);
        this.mCircleChargingPaint = new Paint(1);
        this.mCircleChargingPaint.setStyle(Style.FILL);
        this.mCirclePowerSavePaint = new Paint(1);
        this.mCirclePowerSavePaint.setStrokeCap(Cap.BUTT);
        this.mCirclePowerSavePaint.setDither(true);
        this.mCirclePowerSavePaint.setStyle(Style.STROKE);
        this.mCircleFrontPaint.setStrokeWidth(0.0f);
        this.mBatterySaveOutline = OpImageUtils.drawableToBitmap(this.mContext.getDrawable(R$drawable.op_ic_battery_saver_outline));
        this.mPowerSaveColor = context.getColor(R$color.battery_power_save_color);
        this.mOptimizatedChargePaint = new Paint(1);
        this.mOptimizatedChargePaint.setColor(-16777216);
        this.mOptimizatedChargePaint.setStrokeCap(Cap.BUTT);
        this.mOptimizatedChargePaint.setDither(true);
        this.mOptimizatedChargePaint.setStrokeWidth(0.0f);
        this.mOptimizatedChargePaint.setStyle(Style.FILL_AND_STROKE);
        this.mOptimizatedChargePaint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
        this.mOptimizatedChargeOutline = OpImageUtils.drawableToBitmap(this.mContext.getDrawable(R$drawable.op_optimized_charging_outline));
        this.mChargingOutlineBitmap = OpImageUtils.drawableToBitmap(this.mContext.getDrawable(R$drawable.op_battery_charging_outline));
    }

    public int getIntrinsicWidth() {
        return this.mBatteryStyle == 1 ? super.getIntrinsicHeight() : super.getIntrinsicWidth();
    }

    private void postInvalidate(int i) {
        scheduleSelf(new Runnable() {
            public final void run() {
                OpBatteryMeterDrawable.this.invalidateSelf();
            }
        }, (long) i);
    }

    public void onBatteryStyleChanged(int i) {
        if (this.mBatteryStyle != i) {
            this.mBatteryStyle = i;
            updateViews();
        }
    }

    public void onOptimizatedStatusChange(boolean z) {
        if (this.mIsOptimizatedCharge != z) {
            super.onOptimizatedStatusChange(z);
            this.mIsOptimizatedCharge = z;
            String str = BatteryMeterDrawableBase.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onOptimizatedStatusChange isOptimizatedCharge:");
            sb.append(z);
            Log.i(str, sb.toString());
            updateViews();
        }
    }

    public void setBounds(int i, int i2, int i3, int i4) {
        super.setBounds(i, i2, i3, i4);
        this.mHeight = i4 - i2;
        this.mWidth = i3 - i;
        if (this.mLastHeight != this.mHeight || this.mLastWidth != this.mWidth) {
            this.mLastHeight = this.mHeight;
            this.mLastWidth = this.mWidth;
            postInvalidate(20);
        }
    }

    public void setColors(int i, int i2, int i3) {
        this.mOutlineColor = i3;
        this.mMaskColor = i;
        Drawable drawable = this.mMaskDrawable;
        if (drawable != null) {
            drawable.setTintMode(Mode.SRC_ATOP);
            this.mMaskDrawable.setTintList(ColorStateList.valueOf(this.mMaskColor));
        }
        this.mCirclePowerSavePaint.setColor(this.mMaskColor);
        if (this.mBatteryStyle == 0) {
            i2 = 0;
        }
        if (this.mPowerSaveEnabled) {
            int i4 = this.mBatteryStyle;
            if (i4 == 0) {
                i = this.mPowerSaveColor;
            } else if (i4 == 1) {
                i = this.mPowerSaveColor;
            }
        }
        setColors(i, i2);
    }

    public void setColors(int i, int i2) {
        this.mCircleBackPaint.setColor(i2);
        this.mCircleFrontPaint.setColor(i);
        this.mCircleChargingPaint.setColor(i);
        super.setColors(i, i2);
    }

    public void draw(Canvas canvas) {
        if (getBatteryLevel() != -1) {
            int i = this.mBatteryStyle;
            if (i != 1) {
                if (i != 2) {
                    super.draw(canvas);
                    Bitmap bitmap = this.mMaskOutlineBitmap;
                    if (bitmap != null && !bitmap.isRecycled()) {
                        canvas.drawBitmap(this.mMaskOutlineBitmap, null, new RectF(0.0f, 0.0f, (float) this.mWidth, (float) this.mHeight), this.mOptimizatedChargePaint);
                    }
                    Drawable drawable = this.mMaskDrawable;
                    if (drawable != null) {
                        drawable.setBounds(0, 0, this.mWidth, this.mHeight);
                        this.mMaskDrawable.draw(canvas);
                    }
                }
                return;
            }
            drawCircle(canvas, this.mWidth, this.mHeight);
        }
    }

    private void drawCircle(Canvas canvas, int i, int i2) {
        Canvas canvas2 = canvas;
        int i3 = i;
        int i4 = i2;
        initCircleSize(i3, i4);
        int batteryLevel = getBatteryLevel();
        boolean charging = getCharging();
        this.mCircleFrontPaint.setColor(charging ? getChargeColor() : getColorForLevel(batteryLevel));
        float f = ((float) batteryLevel) * 3.6f;
        Canvas canvas3 = canvas;
        canvas3.drawArc(this.mCircleRect, 270.0f, f - 360.0f, false, this.mCircleBackPaint);
        canvas3.drawArc(this.mCircleRect, 270.0f, f, false, this.mCircleFrontPaint);
        RectF rectF = this.mCircleRect;
        float f2 = (rectF.right - rectF.left) / 4.0f;
        canvas.save();
        if (charging) {
            this.mCircleChargingPaint.setColor(charging ? getChargeColor() : getColorForLevel(batteryLevel));
            if (isOptimizatedCharge()) {
                Drawable drawable = this.mMaskDrawable;
                if (drawable != null) {
                    drawable.setBounds(0, 0, this.mWidth, this.mHeight);
                    this.mMaskDrawable.draw(canvas);
                }
            } else {
                canvas.drawCircle(this.mCircleRect.centerX(), this.mCircleRect.centerY(), f2, this.mCircleChargingPaint);
            }
        } else if (this.mPowerSaveEnabled) {
            float f3 = (float) i3;
            float f4 = f3 * 0.34f;
            float strokeWidth = this.mCirclePowerSavePaint.getStrokeWidth();
            float f5 = (float) i4;
            float f6 = f5 / 2.0f;
            canvas.drawLine(f4, f6, f3 - f4, f6, this.mCirclePowerSavePaint);
            float f7 = (float) (i3 / 2);
            float f8 = (float) (i4 / 2);
            float f9 = strokeWidth / 2.0f;
            canvas.drawLine(f7, f4, f7, f8 - f9, this.mCirclePowerSavePaint);
            canvas.drawLine(f7, f8 + f9, f7, f5 - f4, this.mCirclePowerSavePaint);
        }
        canvas.restore();
    }

    private void initCircleSize(int i, int i2) {
        this.mCircleSize = Math.max(i, i2);
        float f = ((float) this.mCircleSize) / 6.5f;
        this.mCircleBackPaint.setStrokeWidth(f);
        this.mCircleFrontPaint.setStrokeWidth(f);
        this.mCirclePowerSavePaint.setStrokeWidth(0.5f * f);
        float f2 = f / 2.0f;
        int i3 = this.mCircleSize;
        this.mCircleRect.set(f2, f2, ((float) i3) - f2, ((float) i3) - f2);
    }

    private void setMaskDrawable(Drawable drawable) {
        setMaskDrawable(drawable, this.mMaskColor, Mode.SRC_ATOP);
    }

    private void setMaskDrawable(Drawable drawable, int i, Mode mode) {
        this.mMaskDrawable = drawable;
        Drawable drawable2 = this.mMaskDrawable;
        if (drawable2 != null) {
            drawable2.setTintMode(mode);
            this.mMaskDrawable.setTintList(ColorStateList.valueOf(i));
        }
    }

    public void setPowerSaveEnabled(boolean z) {
        if (this.mPowerSaveEnabled != z) {
            this.mPowerSaveEnabled = z;
            updateViews();
        }
    }

    private void updateViews() {
        if (this.mPowerSaveEnabled) {
            if (this.mBatteryStyle == 0) {
                setMaskDrawable(this.mContext.getDrawable(R$drawable.op_ic_battery_saver));
                this.mMaskOutlineBitmap = this.mBatterySaveOutline;
            }
        } else if (isOptimizatedCharge()) {
            int i = this.mBatteryStyle;
            if (i == 0) {
                setMaskDrawable(this.mContext.getDrawable(R$drawable.op_optimized_charging));
                this.mMaskOutlineBitmap = this.mOptimizatedChargeOutline;
            } else if (i == 1) {
                setMaskDrawable(this.mContext.getDrawable(R$drawable.op_circle_optimized_charging));
                this.mMaskOutlineBitmap = null;
            }
        } else if (this.mBatteryStyle != 0) {
            setMaskDrawable(null);
            this.mMaskOutlineBitmap = null;
        } else if (getCharging()) {
            setMaskDrawable(this.mContext.getDrawable(R$drawable.op_battery_charging_mask));
            this.mMaskOutlineBitmap = this.mChargingOutlineBitmap;
        } else {
            setMaskDrawable(this.mContext.getDrawable(R$drawable.op_battery_mask));
            this.mMaskOutlineBitmap = null;
        }
    }

    public void setCharging(boolean z) {
        super.setCharging(z);
        updateViews();
    }

    private boolean isOptimizatedCharge() {
        return getCharging() && this.mIsOptimizatedCharge;
    }
}

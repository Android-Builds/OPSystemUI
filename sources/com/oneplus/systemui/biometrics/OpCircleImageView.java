package com.oneplus.systemui.biometrics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.airbnb.lottie.C0526R;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;

public class OpCircleImageView extends ImageView {
    private final int[][] BRIGHTNESS_ALPHA_ARRAY = {new int[]{0, 255}, new int[]{1, 241}, new int[]{2, 236}, new int[]{4, 235}, new int[]{5, 234}, new int[]{6, 232}, new int[]{10, 228}, new int[]{20, 220}, new int[]{30, 212}, new int[]{45, 204}, new int[]{70, 190}, new int[]{100, 179}, new int[]{150, 166}, new int[]{227, 144}, new int[]{300, 131}, new int[]{400, C0526R.styleable.AppCompatTheme_toolbarNavigationButtonStyle}, new int[]{500, 96}, new int[]{600, 83}, new int[]{800, 60}, new int[]{1023, 34}, new int[]{2000, 131}};
    private final String TAG = "OpCircleImageView";
    private int TYPE_DIM = 2;
    private int TYPE_DISABLE = 0;
    private int TYPE_HIGH_LIGHT = 3;
    private int TYPE_NORMAL = 1;
    private Context mContext;
    private int mDefaultBacklight;
    private boolean mHasCustomizedHightlight;
    Paint mPaint;
    public PaintFlagsDrawFilter mPaintFlagsDrawFilter;
    Path mPath;
    private int mRadius;
    private int mType;

    private int interpolate(int i, int i2, int i3, int i4, int i5) {
        int i6 = i5 - i4;
        int i7 = i - i2;
        int i8 = ((i6 * 2) * i7) / (i3 - i2);
        int i9 = i8 / 2;
        int i10 = i2 - i3;
        return i4 + i9 + (i8 % 2) + ((i10 == 0 || i6 == 0) ? 0 : (((i7 * 2) * (i - i3)) / i6) / i10);
    }

    private int getDimAlpha() {
        int intForUser = System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", this.mDefaultBacklight, -2);
        int length = this.BRIGHTNESS_ALPHA_ARRAY.length;
        StringBuilder sb = new StringBuilder();
        sb.append("brightness = ");
        sb.append(intForUser);
        sb.append(", level = ");
        sb.append(length);
        Log.d("OpCircleImageView", sb.toString());
        int i = 0;
        while (i < length && this.BRIGHTNESS_ALPHA_ARRAY[i][0] < intForUser) {
            i++;
        }
        if (i == 0) {
            return this.BRIGHTNESS_ALPHA_ARRAY[0][1];
        }
        if (i == length) {
            return this.BRIGHTNESS_ALPHA_ARRAY[length - 1][1];
        }
        int[][] iArr = this.BRIGHTNESS_ALPHA_ARRAY;
        int i2 = i - 1;
        return interpolate(intForUser, iArr[i2][0], iArr[i][0], iArr[i2][1], iArr[i][1]);
    }

    public OpCircleImageView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    public OpCircleImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public OpCircleImageView(Context context) {
        super(context);
        init(context);
    }

    public void init(Context context) {
        this.mContext = context;
        int id = getId();
        if (id == R$id.op_fingerprint_icon_white) {
            this.mType = this.TYPE_HIGH_LIGHT;
        } else if (id == R$id.op_fingerprint_icon_disable) {
            this.mType = this.TYPE_DISABLE;
        } else if (id == R$id.op_fingerprint_icon_dim) {
            this.mType = this.TYPE_DIM;
        } else if (id == R$id.op_fingerprint_icon) {
            this.mType = this.TYPE_NORMAL;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("init view: ");
        sb.append(this.mType);
        Log.d("OpCircleImageView", sb.toString());
        int i = this.mType;
        if (i == this.TYPE_HIGH_LIGHT) {
            Drawable drawable = this.mContext.getResources().getDrawable(R$drawable.fod_flash_icon);
            this.mHasCustomizedHightlight = drawable.getMinimumHeight() != 0;
            setBackgroundDrawable(drawable);
            setScaleType(ScaleType.FIT_CENTER);
            this.mRadius = this.mContext.getResources().getDimensionPixelOffset(R$dimen.op_biometric_icon_flash_width);
        } else if (i == this.TYPE_DIM) {
            this.mRadius = this.mContext.getResources().getDimensionPixelOffset(R$dimen.op_biometric_icon_normal_width);
        }
        initPaint();
        this.mDefaultBacklight = ((PowerManager) context.getSystemService(PowerManager.class)).getDefaultScreenBrightnessSetting();
    }

    private void initPaint() {
        this.mPaintFlagsDrawFilter = new PaintFlagsDrawFilter(0, 3);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        if (this.mType == this.TYPE_HIGH_LIGHT && !this.mHasCustomizedHightlight) {
            this.mPaint.setColor(this.mContext.getResources().getColor(R$color.fingerprint_highlight_color));
        } else if (this.mType == this.TYPE_DIM) {
            this.mPaint.setColor(Color.parseColor("#000000"));
        }
        this.mPaint.setStyle(Style.FILL);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int i = this.mType;
        if (i != this.TYPE_HIGH_LIGHT && i != this.TYPE_DIM) {
            return;
        }
        if (this.mType != this.TYPE_HIGH_LIGHT || !this.mHasCustomizedHightlight) {
            float measuredHeight = (float) getMeasuredHeight();
            float measuredWidth = (float) getMeasuredWidth();
            if (this.mPath == null) {
                this.mPath = new Path();
                float f = measuredWidth / 2.0f;
                this.mPath.addCircle(f, measuredHeight / 2.0f, (float) Math.min((double) f, ((double) measuredHeight) / 2.0d), Direction.CCW);
                this.mPath.close();
            }
            if (this.mType == this.TYPE_DIM) {
                this.mPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            }
            canvas.drawCircle(measuredWidth / 2.0f, measuredHeight / 2.0f, (float) (this.mRadius / 2), this.mPaint);
        }
    }

    public void updateIconDim() {
        int dimAlpha = getDimAlpha();
        StringBuilder sb = new StringBuilder();
        sb.append("updateIconDim: ");
        sb.append(dimAlpha);
        String str = "OpCircleImageView";
        Log.d(str, sb.toString());
        float f = ((float) dimAlpha) / 255.0f;
        float f2 = ((float) SystemProperties.getInt("sys.fod.icon.dim", 70)) / 100.0f;
        StringBuilder sb2 = new StringBuilder();
        sb2.append("alpha = ");
        sb2.append(f);
        sb2.append(", ratio = ");
        sb2.append(f2);
        Log.d(str, sb2.toString());
        setAlpha(f * f2);
    }

    public void setVisibility(int i) {
        super.setVisibility(i);
        if (this.mType == this.TYPE_DIM) {
            updateIconDim();
        }
    }

    public void onBrightnessChange() {
        updateIconDim();
    }

    public void updateLayoutDimension(boolean z) {
        int i = this.mType;
        if (i == this.TYPE_HIGH_LIGHT) {
            this.mRadius = this.mContext.getResources().getDimensionPixelOffset(z ? R$dimen.op_biometric_icon_flash_width_2k : R$dimen.op_biometric_icon_flash_width_1080p);
        } else if (i == this.TYPE_DIM) {
            this.mRadius = this.mContext.getResources().getDimensionPixelOffset(z ? R$dimen.op_biometric_icon_normal_width_2k : R$dimen.op_biometric_icon_normal_width_1080p);
        }
    }
}

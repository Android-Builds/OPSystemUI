package com.android.systemui.p007qs;

import android.graphics.Path;
import android.util.Log;
import android.view.animation.BaseInterpolator;
import android.view.animation.Interpolator;

/* renamed from: com.android.systemui.qs.PathInterpolatorBuilder */
public class PathInterpolatorBuilder {
    private float[] mDist;

    /* renamed from: mX */
    private float[] f83mX;

    /* renamed from: mY */
    private float[] f84mY;

    /* renamed from: com.android.systemui.qs.PathInterpolatorBuilder$PathInterpolator */
    private static class PathInterpolator extends BaseInterpolator {

        /* renamed from: mX */
        private final float[] f85mX;

        /* renamed from: mY */
        private final float[] f86mY;

        private PathInterpolator(float[] fArr, float[] fArr2) {
            this.f85mX = fArr;
            this.f86mY = fArr2;
        }

        public float getInterpolation(float f) {
            if (f <= 0.0f) {
                return 0.0f;
            }
            if (f >= 1.0f) {
                return 1.0f;
            }
            int i = 0;
            int length = this.f85mX.length - 1;
            while (length - i > 1) {
                int i2 = (i + length) / 2;
                if (f < this.f85mX[i2]) {
                    length = i2;
                } else {
                    i = i2;
                }
            }
            float[] fArr = this.f85mX;
            float f2 = fArr[length] - fArr[i];
            if (f2 == 0.0f) {
                return this.f86mY[i];
            }
            float f3 = (f - fArr[i]) / f2;
            float[] fArr2 = this.f86mY;
            float f4 = fArr2[i];
            return f4 + (f3 * (fArr2[length] - f4));
        }
    }

    public PathInterpolatorBuilder(float f, float f2, float f3, float f4) {
        initCubic(f, f2, f3, f4);
    }

    private void initCubic(float f, float f2, float f3, float f4) {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(f, f2, f3, f4, 1.0f, 1.0f);
        initPath(path);
    }

    private void initPath(Path path) {
        float[] approximate = path.approximate(0.002f);
        int length = approximate.length / 3;
        if (approximate[1] == 0.0f && approximate[2] == 0.0f && approximate[approximate.length - 2] == 1.0f && approximate[approximate.length - 1] == 1.0f) {
            this.f83mX = new float[length];
            this.f84mY = new float[length];
            this.mDist = new float[length];
            int i = 0;
            float f = 0.0f;
            float f2 = 0.0f;
            int i2 = 0;
            while (i2 < length) {
                int i3 = i + 1;
                float f3 = approximate[i];
                int i4 = i3 + 1;
                float f4 = approximate[i3];
                int i5 = i4 + 1;
                float f5 = approximate[i4];
                if (f3 == f && f4 != f2) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("cannot have discontinuity in X axis: fraction=");
                    sb.append(f3);
                    sb.append(", x=");
                    sb.append(f4);
                    sb.append(", prevX=");
                    sb.append(f2);
                    Log.d("PathInterpolatorBuilder", sb.toString());
                    throw new IllegalArgumentException("The Path cannot have discontinuity in the X axis.");
                } else if (f4 >= f2) {
                    float[] fArr = this.f83mX;
                    fArr[i2] = f4;
                    float[] fArr2 = this.f84mY;
                    fArr2[i2] = f5;
                    if (i2 > 0) {
                        int i6 = i2 - 1;
                        float f6 = fArr[i2] - fArr[i6];
                        float f7 = fArr2[i2] - fArr2[i6];
                        float sqrt = (float) Math.sqrt((double) ((f6 * f6) + (f7 * f7)));
                        float[] fArr3 = this.mDist;
                        fArr3[i2] = fArr3[i6] + sqrt;
                    }
                    i2++;
                    f = f3;
                    f2 = f4;
                    i = i5;
                } else {
                    throw new IllegalArgumentException("The Path cannot loop back on itself.");
                }
            }
            float[] fArr4 = this.mDist;
            float f8 = fArr4[fArr4.length - 1];
            for (int i7 = 0; i7 < length; i7++) {
                float[] fArr5 = this.mDist;
                fArr5[i7] = fArr5[i7] / f8;
            }
            return;
        }
        throw new IllegalArgumentException("The Path must start at (0,0) and end at (1,1)");
    }

    public Interpolator getXInterpolator() {
        return new PathInterpolator(this.mDist, this.f83mX);
    }

    public Interpolator getYInterpolator() {
        return new PathInterpolator(this.mDist, this.f84mY);
    }
}

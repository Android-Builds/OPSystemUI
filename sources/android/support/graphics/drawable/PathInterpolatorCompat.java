package android.support.graphics.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.support.p000v4.content.res.TypedArrayUtils;
import android.support.p000v4.graphics.PathParser;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.animation.Interpolator;
import org.xmlpull.v1.XmlPullParser;

public class PathInterpolatorCompat implements Interpolator {

    /* renamed from: mX */
    private float[] f0mX;

    /* renamed from: mY */
    private float[] f1mY;

    public PathInterpolatorCompat(Context context, AttributeSet attributeSet, XmlPullParser xmlPullParser) {
        this(context.getResources(), context.getTheme(), attributeSet, xmlPullParser);
    }

    public PathInterpolatorCompat(Resources resources, Theme theme, AttributeSet attributeSet, XmlPullParser xmlPullParser) {
        TypedArray obtainAttributes = TypedArrayUtils.obtainAttributes(resources, theme, attributeSet, AndroidResources.STYLEABLE_PATH_INTERPOLATOR);
        parseInterpolatorFromTypeArray(obtainAttributes, xmlPullParser);
        obtainAttributes.recycle();
    }

    private void parseInterpolatorFromTypeArray(TypedArray typedArray, XmlPullParser xmlPullParser) {
        String str = "pathData";
        if (TypedArrayUtils.hasAttribute(xmlPullParser, str)) {
            String namedString = TypedArrayUtils.getNamedString(typedArray, xmlPullParser, str, 4);
            Path createPathFromPathData = PathParser.createPathFromPathData(namedString);
            if (createPathFromPathData != null) {
                initPath(createPathFromPathData);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("The path is null, which is created from ");
            sb.append(namedString);
            throw new InflateException(sb.toString());
        }
        String str2 = "controlX1";
        if (TypedArrayUtils.hasAttribute(xmlPullParser, str2)) {
            String str3 = "controlY1";
            if (TypedArrayUtils.hasAttribute(xmlPullParser, str3)) {
                float namedFloat = TypedArrayUtils.getNamedFloat(typedArray, xmlPullParser, str2, 0, 0.0f);
                float namedFloat2 = TypedArrayUtils.getNamedFloat(typedArray, xmlPullParser, str3, 1, 0.0f);
                String str4 = "controlX2";
                boolean hasAttribute = TypedArrayUtils.hasAttribute(xmlPullParser, str4);
                String str5 = "controlY2";
                if (hasAttribute != TypedArrayUtils.hasAttribute(xmlPullParser, str5)) {
                    throw new InflateException("pathInterpolator requires both controlX2 and controlY2 for cubic Beziers.");
                } else if (!hasAttribute) {
                    initQuad(namedFloat, namedFloat2);
                } else {
                    initCubic(namedFloat, namedFloat2, TypedArrayUtils.getNamedFloat(typedArray, xmlPullParser, str4, 2, 0.0f), TypedArrayUtils.getNamedFloat(typedArray, xmlPullParser, str5, 3, 0.0f));
                }
            } else {
                throw new InflateException("pathInterpolator requires the controlY1 attribute");
            }
        } else {
            throw new InflateException("pathInterpolator requires the controlX1 attribute");
        }
    }

    private void initQuad(float f, float f2) {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.quadTo(f, f2, 1.0f, 1.0f);
        initPath(path);
    }

    private void initCubic(float f, float f2, float f3, float f4) {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(f, f2, f3, f4, 1.0f, 1.0f);
        initPath(path);
    }

    private void initPath(Path path) {
        int i = 0;
        PathMeasure pathMeasure = new PathMeasure(path, false);
        float length = pathMeasure.getLength();
        int min = Math.min(3000, ((int) (length / 0.002f)) + 1);
        if (min > 0) {
            this.f0mX = new float[min];
            this.f1mY = new float[min];
            float[] fArr = new float[2];
            for (int i2 = 0; i2 < min; i2++) {
                pathMeasure.getPosTan((((float) i2) * length) / ((float) (min - 1)), fArr, null);
                this.f0mX[i2] = fArr[0];
                this.f1mY[i2] = fArr[1];
            }
            if (((double) Math.abs(this.f0mX[0])) <= 1.0E-5d && ((double) Math.abs(this.f1mY[0])) <= 1.0E-5d) {
                int i3 = min - 1;
                if (((double) Math.abs(this.f0mX[i3] - 1.0f)) <= 1.0E-5d && ((double) Math.abs(this.f1mY[i3] - 1.0f)) <= 1.0E-5d) {
                    float f = 0.0f;
                    int i4 = 0;
                    while (i < min) {
                        float[] fArr2 = this.f0mX;
                        int i5 = i4 + 1;
                        float f2 = fArr2[i4];
                        if (f2 >= f) {
                            fArr2[i] = f2;
                            i++;
                            f = f2;
                            i4 = i5;
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append("The Path cannot loop back on itself, x :");
                            sb.append(f2);
                            throw new IllegalArgumentException(sb.toString());
                        }
                    }
                    if (pathMeasure.nextContour()) {
                        throw new IllegalArgumentException("The Path should be continuous, can't have 2+ contours");
                    }
                    return;
                }
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("The Path must start at (0,0) and end at (1,1) start: ");
            sb2.append(this.f0mX[0]);
            String str = ",";
            sb2.append(str);
            sb2.append(this.f1mY[0]);
            sb2.append(" end:");
            int i6 = min - 1;
            sb2.append(this.f0mX[i6]);
            sb2.append(str);
            sb2.append(this.f1mY[i6]);
            throw new IllegalArgumentException(sb2.toString());
        }
        StringBuilder sb3 = new StringBuilder();
        sb3.append("The Path has a invalid length ");
        sb3.append(length);
        throw new IllegalArgumentException(sb3.toString());
    }

    public float getInterpolation(float f) {
        if (f <= 0.0f) {
            return 0.0f;
        }
        if (f >= 1.0f) {
            return 1.0f;
        }
        int i = 0;
        int length = this.f0mX.length - 1;
        while (length - i > 1) {
            int i2 = (i + length) / 2;
            if (f < this.f0mX[i2]) {
                length = i2;
            } else {
                i = i2;
            }
        }
        float[] fArr = this.f0mX;
        float f2 = fArr[length] - fArr[i];
        if (f2 == 0.0f) {
            return this.f1mY[i];
        }
        float f3 = (f - fArr[i]) / f2;
        float[] fArr2 = this.f1mY;
        float f4 = fArr2[i];
        return f4 + (f3 * (fArr2[length] - f4));
    }
}

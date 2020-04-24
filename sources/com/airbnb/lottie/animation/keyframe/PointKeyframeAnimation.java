package com.airbnb.lottie.animation.keyframe;

import android.graphics.PointF;
import com.airbnb.lottie.value.Keyframe;
import com.airbnb.lottie.value.LottieValueCallback;
import java.util.List;

public class PointKeyframeAnimation extends KeyframeAnimation<PointF> {
    private final PointF point = new PointF();

    public PointKeyframeAnimation(List<Keyframe<PointF>> list) {
        super(list);
    }

    public PointF getValue(Keyframe<PointF> keyframe, float f) {
        T t = keyframe.startValue;
        if (t != null) {
            T t2 = keyframe.endValue;
            if (t2 != null) {
                PointF pointF = (PointF) t;
                PointF pointF2 = (PointF) t2;
                LottieValueCallback<A> lottieValueCallback = this.valueCallback;
                if (lottieValueCallback != null) {
                    PointF pointF3 = (PointF) lottieValueCallback.getValueInternal(keyframe.startFrame, keyframe.endFrame.floatValue(), pointF, pointF2, f, getLinearCurrentKeyframeProgress(), getProgress());
                    if (pointF3 != null) {
                        return pointF3;
                    }
                }
                PointF pointF4 = this.point;
                float f2 = pointF.x;
                float f3 = f2 + ((pointF2.x - f2) * f);
                float f4 = pointF.y;
                pointF4.set(f3, f4 + (f * (pointF2.y - f4)));
                return this.point;
            }
        }
        throw new IllegalStateException("Missing values for keyframe.");
    }
}

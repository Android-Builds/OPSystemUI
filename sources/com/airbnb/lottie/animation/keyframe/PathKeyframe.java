package com.airbnb.lottie.animation.keyframe;

import android.graphics.Path;
import android.graphics.PointF;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.Keyframe;

public class PathKeyframe extends Keyframe<PointF> {
    private Path path;
    private final Keyframe<PointF> pointKeyFrame;

    public PathKeyframe(LottieComposition lottieComposition, Keyframe<PointF> keyframe) {
        super(lottieComposition, keyframe.startValue, keyframe.endValue, keyframe.interpolator, keyframe.startFrame, keyframe.endFrame);
        this.pointKeyFrame = keyframe;
        createPath();
    }

    public void createPath() {
        boolean z;
        T t;
        T t2 = this.endValue;
        if (t2 != null) {
            T t3 = this.startValue;
            if (t3 != null && ((PointF) t3).equals(((PointF) t2).x, ((PointF) t2).y)) {
                z = true;
                t = this.endValue;
                if (t != null && !z) {
                    PointF pointF = (PointF) this.startValue;
                    PointF pointF2 = (PointF) t;
                    Keyframe<PointF> keyframe = this.pointKeyFrame;
                    this.path = Utils.createPath(pointF, pointF2, keyframe.pathCp1, keyframe.pathCp2);
                    return;
                }
            }
        }
        z = false;
        t = this.endValue;
        if (t != null) {
        }
    }

    /* access modifiers changed from: 0000 */
    public Path getPath() {
        return this.path;
    }
}

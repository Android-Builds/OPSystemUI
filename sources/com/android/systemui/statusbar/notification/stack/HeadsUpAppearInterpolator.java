package com.android.systemui.statusbar.notification.stack;

import android.graphics.Path;
import android.view.animation.PathInterpolator;

public class HeadsUpAppearInterpolator extends PathInterpolator {

    /* renamed from: X1 */
    private static float f93X1 = 250.0f;

    /* renamed from: X2 */
    private static float f94X2 = 200.0f;
    private static float XTOT = (f93X1 + f94X2);

    public HeadsUpAppearInterpolator() {
        super(getAppearPath());
    }

    private static Path getAppearPath() {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        float f = f93X1;
        float f2 = f * 0.8f;
        float f3 = XTOT;
        path.cubicTo(f2 / f3, 1.125f, (0.8f * f) / f3, 1.125f, f / f3, 1.125f);
        float f4 = f93X1;
        float f5 = f94X2;
        float f6 = (0.4f * f5) + f4;
        float f7 = XTOT;
        path.cubicTo(f6 / f7, 1.125f, (f4 + (f5 * 0.2f)) / f7, 1.0f, 1.0f, 1.0f);
        return path;
    }

    public static float getFractionUntilOvershoot() {
        return f93X1 / XTOT;
    }
}

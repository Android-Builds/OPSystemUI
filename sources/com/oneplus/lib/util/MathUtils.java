package com.oneplus.lib.util;

import java.util.Random;

public final class MathUtils {
    private static final Random sRandom = new Random();

    public static float constrain(float f, float f2, float f3) {
        return f < f2 ? f2 : f > f3 ? f3 : f;
    }

    public static int constrain(int i, int i2, int i3) {
        return i < i2 ? i2 : i > i3 ? i3 : i;
    }

    public static long constrain(long j, long j2, long j3) {
        return j < j2 ? j2 : j > j3 ? j3 : j;
    }

    public static float lerp(float f, float f2, float f3) {
        return f + ((f2 - f) * f3);
    }

    public static float lerpDeg(float f, float f2, float f3) {
        return (((((f2 - f) + 180.0f) % 360.0f) - 180.0f) * f3) + f;
    }
}

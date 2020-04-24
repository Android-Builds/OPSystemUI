package com.oneplus.lib.util;

public final class ColorUtils {
    private static final ThreadLocal<double[]> TEMP_ARRAY = new ThreadLocal<>();

    public static int setAlphaComponent(int i, int i2) {
        if (i2 >= 0 && i2 <= 255) {
            return (i & 16777215) | (i2 << 24);
        }
        throw new IllegalArgumentException("alpha must be between 0 and 255.");
    }
}

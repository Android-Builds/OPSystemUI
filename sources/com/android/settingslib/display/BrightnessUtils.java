package com.android.settingslib.display;

import android.util.MathUtils;

public class BrightnessUtils {
    public static final int convertGammaToLinear(int i, int i2, int i3) {
        float f;
        float f2;
        if (i <= 512) {
            f = MathUtils.norm(0.0f, 590.0f, (float) i);
        } else {
            f = MathUtils.norm(0.0f, 1023.0f, (float) ((((i - 512) * 133) / 511) + 890));
        }
        if (f <= 0.5f) {
            f2 = MathUtils.sq(f / 0.5f);
        } else {
            f2 = MathUtils.exp((f - 0.5599107f) / 0.17883277f) + 0.28466892f;
        }
        return Math.round(MathUtils.lerp((float) i2, (float) i3, f2 / 12.0f));
    }

    public static final int convertLinearToGamma(int i, int i2, int i3) {
        float f;
        float norm = MathUtils.norm((float) i2, (float) i3, (float) i) * 12.0f;
        if (norm <= 1.0f) {
            f = MathUtils.sqrt(norm) * 0.5f;
        } else {
            f = (MathUtils.log(norm - 0.28466892f) * 0.17883277f) + 0.5599107f;
        }
        int round = Math.round(MathUtils.lerp(0.0f, 1023.0f, f));
        if (f <= 0.8699902f) {
            return Math.round((float) ((round * 512) / 890));
        }
        return Math.round((float) ((((round - 890) * 511) / 133) + 512));
    }
}

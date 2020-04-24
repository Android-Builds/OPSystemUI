package com.android.settingslib.animation;

import android.content.Context;
import android.view.animation.Interpolator;
import com.android.settingslib.animation.AppearAnimationUtils.RowTranslationScaler;

public class DisappearAnimationUtils extends AppearAnimationUtils {
    private static final RowTranslationScaler ROW_TRANSLATION_SCALER = new RowTranslationScaler() {
        public float getRowTranslationScale(int i, int i2) {
            return (float) (Math.pow((double) (i2 - i), 2.0d) / ((double) i2));
        }
    };

    public DisappearAnimationUtils(Context context, long j, float f, float f2, Interpolator interpolator) {
        this(context, j, f, f2, interpolator, ROW_TRANSLATION_SCALER);
    }

    public DisappearAnimationUtils(Context context, long j, float f, float f2, Interpolator interpolator, RowTranslationScaler rowTranslationScaler) {
        super(context, j, f, f2, interpolator);
        this.mRowTranslationScaler = rowTranslationScaler;
        this.mAppearing = false;
    }

    /* access modifiers changed from: protected */
    public long calculateDelay(int i, int i2) {
        return (long) ((((double) (i * 60)) + (((double) i2) * (Math.pow((double) i, 0.4d) + 0.4d) * 10.0d)) * ((double) this.mDelayScale));
    }
}

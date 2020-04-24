package com.android.settingslib.core.instrumentation;

import android.os.SystemClock;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public class VisibilityLoggerMixin implements LifecycleObserver {
    private final int mMetricsCategory;
    private MetricsFeatureProvider mMetricsFeature;
    private int mSourceMetricsCategory;
    private long mVisibleTimestamp;

    @OnLifecycleEvent(Event.ON_RESUME)
    public void onResume() {
        this.mVisibleTimestamp = SystemClock.elapsedRealtime();
        MetricsFeatureProvider metricsFeatureProvider = this.mMetricsFeature;
        if (metricsFeatureProvider != null) {
            int i = this.mMetricsCategory;
            if (i != 0) {
                metricsFeatureProvider.visible(null, this.mSourceMetricsCategory, i);
                throw null;
            }
        }
    }

    @OnLifecycleEvent(Event.ON_PAUSE)
    public void onPause() {
        this.mVisibleTimestamp = 0;
        MetricsFeatureProvider metricsFeatureProvider = this.mMetricsFeature;
        if (metricsFeatureProvider != null) {
            int i = this.mMetricsCategory;
            if (i != 0) {
                metricsFeatureProvider.hidden(null, i);
                throw null;
            }
        }
    }
}

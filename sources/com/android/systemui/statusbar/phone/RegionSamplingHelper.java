package com.android.systemui.statusbar.phone;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.view.CompositionSamplingListener;
import android.view.SurfaceControl;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver.OnDrawListener;
import com.android.systemui.R$dimen;

public class RegionSamplingHelper implements OnAttachStateChangeListener, OnLayoutChangeListener {
    private final SamplingCallback mCallback;
    private float mCurrentMedianLuma;
    private boolean mFirstSamplingAfterStart;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler();
    private float mLastMedianLuma;
    private final float mLuminanceChangeThreshold;
    private final float mLuminanceThreshold;
    private final Rect mRegisteredSamplingBounds = new Rect();
    private SurfaceControl mRegisteredStopLayer = null;
    /* access modifiers changed from: private */
    public Runnable mRemoveDrawRunnable = new Runnable() {
        public void run() {
            RegionSamplingHelper.this.mSampledView.getViewTreeObserver().removeOnDrawListener(RegionSamplingHelper.this.mUpdateOnDraw);
        }
    };
    /* access modifiers changed from: private */
    public final View mSampledView;
    /* access modifiers changed from: private */
    public boolean mSamplingEnabled = false;
    private final CompositionSamplingListener mSamplingListener;
    private boolean mSamplingListenerRegistered = false;
    private final Rect mSamplingRequestBounds = new Rect();
    /* access modifiers changed from: private */
    public OnDrawListener mUpdateOnDraw = new OnDrawListener() {
        public void onDraw() {
            RegionSamplingHelper.this.mHandler.post(RegionSamplingHelper.this.mRemoveDrawRunnable);
            RegionSamplingHelper.this.onDraw();
        }
    };
    private final Runnable mUpdateSamplingListener = new Runnable() {
        public final void run() {
            RegionSamplingHelper.this.updateSamplingListener();
        }
    };
    private boolean mWaitingOnDraw;

    public interface SamplingCallback {
        Rect getSampledRegion(View view);

        boolean isSamplingEnabled() {
            return true;
        }

        void onRegionDarknessChanged(boolean z);
    }

    public RegionSamplingHelper(View view, SamplingCallback samplingCallback) {
        this.mSamplingListener = new CompositionSamplingListener(view.getContext().getMainExecutor()) {
            public void onSampleCollected(float f) {
                if (RegionSamplingHelper.this.mSamplingEnabled) {
                    RegionSamplingHelper.this.updateMediaLuma(f);
                }
            }
        };
        this.mSampledView = view;
        this.mSampledView.addOnAttachStateChangeListener(this);
        this.mSampledView.addOnLayoutChangeListener(this);
        Resources resources = view.getResources();
        this.mLuminanceThreshold = resources.getFloat(R$dimen.navigation_luminance_threshold);
        this.mLuminanceChangeThreshold = resources.getFloat(R$dimen.navigation_luminance_change_threshold);
        this.mCallback = samplingCallback;
    }

    /* access modifiers changed from: private */
    public void onDraw() {
        if (this.mWaitingOnDraw) {
            this.mWaitingOnDraw = false;
            updateSamplingListener();
        }
    }

    /* access modifiers changed from: 0000 */
    public void start(Rect rect) {
        if (this.mCallback.isSamplingEnabled()) {
            if (rect != null) {
                this.mSamplingRequestBounds.set(rect);
            }
            this.mSamplingEnabled = true;
            this.mLastMedianLuma = -1.0f;
            this.mFirstSamplingAfterStart = true;
            updateSamplingListener();
        }
    }

    /* access modifiers changed from: 0000 */
    public void stop() {
        this.mSamplingEnabled = false;
        updateSamplingListener();
    }

    public void onViewAttachedToWindow(View view) {
        updateSamplingListener();
    }

    public void onViewDetachedFromWindow(View view) {
        postUpdateSamplingListener();
    }

    public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        updateSamplingRect();
    }

    private void postUpdateSamplingListener() {
        this.mHandler.removeCallbacks(this.mUpdateSamplingListener);
        this.mHandler.post(this.mUpdateSamplingListener);
    }

    /* access modifiers changed from: private */
    public void updateSamplingListener() {
        if (this.mSamplingEnabled && !this.mSamplingRequestBounds.isEmpty() && (this.mSampledView.isAttachedToWindow() || this.mFirstSamplingAfterStart)) {
            ViewRootImpl viewRootImpl = this.mSampledView.getViewRootImpl();
            IBinder iBinder = null;
            SurfaceControl surfaceControl = viewRootImpl != null ? viewRootImpl.getSurfaceControl() : null;
            if (surfaceControl == null || !surfaceControl.isValid()) {
                if (!this.mWaitingOnDraw) {
                    this.mWaitingOnDraw = true;
                    if (this.mHandler.hasCallbacks(this.mRemoveDrawRunnable)) {
                        this.mHandler.removeCallbacks(this.mRemoveDrawRunnable);
                    } else {
                        this.mSampledView.getViewTreeObserver().addOnDrawListener(this.mUpdateOnDraw);
                    }
                }
                surfaceControl = null;
            }
            if (!this.mSamplingRequestBounds.equals(this.mRegisteredSamplingBounds) || this.mRegisteredStopLayer != surfaceControl) {
                unregisterSamplingListener();
                this.mSamplingListenerRegistered = true;
                CompositionSamplingListener compositionSamplingListener = this.mSamplingListener;
                if (surfaceControl != null) {
                    iBinder = surfaceControl.getHandle();
                }
                CompositionSamplingListener.register(compositionSamplingListener, 0, iBinder, this.mSamplingRequestBounds);
                this.mRegisteredSamplingBounds.set(this.mSamplingRequestBounds);
                this.mRegisteredStopLayer = surfaceControl;
            }
            this.mFirstSamplingAfterStart = false;
            return;
        }
        unregisterSamplingListener();
    }

    private void unregisterSamplingListener() {
        if (this.mSamplingListenerRegistered) {
            this.mSamplingListenerRegistered = false;
            this.mRegisteredStopLayer = null;
            this.mRegisteredSamplingBounds.setEmpty();
            CompositionSamplingListener.unregister(this.mSamplingListener);
        }
    }

    /* access modifiers changed from: private */
    public void updateMediaLuma(float f) {
        this.mCurrentMedianLuma = f;
        if (Math.abs(this.mCurrentMedianLuma - this.mLastMedianLuma) > this.mLuminanceChangeThreshold) {
            this.mCallback.onRegionDarknessChanged(f < this.mLuminanceThreshold);
            this.mLastMedianLuma = f;
        }
    }

    public void updateSamplingRect() {
        Rect sampledRegion = this.mCallback.getSampledRegion(this.mSampledView);
        if (!this.mSamplingRequestBounds.equals(sampledRegion)) {
            this.mSamplingRequestBounds.set(sampledRegion);
            updateSamplingListener();
        }
    }
}

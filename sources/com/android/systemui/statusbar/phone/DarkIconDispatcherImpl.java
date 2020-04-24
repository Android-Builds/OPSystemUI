package com.android.systemui.statusbar.phone;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.R$color;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.phone.LightBarTransitionsController.DarkIntensityApplier;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DarkIconDispatcherImpl implements SysuiDarkIconDispatcher, DarkIntensityApplier {
    private final int DELAY_INTERVAL = 100;
    private final int MAX_RETRY_TIMES = 2;
    private final String TAG = DarkIconDispatcherImpl.class.getSimpleName();
    private float mDarkIntensity;
    private int mDarkModeIconColorSingleTone;
    private final Handler mHandler;
    private int mIconTint = -1;
    private int mLightModeIconColorSingleTone;
    private Runnable mReApplyIconTint = new Runnable() {
        public void run() {
            DarkIconDispatcherImpl.this.mSetTintRetryTimes = DarkIconDispatcherImpl.this.mSetTintRetryTimes + 1;
            DarkIconDispatcherImpl.this.applyIconTint();
        }
    };
    private final ArrayMap<Object, DarkReceiver> mReceivers = new ArrayMap<>();
    /* access modifiers changed from: private */
    public int mSetTintRetryTimes = 0;
    private final Rect mTintArea = new Rect();
    private View mTraceView;
    private final LightBarTransitionsController mTransitionsController;

    public int getTintAnimationDuration() {
        return 120;
    }

    public DarkIconDispatcherImpl(Context context) {
        this.mDarkModeIconColorSingleTone = context.getColor(R$color.dark_mode_icon_color_single_tone);
        this.mLightModeIconColorSingleTone = context.getColor(R$color.light_mode_icon_color_single_tone);
        this.mTransitionsController = new LightBarTransitionsController(context, this);
        this.mHandler = new Handler();
    }

    public LightBarTransitionsController getTransitionsController() {
        return this.mTransitionsController;
    }

    public void addDarkReceiver(DarkReceiver darkReceiver) {
        this.mReceivers.put(darkReceiver, darkReceiver);
        darkReceiver.onDarkChanged(this.mTintArea, this.mDarkIntensity, this.mIconTint);
    }

    public void addDarkReceiver(ImageView imageView) {
        $$Lambda$DarkIconDispatcherImpl$ok51JmL9mmr4FNW4V8J0PDfHR6I r0 = new DarkReceiver(imageView) {
            private final /* synthetic */ ImageView f$1;

            {
                this.f$1 = r2;
            }

            public final void onDarkChanged(Rect rect, float f, int i) {
                DarkIconDispatcherImpl.this.lambda$addDarkReceiver$0$DarkIconDispatcherImpl(this.f$1, rect, f, i);
            }
        };
        this.mReceivers.put(imageView, r0);
        r0.onDarkChanged(this.mTintArea, this.mDarkIntensity, this.mIconTint);
    }

    public /* synthetic */ void lambda$addDarkReceiver$0$DarkIconDispatcherImpl(ImageView imageView, Rect rect, float f, int i) {
        imageView.setImageTintList(ColorStateList.valueOf(DarkIconDispatcher.getTint(this.mTintArea, imageView, this.mIconTint)));
    }

    public void removeDarkReceiver(DarkReceiver darkReceiver) {
        this.mReceivers.remove(darkReceiver);
    }

    public void removeDarkReceiver(ImageView imageView) {
        this.mReceivers.remove(imageView);
    }

    public void applyDark(DarkReceiver darkReceiver) {
        ((DarkReceiver) this.mReceivers.get(darkReceiver)).onDarkChanged(this.mTintArea, this.mDarkIntensity, this.mIconTint);
    }

    public void setIconsDarkArea(Rect rect) {
        if (rect != null || !this.mTintArea.isEmpty()) {
            if (rect == null) {
                this.mTintArea.setEmpty();
            } else {
                this.mTintArea.set(rect);
            }
            applyIconTint();
        }
    }

    public void applyDarkIntensity(float f) {
        this.mDarkIntensity = f;
        this.mIconTint = ((Integer) ArgbEvaluator.getInstance().evaluate(f, Integer.valueOf(this.mLightModeIconColorSingleTone), Integer.valueOf(this.mDarkModeIconColorSingleTone))).intValue();
        applyIconTint();
    }

    public void applyIconTint() {
        boolean z = this.mSetTintRetryTimes >= 2;
        View view = this.mTraceView;
        if ((view == null || view.isLayoutRequested()) && !z) {
            this.mHandler.postDelayed(this.mReApplyIconTint, 100);
            return;
        }
        this.mHandler.removeCallbacks(this.mReApplyIconTint);
        for (int i = 0; i < this.mReceivers.size(); i++) {
            ((DarkReceiver) this.mReceivers.valueAt(i)).onDarkChanged(this.mTintArea, this.mDarkIntensity, this.mIconTint);
        }
        if (z) {
            String str = this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("apply tint time-out after retrying ");
            sb.append(this.mSetTintRetryTimes);
            sb.append(" times");
            Log.d(str, sb.toString());
        }
        this.mSetTintRetryTimes = 0;
    }

    public void setTraceView(View view) {
        this.mTraceView = view;
    }

    public View getTraceView() {
        return this.mTraceView;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("DarkIconDispatcher: ");
        StringBuilder sb = new StringBuilder();
        sb.append("  mIconTint: 0x");
        sb.append(Integer.toHexString(this.mIconTint));
        printWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("  mDarkIntensity: ");
        sb2.append(this.mDarkIntensity);
        sb2.append("f");
        printWriter.println(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append("  mTintArea: ");
        sb3.append(this.mTintArea);
        printWriter.println(sb3.toString());
    }
}

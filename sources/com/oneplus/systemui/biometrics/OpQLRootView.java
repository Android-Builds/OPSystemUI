package com.oneplus.systemui.biometrics;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.systemui.Interpolators;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$style;

public class OpQLRootView extends FrameLayout {
    private int TRACK_INTERVAL;
    private TextView mHint;
    private boolean mIsCancel;
    private TextView mLabel;
    private View mQLCancelView;
    private OpQLRecyclerView mQLRecyclerView;
    private View mQLTrackView;
    private View mQLTrackViewBackground;
    private int mTrackViewPadding;
    private int mTrackViewWidth;
    private VelocityTracker mVelocityTracker;

    public OpQLRootView(Context context) {
        this(context, null);
    }

    public OpQLRootView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.TRACK_INTERVAL = 0;
        this.mIsCancel = false;
        this.mVelocityTracker = null;
        this.mTrackViewPadding = 0;
        this.mTrackViewWidth = 0;
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mQLRecyclerView = (OpQLRecyclerView) findViewById(R$id.icon_scroller);
        this.mQLCancelView = findViewById(R$id.cancel_area);
        this.mQLTrackView = findViewById(R$id.track_view);
        this.mQLTrackViewBackground = findViewById(R$id.track_view_background);
        updateTrackBarVisibility();
        this.mLabel = (TextView) findViewById(R$id.label);
        this.mHint = (TextView) findViewById(R$id.hint);
        this.mQLRecyclerView.setLabelView(this.mLabel);
        this.mTrackViewPadding = this.mContext.getResources().getDimensionPixelOffset(R$dimen.op_quick_launch_track_view_padding_side) - 75;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mContext.getDisplay().getMetrics(displayMetrics);
        this.mTrackViewWidth = displayMetrics.widthPixels - (this.mTrackViewPadding * 2);
        this.TRACK_INTERVAL = this.mTrackViewWidth / 7;
        setSystemUiVisibility(1792);
    }

    public void onQLExit() {
        this.mQLRecyclerView.onQLExit();
        this.mQLRecyclerView = null;
        this.mContext = null;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x001d, code lost:
        if (r0 != 3) goto L_0x0044;
     */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x0052  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0064  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00ad  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onTouch(android.view.MotionEvent r7) {
        /*
            r6 = this;
            int r0 = r6.getItemCount()
            if (r0 != 0) goto L_0x0007
            return
        L_0x0007:
            android.view.VelocityTracker r0 = r6.mVelocityTracker
            if (r0 != 0) goto L_0x0011
            android.view.VelocityTracker r0 = android.view.VelocityTracker.obtain()
            r6.mVelocityTracker = r0
        L_0x0011:
            int r0 = r7.getAction()
            r1 = 2
            r2 = 1
            r3 = 0
            if (r0 == r2) goto L_0x003c
            if (r0 == r1) goto L_0x0020
            r4 = 3
            if (r0 == r4) goto L_0x003c
            goto L_0x0044
        L_0x0020:
            android.view.VelocityTracker r0 = r6.mVelocityTracker
            r0.addMovement(r7)
            android.view.VelocityTracker r0 = r6.mVelocityTracker
            r4 = 1000(0x3e8, float:1.401E-42)
            r0.computeCurrentVelocity(r4)
            android.view.VelocityTracker r0 = r6.mVelocityTracker
            float r0 = r0.getXVelocity()
            float r0 = java.lang.Math.abs(r0)
            com.oneplus.systemui.biometrics.OpQLRecyclerView r4 = r6.mQLRecyclerView
            r4.onVelocityChanged(r0)
            goto L_0x0045
        L_0x003c:
            android.view.VelocityTracker r0 = r6.mVelocityTracker
            r0.recycle()
            r0 = 0
            r6.mVelocityTracker = r0
        L_0x0044:
            r0 = r3
        L_0x0045:
            float r4 = r7.getX()
            int r5 = r6.mTrackViewPadding
            float r5 = (float) r5
            float r4 = r4 - r5
            int r5 = (r4 > r3 ? 1 : (r4 == r3 ? 0 : -1))
            if (r5 >= 0) goto L_0x0052
            goto L_0x005c
        L_0x0052:
            int r3 = r6.mTrackViewWidth
            float r5 = (float) r3
            int r5 = (r4 > r5 ? 1 : (r4 == r5 ? 0 : -1))
            if (r5 <= 0) goto L_0x005b
            float r3 = (float) r3
            goto L_0x005c
        L_0x005b:
            r3 = r4
        L_0x005c:
            int r4 = r6.TRACK_INTERVAL
            float r4 = (float) r4
            float r3 = r3 / r4
            android.view.View r4 = r6.mQLCancelView
            if (r4 == 0) goto L_0x00a9
            float r4 = r7.getRawY()
            android.view.View r5 = r6.mQLCancelView
            int r5 = r5.getTop()
            float r5 = (float) r5
            int r4 = (r4 > r5 ? 1 : (r4 == r5 ? 0 : -1))
            if (r4 <= 0) goto L_0x0095
            float r4 = r7.getRawX()
            android.view.View r5 = r6.mQLCancelView
            int r5 = r5.getLeft()
            float r5 = (float) r5
            int r4 = (r4 > r5 ? 1 : (r4 == r5 ? 0 : -1))
            if (r4 <= 0) goto L_0x0095
            float r4 = r7.getRawX()
            android.view.View r5 = r6.mQLCancelView
            int r5 = r5.getRight()
            float r5 = (float) r5
            int r4 = (r4 > r5 ? 1 : (r4 == r5 ? 0 : -1))
            if (r4 >= 0) goto L_0x0095
            r6.onEnterCancelView()
            goto L_0x00a9
        L_0x0095:
            float r4 = r7.getRawY()
            android.view.View r5 = r6.mQLTrackView
            int r5 = r5.getBottom()
            int r5 = r5 + 100
            float r5 = (float) r5
            int r4 = (r4 > r5 ? 1 : (r4 == r5 ? 0 : -1))
            if (r4 >= 0) goto L_0x00a9
            r6.onLeaveCancelView()
        L_0x00a9:
            com.oneplus.systemui.biometrics.OpQLRecyclerView r4 = r6.mQLRecyclerView
            if (r4 == 0) goto L_0x00ca
            int r4 = r7.getAction()
            if (r4 != r1) goto L_0x00bf
            r1 = 1092616192(0x41200000, float:10.0)
            int r0 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1))
            if (r0 <= 0) goto L_0x00bf
            com.oneplus.systemui.biometrics.OpQLRecyclerView r6 = r6.mQLRecyclerView
            r6.onScrollProgress(r3)
            goto L_0x00ca
        L_0x00bf:
            int r7 = r7.getAction()
            if (r7 != r2) goto L_0x00ca
            com.oneplus.systemui.biometrics.OpQLRecyclerView r6 = r6.mQLRecyclerView
            r6.launch()
        L_0x00ca:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.systemui.biometrics.OpQLRootView.onTouch(android.view.MotionEvent):void");
    }

    private void onEnterCancelView() {
        if (!this.mIsCancel) {
            this.mIsCancel = true;
            this.mQLCancelView.animate().alpha(0.3f).setDuration(75).start();
            this.mLabel.animate().alpha(0.0f).setDuration(375).start();
            this.mHint.setVisibility(0);
            this.mQLTrackViewBackground.animate().scaleX(0.0f).setDuration(250).setInterpolator(Interpolators.DECELERATE_QUINT).start();
            this.mQLRecyclerView.onEnterCancelView();
        }
    }

    private void onLeaveCancelView() {
        if (this.mIsCancel) {
            this.mIsCancel = false;
            this.mQLCancelView.animate().alpha(0.0f).setDuration(300).start();
            this.mLabel.animate().alpha(1.0f).setDuration(375).start();
            this.mHint.setVisibility(8);
            this.mQLTrackViewBackground.animate().scaleX(1.0f).setDuration(225).setInterpolator(Interpolators.DECELERATE_QUINT).start();
            this.mQLRecyclerView.onLeaveCancelView();
        }
    }

    public LayoutParams getLayoutParams() {
        LayoutParams layoutParams = new LayoutParams(-1, -1, 2303, 16778498, 1);
        layoutParams.privateFlags |= 16;
        layoutParams.setTitle("QuickLaunch");
        layoutParams.gravity = 49;
        layoutParams.dimAmount = 0.8f;
        layoutParams.windowAnimations = R$style.Animation_QuickLaunch;
        return layoutParams;
    }

    public void setQLConfig(String str) {
        this.mQLRecyclerView.setQLConfig(str);
        updateTrackBarVisibility();
    }

    public int getItemCount() {
        return this.mQLRecyclerView.getItemCount();
    }

    private void updateTrackBarVisibility() {
        if (getItemCount() > 0) {
            this.mQLTrackView.setVisibility(0);
        } else {
            this.mQLTrackView.setVisibility(8);
        }
    }
}

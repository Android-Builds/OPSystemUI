package com.oneplus.systemui.biometrics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityOptions;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Recycler;
import androidx.recyclerview.widget.RecyclerView.State;
import com.android.systemui.Interpolators;
import com.oneplus.config.ConfigObserver;
import com.oneplus.config.ConfigObserver.ConfigUpdater;
import com.oneplus.sdk.utils.OpBoostFramework;
import com.oneplus.systemui.OpSystemUIInjector;
import com.oneplus.systemui.biometrics.OpQLAdapter.ActionInfo;
import com.oneplus.util.OpUtils;
import com.oneplus.util.VibratorSceneUtils;
import java.util.ArrayList;
import org.json.JSONArray;

public class OpQLRecyclerView extends RecyclerView {
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new Builder().setContentType(4).setUsage(13).build();
    protected OpQLAdapter mAdapter;
    private boolean mAnimate;
    ArrayList<ActionInfo> mAppMap;
    /* access modifiers changed from: private */
    public int mBarPosition;
    private Runnable mCancelFalseRunnable;
    /* access modifiers changed from: private */
    public Runnable mCheckNextScroll;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public ValueAnimator mEnterAnimator;
    private AnimationViewData mFocusedViewData;
    /* access modifiers changed from: private */
    public OpQLHelper mHelper;
    private boolean mInitialized;
    /* access modifiers changed from: private */
    public boolean mIsCancel;
    /* access modifiers changed from: private */
    public boolean mIsQLExit;
    private TextView mLabel;
    private long mLastScrollTime;
    QLLayoutManager mLayoutManager;
    /* access modifiers changed from: private */
    public ValueAnimator mLeaveAnimator;
    /* access modifiers changed from: private */
    public int mPadding;
    private OpBoostFramework mPerfLock;
    /* access modifiers changed from: private */
    public int mPosition;
    private final ContentObserver mQLVibTimeObserver;
    private ConfigObserver mQuickPayConfigObserver;
    /* access modifiers changed from: private */
    public float mScrollSpeed;
    private Runnable mScrollToPosition;
    /* access modifiers changed from: private */
    public int mVibTime;
    /* access modifiers changed from: private */
    public RecyclerView mView;
    private final float mZoomAmount;
    private final float mZoomDistance;

    class AnimationViewData {
        View view;
        float xAfter;
        float xBefore;

        AnimationViewData() {
        }
    }

    public class QLLayoutManager extends LinearLayoutManager {
        public QLLayoutManager(Context context, int i, boolean z) {
            super(context, i, z);
        }

        public int scrollHorizontallyBy(int i, Recycler recycler, State state) {
            int scrollHorizontallyBy = super.scrollHorizontallyBy(i, recycler, state);
            OpQLRecyclerView.this.updateViewScale();
            OpQLRecyclerView.this.updateLabel();
            return scrollHorizontallyBy;
        }

        public void smoothScrollToPosition(RecyclerView recyclerView, State state, int i) {
            OpQLRecyclerView.this.stopScroll();
            if (!OpQLRecyclerView.this.mIsQLExit) {
                C20781 r2 = new LinearSmoothScroller(recyclerView.getContext()) {
                    /* access modifiers changed from: protected */
                    public float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                        return OpQLRecyclerView.this.mScrollSpeed / ((float) displayMetrics.densityDpi);
                    }
                };
                r2.setTargetPosition(i);
                startSmoothScroll(r2);
                OpQLRecyclerView.this.vibrate();
            }
        }
    }

    class QuickPayConfigUpdater implements ConfigUpdater {
        QuickPayConfigUpdater() {
        }

        public void updateConfig(JSONArray jSONArray) {
            OpQLRecyclerView.this.mHelper.resolveQuickPayConfigFromJSON(jSONArray);
        }
    }

    private int checkBarPosition(float f) {
        for (int i = 6; i >= 0; i--) {
            if (f > ((float) i)) {
                return i;
            }
        }
        return 0;
    }

    public OpQLRecyclerView(Context context) {
        this(context, null);
    }

    public OpQLRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mAppMap = new ArrayList<>();
        this.mFocusedViewData = new AnimationViewData();
        this.mPosition = 0;
        this.mScrollSpeed = 150.0f;
        this.mIsCancel = false;
        this.mInitialized = false;
        this.mBarPosition = 3;
        this.mPadding = 0;
        this.mAnimate = false;
        this.mVibTime = 0;
        this.mLastScrollTime = 0;
        this.mPerfLock = new OpBoostFramework();
        this.mHelper = null;
        this.mIsQLExit = false;
        this.mQLVibTimeObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean z) {
                OpQLRecyclerView opQLRecyclerView = OpQLRecyclerView.this;
                opQLRecyclerView.mVibTime = System.getInt(opQLRecyclerView.mContext.getContentResolver(), "ql_vib_time", 20);
                if (OpQLRecyclerView.this.mVibTime < 0) {
                    OpQLRecyclerView.this.mVibTime = 0;
                } else if (OpQLRecyclerView.this.mVibTime > 100) {
                    OpQLRecyclerView.this.mVibTime = 100;
                }
                if (Build.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("ql_vib_time ");
                    sb.append(OpQLRecyclerView.this.mVibTime);
                    Log.d("QuickLaunch.QLRecyclerView", sb.toString());
                }
            }
        };
        this.mCheckNextScroll = new Runnable() {
            public void run() {
                if (!OpQLRecyclerView.this.mIsQLExit) {
                    int access$300 = OpQLRecyclerView.this.mPosition;
                    if (OpQLRecyclerView.this.mBarPosition >= 6 && access$300 > 0) {
                        access$300--;
                    } else if (OpQLRecyclerView.this.mBarPosition == 0 && access$300 < OpQLRecyclerView.this.mAppMap.size() - 1) {
                        access$300++;
                        if (access$300 >= OpQLRecyclerView.this.mAppMap.size()) {
                            access$300 = OpQLRecyclerView.this.mAppMap.size() - 1;
                        }
                    }
                    if (access$300 != OpQLRecyclerView.this.mPosition) {
                        OpQLRecyclerView.this.mPosition = access$300;
                        OpQLRecyclerView.this.lambda$new$1$OpQLRecyclerView();
                        OpQLRecyclerView opQLRecyclerView = OpQLRecyclerView.this;
                        opQLRecyclerView.postDelayed(opQLRecyclerView.mCheckNextScroll, 500);
                    }
                }
            }
        };
        this.mScrollToPosition = new Runnable() {
            public final void run() {
                OpQLRecyclerView.this.lambda$new$1$OpQLRecyclerView();
            }
        };
        this.mZoomAmount = 0.32999998f;
        this.mZoomDistance = 0.25f;
        this.mCancelFalseRunnable = new Runnable() {
            public final void run() {
                OpQLRecyclerView.this.lambda$new$2$OpQLRecyclerView();
            }
        };
        this.mContext = context;
        this.mLayoutManager = new QLLayoutManager(this.mContext, 0, false);
        setLayoutManager(this.mLayoutManager);
        this.mHelper = new OpQLHelper(context);
        this.mAppMap = this.mHelper.getQLApps();
        setHasFixedSize(true);
        setItemViewCacheSize(6);
        setDrawingCacheEnabled(true);
        setDrawingCacheQuality(1048576);
        setNestedScrollingEnabled(false);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mView = this;
        this.mPadding = getPaddingRight();
        postDelayed(new Runnable() {
            public final void run() {
                OpQLRecyclerView.this.lambda$onFinishInflate$0$OpQLRecyclerView();
            }
        }, 200);
        this.mQuickPayConfigObserver = new ConfigObserver(this.mContext, getHandler(), new QuickPayConfigUpdater(), "QuickPay_APPS_Config");
        this.mQuickPayConfigObserver.register();
    }

    public /* synthetic */ void lambda$onFinishInflate$0$OpQLRecyclerView() {
        View findViewByPosition = this.mLayoutManager.findViewByPosition(this.mPosition);
        if (findViewByPosition != null) {
            findViewByPosition.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(100).start();
        }
        this.mInitialized = true;
    }

    public void onQLExit() {
        this.mIsQLExit = true;
        removeCallbacks(this.mScrollToPosition);
        removeCallbacks(this.mCheckNextScroll);
        this.mQuickPayConfigObserver.unregister();
        this.mQuickPayConfigObserver = null;
        setAdapter(null);
        OpQLAdapter opQLAdapter = this.mAdapter;
        if (opQLAdapter != null) {
            opQLAdapter.onQLExit();
            this.mAdapter = null;
        }
        this.mView = null;
        this.mAppMap = null;
        this.mHelper = null;
        this.mContext = null;
        this.mFocusedViewData.view = null;
        this.mFocusedViewData = null;
    }

    /* access modifiers changed from: private */
    /* renamed from: scrollToPosition */
    public void lambda$new$1$OpQLRecyclerView() {
        this.mLayoutManager.smoothScrollToPosition(this, new State(), this.mPosition);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:35:0x009d, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void onScrollProgress(float r5) {
        /*
            r4 = this;
            monitor-enter(r4)
            boolean r0 = r4.mInitialized     // Catch:{ all -> 0x009e }
            if (r0 != 0) goto L_0x0007
            monitor-exit(r4)
            return
        L_0x0007:
            int r5 = r4.checkBarPosition(r5)     // Catch:{ all -> 0x009e }
            boolean r0 = r4.isLayoutRtl()     // Catch:{ all -> 0x009e }
            if (r0 == 0) goto L_0x0013
            int r5 = 7 - r5
        L_0x0013:
            int r0 = r4.mBarPosition     // Catch:{ all -> 0x009e }
            if (r5 == r0) goto L_0x009c
            int r0 = r4.mBarPosition     // Catch:{ all -> 0x009e }
            int r0 = r5 - r0
            r4.mBarPosition = r5     // Catch:{ all -> 0x009e }
            boolean r5 = r4.mIsCancel     // Catch:{ all -> 0x009e }
            if (r5 == 0) goto L_0x0023
            monitor-exit(r4)
            return
        L_0x0023:
            int r5 = r4.mPosition     // Catch:{ all -> 0x009e }
            int r5 = r5 - r0
            if (r5 >= 0) goto L_0x002a
            r5 = 0
            goto L_0x003a
        L_0x002a:
            java.util.ArrayList<com.oneplus.systemui.biometrics.OpQLAdapter$ActionInfo> r0 = r4.mAppMap     // Catch:{ all -> 0x009e }
            int r0 = r0.size()     // Catch:{ all -> 0x009e }
            if (r5 < r0) goto L_0x003a
            java.util.ArrayList<com.oneplus.systemui.biometrics.OpQLAdapter$ActionInfo> r5 = r4.mAppMap     // Catch:{ all -> 0x009e }
            int r5 = r5.size()     // Catch:{ all -> 0x009e }
            int r5 = r5 + -1
        L_0x003a:
            boolean r0 = android.os.Build.DEBUG_ONEPLUS     // Catch:{ all -> 0x009e }
            if (r0 == 0) goto L_0x0068
            java.lang.String r0 = "QuickLaunch.QLRecyclerView"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x009e }
            r1.<init>()     // Catch:{ all -> 0x009e }
            java.lang.String r2 = "onScrollProgress mBarPosition "
            r1.append(r2)     // Catch:{ all -> 0x009e }
            int r2 = r4.mBarPosition     // Catch:{ all -> 0x009e }
            r1.append(r2)     // Catch:{ all -> 0x009e }
            java.lang.String r2 = " mPosition "
            r1.append(r2)     // Catch:{ all -> 0x009e }
            int r2 = r4.mPosition     // Catch:{ all -> 0x009e }
            r1.append(r2)     // Catch:{ all -> 0x009e }
            java.lang.String r2 = " position "
            r1.append(r2)     // Catch:{ all -> 0x009e }
            r1.append(r5)     // Catch:{ all -> 0x009e }
            java.lang.String r1 = r1.toString()     // Catch:{ all -> 0x009e }
            android.util.Log.d(r0, r1)     // Catch:{ all -> 0x009e }
        L_0x0068:
            java.lang.Runnable r0 = r4.mCheckNextScroll     // Catch:{ all -> 0x009e }
            r4.removeCallbacks(r0)     // Catch:{ all -> 0x009e }
            int r0 = r4.mPosition     // Catch:{ all -> 0x009e }
            if (r0 == r5) goto L_0x0095
            r4.mPosition = r5     // Catch:{ all -> 0x009e }
            long r0 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x009e }
            long r2 = r4.mLastScrollTime     // Catch:{ all -> 0x009e }
            long r0 = r0 - r2
            r2 = 50
            int r5 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
            if (r5 >= 0) goto L_0x008c
            java.lang.Runnable r5 = r4.mScrollToPosition     // Catch:{ all -> 0x009e }
            r4.removeCallbacks(r5)     // Catch:{ all -> 0x009e }
            java.lang.Runnable r5 = r4.mScrollToPosition     // Catch:{ all -> 0x009e }
            long r2 = r2 - r0
            r4.postDelayed(r5, r2)     // Catch:{ all -> 0x009e }
            goto L_0x008f
        L_0x008c:
            r4.lambda$new$1$OpQLRecyclerView()     // Catch:{ all -> 0x009e }
        L_0x008f:
            long r0 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x009e }
            r4.mLastScrollTime = r0     // Catch:{ all -> 0x009e }
        L_0x0095:
            java.lang.Runnable r5 = r4.mCheckNextScroll     // Catch:{ all -> 0x009e }
            r0 = 500(0x1f4, double:2.47E-321)
            r4.postDelayed(r5, r0)     // Catch:{ all -> 0x009e }
        L_0x009c:
            monitor-exit(r4)
            return
        L_0x009e:
            r5 = move-exception
            monitor-exit(r4)
            throw r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.systemui.biometrics.OpQLRecyclerView.onScrollProgress(float):void");
    }

    public void launch() {
        int i;
        int i2;
        removeCallbacks(this.mCheckNextScroll);
        if (!this.mIsCancel) {
            View findViewByPosition = this.mLayoutManager.findViewByPosition(this.mPosition);
            if (findViewByPosition != null) {
                int i3 = 0;
                this.mPerfLock.acquireBoostFor(0, 2000);
                ActionInfo actionInfo = (ActionInfo) findViewByPosition.getTag();
                StringBuilder sb = new StringBuilder();
                sb.append(actionInfo.mPackageName);
                sb.append("0");
                OpSystemUIInjector.addAppLockerPassedPackage(sb.toString());
                if (actionInfo.mActionName.equals("OpenApp")) {
                    int measuredWidth = findViewByPosition.getMeasuredWidth();
                    int measuredHeight = findViewByPosition.getMeasuredHeight();
                    Drawable drawable = ((ImageView) findViewByPosition).getDrawable();
                    if (drawable != null) {
                        Rect bounds = drawable.getBounds();
                        i2 = (measuredWidth - bounds.width()) / 2;
                        int paddingTop = findViewByPosition.getPaddingTop();
                        i = bounds.width();
                        int i4 = paddingTop;
                        measuredHeight = bounds.height();
                        i3 = i4;
                    } else {
                        i = measuredWidth;
                        i2 = 0;
                    }
                    this.mHelper.startApp(actionInfo.mPackageName, ActivityOptions.makeClipRevealAnimation(findViewByPosition, i2, i3, i, measuredHeight), actionInfo.mUid);
                } else if (actionInfo.mActionName.equals("OpenShortcut")) {
                    this.mHelper.startShortcut(actionInfo.mPackageName, actionInfo.mShortcutId, actionInfo.mUid, false);
                } else if (actionInfo.mActionName.equals("OpenQuickPay")) {
                    this.mHelper.startQuickPay(actionInfo.mPaymentWhich, actionInfo.mUid);
                } else if (actionInfo.mActionName.equals("OpenWxMiniProgram")) {
                    this.mHelper.startWxMiniProgram(actionInfo.mWxMiniProgramWhich);
                }
            }
        }
    }

    public void setLabelView(TextView textView) {
        this.mLabel = textView;
    }

    /* access modifiers changed from: private */
    public void updateViewScale() {
        float width = ((float) getWidth()) / 2.0f;
        float f = 0.25f * width;
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            float min = ((-0.32999998f * (Math.min(f, Math.abs(width - (((float) (this.mLayoutManager.getDecoratedRight(childAt) + this.mLayoutManager.getDecoratedLeft(childAt))) / 2.0f))) - 0.0f)) / (f - 0.0f)) + 1.0f;
            if (Build.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("updateViewScale mIsCancel ");
                sb.append(this.mIsCancel);
                Log.d("QuickLaunch.QLRecyclerView", sb.toString());
            }
            if (!this.mIsCancel) {
                childAt.setScaleX(min);
                childAt.setScaleY(min);
                if (min > 0.9f) {
                    childAt.setAlpha(1.0f);
                } else {
                    childAt.setAlpha(0.5f);
                }
            } else {
                childAt.animate().scaleX(0.67f).scaleY(0.67f).alpha(0.5f).start();
            }
        }
    }

    public void onVelocityChanged(float f) {
        if (f <= 250.0f) {
            f = 250.0f;
        }
        this.mScrollSpeed = (250.0f / f) * 250.0f * 1.2f;
        float f2 = this.mScrollSpeed;
        if (f2 < 50.0f) {
            this.mScrollSpeed = 50.0f;
        } else if (f2 > 470.0f) {
            this.mScrollSpeed = 470.0f;
        }
    }

    /* access modifiers changed from: private */
    public void vibrate() {
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        int i = this.mVibTime;
        VibrationEffect createOneShot = VibrationEffect.createOneShot(i != 0 ? (long) i : 20, -1);
        if (OpUtils.isSupportLinearVibration()) {
            VibratorSceneUtils.doVibrateWithSceneIfNeeded(this.mContext, vibrator, 1011);
        } else if (OpUtils.isSupportZVibrationMotor()) {
            vibrator.vibrate(VibrationEffect.get(0), VIBRATION_ATTRIBUTES);
        } else {
            vibrator.vibrate(createOneShot, VIBRATION_ATTRIBUTES);
        }
    }

    /* access modifiers changed from: private */
    public void updateLabel() {
        String str = ((ActionInfo) this.mAppMap.get(this.mPosition)).mLabel;
        TextView textView = this.mLabel;
        if (textView != null && str != null) {
            textView.setText(str);
        }
    }

    public int getItemCount() {
        return this.mAppMap.size();
    }

    /* access modifiers changed from: private */
    public void cancelAnimation(float f, boolean z) {
        float iconPadding = ((float) this.mAdapter.getIconPadding()) - (((float) (this.mAdapter.getIconPadding() - 10)) * (1.0f - f));
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            MarginLayoutParams marginLayoutParams = (MarginLayoutParams) childAt.getLayoutParams();
            int i2 = (int) iconPadding;
            marginLayoutParams.leftMargin = i2;
            marginLayoutParams.rightMargin = i2;
            childAt.setLayoutParams(marginLayoutParams);
            if (childAt.equals(this.mLayoutManager.findViewByPosition(this.mPosition))) {
                float f2 = f * 0.5f;
                float f3 = f2 + 1.0f;
                childAt.setScaleX(f3);
                childAt.setScaleY(f3);
                childAt.setAlpha(f2 + 0.5f);
            }
        }
        if (!z) {
            AnimationViewData animationViewData = this.mFocusedViewData;
            float f4 = animationViewData.xAfter;
            this.mLayoutManager.scrollHorizontallyBy((int) (animationViewData.view.getX() - (f4 + ((animationViewData.xBefore - f4) * f))), new Recycler(), new State());
        }
    }

    public synchronized void onEnterCancelView() {
        if (Build.DEBUG_ONEPLUS) {
            Log.d("QuickLaunch.QLRecyclerView", "onEnterCancelView");
        }
        removeCallbacks(this.mCancelFalseRunnable);
        this.mIsCancel = true;
        removeCallbacks(this.mCheckNextScroll);
        if (this.mAnimate) {
            this.mFocusedViewData.view = this.mLayoutManager.findViewByPosition(this.mPosition);
            this.mFocusedViewData.xBefore = this.mFocusedViewData.view.getX();
            if (this.mLeaveAnimator != null && this.mLeaveAnimator.isRunning()) {
                this.mLeaveAnimator.end();
            }
            if (this.mEnterAnimator != null) {
                this.mEnterAnimator.cancel();
            }
            this.mEnterAnimator = ValueAnimator.ofInt(new int[]{this.mPadding, 0});
            this.mEnterAnimator.setInterpolator(Interpolators.ACCELERATE);
            this.mEnterAnimator.setDuration(300);
            this.mEnterAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    OpQLRecyclerView.this.mView.setPadding(((Integer) valueAnimator.getAnimatedValue()).intValue(), 40, ((Integer) valueAnimator.getAnimatedValue()).intValue(), 40);
                    OpQLRecyclerView.this.cancelAnimation(((float) ((Integer) valueAnimator.getAnimatedValue()).intValue()) / ((float) OpQLRecyclerView.this.mPadding), true);
                }
            });
            this.mEnterAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    if (Build.DEBUG_ONEPLUS) {
                        Log.d("QuickLaunch.QLRecyclerView", "mEnterAnimator end");
                    }
                    OpQLRecyclerView.this.cancelAnimation(0.0f, true);
                    OpQLRecyclerView.this.mEnterAnimator = null;
                }
            });
            this.mEnterAnimator.start();
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                View childAt = getChildAt(i);
                if (childAt.getScaleX() > 0.67f) {
                    childAt.animate().scaleX(0.67f).scaleY(0.67f).alpha(0.5f).setDuration(100).start();
                }
            }
        }
    }

    public /* synthetic */ void lambda$new$2$OpQLRecyclerView() {
        if (Build.DEBUG_ONEPLUS) {
            Log.d("QuickLaunch.QLRecyclerView", "mCancelFalseRunnable");
        }
        this.mIsCancel = false;
    }

    public synchronized void onLeaveCancelView() {
        if (Build.DEBUG_ONEPLUS) {
            Log.d("QuickLaunch.QLRecyclerView", "onLeaveCancelView");
        }
        setOnScrollListener(null);
        if (this.mAnimate) {
            this.mFocusedViewData.xAfter = this.mFocusedViewData.view.getX();
            if (this.mEnterAnimator != null && this.mEnterAnimator.isRunning()) {
                this.mEnterAnimator.end();
            }
            if (this.mLeaveAnimator != null) {
                this.mLeaveAnimator.cancel();
            }
            this.mLeaveAnimator = ValueAnimator.ofInt(new int[]{0, this.mPadding});
            this.mLeaveAnimator.setInterpolator(Interpolators.ACCELERATE);
            this.mLeaveAnimator.setDuration(300);
            this.mLeaveAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float intValue = ((float) ((Integer) valueAnimator.getAnimatedValue()).intValue()) / ((float) OpQLRecyclerView.this.mPadding);
                    OpQLRecyclerView.this.mView.setPadding(((Integer) valueAnimator.getAnimatedValue()).intValue(), 40, ((Integer) valueAnimator.getAnimatedValue()).intValue(), 40);
                    OpQLRecyclerView.this.cancelAnimation(intValue, false);
                }
            });
            this.mLeaveAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    if (Build.DEBUG_ONEPLUS) {
                        Log.d("QuickLaunch.QLRecyclerView", "mLeaveAnimator end");
                    }
                    OpQLRecyclerView.this.mIsCancel = false;
                    OpQLRecyclerView.this.cancelAnimation(1.0f, false);
                    OpQLRecyclerView.this.mLeaveAnimator = null;
                    OpQLRecyclerView.this.updateLabel();
                }
            });
            this.mLeaveAnimator.start();
        } else {
            this.mLayoutManager.findViewByPosition(this.mPosition).animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(100).start();
            postDelayed(this.mCancelFalseRunnable, 100);
            updateLabel();
        }
        postDelayed(this.mCheckNextScroll, 500);
    }

    public void setQLConfig(String str) {
        if (str != null && !str.equals("")) {
            this.mHelper.parseQLConfig(str);
            this.mAdapter = new OpQLAdapter(this.mContext, this.mAppMap);
            setAdapter(this.mAdapter);
            if (this.mAdapter.getItemCount() > 0) {
                this.mPosition = this.mAdapter.getItemCount() - 1;
                this.mLayoutManager.scrollToPosition(this.mPosition);
                updateLabel();
            }
        }
    }
}

package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.MathUtils;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.RegionInterceptingFrameLayout.RegionInterceptableView;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.fragments.FragmentHostManager.FragmentListener;
import com.android.systemui.p007qs.SecureSetting;
import com.android.systemui.plugins.p006qs.C0959QS;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.statusbar.phone.NavigationBarTransitions.DarkIntensityListener;
import com.android.systemui.statusbar.phone.NavigationModeController;
import com.android.systemui.statusbar.phone.NavigationModeController.ModeChangedListener;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.android.systemui.tuner.TunablePadding;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.android.systemui.util.leak.RotationUtils;
import com.oneplus.util.OpUtils;
import com.oneplus.util.SystemSetting;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ScreenDecorations extends SystemUI implements Tunable, DarkIntensityListener {
    private static final boolean DEBUG_SCREENSHOT_ROUNDED_CORNERS = SystemProperties.getBoolean("debug.screenshot_rounded_corners", false);
    /* access modifiers changed from: private */
    public static final boolean DEBUG_SCREEN_DECORATIONS = SystemProperties.getBoolean("debug.screen_decorations", false);
    /* access modifiers changed from: private */
    public static int mOpCustRegionRight;
    /* access modifiers changed from: private */
    public static int mOpCustRegionleft;
    /* access modifiers changed from: private */
    public int MAX_BLOCK_INTERVAL = 1200;
    private boolean mAssistHintBlocked = false;
    private boolean mAssistHintVisible;
    /* access modifiers changed from: private */
    public View mBottomOverlay;
    /* access modifiers changed from: private */
    public SecureSetting mColorInversionSetting;
    private DisplayCutoutView mCutoutBottom;
    private DisplayCutoutView mCutoutTop;
    private float mDensity;
    private DisplayListener mDisplayListener;
    private DisplayManager mDisplayManager;
    private Handler mHandler;
    private boolean mHasRoundedCorner = false;
    private boolean mInGesturalMode;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.USER_SWITCHED")) {
                ScreenDecorations.this.mColorInversionSetting.setUserId(intent.getIntExtra("android.intent.extra.user_handle", ActivityManager.getCurrentUser()));
                ScreenDecorations.this.mTempColorInversionDisableSetting.onUserSwitched();
                ScreenDecorations screenDecorations = ScreenDecorations.this;
                screenDecorations.updateColorInversion(screenDecorations.mColorInversionSetting.getValue());
            }
        }
    };
    private boolean mIsReceivingNavBarColor = false;
    /* access modifiers changed from: private */
    public long mLastLockTime;
    private Runnable mOpPendingMaxTimeRunnable = new Runnable() {
        public void run() {
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb = new StringBuilder();
                sb.append("mOpPendingMaxTimeRunnable, mOverlay.getVisibility:");
                sb.append(ScreenDecorations.this.mOverlay != null ? Integer.valueOf(ScreenDecorations.this.mOverlay.getVisibility()) : "null");
                sb.append(", mLastLockTime:");
                sb.append(ScreenDecorations.this.mLastLockTime);
                sb.append(", System.currentTimeMillis():");
                sb.append(System.currentTimeMillis());
                Log.i("ScreenDecorations", sb.toString());
            }
            if (ScreenDecorations.this.mOverlay != null && System.currentTimeMillis() - ScreenDecorations.this.mLastLockTime >= ((long) ScreenDecorations.this.MAX_BLOCK_INTERVAL)) {
                ScreenDecorations.this.updateOrientation();
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mOpPendingRotationChange = false;
    /* access modifiers changed from: private */
    public boolean mOpPendingRotationChangeBottom = false;
    /* access modifiers changed from: private */
    public View mOverlay;
    /* access modifiers changed from: private */
    public boolean mPendingRotationChange;
    float mResizeRatio = 0.75f;
    /* access modifiers changed from: private */
    public int mRotation;
    protected int mRoundedDefault;
    protected int mRoundedDefaultBottom;
    int mRoundedDefaultBottomResize;
    protected int mRoundedDefaultBottomWidth;
    int mRoundedDefaultBottomWidthResize;
    protected int mRoundedDefaultTop;
    int mRoundedDefaultTopResize;
    protected int mRoundedDefaultTopWidth;
    int mRoundedDefaultTopWidthResize;
    int mScreenResolution = 0;
    SettingsObserver mSettingsObserver;
    private Runnable mShowRunnable = new Runnable() {
        public void run() {
            int exactRotation = RotationUtils.getExactRotation(ScreenDecorations.this.mContext);
            String str = "ScreenDecorations";
            if (exactRotation != ScreenDecorations.this.mRotation) {
                StringBuilder sb = new StringBuilder();
                sb.append("Attention, rotation changed at postDelay interval, mRotation:");
                sb.append(ScreenDecorations.this.mRotation);
                sb.append(", newRotation:");
                sb.append(exactRotation);
                Log.i(str, sb.toString());
                ScreenDecorations.this.mRotation = exactRotation;
            }
            if (ScreenDecorations.this.isRectangleTop() || ScreenDecorations.this.isRectangleBottom()) {
                ScreenDecorations.this.updateDecorSize();
            }
            ScreenDecorations.this.updateLayoutParams();
            if (OpUtils.DEBUG_ONEPLUS) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("mShowRunnable, mOpPendingRotationChange:");
                sb2.append(ScreenDecorations.this.mOpPendingRotationChange);
                sb2.append(", mOpPendingRotationChangeBottom:");
                sb2.append(ScreenDecorations.this.mOpPendingRotationChangeBottom);
                sb2.append(", mRotation:");
                sb2.append(ScreenDecorations.this.mRotation);
                Log.i(str, sb2.toString());
            }
            ScreenDecorations.this.mOpPendingRotationChange = false;
            ScreenDecorations.this.mOpPendingRotationChangeBottom = false;
            ScreenDecorations.this.updateViews();
        }
    };
    /* access modifiers changed from: private */
    public SystemSetting mTempColorInversionDisableSetting;
    /* access modifiers changed from: private */
    public boolean mTempDisableInversion = false;
    private WindowManager mWindowManager;

    public static class DisplayCutoutView extends View implements DisplayListener, RegionInterceptableView {
        private final Path mBoundingPath = new Path();
        private final Rect mBoundingRect = new Rect();
        private final List<Rect> mBounds = new ArrayList();
        private int mColor = -16777216;
        private final ScreenDecorations mDecorations;
        private final DisplayInfo mInfo = new DisplayInfo();
        private final boolean mInitialStart;
        private final int[] mLocation = new int[2];
        private final Paint mPaint = new Paint();
        private int mRotation;
        private boolean mStart;
        private final Runnable mVisibilityChangedListener;

        public void onDisplayAdded(int i) {
        }

        public void onDisplayRemoved(int i) {
        }

        public boolean shouldInterceptTouch() {
            return false;
        }

        public DisplayCutoutView(Context context, boolean z, Runnable runnable, ScreenDecorations screenDecorations) {
            super(context);
            this.mInitialStart = z;
            this.mVisibilityChangedListener = runnable;
            this.mDecorations = screenDecorations;
            setId(R$id.display_cutout);
            if (ScreenDecorations.DEBUG_SCREEN_DECORATIONS) {
                this.mColor = -256;
            }
        }

        public void setColor(int i) {
            this.mColor = i;
            if (ScreenDecorations.DEBUG_SCREEN_DECORATIONS) {
                this.mColor = -256;
            }
            invalidate();
        }

        /* access modifiers changed from: protected */
        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).registerDisplayListener(this, getHandler());
            update();
        }

        /* access modifiers changed from: protected */
        public void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).unregisterDisplayListener(this);
        }

        /* access modifiers changed from: protected */
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            getLocationOnScreen(this.mLocation);
            int[] iArr = this.mLocation;
            canvas.translate((float) (-iArr[0]), (float) (-iArr[1]));
            if (!this.mBoundingPath.isEmpty()) {
                this.mPaint.setColor(this.mColor);
                this.mPaint.setStyle(Style.FILL);
                this.mPaint.setAntiAlias(true);
                canvas.drawPath(this.mBoundingPath, this.mPaint);
            }
        }

        public void onDisplayChanged(final int i) {
            postDelayed(new Runnable() {
                public void run() {
                    if (i == DisplayCutoutView.this.getDisplay().getDisplayId()) {
                        DisplayCutoutView.this.update();
                    }
                }
            }, (long) (KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockRecognizing() ? 500 : 400));
        }

        private boolean isStart() {
            int i = this.mRotation;
            if (!(i == 2 || i == 3)) {
                return this.mInitialStart;
            }
            if (!this.mInitialStart) {
                return true;
            }
            return false;
        }

        /* access modifiers changed from: private */
        public void update() {
            int i;
            if (isAttachedToWindow() && !this.mDecorations.mPendingRotationChange) {
                this.mStart = isStart();
                requestLayout();
                getDisplay().getDisplayInfo(this.mInfo);
                this.mBounds.clear();
                this.mBoundingRect.setEmpty();
                this.mBoundingPath.reset();
                if (!ScreenDecorations.shouldDrawCutout(getContext()) || !hasCutout()) {
                    i = 8;
                } else {
                    this.mBounds.addAll(this.mInfo.displayCutout.getBoundingRects());
                    localBounds(this.mBoundingRect);
                    updateGravity();
                    updateBoundingPath();
                    invalidate();
                    i = 0;
                }
                if (i != getVisibility()) {
                    setVisibility(i);
                }
            }
        }

        private void updateBoundingPath() {
            DisplayInfo displayInfo = this.mInfo;
            int i = displayInfo.logicalWidth;
            int i2 = displayInfo.logicalHeight;
            int i3 = displayInfo.rotation;
            boolean z = true;
            if (!(i3 == 1 || i3 == 3)) {
                z = false;
            }
            int i4 = z ? i2 : i;
            if (!z) {
                i = i2;
            }
            this.mBoundingPath.set(DisplayCutout.pathFromResources(getResources(), i4, i));
            Matrix matrix = new Matrix();
            transformPhysicalToLogicalCoordinates(this.mInfo.rotation, i4, i, matrix);
            this.mBoundingPath.transform(matrix);
        }

        private static void transformPhysicalToLogicalCoordinates(int i, int i2, int i3, Matrix matrix) {
            if (i == 0) {
                matrix.reset();
            } else if (i == 1) {
                matrix.setRotate(270.0f);
                matrix.postTranslate(0.0f, (float) i2);
            } else if (i == 2) {
                matrix.setRotate(180.0f);
                matrix.postTranslate((float) i2, (float) i3);
            } else if (i == 3) {
                matrix.setRotate(90.0f);
                matrix.postTranslate((float) i3, 0.0f);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown rotation: ");
                sb.append(i);
                throw new IllegalArgumentException(sb.toString());
            }
        }

        private void updateGravity() {
            LayoutParams layoutParams = getLayoutParams();
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams layoutParams2 = (FrameLayout.LayoutParams) layoutParams;
                int gravity = getGravity(this.mInfo.displayCutout);
                if (layoutParams2.gravity != gravity) {
                    layoutParams2.gravity = gravity;
                    setLayoutParams(layoutParams2);
                }
            }
        }

        private boolean hasCutout() {
            DisplayCutout displayCutout = this.mInfo.displayCutout;
            boolean z = false;
            if (displayCutout == null) {
                return false;
            }
            if (this.mStart) {
                if (displayCutout.getSafeInsetLeft() > 0 || displayCutout.getSafeInsetTop() > 0 || displayCutout.getSafeInsetRight() > 0 || displayCutout.getSafeInsetBottom() > 0) {
                    z = true;
                }
                return z;
            }
            if (displayCutout.getSafeInsetRight() > 0 || displayCutout.getSafeInsetBottom() > 0) {
                z = true;
            }
            return z;
        }

        /* access modifiers changed from: protected */
        public void onMeasure(int i, int i2) {
            if (this.mBounds.isEmpty()) {
                super.onMeasure(i, i2);
            } else {
                setMeasuredDimension(View.resolveSizeAndState(this.mBoundingRect.width(), i, 0), View.resolveSizeAndState(this.mBoundingRect.height() + 2, i2, 0));
            }
        }

        public static void boundsFromDirection(Context context, DisplayCutout displayCutout, int i, Rect rect) {
            if (displayCutout != null) {
                if (i == 3) {
                    rect.set(displayCutout.getBoundingRectLeft());
                } else if (i == 5) {
                    rect.set(displayCutout.getBoundingRectRight());
                } else if (i == 48) {
                    rect.set(displayCutout.getBoundingRectTop());
                    if (context != null && (!(ScreenDecorations.mOpCustRegionleft == 0 && ScreenDecorations.mOpCustRegionRight == 0) && !OpUtils.isCutoutEmulationEnabled())) {
                        rect.left = ScreenDecorations.mOpCustRegionleft;
                        rect.right = ScreenDecorations.mOpCustRegionRight;
                    }
                } else if (i != 80) {
                    rect.setEmpty();
                } else {
                    rect.set(displayCutout.getBoundingRectBottom());
                }
            }
        }

        private void localBounds(Rect rect) {
            DisplayCutout displayCutout = this.mInfo.displayCutout;
            boundsFromDirection(this.mContext, displayCutout, getGravity(displayCutout), rect);
        }

        private int getGravity(DisplayCutout displayCutout) {
            if (this.mStart) {
                if (displayCutout.getSafeInsetLeft() > 0) {
                    ((FrameLayout.LayoutParams) getLayoutParams()).gravity = 3;
                    return 3;
                } else if (displayCutout.getSafeInsetTop() > 0) {
                    ((FrameLayout.LayoutParams) getLayoutParams()).gravity = 48;
                    return 48;
                } else if (displayCutout.getSafeInsetRight() > 0) {
                    ((FrameLayout.LayoutParams) getLayoutParams()).gravity = 5;
                    return 5;
                } else if (displayCutout.getSafeInsetBottom() > 0) {
                    ((FrameLayout.LayoutParams) getLayoutParams()).gravity = 80;
                    return 80;
                }
            } else if (displayCutout.getSafeInsetRight() > 0) {
                return 5;
            } else {
                if (displayCutout.getSafeInsetBottom() > 0) {
                    return 80;
                }
            }
            return 0;
        }

        public Region getInterceptRegion() {
            if (this.mInfo.displayCutout == null) {
                return null;
            }
            View rootView = getRootView();
            Region rectsToRegion = ScreenDecorations.rectsToRegion(this.mInfo.displayCutout.getBoundingRects());
            rootView.getLocationOnScreen(this.mLocation);
            int[] iArr = this.mLocation;
            rectsToRegion.translate(-iArr[0], -iArr[1]);
            rectsToRegion.op(rootView.getLeft(), rootView.getTop(), rootView.getRight(), rootView.getBottom(), Op.INTERSECT);
            return rectsToRegion;
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri mOPScreenResolutionUri = Global.getUriFor("oneplus_screen_resolution_adjust");

        public SettingsObserver() {
            super(new Handler());
            ScreenDecorations.this.mContext.getContentResolver().registerContentObserver(this.mOPScreenResolutionUri, false, this, -1);
        }

        public void onChange(boolean z, Uri uri) {
            if (uri != null) {
                ScreenDecorations screenDecorations = ScreenDecorations.this;
                screenDecorations.mScreenResolution = Global.getInt(screenDecorations.mContext.getContentResolver(), "oneplus_screen_resolution_adjust", 2);
            }
        }
    }

    static class TunablePaddingTagListener implements FragmentListener {
        private final int mId;
        private final int mPadding;
        private TunablePadding mTunablePadding;

        public TunablePaddingTagListener(int i, int i2) {
            this.mPadding = i;
            this.mId = i2;
        }

        public void onFragmentViewCreated(String str, Fragment fragment) {
            TunablePadding tunablePadding = this.mTunablePadding;
            if (tunablePadding != null) {
                tunablePadding.destroy();
            }
            View view = fragment.getView();
            int i = this.mId;
            if (i != 0) {
                view = view.findViewById(i);
            }
            this.mTunablePadding = TunablePadding.addTunablePadding(view, "sysui_rounded_content_padding", this.mPadding, 3);
        }
    }

    private class ValidatingPreDrawListener implements OnPreDrawListener {
        private final int mId;
        private final View mView;

        public ValidatingPreDrawListener(View view, int i) {
            this.mView = view;
            this.mId = i;
        }

        public boolean onPreDraw() {
            int exactRotation = RotationUtils.getExactRotation(ScreenDecorations.this.mContext);
            StringBuilder sb = new StringBuilder();
            sb.append("onPreDraw pass, mRotation:");
            sb.append(ScreenDecorations.this.mRotation);
            sb.append(", mOpPendingRotationChange:");
            sb.append(ScreenDecorations.this.mOpPendingRotationChange);
            sb.append(", mOpPendingRotationChangeBottom:");
            sb.append(ScreenDecorations.this.mOpPendingRotationChangeBottom);
            sb.append(", mLastLockTime:");
            sb.append(ScreenDecorations.this.mLastLockTime);
            Log.i("ScreenDecorations", sb.toString());
            if (exactRotation != ScreenDecorations.this.mRotation) {
                if (!(ScreenDecorations.this.mOpPendingRotationChange && ScreenDecorations.this.mOpPendingRotationChangeBottom)) {
                    ScreenDecorations.this.mLastLockTime = System.currentTimeMillis();
                    ScreenDecorations.this.postOpPendingMaxTime();
                    ScreenDecorations.this.mOpPendingRotationChange = true;
                    ScreenDecorations.this.mOpPendingRotationChangeBottom = true;
                }
                if (ScreenDecorations.this.mOverlay != null) {
                    ScreenDecorations.this.mOverlay.setVisibility(8);
                    ScreenDecorations.this.mOverlay.invalidate();
                }
                if (ScreenDecorations.this.mBottomOverlay != null) {
                    ScreenDecorations.this.mBottomOverlay.setVisibility(8);
                    ScreenDecorations.this.mBottomOverlay.invalidate();
                }
                return false;
            } else if ((this.mId != 0 || !ScreenDecorations.this.mOpPendingRotationChange) && (this.mId != 1 || !ScreenDecorations.this.mOpPendingRotationChangeBottom)) {
                System.currentTimeMillis();
                ScreenDecorations.this.mLastLockTime;
                ScreenDecorations.this.MAX_BLOCK_INTERVAL;
                return true;
            } else {
                this.mView.setVisibility(8);
                return false;
            }
        }
    }

    private boolean isLandscape(int i) {
        return i == 1 || i == 2;
    }

    public static Region rectsToRegion(List<Rect> list) {
        Region obtain = Region.obtain();
        if (list != null) {
            for (Rect rect : list) {
                if (rect != null && !rect.isEmpty()) {
                    obtain.op(rect, Op.UNION);
                }
            }
        }
        return obtain;
    }

    public void start() {
        this.mHandler = startHandlerThread();
        this.mHandler.post(new Runnable() {
            public final void run() {
                ScreenDecorations.this.startOnScreenDecorationsThread();
            }
        });
        setupStatusBarPaddingIfNeeded();
        putComponent(ScreenDecorations.class, this);
        this.mInGesturalMode = QuickStepContract.isGesturalMode(((NavigationModeController) Dependency.get(NavigationModeController.class)).addListener(new ModeChangedListener() {
            public final void onNavigationModeChanged(int i) {
                ScreenDecorations.this.lambda$handleNavigationModeChange$0$ScreenDecorations(i);
            }
        }));
    }

    /* access modifiers changed from: 0000 */
    /* renamed from: handleNavigationModeChange */
    public void lambda$handleNavigationModeChange$0$ScreenDecorations(int i) {
        if (!this.mHandler.getLooper().isCurrentThread()) {
            this.mHandler.post(new Runnable(i) {
                private final /* synthetic */ int f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    ScreenDecorations.this.lambda$handleNavigationModeChange$0$ScreenDecorations(this.f$1);
                }
            });
            return;
        }
        boolean isGesturalMode = QuickStepContract.isGesturalMode(i);
        if (this.mInGesturalMode != isGesturalMode) {
            this.mInGesturalMode = isGesturalMode;
            if (this.mInGesturalMode && this.mOverlay == null) {
                setupDecorations();
                if (this.mOverlay != null) {
                    updateLayoutParams();
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public Animator getHandleAnimator(View view, float f, float f2, boolean z, long j, Interpolator interpolator) {
        float lerp = MathUtils.lerp(2.0f, 1.0f, f);
        float lerp2 = MathUtils.lerp(2.0f, 1.0f, f2);
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, View.SCALE_X, new float[]{lerp, lerp2});
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(view, View.SCALE_Y, new float[]{lerp, lerp2});
        float lerp3 = MathUtils.lerp(0.2f, 0.0f, f);
        float lerp4 = MathUtils.lerp(0.2f, 0.0f, f2);
        float f3 = (float) (z ? -1 : 1);
        ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, new float[]{f3 * lerp3 * ((float) view.getWidth()), f3 * lerp4 * ((float) view.getWidth())});
        ObjectAnimator ofFloat4 = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, new float[]{lerp3 * ((float) view.getHeight()), lerp4 * ((float) view.getHeight())});
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(ofFloat).with(ofFloat2);
        animatorSet.play(ofFloat).with(ofFloat3);
        animatorSet.play(ofFloat).with(ofFloat4);
        animatorSet.setDuration(j);
        animatorSet.setInterpolator(interpolator);
        return animatorSet;
    }

    private void fade(View view, boolean z, boolean z2) {
        View view2 = view;
        if (z) {
            view.animate().cancel();
            view2.setAlpha(1.0f);
            view2.setVisibility(0);
            AnimatorSet animatorSet = new AnimatorSet();
            View view3 = view;
            boolean z3 = z2;
            Animator handleAnimator = getHandleAnimator(view3, 0.0f, 1.1f, z3, 750, new PathInterpolator(0.0f, 0.45f, 0.67f, 1.0f));
            PathInterpolator pathInterpolator = new PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f);
            Animator handleAnimator2 = getHandleAnimator(view3, 1.1f, 0.97f, z3, 400, pathInterpolator);
            Animator handleAnimator3 = getHandleAnimator(view3, 0.97f, 1.02f, z3, 400, pathInterpolator);
            Animator handleAnimator4 = getHandleAnimator(view3, 1.02f, 1.0f, z3, 400, pathInterpolator);
            animatorSet.play(handleAnimator).before(handleAnimator2);
            animatorSet.play(handleAnimator2).before(handleAnimator3);
            animatorSet.play(handleAnimator3).before(handleAnimator4);
            animatorSet.start();
            return;
        }
        view.animate().cancel();
        view.animate().setInterpolator(new AccelerateInterpolator(1.5f)).setDuration(250).alpha(0.0f);
    }

    /* renamed from: setAssistHintVisible */
    public void lambda$setAssistHintVisible$1$ScreenDecorations(boolean z) {
        if (!this.mHandler.getLooper().isCurrentThread()) {
            this.mHandler.post(new Runnable(z) {
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    ScreenDecorations.this.lambda$setAssistHintVisible$1$ScreenDecorations(this.f$1);
                }
            });
        } else if (!this.mAssistHintBlocked || !z) {
            View view = this.mOverlay;
            if (!(view == null || this.mBottomOverlay == null || this.mAssistHintVisible == z)) {
                this.mAssistHintVisible = z;
                CornerHandleView cornerHandleView = (CornerHandleView) view.findViewById(R$id.assist_hint_left);
                CornerHandleView cornerHandleView2 = (CornerHandleView) this.mOverlay.findViewById(R$id.assist_hint_right);
                CornerHandleView cornerHandleView3 = (CornerHandleView) this.mBottomOverlay.findViewById(R$id.assist_hint_left);
                CornerHandleView cornerHandleView4 = (CornerHandleView) this.mBottomOverlay.findViewById(R$id.assist_hint_right);
                int i = this.mRotation;
                if (i == 0) {
                    fade(cornerHandleView3, this.mAssistHintVisible, true);
                    fade(cornerHandleView4, this.mAssistHintVisible, false);
                } else if (i == 1) {
                    fade(cornerHandleView2, this.mAssistHintVisible, true);
                    fade(cornerHandleView4, this.mAssistHintVisible, false);
                } else if (i == 2) {
                    fade(cornerHandleView, this.mAssistHintVisible, false);
                    fade(cornerHandleView3, this.mAssistHintVisible, true);
                } else if (i == 3) {
                    fade(cornerHandleView, this.mAssistHintVisible, false);
                    fade(cornerHandleView2, this.mAssistHintVisible, true);
                }
            }
        }
    }

    /* renamed from: setAssistHintBlocked */
    public void lambda$setAssistHintBlocked$2$ScreenDecorations(boolean z) {
        if (!this.mHandler.getLooper().isCurrentThread()) {
            this.mHandler.post(new Runnable(z) {
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    ScreenDecorations.this.lambda$setAssistHintBlocked$2$ScreenDecorations(this.f$1);
                }
            });
            return;
        }
        this.mAssistHintBlocked = z;
        if (this.mAssistHintVisible && this.mAssistHintBlocked) {
            lambda$setAssistHintVisible$1$ScreenDecorations(false);
        }
    }

    /* access modifiers changed from: 0000 */
    public Handler startHandlerThread() {
        HandlerThread handlerThread = new HandlerThread("ScreenDecorations");
        handlerThread.start();
        return handlerThread.getThreadHandler();
    }

    private boolean shouldHostHandles() {
        return this.mInGesturalMode;
    }

    /* access modifiers changed from: private */
    public void startOnScreenDecorationsThread() {
        this.mRotation = RotationUtils.getExactRotation(this.mContext);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService(WindowManager.class);
        updateRoundedCornerRadii();
        this.mHasRoundedCorner = this.mContext.getResources().getBoolean(84148228);
        mOpCustRegionleft = this.mContext.getResources().getDimensionPixelSize(R$dimen.op_cust_statusbar_cutout_show_region_left);
        mOpCustRegionRight = this.mContext.getResources().getDimensionPixelSize(R$dimen.op_cust_statusbar_cutout_show_region_right);
        this.mSettingsObserver = new SettingsObserver();
        this.mScreenResolution = Global.getInt(this.mContext.getContentResolver(), "oneplus_screen_resolution_adjust", 2);
        if (hasRoundedCorners() || shouldDrawCutout() || shouldHostHandles()) {
            setupDecorations();
        }
        this.mDisplayListener = new DisplayListener() {
            public void onDisplayAdded(int i) {
            }

            public void onDisplayRemoved(int i) {
            }

            public void onDisplayChanged(int i) {
                int exactRotation = RotationUtils.getExactRotation(ScreenDecorations.this.mContext);
                boolean z = OpUtils.DEBUG_ONEPLUS;
                StringBuilder sb = new StringBuilder();
                sb.append("onDisplayChanged, displayId:");
                sb.append(i);
                sb.append(", now Rotation:");
                sb.append(ScreenDecorations.this.mRotation);
                sb.append(", newRotation:");
                sb.append(exactRotation);
                Log.i("ScreenDecorations", sb.toString());
                ScreenDecorations.this.mOpPendingRotationChange = false;
                ScreenDecorations.this.mOpPendingRotationChangeBottom = false;
                ScreenDecorations.this.updateOrientation();
            }
        };
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mHandler);
        updateOrientation();
    }

    private void setupDecorations() {
        this.mOverlay = LayoutInflater.from(this.mContext).inflate(R$layout.rounded_corners, null);
        this.mCutoutTop = new DisplayCutoutView(this.mContext, true, new Runnable() {
            public final void run() {
                ScreenDecorations.this.updateWindowVisibilities();
            }
        }, this);
        ((ViewGroup) this.mOverlay).addView(this.mCutoutTop);
        this.mBottomOverlay = LayoutInflater.from(this.mContext).inflate(R$layout.rounded_corners, null);
        this.mCutoutBottom = new DisplayCutoutView(this.mContext, false, new Runnable() {
            public final void run() {
                ScreenDecorations.this.updateWindowVisibilities();
            }
        }, this);
        ((ViewGroup) this.mBottomOverlay).addView(this.mCutoutBottom);
        this.mOverlay.setSystemUiVisibility(256);
        this.mOverlay.setAlpha(0.0f);
        this.mOverlay.setForceDarkAllowed(false);
        this.mBottomOverlay.setSystemUiVisibility(256);
        this.mBottomOverlay.setAlpha(0.0f);
        this.mBottomOverlay.setForceDarkAllowed(false);
        updateViews();
        initRoundCornerView();
        this.mWindowManager.addView(this.mOverlay, getWindowLayoutParams());
        this.mWindowManager.addView(this.mBottomOverlay, getBottomLayoutParams());
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
        this.mDensity = displayMetrics.density;
        ((Handler) Dependency.get(Dependency.MAIN_HANDLER)).post(new Runnable() {
            public final void run() {
                ScreenDecorations.this.lambda$setupDecorations$3$ScreenDecorations();
            }
        });
        this.mColorInversionSetting = new SecureSetting(this.mContext, this.mHandler, "accessibility_display_inversion_enabled") {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                ScreenDecorations.this.updateColorInversion(i);
            }
        };
        this.mColorInversionSetting.setListening(true);
        this.mColorInversionSetting.onChange(false);
        C07373 r6 = new SystemSetting(this.mContext, this.mHandler, "temp_disable_inversion", true) {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                ScreenDecorations.this.mTempDisableInversion = i != 0;
                ScreenDecorations screenDecorations = ScreenDecorations.this;
                screenDecorations.updateColorInversion(screenDecorations.mColorInversionSetting.getValue());
            }
        };
        this.mTempColorInversionDisableSetting = r6;
        this.mTempColorInversionDisableSetting.setListening(true);
        this.mTempColorInversionDisableSetting.onChange(false);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter, null, this.mHandler);
        this.mOverlay.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                ScreenDecorations.this.mOverlay.removeOnLayoutChangeListener(this);
                ScreenDecorations.this.mOverlay.animate().alpha(1.0f).setDuration(1000).start();
                ScreenDecorations.this.mBottomOverlay.animate().alpha(1.0f).setDuration(1000).start();
            }
        });
        this.mOverlay.getViewTreeObserver().addOnPreDrawListener(new ValidatingPreDrawListener(this.mOverlay, 0));
        this.mBottomOverlay.getViewTreeObserver().addOnPreDrawListener(new ValidatingPreDrawListener(this.mBottomOverlay, 1));
        if (DEBUG_SCREEN_DECORATIONS) {
            updateColorInversion(-256);
        }
    }

    public /* synthetic */ void lambda$setupDecorations$3$ScreenDecorations() {
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "sysui_rounded_size");
    }

    /* access modifiers changed from: private */
    public void updateColorInversion(int i) {
        int i2 = i != 0 ? -1 : -16777216;
        if (this.mTempDisableInversion) {
            i2 = -16777216;
        }
        if (DEBUG_SCREEN_DECORATIONS) {
            i2 = -256;
        }
        ColorStateList valueOf = ColorStateList.valueOf(i2);
        ((ImageView) this.mOverlay.findViewById(R$id.left)).setImageTintList(valueOf);
        ((ImageView) this.mOverlay.findViewById(R$id.right)).setImageTintList(valueOf);
        ((ImageView) this.mBottomOverlay.findViewById(R$id.left)).setImageTintList(valueOf);
        ((ImageView) this.mBottomOverlay.findViewById(R$id.right)).setImageTintList(valueOf);
        this.mCutoutTop.setColor(i2);
        this.mCutoutBottom.setColor(i2);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        this.mHandler.post(new Runnable(configuration) {
            private final /* synthetic */ Configuration f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                ScreenDecorations.this.lambda$onConfigurationChanged$4$ScreenDecorations(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$onConfigurationChanged$4$ScreenDecorations(Configuration configuration) {
        boolean z = OpUtils.DEBUG_ONEPLUS;
        StringBuilder sb = new StringBuilder();
        sb.append("receive onConfigurationChanged, newConfig.orientation:");
        sb.append(configuration.orientation);
        sb.append(", getRotation:");
        sb.append(RotationUtils.getExactRotation(this.mContext));
        Log.i("ScreenDecorations", sb.toString());
        if (OpUtils.isSupportResolutionSwitch(this.mContext)) {
            this.mScreenResolution = Global.getInt(this.mContext.getContentResolver(), "oneplus_screen_resolution_adjust", 2);
            updateDecorSize();
        }
        this.mPendingRotationChange = false;
        updateOrientation();
        updateRoundedCornerRadii();
        if (shouldDrawCutout() && this.mOverlay == null) {
            setupDecorations();
        }
        if (this.mOverlay != null) {
            updateLayoutParams();
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0055, code lost:
        if (r1.getVisibility() != 0) goto L_0x0057;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateOrientation() {
        /*
            r4 = this;
            android.os.Handler r0 = r4.mHandler
            android.os.Looper r0 = r0.getLooper()
            java.lang.Thread r0 = r0.getThread()
            java.lang.Thread r1 = java.lang.Thread.currentThread()
            r2 = 1
            if (r0 != r1) goto L_0x0013
            r0 = r2
            goto L_0x0014
        L_0x0013:
            r0 = 0
        L_0x0014:
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r3 = "must call on "
            r1.append(r3)
            android.os.Handler r3 = r4.mHandler
            android.os.Looper r3 = r3.getLooper()
            java.lang.Thread r3 = r3.getThread()
            r1.append(r3)
            java.lang.String r3 = ", but was "
            r1.append(r3)
            java.lang.Thread r3 = java.lang.Thread.currentThread()
            r1.append(r3)
            java.lang.String r1 = r1.toString()
            com.android.internal.util.Preconditions.checkState(r0, r1)
            boolean r0 = r4.mPendingRotationChange
            if (r0 == 0) goto L_0x0043
            return
        L_0x0043:
            android.content.Context r0 = r4.mContext
            int r0 = com.android.systemui.util.leak.RotationUtils.getExactRotation(r0)
            int r1 = r4.mRotation
            if (r0 != r1) goto L_0x0057
            android.view.View r1 = r4.mOverlay
            if (r1 == 0) goto L_0x0074
            int r1 = r1.getVisibility()
            if (r1 == 0) goto L_0x0074
        L_0x0057:
            r4.mRotation = r0
            android.view.View r0 = r4.mOverlay
            if (r0 == 0) goto L_0x0074
            r1 = 8
            r0.setVisibility(r1)
            android.view.View r0 = r4.mBottomOverlay
            r0.setVisibility(r1)
            r4.postShow()
            boolean r0 = r4.mAssistHintVisible
            if (r0 == 0) goto L_0x0074
            r4.hideAssistHandles()
            r4.lambda$setAssistHintVisible$1$ScreenDecorations(r2)
        L_0x0074:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.ScreenDecorations.updateOrientation():void");
    }

    private void postShow() {
        this.mHandler.removeCallbacks(this.mShowRunnable);
        this.mHandler.postDelayed(this.mShowRunnable, 400);
    }

    /* access modifiers changed from: private */
    public void postOpPendingMaxTime() {
        this.mHandler.removeCallbacks(this.mOpPendingMaxTimeRunnable);
        this.mHandler.postDelayed(this.mOpPendingMaxTimeRunnable, (long) this.MAX_BLOCK_INTERVAL);
    }

    private void hideAssistHandles() {
        View view = this.mOverlay;
        if (view != null && this.mBottomOverlay != null) {
            view.findViewById(R$id.assist_hint_left).setVisibility(8);
            this.mOverlay.findViewById(R$id.assist_hint_right).setVisibility(8);
            this.mBottomOverlay.findViewById(R$id.assist_hint_left).setVisibility(8);
            this.mBottomOverlay.findViewById(R$id.assist_hint_right).setVisibility(8);
            this.mAssistHintVisible = false;
        }
    }

    private void updateRoundedCornerRadii() {
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(17105400);
        int dimensionPixelSize2 = this.mContext.getResources().getDimensionPixelSize(R$dimen.rounded_corner_radius_top);
        int dimensionPixelSize3 = this.mContext.getResources().getDimensionPixelSize(R$dimen.rounded_corner_radius_bottom);
        int dimensionPixelSize4 = this.mContext.getResources().getDimensionPixelSize(R$dimen.rounded_corner_radius_top_width);
        int dimensionPixelSize5 = this.mContext.getResources().getDimensionPixelSize(R$dimen.rounded_corner_radius_bottom_width);
        if (DEBUG_SCREEN_DECORATIONS) {
            StringBuilder sb = new StringBuilder();
            sb.append("newRoundedDefaultTop:");
            sb.append(dimensionPixelSize2);
            sb.append("newRoundedDefaultBottom:");
            sb.append(dimensionPixelSize3);
            sb.append(", newRoundedDefaultBottomWidth:");
            sb.append(dimensionPixelSize5);
            Log.i("ScreenDecorations", sb.toString());
        }
        if ((this.mRoundedDefault == dimensionPixelSize && this.mRoundedDefaultBottom == dimensionPixelSize3 && this.mRoundedDefaultTop == dimensionPixelSize2) ? false : true) {
            this.mRoundedDefault = dimensionPixelSize;
            this.mRoundedDefaultTop = dimensionPixelSize2;
            this.mRoundedDefaultBottom = dimensionPixelSize3;
            this.mRoundedDefaultTopWidth = dimensionPixelSize4;
            this.mRoundedDefaultBottomWidth = dimensionPixelSize5;
            onTuningChanged("sysui_rounded_size", null);
        }
    }

    /* access modifiers changed from: private */
    public void updateViews() {
        View findViewById = this.mOverlay.findViewById(R$id.left);
        View findViewById2 = this.mOverlay.findViewById(R$id.right);
        View findViewById3 = this.mBottomOverlay.findViewById(R$id.left);
        View findViewById4 = this.mBottomOverlay.findViewById(R$id.right);
        int i = this.mRotation;
        if (i == 0) {
            updateViewDetermination(0, findViewById, 51, 0);
            updateViewDetermination(1, findViewById2, 53, 0);
            updateViewDetermination(2, findViewById3, 83, 0);
            updateViewDetermination(3, findViewById4, 85, 0);
        } else if (i == 1) {
            updateViewDetermination(0, findViewById, 83, 270);
            updateViewDetermination(1, findViewById2, 51, 90);
            updateViewDetermination(2, findViewById3, 85, 270);
            updateViewDetermination(3, findViewById4, 53, 90);
        } else if (i == 3) {
            updateViewDetermination(0, findViewById, 85, 180);
            updateViewDetermination(1, findViewById2, 83, 180);
            updateViewDetermination(2, findViewById3, 53, 180);
            updateViewDetermination(3, findViewById4, 51, 180);
        } else if (i == 2) {
            updateViewDetermination(0, findViewById, 53, 90);
            updateViewDetermination(1, findViewById2, 85, 270);
            updateViewDetermination(2, findViewById3, 51, 90);
            updateViewDetermination(3, findViewById4, 83, 270);
        }
        updateAssistantHandleViews();
        updateWindowVisibilities();
    }

    private void updateAssistantHandleViews() {
        View findViewById = this.mOverlay.findViewById(R$id.assist_hint_left);
        View findViewById2 = this.mOverlay.findViewById(R$id.assist_hint_right);
        View findViewById3 = this.mBottomOverlay.findViewById(R$id.assist_hint_left);
        View findViewById4 = this.mBottomOverlay.findViewById(R$id.assist_hint_right);
        int i = this.mAssistHintVisible ? 0 : 4;
        int i2 = this.mRotation;
        if (i2 == 0) {
            findViewById.setVisibility(8);
            findViewById2.setVisibility(8);
            findViewById3.setVisibility(i);
            findViewById4.setVisibility(i);
            updateView(findViewById3, 83, 270);
            updateView(findViewById4, 85, 180);
        } else if (i2 == 1) {
            findViewById.setVisibility(8);
            findViewById2.setVisibility(i);
            findViewById3.setVisibility(8);
            findViewById4.setVisibility(i);
            updateView(findViewById2, 83, 270);
            updateView(findViewById4, 85, 180);
        } else if (i2 == 3) {
            findViewById.setVisibility(i);
            findViewById2.setVisibility(i);
            findViewById3.setVisibility(8);
            findViewById4.setVisibility(8);
            updateView(findViewById, 83, 270);
            updateView(findViewById2, 85, 180);
        } else if (i2 == 2) {
            findViewById.setVisibility(i);
            findViewById2.setVisibility(8);
            findViewById3.setVisibility(i);
            findViewById4.setVisibility(8);
            updateView(findViewById, 85, 180);
            updateView(findViewById3, 83, 270);
        }
    }

    private void updateView(View view, int i, int i2) {
        ((FrameLayout.LayoutParams) view.getLayoutParams()).gravity = i;
        view.setRotation((float) i2);
    }

    private void updateViewDetermination(int i, View view, int i2, int i3) {
        if (i == 0 || i == 1) {
            if (isRectangleTop()) {
                updateViewByBitmap(i, (ImageView) view, i2, i3);
            } else {
                updateView(view, i2, i3);
            }
        } else if (i != 2 && i != 3) {
        } else {
            if (isRectangleBottom()) {
                updateViewByBitmap(i, (ImageView) view, i2, i3);
            } else {
                updateView(view, i2, i3);
            }
        }
    }

    private void updateViewByBitmap(int i, ImageView imageView, int i2, int i3) {
        int i4;
        ((FrameLayout.LayoutParams) imageView.getLayoutParams()).gravity = i2;
        if (i == 0 || i == 1) {
            i4 = R$drawable.rounded_top;
        } else {
            i4 = R$drawable.rounded_bottom;
        }
        try {
            InputStream openRawResource = this.mContext.getResources().openRawResource(i4);
            if (openRawResource != null) {
                Bitmap decodeStream = BitmapFactory.decodeStream(openRawResource);
                openRawResource.close();
                if (decodeStream == null) {
                    Log.d("ScreenDecorations", "Bitmap is null");
                    return;
                }
                imageView.setImageBitmap(rotaingbitmap(i3, decodeStream));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* access modifiers changed from: private */
    public void updateWindowVisibilities() {
        if (OpUtils.DEBUG_ONEPLUS) {
            Log.i("ScreenDecorations", "updateWindowVisibilities");
        }
        updateWindowVisibility(this.mOverlay);
        updateWindowVisibility(this.mBottomOverlay);
    }

    private void updateWindowVisibility(View view) {
        boolean z = true;
        int i = 0;
        boolean z2 = shouldDrawCutout() && view.findViewById(R$id.display_cutout).getVisibility() == 0;
        boolean hasRoundedCorners = hasRoundedCorners();
        if (!(view.findViewById(R$id.assist_hint_left).getVisibility() == 0 || view.findViewById(R$id.assist_hint_right).getVisibility() == 0)) {
            z = false;
        }
        if (!z2 && !hasRoundedCorners && !z) {
            i = 8;
        }
        view.setVisibility(i);
    }

    private boolean hasRoundedCorners() {
        return this.mHasRoundedCorner;
    }

    private boolean shouldDrawCutout() {
        return shouldDrawCutout(this.mContext);
    }

    static boolean shouldDrawCutout(Context context) {
        return context.getResources().getBoolean(17891457);
    }

    private void setupStatusBarPaddingIfNeeded() {
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R$dimen.rounded_corner_content_padding);
        if (dimensionPixelSize != 0) {
            setupStatusBarPadding(dimensionPixelSize);
        }
    }

    private void setupStatusBarPadding(int i) {
        StatusBar statusBar = (StatusBar) getComponent(StatusBar.class);
        StatusBarWindowView statusBarWindow = statusBar != null ? statusBar.getStatusBarWindow() : null;
        if (statusBarWindow != null) {
            TunablePadding.addTunablePadding(statusBarWindow.findViewById(R$id.keyguard_header), "sysui_rounded_content_padding", i, 2);
            FragmentHostManager fragmentHostManager = FragmentHostManager.get(statusBarWindow);
            fragmentHostManager.addTagListener("CollapsedStatusBarFragment", new TunablePaddingTagListener(i, R$id.status_bar));
            fragmentHostManager.addTagListener(C0959QS.TAG, new TunablePaddingTagListener(i, R$id.header));
        }
    }

    /* access modifiers changed from: 0000 */
    public WindowManager.LayoutParams getWindowLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -2, 2024, 545259816, -3);
        layoutParams.privateFlags |= 80;
        if (!DEBUG_SCREENSHOT_ROUNDED_CORNERS) {
            layoutParams.privateFlags |= 1048576;
        }
        layoutParams.setTitle("ScreenDecorOverlay");
        int i = this.mRotation;
        if (i == 2 || i == 3) {
            layoutParams.gravity = 85;
        } else {
            layoutParams.gravity = 51;
        }
        layoutParams.layoutInDisplayCutoutMode = 1;
        if (isLandscape(this.mRotation)) {
            layoutParams.width = -2;
            layoutParams.height = -1;
        }
        layoutParams.privateFlags |= 16777216;
        return layoutParams;
    }

    private WindowManager.LayoutParams getBottomLayoutParams() {
        WindowManager.LayoutParams windowLayoutParams = getWindowLayoutParams();
        windowLayoutParams.setTitle("ScreenDecorOverlayBottom");
        int i = this.mRotation;
        if (i == 2 || i == 3) {
            windowLayoutParams.gravity = 51;
        } else {
            windowLayoutParams.gravity = 85;
        }
        return windowLayoutParams;
    }

    /* access modifiers changed from: private */
    public void updateLayoutParams() {
        this.mWindowManager.updateViewLayout(this.mOverlay, getWindowLayoutParams());
        this.mWindowManager.updateViewLayout(this.mBottomOverlay, getBottomLayoutParams());
    }

    public void onTuningChanged(String str, String str2) {
        this.mHandler.post(new Runnable(str) {
            private final /* synthetic */ String f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                ScreenDecorations.this.lambda$onTuningChanged$5$ScreenDecorations(this.f$1);
            }
        });
    }

    public /* synthetic */ void lambda$onTuningChanged$5$ScreenDecorations(String str) {
        if (this.mOverlay != null) {
            "sysui_rounded_size".equals(str);
        }
    }

    private void setSize(View view, int i) {
        LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = i;
        layoutParams.height = i;
        view.setLayoutParams(layoutParams);
    }

    private void setSize(View view, int i, int i2) {
        LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = i2;
        layoutParams.height = i;
        view.setLayoutParams(layoutParams);
    }

    /* renamed from: onDarkIntensity */
    public void lambda$onDarkIntensity$6$ScreenDecorations(float f) {
        if (!this.mHandler.getLooper().isCurrentThread()) {
            this.mHandler.post(new Runnable(f) {
                private final /* synthetic */ float f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    ScreenDecorations.this.lambda$onDarkIntensity$6$ScreenDecorations(this.f$1);
                }
            });
            return;
        }
        View view = this.mOverlay;
        if (view != null) {
            CornerHandleView cornerHandleView = (CornerHandleView) this.mOverlay.findViewById(R$id.assist_hint_right);
            ((CornerHandleView) view.findViewById(R$id.assist_hint_left)).updateDarkness(f);
            cornerHandleView.updateDarkness(f);
        }
        View view2 = this.mBottomOverlay;
        if (view2 != null) {
            CornerHandleView cornerHandleView2 = (CornerHandleView) this.mBottomOverlay.findViewById(R$id.assist_hint_right);
            ((CornerHandleView) view2.findViewById(R$id.assist_hint_left)).updateDarkness(f);
            cornerHandleView2.updateDarkness(f);
        }
    }

    private void initRoundCornerView() {
        ImageView imageView = (ImageView) this.mOverlay.findViewById(R$id.right);
        ImageView imageView2 = (ImageView) this.mBottomOverlay.findViewById(R$id.left);
        ImageView imageView3 = (ImageView) this.mBottomOverlay.findViewById(R$id.right);
        ((ImageView) this.mOverlay.findViewById(R$id.left)).setImageResource(R$drawable.rounded_top);
        imageView.setImageResource(R$drawable.rounded_top);
        imageView2.setImageResource(R$drawable.rounded_bottom);
        imageView3.setImageResource(R$drawable.rounded_bottom);
        if (OpUtils.isSupportResolutionSwitch(this.mContext)) {
            float f = (float) this.mRoundedDefaultTop;
            float f2 = this.mResizeRatio;
            this.mRoundedDefaultTopResize = (int) (f * f2);
            this.mRoundedDefaultBottomResize = (int) (((float) this.mRoundedDefaultBottom) * f2);
            this.mRoundedDefaultTopWidthResize = (int) (((float) this.mRoundedDefaultTopWidth) * f2);
            this.mRoundedDefaultBottomWidthResize = (int) (((float) this.mRoundedDefaultBottomWidth) * f2);
        }
        updateDecorSize();
        imageView.setRotationY(180.0f);
        imageView3.setRotationY(180.0f);
    }

    /* access modifiers changed from: 0000 */
    public void updateDecorSize() {
        int i;
        int i2;
        int i3;
        int i4;
        if (this.mOverlay != null && this.mBottomOverlay != null) {
            if (OpUtils.isSupportResolutionSwitch(this.mContext)) {
                int i5 = this.mScreenResolution;
                if (i5 == 0 || i5 == 2) {
                    i4 = this.mRoundedDefaultTop;
                    i3 = this.mRoundedDefaultBottom;
                    i2 = this.mRoundedDefaultTopWidth;
                    i = this.mRoundedDefaultBottomWidth;
                } else {
                    i4 = this.mRoundedDefaultTopResize;
                    i3 = this.mRoundedDefaultBottomResize;
                    i2 = this.mRoundedDefaultTopWidthResize;
                    i = this.mRoundedDefaultBottomWidthResize;
                }
            } else {
                i4 = this.mRoundedDefaultTop;
                i3 = this.mRoundedDefaultBottom;
                i2 = this.mRoundedDefaultTopWidth;
                i = this.mRoundedDefaultBottomWidth;
            }
            if (i2 != 0) {
                int i6 = this.mRotation;
                if (i6 == 1 || i6 == 2) {
                    int i7 = i2;
                    i2 = i4;
                    i4 = i7;
                }
                setSize(this.mOverlay.findViewById(R$id.left), i4, i2);
                setSize(this.mOverlay.findViewById(R$id.right), i4, i2);
            } else {
                setSize(this.mOverlay.findViewById(R$id.left), i4);
                setSize(this.mOverlay.findViewById(R$id.right), i4);
            }
            if (i != 0) {
                int i8 = this.mRotation;
                if (i8 == 1 || i8 == 2) {
                    int i9 = i;
                    i = i3;
                    i3 = i9;
                }
                setSize(this.mBottomOverlay.findViewById(R$id.left), i3, i);
                setSize(this.mBottomOverlay.findViewById(R$id.right), i3, i);
                return;
            }
            setSize(this.mBottomOverlay.findViewById(R$id.left), i3);
            setSize(this.mBottomOverlay.findViewById(R$id.right), i3);
        }
    }

    private static Bitmap rotaingbitmap(int i, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate((float) i);
        Bitmap createBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (!(createBitmap == bitmap || bitmap == null || bitmap.isRecycled())) {
            bitmap.recycle();
        }
        return createBitmap;
    }

    /* access modifiers changed from: private */
    public boolean isRectangleTop() {
        int i = this.mRoundedDefaultTopWidth;
        return (i == 0 || this.mRoundedDefaultTop == i) ? false : true;
    }

    /* access modifiers changed from: private */
    public boolean isRectangleBottom() {
        int i = this.mRoundedDefaultBottomWidth;
        return (i == 0 || this.mRoundedDefaultBottom == i) ? false : true;
    }
}

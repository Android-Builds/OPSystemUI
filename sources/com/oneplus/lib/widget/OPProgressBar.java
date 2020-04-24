package com.oneplus.lib.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.ViewDebug.ExportedProperty;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.RemoteViews.RemoteView;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.util.DrawableUtils;
import com.oneplus.lib.util.MathUtils;
import com.oneplus.lib.util.Pools$SynchronizedPool;
import java.util.ArrayList;

@RemoteView
public class OPProgressBar extends View {
    private static final DecelerateInterpolator PROGRESS_ANIM_INTERPOLATOR = new DecelerateInterpolator();
    private final FloatProperty<OPProgressBar> VISUAL_PROGRESS;
    private AccessibilityEventSender mAccessibilityEventSender;
    private boolean mAggregatedIsVisible;
    private AlphaAnimation mAnimation;
    private boolean mAttached;
    private int mBehavior;
    private Drawable mCurrentDrawable;
    private int mDuration;
    private boolean mHasAnimation;
    private boolean mInDrawing;
    private boolean mIndeterminate;
    private Drawable mIndeterminateDrawable;
    private Interpolator mInterpolator;
    private int mMax;
    int mMaxHeight;
    int mMaxWidth;
    int mMinHeight;
    int mMinWidth;
    boolean mMirrorForRtl;
    private boolean mNoInvalidate;
    private boolean mOnlyIndeterminate;
    protected int mPaddingBottom;
    protected int mPaddingLeft;
    protected int mPaddingRight;
    protected int mPaddingTop;
    private int mProgress;
    private Drawable mProgressDrawable;
    private ProgressTintInfo mProgressTintInfo;
    /* access modifiers changed from: private */
    public final ArrayList<RefreshData> mRefreshData;
    /* access modifiers changed from: private */
    public boolean mRefreshIsPosted;
    private RefreshProgressRunnable mRefreshProgressRunnable;
    Bitmap mSampleTile;
    private int mSecondaryProgress;
    private boolean mShouldStartAnimationDrawable;
    private Transformation mTransformation;
    private long mUiThreadId;
    /* access modifiers changed from: private */
    public float mVisualProgress;

    private class AccessibilityEventSender implements Runnable {
    }

    private static class ProgressTintInfo {
        boolean mHasIndeterminateTint;
        boolean mHasIndeterminateTintMode;
        boolean mHasProgressBackgroundTint;
        boolean mHasProgressBackgroundTintMode;
        boolean mHasProgressTint;
        boolean mHasProgressTintMode;
        boolean mHasSecondaryProgressTint;
        boolean mHasSecondaryProgressTintMode;
        ColorStateList mIndeterminateTintList;
        Mode mIndeterminateTintMode;
        ColorStateList mProgressBackgroundTintList;
        Mode mProgressBackgroundTintMode;
        ColorStateList mProgressTintList;
        Mode mProgressTintMode;
        ColorStateList mSecondaryProgressTintList;
        Mode mSecondaryProgressTintMode;

        private ProgressTintInfo() {
        }
    }

    private static class RefreshData {
        private static final Pools$SynchronizedPool<RefreshData> sPool = new Pools$SynchronizedPool<>(24);
        public boolean animate;
        public boolean fromUser;

        /* renamed from: id */
        public int f117id;
        public int progress;

        private RefreshData() {
        }

        public static RefreshData obtain(int i, int i2, boolean z, boolean z2) {
            RefreshData refreshData = (RefreshData) sPool.acquire();
            if (refreshData == null) {
                refreshData = new RefreshData();
            }
            refreshData.f117id = i;
            refreshData.progress = i2;
            refreshData.fromUser = z;
            refreshData.animate = z2;
            return refreshData;
        }

        public void recycle() {
            sPool.release(this);
        }
    }

    private class RefreshProgressRunnable implements Runnable {
        private RefreshProgressRunnable() {
        }

        public void run() {
            synchronized (OPProgressBar.this) {
                int size = OPProgressBar.this.mRefreshData.size();
                for (int i = 0; i < size; i++) {
                    RefreshData refreshData = (RefreshData) OPProgressBar.this.mRefreshData.get(i);
                    OPProgressBar.this.doRefreshProgress(refreshData.f117id, refreshData.progress, refreshData.fromUser, true, refreshData.animate);
                    refreshData.recycle();
                }
                OPProgressBar.this.mRefreshData.clear();
                OPProgressBar.this.mRefreshIsPosted = false;
            }
        }
    }

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        int progress;
        int secondaryProgress;

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.progress = parcel.readInt();
            this.secondaryProgress = parcel.readInt();
        }

        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.progress);
            parcel.writeInt(this.secondaryProgress);
        }
    }

    /* access modifiers changed from: 0000 */
    public void onProgressRefresh(float f, boolean z, int i) {
    }

    /* access modifiers changed from: 0000 */
    public void onVisualProgressChanged(int i, float f) {
    }

    public OPProgressBar(Context context) {
        this(context, null);
    }

    public OPProgressBar(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842871);
    }

    public OPProgressBar(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public OPProgressBar(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        boolean z = false;
        this.mMirrorForRtl = false;
        this.mRefreshData = new ArrayList<>();
        this.VISUAL_PROGRESS = new FloatProperty<OPProgressBar>("visual_progress") {
            public void setValue(OPProgressBar oPProgressBar, float f) {
                oPProgressBar.setVisualProgress(16908301, f);
                oPProgressBar.mVisualProgress = f;
            }

            public Float get(OPProgressBar oPProgressBar) {
                return Float.valueOf(oPProgressBar.mVisualProgress);
            }
        };
        this.mPaddingRight = getPaddingRight();
        this.mPaddingTop = getPaddingTop();
        this.mPaddingLeft = getPaddingLeft();
        this.mPaddingBottom = getPaddingBottom();
        this.mUiThreadId = Thread.currentThread().getId();
        initProgressBar();
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPProgressBar, i, i2);
        this.mNoInvalidate = true;
        Drawable drawable = obtainStyledAttributes.getDrawable(R$styleable.OPProgressBar_android_progressDrawable);
        if (drawable != null) {
            if (needsTileify(drawable)) {
                setProgressDrawableTiled(drawable);
            } else {
                setProgressDrawable(drawable);
            }
        }
        this.mDuration = obtainStyledAttributes.getInt(R$styleable.OPProgressBar_android_indeterminateDuration, this.mDuration);
        this.mMinWidth = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPProgressBar_android_minWidth, this.mMinWidth);
        this.mMaxWidth = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPProgressBar_android_maxWidth, this.mMaxWidth);
        this.mMinHeight = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPProgressBar_android_minHeight, this.mMinHeight);
        this.mMaxHeight = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPProgressBar_android_maxHeight, this.mMaxHeight);
        this.mBehavior = obtainStyledAttributes.getInt(R$styleable.OPProgressBar_android_indeterminateBehavior, this.mBehavior);
        int resourceId = obtainStyledAttributes.getResourceId(R$styleable.OPProgressBar_android_interpolator, 17432587);
        if (resourceId > 0) {
            setInterpolator(context, resourceId);
        }
        setMax(obtainStyledAttributes.getInt(R$styleable.OPProgressBar_android_max, this.mMax));
        setProgress(obtainStyledAttributes.getInt(R$styleable.OPProgressBar_android_progress, this.mProgress));
        setSecondaryProgress(obtainStyledAttributes.getInt(R$styleable.OPProgressBar_android_secondaryProgress, this.mSecondaryProgress));
        Drawable drawable2 = obtainStyledAttributes.getDrawable(R$styleable.OPProgressBar_android_indeterminateDrawable);
        if (drawable2 != null) {
            if (needsTileify(drawable2)) {
                setIndeterminateDrawableTiled(drawable2);
            } else {
                setIndeterminateDrawable(drawable2);
            }
        }
        this.mOnlyIndeterminate = obtainStyledAttributes.getBoolean(R$styleable.OPProgressBar_android_indeterminateOnly, this.mOnlyIndeterminate);
        this.mNoInvalidate = false;
        if (this.mOnlyIndeterminate || obtainStyledAttributes.getBoolean(R$styleable.OPProgressBar_android_indeterminate, this.mIndeterminate)) {
            z = true;
        }
        setIndeterminate(z);
        this.mMirrorForRtl = obtainStyledAttributes.getBoolean(R$styleable.OPProgressBar_android_mirrorForRtl, this.mMirrorForRtl);
        if (obtainStyledAttributes.hasValue(R$styleable.OPProgressBar_android_progressTintMode)) {
            if (this.mProgressTintInfo == null) {
                this.mProgressTintInfo = new ProgressTintInfo();
            }
            this.mProgressTintInfo.mProgressTintMode = DrawableUtils.parseTintMode(obtainStyledAttributes.getInt(R$styleable.OPProgressBar_android_progressTintMode, -1), null);
            this.mProgressTintInfo.mHasProgressTintMode = true;
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPProgressBar_android_progressTint)) {
            if (this.mProgressTintInfo == null) {
                this.mProgressTintInfo = new ProgressTintInfo();
            }
            this.mProgressTintInfo.mProgressTintList = obtainStyledAttributes.getColorStateList(R$styleable.OPProgressBar_android_progressTint);
            this.mProgressTintInfo.mHasProgressTint = true;
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPProgressBar_android_progressBackgroundTintMode)) {
            if (this.mProgressTintInfo == null) {
                this.mProgressTintInfo = new ProgressTintInfo();
            }
            this.mProgressTintInfo.mProgressBackgroundTintMode = DrawableUtils.parseTintMode(obtainStyledAttributes.getInt(R$styleable.OPProgressBar_android_progressBackgroundTintMode, -1), null);
            this.mProgressTintInfo.mHasProgressBackgroundTintMode = true;
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPProgressBar_android_progressBackgroundTint)) {
            if (this.mProgressTintInfo == null) {
                this.mProgressTintInfo = new ProgressTintInfo();
            }
            this.mProgressTintInfo.mProgressBackgroundTintList = obtainStyledAttributes.getColorStateList(R$styleable.OPProgressBar_android_progressBackgroundTint);
            this.mProgressTintInfo.mHasProgressBackgroundTint = true;
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPProgressBar_android_secondaryProgressTintMode)) {
            if (this.mProgressTintInfo == null) {
                this.mProgressTintInfo = new ProgressTintInfo();
            }
            this.mProgressTintInfo.mSecondaryProgressTintMode = DrawableUtils.parseTintMode(obtainStyledAttributes.getInt(R$styleable.OPProgressBar_android_secondaryProgressTintMode, -1), null);
            this.mProgressTintInfo.mHasSecondaryProgressTintMode = true;
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPProgressBar_android_secondaryProgressTint)) {
            if (this.mProgressTintInfo == null) {
                this.mProgressTintInfo = new ProgressTintInfo();
            }
            this.mProgressTintInfo.mSecondaryProgressTintList = obtainStyledAttributes.getColorStateList(R$styleable.OPProgressBar_android_secondaryProgressTint);
            this.mProgressTintInfo.mHasSecondaryProgressTint = true;
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPProgressBar_android_indeterminateTintMode)) {
            if (this.mProgressTintInfo == null) {
                this.mProgressTintInfo = new ProgressTintInfo();
            }
            this.mProgressTintInfo.mIndeterminateTintMode = DrawableUtils.parseTintMode(obtainStyledAttributes.getInt(R$styleable.OPProgressBar_android_indeterminateTintMode, -1), null);
            this.mProgressTintInfo.mHasIndeterminateTintMode = true;
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPProgressBar_android_indeterminateTint)) {
            if (this.mProgressTintInfo == null) {
                this.mProgressTintInfo = new ProgressTintInfo();
            }
            this.mProgressTintInfo.mIndeterminateTintList = obtainStyledAttributes.getColorStateList(R$styleable.OPProgressBar_android_indeterminateTint);
            this.mProgressTintInfo.mHasIndeterminateTint = true;
        }
        obtainStyledAttributes.recycle();
        applyProgressTints();
        applyIndeterminateTint();
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
    }

    private static boolean needsTileify(Drawable drawable) {
        if (!(drawable instanceof LayerDrawable)) {
            return drawable instanceof BitmapDrawable;
        }
        LayerDrawable layerDrawable = (LayerDrawable) drawable;
        int numberOfLayers = layerDrawable.getNumberOfLayers();
        for (int i = 0; i < numberOfLayers; i++) {
            if (needsTileify(layerDrawable.getDrawable(i))) {
                return true;
            }
        }
        return false;
    }

    private Drawable tileify(Drawable drawable, boolean z) {
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            int numberOfLayers = layerDrawable.getNumberOfLayers();
            Drawable[] drawableArr = new Drawable[numberOfLayers];
            for (int i = 0; i < numberOfLayers; i++) {
                int id = layerDrawable.getId(i);
                drawableArr[i] = tileify(layerDrawable.getDrawable(i), id == 16908301 || id == 16908303);
            }
            LayerDrawable layerDrawable2 = new LayerDrawable(drawableArr);
            for (int i2 = 0; i2 < numberOfLayers; i2++) {
                layerDrawable2.setId(i2, layerDrawable.getId(i2));
                layerDrawable2.setLayerGravity(i2, layerDrawable.getLayerGravity(i2));
                layerDrawable2.setLayerWidth(i2, layerDrawable.getLayerWidth(i2));
                layerDrawable2.setLayerHeight(i2, layerDrawable.getLayerHeight(i2));
                layerDrawable2.setLayerInsetLeft(i2, layerDrawable.getLayerInsetLeft(i2));
                layerDrawable2.setLayerInsetRight(i2, layerDrawable.getLayerInsetRight(i2));
                layerDrawable2.setLayerInsetTop(i2, layerDrawable.getLayerInsetTop(i2));
                layerDrawable2.setLayerInsetBottom(i2, layerDrawable.getLayerInsetBottom(i2));
                layerDrawable2.setLayerInsetStart(i2, layerDrawable.getLayerInsetStart(i2));
                layerDrawable2.setLayerInsetEnd(i2, layerDrawable.getLayerInsetEnd(i2));
            }
            return layerDrawable2;
        } else if (!(drawable instanceof BitmapDrawable)) {
            return drawable;
        } else {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (this.mSampleTile == null) {
                this.mSampleTile = bitmap;
            }
            BitmapDrawable bitmapDrawable2 = (BitmapDrawable) bitmapDrawable.getConstantState().newDrawable();
            bitmapDrawable2.setTileModeXY(TileMode.REPEAT, TileMode.CLAMP);
            return z ? new ClipDrawable(bitmapDrawable2, 3, 1) : bitmapDrawable2;
        }
    }

    private Drawable tileifyIndeterminate(Drawable drawable) {
        if (!(drawable instanceof AnimationDrawable)) {
            return drawable;
        }
        AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
        int numberOfFrames = animationDrawable.getNumberOfFrames();
        AnimationDrawable animationDrawable2 = new AnimationDrawable();
        animationDrawable2.setOneShot(animationDrawable.isOneShot());
        for (int i = 0; i < numberOfFrames; i++) {
            Drawable tileify = tileify(animationDrawable.getFrame(i), true);
            tileify.setLevel(10000);
            animationDrawable2.addFrame(tileify, animationDrawable.getDuration(i));
        }
        animationDrawable2.setLevel(10000);
        return animationDrawable2;
    }

    private void initProgressBar() {
        this.mMax = 100;
        this.mProgress = 0;
        this.mSecondaryProgress = 0;
        this.mIndeterminate = false;
        this.mOnlyIndeterminate = false;
        this.mDuration = 4000;
        this.mBehavior = 1;
        this.mMinWidth = 24;
        this.mMaxWidth = 48;
        this.mMinHeight = 24;
        this.mMaxHeight = 48;
    }

    public synchronized void setIndeterminate(boolean z) {
        if ((!this.mOnlyIndeterminate || !this.mIndeterminate) && z != this.mIndeterminate) {
            this.mIndeterminate = z;
            if (z) {
                swapCurrentDrawable(this.mIndeterminateDrawable);
                startAnimation();
            } else {
                swapCurrentDrawable(this.mProgressDrawable);
                stopAnimation();
            }
        }
    }

    private void swapCurrentDrawable(Drawable drawable) {
        Drawable drawable2 = this.mCurrentDrawable;
        this.mCurrentDrawable = drawable;
        if (drawable2 != this.mCurrentDrawable) {
            if (drawable2 != null) {
                drawable2.setVisible(false, false);
            }
            Drawable drawable3 = this.mCurrentDrawable;
            if (drawable3 != null) {
                drawable3.setVisible(getWindowVisibility() == 0 && isShown(), false);
            }
        }
    }

    public void setIndeterminateDrawable(Drawable drawable) {
        Drawable drawable2 = this.mIndeterminateDrawable;
        if (drawable2 != drawable) {
            if (drawable2 != null) {
                drawable2.setCallback(null);
                unscheduleDrawable(this.mIndeterminateDrawable);
            }
            this.mIndeterminateDrawable = drawable;
            if (drawable != null) {
                drawable.setCallback(this);
                drawable.setLayoutDirection(getLayoutDirection());
                if (drawable.isStateful()) {
                    drawable.setState(getDrawableState());
                }
                applyIndeterminateTint();
            }
            if (this.mIndeterminate) {
                swapCurrentDrawable(drawable);
                postInvalidate();
            }
        }
    }

    private void applyIndeterminateTint() {
        if (this.mIndeterminateDrawable != null) {
            ProgressTintInfo progressTintInfo = this.mProgressTintInfo;
            if (progressTintInfo == null) {
                return;
            }
            if (progressTintInfo.mHasIndeterminateTint || progressTintInfo.mHasIndeterminateTintMode) {
                this.mIndeterminateDrawable = this.mIndeterminateDrawable.mutate();
                if (progressTintInfo.mHasIndeterminateTint) {
                    this.mIndeterminateDrawable.setTintList(progressTintInfo.mIndeterminateTintList);
                }
                if (progressTintInfo.mHasIndeterminateTintMode) {
                    this.mIndeterminateDrawable.setTintMode(progressTintInfo.mIndeterminateTintMode);
                }
                if (this.mIndeterminateDrawable.isStateful()) {
                    this.mIndeterminateDrawable.setState(getDrawableState());
                }
            }
        }
    }

    public void setIndeterminateDrawableTiled(Drawable drawable) {
        if (drawable != null) {
            drawable = tileifyIndeterminate(drawable);
        }
        setIndeterminateDrawable(drawable);
    }

    public Drawable getProgressDrawable() {
        return this.mProgressDrawable;
    }

    public void setProgressDrawable(Drawable drawable) {
        Drawable drawable2 = this.mProgressDrawable;
        if (drawable2 != drawable) {
            if (drawable2 != null) {
                drawable2.setCallback(null);
                unscheduleDrawable(this.mProgressDrawable);
            }
            this.mProgressDrawable = drawable;
            if (drawable != null) {
                drawable.setCallback(this);
                drawable.setLayoutDirection(getLayoutDirection());
                if (drawable.isStateful()) {
                    drawable.setState(getDrawableState());
                }
                int minimumHeight = drawable.getMinimumHeight();
                if (this.mMaxHeight < minimumHeight) {
                    this.mMaxHeight = minimumHeight;
                    requestLayout();
                }
                applyProgressTints();
            }
            if (!this.mIndeterminate) {
                swapCurrentDrawable(drawable);
                postInvalidate();
            }
            updateDrawableBounds(getWidth(), getHeight());
            updateDrawableState();
            doRefreshProgress(16908301, this.mProgress, false, false, false);
            doRefreshProgress(16908303, this.mSecondaryProgress, false, false, false);
        }
    }

    private void applyProgressTints() {
        if (this.mProgressDrawable != null && this.mProgressTintInfo != null) {
            applyPrimaryProgressTint();
            applyProgressBackgroundTint();
            applySecondaryProgressTint();
        }
    }

    private void applyPrimaryProgressTint() {
        ProgressTintInfo progressTintInfo = this.mProgressTintInfo;
        if (progressTintInfo.mHasProgressTint || progressTintInfo.mHasProgressTintMode) {
            Drawable tintTarget = getTintTarget(16908301, true);
            if (tintTarget != null) {
                ProgressTintInfo progressTintInfo2 = this.mProgressTintInfo;
                if (progressTintInfo2.mHasProgressTint) {
                    tintTarget.setTintList(progressTintInfo2.mProgressTintList);
                }
                ProgressTintInfo progressTintInfo3 = this.mProgressTintInfo;
                if (progressTintInfo3.mHasProgressTintMode) {
                    tintTarget.setTintMode(progressTintInfo3.mProgressTintMode);
                }
                if (tintTarget.isStateful()) {
                    tintTarget.setState(getDrawableState());
                }
            }
        }
    }

    private void applyProgressBackgroundTint() {
        ProgressTintInfo progressTintInfo = this.mProgressTintInfo;
        if (progressTintInfo.mHasProgressBackgroundTint || progressTintInfo.mHasProgressBackgroundTintMode) {
            Drawable tintTarget = getTintTarget(16908288, false);
            if (tintTarget != null) {
                ProgressTintInfo progressTintInfo2 = this.mProgressTintInfo;
                if (progressTintInfo2.mHasProgressBackgroundTint) {
                    tintTarget.setTintList(progressTintInfo2.mProgressBackgroundTintList);
                }
                ProgressTintInfo progressTintInfo3 = this.mProgressTintInfo;
                if (progressTintInfo3.mHasProgressBackgroundTintMode) {
                    tintTarget.setTintMode(progressTintInfo3.mProgressBackgroundTintMode);
                }
                if (tintTarget.isStateful()) {
                    tintTarget.setState(getDrawableState());
                }
            }
        }
    }

    private void applySecondaryProgressTint() {
        ProgressTintInfo progressTintInfo = this.mProgressTintInfo;
        if (progressTintInfo.mHasSecondaryProgressTint || progressTintInfo.mHasSecondaryProgressTintMode) {
            Drawable tintTarget = getTintTarget(16908303, false);
            if (tintTarget != null) {
                ProgressTintInfo progressTintInfo2 = this.mProgressTintInfo;
                if (progressTintInfo2.mHasSecondaryProgressTint) {
                    tintTarget.setTintList(progressTintInfo2.mSecondaryProgressTintList);
                }
                ProgressTintInfo progressTintInfo3 = this.mProgressTintInfo;
                if (progressTintInfo3.mHasSecondaryProgressTintMode) {
                    tintTarget.setTintMode(progressTintInfo3.mSecondaryProgressTintMode);
                }
                if (tintTarget.isStateful()) {
                    tintTarget.setState(getDrawableState());
                }
            }
        }
    }

    private Drawable getTintTarget(int i, boolean z) {
        Drawable drawable = this.mProgressDrawable;
        if (drawable == null) {
            return null;
        }
        this.mProgressDrawable = drawable.mutate();
        Drawable findDrawableByLayerId = drawable instanceof LayerDrawable ? ((LayerDrawable) drawable).findDrawableByLayerId(i) : null;
        return (!z || findDrawableByLayerId != null) ? findDrawableByLayerId : drawable;
    }

    public void setProgressDrawableTiled(Drawable drawable) {
        if (drawable != null) {
            drawable = tileify(drawable, false);
        }
        setProgressDrawable(drawable);
    }

    /* access modifiers changed from: 0000 */
    public Drawable getCurrentDrawable() {
        return this.mCurrentDrawable;
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable drawable) {
        return drawable == this.mProgressDrawable || drawable == this.mIndeterminateDrawable || super.verifyDrawable(drawable);
    }

    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        Drawable drawable = this.mProgressDrawable;
        if (drawable != null) {
            drawable.jumpToCurrentState();
        }
        Drawable drawable2 = this.mIndeterminateDrawable;
        if (drawable2 != null) {
            drawable2.jumpToCurrentState();
        }
    }

    public void onResolveDrawables(int i) {
        Drawable drawable = this.mCurrentDrawable;
        if (drawable != null) {
            drawable.setLayoutDirection(i);
        }
        Drawable drawable2 = this.mIndeterminateDrawable;
        if (drawable2 != null) {
            drawable2.setLayoutDirection(i);
        }
        Drawable drawable3 = this.mProgressDrawable;
        if (drawable3 != null) {
            drawable3.setLayoutDirection(i);
        }
    }

    public void postInvalidate() {
        if (!this.mNoInvalidate) {
            super.postInvalidate();
        }
    }

    /* access modifiers changed from: private */
    public synchronized void doRefreshProgress(int i, int i2, boolean z, boolean z2, boolean z3) {
        float f = this.mMax > 0 ? ((float) i2) / ((float) this.mMax) : 0.0f;
        if (i != 16908301 || !z3) {
            setVisualProgress(i, f);
        } else {
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, this.VISUAL_PROGRESS, new float[]{f});
            ofFloat.setAutoCancel(true);
            ofFloat.setDuration(80);
            ofFloat.setInterpolator(PROGRESS_ANIM_INTERPOLATOR);
            ofFloat.start();
        }
        if (z2 && i == 16908301) {
            onProgressRefresh(f, z, i2);
        }
    }

    /* access modifiers changed from: private */
    public void setVisualProgress(int i, float f) {
        this.mVisualProgress = f;
        Drawable drawable = this.mCurrentDrawable;
        if (drawable != null) {
            Drawable drawable2 = null;
            if (drawable instanceof LayerDrawable) {
                drawable2 = ((LayerDrawable) drawable).findDrawableByLayerId(i);
                if (drawable2 != null && canResolveLayoutDirection()) {
                    drawable2.setLayoutDirection(getLayoutDirection());
                }
            }
            int i2 = (int) (10000.0f * f);
            if (drawable2 != null) {
                drawable = drawable2;
            }
            drawable.setLevel(i2);
        } else {
            invalidate();
        }
        onVisualProgressChanged(i, f);
    }

    private synchronized void refreshProgress(int i, int i2, boolean z, boolean z2) {
        if (this.mUiThreadId == Thread.currentThread().getId()) {
            doRefreshProgress(i, i2, z, true, z2);
        } else {
            if (this.mRefreshProgressRunnable == null) {
                this.mRefreshProgressRunnable = new RefreshProgressRunnable();
            }
            this.mRefreshData.add(RefreshData.obtain(i, i2, z, z2));
            if (this.mAttached && !this.mRefreshIsPosted) {
                post(this.mRefreshProgressRunnable);
                this.mRefreshIsPosted = true;
            }
        }
    }

    public synchronized void setProgress(int i) {
        setProgressInternal(i, false, false);
    }

    /* access modifiers changed from: 0000 */
    public synchronized boolean setProgressInternal(int i, boolean z, boolean z2) {
        if (this.mIndeterminate) {
            return false;
        }
        int constrain = MathUtils.constrain(i, 0, this.mMax);
        if (constrain == this.mProgress) {
            return false;
        }
        this.mProgress = constrain;
        refreshProgress(16908301, this.mProgress, z, z2);
        return true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0020, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void setSecondaryProgress(int r3) {
        /*
            r2 = this;
            monitor-enter(r2)
            boolean r0 = r2.mIndeterminate     // Catch:{ all -> 0x0021 }
            if (r0 == 0) goto L_0x0007
            monitor-exit(r2)
            return
        L_0x0007:
            r0 = 0
            if (r3 >= 0) goto L_0x000b
            r3 = r0
        L_0x000b:
            int r1 = r2.mMax     // Catch:{ all -> 0x0021 }
            if (r3 <= r1) goto L_0x0011
            int r3 = r2.mMax     // Catch:{ all -> 0x0021 }
        L_0x0011:
            int r1 = r2.mSecondaryProgress     // Catch:{ all -> 0x0021 }
            if (r3 == r1) goto L_0x001f
            r2.mSecondaryProgress = r3     // Catch:{ all -> 0x0021 }
            r3 = 16908303(0x102000f, float:2.387727E-38)
            int r1 = r2.mSecondaryProgress     // Catch:{ all -> 0x0021 }
            r2.refreshProgress(r3, r1, r0, r0)     // Catch:{ all -> 0x0021 }
        L_0x001f:
            monitor-exit(r2)
            return
        L_0x0021:
            r3 = move-exception
            monitor-exit(r2)
            throw r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.OPProgressBar.setSecondaryProgress(int):void");
    }

    @ExportedProperty(category = "progress")
    public synchronized int getProgress() {
        return this.mIndeterminate ? 0 : this.mProgress;
    }

    @ExportedProperty(category = "progress")
    public synchronized int getMax() {
        return this.mMax;
    }

    public synchronized void setMax(int i) {
        if (i < 0) {
            i = 0;
        }
        if (i != this.mMax) {
            this.mMax = i;
            postInvalidate();
            if (this.mProgress > i) {
                this.mProgress = i;
            }
            refreshProgress(16908301, this.mProgress, false, false);
        }
    }

    /* access modifiers changed from: 0000 */
    public void startAnimation() {
        if (getVisibility() == 0 && getWindowVisibility() == 0) {
            if (this.mIndeterminateDrawable instanceof Animatable) {
                this.mShouldStartAnimationDrawable = true;
                this.mHasAnimation = false;
            } else {
                this.mHasAnimation = true;
                if (this.mInterpolator == null) {
                    this.mInterpolator = new LinearInterpolator();
                }
                Transformation transformation = this.mTransformation;
                if (transformation == null) {
                    this.mTransformation = new Transformation();
                } else {
                    transformation.clear();
                }
                AlphaAnimation alphaAnimation = this.mAnimation;
                if (alphaAnimation == null) {
                    this.mAnimation = new AlphaAnimation(0.0f, 1.0f);
                } else {
                    alphaAnimation.reset();
                }
                this.mAnimation.setRepeatMode(this.mBehavior);
                this.mAnimation.setRepeatCount(-1);
                this.mAnimation.setDuration((long) this.mDuration);
                this.mAnimation.setInterpolator(this.mInterpolator);
                this.mAnimation.setStartTime(-1);
            }
            postInvalidate();
        }
    }

    /* access modifiers changed from: 0000 */
    public void stopAnimation() {
        this.mHasAnimation = false;
        Drawable drawable = this.mIndeterminateDrawable;
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).stop();
            this.mShouldStartAnimationDrawable = false;
        }
        postInvalidate();
    }

    public void setInterpolator(Context context, int i) {
        setInterpolator(AnimationUtils.loadInterpolator(context, i));
    }

    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    public void onVisibilityAggregated(boolean z) {
        super.onVisibilityAggregated(z);
        if (z != this.mAggregatedIsVisible) {
            this.mAggregatedIsVisible = z;
            if (this.mIndeterminate) {
                if (z) {
                    startAnimation();
                } else {
                    stopAnimation();
                }
            }
            Drawable drawable = this.mCurrentDrawable;
            if (drawable != null) {
                drawable.setVisible(z, false);
            }
        }
    }

    public void invalidateDrawable(Drawable drawable) {
        if (this.mInDrawing) {
            return;
        }
        if (verifyDrawable(drawable)) {
            Rect bounds = drawable.getBounds();
            int scrollX = getScrollX() + this.mPaddingLeft;
            int scrollY = getScrollY() + this.mPaddingTop;
            invalidate(bounds.left + scrollX, bounds.top + scrollY, bounds.right + scrollX, bounds.bottom + scrollY);
            return;
        }
        super.invalidateDrawable(drawable);
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        updateDrawableBounds(i, i2);
    }

    private void updateDrawableBounds(int i, int i2) {
        int i3;
        int i4;
        int i5;
        int i6;
        int i7 = i - (this.mPaddingRight + this.mPaddingLeft);
        int i8 = i2 - (this.mPaddingTop + this.mPaddingBottom);
        Drawable drawable = this.mIndeterminateDrawable;
        if (drawable != null) {
            if (this.mOnlyIndeterminate && !(drawable instanceof AnimationDrawable)) {
                float intrinsicWidth = ((float) drawable.getIntrinsicWidth()) / ((float) this.mIndeterminateDrawable.getIntrinsicHeight());
                float f = (float) i7;
                float f2 = (float) i8;
                float f3 = f / f2;
                if (intrinsicWidth != f3) {
                    if (f3 > intrinsicWidth) {
                        int i9 = (int) (f2 * intrinsicWidth);
                        i4 = (i7 - i9) / 2;
                        i5 = i9 + i4;
                        i3 = 0;
                    } else {
                        int i10 = (int) (f * (1.0f / intrinsicWidth));
                        int i11 = (i8 - i10) / 2;
                        i3 = i11;
                        i8 = i10 + i11;
                        i4 = 0;
                        i5 = i7;
                    }
                    if (isLayoutRtl() || !this.mMirrorForRtl) {
                        i7 = i5;
                        i6 = i4;
                    } else {
                        i6 = i7 - i5;
                        i7 -= i4;
                    }
                    this.mIndeterminateDrawable.setBounds(i6, i3, i7, i8);
                }
            }
            i5 = i7;
            i4 = 0;
            i3 = 0;
            if (isLayoutRtl()) {
            }
            i7 = i5;
            i6 = i4;
            this.mIndeterminateDrawable.setBounds(i6, i3, i7, i8);
        }
        Drawable drawable2 = this.mProgressDrawable;
        if (drawable2 != null) {
            drawable2.setBounds(0, 0, i7, i8);
        }
    }

    /* access modifiers changed from: protected */
    public synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTrack(canvas);
    }

    /* JADX INFO: finally extract failed */
    /* access modifiers changed from: 0000 */
    public void drawTrack(Canvas canvas) {
        Drawable drawable = this.mCurrentDrawable;
        if (drawable != null) {
            int save = canvas.save();
            if (!isLayoutRtl() || !this.mMirrorForRtl) {
                canvas.translate((float) this.mPaddingLeft, (float) this.mPaddingTop);
            } else {
                canvas.translate((float) (getWidth() - this.mPaddingRight), (float) this.mPaddingTop);
                canvas.scale(-1.0f, 1.0f);
            }
            long drawingTime = getDrawingTime();
            if (this.mHasAnimation) {
                this.mAnimation.getTransformation(drawingTime, this.mTransformation);
                float alpha = this.mTransformation.getAlpha();
                try {
                    this.mInDrawing = true;
                    drawable.setLevel((int) (alpha * 10000.0f));
                    this.mInDrawing = false;
                    postInvalidateOnAnimation();
                } catch (Throwable th) {
                    this.mInDrawing = false;
                    throw th;
                }
            }
            drawable.draw(canvas);
            canvas.restoreToCount(save);
            if (this.mShouldStartAnimationDrawable && (drawable instanceof Animatable)) {
                ((Animatable) drawable).start();
                this.mShouldStartAnimationDrawable = false;
            }
        }
    }

    /* access modifiers changed from: protected */
    public synchronized void onMeasure(int i, int i2) {
        int i3;
        int i4;
        Drawable drawable = this.mCurrentDrawable;
        if (drawable != null) {
            i3 = Math.max(this.mMinWidth, Math.min(this.mMaxWidth, drawable.getIntrinsicWidth()));
            i4 = Math.max(this.mMinHeight, Math.min(this.mMaxHeight, drawable.getIntrinsicHeight()));
        } else {
            i4 = 0;
            i3 = 0;
        }
        updateDrawableState();
        setMeasuredDimension(View.resolveSizeAndState(i3 + this.mPaddingLeft + this.mPaddingRight, i, 0), View.resolveSizeAndState(i4 + this.mPaddingTop + this.mPaddingBottom, i2, 0));
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        updateDrawableState();
    }

    private void updateDrawableState() {
        int[] drawableState = getDrawableState();
        Drawable drawable = this.mProgressDrawable;
        boolean z = false;
        if (drawable != null && drawable.isStateful()) {
            z = false | drawable.setState(drawableState);
        }
        Drawable drawable2 = this.mIndeterminateDrawable;
        if (drawable2 != null && drawable2.isStateful()) {
            z |= drawable2.setState(drawableState);
        }
        if (z) {
            invalidate();
        }
    }

    public void drawableHotspotChanged(float f, float f2) {
        super.drawableHotspotChanged(f, f2);
        Drawable drawable = this.mProgressDrawable;
        if (drawable != null) {
            drawable.setHotspot(f, f2);
        }
        Drawable drawable2 = this.mIndeterminateDrawable;
        if (drawable2 != null) {
            drawable2.setHotspot(f, f2);
        }
    }

    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.progress = this.mProgress;
        savedState.secondaryProgress = this.mSecondaryProgress;
        return savedState;
    }

    public void onRestoreInstanceState(Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        setProgress(savedState.progress);
        setSecondaryProgress(savedState.secondaryProgress);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mIndeterminate) {
            startAnimation();
        }
        if (this.mRefreshData != null) {
            synchronized (this) {
                int size = this.mRefreshData.size();
                for (int i = 0; i < size; i++) {
                    RefreshData refreshData = (RefreshData) this.mRefreshData.get(i);
                    doRefreshProgress(refreshData.f117id, refreshData.progress, refreshData.fromUser, true, refreshData.animate);
                    refreshData.recycle();
                }
                this.mRefreshData.clear();
            }
        }
        this.mAttached = true;
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        if (this.mIndeterminate) {
            stopAnimation();
        }
        RefreshProgressRunnable refreshProgressRunnable = this.mRefreshProgressRunnable;
        if (refreshProgressRunnable != null) {
            removeCallbacks(refreshProgressRunnable);
            this.mRefreshIsPosted = false;
        }
        AccessibilityEventSender accessibilityEventSender = this.mAccessibilityEventSender;
        if (accessibilityEventSender != null) {
            removeCallbacks(accessibilityEventSender);
        }
        super.onDetachedFromWindow();
        this.mAttached = false;
    }

    public CharSequence getAccessibilityClassName() {
        return OPProgressBar.class.getName();
    }

    public boolean isLayoutRtl() {
        return getLayoutDirection() == 1;
    }
}

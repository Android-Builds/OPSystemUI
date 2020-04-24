package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.util.DrawableUtils;

public abstract class OPAbsSeekBar extends OPProgressBar {
    private float mDisabledAlpha;
    private boolean mHasThumbTint;
    private boolean mHasThumbTintMode;
    private boolean mIsDragging;
    boolean mIsUserSeekable;
    private int mKeyProgressIncrement;
    private int mScaledTouchSlop;
    private boolean mSplitTrack;
    private final Rect mTempRect;
    private Drawable mThumb;
    private int mThumbOffset;
    private ColorStateList mThumbTintList;
    private Mode mThumbTintMode;
    private float mTouchDownX;
    float mTouchProgressOffset;

    /* access modifiers changed from: 0000 */
    public void onKeyChange() {
    }

    public OPAbsSeekBar(Context context) {
        super(context);
        this.mTempRect = new Rect();
        this.mThumbTintList = null;
        this.mThumbTintMode = null;
        this.mHasThumbTint = false;
        this.mHasThumbTintMode = false;
        this.mIsUserSeekable = true;
        this.mKeyProgressIncrement = 1;
    }

    public OPAbsSeekBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTempRect = new Rect();
        this.mThumbTintList = null;
        this.mThumbTintMode = null;
        this.mHasThumbTint = false;
        this.mHasThumbTintMode = false;
        this.mIsUserSeekable = true;
        this.mKeyProgressIncrement = 1;
    }

    public OPAbsSeekBar(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public OPAbsSeekBar(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mTempRect = new Rect();
        this.mThumbTintList = null;
        this.mThumbTintMode = null;
        this.mHasThumbTint = false;
        this.mHasThumbTintMode = false;
        this.mIsUserSeekable = true;
        this.mKeyProgressIncrement = 1;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPSeekBar, i, i2);
        setThumb(obtainStyledAttributes.getDrawable(R$styleable.OPSeekBar_android_thumb));
        if (obtainStyledAttributes.hasValue(R$styleable.OPSeekBar_android_thumbTintMode)) {
            this.mThumbTintMode = DrawableUtils.parseTintMode(obtainStyledAttributes.getInt(R$styleable.OPSeekBar_android_thumbTintMode, -1), this.mThumbTintMode);
            this.mHasThumbTintMode = true;
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPSeekBar_android_thumbTint)) {
            this.mThumbTintList = obtainStyledAttributes.getColorStateList(R$styleable.OPSeekBar_android_thumbTint);
            this.mHasThumbTint = true;
        }
        this.mSplitTrack = obtainStyledAttributes.getBoolean(R$styleable.OPSeekBar_android_splitTrack, false);
        setThumbOffset(obtainStyledAttributes.getDimensionPixelOffset(R$styleable.OPSeekBar_android_thumbOffset, getThumbOffset()));
        obtainStyledAttributes.getBoolean(R$styleable.OPSeekBar_useDisabledAlpha, true);
        this.mDisabledAlpha = 1.0f;
        obtainStyledAttributes.recycle();
        applyThumbTint();
        this.mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setThumb(Drawable drawable) {
        boolean z;
        Drawable drawable2 = this.mThumb;
        if (drawable2 == null || drawable == drawable2) {
            z = false;
        } else {
            drawable2.setCallback(null);
            z = true;
        }
        if (drawable != null) {
            drawable.setCallback(this);
            if (canResolveLayoutDirection()) {
                drawable.setLayoutDirection(getLayoutDirection());
            }
            this.mThumbOffset = drawable.getIntrinsicWidth() / 2;
            if (z && !(drawable.getIntrinsicWidth() == this.mThumb.getIntrinsicWidth() && drawable.getIntrinsicHeight() == this.mThumb.getIntrinsicHeight())) {
                requestLayout();
            }
        }
        this.mThumb = drawable;
        applyThumbTint();
        invalidate();
        if (z) {
            updateThumbAndTrackPos(getWidth(), getHeight());
            if (drawable != null && drawable.isStateful()) {
                drawable.setState(getDrawableState());
            }
        }
    }

    private void applyThumbTint() {
        if (this.mThumb == null) {
            return;
        }
        if (this.mHasThumbTint || this.mHasThumbTintMode) {
            this.mThumb = this.mThumb.mutate();
            if (this.mHasThumbTint) {
                this.mThumb.setTintList(this.mThumbTintList);
            }
            if (this.mHasThumbTintMode) {
                this.mThumb.setTintMode(this.mThumbTintMode);
            }
            if (this.mThumb.isStateful()) {
                this.mThumb.setState(getDrawableState());
            }
        }
    }

    public int getThumbOffset() {
        return this.mThumbOffset;
    }

    public void setThumbOffset(int i) {
        this.mThumbOffset = i;
        invalidate();
    }

    public void setKeyProgressIncrement(int i) {
        if (i < 0) {
            i = -i;
        }
        this.mKeyProgressIncrement = i;
    }

    public synchronized void setMax(int i) {
        super.setMax(i);
        if (this.mKeyProgressIncrement == 0 || getMax() / this.mKeyProgressIncrement > 20) {
            setKeyProgressIncrement(Math.max(1, Math.round(((float) getMax()) / 20.0f)));
        }
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable drawable) {
        return drawable == this.mThumb || super.verifyDrawable(drawable);
    }

    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        Drawable drawable = this.mThumb;
        if (drawable != null) {
            drawable.jumpToCurrentState();
        }
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable != null && this.mDisabledAlpha < 1.0f) {
            progressDrawable.setAlpha(isEnabled() ? 255 : (int) (this.mDisabledAlpha * 255.0f));
        }
        Drawable drawable = this.mThumb;
        if (drawable != null && drawable.isStateful()) {
            drawable.setState(getDrawableState());
        }
    }

    public void drawableHotspotChanged(float f, float f2) {
        super.drawableHotspotChanged(f, f2);
        Drawable drawable = this.mThumb;
        if (drawable != null) {
            drawable.setHotspot(f, f2);
        }
    }

    /* access modifiers changed from: 0000 */
    public void onProgressRefresh(float f, boolean z, int i) {
        super.onProgressRefresh(f, z, i);
        Drawable drawable = this.mThumb;
        if (drawable != null) {
            setThumbPos(getWidth(), drawable, f, Integer.MIN_VALUE);
            invalidate();
        }
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        updateThumbAndTrackPos(i, i2);
    }

    private void updateThumbAndTrackPos(int i, int i2) {
        int i3;
        int i4;
        int i5;
        int i6 = (i2 - this.mPaddingTop) - this.mPaddingBottom;
        Drawable currentDrawable = getCurrentDrawable();
        Drawable drawable = this.mThumb;
        int min = Math.min(this.mMaxHeight, i6);
        if (drawable == null) {
            i3 = 0;
        } else {
            i3 = drawable.getIntrinsicHeight();
        }
        if (i3 > min) {
            int i7 = (i6 - i3) / 2;
            i5 = ((i3 - min) / 2) + i7;
            i4 = i7 + 0;
        } else {
            int i8 = (i6 - min) / 2;
            int i9 = i8 + 0;
            i4 = i8 + ((min - i3) / 2);
            i5 = i9;
        }
        if (currentDrawable != null) {
            currentDrawable.setBounds(0, i5, (i - this.mPaddingRight) - this.mPaddingLeft, min + i5);
        }
        if (drawable != null) {
            setThumbPos(i, drawable, getScale(), i4);
        }
    }

    private float getScale() {
        int max = getMax();
        if (max > 0) {
            return ((float) getProgress()) / ((float) max);
        }
        return 0.0f;
    }

    private void setThumbPos(int i, Drawable drawable, float f, int i2) {
        int i3;
        int i4 = (i - this.mPaddingLeft) - this.mPaddingRight;
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        int i5 = (i4 - intrinsicWidth) + (this.mThumbOffset * 2);
        int i6 = (int) ((f * ((float) i5)) + 0.5f);
        if (i2 == Integer.MIN_VALUE) {
            Rect bounds = drawable.getBounds();
            int i7 = bounds.top;
            i3 = bounds.bottom;
            i2 = i7;
        } else {
            i3 = intrinsicHeight + i2;
        }
        if (isLayoutRtl() && this.mMirrorForRtl) {
            i6 = i5 - i6;
        }
        int i8 = intrinsicWidth + i6;
        Drawable background = getBackground();
        if (background != null) {
            int i9 = this.mPaddingLeft - this.mThumbOffset;
            int i10 = this.mPaddingTop;
            background.setHotspotBounds(i6 + i9, i2 + i10, i9 + i8, i10 + i3);
        }
        drawable.setBounds(i6, i2, i8, i3);
    }

    public void onResolveDrawables(int i) {
        super.onResolveDrawables(i);
        Drawable drawable = this.mThumb;
        if (drawable != null) {
            drawable.setLayoutDirection(i);
        }
    }

    /* access modifiers changed from: protected */
    public synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawThumb(canvas);
    }

    /* access modifiers changed from: 0000 */
    public void drawTrack(Canvas canvas) {
        Drawable drawable = this.mThumb;
        if (drawable == null || !this.mSplitTrack) {
            super.drawTrack(canvas);
            return;
        }
        Rect rect = this.mTempRect;
        int dimension = (int) getResources().getDimension(R$dimen.seekbar_thumb_optical_inset);
        int dimension2 = (int) getResources().getDimension(R$dimen.seekbar_thumb_optical_inset_disabled);
        drawable.copyBounds(rect);
        rect.offset(this.mPaddingLeft - this.mThumbOffset, this.mPaddingTop);
        rect.left += isEnabled() ? dimension : dimension2;
        int i = rect.right;
        if (!isEnabled()) {
            dimension = dimension2;
        }
        rect.right = i - dimension;
        int save = canvas.save();
        canvas.clipRect(rect, Op.DIFFERENCE);
        super.drawTrack(canvas);
        canvas.restoreToCount(save);
    }

    /* access modifiers changed from: 0000 */
    public void drawThumb(Canvas canvas) {
        if (this.mThumb != null) {
            canvas.save();
            canvas.translate((float) (this.mPaddingLeft - this.mThumbOffset), (float) this.mPaddingTop);
            this.mThumb.draw(canvas);
            canvas.restore();
        }
    }

    /* access modifiers changed from: protected */
    public synchronized void onMeasure(int i, int i2) {
        int i3;
        int i4;
        Drawable currentDrawable = getCurrentDrawable();
        int intrinsicHeight = this.mThumb == null ? 0 : this.mThumb.getIntrinsicHeight();
        if (currentDrawable != null) {
            i3 = Math.max(this.mMinWidth, Math.min(this.mMaxWidth, currentDrawable.getIntrinsicWidth()));
            i4 = Math.max(intrinsicHeight, Math.max(this.mMinHeight, Math.min(this.mMaxHeight, currentDrawable.getIntrinsicHeight())));
        } else {
            i4 = 0;
            i3 = 0;
        }
        setMeasuredDimension(View.resolveSizeAndState(i3 + this.mPaddingLeft + this.mPaddingRight, i, 0), View.resolveSizeAndState(i4 + this.mPaddingTop + this.mPaddingBottom, i2, 0));
    }

    public boolean isInScrollingContainer() {
        ViewParent parent = getParent();
        while (parent != null && (parent instanceof ViewGroup)) {
            if (((ViewGroup) parent).shouldDelayChildPressedState()) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mIsUserSeekable || !isEnabled()) {
            return false;
        }
        int action = motionEvent.getAction();
        if (action != 0) {
            if (action == 1) {
                if (this.mIsDragging) {
                    trackTouchEvent(motionEvent);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(motionEvent);
                    onStopTrackingTouch();
                }
                invalidate();
            } else if (action != 2) {
                if (action == 3) {
                    if (this.mIsDragging) {
                        onStopTrackingTouch();
                        setPressed(false);
                    }
                    invalidate();
                }
            } else if (this.mIsDragging) {
                trackTouchEvent(motionEvent);
            } else if (Math.abs(motionEvent.getX() - this.mTouchDownX) > ((float) this.mScaledTouchSlop)) {
                setPressed(true);
                Drawable drawable = this.mThumb;
                if (drawable != null) {
                    invalidate(drawable.getBounds());
                }
                onStartTrackingTouch();
                trackTouchEvent(motionEvent);
                attemptClaimDrag();
            }
        } else if (isInScrollingContainer()) {
            this.mTouchDownX = motionEvent.getX();
        } else {
            setPressed(true);
            Drawable drawable2 = this.mThumb;
            if (drawable2 != null) {
                invalidate(drawable2.getBounds());
            }
            onStartTrackingTouch();
            trackTouchEvent(motionEvent);
            attemptClaimDrag();
        }
        return true;
    }

    private void setHotspot(float f, float f2) {
        Drawable background = getBackground();
        if (background != null) {
            background.setHotspot(f, f2);
        }
    }

    private void trackTouchEvent(MotionEvent motionEvent) {
        int width = getWidth();
        int i = (width - this.mPaddingLeft) - this.mPaddingRight;
        int x = (int) motionEvent.getX();
        float f = 1.0f;
        float f2 = 0.0f;
        if (!isLayoutRtl() || !this.mMirrorForRtl) {
            int i2 = this.mPaddingLeft;
            if (x >= i2) {
                if (x <= width - this.mPaddingRight) {
                    f = ((float) (x - i2)) / ((float) i);
                    f2 = this.mTouchProgressOffset;
                }
                float max = f2 + (f * ((float) getMax()));
                setHotspot((float) x, (float) ((int) motionEvent.getY()));
                setProgressInternal((int) max, true, false);
            }
        } else if (x <= width - this.mPaddingRight) {
            int i3 = this.mPaddingLeft;
            if (x >= i3) {
                f = ((float) ((i - x) + i3)) / ((float) i);
                f2 = this.mTouchProgressOffset;
            }
            float max2 = f2 + (f * ((float) getMax()));
            setHotspot((float) x, (float) ((int) motionEvent.getY()));
            setProgressInternal((int) max2, true, false);
        }
        f = 0.0f;
        float max22 = f2 + (f * ((float) getMax()));
        setHotspot((float) x, (float) ((int) motionEvent.getY()));
        setProgressInternal((int) max22, true, false);
    }

    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    /* access modifiers changed from: 0000 */
    public void onStartTrackingTouch() {
        this.mIsDragging = true;
    }

    /* access modifiers changed from: 0000 */
    public void onStopTrackingTouch() {
        this.mIsDragging = false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:5:0x000e, code lost:
        if (r3 != 22) goto L_0x0029;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onKeyDown(int r3, android.view.KeyEvent r4) {
        /*
            r2 = this;
            boolean r0 = r2.isEnabled()
            if (r0 == 0) goto L_0x0029
            int r0 = r2.mKeyProgressIncrement
            r1 = 21
            if (r3 == r1) goto L_0x0011
            r1 = 22
            if (r3 == r1) goto L_0x0012
            goto L_0x0029
        L_0x0011:
            int r0 = -r0
        L_0x0012:
            boolean r1 = r2.isLayoutRtl()
            if (r1 == 0) goto L_0x0019
            int r0 = -r0
        L_0x0019:
            int r1 = r2.getProgress()
            int r1 = r1 + r0
            r0 = 1
            boolean r1 = r2.setProgressInternal(r1, r0, r0)
            if (r1 == 0) goto L_0x0029
            r2.onKeyChange()
            return r0
        L_0x0029:
            boolean r2 = super.onKeyDown(r3, r4)
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.lib.widget.OPAbsSeekBar.onKeyDown(int, android.view.KeyEvent):boolean");
    }

    public CharSequence getAccessibilityClassName() {
        return OPAbsSeekBar.class.getName();
    }

    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        Drawable drawable = this.mThumb;
        if (drawable != null) {
            setThumbPos(getWidth(), drawable, getScale(), Integer.MIN_VALUE);
            invalidate();
        }
    }
}

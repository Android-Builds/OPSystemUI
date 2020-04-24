package com.oneplus.keyguard;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R$dimen;
import com.android.systemui.R$styleable;
import java.util.ArrayList;
import java.util.Stack;

public class OpPasswordTextViewForPin extends View {
    /* access modifiers changed from: private */
    public Interpolator mAppearInterpolator;
    /* access modifiers changed from: private */
    public int mCharPadding;
    /* access modifiers changed from: private */
    public Stack<CharState> mCharPool;
    /* access modifiers changed from: private */
    public Interpolator mDisappearInterpolator;
    /* access modifiers changed from: private */
    public int mDotSize;
    private final Paint mDrawAlphaPaint1;
    private final Paint mDrawAlphaPaint2;
    private final Paint mDrawAlphaPaint3;
    private final Paint mDrawAlphaPaint4;
    private final Paint mDrawEmptyCirclePaint;
    /* access modifiers changed from: private */
    public final Paint mDrawPaint;
    private Interpolator mFastOutSlowInInterpolator;
    private final int mGravity;
    private onTextChangedListerner mOnTextChangeListerner;
    private PowerManager mPM;
    private int mPinPasswordLength;
    /* access modifiers changed from: private */
    public boolean mShowPassword;
    private String mText;
    /* access modifiers changed from: private */
    public ArrayList<CharState> mTextChars;
    private int mTextColor;
    private final int mTextHeightRaw;
    /* access modifiers changed from: private */
    public UserActivityListener mUserActivityListener;

    private class CharState {
        float currentDotSizeFactor;
        float currentEmptyCircleSizeFactor;
        float currentTextSizeFactor;
        float currentTextTranslationY;
        float currentWidthFactor;
        boolean dotAnimationIsGrowing;
        Animator dotAnimator;
        AnimatorListener dotFinishListener;
        private AnimatorUpdateListener dotSizeUpdater;
        private Runnable dotSwapperRunnable;
        boolean isDotSwapPending;
        AnimatorListener removeEndListener;
        boolean textAnimationIsGrowing;
        ValueAnimator textAnimator;
        AnimatorListener textFinishListener;
        private AnimatorUpdateListener textSizeUpdater;
        ValueAnimator textTranslateAnimator;
        AnimatorListener textTranslateFinishListener;
        private AnimatorUpdateListener textTranslationUpdater;
        char whichChar;
        boolean widthAnimationIsGrowing;
        ValueAnimator widthAnimator;
        AnimatorListener widthFinishListener;
        private AnimatorUpdateListener widthUpdater;

        private CharState() {
            this.currentTextTranslationY = 1.0f;
            this.currentEmptyCircleSizeFactor = 1.0f;
            this.removeEndListener = new AnimatorListenerAdapter() {
                private boolean mCancelled;

                public void onAnimationCancel(Animator animator) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animator) {
                    if (!this.mCancelled) {
                        OpPasswordTextViewForPin.this.mTextChars.remove(CharState.this);
                        OpPasswordTextViewForPin.this.mCharPool.push(CharState.this);
                        CharState.this.reset();
                        CharState charState = CharState.this;
                        charState.cancelAnimator(charState.textTranslateAnimator);
                        CharState.this.textTranslateAnimator = null;
                    }
                }

                public void onAnimationStart(Animator animator) {
                    this.mCancelled = false;
                }
            };
            this.dotFinishListener = new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    CharState.this.dotAnimator = null;
                }
            };
            this.textFinishListener = new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    CharState.this.textAnimator = null;
                }
            };
            this.textTranslateFinishListener = new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    CharState.this.textTranslateAnimator = null;
                }
            };
            this.widthFinishListener = new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    CharState.this.widthAnimator = null;
                }
            };
            this.dotSizeUpdater = new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    CharState.this.currentDotSizeFactor = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    OpPasswordTextViewForPin.this.invalidate();
                }
            };
            this.textSizeUpdater = new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    CharState.this.currentTextSizeFactor = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    OpPasswordTextViewForPin.this.invalidate();
                }
            };
            this.textTranslationUpdater = new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    CharState.this.currentTextTranslationY = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    OpPasswordTextViewForPin.this.invalidate();
                }
            };
            this.widthUpdater = new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    CharState.this.currentWidthFactor = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    OpPasswordTextViewForPin.this.invalidate();
                }
            };
            this.dotSwapperRunnable = new Runnable() {
                public void run() {
                    CharState.this.performSwap();
                    CharState.this.isDotSwapPending = false;
                }
            };
        }

        /* access modifiers changed from: 0000 */
        public void reset() {
            this.whichChar = 0;
            this.currentTextSizeFactor = 0.0f;
            this.currentDotSizeFactor = 0.0f;
            this.currentWidthFactor = 0.0f;
            cancelAnimator(this.textAnimator);
            this.textAnimator = null;
            cancelAnimator(this.dotAnimator);
            this.dotAnimator = null;
            cancelAnimator(this.widthAnimator);
            this.widthAnimator = null;
            this.currentTextTranslationY = 1.0f;
            removeDotSwapCallbacks();
        }

        /* access modifiers changed from: 0000 */
        public void startRemoveAnimation(long j, long j2) {
            boolean z = true;
            boolean z2 = (this.currentDotSizeFactor > 0.0f && this.dotAnimator == null) || (this.dotAnimator != null && this.dotAnimationIsGrowing);
            boolean z3 = (this.currentTextSizeFactor > 0.0f && this.textAnimator == null) || (this.textAnimator != null && this.textAnimationIsGrowing);
            if ((this.currentWidthFactor <= 0.0f || this.widthAnimator != null) && (this.widthAnimator == null || !this.widthAnimationIsGrowing)) {
                z = false;
            }
            if (z2) {
                startDotDisappearAnimation(j);
            }
            if (z3) {
                startTextDisappearAnimation(j);
            }
            if (z) {
                startWidthDisappearAnimation(j2);
            }
        }

        /* access modifiers changed from: 0000 */
        public void startAppearAnimation() {
            boolean z = true;
            boolean z2 = !OpPasswordTextViewForPin.this.mShowPassword && (this.dotAnimator == null || !this.dotAnimationIsGrowing);
            boolean z3 = OpPasswordTextViewForPin.this.mShowPassword && (this.textAnimator == null || !this.textAnimationIsGrowing);
            if (this.widthAnimator != null && this.widthAnimationIsGrowing) {
                z = false;
            }
            if (z2) {
                startDotAppearAnimation(0);
            }
            if (z3) {
                startTextAppearAnimation();
            }
            if (z) {
                startWidthAppearAnimation();
            }
            if (OpPasswordTextViewForPin.this.mShowPassword) {
                postDotSwap(250);
            }
        }

        private void postDotSwap(long j) {
            removeDotSwapCallbacks();
            OpPasswordTextViewForPin.this.postDelayed(this.dotSwapperRunnable, j);
            this.isDotSwapPending = true;
        }

        /* access modifiers changed from: private */
        public void removeDotSwapCallbacks() {
            OpPasswordTextViewForPin.this.removeCallbacks(this.dotSwapperRunnable);
            this.isDotSwapPending = false;
        }

        /* access modifiers changed from: 0000 */
        public void swapToDotWhenAppearFinished() {
            removeDotSwapCallbacks();
            ValueAnimator valueAnimator = this.textAnimator;
            if (valueAnimator != null) {
                postDotSwap((valueAnimator.getDuration() - this.textAnimator.getCurrentPlayTime()) + 100);
            } else {
                performSwap();
            }
        }

        /* access modifiers changed from: private */
        public void performSwap() {
            startTextDisappearAnimation(0);
            startDotAppearAnimation(30);
        }

        private void startWidthDisappearAnimation(long j) {
            cancelAnimator(this.widthAnimator);
            this.widthAnimator = ValueAnimator.ofFloat(new float[]{this.currentWidthFactor, 0.0f});
            this.widthAnimator.addUpdateListener(this.widthUpdater);
            this.widthAnimator.addListener(this.widthFinishListener);
            this.widthAnimator.addListener(this.removeEndListener);
            this.widthAnimator.setDuration((long) (this.currentWidthFactor * 160.0f));
            this.widthAnimator.setStartDelay(j);
            this.widthAnimator.start();
            this.widthAnimationIsGrowing = false;
        }

        private void startTextDisappearAnimation(long j) {
            cancelAnimator(this.textAnimator);
            this.textAnimator = ValueAnimator.ofFloat(new float[]{this.currentTextSizeFactor, 0.0f});
            this.textAnimator.addUpdateListener(this.textSizeUpdater);
            this.textAnimator.addListener(this.textFinishListener);
            this.textAnimator.setInterpolator(OpPasswordTextViewForPin.this.mDisappearInterpolator);
            this.textAnimator.setDuration((long) (this.currentTextSizeFactor * 160.0f));
            this.textAnimator.setStartDelay(j);
            this.textAnimator.start();
            this.textAnimationIsGrowing = false;
        }

        private void startDotDisappearAnimation(long j) {
            cancelAnimator(this.dotAnimator);
            ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{this.currentDotSizeFactor, 0.0f});
            ofFloat.addUpdateListener(this.dotSizeUpdater);
            ofFloat.addListener(this.dotFinishListener);
            ofFloat.setInterpolator(OpPasswordTextViewForPin.this.mDisappearInterpolator);
            ofFloat.setDuration((long) (Math.min(this.currentDotSizeFactor, 1.0f) * 160.0f));
            ofFloat.setStartDelay(j);
            ofFloat.start();
            this.dotAnimator = ofFloat;
            this.dotAnimationIsGrowing = false;
        }

        private void startWidthAppearAnimation() {
            cancelAnimator(this.widthAnimator);
            this.widthAnimator = ValueAnimator.ofFloat(new float[]{this.currentWidthFactor, 1.0f});
            this.widthAnimator.addUpdateListener(this.widthUpdater);
            this.widthAnimator.addListener(this.widthFinishListener);
            this.widthAnimator.setDuration((long) ((1.0f - this.currentWidthFactor) * 160.0f));
            this.widthAnimator.start();
            this.widthAnimationIsGrowing = true;
        }

        private void startTextAppearAnimation() {
            cancelAnimator(this.textAnimator);
            this.textAnimator = ValueAnimator.ofFloat(new float[]{this.currentTextSizeFactor, 1.0f});
            this.textAnimator.addUpdateListener(this.textSizeUpdater);
            this.textAnimator.addListener(this.textFinishListener);
            this.textAnimator.setInterpolator(OpPasswordTextViewForPin.this.mAppearInterpolator);
            this.textAnimator.setDuration((long) ((1.0f - this.currentTextSizeFactor) * 160.0f));
            this.textAnimator.start();
            this.textAnimationIsGrowing = true;
            if (this.textTranslateAnimator == null) {
                this.textTranslateAnimator = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f});
                this.textTranslateAnimator.addUpdateListener(this.textTranslationUpdater);
                this.textTranslateAnimator.addListener(this.textTranslateFinishListener);
                this.textTranslateAnimator.setInterpolator(OpPasswordTextViewForPin.this.mAppearInterpolator);
                this.textTranslateAnimator.setDuration(160);
                this.textTranslateAnimator.start();
            }
        }

        private void startDotAppearAnimation(long j) {
            cancelAnimator(this.dotAnimator);
            if (!OpPasswordTextViewForPin.this.mShowPassword) {
                ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{this.currentDotSizeFactor, 1.5f});
                ofFloat.addUpdateListener(this.dotSizeUpdater);
                ofFloat.setInterpolator(OpPasswordTextViewForPin.this.mAppearInterpolator);
                ofFloat.setDuration(160);
                ValueAnimator ofFloat2 = ValueAnimator.ofFloat(new float[]{1.5f, 1.0f});
                ofFloat2.addUpdateListener(this.dotSizeUpdater);
                ofFloat2.setDuration(160);
                ofFloat2.addListener(this.dotFinishListener);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playSequentially(new Animator[]{ofFloat, ofFloat2});
                animatorSet.setStartDelay(j);
                animatorSet.start();
                this.dotAnimator = animatorSet;
            } else {
                ValueAnimator ofFloat3 = ValueAnimator.ofFloat(new float[]{this.currentDotSizeFactor, 1.0f});
                ofFloat3.addUpdateListener(this.dotSizeUpdater);
                ofFloat3.setDuration((long) ((1.0f - this.currentDotSizeFactor) * 160.0f));
                ofFloat3.addListener(this.dotFinishListener);
                ofFloat3.setStartDelay(j);
                ofFloat3.start();
                this.dotAnimator = ofFloat3;
            }
            this.dotAnimationIsGrowing = true;
        }

        /* access modifiers changed from: private */
        public void cancelAnimator(Animator animator) {
            if (animator != null) {
                animator.cancel();
            }
        }

        public float draw(Canvas canvas, float f, int i, float f2, float f3) {
            boolean z = true;
            boolean z2 = this.currentTextSizeFactor > 0.0f;
            if (this.currentDotSizeFactor <= 0.0f) {
                z = false;
            }
            float f4 = f3 * this.currentWidthFactor;
            if (z2) {
                float f5 = (float) i;
                float f6 = ((f5 / 2.0f) * this.currentTextSizeFactor) + f2 + (f5 * this.currentTextTranslationY * 0.8f);
                canvas.save();
                canvas.translate((f4 / 2.0f) + f, f6);
                float f7 = this.currentTextSizeFactor;
                canvas.scale(f7, f7);
                canvas.drawText(Character.toString(this.whichChar), 0.0f, 0.0f, OpPasswordTextViewForPin.this.mDrawPaint);
                canvas.restore();
            }
            if (z) {
                canvas.save();
                canvas.translate(f + (f4 / 2.0f), f2);
                canvas.drawCircle(0.0f, 0.0f, ((float) (OpPasswordTextViewForPin.this.mDotSize / 2)) * this.currentDotSizeFactor, OpPasswordTextViewForPin.this.mDrawPaint);
                canvas.restore();
            }
            return f4 + (((float) OpPasswordTextViewForPin.this.mCharPadding) * this.currentWidthFactor);
        }
    }

    public interface UserActivityListener {
        void onCheckPasswordAndUnlock();

        void onUserActivity();
    }

    public interface onTextChangedListerner {
        void onTextChanged(String str);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public OpPasswordTextViewForPin(Context context) {
        this(context, null);
    }

    public OpPasswordTextViewForPin(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public OpPasswordTextViewForPin(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    /* JADX INFO: finally extract failed */
    public OpPasswordTextViewForPin(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mTextChars = new ArrayList<>();
        this.mText = "";
        this.mCharPool = new Stack<>();
        this.mDrawPaint = new Paint();
        this.mDrawEmptyCirclePaint = new Paint();
        this.mDrawAlphaPaint1 = new Paint();
        this.mDrawAlphaPaint2 = new Paint();
        this.mDrawAlphaPaint3 = new Paint();
        this.mDrawAlphaPaint4 = new Paint();
        this.mTextColor = -1;
        boolean z = false;
        this.mPinPasswordLength = 0;
        setFocusableInTouchMode(true);
        setFocusable(true);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.PasswordTextView);
        try {
            this.mTextHeightRaw = obtainStyledAttributes.getInt(R$styleable.PasswordTextView_scaledTextSize, 0);
            this.mGravity = obtainStyledAttributes.getInt(R$styleable.PasswordTextView_android_gravity, 17);
            this.mDotSize = obtainStyledAttributes.getDimensionPixelSize(R$styleable.PasswordTextView_dotSize, getContext().getResources().getDimensionPixelSize(R$dimen.password_dot_size));
            this.mCharPadding = obtainStyledAttributes.getDimensionPixelSize(R$styleable.PasswordTextView_charPadding, getContext().getResources().getDimensionPixelSize(R$dimen.password_char_padding));
            int color = obtainStyledAttributes.getColor(R$styleable.PasswordTextView_android_textColor, -1);
            this.mTextColor = color;
            this.mDrawPaint.setColor(color);
            obtainStyledAttributes.recycle();
            this.mDrawPaint.setFlags(129);
            this.mDrawPaint.setTextAlign(Align.CENTER);
            if (System.getInt(this.mContext.getContentResolver(), "show_password", 1) == 1) {
                z = true;
            }
            this.mShowPassword = z;
            this.mAppearInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563662);
            this.mDisappearInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563663);
            this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563661);
            this.mPM = (PowerManager) this.mContext.getSystemService("power");
            this.mPinPasswordLength = KeyguardUpdateMonitor.getInstance(context).keyguardPinPasswordLength();
        } catch (Throwable th) {
            obtainStyledAttributes.recycle();
            throw th;
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        float width = ((float) (getWidth() / 2)) - (getDrawingWidth() / 2.0f);
        int size = this.mTextChars.size();
        Rect charBounds = getCharBounds();
        int i = charBounds.bottom - charBounds.top;
        float height = ((float) getHeight()) / 2.0f;
        float f = (float) (charBounds.right - charBounds.left);
        for (int i2 = 0; i2 < size; i2++) {
            width += ((CharState) this.mTextChars.get(i2)).draw(canvas, width, i, height, f);
        }
    }

    private Rect getCharBounds() {
        this.mDrawPaint.setTextSize(((float) this.mTextHeightRaw) * getResources().getDisplayMetrics().scaledDensity);
        Rect rect = new Rect();
        this.mDrawPaint.getTextBounds("0", 0, 1, rect);
        return rect;
    }

    private float getDrawingWidth() {
        int size = this.mTextChars.size();
        Rect charBounds = getCharBounds();
        int i = charBounds.right - charBounds.left;
        int i2 = 0;
        for (int i3 = 0; i3 < size; i3++) {
            CharState charState = (CharState) this.mTextChars.get(i3);
            if (i3 != 0) {
                i2 = (int) (((float) i2) + (((float) this.mCharPadding) * charState.currentWidthFactor));
            }
            i2 = (int) (((float) i2) + (((float) i) * charState.currentWidthFactor));
        }
        return (float) i2;
    }

    public void append(char c) {
        CharState charState;
        int size = this.mTextChars.size();
        String str = this.mText;
        StringBuilder sb = new StringBuilder();
        sb.append(this.mText);
        sb.append(c);
        this.mText = sb.toString();
        int length = this.mText.length();
        if (length > size) {
            charState = obtainCharState(c);
            this.mTextChars.add(charState);
        } else {
            CharState charState2 = (CharState) this.mTextChars.get(length - 1);
            charState2.whichChar = c;
            charState = charState2;
        }
        charState.startAppearAnimation();
        if (length > 1) {
            CharState charState3 = (CharState) this.mTextChars.get(length - 2);
            if (charState3.isDotSwapPending) {
                charState3.swapToDotWhenAppearFinished();
            }
        }
        int i = this.mPinPasswordLength;
        boolean z = i != 0 && length == i;
        if (length == 16 || z) {
            new Handler().post(new Runnable() {
                public void run() {
                    OpPasswordTextViewForPin.this.mUserActivityListener.onCheckPasswordAndUnlock();
                }
            });
        }
        onTextChangedListerner ontextchangedlisterner = this.mOnTextChangeListerner;
        if (ontextchangedlisterner != null) {
            ontextchangedlisterner.onTextChanged(this.mText);
        }
        userActivity();
        sendAccessibilityEventTypeViewTextChanged(str, str.length(), 0, 1);
    }

    public void setUserActivityListener(UserActivityListener userActivityListener) {
        this.mUserActivityListener = userActivityListener;
    }

    private void userActivity() {
        this.mPM.userActivity(SystemClock.uptimeMillis(), false);
        UserActivityListener userActivityListener = this.mUserActivityListener;
        if (userActivityListener != null) {
            userActivityListener.onUserActivity();
        }
    }

    public void deleteLastChar() {
        int length = this.mText.length();
        String str = this.mText;
        if (length > 0) {
            int i = length - 1;
            this.mText = str.substring(0, i);
            ((CharState) this.mTextChars.get(i)).startRemoveAnimation(0, 0);
        }
        onTextChangedListerner ontextchangedlisterner = this.mOnTextChangeListerner;
        if (ontextchangedlisterner != null) {
            ontextchangedlisterner.onTextChanged(this.mText);
        }
        userActivity();
        sendAccessibilityEventTypeViewTextChanged(str, str.length() - 1, 1, 0);
    }

    public String getText() {
        return this.mText;
    }

    private CharState obtainCharState(char c) {
        CharState charState;
        if (this.mCharPool.isEmpty()) {
            charState = new CharState();
        } else {
            charState = (CharState) this.mCharPool.pop();
            charState.reset();
        }
        charState.whichChar = c;
        return charState;
    }

    public void reset(boolean z, boolean z2) {
        String str = this.mText;
        this.mText = "";
        int size = this.mTextChars.size();
        int i = size - 1;
        int i2 = i / 2;
        int i3 = 0;
        while (i3 < size) {
            CharState charState = (CharState) this.mTextChars.get(i3);
            if (z) {
                charState.startRemoveAnimation(Math.min(((long) (i3 <= i2 ? i3 * 2 : i - (((i3 - i2) - 1) * 2))) * 40, 200), Math.min(40 * ((long) i), 200) + 160);
                charState.removeDotSwapCallbacks();
            } else {
                this.mCharPool.push(charState);
            }
            i3++;
        }
        if (!z) {
            this.mTextChars.clear();
        }
        onTextChangedListerner ontextchangedlisterner = this.mOnTextChangeListerner;
        if (ontextchangedlisterner != null) {
            ontextchangedlisterner.onTextChanged(this.mText);
        }
        if (z2) {
            sendAccessibilityEventTypeViewTextChanged(str, 0, str.length(), 0);
        }
    }

    /* access modifiers changed from: 0000 */
    public void sendAccessibilityEventTypeViewTextChanged(String str, int i, int i2, int i3) {
        if (!AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            return;
        }
        if (isFocused() || (isSelected() && isShown())) {
            if (!shouldSpeakPasswordsForAccessibility()) {
                str = null;
            }
            AccessibilityEvent obtain = AccessibilityEvent.obtain(16);
            obtain.setFromIndex(i);
            obtain.setRemovedCount(i2);
            obtain.setAddedCount(i3);
            obtain.setBeforeText(str);
            obtain.setPassword(true);
            sendAccessibilityEventUnchecked(obtain);
        }
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setClassName(OpPasswordTextViewForPin.class.getName());
        accessibilityEvent.setPassword(true);
    }

    public void onPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onPopulateAccessibilityEvent(accessibilityEvent);
        if (shouldSpeakPasswordsForAccessibility()) {
            String str = this.mText;
            if (!TextUtils.isEmpty(str)) {
                accessibilityEvent.getText().add(str);
            }
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName(OpPasswordTextViewForPin.class.getName());
        accessibilityNodeInfo.setPassword(true);
        if (shouldSpeakPasswordsForAccessibility()) {
            accessibilityNodeInfo.setText(this.mText);
        }
        accessibilityNodeInfo.setEditable(true);
        accessibilityNodeInfo.setInputType(16);
    }

    private boolean shouldSpeakPasswordsForAccessibility() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), "speak_password", 0, -3) == 1;
    }

    public void setTextChangeListener(onTextChangedListerner ontextchangedlisterner) {
        this.mOnTextChangeListerner = ontextchangedlisterner;
    }
}

package com.oneplus.lib.design.widget;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$drawable;
import com.oneplus.commonctrl.R$integer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class OPPageIndicator extends ViewGroup {
    /* access modifiers changed from: private */
    public boolean mAnimating;
    private final Runnable mAnimationDone = new Runnable() {
        public void run() {
            OPPageIndicator.this.mAnimating = false;
            if (OPPageIndicator.this.mQueuedPositions.size() != 0) {
                OPPageIndicator oPPageIndicator = OPPageIndicator.this;
                oPPageIndicator.setPosition(((Integer) oPPageIndicator.mQueuedPositions.remove(0)).intValue());
            }
        }
    };
    private float mMinorAlpha;
    private final int mPageDotWidth = ((int) (((float) this.mPageIndicatorWidth) * 0.4f));
    private final int mPageIndicatorHeight = ((int) getContext().getResources().getDimension(R$dimen.op_qs_page_indicator_height));
    private final int mPageIndicatorWidth = ((int) getContext().getResources().getDimension(R$dimen.op_qs_page_indicator_width));
    private int mPosition = -1;
    /* access modifiers changed from: private */
    public final ArrayList<Integer> mQueuedPositions = new ArrayList<>();

    public OPPageIndicator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        int integer = getContext().getResources().getInteger(R$integer.op_pageIndicator_alpha_material);
        StringBuilder sb = new StringBuilder();
        sb.append("alpha = ");
        sb.append(integer);
        Log.i("OPPageIndicator", sb.toString());
        this.mMinorAlpha = ((float) integer) / 100.0f;
    }

    /* access modifiers changed from: private */
    public void setPosition(int i) {
        if (!isVisibleToUser2() || Math.abs(this.mPosition - i) != 1) {
            setIndex(i >> 1);
        } else {
            animate(this.mPosition, i);
        }
        this.mPosition = i;
    }

    private void setIndex(int i) {
        int childCount = getChildCount();
        int i2 = 0;
        while (i2 < childCount) {
            ImageView imageView = (ImageView) getChildAt(i2);
            imageView.setTranslationX(0.0f);
            imageView.setImageResource(R$drawable.op_major_a_b);
            imageView.setAlpha(getAlpha(i2 == i));
            i2++;
        }
    }

    private void animate(int i, int i2) {
        int i3 = i >> 1;
        int i4 = i2 >> 1;
        setIndex(i3);
        boolean z = (i & 1) != 0;
        boolean z2 = !z ? i < i2 : i > i2;
        int min = Math.min(i3, i4);
        int max = Math.max(i3, i4);
        if (max == min) {
            max++;
        }
        ImageView imageView = (ImageView) getChildAt(min);
        ImageView imageView2 = (ImageView) getChildAt(max);
        if (imageView != null && imageView2 != null) {
            imageView2.setTranslationX(imageView.getX() - imageView2.getX());
            playAnimation(imageView, getTransition(z, z2, false));
            imageView.setAlpha(getAlpha(false));
            playAnimation(imageView2, getTransition(z, z2, true));
            imageView2.setAlpha(getAlpha(true));
            this.mAnimating = true;
        }
    }

    private float getAlpha(boolean z) {
        if (z) {
            return 1.0f;
        }
        return this.mMinorAlpha;
    }

    private void playAnimation(ImageView imageView, int i) {
        AnimatedVectorDrawable animatedVectorDrawable = (AnimatedVectorDrawable) getContext().getDrawable(i);
        imageView.setImageDrawable(animatedVectorDrawable);
        forceAnimationOnUI2(animatedVectorDrawable);
        animatedVectorDrawable.start();
        postDelayed(this.mAnimationDone, 250);
    }

    private int getTransition(boolean z, boolean z2, boolean z3) {
        if (z3) {
            if (z) {
                if (z2) {
                    return R$drawable.op_major_b_a_animation;
                }
                return R$drawable.op_major_b_c_animation;
            } else if (z2) {
                return R$drawable.op_major_a_b_animation;
            } else {
                return R$drawable.op_major_c_b_animation;
            }
        } else if (z) {
            if (z2) {
                return R$drawable.op_minor_b_c_animation;
            }
            return R$drawable.op_minor_b_a_animation;
        } else if (z2) {
            return R$drawable.op_minor_c_b_animation;
        } else {
            return R$drawable.op_minor_a_b_animation;
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int childCount = getChildCount();
        if (childCount == 0) {
            super.onMeasure(i, i2);
            return;
        }
        int makeMeasureSpec = MeasureSpec.makeMeasureSpec(this.mPageIndicatorWidth, 1073741824);
        int makeMeasureSpec2 = MeasureSpec.makeMeasureSpec(this.mPageIndicatorHeight, 1073741824);
        for (int i3 = 0; i3 < childCount; i3++) {
            getChildAt(i3).measure(makeMeasureSpec, makeMeasureSpec2);
        }
        int i4 = this.mPageIndicatorWidth;
        int i5 = this.mPageDotWidth;
        setMeasuredDimension(((i4 - i5) * (childCount - 1)) + i5, this.mPageIndicatorHeight);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int childCount = getChildCount();
        if (childCount != 0) {
            for (int i5 = 0; i5 < childCount; i5++) {
                int i6 = (this.mPageIndicatorWidth - this.mPageDotWidth) * i5;
                getChildAt(i5).layout(i6, 0, this.mPageIndicatorWidth + i6, this.mPageIndicatorHeight);
            }
        }
    }

    private void forceAnimationOnUI2(AnimatedVectorDrawable animatedVectorDrawable) {
        String str = "Could not invoke forceAnimationOnUI.";
        String str2 = "OPPageIndicator";
        try {
            Method method = animatedVectorDrawable.getClass().getMethod("forceAnimationOnUI", new Class[0]);
            method.setAccessible(true);
            method.invoke(animatedVectorDrawable, new Object[0]);
        } catch (NoSuchMethodException unused) {
            Log.d(str2, "Could not find method forceAnimationOnUI.");
        } catch (InvocationTargetException e) {
            Log.d(str2, str, e);
        } catch (IllegalAccessException e2) {
            Log.d(str2, str, e2);
        } catch (Exception e3) {
            e3.printStackTrace();
        }
    }

    /* access modifiers changed from: 0000 */
    public boolean isVisibleToUser2() {
        String str = "Could not invoke isVisibleToUser.";
        String str2 = "OPPageIndicator";
        try {
            Method method = getClass().getMethod("isVisibleToUser", new Class[0]);
            method.setAccessible(true);
            return ((Boolean) method.invoke(this, new Object[0])).booleanValue();
        } catch (NoSuchMethodException unused) {
            Log.d(str2, "Could not find method isVisibleToUser.");
            return true;
        } catch (InvocationTargetException e) {
            Log.d(str2, str, e);
            return true;
        } catch (IllegalAccessException e2) {
            Log.d(str2, str, e2);
            return true;
        } catch (Exception e3) {
            e3.printStackTrace();
            return true;
        }
    }
}

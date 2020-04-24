package com.oneplus.lib.widget.listview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ListView;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$styleable;
import java.util.ArrayList;

public class OPListView extends ListView {
    /* access modifiers changed from: private */
    public boolean mAnimRunning;
    private ArrayList<ObjectAnimator> mAnimatorList;
    AnimatorUpdateListener mAnimatorUpdateListener;
    private DecelerateInterpolator mDecelerateInterpolator;
    AnimatorSet mDelAniSet;
    private boolean mDelAnimationFlag;
    /* access modifiers changed from: private */
    public ArrayList<Integer> mDelOriViewTopList;
    /* access modifiers changed from: private */
    public ArrayList<Integer> mDelPosList;
    /* access modifiers changed from: private */
    public ArrayList<View> mDelViewList;
    /* access modifiers changed from: private */
    public DeleteAnimationListener mDeleteAnimationListener;
    /* access modifiers changed from: private */
    public boolean mDisableTouchEvent;
    private Drawable mDivider;
    private IOPDividerController mDividerController;
    private int mDividerHeight;
    private boolean mFooterDividersEnabled;
    private boolean mHeaderDividersEnabled;
    /* access modifiers changed from: private */
    public boolean mInDeleteAnimation;
    private boolean mIsClipToPadding;
    private boolean mIsDisableAnimation;
    /* access modifiers changed from: private */
    public ArrayList<View> mNowViewList;
    private int mOriBelowLeftCount;
    private int mOriCurDeleteCount;
    private int mOriCurLeftCount;
    private boolean mOriLastPage;
    private int mOriUpperDeleteCount;
    Rect mTempRect;

    public interface DeleteAnimationListener {
        void onAnimationEnd();
    }

    public OPListView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16842868);
    }

    public OPListView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public OPListView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mDividerHeight = 1;
        this.mIsDisableAnimation = true;
        this.mDelViewList = null;
        this.mDelPosList = null;
        this.mNowViewList = null;
        this.mDelOriViewTopList = null;
        this.mDelAniSet = null;
        this.mDecelerateInterpolator = new DecelerateInterpolator(1.2f);
        this.mAnimatorList = new ArrayList<>();
        this.mTempRect = new Rect();
        this.mHeaderDividersEnabled = true;
        this.mFooterDividersEnabled = true;
        this.mIsClipToPadding = true;
        this.mDividerController = null;
        this.mAnimatorUpdateListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                OPListView.this.invalidate();
            }
        };
        init(context, attributeSet, i, i2);
    }

    private void init(Context context, AttributeSet attributeSet, int i, int i2) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPListView, R$attr.OPListViewStyle, 0);
        Drawable drawable = obtainStyledAttributes.getDrawable(R$styleable.OPListView_android_divider);
        Drawable drawable2 = obtainStyledAttributes.getDrawable(R$styleable.OPListView_android_background);
        if (drawable != null) {
            setDivider(drawable);
        }
        if (drawable2 != null) {
            setBackground(drawable2);
        }
        this.mDividerHeight = getResources().getDimensionPixelSize(R$dimen.listview_divider_height);
        setOverScrollMode(0);
        super.setDivider(new ColorDrawable(17170445));
        setDividerHeight(this.mDividerHeight);
        setFooterDividersEnabled(false);
        obtainStyledAttributes.recycle();
    }

    public void setHeaderDividersEnabled(boolean z) {
        this.mHeaderDividersEnabled = z;
    }

    public void setFooterDividersEnabled(boolean z) {
        this.mFooterDividersEnabled = z;
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        int i;
        int i2;
        int i3;
        Canvas canvas2 = canvas;
        super.dispatchDraw(canvas);
        Drawable overscrollHeader = getOverscrollHeader();
        Drawable overscrollFooter = getOverscrollFooter();
        boolean z = false;
        int i4 = overscrollHeader != null ? 1 : 0;
        boolean z2 = overscrollFooter != null;
        boolean z3 = getDivider() != null;
        if (z3 || i4 != 0 || z2) {
            Rect rect = this.mTempRect;
            rect.left = getPaddingLeft();
            rect.right = (getRight() - getLeft()) - getPaddingRight();
            int childCount = getChildCount();
            int headerViewsCount = getHeaderViewsCount();
            int count = getCount() - getFooterViewsCount();
            boolean z4 = this.mHeaderDividersEnabled;
            boolean z5 = this.mFooterDividersEnabled;
            int firstVisiblePosition = getFirstVisiblePosition();
            getAdapter();
            if (isClipToPadding()) {
                i2 = getListPaddingTop();
                i = getListPaddingBottom();
            } else {
                i2 = 0;
                i = 0;
            }
            int bottom = ((getBottom() - getTop()) - i) + getScrollY();
            if (!isStackFromBottom()) {
                int scrollY = getScrollY();
                if (childCount > 0 && scrollY < 0 && z3) {
                    rect.bottom = 0;
                    rect.top = -getDividerHeight();
                    drawDivider(canvas2, rect, -1);
                }
                int i5 = 0;
                while (i5 < childCount) {
                    int i6 = firstVisiblePosition + i5;
                    boolean z6 = i6 < headerViewsCount ? true : z;
                    boolean z7 = i6 >= count ? true : z;
                    if ((z4 || !z6) && (z5 || !z7)) {
                        View childAt = getChildAt(i5);
                        int bottom2 = childAt.getBottom();
                        i3 = firstVisiblePosition;
                        boolean z8 = i5 == childCount + -1;
                        if (z3 && shouldDrawDivider(i5) && childAt.getHeight() > 0 && bottom2 < bottom && (!z2 || !z8)) {
                            int i7 = i6 + 1;
                            if ((z4 || (!z6 && i7 >= headerViewsCount)) && (z8 || z5 || (!z7 && i7 < count))) {
                                int translationY = (int) childAt.getTranslationY();
                                rect.top = bottom2 + translationY;
                                rect.bottom = bottom2 + getDividerHeight() + translationY;
                                drawDivider(canvas2, rect, i5);
                            }
                        }
                    } else {
                        i3 = firstVisiblePosition;
                    }
                    i5++;
                    firstVisiblePosition = i3;
                    z = false;
                }
            } else {
                int i8 = firstVisiblePosition;
                int scrollY2 = getScrollY();
                int i9 = i4;
                while (i9 < childCount) {
                    int i10 = i8 + i9;
                    boolean z9 = i10 < headerViewsCount;
                    boolean z10 = i10 >= count;
                    if ((z4 || !z9) && (z5 || !z10)) {
                        int top = getChildAt(i9).getTop();
                        if (z3 && shouldDrawDivider(i9) && top > i2) {
                            boolean z11 = i9 == i4;
                            int i11 = i10 - 1;
                            if ((z4 || (!z9 && i11 >= headerViewsCount)) && (z11 || z5 || (!z10 && i11 < count))) {
                                rect.top = top - getDividerHeight();
                                rect.bottom = top;
                                drawDivider(canvas2, rect, i9 - 1);
                            }
                        }
                    }
                    i9++;
                }
                if (childCount > 0 && scrollY2 > 0 && z3) {
                    rect.top = bottom;
                    rect.bottom = bottom + getDividerHeight();
                    drawDivider(canvas2, rect, -1);
                }
            }
        }
        if (this.mDelAnimationFlag) {
            this.mDelAnimationFlag = false;
            startDelDropAnimation();
        }
    }

    public Drawable getDivider() {
        return this.mDivider;
    }

    public void setDivider(Drawable drawable) {
        this.mDivider = drawable;
        requestLayout();
        invalidate();
    }

    public int getDividerHeight() {
        return this.mDividerHeight;
    }

    private boolean isClipToPadding() {
        return this.mIsClipToPadding;
    }

    public void setClipToPadding(boolean z) {
        super.setClipToPadding(z);
        this.mIsClipToPadding = z;
    }

    /* access modifiers changed from: 0000 */
    public void drawDivider(Canvas canvas, Rect rect, int i) {
        Drawable divider = getDivider();
        int dividerType = getDividerType(i + getFirstVisiblePosition());
        if (this.mDividerController != null) {
            if (dividerType == 1) {
                rect.left = 0;
                rect.right = getWidth();
            } else if (dividerType == 2) {
                rect.left = 100;
                rect.right = getWidth() - 32;
            }
        }
        divider.setBounds(rect);
        divider.draw(canvas);
    }

    private int getDividerType(int i) {
        IOPDividerController iOPDividerController = this.mDividerController;
        if (iOPDividerController == null) {
            return -1;
        }
        return iOPDividerController.getDividerType(i);
    }

    private boolean shouldDrawDivider(int i) {
        int dividerType = getDividerType(i + getFirstVisiblePosition());
        IOPDividerController iOPDividerController = this.mDividerController;
        return iOPDividerController == null || (iOPDividerController != null && dividerType > 0);
    }

    private ObjectAnimator getAnimator(int i, View view, float f) {
        if (i >= this.mAnimatorList.size()) {
            ObjectAnimator ofPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(view, new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat("y", new float[]{f, (float) view.getTop()})});
            this.mAnimatorList.add(ofPropertyValuesHolder);
            return ofPropertyValuesHolder;
        }
        ObjectAnimator objectAnimator = (ObjectAnimator) this.mAnimatorList.get(i);
        objectAnimator.getValues()[0].setFloatValues(new float[]{f, (float) view.getTop()});
        objectAnimator.setTarget(view);
        return objectAnimator;
    }

    private void startDelDropAnimation() {
        this.mDelAniSet = new AnimatorSet();
        setDelViewLocation();
        for (int i = 0; i < this.mNowViewList.size(); i++) {
            ObjectAnimator animator = getAnimator(i, (View) this.mNowViewList.get(i), (float) ((Integer) this.mDelOriViewTopList.get(i)).intValue());
            animator.setDuration((long) 200);
            animator.setInterpolator(this.mDecelerateInterpolator);
            animator.addUpdateListener(this.mAnimatorUpdateListener);
            this.mDelAniSet.playTogether(new Animator[]{animator});
        }
        this.mDelAniSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                OPListView.this.mAnimRunning = false;
                OPListView.this.mInDeleteAnimation = false;
                OPListView.this.mDisableTouchEvent = false;
                OPListView.this.mDelPosList.clear();
                OPListView.this.mDelOriViewTopList.clear();
                OPListView.this.mDelViewList.clear();
                OPListView.this.mNowViewList.clear();
                OPListView.this.invalidate();
                if (OPListView.this.mDeleteAnimationListener != null) {
                    OPListView.this.mDeleteAnimationListener.onAnimationEnd();
                }
            }
        });
        this.mDelAniSet.start();
    }

    private void setDelViewLocation() {
        int firstVisiblePosition = getFirstVisiblePosition();
        int childCount = getChildCount();
        int i = 0;
        boolean z = getLastVisiblePosition() == getAdapter().getCount() - 1;
        boolean z2 = firstVisiblePosition == 0;
        getTop();
        int bottom = getBottom();
        int childCount2 = getChildCount();
        ArrayList<View> arrayList = this.mNowViewList;
        if (arrayList == null) {
            this.mNowViewList = new ArrayList<>();
        } else {
            arrayList.clear();
        }
        int i2 = 0;
        for (int i3 = 0; i3 < childCount2; i3++) {
            View childAt = getChildAt(i3);
            this.mNowViewList.add(childAt);
            if (i3 == 0 && childAt != null) {
                i2 = childAt.getHeight();
            }
        }
        String str = "OPListView";
        if (this.mOriLastPage) {
            int i4 = this.mOriUpperDeleteCount;
            if (i4 == 0) {
                if (this.mOriCurDeleteCount != 0) {
                    Log.d(str, "DeleteAnimation Case 14 ");
                }
            } else if (this.mOriCurDeleteCount == 0) {
                if (i4 >= this.mOriCurLeftCount) {
                    Log.d(str, "DeleteAnimation Case 12 ");
                    this.mDelOriViewTopList.clear();
                } else {
                    Log.d(str, "DeleteAnimation Case 13 ");
                    for (int i5 = 0; i5 < this.mOriUpperDeleteCount; i5++) {
                        this.mDelOriViewTopList.remove(0);
                    }
                }
            } else if (z2) {
                Log.d(str, "DeleteAnimation Case 17 ");
            } else if (i4 >= this.mOriCurLeftCount) {
                Log.d(str, "DeleteAnimation Case 15 ");
            } else {
                Log.d(str, "DeleteAnimation Case 16 ");
            }
            int i6 = 1;
            while (childCount > this.mDelOriViewTopList.size()) {
                this.mDelOriViewTopList.add(0, Integer.valueOf((-i2) * i6));
                i6++;
            }
        } else if (!z) {
            int i7 = this.mOriUpperDeleteCount;
            if (i7 == 0) {
                Log.d(str, "DeleteAnimation Case 1");
            } else if (i7 >= this.mOriCurLeftCount) {
                Log.d(str, "DeleteAnimation Case 3 ");
                this.mDelOriViewTopList.clear();
            } else {
                Log.d(str, "DeleteAnimation Case 2 ");
                for (int i8 = 0; i8 < this.mOriUpperDeleteCount; i8++) {
                    this.mDelOriViewTopList.remove(0);
                }
            }
        } else {
            if (!z2) {
                int i9 = this.mOriUpperDeleteCount;
                if (i9 == 0) {
                    Log.d(str, "DeleteAnimation Case 4 ");
                } else if (this.mOriCurDeleteCount == 0) {
                    if (i9 >= this.mOriCurLeftCount) {
                        Log.d(str, "DeleteAnimation Case 9 ");
                    } else {
                        Log.d(str, "DeleteAnimation Case 10 ");
                    }
                } else if (i9 >= this.mOriCurLeftCount) {
                    Log.d(str, "DeleteAnimation Case 5 ");
                } else {
                    Log.d(str, "DeleteAnimation Case 6 ");
                }
            } else if (this.mOriCurDeleteCount == 0) {
                Log.d(str, "DeleteAnimation Case 11 ");
            } else if (this.mOriUpperDeleteCount >= this.mOriCurLeftCount) {
                Log.d(str, "DeleteAnimation Case 7 ");
            } else {
                Log.d(str, "DeleteAnimation Case 8 ");
            }
            int i10 = 0;
            while (i10 < this.mOriBelowLeftCount) {
                i10++;
                this.mDelOriViewTopList.add(Integer.valueOf((i10 * i2) + bottom));
            }
            int size = this.mDelOriViewTopList.size() - childCount;
            for (int i11 = 0; i11 < size; i11++) {
                this.mDelOriViewTopList.remove(0);
            }
            int i12 = 1;
            while (childCount > this.mDelOriViewTopList.size()) {
                this.mDelOriViewTopList.add(0, Integer.valueOf((-i2) * i12));
                i12++;
            }
        }
        int size2 = this.mNowViewList.size() - this.mDelOriViewTopList.size();
        int i13 = 0;
        while (i13 < size2) {
            i13++;
            this.mDelOriViewTopList.add(Integer.valueOf((i2 * i13) + bottom));
        }
        int i14 = 0;
        for (int i15 = childCount2 - 1; i15 >= 0; i15--) {
            if (((View) this.mNowViewList.get(i15)).getTop() == ((Integer) this.mDelOriViewTopList.get(i15)).intValue()) {
                this.mNowViewList.remove(i15);
                this.mDelOriViewTopList.remove(i15);
            } else if (((Integer) this.mDelOriViewTopList.get(i15)).intValue() < ((View) this.mNowViewList.get(i15)).getTop()) {
                i14++;
            }
        }
        if (i14 > 1) {
            ArrayList arrayList2 = (ArrayList) this.mNowViewList.clone();
            ArrayList arrayList3 = (ArrayList) this.mDelOriViewTopList.clone();
            this.mNowViewList.clear();
            this.mDelOriViewTopList.clear();
            while (i < arrayList2.size()) {
                int i16 = i < i14 ? (i14 - i) - 1 : i;
                this.mNowViewList.add((View) arrayList2.get(i16));
                this.mDelOriViewTopList.add((Integer) arrayList3.get(i16));
                i++;
            }
        }
    }
}

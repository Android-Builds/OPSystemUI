package com.android.systemui.statusbar.notification.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R$dimen;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.ExpandableView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class StackStateAnimator {
    public static final int ANIMATION_DURATION_HEADS_UP_APPEAR_CLOSED = ((int) (HeadsUpAppearInterpolator.getFractionUntilOvershoot() * 550.0f));
    /* access modifiers changed from: private */
    public AnimationFilter mAnimationFilter = new AnimationFilter();
    /* access modifiers changed from: private */
    public Stack<AnimatorListenerAdapter> mAnimationListenerPool = new Stack<>();
    private final AnimationProperties mAnimationProperties;
    /* access modifiers changed from: private */
    public HashSet<Animator> mAnimatorSet = new HashSet<>();
    /* access modifiers changed from: private */
    public ValueAnimator mBottomOverScrollAnimator;
    private long mCurrentAdditionalDelay;
    private long mCurrentLength;
    private final int mGoToFullShadeAppearingTranslation;
    /* access modifiers changed from: private */
    public HashSet<View> mHeadsUpAppearChildren = new HashSet<>();
    private int mHeadsUpAppearHeightBottom;
    private HashSet<View> mHeadsUpDisappearChildren = new HashSet<>();
    public NotificationStackScrollLayout mHostLayout;
    /* access modifiers changed from: private */
    public ArrayList<View> mNewAddChildren = new ArrayList<>();
    private ArrayList<AnimationEvent> mNewEvents = new ArrayList<>();
    private final int mPulsingAppearingTranslation;
    private boolean mShadeExpanded;
    private NotificationShelf mShelf;
    private int[] mTmpLocation = new int[2];
    private final ExpandableViewState mTmpState = new ExpandableViewState();
    /* access modifiers changed from: private */
    public ValueAnimator mTopOverScrollAnimator;
    private ArrayList<ExpandableView> mTransientViewsToRemove = new ArrayList<>();

    public StackStateAnimator(NotificationStackScrollLayout notificationStackScrollLayout) {
        this.mHostLayout = notificationStackScrollLayout;
        this.mGoToFullShadeAppearingTranslation = notificationStackScrollLayout.getContext().getResources().getDimensionPixelSize(R$dimen.go_to_full_shade_appearing_translation);
        this.mPulsingAppearingTranslation = notificationStackScrollLayout.getContext().getResources().getDimensionPixelSize(R$dimen.pulsing_notification_appear_translation);
        this.mAnimationProperties = new AnimationProperties() {
            public AnimationFilter getAnimationFilter() {
                return StackStateAnimator.this.mAnimationFilter;
            }

            public AnimatorListenerAdapter getAnimationFinishListener() {
                return StackStateAnimator.this.getGlobalAnimationFinishedListener();
            }

            public boolean wasAdded(View view) {
                return StackStateAnimator.this.mNewAddChildren.contains(view);
            }

            public Interpolator getCustomInterpolator(View view, Property property) {
                if (!StackStateAnimator.this.mHeadsUpAppearChildren.contains(view) || !View.TRANSLATION_Y.equals(property)) {
                    return null;
                }
                return Interpolators.HEADS_UP_APPEAR;
            }
        };
    }

    public boolean isRunning() {
        return !this.mAnimatorSet.isEmpty();
    }

    public void startAnimationForEvents(ArrayList<AnimationEvent> arrayList, long j) {
        processAnimationEvents(arrayList);
        int childCount = this.mHostLayout.getChildCount();
        this.mAnimationFilter.applyCombination(this.mNewEvents);
        this.mCurrentAdditionalDelay = j;
        this.mCurrentLength = AnimationEvent.combineLength(this.mNewEvents);
        int i = 0;
        for (int i2 = 0; i2 < childCount; i2++) {
            ExpandableView expandableView = (ExpandableView) this.mHostLayout.getChildAt(i2);
            ExpandableViewState viewState = expandableView.getViewState();
            if (!(viewState == null || expandableView.getVisibility() == 8 || applyWithoutAnimation(expandableView, viewState))) {
                if (this.mAnimationProperties.wasAdded(expandableView) && i < 5) {
                    i++;
                }
                initAnimationProperties(expandableView, viewState, i);
                viewState.animateTo(expandableView, this.mAnimationProperties);
            }
        }
        if (!isRunning()) {
            onAnimationFinished();
        }
        this.mHeadsUpAppearChildren.clear();
        this.mHeadsUpDisappearChildren.clear();
        this.mNewEvents.clear();
        this.mNewAddChildren.clear();
    }

    private void initAnimationProperties(ExpandableView expandableView, ExpandableViewState expandableViewState, int i) {
        boolean wasAdded = this.mAnimationProperties.wasAdded(expandableView);
        this.mAnimationProperties.duration = this.mCurrentLength;
        adaptDurationWhenGoingToFullShade(expandableView, expandableViewState, wasAdded, i);
        this.mAnimationProperties.delay = 0;
        if (!wasAdded) {
            if (!this.mAnimationFilter.hasDelays) {
                return;
            }
            if (expandableViewState.yTranslation == expandableView.getTranslationY() && expandableViewState.zTranslation == expandableView.getTranslationZ() && expandableViewState.alpha == expandableView.getAlpha() && expandableViewState.height == expandableView.getActualHeight() && expandableViewState.clipTopAmount == expandableView.getClipTopAmount() && expandableViewState.dark == expandableView.isDark()) {
                return;
            }
        }
        this.mAnimationProperties.delay = this.mCurrentAdditionalDelay + calculateChildAnimationDelay(expandableViewState, i);
    }

    private void adaptDurationWhenGoingToFullShade(ExpandableView expandableView, ExpandableViewState expandableViewState, boolean z, int i) {
        if (z && this.mAnimationFilter.hasGoToFullShadeEvent) {
            expandableView.setTranslationY(expandableView.getTranslationY() + ((float) this.mGoToFullShadeAppearingTranslation));
            this.mAnimationProperties.duration = ((long) (((float) Math.pow((double) i, 0.699999988079071d)) * 100.0f)) + 514;
        }
    }

    private boolean applyWithoutAnimation(ExpandableView expandableView, ExpandableViewState expandableViewState) {
        if (this.mShadeExpanded || ViewState.isAnimatingY(expandableView) || this.mHeadsUpDisappearChildren.contains(expandableView) || this.mHeadsUpAppearChildren.contains(expandableView) || NotificationStackScrollLayout.isPinnedHeadsUp(expandableView)) {
            return false;
        }
        expandableViewState.applyToView(expandableView);
        return true;
    }

    private long calculateChildAnimationDelay(ExpandableViewState expandableViewState, int i) {
        ExpandableView expandableView;
        AnimationFilter animationFilter = this.mAnimationFilter;
        if (animationFilter.hasGoToFullShadeEvent) {
            return calculateDelayGoToFullShade(expandableViewState, i);
        }
        long j = animationFilter.customDelay;
        if (j != -1) {
            return j;
        }
        long j2 = 0;
        Iterator it = this.mNewEvents.iterator();
        while (it.hasNext()) {
            AnimationEvent animationEvent = (AnimationEvent) it.next();
            long j3 = 80;
            int i2 = animationEvent.animationType;
            if (i2 != 0) {
                if (i2 != 1) {
                    if (i2 == 2) {
                        j3 = 32;
                    }
                }
                int i3 = expandableViewState.notGoneIndex;
                if (animationEvent.viewAfterChangingView == null) {
                    expandableView = this.mHostLayout.getLastChildNotGone();
                } else {
                    expandableView = (ExpandableView) animationEvent.viewAfterChangingView;
                }
                if (expandableView != null) {
                    int i4 = expandableView.getViewState().notGoneIndex;
                    if (i3 >= i4) {
                        i3++;
                    }
                    j2 = Math.max(((long) Math.max(0, Math.min(2, Math.abs(i3 - i4) - 1))) * j3, j2);
                }
            } else {
                j2 = Math.max(((long) (2 - Math.max(0, Math.min(2, Math.abs(expandableViewState.notGoneIndex - animationEvent.mChangingView.getViewState().notGoneIndex) - 1)))) * 80, j2);
            }
        }
        return j2;
    }

    private long calculateDelayGoToFullShade(ExpandableViewState expandableViewState, int i) {
        float f = (float) expandableViewState.notGoneIndex;
        float notGoneIndex = (float) this.mShelf.getNotGoneIndex();
        long j = 0;
        if (f > notGoneIndex) {
            j = 0 + ((long) (((double) (((float) Math.pow((double) i, 0.699999988079071d)) * 48.0f)) * 0.25d));
        } else {
            notGoneIndex = f;
        }
        return j + ((long) (((float) Math.pow((double) notGoneIndex, 0.699999988079071d)) * 48.0f));
    }

    /* access modifiers changed from: private */
    public AnimatorListenerAdapter getGlobalAnimationFinishedListener() {
        if (!this.mAnimationListenerPool.empty()) {
            return (AnimatorListenerAdapter) this.mAnimationListenerPool.pop();
        }
        return new AnimatorListenerAdapter() {
            private boolean mWasCancelled;

            public void onAnimationEnd(Animator animator) {
                StackStateAnimator.this.mAnimatorSet.remove(animator);
                if (StackStateAnimator.this.mAnimatorSet.isEmpty() && !this.mWasCancelled) {
                    StackStateAnimator.this.onAnimationFinished();
                }
                StackStateAnimator.this.mAnimationListenerPool.push(this);
            }

            public void onAnimationCancel(Animator animator) {
                this.mWasCancelled = true;
            }

            public void onAnimationStart(Animator animator) {
                this.mWasCancelled = false;
                StackStateAnimator.this.mAnimatorSet.add(animator);
            }
        };
    }

    /* access modifiers changed from: private */
    public void onAnimationFinished() {
        this.mHostLayout.onChildAnimationFinished();
        Iterator it = this.mTransientViewsToRemove.iterator();
        while (it.hasNext()) {
            ExpandableView expandableView = (ExpandableView) it.next();
            expandableView.getTransientContainer().removeTransientView(expandableView);
        }
        this.mTransientViewsToRemove.clear();
    }

    private void processAnimationEvents(ArrayList<AnimationEvent> arrayList) {
        float f;
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            AnimationEvent animationEvent = (AnimationEvent) it.next();
            ExpandableView expandableView = animationEvent.mChangingView;
            int i = animationEvent.animationType;
            if (i == 0) {
                ExpandableViewState viewState = expandableView.getViewState();
                if (viewState != null && !viewState.gone) {
                    viewState.applyToView(expandableView);
                    this.mNewAddChildren.add(expandableView);
                }
            } else {
                boolean z = true;
                if (i == 1) {
                    if (expandableView.getVisibility() != 0) {
                        removeTransientView(expandableView);
                    } else {
                        if (animationEvent.viewAfterChangingView != null) {
                            float translationY = expandableView.getTranslationY();
                            if (expandableView instanceof ExpandableNotificationRow) {
                                View view = animationEvent.viewAfterChangingView;
                                if (view instanceof ExpandableNotificationRow) {
                                    ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
                                    ExpandableNotificationRow expandableNotificationRow2 = (ExpandableNotificationRow) view;
                                    if (expandableNotificationRow.isRemoved() && expandableNotificationRow.wasChildInGroupWhenRemoved() && !expandableNotificationRow2.isChildInGroup()) {
                                        translationY = expandableNotificationRow.getTranslationWhenRemoved();
                                    }
                                }
                            }
                            float actualHeight = (float) expandableView.getActualHeight();
                            f = Math.max(Math.min(((((ExpandableView) animationEvent.viewAfterChangingView).getViewState().yTranslation - (translationY + (actualHeight / 2.0f))) * 2.0f) / actualHeight, 1.0f), -1.0f);
                        } else {
                            f = -1.0f;
                        }
                        expandableView.performRemoveAnimation(464, 0, f, false, 0.0f, new Runnable() {
                            public final void run() {
                                StackStateAnimator.removeTransientView(ExpandableView.this);
                            }
                        }, null);
                    }
                } else if (i == 2) {
                    if (Math.abs(expandableView.getTranslation()) == ((float) expandableView.getWidth()) && expandableView.getTransientContainer() != null) {
                        expandableView.getTransientContainer().removeTransientView(expandableView);
                    }
                } else if (i == 11) {
                    ((ExpandableNotificationRow) expandableView).prepareExpansionChanged();
                } else {
                    float f2 = 0.0f;
                    if (i == 12) {
                        this.mTmpState.copyFrom(expandableView.getViewState());
                        if (animationEvent.headsUpFromBottom) {
                            this.mTmpState.yTranslation = (float) this.mHeadsUpAppearHeightBottom;
                        } else {
                            this.mTmpState.yTranslation = 0.0f;
                            expandableView.performAddAnimation(0, (long) ANIMATION_DURATION_HEADS_UP_APPEAR_CLOSED, true);
                        }
                        this.mHeadsUpAppearChildren.add(expandableView);
                        this.mTmpState.applyToView(expandableView);
                    } else if (i == 13 || i == 14) {
                        this.mHeadsUpDisappearChildren.add(expandableView);
                        $$Lambda$StackStateAnimator$_Pk5aD8YGtEkv3ND7OecxMpqHJ4 r1 = null;
                        int i2 = animationEvent.animationType == 14 ? 120 : 0;
                        if (expandableView.getParent() == null) {
                            this.mHostLayout.addTransientView(expandableView, 0);
                            expandableView.setTransientContainer(this.mHostLayout);
                            this.mTmpState.initFrom(expandableView);
                            ExpandableViewState expandableViewState = this.mTmpState;
                            expandableViewState.yTranslation = 0.0f;
                            this.mAnimationFilter.animateY = true;
                            AnimationProperties animationProperties = this.mAnimationProperties;
                            animationProperties.delay = (long) (i2 + 120);
                            animationProperties.duration = 300;
                            expandableViewState.animateTo(expandableView, animationProperties);
                            r1 = new Runnable() {
                                public final void run() {
                                    StackStateAnimator.removeTransientView(ExpandableView.this);
                                }
                            };
                        }
                        $$Lambda$StackStateAnimator$_Pk5aD8YGtEkv3ND7OecxMpqHJ4 r9 = r1;
                        if (expandableView instanceof ExpandableNotificationRow) {
                            ExpandableNotificationRow expandableNotificationRow3 = (ExpandableNotificationRow) expandableView;
                            z = true ^ expandableNotificationRow3.isDismissed();
                            StatusBarIconView statusBarIconView = expandableNotificationRow3.getEntry().icon;
                            if (statusBarIconView.getParent() != null) {
                                statusBarIconView.getLocationOnScreen(this.mTmpLocation);
                                float translationX = (((float) this.mTmpLocation[0]) - statusBarIconView.getTranslationX()) + ViewState.getFinalTranslationX(statusBarIconView) + (((float) statusBarIconView.getWidth()) * 0.25f);
                                this.mHostLayout.getLocationOnScreen(this.mTmpLocation);
                                f2 = translationX - ((float) this.mTmpLocation[0]);
                            }
                        }
                        float f3 = f2;
                        if (z) {
                            long performRemoveAnimation = expandableView.performRemoveAnimation(420, (long) i2, 0.0f, true, f3, r9, getGlobalAnimationFinishedListener());
                            this.mAnimationProperties.delay += performRemoveAnimation;
                        } else if (r9 != null) {
                            r9.run();
                        }
                    }
                }
            }
            this.mNewEvents.add(animationEvent);
        }
    }

    public static void removeTransientView(ExpandableView expandableView) {
        if (expandableView.getTransientContainer() != null) {
            expandableView.getTransientContainer().removeTransientView(expandableView);
        }
    }

    public void animateOverScrollToAmount(float f, final boolean z, final boolean z2) {
        float currentOverScrollAmount = this.mHostLayout.getCurrentOverScrollAmount(z);
        if (f != currentOverScrollAmount) {
            cancelOverScrollAnimators(z);
            ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{currentOverScrollAmount, f});
            ofFloat.setDuration(360);
            ofFloat.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    StackStateAnimator.this.mHostLayout.setOverScrollAmount(((Float) valueAnimator.getAnimatedValue()).floatValue(), z, false, false, z2);
                }
            });
            ofFloat.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            ofFloat.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    if (z) {
                        StackStateAnimator.this.mTopOverScrollAnimator = null;
                    } else {
                        StackStateAnimator.this.mBottomOverScrollAnimator = null;
                    }
                }
            });
            ofFloat.start();
            if (z) {
                this.mTopOverScrollAnimator = ofFloat;
            } else {
                this.mBottomOverScrollAnimator = ofFloat;
            }
        }
    }

    public void cancelOverScrollAnimators(boolean z) {
        ValueAnimator valueAnimator = z ? this.mTopOverScrollAnimator : this.mBottomOverScrollAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
    }

    public void setHeadsUpAppearHeightBottom(int i) {
        this.mHeadsUpAppearHeightBottom = i;
    }

    public void setShadeExpanded(boolean z) {
        this.mShadeExpanded = z;
    }

    public void setShelf(NotificationShelf notificationShelf) {
        this.mShelf = notificationShelf;
    }
}

package com.android.systemui.statusbar.notification.stack;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.os.Debug;
import android.os.ServiceManager;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.MathUtils;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.OverScroller;
import android.widget.ScrollView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.graphics.ColorUtils;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.IStatusBarService.Stub;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.ExpandHelper;
import com.android.systemui.ExpandHelper.Callback;
import com.android.systemui.Interpolators;
import com.android.systemui.R$anim;
import com.android.systemui.R$attr;
import com.android.systemui.R$bool;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.R$style;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin.MenuItem;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin.OnMenuEventListener;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper.SnoozeOption;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.AmbientPulseManager;
import com.android.systemui.statusbar.DragDownHelper.DragDownCallback;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.RemoteInputController.Delegate;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator.ExpandAnimationParameters;
import com.android.systemui.statusbar.notification.DynamicPrivacyController;
import com.android.systemui.statusbar.notification.DynamicPrivacyController.Listener;
import com.android.systemui.statusbar.notification.NotificationEntryListener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.VisibilityLocationProvider;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.logging.NotificationLogger.OnChildLocationsChangedListener;
import com.android.systemui.statusbar.notification.row.ActivatableNotificationView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow.LongPressListener;
import com.android.systemui.statusbar.notification.row.ExpandableView;
import com.android.systemui.statusbar.notification.row.ExpandableView.OnHeightChangedListener;
import com.android.systemui.statusbar.notification.row.FooterView;
import com.android.systemui.statusbar.notification.row.NotificationBlockingHelperManager;
import com.android.systemui.statusbar.notification.row.NotificationGuts;
import com.android.systemui.statusbar.notification.row.NotificationGutsManager;
import com.android.systemui.statusbar.notification.row.NotificationSnooze;
import com.android.systemui.statusbar.notification.row.StackScrollerDecorView;
import com.android.systemui.statusbar.notification.stack.NotificationSwipeHelper.NotificationCallback;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.HeadsUpAppearanceController;
import com.android.systemui.statusbar.phone.HeadsUpManagerPhone;
import com.android.systemui.statusbar.phone.HeadsUpManagerPhone.AnimationStateHandler;
import com.android.systemui.statusbar.phone.HeadsUpTouchHelper;
import com.android.systemui.statusbar.phone.LockscreenGestureLogger;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.NotificationGroupManager.NotificationGroup;
import com.android.systemui.statusbar.phone.NotificationGroupManager.OnGroupChangeListener;
import com.android.systemui.statusbar.phone.NotificationIconAreaController;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.statusbar.policy.HeadsUpUtil;
import com.android.systemui.statusbar.policy.ScrollAdapter;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.android.systemui.util.Assert;
import com.oneplus.plugin.OpLsState;
import com.oneplus.util.OpUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NotificationStackScrollLayout extends ViewGroup implements ScrollAdapter, NotificationListContainer, ConfigurationListener, Dumpable, Listener {
    protected static final boolean DEBUG_ONEPLUS = Build.DEBUG_ONEPLUS;
    private boolean mActivateNeedsAnimation;
    private int mActivePointerId = -1;
    private ArrayList<View> mAddedHeadsUpChildren = new ArrayList<>();
    private final boolean mAllowLongPress;
    private final AmbientPulseManager mAmbientPulseManager;
    /* access modifiers changed from: private */
    public final AmbientState mAmbientState;
    private boolean mAnimateBottomOnLayout;
    private boolean mAnimateNextBackgroundBottom;
    private boolean mAnimateNextBackgroundTop;
    private boolean mAnimateNextSectionBoundsChange;
    private ArrayList<AnimationEvent> mAnimationEvents = new ArrayList<>();
    private HashSet<Runnable> mAnimationFinishedRunnables = new HashSet<>();
    private boolean mAnimationRunning;
    /* access modifiers changed from: private */
    public boolean mAnimationsEnabled;
    private int mAntiBurnInOffsetX;
    /* access modifiers changed from: private */
    public final Rect mBackgroundAnimationRect = new Rect();
    private final Paint mBackgroundPaint = new Paint();
    private OnPreDrawListener mBackgroundUpdater = new OnPreDrawListener() {
        public final boolean onPreDraw() {
            return NotificationStackScrollLayout.this.lambda$new$0$NotificationStackScrollLayout();
        }
    };
    /* access modifiers changed from: private */
    public float mBackgroundXFactor = 1.0f;
    private boolean mBackwardScrollable;
    private final IStatusBarService mBarService = Stub.asInterface(ServiceManager.getService("statusbar"));
    private int mBgColor;
    private int mBottomInset = 0;
    private int mBottomMargin;
    private int mCachedBackgroundColor;
    private boolean mChangePositionInProgress;
    boolean mCheckForLeavebehind;
    private boolean mChildTransferInProgress;
    private ArrayList<ExpandableView> mChildrenChangingPositions = new ArrayList<>();
    private HashSet<ExpandableView> mChildrenToAddAnimated = new HashSet<>();
    private ArrayList<ExpandableView> mChildrenToRemoveAnimated = new ArrayList<>();
    /* access modifiers changed from: private */
    public boolean mChildrenUpdateRequested;
    private OnPreDrawListener mChildrenUpdater = new OnPreDrawListener() {
        public boolean onPreDraw() {
            NotificationStackScrollLayout.this.updateForcedScroll();
            NotificationStackScrollLayout.this.updateChildren();
            NotificationStackScrollLayout.this.mChildrenUpdateRequested = false;
            NotificationStackScrollLayout.this.getViewTreeObserver().removeOnPreDrawListener(this);
            return true;
        }
    };
    protected boolean mClearAllEnabled;
    private HashSet<ExpandableView> mClearTransientViewsWhenFinished = new HashSet<>();
    private final Rect mClipRect = new Rect();
    private int mCollapsedSize;
    private final SysuiColorExtractor mColorExtractor = ((SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class));
    private int mContentHeight;
    private boolean mContinuousBackgroundUpdate;
    private boolean mContinuousShadowUpdate;
    /* access modifiers changed from: private */
    public int mCornerRadius;
    private int mCurrentStackHeight = Integer.MAX_VALUE;
    private int mDarkAnimationOriginIndex;
    private boolean mDarkNeedsAnimation;
    /* access modifiers changed from: private */
    public Interpolator mDarkXInterpolator = Interpolators.FAST_OUT_SLOW_IN;
    private float mDimAmount;
    /* access modifiers changed from: private */
    public ValueAnimator mDimAnimator;
    private final AnimatorListener mDimEndListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            NotificationStackScrollLayout.this.mDimAnimator = null;
        }
    };
    private AnimatorUpdateListener mDimUpdateListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            NotificationStackScrollLayout.this.setDimAmount(((Float) valueAnimator.getAnimatedValue()).floatValue());
        }
    };
    private boolean mDimmedNeedsAnimation;
    private boolean mDisallowDismissInThisMotion;
    private boolean mDisallowScrollingInThisMotion;
    /* access modifiers changed from: private */
    public View mDismissAllButton;
    /* access modifiers changed from: private */
    public boolean mDismissAllInProgress;
    private boolean mDismissRtl;
    private boolean mDismissShow = false;
    /* access modifiers changed from: private */
    public final DisplayMetrics mDisplayMetrics = ((DisplayMetrics) Dependency.get(DisplayMetrics.class));
    /* access modifiers changed from: private */
    public boolean mDontClampNextScroll;
    /* access modifiers changed from: private */
    public boolean mDontReportNextOverScroll;
    private int mDownX;
    private final DragDownCallback mDragDownCallback = new DragDownCallback() {
        public boolean onDraggedDown(View view, int i) {
            if (NotificationStackScrollLayout.this.mStatusBar != null && NotificationStackScrollLayout.this.onKeyguard() && NotificationStackScrollLayout.this.mStatusBar.isQsDisabled()) {
                Log.d("StackScroller", "return DraggedDown");
                return false;
            } else if (NotificationStackScrollLayout.this.mStatusBarState != 1 || !NotificationStackScrollLayout.this.hasActiveNotifications()) {
                return false;
            } else {
                NotificationStackScrollLayout.this.mLockscreenGestureLogger.write(187, (int) (((float) i) / NotificationStackScrollLayout.this.mDisplayMetrics.density), 0);
                if (!NotificationStackScrollLayout.this.mAmbientState.isDark() || view != null) {
                    NotificationStackScrollLayout.this.mShadeController.goToLockedShade(view);
                    if (view instanceof ExpandableNotificationRow) {
                        ((ExpandableNotificationRow) view).onExpandedByGesture(true);
                    }
                }
                return true;
            }
        }

        public void onDragDownReset() {
            NotificationStackScrollLayout.this.setDimmed(true, true);
            NotificationStackScrollLayout.this.resetScrollPosition();
            NotificationStackScrollLayout.this.resetCheckSnoozeLeavebehind();
        }

        public void onCrossedThreshold(boolean z) {
            NotificationStackScrollLayout.this.setDimmed(!z, true);
        }

        public void onTouchSlopExceeded() {
            NotificationStackScrollLayout.this.cancelLongPress();
            NotificationStackScrollLayout.this.checkSnoozeLeavebehind();
        }

        public void setEmptyDragAmount(float f) {
            NotificationStackScrollLayout.this.mNotificationPanel.setEmptyDragAmount(f);
        }

        public boolean isFalsingCheckNeeded() {
            return NotificationStackScrollLayout.this.mStatusBarState == 1;
        }
    };
    protected EmptyShadeView mEmptyShadeView;
    private final NotificationEntryManager mEntryManager = ((NotificationEntryManager) Dependency.get(NotificationEntryManager.class));
    private boolean mEverythingNeedsAnimation;
    private ExpandHelper mExpandHelper;
    private Callback mExpandHelperCallback = new Callback() {
        public ExpandableView getChildAtPosition(float f, float f2) {
            return NotificationStackScrollLayout.this.getChildAtPosition(f, f2);
        }

        public ExpandableView getChildAtRawPosition(float f, float f2) {
            return NotificationStackScrollLayout.this.getChildAtRawPosition(f, f2);
        }

        public boolean canChildBeExpanded(View view) {
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                if (expandableNotificationRow.isExpandable() && !expandableNotificationRow.areGutsExposed() && (NotificationStackScrollLayout.this.mIsExpanded || !expandableNotificationRow.isPinned())) {
                    return true;
                }
            }
            return false;
        }

        public void setUserExpandedChild(View view, boolean z) {
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                if (!z || !NotificationStackScrollLayout.this.onKeyguard()) {
                    expandableNotificationRow.setUserExpanded(z, true);
                    expandableNotificationRow.onExpandedByGesture(z);
                } else {
                    expandableNotificationRow.setUserLocked(false);
                    NotificationStackScrollLayout.this.updateContentHeight();
                    NotificationStackScrollLayout.this.notifyHeightChangeListener(expandableNotificationRow);
                }
            }
        }

        public void setExpansionCancelled(View view) {
            if (view instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) view).setGroupExpansionChanging(false);
            }
        }

        public void setUserLockedChild(View view, boolean z) {
            if (view instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) view).setUserLocked(z);
            }
            NotificationStackScrollLayout.this.cancelLongPress();
            NotificationStackScrollLayout.this.requestDisallowInterceptTouchEvent(true);
        }

        public void expansionStateChanged(boolean z) {
            NotificationStackScrollLayout.this.mExpandingNotification = z;
            if (!NotificationStackScrollLayout.this.mExpandedInThisMotion) {
                NotificationStackScrollLayout notificationStackScrollLayout = NotificationStackScrollLayout.this;
                notificationStackScrollLayout.mMaxScrollAfterExpand = notificationStackScrollLayout.mOwnScrollY;
                NotificationStackScrollLayout.this.mExpandedInThisMotion = true;
            }
        }

        public int getMaxExpandHeight(ExpandableView expandableView) {
            return expandableView.getMaxContentHeight();
        }
    };
    /* access modifiers changed from: private */
    public ExpandableView mExpandedGroupView;
    private float mExpandedHeight;
    private ArrayList<BiConsumer<Float, Float>> mExpandedHeightListeners = new ArrayList<>();
    /* access modifiers changed from: private */
    public boolean mExpandedInThisMotion;
    /* access modifiers changed from: private */
    public boolean mExpandingNotification;
    /* access modifiers changed from: private */
    public boolean mFadeNotificationsOnDismiss;
    /* access modifiers changed from: private */
    public FalsingManager mFalsingManager;
    private Runnable mFinishScrollingCallback;
    protected FooterView mFooterView;
    private boolean mForceNoOverlappingRendering;
    private View mForcedScroll;
    private boolean mForwardScrollable;
    private HashSet<View> mFromMoreCardAdditions = new HashSet<>();
    private boolean mGenerateChildOrderChangedEvent;
    private long mGoToFullShadeDelay;
    private boolean mGoToFullShadeNeedsAnimation;
    /* access modifiers changed from: private */
    public boolean mGroupExpandedForMeasure;
    private NotificationGroupManager mGroupManager;
    private boolean mHeadsUpAnimatingAway;
    private HeadsUpAppearanceController mHeadsUpAppearanceController;
    private final HeadsUpTouchHelper.Callback mHeadsUpCallback = new HeadsUpTouchHelper.Callback() {
        public ExpandableView getChildAtRawPosition(float f, float f2) {
            return NotificationStackScrollLayout.this.getChildAtRawPosition(f, f2);
        }

        public boolean isExpanded() {
            return NotificationStackScrollLayout.this.mIsExpanded;
        }

        public Context getContext() {
            return NotificationStackScrollLayout.this.mContext;
        }
    };
    private HashSet<Pair<ExpandableNotificationRow, Boolean>> mHeadsUpChangeAnimations = new HashSet<>();
    private boolean mHeadsUpGoingAwayAnimationsAllowed = true;
    private int mHeadsUpInset;
    /* access modifiers changed from: private */
    public HeadsUpManagerPhone mHeadsUpManager;
    private boolean mHideSensitiveNeedsAnimation;
    private boolean mHighPriorityBeforeSpeedBump;
    private float mHorizontalPanelTranslation;
    private NotificationIconAreaController mIconAreaController;
    private boolean mInHeadsUpPinnedMode;
    private int mIncreasedPaddingBetweenElements;
    private boolean mInited = false;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private float mInterpolatedDarkAmount = 0.0f;
    private int mIntrinsicContentHeight;
    private int mIntrinsicPadding;
    private boolean mIsBeingDragged;
    private boolean mIsClipped;
    /* access modifiers changed from: private */
    public boolean mIsExpanded = true;
    private boolean mIsExpansionChanging;
    private int mLastMotionY;
    /* access modifiers changed from: private */
    public float mLinearDarkAmount = 0.0f;
    private OnChildLocationsChangedListener mListener;
    /* access modifiers changed from: private */
    public final LockscreenGestureLogger mLockscreenGestureLogger = ((LockscreenGestureLogger) Dependency.get(LockscreenGestureLogger.class));
    private final NotificationLockscreenUserManager mLockscreenUserManager = ((NotificationLockscreenUserManager) Dependency.get(NotificationLockscreenUserManager.class));
    /* access modifiers changed from: private */
    public LongPressListener mLongPressListener;
    private int mMaxDisplayedNotifications = -1;
    private int mMaxLayoutHeight;
    private float mMaxOverScroll;
    /* access modifiers changed from: private */
    public int mMaxScrollAfterExpand;
    private int mMaxTopPadding;
    private int mMaximumVelocity;
    @VisibleForTesting
    protected final OnMenuEventListener mMenuEventListener = new OnMenuEventListener() {
        public void onMenuClicked(View view, int i, int i2, MenuItem menuItem) {
            if (NotificationStackScrollLayout.this.mLongPressListener != null) {
                if (view instanceof ExpandableNotificationRow) {
                    NotificationStackScrollLayout.this.mMetricsLogger.write(((ExpandableNotificationRow) view).getStatusBarNotification().getLogMaker().setCategory(333).setType(4));
                }
                NotificationStackScrollLayout.this.mLongPressListener.onLongPress(view, i, i2, menuItem);
            }
        }

        public void onMenuReset(View view) {
            View translatingParentView = NotificationStackScrollLayout.this.mSwipeHelper.getTranslatingParentView();
            if (translatingParentView != null && view == translatingParentView) {
                NotificationStackScrollLayout.this.mSwipeHelper.clearExposedMenuView();
                NotificationStackScrollLayout.this.mSwipeHelper.clearTranslatingParentView();
                if (view instanceof ExpandableNotificationRow) {
                    NotificationStackScrollLayout.this.mHeadsUpManager.setMenuShown(((ExpandableNotificationRow) view).getEntry(), false);
                }
            }
        }

        public void onMenuShown(View view) {
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                NotificationStackScrollLayout.this.mMetricsLogger.write(expandableNotificationRow.getStatusBarNotification().getLogMaker().setCategory(332).setType(4));
                NotificationStackScrollLayout.this.mHeadsUpManager.setMenuShown(expandableNotificationRow.getEntry(), true);
                NotificationStackScrollLayout.this.mSwipeHelper.onMenuShown(view);
                NotificationStackScrollLayout.this.mNotificationGutsManager.closeAndSaveGuts(true, false, false, -1, -1, false);
                NotificationMenuRowPlugin provider = expandableNotificationRow.getProvider();
                if (provider.shouldShowGutsOnSnapOpen()) {
                    MenuItem menuItemToExposeOnSnap = provider.menuItemToExposeOnSnap();
                    if (menuItemToExposeOnSnap != null) {
                        Point revealAnimationOrigin = provider.getRevealAnimationOrigin();
                        NotificationStackScrollLayout.this.mNotificationGutsManager.openGuts(view, revealAnimationOrigin.x, revealAnimationOrigin.y, menuItemToExposeOnSnap);
                    } else {
                        Log.e("StackScroller", "Provider has shouldShowGutsOnSnapOpen, but provided no menu item in menuItemtoExposeOnSnap. Skipping.");
                    }
                    NotificationStackScrollLayout.this.resetExposedMenuView(false, true);
                }
            }
        }
    };
    @VisibleForTesting
    protected final MetricsLogger mMetricsLogger = ((MetricsLogger) Dependency.get(MetricsLogger.class));
    private int mMinInteractionHeight;
    private float mMinTopOverScrollToEscape;
    private int mMinimumVelocity;
    private boolean mNeedViewResizeAnimation;
    /* access modifiers changed from: private */
    public boolean mNeedsAnimation;
    private boolean mNoAmbient;
    private final NotificationCallback mNotificationCallback = new NotificationCallback() {
        public void onDismiss() {
            NotificationStackScrollLayout.this.mNotificationGutsManager.closeAndSaveGuts(true, false, false, -1, -1, false);
        }

        public void onSnooze(StatusBarNotification statusBarNotification, SnoozeOption snoozeOption) {
            NotificationStackScrollLayout.this.mStatusBar.setNotificationSnoozed(statusBarNotification, snoozeOption);
        }

        public boolean shouldDismissQuickly() {
            return NotificationStackScrollLayout.this.isExpanded() && NotificationStackScrollLayout.this.mAmbientState.isFullyAwake();
        }

        public void onDragCancelled(View view) {
            NotificationStackScrollLayout.this.setSwipingInProgress(false);
            NotificationStackScrollLayout.this.mFalsingManager.onNotificatonStopDismissing();
        }

        public void onChildDismissed(View view) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
            if (!expandableNotificationRow.isDismissed()) {
                handleChildViewDismissed(view);
            }
            ViewGroup transientContainer = expandableNotificationRow.getTransientContainer();
            if (transientContainer != null) {
                transientContainer.removeTransientView(view);
            }
        }

        public void handleChildViewDismissed(View view) {
            boolean z = false;
            NotificationStackScrollLayout.this.setSwipingInProgress(false);
            if (!NotificationStackScrollLayout.this.mDismissAllInProgress) {
                NotificationStackScrollLayout.this.mAmbientState.onDragFinished(view);
                NotificationStackScrollLayout.this.updateContinuousShadowDrawing();
                if (view instanceof ExpandableNotificationRow) {
                    ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
                    if (expandableNotificationRow.isHeadsUp()) {
                        NotificationStackScrollLayout.this.mHeadsUpManager.addSwipedOutNotification(expandableNotificationRow.getStatusBarNotification().getKey());
                    }
                    z = expandableNotificationRow.performDismissWithBlockingHelper(false);
                }
                if (!z) {
                    NotificationStackScrollLayout.this.mSwipedOutViews.add(view);
                }
                NotificationStackScrollLayout.this.mFalsingManager.onNotificationDismissed();
                if (NotificationStackScrollLayout.this.mFalsingManager.shouldEnforceBouncer()) {
                    NotificationStackScrollLayout.this.mStatusBar.executeRunnableDismissingKeyguard(null, null, false, true, false);
                }
            }
        }

        public boolean isAntiFalsingNeeded() {
            return NotificationStackScrollLayout.this.onKeyguard();
        }

        public View getChildAtPosition(MotionEvent motionEvent) {
            ExpandableView access$3500 = NotificationStackScrollLayout.this.getChildAtPosition(motionEvent.getX(), motionEvent.getY());
            if (!(access$3500 instanceof ExpandableNotificationRow)) {
                return access$3500;
            }
            ExpandableNotificationRow notificationParent = ((ExpandableNotificationRow) access$3500).getNotificationParent();
            if (notificationParent == null || !notificationParent.areChildrenExpanded()) {
                return access$3500;
            }
            return (notificationParent.areGutsExposed() || NotificationStackScrollLayout.this.mSwipeHelper.getExposedMenuView() == notificationParent || (notificationParent.getNotificationChildren().size() == 1 && notificationParent.getEntry().isClearable())) ? notificationParent : access$3500;
        }

        public void onBeginDrag(View view) {
            NotificationStackScrollLayout.this.mFalsingManager.onNotificatonStartDismissing();
            NotificationStackScrollLayout.this.setSwipingInProgress(true);
            NotificationStackScrollLayout.this.mAmbientState.onBeginDrag((ExpandableView) view);
            NotificationStackScrollLayout.this.updateContinuousShadowDrawing();
            NotificationStackScrollLayout.this.updateContinuousBackgroundDrawing();
            NotificationStackScrollLayout.this.requestChildrenUpdate();
        }

        public void onChildSnappedBack(View view, float f) {
            NotificationStackScrollLayout.this.mAmbientState.onDragFinished(view);
            NotificationStackScrollLayout.this.updateContinuousShadowDrawing();
            NotificationStackScrollLayout.this.updateContinuousBackgroundDrawing();
        }

        public boolean updateSwipeProgress(View view, boolean z, float f) {
            return !NotificationStackScrollLayout.this.mFadeNotificationsOnDismiss;
        }

        public float getFalsingThresholdFactor() {
            return NotificationStackScrollLayout.this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
        }

        public int getConstrainSwipeStartPosition() {
            NotificationMenuRowPlugin currentMenuRow = NotificationStackScrollLayout.this.mSwipeHelper.getCurrentMenuRow();
            if (currentMenuRow != null) {
                return Math.abs(currentMenuRow.getMenuSnapTarget());
            }
            return 0;
        }

        public boolean canChildBeDismissed(View view) {
            return StackScrollAlgorithm.canChildBeDismissed(view);
        }

        public boolean canChildBeDismissedInDirection(View view, boolean z) {
            return canChildBeDismissed(view);
        }
    };
    /* access modifiers changed from: private */
    public final NotificationGutsManager mNotificationGutsManager = ((NotificationGutsManager) Dependency.get(NotificationGutsManager.class));
    /* access modifiers changed from: private */
    public NotificationPanelView mNotificationPanel;
    private OnEmptySpaceClickListener mOnEmptySpaceClickListener;
    private final OnGroupChangeListener mOnGroupChangeListener = new OnGroupChangeListener() {
        public void onGroupExpansionChanged(final ExpandableNotificationRow expandableNotificationRow, boolean z) {
            boolean z2 = !NotificationStackScrollLayout.this.mGroupExpandedForMeasure && NotificationStackScrollLayout.this.mAnimationsEnabled && (NotificationStackScrollLayout.this.mIsExpanded || expandableNotificationRow.isPinned());
            if (z2) {
                NotificationStackScrollLayout.this.mExpandedGroupView = expandableNotificationRow;
                NotificationStackScrollLayout.this.mNeedsAnimation = true;
            }
            expandableNotificationRow.setChildrenExpanded(z, z2);
            if (!NotificationStackScrollLayout.this.mGroupExpandedForMeasure) {
                NotificationStackScrollLayout.this.onHeightChanged(expandableNotificationRow, false);
            }
            NotificationStackScrollLayout.this.runAfterAnimationFinished(new Runnable() {
                public void run() {
                    expandableNotificationRow.onFinishedExpansionChange();
                }
            });
        }

        public void onGroupCreatedFromChildren(NotificationGroup notificationGroup) {
            NotificationStackScrollLayout.this.mStatusBar.requestNotificationUpdate();
        }

        public void onGroupsChanged() {
            NotificationStackScrollLayout.this.mStatusBar.requestNotificationUpdate();
        }
    };
    private OnHeightChangedListener mOnHeightChangedListener;
    private boolean mOnlyScrollingInThisMotion;
    private final ViewOutlineProvider mOutlineProvider = new ViewOutlineProvider() {
        public void getOutline(View view, Outline outline) {
            if (NotificationStackScrollLayout.this.mAmbientState.isDarkAtAll()) {
                outline.setRoundRect(NotificationStackScrollLayout.this.mBackgroundAnimationRect, MathUtils.lerp(((float) NotificationStackScrollLayout.this.mCornerRadius) / 2.0f, (float) NotificationStackScrollLayout.this.mCornerRadius, NotificationStackScrollLayout.this.mDarkXInterpolator.getInterpolation((1.0f - NotificationStackScrollLayout.this.mLinearDarkAmount) * NotificationStackScrollLayout.this.mBackgroundXFactor)));
                return;
            }
            ViewOutlineProvider.BACKGROUND.getOutline(view, outline);
        }
    };
    private float mOverScrolledBottomPixels;
    private float mOverScrolledTopPixels;
    private int mOverflingDistance;
    private OnOverscrollTopChangedListener mOverscrollTopChangedListener;
    /* access modifiers changed from: private */
    public int mOwnScrollY;
    private int mPaddingBetweenElements;
    private boolean mPanelTracking;
    private boolean mPulsing;
    protected ViewGroup mQsContainer;
    private boolean mQsExpanded;
    private float mQsExpansionFraction;
    private Runnable mReclamp = new Runnable() {
        public void run() {
            NotificationStackScrollLayout.this.mScroller.startScroll(NotificationStackScrollLayout.this.mScrollX, NotificationStackScrollLayout.this.mOwnScrollY, 0, NotificationStackScrollLayout.this.getScrollRange() - NotificationStackScrollLayout.this.mOwnScrollY);
            NotificationStackScrollLayout.this.mDontReportNextOverScroll = true;
            NotificationStackScrollLayout.this.mDontClampNextScroll = true;
            NotificationStackScrollLayout.this.lambda$new$1$NotificationStackScrollLayout();
        }
    };
    private Runnable mReflingAndAnimateScroll = new Runnable() {
        public final void run() {
            NotificationStackScrollLayout.this.lambda$new$1$NotificationStackScrollLayout();
        }
    };
    private final NotificationRemoteInputManager mRemoteInputManager = ((NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class));
    private Rect mRequestedClipBounds;
    private final NotificationRoundnessManager mRoundnessManager;
    private OnPreDrawListener mRunningAnimationUpdater = new OnPreDrawListener() {
        public boolean onPreDraw() {
            NotificationStackScrollLayout.this.onPreDrawDuringAnimation();
            return true;
        }
    };
    private ScrimController mScrimController;
    private boolean mScrollable;
    private boolean mScrolledToTopOnFirstDown;
    /* access modifiers changed from: private */
    public OverScroller mScroller;
    protected boolean mScrollingEnabled;
    private NotificationSection[] mSections = new NotificationSection[2];
    private final NotificationSectionsManager mSectionsManager;
    /* access modifiers changed from: private */
    public final ShadeController mShadeController = ((ShadeController) Dependency.get(ShadeController.class));
    private OnPreDrawListener mShadowUpdater = new OnPreDrawListener() {
        public boolean onPreDraw() {
            NotificationStackScrollLayout.this.updateViewShadows();
            return true;
        }
    };
    private NotificationShelf mShelf;
    private final boolean mShouldDrawNotificationBackground;
    private boolean mShouldShowShelfOnly;
    private int mSidePaddings;
    private PorterDuffXfermode mSrcMode = new PorterDuffXfermode(Mode.SRC);
    protected final StackScrollAlgorithm mStackScrollAlgorithm;
    private float mStackTranslation;
    private final StackStateAnimator mStateAnimator = new StackStateAnimator(this);
    private final StateListener mStateListener = new StateListener() {
        public void onStatePreChange(int i, int i2) {
            if (i == 2 && i2 == 1) {
                NotificationStackScrollLayout.this.requestAnimateEverything();
            }
        }

        public void onStateChanged(int i) {
            NotificationStackScrollLayout.this.setStatusBarState(i);
        }

        public void onStatePostChange() {
            NotificationStackScrollLayout.this.onStatePostChange();
        }
    };
    /* access modifiers changed from: private */
    public StatusBar mStatusBar;
    private int mStatusBarHeight;
    /* access modifiers changed from: private */
    public int mStatusBarState;
    /* access modifiers changed from: private */
    public final NotificationSwipeHelper mSwipeHelper;
    /* access modifiers changed from: private */
    public ArrayList<View> mSwipedOutViews = new ArrayList<>();
    private boolean mSwipingInProgress;
    private int[] mTempInt2 = new int[2];
    private final ArrayList<Pair<ExpandableNotificationRow, Boolean>> mTmpList = new ArrayList<>();
    private final Rect mTmpRect = new Rect();
    private ArrayList<ExpandableView> mTmpSortedChildren = new ArrayList<>();
    private int mTopPadding;
    private boolean mTopPaddingNeedsAnimation;
    private float mTopPaddingOverflow;
    private boolean mTouchIsClick;
    private int mTouchSlop;
    private boolean mTrackingHeadsUp;
    private boolean mUsingLightTheme;
    private VelocityTracker mVelocityTracker;
    private Comparator<ExpandableView> mViewPositionComparator = new Comparator<ExpandableView>() {
        public int compare(ExpandableView expandableView, ExpandableView expandableView2) {
            float translationY = expandableView.getTranslationY() + ((float) expandableView.getActualHeight());
            float translationY2 = expandableView2.getTranslationY() + ((float) expandableView2.getActualHeight());
            if (translationY < translationY2) {
                return -1;
            }
            return translationY > translationY2 ? 1 : 0;
        }
    };
    private final VisualStabilityManager mVisualStabilityManager = ((VisualStabilityManager) Dependency.get(VisualStabilityManager.class));

    static class AnimationEvent {
        static AnimationFilter[] FILTERS;
        static int[] LENGTHS = {464, 464, 360, 360, 220, 220, 360, 500, 448, 360, 360, 360, 550, 300, 300, 360, 360};
        final int animationType;
        int darkAnimationOriginIndex;
        final long eventStartTime;
        final AnimationFilter filter;
        boolean headsUpFromBottom;
        final long length;
        final ExpandableView mChangingView;
        View viewAfterChangingView;

        static {
            AnimationFilter animationFilter = new AnimationFilter();
            animationFilter.animateHeight();
            animationFilter.animateTopInset();
            animationFilter.animateY();
            animationFilter.animateZ();
            animationFilter.hasDelays();
            AnimationFilter animationFilter2 = new AnimationFilter();
            animationFilter2.animateHeight();
            animationFilter2.animateTopInset();
            animationFilter2.animateY();
            animationFilter2.animateZ();
            animationFilter2.hasDelays();
            AnimationFilter animationFilter3 = new AnimationFilter();
            animationFilter3.animateHeight();
            animationFilter3.animateTopInset();
            animationFilter3.animateY();
            animationFilter3.animateZ();
            animationFilter3.hasDelays();
            AnimationFilter animationFilter4 = new AnimationFilter();
            animationFilter4.animateHeight();
            animationFilter4.animateTopInset();
            animationFilter4.animateY();
            animationFilter4.animateDimmed();
            animationFilter4.animateZ();
            AnimationFilter animationFilter5 = new AnimationFilter();
            animationFilter5.animateZ();
            AnimationFilter animationFilter6 = new AnimationFilter();
            animationFilter6.animateDimmed();
            AnimationFilter animationFilter7 = new AnimationFilter();
            animationFilter7.animateAlpha();
            animationFilter7.animateHeight();
            animationFilter7.animateTopInset();
            animationFilter7.animateY();
            animationFilter7.animateZ();
            AnimationFilter animationFilter8 = new AnimationFilter();
            animationFilter8.animateHeight();
            animationFilter8.animateTopInset();
            animationFilter8.animateY();
            animationFilter8.animateDimmed();
            animationFilter8.animateZ();
            animationFilter8.hasDelays();
            AnimationFilter animationFilter9 = new AnimationFilter();
            animationFilter9.animateHideSensitive();
            AnimationFilter animationFilter10 = new AnimationFilter();
            animationFilter10.animateHeight();
            animationFilter10.animateTopInset();
            animationFilter10.animateY();
            animationFilter10.animateZ();
            AnimationFilter animationFilter11 = new AnimationFilter();
            animationFilter11.animateAlpha();
            animationFilter11.animateHeight();
            animationFilter11.animateTopInset();
            animationFilter11.animateY();
            animationFilter11.animateZ();
            AnimationFilter animationFilter12 = new AnimationFilter();
            animationFilter12.animateHeight();
            animationFilter12.animateTopInset();
            animationFilter12.animateY();
            animationFilter12.animateZ();
            AnimationFilter animationFilter13 = new AnimationFilter();
            animationFilter13.animateHeight();
            animationFilter13.animateTopInset();
            animationFilter13.animateY();
            animationFilter13.animateZ();
            animationFilter13.hasDelays();
            AnimationFilter animationFilter14 = new AnimationFilter();
            animationFilter14.animateHeight();
            animationFilter14.animateTopInset();
            animationFilter14.animateY();
            animationFilter14.animateZ();
            animationFilter14.hasDelays();
            AnimationFilter animationFilter15 = new AnimationFilter();
            animationFilter15.animateHeight();
            animationFilter15.animateTopInset();
            animationFilter15.animateY();
            animationFilter15.animateZ();
            AnimationFilter animationFilter16 = new AnimationFilter();
            animationFilter16.animateAlpha();
            animationFilter16.animateDark();
            animationFilter16.animateDimmed();
            animationFilter16.animateHideSensitive();
            animationFilter16.animateHeight();
            animationFilter16.animateTopInset();
            animationFilter16.animateY();
            animationFilter16.animateZ();
            FILTERS = new AnimationFilter[]{animationFilter, animationFilter2, animationFilter3, animationFilter4, animationFilter5, animationFilter6, animationFilter7, null, animationFilter8, animationFilter9, animationFilter10, animationFilter11, animationFilter12, animationFilter13, animationFilter14, animationFilter15, animationFilter16};
        }

        AnimationEvent(ExpandableView expandableView, int i) {
            this(expandableView, i, (long) LENGTHS[i]);
        }

        AnimationEvent(ExpandableView expandableView, int i, AnimationFilter animationFilter) {
            this(expandableView, i, (long) LENGTHS[i], animationFilter);
        }

        AnimationEvent(ExpandableView expandableView, int i, long j) {
            this(expandableView, i, j, FILTERS[i]);
        }

        AnimationEvent(ExpandableView expandableView, int i, long j, AnimationFilter animationFilter) {
            this.eventStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.mChangingView = expandableView;
            this.animationType = i;
            this.length = j;
            this.filter = animationFilter;
        }

        static long combineLength(ArrayList<AnimationEvent> arrayList) {
            int size = arrayList.size();
            long j = 0;
            for (int i = 0; i < size; i++) {
                AnimationEvent animationEvent = (AnimationEvent) arrayList.get(i);
                j = Math.max(j, animationEvent.length);
                if (animationEvent.animationType == 8) {
                    return animationEvent.length;
                }
            }
            return j;
        }
    }

    public interface OnEmptySpaceClickListener {
        void onEmptySpaceClicked(float f, float f2);
    }

    public interface OnOverscrollTopChangedListener {
        void flingTopOverscroll(float f, boolean z);

        void onOverscrollTopChanged(float f, boolean z);
    }

    public View getHostView() {
        return this;
    }

    public ViewGroup getViewParentForNotification(NotificationEntry notificationEntry) {
        return this;
    }

    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public /* synthetic */ boolean lambda$new$0$NotificationStackScrollLayout() {
        updateBackground();
        return true;
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attributeSet, boolean z, NotificationRoundnessManager notificationRoundnessManager, AmbientPulseManager ambientPulseManager, DynamicPrivacyController dynamicPrivacyController, ConfigurationController configurationController, ActivityStarter activityStarter, StatusBarStateController statusBarStateController) {
        Context context2 = context;
        AttributeSet attributeSet2 = attributeSet;
        super(context, attributeSet, 0, 0);
        Resources resources = getResources();
        this.mAllowLongPress = z;
        for (int i = 0; i < 2; i++) {
            this.mSections[i] = new NotificationSection(this);
        }
        this.mAmbientPulseManager = ambientPulseManager;
        NotificationSectionsManager notificationSectionsManager = new NotificationSectionsManager(this, activityStarter, statusBarStateController, configurationController, NotificationUtils.useNewInterruptionModel(context));
        this.mSectionsManager = notificationSectionsManager;
        this.mSectionsManager.initialize(LayoutInflater.from(context));
        this.mSectionsManager.setOnClearGentleNotifsClickListener(new OnClickListener() {
            public final void onClick(View view) {
                NotificationStackScrollLayout.this.lambda$new$2$NotificationStackScrollLayout(view);
            }
        });
        this.mAmbientState = new AmbientState(context, this.mSectionsManager);
        this.mRoundnessManager = notificationRoundnessManager;
        this.mBgColor = context.getColor(R$color.notification_shade_background_color);
        this.mExpandHelper = new ExpandHelper(getContext(), this.mExpandHelperCallback, resources.getDimensionPixelSize(R$dimen.notification_min_height), resources.getDimensionPixelSize(R$dimen.notification_max_height));
        this.mExpandHelper.setEventSource(this);
        this.mExpandHelper.setScrollAdapter(this);
        this.mSwipeHelper = new NotificationSwipeHelper(0, this.mNotificationCallback, getContext(), this.mMenuEventListener);
        this.mStackScrollAlgorithm = createStackScrollAlgorithm(context);
        initView(context);
        this.mFalsingManager = FalsingManagerFactory.getInstance(context);
        this.mShouldDrawNotificationBackground = resources.getBoolean(R$bool.config_drawNotificationBackground);
        this.mFadeNotificationsOnDismiss = resources.getBoolean(R$bool.config_fadeNotificationsOnDismiss);
        this.mRoundnessManager.setAnimatedChildren(this.mChildrenToAddAnimated);
        this.mRoundnessManager.setOnRoundingChangedCallback(new Runnable() {
            public final void run() {
                NotificationStackScrollLayout.this.invalidate();
            }
        });
        NotificationRoundnessManager notificationRoundnessManager2 = this.mRoundnessManager;
        Objects.requireNonNull(notificationRoundnessManager2);
        addOnExpandedHeightListener(new BiConsumer() {
            public final void accept(Object obj, Object obj2) {
                NotificationRoundnessManager.this.setExpanded(((Float) obj).floatValue(), ((Float) obj2).floatValue());
            }
        });
        setOutlineProvider(this.mOutlineProvider);
        addOnExpandedHeightListener(new BiConsumer() {
            public final void accept(Object obj, Object obj2) {
                NotificationBlockingHelperManager.this.setNotificationShadeExpanded(((Float) obj).floatValue());
            }
        });
        updateWillNotDraw();
        this.mBackgroundPaint.setAntiAlias(true);
        this.mClearAllEnabled = resources.getBoolean(R$bool.config_enableNotificationsClearAll);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(new Tunable() {
            public final void onTuningChanged(String str, String str2) {
                NotificationStackScrollLayout.this.lambda$new$4$NotificationStackScrollLayout(str, str2);
            }
        }, "high_priority", "notification_dismiss_rtl");
        this.mEntryManager.addNotificationEntryListener(new NotificationEntryListener() {
            public void onPostEntryUpdated(NotificationEntry notificationEntry) {
                if (!notificationEntry.notification.isClearable()) {
                    NotificationStackScrollLayout.this.snapViewIfNeeded(notificationEntry);
                }
            }
        });
        dynamicPrivacyController.addListener(this);
        if (DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("init NotificationStackScrollLayout, mBgColor= ");
            sb.append(this.mBgColor);
            Log.d("StackScroller", sb.toString());
        }
    }

    public /* synthetic */ void lambda$new$2$NotificationStackScrollLayout(View view) {
        clearNotifications(2, true ^ hasActiveClearableNotifications(1));
    }

    public /* synthetic */ void lambda$new$4$NotificationStackScrollLayout(String str, String str2) {
        String str3 = "1";
        if (str.equals("high_priority")) {
            this.mHighPriorityBeforeSpeedBump = str3.equals(str2);
        } else if (str.equals("notification_dismiss_rtl")) {
            updateDismissRtlSetting(str3.equals(str2));
        }
    }

    private void updateDismissRtlSetting(boolean z) {
        this.mDismissRtl = z;
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) childAt).setDismissRtl(z);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        inflateEmptyShadeView();
        inflateFooterView();
        this.mVisualStabilityManager.setVisibilityLocationProvider(new VisibilityLocationProvider() {
            public final boolean isInVisibleLocation(NotificationEntry notificationEntry) {
                return NotificationStackScrollLayout.this.isInVisibleLocation(notificationEntry);
            }
        });
        if (this.mAllowLongPress) {
            NotificationGutsManager notificationGutsManager = this.mNotificationGutsManager;
            Objects.requireNonNull(notificationGutsManager);
            setLongPressListener(new LongPressListener() {
                public final boolean onLongPress(View view, int i, int i2, MenuItem menuItem) {
                    return NotificationGutsManager.this.openGuts(view, i, i2, menuItem);
                }
            });
        }
    }

    public float getPulseHeight() {
        ActivatableNotificationView firstChildWithBackground = getFirstChildWithBackground();
        if (firstChildWithBackground != null) {
            return (float) firstChildWithBackground.getCollapsedHeight();
        }
        return 0.0f;
    }

    public void onDensityOrFontScaleChanged() {
        reinflateViews();
        updateDismissAllButton();
    }

    private void reinflateViews() {
        inflateFooterView();
        inflateEmptyShadeView();
        updateFooter();
        updateEmptyShadeView(!this.mQsExpanded);
        this.mSectionsManager.reinflateViews(LayoutInflater.from(this.mContext));
    }

    public void onThemeChanged() {
        updateDecorViews(this.mColorExtractor.getNeutralColors().supportsDarkText());
        updateFooter();
    }

    public void onOverlayChanged() {
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(Utils.getThemeAttr(this.mContext, 16844145));
        if (this.mCornerRadius != dimensionPixelSize) {
            this.mCornerRadius = dimensionPixelSize;
            invalidate();
        }
        reinflateViews();
    }

    @VisibleForTesting
    public void updateFooter() {
        boolean z = this.mClearAllEnabled && hasActiveClearableNotifications(0) && this.mNotificationPanel.isFullyExpanded();
        updateFooterView((z || this.mEntryManager.getNotificationData().getActiveNotifications().size() != 0) && this.mStatusBarState != 1 && !this.mRemoteInputManager.getController().isRemoteInputActive(), false);
        if (z) {
            showDismissAnimate(true);
            return;
        }
        if (DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateFooter hideDismissAnimate mStatusBarState=");
            sb.append(this.mStatusBarState);
            sb.append(" hasActiveClearableNotifications=");
            sb.append(hasActiveClearableNotifications(0));
            sb.append(" isFullyExpanded=");
            sb.append(this.mNotificationPanel.isFullyExpanded());
            Log.d("StackScroller", sb.toString());
        }
        hideDismissAnimate(true);
    }

    private void updateDismissAllButton() {
        if (this.mDismissAllButton != null) {
            Resources resources = this.mContext.getResources();
            LayoutParams layoutParams = (LayoutParams) this.mDismissAllButton.getLayoutParams();
            layoutParams.width = resources.getDimensionPixelSize(R$dimen.dismiss_all_button_width);
            layoutParams.height = resources.getDimensionPixelSize(R$dimen.dismiss_all_button_height);
            layoutParams.bottomMargin = resources.getDimensionPixelSize(R$dimen.dismiss_all_button_margin_bottom);
            this.mDismissAllButton.setElevation(resources.getDimension(R$dimen.dismiss_all_button_elevation));
            ((ImageView) this.mDismissAllButton).setImageResource(R$drawable.recents_dismiss_all_icon);
        }
    }

    public void showDismissAnimate(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("showDismissAnimate ");
        sb.append(Debug.getCallers(5));
        Log.d("StackScroller", sb.toString());
        if (this.mStatusBarState != 1 && !this.mNotificationPanel.isQsExpanded() && !this.mNotificationPanel.isClosingWithAlphaFadeOut() && (!OpUtils.isCustomFingerprint() || !KeyguardUpdateMonitor.getInstance(this.mContext).shouldHideDismissButton())) {
            if (!z) {
                this.mDismissShow = true;
                this.mDismissAllButton.clearAnimation();
                this.mDismissAllButton.setVisibility(0);
            } else if (!this.mDismissShow) {
                this.mDismissShow = true;
                Animation loadAnimation = AnimationUtils.loadAnimation(this.mContext, R$anim.dismiss_all_show);
                this.mDismissAllButton.setVisibility(0);
                this.mDismissAllButton.startAnimation(loadAnimation);
            }
        }
    }

    public void hideDismissAnimate(boolean z) {
        if (!z) {
            this.mDismissShow = false;
            this.mDismissAllButton.clearAnimation();
            this.mDismissAllButton.setVisibility(8);
        } else if (this.mDismissShow) {
            this.mDismissShow = false;
            Animation loadAnimation = AnimationUtils.loadAnimation(this.mContext, R$anim.dismiss_all_hide);
            loadAnimation.setAnimationListener(new AnimationListener() {
                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    NotificationStackScrollLayout.this.mDismissAllButton.setVisibility(8);
                }
            });
            this.mDismissAllButton.startAnimation(loadAnimation);
        }
    }

    public boolean hasActiveClearableNotifications(int i) {
        int childCount = getChildCount();
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = getChildAt(i2);
            if (childAt instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) childAt;
                if (expandableNotificationRow.canViewBeDismissed() && matchesSelection(expandableNotificationRow, i)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Delegate createDelegate() {
        return new Delegate() {
            public void setRemoteInputActive(NotificationEntry notificationEntry, boolean z) {
                NotificationStackScrollLayout.this.mHeadsUpManager.setRemoteInputActive(notificationEntry, z);
                notificationEntry.notifyHeightChanged(true);
                NotificationStackScrollLayout.this.updateFooter();
            }

            public void lockScrollTo(NotificationEntry notificationEntry) {
                NotificationStackScrollLayout.this.lockScrollTo(notificationEntry.getRow());
            }

            public void requestDisallowLongPressAndDismiss() {
                NotificationStackScrollLayout.this.requestDisallowLongPress();
                NotificationStackScrollLayout.this.requestDisallowDismiss();
            }
        };
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this.mStateListener, 2);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        Log.d("StackScroller", "onAttachedToWindow, register StackScroller to ConfigurationController");
        onUiModeChanged();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).removeCallback(this.mStateListener);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    public NotificationSwipeActionHelper getSwipeActionHelper() {
        return this.mSwipeHelper;
    }

    public void onUiModeChanged() {
        Configuration configuration = getResources().getConfiguration();
        if (configuration != null) {
            boolean z = (configuration.uiMode & 48) == 32;
            StringBuilder sb = new StringBuilder();
            sb.append("opOnUiModeChanged, current theme is in night mode: ");
            sb.append(z);
            sb.append(", mBgColor: ");
            sb.append(this.mBgColor);
            sb.append(", mCachedBackgroundColor: ");
            sb.append(this.mCachedBackgroundColor);
            Log.d("StackScroller", sb.toString());
        }
        this.mBgColor = this.mContext.getColor(R$color.notification_shade_background_color);
        updateBackgroundDimming();
        this.mShelf.onUiModeChanged();
        this.mSectionsManager.onUiModeChanged();
        updateDismissAllButton();
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        if (this.mShouldDrawNotificationBackground && (this.mSections[0].getCurrentBounds().top < this.mSections[1].getCurrentBounds().bottom || this.mAmbientState.isDark())) {
            drawBackground(canvas);
        } else if (this.mInHeadsUpPinnedMode || this.mHeadsUpAnimatingAway) {
            drawHeadsUpBackground(canvas);
        }
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    private void drawBackground(Canvas canvas) {
        int i = this.mSidePaddings;
        int width = getWidth() - this.mSidePaddings;
        boolean z = false;
        int i2 = this.mSections[0].getCurrentBounds().top;
        int i3 = this.mSections[1].getCurrentBounds().bottom;
        int width2 = getWidth() / 2;
        int i4 = this.mTopPadding;
        float f = 1.0f - this.mInterpolatedDarkAmount;
        float interpolation = this.mDarkXInterpolator.getInterpolation((1.0f - this.mLinearDarkAmount) * this.mBackgroundXFactor);
        float f2 = (float) width2;
        int lerp = (int) MathUtils.lerp(f2, (float) i, interpolation);
        int lerp2 = (int) MathUtils.lerp(f2, (float) width, interpolation);
        float f3 = (float) i4;
        int lerp3 = (int) MathUtils.lerp(f3, (float) i2, f);
        this.mBackgroundAnimationRect.set(lerp, lerp3, lerp2, (int) MathUtils.lerp(f3, (float) i3, f));
        int i5 = lerp3 - i2;
        NotificationSection[] notificationSectionArr = this.mSections;
        int length = notificationSectionArr.length;
        int i6 = 0;
        while (true) {
            if (i6 >= length) {
                break;
            } else if (notificationSectionArr[i6].getFirstVisibleChild() != null) {
                z = true;
                break;
            } else {
                i6++;
            }
        }
        if (!this.mAmbientState.isDark() || z) {
            drawBackgroundRects(canvas, lerp, lerp2, lerp3, i5);
        }
        updateClipping();
    }

    private void drawBackgroundRects(Canvas canvas, int i, int i2, int i3, int i4) {
        int i5 = i2;
        int i6 = this.mSections[0].getCurrentBounds().bottom + i4;
        NotificationSection[] notificationSectionArr = this.mSections;
        int length = notificationSectionArr.length;
        int i7 = 1;
        int i8 = i;
        int i9 = i3;
        int i10 = i5;
        int i11 = i6;
        int i12 = 0;
        boolean z = true;
        while (i12 < length) {
            NotificationSection notificationSection = notificationSectionArr[i12];
            if (notificationSection.getFirstVisibleChild() == null) {
                int i13 = i;
            } else {
                int i14 = notificationSection.getCurrentBounds().top + i4;
                int min = Math.min(Math.max(i, notificationSection.getCurrentBounds().left), i5);
                int max = Math.max(Math.min(i5, notificationSection.getCurrentBounds().right), min);
                if (i14 - i11 > i7 || (!(i8 == min && i10 == max) && !z)) {
                    float f = (float) i8;
                    float f2 = (float) i9;
                    float f3 = (float) i10;
                    float f4 = (float) i11;
                    int i15 = this.mCornerRadius;
                    canvas.drawRoundRect(f, f2, f3, f4, (float) i15, (float) i15, this.mBackgroundPaint);
                } else {
                    i14 = i9;
                }
                i11 = notificationSection.getCurrentBounds().bottom + i4;
                i10 = max;
                i9 = i14;
                i8 = min;
                z = false;
            }
            i12++;
            i5 = i2;
            i7 = 1;
        }
        float f5 = (float) i8;
        float f6 = (float) i9;
        float f7 = (float) i10;
        float f8 = (float) i11;
        int i16 = this.mCornerRadius;
        canvas.drawRoundRect(f5, f6, f7, f8, (float) i16, (float) i16, this.mBackgroundPaint);
    }

    private void drawHeadsUpBackground(Canvas canvas) {
        int i = this.mSidePaddings;
        int width = getWidth() - this.mSidePaddings;
        float height = (float) getHeight();
        int childCount = getChildCount();
        float f = height;
        float f2 = 0.0f;
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = getChildAt(i2);
            if (childAt.getVisibility() != 8 && (childAt instanceof ExpandableNotificationRow)) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) childAt;
                if ((expandableNotificationRow.isPinned() || expandableNotificationRow.isHeadsUpAnimatingAway()) && expandableNotificationRow.getTranslation() < 0.0f && expandableNotificationRow.getProvider().shouldShowGutsOnSnapOpen()) {
                    float min = Math.min(f, expandableNotificationRow.getTranslationY());
                    f2 = Math.max(f2, expandableNotificationRow.getTranslationY() + ((float) expandableNotificationRow.getActualHeight()));
                    f = min;
                }
            }
        }
        if (f < f2) {
            float f3 = (float) i;
            float f4 = (float) width;
            int i3 = this.mCornerRadius;
            canvas.drawRoundRect(f3, f, f4, f2, (float) i3, (float) i3, this.mBackgroundPaint);
        }
    }

    /* access modifiers changed from: private */
    public void updateBackgroundDimming() {
        String str = "StackScroller";
        if (!this.mShouldDrawNotificationBackground) {
            Log.d(str, "updateBackgroundDimming, should not draw notification background!");
            return;
        }
        int blendARGB = ColorUtils.blendARGB(this.mBgColor, -1, MathUtils.smoothStep(0.4f, 1.0f, this.mLinearDarkAmount));
        if (this.mCachedBackgroundColor != blendARGB) {
            this.mCachedBackgroundColor = blendARGB;
            this.mBackgroundPaint.setColor(blendARGB);
            invalidate();
            StringBuilder sb = new StringBuilder();
            sb.append("updateBackgroundDimming, set notification background color: ");
            sb.append(blendARGB);
            Log.d(str, sb.toString());
        }
    }

    private void initView(Context context) {
        this.mScroller = new OverScroller(getContext());
        setDescendantFocusability(262144);
        setClipChildren(false);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mOverflingDistance = viewConfiguration.getScaledOverflingDistance();
        Resources resources = context.getResources();
        this.mCollapsedSize = resources.getDimensionPixelSize(R$dimen.notification_min_height);
        this.mStackScrollAlgorithm.initView(context);
        this.mAmbientState.reload(context);
        this.mPaddingBetweenElements = Math.max(1, resources.getDimensionPixelSize(R$dimen.notification_divider_height));
        this.mIncreasedPaddingBetweenElements = resources.getDimensionPixelSize(R$dimen.notification_divider_height_increased);
        this.mMinTopOverScrollToEscape = (float) resources.getDimensionPixelSize(R$dimen.min_top_overscroll_to_qs);
        this.mStatusBarHeight = resources.getDimensionPixelSize(R$dimen.status_bar_height);
        this.mBottomMargin = resources.getDimensionPixelSize(R$dimen.notification_panel_margin_bottom);
        this.mSidePaddings = resources.getDimensionPixelSize(R$dimen.notification_side_paddings);
        this.mMinInteractionHeight = resources.getDimensionPixelSize(R$dimen.notification_min_interaction_height);
        this.mCornerRadius = resources.getDimensionPixelSize(Utils.getThemeAttr(this.mContext, 16844145));
        this.mHeadsUpInset = this.mStatusBarHeight + resources.getDimensionPixelSize(R$dimen.heads_up_status_bar_padding);
    }

    /* access modifiers changed from: private */
    public void notifyHeightChangeListener(ExpandableView expandableView) {
        notifyHeightChangeListener(expandableView, false);
    }

    private void notifyHeightChangeListener(ExpandableView expandableView, boolean z) {
        OnHeightChangedListener onHeightChangedListener = this.mOnHeightChangedListener;
        if (onHeightChangedListener != null) {
            onHeightChangedListener.onHeightChanged(expandableView, z);
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int makeMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(i) - (this.mSidePaddings * 2), MeasureSpec.getMode(i));
        int makeMeasureSpec2 = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(i2), 0);
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            measureChild(getChildAt(i3), makeMeasureSpec, makeMeasureSpec2);
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        float width = ((float) getWidth()) / 2.0f;
        for (int i5 = 0; i5 < getChildCount(); i5++) {
            View childAt = getChildAt(i5);
            float measuredWidth = ((float) childAt.getMeasuredWidth()) / 2.0f;
            childAt.layout((int) (width - measuredWidth), 0, (int) (measuredWidth + width), (int) ((float) childAt.getMeasuredHeight()));
        }
        setMaxLayoutHeight(getHeight());
        updateContentHeight();
        clampScrollPosition();
        requestChildrenUpdate();
        updateFirstAndLastBackgroundViews();
        updateAlgorithmLayoutMinHeight();
    }

    private void requestAnimationOnViewResize(ExpandableNotificationRow expandableNotificationRow) {
        if (!this.mAnimationsEnabled) {
            return;
        }
        if (this.mIsExpanded || (expandableNotificationRow != null && expandableNotificationRow.isPinned())) {
            this.mNeedViewResizeAnimation = true;
            this.mNeedsAnimation = true;
        }
    }

    public void updateSpeedBumpIndex(int i, boolean z) {
        this.mAmbientState.setSpeedBumpIndex(i);
        this.mNoAmbient = z;
    }

    public void setChildLocationsChangedListener(OnChildLocationsChangedListener onChildLocationsChangedListener) {
        this.mListener = onChildLocationsChangedListener;
    }

    public boolean isInVisibleLocation(NotificationEntry notificationEntry) {
        ExpandableNotificationRow row = notificationEntry.getRow();
        ExpandableViewState viewState = row.getViewState();
        if (viewState == null || (viewState.location & 5) == 0 || row.getVisibility() != 0) {
            return false;
        }
        return true;
    }

    private void setMaxLayoutHeight(int i) {
        this.mMaxLayoutHeight = i;
        this.mShelf.setMaxLayoutHeight(i);
        updateAlgorithmHeightAndPadding();
    }

    private void updateAlgorithmHeightAndPadding() {
        this.mAmbientState.setLayoutHeight(getLayoutHeight());
        updateAlgorithmLayoutMinHeight();
        this.mAmbientState.setTopPadding(this.mTopPadding);
    }

    private void updateAlgorithmLayoutMinHeight() {
        int i;
        AmbientState ambientState = this.mAmbientState;
        if (this.mQsExpanded || isHeadsUpTransition()) {
            i = getLayoutMinHeight();
        } else {
            i = 0;
        }
        ambientState.setLayoutMinHeight(i);
    }

    /* access modifiers changed from: private */
    public void updateChildren() {
        float f;
        updateScrollStateForAddedChildren();
        AmbientState ambientState = this.mAmbientState;
        if (this.mScroller.isFinished()) {
            f = 0.0f;
        } else {
            f = this.mScroller.getCurrVelocity();
        }
        ambientState.setCurrentScrollVelocity(f);
        this.mAmbientState.setScrollY(this.mOwnScrollY);
        this.mStackScrollAlgorithm.resetViewStates(this.mAmbientState);
        if (isCurrentlyAnimating() || this.mNeedsAnimation) {
            startAnimationToState();
        } else {
            applyCurrentState();
        }
    }

    /* access modifiers changed from: private */
    public void onPreDrawDuringAnimation() {
        this.mShelf.updateAppearance();
        updateClippingToTopRoundedCorner();
        if (!this.mNeedsAnimation && !this.mChildrenUpdateRequested) {
            updateBackground();
        }
    }

    private void updateClippingToTopRoundedCorner() {
        Float valueOf = Float.valueOf(((float) this.mTopPadding) + this.mStackTranslation + ((float) this.mAmbientState.getExpandAnimationTopChange()));
        Float valueOf2 = Float.valueOf(valueOf.floatValue() + ((float) this.mCornerRadius));
        boolean z = true;
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() != 8) {
                float translationY = expandableView.getTranslationY();
                float actualHeight = ((float) expandableView.getActualHeight()) + translationY;
                expandableView.setDistanceToTopRoundness((!z || !isScrolledToTop()) & (((valueOf.floatValue() > translationY ? 1 : (valueOf.floatValue() == translationY ? 0 : -1)) > 0 && (valueOf.floatValue() > actualHeight ? 1 : (valueOf.floatValue() == actualHeight ? 0 : -1)) < 0) || ((valueOf2.floatValue() > translationY ? 1 : (valueOf2.floatValue() == translationY ? 0 : -1)) >= 0 && (valueOf2.floatValue() > actualHeight ? 1 : (valueOf2.floatValue() == actualHeight ? 0 : -1)) <= 0)) ? Math.max(translationY - valueOf.floatValue(), 0.0f) : -1.0f);
                z = false;
            }
        }
    }

    private void updateScrollStateForAddedChildren() {
        if (!this.mChildrenToAddAnimated.isEmpty()) {
            for (int i = 0; i < getChildCount(); i++) {
                ExpandableView expandableView = (ExpandableView) getChildAt(i);
                if (this.mChildrenToAddAnimated.contains(expandableView)) {
                    int positionInLinearLayout = getPositionInLinearLayout(expandableView);
                    float increasedPaddingAmount = expandableView.getIncreasedPaddingAmount();
                    int i2 = increasedPaddingAmount == 1.0f ? this.mIncreasedPaddingBetweenElements : increasedPaddingAmount == -1.0f ? 0 : this.mPaddingBetweenElements;
                    int intrinsicHeight = getIntrinsicHeight(expandableView) + i2;
                    int i3 = this.mOwnScrollY;
                    if (positionInLinearLayout < i3) {
                        setOwnScrollY(i3 + intrinsicHeight);
                    }
                }
            }
            clampScrollPosition();
        }
    }

    /* access modifiers changed from: private */
    public void updateForcedScroll() {
        View view = this.mForcedScroll;
        if (view != null && (!view.hasFocus() || !this.mForcedScroll.isAttachedToWindow())) {
            this.mForcedScroll = null;
        }
        View view2 = this.mForcedScroll;
        if (view2 != null) {
            ExpandableView expandableView = (ExpandableView) view2;
            int positionInLinearLayout = getPositionInLinearLayout(expandableView);
            int targetScrollForView = targetScrollForView(expandableView, positionInLinearLayout);
            int intrinsicHeight = positionInLinearLayout + expandableView.getIntrinsicHeight();
            int max = Math.max(0, Math.min(targetScrollForView, getScrollRange()));
            int i = this.mOwnScrollY;
            if (i < max || intrinsicHeight < i) {
                setOwnScrollY(max);
            }
        }
    }

    /* access modifiers changed from: private */
    public void requestChildrenUpdate() {
        if (!this.mChildrenUpdateRequested) {
            getViewTreeObserver().addOnPreDrawListener(this.mChildrenUpdater);
            this.mChildrenUpdateRequested = true;
            invalidate();
        }
    }

    public int getVisibleNotificationCount() {
        int i = 0;
        for (int i2 = 0; i2 < getChildCount(); i2++) {
            View childAt = getChildAt(i2);
            if (childAt.getVisibility() != 8 && (childAt instanceof ExpandableNotificationRow)) {
                i++;
            }
        }
        return i;
    }

    private boolean isCurrentlyAnimating() {
        return this.mStateAnimator.isRunning();
    }

    private void clampScrollPosition() {
        int scrollRange = getScrollRange();
        if (scrollRange < this.mOwnScrollY) {
            setOwnScrollY(scrollRange);
        }
    }

    public int getTopPadding() {
        return this.mTopPadding;
    }

    private void setTopPadding(int i, boolean z) {
        if (this.mTopPadding != i) {
            this.mTopPadding = i;
            updateAlgorithmHeightAndPadding();
            updateContentHeight();
            if (z && this.mAnimationsEnabled && this.mIsExpanded) {
                this.mTopPaddingNeedsAnimation = true;
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
            notifyHeightChangeListener(null, z);
        }
    }

    public void setExpandedHeight(float f) {
        int i;
        float f2;
        this.mExpandedHeight = f;
        float f3 = 0.0f;
        boolean z = true;
        setIsExpanded(f > 0.0f);
        float minExpansionHeight = (float) getMinExpansionHeight();
        if (f < minExpansionHeight) {
            Rect rect = this.mClipRect;
            rect.left = 0;
            rect.right = getWidth();
            Rect rect2 = this.mClipRect;
            rect2.top = 0;
            rect2.bottom = (int) f;
            setRequestedClipBounds(rect2);
            f = minExpansionHeight;
        } else {
            setRequestedClipBounds(null);
        }
        float appearEndPosition = getAppearEndPosition();
        float appearStartPosition = getAppearStartPosition();
        float f4 = 1.0f;
        if (f >= appearEndPosition) {
            z = false;
        }
        this.mAmbientState.setAppearing(z);
        if (z) {
            f4 = getAppearFraction(f);
            if (f4 >= 0.0f) {
                f2 = NotificationUtils.interpolate(getExpandTranslationStart(), 0.0f, f4);
            } else {
                f2 = (f - appearStartPosition) + getExpandTranslationStart();
            }
            if (isHeadsUpTransition()) {
                i = getFirstVisibleSection().getFirstVisibleChild().getPinnedHeadsUpHeight();
                f3 = MathUtils.lerp((float) (this.mHeadsUpInset - this.mTopPadding), 0.0f, f4);
            } else {
                i = (int) (f - f2);
                f3 = f2;
            }
        } else if (this.mShouldShowShelfOnly) {
            i = this.mTopPadding + this.mShelf.getIntrinsicHeight();
        } else {
            if (this.mQsExpanded) {
                int i2 = (this.mContentHeight - this.mTopPadding) + this.mIntrinsicPadding;
                int intrinsicHeight = this.mMaxTopPadding + this.mShelf.getIntrinsicHeight();
                if (i2 <= intrinsicHeight) {
                    i = intrinsicHeight;
                } else {
                    f = NotificationUtils.interpolate((float) i2, (float) intrinsicHeight, this.mQsExpansionFraction);
                }
            }
            i = (int) f;
        }
        if (i != this.mCurrentStackHeight) {
            this.mCurrentStackHeight = i;
            updateAlgorithmHeightAndPadding();
            requestChildrenUpdate();
        }
        setStackTranslation(f3);
        for (int i3 = 0; i3 < this.mExpandedHeightListeners.size(); i3++) {
            ((BiConsumer) this.mExpandedHeightListeners.get(i3)).accept(Float.valueOf(this.mExpandedHeight), Float.valueOf(f4));
        }
    }

    private void setRequestedClipBounds(Rect rect) {
        this.mRequestedClipBounds = rect;
        updateClipping();
    }

    public int getIntrinsicContentHeight() {
        return this.mIntrinsicContentHeight;
    }

    public void updateClipping() {
        boolean z = true;
        boolean z2 = this.mRequestedClipBounds != null && !this.mInHeadsUpPinnedMode && !this.mHeadsUpAnimatingAway;
        if (this.mIsClipped != z2) {
            this.mIsClipped = z2;
        }
        if (!this.mPulsing && this.mAmbientState.isFullyDark()) {
            setClipBounds(null);
        } else if (this.mAmbientState.isDarkAtAll()) {
            invalidateOutline();
            setClipToOutline(z);
        } else if (z2) {
            setClipBounds(this.mRequestedClipBounds);
        } else {
            setClipBounds(null);
        }
        z = false;
        setClipToOutline(z);
    }

    private float getExpandTranslationStart() {
        return (float) (((-this.mTopPadding) + getMinExpansionHeight()) - this.mShelf.getIntrinsicHeight());
    }

    private float getAppearStartPosition() {
        if (isHeadsUpTransition()) {
            return (float) (this.mHeadsUpInset + getFirstVisibleSection().getFirstVisibleChild().getPinnedHeadsUpHeight());
        }
        return (float) getMinExpansionHeight();
    }

    private int getTopHeadsUpPinnedHeight() {
        NotificationEntry topEntry = this.mHeadsUpManager.getTopEntry();
        if (topEntry == null) {
            return 0;
        }
        ExpandableNotificationRow row = topEntry.getRow();
        if (row.isChildInGroup()) {
            NotificationEntry groupSummary = this.mGroupManager.getGroupSummary(row.getStatusBarNotification());
            if (groupSummary != null) {
                row = groupSummary.getRow();
            }
        }
        return row.getPinnedHeadsUpHeight();
    }

    private float getAppearEndPosition() {
        int i;
        int notGoneChildCount = getNotGoneChildCount();
        if (this.mEmptyShadeView.getVisibility() != 8 || notGoneChildCount == 0) {
            i = this.mEmptyShadeView.getHeight();
        } else if (isHeadsUpTransition() || (this.mHeadsUpManager.hasPinnedHeadsUp() && !this.mAmbientState.isDark())) {
            i = getTopHeadsUpPinnedHeight();
        } else {
            i = 0;
            if (notGoneChildCount >= 1 && this.mShelf.getVisibility() != 8) {
                i = 0 + this.mShelf.getIntrinsicHeight();
            }
        }
        return (float) (i + (onKeyguard() ? this.mTopPadding : this.mIntrinsicPadding));
    }

    private boolean isHeadsUpTransition() {
        NotificationSection firstVisibleSection = getFirstVisibleSection();
        return this.mTrackingHeadsUp && firstVisibleSection != null && firstVisibleSection.getFirstVisibleChild().isAboveShelf();
    }

    public float getAppearFraction(float f) {
        float appearEndPosition = getAppearEndPosition();
        float appearStartPosition = getAppearStartPosition();
        return (f - appearStartPosition) / (appearEndPosition - appearStartPosition);
    }

    public float getStackTranslation() {
        return this.mStackTranslation;
    }

    private void setStackTranslation(float f) {
        if (f != this.mStackTranslation) {
            this.mStackTranslation = f;
            this.mAmbientState.setStackTranslation(f);
            requestChildrenUpdate();
        }
    }

    private int getLayoutHeight() {
        return Math.min(this.mMaxLayoutHeight, this.mCurrentStackHeight);
    }

    public int getFirstItemMinHeight() {
        ExpandableView firstChildNotGone = getFirstChildNotGone();
        return firstChildNotGone != null ? firstChildNotGone.getMinHeight() : this.mCollapsedSize;
    }

    public void setQsContainer(ViewGroup viewGroup) {
        this.mQsContainer = viewGroup;
    }

    public static boolean isPinnedHeadsUp(View view) {
        if (!(view instanceof ExpandableNotificationRow)) {
            return false;
        }
        ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
        if (!expandableNotificationRow.isHeadsUp() || !expandableNotificationRow.isPinned()) {
            return false;
        }
        return true;
    }

    private boolean isHeadsUp(View view) {
        if (view instanceof ExpandableNotificationRow) {
            return ((ExpandableNotificationRow) view).isHeadsUp();
        }
        return false;
    }

    public ExpandableView getClosestChildAtRawPosition(float f, float f2) {
        getLocationOnScreen(this.mTempInt2);
        float f3 = f2 - ((float) this.mTempInt2[1]);
        int childCount = getChildCount();
        ExpandableView expandableView = null;
        float f4 = Float.MAX_VALUE;
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView2 = (ExpandableView) getChildAt(i);
            if (expandableView2.getVisibility() != 8 && !(expandableView2 instanceof StackScrollerDecorView)) {
                float translationY = expandableView2.getTranslationY();
                float min = Math.min(Math.abs((((float) expandableView2.getClipTopAmount()) + translationY) - f3), Math.abs(((translationY + ((float) expandableView2.getActualHeight())) - ((float) expandableView2.getClipBottomAmount())) - f3));
                if (min < f4) {
                    expandableView = expandableView2;
                    f4 = min;
                }
            }
        }
        return expandableView;
    }

    /* access modifiers changed from: private */
    public ExpandableView getChildAtPosition(float f, float f2) {
        return getChildAtPosition(f, f2, true);
    }

    private ExpandableView getChildAtPosition(float f, float f2, boolean z) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() == 0 && !(expandableView instanceof StackScrollerDecorView)) {
                float translationY = expandableView.getTranslationY();
                float clipTopAmount = ((float) expandableView.getClipTopAmount()) + translationY;
                float actualHeight = (((float) expandableView.getActualHeight()) + translationY) - ((float) expandableView.getClipBottomAmount());
                int width = getWidth();
                if ((actualHeight - clipTopAmount >= ((float) this.mMinInteractionHeight) || !z) && f2 >= clipTopAmount && f2 <= actualHeight && f >= ((float) 0) && f <= ((float) width)) {
                    if (!(expandableView instanceof ExpandableNotificationRow)) {
                        return expandableView;
                    }
                    ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
                    NotificationEntry entry = expandableNotificationRow.getEntry();
                    if (this.mIsExpanded || !expandableNotificationRow.isHeadsUp() || !expandableNotificationRow.isPinned() || this.mHeadsUpManager.getTopEntry().getRow() == expandableNotificationRow || this.mGroupManager.getGroupSummary(this.mHeadsUpManager.getTopEntry().notification) == entry) {
                        return expandableNotificationRow.getViewAtPosition(f2 - translationY);
                    }
                }
            }
        }
        return null;
    }

    public ExpandableView getChildAtRawPosition(float f, float f2) {
        getLocationOnScreen(this.mTempInt2);
        int[] iArr = this.mTempInt2;
        return getChildAtPosition(f - ((float) iArr[0]), f2 - ((float) iArr[1]));
    }

    public void setScrollingEnabled(boolean z) {
        this.mScrollingEnabled = z;
    }

    public void lockScrollTo(View view) {
        if (this.mForcedScroll != view) {
            this.mForcedScroll = view;
            scrollTo(view);
        }
    }

    public boolean scrollTo(View view) {
        ExpandableView expandableView = (ExpandableView) view;
        int positionInLinearLayout = getPositionInLinearLayout(view);
        int targetScrollForView = targetScrollForView(expandableView, positionInLinearLayout);
        int intrinsicHeight = positionInLinearLayout + expandableView.getIntrinsicHeight();
        int i = this.mOwnScrollY;
        if (i >= targetScrollForView && intrinsicHeight >= i) {
            return false;
        }
        OverScroller overScroller = this.mScroller;
        int i2 = this.mScrollX;
        int i3 = this.mOwnScrollY;
        overScroller.startScroll(i2, i3, 0, targetScrollForView - i3);
        this.mDontReportNextOverScroll = true;
        lambda$new$1$NotificationStackScrollLayout();
        return true;
    }

    private int targetScrollForView(ExpandableView expandableView, int i) {
        return (((i + expandableView.getIntrinsicHeight()) + getImeInset()) - getHeight()) + ((isExpanded() || !isPinnedHeadsUp(expandableView)) ? getTopPadding() : this.mHeadsUpInset);
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        this.mBottomInset = windowInsets.getSystemWindowInsetBottom();
        if (this.mOwnScrollY > getScrollRange()) {
            removeCallbacks(this.mReclamp);
            postDelayed(this.mReclamp, 50);
        } else {
            View view = this.mForcedScroll;
            if (view != null) {
                scrollTo(view);
            }
        }
        return windowInsets;
    }

    public void setExpandingEnabled(boolean z) {
        this.mExpandHelper.setEnabled(z);
    }

    private boolean isScrollingEnabled() {
        return this.mScrollingEnabled;
    }

    /* access modifiers changed from: private */
    public boolean onKeyguard() {
        return this.mStatusBarState == 1;
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mStatusBarHeight = getResources().getDimensionPixelOffset(R$dimen.status_bar_height);
        this.mSwipeHelper.setDensityScale(getResources().getDisplayMetrics().density);
        this.mSwipeHelper.setPagingTouchSlop((float) ViewConfiguration.get(getContext()).getScaledPagingTouchSlop());
        initView(getContext());
    }

    public void dismissViewAnimated(View view, Runnable runnable, int i, long j) {
        this.mSwipeHelper.dismissChild(view, 0.0f, runnable, (long) i, true, j, true);
    }

    /* access modifiers changed from: private */
    public void snapViewIfNeeded(NotificationEntry notificationEntry) {
        ExpandableNotificationRow row = notificationEntry.getRow();
        boolean z = this.mIsExpanded || isPinnedHeadsUp(row);
        if (row.getProvider() != null) {
            this.mSwipeHelper.snapChildIfNeeded(row, z, row.getProvider().isMenuVisible() ? row.getTranslation() : 0.0f);
        }
    }

    private float overScrollUp(int i, int i2) {
        int max = Math.max(i, 0);
        float currentOverScrollAmount = getCurrentOverScrollAmount(true);
        float f = currentOverScrollAmount - ((float) max);
        if (currentOverScrollAmount > 0.0f) {
            setOverScrollAmount(f, true, false);
        }
        float f2 = f < 0.0f ? -f : 0.0f;
        float f3 = ((float) this.mOwnScrollY) + f2;
        float f4 = (float) i2;
        if (f3 <= f4) {
            return f2;
        }
        if (!this.mExpandedInThisMotion) {
            setOverScrolledPixels((getCurrentOverScrolledPixels(false) + f3) - f4, false, false);
        }
        setOwnScrollY(i2);
        return 0.0f;
    }

    private float overScrollDown(int i) {
        int min = Math.min(i, 0);
        float currentOverScrollAmount = getCurrentOverScrollAmount(false);
        float f = ((float) min) + currentOverScrollAmount;
        if (currentOverScrollAmount > 0.0f) {
            setOverScrollAmount(f, false, false);
        }
        if (f >= 0.0f) {
            f = 0.0f;
        }
        float f2 = ((float) this.mOwnScrollY) + f;
        if (f2 >= 0.0f) {
            return f;
        }
        setOverScrolledPixels(getCurrentOverScrolledPixels(true) - f2, true, false);
        setOwnScrollY(0);
        return 0.0f;
    }

    private void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void initOrResetVelocityTracker() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }
    }

    public void setFinishScrollingCallback(Runnable runnable) {
        this.mFinishScrollingCallback = runnable;
    }

    /* access modifiers changed from: private */
    /* renamed from: animateScroll */
    public void lambda$new$1$NotificationStackScrollLayout() {
        if (this.mScroller.computeScrollOffset()) {
            int i = this.mOwnScrollY;
            int currY = this.mScroller.getCurrY();
            if (i != currY) {
                int scrollRange = getScrollRange();
                if ((currY < 0 && i >= 0) || (currY > scrollRange && i <= scrollRange)) {
                    setMaxOverScrollFromCurrentVelocity();
                }
                if (this.mDontClampNextScroll) {
                    scrollRange = Math.max(scrollRange, i);
                }
                customOverScrollBy(currY - i, i, scrollRange, (int) this.mMaxOverScroll);
            }
            postOnAnimation(this.mReflingAndAnimateScroll);
            return;
        }
        this.mDontClampNextScroll = false;
        Runnable runnable = this.mFinishScrollingCallback;
        if (runnable != null) {
            runnable.run();
        }
    }

    private void setMaxOverScrollFromCurrentVelocity() {
        float currVelocity = this.mScroller.getCurrVelocity();
        if (currVelocity >= ((float) this.mMinimumVelocity)) {
            this.mMaxOverScroll = (Math.abs(currVelocity) / 1000.0f) * ((float) this.mOverflingDistance);
        }
    }

    private void customOverScrollBy(int i, int i2, int i3, int i4) {
        int i5 = i2 + i;
        int i6 = -i4;
        int i7 = i3 + i4;
        boolean z = true;
        if (i5 > i7) {
            i6 = i7;
        } else if (i5 >= i6) {
            z = false;
            i6 = i5;
        }
        onCustomOverScrolled(i6, z);
    }

    public void setOverScrolledPixels(float f, boolean z, boolean z2) {
        setOverScrollAmount(f * getRubberBandFactor(z), z, z2, true);
    }

    public void setOverScrollAmount(float f, boolean z, boolean z2) {
        setOverScrollAmount(f, z, z2, true);
    }

    public void setOverScrollAmount(float f, boolean z, boolean z2, boolean z3) {
        setOverScrollAmount(f, z, z2, z3, isRubberbanded(z));
    }

    public void setOverScrollAmount(float f, boolean z, boolean z2, boolean z3, boolean z4) {
        if (z3) {
            this.mStateAnimator.cancelOverScrollAnimators(z);
        }
        setOverScrollAmountInternal(f, z, z2, z4);
    }

    private void setOverScrollAmountInternal(float f, boolean z, boolean z2, boolean z3) {
        float max = Math.max(0.0f, f);
        if (z2) {
            this.mStateAnimator.animateOverScrollToAmount(max, z, z3);
            return;
        }
        setOverScrolledPixels(max / getRubberBandFactor(z), z);
        this.mAmbientState.setOverScrollAmount(max, z);
        if (z) {
            notifyOverscrollTopListener(max, z3);
        }
        requestChildrenUpdate();
    }

    private void notifyOverscrollTopListener(float f, boolean z) {
        this.mExpandHelper.onlyObserveMovements(f > 1.0f);
        if (this.mDontReportNextOverScroll) {
            this.mDontReportNextOverScroll = false;
            return;
        }
        OnOverscrollTopChangedListener onOverscrollTopChangedListener = this.mOverscrollTopChangedListener;
        if (onOverscrollTopChangedListener != null) {
            onOverscrollTopChangedListener.onOverscrollTopChanged(f, z);
        }
    }

    public void setOverscrollTopChangedListener(OnOverscrollTopChangedListener onOverscrollTopChangedListener) {
        this.mOverscrollTopChangedListener = onOverscrollTopChangedListener;
    }

    public float getCurrentOverScrollAmount(boolean z) {
        return this.mAmbientState.getOverScrollAmount(z);
    }

    public float getCurrentOverScrolledPixels(boolean z) {
        return z ? this.mOverScrolledTopPixels : this.mOverScrolledBottomPixels;
    }

    private void setOverScrolledPixels(float f, boolean z) {
        if (z) {
            this.mOverScrolledTopPixels = f;
        } else {
            this.mOverScrolledBottomPixels = f;
        }
    }

    private void onCustomOverScrolled(int i, boolean z) {
        if (!this.mScroller.isFinished()) {
            setOwnScrollY(i);
            if (z) {
                springBack();
                return;
            }
            float currentOverScrollAmount = getCurrentOverScrollAmount(true);
            int i2 = this.mOwnScrollY;
            if (i2 < 0) {
                notifyOverscrollTopListener((float) (-i2), isRubberbanded(true));
            } else {
                notifyOverscrollTopListener(currentOverScrollAmount, isRubberbanded(true));
            }
        } else {
            setOwnScrollY(i);
        }
    }

    private void springBack() {
        boolean z;
        float f;
        int scrollRange = getScrollRange();
        boolean z2 = this.mOwnScrollY <= 0;
        boolean z3 = this.mOwnScrollY >= scrollRange;
        if (z2 || z3) {
            if (z2) {
                f = (float) (-this.mOwnScrollY);
                setOwnScrollY(0);
                this.mDontReportNextOverScroll = true;
                z = true;
            } else {
                float f2 = (float) (this.mOwnScrollY - scrollRange);
                setOwnScrollY(scrollRange);
                f = f2;
                z = false;
            }
            setOverScrollAmount(f, z, false);
            setOverScrollAmount(0.0f, z, true);
            this.mScroller.forceFinished(true);
        }
    }

    /* access modifiers changed from: private */
    public int getScrollRange() {
        int i = this.mContentHeight;
        if (!isExpanded() && this.mHeadsUpManager.hasPinnedHeadsUp()) {
            i = this.mHeadsUpInset + getTopHeadsUpPinnedHeight();
        }
        int max = Math.max(0, i - this.mMaxLayoutHeight);
        int imeInset = getImeInset();
        return max + Math.min(imeInset, Math.max(0, i - (getHeight() - imeInset)));
    }

    private int getImeInset() {
        return Math.max(0, this.mBottomInset - (getRootView().getHeight() - getHeight()));
    }

    public ExpandableView getFirstChildNotGone() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() != 8 && childAt != this.mShelf) {
                return (ExpandableView) childAt;
            }
        }
        return null;
    }

    private View getFirstChildBelowTranlsationY(float f, boolean z) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() != 8) {
                float translationY = childAt.getTranslationY();
                if (translationY >= f) {
                    return childAt;
                }
                if (!z && (childAt instanceof ExpandableNotificationRow)) {
                    ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) childAt;
                    if (expandableNotificationRow.isSummaryWithChildren() && expandableNotificationRow.areChildrenExpanded()) {
                        List notificationChildren = expandableNotificationRow.getNotificationChildren();
                        for (int i2 = 0; i2 < notificationChildren.size(); i2++) {
                            ExpandableNotificationRow expandableNotificationRow2 = (ExpandableNotificationRow) notificationChildren.get(i2);
                            if (expandableNotificationRow2.getTranslationY() + translationY >= f) {
                                return expandableNotificationRow2;
                            }
                        }
                        continue;
                    }
                }
            }
        }
        return null;
    }

    public ExpandableView getLastChildNotGone() {
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = getChildAt(childCount);
            if (childAt.getVisibility() != 8 && childAt != this.mShelf) {
                return (ExpandableView) childAt;
            }
        }
        return null;
    }

    public int getNotGoneChildCount() {
        int childCount = getChildCount();
        int i = 0;
        for (int i2 = 0; i2 < childCount; i2++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i2);
            if (!(expandableView.getVisibility() == 8 || expandableView.willBeGone() || expandableView == this.mShelf)) {
                i++;
            }
        }
        return i;
    }

    /* access modifiers changed from: private */
    public void updateContentHeight() {
        int interpolate;
        float f;
        float f2 = (float) this.mPaddingBetweenElements;
        int i = this.mMaxDisplayedNotifications;
        float f3 = f2;
        float f4 = 0.0f;
        int i2 = 0;
        int i3 = 0;
        boolean z = false;
        for (int i4 = 0; i4 < getChildCount(); i4++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i4);
            boolean z2 = expandableView == this.mFooterView && onKeyguard();
            if (expandableView.getVisibility() != 8 && !expandableView.hasNoContentHeight() && !z2) {
                if (i != -1 && i2 >= i) {
                    expandableView = this.mShelf;
                    z = true;
                }
                float increasedPaddingAmount = expandableView.getIncreasedPaddingAmount();
                if (increasedPaddingAmount >= 0.0f) {
                    f = (float) ((int) NotificationUtils.interpolate(f3, (float) this.mIncreasedPaddingBetweenElements, increasedPaddingAmount));
                    interpolate = (int) NotificationUtils.interpolate((float) this.mPaddingBetweenElements, (float) this.mIncreasedPaddingBetweenElements, increasedPaddingAmount);
                } else {
                    interpolate = (int) NotificationUtils.interpolate(0.0f, (float) this.mPaddingBetweenElements, 1.0f + increasedPaddingAmount);
                    f = f4 > 0.0f ? (float) ((int) NotificationUtils.interpolate((float) interpolate, (float) this.mIncreasedPaddingBetweenElements, f4)) : (float) interpolate;
                }
                f3 = (float) interpolate;
                if (i3 != 0) {
                    i3 = (int) (((float) i3) + f);
                }
                i3 += expandableView.getIntrinsicHeight();
                i2++;
                if (z) {
                    break;
                }
                f4 = increasedPaddingAmount;
            }
        }
        this.mIntrinsicContentHeight = i3;
        this.mContentHeight = i3 + this.mTopPadding + this.mBottomMargin;
        updateScrollability();
        clampScrollPosition();
        this.mAmbientState.setLayoutMaxHeight(this.mContentHeight);
    }

    public boolean hasPulsingNotifications() {
        return this.mPulsing;
    }

    private void updateScrollability() {
        boolean z = !this.mQsExpanded && getScrollRange() > 0;
        if (z != this.mScrollable) {
            this.mScrollable = z;
            setFocusable(z);
            updateForwardAndBackwardScrollability();
        }
    }

    private void updateForwardAndBackwardScrollability() {
        boolean z = true;
        boolean z2 = this.mScrollable && !isScrolledToBottom();
        boolean z3 = this.mScrollable && !isScrolledToTop();
        if (z2 == this.mForwardScrollable && z3 == this.mBackwardScrollable) {
            z = false;
        }
        this.mForwardScrollable = z2;
        this.mBackwardScrollable = z3;
        if (z) {
            sendAccessibilityEvent(2048);
        }
    }

    private void updateBackground() {
        if (this.mShouldDrawNotificationBackground && !this.mAmbientState.isFullyDark()) {
            updateBackgroundBounds();
            if (didSectionBoundsChange()) {
                boolean z = this.mAnimateNextSectionBoundsChange || this.mAnimateNextBackgroundTop || this.mAnimateNextBackgroundBottom || areSectionBoundsAnimating();
                if (!isExpanded()) {
                    abortBackgroundAnimators();
                    z = false;
                }
                if (z) {
                    startBackgroundAnimation();
                } else {
                    for (NotificationSection resetCurrentBounds : this.mSections) {
                        resetCurrentBounds.resetCurrentBounds();
                    }
                    invalidate();
                }
            } else {
                abortBackgroundAnimators();
            }
            this.mAnimateNextBackgroundTop = false;
            this.mAnimateNextBackgroundBottom = false;
            this.mAnimateNextSectionBoundsChange = false;
        }
    }

    private void abortBackgroundAnimators() {
        for (NotificationSection cancelAnimators : this.mSections) {
            cancelAnimators.cancelAnimators();
        }
    }

    private boolean didSectionBoundsChange() {
        for (NotificationSection didBoundsChange : this.mSections) {
            if (didBoundsChange.didBoundsChange()) {
                return true;
            }
        }
        return false;
    }

    private boolean areSectionBoundsAnimating() {
        for (NotificationSection areBoundsAnimating : this.mSections) {
            if (areBoundsAnimating.areBoundsAnimating()) {
                return true;
            }
        }
        return false;
    }

    private void startBackgroundAnimation() {
        NotificationSection[] notificationSectionArr;
        boolean z;
        boolean z2;
        NotificationSection firstVisibleSection = getFirstVisibleSection();
        NotificationSection lastVisibleSection = getLastVisibleSection();
        for (NotificationSection notificationSection : this.mSections) {
            if (notificationSection == firstVisibleSection) {
                z = this.mAnimateNextBackgroundTop;
            } else {
                z = this.mAnimateNextSectionBoundsChange;
            }
            if (notificationSection == lastVisibleSection) {
                z2 = this.mAnimateNextBackgroundBottom;
            } else {
                z2 = this.mAnimateNextSectionBoundsChange;
            }
            notificationSection.startBackgroundAnimation(z, z2);
        }
    }

    private void updateBackgroundBounds() {
        NotificationSection[] notificationSectionArr;
        int i;
        NotificationSection[] notificationSectionArr2;
        int i2 = this.mSidePaddings;
        int width = getWidth() - this.mSidePaddings;
        for (NotificationSection notificationSection : this.mSections) {
            notificationSection.getBounds().left = i2;
            notificationSection.getBounds().right = width;
        }
        if (!this.mIsExpanded) {
            for (NotificationSection notificationSection2 : this.mSections) {
                notificationSection2.getBounds().top = 0;
                notificationSection2.getBounds().bottom = 0;
            }
            return;
        }
        NotificationSection lastVisibleSection = getLastVisibleSection();
        boolean z = true;
        if (this.mStatusBarState != 1) {
            i = (int) (((float) this.mTopPadding) + this.mStackTranslation);
        } else if (lastVisibleSection == null) {
            i = this.mTopPadding;
        } else {
            NotificationSection firstVisibleSection = getFirstVisibleSection();
            firstVisibleSection.updateBounds(0, 0, false);
            i = firstVisibleSection.getBounds().top;
        }
        if (this.mAmbientPulseManager.getAllEntries().count() > 1) {
            z = false;
        }
        NotificationSection[] notificationSectionArr3 = this.mSections;
        int length = notificationSectionArr3.length;
        boolean z2 = z;
        int i3 = i;
        int i4 = 0;
        while (i4 < length) {
            NotificationSection notificationSection3 = notificationSectionArr3[i4];
            i3 = notificationSection3.updateBounds(i3, notificationSection3 == lastVisibleSection ? (int) (ViewState.getFinalTranslationY(this.mShelf) + ((float) this.mShelf.getIntrinsicHeight())) : i3, z2);
            i4++;
            z2 = false;
        }
    }

    private NotificationSection getFirstVisibleSection() {
        NotificationSection[] notificationSectionArr;
        for (NotificationSection notificationSection : this.mSections) {
            if (notificationSection.getFirstVisibleChild() != null) {
                return notificationSection;
            }
        }
        return null;
    }

    private NotificationSection getLastVisibleSection() {
        for (int length = this.mSections.length - 1; length >= 0; length--) {
            NotificationSection notificationSection = this.mSections[length];
            if (notificationSection.getLastVisibleChild() != null) {
                return notificationSection;
            }
        }
        return null;
    }

    private ActivatableNotificationView getLastChildWithBackground() {
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = getChildAt(childCount);
            if (childAt.getVisibility() != 8 && (childAt instanceof ActivatableNotificationView) && childAt != this.mShelf) {
                return (ActivatableNotificationView) childAt;
            }
        }
        return null;
    }

    private ActivatableNotificationView getFirstChildWithBackground() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() != 8 && (childAt instanceof ActivatableNotificationView) && childAt != this.mShelf) {
                return (ActivatableNotificationView) childAt;
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void fling(int i) {
        if (getChildCount() > 0) {
            float currentOverScrollAmount = getCurrentOverScrollAmount(true);
            int i2 = 0;
            float currentOverScrollAmount2 = getCurrentOverScrollAmount(false);
            if (i < 0 && currentOverScrollAmount > 0.0f) {
                setOwnScrollY(this.mOwnScrollY - ((int) currentOverScrollAmount));
                this.mDontReportNextOverScroll = true;
                setOverScrollAmount(0.0f, true, false);
                this.mMaxOverScroll = ((((float) Math.abs(i)) / 1000.0f) * getRubberBandFactor(true) * ((float) this.mOverflingDistance)) + currentOverScrollAmount;
            } else if (i <= 0 || currentOverScrollAmount2 <= 0.0f) {
                this.mMaxOverScroll = 0.0f;
            } else {
                setOwnScrollY((int) (((float) this.mOwnScrollY) + currentOverScrollAmount2));
                setOverScrollAmount(0.0f, false, false);
                this.mMaxOverScroll = ((((float) Math.abs(i)) / 1000.0f) * getRubberBandFactor(false) * ((float) this.mOverflingDistance)) + currentOverScrollAmount2;
            }
            int max = Math.max(0, getScrollRange());
            if (this.mExpandedInThisMotion) {
                max = Math.min(max, this.mMaxScrollAfterExpand);
            }
            int i3 = max;
            OverScroller overScroller = this.mScroller;
            int i4 = this.mScrollX;
            int i5 = this.mOwnScrollY;
            if (!this.mExpandedInThisMotion || i5 < 0) {
                i2 = 1073741823;
            }
            overScroller.fling(i4, i5, 1, i, 0, 0, 0, i3, 0, i2);
            lambda$new$1$NotificationStackScrollLayout();
        }
    }

    private boolean shouldOverScrollFling(int i) {
        float currentOverScrollAmount = getCurrentOverScrollAmount(true);
        if (!this.mScrolledToTopOnFirstDown || this.mExpandedInThisMotion || currentOverScrollAmount <= this.mMinTopOverScrollToEscape || i <= 0) {
            return false;
        }
        return true;
    }

    public void updateTopPadding(float f, boolean z, boolean z2) {
        int i = (int) f;
        int layoutMinHeight = getLayoutMinHeight() + i;
        if (layoutMinHeight > getHeight()) {
            this.mTopPaddingOverflow = (float) (layoutMinHeight - getHeight());
        } else {
            this.mTopPaddingOverflow = 0.0f;
        }
        if (!z2) {
            i = clampPadding(i);
        }
        setTopPadding(i, z);
        setExpandedHeight(this.mExpandedHeight);
    }

    public void setMaxTopPadding(int i) {
        this.mMaxTopPadding = i;
    }

    public int getLayoutMinHeight() {
        if (isHeadsUpTransition()) {
            return getTopHeadsUpPinnedHeight();
        }
        return this.mShelf.getVisibility() == 8 ? 0 : this.mShelf.getIntrinsicHeight();
    }

    public float getTopPaddingOverflow() {
        return this.mTopPaddingOverflow;
    }

    private int clampPadding(int i) {
        return Math.max(i, this.mIntrinsicPadding);
    }

    private float getRubberBandFactor(boolean z) {
        if (!z) {
            return 0.35f;
        }
        if (this.mExpandedInThisMotion) {
            return 0.15f;
        }
        if (this.mIsExpansionChanging || this.mPanelTracking) {
            return 0.21f;
        }
        if (this.mScrolledToTopOnFirstDown) {
            return 1.0f;
        }
        return 0.35f;
    }

    private boolean isRubberbanded(boolean z) {
        return !z || this.mExpandedInThisMotion || this.mIsExpansionChanging || this.mPanelTracking || !this.mScrolledToTopOnFirstDown;
    }

    public void setChildTransferInProgress(boolean z) {
        Assert.isMainThread();
        this.mChildTransferInProgress = z;
    }

    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        if (!this.mChildTransferInProgress) {
            onViewRemovedInternal((ExpandableView) view, this);
        }
    }

    public void cleanUpViewStateForEntry(NotificationEntry notificationEntry) {
        if (notificationEntry.getRow() == this.mSwipeHelper.getTranslatingParentView()) {
            this.mSwipeHelper.clearTranslatingParentView();
        }
    }

    private void onViewRemovedInternal(ExpandableView expandableView, ViewGroup viewGroup) {
        if (!this.mChangePositionInProgress) {
            expandableView.setOnHeightChangedListener(null);
            updateScrollStateForRemovedChild(expandableView);
            if (!generateRemoveAnimation(expandableView)) {
                this.mSwipedOutViews.remove(expandableView);
            } else if (!this.mSwipedOutViews.contains(expandableView) || Math.abs(expandableView.getTranslation()) != ((float) expandableView.getWidth())) {
                viewGroup.addTransientView(expandableView, 0);
                expandableView.setTransientContainer(viewGroup);
            }
            updateAnimationState(false, expandableView);
            focusNextViewIfFocused(expandableView);
        }
    }

    private void focusNextViewIfFocused(View view) {
        float f;
        if (view instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
            if (expandableNotificationRow.shouldRefocusOnDismiss()) {
                View childAfterViewWhenDismissed = expandableNotificationRow.getChildAfterViewWhenDismissed();
                if (childAfterViewWhenDismissed == null) {
                    View groupParentWhenDismissed = expandableNotificationRow.getGroupParentWhenDismissed();
                    if (groupParentWhenDismissed != null) {
                        f = groupParentWhenDismissed.getTranslationY();
                    } else {
                        f = view.getTranslationY();
                    }
                    childAfterViewWhenDismissed = getFirstChildBelowTranlsationY(f, true);
                }
                if (childAfterViewWhenDismissed != null) {
                    childAfterViewWhenDismissed.requestAccessibilityFocus();
                }
            }
        }
    }

    private boolean isChildInGroup(View view) {
        return (view instanceof ExpandableNotificationRow) && this.mGroupManager.isChildInGroupWithSummary(((ExpandableNotificationRow) view).getStatusBarNotification());
    }

    private boolean generateRemoveAnimation(ExpandableView expandableView) {
        if (removeRemovedChildFromHeadsUpChangeAnimations(expandableView)) {
            this.mAddedHeadsUpChildren.remove(expandableView);
            return false;
        } else if (isClickedHeadsUp(expandableView)) {
            this.mClearTransientViewsWhenFinished.add(expandableView);
            return true;
        } else {
            if (this.mIsExpanded && this.mAnimationsEnabled && !isChildInInvisibleGroup(expandableView)) {
                if (!this.mChildrenToAddAnimated.contains(expandableView)) {
                    this.mChildrenToRemoveAnimated.add(expandableView);
                    this.mNeedsAnimation = true;
                    return true;
                }
                this.mChildrenToAddAnimated.remove(expandableView);
                this.mFromMoreCardAdditions.remove(expandableView);
            }
            return false;
        }
    }

    private boolean isClickedHeadsUp(View view) {
        return HeadsUpUtil.isClickedHeadsUpNotification(view);
    }

    private boolean removeRemovedChildFromHeadsUpChangeAnimations(View view) {
        Iterator it = this.mHeadsUpChangeAnimations.iterator();
        boolean z = false;
        while (it.hasNext()) {
            Pair pair = (Pair) it.next();
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) pair.first;
            boolean booleanValue = ((Boolean) pair.second).booleanValue();
            if (view == expandableNotificationRow) {
                this.mTmpList.add(pair);
                z |= booleanValue;
            }
        }
        if (z) {
            this.mHeadsUpChangeAnimations.removeAll(this.mTmpList);
            ((ExpandableNotificationRow) view).setHeadsUpAnimatingAway(false);
        }
        this.mTmpList.clear();
        return z;
    }

    private boolean isChildInInvisibleGroup(View view) {
        if (!(view instanceof ExpandableNotificationRow)) {
            return false;
        }
        ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
        NotificationEntry groupSummary = this.mGroupManager.getGroupSummary(expandableNotificationRow.getStatusBarNotification());
        if (groupSummary == null || groupSummary.getRow() == expandableNotificationRow || expandableNotificationRow.getVisibility() != 4) {
            return false;
        }
        return true;
    }

    private void updateScrollStateForRemovedChild(ExpandableView expandableView) {
        float f;
        int positionInLinearLayout = getPositionInLinearLayout(expandableView);
        float increasedPaddingAmount = expandableView.getIncreasedPaddingAmount();
        if (increasedPaddingAmount >= 0.0f) {
            f = NotificationUtils.interpolate((float) this.mPaddingBetweenElements, (float) this.mIncreasedPaddingBetweenElements, increasedPaddingAmount);
        } else {
            f = NotificationUtils.interpolate(0.0f, (float) this.mPaddingBetweenElements, increasedPaddingAmount + 1.0f);
        }
        int intrinsicHeight = getIntrinsicHeight(expandableView) + ((int) f);
        int i = positionInLinearLayout + intrinsicHeight;
        int i2 = this.mOwnScrollY;
        if (i <= i2) {
            setOwnScrollY(i2 - intrinsicHeight);
        } else if (positionInLinearLayout < i2) {
            setOwnScrollY(positionInLinearLayout);
        }
    }

    private int getIntrinsicHeight(View view) {
        if (view instanceof ExpandableView) {
            return ((ExpandableView) view).getIntrinsicHeight();
        }
        return view.getHeight();
    }

    public int getPositionInLinearLayout(View view) {
        ExpandableNotificationRow expandableNotificationRow;
        int interpolate;
        float f;
        ExpandableNotificationRow expandableNotificationRow2 = null;
        if (isChildInGroup(view)) {
            expandableNotificationRow2 = (ExpandableNotificationRow) view;
            r14 = expandableNotificationRow2.getNotificationParent();
            expandableNotificationRow = r14;
            r14 = r14;
        } else {
            expandableNotificationRow = 0;
            r14 = view;
        }
        float f2 = (float) this.mPaddingBetweenElements;
        float f3 = 0.0f;
        int i = 0;
        for (int i2 = 0; i2 < getChildCount(); i2++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i2);
            boolean z = expandableView.getVisibility() != 8;
            if (z && !expandableView.hasNoContentHeight()) {
                float increasedPaddingAmount = expandableView.getIncreasedPaddingAmount();
                if (increasedPaddingAmount >= 0.0f) {
                    f = (float) ((int) NotificationUtils.interpolate(f2, (float) this.mIncreasedPaddingBetweenElements, increasedPaddingAmount));
                    interpolate = (int) NotificationUtils.interpolate((float) this.mPaddingBetweenElements, (float) this.mIncreasedPaddingBetweenElements, increasedPaddingAmount);
                } else {
                    interpolate = (int) NotificationUtils.interpolate(0.0f, (float) this.mPaddingBetweenElements, 1.0f + increasedPaddingAmount);
                    f = f3 > 0.0f ? (float) ((int) NotificationUtils.interpolate((float) interpolate, (float) this.mIncreasedPaddingBetweenElements, f3)) : (float) interpolate;
                }
                f2 = (float) interpolate;
                if (i != 0) {
                    i = (int) (((float) i) + f);
                }
                f3 = increasedPaddingAmount;
            }
            if (expandableView == r14) {
                if (expandableNotificationRow != 0) {
                    i += expandableNotificationRow.getPositionOfChild(expandableNotificationRow2);
                }
                return i;
            }
            if (z) {
                i += getIntrinsicHeight(expandableView);
            }
        }
        return 0;
    }

    public void onViewAdded(View view) {
        super.onViewAdded(view);
        onViewAddedInternal((ExpandableView) view);
    }

    private void updateFirstAndLastBackgroundViews() {
        ActivatableNotificationView activatableNotificationView;
        NotificationSection firstVisibleSection = getFirstVisibleSection();
        NotificationSection lastVisibleSection = getLastVisibleSection();
        ActivatableNotificationView activatableNotificationView2 = null;
        if (firstVisibleSection == null) {
            activatableNotificationView = null;
        } else {
            activatableNotificationView = firstVisibleSection.getFirstVisibleChild();
        }
        if (lastVisibleSection != null) {
            activatableNotificationView2 = lastVisibleSection.getLastVisibleChild();
        }
        ActivatableNotificationView firstChildWithBackground = getFirstChildWithBackground();
        ActivatableNotificationView lastChildWithBackground = getLastChildWithBackground();
        NotificationSectionsManager notificationSectionsManager = this.mSectionsManager;
        NotificationSection[] notificationSectionArr = this.mSections;
        boolean z = true;
        boolean updateFirstAndLastViewsInSections = notificationSectionsManager.updateFirstAndLastViewsInSections(notificationSectionArr[0], notificationSectionArr[1], firstChildWithBackground, lastChildWithBackground);
        if (!this.mAnimationsEnabled || !this.mIsExpanded) {
            this.mAnimateNextBackgroundTop = false;
            this.mAnimateNextBackgroundBottom = false;
            this.mAnimateNextSectionBoundsChange = false;
        } else {
            this.mAnimateNextBackgroundTop = firstChildWithBackground != activatableNotificationView;
            if (lastChildWithBackground == activatableNotificationView2 && !this.mAnimateBottomOnLayout) {
                z = false;
            }
            this.mAnimateNextBackgroundBottom = z;
            this.mAnimateNextSectionBoundsChange = updateFirstAndLastViewsInSections;
        }
        this.mAmbientState.setLastVisibleBackgroundChild(lastChildWithBackground);
        this.mRoundnessManager.updateRoundedChildren(this.mSections);
        this.mAnimateBottomOnLayout = false;
        invalidate();
    }

    private void onViewAddedInternal(ExpandableView expandableView) {
        updateHideSensitiveForChild(expandableView);
        expandableView.setOnHeightChangedListener(this);
        generateAddAnimation(expandableView, false);
        updateAnimationState(expandableView);
        updateChronometerForChild(expandableView);
        if (expandableView instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) expandableView).setDismissRtl(this.mDismissRtl);
        }
    }

    private void updateHideSensitiveForChild(ExpandableView expandableView) {
        expandableView.setHideSensitiveForIntrinsicHeight(shouldHideSensitive(expandableView, this.mAmbientState.isHideSensitive()));
    }

    public void notifyGroupChildRemoved(ExpandableView expandableView, ViewGroup viewGroup) {
        onViewRemovedInternal(expandableView, viewGroup);
    }

    public void notifyGroupChildAdded(ExpandableView expandableView) {
        onViewAddedInternal(expandableView);
    }

    public void setAnimationsEnabled(boolean z) {
        this.mAnimationsEnabled = z;
        updateNotificationAnimationStates();
        if (!z) {
            this.mSwipedOutViews.clear();
            this.mChildrenToRemoveAnimated.clear();
            clearTemporaryViewsInGroup(this);
        }
    }

    private void updateNotificationAnimationStates() {
        boolean z = this.mAnimationsEnabled || hasPulsingNotifications();
        this.mShelf.setAnimationsEnabled(z);
        this.mIconAreaController.setAnimationsEnabled(z);
        int childCount = getChildCount();
        boolean z2 = z;
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            z2 &= this.mIsExpanded || isPinnedHeadsUp(childAt);
            updateAnimationState(z2, childAt);
        }
    }

    private void updateAnimationState(View view) {
        updateAnimationState((this.mAnimationsEnabled || hasPulsingNotifications()) && (this.mIsExpanded || isPinnedHeadsUp(view)), view);
    }

    public void setExpandingNotification(ExpandableNotificationRow expandableNotificationRow) {
        this.mAmbientState.setExpandingNotification(expandableNotificationRow);
        requestChildrenUpdate();
    }

    public void bindRow(ExpandableNotificationRow expandableNotificationRow) {
        expandableNotificationRow.setHeadsUpAnimatingAwayListener(new Consumer(expandableNotificationRow) {
            private final /* synthetic */ ExpandableNotificationRow f$1;

            {
                this.f$1 = r2;
            }

            public final void accept(Object obj) {
                NotificationStackScrollLayout.this.lambda$bindRow$5$NotificationStackScrollLayout(this.f$1, (Boolean) obj);
            }
        });
    }

    public /* synthetic */ void lambda$bindRow$5$NotificationStackScrollLayout(ExpandableNotificationRow expandableNotificationRow, Boolean bool) {
        this.mRoundnessManager.onHeadsupAnimatingAwayChanged(expandableNotificationRow, bool.booleanValue());
        this.mHeadsUpAppearanceController.lambda$updateHeadsUpHeaders$4$HeadsUpAppearanceController(expandableNotificationRow.getEntry());
    }

    public boolean containsView(View view) {
        return view.getParent() == this;
    }

    public void applyExpandAnimationParams(ExpandAnimationParameters expandAnimationParameters) {
        this.mAmbientState.setExpandAnimationTopChange(expandAnimationParameters == null ? 0 : expandAnimationParameters.getTopChange());
        requestChildrenUpdate();
    }

    private void updateAnimationState(boolean z, View view) {
        if (view instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) view).setIconAnimationRunning(z);
        }
    }

    public boolean isAddOrRemoveAnimationPending() {
        return this.mNeedsAnimation && (!this.mChildrenToAddAnimated.isEmpty() || !this.mChildrenToRemoveAnimated.isEmpty());
    }

    public void generateAddAnimation(ExpandableView expandableView, boolean z) {
        if (this.mIsExpanded && this.mAnimationsEnabled && !this.mChangePositionInProgress) {
            this.mChildrenToAddAnimated.add(expandableView);
            if (z) {
                this.mFromMoreCardAdditions.add(expandableView);
            }
            this.mNeedsAnimation = true;
        }
        if (isHeadsUp(expandableView) && this.mAnimationsEnabled && !this.mChangePositionInProgress) {
            this.mAddedHeadsUpChildren.add(expandableView);
            this.mChildrenToAddAnimated.remove(expandableView);
        }
    }

    public void changeViewPosition(ExpandableView expandableView, int i) {
        Assert.isMainThread();
        if (!this.mChangePositionInProgress) {
            int indexOfChild = indexOfChild(expandableView);
            boolean z = false;
            if (indexOfChild == -1) {
                if ((expandableView instanceof ExpandableNotificationRow) && ((ExpandableNotificationRow) expandableView).getTransientContainer() != null) {
                    z = true;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Attempting to re-position ");
                sb.append(z ? "transient" : "");
                sb.append(" view {");
                sb.append(expandableView);
                sb.append("}");
                Log.e("StackScroller", sb.toString());
                return;
            }
            if (!(expandableView == null || expandableView.getParent() != this || indexOfChild == i)) {
                this.mChangePositionInProgress = true;
                expandableView.setChangingPosition(true);
                removeView(expandableView);
                addView(expandableView, i);
                expandableView.setChangingPosition(false);
                this.mChangePositionInProgress = false;
                if (this.mIsExpanded && this.mAnimationsEnabled && expandableView.getVisibility() != 8) {
                    this.mChildrenChangingPositions.add(expandableView);
                    this.mNeedsAnimation = true;
                }
            }
            return;
        }
        throw new IllegalStateException("Reentrant call to changeViewPosition");
    }

    private void startAnimationToState() {
        if (this.mNeedsAnimation) {
            generateAllAnimationEvents();
            this.mNeedsAnimation = false;
        }
        if (!this.mAnimationEvents.isEmpty() || isCurrentlyAnimating()) {
            setAnimationRunning(true);
            this.mStateAnimator.startAnimationForEvents(this.mAnimationEvents, this.mGoToFullShadeDelay);
            this.mAnimationEvents.clear();
            updateBackground();
            updateViewShadows();
            updateClippingToTopRoundedCorner();
        } else {
            applyCurrentState();
        }
        this.mGoToFullShadeDelay = 0;
    }

    private void generateAllAnimationEvents() {
        generateHeadsUpAnimationEvents();
        generateChildRemovalEvents();
        generateChildAdditionEvents();
        generatePositionChangeEvents();
        generateTopPaddingEvent();
        generateActivateEvent();
        generateDimmedEvent();
        generateHideSensitiveEvent();
        generateDarkEvent();
        generateGoToFullShadeEvent();
        generateViewResizeEvent();
        generateGroupExpansionEvent();
        generateAnimateEverythingEvent();
    }

    private void generateHeadsUpAnimationEvents() {
        Iterator it = this.mHeadsUpChangeAnimations.iterator();
        while (it.hasNext()) {
            Pair pair = (Pair) it.next();
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) pair.first;
            boolean booleanValue = ((Boolean) pair.second).booleanValue();
            int i = 15;
            boolean z = false;
            boolean z2 = expandableNotificationRow.isPinned() && !this.mIsExpanded;
            if (this.mIsExpanded || booleanValue) {
                ExpandableViewState viewState = expandableNotificationRow.getViewState();
                if (viewState != null) {
                    if (booleanValue && (this.mAddedHeadsUpChildren.contains(expandableNotificationRow) || z2)) {
                        i = (z2 || shouldHunAppearFromBottom(viewState)) ? 12 : 0;
                        z = !z2;
                    }
                }
            } else {
                i = expandableNotificationRow.wasJustClicked() ? 14 : 13;
                if (expandableNotificationRow.isChildInGroup()) {
                    expandableNotificationRow.setHeadsUpAnimatingAway(false);
                }
            }
            AnimationEvent animationEvent = new AnimationEvent(expandableNotificationRow, i);
            animationEvent.headsUpFromBottom = z;
            this.mAnimationEvents.add(animationEvent);
        }
        this.mHeadsUpChangeAnimations.clear();
        this.mAddedHeadsUpChildren.clear();
    }

    private boolean shouldHunAppearFromBottom(ExpandableViewState expandableViewState) {
        return expandableViewState.yTranslation + ((float) expandableViewState.height) >= this.mAmbientState.getMaxHeadsUpTranslation();
    }

    private void generateGroupExpansionEvent() {
        ExpandableView expandableView = this.mExpandedGroupView;
        if (expandableView != null) {
            this.mAnimationEvents.add(new AnimationEvent(expandableView, 11));
            this.mExpandedGroupView = null;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0021, code lost:
        r0 = true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void generateViewResizeEvent() {
        /*
            r5 = this;
            boolean r0 = r5.mNeedViewResizeAnimation
            r1 = 0
            if (r0 == 0) goto L_0x0033
            java.util.ArrayList<com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout$AnimationEvent> r0 = r5.mAnimationEvents
            java.util.Iterator r0 = r0.iterator()
        L_0x000b:
            boolean r2 = r0.hasNext()
            if (r2 == 0) goto L_0x0023
            java.lang.Object r2 = r0.next()
            com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout$AnimationEvent r2 = (com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout.AnimationEvent) r2
            int r2 = r2.animationType
            r3 = 14
            if (r2 == r3) goto L_0x0021
            r3 = 13
            if (r2 != r3) goto L_0x000b
        L_0x0021:
            r0 = 1
            goto L_0x0024
        L_0x0023:
            r0 = r1
        L_0x0024:
            if (r0 != 0) goto L_0x0033
            java.util.ArrayList<com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout$AnimationEvent> r0 = r5.mAnimationEvents
            com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout$AnimationEvent r2 = new com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout$AnimationEvent
            r3 = 0
            r4 = 10
            r2.<init>(r3, r4)
            r0.add(r2)
        L_0x0033:
            r5.mNeedViewResizeAnimation = r1
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout.generateViewResizeEvent():void");
    }

    private void generateChildRemovalEvents() {
        boolean z;
        boolean z2;
        Iterator it = this.mChildrenToRemoveAnimated.iterator();
        while (it.hasNext()) {
            ExpandableView expandableView = (ExpandableView) it.next();
            boolean contains = this.mSwipedOutViews.contains(expandableView);
            float translationY = expandableView.getTranslationY();
            int i = 1;
            if (expandableView instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
                if (!expandableNotificationRow.isRemoved() || !expandableNotificationRow.wasChildInGroupWhenRemoved()) {
                    z = true;
                } else {
                    translationY = expandableNotificationRow.getTranslationWhenRemoved();
                    z = false;
                }
                contains |= Math.abs(expandableNotificationRow.getTranslation()) == ((float) expandableNotificationRow.getWidth());
            } else {
                z = true;
            }
            if (!z2) {
                Rect clipBounds = expandableView.getClipBounds();
                z2 = clipBounds != null && clipBounds.height() == 0;
                if (z2 && (expandableView instanceof ExpandableView)) {
                    ViewGroup transientContainer = expandableView.getTransientContainer();
                    if (transientContainer != null) {
                        transientContainer.removeTransientView(expandableView);
                    }
                }
            }
            if (z2) {
                i = 2;
            }
            AnimationEvent animationEvent = new AnimationEvent(expandableView, i);
            animationEvent.viewAfterChangingView = getFirstChildBelowTranlsationY(translationY, z);
            this.mAnimationEvents.add(animationEvent);
            this.mSwipedOutViews.remove(expandableView);
        }
        this.mChildrenToRemoveAnimated.clear();
    }

    private void generatePositionChangeEvents() {
        Iterator it = this.mChildrenChangingPositions.iterator();
        while (it.hasNext()) {
            this.mAnimationEvents.add(new AnimationEvent((ExpandableView) it.next(), 6));
        }
        this.mChildrenChangingPositions.clear();
        if (this.mGenerateChildOrderChangedEvent) {
            this.mAnimationEvents.add(new AnimationEvent(null, 6));
            this.mGenerateChildOrderChangedEvent = false;
        }
    }

    private void generateChildAdditionEvents() {
        Iterator it = this.mChildrenToAddAnimated.iterator();
        while (it.hasNext()) {
            ExpandableView expandableView = (ExpandableView) it.next();
            if (this.mFromMoreCardAdditions.contains(expandableView)) {
                this.mAnimationEvents.add(new AnimationEvent(expandableView, 0, 360));
            } else {
                this.mAnimationEvents.add(new AnimationEvent(expandableView, 0));
            }
        }
        this.mChildrenToAddAnimated.clear();
        this.mFromMoreCardAdditions.clear();
    }

    private void generateTopPaddingEvent() {
        AnimationEvent animationEvent;
        if (this.mTopPaddingNeedsAnimation) {
            if (this.mAmbientState.isDark()) {
                animationEvent = new AnimationEvent((ExpandableView) null, 3, 550);
            } else {
                animationEvent = new AnimationEvent(null, 3);
            }
            this.mAnimationEvents.add(animationEvent);
        }
        this.mTopPaddingNeedsAnimation = false;
    }

    private void generateActivateEvent() {
        if (this.mActivateNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 4));
        }
        this.mActivateNeedsAnimation = false;
    }

    private void generateAnimateEverythingEvent() {
        if (this.mEverythingNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 16));
        }
        this.mEverythingNeedsAnimation = false;
    }

    private void generateDimmedEvent() {
        if (this.mDimmedNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 5));
        }
        this.mDimmedNeedsAnimation = false;
    }

    private void generateHideSensitiveEvent() {
        if (this.mHideSensitiveNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 9));
        }
        this.mHideSensitiveNeedsAnimation = false;
    }

    private void generateDarkEvent() {
        if (this.mDarkNeedsAnimation) {
            AnimationFilter animationFilter = new AnimationFilter();
            animationFilter.animateDark();
            animationFilter.animateY(this.mShelf);
            AnimationEvent animationEvent = new AnimationEvent((ExpandableView) null, 7, animationFilter);
            animationEvent.darkAnimationOriginIndex = this.mDarkAnimationOriginIndex;
            this.mAnimationEvents.add(animationEvent);
        }
        this.mDarkNeedsAnimation = false;
    }

    private void generateGoToFullShadeEvent() {
        if (this.mGoToFullShadeNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 8));
        }
        this.mGoToFullShadeNeedsAnimation = false;
    }

    /* access modifiers changed from: protected */
    public StackScrollAlgorithm createStackScrollAlgorithm(Context context) {
        return new StackScrollAlgorithm(context, this);
    }

    public boolean isInContentBounds(float f) {
        return f < ((float) (getHeight() - getEmptyBottomMargin()));
    }

    public void setLongPressListener(LongPressListener longPressListener) {
        this.mLongPressListener = longPressListener;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z;
        boolean z2 = motionEvent.getActionMasked() == 3 || motionEvent.getActionMasked() == 1;
        handleEmptySpaceClick(motionEvent);
        boolean z3 = this.mSwipingInProgress;
        if (!this.mIsExpanded || z3 || this.mOnlyScrollingInThisMotion) {
            z = false;
        } else {
            if (z2) {
                this.mExpandHelper.onlyObserveMovements(false);
            }
            boolean z4 = this.mExpandingNotification;
            z = this.mExpandHelper.onTouchEvent(motionEvent);
            if (this.mExpandedInThisMotion && !this.mExpandingNotification && z4 && !this.mDisallowScrollingInThisMotion) {
                dispatchDownEventToScroller(motionEvent);
            }
        }
        boolean onScrollTouch = (!this.mIsExpanded || z3 || this.mExpandingNotification || this.mDisallowScrollingInThisMotion) ? false : onScrollTouch(motionEvent);
        boolean onTouchEvent = (this.mIsBeingDragged || this.mExpandingNotification || this.mExpandedInThisMotion || this.mOnlyScrollingInThisMotion || this.mDisallowDismissInThisMotion) ? false : this.mSwipeHelper.onTouchEvent(motionEvent);
        NotificationGuts exposedGuts = this.mNotificationGutsManager.getExposedGuts();
        if (exposedGuts != null && !NotificationSwipeHelper.isTouchInView(motionEvent, exposedGuts) && (exposedGuts.getGutsContent() instanceof NotificationSnooze) && ((((NotificationSnooze) exposedGuts.getGutsContent()).isExpanded() && z2) || (!onTouchEvent && onScrollTouch))) {
            checkSnoozeLeavebehind();
        }
        if (motionEvent.getActionMasked() == 1) {
            this.mCheckForLeavebehind = true;
        }
        if (onTouchEvent || onScrollTouch || z || super.onTouchEvent(motionEvent)) {
            return true;
        }
        return false;
    }

    private void dispatchDownEventToScroller(MotionEvent motionEvent) {
        MotionEvent obtain = MotionEvent.obtain(motionEvent);
        obtain.setAction(0);
        onScrollTouch(obtain);
        obtain.recycle();
    }

    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if (!isScrollingEnabled() || !this.mIsExpanded || this.mSwipingInProgress || this.mExpandingNotification || this.mDisallowScrollingInThisMotion) {
            return false;
        }
        if ((motionEvent.getSource() & 2) != 0 && motionEvent.getAction() == 8 && !this.mIsBeingDragged) {
            float axisValue = motionEvent.getAxisValue(9);
            if (axisValue != 0.0f) {
                int verticalScrollFactor = (int) (axisValue * getVerticalScrollFactor());
                int scrollRange = getScrollRange();
                int i = this.mOwnScrollY;
                int i2 = i - verticalScrollFactor;
                if (i2 < 0) {
                    i2 = 0;
                } else if (i2 > scrollRange) {
                    i2 = scrollRange;
                }
                if (i2 != i) {
                    setOwnScrollY(i2);
                    return true;
                }
            }
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    private boolean onScrollTouch(MotionEvent motionEvent) {
        float f;
        if (!isScrollingEnabled()) {
            return false;
        }
        if (isInsideQsContainer(motionEvent) && !this.mIsBeingDragged) {
            return false;
        }
        this.mForcedScroll = null;
        initVelocityTrackerIfNotExists();
        this.mVelocityTracker.addMovement(motionEvent);
        int action = motionEvent.getAction() & 255;
        if (action != 0) {
            if (action != 1) {
                String str = " in onTouchEvent";
                String str2 = "Invalid pointerId=";
                String str3 = "StackScroller";
                if (action == 2) {
                    int findPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                    if (findPointerIndex == -1) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(str2);
                        sb.append(this.mActivePointerId);
                        sb.append(str);
                        Log.e(str3, sb.toString());
                    } else {
                        int y = (int) motionEvent.getY(findPointerIndex);
                        int x = (int) motionEvent.getX(findPointerIndex);
                        int i = this.mLastMotionY - y;
                        int abs = Math.abs(x - this.mDownX);
                        int abs2 = Math.abs(i);
                        if (!this.mIsBeingDragged && abs2 > this.mTouchSlop && abs2 > abs) {
                            setIsBeingDragged(true);
                            i = i > 0 ? i - this.mTouchSlop : i + this.mTouchSlop;
                        }
                        if (this.mIsBeingDragged) {
                            this.mLastMotionY = y;
                            int scrollRange = getScrollRange();
                            if (this.mExpandedInThisMotion) {
                                scrollRange = Math.min(scrollRange, this.mMaxScrollAfterExpand);
                            }
                            if (i < 0) {
                                f = overScrollDown(i);
                            } else {
                                f = overScrollUp(i, scrollRange);
                            }
                            if (f != 0.0f) {
                                customOverScrollBy((int) f, this.mOwnScrollY, scrollRange, getHeight() / 2);
                                checkSnoozeLeavebehind();
                            }
                        }
                    }
                } else if (action != 3) {
                    if (action == 5) {
                        int actionIndex = motionEvent.getActionIndex();
                        this.mLastMotionY = (int) motionEvent.getY(actionIndex);
                        this.mDownX = (int) motionEvent.getX(actionIndex);
                        this.mActivePointerId = motionEvent.getPointerId(actionIndex);
                    } else if (action == 6) {
                        onSecondaryPointerUp(motionEvent);
                        int findPointerIndex2 = motionEvent.findPointerIndex(this.mActivePointerId);
                        if (findPointerIndex2 == -1) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append(str2);
                            sb2.append(this.mActivePointerId);
                            sb2.append(str);
                            Log.e(str3, sb2.toString());
                        } else {
                            this.mLastMotionY = (int) motionEvent.getY(findPointerIndex2);
                            this.mDownX = (int) motionEvent.getX(findPointerIndex2);
                        }
                    }
                } else if (this.mIsBeingDragged && getChildCount() > 0) {
                    if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                        lambda$new$1$NotificationStackScrollLayout();
                    }
                    this.mActivePointerId = -1;
                    endDrag();
                }
            } else if (this.mIsBeingDragged) {
                VelocityTracker velocityTracker = this.mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
                int yVelocity = (int) velocityTracker.getYVelocity(this.mActivePointerId);
                if (shouldOverScrollFling(yVelocity)) {
                    onOverScrollFling(true, yVelocity);
                } else if (getChildCount() > 0) {
                    if (Math.abs(yVelocity) > this.mMinimumVelocity) {
                        if (getCurrentOverScrollAmount(true) == 0.0f || yVelocity > 0) {
                            fling(-yVelocity);
                        } else {
                            onOverScrollFling(false, yVelocity);
                        }
                    } else if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                        lambda$new$1$NotificationStackScrollLayout();
                    }
                }
                this.mActivePointerId = -1;
                endDrag();
            }
        } else if (getChildCount() == 0 || !isInContentBounds(motionEvent)) {
            return false;
        } else {
            setIsBeingDragged(!this.mScroller.isFinished());
            if (!this.mScroller.isFinished()) {
                this.mScroller.forceFinished(true);
            }
            this.mLastMotionY = (int) motionEvent.getY();
            this.mDownX = (int) motionEvent.getX();
            this.mActivePointerId = motionEvent.getPointerId(0);
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean isInsideQsContainer(MotionEvent motionEvent) {
        return motionEvent.getY() < ((float) this.mQsContainer.getBottom());
    }

    private void onOverScrollFling(boolean z, int i) {
        OnOverscrollTopChangedListener onOverscrollTopChangedListener = this.mOverscrollTopChangedListener;
        if (onOverscrollTopChangedListener != null) {
            onOverscrollTopChangedListener.flingTopOverscroll((float) i, z);
        }
        this.mDontReportNextOverScroll = true;
        setOverScrollAmount(0.0f, true, false);
    }

    private void onSecondaryPointerUp(MotionEvent motionEvent) {
        int action = (motionEvent.getAction() & 65280) >> 8;
        if (motionEvent.getPointerId(action) == this.mActivePointerId) {
            int i = action == 0 ? 1 : 0;
            this.mLastMotionY = (int) motionEvent.getY(i);
            this.mActivePointerId = motionEvent.getPointerId(i);
            VelocityTracker velocityTracker = this.mVelocityTracker;
            if (velocityTracker != null) {
                velocityTracker.clear();
            }
        }
    }

    private void endDrag() {
        setIsBeingDragged(false);
        recycleVelocityTracker();
        if (getCurrentOverScrollAmount(true) > 0.0f) {
            setOverScrollAmount(0.0f, true, true);
        }
        if (getCurrentOverScrollAmount(false) > 0.0f) {
            setOverScrollAmount(0.0f, false, true);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        initDownStates(motionEvent);
        handleEmptySpaceClick(motionEvent);
        boolean z = this.mSwipingInProgress;
        boolean onInterceptTouchEvent = (z || this.mOnlyScrollingInThisMotion) ? false : this.mExpandHelper.onInterceptTouchEvent(motionEvent);
        boolean onInterceptTouchEventScroll = (z || this.mExpandingNotification) ? false : onInterceptTouchEventScroll(motionEvent);
        boolean onInterceptTouchEvent2 = (this.mIsBeingDragged || this.mExpandingNotification || this.mExpandedInThisMotion || this.mOnlyScrollingInThisMotion || this.mDisallowDismissInThisMotion) ? false : this.mSwipeHelper.onInterceptTouchEvent(motionEvent);
        boolean z2 = motionEvent.getActionMasked() == 1;
        if (!NotificationSwipeHelper.isTouchInView(motionEvent, this.mNotificationGutsManager.getExposedGuts()) && z2 && !onInterceptTouchEvent2 && !onInterceptTouchEvent && !onInterceptTouchEventScroll) {
            this.mCheckForLeavebehind = false;
            this.mNotificationGutsManager.closeAndSaveGuts(true, false, false, -1, -1, false);
        }
        if (motionEvent.getActionMasked() == 1) {
            this.mCheckForLeavebehind = true;
        }
        if (onInterceptTouchEvent2 || onInterceptTouchEventScroll || onInterceptTouchEvent || super.onInterceptTouchEvent(motionEvent)) {
            return true;
        }
        return false;
    }

    private void handleEmptySpaceClick(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 1) {
            if (actionMasked != 2 || !this.mTouchIsClick) {
                return;
            }
            if (Math.abs(motionEvent.getY() - this.mInitialTouchY) > ((float) this.mTouchSlop) || Math.abs(motionEvent.getX() - this.mInitialTouchX) > ((float) this.mTouchSlop)) {
                this.mTouchIsClick = false;
            }
        } else if (this.mStatusBarState != 1 && this.mTouchIsClick && isBelowLastNotification(this.mInitialTouchX, this.mInitialTouchY)) {
            this.mOnEmptySpaceClickListener.onEmptySpaceClicked(this.mInitialTouchX, this.mInitialTouchY);
        }
    }

    private void initDownStates(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.mExpandedInThisMotion = false;
            this.mOnlyScrollingInThisMotion = !this.mScroller.isFinished();
            this.mDisallowScrollingInThisMotion = false;
            this.mDisallowDismissInThisMotion = false;
            this.mTouchIsClick = true;
            this.mInitialTouchX = motionEvent.getX();
            this.mInitialTouchY = motionEvent.getY();
        }
    }

    public void requestDisallowInterceptTouchEvent(boolean z) {
        super.requestDisallowInterceptTouchEvent(z);
        if (z) {
            cancelLongPress();
        }
    }

    private boolean onInterceptTouchEventScroll(MotionEvent motionEvent) {
        if (!isScrollingEnabled()) {
            return false;
        }
        int action = motionEvent.getAction();
        if (action == 2 && this.mIsBeingDragged) {
            return true;
        }
        int i = action & 255;
        if (i != 0) {
            if (i != 1) {
                if (i == 2) {
                    int i2 = this.mActivePointerId;
                    if (i2 != -1) {
                        int findPointerIndex = motionEvent.findPointerIndex(i2);
                        if (findPointerIndex == -1) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Invalid pointerId=");
                            sb.append(i2);
                            sb.append(" in onInterceptTouchEvent");
                            Log.e("StackScroller", sb.toString());
                        } else {
                            int y = (int) motionEvent.getY(findPointerIndex);
                            int x = (int) motionEvent.getX(findPointerIndex);
                            int abs = Math.abs(y - this.mLastMotionY);
                            int abs2 = Math.abs(x - this.mDownX);
                            if (abs > this.mTouchSlop && abs > abs2) {
                                setIsBeingDragged(true);
                                this.mLastMotionY = y;
                                this.mDownX = x;
                                initVelocityTrackerIfNotExists();
                                this.mVelocityTracker.addMovement(motionEvent);
                            }
                        }
                    }
                } else if (i != 3) {
                    if (i == 6) {
                        onSecondaryPointerUp(motionEvent);
                    }
                }
            }
            setIsBeingDragged(false);
            this.mActivePointerId = -1;
            recycleVelocityTracker();
            if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                lambda$new$1$NotificationStackScrollLayout();
            }
        } else {
            int y2 = (int) motionEvent.getY();
            this.mScrolledToTopOnFirstDown = isScrolledToTop();
            if (getChildAtPosition(motionEvent.getX(), (float) y2, false) == null) {
                setIsBeingDragged(false);
                recycleVelocityTracker();
            } else {
                this.mLastMotionY = y2;
                this.mDownX = (int) motionEvent.getX();
                this.mActivePointerId = motionEvent.getPointerId(0);
                initOrResetVelocityTracker();
                this.mVelocityTracker.addMovement(motionEvent);
                setIsBeingDragged(!this.mScroller.isFinished());
            }
        }
        return this.mIsBeingDragged;
    }

    private boolean isInContentBounds(MotionEvent motionEvent) {
        return isInContentBounds(motionEvent.getY());
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void setIsBeingDragged(boolean z) {
        this.mIsBeingDragged = z;
        if (z) {
            requestDisallowInterceptTouchEvent(true);
            cancelLongPress();
            resetExposedMenuView(true, true);
        }
    }

    public void requestDisallowLongPress() {
        cancelLongPress();
    }

    public void requestDisallowDismiss() {
        this.mDisallowDismissInThisMotion = true;
    }

    public void cancelLongPress() {
        this.mSwipeHelper.cancelLongPress();
    }

    public void setOnEmptySpaceClickListener(OnEmptySpaceClickListener onEmptySpaceClickListener) {
        this.mOnEmptySpaceClickListener = onEmptySpaceClickListener;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0021, code lost:
        if (r5 != 16908346) goto L_0x005b;
     */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x004d  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean performAccessibilityActionInternal(int r5, android.os.Bundle r6) {
        /*
            r4 = this;
            boolean r6 = super.performAccessibilityActionInternal(r5, r6)
            r0 = 1
            if (r6 == 0) goto L_0x0008
            return r0
        L_0x0008:
            boolean r6 = r4.isEnabled()
            r1 = 0
            if (r6 != 0) goto L_0x0010
            return r1
        L_0x0010:
            r6 = -1
            r2 = 4096(0x1000, float:5.74E-42)
            if (r5 == r2) goto L_0x0024
            r2 = 8192(0x2000, float:1.14794E-41)
            if (r5 == r2) goto L_0x0025
            r2 = 16908344(0x1020038, float:2.3877386E-38)
            if (r5 == r2) goto L_0x0025
            r6 = 16908346(0x102003a, float:2.3877392E-38)
            if (r5 == r6) goto L_0x0024
            goto L_0x005b
        L_0x0024:
            r6 = r0
        L_0x0025:
            int r5 = r4.getHeight()
            int r2 = r4.mPaddingBottom
            int r5 = r5 - r2
            int r2 = r4.mTopPadding
            int r5 = r5 - r2
            int r2 = r4.mPaddingTop
            int r5 = r5 - r2
            com.android.systemui.statusbar.NotificationShelf r2 = r4.mShelf
            int r2 = r2.getIntrinsicHeight()
            int r5 = r5 - r2
            int r2 = r4.mOwnScrollY
            int r6 = r6 * r5
            int r2 = r2 + r6
            int r5 = r4.getScrollRange()
            int r5 = java.lang.Math.min(r2, r5)
            int r5 = java.lang.Math.max(r1, r5)
            int r6 = r4.mOwnScrollY
            if (r5 == r6) goto L_0x005b
            android.widget.OverScroller r6 = r4.mScroller
            int r2 = r4.mScrollX
            int r3 = r4.mOwnScrollY
            int r5 = r5 - r3
            r6.startScroll(r2, r3, r1, r5)
            r4.lambda$new$1$NotificationStackScrollLayout()
            return r0
        L_0x005b:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout.performAccessibilityActionInternal(int, android.os.Bundle):boolean");
    }

    /* JADX WARNING: type inference failed for: r2v1, types: [android.view.View] */
    /* JADX WARNING: type inference failed for: r0v2, types: [android.view.View] */
    /* JADX WARNING: type inference failed for: r0v4 */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Unknown variable types count: 2 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeControlsIfOutsideTouch(android.view.MotionEvent r8) {
        /*
            r7 = this;
            com.android.systemui.statusbar.notification.row.NotificationGutsManager r0 = r7.mNotificationGutsManager
            com.android.systemui.statusbar.notification.row.NotificationGuts r0 = r0.getExposedGuts()
            com.android.systemui.statusbar.notification.stack.NotificationSwipeHelper r1 = r7.mSwipeHelper
            com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin r1 = r1.getCurrentMenuRow()
            com.android.systemui.statusbar.notification.stack.NotificationSwipeHelper r2 = r7.mSwipeHelper
            android.view.View r2 = r2.getTranslatingParentView()
            if (r0 == 0) goto L_0x001f
            com.android.systemui.statusbar.notification.row.NotificationGuts$GutsContent r3 = r0.getGutsContent()
            boolean r3 = r3.isLeavebehind()
            if (r3 != 0) goto L_0x001f
            goto L_0x002c
        L_0x001f:
            if (r1 == 0) goto L_0x002b
            boolean r0 = r1.isMenuVisible()
            if (r0 == 0) goto L_0x002b
            if (r2 == 0) goto L_0x002b
            r0 = r2
            goto L_0x002c
        L_0x002b:
            r0 = 0
        L_0x002c:
            if (r0 == 0) goto L_0x0043
            boolean r8 = com.android.systemui.statusbar.notification.stack.NotificationSwipeHelper.isTouchInView(r8, r0)
            if (r8 != 0) goto L_0x0043
            com.android.systemui.statusbar.notification.row.NotificationGutsManager r0 = r7.mNotificationGutsManager
            r1 = 0
            r2 = 0
            r3 = 1
            r4 = -1
            r5 = -1
            r6 = 0
            r0.closeAndSaveGuts(r1, r2, r3, r4, r5, r6)
            r8 = 1
            r7.resetExposedMenuView(r8, r8)
        L_0x0043:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout.closeControlsIfOutsideTouch(android.view.MotionEvent):void");
    }

    /* access modifiers changed from: private */
    public void setSwipingInProgress(boolean z) {
        this.mSwipingInProgress = z;
        if (z) {
            requestDisallowInterceptTouchEvent(true);
        }
    }

    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (!z) {
            cancelLongPress();
        }
    }

    public void clearChildFocus(View view) {
        super.clearChildFocus(view);
        if (this.mForcedScroll == view) {
            this.mForcedScroll = null;
        }
    }

    public boolean isScrolledToTop() {
        return this.mOwnScrollY == 0;
    }

    public boolean isScrolledToBottom() {
        return this.mOwnScrollY >= getScrollRange();
    }

    public int getEmptyBottomMargin() {
        return Math.max(this.mMaxLayoutHeight - this.mContentHeight, 0);
    }

    public void checkSnoozeLeavebehind() {
        if (this.mCheckForLeavebehind) {
            this.mNotificationGutsManager.closeAndSaveGuts(true, false, false, -1, -1, false);
            this.mCheckForLeavebehind = false;
        }
    }

    public void resetCheckSnoozeLeavebehind() {
        this.mCheckForLeavebehind = true;
    }

    public void onExpansionStarted() {
        this.mIsExpansionChanging = true;
        this.mAmbientState.setExpansionChanging(true);
        checkSnoozeLeavebehind();
    }

    public void onExpansionStopped() {
        this.mIsExpansionChanging = false;
        resetCheckSnoozeLeavebehind();
        this.mAmbientState.setExpansionChanging(false);
        if (!this.mIsExpanded) {
            resetScrollPosition();
            this.mStatusBar.resetUserExpandedStates();
            clearTemporaryViews();
            clearUserLockedViews();
            ArrayList draggedViews = this.mAmbientState.getDraggedViews();
            if (draggedViews.size() > 0) {
                draggedViews.clear();
                updateContinuousShadowDrawing();
            }
        }
    }

    private void clearUserLockedViews() {
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) expandableView).setUserLocked(false);
            }
        }
    }

    private void clearTemporaryViews() {
        clearTemporaryViewsInGroup(this);
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView instanceof ExpandableNotificationRow) {
                clearTemporaryViewsInGroup(((ExpandableNotificationRow) expandableView).getChildrenContainer());
            }
        }
    }

    private void clearTemporaryViewsInGroup(ViewGroup viewGroup) {
        while (viewGroup != null && viewGroup.getTransientViewCount() != 0) {
            viewGroup.removeTransientView(viewGroup.getTransientView(0));
        }
    }

    public void onPanelTrackingStarted() {
        this.mPanelTracking = true;
        this.mAmbientState.setPanelTracking(true);
        resetExposedMenuView(true, true);
    }

    public void onPanelTrackingStopped() {
        this.mPanelTracking = false;
        this.mAmbientState.setPanelTracking(false);
    }

    public void resetScrollPosition() {
        this.mScroller.abortAnimation();
        setOwnScrollY(0);
    }

    private void setIsExpanded(boolean z) {
        boolean z2 = z != this.mIsExpanded;
        this.mIsExpanded = z;
        this.mStackScrollAlgorithm.setIsExpanded(z);
        this.mAmbientState.setShadeExpanded(z);
        this.mStateAnimator.setShadeExpanded(z);
        this.mSwipeHelper.setIsExpanded(z);
        if (z2) {
            if (!this.mIsExpanded) {
                this.mGroupManager.collapseAllGroups();
                this.mExpandHelper.cancelImmediately();
            }
            updateNotificationAnimationStates();
            updateChronometers();
            requestChildrenUpdate();
        }
    }

    private void updateChronometers() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            updateChronometerForChild(getChildAt(i));
        }
    }

    private void updateChronometerForChild(View view) {
        if (view instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) view).setChronometerRunning(this.mIsExpanded);
        }
    }

    public void onHeightChanged(ExpandableView expandableView, boolean z) {
        updateContentHeight();
        updateScrollPositionOnExpandInBottom(expandableView);
        clampScrollPosition();
        notifyHeightChangeListener(expandableView, z);
        ActivatableNotificationView activatableNotificationView = null;
        ExpandableNotificationRow expandableNotificationRow = expandableView instanceof ExpandableNotificationRow ? (ExpandableNotificationRow) expandableView : null;
        NotificationSection firstVisibleSection = getFirstVisibleSection();
        if (firstVisibleSection != null) {
            activatableNotificationView = firstVisibleSection.getFirstVisibleChild();
        }
        if (expandableNotificationRow != null && (expandableNotificationRow == activatableNotificationView || expandableNotificationRow.getNotificationParent() == activatableNotificationView)) {
            updateAlgorithmLayoutMinHeight();
        }
        if (z) {
            requestAnimationOnViewResize(expandableNotificationRow);
        }
        requestChildrenUpdate();
    }

    public void onReset(ExpandableView expandableView) {
        updateAnimationState(expandableView);
        updateChronometerForChild(expandableView);
    }

    private void updateScrollPositionOnExpandInBottom(ExpandableView expandableView) {
        ActivatableNotificationView activatableNotificationView;
        if ((expandableView instanceof ExpandableNotificationRow) && !onKeyguard()) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
            if (expandableNotificationRow.isUserLocked() && expandableNotificationRow != getFirstChildNotGone() && !expandableNotificationRow.isSummaryWithChildren()) {
                float translationY = expandableNotificationRow.getTranslationY() + ((float) expandableNotificationRow.getActualHeight());
                if (expandableNotificationRow.isChildInGroup()) {
                    translationY += expandableNotificationRow.getNotificationParent().getTranslationY();
                }
                int i = this.mMaxLayoutHeight + ((int) this.mStackTranslation);
                NotificationSection lastVisibleSection = getLastVisibleSection();
                if (lastVisibleSection == null) {
                    activatableNotificationView = null;
                } else {
                    activatableNotificationView = lastVisibleSection.getLastVisibleChild();
                }
                if (!(expandableNotificationRow == activatableNotificationView || this.mShelf.getVisibility() == 8)) {
                    i -= this.mShelf.getIntrinsicHeight() + this.mPaddingBetweenElements;
                }
                float f = (float) i;
                if (translationY > f) {
                    setOwnScrollY((int) ((((float) this.mOwnScrollY) + translationY) - f));
                    this.mDisallowScrollingInThisMotion = true;
                }
            }
        }
    }

    public void setOnHeightChangedListener(OnHeightChangedListener onHeightChangedListener) {
        this.mOnHeightChangedListener = onHeightChangedListener;
    }

    public void onChildAnimationFinished() {
        setAnimationRunning(false);
        requestChildrenUpdate();
        runAnimationFinishedRunnables();
        clearTransient();
        clearHeadsUpDisappearRunning();
    }

    private void clearHeadsUpDisappearRunning() {
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            if (childAt instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) childAt;
                expandableNotificationRow.setHeadsUpAnimatingAway(false);
                if (expandableNotificationRow.isSummaryWithChildren()) {
                    for (ExpandableNotificationRow headsUpAnimatingAway : expandableNotificationRow.getNotificationChildren()) {
                        headsUpAnimatingAway.setHeadsUpAnimatingAway(false);
                    }
                }
            }
        }
    }

    private void clearTransient() {
        Iterator it = this.mClearTransientViewsWhenFinished.iterator();
        while (it.hasNext()) {
            StackStateAnimator.removeTransientView((ExpandableView) it.next());
        }
        this.mClearTransientViewsWhenFinished.clear();
    }

    private void runAnimationFinishedRunnables() {
        Iterator it = this.mAnimationFinishedRunnables.iterator();
        while (it.hasNext()) {
            ((Runnable) it.next()).run();
        }
        this.mAnimationFinishedRunnables.clear();
    }

    public void setDimmed(boolean z, boolean z2) {
        boolean onKeyguard = z & onKeyguard();
        this.mAmbientState.setDimmed(onKeyguard);
        if (!z2 || !this.mAnimationsEnabled) {
            setDimAmount(onKeyguard ? 1.0f : 0.0f);
        } else {
            this.mDimmedNeedsAnimation = true;
            this.mNeedsAnimation = true;
            animateDimmed(onKeyguard);
        }
        requestChildrenUpdate();
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public boolean isDimmed() {
        return this.mAmbientState.isDimmed();
    }

    /* access modifiers changed from: private */
    public void setDimAmount(float f) {
        this.mDimAmount = f;
        updateBackgroundDimming();
    }

    private void animateDimmed(boolean z) {
        ValueAnimator valueAnimator = this.mDimAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        float f = z ? 1.0f : 0.0f;
        float f2 = this.mDimAmount;
        if (f != f2) {
            this.mDimAnimator = TimeAnimator.ofFloat(new float[]{f2, f});
            this.mDimAnimator.setDuration(220);
            this.mDimAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            this.mDimAnimator.addListener(this.mDimEndListener);
            this.mDimAnimator.addUpdateListener(this.mDimUpdateListener);
            this.mDimAnimator.start();
        }
    }

    public void setHideSensitive(boolean z, boolean z2) {
        if (z != this.mAmbientState.isHideSensitive()) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                ExpandableView expandableView = (ExpandableView) getChildAt(i);
                expandableView.setHideSensitiveForIntrinsicHeight(shouldHideSensitive(expandableView, z));
            }
            this.mAmbientState.setHideSensitive(z);
            if (z2 && this.mAnimationsEnabled) {
                this.mHideSensitiveNeedsAnimation = true;
                this.mNeedsAnimation = true;
            }
            updateContentHeight();
            requestChildrenUpdate();
        }
    }

    public void setActivatedChild(ActivatableNotificationView activatableNotificationView) {
        this.mAmbientState.setActivatedChild(activatableNotificationView);
        if (this.mAnimationsEnabled) {
            this.mActivateNeedsAnimation = true;
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    public ActivatableNotificationView getActivatedChild() {
        return this.mAmbientState.getActivatedChild();
    }

    private void applyCurrentState() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((ExpandableView) getChildAt(i)).applyViewState();
        }
        OnChildLocationsChangedListener onChildLocationsChangedListener = this.mListener;
        if (onChildLocationsChangedListener != null) {
            onChildLocationsChangedListener.onChildLocationsChanged();
        }
        runAnimationFinishedRunnables();
        setAnimationRunning(false);
        updateBackground();
        updateViewShadows();
        updateClippingToTopRoundedCorner();
    }

    /* access modifiers changed from: private */
    public void updateViewShadows() {
        float f;
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() != 8) {
                this.mTmpSortedChildren.add(expandableView);
            }
        }
        Collections.sort(this.mTmpSortedChildren, this.mViewPositionComparator);
        ExpandableView expandableView2 = null;
        int i2 = 0;
        while (i2 < this.mTmpSortedChildren.size()) {
            ExpandableView expandableView3 = (ExpandableView) this.mTmpSortedChildren.get(i2);
            float translationZ = expandableView3.getTranslationZ();
            if (expandableView2 == null) {
                f = translationZ;
            } else {
                f = expandableView2.getTranslationZ();
            }
            float f2 = f - translationZ;
            if (f2 <= 0.0f || f2 >= 0.1f) {
                expandableView3.setFakeShadowIntensity(0.0f, 0.0f, 0, 0);
            } else {
                expandableView3.setFakeShadowIntensity(f2 / 0.1f, expandableView2.getOutlineAlpha(), (int) (((expandableView2.getTranslationY() + ((float) expandableView2.getActualHeight())) - expandableView3.getTranslationY()) - ((float) expandableView2.getExtraBottomPadding())), expandableView2.getOutlineTranslation());
            }
            i2++;
            expandableView2 = expandableView3;
        }
        this.mTmpSortedChildren.clear();
    }

    public void updateDecorViews(boolean z) {
        if (z != this.mUsingLightTheme || !this.mInited) {
            this.mInited = true;
            this.mUsingLightTheme = z;
            int colorAttrDefaultColor = Utils.getColorAttrDefaultColor(new ContextThemeWrapper(this.mContext, z ? R$style.Theme_SystemUI_Light : R$style.Theme_SystemUI), R$attr.wallpaperTextColor);
            this.mFooterView.setTextColor(colorAttrDefaultColor);
            this.mEmptyShadeView.setTextColor(colorAttrDefaultColor);
        }
    }

    public void goToFullShade(long j) {
        this.mGoToFullShadeNeedsAnimation = true;
        this.mGoToFullShadeDelay = j;
        this.mNeedsAnimation = true;
        requestChildrenUpdate();
    }

    public void cancelExpandHelper() {
        this.mExpandHelper.cancel();
    }

    public void setIntrinsicPadding(int i) {
        this.mIntrinsicPadding = i;
        this.mAmbientState.setIntrinsicPadding(i);
    }

    public int getIntrinsicPadding() {
        return this.mIntrinsicPadding;
    }

    public float getNotificationsTopY() {
        return ((float) this.mTopPadding) + getStackTranslation();
    }

    public void setDark(boolean z, boolean z2, PointF pointF) {
        if (this.mAmbientState.isDark() != z) {
            this.mAmbientState.setDark(z);
            if (!z2 || !this.mAnimationsEnabled) {
                setDarkAmount(z ? 1.0f : 0.0f);
                updateBackground();
            } else {
                this.mDarkNeedsAnimation = true;
                this.mDarkAnimationOriginIndex = findDarkAnimationOriginIndex(pointF);
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
            updateWillNotDraw();
            notifyHeightChangeListener(this.mShelf);
        }
    }

    private void updatePanelTranslation() {
        setTranslationX(this.mHorizontalPanelTranslation + (((float) this.mAntiBurnInOffsetX) * this.mInterpolatedDarkAmount));
    }

    public void setHorizontalPanelTranslation(float f) {
        this.mHorizontalPanelTranslation = f;
        updatePanelTranslation();
    }

    private void updateWillNotDraw() {
        setWillNotDraw(!(this.mShouldDrawNotificationBackground));
    }

    private void setDarkAmount(float f) {
        setDarkAmount(f, f);
    }

    public void setDarkAmount(float f, float f2) {
        this.mLinearDarkAmount = f;
        this.mInterpolatedDarkAmount = f2;
        boolean isFullyDark = this.mAmbientState.isFullyDark();
        boolean isDarkAtAll = this.mAmbientState.isDarkAtAll();
        this.mAmbientState.setDarkAmount(f2);
        boolean isFullyDark2 = this.mAmbientState.isFullyDark();
        boolean isDarkAtAll2 = this.mAmbientState.isDarkAtAll();
        if (isFullyDark2 != isFullyDark) {
            updateContentHeight();
            if (isFullyDark2) {
                updateDarkShelfVisibility();
            }
        }
        if (!isDarkAtAll && isDarkAtAll2) {
            resetExposedMenuView(true, true);
        }
        if (!(isFullyDark2 == isFullyDark && isDarkAtAll == isDarkAtAll2)) {
            invalidateOutline();
        }
        updateAlgorithmHeightAndPadding();
        updateBackgroundDimming();
        updatePanelTranslation();
        requestChildrenUpdate();
    }

    private void updateDarkShelfVisibility() {
        if (DozeParameters.getInstance(this.mContext).shouldControlScreenOff()) {
            this.mShelf.fadeInTranslating();
        }
        updateClipping();
    }

    public void notifyDarkAnimationStart(boolean z) {
        Interpolator interpolator;
        float f = this.mInterpolatedDarkAmount;
        if (f == 0.0f || f == 1.0f) {
            this.mBackgroundXFactor = z ? 1.8f : 1.5f;
            if (z) {
                interpolator = Interpolators.FAST_OUT_SLOW_IN_REVERSE;
            } else {
                interpolator = Interpolators.FAST_OUT_SLOW_IN;
            }
            this.mDarkXInterpolator = interpolator;
        }
    }

    private int findDarkAnimationOriginIndex(PointF pointF) {
        if (pointF != null) {
            float f = pointF.y;
            if (f >= ((float) this.mTopPadding)) {
                if (f > getBottomMostNotificationBottom()) {
                    return -2;
                }
                ExpandableView closestChildAtRawPosition = getClosestChildAtRawPosition(pointF.x, pointF.y);
                if (closestChildAtRawPosition != null) {
                    return getNotGoneIndex(closestChildAtRawPosition);
                }
            }
        }
        return -1;
    }

    private int getNotGoneIndex(View view) {
        int childCount = getChildCount();
        int i = 0;
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = getChildAt(i2);
            if (view == childAt) {
                return i;
            }
            if (childAt.getVisibility() != 8) {
                i++;
            }
        }
        return -1;
    }

    public void setFooterView(FooterView footerView) {
        int i;
        FooterView footerView2 = this.mFooterView;
        if (footerView2 != null) {
            i = indexOfChild(footerView2);
            removeView(this.mFooterView);
        } else {
            i = -1;
        }
        this.mFooterView = footerView;
        addView(this.mFooterView, i);
    }

    public void setEmptyShadeView(EmptyShadeView emptyShadeView) {
        int i;
        EmptyShadeView emptyShadeView2 = this.mEmptyShadeView;
        if (emptyShadeView2 != null) {
            i = indexOfChild(emptyShadeView2);
            removeView(this.mEmptyShadeView);
        } else {
            i = -1;
        }
        this.mEmptyShadeView = emptyShadeView;
        addView(this.mEmptyShadeView, i);
    }

    public void updateEmptyShadeView(boolean z) {
        this.mEmptyShadeView.setVisible(z, this.mIsExpanded && this.mAnimationsEnabled);
        int textResource = this.mEmptyShadeView.getTextResource();
        int i = this.mStatusBar.areNotificationsHidden() ? R$string.dnd_suppressing_shade_text : R$string.empty_shade_text;
        if (textResource != i) {
            this.mEmptyShadeView.setText(i);
        }
    }

    public void updateFooterView(boolean z, boolean z2) {
        if (this.mFooterView != null) {
            boolean z3 = this.mIsExpanded && this.mAnimationsEnabled;
            this.mFooterView.setVisible(z, z3);
            this.mFooterView.setSecondaryVisible(z2, z3);
        }
    }

    public void setDismissAllInProgress(boolean z) {
        this.mDismissAllInProgress = z;
        this.mAmbientState.setDismissAllInProgress(z);
        handleDismissAllClipping();
    }

    private void handleDismissAllClipping() {
        int childCount = getChildCount();
        boolean z = false;
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() != 8) {
                if (!this.mDismissAllInProgress || !z) {
                    expandableView.setMinClipTopAmount(0);
                } else {
                    expandableView.setMinClipTopAmount(expandableView.getClipTopAmount());
                }
                z = StackScrollAlgorithm.canChildBeDismissed(expandableView);
            }
        }
    }

    public boolean isFooterViewNotGone() {
        FooterView footerView = this.mFooterView;
        return (footerView == null || footerView.getVisibility() == 8 || this.mFooterView.willBeGone()) ? false : true;
    }

    public boolean isFooterViewContentVisible() {
        FooterView footerView = this.mFooterView;
        return footerView != null && footerView.isContentVisible();
    }

    public int getFooterViewHeight() {
        FooterView footerView = this.mFooterView;
        if (footerView == null) {
            return 0;
        }
        return this.mPaddingBetweenElements + footerView.getHeight();
    }

    public int getEmptyShadeViewHeight() {
        return this.mEmptyShadeView.getHeight();
    }

    public float getBottomMostNotificationBottom() {
        int childCount = getChildCount();
        float f = 0.0f;
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() != 8) {
                float translationY = (expandableView.getTranslationY() + ((float) expandableView.getActualHeight())) - ((float) expandableView.getClipBottomAmount());
                if (translationY > f) {
                    f = translationY;
                }
            }
        }
        return f + getStackTranslation();
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
    }

    public void setGroupManager(NotificationGroupManager notificationGroupManager) {
        this.mGroupManager = notificationGroupManager;
        this.mGroupManager.addOnGroupChangeListener(this.mOnGroupChangeListener);
    }

    /* access modifiers changed from: private */
    public void requestAnimateEverything() {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mEverythingNeedsAnimation = true;
            this.mNeedsAnimation = true;
            requestChildrenUpdate();
        }
    }

    public boolean isBelowLastNotification(float f, float f2) {
        boolean z = true;
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            ExpandableView expandableView = (ExpandableView) getChildAt(childCount);
            if (expandableView.getVisibility() != 8) {
                float y = expandableView.getY();
                if (y > f2) {
                    return false;
                }
                boolean z2 = f2 > (((float) expandableView.getActualHeight()) + y) - ((float) expandableView.getClipBottomAmount());
                FooterView footerView = this.mFooterView;
                if (expandableView == footerView) {
                    if (!z2 && !footerView.isOnEmptySpace(f - footerView.getX(), f2 - y)) {
                        return false;
                    }
                } else if (expandableView == this.mEmptyShadeView) {
                    return true;
                } else {
                    if (!z2) {
                        return false;
                    }
                }
            }
        }
        if (f2 <= ((float) this.mTopPadding) + this.mStackTranslation) {
            z = false;
        }
        return z;
    }

    public void onInitializeAccessibilityEventInternal(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEventInternal(accessibilityEvent);
        accessibilityEvent.setScrollable(this.mScrollable);
        accessibilityEvent.setScrollX(this.mScrollX);
        accessibilityEvent.setMaxScrollX(this.mScrollX);
        accessibilityEvent.setScrollY(this.mOwnScrollY);
        accessibilityEvent.setMaxScrollY(getScrollRange());
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
        if (this.mScrollable) {
            accessibilityNodeInfo.setScrollable(true);
            if (this.mBackwardScrollable) {
                accessibilityNodeInfo.addAction(AccessibilityAction.ACTION_SCROLL_BACKWARD);
                accessibilityNodeInfo.addAction(AccessibilityAction.ACTION_SCROLL_UP);
            }
            if (this.mForwardScrollable) {
                accessibilityNodeInfo.addAction(AccessibilityAction.ACTION_SCROLL_FORWARD);
                accessibilityNodeInfo.addAction(AccessibilityAction.ACTION_SCROLL_DOWN);
            }
        }
        accessibilityNodeInfo.setClassName(ScrollView.class.getName());
    }

    public void generateChildOrderChangedEvent() {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mGenerateChildOrderChangedEvent = true;
            this.mNeedsAnimation = true;
            requestChildrenUpdate();
        }
    }

    public int getContainerChildCount() {
        return getChildCount();
    }

    public View getContainerChildAt(int i) {
        return getChildAt(i);
    }

    public void removeContainerView(View view) {
        Assert.isMainThread();
        removeView(view);
    }

    public void addContainerView(View view) {
        Assert.isMainThread();
        addView(view);
    }

    public void runAfterAnimationFinished(Runnable runnable) {
        this.mAnimationFinishedRunnables.add(runnable);
    }

    public void setHeadsUpManager(HeadsUpManagerPhone headsUpManagerPhone) {
        this.mHeadsUpManager = headsUpManagerPhone;
        this.mHeadsUpManager.addListener(this.mRoundnessManager);
        this.mHeadsUpManager.setAnimationStateHandler(new AnimationStateHandler() {
            public final void setHeadsUpGoingAwayAnimationsAllowed(boolean z) {
                NotificationStackScrollLayout.this.setHeadsUpGoingAwayAnimationsAllowed(z);
            }
        });
    }

    public void generateHeadsUpAnimation(NotificationEntry notificationEntry, boolean z) {
        generateHeadsUpAnimation(notificationEntry.getHeadsUpAnimationView(), z);
    }

    public void generateHeadsUpAnimation(ExpandableNotificationRow expandableNotificationRow, boolean z) {
        if (!this.mAnimationsEnabled) {
            return;
        }
        if (z || this.mHeadsUpGoingAwayAnimationsAllowed) {
            this.mHeadsUpChangeAnimations.add(new Pair(expandableNotificationRow, Boolean.valueOf(z)));
            this.mNeedsAnimation = true;
            if (!this.mIsExpanded && !z) {
                expandableNotificationRow.setHeadsUpAnimatingAway(true);
            }
            requestChildrenUpdate();
        }
    }

    public void setHeadsUpBoundaries(int i, int i2) {
        this.mAmbientState.setMaxHeadsUpTranslation((float) (i - i2));
        this.mStateAnimator.setHeadsUpAppearHeightBottom(i);
        requestChildrenUpdate();
    }

    public void setTrackingHeadsUp(ExpandableNotificationRow expandableNotificationRow) {
        this.mTrackingHeadsUp = expandableNotificationRow != null;
        this.mRoundnessManager.setTrackingHeadsUp(expandableNotificationRow);
    }

    public void setScrimController(ScrimController scrimController) {
        this.mScrimController = scrimController;
        this.mScrimController.setScrimBehindChangeRunnable(new Runnable() {
            public final void run() {
                NotificationStackScrollLayout.this.updateBackgroundDimming();
            }
        });
    }

    public void forceNoOverlappingRendering(boolean z) {
        this.mForceNoOverlappingRendering = z;
    }

    public boolean hasOverlappingRendering() {
        return !this.mForceNoOverlappingRendering && super.hasOverlappingRendering();
    }

    public void setAnimationRunning(boolean z) {
        if (z != this.mAnimationRunning) {
            if (z) {
                getViewTreeObserver().addOnPreDrawListener(this.mRunningAnimationUpdater);
            } else {
                getViewTreeObserver().removeOnPreDrawListener(this.mRunningAnimationUpdater);
            }
            this.mAnimationRunning = z;
            updateContinuousShadowDrawing();
        }
    }

    public boolean isExpanded() {
        return this.mIsExpanded;
    }

    public void setPulsing(boolean z, boolean z2) {
        if (this.mPulsing || z) {
            this.mPulsing = z;
            updateClipping();
            this.mAmbientState.setPulsing(z);
            this.mSwipeHelper.setPulsing(z);
            updateNotificationAnimationStates();
            updateAlgorithmHeightAndPadding();
            updateContentHeight();
            requestChildrenUpdate();
            notifyHeightChangeListener(null, z2);
        }
    }

    public void setQsExpanded(boolean z) {
        this.mQsExpanded = z;
        updateAlgorithmLayoutMinHeight();
        updateScrollability();
    }

    public void setQsExpansionFraction(float f) {
        this.mQsExpansionFraction = f;
    }

    private void setOwnScrollY(int i) {
        if (i != this.mOwnScrollY) {
            onScrollChanged(this.mScrollX, i, this.mScrollX, this.mOwnScrollY);
            this.mOwnScrollY = i;
            updateOnScrollChange();
        }
    }

    private void updateOnScrollChange() {
        updateForwardAndBackwardScrollability();
        requestChildrenUpdate();
    }

    public void setShelf(NotificationShelf notificationShelf) {
        int i;
        NotificationShelf notificationShelf2 = this.mShelf;
        if (notificationShelf2 != null) {
            i = indexOfChild(notificationShelf2);
            removeView(this.mShelf);
        } else {
            i = -1;
        }
        this.mShelf = notificationShelf;
        addView(this.mShelf, i);
        this.mAmbientState.setShelf(notificationShelf);
        this.mStateAnimator.setShelf(notificationShelf);
        notificationShelf.bind(this.mAmbientState, this);
    }

    public NotificationShelf getNotificationShelf() {
        return this.mShelf;
    }

    public void setMaxDisplayedNotifications(int i) {
        if (this.mMaxDisplayedNotifications != i) {
            this.mMaxDisplayedNotifications = i;
            updateContentHeight();
            notifyHeightChangeListener(this.mShelf);
        }
    }

    public void setShouldShowShelfOnly(boolean z) {
        this.mShouldShowShelfOnly = z;
        updateAlgorithmLayoutMinHeight();
    }

    public int getMinExpansionHeight() {
        return this.mShelf.getIntrinsicHeight() - ((this.mShelf.getIntrinsicHeight() - this.mStatusBarHeight) / 2);
    }

    public void setInHeadsUpPinnedMode(boolean z) {
        this.mInHeadsUpPinnedMode = z;
        updateClipping();
    }

    public void setHeadsUpAnimatingAway(boolean z) {
        this.mHeadsUpAnimatingAway = z;
        updateClipping();
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void setStatusBarState(int i) {
        this.mStatusBarState = i;
        this.mAmbientState.setStatusBarState(i);
    }

    /* access modifiers changed from: private */
    public void onStatePostChange() {
        boolean onKeyguard = onKeyguard();
        boolean z = this.mLockscreenUserManager.isAnyProfilePublicMode() || this.mStatusBar.getViewHierarchyManager().isAnyNotificationLocked();
        HeadsUpAppearanceController headsUpAppearanceController = this.mHeadsUpAppearanceController;
        if (headsUpAppearanceController != null) {
            headsUpAppearanceController.setPublicMode(z);
        }
        SysuiStatusBarStateController sysuiStatusBarStateController = (SysuiStatusBarStateController) Dependency.get(StatusBarStateController.class);
        setHideSensitive(z, sysuiStatusBarStateController.goingToFullShade());
        setDimmed(onKeyguard, sysuiStatusBarStateController.fromShadeLocked());
        setExpandingEnabled(!onKeyguard);
        ActivatableNotificationView activatedChild = getActivatedChild();
        setActivatedChild(null);
        if (activatedChild != null) {
            activatedChild.makeInactive(false);
        }
        updateFooter();
        requestChildrenUpdate();
        onUpdateRowStates();
        this.mEntryManager.updateNotifications();
    }

    public void setExpandingVelocity(float f) {
        this.mAmbientState.setExpandingVelocity(f);
    }

    public float getOpeningHeight() {
        if (this.mEmptyShadeView.getVisibility() == 8) {
            return (float) getMinExpansionHeight();
        }
        return getAppearEndPosition();
    }

    public void setIsFullWidth(boolean z) {
        this.mAmbientState.setPanelFullWidth(z);
    }

    public void setUnlockHintRunning(boolean z) {
        this.mAmbientState.setUnlockHintRunning(z);
    }

    public void setQsCustomizerShowing(boolean z) {
        this.mAmbientState.setQsCustomizerShowing(z);
        requestChildrenUpdate();
    }

    public void setHeadsUpGoingAwayAnimationsAllowed(boolean z) {
        this.mHeadsUpGoingAwayAnimationsAllowed = z;
    }

    public void setAntiBurnInOffsetX(int i) {
        this.mAntiBurnInOffsetX = i;
        updatePanelTranslation();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        Object[] objArr = new Object[9];
        objArr[0] = NotificationStackScrollLayout.class.getSimpleName();
        String str = "T";
        String str2 = "f";
        objArr[1] = this.mPulsing ? str : str2;
        objArr[2] = this.mAmbientState.isQsCustomizerShowing() ? str : str2;
        String str3 = getVisibility() == 0 ? "visible" : getVisibility() == 8 ? "gone" : "invisible";
        objArr[3] = str3;
        objArr[4] = Float.valueOf(getAlpha());
        objArr[5] = Integer.valueOf(this.mAmbientState.getScrollY());
        objArr[6] = Integer.valueOf(this.mMaxTopPadding);
        if (!this.mShouldShowShelfOnly) {
            str = str2;
        }
        objArr[7] = str;
        objArr[8] = Float.valueOf(this.mQsExpansionFraction);
        printWriter.println(String.format("[%s: pulsing=%s qsCustomizerShowing=%s visibility=%s alpha:%f scrollY:%d maxTopPadding:%d showShelfOnly=%s qsExpandFraction=%f]", objArr));
        int childCount = getChildCount();
        StringBuilder sb = new StringBuilder();
        sb.append("  Number of children: ");
        sb.append(childCount);
        printWriter.println(sb.toString());
        printWriter.println();
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            expandableView.dump(fileDescriptor, printWriter, strArr);
            if (!(expandableView instanceof ExpandableNotificationRow)) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("  ");
                sb2.append(expandableView.getClass().getSimpleName());
                printWriter.println(sb2.toString());
                ExpandableViewState viewState = expandableView.getViewState();
                if (viewState == null) {
                    printWriter.println("    no viewState!!!");
                } else {
                    printWriter.print("    ");
                    viewState.dump(fileDescriptor, printWriter, strArr);
                    printWriter.println();
                    printWriter.println();
                }
            }
        }
        int transientViewCount = getTransientViewCount();
        StringBuilder sb3 = new StringBuilder();
        sb3.append("  Transient Views: ");
        sb3.append(transientViewCount);
        printWriter.println(sb3.toString());
        for (int i2 = 0; i2 < transientViewCount; i2++) {
            ((ExpandableView) getTransientView(i2)).dump(fileDescriptor, printWriter, strArr);
        }
        ArrayList draggedViews = this.mAmbientState.getDraggedViews();
        int size = draggedViews.size();
        StringBuilder sb4 = new StringBuilder();
        sb4.append("  Dragged Views: ");
        sb4.append(size);
        printWriter.println(sb4.toString());
        for (int i3 = 0; i3 < size; i3++) {
            ((ExpandableView) draggedViews.get(i3)).dump(fileDescriptor, printWriter, strArr);
        }
        StringBuilder sb5 = new StringBuilder();
        sb5.append("  mShouldDrawNotificationBackground: ");
        sb5.append(this.mShouldDrawNotificationBackground);
        printWriter.println(sb5.toString());
        StringBuilder sb6 = new StringBuilder();
        sb6.append("  mBgColor: ");
        sb6.append(this.mBgColor);
        printWriter.println(sb6.toString());
        StringBuilder sb7 = new StringBuilder();
        sb7.append("  mCachedBackgroundColor: ");
        sb7.append(this.mCachedBackgroundColor);
        printWriter.println(sb7.toString());
    }

    public void addOnExpandedHeightListener(BiConsumer<Float, Float> biConsumer) {
        this.mExpandedHeightListeners.add(biConsumer);
    }

    public void removeOnExpandedHeightListener(BiConsumer<Float, Float> biConsumer) {
        this.mExpandedHeightListeners.remove(biConsumer);
    }

    public void setHeadsUpAppearanceController(HeadsUpAppearanceController headsUpAppearanceController) {
        this.mHeadsUpAppearanceController = headsUpAppearanceController;
    }

    public void setIconAreaController(NotificationIconAreaController notificationIconAreaController) {
        this.mIconAreaController = notificationIconAreaController;
    }

    public void manageNotifications(View view) {
        this.mStatusBar.startActivity(new Intent("android.settings.ALL_APPS_NOTIFICATION_SETTINGS"), true, true, 536870912);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:16:0x004f, code lost:
        if (r11.mTmpRect.height() > 0) goto L_0x0053;
     */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0059  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x0094 A[SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void clearNotifications(int r12, boolean r13) {
        /*
            r11 = this;
            int r0 = r11.getChildCount()
            java.util.ArrayList r1 = new java.util.ArrayList
            r1.<init>(r0)
            java.util.ArrayList r2 = new java.util.ArrayList
            r2.<init>(r0)
            r3 = 0
            r4 = r3
        L_0x0010:
            if (r4 >= r0) goto L_0x0098
            android.view.View r5 = r11.getChildAt(r4)
            boolean r6 = r5 instanceof com.android.systemui.statusbar.notification.row.ExpandableNotificationRow
            if (r6 == 0) goto L_0x0094
            r6 = r5
            com.android.systemui.statusbar.notification.row.ExpandableNotificationRow r6 = (com.android.systemui.statusbar.notification.row.ExpandableNotificationRow) r6
            android.graphics.Rect r7 = r11.mTmpRect
            boolean r7 = r5.getClipBounds(r7)
            boolean r8 = r11.includeChildInDismissAll(r6, r12)
            r9 = 1
            if (r8 == 0) goto L_0x0041
            r2.add(r6)
            int r8 = r5.getVisibility()
            if (r8 != 0) goto L_0x0052
            if (r7 == 0) goto L_0x003d
            android.graphics.Rect r7 = r11.mTmpRect
            int r7 = r7.height()
            if (r7 <= 0) goto L_0x0052
        L_0x003d:
            r1.add(r5)
            goto L_0x0053
        L_0x0041:
            int r5 = r5.getVisibility()
            if (r5 != 0) goto L_0x0052
            if (r7 == 0) goto L_0x0053
            android.graphics.Rect r5 = r11.mTmpRect
            int r5 = r5.height()
            if (r5 <= 0) goto L_0x0052
            goto L_0x0053
        L_0x0052:
            r9 = r3
        L_0x0053:
            java.util.List r5 = r6.getNotificationChildren()
            if (r5 == 0) goto L_0x0094
            java.util.Iterator r5 = r5.iterator()
        L_0x005d:
            boolean r7 = r5.hasNext()
            if (r7 == 0) goto L_0x0094
            java.lang.Object r7 = r5.next()
            com.android.systemui.statusbar.notification.row.ExpandableNotificationRow r7 = (com.android.systemui.statusbar.notification.row.ExpandableNotificationRow) r7
            boolean r8 = r11.includeChildInDismissAll(r6, r12)
            if (r8 == 0) goto L_0x005d
            r2.add(r7)
            if (r9 == 0) goto L_0x005d
            boolean r8 = r6.areChildrenExpanded()
            if (r8 == 0) goto L_0x005d
            android.graphics.Rect r8 = r11.mTmpRect
            boolean r8 = r7.getClipBounds(r8)
            int r10 = r7.getVisibility()
            if (r10 != 0) goto L_0x005d
            if (r8 == 0) goto L_0x0090
            android.graphics.Rect r8 = r11.mTmpRect
            int r8 = r8.height()
            if (r8 <= 0) goto L_0x005d
        L_0x0090:
            r1.add(r7)
            goto L_0x005d
        L_0x0094:
            int r4 = r4 + 1
            goto L_0x0010
        L_0x0098:
            boolean r0 = r2.isEmpty()
            if (r0 == 0) goto L_0x00a6
            if (r13 == 0) goto L_0x00a5
            com.android.systemui.statusbar.phone.StatusBar r11 = r11.mStatusBar
            r11.animateCollapsePanels(r3)
        L_0x00a5:
            return
        L_0x00a6:
            com.android.systemui.statusbar.notification.stack.-$$Lambda$NotificationStackScrollLayout$1JoW9tMXjFe-6yv5uN3FfACI74A r0 = new com.android.systemui.statusbar.notification.stack.-$$Lambda$NotificationStackScrollLayout$1JoW9tMXjFe-6yv5uN3FfACI74A
            r0.<init>(r2, r12)
            r11.performDismissAllAnimations(r1, r13, r0)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout.clearNotifications(int, boolean):void");
    }

    public /* synthetic */ void lambda$clearNotifications$6$NotificationStackScrollLayout(ArrayList arrayList, int i) {
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) it.next();
            if (!StackScrollAlgorithm.canChildBeDismissed(expandableNotificationRow)) {
                expandableNotificationRow.resetTranslation();
            } else if (i == 0) {
                this.mEntryManager.removeNotification(expandableNotificationRow.getEntry().key, null, 3);
            } else {
                this.mEntryManager.performRemoveNotification(expandableNotificationRow.getEntry().notification, 3);
            }
        }
        if (i == 0) {
            try {
                this.mBarService.onClearAllNotifications(this.mLockscreenUserManager.getCurrentUserId());
            } catch (Exception unused) {
            }
        }
    }

    private boolean includeChildInDismissAll(ExpandableNotificationRow expandableNotificationRow, int i) {
        return StackScrollAlgorithm.canChildBeDismissed(expandableNotificationRow) && matchesSelection(expandableNotificationRow, i);
    }

    private void performDismissAllAnimations(ArrayList<View> arrayList, boolean z, Runnable runnable) {
        C1288x1738711c r0 = new Runnable(z, runnable) {
            private final /* synthetic */ boolean f$1;
            private final /* synthetic */ Runnable f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                NotificationStackScrollLayout.this.mo16739x18812a4e(this.f$1, this.f$2);
            }
        };
        if (arrayList.isEmpty()) {
            r0.run();
            return;
        }
        setDismissAllInProgress(true);
        int i = 140;
        int i2 = 180;
        int size = arrayList.size() - 1;
        while (size >= 0) {
            dismissViewAnimated((View) arrayList.get(size), size == 0 ? r0 : null, i2, 260);
            i = Math.max(50, i - 10);
            i2 += i;
            size--;
        }
    }

    /* renamed from: lambda$performDismissAllAnimations$8$NotificationStackScrollLayout */
    public /* synthetic */ void mo16739x18812a4e(boolean z, Runnable runnable) {
        if (z) {
            this.mShadeController.addPostCollapseAction(new Runnable(runnable) {
                private final /* synthetic */ Runnable f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    NotificationStackScrollLayout.this.mo16738x398620d(this.f$1);
                }
            });
            this.mStatusBar.animateCollapsePanels(0);
            return;
        }
        setDismissAllInProgress(false);
        runnable.run();
    }

    /* renamed from: lambda$performDismissAllAnimations$7$NotificationStackScrollLayout */
    public /* synthetic */ void mo16738x398620d(Runnable runnable) {
        setDismissAllInProgress(false);
        runnable.run();
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void inflateFooterView() {
        FooterView footerView = (FooterView) LayoutInflater.from(this.mContext).inflate(R$layout.status_bar_notification_footer, this, false);
        footerView.setDismissButtonClickListener(new OnClickListener() {
            public final void onClick(View view) {
                NotificationStackScrollLayout.this.lambda$inflateFooterView$9$NotificationStackScrollLayout(view);
            }
        });
        footerView.setManageButtonClickListener(new OnClickListener() {
            public final void onClick(View view) {
                NotificationStackScrollLayout.this.manageNotifications(view);
            }
        });
        setFooterView(footerView);
        if (OpLsState.getInstance().getContainer() != null) {
            this.mDismissAllButton = OpLsState.getInstance().getContainer().findViewById(R$id.clear_notifications);
            this.mDismissAllButton.setOnClickListener(new OnClickListener() {
                public final void onClick(View view) {
                    NotificationStackScrollLayout.this.lambda$inflateFooterView$10$NotificationStackScrollLayout(view);
                }
            });
        }
    }

    public /* synthetic */ void lambda$inflateFooterView$9$NotificationStackScrollLayout(View view) {
        this.mMetricsLogger.action(148);
        clearNotifications(0, true);
    }

    public /* synthetic */ void lambda$inflateFooterView$10$NotificationStackScrollLayout(View view) {
        this.mMetricsLogger.action(148);
        clearNotifications(0, true);
    }

    private void inflateEmptyShadeView() {
        EmptyShadeView emptyShadeView = (EmptyShadeView) LayoutInflater.from(this.mContext).inflate(R$layout.status_bar_no_notifications, this, false);
        emptyShadeView.setText(R$string.empty_shade_text);
        setEmptyShadeView(emptyShadeView);
    }

    public void onUpdateRowStates() {
        changeViewPosition(this.mFooterView, -1);
        changeViewPosition(this.mEmptyShadeView, getChildCount() - 1);
        changeViewPosition(this.mShelf, getChildCount() - 2);
    }

    public void setNotificationPanel(NotificationPanelView notificationPanelView) {
        this.mNotificationPanel = notificationPanelView;
    }

    public void updateIconAreaViews() {
        this.mIconAreaController.updateNotificationIcons();
    }

    public float setPulseHeight(float f) {
        this.mAmbientState.setPulseHeight(f);
        requestChildrenUpdate();
        return Math.max(0.0f, f - ((float) this.mAmbientState.getInnerHeight(true)));
    }

    public void setDozeAmount(float f) {
        this.mAmbientState.setDozeAmount(f);
        updateContinuousBackgroundDrawing();
        requestChildrenUpdate();
    }

    public void wakeUpFromPulse() {
        setPulseHeight(getPulseHeight());
        int childCount = getChildCount();
        boolean z = true;
        float f = -1.0f;
        for (int i = 0; i < childCount; i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() != 8) {
                boolean z2 = expandableView == this.mShelf;
                if ((expandableView instanceof ExpandableNotificationRow) || z2) {
                    if (expandableView.getVisibility() != 0 || z2) {
                        if (!z) {
                            expandableView.setTranslationY(f);
                        }
                    } else if (z) {
                        f = (expandableView.getTranslationY() + ((float) expandableView.getActualHeight())) - ((float) this.mShelf.getIntrinsicHeight());
                        z = false;
                    }
                }
            }
        }
        this.mDimmedNeedsAnimation = true;
    }

    public void onDynamicPrivacyChanged() {
        if (this.mIsExpanded) {
            this.mAnimateBottomOnLayout = true;
        }
    }

    public boolean hasActiveNotifications() {
        return !this.mEntryManager.getNotificationData().getActiveNotifications().isEmpty();
    }

    public void updateSpeedBumpIndex() {
        boolean z;
        int childCount = getChildCount();
        boolean z2 = false;
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt.getVisibility() != 8 && (childAt instanceof ExpandableNotificationRow)) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) childAt;
                i2++;
                if (this.mHighPriorityBeforeSpeedBump) {
                    z = expandableNotificationRow.getEntry().isHighPriority();
                } else {
                    z = true ^ expandableNotificationRow.getEntry().ambient;
                }
                if (z) {
                    i = i2;
                }
            }
        }
        if (i == childCount) {
            z2 = true;
        }
        updateSpeedBumpIndex(i, z2);
    }

    public void updateSectionBoundaries() {
        this.mSectionsManager.updateSectionBoundaries();
    }

    /* access modifiers changed from: private */
    public void updateContinuousBackgroundDrawing() {
        boolean z = !this.mAmbientState.isFullyAwake() && !this.mAmbientState.getDraggedViews().isEmpty();
        if (z != this.mContinuousBackgroundUpdate) {
            this.mContinuousBackgroundUpdate = z;
            if (z) {
                getViewTreeObserver().addOnPreDrawListener(this.mBackgroundUpdater);
            } else {
                getViewTreeObserver().removeOnPreDrawListener(this.mBackgroundUpdater);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateContinuousShadowDrawing() {
        boolean z = this.mAnimationRunning || !this.mAmbientState.getDraggedViews().isEmpty();
        if (z != this.mContinuousShadowUpdate) {
            if (z) {
                getViewTreeObserver().addOnPreDrawListener(this.mShadowUpdater);
            } else {
                getViewTreeObserver().removeOnPreDrawListener(this.mShadowUpdater);
            }
            this.mContinuousShadowUpdate = z;
        }
    }

    public void resetExposedMenuView(boolean z, boolean z2) {
        this.mSwipeHelper.resetExposedMenuView(z, z2);
    }

    private static boolean matchesSelection(ExpandableNotificationRow expandableNotificationRow, int i) {
        if (i == 0) {
            return true;
        }
        if (i == 1) {
            return expandableNotificationRow.getEntry().isHighPriority();
        }
        if (i == 2) {
            return !expandableNotificationRow.getEntry().isHighPriority();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Unknown selection: ");
        sb.append(i);
        throw new IllegalArgumentException(sb.toString());
    }

    public DragDownCallback getDragDownCallback() {
        return this.mDragDownCallback;
    }

    public HeadsUpTouchHelper.Callback getHeadsUpCallback() {
        return this.mHeadsUpCallback;
    }

    public Callback getExpandHelperCallback() {
        return this.mExpandHelperCallback;
    }

    private boolean shouldHideSensitive(View view, boolean z) {
        boolean isContentHidden = view instanceof ExpandableNotificationRow ? ((ExpandableNotificationRow) view).isContentHidden() : false;
        if (z || isContentHidden) {
            return true;
        }
        return false;
    }
}
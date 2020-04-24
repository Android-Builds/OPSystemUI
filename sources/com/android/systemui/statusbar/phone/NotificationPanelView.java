package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardClockSwitch;
import com.android.keyguard.KeyguardStatusView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.DejankUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$integer;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.fragments.FragmentHostManager.FragmentListener;
import com.android.systemui.p007qs.QSFragment;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.p006qs.C0959QS;
import com.android.systemui.plugins.p006qs.C0959QS.HeightListener;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.PulseExpansionHandler;
import com.android.systemui.statusbar.PulseExpansionHandler.ExpansionCallback;
import com.android.systemui.statusbar.RemoteInputController.Delegate;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator.ExpandAnimationParameters;
import com.android.systemui.statusbar.notification.AnimatableProperty;
import com.android.systemui.statusbar.notification.DynamicPrivacyController;
import com.android.systemui.statusbar.notification.DynamicPrivacyController.Listener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator;
import com.android.systemui.statusbar.notification.PropertyAnimator;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ActivatableNotificationView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.ExpandableView;
import com.android.systemui.statusbar.notification.row.ExpandableView.OnHeightChangedListener;
import com.android.systemui.statusbar.notification.stack.AnimationProperties;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout.OnEmptySpaceClickListener;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout.OnOverscrollTopChangedListener;
import com.android.systemui.statusbar.phone.KeyguardAffordanceHelper.Callback;
import com.android.systemui.statusbar.phone.KeyguardClockPositionAlgorithm.Result;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.util.InjectionInflationController;
import com.oneplus.systemui.statusbar.phone.OpNotificationPanelView;
import com.oneplus.systemui.util.OpMdmLogger;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class NotificationPanelView extends OpNotificationPanelView implements OnHeightChangedListener, OnClickListener, OnOverscrollTopChangedListener, Callback, OnEmptySpaceClickListener, OnHeadsUpChangedListener, HeightListener, ZenModeController.Callback, ConfigurationListener, StateListener, ExpansionCallback, Listener {
    private static final AnimationProperties CLOCK_ANIMATION_PROPERTIES;
    /* access modifiers changed from: private */
    public static final Rect mDummyDirtyRect = new Rect(0, 0, 1, 1);
    private static final Rect mEmptyRect = new Rect();
    private final AnimatableProperty PANEL_ALPHA = AnimatableProperty.from("panelAlpha", $$Lambda$aKsp0zdf_wKFZXD1TonJ2cFEsN4.INSTANCE, $$Lambda$SmdYpsZqQm1fpR9OgK3SiEL3pJQ.INSTANCE, R$id.panel_alpha_animator_tag, R$id.panel_alpha_animator_start_tag, R$id.panel_alpha_animator_end_tag);
    private final AnimationProperties PANEL_ALPHA_IN_PROPERTIES;
    private final AnimationProperties PANEL_ALPHA_OUT_PROPERTIES;
    private final AccessibilityManager mAccessibilityManager;
    private boolean mAffordanceHasPreview;
    @VisibleForTesting
    protected KeyguardAffordanceHelper mAffordanceHelper;
    private Consumer<Boolean> mAffordanceLaunchListener;
    private final Paint mAlphaPaint = new Paint();
    private int mAmbientIndicationBottomPadding;
    private final Runnable mAnimateKeyguardBottomAreaInvisibleEndRunnable;
    /* access modifiers changed from: private */
    public final Runnable mAnimateKeyguardStatusBarInvisibleEndRunnable;
    private final Runnable mAnimateKeyguardStatusViewGoneEndRunnable;
    private final Runnable mAnimateKeyguardStatusViewInvisibleEndRunnable;
    private final Runnable mAnimateKeyguardStatusViewVisibleEndRunnable;
    private boolean mAnimateNextPositionUpdate;
    private AnimatorListenerAdapter mAnimatorListenerAdapter = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animator) {
            if (NotificationPanelView.this.mPanelAlphaEndAction != null) {
                NotificationPanelView.this.mPanelAlphaEndAction.run();
            }
        }
    };
    protected int mBarState;
    @VisibleForTesting
    protected ViewGroup mBigClockContainer;
    private boolean mBlockTouches;
    private boolean mBlockingExpansionForCurrentTouch;
    private float mBottomAreaShadeAlpha;
    private final ValueAnimator mBottomAreaShadeAlphaAnimator;
    private final KeyguardClockPositionAlgorithm mClockPositionAlgorithm = new KeyguardClockPositionAlgorithm();
    private final Result mClockPositionResult = new Result();
    private boolean mClosingWithAlphaFadeOut;
    private boolean mCollapsedOnDown;
    private final CommandQueue mCommandQueue;
    private boolean mConflictingQsExpansionGesture;
    private int mCurrentPanelAlpha;
    private int mDisplayId;
    private boolean mDozing;
    private boolean mDozingOnDown;
    private float mEmptyDragAmount;
    private final NotificationEntryManager mEntryManager;
    private float mExpandOffset;
    private boolean mExpandingFromHeadsUp;
    private FalsingManager mFalsingManager;
    private FlingAnimationUtils mFlingAnimationUtils;
    private final FragmentListener mFragmentListener;
    private NotificationGroupManager mGroupManager;
    private boolean mHeadsUpAnimatingAway;
    private HeadsUpAppearanceController mHeadsUpAppearanceController;
    private Runnable mHeadsUpExistenceChangedRunnable = new Runnable() {
        public void run() {
            NotificationPanelView.this.setHeadsUpAnimatingAway(false);
            NotificationPanelView.this.notifyBarPanelExpansionChanged();
        }
    };
    private HeadsUpTouchHelper mHeadsUpTouchHelper;
    private boolean mHideIconsDuringNotificationLaunch = true;
    private int mIndicationBottomPadding;
    private float mInitialHeightOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private final InjectionInflationController mInjectionInflationController;
    private boolean mIntercepting;
    private float mInterpolatedDarkAmount;
    private boolean mIsExpanding;
    private boolean mIsExpansionFromHeadsUp;
    private boolean mIsFullWidth;
    private boolean mIsLaunchTransitionFinished;
    private boolean mIsLaunchTransitionRunning;
    private KeyguardIndicationController mKeyguardIndicationController;
    /* access modifiers changed from: private */
    public boolean mKeyguardShowing;
    @VisibleForTesting
    protected KeyguardStatusBarView mKeyguardStatusBar;
    /* access modifiers changed from: private */
    public float mKeyguardStatusBarAnimateAlpha = 1.0f;
    @VisibleForTesting
    protected KeyguardStatusView mKeyguardStatusView;
    /* access modifiers changed from: private */
    public boolean mKeyguardStatusViewAnimating;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    /* access modifiers changed from: private */
    public String mLastCameraLaunchSource = "lockscreen_affordance";
    private int mLastOrientation = -1;
    private float mLastOverscroll;
    private float mLastTouchX;
    private float mLastTouchY;
    private Runnable mLaunchAnimationEndRunnable;
    private boolean mLaunchingAffordance;
    private float mLinearDarkAmount;
    private boolean mListenForHeadsUp;
    private LockscreenGestureLogger mLockscreenGestureLogger = new LockscreenGestureLogger();
    private final NotificationLockscreenUserManager mLockscreenUserManager;
    private int mMaxFadeoutHeight;
    private int mNavigationBarBottomHeight;
    private boolean mNeedShowOTAWizard;
    private boolean mNoVisibleNotifications = true;
    protected NotificationsQuickSettingsContainer mNotificationContainerParent;
    protected NotificationStackScrollLayout mNotificationStackScroller;
    private int mNotificationsHeaderCollideDistance;
    private int mOldLayoutDirection;
    private boolean mOnlyAffordanceInThisMotion;
    private int mPanelAlpha;
    /* access modifiers changed from: private */
    public Runnable mPanelAlphaEndAction;
    private boolean mPanelExpanded;
    private int mPositionMinSideMargin;
    private final PowerManager mPowerManager;
    private final PulseExpansionHandler mPulseExpansionHandler;
    private boolean mPulsing;
    /* access modifiers changed from: private */
    public C0959QS mQs;
    private boolean mQsAnimatorExpand;
    private boolean mQsExpandImmediate;
    private boolean mQsExpanded;
    private boolean mQsExpandedWhenExpandingStarted;
    /* access modifiers changed from: private */
    public ValueAnimator mQsExpansionAnimator;
    protected boolean mQsExpansionEnabled = true;
    private boolean mQsExpansionFromOverscroll;
    protected float mQsExpansionHeight;
    private int mQsFalsingThreshold;
    @VisibleForTesting
    protected FrameLayout mQsFrame;
    private boolean mQsFullyExpanded;
    protected int mQsMaxExpansionHeight;
    protected int mQsMinExpansionHeight;
    private int mQsNotificationTopPadding;
    private int mQsPeekHeight;
    private boolean mQsScrimEnabled = true;
    /* access modifiers changed from: private */
    public ValueAnimator mQsSizeChangeAnimator;
    private boolean mQsTouchAboveFalsingThreshold;
    private boolean mQsTracking;
    private VelocityTracker mQsVelocityTracker;
    private final ShadeController mShadeController;
    private boolean mShowEmptyShadeView;
    private boolean mShowIconsWhenExpanded;
    private int mStackScrollerMeasuringPass;
    /* access modifiers changed from: private */
    public boolean mStackScrollerOverscrolling;
    private final AnimatorUpdateListener mStatusBarAnimateAlphaListener;
    private int mStatusBarMinHeight;
    private int mThemeResId;
    private ArrayList<Consumer<ExpandableNotificationRow>> mTrackingHeadsUpListeners = new ArrayList<>();
    private int mTrackingPointer;
    private boolean mTwoFingerQsExpandPossible;
    private int mUnlockMoveDistance;
    private boolean mUserSetupComplete;
    private ArrayList<Runnable> mVerticalTranslationListener = new ArrayList<>();
    private final NotificationWakeUpCoordinator mWakeUpCoordinator;

    public interface PanelExpansionListener {
        void onPanelExpansionChanged(float f, boolean z);

        void onQsExpansionChanged(float f);
    }

    public void onReset(ExpandableView expandableView) {
    }

    public boolean shouldDelayChildPressedState() {
        return true;
    }

    static {
        AnimationProperties animationProperties = new AnimationProperties();
        animationProperties.setDuration(360);
        CLOCK_ANIMATION_PROPERTIES = animationProperties;
    }

    public NotificationPanelView(Context context, AttributeSet attributeSet, InjectionInflationController injectionInflationController, NotificationWakeUpCoordinator notificationWakeUpCoordinator, PulseExpansionHandler pulseExpansionHandler, DynamicPrivacyController dynamicPrivacyController) {
        super(context, attributeSet);
        AnimationProperties animationProperties = new AnimationProperties();
        animationProperties.setDuration(150);
        animationProperties.setCustomInterpolator(this.PANEL_ALPHA.getProperty(), Interpolators.ALPHA_OUT);
        this.PANEL_ALPHA_OUT_PROPERTIES = animationProperties;
        AnimationProperties animationProperties2 = new AnimationProperties();
        animationProperties2.setDuration(200);
        animationProperties2.setAnimationFinishListener(this.mAnimatorListenerAdapter);
        animationProperties2.setCustomInterpolator(this.PANEL_ALPHA.getProperty(), Interpolators.ALPHA_IN);
        this.PANEL_ALPHA_IN_PROPERTIES = animationProperties2;
        this.mEntryManager = (NotificationEntryManager) Dependency.get(NotificationEntryManager.class);
        this.mLockscreenUserManager = (NotificationLockscreenUserManager) Dependency.get(NotificationLockscreenUserManager.class);
        this.mShadeController = (ShadeController) Dependency.get(ShadeController.class);
        this.mAnimateKeyguardStatusViewInvisibleEndRunnable = new Runnable() {
            public void run() {
                NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
                NotificationPanelView.this.mKeyguardStatusView.setVisibility(4);
            }
        };
        this.mAnimateKeyguardStatusViewGoneEndRunnable = new Runnable() {
            public void run() {
                NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
                NotificationPanelView.this.mKeyguardStatusView.setVisibility(8);
            }
        };
        this.mAnimateKeyguardStatusViewVisibleEndRunnable = new Runnable() {
            public void run() {
                NotificationPanelView.this.mKeyguardStatusViewAnimating = false;
            }
        };
        this.mAnimateKeyguardStatusBarInvisibleEndRunnable = new Runnable() {
            public void run() {
                NotificationPanelView.this.mKeyguardStatusBar.setVisibility(4);
                NotificationPanelView.this.mKeyguardStatusBar.setAlpha(1.0f);
                NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = 1.0f;
            }
        };
        this.mStatusBarAnimateAlphaListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                NotificationPanelView.this.mKeyguardStatusBarAnimateAlpha = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                NotificationPanelView.this.updateHeaderKeyguardAlpha();
            }
        };
        this.mAnimateKeyguardBottomAreaInvisibleEndRunnable = new Runnable() {
            public void run() {
                NotificationPanelView.this.mKeyguardBottomArea.setVisibility(8);
            }
        };
        this.mFragmentListener = new FragmentListener() {
            public void onFragmentViewCreated(String str, Fragment fragment) {
                NotificationPanelView.this.mQs = (C0959QS) fragment;
                NotificationPanelView.this.mQs.setPanelView(NotificationPanelView.this);
                NotificationPanelView.this.mQs.setExpandClickListener(NotificationPanelView.this);
                NotificationPanelView.this.mQs.setHeaderClickable(NotificationPanelView.this.mQsExpansionEnabled);
                NotificationPanelView.this.mQs.setKeyguardShowing(NotificationPanelView.this.mKeyguardShowing);
                NotificationPanelView.this.mQs.setOverscrolling(NotificationPanelView.this.mStackScrollerOverscrolling);
                NotificationPanelView.this.mQs.getView().addOnLayoutChangeListener(new OnLayoutChangeListener() {
                    public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                        C148219.this.lambda$onFragmentViewCreated$0$NotificationPanelView$19(view, i, i2, i3, i4, i5, i6, i7, i8);
                    }
                });
                NotificationPanelView notificationPanelView = NotificationPanelView.this;
                notificationPanelView.mNotificationStackScroller.setQsContainer((ViewGroup) notificationPanelView.mQs.getView());
                if (NotificationPanelView.this.mQs instanceof QSFragment) {
                    NotificationPanelView notificationPanelView2 = NotificationPanelView.this;
                    notificationPanelView2.mKeyguardStatusBar.setQSPanel(((QSFragment) notificationPanelView2.mQs).getQsPanel());
                }
                NotificationPanelView.this.updateQsExpansion();
            }

            public /* synthetic */ void lambda$onFragmentViewCreated$0$NotificationPanelView$19(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                if (i4 - i2 != i8 - i6) {
                    NotificationPanelView.this.onQsHeightChanged();
                }
            }

            public void onFragmentViewDestroyed(String str, Fragment fragment) {
                if (fragment == NotificationPanelView.this.mQs) {
                    NotificationPanelView.this.mQs = null;
                }
            }
        };
        this.mNeedShowOTAWizard = false;
        setWillNotDraw(true);
        this.mInjectionInflationController = injectionInflationController;
        this.mFalsingManager = FalsingManagerFactory.getInstance(context);
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mWakeUpCoordinator = notificationWakeUpCoordinator;
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
        setAccessibilityPaneTitle(determineAccessibilityPaneTitle());
        this.mAlphaPaint.setXfermode(new PorterDuffXfermode(Mode.MULTIPLY));
        setPanelAlpha(255, false);
        this.mCommandQueue = (CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class);
        this.mDisplayId = context.getDisplayId();
        this.mPulseExpansionHandler = pulseExpansionHandler;
        this.mThemeResId = context.getThemeResId();
        dynamicPrivacyController.addListener(this);
        this.mBottomAreaShadeAlphaAnimator = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f});
        this.mBottomAreaShadeAlphaAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                NotificationPanelView.this.lambda$new$0$NotificationPanelView(valueAnimator);
            }
        });
        this.mBottomAreaShadeAlphaAnimator.setDuration(160);
        this.mBottomAreaShadeAlphaAnimator.setInterpolator(Interpolators.ALPHA_OUT);
    }

    public /* synthetic */ void lambda$new$0$NotificationPanelView(ValueAnimator valueAnimator) {
        this.mBottomAreaShadeAlpha = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        updateKeyguardBottomAreaAlpha();
    }

    public boolean hasCustomClock() {
        return this.mKeyguardStatusView.hasCustomClock();
    }

    private void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
        this.mKeyguardBottomArea.setStatusBar(this.mStatusBar);
        initOpBottomArea();
        updateOpLockIcon();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mKeyguardStatusBar = (KeyguardStatusBarView) findViewById(R$id.keyguard_header);
        this.mKeyguardStatusView = (KeyguardStatusView) findViewById(R$id.keyguard_status_view);
        KeyguardClockSwitch keyguardClockSwitch = (KeyguardClockSwitch) findViewById(R$id.keyguard_clock_container);
        this.mBigClockContainer = (ViewGroup) findViewById(R$id.big_clock_container);
        keyguardClockSwitch.setBigClockContainer(this.mBigClockContainer);
        this.mNotificationContainerParent = (NotificationsQuickSettingsContainer) findViewById(R$id.notification_container_parent);
        this.mNotificationStackScroller = (NotificationStackScrollLayout) findViewById(R$id.notification_stack_scroller);
        this.mNotificationStackScroller.setOnHeightChangedListener(this);
        this.mNotificationStackScroller.setOverscrollTopChangedListener(this);
        this.mNotificationStackScroller.setOnEmptySpaceClickListener(this);
        NotificationStackScrollLayout notificationStackScrollLayout = this.mNotificationStackScroller;
        Objects.requireNonNull(notificationStackScrollLayout);
        addTrackingHeadsUpListener(new Consumer() {
            public final void accept(Object obj) {
                NotificationStackScrollLayout.this.setTrackingHeadsUp((ExpandableNotificationRow) obj);
            }
        });
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) findViewById(R$id.keyguard_bottom_area);
        this.mLastOrientation = getResources().getConfiguration().orientation;
        initBottomArea();
        this.mWakeUpCoordinator.setStackScroller(this.mNotificationStackScroller);
        this.mQsFrame = (FrameLayout) findViewById(R$id.qs_frame);
        this.mPulseExpansionHandler.setUp(this.mNotificationStackScroller, this, this.mShadeController);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        FragmentHostManager.get(this).addTagListener(C0959QS.TAG, this.mFragmentListener);
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).addCallback(this);
        ((ZenModeController) Dependency.get(ZenModeController.class)).addCallback(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        onThemeChanged();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        FragmentHostManager.get(this).removeTagListener(C0959QS.TAG, this.mFragmentListener);
        ((StatusBarStateController) Dependency.get(StatusBarStateController.class)).removeCallback(this);
        ((ZenModeController) Dependency.get(ZenModeController.class)).removeCallback(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    /* access modifiers changed from: protected */
    public void loadDimens() {
        super.loadDimens();
        this.mFlingAnimationUtils = new FlingAnimationUtils(getContext(), 0.4f);
        this.mStatusBarMinHeight = getResources().getDimensionPixelSize(17105422);
        this.mQsPeekHeight = getResources().getDimensionPixelSize(R$dimen.qs_peek_height);
        this.mNotificationsHeaderCollideDistance = getResources().getDimensionPixelSize(R$dimen.header_notifications_collide_distance);
        this.mUnlockMoveDistance = getResources().getDimensionPixelOffset(R$dimen.unlock_move_distance);
        this.mClockPositionAlgorithm.loadDimens(getResources());
        this.mQsFalsingThreshold = getResources().getDimensionPixelSize(R$dimen.qs_falsing_threshold);
        this.mPositionMinSideMargin = getResources().getDimensionPixelSize(R$dimen.notification_panel_min_side_margin);
        this.mMaxFadeoutHeight = getResources().getDimensionPixelSize(R$dimen.max_notification_fadeout_height);
        this.mIndicationBottomPadding = getResources().getDimensionPixelSize(R$dimen.keyguard_indication_bottom_padding);
        this.mQsNotificationTopPadding = getResources().getDimensionPixelSize(R$dimen.qs_notification_padding);
    }

    public void setLaunchAffordanceListener(Consumer<Boolean> consumer) {
        this.mAffordanceLaunchListener = consumer;
    }

    public void updateResources() {
        Resources resources = getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(R$dimen.qs_panel_width);
        int integer = getResources().getInteger(R$integer.notification_panel_layout_gravity);
        LayoutParams layoutParams = (LayoutParams) this.mQsFrame.getLayoutParams();
        if (!(layoutParams.width == dimensionPixelSize && layoutParams.gravity == integer)) {
            layoutParams.width = dimensionPixelSize;
            layoutParams.gravity = integer;
            this.mQsFrame.setLayoutParams(layoutParams);
        }
        int dimensionPixelSize2 = resources.getDimensionPixelSize(R$dimen.notification_panel_width);
        LayoutParams layoutParams2 = (LayoutParams) this.mNotificationStackScroller.getLayoutParams();
        if (layoutParams2.width != dimensionPixelSize2 || layoutParams2.gravity != integer) {
            layoutParams2.width = dimensionPixelSize2;
            layoutParams2.gravity = integer;
            this.mNotificationStackScroller.setLayoutParams(layoutParams2);
        }
    }

    public void onDensityOrFontScaleChanged() {
        updateShowEmptyShadeView();
    }

    public void onThemeChanged() {
        int themeResId = getContext().getThemeResId();
        if (this.mThemeResId != themeResId) {
            this.mThemeResId = themeResId;
            reInflateViews();
        }
    }

    public void onOverlayChanged() {
        reInflateViews();
    }

    private void reInflateViews() {
        updateShowEmptyShadeView();
        int indexOfChild = indexOfChild(this.mKeyguardStatusView);
        removeView(this.mKeyguardStatusView);
        this.mKeyguardStatusView = (KeyguardStatusView) this.mInjectionInflationController.injectable(LayoutInflater.from(this.mContext)).inflate(R$layout.keyguard_status_view, this, false);
        addView(this.mKeyguardStatusView, indexOfChild);
        this.mStatusBar.createKeyguardIndication(this.mLockIcon);
        this.mBigClockContainer.removeAllViews();
        ((KeyguardClockSwitch) findViewById(R$id.keyguard_clock_container)).setBigClockContainer(this.mBigClockContainer);
        int indexOfChild2 = indexOfChild(this.mKeyguardBottomArea);
        removeView(this.mKeyguardBottomArea);
        KeyguardBottomAreaView keyguardBottomAreaView = this.mKeyguardBottomArea;
        this.mKeyguardBottomArea = (KeyguardBottomAreaView) this.mInjectionInflationController.injectable(LayoutInflater.from(this.mContext)).inflate(R$layout.keyguard_bottom_area, this, false);
        this.mKeyguardBottomArea.initFrom(keyguardBottomAreaView);
        addView(this.mKeyguardBottomArea, indexOfChild2);
        initBottomArea();
        this.mKeyguardIndicationController.setIndicationArea(this.mKeyguardBottomArea);
        this.mKeyguardIndicationController.reInflateAdjust();
        onDozeAmountChanged(this.mStatusBarStateController.getDozeAmount(), this.mStatusBarStateController.getInterpolatedDozeAmount());
        KeyguardStatusBarView keyguardStatusBarView = this.mKeyguardStatusBar;
        if (keyguardStatusBarView != null) {
            keyguardStatusBarView.onThemeChanged();
        }
        setKeyguardStatusViewVisibility(this.mBarState, false, false);
        setKeyguardBottomAreaVisibility(this.mBarState, false);
    }

    private void initBottomArea() {
        this.mAffordanceHelper = new KeyguardAffordanceHelper(this, getContext());
        this.mKeyguardBottomArea.setAffordanceHelper(this.mAffordanceHelper);
        this.mKeyguardBottomArea.setStatusBar(this.mStatusBar);
        this.mKeyguardBottomArea.setUserSetupComplete(this.mUserSetupComplete);
        initOpBottomArea();
        updateOpLockIcon();
    }

    public void setKeyguardIndicationController(KeyguardIndicationController keyguardIndicationController) {
        this.mKeyguardIndicationController = keyguardIndicationController;
        this.mKeyguardIndicationController.setIndicationArea(this.mKeyguardBottomArea);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        setIsFullWidth(this.mNotificationStackScroller.getWidth() == getWidth());
        this.mKeyguardStatusView.setPivotX((float) (getWidth() / 2));
        KeyguardStatusView keyguardStatusView = this.mKeyguardStatusView;
        keyguardStatusView.setPivotY(keyguardStatusView.getClockTextSize() * 0.34521484f);
        int i5 = this.mQsMaxExpansionHeight;
        C0959QS qs = this.mQs;
        if (qs != null) {
            this.mQsMinExpansionHeight = this.mKeyguardShowing ? 0 : qs.getQsMinExpansionHeight();
            this.mQsMaxExpansionHeight = this.mQs.getDesiredHeight();
            this.mNotificationStackScroller.setMaxTopPadding(this.mQsMaxExpansionHeight + this.mQsNotificationTopPadding);
        }
        positionClockAndNotifications();
        if (this.mQsExpanded && this.mQsFullyExpanded) {
            this.mQsExpansionHeight = (float) this.mQsMaxExpansionHeight;
            requestScrollerTopPaddingUpdate(false);
            requestPanelHeightUpdate();
            int i6 = this.mQsMaxExpansionHeight;
            if (i6 != i5) {
                startQsSizeChangeAnimation(i5, i6);
            }
        } else if (!this.mQsExpanded) {
            setQsExpansion(((float) this.mQsMinExpansionHeight) + this.mLastOverscroll);
        }
        updateExpandedHeight(getExpandedHeight());
        updateHeader();
        if (this.mQsSizeChangeAnimator == null) {
            C0959QS qs2 = this.mQs;
            if (qs2 != null) {
                qs2.setHeightOverride(qs2.getDesiredHeight());
            }
        }
        updateMaxHeadsUpTranslation();
    }

    private Rect calculateGestureExclusionRect() {
        Region calculateTouchableRegion = this.mHeadsUpManager.calculateTouchableRegion();
        Rect bounds = (!isFullyCollapsed() || calculateTouchableRegion == null) ? null : calculateTouchableRegion.getBounds();
        return bounds != null ? bounds : mEmptyRect;
    }

    private void setIsFullWidth(boolean z) {
        this.mIsFullWidth = z;
        this.mNotificationStackScroller.setIsFullWidth(z);
    }

    private void startQsSizeChangeAnimation(int i, int i2) {
        ValueAnimator valueAnimator = this.mQsSizeChangeAnimator;
        if (valueAnimator != null) {
            i = ((Integer) valueAnimator.getAnimatedValue()).intValue();
            this.mQsSizeChangeAnimator.cancel();
        }
        this.mQsSizeChangeAnimator = ValueAnimator.ofInt(new int[]{i, i2});
        this.mQsSizeChangeAnimator.setDuration(300);
        this.mQsSizeChangeAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        this.mQsSizeChangeAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                NotificationPanelView.this.requestScrollerTopPaddingUpdate(false);
                NotificationPanelView.this.requestPanelHeightUpdate();
                NotificationPanelView.this.mQs.setHeightOverride(((Integer) NotificationPanelView.this.mQsSizeChangeAnimator.getAnimatedValue()).intValue());
            }
        });
        this.mQsSizeChangeAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                NotificationPanelView.this.mQsSizeChangeAnimator = null;
            }
        });
        this.mQsSizeChangeAnimator.start();
    }

    private void positionClockAndNotifications() {
        int i;
        boolean isAddOrRemoveAnimationPending = this.mNotificationStackScroller.isAddOrRemoveAnimationPending();
        boolean z = isAddOrRemoveAnimationPending || this.mAnimateNextPositionUpdate;
        if (this.mBarState != 1) {
            C0959QS qs = this.mQs;
            i = (qs != null ? qs.getHeader().getHeight() : 0) + this.mQsPeekHeight + this.mQsNotificationTopPadding;
        } else {
            int height = getHeight();
            int max = Math.max(this.mIndicationBottomPadding, this.mAmbientIndicationBottomPadding);
            this.mClockPositionAlgorithm.setup(this.mStatusBarMinHeight, height - max, this.mNotificationStackScroller.getIntrinsicContentHeight(), getExpandedFraction(), height, this.mKeyguardStatusView.getHeight(), this.mKeyguardStatusView.getClockPreferredY(height), hasCustomClock(), this.mNotificationStackScroller.getVisibleNotificationCount() != 0, this.mInterpolatedDarkAmount, this.mEmptyDragAmount);
            this.mClockPositionAlgorithm.run(this.mClockPositionResult);
            PropertyAnimator.setProperty(this.mKeyguardStatusView, AnimatableProperty.f91X, (float) this.mClockPositionResult.clockX, CLOCK_ANIMATION_PROPERTIES, z);
            PropertyAnimator.setProperty(this.mKeyguardStatusView, AnimatableProperty.f92Y, (float) this.mClockPositionResult.clockY, CLOCK_ANIMATION_PROPERTIES, z);
            updateNotificationTranslucency();
            updateClock();
            i = this.mClockPositionResult.stackScrollerPadding;
        }
        this.mNotificationStackScroller.setIntrinsicPadding(i);
        this.mNotificationStackScroller.setAntiBurnInOffsetX(this.mClockPositionResult.clockX);
        this.mKeyguardBottomArea.setAntiBurnInOffsetX(this.mClockPositionResult.clockX);
        this.mStackScrollerMeasuringPass++;
        requestScrollerTopPaddingUpdate(isAddOrRemoveAnimationPending);
        this.mStackScrollerMeasuringPass = 0;
        this.mAnimateNextPositionUpdate = false;
    }

    public int computeMaxKeyguardNotifications(int i) {
        float f;
        float minStackScrollerPadding = this.mClockPositionAlgorithm.getMinStackScrollerPadding();
        int max = Math.max(1, getResources().getDimensionPixelSize(R$dimen.notification_divider_height));
        NotificationShelf notificationShelf = this.mNotificationStackScroller.getNotificationShelf();
        if (notificationShelf.getVisibility() == 8) {
            f = 0.0f;
        } else {
            f = (float) (notificationShelf.getIntrinsicHeight() + this.mKeyguardBottomArea.getIndicationArea().getMeasuredHeight());
        }
        int i2 = 0;
        float height = (((((float) this.mNotificationStackScroller.getHeight()) - minStackScrollerPadding) - f) - ((float) Math.max(this.mIndicationBottomPadding, this.mAmbientIndicationBottomPadding))) - ((float) this.mKeyguardStatusView.getLogoutButtonHeight());
        int i3 = 0;
        while (true) {
            if (i3 >= this.mNotificationStackScroller.getChildCount()) {
                break;
            }
            ExpandableView expandableView = (ExpandableView) this.mNotificationStackScroller.getChildAt(i3);
            if (expandableView instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) expandableView;
                NotificationGroupManager notificationGroupManager = this.mGroupManager;
                if (!(notificationGroupManager != null && notificationGroupManager.isSummaryOfSuppressedGroup(expandableNotificationRow.getStatusBarNotification())) && this.mLockscreenUserManager.shouldShowOnKeyguard(expandableNotificationRow.getStatusBarNotification()) && !expandableNotificationRow.isRemoved()) {
                    height -= (float) (expandableView.getMinHeight(true) + max);
                    if (height >= 0.0f && i2 < i) {
                        i2++;
                    }
                }
            }
            i3++;
        }
        if (height > (-f)) {
            for (int i4 = i3 + 1; i4 < this.mNotificationStackScroller.getChildCount(); i4++) {
                if (this.mNotificationStackScroller.getChildAt(i4) instanceof ExpandableNotificationRow) {
                    return i2;
                }
            }
            i2++;
        }
        return i2;
    }

    private void updateClock() {
        if (!this.mKeyguardStatusViewAnimating) {
            this.mKeyguardStatusView.setAlpha(this.mClockPositionResult.clockAlpha);
        }
    }

    public void animateToFullShade(long j) {
        this.mNotificationStackScroller.goToFullShade(j);
        requestLayout();
        this.mAnimateNextPositionUpdate = true;
    }

    public void setQsExpansionEnabled(boolean z) {
        this.mQsExpansionEnabled = z;
        C0959QS qs = this.mQs;
        if (qs != null) {
            qs.setHeaderClickable(z);
        }
    }

    public void resetViews(boolean z) {
        this.mIsLaunchTransitionFinished = false;
        this.mBlockTouches = false;
        if (!this.mLaunchingAffordance) {
            this.mAffordanceHelper.reset(false);
            this.mLastCameraLaunchSource = "lockscreen_affordance";
        }
        this.mStatusBar.getGutsManager().closeAndSaveGuts(true, true, true, -1, -1, true);
        if (z) {
            animateCloseQs(true);
        } else {
            closeQs();
        }
        this.mNotificationStackScroller.setOverScrollAmount(0.0f, true, z, !z);
        this.mNotificationStackScroller.resetScrollPosition();
        boolean goingToFullShade = this.mStatusBarStateController.goingToFullShade();
        boolean isKeyguardFadingAway = this.mKeyguardMonitor.isKeyguardFadingAway();
        if (this.mBarState == 1 && !isKeyguardFadingAway && this.mKeyguardStatusView.getVisibility() != 0) {
            setKeyguardStatusViewVisibility(this.mBarState, isKeyguardFadingAway, goingToFullShade);
        }
    }

    public void collapse(boolean z, float f) {
        if (canPanelBeCollapsed()) {
            if (this.mQsExpanded) {
                this.mQsExpandImmediate = true;
                this.mNotificationStackScroller.setShouldShowShelfOnly(true);
            }
            super.collapse(z, f);
        }
    }

    public void closeQs() {
        cancelQsAnimation();
        setQsExpansion((float) this.mQsMinExpansionHeight);
    }

    public void animateCloseQs(boolean z) {
        ValueAnimator valueAnimator = this.mQsExpansionAnimator;
        if (valueAnimator != null) {
            if (this.mQsAnimatorExpand) {
                float f = this.mQsExpansionHeight;
                valueAnimator.cancel();
                setQsExpansion(f);
            } else {
                return;
            }
        }
        flingSettings(0.0f, z ? 2 : 1);
    }

    public void expandWithQs() {
        if (this.mQsExpansionEnabled) {
            this.mQsExpandImmediate = true;
            this.mNotificationStackScroller.setShouldShowShelfOnly(true);
        }
        if (isFullyCollapsed()) {
            expand(true);
        } else {
            flingSettings(0.0f, 0);
        }
    }

    public void expandWithoutQs() {
        if (isQsExpanded()) {
            flingSettings(0.0f, 1);
        } else {
            expand(true);
        }
    }

    public void fling(float f, boolean z) {
        GestureRecorder gestureRecorder = ((PhoneStatusBarView) this.mBar).mBar.getGestureRecorder();
        if (gestureRecorder != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("fling ");
            sb.append(f > 0.0f ? "open" : "closed");
            String sb2 = sb.toString();
            StringBuilder sb3 = new StringBuilder();
            sb3.append("notifications,v=");
            sb3.append(f);
            gestureRecorder.tag(sb2, sb3.toString());
        }
        super.fling(f, z);
    }

    /* access modifiers changed from: protected */
    public void flingToHeight(float f, boolean z, float f2, float f3, boolean z2) {
        this.mHeadsUpTouchHelper.notifyFling(!z);
        setClosingWithAlphaFadeout(!z && getFadeoutAlpha() == 1.0f);
        super.flingToHeight(f, z, f2, f3, z2);
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (this.mBlockTouches || (this.mQsFullyExpanded && this.mQs.onInterceptTouchEvent(motionEvent))) {
            return false;
        }
        initDownStates(motionEvent);
        if (this.mStatusBar.isBouncerShowing()) {
            return true;
        }
        if (this.mBar.panelEnabled() && this.mHeadsUpTouchHelper.onInterceptTouchEvent(motionEvent)) {
            this.mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(this.mContext, "panel_open", 1);
            MetricsLogger.count(this.mContext, "panel_open_peek", 1);
            return true;
        } else if (this.mPulseExpansionHandler.onInterceptTouchEvent(motionEvent)) {
            return true;
        } else {
            if (isFullyCollapsed() || !onQsIntercept(motionEvent)) {
                return super.onInterceptTouchEvent(motionEvent);
            }
            return true;
        }
    }

    private boolean onQsIntercept(MotionEvent motionEvent) {
        int findPointerIndex = motionEvent.findPointerIndex(this.mTrackingPointer);
        if (findPointerIndex < 0) {
            this.mTrackingPointer = motionEvent.getPointerId(0);
            findPointerIndex = 0;
        }
        float x = motionEvent.getX(findPointerIndex);
        float y = motionEvent.getY(findPointerIndex);
        int actionMasked = motionEvent.getActionMasked();
        boolean z = true;
        if (actionMasked != 0) {
            if (actionMasked != 1) {
                if (actionMasked == 2) {
                    float f = y - this.mInitialTouchY;
                    trackMovement(motionEvent);
                    if (this.mQsTracking) {
                        setQsExpansion(f + this.mInitialHeightOnTouch);
                        trackMovement(motionEvent);
                        this.mIntercepting = false;
                        return true;
                    } else if (Math.abs(f) > ((float) this.mTouchSlop) && Math.abs(f) > Math.abs(x - this.mInitialTouchX) && shouldQuickSettingsIntercept(this.mInitialTouchX, this.mInitialTouchY, f)) {
                        this.mQsTracking = true;
                        onQsExpansionStarted();
                        notifyExpandingFinished();
                        this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                        this.mInitialTouchY = y;
                        this.mInitialTouchX = x;
                        this.mIntercepting = false;
                        this.mNotificationStackScroller.cancelLongPress();
                        return true;
                    }
                } else if (actionMasked != 3) {
                    if (actionMasked == 6) {
                        int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
                        if (this.mTrackingPointer == pointerId) {
                            if (motionEvent.getPointerId(0) != pointerId) {
                                z = false;
                            }
                            this.mTrackingPointer = motionEvent.getPointerId(z ? 1 : 0);
                            this.mInitialTouchX = motionEvent.getX(z);
                            this.mInitialTouchY = motionEvent.getY(z);
                        }
                    }
                }
            }
            trackMovement(motionEvent);
            if (this.mQsTracking) {
                if (motionEvent.getActionMasked() != 3) {
                    z = false;
                }
                flingQsWithCurrentVelocity(y, z);
                this.mQsTracking = false;
            }
            this.mIntercepting = false;
        } else {
            this.mIntercepting = true;
            this.mInitialTouchY = y;
            this.mInitialTouchX = x;
            initVelocityTracker();
            trackMovement(motionEvent);
            if (shouldQuickSettingsIntercept(this.mInitialTouchX, this.mInitialTouchY, 0.0f)) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (this.mQsExpansionAnimator != null) {
                onQsExpansionStarted();
                this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                this.mQsTracking = true;
                this.mIntercepting = false;
                this.mNotificationStackScroller.cancelLongPress();
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isInContentBounds(float f, float f2) {
        float x = this.mNotificationStackScroller.getX();
        return !this.mNotificationStackScroller.isBelowLastNotification(f - x, f2) && x < f && f < x + ((float) this.mNotificationStackScroller.getWidth());
    }

    private void initDownStates(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0) {
            boolean z = false;
            this.mOnlyAffordanceInThisMotion = false;
            this.mQsTouchAboveFalsingThreshold = this.mQsFullyExpanded;
            this.mDozingOnDown = isDozing();
            this.mCollapsedOnDown = isFullyCollapsed();
            if (this.mCollapsedOnDown && this.mHeadsUpManager.hasPinnedHeadsUp()) {
                z = true;
            }
            this.mListenForHeadsUp = z;
        }
    }

    private void flingQsWithCurrentVelocity(float f, boolean z) {
        float currentQSVelocity = getCurrentQSVelocity();
        boolean flingExpandsQs = flingExpandsQs(currentQSVelocity);
        if (flingExpandsQs) {
            logQsSwipeDown(f);
        }
        flingSettings(currentQSVelocity, (!flingExpandsQs || z) ? 1 : 0);
    }

    private void logQsSwipeDown(float f) {
        this.mLockscreenGestureLogger.write(this.mBarState == 1 ? 193 : 194, (int) ((f - this.mInitialTouchY) / this.mStatusBar.getDisplayDensity()), (int) (getCurrentQSVelocity() / this.mStatusBar.getDisplayDensity()));
    }

    private boolean flingExpandsQs(float f) {
        boolean z = false;
        if (!this.mFalsingManager.isUnlockingDisabled() && !isFalseTouch()) {
            if (Math.abs(f) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond()) {
                if (getQsExpansionFraction() > 0.5f) {
                    z = true;
                }
                return z;
            } else if (f > 0.0f) {
                z = true;
            }
        }
        return z;
    }

    private boolean isFalseTouch() {
        if (!needsAntiFalsing()) {
            return false;
        }
        if (this.mFalsingManager.isClassiferEnabled()) {
            return this.mFalsingManager.isFalseTouch();
        }
        return !this.mQsTouchAboveFalsingThreshold;
    }

    private float getQsExpansionFraction() {
        float f = this.mQsExpansionHeight;
        int i = this.mQsMinExpansionHeight;
        return Math.min(1.0f, (f - ((float) i)) / ((float) (this.mQsMaxExpansionHeight - i)));
    }

    /* access modifiers changed from: protected */
    public float getOpeningHeight() {
        return this.mNotificationStackScroller.getOpeningHeight();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:47:0x0089, code lost:
        if (r0.isBouncerShowing() != false) goto L_0x0093;
     */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x0098 A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0099  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onTouchEvent(android.view.MotionEvent r5) {
        /*
            r4 = this;
            boolean r0 = r4.mBlockTouches
            r1 = 0
            r2 = 1
            if (r0 != 0) goto L_0x00db
            com.android.systemui.plugins.qs.QS r0 = r4.mQs
            if (r0 == 0) goto L_0x0012
            boolean r0 = r0.isCustomizing()
            if (r0 == 0) goto L_0x0012
            goto L_0x00db
        L_0x0012:
            com.android.systemui.statusbar.phone.StatusBar r0 = r4.mStatusBar
            if (r0 == 0) goto L_0x0024
            boolean r0 = r0.isBouncerShowingScrimmed()
            if (r0 == 0) goto L_0x0024
            java.lang.String r4 = com.oneplus.systemui.statusbar.phone.OpNotificationPanelView.TAG
            java.lang.String r5 = "Skip onTouchEvent BouncerShowingScrimmed"
            android.util.Log.d(r4, r5)
            return r1
        L_0x0024:
            boolean r0 = r4.skipOnTouchEvent()
            if (r0 == 0) goto L_0x002b
            return r1
        L_0x002b:
            r4.initDownStates(r5)
            int r0 = r5.getAction()
            if (r0 == r2) goto L_0x003b
            int r0 = r5.getAction()
            r3 = 3
            if (r0 != r3) goto L_0x003d
        L_0x003b:
            r4.mBlockingExpansionForCurrentTouch = r1
        L_0x003d:
            boolean r0 = r4.mIsExpanding
            if (r0 != 0) goto L_0x004a
            com.android.systemui.statusbar.PulseExpansionHandler r0 = r4.mPulseExpansionHandler
            boolean r0 = r0.onTouchEvent(r5)
            if (r0 == 0) goto L_0x004a
            return r2
        L_0x004a:
            boolean r0 = r4.mListenForHeadsUp
            if (r0 == 0) goto L_0x0067
            com.android.systemui.statusbar.phone.HeadsUpTouchHelper r0 = r4.mHeadsUpTouchHelper
            boolean r0 = r0.isTrackingHeadsUp()
            if (r0 != 0) goto L_0x0067
            com.android.systemui.statusbar.phone.HeadsUpTouchHelper r0 = r4.mHeadsUpTouchHelper
            boolean r0 = r0.onInterceptTouchEvent(r5)
            if (r0 == 0) goto L_0x0067
            r4.mIsExpansionFromHeadsUp = r2
            android.content.Context r0 = r4.mContext
            java.lang.String r3 = "panel_open_peek"
            com.android.internal.logging.MetricsLogger.count(r0, r3, r2)
        L_0x0067:
            boolean r0 = r4.mIsExpanding
            if (r0 == 0) goto L_0x006f
            boolean r0 = r4.mHintAnimationRunning
            if (r0 == 0) goto L_0x0093
        L_0x006f:
            boolean r0 = r4.mQsExpanded
            if (r0 != 0) goto L_0x0093
            int r0 = r4.mBarState
            if (r0 == 0) goto L_0x0093
            boolean r0 = r4.mDozing
            if (r0 != 0) goto L_0x0093
            int r0 = r5.getAction()
            if (r0 != 0) goto L_0x008b
            com.android.systemui.statusbar.phone.StatusBar r0 = r4.mStatusBar
            if (r0 == 0) goto L_0x008b
            boolean r0 = r0.isBouncerShowing()
            if (r0 != 0) goto L_0x0093
        L_0x008b:
            com.android.systemui.statusbar.phone.KeyguardAffordanceHelper r0 = r4.mAffordanceHelper
            boolean r0 = r0.onTouchEvent(r5)
            r0 = r0 | r1
            goto L_0x0094
        L_0x0093:
            r0 = r1
        L_0x0094:
            boolean r3 = r4.mOnlyAffordanceInThisMotion
            if (r3 == 0) goto L_0x0099
            return r2
        L_0x0099:
            com.android.systemui.statusbar.phone.HeadsUpTouchHelper r3 = r4.mHeadsUpTouchHelper
            boolean r3 = r3.onTouchEvent(r5)
            r0 = r0 | r3
            com.android.systemui.statusbar.phone.HeadsUpTouchHelper r3 = r4.mHeadsUpTouchHelper
            boolean r3 = r3.isTrackingHeadsUp()
            if (r3 != 0) goto L_0x00af
            boolean r3 = r4.handleQsTouch(r5)
            if (r3 == 0) goto L_0x00af
            return r2
        L_0x00af:
            int r3 = r5.getActionMasked()
            if (r3 != 0) goto L_0x00ca
            boolean r3 = r4.isFullyCollapsed()
            if (r3 == 0) goto L_0x00ca
            android.content.Context r0 = r4.mContext
            java.lang.String r3 = "panel_open"
            com.android.internal.logging.MetricsLogger.count(r0, r3, r2)
            float r0 = r5.getX()
            r4.updateVerticalPanelPosition(r0)
            r0 = r2
        L_0x00ca:
            boolean r5 = super.onTouchEvent(r5)
            r5 = r5 | r0
            boolean r0 = r4.mDozing
            if (r0 == 0) goto L_0x00d9
            boolean r4 = r4.mPulsing
            if (r4 != 0) goto L_0x00d9
            if (r5 == 0) goto L_0x00da
        L_0x00d9:
            r1 = r2
        L_0x00da:
            return r1
        L_0x00db:
            java.lang.String r5 = com.oneplus.systemui.statusbar.phone.OpNotificationPanelView.TAG
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r3 = "Skip onTouchEvent by "
            r0.append(r3)
            boolean r3 = r4.mBlockTouches
            r0.append(r3)
            java.lang.String r3 = ", "
            r0.append(r3)
            com.android.systemui.plugins.qs.QS r4 = r4.mQs
            if (r4 == 0) goto L_0x00f6
            goto L_0x00f7
        L_0x00f6:
            r2 = r1
        L_0x00f7:
            r0.append(r2)
            java.lang.String r4 = r0.toString()
            android.util.Log.d(r5, r4)
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NotificationPanelView.onTouchEvent(android.view.MotionEvent):boolean");
    }

    private boolean handleQsTouch(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        StatusBar statusBar = this.mStatusBar;
        if (statusBar == null || this.mBarState != 1 || !statusBar.isQsDisabled()) {
            if (actionMasked == 0 && getExpandedFraction() == 1.0f && this.mBarState != 1 && !this.mQsExpanded && this.mQsExpansionEnabled) {
                this.mQsTracking = true;
                this.mConflictingQsExpansionGesture = true;
                onQsExpansionStarted();
                this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                this.mInitialTouchY = motionEvent.getX();
                this.mInitialTouchX = motionEvent.getY();
            }
            if (!isFullyCollapsed() && this.mKeyguardShowing) {
                handleQsDown(motionEvent);
            }
            if (!this.mQsExpandImmediate && this.mQsTracking) {
                onQsTouch(motionEvent);
                if (!this.mConflictingQsExpansionGesture) {
                    return true;
                }
            }
            if (actionMasked == 3 || actionMasked == 1) {
                this.mConflictingQsExpansionGesture = false;
            }
            if (actionMasked == 0 && isFullyCollapsed() && this.mQsExpansionEnabled) {
                this.mTwoFingerQsExpandPossible = true;
            }
            if (this.mTwoFingerQsExpandPossible && isOpenQsEvent(motionEvent) && motionEvent.getY(motionEvent.getActionIndex()) < ((float) this.mStatusBarMinHeight)) {
                MetricsLogger.count(this.mContext, "panel_open_qs", 1);
                this.mQsExpandImmediate = true;
                this.mNotificationStackScroller.setShouldShowShelfOnly(true);
                requestPanelHeightUpdate();
                setListening(true);
            }
            return false;
        }
        if (Build.DEBUG_ONEPLUS && actionMasked == 0) {
            Log.d(OpNotificationPanelView.TAG, "return QS touch");
        }
        return false;
    }

    private boolean isInQsArea(float f, float f2) {
        return f >= this.mQsFrame.getX() && f <= this.mQsFrame.getX() + ((float) this.mQsFrame.getWidth()) && (f2 <= this.mNotificationStackScroller.getBottomMostNotificationBottom() || f2 <= this.mQs.getView().getY() + ((float) this.mQs.getView().getHeight()));
    }

    private boolean isOpenQsEvent(MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();
        int actionMasked = motionEvent.getActionMasked();
        boolean z = actionMasked == 5 && pointerCount == 2;
        boolean z2 = actionMasked == 0 && (motionEvent.isButtonPressed(32) || motionEvent.isButtonPressed(64));
        boolean z3 = actionMasked == 0 && (motionEvent.isButtonPressed(2) || motionEvent.isButtonPressed(4));
        if (z || z2 || z3) {
            return true;
        }
        return false;
    }

    private void handleQsDown(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0 && shouldQuickSettingsIntercept(motionEvent.getX(), motionEvent.getY(), -1.0f)) {
            this.mFalsingManager.onQsDown();
            this.mQsTracking = true;
            onQsExpansionStarted();
            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
            this.mInitialTouchY = motionEvent.getX();
            this.mInitialTouchX = motionEvent.getY();
            notifyExpandingFinished();
        }
    }

    /* access modifiers changed from: protected */
    public boolean flingExpands(float f, float f2, float f3, float f4) {
        boolean flingExpands = super.flingExpands(f, f2, f3, f4);
        if (this.mQsExpansionAnimator != null) {
            return true;
        }
        return flingExpands;
    }

    /* access modifiers changed from: protected */
    public boolean hasConflictingGestures() {
        return this.mBarState != 0;
    }

    /* access modifiers changed from: protected */
    public boolean shouldGestureIgnoreXTouchSlop(float f, float f2) {
        return !this.mAffordanceHelper.isOnAffordanceIcon(f, f2);
    }

    private void onQsTouch(MotionEvent motionEvent) {
        int findPointerIndex = motionEvent.findPointerIndex(this.mTrackingPointer);
        boolean z = false;
        if (findPointerIndex < 0) {
            this.mTrackingPointer = motionEvent.getPointerId(0);
            findPointerIndex = 0;
        }
        float y = motionEvent.getY(findPointerIndex);
        float x = motionEvent.getX(findPointerIndex);
        float f = y - this.mInitialTouchY;
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 0) {
            if (actionMasked != 1) {
                if (actionMasked == 2) {
                    setQsExpansion(this.mInitialHeightOnTouch + f);
                    if (f >= ((float) getFalsingThreshold())) {
                        this.mQsTouchAboveFalsingThreshold = true;
                    }
                    trackMovement(motionEvent);
                    return;
                } else if (actionMasked != 3) {
                    if (actionMasked == 6) {
                        int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
                        if (this.mTrackingPointer == pointerId) {
                            if (motionEvent.getPointerId(0) == pointerId) {
                                z = true;
                            }
                            float y2 = motionEvent.getY(z ? 1 : 0);
                            float x2 = motionEvent.getX(z);
                            this.mTrackingPointer = motionEvent.getPointerId(z);
                            this.mInitialHeightOnTouch = this.mQsExpansionHeight;
                            this.mInitialTouchY = y2;
                            this.mInitialTouchX = x2;
                            return;
                        }
                        return;
                    }
                    return;
                }
            }
            this.mQsTracking = false;
            this.mTrackingPointer = -1;
            trackMovement(motionEvent);
            if (getQsExpansionFraction() != 0.0f || y >= this.mInitialTouchY) {
                if (motionEvent.getActionMasked() == 3) {
                    z = true;
                }
                flingQsWithCurrentVelocity(y, z);
            }
            VelocityTracker velocityTracker = this.mQsVelocityTracker;
            if (velocityTracker != null) {
                velocityTracker.recycle();
                this.mQsVelocityTracker = null;
                return;
            }
            return;
        }
        this.mQsTracking = true;
        this.mInitialTouchY = y;
        this.mInitialTouchX = x;
        if (!((HighlightHintController) Dependency.get(HighlightHintController.class)).isHighLightHintShow() && !((HighlightHintController) Dependency.get(HighlightHintController.class)).isCarModeHighlightHintSHow()) {
            onQsExpansionStarted();
        }
        this.mInitialHeightOnTouch = this.mQsExpansionHeight;
        initVelocityTracker();
        trackMovement(motionEvent);
    }

    private int getFalsingThreshold() {
        return (int) (((float) this.mQsFalsingThreshold) * (this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f));
    }

    public void onOverscrollTopChanged(float f, boolean z) {
        cancelQsAnimation();
        if (!this.mQsExpansionEnabled) {
            f = 0.0f;
        }
        if (f < 1.0f) {
            f = 0.0f;
        }
        int i = (f > 0.0f ? 1 : (f == 0.0f ? 0 : -1));
        boolean z2 = true;
        setOverScrolling(i != 0 && z);
        if (i == 0) {
            z2 = false;
        }
        this.mQsExpansionFromOverscroll = z2;
        this.mLastOverscroll = f;
        updateQsState();
        setQsExpansion(((float) this.mQsMinExpansionHeight) + f);
    }

    public void flingTopOverscroll(float f, boolean z) {
        this.mLastOverscroll = 0.0f;
        this.mQsExpansionFromOverscroll = false;
        setQsExpansion(this.mQsExpansionHeight);
        if (!this.mQsExpansionEnabled && z) {
            f = 0.0f;
        }
        flingSettings(f, (!z || !this.mQsExpansionEnabled) ? 1 : 0, new Runnable() {
            public void run() {
                NotificationPanelView.this.mStackScrollerOverscrolling = false;
                NotificationPanelView.this.setOverScrolling(false);
                NotificationPanelView.this.updateQsState();
            }
        }, false);
    }

    /* access modifiers changed from: private */
    public void setOverScrolling(boolean z) {
        this.mStackScrollerOverscrolling = z;
        C0959QS qs = this.mQs;
        if (qs != null) {
            qs.setOverscrolling(z);
        }
    }

    private void onQsExpansionStarted() {
        onQsExpansionStarted(0);
    }

    /* access modifiers changed from: protected */
    public void onQsExpansionStarted(int i) {
        cancelQsAnimation();
        cancelHeightAnimator();
        float f = this.mQsExpansionHeight - ((float) i);
        setQsExpansion(f);
        requestPanelHeightUpdate();
        this.mNotificationStackScroller.checkSnoozeLeavebehind();
        if (f == 0.0f) {
            this.mStatusBar.requestFaceAuth();
        }
    }

    private void setQsExpanded(boolean z) {
        if (this.mQsExpanded != z) {
            this.mQsExpanded = z;
            OpMdmLogger.notifyQsExpanded(z);
            if (this.mQsExpanded && !this.mStatusBar.areLaunchAnimationsEnabled()) {
                OpMdmLogger.log("lock_quicksetting", "", "1");
            }
            updateQsState();
            requestPanelHeightUpdate();
            this.mFalsingManager.setQsExpanded(z);
            this.mStatusBar.setQsExpanded(z);
            this.mNotificationContainerParent.setQsExpanded(z);
        }
    }

    public void onStateChanged(int i) {
        long j;
        boolean goingToFullShade = this.mStatusBarStateController.goingToFullShade();
        boolean isKeyguardFadingAway = this.mKeyguardMonitor.isKeyguardFadingAway();
        int i2 = this.mBarState;
        boolean z = i == 1;
        setKeyguardStatusViewVisibility(i, isKeyguardFadingAway, goingToFullShade);
        setKeyguardBottomAreaVisibility(i, goingToFullShade);
        this.mBarState = i;
        this.mKeyguardShowing = z;
        C0959QS qs = this.mQs;
        if (qs != null) {
            qs.setKeyguardShowing(this.mKeyguardShowing);
        }
        if (i2 == 1 && (goingToFullShade || i == 2)) {
            animateKeyguardStatusBarOut();
            if (this.mBarState == 2) {
                j = 0;
            } else {
                j = this.mKeyguardMonitor.calculateGoingToFullShadeDelay();
            }
            this.mQs.animateHeaderSlidingIn(j);
        } else if (i2 == 2 && i == 1) {
            animateKeyguardStatusBarIn(360);
            if (!this.mQsExpanded) {
                this.mQs.animateHeaderSlidingOut();
            }
        } else {
            this.mKeyguardStatusBar.setAlpha(1.0f);
            this.mKeyguardStatusBar.setVisibility(z ? 0 : 4);
            if (z && i2 != this.mBarState) {
                C0959QS qs2 = this.mQs;
                if (qs2 != null) {
                    qs2.hideImmediately();
                }
            }
        }
        if (z) {
            updateDozingVisibilities(false);
        }
        maybeAnimateBottomAreaAlpha();
        resetHorizontalPanelPosition();
        updateQsState();
    }

    private void maybeAnimateBottomAreaAlpha() {
        this.mBottomAreaShadeAlphaAnimator.cancel();
        if (this.mBarState == 2) {
            this.mBottomAreaShadeAlphaAnimator.start();
        } else {
            this.mBottomAreaShadeAlpha = 1.0f;
        }
    }

    private void animateKeyguardStatusBarOut() {
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{this.mKeyguardStatusBar.getAlpha(), 0.0f});
        ofFloat.addUpdateListener(this.mStatusBarAnimateAlphaListener);
        ofFloat.setStartDelay(this.mKeyguardMonitor.isKeyguardFadingAway() ? this.mKeyguardMonitor.getKeyguardFadingAwayDelay() : 0);
        ofFloat.setDuration(this.mKeyguardMonitor.isKeyguardFadingAway() ? this.mKeyguardMonitor.getKeyguardFadingAwayDuration() / 2 : 360);
        ofFloat.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        ofFloat.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animator) {
                NotificationPanelView.this.mAnimateKeyguardStatusBarInvisibleEndRunnable.run();
            }
        });
        ofFloat.start();
    }

    private void animateKeyguardStatusBarIn(long j) {
        this.mKeyguardStatusBar.setVisibility(0);
        this.mKeyguardStatusBar.setAlpha(0.0f);
        ValueAnimator ofFloat = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        ofFloat.addUpdateListener(this.mStatusBarAnimateAlphaListener);
        ofFloat.setDuration(j);
        ofFloat.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        ofFloat.start();
    }

    private void setKeyguardBottomAreaVisibility(int i, boolean z) {
        this.mKeyguardBottomArea.animate().cancel();
        if (z) {
            this.mKeyguardBottomArea.animate().alpha(0.0f).setStartDelay(this.mKeyguardMonitor.getKeyguardFadingAwayDelay()).setDuration(this.mKeyguardMonitor.getKeyguardFadingAwayDuration() / 2).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(this.mAnimateKeyguardBottomAreaInvisibleEndRunnable).start();
        } else if (i == 1 || i == 2) {
            this.mKeyguardBottomArea.setVisibility(0);
            this.mKeyguardBottomArea.setAlpha(1.0f);
        } else {
            this.mKeyguardBottomArea.setVisibility(8);
        }
    }

    private void setKeyguardStatusViewVisibility(int i, boolean z, boolean z2) {
        this.mKeyguardStatusView.animate().cancel();
        this.mKeyguardStatusViewAnimating = false;
        if ((!z && this.mBarState == 1 && i != 1) || z2) {
            this.mKeyguardStatusViewAnimating = true;
            this.mKeyguardStatusView.animate().alpha(0.0f).setStartDelay(0).setDuration(160).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(this.mAnimateKeyguardStatusViewGoneEndRunnable);
            if (z) {
                this.mKeyguardStatusView.animate().setStartDelay(this.mKeyguardMonitor.getKeyguardFadingAwayDelay()).setDuration(this.mKeyguardMonitor.getKeyguardFadingAwayDuration() / 2).start();
            }
        } else if (this.mBarState == 2 && i == 1) {
            this.mKeyguardStatusView.setVisibility(0);
            this.mKeyguardStatusViewAnimating = true;
            this.mKeyguardStatusView.setAlpha(0.0f);
            this.mKeyguardStatusView.animate().alpha(1.0f).setStartDelay(0).setDuration(320).setInterpolator(Interpolators.ALPHA_IN).withEndAction(this.mAnimateKeyguardStatusViewVisibleEndRunnable);
        } else if (i != 1) {
            this.mKeyguardStatusView.setVisibility(8);
            this.mKeyguardStatusView.setAlpha(1.0f);
        } else if (z) {
            this.mKeyguardStatusViewAnimating = true;
            this.mKeyguardStatusView.animate().alpha(0.0f).translationYBy(((float) (-getHeight())) * 0.05f).setInterpolator(Interpolators.FAST_OUT_LINEAR_IN).setDuration(125).setStartDelay(0).withEndAction(this.mAnimateKeyguardStatusViewInvisibleEndRunnable).start();
        } else {
            this.mKeyguardStatusView.setVisibility(0);
            this.mKeyguardStatusView.setAlpha(1.0f);
        }
    }

    /* access modifiers changed from: private */
    public void updateQsState() {
        this.mNotificationStackScroller.setQsExpanded(this.mQsExpanded);
        this.mNotificationStackScroller.setScrollingEnabled(this.mBarState != 1 && (!this.mQsExpanded || this.mQsExpansionFromOverscroll));
        updateEmptyShadeView();
        KeyguardUserSwitcher keyguardUserSwitcher = this.mKeyguardUserSwitcher;
        if (keyguardUserSwitcher != null && this.mQsExpanded && !this.mStackScrollerOverscrolling) {
            keyguardUserSwitcher.hideIfNotSimple(true);
        }
        C0959QS qs = this.mQs;
        if (qs != null) {
            qs.setExpanded(this.mQsExpanded);
        }
    }

    private void setQsExpansion(float f) {
        C0959QS qs = this.mQs;
        if (qs instanceof QSFragment) {
            ((QSFragment) qs).setExpansionHight(f);
        }
        float min = Math.min(Math.max(f, (float) this.mQsMinExpansionHeight), (float) this.mQsMaxExpansionHeight);
        int i = this.mQsMaxExpansionHeight;
        this.mQsFullyExpanded = min == ((float) i) && i != 0;
        if (min > ((float) this.mQsMinExpansionHeight) && !this.mQsExpanded && !this.mStackScrollerOverscrolling) {
            setQsExpanded(true);
        } else if (min <= ((float) this.mQsMinExpansionHeight) && this.mQsExpanded) {
            setQsExpanded(false);
        }
        this.mQsExpansionHeight = min;
        updateQsExpansion();
        requestScrollerTopPaddingUpdate(false);
        updateHeaderKeyguardAlpha();
        int i2 = this.mBarState;
        if (i2 == 2 || i2 == 1) {
            updateKeyguardBottomAreaAlpha();
            updateBigClockAlpha();
        }
        if (this.mAccessibilityManager.isEnabled()) {
            setAccessibilityPaneTitle(determineAccessibilityPaneTitle());
        }
        if (!this.mFalsingManager.isUnlockingDisabled() && this.mQsFullyExpanded && this.mFalsingManager.shouldEnforceBouncer()) {
            this.mStatusBar.executeRunnableDismissingKeyguard(null, null, false, true, false);
        }
        PanelExpansionListener panelExpansionListener = this.mExpansionListener;
        if (panelExpansionListener != null) {
            int i3 = this.mQsMaxExpansionHeight;
            panelExpansionListener.onQsExpansionChanged(i3 != 0 ? this.mQsExpansionHeight / ((float) i3) : 0.0f);
        }
    }

    /* access modifiers changed from: protected */
    public void updateQsExpansion() {
        if (this.mQs != null) {
            float qsExpansionFraction = getQsExpansionFraction();
            this.mQs.setQsExpansion(qsExpansionFraction, getHeaderTranslation());
            this.mNotificationStackScroller.setQsExpansionFraction(qsExpansionFraction);
        }
    }

    private String determineAccessibilityPaneTitle() {
        C0959QS qs = this.mQs;
        if (qs != null && qs.isCustomizing()) {
            return getContext().getString(R$string.accessibility_desc_quick_settings_edit);
        }
        if (this.mQsExpansionHeight != 0.0f && this.mQsFullyExpanded) {
            return getContext().getString(R$string.accessibility_desc_quick_settings);
        }
        if (this.mBarState == 1) {
            return getContext().getString(R$string.accessibility_desc_lock_screen);
        }
        return getContext().getString(R$string.accessibility_desc_notification_shade);
    }

    private float calculateQsTopPadding() {
        if (!this.mKeyguardShowing || (!this.mQsExpandImmediate && (!this.mIsExpanding || !this.mQsExpandedWhenExpandingStarted))) {
            ValueAnimator valueAnimator = this.mQsSizeChangeAnimator;
            if (valueAnimator != null) {
                return (float) ((Integer) valueAnimator.getAnimatedValue()).intValue();
            }
            if (this.mKeyguardShowing) {
                return MathUtils.lerp((float) this.mNotificationStackScroller.getIntrinsicPadding(), (float) (this.mQsMaxExpansionHeight + this.mQsNotificationTopPadding), getQsExpansionFraction());
            }
            return this.mQsExpansionHeight + ((float) this.mQsNotificationTopPadding);
        }
        int i = this.mClockPositionResult.stackScrollerPadding;
        int i2 = this.mQsMaxExpansionHeight + this.mQsNotificationTopPadding;
        if (this.mBarState == 1) {
            i2 = Math.max(i, i2);
        }
        return (float) ((int) MathUtils.lerp((float) this.mQsMinExpansionHeight, (float) i2, getExpandedFraction()));
    }

    /* access modifiers changed from: protected */
    public void requestScrollerTopPaddingUpdate(boolean z) {
        this.mNotificationStackScroller.updateTopPadding(calculateQsTopPadding(), z, this.mKeyguardShowing && (this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted)));
    }

    private void trackMovement(MotionEvent motionEvent) {
        VelocityTracker velocityTracker = this.mQsVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.addMovement(motionEvent);
        }
        this.mLastTouchX = motionEvent.getX();
        this.mLastTouchY = motionEvent.getY();
    }

    private void initVelocityTracker() {
        VelocityTracker velocityTracker = this.mQsVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.recycle();
        }
        this.mQsVelocityTracker = VelocityTracker.obtain();
    }

    private float getCurrentQSVelocity() {
        VelocityTracker velocityTracker = this.mQsVelocityTracker;
        if (velocityTracker == null) {
            return 0.0f;
        }
        velocityTracker.computeCurrentVelocity(1000);
        return this.mQsVelocityTracker.getYVelocity();
    }

    private void cancelQsAnimation() {
        ValueAnimator valueAnimator = this.mQsExpansionAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
    }

    public void flingSettings(float f, int i) {
        flingSettings(f, i, null, false);
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x001a  */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x0014  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void flingSettings(float r7, int r8, final java.lang.Runnable r9, boolean r10) {
        /*
            r6 = this;
            r0 = 0
            r1 = 1
            if (r8 == 0) goto L_0x000b
            if (r8 == r1) goto L_0x0008
            r2 = r0
            goto L_0x000e
        L_0x0008:
            int r2 = r6.mQsMinExpansionHeight
            goto L_0x000d
        L_0x000b:
            int r2 = r6.mQsMaxExpansionHeight
        L_0x000d:
            float r2 = (float) r2
        L_0x000e:
            float r3 = r6.mQsExpansionHeight
            int r3 = (r2 > r3 ? 1 : (r2 == r3 ? 0 : -1))
            if (r3 != 0) goto L_0x001a
            if (r9 == 0) goto L_0x0019
            r9.run()
        L_0x0019:
            return
        L_0x001a:
            r3 = 0
            if (r8 != 0) goto L_0x001f
            r8 = r1
            goto L_0x0020
        L_0x001f:
            r8 = r3
        L_0x0020:
            int r4 = (r7 > r0 ? 1 : (r7 == r0 ? 0 : -1))
            if (r4 <= 0) goto L_0x0026
            if (r8 == 0) goto L_0x002c
        L_0x0026:
            int r4 = (r7 > r0 ? 1 : (r7 == r0 ? 0 : -1))
            if (r4 >= 0) goto L_0x002f
            if (r8 == 0) goto L_0x002f
        L_0x002c:
            r7 = r0
            r0 = r1
            goto L_0x0030
        L_0x002f:
            r0 = r3
        L_0x0030:
            r4 = 2
            float[] r4 = new float[r4]
            float r5 = r6.mQsExpansionHeight
            r4[r3] = r5
            r4[r1] = r2
            android.animation.ValueAnimator r1 = android.animation.ValueAnimator.ofFloat(r4)
            if (r10 == 0) goto L_0x004a
            android.view.animation.Interpolator r7 = com.android.systemui.Interpolators.TOUCH_RESPONSE
            r1.setInterpolator(r7)
            r2 = 368(0x170, double:1.82E-321)
            r1.setDuration(r2)
            goto L_0x0051
        L_0x004a:
            com.android.systemui.statusbar.FlingAnimationUtils r10 = r6.mFlingAnimationUtils
            float r3 = r6.mQsExpansionHeight
            r10.apply(r1, r3, r2, r7)
        L_0x0051:
            if (r0 == 0) goto L_0x0058
            r2 = 350(0x15e, double:1.73E-321)
            r1.setDuration(r2)
        L_0x0058:
            com.android.systemui.statusbar.phone.-$$Lambda$NotificationPanelView$awsgvWVf3VvQ3psyX9_x0iLYPhM r7 = new com.android.systemui.statusbar.phone.-$$Lambda$NotificationPanelView$awsgvWVf3VvQ3psyX9_x0iLYPhM
            r7.<init>()
            r1.addUpdateListener(r7)
            com.android.systemui.statusbar.phone.NotificationPanelView$13 r7 = new com.android.systemui.statusbar.phone.NotificationPanelView$13
            r7.<init>(r9)
            r1.addListener(r7)
            r1.start()
            r6.mQsExpansionAnimator = r1
            r6.mQsAnimatorExpand = r8
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NotificationPanelView.flingSettings(float, int, java.lang.Runnable, boolean):void");
    }

    public /* synthetic */ void lambda$flingSettings$1$NotificationPanelView(ValueAnimator valueAnimator) {
        setQsExpansion(((Float) valueAnimator.getAnimatedValue()).floatValue());
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0050 A[ADDED_TO_REGION] */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x005f A[RETURN] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean shouldQuickSettingsIntercept(float r6, float r7, float r8) {
        /*
            r5 = this;
            boolean r0 = r5.mQsExpansionEnabled
            r1 = 0
            if (r0 == 0) goto L_0x0060
            boolean r0 = r5.mCollapsedOnDown
            if (r0 == 0) goto L_0x000a
            goto L_0x0060
        L_0x000a:
            boolean r0 = r5.mKeyguardShowing
            if (r0 != 0) goto L_0x0018
            com.android.systemui.plugins.qs.QS r0 = r5.mQs
            if (r0 != 0) goto L_0x0013
            goto L_0x0018
        L_0x0013:
            android.view.View r0 = r0.getHeader()
            goto L_0x001a
        L_0x0018:
            com.android.systemui.statusbar.phone.KeyguardStatusBarView r0 = r5.mKeyguardStatusBar
        L_0x001a:
            android.widget.FrameLayout r2 = r5.mQsFrame
            float r2 = r2.getX()
            int r2 = (r6 > r2 ? 1 : (r6 == r2 ? 0 : -1))
            r3 = 1
            if (r2 < 0) goto L_0x004b
            android.widget.FrameLayout r2 = r5.mQsFrame
            float r2 = r2.getX()
            android.widget.FrameLayout r4 = r5.mQsFrame
            int r4 = r4.getWidth()
            float r4 = (float) r4
            float r2 = r2 + r4
            int r2 = (r6 > r2 ? 1 : (r6 == r2 ? 0 : -1))
            if (r2 > 0) goto L_0x004b
            int r2 = r0.getTop()
            float r2 = (float) r2
            int r2 = (r7 > r2 ? 1 : (r7 == r2 ? 0 : -1))
            if (r2 < 0) goto L_0x004b
            int r0 = r0.getBottom()
            float r0 = (float) r0
            int r0 = (r7 > r0 ? 1 : (r7 == r0 ? 0 : -1))
            if (r0 > 0) goto L_0x004b
            r0 = r3
            goto L_0x004c
        L_0x004b:
            r0 = r1
        L_0x004c:
            boolean r2 = r5.mQsExpanded
            if (r2 == 0) goto L_0x005f
            if (r0 != 0) goto L_0x005d
            r0 = 0
            int r8 = (r8 > r0 ? 1 : (r8 == r0 ? 0 : -1))
            if (r8 >= 0) goto L_0x005e
            boolean r5 = r5.isInQsArea(r6, r7)
            if (r5 == 0) goto L_0x005e
        L_0x005d:
            r1 = r3
        L_0x005e:
            return r1
        L_0x005f:
            return r0
        L_0x0060:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NotificationPanelView.shouldQuickSettingsIntercept(float, float, float):boolean");
    }

    /* access modifiers changed from: protected */
    public boolean isScrolledToBottom() {
        if (isInSettings() || this.mBarState == 1 || this.mNotificationStackScroller.isScrolledToBottom()) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public int getMaxPanelHeight() {
        int i;
        int i2 = this.mStatusBarMinHeight;
        if (this.mBarState != 1 && this.mNotificationStackScroller.getNotGoneChildCount() == 0) {
            i2 = Math.max(i2, (int) (((float) this.mQsMinExpansionHeight) + getOverExpansionAmount()));
        }
        if (this.mQsExpandImmediate || this.mQsExpanded || ((this.mIsExpanding && this.mQsExpandedWhenExpandingStarted) || this.mPulsing)) {
            i = calculatePanelHeightQsExpanded();
        } else {
            i = calculatePanelHeightShade();
        }
        return Math.max(i, i2);
    }

    public boolean isInSettings() {
        return this.mQsExpanded;
    }

    public boolean isExpanding() {
        return this.mIsExpanding;
    }

    /* access modifiers changed from: protected */
    public void onHeightUpdated(float f) {
        float f2;
        if ((!this.mQsExpanded || this.mQsExpandImmediate || (this.mIsExpanding && this.mQsExpandedWhenExpandingStarted)) && this.mStackScrollerMeasuringPass <= 2) {
            positionClockAndNotifications();
        }
        if (this.mQsExpandImmediate || (this.mQsExpanded && !this.mQsTracking && this.mQsExpansionAnimator == null && !this.mQsExpansionFromOverscroll)) {
            if (this.mKeyguardShowing) {
                f2 = f / ((float) getMaxPanelHeight());
            } else {
                float intrinsicPadding = (float) (this.mNotificationStackScroller.getIntrinsicPadding() + this.mNotificationStackScroller.getLayoutMinHeight());
                f2 = (f - intrinsicPadding) / (((float) calculatePanelHeightQsExpanded()) - intrinsicPadding);
            }
            int i = this.mQsMinExpansionHeight;
            setQsExpansion(((float) i) + (f2 * ((float) (this.mQsMaxExpansionHeight - i))));
        }
        updateExpandedHeight(f);
        updateHeader();
        updateNotificationTranslucency();
        updatePanelExpanded();
    }

    private void updatePanelExpanded() {
        boolean z = !isFullyCollapsed();
        if (this.mPanelExpanded != z) {
            this.mHeadsUpManager.setIsPanelExpanded(z);
            this.mStatusBar.setPanelExpanded(z);
            this.mPanelExpanded = z;
        }
    }

    private int calculatePanelHeightShade() {
        int emptyBottomMargin = this.mNotificationStackScroller.getEmptyBottomMargin();
        int height = (int) (((float) (this.mNotificationStackScroller.getHeight() - emptyBottomMargin)) + this.mNotificationStackScroller.getTopPaddingOverflow());
        if (Build.DEBUG_ONEPLUS) {
            String str = OpNotificationPanelView.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append(" calculatePanelHeightShade maxHeight:");
            sb.append(height);
            sb.append(" getHeight:");
            sb.append(this.mNotificationStackScroller.getHeight());
            sb.append(" emptyBottomMargin:");
            sb.append(emptyBottomMargin);
            sb.append(" getTopPaddingOverflow:");
            sb.append(this.mNotificationStackScroller.getTopPaddingOverflow());
            Log.i(str, sb.toString());
        }
        return this.mBarState == 1 ? Math.max(height, this.mClockPositionAlgorithm.getExpandedClockPosition() + this.mKeyguardStatusView.getHeight() + this.mNotificationStackScroller.getIntrinsicContentHeight()) : height;
    }

    private int calculatePanelHeightQsExpanded() {
        float height = (float) ((this.mNotificationStackScroller.getHeight() - this.mNotificationStackScroller.getEmptyBottomMargin()) - this.mNotificationStackScroller.getTopPadding());
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0 && this.mShowEmptyShadeView) {
            height = (float) this.mNotificationStackScroller.getEmptyShadeViewHeight();
        }
        int i = this.mQsMaxExpansionHeight;
        if (this.mKeyguardShowing) {
            i += this.mQsNotificationTopPadding;
        }
        ValueAnimator valueAnimator = this.mQsSizeChangeAnimator;
        if (valueAnimator != null) {
            i = ((Integer) valueAnimator.getAnimatedValue()).intValue();
        }
        float max = ((float) Math.max(i, this.mBarState == 1 ? this.mClockPositionResult.stackScrollerPadding : 0)) + height + this.mNotificationStackScroller.getTopPaddingOverflow();
        if (max > ((float) this.mNotificationStackScroller.getHeight())) {
            max = Math.max((float) (i + this.mNotificationStackScroller.getLayoutMinHeight()), (float) this.mNotificationStackScroller.getHeight());
        }
        return (int) max;
    }

    private void updateNotificationTranslucency() {
        float fadeoutAlpha = (!this.mClosingWithAlphaFadeOut || this.mExpandingFromHeadsUp || this.mHeadsUpManager.hasPinnedHeadsUp()) ? 1.0f : getFadeoutAlpha();
        if (this.mBarState == 1 && !this.mHintAnimationRunning) {
            fadeoutAlpha *= this.mClockPositionResult.clockAlpha;
        }
        this.mNotificationStackScroller.setAlpha(fadeoutAlpha);
    }

    private float getFadeoutAlpha() {
        return (float) Math.pow((double) Math.max(0.0f, Math.min((getNotificationsTopY() + ((float) this.mNotificationStackScroller.getFirstItemMinHeight())) / ((float) this.mQsMinExpansionHeight), 1.0f)), 0.75d);
    }

    /* access modifiers changed from: protected */
    public float getOverExpansionAmount() {
        return this.mNotificationStackScroller.getCurrentOverScrollAmount(true);
    }

    /* access modifiers changed from: protected */
    public float getOverExpansionPixels() {
        return this.mNotificationStackScroller.getCurrentOverScrolledPixels(true);
    }

    private void updateHeader() {
        if (this.mBarState == 1) {
            updateHeaderKeyguardAlpha();
        }
        updateQsExpansion();
    }

    /* access modifiers changed from: protected */
    public float getHeaderTranslation() {
        if (this.mBarState == 1) {
            return 0.0f;
        }
        return Math.min(0.0f, MathUtils.lerp((float) (-this.mQsMinExpansionHeight), 0.0f, Math.min(1.0f, this.mNotificationStackScroller.getAppearFraction(this.mExpandedHeight))) + this.mExpandOffset);
    }

    private float getKeyguardContentsAlpha() {
        float f;
        float f2;
        if (this.mBarState == 1) {
            f2 = getNotificationsTopY();
            f = (float) (this.mKeyguardStatusBar.getHeight() + this.mNotificationsHeaderCollideDistance);
        } else {
            f2 = getNotificationsTopY();
            f = (float) this.mKeyguardStatusBar.getHeight();
        }
        return (float) Math.pow((double) MathUtils.constrain(f2 / f, 0.0f, 1.0f), 0.75d);
    }

    /* access modifiers changed from: private */
    public void updateHeaderKeyguardAlpha() {
        if (this.mKeyguardShowing) {
            float min = Math.min(getKeyguardContentsAlpha(), 1.0f - Math.min(1.0f, getQsExpansionFraction() * 2.0f)) * this.mKeyguardStatusBarAnimateAlpha;
            this.mKeyguardStatusBar.setAlpha(min);
            this.mKeyguardStatusBar.setVisibility((min == 0.0f || this.mDozing) ? 4 : 0);
        }
    }

    private void updateKeyguardBottomAreaAlpha() {
        float min = Math.min(MathUtils.map(isUnlockHintRunning() ? 0.0f : 0.95f, 1.0f, 0.0f, 1.0f, getExpandedFraction()), 1.0f - getQsExpansionFraction()) * this.mBottomAreaShadeAlpha;
        this.mKeyguardBottomArea.setAffordanceAlpha(min);
        this.mKeyguardBottomArea.setImportantForAccessibility(min == 0.0f ? 4 : 0);
        View ambientIndicationContainer = this.mStatusBar.getAmbientIndicationContainer();
        if (ambientIndicationContainer != null) {
            ambientIndicationContainer.setAlpha(min);
        }
    }

    private void updateBigClockAlpha() {
        this.mBigClockContainer.setAlpha(Math.min(MathUtils.map(isUnlockHintRunning() ? 0.0f : 0.95f, 1.0f, 0.0f, 1.0f, getExpandedFraction()), 1.0f - getQsExpansionFraction()));
    }

    private float getNotificationsTopY() {
        if (this.mNotificationStackScroller.getNotGoneChildCount() == 0) {
            return getExpandedHeight();
        }
        return this.mNotificationStackScroller.getNotificationsTopY();
    }

    /* access modifiers changed from: protected */
    public void onExpandingStarted() {
        super.onExpandingStarted();
        this.mNotificationStackScroller.onExpansionStarted();
        this.mIsExpanding = true;
        this.mQsExpandedWhenExpandingStarted = this.mQsFullyExpanded;
        if (this.mQsExpanded) {
            onQsExpansionStarted();
        }
        C0959QS qs = this.mQs;
        if (qs != null) {
            qs.setHeaderListening(true);
        }
    }

    /* access modifiers changed from: protected */
    public void onExpandingFinished() {
        super.onExpandingFinished();
        this.mNotificationStackScroller.onExpansionStopped();
        this.mHeadsUpManager.onExpandingFinished();
        this.mIsExpanding = false;
        if (isFullyCollapsed()) {
            OpMdmLogger.notifyNpvExpanded(false);
            DejankUtils.postAfterTraversal(new Runnable() {
                public void run() {
                    NotificationPanelView.this.setListening(false);
                }
            });
            postOnAnimation(new Runnable() {
                public void run() {
                    NotificationPanelView.this.getParent().invalidateChild(NotificationPanelView.this, NotificationPanelView.mDummyDirtyRect);
                }
            });
        } else {
            OpMdmLogger.notifyNpvExpanded(true);
            setListening(true);
        }
        this.mQsExpandImmediate = false;
        this.mNotificationStackScroller.setShouldShowShelfOnly(false);
        this.mTwoFingerQsExpandPossible = false;
        this.mIsExpansionFromHeadsUp = false;
        notifyListenersTrackingHeadsUp(null);
        this.mExpandingFromHeadsUp = false;
        setPanelScrimMinFraction(0.0f);
    }

    private void notifyListenersTrackingHeadsUp(ExpandableNotificationRow expandableNotificationRow) {
        for (int i = 0; i < this.mTrackingHeadsUpListeners.size(); i++) {
            ((Consumer) this.mTrackingHeadsUpListeners.get(i)).accept(expandableNotificationRow);
        }
    }

    /* access modifiers changed from: private */
    public void setListening(boolean z) {
        this.mKeyguardStatusBar.setListening(z);
        C0959QS qs = this.mQs;
        if (qs != null) {
            qs.setListening(z);
        }
    }

    public void expand(boolean z) {
        super.expand(z);
        setListening(true);
    }

    /* access modifiers changed from: protected */
    public void setOverExpansion(float f, boolean z) {
        if (!this.mConflictingQsExpansionGesture && !this.mQsExpandImmediate && this.mBarState != 1) {
            this.mNotificationStackScroller.setOnHeightChangedListener(null);
            if (z) {
                this.mNotificationStackScroller.setOverScrolledPixels(f, true, false);
            } else {
                this.mNotificationStackScroller.setOverScrollAmount(f, true, false);
            }
            this.mNotificationStackScroller.setOnHeightChangedListener(this);
        }
    }

    /* access modifiers changed from: protected */
    public void onTrackingStarted() {
        this.mFalsingManager.onTrackingStarted(this.mStatusBar.isKeyguardCurrentlySecure());
        super.onTrackingStarted();
        if (this.mQsFullyExpanded) {
            this.mQsExpandImmediate = true;
            this.mNotificationStackScroller.setShouldShowShelfOnly(true);
        }
        int i = this.mBarState;
        if (i == 1 || i == 2) {
            this.mAffordanceHelper.animateHideLeftRightIcon();
        }
        this.mNotificationStackScroller.onPanelTrackingStarted();
    }

    /* access modifiers changed from: protected */
    public void onTrackingStopped(boolean z) {
        this.mFalsingManager.onTrackingStopped();
        super.onTrackingStopped(z);
        if (z) {
            this.mNotificationStackScroller.setOverScrolledPixels(0.0f, true, true);
        }
        this.mNotificationStackScroller.onPanelTrackingStopped();
        if (z) {
            int i = this.mBarState;
            if ((i == 1 || i == 2) && !this.mHintAnimationRunning) {
                this.mAffordanceHelper.reset(true);
            }
        }
    }

    public void onHeightChanged(ExpandableView expandableView, boolean z) {
        if (expandableView != null || !this.mQsExpanded) {
            if (z && this.mInterpolatedDarkAmount == 0.0f) {
                this.mAnimateNextPositionUpdate = true;
            }
            ExpandableView firstChildNotGone = this.mNotificationStackScroller.getFirstChildNotGone();
            ExpandableNotificationRow expandableNotificationRow = firstChildNotGone instanceof ExpandableNotificationRow ? (ExpandableNotificationRow) firstChildNotGone : null;
            if (expandableNotificationRow != null && (expandableView == expandableNotificationRow || expandableNotificationRow.getNotificationParent() == expandableNotificationRow)) {
                requestScrollerTopPaddingUpdate(false);
            }
            requestPanelHeightUpdate();
        }
    }

    public void onQsHeightChanged() {
        C0959QS qs = this.mQs;
        this.mQsMaxExpansionHeight = qs != null ? qs.getDesiredHeight() : 0;
        if (this.mQsExpanded && this.mQsFullyExpanded) {
            this.mQsExpansionHeight = (float) this.mQsMaxExpansionHeight;
            requestScrollerTopPaddingUpdate(false);
            requestPanelHeightUpdate();
        }
        if (this.mAccessibilityManager.isEnabled()) {
            setAccessibilityPaneTitle(determineAccessibilityPaneTitle());
        }
        this.mNotificationStackScroller.setMaxTopPadding(this.mQsMaxExpansionHeight + this.mQsNotificationTopPadding);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mAffordanceHelper.onConfigurationChanged();
        if (configuration.orientation != this.mLastOrientation) {
            resetHorizontalPanelPosition();
        }
        this.mLastOrientation = configuration.orientation;
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        this.mNavigationBarBottomHeight = windowInsets.getStableInsetBottom();
        updateMaxHeadsUpTranslation();
        return windowInsets;
    }

    private void updateMaxHeadsUpTranslation() {
        this.mNotificationStackScroller.setHeadsUpBoundaries(getHeight(), this.mNavigationBarBottomHeight);
    }

    public void onRtlPropertiesChanged(int i) {
        if (i != this.mOldLayoutDirection) {
            this.mAffordanceHelper.onRtlPropertiesChanged();
            this.mOldLayoutDirection = i;
        }
    }

    public void onClick(View view) {
        onQsExpansionStarted();
        if (this.mQsExpanded) {
            flingSettings(0.0f, 1, null, true);
        } else if (this.mQsExpansionEnabled) {
            this.mLockscreenGestureLogger.write(195, 0, 0);
            flingSettings(0.0f, 0, null, true);
        }
    }

    public void onAnimationToSideStarted(boolean z, float f, float f2) {
        if (getLayoutDirection() != 1) {
            z = !z;
        }
        this.mIsLaunchTransitionRunning = true;
        this.mLaunchAnimationEndRunnable = null;
        float displayDensity = this.mStatusBar.getDisplayDensity();
        int abs = Math.abs((int) (f / displayDensity));
        int abs2 = Math.abs((int) (f2 / displayDensity));
        if (z) {
            this.mLockscreenGestureLogger.write(190, abs, abs2);
            this.mFalsingManager.onLeftAffordanceOn();
            if (this.mKeyguardBottomArea.isLeftVoiceAssist()) {
                KeyguardUpdateMonitor.getInstance(this.mContext).updateLaunchingLeftAffordance(true);
            }
            if (this.mFalsingManager.shouldEnforceBouncer()) {
                this.mStatusBar.executeRunnableDismissingKeyguard(new Runnable() {
                    public void run() {
                        NotificationPanelView.this.mKeyguardBottomArea.launchLeftAffordance();
                    }
                }, null, true, false, true);
            } else {
                this.mKeyguardBottomArea.launchLeftAffordance();
            }
        } else {
            if ("lockscreen_affordance".equals(this.mLastCameraLaunchSource)) {
                this.mHandler.removeCallbacks(this.mUpdateCameraStateTimeout);
                KeyguardUpdateMonitor.getInstance(this.mContext).updateLaunchingCameraState(true);
                this.mHandler.postDelayed(this.mUpdateCameraStateTimeout, 2000);
                this.mLockscreenGestureLogger.write(189, abs, abs2);
                OpMdmLogger.log("lock_camera", "", "1");
            }
            this.mFalsingManager.onCameraOn();
            this.mStatusBar.notifyCameraLaunching(null);
            if (this.mFalsingManager.shouldEnforceBouncer()) {
                this.mStatusBar.executeRunnableDismissingKeyguard(new Runnable() {
                    public void run() {
                        NotificationPanelView notificationPanelView = NotificationPanelView.this;
                        notificationPanelView.mKeyguardBottomArea.launchCamera(notificationPanelView.mLastCameraLaunchSource);
                    }
                }, null, true, false, true);
            } else {
                this.mKeyguardBottomArea.launchCamera(this.mLastCameraLaunchSource);
            }
        }
        if (!this.mStatusBar.isOccluded()) {
            this.mStatusBar.startLaunchTransitionTimeout();
        } else {
            Log.w(OpNotificationPanelView.TAG, "do not start launch transition timeout");
        }
        this.mBlockTouches = true;
    }

    public void onAnimationToSideEnded() {
        this.mIsLaunchTransitionRunning = false;
        this.mIsLaunchTransitionFinished = true;
        Runnable runnable = this.mLaunchAnimationEndRunnable;
        if (runnable != null) {
            runnable.run();
            this.mLaunchAnimationEndRunnable = null;
        }
        this.mStatusBar.readyForKeyguardDone();
    }

    /* access modifiers changed from: protected */
    public void startUnlockHintAnimation() {
        if (this.mPowerManager.isPowerSaveMode()) {
            onUnlockHintStarted();
            onUnlockHintFinished();
            return;
        }
        super.startUnlockHintAnimation();
    }

    public float getMaxTranslationDistance() {
        return (float) Math.hypot((double) getWidth(), (double) getHeight());
    }

    public void onSwipingStarted(boolean z) {
        this.mFalsingManager.onAffordanceSwipingStarted(z);
        if (getLayoutDirection() == 1) {
            z = !z;
        }
        if (z) {
            this.mKeyguardBottomArea.bindCameraPrewarmService();
        }
        requestDisallowInterceptTouchEvent(true);
        this.mOnlyAffordanceInThisMotion = true;
        this.mQsTracking = false;
    }

    public void onSwipingAborted() {
        this.mFalsingManager.onAffordanceSwipingAborted();
        this.mKeyguardBottomArea.unbindCameraPrewarmService(false);
    }

    public void onIconClicked(boolean z) {
        if (!this.mHintAnimationRunning) {
            this.mHintAnimationRunning = true;
            this.mAffordanceHelper.startHintAnimation(z, new Runnable() {
                public void run() {
                    NotificationPanelView notificationPanelView = NotificationPanelView.this;
                    notificationPanelView.mHintAnimationRunning = false;
                    notificationPanelView.mStatusBar.onHintFinished();
                }
            });
            if (getLayoutDirection() == 1) {
                z = !z;
            }
            if (z) {
                this.mStatusBar.onCameraHintStarted();
            } else if (this.mKeyguardBottomArea.isLeftVoiceAssist()) {
                this.mStatusBar.onVoiceAssistHintStarted();
            } else {
                this.mStatusBar.onPhoneHintStarted();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onUnlockHintFinished() {
        super.onUnlockHintFinished();
        this.mNotificationStackScroller.setUnlockHintRunning(false);
    }

    /* access modifiers changed from: protected */
    public void onUnlockHintStarted() {
        super.onUnlockHintStarted();
        this.mNotificationStackScroller.setUnlockHintRunning(true);
    }

    public KeyguardAffordanceView getLeftIcon() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getRightView();
        }
        return this.mKeyguardBottomArea.getLeftView();
    }

    public KeyguardAffordanceView getRightIcon() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getLeftView();
        }
        return this.mKeyguardBottomArea.getRightView();
    }

    public View getLeftPreview() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getRightPreview();
        }
        return this.mKeyguardBottomArea.getLeftPreview();
    }

    public View getRightPreview() {
        if (getLayoutDirection() == 1) {
            return this.mKeyguardBottomArea.getLeftPreview();
        }
        return this.mKeyguardBottomArea.getRightPreview();
    }

    public float getAffordanceFalsingFactor() {
        return this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
    }

    public boolean needsAntiFalsing() {
        return this.mBarState == 1;
    }

    /* access modifiers changed from: protected */
    public boolean shouldUseDismissingAnimation() {
        return this.mBarState != 0 && (!this.mStatusBar.isKeyguardCurrentlySecure() || !isTracking());
    }

    /* access modifiers changed from: protected */
    public boolean fullyExpandedClearAllVisible() {
        return this.mNotificationStackScroller.isFooterViewNotGone() && this.mNotificationStackScroller.isScrolledToBottom() && !this.mQsExpandImmediate;
    }

    /* access modifiers changed from: protected */
    public boolean isClearAllVisible() {
        return this.mNotificationStackScroller.isFooterViewContentVisible();
    }

    /* access modifiers changed from: protected */
    public int getClearAllHeight() {
        return this.mNotificationStackScroller.getFooterViewHeight();
    }

    /* access modifiers changed from: protected */
    public boolean isTrackingBlocked() {
        return (this.mConflictingQsExpansionGesture && this.mQsExpanded) || this.mBlockingExpansionForCurrentTouch;
    }

    public boolean isQsExpanded() {
        return this.mQsExpanded;
    }

    public boolean isQsDetailShowing() {
        return this.mQs.isShowingDetail();
    }

    public void closeQsDetail() {
        this.mQs.closeDetail();
    }

    public boolean isLaunchTransitionFinished() {
        return this.mIsLaunchTransitionFinished;
    }

    public boolean isLaunchTransitionRunning() {
        return this.mIsLaunchTransitionRunning;
    }

    public void setLaunchTransitionEndRunnable(Runnable runnable) {
        this.mLaunchAnimationEndRunnable = runnable;
    }

    public void setEmptyDragAmount(float f) {
        this.mEmptyDragAmount = f * 0.2f;
        positionClockAndNotifications();
    }

    private void updateDozingVisibilities(boolean z) {
        this.mKeyguardBottomArea.setDozing(this.mDozing, z);
        if (!this.mDozing && z) {
            animateKeyguardStatusBarIn(360);
        }
    }

    public boolean isDozing() {
        return this.mDozing;
    }

    public void showEmptyShadeView(boolean z) {
        this.mShowEmptyShadeView = z;
        updateEmptyShadeView();
    }

    private void updateEmptyShadeView() {
        this.mNotificationStackScroller.updateEmptyShadeView(this.mShowEmptyShadeView && !this.mQsExpanded);
    }

    public void setQsScrimEnabled(boolean z) {
        boolean z2 = this.mQsScrimEnabled != z;
        this.mQsScrimEnabled = z;
        if (z2) {
            updateQsState();
        }
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        this.mKeyguardUserSwitcher = keyguardUserSwitcher;
    }

    public void onScreenTurningOn() {
        this.mKeyguardStatusView.dozeTimeTick();
    }

    public void onEmptySpaceClicked(float f, float f2) {
        onEmptySpaceClick(f);
    }

    /* access modifiers changed from: protected */
    public boolean onMiddleClicked() {
        int i = this.mBarState;
        if (i == 0) {
            post(this.mPostCollapseRunnable);
            return false;
        } else if (i != 1) {
            if (i == 2 && !this.mQsExpanded) {
                this.mShadeController.goToKeyguard();
            }
            return true;
        } else {
            if (!this.mDozingOnDown) {
                this.mLockscreenGestureLogger.write(188, 0, 0);
                startUnlockHintAnimation();
            }
            return true;
        }
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mCurrentPanelAlpha != 255) {
            canvas.drawRect(0.0f, 0.0f, (float) canvas.getWidth(), (float) canvas.getHeight(), this.mAlphaPaint);
        }
    }

    public float getCurrentPanelAlpha() {
        return (float) this.mCurrentPanelAlpha;
    }

    public boolean setPanelAlpha(int i, boolean z) {
        if (this.mPanelAlpha == i) {
            return false;
        }
        this.mPanelAlpha = i;
        PropertyAnimator.setProperty(this, this.PANEL_ALPHA, (float) i, i == 255 ? this.PANEL_ALPHA_IN_PROPERTIES : this.PANEL_ALPHA_OUT_PROPERTIES, z);
        return true;
    }

    public void setPanelAlphaInternal(float f) {
        this.mCurrentPanelAlpha = (int) f;
        this.mAlphaPaint.setARGB(this.mCurrentPanelAlpha, 255, 255, 255);
        invalidate();
    }

    public void setPanelAlphaEndAction(Runnable runnable) {
        this.mPanelAlphaEndAction = runnable;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void onHeadsUpPinnedModeChanged(boolean z) {
        this.mNotificationStackScroller.setInHeadsUpPinnedMode(z);
        if (z) {
            this.mHeadsUpExistenceChangedRunnable.run();
            updateNotificationTranslucency();
            return;
        }
        setHeadsUpAnimatingAway(true);
        this.mNotificationStackScroller.runAfterAnimationFinished(this.mHeadsUpExistenceChangedRunnable);
    }

    public void setHeadsUpAnimatingAway(boolean z) {
        this.mHeadsUpAnimatingAway = z;
        this.mNotificationStackScroller.setHeadsUpAnimatingAway(z);
    }

    public void onHeadsUpPinned(NotificationEntry notificationEntry) {
        this.mNotificationStackScroller.generateHeadsUpAnimation(notificationEntry.getHeadsUpAnimationView(), true);
    }

    public void onHeadsUpUnPinned(NotificationEntry notificationEntry) {
        if (isFullyCollapsed() && notificationEntry.isRowHeadsUp()) {
            this.mNotificationStackScroller.generateHeadsUpAnimation(notificationEntry.getHeadsUpAnimationView(), false);
            notificationEntry.setHeadsUpIsVisible();
        }
    }

    public void onHeadsUpStateChanged(NotificationEntry notificationEntry, boolean z) {
        this.mNotificationStackScroller.generateHeadsUpAnimation(notificationEntry, z);
    }

    public void setHeadsUpManager(HeadsUpManagerPhone headsUpManagerPhone) {
        super.setHeadsUpManager(headsUpManagerPhone);
        this.mHeadsUpTouchHelper = new HeadsUpTouchHelper(headsUpManagerPhone, this.mNotificationStackScroller.getHeadsUpCallback(), this);
    }

    public void setTrackedHeadsUp(ExpandableNotificationRow expandableNotificationRow) {
        if (expandableNotificationRow != null) {
            notifyListenersTrackingHeadsUp(expandableNotificationRow);
            this.mExpandingFromHeadsUp = true;
        }
    }

    /* access modifiers changed from: protected */
    public void onClosingFinished() {
        super.onClosingFinished();
        resetHorizontalPanelPosition();
        setClosingWithAlphaFadeout(false);
    }

    private void setClosingWithAlphaFadeout(boolean z) {
        this.mClosingWithAlphaFadeOut = z;
        this.mNotificationStackScroller.forceNoOverlappingRendering(z);
    }

    /* access modifiers changed from: protected */
    public void updateVerticalPanelPosition(float f) {
        resetHorizontalPanelPosition();
    }

    private void resetHorizontalPanelPosition() {
        setHorizontalPanelTranslation(0.0f);
    }

    /* access modifiers changed from: protected */
    public void setHorizontalPanelTranslation(float f) {
        this.mNotificationStackScroller.setHorizontalPanelTranslation(f);
        this.mQsFrame.setTranslationX(f);
        int size = this.mVerticalTranslationListener.size();
        for (int i = 0; i < size; i++) {
            ((Runnable) this.mVerticalTranslationListener.get(i)).run();
        }
    }

    /* access modifiers changed from: protected */
    public void updateExpandedHeight(float f) {
        if (this.mTracking) {
            this.mNotificationStackScroller.setExpandingVelocity(getCurrentExpandVelocity());
        }
        this.mNotificationStackScroller.setExpandedHeight(f);
        updateKeyguardBottomAreaAlpha();
        updateBigClockAlpha();
        updateStatusBarIcons();
    }

    public boolean isFullWidth() {
        return this.mIsFullWidth;
    }

    private void updateStatusBarIcons() {
        boolean z = (isPanelVisibleBecauseOfHeadsUp() || isFullWidth()) && getExpandedHeight() < getOpeningHeight();
        if (z && this.mNoVisibleNotifications && isOnKeyguard()) {
            z = false;
        }
        if (z != this.mShowIconsWhenExpanded) {
            this.mShowIconsWhenExpanded = z;
            this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, false);
        }
    }

    private boolean isOnKeyguard() {
        return this.mBarState == 1;
    }

    public void setPanelScrimMinFraction(float f) {
        this.mBar.panelScrimMinFractionChanged(f);
    }

    public void clearNotificationEffects() {
        this.mStatusBar.clearNotificationEffects();
    }

    /* access modifiers changed from: protected */
    public boolean isPanelVisibleBecauseOfHeadsUp() {
        return this.mHeadsUpManager.hasPinnedHeadsUp() || this.mHeadsUpAnimatingAway;
    }

    public boolean hasOverlappingRendering() {
        return !this.mDozing;
    }

    public void launchCamera(boolean z, int i) {
        boolean z2 = true;
        if (i == 1) {
            this.mLastCameraLaunchSource = "power_double_tap";
        } else if (i == 0) {
            this.mLastCameraLaunchSource = "wiggle_gesture";
        } else if (i == 2) {
            this.mLastCameraLaunchSource = "lift_to_launch_ml";
        } else {
            this.mLastCameraLaunchSource = "lockscreen_affordance";
        }
        if (!isFullyCollapsed()) {
            this.mLaunchingAffordance = true;
            setLaunchingAffordance(true);
        } else {
            z = false;
        }
        if (this.mStatusBar.getFacelockController().isFacelockRunning()) {
            z = false;
        }
        updateLaunchingCameraState(z, i);
        this.mAffordanceHasPreview = this.mKeyguardBottomArea.getRightPreview() != null;
        KeyguardAffordanceHelper keyguardAffordanceHelper = this.mAffordanceHelper;
        if (getLayoutDirection() != 1) {
            z2 = false;
        }
        keyguardAffordanceHelper.launchAffordance(z, z2);
    }

    public void onAffordanceLaunchEnded() {
        this.mLaunchingAffordance = false;
        setLaunchingAffordance(false);
        KeyguardUpdateMonitor.getInstance(this.mContext).updateLaunchingLeftAffordance(false);
    }

    private void setLaunchingAffordance(boolean z) {
        getLeftIcon().setLaunchingAffordance(z);
        getRightIcon().setLaunchingAffordance(z);
        Consumer<Boolean> consumer = this.mAffordanceLaunchListener;
        if (consumer != null) {
            consumer.accept(Boolean.valueOf(z));
        }
    }

    public boolean isLaunchingAffordanceWithPreview() {
        return this.mLaunchingAffordance && this.mAffordanceHasPreview;
    }

    public boolean canCameraGestureBeLaunched(boolean z) {
        String str;
        boolean z2 = false;
        if (!this.mStatusBar.isCameraAllowedByAdmin()) {
            return false;
        }
        ResolveInfo resolveCameraIntent = this.mKeyguardBottomArea.resolveCameraIntent();
        if (resolveCameraIntent != null) {
            ActivityInfo activityInfo = resolveCameraIntent.activityInfo;
            if (activityInfo != null) {
                str = activityInfo.packageName;
                boolean isDreaming = KeyguardUpdateMonitor.getInstance(this.mContext).isDreaming();
                String str2 = OpNotificationPanelView.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("canCameraGestureBeLaunched: packageToLaunch = ");
                sb.append(str);
                sb.append(", keyguardIsShowing = ");
                sb.append(z);
                sb.append(", isForegroundApp = ");
                sb.append(isForegroundApp(str));
                sb.append(", isSwipingInProgress = ");
                sb.append(this.mAffordanceHelper.isSwipingInProgress());
                sb.append(", isDream:");
                sb.append(isDreaming);
                Log.d(str2, sb.toString());
                if (str != null && ((z || !isForegroundApp(str) || isDreaming) && !this.mAffordanceHelper.isSwipingInProgress())) {
                    z2 = true;
                }
                return z2;
            }
        }
        str = null;
        boolean isDreaming2 = KeyguardUpdateMonitor.getInstance(this.mContext).isDreaming();
        String str22 = OpNotificationPanelView.TAG;
        StringBuilder sb2 = new StringBuilder();
        sb2.append("canCameraGestureBeLaunched: packageToLaunch = ");
        sb2.append(str);
        sb2.append(", keyguardIsShowing = ");
        sb2.append(z);
        sb2.append(", isForegroundApp = ");
        sb2.append(isForegroundApp(str));
        sb2.append(", isSwipingInProgress = ");
        sb2.append(this.mAffordanceHelper.isSwipingInProgress());
        sb2.append(", isDream:");
        sb2.append(isDreaming2);
        Log.d(str22, sb2.toString());
        z2 = true;
        return z2;
    }

    private boolean isForegroundApp(String str) {
        boolean z = false;
        if (str == null) {
            return false;
        }
        List runningTasks = ((ActivityManager) getContext().getSystemService(ActivityManager.class)).getRunningTasks(1);
        if (!runningTasks.isEmpty() && str.equals(((RunningTaskInfo) runningTasks.get(0)).topActivity.getPackageName())) {
            z = true;
        }
        return z;
    }

    private void setGroupManager(NotificationGroupManager notificationGroupManager) {
        this.mGroupManager = notificationGroupManager;
    }

    public boolean hideStatusBarIconsWhenExpanded() {
        if (this.mLaunchingNotification) {
            return this.mHideIconsDuringNotificationLaunch;
        }
        HeadsUpAppearanceController headsUpAppearanceController = this.mHeadsUpAppearanceController;
        boolean z = false;
        if (headsUpAppearanceController != null && headsUpAppearanceController.shouldBeVisible()) {
            return false;
        }
        if (!isFullWidth() || !this.mShowIconsWhenExpanded) {
            z = true;
        }
        return z;
    }

    public void setTouchAndAnimationDisabled(boolean z) {
        super.setTouchAndAnimationDisabled(z);
        if (z && this.mAffordanceHelper.isSwipingInProgress() && !this.mIsLaunchTransitionRunning) {
            this.mAffordanceHelper.reset(false);
        }
        this.mNotificationStackScroller.setAnimationsEnabled(!z);
    }

    public void setDozing(boolean z, boolean z2, PointF pointF) {
        if (z != this.mDozing) {
            this.mDozing = z;
            this.mNotificationStackScroller.setDark(this.mDozing, z2, pointF);
            this.mKeyguardBottomArea.setDozing(this.mDozing, z2);
            if (z) {
                this.mBottomAreaShadeAlphaAnimator.cancel();
            }
            int i = this.mBarState;
            if (i == 1 || i == 2) {
                updateDozingVisibilities(z2);
            }
            this.mStatusBarStateController.setDozeAmount(z ? 1.0f : 0.0f, z2);
        }
    }

    public void onDozeAmountChanged(float f, float f2) {
        this.mInterpolatedDarkAmount = f2;
        this.mLinearDarkAmount = f;
        this.mKeyguardStatusView.setDarkAmount(this.mInterpolatedDarkAmount);
        this.mKeyguardBottomArea.setDarkAmount(this.mInterpolatedDarkAmount);
        positionClockAndNotifications();
    }

    public void setPulsing(boolean z) {
        this.mPulsing = z;
        DozeParameters instance = DozeParameters.getInstance(this.mContext);
        boolean z2 = !instance.getDisplayNeedsBlanking() && instance.getAlwaysOn();
        if (z2) {
            this.mAnimateNextPositionUpdate = true;
        }
        if (!this.mPulsing && !this.mDozing) {
            this.mAnimateNextPositionUpdate = false;
        }
        this.mNotificationStackScroller.setPulsing(z, z2);
        this.mKeyguardStatusView.setPulsing(z);
    }

    public void dozeTimeTick() {
        this.mKeyguardBottomArea.dozeTimeTick();
        this.mKeyguardStatusView.dozeTimeTick();
        if (this.mInterpolatedDarkAmount > 0.0f) {
            positionClockAndNotifications();
        }
    }

    public void setStatusAccessibilityImportance(int i) {
        this.mKeyguardStatusView.setImportantForAccessibility(i);
    }

    public KeyguardBottomAreaView getKeyguardBottomAreaView() {
        return this.mKeyguardBottomArea;
    }

    public void setUserSetupComplete(boolean z) {
        this.mUserSetupComplete = z;
        this.mKeyguardBottomArea.setUserSetupComplete(z);
    }

    public void setShowOTAWizard(boolean z) {
        this.mNeedShowOTAWizard = z;
        String str = OpNotificationPanelView.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("setShowOTAWizard, ");
        sb.append(z);
        Log.d(str, sb.toString());
        this.mKeyguardBottomArea.setShowOTAWizard(this.mNeedShowOTAWizard);
    }

    public void applyExpandAnimationParams(ExpandAnimationParameters expandAnimationParameters) {
        this.mExpandOffset = expandAnimationParameters != null ? (float) expandAnimationParameters.getTopChange() : 0.0f;
        updateQsExpansion();
        if (expandAnimationParameters != null) {
            boolean z = expandAnimationParameters.getProgress(14, 100) == 0.0f;
            if (z != this.mHideIconsDuringNotificationLaunch) {
                this.mHideIconsDuringNotificationLaunch = z;
                if (!z) {
                    this.mCommandQueue.recomputeDisableFlags(this.mDisplayId, true);
                }
            }
        }
    }

    public void addTrackingHeadsUpListener(Consumer<ExpandableNotificationRow> consumer) {
        this.mTrackingHeadsUpListeners.add(consumer);
    }

    public void removeTrackingHeadsUpListener(Consumer<ExpandableNotificationRow> consumer) {
        this.mTrackingHeadsUpListeners.remove(consumer);
    }

    public void addVerticalTranslationListener(Runnable runnable) {
        this.mVerticalTranslationListener.add(runnable);
    }

    public void removeVerticalTranslationListener(Runnable runnable) {
        this.mVerticalTranslationListener.remove(runnable);
    }

    public void setHeadsUpAppearanceController(HeadsUpAppearanceController headsUpAppearanceController) {
        this.mHeadsUpAppearanceController = headsUpAppearanceController;
    }

    public void onBouncerPreHideAnimation() {
        setKeyguardStatusViewVisibility(this.mBarState, true, false);
    }

    public void blockExpansionForCurrentTouch() {
        this.mBlockingExpansionForCurrentTouch = this.mTracking;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(fileDescriptor, printWriter, strArr);
        StringBuilder sb = new StringBuilder();
        sb.append("    gestureExclusionRect: ");
        sb.append(calculateGestureExclusionRect());
        printWriter.println(sb.toString());
        KeyguardStatusBarView keyguardStatusBarView = this.mKeyguardStatusBar;
        if (keyguardStatusBarView != null) {
            keyguardStatusBarView.dump(fileDescriptor, printWriter, strArr);
        }
        KeyguardStatusView keyguardStatusView = this.mKeyguardStatusView;
        if (keyguardStatusView != null) {
            keyguardStatusView.dump(fileDescriptor, printWriter, strArr);
        }
    }

    public void onZenChanged(int i) {
        updateShowEmptyShadeView();
    }

    private void updateShowEmptyShadeView() {
        boolean z = true;
        if (this.mBarState == 1 || this.mEntryManager.getNotificationData().getActiveNotifications().size() != 0) {
            z = false;
        }
        showEmptyShadeView(z);
    }

    public Delegate createRemoteInputDelegate() {
        return this.mNotificationStackScroller.createDelegate();
    }

    public void updateNotificationViews() {
        boolean z = this.mLockscreenUserManager.isAnyProfilePublicMode() || this.mStatusBar.getViewHierarchyManager().isAnyNotificationLocked();
        HeadsUpAppearanceController headsUpAppearanceController = this.mHeadsUpAppearanceController;
        if (headsUpAppearanceController != null) {
            headsUpAppearanceController.setPublicMode(z);
        }
        this.mNotificationStackScroller.setHideSensitive(z, false);
        this.mNotificationStackScroller.updateSectionBoundaries();
        this.mNotificationStackScroller.updateSpeedBumpIndex();
        this.mNotificationStackScroller.updateFooter();
        updateShowEmptyShadeView();
        this.mNotificationStackScroller.updateIconAreaViews();
    }

    public void onUpdateRowStates() {
        this.mNotificationStackScroller.onUpdateRowStates();
    }

    public boolean hasPulsingNotifications() {
        return this.mNotificationStackScroller.hasPulsingNotifications();
    }

    public ActivatableNotificationView getActivatedChild() {
        return this.mNotificationStackScroller.getActivatedChild();
    }

    public void setActivatedChild(ActivatableNotificationView activatableNotificationView) {
        this.mNotificationStackScroller.setActivatedChild(activatableNotificationView);
    }

    public void runAfterAnimationFinished(Runnable runnable) {
        this.mNotificationStackScroller.runAfterAnimationFinished(runnable);
    }

    public void initDependencies(StatusBar statusBar, NotificationGroupManager notificationGroupManager, NotificationShelf notificationShelf, HeadsUpManagerPhone headsUpManagerPhone, NotificationIconAreaController notificationIconAreaController, ScrimController scrimController) {
        setStatusBar(statusBar);
        setGroupManager(this.mGroupManager);
        this.mNotificationStackScroller.setNotificationPanel(this);
        this.mNotificationStackScroller.setIconAreaController(notificationIconAreaController);
        this.mNotificationStackScroller.setStatusBar(statusBar);
        this.mNotificationStackScroller.setGroupManager(notificationGroupManager);
        this.mNotificationStackScroller.setHeadsUpManager(headsUpManagerPhone);
        this.mNotificationStackScroller.setShelf(notificationShelf);
        this.mNotificationStackScroller.setScrimController(scrimController);
        updateShowEmptyShadeView();
    }

    public void showTransientIndication(int i) {
        this.mKeyguardIndicationController.showTransientIndication(i);
    }

    public void onDynamicPrivacyChanged() {
        this.mAnimateNextPositionUpdate = true;
    }
}

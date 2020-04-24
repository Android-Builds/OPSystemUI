package com.android.systemui.statusbar.phone;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.DisplayCutout;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.WindowInsets;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.widget.ViewClippingUtil;
import com.android.internal.widget.ViewClippingUtil.ClippingParameters;
import com.android.systemui.Dependency;
import com.android.systemui.R$id;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.HeadsUpStatusBarView;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.oneplus.scene.OpSceneModeObserver;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HeadsUpAppearanceController implements OnHeadsUpChangedListener, DarkReceiver {
    private boolean mAnimationsEnabled;
    private final View mCenteredIconView;
    private final View mClockView;
    private final DarkIconDispatcher mDarkIconDispatcher;
    @VisibleForTesting
    float mExpandFraction;
    @VisibleForTesting
    float mExpandedHeight;
    private final HeadsUpManagerPhone mHeadsUpManager;
    /* access modifiers changed from: private */
    public final HeadsUpStatusBarView mHeadsUpStatusBarView;
    @VisibleForTesting
    boolean mIsExpanded;
    private final NotificationIconAreaController mNotificationIconAreaController;
    private final OpSceneModeObserver mOpSceneModeObserver;
    private final View mOperatorNameView;
    private final NotificationPanelView mPanelView;
    private final ClippingParameters mParentClippingParams;
    Point mPoint;
    private final BiConsumer<Float, Float> mSetExpandedHeight;
    private final Consumer<ExpandableNotificationRow> mSetTrackingHeadsUp;
    private boolean mShown;
    private final OnLayoutChangeListener mStackScrollLayoutChangeListener;
    /* access modifiers changed from: private */
    public final NotificationStackScrollLayout mStackScroller;
    private ExpandableNotificationRow mTrackedChild;
    private final Runnable mUpdatePanelTranslation;

    public /* synthetic */ void lambda$new$0$HeadsUpAppearanceController(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        updatePanelTranslation();
    }

    public HeadsUpAppearanceController(NotificationIconAreaController notificationIconAreaController, HeadsUpManagerPhone headsUpManagerPhone, View view) {
        this(notificationIconAreaController, headsUpManagerPhone, (HeadsUpStatusBarView) view.findViewById(R$id.heads_up_status_bar_view), (NotificationStackScrollLayout) view.findViewById(R$id.notification_stack_scroller), (NotificationPanelView) view.findViewById(R$id.notification_panel), view.findViewById(R$id.clock), view.findViewById(R$id.operator_name_frame), view.findViewById(R$id.centered_icon_area));
    }

    @VisibleForTesting
    public HeadsUpAppearanceController(NotificationIconAreaController notificationIconAreaController, HeadsUpManagerPhone headsUpManagerPhone, HeadsUpStatusBarView headsUpStatusBarView, NotificationStackScrollLayout notificationStackScrollLayout, NotificationPanelView notificationPanelView, View view, View view2, View view3) {
        this.mSetTrackingHeadsUp = new Consumer() {
            public final void accept(Object obj) {
                HeadsUpAppearanceController.this.setTrackingHeadsUp((ExpandableNotificationRow) obj);
            }
        };
        this.mUpdatePanelTranslation = new Runnable() {
            public final void run() {
                HeadsUpAppearanceController.this.updatePanelTranslation();
            }
        };
        this.mSetExpandedHeight = new BiConsumer() {
            public final void accept(Object obj, Object obj2) {
                HeadsUpAppearanceController.this.setExpandedHeight(((Float) obj).floatValue(), ((Float) obj2).floatValue());
            }
        };
        this.mStackScrollLayoutChangeListener = new OnLayoutChangeListener() {
            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                HeadsUpAppearanceController.this.lambda$new$0$HeadsUpAppearanceController(view, i, i2, i3, i4, i5, i6, i7, i8);
            }
        };
        this.mParentClippingParams = new ClippingParameters() {
            public boolean shouldFinish(View view) {
                return view.getId() == R$id.status_bar;
            }
        };
        this.mAnimationsEnabled = true;
        this.mOpSceneModeObserver = (OpSceneModeObserver) Dependency.get(OpSceneModeObserver.class);
        this.mNotificationIconAreaController = notificationIconAreaController;
        this.mHeadsUpManager = headsUpManagerPhone;
        this.mHeadsUpManager.addListener(this);
        this.mHeadsUpStatusBarView = headsUpStatusBarView;
        this.mCenteredIconView = view3;
        headsUpStatusBarView.setOnDrawingRectChangedListener(new Runnable() {
            public final void run() {
                HeadsUpAppearanceController.this.lambda$new$1$HeadsUpAppearanceController();
            }
        });
        this.mStackScroller = notificationStackScrollLayout;
        this.mPanelView = notificationPanelView;
        notificationPanelView.addTrackingHeadsUpListener(this.mSetTrackingHeadsUp);
        notificationPanelView.addVerticalTranslationListener(this.mUpdatePanelTranslation);
        notificationPanelView.setHeadsUpAppearanceController(this);
        this.mStackScroller.addOnExpandedHeightListener(this.mSetExpandedHeight);
        this.mStackScroller.addOnLayoutChangeListener(this.mStackScrollLayoutChangeListener);
        this.mStackScroller.setHeadsUpAppearanceController(this);
        this.mClockView = view;
        this.mOperatorNameView = view2;
        this.mDarkIconDispatcher = (DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class);
        this.mDarkIconDispatcher.addDarkReceiver((DarkReceiver) this);
        this.mHeadsUpStatusBarView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                if (HeadsUpAppearanceController.this.shouldBeVisible()) {
                    HeadsUpAppearanceController.this.updateTopEntry();
                    HeadsUpAppearanceController.this.mStackScroller.requestLayout();
                }
                HeadsUpAppearanceController.this.mHeadsUpStatusBarView.removeOnLayoutChangeListener(this);
            }
        });
    }

    public /* synthetic */ void lambda$new$1$HeadsUpAppearanceController() {
        updateIsolatedIconLocation(true);
    }

    public void destroy() {
        this.mHeadsUpManager.removeListener(this);
        this.mHeadsUpStatusBarView.setOnDrawingRectChangedListener(null);
        this.mPanelView.removeTrackingHeadsUpListener(this.mSetTrackingHeadsUp);
        this.mPanelView.removeVerticalTranslationListener(this.mUpdatePanelTranslation);
        this.mPanelView.setHeadsUpAppearanceController(null);
        this.mStackScroller.removeOnExpandedHeightListener(this.mSetExpandedHeight);
        this.mStackScroller.removeOnLayoutChangeListener(this.mStackScrollLayoutChangeListener);
        this.mDarkIconDispatcher.removeDarkReceiver((DarkReceiver) this);
    }

    private void updateIsolatedIconLocation(boolean z) {
        this.mNotificationIconAreaController.setIsolatedIconLocation(this.mHeadsUpStatusBarView.getIconDrawingRect(), z);
    }

    public void onHeadsUpPinned(NotificationEntry notificationEntry) {
        updateTopEntry();
        lambda$updateHeadsUpHeaders$4$HeadsUpAppearanceController(notificationEntry);
    }

    private int getRtlTranslation() {
        int i;
        if (this.mPoint == null) {
            this.mPoint = new Point();
        }
        int i2 = 0;
        if (this.mStackScroller.getDisplay() != null) {
            this.mStackScroller.getDisplay().getRealSize(this.mPoint);
            i = this.mPoint.x;
        } else {
            i = 0;
        }
        WindowInsets rootWindowInsets = this.mStackScroller.getRootWindowInsets();
        DisplayCutout displayCutout = rootWindowInsets != null ? rootWindowInsets.getDisplayCutout() : null;
        int stableInsetLeft = rootWindowInsets != null ? rootWindowInsets.getStableInsetLeft() : 0;
        int stableInsetRight = rootWindowInsets != null ? rootWindowInsets.getStableInsetRight() : 0;
        int safeInsetLeft = displayCutout != null ? displayCutout.getSafeInsetLeft() : 0;
        if (displayCutout != null) {
            i2 = displayCutout.getSafeInsetRight();
        }
        return ((Math.max(stableInsetLeft, safeInsetLeft) + this.mStackScroller.getRight()) + Math.max(stableInsetRight, i2)) - i;
    }

    public void updatePanelTranslation() {
        int i;
        if (this.mStackScroller.isLayoutRtl()) {
            i = getRtlTranslation();
        } else {
            i = this.mStackScroller.getLeft();
        }
        this.mHeadsUpStatusBarView.setPanelTranslation(((float) i) + this.mStackScroller.getTranslationX());
    }

    /* JADX WARNING: Removed duplicated region for block: B:19:0x0046  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateTopEntry() {
        /*
            r5 = this;
            boolean r0 = r5.mIsExpanded
            r1 = 0
            if (r0 != 0) goto L_0x001c
            com.android.systemui.statusbar.phone.HeadsUpManagerPhone r0 = r5.mHeadsUpManager
            boolean r0 = r0.hasPinnedHeadsUp()
            if (r0 == 0) goto L_0x001c
            com.oneplus.scene.OpSceneModeObserver r0 = r5.mOpSceneModeObserver
            boolean r0 = r0.isInBrickMode()
            if (r0 != 0) goto L_0x001c
            com.android.systemui.statusbar.phone.HeadsUpManagerPhone r0 = r5.mHeadsUpManager
            com.android.systemui.statusbar.notification.collection.NotificationEntry r0 = r0.getTopEntry()
            goto L_0x001d
        L_0x001c:
            r0 = r1
        L_0x001d:
            com.android.systemui.statusbar.HeadsUpStatusBarView r2 = r5.mHeadsUpStatusBarView
            com.android.systemui.statusbar.notification.collection.NotificationEntry r2 = r2.getShowingEntry()
            com.android.systemui.statusbar.HeadsUpStatusBarView r3 = r5.mHeadsUpStatusBarView
            r3.setEntry(r0)
            if (r0 == r2) goto L_0x004b
            r3 = 1
            r4 = 0
            if (r0 != 0) goto L_0x0035
            r5.setShown(r4)
            boolean r2 = r5.mIsExpanded
        L_0x0033:
            r2 = r2 ^ r3
            goto L_0x003e
        L_0x0035:
            if (r2 != 0) goto L_0x003d
            r5.setShown(r3)
            boolean r2 = r5.mIsExpanded
            goto L_0x0033
        L_0x003d:
            r2 = r4
        L_0x003e:
            r5.updateIsolatedIconLocation(r4)
            com.android.systemui.statusbar.phone.NotificationIconAreaController r5 = r5.mNotificationIconAreaController
            if (r0 != 0) goto L_0x0046
            goto L_0x0048
        L_0x0046:
            com.android.systemui.statusbar.StatusBarIconView r1 = r0.icon
        L_0x0048:
            r5.showIconIsolated(r1, r2)
        L_0x004b:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.HeadsUpAppearanceController.updateTopEntry():void");
    }

    private void setShown(boolean z) {
        if (this.mShown != z) {
            this.mShown = z;
            if (z) {
                updateParentClipping(false);
                this.mHeadsUpStatusBarView.setVisibility(0);
                show(this.mHeadsUpStatusBarView);
                hide(this.mClockView, 4);
                if (this.mCenteredIconView.getVisibility() != 8) {
                    hide(this.mCenteredIconView, 4);
                }
                View view = this.mOperatorNameView;
                if (view != null) {
                    hide(view, 4);
                    return;
                }
                return;
            }
            if (!this.mOpSceneModeObserver.isInBrickMode()) {
                show(this.mClockView);
            }
            if (this.mCenteredIconView.getVisibility() != 8) {
                show(this.mCenteredIconView);
            }
            View view2 = this.mOperatorNameView;
            if (view2 != null) {
                show(view2);
            }
            hide(this.mHeadsUpStatusBarView, 8, new Runnable() {
                public final void run() {
                    HeadsUpAppearanceController.this.lambda$setShown$2$HeadsUpAppearanceController();
                }
            });
        }
    }

    public /* synthetic */ void lambda$setShown$2$HeadsUpAppearanceController() {
        updateParentClipping(true);
    }

    private void updateParentClipping(boolean z) {
        ViewClippingUtil.setClippingDeactivated(this.mHeadsUpStatusBarView, !z, this.mParentClippingParams);
    }

    private void hide(View view, int i) {
        hide(view, i, null);
    }

    private void hide(View view, int i, Runnable runnable) {
        if (this.mAnimationsEnabled) {
            CrossFadeHelper.fadeOut(view, 110, 0, new Runnable(view, i, runnable) {
                private final /* synthetic */ View f$0;
                private final /* synthetic */ int f$1;
                private final /* synthetic */ Runnable f$2;

                {
                    this.f$0 = r1;
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    HeadsUpAppearanceController.lambda$hide$3(this.f$0, this.f$1, this.f$2);
                }
            });
            return;
        }
        view.setVisibility(i);
        if (runnable != null) {
            runnable.run();
        }
    }

    static /* synthetic */ void lambda$hide$3(View view, int i, Runnable runnable) {
        view.setVisibility(i);
        if (runnable != null) {
            runnable.run();
        }
    }

    private void show(View view) {
        if (this.mAnimationsEnabled) {
            CrossFadeHelper.fadeIn(view, 110, 100);
        } else {
            view.setVisibility(0);
        }
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void setAnimationsEnabled(boolean z) {
        this.mAnimationsEnabled = z;
    }

    @VisibleForTesting
    public boolean isShown() {
        return this.mShown;
    }

    public boolean shouldBeVisible() {
        return !this.mIsExpanded && this.mHeadsUpManager.hasPinnedHeadsUp();
    }

    public void onHeadsUpUnPinned(NotificationEntry notificationEntry) {
        updateTopEntry();
        lambda$updateHeadsUpHeaders$4$HeadsUpAppearanceController(notificationEntry);
    }

    public void setExpandedHeight(float f, float f2) {
        boolean z = true;
        boolean z2 = f != this.mExpandedHeight;
        this.mExpandedHeight = f;
        this.mExpandFraction = f2;
        if (f <= 0.0f) {
            z = false;
        }
        if (z2) {
            updateHeadsUpHeaders();
        }
        if (z != this.mIsExpanded) {
            this.mIsExpanded = z;
            updateTopEntry();
        }
    }

    public void setTrackingHeadsUp(ExpandableNotificationRow expandableNotificationRow) {
        ExpandableNotificationRow expandableNotificationRow2 = this.mTrackedChild;
        this.mTrackedChild = expandableNotificationRow;
        if (expandableNotificationRow2 != null) {
            lambda$updateHeadsUpHeaders$4$HeadsUpAppearanceController(expandableNotificationRow2.getEntry());
        }
    }

    private void updateHeadsUpHeaders() {
        this.mHeadsUpManager.getAllEntries().forEach(new Consumer() {
            public final void accept(Object obj) {
                HeadsUpAppearanceController.this.lambda$updateHeadsUpHeaders$4$HeadsUpAppearanceController((NotificationEntry) obj);
            }
        });
    }

    /* renamed from: updateHeader */
    public void lambda$updateHeadsUpHeaders$4$HeadsUpAppearanceController(NotificationEntry notificationEntry) {
        float f;
        ExpandableNotificationRow row = notificationEntry.getRow();
        if (row.isPinned() || row.isHeadsUpAnimatingAway() || row == this.mTrackedChild) {
            f = this.mExpandFraction;
        } else {
            f = 1.0f;
        }
        row.setHeaderVisibleAmount(f);
    }

    public void onDarkChanged(Rect rect, float f, int i) {
        this.mHeadsUpStatusBarView.onDarkChanged(rect, f, i);
    }

    public void setPublicMode(boolean z) {
        this.mHeadsUpStatusBarView.setPublicMode(z);
        updateTopEntry();
    }

    /* access modifiers changed from: 0000 */
    public void readFrom(HeadsUpAppearanceController headsUpAppearanceController) {
        if (headsUpAppearanceController != null) {
            this.mTrackedChild = headsUpAppearanceController.mTrackedChild;
            this.mExpandedHeight = headsUpAppearanceController.mExpandedHeight;
            this.mIsExpanded = headsUpAppearanceController.mIsExpanded;
            this.mExpandFraction = headsUpAppearanceController.mExpandFraction;
        }
    }
}

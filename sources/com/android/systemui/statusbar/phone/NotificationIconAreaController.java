package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import androidx.collection.ArrayMap;
import com.android.internal.util.ContrastColorUtil;
import com.android.systemui.Dependency;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.oneplus.systemui.statusbar.phone.OpNotificationIconAreaController;
import java.util.ArrayList;
import java.util.function.Function;

public class NotificationIconAreaController extends OpNotificationIconAreaController implements DarkReceiver, StateListener {
    private boolean mAnimationsEnabled;
    private NotificationIconContainer mCenteredIcon;
    protected View mCenteredIconArea;
    private int mCenteredIconTint = -1;
    private Context mContext;
    private final ContrastColorUtil mContrastColorUtil;
    private float mDarkAmount;
    private final NotificationEntryManager mEntryManager;
    private boolean mFullyDark;
    private int mIconHPadding;
    private int mIconSize;
    private int mIconTint = -1;
    private final NotificationMediaManager mMediaManager;
    protected View mNotificationIconArea;
    private NotificationIconContainer mNotificationIcons;
    private ViewGroup mNotificationScrollLayout;
    private NotificationIconContainer mShelfIcons;
    private StatusBar mStatusBar;
    private final StatusBarStateController mStatusBarStateController;
    private final Rect mTintArea = new Rect();
    private final Runnable mUpdateStatusBarIcons = new Runnable() {
        public final void run() {
            NotificationIconAreaController.this.updateStatusBarIcons();
        }
    };

    public NotificationIconAreaController(Context context, StatusBar statusBar, StatusBarStateController statusBarStateController, NotificationMediaManager notificationMediaManager) {
        this.mStatusBar = statusBar;
        this.mContrastColorUtil = ContrastColorUtil.getInstance(context);
        this.mContext = context;
        this.mEntryManager = (NotificationEntryManager) Dependency.get(NotificationEntryManager.class);
        this.mStatusBarStateController = statusBarStateController;
        this.mStatusBarStateController.addCallback(this);
        this.mMediaManager = notificationMediaManager;
        initializeNotificationAreaViews(context);
    }

    /* access modifiers changed from: protected */
    public View inflateIconArea(LayoutInflater layoutInflater) {
        return layoutInflater.inflate(R$layout.notification_icon_area, null);
    }

    /* access modifiers changed from: protected */
    public void initializeNotificationAreaViews(Context context) {
        reloadDimens(context);
        LayoutInflater from = LayoutInflater.from(context);
        this.mNotificationIconArea = inflateIconArea(from);
        this.mNotificationIcons = (NotificationIconContainer) this.mNotificationIconArea.findViewById(R$id.notificationIcons);
        this.mNotificationScrollLayout = this.mStatusBar.getNotificationScrollLayout();
        this.mCenteredIconArea = from.inflate(R$layout.center_icon_area, null);
        this.mCenteredIcon = (NotificationIconContainer) this.mCenteredIconArea.findViewById(R$id.centeredIcon);
    }

    public void setupShelf(NotificationShelf notificationShelf) {
        this.mShelfIcons = notificationShelf.getShelfIcons();
        notificationShelf.setCollapsedIcons(this.mNotificationIcons);
    }

    public void onDensityOrFontScaleChanged(Context context) {
        reloadDimens(context);
        LayoutParams generateIconLayoutParams = generateIconLayoutParams();
        for (int i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            this.mNotificationIcons.getChildAt(i).setLayoutParams(generateIconLayoutParams);
        }
        for (int i2 = 0; i2 < this.mShelfIcons.getChildCount(); i2++) {
            this.mShelfIcons.getChildAt(i2).setLayoutParams(generateIconLayoutParams);
        }
        for (int i3 = 0; i3 < this.mCenteredIcon.getChildCount(); i3++) {
            this.mCenteredIcon.getChildAt(i3).setLayoutParams(generateIconLayoutParams);
        }
    }

    private LayoutParams generateIconLayoutParams() {
        return new LayoutParams(this.mIconSize + (this.mIconHPadding * 2), getHeight());
    }

    private void reloadDimens(Context context) {
        Resources resources = context.getResources();
        this.mIconSize = resources.getDimensionPixelSize(17105425);
        this.mIconHPadding = resources.getDimensionPixelSize(R$dimen.status_bar_icon_padding);
    }

    public View getNotificationInnerAreaView() {
        return this.mNotificationIconArea;
    }

    public View getCenteredNotificationAreaView() {
        return this.mCenteredIconArea;
    }

    public void onDarkChanged(Rect rect, float f, int i) {
        if (rect == null) {
            this.mTintArea.setEmpty();
        } else {
            this.mTintArea.set(rect);
        }
        View view = this.mNotificationIconArea;
        if (view == null) {
            this.mIconTint = i;
        } else if (DarkIconDispatcher.isInArea(rect, view)) {
            this.mIconTint = i;
        }
        View view2 = this.mCenteredIconArea;
        if (view2 == null) {
            this.mCenteredIconTint = i;
        } else if (DarkIconDispatcher.isInArea(rect, view2)) {
            this.mCenteredIconTint = i;
        }
        applyNotificationIconsTint();
    }

    /* access modifiers changed from: protected */
    public int getHeight() {
        return this.mStatusBar.getStatusBarHeight();
    }

    public void updateNotificationIcons() {
        updateStatusBarIcons();
        updateShelfIcons();
        updateCenterIcon();
        applyNotificationIconsTint();
    }

    private void updateShelfIcons() {
        C1351xeccd2fb2 r1 = C1351xeccd2fb2.INSTANCE;
        NotificationIconContainer notificationIconContainer = this.mShelfIcons;
        boolean z = this.mFullyDark;
        updateIconsForLayout(r1, notificationIconContainer, true, true, false, z, z, true);
    }

    public void updateStatusBarIcons() {
        updateIconsForLayout(C1353x339b1a97.INSTANCE, this.mNotificationIcons, false, false, true, true, false, true);
    }

    private void updateCenterIcon() {
        updateIconsForLayout(C1350x603b2dbf.INSTANCE, this.mCenteredIcon, false, true, false, false, this.mFullyDark, false);
    }

    public void setAnimationsEnabled(boolean z) {
        this.mAnimationsEnabled = z;
        updateAnimations();
    }

    public void onStateChanged(int i) {
        updateAnimations();
    }

    private void updateAnimations() {
        boolean z = true;
        boolean z2 = this.mStatusBarStateController.getState() == 0;
        this.mCenteredIcon.setAnimationsEnabled(this.mAnimationsEnabled && z2);
        NotificationIconContainer notificationIconContainer = this.mNotificationIcons;
        if (!this.mAnimationsEnabled || !z2) {
            z = false;
        }
        notificationIconContainer.setAnimationsEnabled(z);
    }

    private void updateIconsForLayout(Function<NotificationEntry, StatusBarIconView> function, NotificationIconContainer notificationIconContainer, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6) {
        NotificationIconContainer notificationIconContainer2 = notificationIconContainer;
        ArrayList arrayList = new ArrayList(this.mNotificationScrollLayout.getChildCount());
        boolean equals = notificationIconContainer2.equals(this.mNotificationIcons);
        boolean equals2 = notificationIconContainer2.equals(this.mShelfIcons);
        for (int i = 0; i < this.mNotificationScrollLayout.getChildCount(); i++) {
            View childAt = this.mNotificationScrollLayout.getChildAt(i);
            if (childAt instanceof ExpandableNotificationRow) {
                NotificationEntry entry = ((ExpandableNotificationRow) childAt).getEntry();
                NotificationEntry notificationEntry = entry;
                if (super.shouldShowNotificationIcon(entry, z, z2, z3, z4, z5, z6, equals)) {
                    StatusBarIconView statusBarIconView = (StatusBarIconView) function.apply(notificationEntry);
                    if (statusBarIconView != null) {
                        arrayList.add(statusBarIconView);
                    }
                }
            }
            Function<NotificationEntry, StatusBarIconView> function2 = function;
        }
        ArrayMap arrayMap = new ArrayMap();
        ArrayList arrayList2 = new ArrayList();
        for (int i2 = 0; i2 < notificationIconContainer.getChildCount(); i2++) {
            View childAt2 = notificationIconContainer2.getChildAt(i2);
            if ((childAt2 instanceof StatusBarIconView) && !arrayList.contains(childAt2)) {
                StatusBarIconView statusBarIconView2 = (StatusBarIconView) childAt2;
                String groupKey = statusBarIconView2.getNotification().getGroupKey();
                int i3 = 0;
                boolean z7 = false;
                while (true) {
                    if (i3 >= arrayList.size()) {
                        break;
                    }
                    StatusBarIconView statusBarIconView3 = (StatusBarIconView) arrayList.get(i3);
                    if (statusBarIconView3.getSourceIcon().sameAs(statusBarIconView2.getSourceIcon()) && statusBarIconView3.getNotification().getGroupKey().equals(groupKey)) {
                        if (z7) {
                            z7 = false;
                            break;
                        }
                        z7 = true;
                    }
                    i3++;
                }
                if (z7) {
                    ArrayList arrayList3 = (ArrayList) arrayMap.get(groupKey);
                    if (arrayList3 == null) {
                        arrayList3 = new ArrayList();
                        arrayMap.put(groupKey, arrayList3);
                    }
                    arrayList3.add(statusBarIconView2.getStatusBarIcon());
                }
                arrayList2.add(statusBarIconView2);
            }
        }
        ArrayList arrayList4 = new ArrayList();
        for (String str : arrayMap.keySet()) {
            if (((ArrayList) arrayMap.get(str)).size() != 1) {
                arrayList4.add(str);
            }
        }
        arrayMap.removeAll(arrayList4);
        notificationIconContainer2.setReplacingIcons(arrayMap);
        int size = arrayList2.size();
        if (equals2) {
            notificationIconContainer2.setRemoveWithoutAnimation(arrayList.size() - size <= 1);
        }
        for (int i4 = 0; i4 < size; i4++) {
            notificationIconContainer2.removeView((View) arrayList2.get(i4));
        }
        if (equals2) {
            notificationIconContainer2.setRemoveWithoutAnimation(false);
        }
        LayoutParams generateIconLayoutParams = generateIconLayoutParams();
        for (int i5 = 0; i5 < arrayList.size(); i5++) {
            StatusBarIconView statusBarIconView4 = (StatusBarIconView) arrayList.get(i5);
            notificationIconContainer2.removeTransientView(statusBarIconView4);
            if (statusBarIconView4.getParent() == null) {
                if (z3) {
                    statusBarIconView4.setOnDismissListener(this.mUpdateStatusBarIcons);
                }
                notificationIconContainer2.addView(statusBarIconView4, i5, generateIconLayoutParams);
            }
        }
        notificationIconContainer2.setChangingViewPositions(true);
        int childCount = notificationIconContainer.getChildCount();
        for (int i6 = 0; i6 < childCount; i6++) {
            StatusBarIconView statusBarIconView5 = (StatusBarIconView) arrayList.get(i6);
            if (notificationIconContainer2.getChildAt(i6) != statusBarIconView5) {
                notificationIconContainer2.removeView(statusBarIconView5);
                notificationIconContainer2.addView(statusBarIconView5, i6);
            }
        }
        if (equals && (this.mStatusBar.isDozing() || this.mStatusBar.isDozingCustom())) {
            this.mAodNotificationIconCtrl.updateNotificationIcons(notificationIconContainer2);
        }
        notificationIconContainer2.setChangingViewPositions(false);
        notificationIconContainer2.setReplacingIcons(null);
    }

    private void applyNotificationIconsTint() {
        for (int i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            StatusBarIconView statusBarIconView = (StatusBarIconView) this.mNotificationIcons.getChildAt(i);
            if (statusBarIconView.getWidth() != 0) {
                updateTintForIcon(statusBarIconView, this.mIconTint);
            } else {
                statusBarIconView.executeOnLayout(new Runnable(statusBarIconView) {
                    private final /* synthetic */ StatusBarIconView f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        NotificationIconAreaController.this.mo17787xec38b269(this.f$1);
                    }
                });
            }
        }
        for (int i2 = 0; i2 < this.mCenteredIcon.getChildCount(); i2++) {
            StatusBarIconView statusBarIconView2 = (StatusBarIconView) this.mCenteredIcon.getChildAt(i2);
            if (statusBarIconView2.getWidth() != 0) {
                updateTintForIcon(statusBarIconView2, this.mCenteredIconTint);
            } else {
                statusBarIconView2.executeOnLayout(new Runnable(statusBarIconView2) {
                    private final /* synthetic */ StatusBarIconView f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        NotificationIconAreaController.this.mo17788x7468f248(this.f$1);
                    }
                });
            }
        }
    }

    /* renamed from: lambda$applyNotificationIconsTint$3$NotificationIconAreaController */
    public /* synthetic */ void mo17787xec38b269(StatusBarIconView statusBarIconView) {
        updateTintForIcon(statusBarIconView, this.mIconTint);
    }

    /* renamed from: lambda$applyNotificationIconsTint$4$NotificationIconAreaController */
    public /* synthetic */ void mo17788x7468f248(StatusBarIconView statusBarIconView) {
        updateTintForIcon(statusBarIconView, this.mCenteredIconTint);
    }

    private void updateTintForIcon(StatusBarIconView statusBarIconView, int i) {
        updateTintForIconInternal(statusBarIconView, i, this.mContext, this.mContrastColorUtil, this.mTintArea);
    }

    public void showIconIsolated(StatusBarIconView statusBarIconView, boolean z) {
        this.mNotificationIcons.showIconIsolated(statusBarIconView, z);
    }

    public void setIsolatedIconLocation(Rect rect, boolean z) {
        this.mNotificationIcons.setIsolatedIconLocation(rect, z);
    }

    public void onDozeAmountChanged(float f, float f2) {
        this.mDarkAmount = f;
        boolean z = this.mDarkAmount == 1.0f;
        if (this.mFullyDark != z) {
            this.mFullyDark = z;
            updateShelfIcons();
        }
    }
}

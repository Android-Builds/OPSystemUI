package com.android.systemui.statusbar.phone;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.ViewStub.OnInflateListener;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.fragments.FragmentHostManager.FragmentListener;
import com.android.systemui.plugins.p006qs.C0959QS;
import com.android.systemui.statusbar.notification.AboveShelfObserver.HasViewAboveShelfChangedListener;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.oneplus.util.OpUtils;

public class NotificationsQuickSettingsContainer extends FrameLayout implements OnInflateListener, FragmentListener, HasViewAboveShelfChangedListener {
    private int mBottomPadding;
    private boolean mCustomizerAnimating;
    private boolean mHasViewsAboveShelf;
    private boolean mHeadUpStatus;
    private boolean mInflated;
    private View mKeyguardStatusBar;
    private boolean mQsExpanded;
    private FrameLayout mQsFrame;
    private NotificationStackScrollLayout mStackScroller;
    private int mStackScrollerMargin;
    private View mUserSwitcher;

    public NotificationsQuickSettingsContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mQsFrame = (FrameLayout) findViewById(R$id.qs_frame);
        this.mStackScroller = (NotificationStackScrollLayout) findViewById(R$id.notification_stack_scroller);
        this.mStackScrollerMargin = getStackScrollerBottomMargin(getResources().getConfiguration().orientation);
        this.mKeyguardStatusBar = findViewById(R$id.keyguard_header);
        ViewStub viewStub = (ViewStub) findViewById(R$id.keyguard_user_switcher);
        viewStub.setOnInflateListener(this);
        this.mUserSwitcher = viewStub;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        FragmentHostManager.get(this).addTagListener(C0959QS.TAG, this);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        FragmentHostManager.get(this).removeTagListener(C0959QS.TAG, this);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        reloadWidth(this.mQsFrame, R$dimen.qs_panel_width);
        reloadWidth(this.mStackScroller, R$dimen.notification_panel_width);
        this.mStackScrollerMargin = getStackScrollerBottomMargin(configuration.orientation);
        setBottomMargin(this.mStackScroller, this.mStackScrollerMargin);
    }

    private void reloadWidth(View view, int i) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        layoutParams.width = getResources().getDimensionPixelSize(i);
        view.setLayoutParams(layoutParams);
    }

    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        this.mBottomPadding = windowInsets.getStableInsetBottom();
        setPadding(0, 0, 0, this.mBottomPadding);
        return windowInsets;
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0028, code lost:
        if (r5.mHeadUpStatus == false) goto L_0x002b;
     */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x002e  */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0031  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0035  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0038  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x003e  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0053  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean drawChild(android.graphics.Canvas r6, android.view.View r7, long r8) {
        /*
            r5 = this;
            boolean r0 = r5.mInflated
            r1 = 1
            r2 = 0
            if (r0 == 0) goto L_0x0010
            android.view.View r0 = r5.mUserSwitcher
            int r0 = r0.getVisibility()
            if (r0 != 0) goto L_0x0010
            r0 = r1
            goto L_0x0011
        L_0x0010:
            r0 = r2
        L_0x0011:
            android.view.View r3 = r5.mKeyguardStatusBar
            int r3 = r3.getVisibility()
            if (r3 != 0) goto L_0x001b
            r3 = r1
            goto L_0x001c
        L_0x001b:
            r3 = r2
        L_0x001c:
            boolean r4 = r5.mHasViewsAboveShelf
            if (r4 == 0) goto L_0x002b
            boolean r4 = r5.mQsExpanded
            if (r4 == 0) goto L_0x002c
            if (r4 == 0) goto L_0x002b
            boolean r4 = r5.mHeadUpStatus
            if (r4 == 0) goto L_0x002b
            goto L_0x002c
        L_0x002b:
            r1 = r2
        L_0x002c:
            if (r1 == 0) goto L_0x0031
            com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout r2 = r5.mStackScroller
            goto L_0x0033
        L_0x0031:
            android.widget.FrameLayout r2 = r5.mQsFrame
        L_0x0033:
            if (r1 != 0) goto L_0x0038
            com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout r1 = r5.mStackScroller
            goto L_0x003a
        L_0x0038:
            android.widget.FrameLayout r1 = r5.mQsFrame
        L_0x003a:
            android.widget.FrameLayout r4 = r5.mQsFrame
            if (r7 != r4) goto L_0x0053
            if (r0 == 0) goto L_0x0045
            if (r3 == 0) goto L_0x0045
            android.view.View r1 = r5.mUserSwitcher
            goto L_0x004e
        L_0x0045:
            if (r3 == 0) goto L_0x004a
            android.view.View r1 = r5.mKeyguardStatusBar
            goto L_0x004e
        L_0x004a:
            if (r0 == 0) goto L_0x004e
            android.view.View r1 = r5.mUserSwitcher
        L_0x004e:
            boolean r5 = super.drawChild(r6, r1, r8)
            return r5
        L_0x0053:
            com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout r4 = r5.mStackScroller
            if (r7 != r4) goto L_0x0068
            if (r0 == 0) goto L_0x005e
            if (r3 == 0) goto L_0x005e
            android.view.View r2 = r5.mKeyguardStatusBar
            goto L_0x0063
        L_0x005e:
            if (r3 != 0) goto L_0x0062
            if (r0 == 0) goto L_0x0063
        L_0x0062:
            r2 = r1
        L_0x0063:
            boolean r5 = super.drawChild(r6, r2, r8)
            return r5
        L_0x0068:
            android.view.View r4 = r5.mUserSwitcher
            if (r7 != r4) goto L_0x0077
            if (r0 == 0) goto L_0x0071
            if (r3 == 0) goto L_0x0071
            goto L_0x0072
        L_0x0071:
            r1 = r2
        L_0x0072:
            boolean r5 = super.drawChild(r6, r1, r8)
            return r5
        L_0x0077:
            android.view.View r0 = r5.mKeyguardStatusBar
            if (r7 != r0) goto L_0x0080
            boolean r5 = super.drawChild(r6, r2, r8)
            return r5
        L_0x0080:
            boolean r5 = super.drawChild(r6, r7, r8)
            return r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer.drawChild(android.graphics.Canvas, android.view.View, long):boolean");
    }

    public void onInflate(ViewStub viewStub, View view) {
        if (viewStub == this.mUserSwitcher) {
            this.mUserSwitcher = view;
            this.mInflated = true;
        }
    }

    public void onFragmentViewCreated(String str, Fragment fragment) {
        ((C0959QS) fragment).setContainer(this);
    }

    public void setQsExpanded(boolean z) {
        if (this.mQsExpanded != z) {
            this.mQsExpanded = z;
            invalidate();
        }
    }

    public void setCustomizerAnimating(boolean z) {
        if (this.mCustomizerAnimating != z) {
            this.mCustomizerAnimating = z;
            invalidate();
        }
    }

    public void setCustomizerShowing(boolean z) {
        if (z) {
            setPadding(0, 0, 0, 0);
            setBottomMargin(this.mStackScroller, 0);
        } else {
            setPadding(0, 0, 0, this.mBottomPadding);
            setBottomMargin(this.mStackScroller, this.mStackScrollerMargin);
        }
        this.mStackScroller.setQsCustomizerShowing(z);
    }

    private void setBottomMargin(View view, int i) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        layoutParams.bottomMargin = i;
        view.setLayoutParams(layoutParams);
    }

    public void onHasViewsAboveShelfChanged(boolean z) {
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("onHasViewsAboveShelfChanged, hasViewsAboveShelf:");
            sb.append(z);
            sb.append(", stack:");
            sb.append(Log.getStackTraceString(new Throwable()));
            Log.i("NotificationsQuickSettingsContainer", sb.toString());
        }
        this.mHasViewsAboveShelf = z;
        invalidate();
    }

    public void onHeadsUpChanged(boolean z) {
        if (OpUtils.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append("onHeadsUpChanged, value:");
            sb.append(z);
            Log.i("NotificationsQuickSettingsContainer", sb.toString());
        }
        this.mHeadUpStatus = z;
    }

    private int getStackScrollerBottomMargin(int i) {
        if (i == 1) {
            return getResources().getDimensionPixelSize(R$dimen.dismiss_all_button_margin_bottom) + getResources().getDimensionPixelSize(R$dimen.dismiss_all_button_height) + getResources().getDimensionPixelSize(R$dimen.dismiss_all_button_margin_top);
        }
        return getResources().getDimensionPixelSize(R$dimen.close_handle_underlap);
    }
}

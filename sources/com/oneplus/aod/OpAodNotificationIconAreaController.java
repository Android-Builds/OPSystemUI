package com.oneplus.aod;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.internal.util.ContrastColorUtil;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationIconDozeHelper;
import com.android.systemui.statusbar.notification.collection.NotificationData;
import com.android.systemui.statusbar.phone.NotificationIconContainer;
import com.oneplus.util.OpUtils;

public class OpAodNotificationIconAreaController {
    private Context mContext;
    private final ContrastColorUtil mContrastColorUtil;
    private NotificationEntryManager mEntryManager = ((NotificationEntryManager) Dependency.get(NotificationEntryManager.class));
    private int mIconHPadding;
    private int mIconSize;
    private TextView mMoreIcon;
    private View mNotificationIconArea;
    private NotificationIconDozeHelper mNotificationIconDozeHelper;
    private OpIconMerger mNotificationIcons;
    private int mUserId;

    public OpAodNotificationIconAreaController(Context context, ViewGroup viewGroup) {
        this.mContrastColorUtil = ContrastColorUtil.getInstance(context);
        this.mNotificationIconDozeHelper = new NotificationIconDozeHelper(context);
        this.mContext = context;
        this.mUserId = KeyguardUpdateMonitor.getCurrentUser();
        initViews(viewGroup);
    }

    public void initViews(ViewGroup viewGroup) {
        this.mNotificationIconArea = viewGroup.findViewById(R$id.notification_icon_area_inner);
        this.mNotificationIcons = (OpIconMerger) this.mNotificationIconArea.findViewById(R$id.notificationIcons);
        this.mMoreIcon = (TextView) this.mNotificationIconArea.findViewById(R$id.moreIcon);
        if (OpUtils.isMCLVersion()) {
            Typeface mclTypeface = OpUtils.getMclTypeface(3);
            if (mclTypeface != null) {
                this.mMoreIcon.setTypeface(mclTypeface);
            }
        }
        reloadDimens();
    }

    private LayoutParams generateIconLayoutParams(int i) {
        int i2 = this.mIconSize;
        LayoutParams layoutParams = new LayoutParams(i2, i2);
        if (i != 0) {
            layoutParams.setMarginStart(this.mIconHPadding);
        }
        return layoutParams;
    }

    public void onUserSwitchComplete(int i) {
        this.mUserId = i;
    }

    private void reloadDimens() {
        Resources resources = this.mContext.getResources();
        this.mIconSize = resources.getDimensionPixelSize(R$dimen.aod_notification_icon_size);
        this.mIconHPadding = resources.getDimensionPixelSize(R$dimen.aod_notification_icon_padding);
        ((LayoutParams) this.mMoreIcon.getLayoutParams()).setMarginStart(this.mContext.getResources().getDimensionPixelSize(R$dimen.aod_notification_icon_padding));
    }

    public void updateNotificationIcons(NotificationIconContainer notificationIconContainer) {
        int intForUser = Secure.getIntForUser(this.mContext.getContentResolver(), "aod_clock_style", 0, this.mUserId);
        if (2 != intForUser) {
            int i = 3;
            if (3 != intForUser) {
                reloadDimens();
                int childCount = notificationIconContainer.getChildCount();
                this.mNotificationIcons.removeAllViews();
                boolean z = childCount > 3;
                String str = "AodNotificationIconArea";
                if (OpUtils.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("updateNotificationIcons: iconSize=");
                    sb.append(childCount);
                    sb.append(", maxIconAmounts=");
                    sb.append(3);
                    sb.append(", showMore=");
                    sb.append(z);
                    Log.d(str, sb.toString());
                }
                if (childCount == 0) {
                    Log.d(str, "updateNotificationIcons: setVisibility to gone");
                    this.mNotificationIconArea.setVisibility(8);
                    return;
                }
                MarginLayoutParams marginLayoutParams = (MarginLayoutParams) this.mNotificationIconArea.getLayoutParams();
                if (intForUser == 0) {
                    marginLayoutParams.topMargin = this.mContext.getResources().getDimensionPixelSize(R$dimen.notification_icon_default_empty_view_height);
                } else if (intForUser == 1) {
                    marginLayoutParams.topMargin = this.mContext.getResources().getDimensionPixelSize(R$dimen.notification_icon_analog_empty_view_height);
                } else if (intForUser == 10) {
                    marginLayoutParams.topMargin = OpUtils.convertDpToFixedPx(this.mContext.getResources().getDimension(R$dimen.aod_mcl_notification_icon_marginTop));
                }
                this.mNotificationIconArea.setVisibility(0);
                if (z) {
                    this.mMoreIcon.setVisibility(0);
                    TextView textView = this.mMoreIcon;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("+");
                    sb2.append(childCount - 3);
                    textView.setText(sb2.toString());
                } else {
                    this.mMoreIcon.setVisibility(8);
                }
                if (!z) {
                    i = childCount;
                }
                this.mNotificationIcons.removeAllViews();
                NotificationData notificationData = this.mEntryManager.getNotificationData();
                for (int i2 = 0; i2 < i; i2++) {
                    StatusBarIconView statusBarIconView = (StatusBarIconView) notificationIconContainer.getChildAt(i2);
                    if (statusBarIconView != null && notificationData.isHighPriority(statusBarIconView.getNotification())) {
                        OpAodNotificationIconView opAodNotificationIconView = new OpAodNotificationIconView(this.mContext, statusBarIconView.getSlot(), statusBarIconView.getNotification().getNotification());
                        opAodNotificationIconView.set(statusBarIconView.getStatusBarIcon());
                        LayoutParams generateIconLayoutParams = generateIconLayoutParams(i2);
                        if (this.mContrastColorUtil.isGrayscaleIcon(opAodNotificationIconView.getDrawable())) {
                            opAodNotificationIconView.setImageTintList(ColorStateList.valueOf(-1));
                        } else {
                            this.mNotificationIconDozeHelper.setImageDark(opAodNotificationIconView, true, false, 0, true);
                        }
                        this.mNotificationIcons.addView(opAodNotificationIconView, generateIconLayoutParams);
                    }
                }
                return;
            }
        }
        this.mNotificationIconArea.setVisibility(8);
    }
}

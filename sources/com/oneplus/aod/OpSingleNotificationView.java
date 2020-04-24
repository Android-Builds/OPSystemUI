package com.oneplus.aod;

import android.app.Notification;
import android.app.Notification.Builder;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.internal.util.ContrastColorUtil;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$style;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.oneplus.plugin.OpLsState;
import com.oneplus.util.OpUtils;

public class OpSingleNotificationView extends LinearLayout {
    private Context mContext;
    private TextView mHeader;
    private LinearLayout mHeaderContainer;
    private ImageView mIcon;
    private StatusBarNotification mNewPostedNotification;
    private LinearLayout mNotificationViewCustom;
    private LinearLayout mNotificationViewDefault;
    private TextView mSmallText;
    private TextView mTitle;

    public OpSingleNotificationView(Context context) {
        super(context);
    }

    public OpSingleNotificationView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
    }

    public OpSingleNotificationView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mContext = context;
    }

    public OpSingleNotificationView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mIcon = (ImageView) findViewById(R$id.single_notification_icon);
        this.mHeader = (TextView) findViewById(R$id.single_notification_header);
        this.mHeaderContainer = (LinearLayout) findViewById(R$id.header_container);
        this.mTitle = (TextView) findViewById(R$id.single_notification_title);
        this.mSmallText = (TextView) findViewById(R$id.single_notification_smallText);
        this.mNotificationViewDefault = (LinearLayout) findViewById(R$id.notification_default);
        this.mNotificationViewCustom = (LinearLayout) findViewById(R$id.notificaiton_custom);
        adjustNotificationMargin();
    }

    private void adjustNotificationMargin() {
        if (this.mNotificationViewDefault != null) {
            int convertDpToFixedPx = OpUtils.convertDpToFixedPx(getResources().getDimension(R$dimen.single_notification_horizontal_margin));
            LayoutParams layoutParams = (LayoutParams) this.mNotificationViewDefault.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.setMarginStart(convertDpToFixedPx);
                layoutParams.setMarginEnd(convertDpToFixedPx);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        int i = 1;
        if (this.mContext.getResources().getConfiguration().getLayoutDirection() != 1) {
            i = 0;
        }
        ViewGroup.LayoutParams layoutParams = this.mIcon.getLayoutParams();
        layoutParams.width = this.mContext.getResources().getDimensionPixelSize(R$dimen.single_notification_icon_width);
        layoutParams.height = this.mContext.getResources().getDimensionPixelSize(R$dimen.single_notification_icon_height);
        this.mIcon.setLayoutParams(layoutParams);
        this.mHeaderContainer.setTextDirection(i);
        this.mHeader.setTextAppearance(R$style.single_notification_header);
        int i2 = 4;
        this.mTitle.setTextDirection(i != 0 ? 4 : 3);
        this.mTitle.setTextAppearance(R$style.single_notification_title);
        TextView textView = this.mSmallText;
        if (i == 0) {
            i2 = 3;
        }
        textView.setTextDirection(i2);
        this.mSmallText.setTextAppearance(R$style.single_notification_smallText);
    }

    public void onNotificationHeadsUp(NotificationEntry notificationEntry) {
        this.mNewPostedNotification = notificationEntry.notification;
        updateViewInternal(notificationEntry);
    }

    private void updateViewInternal(NotificationEntry notificationEntry) {
        int i;
        String str = "SingleNotificationView";
        Log.d(str, "updateViewInternal");
        StatusBarNotification statusBarNotification = notificationEntry.notification;
        Bundle bundle = statusBarNotification.getNotification().extras;
        CharSequence charSequence = bundle.getCharSequence("android.title");
        CharSequence charSequence2 = bundle.getCharSequence("android.text");
        CharSequence[] charSequenceArray = bundle.getCharSequenceArray("android.textLines");
        if (charSequenceArray != null) {
            charSequence2 = charSequenceArray[charSequenceArray.length - 1];
        }
        Icon smallIcon = statusBarNotification.getNotification().getSmallIcon();
        showCustomNotification(false, null);
        try {
            showCustomNotification(false, null);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Exception e = ");
            sb.append(e.toString());
            Log.e(str, sb.toString());
        }
        int i2 = statusBarNotification.getNotification().color;
        if (i2 == 0) {
            i = this.mContext.getResources().getColor(17170443);
        } else {
            i = ContrastColorUtil.changeColorLightness(i2, 25);
        }
        boolean shouldHideSensitive = OpLsState.getInstance().getPhoneStatusBar().shouldHideSensitive(notificationEntry);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("updateViewInternal: custom=");
        sb2.append(false);
        sb2.append(", hideSensitivie=");
        sb2.append(shouldHideSensitive);
        sb2.append(", isLock=");
        sb2.append(notificationEntry.getRow().isUserLocked());
        sb2.append(", color=0x");
        sb2.append(Integer.toHexString(i2));
        sb2.append(", headerColor=0x");
        sb2.append(Integer.toHexString(i));
        sb2.append(", titleVis = ");
        TextView textView = this.mTitle;
        Object obj = "null";
        sb2.append(textView != null ? Integer.valueOf(textView.getVisibility()) : obj);
        sb2.append(", smallTextVis = ");
        TextView textView2 = this.mSmallText;
        if (textView2 != null) {
            obj = Integer.valueOf(textView2.getVisibility());
        }
        sb2.append(obj);
        Log.d(str, sb2.toString());
        if (this.mHeader != null) {
            String resolveAppName = resolveAppName(statusBarNotification);
            if (resolveAppName != null) {
                this.mHeader.setText(resolveAppName);
                this.mHeader.setTextColor(i);
            }
        } else {
            StringBuilder sb3 = new StringBuilder();
            sb3.append(statusBarNotification.getKey());
            sb3.append(" mHeader is null");
            Log.w(str, sb3.toString());
        }
        if (this.mIcon == null || smallIcon == null) {
            StringBuilder sb4 = new StringBuilder();
            sb4.append(statusBarNotification.getKey());
            sb4.append(" mIcon and icon is null");
            Log.w(str, sb4.toString());
        } else {
            Drawable loadDrawable = smallIcon.loadDrawable(this.mContext);
            if (loadDrawable == null) {
                Log.d(str, "drawable = null");
                return;
            }
            Drawable newDrawable = loadDrawable.getConstantState().newDrawable();
            this.mIcon.setColorFilter(null);
            if (i2 != 0) {
                this.mIcon.setColorFilter(i);
            }
            if (smallIcon != null) {
                this.mIcon.setImageDrawable(newDrawable);
            } else {
                Log.d(str, "private layout icon null");
            }
        }
        TextView textView3 = this.mSmallText;
        if (textView3 != null) {
            TextView textView4 = this.mTitle;
            if (textView4 != null) {
                String str2 = "";
                if (shouldHideSensitive) {
                    textView3.setText(this.mContext.getResources().getQuantityString(84738048, 1, new Object[]{Integer.valueOf(1)}));
                    if (TextUtils.isEmpty(this.mSmallText.getText())) {
                        Log.d(str, "small text content is empty");
                    }
                    this.mSmallText.setVisibility(0);
                    this.mTitle.setText(str2);
                    this.mTitle.setVisibility(8);
                    return;
                }
                textView4.setText(str2);
                this.mSmallText.setText(str2);
                if (charSequence != null) {
                    this.mTitle.setVisibility(0);
                    this.mTitle.setText(charSequence.toString());
                } else {
                    this.mTitle.setVisibility(8);
                }
                if (charSequence2 != null) {
                    this.mSmallText.setVisibility(0);
                    this.mSmallText.setText(charSequence2.toString());
                    if (TextUtils.isEmpty(charSequence2)) {
                        Log.d(str, "small text is null or empty");
                    }
                } else {
                    this.mSmallText.setVisibility(8);
                    Log.d(str, "small text is null");
                }
                return;
            }
        }
        StringBuilder sb5 = new StringBuilder();
        sb5.append("Title = ");
        sb5.append(this.mTitle);
        sb5.append(" or SmallText = ");
        sb5.append(this.mSmallText);
        sb5.append(" is null");
        Log.w(str, sb5.toString());
    }

    private void showCustomNotification(boolean z, View view) {
        this.mNotificationViewCustom.removeAllViews();
        if (!z) {
            this.mNotificationViewCustom.setVisibility(8);
            this.mNotificationViewDefault.setVisibility(0);
            return;
        }
        this.mNotificationViewCustom.addView(view);
        this.mNotificationViewCustom.setVisibility(0);
        this.mNotificationViewDefault.setVisibility(8);
    }

    private String resolveAppName(StatusBarNotification statusBarNotification) {
        Notification notification = statusBarNotification.getNotification();
        try {
            return Builder.recoverBuilder(this.mContext, notification).loadHeaderAppName();
        } catch (RuntimeException e) {
            Log.e("SingleNotificationView", "Unable to recover builder", e);
            Parcelable parcelable = notification.extras.getParcelable("android.appInfo");
            if (parcelable instanceof ApplicationInfo) {
                return String.valueOf(((ApplicationInfo) parcelable).loadLabel(this.mContext.getPackageManager()));
            }
            return null;
        }
    }

    public void updateRTL(int i) {
        int i2 = 1;
        if (i != 1) {
            i2 = 0;
        }
        this.mHeaderContainer.setLayoutDirection(i2);
        invalidate();
    }
}

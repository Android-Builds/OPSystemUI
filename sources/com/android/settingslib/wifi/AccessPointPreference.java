package com.android.settingslib.wifi;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.os.Looper;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.OpFeatures;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import com.android.settingslib.R$attr;
import com.android.settingslib.R$dimen;
import com.android.settingslib.R$drawable;
import com.android.settingslib.R$id;
import com.android.settingslib.R$layout;
import com.android.settingslib.R$string;

public class AccessPointPreference extends Preference {
    private static final int[] FRICTION_ATTRS = {R$attr.wifi_friction};
    private static final int[] STATE_METERED = {R$attr.state_metered};
    private static final int[] STATE_SECURED = {R$attr.state_encrypted};
    private static final int[] WIFI_CONNECTION_STRENGTH = {R$string.accessibility_no_wifi, R$string.accessibility_wifi_one_bar, R$string.accessibility_wifi_two_bars, R$string.accessibility_wifi_three_bars, R$string.accessibility_wifi_signal_full};
    private AccessPoint mAccessPoint;
    private Drawable mBadge;
    private final UserBadgeCache mBadgeCache;
    private final int mBadgePadding;
    private CharSequence mContentDescription;
    private Context mContext;
    private int mDefaultIconResId;
    private boolean mForSavedNetworks;
    private final StateListDrawable mFrictionSld;
    private final IconInjector mIconInjector;
    private int mLevel;
    private final Runnable mNotifyChanged;
    private boolean mShowDivider;
    private TextView mTitleView;
    private int mWifiSpeed;

    static class IconInjector {
        private final Context mContext;

        public IconInjector(Context context) {
            this.mContext = context;
        }
    }

    public static class UserBadgeCache {
    }

    public AccessPointPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mForSavedNetworks = false;
        this.mWifiSpeed = 0;
        this.mNotifyChanged = new Runnable() {
            public void run() {
                AccessPointPreference.this.notifyChanged();
            }
        };
        this.mFrictionSld = null;
        this.mBadgePadding = 0;
        this.mBadgeCache = null;
        this.mIconInjector = new IconInjector(context);
        this.mContext = context;
    }

    AccessPointPreference(AccessPoint accessPoint, Context context, UserBadgeCache userBadgeCache, int i, boolean z, StateListDrawable stateListDrawable, int i2, IconInjector iconInjector) {
        super(context);
        this.mForSavedNetworks = false;
        this.mWifiSpeed = 0;
        this.mNotifyChanged = new Runnable() {
            public void run() {
                AccessPointPreference.this.notifyChanged();
            }
        };
        setLayoutResource(R$layout.preference_access_point);
        setWidgetLayoutResource(getWidgetLayoutResourceId());
        this.mBadgeCache = userBadgeCache;
        this.mAccessPoint = accessPoint;
        this.mForSavedNetworks = z;
        this.mAccessPoint.setTag(this);
        this.mLevel = i2;
        this.mDefaultIconResId = i;
        this.mFrictionSld = stateListDrawable;
        this.mIconInjector = iconInjector;
        this.mBadgePadding = context.getResources().getDimensionPixelSize(R$dimen.wifi_preference_badge_padding);
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public int getWidgetLayoutResourceId() {
        return R$layout.access_point_friction_widget;
    }

    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        if (this.mAccessPoint != null) {
            Drawable icon = getIcon();
            if (icon != null) {
                icon.setLevel(this.mLevel);
            }
            this.mTitleView = (TextView) preferenceViewHolder.findViewById(16908310);
            TextView textView = this.mTitleView;
            if (textView != null) {
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, this.mBadge, null);
                this.mTitleView.setCompoundDrawablePadding(this.mBadgePadding);
            }
            preferenceViewHolder.itemView.setContentDescription(this.mContentDescription);
            bindFrictionImage((ImageView) preferenceViewHolder.findViewById(R$id.friction_icon));
            ImageView imageView = (ImageView) preferenceViewHolder.findViewById(R$id.icon_passpoint);
            int i = 0;
            boolean z = System.getInt(this.mContext.getContentResolver(), "oneplus_passpoint", 0) == 1;
            if (!OpFeatures.isSupport(new int[]{93}) || !z || this.mAccessPoint.getDetailedState() != DetailedState.CONNECTED) {
                imageView.setVisibility(8);
            } else {
                imageView.setVisibility(0);
                WifiConfiguration config = this.mAccessPoint.getConfig();
                if (config == null || !config.isPasspoint()) {
                    imageView.setVisibility(8);
                } else if (config.isHomeProviderNetwork) {
                    imageView.setImageDrawable(this.mContext.getResources().getDrawable(R$drawable.ic_passpoint_r));
                } else {
                    imageView.setImageDrawable(this.mContext.getResources().getDrawable(R$drawable.ic_passpoint_h));
                }
            }
            View findViewById = preferenceViewHolder.findViewById(R$id.two_target_divider);
            if (!shouldShowDivider()) {
                i = 4;
            }
            findViewById.setVisibility(i);
        }
    }

    public boolean shouldShowDivider() {
        return this.mShowDivider;
    }

    private void bindFrictionImage(ImageView imageView) {
        if (imageView != null && this.mFrictionSld != null) {
            if (this.mAccessPoint.getSecurity() != 0 && this.mAccessPoint.getSecurity() != 4 && this.mAccessPoint.getSecurity() != 8) {
                this.mFrictionSld.setState(STATE_SECURED);
            } else if (this.mAccessPoint.isMetered()) {
                this.mFrictionSld.setState(STATE_METERED);
            }
            imageView.setImageDrawable(this.mFrictionSld.getCurrent());
        }
    }

    /* access modifiers changed from: protected */
    public void notifyChanged() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            postNotifyChanged();
        } else {
            super.notifyChanged();
        }
    }

    static void setTitle(AccessPointPreference accessPointPreference, AccessPoint accessPoint) {
        accessPointPreference.setTitle((CharSequence) accessPoint.getTitle());
    }

    static CharSequence buildContentDescription(Context context, Preference preference, AccessPoint accessPoint) {
        String str;
        CharSequence title = preference.getTitle();
        CharSequence summary = preference.getSummary();
        String str2 = ",";
        if (!TextUtils.isEmpty(summary)) {
            title = TextUtils.concat(new CharSequence[]{title, str2, summary});
        }
        int level = accessPoint.getLevel();
        if (level >= 0) {
            int[] iArr = WIFI_CONNECTION_STRENGTH;
            if (level < iArr.length) {
                title = TextUtils.concat(new CharSequence[]{title, str2, context.getString(iArr[level])});
            }
        }
        CharSequence[] charSequenceArr = new CharSequence[3];
        charSequenceArr[0] = title;
        charSequenceArr[1] = str2;
        if (accessPoint.getSecurity() == 0) {
            str = context.getString(R$string.accessibility_wifi_security_type_none);
        } else {
            str = context.getString(R$string.accessibility_wifi_security_type_secured);
        }
        charSequenceArr[2] = str;
        return TextUtils.concat(charSequenceArr);
    }

    private void postNotifyChanged() {
        TextView textView = this.mTitleView;
        if (textView != null) {
            textView.post(this.mNotifyChanged);
        }
    }
}

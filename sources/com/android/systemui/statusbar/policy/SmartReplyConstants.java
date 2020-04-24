package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.provider.DeviceConfig;
import android.provider.DeviceConfig.OnPropertiesChangedListener;
import android.provider.DeviceConfig.Properties;
import android.text.TextUtils;
import android.util.KeyValueListParser;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.R$bool;
import com.android.systemui.R$integer;
import java.util.concurrent.Executor;

public final class SmartReplyConstants {
    private final Context mContext;
    private final boolean mDefaultEditChoicesBeforeSending;
    private final boolean mDefaultEnabled;
    private final int mDefaultMaxNumActions;
    private final int mDefaultMaxSqueezeRemeasureAttempts;
    private final int mDefaultMinNumSystemGeneratedReplies;
    private final int mDefaultOnClickInitDelay;
    private final boolean mDefaultRequiresP;
    private final boolean mDefaultShowInHeadsUp;
    private volatile boolean mEditChoicesBeforeSending;
    private volatile boolean mEnabled;
    private final Handler mHandler;
    private volatile int mMaxNumActions;
    private volatile int mMaxSqueezeRemeasureAttempts;
    private volatile int mMinNumSystemGeneratedReplies;
    private volatile long mOnClickInitDelay;
    private final KeyValueListParser mParser = new KeyValueListParser(',');
    private volatile boolean mRequiresTargetingP;
    private volatile boolean mShowInHeadsUp;

    public SmartReplyConstants(Handler handler, Context context) {
        this.mHandler = handler;
        this.mContext = context;
        Resources resources = this.mContext.getResources();
        this.mDefaultEnabled = resources.getBoolean(R$bool.config_smart_replies_in_notifications_enabled);
        this.mDefaultRequiresP = resources.getBoolean(R$bool.config_smart_replies_in_notifications_requires_targeting_p);
        this.mDefaultMaxSqueezeRemeasureAttempts = resources.getInteger(R$integer.f61xb8282359);
        this.mDefaultEditChoicesBeforeSending = resources.getBoolean(R$bool.f58xa48abd95);
        this.mDefaultShowInHeadsUp = resources.getBoolean(R$bool.config_smart_replies_in_notifications_show_in_heads_up);
        this.mDefaultMinNumSystemGeneratedReplies = resources.getInteger(R$integer.f62xce369515);
        this.mDefaultMaxNumActions = resources.getInteger(R$integer.config_smart_replies_in_notifications_max_num_actions);
        this.mDefaultOnClickInitDelay = resources.getInteger(R$integer.config_smart_replies_in_notifications_onclick_init_delay);
        registerDeviceConfigListener();
        updateConstants();
    }

    private void registerDeviceConfigListener() {
        DeviceConfig.addOnPropertiesChangedListener("systemui", new Executor() {
            public final void execute(Runnable runnable) {
                SmartReplyConstants.this.postToHandler(runnable);
            }
        }, new OnPropertiesChangedListener() {
            public final void onPropertiesChanged(Properties properties) {
                SmartReplyConstants.this.lambda$registerDeviceConfigListener$0$SmartReplyConstants(properties);
            }
        });
    }

    public /* synthetic */ void lambda$registerDeviceConfigListener$0$SmartReplyConstants(Properties properties) {
        onDeviceConfigPropertiesChanged(properties.getNamespace());
    }

    /* access modifiers changed from: private */
    public void postToHandler(Runnable runnable) {
        this.mHandler.post(runnable);
    }

    /* access modifiers changed from: 0000 */
    @VisibleForTesting
    public void onDeviceConfigPropertiesChanged(String str) {
        if (!"systemui".equals(str)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Received update from DeviceConfig for unrelated namespace: ");
            sb.append(str);
            Log.e("SmartReplyConstants", sb.toString());
            return;
        }
        updateConstants();
    }

    private void updateConstants() {
        synchronized (this) {
            this.mEnabled = readDeviceConfigBooleanOrDefaultIfEmpty("ssin_enabled", this.mDefaultEnabled);
            this.mRequiresTargetingP = readDeviceConfigBooleanOrDefaultIfEmpty("ssin_requires_targeting_p", this.mDefaultRequiresP);
            this.mMaxSqueezeRemeasureAttempts = DeviceConfig.getInt("systemui", "ssin_max_squeeze_remeasure_attempts", this.mDefaultMaxSqueezeRemeasureAttempts);
            this.mEditChoicesBeforeSending = readDeviceConfigBooleanOrDefaultIfEmpty("ssin_edit_choices_before_sending", this.mDefaultEditChoicesBeforeSending);
            this.mShowInHeadsUp = readDeviceConfigBooleanOrDefaultIfEmpty("ssin_show_in_heads_up", this.mDefaultShowInHeadsUp);
            this.mMinNumSystemGeneratedReplies = DeviceConfig.getInt("systemui", "ssin_min_num_system_generated_replies", this.mDefaultMinNumSystemGeneratedReplies);
            this.mMaxNumActions = DeviceConfig.getInt("systemui", "ssin_max_num_actions", this.mDefaultMaxNumActions);
            this.mOnClickInitDelay = (long) DeviceConfig.getInt("systemui", "ssin_onclick_init_delay", this.mDefaultOnClickInitDelay);
        }
    }

    private static boolean readDeviceConfigBooleanOrDefaultIfEmpty(String str, boolean z) {
        String property = DeviceConfig.getProperty("systemui", str);
        if (TextUtils.isEmpty(property)) {
            return z;
        }
        if ("true".equals(property)) {
            return true;
        }
        if ("false".equals(property)) {
            return false;
        }
        return z;
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public boolean requiresTargetingP() {
        return this.mRequiresTargetingP;
    }

    public int getMaxSqueezeRemeasureAttempts() {
        return this.mMaxSqueezeRemeasureAttempts;
    }

    public boolean getEffectiveEditChoicesBeforeSending(int i) {
        if (i == 1) {
            return false;
        }
        if (i != 2) {
            return this.mEditChoicesBeforeSending;
        }
        return true;
    }

    public boolean getShowInHeadsUp() {
        return this.mShowInHeadsUp;
    }

    public int getMinNumSystemGeneratedReplies() {
        return this.mMinNumSystemGeneratedReplies;
    }

    public int getMaxNumActions() {
        return this.mMaxNumActions;
    }

    public long getOnClickInitDelay() {
        return this.mOnClickInitDelay;
    }
}

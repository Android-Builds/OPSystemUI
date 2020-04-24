package com.android.systemui.statusbar.notification.row;

import android.app.Dialog;
import android.app.INotificationManager;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.statusbar.notification.row.NotificationInfo.OnSettingsClickListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: ChannelEditorDialogController.kt */
public final class ChannelEditorDialogController {
    private Drawable appIcon;
    private String appName;
    private boolean appNotificationsEnabled = true;
    private Integer appUid;
    private final List<NotificationChannelGroup> channelGroupList = new ArrayList();
    private final Context context;
    public Dialog dialog;
    private final Map<NotificationChannel, Integer> edits = new LinkedHashMap();
    private final HashMap<String, CharSequence> groupNameLookup = new HashMap<>();
    private final INotificationManager noMan;
    private OnChannelEditorDialogFinishedListener onFinishListener;
    private OnSettingsClickListener onSettingsClickListener;
    private String packageName;
    private final List<NotificationChannel> providedChannels = new ArrayList();
    private final int wmFlags = -2130444032;

    @VisibleForTesting
    public static /* synthetic */ void groupNameLookup$annotations() {
    }

    @VisibleForTesting
    public static /* synthetic */ void providedChannels$annotations() {
    }

    public ChannelEditorDialogController(Context context2, INotificationManager iNotificationManager) {
        Intrinsics.checkParameterIsNotNull(context2, "c");
        Intrinsics.checkParameterIsNotNull(iNotificationManager, "noMan");
        this.noMan = iNotificationManager;
        Context applicationContext = context2.getApplicationContext();
        Intrinsics.checkExpressionValueIsNotNull(applicationContext, "c.applicationContext");
        this.context = applicationContext;
    }

    public final void setOnFinishListener(OnChannelEditorDialogFinishedListener onChannelEditorDialogFinishedListener) {
        this.onFinishListener = onChannelEditorDialogFinishedListener;
    }

    public final void close() {
        done();
    }

    private final void done() {
        resetState();
        Dialog dialog2 = this.dialog;
        if (dialog2 != null) {
            dialog2.dismiss();
            OnChannelEditorDialogFinishedListener onChannelEditorDialogFinishedListener = this.onFinishListener;
            if (onChannelEditorDialogFinishedListener != null) {
                onChannelEditorDialogFinishedListener.onChannelEditorDialogFinished();
                return;
            }
            return;
        }
        Intrinsics.throwUninitializedPropertyAccessException("dialog");
        throw null;
    }

    private final void resetState() {
        this.appIcon = null;
        this.appUid = null;
        this.packageName = null;
        this.appName = null;
        this.edits.clear();
        this.providedChannels.clear();
        this.groupNameLookup.clear();
    }

    public final void proposeEditForChannel(NotificationChannel notificationChannel, int i) {
        Intrinsics.checkParameterIsNotNull(notificationChannel, "channel");
        if (notificationChannel.getImportance() == i) {
            this.edits.remove(notificationChannel);
        } else {
            this.edits.put(notificationChannel, Integer.valueOf(i));
        }
    }

    private final boolean checkAreAppNotificationsOn() {
        try {
            INotificationManager iNotificationManager = this.noMan;
            String str = this.packageName;
            if (str != null) {
                Integer num = this.appUid;
                if (num != null) {
                    return iNotificationManager.areNotificationsEnabledForPackage(str, num.intValue());
                }
                Intrinsics.throwNpe();
                throw null;
            }
            Intrinsics.throwNpe();
            throw null;
        } catch (Exception e) {
            Log.e("ChannelDialogController", "Error calling NoMan", e);
            return false;
        }
    }

    private final void applyAppNotificationsOn(boolean z) {
        try {
            INotificationManager iNotificationManager = this.noMan;
            String str = this.packageName;
            if (str != null) {
                Integer num = this.appUid;
                if (num != null) {
                    iNotificationManager.setNotificationsEnabledForPackage(str, num.intValue(), z);
                } else {
                    Intrinsics.throwNpe();
                    throw null;
                }
            } else {
                Intrinsics.throwNpe();
                throw null;
            }
        } catch (Exception e) {
            Log.e("ChannelDialogController", "Error calling NoMan", e);
        }
    }

    private final void setChannelImportance(NotificationChannel notificationChannel, int i) {
        try {
            notificationChannel.setImportance(i);
            INotificationManager iNotificationManager = this.noMan;
            String str = this.packageName;
            if (str != null) {
                Integer num = this.appUid;
                if (num != null) {
                    iNotificationManager.updateNotificationChannelForPackage(str, num.intValue(), notificationChannel);
                } else {
                    Intrinsics.throwNpe();
                    throw null;
                }
            } else {
                Intrinsics.throwNpe();
                throw null;
            }
        } catch (Exception e) {
            Log.e("ChannelDialogController", "Unable to update notification importance", e);
        }
    }

    @VisibleForTesting
    public final void apply() {
        for (Entry entry : this.edits.entrySet()) {
            NotificationChannel notificationChannel = (NotificationChannel) entry.getKey();
            int intValue = ((Number) entry.getValue()).intValue();
            if (notificationChannel.getImportance() != intValue) {
                setChannelImportance(notificationChannel, intValue);
            }
        }
        if (this.appNotificationsEnabled != checkAreAppNotificationsOn()) {
            applyAppNotificationsOn(this.appNotificationsEnabled);
        }
    }

    @VisibleForTesting
    public final void launchSettings(View view) {
        Intrinsics.checkParameterIsNotNull(view, "sender");
        OnSettingsClickListener onSettingsClickListener2 = this.onSettingsClickListener;
        if (onSettingsClickListener2 != null) {
            Integer num = this.appUid;
            if (num != null) {
                onSettingsClickListener2.onClick(view, null, num.intValue());
            } else {
                Intrinsics.throwNpe();
                throw null;
            }
        }
    }
}

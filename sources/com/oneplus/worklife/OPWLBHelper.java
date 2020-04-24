package com.oneplus.worklife;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.System;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.android.systemui.statusbar.NotificationMediaManager;
import java.util.HashSet;
import java.util.Set;

public class OPWLBHelper {
    private static OPWLBHelper sOpwlbHelper;
    private AppOpsManager mAppOpsManager;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.oneplus.wlb.intent.ACTION_RELOAD_NOTIFICATION".equals(action)) {
                if (OPWLBHelper.this.mWLBModeChangeListener != null) {
                    OPWLBHelper.this.mWLBModeChangeListener.onModeChanged();
                }
            } else if ("com.oneplus.intent.action_DISABLE_WLB_FEATURE".equals(action)) {
                int intExtra = intent.getIntExtra("enable", 0);
                StringBuilder sb = new StringBuilder();
                sb.append("FeatureEnable : ");
                sb.append(intExtra);
                Log.d("OPSystemUIWLBHelper", sb.toString());
                System.putInt(OPWLBHelper.this.mContext.getContentResolver(), "worklife_feature_enable", intExtra);
            }
        }
    };
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public int mCurrentMode = 0;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mLastWifiConnected;
    private Set<String> mMediaNotificationKeys = new HashSet();
    private NotificationMediaManager mNotificationMediaManager;
    private PackageManager mPackageManager;
    private ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean z, Uri uri) {
            int access$000 = OPWLBHelper.this.mCurrentMode;
            OPWLBHelper.this.readCurrentMode();
            if (!(access$000 == OPWLBHelper.this.mCurrentMode || OPWLBHelper.this.mWLBModeChangeListener == null)) {
                OPWLBHelper.this.mHandler.post(new Runnable() {
                    public void run() {
                        OPWLBHelper.this.mWLBModeChangeListener.onModeChanged();
                    }
                });
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Current Mode changed to : ");
            sb.append(OPWLBHelper.this.mCurrentMode);
            sb.append(" ");
            sb.append(uri);
            Log.d("OPSystemUIWLBHelper", sb.toString());
        }
    };
    /* access modifiers changed from: private */
    public IWLBModeChangeListener mWLBModeChangeListener;

    public interface IWLBModeChangeListener {
        void onModeChanged();
    }

    public static OPWLBHelper getInstance(Context context) {
        if (sOpwlbHelper == null) {
            sOpwlbHelper = new OPWLBHelper(context);
        }
        return sOpwlbHelper;
    }

    private OPWLBHelper(Context context) {
        this.mContext = context;
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mPackageManager = this.mContext.getPackageManager();
        readCurrentMode();
    }

    public void processWifiConnectivity(boolean z) {
        if (this.mLastWifiConnected != z) {
            sendConnectedBroadcast(z);
            this.mLastWifiConnected = z;
        }
    }

    public void sendShutDownBroadcast() {
        Log.d("OPSystemUIWLBHelper", "sending Shutdown event to WLB");
        Intent intent = new Intent("com.oneplus.intent.ACTION_SHUTDOWN");
        intent.setPackage("com.oneplus.opwlb");
        this.mContext.sendBroadcast(intent);
    }

    private void sendConnectedBroadcast(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("wificonnected ");
        sb.append(z);
        Log.d("OPSystemUIWLBHelper", sb.toString());
        Intent intent = new Intent("com.oneplus.intent.ACTION_WIFI_CONNECTED");
        intent.setPackage("com.oneplus.opwlb");
        intent.putExtra("is_connected", z);
        this.mContext.sendBroadcast(intent);
    }

    public void registerChanges(IWLBModeChangeListener iWLBModeChangeListener) {
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("oneplus_wlb_activated_mode"), false, this.mSettingsObserver);
        IntentFilter intentFilter = new IntentFilter("com.oneplus.wlb.intent.ACTION_RELOAD_NOTIFICATION");
        intentFilter.addAction("com.oneplus.intent.action_DISABLE_WLB_FEATURE");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mWLBModeChangeListener = iWLBModeChangeListener;
    }

    /* access modifiers changed from: private */
    public void readCurrentMode() {
        this.mCurrentMode = System.getInt(this.mContext.getContentResolver(), "oneplus_wlb_activated_mode", 0);
    }

    public boolean isApplicationBlocked(StatusBarNotification statusBarNotification) {
        String packageName = statusBarNotification.getPackageName();
        boolean isMediaNotificationAllowed = isMediaNotificationAllowed(statusBarNotification);
        StringBuilder sb = new StringBuilder();
        sb.append("packageName ");
        sb.append(packageName);
        sb.append(" mCurrentMode ");
        sb.append(this.mCurrentMode);
        sb.append(" isMediaAllowed ");
        sb.append(isMediaNotificationAllowed);
        String str = "OPSystemUIWLBHelper";
        Log.d(str, sb.toString());
        boolean z = false;
        if (packageName != null && !packageName.isEmpty() && this.mCurrentMode != 0 && !isMediaNotificationAllowed) {
            ApplicationInfo applicationInfo = null;
            try {
                applicationInfo = this.mPackageManager.getApplicationInfo(packageName, 128);
            } catch (NameNotFoundException e) {
                Log.d(str, "Couldn't find package", e);
            }
            if (applicationInfo == null) {
                return false;
            }
            int i = this.mCurrentMode;
            String str2 = "isApplicationBlocked ";
            String str3 = " ";
            if (i == 2) {
                int checkOpNoThrow = this.mAppOpsManager.checkOpNoThrow(1009, applicationInfo.uid, packageName);
                StringBuilder sb2 = new StringBuilder();
                sb2.append(str2);
                sb2.append(packageName);
                sb2.append(str3);
                sb2.append(checkOpNoThrow);
                sb2.append(str3);
                sb2.append(this.mCurrentMode);
                Log.d(str, sb2.toString());
                if (checkOpNoThrow == 0) {
                    z = true;
                }
                return z;
            } else if (i == 1) {
                int checkOpNoThrow2 = this.mAppOpsManager.checkOpNoThrow(1008, applicationInfo.uid, packageName);
                StringBuilder sb3 = new StringBuilder();
                sb3.append(str2);
                sb3.append(packageName);
                sb3.append(str3);
                sb3.append(checkOpNoThrow2);
                sb3.append(str3);
                sb3.append(this.mCurrentMode);
                Log.d(str, sb3.toString());
                if (checkOpNoThrow2 == 0) {
                    z = true;
                }
            }
        }
        return z;
    }

    public void removeNotificationKey(String str) {
        if (!this.mMediaNotificationKeys.isEmpty()) {
            this.mMediaNotificationKeys.remove(str);
        }
    }

    public boolean isMediaNotificationAllowed(StatusBarNotification statusBarNotification) {
        if (!(statusBarNotification == null || statusBarNotification.getPackageName() == null)) {
            String key = statusBarNotification.getKey();
            NotificationMediaManager notificationMediaManager = this.mNotificationMediaManager;
            if (notificationMediaManager != null) {
                String mediaNotificationKey = notificationMediaManager.getMediaNotificationKey();
                String str = "OPSystemUIWLBHelper";
                if (mediaNotificationKey != null && mediaNotificationKey.equals(key)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Added Media Notification Key :");
                    sb.append(mediaNotificationKey);
                    Log.d(str, sb.toString());
                    this.mMediaNotificationKeys.add(key);
                }
                boolean contains = this.mMediaNotificationKeys.contains(key);
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Allowed Media Notification Key :");
                sb2.append(contains);
                Log.d(str, sb2.toString());
                return contains;
            }
        }
        return false;
    }

    public void setNotificationMediaManager(NotificationMediaManager notificationMediaManager) {
        if (notificationMediaManager != null) {
            this.mNotificationMediaManager = notificationMediaManager;
        }
    }
}

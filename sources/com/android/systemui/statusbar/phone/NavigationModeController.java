package com.android.systemui.statusbar.phone;

import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.om.IOverlayManager;
import android.content.om.IOverlayManager.Stub;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.ApkAssets;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.util.SparseBooleanArray;
import com.android.systemui.Dumpable;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController.DeviceProvisionedListener;
import com.android.systemui.util.NotificationChannels;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class NavigationModeController implements Dumpable {
    /* access modifiers changed from: private */
    public static final String TAG = "NavigationModeController";
    private final Context mContext;
    /* access modifiers changed from: private */
    public Context mCurrentUserContext;
    private final DeviceProvisionedListener mDeviceProvisionedCallback = new DeviceProvisionedListener() {
        public void onDeviceProvisionedChanged() {
            String access$000 = NavigationModeController.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onDeviceProvisionedChanged: ");
            sb.append(NavigationModeController.this.mDeviceProvisionedController.isDeviceProvisioned());
            Log.d(access$000, sb.toString());
            NavigationModeController.this.restoreGesturalNavOverlayIfNecessary();
        }

        public void onUserSetupChanged() {
            String access$000 = NavigationModeController.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onUserSetupChanged: ");
            sb.append(NavigationModeController.this.mDeviceProvisionedController.isCurrentUserSetup());
            Log.d(access$000, sb.toString());
            NavigationModeController.this.restoreGesturalNavOverlayIfNecessary();
        }

        public void onUserSwitched() {
            String access$000 = NavigationModeController.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onUserSwitched: ");
            sb.append(ActivityManagerWrapper.getInstance().getCurrentUserId());
            Log.d(access$000, sb.toString());
            NavigationModeController.this.updateCurrentInteractionMode(true);
            NavigationModeController.this.switchFromGestureNavModeIfNotSupportedByDefaultLauncher();
            NavigationModeController.this.deferGesturalNavOverlayIfNecessary();
        }
    };
    /* access modifiers changed from: private */
    public final DeviceProvisionedController mDeviceProvisionedController;
    /* access modifiers changed from: private */
    public String mLastDefaultLauncher;
    private ArrayList<ModeChangedListener> mListeners = new ArrayList<>();
    private int mMode = 0;
    private final IOverlayManager mOverlayManager;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* JADX WARNING: Removed duplicated region for block: B:12:0x002b  */
        /* JADX WARNING: Removed duplicated region for block: B:16:0x005d  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(android.content.Context r3, android.content.Intent r4) {
            /*
                r2 = this;
                java.lang.String r3 = r4.getAction()
                int r4 = r3.hashCode()
                r0 = -1946981856(0xffffffff8bf36a20, float:-9.3759874E-32)
                r1 = 1
                if (r4 == r0) goto L_0x001e
                r0 = 1358685446(0x50fbe506, float:3.3808724E10)
                if (r4 == r0) goto L_0x0014
                goto L_0x0028
            L_0x0014:
                java.lang.String r4 = "android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED"
                boolean r3 = r3.equals(r4)
                if (r3 == 0) goto L_0x0028
                r3 = r1
                goto L_0x0029
            L_0x001e:
                java.lang.String r4 = "android.intent.action.OVERLAY_CHANGED"
                boolean r3 = r3.equals(r4)
                if (r3 == 0) goto L_0x0028
                r3 = 0
                goto L_0x0029
            L_0x0028:
                r3 = -1
            L_0x0029:
                if (r3 == 0) goto L_0x005d
                if (r3 == r1) goto L_0x002e
                goto L_0x006b
            L_0x002e:
                java.lang.String r3 = com.android.systemui.statusbar.phone.NavigationModeController.TAG
                java.lang.String r4 = "ACTION_PREFERRED_ACTIVITY_CHANGED"
                android.util.Log.d(r3, r4)
                com.android.systemui.statusbar.phone.NavigationModeController r3 = com.android.systemui.statusbar.phone.NavigationModeController.this
                android.content.Context r4 = r3.mCurrentUserContext
                java.lang.String r3 = r3.getDefaultLauncherPackageName(r4)
                com.android.systemui.statusbar.phone.NavigationModeController r4 = com.android.systemui.statusbar.phone.NavigationModeController.this
                java.lang.String r4 = r4.mLastDefaultLauncher
                boolean r4 = android.text.TextUtils.equals(r4, r3)
                if (r4 != 0) goto L_0x006b
                com.android.systemui.statusbar.phone.NavigationModeController r4 = com.android.systemui.statusbar.phone.NavigationModeController.this
                r4.switchFromGestureNavModeIfNotSupportedByDefaultLauncher()
                com.android.systemui.statusbar.phone.NavigationModeController r4 = com.android.systemui.statusbar.phone.NavigationModeController.this
                r4.showNotificationIfDefaultLauncherSupportsGestureNav()
                com.android.systemui.statusbar.phone.NavigationModeController r2 = com.android.systemui.statusbar.phone.NavigationModeController.this
                r2.mLastDefaultLauncher = r3
                goto L_0x006b
            L_0x005d:
                java.lang.String r3 = com.android.systemui.statusbar.phone.NavigationModeController.TAG
                java.lang.String r4 = "ACTION_OVERLAY_CHANGED"
                android.util.Log.d(r3, r4)
                com.android.systemui.statusbar.phone.NavigationModeController r2 = com.android.systemui.statusbar.phone.NavigationModeController.this
                r2.updateCurrentInteractionMode(r1)
            L_0x006b:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.NavigationModeController.C14611.onReceive(android.content.Context, android.content.Intent):void");
        }
    };
    private SparseBooleanArray mRestoreGesturalNavBarMode = new SparseBooleanArray();
    private final UiOffloadThread mUiOffloadThread;

    public interface ModeChangedListener {
        void onNavigationModeChanged(int i);
    }

    public NavigationModeController(Context context, DeviceProvisionedController deviceProvisionedController, UiOffloadThread uiOffloadThread) {
        Context context2 = context;
        this.mContext = context2;
        this.mCurrentUserContext = context2;
        this.mOverlayManager = Stub.asInterface(ServiceManager.getService("overlay"));
        this.mUiOffloadThread = uiOffloadThread;
        this.mDeviceProvisionedController = deviceProvisionedController;
        this.mDeviceProvisionedController.addCallback(this.mDeviceProvisionedCallback);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.OVERLAY_CHANGED");
        intentFilter.addDataScheme("package");
        intentFilter.addDataSchemeSpecificPart("android", 0);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, null);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED"), null, null);
        this.mLastDefaultLauncher = getDefaultLauncherPackageName(this.mContext);
        updateCurrentInteractionMode(false);
        switchFromGestureNavModeIfNotSupportedByDefaultLauncher();
        deferGesturalNavOverlayIfNecessary();
    }

    public void updateCurrentInteractionMode(boolean z) {
        this.mCurrentUserContext = getCurrentUserContext();
        int currentInteractionMode = getCurrentInteractionMode(this.mCurrentUserContext);
        this.mMode = currentInteractionMode;
        this.mUiOffloadThread.submit(new Runnable(currentInteractionMode) {
            private final /* synthetic */ int f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                NavigationModeController.this.lambda$updateCurrentInteractionMode$0$NavigationModeController(this.f$1);
            }
        });
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("updateCurrentInteractionMode: mode=");
        sb.append(this.mMode);
        sb.append(" contextUser=");
        sb.append(this.mCurrentUserContext.getUserId());
        Log.e(str, sb.toString());
        dumpAssetPaths(this.mCurrentUserContext);
        if (z) {
            for (int i = 0; i < this.mListeners.size(); i++) {
                ((ModeChangedListener) this.mListeners.get(i)).onNavigationModeChanged(currentInteractionMode);
            }
        }
    }

    public /* synthetic */ void lambda$updateCurrentInteractionMode$0$NavigationModeController(int i) {
        Secure.putString(this.mCurrentUserContext.getContentResolver(), "navigation_mode", String.valueOf(i));
    }

    public int addListener(ModeChangedListener modeChangedListener) {
        if (!this.mListeners.contains(modeChangedListener)) {
            this.mListeners.add(modeChangedListener);
        }
        return getCurrentInteractionMode(this.mCurrentUserContext);
    }

    public void removeListener(ModeChangedListener modeChangedListener) {
        this.mListeners.remove(modeChangedListener);
    }

    private int getCurrentInteractionMode(Context context) {
        int integer = context.getResources().getInteger(17694853);
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("getCurrentInteractionMode: mode=");
        sb.append(this.mMode);
        sb.append(" contextUser=");
        sb.append(context.getUserId());
        Log.d(str, sb.toString());
        return integer;
    }

    public Context getCurrentUserContext() {
        int currentUserId = ActivityManagerWrapper.getInstance().getCurrentUserId();
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("getCurrentUserContext: contextUser=");
        sb.append(this.mContext.getUserId());
        sb.append(" currentUser=");
        sb.append(currentUserId);
        Log.d(str, sb.toString());
        if (this.mContext.getUserId() == currentUserId) {
            return this.mContext;
        }
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, UserHandle.of(currentUserId));
        } catch (NameNotFoundException unused) {
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void deferGesturalNavOverlayIfNecessary() {
        int currentUser = this.mDeviceProvisionedController.getCurrentUser();
        this.mRestoreGesturalNavBarMode.put(currentUser, false);
        if (!this.mDeviceProvisionedController.isDeviceProvisioned() || !this.mDeviceProvisionedController.isCurrentUserSetup()) {
            ArrayList arrayList = new ArrayList();
            try {
                arrayList.addAll(Arrays.asList(this.mOverlayManager.getDefaultOverlayPackages()));
            } catch (RemoteException unused) {
                Log.e(TAG, "deferGesturalNavOverlayIfNecessary: failed to fetch default overlays");
            }
            if (!arrayList.contains("com.android.internal.systemui.navbar.gestural")) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("deferGesturalNavOverlayIfNecessary: no default gestural overlay, default=");
                sb.append(arrayList);
                Log.d(str, sb.toString());
                return;
            }
            setModeOverlay("com.android.internal.systemui.navbar.threebutton", -2);
            this.mRestoreGesturalNavBarMode.put(currentUser, true);
            Log.d(TAG, "deferGesturalNavOverlayIfNecessary: setting to 3 button mode");
            return;
        }
        Log.d(TAG, "deferGesturalNavOverlayIfNecessary: device is provisioned and user is setup");
    }

    /* access modifiers changed from: private */
    public void restoreGesturalNavOverlayIfNecessary() {
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("restoreGesturalNavOverlayIfNecessary: needs restore=");
        sb.append(this.mRestoreGesturalNavBarMode);
        Log.d(str, sb.toString());
        int currentUser = this.mDeviceProvisionedController.getCurrentUser();
        if (this.mRestoreGesturalNavBarMode.get(currentUser)) {
            setModeOverlay("com.android.internal.systemui.navbar.gestural", -2);
            this.mRestoreGesturalNavBarMode.put(currentUser, false);
        }
    }

    public void setModeOverlay(String str, int i) {
        this.mUiOffloadThread.submit(new Runnable(str, i) {
            private final /* synthetic */ String f$1;
            private final /* synthetic */ int f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                NavigationModeController.this.lambda$setModeOverlay$1$NavigationModeController(this.f$1, this.f$2);
            }
        });
    }

    public /* synthetic */ void lambda$setModeOverlay$1$NavigationModeController(String str, int i) {
        try {
            this.mOverlayManager.setEnabledExclusiveInCategory(str, i);
            String str2 = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("setModeOverlay: overlayPackage=");
            sb.append(str);
            sb.append(" userId=");
            sb.append(i);
            Log.d(str2, sb.toString());
        } catch (RemoteException unused) {
            String str3 = TAG;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Failed to enable overlay ");
            sb2.append(str);
            sb2.append(" for user ");
            sb2.append(i);
            Log.e(str3, sb2.toString());
        }
    }

    /* access modifiers changed from: private */
    public void switchFromGestureNavModeIfNotSupportedByDefaultLauncher() {
        Log.d(TAG, "not Switching when changing launcher");
    }

    /* access modifiers changed from: private */
    public void showNotificationIfDefaultLauncherSupportsGestureNav() {
        String str = "navigation_mode_controller_preferences";
        String str2 = "switched_from_gesture_nav";
        if (this.mCurrentUserContext.getSharedPreferences(str, 0).getBoolean(str2, false) && getCurrentInteractionMode(this.mCurrentUserContext) != 2) {
            Boolean isGestureNavSupportedByDefaultLauncher = isGestureNavSupportedByDefaultLauncher(this.mCurrentUserContext);
            if (isGestureNavSupportedByDefaultLauncher != null && isGestureNavSupportedByDefaultLauncher.booleanValue()) {
                showNotification(this.mCurrentUserContext, R$string.notification_content_gesture_nav_available);
                this.mCurrentUserContext.getSharedPreferences(str, 0).edit().putBoolean(str2, false).apply();
            }
        }
    }

    private Boolean isGestureNavSupportedByDefaultLauncher(Context context) {
        String defaultLauncherPackageName = getDefaultLauncherPackageName(context);
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("isGestureNavSupportedByDefaultLauncher: defaultLauncher=");
        sb.append(defaultLauncherPackageName);
        sb.append(" contextUser=");
        sb.append(context.getUserId());
        Log.d(str, sb.toString());
        if (defaultLauncherPackageName == null) {
            return null;
        }
        if (defaultLauncherPackageName.equals("com.oneplus.brickmode")) {
            return Boolean.valueOf(true);
        }
        if (isSystemApp(context, defaultLauncherPackageName)) {
            return Boolean.valueOf(true);
        }
        return Boolean.valueOf(false);
    }

    /* access modifiers changed from: private */
    public String getDefaultLauncherPackageName(Context context) {
        ComponentName homeActivities = context.getPackageManager().getHomeActivities(new ArrayList());
        if (homeActivities == null) {
            return null;
        }
        return homeActivities.getPackageName();
    }

    private boolean isSystemApp(Context context, String str) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(str, 128);
            if (applicationInfo == null || (applicationInfo.flags & 129) == 0) {
                return false;
            }
            return true;
        } catch (NameNotFoundException unused) {
            return false;
        }
    }

    private void showNotification(Context context, int i) {
        String string = context.getResources().getString(i);
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("showNotification: message=");
        sb.append(string);
        Log.d(str, sb.toString());
        ((NotificationManager) context.getSystemService(NotificationManager.class)).notify(TAG, 0, new Builder(this.mContext, NotificationChannels.ALERTS).setContentText(string).setStyle(new BigTextStyle()).setSmallIcon(R$drawable.ic_info).setAutoCancel(true).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0)).build());
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String str;
        printWriter.println("NavigationModeController:");
        StringBuilder sb = new StringBuilder();
        sb.append("  mode=");
        sb.append(this.mMode);
        printWriter.println(sb.toString());
        try {
            str = String.join(", ", this.mOverlayManager.getDefaultOverlayPackages());
        } catch (RemoteException unused) {
            str = "failed_to_fetch";
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("  defaultOverlays=");
        sb2.append(str);
        printWriter.println(sb2.toString());
        dumpAssetPaths(this.mCurrentUserContext);
        StringBuilder sb3 = new StringBuilder();
        sb3.append("  defaultLauncher=");
        sb3.append(this.mLastDefaultLauncher);
        printWriter.println(sb3.toString());
        boolean z = this.mCurrentUserContext.getSharedPreferences("navigation_mode_controller_preferences", 0).getBoolean("switched_from_gesture_nav", false);
        StringBuilder sb4 = new StringBuilder();
        sb4.append("  previouslySwitchedFromGestureNav=");
        sb4.append(z);
        printWriter.println(sb4.toString());
    }

    private void dumpAssetPaths(Context context) {
        ApkAssets[] apkAssets;
        Log.d(TAG, "assetPaths=");
        for (ApkAssets apkAssets2 : context.getResources().getAssets().getApkAssets()) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("    ");
            sb.append(apkAssets2.getAssetPath());
            Log.d(str, sb.toString());
        }
    }
}

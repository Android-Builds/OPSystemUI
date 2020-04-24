package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.AppOpsManager.OpEntry;
import android.app.AppOpsManager.PackageOps;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.systemui.statusbar.policy.LocationController.LocationChangeCallback;
import com.android.systemui.util.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LocationControllerImpl extends BroadcastReceiver implements LocationController {
    private static final int[] mHighPowerRequestAppOpArray = {42};
    private AppOpsManager mAppOpsManager;
    /* access modifiers changed from: private */
    public boolean mAreActiveLocationRequests;
    private Context mContext;
    private final C1608H mHandler = new C1608H();
    private String mRequestedPackage;
    /* access modifiers changed from: private */
    public ArrayList<LocationChangeCallback> mSettingsChangeCallbacks = new ArrayList<>();
    private StatusBarManager mStatusBarManager;

    /* renamed from: com.android.systemui.statusbar.policy.LocationControllerImpl$H */
    private final class C1608H extends Handler {
        private C1608H() {
        }

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                locationSettingsChanged();
            } else if (i == 2) {
                locationActiveChanged();
            } else if (i == 3) {
                addCallback((LocationChangeCallback) message.obj);
            } else if (i == 4) {
                removeCallback((LocationChangeCallback) message.obj);
            }
        }

        private void locationActiveChanged() {
            Utils.safeForeach(LocationControllerImpl.this.mSettingsChangeCallbacks, new Consumer() {
                public final void accept(Object obj) {
                    C1608H.this.lambda$locationActiveChanged$0$LocationControllerImpl$H((LocationChangeCallback) obj);
                }
            });
        }

        public /* synthetic */ void lambda$locationActiveChanged$0$LocationControllerImpl$H(LocationChangeCallback locationChangeCallback) {
            locationChangeCallback.onLocationActiveChanged(LocationControllerImpl.this.mAreActiveLocationRequests);
        }

        private void locationSettingsChanged() {
            Utils.safeForeach(LocationControllerImpl.this.mSettingsChangeCallbacks, new Consumer(LocationControllerImpl.this.isLocationEnabled()) {
                private final /* synthetic */ boolean f$0;

                {
                    this.f$0 = r1;
                }

                public final void accept(Object obj) {
                    ((LocationChangeCallback) obj).onLocationSettingsChanged(this.f$0);
                }
            });
        }

        private void addCallback(LocationChangeCallback locationChangeCallback) {
            LocationControllerImpl.this.mSettingsChangeCallbacks.add(locationChangeCallback);
        }

        private void removeCallback(LocationChangeCallback locationChangeCallback) {
            LocationControllerImpl.this.mSettingsChangeCallbacks.remove(locationChangeCallback);
        }
    }

    public LocationControllerImpl(Context context, Looper looper) {
        this.mContext = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.location.HIGH_POWER_REQUEST_CHANGE");
        intentFilter.addAction("android.location.MODE_CHANGED");
        context.registerReceiverAsUser(this, UserHandle.ALL, intentFilter, null, new Handler(looper));
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        updateActiveLocationRequests();
    }

    public void addCallback(LocationChangeCallback locationChangeCallback) {
        this.mHandler.obtainMessage(3, locationChangeCallback).sendToTarget();
        this.mHandler.sendEmptyMessage(1);
    }

    public void removeCallback(LocationChangeCallback locationChangeCallback) {
        this.mHandler.obtainMessage(4, locationChangeCallback).sendToTarget();
    }

    public boolean setLocationEnabled(boolean z) {
        int currentUser = ActivityManager.getCurrentUser();
        if (isUserLocationRestricted(currentUser)) {
            return false;
        }
        com.android.settingslib.Utils.updateLocationEnabled(this.mContext, z, currentUser, 2);
        return true;
    }

    public boolean isLocationEnabled() {
        return ((LocationManager) this.mContext.getSystemService("location")).isLocationEnabledForUser(UserHandle.of(ActivityManager.getCurrentUser()));
    }

    public boolean isLocationActive() {
        return this.mAreActiveLocationRequests;
    }

    private boolean isUserLocationRestricted(int i) {
        return ((UserManager) this.mContext.getSystemService("user")).hasUserRestriction("no_share_location", UserHandle.of(i));
    }

    /* access modifiers changed from: protected */
    public boolean areActiveHighPowerLocationRequests() {
        List packagesForOps = this.mAppOpsManager.getPackagesForOps(mHighPowerRequestAppOpArray);
        if (packagesForOps != null) {
            int size = packagesForOps.size();
            for (int i = 0; i < size; i++) {
                PackageOps packageOps = (PackageOps) packagesForOps.get(i);
                List ops = packageOps.getOps();
                if (ops != null) {
                    int size2 = ops.size();
                    int i2 = 0;
                    while (i2 < size2) {
                        OpEntry opEntry = (OpEntry) ops.get(i2);
                        if (opEntry.getOp() != 42 || !opEntry.isRunning()) {
                            i2++;
                        } else {
                            this.mRequestedPackage = packageOps.getPackageName();
                            return true;
                        }
                    }
                    continue;
                }
            }
        }
        return false;
    }

    private void updateActiveLocationRequests() {
        boolean z = this.mAreActiveLocationRequests;
        this.mAreActiveLocationRequests = areActiveHighPowerLocationRequests();
        if (this.mAreActiveLocationRequests != z) {
            StringBuilder sb = new StringBuilder();
            sb.append("updateActiveLocationRequests, high-power location is requested by pkg: ");
            sb.append(this.mRequestedPackage);
            sb.append(", active: ");
            sb.append(this.mAreActiveLocationRequests);
            Log.d("LocationControllerImpl", sb.toString());
            this.mHandler.sendEmptyMessage(2);
        }
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.location.HIGH_POWER_REQUEST_CHANGE".equals(action)) {
            updateActiveLocationRequests();
        } else if ("android.location.MODE_CHANGED".equals(action)) {
            this.mHandler.sendEmptyMessage(1);
        }
    }
}

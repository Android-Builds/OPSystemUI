package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.IConnectivityManager;
import android.net.IConnectivityManager.Stub;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.systemui.R$string;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.SecurityController.SecurityControllerCallback;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class SecurityControllerImpl extends CurrentUserTracker implements SecurityController {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("SecurityController", 3);
    private static final NetworkRequest REQUEST = new Builder().removeCapability(15).removeCapability(13).removeCapability(14).setUids(null).build();
    /* access modifiers changed from: private */
    public final Handler mBgHandler;
    private final BroadcastReceiver mBroadcastReceiver;
    @GuardedBy({"mCallbacks"})
    private final ArrayList<SecurityControllerCallback> mCallbacks;
    private final ConnectivityManager mConnectivityManager;
    private final IConnectivityManager mConnectivityManagerService;
    /* access modifiers changed from: private */
    public final Context mContext;
    private int mCurrentUserId;
    private SparseArray<VpnConfig> mCurrentVpns;
    private final DevicePolicyManager mDevicePolicyManager;
    /* access modifiers changed from: private */
    public ArrayMap<Integer, Boolean> mHasCACerts;
    private final NetworkCallback mNetworkCallback;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private int mVpnUserId;

    protected class CACertLoader extends AsyncTask<Integer, Void, Pair<Integer, Boolean>> {
        protected CACertLoader() {
        }

        /* access modifiers changed from: protected */
        /* JADX WARNING: Code restructure failed: missing block: B:16:0x003d, code lost:
            r3 = move-exception;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:17:0x003e, code lost:
            if (r1 != null) goto L_0x0040;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:19:?, code lost:
            r1.close();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:23:0x0048, code lost:
            throw r3;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public android.util.Pair<java.lang.Integer, java.lang.Boolean> doInBackground(java.lang.Integer... r6) {
            /*
                r5 = this;
                r0 = 0
                com.android.systemui.statusbar.policy.SecurityControllerImpl r1 = com.android.systemui.statusbar.policy.SecurityControllerImpl.this     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0049 }
                android.content.Context r1 = r1.mContext     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0049 }
                r2 = r6[r0]     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0049 }
                int r2 = r2.intValue()     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0049 }
                android.os.UserHandle r2 = android.os.UserHandle.of(r2)     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0049 }
                android.security.KeyChain$KeyChainConnection r1 = android.security.KeyChain.bindAsUser(r1, r2)     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0049 }
                android.security.IKeyChainService r2 = r1.getService()     // Catch:{ all -> 0x003b }
                android.content.pm.StringParceledListSlice r2 = r2.getUserCaAliases()     // Catch:{ all -> 0x003b }
                java.util.List r2 = r2.getList()     // Catch:{ all -> 0x003b }
                boolean r2 = r2.isEmpty()     // Catch:{ all -> 0x003b }
                if (r2 != 0) goto L_0x0029
                r2 = 1
                goto L_0x002a
            L_0x0029:
                r2 = r0
            L_0x002a:
                android.util.Pair r3 = new android.util.Pair     // Catch:{ all -> 0x003b }
                r4 = r6[r0]     // Catch:{ all -> 0x003b }
                java.lang.Boolean r2 = java.lang.Boolean.valueOf(r2)     // Catch:{ all -> 0x003b }
                r3.<init>(r4, r2)     // Catch:{ all -> 0x003b }
                if (r1 == 0) goto L_0x003a
                r1.close()     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0049 }
            L_0x003a:
                return r3
            L_0x003b:
                r2 = move-exception
                throw r2     // Catch:{ all -> 0x003d }
            L_0x003d:
                r3 = move-exception
                if (r1 == 0) goto L_0x0048
                r1.close()     // Catch:{ all -> 0x0044 }
                goto L_0x0048
            L_0x0044:
                r1 = move-exception
                r2.addSuppressed(r1)     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0049 }
            L_0x0048:
                throw r3     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0049 }
            L_0x0049:
                r1 = move-exception
                java.lang.String r2 = "SecurityController"
                java.lang.String r3 = "failed to get CA certs"
                android.util.Log.i(r2, r3, r1)
                com.android.systemui.statusbar.policy.SecurityControllerImpl r1 = com.android.systemui.statusbar.policy.SecurityControllerImpl.this
                android.os.Handler r1 = r1.mBgHandler
                com.android.systemui.statusbar.policy.-$$Lambda$SecurityControllerImpl$CACertLoader$xO5ELH-ynhsu1kwnRVzV4aHRUJ0 r2 = new com.android.systemui.statusbar.policy.-$$Lambda$SecurityControllerImpl$CACertLoader$xO5ELH-ynhsu1kwnRVzV4aHRUJ0
                r2.<init>(r6)
                r3 = 30000(0x7530, double:1.4822E-319)
                r1.postDelayed(r2, r3)
                android.util.Pair r5 = new android.util.Pair
                r6 = r6[r0]
                r0 = 0
                r5.<init>(r6, r0)
                return r5
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.SecurityControllerImpl.CACertLoader.doInBackground(java.lang.Integer[]):android.util.Pair");
        }

        public /* synthetic */ void lambda$doInBackground$0$SecurityControllerImpl$CACertLoader(Integer[] numArr) {
            new CACertLoader().execute(new Integer[]{numArr[0]});
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Pair<Integer, Boolean> pair) {
            if (SecurityControllerImpl.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("onPostExecute ");
                sb.append(pair);
                Log.d("SecurityController", sb.toString());
            }
            if (pair.second != null) {
                SecurityControllerImpl.this.mHasCACerts.put((Integer) pair.first, (Boolean) pair.second);
                SecurityControllerImpl.this.fireCallbacks();
            }
        }
    }

    public SecurityControllerImpl(Context context, Handler handler) {
        this(context, handler, null);
    }

    public SecurityControllerImpl(Context context, Handler handler, SecurityControllerCallback securityControllerCallback) {
        super(context);
        this.mCallbacks = new ArrayList<>();
        this.mCurrentVpns = new SparseArray<>();
        this.mHasCACerts = new ArrayMap<>();
        this.mNetworkCallback = new NetworkCallback() {
            public void onAvailable(Network network) {
                if (SecurityControllerImpl.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onAvailable ");
                    sb.append(network.netId);
                    Log.d("SecurityController", sb.toString());
                }
                SecurityControllerImpl.this.updateState();
                SecurityControllerImpl.this.fireCallbacks();
            }

            public void onLost(Network network) {
                if (SecurityControllerImpl.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onLost ");
                    sb.append(network.netId);
                    Log.d("SecurityController", sb.toString());
                }
                SecurityControllerImpl.this.updateState();
                SecurityControllerImpl.this.fireCallbacks();
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.security.action.TRUST_STORE_CHANGED".equals(intent.getAction())) {
                    SecurityControllerImpl.this.refreshCACerts();
                }
            }
        };
        this.mContext = context;
        this.mBgHandler = handler;
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        String str = "connectivity";
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService(str);
        this.mConnectivityManagerService = Stub.asInterface(ServiceManager.getService(str));
        this.mPackageManager = context.getPackageManager();
        this.mUserManager = (UserManager) context.getSystemService("user");
        addCallback(securityControllerCallback);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.security.action.TRUST_STORE_CHANGED");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, handler);
        this.mConnectivityManager.registerNetworkCallback(REQUEST, this.mNetworkCallback);
        onUserSwitched(ActivityManager.getCurrentUser());
        startTracking();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("SecurityController state:");
        printWriter.print("  mCurrentVpns={");
        for (int i = 0; i < this.mCurrentVpns.size(); i++) {
            if (i > 0) {
                printWriter.print(", ");
            }
            printWriter.print(this.mCurrentVpns.keyAt(i));
            printWriter.print('=');
            printWriter.print(((VpnConfig) this.mCurrentVpns.valueAt(i)).user);
        }
        printWriter.println("}");
    }

    public boolean isDeviceManaged() {
        return this.mDevicePolicyManager.isDeviceManaged();
    }

    public CharSequence getDeviceOwnerOrganizationName() {
        return this.mDevicePolicyManager.getDeviceOwnerOrganizationName();
    }

    public CharSequence getWorkProfileOrganizationName() {
        int workProfileUserId = getWorkProfileUserId(this.mCurrentUserId);
        if (workProfileUserId == -10000) {
            return null;
        }
        return this.mDevicePolicyManager.getOrganizationNameForUser(workProfileUserId);
    }

    public String getPrimaryVpnName() {
        VpnConfig vpnConfig = (VpnConfig) this.mCurrentVpns.get(this.mVpnUserId);
        if (vpnConfig != null) {
            return getNameForVpnConfig(vpnConfig, new UserHandle(this.mVpnUserId));
        }
        return null;
    }

    private int getWorkProfileUserId(int i) {
        for (UserInfo userInfo : this.mUserManager.getProfiles(i)) {
            if (userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        return -10000;
    }

    public boolean hasWorkProfile() {
        return getWorkProfileUserId(this.mCurrentUserId) != -10000;
    }

    public String getWorkProfileVpnName() {
        int workProfileUserId = getWorkProfileUserId(this.mVpnUserId);
        if (workProfileUserId == -10000) {
            return null;
        }
        VpnConfig vpnConfig = (VpnConfig) this.mCurrentVpns.get(workProfileUserId);
        if (vpnConfig != null) {
            return getNameForVpnConfig(vpnConfig, UserHandle.of(workProfileUserId));
        }
        return null;
    }

    public boolean isNetworkLoggingEnabled() {
        return this.mDevicePolicyManager.isNetworkLoggingEnabled(null);
    }

    public boolean isVpnEnabled() {
        for (int i : this.mUserManager.getProfileIdsWithDisabled(this.mVpnUserId)) {
            if (this.mCurrentVpns.get(i) != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isVpnBranded() {
        VpnConfig vpnConfig = (VpnConfig) this.mCurrentVpns.get(this.mVpnUserId);
        if (vpnConfig == null) {
            return false;
        }
        String packageNameForVpnConfig = getPackageNameForVpnConfig(vpnConfig);
        if (packageNameForVpnConfig == null) {
            return false;
        }
        return isVpnPackageBranded(packageNameForVpnConfig);
    }

    public boolean hasCACertInCurrentUser() {
        Boolean bool = (Boolean) this.mHasCACerts.get(Integer.valueOf(this.mCurrentUserId));
        return bool != null && bool.booleanValue();
    }

    public boolean hasCACertInWorkProfile() {
        int workProfileUserId = getWorkProfileUserId(this.mCurrentUserId);
        boolean z = false;
        if (workProfileUserId == -10000) {
            return false;
        }
        Boolean bool = (Boolean) this.mHasCACerts.get(Integer.valueOf(workProfileUserId));
        if (bool != null && bool.booleanValue()) {
            z = true;
        }
        return z;
    }

    public void removeCallback(SecurityControllerCallback securityControllerCallback) {
        synchronized (this.mCallbacks) {
            if (securityControllerCallback != null) {
                if (DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("removeCallback ");
                    sb.append(securityControllerCallback);
                    Log.d("SecurityController", sb.toString());
                }
                this.mCallbacks.remove(securityControllerCallback);
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0030, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addCallback(com.android.systemui.statusbar.policy.SecurityController.SecurityControllerCallback r5) {
        /*
            r4 = this;
            java.util.ArrayList<com.android.systemui.statusbar.policy.SecurityController$SecurityControllerCallback> r0 = r4.mCallbacks
            monitor-enter(r0)
            if (r5 == 0) goto L_0x002f
            java.util.ArrayList<com.android.systemui.statusbar.policy.SecurityController$SecurityControllerCallback> r1 = r4.mCallbacks     // Catch:{ all -> 0x0031 }
            boolean r1 = r1.contains(r5)     // Catch:{ all -> 0x0031 }
            if (r1 == 0) goto L_0x000e
            goto L_0x002f
        L_0x000e:
            boolean r1 = DEBUG     // Catch:{ all -> 0x0031 }
            if (r1 == 0) goto L_0x0028
            java.lang.String r1 = "SecurityController"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x0031 }
            r2.<init>()     // Catch:{ all -> 0x0031 }
            java.lang.String r3 = "addCallback "
            r2.append(r3)     // Catch:{ all -> 0x0031 }
            r2.append(r5)     // Catch:{ all -> 0x0031 }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x0031 }
            android.util.Log.d(r1, r2)     // Catch:{ all -> 0x0031 }
        L_0x0028:
            java.util.ArrayList<com.android.systemui.statusbar.policy.SecurityController$SecurityControllerCallback> r4 = r4.mCallbacks     // Catch:{ all -> 0x0031 }
            r4.add(r5)     // Catch:{ all -> 0x0031 }
            monitor-exit(r0)     // Catch:{ all -> 0x0031 }
            return
        L_0x002f:
            monitor-exit(r0)     // Catch:{ all -> 0x0031 }
            return
        L_0x0031:
            r4 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0031 }
            throw r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.SecurityControllerImpl.addCallback(com.android.systemui.statusbar.policy.SecurityController$SecurityControllerCallback):void");
    }

    public void onUserSwitched(int i) {
        this.mCurrentUserId = i;
        UserInfo userInfo = this.mUserManager.getUserInfo(i);
        if (userInfo.isRestricted()) {
            this.mVpnUserId = userInfo.restrictedProfileParentId;
        } else {
            this.mVpnUserId = this.mCurrentUserId;
        }
        refreshCACerts();
        fireCallbacks();
    }

    /* access modifiers changed from: private */
    public void refreshCACerts() {
        new CACertLoader().execute(new Integer[]{Integer.valueOf(this.mCurrentUserId)});
        int workProfileUserId = getWorkProfileUserId(this.mCurrentUserId);
        if (workProfileUserId != -10000) {
            new CACertLoader().execute(new Integer[]{Integer.valueOf(workProfileUserId)});
        }
    }

    private String getNameForVpnConfig(VpnConfig vpnConfig, UserHandle userHandle) {
        if (vpnConfig.legacy) {
            return this.mContext.getString(R$string.legacy_vpn_name);
        }
        String str = vpnConfig.user;
        try {
            return VpnConfig.getVpnLabel(this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, userHandle), str).toString();
        } catch (NameNotFoundException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Package ");
            sb.append(str);
            sb.append(" is not present");
            Log.e("SecurityController", sb.toString(), e);
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void fireCallbacks() {
        synchronized (this.mCallbacks) {
            Iterator it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                ((SecurityControllerCallback) it.next()).onStateChanged();
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateState() {
        SparseArray<VpnConfig> sparseArray = new SparseArray<>();
        try {
            for (UserInfo userInfo : this.mUserManager.getUsers()) {
                VpnConfig vpnConfig = this.mConnectivityManagerService.getVpnConfig(userInfo.id);
                if (vpnConfig != null) {
                    if (vpnConfig.legacy) {
                        LegacyVpnInfo legacyVpnInfo = this.mConnectivityManagerService.getLegacyVpnInfo(userInfo.id);
                        if (legacyVpnInfo != null) {
                            if (legacyVpnInfo.state != 3) {
                            }
                        }
                    }
                    sparseArray.put(userInfo.id, vpnConfig);
                }
            }
            this.mCurrentVpns = sparseArray;
        } catch (RemoteException e) {
            Log.e("SecurityController", "Unable to list active VPNs", e);
        }
    }

    private String getPackageNameForVpnConfig(VpnConfig vpnConfig) {
        if (vpnConfig.legacy) {
            return null;
        }
        return vpnConfig.user;
    }

    private boolean isVpnPackageBranded(String str) {
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(str, 128);
            if (!(applicationInfo == null || applicationInfo.metaData == null)) {
                if (applicationInfo.isSystemApp()) {
                    return applicationInfo.metaData.getBoolean("com.android.systemui.IS_BRANDED", false);
                }
            }
        } catch (NameNotFoundException unused) {
        }
        return false;
    }
}

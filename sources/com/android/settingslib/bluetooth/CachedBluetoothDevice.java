package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CachedBluetoothDevice implements Comparable<CachedBluetoothDevice> {
    private final Collection<Callback> mCallbacks = new ArrayList();
    private long mConnectAttempted;
    private final Context mContext;
    BluetoothDevice mDevice;
    private long mHiSyncId;
    private boolean mIsActiveDeviceA2dp = false;
    private boolean mIsActiveDeviceHeadset = false;
    private boolean mIsActiveDeviceHearingAid = false;
    boolean mJustDiscovered;
    private final BluetoothAdapter mLocalAdapter;
    private boolean mLocalNapRoleConnected;
    private final Object mProfileLock = new Object();
    private final LocalBluetoothProfileManager mProfileManager;
    private final List<LocalBluetoothProfile> mProfiles = new ArrayList();
    private final List<LocalBluetoothProfile> mRemovedProfiles = new ArrayList();
    short mRssi;
    private CachedBluetoothDevice mSubDevice;
    public int mTwspBatteryLevel;
    public int mTwspBatteryState;

    public interface Callback {
        void onDeviceAttributesChanged();
    }

    CachedBluetoothDevice(Context context, LocalBluetoothProfileManager localBluetoothProfileManager, BluetoothDevice bluetoothDevice) {
        this.mContext = context;
        this.mLocalAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mProfileManager = localBluetoothProfileManager;
        this.mDevice = bluetoothDevice;
        fillData();
        this.mHiSyncId = 0;
        this.mTwspBatteryState = -1;
        this.mTwspBatteryLevel = -1;
    }

    private BluetoothDevice getTwsPeerDevice() {
        if (this.mDevice.isTwsPlusDevice()) {
            return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(this.mDevice.getTwsPlusPeerAddress());
        }
        return null;
    }

    private String describe(LocalBluetoothProfile localBluetoothProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Address:");
        sb.append(this.mDevice);
        if (localBluetoothProfile != null) {
            sb.append(" Profile:");
            sb.append(localBluetoothProfile);
        }
        return sb.toString();
    }

    /* access modifiers changed from: 0000 */
    public void onProfileStateChanged(LocalBluetoothProfile localBluetoothProfile, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("onProfileStateChanged: profile ");
        sb.append(localBluetoothProfile);
        sb.append(", device=");
        sb.append(this.mDevice);
        sb.append(", newProfileState ");
        sb.append(i);
        Log.d("CachedBluetoothDevice", sb.toString());
        if (this.mLocalAdapter.getState() == 13) {
            Log.d("CachedBluetoothDevice", " BT Turninig Off...Profile conn state change ignored...");
            return;
        }
        synchronized (this.mProfileLock) {
            if (i == 2) {
                if (localBluetoothProfile instanceof MapProfile) {
                    localBluetoothProfile.setPreferred(this.mDevice, true);
                }
                if (!this.mProfiles.contains(localBluetoothProfile)) {
                    this.mRemovedProfiles.remove(localBluetoothProfile);
                    this.mProfiles.add(localBluetoothProfile);
                    if ((localBluetoothProfile instanceof PanProfile) && ((PanProfile) localBluetoothProfile).isLocalRoleNap(this.mDevice)) {
                        this.mLocalNapRoleConnected = true;
                    }
                }
            } else if ((localBluetoothProfile instanceof MapProfile) && i == 0) {
                localBluetoothProfile.setPreferred(this.mDevice, false);
            } else if (this.mLocalNapRoleConnected && (localBluetoothProfile instanceof PanProfile) && ((PanProfile) localBluetoothProfile).isLocalRoleNap(this.mDevice) && i == 0) {
                Log.d("CachedBluetoothDevice", "Removing PanProfile from device after NAP disconnect");
                this.mProfiles.remove(localBluetoothProfile);
                this.mRemovedProfiles.add(localBluetoothProfile);
                this.mLocalNapRoleConnected = false;
            } else if ((localBluetoothProfile instanceof HeadsetProfile) && i == 0) {
                this.mTwspBatteryState = -1;
                this.mTwspBatteryLevel = -1;
            }
        }
        fetchActiveDevices();
    }

    public void disconnect() {
        synchronized (this.mProfileLock) {
            for (LocalBluetoothProfile disconnect : this.mProfiles) {
                disconnect(disconnect);
            }
        }
        PbapServerProfile pbapProfile = this.mProfileManager.getPbapProfile();
        if (pbapProfile != null && isConnectedProfile(pbapProfile)) {
            pbapProfile.disconnect(this.mDevice);
        }
    }

    public void disconnect(LocalBluetoothProfile localBluetoothProfile) {
        if (localBluetoothProfile.disconnect(this.mDevice)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Command sent successfully:DISCONNECT ");
            sb.append(describe(localBluetoothProfile));
            Log.d("CachedBluetoothDevice", sb.toString());
        }
    }

    public void connect(boolean z) {
        if (ensurePaired()) {
            this.mConnectAttempted = SystemClock.elapsedRealtime();
            connectWithoutResettingTimer(z);
        }
    }

    public long getHiSyncId() {
        return this.mHiSyncId;
    }

    public void setHiSyncId(long j) {
        StringBuilder sb = new StringBuilder();
        sb.append("setHiSyncId: mDevice ");
        sb.append(this.mDevice);
        sb.append(", id ");
        sb.append(j);
        Log.d("CachedBluetoothDevice", sb.toString());
        this.mHiSyncId = j;
    }

    public boolean isHearingAidDevice() {
        return this.mHiSyncId != 0;
    }

    /* access modifiers changed from: 0000 */
    public void onBondingDockConnect() {
        connect(false);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0071, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void connectWithoutResettingTimer(boolean r6) {
        /*
            r5 = this;
            java.lang.Object r0 = r5.mProfileLock
            monitor-enter(r0)
            java.util.List<com.android.settingslib.bluetooth.LocalBluetoothProfile> r1 = r5.mProfiles     // Catch:{ all -> 0x0072 }
            boolean r1 = r1.isEmpty()     // Catch:{ all -> 0x0072 }
            if (r1 == 0) goto L_0x0025
            java.lang.String r6 = "CachedBluetoothDevice"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch:{ all -> 0x0072 }
            r1.<init>()     // Catch:{ all -> 0x0072 }
            java.lang.String r2 = "No profiles. Maybe we will connect later for device "
            r1.append(r2)     // Catch:{ all -> 0x0072 }
            android.bluetooth.BluetoothDevice r5 = r5.mDevice     // Catch:{ all -> 0x0072 }
            r1.append(r5)     // Catch:{ all -> 0x0072 }
            java.lang.String r5 = r1.toString()     // Catch:{ all -> 0x0072 }
            android.util.Log.d(r6, r5)     // Catch:{ all -> 0x0072 }
            monitor-exit(r0)     // Catch:{ all -> 0x0072 }
            return
        L_0x0025:
            r1 = 0
            java.util.List<com.android.settingslib.bluetooth.LocalBluetoothProfile> r2 = r5.mProfiles     // Catch:{ all -> 0x0072 }
            java.util.Iterator r2 = r2.iterator()     // Catch:{ all -> 0x0072 }
        L_0x002c:
            boolean r3 = r2.hasNext()     // Catch:{ all -> 0x0072 }
            if (r3 == 0) goto L_0x0055
            java.lang.Object r3 = r2.next()     // Catch:{ all -> 0x0072 }
            com.android.settingslib.bluetooth.LocalBluetoothProfile r3 = (com.android.settingslib.bluetooth.LocalBluetoothProfile) r3     // Catch:{ all -> 0x0072 }
            if (r6 == 0) goto L_0x0041
            boolean r4 = r3.accessProfileEnabled()     // Catch:{ all -> 0x0072 }
            if (r4 == 0) goto L_0x002c
            goto L_0x0047
        L_0x0041:
            boolean r4 = r3.isAutoConnectable()     // Catch:{ all -> 0x0072 }
            if (r4 == 0) goto L_0x002c
        L_0x0047:
            android.bluetooth.BluetoothDevice r4 = r5.mDevice     // Catch:{ all -> 0x0072 }
            boolean r4 = r3.isPreferred(r4)     // Catch:{ all -> 0x0072 }
            if (r4 == 0) goto L_0x002c
            int r1 = r1 + 1
            r5.connectInt(r3)     // Catch:{ all -> 0x0072 }
            goto L_0x002c
        L_0x0055:
            java.lang.String r6 = "CachedBluetoothDevice"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x0072 }
            r2.<init>()     // Catch:{ all -> 0x0072 }
            java.lang.String r3 = "Preferred profiles = "
            r2.append(r3)     // Catch:{ all -> 0x0072 }
            r2.append(r1)     // Catch:{ all -> 0x0072 }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x0072 }
            android.util.Log.d(r6, r2)     // Catch:{ all -> 0x0072 }
            if (r1 != 0) goto L_0x0070
            r5.connectAutoConnectableProfiles()     // Catch:{ all -> 0x0072 }
        L_0x0070:
            monitor-exit(r0)     // Catch:{ all -> 0x0072 }
            return
        L_0x0072:
            r5 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0072 }
            throw r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.settingslib.bluetooth.CachedBluetoothDevice.connectWithoutResettingTimer(boolean):void");
    }

    private void connectAutoConnectableProfiles() {
        if (ensurePaired()) {
            synchronized (this.mProfileLock) {
                for (LocalBluetoothProfile localBluetoothProfile : this.mProfiles) {
                    if (localBluetoothProfile.isAutoConnectable()) {
                        localBluetoothProfile.setPreferred(this.mDevice, true);
                        connectInt(localBluetoothProfile);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public synchronized void connectInt(LocalBluetoothProfile localBluetoothProfile) {
        if (ensurePaired()) {
            if (localBluetoothProfile.connect(this.mDevice)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Command sent successfully:CONNECT ");
                sb.append(describe(localBluetoothProfile));
                Log.d("CachedBluetoothDevice", sb.toString());
                return;
            }
            String str = "CachedBluetoothDevice";
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Failed to connect ");
            sb2.append(localBluetoothProfile.toString());
            sb2.append(" to ");
            sb2.append(getName());
            Log.i(str, sb2.toString());
        }
    }

    private boolean ensurePaired() {
        if (getBondState() != 10) {
            return true;
        }
        startPairing();
        return false;
    }

    public boolean startPairing() {
        if (this.mLocalAdapter.isDiscovering()) {
            this.mLocalAdapter.cancelDiscovery();
        }
        return this.mDevice.createBond();
    }

    public void unpair() {
        int bondState = getBondState();
        if (bondState == 11) {
            this.mDevice.cancelBondProcess();
        }
        if (bondState != 10) {
            BluetoothDevice bluetoothDevice = this.mDevice;
            String str = "Command sent successfully:REMOVE_BOND ";
            String str2 = "CachedBluetoothDevice";
            if (bluetoothDevice.isTwsPlusDevice()) {
                BluetoothDevice twsPeerDevice = getTwsPeerDevice();
                if (twsPeerDevice != null && twsPeerDevice.removeBond()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(str);
                    sb.append(twsPeerDevice.getName());
                    Log.d(str2, sb.toString());
                }
            }
            if (bluetoothDevice != null && bluetoothDevice.removeBond()) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(str);
                sb2.append(describe(null));
                Log.d(str2, sb2.toString());
            }
        }
    }

    public int getProfileConnectionState(LocalBluetoothProfile localBluetoothProfile) {
        if (localBluetoothProfile != null) {
            return localBluetoothProfile.getConnectionStatus(this.mDevice);
        }
        return 0;
    }

    private void fillData() {
        updateProfiles();
        fetchActiveDevices();
        migratePhonebookPermissionChoice();
        migrateMessagePermissionChoice();
        dispatchAttributesChanged();
    }

    public BluetoothDevice getDevice() {
        return this.mDevice;
    }

    public String getAddress() {
        return this.mDevice.getAddress();
    }

    public String getName() {
        String aliasName = this.mDevice.getAliasName();
        return TextUtils.isEmpty(aliasName) ? getAddress() : aliasName;
    }

    public boolean setActive() {
        boolean z;
        A2dpProfile a2dpProfile = this.mProfileManager.getA2dpProfile();
        String str = "CachedBluetoothDevice";
        if (a2dpProfile == null || !isConnectedProfile(a2dpProfile) || !a2dpProfile.setActiveDevice(getDevice())) {
            z = false;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("OnPreferenceClickListener: A2DP active device=");
            sb.append(this);
            Log.i(str, sb.toString());
            z = true;
        }
        HeadsetProfile headsetProfile = this.mProfileManager.getHeadsetProfile();
        if (headsetProfile != null && isConnectedProfile(headsetProfile) && headsetProfile.setActiveDevice(getDevice())) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("OnPreferenceClickListener: Headset active device=");
            sb2.append(this);
            Log.i(str, sb2.toString());
            z = true;
        }
        HearingAidProfile hearingAidProfile = this.mProfileManager.getHearingAidProfile();
        if (hearingAidProfile == null || !isConnectedProfile(hearingAidProfile) || !hearingAidProfile.setActiveDevice(getDevice())) {
            return z;
        }
        StringBuilder sb3 = new StringBuilder();
        sb3.append("OnPreferenceClickListener: Hearing Aid active device=");
        sb3.append(this);
        Log.i(str, sb3.toString());
        return true;
    }

    /* access modifiers changed from: 0000 */
    public void refreshName() {
        StringBuilder sb = new StringBuilder();
        sb.append("Device name: ");
        sb.append(getName());
        Log.d("CachedBluetoothDevice", sb.toString());
        dispatchAttributesChanged();
    }

    public int getBatteryLevel() {
        return this.mDevice.getBatteryLevel();
    }

    /* access modifiers changed from: 0000 */
    public void refresh() {
        dispatchAttributesChanged();
    }

    public void setJustDiscovered(boolean z) {
        if (this.mJustDiscovered != z) {
            this.mJustDiscovered = z;
            dispatchAttributesChanged();
        }
    }

    public int getBondState() {
        StringBuilder sb = new StringBuilder();
        sb.append("device name : ");
        sb.append(this.mDevice.getName());
        sb.append(" bond state : ");
        sb.append(this.mDevice.getBondState());
        Log.d("CachedBluetoothDevice", sb.toString());
        return this.mDevice.getBondState();
    }

    public void onActiveDeviceChanged(boolean z, int i) {
        boolean z2 = false;
        if (i == 1) {
            if (this.mIsActiveDeviceHeadset != z) {
                z2 = true;
            }
            this.mIsActiveDeviceHeadset = z;
        } else if (i == 2) {
            if (this.mIsActiveDeviceA2dp != z) {
                z2 = true;
            }
            this.mIsActiveDeviceA2dp = z;
        } else if (i != 21) {
            StringBuilder sb = new StringBuilder();
            sb.append("onActiveDeviceChanged: unknown profile ");
            sb.append(i);
            sb.append(" isActive ");
            sb.append(z);
            Log.w("CachedBluetoothDevice", sb.toString());
        } else {
            if (this.mIsActiveDeviceHearingAid != z) {
                z2 = true;
            }
            this.mIsActiveDeviceHearingAid = z;
        }
        if (z2) {
            dispatchAttributesChanged();
        }
    }

    /* access modifiers changed from: 0000 */
    public void onAudioModeChanged() {
        dispatchAttributesChanged();
    }

    public boolean isActiveDevice(int i) {
        if (i == 1) {
            return this.mIsActiveDeviceHeadset;
        }
        if (i == 2) {
            return this.mIsActiveDeviceA2dp;
        }
        if (i == 21) {
            return this.mIsActiveDeviceHearingAid;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("getActiveDevice: unknown profile ");
        sb.append(i);
        Log.w("CachedBluetoothDevice", sb.toString());
        return false;
    }

    /* access modifiers changed from: 0000 */
    public void setRssi(short s) {
        if (this.mRssi != s) {
            this.mRssi = s;
            dispatchAttributesChanged();
        }
    }

    public boolean isConnected() {
        synchronized (this.mProfileLock) {
            for (LocalBluetoothProfile profileConnectionState : this.mProfiles) {
                if (getProfileConnectionState(profileConnectionState) == 2) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isConnectedProfile(LocalBluetoothProfile localBluetoothProfile) {
        return getProfileConnectionState(localBluetoothProfile) == 2;
    }

    private boolean updateProfiles() {
        ParcelUuid[] uuids = this.mDevice.getUuids();
        if (uuids == null) {
            return false;
        }
        ParcelUuid[] uuids2 = this.mLocalAdapter.getUuids();
        if (uuids2 == null) {
            return false;
        }
        processPhonebookAccess();
        synchronized (this.mProfileLock) {
            this.mProfileManager.updateProfiles(uuids, uuids2, this.mProfiles, this.mRemovedProfiles, this.mLocalNapRoleConnected, this.mDevice);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("updating profiles for ");
        sb.append(this.mDevice.getAliasName());
        sb.append(", ");
        sb.append(this.mDevice);
        Log.e("CachedBluetoothDevice", sb.toString());
        BluetoothClass bluetoothClass = this.mDevice.getBluetoothClass();
        if (bluetoothClass != null) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Class: ");
            sb2.append(bluetoothClass.toString());
            Log.v("CachedBluetoothDevice", sb2.toString());
        }
        Log.v("CachedBluetoothDevice", "UUID:");
        for (ParcelUuid parcelUuid : uuids) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("  ");
            sb3.append(parcelUuid);
            Log.v("CachedBluetoothDevice", sb3.toString());
        }
        return true;
    }

    private void fetchActiveDevices() {
        A2dpProfile a2dpProfile = this.mProfileManager.getA2dpProfile();
        if (a2dpProfile != null) {
            this.mIsActiveDeviceA2dp = this.mDevice.equals(a2dpProfile.getActiveDevice());
        }
        HeadsetProfile headsetProfile = this.mProfileManager.getHeadsetProfile();
        if (headsetProfile != null) {
            this.mIsActiveDeviceHeadset = this.mDevice.equals(headsetProfile.getActiveDevice());
        }
        HearingAidProfile hearingAidProfile = this.mProfileManager.getHearingAidProfile();
        if (hearingAidProfile != null) {
            this.mIsActiveDeviceHearingAid = hearingAidProfile.getActiveDevices().contains(this.mDevice);
        }
    }

    /* access modifiers changed from: 0000 */
    public void onUuidChanged() {
        updateProfiles();
        ParcelUuid[] uuids = this.mDevice.getUuids();
        long j = BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Hogp) ? 30000 : BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HearingAid) ? 15000 : 5000;
        StringBuilder sb = new StringBuilder();
        sb.append("onUuidChanged: Time since last connect=");
        sb.append(SystemClock.elapsedRealtime() - this.mConnectAttempted);
        Log.d("CachedBluetoothDevice", sb.toString());
        if (!this.mProfiles.isEmpty() && this.mConnectAttempted + j > SystemClock.elapsedRealtime()) {
            connectWithoutResettingTimer(false);
        }
        dispatchAttributesChanged();
    }

    /* access modifiers changed from: 0000 */
    public void onBondingStateChanged(int i) {
        if (i == 10) {
            synchronized (this.mProfileLock) {
                this.mProfiles.clear();
            }
            this.mDevice.setPhonebookAccessPermission(0);
            this.mDevice.setMessageAccessPermission(0);
            this.mDevice.setSimAccessPermission(0);
        }
        refresh();
        if (i == 12) {
            boolean isBondingInitiatedLocally = this.mDevice.isBondingInitiatedLocally();
            StringBuilder sb = new StringBuilder();
            sb.append("mIsBondingInitiatedLocally");
            sb.append(isBondingInitiatedLocally);
            Log.w("CachedBluetoothDevice", sb.toString());
            if (isBondingInitiatedLocally) {
                this.mDevice.setBondingInitiatedLocally(false);
            }
            if (this.mDevice.isBluetoothDock()) {
                onBondingDockConnect();
            } else if (isBondingInitiatedLocally) {
                connect(false);
            }
        }
    }

    public BluetoothClass getBtClass() {
        return this.mDevice.getBluetoothClass();
    }

    public List<LocalBluetoothProfile> getProfiles() {
        return Collections.unmodifiableList(this.mProfiles);
    }

    public void registerCallback(Callback callback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.add(callback);
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchAttributesChanged() {
        synchronized (this.mCallbacks) {
            for (Callback onDeviceAttributesChanged : this.mCallbacks) {
                onDeviceAttributesChanged.onDeviceAttributesChanged();
            }
        }
    }

    public String toString() {
        return this.mDevice.toString();
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CachedBluetoothDevice)) {
            return false;
        }
        return this.mDevice.equals(((CachedBluetoothDevice) obj).mDevice);
    }

    public int hashCode() {
        return this.mDevice.getAddress().hashCode();
    }

    public int compareTo(CachedBluetoothDevice cachedBluetoothDevice) {
        int i = (cachedBluetoothDevice.isConnected() ? 1 : 0) - (isConnected() ? 1 : 0);
        if (i != 0) {
            return i;
        }
        int i2 = 1;
        int i3 = cachedBluetoothDevice.getBondState() == 12 ? 1 : 0;
        if (getBondState() != 12) {
            i2 = 0;
        }
        int i4 = i3 - i2;
        if (i4 != 0) {
            return i4;
        }
        int i5 = (cachedBluetoothDevice.mJustDiscovered ? 1 : 0) - (this.mJustDiscovered ? 1 : 0);
        if (i5 != 0) {
            return i5;
        }
        int i6 = cachedBluetoothDevice.mRssi - this.mRssi;
        if (i6 != 0) {
            return i6;
        }
        return getName().compareTo(cachedBluetoothDevice.getName());
    }

    private void migratePhonebookPermissionChoice() {
        SharedPreferences sharedPreferences = this.mContext.getSharedPreferences("bluetooth_phonebook_permission", 0);
        if (sharedPreferences.contains(this.mDevice.getAddress())) {
            if (this.mDevice.getPhonebookAccessPermission() == 0) {
                int i = sharedPreferences.getInt(this.mDevice.getAddress(), 0);
                if (i == 1) {
                    this.mDevice.setPhonebookAccessPermission(1);
                } else if (i == 2) {
                    this.mDevice.setPhonebookAccessPermission(2);
                }
            }
            Editor edit = sharedPreferences.edit();
            edit.remove(this.mDevice.getAddress());
            edit.commit();
        }
    }

    private void migrateMessagePermissionChoice() {
        SharedPreferences sharedPreferences = this.mContext.getSharedPreferences("bluetooth_message_permission", 0);
        if (sharedPreferences.contains(this.mDevice.getAddress())) {
            if (this.mDevice.getMessageAccessPermission() == 0) {
                int i = sharedPreferences.getInt(this.mDevice.getAddress(), 0);
                if (i == 1) {
                    this.mDevice.setMessageAccessPermission(1);
                } else if (i == 2) {
                    this.mDevice.setMessageAccessPermission(2);
                }
            }
            Editor edit = sharedPreferences.edit();
            edit.remove(this.mDevice.getAddress());
            edit.commit();
        }
    }

    private void processPhonebookAccess() {
        if (this.mDevice.getBondState() == 12 && BluetoothUuid.containsAnyUuid(this.mDevice.getUuids(), PbapServerProfile.PBAB_CLIENT_UUIDS) && this.mDevice.getPhonebookAccessPermission() == 0) {
            if (this.mDevice.getBluetoothClass() != null && (this.mDevice.getBluetoothClass().getDeviceClass() == 1032 || this.mDevice.getBluetoothClass().getDeviceClass() == 1028)) {
                EventLog.writeEvent(1397638484, new Object[]{"138529441", Integer.valueOf(-1), ""});
            }
            this.mDevice.setPhonebookAccessPermission(2);
        }
    }

    public int getMaxConnectionState() {
        int i;
        synchronized (this.mProfileLock) {
            i = 0;
            for (LocalBluetoothProfile profileConnectionState : getProfiles()) {
                int profileConnectionState2 = getProfileConnectionState(profileConnectionState);
                if (profileConnectionState2 > i) {
                    i = profileConnectionState2;
                }
            }
        }
        return i;
    }

    public CachedBluetoothDevice getSubDevice() {
        return this.mSubDevice;
    }

    public void setSubDevice(CachedBluetoothDevice cachedBluetoothDevice) {
        this.mSubDevice = cachedBluetoothDevice;
    }

    public void switchSubDeviceContent() {
        BluetoothDevice bluetoothDevice = this.mDevice;
        short s = this.mRssi;
        boolean z = this.mJustDiscovered;
        CachedBluetoothDevice cachedBluetoothDevice = this.mSubDevice;
        this.mDevice = cachedBluetoothDevice.mDevice;
        this.mRssi = cachedBluetoothDevice.mRssi;
        this.mJustDiscovered = cachedBluetoothDevice.mJustDiscovered;
        cachedBluetoothDevice.mDevice = bluetoothDevice;
        cachedBluetoothDevice.mRssi = s;
        cachedBluetoothDevice.mJustDiscovered = z;
        fetchActiveDevices();
    }
}

package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.internal.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalBluetoothProfileManager {
    private A2dpProfile mA2dpProfile;
    private A2dpSinkProfile mA2dpSinkProfile;
    private final Context mContext;
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    private DunServerProfile mDunProfile;
    /* access modifiers changed from: private */
    public final BluetoothEventManager mEventManager;
    private HeadsetProfile mHeadsetProfile;
    private HearingAidProfile mHearingAidProfile;
    private HfpClientProfile mHfpClientProfile;
    private HidDeviceProfile mHidDeviceProfile;
    private HidProfile mHidProfile;
    private MapClientProfile mMapClientProfile;
    private MapProfile mMapProfile;
    private OppProfile mOppProfile;
    private PanProfile mPanProfile;
    private PbapClientProfile mPbapClientProfile;
    private PbapServerProfile mPbapProfile;
    private final Map<String, LocalBluetoothProfile> mProfileNameMap = new HashMap();
    private SapProfile mSapProfile;
    private final Collection<ServiceListener> mServiceListeners = new ArrayList();

    private class HeadsetStateChangeHandler extends StateChangedHandler {
        private final String mAudioChangeAction;
        private final int mAudioDisconnectedState;

        HeadsetStateChangeHandler(LocalBluetoothProfile localBluetoothProfile, String str, int i) {
            super(localBluetoothProfile);
            this.mAudioChangeAction = str;
            this.mAudioDisconnectedState = i;
        }

        public void onReceiveInternal(Intent intent, CachedBluetoothDevice cachedBluetoothDevice) {
            if (this.mAudioChangeAction.equals(intent.getAction())) {
                if (intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0) != this.mAudioDisconnectedState) {
                    cachedBluetoothDevice.onProfileStateChanged(this.mProfile, 2);
                }
                cachedBluetoothDevice.refresh();
                return;
            }
            super.onReceiveInternal(intent, cachedBluetoothDevice);
        }
    }

    private class PanStateChangedHandler extends StateChangedHandler {
        PanStateChangedHandler(LocalBluetoothProfile localBluetoothProfile) {
            super(localBluetoothProfile);
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            ((PanProfile) this.mProfile).setLocalRole(bluetoothDevice, intent.getIntExtra("android.bluetooth.pan.extra.LOCAL_ROLE", 0));
            super.onReceive(context, intent, bluetoothDevice);
        }
    }

    public interface ServiceListener {
        void onServiceConnected();

        void onServiceDisconnected();
    }

    private class StateChangedHandler implements Handler {
        final LocalBluetoothProfile mProfile;

        StateChangedHandler(LocalBluetoothProfile localBluetoothProfile) {
            this.mProfile = localBluetoothProfile;
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice) {
            String str = "LocalBluetoothProfileManager";
            if (bluetoothDevice == null) {
                Log.w(str, "StateChangedHandler receives state-change for invalid device");
                return;
            }
            CachedBluetoothDevice findDevice = LocalBluetoothProfileManager.this.mDeviceManager.findDevice(bluetoothDevice);
            if (findDevice == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("StateChangedHandler found new device: ");
                sb.append(bluetoothDevice);
                Log.w(str, sb.toString());
                findDevice = LocalBluetoothProfileManager.this.mDeviceManager.addDevice(bluetoothDevice);
            }
            onReceiveInternal(intent, findDevice);
        }

        /* access modifiers changed from: protected */
        public void onReceiveInternal(Intent intent, CachedBluetoothDevice cachedBluetoothDevice) {
            int intExtra = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
            int intExtra2 = intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", 0);
            if (intExtra == 0 && intExtra2 == 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("Failed to connect ");
                sb.append(this.mProfile);
                sb.append(" device");
                Log.i("LocalBluetoothProfileManager", sb.toString());
            }
            if (LocalBluetoothProfileManager.this.getHearingAidProfile() != null && (this.mProfile instanceof HearingAidProfile) && intExtra == 2 && cachedBluetoothDevice.getHiSyncId() == 0) {
                long hiSyncId = LocalBluetoothProfileManager.this.getHearingAidProfile().getHiSyncId(cachedBluetoothDevice.getDevice());
                if (hiSyncId != 0) {
                    cachedBluetoothDevice.setHiSyncId(hiSyncId);
                }
            }
            cachedBluetoothDevice.onProfileStateChanged(this.mProfile, intExtra);
            if (cachedBluetoothDevice.getHiSyncId() == 0 || !LocalBluetoothProfileManager.this.mDeviceManager.onProfileConnectionStateChangedIfProcessed(cachedBluetoothDevice, intExtra)) {
                cachedBluetoothDevice.refresh();
                LocalBluetoothProfileManager.this.mEventManager.dispatchProfileConnectionStateChanged(cachedBluetoothDevice, intExtra, this.mProfile.getProfileId());
            }
        }
    }

    LocalBluetoothProfileManager(Context context, LocalBluetoothAdapter localBluetoothAdapter, CachedBluetoothDeviceManager cachedBluetoothDeviceManager, BluetoothEventManager bluetoothEventManager) {
        this.mContext = context;
        this.mDeviceManager = cachedBluetoothDeviceManager;
        this.mEventManager = bluetoothEventManager;
        localBluetoothAdapter.setProfileManager(this);
        Log.d("LocalBluetoothProfileManager", "LocalBluetoothProfileManager construction complete");
    }

    /* access modifiers changed from: 0000 */
    public void updateLocalProfiles() {
        List supportedProfiles = BluetoothAdapter.getDefaultAdapter().getSupportedProfiles();
        String str = "LocalBluetoothProfileManager";
        if (CollectionUtils.isEmpty(supportedProfiles)) {
            Log.d(str, "supportedList is null");
            return;
        }
        if (this.mA2dpProfile == null && supportedProfiles.contains(Integer.valueOf(2))) {
            Log.d(str, "Adding local A2DP profile");
            this.mA2dpProfile = new A2dpProfile(this.mContext, this.mDeviceManager, this);
            addProfile(this.mA2dpProfile, "A2DP", "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mA2dpSinkProfile == null && supportedProfiles.contains(Integer.valueOf(11))) {
            Log.d(str, "Adding local A2DP SINK profile");
            this.mA2dpSinkProfile = new A2dpSinkProfile(this.mContext, this.mDeviceManager, this);
            addProfile(this.mA2dpSinkProfile, "A2DPSink", "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mHeadsetProfile == null && supportedProfiles.contains(Integer.valueOf(1))) {
            Log.d(str, "Adding local HEADSET profile");
            this.mHeadsetProfile = new HeadsetProfile(this.mContext, this.mDeviceManager, this);
            addHeadsetProfile(this.mHeadsetProfile, "HEADSET", "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED", "android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED", 10);
        }
        if (this.mHfpClientProfile == null && supportedProfiles.contains(Integer.valueOf(16))) {
            Log.d(str, "Adding local HfpClient profile");
            this.mHfpClientProfile = new HfpClientProfile(this.mContext, this.mDeviceManager, this);
            addHeadsetProfile(this.mHfpClientProfile, "HEADSET_CLIENT", "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED", "android.bluetooth.headsetclient.profile.action.AUDIO_STATE_CHANGED", 0);
        }
        if (this.mMapClientProfile == null && supportedProfiles.contains(Integer.valueOf(18))) {
            Log.d(str, "Adding local MAP CLIENT profile");
            this.mMapClientProfile = new MapClientProfile(this.mContext, this.mDeviceManager, this);
            addProfile(this.mMapClientProfile, "MAP Client", "android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mMapProfile == null && supportedProfiles.contains(Integer.valueOf(9))) {
            Log.d(str, "Adding local MAP profile");
            this.mMapProfile = new MapProfile(this.mContext, this.mDeviceManager, this);
            addProfile(this.mMapProfile, "MAP", "android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mOppProfile == null && supportedProfiles.contains(Integer.valueOf(20))) {
            Log.d(str, "Adding local OPP profile");
            this.mOppProfile = new OppProfile();
            this.mProfileNameMap.put("OPP", this.mOppProfile);
        }
        if (this.mHearingAidProfile == null && supportedProfiles.contains(Integer.valueOf(21))) {
            Log.d(str, "Adding local Hearing Aid profile");
            this.mHearingAidProfile = new HearingAidProfile(this.mContext, this.mDeviceManager, this);
            addProfile(this.mHearingAidProfile, "HearingAid", "android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mHidProfile == null && supportedProfiles.contains(Integer.valueOf(4))) {
            Log.d(str, "Adding local HID_HOST profile");
            this.mHidProfile = new HidProfile(this.mContext, this.mDeviceManager, this);
            addProfile(this.mHidProfile, "HID", "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mHidDeviceProfile == null && supportedProfiles.contains(Integer.valueOf(19))) {
            Log.d(str, "Adding local HID_DEVICE profile");
            this.mHidDeviceProfile = new HidDeviceProfile(this.mContext, this.mDeviceManager, this);
            addProfile(this.mHidDeviceProfile, "HID DEVICE", "android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mPanProfile == null && supportedProfiles.contains(Integer.valueOf(5))) {
            Log.d(str, "Adding local PAN profile");
            this.mPanProfile = new PanProfile(this.mContext);
            addPanProfile(this.mPanProfile, "PAN", "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mPbapProfile == null && supportedProfiles.contains(Integer.valueOf(6))) {
            Log.d(str, "Adding local PBAP profile");
            this.mPbapProfile = new PbapServerProfile(this.mContext);
            addProfile(this.mPbapProfile, PbapServerProfile.NAME, "android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mPbapClientProfile == null && supportedProfiles.contains(Integer.valueOf(17))) {
            Log.d(str, "Adding local PBAP Client profile");
            this.mPbapClientProfile = new PbapClientProfile(this.mContext, this.mDeviceManager, this);
            addProfile(this.mPbapClientProfile, "PbapClient", "android.bluetooth.pbapclient.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mSapProfile == null && supportedProfiles.contains(Integer.valueOf(10))) {
            Log.d(str, "Adding local SAP profile");
            this.mSapProfile = new SapProfile(this.mContext, this.mDeviceManager, this);
            addProfile(this.mSapProfile, "SAP", "android.bluetooth.sap.profile.action.CONNECTION_STATE_CHANGED");
        }
        if (this.mDunProfile == null && supportedProfiles.contains(Integer.valueOf(22))) {
            Log.d(str, "Adding local DUN profile");
            this.mDunProfile = new DunServerProfile(this.mContext);
            addProfile(this.mDunProfile, "DUN Server", "codeaurora.bluetooth.dun.profile.action.CONNECTION_STATE_CHANGED");
        }
        this.mEventManager.registerProfileIntentReceiver();
    }

    private void addHeadsetProfile(LocalBluetoothProfile localBluetoothProfile, String str, String str2, String str3, int i) {
        HeadsetStateChangeHandler headsetStateChangeHandler = new HeadsetStateChangeHandler(localBluetoothProfile, str3, i);
        this.mEventManager.addProfileHandler(str2, headsetStateChangeHandler);
        this.mEventManager.addProfileHandler(str3, headsetStateChangeHandler);
        this.mProfileNameMap.put(str, localBluetoothProfile);
    }

    private void addProfile(LocalBluetoothProfile localBluetoothProfile, String str, String str2) {
        this.mEventManager.addProfileHandler(str2, new StateChangedHandler(localBluetoothProfile));
        this.mProfileNameMap.put(str, localBluetoothProfile);
    }

    private void addPanProfile(LocalBluetoothProfile localBluetoothProfile, String str, String str2) {
        this.mEventManager.addProfileHandler(str2, new PanStateChangedHandler(localBluetoothProfile));
        this.mProfileNameMap.put(str, localBluetoothProfile);
    }

    /* access modifiers changed from: 0000 */
    public void setBluetoothStateOn() {
        updateLocalProfiles();
        this.mEventManager.readPairedDevices();
    }

    public void addServiceListener(ServiceListener serviceListener) {
        this.mServiceListeners.add(serviceListener);
    }

    /* access modifiers changed from: 0000 */
    public void callServiceConnectedListeners() {
        for (ServiceListener onServiceConnected : this.mServiceListeners) {
            onServiceConnected.onServiceConnected();
        }
    }

    /* access modifiers changed from: 0000 */
    public void callServiceDisconnectedListeners() {
        for (ServiceListener onServiceDisconnected : this.mServiceListeners) {
            onServiceDisconnected.onServiceDisconnected();
        }
    }

    public A2dpProfile getA2dpProfile() {
        return this.mA2dpProfile;
    }

    public HeadsetProfile getHeadsetProfile() {
        return this.mHeadsetProfile;
    }

    public PbapServerProfile getPbapProfile() {
        return this.mPbapProfile;
    }

    public HearingAidProfile getHearingAidProfile() {
        return this.mHearingAidProfile;
    }

    /* access modifiers changed from: 0000 */
    public HidProfile getHidProfile() {
        return this.mHidProfile;
    }

    /* access modifiers changed from: 0000 */
    public HidDeviceProfile getHidDeviceProfile() {
        return this.mHidDeviceProfile;
    }

    /* access modifiers changed from: 0000 */
    public synchronized void updateProfiles(ParcelUuid[] parcelUuidArr, ParcelUuid[] parcelUuidArr2, Collection<LocalBluetoothProfile> collection, Collection<LocalBluetoothProfile> collection2, boolean z, BluetoothDevice bluetoothDevice) {
        collection2.clear();
        collection2.addAll(collection);
        StringBuilder sb = new StringBuilder();
        sb.append("Current Profiles");
        sb.append(collection.toString());
        Log.d("LocalBluetoothProfileManager", sb.toString());
        collection.clear();
        if (parcelUuidArr != null) {
            if (this.mHeadsetProfile != null && ((BluetoothUuid.isUuidPresent(parcelUuidArr2, BluetoothUuid.HSP_AG) && BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.HSP)) || ((BluetoothUuid.isUuidPresent(parcelUuidArr2, BluetoothUuid.Handsfree_AG) && BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Handsfree)) || this.mHeadsetProfile.getConnectionStatus(bluetoothDevice) == 2))) {
                collection.add(this.mHeadsetProfile);
                collection2.remove(this.mHeadsetProfile);
            }
            if (this.mHfpClientProfile != null && BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Handsfree_AG) && BluetoothUuid.isUuidPresent(parcelUuidArr2, BluetoothUuid.Handsfree)) {
                collection.add(this.mHfpClientProfile);
                collection2.remove(this.mHfpClientProfile);
            }
            if (this.mA2dpProfile != null && (BluetoothUuid.containsAnyUuid(parcelUuidArr, A2dpProfile.SINK_UUIDS) || this.mA2dpProfile.getConnectionStatus(bluetoothDevice) == 2)) {
                collection.add(this.mA2dpProfile);
                collection2.remove(this.mA2dpProfile);
            }
            if (BluetoothUuid.containsAnyUuid(parcelUuidArr, A2dpSinkProfile.SRC_UUIDS) && this.mA2dpSinkProfile != null) {
                collection.add(this.mA2dpSinkProfile);
                collection2.remove(this.mA2dpSinkProfile);
            }
            if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.ObexObjectPush) && this.mOppProfile != null) {
                collection.add(this.mOppProfile);
                collection2.remove(this.mOppProfile);
            }
            if ((BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Hid) || BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.Hogp)) && this.mHidProfile != null) {
                collection.add(this.mHidProfile);
                collection2.remove(this.mHidProfile);
            }
            if (!(this.mHidDeviceProfile == null || this.mHidDeviceProfile.getConnectionStatus(bluetoothDevice) == 0)) {
                collection.add(this.mHidDeviceProfile);
                collection2.remove(this.mHidDeviceProfile);
            }
            if (z) {
                Log.d("LocalBluetoothProfileManager", "Valid PAN-NAP connection exists.");
            }
            if ((BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.NAP) && this.mPanProfile != null) || z) {
                collection.add(this.mPanProfile);
                collection2.remove(this.mPanProfile);
            }
            if (this.mMapProfile != null && this.mMapProfile.getConnectionStatus(bluetoothDevice) == 2) {
                collection.add(this.mMapProfile);
                collection2.remove(this.mMapProfile);
                this.mMapProfile.setPreferred(bluetoothDevice, true);
            }
            if (this.mPbapProfile != null && this.mPbapProfile.getConnectionStatus(bluetoothDevice) == 2) {
                collection.add(this.mPbapProfile);
                collection2.remove(this.mPbapProfile);
                this.mPbapProfile.setPreferred(bluetoothDevice, true);
            }
            if (this.mMapClientProfile != null) {
                collection.add(this.mMapClientProfile);
                collection2.remove(this.mMapClientProfile);
            }
            if (this.mPbapClientProfile != null && BluetoothUuid.isUuidPresent(parcelUuidArr2, BluetoothUuid.PBAP_PCE) && BluetoothUuid.containsAnyUuid(parcelUuidArr, PbapClientProfile.SRC_UUIDS)) {
                collection.add(this.mPbapClientProfile);
                collection2.remove(this.mPbapClientProfile);
            }
            if (BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.HearingAid) && this.mHearingAidProfile != null) {
                collection.add(this.mHearingAidProfile);
                collection2.remove(this.mHearingAidProfile);
            }
            if (this.mSapProfile != null && BluetoothUuid.isUuidPresent(parcelUuidArr, BluetoothUuid.SAP)) {
                collection.add(this.mSapProfile);
                collection2.remove(this.mSapProfile);
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("New Profiles");
            sb2.append(collection.toString());
            Log.d("LocalBluetoothProfileManager", sb2.toString());
        }
    }
}

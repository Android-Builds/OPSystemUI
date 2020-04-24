package com.oneplus.volume;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils.ErrorListener;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager.BluetoothManagerCallback;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.systemui.Dependency;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class OpOutputChooser {
    /* access modifiers changed from: private */
    public static boolean DEBUG = Build.DEBUG_ONEPLUS;
    private static final ErrorListener mErrorListener = new ErrorListener() {
        public void onShowError(Context context, String str, int i) {
            Log.i("OpOutputChooser", " init BluetoothManager error");
        }
    };
    private static final BluetoothManagerCallback mOnInitCallback = new BluetoothManagerCallback() {
    };
    private AudioManager mAudioManager;
    private BluetoothCallbackHandler mBluetoothCallbackHandler;
    protected final List<BluetoothDevice> mConnectedDevices = new ArrayList();
    private final Context mContext;
    private String mHeadSetString = null;
    private LocalBluetoothManager mLocalBluetoothManager;
    private OutputChooserCallback mOutputChooserCallback;
    protected LocalBluetoothProfileManager mProfileManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String str = "OpOutputChooser";
            if (OpOutputChooser.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("mReceiver, intent.getAction():");
                sb.append(intent.getAction());
                Log.i(str, sb.toString());
            }
            if (!"android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                if (intent.getAction().equals("android.intent.action.HEADSET_PLUG")) {
                    boolean z = true;
                    boolean z2 = intent.getIntExtra("state", 0) != 0;
                    if (intent.getIntExtra("microphone", 0) == 0) {
                        z = false;
                    }
                    if (OpOutputChooser.DEBUG) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("receive ACTION_USBHEADSET_PLUG, connected:");
                        sb2.append(z2);
                        sb2.append(", hasMic");
                        sb2.append(z);
                        Log.d(str, sb2.toString());
                    }
                    OpOutputChooser.this.updateItems(false);
                } else if (intent.getAction().equals("android.media.STREAM_DEVICES_CHANGED_ACTION")) {
                    OpOutputChooser.this.updateItems(false);
                }
            }
        }
    };
    private String mSpeakerString = null;

    private final class BluetoothCallbackHandler implements BluetoothCallback {
        public void onAudioModeChanged() {
        }

        public void onBluetoothStateChanged(int i) {
        }

        public void onDeviceBondStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        }

        public void onScanningStateChanged(boolean z) {
        }

        private BluetoothCallbackHandler() {
        }

        public void onDeviceAdded(CachedBluetoothDevice cachedBluetoothDevice) {
            if (OpOutputChooser.DEBUG) {
                Log.d("OpOutputChooser", "BluetoothCallbackHandler onDeviceAdded");
            }
            OpOutputChooser.this.updateItems(false);
        }

        public void onDeviceDeleted(CachedBluetoothDevice cachedBluetoothDevice) {
            if (OpOutputChooser.DEBUG) {
                Log.d("OpOutputChooser", "BluetoothCallbackHandler onDeviceDeleted");
            }
            OpOutputChooser.this.updateItems(false);
        }

        public void onConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
            if (OpOutputChooser.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("BluetoothCallbackHandler onConnectionStateChanged, cachedDevice:");
                sb.append(cachedBluetoothDevice);
                Log.d("OpOutputChooser", sb.toString());
            }
        }

        public void onActiveDeviceChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
            if (OpOutputChooser.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("BluetoothCallbackHandler onActiveDeviceChanged, activeDevice:");
                sb.append(cachedBluetoothDevice);
                Log.d("OpOutputChooser", sb.toString());
            }
            OpOutputChooser.this.updateItems(false);
        }
    }

    public static class OutputChooserCallback {
        public void onOutputChooserNotifyActiveDeviceChange(int i, int i2, String str, String str2) {
            throw null;
        }
    }

    public void addCallback(OutputChooserCallback outputChooserCallback) {
        Log.i("OpOutputChooser", "OutputChooserCallback, addCallback");
        this.mOutputChooserCallback = outputChooserCallback;
        findActiveDevice(3);
    }

    public void removeCallback() {
        this.mOutputChooserCallback = null;
    }

    public OpOutputChooser(Context context) {
        String str = "OpOutputChooser";
        Log.i(str, "new OpOutputChooser");
        this.mContext = context;
        this.mHeadSetString = this.mContext.getResources().getString(R$string.quick_settings_footer_audio_headset);
        this.mSpeakerString = this.mContext.getResources().getString(R$string.quick_settings_footer_audio_speaker);
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        FutureTask futureTask = new FutureTask(new Callable() {
            public final Object call() {
                return OpOutputChooser.this.lambda$new$0$OpOutputChooser();
            }
        });
        try {
            futureTask.run();
            this.mLocalBluetoothManager = (LocalBluetoothManager) futureTask.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w(str, "Error getting LocalBluetoothManager.", e);
        }
        LocalBluetoothManager localBluetoothManager = this.mLocalBluetoothManager;
        if (localBluetoothManager == null) {
            Log.e(str, "Bluetooth is not supported on this device");
        } else {
            this.mProfileManager = localBluetoothManager.getProfileManager();
        }
        LocalBluetoothManager localBluetoothManager2 = this.mLocalBluetoothManager;
        if (localBluetoothManager2 != null) {
            localBluetoothManager2.setForegroundActivity(this.mContext);
            this.mBluetoothCallbackHandler = new BluetoothCallbackHandler();
            this.mLocalBluetoothManager.getEventManager().registerCallback(this.mBluetoothCallbackHandler);
            IntentFilter intentFilter = new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            intentFilter.addAction("android.intent.action.HEADSET_PLUG");
            intentFilter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");
            this.mContext.registerReceiver(this.mReceiver, intentFilter);
        }
    }

    public /* synthetic */ LocalBluetoothManager lambda$new$0$OpOutputChooser() throws Exception {
        return getLocalBtManager(this.mContext);
    }

    public void destory() {
        Log.i("OpOutputChooser", "destory OpOutputChooser");
        this.mLocalBluetoothManager.getEventManager().unregisterCallback(this.mBluetoothCallbackHandler);
        this.mLocalBluetoothManager.setForegroundActivity(null);
        this.mContext.unregisterReceiver(this.mReceiver);
    }

    public static LocalBluetoothManager getLocalBtManager(Context context) {
        return (LocalBluetoothManager) Dependency.get(LocalBluetoothManager.class);
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x012c  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0140  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x0145  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void findActiveDevice(int r9) {
        /*
            r8 = this;
            r0 = 3
            java.lang.String r1 = "OpOutputChooser"
            if (r9 == r0) goto L_0x0011
            if (r9 == 0) goto L_0x0011
            boolean r8 = DEBUG
            if (r8 == 0) goto L_0x0010
            java.lang.String r8 = "!= STREAM_MUSIC && != STREAM_VOICE_CALL"
            android.util.Log.i(r1, r8)
        L_0x0010:
            return
        L_0x0011:
            r2 = 896(0x380, float:1.256E-42)
            boolean r2 = r8.isStreamFromOutputDevice(r0, r2)
            r3 = 2
            r4 = 0
            java.lang.String r5 = ""
            java.lang.String r6 = " type:"
            r7 = 0
            if (r2 == 0) goto L_0x0096
            com.android.settingslib.bluetooth.LocalBluetoothProfileManager r9 = r8.mProfileManager
            if (r9 == 0) goto L_0x0034
            com.android.settingslib.bluetooth.A2dpProfile r9 = r9.getA2dpProfile()
            if (r9 == 0) goto L_0x0034
            com.android.settingslib.bluetooth.LocalBluetoothProfileManager r9 = r8.mProfileManager
            com.android.settingslib.bluetooth.A2dpProfile r9 = r9.getA2dpProfile()
            android.bluetooth.BluetoothDevice r4 = r9.getActiveDevice()
        L_0x0034:
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r2 = "StreamFrom output DEVICE_OUT_ALL_A2DP:"
            r9.append(r2)
            r9.append(r4)
            java.lang.String r9 = r9.toString()
            android.util.Log.i(r1, r9)
            if (r4 == 0) goto L_0x0128
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r2 = " getName:"
            r9.append(r2)
            java.lang.String r2 = r4.getName()
            r9.append(r2)
            java.lang.String r2 = " getAddress:"
            r9.append(r2)
            java.lang.String r2 = r4.getAddress()
            r9.append(r2)
            java.lang.String r2 = " getAlias:"
            r9.append(r2)
            java.lang.String r2 = r4.getAlias()
            r9.append(r2)
            java.lang.String r2 = " getAliasName:"
            r9.append(r2)
            java.lang.String r2 = r4.getAliasName()
            r9.append(r2)
            r9.append(r6)
            r9.append(r0)
            java.lang.String r9 = r9.toString()
            android.util.Log.i(r1, r9)
            java.lang.String r5 = r4.getName()
            java.lang.String r9 = r4.getAddress()
            goto L_0x012a
        L_0x0096:
            r0 = 112(0x70, float:1.57E-43)
            boolean r0 = r8.isStreamFromOutputDevice(r7, r0)
            if (r0 == 0) goto L_0x00d7
            com.android.settingslib.bluetooth.LocalBluetoothProfileManager r9 = r8.mProfileManager
            if (r9 == 0) goto L_0x00b2
            com.android.settingslib.bluetooth.HeadsetProfile r9 = r9.getHeadsetProfile()
            if (r9 == 0) goto L_0x00b2
            com.android.settingslib.bluetooth.LocalBluetoothProfileManager r9 = r8.mProfileManager
            com.android.settingslib.bluetooth.HeadsetProfile r9 = r9.getHeadsetProfile()
            android.bluetooth.BluetoothDevice r4 = r9.getActiveDevice()
        L_0x00b2:
            if (r4 == 0) goto L_0x0128
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r0 = "StreamFrom output DEVICE_OUT_ALL_SCO:"
            r9.append(r0)
            r9.append(r4)
            r9.append(r6)
            r9.append(r7)
            java.lang.String r9 = r9.toString()
            android.util.Log.i(r1, r9)
            java.lang.String r5 = r4.getName()
            java.lang.String r9 = r4.getAddress()
            goto L_0x012a
        L_0x00d7:
            r0 = 134217728(0x8000000, float:3.85186E-34)
            boolean r0 = r8.isStreamFromOutputDevice(r9, r0)
            if (r0 == 0) goto L_0x0128
            com.android.settingslib.bluetooth.LocalBluetoothProfileManager r0 = r8.mProfileManager
            if (r0 == 0) goto L_0x0128
            com.android.settingslib.bluetooth.HearingAidProfile r0 = r0.getHearingAidProfile()
            if (r0 == 0) goto L_0x0128
            com.android.settingslib.bluetooth.LocalBluetoothProfileManager r0 = r8.mProfileManager
            com.android.settingslib.bluetooth.HearingAidProfile r0 = r0.getHearingAidProfile()
            java.util.List r0 = r0.getActiveDevices()
            java.util.Iterator r0 = r0.iterator()
        L_0x00f7:
            boolean r2 = r0.hasNext()
            if (r2 == 0) goto L_0x0128
            java.lang.Object r2 = r0.next()
            android.bluetooth.BluetoothDevice r2 = (android.bluetooth.BluetoothDevice) r2
            if (r2 == 0) goto L_0x00f7
            java.util.List<android.bluetooth.BluetoothDevice> r3 = r8.mConnectedDevices
            boolean r3 = r3.contains(r2)
            if (r3 == 0) goto L_0x00f7
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "StreamFrom output DEVICE_OUT_HEARING_AID:"
            r3.append(r4)
            r3.append(r2)
            r3.append(r6)
            r3.append(r9)
            java.lang.String r2 = r3.toString()
            android.util.Log.i(r1, r2)
            goto L_0x00f7
        L_0x0128:
            r9 = r5
            r3 = r7
        L_0x012a:
            if (r3 != 0) goto L_0x0140
            java.lang.String r0 = "StreamFrom no active device"
            android.util.Log.i(r1, r0)
            android.media.AudioManager r0 = r8.mAudioManager
            boolean r0 = r0.isWiredHeadsetOn()
            if (r0 == 0) goto L_0x013d
            java.lang.String r5 = r8.mHeadSetString
            r7 = 1
            goto L_0x0141
        L_0x013d:
            java.lang.String r5 = r8.mSpeakerString
            goto L_0x0141
        L_0x0140:
            r7 = r3
        L_0x0141:
            com.oneplus.volume.OpOutputChooser$OutputChooserCallback r0 = r8.mOutputChooserCallback
            if (r0 == 0) goto L_0x014c
            int r8 = r8.getIconResId(r7)
            r0.onOutputChooserNotifyActiveDeviceChange(r7, r8, r5, r9)
        L_0x014c:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.volume.OpOutputChooser.findActiveDevice(int):void");
    }

    /* access modifiers changed from: protected */
    public boolean isStreamFromOutputDevice(int i, int i2) {
        return (this.mAudioManager.getDevicesForStream(i) & i2) != 0;
    }

    /* access modifiers changed from: private */
    public void updateItems(boolean z) {
        findActiveDevice(3);
    }

    private int getIconResId(int i) {
        if (i == 0) {
            return R$drawable.ic_output_chooser_phone;
        }
        if (i == 1) {
            return R$drawable.ic_output_chooser_headset;
        }
        if (i != 2) {
            return 0;
        }
        return R$drawable.ic_qs_bluetooth_connected;
    }
}

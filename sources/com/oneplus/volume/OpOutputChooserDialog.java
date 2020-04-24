package com.oneplus.volume;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouteSelector.Builder;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouter.RouteInfo;
import com.airbnb.lottie.C0526R;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.Utils;
import com.android.settingslib.bluetooth.A2dpProfile;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothUtils.ErrorListener;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.HeadsetProfile;
import com.android.settingslib.bluetooth.HearingAidProfile;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager.BluetoothManagerCallback;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.R$style;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.plugins.VolumeDialogController.Callbacks;
import com.android.systemui.plugins.VolumeDialogController.State;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.volume.MediaRouterWrapper;
import com.android.systemui.volume.SystemUIInterpolators$LogAccelerateInterpolator;
import com.oneplus.volume.OpOutputChooserLayout.Callback;
import com.oneplus.volume.OpOutputChooserLayout.Item;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class OpOutputChooserDialog extends Dialog implements OnDismissListener, Callback {
    /* access modifiers changed from: private */
    public static boolean DEBUG = Build.DEBUG_ONEPLUS;
    private static final ErrorListener mErrorListener = new ErrorListener() {
        public void onShowError(Context context, String str, int i) {
            Log.i("OpOutputChooserDialog", " init BluetoothManager error");
        }
    };
    private static final BluetoothManagerCallback mOnInitCallback = new BluetoothManagerCallback() {
    };
    protected boolean isAttached;
    private AudioManager mAudioManager;
    private int mBgDrawable = 0;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothCallbackHandler mBluetoothCallbackHandler;
    private final BluetoothController mBluetoothController;
    /* access modifiers changed from: private */
    public BluetoothHeadset mBluetoothHeadset;
    private final ServiceListener mBluetoothProfileServiceListener = new ServiceListener() {
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            StringBuilder sb = new StringBuilder();
            sb.append("BluetoothProfile.ServiceListener / onServiceConnected / profile:");
            sb.append(i);
            sb.append("  / proxy: ");
            sb.append(bluetoothProfile);
            Log.i("OpOutputChooserDialog", sb.toString());
            synchronized (this) {
                if (i == 1) {
                    OpOutputChooserDialog.this.mBluetoothHeadset = (BluetoothHeadset) bluetoothProfile;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("- Got BluetoothHeadset: ");
                    sb2.append(OpOutputChooserDialog.this.mBluetoothHeadset);
                    Log.i("OpOutputChooserDialog", sb2.toString());
                } else {
                    Log.w("OpOutputChooserDialog", "Connected to non-headset bluetooth service. Not changing bluetooth headset.");
                }
            }
        }

        public void onServiceDisconnected(int i) {
            StringBuilder sb = new StringBuilder();
            sb.append("BluetoothProfile.ServiceListener / onServiceDisconnected / profile:");
            sb.append(i);
            Log.i("OpOutputChooserDialog", sb.toString());
            synchronized (this) {
                OpOutputChooserDialog.this.mBluetoothHeadset = null;
                Log.i("OpOutputChooserDialog", "Lost BluetoothHeadset service. Removing all tracked devices.");
            }
        }
    };
    private final BluetoothController.Callback mCallback = new BluetoothController.Callback() {
        public void onBluetoothStateChange(boolean z) {
            OpOutputChooserDialog.this.updateItems(false);
        }

        public void onBluetoothDevicesChanged() {
            OpOutputChooserDialog.this.updateItems(false);
        }
    };
    /* access modifiers changed from: private */
    public Runnable mCheckActiveDeviceRunnable = new Runnable() {
        public void run() {
            Log.i("OpOutputChooserDialog", " CheckActiveDevice again");
            OpOutputChooserDialog.this.mPreSelectDevice = null;
            OpOutputChooserDialog.this.updateItems(false);
        }
    };
    protected final List<BluetoothDevice> mConnectedDevices = new ArrayList();
    private final Context mContext;
    private final VolumeDialogController mController;
    private final Callbacks mControllerCallbackH = new Callbacks() {
        public void onAccessibilityModeChanged(Boolean bool) {
        }

        public void onCaptionComponentStateChanged(Boolean bool, Boolean bool2) {
        }

        public void onDismissRequested(int i) {
        }

        public void onLayoutDirectionChanged(int i) {
        }

        public void onShowSafetyWarning(int i) {
        }

        public void onShowSilentHint() {
        }

        public void onShowVibrateHint() {
        }

        public void onStateChanged(State state) {
        }

        public void onShowRequested(int i) {
            OpOutputChooserDialog.this.dismiss();
        }

        public void onScreenOff() {
            OpOutputChooserDialog.this.dismiss();
        }

        public void onConfigurationChanged() {
            OpOutputChooserDialog.this.dismiss();
        }
    };
    private CachedBluetoothDevice mCurrentActiveDevice = null;
    private Drawable mDefaultIcon;
    private int mEmytyIconColor = 0;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                OpOutputChooserDialog.this.updateItems(((Boolean) message.obj).booleanValue());
            } else if (i == 2) {
                CachedBluetoothDevice cachedBluetoothDevice = (CachedBluetoothDevice) message.obj;
                OpOutputChooserDialog.this.setActiveBluetoothDevice(cachedBluetoothDevice);
                OpOutputChooserDialog.this.mPaddingActiveDevice = null;
                StringBuilder sb = new StringBuilder();
                sb.append("active the select device:");
                sb.append(cachedBluetoothDevice.getName());
                Log.i("OpOutputChooserDialog", sb.toString());
            } else if (i == 3) {
                OpOutputChooserDialog.this.onDetailItemClick((Item) message.obj);
            }
        }
    };
    private String mHeadSetString = null;
    private int mIconColor = 0;
    private boolean mIsInCall;
    private long mLastDetailItemClickTime = 0;
    private long mLastUpdateTime;
    private LocalBluetoothManager mLocalBluetoothManager;
    /* access modifiers changed from: private */
    public CachedBluetoothDevice mPaddingActiveDevice = null;
    /* access modifiers changed from: private */
    public CachedBluetoothDevice mPreSelectDevice = null;
    private int mPrimaryTextColor = 0;
    protected LocalBluetoothProfileManager mProfileManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String str = "OpOutputChooserDialog";
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                if (OpOutputChooserDialog.DEBUG) {
                    Log.d(str, "Received ACTION_CLOSE_SYSTEM_DIALOGS");
                }
                OpOutputChooserDialog.this.cancel();
                OpOutputChooserDialog.this.cleanUp();
            } else if (intent.getAction().equals("android.intent.action.HEADSET_PLUG") || intent.getAction().equals("android.media.STREAM_DEVICES_CHANGED_ACTION")) {
                if (OpOutputChooserDialog.DEBUG) {
                    Log.d(str, "Received ACTION_HEADSET_PLUG");
                }
                OpOutputChooserDialog.this.updateItems(false);
            }
        }
    };
    private final MediaRouteSelector mRouteSelector;
    private final MediaRouterWrapper mRouter;
    private final MediaRouterCallback mRouterCallback;
    private int mSecondaryTextColor = 0;
    private Drawable mSpeakerGroupIcon;
    private Drawable mSpeakerIcon;
    private String mSpeakerString = null;
    private TelecomManager mTelecomManager;
    private int mThemeColorMode = 0;
    private Drawable mTvIcon;
    private OpOutputChooserLayout mView;
    private WifiManager mWifiManager;

    private final class BluetoothCallbackHandler implements BluetoothCallback {
        public void onAudioModeChanged() {
        }

        public void onBluetoothStateChanged(int i) {
        }

        public void onConnectionStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        }

        public void onDeviceBondStateChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
        }

        public void onScanningStateChanged(boolean z) {
        }

        private BluetoothCallbackHandler() {
        }

        public void onDeviceAdded(CachedBluetoothDevice cachedBluetoothDevice) {
            OpOutputChooserDialog.this.updateItems(false);
        }

        public void onDeviceDeleted(CachedBluetoothDevice cachedBluetoothDevice) {
            OpOutputChooserDialog.this.updateItems(false);
        }

        public void onActiveDeviceChanged(CachedBluetoothDevice cachedBluetoothDevice, int i) {
            StringBuilder sb = new StringBuilder();
            sb.append(" onActiveDeviceChanged:");
            sb.append(cachedBluetoothDevice);
            Log.i("OpOutputChooserDialog", sb.toString());
            OpOutputChooserDialog.this.mHandler.removeCallbacks(OpOutputChooserDialog.this.mCheckActiveDeviceRunnable);
            if (cachedBluetoothDevice == OpOutputChooserDialog.this.mPreSelectDevice) {
                OpOutputChooserDialog.this.mHandler.postDelayed(OpOutputChooserDialog.this.mCheckActiveDeviceRunnable, 3000);
            } else {
                OpOutputChooserDialog.this.mPreSelectDevice = null;
            }
            OpOutputChooserDialog.this.updateItems(false);
        }
    }

    static final class ItemComparator implements Comparator<Item> {
        public static final ItemComparator sInstance = new ItemComparator();

        ItemComparator() {
        }

        public int compare(Item item, Item item2) {
            boolean z = item.canDisconnect;
            boolean z2 = item2.canDisconnect;
            if (z != z2) {
                return Boolean.compare(z2, z);
            }
            int i = item.deviceType;
            int i2 = item2.deviceType;
            if (i != i2) {
                return Integer.compare(i, i2);
            }
            return item.line1.toString().compareToIgnoreCase(item2.line1.toString());
        }
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        private MediaRouterCallback() {
        }

        public void onRouteAdded(MediaRouter mediaRouter, RouteInfo routeInfo) {
            OpOutputChooserDialog.this.updateItems(false);
        }

        public void onRouteRemoved(MediaRouter mediaRouter, RouteInfo routeInfo) {
            OpOutputChooserDialog.this.updateItems(false);
        }

        public void onRouteChanged(MediaRouter mediaRouter, RouteInfo routeInfo) {
            OpOutputChooserDialog.this.updateItems(false);
        }

        public void onRouteSelected(MediaRouter mediaRouter, RouteInfo routeInfo) {
            OpOutputChooserDialog.this.updateItems(false);
        }
    }

    private int getDefaultIndext() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public void cleanUp() {
        throw null;
    }

    public OpOutputChooserDialog(Context context, MediaRouterWrapper mediaRouterWrapper) {
        super(context, R$style.qs_theme);
        this.mContext = context;
        this.mBluetoothController = (BluetoothController) Dependency.get(BluetoothController.class);
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mTelecomManager = (TelecomManager) context.getSystemService("telecom");
        this.mIsInCall = this.mTelecomManager.isInCall();
        this.mRouter = mediaRouterWrapper;
        this.mRouterCallback = new MediaRouterCallback();
        Builder builder = new Builder();
        builder.addControlCategory("android.media.intent.category.REMOTE_PLAYBACK");
        this.mRouteSelector = builder.build();
        IntentFilter intentFilter = new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        intentFilter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");
        context.registerReceiver(this.mReceiver, intentFilter);
        this.mController = (VolumeDialogController) Dependency.get(VolumeDialogController.class);
        Window window = getWindow();
        window.requestFeature(1);
        window.setBackgroundDrawable(new ColorDrawable(0));
        window.clearFlags(65538);
        window.addFlags(17563944);
        window.setType(2020);
        LayoutParams attributes = window.getAttributes();
        attributes.gravity = isLandscape() ? 21 : 19;
        window.setAttributes(attributes);
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        String str = "OpOutputChooserDialog";
        super.onCreate(bundle);
        setContentView(R$layout.output_chooser);
        setCanceledOnTouchOutside(true);
        setOnDismissListener(new OnDismissListener() {
            public final void onDismiss(DialogInterface dialogInterface) {
                OpOutputChooserDialog.this.onDismiss(dialogInterface);
            }
        });
        this.mView = (OpOutputChooserLayout) findViewById(R$id.output_chooser);
        this.mView.setCallback(this);
        updateTile();
        this.mDefaultIcon = this.mContext.getDrawable(R$drawable.ic_cast);
        this.mTvIcon = this.mContext.getDrawable(R$drawable.ic_tv);
        this.mSpeakerIcon = this.mContext.getDrawable(R$drawable.ic_speaker);
        this.mSpeakerGroupIcon = this.mContext.getDrawable(R$drawable.ic_speaker_group);
        this.mHeadSetString = this.mContext.getResources().getString(R$string.quick_settings_footer_audio_headset);
        this.mSpeakerString = this.mContext.getResources().getString(R$string.quick_settings_footer_audio_speaker);
        this.mPreSelectDevice = null;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        FutureTask futureTask = new FutureTask(new Callable() {
            public final Object call() {
                return OpOutputChooserDialog.this.lambda$onCreate$0$OpOutputChooserDialog();
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
            boolean z = !this.mWifiManager.isWifiEnabled();
            boolean z2 = !this.mBluetoothController.isBluetoothEnabled();
            if (z && z2) {
                this.mView.setEmptyState(getDisabledServicesMessage(z, z2));
            }
            this.mView.postDelayed(new Runnable() {
                public final void run() {
                    OpOutputChooserDialog.this.lambda$onCreate$1$OpOutputChooserDialog();
                }
            }, 5000);
            this.mBluetoothCallbackHandler = new BluetoothCallbackHandler();
            this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothAdapter bluetoothAdapter = this.mBluetoothAdapter;
            if (bluetoothAdapter != null && bluetoothAdapter.getState() == 12) {
                this.mBluetoothAdapter.getProfileProxy(this.mContext, this.mBluetoothProfileServiceListener, 1);
            }
        }
    }

    public /* synthetic */ LocalBluetoothManager lambda$onCreate$0$OpOutputChooserDialog() throws Exception {
        return getLocalBtManager(this.mContext);
    }

    public /* synthetic */ void lambda$onCreate$1$OpOutputChooserDialog() {
        updateItems(true);
    }

    /* access modifiers changed from: protected */
    public void onStart() {
        super.onStart();
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mBluetoothController.addCallback(this.mCallback);
        this.mController.addCallback(this.mControllerCallbackH, this.mHandler);
        this.mLocalBluetoothManager.getEventManager().registerCallback(this.mBluetoothCallbackHandler);
        this.isAttached = true;
    }

    public void onDetachedFromWindow() {
        this.isAttached = false;
        this.mRouter.removeCallback(this.mRouterCallback);
        this.mController.removeCallback(this.mControllerCallbackH);
        this.mBluetoothController.removeCallback(this.mCallback);
        this.mLocalBluetoothManager.getEventManager().unregisterCallback(this.mBluetoothCallbackHandler);
        this.mLocalBluetoothManager.setForegroundActivity(null);
        super.onDetachedFromWindow();
    }

    public void onDismiss(DialogInterface dialogInterface) {
        this.mContext.unregisterReceiver(this.mReceiver);
        cleanUp();
    }

    private void updateTile() {
        if (this.mIsInCall) {
            this.mView.setTitle(R$string.output_calls_title);
        } else {
            this.mView.setTitle(R$string.output_title);
        }
    }

    public void show() {
        super.show();
        ((MetricsLogger) Dependency.get(MetricsLogger.class)).visible(1295);
        this.mView.setTranslationX(getAnimTranslation());
        this.mView.setAlpha(0.0f);
        this.mView.animate().alpha(1.0f).translationX(0.0f).setDuration(300).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).withEndAction(new Runnable() {
            public final void run() {
                OpOutputChooserDialog.this.lambda$show$2$OpOutputChooserDialog();
            }
        }).start();
    }

    public /* synthetic */ void lambda$show$2$OpOutputChooserDialog() {
        getWindow().getDecorView().requestAccessibilityFocus();
    }

    public void dismiss() {
        ((MetricsLogger) Dependency.get(MetricsLogger.class)).hidden(1295);
        this.mView.setTranslationX(0.0f);
        this.mView.setAlpha(1.0f);
        this.mView.animate().alpha(0.0f).translationX(getAnimTranslation()).setDuration(300).withEndAction(new Runnable() {
            public final void run() {
                OpOutputChooserDialog.this.lambda$dismiss$3$OpOutputChooserDialog();
            }
        }).setInterpolator(new SystemUIInterpolators$LogAccelerateInterpolator()).start();
    }

    public /* synthetic */ void lambda$dismiss$3$OpOutputChooserDialog() {
        super.dismiss();
    }

    private float getAnimTranslation() {
        float dimension = getContext().getResources().getDimension(R$dimen.op_output_chooser_dialog_panel_width) / 2.0f;
        return isLandscape() ? dimension : -dimension;
    }

    public void onDetailItemClick(Item item) {
        String str = "OpOutputChooserDialog";
        if (item == null || item.tag == null) {
            Log.i(str, "onDetailItemClick / item == null || item.tag == null");
        } else if (SystemClock.uptimeMillis() - this.mLastDetailItemClickTime < 500) {
            this.mHandler.removeMessages(3);
            Handler handler = this.mHandler;
            handler.sendMessageAtTime(handler.obtainMessage(3, 0, 0, item), this.mLastDetailItemClickTime + 500);
        } else {
            this.mLastDetailItemClickTime = SystemClock.uptimeMillis();
            StringBuilder sb = new StringBuilder();
            sb.append("onDetailItemClick:");
            sb.append(item.deviceType);
            sb.append(" tag:");
            sb.append(item.tag);
            Log.i(str, sb.toString());
            this.mPaddingActiveDevice = null;
            int i = item.deviceType;
            if (i == Item.DEVICE_TYPE_BT) {
                CachedBluetoothDevice cachedBluetoothDevice = (CachedBluetoothDevice) item.tag;
                if (cachedBluetoothDevice.getMaxConnectionState() == 0) {
                    ((MetricsLogger) Dependency.get(MetricsLogger.class)).action(1296);
                    this.mPaddingActiveDevice = cachedBluetoothDevice;
                    this.mBluetoothController.connect(cachedBluetoothDevice);
                } else {
                    this.mPreSelectDevice = cachedBluetoothDevice;
                    setActiveBluetoothDevice(cachedBluetoothDevice);
                }
            } else if (i == Item.DEVICE_TYPE_MEDIA_ROUTER) {
                RouteInfo routeInfo = (RouteInfo) item.tag;
                if (routeInfo.isEnabled()) {
                    ((MetricsLogger) Dependency.get(MetricsLogger.class)).action(1296);
                    routeInfo.select();
                }
            } else if (i == Item.DEVICE_TYPE_PHONE || i == Item.DEVICE_TYPE_HEADSET) {
                setActiveBluetoothDevice(null);
                this.mPreSelectDevice = null;
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("onDetailItemClick mPreSelectDevice:");
            sb2.append(this.mPreSelectDevice);
            sb2.append(" mPaddingActiveDevice:");
            sb2.append(this.mPaddingActiveDevice);
            Log.d(str, sb2.toString());
        }
    }

    /* access modifiers changed from: private */
    public void updateItems(boolean z) {
        if (SystemClock.uptimeMillis() - this.mLastUpdateTime < 300) {
            this.mHandler.removeMessages(1);
            Handler handler = this.mHandler;
            handler.sendMessageAtTime(handler.obtainMessage(1, Boolean.valueOf(z)), this.mLastUpdateTime + 300);
            return;
        }
        this.mLastUpdateTime = SystemClock.uptimeMillis();
        if (this.mView != null) {
            ArrayList arrayList = new ArrayList();
            boolean z2 = !this.mWifiManager.isWifiEnabled();
            boolean isBluetoothEnabled = true ^ this.mBluetoothController.isBluetoothEnabled();
            if (!isBluetoothEnabled) {
                addBluetoothDevices(arrayList);
            }
            addPhoneDevices(arrayList);
            arrayList.sort(ItemComparator.sInstance);
            setSelecter(arrayList);
            if (arrayList.size() == 0 && z) {
                String string = this.mContext.getString(R$string.output_none_found);
                if (z2 || isBluetoothEnabled) {
                    string = getDisabledServicesMessage(z2, isBluetoothEnabled);
                }
                this.mView.setEmptyState(string);
            }
            this.mView.setItems((Item[]) arrayList.toArray(new Item[arrayList.size()]));
        }
    }

    private String getDisabledServicesMessage(boolean z, boolean z2) {
        String str;
        Context context = this.mContext;
        int i = R$string.output_none_found_service_off;
        Object[] objArr = new Object[1];
        if (z && z2) {
            str = context.getString(R$string.output_service_bt_wifi);
        } else if (z) {
            str = this.mContext.getString(R$string.output_service_wifi);
        } else {
            str = this.mContext.getString(R$string.output_service_bt);
        }
        objArr[0] = str;
        return context.getString(i, objArr);
    }

    private void addBluetoothDevices(List<Item> list) {
        this.mConnectedDevices.clear();
        Collection<CachedBluetoothDevice> devices = getDevices();
        if (devices != null) {
            int i = 0;
            int i2 = 0;
            for (CachedBluetoothDevice cachedBluetoothDevice : devices) {
                if (!(this.mBluetoothController.getBondState(cachedBluetoothDevice) == 10 || cachedBluetoothDevice == null)) {
                    if (cachedBluetoothDevice.getBtClass() != null) {
                        int majorDeviceClass = cachedBluetoothDevice.getBtClass().getMajorDeviceClass();
                        if (!(majorDeviceClass == 1024 || majorDeviceClass == 7936)) {
                        }
                    }
                    Item item = new Item();
                    item.iconResId = R$drawable.ic_qs_bluetooth_on;
                    item.line1 = cachedBluetoothDevice.getName();
                    item.tag = cachedBluetoothDevice;
                    item.deviceType = Item.DEVICE_TYPE_BT;
                    int maxConnectionState = cachedBluetoothDevice.getMaxConnectionState();
                    if (maxConnectionState == 2) {
                        item.iconResId = R$drawable.ic_qs_bluetooth_connected;
                        int batteryLevel = cachedBluetoothDevice.getBatteryLevel();
                        if (batteryLevel != -1) {
                            item.icon = (Drawable) BluetoothUtils.getBtClassDrawableWithDescription(getContext(), cachedBluetoothDevice).first;
                            item.line2 = this.mContext.getString(R$string.quick_settings_connected_battery_level, new Object[]{Utils.formatPercentage(batteryLevel)});
                        } else {
                            item.line2 = this.mContext.getString(R$string.quick_settings_connected);
                        }
                        item.canDisconnect = true;
                        list.add(i, item);
                        this.mConnectedDevices.add(cachedBluetoothDevice.getDevice());
                        if (this.mPaddingActiveDevice == cachedBluetoothDevice) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("The active device:");
                            sb.append(cachedBluetoothDevice.getName());
                            Log.i("OpOutputChooserDialog", sb.toString());
                            Message message = new Message();
                            message.what = 2;
                            message.obj = this.mPaddingActiveDevice;
                            this.mHandler.sendMessage(message);
                        }
                        i++;
                    } else if (maxConnectionState == 1) {
                        item.iconResId = R$drawable.ic_qs_bluetooth_connecting;
                        item.line2 = this.mContext.getString(R$string.quick_settings_connecting);
                        list.add(i, item);
                    } else {
                        list.add(item);
                    }
                    i2++;
                    if (i2 == 10) {
                        return;
                    }
                }
            }
        }
    }

    private void addPhoneDevices(List<Item> list) {
        Item item = new Item();
        if (this.mAudioManager.isWiredHeadsetOn()) {
            item.line1 = this.mHeadSetString;
            item.iconResId = R$drawable.ic_output_chooser_headset;
            item.deviceType = Item.DEVICE_TYPE_HEADSET;
        } else {
            item.line1 = this.mSpeakerString;
            item.iconResId = R$drawable.ic_output_chooser_phone;
            item.deviceType = Item.DEVICE_TYPE_PHONE;
        }
        item.tag = Integer.valueOf(item.deviceType);
        item.canDisconnect = true;
        list.add(item);
    }

    private void setSelecter(List<Item> list) {
        if (list.size() > 0) {
            int defaultIndext = getDefaultIndext();
            BluetoothDevice findActiveDevice = findActiveDevice(3);
            int size = list.size();
            int i = -1;
            int i2 = -1;
            for (int i3 = 1; i3 < size; i3++) {
                if (((Item) list.get(i3)).tag instanceof CachedBluetoothDevice) {
                    BluetoothDevice device = ((CachedBluetoothDevice) ((Item) list.get(i3)).tag).getDevice();
                    if (device.equals(findActiveDevice)) {
                        i = i3;
                    }
                    CachedBluetoothDevice cachedBluetoothDevice = this.mPreSelectDevice;
                    if (cachedBluetoothDevice != null && cachedBluetoothDevice.getDevice().equals(device)) {
                        i2 = i3;
                    }
                }
            }
            if (i != -1) {
                defaultIndext = i;
            } else if (i2 != -1) {
                defaultIndext = i2;
            }
            if (defaultIndext < list.size()) {
                StringBuilder sb = new StringBuilder();
                sb.append("activeDevice = ");
                sb.append(findActiveDevice);
                sb.append(" mPreSelectDevice:");
                sb.append(this.mPreSelectDevice);
                sb.append(" selectedDeviceIndex:");
                sb.append(defaultIndext);
                sb.append(" cehck:");
                sb.append(((Item) list.get(defaultIndext)).tag);
                Log.d("OpOutputChooserDialog", sb.toString());
                ((Item) list.get(defaultIndext)).selected = true;
                if (((Item) list.get(defaultIndext)).tag instanceof CachedBluetoothDevice) {
                    this.mCurrentActiveDevice = (CachedBluetoothDevice) ((Item) list.get(defaultIndext)).tag;
                } else {
                    this.mCurrentActiveDevice = null;
                }
            }
        }
    }

    public void setActiveBluetoothDevice(CachedBluetoothDevice cachedBluetoothDevice) {
        if (this.mProfileManager != null && this.mCurrentActiveDevice != cachedBluetoothDevice) {
            this.mCurrentActiveDevice = cachedBluetoothDevice;
            StringBuilder sb = new StringBuilder();
            sb.append("setActiveBluetoothDevice:");
            sb.append(cachedBluetoothDevice);
            String str = "OpOutputChooserDialog";
            Log.i(str, sb.toString());
            if (cachedBluetoothDevice != null) {
                cachedBluetoothDevice.setActive();
            } else {
                A2dpProfile a2dpProfile = this.mProfileManager.getA2dpProfile();
                HeadsetProfile headsetProfile = this.mProfileManager.getHeadsetProfile();
                HearingAidProfile hearingAidProfile = this.mProfileManager.getHearingAidProfile();
                if (a2dpProfile != null) {
                    a2dpProfile.setActiveDevice(null);
                }
                if (headsetProfile != null) {
                    headsetProfile.setActiveDevice(null);
                }
                if (hearingAidProfile != null) {
                    hearingAidProfile.setActiveDevice(null);
                }
            }
            if (this.mBluetoothHeadset != null) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("mBluetoothHeadset:");
                sb2.append(this.mBluetoothHeadset);
                sb2.append(" / mBluetoothHeadset.isAudioOn():");
                sb2.append(this.mBluetoothHeadset.isAudioOn());
                Log.i(str, sb2.toString());
                if (!this.mBluetoothHeadset.isAudioOn()) {
                    this.mBluetoothHeadset.connectAudio();
                }
            }
        }
    }

    public static LocalBluetoothManager getLocalBtManager(Context context) {
        return (LocalBluetoothManager) Dependency.get(LocalBluetoothManager.class);
    }

    /* access modifiers changed from: protected */
    public BluetoothDevice findActiveDevice(int i) {
        if (i != 3 && i != 0) {
            return null;
        }
        String str = " type:";
        String str2 = "OpOutputChooserDialog";
        if (isStreamFromOutputDevice(3, 896)) {
            StringBuilder sb = new StringBuilder();
            sb.append("StreamFrom output DEVICE_OUT_ALL_A2DP:");
            sb.append(this.mProfileManager.getA2dpProfile().getActiveDevice());
            sb.append(str);
            sb.append(3);
            Log.i(str2, sb.toString());
            return this.mProfileManager.getA2dpProfile().getActiveDevice();
        } else if (isStreamFromOutputDevice(0, C0526R.styleable.AppCompatTheme_toolbarNavigationButtonStyle)) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("StreamFrom output DEVICE_OUT_ALL_SCO:");
            sb2.append(this.mProfileManager.getHeadsetProfile().getActiveDevice());
            sb2.append(str);
            sb2.append(0);
            Log.i(str2, sb2.toString());
            return this.mProfileManager.getHeadsetProfile().getActiveDevice();
        } else {
            if (isStreamFromOutputDevice(i, 134217728)) {
                for (BluetoothDevice bluetoothDevice : this.mProfileManager.getHearingAidProfile().getActiveDevices()) {
                    if (bluetoothDevice != null && this.mConnectedDevices.contains(bluetoothDevice)) {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("StreamFrom output DEVICE_OUT_HEARING_AID:");
                        sb3.append(bluetoothDevice);
                        sb3.append(str);
                        sb3.append(i);
                        Log.i(str2, sb3.toString());
                        return bluetoothDevice;
                    }
                }
            }
            Log.i(str2, "no active device");
            return null;
        }
    }

    /* access modifiers changed from: protected */
    public boolean isStreamFromOutputDevice(int i, int i2) {
        return (this.mAudioManager.getDevicesForStream(i) & i2) != 0;
    }

    public void setTheme(int i) {
        Resources resources = this.mContext.getResources();
        if (i != 1) {
            this.mIconColor = resources.getColor(R$color.oneplus_contorl_icon_color_accent_active_light);
            this.mEmytyIconColor = resources.getColor(R$color.oneplus_contorl_icon_color_active_light);
            this.mPrimaryTextColor = resources.getColor(R$color.oneplus_contorl_text_color_primary_light);
            this.mSecondaryTextColor = resources.getColor(R$color.oneplus_contorl_text_color_secondary_light);
            this.mBgDrawable = R$drawable.volume_dialog_bg_light;
        } else {
            this.mIconColor = resources.getColor(R$color.oneplus_contorl_icon_color_accent_active_dark);
            this.mEmytyIconColor = resources.getColor(R$color.oneplus_contorl_icon_color_active_dark);
            this.mPrimaryTextColor = resources.getColor(R$color.oneplus_contorl_text_color_primary_dark);
            this.mSecondaryTextColor = resources.getColor(R$color.oneplus_contorl_text_color_secondary_dark);
            this.mBgDrawable = R$drawable.volume_dialog_bg_dark;
        }
        this.mView.setBackgroundDrawable(this.mContext.getResources().getDrawable(this.mBgDrawable));
        this.mView.setTitleColor(this.mSecondaryTextColor);
        this.mView.setEmptyIconColor(this.mEmytyIconColor);
        this.mView.setEmptyTextColor(this.mSecondaryTextColor);
        this.mView.setBackKeyColor(this.mIconColor);
        updateItems(false);
    }

    public int getPrimaryTextColor() {
        return this.mPrimaryTextColor;
    }

    public int getSecondaryTextColor() {
        return this.mSecondaryTextColor;
    }

    public int getIconColor() {
        return this.mIconColor;
    }

    private boolean isLandscape() {
        return this.mContext.getResources().getConfiguration().orientation == 2;
    }

    public Collection<CachedBluetoothDevice> getDevices() {
        LocalBluetoothManager localBluetoothManager = this.mLocalBluetoothManager;
        if (localBluetoothManager != null) {
            return localBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();
        }
        return null;
    }

    public void backToVolumeDialog() {
        dismiss();
        VolumeDialogController volumeDialogController = this.mController;
        if (volumeDialogController != null) {
            volumeDialogController.showVolumeDialog(4);
        }
    }
}

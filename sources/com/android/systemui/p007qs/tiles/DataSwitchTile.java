package com.android.systemui.p007qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.SysUIToast;
import com.android.systemui.p007qs.QSHost;
import com.android.systemui.p007qs.tileimpl.QSTileImpl;
import com.android.systemui.p007qs.tileimpl.QSTileImpl.ResourceIcon;
import com.android.systemui.plugins.p006qs.QSTile.BooleanState;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.oneplus.util.SystemSetting;
import java.lang.reflect.Method;
import org.codeaurora.internal.IExtTelephony;
import org.codeaurora.internal.IExtTelephony.Stub;

/* renamed from: com.android.systemui.qs.tiles.DataSwitchTile */
public class DataSwitchTile extends QSTileImpl<BooleanState> {
    static final Intent DATA_SWITCH_SETTINGS = new Intent("oneplus.intent.action.SIM_AND_NETWORK_SETTINGS");
    private final String ESPORT_MODE_KEY = "esport_mode_enabled";
    /* access modifiers changed from: private */
    public boolean mCanSwitch = true;
    protected final NetworkController mController;
    private SystemSetting mEsportModeSetting;
    private IExtTelephony mExtTelephony = null;
    private MyCallStateListener mPhoneStateListener;
    private boolean mRegistered = false;
    protected final DataSwitchSignalCallback mSignalCallback = new DataSwitchSignalCallback();
    private int mSimCount = 0;
    BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(DataSwitchTile.this.TAG, "mSimReceiver:onReceive");
            DataSwitchTile.this.refreshState();
        }
    };
    private SubscriptionManager mSubscriptionManager;
    /* access modifiers changed from: private */
    public TelephonyManager mTelephonyManager;
    /* access modifiers changed from: private */
    public boolean mVirtualSimExist = false;

    /* renamed from: com.android.systemui.qs.tiles.DataSwitchTile$DataSwitchSignalCallback */
    protected final class DataSwitchSignalCallback implements SignalCallback {
        protected DataSwitchSignalCallback() {
        }

        public void setVirtualSimstate(int[] iArr) {
            boolean z = false;
            if (iArr != null && iArr.length > 0) {
                int length = iArr.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    } else if (iArr[i] != NetworkControllerImpl.SOFTSIM_DISABLE) {
                        z = true;
                        break;
                    } else {
                        i++;
                    }
                }
            }
            String access$400 = DataSwitchTile.this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("virtual sim state change: ");
            sb.append(DataSwitchTile.this.mVirtualSimExist);
            sb.append(" to ");
            sb.append(z);
            Log.d(access$400, sb.toString());
            DataSwitchTile.this.mVirtualSimExist = z;
            DataSwitchTile.this.refreshState();
        }
    }

    /* renamed from: com.android.systemui.qs.tiles.DataSwitchTile$MyCallStateListener */
    class MyCallStateListener extends PhoneStateListener {
        MyCallStateListener() {
        }

        public void onCallStateChanged(int i, String str) {
            DataSwitchTile dataSwitchTile = DataSwitchTile.this;
            dataSwitchTile.mCanSwitch = dataSwitchTile.mTelephonyManager.getCallState() == 0;
            DataSwitchTile.this.refreshState();
        }
    }

    public int getMetricsCategory() {
        return 2002;
    }

    public DataSwitchTile(QSHost qSHost) {
        super(qSHost);
        this.mSubscriptionManager = SubscriptionManager.from(qSHost.getContext());
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mPhoneStateListener = new MyCallStateListener();
        this.mController = (NetworkController) Dependency.get(NetworkController.class);
        C10361 r0 = new SystemSetting(this.mContext, null, "esport_mode_enabled", true) {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int i, boolean z) {
                String access$000 = DataSwitchTile.this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("handleValueChanged: ESPORT_MODE_STATUS=");
                sb.append(i);
                Log.d(access$000, sb.toString());
                DataSwitchTile.this.refreshState();
            }
        };
        this.mEsportModeSetting = r0;
    }

    public boolean isAvailable() {
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        String str = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("phoneCount: ");
        sb.append(phoneCount);
        Log.d(str, sb.toString());
        return phoneCount >= 2;
    }

    public BooleanState newTileState() {
        return new BooleanState();
    }

    public void handleSetListening(boolean z) {
        this.mEsportModeSetting.setListening(z);
        if (z) {
            if (!this.mRegistered) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
                this.mContext.registerReceiver(this.mSimReceiver, intentFilter);
                this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
                this.mController.addCallback(this.mSignalCallback);
                this.mRegistered = true;
            }
            refreshState();
        } else if (this.mRegistered) {
            this.mContext.unregisterReceiver(this.mSimReceiver);
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
            this.mController.removeCallback(this.mSignalCallback);
            this.mRegistered = false;
        }
    }

    private void updateSimCount() {
        String str = SystemProperties.get("gsm.sim.state");
        String str2 = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("DataSwitchTile:updateSimCount:simState=");
        sb.append(str);
        Log.d(str2, sb.toString());
        this.mSimCount = 0;
        try {
            String[] split = TextUtils.split(str, ",");
            for (int i = 0; i < split.length; i++) {
                if (!split[i].isEmpty() && !split[i].equalsIgnoreCase("ABSENT")) {
                    if (!split[i].equalsIgnoreCase("NOT_READY")) {
                        this.mSimCount++;
                    }
                }
            }
        } catch (Exception unused) {
            Log.e(this.TAG, "Error to parse sim state");
        }
        String str3 = this.TAG;
        StringBuilder sb2 = new StringBuilder();
        sb2.append("DataSwitchTile:updateSimCount:mSimCount=");
        sb2.append(this.mSimCount);
        Log.d(str3, sb2.toString());
    }

    private void setDefaultDataSimIndex(int i) {
        try {
            if (this.mExtTelephony == null) {
                this.mExtTelephony = Stub.asInterface(ServiceManager.getService("extphone"));
            }
            String str = this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("oemDdsSwitch:phoneId=");
            sb.append(i);
            Log.d(str, sb.toString());
            Method declaredMethod = this.mExtTelephony.getClass().getDeclaredMethod("oemDdsSwitch", new Class[]{Integer.TYPE});
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(this.mExtTelephony, new Object[]{Integer.valueOf(i)});
        } catch (Exception e) {
            Log.d(this.TAG, "setDefaultDataSimId", e);
            Log.d(this.TAG, "clear ext telephony service ref");
            this.mExtTelephony = null;
        }
    }

    public void handleClick() {
        if (this.mEsportModeSetting.getValue() != 1) {
            if (!this.mCanSwitch) {
                String str = this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Call state=");
                sb.append(this.mTelephonyManager.getCallState());
                Log.d(str, sb.toString());
            } else if (this.mVirtualSimExist) {
                Log.d(this.TAG, "virtual sim exist. ignore click.");
            } else {
                int i = this.mSimCount;
                if (i == 0) {
                    Log.d(this.TAG, "handleClick:no sim card");
                    Context context = this.mContext;
                    SysUIToast.makeText(context, (CharSequence) context.getString(R$string.quick_settings_data_switch_toast_0), 1).show();
                } else if (i == 1) {
                    Log.d(this.TAG, "handleClick:only one sim card");
                    Context context2 = this.mContext;
                    SysUIToast.makeText(context2, (CharSequence) context2.getString(R$string.quick_settings_data_switch_toast_1), 1).show();
                } else {
                    AsyncTask.execute(new Runnable() {
                        public final void run() {
                            DataSwitchTile.this.lambda$handleClick$0$DataSwitchTile();
                        }
                    });
                }
            }
        }
    }

    public /* synthetic */ void lambda$handleClick$0$DataSwitchTile() {
        setDefaultDataSimIndex(1 - this.mSubscriptionManager.getDefaultDataPhoneId());
        refreshState();
    }

    public Intent getLongClickIntent() {
        return DATA_SWITCH_SETTINGS;
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R$string.quick_settings_data_switch_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(BooleanState booleanState, Object obj) {
        boolean z;
        int i = 1;
        if (obj == null) {
            int defaultDataPhoneId = this.mSubscriptionManager.getDefaultDataPhoneId();
            String str = this.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("default data phone id=");
            sb.append(defaultDataPhoneId);
            Log.d(str, sb.toString());
            z = defaultDataPhoneId == 0;
        } else {
            z = ((Boolean) obj).booleanValue();
        }
        updateSimCount();
        int i2 = this.mSimCount;
        if (i2 == 1) {
            booleanState.icon = ResourceIcon.get(z ? R$drawable.ic_qs_data_switch_1_disable : R$drawable.ic_qs_data_switch_2_disable);
            booleanState.value = false;
        } else if (i2 != 2) {
            booleanState.icon = ResourceIcon.get(R$drawable.ic_qs_data_switch_1_disable);
            booleanState.value = false;
        } else {
            booleanState.icon = ResourceIcon.get(z ? R$drawable.ic_qs_data_switch_1 : R$drawable.ic_qs_data_switch_2);
            booleanState.value = true;
        }
        if (this.mEsportModeSetting.getValue() == 1) {
            Log.d(this.TAG, "E-Sport, set to unavailable.");
            booleanState.state = 0;
        } else if (this.mSimCount < 2) {
            booleanState.state = 0;
        } else if (this.mVirtualSimExist) {
            booleanState.state = 0;
            Log.d(this.TAG, "virtual sim exist, set to unavailable.");
        } else if (!this.mCanSwitch) {
            booleanState.state = 0;
            Log.d(this.TAG, "call state isn't idle, set to unavailable.");
        } else {
            if (booleanState.value) {
                i = 2;
            }
            booleanState.state = i;
        }
        if (z) {
            booleanState.contentDescription = this.mContext.getString(R$string.accessibility_quick_settings_data_switch_changed_1);
        } else {
            booleanState.contentDescription = this.mContext.getString(R$string.accessibility_quick_settings_data_switch_changed_2);
        }
        booleanState.label = this.mContext.getString(R$string.quick_settings_data_switch_label);
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((BooleanState) this.mState).value) {
            return this.mContext.getString(R$string.accessibility_quick_settings_data_switch_changed_1);
        }
        return this.mContext.getString(R$string.accessibility_quick_settings_data_switch_changed_2);
    }
}

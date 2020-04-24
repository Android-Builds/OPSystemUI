package com.android.systemui.usb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.debug.IAdbManager;
import android.debug.IAdbManager.Stub;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.EventLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.Toast;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController.AlertParams;
import com.android.systemui.R$string;
import com.android.systemui.R$style;
import com.oneplus.util.ThemeColorUtils;

public class UsbDebuggingActivity extends AlertActivity implements OnClickListener {
    private CheckBox mAlwaysAllow;
    private UsbDisconnectedReceiver mDisconnectedReceiver;
    private String mKey;

    private class UsbDisconnectedReceiver extends BroadcastReceiver {
        private final Activity mActivity;

        public UsbDisconnectedReceiver(Activity activity) {
            this.mActivity = activity;
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.hardware.usb.action.USB_STATE".equals(intent.getAction()) && !intent.getBooleanExtra("connected", false)) {
                this.mActivity.finish();
            }
        }
    }

    /* JADX WARNING: type inference failed for: r5v0, types: [android.content.DialogInterface$OnClickListener, com.android.internal.app.AlertActivity, com.android.systemui.usb.UsbDebuggingActivity, android.app.Activity] */
    public void onCreate(Bundle bundle) {
        int i;
        Window window = getWindow();
        window.addSystemFlags(524288);
        window.setType(2008);
        UsbDebuggingActivity.super.onCreate(bundle);
        if (SystemProperties.getInt("service.adb.tcp.port", 0) == 0) {
            this.mDisconnectedReceiver = new UsbDisconnectedReceiver(this);
        }
        Intent intent = getIntent();
        String stringExtra = intent.getStringExtra("fingerprints");
        this.mKey = intent.getStringExtra("key");
        if (stringExtra == null || this.mKey == null) {
            finish();
            return;
        }
        AlertParams alertParams = this.mAlertParams;
        alertParams.mTitle = getString(R$string.usb_debugging_title);
        alertParams.mMessage = getString(R$string.usb_debugging_message, new Object[]{stringExtra});
        alertParams.mPositiveButtonText = getString(R$string.usb_debugging_allow);
        alertParams.mNegativeButtonText = getString(17039360);
        alertParams.mPositiveButtonListener = this;
        alertParams.mNegativeButtonListener = this;
        if (ThemeColorUtils.getCurrentTheme() == 1) {
            i = R$style.Oneplus_SystemUI_Dialog_Alert_Dark;
        } else {
            i = R$style.Oneplus_SystemUI_Dialog_Alert_Light;
        }
        alertParams.mContext.setTheme(i);
        View inflate = LayoutInflater.from(alertParams.mContext).inflate(17367090, null);
        this.mAlwaysAllow = (CheckBox) inflate.findViewById(16908722);
        this.mAlwaysAllow.setText(getString(R$string.usb_debugging_always));
        alertParams.mView = inflate;
        setupAlert();
        this.mAlert.getButton(-1).setOnTouchListener($$Lambda$UsbDebuggingActivity$XWtqGCtWBJlTLnAvCSF7AuSg8.INSTANCE);
    }

    static /* synthetic */ boolean lambda$onCreate$0(View view, MotionEvent motionEvent) {
        if ((motionEvent.getFlags() & 1) == 0 && (motionEvent.getFlags() & 2) == 0) {
            return false;
        }
        if (motionEvent.getAction() == 1) {
            EventLog.writeEvent(1397638484, "62187985");
            Toast.makeText(view.getContext(), R$string.touch_filtered_warning, 0).show();
        }
        return true;
    }

    public void onWindowAttributesChanged(LayoutParams layoutParams) {
        UsbDebuggingActivity.super.onWindowAttributesChanged(layoutParams);
    }

    public void onStart() {
        UsbDebuggingActivity.super.onStart();
        registerReceiver(this.mDisconnectedReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        UsbDisconnectedReceiver usbDisconnectedReceiver = this.mDisconnectedReceiver;
        if (usbDisconnectedReceiver != null) {
            unregisterReceiver(usbDisconnectedReceiver);
        }
        UsbDebuggingActivity.super.onStop();
    }

    public void onClick(DialogInterface dialogInterface, int i) {
        boolean z = true;
        boolean z2 = i == -1;
        if (!z2 || !this.mAlwaysAllow.isChecked()) {
            z = false;
        }
        try {
            IAdbManager asInterface = Stub.asInterface(ServiceManager.getService("adb"));
            if (z2) {
                asInterface.allowDebugging(z, this.mKey);
            } else {
                asInterface.denyDebugging();
            }
        } catch (Exception e) {
            Log.e("UsbDebuggingActivity", "Unable to notify Usb service", e);
        }
        finish();
    }
}

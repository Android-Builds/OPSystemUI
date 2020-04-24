package com.android.systemui.volume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.media.AudioManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.KeyEvent;
import com.android.systemui.statusbar.phone.SystemUIDialog;

public abstract class SafetyWarningDialog extends SystemUIDialog implements OnDismissListener, OnClickListener {
    /* access modifiers changed from: private */
    public static final String TAG = Util.logTag(SafetyWarningDialog.class);
    private final AudioManager mAudioManager;
    private final Context mContext;
    private boolean mDisableOnVolumeUp;
    private boolean mNewVolumeUp;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                if (C1684D.BUG) {
                    Log.d(SafetyWarningDialog.TAG, "Received ACTION_CLOSE_SYSTEM_DIALOGS");
                }
                SafetyWarningDialog.this.cancel();
                SafetyWarningDialog.this.cleanUp();
            }
        }
    };
    private long mShowTime;

    /* access modifiers changed from: protected */
    public abstract void cleanUp();

    public SafetyWarningDialog(Context context, AudioManager audioManager) {
        super(context);
        Log.i(TAG, "SafetyWarningDialog init");
        this.mContext = context;
        this.mAudioManager = audioManager;
        try {
            this.mDisableOnVolumeUp = this.mContext.getResources().getBoolean(17891508);
        } catch (NotFoundException unused) {
            this.mDisableOnVolumeUp = true;
        }
        getWindow().setType(2010);
        setShowForAllUsers(true);
        setMessage(this.mContext.getString(17040973));
        setButton(-1, this.mContext.getString(17039379), this);
        setButton(-2, this.mContext.getString(17039369), null);
        setOnDismissListener(this);
        context.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (this.mDisableOnVolumeUp && i == 24 && keyEvent.getRepeatCount() == 0) {
            this.mNewVolumeUp = true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (i == 24 && this.mNewVolumeUp && System.currentTimeMillis() - this.mShowTime > 1000) {
            boolean isEUVersion = isEUVersion();
            if (C1684D.BUG) {
                Log.d(TAG, "Confirmed warning via VOLUME_UP");
            }
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("SafetyWarningDialog onKeyUp:");
            sb.append(System.currentTimeMillis());
            sb.append(" mShowTime:");
            sb.append(this.mShowTime);
            sb.append(" KEY_CONFIRM_ALLOWED_AFTER:");
            sb.append(1000);
            sb.append("isEUVersion:");
            sb.append(isEUVersion);
            Log.i(str, sb.toString());
            if (!isEUVersion) {
                this.mAudioManager.disableSafeMediaVolume();
            }
            dismiss();
        }
        return super.onKeyUp(i, keyEvent);
    }

    public void onClick(DialogInterface dialogInterface, int i) {
        Log.i(TAG, "SafetyWarningDialog onClick");
        this.mAudioManager.disableSafeMediaVolume();
    }

    /* access modifiers changed from: protected */
    public void onStart() {
        super.onStart();
        this.mShowTime = System.currentTimeMillis();
    }

    public void onDismiss(DialogInterface dialogInterface) {
        Log.i(TAG, "SafetyWarningDialog onDismiss");
        this.mContext.unregisterReceiver(this.mReceiver);
        cleanUp();
    }

    public static boolean isEUVersion() {
        return "true".equals(SystemProperties.get("ro.build.eu", "false"));
    }
}

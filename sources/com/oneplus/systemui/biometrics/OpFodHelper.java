package com.oneplus.systemui.biometrics;

import android.content.Context;
import android.hardware.fingerprint.IFingerprintService;
import android.hardware.fingerprint.IFingerprintService.Stub;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import java.util.ArrayList;
import java.util.Iterator;

public class OpFodHelper {
    private static OpFodHelper sInstance;
    private boolean mActionEnroll;
    private boolean mBiometricPromptVisible;
    private Context mContext;
    private int mCookie;
    private String mCurrentClient;
    private FingerprintState mFingerprintState = FingerprintState.STOP;
    private String mLastClient;
    private ArrayList<OnFingerprintStateChangeListener> mOnFingerprintStateChangeListeners = new ArrayList<>();
    private IFingerprintService mService;
    private KeyguardUpdateMonitor mUpdateMonitor;

    enum FingerprintState {
        RUNNING,
        STOP,
        SUSPEND,
        LOCKOUT
    }

    public interface OnFingerprintStateChangeListener {
        void onFingerprintStateChanged();
    }

    private OpFodHelper(Context context) {
        this.mContext = context;
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mService = Stub.asInterface(ServiceManager.getService("fingerprint"));
    }

    public static void init(Context context) {
        synchronized (OpFodHelper.class) {
            if (sInstance == null) {
                sInstance = new OpFodHelper(context);
            } else {
                Log.w("OpFodHelper", "already init.");
            }
        }
    }

    public static OpFodHelper getInstance() {
        OpFodHelper opFodHelper = sInstance;
        if (opFodHelper != null) {
            return opFodHelper;
        }
        String str = "not initial yet, call init before.";
        Log.e("OpFodHelper", str);
        throw new RuntimeException(str);
    }

    public static boolean isSystemUI(String str) {
        return !TextUtils.isEmpty(str) && "com.android.systemui".equals(str);
    }

    public static boolean isForceShow(String str) {
        return !TextUtils.isEmpty(str) && "forceShow-keyguard".equals(str);
    }

    public static boolean isAppLocker(String str) {
        return !TextUtils.isEmpty(str) && "com.oneplus.applocker".equals(str);
    }

    public static boolean isSettings(String str) {
        return !TextUtils.isEmpty(str) && "com.android.settings".equals(str);
    }

    public static boolean isFileManager(String str) {
        return !TextUtils.isEmpty(str) && "com.oneplus.filemanager".equals(str);
    }

    public static boolean isKeyguard(String str) {
        return isSystemUI(str) || isForceShow(str);
    }

    public void addFingerprintStateChangeListener(OnFingerprintStateChangeListener onFingerprintStateChangeListener) {
        synchronized (this.mOnFingerprintStateChangeListeners) {
            if (!this.mOnFingerprintStateChangeListeners.contains(onFingerprintStateChangeListener)) {
                this.mOnFingerprintStateChangeListeners.add(onFingerprintStateChangeListener);
            }
        }
    }

    public void handleQSExpandChanged(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("handleQSExpandChanged ");
        sb.append(z);
        String str = "OpFodHelper";
        Log.d(str, sb.toString());
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                String authenticatedPackage = iFingerprintService.getAuthenticatedPackage();
                if (TextUtils.isEmpty(authenticatedPackage)) {
                    Log.d(str, "empty client, do not handle it.");
                } else if (isKeyguard(authenticatedPackage)) {
                    Log.d(str, "keyguard client. do not handle it.");
                } else {
                    this.mService.updateStatus(z ? 12 : 11);
                }
            } catch (RemoteException e) {
                Log.e(str, "updateStatus occur remote exception", e);
            }
        } else {
            Log.d(str, "handleQSExpandChanged null pointer");
        }
    }

    public void updateOwner(Bundle bundle) {
        boolean z;
        boolean z2;
        boolean z3;
        int i;
        String str;
        boolean z4 = false;
        if (bundle != null) {
            str = bundle.getString("key_fingerprint_package_name", "");
            i = bundle.getInt("key_cookie", 0);
            z3 = bundle.getBoolean("key_resume", false);
            z2 = bundle.getBoolean("key_suspend", false);
            z = bundle.getBoolean("key_enroll", false);
        } else {
            str = null;
            z = false;
            i = 0;
            z3 = false;
            z2 = false;
        }
        this.mLastClient = this.mCurrentClient;
        this.mCurrentClient = str;
        this.mCookie = i;
        this.mBiometricPromptVisible = false;
        this.mActionEnroll = z;
        if (z3 && i != 0) {
            this.mBiometricPromptVisible = true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("updateOwner: current= ");
        sb.append(this.mCurrentClient);
        sb.append(", last= ");
        sb.append(this.mLastClient);
        sb.append(", from BiometricPrompt? ");
        if (i != 0) {
            z4 = true;
        }
        sb.append(z4);
        sb.append(", resume= ");
        sb.append(z3);
        sb.append(", enroll= ");
        sb.append(this.mActionEnroll);
        Log.d("OpFodHelper", sb.toString());
        boolean equals = true ^ TextUtils.equals(str, this.mLastClient);
        if (TextUtils.isEmpty(str)) {
            changeState(FingerprintState.STOP, equals);
        } else if (!this.mUpdateMonitor.isUnlockingWithBiometricAllowed() && !this.mActionEnroll) {
            changeState(FingerprintState.LOCKOUT, equals);
        } else if (z2) {
            changeState(FingerprintState.SUSPEND, equals);
        } else {
            changeState(FingerprintState.RUNNING, equals);
        }
    }

    public boolean updateBiometricPromptReady(int i) {
        if (this.mCookie != i) {
            Log.d("OpFodHelper", "updateBiometricPromptReady: cookie not matched");
            return false;
        }
        this.mBiometricPromptVisible = true;
        return true;
    }

    public boolean isFromBiometricPrompt() {
        return this.mCookie != 0;
    }

    public boolean isBiometricPromptReadyToShow() {
        return isFromBiometricPrompt() && this.mBiometricPromptVisible;
    }

    public String getCurrentOwner() {
        return this.mCurrentClient;
    }

    public String getLastOwner() {
        return this.mLastClient;
    }

    public boolean isEmptyClient() {
        return TextUtils.isEmpty(this.mCurrentClient);
    }

    public boolean isForceShowClient() {
        return isForceShow(this.mCurrentClient);
    }

    public boolean isKeyguardClient() {
        return isKeyguard(this.mCurrentClient);
    }

    public boolean isKeyguardAuthenticating() {
        return isKeyguard(this.mCurrentClient);
    }

    public boolean isKeyguardUnlocked() {
        return isKeyguard(this.mLastClient);
    }

    public boolean isFingerprintDetecting() {
        return this.mFingerprintState == FingerprintState.RUNNING;
    }

    public boolean isFingerprintSuspended() {
        return this.mFingerprintState == FingerprintState.SUSPEND;
    }

    public boolean isDoingEnroll() {
        return this.mActionEnroll;
    }

    public boolean isFingerprintStopped() {
        return this.mFingerprintState == FingerprintState.STOP;
    }

    public boolean isFingerprintLockout() {
        return this.mFingerprintState == FingerprintState.LOCKOUT;
    }

    private void changeState(FingerprintState fingerprintState, boolean z) {
        if (this.mFingerprintState != fingerprintState || z) {
            StringBuilder sb = new StringBuilder();
            sb.append("changeState ( ");
            sb.append(this.mFingerprintState);
            sb.append(" -> ");
            sb.append(fingerprintState);
            sb.append(" ) , force? ");
            sb.append(z);
            Log.d("OpFodHelper", sb.toString());
            this.mFingerprintState = fingerprintState;
            synchronized (this.mOnFingerprintStateChangeListeners) {
                Iterator it = this.mOnFingerprintStateChangeListeners.iterator();
                while (it.hasNext()) {
                    OnFingerprintStateChangeListener onFingerprintStateChangeListener = (OnFingerprintStateChangeListener) it.next();
                    if (onFingerprintStateChangeListener != null) {
                        onFingerprintStateChangeListener.onFingerprintStateChanged();
                    }
                }
            }
        }
    }

    public void changeState(FingerprintState fingerprintState) {
        changeState(fingerprintState, false);
    }
}

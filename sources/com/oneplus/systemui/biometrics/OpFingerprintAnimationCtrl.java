package com.oneplus.systemui.biometrics;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings.System;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$integer;
import com.oneplus.systemui.biometrics.OpFrameAnimationHelper.Callbacks;
import com.oneplus.util.OpUtils;

public class OpFingerprintAnimationCtrl {
    private static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    private long mAnimPostDelayTime = 0;
    private long mAnimPostDelayTimeOnAod = 0;
    private int mAnimationState = 0;
    private Context mContext;
    private int mCurAnimationType = 0;
    private int mDownAnimFrameNum = 0;
    private int mDownAnimStartIndex = 0;
    private OpFrameAnimationHelper mDownAnimationHelper;
    private OpFingerprintAnimationView mDownAnimationView;
    private Handler mHandler = new Handler();
    private boolean mIsInteractive = false;
    private OpFrameAnimationHelper mOnGoingAnimationHelper;
    private OpFingerprintAnimationView mOnGoingAnimationView;
    ContentObserver mSettingsObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean z) {
            super.onChange(z);
            OpFingerprintAnimationCtrl.this.checkAnimationValueValid();
        }

        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            OpFingerprintAnimationCtrl.this.checkAnimationValueValid();
        }
    };
    private int mUpAnimFrameNum = 0;
    private int mUpAnimStartIndex = 0;
    private OpFrameAnimationHelper mUpAnimationHelper;
    private OpFingerprintAnimationView mUpAnimationView;

    /* access modifiers changed from: protected */
    public void checkAnimationValueValid() {
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        String str = "op_custom_unlock_animation_style";
        int intForUser = System.getIntForUser(this.mContext.getContentResolver(), str, 0, currentUser);
        StringBuilder sb = new StringBuilder();
        sb.append(" checkAnimationValueValid: current: ");
        sb.append(this.mCurAnimationType);
        sb.append(" new: ");
        sb.append(intForUser);
        Log.d("FingerprintAnimationCtrl", sb.toString());
        if (!OpUtils.isMCLVersion()) {
            if (intForUser == 3 || intForUser == 10) {
                System.putIntForUser(this.mContext.getContentResolver(), str, this.mCurAnimationType, currentUser);
                return;
            }
        } else if (intForUser == 11 && OpFingerprintAnimationResHelper.getDownEndFrameIndex(this.mContext, this.mCurAnimationType) == 0) {
            System.putIntForUser(this.mContext.getContentResolver(), str, this.mCurAnimationType, currentUser);
            return;
        }
        this.mCurAnimationType = intForUser;
        this.mDownAnimStartIndex = OpFingerprintAnimationResHelper.getDownStartFrameIndex(this.mContext, this.mCurAnimationType);
        this.mDownAnimFrameNum = OpFingerprintAnimationResHelper.getDownPlayFrameNum(this.mContext, this.mCurAnimationType);
        this.mUpAnimStartIndex = OpFingerprintAnimationResHelper.getUpStartFrameIndex(this.mContext, this.mCurAnimationType);
        this.mUpAnimFrameNum = OpFingerprintAnimationResHelper.getUpPlayFrameNum(this.mContext, this.mCurAnimationType);
        if (!(this.mDownAnimationHelper == null && this.mUpAnimationHelper == null)) {
            this.mDownAnimationHelper = null;
            this.mUpAnimationHelper = null;
        }
        updateAnimationRes(this.mIsInteractive);
    }

    OpFingerprintAnimationCtrl(ViewGroup viewGroup, Context context) {
        this.mContext = context;
        this.mDownAnimationView = (OpFingerprintAnimationView) viewGroup.findViewById(R$id.op_fingerprint_animation_view_1);
        this.mUpAnimationView = (OpFingerprintAnimationView) viewGroup.findViewById(R$id.op_fingerprint_animation_view_3);
        try {
            this.mAnimPostDelayTime = (long) this.mContext.getResources().getInteger(R$integer.fingerprint_animation_post_delay_time);
            this.mAnimPostDelayTimeOnAod = (long) this.mContext.getResources().getInteger(R$integer.fingerprint_animation_post_delay_time_on_aod);
        } catch (Exception unused) {
            Log.e("FingerprintAnimationCtrl", "Parse fingerprint animation post delay time error");
            this.mAnimPostDelayTime = 0;
            this.mAnimPostDelayTimeOnAod = 0;
        }
        this.mSettingsObserver.onChange(true);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("op_custom_unlock_animation_style"), true, this.mSettingsObserver, -1);
    }

    public void updateAnimationRes(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append(" updateanimationRes to ");
        sb.append(this.mCurAnimationType);
        sb.append(", isInteractive = ");
        sb.append(z);
        String str = "FingerprintAnimationCtrl";
        Log.d(str, sb.toString());
        this.mIsInteractive = z;
        if (DEBUG) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("mDownAnimationHelper = ");
            sb2.append(this.mDownAnimationHelper);
            sb2.append(", mUpAnimationHelper = ");
            sb2.append(this.mUpAnimationHelper);
            Log.d(str, sb2.toString());
        }
        long j = z ? this.mAnimPostDelayTime : this.mAnimPostDelayTimeOnAod;
        OpFrameAnimationHelper opFrameAnimationHelper = this.mDownAnimationHelper;
        if (opFrameAnimationHelper == null) {
            OpFrameAnimationHelper opFrameAnimationHelper2 = new OpFrameAnimationHelper(this.mDownAnimationView, OpFingerprintAnimationResHelper.getDownAnimationRes(this.mContext, this.mCurAnimationType), j, this.mDownAnimStartIndex, this.mDownAnimFrameNum);
            this.mDownAnimationHelper = opFrameAnimationHelper2;
        } else {
            opFrameAnimationHelper.updateAnimPostDelayTime(j);
        }
        OpFrameAnimationHelper opFrameAnimationHelper3 = this.mUpAnimationHelper;
        if (opFrameAnimationHelper3 == null) {
            OpFrameAnimationHelper opFrameAnimationHelper4 = new OpFrameAnimationHelper(this.mUpAnimationView, OpFingerprintAnimationResHelper.getUpAnimationRes(this.mCurAnimationType), j, this.mUpAnimStartIndex, this.mUpAnimFrameNum);
            this.mUpAnimationHelper = opFrameAnimationHelper4;
            return;
        }
        opFrameAnimationHelper3.updateAnimPostDelayTime(j);
    }

    public void playAnimation(int i) {
        String str = "FingerprintAnimationCtrl";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("playAnimation: type = ");
            sb.append(i);
            sb.append(", current state = ");
            sb.append(this.mAnimationState);
            Log.d(str, sb.toString());
        }
        int i2 = this.mAnimationState;
        if (i == i2) {
            if (DEBUG) {
                Log.d(str, "playAnimation: type no change");
            }
        } else if (i2 != 0 || i == 1) {
            stopAnimation(i);
            if (i == 1) {
                OpFrameAnimationHelper opFrameAnimationHelper = this.mDownAnimationHelper;
                if (opFrameAnimationHelper != null) {
                    opFrameAnimationHelper.start(true);
                    this.mOnGoingAnimationView = this.mDownAnimationView;
                    this.mOnGoingAnimationHelper = this.mDownAnimationHelper;
                }
            } else if (i == 2 && this.mUpAnimationHelper != null) {
                this.mOnGoingAnimationView = this.mUpAnimationView;
                this.mDownAnimationHelper.stop();
                this.mUpAnimationHelper.start(false);
                this.mOnGoingAnimationHelper = this.mUpAnimationHelper;
            }
        } else {
            if (DEBUG) {
                Log.d(str, "playAnimation: type none or not touch down");
            }
        }
    }

    public void stopAnimation(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("stopAnimation: current state = ");
        sb.append(this.mAnimationState);
        sb.append(", mOnGoingAnimationView = ");
        sb.append(this.mOnGoingAnimationView);
        Log.d("FingerprintAnimationCtrl", sb.toString());
        if (this.mAnimationState != 0) {
            OpFingerprintAnimationView opFingerprintAnimationView = this.mOnGoingAnimationView;
            if (opFingerprintAnimationView != null) {
                if (opFingerprintAnimationView != null) {
                    if (opFingerprintAnimationView.equals(this.mDownAnimationView)) {
                        OpFrameAnimationHelper opFrameAnimationHelper = this.mDownAnimationHelper;
                        if (opFrameAnimationHelper != null) {
                            opFrameAnimationHelper.stop();
                            this.mOnGoingAnimationView = null;
                            this.mOnGoingAnimationHelper = null;
                        }
                    }
                    OpFrameAnimationHelper opFrameAnimationHelper2 = this.mUpAnimationHelper;
                    if (opFrameAnimationHelper2 != null) {
                        opFrameAnimationHelper2.stop();
                    }
                    this.mOnGoingAnimationView = null;
                    this.mOnGoingAnimationHelper = null;
                } else {
                    return;
                }
            }
        }
        this.mAnimationState = i;
    }

    public void updateAnimationDelayTime(boolean z) {
        long j = z ? this.mAnimPostDelayTime : this.mAnimPostDelayTimeOnAod;
        OpFrameAnimationHelper opFrameAnimationHelper = this.mDownAnimationHelper;
        if (opFrameAnimationHelper != null) {
            opFrameAnimationHelper.updateAnimPostDelayTime(j);
        }
        OpFrameAnimationHelper opFrameAnimationHelper2 = this.mUpAnimationHelper;
        if (opFrameAnimationHelper2 != null) {
            opFrameAnimationHelper2.updateAnimPostDelayTime(j);
        }
    }

    public void resetState() {
        Log.d("FingerprintAnimationCtrl", "resetState");
        OpFrameAnimationHelper opFrameAnimationHelper = this.mDownAnimationHelper;
        if (opFrameAnimationHelper != null) {
            opFrameAnimationHelper.stop();
            this.mDownAnimationHelper.resetResource();
        }
        OpFrameAnimationHelper opFrameAnimationHelper2 = this.mUpAnimationHelper;
        if (opFrameAnimationHelper2 != null) {
            opFrameAnimationHelper2.stop();
            this.mUpAnimationHelper.resetResource();
        }
        this.mDownAnimationHelper = null;
        this.mUpAnimationHelper = null;
    }

    public void updateLayoutDimension(boolean z) {
        int dimension = (int) this.mContext.getResources().getDimension(z ? R$dimen.fp_animation_width_2k : R$dimen.fp_animation_width_1080p);
        int dimension2 = (int) this.mContext.getResources().getDimension(z ? R$dimen.fp_animation_height_2k : R$dimen.fp_animation_height_1080p);
        LayoutParams layoutParams = this.mDownAnimationView.getLayoutParams();
        layoutParams.width = dimension;
        layoutParams.height = dimension2;
        this.mDownAnimationView.setLayoutParams(layoutParams);
        LayoutParams layoutParams2 = this.mUpAnimationView.getLayoutParams();
        layoutParams2.width = dimension;
        layoutParams2.height = dimension2;
        this.mUpAnimationView.setLayoutParams(layoutParams2);
    }

    public void waitAnimationFinished(Callbacks callbacks) {
        String str = "FingerprintAnimationCtrl";
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("register fp animation's callback = ");
            sb.append(callbacks);
            sb.append(", animationState = ");
            sb.append(this.mAnimationState);
            Log.i(str, sb.toString());
        }
        int i = this.mAnimationState;
        if (i == 0) {
            Log.e(str, "It shouldn't go into the state.");
            return;
        }
        if (i == 1) {
            OpFrameAnimationHelper opFrameAnimationHelper = this.mDownAnimationHelper;
            if (opFrameAnimationHelper != null) {
                opFrameAnimationHelper.waitAnimationFinished(callbacks);
                return;
            }
        }
        if (this.mAnimationState == 2) {
            OpFrameAnimationHelper opFrameAnimationHelper2 = this.mUpAnimationHelper;
            if (opFrameAnimationHelper2 != null) {
                opFrameAnimationHelper2.waitAnimationFinished(callbacks);
            }
        }
    }

    public boolean isPlayingAnimation() {
        OpFrameAnimationHelper opFrameAnimationHelper = this.mOnGoingAnimationHelper;
        if (opFrameAnimationHelper != null) {
            return opFrameAnimationHelper.isAnimationRunning();
        }
        return false;
    }
}

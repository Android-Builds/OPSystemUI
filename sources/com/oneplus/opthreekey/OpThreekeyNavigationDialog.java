package com.oneplus.opthreekey;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$integer;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.oneplus.opzenmode.OpZenModeController;
import com.oneplus.opzenmode.OpZenModeController.Callback;
import com.oneplus.util.OpUtils;
import com.oneplus.util.ThemeColorUtils;

public class OpThreekeyNavigationDialog implements Callback, ConfigurationListener {
    private static boolean DEBUG = Build.DEBUG_ONEPLUS;
    /* access modifiers changed from: private */
    public static String TAG = "OpThreekeyNavigationDialog";
    private static OpThreekeyNavigationDialog mOpThreekeyNavigationDialog;
    private LayoutInflater layoutInflater;
    private int mAccentColor = 0;
    private final OnClickListener mClickThreeKeyNavigationGotItTextButton = new OnClickListener() {
        public void onClick(View view) {
            OpThreekeyNavigationDialog opThreekeyNavigationDialog = OpThreekeyNavigationDialog.this;
            opThreekeyNavigationDialog.dismiss(opThreekeyNavigationDialog.mViewContainer);
            OpThreekeyNavigationDialog.this.mShowingType = 0;
            OpThreekeyNavigationDialog.this.setFinished("op_threekey_navigation_completed", 1);
        }
    };
    private final OnClickListener mClickThreeKeyNavigationNextTextButton = new OnClickListener() {
        public void onClick(View view) {
            Log.i(OpThreekeyNavigationDialog.TAG, "onClickNextButton");
            OpThreekeyNavigationDialog.this.change();
        }
    };
    private Context mContext;
    private int mDensity;
    /* access modifiers changed from: private */
    public final C1974H mHandler = new C1974H();
    private LinearLayout[] mInnerVirtualThreekeyView;
    /* access modifiers changed from: private */
    public View mMainView;
    KeyguardUpdateMonitorCallback mMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onUserSwitching(int i) {
            String access$100 = OpThreekeyNavigationDialog.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onUserSwitching / userId:");
            sb.append(i);
            Log.i(access$100, sb.toString());
            synchronized (this) {
            }
        }
    };
    private OpZenModeController mOpZenModeController;
    OrientationEventListener mOrientationListener;
    /* access modifiers changed from: private */
    public int mOrientationType = 0;
    private int mParentStatus = -1;
    /* access modifiers changed from: private */
    public View mSecondView;
    /* access modifiers changed from: private */
    public boolean mShowing = false;
    /* access modifiers changed from: private */
    public int mShowingType = 0;
    private int mTextSize = 0;
    private int mThemeBgColor = 0;
    private int mThemeColorMode = 0;
    private int mThemeIconColor = 0;
    private int mThemeTextColor = 0;
    private TextView mThreeKeyNavigationNextGotItButton;
    private TextView mThreeKeyNavigationNextTextButton;
    private ImageView mThreeKeyNavigationTriangle;
    ImageView[] mThreeKeyRowIcon;
    TextView[] mThreeKeyRowText;
    private LinearLayout mThreekeyNavigationFullBlueDialog;
    private int mThreekeyType;
    KeyguardUpdateMonitor mUpdateMonitor;
    private final int mUserId = UserHandle.myUserId();
    /* access modifiers changed from: private */
    public LinearLayout mViewContainer;
    private LayoutParams mWindowLayoutParams;
    private WindowManager mWindowManager;

    /* renamed from: com.oneplus.opthreekey.OpThreekeyNavigationDialog$H */
    private final class C1974H extends Handler {
        public C1974H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    OpThreekeyNavigationDialog.this.showH((View) message.obj);
                    break;
                case 2:
                    OpThreekeyNavigationDialog.this.dismissH((View) message.obj);
                    break;
                case 3:
                    OpThreekeyNavigationDialog.this.stateChange(message.arg1);
                    break;
                case 4:
                    if (OpThreekeyNavigationDialog.this.mShowing) {
                        OpThreekeyNavigationDialog.this.changeH();
                        break;
                    } else {
                        return;
                    }
                case 5:
                    if (OpThreekeyNavigationDialog.this.mShowing) {
                        OpThreekeyNavigationDialog.this.rotateH();
                        break;
                    } else {
                        return;
                    }
                case 6:
                    OpThreekeyNavigationDialog.this.updateTheme(((Boolean) message.obj).booleanValue());
                    break;
                case 7:
                    OpThreekeyNavigationDialog opThreekeyNavigationDialog = OpThreekeyNavigationDialog.this;
                    opThreekeyNavigationDialog.dismissH(opThreekeyNavigationDialog.mMainView);
                    OpThreekeyNavigationDialog opThreekeyNavigationDialog2 = OpThreekeyNavigationDialog.this;
                    opThreekeyNavigationDialog2.dismissH(opThreekeyNavigationDialog2.mSecondView);
                    OpThreekeyNavigationDialog opThreekeyNavigationDialog3 = OpThreekeyNavigationDialog.this;
                    opThreekeyNavigationDialog3.dismissH(opThreekeyNavigationDialog3.mViewContainer);
                    OpThreekeyNavigationDialog.this.mShowing = false;
                    OpThreekeyNavigationDialog.this.inflateView();
                    if (OpThreekeyNavigationDialog.this.mShowingType != 0) {
                        OpThreekeyNavigationDialog.this.show();
                        break;
                    }
                    break;
                case 8:
                    OpThreekeyNavigationDialog.this.setAlphaH((View) message.obj);
                    break;
            }
        }
    }

    public static OpThreekeyNavigationDialog getInstance(Context context) {
        synchronized (OpThreekeyNavigationDialog.class) {
            if (mOpThreekeyNavigationDialog == null) {
                Log.v(TAG, "OpThreekeyNavigationDialog getInstance");
                mOpThreekeyNavigationDialog = new OpThreekeyNavigationDialog(context);
                mOpThreekeyNavigationDialog.init();
            }
        }
        return mOpThreekeyNavigationDialog;
    }

    public OpThreekeyNavigationDialog(Context context) {
        this.mContext = context;
        this.mOpZenModeController = (OpZenModeController) Dependency.get(OpZenModeController.class);
        this.mOrientationListener = new OrientationEventListener(this.mContext, 3) {
            public void onOrientationChanged(int i) {
                Display realDisplay = DisplayManagerGlobal.getInstance().getRealDisplay(0);
                if (realDisplay != null) {
                    int rotation = realDisplay.getRotation();
                    if (rotation != OpThreekeyNavigationDialog.this.mOrientationType) {
                        String access$100 = OpThreekeyNavigationDialog.TAG;
                        StringBuilder sb = new StringBuilder();
                        sb.append("OrientType to ");
                        sb.append(rotation);
                        Log.v(access$100, sb.toString());
                        OpThreekeyNavigationDialog.this.mOrientationType = rotation;
                        if (OpThreekeyNavigationDialog.this.mShowingType != 0) {
                            OpThreekeyNavigationDialog.this.mHandler.obtainMessage(7).sendToTarget();
                        }
                    }
                }
            }
        };
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
    }

    private void init() {
        this.mThreekeyType = this.mContext.getResources().getInteger(R$integer.oneplus_config_threekey_type);
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("init / mThreekeyType:");
        sb.append(this.mThreekeyType);
        Log.v(str, sb.toString());
        if (this.mThreekeyType != 0) {
            inflateView();
            this.mDensity = this.mContext.getResources().getConfiguration().densityDpi;
            this.mOpZenModeController.addCallback(this);
            ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
            this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
            this.mUpdateMonitor.registerCallback(this.mMonitorCallback);
        }
    }

    /* access modifiers changed from: private */
    public void inflateView() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("inflateView mOrientationType == ");
            sb.append(this.mOrientationType);
            Log.i(str, sb.toString());
        }
        this.layoutInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        this.mMainView = this.layoutInflater.inflate(R$layout.op_threekey_navigation_dialog_first, null);
        this.mSecondView = this.layoutInflater.inflate(R$layout.op_threekey_navigation_dialog_second, null);
        this.mViewContainer = new LinearLayout(this.mContext);
        LayoutParams layoutParams = new LayoutParams(-2, -2, 0, 0, 1003, 16777512, -3);
        this.mWindowLayoutParams = layoutParams;
        LayoutParams layoutParams2 = this.mWindowLayoutParams;
        layoutParams2.type = 2038;
        layoutParams2.format = -3;
        layoutParams2.setTitle("OpThreekeyNavigationDialog");
        this.mWindowLayoutParams.layoutInDisplayCutoutMode = 1;
    }

    private void initDialog() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("initDialog / mMainView:");
            sb.append(this.mMainView);
            sb.append(" / mSecondView:");
            sb.append(this.mSecondView);
            Log.i(str, sb.toString());
        }
        View view = this.mMainView;
        if (view != null && this.mSecondView != null) {
            this.mThreekeyNavigationFullBlueDialog = (LinearLayout) view.findViewById(R$id.threekey_navigation_full_blue_dialog);
            this.mThreeKeyNavigationNextTextButton = (TextView) this.mMainView.findViewById(R$id.threekey_navigation_next_text);
            this.mThreeKeyNavigationNextTextButton.setOnClickListener(this.mClickThreeKeyNavigationNextTextButton);
            this.mThreeKeyNavigationTriangle = (ImageView) this.mMainView.findViewById(R$id.threekey_navigation_triangle_icon);
            this.mThreeKeyNavigationNextGotItButton = (TextView) this.mSecondView.findViewById(R$id.threekey_navigation_gotit_text);
            this.mThreeKeyNavigationNextGotItButton.setOnClickListener(this.mClickThreeKeyNavigationGotItTextButton);
            Resources resources = this.mContext.getResources();
            this.mInnerVirtualThreekeyView = new LinearLayout[3];
            this.mInnerVirtualThreekeyView[0] = (LinearLayout) this.mMainView.findViewById(R$id.threekey_vurtual_silent);
            this.mInnerVirtualThreekeyView[1] = (LinearLayout) this.mMainView.findViewById(R$id.threekey_vurtual_vibrate);
            this.mInnerVirtualThreekeyView[2] = (LinearLayout) this.mMainView.findViewById(R$id.threekey_vurtual_ring);
            this.mInnerVirtualThreekeyView[0].findViewById(R$id.threekey_layout).setBackgroundDrawable(resources.getDrawable(R$drawable.dialog_threekey_up_bg));
            this.mInnerVirtualThreekeyView[1].findViewById(R$id.threekey_layout).setBackgroundDrawable(resources.getDrawable(R$drawable.dialog_threekey_middle_bg));
            this.mInnerVirtualThreekeyView[2].findViewById(R$id.threekey_layout).setBackgroundDrawable(resources.getDrawable(R$drawable.dialog_threekey_down_bg));
            this.mThreeKeyRowIcon = new ImageView[3];
            this.mThreeKeyRowText = new TextView[3];
            for (int i = 0; i < 3; i++) {
                this.mThreeKeyRowIcon[i] = (ImageView) this.mInnerVirtualThreekeyView[i].findViewById(R$id.threekey_icon);
                this.mThreeKeyRowText[i] = (TextView) this.mInnerVirtualThreekeyView[i].findViewById(R$id.threekey_text);
            }
            this.mThreeKeyRowIcon[0].setImageResource(R$drawable.op_ic_silence);
            this.mThreeKeyRowIcon[1].setImageResource(R$drawable.op_ic_vibrate);
            this.mThreeKeyRowIcon[2].setImageResource(R$drawable.op_ic_ring);
            this.mThreeKeyRowText[0].setText(resources.getString(R$string.volume_footer_slient));
            this.mThreeKeyRowText[1].setText(resources.getString(R$string.volume_vibrate));
            this.mThreeKeyRowText[2].setText(resources.getString(R$string.volume_footer_ring));
            updateTheme(true);
            int i2 = this.mOrientationType;
            if (i2 == 1) {
                for (int i3 = 0; i3 < 3; i3++) {
                    this.mInnerVirtualThreekeyView[i3].setVisibility(8);
                }
                this.mInnerVirtualThreekeyView[this.mParentStatus - 1].setVisibility(0);
                this.mInnerVirtualThreekeyView[this.mParentStatus - 1].findViewById(R$id.threekey_layout).setBackgroundDrawable(resources.getDrawable(R$drawable.dialog_threekey_middle_bg));
            } else if (i2 == 2) {
            } else {
                if (i2 != 3) {
                    int dimensionPixelSize = resources.getDimensionPixelSize(R$dimen.three_key_up_dialog_position);
                    int dimensionPixelSize2 = resources.getDimensionPixelSize(R$dimen.three_key_middle_dialog_position);
                    int dimensionPixelSize3 = resources.getDimensionPixelSize(R$dimen.three_key_down_dialog_position);
                    int dimensionPixelSize4 = resources.getDimensionPixelSize(R$dimen.op_threekey_dialog_padding);
                    if (dimensionPixelSize == dimensionPixelSize2 && dimensionPixelSize2 == dimensionPixelSize3) {
                        this.mInnerVirtualThreekeyView[(this.mParentStatus - 1) % 3].setVisibility(0);
                        this.mInnerVirtualThreekeyView[((this.mParentStatus - 1) + 1) % 3].setVisibility(8);
                        this.mInnerVirtualThreekeyView[((this.mParentStatus - 1) + 2) % 3].setVisibility(8);
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2);
                        layoutParams.setMargins(0, resources.getDimensionPixelSize(R$dimen.op_threekey_dialog_triangle_short) - dimensionPixelSize4, 0, 0);
                        this.mThreeKeyNavigationTriangle.setLayoutParams(layoutParams);
                    } else {
                        setAlpha(this.mInnerVirtualThreekeyView[(this.mParentStatus - 1) % 3].findViewById(R$id.threekey_layout), 1000);
                        this.mInnerVirtualThreekeyView[((this.mParentStatus - 1) + 1) % 3].findViewById(R$id.threekey_layout).setAlpha(0.3f);
                        this.mInnerVirtualThreekeyView[((this.mParentStatus - 1) + 2) % 3].findViewById(R$id.threekey_layout).setAlpha(0.3f);
                        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(-2, -2);
                        layoutParams2.setMargins(0, resources.getDimensionPixelSize(R$dimen.op_threekey_dialog_triangle_long) - (dimensionPixelSize4 * 2), 0, 0);
                        this.mThreeKeyNavigationTriangle.setLayoutParams(layoutParams2);
                    }
                    int dimensionPixelSize5 = resources.getDimensionPixelSize(17105422);
                    LinearLayout.LayoutParams layoutParams3 = (LinearLayout.LayoutParams) this.mThreekeyNavigationFullBlueDialog.getLayoutParams();
                    layoutParams3.topMargin = dimensionPixelSize5;
                    this.mThreekeyNavigationFullBlueDialog.setLayoutParams(layoutParams3);
                    FrameLayout.LayoutParams layoutParams4 = new FrameLayout.LayoutParams(-2, -2);
                    int i4 = -dimensionPixelSize4;
                    layoutParams4.setMargins(i4, (dimensionPixelSize - dimensionPixelSize4) + dimensionPixelSize5, 0, 0);
                    this.mInnerVirtualThreekeyView[0].setLayoutParams(layoutParams4);
                    FrameLayout.LayoutParams layoutParams5 = new FrameLayout.LayoutParams(-2, -2);
                    layoutParams5.setMargins(i4, (dimensionPixelSize2 - dimensionPixelSize4) + dimensionPixelSize5, 0, 0);
                    this.mInnerVirtualThreekeyView[1].setLayoutParams(layoutParams5);
                    FrameLayout.LayoutParams layoutParams6 = new FrameLayout.LayoutParams(-2, -2);
                    layoutParams6.setMargins(i4, (dimensionPixelSize3 - dimensionPixelSize4) + dimensionPixelSize5, 0, 0);
                    this.mInnerVirtualThreekeyView[2].setLayoutParams(layoutParams6);
                    return;
                }
                for (int i5 = 0; i5 < 3; i5++) {
                    this.mInnerVirtualThreekeyView[i5].setVisibility(8);
                }
                this.mInnerVirtualThreekeyView[this.mParentStatus - 1].setVisibility(0);
                this.mInnerVirtualThreekeyView[this.mParentStatus - 1].findViewById(R$id.threekey_layout).setBackgroundDrawable(resources.getDrawable(R$drawable.dialog_threekey_middle_bg));
            }
        }
    }

    /* access modifiers changed from: private */
    public void show() {
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("show mShowingType:");
        sb.append(this.mShowingType);
        Log.i(str, sb.toString());
        Display defaultDisplay = this.mWindowManager.getDefaultDisplay();
        if (defaultDisplay != null) {
            int rotation = defaultDisplay.getRotation();
            if (rotation != this.mOrientationType) {
                this.mOrientationType = rotation;
            }
        }
        int i = this.mShowingType;
        if (i == 1 || i == 0) {
            int i2 = R$layout.op_threekey_navigation_dialog_first;
            int i3 = this.mOrientationType;
            if (i3 == 1) {
                i2 = R$layout.op_threekey_navigation_dialog_first_left_land;
            } else if (i3 != 2 && i3 == 3) {
                i2 = R$layout.op_threekey_navigation_dialog_first_right_land;
            }
            this.mMainView = this.layoutInflater.inflate(i2, null);
            this.mShowing = true;
            this.mHandler.obtainMessage(1, this.mMainView).sendToTarget();
        } else if (i == 2) {
            this.mSecondView = this.layoutInflater.inflate(R$layout.op_threekey_navigation_dialog_second, null);
            this.mShowing = true;
            this.mHandler.obtainMessage(1, this.mSecondView).sendToTarget();
        }
        registerOrientationListener(true);
    }

    /* access modifiers changed from: private */
    public void dismiss(View view) {
        this.mHandler.obtainMessage(2, view).sendToTarget();
    }

    /* access modifiers changed from: private */
    public void change() {
        this.mHandler.obtainMessage(4).sendToTarget();
    }

    private void registerOrientationListener(boolean z) {
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("registerOrientationListener:");
        sb.append(z);
        Log.v(str, sb.toString());
        OrientationEventListener orientationEventListener = this.mOrientationListener;
        if (orientationEventListener != null) {
            if (!orientationEventListener.canDetectOrientation() || !z) {
                Log.v(TAG, "Cannot detect orientation");
                this.mOrientationListener.disable();
            } else {
                Log.v(TAG, "Can detect orientation");
                this.mOrientationListener.enable();
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0062, code lost:
        if (r0 != false) goto L_0x0064;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0068, code lost:
        if (r0 != false) goto L_0x0072;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x006b, code lost:
        if (r0 != false) goto L_0x0071;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x006e, code lost:
        if (r0 != false) goto L_0x0066;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0071, code lost:
        r4 = 83;
     */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x007e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateThreekeyLayout() {
        /*
            r8 = this;
            android.content.Context r0 = r8.mContext
            if (r0 == 0) goto L_0x0087
            android.view.WindowManager$LayoutParams r1 = r8.mWindowLayoutParams
            if (r1 != 0) goto L_0x000a
            goto L_0x0087
        L_0x000a:
            android.content.res.Resources r0 = r0.getResources()
            if (r0 != 0) goto L_0x0011
            return
        L_0x0011:
            boolean r0 = r8.mShowing
            if (r0 != 0) goto L_0x0016
            return
        L_0x0016:
            android.view.WindowManager$LayoutParams r0 = r8.mWindowLayoutParams
            int r0 = r0.gravity
            boolean r0 = DEBUG
            if (r0 == 0) goto L_0x0037
            java.lang.String r0 = TAG
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "updateThreekeyLayout() / mShowingType == "
            r1.append(r2)
            int r2 = r8.mShowingType
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.i(r0, r1)
        L_0x0037:
            int r0 = r8.mShowingType
            r1 = 2
            if (r0 != r1) goto L_0x004a
            r0 = 49
            android.view.WindowManager$LayoutParams r1 = r8.mWindowLayoutParams
            r1.gravity = r0
            android.view.WindowManager r0 = r8.mWindowManager
            android.widget.LinearLayout r8 = r8.mViewContainer
            r0.updateViewLayout(r8, r1)
            return
        L_0x004a:
            r0 = 0
            int r2 = r8.mThreekeyType
            r3 = 1
            if (r2 != 0) goto L_0x0051
            r0 = r3
        L_0x0051:
            int r2 = r8.mOrientationType
            r4 = 85
            r5 = 83
            r6 = 53
            r7 = 51
            if (r2 == r3) goto L_0x006e
            if (r2 == r1) goto L_0x006b
            r1 = 3
            if (r2 == r1) goto L_0x0068
            if (r0 == 0) goto L_0x0066
        L_0x0064:
            r4 = r6
            goto L_0x0072
        L_0x0066:
            r4 = r7
            goto L_0x0072
        L_0x0068:
            if (r0 == 0) goto L_0x0064
            goto L_0x0072
        L_0x006b:
            if (r0 == 0) goto L_0x0072
            goto L_0x0071
        L_0x006e:
            if (r0 == 0) goto L_0x0071
            goto L_0x0066
        L_0x0071:
            r4 = r5
        L_0x0072:
            android.view.WindowManager$LayoutParams r0 = r8.mWindowLayoutParams
            r0.gravity = r4
            android.widget.LinearLayout r0 = r8.mViewContainer
            android.view.ViewParent r0 = r0.getParent()
            if (r0 == 0) goto L_0x0087
            android.view.WindowManager r0 = r8.mWindowManager
            android.widget.LinearLayout r1 = r8.mViewContainer
            android.view.WindowManager$LayoutParams r8 = r8.mWindowLayoutParams
            r0.updateViewLayout(r1, r8)
        L_0x0087:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.opthreekey.OpThreekeyNavigationDialog.updateThreekeyLayout():void");
    }

    /* access modifiers changed from: private */
    public void showH(View view) {
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("showH view.getParent():");
        sb.append(view.getParent());
        sb.append(", type:");
        sb.append(this.mShowingType);
        Log.d(str, sb.toString());
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(5);
        if (view.getParent() == null) {
            this.mWindowManager.addView(this.mViewContainer, this.mWindowLayoutParams);
            this.mViewContainer.addView(view);
            checkShowPage();
        }
        updateThreekeyLayout();
        initDialog();
    }

    /* access modifiers changed from: private */
    public void dismissH(View view) {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(5);
        if (view != null && view.getParent() != null && view == this.mViewContainer) {
            this.mWindowManager.removeView(view);
        } else if (view != null && view.getParent() != null) {
            LinearLayout linearLayout = this.mViewContainer;
            if (linearLayout != null) {
                linearLayout.removeView(view);
            }
        }
    }

    /* access modifiers changed from: private */
    public void changeH() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(5);
        if (this.mMainView.getParent() != null && this.mSecondView.getParent() == null) {
            this.mViewContainer.addView(this.mSecondView);
            this.mViewContainer.removeView(this.mMainView);
            checkShowPage();
            updateThreekeyLayout();
            initDialog();
        }
    }

    private int checkShowPage() {
        boolean z = this.mMainView.getParent() != null;
        boolean z2 = this.mSecondView.getParent() != null;
        if (z && z2) {
            Log.d(TAG, "mShowingType Invalid");
        } else if (z && !z2) {
            this.mShowingType = 1;
        } else if (!z && z2) {
            this.mShowingType = 2;
        } else if (!z && !z2) {
            this.mShowingType = 0;
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("checkShowPage mShowingType:");
        sb.append(this.mShowingType);
        Log.d(str, sb.toString());
        return this.mShowingType;
    }

    /* access modifiers changed from: private */
    public void stateChange(int i) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("stateChange: ");
            sb.append(i);
            sb.append(", parent: ");
            sb.append(this.mParentStatus);
            Log.d(str, sb.toString());
        }
        if (i != this.mParentStatus) {
            this.mParentStatus = i;
            initDialog();
        }
    }

    /* access modifiers changed from: private */
    public void rotateH() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append(" rotateH /mOrientationType == ");
            sb.append(this.mOrientationType);
            Log.i(str, sb.toString());
        }
        if (this.mShowing) {
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
            this.mHandler.removeMessages(4);
            this.mHandler.removeMessages(5);
            updateThreekeyLayout();
            ViewParent parent = this.mMainView.getParent();
            int i = this.mOrientationType;
            if (i != 1) {
                if (i != 2) {
                    if (i != 3) {
                        if (parent != null) {
                            this.mViewContainer.removeView(this.mMainView);
                            this.mMainView = this.layoutInflater.inflate(R$layout.op_threekey_navigation_dialog_first, null);
                            this.mViewContainer.addView(this.mMainView);
                        }
                    } else if (this.mShowingType == 1 && parent != null) {
                        this.mViewContainer.removeView(this.mMainView);
                        this.mMainView = this.layoutInflater.inflate(R$layout.op_threekey_navigation_dialog_first_right_land, null);
                        this.mViewContainer.addView(this.mMainView);
                    }
                }
            } else if (this.mShowingType == 1 && parent != null) {
                this.mViewContainer.removeView(this.mMainView);
                this.mMainView = this.layoutInflater.inflate(R$layout.op_threekey_navigation_dialog_first_left_land, null);
                this.mViewContainer.addView(this.mMainView);
            }
            initDialog();
        }
    }

    private void setAlpha(View view, int i) {
        C1974H h = this.mHandler;
        h.sendMessageDelayed(h.obtainMessage(8, view), (long) i);
    }

    /* access modifiers changed from: private */
    public void setAlphaH(View view) {
        if (!this.mHandler.hasMessages(8)) {
            view.setAlpha(1.0f);
        }
    }

    public void onThreeKeyStatus(int i) {
        boolean z = true;
        if (System.getIntForUser(this.mContext.getContentResolver(), "op_threekey_navigation_completed", 0, KeyguardUpdateMonitor.getCurrentUser()) != 1) {
            z = false;
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("onThreeKeyStatus() / completed:");
        sb.append(z);
        sb.append(" / userId:");
        sb.append(KeyguardUpdateMonitor.getCurrentUser());
        sb.append(" / UserHandle.myUserId():");
        sb.append(UserHandle.myUserId());
        sb.append(", threekey:");
        sb.append(i);
        sb.append(", mShowing:");
        sb.append(this.mShowing);
        sb.append(", mParentStatus:");
        sb.append(this.mParentStatus);
        Log.i(str, sb.toString());
        if (!z && mOpThreekeyNavigationDialog != null && KeyguardUpdateMonitor.getCurrentUser() == 0) {
            this.mHandler.obtainMessage(3, i, 0).sendToTarget();
            if (this.mParentStatus != -1 && !this.mShowing) {
                show();
            }
        }
    }

    /* access modifiers changed from: private */
    public void setFinished(String str, int i) {
        String str2 = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("setFinished / UserHandle.myUserId():");
        sb.append(UserHandle.myUserId());
        sb.append(" mUpdateMonitor.getCurrentUser():");
        sb.append(KeyguardUpdateMonitor.getCurrentUser());
        Log.i(str2, sb.toString());
        System.putIntForUser(this.mContext.getContentResolver(), str, i, KeyguardUpdateMonitor.getCurrentUser());
    }

    /* access modifiers changed from: private */
    public void updateTheme(boolean z) {
        if (this.mShowing) {
            int themeColor = OpUtils.getThemeColor(this.mContext);
            int color = ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT);
            boolean z2 = (this.mThemeColorMode == themeColor && this.mAccentColor == color) ? false : true;
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("updateTheme change:");
            sb.append(z2);
            sb.append(" force:");
            sb.append(z);
            sb.append(" theme:");
            sb.append(themeColor);
            sb.append(" accentColor:");
            String str2 = "0x%08X";
            sb.append(String.format(str2, new Object[]{Integer.valueOf(color)}));
            sb.append(" mThemeColorMode:");
            sb.append(this.mThemeColorMode);
            sb.append(" mAccentColor:");
            sb.append(String.format(str2, new Object[]{Integer.valueOf(this.mAccentColor)}));
            Log.i(str, sb.toString());
            if (z2 || z) {
                this.mThemeColorMode = themeColor;
                this.mAccentColor = color;
                applyTheme();
            }
        }
    }

    private void applyTheme() {
        Resources resources = this.mContext.getResources();
        if (this.mShowing) {
            if (this.mThemeColorMode != 1) {
                this.mThemeIconColor = this.mAccentColor;
                this.mThemeTextColor = resources.getColor(R$color.oneplus_contorl_text_color_primary_light);
                this.mThemeBgColor = resources.getColor(R$color.oneplus_contorl_bg_color_steppers_light);
            } else {
                this.mThemeIconColor = this.mAccentColor;
                this.mThemeTextColor = resources.getColor(R$color.oneplus_contorl_text_color_primary_dark);
                this.mThemeBgColor = resources.getColor(R$color.oneplus_contorl_bg_color_steppers_dark);
            }
            this.mInnerVirtualThreekeyView[0].findViewById(R$id.threekey_layout).setBackgroundTintList(ColorStateList.valueOf(this.mThemeBgColor));
            this.mInnerVirtualThreekeyView[1].findViewById(R$id.threekey_layout).setBackgroundTintList(ColorStateList.valueOf(this.mThemeBgColor));
            this.mInnerVirtualThreekeyView[2].findViewById(R$id.threekey_layout).setBackgroundTintList(ColorStateList.valueOf(this.mThemeBgColor));
            this.mThreeKeyRowText[0].setTextColor(this.mThemeTextColor);
            this.mThreeKeyRowText[1].setTextColor(this.mThemeTextColor);
            this.mThreeKeyRowText[2].setTextColor(this.mThemeTextColor);
            this.mThreeKeyRowIcon[0].setColorFilter(this.mThemeIconColor);
            this.mThreeKeyRowIcon[1].setColorFilter(this.mThemeIconColor);
            this.mThreeKeyRowIcon[2].setColorFilter(this.mThemeIconColor);
        }
    }

    public void onConfigChanged(Configuration configuration) {
        if (configuration != null) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onConfigChanged, mShowingType:");
            sb.append(this.mShowingType);
            sb.append(" / newConfig.toString():");
            sb.append(configuration.toString());
            Log.i(str, sb.toString());
        }
        this.mHandler.obtainMessage(7).sendToTarget();
    }
}

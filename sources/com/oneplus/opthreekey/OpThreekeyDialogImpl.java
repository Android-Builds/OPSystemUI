package com.oneplus.opthreekey;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$drawable;
import com.android.systemui.R$id;
import com.android.systemui.R$integer;
import com.android.systemui.R$layout;
import com.android.systemui.R$string;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;
import com.oneplus.opthreekey.OpThreekeyDialog.UserActivityListener;
import com.oneplus.opzenmode.OpZenModeController;
import com.oneplus.opzenmode.OpZenModeController.Callback;
import com.oneplus.util.OpUtils;
import com.oneplus.util.ThemeColorUtils;

public class OpThreekeyDialogImpl implements OpThreekeyDialog, Callback, ConfigurationListener {
    private static boolean DEBUG = Build.DEBUG_ONEPLUS;
    private static String TAG = "OpThreekeyDialogImpl";
    private int mAccentColor = 0;
    private Context mContext;
    private final VolumeDialogController mController;
    private int mDensity;
    private Dialog mDialog;
    private int mDialogPosition;
    private ViewGroup mDialogView;
    private final C1969H mHandler = new C1969H();
    private UserActivityListener mListener;
    private OpZenModeController mOpZenModeController;
    OrientationEventListener mOrientationListener;
    private int mOrientationType = 0;
    private boolean mShowing = false;
    private int mThemeBgColor = 0;
    private int mThemeColorMode = 0;
    private int mThemeIconColor = 0;
    private int mThemeTextColor = 0;
    private ImageView mThreeKeyIcon;
    private TextView mThreeKeyText;
    private int mThreeKeystate = -1;
    private Window mWindow;
    private LayoutParams mWindowLayoutParams;
    private int mWindowType;

    /* renamed from: com.oneplus.opthreekey.OpThreekeyDialogImpl$H */
    private final class C1969H extends Handler {
        public C1969H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                OpThreekeyDialogImpl.this.showH(message.arg1);
            } else if (i == 2) {
                OpThreekeyDialogImpl.this.dismissH(message.arg1);
            } else if (i == 3) {
                OpThreekeyDialogImpl.this.rescheduleTimeoutH();
            } else if (i == 4) {
                OpThreekeyDialogImpl.this.stateChange(message.arg1);
            }
        }
    }

    private int computeTimeoutH() {
        return 3000;
    }

    public OpThreekeyDialogImpl(Context context) {
        this.mContext = context;
        this.mOpZenModeController = (OpZenModeController) Dependency.get(OpZenModeController.class);
        this.mOrientationListener = new OrientationEventListener(this.mContext, 3) {
            public void onOrientationChanged(int i) {
                OpThreekeyDialogImpl.this.checkOrientationType();
            }
        };
        this.mController = (VolumeDialogController) Dependency.get(VolumeDialogController.class);
    }

    /* access modifiers changed from: private */
    public void checkOrientationType() {
        Display realDisplay = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        if (realDisplay != null) {
            int rotation = realDisplay.getRotation();
            if (rotation != this.mOrientationType) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Orientype to ");
                sb.append(rotation);
                Log.v(str, sb.toString());
                this.mOrientationType = rotation;
                updateThreekeyLayout();
            }
        }
    }

    public void init(int i, UserActivityListener userActivityListener) {
        Log.v(TAG, "init");
        this.mWindowType = i;
        this.mDensity = this.mContext.getResources().getConfiguration().densityDpi;
        this.mOpZenModeController.addCallback(this);
        this.mListener = userActivityListener;
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        initDialog();
        Log.v(TAG, "in OpThreekeyDialog init mOpThreekeyNavigationDialog.getInstance");
        OpThreekeyNavigationDialog.getInstance(this.mContext);
    }

    public void destroy() {
        Log.v(TAG, "destroy");
        this.mOpZenModeController.removeCallback(this);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    private void initDialog() {
        this.mDialog = new Dialog(this.mContext);
        this.mShowing = false;
        this.mWindow = this.mDialog.getWindow();
        this.mWindow.requestFeature(1);
        this.mWindow.setBackgroundDrawable(new ColorDrawable(0));
        this.mWindow.clearFlags(2);
        this.mWindow.addFlags(17563944);
        this.mDialog.setCanceledOnTouchOutside(false);
        this.mWindowLayoutParams = this.mWindow.getAttributes();
        LayoutParams layoutParams = this.mWindowLayoutParams;
        layoutParams.type = this.mWindowType;
        layoutParams.format = -3;
        layoutParams.setTitle(OpThreekeyDialogImpl.class.getSimpleName());
        LayoutParams layoutParams2 = this.mWindowLayoutParams;
        layoutParams2.gravity = 53;
        layoutParams2.y = this.mDialogPosition;
        this.mWindow.setAttributes(layoutParams2);
        this.mWindow.setSoftInputMode(48);
        this.mDialog.setContentView(R$layout.op_threekey_dialog);
        this.mDialogView = (ViewGroup) this.mDialog.findViewById(R$id.threekey_layout);
        this.mThreeKeyIcon = (ImageView) this.mDialog.findViewById(R$id.threekey_icon);
        this.mThreeKeyText = (TextView) this.mDialog.findViewById(R$id.threekey_text);
        updateTheme(true);
    }

    public void show(int i) {
        this.mHandler.obtainMessage(1, i, 0).sendToTarget();
    }

    private void registerOrientationListener(boolean z) {
        if (!this.mOrientationListener.canDetectOrientation() || !z) {
            Log.v(TAG, "Cannot detect orientation");
            this.mOrientationListener.disable();
            return;
        }
        Log.v(TAG, "Can detect orientation");
        this.mOrientationListener.enable();
    }

    private void updateThreekeyLayout() {
        int i;
        int i2;
        int i3;
        int dimensionPixelSize;
        int dimensionPixelSize2;
        Context context = this.mContext;
        if (context != null) {
            Resources resources = context.getResources();
            if (resources != null) {
                LayoutParams layoutParams = this.mWindowLayoutParams;
                int i4 = layoutParams.y;
                int i5 = layoutParams.x;
                int i6 = layoutParams.gravity;
                int i7 = this.mThreeKeystate;
                int i8 = 0;
                int i9 = 2;
                if (i7 == 1) {
                    i2 = R$drawable.op_ic_silence;
                    i = R$string.volume_footer_slient;
                } else if (i7 == 2) {
                    i2 = R$drawable.op_ic_vibrate;
                    i = R$string.volume_vibrate;
                } else if (i7 != 3) {
                    i2 = 0;
                    i = 0;
                } else {
                    i2 = R$drawable.op_ic_ring;
                    i = R$string.volume_footer_ring;
                }
                boolean z = resources.getInteger(R$integer.oneplus_config_threekey_type) != 1;
                int i10 = this.mOrientationType;
                int i11 = 83;
                if (i10 == 1) {
                    if (z) {
                        i11 = 51;
                    }
                    int dimensionPixelSize3 = resources.getDimensionPixelSize(R$dimen.three_key_up_dialog_position_deep_land);
                    if (z) {
                        dimensionPixelSize3 += resources.getDimensionPixelSize(17105422);
                    }
                    int i12 = this.mThreeKeystate;
                    if (i12 == 1) {
                        i5 = resources.getDimensionPixelSize(R$dimen.three_key_up_dialog_position_l);
                    } else if (i12 == 2) {
                        i5 = resources.getDimensionPixelSize(R$dimen.three_key_middle_dialog_position_l);
                    } else if (i12 == 3) {
                        i5 = resources.getDimensionPixelSize(R$dimen.three_key_down_dialog_position_l);
                    }
                    i8 = R$drawable.dialog_threekey_middle_bg;
                } else if (i10 != 2) {
                    i3 = 53;
                    if (i10 != 3) {
                        if (!z) {
                            i3 = 51;
                        }
                        i5 = resources.getDimensionPixelSize(R$dimen.three_key_up_dialog_position_deep);
                        int i13 = this.mThreeKeystate;
                        if (i13 == 1) {
                            i4 = resources.getDimensionPixelSize(R$dimen.three_key_up_dialog_position) + resources.getDimensionPixelSize(17105422);
                            i8 = R$drawable.dialog_threekey_up_bg;
                            i9 = 1;
                        } else if (i13 == 2) {
                            i4 = resources.getDimensionPixelSize(R$dimen.three_key_middle_dialog_position) + resources.getDimensionPixelSize(17105422);
                            i8 = R$drawable.dialog_threekey_middle_bg;
                        } else if (i13 == 3) {
                            i4 = resources.getDimensionPixelSize(R$dimen.three_key_down_dialog_position) + resources.getDimensionPixelSize(17105422);
                            i8 = R$drawable.dialog_threekey_down_bg;
                            i9 = 3;
                        } else {
                            i9 = 0;
                        }
                    } else {
                        if (z) {
                            i3 = 85;
                        }
                        i4 = resources.getDimensionPixelSize(R$dimen.three_key_up_dialog_position_deep_land);
                        if (!z) {
                            i4 += resources.getDimensionPixelSize(17105422);
                        }
                        int i14 = this.mThreeKeystate;
                        if (i14 == 1) {
                            i5 = resources.getDimensionPixelSize(R$dimen.three_key_up_dialog_position_l);
                        } else if (i14 == 2) {
                            i5 = resources.getDimensionPixelSize(R$dimen.three_key_middle_dialog_position_l);
                        } else if (i14 == 3) {
                            i5 = resources.getDimensionPixelSize(R$dimen.three_key_down_dialog_position_l);
                        }
                        i8 = R$drawable.dialog_threekey_middle_bg;
                    }
                } else {
                    if (!z) {
                        i11 = 85;
                    }
                    i5 = resources.getDimensionPixelSize(R$dimen.three_key_up_dialog_position_deep);
                    int i15 = this.mThreeKeystate;
                    if (i15 == 1) {
                        dimensionPixelSize = resources.getDimensionPixelSize(R$dimen.three_key_up_dialog_position);
                        dimensionPixelSize2 = resources.getDimensionPixelSize(17105422);
                    } else if (i15 == 2) {
                        dimensionPixelSize = resources.getDimensionPixelSize(R$dimen.three_key_middle_dialog_position);
                        dimensionPixelSize2 = resources.getDimensionPixelSize(17105422);
                    } else {
                        if (i15 == 3) {
                            dimensionPixelSize = resources.getDimensionPixelSize(R$dimen.three_key_down_dialog_position);
                            dimensionPixelSize2 = resources.getDimensionPixelSize(17105422);
                        }
                        i8 = R$drawable.dialog_threekey_middle_bg;
                    }
                    i4 = dimensionPixelSize + dimensionPixelSize2;
                    i8 = R$drawable.dialog_threekey_middle_bg;
                }
                if (this.mThreeKeystate != -1) {
                    ImageView imageView = this.mThreeKeyIcon;
                    if (imageView != null) {
                        imageView.setImageResource(i2);
                    }
                    if (this.mThreeKeyText != null) {
                        String string = resources.getString(i);
                        if (string != null && this.mThreeKeyText.length() == string.length()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(string);
                            sb.append(" ");
                            string = sb.toString();
                        }
                        this.mThreeKeyText.setText(string);
                    }
                    ViewGroup viewGroup = this.mDialogView;
                    if (viewGroup != null) {
                        viewGroup.setBackgroundDrawable(getCornerGradientDrawable(i8, z, i9));
                    }
                    this.mDialogPosition = i4;
                }
                int dimensionPixelSize4 = resources.getDimensionPixelSize(R$dimen.op_threekey_dialog_padding);
                LayoutParams layoutParams2 = this.mWindowLayoutParams;
                layoutParams2.gravity = i3;
                layoutParams2.y = i4 - dimensionPixelSize4;
                layoutParams2.x = i5 - dimensionPixelSize4;
                this.mWindow.setAttributes(layoutParams2);
                rescheduleTimeoutH();
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("updateThreekeyLayout mThreeKeystate:");
                    sb2.append(this.mThreeKeystate);
                    Log.d(str, sb2.toString());
                }
            }
        }
    }

    private GradientDrawable getCornerGradientDrawable(int i, boolean z, int i2) {
        GradientDrawable gradientDrawable = (GradientDrawable) ((LayerDrawable) this.mContext.getResources().getDrawable(i)).getDrawable(0);
        gradientDrawable.setCornerRadii(getTheekeyCornerRadii(this.mContext, z, i2));
        return gradientDrawable;
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0069  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private float getCuttingEdgeCornerRadiusValue(android.content.Context r6) {
        /*
            r5 = this;
            android.content.res.Resources r5 = r6.getResources()
            int r0 = com.android.systemui.R$dimen.shape_corner_radius
            int r5 = r5.getDimensionPixelSize(r0)
            float r5 = (float) r5
            android.content.res.Resources r0 = r6.getResources()
            int r1 = com.android.systemui.R$dimen.shape_corner_radius_for_check_roundedrect
            int r0 = r0.getDimensionPixelSize(r1)
            float r0 = (float) r0
            android.content.res.Resources r1 = r6.getResources()
            int r2 = com.android.systemui.R$dimen.shape_corner_radius_for_check_teardrop
            int r1 = r1.getDimensionPixelSize(r2)
            float r1 = (float) r1
            android.content.res.Resources r2 = r6.getResources()
            int r3 = com.android.systemui.R$dimen.shape_corner_radius_for_check_squircle
            int r2 = r2.getDimensionPixelSize(r3)
            float r2 = (float) r2
            android.content.res.Resources r3 = r6.getResources()
            int r4 = com.android.systemui.R$dimen.shape_corner_radius_cutting_edge_circle
            int r3 = r3.getDimensionPixelSize(r4)
            float r3 = (float) r3
            int r0 = (r5 > r0 ? 1 : (r5 == r0 ? 0 : -1))
            if (r0 != 0) goto L_0x0047
            android.content.res.Resources r5 = r6.getResources()
            int r6 = com.android.systemui.R$dimen.shape_corner_radius_cutting_edge_roundedrect
            int r5 = r5.getDimensionPixelSize(r6)
        L_0x0045:
            float r3 = (float) r5
            goto L_0x0065
        L_0x0047:
            int r0 = (r5 > r1 ? 1 : (r5 == r1 ? 0 : -1))
            if (r0 != 0) goto L_0x0056
            android.content.res.Resources r5 = r6.getResources()
            int r6 = com.android.systemui.R$dimen.shape_corner_radius_cutting_edge_teardrop
            int r5 = r5.getDimensionPixelSize(r6)
            goto L_0x0045
        L_0x0056:
            int r5 = (r5 > r2 ? 1 : (r5 == r2 ? 0 : -1))
            if (r5 != 0) goto L_0x0065
            android.content.res.Resources r5 = r6.getResources()
            int r6 = com.android.systemui.R$dimen.shape_corner_radius_cutting_edge_squircle
            int r5 = r5.getDimensionPixelSize(r6)
            goto L_0x0045
        L_0x0065:
            boolean r5 = com.oneplus.util.OpUtils.DEBUG_ONEPLUS
            if (r5 == 0) goto L_0x007f
            java.lang.String r5 = TAG
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r0 = "getCornerRadiusValue, value:"
            r6.append(r0)
            r6.append(r3)
            java.lang.String r6 = r6.toString()
            android.util.Log.i(r5, r6)
        L_0x007f:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.opthreekey.OpThreekeyDialogImpl.getCuttingEdgeCornerRadiusValue(android.content.Context):float");
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0062  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private float getMiddleCuttingEdgeCornerRadiusValue(android.content.Context r6) {
        /*
            r5 = this;
            android.content.res.Resources r5 = r6.getResources()
            int r0 = com.android.systemui.R$dimen.shape_corner_radius
            int r5 = r5.getDimensionPixelSize(r0)
            float r5 = (float) r5
            android.content.res.Resources r0 = r6.getResources()
            int r1 = com.android.systemui.R$dimen.shape_corner_radius_for_check_roundedrect
            int r0 = r0.getDimensionPixelSize(r1)
            float r0 = (float) r0
            android.content.res.Resources r1 = r6.getResources()
            int r2 = com.android.systemui.R$dimen.shape_corner_radius_for_check_teardrop
            int r1 = r1.getDimensionPixelSize(r2)
            float r1 = (float) r1
            android.content.res.Resources r2 = r6.getResources()
            int r3 = com.android.systemui.R$dimen.shape_corner_radius_for_check_squircle
            int r2 = r2.getDimensionPixelSize(r3)
            float r2 = (float) r2
            android.content.res.Resources r3 = r6.getResources()
            int r4 = com.android.systemui.R$dimen.op_threekey_dialog_inner_height
            int r3 = r3.getDimensionPixelSize(r4)
            float r3 = (float) r3
            r4 = 1073741824(0x40000000, float:2.0)
            float r3 = r3 / r4
            int r0 = (r5 > r0 ? 1 : (r5 == r0 ? 0 : -1))
            if (r0 != 0) goto L_0x004a
            android.content.res.Resources r5 = r6.getResources()
            int r6 = com.android.systemui.R$dimen.shape_corner_radius_cutting_edge_roundedrect
            int r5 = r5.getDimensionPixelSize(r6)
        L_0x0048:
            float r3 = (float) r5
            goto L_0x005e
        L_0x004a:
            int r0 = (r5 > r1 ? 1 : (r5 == r1 ? 0 : -1))
            if (r0 != 0) goto L_0x004f
            goto L_0x005e
        L_0x004f:
            int r5 = (r5 > r2 ? 1 : (r5 == r2 ? 0 : -1))
            if (r5 != 0) goto L_0x005e
            android.content.res.Resources r5 = r6.getResources()
            int r6 = com.android.systemui.R$dimen.shape_corner_radius_cutting_edge_squircle
            int r5 = r5.getDimensionPixelSize(r6)
            goto L_0x0048
        L_0x005e:
            boolean r5 = com.oneplus.util.OpUtils.DEBUG_ONEPLUS
            if (r5 == 0) goto L_0x0078
            java.lang.String r5 = TAG
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r0 = "getMiddleCuttingEdgeCornerRadiusValue, value:"
            r6.append(r0)
            r6.append(r3)
            java.lang.String r6 = r6.toString()
            android.util.Log.i(r5, r6)
        L_0x0078:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.oneplus.opthreekey.OpThreekeyDialogImpl.getMiddleCuttingEdgeCornerRadiusValue(android.content.Context):float");
    }

    private float[] getTheekeyCornerRadii(Context context, boolean z, int i) {
        int i2 = i;
        float dimensionPixelSize = ((float) context.getResources().getDimensionPixelSize(R$dimen.op_threekey_dialog_inner_height)) / 2.0f;
        int i3 = 0;
        float[] fArr = {dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize};
        float cuttingEdgeCornerRadiusValue = getCuttingEdgeCornerRadiusValue(context);
        float middleCuttingEdgeCornerRadiusValue = getMiddleCuttingEdgeCornerRadiusValue(context);
        if (OpUtils.DEBUG_ONEPLUS) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("getTheekeyCornerRadii, radius:");
            sb.append(dimensionPixelSize);
            sb.append(" cuttingEdgeCornerRadii:");
            sb.append(cuttingEdgeCornerRadiusValue);
            sb.append(" cuttingEdgeMiddleCornerRadii:");
            sb.append(middleCuttingEdgeCornerRadiusValue);
            Log.i(str, sb.toString());
        }
        if (this.mOrientationType != 0) {
            while (i3 < fArr.length) {
                fArr[i3] = middleCuttingEdgeCornerRadiusValue;
                i3++;
            }
        } else if (z) {
            if (i2 == 1) {
                fArr[4] = cuttingEdgeCornerRadiusValue;
                fArr[5] = cuttingEdgeCornerRadiusValue;
            } else if (i2 == 2) {
                while (i3 < fArr.length) {
                    fArr[i3] = middleCuttingEdgeCornerRadiusValue;
                    i3++;
                }
            } else if (i2 == 3) {
                fArr[2] = cuttingEdgeCornerRadiusValue;
                fArr[3] = cuttingEdgeCornerRadiusValue;
            }
        } else if (i2 == 1) {
            fArr[6] = cuttingEdgeCornerRadiusValue;
            fArr[7] = cuttingEdgeCornerRadiusValue;
        } else if (i2 == 2) {
            while (i3 < fArr.length) {
                fArr[i3] = middleCuttingEdgeCornerRadiusValue;
                i3++;
            }
        } else if (i2 == 3) {
            fArr[0] = cuttingEdgeCornerRadiusValue;
            fArr[1] = cuttingEdgeCornerRadiusValue;
        }
        return fArr;
    }

    public void onThreeKeyStatus(int i) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("onThreeKeyStatus:");
            sb.append(i);
            Log.d(str, sb.toString());
        }
        this.mHandler.obtainMessage(4, i, 0).sendToTarget();
        if (this.mThreeKeystate != -1) {
            show(0);
        }
    }

    /* access modifiers changed from: private */
    public void showH(int i) {
        if (DEBUG) {
            Log.d(TAG, "showH r=");
        }
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        rescheduleTimeoutH();
        if (!this.mShowing) {
            updateTheme(false);
            registerOrientationListener(true);
            checkOrientationType();
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("showH mOrientationType=");
            sb.append(this.mOrientationType);
            Log.d(str, sb.toString());
            this.mShowing = true;
            this.mDialog.show();
            UserActivityListener userActivityListener = this.mListener;
            if (userActivityListener != null) {
                userActivityListener.onThreekeyStateUserActivity();
            }
        }
    }

    /* access modifiers changed from: private */
    public void dismissH(int i) {
        if (DEBUG) {
            Log.d(TAG, "dismissH r=");
        }
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        if (this.mShowing) {
            registerOrientationListener(false);
            this.mShowing = false;
            this.mDialog.dismiss();
        }
    }

    /* access modifiers changed from: private */
    public void stateChange(int i) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("stateChange ");
            sb.append(i);
            Log.d(str, sb.toString());
        }
        if (i != this.mThreeKeystate) {
            this.mThreeKeystate = i;
            updateThreekeyLayout();
            UserActivityListener userActivityListener = this.mListener;
            if (userActivityListener != null) {
                userActivityListener.onThreekeyStateUserActivity();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void rescheduleTimeoutH() {
        this.mHandler.removeMessages(2);
        int computeTimeoutH = computeTimeoutH();
        C1969H h = this.mHandler;
        h.sendMessageDelayed(h.obtainMessage(2, 3, 0), (long) computeTimeoutH);
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("rescheduleTimeout ");
            sb.append(computeTimeoutH);
            Log.d(str, sb.toString());
        }
        UserActivityListener userActivityListener = this.mListener;
        if (userActivityListener != null) {
            userActivityListener.onThreekeyStateUserActivity();
        }
    }

    public void onDensityOrFontScaleChanged() {
        dismissH(1);
        initDialog();
        updateThreekeyLayout();
    }

    public void applyTheme() {
        Resources resources = this.mContext.getResources();
        if (this.mThemeColorMode != 1) {
            this.mThemeIconColor = this.mAccentColor;
            this.mThemeTextColor = resources.getColor(R$color.oneplus_contorl_text_color_primary_light);
            this.mThemeBgColor = resources.getColor(R$color.oneplus_contorl_bg_color_steppers_light);
        } else {
            this.mThemeIconColor = this.mAccentColor;
            this.mThemeTextColor = resources.getColor(R$color.oneplus_contorl_text_color_primary_dark);
            this.mThemeBgColor = resources.getColor(R$color.oneplus_contorl_bg_color_steppers_dark);
        }
        this.mDialogView.setBackgroundTintList(ColorStateList.valueOf(this.mThemeBgColor));
        this.mThreeKeyText.setTextColor(this.mThemeTextColor);
        this.mThreeKeyIcon.setColorFilter(this.mThemeIconColor);
    }

    public void onConfigChanged(Configuration configuration) {
        updateTheme(false);
        updateThreekeyLayout();
    }

    private void updateTheme(boolean z) {
        int themeColor = OpUtils.getThemeColor(this.mContext);
        int color = ThemeColorUtils.getColor(ThemeColorUtils.QS_ACCENT);
        boolean z2 = (this.mThemeColorMode == themeColor && this.mAccentColor == color) ? false : true;
        if (DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("updateTheme change");
            sb.append(z2);
            sb.append(" force:");
            sb.append(z);
            Log.i(str, sb.toString());
        }
        if (z2 || z) {
            this.mThemeColorMode = themeColor;
            this.mAccentColor = color;
            applyTheme();
        }
    }
}

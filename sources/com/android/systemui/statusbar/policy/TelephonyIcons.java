package com.android.systemui.statusbar.policy;

import android.telephony.SignalStrength;
import android.util.Log;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.oneplus.signal.OpSignalIcons;
import com.oneplus.util.OpUtils;
import java.util.HashMap;
import java.util.Map;

class TelephonyIcons {
    static final MobileIconGroup CARRIER_NETWORK_CHANGE;
    static final MobileIconGroup DATA_DISABLED;
    static final boolean DEBUG = Log.isLoggable("TelephonyIcons", 3);

    /* renamed from: E */
    static final MobileIconGroup f100E;
    static final MobileIconGroup FIVE_G;
    static final MobileIconGroup FIVE_G_BASIC;
    static final MobileIconGroup FIVE_G_SA;
    static final MobileIconGroup FIVE_G_UWB;
    static final int FLIGHT_MODE_ICON = R$drawable.stat_sys_airplane_mode;
    static final MobileIconGroup FOUR_G;
    static final MobileIconGroup FOUR_G_LTE;
    static final MobileIconGroup FOUR_G_PLUS;

    /* renamed from: G */
    static final MobileIconGroup f101G;

    /* renamed from: H */
    static final MobileIconGroup f102H;
    static final MobileIconGroup H_PLUS;
    static final int ICON_1X = R$drawable.stat_sys_data_fully_connected_1x;
    static final int ICON_2G = R$drawable.stat_sys_data_fully_connected_2g;
    static final int ICON_3G = R$drawable.stat_sys_data_fully_connected_3g;
    static final int ICON_3G_PLUS = R$drawable.stat_sys_data_fully_connected_3g_plus;
    static final int ICON_4G = R$drawable.stat_sys_data_fully_connected_4g;
    static final int ICON_4G_PLUS = R$drawable.stat_sys_data_fully_connected_4g_plus;
    static final int ICON_5G;
    static final int ICON_5G_BASIC;
    static final int ICON_5G_E = R$drawable.ic_5g_e_mobiledata;
    static final int ICON_5G_PLUS = R$drawable.ic_5g_plus_mobiledata;
    static final int ICON_5G_SA;
    static final int ICON_5G_UWB = R$drawable.stat_sys_data_fully_connected_5g_uwb;
    static final int ICON_DATA_DISABLED = R$drawable.stat_sys_data_disabled;
    static final int ICON_E = R$drawable.stat_sys_data_fully_connected_e;
    static final int ICON_G = R$drawable.stat_sys_data_fully_connected_g;
    static final int ICON_H = R$drawable.stat_sys_data_fully_connected_h;
    static final int ICON_H_PLUS = R$drawable.stat_sys_data_fully_connected_h_plus;
    static final int ICON_LTE = R$drawable.stat_sys_data_fully_connected_lte;
    static final int ICON_LTE_PLUS = R$drawable.stat_sys_data_fully_connected_lte_plus;
    static final Map<String, MobileIconGroup> ICON_NAME_TO_ICON = new HashMap();
    static final MobileIconGroup LTE;
    static final MobileIconGroup LTE_CA_5G_E;
    static final MobileIconGroup LTE_PLUS;
    static final MobileIconGroup NOT_DEFAULT_DATA;
    static final MobileIconGroup NR_5G;
    static final MobileIconGroup NR_5G_PLUS;
    static final int[] ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_ROAMING = (OpUtils.isSupportFiveBar() ? ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_ROAMING_FIVE_BAR : ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_ROAMING_FOUR_BAR);
    static final int[] ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_ROAMING_FIVE_BAR = {R$drawable.stat_sys_signal_0_op_5_bar_roam, R$drawable.stat_sys_signal_1_op_5_bar_roam, R$drawable.stat_sys_signal_2_op_5_bar_roam, R$drawable.stat_sys_signal_3_op_5_bar_roam, R$drawable.stat_sys_signal_4_op_5_bar_roam, R$drawable.stat_sys_signal_5_op_5_bar_roam};
    static final int[] ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_ROAMING_FOUR_BAR = {R$drawable.stat_sys_signal_oneplus_roam_0, R$drawable.stat_sys_signal_oneplus_roam_1, R$drawable.stat_sys_signal_oneplus_roam_2, R$drawable.stat_sys_signal_oneplus_roam_3, R$drawable.stat_sys_signal_oneplus_roam_4};
    static final int[] ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_VIRTUAL = (OpUtils.isSupportFiveBar() ? ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_VIRTUAL_FIVE_BAR : ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_VIRTUAL_FOUR_BAR);
    static final int[] ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_VIRTUAL_FIVE_BAR = {R$drawable.stat_sys_signal_0_op_5_bar_virtual, R$drawable.stat_sys_signal_1_op_5_bar_virtual, R$drawable.stat_sys_signal_2_op_5_bar_virtual, R$drawable.stat_sys_signal_3_op_5_bar_virtual, R$drawable.stat_sys_signal_4_op_5_bar_virtual, R$drawable.stat_sys_signal_5_op_5_bar_virtual};
    static final int[] ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_VIRTUAL_FOUR_BAR = {R$drawable.stat_sys_signal_0_op_4_bar_virtual, R$drawable.stat_sys_signal_1_op_4_bar_virtual, R$drawable.stat_sys_signal_2_op_4_bar_virtual, R$drawable.stat_sys_signal_3_op_4_bar_virtual, R$drawable.stat_sys_signal_4_op_4_bar_virtual};
    static final MobileIconGroup ONE_X;
    static final int SIGNAL_LEVEL_NUM = SignalStrength.NUM_SIGNAL_STRENGTH_BINS;
    static final int STACKED_ICON_1X = R$drawable.stat_sys_data_op_stacked_1x;
    static final int STACKED_ICON_2G = R$drawable.stat_sys_data_op_stacked_2g;
    static final int STACKED_ICON_3G = R$drawable.stat_sys_data_op_stacked_3g;
    static final int STACKED_ICON_4G = R$drawable.stat_sys_data_op_stacked_4g;
    static final int STACKED_ICON_4G_PLUS = R$drawable.stat_sys_data_op_stacked_4g_plus;
    static final int STACKED_ICON_G = R$drawable.stat_sys_data_op_stacked_g;
    static final int STACKED_ICON_LTE = R$drawable.stat_sys_data_op_stacked_lte;
    static final int STACKED_ICON_LTE_PLUS = R$drawable.stat_sys_data_op_stacked_lte_plus;
    static final int STACKED_ICON_ROAM = R$drawable.stat_sys_data_op_stacked_roam;
    static final int[] STACKED_STRENGTH_ICONS = (OpUtils.isSupportFiveBar() ? STACKED_STRENGTH_ICONS_FIVE_BAR : STACKED_STRENGTH_ICONS_FOUR_BAR);
    static final int[] STACKED_STRENGTH_ICONS_FIVE_BAR = {R$drawable.stat_sys_signal_0_op_5_bar_stacked, R$drawable.stat_sys_signal_1_op_5_bar_stacked, R$drawable.stat_sys_signal_2_op_5_bar_stacked, R$drawable.stat_sys_signal_3_op_5_bar_stacked, R$drawable.stat_sys_signal_4_op_5_bar_stacked, R$drawable.stat_sys_signal_5_op_5_bar_stacked};
    static final int[] STACKED_STRENGTH_ICONS_FOUR_BAR = {R$drawable.stat_sys_signal_0_op_4_bar_stacked, R$drawable.stat_sys_signal_1_op_4_bar_stacked, R$drawable.stat_sys_signal_2_op_4_bar_stacked, R$drawable.stat_sys_signal_3_op_4_bar_stacked, R$drawable.stat_sys_signal_4_op_4_bar_stacked};
    static final int TELEPHONY_NO_NETWORK = R$drawable.stat_sys_signal_null;
    static final int[][] TELEPHONY_SIGNAL_STRENGTH = (OpUtils.isSupportFiveBar() ? TELEPHONY_SIGNAL_STRENGTH_FIVE_BAR : TELEPHONY_SIGNAL_STRENGTH_FOUR_BAR);
    static final int[][] TELEPHONY_SIGNAL_STRENGTH_FIVE_BAR = {new int[]{R$drawable.stat_sys_signal_0_op_5_bar, R$drawable.stat_sys_signal_1_op_5_bar, R$drawable.stat_sys_signal_2_op_5_bar, R$drawable.stat_sys_signal_3_op_5_bar, R$drawable.stat_sys_signal_4_op_5_bar, R$drawable.stat_sys_signal_5_op_5_bar}, new int[]{R$drawable.stat_sys_signal_0_op_5_bar_fully, R$drawable.stat_sys_signal_1_op_5_bar_fully, R$drawable.stat_sys_signal_2_op_5_bar_fully, R$drawable.stat_sys_signal_3_op_5_bar_fully, R$drawable.stat_sys_signal_4_op_5_bar_fully, R$drawable.stat_sys_signal_5_op_5_bar_fully}};
    static final int[][] TELEPHONY_SIGNAL_STRENGTH_FOUR_BAR = {new int[]{R$drawable.stat_sys_signal_0, R$drawable.stat_sys_signal_1, R$drawable.stat_sys_signal_2, R$drawable.stat_sys_signal_3, R$drawable.stat_sys_signal_4}, new int[]{R$drawable.stat_sys_signal_0_fully, R$drawable.stat_sys_signal_1_fully, R$drawable.stat_sys_signal_2_fully, R$drawable.stat_sys_signal_3_fully, R$drawable.stat_sys_signal_4_fully}};
    static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING_R = {new int[]{R$drawable.stat_sys_signal_0_default_roam, R$drawable.stat_sys_signal_1_default_roam, R$drawable.stat_sys_signal_2_default_roam, R$drawable.stat_sys_signal_3_default_roam, R$drawable.stat_sys_signal_4_default_roam}, new int[]{R$drawable.stat_sys_signal_0_default_fully_roam, R$drawable.stat_sys_signal_1_default_fully_roam, R$drawable.stat_sys_signal_2_default_fully_roam, R$drawable.stat_sys_signal_3_default_fully_roam, R$drawable.stat_sys_signal_4_default_fully_roam}};
    static final MobileIconGroup THREE_G;
    static final MobileIconGroup THREE_G_PLUS;
    static final MobileIconGroup TWO_G;
    static final MobileIconGroup UNKNOWN;
    static final MobileIconGroup WFC;
    private static boolean isInitiated = false;

    static {
        int i = R$drawable.stat_sys_data_fully_connected_5g;
        ICON_5G = i;
        ICON_5G_SA = i;
        ICON_5G_BASIC = i;
        int[] iArr = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup = new MobileIconGroup("CARRIER_NETWORK_CHANGE", null, null, iArr, 0, 0, 0, 0, iArr[0], R$string.carrier_network_change_mode, 0, false);
        CARRIER_NETWORK_CHANGE = mobileIconGroup;
        int[] iArr2 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup2 = new MobileIconGroup("3G", null, null, iArr2, 0, 0, 0, 0, iArr2[0], R$string.data_connection_3g, ICON_3G, true);
        THREE_G = mobileIconGroup2;
        int[] iArr3 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup3 = new MobileIconGroup("WFC", null, null, iArr3, 0, 0, 0, 0, iArr3[0], 0, 0, false);
        WFC = mobileIconGroup3;
        int[] iArr4 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup4 = new MobileIconGroup("Unknown", null, null, iArr4, 0, 0, 0, 0, iArr4[0], 0, 0, false);
        UNKNOWN = mobileIconGroup4;
        int[] iArr5 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup5 = new MobileIconGroup("E", null, null, iArr5, 0, 0, 0, 0, iArr5[0], R$string.data_connection_edge, ICON_E, false);
        f100E = mobileIconGroup5;
        int[] iArr6 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup6 = new MobileIconGroup("1X", null, null, iArr6, 0, 0, 0, 0, iArr6[0], R$string.data_connection_cdma, ICON_1X, true);
        ONE_X = mobileIconGroup6;
        int[] iArr7 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup7 = new MobileIconGroup("G", null, null, iArr7, 0, 0, 0, 0, iArr7[0], R$string.data_connection_gprs, ICON_G, false);
        f101G = mobileIconGroup7;
        int[] iArr8 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup8 = new MobileIconGroup("H", null, null, iArr8, 0, 0, 0, 0, iArr8[0], R$string.data_connection_3_5g, ICON_H, false);
        f102H = mobileIconGroup8;
        int[] iArr9 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup9 = new MobileIconGroup("H+", null, null, iArr9, 0, 0, 0, 0, iArr9[0], R$string.data_connection_3_5g_plus, ICON_H_PLUS, false);
        H_PLUS = mobileIconGroup9;
        int[] iArr10 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup10 = new MobileIconGroup("4G", null, null, iArr10, 0, 0, 0, 0, iArr10[0], R$string.data_connection_4g, ICON_4G, true);
        FOUR_G = mobileIconGroup10;
        int[] iArr11 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup11 = new MobileIconGroup("4G+", null, null, iArr11, 0, 0, 0, 0, iArr11[0], R$string.data_connection_4g_plus, ICON_4G_PLUS, true);
        FOUR_G_PLUS = mobileIconGroup11;
        int[] iArr12 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup12 = new MobileIconGroup("LTE", null, null, iArr12, 0, 0, 0, 0, iArr12[0], R$string.data_connection_lte, ICON_LTE, true);
        LTE = mobileIconGroup12;
        int[] iArr13 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup13 = new MobileIconGroup("LTE+", null, null, iArr13, 0, 0, 0, 0, iArr13[0], R$string.data_connection_lte_plus, ICON_LTE_PLUS, true);
        LTE_PLUS = mobileIconGroup13;
        int[] iArr14 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup14 = new MobileIconGroup("5Ge", null, null, iArr14, 0, 0, 0, 0, iArr14[0], R$string.data_connection_5ge, ICON_5G_E, true);
        LTE_CA_5G_E = mobileIconGroup14;
        int[] iArr15 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup15 = new MobileIconGroup("5G", null, null, iArr15, 0, 0, 0, 0, iArr15[0], R$string.data_connection_5g, ICON_5G, true);
        NR_5G = mobileIconGroup15;
        int[] iArr16 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup16 = new MobileIconGroup("5G_PLUS", null, null, iArr16, 0, 0, 0, 0, iArr16[0], R$string.data_connection_5g_plus, ICON_5G_PLUS, true);
        NR_5G_PLUS = mobileIconGroup16;
        int[] iArr17 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup17 = new MobileIconGroup("DataDisabled", null, null, iArr17, 0, 0, 0, 0, iArr17[0], R$string.cell_data_off_content_description, ICON_DATA_DISABLED, false);
        DATA_DISABLED = mobileIconGroup17;
        int[] iArr18 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup18 = new MobileIconGroup("NotDefaultData", null, null, iArr18, 0, 0, 0, 0, iArr18[0], R$string.not_default_data_content_description, 0, false);
        NOT_DEFAULT_DATA = mobileIconGroup18;
        int[] iArr19 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup19 = new MobileIconGroup("5G", null, null, iArr19, 0, 0, 0, 0, iArr19[0], R$string.data_connection_5g, ICON_5G, false);
        FIVE_G = mobileIconGroup19;
        int[] iArr20 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup20 = new MobileIconGroup("5GBasic", null, null, iArr20, 0, 0, 0, 0, iArr20[0], R$string.data_connection_5g_basic, ICON_5G_BASIC, false);
        FIVE_G_BASIC = mobileIconGroup20;
        int[] iArr21 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup21 = new MobileIconGroup("5GUWB", null, null, iArr21, 0, 0, 0, 0, iArr21[0], R$string.data_connection_5g_uwb, ICON_5G_UWB, false);
        FIVE_G_UWB = mobileIconGroup21;
        int[] iArr22 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup22 = new MobileIconGroup("5GSA", null, null, iArr22, 0, 0, 0, 0, iArr22[0], R$string.data_connection_5g_sa, ICON_5G_SA, false);
        FIVE_G_SA = mobileIconGroup22;
        int[] iArr23 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup23 = new MobileIconGroup("2G", null, null, iArr23, 0, 0, 0, 0, iArr23[0], R$string.data_connection_2g, ICON_2G, true);
        TWO_G = mobileIconGroup23;
        int[] iArr24 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup24 = new MobileIconGroup("3G+", null, null, iArr24, 0, 0, 0, 0, iArr24[0], R$string.data_connection_3_5g, ICON_3G_PLUS, true);
        THREE_G_PLUS = mobileIconGroup24;
        int[] iArr25 = AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH;
        MobileIconGroup mobileIconGroup25 = new MobileIconGroup("LTE", null, null, iArr25, 0, 0, 0, 0, iArr25[0], R$string.data_connection_lte, OpSignalIcons.FOUR_G_LTE, true);
        FOUR_G_LTE = mobileIconGroup25;
        ICON_NAME_TO_ICON.put("carrier_network_change", CARRIER_NETWORK_CHANGE);
        ICON_NAME_TO_ICON.put("3g", THREE_G);
        ICON_NAME_TO_ICON.put("wfc", WFC);
        ICON_NAME_TO_ICON.put("unknown", UNKNOWN);
        ICON_NAME_TO_ICON.put("e", f100E);
        ICON_NAME_TO_ICON.put("1x", ONE_X);
        ICON_NAME_TO_ICON.put("g", f101G);
        ICON_NAME_TO_ICON.put("h", f102H);
        ICON_NAME_TO_ICON.put("h+", H_PLUS);
        ICON_NAME_TO_ICON.put("4g", FOUR_G);
        ICON_NAME_TO_ICON.put("4g+", FOUR_G_PLUS);
        ICON_NAME_TO_ICON.put("5ge", LTE_CA_5G_E);
        ICON_NAME_TO_ICON.put("lte", LTE);
        ICON_NAME_TO_ICON.put("lte+", LTE_PLUS);
        ICON_NAME_TO_ICON.put("5g", NR_5G);
        ICON_NAME_TO_ICON.put("5g_plus", NR_5G_PLUS);
        ICON_NAME_TO_ICON.put("datadisable", DATA_DISABLED);
        ICON_NAME_TO_ICON.put("notdefaultdata", NOT_DEFAULT_DATA);
    }

    static int[] getStackedVoiceIcon(int i, int i2, boolean z, boolean z2) {
        int i3;
        int[] iArr = new int[2];
        int i4 = STACKED_STRENGTH_ICONS[i2];
        if (z) {
            i3 = STACKED_ICON_ROAM;
        } else if (i == 7) {
            i3 = STACKED_ICON_1X;
        } else if (i != 13) {
            int i5 = STACKED_ICON_1X;
            StringBuilder sb = new StringBuilder();
            sb.append("Unknow network type:");
            sb.append(i);
            Log.w("TelephonyIcons", sb.toString());
            i3 = i5;
        } else {
            i3 = z2 ? STACKED_ICON_LTE : STACKED_ICON_4G;
        }
        iArr[0] = i4;
        iArr[1] = i3;
        return iArr;
    }

    static int[] getStackedDataIcon(int i, int i2, boolean z) {
        int i3;
        int i4;
        int[] iArr = new int[2];
        int i5 = STACKED_STRENGTH_ICONS[i2];
        if (i != 3) {
            if (i != 4) {
                if (!(i == 5 || i == 6)) {
                    if (i != 7) {
                        if (i != 17) {
                            if (i != 19) {
                                switch (i) {
                                    case 12:
                                    case 14:
                                        break;
                                    case 13:
                                        if (!z) {
                                            i4 = STACKED_ICON_4G;
                                            break;
                                        } else {
                                            i4 = STACKED_ICON_LTE;
                                            break;
                                        }
                                    default:
                                        i3 = STACKED_ICON_G;
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("Unknow network type:");
                                        sb.append(i);
                                        Log.w("TelephonyIcons", sb.toString());
                                        break;
                                }
                            } else {
                                i4 = z ? STACKED_ICON_LTE_PLUS : STACKED_ICON_4G_PLUS;
                            }
                            i3 = i4;
                            iArr[0] = i5;
                            iArr[1] = i3;
                            return iArr;
                        }
                    }
                }
            }
            i3 = STACKED_ICON_2G;
            iArr[0] = i5;
            iArr[1] = i3;
            return iArr;
        }
        i3 = STACKED_ICON_3G;
        iArr[0] = i5;
        iArr[1] = i3;
        return iArr;
    }

    static int getOneplusVirtualSimSignalIconId(int i) {
        return ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_VIRTUAL[i];
    }

    static int getOneplusRoamingSignalIconId(int i) {
        return ONEPLUS_TELEPHONY_SIGNAL_STRENGTH_ROAMING[i];
    }
}

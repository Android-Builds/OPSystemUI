package com.android.systemui.statusbar.policy;

import com.android.systemui.R$string;
import com.oneplus.util.OpUtils;

public class AccessibilityContentDescriptions {
    static final int[] DATA_CONNECTION_STRENGTH = {R$string.accessibility_no_data, R$string.accessibility_data_one_bar, R$string.accessibility_data_two_bars, R$string.accessibility_data_three_bars, R$string.accessibility_data_signal_full};
    static final int[] ETHERNET_CONNECTION_VALUES = {R$string.accessibility_ethernet_disconnected, R$string.accessibility_ethernet_connected};
    static final int[] PHONE_SIGNAL_STRENGTH = (OpUtils.isSupportFiveBar() ? PHONE_SIGNAL_STRENGTH_FIVE_BAR : PHONE_SIGNAL_STRENGTH_FOUR_BAR);
    static final int[] PHONE_SIGNAL_STRENGTH_FIVE_BAR = {R$string.accessibility_no_phone, R$string.accessibility_phone_one_bar, R$string.accessibility_phone_two_bars, R$string.accessibility_phone_three_bars, R$string.accessibility_phone_four_bars, R$string.accessibility_phone_signal_full};
    static final int[] PHONE_SIGNAL_STRENGTH_FOUR_BAR = {R$string.accessibility_no_phone, R$string.accessibility_phone_one_bar, R$string.accessibility_phone_two_bars, R$string.accessibility_phone_three_bars, R$string.accessibility_phone_signal_full};
    static final int[] WIFI_CONNECTION_STRENGTH = {R$string.accessibility_no_wifi, R$string.accessibility_wifi_one_bar, R$string.accessibility_wifi_two_bars, R$string.accessibility_wifi_three_bars, R$string.accessibility_wifi_signal_full};
    static final int WIFI_NO_CONNECTION = R$string.accessibility_no_wifi;
}

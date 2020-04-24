package com.android.systemui.assist;

import android.provider.DeviceConfig;
import android.provider.DeviceConfig.OnPropertiesChangedListener;
import java.util.concurrent.Executor;

class PhenotypeHelper {
    PhenotypeHelper() {
    }

    /* access modifiers changed from: 0000 */
    public long getLong(String str, long j) {
        return DeviceConfig.getLong("systemui", str, j);
    }

    /* access modifiers changed from: 0000 */
    public int getInt(String str, int i) {
        return DeviceConfig.getInt("systemui", str, i);
    }

    /* access modifiers changed from: 0000 */
    public String getString(String str, String str2) {
        return DeviceConfig.getString("systemui", str, str2);
    }

    /* access modifiers changed from: 0000 */
    public boolean getBoolean(String str, boolean z) {
        return DeviceConfig.getBoolean("systemui", str, z);
    }

    /* access modifiers changed from: 0000 */
    public void addOnPropertiesChangedListener(Executor executor, OnPropertiesChangedListener onPropertiesChangedListener) {
        DeviceConfig.addOnPropertiesChangedListener("systemui", executor, onPropertiesChangedListener);
    }
}

package com.android.systemui.power;

/* compiled from: BatteryStateSnapshot.kt */
public final class BatteryStateSnapshot {
    private final long averageTimeToDischargeMillis;
    private final int batteryLevel;
    private final int batteryStatus;
    private final int bucket;
    private final boolean isBasedOnUsage;
    private boolean isHybrid = false;
    private final boolean isLowWarningEnabled;
    private final boolean isPowerSaver;
    private final int lowLevelThreshold;
    private final long lowThresholdMillis;
    private final boolean plugged;
    private final int severeLevelThreshold;
    private final long severeThresholdMillis;
    private final long timeRemainingMillis;

    public boolean equals(Object obj) {
        if (this != obj) {
            if (obj instanceof BatteryStateSnapshot) {
                BatteryStateSnapshot batteryStateSnapshot = (BatteryStateSnapshot) obj;
                if (this.batteryLevel == batteryStateSnapshot.batteryLevel) {
                    if (this.isPowerSaver == batteryStateSnapshot.isPowerSaver) {
                        if (this.plugged == batteryStateSnapshot.plugged) {
                            if (this.bucket == batteryStateSnapshot.bucket) {
                                if (this.batteryStatus == batteryStateSnapshot.batteryStatus) {
                                    if (this.severeLevelThreshold == batteryStateSnapshot.severeLevelThreshold) {
                                        if (this.lowLevelThreshold == batteryStateSnapshot.lowLevelThreshold) {
                                            if (this.timeRemainingMillis == batteryStateSnapshot.timeRemainingMillis) {
                                                if (this.averageTimeToDischargeMillis == batteryStateSnapshot.averageTimeToDischargeMillis) {
                                                    if (this.severeThresholdMillis == batteryStateSnapshot.severeThresholdMillis) {
                                                        if (this.lowThresholdMillis == batteryStateSnapshot.lowThresholdMillis) {
                                                            if (this.isBasedOnUsage == batteryStateSnapshot.isBasedOnUsage) {
                                                                if (this.isLowWarningEnabled == batteryStateSnapshot.isLowWarningEnabled) {
                                                                    return true;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
        return true;
    }

    public int hashCode() {
        int hashCode = Integer.hashCode(this.batteryLevel) * 31;
        int i = this.isPowerSaver;
        if (i != 0) {
            i = 1;
        }
        int i2 = (hashCode + i) * 31;
        int i3 = this.plugged;
        if (i3 != 0) {
            i3 = 1;
        }
        int hashCode2 = (((((((((((((((((i2 + i3) * 31) + Integer.hashCode(this.bucket)) * 31) + Integer.hashCode(this.batteryStatus)) * 31) + Integer.hashCode(this.severeLevelThreshold)) * 31) + Integer.hashCode(this.lowLevelThreshold)) * 31) + Long.hashCode(this.timeRemainingMillis)) * 31) + Long.hashCode(this.averageTimeToDischargeMillis)) * 31) + Long.hashCode(this.severeThresholdMillis)) * 31) + Long.hashCode(this.lowThresholdMillis)) * 31;
        int i4 = this.isBasedOnUsage;
        if (i4 != 0) {
            i4 = 1;
        }
        int i5 = (hashCode2 + i4) * 31;
        int i6 = this.isLowWarningEnabled;
        if (i6 != 0) {
            i6 = 1;
        }
        return i5 + i6;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BatteryStateSnapshot(batteryLevel=");
        sb.append(this.batteryLevel);
        sb.append(", isPowerSaver=");
        sb.append(this.isPowerSaver);
        sb.append(", plugged=");
        sb.append(this.plugged);
        sb.append(", bucket=");
        sb.append(this.bucket);
        sb.append(", batteryStatus=");
        sb.append(this.batteryStatus);
        sb.append(", severeLevelThreshold=");
        sb.append(this.severeLevelThreshold);
        sb.append(", lowLevelThreshold=");
        sb.append(this.lowLevelThreshold);
        sb.append(", timeRemainingMillis=");
        sb.append(this.timeRemainingMillis);
        sb.append(", averageTimeToDischargeMillis=");
        sb.append(this.averageTimeToDischargeMillis);
        sb.append(", severeThresholdMillis=");
        sb.append(this.severeThresholdMillis);
        sb.append(", lowThresholdMillis=");
        sb.append(this.lowThresholdMillis);
        sb.append(", isBasedOnUsage=");
        sb.append(this.isBasedOnUsage);
        sb.append(", isLowWarningEnabled=");
        sb.append(this.isLowWarningEnabled);
        sb.append(")");
        return sb.toString();
    }

    public BatteryStateSnapshot(int i, boolean z, boolean z2, int i2, int i3, int i4, int i5, long j, long j2, long j3, long j4, boolean z3, boolean z4) {
        this.batteryLevel = i;
        this.isPowerSaver = z;
        this.plugged = z2;
        this.bucket = i2;
        this.batteryStatus = i3;
        this.severeLevelThreshold = i4;
        this.lowLevelThreshold = i5;
        this.timeRemainingMillis = j;
        this.averageTimeToDischargeMillis = j2;
        this.severeThresholdMillis = j3;
        this.lowThresholdMillis = j4;
        this.isBasedOnUsage = z3;
        this.isLowWarningEnabled = z4;
    }

    public final int getBatteryLevel() {
        return this.batteryLevel;
    }

    public final boolean isPowerSaver() {
        return this.isPowerSaver;
    }

    public final boolean getPlugged() {
        return this.plugged;
    }

    public final int getBucket() {
        return this.bucket;
    }

    public final int getBatteryStatus() {
        return this.batteryStatus;
    }

    public final int getSevereLevelThreshold() {
        return this.severeLevelThreshold;
    }

    public final int getLowLevelThreshold() {
        return this.lowLevelThreshold;
    }

    public final long getTimeRemainingMillis() {
        return this.timeRemainingMillis;
    }

    public final long getAverageTimeToDischargeMillis() {
        return this.averageTimeToDischargeMillis;
    }

    public final long getSevereThresholdMillis() {
        return this.severeThresholdMillis;
    }

    public final long getLowThresholdMillis() {
        return this.lowThresholdMillis;
    }

    public final boolean isBasedOnUsage() {
        return this.isBasedOnUsage;
    }

    public final boolean isLowWarningEnabled() {
        return this.isLowWarningEnabled;
    }

    public final boolean isHybrid() {
        return this.isHybrid;
    }

    public BatteryStateSnapshot(int i, boolean z, boolean z2, int i2, int i3, int i4, int i5) {
        long j = (long) -1;
        this(i, z, z2, i2, i3, i4, i5, j, j, j, j, false, true);
    }
}

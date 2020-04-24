package com.android.systemui.usb;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.Notification.TvExtender;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.MoveCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.os.StrictMode.VmPolicy;
import android.os.UserHandle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.SystemUI;
import com.android.systemui.util.NotificationChannels;

public class StorageNotification extends SystemUI {
    private final BroadcastReceiver mFinishReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            StorageNotification.this.mNotificationManager.cancelAsUser(null, 1397575510, UserHandle.ALL);
        }
    };
    private final StorageEventListener mListener = new StorageEventListener() {
        public void onVolumeStateChanged(VolumeInfo volumeInfo, int i, int i2) {
            StorageNotification.this.onVolumeStateChangedInternal(volumeInfo);
        }

        public void onVolumeRecordChanged(VolumeRecord volumeRecord) {
            VolumeInfo findVolumeByUuid = StorageNotification.this.mStorageManager.findVolumeByUuid(volumeRecord.getFsUuid());
            if (findVolumeByUuid != null && findVolumeByUuid.isMountedReadable()) {
                StorageNotification.this.onVolumeStateChangedInternal(findVolumeByUuid);
            }
        }

        public void onVolumeForgotten(String str) {
            StorageNotification.this.mNotificationManager.cancelAsUser(str, 1397772886, UserHandle.ALL);
        }

        public void onDiskScanned(DiskInfo diskInfo, int i) {
            StorageNotification.this.onDiskScannedInternal(diskInfo, i);
        }

        public void onDiskDestroyed(DiskInfo diskInfo) {
            StorageNotification.this.onDiskDestroyedInternal(diskInfo);
        }
    };
    private final MoveCallback mMoveCallback = new MoveCallback() {
        public void onCreated(int i, Bundle bundle) {
            MoveInfo moveInfo = new MoveInfo();
            moveInfo.moveId = i;
            moveInfo.extras = bundle;
            if (bundle != null) {
                moveInfo.packageName = bundle.getString("android.intent.extra.PACKAGE_NAME");
                moveInfo.label = bundle.getString("android.intent.extra.TITLE");
                moveInfo.volumeUuid = bundle.getString("android.os.storage.extra.FS_UUID");
            }
            StorageNotification.this.mMoves.put(i, moveInfo);
        }

        public void onStatusChanged(int i, int i2, long j) {
            MoveInfo moveInfo = (MoveInfo) StorageNotification.this.mMoves.get(i);
            if (moveInfo == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Ignoring unknown move ");
                sb.append(i);
                Log.w("StorageNotification", sb.toString());
                return;
            }
            if (PackageManager.isMoveStatusFinished(i2)) {
                StorageNotification.this.onMoveFinished(moveInfo, i2);
            } else {
                StorageNotification.this.onMoveProgress(moveInfo, i2, j);
            }
        }
    };
    /* access modifiers changed from: private */
    public final SparseArray<MoveInfo> mMoves = new SparseArray<>();
    /* access modifiers changed from: private */
    public NotificationManager mNotificationManager;
    private final BroadcastReceiver mSnoozeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            StorageNotification.this.mStorageManager.setVolumeSnoozed(intent.getStringExtra("android.os.storage.extra.FS_UUID"), true);
        }
    };
    /* access modifiers changed from: private */
    public StorageManager mStorageManager;

    private static class MoveInfo {
        public Bundle extras;
        public String label;
        public int moveId;
        public String packageName;
        public String volumeUuid;

        private MoveInfo() {
        }
    }

    private Notification onVolumeFormatting(VolumeInfo volumeInfo) {
        return null;
    }

    private Notification onVolumeUnmounted(VolumeInfo volumeInfo) {
        return null;
    }

    public void start() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        this.mStorageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        this.mStorageManager.registerListener(this.mListener);
        String str = "android.permission.MOUNT_UNMOUNT_FILESYSTEMS";
        this.mContext.registerReceiver(this.mSnoozeReceiver, new IntentFilter("com.android.systemui.action.SNOOZE_VOLUME"), str, null);
        this.mContext.registerReceiver(this.mFinishReceiver, new IntentFilter("com.android.systemui.action.FINISH_WIZARD"), str, null);
        for (DiskInfo diskInfo : this.mStorageManager.getDisks()) {
            onDiskScannedInternal(diskInfo, diskInfo.volumeCount);
        }
        for (VolumeInfo onVolumeStateChangedInternal : this.mStorageManager.getVolumes()) {
            onVolumeStateChangedInternal(onVolumeStateChangedInternal);
        }
        this.mContext.getPackageManager().registerMoveCallback(this.mMoveCallback, new Handler());
        updateMissingPrivateVolumes();
    }

    private void updateMissingPrivateVolumes() {
        if (!isTv()) {
            for (VolumeRecord volumeRecord : this.mStorageManager.getVolumeRecords()) {
                if (volumeRecord.getType() == 1) {
                    String fsUuid = volumeRecord.getFsUuid();
                    VolumeInfo findVolumeByUuid = this.mStorageManager.findVolumeByUuid(fsUuid);
                    if ((findVolumeByUuid == null || !findVolumeByUuid.isMountedWritable()) && !volumeRecord.isSnoozed()) {
                        String string = this.mContext.getString(17039937, new Object[]{volumeRecord.getNickname()});
                        String string2 = this.mContext.getString(17039936);
                        Builder extend = new Builder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(17302790).setColor(this.mContext.getColor(17170460)).setContentTitle(string).setContentText(string2).setContentIntent(buildForgetPendingIntent(volumeRecord)).setStyle(new BigTextStyle().bigText(string2)).setVisibility(1).setLocalOnly(true).setCategory("sys").setDeleteIntent(buildSnoozeIntent(fsUuid)).extend(new TvExtender());
                        SystemUI.overrideNotificationAppName(this.mContext, extend, false);
                        this.mNotificationManager.notifyAsUser(fsUuid, 1397772886, extend.build(), UserHandle.ALL);
                    } else {
                        this.mNotificationManager.cancelAsUser(fsUuid, 1397772886, UserHandle.ALL);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void onDiskScannedInternal(DiskInfo diskInfo, int i) {
        if (i != 0 || diskInfo.size <= 0) {
            this.mNotificationManager.cancelAsUser(diskInfo.getId(), 1396986699, UserHandle.ALL);
            return;
        }
        String string = this.mContext.getString(17039967, new Object[]{diskInfo.getDescription()});
        String string2 = this.mContext.getString(17039966, new Object[]{diskInfo.getDescription()});
        Builder extend = new Builder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(getSmallIcon(diskInfo, 6)).setColor(this.mContext.getColor(17170460)).setContentTitle(string).setContentText(string2).setContentIntent(buildInitPendingIntent(diskInfo)).setStyle(new BigTextStyle().bigText(string2)).setVisibility(1).setLocalOnly(true).setCategory("err").extend(new TvExtender());
        SystemUI.overrideNotificationAppName(this.mContext, extend, false);
        this.mNotificationManager.notifyAsUser(diskInfo.getId(), 1396986699, extend.build(), UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public void onDiskDestroyedInternal(DiskInfo diskInfo) {
        this.mNotificationManager.cancelAsUser(diskInfo.getId(), 1396986699, UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public void onVolumeStateChangedInternal(VolumeInfo volumeInfo) {
        int type = volumeInfo.getType();
        if (type == 0) {
            onPublicVolumeStateChangedInternal(volumeInfo);
        } else if (type == 1) {
            onPrivateVolumeStateChangedInternal(volumeInfo);
        }
    }

    private void onPrivateVolumeStateChangedInternal(VolumeInfo volumeInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Notifying about private volume: ");
        sb.append(volumeInfo.toString());
        Log.d("StorageNotification", sb.toString());
        updateMissingPrivateVolumes();
    }

    private void onPublicVolumeStateChangedInternal(VolumeInfo volumeInfo) {
        Notification notification;
        StringBuilder sb = new StringBuilder();
        sb.append("Notifying about public volume: ");
        sb.append(volumeInfo.toString());
        Log.d("StorageNotification", sb.toString());
        switch (volumeInfo.getState()) {
            case 0:
                notification = onVolumeUnmounted(volumeInfo);
                break;
            case 1:
                notification = onVolumeChecking(volumeInfo);
                break;
            case 2:
            case 3:
                notification = onVolumeMounted(volumeInfo);
                break;
            case 4:
                notification = onVolumeFormatting(volumeInfo);
                break;
            case 5:
                notification = onVolumeEjecting(volumeInfo);
                break;
            case 6:
                notification = onVolumeUnmountable(volumeInfo);
                break;
            case 7:
                notification = onVolumeRemoved(volumeInfo);
                break;
            case 8:
                notification = onVolumeBadRemoval(volumeInfo);
                break;
            default:
                notification = null;
                break;
        }
        if (notification != null) {
            this.mNotificationManager.notifyAsUser(volumeInfo.getId(), 1397773634, notification, UserHandle.of(volumeInfo.getMountUserId()));
        } else {
            this.mNotificationManager.cancelAsUser(volumeInfo.getId(), 1397773634, UserHandle.of(volumeInfo.getMountUserId()));
        }
    }

    private Notification onVolumeChecking(VolumeInfo volumeInfo) {
        DiskInfo disk = volumeInfo.getDisk();
        return buildNotificationBuilder(volumeInfo, this.mContext.getString(17039934, new Object[]{disk.getDescription()}), this.mContext.getString(17039933, new Object[]{disk.getDescription()})).setCategory("progress").setOngoing(true).build();
    }

    private Notification onVolumeMounted(VolumeInfo volumeInfo) {
        VolumeRecord findRecordByUuid = this.mStorageManager.findRecordByUuid(volumeInfo.getFsUuid());
        DiskInfo disk = volumeInfo.getDisk();
        if (findRecordByUuid.isSnoozed() && disk.isAdoptable()) {
            return null;
        }
        if (!disk.isAdoptable() || findRecordByUuid.isInited()) {
            String description = disk.getDescription();
            String string = this.mContext.getString(17039948, new Object[]{disk.getDescription()});
            PendingIntent buildBrowsePendingIntent = buildBrowsePendingIntent(volumeInfo);
            Builder category = buildNotificationBuilder(volumeInfo, description, string).addAction(new Action(17302420, this.mContext.getString(17039932), buildBrowsePendingIntent)).addAction(new Action(17302402, this.mContext.getString(17039961), buildUnmountPendingIntent(volumeInfo))).setContentIntent(buildBrowsePendingIntent).setCategory("sys");
            if (disk.isAdoptable()) {
                category.setDeleteIntent(buildSnoozeIntent(volumeInfo.getFsUuid()));
            }
            return category.build();
        }
        String description2 = disk.getDescription();
        String string2 = this.mContext.getString(17039944, new Object[]{disk.getDescription()});
        PendingIntent buildInitPendingIntent = buildInitPendingIntent(volumeInfo);
        return buildNotificationBuilder(volumeInfo, description2, string2).addAction(new Action(17302796, this.mContext.getString(17039935), buildInitPendingIntent)).addAction(new Action(17302402, this.mContext.getString(17039961), buildUnmountPendingIntent(volumeInfo))).setContentIntent(buildInitPendingIntent).setDeleteIntent(buildSnoozeIntent(volumeInfo.getFsUuid())).build();
    }

    private Notification onVolumeEjecting(VolumeInfo volumeInfo) {
        DiskInfo disk = volumeInfo.getDisk();
        return buildNotificationBuilder(volumeInfo, this.mContext.getString(17039965, new Object[]{disk.getDescription()}), this.mContext.getString(17039964, new Object[]{disk.getDescription()})).setCategory("progress").setOngoing(true).build();
    }

    private Notification onVolumeUnmountable(VolumeInfo volumeInfo) {
        DiskInfo disk = volumeInfo.getDisk();
        return buildNotificationBuilder(volumeInfo, this.mContext.getString(17039963, new Object[]{disk.getDescription()}), this.mContext.getString(17039962, new Object[]{disk.getDescription()})).setContentIntent(buildInitPendingIntent(volumeInfo)).setCategory("err").build();
    }

    private Notification onVolumeRemoved(VolumeInfo volumeInfo) {
        if (!volumeInfo.isPrimary() || volumeInfo.getDisk().isUsb()) {
            return null;
        }
        DiskInfo disk = volumeInfo.getDisk();
        return buildNotificationBuilder(volumeInfo, this.mContext.getString(17039947, new Object[]{disk.getDescription()}), this.mContext.getString(17039946, new Object[]{disk.getDescription()})).setCategory("err").build();
    }

    private Notification onVolumeBadRemoval(VolumeInfo volumeInfo) {
        if (!volumeInfo.isPrimary() || volumeInfo.getDisk().isUsb()) {
            return null;
        }
        DiskInfo disk = volumeInfo.getDisk();
        return buildNotificationBuilder(volumeInfo, this.mContext.getString(17039931, new Object[]{disk.getDescription()}), this.mContext.getString(17039930, new Object[]{disk.getDescription()})).setCategory("err").build();
    }

    /* access modifiers changed from: private */
    public void onMoveProgress(MoveInfo moveInfo, int i, long j) {
        String str;
        CharSequence charSequence;
        PendingIntent pendingIntent;
        if (!TextUtils.isEmpty(moveInfo.label)) {
            str = this.mContext.getString(17039940, new Object[]{moveInfo.label});
        } else {
            str = this.mContext.getString(17039943);
        }
        if (j < 0) {
            charSequence = null;
        } else {
            charSequence = DateUtils.formatDuration(j);
        }
        if (moveInfo.packageName != null) {
            pendingIntent = buildWizardMovePendingIntent(moveInfo);
        } else {
            pendingIntent = buildWizardMigratePendingIntent(moveInfo);
        }
        Builder ongoing = new Builder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(17302790).setColor(this.mContext.getColor(17170460)).setContentTitle(str).setContentText(charSequence).setContentIntent(pendingIntent).setStyle(new BigTextStyle().bigText(charSequence)).setVisibility(1).setLocalOnly(true).setCategory("progress").setProgress(100, i, false).setOngoing(true);
        SystemUI.overrideNotificationAppName(this.mContext, ongoing, false);
        this.mNotificationManager.notifyAsUser(moveInfo.packageName, 1397575510, ongoing.build(), UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public void onMoveFinished(MoveInfo moveInfo, int i) {
        String str;
        String str2;
        String str3 = moveInfo.packageName;
        if (str3 != null) {
            this.mNotificationManager.cancelAsUser(str3, 1397575510, UserHandle.ALL);
            return;
        }
        VolumeInfo primaryStorageCurrentVolume = this.mContext.getPackageManager().getPrimaryStorageCurrentVolume();
        String bestVolumeDescription = this.mStorageManager.getBestVolumeDescription(primaryStorageCurrentVolume);
        if (i == -100) {
            str = this.mContext.getString(17039942);
            str2 = this.mContext.getString(17039941, new Object[]{bestVolumeDescription});
        } else {
            str = this.mContext.getString(17039939);
            str2 = this.mContext.getString(17039938);
        }
        PendingIntent pendingIntent = (primaryStorageCurrentVolume == null || primaryStorageCurrentVolume.getDisk() == null) ? primaryStorageCurrentVolume != null ? buildVolumeSettingsPendingIntent(primaryStorageCurrentVolume) : null : buildWizardReadyPendingIntent(primaryStorageCurrentVolume.getDisk());
        Builder autoCancel = new Builder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(17302790).setColor(this.mContext.getColor(17170460)).setContentTitle(str).setContentText(str2).setContentIntent(pendingIntent).setStyle(new BigTextStyle().bigText(str2)).setVisibility(1).setLocalOnly(true).setCategory("sys").setAutoCancel(true);
        SystemUI.overrideNotificationAppName(this.mContext, autoCancel, false);
        this.mNotificationManager.notifyAsUser(moveInfo.packageName, 1397575510, autoCancel.build(), UserHandle.ALL);
    }

    private int getSmallIcon(DiskInfo diskInfo, int i) {
        if (diskInfo.isSd()) {
            if (i == 1 || i == 5) {
            }
            return 17302790;
        } else if (diskInfo.isUsb()) {
            return 17302831;
        } else {
            return 17302790;
        }
    }

    private Builder buildNotificationBuilder(VolumeInfo volumeInfo, CharSequence charSequence, CharSequence charSequence2) {
        Builder extend = new Builder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(getSmallIcon(volumeInfo.getDisk(), volumeInfo.getState())).setColor(this.mContext.getColor(17170460)).setContentTitle(charSequence).setContentText(charSequence2).setStyle(new BigTextStyle().bigText(charSequence2)).setVisibility(1).setLocalOnly(true).extend(new TvExtender());
        SystemUI.overrideNotificationAppName(this.mContext, extend, false);
        return extend;
    }

    private PendingIntent buildInitPendingIntent(DiskInfo diskInfo) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.NEW_STORAGE");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardInit");
        }
        intent.putExtra("android.os.storage.extra.DISK_ID", diskInfo.getId());
        return PendingIntent.getActivityAsUser(this.mContext, diskInfo.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildInitPendingIntent(VolumeInfo volumeInfo) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.NEW_STORAGE");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardInit");
        }
        intent.putExtra("android.os.storage.extra.VOLUME_ID", volumeInfo.getId());
        return PendingIntent.getActivityAsUser(this.mContext, volumeInfo.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildUnmountPendingIntent(VolumeInfo volumeInfo) {
        Intent intent = new Intent();
        String str = "android.os.storage.extra.VOLUME_ID";
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.UNMOUNT_STORAGE");
            intent.putExtra(str, volumeInfo.getId());
            return PendingIntent.getActivityAsUser(this.mContext, volumeInfo.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
        }
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageUnmountReceiver");
        intent.putExtra(str, volumeInfo.getId());
        return PendingIntent.getBroadcastAsUser(this.mContext, volumeInfo.getId().hashCode(), intent, 268435456, UserHandle.CURRENT);
    }

    private PendingIntent buildBrowsePendingIntent(VolumeInfo volumeInfo) {
        VmPolicy allowVmViolations = StrictMode.allowVmViolations();
        try {
            Intent buildBrowseIntentForUser = volumeInfo.buildBrowseIntentForUser(volumeInfo.getMountUserId());
            return PendingIntent.getActivityAsUser(this.mContext, volumeInfo.getId().hashCode(), buildBrowseIntentForUser, 268435456, null, UserHandle.CURRENT);
        } finally {
            StrictMode.setVmPolicy(allowVmViolations);
        }
    }

    private PendingIntent buildVolumeSettingsPendingIntent(VolumeInfo volumeInfo) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("android.settings.INTERNAL_STORAGE_SETTINGS");
        } else {
            int type = volumeInfo.getType();
            String str = "com.android.settings";
            if (type == 0) {
                intent.setClassName(str, "com.android.settings.Settings$PublicVolumeSettingsActivity");
            } else if (type != 1) {
                return null;
            } else {
                intent.setClassName(str, "com.android.settings.Settings$PrivateVolumeSettingsActivity");
            }
        }
        intent.putExtra("android.os.storage.extra.VOLUME_ID", volumeInfo.getId());
        return PendingIntent.getActivityAsUser(this.mContext, volumeInfo.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildSnoozeIntent(String str) {
        Intent intent = new Intent("com.android.systemui.action.SNOOZE_VOLUME");
        intent.putExtra("android.os.storage.extra.FS_UUID", str);
        return PendingIntent.getBroadcastAsUser(this.mContext, str.hashCode(), intent, 268435456, UserHandle.CURRENT);
    }

    private PendingIntent buildForgetPendingIntent(VolumeRecord volumeRecord) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.Settings$PrivateVolumeForgetActivity");
        intent.putExtra("android.os.storage.extra.FS_UUID", volumeRecord.getFsUuid());
        return PendingIntent.getActivityAsUser(this.mContext, volumeRecord.getFsUuid().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardMigratePendingIntent(MoveInfo moveInfo) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.MIGRATE_STORAGE");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardMigrateProgress");
        }
        intent.putExtra("android.content.pm.extra.MOVE_ID", moveInfo.moveId);
        VolumeInfo findVolumeByQualifiedUuid = this.mStorageManager.findVolumeByQualifiedUuid(moveInfo.volumeUuid);
        if (findVolumeByQualifiedUuid != null) {
            intent.putExtra("android.os.storage.extra.VOLUME_ID", findVolumeByQualifiedUuid.getId());
        }
        return PendingIntent.getActivityAsUser(this.mContext, moveInfo.moveId, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardMovePendingIntent(MoveInfo moveInfo) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.MOVE_APP");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardMoveProgress");
        }
        intent.putExtra("android.content.pm.extra.MOVE_ID", moveInfo.moveId);
        return PendingIntent.getActivityAsUser(this.mContext, moveInfo.moveId, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardReadyPendingIntent(DiskInfo diskInfo) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("android.settings.INTERNAL_STORAGE_SETTINGS");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardReady");
        }
        intent.putExtra("android.os.storage.extra.DISK_ID", diskInfo.getId());
        return PendingIntent.getActivityAsUser(this.mContext, diskInfo.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private boolean isTv() {
        return this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
    }
}

package com.android.systemui.screenrecord;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.BigPictureStyle;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings.System;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import androidx.core.content.FileProvider;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class RecordingService extends Service {
    private Surface mInputSurface;
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaRecorder mMediaRecorder;
    private Builder mRecordingNotificationBuilder;
    private boolean mShowTaps;
    private File mTempFile;
    private boolean mUseAudio;
    private VirtualDisplay mVirtualDisplay;

    public IBinder onBind(Intent intent) {
        return null;
    }

    public static Intent getStartIntent(Context context, int i, Intent intent, boolean z, boolean z2) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.START").putExtra("extra_resultCode", i).putExtra("extra_data", intent).putExtra("extra_useAudio", z).putExtra("extra_showTaps", z2);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:6:0x0028, code lost:
        if (r0.equals("com.android.systemui.screenrecord.STOP") != false) goto L_0x0068;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onStartCommand(android.content.Intent r7, int r8, int r9) {
        /*
            r6 = this;
            java.lang.String r8 = "RecordingService"
            java.lang.String r9 = "RecordingService is starting"
            android.util.Log.d(r8, r9)
            r9 = 2
            if (r7 != 0) goto L_0x000b
            return r9
        L_0x000b:
            java.lang.String r0 = r7.getAction()
            java.lang.String r1 = "notification"
            java.lang.Object r1 = r6.getSystemService(r1)
            android.app.NotificationManager r1 = (android.app.NotificationManager) r1
            r2 = -1
            int r3 = r0.hashCode()
            r4 = 0
            r5 = 1
            switch(r3) {
                case -1691100604: goto L_0x005d;
                case -1688140755: goto L_0x0053;
                case -1687783248: goto L_0x0049;
                case -1256913972: goto L_0x003f;
                case -1224647939: goto L_0x0035;
                case -823616129: goto L_0x002b;
                case -470086188: goto L_0x0022;
                default: goto L_0x0021;
            }
        L_0x0021:
            goto L_0x0067
        L_0x0022:
            java.lang.String r3 = "com.android.systemui.screenrecord.STOP"
            boolean r0 = r0.equals(r3)
            if (r0 == 0) goto L_0x0067
            goto L_0x0068
        L_0x002b:
            java.lang.String r9 = "com.android.systemui.screenrecord.RESUME"
            boolean r9 = r0.equals(r9)
            if (r9 == 0) goto L_0x0067
            r9 = 4
            goto L_0x0068
        L_0x0035:
            java.lang.String r9 = "com.android.systemui.screenrecord.DELETE"
            boolean r9 = r0.equals(r9)
            if (r9 == 0) goto L_0x0067
            r9 = 6
            goto L_0x0068
        L_0x003f:
            java.lang.String r9 = "com.android.systemui.screenrecord.CANCEL"
            boolean r9 = r0.equals(r9)
            if (r9 == 0) goto L_0x0067
            r9 = r5
            goto L_0x0068
        L_0x0049:
            java.lang.String r9 = "com.android.systemui.screenrecord.START"
            boolean r9 = r0.equals(r9)
            if (r9 == 0) goto L_0x0067
            r9 = r4
            goto L_0x0068
        L_0x0053:
            java.lang.String r9 = "com.android.systemui.screenrecord.SHARE"
            boolean r9 = r0.equals(r9)
            if (r9 == 0) goto L_0x0067
            r9 = 5
            goto L_0x0068
        L_0x005d:
            java.lang.String r9 = "com.android.systemui.screenrecord.PAUSE"
            boolean r9 = r0.equals(r9)
            if (r9 == 0) goto L_0x0067
            r9 = 3
            goto L_0x0068
        L_0x0067:
            r9 = r2
        L_0x0068:
            java.lang.String r0 = "extra_path"
            java.lang.String r2 = "android.intent.action.CLOSE_SYSTEM_DIALOGS"
            switch(r9) {
                case 0: goto L_0x017a;
                case 1: goto L_0x014e;
                case 2: goto L_0x0101;
                case 3: goto L_0x00f7;
                case 4: goto L_0x00ed;
                case 5: goto L_0x00a6;
                case 6: goto L_0x0071;
                default: goto L_0x006f;
            }
        L_0x006f:
            goto L_0x01a5
        L_0x0071:
            android.content.Intent r9 = new android.content.Intent
            r9.<init>(r2)
            r6.sendBroadcast(r9)
            java.io.File r9 = new java.io.File
            java.lang.String r7 = r7.getStringExtra(r0)
            r9.<init>(r7)
            boolean r7 = r9.delete()
            if (r7 == 0) goto L_0x0096
            int r7 = com.android.systemui.R$string.screenrecord_delete_description
            android.widget.Toast r6 = android.widget.Toast.makeText(r6, r7, r5)
            r6.show()
            r1.cancel(r5)
            goto L_0x01a5
        L_0x0096:
            java.lang.String r7 = "Error deleting screen recording!"
            android.util.Log.e(r8, r7)
            int r7 = com.android.systemui.R$string.screenrecord_delete_error
            android.widget.Toast r6 = android.widget.Toast.makeText(r6, r7, r5)
            r6.show()
            goto L_0x01a5
        L_0x00a6:
            java.io.File r8 = new java.io.File
            java.lang.String r7 = r7.getStringExtra(r0)
            r8.<init>(r7)
            java.lang.String r7 = "com.android.systemui.fileprovider"
            android.net.Uri r7 = androidx.core.content.FileProvider.getUriForFile(r6, r7, r8)
            android.content.Intent r8 = new android.content.Intent
            java.lang.String r9 = "android.intent.action.SEND"
            r8.<init>(r9)
            java.lang.String r9 = "video/mp4"
            android.content.Intent r8 = r8.setType(r9)
            java.lang.String r9 = "android.intent.extra.STREAM"
            android.content.Intent r7 = r8.putExtra(r9, r7)
            android.content.res.Resources r8 = r6.getResources()
            int r9 = com.android.systemui.R$string.screenrecord_share_label
            java.lang.String r8 = r8.getString(r9)
            android.content.Intent r9 = new android.content.Intent
            r9.<init>(r2)
            r6.sendBroadcast(r9)
            r1.cancel(r5)
            android.content.Intent r7 = android.content.Intent.createChooser(r7, r8)
            r8 = 268435456(0x10000000, float:2.5243549E-29)
            android.content.Intent r7 = r7.setFlags(r8)
            r6.startActivity(r7)
            goto L_0x01a5
        L_0x00ed:
            android.media.MediaRecorder r7 = r6.mMediaRecorder
            r7.resume()
            r6.setNotificationActions(r4, r1)
            goto L_0x01a5
        L_0x00f7:
            android.media.MediaRecorder r7 = r6.mMediaRecorder
            r7.pause()
            r6.setNotificationActions(r5, r1)
            goto L_0x01a5
        L_0x0101:
            r6.stopRecording()
            java.io.File r7 = new java.io.File
            java.lang.String r8 = android.os.Environment.DIRECTORY_MOVIES
            java.io.File r8 = android.os.Environment.getExternalStoragePublicDirectory(r8)
            java.lang.String r9 = "Captures"
            r7.<init>(r8, r9)
            r7.mkdirs()
            java.text.SimpleDateFormat r8 = new java.text.SimpleDateFormat
            java.lang.String r9 = "'screen-'yyyyMMdd-HHmmss'.mp4'"
            r8.<init>(r9)
            java.util.Date r9 = new java.util.Date
            r9.<init>()
            java.lang.String r8 = r8.format(r9)
            java.io.File r9 = new java.io.File
            r9.<init>(r7, r8)
            java.nio.file.Path r7 = r9.toPath()
            java.io.File r8 = r6.mTempFile     // Catch:{ IOException -> 0x0140 }
            java.nio.file.Path r8 = r8.toPath()     // Catch:{ IOException -> 0x0140 }
            java.nio.file.CopyOption[] r9 = new java.nio.file.CopyOption[r4]     // Catch:{ IOException -> 0x0140 }
            java.nio.file.Files.move(r8, r7, r9)     // Catch:{ IOException -> 0x0140 }
            android.app.Notification r7 = r6.createSaveNotification(r7)     // Catch:{ IOException -> 0x0140 }
            r1.notify(r5, r7)     // Catch:{ IOException -> 0x0140 }
            goto L_0x01a5
        L_0x0140:
            r7 = move-exception
            r7.printStackTrace()
            int r7 = com.android.systemui.R$string.screenrecord_delete_error
            android.widget.Toast r6 = android.widget.Toast.makeText(r6, r7, r5)
            r6.show()
            goto L_0x01a5
        L_0x014e:
            r6.stopRecording()
            java.io.File r7 = r6.mTempFile
            boolean r7 = r7.delete()
            if (r7 != 0) goto L_0x0168
            java.lang.String r7 = "Error canceling screen recording!"
            android.util.Log.e(r8, r7)
            int r7 = com.android.systemui.R$string.screenrecord_delete_error
            android.widget.Toast r7 = android.widget.Toast.makeText(r6, r7, r5)
            r7.show()
            goto L_0x0171
        L_0x0168:
            int r7 = com.android.systemui.R$string.screenrecord_cancel_success
            android.widget.Toast r7 = android.widget.Toast.makeText(r6, r7, r5)
            r7.show()
        L_0x0171:
            android.content.Intent r7 = new android.content.Intent
            r7.<init>(r2)
            r6.sendBroadcast(r7)
            goto L_0x01a5
        L_0x017a:
            java.lang.String r8 = "extra_resultCode"
            int r8 = r7.getIntExtra(r8, r4)
            java.lang.String r9 = "extra_useAudio"
            boolean r9 = r7.getBooleanExtra(r9, r4)
            r6.mUseAudio = r9
            java.lang.String r9 = "extra_showTaps"
            boolean r9 = r7.getBooleanExtra(r9, r4)
            r6.mShowTaps = r9
            java.lang.String r9 = "extra_data"
            android.os.Parcelable r7 = r7.getParcelableExtra(r9)
            android.content.Intent r7 = (android.content.Intent) r7
            if (r7 == 0) goto L_0x01a5
            android.media.projection.MediaProjectionManager r9 = r6.mMediaProjectionManager
            android.media.projection.MediaProjection r7 = r9.getMediaProjection(r8, r7)
            r6.mMediaProjection = r7
            r6.startRecording()
        L_0x01a5:
            return r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.screenrecord.RecordingService.onStartCommand(android.content.Intent, int, int):int");
    }

    public void onCreate() {
        super.onCreate();
        this.mMediaProjectionManager = (MediaProjectionManager) getSystemService("media_projection");
    }

    private void startRecording() {
        try {
            this.mTempFile = File.createTempFile("temp", ".mp4");
            StringBuilder sb = new StringBuilder();
            sb.append("Writing video output to: ");
            sb.append(this.mTempFile.getAbsolutePath());
            Log.d("RecordingService", sb.toString());
            setTapsVisible(this.mShowTaps);
            this.mMediaRecorder = new MediaRecorder();
            if (this.mUseAudio) {
                this.mMediaRecorder.setAudioSource(1);
            }
            this.mMediaRecorder.setVideoSource(2);
            this.mMediaRecorder.setOutputFormat(2);
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int i = displayMetrics.widthPixels;
            int i2 = displayMetrics.heightPixels;
            this.mMediaRecorder.setVideoEncoder(2);
            this.mMediaRecorder.setVideoSize(i, i2);
            this.mMediaRecorder.setVideoFrameRate(30);
            this.mMediaRecorder.setVideoEncodingBitRate(6000000);
            if (this.mUseAudio) {
                this.mMediaRecorder.setAudioEncoder(1);
                this.mMediaRecorder.setAudioChannels(1);
                this.mMediaRecorder.setAudioEncodingBitRate(16);
                this.mMediaRecorder.setAudioSamplingRate(44100);
            }
            this.mMediaRecorder.setOutputFile(this.mTempFile);
            this.mMediaRecorder.prepare();
            this.mInputSurface = this.mMediaRecorder.getSurface();
            this.mVirtualDisplay = this.mMediaProjection.createVirtualDisplay("Recording Display", i, i2, displayMetrics.densityDpi, 16, this.mInputSurface, null, null);
            this.mMediaRecorder.start();
            createRecordingNotification();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void createRecordingNotification() {
        String str = "screen_record";
        NotificationChannel notificationChannel = new NotificationChannel(str, getString(R$string.screenrecord_name), 4);
        notificationChannel.setDescription(getString(R$string.screenrecord_channel_description));
        notificationChannel.enableVibration(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService("notification");
        notificationManager.createNotificationChannel(notificationChannel);
        this.mRecordingNotificationBuilder = new Builder(this, str).setSmallIcon(R$drawable.ic_android).setContentTitle(getResources().getString(R$string.screenrecord_name)).setUsesChronometer(true).setOngoing(true);
        setNotificationActions(false, notificationManager);
        startForeground(1, this.mRecordingNotificationBuilder.build());
    }

    private void setNotificationActions(boolean z, NotificationManager notificationManager) {
        int i;
        Resources resources = getResources();
        if (z) {
            i = R$string.screenrecord_resume_label;
        } else {
            i = R$string.screenrecord_pause_label;
        }
        this.mRecordingNotificationBuilder.setActions(new Action[]{new Action.Builder(Icon.createWithResource(this, R$drawable.ic_android), getResources().getString(R$string.screenrecord_stop_label), PendingIntent.getService(this, 2, getStopIntent(this), 134217728)).build(), new Action.Builder(Icon.createWithResource(this, R$drawable.ic_android), resources.getString(i), PendingIntent.getService(this, 2, z ? getResumeIntent(this) : getPauseIntent(this), 134217728)).build(), new Action.Builder(Icon.createWithResource(this, R$drawable.ic_android), getResources().getString(R$string.screenrecord_cancel_label), PendingIntent.getService(this, 2, getCancelIntent(this), 134217728)).build()});
        notificationManager.notify(1, this.mRecordingNotificationBuilder.build());
    }

    private Notification createSaveNotification(Path path) {
        Uri uriForFile = FileProvider.getUriForFile(this, "com.android.systemui.fileprovider", path.toFile());
        StringBuilder sb = new StringBuilder();
        sb.append("Screen recording saved to ");
        sb.append(path.toString());
        Log.d("RecordingService", sb.toString());
        Intent dataAndType = new Intent("android.intent.action.VIEW").setFlags(268435457).setDataAndType(uriForFile, "video/mp4");
        Action build = new Action.Builder(Icon.createWithResource(this, R$drawable.ic_android), getResources().getString(R$string.screenrecord_share_label), PendingIntent.getService(this, 2, getShareIntent(this, path.toString()), 134217728)).build();
        Builder autoCancel = new Builder(this, "screen_record").setSmallIcon(R$drawable.ic_android).setContentTitle(getResources().getString(R$string.screenrecord_name)).setContentText(getResources().getString(R$string.screenrecord_save_message)).setContentIntent(PendingIntent.getActivity(this, 2, dataAndType, 1)).addAction(build).addAction(new Action.Builder(Icon.createWithResource(this, R$drawable.ic_android), getResources().getString(R$string.screenrecord_delete_label), PendingIntent.getService(this, 2, getDeleteIntent(this, path.toString()), 134217728)).build()).setAutoCancel(true);
        Bitmap createVideoThumbnail = ThumbnailUtils.createVideoThumbnail(path.toString(), 1);
        if (createVideoThumbnail != null) {
            autoCancel.setLargeIcon(createVideoThumbnail).setStyle(new BigPictureStyle().bigPicture(createVideoThumbnail).bigLargeIcon(null));
        }
        return autoCancel.build();
    }

    private void stopRecording() {
        setTapsVisible(false);
        this.mMediaRecorder.stop();
        this.mMediaRecorder.release();
        this.mMediaRecorder = null;
        this.mMediaProjection.stop();
        this.mMediaProjection = null;
        this.mInputSurface.release();
        this.mVirtualDisplay.release();
        stopSelf();
    }

    private void setTapsVisible(boolean z) {
        System.putInt(getApplicationContext().getContentResolver(), "show_touches", z ? 1 : 0);
    }

    private static Intent getStopIntent(Context context) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.STOP");
    }

    private static Intent getPauseIntent(Context context) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.PAUSE");
    }

    private static Intent getResumeIntent(Context context) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.RESUME");
    }

    private static Intent getCancelIntent(Context context) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.CANCEL");
    }

    private static Intent getShareIntent(Context context, String str) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.SHARE").putExtra("extra_path", str);
    }

    private static Intent getDeleteIntent(Context context, String str) {
        return new Intent(context, RecordingService.class).setAction("com.android.systemui.screenrecord.DELETE").putExtra("extra_path", str);
    }
}

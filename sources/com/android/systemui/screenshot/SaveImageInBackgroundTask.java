package com.android.systemui.screenshot;

import android.app.Notification.BigPictureStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Picture;
import android.os.AsyncTask;
import com.android.systemui.R$drawable;
import com.android.systemui.R$string;
import com.android.systemui.SystemUI;
import com.android.systemui.util.NotificationChannels;
import java.text.SimpleDateFormat;
import java.util.Date;

/* compiled from: GlobalScreenshot */
class SaveImageInBackgroundTask extends AsyncTask<Void, Void, Void> {
    private final String mImageFileName = String.format("Screenshot_%s.png", new Object[]{new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(this.mImageTime))});
    private final int mImageHeight;
    private final long mImageTime = System.currentTimeMillis();
    private final int mImageWidth;
    private final Builder mNotificationBuilder;
    private final NotificationManager mNotificationManager;
    private final BigPictureStyle mNotificationStyle;
    private final SaveImageInBackgroundData mParams;
    private final Builder mPublicNotificationBuilder;

    SaveImageInBackgroundTask(Context context, SaveImageInBackgroundData saveImageInBackgroundData, NotificationManager notificationManager) {
        Context context2 = context;
        SaveImageInBackgroundData saveImageInBackgroundData2 = saveImageInBackgroundData;
        Resources resources = context.getResources();
        this.mParams = saveImageInBackgroundData2;
        this.mImageWidth = saveImageInBackgroundData2.image.getWidth();
        this.mImageHeight = saveImageInBackgroundData2.image.getHeight();
        int i = saveImageInBackgroundData2.iconSize;
        int i2 = saveImageInBackgroundData2.previewWidth;
        int i3 = saveImageInBackgroundData2.previewheight;
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0.25f);
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        Matrix matrix = new Matrix();
        matrix.setTranslate((float) ((i2 - this.mImageWidth) / 2), (float) ((i3 - this.mImageHeight) / 2));
        Paint paint2 = paint;
        Bitmap generateAdjustedHwBitmap = generateAdjustedHwBitmap(saveImageInBackgroundData2.image, i2, i3, matrix, paint2, 1090519039);
        float f = (float) i;
        float min = f / ((float) Math.min(this.mImageWidth, this.mImageHeight));
        matrix.setScale(min, min);
        matrix.postTranslate((f - (((float) this.mImageWidth) * min)) / 2.0f, (f - (min * ((float) this.mImageHeight))) / 2.0f);
        Bitmap generateAdjustedHwBitmap2 = generateAdjustedHwBitmap(saveImageInBackgroundData2.image, i, i, matrix, paint2, 1090519039);
        this.mNotificationManager = notificationManager;
        long currentTimeMillis = System.currentTimeMillis();
        this.mNotificationStyle = new BigPictureStyle().bigPicture(generateAdjustedHwBitmap.createAshmemBitmap());
        this.mPublicNotificationBuilder = new Builder(context2, NotificationChannels.SCREENSHOTS_HEADSUP).setContentTitle(resources.getString(R$string.screenshot_saving_title)).setSmallIcon(R$drawable.stat_notify_image).setCategory("progress").setWhen(currentTimeMillis).setShowWhen(true).setColor(resources.getColor(17170460));
        SystemUI.overrideNotificationAppName(context2, this.mPublicNotificationBuilder, true);
        this.mNotificationBuilder = new Builder(context2, NotificationChannels.SCREENSHOTS_HEADSUP).setContentTitle(resources.getString(R$string.screenshot_saving_title)).setSmallIcon(R$drawable.stat_notify_image).setWhen(currentTimeMillis).setShowWhen(true).setColor(resources.getColor(17170460)).setStyle(this.mNotificationStyle).setPublicVersion(this.mPublicNotificationBuilder.build());
        this.mNotificationBuilder.setFlag(32, true);
        SystemUI.overrideNotificationAppName(context2, this.mNotificationBuilder, true);
        this.mNotificationManager.notify(1, this.mNotificationBuilder.build());
        this.mNotificationBuilder.setLargeIcon(generateAdjustedHwBitmap2.createAshmemBitmap());
        this.mNotificationStyle.bigLargeIcon(null);
    }

    private Bitmap generateAdjustedHwBitmap(Bitmap bitmap, int i, int i2, Matrix matrix, Paint paint, int i3) {
        Picture picture = new Picture();
        Canvas beginRecording = picture.beginRecording(i, i2);
        beginRecording.drawColor(i3);
        beginRecording.drawBitmap(bitmap, matrix, paint);
        picture.endRecording();
        return Bitmap.createBitmap(picture);
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x018c, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x018d, code lost:
        if (r7 != null) goto L_0x018f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:?, code lost:
        r7.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x0197, code lost:
        throw r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.Void doInBackground(java.lang.Void... r15) {
        /*
            r14 = this;
            java.lang.String r15 = "android:screenshot_action_intent"
            java.lang.String r0 = "image/png"
            boolean r1 = r14.isCancelled()
            r2 = 0
            if (r1 == 0) goto L_0x000c
            return r2
        L_0x000c:
            r1 = -2
            android.os.Process.setThreadPriority(r1)
            com.android.systemui.screenshot.SaveImageInBackgroundData r1 = r14.mParams
            android.content.Context r3 = r1.context
            android.graphics.Bitmap r1 = r1.image
            android.content.res.Resources r4 = r3.getResources()
            android.provider.MediaStore$PendingParams r5 = new android.provider.MediaStore$PendingParams     // Catch:{ Exception -> 0x01a3 }
            android.net.Uri r6 = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r7 = r14.mImageFileName     // Catch:{ Exception -> 0x01a3 }
            r5.<init>(r6, r7, r0)     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r6 = android.os.Environment.DIRECTORY_PICTURES     // Catch:{ Exception -> 0x01a3 }
            r5.setPrimaryDirectory(r6)     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r6 = android.os.Environment.DIRECTORY_SCREENSHOTS     // Catch:{ Exception -> 0x01a3 }
            r5.setSecondaryDirectory(r6)     // Catch:{ Exception -> 0x01a3 }
            android.net.Uri r5 = android.provider.MediaStore.createPending(r3, r5)     // Catch:{ Exception -> 0x01a3 }
            android.provider.MediaStore$PendingSession r6 = android.provider.MediaStore.openPending(r3, r5)     // Catch:{ Exception -> 0x01a3 }
            java.io.OutputStream r7 = r6.openOutputStream()     // Catch:{ Exception -> 0x019a }
            android.graphics.Bitmap$CompressFormat r8 = android.graphics.Bitmap.CompressFormat.PNG     // Catch:{ all -> 0x018a }
            r9 = 100
            boolean r8 = r1.compress(r8, r9, r7)     // Catch:{ all -> 0x018a }
            if (r8 == 0) goto L_0x0182
            if (r7 == 0) goto L_0x0048
            r7.close()     // Catch:{ Exception -> 0x019a }
        L_0x0048:
            r6.publish()     // Catch:{ Exception -> 0x019a }
            libcore.io.IoUtils.closeQuietly(r6)     // Catch:{ Exception -> 0x01a3 }
            java.text.DateFormat r6 = java.text.DateFormat.getDateTimeInstance()     // Catch:{ Exception -> 0x01a3 }
            java.util.Date r7 = new java.util.Date     // Catch:{ Exception -> 0x01a3 }
            long r8 = r14.mImageTime     // Catch:{ Exception -> 0x01a3 }
            r7.<init>(r8)     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r6 = r6.format(r7)     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r7 = "Screenshot (%s)"
            r8 = 1
            java.lang.Object[] r9 = new java.lang.Object[r8]     // Catch:{ Exception -> 0x01a3 }
            r10 = 0
            r9[r10] = r6     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r6 = java.lang.String.format(r7, r9)     // Catch:{ Exception -> 0x01a3 }
            android.content.Intent r7 = new android.content.Intent     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r9 = "android.intent.action.SEND"
            r7.<init>(r9)     // Catch:{ Exception -> 0x01a3 }
            r7.setType(r0)     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r9 = "android.intent.extra.STREAM"
            r7.putExtra(r9, r5)     // Catch:{ Exception -> 0x01a3 }
            android.content.ClipData r9 = new android.content.ClipData     // Catch:{ Exception -> 0x01a3 }
            android.content.ClipDescription r11 = new android.content.ClipDescription     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r12 = "content"
            java.lang.String r13 = "text/plain"
            java.lang.String[] r13 = new java.lang.String[]{r13}     // Catch:{ Exception -> 0x01a3 }
            r11.<init>(r12, r13)     // Catch:{ Exception -> 0x01a3 }
            android.content.ClipData$Item r12 = new android.content.ClipData$Item     // Catch:{ Exception -> 0x01a3 }
            r12.<init>(r5)     // Catch:{ Exception -> 0x01a3 }
            r9.<init>(r11, r12)     // Catch:{ Exception -> 0x01a3 }
            r7.setClipData(r9)     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r9 = "android.intent.extra.SUBJECT"
            r7.putExtra(r9, r6)     // Catch:{ Exception -> 0x01a3 }
            r7.addFlags(r8)     // Catch:{ Exception -> 0x01a3 }
            android.content.Intent r6 = new android.content.Intent     // Catch:{ Exception -> 0x01a3 }
            java.lang.Class<com.android.systemui.screenshot.GlobalScreenshot$TargetChosenReceiver> r9 = com.android.systemui.screenshot.GlobalScreenshot.TargetChosenReceiver.class
            r6.<init>(r3, r9)     // Catch:{ Exception -> 0x01a3 }
            r9 = 1342177280(0x50000000, float:8.5899346E9)
            android.app.PendingIntent r6 = android.app.PendingIntent.getBroadcast(r3, r10, r6, r9)     // Catch:{ Exception -> 0x01a3 }
            android.content.IntentSender r6 = r6.getIntentSender()     // Catch:{ Exception -> 0x01a3 }
            android.content.Intent r6 = android.content.Intent.createChooser(r7, r2, r6)     // Catch:{ Exception -> 0x01a3 }
            r7 = 268468224(0x10008000, float:2.5342157E-29)
            android.content.Intent r6 = r6.addFlags(r7)     // Catch:{ Exception -> 0x01a3 }
            android.content.Intent r6 = r6.addFlags(r8)     // Catch:{ Exception -> 0x01a3 }
            android.content.Intent r7 = new android.content.Intent     // Catch:{ Exception -> 0x01a3 }
            java.lang.Class<com.android.systemui.screenshot.GlobalScreenshot$ActionProxyReceiver> r11 = com.android.systemui.screenshot.GlobalScreenshot.ActionProxyReceiver.class
            r7.<init>(r3, r11)     // Catch:{ Exception -> 0x01a3 }
            android.content.Intent r6 = r7.putExtra(r15, r6)     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r7 = "android:screenshot_disallow_enter_pip"
            android.content.Intent r6 = r6.putExtra(r7, r8)     // Catch:{ Exception -> 0x01a3 }
            android.os.UserHandle r7 = android.os.UserHandle.SYSTEM     // Catch:{ Exception -> 0x01a3 }
            r11 = 268435456(0x10000000, float:2.5243549E-29)
            android.app.PendingIntent r6 = android.app.PendingIntent.getBroadcastAsUser(r3, r10, r6, r11, r7)     // Catch:{ Exception -> 0x01a3 }
            android.app.Notification$Action$Builder r7 = new android.app.Notification$Action$Builder     // Catch:{ Exception -> 0x01a3 }
            int r12 = com.android.systemui.R$drawable.ic_screenshot_share     // Catch:{ Exception -> 0x01a3 }
            r13 = 17041023(0x104067f, float:2.4249232E-38)
            java.lang.String r13 = r4.getString(r13)     // Catch:{ Exception -> 0x01a3 }
            r7.<init>(r12, r13, r6)     // Catch:{ Exception -> 0x01a3 }
            android.app.Notification$Builder r6 = r14.mNotificationBuilder     // Catch:{ Exception -> 0x01a3 }
            android.app.Notification$Action r7 = r7.build()     // Catch:{ Exception -> 0x01a3 }
            r6.addAction(r7)     // Catch:{ Exception -> 0x01a3 }
            int r6 = com.android.systemui.R$string.config_screenshotEditor     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r6 = r3.getString(r6)     // Catch:{ Exception -> 0x01a3 }
            android.content.Intent r7 = new android.content.Intent     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r12 = "android.intent.action.EDIT"
            r7.<init>(r12)     // Catch:{ Exception -> 0x01a3 }
            boolean r12 = android.text.TextUtils.isEmpty(r6)     // Catch:{ Exception -> 0x01a3 }
            if (r12 != 0) goto L_0x0105
            android.content.ComponentName r6 = android.content.ComponentName.unflattenFromString(r6)     // Catch:{ Exception -> 0x01a3 }
            r7.setComponent(r6)     // Catch:{ Exception -> 0x01a3 }
        L_0x0105:
            r7.setType(r0)     // Catch:{ Exception -> 0x01a3 }
            r7.setData(r5)     // Catch:{ Exception -> 0x01a3 }
            r7.addFlags(r8)     // Catch:{ Exception -> 0x01a3 }
            r0 = 2
            r7.addFlags(r0)     // Catch:{ Exception -> 0x01a3 }
            android.content.Intent r0 = new android.content.Intent     // Catch:{ Exception -> 0x01a3 }
            java.lang.Class<com.android.systemui.screenshot.GlobalScreenshot$ActionProxyReceiver> r6 = com.android.systemui.screenshot.GlobalScreenshot.ActionProxyReceiver.class
            r0.<init>(r3, r6)     // Catch:{ Exception -> 0x01a3 }
            android.content.Intent r15 = r0.putExtra(r15, r7)     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r0 = "android:screenshot_cancel_notification"
            android.content.ComponentName r6 = r7.getComponent()     // Catch:{ Exception -> 0x01a3 }
            if (r6 == 0) goto L_0x0127
            r6 = r8
            goto L_0x0128
        L_0x0127:
            r6 = r10
        L_0x0128:
            android.content.Intent r15 = r15.putExtra(r0, r6)     // Catch:{ Exception -> 0x01a3 }
            android.os.UserHandle r0 = android.os.UserHandle.SYSTEM     // Catch:{ Exception -> 0x01a3 }
            android.app.PendingIntent r15 = android.app.PendingIntent.getBroadcastAsUser(r3, r8, r15, r11, r0)     // Catch:{ Exception -> 0x01a3 }
            android.app.Notification$Action$Builder r0 = new android.app.Notification$Action$Builder     // Catch:{ Exception -> 0x01a3 }
            int r6 = com.android.systemui.R$drawable.ic_screenshot_edit     // Catch:{ Exception -> 0x01a3 }
            r7 = 17040984(0x1040658, float:2.4249122E-38)
            java.lang.String r7 = r4.getString(r7)     // Catch:{ Exception -> 0x01a3 }
            r0.<init>(r6, r7, r15)     // Catch:{ Exception -> 0x01a3 }
            android.app.Notification$Builder r15 = r14.mNotificationBuilder     // Catch:{ Exception -> 0x01a3 }
            android.app.Notification$Action r0 = r0.build()     // Catch:{ Exception -> 0x01a3 }
            r15.addAction(r0)     // Catch:{ Exception -> 0x01a3 }
            android.content.Intent r15 = new android.content.Intent     // Catch:{ Exception -> 0x01a3 }
            java.lang.Class<com.android.systemui.screenshot.GlobalScreenshot$DeleteScreenshotReceiver> r0 = com.android.systemui.screenshot.GlobalScreenshot.DeleteScreenshotReceiver.class
            r15.<init>(r3, r0)     // Catch:{ Exception -> 0x01a3 }
            java.lang.String r0 = "android:screenshot_uri_id"
            java.lang.String r6 = r5.toString()     // Catch:{ Exception -> 0x01a3 }
            android.content.Intent r15 = r15.putExtra(r0, r6)     // Catch:{ Exception -> 0x01a3 }
            android.app.PendingIntent r15 = android.app.PendingIntent.getBroadcast(r3, r10, r15, r9)     // Catch:{ Exception -> 0x01a3 }
            android.app.Notification$Action$Builder r0 = new android.app.Notification$Action$Builder     // Catch:{ Exception -> 0x01a3 }
            int r3 = com.android.systemui.R$drawable.ic_screenshot_delete     // Catch:{ Exception -> 0x01a3 }
            r6 = 17039866(0x10401fa, float:2.424599E-38)
            java.lang.String r4 = r4.getString(r6)     // Catch:{ Exception -> 0x01a3 }
            r0.<init>(r3, r4, r15)     // Catch:{ Exception -> 0x01a3 }
            android.app.Notification$Builder r15 = r14.mNotificationBuilder     // Catch:{ Exception -> 0x01a3 }
            android.app.Notification$Action r0 = r0.build()     // Catch:{ Exception -> 0x01a3 }
            r15.addAction(r0)     // Catch:{ Exception -> 0x01a3 }
            com.android.systemui.screenshot.SaveImageInBackgroundData r15 = r14.mParams     // Catch:{ Exception -> 0x01a3 }
            r15.imageUri = r5     // Catch:{ Exception -> 0x01a3 }
            com.android.systemui.screenshot.SaveImageInBackgroundData r15 = r14.mParams     // Catch:{ Exception -> 0x01a3 }
            r15.image = r2     // Catch:{ Exception -> 0x01a3 }
            com.android.systemui.screenshot.SaveImageInBackgroundData r15 = r14.mParams     // Catch:{ Exception -> 0x01a3 }
            r15.errorMsgResId = r10     // Catch:{ Exception -> 0x01a3 }
            goto L_0x01b7
        L_0x0182:
            java.io.IOException r15 = new java.io.IOException     // Catch:{ all -> 0x018a }
            java.lang.String r0 = "Failed to compress"
            r15.<init>(r0)     // Catch:{ all -> 0x018a }
            throw r15     // Catch:{ all -> 0x018a }
        L_0x018a:
            r15 = move-exception
            throw r15     // Catch:{ all -> 0x018c }
        L_0x018c:
            r0 = move-exception
            if (r7 == 0) goto L_0x0197
            r7.close()     // Catch:{ all -> 0x0193 }
            goto L_0x0197
        L_0x0193:
            r3 = move-exception
            r15.addSuppressed(r3)     // Catch:{ Exception -> 0x019a }
        L_0x0197:
            throw r0     // Catch:{ Exception -> 0x019a }
        L_0x0198:
            r15 = move-exception
            goto L_0x019f
        L_0x019a:
            r15 = move-exception
            r6.abandon()     // Catch:{ all -> 0x0198 }
            throw r15     // Catch:{ all -> 0x0198 }
        L_0x019f:
            libcore.io.IoUtils.closeQuietly(r6)     // Catch:{ Exception -> 0x01a3 }
            throw r15     // Catch:{ Exception -> 0x01a3 }
        L_0x01a3:
            r15 = move-exception
            java.lang.String r0 = "SaveImageInBackgroundTask"
            java.lang.String r3 = "unable to save screenshot"
            android.util.Slog.e(r0, r3, r15)
            com.android.systemui.screenshot.SaveImageInBackgroundData r15 = r14.mParams
            r15.clearImage()
            com.android.systemui.screenshot.SaveImageInBackgroundData r14 = r14.mParams
            int r15 = com.android.systemui.R$string.screenshot_failed_to_save_text
            r14.errorMsgResId = r15
        L_0x01b7:
            if (r1 == 0) goto L_0x01bc
            r1.recycle()
        L_0x01bc:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.screenshot.SaveImageInBackgroundTask.doInBackground(java.lang.Void[]):java.lang.Void");
    }

    /* access modifiers changed from: protected */
    public void onPostExecute(Void voidR) {
        SaveImageInBackgroundData saveImageInBackgroundData = this.mParams;
        int i = saveImageInBackgroundData.errorMsgResId;
        if (i != 0) {
            GlobalScreenshot.notifyScreenshotError(saveImageInBackgroundData.context, this.mNotificationManager, i);
        } else {
            Context context = saveImageInBackgroundData.context;
            Resources resources = context.getResources();
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.setDataAndType(this.mParams.imageUri, "image/png");
            intent.setFlags(268435457);
            long currentTimeMillis = System.currentTimeMillis();
            this.mPublicNotificationBuilder.setContentTitle(resources.getString(R$string.screenshot_saved_title)).setContentText(resources.getString(R$string.screenshot_saved_text)).setContentIntent(PendingIntent.getActivity(this.mParams.context, 0, intent, 0)).setWhen(currentTimeMillis).setAutoCancel(true).setColor(context.getColor(17170460));
            this.mNotificationBuilder.setContentTitle(resources.getString(R$string.screenshot_saved_title)).setContentText(resources.getString(R$string.screenshot_saved_text)).setContentIntent(PendingIntent.getActivity(this.mParams.context, 0, intent, 0)).setWhen(currentTimeMillis).setAutoCancel(true).setColor(context.getColor(17170460)).setPublicVersion(this.mPublicNotificationBuilder.build()).setFlag(32, false);
            this.mNotificationManager.notify(1, this.mNotificationBuilder.build());
        }
        this.mParams.finisher.run();
        this.mParams.clearContext();
    }

    /* access modifiers changed from: protected */
    public void onCancelled(Void voidR) {
        this.mParams.finisher.run();
        this.mParams.clearImage();
        this.mParams.clearContext();
        this.mNotificationManager.cancel(1);
    }
}

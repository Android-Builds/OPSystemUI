package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaController.Callback;
import android.media.session.MediaSession.Token;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Trace;
import android.provider.DeviceConfig;
import android.provider.DeviceConfig.OnPropertiesChangedListener;
import android.provider.DeviceConfig.Properties;
import android.util.ArraySet;
import android.util.Log;
import android.widget.ImageView;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.notification.NotificationEntryListener;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.LockscreenWallpaper;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.ShadeController;
import com.android.systemui.statusbar.phone.StatusBarWindowController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.oneplus.util.OpUtils;
import dagger.Lazy;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

public class NotificationMediaManager implements Dumpable {
    public static final boolean DEBUG_MEDIA = (OP_DEBUG | false);
    public static final boolean OP_DEBUG = OpUtils.DEBUG_ONEPLUS;
    private BackDropView mBackdrop;
    private ImageView mBackdropBack;
    /* access modifiers changed from: private */
    public ImageView mBackdropFront;
    private BiometricUnlockController mBiometricUnlockController;
    private final SysuiColorExtractor mColorExtractor = ((SysuiColorExtractor) Dependency.get(SysuiColorExtractor.class));
    private final Context mContext;
    private NotificationEntryManager mEntryManager;
    private final Handler mHandler = ((Handler) Dependency.get(Dependency.MAIN_HANDLER));
    protected final Runnable mHideBackdropFront = new Runnable() {
        public void run() {
            if (NotificationMediaManager.DEBUG_MEDIA) {
                Log.v("NotificationMediaManager", "DEBUG_MEDIA: removing fade layer");
            }
            NotificationMediaManager.this.mBackdropFront.setVisibility(4);
            NotificationMediaManager.this.mBackdropFront.animate().cancel();
            NotificationMediaManager.this.mBackdropFront.setImageDrawable(null);
        }
    };
    private final KeyguardMonitor mKeyguardMonitor = ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class));
    private LockscreenWallpaper mLockscreenWallpaper;
    /* access modifiers changed from: private */
    public final MediaArtworkProcessor mMediaArtworkProcessor;
    private MediaController mMediaController;
    private final Callback mMediaListener = new Callback() {
        public void onPlaybackStateChanged(PlaybackState playbackState) {
            super.onPlaybackStateChanged(playbackState);
            if (NotificationMediaManager.DEBUG_MEDIA) {
                StringBuilder sb = new StringBuilder();
                sb.append("DEBUG_MEDIA: onPlaybackStateChanged: ");
                sb.append(playbackState);
                Log.v("NotificationMediaManager", sb.toString());
            }
            if (playbackState != null) {
                if (!NotificationMediaManager.this.isPlaybackActive(playbackState.getState())) {
                    NotificationMediaManager.this.clearCurrentMediaNotification();
                }
                NotificationMediaManager.this.dispatchUpdateMediaMetaData(true, true);
            }
        }

        public void onMetadataChanged(MediaMetadata mediaMetadata) {
            super.onMetadataChanged(mediaMetadata);
            if (NotificationMediaManager.DEBUG_MEDIA) {
                StringBuilder sb = new StringBuilder();
                sb.append("DEBUG_MEDIA: onMetadataChanged: ");
                sb.append(mediaMetadata);
                Log.v("NotificationMediaManager", sb.toString());
            }
            NotificationMediaManager.this.mMediaArtworkProcessor.clearCache();
            NotificationMediaManager.this.mMediaMetadata = mediaMetadata;
            NotificationMediaManager.this.dispatchUpdateMediaMetaData(true, true);
        }
    };
    private final ArrayList<MediaListener> mMediaListeners;
    /* access modifiers changed from: private */
    public MediaMetadata mMediaMetadata;
    private String mMediaNotificationKey;
    private final MediaSessionManager mMediaSessionManager;
    protected NotificationPresenter mPresenter;
    private final Set<AsyncTask<?, ?, ?>> mProcessArtworkTasks = new ArraySet();
    private final OnPropertiesChangedListener mPropertiesChangedListener = new OnPropertiesChangedListener() {
        public void onPropertiesChanged(Properties properties) {
            for (String str : properties.getKeyset()) {
                if ("compact_media_notification_seekbar_enabled".equals(str)) {
                    String string = properties.getString(str, null);
                    if (NotificationMediaManager.DEBUG_MEDIA) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("DEBUG_MEDIA: compact media seekbar flag updated: ");
                        sb.append(string);
                        Log.v("NotificationMediaManager", sb.toString());
                    }
                    NotificationMediaManager.this.mShowCompactMediaSeekbar = "true".equals(string);
                }
            }
        }
    };
    private ScrimController mScrimController;
    private Lazy<ShadeController> mShadeController;
    /* access modifiers changed from: private */
    public boolean mShowCompactMediaSeekbar;
    private final StatusBarStateController mStatusBarStateController = ((StatusBarStateController) Dependency.get(StatusBarStateController.class));
    private Lazy<StatusBarWindowController> mStatusBarWindowController;

    public interface MediaListener {
        void onMetadataOrStateChanged(MediaMetadata mediaMetadata, int i);
    }

    private static final class ProcessArtworkTask extends AsyncTask<Bitmap, Void, Bitmap> {
        private final boolean mAllowEnterAnimation;
        private final WeakReference<NotificationMediaManager> mManagerRef;
        private final boolean mMetaDataChanged;

        ProcessArtworkTask(NotificationMediaManager notificationMediaManager, boolean z, boolean z2) {
            this.mManagerRef = new WeakReference<>(notificationMediaManager);
            this.mMetaDataChanged = z;
            this.mAllowEnterAnimation = z2;
        }

        /* access modifiers changed from: protected */
        public Bitmap doInBackground(Bitmap... bitmapArr) {
            NotificationMediaManager notificationMediaManager = (NotificationMediaManager) this.mManagerRef.get();
            if (notificationMediaManager == null || bitmapArr.length == 0 || isCancelled()) {
                return null;
            }
            return notificationMediaManager.processArtwork(bitmapArr[0]);
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Bitmap bitmap) {
            NotificationMediaManager notificationMediaManager = (NotificationMediaManager) this.mManagerRef.get();
            if (notificationMediaManager != null && !isCancelled()) {
                notificationMediaManager.removeTask(this);
                notificationMediaManager.finishUpdateMediaMetaData(this.mMetaDataChanged, this.mAllowEnterAnimation, bitmap);
            }
        }

        /* access modifiers changed from: protected */
        public void onCancelled(Bitmap bitmap) {
            if (bitmap != null) {
                bitmap.recycle();
            }
            NotificationMediaManager notificationMediaManager = (NotificationMediaManager) this.mManagerRef.get();
            if (notificationMediaManager != null) {
                notificationMediaManager.removeTask(this);
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isPlaybackActive(int i) {
        return (i == 1 || i == 7 || i == 0) ? false : true;
    }

    public NotificationMediaManager(Context context, Lazy<ShadeController> lazy, Lazy<StatusBarWindowController> lazy2, NotificationEntryManager notificationEntryManager, MediaArtworkProcessor mediaArtworkProcessor) {
        this.mContext = context;
        this.mMediaArtworkProcessor = mediaArtworkProcessor;
        this.mMediaListeners = new ArrayList<>();
        this.mMediaSessionManager = (MediaSessionManager) this.mContext.getSystemService("media_session");
        this.mShadeController = lazy;
        this.mStatusBarWindowController = lazy2;
        this.mEntryManager = notificationEntryManager;
        notificationEntryManager.addNotificationEntryListener(new NotificationEntryListener() {
            public void onEntryRemoved(NotificationEntry notificationEntry, NotificationVisibility notificationVisibility, boolean z) {
                NotificationMediaManager.this.onNotificationRemoved(notificationEntry.key);
            }
        });
        String str = "systemui";
        this.mShowCompactMediaSeekbar = "true".equals(DeviceConfig.getProperty(str, "compact_media_notification_seekbar_enabled"));
        DeviceConfig.addOnPropertiesChangedListener(str, this.mContext.getMainExecutor(), this.mPropertiesChangedListener);
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter) {
        this.mPresenter = notificationPresenter;
    }

    public void onNotificationRemoved(String str) {
        if (DEBUG_MEDIA) {
            StringBuilder sb = new StringBuilder();
            sb.append("DEBUG_MEDIA: onNotificationRemoved / key: ");
            sb.append(str);
            sb.append(" / mMediaNotificationKey:");
            sb.append(this.mMediaNotificationKey);
            Log.v("NotificationMediaManager", sb.toString());
        }
        if (str.equals(this.mMediaNotificationKey)) {
            clearCurrentMediaNotification();
            dispatchUpdateMediaMetaData(true, true);
        }
    }

    public String getMediaNotificationKey() {
        return this.mMediaNotificationKey;
    }

    public MediaMetadata getMediaMetadata() {
        return this.mMediaMetadata;
    }

    public boolean getShowCompactMediaSeekbar() {
        return this.mShowCompactMediaSeekbar;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0029, code lost:
        return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public android.graphics.drawable.Icon getMediaIcon() {
        /*
            r3 = this;
            java.lang.String r0 = r3.mMediaNotificationKey
            r1 = 0
            if (r0 != 0) goto L_0x0006
            return r1
        L_0x0006:
            com.android.systemui.statusbar.notification.NotificationEntryManager r0 = r3.mEntryManager
            com.android.systemui.statusbar.notification.collection.NotificationData r0 = r0.getNotificationData()
            monitor-enter(r0)
            com.android.systemui.statusbar.notification.NotificationEntryManager r2 = r3.mEntryManager     // Catch:{ all -> 0x002a }
            com.android.systemui.statusbar.notification.collection.NotificationData r2 = r2.getNotificationData()     // Catch:{ all -> 0x002a }
            java.lang.String r3 = r3.mMediaNotificationKey     // Catch:{ all -> 0x002a }
            com.android.systemui.statusbar.notification.collection.NotificationEntry r3 = r2.get(r3)     // Catch:{ all -> 0x002a }
            if (r3 == 0) goto L_0x0028
            com.android.systemui.statusbar.StatusBarIconView r2 = r3.expandedIcon     // Catch:{ all -> 0x002a }
            if (r2 != 0) goto L_0x0020
            goto L_0x0028
        L_0x0020:
            com.android.systemui.statusbar.StatusBarIconView r3 = r3.expandedIcon     // Catch:{ all -> 0x002a }
            android.graphics.drawable.Icon r3 = r3.getSourceIcon()     // Catch:{ all -> 0x002a }
            monitor-exit(r0)     // Catch:{ all -> 0x002a }
            return r3
        L_0x0028:
            monitor-exit(r0)     // Catch:{ all -> 0x002a }
            return r1
        L_0x002a:
            r3 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x002a }
            throw r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.NotificationMediaManager.getMediaIcon():android.graphics.drawable.Icon");
    }

    public void addCallback(MediaListener mediaListener) {
        this.mMediaListeners.add(mediaListener);
        mediaListener.onMetadataOrStateChanged(this.mMediaMetadata, getMediaControllerPlaybackState(this.mMediaController));
    }

    public void removeCallback(MediaListener mediaListener) {
        this.mMediaListeners.remove(mediaListener);
    }

    public void findAndUpdateMediaNotifications() {
        boolean z;
        NotificationEntry notificationEntry;
        MediaController mediaController;
        synchronized (this.mEntryManager.getNotificationData()) {
            ArrayList activeNotifications = this.mEntryManager.getNotificationData().getActiveNotifications();
            int size = activeNotifications.size();
            z = false;
            int i = 0;
            while (true) {
                if (i >= size) {
                    notificationEntry = null;
                    mediaController = null;
                    break;
                }
                notificationEntry = (NotificationEntry) activeNotifications.get(i);
                if (notificationEntry.isMediaNotification()) {
                    Token token = (Token) notificationEntry.notification.getNotification().extras.getParcelable("android.mediaSession");
                    if (token != null) {
                        mediaController = new MediaController(this.mContext, token);
                        if (3 == getMediaControllerPlaybackState(mediaController)) {
                            if (DEBUG_MEDIA) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("DEBUG_MEDIA: found mediastyle controller matching ");
                                sb.append(notificationEntry.notification.getKey());
                                Log.v("NotificationMediaManager", sb.toString());
                            }
                        }
                    } else {
                        continue;
                    }
                }
                i++;
            }
            if (notificationEntry == null && this.mMediaSessionManager != null) {
                for (MediaController mediaController2 : this.mMediaSessionManager.getActiveSessionsForUser(null, -1)) {
                    if (3 == getMediaControllerPlaybackState(mediaController2)) {
                        String packageName = mediaController2.getPackageName();
                        int i2 = 0;
                        while (true) {
                            if (i2 >= size) {
                                break;
                            }
                            NotificationEntry notificationEntry2 = (NotificationEntry) activeNotifications.get(i2);
                            if (notificationEntry2.notification.getPackageName().equals(packageName)) {
                                if (DEBUG_MEDIA) {
                                    StringBuilder sb2 = new StringBuilder();
                                    sb2.append("DEBUG_MEDIA: found controller matching ");
                                    sb2.append(notificationEntry2.notification.getKey());
                                    Log.v("NotificationMediaManager", sb2.toString());
                                }
                                mediaController = mediaController2;
                                notificationEntry = notificationEntry2;
                            } else {
                                i2++;
                            }
                        }
                    }
                }
            }
            if (mediaController != null && !sameSessions(this.mMediaController, mediaController)) {
                clearCurrentMediaNotificationSession();
                this.mMediaController = mediaController;
                this.mMediaController.registerCallback(this.mMediaListener);
                this.mMediaMetadata = this.mMediaController.getMetadata();
                if (DEBUG_MEDIA) {
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("DEBUG_MEDIA: insert listener, found new controller: ");
                    sb3.append(this.mMediaController);
                    sb3.append(", receive metadata: ");
                    sb3.append(this.mMediaMetadata);
                    Log.v("NotificationMediaManager", sb3.toString());
                }
                z = true;
            }
            if (notificationEntry != null && !notificationEntry.notification.getKey().equals(this.mMediaNotificationKey)) {
                this.mMediaNotificationKey = notificationEntry.notification.getKey();
                if (DEBUG_MEDIA) {
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append("DEBUG_MEDIA: Found new media notification: key=");
                    sb4.append(this.mMediaNotificationKey);
                    Log.v("NotificationMediaManager", sb4.toString());
                }
            }
        }
        if (z) {
            this.mEntryManager.updateNotifications();
        }
        dispatchUpdateMediaMetaData(z, true);
    }

    public void clearCurrentMediaNotification() {
        this.mMediaNotificationKey = null;
        clearCurrentMediaNotificationSession();
    }

    /* access modifiers changed from: private */
    public void dispatchUpdateMediaMetaData(boolean z, boolean z2) {
        NotificationPresenter notificationPresenter = this.mPresenter;
        if (notificationPresenter != null) {
            notificationPresenter.updateMediaMetaData(z, z2);
        }
        int mediaControllerPlaybackState = getMediaControllerPlaybackState(this.mMediaController);
        ArrayList arrayList = new ArrayList(this.mMediaListeners);
        for (int i = 0; i < arrayList.size(); i++) {
            ((MediaListener) arrayList.get(i)).onMetadataOrStateChanged(this.mMediaMetadata, mediaControllerPlaybackState);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("    mMediaSessionManager=");
        printWriter.println(this.mMediaSessionManager);
        printWriter.print("    mMediaNotificationKey=");
        printWriter.println(this.mMediaNotificationKey);
        printWriter.print("    mMediaController=");
        printWriter.print(this.mMediaController);
        if (this.mMediaController != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(" state=");
            sb.append(this.mMediaController.getPlaybackState());
            printWriter.print(sb.toString());
        }
        printWriter.println();
        printWriter.print("    mMediaMetadata=");
        printWriter.print(this.mMediaMetadata);
        if (this.mMediaMetadata != null) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(" title=");
            sb2.append(this.mMediaMetadata.getText("android.media.metadata.TITLE"));
            printWriter.print(sb2.toString());
        }
        printWriter.println();
    }

    private boolean sameSessions(MediaController mediaController, MediaController mediaController2) {
        if (mediaController == mediaController2) {
            return true;
        }
        if (mediaController == null) {
            return false;
        }
        return mediaController.controlsSameSession(mediaController2);
    }

    private int getMediaControllerPlaybackState(MediaController mediaController) {
        if (mediaController != null) {
            PlaybackState playbackState = mediaController.getPlaybackState();
            if (playbackState != null) {
                return playbackState.getState();
            }
        }
        return 0;
    }

    private void clearCurrentMediaNotificationSession() {
        this.mMediaArtworkProcessor.clearCache();
        this.mMediaMetadata = null;
        if (this.mMediaController != null) {
            if (DEBUG_MEDIA) {
                StringBuilder sb = new StringBuilder();
                sb.append("DEBUG_MEDIA: Disconnecting from old controller: ");
                sb.append(this.mMediaController.getPackageName());
                Log.v("NotificationMediaManager", sb.toString());
            }
            this.mMediaController.unregisterCallback(this.mMediaListener);
        }
        this.mMediaController = null;
    }

    public void updateMediaMetaData(boolean z, boolean z2) {
        Bitmap bitmap;
        Trace.beginSection("StatusBar#updateMediaMetaData");
        if (this.mBackdrop == null) {
            Trace.endSection();
            return;
        }
        BiometricUnlockController biometricUnlockController = this.mBiometricUnlockController;
        boolean isFacelockUnlocking = (biometricUnlockController != null && biometricUnlockController.isWakeAndUnlock()) | KeyguardUpdateMonitor.getInstance(this.mContext).isFacelockUnlocking();
        if (this.mKeyguardMonitor.isLaunchTransitionFadingAway() || isFacelockUnlocking) {
            this.mBackdrop.setVisibility(4);
            Trace.endSection();
            return;
        }
        MediaMetadata mediaMetadata = getMediaMetadata();
        String str = "NotificationMediaManager";
        if (DEBUG_MEDIA) {
            StringBuilder sb = new StringBuilder();
            sb.append("DEBUG_MEDIA: updating album art for notification ");
            sb.append(getMediaNotificationKey());
            sb.append(" metadata=");
            sb.append(mediaMetadata);
            sb.append(" metaDataChanged=");
            sb.append(z);
            sb.append(" state=");
            sb.append(this.mStatusBarStateController.getState());
            Log.v(str, sb.toString());
        }
        if (mediaMetadata != null) {
            Bitmap bitmap2 = mediaMetadata.getBitmap("android.media.metadata.ART");
            bitmap = bitmap2 == null ? mediaMetadata.getBitmap("android.media.metadata.ALBUM_ART") : bitmap2;
        } else {
            bitmap = null;
        }
        if (z) {
            for (AsyncTask cancel : this.mProcessArtworkTasks) {
                cancel.cancel(true);
            }
            this.mProcessArtworkTasks.clear();
        }
        ShadeController shadeController = (ShadeController) this.mShadeController.get();
        boolean z3 = shadeController != null && shadeController.isOccluded();
        if (bitmap == null || z3 || !this.mKeyguardMonitor.isShowing()) {
            finishUpdateMediaMetaData(z, z2, null);
        } else {
            try {
                this.mProcessArtworkTasks.add(new ProcessArtworkTask(this, z, z2).execute(new Bitmap[]{bitmap}));
            } catch (Exception e) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("DEBUG_MEDIA: ");
                sb2.append(e.toString());
                Log.w(str, sb2.toString());
            }
        }
        Trace.endSection();
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Removed duplicated region for block: B:105:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0064  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x0066  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x006d  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00ec  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x0115  */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x014a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void finishUpdateMediaMetaData(boolean r13, boolean r14, android.graphics.Bitmap r15) {
        /*
            r12 = this;
            r0 = 0
            if (r15 == 0) goto L_0x000f
            android.graphics.drawable.BitmapDrawable r1 = new android.graphics.drawable.BitmapDrawable
            android.widget.ImageView r2 = r12.mBackdropBack
            android.content.res.Resources r2 = r2.getResources()
            r1.<init>(r2, r15)
            goto L_0x0010
        L_0x000f:
            r1 = r0
        L_0x0010:
            r15 = 1
            r2 = 0
            if (r1 == 0) goto L_0x0016
            r3 = r15
            goto L_0x0017
        L_0x0016:
            r3 = r2
        L_0x0017:
            if (r1 != 0) goto L_0x0046
            com.android.systemui.statusbar.phone.LockscreenWallpaper r4 = r12.mLockscreenWallpaper
            if (r4 == 0) goto L_0x0022
            android.graphics.Bitmap r4 = r4.getBitmap()
            goto L_0x0023
        L_0x0022:
            r4 = r0
        L_0x0023:
            if (r4 == 0) goto L_0x0046
            com.android.systemui.statusbar.phone.LockscreenWallpaper$WallpaperDrawable r1 = new com.android.systemui.statusbar.phone.LockscreenWallpaper$WallpaperDrawable
            android.widget.ImageView r5 = r12.mBackdropBack
            android.content.res.Resources r5 = r5.getResources()
            r1.<init>(r5, r4)
            com.android.systemui.plugins.statusbar.StatusBarStateController r4 = r12.mStatusBarStateController
            int r4 = r4.getState()
            if (r4 == r15) goto L_0x0044
            android.content.Context r4 = r12.mContext
            com.android.keyguard.KeyguardUpdateMonitor r4 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r4)
            boolean r4 = r4.isSimPinSecure()
            if (r4 == 0) goto L_0x0046
        L_0x0044:
            r4 = r15
            goto L_0x0047
        L_0x0046:
            r4 = r2
        L_0x0047:
            dagger.Lazy<com.android.systemui.statusbar.phone.ShadeController> r5 = r12.mShadeController
            java.lang.Object r5 = r5.get()
            com.android.systemui.statusbar.phone.ShadeController r5 = (com.android.systemui.statusbar.phone.ShadeController) r5
            dagger.Lazy<com.android.systemui.statusbar.phone.StatusBarWindowController> r6 = r12.mStatusBarWindowController
            java.lang.Object r6 = r6.get()
            com.android.systemui.statusbar.phone.StatusBarWindowController r6 = (com.android.systemui.statusbar.phone.StatusBarWindowController) r6
            if (r5 == 0) goto L_0x0061
            boolean r7 = r5.isOccluded()
            if (r7 == 0) goto L_0x0061
            r7 = r15
            goto L_0x0062
        L_0x0061:
            r7 = r2
        L_0x0062:
            if (r1 == 0) goto L_0x0066
            r8 = r15
            goto L_0x0067
        L_0x0066:
            r8 = r2
        L_0x0067:
            boolean r9 = com.oneplus.util.OpUtils.DEBUG_ONEPLUS
            java.lang.String r10 = "NotificationMediaManager"
            if (r9 == 0) goto L_0x00e3
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.String r11 = "updateMediaMetaData: hasArtwork = "
            r9.append(r11)
            r9.append(r8)
            java.lang.String r11 = ", allowWhenShade:"
            r9.append(r11)
            r9.append(r4)
            java.lang.String r11 = ", allowEnterAnimation:"
            r9.append(r11)
            r9.append(r14)
            java.lang.String r11 = ", vis:"
            r9.append(r11)
            com.android.systemui.statusbar.BackDropView r11 = r12.mBackdrop
            int r11 = r11.getVisibility()
            r9.append(r11)
            java.lang.String r11 = ", alpha:"
            r9.append(r11)
            com.android.systemui.statusbar.BackDropView r11 = r12.mBackdrop
            float r11 = r11.getAlpha()
            r9.append(r11)
            java.lang.String r11 = ", , mStatusBarStateController.getState():"
            r9.append(r11)
            com.android.systemui.plugins.statusbar.StatusBarStateController r11 = r12.mStatusBarStateController
            int r11 = r11.getState()
            r9.append(r11)
            java.lang.String r11 = ", metaDataChanged:"
            r9.append(r11)
            r9.append(r13)
            java.lang.String r11 = ", mBiometricUnlockController.getMode():"
            r9.append(r11)
            com.android.systemui.statusbar.phone.BiometricUnlockController r11 = r12.mBiometricUnlockController
            if (r11 == 0) goto L_0x00cf
            int r11 = r11.getMode()
            java.lang.Integer r11 = java.lang.Integer.valueOf(r11)
            goto L_0x00d1
        L_0x00cf:
            java.lang.String r11 = "null"
        L_0x00d1:
            r9.append(r11)
            java.lang.String r11 = ", hideBecauseOccluded:"
            r9.append(r11)
            r9.append(r7)
            java.lang.String r9 = r9.toString()
            android.util.Log.d(r10, r9)
        L_0x00e3:
            com.android.systemui.colorextraction.SysuiColorExtractor r9 = r12.mColorExtractor
            r9.setHasMediaArtwork(r3)
            com.android.systemui.statusbar.phone.ScrimController r3 = r12.mScrimController
            if (r3 == 0) goto L_0x00ef
            r3.setHasBackdrop(r8)
        L_0x00ef:
            r3 = 2
            r9 = 0
            if (r8 != 0) goto L_0x00f5
            goto L_0x01d0
        L_0x00f5:
            com.android.systemui.plugins.statusbar.StatusBarStateController r8 = r12.mStatusBarStateController
            int r8 = r8.getState()
            if (r8 != 0) goto L_0x00ff
            if (r4 == 0) goto L_0x01d0
        L_0x00ff:
            com.android.systemui.statusbar.phone.BiometricUnlockController r4 = r12.mBiometricUnlockController
            if (r4 == 0) goto L_0x01d0
            int r4 = r4.getMode()
            if (r4 == r3) goto L_0x01d0
            if (r7 != 0) goto L_0x01d0
            com.android.systemui.statusbar.BackDropView r0 = r12.mBackdrop
            int r0 = r0.getVisibility()
            r3 = 1065353216(0x3f800000, float:1.0)
            if (r0 == 0) goto L_0x0148
            com.android.systemui.statusbar.BackDropView r13 = r12.mBackdrop
            r13.setVisibility(r2)
            if (r14 == 0) goto L_0x012b
            com.android.systemui.statusbar.BackDropView r13 = r12.mBackdrop
            r13.setAlpha(r9)
            com.android.systemui.statusbar.BackDropView r13 = r12.mBackdrop
            android.view.ViewPropertyAnimator r13 = r13.animate()
            r13.alpha(r3)
            goto L_0x0139
        L_0x012b:
            com.android.systemui.statusbar.BackDropView r13 = r12.mBackdrop
            android.view.ViewPropertyAnimator r13 = r13.animate()
            r13.cancel()
            com.android.systemui.statusbar.BackDropView r13 = r12.mBackdrop
            r13.setAlpha(r3)
        L_0x0139:
            if (r6 == 0) goto L_0x013e
            r6.setBackdropShowing(r15)
        L_0x013e:
            boolean r13 = DEBUG_MEDIA
            if (r13 == 0) goto L_0x0147
            java.lang.String r13 = "DEBUG_MEDIA: Fading in album artwork"
            android.util.Log.v(r10, r13)
        L_0x0147:
            r13 = r15
        L_0x0148:
            if (r13 == 0) goto L_0x02a0
            android.widget.ImageView r13 = r12.mBackdropBack
            android.graphics.drawable.Drawable r13 = r13.getDrawable()
            if (r13 == 0) goto L_0x017a
            android.widget.ImageView r13 = r12.mBackdropBack
            android.graphics.drawable.Drawable r13 = r13.getDrawable()
            android.graphics.drawable.Drawable$ConstantState r13 = r13.getConstantState()
            android.widget.ImageView r14 = r12.mBackdropFront
            android.content.res.Resources r14 = r14.getResources()
            android.graphics.drawable.Drawable r13 = r13.newDrawable(r14)
            android.graphics.drawable.Drawable r13 = r13.mutate()
            android.widget.ImageView r14 = r12.mBackdropFront
            r14.setImageDrawable(r13)
            android.widget.ImageView r13 = r12.mBackdropFront
            r13.setAlpha(r3)
            android.widget.ImageView r13 = r12.mBackdropFront
            r13.setVisibility(r2)
            goto L_0x0180
        L_0x017a:
            android.widget.ImageView r13 = r12.mBackdropFront
            r14 = 4
            r13.setVisibility(r14)
        L_0x0180:
            android.widget.ImageView r13 = r12.mBackdropBack
            r13.setImageDrawable(r1)
            android.widget.ImageView r13 = r12.mBackdropFront
            int r13 = r13.getVisibility()
            if (r13 != 0) goto L_0x02a0
            boolean r13 = DEBUG_MEDIA
            if (r13 == 0) goto L_0x01b9
            java.lang.StringBuilder r13 = new java.lang.StringBuilder
            r13.<init>()
            java.lang.String r14 = "DEBUG_MEDIA: Crossfading album artwork from "
            r13.append(r14)
            android.widget.ImageView r14 = r12.mBackdropFront
            android.graphics.drawable.Drawable r14 = r14.getDrawable()
            r13.append(r14)
            java.lang.String r14 = " to "
            r13.append(r14)
            android.widget.ImageView r14 = r12.mBackdropBack
            android.graphics.drawable.Drawable r14 = r14.getDrawable()
            r13.append(r14)
            java.lang.String r13 = r13.toString()
            android.util.Log.v(r10, r13)
        L_0x01b9:
            android.widget.ImageView r13 = r12.mBackdropFront
            android.view.ViewPropertyAnimator r13 = r13.animate()
            r14 = 250(0xfa, double:1.235E-321)
            android.view.ViewPropertyAnimator r13 = r13.setDuration(r14)
            android.view.ViewPropertyAnimator r13 = r13.alpha(r9)
            java.lang.Runnable r12 = r12.mHideBackdropFront
            r13.withEndAction(r12)
            goto L_0x02a0
        L_0x01d0:
            com.android.systemui.statusbar.BackDropView r13 = r12.mBackdrop
            int r13 = r13.getVisibility()
            r14 = 8
            if (r13 == r14) goto L_0x02a0
            boolean r13 = DEBUG_MEDIA
            if (r13 == 0) goto L_0x01e3
            java.lang.String r13 = "DEBUG_MEDIA: Fading out album artwork"
            android.util.Log.v(r10, r13)
        L_0x01e3:
            if (r5 == 0) goto L_0x01f4
            boolean r13 = r5.isDozing()
            if (r13 == 0) goto L_0x01f4
            com.android.systemui.statusbar.phone.ScrimState r13 = com.android.systemui.statusbar.phone.ScrimState.AOD
            boolean r13 = r13.getAnimateChange()
            if (r13 != 0) goto L_0x01f4
            goto L_0x01f5
        L_0x01f4:
            r15 = r2
        L_0x01f5:
            com.android.systemui.statusbar.phone.BiometricUnlockController r13 = r12.mBiometricUnlockController
            if (r13 == 0) goto L_0x01ff
            int r13 = r13.getMode()
            if (r13 == r3) goto L_0x0291
        L_0x01ff:
            if (r7 != 0) goto L_0x0291
            if (r15 == 0) goto L_0x0205
            goto L_0x0291
        L_0x0205:
            if (r6 == 0) goto L_0x020a
            r6.setBackdropShowing(r2)
        L_0x020a:
            com.android.systemui.statusbar.BackDropView r13 = r12.mBackdrop
            android.view.ViewPropertyAnimator r13 = r13.animate()
            android.view.ViewPropertyAnimator r13 = r13.alpha(r9)
            android.view.animation.Interpolator r14 = com.android.systemui.Interpolators.ACCELERATE_DECELERATE
            android.view.ViewPropertyAnimator r13 = r13.setInterpolator(r14)
            r14 = 300(0x12c, double:1.48E-321)
            android.view.ViewPropertyAnimator r13 = r13.setDuration(r14)
            r14 = 0
            android.view.ViewPropertyAnimator r13 = r13.setStartDelay(r14)
            com.android.systemui.statusbar.-$$Lambda$NotificationMediaManager$5ApBYxWBRgBH6AkWUHgwLiCFqEk r0 = new com.android.systemui.statusbar.-$$Lambda$NotificationMediaManager$5ApBYxWBRgBH6AkWUHgwLiCFqEk
            r0.<init>()
            r13.withEndAction(r0)
            com.android.systemui.statusbar.policy.KeyguardMonitor r13 = r12.mKeyguardMonitor
            boolean r13 = r13.isKeyguardFadingAway()
            if (r13 == 0) goto L_0x02a0
            com.android.systemui.statusbar.policy.KeyguardMonitor r13 = r12.mKeyguardMonitor
            boolean r13 = r13.isSecure()
            if (r13 != 0) goto L_0x026a
            android.content.Context r13 = r12.mContext
            com.android.keyguard.KeyguardUpdateMonitor r13 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r13)
            boolean r13 = r13.isDeviceInteractive()
            if (r13 == 0) goto L_0x026a
            android.content.Context r13 = r12.mContext
            com.android.keyguard.KeyguardUpdateMonitor r13 = com.android.keyguard.KeyguardUpdateMonitor.getInstance(r13)
            boolean r13 = r13.needsSlowUnlockTransition()
            if (r13 != 0) goto L_0x026a
            com.android.systemui.statusbar.BackDropView r12 = r12.mBackdrop
            android.view.ViewPropertyAnimator r12 = r12.animate()
            android.view.ViewPropertyAnimator r12 = r12.setDuration(r14)
            android.view.animation.Interpolator r13 = com.android.systemui.Interpolators.LINEAR
            android.view.ViewPropertyAnimator r12 = r12.setInterpolator(r13)
            r12.start()
            goto L_0x02a0
        L_0x026a:
            com.android.systemui.statusbar.BackDropView r13 = r12.mBackdrop
            android.view.ViewPropertyAnimator r13 = r13.animate()
            com.android.systemui.statusbar.policy.KeyguardMonitor r14 = r12.mKeyguardMonitor
            long r14 = r14.getKeyguardFadingAwayDuration()
            r0 = 2
            long r14 = r14 / r0
            android.view.ViewPropertyAnimator r13 = r13.setDuration(r14)
            com.android.systemui.statusbar.policy.KeyguardMonitor r12 = r12.mKeyguardMonitor
            long r14 = r12.getKeyguardFadingAwayDelay()
            android.view.ViewPropertyAnimator r12 = r13.setStartDelay(r14)
            android.view.animation.Interpolator r13 = com.android.systemui.Interpolators.LINEAR
            android.view.ViewPropertyAnimator r12 = r12.setInterpolator(r13)
            r12.start()
            goto L_0x02a0
        L_0x0291:
            com.android.systemui.statusbar.BackDropView r13 = r12.mBackdrop
            r13.setVisibility(r14)
            android.widget.ImageView r12 = r12.mBackdropBack
            r12.setImageDrawable(r0)
            if (r6 == 0) goto L_0x02a0
            r6.setBackdropShowing(r2)
        L_0x02a0:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.NotificationMediaManager.finishUpdateMediaMetaData(boolean, boolean, android.graphics.Bitmap):void");
    }

    public /* synthetic */ void lambda$finishUpdateMediaMetaData$0$NotificationMediaManager() {
        this.mBackdrop.setVisibility(8);
        this.mBackdropFront.animate().cancel();
        this.mBackdropBack.setImageDrawable(null);
        this.mHandler.post(this.mHideBackdropFront);
    }

    public void setup(BackDropView backDropView, ImageView imageView, ImageView imageView2, ScrimController scrimController, LockscreenWallpaper lockscreenWallpaper) {
        this.mBackdrop = backDropView;
        this.mBackdropFront = imageView;
        this.mBackdropBack = imageView2;
        this.mScrimController = scrimController;
        this.mLockscreenWallpaper = lockscreenWallpaper;
    }

    public void setBiometricUnlockController(BiometricUnlockController biometricUnlockController) {
        this.mBiometricUnlockController = biometricUnlockController;
    }

    /* access modifiers changed from: private */
    public Bitmap processArtwork(Bitmap bitmap) {
        return this.mMediaArtworkProcessor.processArtwork(this.mContext, bitmap);
    }

    /* access modifiers changed from: private */
    public void removeTask(AsyncTask<?, ?, ?> asyncTask) {
        this.mProcessArtworkTasks.remove(asyncTask);
    }
}

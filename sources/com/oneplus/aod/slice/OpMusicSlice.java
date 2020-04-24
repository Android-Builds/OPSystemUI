package com.oneplus.aod.slice;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.util.Log;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationMediaManager.MediaListener;
import com.oneplus.aod.slice.OpSliceManager.Callback;

public class OpMusicSlice extends OpSlice implements MediaListener {
    private boolean mIsPlaying = false;
    private MediaController mMediaController = null;
    private MediaMetadata mMediaMetadata = null;
    private final MediaSessionManager mMediaSessionManager;
    private final NotificationMediaManager mNotificationMediaManager;

    public OpMusicSlice(Context context, Callback callback) {
        super(callback);
        this.mMediaSessionManager = (MediaSessionManager) context.getSystemService("media_session");
        this.mNotificationMediaManager = (NotificationMediaManager) Dependency.get(NotificationMediaManager.class);
    }

    /* access modifiers changed from: protected */
    public void handleSetListening(boolean z) {
        super.handleSetListening(z);
        if (OpSlice.DEBUG) {
            String str = this.mTag;
            StringBuilder sb = new StringBuilder();
            sb.append("handleSetListening = ");
            sb.append(z);
            sb.append(", mNotificationMediaManager = ");
            sb.append(this.mNotificationMediaManager);
            Log.i(str, sb.toString());
        }
        if (z) {
            NotificationMediaManager notificationMediaManager = this.mNotificationMediaManager;
            if (notificationMediaManager != null) {
                this.mMediaMetadata = notificationMediaManager.getMediaMetadata();
                this.mNotificationMediaManager.addCallback(this);
                return;
            }
            return;
        }
        NotificationMediaManager notificationMediaManager2 = this.mNotificationMediaManager;
        if (notificationMediaManager2 != null) {
            notificationMediaManager2.removeCallback(this);
        }
    }

    private void updateInfo() {
        MediaMetadata mediaMetadata = this.mMediaMetadata;
        if (mediaMetadata != null) {
            this.mPrimary = mediaMetadata.getString("android.media.metadata.TITLE");
            this.mSecondary = this.mMediaMetadata.getString("android.media.metadata.ARTIST");
            if (OpSlice.DEBUG) {
                String str = this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("updateInfo: primary = ");
                sb.append(this.mPrimary);
                sb.append(", secondary = ");
                sb.append(this.mSecondary);
                sb.append(", playing = ");
                sb.append(this.mIsPlaying);
                Log.i(str, sb.toString());
            }
            if (this.mPrimary == null) {
                this.mPrimary = "Unknow";
            }
            String str2 = this.mSecondary;
            if (str2 == null || str2.trim().length() == 0) {
                this.mSecondary = "Unknown artist";
            }
            setActive(this.mIsPlaying);
            updateUI();
            return;
        }
        setActive(false);
    }

    public void onMetadataOrStateChanged(MediaMetadata mediaMetadata, int i) {
        this.mMediaMetadata = mediaMetadata;
        this.mIsPlaying = i == 3;
        updateInfo();
    }
}

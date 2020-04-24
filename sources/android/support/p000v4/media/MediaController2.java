package android.support.p000v4.media;

import android.annotation.TargetApi;

@TargetApi(19)
/* renamed from: android.support.v4.media.MediaController2 */
public class MediaController2 implements AutoCloseable {

    /* renamed from: android.support.v4.media.MediaController2$ControllerCallback */
    public static abstract class ControllerCallback {
        public abstract void onDisconnected(MediaController2 mediaController2);
    }

    /* renamed from: android.support.v4.media.MediaController2$SupportLibraryImpl */
    interface SupportLibraryImpl extends AutoCloseable {
    }
}

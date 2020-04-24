package com.android.systemui.volume;

import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouter.Callback;

public class MediaRouterWrapper {
    private final MediaRouter mRouter;

    public MediaRouterWrapper(MediaRouter mediaRouter) {
        this.mRouter = mediaRouter;
    }

    public void removeCallback(Callback callback) {
        this.mRouter.removeCallback(callback);
    }
}

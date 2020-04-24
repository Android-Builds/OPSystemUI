package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.util.ArraySet;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.phone.UnlockMethodCache.OnUnlockMethodChangedListener;
import java.util.Iterator;

public class DynamicPrivacyController implements OnUnlockMethodChangedListener {
    private boolean mCacheInvalid;
    private boolean mLastDynamicUnlocked;
    private ArraySet<Listener> mListeners;
    private final NotificationLockscreenUserManager mLockscreenUserManager;
    private final UnlockMethodCache mUnlockMethodCache;

    public interface Listener {
        void onDynamicPrivacyChanged();
    }

    DynamicPrivacyController(Context context, NotificationLockscreenUserManager notificationLockscreenUserManager) {
        this(notificationLockscreenUserManager, UnlockMethodCache.getInstance(context));
    }

    @VisibleForTesting
    DynamicPrivacyController(NotificationLockscreenUserManager notificationLockscreenUserManager, UnlockMethodCache unlockMethodCache) {
        this.mListeners = new ArraySet<>();
        this.mLockscreenUserManager = notificationLockscreenUserManager;
        this.mUnlockMethodCache = unlockMethodCache;
        this.mUnlockMethodCache.addListener(this);
        this.mLastDynamicUnlocked = isDynamicallyUnlocked();
    }

    public void onUnlockMethodStateChanged() {
        if (isDynamicPrivacyEnabled()) {
            boolean isDynamicallyUnlocked = isDynamicallyUnlocked();
            if (isDynamicallyUnlocked != this.mLastDynamicUnlocked || this.mCacheInvalid) {
                this.mLastDynamicUnlocked = isDynamicallyUnlocked;
                Iterator it = this.mListeners.iterator();
                while (it.hasNext()) {
                    ((Listener) it.next()).onDynamicPrivacyChanged();
                }
            }
            this.mCacheInvalid = false;
            return;
        }
        this.mCacheInvalid = true;
    }

    private boolean isDynamicPrivacyEnabled() {
        NotificationLockscreenUserManager notificationLockscreenUserManager = this.mLockscreenUserManager;
        return !notificationLockscreenUserManager.shouldHideNotifications(notificationLockscreenUserManager.getCurrentUserId());
    }

    public boolean isDynamicallyUnlocked() {
        return this.mUnlockMethodCache.canSkipBouncer() && isDynamicPrivacyEnabled();
    }

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }
}

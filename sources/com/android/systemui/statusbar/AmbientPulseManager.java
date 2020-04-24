package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Resources;
import android.util.ArraySet;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.R$integer;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import java.util.Iterator;

public class AmbientPulseManager extends AlertingNotificationManager {
    @VisibleForTesting
    protected long mExtensionTime;
    protected final ArraySet<OnAmbientChangedListener> mListeners = new ArraySet<>();

    private final class AmbientEntry extends AlertEntry {
        private boolean extended;

        private AmbientEntry() {
            super();
        }

        /* access modifiers changed from: private */
        public void extendPulse() {
            if (!this.extended) {
                this.extended = true;
                updateEntry(false);
            }
        }

        public void reset() {
            super.reset();
            this.extended = false;
        }

        /* access modifiers changed from: protected */
        public long calculateFinishTime() {
            return super.calculateFinishTime() + (this.extended ? AmbientPulseManager.this.mExtensionTime : 0);
        }
    }

    public interface OnAmbientChangedListener {
        void onAmbientStateChanged(NotificationEntry notificationEntry, boolean z);
    }

    public int getContentFlag() {
        return 8;
    }

    public AmbientPulseManager(Context context) {
        Resources resources = context.getResources();
        this.mAutoDismissNotificationDecay = resources.getInteger(R$integer.ambient_notification_decay);
        this.mMinimumDisplayTime = resources.getInteger(R$integer.ambient_notification_minimum_time);
        this.mExtensionTime = (long) resources.getInteger(R$integer.ambient_notification_extension_time);
    }

    public void addListener(OnAmbientChangedListener onAmbientChangedListener) {
        this.mListeners.add(onAmbientChangedListener);
    }

    public void extendPulse() {
        AmbientEntry topEntry = getTopEntry();
        if (topEntry != null) {
            topEntry.extendPulse();
        }
    }

    /* access modifiers changed from: protected */
    public void onAlertEntryAdded(AlertEntry alertEntry) {
        NotificationEntry notificationEntry = alertEntry.mEntry;
        notificationEntry.setAmbientPulsing(true);
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((OnAmbientChangedListener) it.next()).onAmbientStateChanged(notificationEntry, true);
        }
    }

    /* access modifiers changed from: protected */
    public void onAlertEntryRemoved(AlertEntry alertEntry) {
        NotificationEntry notificationEntry = alertEntry.mEntry;
        notificationEntry.setAmbientPulsing(false);
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((OnAmbientChangedListener) it.next()).onAmbientStateChanged(notificationEntry, false);
        }
        notificationEntry.freeContentViewWhenSafe(8);
    }

    /* access modifiers changed from: protected */
    public AlertEntry createAlertEntry() {
        return new AmbientEntry();
    }

    private AmbientEntry getTopEntry() {
        AmbientEntry ambientEntry = null;
        if (this.mAlertEntries.isEmpty()) {
            return null;
        }
        for (AlertEntry alertEntry : this.mAlertEntries.values()) {
            if (ambientEntry == null || alertEntry.compareTo((AlertEntry) ambientEntry) < 0) {
                ambientEntry = alertEntry;
            }
        }
        return ambientEntry;
    }
}

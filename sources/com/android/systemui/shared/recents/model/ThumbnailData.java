package com.android.systemui.shared.recents.model;

import android.app.ActivityManager.TaskSnapshot;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

public class ThumbnailData {
    public Rect insets;
    public boolean isRealSnapshot;
    public boolean isTranslucent;
    public int orientation;
    public boolean reducedResolution;
    public float scale;
    public int systemUiVisibility;
    public Bitmap thumbnail;
    public int windowingMode;

    public ThumbnailData(TaskSnapshot taskSnapshot) {
        try {
            this.thumbnail = Bitmap.wrapHardwareBuffer(taskSnapshot.getSnapshot(), taskSnapshot.getColorSpace());
        } catch (IllegalArgumentException e) {
            Log.w("ThumbnailData", e.getMessage());
        }
        this.insets = new Rect(taskSnapshot.getContentInsets());
        this.orientation = taskSnapshot.getOrientation();
        this.reducedResolution = taskSnapshot.isReducedResolution();
        this.scale = taskSnapshot.getScale();
        this.isRealSnapshot = taskSnapshot.isRealSnapshot();
        this.isTranslucent = taskSnapshot.isTranslucent();
        this.windowingMode = taskSnapshot.getWindowingMode();
        this.systemUiVisibility = taskSnapshot.getSystemUiVisibility();
    }
}

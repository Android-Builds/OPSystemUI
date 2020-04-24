package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Allocation.MipmapControl;
import android.renderscript.RenderScript;
import android.util.MathUtils;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: MediaArtworkProcessor.kt */
public final class MediaArtworkProcessor {
    private Bitmap mArtworkCache;
    private final Point mTmpSize = new Point();

    public final Bitmap processArtwork(Context context, Bitmap bitmap) {
        Intrinsics.checkParameterIsNotNull(context, "context");
        Intrinsics.checkParameterIsNotNull(bitmap, "artwork");
        Bitmap bitmap2 = this.mArtworkCache;
        if (bitmap2 == null) {
            context.getDisplay().getSize(this.mTmpSize);
            RenderScript create = RenderScript.create(context);
            Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            Point point = this.mTmpSize;
            MathUtils.fitRect(rect, Math.max(point.x / 6, point.y / 6));
            Bitmap createScaledBitmap = Bitmap.createScaledBitmap(bitmap, rect.width(), rect.height(), true);
            String str = "inBitmap";
            Intrinsics.checkExpressionValueIsNotNull(createScaledBitmap, str);
            Config config = createScaledBitmap.getConfig();
            Config config2 = Config.ARGB_8888;
            if (config != config2) {
                Bitmap copy = createScaledBitmap.copy(config2, false);
                createScaledBitmap.recycle();
                createScaledBitmap = copy;
            }
            Allocation createFromBitmap = Allocation.createFromBitmap(create, createScaledBitmap, MipmapControl.MIPMAP_NONE, 2);
            Intrinsics.checkExpressionValueIsNotNull(createScaledBitmap, str);
            Bitmap createBitmap = Bitmap.createBitmap(createScaledBitmap.getWidth(), createScaledBitmap.getHeight(), Config.ARGB_8888);
            Allocation createFromBitmap2 = Allocation.createFromBitmap(create, createBitmap);
            createFromBitmap.copyTo(createBitmap);
            createFromBitmap.destroy();
            createFromBitmap2.destroy();
            createScaledBitmap.recycle();
            Intrinsics.checkExpressionValueIsNotNull(createBitmap, "outBitmap");
            return createBitmap;
        } else if (bitmap2 != null) {
            return bitmap2;
        } else {
            Intrinsics.throwNpe();
            throw null;
        }
    }

    public final void clearCache() {
        Bitmap bitmap = this.mArtworkCache;
        if (bitmap != null) {
            bitmap.recycle();
        }
        this.mArtworkCache = null;
    }
}

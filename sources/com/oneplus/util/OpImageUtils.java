package com.oneplus.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class OpImageUtils {
    public static Bitmap computeCustomBackgroundBounds(Context context, Bitmap bitmap) {
        if (bitmap == null) {
            return bitmap;
        }
        Display defaultDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getRealMetrics(displayMetrics);
        int i = displayMetrics.widthPixels;
        int i2 = displayMetrics.heightPixels;
        if (context.getResources().getConfiguration().orientation == 2) {
            i = displayMetrics.heightPixels;
            i2 = displayMetrics.widthPixels;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == i && height == i2) {
            StringBuilder sb = new StringBuilder();
            sb.append("bitmapWidth:");
            sb.append(width);
            sb.append(", bitmapHeight:");
            sb.append(height);
            Log.d("ImageUtils", sb.toString());
            return bitmap;
        }
        if (!(bitmap == null || context.getResources() == null)) {
            bitmap = new BitmapDrawable(context.getResources(), createCenterCroppedBitmap(bitmap, i, i2)).getBitmap();
        }
        return bitmap;
    }

    public static Bitmap createCenterCroppedBitmap(Bitmap bitmap, int i, int i2) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap createBitmap = Bitmap.createBitmap(i, i2, Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        canvas.drawColor(-1);
        float width2 = ((float) bitmap.getWidth()) / ((float) bitmap.getHeight());
        float f = ((float) i) / ((float) i2);
        StringBuilder sb = new StringBuilder();
        sb.append("src aspectSrc:");
        sb.append(width2);
        sb.append(", srcWidth:");
        sb.append(width);
        sb.append(", srcHeight:");
        sb.append(height);
        String str = "ImageUtils";
        Log.w(str, sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("dst aspectDst:");
        sb2.append(f);
        sb2.append(", dstWidth:");
        sb2.append(i);
        sb2.append(", dstHeight:");
        sb2.append(i2);
        Log.w(str, sb2.toString());
        int width3 = bitmap.getWidth();
        int height2 = bitmap.getHeight();
        if (width2 > f) {
            width3 = (int) (((float) height2) * f);
        } else {
            height2 = (int) (((float) width3) / f);
        }
        canvas.drawBitmap(bitmap, new Rect((bitmap.getWidth() - width3) / 2, (bitmap.getHeight() - height2) / 2, (bitmap.getWidth() + width3) / 2, (bitmap.getHeight() + height2) / 2), new Rect(0, 0, i, i2), new Paint());
        return createBitmap;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}

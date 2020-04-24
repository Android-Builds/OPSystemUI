package android.support.p002v7.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import java.lang.ref.WeakReference;

/* renamed from: android.support.v7.widget.VectorEnabledTintResources */
public class VectorEnabledTintResources extends Resources {
    private static boolean sCompatVectorFromResourcesEnabled;
    private final WeakReference<Context> mContextRef;

    public static boolean shouldBeUsed() {
        return isCompatVectorFromResourcesEnabled() && VERSION.SDK_INT <= 20;
    }

    public VectorEnabledTintResources(Context context, Resources resources) {
        super(resources.getAssets(), resources.getDisplayMetrics(), resources.getConfiguration());
        this.mContextRef = new WeakReference<>(context);
    }

    public Drawable getDrawable(int i) throws NotFoundException {
        Context context = (Context) this.mContextRef.get();
        if (context != null) {
            return AppCompatDrawableManager.get().onDrawableLoadedFromResources(context, this, i);
        }
        return super.getDrawable(i);
    }

    /* access modifiers changed from: 0000 */
    public final Drawable superGetDrawable(int i) {
        return super.getDrawable(i);
    }

    public static boolean isCompatVectorFromResourcesEnabled() {
        return sCompatVectorFromResourcesEnabled;
    }
}

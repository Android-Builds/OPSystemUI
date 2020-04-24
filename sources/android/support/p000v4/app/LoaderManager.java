package android.support.p000v4.app;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.ViewModelStoreOwner;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/* renamed from: android.support.v4.app.LoaderManager */
public abstract class LoaderManager {
    @Deprecated
    public abstract void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

    public abstract void markForRedelivery();

    public static <T extends LifecycleOwner & ViewModelStoreOwner> LoaderManager getInstance(T t) {
        return new LoaderManagerImpl(t, ((ViewModelStoreOwner) t).getViewModelStore());
    }
}

package com.oneplus.support.loader.app;

import com.oneplus.support.lifecycle.LifecycleOwner;
import com.oneplus.support.lifecycle.ViewModelStoreOwner;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class LoaderManager {
    @Deprecated
    public abstract void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

    public abstract void markForRedelivery();

    public static <T extends LifecycleOwner & ViewModelStoreOwner> LoaderManager getInstance(T t) {
        return new LoaderManagerImpl(t, ((ViewModelStoreOwner) t).getViewModelStore());
    }
}

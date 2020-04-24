package com.oneplus.support.loader.app;

import android.os.Bundle;
import android.util.Log;
import com.oneplus.support.collection.SparseArrayCompat;
import com.oneplus.support.core.util.DebugUtils;
import com.oneplus.support.lifecycle.LifecycleOwner;
import com.oneplus.support.lifecycle.MutableLiveData;
import com.oneplus.support.lifecycle.Observer;
import com.oneplus.support.lifecycle.ViewModel;
import com.oneplus.support.lifecycle.ViewModelProvider;
import com.oneplus.support.lifecycle.ViewModelProvider.Factory;
import com.oneplus.support.lifecycle.ViewModelStore;
import com.oneplus.support.loader.content.Loader;
import com.oneplus.support.loader.content.Loader.OnLoadCompleteListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;

class LoaderManagerImpl extends LoaderManager {
    static boolean DEBUG;
    private final LifecycleOwner mLifecycleOwner;
    private final LoaderViewModel mLoaderViewModel;

    public static class LoaderInfo<D> extends MutableLiveData<D> implements OnLoadCompleteListener<D> {
        private final Bundle mArgs;
        private final int mId;
        private LifecycleOwner mLifecycleOwner;
        private final Loader<D> mLoader;
        private LoaderObserver<D> mObserver;
        private Loader<D> mPriorLoader;

        /* access modifiers changed from: protected */
        public void onActive() {
            if (LoaderManagerImpl.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("  Starting: ");
                sb.append(this);
                Log.v("LoaderManager", sb.toString());
            }
            this.mLoader.startLoading();
            throw null;
        }

        /* access modifiers changed from: protected */
        public void onInactive() {
            if (LoaderManagerImpl.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("  Stopping: ");
                sb.append(this);
                Log.v("LoaderManager", sb.toString());
            }
            this.mLoader.stopLoading();
            throw null;
        }

        /* access modifiers changed from: 0000 */
        public void markForRedelivery() {
            LifecycleOwner lifecycleOwner = this.mLifecycleOwner;
            LoaderObserver<D> loaderObserver = this.mObserver;
            if (lifecycleOwner != null && loaderObserver != null) {
                super.removeObserver(loaderObserver);
                observe(lifecycleOwner, loaderObserver);
            }
        }

        public void removeObserver(Observer<? super D> observer) {
            super.removeObserver(observer);
            this.mLifecycleOwner = null;
            this.mObserver = null;
        }

        /* access modifiers changed from: 0000 */
        public Loader<D> destroy(boolean z) {
            if (LoaderManagerImpl.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("  Destroying: ");
                sb.append(this);
                Log.v("LoaderManager", sb.toString());
            }
            this.mLoader.cancelLoad();
            throw null;
        }

        public void setValue(D d) {
            super.setValue(d);
            Loader<D> loader = this.mPriorLoader;
            if (loader != null) {
                loader.reset();
                throw null;
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append("LoaderInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" #");
            sb.append(this.mId);
            sb.append(" : ");
            DebugUtils.buildShortClassTag(this.mLoader, sb);
            sb.append("}}");
            return sb.toString();
        }

        public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            printWriter.print(str);
            printWriter.print("mId=");
            printWriter.print(this.mId);
            printWriter.print(" mArgs=");
            printWriter.println(this.mArgs);
            printWriter.print(str);
            printWriter.print("mLoader=");
            printWriter.println(this.mLoader);
            Loader<D> loader = this.mLoader;
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append("  ");
            loader.dump(sb.toString(), fileDescriptor, printWriter, strArr);
            throw null;
        }
    }

    static class LoaderObserver<D> implements Observer<D> {
    }

    static class LoaderViewModel extends ViewModel {
        private static final Factory FACTORY = new Factory() {
            public <T extends ViewModel> T create(Class<T> cls) {
                return new LoaderViewModel();
            }
        };
        private boolean mCreatingLoader = false;
        private SparseArrayCompat<LoaderInfo> mLoaders = new SparseArrayCompat<>();

        LoaderViewModel() {
        }

        static LoaderViewModel getInstance(ViewModelStore viewModelStore) {
            return (LoaderViewModel) new ViewModelProvider(viewModelStore, FACTORY).get(LoaderViewModel.class);
        }

        /* access modifiers changed from: 0000 */
        public void markForRedelivery() {
            int size = this.mLoaders.size();
            for (int i = 0; i < size; i++) {
                ((LoaderInfo) this.mLoaders.valueAt(i)).markForRedelivery();
            }
        }

        /* access modifiers changed from: protected */
        public void onCleared() {
            super.onCleared();
            if (this.mLoaders.size() <= 0) {
                this.mLoaders.clear();
            } else {
                ((LoaderInfo) this.mLoaders.valueAt(0)).destroy(true);
                throw null;
            }
        }

        public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (this.mLoaders.size() > 0) {
                printWriter.print(str);
                printWriter.println("Loaders:");
                StringBuilder sb = new StringBuilder();
                sb.append(str);
                sb.append("    ");
                String sb2 = sb.toString();
                if (this.mLoaders.size() > 0) {
                    LoaderInfo loaderInfo = (LoaderInfo) this.mLoaders.valueAt(0);
                    printWriter.print(str);
                    printWriter.print("  #");
                    printWriter.print(this.mLoaders.keyAt(0));
                    printWriter.print(": ");
                    printWriter.println(loaderInfo.toString());
                    loaderInfo.dump(sb2, fileDescriptor, printWriter, strArr);
                    throw null;
                }
            }
        }
    }

    LoaderManagerImpl(LifecycleOwner lifecycleOwner, ViewModelStore viewModelStore) {
        this.mLifecycleOwner = lifecycleOwner;
        this.mLoaderViewModel = LoaderViewModel.getInstance(viewModelStore);
    }

    public void markForRedelivery() {
        this.mLoaderViewModel.markForRedelivery();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("LoaderManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        DebugUtils.buildShortClassTag(this.mLifecycleOwner, sb);
        sb.append("}}");
        return sb.toString();
    }

    @Deprecated
    public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        this.mLoaderViewModel.dump(str, fileDescriptor, printWriter, strArr);
    }
}

package android.arch.lifecycle;

public class ViewModelProvider {
    private final Factory mFactory;
    private final ViewModelStore mViewModelStore;

    public interface Factory {
        <T extends ViewModel> T create(Class<T> cls);
    }

    public ViewModelProvider(ViewModelStore viewModelStore, Factory factory) {
        this.mFactory = factory;
        this.mViewModelStore = viewModelStore;
    }

    public <T extends ViewModel> T get(Class<T> cls) {
        String canonicalName = cls.getCanonicalName();
        if (canonicalName != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("android.arch.lifecycle.ViewModelProvider.DefaultKey:");
            sb.append(canonicalName);
            return get(sb.toString(), cls);
        }
        throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
    }

    public <T extends ViewModel> T get(String str, Class<T> cls) {
        T t = this.mViewModelStore.get(str);
        if (cls.isInstance(t)) {
            return t;
        }
        T create = this.mFactory.create(cls);
        this.mViewModelStore.put(str, create);
        return create;
    }
}

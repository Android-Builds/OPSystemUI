package com.oneplus.support.lifecycle;

@Deprecated
public interface LifecycleRegistryOwner extends LifecycleOwner {
    LifecycleRegistry getLifecycle();
}

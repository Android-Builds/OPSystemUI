package com.oneplus.support.lifecycle;

import com.oneplus.support.lifecycle.Lifecycle.Event;

public interface GenericLifecycleObserver extends LifecycleObserver {
    void onStateChanged(LifecycleOwner lifecycleOwner, Event event);
}

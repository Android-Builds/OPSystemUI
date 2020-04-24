package com.oneplus.lib.widget.recyclerview;

public interface Pools$Pool<T> {
    T acquire();

    boolean release(T t);
}

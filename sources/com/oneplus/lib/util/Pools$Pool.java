package com.oneplus.lib.util;

public interface Pools$Pool<T> {
    T acquire();

    boolean release(T t);
}

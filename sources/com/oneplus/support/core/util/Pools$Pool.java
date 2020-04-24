package com.oneplus.support.core.util;

public interface Pools$Pool<T> {
    T acquire();

    boolean release(T t);
}

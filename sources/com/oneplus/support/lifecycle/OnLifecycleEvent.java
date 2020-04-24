package com.oneplus.support.lifecycle;

import com.oneplus.support.lifecycle.Lifecycle.Event;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OnLifecycleEvent {
    Event value();
}

package com.oneplus.support.core.fragment.app;

import android.util.AndroidRuntimeException;

final class SuperNotCalledException extends AndroidRuntimeException {
    public SuperNotCalledException(String str) {
        super(str);
    }
}

package com.oneplus.support.core.internal.view;

import android.view.MenuItem;
import com.oneplus.support.core.view.ActionProvider;

public interface SupportMenuItem extends MenuItem {
    SupportMenuItem setSupportActionProvider(ActionProvider actionProvider);
}

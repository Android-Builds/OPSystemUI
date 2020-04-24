package com.oneplus.lib.design.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.ClassLoaderCreator;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$color;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$style;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.app.appcompat.SupportMenuInflater;
import com.oneplus.lib.app.appcompat.TintTypedArray;
import com.oneplus.lib.menu.MenuBuilder;
import com.oneplus.lib.menu.MenuBuilder.Callback;
import com.oneplus.lib.util.ReflectUtil;
import com.oneplus.support.annotation.GestureBarAdapterPolicy;
import com.oneplus.support.core.content.ContextCompat;
import com.oneplus.support.core.view.AbsSavedState;
import com.oneplus.support.core.view.ViewCompat;

public class BottomNavigationView extends LinearLayout implements Badge {
    private static String TAG = "BottomNavigationView";
    private final MenuBuilder menu;
    private MenuInflater menuInflater;
    private final BottomNavigationMenuView menuView;
    private final BottomNavigationPresenter presenter;
    /* access modifiers changed from: private */
    public OnNavigationItemReselectedListener reselectedListener;
    /* access modifiers changed from: private */
    public OnNavigationItemSelectedListener selectedListener;

    public interface OnNavigationItemReselectedListener {
        void onNavigationItemReselected(MenuItem menuItem);
    }

    public interface OnNavigationItemSelectedListener {
        boolean onNavigationItemSelected(MenuItem menuItem);
    }

    static class SavedState extends AbsSavedState {
        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel, ClassLoader classLoader) {
                return new SavedState(parcel, classLoader);
            }

            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel, null);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        Bundle menuPresenterState;

        public SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        public SavedState(Parcel parcel, ClassLoader classLoader) {
            super(parcel, classLoader);
            readFromParcel(parcel, classLoader);
        }

        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeBundle(this.menuPresenterState);
        }

        private void readFromParcel(Parcel parcel, ClassLoader classLoader) {
            this.menuPresenterState = parcel.readBundle(classLoader);
        }
    }

    public BottomNavigationView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.bottomNavigationStyle);
    }

    public BottomNavigationView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.presenter = new BottomNavigationPresenter();
        this.menu = new BottomNavigationMenu(context);
        this.menuView = new BottomNavigationMenuView(context);
        LayoutParams layoutParams = new LayoutParams(-1, -1);
        if (isAfter5T()) {
            layoutParams.height = getResources().getDimensionPixelSize(R$dimen.design_bottom_navigation_height);
        } else {
            layoutParams.height = getResources().getDimensionPixelSize(R$dimen.op_bottom_navigation_height_with_bottom_softkey_navigation);
        }
        layoutParams.gravity = 17;
        this.menuView.setLayoutParams(layoutParams);
        this.presenter.setBottomNavigationMenuView(this.menuView);
        this.presenter.setId(1);
        this.menuView.setPresenter(this.presenter);
        this.menu.addMenuPresenter(this.presenter);
        this.presenter.initForMenu(getContext(), this.menu);
        TintTypedArray obtainTintedStyledAttributes = ThemeEnforcement.obtainTintedStyledAttributes(context, attributeSet, R$styleable.BottomNavigationView, i, R$style.Widget_Design_BottomNavigationView, R$styleable.BottomNavigationView_itemTextAppearanceInactive, R$styleable.BottomNavigationView_itemTextAppearanceActive);
        if (obtainTintedStyledAttributes.hasValue(R$styleable.BottomNavigationView_itemIconTint)) {
            this.menuView.setIconTintList(obtainTintedStyledAttributes.getColorStateList(R$styleable.BottomNavigationView_itemIconTint));
        } else {
            BottomNavigationMenuView bottomNavigationMenuView = this.menuView;
            bottomNavigationMenuView.setIconTintList(bottomNavigationMenuView.createDefaultColorStateList(16842808));
        }
        setItemIconSize(obtainTintedStyledAttributes.getDimensionPixelSize(R$styleable.BottomNavigationView_itemIconSize, getResources().getDimensionPixelSize(R$dimen.design_bottom_navigation_icon_size)));
        if (obtainTintedStyledAttributes.hasValue(R$styleable.BottomNavigationView_itemTextAppearanceInactive)) {
            setItemTextAppearanceInactive(obtainTintedStyledAttributes.getResourceId(R$styleable.BottomNavigationView_itemTextAppearanceInactive, 0));
        }
        if (obtainTintedStyledAttributes.hasValue(R$styleable.BottomNavigationView_itemTextAppearanceActive)) {
            setItemTextAppearanceActive(obtainTintedStyledAttributes.getResourceId(R$styleable.BottomNavigationView_itemTextAppearanceActive, 0));
        }
        if (obtainTintedStyledAttributes.hasValue(R$styleable.BottomNavigationView_itemTextColor)) {
            setItemTextColor(obtainTintedStyledAttributes.getColorStateList(R$styleable.BottomNavigationView_itemTextColor));
        }
        if (obtainTintedStyledAttributes.hasValue(R$styleable.BottomNavigationView_elevation)) {
            ViewCompat.setElevation(this, (float) obtainTintedStyledAttributes.getDimensionPixelSize(R$styleable.BottomNavigationView_elevation, 0));
        }
        setLabelVisibilityMode(obtainTintedStyledAttributes.getInteger(R$styleable.BottomNavigationView_labelVisibilityMode, -1));
        setItemHorizontalTranslationEnabled(obtainTintedStyledAttributes.getBoolean(R$styleable.BottomNavigationView_itemHorizontalTranslationEnabled, true));
        this.menuView.setItemBackgroundRes(obtainTintedStyledAttributes.getResourceId(R$styleable.BottomNavigationView_itemBackground, 0));
        if (obtainTintedStyledAttributes.hasValue(R$styleable.BottomNavigationView_menu)) {
            inflateMenu(obtainTintedStyledAttributes.getResourceId(R$styleable.BottomNavigationView_menu, 0));
        }
        obtainTintedStyledAttributes.recycle();
        addView(this.menuView, layoutParams);
        if (VERSION.SDK_INT < 21) {
            addCompatibilityTopDivider(context);
        }
        this.menu.setCallback(new Callback() {
            public void onMenuModeChange(MenuBuilder menuBuilder) {
            }

            public boolean onMenuItemSelected(MenuBuilder menuBuilder, MenuItem menuItem) {
                boolean z = true;
                if (BottomNavigationView.this.reselectedListener == null || menuItem.getItemId() != BottomNavigationView.this.getSelectedItemId()) {
                    if (BottomNavigationView.this.selectedListener == null || BottomNavigationView.this.selectedListener.onNavigationItemSelected(menuItem)) {
                        z = false;
                    }
                    return z;
                }
                BottomNavigationView.this.reselectedListener.onNavigationItemReselected(menuItem);
                return true;
            }
        });
    }

    public void inflateMenu(int i) {
        this.presenter.setUpdateSuspended(true);
        getMenuInflater().inflate(i, this.menu);
        this.presenter.setUpdateSuspended(false);
        this.presenter.updateMenuView(true);
    }

    public void setItemIconSize(int i) {
        this.menuView.setItemIconSize(i);
    }

    public void setItemTextColor(ColorStateList colorStateList) {
        this.menuView.setItemTextColor(colorStateList);
    }

    public int getSelectedItemId() {
        return this.menuView.getSelectedItemId();
    }

    public void setLabelVisibilityMode(int i) {
        if (this.menuView.getLabelVisibilityMode() != i) {
            this.menuView.setLabelVisibilityMode(i);
            this.presenter.updateMenuView(false);
        }
    }

    public void setItemTextAppearanceInactive(int i) {
        this.menuView.setItemTextAppearanceInactive(i);
    }

    public void setItemTextAppearanceActive(int i) {
        this.menuView.setItemTextAppearanceActive(i);
    }

    public void setItemHorizontalTranslationEnabled(boolean z) {
        if (this.menuView.isItemHorizontalTranslationEnabled() != z) {
            this.menuView.setItemHorizontalTranslationEnabled(z);
            this.presenter.updateMenuView(false);
        }
    }

    private void addCompatibilityTopDivider(Context context) {
        View view = new View(context);
        view.setBackgroundColor(ContextCompat.getColor(context, R$color.design_bottom_navigation_shadow_color));
        view.setLayoutParams(new LayoutParams(-1, getResources().getDimensionPixelSize(R$dimen.design_bottom_navigation_shadow_height)));
        addView(view);
    }

    private MenuInflater getMenuInflater() {
        if (this.menuInflater == null) {
            this.menuInflater = new SupportMenuInflater(getContext());
        }
        return this.menuInflater;
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.menuPresenterState = new Bundle();
        this.menu.savePresenterStates(savedState.menuPresenterState);
        return savedState;
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedState)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.menu.restorePresenterStates(savedState.menuPresenterState);
    }

    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        boolean z2 = !GestureBarAdapterPolicy.gestureButtonEnabled(getContext());
        if (z && z2) {
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            layoutParams.height = getResources().getDimensionPixelSize(R$dimen.op_bottom_navigation_height_with_bottom_softkey_navigation);
            setLayoutParams(layoutParams);
        }
    }

    private boolean isAfter5T() {
        String str = ReflectUtil.get("ro.boot.project_name");
        String str2 = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("isAfter5T devices = ");
        sb.append(str);
        Log.d(str2, sb.toString());
        if (!TextUtils.isEmpty(str) && Integer.parseInt(str) <= 17801) {
            return false;
        }
        return true;
    }
}

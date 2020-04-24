package com.oneplus.lib.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$styleable;
import com.oneplus.lib.app.OPAlertDialog.Builder;
import com.oneplus.lib.preference.Preference.BaseSavedState;
import com.oneplus.lib.preference.PreferenceManager.OnActivityDestroyListener;

public abstract class DialogPreference extends Preference implements OnClickListener, OnDismissListener, OnActivityDestroyListener {
    private Builder mBuilder;
    private Dialog mDialog;
    private Drawable mDialogIcon;
    private int mDialogLayoutResId;
    private CharSequence mDialogMessage;
    private CharSequence mDialogTitle;
    private CharSequence mNegativeButtonText;
    private boolean mOnlyDarkTheme;
    private CharSequence mPositiveButtonText;
    private int mWhichButtonClicked;

    private static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        Bundle dialogBundle;
        boolean isDialogShowing;

        public SavedState(Parcel parcel) {
            super(parcel);
            boolean z = true;
            if (parcel.readInt() != 1) {
                z = false;
            }
            this.isDialogShowing = z;
            this.dialogBundle = parcel.readBundle();
        }

        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.isDialogShowing ? 1 : 0);
            parcel.writeBundle(this.dialogBundle);
        }
    }

    /* access modifiers changed from: protected */
    public void onBindDialogView(View view) {
    }

    /* access modifiers changed from: protected */
    public void onPrepareDialogBuilder(Builder builder) {
    }

    public DialogPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.DialogPreference, i, i2);
        this.mDialogTitle = obtainStyledAttributes.getString(R$styleable.DialogPreference_android_dialogTitle);
        if (this.mDialogTitle == null) {
            this.mDialogTitle = getTitle();
        }
        this.mOnlyDarkTheme = obtainStyledAttributes.getBoolean(R$styleable.DialogPreference_opOnlyDarkTheme, false);
        this.mDialogMessage = obtainStyledAttributes.getString(R$styleable.DialogPreference_android_dialogMessage);
        this.mDialogIcon = obtainStyledAttributes.getDrawable(R$styleable.DialogPreference_android_dialogIcon);
        this.mPositiveButtonText = obtainStyledAttributes.getString(R$styleable.DialogPreference_android_positiveButtonText);
        this.mNegativeButtonText = obtainStyledAttributes.getString(R$styleable.DialogPreference_android_negativeButtonText);
        this.mDialogLayoutResId = obtainStyledAttributes.getResourceId(R$styleable.DialogPreference_android_dialogLayout, this.mDialogLayoutResId);
        obtainStyledAttributes.recycle();
    }

    public DialogPreference(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public DialogPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R$attr.op_dialogPreferenceStyle);
    }

    /* access modifiers changed from: protected */
    public void onClick() {
        Dialog dialog = this.mDialog;
        if (dialog == null || !dialog.isShowing()) {
            showDialog(null);
        }
    }

    /* access modifiers changed from: protected */
    public void showDialog(Bundle bundle) {
        Context context = getContext();
        this.mWhichButtonClicked = -2;
        Builder builder = new Builder(context);
        builder.setOnlyDarkTheme(this.mOnlyDarkTheme);
        builder.setTitle(this.mDialogTitle);
        builder.setIcon(this.mDialogIcon);
        builder.setPositiveButton(this.mPositiveButtonText, this);
        builder.setNegativeButton(this.mNegativeButtonText, this);
        this.mBuilder = builder;
        View onCreateDialogView = onCreateDialogView();
        if (onCreateDialogView != null) {
            onBindDialogView(onCreateDialogView);
            this.mBuilder.setView(onCreateDialogView);
            this.mBuilder.setMessage(this.mDialogMessage);
        } else {
            this.mBuilder.setMessage(this.mDialogMessage);
        }
        onPrepareDialogBuilder(this.mBuilder);
        getPreferenceManager().registerOnActivityDestroyListener(this);
        throw null;
    }

    /* access modifiers changed from: protected */
    public View onCreateDialogView() {
        if (this.mDialogLayoutResId == 0) {
            return null;
        }
        return LayoutInflater.from(this.mBuilder.getContext()).inflate(this.mDialogLayoutResId, null);
    }

    public void onClick(DialogInterface dialogInterface, int i) {
        this.mWhichButtonClicked = i;
    }

    public void onDismiss(DialogInterface dialogInterface) {
        getPreferenceManager().unregisterOnActivityDestroyListener(this);
        throw null;
    }
}

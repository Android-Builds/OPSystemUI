package com.oneplus.lib.app;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import com.oneplus.lib.app.OPAlertController.AlertParams;

public class OPAlertDialog extends Dialog implements DialogInterface {
    private OPAlertController mAlert;
    protected Context mContext;

    public static class Builder {

        /* renamed from: P */
        private final AlertParams f114P;

        public Builder(Context context) {
            this.f114P = new AlertParams(context);
        }

        public Context getContext() {
            return this.f114P.mContext;
        }

        public Builder setTitle(CharSequence charSequence) {
            this.f114P.mTitle = charSequence;
            return this;
        }

        public Builder setMessage(CharSequence charSequence) {
            this.f114P.mMessage = charSequence;
            return this;
        }

        public Builder setIcon(Drawable drawable) {
            this.f114P.mIcon = drawable;
            return this;
        }

        public Builder setOnlyDarkTheme(boolean z) {
            this.f114P.mOnlyDarkTheme = z;
            return this;
        }

        public Builder setPositiveButton(CharSequence charSequence, OnClickListener onClickListener) {
            AlertParams alertParams = this.f114P;
            alertParams.mPositiveButtonText = charSequence;
            alertParams.mPositiveButtonListener = onClickListener;
            return this;
        }

        public Builder setNegativeButton(CharSequence charSequence, OnClickListener onClickListener) {
            AlertParams alertParams = this.f114P;
            alertParams.mNegativeButtonText = charSequence;
            alertParams.mNegativeButtonListener = onClickListener;
            return this;
        }

        public Builder setSingleChoiceItems(CharSequence[] charSequenceArr, int i, OnClickListener onClickListener) {
            AlertParams alertParams = this.f114P;
            alertParams.mItems = charSequenceArr;
            alertParams.mOnClickListener = onClickListener;
            alertParams.mCheckedItem = i;
            alertParams.mIsSingleChoice = true;
            return this;
        }

        public Builder setView(View view) {
            AlertParams alertParams = this.f114P;
            alertParams.mView = view;
            alertParams.mViewLayoutResId = 0;
            alertParams.mViewSpacingSpecified = false;
            return this;
        }
    }

    protected OPAlertDialog(Context context, int i) {
        this(context, i, true);
    }

    OPAlertDialog(Context context, int i, boolean z) {
        super(context, resolveDialogTheme(context, i));
        this.mAlert = new OPAlertController(getContext(), this, getWindow());
        this.mContext = context;
    }

    static int resolveDialogTheme(Context context, int i) {
        if (i >= 16777216) {
            return i;
        }
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(16843529, typedValue, true);
        return typedValue.resourceId;
    }

    public void setTitle(CharSequence charSequence) {
        super.setTitle(charSequence);
        this.mAlert.setTitle(charSequence);
    }

    public void setMessage(CharSequence charSequence) {
        this.mAlert.setMessage(charSequence);
    }

    public void setButton(int i, CharSequence charSequence, OnClickListener onClickListener) {
        this.mAlert.setButton(i, charSequence, onClickListener, null);
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mAlert.installContent();
    }

    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (this.mAlert.onKeyDown(i, keyEvent)) {
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (this.mAlert.onKeyUp(i, keyEvent)) {
            return true;
        }
        return super.onKeyUp(i, keyEvent);
    }

    public void onAttachedToWindow() {
        if (getOwnerActivity() != null && getOwnerActivity().isInMultiWindowMode()) {
            getWindow().clearFlags(65792);
            getWindow().addFlags(65792);
        }
    }
}

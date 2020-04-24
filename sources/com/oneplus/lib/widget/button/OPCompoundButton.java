package com.oneplus.lib.widget.button;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View.BaseSavedState;
import android.view.ViewDebug.ExportedProperty;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Checkable;
import com.airbnb.lottie.C0526R;
import com.oneplus.commonctrl.R$attr;
import com.oneplus.commonctrl.R$styleable;

public abstract class OPCompoundButton extends Button implements Checkable {
    private static final int[] CHECKED_STATE_SET = {16842912};
    private static final int[] INDETERMINATE_STATE_SET = {R$attr.state_indeterminate};
    public static String TAG = "OPCompoundButton";
    private boolean mBroadcasting;
    private Drawable mButtonDrawable;
    private ColorStateList mButtonTintList;
    private Mode mButtonTintMode;
    private boolean mChecked;
    private boolean mHasButtonTint;
    private boolean mHasButtonTintMode;
    private boolean mIndeterminate;
    private OnCheckedChangeListener mOnCheckedChangeListener;
    private OnCheckedChangeListener mOnCheckedChangeWidgetListener;
    private OnTriStateCheckedChangeListener mOnTriStateCheckedChangeListener;
    private boolean mThreeState;

    public interface OnCheckedChangeListener {
        void onCheckedChanged(OPCompoundButton oPCompoundButton, boolean z);
    }

    public interface OnTriStateCheckedChangeListener {
        void onCheckedChanged(OPCompoundButton oPCompoundButton, Boolean bool);
    }

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        boolean checked;
        boolean indeterminate;
        boolean threeState;

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.checked = ((Boolean) parcel.readValue(null)).booleanValue();
            this.threeState = ((Boolean) parcel.readValue(null)).booleanValue();
            this.indeterminate = ((Boolean) parcel.readValue(null)).booleanValue();
        }

        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeValue(Boolean.valueOf(this.checked));
            parcel.writeValue(Boolean.valueOf(this.threeState));
            parcel.writeValue(Boolean.valueOf(this.indeterminate));
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("CompoundButton.SavedState{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" checked=");
            sb.append(this.checked);
            sb.append(", indeterminate=");
            sb.append(this.indeterminate);
            sb.append(", threeState=");
            sb.append(this.threeState);
            sb.append("}");
            return sb.toString();
        }
    }

    public OPCompoundButton(Context context) {
        this(context, null);
    }

    public OPCompoundButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public OPCompoundButton(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public OPCompoundButton(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        Boolean bool = null;
        this.mButtonTintList = null;
        this.mButtonTintMode = null;
        this.mHasButtonTint = false;
        this.mHasButtonTintMode = false;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.OPCompoundbutton, i, i2);
        Drawable drawable = obtainStyledAttributes.getDrawable(R$styleable.OPCompoundbutton_android_button);
        if (drawable != null) {
            setButtonDrawable(drawable);
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPCompoundbutton_android_buttonTintMode)) {
            this.mButtonTintMode = parseTintMode(obtainStyledAttributes.getInt(R$styleable.OPCompoundbutton_android_buttonTintMode, -1), this.mButtonTintMode);
            this.mHasButtonTintMode = true;
        }
        if (obtainStyledAttributes.hasValue(R$styleable.OPCompoundbutton_android_buttonTint)) {
            this.mButtonTintList = obtainStyledAttributes.getColorStateList(R$styleable.OPCompoundbutton_android_buttonTint);
            this.mHasButtonTint = true;
        }
        boolean z = obtainStyledAttributes.getBoolean(R$styleable.OPCompoundbutton_threeState, false);
        boolean z2 = obtainStyledAttributes.getBoolean(R$styleable.OPCompoundbutton_android_checked, false);
        boolean z3 = obtainStyledAttributes.getBoolean(R$styleable.OPCompoundbutton_indeterminate, false);
        setThreeState(z);
        if (z3) {
            if (!z3) {
                bool = Boolean.valueOf(z2);
            }
            setTriStateChecked(bool);
        } else {
            setCheckedInternal(z2);
        }
        setRadius(obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPCompoundbutton_android_radius, -1));
        obtainStyledAttributes.recycle();
        applyButtonTint();
    }

    private void setRadius(int i) {
        if (i != -1) {
            Drawable background = getBackground();
            if (background == null || !(background instanceof RippleDrawable)) {
                Log.i(TAG, "setRaidus fail , background not a rippleDrawable");
            } else {
                background.mutate();
                ((RippleDrawable) background).setRadius(i);
            }
        }
    }

    private static Mode parseTintMode(int i, Mode mode) {
        if (i == 3) {
            return Mode.SRC_OVER;
        }
        if (i == 5) {
            return Mode.SRC_IN;
        }
        if (i == 9) {
            return Mode.SRC_ATOP;
        }
        switch (i) {
            case 14:
                return Mode.MULTIPLY;
            case 15:
                return Mode.SCREEN;
            case 16:
                return Mode.ADD;
            default:
                return mode;
        }
    }

    public void toggle() {
        setCheckedInternal(!this.mChecked);
    }

    public boolean performClick() {
        if (!this.mThreeState) {
            toggle();
        } else if (this.mIndeterminate) {
            setTriStateChecked(Boolean.valueOf(true));
        } else {
            setTriStateChecked(Boolean.valueOf(!this.mChecked));
        }
        boolean performClick = super.performClick();
        if (!performClick) {
            playSoundEffect(0);
        }
        return performClick;
    }

    public void setThreeState(boolean z) {
        this.mThreeState = z;
    }

    @ExportedProperty
    public boolean isIndeterminate() {
        return this.mIndeterminate;
    }

    @ExportedProperty
    public boolean isChecked() {
        return this.mChecked;
    }

    public void setChecked(boolean z) {
        setCheckedInternal(z);
    }

    public void setCheckedInternal(boolean z) {
        setCheckedInternal(z, false);
    }

    public void setCheckedInternal(boolean z, boolean z2) {
        boolean z3 = this.mChecked != z;
        if (z3 || z2) {
            this.mChecked = z;
            refreshDrawableState();
            notifyViewAccessibilityStateChangedIfNeededInternal(0);
            if (z3 && !this.mBroadcasting) {
                this.mBroadcasting = true;
                OnCheckedChangeListener onCheckedChangeListener = this.mOnCheckedChangeListener;
                if (onCheckedChangeListener != null) {
                    onCheckedChangeListener.onCheckedChanged(this, this.mChecked);
                }
                OnCheckedChangeListener onCheckedChangeListener2 = this.mOnCheckedChangeWidgetListener;
                if (onCheckedChangeListener2 != null) {
                    onCheckedChangeListener2.onCheckedChanged(this, this.mChecked);
                }
                this.mBroadcasting = false;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void setTriStateChecked(Boolean bool) {
        if (!(this.mIndeterminate == (bool == null) && (bool == null || bool.booleanValue() == this.mChecked))) {
            this.mIndeterminate = bool == null;
            if (bool != null) {
                setCheckedInternal(bool.booleanValue(), true);
            } else {
                refreshDrawableState();
                notifyViewAccessibilityStateChangedIfNeededInternal(0);
            }
            if (!this.mBroadcasting) {
                this.mBroadcasting = true;
                OnTriStateCheckedChangeListener onTriStateCheckedChangeListener = this.mOnTriStateCheckedChangeListener;
                if (onTriStateCheckedChangeListener != null) {
                    onTriStateCheckedChangeListener.onCheckedChanged(this, bool);
                }
                this.mBroadcasting = false;
            }
        }
    }

    private void notifyViewAccessibilityStateChangedIfNeededInternal(int i) {
        try {
            Class.forName("android.view.View").getMethod("notifyViewAccessibilityStateChangedIfNeeded", new Class[]{Integer.TYPE}).invoke(this, new Object[]{Integer.valueOf(i)});
        } catch (Exception e) {
            Log.e(TAG, "notifyViewAccessibilityStateChangedIfNeeded with Exception!", e);
        }
    }

    /* access modifiers changed from: 0000 */
    public void setOnCheckedChangeWidgetListener(OnCheckedChangeListener onCheckedChangeListener) {
        this.mOnCheckedChangeWidgetListener = onCheckedChangeListener;
    }

    public void setButtonDrawable(Drawable drawable) {
        Drawable drawable2 = this.mButtonDrawable;
        if (drawable2 != drawable) {
            if (drawable2 != null) {
                drawable2.setCallback(null);
                unscheduleDrawable(this.mButtonDrawable);
            }
            this.mButtonDrawable = drawable;
            if (drawable != null) {
                drawable.setCallback(this);
                boolean z = true;
                try {
                    Class.forName("android.graphics.drawable.Drawable").getMethod("setLayoutDirection", new Class[]{Integer.TYPE}).invoke(drawable, new Object[]{Integer.valueOf(getLayoutDirection())});
                } catch (Exception e) {
                    Log.e(TAG, "setLayoutDirection with Exception!", e);
                }
                if (drawable.isStateful()) {
                    drawable.setState(getDrawableState());
                }
                if (getVisibility() != 0) {
                    z = false;
                }
                drawable.setVisible(z, false);
                setMinHeight(drawable.getIntrinsicHeight());
                applyButtonTint();
            }
        }
    }

    private void applyButtonTint() {
        if (this.mButtonDrawable == null) {
            return;
        }
        if (this.mHasButtonTint || this.mHasButtonTintMode) {
            this.mButtonDrawable = this.mButtonDrawable.mutate();
            if (this.mHasButtonTint) {
                this.mButtonDrawable.setTintList(this.mButtonTintList);
            }
            if (this.mHasButtonTintMode) {
                this.mButtonDrawable.setTintMode(this.mButtonTintMode);
            }
            if (this.mButtonDrawable.isStateful()) {
                this.mButtonDrawable.setState(getDrawableState());
            }
        }
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setClassName(OPCompoundButton.class.getName());
        accessibilityEvent.setChecked(this.mChecked);
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.setClassName(OPCompoundButton.class.getName());
        accessibilityNodeInfo.setCheckable(true);
        accessibilityNodeInfo.setChecked(this.mChecked);
    }

    public int getCompoundPaddingLeft() {
        int compoundPaddingLeft = super.getCompoundPaddingLeft();
        if (isLayoutRtl()) {
            return compoundPaddingLeft;
        }
        Drawable drawable = this.mButtonDrawable;
        return drawable != null ? compoundPaddingLeft + drawable.getIntrinsicWidth() : compoundPaddingLeft;
    }

    public int getCompoundPaddingRight() {
        int compoundPaddingRight = super.getCompoundPaddingRight();
        if (!isLayoutRtl()) {
            return compoundPaddingRight;
        }
        Drawable drawable = this.mButtonDrawable;
        return drawable != null ? compoundPaddingRight + drawable.getIntrinsicWidth() : compoundPaddingRight;
    }

    public int getHorizontalOffsetForDrawables() {
        Drawable drawable = this.mButtonDrawable;
        if (drawable != null) {
            return drawable.getIntrinsicWidth();
        }
        return 0;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        int i;
        Drawable drawable = this.mButtonDrawable;
        if (drawable != null) {
            int gravity = getGravity() & C0526R.styleable.AppCompatTheme_toolbarNavigationButtonStyle;
            int intrinsicHeight = drawable.getIntrinsicHeight();
            int intrinsicWidth = drawable.getIntrinsicWidth();
            int i2 = 0;
            if (gravity == 16) {
                i = (getHeight() - intrinsicHeight) / 2;
            } else if (gravity != 80) {
                i = 0;
            } else {
                i = getHeight() - intrinsicHeight;
            }
            int i3 = intrinsicHeight + i;
            if (isLayoutRtl()) {
                i2 = getWidth() - intrinsicWidth;
            }
            if (isLayoutRtl()) {
                intrinsicWidth = getWidth();
            }
            drawable.setBounds(i2, i, intrinsicWidth, i3);
            Drawable background = getBackground();
            if (background != null) {
                background.setHotspotBounds(i2, i, intrinsicWidth, i3);
            }
        }
        super.onDraw(canvas);
        if (drawable != null) {
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            if (scrollX == 0 && scrollY == 0) {
                drawable.draw(canvas);
                return;
            }
            canvas.translate((float) scrollX, (float) scrollY);
            drawable.draw(canvas);
            canvas.translate((float) (-scrollX), (float) (-scrollY));
        }
    }

    /* access modifiers changed from: protected */
    public int[] onCreateDrawableState(int i) {
        int[] onCreateDrawableState = super.onCreateDrawableState(i + 1);
        if (isIndeterminate()) {
            Button.mergeDrawableStates(onCreateDrawableState, INDETERMINATE_STATE_SET);
        } else if (isChecked()) {
            Button.mergeDrawableStates(onCreateDrawableState, CHECKED_STATE_SET);
        }
        return onCreateDrawableState;
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mButtonDrawable != null) {
            this.mButtonDrawable.setState(getDrawableState());
            invalidate();
        }
    }

    public void drawableHotspotChanged(float f, float f2) {
        super.drawableHotspotChanged(f, f2);
        Drawable drawable = this.mButtonDrawable;
        if (drawable != null) {
            drawable.setHotspot(f, f2);
        }
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable drawable) {
        return super.verifyDrawable(drawable) || drawable == this.mButtonDrawable;
    }

    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        Drawable drawable = this.mButtonDrawable;
        if (drawable != null) {
            drawable.jumpToCurrentState();
        }
    }

    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.checked = isChecked();
        savedState.threeState = this.mThreeState;
        savedState.indeterminate = this.mIndeterminate;
        return savedState;
    }

    public void onRestoreInstanceState(Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.mThreeState = savedState.threeState;
        if (this.mThreeState) {
            setTriStateChecked(savedState.indeterminate ? null : Boolean.valueOf(savedState.checked));
        } else {
            setCheckedInternal(savedState.checked);
        }
        requestLayout();
    }

    public boolean isLayoutRtl() {
        return getLayoutDirection() == 1;
    }
}

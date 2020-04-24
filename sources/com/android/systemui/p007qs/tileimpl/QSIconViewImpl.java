package com.android.systemui.p007qs.tileimpl;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.Animatable2.AnimationCallback;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.android.systemui.R$color;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.p007qs.AlphaControlledSignalTileView.AlphaControlledSlashImageView;
import com.android.systemui.plugins.p006qs.QSIconView;
import com.android.systemui.plugins.p006qs.QSTile.Icon;
import com.android.systemui.plugins.p006qs.QSTile.State;
import java.util.Objects;
import java.util.function.Supplier;

/* renamed from: com.android.systemui.qs.tileimpl.QSIconViewImpl */
public class QSIconViewImpl extends QSIconView {
    protected final String TAG;
    private boolean mAnimationEnabled = true;
    protected final View mIcon;
    protected final int mIconSizePx;
    private Icon mLastIcon;
    private int mState = -1;
    private int mTint;

    /* access modifiers changed from: protected */
    public int getIconMeasureMode() {
        return 1073741824;
    }

    public QSIconViewImpl(Context context) {
        super(context);
        StringBuilder sb = new StringBuilder();
        sb.append("QSIconViewImpl.");
        sb.append(getClass().getSimpleName());
        this.TAG = sb.toString();
        this.mIconSizePx = context.getResources().getDimensionPixelSize(R$dimen.qs_tile_icon_size);
        this.mIcon = createIcon();
        addView(this.mIcon);
    }

    public void disableAnimation() {
        this.mAnimationEnabled = false;
    }

    public View getIconView() {
        return this.mIcon;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        int size = MeasureSpec.getSize(i);
        this.mIcon.measure(MeasureSpec.makeMeasureSpec(size, getIconMeasureMode()), exactly(this.mIconSizePx));
        setMeasuredDimension(size, this.mIcon.getMeasuredHeight());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('[');
        StringBuilder sb2 = new StringBuilder();
        sb2.append("state=");
        sb2.append(this.mState);
        sb.append(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append(", tint=");
        sb3.append(this.mTint);
        sb.append(sb3.toString());
        if (this.mLastIcon != null) {
            StringBuilder sb4 = new StringBuilder();
            sb4.append(", lastIcon=");
            sb4.append(this.mLastIcon.toString());
            sb.append(sb4.toString());
        }
        sb.append("]");
        return sb.toString();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        layout(this.mIcon, (getMeasuredWidth() - this.mIcon.getMeasuredWidth()) / 2, 0);
    }

    public void setIcon(State state, boolean z) {
        setIcon((ImageView) this.mIcon, state, z);
    }

    /* access modifiers changed from: protected */
    public void updateIcon(ImageView imageView, State state, boolean z) {
        String str;
        Supplier<Icon> supplier = state.iconSupplier;
        Icon icon = supplier != null ? (Icon) supplier.get() : state.icon;
        if (!Objects.equals(icon, imageView.getTag(R$id.qs_icon_tag)) || this.mState != state.state || !Objects.equals(state.slash, imageView.getTag(R$id.qs_slash_tag))) {
            boolean z2 = z && shouldAnimate(imageView);
            this.mLastIcon = icon;
            Drawable drawable = icon != null ? z2 ? icon.getDrawable(this.mContext) : icon.getInvisibleDrawable(this.mContext) : null;
            int padding = icon != null ? icon.getPadding() : 0;
            if (drawable != null) {
                drawable.setAutoMirrored(false);
                drawable.setLayoutDirection(getLayoutDirection());
            }
            if (Build.DEBUG_ONEPLUS) {
                String str2 = this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("updateIcon: label=");
                sb.append(state.label);
                sb.append(", icon=");
                sb.append(icon);
                sb.append(", iconSupplier.get=");
                Supplier<Icon> supplier2 = state.iconSupplier;
                sb.append(supplier2 != null ? (Icon) supplier2.get() : null);
                sb.append(", state.icon=");
                sb.append(state.icon);
                sb.append(", shouldAnimate=");
                sb.append(z2);
                sb.append(", d=");
                if (drawable != null) {
                    str = drawable.toString();
                } else {
                    str = null;
                }
                sb.append(str);
                Log.d(str2, sb.toString());
            }
            if (imageView instanceof SlashImageView) {
                SlashImageView slashImageView = (SlashImageView) imageView;
                slashImageView.setAnimationEnabled(z2);
                slashImageView.setState(null, drawable);
            } else {
                imageView.setImageDrawable(drawable);
            }
            imageView.setTag(R$id.qs_icon_tag, icon);
            imageView.setTag(R$id.qs_slash_tag, state.slash);
            imageView.setPadding(0, padding, 0, padding);
            if (drawable instanceof Animatable2) {
                final Animatable2 animatable2 = (Animatable2) drawable;
                animatable2.start();
                if (state.isTransient) {
                    animatable2.registerAnimationCallback(new AnimationCallback() {
                        public void onAnimationEnd(Drawable drawable) {
                            animatable2.start();
                        }
                    });
                }
            }
        }
    }

    private boolean shouldAnimate(ImageView imageView) {
        return this.mAnimationEnabled && imageView.isShown() && imageView.getDrawable() != null;
    }

    /* access modifiers changed from: protected */
    public void setIcon(ImageView imageView, State state, boolean z) {
        if (state.disabledByPolicy) {
            imageView.setColorFilter(getContext().getColor(R$color.qs_tile_disabled_color));
        } else {
            imageView.clearColorFilter();
        }
        int i = state.state;
        if (i != this.mState) {
            int color = getColor(i);
            this.mState = state.state;
            if (this.mTint == 0 || !z || !shouldAnimate(imageView)) {
                if (imageView instanceof AlphaControlledSlashImageView) {
                    ((AlphaControlledSlashImageView) imageView).setFinalImageTintList(ColorStateList.valueOf(color));
                } else {
                    setTint(imageView, color);
                }
                this.mTint = color;
                updateIcon(imageView, state, z);
                return;
            }
            setTint(imageView, color);
            updateIcon(imageView, state, z);
            this.mTint = color;
            return;
        }
        updateIcon(imageView, state, z);
    }

    /* access modifiers changed from: protected */
    public int getColor(int i) {
        return QSTileImpl.getColorForState(getContext(), i);
    }

    public static void setTint(ImageView imageView, int i) {
        imageView.setImageTintList(ColorStateList.valueOf(i));
    }

    /* access modifiers changed from: protected */
    public View createIcon() {
        SlashImageView slashImageView = new SlashImageView(this.mContext);
        slashImageView.setId(16908294);
        slashImageView.setScaleType(ScaleType.FIT_CENTER);
        return slashImageView;
    }

    /* access modifiers changed from: protected */
    public final int exactly(int i) {
        return MeasureSpec.makeMeasureSpec(i, 1073741824);
    }

    /* access modifiers changed from: protected */
    public final void layout(View view, int i, int i2) {
        view.layout(i, i2, view.getMeasuredWidth() + i, view.getMeasuredHeight() + i2);
    }
}

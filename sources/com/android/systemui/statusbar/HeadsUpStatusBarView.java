package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.DisplayCutout;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.keyguard.AlphaOptimizedLinearLayout;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.ScreenDecorations.DisplayCutoutView;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import java.util.List;

public class HeadsUpStatusBarView extends AlphaOptimizedLinearLayout {
    private int mAbsoluteStartPadding;
    private List<Rect> mCutOutBounds;
    private int mCutOutInset;
    private DisplayCutout mDisplayCutout;
    private Point mDisplaySize;
    private int mEndMargin;
    private boolean mFirstLayout;
    private Rect mIconDrawingRect;
    private View mIconPlaceholder;
    private Rect mLayoutedIconRect;
    private int mMaxWidth;
    private Runnable mOnDrawingRectChangedListener;
    private boolean mPublicMode;
    private NotificationEntry mShowingEntry;
    private int mSysWinInset;
    private TextView mTextView;
    private int[] mTmpPosition;

    public HeadsUpStatusBarView(Context context) {
        this(context, null);
    }

    public HeadsUpStatusBarView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public HeadsUpStatusBarView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public HeadsUpStatusBarView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mLayoutedIconRect = new Rect();
        this.mTmpPosition = new int[2];
        this.mFirstLayout = true;
        this.mIconDrawingRect = new Rect();
        Resources resources = getResources();
        this.mAbsoluteStartPadding = resources.getDimensionPixelSize(R$dimen.notification_side_paddings) + resources.getDimensionPixelSize(R$dimen.heads_up_status_bar_padding_start) + resources.getDimensionPixelSize(17105308);
        this.mEndMargin = resources.getDimensionPixelSize(17105307);
        setPaddingRelative(this.mAbsoluteStartPadding, 0, this.mEndMargin, 0);
        updateMaxWidth();
    }

    private void updateMaxWidth() {
        int dimensionPixelSize = getResources().getDimensionPixelSize(R$dimen.qs_panel_width);
        if (dimensionPixelSize != this.mMaxWidth) {
            this.mMaxWidth = dimensionPixelSize;
            requestLayout();
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        if (this.mMaxWidth > 0) {
            i = MeasureSpec.makeMeasureSpec(Math.min(MeasureSpec.getSize(i), this.mMaxWidth), MeasureSpec.getMode(i));
        }
        super.onMeasure(i, i2);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateMaxWidth();
    }

    public Bundle onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("heads_up_status_bar_view_super_parcelable", super.onSaveInstanceState());
        bundle.putBoolean("first_layout", this.mFirstLayout);
        bundle.putBoolean("public_mode", this.mPublicMode);
        bundle.putInt("visibility", getVisibility());
        bundle.putFloat("alpha", getAlpha());
        return bundle;
    }

    public void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable == null || !(parcelable instanceof Bundle)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        Bundle bundle = (Bundle) parcelable;
        super.onRestoreInstanceState(bundle.getParcelable("heads_up_status_bar_view_super_parcelable"));
        this.mFirstLayout = bundle.getBoolean("first_layout", true);
        this.mPublicMode = bundle.getBoolean("public_mode", false);
        String str = "visibility";
        if (bundle.containsKey(str)) {
            setVisibility(bundle.getInt(str));
        }
        String str2 = "alpha";
        if (bundle.containsKey(str2)) {
            setAlpha(bundle.getFloat(str2));
        }
    }

    @VisibleForTesting
    public HeadsUpStatusBarView(Context context, View view, TextView textView) {
        this(context);
        this.mIconPlaceholder = view;
        this.mTextView = textView;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mIconPlaceholder = findViewById(R$id.icon_placeholder);
        this.mTextView = (TextView) findViewById(R$id.text);
    }

    public void setEntry(NotificationEntry notificationEntry) {
        if (notificationEntry != null) {
            this.mShowingEntry = notificationEntry;
            CharSequence charSequence = notificationEntry.headsUpStatusBarText;
            if (this.mPublicMode) {
                charSequence = notificationEntry.headsUpStatusBarTextPublic;
            }
            this.mTextView.setText(charSequence);
            return;
        }
        this.mShowingEntry = null;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        post(new Runnable() {
            public final void run() {
                HeadsUpStatusBarView.this.lambda$onLayout$0$HeadsUpStatusBarView();
            }
        });
    }

    /* access modifiers changed from: private */
    /* renamed from: updatePosition */
    public void lambda$onLayout$0$HeadsUpStatusBarView() {
        this.mIconPlaceholder.getLocationOnScreen(this.mTmpPosition);
        int translationX = (int) (((float) this.mTmpPosition[0]) - getTranslationX());
        int i = this.mTmpPosition[1];
        int width = this.mIconPlaceholder.getWidth() + translationX;
        this.mLayoutedIconRect.set(translationX, i, width, this.mIconPlaceholder.getHeight() + i);
        updateDrawingRect();
        int i2 = this.mAbsoluteStartPadding + this.mSysWinInset + this.mCutOutInset;
        boolean isLayoutRtl = isLayoutRtl();
        if (isLayoutRtl) {
            translationX = this.mDisplaySize.x - width;
        }
        if (translationX != i2) {
            Rect rect = new Rect();
            if (!(this.mDisplayCutout == null || rect.width() == 0)) {
                DisplayCutoutView.boundsFromDirection(this.mContext, this.mDisplayCutout, 48, rect);
                if (translationX > (isLayoutRtl ? this.mDisplaySize.x - rect.right : rect.left)) {
                    translationX -= rect.width();
                }
            }
            setPaddingRelative((i2 - translationX) + getPaddingStart(), 0, this.mEndMargin, 0);
        }
        if (this.mFirstLayout) {
            setVisibility(8);
            this.mFirstLayout = false;
        }
    }

    public void setPanelTranslation(float f) {
        setTranslationX(f);
        updateDrawingRect();
    }

    private void updateDrawingRect() {
        Rect rect = this.mIconDrawingRect;
        float f = (float) rect.left;
        rect.set(this.mLayoutedIconRect);
        this.mIconDrawingRect.offset((int) getTranslationX(), 0);
        if (f != ((float) this.mIconDrawingRect.left)) {
            Runnable runnable = this.mOnDrawingRectChangedListener;
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean fitSystemWindows(Rect rect) {
        boolean isLayoutRtl = isLayoutRtl();
        this.mSysWinInset = isLayoutRtl ? rect.right : rect.left;
        this.mDisplayCutout = getRootWindowInsets().getDisplayCutout();
        DisplayCutout displayCutout = this.mDisplayCutout;
        int i = displayCutout != null ? isLayoutRtl ? displayCutout.getSafeInsetRight() : displayCutout.getSafeInsetLeft() : 0;
        this.mCutOutInset = i;
        getDisplaySize();
        this.mCutOutBounds = null;
        if (displayCutout != null && displayCutout.getSafeInsetRight() == 0 && displayCutout.getSafeInsetLeft() == 0) {
            this.mCutOutBounds = displayCutout.getBoundingRects();
        }
        if (this.mSysWinInset != 0) {
            this.mCutOutInset = 0;
        }
        return super.fitSystemWindows(rect);
    }

    public NotificationEntry getShowingEntry() {
        return this.mShowingEntry;
    }

    public Rect getIconDrawingRect() {
        return this.mIconDrawingRect;
    }

    public void onDarkChanged(Rect rect, float f, int i) {
        this.mTextView.setTextColor(DarkIconDispatcher.getTint(rect, this, i));
    }

    public void setPublicMode(boolean z) {
        this.mPublicMode = z;
    }

    public void setOnDrawingRectChangedListener(Runnable runnable) {
        this.mOnDrawingRectChangedListener = runnable;
    }

    private void getDisplaySize() {
        if (this.mDisplaySize == null) {
            this.mDisplaySize = new Point();
        }
        getDisplay().getRealSize(this.mDisplaySize);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getDisplaySize();
    }
}

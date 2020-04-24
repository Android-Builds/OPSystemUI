package com.oneplus.lib.design.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import com.oneplus.commonctrl.R$dimen;

public class BadgeView extends View {
    private final Paint mBackgroundPaint;
    private final int mDefaultSize;
    private final int mDefaultSizeIndeterminate;
    private final int mDefaultTextPadding;
    private final int mDefaultTextSize;
    private BottomNavigationNotification mNotification;
    private String mText;
    private float mTextHeight;
    private final Paint mTextPaint;
    private float mTextWidth;

    public BadgeView(Context context) {
        this(context, null);
    }

    public BadgeView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDefaultSize = getResources().getDimensionPixelSize(R$dimen.bottom_navigation_badge_mini_width);
        this.mDefaultSizeIndeterminate = getResources().getDimensionPixelSize(R$dimen.bottom_navigation_badge_size_indeterminate);
        this.mDefaultTextSize = getResources().getDimensionPixelSize(R$dimen.bottom_navigation_badge_text_size);
        this.mDefaultTextPadding = getResources().getDimensionPixelSize(R$dimen.bottom_navigation_badge_text_horizontal_padding);
        this.mTextPaint = new TextPaint(1);
        this.mBackgroundPaint = new Paint(1);
        this.mTextPaint.setTextSize((float) this.mDefaultTextSize);
        this.mTextPaint.setTextAlign(Align.CENTER);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        BottomNavigationNotification bottomNavigationNotification = this.mNotification;
        if (bottomNavigationNotification == null) {
            return;
        }
        if (bottomNavigationNotification.isIndeterminate()) {
            canvas.drawCircle(((float) getWidth()) / 2.0f, ((float) getHeight()) / 2.0f, ((float) this.mDefaultSizeIndeterminate) / 2.0f, this.mBackgroundPaint);
            return;
        }
        canvas.drawRoundRect(0.0f, 0.0f, (float) getWidth(), (float) getHeight(), ((float) getHeight()) / 2.0f, ((float) getHeight()) / 2.0f, this.mBackgroundPaint);
        canvas.drawText(this.mText, ((float) getWidth()) / 2.0f, (((float) getHeight()) / 2.0f) + (this.mTextHeight / 2.16f), this.mTextPaint);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        BottomNavigationNotification bottomNavigationNotification = this.mNotification;
        if (bottomNavigationNotification != null) {
            setMeasuredDimension(MeasureSpec.makeMeasureSpec((int) Math.max((float) this.mDefaultSize, bottomNavigationNotification.isIndeterminate() ? 0.0f : this.mTextWidth + ((float) (this.mDefaultTextPadding * 2))), 1073741824), MeasureSpec.makeMeasureSpec(this.mDefaultSize, 1073741824));
        } else {
            super.onMeasure(i, i2);
        }
    }
}

package com.oneplus.lib.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;
import com.oneplus.commonctrl.R$styleable;

public class DialogTitle extends TextView {
    public DialogTitle(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public DialogTitle(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public DialogTitle(Context context) {
        super(context);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        Layout layout = getLayout();
        if (layout != null) {
            int lineCount = layout.getLineCount();
            if (lineCount > 0 && layout.getEllipsisCount(lineCount - 1) > 0) {
                setSingleLine(false);
                setMaxLines(4);
                TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(null, R$styleable.OPTextAppearance);
                int dimensionPixelSize = obtainStyledAttributes.getDimensionPixelSize(R$styleable.OPTextAppearance_android_textSize, 0);
                if (dimensionPixelSize != 0) {
                    setTextSize(0, (float) dimensionPixelSize);
                }
                obtainStyledAttributes.recycle();
                super.onMeasure(i, i2);
            }
        }
    }
}

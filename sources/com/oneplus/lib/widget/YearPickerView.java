package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import com.oneplus.commonctrl.R$dimen;
import com.oneplus.commonctrl.R$id;
import com.oneplus.commonctrl.R$layout;
import java.util.Calendar;

class YearPickerView extends FrameLayout {
    private static final int ITEM_LAYOUT = R$layout.op_year_label_text_view;
    private final int mChildSize;
    private OnYearSelectedListener mOnYearSelectedListener;
    private NumberPicker mPicker;
    private final int mViewSize;

    public interface OnYearSelectedListener {
    }

    public YearPickerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16844068);
    }

    public YearPickerView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public YearPickerView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        Resources resources = context.getResources();
        LayoutInflater.from(context).inflate(ITEM_LAYOUT, this, true);
        this.mViewSize = resources.getDimensionPixelOffset(R$dimen.datepicker_view_animator_height);
        this.mChildSize = resources.getDimensionPixelOffset(R$dimen.datepicker_year_label_height);
        this.mPicker = (NumberPicker) findViewById(R$id.year_picker);
        this.mPicker.setSelectNumberCount(5);
    }

    public void setOnYearSelectedListener(OnYearSelectedListener onYearSelectedListener) {
        this.mOnYearSelectedListener = onYearSelectedListener;
    }

    public void setYear(int i) {
        this.mPicker.setValue(i);
    }

    public void setRange(Calendar calendar, Calendar calendar2) {
        int i = calendar.get(1);
        int i2 = calendar2.get(1);
        this.mPicker.setMinValue(i);
        this.mPicker.setMaxValue(i2);
    }
}

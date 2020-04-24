package com.oneplus.lib.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.oneplus.lib.widget.SimpleMonthView.OnDayClickListener;
import com.oneplus.support.viewpager.widget.PagerAdapter;
import java.util.Calendar;

class DayPickerPagerAdapter extends PagerAdapter {
    private ColorStateList mCalendarTextColor;
    private final int mCalendarViewId;
    private int mCount;
    private ColorStateList mDayHighlightColor;
    private int mDayOfWeekTextAppearance;
    private ColorStateList mDaySelectorColor;
    private int mDayTextAppearance;
    private int mFirstDayOfWeek;
    private final LayoutInflater mInflater;
    private final SparseArray<ViewHolder> mItems = new SparseArray<>();
    private final int mLayoutResId;
    private final Calendar mMaxDate = Calendar.getInstance();
    private final Calendar mMinDate = Calendar.getInstance();
    private int mMonthTextAppearance;
    private final OnDayClickListener mOnDayClickListener = new OnDayClickListener() {
        public void onDayClick(SimpleMonthView simpleMonthView, Calendar calendar) {
            if (calendar != null) {
                DayPickerPagerAdapter.this.setSelectedDay(calendar);
                if (DayPickerPagerAdapter.this.mOnDaySelectedListener != null) {
                    DayPickerPagerAdapter.this.mOnDaySelectedListener.onDaySelected(DayPickerPagerAdapter.this, calendar);
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public OnDaySelectedListener mOnDaySelectedListener;
    private Calendar mSelectedDay = null;

    public interface OnDaySelectedListener {
        void onDaySelected(DayPickerPagerAdapter dayPickerPagerAdapter, Calendar calendar);
    }

    private static class ViewHolder {
        public final SimpleMonthView calendar;
        public final View container;
        public final int position;

        public ViewHolder(int i, View view, SimpleMonthView simpleMonthView) {
            this.position = i;
            this.container = view;
            this.calendar = simpleMonthView;
        }
    }

    public DayPickerPagerAdapter(Context context, int i, int i2) {
        this.mInflater = LayoutInflater.from(context);
        this.mLayoutResId = i;
        this.mCalendarViewId = i2;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(new int[]{16843820});
        this.mDayHighlightColor = obtainStyledAttributes.getColorStateList(0);
        obtainStyledAttributes.recycle();
    }

    public void setRange(Calendar calendar, Calendar calendar2) {
        this.mMinDate.setTimeInMillis(calendar.getTimeInMillis());
        this.mMaxDate.setTimeInMillis(calendar2.getTimeInMillis());
        this.mCount = (this.mMaxDate.get(2) - this.mMinDate.get(2)) + ((this.mMaxDate.get(1) - this.mMinDate.get(1)) * 12) + 1;
        notifyDataSetChanged();
    }

    public void setFirstDayOfWeek(int i) {
        this.mFirstDayOfWeek = i;
        int size = this.mItems.size();
        for (int i2 = 0; i2 < size; i2++) {
            ((ViewHolder) this.mItems.valueAt(i2)).calendar.setFirstDayOfWeek(i);
        }
    }

    public void setSelectedDay(Calendar calendar) {
        int positionForDay = getPositionForDay(this.mSelectedDay);
        int positionForDay2 = getPositionForDay(calendar);
        if (positionForDay != positionForDay2 && positionForDay >= 0) {
            ViewHolder viewHolder = (ViewHolder) this.mItems.get(positionForDay, null);
            if (viewHolder != null) {
                viewHolder.calendar.setSelectedDay(-1);
            }
        }
        if (positionForDay2 >= 0) {
            ViewHolder viewHolder2 = (ViewHolder) this.mItems.get(positionForDay2, null);
            if (viewHolder2 != null) {
                viewHolder2.calendar.setSelectedDay(calendar.get(5));
            }
        }
        this.mSelectedDay = calendar;
    }

    public void setOnDaySelectedListener(OnDaySelectedListener onDaySelectedListener) {
        this.mOnDaySelectedListener = onDaySelectedListener;
    }

    /* access modifiers changed from: 0000 */
    public void setDaySelectorColor(ColorStateList colorStateList) {
        this.mDaySelectorColor = colorStateList;
        notifyDataSetChanged();
    }

    /* access modifiers changed from: 0000 */
    public void setMonthTextAppearance(int i) {
        this.mMonthTextAppearance = i;
        notifyDataSetChanged();
    }

    /* access modifiers changed from: 0000 */
    public void setDayOfWeekTextAppearance(int i) {
        this.mDayOfWeekTextAppearance = i;
        notifyDataSetChanged();
    }

    /* access modifiers changed from: 0000 */
    public void setDayTextAppearance(int i) {
        this.mDayTextAppearance = i;
        notifyDataSetChanged();
    }

    public int getCount() {
        return this.mCount;
    }

    public boolean isViewFromObject(View view, Object obj) {
        return view == ((ViewHolder) obj).container;
    }

    private int getMonthForPosition(int i) {
        return (i + this.mMinDate.get(2)) % 12;
    }

    private int getYearForPosition(int i) {
        return ((i + this.mMinDate.get(2)) / 12) + this.mMinDate.get(1);
    }

    private int getPositionForDay(Calendar calendar) {
        if (calendar == null) {
            return -1;
        }
        return ((calendar.get(1) - this.mMinDate.get(1)) * 12) + (calendar.get(2) - this.mMinDate.get(2));
    }

    public Object instantiateItem(ViewGroup viewGroup, int i) {
        View inflate = this.mInflater.inflate(this.mLayoutResId, viewGroup, false);
        SimpleMonthView simpleMonthView = (SimpleMonthView) inflate.findViewById(this.mCalendarViewId);
        simpleMonthView.setOnDayClickListener(this.mOnDayClickListener);
        simpleMonthView.setMonthTextAppearance(this.mMonthTextAppearance);
        simpleMonthView.setDayOfWeekTextAppearance(this.mDayOfWeekTextAppearance);
        simpleMonthView.setDayTextAppearance(this.mDayTextAppearance);
        ColorStateList colorStateList = this.mDaySelectorColor;
        if (colorStateList != null) {
            simpleMonthView.setDaySelectorColor(colorStateList);
        }
        ColorStateList colorStateList2 = this.mDayHighlightColor;
        if (colorStateList2 != null) {
            simpleMonthView.setDayHighlightColor(colorStateList2);
        }
        ColorStateList colorStateList3 = this.mCalendarTextColor;
        if (colorStateList3 != null) {
            simpleMonthView.setMonthTextColor(colorStateList3);
            simpleMonthView.setDayOfWeekTextColor(this.mCalendarTextColor);
            simpleMonthView.setDayTextColor(this.mCalendarTextColor);
        }
        int monthForPosition = getMonthForPosition(i);
        int yearForPosition = getYearForPosition(i);
        Calendar calendar = this.mSelectedDay;
        simpleMonthView.setMonthParams((calendar == null || calendar.get(2) != monthForPosition) ? -1 : this.mSelectedDay.get(5), monthForPosition, yearForPosition, this.mFirstDayOfWeek, (this.mMinDate.get(2) == monthForPosition && this.mMinDate.get(1) == yearForPosition) ? this.mMinDate.get(5) : 1, (this.mMaxDate.get(2) == monthForPosition && this.mMaxDate.get(1) == yearForPosition) ? this.mMaxDate.get(5) : 31);
        ViewHolder viewHolder = new ViewHolder(i, inflate, simpleMonthView);
        this.mItems.put(i, viewHolder);
        viewGroup.addView(inflate);
        return viewHolder;
    }

    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        viewGroup.removeView(((ViewHolder) obj).container);
        this.mItems.remove(i);
    }

    public int getItemPosition(Object obj) {
        return ((ViewHolder) obj).position;
    }

    public CharSequence getPageTitle(int i) {
        SimpleMonthView simpleMonthView = ((ViewHolder) this.mItems.get(i)).calendar;
        if (simpleMonthView != null) {
            return simpleMonthView.getMonthYearLabel();
        }
        return null;
    }
}

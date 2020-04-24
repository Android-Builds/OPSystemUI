package com.oneplus.aod.slice;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.util.Log;
import com.android.systemui.R$plurals;
import com.oneplus.aod.slice.OpSliceManager.Callback;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class OpCalendarSlice extends OpSlice {
    private static final String[] EVENT_PROJECTION = {"_id", "title", "allDay", "dtstart", "eventLocation"};
    private Calendar mCalendar = null;
    private Callback mCallback;
    private ContentResolver mContentResolver;
    private Context mContext;
    private final String mDateFormat = "dd/MM/yyyy hh:mm";
    private CalendarEvent mEvent = null;
    private final int mEventIntervalInMin = 45;
    private Uri mEventUri;
    private final int mMillisInMinute = 60000;
    private Uri mReminderUri;

    private class CalendarEvent {
        public long mDateStartTimeInMillis;
        public int mID;
        public String mLocation;
        public long mReminderTimeInMillis;
        public String mTitle;

        public CalendarEvent(int i, String str, long j, String str2) {
            this.mID = i;
            this.mTitle = str;
            this.mDateStartTimeInMillis = j;
            this.mLocation = str2;
        }

        public void setReminderInMinutes(int i) {
            Calendar instance = Calendar.getInstance();
            instance.setTimeInMillis(this.mDateStartTimeInMillis);
            instance.add(12, 0 - i);
            this.mReminderTimeInMillis = instance.getTimeInMillis();
        }

        public int getEventIntervalInMinutes() {
            long timeInMillis = this.mDateStartTimeInMillis - Calendar.getInstance().getTimeInMillis();
            if (timeInMillis <= 0) {
                return 0;
            }
            return ((int) (timeInMillis / 60000)) + 1;
        }
    }

    public OpCalendarSlice(Context context, Callback callback) {
        super(callback);
        this.mContext = context;
        this.mCallback = callback;
        this.mContentResolver = context.getContentResolver();
        this.mCalendar = Calendar.getInstance();
        this.mEventUri = Events.CONTENT_URI;
        this.mReminderUri = Reminders.CONTENT_URI;
    }

    private void updateEvent() {
        getSoonestEvent();
        CalendarEvent calendarEvent = this.mEvent;
        if (calendarEvent != null) {
            this.mPrimary = calendarEvent.mTitle;
            this.mSecondary = calendarEvent.mLocation;
            int eventIntervalInMinutes = calendarEvent.getEventIntervalInMinutes();
            this.mRemark = this.mContext.getResources().getQuantityString(R$plurals.smart_aod_calendar_remain_time, eventIntervalInMinutes, new Object[]{Integer.valueOf(eventIntervalInMinutes)});
            if (OpSlice.DEBUG) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm");
                this.mCalendar.setTimeInMillis(this.mEvent.mDateStartTimeInMillis);
                String format = simpleDateFormat.format(this.mCalendar.getTime());
                this.mCalendar.setTimeInMillis(this.mEvent.mReminderTimeInMillis);
                String format2 = simpleDateFormat.format(this.mCalendar.getTime());
                String str = this.mTag;
                StringBuilder sb = new StringBuilder();
                sb.append("Event time = ");
                sb.append(format);
                sb.append(", reminderTime = ");
                sb.append(format2);
                sb.append(", title = ");
                sb.append(this.mPrimary);
                sb.append(", location = ");
                sb.append(this.mSecondary);
                Log.i(str, sb.toString());
            }
            if (this.mEvent.getEventIntervalInMinutes() <= 0 || this.mEvent.getEventIntervalInMinutes() > 45) {
                setActive(false);
                return;
            }
            setActive(true);
            updateUI();
            return;
        }
        setActive(false);
    }

    /* access modifiers changed from: protected */
    public void handleSetListening(boolean z) {
        super.handleSetListening(z);
        if (z) {
            updateEvent();
        }
    }

    public void handleTimeChanged() {
        super.handleTimeChanged();
        updateEvent();
    }

    private void getSoonestEvent() {
        int i;
        this.mEvent = null;
        Cursor query = this.mContentResolver.query(this.mEventUri, EVENT_PROJECTION, "((hasAlarm = 1) AND (dtstart >= ?) AND (deleted != 1))", new String[]{String.valueOf(Calendar.getInstance().getTimeInMillis())}, "dtstart LIMIT 1");
        if (!(query == null || query.getCount() == 0)) {
            if (query.moveToNext()) {
                i = query.getInt(query.getColumnIndex("_id"));
                CalendarEvent calendarEvent = new CalendarEvent(i, query.getString(query.getColumnIndex("title")), query.getLong(query.getColumnIndex("dtstart")), query.getString(query.getColumnIndex("eventLocation")));
                this.mEvent = calendarEvent;
            } else {
                i = -1;
            }
            if (i != -1) {
                getReminder(i);
            }
        }
        if (query != null) {
            query.close();
        }
    }

    private void getReminder(int i) {
        Cursor query = this.mContentResolver.query(this.mReminderUri, null, "(event_id = ?)", new String[]{String.valueOf(i)}, "_id");
        if (!(query == null || query.getCount() == 0)) {
            int i2 = query.moveToNext() ? query.getInt(query.getColumnIndex("minutes")) : -1;
            if (i2 != -1) {
                CalendarEvent calendarEvent = this.mEvent;
                if (calendarEvent != null) {
                    calendarEvent.setReminderInMinutes(i2);
                }
            }
        }
        if (query != null) {
            query.close();
        }
    }
}

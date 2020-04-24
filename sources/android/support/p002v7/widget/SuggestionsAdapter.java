package android.support.p002v7.widget;

import android.database.Cursor;
import android.support.p000v4.widget.ResourceCursorAdapter;
import android.util.Log;
import android.view.View.OnClickListener;

/* renamed from: android.support.v7.widget.SuggestionsAdapter */
class SuggestionsAdapter extends ResourceCursorAdapter implements OnClickListener {
    public static String getColumnString(Cursor cursor, String str) {
        return getStringOrNull(cursor, cursor.getColumnIndex(str));
    }

    private static String getStringOrNull(Cursor cursor, int i) {
        if (i == -1) {
            return null;
        }
        try {
            return cursor.getString(i);
        } catch (Exception e) {
            Log.e("SuggestionsAdapter", "unexpected error retrieving valid column from cursor, did the remote process die?", e);
            return null;
        }
    }
}

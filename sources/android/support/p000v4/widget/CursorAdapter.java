package android.support.p000v4.widget;

import android.database.Cursor;
import android.widget.BaseAdapter;
import android.widget.Filterable;

/* renamed from: android.support.v4.widget.CursorAdapter */
public abstract class CursorAdapter extends BaseAdapter implements Filterable, CursorFilter$CursorFilterClient {
    public abstract CharSequence convertToString(Cursor cursor);

    public abstract Cursor getCursor();
}

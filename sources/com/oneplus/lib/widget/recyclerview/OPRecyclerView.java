package com.oneplus.lib.widget.recyclerview;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

public class OPRecyclerView extends RecyclerView {
    private final Rect mContentPadding = new Rect();

    public OPRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initialize(context, attributeSet, 0);
    }

    private void initialize(Context context, AttributeSet attributeSet, int i) {
        setClipToPadding(false);
    }
}

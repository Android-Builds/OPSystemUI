package com.android.systemui.statusbar.notification.row;

import android.app.NotificationChannel;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import com.android.systemui.R$id;
import java.util.ArrayList;
import java.util.List;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: ChannelEditorListView.kt */
public final class ChannelEditorListView extends LinearLayout {
    private AppControlView appControlRow;
    private List<NotificationChannel> channels = new ArrayList();

    public ChannelEditorListView(Context context, AttributeSet attributeSet) {
        Intrinsics.checkParameterIsNotNull(context, "c");
        Intrinsics.checkParameterIsNotNull(attributeSet, "attrs");
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        View findViewById = findViewById(R$id.app_control);
        Intrinsics.checkExpressionValueIsNotNull(findViewById, "findViewById(R.id.app_control)");
        this.appControlRow = (AppControlView) findViewById;
    }
}

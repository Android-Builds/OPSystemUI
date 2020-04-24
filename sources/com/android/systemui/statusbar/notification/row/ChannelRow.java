package com.android.systemui.statusbar.notification.row;

import android.app.NotificationChannel;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.systemui.R$id;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: ChannelEditorListView.kt */
public final class ChannelRow extends LinearLayout {
    private NotificationChannel channel;
    private TextView channelDescription;
    private TextView channelName;
    public ChannelEditorDialogController controller;

    /* renamed from: switch reason: not valid java name */
    private Switch f124switch;

    public ChannelRow(Context context, AttributeSet attributeSet) {
        Intrinsics.checkParameterIsNotNull(context, "c");
        Intrinsics.checkParameterIsNotNull(attributeSet, "attrs");
        super(context, attributeSet);
    }

    public final ChannelEditorDialogController getController() {
        ChannelEditorDialogController channelEditorDialogController = this.controller;
        if (channelEditorDialogController != null) {
            return channelEditorDialogController;
        }
        Intrinsics.throwUninitializedPropertyAccessException("controller");
        throw null;
    }

    public final NotificationChannel getChannel() {
        return this.channel;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        View findViewById = findViewById(R$id.channel_name);
        Intrinsics.checkExpressionValueIsNotNull(findViewById, "findViewById(R.id.channel_name)");
        this.channelName = (TextView) findViewById;
        View findViewById2 = findViewById(R$id.channel_description);
        Intrinsics.checkExpressionValueIsNotNull(findViewById2, "findViewById(R.id.channel_description)");
        this.channelDescription = (TextView) findViewById2;
        View findViewById3 = findViewById(R$id.toggle);
        Intrinsics.checkExpressionValueIsNotNull(findViewById3, "findViewById(R.id.toggle)");
        this.f124switch = (Switch) findViewById3;
        Switch switchR = this.f124switch;
        if (switchR != null) {
            switchR.setOnCheckedChangeListener(new ChannelRow$onFinishInflate$1(this));
        } else {
            Intrinsics.throwUninitializedPropertyAccessException("switch");
            throw null;
        }
    }
}

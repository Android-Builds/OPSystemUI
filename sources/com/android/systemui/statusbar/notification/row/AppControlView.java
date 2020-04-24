package com.android.systemui.statusbar.notification.row;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.systemui.R$id;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: ChannelEditorListView.kt */
public final class AppControlView extends LinearLayout {
    public TextView channelName;
    public ImageView iconView;

    /* renamed from: switch reason: not valid java name */
    public Switch f123switch;

    public AppControlView(Context context, AttributeSet attributeSet) {
        Intrinsics.checkParameterIsNotNull(context, "c");
        Intrinsics.checkParameterIsNotNull(attributeSet, "attrs");
        super(context, attributeSet);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        View findViewById = findViewById(R$id.icon);
        Intrinsics.checkExpressionValueIsNotNull(findViewById, "findViewById(R.id.icon)");
        this.iconView = (ImageView) findViewById;
        View findViewById2 = findViewById(R$id.app_name);
        Intrinsics.checkExpressionValueIsNotNull(findViewById2, "findViewById(R.id.app_name)");
        this.channelName = (TextView) findViewById2;
        View findViewById3 = findViewById(R$id.toggle);
        Intrinsics.checkExpressionValueIsNotNull(findViewById3, "findViewById(R.id.toggle)");
        this.f123switch = (Switch) findViewById3;
    }
}

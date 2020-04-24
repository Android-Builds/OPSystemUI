package com.android.systemui.charging;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.Interpolators;
import com.android.systemui.R$dimen;
import com.android.systemui.R$id;
import com.android.systemui.R$integer;
import com.android.systemui.R$layout;
import com.android.systemui.R$style;
import java.text.NumberFormat;

public class WirelessChargingLayout extends FrameLayout {
    public WirelessChargingLayout(Context context, int i, boolean z) {
        super(context);
        init(context, null, i, z);
    }

    public WirelessChargingLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet, false);
    }

    private void init(Context context, AttributeSet attributeSet, boolean z) {
        init(context, attributeSet, -1, false);
    }

    private void init(Context context, AttributeSet attributeSet, int i, boolean z) {
        int i2 = R$style.ChargingAnim_WallpaperBackground;
        if (z) {
            i2 = R$style.ChargingAnim_DarkBackground;
        }
        FrameLayout.inflate(new ContextThemeWrapper(context, i2), R$layout.wireless_charging_layout, this);
        Animatable animatable = (Animatable) ((ImageView) findViewById(R$id.wireless_charging_view)).getDrawable();
        TextView textView = (TextView) findViewById(R$id.wireless_charging_percentage);
        if (i != -1) {
            textView.setText(NumberFormat.getPercentInstance().format((double) (((float) i) / 100.0f)));
            textView.setAlpha(0.0f);
        }
        long integer = (long) context.getResources().getInteger(R$integer.wireless_charging_fade_offset);
        long integer2 = (long) context.getResources().getInteger(R$integer.wireless_charging_fade_duration);
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(textView, "textSize", new float[]{context.getResources().getFloat(R$dimen.wireless_charging_anim_battery_level_text_size_start), context.getResources().getFloat(R$dimen.wireless_charging_anim_battery_level_text_size_end)});
        ofFloat.setInterpolator(new PathInterpolator(0.0f, 0.0f, 0.0f, 1.0f));
        ofFloat.setDuration((long) context.getResources().getInteger(R$integer.wireless_charging_battery_level_text_scale_animation_duration));
        String str = "alpha";
        ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(textView, str, new float[]{0.0f, 1.0f});
        ofFloat2.setInterpolator(Interpolators.LINEAR);
        ofFloat2.setDuration((long) context.getResources().getInteger(R$integer.wireless_charging_battery_level_text_opacity_duration));
        ofFloat2.setStartDelay((long) context.getResources().getInteger(R$integer.wireless_charging_anim_opacity_offset));
        ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(textView, str, new float[]{1.0f, 0.0f});
        ofFloat3.setDuration(integer2);
        ofFloat3.setInterpolator(Interpolators.LINEAR);
        ofFloat3.setStartDelay(integer);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(new Animator[]{ofFloat, ofFloat2, ofFloat3});
        animatable.start();
        animatorSet.start();
    }
}

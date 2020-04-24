package com.android.keyguard.clock;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint.Style;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextClock;
import com.android.internal.colorextraction.ColorExtractor.GradientColors;
import com.android.keyguard.R$drawable;
import com.android.keyguard.R$id;
import com.android.keyguard.R$layout;
import com.android.keyguard.R$string;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.plugins.ClockPlugin;
import java.util.TimeZone;

public class BubbleClockController implements ClockPlugin {
    private ImageClock mAnalogClock;
    private final SmallClockPosition mClockPosition;
    private final SysuiColorExtractor mColorExtractor;
    private final LayoutInflater mLayoutInflater;
    private TextClock mLockClock;
    private View mLockClockContainer;
    private final ViewPreviewer mRenderer = new ViewPreviewer();
    private final Resources mResources;
    private ClockLayout mView;

    public String getName() {
        return "bubble";
    }

    public void setStyle(Style style) {
    }

    public void setTextColor(int i) {
    }

    public boolean shouldShowStatusArea() {
        return true;
    }

    public BubbleClockController(Resources resources, LayoutInflater layoutInflater, SysuiColorExtractor sysuiColorExtractor) {
        this.mResources = resources;
        this.mLayoutInflater = layoutInflater;
        this.mColorExtractor = sysuiColorExtractor;
        this.mClockPosition = new SmallClockPosition(resources);
    }

    private void createViews() {
        this.mView = (ClockLayout) this.mLayoutInflater.inflate(R$layout.bubble_clock, null);
        this.mAnalogClock = (ImageClock) this.mView.findViewById(R$id.analog_clock);
        this.mLockClockContainer = this.mLayoutInflater.inflate(R$layout.digital_clock, null);
        this.mLockClock = (TextClock) this.mLockClockContainer.findViewById(R$id.lock_screen_clock);
    }

    public void onDestroyView() {
        this.mView = null;
        this.mAnalogClock = null;
        this.mLockClockContainer = null;
        this.mLockClock = null;
    }

    public String getTitle() {
        return this.mResources.getString(R$string.clock_title_bubble);
    }

    public Bitmap getThumbnail() {
        return BitmapFactory.decodeResource(this.mResources, R$drawable.bubble_thumbnail);
    }

    public Bitmap getPreview(int i, int i2) {
        View bigClockView = getBigClockView();
        setDarkAmount(1.0f);
        setTextColor(-1);
        GradientColors colors = this.mColorExtractor.getColors(2);
        setColorPalette(colors.supportsDarkText(), colors.getColorPalette());
        onTimeTick();
        return this.mRenderer.createPreview(bigClockView, i, i2);
    }

    public View getView() {
        if (this.mLockClockContainer == null) {
            createViews();
        }
        return this.mLockClockContainer;
    }

    public View getBigClockView() {
        if (this.mView == null) {
            createViews();
        }
        return this.mView;
    }

    public int getPreferredY(int i) {
        return this.mClockPosition.getPreferredY();
    }

    public void setColorPalette(boolean z, int[] iArr) {
        if (iArr != null && iArr.length != 0) {
            int i = iArr[Math.max(0, iArr.length - 3)];
            this.mLockClock.setTextColor(i);
            this.mAnalogClock.setClockColors(i, i);
        }
    }

    public void setDarkAmount(float f) {
        this.mClockPosition.setDarkAmount(f);
        this.mView.setDarkAmount(f);
    }

    public void onTimeTick() {
        this.mAnalogClock.onTimeChanged();
        this.mView.onTimeChanged();
        this.mLockClock.refresh();
    }

    public void onTimeZoneChanged(TimeZone timeZone) {
        this.mAnalogClock.onTimeZoneChanged(timeZone);
    }
}

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

public class AnalogClockController implements ClockPlugin {
    private ImageClock mAnalogClock;
    private ClockLayout mBigClockView;
    private final SmallClockPosition mClockPosition;
    private final SysuiColorExtractor mColorExtractor;
    private final LayoutInflater mLayoutInflater;
    private TextClock mLockClock;
    private final ViewPreviewer mRenderer = new ViewPreviewer();
    private final Resources mResources;
    private View mView;

    public String getName() {
        return "analog";
    }

    public void setStyle(Style style) {
    }

    public void setTextColor(int i) {
    }

    public boolean shouldShowStatusArea() {
        return true;
    }

    public AnalogClockController(Resources resources, LayoutInflater layoutInflater, SysuiColorExtractor sysuiColorExtractor) {
        this.mResources = resources;
        this.mLayoutInflater = layoutInflater;
        this.mColorExtractor = sysuiColorExtractor;
        this.mClockPosition = new SmallClockPosition(resources);
    }

    private void createViews() {
        this.mBigClockView = (ClockLayout) this.mLayoutInflater.inflate(R$layout.analog_clock, null);
        this.mAnalogClock = (ImageClock) this.mBigClockView.findViewById(R$id.analog_clock);
        this.mView = this.mLayoutInflater.inflate(R$layout.digital_clock, null);
        this.mLockClock = (TextClock) this.mView.findViewById(R$id.lock_screen_clock);
    }

    public void onDestroyView() {
        this.mBigClockView = null;
        this.mAnalogClock = null;
        this.mView = null;
        this.mLockClock = null;
    }

    public String getTitle() {
        return this.mResources.getString(R$string.clock_title_analog);
    }

    public Bitmap getThumbnail() {
        return BitmapFactory.decodeResource(this.mResources, R$drawable.analog_thumbnail);
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
        if (this.mView == null) {
            createViews();
        }
        return this.mView;
    }

    public View getBigClockView() {
        if (this.mBigClockView == null) {
            createViews();
        }
        return this.mBigClockView;
    }

    public int getPreferredY(int i) {
        return this.mClockPosition.getPreferredY();
    }

    public void setColorPalette(boolean z, int[] iArr) {
        if (iArr != null && iArr.length != 0) {
            int length = iArr.length;
            int i = length - 2;
            this.mLockClock.setTextColor(iArr[Math.max(0, i)]);
            this.mAnalogClock.setClockColors(iArr[Math.max(0, length - 5)], iArr[Math.max(0, i)]);
        }
    }

    public void onTimeTick() {
        this.mAnalogClock.onTimeChanged();
        this.mBigClockView.onTimeChanged();
        this.mLockClock.refresh();
    }

    public void setDarkAmount(float f) {
        this.mClockPosition.setDarkAmount(f);
        this.mBigClockView.setDarkAmount(f);
    }

    public void onTimeZoneChanged(TimeZone timeZone) {
        this.mAnalogClock.onTimeZoneChanged(timeZone);
    }
}

package com.android.launcher3.icons;

import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.DrawableWrapper;
import android.util.AttributeSet;
import org.xmlpull.v1.XmlPullParser;

public class FixedScaleDrawable extends DrawableWrapper {
    private float mScaleX = 0.46669f;
    private float mScaleY = 0.46669f;

    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet) {
    }

    public void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Theme theme) {
    }

    public FixedScaleDrawable() {
        super(new ColorDrawable());
    }

    public void draw(Canvas canvas) {
        int save = canvas.save();
        canvas.scale(this.mScaleX, this.mScaleY, getBounds().exactCenterX(), getBounds().exactCenterY());
        super.draw(canvas);
        canvas.restoreToCount(save);
    }
}

package com.oneplus.networkspeed;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.systemui.R$dimen;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.StatusIconDisplayable;
import com.oneplus.plugin.OpLsState;

public class StatusBarOPCustView extends LinearLayout implements DarkReceiver, StatusIconDisplayable {
    private boolean mDirty = true;
    private StatusBarIconView mDotView;
    private OPCustView mOPCustView;
    private Rect mRect;
    private String mSlot;
    private int mTint;
    private boolean mVisible;
    private int mVisibleState = -1;

    private class OPCustView {
        private Context mContext;
        Class[] mValidArray = {TextView.class, NetworkSpeedView.class};
        private View mView;

        public OPCustView(Context context) {
            this.mContext = context;
        }

        public boolean setView(View view) {
            this.mView = view;
            boolean z = false;
            for (Class isInstance : this.mValidArray) {
                if (isInstance.isInstance(this.mView)) {
                    z = true;
                }
            }
            if (!z) {
                Log.w("StatusBarOPCustView", "Load StatusBarOPCustView error, the resource is not valid.");
                this.mView = new TextView(this.mContext);
            }
            return z;
        }

        public void setVisibility(int i) {
            this.mView.setVisibility(i);
        }

        public void setColor(int i) {
            if (this.mValidArray[0].isInstance(this.mView)) {
                ((TextView) this.mView).setTextColor(i);
            } else if (this.mValidArray[1].isInstance(this.mView)) {
                ((NetworkSpeedView) this.mView).setTextColor(i);
            }
        }
    }

    public static StatusBarOPCustView fromResId(Context context, int i) {
        StatusBarOPCustView statusBarOPCustView = new StatusBarOPCustView(context);
        View inflate = LayoutInflater.from(context).inflate(i, null);
        LayoutParams layoutParams = new LayoutParams(-2, -2);
        layoutParams.gravity = 16;
        statusBarOPCustView.addView(inflate, layoutParams);
        statusBarOPCustView.setView(inflate, context);
        statusBarOPCustView.initDotView();
        return statusBarOPCustView;
    }

    public StatusBarOPCustView(Context context) {
        super(context);
    }

    public StatusBarOPCustView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void setSlot(String str) {
        this.mSlot = str;
    }

    public void applyVisible(boolean z) {
        if (this.mVisible != z) {
            this.mVisible = z;
            setVisibility(z ? 0 : 8);
            updateState();
        }
    }

    public String getSlot() {
        return this.mSlot;
    }

    public void setStaticDrawableColor(int i) {
        this.mOPCustView.setColor(i);
        this.mDotView.setDecorColor(i);
    }

    public void setDecorColor(int i) {
        this.mDotView.setDecorColor(i);
    }

    public void setVisibleState(int i) {
        if (this.mVisibleState != i) {
            this.mVisibleState = i;
            updateState();
        }
    }

    public void setVisibleState(int i, boolean z) {
        setVisibleState(i);
    }

    public int getVisibleState() {
        return this.mVisibleState;
    }

    public boolean isIconVisible() {
        return this.mVisible;
    }

    public void onDarkChanged(Rect rect, float f, int i) {
        this.mRect = rect;
        this.mTint = i;
        applyColors();
    }

    private void applyColors() {
        Rect rect = this.mRect;
        if (rect != null) {
            int i = this.mTint;
            this.mOPCustView.setColor(DarkIconDispatcher.getTint(rect, this, i));
            this.mDotView.setDecorColor(i);
            this.mDotView.setIconColor(i, false);
        }
    }

    private boolean setView(View view, Context context) {
        if (this.mOPCustView == null) {
            this.mOPCustView = new OPCustView(context);
        }
        return this.mOPCustView.setView(view);
    }

    private void initDotView() {
        this.mDotView = new StatusBarIconView(this.mContext, this.mSlot, null);
        this.mDotView.setVisibleState(1);
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R$dimen.status_bar_icon_size);
        LayoutParams layoutParams = new LayoutParams(dimensionPixelSize, dimensionPixelSize);
        layoutParams.gravity = 8388627;
        addView(this.mDotView, layoutParams);
    }

    private void updateState() {
        if (this.mVisible) {
            int i = this.mVisibleState;
            if (i == 0) {
                this.mOPCustView.setVisibility(0);
                this.mDotView.setVisibility(8);
            } else if (i == 1) {
                this.mOPCustView.setVisibility(4);
                this.mDotView.setVisibility(0);
            } else if (i != 2) {
                this.mOPCustView.setVisibility(0);
                this.mDotView.setVisibility(8);
            } else {
                this.mOPCustView.setVisibility(8);
                this.mDotView.setVisibility(8);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (OpLsState.getInstance().getPhoneStatusBar().isInMultiWindow() && this.mDirty && getWidth() > 0) {
            applyColors();
            this.mDirty = false;
        }
    }

    public void setVisibility(int i) {
        super.setVisibility(i);
        if (OpLsState.getInstance().getPhoneStatusBar().isInMultiWindow() && i == 0) {
            if (getWidth() > 0) {
                applyColors();
            }
            this.mDirty = true;
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        this.mDirty = true;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int i, int i2) {
        super.onMeasure(0, 0);
    }
}

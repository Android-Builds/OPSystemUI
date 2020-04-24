package com.oneplus.networkspeed;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.R$id;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.ScreenLifecycle.Observer;
import com.oneplus.networkspeed.NetworkSpeedController.INetworkSpeedStateCallBack;

public class NetworkSpeedView extends LinearLayout implements INetworkSpeedStateCallBack {
    private Context mContext;
    private boolean mIsVisible;
    private NetworkSpeedController mNetworkSpeedController;
    private ScreenLifecycle mScreenLifecycle;
    private final Observer mScreenObserver;
    private String mTextDown;
    private String mTextUp;
    private TextView mTextViewDown;
    private TextView mTextViewUp;

    public NetworkSpeedView(Context context) {
        this(context, null);
    }

    public NetworkSpeedView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public NetworkSpeedView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIsVisible = false;
        this.mScreenObserver = new Observer() {
            public void onScreenTurnedOff() {
            }

            public void onScreenTurnedOn() {
                NetworkSpeedView.this.updateText();
            }
        };
        this.mNetworkSpeedController = (NetworkSpeedController) Dependency.get(NetworkSpeedController.class);
        this.mScreenLifecycle = (ScreenLifecycle) Dependency.get(ScreenLifecycle.class);
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mTextViewUp = (TextView) findViewById(R$id.speed_word_up);
        this.mTextViewDown = (TextView) findViewById(R$id.speed_word_down);
        Log.i("NetworkSpeedView", "onFinishInflate");
        this.mContext.getResources().getConfiguration();
        refreshTextView();
    }

    public void onSpeedChange(String str) {
        String[] split = str.split(":");
        if (split.length == 2) {
            this.mTextUp = split[0];
            this.mTextDown = split[1];
            updateText();
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerReceiver();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregisterReceiver();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append(" onConfigurationChanged:");
            sb.append(configuration);
            Log.i("NetworkSpeedView", sb.toString());
        }
    }

    public void registerReceiver() {
        this.mNetworkSpeedController.addCallback(this);
        this.mScreenLifecycle.addObserver(this.mScreenObserver);
    }

    public void unregisterReceiver() {
        this.mNetworkSpeedController.removeCallback(this);
        this.mScreenLifecycle.removeObserver(this.mScreenObserver);
    }

    public void setVisibility(int i) {
        super.setVisibility(i);
        boolean z = i == 0;
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append(" setVisibility:");
            sb.append(i);
            Log.i("NetworkSpeedView", sb.toString());
        }
        if (this.mIsVisible != z) {
            this.mIsVisible = z;
            updateText();
        }
    }

    /* access modifiers changed from: private */
    public void updateText() {
        boolean z = this.mScreenLifecycle.getScreenState() == 2;
        if (Build.DEBUG_ONEPLUS) {
            StringBuilder sb = new StringBuilder();
            sb.append(" updateText:");
            sb.append(this.mTextUp);
            sb.append(" ");
            sb.append(this.mTextDown);
            sb.append(" mIsVisible:");
            sb.append(this.mIsVisible);
            sb.append(" mScreenOn:");
            sb.append(z);
            Log.i("NetworkSpeedView", sb.toString());
        }
        if (this.mIsVisible && z) {
            TextView textView = this.mTextViewUp;
            if (textView != null && this.mTextViewDown != null) {
                textView.setText(this.mTextUp);
                this.mTextViewDown.setText(this.mTextDown);
            }
        }
    }

    public void setTextColor(int i) {
        TextView textView = this.mTextViewUp;
        if (textView != null && this.mTextViewDown != null) {
            textView.setTextColor(i);
            this.mTextViewDown.setTextColor(i);
        }
    }

    private void refreshTextView() {
        TextView textView = this.mTextViewUp;
        if (textView != null && this.mTextViewDown != null) {
            textView.setLetterSpacing(-0.05f);
            this.mTextViewDown.setLetterSpacing(0.05f);
        }
    }
}

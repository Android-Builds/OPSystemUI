package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.metrics.LogMaker;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.widget.ImageView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.R$id;
import com.android.systemui.R$styleable;
import com.android.systemui.bubbles.BubbleController;
import com.android.systemui.recents.OverviewProxyService;
import com.android.systemui.shared.system.QuickStepContract;
import com.android.systemui.statusbar.phone.ButtonInterface;
import com.android.systemui.statusbar.policy.KeyButtonRipple.Type;
import com.oneplus.util.OpNavBarUtils;

public class KeyButtonView extends ImageView implements ButtonInterface {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Build.DEBUG_ONEPLUS;
    /* access modifiers changed from: private */
    public static final String TAG = "KeyButtonView";
    private AudioManager mAudioManager;
    private final Runnable mCheckLongPress;
    /* access modifiers changed from: private */
    public int mCode;
    private int mContentDescriptionRes;
    private float mDarkIntensity;
    private long mDownTime;
    private boolean mGestureAborted;
    private boolean mHasOvalBg;
    private final InputManager mInputManager;
    private boolean mIsVertical;
    private int mKey;
    /* access modifiers changed from: private */
    public boolean mLongClicked;
    private final MetricsLogger mMetricsLogger;
    private OnClickListener mOnClickListener;
    private final Paint mOvalBgPaint;
    private final OverviewProxyService mOverviewProxyService;
    private final boolean mPlaySounds;
    private final KeyButtonRipple mRipple;
    private int mThemeColor;
    private int mTouchDownX;
    private int mTouchDownY;

    public KeyButtonView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public KeyButtonView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, InputManager.getInstance());
    }

    @VisibleForTesting
    public KeyButtonView(Context context, AttributeSet attributeSet, int i, InputManager inputManager) {
        super(context, attributeSet);
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mOvalBgPaint = new Paint(3);
        this.mHasOvalBg = false;
        this.mKey = 3;
        this.mThemeColor = 0;
        this.mCheckLongPress = new Runnable() {
            public void run() {
                if (!KeyButtonView.this.isPressed()) {
                    return;
                }
                if (KeyButtonView.this.isLongClickable()) {
                    KeyButtonView.this.performLongClick();
                    KeyButtonView.this.mLongClicked = true;
                    return;
                }
                if (KeyButtonView.DEBUG) {
                    String access$200 = KeyButtonView.TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Logpress mCode: ");
                    sb.append(KeyButtonView.this.mCode);
                    Log.i(access$200, sb.toString());
                }
                KeyButtonView.this.sendEvent(0, 128);
                KeyButtonView.this.sendAccessibilityEvent(2);
                KeyButtonView.this.mLongClicked = true;
            }
        };
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R$styleable.KeyButtonView, i, 0);
        this.mCode = obtainStyledAttributes.getInteger(R$styleable.KeyButtonView_keyCode, 0);
        this.mPlaySounds = obtainStyledAttributes.getBoolean(R$styleable.KeyButtonView_playSound, true);
        TypedValue typedValue = new TypedValue();
        if (obtainStyledAttributes.getValue(R$styleable.KeyButtonView_android_contentDescription, typedValue)) {
            this.mContentDescriptionRes = typedValue.resourceId;
        }
        obtainStyledAttributes.recycle();
        setClickable(true);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mRipple = new KeyButtonRipple(context, this);
        this.mOverviewProxyService = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        this.mInputManager = inputManager;
        setBackground(this.mRipple);
        setWillNotDraw(false);
        forceHasOverlappingRendering(false);
        this.mKey = determineKey();
        if (!OpNavBarUtils.isSupportCustomKeys() && this.mCode == 187) {
            this.mCode = 0;
        }
    }

    public boolean isClickable() {
        return this.mCode != 0 || super.isClickable();
    }

    public void setCode(int i) {
        this.mCode = i;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        super.setOnClickListener(onClickListener);
        this.mOnClickListener = onClickListener;
    }

    public void loadAsync(Icon icon) {
        new AsyncTask<Icon, Void, Drawable>() {
            /* access modifiers changed from: protected */
            public Drawable doInBackground(Icon... iconArr) {
                return iconArr[0].loadDrawable(KeyButtonView.this.mContext);
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Drawable drawable) {
                KeyButtonView.this.setImageDrawable(drawable);
            }
        }.execute(new Icon[]{icon});
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.mContentDescriptionRes != 0) {
            setContentDescription(this.mContext.getString(this.mContentDescriptionRes));
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        if (this.mCode != 0) {
            accessibilityNodeInfo.addAction(new AccessibilityAction(16, null));
            if (isLongClickable()) {
                accessibilityNodeInfo.addAction(new AccessibilityAction(32, null));
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        if (i != 0) {
            jumpDrawablesToCurrentState();
        }
    }

    public boolean performAccessibilityActionInternal(int i, Bundle bundle) {
        if (i == 16 && this.mCode != 0) {
            sendEvent(0, 0, SystemClock.uptimeMillis());
            sendEvent(1, 0);
            sendAccessibilityEvent(1);
            playSoundEffect(0);
            return true;
        } else if (i != 32 || this.mCode == 0) {
            return super.performAccessibilityActionInternal(i, bundle);
        } else {
            sendEvent(0, 128);
            sendEvent(1, 0);
            sendAccessibilityEvent(2);
            return true;
        }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean shouldShowSwipeUpUI = this.mOverviewProxyService.shouldShowSwipeUpUI();
        int action = motionEvent.getAction();
        if (action == 0) {
            this.mGestureAborted = false;
        }
        if (this.mGestureAborted) {
            setPressed(false);
            return false;
        }
        if (action == 0) {
            this.mDownTime = SystemClock.uptimeMillis();
            this.mLongClicked = false;
            setPressed(true);
            this.mTouchDownX = (int) motionEvent.getRawX();
            this.mTouchDownY = (int) motionEvent.getRawY();
            if (this.mCode != 0) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("ACTION_DOWN mCode: ");
                    sb.append(this.mCode);
                    Log.i(str, sb.toString());
                }
                sendEvent(0, 0, this.mDownTime);
            } else {
                performHapticFeedback(1);
            }
            if (!shouldShowSwipeUpUI) {
                playSoundEffect(0);
            }
            removeCallbacks(this.mCheckLongPress);
            postDelayed(this.mCheckLongPress, (long) ViewConfiguration.getLongPressTimeout());
        } else if (action == 1) {
            boolean z = isPressed() && !this.mLongClicked;
            setPressed(false);
            boolean z2 = SystemClock.uptimeMillis() - this.mDownTime > 150;
            if (shouldShowSwipeUpUI) {
                if (z) {
                    performHapticFeedback(1);
                    playSoundEffect(0);
                }
            } else if (z2 && !this.mLongClicked) {
                performHapticFeedback(8);
            }
            if (this.mCode != 0) {
                if (z) {
                    if (DEBUG) {
                        String str2 = TAG;
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("ACTION_UP mCode: ");
                        sb2.append(this.mCode);
                        Log.i(str2, sb2.toString());
                    }
                    sendEvent(1, 0);
                    sendAccessibilityEvent(1);
                } else {
                    sendEvent(1, 32);
                }
            } else if (z) {
                OnClickListener onClickListener = this.mOnClickListener;
                if (onClickListener != null) {
                    onClickListener.onClick(this);
                    sendAccessibilityEvent(1);
                }
            }
            removeCallbacks(this.mCheckLongPress);
        } else if (action == 2) {
            int rawX = (int) motionEvent.getRawX();
            int rawY = (int) motionEvent.getRawY();
            float quickStepTouchSlopPx = QuickStepContract.getQuickStepTouchSlopPx(getContext());
            if (((float) Math.abs(rawX - this.mTouchDownX)) > quickStepTouchSlopPx || ((float) Math.abs(rawY - this.mTouchDownY)) > quickStepTouchSlopPx) {
                setPressed(false);
                removeCallbacks(this.mCheckLongPress);
            }
        } else if (action == 3) {
            setPressed(false);
            if (this.mCode != 0) {
                sendEvent(1, 32);
            }
            removeCallbacks(this.mCheckLongPress);
        }
        return true;
    }

    public void setImageDrawable(Drawable drawable) {
        Type type;
        super.setImageDrawable(drawable);
        if (drawable != null && (drawable instanceof KeyButtonDrawable)) {
            KeyButtonDrawable keyButtonDrawable = (KeyButtonDrawable) drawable;
            keyButtonDrawable.setDarkIntensity(this.mDarkIntensity);
            this.mHasOvalBg = keyButtonDrawable.hasOvalBg();
            if (this.mHasOvalBg) {
                this.mOvalBgPaint.setColor(keyButtonDrawable.getDrawableBackgroundColor());
            }
            KeyButtonRipple keyButtonRipple = this.mRipple;
            if (keyButtonDrawable.hasOvalBg()) {
                type = Type.OVAL;
            } else {
                type = Type.ROUNDED_RECT;
            }
            keyButtonRipple.setType(type);
            if (OpNavBarUtils.isSupportCustomNavBar()) {
                updateThemeColorInternal();
            }
        }
    }

    public void playSoundEffect(int i) {
        if (this.mPlaySounds) {
            this.mAudioManager.playSoundEffect(i, ActivityManager.getCurrentUser());
        }
    }

    public void sendEvent(int i, int i2) {
        sendEvent(i, i2, SystemClock.uptimeMillis());
    }

    private void sendEvent(int i, int i2, long j) {
        int i3 = i2;
        this.mMetricsLogger.write(new LogMaker(931).setType(4).setSubtype(this.mCode).addTaggedData(933, Integer.valueOf(i)).addTaggedData(932, Integer.valueOf(i2)));
        if (this.mCode != 4 || i3 == 128) {
            int i4 = i;
        } else {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Back button event: ");
            sb.append(KeyEvent.actionToString(i));
            Log.i(str, sb.toString());
            if (i == 1) {
                this.mOverviewProxyService.notifyBackAction((i3 & 32) == 0, -1, -1, true, false);
            }
        }
        KeyEvent keyEvent = new KeyEvent(this.mDownTime, j, i, this.mCode, (i3 & 128) != 0 ? 1 : 0, 0, -1, 0, i3 | 8 | 64, 257);
        int displayId = getDisplay() != null ? getDisplay().getDisplayId() : -1;
        int expandedDisplayId = ((BubbleController) Dependency.get(BubbleController.class)).getExpandedDisplayId(this.mContext);
        if (this.mCode == 4 && expandedDisplayId != -1) {
            displayId = expandedDisplayId;
        }
        if (displayId != -1) {
            keyEvent.setDisplayId(displayId);
        }
        this.mInputManager.injectInputEvent(keyEvent, 0);
    }

    public void setRippleColor(int i) {
        KeyButtonRipple keyButtonRipple = this.mRipple;
        if (keyButtonRipple != null) {
            keyButtonRipple.setColor(i);
        }
    }

    public void updateThemeColor(int i) {
        this.mThemeColor = i;
        updateThemeColorInternal();
    }

    private void updateThemeColorInternal() {
        Drawable drawable = getDrawable();
        if (drawable != null && (drawable instanceof KeyButtonDrawable)) {
            ((KeyButtonDrawable) drawable).updateThemeColor(this.mThemeColor);
        }
        postInvalidate();
    }

    public void abortCurrentGesture() {
        setPressed(false);
        this.mRipple.abortDelayedRipple();
        this.mGestureAborted = true;
    }

    public void setDarkIntensity(float f) {
        this.mDarkIntensity = f;
        Drawable drawable = getDrawable();
        if (drawable != null && (drawable instanceof KeyButtonDrawable)) {
            ((KeyButtonDrawable) drawable).setDarkIntensity(f);
            invalidate();
        }
        this.mRipple.setDarkIntensity(f);
    }

    public void setDelayTouchFeedback(boolean z) {
        this.mRipple.setDelayTouchFeedback(z);
    }

    public void draw(Canvas canvas) {
        if (this.mHasOvalBg) {
            canvas.save();
            canvas.translate((float) ((getLeft() + getRight()) / 2), (float) ((getTop() + getBottom()) / 2));
            int min = Math.min(getWidth(), getHeight()) / 2;
            float f = (float) (-min);
            float f2 = (float) min;
            canvas.drawOval(f, f, f2, f2, this.mOvalBgPaint);
            canvas.restore();
        }
        super.draw(canvas);
    }

    public void setVertical(boolean z) {
        this.mIsVertical = z;
    }

    public boolean isLongClickable() {
        if (!OpNavBarUtils.isSupportCustomKeys() || this.mKey == 3) {
            return super.isLongClickable();
        }
        return false;
    }

    private int determineKey() {
        int id = getId();
        if (id == R$id.back) {
            return 0;
        }
        if (id == R$id.home) {
            return 1;
        }
        return id == R$id.recent_apps ? 2 : 3;
    }
}

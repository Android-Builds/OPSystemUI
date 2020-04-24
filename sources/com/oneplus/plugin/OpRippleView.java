package com.oneplus.plugin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.Random;

public class OpRippleView extends View {
    public static int MESSAGE_DELAY = 20;
    private int COLOR = Color.parseColor("#888888");
    /* access modifiers changed from: private */
    public float DURATION = 1000.0f;
    private float END_RADIUS_FISRT = 120.0f;
    private float END_RADIUS_SECOND = 150.0f;
    private float START_RADIUS_FIRST = 30.0f;
    private float START_RADIUS_SECOND = 50.0f;
    private int STROKE_WIDTH_FIRST = 4;
    private int STROKE_WIDTH_SECOUND = 2;
    private final String TAG = "OpRippleView";
    private Handler handler = new Handler() {
        public void handleMessage(Message message) {
            super.handleMessage(message);
            OpRippleView.this.invalidate();
            if (OpRippleView.this.isStartRipple) {
                OpRippleView.this.rippleFirstRadius = OpRippleView.this.rippleFirstRadius + 1;
                if (((float) OpRippleView.this.rippleFirstRadius) > OpRippleView.this.DURATION / ((float) OpRippleView.MESSAGE_DELAY)) {
                    OpRippleView.this.rippleFirstRadius = 0;
                }
                OpRippleView.this.rippleSecendRadius = OpRippleView.this.rippleSecendRadius + 1;
                if (((float) OpRippleView.this.rippleSecendRadius) > OpRippleView.this.DURATION / ((float) OpRippleView.MESSAGE_DELAY)) {
                    OpRippleView.this.rippleSecendRadius = 0;
                }
                sendEmptyMessageDelayed(0, (long) OpRippleView.MESSAGE_DELAY);
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean isStartRipple;
    private int mClickCount = 0;
    private float mOffsetFirst;
    private float mOffsetSecond;
    private int mPositionX;
    private int mPositionY;
    private Paint mRipplePaintFirst = new Paint();
    private Paint mRipplePaintSecond = new Paint();
    private int mScreenHeight;
    private int mScreenWidth;
    /* access modifiers changed from: private */
    public int rippleFirstRadius = -5;
    /* access modifiers changed from: private */
    public int rippleSecendRadius = 0;

    public OpRippleView(Context context) {
        super(context);
        init(context);
    }

    public OpRippleView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public OpRippleView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    private void init(Context context) {
        this.mRipplePaintFirst.setAntiAlias(true);
        this.mRipplePaintFirst.setStyle(Style.STROKE);
        this.mRipplePaintSecond.setAntiAlias(true);
        this.mRipplePaintSecond.setStyle(Style.STROKE);
        Display defaultDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        this.mScreenWidth = defaultDisplay.getWidth();
        this.mScreenHeight = defaultDisplay.getHeight();
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.isStartRipple) {
            int i = this.rippleFirstRadius;
            if (i >= 0) {
                this.mRipplePaintFirst.setAlpha((int) (Math.sin(((((double) MESSAGE_DELAY) * 3.1416d) / ((double) this.DURATION)) * ((double) i)) * 255.0d));
                canvas.drawCircle((float) this.mPositionX, (float) this.mPositionY, this.START_RADIUS_FIRST + (this.mOffsetFirst * ((float) this.rippleFirstRadius)), this.mRipplePaintFirst);
            }
            int i2 = this.rippleSecendRadius;
            if (i2 >= 0) {
                this.mRipplePaintSecond.setAlpha((int) (Math.sin(((((double) MESSAGE_DELAY) * 3.1416d) / ((double) this.DURATION)) * ((double) i2)) * 255.0d));
                canvas.drawCircle((float) this.mPositionX, (float) this.mPositionY, this.START_RADIUS_SECOND + (this.mOffsetSecond * ((float) this.rippleSecendRadius)), this.mRipplePaintSecond);
            }
        }
    }

    public void startRipple() {
        generatePosition();
        startRipple(0);
    }

    public void startRipple(int i) {
        this.isStartRipple = true;
        this.handler.sendEmptyMessageDelayed(0, (long) i);
    }

    public void stopRipple() {
        this.isStartRipple = false;
        this.handler.removeMessages(0);
    }

    public void prepare() {
        this.mClickCount = 0;
        generatePosition();
        float f = this.END_RADIUS_FISRT;
        float f2 = this.START_RADIUS_FIRST;
        float f3 = f - f2;
        int i = MESSAGE_DELAY;
        float f4 = f3 * ((float) i);
        float f5 = this.DURATION;
        this.mOffsetFirst = f4 / f5;
        this.mOffsetSecond = ((this.END_RADIUS_SECOND - f2) * ((float) i)) / f5;
        this.mRipplePaintFirst.setStrokeWidth((float) this.STROKE_WIDTH_FIRST);
        this.mRipplePaintFirst.setColor(this.COLOR);
        this.mRipplePaintSecond.setStrokeWidth((float) this.STROKE_WIDTH_SECOUND);
        this.mRipplePaintSecond.setColor(this.COLOR);
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean isValidPosition = isValidPosition(motionEvent);
        StringBuilder sb = new StringBuilder();
        sb.append("onTouchEvent: isValid = ");
        sb.append(isValidPosition);
        Log.d("OpRippleView", sb.toString());
        if (isValidPosition) {
            int i = this.mClickCount;
            if (i == 3) {
                OpLsState.getInstance().getPreventModeCtrl().stopPreventMode();
            } else {
                this.mClickCount = i + 1;
                stopRipple();
                startRipple();
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    public void generatePosition() {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        Random random = new Random();
        int i7 = this.mScreenWidth;
        float f = this.END_RADIUS_FISRT;
        int i8 = this.mScreenHeight;
        int i9 = this.mClickCount;
        if (i9 == 0) {
            i5 = (int) (((float) (i7 / 2)) - f);
            i6 = (int) f;
            i2 = (int) (((float) (i8 / 2)) - f);
            i3 = (int) f;
            this.mPositionX = (int) (((float) random.nextInt(i5 - i6)) + this.END_RADIUS_FISRT);
            this.mPositionY = (int) (((float) random.nextInt(i2 - i3)) + this.END_RADIUS_FISRT);
        } else {
            if (i9 == 1) {
                i = (int) (((float) i7) - f);
                i4 = (int) (((float) (i7 / 2)) + f);
                i2 = (int) (((float) (i8 / 2)) - f);
                i3 = (int) f;
                this.mPositionX = (int) (((float) (random.nextInt(i - i4) + (this.mScreenWidth / 2))) + this.END_RADIUS_FISRT);
                this.mPositionY = (int) (((float) random.nextInt(i2 - i3)) + this.END_RADIUS_FISRT);
            } else if (i9 == 2) {
                i = (int) (((float) i7) - f);
                i4 = (int) (((float) (i7 / 2)) + f);
                int i10 = (int) (((float) i8) - f);
                i3 = (int) (((float) (i8 / 2)) + f);
                this.mPositionX = (int) (((float) (random.nextInt(i - i4) + (this.mScreenWidth / 2))) + this.END_RADIUS_FISRT);
                this.mPositionY = (int) (((float) (random.nextInt(i10 - i3) + (this.mScreenHeight / 2))) + this.END_RADIUS_FISRT);
                i2 = i10;
            } else {
                i5 = (int) (((float) (i7 / 2)) - f);
                i6 = (int) f;
                int i11 = (int) (((float) i8) - f);
                i3 = (int) (((float) (i8 / 2)) + f);
                this.mPositionX = (int) (((float) random.nextInt(i5 - i6)) + this.END_RADIUS_FISRT);
                this.mPositionY = (int) (((float) (random.nextInt(i11 - i3) + (this.mScreenHeight / 2))) + this.END_RADIUS_FISRT);
                i2 = i11;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("generatePosition : click = ");
            sb.append(this.mClickCount);
            String str = ", ";
            sb.append(str);
            sb.append(i4);
            sb.append(" < x < ");
            sb.append(i);
            sb.append(str);
            sb.append(i3);
            sb.append(" < y < ");
            sb.append(i2);
            String str2 = "OpRippleView";
            Log.d(str2, sb.toString());
            StringBuilder sb2 = new StringBuilder();
            sb2.append("generatePosition: (");
            sb2.append(this.mPositionX);
            sb2.append(str);
            sb2.append(this.mPositionY);
            sb2.append(")");
            Log.d(str2, sb2.toString());
        }
        int i12 = i6;
        i = i5;
        i4 = i12;
        StringBuilder sb3 = new StringBuilder();
        sb3.append("generatePosition : click = ");
        sb3.append(this.mClickCount);
        String str3 = ", ";
        sb3.append(str3);
        sb3.append(i4);
        sb3.append(" < x < ");
        sb3.append(i);
        sb3.append(str3);
        sb3.append(i3);
        sb3.append(" < y < ");
        sb3.append(i2);
        String str22 = "OpRippleView";
        Log.d(str22, sb3.toString());
        StringBuilder sb22 = new StringBuilder();
        sb22.append("generatePosition: (");
        sb22.append(this.mPositionX);
        sb22.append(str3);
        sb22.append(this.mPositionY);
        sb22.append(")");
        Log.d(str22, sb22.toString());
    }

    public boolean isValidPosition(MotionEvent motionEvent) {
        if (motionEvent.getAction() != 0) {
            return false;
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        StringBuilder sb = new StringBuilder();
        sb.append("isValidPosition: (");
        sb.append(x);
        sb.append(", ");
        sb.append(y);
        sb.append(")");
        String str = "OpRippleView";
        Log.d(str, sb.toString());
        int i = this.mPositionX;
        float f = (float) i;
        float f2 = this.END_RADIUS_FISRT;
        if (x <= f + f2 && x >= ((float) i) - f2) {
            int i2 = this.mPositionY;
            if (y <= ((float) i2) + f2 && y > ((float) i2) - f2) {
                Log.d(str, "isValidPosition: true");
                return true;
            }
        }
        return false;
    }
}

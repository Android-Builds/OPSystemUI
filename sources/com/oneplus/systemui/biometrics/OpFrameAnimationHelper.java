package com.oneplus.systemui.biometrics;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Trace;
import android.util.Log;
import android.widget.ImageView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OpFrameAnimationHelper {
    /* access modifiers changed from: private */
    public long mAnimPostDelayTime;
    private boolean mAnimationRunning;
    ImageView mAnimationView;
    Bitmap[] mBitmapArray;
    /* access modifiers changed from: private */
    public Callbacks mCallback = null;
    ExecutorService mExecutorService;
    int[] mFrames = null;
    /* access modifiers changed from: private */
    public boolean mLoop;
    /* access modifiers changed from: private */
    public int mOrder = 0;
    int mPlayFrameNum = 0;
    int mStartFrameIndex;
    AtomicBoolean mStop = new AtomicBoolean(false);
    /* access modifiers changed from: private */
    public Runnable runnable = new Runnable() {
        public void run() {
            int access$000 = OpFrameAnimationHelper.this.mOrder;
            OpFrameAnimationHelper opFrameAnimationHelper = OpFrameAnimationHelper.this;
            if (access$000 < opFrameAnimationHelper.mPlayFrameNum) {
                StringBuilder sb = new StringBuilder();
                sb.append("set mBitmap = ");
                sb.append(OpFrameAnimationHelper.this.mOrder);
                Log.d("FrameAnimationHelper", sb.toString());
                OpFrameAnimationHelper opFrameAnimationHelper2 = OpFrameAnimationHelper.this;
                if (opFrameAnimationHelper2.mAnimationView == null) {
                    opFrameAnimationHelper2.mOrder = 0;
                } else if (opFrameAnimationHelper2.mBitmapArray == null) {
                    opFrameAnimationHelper2.mOrder = 0;
                } else {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("AnimationView.mBitmap#");
                    sb2.append(OpFrameAnimationHelper.this.mOrder);
                    Trace.traceBegin(8, sb2.toString());
                    OpFrameAnimationHelper opFrameAnimationHelper3 = OpFrameAnimationHelper.this;
                    opFrameAnimationHelper3.mAnimationView.setImageBitmap(opFrameAnimationHelper3.mBitmapArray[opFrameAnimationHelper3.mOrder]);
                    OpFrameAnimationHelper opFrameAnimationHelper4 = OpFrameAnimationHelper.this;
                    opFrameAnimationHelper4.mAnimationView.postDelayed(opFrameAnimationHelper4.runnable, OpFrameAnimationHelper.this.mAnimPostDelayTime);
                    Trace.traceEnd(8);
                    OpFrameAnimationHelper.this.mOrder = OpFrameAnimationHelper.this.mOrder + 1;
                }
            } else {
                if (opFrameAnimationHelper.mLoop) {
                    int access$0002 = OpFrameAnimationHelper.this.mOrder;
                    OpFrameAnimationHelper opFrameAnimationHelper5 = OpFrameAnimationHelper.this;
                    if (access$0002 == opFrameAnimationHelper5.mPlayFrameNum && opFrameAnimationHelper5.mCallback != null) {
                        OpFrameAnimationHelper.this.mCallback.animationFinished();
                        OpFrameAnimationHelper.this.mCallback = null;
                        OpFrameAnimationHelper.this.mLoop = false;
                    }
                }
                if (OpFrameAnimationHelper.this.mLoop) {
                    OpFrameAnimationHelper.this.start(true);
                } else {
                    ImageView imageView = OpFrameAnimationHelper.this.mAnimationView;
                    if (imageView != null) {
                        imageView.setVisibility(4);
                    }
                }
            }
        }
    };

    public interface Callbacks {
        void animationFinished() {
        }
    }

    private static class DecodeBitmapTask implements Runnable {
        private OpFrameAnimationHelper mHelper;
        private int mIndex;

        public DecodeBitmapTask(OpFrameAnimationHelper opFrameAnimationHelper, int i) {
            this.mHelper = opFrameAnimationHelper;
            this.mIndex = i;
        }

        public void run() {
            if (this.mHelper != null) {
                String str = "FrameAnimationHelper";
                if (Build.DEBUG_ONEPLUS) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("decode bitmap = ");
                    sb.append(this.mIndex);
                    Log.d(str, sb.toString());
                }
                OpFrameAnimationHelper opFrameAnimationHelper = this.mHelper;
                if (opFrameAnimationHelper.mAnimationView != null) {
                    ExecutorService executorService = opFrameAnimationHelper.mExecutorService;
                    if (executorService != null && !executorService.isShutdown()) {
                        if (this.mHelper.mStop.get()) {
                            Log.d(str, "resetResource return");
                            return;
                        }
                        OpFrameAnimationHelper opFrameAnimationHelper2 = this.mHelper;
                        int i = opFrameAnimationHelper2.mStartFrameIndex;
                        int i2 = this.mIndex + i;
                        if (i2 < opFrameAnimationHelper2.mPlayFrameNum + i) {
                            ImageView imageView = opFrameAnimationHelper2.mAnimationView;
                            if (imageView != null) {
                                Bitmap decodeResource = BitmapFactory.decodeResource(imageView.getResources(), this.mHelper.mFrames[i2]);
                                Bitmap[] bitmapArr = this.mHelper.mBitmapArray;
                                if (bitmapArr != null) {
                                    bitmapArr[this.mIndex] = decodeResource;
                                }
                            }
                            try {
                                TimeUnit.MILLISECONDS.sleep(5);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    public OpFrameAnimationHelper(ImageView imageView, int[] iArr, long j, int i, int i2) {
        this.mAnimationView = imageView;
        if (j < 0) {
            j = 0;
        }
        this.mAnimPostDelayTime = j;
        this.mFrames = iArr;
        int[] iArr2 = this.mFrames;
        if (iArr2 != null && iArr2.length >= 1) {
            this.mStartFrameIndex = i;
            this.mPlayFrameNum = i2;
        }
    }

    public void startExecutor() {
        if (this.mExecutorService == null) {
            this.mExecutorService = Executors.newFixedThreadPool(2);
            for (int i = 0; i < this.mPlayFrameNum; i++) {
                this.mExecutorService.execute(new DecodeBitmapTask(this, i));
            }
        }
    }

    public void start(boolean z) {
        int[] iArr = this.mFrames;
        if (iArr != null && iArr.length >= 1 && this.mPlayFrameNum != 0) {
            Log.d("FrameAnimationHelper", "start");
            this.mAnimationRunning = true;
            prepareResource();
            this.mOrder = 0;
            this.mLoop = z;
            this.mAnimationView.setImageResource(17170445);
            this.mAnimationView.setVisibility(0);
            this.mAnimationView.postDelayed(this.runnable, 50);
        }
    }

    public void stop() {
        int[] iArr = this.mFrames;
        if (iArr != null && iArr.length >= 1) {
            Log.d("FrameAnimationHelper", "stop");
            this.mAnimationRunning = false;
            ImageView imageView = this.mAnimationView;
            if (imageView != null) {
                this.mOrder = 0;
                this.mLoop = false;
                this.mCallback = null;
                imageView.removeCallbacks(this.runnable);
                this.mAnimationView.setImageBitmap(null);
                this.mAnimationView.setVisibility(4);
            }
        }
    }

    public void resetResource() {
        Log.d("FrameAnimationHelper", "resetResource");
        this.mStop.set(true);
        if (this.mBitmapArray != null) {
            int i = 0;
            while (true) {
                Bitmap[] bitmapArr = this.mBitmapArray;
                if (i >= bitmapArr.length) {
                    break;
                }
                if (!(bitmapArr == null || bitmapArr[i] == null)) {
                    bitmapArr[i].recycle();
                }
                i++;
            }
        }
        ExecutorService executorService = this.mExecutorService;
        if (executorService != null) {
            executorService.shutdown();
            this.mExecutorService.shutdownNow();
            this.mExecutorService = null;
        }
        this.mBitmapArray = null;
        this.mAnimationView.removeCallbacks(this.runnable);
        this.mAnimationView = null;
        this.mAnimationRunning = false;
    }

    public void prepareResource() {
        if (this.mAnimationView != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("prepareResource startFrameIndex = ");
            sb.append(this.mStartFrameIndex);
            sb.append(" frameNum = ");
            sb.append(this.mPlayFrameNum);
            Log.d("FrameAnimationHelper", sb.toString());
            if (this.mBitmapArray == null) {
                this.mBitmapArray = new Bitmap[this.mPlayFrameNum];
            }
            this.mStop.set(false);
            startExecutor();
        }
    }

    public void updateAnimPostDelayTime(long j) {
        if (j < 0) {
            j = 0;
        }
        this.mAnimPostDelayTime = j;
    }

    public void waitAnimationFinished(Callbacks callbacks) {
        this.mCallback = callbacks;
        if (this.mCallback != null) {
            int[] iArr = this.mFrames;
            if (iArr == null || iArr.length == 0) {
                this.mCallback.animationFinished();
            }
        }
    }

    public boolean isAnimationRunning() {
        return this.mAnimationRunning;
    }
}

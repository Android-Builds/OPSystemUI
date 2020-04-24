package com.android.systemui.glwallpaper;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

class ImageGLWallpaper {
    private static final String TAG = "ImageGLWallpaper";
    private static final float[] TEXTURES = {0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    private static final float[] VERTICES = {-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f};
    private int mAttrPosition;
    private int mAttrTextureCoordinates;
    private float[] mCurrentTexCoordinate;
    private final ImageGLProgram mProgram;
    private final FloatBuffer mTextureBuffer;
    private int mTextureId;
    private int mUniAod2Opacity;
    private int mUniPer85;
    private int mUniReveal;
    private int mUniTexture;
    private final FloatBuffer mVertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

    ImageGLWallpaper(ImageGLProgram imageGLProgram) {
        this.mProgram = imageGLProgram;
        this.mVertexBuffer.put(VERTICES);
        this.mVertexBuffer.position(0);
        this.mTextureBuffer = ByteBuffer.allocateDirect(TEXTURES.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mTextureBuffer.put(TEXTURES);
        this.mTextureBuffer.position(0);
    }

    /* access modifiers changed from: 0000 */
    public void setup(Bitmap bitmap) {
        setupAttributes();
        setupUniforms();
        setupTexture(bitmap);
    }

    private void setupAttributes() {
        this.mAttrPosition = this.mProgram.getAttributeHandle("aPosition");
        this.mVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mAttrPosition, 2, 5126, false, 0, this.mVertexBuffer);
        GLES20.glEnableVertexAttribArray(this.mAttrPosition);
        this.mAttrTextureCoordinates = this.mProgram.getAttributeHandle("aTextureCoordinates");
        this.mTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mAttrTextureCoordinates, 2, 5126, false, 0, this.mTextureBuffer);
        GLES20.glEnableVertexAttribArray(this.mAttrTextureCoordinates);
    }

    private void setupUniforms() {
        this.mUniAod2Opacity = this.mProgram.getUniformHandle("uAod2Opacity");
        this.mUniPer85 = this.mProgram.getUniformHandle("uPer85");
        this.mUniReveal = this.mProgram.getUniformHandle("uReveal");
        this.mUniTexture = this.mProgram.getUniformHandle("uTexture");
    }

    /* access modifiers changed from: 0000 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getHandle(java.lang.String r8) {
        /*
            r7 = this;
            int r0 = r8.hashCode()
            r1 = 5
            r2 = 4
            r3 = 3
            r4 = 2
            r5 = 1
            r6 = -1
            switch(r0) {
                case -2002784538: goto L_0x0043;
                case -1971276870: goto L_0x0038;
                case -1091770206: goto L_0x002d;
                case -868354715: goto L_0x0022;
                case 17245217: goto L_0x0018;
                case 1583025322: goto L_0x000e;
                default: goto L_0x000d;
            }
        L_0x000d:
            goto L_0x004e
        L_0x000e:
            java.lang.String r0 = "aPosition"
            boolean r8 = r8.equals(r0)
            if (r8 == 0) goto L_0x004e
            r8 = 0
            goto L_0x004f
        L_0x0018:
            java.lang.String r0 = "aTextureCoordinates"
            boolean r8 = r8.equals(r0)
            if (r8 == 0) goto L_0x004e
            r8 = r5
            goto L_0x004f
        L_0x0022:
            java.lang.String r0 = "uPer85"
            boolean r8 = r8.equals(r0)
            if (r8 == 0) goto L_0x004e
            r8 = r3
            goto L_0x004f
        L_0x002d:
            java.lang.String r0 = "uReveal"
            boolean r8 = r8.equals(r0)
            if (r8 == 0) goto L_0x004e
            r8 = r2
            goto L_0x004f
        L_0x0038:
            java.lang.String r0 = "uAod2Opacity"
            boolean r8 = r8.equals(r0)
            if (r8 == 0) goto L_0x004e
            r8 = r4
            goto L_0x004f
        L_0x0043:
            java.lang.String r0 = "uTexture"
            boolean r8 = r8.equals(r0)
            if (r8 == 0) goto L_0x004e
            r8 = r1
            goto L_0x004f
        L_0x004e:
            r8 = r6
        L_0x004f:
            if (r8 == 0) goto L_0x006b
            if (r8 == r5) goto L_0x0068
            if (r8 == r4) goto L_0x0065
            if (r8 == r3) goto L_0x0062
            if (r8 == r2) goto L_0x005f
            if (r8 == r1) goto L_0x005c
            return r6
        L_0x005c:
            int r7 = r7.mUniTexture
            return r7
        L_0x005f:
            int r7 = r7.mUniReveal
            return r7
        L_0x0062:
            int r7 = r7.mUniPer85
            return r7
        L_0x0065:
            int r7 = r7.mUniAod2Opacity
            return r7
        L_0x0068:
            int r7 = r7.mAttrTextureCoordinates
            return r7
        L_0x006b:
            int r7 = r7.mAttrPosition
            return r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.glwallpaper.ImageGLWallpaper.getHandle(java.lang.String):int");
    }

    /* access modifiers changed from: 0000 */
    public void draw() {
        GLES20.glDrawArrays(4, 0, VERTICES.length / 2);
    }

    private void setupTexture(Bitmap bitmap) {
        int[] iArr = new int[1];
        if (bitmap == null) {
            Log.w(TAG, "setupTexture: invalid bitmap");
            return;
        }
        GLES20.glGenTextures(1, iArr, 0);
        if (iArr[0] == 0) {
            Log.w(TAG, "setupTexture: glGenTextures() failed");
            return;
        }
        GLES20.glBindTexture(3553, iArr[0]);
        GLUtils.texImage2D(3553, 0, bitmap, 0);
        GLES20.glTexParameteri(3553, 10241, 9729);
        GLES20.glTexParameteri(3553, 10240, 9729);
        this.mTextureId = iArr[0];
    }

    /* access modifiers changed from: 0000 */
    public void useTexture() {
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, this.mTextureId);
        GLES20.glUniform1i(this.mUniTexture, 0);
    }

    /* access modifiers changed from: 0000 */
    public void adjustTextureCoordinates(Rect rect, Rect rect2, float f, float f2) {
        this.mCurrentTexCoordinate = (float[]) TEXTURES.clone();
        if (rect == null || rect2 == null) {
            this.mTextureBuffer.put(this.mCurrentTexCoordinate);
            this.mTextureBuffer.position(0);
            return;
        }
        int width = rect.width();
        int height = rect.height();
        int width2 = rect2.width();
        int height2 = rect2.height();
        if (width > width2) {
            float f3 = (float) width;
            float round = ((float) Math.round(((float) (width - width2)) * f)) / f3;
            float f4 = ((float) width2) / f3;
            if (height < height2) {
                f4 *= ((float) height) / ((float) height2);
            }
            if (round + f4 > 1.0f) {
                round = 1.0f - f4;
            }
            int i = 0;
            while (true) {
                float[] fArr = this.mCurrentTexCoordinate;
                if (i >= fArr.length) {
                    break;
                }
                if (i == 2 || i == 4 || i == 6) {
                    this.mCurrentTexCoordinate[i] = Math.min(1.0f, round + f4);
                } else {
                    fArr[i] = round;
                }
                i += 2;
            }
        }
        if (height > height2) {
            float f5 = (float) height;
            float round2 = ((float) Math.round(((float) (height - height2)) * f2)) / f5;
            float f6 = ((float) height2) / f5;
            if (width < width2) {
                f6 *= ((float) width) / ((float) width2);
            }
            if (round2 + f6 > 1.0f) {
                round2 = 1.0f - f6;
            }
            int i2 = 1;
            while (true) {
                float[] fArr2 = this.mCurrentTexCoordinate;
                if (i2 >= fArr2.length) {
                    break;
                }
                if (i2 == 1 || i2 == 3 || i2 == 11) {
                    this.mCurrentTexCoordinate[i2] = Math.min(1.0f, round2 + f6);
                } else {
                    fArr2[i2] = round2;
                }
                i2 += 2;
            }
        }
        this.mTextureBuffer.put(this.mCurrentTexCoordinate);
        this.mTextureBuffer.position(0);
    }

    public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        if (this.mCurrentTexCoordinate != null) {
            int i = 0;
            while (true) {
                float[] fArr = this.mCurrentTexCoordinate;
                if (i >= fArr.length) {
                    break;
                }
                sb.append(fArr[i]);
                sb.append(',');
                if (i == this.mCurrentTexCoordinate.length - 1) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                i++;
            }
        }
        sb.append('}');
        printWriter.print(str);
        printWriter.print("mTexCoordinates=");
        printWriter.println(sb.toString());
    }
}

package com.android.keyguard.clock;

import android.graphics.Bitmap;
import java.util.function.Supplier;

final class ClockInfo {
    private final String mId;
    private final String mName;
    private final Supplier<Bitmap> mPreview;
    private final Supplier<Bitmap> mThumbnail;
    private final String mTitle;

    static class Builder {
        private String mId;
        private String mName;
        private Supplier<Bitmap> mPreview;
        private Supplier<Bitmap> mThumbnail;
        private String mTitle;

        Builder() {
        }

        public ClockInfo build() {
            ClockInfo clockInfo = new ClockInfo(this.mName, this.mTitle, this.mId, this.mThumbnail, this.mPreview);
            return clockInfo;
        }

        public Builder setName(String str) {
            this.mName = str;
            return this;
        }

        public Builder setTitle(String str) {
            this.mTitle = str;
            return this;
        }

        public Builder setId(String str) {
            this.mId = str;
            return this;
        }

        public Builder setThumbnail(Supplier<Bitmap> supplier) {
            this.mThumbnail = supplier;
            return this;
        }

        public Builder setPreview(Supplier<Bitmap> supplier) {
            this.mPreview = supplier;
            return this;
        }
    }

    private ClockInfo(String str, String str2, String str3, Supplier<Bitmap> supplier, Supplier<Bitmap> supplier2) {
        this.mName = str;
        this.mTitle = str2;
        this.mId = str3;
        this.mThumbnail = supplier;
        this.mPreview = supplier2;
    }

    /* access modifiers changed from: 0000 */
    public String getName() {
        return this.mName;
    }

    /* access modifiers changed from: 0000 */
    public String getTitle() {
        return this.mTitle;
    }

    /* access modifiers changed from: 0000 */
    public String getId() {
        return this.mId;
    }

    /* access modifiers changed from: 0000 */
    public Bitmap getThumbnail() {
        return (Bitmap) this.mThumbnail.get();
    }

    /* access modifiers changed from: 0000 */
    public Bitmap getPreview() {
        return (Bitmap) this.mPreview.get();
    }

    static Builder builder() {
        return new Builder();
    }
}

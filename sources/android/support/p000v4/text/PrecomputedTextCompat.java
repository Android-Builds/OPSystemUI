package android.support.p000v4.text;

import android.os.Build.VERSION;
import android.support.p000v4.p001os.BuildCompat;
import android.support.p000v4.util.ObjectsCompat;
import android.text.PrecomputedText;
import android.text.Spannable;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.MetricAffectingSpan;
import java.util.concurrent.Executor;

/* renamed from: android.support.v4.text.PrecomputedTextCompat */
public class PrecomputedTextCompat implements Spannable {
    private static Executor sExecutor = null;
    private static final Object sLock = new Object();
    private final Params mParams;
    private final Spannable mText;
    private final PrecomputedText mWrapped;

    /* renamed from: android.support.v4.text.PrecomputedTextCompat$Params */
    public static final class Params {
        private final int mBreakStrategy;
        private final int mHyphenationFrequency;
        private final TextPaint mPaint;
        private final TextDirectionHeuristic mTextDir;
        private final android.text.PrecomputedText.Params mWrapped;

        /* renamed from: android.support.v4.text.PrecomputedTextCompat$Params$Builder */
        public static class Builder {
            private int mBreakStrategy;
            private int mHyphenationFrequency;
            private final TextPaint mPaint;
            private TextDirectionHeuristic mTextDir;

            public Builder(TextPaint textPaint) {
                this.mPaint = textPaint;
                if (VERSION.SDK_INT >= 23) {
                    this.mBreakStrategy = 1;
                    this.mHyphenationFrequency = 1;
                } else {
                    this.mHyphenationFrequency = 0;
                    this.mBreakStrategy = 0;
                }
                if (VERSION.SDK_INT >= 18) {
                    this.mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
                } else {
                    this.mTextDir = null;
                }
            }

            public Builder setBreakStrategy(int i) {
                this.mBreakStrategy = i;
                return this;
            }

            public Builder setHyphenationFrequency(int i) {
                this.mHyphenationFrequency = i;
                return this;
            }

            public Builder setTextDirection(TextDirectionHeuristic textDirectionHeuristic) {
                this.mTextDir = textDirectionHeuristic;
                return this;
            }

            public Params build() {
                Params params = new Params(this.mPaint, this.mTextDir, this.mBreakStrategy, this.mHyphenationFrequency);
                return params;
            }
        }

        private Params(TextPaint textPaint, TextDirectionHeuristic textDirectionHeuristic, int i, int i2) {
            if (BuildCompat.isAtLeastP()) {
                this.mWrapped = new android.text.PrecomputedText.Params.Builder(textPaint).setBreakStrategy(i).setHyphenationFrequency(i2).setTextDirection(textDirectionHeuristic).build();
            } else {
                this.mWrapped = null;
            }
            this.mPaint = textPaint;
            this.mTextDir = textDirectionHeuristic;
            this.mBreakStrategy = i;
            this.mHyphenationFrequency = i2;
        }

        public Params(android.text.PrecomputedText.Params params) {
            this.mPaint = params.getTextPaint();
            this.mTextDir = params.getTextDirection();
            this.mBreakStrategy = params.getBreakStrategy();
            this.mHyphenationFrequency = params.getHyphenationFrequency();
            this.mWrapped = params;
        }

        public TextPaint getTextPaint() {
            return this.mPaint;
        }

        public TextDirectionHeuristic getTextDirection() {
            return this.mTextDir;
        }

        public int getBreakStrategy() {
            return this.mBreakStrategy;
        }

        public int getHyphenationFrequency() {
            return this.mHyphenationFrequency;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || !(obj instanceof Params)) {
                return false;
            }
            Params params = (Params) obj;
            android.text.PrecomputedText.Params params2 = this.mWrapped;
            if (params2 != null) {
                return params2.equals(params.mWrapped);
            }
            if (VERSION.SDK_INT >= 23 && (this.mBreakStrategy != params.getBreakStrategy() || this.mHyphenationFrequency != params.getHyphenationFrequency())) {
                return false;
            }
            if ((VERSION.SDK_INT >= 18 && this.mTextDir != params.getTextDirection()) || this.mPaint.getTextSize() != params.getTextPaint().getTextSize() || this.mPaint.getTextScaleX() != params.getTextPaint().getTextScaleX() || this.mPaint.getTextSkewX() != params.getTextPaint().getTextSkewX()) {
                return false;
            }
            if ((VERSION.SDK_INT >= 21 && (this.mPaint.getLetterSpacing() != params.getTextPaint().getLetterSpacing() || !TextUtils.equals(this.mPaint.getFontFeatureSettings(), params.getTextPaint().getFontFeatureSettings()))) || this.mPaint.getFlags() != params.getTextPaint().getFlags()) {
                return false;
            }
            int i = VERSION.SDK_INT;
            if (i >= 24) {
                if (!this.mPaint.getTextLocales().equals(params.getTextPaint().getTextLocales())) {
                    return false;
                }
            } else if (i >= 17 && !this.mPaint.getTextLocale().equals(params.getTextPaint().getTextLocale())) {
                return false;
            }
            if (this.mPaint.getTypeface() == null) {
                if (params.getTextPaint().getTypeface() != null) {
                    return false;
                }
            } else if (!this.mPaint.getTypeface().equals(params.getTextPaint().getTypeface())) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            int i = VERSION.SDK_INT;
            if (i >= 24) {
                return ObjectsCompat.hash(Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Float.valueOf(this.mPaint.getLetterSpacing()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTextLocales(), this.mPaint.getTypeface(), Boolean.valueOf(this.mPaint.isElegantTextHeight()), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency));
            } else if (i >= 21) {
                return ObjectsCompat.hash(Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Float.valueOf(this.mPaint.getLetterSpacing()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTextLocale(), this.mPaint.getTypeface(), Boolean.valueOf(this.mPaint.isElegantTextHeight()), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency));
            } else if (i >= 18) {
                return ObjectsCompat.hash(Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTextLocale(), this.mPaint.getTypeface(), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency));
            } else if (i >= 17) {
                return ObjectsCompat.hash(Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTextLocale(), this.mPaint.getTypeface(), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency));
            } else {
                return ObjectsCompat.hash(Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTypeface(), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency));
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            StringBuilder sb2 = new StringBuilder();
            sb2.append("textSize=");
            sb2.append(this.mPaint.getTextSize());
            sb.append(sb2.toString());
            StringBuilder sb3 = new StringBuilder();
            sb3.append(", textScaleX=");
            sb3.append(this.mPaint.getTextScaleX());
            sb.append(sb3.toString());
            StringBuilder sb4 = new StringBuilder();
            sb4.append(", textSkewX=");
            sb4.append(this.mPaint.getTextSkewX());
            sb.append(sb4.toString());
            if (VERSION.SDK_INT >= 21) {
                StringBuilder sb5 = new StringBuilder();
                sb5.append(", letterSpacing=");
                sb5.append(this.mPaint.getLetterSpacing());
                sb.append(sb5.toString());
                StringBuilder sb6 = new StringBuilder();
                sb6.append(", elegantTextHeight=");
                sb6.append(this.mPaint.isElegantTextHeight());
                sb.append(sb6.toString());
            }
            int i = VERSION.SDK_INT;
            String str = ", textLocale=";
            if (i >= 24) {
                StringBuilder sb7 = new StringBuilder();
                sb7.append(str);
                sb7.append(this.mPaint.getTextLocales());
                sb.append(sb7.toString());
            } else if (i >= 17) {
                StringBuilder sb8 = new StringBuilder();
                sb8.append(str);
                sb8.append(this.mPaint.getTextLocale());
                sb.append(sb8.toString());
            }
            StringBuilder sb9 = new StringBuilder();
            sb9.append(", typeface=");
            sb9.append(this.mPaint.getTypeface());
            sb.append(sb9.toString());
            if (VERSION.SDK_INT >= 26) {
                StringBuilder sb10 = new StringBuilder();
                sb10.append(", variationSettings=");
                sb10.append(this.mPaint.getFontVariationSettings());
                sb.append(sb10.toString());
            }
            StringBuilder sb11 = new StringBuilder();
            sb11.append(", textDir=");
            sb11.append(this.mTextDir);
            sb.append(sb11.toString());
            StringBuilder sb12 = new StringBuilder();
            sb12.append(", breakStrategy=");
            sb12.append(this.mBreakStrategy);
            sb.append(sb12.toString());
            StringBuilder sb13 = new StringBuilder();
            sb13.append(", hyphenationFrequency=");
            sb13.append(this.mHyphenationFrequency);
            sb.append(sb13.toString());
            sb.append("}");
            return sb.toString();
        }
    }

    public PrecomputedText getPrecomputedText() {
        Spannable spannable = this.mText;
        if (spannable instanceof PrecomputedText) {
            return (PrecomputedText) spannable;
        }
        return null;
    }

    public Params getParams() {
        return this.mParams;
    }

    public void setSpan(Object obj, int i, int i2, int i3) {
        if (obj instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException("MetricAffectingSpan can not be set to PrecomputedText.");
        } else if (BuildCompat.isAtLeastP()) {
            this.mWrapped.setSpan(obj, i, i2, i3);
        } else {
            this.mText.setSpan(obj, i, i2, i3);
        }
    }

    public void removeSpan(Object obj) {
        if (obj instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException("MetricAffectingSpan can not be removed from PrecomputedText.");
        } else if (BuildCompat.isAtLeastP()) {
            this.mWrapped.removeSpan(obj);
        } else {
            this.mText.removeSpan(obj);
        }
    }

    public <T> T[] getSpans(int i, int i2, Class<T> cls) {
        if (BuildCompat.isAtLeastP()) {
            return this.mWrapped.getSpans(i, i2, cls);
        }
        return this.mText.getSpans(i, i2, cls);
    }

    public int getSpanStart(Object obj) {
        return this.mText.getSpanStart(obj);
    }

    public int getSpanEnd(Object obj) {
        return this.mText.getSpanEnd(obj);
    }

    public int getSpanFlags(Object obj) {
        return this.mText.getSpanFlags(obj);
    }

    public int nextSpanTransition(int i, int i2, Class cls) {
        return this.mText.nextSpanTransition(i, i2, cls);
    }

    public int length() {
        return this.mText.length();
    }

    public char charAt(int i) {
        return this.mText.charAt(i);
    }

    public CharSequence subSequence(int i, int i2) {
        return this.mText.subSequence(i, i2);
    }

    public String toString() {
        return this.mText.toString();
    }
}

package com.airbnb.lottie.parser;

import android.graphics.PointF;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import androidx.collection.SparseArrayCompat;
import androidx.core.view.animation.PathInterpolatorCompat;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.parser.moshi.JsonReader.Options;
import com.airbnb.lottie.utils.MiscUtils;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.Keyframe;
import java.io.IOException;
import java.lang.ref.WeakReference;

class KeyframeParser {
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final float MAX_CP_VALUE = 100.0f;
    static Options NAMES = Options.m6of("t", "s", "e", "o", "i", "h", "to", "ti");
    private static SparseArrayCompat<WeakReference<Interpolator>> pathInterpolatorCache;

    KeyframeParser() {
    }

    private static SparseArrayCompat<WeakReference<Interpolator>> pathInterpolatorCache() {
        if (pathInterpolatorCache == null) {
            pathInterpolatorCache = new SparseArrayCompat<>();
        }
        return pathInterpolatorCache;
    }

    private static WeakReference<Interpolator> getInterpolator(int i) {
        WeakReference<Interpolator> weakReference;
        synchronized (KeyframeParser.class) {
            weakReference = (WeakReference) pathInterpolatorCache().get(i);
        }
        return weakReference;
    }

    private static void putInterpolator(int i, WeakReference<Interpolator> weakReference) {
        synchronized (KeyframeParser.class) {
            pathInterpolatorCache.put(i, weakReference);
        }
    }

    static <T> Keyframe<T> parse(JsonReader jsonReader, LottieComposition lottieComposition, float f, ValueParser<T> valueParser, boolean z) throws IOException {
        if (z) {
            return parseKeyframe(lottieComposition, jsonReader, f, valueParser);
        }
        return parseStaticValue(jsonReader, f, valueParser);
    }

    private static <T> Keyframe<T> parseKeyframe(LottieComposition lottieComposition, JsonReader jsonReader, float f, ValueParser<T> valueParser) throws IOException {
        Interpolator interpolator;
        Object obj;
        JsonReader jsonReader2 = jsonReader;
        float f2 = f;
        ValueParser<T> valueParser2 = valueParser;
        jsonReader.beginObject();
        Interpolator interpolator2 = null;
        PointF pointF = null;
        PointF pointF2 = null;
        Object obj2 = null;
        Object obj3 = null;
        PointF pointF3 = null;
        PointF pointF4 = null;
        float f3 = 0.0f;
        while (true) {
            boolean z = false;
            while (true) {
                if (jsonReader.hasNext()) {
                    switch (jsonReader2.selectName(NAMES)) {
                        case 0:
                            f3 = (float) jsonReader.nextDouble();
                            break;
                        case 1:
                            obj3 = valueParser2.parse(jsonReader2, f2);
                            break;
                        case 2:
                            obj2 = valueParser2.parse(jsonReader2, f2);
                            break;
                        case 3:
                            pointF = JsonUtils.jsonToPoint(jsonReader, f);
                            break;
                        case 4:
                            pointF2 = JsonUtils.jsonToPoint(jsonReader, f);
                            break;
                        case 5:
                            if (jsonReader.nextInt() == 1) {
                                z = true;
                                break;
                            }
                        case 6:
                            pointF3 = JsonUtils.jsonToPoint(jsonReader, f);
                            break;
                        case 7:
                            pointF4 = JsonUtils.jsonToPoint(jsonReader, f);
                            break;
                        default:
                            jsonReader.skipValue();
                            break;
                    }
                } else {
                    jsonReader.endObject();
                    if (z) {
                        interpolator = LINEAR_INTERPOLATOR;
                        obj = obj3;
                    } else {
                        if (pointF == null || pointF2 == null) {
                            interpolator = LINEAR_INTERPOLATOR;
                        } else {
                            float f4 = -f2;
                            pointF.x = MiscUtils.clamp(pointF.x, f4, f2);
                            pointF.y = MiscUtils.clamp(pointF.y, -100.0f, (float) MAX_CP_VALUE);
                            pointF2.x = MiscUtils.clamp(pointF2.x, f4, f2);
                            pointF2.y = MiscUtils.clamp(pointF2.y, -100.0f, (float) MAX_CP_VALUE);
                            int hashFor = Utils.hashFor(pointF.x, pointF.y, pointF2.x, pointF2.y);
                            WeakReference interpolator3 = getInterpolator(hashFor);
                            if (interpolator3 != null) {
                                interpolator2 = (Interpolator) interpolator3.get();
                            }
                            if (interpolator3 == null || interpolator2 == null) {
                                interpolator2 = PathInterpolatorCompat.create(pointF.x / f2, pointF.y / f2, pointF2.x / f2, pointF2.y / f2);
                                try {
                                    putInterpolator(hashFor, new WeakReference(interpolator2));
                                } catch (ArrayIndexOutOfBoundsException unused) {
                                }
                            }
                            interpolator = interpolator2;
                        }
                        obj = obj2;
                    }
                    Keyframe keyframe = new Keyframe(lottieComposition, obj3, obj, interpolator, f3, null);
                    keyframe.pathCp1 = pointF3;
                    keyframe.pathCp2 = pointF4;
                    return keyframe;
                }
            }
        }
    }

    private static <T> Keyframe<T> parseStaticValue(JsonReader jsonReader, float f, ValueParser<T> valueParser) throws IOException {
        return new Keyframe<>(valueParser.parse(jsonReader, f));
    }
}

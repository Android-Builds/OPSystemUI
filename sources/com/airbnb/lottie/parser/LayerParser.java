package com.airbnb.lottie.parser;

import android.graphics.Color;
import android.graphics.Rect;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.model.animatable.AnimatableFloatValue;
import com.airbnb.lottie.model.animatable.AnimatableTextFrame;
import com.airbnb.lottie.model.animatable.AnimatableTextProperties;
import com.airbnb.lottie.model.animatable.AnimatableTransform;
import com.airbnb.lottie.model.content.ContentModel;
import com.airbnb.lottie.model.layer.Layer;
import com.airbnb.lottie.model.layer.Layer.LayerType;
import com.airbnb.lottie.model.layer.Layer.MatteType;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.parser.moshi.JsonReader.Options;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.Keyframe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LayerParser {
    private static final Options EFFECTS_NAMES = Options.m6of("nm");
    private static final Options NAMES = Options.m6of("nm", "ind", "refId", "ty", "parent", "sw", "sh", "sc", "ks", "tt", "masksProperties", "shapes", "t", "ef", "sr", "st", "w", "h", "ip", "op", "tm", "cl", "hd");
    private static final Options TEXT_NAMES = Options.m6of("d", "a");

    private LayerParser() {
    }

    public static Layer parse(LottieComposition lottieComposition) {
        LottieComposition lottieComposition2 = lottieComposition;
        Rect bounds = lottieComposition.getBounds();
        List emptyList = Collections.emptyList();
        LayerType layerType = LayerType.PRE_COMP;
        List emptyList2 = Collections.emptyList();
        AnimatableTransform animatableTransform = r4;
        AnimatableTransform animatableTransform2 = new AnimatableTransform();
        Layer layer = new Layer(emptyList, lottieComposition2, "__container", -1, layerType, -1, null, emptyList2, animatableTransform, 0, 0, 0, 0.0f, 0.0f, bounds.width(), bounds.height(), null, null, Collections.emptyList(), MatteType.NONE, null, false);
        return layer;
    }

    public static Layer parse(JsonReader jsonReader, LottieComposition lottieComposition) throws IOException {
        float f;
        ArrayList arrayList;
        float f2;
        JsonReader jsonReader2 = jsonReader;
        LottieComposition lottieComposition2 = lottieComposition;
        MatteType matteType = MatteType.NONE;
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        jsonReader.beginObject();
        Float valueOf = Float.valueOf(1.0f);
        Float valueOf2 = Float.valueOf(0.0f);
        MatteType matteType2 = matteType;
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        boolean z = false;
        LayerType layerType = null;
        String str = null;
        AnimatableTransform animatableTransform = null;
        AnimatableTextFrame animatableTextFrame = null;
        AnimatableTextProperties animatableTextProperties = null;
        AnimatableFloatValue animatableFloatValue = null;
        float f3 = 0.0f;
        float f4 = 0.0f;
        long j = -1;
        float f5 = 1.0f;
        float f6 = 0.0f;
        long j2 = 0;
        Object obj = null;
        String str2 = "UNSET";
        while (jsonReader.hasNext()) {
            switch (jsonReader2.selectName(NAMES)) {
                case 0:
                    str2 = jsonReader.nextString();
                    break;
                case 1:
                    j2 = (long) jsonReader.nextInt();
                    break;
                case 2:
                    str = jsonReader.nextString();
                    break;
                case 3:
                    int nextInt = jsonReader.nextInt();
                    if (nextInt >= LayerType.UNKNOWN.ordinal()) {
                        layerType = LayerType.UNKNOWN;
                        break;
                    } else {
                        layerType = LayerType.values()[nextInt];
                        break;
                    }
                case 4:
                    j = (long) jsonReader.nextInt();
                    break;
                case 5:
                    i = (int) (((float) jsonReader.nextInt()) * Utils.dpScale());
                    break;
                case 6:
                    i2 = (int) (((float) jsonReader.nextInt()) * Utils.dpScale());
                    break;
                case 7:
                    i3 = Color.parseColor(jsonReader.nextString());
                    break;
                case 8:
                    animatableTransform = AnimatableTransformParser.parse(jsonReader, lottieComposition);
                    break;
                case 9:
                    matteType2 = MatteType.values()[jsonReader.nextInt()];
                    lottieComposition2.incrementMatteOrMaskCount(1);
                    break;
                case 10:
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        arrayList2.add(MaskParser.parse(jsonReader, lottieComposition));
                    }
                    lottieComposition2.incrementMatteOrMaskCount(arrayList2.size());
                    jsonReader.endArray();
                    break;
                case 11:
                    jsonReader.beginArray();
                    while (jsonReader.hasNext()) {
                        ContentModel parse = ContentModelParser.parse(jsonReader, lottieComposition);
                        if (parse != null) {
                            arrayList3.add(parse);
                        }
                    }
                    jsonReader.endArray();
                    break;
                case 12:
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        int selectName = jsonReader2.selectName(TEXT_NAMES);
                        if (selectName == 0) {
                            animatableTextFrame = AnimatableValueParser.parseDocumentData(jsonReader, lottieComposition);
                        } else if (selectName != 1) {
                            jsonReader.skipName();
                            jsonReader.skipValue();
                        } else {
                            jsonReader.beginArray();
                            if (jsonReader.hasNext()) {
                                animatableTextProperties = AnimatableTextPropertiesParser.parse(jsonReader, lottieComposition);
                            }
                            while (jsonReader.hasNext()) {
                                jsonReader.skipValue();
                            }
                            jsonReader.endArray();
                        }
                    }
                    jsonReader.endObject();
                    break;
                case 13:
                    jsonReader.beginArray();
                    ArrayList arrayList4 = new ArrayList();
                    while (jsonReader.hasNext()) {
                        jsonReader.beginObject();
                        while (jsonReader.hasNext()) {
                            if (jsonReader2.selectName(EFFECTS_NAMES) != 0) {
                                jsonReader.skipName();
                                jsonReader.skipValue();
                            } else {
                                arrayList4.add(jsonReader.nextString());
                            }
                        }
                        jsonReader.endObject();
                    }
                    jsonReader.endArray();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Lottie doesn't support layer effects. If you are using them for  fills, strokes, trim paths etc. then try adding them directly as contents  in your shape. Found: ");
                    sb.append(arrayList4);
                    lottieComposition2.addWarning(sb.toString());
                    break;
                case 14:
                    f5 = (float) jsonReader.nextDouble();
                    break;
                case 15:
                    f4 = (float) jsonReader.nextDouble();
                    break;
                case 16:
                    i4 = (int) (((float) jsonReader.nextInt()) * Utils.dpScale());
                    break;
                case 17:
                    i5 = (int) (((float) jsonReader.nextInt()) * Utils.dpScale());
                    break;
                case 18:
                    f3 = (float) jsonReader.nextDouble();
                    break;
                case 19:
                    f6 = (float) jsonReader.nextDouble();
                    break;
                case 20:
                    animatableFloatValue = AnimatableValueParser.parseFloat(jsonReader2, lottieComposition2, false);
                    break;
                case 21:
                    obj = jsonReader.nextString();
                    break;
                case 22:
                    z = jsonReader.nextBoolean();
                    break;
                default:
                    jsonReader.skipName();
                    jsonReader.skipValue();
                    break;
            }
        }
        jsonReader.endObject();
        float f7 = f3 / f5;
        float f8 = f6 / f5;
        ArrayList arrayList5 = new ArrayList();
        if (f7 > 0.0f) {
            Keyframe keyframe = r0;
            f = f5;
            arrayList = arrayList5;
            Keyframe keyframe2 = new Keyframe(lottieComposition, valueOf2, valueOf2, null, 0.0f, Float.valueOf(f7));
            arrayList.add(keyframe);
            f2 = 0.0f;
        } else {
            f = f5;
            arrayList = arrayList5;
            f2 = 0.0f;
        }
        if (f8 <= f2) {
            f8 = lottieComposition.getEndFrame();
        }
        LottieComposition lottieComposition3 = lottieComposition;
        Keyframe keyframe3 = new Keyframe(lottieComposition3, valueOf, valueOf, null, f7, Float.valueOf(f8));
        arrayList.add(keyframe3);
        Keyframe keyframe4 = new Keyframe(lottieComposition3, valueOf2, valueOf2, null, f8, Float.valueOf(Float.MAX_VALUE));
        arrayList.add(keyframe4);
        if (str2.endsWith(".ai") || "ai".equals(obj)) {
            lottieComposition2.addWarning("Convert your Illustrator layers to shape layers.");
        }
        Layer layer = new Layer(arrayList3, lottieComposition, str2, j2, layerType, j, str, arrayList2, animatableTransform, i, i2, i3, f, f4, i4, i5, animatableTextFrame, animatableTextProperties, arrayList, matteType2, animatableFloatValue, z);
        return layer;
    }
}

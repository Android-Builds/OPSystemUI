package com.airbnb.lottie.parser;

import com.airbnb.lottie.model.DocumentData;
import com.airbnb.lottie.model.DocumentData.Justification;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.parser.moshi.JsonReader.Options;
import java.io.IOException;

public class DocumentDataParser implements ValueParser<DocumentData> {
    public static final DocumentDataParser INSTANCE = new DocumentDataParser();
    private static final Options NAMES = Options.m6of("t", "f", "s", "j", "tr", "lh", "ls", "fc", "sc", "sw", "of");

    private DocumentDataParser() {
    }

    public DocumentData parse(JsonReader jsonReader, float f) throws IOException {
        Justification justification = Justification.CENTER;
        jsonReader.beginObject();
        Justification justification2 = justification;
        String str = null;
        String str2 = null;
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        double d = 0.0d;
        double d2 = 0.0d;
        double d3 = 0.0d;
        double d4 = 0.0d;
        boolean z = true;
        while (jsonReader.hasNext()) {
            switch (jsonReader.selectName(NAMES)) {
                case 0:
                    str = jsonReader.nextString();
                    break;
                case 1:
                    str2 = jsonReader.nextString();
                    break;
                case 2:
                    d = jsonReader.nextDouble();
                    break;
                case 3:
                    int nextInt = jsonReader.nextInt();
                    if (nextInt <= Justification.CENTER.ordinal() && nextInt >= 0) {
                        justification2 = Justification.values()[nextInt];
                        break;
                    } else {
                        justification2 = Justification.CENTER;
                        break;
                    }
                case 4:
                    i = jsonReader.nextInt();
                    break;
                case 5:
                    d2 = jsonReader.nextDouble();
                    break;
                case 6:
                    d3 = jsonReader.nextDouble();
                    break;
                case 7:
                    i2 = JsonUtils.jsonToColor(jsonReader);
                    break;
                case 8:
                    i3 = JsonUtils.jsonToColor(jsonReader);
                    break;
                case 9:
                    d4 = jsonReader.nextDouble();
                    break;
                case 10:
                    z = jsonReader.nextBoolean();
                    break;
                default:
                    jsonReader.skipName();
                    jsonReader.skipValue();
                    break;
            }
        }
        JsonReader jsonReader2 = jsonReader;
        jsonReader.endObject();
        DocumentData documentData = new DocumentData(str, str2, d, justification2, i, d2, d3, i2, i3, d4, z);
        return documentData;
    }
}

package com.airbnb.lottie;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.airbnb.lottie.model.LottieCompositionCache;
import com.airbnb.lottie.network.NetworkFetcher;
import com.airbnb.lottie.parser.LottieCompositionMoshiParser;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.utils.Utils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import okio.Okio;
import org.json.JSONObject;

public class LottieCompositionFactory {
    /* access modifiers changed from: private */
    public static final Map<String, LottieTask<LottieComposition>> taskCache = new HashMap();

    private LottieCompositionFactory() {
    }

    public static void setMaxCacheSize(int i) {
        LottieCompositionCache.getInstance().resize(i);
    }

    public static LottieTask<LottieComposition> fromUrl(final Context context, final String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("url_");
        sb.append(str);
        return cache(sb.toString(), new Callable<LottieResult<LottieComposition>>() {
            public LottieResult<LottieComposition> call() {
                return NetworkFetcher.fetchSync(context, str);
            }
        });
    }

    public static LottieResult<LottieComposition> fromUrlSync(Context context, String str) {
        return NetworkFetcher.fetchSync(context, str);
    }

    public static LottieTask<LottieComposition> fromAsset(Context context, final String str) {
        final Context applicationContext = context.getApplicationContext();
        return cache(str, new Callable<LottieResult<LottieComposition>>() {
            public LottieResult<LottieComposition> call() {
                return LottieCompositionFactory.fromAssetSync(applicationContext, str);
            }
        });
    }

    public static LottieResult<LottieComposition> fromAssetSync(Context context, String str) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("asset_");
            sb.append(str);
            String sb2 = sb.toString();
            if (str.endsWith(".zip")) {
                return fromZipStreamSync(new ZipInputStream(context.getAssets().open(str)), sb2);
            }
            return fromJsonInputStreamSync(context.getAssets().open(str), sb2);
        } catch (IOException e) {
            return new LottieResult<>((Throwable) e);
        }
    }

    public static LottieTask<LottieComposition> fromRawRes(Context context, final int i) {
        final WeakReference weakReference = new WeakReference(context);
        final Context applicationContext = context.getApplicationContext();
        return cache(rawResCacheKey(context, i), new Callable<LottieResult<LottieComposition>>() {
            public LottieResult<LottieComposition> call() {
                Context context = (Context) weakReference.get();
                if (context == null) {
                    context = applicationContext;
                }
                return LottieCompositionFactory.fromRawResSync(context, i);
            }
        });
    }

    public static LottieResult<LottieComposition> fromRawResSync(Context context, int i) {
        try {
            return fromJsonInputStreamSync(context.getResources().openRawResource(i), rawResCacheKey(context, i));
        } catch (NotFoundException e) {
            return new LottieResult<>((Throwable) e);
        }
    }

    private static String rawResCacheKey(Context context, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("rawRes");
        sb.append(isNightMode(context) ? "_night_" : "_day_");
        sb.append(i);
        return sb.toString();
    }

    private static boolean isNightMode(Context context) {
        return (context.getResources().getConfiguration().uiMode & 48) == 32;
    }

    public static LottieTask<LottieComposition> fromJsonInputStream(final InputStream inputStream, final String str) {
        return cache(str, new Callable<LottieResult<LottieComposition>>() {
            public LottieResult<LottieComposition> call() {
                return LottieCompositionFactory.fromJsonInputStreamSync(inputStream, str);
            }
        });
    }

    public static LottieResult<LottieComposition> fromJsonInputStreamSync(InputStream inputStream, String str) {
        return fromJsonInputStreamSync(inputStream, str, true);
    }

    private static LottieResult<LottieComposition> fromJsonInputStreamSync(InputStream inputStream, String str, boolean z) {
        try {
            return fromJsonReaderSync(JsonReader.m5of(Okio.buffer(Okio.source(inputStream))), str);
        } finally {
            if (z) {
                Utils.closeQuietly(inputStream);
            }
        }
    }

    @Deprecated
    public static LottieTask<LottieComposition> fromJson(final JSONObject jSONObject, final String str) {
        return cache(str, new Callable<LottieResult<LottieComposition>>() {
            public LottieResult<LottieComposition> call() {
                return LottieCompositionFactory.fromJsonSync(jSONObject, str);
            }
        });
    }

    @Deprecated
    public static LottieResult<LottieComposition> fromJsonSync(JSONObject jSONObject, String str) {
        return fromJsonStringSync(jSONObject.toString(), str);
    }

    public static LottieTask<LottieComposition> fromJsonString(final String str, final String str2) {
        return cache(str2, new Callable<LottieResult<LottieComposition>>() {
            public LottieResult<LottieComposition> call() {
                return LottieCompositionFactory.fromJsonStringSync(str, str2);
            }
        });
    }

    public static LottieResult<LottieComposition> fromJsonStringSync(String str, String str2) {
        return fromJsonReaderSync(JsonReader.m5of(Okio.buffer(Okio.source(new ByteArrayInputStream(str.getBytes())))), str2);
    }

    public static LottieTask<LottieComposition> fromJsonReader(final JsonReader jsonReader, final String str) {
        return cache(str, new Callable<LottieResult<LottieComposition>>() {
            public LottieResult<LottieComposition> call() {
                return LottieCompositionFactory.fromJsonReaderSync(JsonReader.this, str);
            }
        });
    }

    public static LottieResult<LottieComposition> fromJsonReaderSync(JsonReader jsonReader, String str) {
        return fromJsonReaderSyncInternal(jsonReader, str, true);
    }

    private static LottieResult<LottieComposition> fromJsonReaderSyncInternal(JsonReader jsonReader, String str, boolean z) {
        try {
            LottieComposition parse = LottieCompositionMoshiParser.parse(jsonReader);
            if (str != null) {
                LottieCompositionCache.getInstance().put(str, parse);
            }
            LottieResult<LottieComposition> lottieResult = new LottieResult<>(parse);
            if (z) {
                Utils.closeQuietly(jsonReader);
            }
            return lottieResult;
        } catch (Exception e) {
            LottieResult<LottieComposition> lottieResult2 = new LottieResult<>((Throwable) e);
            if (z) {
                Utils.closeQuietly(jsonReader);
            }
            return lottieResult2;
        } catch (Throwable th) {
            if (z) {
                Utils.closeQuietly(jsonReader);
            }
            throw th;
        }
    }

    public static LottieTask<LottieComposition> fromZipStream(final ZipInputStream zipInputStream, final String str) {
        return cache(str, new Callable<LottieResult<LottieComposition>>() {
            public LottieResult<LottieComposition> call() {
                return LottieCompositionFactory.fromZipStreamSync(zipInputStream, str);
            }
        });
    }

    public static LottieResult<LottieComposition> fromZipStreamSync(ZipInputStream zipInputStream, String str) {
        try {
            return fromZipStreamSyncInternal(zipInputStream, str);
        } finally {
            Utils.closeQuietly(zipInputStream);
        }
    }

    private static LottieResult<LottieComposition> fromZipStreamSyncInternal(ZipInputStream zipInputStream, String str) {
        HashMap hashMap = new HashMap();
        try {
            ZipEntry nextEntry = zipInputStream.getNextEntry();
            LottieComposition lottieComposition = null;
            while (nextEntry != null) {
                String name = nextEntry.getName();
                if (name.contains("__MACOSX")) {
                    zipInputStream.closeEntry();
                } else if (nextEntry.getName().contains(".json")) {
                    lottieComposition = (LottieComposition) fromJsonReaderSyncInternal(JsonReader.m5of(Okio.buffer(Okio.source(zipInputStream))), null, false).getValue();
                } else {
                    if (!name.contains(".png")) {
                        if (!name.contains(".webp")) {
                            zipInputStream.closeEntry();
                        }
                    }
                    String[] split = name.split("/");
                    hashMap.put(split[split.length - 1], BitmapFactory.decodeStream(zipInputStream));
                }
                nextEntry = zipInputStream.getNextEntry();
            }
            if (lottieComposition == null) {
                return new LottieResult<>((Throwable) new IllegalArgumentException("Unable to parse composition"));
            }
            for (Entry entry : hashMap.entrySet()) {
                LottieImageAsset findImageAssetForFileName = findImageAssetForFileName(lottieComposition, (String) entry.getKey());
                if (findImageAssetForFileName != null) {
                    findImageAssetForFileName.setBitmap(Utils.resizeBitmapIfNeeded((Bitmap) entry.getValue(), findImageAssetForFileName.getWidth(), findImageAssetForFileName.getHeight()));
                }
            }
            for (Entry entry2 : lottieComposition.getImages().entrySet()) {
                if (((LottieImageAsset) entry2.getValue()).getBitmap() == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("There is no image for ");
                    sb.append(((LottieImageAsset) entry2.getValue()).getFileName());
                    return new LottieResult<>((Throwable) new IllegalStateException(sb.toString()));
                }
            }
            if (str != null) {
                LottieCompositionCache.getInstance().put(str, lottieComposition);
            }
            return new LottieResult<>(lottieComposition);
        } catch (IOException e) {
            return new LottieResult<>((Throwable) e);
        }
    }

    private static LottieImageAsset findImageAssetForFileName(LottieComposition lottieComposition, String str) {
        for (LottieImageAsset lottieImageAsset : lottieComposition.getImages().values()) {
            if (lottieImageAsset.getFileName().equals(str)) {
                return lottieImageAsset;
            }
        }
        return null;
    }

    private static LottieTask<LottieComposition> cache(final String str, Callable<LottieResult<LottieComposition>> callable) {
        final LottieComposition lottieComposition = str == null ? null : LottieCompositionCache.getInstance().get(str);
        if (lottieComposition != null) {
            return new LottieTask<>(new Callable<LottieResult<LottieComposition>>() {
                public LottieResult<LottieComposition> call() {
                    return new LottieResult<>(LottieComposition.this);
                }
            });
        }
        if (str != null && taskCache.containsKey(str)) {
            return (LottieTask) taskCache.get(str);
        }
        LottieTask<LottieComposition> lottieTask = new LottieTask<>(callable);
        lottieTask.addListener(new LottieListener<LottieComposition>() {
            public void onResult(LottieComposition lottieComposition) {
                LottieCompositionFactory.taskCache.remove(str);
            }
        });
        lottieTask.addFailureListener(new LottieListener<Throwable>() {
            public void onResult(Throwable th) {
                LottieCompositionFactory.taskCache.remove(str);
            }
        });
        taskCache.put(str, lottieTask);
        return lottieTask;
    }
}

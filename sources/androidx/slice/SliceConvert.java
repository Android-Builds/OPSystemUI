package androidx.slice;

import android.app.slice.Slice;
import android.app.slice.SliceItem;
import android.app.slice.SliceSpec;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;
import androidx.collection.ArraySet;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice.Builder;
import java.util.Set;

public class SliceConvert {
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static android.app.slice.Slice unwrap(androidx.slice.Slice r8) {
        /*
            if (r8 == 0) goto L_0x0107
            android.net.Uri r0 = r8.getUri()
            if (r0 != 0) goto L_0x000a
            goto L_0x0107
        L_0x000a:
            android.app.slice.Slice$Builder r0 = new android.app.slice.Slice$Builder
            android.net.Uri r1 = r8.getUri()
            androidx.slice.SliceSpec r2 = r8.getSpec()
            android.app.slice.SliceSpec r2 = unwrap(r2)
            r0.<init>(r1, r2)
            java.util.List r1 = r8.getHints()
            r0.addHints(r1)
            androidx.slice.SliceItem[] r8 = r8.getItemArray()
            int r1 = r8.length
            r2 = 0
            r3 = r2
        L_0x0029:
            if (r3 >= r1) goto L_0x0102
            r4 = r8[r3]
            java.lang.String r5 = r4.getFormat()
            r6 = -1
            int r7 = r5.hashCode()
            switch(r7) {
                case -1422950858: goto L_0x0077;
                case 104431: goto L_0x006d;
                case 3327612: goto L_0x0063;
                case 3556653: goto L_0x0058;
                case 100313435: goto L_0x004e;
                case 100358090: goto L_0x0044;
                case 109526418: goto L_0x003a;
                default: goto L_0x0039;
            }
        L_0x0039:
            goto L_0x0081
        L_0x003a:
            java.lang.String r7 = "slice"
            boolean r5 = r5.equals(r7)
            if (r5 == 0) goto L_0x0081
            r5 = r2
            goto L_0x0082
        L_0x0044:
            java.lang.String r7 = "input"
            boolean r5 = r5.equals(r7)
            if (r5 == 0) goto L_0x0081
            r5 = 2
            goto L_0x0082
        L_0x004e:
            java.lang.String r7 = "image"
            boolean r5 = r5.equals(r7)
            if (r5 == 0) goto L_0x0081
            r5 = 1
            goto L_0x0082
        L_0x0058:
            java.lang.String r7 = "text"
            boolean r5 = r5.equals(r7)
            if (r5 == 0) goto L_0x0081
            r5 = 4
            goto L_0x0082
        L_0x0063:
            java.lang.String r7 = "long"
            boolean r5 = r5.equals(r7)
            if (r5 == 0) goto L_0x0081
            r5 = 6
            goto L_0x0082
        L_0x006d:
            java.lang.String r7 = "int"
            boolean r5 = r5.equals(r7)
            if (r5 == 0) goto L_0x0081
            r5 = 5
            goto L_0x0082
        L_0x0077:
            java.lang.String r7 = "action"
            boolean r5 = r5.equals(r7)
            if (r5 == 0) goto L_0x0081
            r5 = 3
            goto L_0x0082
        L_0x0081:
            r5 = r6
        L_0x0082:
            switch(r5) {
                case 0: goto L_0x00ef;
                case 1: goto L_0x00db;
                case 2: goto L_0x00cb;
                case 3: goto L_0x00b7;
                case 4: goto L_0x00a7;
                case 5: goto L_0x0097;
                case 6: goto L_0x0087;
                default: goto L_0x0085;
            }
        L_0x0085:
            goto L_0x00fe
        L_0x0087:
            long r5 = r4.getLong()
            java.lang.String r7 = r4.getSubType()
            java.util.List r4 = r4.getHints()
            r0.addLong(r5, r7, r4)
            goto L_0x00fe
        L_0x0097:
            int r5 = r4.getInt()
            java.lang.String r6 = r4.getSubType()
            java.util.List r4 = r4.getHints()
            r0.addInt(r5, r6, r4)
            goto L_0x00fe
        L_0x00a7:
            java.lang.CharSequence r5 = r4.getText()
            java.lang.String r6 = r4.getSubType()
            java.util.List r4 = r4.getHints()
            r0.addText(r5, r6, r4)
            goto L_0x00fe
        L_0x00b7:
            android.app.PendingIntent r5 = r4.getAction()
            androidx.slice.Slice r6 = r4.getSlice()
            android.app.slice.Slice r6 = unwrap(r6)
            java.lang.String r4 = r4.getSubType()
            r0.addAction(r5, r6, r4)
            goto L_0x00fe
        L_0x00cb:
            android.app.RemoteInput r5 = r4.getRemoteInput()
            java.lang.String r6 = r4.getSubType()
            java.util.List r4 = r4.getHints()
            r0.addRemoteInput(r5, r6, r4)
            goto L_0x00fe
        L_0x00db:
            androidx.core.graphics.drawable.IconCompat r5 = r4.getIcon()
            android.graphics.drawable.Icon r5 = r5.toIcon()
            java.lang.String r6 = r4.getSubType()
            java.util.List r4 = r4.getHints()
            r0.addIcon(r5, r6, r4)
            goto L_0x00fe
        L_0x00ef:
            androidx.slice.Slice r5 = r4.getSlice()
            android.app.slice.Slice r5 = unwrap(r5)
            java.lang.String r4 = r4.getSubType()
            r0.addSubSlice(r5, r4)
        L_0x00fe:
            int r3 = r3 + 1
            goto L_0x0029
        L_0x0102:
            android.app.slice.Slice r8 = r0.build()
            return r8
        L_0x0107:
            r8 = 0
            return r8
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.slice.SliceConvert.unwrap(androidx.slice.Slice):android.app.slice.Slice");
    }

    private static SliceSpec unwrap(SliceSpec sliceSpec) {
        if (sliceSpec == null) {
            return null;
        }
        return new SliceSpec(sliceSpec.getType(), sliceSpec.getRevision());
    }

    static Set<SliceSpec> unwrap(Set<SliceSpec> set) {
        ArraySet arraySet = new ArraySet();
        if (set != null) {
            for (SliceSpec unwrap : set) {
                arraySet.add(unwrap(unwrap));
            }
        }
        return arraySet;
    }

    public static Slice wrap(Slice slice, Context context) {
        String str = "The icon resource isn't available.";
        String str2 = "SliceConvert";
        if (slice == null || slice.getUri() == null) {
            return null;
        }
        Builder builder = new Builder(slice.getUri());
        builder.addHints(slice.getHints());
        builder.setSpec(wrap(slice.getSpec()));
        for (SliceItem sliceItem : slice.getItems()) {
            String format = sliceItem.getFormat();
            char c = 65535;
            switch (format.hashCode()) {
                case -1422950858:
                    if (format.equals("action")) {
                        c = 3;
                        break;
                    }
                    break;
                case 104431:
                    if (format.equals("int")) {
                        c = 5;
                        break;
                    }
                    break;
                case 3327612:
                    if (format.equals("long")) {
                        c = 6;
                        break;
                    }
                    break;
                case 3556653:
                    if (format.equals("text")) {
                        c = 4;
                        break;
                    }
                    break;
                case 100313435:
                    if (format.equals("image")) {
                        c = 1;
                        break;
                    }
                    break;
                case 100358090:
                    if (format.equals("input")) {
                        c = 2;
                        break;
                    }
                    break;
                case 109526418:
                    if (format.equals("slice")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    builder.addSubSlice(wrap(sliceItem.getSlice(), context), sliceItem.getSubType());
                    break;
                case 1:
                    try {
                        builder.addIcon(IconCompat.createFromIcon(context, sliceItem.getIcon()), sliceItem.getSubType(), sliceItem.getHints());
                        break;
                    } catch (IllegalArgumentException e) {
                        Log.w(str2, str, e);
                        break;
                    } catch (NotFoundException e2) {
                        Log.w(str2, str, e2);
                        break;
                    }
                case 2:
                    builder.addRemoteInput(sliceItem.getRemoteInput(), sliceItem.getSubType(), sliceItem.getHints());
                    break;
                case 3:
                    builder.addAction(sliceItem.getAction(), wrap(sliceItem.getSlice(), context), sliceItem.getSubType());
                    break;
                case 4:
                    builder.addText(sliceItem.getText(), sliceItem.getSubType(), sliceItem.getHints());
                    break;
                case 5:
                    builder.addInt(sliceItem.getInt(), sliceItem.getSubType(), sliceItem.getHints());
                    break;
                case 6:
                    builder.addLong(sliceItem.getLong(), sliceItem.getSubType(), sliceItem.getHints());
                    break;
            }
        }
        return builder.build();
    }

    private static SliceSpec wrap(SliceSpec sliceSpec) {
        if (sliceSpec == null) {
            return null;
        }
        return new SliceSpec(sliceSpec.getType(), sliceSpec.getRevision());
    }

    public static Set<SliceSpec> wrap(Set<SliceSpec> set) {
        ArraySet arraySet = new ArraySet();
        if (set != null) {
            for (SliceSpec wrap : set) {
                arraySet.add(wrap(wrap));
            }
        }
        return arraySet;
    }
}

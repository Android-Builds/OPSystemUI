package androidx.slice.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.Slice.Builder;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;

public class SliceContent {
    protected SliceItem mColorItem;
    protected SliceItem mContentDescr;
    protected SliceItem mLayoutDirItem;
    protected int mRowIndex;
    protected SliceItem mSliceItem;

    public int getHeight(SliceStyle sliceStyle, SliceViewPolicy sliceViewPolicy) {
        return 0;
    }

    public SliceContent(Slice slice) {
        if (slice != null) {
            init(new SliceItem((Object) slice, "slice", (String) null, slice.getHints()));
            this.mRowIndex = -1;
        }
    }

    public SliceContent(SliceItem sliceItem, int i) {
        if (sliceItem != null) {
            init(sliceItem);
            this.mRowIndex = i;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:3:0x0018, code lost:
        if ("action".equals(r5.getFormat()) != false) goto L_0x001a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void init(androidx.slice.SliceItem r5) {
        /*
            r4 = this;
            r4.mSliceItem = r5
            java.lang.String r0 = r5.getFormat()
            java.lang.String r1 = "slice"
            boolean r0 = r1.equals(r0)
            if (r0 != 0) goto L_0x001a
            java.lang.String r0 = r5.getFormat()
            java.lang.String r1 = "action"
            boolean r0 = r1.equals(r0)
            if (r0 == 0) goto L_0x0035
        L_0x001a:
            androidx.slice.Slice r0 = r5.getSlice()
            java.lang.String r1 = "int"
            r2 = 0
            java.lang.String r3 = "color"
            androidx.slice.SliceItem r0 = androidx.slice.core.SliceQuery.findTopLevelItem(r0, r1, r3, r2, r2)
            r4.mColorItem = r0
            androidx.slice.Slice r0 = r5.getSlice()
            java.lang.String r3 = "layout_direction"
            androidx.slice.SliceItem r0 = androidx.slice.core.SliceQuery.findTopLevelItem(r0, r1, r3, r2, r2)
            r4.mLayoutDirItem = r0
        L_0x0035:
            java.lang.String r0 = "text"
            java.lang.String r1 = "content_description"
            androidx.slice.SliceItem r5 = androidx.slice.core.SliceQuery.findSubtype(r5, r0, r1)
            r4.mContentDescr = r5
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: androidx.slice.widget.SliceContent.init(androidx.slice.SliceItem):void");
    }

    public SliceItem getSliceItem() {
        return this.mSliceItem;
    }

    public int getLayoutDir() {
        SliceItem sliceItem = this.mLayoutDirItem;
        if (sliceItem != null) {
            return SliceViewUtil.resolveLayoutDirection(sliceItem.getInt());
        }
        return -1;
    }

    public CharSequence getContentDescription() {
        SliceItem sliceItem = this.mContentDescr;
        if (sliceItem != null) {
            return sliceItem.getText();
        }
        return null;
    }

    public int getRowIndex() {
        return this.mRowIndex;
    }

    public boolean isValid() {
        return this.mSliceItem != null;
    }

    public SliceAction getShortcut(Context context) {
        SliceItem sliceItem;
        SliceItem sliceItem2;
        int i;
        SliceItem sliceItem3 = this.mSliceItem;
        if (sliceItem3 == null) {
            return null;
        }
        String str = "title";
        String str2 = "action";
        SliceItem find = SliceQuery.find(sliceItem3, str2, new String[]{str, "shortcut"}, (String[]) null);
        String str3 = "text";
        String str4 = "image";
        if (find != null) {
            sliceItem2 = SliceQuery.find(find, str4, str, (String) null);
            sliceItem = SliceQuery.find(find, str3, (String) null, (String) null);
        } else {
            sliceItem2 = null;
            sliceItem = null;
        }
        if (find == null) {
            find = SliceQuery.find(this.mSliceItem, str2, (String) null, (String) null);
        }
        SliceItem sliceItem4 = find;
        if (sliceItem2 == null) {
            sliceItem2 = SliceQuery.find(this.mSliceItem, str4, str, (String) null);
        }
        if (sliceItem == null) {
            sliceItem = SliceQuery.find(this.mSliceItem, str3, str, (String) null);
        }
        if (sliceItem2 == null) {
            sliceItem2 = SliceQuery.find(this.mSliceItem, str4, (String) null, (String) null);
        }
        if (sliceItem == null) {
            sliceItem = SliceQuery.find(this.mSliceItem, str3, (String) null, (String) null);
        }
        if (sliceItem2 != null) {
            int i2 = sliceItem2.hasHint("no_tint") ? sliceItem2.hasHint("large") ? 2 : 1 : 0;
            i = i2;
        } else {
            i = 3;
        }
        if (context != null) {
            return fallBackToAppData(context, sliceItem, sliceItem2, i, sliceItem4);
        }
        if (sliceItem2 == null || sliceItem4 == null || sliceItem == null) {
            return null;
        }
        return new SliceActionImpl(sliceItem4.getAction(), sliceItem2.getIcon(), i, sliceItem.getText());
    }

    private SliceAction fallBackToAppData(Context context, SliceItem sliceItem, SliceItem sliceItem2, int i, SliceItem sliceItem3) {
        SliceItem find = SliceQuery.find(this.mSliceItem, "slice", (String) null, (String) null);
        if (find == null) {
            return null;
        }
        Uri uri = find.getSlice().getUri();
        IconCompat icon = sliceItem2 != null ? sliceItem2.getIcon() : null;
        CharSequence text = sliceItem != null ? sliceItem.getText() : null;
        if (context != null) {
            PackageManager packageManager = context.getPackageManager();
            ProviderInfo resolveContentProvider = packageManager.resolveContentProvider(uri.getAuthority(), 0);
            ApplicationInfo applicationInfo = resolveContentProvider != null ? resolveContentProvider.applicationInfo : null;
            if (applicationInfo != null) {
                if (icon == null) {
                    icon = SliceViewUtil.createIconFromDrawable(packageManager.getApplicationIcon(applicationInfo));
                    i = 2;
                }
                if (text == null) {
                    text = packageManager.getApplicationLabel(applicationInfo);
                }
                if (sliceItem3 == null) {
                    Intent launchIntentForPackage = packageManager.getLaunchIntentForPackage(applicationInfo.packageName);
                    if (launchIntentForPackage != null) {
                        sliceItem3 = new SliceItem(PendingIntent.getActivity(context, 0, launchIntentForPackage, 0), new Builder(uri).build(), "action", null, new String[0]);
                    }
                }
            }
        }
        SliceItem sliceItem4 = sliceItem3 == null ? new SliceItem(PendingIntent.getActivity(context, 0, new Intent(), 0), null, "action", null, null) : sliceItem3;
        if (text == null || icon == null || sliceItem4 == null) {
            return null;
        }
        return new SliceActionImpl(sliceItem4.getAction(), icon, i, text);
    }
}

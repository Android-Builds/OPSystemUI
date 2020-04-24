package android.support.p002v7.widget;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.DataSetObservable;
import java.util.HashMap;
import java.util.Map;

/* renamed from: android.support.v7.widget.ActivityChooserModel */
class ActivityChooserModel extends DataSetObservable {
    private static final Map<String, ActivityChooserModel> sDataModelRegistry = new HashMap();
    private static final Object sRegistryLock = new Object();

    /* renamed from: android.support.v7.widget.ActivityChooserModel$ActivityChooserModelClient */
    public interface ActivityChooserModelClient {
    }

    public Intent chooseActivity(int i) {
        throw null;
    }

    public ResolveInfo getActivity(int i) {
        throw null;
    }

    public int getActivityCount() {
        throw null;
    }

    public int getActivityIndex(ResolveInfo resolveInfo) {
        throw null;
    }

    public ResolveInfo getDefaultActivity() {
        throw null;
    }

    public int getHistorySize() {
        throw null;
    }

    public void setDefaultActivity(int i) {
        throw null;
    }
}

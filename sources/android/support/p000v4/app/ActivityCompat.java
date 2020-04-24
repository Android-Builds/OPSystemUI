package android.support.p000v4.app;

import android.app.Activity;
import android.content.Intent;
import android.support.p000v4.content.ContextCompat;

/* renamed from: android.support.v4.app.ActivityCompat */
public class ActivityCompat extends ContextCompat {
    private static PermissionCompatDelegate sDelegate;

    /* renamed from: android.support.v4.app.ActivityCompat$OnRequestPermissionsResultCallback */
    public interface OnRequestPermissionsResultCallback {
    }

    /* renamed from: android.support.v4.app.ActivityCompat$PermissionCompatDelegate */
    public interface PermissionCompatDelegate {
        boolean onActivityResult(Activity activity, int i, int i2, Intent intent);
    }

    /* renamed from: android.support.v4.app.ActivityCompat$RequestPermissionsRequestCodeValidator */
    public interface RequestPermissionsRequestCodeValidator {
    }

    public static PermissionCompatDelegate getPermissionCompatDelegate() {
        return sDelegate;
    }
}

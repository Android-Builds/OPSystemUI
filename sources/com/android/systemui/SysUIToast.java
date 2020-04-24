package com.android.systemui;

import android.content.Context;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

public class SysUIToast {
    public static Toast makeText(Context context, int i, int i2) {
        return makeText(context, (CharSequence) context.getString(i), i2);
    }

    public static Toast makeText(Context context, CharSequence charSequence, int i) {
        Toast makeText = Toast.makeText(context, charSequence, i);
        LayoutParams windowParams = makeText.getWindowParams();
        windowParams.privateFlags |= 16;
        makeText.getWindowParams().type = 2006;
        return makeText;
    }
}

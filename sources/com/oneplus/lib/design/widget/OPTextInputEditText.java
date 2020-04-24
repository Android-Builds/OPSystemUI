package com.oneplus.lib.design.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import com.oneplus.lib.widget.OPEditText;

public class OPTextInputEditText extends OPEditText {
    public OPTextInputEditText(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
        InputConnection onCreateInputConnection = super.onCreateInputConnection(editorInfo);
        if (onCreateInputConnection != null && editorInfo.hintText == null) {
            ViewParent parent = getParent();
            while (true) {
                if (!(parent instanceof View)) {
                    break;
                } else if (parent instanceof OPTextInputLayout) {
                    editorInfo.hintText = ((OPTextInputLayout) parent).getHint();
                    break;
                } else {
                    parent = parent.getParent();
                }
            }
        }
        return onCreateInputConnection;
    }
}

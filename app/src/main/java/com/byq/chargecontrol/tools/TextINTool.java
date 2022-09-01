package com.byq.chargecontrol.tools;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class TextINTool {
    /**
     * 设置文本框错误自动消失
     * @param isRecycle 是否允许在文本更变后恢复错误提示 （这会导致占位）
     * @param layout
     * @param editText
     */
    public static void setAutoDismissErrorTip(boolean isRecycle,TextInputLayout layout, TextInputEditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setErrorEnabled(false);
                if (isRecycle) {
                    layout.setErrorEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}

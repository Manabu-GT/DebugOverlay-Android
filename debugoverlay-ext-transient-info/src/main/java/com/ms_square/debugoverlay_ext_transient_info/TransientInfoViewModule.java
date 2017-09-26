package com.ms_square.debugoverlay_ext_transient_info;

import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ms_square.debugoverlay.modules.BaseViewModule;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

class TransientInfoViewModule extends BaseViewModule<String> {

    private TextView textView;

    TransientInfoViewModule(@LayoutRes int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(String data) {
        if (textView != null) {
            if (data.isEmpty())
            {
                textView.setVisibility(GONE);
            }
            else {
                textView.setVisibility(VISIBLE);
                textView.setText(data);
            }
        }
    }

    @NonNull
    @Override
    public View createView(@NonNull ViewGroup root, @ColorInt int textColor, float textSize, float textAlpha) {
        View view = LayoutInflater.from(root.getContext()).inflate(layoutResId, root, false);
        textView = view.findViewById(R.id.debugoverlay_overlay_text);
        textView.setTextColor(textColor);
        textView.setTextSize(textSize);
        textView.setAlpha(textAlpha);
        return view;
    }
}
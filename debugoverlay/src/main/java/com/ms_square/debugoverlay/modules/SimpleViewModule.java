package com.ms_square.debugoverlay.modules;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.ms_square.debugoverlay.R;

public class SimpleViewModule extends BaseViewModule<String> {

    private TextView textView;

    public SimpleViewModule(@LayoutRes int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(String data) {
        if (textView != null) {
            textView.setText(data);
        }
    }

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

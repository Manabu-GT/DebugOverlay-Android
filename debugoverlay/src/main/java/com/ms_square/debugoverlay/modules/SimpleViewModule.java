package com.ms_square.debugoverlay.modules;

import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        textView = (TextView) view.findViewById(R.id.debugoverlay_overlay_text);
        textView.setTextColor(textColor);
        textView.setTextSize(textSize);
        textView.setAlpha(textAlpha);
        return view;
    }
}

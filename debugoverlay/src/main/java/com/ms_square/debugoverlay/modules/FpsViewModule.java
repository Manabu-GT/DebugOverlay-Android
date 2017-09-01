package com.ms_square.debugoverlay.modules;

import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ms_square.debugoverlay.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FpsViewModule extends BaseViewModule<Double> {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("'fps:' 0.0",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private TextView fpsTxtView;

    public FpsViewModule() {
        super(R.layout.debugoverlay_fps);
    }

    public FpsViewModule(@LayoutRes int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(Double data) {
        if (fpsTxtView != null) {
            fpsTxtView.setText(DECIMAL_FORMAT.format(data));
        }
    }

    @Override
    public View createView(ViewGroup root, @ColorInt int textColor, float textSize, float textAlpha) {
        View view = LayoutInflater.from(root.getContext()).inflate(layoutResId, root, false);
        fpsTxtView = view.findViewById(R.id.debugoverlay_overlay_text);
        fpsTxtView.setTextColor(textColor);
        fpsTxtView.setTextSize(textSize);
        fpsTxtView.setAlpha(textAlpha);
        return view;
    }
}

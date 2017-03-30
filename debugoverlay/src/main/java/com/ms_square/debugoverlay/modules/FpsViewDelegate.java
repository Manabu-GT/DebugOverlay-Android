package com.ms_square.debugoverlay.modules;

import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ms_square.debugoverlay.OverlayViewDelegate;
import com.ms_square.debugoverlay.OverlayViewDelegateFactory;
import com.ms_square.debugoverlay.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FpsViewDelegate extends BaseOverlayViewDelegate<Double> {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("'fps:' 0.0",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private TextView fpsTxtView;

    private FpsViewDelegate(@LayoutRes int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(Double data) {
        fpsTxtView.setText(DECIMAL_FORMAT.format(data));
    }

    @Override
    public View createView(ViewGroup root, @ColorInt int textColor, float textSize, float textAlpha) {
        View view = LayoutInflater.from(root.getContext()).inflate(layoutResId, root, false);
        fpsTxtView = (TextView) view.findViewById(R.id.overlay_module_text);
        fpsTxtView.setTextColor(textColor);
        fpsTxtView.setTextSize(textSize);
        fpsTxtView.setAlpha(textAlpha);
        return view;
    }

    public static class Factory implements OverlayViewDelegateFactory<Double> {

        @LayoutRes
        private final int layoutResId;

        public Factory() {
            this(R.layout.fps);
        }

        public Factory(@LayoutRes int layoutResId) {
            this.layoutResId = layoutResId;
        }

        @Override
        public OverlayViewDelegate<Double> create() {
            return new FpsViewDelegate(layoutResId);
        }
    }
}

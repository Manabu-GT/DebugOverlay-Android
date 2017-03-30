package com.ms_square.debugoverlay.modules;

import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ms_square.debugoverlay.DebugOverlay;
import com.ms_square.debugoverlay.OverlayViewDelegate;
import com.ms_square.debugoverlay.OverlayViewDelegateFactory;
import com.ms_square.debugoverlay.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CpuUsageViewDelegate extends BaseOverlayViewDelegate<CpuUsageModule.CpuUsage> {

    private static final String TAG = CpuUsageViewDelegate.class.getSimpleName();

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private TextView cpuUsageTextView;

    private CpuUsageViewDelegate(@LayoutRes int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(CpuUsageModule.CpuUsage data) {
        String totalCpuUsage = DECIMAL_FORMAT.format(data.getTotal());
        String myPidCpuUsage = DECIMAL_FORMAT.format(data.getMyPid());

        if (DebugOverlay.isDebugLoggingEnabled()) {
            Log.d(TAG, "Total CPU Usage(%): " + totalCpuUsage);
            Log.d(TAG, "App CPU Usage(%): " + myPidCpuUsage);
        }

        StringBuilder builder = new StringBuilder("cpu: ");
        cpuUsageTextView.setText(builder.append(totalCpuUsage).append(" ").append(myPidCpuUsage).toString());
    }

    @Override
    public View createView(ViewGroup root, @ColorInt int textColor, float textSize, float textAlpha) {
        View view = LayoutInflater.from(root.getContext()).inflate(layoutResId, root, false);
        cpuUsageTextView = (TextView) view.findViewById(R.id.overlay_module_text);
        cpuUsageTextView.setTextColor(textColor);
        cpuUsageTextView.setTextSize(textSize);
        cpuUsageTextView.setAlpha(textAlpha);
        return view;
    }

    public static class Factory implements OverlayViewDelegateFactory<CpuUsageModule.CpuUsage> {

        @LayoutRes
        private final int layoutResId;

        public Factory() {
            this(R.layout.cpu_usage);
        }

        public Factory(@LayoutRes int layoutResId) {
            this.layoutResId = layoutResId;
        }

        @Override
        public OverlayViewDelegate<CpuUsageModule.CpuUsage> create() {
            return new CpuUsageViewDelegate(layoutResId);
        }
    }
}

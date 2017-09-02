package com.ms_square.debugoverlay.modules;

import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ms_square.debugoverlay.DebugOverlay;
import com.ms_square.debugoverlay.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CpuUsageViewModule extends BaseViewModule<CpuUsage> {

    private static final String TAG = "CpuUsageViewModule";

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private TextView cpuUsageTextView;

    public CpuUsageViewModule() {
        super(R.layout.debugoverlay_cpu_usage);
    }

    public CpuUsageViewModule(@LayoutRes int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(CpuUsage data) {
        if (data != null) {
            String totalCpuUsage = DECIMAL_FORMAT.format(data.getTotal());
            String myPidCpuUsage = DECIMAL_FORMAT.format(data.getMyPid());

            if (DebugOverlay.isDebugLoggingEnabled()) {
                Log.d(TAG, "Total CPU Usage(%): " + totalCpuUsage);
                Log.d(TAG, "App CPU Usage(%): " + myPidCpuUsage);
            }

            if (cpuUsageTextView != null) {
                StringBuilder builder = new StringBuilder("cpu(%): ");
                cpuUsageTextView.setText(builder.append(totalCpuUsage).append(" ").append(myPidCpuUsage).toString());
            }
        }
    }

    @Override
    public View createView(ViewGroup root, @ColorInt int textColor, float textSize, float textAlpha) {
        View view = LayoutInflater.from(root.getContext()).inflate(layoutResId, root, false);
        cpuUsageTextView = view.findViewById(R.id.debugoverlay_overlay_text);
        cpuUsageTextView.setTextColor(textColor);
        cpuUsageTextView.setTextSize(textSize);
        cpuUsageTextView.setAlpha(textAlpha);
        return view;
    }
}

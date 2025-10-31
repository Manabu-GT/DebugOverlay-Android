package com.ms.square.debugoverlay.modules;

import android.app.ActivityManager;
import android.os.Debug;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;

import com.ms.square.debugoverlay.DebugOverlay;
import com.ms.square.debugoverlay.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MemInfoViewModule extends BaseViewModule<MemInfo> {

    private static final String TAG = "MemInfoViewModule";

    private static final String HEADER = "memory(mb):\n";

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private TextView memInfoTxtView;

    public MemInfoViewModule() {
        super(R.layout.debugoverlay_mem_usage);
    }

    public MemInfoViewModule(@LayoutRes int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(MemInfo data) {
        ActivityManager.MemoryInfo systemMemInfo = data.getSystemMemInfo();
        Debug.MemoryInfo procMemInfo = data.getProcessMemInfo();

        if (DebugOverlay.isDebugLoggingEnabled()) {
            Log.d(TAG, "MemTotal(MB):" + DECIMAL_FORMAT.format(systemMemInfo.totalMem / 1048576f));
            Log.d(TAG, "MemAvail(MB):" + DECIMAL_FORMAT.format(systemMemInfo.availMem / 1048576f));
            Log.d(TAG, "MemThreshold(MB):" + DECIMAL_FORMAT.format(systemMemInfo.threshold / 1048576f));
            Log.d(TAG, "TotalPss(MB):" + DECIMAL_FORMAT.format(procMemInfo.getTotalPss() / 1024f));
            Log.d(TAG, "TotalPrivateDirty(MB):" + DECIMAL_FORMAT.format(procMemInfo.getTotalPrivateDirty() / 1024f));
        }

        if (memInfoTxtView != null) {
            StringBuilder builder = new StringBuilder(HEADER);
            builder.append(DECIMAL_FORMAT.format(systemMemInfo.availMem / 1048576f)).append(" ")
                    .append(DECIMAL_FORMAT.format(procMemInfo.getTotalPss() / 1024f)).append(" ")
                    .append(DECIMAL_FORMAT.format(procMemInfo.getTotalPrivateDirty() / 1024f));

            SpannableStringBuilder spannableBuilder = new SpannableStringBuilder(builder.toString());
            if (systemMemInfo.lowMemory) {
                spannableBuilder.setSpan(
                        new TextAppearanceSpan(memInfoTxtView.getContext(), R.style.debugoverlay_LowMemoryTextAppearance),
                        HEADER.length(),
                        spannableBuilder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            memInfoTxtView.setText(spannableBuilder);
        }
    }

    @Override
    public View createView(ViewGroup root, @ColorInt int textColor, float textSize, float textAlpha) {
        View view = LayoutInflater.from(root.getContext()).inflate(layoutResId, root, false);
        memInfoTxtView = view.findViewById(R.id.debugoverlay_overlay_text);
        memInfoTxtView.setTextColor(textColor);
        memInfoTxtView.setTextSize(textSize);
        memInfoTxtView.setAlpha(textAlpha);
        return view;
    }
}

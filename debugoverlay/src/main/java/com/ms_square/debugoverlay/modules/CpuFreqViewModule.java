package com.ms_square.debugoverlay.modules;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.ms_square.debugoverlay.R;

import java.util.List;

public class CpuFreqViewModule extends BaseViewModule<List<CpuFreq>> {

    private TextView cpuFreqTextView;

    public CpuFreqViewModule() {
        super(R.layout.debugoverlay_cpu_freq);
    }

    public CpuFreqViewModule(@LayoutRes int layoutResId) {
        super(layoutResId);
    }

    @Override
    public void onDataAvailable(List<CpuFreq> cpuFreqList) {
        if (cpuFreqList != null && cpuFreqTextView != null) {
            StringBuilder builder = new StringBuilder();
            for (CpuFreq cpuFreq : cpuFreqList) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(cpuFreq.toString());
            }
            cpuFreqTextView.setText(builder.toString());
        }
    }

    @NonNull
    @Override
    public View createView(@NonNull ViewGroup root, @ColorInt int textColor, float textSize, float textAlpha) {
        View view = LayoutInflater.from(root.getContext()).inflate(layoutResId, root, false);
        cpuFreqTextView = view.findViewById(R.id.debugoverlay_overlay_text);
        cpuFreqTextView.setTextColor(textColor);
        cpuFreqTextView.setTextSize(textSize);
        cpuFreqTextView.setAlpha(textAlpha);
        return view;
    }
}

package com.ms_square.debugoverlay.modules;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.ViewModule;

import java.util.List;

/**
 * @deprecated will not work on Android O and above.
 * @see <a href="https://issuetracker.google.com/issues/37140047">this issue</a> for its background.
 */
@Deprecated
public class CpuFreqModule extends OverlayModule<List<CpuFreq>> {

    public static final int DEFAULT_INTERVAL = 2000; // 2000ms

    public CpuFreqModule() {
        super(new CpuFreqDataModule(DEFAULT_INTERVAL), new CpuFreqViewModule());
    }

    public CpuFreqModule(int interval) {
        super(new CpuFreqDataModule(interval), new CpuFreqViewModule());
    }

    public CpuFreqModule(int interval, @LayoutRes int layoutResId) {
        super(new CpuFreqDataModule(interval), new CpuFreqViewModule(layoutResId));
    }

    public CpuFreqModule(@NonNull ViewModule<List<CpuFreq>> viewModule) {
        super(new CpuFreqDataModule(DEFAULT_INTERVAL), viewModule);
    }

    public CpuFreqModule(int interval, @NonNull ViewModule<List<CpuFreq>> viewModule) {
        super(new CpuFreqDataModule(interval), viewModule);
    }
}

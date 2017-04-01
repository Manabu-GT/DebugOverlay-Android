package com.ms_square.debugoverlay.modules;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.ViewModule;

public class CpuUsageModule extends OverlayModule<CpuUsageDataModule.CpuUsage> {

    public CpuUsageModule() {
        super(new CpuUsageDataModule(), new CpuUsageViewModule());
    }

    public CpuUsageModule(int interval) {
        super(new CpuUsageDataModule(interval), new CpuUsageViewModule());
    }

    public CpuUsageModule(int interval, @LayoutRes int layoutResId) {
        super(new CpuUsageDataModule(interval), new CpuUsageViewModule(layoutResId));
    }

    public CpuUsageModule(@NonNull ViewModule<CpuUsageDataModule.CpuUsage> viewModule) {
        super(new CpuUsageDataModule(), viewModule);
    }

    public CpuUsageModule(int interval, @NonNull ViewModule<CpuUsageDataModule.CpuUsage> viewModule) {
        super(new CpuUsageDataModule(interval), viewModule);
    }
}
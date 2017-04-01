package com.ms_square.debugoverlay.modules;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.ViewModule;

public class MemInfoModule extends OverlayModule<MemInfoDataModule.MemInfo> {

    public static final int DEFAULT_INTERVAL = 1500; // 1500ms

    public MemInfoModule(@NonNull Context context) {
        super(new MemInfoDataModule(context, DEFAULT_INTERVAL), new MemInfoViewModule());
    }

    public MemInfoModule(@NonNull Context context, int interval) {
        super(new MemInfoDataModule(context, interval), new MemInfoViewModule());
    }

    public MemInfoModule(@NonNull Context context, int interval, @LayoutRes int layoutResId) {
        super(new MemInfoDataModule(context, interval), new MemInfoViewModule(layoutResId));
    }

    public MemInfoModule(@NonNull Context context, @NonNull ViewModule<MemInfoDataModule.MemInfo> viewModule) {
        super(new MemInfoDataModule(context, DEFAULT_INTERVAL), viewModule);
    }

    public MemInfoModule(@NonNull Context context, int interval, @NonNull ViewModule<MemInfoDataModule.MemInfo> viewModule) {
        super(new MemInfoDataModule(context, interval), viewModule);
    }
}

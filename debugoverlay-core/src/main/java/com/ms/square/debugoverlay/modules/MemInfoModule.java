package com.ms.square.debugoverlay.modules;

import android.content.Context;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.ms.square.debugoverlay.OverlayModule;
import com.ms.square.debugoverlay.ViewModule;

public class MemInfoModule extends OverlayModule<MemInfo> {

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

    public MemInfoModule(@NonNull Context context, @NonNull ViewModule<MemInfo> viewModule) {
        super(new MemInfoDataModule(context, DEFAULT_INTERVAL), viewModule);
    }

    public MemInfoModule(@NonNull Context context, int interval, @NonNull ViewModule<MemInfo> viewModule) {
        super(new MemInfoDataModule(context, interval), viewModule);
    }
}

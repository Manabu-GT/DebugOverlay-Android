package com.ms_square.debugoverlay.modules;

import androidx.annotation.NonNull;
import androidx.annotation.Size;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.ViewModule;

public class LogcatModule extends OverlayModule<LogcatLine> {

    public static final int DEFAULT_MAX_LINES = 15;

    public LogcatModule() {
        super(new LogcatDataModule(), new LogcatViewModule(DEFAULT_MAX_LINES));
    }

    public LogcatModule(@Size(min=1,max=100) int maxLines) {
        super(new LogcatDataModule(), new LogcatViewModule(maxLines));
    }

    public LogcatModule(@Size(min=1,max=100) int maxLines, @NonNull LogcatLineFilter lineFilter) {
        super(new LogcatDataModule(), new LogcatViewModule(maxLines, lineFilter));
    }

    public LogcatModule(@Size(min=1,max=100) int maxLines, @NonNull LogcatLineColorScheme colorScheme) {
        super(new LogcatDataModule(), new LogcatViewModule(maxLines, colorScheme));
    }

    public LogcatModule(@Size(min=1,max=100) int maxLines, @NonNull LogcatLineFilter lineFilter,
                        @NonNull LogcatLineColorScheme colorScheme) {
        super(new LogcatDataModule(), new LogcatViewModule(maxLines, lineFilter, colorScheme));
    }

    public LogcatModule(@NonNull ViewModule<LogcatLine> viewModule) {
        super(new LogcatDataModule(), viewModule);
    }
}

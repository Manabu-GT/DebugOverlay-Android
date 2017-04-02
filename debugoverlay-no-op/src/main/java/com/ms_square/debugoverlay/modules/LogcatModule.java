package com.ms_square.debugoverlay.modules;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.ViewModule;

public class LogcatModule extends OverlayModule<LogcatLine> {

    public static final int DEFAULT_MAX_LINES = 15;

    public LogcatModule() {
        super(null, null);
    }

    public LogcatModule(int maxLines) {
        super(null, null);
    }

    public LogcatModule(int maxLines, LogcatLineFilter lineFilter) {
        super(null, null);
    }

    public LogcatModule(int maxLines, LogcatLineColorScheme colorScheme) {
        super(null, null);
    }

    public LogcatModule(int maxLines, LogcatLineFilter lineFilter, LogcatLineColorScheme colorScheme) {
        super(null, null);
    }

    public LogcatModule(ViewModule<LogcatLine> viewModule) {
        super(null, null);
    }
}

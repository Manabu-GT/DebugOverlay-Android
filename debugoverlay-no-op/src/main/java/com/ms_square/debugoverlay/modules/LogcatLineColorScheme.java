package com.ms_square.debugoverlay.modules;

public interface LogcatLineColorScheme {

    int getTextColor(LogcatLine.Priority priority, String tag);

    LogcatLineColorScheme DEFAULT_COLOR_SCHEME = null;
}

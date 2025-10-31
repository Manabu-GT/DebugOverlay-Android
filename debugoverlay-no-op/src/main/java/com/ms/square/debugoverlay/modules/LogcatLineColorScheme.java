package com.ms.square.debugoverlay.modules;

public interface LogcatLineColorScheme {

    int getTextColor(LogcatLine.Priority priority, String tag);

    LogcatLineColorScheme DEFAULT_COLOR_SCHEME = null;
}

package com.ms_square.debugoverlay.modules;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

public interface LogcatLineColorScheme {

    @ColorInt
    int getTextColor(LogcatLine.Priority priority, @NonNull String tag);

    /**
     * Ref...https://material.io/guidelines/style/color.html#color-color-palette
     */
    LogcatLineColorScheme DEFAULT_COLOR_SCHEME = new LogcatLineColorScheme() {

        private final int MATERIAL_BLUE = Color.parseColor("#1E88E5");

        private final int MATERIAL_GREEN = Color.parseColor("#43A047");

        private final int MATERIAL_YELLOW = Color.parseColor("#FDD835");

        private final int MATERIAL_RED = Color.parseColor("#E53935");

        @Override
        public int getTextColor(LogcatLine.Priority priority, String tag) {
            switch (priority) {
                case VERBOSE: {
                    return Color.BLACK;
                }
                case DEBUG: {
                    return MATERIAL_BLUE;
                }
                case INFO: {
                    return MATERIAL_GREEN;
                }
                case WARNING: {
                    return MATERIAL_YELLOW;
                }
                case ERROR:
                case FATAL:
                case ASSERT:
                case SILENT: {
                    return MATERIAL_RED;
                }
                default: {
                    return Color.BLACK;
                }
            }
        }
    };
}

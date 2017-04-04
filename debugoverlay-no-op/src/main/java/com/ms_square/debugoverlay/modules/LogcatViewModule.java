package com.ms_square.debugoverlay.modules;

import android.view.View;
import android.view.ViewGroup;

public class LogcatViewModule extends BaseViewModule<LogcatLine> {

    public LogcatViewModule(int maxLines) {
        super(0);
    }

    public LogcatViewModule(int maxLines, LogcatLineFilter lineFilter) {
        super(0);
    }

    public LogcatViewModule(int maxLines, LogcatLineColorScheme colorScheme) {
        super(0);
    }

    public LogcatViewModule(int maxLines, LogcatLineFilter lineFilter,
                   LogcatLineColorScheme colorScheme) {
        super(0);
    }

    public LogcatViewModule(int layoutResId, int maxLines,
                             LogcatLineFilter lineFilter, LogcatLineColorScheme colorScheme) {
        super(0);
    }

    @Override
    public void onDataAvailable(LogcatLine logcatLine) {

    }

    @Override
    public View createView(ViewGroup root, int textColor, float textSize, float textAlpha) {
        return null;
    }
}

package com.ms_square.debugoverlay.modules;

public interface LogcatLineFilter {

    boolean shouldFilterOut(LogcatLine.Priority priority, String tag);

    LogcatLineFilter DEFAULT_LINE_FILTER = null;

    class SimpleLogcatLineFilter implements LogcatLineFilter {

        public SimpleLogcatLineFilter(LogcatLine.Priority minPriority) {
        }

        @Override
        public boolean shouldFilterOut(LogcatLine.Priority priority, String tag) {
            return true;
        }
    }
}

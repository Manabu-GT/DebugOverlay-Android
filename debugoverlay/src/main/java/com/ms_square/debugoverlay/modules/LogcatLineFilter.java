package com.ms_square.debugoverlay.modules;

import androidx.annotation.NonNull;

public interface LogcatLineFilter {

    boolean shouldFilterOut(LogcatLine.Priority priority, @NonNull String tag);

    LogcatLineFilter DEFAULT_LINE_FILTER =
            new LogcatLineFilter.SimpleLogcatLineFilter(LogcatLine.Priority.VERBOSE);

    class SimpleLogcatLineFilter implements LogcatLineFilter {

        private final LogcatLine.Priority minPriority;

        public SimpleLogcatLineFilter(LogcatLine.Priority minPriority) {
            this.minPriority = minPriority;
        }

        @Override
        public boolean shouldFilterOut(LogcatLine.Priority priority, @NonNull String tag) {
            return priority.getIntValue() < minPriority.getIntValue();
        }
    }
}

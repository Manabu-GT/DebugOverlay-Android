package com.ms_square.debugoverlay.modules;

import android.support.annotation.NonNull;

public interface LogcatLineFilter {

    boolean shouldFilterOut(LogcatLine.Priority priority, @NonNull String tag);

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

package com.ms_square.debugoverlay.modules;

public class LogcatLine {

    public enum Priority {

        VERBOSE(),
        DEBUG(),
        INFO(),
        WARNING(),
        ERROR(),
        FATAL(),
        ASSERT(),
        SILENT();

        public String getValue() {
            return null;
        }

        public int getIntValue() {
            return 0;
        }

        public static Priority getPriorityFromValue(String value) {
            return VERBOSE;
        }
    }

    public LogcatLine(String rawLine) {

    }

    public String getRawLine() {
        return null;
    }

    public String getDate() {
        return null;
    }

    public String getTime() {
        return null;
    }

    public Priority getPriority() {
        return null;
    }

    public String getTag() {
        return null;
    }

    public int getPid() {
        return 0;
    }

    public int getTid() {
        return 0;
    }

    public String getMessage() {
        return null;
    }
}

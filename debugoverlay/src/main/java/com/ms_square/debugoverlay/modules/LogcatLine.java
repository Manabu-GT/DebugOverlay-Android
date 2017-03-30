package com.ms_square.debugoverlay.modules;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Ref...https://developer.android.com/studio/command-line/logcat.html#outputFormat
 */
public class LogcatLine {

    private static final String TAG = LogcatLine.class.getSimpleName();

    public enum Priority {

        /* The priority is one of the following enums, ordered from lowest to highest priority */
        VERBOSE("V", 1),
        DEBUG("D", 2),
        INFO("I", 3),
        WARNING("W", 4),
        ERROR("E", 5),
        FATAL("F", 6),
        ASSERT("A", 7),
        SILENT("S", 8);

        private static final Map<String, Priority> sValueToEnum = new HashMap();

        static {
            for (Priority priority : values()) {
                sValueToEnum.put(priority.getValue(), priority);
            }
        }

        private final String value;

        /**
         *  Used for priority high/low comparison. not something official on Android's logcat.
         *  Higher the value, higher the priority.
         */
        private final int intValue;

        Priority(String value, int intValue) {
            this.value = value;
            this.intValue = intValue;
        }

        public String getValue() {
            return value;
        }

        public int getIntValue() {
            return intValue;
        }

        public static Priority getPriorityFromValue(@NonNull String value) {
            if (sValueToEnum.containsKey(value)) {
                return sValueToEnum.get(value);
            }
            throw new AssertionError("no enum found for the given value -> " + value);
        }
    }

    private static final int DATE_INDEX = 0;
    private static final int TIME_INDEX = 1;
    private static final int PID_INDEX = 2;
    private static final int TID_INDEX = 3;
    private static final int PRIORITY_INDEX = 4;
    private static final int TAG_INDEX = 5;

    private final String rawLine;

    private final String date;

    private final String time;

    private final int pid;

    private final int tid;

    private final Priority priority;

    private final String tag;

    private final String message;

    public LogcatLine(String rawLine) {
        this.rawLine = rawLine;

        String[] outputs = rawLine.split(": ");
        String[] metaFields = outputs[0].split("[ ]+");

        if (metaFields.length >= TAG_INDEX + 1) {
            date = metaFields[DATE_INDEX];
            time = metaFields[TIME_INDEX];

            int pid = 0;
            try {
                pid = Integer.parseInt(metaFields[PID_INDEX]);
            } catch (NumberFormatException ne) {
                Log.w(TAG, "Value for PID is not an integer -> " + metaFields[PID_INDEX]);
            } finally {
                this.pid = pid;
            }

            int tid = 0;
            try {
                tid = Integer.parseInt(metaFields[TID_INDEX]);
            } catch (NumberFormatException ne) {
                Log.w(TAG, "Value for TID is not an integer -> " + metaFields[TID_INDEX]);
            } finally {
                this.tid = tid;
            }

            this.priority = Priority.getPriorityFromValue(metaFields[PRIORITY_INDEX]);

            StringBuilder tagBuilder = new StringBuilder();
            for (int i = TAG_INDEX; i < metaFields.length; i++) {
                if (i == metaFields.length - 1) {
                    tagBuilder.append(metaFields[i]);
                } else {
                    tagBuilder.append(metaFields[i]).append(" ");
                }
            }
            this.tag = tagBuilder.toString();

            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 1; i < outputs.length; i++) {
                if (i == outputs.length - 1) {
                    messageBuilder.append(outputs[i]);
                } else {
                    messageBuilder.append(outputs[i]).append(" ");
                }
            }
            this.message = messageBuilder.toString();
        } else {
            throw new IllegalArgumentException("Unexpected raw line format -> " + rawLine);
        }
    }

    public String getRawLine() {
        return rawLine;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getTag() {
        return tag;
    }

    public int getPid() {
        return pid;
    }

    public int getTid() {
        return tid;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("---LogcatLine---\n");
        builder.append("rawLine:").append(rawLine).append("\n");
        builder.append("date:").append(date).append("\n");
        builder.append("time:").append(time).append("\n");
        builder.append("pid:").append(pid).append("\n");
        builder.append("tid:").append(tid).append("\n");
        builder.append("priority:").append(priority.getValue()).append("\n");
        builder.append("tag:").append(tag).append("\n");
        builder.append("message:").append(message).append("\n");
        return builder.toString();
    }
}

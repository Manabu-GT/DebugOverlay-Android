package com.ms_square.debugoverlay;

import android.app.Application;

import java.util.List;

public class DebugOverlay {

    public static final Position DEFAULT_POSITION = Position.BOTTOM_START;
    public static final int DEFAULT_BG_COLOR = 0;
    public static final int DEFAULT_TEXT_COLOR = 0;
    public static final float DEFAULT_TEXT_SIZE = 0f;
    public static final float DEFAULT_TEXT_ALPHA = 0f;

    public static DebugOverlay with(Application application) {
        return new DebugOverlay();
    }

    public static void enableDebugLogging(boolean enabled) {

    }

    public static boolean isDebugLoggingEnabled() {
        return false;
    }

    public void install() {

    }

    public static class Builder {


        public Builder(Application application) {

        }

        public Builder modules(List<OverlayModule> overlayModules) {
            return this;
        }

        public Builder modules(OverlayModule overlayModule, OverlayModule... other) {
            return this;
        }

        public Builder position(Position position) {
            return this;
        }

        public Builder bgColor(int color) {
            return this;
        }

        public Builder textColor(int color) {
            return this;
        }

        public Builder textSize(float size) {
            return this;
        }

        public Builder textAlpha(float alpha) {
            return this;
        }

        public Builder allowSystemLayer(boolean allowSystemLayer) {
            return this;
        }

        public Builder notification(boolean show) {
            return this;
        }

        public Builder notification(boolean show, String activityName) {
            return this;
        }

        public DebugOverlay build() {
            return new DebugOverlay();
        }
    }
}

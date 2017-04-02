package com.ms_square.debugoverlay;

public enum Position {

    TOP_START(),
    TOP_CENTER(),
    TOP_END(),

    CENTER_START(),
    CENTER(),
    CENTER_END(),

    BOTTOM_START(),
    BOTTOM(),
    BOTTOM_END();

    public int getGravity() {
        return 0;
    }
}

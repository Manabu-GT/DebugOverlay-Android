package com.ms_square.debugoverlay;

import android.view.Gravity;

import androidx.annotation.GravityInt;

public enum Position {

    TOP_START(Gravity.TOP | Gravity.START),
    TOP_CENTER(Gravity.TOP | Gravity.CENTER_HORIZONTAL),
    TOP_END(Gravity.TOP | Gravity.END),

    CENTER_START(Gravity.CENTER_VERTICAL | Gravity.START),
    CENTER(Gravity.CENTER),
    CENTER_END(Gravity.CENTER_VERTICAL | Gravity.END),

    BOTTOM_START(Gravity.BOTTOM | Gravity.START),
    BOTTOM(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL),
    BOTTOM_END(Gravity.BOTTOM | Gravity.END);

    private final int gravity;

    Position(int gravity) {
        this.gravity = gravity;
    }

    @GravityInt
    public int getGravity() {
        return gravity;
    }
}

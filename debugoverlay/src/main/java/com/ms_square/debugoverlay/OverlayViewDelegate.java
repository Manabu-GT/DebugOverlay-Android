package com.ms_square.debugoverlay;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

public interface OverlayViewDelegate<T> extends OverlayModuleObserver<T> {

    /**
     * Given the root (overlay's view container), create an overlay view to add
     * either programmatically or using LayoutInflater.
     * You are also given the preferred textColor, textSize, and textAlpha for your overlay view.
     * Please respect those passed values unless you have good reason not to do so.
     *
     * @param root
     * @param textColor
     * @param textSize - in sp
     * @param textAlpha
     * @return View to be added to the overlay container.
     */
    View createView(@NonNull ViewGroup root, @ColorInt int textColor, float textSize, float textAlpha);
}

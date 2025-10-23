package com.ms_square.debugoverlay;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public interface ViewModule<T> extends DataObserver<T> {

    /**
     * Given the root (overlay's view container), create an overlay view to add
     * either programmatically or using LayoutInflater.
     * You are also given the preferred textColor, textSize, and textAlpha for your overlay view.
     * Please respect those passed values unless you have good reason not to do so.
     *
     * Note:
     * {@link DataObserver}'s onDataAvailable could be called before this method if systemLayer is
     * not allowed to use. So, it's safe to do null checks within onDataAvailable()
     * for any view variable references you keep as a result of this method.
     *
     * @param root
     * @param textColor
     * @param textSize - in sp
     * @param textAlpha
     * @return View to be added to the overlay container.
     */
    @NonNull
    View createView(@NonNull ViewGroup root, @ColorInt int textColor, float textSize, float textAlpha);
}

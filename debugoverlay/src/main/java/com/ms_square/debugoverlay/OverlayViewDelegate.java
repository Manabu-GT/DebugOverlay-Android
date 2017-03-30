package com.ms_square.debugoverlay;

import android.support.annotation.ColorInt;
import android.view.View;
import android.view.ViewGroup;

public interface OverlayViewDelegate<T> extends OverlayModuleObserver<T> {

    View createView(ViewGroup root, @ColorInt int textColor, float textSize, float textAlpha);
}

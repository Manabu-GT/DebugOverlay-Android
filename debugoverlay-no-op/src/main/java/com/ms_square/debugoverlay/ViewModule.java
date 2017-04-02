package com.ms_square.debugoverlay;

import android.view.View;
import android.view.ViewGroup;

public interface ViewModule<T> extends DataObserver<T> {

    View createView(ViewGroup root, int textColor, float textSize, float textAlpha);
}

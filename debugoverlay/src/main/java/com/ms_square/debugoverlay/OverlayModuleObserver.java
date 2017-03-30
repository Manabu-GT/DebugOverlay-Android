package com.ms_square.debugoverlay;

import android.support.annotation.UiThread;

public interface OverlayModuleObserver<T> {
    @UiThread
    void onDataAvailable(T data);
}

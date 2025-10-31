package com.ms.square.debugoverlay;

import androidx.annotation.UiThread;

public interface DataObserver<T> {
    @UiThread
    void onDataAvailable(T data);
}

package com.ms_square.debugoverlay;

import android.support.annotation.UiThread;

public interface DataObserver<T> {
    @UiThread
    void onDataAvailable(T data);
}

package com.ms_square.debugoverlay;

public interface DataObserver<T> {
    void onDataAvailable(T data);
}

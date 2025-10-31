package com.ms.square.debugoverlay;

public interface DataObserver<T> {
    void onDataAvailable(T data);
}

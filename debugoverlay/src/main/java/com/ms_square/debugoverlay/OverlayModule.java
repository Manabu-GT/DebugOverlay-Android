package com.ms_square.debugoverlay;

import android.support.annotation.NonNull;

public interface OverlayModule<T> {

    OverlayViewDelegate<T> createOverlayViewDelegate();

    void start();

    void stop();

    void addObserver(@NonNull OverlayModuleObserver observer);

    void removeObserver(@NonNull OverlayModuleObserver observer);
}

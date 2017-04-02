package com.ms_square.debugoverlay;

import android.view.View;
import android.view.ViewGroup;

public abstract class OverlayModule<T> implements DataModule<T>, ViewModule<T> {

    public OverlayModule(DataModule<T> dataModule, ViewModule<T> viewModule) {

    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void notifyObservers() {
    }

    @Override
    public void addObserver(DataObserver observer) {
    }

    @Override
    public void removeObserver(DataObserver observer) {
    }

    @Override
    public void onDataAvailable(T data) {
    }

    @Override
    public View createView(ViewGroup root, int textColor, float textSize, float textAlpha) {
        return null;
    }
}

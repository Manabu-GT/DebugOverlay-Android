package com.ms_square.debugoverlay;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

public abstract class OverlayModule<T> implements DataModule<T>, ViewModule<T> {

    private final DataModule<T> dataModule;

    private final ViewModule<T> viewModule;

    public OverlayModule(DataModule<T> dataModule, ViewModule<T> viewModule) {
        this.dataModule = dataModule;
        this.viewModule = viewModule;
    }

    @Override
    public void start() {
        dataModule.addObserver(viewModule);
        dataModule.start();
    }

    @Override
    public void stop() {
        dataModule.removeObserver(viewModule);
        dataModule.stop();
    }

    @Override
    public void notifyObservers() {
        dataModule.notifyObservers();
    }

    @Override
    public void addObserver(@NonNull DataObserver observer) {
        dataModule.addObserver(observer);
    }

    @Override
    public void removeObserver(@NonNull DataObserver observer) {
        dataModule.removeObserver(observer);
    }

    @Override
    public void onDataAvailable(T data) {
        viewModule.onDataAvailable(data);
    }

    @Override
    public View createView(@NonNull ViewGroup root, @ColorInt int textColor, float textSize, float textAlpha) {
        return viewModule.createView(root, textColor, textSize, textAlpha);
    }
}

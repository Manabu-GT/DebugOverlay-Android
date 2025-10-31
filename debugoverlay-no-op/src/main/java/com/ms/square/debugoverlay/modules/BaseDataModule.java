package com.ms.square.debugoverlay.modules;

import com.ms.square.debugoverlay.DataModule;
import com.ms.square.debugoverlay.DataObserver;

public abstract class BaseDataModule<T> implements DataModule<T> {

    @Override
    public void notifyObservers() {

    }

    @Override
    public void addObserver(DataObserver observer) {

    }

    @Override
    public void removeObserver(DataObserver observer) {
    }

    protected abstract T getLatestData();
}

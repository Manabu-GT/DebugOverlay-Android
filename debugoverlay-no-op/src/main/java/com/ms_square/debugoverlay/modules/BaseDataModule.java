package com.ms_square.debugoverlay.modules;

import com.ms_square.debugoverlay.DataModule;
import com.ms_square.debugoverlay.DataObserver;

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

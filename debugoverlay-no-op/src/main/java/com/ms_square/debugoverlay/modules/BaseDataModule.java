package com.ms_square.debugoverlay.modules;

import com.ms_square.debugoverlay.DataModule;
import com.ms_square.debugoverlay.DataObserver;

public abstract class BaseDataModule<T> implements DataModule<T> {

    public BaseDataModule(int interval) {

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

    protected int getInterval() {
        return 0;
    }

    protected abstract T getLatestData();
}

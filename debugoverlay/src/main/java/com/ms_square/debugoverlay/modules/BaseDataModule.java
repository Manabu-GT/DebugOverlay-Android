package com.ms_square.debugoverlay.modules;

import androidx.annotation.NonNull;

import com.ms_square.debugoverlay.DataModule;
import com.ms_square.debugoverlay.DataObserver;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseDataModule<T> implements DataModule<T> {

    private final List<DataObserver> observers = new ArrayList<>();

    @Override
    public void notifyObservers() {
        T data = getLatestData();
        for (DataObserver observer : observers) {
            observer.onDataAvailable(data);
        }
    }

    @Override
    public void addObserver(@NonNull DataObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(@NonNull DataObserver observer) {
        observers.remove(observer);
    }

    protected abstract T getLatestData();
}

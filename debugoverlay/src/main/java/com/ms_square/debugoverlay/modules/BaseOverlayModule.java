package com.ms_square.debugoverlay.modules;

import android.support.annotation.NonNull;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.OverlayModuleObserver;
import com.ms_square.debugoverlay.OverlayViewDelegate;
import com.ms_square.debugoverlay.OverlayViewDelegateFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseOverlayModule<T> implements OverlayModule<T> {

    private final List<OverlayModuleObserver> observers = new ArrayList<>();

    private final OverlayViewDelegateFactory<T> viewDelegateFactory;

    private int interval;

    public BaseOverlayModule(int interval) {
        this.interval = interval;
        this.viewDelegateFactory = getDefaultFactory();
    }

    public BaseOverlayModule(int interval, @NonNull OverlayViewDelegateFactory<T> viewDelegateFactory) {
        this.interval = interval;
        this.viewDelegateFactory = viewDelegateFactory;
    }

    @Override
    public final OverlayViewDelegate<T> createOverlayViewDelegate() {
        return viewDelegateFactory.create();
    }

    @Override
    public void addObserver(@NonNull OverlayModuleObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(@NonNull OverlayModuleObserver observer) {
        observers.remove(observer);
    }

    protected int getInterval() {
        return interval;
    }

    protected abstract OverlayViewDelegateFactory<T> getDefaultFactory();

    protected abstract T getLatestData();

    protected void notifyObservers() {
        T data = getLatestData();
        for (OverlayModuleObserver observer : observers) {
            observer.onDataAvailable(data);
        }
    }
}

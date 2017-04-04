package com.ms_square.debugoverlay;

public interface DataModule<T> {

    void start();

    void stop();

    void notifyObservers();

    void addObserver(DataObserver<T> observer);

    void removeObserver(DataObserver<T> observer);
}

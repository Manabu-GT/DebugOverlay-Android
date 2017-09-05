package com.ms_square.debugoverlay.modules;

import android.view.Choreographer;

import java.util.concurrent.TimeUnit;

class FpsDataModule extends BaseDataModule<Double> implements Choreographer.FrameCallback {

    private final Choreographer choreographer;

    private long startFrameTimeMillis;
    private int numFramesRendered;

    private double fps;

    private final int interval;

    public FpsDataModule(int interval) {
        this.interval = interval;
        this.choreographer = Choreographer.getInstance();
    }

    @Override
    public void start() {
        choreographer.postFrameCallback(this);
    }

    @Override
    public void stop() {
        choreographer.removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        long currentFrameTimeMillis = TimeUnit.NANOSECONDS.toMillis(frameTimeNanos);

        if (startFrameTimeMillis > 0) {
            long duration = currentFrameTimeMillis - startFrameTimeMillis;
            numFramesRendered++;

            if (duration > interval) {
                fps = numFramesRendered * 1000f / duration;

                notifyObservers();

                startFrameTimeMillis = currentFrameTimeMillis;
                numFramesRendered = 0;
            }
        } else {
            startFrameTimeMillis = currentFrameTimeMillis;
        }

        choreographer.postFrameCallback(this);
    }

    @Override
    protected Double getLatestData() {
        return Double.valueOf(fps);
    }
}

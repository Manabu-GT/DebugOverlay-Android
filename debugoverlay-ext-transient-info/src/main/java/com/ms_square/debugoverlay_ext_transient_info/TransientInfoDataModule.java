package com.ms_square.debugoverlay_ext_transient_info;

import android.os.Handler;
import android.os.Looper;

import com.ms_square.debugoverlay.modules.BaseDataModule;

import java.util.LinkedList;
import java.util.List;

public class TransientInfoDataModule extends BaseDataModule<String> {

    public interface InfoProvider
    {
        String getInfo();
    }

    private final List<InfoProvider> infoProviders = new LinkedList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int interval;
    private String latestData;

    TransientInfoDataModule(int interval) {
        this.interval = interval;
    }

    void setInterval(int interval) {
        this.interval = interval;
    }

    @Override
    protected String getLatestData() {
        return latestData;
    }

    void addProvider(InfoProvider provider)
    {
        synchronized (infoProviders) {
            infoProviders.add(provider);
        }
    }

    void clearProviders()
    {
        synchronized (infoProviders) {
            infoProviders.clear();
        }
    }

    @Override
    public void start() {
        handler.post(infoCollectionRunnable);
    }

    @Override
    public void stop() {
        handler.removeCallbacks(infoCollectionRunnable);
    }

    private final Runnable infoCollectionRunnable = new Runnable() {

        @Override
        public void run() {

            boolean first = true;
            StringBuilder builder = new StringBuilder();
            synchronized (infoProviders) {
                for (InfoProvider provider : infoProviders) {
                    if (first) {
                        first = false;
                    } else {
                        builder.append('\n');
                    }
                    builder.append(provider.getInfo());
                }
            }
            latestData = builder.toString();
            notifyObservers();
            handler.postDelayed(infoCollectionRunnable, interval);
        }
    };
}

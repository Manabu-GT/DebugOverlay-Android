package com.ms_square.debugoverlay_ext_netstats;

import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;

import com.ms_square.debugoverlay.modules.BaseDataModule;

import java.util.Locale;

public class NetStatsDataModule extends BaseDataModule<String> {

    private static final String TAG = NetStatsDataModule.class.getSimpleName();

    private static final String HEADER = "Received: %8s/s\nSent: %12s/s";

    private Handler handler = new Handler(Looper.getMainLooper());

    private final int interval;
    private final int uid;

    private double mPreviousReceived = 0;
    private double mPreviousSent = 0;
    private double mReceived;
    private double mSent;

    public NetStatsDataModule(int interval, int uid) {
        this.interval = interval;
        this.uid = uid;
    }

    @Override
    protected String getLatestData() {
        return String.format(
                Locale.ENGLISH, HEADER, bytesToPrettyString(mReceived), bytesToPrettyString(mSent));
    }

    @Override
    public void start() {
        handler.post(networkStatisticsQueryRunnable);
    }

    @Override
    public void stop() {
        handler.removeCallbacks(networkStatisticsQueryRunnable);
    }

    private final Runnable networkStatisticsQueryRunnable = new Runnable() {

        @Override
        public void run() {
            double seconds = interval * 0.001;
            double totalBytesReceived = TrafficStats.getUidRxBytes(uid) / seconds;
            double totalBytesSent = TrafficStats.getUidTxBytes(uid) / seconds;
            mReceived = totalBytesReceived - mPreviousReceived + Math.random() * 10000000;
            mSent = totalBytesSent - mPreviousSent + Math.random() * 100000;
            mPreviousSent = totalBytesSent;
            mPreviousReceived = totalBytesReceived;

            notifyObservers();
            handler.postDelayed(networkStatisticsQueryRunnable, interval);
        }
    };

    private String bytesToPrettyString(double bytes)
    {
        if (bytes >= 1000000000.0)
            return String.format(Locale.ENGLISH, "%.1f GB", bytes / 1000000000.0);
        else if (bytes >= 1000000.0)
            return String.format(Locale.ENGLISH, "%.1f MB", bytes / 1000000.0);
        else if (bytes >= 1000.0)
            return String.format(Locale.ENGLISH, "%.1f kB", bytes / 1000.0);
        else
            return String.format(Locale.ENGLISH, "%.1f  B", bytes);
    }
}

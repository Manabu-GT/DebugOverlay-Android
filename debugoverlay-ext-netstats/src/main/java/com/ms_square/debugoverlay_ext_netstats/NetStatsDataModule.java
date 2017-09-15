package com.ms_square.debugoverlay_ext_netstats;

import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;

import com.ms_square.debugoverlay.modules.BaseDataModule;

import java.util.Locale;

public class NetStatsDataModule extends BaseDataModule<String> {

    private static final String TAG = NetStatsDataModule.class.getSimpleName();

    private static final String HEADER = "Received: %8s/s\nSent: %12s/s";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final int intervalMilliseconds;
    private final int uid;
    private final double intervalSeconds;

    private Double previousReceived = null;
    private Double previousSent = null;
    private double received;
    private double sent;

    public NetStatsDataModule(int intervalMilliseconds) {
        this.uid = android.os.Process.myUid();
        this.intervalMilliseconds = intervalMilliseconds;
        this.intervalSeconds = intervalMilliseconds * 0.001;
    }

    @Override
    protected String getLatestData() {
        return String.format(
                Locale.US, HEADER, bytesToPrettyString(received), bytesToPrettyString(sent));
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
            double totalBytesReceived = TrafficStats.getUidRxBytes(uid);
            double totalBytesSent = TrafficStats.getUidTxBytes(uid);
            if (previousReceived == null)
                previousReceived = totalBytesReceived;
            if (previousSent == null)
                previousSent = totalBytesSent;

            received = (totalBytesReceived - previousReceived) / intervalSeconds;
            sent = (totalBytesSent - previousSent) / intervalSeconds;
            previousSent = totalBytesSent;
            previousReceived = totalBytesReceived;

            notifyObservers();
            handler.postDelayed(networkStatisticsQueryRunnable, intervalMilliseconds);
        }
    };

    private String bytesToPrettyString(double bytes)
    {
        if (bytes >= 1000000000.0)
            return String.format(Locale.US, "%.1f GB", bytes / 1000000000.0);
        else if (bytes >= 1000000.0)
            return String.format(Locale.US, "%.1f MB", bytes / 1000000.0);
        else if (bytes >= 1000.0)
            return String.format(Locale.US, "%.1f kB", bytes / 1000.0);
        else
            return String.format(Locale.US, "%.1f  B", bytes);
    }
}

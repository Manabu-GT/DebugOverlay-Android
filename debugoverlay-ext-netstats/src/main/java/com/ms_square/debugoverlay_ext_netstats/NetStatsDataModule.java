package com.ms_square.debugoverlay_ext_netstats;

import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ms_square.debugoverlay.modules.BaseDataModule;

import java.util.Locale;

public class NetStatsDataModule extends BaseDataModule<String> {

    private static final String TAG = NetStatsDataModule.class.getSimpleName();

    private static final String HEADER = "Received: %8s/s\nSent: %12s/s";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final int intervalMilliseconds;
    private final int uid;
    private final double intervalSeconds;

    private double previousReceived;
    private double previousSent;
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
        // initialize these values here as
        previousReceived = -1f;
        previousSent = -1f;
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

            if (totalBytesReceived == TrafficStats.UNSUPPORTED || totalBytesSent == TrafficStats.UNSUPPORTED) {
                Log.w(TAG, "The use of TrafficStats is not supported on this device.");
                return;
            }

            if (previousReceived >= 0 && previousSent >= 0) {
                received = (totalBytesReceived - previousReceived) / intervalSeconds;
                sent = (totalBytesSent - previousSent) / intervalSeconds;
                notifyObservers();
            }
            previousReceived = totalBytesReceived;
            previousSent = totalBytesSent;

            handler.postDelayed(this, intervalMilliseconds);
        }
    };

    private static final double BYTES_PER_GIGABYTE = 1000000000f;
    private static final double BYTES_PER_MEGABYTE = 1000000f;
    private static final double BYTES_PER_KILOBYTE = 1000f;

    private String bytesToPrettyString(double bytes)
    {
        if (bytes >= BYTES_PER_GIGABYTE) {
            return String.format(Locale.US, "%.1f GB", bytes / BYTES_PER_GIGABYTE);
        }
        else if (bytes >= BYTES_PER_MEGABYTE) {
            return String.format(Locale.US, "%.1f MB", bytes / BYTES_PER_MEGABYTE);
        }
        else if (bytes >= BYTES_PER_KILOBYTE) {
            return String.format(Locale.US, "%.1f kB", bytes / BYTES_PER_KILOBYTE);
        }
        else {
            return String.format(Locale.US, "%.1f  B", bytes);
        }
    }
}

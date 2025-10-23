package com.ms_square.debugoverlay.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.ms_square.debugoverlay.modules.BaseDataModule;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IPAddressDataModule extends BaseDataModule<String> {

    private static final String TAG = IPAddressDataModule.class.getSimpleName();

    private final Context context;

    private String ipAddresses;

    public IPAddressDataModule(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public void start() {
        context.registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        ipAddresses = getV4IPAddressesString();
        notifyObservers();
    }

    @Override
    public void stop() {
        context.unregisterReceiver(receiver);
    }

    @Override
    protected String getLatestData() {
        return ipAddresses;
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ConnectivityManager.CONNECTIVITY_ACTION: {
                    ipAddresses = getV4IPAddressesString();
                    notifyObservers();
                    break;
                }
            }
        }
    };

    private static String getV4IPAddressesString() {
        return Arrays.toString(getIPAddresses(true).toArray());
    }

    /**
     * Updated based on the discussion in
     * http://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
     * Get IP addresses from the non-localhost interfaces
     * @param useIPv4  true -> returns ipv4, false -> returns ipv6
     * @return  addresses or empty list
     */
    private static List<String> getIPAddresses(boolean useIPv4) {
        List<String> ipAddresses = new ArrayList<>();
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.getName().startsWith("dummy")) {
                    continue;
                }
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (useIPv4) {
                            if (isIPv4) {
                                ipAddresses.add(sAddr);
                            }
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                ipAddresses.add(delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase());
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // for now, just catch all...
            Log.w(TAG, "Exception:" + ex.getMessage());
        }
        return ipAddresses;
    }
}

package com.ms_square.debugoverlay_ext_transient_info;

import android.util.Log;

import com.ms_square.debugoverlay.OverlayModule;

import java.util.HashMap;

public class TransientInfoModule extends OverlayModule<String> {

    private static final String TAG = TransientInfoModule.class.getSimpleName();
    private static final String DEFAULT_KEY = "default_";
    private static HashMap<String, TransientInfoDataModule> modules = new HashMap<>();

    public TransientInfoModule(int interval) {
        this(interval, DEFAULT_KEY);
    }

    public TransientInfoModule(int interval, String key) {
        super(init(interval, key), new TransientInfoViewModule(R.layout.overlay_view));
    }

    private static TransientInfoDataModule init(int interval, String key) {
        TransientInfoDataModule transientInfoDataModule = TransientInfoModule.modules.get(key);
        if (transientInfoDataModule == null) {
            transientInfoDataModule = new TransientInfoDataModule(interval);
            TransientInfoModule.modules.put(key, transientInfoDataModule);
        }
        else {
            transientInfoDataModule.setInterval(interval);
        }
        return transientInfoDataModule;
    }

    public static void addProvider(TransientInfoDataModule.InfoProvider provider) {
        addProvider(provider, DEFAULT_KEY);
    }

    public static void addProvider(TransientInfoDataModule.InfoProvider provider, String key) {
        TransientInfoDataModule transientInfoDataModule = TransientInfoModule.modules.get(key);
        if (transientInfoDataModule == null) {
            Log.e(TAG, "Key '" + key + "' does not exist.");
            return;
        }
        transientInfoDataModule.addProvider(provider);
    }

    public static void clearProviders() {
        clearProviders(DEFAULT_KEY);
    }

    public static void clearProviders(String key) {
        TransientInfoDataModule transientInfoDataModule = TransientInfoModule.modules.get(key);
        if (transientInfoDataModule == null) {
            Log.e(TAG, "Key '" + key + "' does not exist.");
            return;
        }
        transientInfoDataModule.clearProviders();
    }
}

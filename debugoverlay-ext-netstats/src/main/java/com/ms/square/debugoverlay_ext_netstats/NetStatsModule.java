package com.ms.square.debugoverlay_ext_netstats;

import com.ms.square.debugoverlay.OverlayModule;
import com.ms.square.debugoverlay.modules.SimpleViewModule;

public class NetStatsModule extends OverlayModule<String> {

    private static final int DEFAULT_INTERVAL = 1000; // ms

    public NetStatsModule() {
        this(DEFAULT_INTERVAL);
    }

    public NetStatsModule(int interval) {
        super(new NetStatsDataModule(interval), new SimpleViewModule(R.layout.debugoverlay_netstats));
    }
}

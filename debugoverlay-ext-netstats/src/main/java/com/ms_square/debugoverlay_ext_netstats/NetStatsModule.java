package com.ms_square.debugoverlay_ext_netstats;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.modules.SimpleViewModule;

public class NetStatsModule extends OverlayModule<String> {

    private static final int DEFAULT_INTERVAL = 1000; // ms

    public NetStatsModule() {
        this(DEFAULT_INTERVAL, android.os.Process.myUid());
    }

    public NetStatsModule(int interval, int uid) {
        super(new NetStatsDataModule(interval, uid), new SimpleViewModule(R.layout.debugoverlay_netstats));
    }
}

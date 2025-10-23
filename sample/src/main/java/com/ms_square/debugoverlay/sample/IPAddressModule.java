package com.ms_square.debugoverlay.sample;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ms_square.debugoverlay.OverlayModule;
import com.ms_square.debugoverlay.modules.SimpleViewModule;

public class IPAddressModule extends OverlayModule<String> {

    public IPAddressModule(@NonNull Context context) {
        super(new IPAddressDataModule(context), new SimpleViewModule(R.layout.view_overlay_ip));
    }
}

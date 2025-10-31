package com.ms.square.debugoverlay.sample

import android.content.Context
import com.ms_square.debugoverlay.OverlayModule
import com.ms_square.debugoverlay.modules.SimpleViewModule

class IPAddressModule(context: Context) :
  OverlayModule<String>(
    IPAddressDataModule(context),
    SimpleViewModule(R.layout.view_overlay_ip)
  )

package com.ms.square.debugoverlay.sample

import android.content.Context
import com.ms.square.debugoverlay.OverlayModule
import com.ms.square.debugoverlay.modules.SimpleViewModule

class IPAddressModule(context: Context) :
  OverlayModule<String>(
    IPAddressDataModule(context),
    SimpleViewModule(R.layout.view_overlay_ip)
  )

package com.ms.square.debugoverlay.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.ms_square.debugoverlay.modules.BaseDataModule
import timber.log.Timber
import java.io.IOException
import java.net.NetworkInterface

class IPAddressDataModule(private val context: Context) : BaseDataModule<String>() {

  private var ipAddresses: String = ""

  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        ConnectivityManager.CONNECTIVITY_ACTION -> {
          ipAddresses = getV4IPAddressesString()
          notifyObservers()
        }
      }
    }
  }

  override fun start() {
    context.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    ipAddresses = getV4IPAddressesString()
    notifyObservers()
  }

  override fun stop() {
    context.unregisterReceiver(receiver)
  }

  override fun getLatestData(): String = ipAddresses

  companion object {
    private val TAG = IPAddressDataModule::class.java.simpleName

    /**
     * Get IP addresses from non-localhost interfaces.
     * Updated based on discussion in:
     * http://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
     *
     * @param useIPv4 true -> returns IPv4, false -> returns IPv6
     * @return addresses or empty list
     */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    private fun getIPAddresses(useIPv4: Boolean): List<String> {
      val ipAddresses = mutableListOf<String>()
      try {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
        for (intf in interfaces) {
          if (intf.name.startsWith("dummy")) {
            continue
          }
          val addrs = intf.inetAddresses?.toList() ?: emptyList()
          for (addr in addrs) {
            if (!addr.isLoopbackAddress) {
              val sAddr = addr.hostAddress ?: continue
              val isIPv4 = sAddr.indexOf(':') < 0
              if (useIPv4) {
                if (isIPv4) {
                  ipAddresses.add(sAddr)
                }
              } else {
                if (!isIPv4) {
                  val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                  ipAddresses.add(
                    if (delim < 0) {
                      sAddr.uppercase()
                    } else {
                      sAddr.substring(0, delim).uppercase()
                    }
                  )
                }
              }
            }
          }
        }
      } catch (ex: IOException) {
        Timber.tag(TAG).w("Exception: ${ex.message}")
      }
      return ipAddresses
    }

    private fun getV4IPAddressesString(): String = getIPAddresses(true).toString()
  }
}

package com.ms.square.debugoverlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ms.square.debugoverlay.internal.Logger
import com.ms.square.debugoverlay.internal.OverlayViewManager

@Suppress("TooManyFunctions")
class DebugOverlayService : Service() {

  private val binder = LocalBinder()

  private lateinit var config: DebugOverlay.Config
  private var overlayViewManager: OverlayViewManager? = null
  private lateinit var notificationManager: NotificationManager

  private var modulesStarted = false

  private var actionShow = ""
  private var actionHide = ""

  inner class LocalBinder : Binder() {
    val service: DebugOverlayService
      get() = this@DebugOverlayService
  }

  override fun onCreate() {
    notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    createNotificationChannel()

    val packageName = packageName
    actionShow = packageName + ACTION_SHOW_SUFFIX
    actionHide = packageName + ACTION_HIDE_SUFFIX

    val intentFilter = IntentFilter().apply {
      addAction(actionShow)
      addAction(actionHide)
    }
    ContextCompat.registerReceiver(this, receiver, intentFilter, ContextCompat.RECEIVER_EXPORTED)
  }

  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    intent.getParcelableExtra<DebugOverlay.Config>(DebugOverlay.KEY_CONFIG)?.let {
      config = it
    }
    // no need to restart this service
    return START_NOT_STICKY
  }

  override fun onDestroy() {
    unregisterReceiver(receiver)
    cancelNotification()
    stopModules()
    overlayViewManager?.hideOverlay()
  }

  override fun onBind(intent: Intent?): IBinder = binder

  override fun onTaskRemoved(rootIntent: Intent?) {
    stopSelf()
    LocalBroadcastManager.getInstance(applicationContext)
      .sendBroadcastSync(Intent(DebugOverlay.ACTION_UNBIND))
  }

  internal fun setOverlayViewManager(overlayViewManager: OverlayViewManager) {
    this.overlayViewManager = overlayViewManager
    overlayViewManager.showOverlay()
    showNotification()
  }

  fun startModules() {
    if (!modulesStarted) {
      // used to start all overlay modules here
      modulesStarted = true
    }
  }

  fun stopModules() {
    if (modulesStarted) {
      // used to stop all overlay modules here
      modulesStarted = false
    }
  }

  fun updateNotification() {
    showNotification()
  }

  private fun createNotificationChannel() {
    if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
      val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        getString(R.string.debugoverlay_notification_channel_name),
        NotificationManager.IMPORTANCE_LOW
      )
      notificationManager.createNotificationChannel(channel)
    }
  }

  private fun showNotification() {
    val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText(getString(R.string.debugoverlay_notification_big_text))
      )
      .setSmallIcon(R.drawable.debugoverlay_ic_notification)
      .setLargeIcon(getAppIcon(this))
      .setOngoing(true)
      .setContentTitle(getString(R.string.debugoverlay_notification_title, getAppName(this), getAppVersion(this)))
      .setContentText(getString(R.string.debugoverlay_notification_small_text))
      .setContentIntent(getLaunchIntent())

    overlayViewManager?.let { manager ->
      if (manager.isOverlayShown()) {
        builder.addAction(
          R.drawable.debugoverlay_ic_action_pause,
          getString(R.string.debugoverlay_notification_action_hide),
          getBroadcastIntent(actionHide)
        )
      } else {
        builder.addAction(
          R.drawable.debugoverlay_ic_action_play,
          getString(R.string.debugoverlay_notification_action_show),
          getBroadcastIntent(actionShow)
        )
      }
    }

    // show the notification
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(NOTIFICATION_ID, builder.build())
    }
  }

  private fun cancelNotification() {
    notificationManager.cancel(NOTIFICATION_ID)
  }

  private fun getBroadcastIntent(action: String): PendingIntent {
    val intent = Intent(action)
    return PendingIntent.getBroadcast(
      this,
      0,
      intent,
      PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  private fun getLaunchIntent(): PendingIntent? = config.activityName?.let { className ->
    try {
      val intent = Intent(this, Class.forName(className))
      PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
    } catch (ne: ClassNotFoundException) {
      Logger.w("${config.activityName} was not found - ${ne.message}", ne)
      null
    }
  }

  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        actionShow -> {
          overlayViewManager?.showOverlay()
          startModules()
          // update notification
          showNotification()
        }
        actionHide -> {
          stopModules()
          overlayViewManager?.hideOverlay()
          // update notification
          showNotification()
        }
      }
    }
  }

  companion object {

    private const val NOTIFICATION_CHANNEL_ID = "com.ms_square.debugoverlay"
    private const val NOTIFICATION_ID = Int.MAX_VALUE - 100

    private const val ACTION_SHOW_SUFFIX = ".debugoverlay_ACTION_SHOW"
    private const val ACTION_HIDE_SUFFIX = ".debugoverlay_ACTION_HIDE"

    @JvmStatic
    fun createIntent(context: Context): Intent = Intent(context, DebugOverlayService::class.java)

    private fun getAppIcon(context: Context): Bitmap? {
      val drawable: Drawable? = try {
        context.packageManager.getApplicationIcon(context.packageName)
      } catch (e: PackageManager.NameNotFoundException) {
        Logger.w("Package Not found:${context.packageName}", e)
        null
      }
      return (drawable as? BitmapDrawable)?.bitmap
    }

    private fun getAppName(context: Context): String {
      val packageManager = context.packageManager
      val applicationInfo: ApplicationInfo? = try {
        packageManager.getApplicationInfo(context.packageName, 0)
      } catch (e: PackageManager.NameNotFoundException) {
        Logger.w("Package Not found:${context.packageName}", e)
        null
      }
      return applicationInfo?.let { packageManager.getApplicationLabel(it).toString() } ?: "Unknown"
    }

    private fun getAppVersion(context: Context): String = try {
      context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (ex: PackageManager.NameNotFoundException) {
      Logger.w("Package Not found:${context.packageName}", ex)
      "Unknown"
    }
  }
}

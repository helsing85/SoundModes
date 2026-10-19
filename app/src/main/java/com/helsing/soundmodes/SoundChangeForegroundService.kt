package com.helsing.soundmodes

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.helsing.soundmodes.widgets.ToggleAllWidgetGlanceReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Since Android 17 ("background audio hardening") volume and ringer APIs such as
 * [android.media.AudioManager.setRingerMode] are silently ignored unless the app has a visible
 * activity or a running foreground service. The tile and the widget run in the background,
 * so the sound mode change is performed while this short-lived foreground service is running.
 */
class SoundChangeForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.sound_change_channel_name),
            NotificationManager.IMPORTANCE_MIN
        )
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.sound_change_notification_title))
            .setSmallIcon(R.drawable.app_icon)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        // Must be called before changing the sound mode, so the app is in a valid
        // lifecycle state for the audio framework
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        SoundModesManager(this).changeSoundMode {}

        // Keep the service in foreground until the widgets are refreshed, otherwise
        // the process can be frozen before the update is finished
        serviceScope.launch {
            ToggleAllWidgetGlanceReceiver.refreshAllGlanceWidgets(this@SoundChangeForegroundService)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "SoundChangeFgService"
        private const val CHANNEL_ID = "sound_modes_action_channel"
        private const val NOTIFICATION_ID = 1001

        /**
         * Starts the service which changes the sound mode.
         * Returns false if the system doesn't allow to start the foreground service right now.
         */
        fun start(context: Context): Boolean {
            return try {
                context.startForegroundService(
                    Intent(context, SoundChangeForegroundService::class.java)
                )
                true
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Log.w(TAG, "Foreground service start not allowed", e)
                false
            }
        }
    }
}

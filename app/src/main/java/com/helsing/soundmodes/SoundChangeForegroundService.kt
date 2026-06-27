package com.helsing.soundmodes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundChangeForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "sound_modes_action_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Zmiana trybu dźwięku",
            NotificationManager.IMPORTANCE_MIN
        )
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Aktualizowanie trybu dźwięku...")
            .setSmallIcon(R.drawable.app_icon)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(1001, notification)

        val soundManager = SoundModesManager(this)

        soundManager.changeSoundMode {}

        TileService.requestListeningState(this, ComponentName(this, SoundModesService::class.java))

        serviceScope.launch {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

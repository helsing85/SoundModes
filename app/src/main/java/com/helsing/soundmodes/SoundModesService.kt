package com.helsing.soundmodes

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService


class SoundModesService : TileService() {
    private val manager by lazy { SoundModesManager(this) }

    // Refreshes the tile when the ringer mode is changed while the tile is visible,
    // either by this app or by the system (e.g. volume buttons)
    private val ringerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshTile()
        }
    }
    private var isReceiverRegistered = false

    private fun refreshTile() {
        val tile = qsTile ?: return
        val tileLabel = manager.getModeLabel()

        tile.contentDescription = tileLabel
        tile.label = tileLabel
        tile.icon = Icon.createWithResource(this, manager.getModeIcon())
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    private fun collapseAndStart(intent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        startActivityAndCollapse(pendingIntent)
    }

    override fun onClick() {
        super.onClick()

        if (!manager.isNotificationPolicyAccessPermissionEnabled()) {
            collapseAndStart(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            return
        }

        // Tile is refreshed by ringerModeReceiver when the mode is changed
        if (!SoundChangeForegroundService.start(this)) {
            // Fallback: a visible activity is also a valid state to change the ringer mode
            collapseAndStart(
                Intent(this, SoundChangeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        if (!isReceiverRegistered) {
            registerReceiver(
                ringerModeReceiver,
                IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION),
                RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
        }
        refreshTile()
    }

    override fun onStopListening() {
        if (isReceiverRegistered) {
            unregisterReceiver(ringerModeReceiver)
            isReceiverRegistered = false
        }
        super.onStopListening()
    }
}

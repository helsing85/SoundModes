package com.helsing.soundmodes

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService


class SoundModesService : TileService() {
    private val manager by lazy { SoundModesManager(this) }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val tileLabel = manager.getModeLabel()

        tile.contentDescription = tileLabel
        tile.label = tileLabel
        tile.icon = Icon.createWithResource(this, manager.getModeIcon())
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()

        if (!manager.isNotificationPolicyAccessPermissionEnabled(this)) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
            return
        }

        val tile = qsTile
        if (tile != null) {
            val currentMode = (getSystemService(AUDIO_SERVICE) as AudioManager).ringerMode

            val allModes = listOf(
                AudioManager.RINGER_MODE_NORMAL,
                AudioManager.RINGER_MODE_VIBRATE,
                AudioManager.RINGER_MODE_SILENT
            )
            val nextMode = allModes[(allModes.indexOf(currentMode) + 1) % allModes.size]

            val nextIcon = when (nextMode) {
                AudioManager.RINGER_MODE_SILENT -> R.drawable.volume_off
                AudioManager.RINGER_MODE_VIBRATE -> R.drawable.mobile_vibrate
                else -> R.drawable.volume_up
            }

            tile.icon = Icon.createWithResource(this, nextIcon)
            tile.updateTile()
        }


        val serviceIntent = Intent(this, SoundChangeForegroundService::class.java)
        this.startForegroundService(serviceIntent)
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }
}



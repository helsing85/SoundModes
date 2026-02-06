package com.helsing.soundmodes

import android.app.PendingIntent
import android.graphics.drawable.Icon
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
        manager.changeSoundMode { intent ->
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        }
        refreshTile()
    }

    override fun onStartListening() {
        refreshTile()
    }
}



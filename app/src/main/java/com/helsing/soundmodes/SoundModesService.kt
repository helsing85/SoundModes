package com.helsing.soundmodes

import android.graphics.drawable.Icon
import android.media.AudioManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class SoundModesService : TileService() {


    private val audioManager by lazy {
        getSystemService(AUDIO_SERVICE) as AudioManager
    }

    private fun setIcon(): Int {
        val ringerMode = audioManager.ringerMode
        return when (ringerMode) {
            0 -> R.drawable.volume_off_50dp
            1 -> R.drawable.mobile_vibrate_50dp
            else -> R.drawable.volume_up_50dp
        }
    }

    fun updateTile() {
        val tile = qsTile ?: return
        val ringerMode = audioManager.ringerMode

        val tileLabel = when (ringerMode) {
            0 -> getString(R.string.silent)
            1 -> getString(R.string.vibrate)
            2 -> getString(R.string.normal)
            else -> getString(R.string.unknown)
        }

        tile.apply {
            contentDescription = tileLabel
            label = tileLabel
            icon = Icon.createWithResource(this@SoundModesService, setIcon())
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    fun clickTile() {
        val currentMode: Int = audioManager.ringerMode

        val nextMode: Int = getNextMode(currentMode)

        if (nextMode != currentMode) {
            if (nextMode == AudioManager.RINGER_MODE_VIBRATE) {
                //vibrate()
            }

            if (currentMode == AudioManager.RINGER_MODE_SILENT) {
                //toggleZenMode(getResources().getString(R.string.silent_mode), false)
            }

            if (nextMode == AudioManager.RINGER_MODE_SILENT) {
                //toggleZenMode(getResources().getString(R.string.silent_mode), true)
            } else {
                //audioManager.setRingerMode(nextMode)
            }
        }
        audioManager.setRingerMode(nextMode)
    }

    private fun getNextMode(
        currentMode: Int,
        excluded: Set<Int> = emptySet()
    ): Int {
        val allModes = listOf(
            AudioManager.RINGER_MODE_NORMAL,
            AudioManager.RINGER_MODE_VIBRATE,
            AudioManager.RINGER_MODE_SILENT
        )

        val currentIndex = allModes.indexOf(currentMode)
        val size = allModes.size

        for (i in 1..size) {
            val nextMode = allModes[(currentIndex + i) % size]
            if (nextMode !in excluded) {
                return nextMode
            }
        }
        return currentMode
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        clickTile()
        updateTile()
    }

}


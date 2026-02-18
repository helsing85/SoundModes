package com.helsing.soundmodes

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.service.quicksettings.TileService
import com.helsing.soundmodes.widgets.ToggleAllWidgetGlanceReceiver

class RingerModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == AudioManager.RINGER_MODE_CHANGED_ACTION ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {

            TileService.requestListeningState(
                context,
                ComponentName(context, SoundModesService::class.java)
            )

            ToggleAllWidgetGlanceReceiver.updateAllGlanceWidgets(context)
        }
    }
}
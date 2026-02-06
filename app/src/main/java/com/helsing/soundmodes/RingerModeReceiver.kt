package com.helsing.soundmodes

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.service.quicksettings.TileService
import com.helsing.soundmodes.widgets.ToggleAllWidgetClassic
import com.helsing.soundmodes.widgets.SoundModesGlanceWidgetReceiver

class RingerModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
            TileService.requestListeningState(
                context,
                ComponentName(context, SoundModesService::class.java)
            )

            ToggleAllWidgetClassic.updateAllClassicWidgets(context)

            SoundModesGlanceWidgetReceiver.updateAllGlanceWidgets(context)
        }
    }
}
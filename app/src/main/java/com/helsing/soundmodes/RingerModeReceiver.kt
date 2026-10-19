package com.helsing.soundmodes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.helsing.soundmodes.widgets.ToggleAllWidgetGlanceReceiver

class RingerModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {

            // Tile refreshes itself in onStartListening() every time Quick Settings are opened
            ToggleAllWidgetGlanceReceiver.updateAllGlanceWidgets(context)
        }
    }
}
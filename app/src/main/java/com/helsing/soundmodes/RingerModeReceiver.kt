package com.helsing.soundmodes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.helsing.soundmodes.widgets.ToggleAllWidgetGlanceReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RingerModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Tile refreshes itself in onStartListening() every time Quick Settings are opened.
            // goAsync() keeps the process alive until the widgets are refreshed.
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    ToggleAllWidgetGlanceReceiver.refreshAllGlanceWidgets(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

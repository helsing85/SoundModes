package com.helsing.soundmodes

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.helsing.soundmodes.widgets.ToggleAllWidgetGlanceReceiver

class SoundModesApp : Application() {

    // RINGER_MODE_CHANGED cannot be received by a manifest receiver, so the widget can only follow
    // changes made outside the app (volume buttons, system settings) while the process is alive.
    private val ringerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            ToggleAllWidgetGlanceReceiver.updateAllGlanceWidgets(context, onlyIfChanged = true)
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(
            ringerModeReceiver,
            IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION),
            RECEIVER_NOT_EXPORTED
        )
        // Catch up with changes made while the process was dead
        ToggleAllWidgetGlanceReceiver.updateAllGlanceWidgets(this, onlyIfChanged = true)
    }
}

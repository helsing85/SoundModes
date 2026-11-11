package com.helsing.soundmodes

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.helsing.soundmodes.widgets.ToggleAllWidgetGlanceReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SoundModesApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        // Generated widget picker preview, once per app version
        appScope.launch {
            ToggleAllWidgetGlanceReceiver.publishWidgetPreviews(this@SoundModesApp)
        }
    }
}

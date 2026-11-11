package com.helsing.soundmodes

import android.app.Activity
import com.helsing.soundmodes.widgets.ToggleAllWidgetGlanceReceiver
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Invisible activity used as a fallback when [SoundChangeForegroundService] can't be started.
 * Changes the sound mode while the activity is resumed (valid state for the audio framework)
 * and finishes immediately.
 */
class SoundChangeActivity : Activity() {
    private val activityScope = MainScope()
    private var isModeChanged = false

    override fun onResume() {
        super.onResume()
        if (isModeChanged) return
        isModeChanged = true

        // The activity stays resumed until the mode change is finished
        activityScope.launch {
            SoundModesManager(this@SoundChangeActivity).changeSoundMode { intent ->
                startActivity(intent)
            }
            ToggleAllWidgetGlanceReceiver.updateAllGlanceWidgets(applicationContext)
            finish()
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}

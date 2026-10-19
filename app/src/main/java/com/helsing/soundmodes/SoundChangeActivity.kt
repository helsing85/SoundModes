package com.helsing.soundmodes

import android.app.Activity
import com.helsing.soundmodes.widgets.ToggleAllWidgetGlanceReceiver

/**
 * Invisible activity used as a fallback when [SoundChangeForegroundService] can't be started.
 * Changes the sound mode while the activity is resumed (valid state for the audio framework)
 * and finishes immediately.
 */
class SoundChangeActivity : Activity() {
    private var isModeChanged = false

    override fun onResume() {
        super.onResume()
        if (!isModeChanged) {
            isModeChanged = true
            SoundModesManager(this).changeSoundMode { intent ->
                startActivity(intent)
            }
            ToggleAllWidgetGlanceReceiver.updateAllGlanceWidgets(applicationContext)
        }
        finish()
    }
}

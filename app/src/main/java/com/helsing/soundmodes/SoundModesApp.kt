package com.helsing.soundmodes

import android.app.Application
import android.content.IntentFilter
import android.media.AudioManager

class SoundModesApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        registerReceiver(RingerModeReceiver(), filter)
    }
}
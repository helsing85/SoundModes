package com.helsing.soundmodes

import android.media.AudioManager.RINGER_MODE_NORMAL
import android.media.AudioManager.RINGER_MODE_SILENT
import android.media.AudioManager.RINGER_MODE_VIBRATE
import org.junit.Assert.assertEquals
import org.junit.Test

class NextRingerModeTest {
    @Test
    fun cyclesNormalVibrateSilent() {
        assertEquals(RINGER_MODE_VIBRATE, nextRingerMode(RINGER_MODE_NORMAL))
        assertEquals(RINGER_MODE_SILENT, nextRingerMode(RINGER_MODE_VIBRATE))
        assertEquals(RINGER_MODE_NORMAL, nextRingerMode(RINGER_MODE_SILENT))
    }

    @Test
    fun skipsExcludedMode() {
        val excluded = setOf(RINGER_MODE_VIBRATE)
        assertEquals(RINGER_MODE_SILENT, nextRingerMode(RINGER_MODE_NORMAL, excluded))
        assertEquals(RINGER_MODE_NORMAL, nextRingerMode(RINGER_MODE_SILENT, excluded))
    }

    @Test
    fun keepsCurrentModeWhenAllOthersExcluded() {
        val excluded = setOf(RINGER_MODE_VIBRATE, RINGER_MODE_SILENT)
        assertEquals(RINGER_MODE_NORMAL, nextRingerMode(RINGER_MODE_NORMAL, excluded))
    }

    @Test
    fun unknownModeStartsFromNormal() {
        assertEquals(RINGER_MODE_NORMAL, nextRingerMode(-1))
    }
}

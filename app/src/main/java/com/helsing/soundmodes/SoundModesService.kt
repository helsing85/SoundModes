package com.helsing.soundmodes

import android.app.AutomaticZenRule
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.app.NotificationManager
import android.content.ComponentName
import androidx.core.net.toUri
import android.service.notification.Condition

class SoundModesService : TileService() {


    private val audioManager by lazy {
        getSystemService(AUDIO_SERVICE) as AudioManager
    }

    private fun vibrate() {
        val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator

        val effect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)

        vibrator.vibrate(
            effect,
            VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_NOTIFICATION)
                .build()
        )
    }

    private fun setIcon(): Int {
        val ringerMode = audioManager.ringerMode
        return when (ringerMode) {
            0 -> R.drawable.volume_off
            1 -> R.drawable.mobile_vibrate
            else -> R.drawable.volume_up
        }
    }

    private fun getOrCreateZenRuleID(ruleName: String): String? {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Check existing ZenRules
        val existingRules = notificationManager.automaticZenRules
        val existingId = existingRules.entries.firstOrNull { it.value.name == ruleName }?.key
        if (existingId != null) return existingId


        val iconResId = R.drawable.volume_off
        val conditionUri = resources.getString(R.string.zen_rule_condition_uri).toUri()

        // ComponentName (MainActivity is my starting class)
        val configActivity = ComponentName(packageName, MainActivity::class.java.name)

        // ZenRule builder (Android 10+)
        val newRule = AutomaticZenRule.Builder(ruleName, conditionUri)
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            // .setZenPolicy(zenPolicy)  // ZenPolicy is not needed when INTERRUPTION_FILTER_ALARMS
            .setIconResId(iconResId)
            .setTriggerDescription(resources.getString(R.string.silent_mode_description))
            .setConfigurationActivity(configActivity)
            .setEnabled(true)
            .build()

        // Register the rule and return ruleID
        return notificationManager.addAutomaticZenRule(newRule)
    }

    fun toggleZenMode(ruleName: String, turnOn: Boolean) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val ruleId = getOrCreateZenRuleID(ruleName) ?: return
        val conditionUri = resources.getString(R.string.zen_rule_condition_uri).toUri()

        val summary = resources.getString(
            if (turnOn) R.string.silent_mode_active else R.string.silent_mode_inactive
        )
        val state = if (turnOn) Condition.STATE_TRUE else Condition.STATE_FALSE

        // Before activation force deactivation
        // If ZenRule is deactivated by switching to different Ringer Mode using other methods
        // it cannot be reactivated again
        if (turnOn) {
            notificationManager.setAutomaticZenRuleState(
                ruleId,
                Condition(conditionUri, "", Condition.STATE_FALSE)
            )
        }

        // Change state of the ZenRule
        notificationManager.setAutomaticZenRuleState(
            ruleId,
            Condition(conditionUri, summary, state)
        )
    }

    fun updateTile() {
        val tile = qsTile ?: return
        val ringerMode = audioManager.ringerMode

        val tileLabel = when (ringerMode) {
            0 -> getString(R.string.silent)
            1 -> getString(R.string.vibrate)
            2 -> getString(R.string.normal)
            else -> getString(R.string.unknown)
        }

        tile.apply {
            contentDescription = tileLabel
            label = tileLabel
            icon = Icon.createWithResource(this@SoundModesService, setIcon())
            state = Tile.STATE_ACTIVE
            updateTile()
        }
    }

    fun changeSoundMode() {
        val currentMode: Int = audioManager.ringerMode

        val nextMode: Int = getNextMode(currentMode)

        if (nextMode != currentMode) {
            if (nextMode == AudioManager.RINGER_MODE_VIBRATE) {
                vibrate()
            }

            if (currentMode == AudioManager.RINGER_MODE_SILENT) {
                toggleZenMode(resources.getString(R.string.silent_mode), false)
            }

            if (nextMode == AudioManager.RINGER_MODE_SILENT) {
                toggleZenMode(resources.getString(R.string.silent_mode), true)
            } else {
                audioManager.setRingerMode(nextMode)
            }
        }
    }

    private fun getNextMode(
        currentMode: Int,
        excluded: Set<Int> = emptySet()
    ): Int {
        val allModes = listOf(
            AudioManager.RINGER_MODE_NORMAL,
            AudioManager.RINGER_MODE_VIBRATE,
            AudioManager.RINGER_MODE_SILENT
        )

        val currentIndex = allModes.indexOf(currentMode)
        val size = allModes.size

        for (i in 1..size) {
            val nextMode = allModes[(currentIndex + i) % size]
            if (nextMode !in excluded) {
                return nextMode
            }
        }
        return currentMode
    }


    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        changeSoundMode()
        updateTile()
    }

}


package com.helsing.soundmodes

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import android.service.notification.Condition
import androidx.core.net.toUri

/**
 * Returns the ringer mode which follows [currentMode] in the cycle Normal -> Vibrate -> Silent,
 * skipping [excluded] modes.
 */
internal fun nextRingerMode(currentMode: Int, excluded: Set<Int> = emptySet()): Int {
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

class SoundModesManager(private val context: Context) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val conditionUri = context.getString(R.string.zen_rule_condition_uri).toUri()

    private fun vibrate() {
        val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
        val effect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)

        vibrator.vibrate(
            effect,
            VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_NOTIFICATION)
                .build()
        )
    }

    private fun getOrCreateZenRuleID(): String? {
        // The rule is identified by its condition URI, because its name is translated
        // and changes together with the device language
        val existingIds = notificationManager.automaticZenRules.entries
            .filter { it.value.conditionId == conditionUri }
            .map { it.key }

        if (existingIds.isNotEmpty()) {
            // Older versions found the rule by name and could create duplicates
            // after a language change
            existingIds.drop(1).forEach { notificationManager.removeAutomaticZenRule(it) }
            return existingIds.first()
        }

        val ruleName = context.getString(R.string.silent_mode)
        val iconResId = R.drawable.volume_off
        val description = context.getString(R.string.silent_mode_description)
        val configActivity = ComponentName(context.packageName, MainActivity::class.java.name)

        val newRule = AutomaticZenRule.Builder(ruleName, conditionUri)
            .setType(AutomaticZenRule.TYPE_OTHER)
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            // .setZenPolicy(zenPolicy)  // ZenPolicy is not needed when INTERRUPTION_FILTER_ALARMS
            .setIconResId(iconResId)
            .setTriggerDescription(description)
            .setConfigurationActivity(configActivity)
            .setManualInvocationAllowed(true)
            .setEnabled(true)
            .build()

        return notificationManager.addAutomaticZenRule(newRule)
    }

    private fun toggleZenMode(turnOn: Boolean) {
        val ruleId = getOrCreateZenRuleID() ?: return
        val summary = context.getString(
            if (turnOn) R.string.silent_mode_active else R.string.silent_mode_inactive
        )
        val state = if (turnOn) Condition.STATE_TRUE else Condition.STATE_FALSE

        if (turnOn) {
            notificationManager.setAutomaticZenRuleState(
                ruleId,
                Condition(conditionUri, "", Condition.STATE_FALSE)
            )
        }
        notificationManager.setAutomaticZenRuleState(
            ruleId,
            Condition(conditionUri, summary, state)
        )
    }

    fun changeSoundMode(onPermissionRequired: (Intent) -> Unit) {
        if (!isNotificationPolicyAccessPermissionEnabled()) {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(context.getString(R.string.toast_name_permission), true)
            }

            onPermissionRequired(intent)
            return
        }

        val currentMode = audioManager.ringerMode
        val nextMode = nextRingerMode(currentMode)

        if (nextMode != currentMode) {
            if (nextMode == AudioManager.RINGER_MODE_VIBRATE) {
                vibrate()
            }

            if (currentMode == AudioManager.RINGER_MODE_SILENT) {
                toggleZenMode(false)
            }

            if (nextMode == AudioManager.RINGER_MODE_SILENT) {
                toggleZenMode(true)
            } else {
                audioManager.ringerMode = nextMode
            }
        }
    }

    fun getRingerMode(): Int = audioManager.ringerMode

    fun getModeIcon(mode: Int = audioManager.ringerMode): Int {
        return when (mode) {
            AudioManager.RINGER_MODE_SILENT -> R.drawable.volume_off
            AudioManager.RINGER_MODE_VIBRATE -> R.drawable.mobile_vibrate
            else -> R.drawable.volume_up
        }
    }

    fun getModeLabel(): String {
        return context.getString(
            when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> R.string.silent
                AudioManager.RINGER_MODE_VIBRATE -> R.string.vibrate
                AudioManager.RINGER_MODE_NORMAL -> R.string.normal
                else -> R.string.unknown
            }
        )
    }

    fun isNotificationPolicyAccessPermissionEnabled(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

}

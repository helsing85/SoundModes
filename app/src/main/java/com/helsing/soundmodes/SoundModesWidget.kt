package com.helsing.soundmodes

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Intent
import android.media.AudioManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import android.service.notification.Condition
import android.widget.RemoteViews
import androidx.core.net.toUri

class SoundModesWidget : AppWidgetProvider() {

    private fun vibrate(context: Context) {
        val vibratorManager = context.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator

        val effect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)

        vibrator.vibrate(
            effect,
            VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_NOTIFICATION)
                .build()
        )
    }


    private fun getOrCreateZenRuleID(context: Context, ruleName: String): String? {
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Check existing ZenRules
        val existingRules = notificationManager.automaticZenRules
        val existingId = existingRules.entries.firstOrNull { it.value.name == ruleName }?.key
        if (existingId != null) return existingId


        val iconResId = R.drawable.volume_off
        val conditionUri = context.getString(R.string.zen_rule_condition_uri).toUri()

        // ComponentName (MainActivity is my starting class)
        val configActivity = ComponentName(context.packageName, MainActivity::class.java.name)

        // ZenRule builder (Android 10+)
        val newRule = AutomaticZenRule.Builder(ruleName, conditionUri)
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            // .setZenPolicy(zenPolicy)  // ZenPolicy is not needed when INTERRUPTION_FILTER_ALARMS
            .setIconResId(iconResId)
            .setTriggerDescription(context.getString(R.string.silent_mode_description))
            .setConfigurationActivity(configActivity)
            .setEnabled(true)
            .build()

        // Register the rule and return ruleID
        return notificationManager.addAutomaticZenRule(newRule)
    }

    private fun toggleZenMode(context: Context, ruleName: String, turnOn: Boolean) {
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val ruleId = getOrCreateZenRuleID(context, ruleName) ?: return
        val conditionUri = context.getString(R.string.zen_rule_condition_uri).toUri()

        val summary = context.getString(
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

    private fun changeSoundMode(context: Context) {
        if (!isNotificationPolicyAccessPermissionEnabled(context)) {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(context.getString(R.string.toast_name_permission), true)
            }
            context.startActivity(intent)
            return
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.ringerMode
        val nextMode = getNextMode(currentMode)


        if (nextMode != currentMode) {
            if (nextMode == AudioManager.RINGER_MODE_VIBRATE) {
                vibrate(context)
            }

            if (currentMode == AudioManager.RINGER_MODE_SILENT) {
                toggleZenMode(context, context.getString(R.string.silent_mode), false)
            }

            if (nextMode == AudioManager.RINGER_MODE_SILENT) {
                toggleZenMode(context, context.resources.getString(R.string.silent_mode), true)
            } else {
                audioManager.setRingerMode(nextMode)
            }
        }
    }

    companion object {
        const val WIDGET_TOGGLE_MODE = "com.helsing.soundmodes.WIDGET_TOGGLE_MODE"

        private fun setIcon(context: Context): Int {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val ringerMode = audioManager.ringerMode
            return when (ringerMode) {
                0 -> R.drawable.volume_off
                1 -> R.drawable.mobile_vibrate
                else -> R.drawable.volume_up
            }
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SoundModesWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val iconRes = setIcon(context)
            val views = RemoteViews(context.packageName, R.layout.sound_modes_widget_layout)
            views.setImageViewResource(R.id.widget_button, iconRes)

            val intent = Intent(context, SoundModesWidget::class.java).apply {
                action = WIDGET_TOGGLE_MODE
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WIDGET_TOGGLE_MODE) {
            changeSoundMode(context)
            updateAllWidgets(context)
        }
    }
}

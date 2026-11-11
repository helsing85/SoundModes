package com.helsing.soundmodes.widgets

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.helsing.soundmodes.SoundModesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ToggleAllWidgetGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ToggleAllWidgetGlance()

    companion object {
        /**
         * Publishes the generated widget picker preview once per app version.
         * The system rate-limits this call, so it is not repeated on every process start.
         */
        suspend fun publishWidgetPreviews(context: Context) {
            val soundModesManager = SoundModesManager(context)
            val versionCode = context.packageManager
                .getPackageInfo(context.packageName, 0).longVersionCode
            if (soundModesManager.getWidgetPreviewVersion() == versionCode) return

            val result = GlanceAppWidgetManager(context)
                .setWidgetPreviews(ToggleAllWidgetGlanceReceiver::class)
            if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
                soundModesManager.setWidgetPreviewVersion(versionCode)
            } else {
                Log.w(TAG, "Widget preview not published, result $result")
            }
        }

        private const val TAG = "SoundModesWidget"

        /**
         * Fire-and-forget version of [refreshAllGlanceWidgets].
         */
        fun updateAllGlanceWidgets(context: Context, onlyIfChanged: Boolean = false) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.Main).launch {
                refreshAllGlanceWidgets(appContext, onlyIfChanged)
            }
        }

        /**
         * Stores the current ringer mode in the state of every widget and redraws it.
         *
         * @param onlyIfChanged skip widgets which already show the current ringer mode.
         * Used by hooks which run often (process start, Quick Settings opened).
         */
        suspend fun refreshAllGlanceWidgets(context: Context, onlyIfChanged: Boolean = false) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(ToggleAllWidgetGlance::class.java)
            val ringerMode = SoundModesManager(context).getRingerMode()

            glanceIds.forEach { glanceId ->
                if (onlyIfChanged) {
                    val state = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
                    if (state[ToggleAllWidgetGlance.RINGER_MODE_KEY] == ringerMode) return@forEach
                }
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[ToggleAllWidgetGlance.RINGER_MODE_KEY] = ringerMode
                }
                ToggleAllWidgetGlance().update(context, glanceId)
            }
        }
    }
}

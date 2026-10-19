package com.helsing.soundmodes.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import com.helsing.soundmodes.SoundModesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ToggleAllWidgetGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ToggleAllWidgetGlance()

    companion object {
        fun updateAllGlanceWidgets(context: Context) {
            val scope = CoroutineScope(Dispatchers.Main)
            scope.launch {
                refreshAllGlanceWidgets(context)
            }
        }

        suspend fun refreshAllGlanceWidgets(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(ToggleAllWidgetGlance::class.java)
            val ringerMode = SoundModesManager(context).getRingerMode()

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[ToggleAllWidgetGlance.RINGER_MODE_KEY] = ringerMode
                }
                ToggleAllWidgetGlance().update(context, glanceId)
            }
        }
    }
}

package com.helsing.soundmodes.widgets

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ToggleAllWidgetGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ToggleAllWidgetGlance()

    companion object {
        fun updateAllGlanceWidgets(context: Context) {
            val scope = CoroutineScope(Dispatchers.Main)
            scope.launch {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(ToggleAllWidgetGlance::class.java)

                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[longPreferencesKey("now")] = System.currentTimeMillis()
                    }
                    ToggleAllWidgetGlance().update(context, glanceId)
                }
            }
        }
    }
}

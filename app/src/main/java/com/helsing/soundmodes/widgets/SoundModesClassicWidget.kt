package com.helsing.soundmodes.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.helsing.soundmodes.R
import com.helsing.soundmodes.SoundModesManager

class SoundModesClassicWidget : AppWidgetProvider() {

    companion object {

        // The same action name must be present in AndroidManifest.xml for this receiver
        const val WIDGET_TOGGLE_MODE = "com.helsing.soundmodes.widgets.TOGGLE_ALL_MODES"

        fun updateAllClassicWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SoundModesClassicWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            for (appWidgetId in appWidgetIds) {
                updateClassicWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateClassicWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val iconRes = SoundModesManager(context).getModeIcon()
            val views = RemoteViews(context.packageName, R.layout.sound_modes_classic_widget_layout)
            views.setImageViewResource(R.id.widget_button, iconRes)

            val intent = Intent(context, SoundModesClassicWidget::class.java).apply {
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
            updateClassicWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WIDGET_TOGGLE_MODE) {
            SoundModesManager(context).changeSoundMode { intent ->
                context.startActivity(intent)
            }
        }
    }
}

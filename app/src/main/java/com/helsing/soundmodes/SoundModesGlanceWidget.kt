package com.helsing.soundmodes

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition

class SoundModesGlanceWidget : GlanceAppWidget() {
    override var stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val iconRes = SoundModesManager(context).getModeIcon()
            currentState<Preferences>()

            MyWidgetContent(iconRes)
        }
    }

    @Composable
    private fun MyWidgetContent(iconRes: Int) {
        val context = LocalContext.current
        val description = context.getString(R.string.app_name)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
                .clickable(actionRunCallback<ToggleModeAction>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = description,
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}

class ToggleModeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        SoundModesManager(context).changeSoundMode { intent ->
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

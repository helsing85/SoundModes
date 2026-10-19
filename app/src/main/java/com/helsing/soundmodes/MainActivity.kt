package com.helsing.soundmodes

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.helsing.soundmodes.ui.theme.SoundModesTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppMainView()
        }
    }
}

@Preview(
    showBackground = true, device = "id:pixel_8_pro", showSystemUi = true
)
@Composable
fun MainAppPreview() {
    AppMainView()

}

@Composable
fun AppMainView() {
    SoundModesTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AppBar()
            }
        ) { contentPadding ->
            MainColumn(
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val githubRepoUrl = stringResource(R.string.github_repo_link)

    var secretClickCount by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        secretClickCount = 0
    }

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = stringResource(R.string.app_logo_description),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            secretClickCount++
                            if (secretClickCount >= 7) {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                secretClickCount = 0
                            }
                        }
                )
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier
                        .padding(start = 4.dp),
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    secretClickCount++

                    if (secretClickCount >= 7) {
                        secretClickCount = 0
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    } else {
                        uriHandler.openUri(githubRepoUrl)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.github_logo),
                    contentDescription = stringResource(R.string.github_logo_description),
                )
            }
        }
    )
}

@Composable
fun MainColumn(modifier: Modifier = Modifier) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val manager by remember(context) { lazy { SoundModesManager(context) } }

    var showDialog by remember { mutableStateOf(false) }

    var isNotificationPolicyAccessGranted by remember {
        mutableStateOf(isPreview || manager.isNotificationPolicyAccessPermissionEnabled())
    }

    if (!isPreview) {
        val key = stringResource(R.string.toast_name_permission)
        val activity = context as? ComponentActivity
        LaunchedEffect(context) {
            activity?.intent?.let { intent ->
                if (intent.getBooleanExtra(key, false)) {
                    Toast.makeText(
                        context,
                        R.string.toast_notification_policy_access,
                        Toast.LENGTH_LONG
                    ).show()

                    // To make sure Toast is displayed only one it must be removed
                    intent.removeExtra(key)
                }
            }
        }

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            isNotificationPolicyAccessGranted =
                manager.isNotificationPolicyAccessPermissionEnabled()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            )
            {
                Text(
                    text = stringResource(R.string.app_description),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.required_permission),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.required_permission_description),
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.notification_policy_access),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { showDialog = true },
                        enabled = !isNotificationPolicyAccessGranted,
                    ) {
                        Text(
                            text = if (isNotificationPolicyAccessGranted) {
                                stringResource(R.string.enabled)
                            } else {
                                stringResource(R.string.enable)
                            }
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.add_tile_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.add_tile_description),
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(
                    onClick = { requestAddTile(context) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.add_tile))
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.notification_policy_access_permission_title)) },
                text = { Text(stringResource(R.string.notification_policy_access_permission_description)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        }
                    ) {
                        Text(stringResource(R.string.agree))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDialog = false }
                    ) {
                        Text(stringResource(R.string.not_now))
                    }
                }
            )
        }

    }
}

/**
 * Shows the system dialog which adds the Sound Modes tile to Quick Settings.
 */
private fun requestAddTile(context: Context) {
    val statusBarManager = context.getSystemService(StatusBarManager::class.java)
    statusBarManager.requestAddTileService(
        ComponentName(context, SoundModesService::class.java),
        context.getString(R.string.app_name),
        Icon.createWithResource(context, R.drawable.volume_up),
        context.mainExecutor
    ) { result ->
        if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
            Toast.makeText(context, R.string.tile_already_added, Toast.LENGTH_SHORT).show()
        }
    }
}

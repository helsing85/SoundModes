package com.helsing.soundmodes

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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


@Composable
private fun AppBar(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val githubRepoUrl = stringResource(R.string.github_repo_link)

    var secretClickCount = 0

    DisposableEffect(context) {
        val lifecycleOwner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                secretClickCount = 0
            }
        }

        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(top = 40.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    secretClickCount++

                    if (secretClickCount >= 7) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        secretClickCount = 0
                    }
                },
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineLarge,

            )

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
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun MainColumn(modifier: Modifier = Modifier) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val manager by lazy { SoundModesManager(context) }
    val activity = context as? ComponentActivity

    var showDialog by remember { mutableStateOf(false) }

    var isNotificationPolicyAccessGranted by remember {
        if (isPreview) {
            mutableStateOf(true)
        } else {
            mutableStateOf(manager.isNotificationPolicyAccessPermissionEnabled(context))
        }
    }

    if (!isPreview) {
        val key = stringResource(R.string.toast_name_permission)
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

        DisposableEffect(context) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isNotificationPolicyAccessGranted =
                        manager.isNotificationPolicyAccessPermissionEnabled(context)
                }
            }
            activity?.lifecycle?.addObserver(observer)
            onDispose {
                activity?.lifecycle?.removeObserver(observer)
            }
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


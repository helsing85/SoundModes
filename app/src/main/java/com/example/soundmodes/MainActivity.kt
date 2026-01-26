package com.example.soundmodes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.soundmodes.ui.theme.SoundModesTheme



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppMainView()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainView() {
    SoundModesTheme {
        Scaffold(modifier = Modifier.fillMaxSize(),
            topBar = {
                AppBar()
            }
        ) { contentPadding ->
            MainColumn(
                name = "Android",
                modifier = Modifier.padding(contentPadding)
            )
        }
    }
}


@Composable
private fun AppBar(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}


@Composable
fun MainColumn(name: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Card(modifier = Modifier
            .fillMaxWidth()) {
            Text(
                text = "Hello $name!",
                modifier = modifier
            )
        }
        Card(modifier = Modifier
            .fillMaxWidth()) {
            Text(
                text = "Hello $name!",
                modifier = modifier
            )
        }
    }
}


@Preview(showBackground = true, device = "id:pixel_8_pro", showSystemUi = true)
@Composable
fun MainAppPreview() {
    AppMainView()

}
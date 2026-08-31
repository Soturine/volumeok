@file:Suppress("FunctionName", "UnusedPrivateMember", "ktlint:standard:function-naming")

package com.soturine.volumeok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soturine.volumeok.ui.theme.VolumeOkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VolumeOkTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FoundationScreen()
                }
            }
        }
    }
}

@Composable
private fun FoundationScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(text = stringResource(R.string.foundation_status))
        Button(onClick = {}) {
            Text(text = stringResource(R.string.refresh))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FoundationScreenPreview() {
    VolumeOkTheme {
        FoundationScreen()
    }
}

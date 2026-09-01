package com.soturine.volumeok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.soturine.volumeok.feature.home.HomeEvent
import com.soturine.volumeok.feature.home.HomeScreen
import com.soturine.volumeok.feature.home.HomeViewModel
import com.soturine.volumeok.feature.safetest.SafeTestScreen
import com.soturine.volumeok.feature.safetest.SafeTestViewModel
import com.soturine.volumeok.platform.audio.AndroidSafeTonePlayer
import com.soturine.volumeok.platform.audio.AndroidSoundStateAdapter
import com.soturine.volumeok.platform.audio.SoundSettingsObserver
import com.soturine.volumeok.ui.theme.VolumeOkTheme

class MainActivity : ComponentActivity() {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var safeTestViewModel: SafeTestViewModel
    private lateinit var settingsObserver: SoundSettingsObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gateway = AndroidSoundStateAdapter(applicationContext)
        homeViewModel = ViewModelProvider(this, HomeViewModel.Factory(gateway))[HomeViewModel::class.java]
        safeTestViewModel = ViewModelProvider(
            this,
            SafeTestViewModel.Factory(gateway, AndroidSafeTonePlayer(applicationContext))
        )[SafeTestViewModel::class.java]
        settingsObserver = SoundSettingsObserver(contentResolver) {
            homeViewModel.onEvent(HomeEvent.ObservedSettingsChanged)
        }

        setContent {
            VolumeOkTheme {
                var showingSafeTest by rememberSaveable { mutableStateOf(false) }
                if (showingSafeTest) {
                    SafeTestScreen(
                        state = safeTestViewModel.state,
                        onEvent = safeTestViewModel::onEvent,
                        onClose = { showingSafeTest = false }
                    )
                } else {
                    HomeScreen(
                        state = homeViewModel.state,
                        onEvent = homeViewModel::onEvent,
                        onOpenSafeTest = { showingSafeTest = true }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        settingsObserver.start()
        homeViewModel.onEvent(HomeEvent.Refresh)
    }

    override fun onStop() {
        settingsObserver.stop()
        safeTestViewModel.onLifecycleInterrupted()
        super.onStop()
    }
}

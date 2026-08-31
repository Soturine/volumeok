package com.soturine.volumeok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.soturine.volumeok.feature.home.HomeEvent
import com.soturine.volumeok.feature.home.HomeScreen
import com.soturine.volumeok.feature.home.HomeViewModel
import com.soturine.volumeok.platform.audio.AndroidSoundStateAdapter
import com.soturine.volumeok.platform.audio.SoundSettingsObserver
import com.soturine.volumeok.ui.theme.VolumeOkTheme

class MainActivity : ComponentActivity() {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var settingsObserver: SoundSettingsObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gateway = AndroidSoundStateAdapter(applicationContext)
        homeViewModel = ViewModelProvider(this, HomeViewModel.Factory(gateway))[HomeViewModel::class.java]
        settingsObserver = SoundSettingsObserver(contentResolver) {
            homeViewModel.onEvent(HomeEvent.Refresh)
        }

        setContent {
            VolumeOkTheme {
                HomeScreen(state = homeViewModel.state, onEvent = homeViewModel::onEvent)
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
        super.onStop()
    }
}

package com.soturine.volumeok.feature.safetest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.soturine.volumeok.application.RingVolumeGateway
import com.soturine.volumeok.application.SafeSoundTestSession
import com.soturine.volumeok.application.SafeTonePlayer
import com.soturine.volumeok.domain.Reading
import com.soturine.volumeok.domain.RingVolume
import com.soturine.volumeok.domain.SafeSoundTestState

sealed interface SafeTestEvent {
    data object Start : SafeTestEvent
    data object TryAgain : SafeTestEvent
    data object Heard : SafeTestEvent
    data object NotHeard : SafeTestEvent
    data object Stop : SafeTestEvent
    data object Reset : SafeTestEvent
}

class SafeTestViewModel(private val gateway: RingVolumeGateway, private val player: SafeTonePlayer) : ViewModel() {
    var state by mutableStateOf<SafeSoundTestState>(SafeSoundTestState.Explaining)
        private set

    private var session = newSession()

    fun onEvent(event: SafeTestEvent) {
        when (event) {
            SafeTestEvent.Start -> start()
            SafeTestEvent.TryAgain -> session.tryAgain()
            SafeTestEvent.Heard -> session.heard()
            SafeTestEvent.NotHeard -> session.notHeard()
            SafeTestEvent.Stop -> session.stop()
            SafeTestEvent.Reset -> reset()
        }
    }

    fun onLifecycleInterrupted() {
        session.onLifecycleInterrupted()
    }

    override fun onCleared() {
        session.stop()
    }

    private fun start() {
        val volume = (gateway.read() as? Reading.Available)?.value ?: RingVolume(0, 0)
        session.start(volume)
    }

    private fun reset() {
        session.stop()
        state = SafeSoundTestState.Explaining
        session = newSession()
    }

    private fun newSession() = SafeSoundTestSession(player) { state = it }

    class Factory(private val gateway: RingVolumeGateway, private val player: SafeTonePlayer) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SafeTestViewModel(gateway, player) as T
    }
}

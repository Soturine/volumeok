package com.soturine.volumeok.platform.audio

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings

class SoundSettingsObserver(
    private val contentResolver: ContentResolver,
    private val onPossibleSoundChange: () -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {
    private var registered = false

    fun start() {
        if (registered) return
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, this)
        registered = true
    }

    fun stop() {
        if (!registered) return
        contentResolver.unregisterContentObserver(this)
        registered = false
    }

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        onPossibleSoundChange()
    }
}

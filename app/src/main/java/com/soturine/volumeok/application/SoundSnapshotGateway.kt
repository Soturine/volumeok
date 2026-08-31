package com.soturine.volumeok.application

import com.soturine.volumeok.domain.SoundSnapshot

interface SoundSnapshotGateway : RingVolumeGateway {
    fun readSnapshot(): SoundSnapshot
}

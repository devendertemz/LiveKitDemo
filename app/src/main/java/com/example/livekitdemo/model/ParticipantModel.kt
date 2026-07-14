package com.example.livekitdemo.model

import io.livekit.android.room.track.VideoTrack

data class ParticipantModel(
    val sid: String,
    val identity: String,
    val displayName: String,
    val isLocal: Boolean,
    val videoTrack: VideoTrack? = null,
    val isMicEnabled: Boolean = false,
    val isCameraEnabled: Boolean = false,
    val isSpeaking: Boolean = false,
)

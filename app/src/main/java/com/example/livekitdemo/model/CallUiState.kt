package com.example.livekitdemo.model

import io.livekit.android.room.Room

data class CallUiState(
    val connectionState: Room.State = Room.State.DISCONNECTED,
    val roomName: String = "",
    val participants: List<ParticipantModel> = emptyList(),
    val isMicEnabled: Boolean = true,
    val isCameraEnabled: Boolean = true,
    val isSpeakerEnabled: Boolean = false,
    val errorMessage: String? = null,
)

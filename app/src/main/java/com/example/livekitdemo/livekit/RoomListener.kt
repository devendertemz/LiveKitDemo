package com.example.livekitdemo.livekit

import com.example.livekitdemo.model.ParticipantModel
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack

/**
 * Translates the current [Room] snapshot into the flat list of [ParticipantModel]s
 * the UI renders. Called after every [io.livekit.android.events.RoomEvent] so the
 * list always reflects the latest participant/track state.
 */
object RoomListener {

    fun snapshotParticipants(room: Room): List<ParticipantModel> {
        val remote = room.remoteParticipants.values.sortedBy { it.identity?.value.orEmpty() }
        return (listOf(room.localParticipant) + remote).map { it.toModel() }
    }

    private fun Participant.toModel(): ParticipantModel {
        val videoTrack = getTrackPublication(Track.Source.CAMERA)?.track as? VideoTrack
        val identityValue = identity?.value.orEmpty()
        return ParticipantModel(
            sid = sid.value,
            identity = identityValue,
            displayName = name?.takeIf { it.isNotBlank() } ?: identityValue,
            isLocal = this is LocalParticipant,
            videoTrack = videoTrack,
            isMicEnabled = isMicrophoneEnabled,
            isCameraEnabled = isCameraEnabled,
            isSpeaking = isSpeaking,
        )
    }
}

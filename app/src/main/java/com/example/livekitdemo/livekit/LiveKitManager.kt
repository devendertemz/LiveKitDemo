package com.example.livekitdemo.livekit

import android.content.Context
import com.example.livekitdemo.model.ParticipantModel
import com.twilio.audioswitch.AudioDevice
import io.livekit.android.LiveKit
import io.livekit.android.audio.AudioSwitchHandler
import io.livekit.android.events.collect
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.util.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveKitManager(context: Context) {

    private val room: Room = LiveKit.create(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _connectionState = MutableStateFlow(Room.State.DISCONNECTED)
    val connectionState: StateFlow<Room.State> = _connectionState.asStateFlow()

    private val _participants = MutableStateFlow<List<ParticipantModel>>(emptyList())
    val participants: StateFlow<List<ParticipantModel>> = _participants.asStateFlow()

    init {
        scope.launch {
            room::state.flow.collect { state -> _connectionState.value = state }
        }
        scope.launch {
            room.events.collect { _participants.value = RoomListener.snapshotParticipants(room) }
        }
    }

    fun initRenderer(renderer: SurfaceViewRenderer) {
        room.initVideoRenderer(renderer)
    }

    suspend fun connect(url: String, token: String) {
        room.connect(url, token)
        room.localParticipant.setMicrophoneEnabled(true)
        room.localParticipant.setCameraEnabled(true)
        _participants.value = RoomListener.snapshotParticipants(room)
    }

    suspend fun setMicEnabled(enabled: Boolean) {
        room.localParticipant.setMicrophoneEnabled(enabled)
    }

    suspend fun setCameraEnabled(enabled: Boolean) {
        room.localParticipant.setCameraEnabled(enabled)
    }

    fun switchCamera() {
        val videoTrack = room.localParticipant
            .getTrackPublication(Track.Source.CAMERA)?.track as? LocalVideoTrack
            ?: return
        val newPosition = when (videoTrack.options.position) {
            CameraPosition.FRONT -> CameraPosition.BACK
            CameraPosition.BACK -> CameraPosition.FRONT
            else -> null
        }
        videoTrack.switchCamera(position = newPosition)
    }

    fun setSpeakerEnabled(enabled: Boolean) {
        val audioHandler = room.audioHandler as? AudioSwitchHandler ?: return
        val target = if (enabled) {
            audioHandler.availableAudioDevices.filterIsInstance<AudioDevice.Speakerphone>().firstOrNull()
        } else {
            audioHandler.availableAudioDevices.firstOrNull { it !is AudioDevice.Speakerphone }
        }
        audioHandler.selectDevice(target)
    }

    fun disconnect() {
        room.disconnect()
    }

    fun release() {
        room.release()
        scope.cancel()
    }
}

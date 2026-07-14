package com.example.livekitdemo.repository

import android.content.Context
import com.example.livekitdemo.livekit.LiveKitManager
import com.example.livekitdemo.model.ParticipantModel
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.Room
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin seam between [com.example.livekitdemo.viewmodel.CallViewModel] and [LiveKitManager],
 * so the ViewModel never depends on concrete LiveKit SDK types directly.
 */
class LiveKitRepository(context: Context) {

    private val manager = LiveKitManager(context)

    val connectionState: StateFlow<Room.State> = manager.connectionState
    val participants: StateFlow<List<ParticipantModel>> = manager.participants

    fun initRenderer(renderer: SurfaceViewRenderer) = manager.initRenderer(renderer)

    suspend fun connect(url: String, token: String) = manager.connect(url, token)

    suspend fun setMicEnabled(enabled: Boolean) = manager.setMicEnabled(enabled)

    suspend fun setCameraEnabled(enabled: Boolean) = manager.setCameraEnabled(enabled)

    fun switchCamera() = manager.switchCamera()

    fun setSpeakerEnabled(enabled: Boolean) = manager.setSpeakerEnabled(enabled)

    fun disconnect() = manager.disconnect()

    fun release() = manager.release()
}

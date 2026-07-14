package com.example.livekitdemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.livekitdemo.model.CallUiState
import com.example.livekitdemo.repository.LiveKitRepository
import io.livekit.android.renderer.SurfaceViewRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LiveKitRepository(application)

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
        viewModelScope.launch {
            repository.participants.collect { participants ->
                _uiState.update { it.copy(participants = participants) }
            }
        }
    }

    fun initRenderer(renderer: SurfaceViewRenderer) = repository.initRenderer(renderer)

    fun connect(url: String, token: String, roomName: String) {
        _uiState.update { it.copy(roomName = roomName) }
        viewModelScope.launch {
            try {
                repository.connect(url, token)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to connect") }
            }
        }
    }

    fun toggleMic() {
        val enabled = !_uiState.value.isMicEnabled
        _uiState.update { it.copy(isMicEnabled = enabled) }
        viewModelScope.launch { repository.setMicEnabled(enabled) }
    }

    fun toggleCamera() {
        val enabled = !_uiState.value.isCameraEnabled
        _uiState.update { it.copy(isCameraEnabled = enabled) }
        viewModelScope.launch { repository.setCameraEnabled(enabled) }
    }

    fun switchCamera() = repository.switchCamera()

    fun toggleSpeaker() {
        val enabled = !_uiState.value.isSpeakerEnabled
        _uiState.update { it.copy(isSpeakerEnabled = enabled) }
        repository.setSpeakerEnabled(enabled)
    }

    fun leave() = repository.disconnect()

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    override fun onCleared() {
        repository.release()
    }
}

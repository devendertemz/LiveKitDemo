package com.example.livekitdemo.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.livekitdemo.R
import com.example.livekitdemo.adapter.ParticipantAdapter
import com.example.livekitdemo.databinding.ActivityCallBinding
import com.example.livekitdemo.model.CallUiState
import com.example.livekitdemo.viewmodel.CallViewModel
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.launch

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private val viewModel: CallViewModel by viewModels()
    private lateinit var adapter: ParticipantAdapter

    private var localVideoTrack: VideoTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL).orEmpty()
        val accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN).orEmpty()
        val roomName = intent.getStringExtra(EXTRA_ROOM_NAME).orEmpty()

        adapter = ParticipantAdapter(initRenderer = viewModel::initRenderer)
        binding.recyclerParticipants.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerParticipants.adapter = adapter

        viewModel.initRenderer(binding.localVideoRenderer)

        binding.buttonMic.setOnClickListener { viewModel.toggleMic() }
        binding.buttonCamera.setOnClickListener { viewModel.toggleCamera() }
        binding.buttonSwitchCamera.setOnClickListener { viewModel.switchCamera() }
        binding.buttonSpeaker.setOnClickListener { viewModel.toggleSpeaker() }
        binding.buttonLeave.setOnClickListener {
            viewModel.leave()
            finish()
        }

        if (savedInstanceState == null) {
            viewModel.connect(serverUrl, accessToken, roomName)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: CallUiState) {
        binding.textStatus.text = getString(statusTextRes(state.connectionState), state.roomName)

        val remoteParticipants = state.participants.filterNot { it.isLocal }
        updateGridLayout(remoteParticipants.size)
        adapter.submitList(remoteParticipants)

        val newLocalTrack = state.participants.firstOrNull { it.isLocal }?.videoTrack
        if (newLocalTrack !== localVideoTrack) {
            localVideoTrack?.removeRenderer(binding.localVideoRenderer)
            newLocalTrack?.addRenderer(binding.localVideoRenderer)
            localVideoTrack = newLocalTrack
        }

        binding.buttonMic.backgroundTintList = tintFor(state.isMicEnabled)
        binding.buttonCamera.backgroundTintList = tintFor(state.isCameraEnabled)
        binding.buttonSpeaker.backgroundTintList = tintFor(!state.isSpeakerEnabled)

        state.errorMessage?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    /** WhatsApp-style adaptive grid: 1 participant fills the screen, 2 stack, 3-4 form a 2x2 grid, 5+ scroll. */
    private fun updateGridLayout(remoteCount: Int) {
        val columns = when {
            remoteCount <= 2 -> 1
            remoteCount <= 4 -> 2
            else -> 3
        }
        val layoutManager = binding.recyclerParticipants.layoutManager as GridLayoutManager
        if (layoutManager.spanCount != columns) {
            layoutManager.spanCount = columns
        }

        val recyclerHeight = binding.recyclerParticipants.height
        if (recyclerHeight > 0 && remoteCount > 0) {
            val rows = (remoteCount + columns - 1) / columns
            val marginPx = (8 * resources.displayMetrics.density).toInt()
            adapter.itemHeightPx = (recyclerHeight - rows * 2 * marginPx) / rows
        }
    }

    private fun tintFor(enabled: Boolean) =
        android.content.res.ColorStateList.valueOf(
            if (enabled) 0xFF424242.toInt() else 0xFFD32F2F.toInt()
        )

    private fun statusTextRes(state: Room.State): Int = when (state) {
        Room.State.CONNECTING -> R.string.status_connecting_room
        Room.State.CONNECTED -> R.string.status_connected_room
        Room.State.RECONNECTING -> R.string.status_reconnecting_room
        Room.State.DISCONNECTED -> R.string.status_disconnected_room
    }

    companion object {
        const val EXTRA_SERVER_URL = "extra_server_url"
        const val EXTRA_ACCESS_TOKEN = "extra_access_token"
        const val EXTRA_ROOM_NAME = "extra_room_name"
    }
}

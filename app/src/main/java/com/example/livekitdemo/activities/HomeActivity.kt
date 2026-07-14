package com.example.livekitdemo.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.livekitdemo.R
import com.example.livekitdemo.databinding.ActivityHomeBinding
import com.example.livekitdemo.network.TokenApiClient
import com.example.livekitdemo.utils.PermissionHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var permissionHelper: PermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHelper = PermissionHelper(this)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        binding.editTokenServer.setText(prefs.getString(KEY_TOKEN_SERVER, DEFAULT_TOKEN_SERVER))
        binding.editIdentity.setText(prefs.getString(KEY_IDENTITY, ""))
        binding.editRoomName.setText(prefs.getString(KEY_ROOM_NAME, DEFAULT_ROOM_NAME))

        binding.buttonJoin.setOnClickListener { onJoinClicked() }
    }

    private fun onJoinClicked() {
        val tokenServer = binding.editTokenServer.text?.toString()?.trim().orEmpty()
        val identity = binding.editIdentity.text?.toString()?.trim().orEmpty()
        val roomName = binding.editRoomName.text?.toString()?.trim().orEmpty()

        if (tokenServer.isEmpty() || identity.isEmpty() || roomName.isEmpty()) {
            Snackbar.make(binding.root, R.string.error_missing_fields, Snackbar.LENGTH_SHORT).show()
            return
        }

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TOKEN_SERVER, tokenServer)
            .putString(KEY_IDENTITY, identity)
            .putString(KEY_ROOM_NAME, roomName)
            .apply()

        permissionHelper.request(
            onGranted = { fetchTokenAndJoin(tokenServer, identity, roomName) },
            onDenied = {
                Snackbar.make(binding.root, R.string.error_permissions_denied, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_open_settings) {
                        PermissionHelper.openAppSettings(this)
                    }
                    .show()
            }
        )
    }

    private fun fetchTokenAndJoin(tokenServer: String, identity: String, roomName: String) {
        binding.buttonJoin.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = TokenApiClient.fetchToken(tokenServer, roomName, identity)
                joinRoom(response.serverUrl, response.token, roomName)
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.error_token_fetch_failed, e.message),
                    Snackbar.LENGTH_LONG
                ).show()
            } finally {
                binding.buttonJoin.isEnabled = true
            }
        }
    }

    private fun joinRoom(serverUrl: String, accessToken: String, roomName: String) {
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra(CallActivity.EXTRA_SERVER_URL, serverUrl)
            putExtra(CallActivity.EXTRA_ACCESS_TOKEN, accessToken)
            putExtra(CallActivity.EXTRA_ROOM_NAME, roomName)
        }
        startActivity(intent)
    }

    companion object {
        private const val PREFS_NAME = "livekit_demo_prefs"
        private const val KEY_TOKEN_SERVER = "token_server"
        private const val KEY_IDENTITY = "identity"
        private const val KEY_ROOM_NAME = "room_name"

        private const val DEFAULT_TOKEN_SERVER = "http://192.168.1.19:3000"
        private const val DEFAULT_ROOM_NAME = "test-room"
    }
}

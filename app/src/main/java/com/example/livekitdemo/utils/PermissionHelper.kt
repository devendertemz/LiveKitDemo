package com.example.livekitdemo.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: AppCompatActivity) {

    private var onGranted: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            onGranted?.invoke()
        } else {
            onDenied?.invoke()
        }
    }

    fun request(onGranted: () -> Unit, onDenied: () -> Unit) {
        this.onGranted = onGranted
        this.onDenied = onDenied

        if (hasRequiredPermissions(activity)) {
            onGranted()
            return
        }
        launcher.launch(REQUIRED_PERMISSIONS)
    }

    companion object {
        private val REQUIRED_PERMISSIONS: Array<String> = buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()

        fun hasRequiredPermissions(context: Context): Boolean {
            return REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun openAppSettings(context: Context) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

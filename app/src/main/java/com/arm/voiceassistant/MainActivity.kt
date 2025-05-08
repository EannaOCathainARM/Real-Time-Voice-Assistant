/*
 * SPDX-FileCopyrightText: Copyright 2024-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant

import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.MODIFY_AUDIO_SETTINGS
import android.app.AlertDialog
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

import com.arm.voiceassistant.ui.theme.VoiceAssistantTheme
import com.arm.voiceassistant.utils.AppContext
import com.arm.voiceassistant.viewmodels.MainViewModel

// Main activity referenced in AndroidManifest.xml which sets up the screens

class MainActivity : ComponentActivity() {
    // Save the mainViewModel for the post initialized parts
    private lateinit var mainViewModel: MainViewModel
    // Permissions required
    private val permissions = arrayOf(RECORD_AUDIO, MODIFY_AUDIO_SETTINGS)

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allPermissionsGranted = permissions.entries.all { it.value }
        if (!allPermissionsGranted) {
            // Handle the denied permission requests as the permission request intent will not
            // pop up after the user denied the permission request twice. If we finish the app here,
            // the app will be finished immediately without any instructions after the permission is denied twice.
            // This handler will navigate the user to 'Settings' and get the permission setup according to the instructions.
            handlePermissionsNotGranted(permissions)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VoiceAssistantTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Save the context
                    AppContext.getInstance().context = this

                    // Build the screen and return the mainViewModel for post init
                    mainViewModel = ScreenScaffold()

                    // Check the permissions
                    requestPermissionLauncher.launch(permissions)
                }
            }
        }
    }

    // Pop up a Dialog and navigate the user to the Settings if the user denied the permission request
    private fun handlePermissionsNotGranted(permissions: Map<String, Boolean>) {
        val permanentlyDeniedPermissions = permissions.filter { !it.value && !shouldShowRequestPermissionRationale(it.key) }
        if (permanentlyDeniedPermissions.isNotEmpty()) {
            // Show a dialog explaining why the permissions are needed and provide a way to open app settings
            AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("The Voice Assistant requires 'Microphone' permissions to function properly. Please enable them in the Settings.")
                .setPositiveButton("Open Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // If the user choose 'Cancel', the app will be closed
                    finish()
                }
                .show()
        } else {
            // Show a message explaining why the permissions are needed
            Toast.makeText(this, "Permissions are required for the app to function properly.", Toast.LENGTH_LONG).show()
        }
    }
}

class VoiceAssistantApplication : Application()

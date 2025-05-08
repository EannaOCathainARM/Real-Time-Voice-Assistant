/*
 * SPDX-FileCopyrightText: Copyright 2024-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.viewmodels

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arm.voiceassistant.VoiceAssistantApplication


object ViewModelProvider {
    val Factory = viewModelFactory {
        // MainViewModel initialize
        initializer {
            MainViewModel(
                voiceAssistantApplication()
            )
        }
    }
}

/**
 * Returns casted instance of VoiceAssistantApplication from CreationExtras map using the provided key
 */
fun CreationExtras.voiceAssistantApplication(): VoiceAssistantApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VoiceAssistantApplication)

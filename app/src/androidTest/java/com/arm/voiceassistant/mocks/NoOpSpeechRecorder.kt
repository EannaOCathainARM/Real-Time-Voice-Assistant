/*
 * SPDX-FileCopyrightText: Copyright 2026 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.mocks

// Test-only no-op SpeechRecorder to bypass mic/permissions in UI tests.
// As recorderInitialized is a null check we need to be able to control it's return type for UI navigation tests etc to work correctly
import com.arm.voiceassistant.speech.SpeechRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NoOpSpeechRecorder(
    private val initialized: Boolean = true,
    audioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
) : SpeechRecorder(audioScope) {

    override fun startRecording() { /* no-op */ }

    override fun stopRecording(outputAudioFilePath: String) { /* no-op */ }

    override fun cancelRecording() { /* no-op */ }

    override fun release() { /* no-op */ }

    override fun recorderInitialized(): Boolean = initialized
}

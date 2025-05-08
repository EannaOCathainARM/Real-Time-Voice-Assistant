/*
 * SPDX-FileCopyrightText: Copyright 2024-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant

import android.util.Log
import com.arm.voiceassistant.utils.Constants
import com.arm.voiceassistant.utils.Timer

/**
 * Container for the pipeline timers
 */
class PipelineTimers {

    private var speechRecTimer: Timer = Timer()

    private var realTimer: Timer = Timer() // End of speech to first synthesized speech word
    private var firstResponseTimer: Timer = Timer() // LLM start to first token response

    /**
     * Toggle the first response timer
     */
    fun toggleFirstResponseTimer(start: Boolean, dump: Boolean) {
        toggleTimer(start, firstResponseTimer)
        if (dump) {
            // Log response time from start of LLM to when first response comes back
            dumpTimer(firstResponseTimer, "first response time")
        }
    }

    /**
     * Dump out timer information
     */
    private fun dumpTimer(timer: Timer, msg: String) {
        val seconds = if (timer.elapsedTime > 0) { timer.elapsedTime/1000f } else { 0.0f }
        Log.d(Constants.VOICE_ASSISTANT_TAG, "${msg}=${seconds}")
        timer.reset()
    }

    /**
     * Toggle the real timer
     */
    fun toggleRealTimer(start: Boolean, dump: Boolean) {
        toggleTimer(start, realTimer)
        if (dump) {
            dumpTimer(realTimer, "real time")
        }
    }

    /**
     * Toggle timer
     */
    private fun toggleTimer(start: Boolean, timer: Timer) {
        if (start) {
            timer.start()
        } else {
            timer.stop()
        }
    }

    /**
     * Toggle the speech rec timer
     */
    fun toggleSpeechRecTimer(start: Boolean) {
        toggleTimer(start, speechRecTimer)
    }

    /**
     * Return speech recognition time
     */
    fun getSpeechRecognitionTime() : Float {
        val speechRecognitionTimeSeconds = if (speechRecTimer.elapsedTime > 0) { speechRecTimer.elapsedTime/1000f } else { 0.0f }
        return speechRecognitionTimeSeconds
    }
}

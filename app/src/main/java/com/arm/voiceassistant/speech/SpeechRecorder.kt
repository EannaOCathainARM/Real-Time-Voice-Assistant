/*
 * SPDX-FileCopyrightText: Copyright 2024-2026 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.speech

import android.annotation.SuppressLint
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.arm.voiceassistant.audio.AudioRecorder
import com.arm.voiceassistant.utils.Constants.VOICE_ASSISTANT_TAG
import kotlinx.coroutines.CoroutineScope


/**
 * Speech recorder
 */
open class SpeechRecorder (private val audioScope: CoroutineScope) {
    private var recorder: AudioRecorder? = null




    /**
     * Initialize the recorder with an appropriate audio source and sample rate.
     * Tries multiple input sources (VOICE_RECOGNITION, then MIC) and sample rates
     * (16 kHz, 48 kHz, 44.1 kHz) to find a supported configuration.
     */
    @SuppressLint("MissingPermission")
    open fun initRecorder() {
        val channelConfig = AudioRecorder.RECORDER_CHANNELS
        val audioEncoding = AudioRecorder.RECORDER_AUDIO_ENCODING
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )
        val sampleRates = listOf(16000, 48000, 44100)  // target 16k, then fallbacks

        // Try preferred sources + sample rates
        for (src in sources) {
            for (rate in sampleRates) {
                val minBuf = AudioRecord.getMinBufferSize(rate, channelConfig, audioEncoding)
                if (minBuf <= 0) continue

                try {
                    val rec = AudioRecord(src, rate, channelConfig, audioEncoding, minBuf)
                    if (rec.state == AudioRecord.STATE_INITIALIZED) {
                        // Found a working config: construct and return
                        recorder = AudioRecorder(rec, rate, minBuf, scope = audioScope)

                        return
                    } else {
                        rec.release()
                    }
                } catch (e: Exception) {
                    Log.i(
                        VOICE_ASSISTANT_TAG,
                        "AudioRecord init failed for src=$src, rate=$rate: ${e.message}"
                    )
                }
            }
        }

        // Fallback if none of the above worked
        val fallbackRate = AudioRecorder.SAMPLE_RATE
        val minBuf = AudioRecord.getMinBufferSize(fallbackRate, channelConfig, audioEncoding)
        val bufSize = if (minBuf > 0) minBuf else 512

        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            fallbackRate,
            channelConfig,
            audioEncoding,
            bufSize
        )

        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            Log.i(
                VOICE_ASSISTANT_TAG,
                "Unable to initialize AudioRecord with any configuration; last tried fallbackRate=$fallbackRate"
            )
            throw RuntimeException("Unable to initialize AudioRecord with any configuration")
        }

        // Fallback succeeded
        recorder = AudioRecorder(rec, fallbackRate, bufSize, scope = audioScope)

    }

    /**
     * Check if the recorder has been initialized
     * @return `true` if the recorder is initialized, `false` otherwise.
     */
    open fun recorderInitialized(): Boolean {
        return recorder != null
    }

    /**
     * Start recording
     */
    open fun startRecording() {
        //  Ensure recorder exists and underlying AudioRecord is still usable.
        if (recorder == null) {
            initRecorder()
        }

        try {
            recorder!!.startRecording()
        } catch (e: IllegalStateException) {
            // Underlying AudioRecord was released or invalid; re-init and retry once.
            initRecorder()
            recorder!!.startRecording()
        }
    }

    /**
     * Stop recording
     * @param outputAudioFilePath The full path to save the recorded WAV file.
     */
    open fun stopRecording(outputAudioFilePath: String) {
        if (recorder == null) return
        recorder!!.stopRecording(true)
        recorder!!.writeToFile(outputAudioFilePath)
    }

    /**
     * Cancel recording
     */
    open fun cancelRecording() {
        if (recorder == null) return
        recorder!!.stopRecording(false)
    }

    /**
     * Release the recorder and associated resources.
     */
    open fun release() {
        if (recorder == null) return
        try {
            recorder!!.stopRecording(false)
            recorder!!.release()
        } catch (e: Exception) {
            Log.i(VOICE_ASSISTANT_TAG, "Recorder release failed: ${e.message}")
        }
        recorder = null  // mark as uninitialized, allowing fresh init later
    }
}

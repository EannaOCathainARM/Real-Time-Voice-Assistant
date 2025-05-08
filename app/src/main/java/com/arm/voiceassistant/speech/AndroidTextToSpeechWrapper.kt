/*
 * SPDX-FileCopyrightText: Copyright 2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.arm.voiceassistant.utils.Constants
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android TTS wrapper
 * Generate the speech and save to an audio wav file to the output path parameter
 */
class AndroidTTS(context: Context) : TextToSpeech.OnInitListener {
    var ttsInitialized = false
    var tts: TextToSpeech? = null
    var ttsInProgress = AtomicBoolean(false)

    init {
        tts = TextToSpeech(context, this)
    }

    companion object {
        @Volatile
        private var instance: AndroidTTS? = null

        fun getInstance(context: Context): AndroidTTS {
            return instance ?: synchronized(this) {
                instance ?: AndroidTTS(context).also { instance = it }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsInitialized = true
            tts?.language = Locale.US
            setOnUtteranceProgressListener()
            Log.d("AndroidTTS", "Initialization success")
        } else {
            Log.e("AndroidTTS", "Initialization failed")
        }
    }

    // TODO Progress listener is used to ensure speech file saving completes.
    // This requires the onDone listener to be overridden. It is only used
    // once so it may be possible to place the countdown latch in the main definition here
    private fun setOnUtteranceProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                Log.d(Constants.VOICE_ASSISTANT_TAG, "Utterance: $utteranceId")
                ttsInProgress.set(true)
            }

            override fun onDone(utteranceId: String) {
                Log.d(Constants.VOICE_ASSISTANT_TAG, "ATTS Utterance completed: $utteranceId")
                ttsInProgress.set(false)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                Log.e(
                    Constants.VOICE_ASSISTANT_TAG,
                    "Error with TTS utterance progress listener for: $utteranceId"
                )
                ttsInProgress.set(false)
            }
        })
    }

    fun isSpeaking():Boolean
    {
        return ttsInProgress.get()
    }
}

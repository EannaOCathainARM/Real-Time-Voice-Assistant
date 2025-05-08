/*
 * SPDX-FileCopyrightText: Copyright 2024-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.speech

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SpeechSynthesisTest {
    private var speechSynthesis: SpeechSynthesis? = null
    private var context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var latch: CountDownLatch

    @Before
    fun setup() {
        speechSynthesis = SpeechSynthesis()
        speechSynthesis?.setContext(context)
        speechSynthesis?.initSpeechSynthesis()
        latch = CountDownLatch(1)
        // Due to lateinit and complexity of subclass initialization a small delay is required to ensure the A-TTS object
        // has been initialized. Empirically determined 500ms is close to the minimum requirement but 1000ms used to mitigate issues with CI etc.
        latch.await(
            1000,
            TimeUnit.MILLISECONDS
        ) // Wait for onInit function in SpeechSynthesis to finish executing
    }

    @After
    fun tearDown() {
        context = null
        speechSynthesis = null
    }

    @Test
    fun testSpeechSynthesisInitialization() {
        val initialized = speechSynthesis?.speechSynthesisInitialized()
        assertEquals(true, initialized)
    }

    @Test
    fun testSpeechSynthesisStarted(): Unit = runBlocking {
        speechSynthesis?.startSpeechSynthesis()
        assert(speechSynthesis?.speechSynthesisInProgress() == true)
    }



    @Test
    fun testGenerateSpeechCancellation(): Unit = runBlocking {
        speechSynthesis?.startSpeechSynthesis()
        speechSynthesis?.generateSpeech("First sentence answer to a question. ")
        assert(speechSynthesis?.speechSynthesisInProgress() == true)
        latch.await(5, TimeUnit.SECONDS)
        speechSynthesis?.cancelSpeechSynthesis()

        speechSynthesis?.startSpeechSynthesis()
        speechSynthesis?.generateSpeech("Speaking again after cancel")
        assert(speechSynthesis?.speechSynthesisInProgress() == true)
        latch.await(5, TimeUnit.SECONDS)
    }

    @Test
    fun testSpeechSynthesisGenerationOnPeriodBreakOnly(): Unit = runBlocking {
        val tokens = "First sentence answer to a generation question.".split(" ")
        speechSynthesis?.startSpeechSynthesis()
        for(token in tokens) {
            speechSynthesis?.addWordsToSpeechSynthesis(token)
        }

        latch.await(5, TimeUnit.SECONDS)
        speechSynthesis?.addWordsToSpeechSynthesis(" Another")
        assert(speechSynthesis?.speechSynthesisInProgress() == true)

        speechSynthesis?.addWordsToSpeechSynthesis(" sentence. Third")
        assert(speechSynthesis?.speechSynthesisInProgress() == true)
        latch.await(5, TimeUnit.SECONDS)
    }
}



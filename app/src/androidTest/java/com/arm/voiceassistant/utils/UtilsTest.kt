/*
 * SPDX-FileCopyrightText: Copyright 2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.utils

import com.arm.voiceassistant.utils.Utils.removeTags
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test common utilities methods found in utils package
 */
class UtilsTest {

    /**
     *  Test remove tags method which removes the known tags
     *  from the transcription
     */
    @Test
    fun testRemoveTags() {
        val sampleTranscriptions = listOf("[BEEP]", "[Coughing]")
        val sampleNonFinishedTranscriptions =  listOf("Coughing", "[BEEP", "Laughing]")

        for( transcription in sampleTranscriptions ) {
            assertEquals("", removeTags(transcription));
        }

        for( transcription in sampleNonFinishedTranscriptions ) {
            assertEquals(transcription, removeTags(transcription));
        }
    }
}

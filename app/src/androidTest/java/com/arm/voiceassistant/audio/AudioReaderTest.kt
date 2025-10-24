/*
 * SPDX-FileCopyrightText: Copyright 2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.math.abs

/**
 * Unit tests for AudioReader:
 * - Works with recorder-style WAV headers (sizes set to zero).
 * - Correct normalization of PCM16 to floats.
 * - Enforces 20s cap.
 * - Ignores an odd trailing byte.
 */
class AudioReaderTest {

    private fun le16(v: Int) = byteArrayOf((v and 0xFF).toByte(), ((v ushr 8) and 0xFF).toByte())
    private fun le32(v: Int) = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v ushr 8) and 0xFF).toByte(),
        ((v ushr 16) and 0xFF).toByte(),
        ((v ushr 24) and 0xFF).toByte()
    )

    /**
     * Build a canonical 44-byte PCM16-LE WAV header, with RIFF/data sizes set to zero
     * (matches the current AudioRecorder behavior).
     */
    private fun buildWavHeader(sampleRate: Int = 16_000, channels: Int = 1, bitsPerSample: Int = 16): ByteArray {
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = (channels * (bitsPerSample / 8))
        return ByteArray(44).also { h ->
            // RIFF
            "RIFF".toByteArray().copyInto(h, 0)
            le32(0).copyInto(h, 4) // riff size (zero per recorder)
            "WAVE".toByteArray().copyInto(h, 8)
            // fmt chunk
            "fmt ".toByteArray().copyInto(h, 12)
            le32(16).copyInto(h, 16)                 // fmt chunk size
            le16(1).copyInto(h, 20)                  // audio format PCM
            le16(channels).copyInto(h, 22)           // channels
            le32(sampleRate).copyInto(h, 24)         // sample rate
            le32(byteRate).copyInto(h, 28)           // byte rate
            le16(blockAlign).copyInto(h, 32)         // block align
            le16(bitsPerSample).copyInto(h, 34)      // bits per sample
            // data chunk
            "data".toByteArray().copyInto(h, 36)
            le32(0).copyInto(h, 40)                  // data size (zero per recorder)
        }
    }

    /** Helper to turn ShortArray PCM16 into little-endian bytes. */
    private fun pcm16ToBytes(samples: ShortArray): ByteArray {
        val out = ByteArray(samples.size * 2)
        var i = 0
        for (s in samples) {
            val v = s.toInt()
            out[i++] = (v and 0xFF).toByte()
            out[i++] = ((v ushr 8) and 0xFF).toByte()
        }
        return out
    }

    @Test
    fun readWavData_normalizationAndLength() {
        // Three known samples: 0, +32767, -32768
        val pcm = shortArrayOf(0, 32767, -32768)
        val header = buildWavHeader(sampleRate = 16_000, channels = 1, bitsPerSample = 16)
        val body = pcm16ToBytes(pcm)
        val wavBytes = header + body

        val reader = AudioReader()
        val result = reader.readWavData(ByteArrayInputStream(wavBytes))

        // Length matches number of samples.
        assertEquals(3, result.size)

        // Normalization checks (within a small tolerance).
        // 32767 / 32768 ≈ 0.9999695
        assertEquals(0.0f, result[0], 1e-6f)
        assertEquals(32767f / 32768f, result[1], 1e-4f)
        assertEquals(-1.0f, result[2], 1e-6f)
    }

    @Test
    fun readWavData_ignoresOddTrailingByte() {
        // Two samples (4 bytes) + 1 dangling byte at the end.
        val pcm = shortArrayOf(1000, -1000)
        val header = buildWavHeader()
        val body = pcm16ToBytes(pcm) + byteArrayOf(0x7F) // odd trailing byte
        val wavBytes = header + body

        val reader = AudioReader()
        val result = reader.readWavData(ByteArrayInputStream(wavBytes))

        // Only 2 samples should be decoded; dangling byte is ignored.
        assertEquals(2, result.size)
        // Basic sanity on values:
        assertTrue(abs(result[0] - (1000f / 32768f)) < 1e-4f)
        assertTrue(abs(result[1] - (-1000f / 32768f)) < 1e-4f)
    }

    @Test
    fun readWavData_enforcesTwentySecondCap() {
        val sampleRate = 16_000
        val channels = 1
        val secondsRequested = 25 // attempt to exceed the cap

        // Build >20s worth of PCM bytes.
        val totalSamples = sampleRate * channels * secondsRequested
        val pcm = ShortArray(totalSamples) { 1234 } // arbitrary non-zero
        val header = buildWavHeader(sampleRate = sampleRate, channels = channels, bitsPerSample = 16)
        val body = pcm16ToBytes(pcm)
        val wavBytes = header + body

        val reader = AudioReader()
        val result = reader.readWavData(ByteArrayInputStream(wavBytes))

        // Expect exactly 20s worth of samples.
        val expectedMaxSamples = sampleRate * channels * 20
        assertEquals(expectedMaxSamples, result.size)
        // Spot-check a value is properly normalized.
        assertTrue(abs(result[0] - (1234f / 32768f)) < 1e-4f)
    }
}
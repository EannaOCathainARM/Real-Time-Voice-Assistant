/*
 * SPDX-FileCopyrightText: Copyright 2023-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.audio

import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Class to read data from WAV format
 */
class AudioReader {
    companion object {
        // Normalisation factor for signed 16-bit PCM.
        const val NORMALISATION_FACTOR = (1.0 / 0x8000).toFloat()
        // Hard cap to block excessive recordings (seconds).
        private const val MAX_SECONDS = 20
        // I/O chunk size; affects read() frequency, not total decoded samples. 32 KiB balances syscall overhead and cache locality.
        private const val READ_BUFFER_BYTES = 32 * 1024

        // Duration to pre-allocate for the output FloatArray (seconds).
        // 4s reduces reallocations while avoiding over-allocation and is divisible into MAX_SECONDS.
        private const val PRE_ALLOCATED_SECONDS = 4

        // Fallback sample rate if header is malformed or zero.
        private const val FALLBACK_SAMPLE_RATE_HZ = 16_000

        // PCM16 uses 2 bytes per sample (per channel).
        private const val BYTES_PER_SAMPLE = 2
    }

    private data class WavHeaderInfo(
        val sampleRate: Int,
        val channels: Int,
        val bitsPerSample: Int
    )

    /**
     * Read and convert wav data to usable format.
     * - Reads the canonical 44-byte header and minimally validates RIFF/WAVE.
     * - Derives a decode cap from (sampleRate * channels * MAX_SECONDS).
     * - Streams and decodes in chunks; no per-sample allocations.
     * - Tolerant of RIFF/data sizes in the header being zero (as produced by AudioRecorder).
     * @param inputStream The input stream pointing to a WAV file.
     * @return A [FloatArray] containing normalized PCM audio data.
     */
    fun readWavData(inputStream: InputStream): FloatArray {
        val bufferedInput = if (inputStream is BufferedInputStream) inputStream else BufferedInputStream(inputStream)
        bufferedInput.use { inStream ->
            val header = parseWavHeader(inStream)
            val (maxSamplesLong, maxSamples, initialCapacity) = deriveDecodeCaps(header.sampleRate, header.channels)
            return streamDecodePcm16Le(inStream, maxSamplesLong, maxSamples, initialCapacity)
        }
    }

    private fun parseWavHeader(inStream: InputStream): WavHeaderInfo {
        // Read 44-byte header (recorder writes header with zeroed sizes)
        val header = ByteArray(AudioRecorder.WAV_HEADER_SIZE)
        readExactly(inStream, header, 0, header.size)

        // Minimal RIFF/WAVE check
        if (!isHeaderMatching("RIFF", header, 0) || !isHeaderMatching("WAVE", header, 8)) {
            throw IllegalArgumentException("Invalid WAV header")
        }

        // Extract little-endian format fields using ByteBuffer.
        val headerByteBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        val channels = headerByteBuffer.getShort(22).toInt() and 0xFFFF
        if (channels <= 0) throw IllegalArgumentException("Invalid channel count: $channels")

        val sampleRate = headerByteBuffer.getInt(24).let { if (it > 0) it else FALLBACK_SAMPLE_RATE_HZ }

        val bitsPerSample = headerByteBuffer.getShort(34).toInt() and 0xFFFF
        if (bitsPerSample != 16) {
            throw IllegalArgumentException("Unsupported PCM depth: $bitsPerSample bits")
        }

        return WavHeaderInfo(sampleRate = sampleRate, channels = channels, bitsPerSample = bitsPerSample)
    }

    private fun isHeaderMatching(text: String, header: ByteArray, offset: Int): Boolean {
        if (offset < 0 || offset + text.length > header.size) return false
        for (i in text.indices) {
            if (header[offset + i] != text[i].code.toByte()) return false
        }
        return true
    }

    private fun deriveDecodeCaps(sampleRate: Int, channels: Int): Triple<Long, Int, Int> {
        // Compute a hard cap: samples = sampleRate * channels * MAX_SECONDS.
        val maxSamplesLong = multiplyChecked(
            multiplyChecked(sampleRate.toLong(), channels.toLong()),
            MAX_SECONDS.toLong()
        )
        val maxSamples = maxSamplesLong.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        // Pre-allocate up to the cap; if fewer samples are read, we trim at the end.
        val preallocSamplesLong = multiplyChecked(
            multiplyChecked(sampleRate.toLong(), channels.toLong()),
            PRE_ALLOCATED_SECONDS.toLong()
        )
        val initialCapacity = minOf(maxSamplesLong, preallocSamplesLong, Int.MAX_VALUE.toLong()).toInt()

        return Triple(maxSamplesLong, maxSamples, initialCapacity)
    }

    private fun streamDecodePcm16Le(
        inStream: InputStream,
        maxSamplesLong: Long,
        maxSamples: Int,
        initialCapacity: Int
    ): FloatArray {
        var sampleBuffer = FloatArray(initialCapacity)
        val readBuffer = ByteArray(READ_BUFFER_BYTES)
        var outIndex = 0

        // If a read returns an odd number of bytes, keep the low byte here and combine it with the first new byte below.
        var carryLo: Int = -1

        // Stream until EOF or sample cap is reached
        while (outIndex.toLong() < maxSamplesLong) {
            // Don’t read more than the remaining samples budget, converted to bytes.
            val samplesRemaining = maxSamplesLong - outIndex.toLong()
            val bytesRemainingForCap = samplesRemaining * BYTES_PER_SAMPLE.toLong()
            if (bytesRemainingForCap <= 0L) break

            val toRead = minOf(readBuffer.size.toLong(), bytesRemainingForCap).toInt()
            val bytesRead = inStream.read(readBuffer, 0, toRead)
            if (bytesRead <= 0) break // avoid potential spin on zero-byte or EOF reads

            var i = 0

            // Complete a cross-boundary sample if needed.
            if (carryLo != -1 && bytesRead > 0 && outIndex.toLong() < maxSamplesLong) {
                val lo = carryLo and 0xFF
                val hi = readBuffer[0].toInt()
                val sample = ((hi shl 8) or lo).toShort()
                sampleBuffer = ensureCapacity(sampleBuffer, outIndex + 1, maxSamples)
                sampleBuffer[outIndex++] = sample.toFloat() * NORMALISATION_FACTOR
                i = 1
                carryLo = -1
            }

            // Decode full 16-bit little-endian samples from the buffer.
            // Make limit even so i+1 is always in-bounds.
            val evenLimit = bytesRead - ((bytesRead - i) and 1)

            if (i < evenLimit) {
                val samplesByteBuffer = ByteBuffer.wrap(readBuffer, i, evenLimit - i).order(ByteOrder.LITTLE_ENDIAN)
                val shortCount = (evenLimit - i) / 2
                var j = 0
                while (j < shortCount && outIndex.toLong() < maxSamplesLong) {
                    val s = samplesByteBuffer.getShort(j * 2)
                    sampleBuffer = ensureCapacity(sampleBuffer, outIndex + 1, maxSamples)
                    sampleBuffer[outIndex++] = s.toFloat() * NORMALISATION_FACTOR
                    j++
                }
                i = evenLimit
            }

            // If one byte remains, carry it to pair with the first byte of the next read.
            carryLo = if (i < bytesRead) readBuffer[i].toInt() else -1
        }

        // Return only the decoded region.
        return if (outIndex == sampleBuffer.size) sampleBuffer else sampleBuffer.copyOf(outIndex)
    }

    private fun ensureCapacity(buffer: FloatArray, neededSize: Int, maxSamples: Int): FloatArray {
        if (neededSize <= buffer.size) return buffer
        val newSize = minOf(buffer.size * 2, maxSamples)
        if (newSize <= buffer.size) return buffer // at cap; caller will trim at return
        return buffer.copyOf(newSize)
    }

    private fun readExactly(stream: InputStream, buffer: ByteArray, offset: Int, length: Int) {
        var currentOffset = offset
        var remaining = length
        while (remaining > 0) {
            val n = stream.read(buffer, currentOffset, remaining)
            if (n <= 0) throw IllegalArgumentException("Unexpected EOF")
            currentOffset += n
            remaining -= n
        }
    }

    private fun multiplyChecked(a: Long, b: Long): Long {
        val result = a * b
        if (a != 0L && result / a != b) throw IllegalArgumentException("Size overflow")
        return result
    }
}

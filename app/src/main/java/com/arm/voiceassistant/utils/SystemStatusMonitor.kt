/*
 * SPDX-FileCopyrightText: Copyright 2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.arm.voiceassistant.utils.Constants.VOICE_ASSISTANT_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.floor
import android.os.PowerManager

/**
 * Monitors system-level metrics such as memory usage, available memory,
 * and device thermal status at a fixed polling interval.
 *
 * The monitor runs its own coroutine scope and exposes all metrics as
 * {@link StateFlow}s so they can be safely observed from Jetpack Compose
 * or other reactive consumers.
 *
 * @param context Android context used to access system services.
 *                The application context is retained internally to avoid leaks.
 * @param intervalSeconds Polling interval (in seconds) for refreshing system metrics.
 */
class SystemStatusMonitor(
    context: Context,
    private val intervalSeconds: Long = 5L // configurable, default 3 seconds
) {

    private val appContext = context.applicationContext
    private val activityManager =
        appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager =
        appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _memoryUsageGb = MutableStateFlow(0.0f)
    val memoryUsageGb: StateFlow<Float> = _memoryUsageGb.asStateFlow()

    private val _memoryAvailableGb = MutableStateFlow(0.0f)
    val memoryAvailableGb: StateFlow<Float> = _memoryAvailableGb.asStateFlow()

    private val _thermalStatus = MutableStateFlow(0)
    val thermalStatus: StateFlow<Int> = _thermalStatus.asStateFlow()

    private var job: Job? = null

    /**
     * Starts monitoring system metrics.
     *
     * If monitoring is already active, this call is ignored.
     * Metrics are updated on a background thread at the configured interval.
     */
    fun start() {
        if (job != null) return // already running
        job = scope.launch {
            while (isActive) {
                _memoryUsageGb.value = readMemoryUsageGb()
                _memoryAvailableGb.value = readMemoryAvailableGb()
                _thermalStatus.value = readThermalStatus()

                delay(intervalSeconds * 1000L)
            }
        }
    }

    /*
     * Stops monitoring and cancels all running coroutines.
     *
     * After calling this method, no further metric updates will occur
     * until {@link #start} is called again.
     */
    fun shutdown() {
        job?.cancel()
        (scope.coroutineContext[Job] as? Job)?.cancel()
        job = null
    }

    /**
     * Rounds a floating-point value down to two decimal places.
     *
     * @param x Value to be rounded.
     * @return Value rounded down to two decimal places.
     */
    fun roundDownTo2Decimals(x: Float): Float = floor(x * 100f) / 100f

    /**
     * Reads the current process memory usage in gigabytes.
     *
     * The calculation includes private and shared clean/dirty memory
     *
     * @return Current memory usage in gigabytes.
     */
    private fun readMemoryUsageGb(): Float {
        val pid = android.os.Process.myPid()
        val memoryInfoArray = activityManager.getProcessMemoryInfo(intArrayOf(pid))
        val memoryInfo = memoryInfoArray.firstOrNull() ?: return 0.0f
        val memoryUsageKB = memoryInfo.totalPrivateClean + memoryInfo.totalPrivateDirty + memoryInfo.totalSharedClean + memoryInfo.totalSharedDirty
        val memoryUsageGB = roundDownTo2Decimals(memoryUsageKB.toFloat() / (1024 * 1024))
        Log.d(VOICE_ASSISTANT_TAG,"Current memory usage $memoryUsageGB GB")
        return memoryUsageGB
    }

    /**
     * Reads the currently available system memory in gigabytes.
     *
     * @return Available memory in gigabytes.
     */
    private fun readMemoryAvailableGb(): Float {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val memoryFreeB = memoryInfo.availMem
        val memoryFreeGB = roundDownTo2Decimals(memoryFreeB.toFloat() / (1024 * 1024 * 1024))
        Log.d(VOICE_ASSISTANT_TAG,"Current memory available $memoryFreeGB GB")
        return memoryFreeGB
    }

    /**
     * Reads the current device thermal status from {@link PowerManager}.
     *
     * @return Thermal status value as defined by {@link PowerManager} constants.
     */
    private fun readThermalStatus(): Int {
       var thermalStatus =  powerManager.currentThermalStatus
       Log.d(VOICE_ASSISTANT_TAG,"Current thermal status $thermalStatus / 6")
       return thermalStatus
    }
}

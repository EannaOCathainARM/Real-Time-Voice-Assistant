/*
 * SPDX-FileCopyrightText: Copyright 2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.ui.composables


import androidx.compose.material3.Text
import androidx.compose.runtime.*
import android.content.Context
import android.os.PowerManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.arm.voiceassistant.utils.SystemStatusMonitor

/**
 * Creates and manages a {@link SystemStatusMonitor} instance scoped to this composable lifecycle.
 *
 * This helper remembers a single monitor instance for the given application context and polling interval,
 * starts monitoring when the composable enters the composition, and shuts it down when it leaves.
 *
 * @param context Android context used to create the monitor. Defaults to the current composition context.
 *                The application context is used internally to avoid leaking an Activity.
 * @param intervalSeconds Polling interval (in seconds) used by the monitor to refresh system metrics.
 *
 * @return A {@link SystemStatusMonitor} that exposes system metrics as reactive flows/state.
 */
@Composable
fun systemMetricsMonitor(
    context: Context = LocalContext.current,
    intervalSeconds: Long = 1L
): SystemStatusMonitor {
    val monitor = remember(context.applicationContext, intervalSeconds) {
        SystemStatusMonitor(context.applicationContext, intervalSeconds)
    }

    // Start/stop tied to the composable's lifecycle
    DisposableEffect(monitor) {
        monitor.start()
        onDispose {
            monitor.shutdown()
        }
    }

    return monitor
}

/**
 * Displays the current memory usage in gigabytes as a text label.
 *
 * This composable subscribes to {@link SystemStatusMonitor#memoryUsageGb} and automatically recomposes
 * whenever the underlying value changes.
 *
 * Preview for this component can be seen in PerformanceMetrics.kt
 *
 * @param textStyle Style applied to the rendered text.
 * @param intervalSeconds Polling interval (in seconds) used to refresh the memory usage metric.
 */
@Composable
fun MemoryUsageText(
    textStyle: TextStyle = TextStyle(),
    intervalSeconds: Long = 3L
) {
    val context = LocalContext.current
    val monitor = systemMetricsMonitor(context, intervalSeconds)

    // Automatically recompose when memoryUsageMb changes
    val memoryGb by monitor.memoryUsageGb.collectAsState()

    Text(
        text = "$memoryGb GB",
        style = textStyle
    )
}

/**
 * Displays the currently available memory in gigabytes as a text label.
 *
 * This composable subscribes to {@link SystemStatusMonitor#memoryAvailableGb} and automatically recomposes
 * whenever the underlying value changes.
 *
 * Preview for this component can be seen in PerformanceMetrics.kt
 *
 * @param textStyle Style applied to the rendered text.
 * @param intervalSeconds Polling interval (in seconds) used to refresh the available memory metric.
 */
@Composable
fun MemoryAvailableText(
    textStyle: TextStyle = TextStyle(),
    intervalSeconds: Long = 3L
) {
    val context = LocalContext.current
    val monitor = systemMetricsMonitor(context, intervalSeconds)

    // Automatically recompose when memoryUsageMb changes
    val memoryGb by monitor.memoryAvailableGb.collectAsState()

    Text(
        text = "$memoryGb GB",
        style = textStyle
    )
}

/**
 * Displays the current device thermal status as a human-readable text label.
 *
 * This composable subscribes to {@link SystemStatusMonitor#thermalStatus} and converts the raw
 * {@link PowerManager} thermal status constant into a readable description via {@link #thermalStatusToText}.
 *
 * Preview for this component can be seen in PerformanceMetrics.kt
 *
 * @param textStyle Style applied to the rendered text.
 * @param intervalSeconds Polling interval (in seconds) used to refresh the thermal status metric.
 */
@Composable
fun ThermalStatusText(
    textStyle: TextStyle = TextStyle(),
    intervalSeconds: Long = 3L
) {
    val context = LocalContext.current
    val monitor = systemMetricsMonitor(context, intervalSeconds)

    // Automatically recompose when memoryUsageMb changes
    val thermalStatus by monitor.thermalStatus.collectAsState()

    val thermalStatusDescription = thermalStatusToText(thermalStatus)
    Text(
        text = "$thermalStatusDescription",
        style = textStyle
    )
}

/**
 * Converts a {@link PowerManager} thermal status constant into a user-friendly description.
 *
 * @param status Thermal status value, typically one of {@link PowerManager#THERMAL_STATUS_NONE},
 *               {@link PowerManager#THERMAL_STATUS_LIGHT}, {@link PowerManager#THERMAL_STATUS_MODERATE},
 *               {@link PowerManager#THERMAL_STATUS_SEVERE}, {@link PowerManager#THERMAL_STATUS_CRITICAL},
 *               {@link PowerManager#THERMAL_STATUS_EMERGENCY}, {@link PowerManager#THERMAL_STATUS_SHUTDOWN}.
 *
 * @return A human-readable string describing the thermal state (or "Unknown" if not recognized).
 */
fun thermalStatusToText(status: Int): String = when (status) {
    PowerManager.THERMAL_STATUS_NONE ->
        "None (normal)"
    PowerManager.THERMAL_STATUS_LIGHT ->
        "Light (slight throttling)"
    PowerManager.THERMAL_STATUS_MODERATE ->
        "Moderate (performance may be reduced)"
    PowerManager.THERMAL_STATUS_SEVERE ->
        "Severe (significant throttling)"
    PowerManager.THERMAL_STATUS_CRITICAL ->
        "Critical (device very hot)"
    PowerManager.THERMAL_STATUS_EMERGENCY ->
        "Emergency (shutdown imminent)"
    PowerManager.THERMAL_STATUS_SHUTDOWN ->
        "Shutdown (shutting down)"
    else ->
        "Unknown"
}
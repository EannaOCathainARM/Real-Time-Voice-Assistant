/*
 * SPDX-FileCopyrightText: Copyright 2024-2026 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.ui.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arm.voiceassistant.utils.TimingStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared footer text renderer for timing/performance labels.
 *
 * Centralizes style and base padding so timing footers are consistent and easy to maintain.
 *
 * @param text Footer text to display
 * @param modifier Optional extra modifier applied before the standard padding
 */
@Composable
private fun TimingFooterText(
    text: String,
    modifier: Modifier = Modifier
) {
    val style = TextStyle(
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.25f),
            offset = Offset(0f, 1f),
            blurRadius = 3f
        )
    )

    Text(
        text = text,
        style = style,
        modifier = modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp)
    )
}

/**
 * Displays detailed timing statistics for an assistant response.
 *
 * Shows STT latency and LLM encode/decode throughput below an assistant message.
 *
 * @param timing Timing statistics associated with the assistant response
 */
@Composable
fun AssistantTimingFooter(timing: TimingStats) {
    TimingFooterText(
        text = "STT: ${timing.sttTime}s  •  Encode: ${timing.llmEncodeTps} tok/s  •  Decode: ${timing.llmDecodeTps} tok/s"
    )
}

/**
 * Wraps a chat bubble with a speaker label and alignment.
 *
 * Handles left/right alignment and label positioning for user
 * and assistant messages.
 *
 * @param label Display label (e.g. "User", "Voice Assistant")
 * @param isUser Whether the message is from the user
 * @param bubble Composable representing the message content
 */
@Composable
fun MessageItem(
    label: String,
    isUser: Boolean,
    bubble: @Composable () -> Unit
) {
    val align = if (isUser) Alignment.End else Alignment.Start
    val labelMod = if (isUser) Modifier.padding(end = 12.dp, bottom = 4.dp)
    else Modifier.padding(start = 12.dp, bottom = 4.dp)

    Column(horizontalAlignment = align) {
        Text(
            text = label,
            fontSize = 16.sp,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = labelMod
        )
        bubble()
    }
}

/**
 * Displays timing statistics associated with a user message.
 *
 * Typically shows STT latency and LLM encode throughput.
 *
 * @param timing Timing statistics associated with the user input
 */
@Composable
fun UserTimingFooter(timing: TimingStats) {
    TimingFooterText(
        text = "STT: ${timing.sttTime}s  •  Encode: ${timing.llmEncodeTps} tok/s"
    )
}

/**
 * Displays decode throughput for an assistant message and ensures
 * it scrolls into view when it appears.
 *
 * @param timing Timing statistics containing decode performance
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssistantDecodeFooter(timing: TimingStats) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    // When decode timing appears, make sure it becomes visible
    LaunchedEffect(timing.llmDecodeTps) {
        // tiny delay allows layout to settle after the bubble expands
        delay(16)
        scope.launch { bringIntoViewRequester.bringIntoView() }
    }

    TimingFooterText(
        text = "Decode: ${timing.llmDecodeTps} tok/s",
        modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
    )
}

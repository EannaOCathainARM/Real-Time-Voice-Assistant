/*
 * SPDX-FileCopyrightText: Copyright 2024-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithContentDescription
import com.arm.voiceassistant.ui.screens.MainScreen
import com.arm.voiceassistant.ui.theme.VoiceAssistantTheme
import com.arm.voiceassistant.utils.Constants
import com.arm.voiceassistant.viewmodels.MainUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import org.junit.Rule
import org.junit.Test

class UiStateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var userTextBox =
        composeTestRule.onNodeWithContentDescription("user_input")
    private var responseTextBox =
        composeTestRule.onNodeWithContentDescription("response_output")
    private var startRecordingButton =
        composeTestRule.onNodeWithContentDescription("record")
    private var stopRecordingButton =
        composeTestRule.onNodeWithContentDescription("stop_recording")
    private var cancelRecordingButton =
        composeTestRule.onNodeWithContentDescription("cancel_recording")
    private var cancelPipelineButton =
        composeTestRule.onNodeWithContentDescription("cancel")
    private var cancellingPipelineButton =
        composeTestRule.onNodeWithContentDescription("cancelling")
    private var userIcon =
        composeTestRule.onNodeWithContentDescription("user_icon")
    private var assistantIcon =
        composeTestRule.onNodeWithContentDescription("voice_assistant_icon")

    private fun setContent(screen: @Composable () -> Unit) {
        composeTestRule.setContent {
            VoiceAssistantTheme {
                screen()
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Test
    fun testInitialIdleState() {
        setContent { MainScreen(state = MainUiState()) }

        startRecordingButton.assertExists()
        startRecordingButton.assertTextEquals("Press to talk")
        composeTestRule.onNodeWithContentDescription("microphone").assertExists()

        stopRecordingButton.assertDoesNotExist()
        cancelRecordingButton.assertDoesNotExist()
        cancelPipelineButton.assertDoesNotExist()
        cancellingPipelineButton.assertDoesNotExist()

        // assert text boxes are blank
        userTextBox.assert(hasText(""))
        responseTextBox.assert(hasText(""))

    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Test
    fun testRecordingState() {
        setContent { MainScreen(state = MainUiState(
            contentState = Constants.ContentStates.Recording)) }

        stopRecordingButton.assertExists()
        stopRecordingButton.assertTextContains("Recording... press to finish")
        composeTestRule.onNodeWithContentDescription("microphone").assertExists()
        cancelRecordingButton.assertExists()
        composeTestRule.onNodeWithContentDescription("cancel_recording").assertExists()

        startRecordingButton.assertDoesNotExist()
        cancelPipelineButton.assertDoesNotExist()
        cancellingPipelineButton.assertDoesNotExist()

        userIcon.onChild().assertContentDescriptionEquals("RecordVoiceOver")
        assistantIcon.onChild().assertContentDescriptionEquals("Person")

    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Test
    fun testTranscribingState() {
        setContent { MainScreen(state = MainUiState(
            contentState = Constants.ContentStates.Transcribing)) }

        cancelPipelineButton.assertExists()
        cancelPipelineButton.assertTextEquals("Cancel")
        composeTestRule.onNodeWithContentDescription("cancel").assertExists()

        startRecordingButton.assertDoesNotExist()
        stopRecordingButton.assertDoesNotExist()
        cancelRecordingButton.assertDoesNotExist()
        cancellingPipelineButton.assertDoesNotExist()

        userIcon.onChild().assertContentDescriptionEquals("Person")
        assistantIcon.onChild().assertContentDescriptionEquals("Person")

    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Test
    fun testRespondingState() {
        setContent { MainScreen(state = MainUiState(
            contentState = Constants.ContentStates.Responding)) }

        cancelPipelineButton.assertExists()
        cancelPipelineButton.assertTextEquals("Cancel")
        composeTestRule.onNodeWithContentDescription("cancel").assertExists()

        startRecordingButton.assertDoesNotExist()
        stopRecordingButton.assertDoesNotExist()
        cancelRecordingButton.assertDoesNotExist()
        cancellingPipelineButton.assertDoesNotExist()

        userIcon.onChild().assertContentDescriptionEquals("Person")
        assistantIcon.onChild().assertContentDescriptionEquals("Person")
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Test
    fun testSpeakingState() {
        setContent { MainScreen(state = MainUiState(
            contentState = Constants.ContentStates.Responding)) }

        cancelPipelineButton.assertExists()
        cancelPipelineButton.assertTextEquals("Cancel")
        composeTestRule.onNodeWithContentDescription("cancel").assertExists()

        startRecordingButton.assertDoesNotExist()
        stopRecordingButton.assertDoesNotExist()
        cancelRecordingButton.assertDoesNotExist()
        cancellingPipelineButton.assertDoesNotExist()

        userIcon.onChild().assertContentDescriptionEquals("Person")
        assistantIcon.onChild().assertContentDescriptionEquals("Person")
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Test
    fun testCancellingState() {
        setContent { MainScreen(state = MainUiState(
            contentState = Constants.ContentStates.Cancelling)) }

        cancellingPipelineButton.assertExists()
        cancellingPipelineButton.assertTextEquals("Cancelling...")

        startRecordingButton.assertDoesNotExist()
        stopRecordingButton.assertDoesNotExist()
        cancelRecordingButton.assertDoesNotExist()
        cancelPipelineButton.assertDoesNotExist()

        userIcon.onChild().assertContentDescriptionEquals("Person")
        assistantIcon.onChild().assertContentDescriptionEquals("Person")

    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Test
    fun testTextVisible() {
        val exampleUserText = "Example transcription of speech from user"
        val exampleResponseText = "Example response from Voice Assistant"
        setContent { MainScreen(state = MainUiState(
            contentState = Constants.ContentStates.Responding,
            userText = exampleUserText,
            responseText = exampleResponseText)) }

        userTextBox.assert(hasText(exampleUserText))
        responseTextBox.assert(hasText(exampleResponseText))
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Test
    fun testPerformanceMetricsVisible() {
        val sttTime = "1.4"
        val llmEncodeTPS = "8.3"
        val llmDecodeTPS = "5.3"
        val ttsTime = "0.7"

        val model1metric =
            composeTestRule.onNodeWithContentDescription("Speech recognition time")
        val model2metric =
            composeTestRule.onNodeWithContentDescription("LLM encode tokens/s")
        val model3metric =
            composeTestRule.onNodeWithContentDescription("LLM decode tokens/s")

        setContent { MainScreen(state = MainUiState(
            displayPerformance = true,
            sttTime = sttTime,
            llmEncodeTPS = llmEncodeTPS,
            llmDecodeTPS = llmDecodeTPS
        )
        ) }

        model1metric.onChildAt(1).assertTextEquals(sttTime)
        model2metric.onChildAt(1).assertTextEquals(llmEncodeTPS)
        model3metric.onChildAt(1).assertTextEquals(llmDecodeTPS)
    }

}
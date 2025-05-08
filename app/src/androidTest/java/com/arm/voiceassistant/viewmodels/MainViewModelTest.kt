/*
 * SPDX-FileCopyrightText: Copyright 2024-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.viewmodels
import android.app.Application
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.arm.voiceassistant.utils.AppContext
import com.arm.voiceassistant.utils.Constants
import com.arm.voiceassistant.utils.Constants.ContentStates
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
/**
 * Testing MainViewModel functions
*/
@RunWith(MockitoJUnitRunner::class)
class MainViewModelTest {
    private var mainViewModel: MainViewModel? = null
    private var mainUiState: StateFlow<MainUiState>? = null
    @Before
    fun setupViewModel() {
        val application: Application = Mockito.mock(Application::class.java)
        val appContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
        Mockito.`when`(application.applicationContext).thenReturn(appContext)
        AppContext.getInstance().context = appContext

        mainViewModel = MainViewModel(application, true)
        mainUiState = mainViewModel?.uiState
    }
    @After
    fun tearDown() {
        mainViewModel = null
        mainUiState = null

    }
    @Test
    fun testInitialState() {
        assertEquals(ContentStates.Idle, mainUiState?.value?.contentState)
        assertEquals(Error(state = false, message = ""), mainUiState?.value?.error)
        assertEquals("", mainUiState?.value?.userText)
        assertEquals("", mainUiState?.value?.responseText)
        assertEquals("00:00", mainUiState?.value?.recTime)
        assertEquals(Constants.INITIAL_METRICS_VALUE, mainUiState?.value?.sttTime)
        assertEquals(Constants.INITIAL_METRICS_VALUE, mainUiState?.value?.llmEncodeTPS)
        assertEquals(Constants.INITIAL_METRICS_VALUE, mainUiState?.value?.llmDecodeTPS)
        assertEquals(false, mainUiState?.value?.displayPerformance)
    }
    @Test
    fun testOnError() {
        mainViewModel?.onError("error message")
        assertEquals(true, mainUiState?.value?.error?.state)
        assertEquals("error message", mainUiState?.value?.error?.message)
    }
    @Test
    fun testClearError() {
        testOnError()
        mainViewModel?.clearError()
        assertEquals(false, mainUiState?.value?.error?.state)
        assertEquals("", mainUiState?.value?.error?.message)
    }

    @Test
    fun testTogglePerformanceMetrics() {
        assertEquals(false, mainUiState?.value?.displayPerformance)
        mainViewModel?.togglePerformanceMetrics()
        assertEquals(true, mainUiState?.value?.displayPerformance)
        mainViewModel?.togglePerformanceMetrics()
        assertEquals(false, mainUiState?.value?.displayPerformance)
    }

}
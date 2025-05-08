/*
 * SPDX-FileCopyrightText: Copyright 2024-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.platform.app.InstrumentationRegistry
import com.arm.voiceassistant.ScreenScaffold
import com.arm.voiceassistant.ui.navigation.Routes
import com.arm.voiceassistant.utils.AppContext
import com.arm.voiceassistant.viewmodels.MainViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class NavigationTest {
    private lateinit var navController: TestNavHostController

    @get:Rule
    val composeTestRule = createComposeRule()

    private var mainViewModel: MainViewModel? = null

    @Before
    fun setupViewModel() {
        val application: Application = Mockito.mock(Application::class.java)
        val appContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
        Mockito.`when`(application.applicationContext).thenReturn(appContext)
        AppContext.getInstance().context = appContext

        mainViewModel = MainViewModel(application, true)
    }

    @Before
    fun setContent() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            ScreenScaffold(
                navController = navController,
                mainViewModel = mainViewModel!!
            )
        }
    }

    @After
    fun tearDown() {
        mainViewModel = null
        AppContext.getInstance().context = null
    }

    private fun getCurrentRoute(): String? {
        return navController.currentBackStackEntry?.destination?.route
    }

    /**
     *  Test Main screen displayed on app started
     */
    @Test
    fun testStartDestination() {
        assertEquals(Routes.Main.route, getCurrentRoute())
    }
}

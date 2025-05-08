/*
 * SPDX-FileCopyrightText: Copyright 2024-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arm.voiceassistant.ui.screens.MainScreen
import com.arm.voiceassistant.viewmodels.MainViewModel
import com.arm.voiceassistant.viewmodels.ViewModelProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi

// Main navigation graph for application screens

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    padding: PaddingValues = PaddingValues(0.dp),
    startDestination: String = Routes.Main.route,
    mainViewModel: MainViewModel = viewModel(factory = ViewModelProvider.Factory)
) {
    val mainUiState by mainViewModel.uiState.collectAsState()

    NavHost(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        navController = navController,
        startDestination = startDestination
        ) {
            composable(route = Routes.Main.route) {
                MainScreen(
                    snackbarHostState = snackbarHostState,
                    state = mainUiState,
                    onClickStartRecording = mainViewModel::onStartRecording,
                    onClickStopRecording = mainViewModel::onStopRecording,
                    onClickCancelRecording = mainViewModel::cancelRecording,
                    onClickCancel = mainViewModel::cancelPipeline,
                    onError = mainViewModel::onError,
                    clearErrorMsg = mainViewModel::clearError
                )
        }

    }
}

/*
 * SPDX-FileCopyrightText: Copyright 2024-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arm.voiceassistant.ui.navigation.Routes
import com.arm.voiceassistant.ui.theme.VoiceAssistantTheme

// Currently not being used

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector
)

object BottomNavItems {
    private val home =
        BottomNavItem(
            name = "Home",
            route = Routes.Main.route,
            icon = Icons.Outlined.Home
        )

    val navItemsList = listOf(home)
}

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController()
) {
    NavigationBar(
        modifier = modifier
            .semantics { testTag = "bottom_navigation_bar" }
            .height(80.dp)
            .fillMaxWidth()
    ) {
        val backStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry.value?.destination?.route

        BottomNavItems.navItemsList.forEach { screen ->
            val selected = screen.route == currentRoute
            NavigationBarItem(
                modifier = Modifier.height(80.dp),
                icon = { Icon(screen.icon, contentDescription = screen.name) },
                label = { Text(screen.name, fontWeight = FontWeight.Bold) },
                selected = selected,
                onClick = {
                    if (!selected) {
                        if (screen.name == "Main") {
                            navController.popBackStack(screen.route, inclusive = false)
                        }
                        else {
                            // Pop routes from backstack to prevent stacked screens
                            navController.navigate(screen.route) { popUpTo(Routes.Main.route) }
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomBarPreview() {
    VoiceAssistantTheme {
        BottomNavBar()
    }
}

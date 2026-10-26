package com.perimenopauseintelligence.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.perimenopauseintelligence.app.AppContainer

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("today", "Today", Icons.Default.Create),
    Tab("history", "History", Icons.AutoMirrored.Filled.List),
    Tab("insights", "Insights", Icons.Default.Search),
    Tab("learn", "Learn", Icons.Default.Info),
    Tab("more", "More", Icons.Default.Menu),
)

@Composable
fun AppRoot(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    var showWelcome by remember { mutableStateOf(!container.settings.welcomeSeen) }

    if (showWelcome) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Welcome") },
            text = {
                Text(
                    "Record how you feel, look back at your own history, and prepare for appointments.\n\n" +
                        "• Everything stays on this device, encrypted. There is no account and no internet access.\n" +
                        "• Log only what you want to. Skipping a day is fine.\n" +
                        "• This app does not diagnose or give medical advice.",
                )
            },
            confirmButton = {
                TextButton(onClick = { container.settings.welcomeSeen = true; showWelcome = false }) { Text("Get started") }
            },
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "today", modifier = Modifier.padding(padding)) {
            composable("today") { TodayScreen(container) }
            composable("history") { HistoryScreen(container, onOpenDay = { nav.navigate("today") { launchSingleTop = true; popUpTo("today") } }) }
            composable("insights") { InsightsScreen(container) }
            composable("learn") { LearnScreen(onOpen = { nav.navigate("article/$it") }) }
            composable("article/{id}") { entry -> ArticleScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }) }
            composable("more") { MoreScreen(onOpen = { nav.navigate(it) }) }
            composable("export") { ExportScreen(container, onBack = { nav.popBackStack() }) }
            composable("appointment") { AppointmentScreen(container, onBack = { nav.popBackStack() }) }
            composable("symptoms") { SymptomsScreen(container, onBack = { nav.popBackStack() }) }
            composable("reminders") { RemindersScreen(container, onBack = { nav.popBackStack() }) }
            composable("privacy") { PrivacyScreen(container, onBack = { nav.popBackStack() }) }
        }
    }
}

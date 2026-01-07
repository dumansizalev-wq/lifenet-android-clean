package net.lifenet.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Face 
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.lifenet.core.ui.screens.*
import net.lifenet.core.ui.theme.LIFENETTheme
import net.lifenet.core.ui.theme.ElectricBlue
import net.lifenet.core.ui.theme.CyanNeon
import net.lifenet.core.ui.theme.DeepBlue
import net.lifenet.core.ui.components.AmbientConsentDialog
import net.lifenet.core.utils.PersistenceManager

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Ana Sayfa", Icons.Default.Home)
    object Security : Screen("security", "Güvenlik", Icons.Default.Lock)
    object Messages : Screen("messages", "Mesajlar", Icons.Default.Email)
    object Autonomous : Screen("autonomous", "Otonom", Icons.Default.Face)
    object Settings : Screen("settings", "Ayarlar", Icons.Default.Settings)
}

@Composable
fun LifenetApp(
    persistenceManager: PersistenceManager,
    onConsentGranted: () -> Unit
) {
    var showConsent by remember { mutableStateOf(!persistenceManager.isVsieConsentGranted()) }

    if (showConsent) {
        AmbientConsentDialog(
            onConfirm = {
                persistenceManager.saveVsieConsent(true)
                showConsent = false
                onConsentGranted()
            }
        )
    }

    LIFENETTheme {
        val navController = rememberNavController()
        val items = listOf(
            Screen.Home,
            Screen.Security,
            Screen.Messages,
            Screen.Autonomous,
            Screen.Settings
        )

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = DeepBlue,
                    contentColor = ElectricBlue
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.route == screen.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanNeon,
                                selectedTextColor = CyanNeon,
                                indicatorColor = DeepBlue, // No bubble, just icon color change
                                unselectedIconColor = ElectricBlue.copy(alpha = 0.5f),
                                unselectedTextColor = ElectricBlue.copy(alpha = 0.5f)
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { HomeScreen() }
                composable(Screen.Security.route) { SecurityScreen() }
                composable(Screen.Messages.route) { MessagesScreen(navController) }
                composable("chat/{deviceId}") { backStackEntry ->
                    val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                    ChatScreen(deviceId, navController)
                }
                composable(Screen.Autonomous.route) { AutonomousScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
            }
        }
    }
}

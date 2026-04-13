package com.questkeep.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.questkeep.R
import com.questkeep.ui.components.DiceRollerModal
import com.questkeep.ui.screens.*
import com.questkeep.ui.theme.DarkBackground
import com.questkeep.ui.theme.DarkSurface
import com.questkeep.ui.theme.PrimaryPurple
import com.questkeep.ui.theme.TextSecondary

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int
)

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    var showDiceRoller by remember { mutableStateOf(false) }

    // Currently selected campaign — rememberSaveable survives screen rotation
    var selectedCampaignId by rememberSaveable { mutableStateOf<String?>(null) }
    var isGmInSelectedCampaign by rememberSaveable { mutableStateOf(false) }
    var selectedCampaignGmId by rememberSaveable { mutableStateOf<String?>(null) }

    val bottomItems = listOf(
        BottomNavItem(NavRoutes.CHARACTER_SHEET, Icons.Filled.Person, R.string.tab_character),
        BottomNavItem(NavRoutes.CHAT, Icons.AutoMirrored.Filled.Chat, R.string.tab_chat),
        BottomNavItem(NavRoutes.CAMPAIGN_LIST, Icons.Filled.Group, R.string.tab_group),
        BottomNavItem(NavRoutes.MENU, Icons.Filled.Menu, R.string.tab_menu)
    )

    // Bottom bar routes — hide it on sub-screens
    val bottomBarRoutes = bottomItems.map { it.route }
    val currentRoute by navController.currentBackStackEntryAsState()
    val showBottomBar = currentRoute?.destination?.route in bottomBarRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = DarkBackground,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(containerColor = DarkSurface) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDest = navBackStackEntry?.destination
                        bottomItems.forEach { item ->
                            val selected = currentDest?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryPurple,
                                    selectedTextColor = PrimaryPurple,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = DarkBackground
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = NavRoutes.CHARACTER_SHEET,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(NavRoutes.CHARACTER_SHEET) {
                    CharacterSheetScreen(
                        campaignId = selectedCampaignId,
                        isGm = isGmInSelectedCampaign,
                        onNavigateToBestiary = { navController.navigate(NavRoutes.BESTIARY) }
                    )
                }
                composable(NavRoutes.CHAT) {
                    ChatScreen(campaignId = selectedCampaignId)
                }
                composable(NavRoutes.CAMPAIGN_LIST) {
                    CampaignListScreen(
                        onCampaignSelected = { campaignId, isGm, gmId ->
                            selectedCampaignId = campaignId
                            isGmInSelectedCampaign = isGm
                            selectedCampaignGmId = gmId
                            navController.navigate(NavRoutes.CHARACTER_SHEET) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(NavRoutes.MENU) {
                    MenuScreen(
                        onLogout = onLogout,
                        isGm = isGmInSelectedCampaign,
                        campaignId = selectedCampaignId,
                        onNavigateToProfile = { navController.navigate(NavRoutes.PROFILE) },
                        onNavigateToGmNotes = { navController.navigate(NavRoutes.GM_NOTES) },
                        onNavigateToBestiary = { navController.navigate(NavRoutes.BESTIARY) },
                        onNavigateToPlayerList = { navController.navigate(NavRoutes.PLAYER_LIST) },
                        onNavigateToItemCards = { navController.navigate(NavRoutes.ITEM_CARDS) },
                        onNavigateToInventory = { navController.navigate(NavRoutes.INVENTORY) },
                        onNavigateToSharedInventory = { navController.navigate(NavRoutes.SHARED_INVENTORY) },
                        onNavigateToPlayerNotes = { navController.navigate(NavRoutes.PLAYER_NOTES) }
                    )
                }

                // Phase 2 sub-screens (no bottom bar)
                composable(NavRoutes.PROFILE) {
                    ProfileScreen(onBack = { navController.popBackStack() })
                }
                composable(NavRoutes.GM_NOTES) {
                    GmNotesScreen(
                        campaignId = selectedCampaignId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NavRoutes.BESTIARY) {
                    BestiaryScreen(
                        campaignId = selectedCampaignId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NavRoutes.PLAYER_LIST) {
                    PlayerListScreen(
                        campaignId = selectedCampaignId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NavRoutes.ITEM_CARDS) {
                    ItemCardManagerScreen(
                        campaignId = selectedCampaignId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NavRoutes.INVENTORY) {
                    InventoryScreen(
                        campaignId = selectedCampaignId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NavRoutes.SHARED_INVENTORY) {
                    SharedInventoryScreen(
                        campaignId = selectedCampaignId,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NavRoutes.PLAYER_NOTES) {
                    PlayerNotesScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        // Dice FAB — always visible on every screen, pinned at BottomStart to avoid
        // overlapping per-screen FABs that live at BottomEnd (inventory +, notes +, etc.)
        FloatingActionButton(
            onClick = { showDiceRoller = true },
            containerColor = PrimaryPurple,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 16.dp,
                    bottom = if (showBottomBar) 96.dp else 16.dp  // 80dp nav bar + 16dp gap
                )
        ) {
            Icon(Icons.Filled.Casino, contentDescription = stringResource(R.string.dice_roller))
        }
    }

    if (showDiceRoller) {
        DiceRollerModal(
            campaignId = selectedCampaignId,
            gmId = selectedCampaignGmId,
            onDismiss = { showDiceRoller = false }
        )
    }
}

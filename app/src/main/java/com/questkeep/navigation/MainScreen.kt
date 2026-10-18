package com.questkeep.navigation

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import com.questkeep.data.local.SessionManager
import com.questkeep.ui.components.DiceRollerModal
import com.questkeep.ui.screens.*
import com.questkeep.ui.theme.*
import kotlin.math.hypot
import kotlin.math.roundToInt

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context.applicationContext) }
    val networkMonitor = remember { com.questkeep.utils.NetworkMonitor(context.applicationContext) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.checkCurrentConnectivity())
    val savedCampId by sessionManager.selectedCampaignId.collectAsState()
    val savedIsGm by sessionManager.isGm.collectAsState()
    val savedGmId by sessionManager.selectedGmId.collectAsState()

    val t = LocalXanteya.current
    val navController = rememberNavController()
    var showDiceRoller by remember { mutableStateOf(false) }

    // Persistent selected campaign context
    var selectedCampaignId by rememberSaveable { mutableStateOf(savedCampId) }
    var isGmInSelectedCampaign by rememberSaveable { mutableStateOf(savedIsGm) }
    var selectedCampaignGmId by rememberSaveable { mutableStateOf(savedGmId) }

    LaunchedEffect(savedCampId, savedIsGm, savedGmId) {
        if (selectedCampaignId == null && savedCampId != null) {
            selectedCampaignId = savedCampId
            isGmInSelectedCampaign = savedIsGm
            selectedCampaignGmId = savedGmId
        }
    }

    val bottomItems = listOf(
        BottomNavItem(NavRoutes.CHARACTER_SHEET, Icons.Filled.Person, "Лист"),
        BottomNavItem(NavRoutes.CHAT, Icons.AutoMirrored.Filled.Chat, "Чат"),
        BottomNavItem(NavRoutes.CAMPAIGN_LIST, Icons.Filled.Group, "Кампании"),
        BottomNavItem(NavRoutes.MENU, Icons.Filled.Menu, "Меню")
    )

    val bottomBarRoutes = bottomItems.map { it.route }
    val currentRoute by navController.currentBackStackEntryAsState()
    val showBottomBar = currentRoute?.destination?.route in bottomBarRoutes

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var quickRollStatName by remember { mutableStateOf<String?>(null) }
    var quickRollModifier by remember { mutableStateOf<Int?>(null) }

    val onNavigateToRoute: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(t.colors.bg)) {
        if (isLandscape) {
            // Landscape Layout: Side Navigation Rail on the left, full-height content on the right
            Row(modifier = Modifier.fillMaxSize()) {
                if (showBottomBar) {
                    ThemedNavigationRail(
                        items = bottomItems,
                        currentRoute = currentRoute?.destination?.route,
                        onNavigate = onNavigateToRoute,
                        onOpenDice = {
                            quickRollStatName = null
                            quickRollModifier = null
                            showDiceRoller = true
                        }
                    )
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(t.colors.line))
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    androidx.compose.animation.AnimatedVisibility(visible = !isOnline) {
                        OfflineBanner()
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AppNavHost(
                            navController = navController,
                            selectedCampaignId = selectedCampaignId,
                            isGmInSelectedCampaign = isGmInSelectedCampaign,
                            selectedCampaignGmId = selectedCampaignGmId,
                            sessionManager = sessionManager,
                            onLogout = onLogout,
                            onSelectCampaign = { campId, isGm, gmId ->
                                selectedCampaignId = campId
                                isGmInSelectedCampaign = isGm
                                selectedCampaignGmId = gmId
                                sessionManager.saveSelectedCampaign(campId, isGm, gmId)
                                navController.navigate(NavRoutes.CHARACTER_SHEET) { launchSingleTop = true }
                            },
                            onOpenDiceRoller = {
                                quickRollStatName = null
                                quickRollModifier = null
                                showDiceRoller = true
                            },
                            onQuickRoll = { name, modifier ->
                                quickRollStatName = name
                                quickRollModifier = modifier
                                showDiceRoller = true
                            }
                        )
                    }
                }
            }
        } else {
            // Portrait Layout: Scaffold with BottomNavigationBar
            Scaffold(
                containerColor = t.colors.bg,
                bottomBar = {
                    if (showBottomBar) {
                        ThemedNavigationBar(
                            items = bottomItems,
                            currentRoute = currentRoute?.destination?.route,
                            onNavigate = onNavigateToRoute
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(t.colors.bg)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.animation.AnimatedVisibility(visible = !isOnline) {
                            OfflineBanner()
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            AppNavHost(
                                navController = navController,
                                selectedCampaignId = selectedCampaignId,
                                isGmInSelectedCampaign = isGmInSelectedCampaign,
                                selectedCampaignGmId = selectedCampaignGmId,
                                sessionManager = sessionManager,
                                onLogout = onLogout,
                                onSelectCampaign = { campId, isGm, gmId ->
                                    selectedCampaignId = campId
                                    isGmInSelectedCampaign = isGm
                                    selectedCampaignGmId = gmId
                                    sessionManager.saveSelectedCampaign(campId, isGm, gmId)
                                    navController.navigate(NavRoutes.CHARACTER_SHEET) { launchSingleTop = true }
                                },
                                onOpenDiceRoller = {
                                    quickRollStatName = null
                                    quickRollModifier = null
                                    showDiceRoller = true
                                },
                                onQuickRoll = { name, modifier ->
                                    quickRollStatName = name
                                    quickRollModifier = modifier
                                    showDiceRoller = true
                                }
                            )
                        }
                    }
                }
            }

            // Draggable Lore Dice FAB in portrait mode with edge-snapping
            ThemedDiceFab(
                onClick = {
                    quickRollStatName = null
                    quickRollModifier = null
                    showDiceRoller = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = if (showBottomBar) 88.dp else 24.dp
                    )
            )
        }
    }

    if (showDiceRoller) {
        DiceRollerModal(
            campaignId = selectedCampaignId,
            gmId = selectedCampaignGmId,
            initialModifier = quickRollModifier,
            initialStatName = quickRollStatName,
            onDismiss = {
                showDiceRoller = false
                quickRollStatName = null
                quickRollModifier = null
            }
        )
    }
}

@Composable
private fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    selectedCampaignId: String?,
    isGmInSelectedCampaign: Boolean,
    selectedCampaignGmId: String?,
    sessionManager: SessionManager,
    onLogout: () -> Unit,
    onSelectCampaign: (String, Boolean, String) -> Unit,
    onOpenDiceRoller: () -> Unit,
    onQuickRoll: (name: String, modifier: Int) -> Unit
) {
    val t = LocalXanteya.current
    NavHost(
        navController = navController,
        startDestination = NavRoutes.CHARACTER_SHEET,
        modifier = Modifier.fillMaxSize().background(t.colors.bg)
    ) {
        composable(NavRoutes.CHARACTER_SHEET) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                CharacterSheetScreen(
                    campaignId = selectedCampaignId,
                    isGm = isGmInSelectedCampaign,
                    onNavigateToBestiary = { navController.navigate(NavRoutes.BESTIARY) },
                    onOpenDiceRoller = onOpenDiceRoller,
                    onQuickRoll = onQuickRoll
                )
            }
        }
        composable(NavRoutes.CHAT) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                ChatScreen(campaignId = selectedCampaignId)
            }
        }
        composable(NavRoutes.CAMPAIGN_LIST) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                CampaignListScreen(
                    onCampaignSelected = onSelectCampaign
                )
            }
        }
        composable(NavRoutes.MENU) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                MenuScreen(
                    onLogout = {
                        sessionManager.clearSession()
                        onLogout()
                    },
                    isGm = isGmInSelectedCampaign,
                    campaignId = selectedCampaignId,
                    onNavigateToProfile = { navController.navigate(NavRoutes.PROFILE) },
                    onNavigateToGmNotes = { navController.navigate(NavRoutes.GM_NOTES) },
                    onNavigateToBestiary = { navController.navigate(NavRoutes.BESTIARY) },
                    onNavigateToPlayerList = { navController.navigate(NavRoutes.PLAYER_LIST) },
                    onNavigateToItemCards = { navController.navigate(NavRoutes.ITEM_CARDS) },
                    onNavigateToInventory = { navController.navigate(NavRoutes.INVENTORY) },
                    onNavigateToSharedInventory = { navController.navigate(NavRoutes.SHARED_INVENTORY) },
                    onNavigateToPlayerNotes = { navController.navigate(NavRoutes.PLAYER_NOTES) },
                    onNavigateToRules = { navController.navigate(NavRoutes.RULES) }
                )
            }
        }

        // Sub-screens
        composable(NavRoutes.PROFILE) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                ProfileScreen(onBack = { navController.popBackStack() })
            }
        }
        composable(NavRoutes.GM_NOTES) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                GmNotesScreen(
                    campaignId = selectedCampaignId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(NavRoutes.BESTIARY) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                BestiaryScreen(
                    campaignId = selectedCampaignId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(NavRoutes.PLAYER_LIST) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                PlayerListScreen(
                    campaignId = selectedCampaignId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(NavRoutes.ITEM_CARDS) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                ItemCardManagerScreen(
                    campaignId = selectedCampaignId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(NavRoutes.INVENTORY) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                InventoryScreen(
                    campaignId = selectedCampaignId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(NavRoutes.SHARED_INVENTORY) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                SharedInventoryScreen(
                    campaignId = selectedCampaignId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(NavRoutes.PLAYER_NOTES) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                PlayerNotesScreen(onBack = { navController.popBackStack() })
            }
        }
        composable(NavRoutes.RULES) {
            Surface(modifier = Modifier.fillMaxSize(), color = t.colors.bg) {
                RulesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/**
 * Landscape Side Navigation Rail
 */
@Composable
private fun ThemedNavigationRail(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onOpenDice: () -> Unit
) {
    val t = LocalXanteya.current
    Surface(
        color = t.colors.panel,
        modifier = Modifier
            .fillMaxHeight()
            .width(84.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val shape = when (t.id) {
                        "falloran" -> RoundedCornerShape(2.dp)
                        "nirventhal" -> RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp)
                        "tsingate" -> CutCornerShape
                        else -> RoundedCornerShape(14.dp)
                    }

                    Surface(
                        onClick = { onNavigate(item.route) },
                        shape = shape,
                        color = if (isSelected) t.colors.accent else t.colors.bg,
                        border = BorderStroke(1.dp, if (isSelected) t.colors.accent else t.colors.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) t.colors.bg else t.colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                fontFamily = t.display,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) t.colors.bg else t.colors.text
                            )
                        }
                    }
                }
            }

            // Dedicated Dice Button at the bottom of the navigation rail
            Surface(
                onClick = onOpenDice,
                shape = RoundedCornerShape(t.corner),
                color = t.colors.bg,
                border = BorderStroke(1.5.dp, t.colors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Casino,
                        contentDescription = "Кубы",
                        tint = t.colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Кубы",
                        fontFamily = t.display,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.colors.accent
                    )
                }
            }
        }
    }
}

/**
 * Themed Navigation Bar for Portrait orientation matching each lore theme
 */
@Composable
private fun ThemedNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val t = LocalXanteya.current
    Surface(
        color = t.colors.bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (t.id) {
            "falloran" -> {
                // 4 connected colored ЦМК pass tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    val passColors = listOf(
                        Color(0xFFE8E2D8) to Color(0xFF18181A), // Лист: cream
                        Color(0xFF9E8A38) to Color(0xFF18181A), // Чат: olive
                        Color(0xFF9E5528) to Color(0xFFFFFFFF), // Кампании: copper
                        Color(0xFFA83226) to Color(0xFFFFFFFF)  // Меню: rust red
                    )
                    val cutCorner = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 14.dp)
                    items.forEachIndexed { index, item ->
                        val isSelected = currentRoute == item.route
                        val (baseBg, baseText) = passColors.getOrElse(index) { t.colors.accent to t.colors.bg }
                        val bgColor = if (isSelected) baseBg else baseBg.copy(alpha = 0.45f)
                        val textColor = if (isSelected) baseText else baseText.copy(alpha = 0.7f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 1.dp)
                                .clip(cutCorner)
                                .background(bgColor)
                                .clickable { onNavigate(item.route) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.label,
                                fontFamily = RussoOneFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }
            "nirventhal" -> {
                val leafShape = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 6.dp, bottomStart = 6.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        val bg = if (isSelected) t.colors.accent else t.colors.panel
                        val txt = if (isSelected) t.colors.bg else t.colors.accent

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(leafShape)
                                .background(bg)
                                .border(1.dp, t.colors.line, leafShape)
                                .clickable { onNavigate(item.route) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.label,
                                fontFamily = PhilosopherFamily,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = txt
                            )
                        }
                    }
                }
            }
            "xanteya" -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        val bg = if (isSelected) t.colors.hot else t.colors.panel
                        val txt = if (isSelected) t.colors.bg else t.colors.accent

                        Box(
                            modifier = Modifier
                                .size(width = 72.dp, height = 48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(bg)
                                .border(1.5.dp, if (isSelected) t.colors.hot else t.colors.line, RoundedCornerShape(24.dp))
                                .clickable { onNavigate(item.route) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.label,
                                fontFamily = ForumFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = txt
                            )
                        }
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { item ->
                        val isSelected = currentRoute == item.route
                        val bg = if (isSelected) t.colors.accent else t.colors.panel
                        val txt = if (isSelected) t.colors.bg else t.colors.accent

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(CutCornerShape)
                                .background(bg)
                                .border(1.dp, t.colors.line, CutCornerShape)
                                .clickable { onNavigate(item.route) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.label,
                                fontFamily = RuslanDisplayFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = txt
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Atmospheric Lore Dice Catalyst (FAB) with smooth touch drag-repositioning and edge-snapping
 */
@Composable
private fun ThemedDiceFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    val shape = when (t.id) {
        "nirventhal" -> RoundedCornerShape(topStart = 20.dp, bottomEnd = 20.dp, topEnd = 6.dp, bottomStart = 6.dp)
        "falloran" -> RoundedCornerShape(3.dp)
        "tsingate" -> CutCornerShape
        else -> RoundedCornerShape(16.dp)
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val marginPx = with(density) { 16.dp.toPx() }

    var fabWidthPx by remember { mutableFloatStateOf(with(density) { 96.dp.toPx() }) }

    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val leftSnapOffset = (2 * marginPx + fabWidthPx - screenWidthPx).coerceAtMost(0f)
    val rightSnapOffset = 0f
    val minY = -(screenHeightPx - with(density) { 180.dp.toPx() })
    val maxY = with(density) { 20.dp.toPx() }

    Surface(
        shape = shape,
        color = t.colors.panel,
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                fabWidthPx = coordinates.size.width.toFloat()
            }
            .offset {
                IntOffset(
                    animOffsetX.value.roundToInt(),
                    animOffsetY.value.roundToInt()
                )
            }
            .pointerInput(Unit) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDragDistance = 0f
                    var isDragging = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.changedToUp()) {
                            if (!isDragging) {
                                onClick()
                            } else {
                                coroutineScope.launch {
                                    val targetX = if (animOffsetX.value < leftSnapOffset / 2f) leftSnapOffset else rightSnapOffset
                                    animOffsetX.animateTo(
                                        targetValue = targetX,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                            break
                        }

                        val dragAmount = change.positionChange()
                        totalDragDistance += hypot(dragAmount.x, dragAmount.y)

                        if (!isDragging && totalDragDistance > touchSlop) {
                            isDragging = true
                        }

                        if (isDragging) {
                            change.consume()
                            coroutineScope.launch {
                                val newX = (animOffsetX.value + dragAmount.x).coerceIn(leftSnapOffset - 40f, 40f)
                                val newY = (animOffsetY.value + dragAmount.y).coerceIn(minY, maxY)
                                animOffsetX.snapTo(newX)
                                animOffsetY.snapTo(newY)
                            }
                        }
                    }
                }
            }
            .border(1.5.dp, t.colors.accent, shape)
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Casino,
                contentDescription = "Кубы",
                tint = t.colors.accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Кубы",
                fontFamily = t.display,
                color = t.colors.accent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun OfflineBanner() {
    val t = LocalXanteya.current
    Surface(
        color = t.colors.panel,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, t.colors.line))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(t.colors.hot)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Оффлайн режим • Изменения сохраняются на устройстве",
                color = t.colors.text,
                fontSize = 12.sp,
                fontFamily = t.display,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

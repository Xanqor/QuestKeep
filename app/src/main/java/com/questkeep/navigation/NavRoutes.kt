package com.questkeep.navigation

object NavRoutes {
    // Auth graph
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Main graph (tabs inside MainScreen)
    const val MAIN = "main"
    const val CHARACTER_SHEET = "character_sheet"
    const val CHAT = "chat"
    const val CAMPAIGN_LIST = "campaign_list"
    const val MENU = "menu"

    // Campaign detail with arg
    const val CAMPAIGN_DETAIL = "campaign_detail/{campaignId}"
    fun campaignDetail(campaignId: String) = "campaign_detail/$campaignId"

    // Phase 2 screens (accessible from Menu tab)
    const val PROFILE = "profile"
    const val GM_NOTES = "gm_notes"
    const val BESTIARY = "bestiary"
    const val PLAYER_LIST = "player_list"
    const val INVENTORY = "inventory"
    const val ITEM_CARDS = "item_cards"
    const val SHARED_INVENTORY = "shared_inventory"
    const val PLAYER_NOTES = "player_notes"
    const val RULES = "rules"
}

enum class BottomTab(val route: String, val labelRes: Int, val iconRes: String) {
    CHARACTER(NavRoutes.CHARACTER_SHEET, 0, "character"),
    CHAT(NavRoutes.CHAT, 0, "chat"),
    GROUP(NavRoutes.CAMPAIGN_LIST, 0, "group"),
    MENU(NavRoutes.MENU, 0, "menu")
}

package com.questkeep.utils

object Constants {
    // Firestore collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_CAMPAIGNS = "campaigns"
    const val COLLECTION_CHARACTERS = "characters"
    const val COLLECTION_MESSAGES = "messages"
    const val COLLECTION_INVENTORY = "inventory"
    const val COLLECTION_ITEM_CARDS = "item_cards"

    // Imgbb
    const val IMGBB_API_KEY = "3313f8ba8ea1748efbdbf21247c0d1f0"
    const val IMGBB_BASE_URL = "https://api.imgbb.com/"

    // Validation
    const val MIN_PASSWORD_LENGTH = 8
    const val MIN_NICKNAME_LENGTH = 3
    const val MAX_NICKNAME_LENGTH = 20
    const val MAX_CAMPAIGN_PLAYERS = 5
    const val INVITE_CODE_LENGTH = 6
    const val MAX_INSPIRATION = 6
    const val MAX_MESSAGES_CACHED = 100

    // Resource
    const val RESOURCE_STEP = 0.5f
    const val RESOURCE_MAX_DEFAULT = 4f

    // Datastore
    const val DATASTORE_NAME = "questkeep_prefs"
    const val KEY_USER_ID = "user_id"
}
